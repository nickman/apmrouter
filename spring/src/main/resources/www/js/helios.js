
/**
	Helios WebConsole OT Server API for JavaScript.
	Whitehead, Helios Development Group, 2011
	
	All Ajax calls return JSON unless otherwise specified.
	This is mostly everything except metric feeds and some basic text responses.
*/

/**
======================================================================================
    Constants
======================================================================================
*/

/** Currently supported output formats */
var OUTPUT_FORMATS = ['JSON', 'TEXT', 'XML', 'JAVA'];  // why java ?  applet maybe ?
/** A '/' regex replacer */
var forwardSlashRegex = new RegExp('/', 'g');
/** The default base properties for feed subscriptions */
var FEEDSUB_DEFAULT_KEYS = ['completionSize', 'completionTimeout'];



/** ====================================================================================== */

/** The active session id */
var sessionId = "";
/** Indicates if the browser is websocket capable */
var webSocketCapable = false;
/** Indicates if this js client is signed in */
var signedIn = false;
/** Indicates if a client subscriber proxy has been started */
var subscriberStarted = false;
/** This client's default feed properties */
var feedSubProperties = {};
/** The feed subscriber aggregation completion size */
feedSubProperties[FEEDSUB_DEFAULT_KEYS[0]] = 10;
/** The feed subscriber aggregation completion timeout */
feedSubProperties[FEEDSUB_DEFAULT_KEYS[1]] = 10000;
/** The maximim number of feed items to retrieve at a time when polling */
var pollFeedMaxItems = 10;
/** The feed polling timeout */
var pollFeedTimeout = 15000;
/** Indicates if the feel poller is running */
var feedPollerRunning = false;
/** The fixed delay frequency of the feed poller polling operation (ms.) */
var feedPollerFrequency = 5000;
/** The timer id for the feedPoller timer */
var feedPollerTimerId = null;

/** The subscriber session JMX object name template */
var subscriberSessionObjectTemplate = "org.helios.server.ot.session:type=SubscriberSession,id=";
/** The rendered subscriber session JMX object name */
var subscriberSessionObjectName = null;
/** The javascript jmx client */
var jmxClient = null;
/** A '/' regex replacer */
var forwardSlashRegex = new RegExp('/', 'g');
/** This js client's data marshalling preference for handling feed data */
var outputFormat = 'JSON';
/** The currently active data feeds. The value is the feed meta-data and the key is the data feed id */
var activeFeeds = {};
/** The total number of metric feed batches delivered */
var totalFeedBatchCount = 0;

var configurationContext = null;


//======================================================================================

/**
 * Initializes the helios javascript client
 */
function helios_init() {
	initConfigurationContext();
	try {
		webSocketCapable=(WebSocket!=null);
	} catch (e) { webSocketCapable=false; }
	console.info("WebSocket Support:%s", webSocketCapable);
	$('#wsEnabled').prop("checked", webSocketCapable);
	$('#tabs').tabs({'cache':false, ajaxOptions: { async: false }});	
	$('.init-hide').hide();
	
	$('#signInButton').bind('click', function() {
		helios_signin();
	});
	$('#signOutButton').bind('click', function() {
		helios_signout();
	});
	
	$('#startSubscriber').bind('click', function() {
		helios_startSubscriber();
	});
	
	$('#subMask').keydown(function (e) {
		//console.debug("Key:%s, Ctrl:%s, Shift:%s, Meta:%s", e.keyCode, e.ctrlKey, e.shiftKey, e.metaKey);
		//console.dir(e);
		var target = this;
		if (e.keyCode == 13) {
			if(e.ctrlKey) {
				console.info("Viewing Metrics....")				
			} else {
				var mask = $(target).val().trim();
				if(""!=mask) {
					console.info("Subscribing to [" + mask + "]");
					helios_startFeed("metric-feed", {'mask': mask });					
				} else {
					helios_stopFeed("metric-feed");
				}				
			}
		}
	});

	

	$('#startSubButton').bind('click', function() {
		helios_startSubscriber();
	});
	$('#stopSubButton').bind('click', function() {
		helios_stopSubscriber();
	}).hide();
	/*
	$('#startSubscriber').bind('click', function() {
		helios_startSubscriber();
	}).hide();
	$('#stopSubscriber').bind('click', function() {
		helios_stopSubscriber();
	}).hide();
	$('#subscription').hide();
	
	
	*/
}

