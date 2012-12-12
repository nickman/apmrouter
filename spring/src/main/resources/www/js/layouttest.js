var topLayouts = {};
var metricDisplayGrid = null;
function initMain() {
    $(document).ready(function() {
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
    				center__onresize : "metricDisplayLayout.resizeAll"
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
    		}
    	});
    	
    });
}
