/**
 * ICE Futures, US
 */
package org.helios.apmrouter.metric;

import java.nio.ByteBuffer;
import java.util.Date;

import org.helios.apmrouter.util.SystemClock;

/**
 * <p>Title: ICEMetricValue</p>
 * <p>Description: The container object for a metric instance's value</p> 
 * <p>Company: ICE Futures US</p>
 * @author Whitehead (nicholas.whitehead@theice.com)
 * @version $LastChangedRevision$
 * <p><code>org.helios.apmrouter.metric.ICEMetricValue</code></p>
 */

public class ICEMetricValue {
	/** The timestamp of this metric */
	protected final long time;
	/** The value of this metric */
	protected ByteBuffer value;
	/** The long value of this metric as an optimization of long type metrics */
	protected long longValue = 0;
	/** The metric type */
	protected final MetricType type;
	
	/**
	 * Creates a new ICEMetricValue
	 * @param type The metric type
	 * @param longValue The value of this metric as a long
	 */
	private ICEMetricValue(MetricType type, long longValue) {
		this.time = SystemClock.time();
		this.type = type;
		this.longValue = longValue;
	}
	
	/**
	 * Creates a new ICEMetricValue
	 * @param type The metric type
	 * @param value The value of this metric
	 */
	private ICEMetricValue(MetricType type, Object value) {
		this.time = SystemClock.time();
		this.type = type;
		// SET VALUE
	}
	
	


	/**
	 * The timestamp of this metric as a UTC long
	 * @return the time
	 */
	public long getTime() {
		return time;
	}
	
	/**
	 * The timestamp of this metric as a java date
	 * @return the date
	 */
	public Date getDate() {
		return new Date(time);
	}
	

	/**
	 * Returns the value of this metric
	 * @return the value
	 */
	public Object getValue() {
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
	 * @return the type
	 */
	public MetricType getType() {
		return type;
	}
	
	
	

	
}
