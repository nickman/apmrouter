/**
 * Monitoring script to monitor JBoss 4.x queue depths
 * Whitehead, Oct 1, 2012
 */


mbs = jmx.getLocalMBeanServer("jboss");
try {
  if(mbs!=null) {    
    onArr = jmx.query(mbs, "jboss.mq.destination:service=Queue,*");
    for(var i = 0, l = onArr.length; i < l; i++) {
	on = onArr[i];
	var name = mbs.getAttribute(on, "Name");
	var depth = mbs.getAttribute(on, "QueueDepth");
	tracer.traceLong(depth, "QueueDepth", "platform=JBoss", "category=JMS", "resource=Queue", "name=" + name);
    }
  } else {
    pout.println("No jboss mbeanserver yet"); 
  }
} catch (e) {
  pout.println("Monitor failed:" + e); 
}
