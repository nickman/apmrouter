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
sender = SenderFactory.getInstance().getDefaultSender();
sender.registerMetricURISubscriptionEventListener(listener);