/**
 * Initiates an http session
 */
function helios_signin() {
	$.ajax({
		url: "api/sub/sessionid",		  
		context: document.body,
		success: function(data){
		growl('Session started:  ' + data);
			onSignIn(data);
			helios_startSubscriber();
		}	
	});
}


/**
 * Returns the child entries for the next level down the metric tree from the passed path 
 * @param path The path to get the child entries for
 * @param callback The result processor
 */
function helios_metricPath(path, callback) {
	var append = path.replace(forwardSlashRegex, '%2F');
	console.info("Metric Path Append:[%s]", append);
	$.ajax({
		url: "api/sub/metrictree/" + append,		  
		context: document.body,
		success: function(data){
			$.jGrowl('Metric Path for:' + path);
			if(callback==null) {
				$('#output').val(JSON.stringify(data));
				console.dir(JSON.stringify(data));
			} else {
				return callback(data);
				callback(res);
			}
		}	
	});
}

/**
 * Terminates the http or websocket session.
 */
function helios_signout() {
	helios_stopSubscriber();
	$.ajax({
		url: "api/sub/signout",		  
		context: document.body,
		success: function(data){
			growl('Session terminated');
			onSignOut(data);
		}	
	});
}

/**
 * Starts the client subscriber proxy service
 * @return The session id that the subscriber was started for
 */
function helios_startSubscriber() {
	console.info("Starting Subscriber....");
	$.ajax({
		url: "api/sub/startsubscriber/" + outputFormat + "/" + webSocketCapable,		  
		context: document.body,
		success: function(data){
		$.jGrowl('<p>Subscriber started<ul><li>Output Format:' + outputFormat + '</li><li>Websocket:' + webSocketCapable + '</li></ul>');
			onStartSubscriber(data);
		},	
		error: function(err) {
			console.error("Failed to start subscriber:" + err);
		}
	});
}

/**
 * Stops the client subscriber proxy service
 * @return "OK" if the subscriber proxy was stopped, "NOSUBSCRIBER" if a subscriber proxy did not exist
 */
function helios_stopSubscriber() {
	if(!subscriberStarted) return;	
	activeFeeds = {};
	helios_stopPoller();
	console.info("Stopping Subscriber....");
	$.ajax({
		url: "api/sub/stopsubscriber",
		dataType: 'text',
		context: document.body,
		success: function(data) {
			growl('Subscriber terminated.');
			onStopSubscriber(data);
		},	
		error: function(err) {
			console.error("Failed to stop subscriber:" + err);
		}
	});
}

/**
 * Starts or replaces an active data feed
 * @param feedName The id of feed
 * @param feedProperties The feed properties
 * @param callback The result handler
 */
function helios_startFeed(feedName, feedProperties, callback) {
	if(feedProperties==null) feedProperties = {};
	$.each(FEEDSUB_DEFAULT_KEYS, function(i,v){
		if(feedProperties[v]==null)  feedProperties[v]=feedSubProperties[v]; 
	});

	$.ajax({
		url: "api/sub/startFeed/" + feedName,
        type : 'POST',
        data: feedProperties,
		success: function(data){
			growl("Feed Started", data);
			console.debug("Feed started for [%s] with properties [%s]", feedName, feedProperties);
			updateActiveFeeds(data);
			if(!feedPollerRunning) {
				helios_startPoller();
			}
			if(callback!=null) callback(data);
		}	
	});
}

function updateActiveFeeds(data) {
	if(data.type==null) return;
	var activeFeed = getSub(activeFeeds, data.type, {});
	activeFeed.data = data;
	var subFeeds = getSub(activeFeed, 'subFeeds', {});
	var subFeed = getSub(subFeeds, data.requestedSubFeedKey, 0);
	activeFeeds[data.type].subFeeds[data.requestedSubFeedKey] += 1
	console.info("ActiveFeedsUpdate");
	console.dir(activeFeeds);
	
}

function getSub(target, key, defaultValue) {
	var item = target[key];
	if(item==null) {
		item = defaultValue;
		target[key] = item;
	}	
	return item;
}



/**
 * Stops an active data feed
 * @param feedName The id of feed
 * @param subKey The feed subKey
 * @param callback The result handler
 */
