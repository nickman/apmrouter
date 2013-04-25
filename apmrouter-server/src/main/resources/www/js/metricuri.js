		var stateCache = {};
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
	
	$( "#uri" ).autocomplete({
		source: "/mauto",
		minLength: 0,
		disabled: false,
		response: function( event, ui ) {
			console.info("Response:  event: %o   ui:%o", event, ui);
		},	
		select: function( event, ui ) {
	        console.info( ui.item ?
	          "Selected: " + ui.item.value + " aka " + ui.item.id :
	          "Nothing selected, input was " + this.value );
	    }		
	});
	$("#uri").keypress(function(event) {
		   if(event.keyCode==10) {
			   if(event.ctrlKey) {
				   $( "#uri" ).autocomplete( "search", "" );
			   }
		   }
	});

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