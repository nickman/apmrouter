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
package org.helios.apmrouter.destination.wily;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * <p>Title: WilyIntroscopeTracer</p>
 * <p>Description: A reflection based wrapper for dispatching metrics to Wily Introscope</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.wily.WilyIntroscopeTracer</code></p>
 */

public class WilyIntroscopeTracer {
	/** Singleton instance */
	protected static volatile WilyIntroscopeTracer instance = null;
	/** Singleton instance ctor lock */
	protected static final Object lock = new Object();
	
	/** The data recorder factory class */
	protected Class<?> drfClazz = null;
	/** A cache of data recorder creating methods indexed by the data recorder simple type name */
	protected final Map<String, Method> recorderCreateMethods = new ConcurrentHashMap<String, Method>(); 
	/** A cache of data recorder recording methods indexed by the data recorder simple type name and method name*/
	protected final Map<String, Map<String, Method>> recordingMethods = new ConcurrentHashMap<String, Map<String, Method>>(); 
	
	
	/** Regex pattern to match data recorder creation methods */
	public static final Pattern CREATE_PATTERN = Pattern.compile("create(.*)DataRecorder");
	/** A cache of data recorders keyed by metric name */
	protected final Map<String, Object> dataRecorders = new ConcurrentHashMap<String, Object>(128);
	/** A null argument object array */
	public static final Object[] NULL_ARGS = new Object[0];

	/** Iscope Resource Delimeter */
	public static final String RES_DELIM = "|";
	/** Iscope Metric Delimeter */
	public static final String MET_DELIM = ":";
	/** Iscope Metric Patterns */
	public static final String[] MET_PATTERNS = new String[50];
	
	/** The metric type name constant for LongAverages */
	public static final String LONGAVERAGE_TYPE = "LongAverage";
	/** The metric type name constant for IntAverages */
	public static final String INTAVERAGE_TYPE = "IntAverage";
	/** The metric type name constant for LongCounters */
	public static final String LONGCOUNTER_TYPE = "LongCounter";
	/** The metric type name constant for IntCounters */
	public static final String INTCOUNTER_TYPE = "IntCounter";
	/** The metric type name constant for IntRates */
	public static final String INTRATE_TYPE = "IntRate";
	/** The metric type name constant for StringEvents */
	public static final String STRINGEVENT_TYPE = "StringEvent";
	/** The metric type name constant for Timestamps */
	public static final String TIMESTAMP_TYPE = "Timestamp";
	/** The metric type name constant for PerIntervalCounters */
	public static final String PERINTERVALCOUNTER_TYPE = "PerIntervalCounter";
	
	/** Recorder op Name */
	public static final String RECORDMULTIPLEINCIDENTS_OP = "recordMultipleIncidents";
	/** Recorder op Name */
	public static final String SUBTRACT_OP = "subtract";
	/** Recorder op Name */
	public static final String RECORDDATAPOINT_OP = "recordDataPoint";
	/** Recorder op Name */
	public static final String ADD_OP = "add";
	/** Recorder op Name */
	public static final String RECORDTIMESTAMP_OP = "recordTimestamp";
	/** Recorder op Name */
	public static final String RECORDCURRENTVALUE_OP = "recordCurrentValue";
	/** Recorder op Name */
	public static final String RECORDINCIDENT_OP = "recordIncident";

	
	
	static {
		MET_PATTERNS[1] = "%s";
		MET_PATTERNS[2] = "%s" + MET_DELIM + "%s";		
		for(int i = 3; i < 50; i++) {
			StringBuilder b = new StringBuilder(MET_PATTERNS[2]);
			int rez = i-2;
			for(int x = 0; x < rez; x++) {
				b.insert(0, "%s" + RES_DELIM);
			}
			MET_PATTERNS[i] = b.toString();
		}
	}
	
