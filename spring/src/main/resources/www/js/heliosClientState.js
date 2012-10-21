/**
	Helios WebConsole JavaScript Client State Manager.
	Whitehead, Helios Development Group, 2011
*/

(function($) {
	$.heliosfn = {};
	/** The client top level state */
	var routers = {};
	/** A map of wildcard subscription callbacks keyed by the expression */
	var rSubscribers = {};    //  expression = [callbacks]
	/** Inits the namespace */
	var root = $.heliosfn;
	/** The root hub (black hole singularity) */
	var o = $({});
	
	/**
	 * Performs a clean split of the passed namespace into a string array.
	 */
	root.splitNamespace = function(namespace) {
		if(namespace==null || namespace.trim()=="") throw ('The passed namespace was null');
		var spaces = namespace.trim().replace(/ /g, '').replace(/\/$/g, '').replace(/^\//g, '');
		if(spaces[0]=='') spaces.shift();
		return spaces;
	};
	
	/**
	 * Sets the value of the space pointed to by the namespace
	 * @param namespace A '/' separated namespace
	 * @param value The value to set the namespace to
	 */
	root.set = function(namespace, value) {
		if(namespace==null || namespace.trim()=="") throw ('The passed namespace was null');		
		var spaces = namespace.trim().split('/');
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
				root.publish(namespace + "/" + key, [val]);
			});
		} else {
			root.publish(namespace, [value]);
		}
	};
	
	/**
	 * Returns the value bound at the specified namespace
	 */
	root.get = function(namespace, defaultValue) {				
		var spaces = root.splitNamespace(namespace);
		if(!root.exists(namespace)) {
			if(defaultValue==null) return null;
			root.set(namespace, defaultValue);
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
	root.exists = function(namespace) {
		var spaces = root.splitNamespace(namespace);		
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
	root.incr = function(namespace, increment) {
		if(increment==null) increment = 1;
		var n = root.get(namespace);
		if(n==null) root.set(namespace, increment);
		else if(!$.isNumeric(n)) {
			throw ('The value at namespace [' + namespace + "] is not a number [" + n + "]");
		} else {
			n = n+increment;
			root.set(namespace, n);
		}
	};
	
	/**
	 * Decrements the value of a number at the addressed namespace. If the value at the namespace is null, the decrement initializes it.
	 * If the decrement is null, the space is initialized with a -1. 
	 */
	root.decr = function(namespace, decrement) {
		if(decrement==null) decrement = 1;
		var n = root.get(namespace);
		if(n==null) root.set(namespace, decrement);
		else if(!$.isNumeric(n)) {
			throw ('The value at namespace [' + namespace + "] is not a number [" + n + "]");
		} else {
			n = n-decrement;
			root.set(namespace, n);
		}
	};
	
	/**
	 * Clears the object at the named namespace. If the value there is not an Object and not null, it is deleted.
	 */
	root.clear = function(namespace) {
		var space = root.get(namespace);
		if(space==null) return;
		if($.isPlainObject(space)) {
			$.each(space, function(k,v){
				delete space[k];
				root.publish(namespace + "/" + k, [null]);
			});
		} else {
			root.deleten(namespace);
		}
	};
	
	/**
	 * Deletes the item at the passed namespace
	 */
	root.deleten = function(namespace) {
		var pair = root.parentOf(namespace);
		delete root.get(pair[0])[pair[1]];		
		root.publish(namespace, [null]);
	};
	
	/**
	 * Returns an array of the parent of the passed namespace and the key
	 */
	root.parentOf = function(namespace) {
		if(namespace==null || namespace.trim()=="") return "/";
		var narr = namespace.split("/");
		if(narr.length<2) return "/";
		narr.reverse();
		var key = narr.shift();
		return [narr.reverse().join('/'), key];
	};
	
	
	
	/**
	 * Subscribes to events published to the passed namespace
	 * @param the namespace
	 * @param the callback to the subscriber
	 */
	root.subscribe = function() {
		o.on.apply(o, arguments);
	};
	

	/**
	 * Subscribes to events published to namespaces matching the passed regexp
	 * @param the namespace regexp
	 * @param the callback to the subscriber
	 */
	root.rsubscribe = function(pattern) {
		//o.on.apply(o, arguments);
		if(pattern==null) throw ("The passed pattern was null");		
		var callBacksArr = rSubscribers[pattern];
		if(callBacksArr==null) {
			callBacksArr = [];
			rSubscribers[pattern] = callBacksArr;			
		}		
		callBacksArr.push(arguments[1]);		
	};
	
	
	root.unsubscribe = function() {
		o.off.apply(o, arguments);
	};
	
	root.publish = function() {
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

}(jQuery));

/*
 console.clear();
var callback = function(ev, value) { console.info("1. Sub Callback Value for [%s] --> %s]", ev.type, value); }
$.heliosfn.rsubscribe('a/b/*', callback);
$.heliosfn.set("/a/b/c", "XXX");
$.heliosfn.set("/a/b/d", "YYY");
 **/