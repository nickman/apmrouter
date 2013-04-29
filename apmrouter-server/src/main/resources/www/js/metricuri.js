		
		var stateCache = {};
		var _RID_FACTORY_ = 0;
		var _RERID_FACTORY_ = 0;
		var _SUBSCRIPTIONS_ = {};
		var _TIMEOUT_ = 2000;
		var metricGridColumnModel = [
	                  			{ "sTitle": "Metric URI"},   
	                  			{ "sTitle": "Sub Count" },
	                 			{ "sTitle": "Metric Types" },
	                 			{ "sTitle": "Subscription Types" },                			
	                 			{ "sTitle": "Metric Status" },
	                 			{ "sTitle": "Max Depth" },
	                 			{ "sTitle": "Name" },
	                 			{ "sTitle": "Data" },
	                 			{ "sTitle": "New" },
	                 			{ "sTitle": "toActive" },
	                 			{ "sTitle": "toStale" },
	                 			{ "sTitle": "toOffline" }
	                 ];


/*
 * Metric Subscription
 *  - uri
 *  - sub-count
 *  - subId (rid)
 *  - 
 *   
 */		
		
		
		
		
function init_metricuri() {
	var spinner = $( "#maxDepth" ).spinner({ min: 0, max: 99 });
    $('body').layout({ applyDefaultStyles: true, center__size: "50%", north__size: "50%" });
	metricGrid = $('#metricDisplayTable').dataTable({
        "bJQueryUI": true,
        "sPaginationType": "full_numbers",
        "aoColumns" : metricGridColumnModel,
        "bInfo" : true,
        "bPaginate" : false,
        "bSort" : true,
        "sScrollY": "90px",
        "bCollapse" : true,
        "bScrollCollapse": true,
        "sRowSelect": "multi"
    });

	// ========================================
	//  Set all the default options
	// ========================================	
	$('input[name="subType"]').attr('checked', 'true');
	$('input[name="metricStatus"]').attr('checked', 'true');
	$('#metricType option.default-option').attr('selected', true);
	
	
	// ========================================
	//  Bind the websocket session events
	// ========================================
	$(document).bind('websocket-sessionid', function(event, sessionid){
		console.info("SESSIONID:%s", sessionid);
	});
	$('#sessionIdDisplay').bind('websocket-sessionid', function(event, sessionid){
		$(this).val(sessionid);
	});
	
	$('#sessionIdDisplay').bind('websocket-disconnected', function(event, sessionid){
		$(this).val("");
	});
	$('#connectedLight').bind('websocket-connected', function(event){
		console.debug("Processing websocket.connected. event [%o]", event);
		$(this).html(stateCache.connected_img);
	});
	$('#connectedLight').bind('websocket-connecting', function(event){
		console.debug("Processing websocket.connecting. event [%o]", event);
		$(this).html(stateCache.connecting_img);
	});	    	
	
	$('#connectedLight').bind('websocket-disconnected websocket-connecttimeout', function(event, sessionid){
		console.debug("Processing websocket.disconnected. event [%o]", event);
		$(this).html(stateCache.disconnected_img);
	});
	$('#uri').bind('websocket-sessionid', function(event, sessionid){
		//$(this).autocomplete( "option", "autoFocus" );
	});
	
//	$( "#uri" ).autocomplete({
//		source: "/mauto",
//		minLength: 0,
//		disabled: false,
//		response: function( event, ui ) {
//			console.info("Response:  event: %o   ui:%o", event, ui);
//		},	
//		select: function( event, ui ) {
//	        console.info( ui.item ?
//	          "Selected: " + ui.item.value + " aka " + ui.item.id :
//	          "Nothing selected, input was " + this.value );
//	    }		
//	});
//	$("#uri").keypress(function(event) {
//		   if(event.keyCode==10) {
//			   if(event.ctrlKey) {
//				   $( "#uri" ).autocomplete( "search", "" );
//			   }
//		   }
//	});

	// ========================================
	// Cache the content for session manageent
	// so we have it on session loss
	// and then initialize the websocket
	// ========================================
	$.when(
	    	getContentBatch(stateCache, {
	    		'connected_img' : 'img/svg/connected-status.svg',
	    		'disconnected_img' : 'img/svg/disconnected-status.svg',
	    		'connecting_img' : 'img/svg/connecting-status.svg'	    		
	    	})).then(function(){
	    		$(document).websocket({wsuri:'ws://' + document.location.host + '/ws'});			    		
	    	}
	);	    	
}


/**
 * Generalized websocket service invoker
 * @param command A (mandatory) JSON subscription request
 * @param options:<ul>
 * 	<li><b>timeout</b>: The timeout in ms. on the request invocation confirm. (i.e. not a subscriber timeout) Default is 2000 ms.</li>
 * 	<li><b>onresponse</b>: A callback invoked when the immediate response of the command invocation is received.</li>
 * 	<li><b>ontimeout</b>: A callback invoked when the request times out</li>
 * 	<li><b>onevent</b>: A callback invoked when an asynchronous event is received associated to the original invocation.</li>
 * 	<li><b>oncancel</b>: A callback invoked the asynchronous event subscription associated to the original invocation is cancelled</li>
 * </ul>
 * @return the unique request identifier which is also the handle to the subscription.
 */
