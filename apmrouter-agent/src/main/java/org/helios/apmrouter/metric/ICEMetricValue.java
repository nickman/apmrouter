/**
 * ICE Futures, US
 */
package org.helios.apmrouter.metric;

import org.helios.apmrouter.util.SystemClock;

import java.nio.ByteBuffer;
import java.util.Date;

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
	 * Conflates the passed metric value into this one
	 * @param metricValue The metric value to conflate into this metric value
	 */
	public synchronized void conflate(ICEMetricValue metricValue) {
		if(!type.isLong() || !metricValue.type.isLong()) {
			StringBuilder b = new StringBuilder("Cannot conflate non-numeric values.");
			b.append("\n\tThis metric:").append(this.toString());
			b.append("\n\tThat value:").append(metricValue);
			throw new RuntimeException(b.toString(), new Throwable());
		}
		if(type!=metricValue.type) {
			StringBuilder b = new StringBuilder("Cannot conflate values of different types.");
			b.append("\n\tThis metric:").append(this.toString());
			b.append("\n\tThat value:").append(metricValue);
			throw new RuntimeException(b.toString(), new Throwable());
		}
		if(type.isGauge()) {
			this.longValue = avg(2, this.longValue + metricValue.longValue);
		} else {
			this.longValue += metricValue.longValue;
		}
	}
	
	/**
	 * Calculates the average
	 * @param count The number of instances
	 * @param total The total
	 * @return the average
	 */
	public static long avg(double count, double total) {
		if(total==0 || count==0) return 0;
		double d = total/count;
		return (long)d;		
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
	public Object getValue() {
		return type.getDataAccessor().read(this);
	}
	
	public ByteBuffer getRawValue() {
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

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ICEMetricValue [time=" + new Date(time) + ", type=" + type + "]";
	}
	
	
	

	
}
