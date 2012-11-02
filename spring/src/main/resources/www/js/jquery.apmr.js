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
		/** The connection timeout in ms. */
		connectionTimeout : 5000,
		/** The session Id, set when connected, cleared when disconnected  */
		sessionId : "",
		/** The request ID counter */
		requestId : 0
	},
	
	$.apmr.connect = function(callback) {
		// send connecting event
		this.connecting = true;		
		var cli = this;
		this.config.connectTimeoutHandle = setTimeout(function(){$.apmr.onConnectionTimeout();},this.config.connectionTimeout);
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
		console.info("Connect timeout");
		// send connect timeout event
		connecting = false;
		connectTimeoutHandle = -1;
	},
	
	
	$.apmr.onOpen = function() {
	    console.info("WebSocket Opened");
	    clearTimeout(this.c.config.connectTimeoutHandle);
	    this.c.config.connectTimeoutHandle = -1;
	    this.c.sendWho();
	    $(document).trigger('status.connected',[true]);

	},
	$.apmr.onError = function(e) {
		console.info("WebSocket Error");
		console.dir(e);		
	},
	$.apmr.onClose = function() {
		console.info("WebSocket Closed"); 
		$(document).trigger('status.connected',[false]);
	},
	$.apmr.onMessage = function(event) {
		try {
			var json = JSON.parse(event.data);
			if(json.sessionid !=null) {
				this.c.sessionId = json.sessionid;
				console.info("Set SessionID [%s]", this.c.sessionId);
			}
			console.dir(json);
			var topic = '/' + 'req' + '/' + json.rerid;
			$.publish(topic, [json]);
		} finally {
		}		
	},
	$.apmr.send = function(req, callback) {
		this.config.requestId++;
		if(callback!=null) {
			var topic = '/' + req.t + '/' + this.config.requestId;
			$.oneTime(topic, callback);
			console.info("Registered Callback for oneTime [%s]", topic);
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
	$.apmr.findAllHosts = function(callback) {
		$.apmr.svcOp("catalog", "nq", {name:"findAllHosts"}, callback || function(data){
			console.info("FindAllHosts Response:%o", data);
		});
	},
	$.apmr.findOnlineHosts = function() {
		$.apmr.svcOp("catalog", "nq", {name:"findOnlineHosts"}, function(data){
			console.info("findOnlineHosts Response:%o", data);
		});
	},
	
	$.apmr.findAgentsByHost = function(hostId, callback) {
		$.apmr.svcOp("catalog", "nq", {name:"findAgentsByHost", p : {'hostId': hostId}}, callback || function(data){
			console.info("findAgentsByHost Response:%o", data);
		});		
	},
	$.apmr.findMinLevelMetricsForAgent = function(agentId) {
		$.apmr.svcOp("catalog", "nq", {name:"findMinLevelMetricsForAgent", p : {'agentId': agentId}}, function(data){
			console.info("findMinLevelMetricsForAgent Response:%o", data);
		});						
	},
	$.apmr.metricNodesForAgent = function(agentId, namespace) {
		$.apmr.svcOp("catalog", "nq", {name:"metricNodesForAgent", p : {'agentId': agentId, 'namespace' : namespace}}, function(data){
			console.info("metricNodesForAgent Response:%o", data);
		});						
	},
	$.apmr.metricParentsForAgent = function(agentId, parent) {
		$.apmr.svcOp("catalog", "nq", {name:"metricParentsForAgent", p : {'agentId': agentId, 'parent' : parent}}, function(data){
			console.info("metricParentsForAgent Response:%o", data);
		});						
	},
	$.apmr.rootMetricsForAgent = function(agentId, callback) {
		$.apmr.svcOp("catalog", "nq", {name:"rootMetricsForAgent", p : {'agentId': agentId}}, callback || function(data){
			console.info("rootMetricsForAgent Response:%o", data);
		});						
	},
	$.apmr.rootPlusMetricsForAgent = function(agentId, root) {
		$.apmr.svcOp("catalog", "nq", {name:"rootPlusMetricsForAgent", p : {'agentId': agentId, 'root': root}}, function(data){
			console.info("rootPlusMetricsForAgent Response:%o", data);
		});						
	}
	
	
	
	
	
	
})(jQuery);
