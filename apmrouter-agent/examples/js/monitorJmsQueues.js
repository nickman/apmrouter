/**
 * Monitoring script to monitor JBoss 4.x queue depths
 * Whitehead, Oct 1, 2012
 */
function isNumber(v) {try { var i = parseInt(v); return !isNaN(i); } catch (e) { return false; }}
var query = {mbs:'jboss', on:'com.ecs.jms.destinations:*', attrs:[
	/* jboss queues */	'MessageCount', 'DeliveringCount', 'ScheduledMessageCount', 'ConsumerCount',
	/* jboss topics */	'DurableMessageCount', 'NonDurableMessageCount', 'DurableSubscriptionsCount', 'NonDurableSubscriptionsCount',
	/* wmq queues */	 'QueueDepth', 'QueueName', 'OpenOutputCount', 'OpenInputCount'
]};
	
if(jmx.isRegistered('jboss', 'com.ecs.jms.destinations:*')) {	
	var r = jmx.getAttributes(query);	
	if(r!=null) {		
		for(var i = 0, m = r.results.length; i < m; i++) {
			rez = r.results[i];  // p has name, service and type
			for(d in rez.data) {				
				if(isNumber(rez.data[d])) {				
					tracer.traceGauge(rez.data[d], d, ["platform=JBoss", "category=JMS", "service=" + rez.p.service, "type=" + rez.p.type, "name=" + rez.p.name]);
				}
			}
		}
	}
}

pout.println("JMS Monitor OK");