	/**
	 * Acquires the IScopeOneOffTracer singleton instance
	 * @return the IScopeOneOffTracer singleton instance
	 */
	public static WilyIntroscopeTracer getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new WilyIntroscopeTracer(); 
				}
			}
		}
		return instance;
	}
	
	/**
	 * Creates a new WilyIntroscopeTracer
	 */
	private WilyIntroscopeTracer() {
		try {
			Class<?> drfClazz = Class.forName("com.wily.introscope.agent.api.DataRecorderFactory", true, Thread.currentThread().getContextClassLoader());
			log("DataRecorderFactory Class:" + drfClazz.getName());
			for(Method m: drfClazz.getDeclaredMethods()) {
				String methodName = m.getName();
				Matcher matcher = CREATE_PATTERN.matcher(methodName);
				if(matcher.matches()) {
					String type = matcher.group(1);
					if(type!=null && !type.isEmpty()) {
						m.setAccessible(true);
						recorderCreateMethods.put(type, m);
						Class<?> recorderClass = m.getReturnType();
						Map<String, Method> rMethods = new HashMap<String, Method>();
						recordingMethods.put(type, rMethods);
						for(Method rm: recorderClass.getDeclaredMethods()) {
							rMethods.put(rm.getName(), rm);
							String gs = rm.toGenericString();
							if(gs.contains(" throws ")) {
								gs = gs.split(" throws ")[0];
							}
							StringBuilder b = new StringBuilder("\n\tpublic void ");
							String[] frags = gs.split("\\.");
							b.append(frags[frags.length-1]);
							b.append(" {\n\t\t\n\t}");
						}
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to initialize DataRecorderFactory", e);
		}
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		log("IScope Test");
		try {
			Blocker blocker = new Blocker();
			WilyIntroscopeTracer iscope = WilyIntroscopeTracer.getInstance();
			
			CountDownLatch latch = new CountDownLatch(1);
			for(int i = 0; i < 30; i++) {
				log("Loop #" + i);
				ThreadInfo ti = ManagementFactory.getThreadMXBean().getThreadInfo(Thread.currentThread().getId());
				if(i%2==0) {
					try { Thread.sleep(12000); } catch (Exception e) {}
				} else {
					blocker.enter(12000);
					
				}
				ThreadInfo ti2 = ManagementFactory.getThreadMXBean().getThreadInfo(Thread.currentThread().getId());
				iscope.recordCurrentValue(ti2.getBlockedCount() - ti.getBlockedCount(), "JVM", "Thread Contention", "Blocked Count");
				iscope.recordCurrentValue(ti2.getBlockedTime() - ti.getBlockedTime(), "JVM", "Thread Contention", "Blocked Time");

				iscope.recordCurrentValue(ti2.getWaitedCount() - ti.getWaitedCount(), "JVM", "Thread Contention", "Waited Count");
				iscope.recordCurrentValue(ti2.getWaitedTime() - ti.getWaitedTime(), "JVM", "Thread Contention", "Waited Time");
				
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}
	
	public static class Blocker {
		public void enter(final long blockTime) {
			final CountDownLatch latch = new CountDownLatch(1);
			new Thread() {
				public void run() {
					waitForMe(latch, blockTime);
				}
			}.start();
			try { latch.await(); } catch (Exception e) {}
			waitForMe(null, 1);
		}
		public synchronized void waitForMe(CountDownLatch latch, long blockTime) {
			if(latch!=null) latch.countDown();
			try { Thread.currentThread().join(blockTime); } catch (Exception e) {}
		}
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}

	/**
	 * Records a single data point and rolls it into the current average
	 * @param value The value to record
	 * @param args The metric name fragments
	 */
	public void recordDataPoint(long value, Object...args) {
		try {
			invokeRecording(LONGAVERAGE_TYPE, RECORDDATAPOINT_OP, value, args);		
		} catch (Throwable t) {
			t.printStackTrace(System.err);
		}
	}

	/**
	 * Records a single data point and rolls it into the current average
	 * @param value The value to record
	 * @param args The metric name fragments
	 */
	public void recordDataPoint(int value, Object...args) {
		try {
			invokeRecording(INTAVERAGE_TYPE, RECORDDATAPOINT_OP, value, args);		
		} catch (Throwable t) {
			t.printStackTrace(System.err);
		}
	}

	/**
	 * Adds the given delta from the current value.
	 * @param value The value to add from the counter
	 * @param args The metric name fragments
	 */
	public void add(long value, Object...args) {
		try {
			invokeRecording(LONGCOUNTER_TYPE, ADD_OP, value, args);		
		} catch (Throwable t) {
			t.printStackTrace(System.err);
		}		
	}

	/**
	 * Records the current counter value
	 * @param value The value to record
	 * @param args The metric name fragments
	 */
	public void recordCurrentValue(long value, Object...args) {
		try {
			invokeRecording(LONGCOUNTER_TYPE, RECORDCURRENTVALUE_OP, value, args);		
		} catch (Throwable t) {
			t.printStackTrace(System.err);
		}		
	}


	/**
	 * Subtracts the given delta from the current value.
	 * @param value The value to subtract from the counter
	 * @param args The metric name fragments
	 */
	public void subtract(long value, Object...args) {
		try {
			invokeRecording(LONGCOUNTER_TYPE, SUBTRACT_OP, value, args);		
		} catch (Throwable t) {
			t.printStackTrace(System.err);
		}		
	}

	/**
	 * Adds the given delta from the current value.
	 * @param value The value to add from the counter
	 * @param args The metric name fragments
	 */
	public void add(int value, Object...args) {
		try {
			invokeRecording(INTCOUNTER_TYPE, ADD_OP, value, args);		
		} catch (Throwable t) {
			t.printStackTrace(System.err);
		}		
	}

	/**
	 * Records the current counter value
	 * @param value The value to record
	 * @param args The metric name fragments
	 */
	public void recordCurrentValue(int value, Object...args) {
		try {
			invokeRecording(INTCOUNTER_TYPE, RECORDCURRENTVALUE_OP, value, args);		
		} catch (Throwable t) {
			t.printStackTrace(System.err);
		}		
	}

	/**
	 * Subtracts the given delta from the current value.
	 * @param value The value to subtract from the counter
	 * @param args The metric name fragments
	 */
	public void subtract(int value, Object...args) {
		try {
			invokeRecording(INTCOUNTER_TYPE, SUBTRACT_OP, value, args);		
		} catch (Throwable t) {
			t.printStackTrace(System.err);
		}		
	}



	/**
	 * Records a single string type point 
	 * @param value The value to record
	 * @param args The metric name fragments
	 */
	public void recordDataPoint(CharSequence value, Object...args) {
		try {
			invokeRecording(STRINGEVENT_TYPE, RECORDDATAPOINT_OP, value, args);		
		} catch (Throwable t) {
			t.printStackTrace(System.err);
		}
	}

	/**
	 * Records a single string type point 
	 * @param value The value to record
	 * @param metricName THe formatted metric name
	 */
	public void recordDataPoint(CharSequence value, String metricName) {
		try {
			invokeRecording(STRINGEVENT_TYPE, RECORDDATAPOINT_OP, value, metricName);		
		} catch (Throwable t) {
			t.printStackTrace(System.err);
		}
	}
	
	/**
	 * Records a timestamp
	 * @param timestamp The timestamp in the form of a UTC long
	 * @param args The metric name fragments
	 */
	public void recordTimestamp(long timestamp, Object...args) {
		try {
			invokeRecording(TIMESTAMP_TYPE, RECORDTIMESTAMP_OP, timestamp, args);		
		} catch (Throwable t) {
			t.printStackTrace(System.err);
		}
	}
	
	/**
	 * Records the current timestamp
	 * @param args The metric name fragments
	 */
	public void recordTimestamp(Object...args) {
		try {
			invokeRecording(TIMESTAMP_TYPE, RECORDTIMESTAMP_OP, System.currentTimeMillis(), args);		
		} catch (Throwable t) {
			t.printStackTrace(System.err);
		}
	}
	

	/**
	 * Records a single interval incident
	 * @param args The metric name fragments
	 */
	public void recordIntervalIncident(Object...args) {
		try {
			invokeRecording(PERINTERVALCOUNTER_TYPE, RECORDINCIDENT_OP, args);		
		} catch (Throwable t) {
			t.printStackTrace(System.err);
		}
	}
	
	/**
	 * Record multiple interval incidents. The number of incidents must be positive. 
	 * @param incidentCount The number of incidents
	 * @param args The metric name fragments
	 */
	public void recordMultipleIntervalIncidents(int incidentCount, Object...args) {
		try {
			invokeRecording(PERINTERVALCOUNTER_TYPE, RECORDMULTIPLEINCIDENTS_OP, incidentCount, args);		
		} catch (Throwable t) {
			t.printStackTrace(System.err);
		}
	}
	
	
	/**
	 * Records a single rate incident
	 * @param args The metric name fragments
	 */
	public void recordRateIncident(Object...args) {
		try {
			invokeRecording(INTRATE_TYPE, RECORDINCIDENT_OP, args);		
		} catch (Throwable t) {
			t.printStackTrace(System.err);
		}
	}

	
	/**
	 * Record multiple rate incidents. The number of incidents must be positive. 
	 * @param value The number of incidents
	 * @param args The metric name fragments
	 */
	public void recordMultipleRateIncidents(int value, Object...args) {
		try {
			invokeRecording(INTRATE_TYPE, RECORDMULTIPLEINCIDENTS_OP, value, args);		
		} catch (Throwable t) {
			t.printStackTrace(System.err);
		}
	}
	
	
	/**
	 * Invokes a recorder call
	 * @param type The metric type
	 * @param op The recorder op
	 * @param args The metric name fragments
	 */
	public void invokeRecording(String type, String op, Object...args) {
		invokeRecording(type, op, null, args);
	}
	
	/**
	 * Invokes a recorder call
	 * @param type The metric type
	 * @param op The recorder op
	 * @param args The formatted metric name 
	 */
	public void invokeRecording(String type, String op, String metricName) {
		invokeRecording(type, op, null, metricName);
	}
	
	/**
	 * Invokes a recorder call
	 * @param type The metric type
	 * @param op The recorder op
	 * @param value The value to pass to the recording. Ignored if null
	 * @param args The metric name fragments
	 */
	public void invokeRecording(String type, String op, Object value, Object...args) {
		Object recorder = getRecorder(type, args);
		try {
			recordingMethods.get(type).get(op).invoke(recorder, value==null ? NULL_ARGS : value );
		} catch (Exception e) {
			throw new RuntimeException("Failed to invoke [" + type + "/" + op + "]", e);
		}
	}
	
	/**
	 * Invokes a recorder call
	 * @param type The metric type
	 * @param op The recorder op
	 * @param value The value to pass to the recording. Ignored if null
	 * @param metricName The formatted metric name
	 */
	public void invokeRecording(String type, String op, Object value, String metricName) {
		Object recorder = getRecorder(type, metricName);
		try {
			recordingMethods.get(type).get(op).invoke(recorder, value==null ? NULL_ARGS : value );
		} catch (Exception e) {
			throw new RuntimeException("Failed to invoke [" + type + "/" + op + "]", e);
		}
	}
	
	
	/**
	 * Returns a data recorder for the passed type and metric name fragments
	 * @param type The data recorder type
	 * @param args the metric name fragments
	 * @return a data recorder
	 */
	public Object getRecorder(String type, Object...args) {
		return getRecorder(type, buildMetricName(args));
	}

	
	/**
	 * Returns a data recorder for the passed type and metric name fragments
	 * @param type The data recorder type
	 * @param metricName The formatted metric name
	 * @return a data recorder
	 */
	public Object getRecorder(String type, String metricName) {
		Object recorder = dataRecorders.get(metricName);
		if(recorder==null) {
			synchronized(dataRecorders) {
				recorder = dataRecorders.get(metricName);
				if(recorder==null) {
					Method m = recorderCreateMethods.get(type);
					if(m==null) throw new IllegalArgumentException("The passed type [" + type + "] is invalid", new Throwable());
					try {
						recorder = m.invoke(null, metricName);
					} catch (Exception e) {
						throw new RuntimeException("Failed to create recorder of type [" + type + "]", e);
					}
				}
			}
		}
		return recorder;
	}
	
	/**
	 * Builds an iscope metric name
	 * @param args The components of each resource and then metric name
	 * @return an Iscope metric name
	 */
	public String buildMetricName(Object...args) {
		if(args==null || args.length<1) throw new IllegalArgumentException("The passed char sequence array was null or zero length", new Throwable());
		return String.format(MET_PATTERNS[args.length], args);
	}

}
