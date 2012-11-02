;(function(d){

	// the topic/subscription hash
	var cache = {};
	// a map of one times where each item will be cleared on a successfull callback or timeout
	var oneTimes = {};
	// The default timeout for onetimes, in ms.
	var defaultTimeout = 5000;

	d.publish = function(/* String */topic, /* Array? */args){
		// summary: 
		//		Publish some data on a named topic.
		// topic: String
		//		The channel to publish on
		// args: Array?
		//		The data to publish. Each array item is converted into an ordered
		//		arguments on the subscribed functions. 
		//
		// example:
		//		Publish stuff on '/some/topic'. Anything subscribed will be called
		//		with a function signature like: function(a,b,c){ ... }
		//
		//	|		$.publish("/some/topic", ["a","b","c"]);
		cache[topic] && d.each(cache[topic], function(){			
			if(this.oneTime==true) {
				clearTimeout(this.timeoutHandle);
				console.info("Cleared Timeout Handle:%s", this.timeoutHandle);
				var handle = oneTimes[topic];
				if(handle!=null) {
					d.unsubscribe(handle);
					delete oneTimes[topic];
				}
			}
			this.apply(d, args || []);
		});
	};

	d.subscribe = function(/* String */topic, /* Function */callback){
		// summary:
		//		Register a callback on a named topic.
		// topic: String
		//		The channel to subscribe to
		// callback: Function
		//		The handler event. Anytime something is $.publish'ed on a 
		//		subscribed channel, the callback will be called with the
		//		published array as ordered arguments.
		//
		// returns: Array
		//		A handle which can be used to unsubscribe this particular subscription.
		//	
		// example:
		//	|	$.subscribe("/some/topic", function(a, b, c){ /* handle data */ });
		//
		if(!cache[topic]){
			cache[topic] = [];
		}
		cache[topic].push(callback);
		return [topic, callback]; // Array
	};
	
	d.oneTime = function(/* String */topic, /* Function */callback, /* Optional timeout */ timeout){
		// summary:
		//		Register a callback for a one time callback on a named topic.
		// topic: String
		//		The channel to subscribe to
		// callback: Function
		//		The handler event. Anytime something is $.publish'ed on a 
		//		subscribed channel, the callback will be called with the
		//		published array as ordered arguments.
		//
		// returns: nothing
		//	
		// example:
		//	|	$.subscribe("/some/topic", function(a, b, c){ /* handle data */ });
		//
		if(!cache[topic]){
			cache[topic] = [];
		}
		cache[topic].push(callback);
		callback.oneTime = true;
		callback.timeoutHandle = setTimeout(function(){
			d.unsubscribe([topic, callback]);
		}, timeout || defaultTimeout);
		console.info("Set oneTime Timeout:%s  (%s)", callback.timeoutHandle, timeout || defaultTimeout);
		oneTimes[topic] = [topic, callback];
	};	

	d.unsubscribe = function(/* Array */handle){
		// summary:
		//		Disconnect a subscribed function for a topic.
		// handle: Array
		//		The return value from a $.subscribe call.
		// example:
		//	|	var handle = $.subscribe("/something", function(){});
		//	|	$.unsubscribe(handle);

		var t = handle[0];
		cache[t] && d.each(cache[t], function(idx){
			if(this == handle[1]){
				cache[t].splice(idx, 1);
			}
		});
	};
	
	d.pendingOneTimes = function() {
		var cnt = 0;
		d.each(oneTimes, function(){cnt++;});
		return cnt;
	};

})(jQuery);