function helios_stopFeed(feedName, subKey, callback) {
	$.ajax({
		url: "api/sub/stopFeed/" + feedName + '/' + subKey,
        type : 'GET',
        cache: false,
        dataType: 'text',
		success: function(data){
			if("OK"==data) { 
				growl("Feed Stopped" + feedName);  
				console.debug("Feed stopped for [%s]", feedName);
				// Need to remove, not set to null
				
				delete activeFeeds[feedName];
				console.info("ActiveFeeds:");
				console.dir(activeFeeds);
				
				if($.isEmptyObject(activeFeeds) && feedPollerRunning) {
					helios_stopPoller();
				}

				if(callback!=null) callback(data);
			} else {
				growl("Stop Feed Request Invalid:" + feedName); 
			}
		}	
	});
}


/**
 * Starts the feed poller
 * @param timeout The frequency of the feed poll.
 */
function helios_startPoller(timeout) {
	if(feedPollerRunning) return;
	var delay = timeout==null ? feedPollerFrequency : timeout;
	feedPollerFrequency = delay;
	window.setTimeout(function(){
		helios_pollFeed(function(){
			helios_repoll();
		});
	}, 10);
	feedPollerRunning = true;
	$.jGrowl("Feed Poller Started:" + delay + " ms.");
}

/**
 * Schedules a feed pool
 */
function helios_repoll() {
	if(!feedPollerRunning) return;
	console.info("Scheduling Next Feed Poll:" + feedPollerFrequency);
	feedPollerTimerId = window.setTimeout(function(){
		helios_pollFeed(function(){
			helios_repoll();
		});		
	}, feedPollerFrequency);	
}

/**
 * Stops the feed poller
 */
function helios_stopPoller() {
	if(!feedPollerRunning) return;
	window.clearTimeout(feedPollerTimerId);
	feedPollerTimerId = null;
	feedPollerRunning = false;
	growl("Feed Poller Stopped");
}




/**
 * Polls the metric feed for push messages
 * @param an optional callback 
 * @param maxitems Optional maximum number of items
 * @return the polled data
 */
function helios_pollFeed(callback) {	
	jQuery.ajax({
		url: 'api/sub/poll/' + pollFeedTimeout + '/' + pollFeedMaxItems ,   // + '?serial=' + $.now()
		cache: false,
		dataType: 'json',
		error: function(err) {
			console.error("Failed to poll:" + err);
			console.dir(err);
			if(callback!=null) {
				//callback();
			}			
		},
		204: function() {
			if(callback!=null) {
				callback();
			}
		},
		success: function(json) {
			if(json==null) {
				if(callback!=null) {
					callback();					
				}
				return;
			}
			var batchArray = json['batch'];
			if(batchArray==null) {
				console.error('Feed Response had no batch header');
				console.dir(json);
				return;
			}
			var size = 0;
            var items = 0;
            var itemMap = {};
            totalFeedBatchCount += batchArray.length;
            $('#feedBatchCount').val(totalFeedBatchCount);
			$.each(batchArray, function(i, feed){
				$.each(feed, function(feedName, feedItems) {
					size += feedItems.length;
					map = itemMap[feedName];
					if(map==null) {
						map = [];
						itemMap[feedName] = map;
					}
					$.merge(map, feedItems);					
				});
			});
			growl("Feed Poller Event", {"Batch" : batchArray.length, "Items" : size});			
			console.dir(itemMap);
			if(callback!=null) {
				callback(json);
			}
		}
	});	
}
/*
 * ==========================================================
 * 		Callback Events
 * ==========================================================
 */
function onSignIn(session) {
	signedIn = true;
	subscriberStarted = false;
	sessionId = session;
	
	$('#sessionId').val(sessionId);
    console.info("SessionID Acquired:%s", sessionId);
    $('.signin-show').show();
    $('.signin-hide').hide();
	try {
		try { jmxClient = new Jolokia("/helios/jmx"); } catch (e) {}
		if(jmxClient==null) {
			$.getScript("js/jolokia/jolokia-min.js");
			$.getScript("js/jolokia/jolokia-simple-min.js", function() {
				jmxClient = new Jolokia("/helios/jmx");
			});
			$.getScript("js/jolokia/json2-min.js");
		}
		
	} catch (e) {
		console.error("Failed to init jolokia client:" + e);
	}
    
        
    
}

