/**
 * ICE Futures, US
 */
package org.helios.apmrouter.metric;


/**
 * <p>Title: IMetricDataAccessor</p>
 * <p>Description: Defines an accessor class that knows how to read and write a specific data type to a metricId's byte buffer </p> 
 * <p>Company: ICE Futures US</p>
 * @author Whitehead (nicholas.whitehead@theice.com)
 * @version $LastChangedRevision$
 * <p><code>org.helios.apmrouter.metric.IMetricDataAccessor</code></p>
 */

interface IMetricDataAccessor<T extends Object> {
	/**
	 * Writes the passed value to a newly allocated {@link ICEMetricValue}
	 * @param value The value to write
	 * @return The created {@link ICEMetricValue}
	 */
	public ICEMetricValue write(T value);
	
	/**
	 * Writes the passed value to a newly allocated {@link ICEMetricValue}
	 * @param value The value to write
	 * @return The created {@link ICEMetricValue}
	 */
	public ICEMetricValue writeObject(Object value);
	
	/**
	 * Reads the value from the passed {@link ICEMetricValue}
	 * @param metricValue The metric's {@link ICEMetricValue} instance to read the value from
	 * @return The read value
	 */
	public T read(ICEMetricValue metricValue);
}
