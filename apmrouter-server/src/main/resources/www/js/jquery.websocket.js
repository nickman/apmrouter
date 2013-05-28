


;(function ( $, window, document, undefined ) {
	var settings = {};
	// Arrays of listeners keyed by the message type key extracted from each message
	var messageSubscribers = {};
	// Default handler for messages of an undetermined type
	var unknownMessageType = function(data){
		console.error("====================================================================");
		console.error("Message received of unknown type:%o", data);
		console.error("====================================================================");
	};
	// Default handler for messages of a recognized type, but with no listeners
	var unhandledMessage = function(key, data){
		console.warn("====================================================================");
		console.warn("Message received of type [%s] had no handlers:%o", key, data);
		console.warn("====================================================================");
	};
	
	// The websocket possible states
	var states = ['connected', 'disconnected', 'connecting'];
	function onopen() {				
		console.info("WS: Connected to [%s]", settings.wsuri);
	}
	
	function getListenersFor(typeKey, prefix) {
		var listeners = [];
		$.each([typeKey, prefix + "=" + prefix], function(index, value){
			var _found = messageSubscribers[value];
			if(_found!=null && $.isArray(_found)) {
				$.each(_found, function(i, v) {
					listeners.push(v);
				});
			}
		});
		return listeners;
	}
	
	function onmessage(message) {
		var json = JSON.parse(message);
		console.debug("WS: MessageEvent: [%o]",json);
		/*
		 * Get the message type key, which could be:
		 * 	"rerid": The id of the request that the response is generated for
		 * 	"subkey": The subscription request identifier
		 * 	"bcastkey": The key this sub response should be broadcast with for cases where subscribers do not directly register but bind to named events with this name. 
		 */
		var typeKeys = ["rerid", "subkey", "bcastkey"];
		var compoundKey = [];
		var keysWithHandlers = 0;
		var foundMessageKeys = 0;
		$.each(typeKeys, function(index, value){
			var typeKey = json[value];
			if(typeKey!=null) {
				compoundKey.push(typeKey);
				foundMessageKeys++;
				var listeners = getListenersFor(typeKey);
				if(listeners.length>0) {
					keysWithHandlers++;
					$.each(listeners, function(index, listener){
						listener.apply(json);
					});
				}
			}
		});
		if(foundMessageKeys==0) {
			unknownMessageType(json);
		} else {
			if(keysWithHandlers==0) {
				unhandledMessage(compoundKey.join('|'), json);
			}
		}
	}
	
	function onclose(e) {		
		console.info("WS: Closed [%s]", settings.sessionid==null ? "<>" : settings.sessionid);
		delete settings.sessionid;
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
		},
		/**
		 * Internal add message listener function. No Op if either parameter is null
		 * @param typeKey The type key the listener responds to
		 * @param listener The listener
		 */
		_addMessageListener : function(typeKey, listener) {
			if(typeKey==null || listener==null) return;
			var listeners = messageSubscribers[typeKey]|[];
			listeners.push(listener);
			messageSubscribers[typeKey] = listeners;
		},
		/**
		 * Internal remove message listener function. 
		 * @param typeKey The type key the listener might be registered under. Ignored if null.
		 * @param listener The listener to remove. No Op if null.
		 */
		_removeMessageListener : function(typeKey, listener) {
			if(typeKey==null) {
				if(listener==null) return;
				$.each(messageSubscribers, function(key, listeners){
					var index = listeners.indexOf(listener);
					if(index>-1) listeners.splice(index, 1);
				});
			} else {
				delete messageSubscribers[typeKey];
			}
			var listeners = messageSubscribers[typeKey]|[];
			listeners.push(listener);
			messageSubscribers[typeKey] = listeners;
		},
		
		addMessageListener : function(listeners) {
			if(listeners==null) throw "Attempted to register null listener";
			if(!$.isArray(listeners)) {
				
			} else if(!$.isPlainObject(listeners)) {
				
			} else if(!$.isFunction(listeners)) {
				if(listeners['typeKey']!=null) {
					_addMessageListener(listeners.typeKey, listeners);
				}
			}
			
			
			if(listeners!=null) {
				if(!$.isArray(listeners)) {
					listeners = [listeners];
				} 
				var added = 0;
				$.each(listeners, function(index, listener){
					if(listener!=null && ($.isFunction(listener) || $.isFunction(listener.onMessage)) && $.inArray(listener, messageSubscribers)==-1) {					
						messageSubscribers.push(listener);
						added++;
					}
				});
				console.debug("Registered [%s] New Message Listeners for a total of [%s]", added, messageSubscribers.length);
			}
		},
		removeMessageListener : function(listeners) {
			if(listeners!=null) {
				if(!$.isArray(listeners)) {
					listeners = [listeners];
				} 
				var removed = 0;
				$.each(listeners, function(index, listener){
					var indexOfR = $.inArray(listener, messageSubscribers);
					if(listener!=null && indexOfR!=-1) {					
						messageSubscribers.splice(indexOfR, 1);
						removed++;
					}
				});
				console.debug("Removed [%s] Message Listeners leaving a total of [%s]", removed, messageSubscribers.length);
			}
		},
		
		send : function(data) {
			var deferred = $.Deferred();
			if(settings.state != 'connected') {
				deferred.reject("Cannot send websocket data. We're not connected !");
			} else {
				var result = null;
				if(data==null) {
					deferred.reject(false);
				} else if(typeof(data)=="object") {
					var _data = JSON.stringify(data);					
					result = settings.ws.send(_data); 
					console.debug("Sent Data[%s]", _data);
				} else if(typeof(data)=="string") {
					result = settings.ws.send(data);
					console.debug("Sent [%s]", data);
				}
				if(result!=null) {
					if(result) deferred.resolve(result);
					else deferred.reject(result);
				} else {
					deferred.reject("I don't know how to handle this data type [" + typeof(data) + "]");
				}
				return deferred.promise();
			}
		},
		close:  function() {
			if(settings.ws!=null) {
				try {
					settings.ws.close();
				} catch (e) {}
			}
		}
	}
	
	
	$.websocket = function(arg, args) {
		if(arg==null) throw "No first arg passed";
		if(privatePattern.test(arg)) throw "Cannot invoke private signatures [" + arg + "]";
		var f = methods[arg];		
		if(!$.isFunction(f)) throw "Not a jQuery.websocket method [" + arg + "]";
		return f.apply($.fn.websocket, args==null ? [] : $.isArray(args) ? args : [args]);			
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
	    	_connect();
		}

		
		function _setState(state, args) {
			if(state==null || states.indexOf(state)==-1) throw "The value [" + state + "] is not a valid state";
			settings.state = state;
			$.event.trigger('websocket-' + state, args==null ? [] : $.isArray(args) ? args : [args]);
		}
		
		
		function _setConnectTimeout() {
			_cancelConnectTimeout();
			settings.connectTimeoutHandle = setTimeout(function(){
				if(settings.ws!=null) settings.ws.close();
			}, settings.connectTimeout);
			console.debug("Connect timeout set to [%s] on handle [%s]", settings.connectTimeout, settings.connectTimeoutHandle);
		}
		
		function _cancelConnectTimeout() {
			if(settings.connectTimeoutHandle!=-1) {
				clearTimeout(settings.connectTimeoutHandle);
				console.debug("Cancelled connect timeout on handle [%s]", settings.connectTimeoutHandle);
				settings.connectTimeoutHandle = -1;
			}			
		}
		
		function _scheduleConect() {
			_cancelScheduleConect();
			settings.reconnectTimeoutHandle = setTimeout(function(){
				settings.reconnectTimeoutHandle = -1;
				_connect();
			}, settings.reconnectPeriod);
			console.debug("Scheduled connect attempt in [%s] ms. on handle [%s]", settings.reconnectPeriod, settings.reconnectTimeoutHandle);
		}
		
		
		function _cancelScheduleConect() {
			if(settings.reconnectTimeoutHandle!=-1) {
				clearTimeout(settings.reconnectTimeoutHandle);
				console.debug("Cancelled connect loop on handle [%s]", settings.reconnectTimeoutHandle);
				settings.reconnectTimeoutHandle = -1;
			}			
		}
		
		
		function _connect() {
			if(settings.ws!=null && (settings.ws.readyState==0 || settings.ws.readyState==1)) {
				console.warn("WS Connect requested but websocket state already [%s]", settings.ws.readyState);
				return;
			}
			_setConnectTimeout();
			_setState('connecting');
			settings.ws = new WebSocket(settings.wsuri);
	    	settings.ws.onopen = function() {
	    		_cancelConnectTimeout();
	    		_setState('connected');
	    		onopen();
	    	};
	    	settings.ws.onclose = function(e) {
	    		_setState('disconnected');
	    		onclose(e);
	    		console.debug("About to start reconnect loop");
	    		_startReconnectLoop();
	    	};
	    	settings.ws.onerror = function(e) {
	    		onerror(e);
	    	};
	    	settings.ws.onmessage = function(message) {
	    		onmessage(message.data);
	    	};
			console.debug("WS Connecting to [%s]", settings.wsuri);
		}
		
		/*
		 * 
		 */
		
		
		function _startReconnectLoop() {
			console.debug("isConnectingOrConnected:[%s]   isReconnectScheduled:[%s]", methods.isConnectingOrConnected(), methods.isReconnectScheduled());
			if(methods.isConnectingOrConnected() || methods.isReconnectScheduled()) return;
			_scheduleConect();
		}
    }
	
	/**
	 * Sends the passed request and returns a promise on the send op.
	 */
	$.websocket.send = function(data) {
		return $.websocket('send', data);
	}
	
	$.websocket.addMessageListener = function(data) {
		$.websocket('addMessageListener', data);
	}
	
	$.websocket.removeMessageListener = function(data) {
		$.websocket('removeMessageListener', data);
	}
	
	
	$.websocket.close = function() {
		$.websocket('close');
	}
	
})( jQuery, window, document );

