var topLayouts = {};
var metricGrid = null;
var metricGridColumnModel = [
                 			{ "sTitle": "MetricId", "bVisible":    false },                             
                			{ "sTitle": "Domain" },
                			{ "sTitle": "Host" },                			
                			{ "sTitle": "Agent" },
                			{ "sTitle": "Namespace" },
                			{ "sTitle": "Name" },
                			{ "sTitle": "Type" }
                ];

function initMain() {
    $(document).ready(function() {
    	try { addThemeSwitcher(); } catch (e) {}
    	topLayouts.main = $("#top-body").layout({ 
    			applyDefaultStyles: false,
    			west: {
    				initClosed: true
    			},
    			east: {
    				initClosed: true
    			}	        			
    		}
    	); 
    	//addTreeTooltipListener();
    	$('#top-toolbar').children().css('vertical-align', 'middle');
    	$('#maintabs').tabs({
    		show: function(event, ui) {    			
    			var tabName = $(ui.tab).attr('href').replace('#', '').replace('.html', '');
    			console.info("Showing tab ui [%s]", tabName);
    			var methodName = 'init' + tabName;
    			switch(tabName) {
    				case 'metricBrowserTab':
    					$.getScript('metricBrowserTab.js', function(){
    						initmetricBrowserTab();
    					});    					
    					break;
    				case 'jmxBrowserTab':
    					$.getScript('jmxBrowserTab.js', function(){
    						initjmxBrowserTab();
    					});    					
    					break;    				    					
    			}
    		}
    	});
//    	$( "#metricSearchEntry" ).autocomplete({
//    	    source: function( request, response ) {
//    	            var matcher = new RegExp( "^" + $.ui.autocomplete.escapeRegex( request.term ), "i" );
//    	            response( $.grep( tags, function( item ){
//    	                return matcher.test( item );
//    	            }) );
//    	        }
//    	});
    	
    });
    
}

function resizeMetricGrid() {
	var oSettings = metricGrid.fnSettings();
	var barHeights = 0;
	$('#metricDisplayGrid div.fg-toolbar').each(function(k,v){
		barHeights += $(v).height()+2;
	});
	console.info("Toolbar:%s", barHeights);
	$('#metricDisplayGrid thead').each(function(k,v){
		barHeights += $(v).height()+2;
	});
	oSettings.oScroll.sY = $('#metricDisplayGrid').height()-(barHeights);
	metricGrid.fnDraw();
	return true;    					
	
}

function onSelectedMetricFolder(uri, callback) {
	console.info("Processing URI [%s]", uri);
	$('#metricSearchEntry').val(uri);
	var metricData = null;
	$.apmr.metricUri(uri, function(data) {
		metricData = data;
		metricGrid.fnClearTable();
		console.info("Refreshing Grid with [%s] Records. First Record:[%o]", data.msg.length, data.msg[0]);
		$.each(data.msg, function(index, metric){
			metricGrid.fnAddData([
			   metric.id,
			   metric.ag.host.domain,
			   metric.ag.host.name,
			   metric.ag.name,
			   metric.ns,
			   metric.name,
			   metric.type.typeName
			]);			
		});
		$('#metricDisplayTable tr').each(function(k,v){
			var rowId = k-1;
			if(rowId>=0) {
				$(v).data('metric', metricData.msg[rowId]);
				$(v).children('td').each(function(colId, col){					
					$(col).attr('id', 'row-' + rowId + '-col-' + colId)
					.attr('row', rowId)
					.attr('col', colId);
					//.data('metricId', metricData.msg[rowId].ns); //data.msg[rowId-1].id
				});				
			}
		});
		if(callback!=null) {
			callback.apply();
		}
	});
}

function execSearch() {	
	$("body").css("cursor", "progress");
	var timeout = setTimeout(function(){
		$("#metricLayout").css("cursor", "default");
	}, 5000);
	metricGrid.fnClearTable();
	var expr = $("#metricSearchEntry").val();
	$.cookie('metric_browser.gridMaskInput', expr, { expires: 365 });
	onSelectedMetricFolder(expr, function(){
		clearTimeout(timeout);
		$("body").css("cursor", "default");
	});	
}

function addTreeTooltipListener() {
	$('#metricTree ins.jstree-icon').livequery(
		// registers a tooltip
		function(){			
			var target = $(this);
			var path = metricTree.get_path(target.parent('li[rel]').first());
			//target = target.parent('a[href="#"]').first();
			if(path!=false) {
				path.shift();
				if(path.length>0) {					
					var txt = path.join("/");
//					var iconSrc = target.children('a[href="#"]').first().children('ins.jstree-icon').css('background-image');
//					iconSrc = iconSrc.replace('url(', '').replace(')','');
					console.info("Registering tooltip for [%s]", txt);
					target.tooltip({
//						extraClass: 'ui-tooltip',
						bodyHandler: function() {
							var t = target;
							var iconSrc = t.parent().children('a[href="#"]').first().children('ins.jstree-icon').css('background-image');
							iconSrc = iconSrc.replace('url(', '').replace(')','');
							
						     return $("<div><p style='vertical-align: middle; display: inline-block;'>" + txt + "</p>&nbsp;<img style='vertical-align: middle; display: inline-block;' src='" + iconSrc + "'></img></div>").css('background-image', iconSrc).css('display', 'block').css('display', 'inline-block').css('vertical-align', 'middle');
//							return $("<div>" + txt + "</div>");
						}
					});					
				}
			}
			
		}, 
		// noop ?
		function(){
		}
	);
}

