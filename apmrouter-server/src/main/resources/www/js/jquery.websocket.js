



;(function ( $, window, document, undefined ) {
	var settings = {};
	function onopen() {		
		$('#connectedLight').html(settings.connected_img);
		console.info("WS: Connected to [%s]---[%s]", settings.wsuri, settings.connected_img);
	}
	
	function onmessage(message) {
		
	}
	
	function onclose(e) {
		
	}
	
	function onerror(e) {
		
	}

	
	
    
	$.fn.websocket = function(options) {
		if(!$.fn.websocket.state) {
			if(!options['wsuri']) {
				throw "No WebSocket URI (wsuri) defined and state undefined";
			} 
			$.fn.websocket.state = {};
			var wsuri = $.trim(options.wsuri);
			delete options.wsuri;
			init(wsuri , options);
		} else {
			if(options.wsuri) {
				throw "WebSocket URI (wsuri) already defined. Please reset to change URI";
			}
		}
		/**
		 * Initializes the plugin state
		 * @param options The init options
		 */
		function init(wsuri, options) {		
	    	$.fn.websocket.state = $.extend({
				connectTimeout: 2000,
				reconnectPeriod: 5000
	    	},options||{});
	    	
	    	settings = $.fn.websocket.state;
	    	settings.wsuri = wsuri;
			settings.connected_img = getContent("img/svg/connected-status.svg");
			settings.disconnected_img = getContent("img/svg/disconnected-status.svg");
			settings.connecting_img = getContent("img/svg/connecting-status.svg");	    	
	    	settings.ws = new WebSocket(settings.wsuri);
	    	settings.ws.onopen = onopen;
	    	settings.ws.onclose = onclose;
	    	settings.ws.onerror = onerror;
	    	settings.ws.onmessage = onmessage;
	    	
		}
		
		
    }
	
})( jQuery, window, document );


function getContent(url) {
	$.get(url).then(function(data){
		return data;
	});
	
	
}