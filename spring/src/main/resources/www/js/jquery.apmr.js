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
	
	$.apmr.connect = function() {
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
		} finally {
		}		
	},
	$.apmr.send = function(req) {
		this.config.requestId++;
		req['rid']=this.config.requestId;
		this.config.ws.send(JSON.stringify(req));
		return this.config.requestId;
	},
	
	$.apmr.sendWho = function() {
		return this.send({'t': 'who', 'agent' : 'Anonymous'});
	},
	
	$.apmr.svcOp = function(svc, op) {
		var req = {'t': 'req', 'svc' : svc, 'op' : op};
		return this.send(req);
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
	}
	
	
	
})(jQuery);
