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
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

import org.helios.apmrouter.metric.MetricType;
import org.helios.apmrouter.trace.ITracer;
import org.helios.apmrouter.trace.TracerFactory;
import org.helios.apmrouter.util.SystemClock;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.Gauge;
import com.yammer.metrics.core.Histogram;
import com.yammer.metrics.core.Metered;
import com.yammer.metrics.core.Metric;
import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.core.MetricPredicate;
import com.yammer.metrics.core.MetricProcessor;
import com.yammer.metrics.core.MetricsRegistry;
import com.yammer.metrics.core.MetricsRegistryListener;
import com.yammer.metrics.core.Sampling;
import com.yammer.metrics.core.Summarizable;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.reporting.AbstractPollingReporter;
import com.yammer.metrics.reporting.MetricDispatcher;
import com.yammer.metrics.stats.Snapshot;

/**
 * <p>Title: HeliosReporter</p>
 * <p>Description: Codahale metrics reporter for Helios APMRouter</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.codahale.helios.HeliosReporter</code></p>
 */

public class HeliosReporter extends AbstractPollingReporter implements MetricProcessor<Long>, MetricsRegistryListener {
	/** The tracer used by this reporter */
	protected final ITracer tracer;
	/** The provided metric predicate */
	protected final MetricPredicate predicate;
	/** The metric dispatcher */
	protected final MetricDispatcher dispatcher = new MetricDispatcher();	
	/** Indicates if metrics should be mapped */
	protected final boolean mappedMetrics;
	
	/** The default reporter name */
	public static final String DEFAULT_NAME = "helios-reporter";
	
    /**
     * Enables the helios reporter to send data to the apmrouter server with the specified period.     
     * @param metricsRegistry the metrics registry
     * @param period          the period between successive outputs
     * @param unit            the time unit of {@code period}
     * @param predicate       filters metrics to be reported
     * @param mappedMetrics	  if true, metrics will be mapped, if false, they will be flat
     */
    public static void enable(MetricsRegistry metricsRegistry, long period, TimeUnit unit, MetricPredicate predicate, boolean mappedMetrics) {
    	HeliosReporter reporter = new HeliosReporter(metricsRegistry, predicate, DEFAULT_NAME, mappedMetrics);
    	reporter.start(period, unit);
    	metricsRegistry.addListener(reporter);
    }
	
	
	/**
	 * Creates a new HeliosReporter
	 */
	public HeliosReporter() {
		this(Metrics.defaultRegistry(), null, DEFAULT_NAME, true);
	}
	
	

	
	/**
	 * Creates a new HeliosReporter
	 * @param registry the {@link MetricsRegistry} containing the metrics this reporter will report
	 * @param predicate The metric filter
	 * @param name The name of this reporter
	 * @param mappedMetrics If true, metrics will be mapped, if false, they will be flat
	 */
	public HeliosReporter(MetricsRegistry registry, MetricPredicate predicate, String name, boolean mappedMetrics) {
		super(registry==null ? Metrics.defaultRegistry() : registry, name);
		this.mappedMetrics = mappedMetrics;
		tracer = TracerFactory.getTracer();
		this.predicate = predicate==null ? MetricPredicate.ALL : predicate;
		this.getMetricsRegistry().addListener(this);
		
	}

	/**
	 * Sanitizes a metric name
	 * @param name The name to sanitize
	 * @param subNames An optional array of additional sub-names to be appended to the returned array
	 * @return the helios tracer namespace
	 */
	public String[] ns(MetricName name, String...subNames) {
        return mappedMetrics ? nsMapped(name, subNames) : nsFlat(name, subNames);		
	}
	
	/**
	 * Sanitizes a metric name to flat metric names
	 * @param name The name to sanitize
	 * @param subNames An optional array of additional sub-names to be appended to the returned array
	 * @return the helios tracer namespace
	 */
	public String[] nsFlat(MetricName name, String...subNames) {
		boolean scope = name.hasScope();
		int baseLength = scope ? 4 : 3;
		String[] ns = new String[baseLength+subNames.length];
		ns[0] = name.getDomain();
		ns[1] = name.getType();
		if(scope) {
			ns[2] = name.getScope();
			ns[3] = name.getName();
		} else {
			ns[2] = name.getName();
		}
		for(int i = 0; i < subNames.length; i++) {
			ns[i+baseLength] = subNames[i];
		}
        return ns;		
	}
	
