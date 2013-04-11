function initmetricBrowserTab() {		
	if($('#metricBrowserTabInited').length==1) {
		console.debug("########## metricBrowserTab already inited ##########");
		return;
	}
	console.info("########## initing metricBrowserTab ##########");
	var initedFlag = $('<div id="metricBrowserTabInited"></div>').hide();
	$('#metricBrowserTab').append(initedFlag);	
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