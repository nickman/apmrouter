/**
 * ICE Futures, US
 */
package org.helios.apmrouter.metric;

import java.nio.ByteBuffer;
import java.util.Date;

import org.helios.apmrouter.util.SystemClock;

/**
 * <p>Title: ICEMetricValue</p>
 * <p>Description: The container object for a metricId instance's value</p> 
 * <p>Company: ICE Futures US</p>
 * @author Whitehead (nicholas.whitehead@theice.com)
 * @version $LastChangedRevision$
 * <p><code>org.helios.apmrouter.metric.ICEMetricValue</code></p>
 */

public class ICEMetricValue {
	/** The timestamp of this metricId */
	protected final long time;
	/** The value of this metricId */
	protected ByteBuffer value;
	/** The long value of this metricId as an optimization of long type metrics */
	protected long longValue = 0;
	/** The metricId type */
	protected final MetricType type;
	
	/**
	 * Creates a new ICEMetricValue
	 * @param type The metricId type
	 * @param longValue The value of this metricId as a long
	 */
	ICEMetricValue(MetricType type, long longValue) {
		this.time = SystemClock.time();
		this.type = type;
		this.longValue = longValue;
	}
	
	/**
	 * Creates a new ICEMetricValue
	 * @param type The metricId type
	 * @param longValue The value of this metricId as a long
	 * @param timestamp The metric value timestamp
	 */
	ICEMetricValue(MetricType type, long longValue, long timestamp) {
		this.time = timestamp;
		this.type = type;
		this.longValue = longValue;
	}
	
	
	/**
	 * Creates a new ICEMetricValue
	 * @param type The metricId type
	 * @param value The value of this metricId in bytes
	 */
	ICEMetricValue(MetricType type, ByteBuffer value) {
		this.time = SystemClock.time();
		this.type = type;
		this.value = value;
		if(this.value.position()!=0) this.value.flip(); 
	}
	
	/**
	 * Creates a new ICEMetricValue
	 * @param type The metricId type
	 * @param value The value of this metricId in bytes
	 * @param timestamp The metric value timestamp
	 */
	ICEMetricValue(MetricType type, ByteBuffer value, long timestamp) {
		this.time = timestamp;
		this.type = type;
		this.value = value;
		if(this.value.position()!=0) this.value.flip(); 
	}
	
	
	


	/**
	 * The timestamp of this metricId as a UTC long
	 * @return the time
	 */
	public long getTime() {
		return time;
	}
	
	/**
	 * The timestamp of this metricId as a java date
	 * @return the date
	 */
	public Date getDate() {
		return new Date(time);
	}
	

	/**
	 * Returns the value of this metricId
	 * @return the value
	 */
	public ByteBuffer getValue() {
		return value;
	}

	/**
	 * Returns the value as a long, or throws a RuntimeException if the type is not long based
	 * @return the long value
	 */
	public long getLongValue() {
		if(!type.isLong()) throw new RuntimeException("This value is not a long type", new Throwable());
		return longValue;
	}

	/**
	 * Returns the type of this metric
	 * @return the type
	 */
	public MetricType getType() {
		return type;
	}
	
	
	

	
}
