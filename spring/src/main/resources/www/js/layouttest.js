var topLayouts = {};
var metricGrid = null;
var metricGridColumnModel = [
                 			{ "sTitle": "MetricId", "bVisible":    false },                             
                			{ "sTitle": "Domain" },
                			{ "sTitle": "Host" },                			
                			{ "sTitle": "Agent" },
                			{ "sTitle": "Namespace" },
                			{ "sTitle": "Name" }
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
    	$('#top-toolbar').children().css('vertical-align', 'middle');
    	$('#maintabs').tabs({
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
    			$('#metricSearchButton').button().bind('click', function(e) {
    				console.debug("Searching......");
    				$("#metricLayout").css("cursor", "progress");
    				setTimeout(function(){
    					$("#metricLayout").css("cursor", "default");
    				}, 3000);
    			});
    			
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
    			$('#metricSearchEntry').keydown(function (e) {
    				var target = this;
    				var subscribedColor = '#FFF68F'; 
    				
    				if (e.keyCode == 13) {
    					metricGrid.clearGridData();
    					var expr = $("#metricSearchEntry").val();
    					$.cookie('metric_browser.gridMaskInput', expr, { expires: 365 });
    					// Retrieve Latest
    					
    					if(e.ctrlKey) {
    						console.info("Ctrl-Enter [%s]", expr);
    						$('#metricSearchEntry').css('background', subscribedColor);
    					} else {
    						console.info("Enter [%s]", expr);
    						$('#metricSearchEntry').css('background', defaultColor);
    					}
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
    					$(this).toggleClass('selectedGridMetric')
    					
    				}
    				$(this).toggleClass('selectedGridMetric');
    				console.info("===== Clicked Row  ===== [%o]", data.srcElement);
    			});    			
    		}
    	});
    	$( "#metricSearchEntry" ).autocomplete({
    	    source: function( request, response ) {
    	            var matcher = new RegExp( "^" + $.ui.autocomplete.escapeRegex( request.term ), "i" );
    	            response( $.grep( tags, function( item ){
    	                return matcher.test( item );
    	            }) );
    	        }
    	});
    	
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

function onSelectedMetricFolder(uri) {
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
			   metric.name
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
			} else {
				console.info("Skipping row [%s]", k);
			}
		});
//		console.info("============== DATA ==============");
//		console.dir(data);
	});
}

/*
var metricGridColumnModel = [
	{ "sTitle": "MetricId", "bVisible":    false },                             
{ "sTitle": "Domain" },
{ "sTitle": "Host" },                			
{ "sTitle": "Agent" },
{ "sTitle": "Namespace" },
{ "sTitle": "Name" }
];
*/