/**
 * ICE Futures, US
 */
package org.helios.apmrouter.metric;

import java.nio.ByteBuffer;

/**
 * <p>Title: IMetricDataAccessor</p>
 * <p>Description: Defines an accessor class that knows how to read and write a specific data type to a metric's byte buffer </p> 
 * <p>Company: ICE Futures US</p>
 * @author Whitehead (nicholas.whitehead@theice.com)
 * @version $LastChangedRevision$
 * <p><code>org.helios.apmrouter.metric.IMetricDataAccessor</code></p>
 */

interface IMetricDataAccessor<T extends Object> {
	/**
	 * Writes the passed value to a newly allocated byte buffer
	 * @param value The value to write
	 * @return The created and loaded byte buffer
	 */
	public ByteBuffer write(T value);
	
	/**
	 * Writes the passed value to a newly allocated byte buffer
	 * @param value The value to write
	 * @return The created and loaded byte buffer
	 */
	public ByteBuffer writeObject(Object value);
	
	/**
	 * Reads the value from the passed byte buffer
	 * @param metricBuffer The metric's byte buffer to read the value from
	 * @return The read value
	 */
	public T read(ByteBuffer metricBuffer);
}