function wsinvoke(command, options) {
	if(command==null || !$.isPlainObject(command)) throw "The command must be a valid object";
	if(command.svc==null || $.trim(command.svc)=="") throw "The command must specicy a valid service name";
	if(command.op==null || $.trim(command.op)=="") throw "The command must specicy a valid service operation name";
	if(command.t==null || $.trim(command.op)=="") command.op = "req";
	var returnValue = null;
	
	if(!$.isEmptyObject(options)) {
		var RID = _RID_FACTORY_++;
		command.rid = RID;
		console.debug("RID:[%s]", RID);
		returnValue = RID;
		var deferred = null, promise = null, done = false, wsInvokeTimeoutHandle = -1;		
		deferred = $.Deferred();
		promise = deferred.promise();
		var responseListener = function(json){
			if(json!=null && json.rerid==RID) {
				if(wsInvokeTimeoutHandle!=-1) {
					clearTimeout(wsInvokeTimeoutHandle);
				}
				deferred.resolve.apply(RID);
				console.debug("Received Response for RID [%s]-->[%o]", RID, json);
				if(options.onresponse!=null && $.isFunction(options.onresponse)) {
					options.onresponse(json);
				}
			}
		};
		promise.always(function(){
			$.websocket.removeMessageListener(responseListener);
			console.debug("Removing Message Listener for RID [%s]", RID);
		});
		$.websocket.addMessageListener(responseListener);
		console.debug("Added Message Listener for RID [%s]-->[%o]", RID, responseListener);
		wsInvokeTimeoutHandle = setTimeout(function(){
			$.websocket.removeMessageListener(responseListener);
			console.error("Request for RID [%s] Timed Out", RID);
			if(options.ontimeout!=null && $.isFunction(options.ontimeout)) {
				options.ontimeout();
			}			
			promise.fail();			
		}, options.timeout || _TIMEOUT_);
	}
	$.websocket.send(command);
	console.debug("Sent Message Listener for RID [%s]", RID);
	if(promise!=null) {
		return promise;
	}
}

/*
 * {"t":"req","svc":"catalog","op":"nq","args":{"name":"findLevelFoldersForAgent","p":{"level":1,"agentId":"1","parent":"/platform=JVM%"}},"rid":7}

 * 
 * 
 * Sample Response:
 * {"rerid":9,"t":"resp","msg":[]}
 
 * Sample Request:
{
 "args": {
  "es": "jmx",
  "esn": "service:jmx:local://DefaultDomain",
  "f": "org.helios.apmrouter.destination:service=H2TimeSeriesDestination,name=H2TimeSeriesDestination",
  "stf": ["apmrouter.h2timeseries.intervalroll.65"]
 },
 "op": "start",
 "rid": 11,
 "svc": "sub",
 "t": "req"
}

 */


/**
 * Initiates a MetricURI subscription
 * @param request A (mandatory) JSON subscription request
 * @param options:<ul>
 * 	<li><b>timeout</b>: The timeout in ms. on the request invocation confirm. (i.e. not a subscriber timeout) Default is 2000 ms.</li>
 * 	<li><b>oncomplete</b>: A callback invoked when the request is confirmed. If the metricuri subscription already exists, this will callback immediately.</li>
 * 	<li><b>ontimeout</b>: A callback invoked when the request times out</li>
 * 	<li><b>onevent</b>: A callback invoked when a subscription event is fired for this subscription</li>
 * </ul>
 * @return the unique request identifier which is also the handle to the subscription.
 */
function subscribe(request, options) {
	var muri = _SUBSCRIPTIONS_[request.args.uri];
	if(muri==null) {
		var newrid = _RID_FACTORY_++; 
		muri = new MetricSubscription(request.args.uri, newrid);
		_SUBSCRIPTIONS_[request.args.uri] = muri;
	}
}

/**
 * Validates the passed request to make sure it is a valid op call
 * @param request The request to validate
 */
function validate(request) {
	
}

var MetricSubscription = Object.subClass({
	init: function(metricuri, rid){
				this._metricuri = metricuri;				
				this._rid = rid;
				this._sub_count = 0;
	},
	MetricSubscription : function(metricuri, rid) {
		if (!(this instanceof arguments.callee)) {
			return new MetricSubscription(metricuri, rid);
		}
	}	
});


//.put("svc", "catalog")
//.put("op", "submetricuri")
//.putMapPair("args", "uri", metricURI.toASCIIString())




//$.apmr.svcOp = function(svc, op, args, callback) {
//	var req = {'t': 'req', 'svc' : svc, 'op' : op};
//	if(args!=null) {
//		req['args'] = args;
//	}
//	console.debug("Request Object:[%o]", req);
//	console.debug("JSON Request:[%s]", JSON.stringify(req));
//	return $.apmr.send(req, callback);
//},


//* Initiates a subscription.
//* @param subprops: <ul>
//* 	<li><b>sourcetype</b>: The event source type, e.g. "jmx". <b>Mandatory</b></li>
//* 	<li><b>sourcename</b>: The event source name, e.g. "service:jmx:local://DefaultDomain". <b>Mandatory</b></li>
//*  <li><b>op</b>: The operation to execute, i.e. "start" or "stop". <b>Mandatory</b></li>
//* 	<li><b>filter</b>: The event filter specifier, e.g. "org.helios.apmrouter.session:service=SharedChannelGroup". <b>Mandatory</b></li>
//* 	<li><b>xfilter</b>: An optional extended filter, e.g. and array of JMX notification types</li>
//*  <li><b>args</b>: Optional additional arguments in the form of properties.</li>
//* 	<li><b>subhandler</b>: The callback handler that will receive subscribed events</li>
//* 	<li><b>confirmhandler</b>: The callback handler that will receive the confirmation of a subscription operation or a timeout notification
//* which indicates the subscription request failed.</li>
//* </ul>
//*/
