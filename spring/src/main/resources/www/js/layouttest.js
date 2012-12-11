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
    			topLayouts.metricDisplayLayout = $('#metricDisplayLayout').layout();
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
    			   	colNames:['Inv No','Date', 'Client', 'Amount','Tax','Total','Notes'],
    			   	colModel:[
    			   		{name:'id',index:'id', width:55},
    			   		{name:'invdate',index:'invdate', width:90},
    			   		{name:'name',index:'name asc, invdate', width:100},
    			   		{name:'amount',index:'amount', width:80, align:"right"},
    			   		{name:'tax',index:'tax', width:80, align:"right"},		
    			   		{name:'total',index:'total', width:80,align:"right"},		
    			   		{name:'note',index:'note', width:150, sortable:false}		
    			   	],
    			   	rowNum:10,
    			   	rowList:[10,20,30],
    			   	pager: '#metricDisplayPager',
    			   	sortname: 'id',
    			    viewrecords: true,
    			    sortorder: "desc"
    			});    			
    			$("#metricDisplayTable").css('float', 'left');
    			$("#metricDisplayTable").css('height', '100%');
    			$("#metricDisplayTable").css('width', '100%');
    			
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
