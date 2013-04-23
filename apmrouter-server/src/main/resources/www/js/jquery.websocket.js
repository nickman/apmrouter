



;(function ( $, window, document, undefined ) {
	var settings = {};
	function onopen() {				
		console.info("WS: Connected to [%s]", settings.wsuri);
		//$.event.trigger('websocket-connected');
		settings.state = 'connected';
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
		settings.state = 'disconnected';
		//$.event.trigger('websocket-disconnected');
	}
	
	function onerror(e) {
		console.info("WS: Error [%o] [%s] : [%s]", e, e.name, e.message);
	}

	function doConnect() {
		
	}
	
	var privatePattern = new RegExp("^_.*");
	
	var methods = {
		getState : function() {
			return $.fn.websocket.state.state;
		},
		isConnectingOrConnected : function() {
			return settings.state == 'connecting' || settings.state == 'connected';  
		},
		isReconnectScheduled : function() {
			return settings.reconnectTimeoutHandle != -1;
		}	
	}
	
	
	
	$.websocket = function(arg, args) {
		if(arg==null) throw "No first arg passed";
		if(privatePattern.test(arg)) throw "Cannot invoke private signatures [" + arg + "]";
		var f = methods[arg];		
		if(!$.isFunction(f)) throw "Not a jQuery.websocket method [" + arg + "]";
		return f.apply($.fn.websocket, $.isArray(args) ? args : []);			
	}
    
	$.fn.websocket = function(arg, args) {
		if(arg==null) throw "No first arg passed";
		if($.isPlainObject(arg)) {
			var options = arg;
			if(!$.fn.websocket.state) {
				if(!options['wsuri']) {
					throw "No WebSocket URI (wsuri) defined and state undefined";
				} 
				$.fn.websocket.state = {};
				var wsuri = $.trim(options.wsuri);
				delete options.wsuri;
				_init(wsuri , options);
			} else {
				if(options.wsuri) {
					throw "WebSocket URI (wsuri) already defined. Please reset to change URI";
				}
			}
		} else {
		}
		/**
		 * Initializes the plugin state
		 * @param options The init options
		 */
		function _init(wsuri, options) {		
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
	    	Object.observe(settings, function(changes){
	    		$.each(changes, function(index, change){
	    			if(change.name == 'state' && change.type == 'updated') {
	    				var newState = change.object.state;
	    				$.event.trigger('websocket-' + newState);
	    			}
	    		});
	    	});
		}
		
		
		function _setConnectTimeout() {
			
		}
		
		function _connect(callback) {
			// send connecting event
			$.apmr.config.connecting = true;		
			var cli = this;
			$.apmr.config.connectTimeoutHandle = setTimeout(function(){
				$.apmr.onConnectionTimeout();
				if(callback!=null) callback();
			},$.apmr.config.connectionTimeout);
			setTimeout(function(){
				cli.config.ws = new WebSocket(cli.config.wsUrl);
				cli.config.ws.c = cli;
				cli.config.ws.onopen = $.apmr.onOpen;
				cli.config.ws.onerror = $.apmr.onError;
				cli.config.ws.onclose = $.apmr.onClose;
				cli.config.ws.onmessage = $.apmr.onMessage;
			},1);
			console.info("Connecting.....");
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
		}
		
		
		
    }
	
})( jQuery, window, document );

