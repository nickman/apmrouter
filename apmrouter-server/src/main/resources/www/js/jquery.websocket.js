



;(function ( $, window, document, undefined ) {
	var settings = {};
	function onopen() {				
		console.info("WS: Connected to [%s]", settings.wsuri);
		$.event.trigger('websocket-connected');
	}
	
	function onmessage(message) {
		var json = JSON.parse(message.data);
		console.info("WS: MessageEvent: [%o]:[%o]", message, json);
		if(json.sessionid) {
			settings.sessionid = json.sessionid;
			$.event.trigger('websocket-sessionid', [json.sessionid]);
		}
	}
	
	function onclose(e) {		
		console.info("WS: Closed");
		$.event.trigger('websocket-disconnected');
	}
	
	function onerror(e) {
		
	}

	function doConnect() {
		
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
	    	console.debug("Initializing jquery.websocket [%s]", wsuri);
			$.fn.websocket.state = $.extend({
				connectTimeout: 2000,
				reconnectPeriod: 5000
	    	},options||{});

			
	    	settings = $.fn.websocket.state;
	    	settings.wsuri = wsuri;
	    	settings.connectTimeoutHandle = -1;
	    	settings.reconnectTimeoutHandle = -1;
	    	settings.state = 'disconnected';

	    	settings.ws = new WebSocket(settings.wsuri);
	    	settings.ws.onopen = onopen;
	    	settings.ws.onclose = onclose;
	    	settings.ws.onerror = onerror;
	    	settings.ws.onmessage = onmessage;		    		
		}
		
		function isConnectingOrConnected() {
			return settings.state == 'connecting' || settings.state == 'connected';  
		}
		
		function isReconnectScheduled() {
			return settings.reconnectTimeoutHandle != -1;
		}
		
		function startReconnectLoop() {
			if(isConnectingOrConnected() || isReconnectScheduled()) return;
			settings.reconnectTimeoutHandle = setTimeout(function(){
				settings.reconnectTimeoutHandle = -1;
				$.event.trigger('websocket-connecting',[]);
				$.apmr.connect(function(){ // callback called when reconnect times out
					if($.apmr.isConnectingOrConnected()) return;
					if($.apmr.config.ws) {
						$.apmr.config.ws.close();
					}
					//$.apmr.config.reconnectTimeoutHandle = $.apmr.startReconnectLoop();
				});
			}, $.apmr.config.reconnectPauseTime);
		},
		
		
		
    }
	
})( jQuery, window, document );

