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
	
	connect : function() {
		// send connecting event
		connecting = true;
		var cli = apmr;
		connectTimeoutHandle = setTimeout(function(){cli.onConnectionTimeout();},this.connectionTimeout);
		setTimeout(function(){
			cli.ws = new WebSocket(cli.wsUrl);
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
	
	onConnect : function() {
		console.info("Connected");
		// send connected event
		clearTimeout(connectTimeoutHandle);
		connecting = false;
		connectTimeoutHandle = -1;
	},
	
	onOpen : function() {
	    console.info("WebSocket Opened");
	    //sendWho();
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
			console.dir(json);
		} finally {
		}		
	}
}