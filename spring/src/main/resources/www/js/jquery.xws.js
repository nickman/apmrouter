/**
 * @author Nicholas Whitehead
 */
(function($){
$.extend({
	websocketSettings: {
		open: function(){},
		close: function(){},
		message: function(){},
		options: {},
		events: {}
	},
	websocket: function(url, s) {
		var ws = WebSocket ? new WebSocket( url ) : {
			send: function(m){ return false },
			close: function(){}
		};
		$(ws)
			.bind('open', $.websocketSettings.open)
			.bind('close', $.websocketSettings.close)
			.bind('message', $.websocketSettings.message)
			.bind('message', function(e){
				var m = $.evalJSON(e.originalEvent.data);
				var h = $.websocketSettings.events[m.type];
				if (h) h.call(this, m);
			});
		
		ws._settings = $.extend($.websocketSettings, s);
		ws._send = ws.send;
		ws.send = function(type, data) {
			var m = {type: type};
			m = $.extend(true, m, $.extend(true, {}, $.websocketSettings.options, m));
			if (data) m['data'] = data;
			return this._send($.toJSON(m));
		}
		$(window).unload(function(){ ws.close(); ws = null });
		return ws;
	}
});
})(jQuery);


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
SubscriptionClosedHandler[]


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
	
