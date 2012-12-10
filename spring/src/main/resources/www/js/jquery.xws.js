/**
 * Extended and modified version of http://code.google.com/p/jquery-websocket/
 * to support the more specific protocols of Request/Response and Pub/Sub, plus request timeout.
 * @author Nicholas Whitehead
 */
(function($){
$.extend({
	websocketSettings: {
		open: function(){},
		close: function(){},
		message: function(){},
		error: function(){},
		options: {},
		events: {}
	},
	requestIdFactory: 0,
	timeoutRegistry: {
		
	},
	subscriptionRegistry: {
		
	},
	websocket: function(url, s) {
		var ws = WebSocket ? new WebSocket( url ) : {
			send: function(m){ return false },
			close: function(){}
		};
		$(ws)
			.bind('open', $.websocketSettings.open)
			.bind('close', $.websocketSettings.close)
			.bind('error', $.websocketSettings.error)
			.bind('message', $.websocketSettings.message)
			.bind('message', function(e){
				var m = JSON.parse(e.originalEvent.data);
				var h = $.websocketSettings.events[m.t];
				if (h) h.call(this, m);
			});
		
		ws._settings = $.extend($.websocketSettings, s);
		ws._send = ws.send;
		ws.send = function(data) {			
			//m = $.extend(true, m, $.extend(true, {}, $.websocketSettings.options, m));
			//if (data) m['data'] = data;
			// === Add any pre-configured options to the data payload if not defined already
			var payload = $.extend({}, $.websocketSettings.options, data);
			return this._send(JSON.stringify(payload));
		}
		$(window).unload(function(){ ws.close(); ws = null });
		return ws;
	}
});
})(jQuery);

var REQ = {"rid" : 1, "t":"req","svc":"catalog","op":"nq","args":{"name":"allDomains"}};
var ws = $.websocket("ws://localhost:8087/ws", {
	events: { 
		"resp" : function(e){ console.debug("RESPONSE:%o", e); }
	}
});


/*
==================================
Single WS vs. One WS per Request ?
==================================		

========================	
Session & Global Events:
========================
WS URL
Connect Timeout Override
---------
[Statics]
---------
Connect Timeout
OnOpenEvent[]
OnCloseEvent[]
OnErrorEvent[]
OnTimeoutEvent[]
	
=================	
Request/Response:
=================
Request
ResponseHandler[]
RequestErrorHandler[]
RequestTimeoutHandler[]

=================	
Pub/Sub:
=================
SubscribeRequest
SubscribeConfirmHandler[]
SubscriptionMessageHandler[]
SubscriptionClosedHandler[]events: {


	
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

	Sample of Original:
	===================
	<script>
	var ws = $.websocket("ws://127.0.0.1:8080/", {
	        events: {
	                message: function(e) { $('#content').append(e.data + '<br>') }
	        }
	});
	$('#message').change(function(){
	  ws.send('message', this.value);
	  this.value = '';
	});
	</script>
	
	
*/