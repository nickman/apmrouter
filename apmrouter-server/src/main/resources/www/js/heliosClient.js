
/**
	Helios WebConsole OT Client API for JavaScript.
	Whitehead, Helios Development Group, 2011
*/
(function( $ ){
$.helios = function(options) {
	var defaults = {
			/** Turns debug on and off */
			debug : true,
			/** The feed subscriber aggregation completion size */
			feedSubCompletionSize : 100,
			/** The feed subscriber aggregation completion timeout */
			feedSubCompletionTimeout : 15000,
			/** The maximim number of feed items to retrieve at a time when polling */
			pollFeedMaxItems : 50,
			/** The feed polling timeout */
			pollFeedTimeout : 15000,
			/** The fixed delay frequency of the feed poller polling operation (ms.) */
			feedPollerFrequency : 5000,
			/** This js client's data marshalling preference for handling feed data */
			outputFormat : 'JSON',
			/** The default URI prefix for the OT server REST endpoint */ 
			restUri: "api/sub/",
			/** Indicates if this client is websocket capable */
			webSocketCapable: (window.WebSocket!=null),
	}; // default options
	
	//inherit from provided configuration
	var options = $.extend(defaults, options);
	
	/** A map of wildcard subscription callbacks keyed by the expression */
	var rSubscribers = {};    //  expression = [callbacks]
	/** Inits the namespace */
	var root = {};
	/** The root hub (black hole singularity) */
	var o = $({});
	
	// ===================================================
	// Session Maintenance
	// ===================================================
	

	/**
	 * Processes callbacks to utility methods
	 */
	$.helios._processCallbacks = function(callbacks, args) {
		if(callbacks!=null) {
			if(!$.isArray(callbacks)) {
				callbacks = [callbacks];
			}
			$.each(callbacks, function(i, v){
				if($.isFunction(v)) {
					v(args);
				}
			});
		}
	};
	
	/**
	 * Initializes the session with the server, acquiring the session Id.
	 */
	$.helios.initSession = function(callbacks) {
		$.ajax({
			url: options.restUri + "sessionid",		  
			context: document.body,
			success: function(data){							
				$.helios.set("/session/sessionId", data);
				$.helios._processCallbacks(callbacks, [data]);
			}	
		});		
	};
	
	/**
	 * Terminates the http or websocket session.
	 */
	$.helios.termSession = function(callbacks) {
		if(!$.helios.exists("/session/sessionId") || $.helios.get("/session/sessionId")==null) return;
		$.helios.stopPoller();
		$.helios.stopSubscriber(function() {
			$.ajax({
				url: options.restUri + "signout",		  
				context: document.body,
				cache: false,
				success: function(data){								
					$.helios._processCallbacks(callbacks, [data]);
					$.helios.deleten("/session/sessionId", true);
				}
			});			
		});
	};
	
	/**
	 * Starts the server side subscriber proxy
	 */
	$.helios.startSubscriber = function(callbacks) {
		$.ajax({
			url: options.restUri + "startsubscriber/" + options.outputFormat + "/" + options.webSocketCapable,		  
			context: document.body,
			cache: false,
			success: function(data){
				var on = new ObjectName(data);
				$.helios.set("/session/subscriberObjectName", on);
				$.helios._processCallbacks(callbacks, [on]);
			}
		});
	};
	
	/**
	 * Stops the server side subscriber proxy
	 */
	$.helios.stopSubscriber = function(callbacks) {
		if(!$.helios.exists("/session/subscriberObjectName")) return; 
		$.ajax({
			url: options.restUri + "stopsubscriber",		  
			context: document.body,
			cache: false,
			success: function(data){				
				$.helios.deleten("/session/subscriberObjectName");
				$.helios._processCallbacks(callbacks, [data]);
			}	
		});
	};
	
	
	
	/**
	 * Starts a router key feed
	 * Args are feedName, feedProperties, [callbacks]
	 * Callbacks are invoked as callback(feedInfo)
	 * 		completionSize
	 * 		completionTimeout
	 * 		requestedSubFeedKey
	 * 		routeId
	 * 		routerKeys
	 * 		type
	 */
	$.helios.startFeed = function() {
		var args = $.makeArray(arguments);
		var feedName = args.shift();		
		var argProps = args.shift();
		var props = $.extend(argProps, options);
		$.ajax({
			url: options.restUri + "startFeed/" + feedName,
	        type : 'POST',
	        data: props,
			success: function(data){
				$.helios.incr("/subFeeds/counts/" + data.requestedSubFeedKey + "/count");
				$.helios.set("/subFeeds/meta/routerKeys/" + data.requestedSubFeedKey, data.routerKeys[0]);								
				$.helios.set("/feeds/meta/" + data.type, data.routeId);
				console.dir(data);
				$.helios._processCallbacks(args, data);
			}	
		});		
	};
	
	
	/**
	 * Stops a router key feed
	 * Args are feedName, feedProperties, [callbacks]
	 */
	$.helios.stopFeed = function(feedName, subKey) {
		$.ajax({
			url: options.restUri + "stopFeed/" + feedName + '/' + subKey,
	        type : 'GET',
	        cache: false,
	        dataType: 'text',
			success: function(data){
				if("OK"==data) {
					$.helios.decr("/subFeeds/counts/" + subKey + "/count");
				} else {
					// no action ? 
				}
			}
		});
	};
	


	/**
	 * Starts the feed poller
	 * @param timeout The frequency of the feed poll.
	 */
	$.helios.startPoller = function(timeout) {
		if($.helios.get("/poller/active")) return;
		var delay = timeout==null ? options.feedPollerFrequency : timeout;
		options.feedPollerFrequency = delay;
		var timerId = window.setTimeout(function(){
			$.helios._pollFeed(function(){
				$.helios._repoll(delay);
			});
		}, delay);
		$.helios.set("/poller/timerId", timerId);
		$.helios.set("/poller/active", true);
		
	}

	/**
	 * Schedules a feed poll
	 */
	$.helios._repoll = function() {
		if(!$.helios.get("/poller/active")) return;		
		var delay = options.feedPollerFrequency;
		console.info("Scheduling Next Feed Poll:" + delay);
		var timerId = window.setTimeout(function(){
			$.helios._pollFeed(function(){
				$.helios._repoll();
			});		
		}, delay);	
		$.helios.set("/poller/timerId", timerId);
	};

	/**
	 * Stops the feed poller
	 */
	$.helios.stopPoller = function() {
		if(!$.helios.get("/poller/active")) return;
		var timerId = $.helios.get("/poller/timerId");
		if(timerId!=null) {
			window.clearTimeout(timerId);
			$.helios.set("/poller/timerId", null);
			$.helios.set("/poller/active", false);			
		}
	};
	
	/**
	 * Polls the metric feed for push messages
	 * @param an optional array of callbacks
	 * Format is: 
	 * "batch" : [
	 * 		<type-key> {
	 * 			<subFeedKey> : [
	 * 				<ClosedTrace>
	 * 			]
	 * 		}
	 * ]
	 * 
	 */
	$.helios._pollFeed = function(callbacks) {	
		$.ajax({
			url: options.restUri + "poll/" + options.pollFeedTimeout + '/' + options.pollFeedMaxItems ,   
			dataType: 'json',
			204: function() {
				if(callbacks!=null) {
					$.helios._processCallbacks(callbacks, [null]);
				}
			},
			success: function(json) {
				$('.feedActivityAware').attr('src', 'img/green-light-16X16.png');
				try {
					console.dir(json);
					if(json==null) {
						if(callbacks!=null) {
							$.helios._processCallbacks(callbacks, [null]);					
						}
						return;
					}
					var batchArray = json['batch'];
					if(batchArray==null) {
						$.helios.publish("/error/poller", ['Feed Response had no batch header', json]);
						return;
					}
					var typeKeyCounts = {};
		            var subFeedKeyCounts = {};
		            var totalItemCount = 0;
		            var feedMap = {};
		            $.helios.incr("/poller/batchCount/", batchArray.length);
		            
		            // Can we use jQuery.extend(deep....) for this ?
		            
		            // Get the counts and initialize the accumulation feedMap for the feedTypes and subFeeds.
					$.each(batchArray, function(i, batch){
						$.each(batch, function(feedType, subFeeds){
							feedMap[feedType] = {};
							$.each(subFeeds, function(subFeedKey, feedItems) {
								totalItemCount += feedItems.length;
								feedMap[feedType][subFeedKey] = [];
								subFeedKeyCounts[subFeedKey] = subFeedKeyCounts[subFeedKey]==null ? feedItems.length : subFeedKeyCounts[subFeedKey]+feedItems.length;
								typeKeyCounts[feedType] = typeKeyCounts[feedType]==null ? feedItems.length : typeKeyCounts[feedType]+feedItems.length;
							});
						});
					});
					// Get the total item count load the accumulation feedMap
					$.each(batchArray, function(i, batch){
						$.each(batch, function(feedType, subFeeds){						
							$.each(subFeeds, function(subFeedKey, feedItems) {
								$.merge(feedMap[feedType][subFeedKey], feedItems);
								totalItemCount += feedItems.length;
							});
						});
					});				
					// Iterate the feed map and publish the data and stats.
					$.each(feedMap, function(feedType, subFeeds){
						$.helios.incr("/feeds/" + feedType + "/totalItems", typeKeyCounts[feedType]);
						$.each(subFeeds, function(subFeedKey, items){						
							$.helios.incr("/feeds/" + feedType + "/" + subFeedKey + "/totalItems", subFeedKeyCounts[subFeedKey]);
							$.helios.publish("/livedata/" + feedType + "/" + subFeedKey, items);						
						});
					});
	
					$.helios.incr("/feeds/totalItems", totalItemCount);
					if(callbacks!=null) {
						$.helios._processCallbacks(callbacks, [batchArray]);
					}
				} catch (e) {
					$.helios.publish("/error/poller", ['Unexpected Error In Poller Process', e]);
				} finally {
					$('.feedActivityAware').attr('src', 'img/red-light-16X16.png');
				}
			}
		});	
	};
	
	
	/**
	 * Searches the last metric cache and returns the most recent ClosedTrace for any matching FQNs
	 * @param expression The expression to match against the ClosedTrace FQN
	 * @param The callbacks.  
	 */
	$.helios.lastMetricSearch = function(expression, callbacks) {
		var append = escape(expression.replace(forwardSlashRegex, '%2F'));
		$.ajax({
			url: options.restUri  + "lastMetric/" + append,		  
			success: function(data){
				$.helios._processCallbacks(callbacks, data);
			}	
		});
	}
	
	
	/**
	 * Returns the child entries for the next level down the metric tree from the passed path 
	 * @param path The path to get the child entries for
	 * @param callbacks The result processors
	 */
	$.helios.metricPath = function(path, callbacks) {
		var append = path.replace(forwardSlashRegex, '%2F');
		$.ajax({
			url: options.restUri  + "metrictree/" + append,		  
			success: function(data){
				$.helios._processCallbacks(callbacks, data);
			}	
		});
	}
	
	// ===================================================
	// The pubsub  framework
	// ===================================================
	
	$.helios.getRoot = function() {
		return root;
	}
	
	/**
	 * Performs a clean split of the passed namespace into a string array.
	 */
	$.helios.splitNamespace = function(namespace) {
		if(namespace==null || namespace.trim()=="") throw ('The passed namespace was null');
		var spaces = namespace.trim().replace(/ /g, '').replace(/\/$/g, '').replace(/^\//g, '').split('/');
		if(spaces[0]=='') spaces.shift();
		return spaces;
	};
	
	/**
	 * Sets the value of the space pointed to by the namespace
	 * @param namespace A '/' separated namespace
	 * @param value The value to set the namespace to
	 */
	$.helios.set = function(namespace, value) {
		try {
			if(namespace==null || namespace.trim()=="") throw ('The passed namespace was null');		
			var spaces = $.helios.splitNamespace(namespace);
			var entryLength = spaces.length-1;
			if(entryLength<1) {
				root[spaces[0]]=value; 			
			} else {		 
				var current = root;
				for(var i = 0, l = entryLength; i < l; i++) {		
					if(spaces[i]=="") continue;
					if(current[spaces[i]]==null) current[spaces[i]] = {};			
					current = current[spaces[i]];						
				}
				current[spaces[entryLength]]=value;
			}
			if(value!=null && $.isPlainObject(value)) {
				$.each(value, function(key, val){
					$.helios.publish(namespace + "/" + key, [val]);
				});
			} else {
				$.helios.publish(namespace, [value]);
			}
		} catch (e) {
			$.helios.publish("/error/singularity", "Failed to execute a singularity set on [" + namespace + "] for value [" + value + "]");
		}
	};
	
	/**
	 * Returns the value bound at the specified namespace
	 */
	$.helios.get = function(namespace, defaultValue) {		
		if("/"==namespace) return root;
		var spaces = $.helios.splitNamespace(namespace);
		if(!$.helios.exists(namespace)) {
			if(defaultValue==null) return null;
			$.helios.set(namespace, defaultValue);
			return defaultValue;
		}
		var current = root;
		for(var i = 0, l = spaces.length; i < l; i++) {			
			if(current[spaces[i]]==null) return false;
			current = current[spaces[i]];
		}
		return current;		
	};
	
	/**
	 * Indicates if the passed namespace is bound.
	 */
	$.helios.exists = function(namespace) {
		if("/"==namespace) return true;
		var spaces = $.helios.splitNamespace(namespace);		
		var current = root;
		for(var i = 0, l = spaces.length; i < l; i++) {			
			if(current[spaces[i]]==null) return false;
			current = current[spaces[i]];
		}
		return true;
	};
	
	/**
	 * Increments the value of a number at the addressed namespace. If the value at the namespace is null, the increment initializes it.
	 * If the increment is null, the space is initialized with a 1. 
	 */
	$.helios.incr = function(namespace, increment) {
		if(increment==null) increment = 1;
		var n = $.helios.get(namespace);
		if(n==null) $.helios.set(namespace, increment);
		else if(!$.isNumeric(n)) {
			throw ('The value at namespace [' + namespace + "] is not a number [" + n + "]");
		} else {
			n = n+increment;
			$.helios.set(namespace, n);
		}
	};
	
	/**
	 * Decrements the value of a number at the addressed namespace. If the value at the namespace is null, the decrement initializes it.
	 * If the decrement is null, the space is initialized with a -1. 
	 */
	$.helios.decr = function(namespace, decrement) {
		if(decrement==null) decrement = 1;
		var n = $.helios.get(namespace);
		if(n==null) $.helios.set(namespace, decrement);
		else if(!$.isNumeric(n)) {
			throw ('The value at namespace [' + namespace + "] is not a number [" + n + "]");
		} else {
			n = n-decrement;
			$.helios.set(namespace, n);
		}
	};
	
	/**
	 * Clears the object at the named namespace. If the value there is not an Object and not null, it is deleted.
	 */
	$.helios.clear = function(namespace) {
		var space = $.helios.get(namespace);
		if(space==null) return;
		if($.isPlainObject(space)) {
			$.each(space, function(k,v){
				delete space[k];
				$.helios.publish(namespace + "/" + k, [null]);
			});
		} else {
			$.helios.deleten(namespace);
		}
	};
	
	/**
	 * Deletes the item at the passed namespace
	 */
	$.helios.deleten = function(namespace, noPub) {
		var noPublish = noPub || false;
		var node = $.helios.get(namespace);
		var context = [];
		var parent = [];
		var ctx = function(arr) {
		    return namespace + ((namespace=="/") ? "" : "/") + arr.join("/");
		}		
		var recurse = function(key, value) {
		    parent.push(value);
		    var plain = $.isPlainObject(value);
		    if(!key || plain) {
		        if(key) context.push(key);
		        $.each(value, recurse);
		        if(key) {
		            delete parent[(parent.length-2)][key];
		            if(!noPublish) $.helios.publish(ctx(context), [null]);
		        }
		    } else {
		        context.push(key);
		        //console.info("Context:[%s]  Key:[%s], Value:[%s]", ctx(context), key, value);
		        if(!noPublish) $.helios.publish(ctx(context), [null]);
		        delete parent[(parent.length-2)][key];
		    }
		    parent.pop();
		    context.pop();
		};
		recurse(null, node);
		var topRef = $.helios.parentOf(namespace); 
		delete $.helios.get(topRef[0])[topRef[1]];
		if(!noPublish) $.helios.publish(topRef[0] + topRef[1], [null]);
	};
	
	/**
	 * Returns an array of the parent of the passed namespace and the key
	 */
	$.helios.parentOf = function(namespace) {
		if(namespace==null || namespace.trim()=="") return "/";
		var narr = namespace.split("/");
		if(narr.length<2) return "/";
		narr.reverse();
		var key = narr.shift();		
		return [(narr.length==1 && narr[0]=="") ? "/" : narr.reverse().join('/'), key];
	};
	
	
	
	/**
	 * Subscribes to events published to the passed namespace
	 * @param the namespace
	 * @param the callback to the subscriber
	 */
	$.helios.subscribe = function() {
		if(arguments[0].match(/\/\*$/)!=null) {
			$.helios.rsubscribe.apply(arguments);
		} else {
			o.on.apply(o, arguments);
		}
	};
	

	/**
	 * Subscribes to events published to namespaces matching the passed regexp
	 * @param the namespace regexp
	 * @param the callback to the subscriber
	 */
	$.helios.rsubscribe = function() {
		//o.on.apply(o, arguments);
		var args = $.isArray(arguments[0]) ? arguments[0] : arguments;
		if(args.length==0) {
			args = this;
		}
		var pattern = args[0];
		if(pattern==null) throw ("The passed pattern was null");		
		var callBacksArr = rSubscribers[pattern];
		if(callBacksArr==null) {
			callBacksArr = [];
			rSubscribers[pattern] = callBacksArr;			
		}		
		callBacksArr.push(args[1]);		
	};
	
	/**
	 * Returns the total number of active subfeeds
	 */
	$.helios.getSubFeedTotals = function() {
		var count = 0;
		var subFeeds = $.helios.get("/subFeeds/counts");
		$.each(subFeeds, function(feedName, subcount) {
			count += subcount.count;
		});				
		return count;
	};

	
	/**
	 * Unsubscribe a singularity listener
	 */
	$.helios.unsubscribe = function() {
		o.off.apply(o, arguments);
	};
	
	/**
	 * Publishes an event to the singularity
	 */
	$.helios.publish = function() {
		var event = o.trigger.apply(o, arguments);
		var args = arguments;
		$.each(rSubscribers, function(pattern, listeners) {
			if(new RegExp(pattern).test(args[0])) {
				$.each(listeners, function(i, listener){
					listener($.Event(args[0]), args[1]);
				});				
			}
		});		
	};
	
	/**
	 * Recursively iterates the singularity space and issues callbacks on each found branch and leaf.
	 * @param The optional starting namespace
	 * @param A leaf callback with arguments (<full-path elements array>, <value>).  Ignored if null.
	 * @param A branch callback with arguments (<full-path elements array>). Ignored if null.
	 */
	$.helios.iteratePath = function(node, leafer, brancher) {
		var target = node==null ? $.helios.getRoot() : $.helios.get(node);
		if(target==null) return;
		var context = [];
		function recurse(key, val) {
			context.push(key);
		    if (val instanceof Object) {		        
		        if(brancher!=null) {
		        	brancher(context);
		        }
		        $.each(val, function(key, val) {
		                recurse(key, val);
		        });		        
		    } else {
		    	if(leafer!=null) {
		    		leafer(context, val);
		    	}
		    }
		    context.pop();
		}
		$.each(target, function(k, v) { recurse(k, v); });		
	}
	
	/**
	 * Builds a singularity path from the passed array
	 * @param arr the array
	 * @return a singularity path
	 */
	$.helios.buildPath = function ctx(arr) {
	    return "/" + arr.join("/");
	}			

	
	//===============================================================================
	//		Wrapper Functions and private wrapper utilities
	//===============================================================================
	
	
	
	
	/**
	 * Delegated to from $.fn.heliosState
	 */
	$.helios._setState = function(target, props) {
		var wildCardSnode = function(snode) {
			return snode=="/" ? "/*" : snode + "/*";
		};
		var namespace = props.node!=null ? props.node : (target.data('singularityNode') || "/");
		var snapshot = props.snapshot!=null ? props.snapshot : target.snapshot;
		var paths = props.paths!=null ? props.paths : target.paths;
		var values = props.values!=null ? props.values : target.values;		
		if(snapshot!=null && $.isFunction(snapshot)) {
			var singularitySnapshot = {};
			$.helios.iteratePath(namespace,
				function(path, value) {  /* leafer */
					singularitySnapshot[$.helios.buildPath(path)]=value;
				}
			);
			target.data('singularityNode', namespace);
			snapshot(singularitySnapshot);
		}
		if(paths!=null && $.isFunction(paths)) {
			$.helios.subscribe(wildCardSnode(namespace), function(ev, data) {
				paths($.helios.splitNamespace(ev.type));
			});
		}
		if(values!=null && $.isFunction(values)) {
			$.helios.subscribe(wildCardSnode(namespace), function(ev, data) {
				values($.helios.splitNamespace(ev.type), data);
			});			
		}
		//target.class('singularitySubscriber')
		
	}

	
	/**
	 * <p>Subscribes resolved jQuery items to a node in the singularity recursively.
	 * <p>Arguments are passed in a properties instance with the following keys:<ul>
	 * 	<li><b>node</b>: The singularity namespace to subscribe to. Blank or "/" means root. e.g. "/a/b/c"</li>
	 *  <li><b>snapshot</b>: Callsback to the items with a map of current data with the path as the key and the value as the value.</li>
	 *  <li><b>paths</b>: Callsback to the items with path updates with the array of the path segments.</li>
	 *  <li><b>values</b>: Callsback to the items with singularity value updates with the array of the path and the value.</li>
	 * </ul>
	 * @return the JQuery set
	 */
	$.fn.heliosState = function(props) {
		if(props==null) props = {};
		return this.each(function(){
			$.helios._setState($(this), props);
		});
	};
	
	/**
	 * Delegated to from $.fn.heliosFeed
	 * Target is jQuery(<subscribed item>)
	 */
	$.helios._subscribeItem = function(target, feedName, subFeedKey, callbacks) {
		if(callbacks==null || callbacks.length<1) {
			if(target[0] != null &&  target[0].onFeedData != null && $.isFunction(target[0].onFeedData)) {
				callbacks = [target[0].onFeedData];
			}			
		}
		console.info("Target:%o", callbacks);
		
		$.each(callbacks, function(i, callback) {
			$.helios.subscribe('/livedata/' + feedName + '/' + subFeedKey, callback);
		});
		
	}
	
	/**
	 * Subscribes resolved jQuery items to a feed.
	 * @param feedName The name of the feed e.g. "metric-feed"
	 * @param props The feed properties e.g. {'routerKey': 'helios.metrictree.njw810.*.>'}
	 * @param callbacks The function to call when data is available. If null, will attempt to find a "onFeedData" method on the item.
	 * @return the JQuery set 
	 */
	$.fn.heliosFeed = function(feedName, props, callbacks) {
		if(props==null) props = {};
		var subFeedKey = null;
		var target = this;
		$.helios.startFeed(feedName, props, function(feedInfo){
			subFeedKey = feedInfo.requestedSubFeedKey;
			target.each(function(index, item){
				$.helios._subscribeItem($(item), feedName, subFeedKey, callbacks);
			});			
			return target;
		});
	};
	
	/**
	 * Initializes internal tracking singularity subscriptions.
	 */
	$.helios._initClientSubscriptions = function() {
		/**
		 * Starts the subscriber on session set and stops it on session unbind
		 */
		$.helios.subscribe("/session/sessionId", function(ev, session){
			if(session==null) {
				$.helios.stopSubscriber();
			} else {
				$.helios.startSubscriber();
			}
		});
		/**
		 * Starts the error listener
		 */
		$.helios.subscribe("/error/*", function(ev, error){
			console.error("Error in namespace [%s]:[%s]", ev.type, error);
			console.error("=== Published Error ===");
			console.group();
			console.error("Event:%o", ev);
			console.error("Error:%o", error);
			console.trace();
			console.groupEnd();
			console.error("=== End Published Error ===");			
			// need some more error handling here ?
		});		
		/**
		 * Initializes poller state control
		 */
		$.helios.set("/poller/active", false);
		$.helios.subscribe("/subFeeds/counts/*", function(ev, data){
			// $.helios.incr("/subFeeds/counts/" + data.requestedSubFeedKey + "/count");
			var cnt = $.helios.getSubFeedTotals();
			var pollerRunning = $.helios.get("/poller/active"); 
			if(cnt>0) {
				if(!pollerRunning && !options.webSocketCapable) {
					$.helios.startPoller();
				}
			} else {
				if(pollerRunning) {
					$.helios.stopPoller();
				}				
			}			
		});
		
	}

	
	/**
	 * Initialize the default ajax settings
	 */
	$.ajaxSetup({
		cache: false,
		error: function(e, xhr, settings, ex) {
			console.error("=== Ajax Error ===");
			console.group();
			console.error("Event:%o", e);
			console.error("XHR:%o", xhr);
			console.error("Settings:%o", settings);
			console.error("Exception:%o", ex);
			console.trace();
			console.groupEnd();
			console.error("=== End Ajax Error ===");
		}
	});
	
	/** The javascript jmx client */
	var jmxClient = null;
	try {
		$.getScript("js/jolokia/jolokia-min.js", function(){
			$.getScript("js/jolokia/jolokia-simple-min.js", function() {
				$.getScript("js/jolokia/json2-min.js", function(){
					jmxClient = new Jolokia("/jmx");
					jmxClient.request({ 
						type: "read", 
						mbean: "java.lang:type=Runtime", 
						attribute: "Name" 
						}, { 
						success: function(response) {
							$.helios.set("/server/runtime/name", response.value);							
						}
					});
				});				
			});			
		});		
	} catch (e) {
		$.helios.publish("/error/jmx/clientInit", e);		
	}
	$.helios._initClientSubscriptions();
};

//==============================
//  Some utility constants
//==============================
/** A '/' regex replacer */
var forwardSlashRegex = new RegExp('/', 'g');



})( jQuery );
$.helios();


/*

To test simple log in / log out
===============================
console.clear();
var callback = function(ev, value) { console.info("1. Sub Callback Value for [%s] --> [%s]", ev.type, value); }
$.helios.subscribe('/*', callback);
$.helios.initSession(function(session){
    console.info("Started Session [%s]", session); 
});
setTimeout(
    function() {
        console.info("===========Terminating Session==============");
        $.helios.termSession(function(session){
            console.info("Terminated Session [%s]", session);
        });
    }, 5000
);

Test for Listening On Hierarchical Node Deletes
===============================================
console.clear();
$.helios.set("/a/b/c", 1);
$.helios.set("/x/y/z", 2);
$.helios.set("/x/y/y", {'Foo' : {'Bar' : 'Snafu'}});
console.dir($.helios.getRoot());
var callback = function(ev, value) { console.info("1. Sub Callback Value for [%s] --> [%s]", ev.type, value); }
$.helios.subscribe('/*', callback);

$.helios.deleten("/a");
console.info("==================================================");
console.dir($.helios.getRoot());




console.clear();
var callback = function(ev, value) { console.info("1. Sub Callback Value for [%s] --> [%s]", ev.type, value); }
$.helios.subscribe('/*', callback);
$.helios.subscribe("/session/subscriberObjectName", function(ev, data) {

});
$.helios.initSession(function(session){
    console.info("Started Session [%s]", session);     
});
setTimeout(
    function() {
        console.info("===========Terminating Session==============");
        $.helios.termSession(function(session){
            console.info("Terminated Session [%s]", session);
        });
    }, 5000
);




console.clear();
var callback = function(ev, value) { console.info("1. Sub Callback Value for [%s] --> [%s]", ev.type, value); }
$.helios.subscribe('/*', callback);
$.helios.subscribe("/session/subscriberObjectName", function(ev, data) {
    if(data!=null) {
        $.helios.startFeed("metric-feed", {'routerKey': 'helios.metrictree.njw810.*.>'});
    } else {
        //console.info("Odd Feed Start: Event:[%o] Data:[%o]", ev, data);
    }
});
$.helios.initSession(function(session){
    console.info("Started Session [%s]", session);     
});
setTimeout(
    function() {
        console.info("===========Terminating Session==============");
        $.helios.termSession(function(session){
            console.info("Terminated Session [%s]", session);
        });
    }, 150000
);



*/

	