function onSignOut(session) {
	signedIn = false;
	subscriberStarted = false;
	totalFeedBatchCount = 0;
	helios_stopPoller();
	activeFeeds = {};
	if(session=="NOSESSION") {
		console.info("No session to signout from");
	} else {
		console.info("Session [%s] signed out", session);
	}
	$('#sessionId').val("");
	sessionId = null;
    $('.signout-hide').hide();
    $('.signout-show').show();	
}

function onStartSubscriber(response) {
	subscriberStarted = true;
	$('.startsub-show').show();
	$('.startsub-hide').hide();
	if(response==sessionId) {
		console.info("Subscriber for session [%s] started", response);
		subscriberSessionObjectName = subscriberSessionObjectTemplate + sessionId;
	} else {
		console.info("Subscriber Start Failed [%s]", response);
	}
}

function onStopSubscriber(response) {
	subscriberStarted = false;	
	$('.stopsub-show').show();
	$('.stopsub-hide').hide();
	if(response=="OK") {
		console.info("Subscriber for session [%s] stopped", response);
	} else {
		console.info("Subscriber Stop Failed [%s]", response);
	}
	
}


function stripFs(path) {
	return path.replace(forwardSlashRegex, '%2F');
}

function growl(message, displayProperties, jGrowlProperties) {
	var content = "<p>" + message + "</p>";
	if(displayProperties!=null) {
		content += "<ul>";
		$.each(displayProperties, function(k,v){
			content += ("<li><b>" + k + "</b>&nbsp;:&nbsp;" + v + "</li>"); 
		});
		content += "</ul>";
	}
	$.jGrowl(content, jGrowlProperties);
}

function initConfigurationContext() {
	if(configurationContext==null) {
		configurationContext = $({}).attr('id', 'helios-configurationContext');
		$.helios = {};
		$.helios.configurationContext = configurationContext;
	}	
}

function initConfigurationItem(name, description, initialValue) {
	if(configurationContext==null) {
		initConfigurationContext();
	}
	
	if(configurationContext.data(name)!=null) {
		console.error("A configuration item for [%s] is already registered.", name);
		return;
	}
	configurationContext.data(name, {'value': initialValue, 'description' : description==null ? "Configuration for " + name : description});

	var method = 'function get' + name + '() {  return jQuery.helios.configurationContext.data("' + name + '")["value"]; };';
	method += 'function set' + name + '(value) { ';
	method += 'if(get' + name + '()==value) return;';
	method += 'jQuery.helios.configurationContext.data("' + name + '")["value"]=value;';
	method += 'jQuery.publish("' + name + 'Change", [value]);';
	method += '};'	
	
	$.globalEval(method);
	
	
	
}



/*

console.clear();
//helios_startFeed("metric-feed", {'mask': 'helios.metrictree.njw810.*.>'});
//helios_stopFeed("metric-feed");

//helios_stopPoller();


initConfigurationItem("FeedPollerEnabled", "Indicates if the Feed Poller Is Enabled", false);

$.subscribe("FeedPollerEnabledChange", function(e, val) {
    console.info("FeedPollerEnabledChange:" + val);
});

setFeedPollerEnabled(false);
setFeedPollerEnabled(true);
//console.info("Testing FeedPollerEnabled:" + getFeedPollerEnabled());


	defaults: {
			pool: 			0,
			header: 		'',
			group: 			'',
			sticky: 		false,
			position: 		'top-right',
			glue: 			'after',
			theme: 			'default',
			themeState: 	'highlight',
			corners: 		'10px',
			check: 			250,
			life: 			3000,
			closeDuration:  'normal',
			openDuration:   'normal',
			easing: 		'swing',
			closer: 		true,
			closeTemplate: '&times;',
			closerTemplate: '<div>[ close all ]</div>',
			log: 			function(e,m,o) {},
			beforeOpen: 	function(e,m,o) {},
			afterOpen: 		function(e,m,o) {},
			open: 			function(e,m,o) {},
			beforeClose: 	function(e,m,o) {},
			close: 			function(e,m,o) {},
			animateOpen: 	{
				opacity: 	'show'
			},
			animateClose: 	{
				opacity: 	'hide'
			}
		},
		
		
		
console.clear();
helios_startFeed("metric-feed", {'routerKey': 'helios.metrictree.njw810.*.>'});
//helios_stopFeed("metric-feed", 'metric-feed-1', function(a) {console.info(a)});		

 */

