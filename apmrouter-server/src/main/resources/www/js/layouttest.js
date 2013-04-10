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
    	addThemeSwitcher();
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
    		load: function( event, ui ) {
        		console.info("Loaded tab event [%o]", event);
        		console.info("Loaded tab ui [%o]", ui);
        	}, 
    		show: function() {
    			topLayouts.metricLayout = $('#metricLayout').layout();
    			topLayouts.metricDisplayLayout = $('#metricDisplayLayout').layout({    				
    				//center__onresize : "metricDisplayLayout.resizeAll",
    				center__onresize : function() {
    					console.info("Center OnResize");
    					//return true;    					
    				},
    				center__onresize_end : function() {
    					//metricDisplayLayout.resizeAll();
    					//$('#metricDisplayTable').height($('#metricDisplayGrid').height());
    					//$('#metricDisplayTable').width($('#metricDisplayGrid').width());    					
    					console.info("Center OnResize End");
    					//return true;
    				},
    				east__onresize : function() {
    					metricDisplayLayout.resizeAll();
    					$('#metricDisplayTable').height($('#metricDisplayGrid').height());
    					$('#metricDisplayTable').width($('#metricDisplayGrid').width());
    					metricGrid.fnDraw();
    					console.info("East OnResize");
    					//return true;
    				},
    				east__onresize_end : resizeMetricGrid,
    				
					north__onresize : function() {
						metricDisplayLayout.resizeAll();
						console.info("North OnResize");
						//return true;
					},
					north__onresize_end : function() {
						metricDisplayLayout.resizeAll();
						console.info("North OnResize End");
						//return true;
					},
					
    				south__onresize : function() {
    					metricDisplayLayout.resizeAll();
    					console.info("South OnResize");
    					//return true;
    				},
    				south__onresize_end : function() {
    					//metricDisplayLayout.resizeAll();
    					console.info("South OnResize End");
    					//return true;    					
    				},
    			
    				center__spacing_closed: 0,
    				center__spacing_open: 0,
    				//east__spacing_closed: 0,
    				//east__spacing_open: 0,
    				
    				east__size: '100%'
    				
    					
    			});
    			topLayouts.metricDisplayLayout.sizePane('south', '50%');
    			
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
    			resizeMetricGrid();
    			$('#metricDisplayTable_wrapper').css('height', '100%');
    			$('#metricDisplayTable_wrapper').css('width', '100%');
    			$('#metricDisplayGrid div.fg-toolbar').css('padding', '0px')
    			var defaultColor = $('#metricSearchEntry').css('background');
    			$('#metricSearchButton').click(function() {
    				console.info("CLICK!");
    				execSearch();
    			});
    			$('#metricSearchEntry').keydown(function (e) {    				
    				if (e.keyCode == 13) {
						execSearch();
//    					
//    					if(e.ctrlKey) {
//    						console.info("Ctrl-Enter [%s]", expr);
//    						$('#metricSearchEntry').css('background', subscribedColor);
//    					} else {
//    						console.info("Enter [%s]", expr);
//    						$('#metricSearchEntry').css('background', defaultColor);
//    					}
    				}
    				
    			});
//    			$('#metricDisplayTable tr').click( function() {
//    		        $(this).toggleClass('row_selected');
//    		    });    			
    			$('#metricDisplayTable tbody tr').live('click', function (data) {
    				if($(this).hasClass('selectedGridMetric')) {
    					// UN-SELECTING
    					if($(this).hasClass('selectedGridMetricOdd')) {
    						$(this).toggleClass('selectedGridMetricOdd'); 
    						$(this).toggleClass('odd');
    					} else {
    						$(this).toggleClass('selectedGridMetricEven'); 
    						$(this).toggleClass('even');    						
    					}
    					$(this).toggleClass('selectedGridMetric')
    				} else {
    					// SELECTING
    					if($(this).hasClass('odd')) {
    						$(this).toggleClass('selectedGridMetricOdd'); 
    						$(this).toggleClass('odd');
    					} else {
    						$(this).toggleClass('selectedGridMetricEven'); 
    						$(this).toggleClass('even');    						
    					}
    					$(this).toggleClass('selectedGridMetric');
    					ChartModel.find($(this).data('metric').id, function(model){model.renderChart({'auto' : true});});
    					
    				}
    				$(this).toggleClass('selectedGridMetric');    				
    			});    			
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
