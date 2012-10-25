/**
 * Helios APMRouter JavaScript Client
 * Whitehead, 2012 
 */

/**
 * The main class definition for the client.
 */
var apmr =  {
	/** The websocket URL that connects back from whence the document came */
	wsUrl : 'ws://' + document.location.host + '/ws',
	/** The websocket instance */
	ws : null,
	/** The connecting state indicator  */
	connecting : false,
	/** A handle to the timeout set to timeout the connection attempt */
	connectTimeoutHandle : -1,
	/** The connection timeout in ms. */
	connectionTimeout : 5000,
	/** The session Id, set when connected, cleared when disconnected  */
	sessionId : "",
	/** The request ID counter */
	requestId : 0,
	
	connect : function() {
		// send connecting event
		connecting = true;
		var cli = apmr;
		this.connectTimeoutHandle = setTimeout(function(){cli.onConnectionTimeout();},this.connectionTimeout);
		setTimeout(function(){
			cli.ws = new WebSocket(cli.wsUrl);
			cli.ws.c = cli;
	        cli.ws.onopen = cli.onOpen;
		    cli.ws.onerror = cli.onError;
		    cli.ws.onclose = cli.onClose;
		    cli.ws.onmessage = cli.onMessage;
		},1);
		console.info("Connecting.....");
	},
		
	/**
	 * Called when the connection attempt timesout
	 */
	onConnectionTimeout : function() {
		console.info("Connect timeout");
		// send connect timeout event
		connecting = false;
		connectTimeoutHandle = -1;
	},
	
	
	onOpen : function() {
	    console.info("WebSocket Opened");
	    clearTimeout(this.c.connectTimeoutHandle);
	    this.c.connectTimeoutHandle = -1;
	    this.c.sendWho();
	},
	onError : function(e) {
		console.info("WebSocket Error");
		console.dir(e);		
	},
	onClose : function() {
		console.info("WebSocket Closed"); 
	},
	onMessage : function(event) {
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
	
	send : function(req) {
		this.requestId++;
		req['rid']=this.requestId;
		this.ws.send(JSON.stringify(req));
		return this.requestId;
	},
	
	sendWho : function() {
		return this.send({'t': 'who', 'agent' : 'Anonymous'});
	},
	
	svcOp : function(svc, op) {
		var req = {'t': 'req', 'svc' : svc, 'op' : op};
		return this.send(req);
	},
	sub : function(op, type, esn, filter, ex) {
		var req = {'t': 'req', 'svc' : 'sub', 'op' : op};
		var args = {'es' : type, 'esn': esn, 'f' : filter};
		if(ex!=null) {
			args['exf'] = ex;
		}
		req['args'] = args;
		console.info("Sending Sub Request:%o", req);
		return this.send(req);
		
		//  apmr.sub("start", "jmx", "service:jmx:local://DefaultDomain", "java.lang:type=GarbageCollector,name=*")
	}
	
	
}