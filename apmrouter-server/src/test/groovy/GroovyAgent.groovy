import org.helios.apmrouter.trace.*;
import org.helios.apmrouter.metric.*;
import org.helios.apmrouter.sender.*;
import org.helios.apmrouter.subscription.*;
System.setProperty("org.helios.agent", "GroovyAgent");

class MListener implements MetricURISubscriptionEventListener {
    public void onNewMetric(Object newMetric) {
        println "NEW METRIC:${newMetric.dump()}";
    }
    public void onMetricStateChange(Object metric) {
        println "METRIC STATE CHANGE:${metric.dump()}";
    }
}
listener = new MListener();
println Class.forName("org.jboss.netty.buffer.ChannelBuffer").getProtectionDomain().getCodeSource().getLocation();

sender = SenderFactory.getInstance().getDefaultSender();
sender.subListeners.clear();
//if(sender.subListeners.isEmpty()) {
    sender.subscribeMetricURI("com.cpex/ne-wk-nwhi-01/GroovyAgent/groovy/random/ints", listener);
//}
//sender.unSubscribeMetricURI("com.cpex/ne-wk-nwhi-01/GroovyAgent/groovy/random/ints");
sender.registerMetricURISubscriptionEventListener(listener);
RANDOM = new Random(System.currentTimeMillis());
randInt = { return Math.abs(RANDOM.nextInt(100)); }

tracer = TracerFactory.getTracer();
tracer.trace(randInt(), "Foo73", MetricType.LONG_COUNTER, "groovy", "random", "ints");
return null;