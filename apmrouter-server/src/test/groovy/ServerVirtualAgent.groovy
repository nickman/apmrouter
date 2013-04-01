import org.helios.apmrouter.trace.*;
import org.helios.apmrouter.metric.*;
import org.helios.apmrouter.sender.*;
import org.helios.apmrouter.subscription.*;
import org.helios.apmrouter.server.tracing.*;

RANDOM = new Random(System.currentTimeMillis());
randInt = { return Math.abs(RANDOM.nextInt(100)); }


va1 = ServerTracerFactory.getInstance().getTracer("org.helios.virtual", "SecretAgent1");
va2 = ServerTracerFactory.getInstance().getTracer("org.helios.virtual", "SecretAgent2");
println "Tracer Type:${va1.getClass().getName()}";
for(i in 0..100) {
	println va1.trace(randInt(), "Foo1", MetricType.LONG_COUNTER, "groovy", "random", "ints");
	println va2.trace(randInt(), "Foo2", MetricType.LONG_COUNTER, "groovy", "random", "ints");
	if(i==50) {
		va2.trace(0, "Availability", MetricType.LONG_COUNTER);
	}

	Thread.sleep(14500);
}

return null;