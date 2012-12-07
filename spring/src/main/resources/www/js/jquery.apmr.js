/**
 * APMRouter jQuery Plugin
 * Whitehead, Helios Development Group
 */

(function($){
	$.apmr = {};
	$.apmr.config =  {
		/** The websocket URL that connects back from whence the document came */
		wsUrl : 'ws://' + document.location.host + '/ws',
		/** The websocket instance */
		ws : false,
		/** The connecting state indicator  */
		connecting : false,
		/** A handle to the timeout set to timeout the connection attempt */
		connectTimeoutHandle : -1,
		/** A handle to the timeout for the reconnect attempt */
		reconnectTimeoutHandle : -1,
		
		/** The connection timeout in ms. */
		connectionTimeout : 5000,
		/** The pause time before attempting a reconnect in ms. */
		reconnectPauseTime : 3000,
		
		/** The session Id, set when connected, cleared when disconnected  */
		sessionId : "",
		/** The request ID counter */
		requestId : 0,
		/** A repository of subscriptions keyed by sub key */
		subsBySubKey: {},
		/** A repository of subscriptions keyed by req Id */
		subsByReqId: {},
		
		/** Indicates if down nodes (hosts and agents) should be loaded in the tree when expanding */
		loadDownNodes: true,
		/** Indicates if downed nodes (hosts and agents) should be removed from the tree when timed out */
		unloadDownNodes: false,
		/** The amount of time in ms. that downed nodes (hosts and agents) will linger in the tree before being unloaded */
		downedNodeLingerTime: 15000,
		/** The handle of the downed node reaper */
		downedNodeReaper: -1,
		/** The last sequence number received */
		lastSequence: -1
		
	},
	
	$.apmr.isReconnectScheduled = function() {
		return ($.apmr.config.reconnectTimeoutHandle != -1);
	},
	
	$.apmr.isConnectingOrConnected = function() {
		if($.apmr.config.ws) {
			var readyState = $.apmr.config.ws.readyState;
			if(readyState!=null && (readyState==0 || readyState==1)) return true;
		}
		return false;		
	},
	
	$.apmr.startReconnectLoop = function() {
		if($.apmr.isConnectingOrConnected() || $.apmr.isReconnectScheduled()) return;
		$.apmr.config.reconnectTimeoutHandle = setTimeout(function(){
			$.apmr.config.reconnectTimeoutHandle = -1;
			$(document).trigger('status.reconnect.attempt',[]);
			$.apmr.connect(function(){ // callback called when reconnect times out
				if($.apmr.isConnectingOrConnected()) return;
				if($.apmr.config.ws) {
					$.apmr.config.ws.close();
				}
				//$.apmr.config.reconnectTimeoutHandle = $.apmr.startReconnectLoop();
			});
		}, $.apmr.config.reconnectPauseTime);
	},
	
	$.apmr.connect = function(callback) {
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
	},
	
	/**
	 * Called when the connection attempt timesout
	 */
	$.apmr.onConnectionTimeout = function() {
		if($.apmr.isConnectingOrConnected() || $.apmr.isReconnectScheduled()) return;
		console.info("Connect timeout");
		// send connect timeout event
		$.apmr.config.connecting = false;
		$.apmr.config.connectTimeoutHandle = -1;
		if($.apmr.config.ws) $.apmr.config.ws.close();
	},
	
	
	$.apmr.onOpen = function() {
	    console.info("WebSocket Opened");
	    clearTimeout(this.c.config.connectTimeoutHandle);
	    clearTimeout($.apmr.config.reconnectTimeoutHandle);
	    this.c.config.connectTimeoutHandle = -1;
	    //this.c.sendWho();
	    $(document).trigger('status.connected',[true]);
	    $.apmr.subtreeOn();

	},
	$.apmr.onError = function(e) {
		console.info("WebSocket Error");
		console.dir(e);		
	},
	$.apmr.onClose = function() {
		$.apmr.config.ws = false;
		console.info("WebSocket Closed"); 
		$(document).trigger('status.connected',[false]);
		$.apmr.startReconnectLoop();
	},
	$.apmr.onMessage = function(event) {
		try {
			var json = JSON.parse(event.data);
			if(json.sessionid !=null) {
				this.c.sessionId = json.sessionid;
				console.info("Set SessionID [%s]", this.c.sessionId);
				$(document).trigger('connection.session',[this.c.sessionId]);
			} else {
				var topic = '/' + 'req' + '/' + json.rerid;
				$.publish(topic, [json]);
			}
		} finally {
		}		
	},
	$.apmr.send = function(req, callback) {
		var rid = this.config.requestId++;
		if(callback!=null) {
			var topic = '/' + req.t + '/' + rid;
			$.oneTime(topic, callback);
			//console.info("Registered Callback for oneTime [%s]", topic);
		}
		req['rid']=rid;
		this.config.ws.send(JSON.stringify(req));
		return rid;
	},
	$.apmr.sendSub = function(req, subCallback, handshakeCallback) {
		var rid = this.config.requestId++;
		var topic = '/' + req.t + '/' + rid;
		if(handshakeCallback!=null) {			
			$.oneTime(topic, handshakeCallback);			
		}
		if(subCallback!=null) {			
			$.subscribe(topic, subCallback);			
		}		
		req['rid']=rid;
		this.config.ws.send(JSON.stringify(req));
		return rid;
	},
	
	
	$.apmr.sendWho = function() {
		return this.send({'t': 'who', 'agent' : 'Anonymous'});
	},
	
	$.apmr.svcOp = function(svc, op, args, callback) {
		var req = {'t': 'req', 'svc' : svc, 'op' : op};
		if(args!=null) {
			req['args'] = args;
		}
		return $.apmr.send(req, callback);
	},
	
	// subsBySubKey: {},
	$.apmr._subStore = function(type, esn, filter, ex, callback) {
		var subKey = type + '/' + esn + '/' + filter + '/' + (ex || '');
		if($.apmr.config.subsBySubKey[subKey]==null) {						
			var sub = {
				'type' : type,
				'esn' : esn,
				'filter' : filter,
				'ex' : ex,
				'callback' : callback,
				'key' : subKey
				// unsubkey
				// topic
				// timestamp
			};
			$.apmr.config.subsBySubKey[subKey] = sub;
			return sub;
		}
		return null;
	}
	
	/**
	 * Initiates a subscription.
	 * @param subprops: <ul>
	 * 	<li><b>sourcetype</b>: The event source type, e.g. "jmx". <b>Mandatory</b></li>
	 * 	<li><b>sourcename</b>: The event source name, e.g. "service:jmx:local://DefaultDomain". <b>Mandatory</b></li>
	 *  <li><b>op</b>: The operation to execute, i.e. "start" or "stop". <b>Mandatory</b></li>
	 * 	<li><b>filter</b>: The event filter specifier, e.g. "org.helios.apmrouter.session:service=SharedChannelGroup". <b>Mandatory</b></li>
	 * 	<li><b>xfilter</b>: An optional extended filter, e.g. and array of JMX notification types</li>
	 *  <li><b>args</b>: Optional additional arguments in the form of properties.</li>
	 * 	<li><b>subhandler</b>: The callback handler that will receive subscribed events</li>
	 * 	<li><b>confirmhandler</b>: The callback handler that will receive the confirmation of a subscription operation or a timeout notification
	 * which indicates the subscription request failed.</li>
	 * </ul>
	 */
	
	$.apmr.sub = function(callback, op, type, esn, filter, ex) {
		var sub = $.apmr._subStore(type, esn, filter, ex, callback);
		if(sub==null) return;
		var req = {'t': 'req', 'svc' : 'sub', 'op' : op};
		var args = {'es' : type, 'esn': esn, 'f' : filter};
		if(ex!=null) {
			if($.isArray(ex)) {
				args['stf'] = ex;
			} else {
				args['exf'] = ex;
			}
			
		}
		req['args'] = args;
		var cb = callback;		
				
		var rid = $.apmr.send(req, function(data){
			var unsubKey = $.subscribe(topic, cb);			
			sub['ts'] = new Date().getTime();		
			sub['subId'] = data.msg;
		});
		$.apmr.config.subsByReqId[rid] = sub;
		sub['rid'] = rid;
		var topic = '/' + 'req' + '/' + rid;
		sub['topic'] = topic;
		return sub;
	},
	
	$.apmr.unsub = function(reqId) {
		var sub = $.apmr.config.subsByReqId[reqId];
		if(sub==null) return;
		delete $.apmr.config.subsByReqId[reqId];
		delete $.apmr.config.subsBySubKey[sub.key];
		$.unsubscribeTopic(sub.topic);
		var req = {'t': 'req', 'svc' : 'sub', 'op' : 'stop', 'args' : {'subId' : sub.subId}};
		$.apmr.send(req, function(data){
			console.info("Unsubscribed from [%s]", sub.key);
		});
		
		
		
		
	},
	
	
	$.apmr.subtreeOn = function(callback) {
		var sub = $.apmr.sub(callback || $.apmr.stateChange, "start", "jmx", "service:jmx:local://DefaultDomain", "org.helios.apmrouter.session:service=SharedChannelGroup");
		if(sub==null) {  // restart the sub.
			console.warn("Need to restart tree sub");
		} else {
			console.info("Started Sub [#%s] for [%s]-[%s]", sub.topic, "service:jmx:local://DefaultDomain", "org.helios.apmrouter.session:service=SharedChannelGroup");
			var subHandle = $.subscribe(sub.topic, callback || $.apmr.stateChange);
			sub.subHandle = subHandle;
		}
	},
	$.apmr.subMetricOn = function(metricId, callback) {		
		var sub = $.apmr.sub(callback || $.apmr.liveMetric, 
				"start", 
				"jmx", 
				"service:jmx:local://DefaultDomain", 
				"org.helios.apmrouter.destination:service=H2TimeSeriesDestination,name=H2TimeSeriesDestination",
				['apmrouter.h2timeseries.intervalroll.' + metricId]
		);
		if(sub!=null) {
			console.info("Started Sub [#%s] for [%s]-[%s]", sub.topic, "service:jmx:local://DefaultDomain", "org.helios.apmrouter.destination:service=H2TimeSeriesDestination,name=H2TimeSeriesDestination");
			var subHandle = $.subscribe(sub.topic, callback || $.apmr.stateChange);
			sub.subHandle = subHandle;
			return sub['rid'];
		} else {
			console.warn("Need to restart metric sub");
		}
	},
	$.apmr.subMetricOff = function(reqId, callback) {
		$.apmr.unsub(reqId);
	},
	
	
	$.apmr.liveMetric = function(event) {
		console.info("LIVE METRIC:%o", event);
	},
	// apmrouter.h2timeseries.intervalroll
	$.apmr.stateChange = function(event) {
		if(event.msg.sequenceNumber<=$.apmr.config.lastSequence) return;
		$.apmr.config.lastSequence = event.msg.sequenceNumber; 
		console.info("Sub Response [%o]", event);
		var e = event.msg;
		if(e.userData==null) return;
		try {
			switch(e.type) {
				case "apmrouter.session.start":
					break;  // nuthin'
				case "apmrouter.session.identified":	
					$.jGrowl('[' + e.userData.h + ' / ' + e.userData.a + ']', { header: 'Agent Started' }); 
					if($('#host-' + e.userData.hi).length==0) {
						var domainId = '#domain-' + (e.userData.d.join('_'));
						// ==================================================================================
						//  Adding new domain if not present into the tree
						// ==================================================================================
						
						if($(domainId).length<1) {
							var uid = 'domain-' + (e.userData.d.join('_'));
							$("#metricTree").jstree("create", $('#root'), "inside" , {
								attr: {id: uid, rel: "domain", 'domain' : e.userData.d.join('.')},  
								data : {title: e.userData.d.join('.')}
							}, false, true);
						}
						
						// ==================================================================================
						//  Adding a new host into the tree
						// ==================================================================================						
						
						var hostName = e.userData.h.split('.').pop();
						$("#metricTree").jstree("create", $(domainId), "inside" , {
							attr: {id: "host-" + e.userData.hi, rel: "server", 'host' : e.userData.hi},  
							data : {title: hostName}
						}, false, true);
						$("#host-" + e.userData.hi).removeClass('jstree-leaf').addClass('jstree-closed');						
					} else {
						// ==================================================================================
						//  Marking a host in the tree as UP
						// ==================================================================================						
						console.info("Marking host [%s] UP", e.userData.h);
						$('#host-' + e.userData.hi).attr('rel', 'server').removeAttr('apmr_dnode_downTime');
					}
					var hostId = '#host-' + e.userData.hi;
					var agentId = "agent-" + e.userData.ai;
					if($('#' + agentId).length==0 && !parentContainsChild(hostId, agentId)) {
						// ==================================================================================
						//  Adding a new agent into the tree
						// ==================================================================================
						$("#metricTree").jstree("create", $(hostId), "inside" , {
							attr: {id: agentId, rel: "online-agent", agent : e.userData.ai},  
							data : {title: e.userData.a}
						}, function(){
							$("#" + agentId).removeClass('jstree-leaf').addClass('jstree-closed');
						}, true);
						console.info("Added Agent [%s]", agentId);
					} else {
						// ==================================================================================
						//  Marking an agent in the tree as UP
						// ==================================================================================						
						console.info("Marking agent [%s] UP", e.userData.a)
						$('#agent-' + e.userData.ai).attr('rel', 'online-agent').removeAttr('apmr_dnode_downTime');						
					}
					break;
				case "apmrouter.session.end":
					$.jGrowl('[' + e.userData.h + ' / ' + e.userData.a + ']', { header: 'Agent Stopped' });
					var ts = new Date().getTime();
					if($('#host-' + e.userData.hi).length==1 && e.userData.hc) {
						// ==================================================================================
						//  Marking a host in the tree as DOWN
						// ==================================================================================												
						console.info("Marking host [%s] DOWN", e.userData.h);
						var hostId = '#host-' + e.userData.hi;
						$(hostId).attr('rel', 'down-server').attr('apmr_dnode_downTime', ts);
						// ==================================================================================
						//  Mark all the agents down too.
						// ==================================================================================												
						metricTree._get_children(hostId).attr('rel', 'agent');
						metricTree._get_children(hostId).not('[apmr_dnode_downTime]').attr('apmr_dnode_downTime', ts);
					}				
					if($('#agent-' + e.userData.ai).length==1) {
						// ==================================================================================
						//  Marking an agent in the tree as DOWN
						// ==================================================================================						
						console.info("Marking agent [%s] DOWN", e.userData.a)
						$('#agent-' + e.userData.ai).attr('rel', 'agent').not('[apmr_dnode_downTime]').attr('apmr_dnode_downTime', ts);																		
					}
					break;
				default :
					// nothin'
					
			}
		} catch (e) {
			console.error("State Change Handler Event Error [%o], %o", e, e.stack);
		}
			
	},
	
	$.apmr.ams = function(agentId, callback) {
		$.apmr.svcOp("catalog", "ams", {'agentId': agentId}, callback || function(data){
			console.info("ams Response:%o", data);
		});
	},
	$.apmr.allDomains = function(callback) {
		$.apmr.svcOp("catalog", "nq", {name:"allDomains"}, callback || function(data){
			console.info("allDomains Response:%o", data);
		});
	},
	
	$.apmr.hostsByDomain = function(domain, callback) {
		var queryName = null;
		if($.apmr.config.loadDownNodes) {
			queryName = 'hostsByDomain';
		} else {
			queryName = 'upHostsByDomain';
		}
		$.apmr.svcOp("catalog", "nq", {name:queryName, p : {'domain': domain}}, callback || function(data){
			console.info("hostsByDomain Response:%o", data);
		});
	},
	
	$.apmr.agentsByHost = function(hostId, callback) {
		var queryName = null;
		if($.apmr.config.loadDownNodes) {
			queryName = 'agentsByHost';
		} else {
			queryName = 'upAgentsByHost';
		}		
		$.apmr.svcOp("catalog", "nq", {name:queryName, p : {'hostId': hostId}}, callback || function(data){
			console.info("agentsByHost Response:%o", data);
		});		
	},
	$.apmr.findMinLevelMetricsForAgent = function(agentId, callback) {
		$.apmr.svcOp("catalog", "nq", {name:"findMinLevelMetricsForAgent", p : {'agentId': agentId}}, callback || function(data){
			console.info("findMinLevelMetricsForAgent Response:%o", data);
		});						
	},
	$.apmr.findLevelMetricsForAgent = function(level, agentId, callback) {
		$.apmr.svcOp("catalog", "nq", {name:"findLevelMetricsForAgent", p : {'level' : level, 'agentId': agentId}}, callback || function(data){
			console.info("findLevelMetricsForAgent Response:%o", data);
		});								
	},
	$.apmr.findLevelFoldersForAgent = function(level, agentId, parent, callback) {
		$.apmr.svcOp("catalog", "nq", {name:"findLevelFoldersForAgent", p : {'level' : level, 'agentId': agentId, 'parent' : parent || ''}}, callback || function(data){
			console.info("findLevelFoldersForAgent Response:%o", data);
		});						
	},
	$.apmr.findLevelMetricsForAgentWithParent = function(level, agentId, parent, callback) {
		$.apmr.svcOp("catalog", "nq", {name:"findLevelMetricsForAgentWithParent", p : {'level' : level, 'agentId': agentId, 'parent' : parent}}, callback || function(data){
			console.info("findLevelMetricsForAgentWithParent Response:%o", data);
		});						
	},
	$.apmr.liveData = function(ids, callback) {
		return $.apmr.svcOp("h2ts", "liveData", {'IDS':$.isArray(ids) ? ids : [ids]}, callback || function(data){
			console.info("Live Data Response:%o", data);
		});						
	},
	$.apmr.metricById = function(metricId, callback) {
		$.apmr.svcOp("catalog", "nq", {name:"metricById", p : {'metricId' : metricId}}, callback || function(data){
			console.info("metricById Response:%o", data);
		});										
	}
	
	
	
	
	
	
	
	
})(jQuery);
