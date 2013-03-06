import org.helios.apmrouter.trace.*;
import org.helios.apmrouter.metric.*;
import org.helios.apmrouter.sender.*;
import org.helios.apmrouter.subscription.*;
System.setProperty("org.helios.agent", "GroovyAgent");

class MListener implements MetricURISubscriptionEventListener {
    public void onNewMetric(Object newMetric) {
        println "NEW METRIC:${newMetric.dump()}";
    }
    public void onMetricStateChangeEntry(Object metric) {
        println "METRIC ENTRY:${metric.dump()}";
    }
    public void onMetricStateChangeExit(Object metric) {
        println "METRIC EXIT:${metric.dump()}";
    }
    public void onMetricData(Object metricData) {
        println "METRIC DATA:${metricData.dump()}";
    }
    public void onMetricStateChange(Object metric) {
        println "METRIC STATE CHANGE:${metric.dump()}";
    }
    
}
listener = new MListener();
//println Class.forName("org.jboss.netty.buffer.ChannelBuffer").getProtectionDomain().getCodeSource().getLocation();

sender = SenderFactory.getInstance().getDefaultSender();
if(sender.subListeners.isEmpty()) {
    sender.subscribeMetricURI("DefaultDomain/njw810/GroovyAgent/groovy/random/ints", listener);
    Thread.sleep(2000);
}

//sender.unSubscribeMetricURI("com.cpex/ne-wk-nwhi-01/GroovyAgent/groovy/random/ints");
sender.registerMetricURISubscriptionEventListener(listener);
RANDOM = new Random(System.currentTimeMillis());
randInt = { return Math.abs(RANDOM.nextInt(100)); }

tracer = TracerFactory.getTracer();
println tracer.trace(randInt(), "Foo1", MetricType.LONG_COUNTER, "groovy", "random", "ints");
println tracer.trace(randInt(), "Foo2", MetricType.LONG_COUNTER, "groovy", "random", "ints");
return null;