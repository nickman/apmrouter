/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2007, Helios Development Group and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org. 
 *
 */
package org.helios.apmrouter.codahale.helios;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.helios.apmrouter.metric.MetricType;
import org.helios.apmrouter.trace.ITracer;
import org.helios.apmrouter.trace.TracerFactory;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.Gauge;
import com.yammer.metrics.core.Histogram;
import com.yammer.metrics.core.Metered;
import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.core.MetricPredicate;
import com.yammer.metrics.core.MetricProcessor;
import com.yammer.metrics.core.MetricsRegistry;
import com.yammer.metrics.core.Sampling;
import com.yammer.metrics.core.Summarizable;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.reporting.AbstractPollingReporter;
import com.yammer.metrics.stats.Snapshot;

/**
 * <p>Title: HeliosReporter</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.codahale.helios.HeliosReporter</code></p>
 */

public class HeliosReporter extends AbstractPollingReporter implements MetricProcessor<Long> {
	/** The tracer used by this reporter */
	protected final ITracer tracer;
	/** The provided metric predicate */
	protected final MetricPredicate predicate;
	
	/** The default reporter name */
	public static final String DEFAULT_NAME = "helios-reporter";
	
    /**
     * Enables the helios reporter to send data to the apmrouter server with the specified period.     
     * @param metricsRegistry the metrics registry
     * @param period          the period between successive outputs
     * @param unit            the time unit of {@code period}
     * @param predicate       filters metrics to be reported
     */
    public static void enable(MetricsRegistry metricsRegistry, long period, TimeUnit unit, MetricPredicate predicate) {
            new HeliosReporter(metricsRegistry, predicate, DEFAULT_NAME).start(period, unit);
    }
	
	
	/**
	 * Creates a new HeliosReporter
	 */
	public HeliosReporter() {
		this(Metrics.defaultRegistry(), null, DEFAULT_NAME);
	}
	
	

	
	/**
	 * Creates a new HeliosReporter
	 * @param registry the {@link MetricsRegistry} containing the metrics this reporter will report
	 * @param predicate The metric filter
	 * @param name The name of this reporter
	 */
	public HeliosReporter(MetricsRegistry registry, MetricPredicate predicate, String name) {
		super(registry==null ? Metrics.defaultRegistry() : registry, name);
		tracer = TracerFactory.getTracer();
		this.predicate = predicate==null ? MetricPredicate.ALL : predicate;
	}

	/**
	 * Sanitizes a metric name
	 * @param name The name to sanitize
	 * @return the helios tracer namespace
	 */
	public static String[] ns(MetricName name) {
		boolean scope = name.hasScope();
		String[] ns = new String[scope ? 4 : 3];
		ns[0] = name.getDomain();
		ns[1] = name.getType();
		if(scope) {
			ns[2] = name.getScope();
			ns[3] = name.getName();
		} else {
			ns[2] = name.getName();
		}
        return ns;		
	}

	/**
	 * {@inheritDoc}
	 * @see com.yammer.metrics.core.MetricProcessor#processMeter(com.yammer.metrics.core.MetricName, com.yammer.metrics.core.Metered, java.lang.Object)
	 */
	@Override
	public void processMeter(MetricName name, Metered meter, Long epoch) throws Exception {
        final String[] namespace = ns(name);
        tracer.traceGauge(meter.getCount(), "count", namespace);
        tracer.traceGauge((long)meter.getMeanRate(), "meanRate", namespace);
        tracer.traceGauge((long)meter.getOneMinuteRate(), "1MinuteRate", namespace);
        tracer.traceGauge((long)meter.getFiveMinuteRate(), "5MinuteRate", namespace);
        tracer.traceGauge((long)meter.getFifteenMinuteRate(), "15MinuteRate", namespace);
	}

	/**
	 * {@inheritDoc}
	 * @see com.yammer.metrics.core.MetricProcessor#processCounter(com.yammer.metrics.core.MetricName, com.yammer.metrics.core.Counter, java.lang.Object)
	 */
	@Override
	public void processCounter(MetricName name, Counter counter, Long context) throws Exception {
		tracer.traceIncrement(counter.getCount(), "count", ns(name));		
	}

	/**
	 * {@inheritDoc}
	 * @see com.yammer.metrics.core.MetricProcessor#processHistogram(com.yammer.metrics.core.MetricName, com.yammer.metrics.core.Histogram, java.lang.Object)
	 */
	@Override
	public void processHistogram(MetricName name, Histogram histogram, Long context) throws Exception {
        final String[] ns = ns(name);
        sendSummarizable(ns, histogram);
        sendSummarizable(ns, histogram);
        sendSampling(ns, histogram);
	}

	/**
	 * {@inheritDoc}
	 * @see com.yammer.metrics.core.MetricProcessor#processTimer(com.yammer.metrics.core.MetricName, com.yammer.metrics.core.Timer, java.lang.Object)
	 */
	@Override
	public void processTimer(MetricName name, Timer timer, Long epoch) throws Exception {
        processMeter(name, timer, epoch);
        final String[] ns = ns(name);
        sendSummarizable(ns, timer);
        sendSampling(ns, timer);
	}
	
    /**
     * Sends all the metrics associated with the passed {@link Summarizable}
     * @param namespace The metric namespace
     * @param metric The {@link Summarizable} to trace
     * @throws IOException thrown on any tracing error
     */
    protected void sendSummarizable(String[] namespace, Summarizable metric) throws IOException {
    	tracer.traceGauge((long)metric.getMin(), "min", namespace);
    	tracer.traceGauge((long)metric.getMax(), "max", namespace);
    	tracer.traceGauge((long)metric.getMean(), "mean", namespace);
    	tracer.traceGauge((long)metric.getStdDev(), "stddev", namespace);
    }
    
    /**
     * Sends all the metrics associated with the passed {@link Sampling}
     * @param namespace The metric namespace
     * @param metric The {@link Sampling} to trace
     * @throws IOException thrown on any tracing error
     */
    protected void sendSampling(String[] namespace, Sampling metric) throws IOException {
        final Snapshot snapshot = metric.getSnapshot();
        tracer.traceGauge((long)snapshot.getMedian(), "median", namespace);
        tracer.traceGauge((long)snapshot.get75thPercentile(), "75percentile", namespace);
        tracer.traceGauge((long)snapshot.get95thPercentile(), "95percentile", namespace);
        tracer.traceGauge((long)snapshot.get98thPercentile(), "98percentile", namespace);
        tracer.traceGauge((long)snapshot.get99thPercentile(), "99percentile", namespace);
        tracer.traceGauge((long)snapshot.get999thPercentile(), "999percentile", namespace);
    }
    
	

	/**
	 * {@inheritDoc}
	 * @see com.yammer.metrics.core.MetricProcessor#processGauge(com.yammer.metrics.core.MetricName, com.yammer.metrics.core.Gauge, java.lang.Object)
	 */
	@Override
	public void processGauge(MetricName name, Gauge<?> gauge, Long context) throws Exception {
		tracer.trace(gauge.getValue(), "value", MetricType.LONG_GAUGE, ns(name));
	}

	/**
	 * {@inheritDoc}
	 * @see com.yammer.metrics.reporting.AbstractPollingReporter#run()
	 */
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}

}
