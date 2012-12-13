var topLayouts = {};
var metricDisplayGrid = null;
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
    				east__onresize : function() {
    					metricDisplayLayout.resizeAll();
    	    			metricDisplayGrid.setGridHeight($('#metricDisplayGrid').height()-25, true);
    	    			metricDisplayGrid.setGridWidth($('#metricDisplayGrid').width()-10, true);
    					
    					console.info("East Resized");
    					return true;    					
    				},
					north__onresize : function() {
						metricDisplayLayout.resizeAll();
						console.info("North Resized");
					},
    				south__onresize : function() {
    					metricDisplayLayout.resizeAll();
    					console.info("South Resized");
    				},
    				south__onresize_end : function() {},
    				east__onresize_end : function() {
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
    			metricDisplayGrid = $("#metricDisplayTable").jqGrid({    			   	
    				datatype: "json",
    			   	//colNames:['Inv No','Date', 'Client', 'Amount','Tax','Total','Notes'],
    			   	colNames:['Time','Domain', 'Host', 'Agent', 'Namespace', 'Name', 'Min', 'Max', 'Avg', 'Cnt'],
    			   	colModel:[
    			   		{name:'Time',index:'time', width:55},
    			   		{name:'Domain',index:'domain', width:90},
    			   		{name:'Host',index:'host', width:100},
    			   		{name:'Agent',index:'agent', width:80, align:"right"},
    			   		{name:'NS',index:'ns', width:80, align:"right"},		
    			   		{name:'Name',index:'name', width:80,align:"right"},		
    			   		{name:'Min',index:'min', width:15},		
    			   		{name:'Max',index:'max', width:15},
    			   		{name:'Avg',index:'avg', width:15},
    			   		{name:'Cnt',index:'cnt', width:15}
    			   	],
    			   	rowNum:1,
    			   	//rowList:[10,20,30],
    			   	//pager: '#metricDisplayPager',
    			   	sortname: 'id',
    			    viewrecords: true,
    			    sortorder: "desc"
    			});    			
    			$('#metricDisplayGrid')
    			metricDisplayGrid.setGridHeight($('#metricDisplayGrid').height()-25, true);
    			metricDisplayGrid.setGridWidth($('#metricDisplayGrid').width()-10, true);
//    			$('#metricDisplayLayout').resize( function(e){
//    				console.info("Resized jqGrid:%s,%s,%o",$('#metricDisplayLayout').width(), $('#metricDisplayLayout').height(), e);
//    				metricDisplayGrid.setGridHeight($('#metricDisplayGrid').height()-2, true);
//        			metricDisplayGrid.setGridWidth($('#metricDisplayGrid').width()-2, true);
//        			
//    			});
    			/*
    			metricDisplayGrid.setGridWidth(parseInt($('#metricDisplayGrid').width())-10, true);
    			$('window').bind('resize', function(e){
    				metricDisplayGrid.setGridWidth(parseInt($('#metricDisplayGrid').width())-10, true);
    				console.info("Resized jqGrid");
    			});
    			*/
    			
    			//$('#tab0').height($('#tab0').parent().parent().height() - parseInt($('#tab0').parent().parent().children('ul').height()));
    			
    			// //$("#metricBrowserGrid").jqGrid('addRowData',v.fqn, v);
    			var defaultColor = $('#metricSearchEntry').css('background');
    			$('#metricSearchEntry').keydown(function (e) {
    				var target = this;
    				var subscribedColor = '#FFF68F'; 
    				
    				if (e.keyCode == 13) {
    					metricDisplayGrid.clearGridData();
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
    
    function handleMetricSearchAutoComplete(request, response) {
    	
    }
}
