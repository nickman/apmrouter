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
		requestId : 0
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
	    this.c.sendWho();
	    $(document).trigger('status.connected',[true]);

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
		this.config.requestId++;
		if(callback!=null) {
			var topic = '/' + req.t + '/' + this.config.requestId;
			$.oneTime(topic, callback);
			//console.info("Registered Callback for oneTime [%s]", topic);
		}
		req['rid']=this.config.requestId;
		this.config.ws.send(JSON.stringify(req));
		return this.config.requestId;
	},
	
	$.apmr.sendWho = function() {
		return this.send({'t': 'who', 'agent' : 'Anonymous'});
	},
	
	$.apmr.svcOp = function(svc, op, args, callback) {
		var req = {'t': 'req', 'svc' : svc, 'op' : op};
		if(args!=null) {
			req['args'] = args;
		}
		return this.send(req, callback);
	},
	$.apmr.sub = function(op, type, esn, filter, ex) {
		var req = {'t': 'req', 'svc' : 'sub', 'op' : op};
		var args = {'es' : type, 'esn': esn, 'f' : filter};
		if(ex!=null) {
			args['exf'] = ex;
		}
		req['args'] = args;
		console.info("Sending Sub Request:%o", req);
		return this.send(req);
		
		//  $.apmr.sub("start", "jmx", "service:jmx:local://DefaultDomain", "java.lang:type=GarbageCollector,name=*")
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
		$.apmr.svcOp("catalog", "nq", {name:"hostsByDomain", p : {'domain': domain}}, callback || function(data){
			console.info("hostsByDomain Response:%o", data);
		});
	},
	$.apmr.findOnlineHosts = function() {
		$.apmr.svcOp("catalog", "nq", {name:"findOnlineHosts"}, function(data){
			console.info("findOnlineHosts Response:%o", data);
		});
	},
	
	$.apmr.agentsByHost = function(hostId, callback) {
		$.apmr.svcOp("catalog", "nq", {name:"agentsByHost", p : {'hostId': hostId}}, callback || function(data){
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
	}
	
	
	
	
	
	
	
})(jQuery);
