function init_metricsBrowser_ui() {
	
	var metricBrowserTree = null;		
	var metricBrowserGrid = null;
	var tsFormatter = function(cellvalue, options, rowObject) {
		return new Date(Number(cellvalue)).toString();
	};
	var gridColumnModel = [
	               {name: "fqn", index: "fqn", hidden: true, key: true},
	               {name: "hostName", index: "hostName", width: 20, align:'left', sortable: true}, 
	               {name: "agentName", index: "agentName", width: 20, align:'left', sortable: true},
	               {name: "localName", index: "localName", width: 60, align:'left', sortable: true},
	               {name: "endTimestamp", index: "endTimestamp", width: 20, align:'left', sortable: true, formatter: tsFormatter}
	               //datefmt:'Y-m-d H:i:s', formatter:'date', formatoptions:{srcformat:'u', newformat:'Y-m-d H:i:s'} 
	           ];
	var gridColumnNames = ["FQN", "Host", "Agent", "Metric", "End Time"];
	
	$("#metricBrowserGrid").jqGrid({
	    height: 250,
	    width: 1400,			
	    altRows: true,
	    altclass:'myAltRowClass',
		colNames: gridColumnNames, 
		colModel: gridColumnModel,
		rowNum:10,
	    rowList:[10,20,30],
	    pager: $('#gridpager'), 
	    onFeedData: function() {
			var data = $.makeArray(arguments);
			var event = data.shift();
			//console.info("metricBrowserGrid onFeedData [%o]", data);
			$.each(data, function(index, row) {
				var rowId = row.fqn;
				var data = $("#metricBrowserGrid").jqGrid('getRowData' , rowId );
				if($.isEmptyObject(data)) {
					$("#metricBrowserGrid").jqGrid('addRowData',row.fqn, row);
					console.info("ADD ROW [%s]", row.fqn);
				} else {
					$("#metricBrowserGrid").jqGrid('setRowData',row.fqn, row);
					console.info("UPDATE ROW [%s]", row.fqn);
				}
			});
		}
	});
	$('table#metricBrowserGrid.ui-jqgrid-btable')[0]['onFeedData'] = $("#metricBrowserGrid").getGridParam('onFeedData');
	//$("#metricBrowserGridContainer").draggable().resizable({
	//	alsoResize: $("#metricBrowserGridContainer").add('#gridpager').children()
	//});
	$("#metricBrowserGridContainer").draggable();
	$("#metricBrowserGrid").gridResize();
	    
	$('#gridMaskInput').keydown(function (e) {
		var target = this;
		var subscribedColor = '#FFF68F'; 
		if (e.keyCode == 13) {
			$("#metricBrowserGrid").clearGridData();
			var expr = $("#gridMaskInput").val();
			$.cookie('metric_browser.gridMaskInput', expr, { expires: 365 });
			// Retrieve Latest
			console.info("Enter [%s]", expr); 
			$.helios.lastMetricSearch(expr, function(data) {
				if(data!=null && data.length>0) {						
					$.each(data, function(i, v) {
						$("#metricBrowserGrid").jqGrid('addRowData',v.fqn, v);
					});
				}
				if(e.ctrlKey) {
					// Retrieve Latest And Subscribe					
					var subExpr = 'helios.metrictree.' + expr.replace(/\//g, '.');
					console.info("Ctrl-Enter [%s]", subExpr);
					$("#metricBrowserGrid").heliosFeed("metric-feed", {'routerKey': subExpr});
					$('#gridMaskInput').css('background', subscribedColor);					
				}				
			});			
		}
	});
	$("#gridMaskInput").val($.cookie('metric_browser.gridMaskInput') || "");	
	$("#metricBrowserGrid").show();
	if($("#gridMaskInput").val().trim().length>0) {
		$("#gridMaskInput").trigger($.Event("keydown", {keyCode: 13}));
	}
	/*
	$("#metricBrowserTreeExtruder").buildMbExtruder({
        positionFixed:true,
        width:350,
        sensibility:800,
        position:"left", // left, right, bottom
        extruderOpacity:0.7,
        flapDim:50,
        textOrientation:"bt", // or "tb" (top-bottom or bottom-top)
        onExtOpen:function(){
		},
        onExtContentLoad:function(){
        },
        onExtClose:function(){
        	
        },
        hidePanelsOnClose:true,
        autoCloseTime:0, // 0=never
        slideTimer:300
    });
	$('#metricBrowserTreeExtruder').show();
	$('#metricBrowserTreeExtruder div.content').css('overflow' , 'scroll');
	*/    				    
	$("#metricBrowserTree").dynatree({
		onLazyRead: function(node){
			$.helios.metricPath(node.data.key, function(data) {
				node.addChild(data);
			});
        },
	    onFeedData: function() {
			var data = $.makeArray(arguments);
			var event = data.shift();
			console.info("metricBrowserTree onFeedData [%o]", data);
			$.each(data, function(index, row) {
				var nodeKey = row.userData[1];
				var exists = metricBrowserTree.getNodeByKey(nodeKey)!=null;
				console.info("Node Key [%s] Exists [%s]", nodeKey, exists); 
				if(!exists) {
					
				}
			});
		}        
    });  
	$('#metricBrowserTree')[0]['onFeedData'] = $("#metricBrowserTree").dynatree("getTree").options.onFeedData;
	metricBrowserTree = $("#metricBrowserTree").dynatree("getTree");
	$.helios.metricPath("/", function(data) {
		metricBrowserTree.getRoot().addChild(data);
	});	
	if($.helios.get("/session/subscriberObjectName")==null) {
		$.helios.subscribe("/session/subscriberObjectName", function(ev, data){
			if(data!=null) {
				$("#metricBrowserTree").heliosFeed("jmx-feed", {'routerKey': "org.helios.server.ot.cache:name=CacheListener,type=CacheEventManager", 'notification' : 'org.helios.cache.*.metricNameCache'});				
			} 
		});
	} else {
		$("#metricBrowserTree").heliosFeed("jmx-feed", {'routerKey': "org.helios.server.ot.cache:name=CacheListener,type=CacheEventManager", 'notification' : 'org.helios.cache.*.metricNameCache'});
	}
	
	
	
	
}