	/**
	 * Sanitizes a metric name to mapped metric names
	 * @param name The name to sanitize
	 * @param subNames An optional array of additional sub-names to be appended to the returned array, 
	 * although for mapped, only the first subName is appended to the metric name.
	 * @return the helios tracer namespace
	 */
	public String[] nsMapped(MetricName name, String...subNames) {
		boolean scope = name.hasScope();
		int baseLength = scope ? 4 : 3;
		String[] ns = new String[baseLength+(subNames.length==0 ? 0 : 1)];
		ns[0] = "domain=" + name.getDomain();
		ns[1] = "type=" + name.getType();
		if(scope) {
			ns[2] = "scope=" + name.getScope();
			ns[3] = "name=" + name.getName();
		} else {
			ns[2] = "name=" + name.getName();
		}
		if(subNames.length>0) {
			ns[baseLength] = "aggregate=" + subNames[0];
		}
        return ns;		
	}
	
	

	/**
	 * {@inheritDoc}
	 * @see com.yammer.metrics.core.MetricProcessor#processMeter(com.yammer.metrics.core.MetricName, com.yammer.metrics.core.Metered, java.lang.Object)
	 */
	@Override
	public void processMeter(MetricName name, Metered meter, Long epoch) throws Exception {
        final String[] namespace = ns(name, "Meter");
        tracer.traceGauge(meter.getCount(), "Count", namespace);
        tracer.traceGauge((long)meter.getMeanRate(), "MeanRate", namespace);
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
		//tracer.traceIncrement(counter.getCount(), "count", ns(name));
		//tracer.traceGauge(counter.getCount(), "Count", ns(name));
	}

	/**
	 * {@inheritDoc}
	 * @see com.yammer.metrics.core.MetricProcessor#processHistogram(com.yammer.metrics.core.MetricName, com.yammer.metrics.core.Histogram, java.lang.Object)
	 */
	@Override
	public void processHistogram(MetricName name, Histogram histogram, Long context) throws Exception {
        final String[] ns = ns(name, "Histogram");
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
        sendSummarizable(ns(name), timer);
        sendSampling(ns(name, "Sampling"), timer);
	}
	
    /**
     * Sends all the metrics associated with the passed {@link Summarizable}
     * @param namespace The metric namespace
     * @param metric The {@link Summarizable} to trace
     * @throws IOException thrown on any tracing error
     */
    protected void sendSummarizable(String[] namespace, Summarizable metric) throws IOException {
    	tracer.traceGauge((long)metric.getMin(), "Min", namespace);
    	tracer.traceGauge((long)metric.getMax(), "Max", namespace);
    	tracer.traceGauge((long)metric.getMean(), "Mean", namespace);
    	tracer.traceGauge((long)metric.getStdDev(), "Stddev", namespace);
    }
    
    /**
     * Sends all the metrics associated with the passed {@link Sampling}
     * @param namespace The metric namespace
     * @param metric The {@link Sampling} to trace
     * @throws IOException thrown on any tracing error
     */
    protected void sendSampling(String[] namespace, Sampling metric) throws IOException {
        final Snapshot snapshot = metric.getSnapshot();
        tracer.traceGauge((long)snapshot.getMedian(), "Median", namespace);
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
		final Map<String,SortedMap<MetricName,Metric>> metricMap = getMetricsRegistry().getGroupedMetrics(predicate);
		if(!metricMap.isEmpty()) {
	        for (Map.Entry<String,SortedMap<MetricName,Metric>> entry : metricMap.entrySet()) {
	            for (Entry<MetricName, Metric> subEntry : entry.getValue().entrySet()) {
	            	//System.out.println("Processing MetricName [" + subEntry.getKey() + "]");
	                final Metric metric = subEntry.getValue();
	                if (metric != null) {
	                    try {
	                        dispatcher.dispatch(subEntry.getValue(), subEntry.getKey(), this, SystemClock.unixTime());
	                    } catch (Exception ignored) {
	                    	System.err.println("Error printing regular metrics. Stack trace follows");
	                    	ignored.printStackTrace(System.err);
	                    }
	                }
	            }
	        }
		}
	}


	/**
	 * {@inheritDoc}
	 * @see com.yammer.metrics.core.MetricsRegistryListener#onMetricAdded(com.yammer.metrics.core.MetricName, com.yammer.metrics.core.Metric)
	 */
	@Override
	public void onMetricAdded(MetricName name, Metric metric) {
        if (metric != null) {
            try {
                dispatcher.dispatch(metric, name, this, SystemClock.unixTime());
            } catch (Exception e) {
                System.err.println("Error processing " + name + ". Stack trace fillows:");
                e.printStackTrace(System.err);
            }
        }

		
	}


	/**
	 * {@inheritDoc}
	 * @see com.yammer.metrics.core.MetricsRegistryListener#onMetricRemoved(com.yammer.metrics.core.MetricName)
	 */
	@Override
	public void onMetricRemoved(MetricName name) {
		// TODO Auto-generated method stub
		
	}

}
