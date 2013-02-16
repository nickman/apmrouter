
var Jolokia=(function($){var DEFAULT_CLIENT_PARAMS={type:"POST",jsonp:false};var GET_AJAX_PARAMS={type:"GET"};var POST_AJAX_PARAMS={type:"POST",processData:false,dataType:"json",contentType:"text/json"};var PROCESSING_PARAMS=["maxDepth","maxCollectionSize","maxObjects","ignoreErrors"];function Jolokia(param){if(!(this instanceof arguments.callee)){return new Jolokia(param);}
this.CLIENT_VERSION="1.0.1";if(typeof param==="string"){param={url:param};}
$.extend(this,DEFAULT_CLIENT_PARAMS,param);this.request=function(request,params){var opts=$.extend({},this,params);assertNotNull(opts.url,"No URL given");var ajaxParams={};$.each(["username","password","timeout"],function(i,key){if(opts[key]){ajaxParams[key]=opts[key];}});if(extractMethod(request,opts)==="post"){$.extend(ajaxParams,POST_AJAX_PARAMS);ajaxParams.data=JSON.stringify(request);ajaxParams.url=ensureTrailingSlash(opts.url);}else{$.extend(ajaxParams,GET_AJAX_PARAMS);ajaxParams.dataType=opts.jsonp?"jsonp":"json";ajaxParams.url=opts.url+"/"+constructGetUrlPath(request);}
ajaxParams.url=addProcessingParameters(ajaxParams.url,opts);if(opts.ajaxError){ajaxParams.error=opts.ajaxError;}
if(opts.success){var success_callback=constructCallbackDispatcher(opts.success);var error_callback=constructCallbackDispatcher(opts.error);ajaxParams.success=function(data){var responses=$.isArray(data)?data:[data];for(var idx=0;idx<responses.length;idx++){var resp=responses[idx];if(resp.status==null||resp.status!=200){error_callback(resp,idx);}else{success_callback(resp,idx);}}};$.ajax(ajaxParams);}else{if(opts.jsonp){throw Error("JSONP is not supported for synchronous requests");}
ajaxParams.async=false;var xhr=$.ajax(ajaxParams);if(httpSuccess(xhr)){return $.parseJSON(xhr.responseText);}else{return null;}}};}
function constructCallbackDispatcher(callback){if(callback==null){return function(response){console.log("Ignoring response "+JSON.stringify(response));};}else if(callback==="ignore"){return function(){};}
var callbackArray=$.isArray(callback)?callback:[callback];return function(response,idx){callbackArray[idx%callbackArray.length](response,idx);}}
function extractMethod(request,opts){var methodGiven=opts&&opts.method?opts.method.toLowerCase():null,method;if(methodGiven){if(methodGiven==="get"){if($.isArray(request)){throw new Error("Cannot use GET with bulk requests");}
if(request.type.toLowerCase()==="read"&&$.isArray(request.attribute)){throw new Error("Cannot use GET for read with multiple attributes");}
if(request.target){throw new Error("Cannot use GET request with proxy mode");}}
method=methodGiven;}else{method=$.isArray(request)||(request.type.toLowerCase()==="read"&&$.isArray(request.attribute))||request.target?"post":"get";}
if(opts.jsonp&&method==="post"){throw new Error("Can not use JSONP with POST requests");}
return method;}
function addProcessingParameters(url,opts){var sep=url.indexOf("?")>0?"&":"?";$.each(PROCESSING_PARAMS,function(i,key){if(opts[key]!=null){url+=sep+key+"="+opts[key];sep="&";}});return url;}
function constructGetUrlPath(request){var type=request.type;assertNotNull(type,"No request type given for building a GET request");type=type.toLowerCase();var extractor=GET_URL_EXTRACTORS[type];assertNotNull(extractor,"Unknown request type "+type);var result=extractor(request);var parts=result.parts||{};var url=type;$.each(parts,function(i,v){url+="/"+Jolokia.escape(v)});if(result.path){url+=(result.path[0]=='/'?"":"/")+result.path;}
return url;}
function ensureTrailingSlash(url){return url.replace(/\/*$/,"/");}
var GET_URL_EXTRACTORS={"read":function(request){if(request.attribute==null){return{parts:[request.mbean]};}else{return{parts:[request.mbean,request.attribute],path:request.path};}},"write":function(request){return{parts:[request.mbean,request.attribute,valueToString(request.value)],path:request.path};},"exec":function(request){var ret=[request.mbean,request.operation];if(request.arguments&&request.arguments.length>0){$.each(request.arguments,function(index,value){ret.push(valueToString(value));});}
return{parts:ret};},"version":function(){return{};},"search":function(request){return{parts:[request.mbean]};},"list":function(request){return{path:request.path};}};function valueToString(value){if(value==null){return"[null]";}
if($.isArray(value)){var ret="";for(var i=0;i<value.length;i++){ret+=value==null?"[null]":singleValueToString(value[i]);if(i<value.length-1){ret+=",";}}
return ret;}else{return singleValueToString(value);}}
function singleValueToString(value){if(typeof value==="string"&&value.length==0){return"\"\"";}else{return value.toString();}}
function httpSuccess(xhr){try{return!xhr.status&&location.protocol==="file:"||xhr.status>=200&&xhr.status<300||xhr.status===304||xhr.status===1223;}catch(e){}
return false;}
function assertNotNull(object,message){if(object==null){throw new Error(message);}}
Jolokia.escape=function(part){return part.replace(/!/g,"!!").replace(/\//g,"!/");};return Jolokia;})(jQuery);