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
package org.helios.apmrouter.metric;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;

import org.helios.apmrouter.metric.catalog.IDelegateMetric;
import org.helios.apmrouter.util.IO;
import org.snmp4j.PDU;

/**
 * <p>Title: ExpandedMetric</p>
 * <p>Description: An extension of {@link ICEMetric} that provides extended functionality and overrides for the apmrouter server environment.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.metric.ExpandedMetric</code></p>
 */

public class ExpandedMetric extends ICEMetric {
	/** The classname of the metric's value */
	protected final String valueClassName;
	/** The metric's value type */
	protected final Class<?> valueType;
	/** The metric's value if a blob */
	protected Object blobValue;
	
	/**
	 * Creates a new ExpandedMetric
	 * @param metric The underlying metric
	 */
	public ExpandedMetric(ICEMetric metric) {
		this(metric.value, metric.metricId);
		this.txContext = metric.txContext;
	}
	
	public Object getValue() {
		if(getType()==MetricType.BLOB) return blobValue;
		return super.getValue();
	}
	
	
	/**
	 * Creates a new ExpandedMetric
	 * @param value The metric value
	 * @param metricId The metric ID
	 */
	public ExpandedMetric(ICEMetricValue value, IDelegateMetric metricId) {
		super(value, metricId);
		switch (getType()) {
		case BLOB:
			blobValue = extractValue(value);
			valueClassName = blobValue.getClass().getName();
			//valueType = blobValue.getClass();
			break;
		case ERROR:
			valueClassName = Throwable.class.getName();
			break;
		case PDU:
			valueClassName = PDU.class.getName();
			break;
		case STRING:
			valueClassName = String.class.getName();
			break;
		default:
			valueClassName = "long";
		}
		try {
			valueType = Class.forName(valueClassName);
		} catch (Exception e) {
			throw new RuntimeException("Failed to get class for name [" + valueClassName + "]", e);
		}
		
	}
	
	/**
	 * Returns the classname of the metric value
	 * @return the classname of the metric value
	 */
	public String getValueClassName() {
		return valueClassName;
	}
	
	/**
	 * Returns the type of the metric value
	 * @return the type of the metric value
	 */
	public Class<?> getValueClass() {
		return valueType;
	}
	
	/**
	 * <p>Preprends the value type name to the routing key
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.ICEMetric#getRoutingKey()
	 */
	@Override
	public CharSequence getRoutingKey() {
		return String.format("%s-%s-%s", valueClassName, getType().name(), getFQN());
	}


	
	/**
	 * <p>Title: ClassNameReader</p>
	 * <p>Description: A quickie utility to read the classname from the value ByteBuffer</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.metric.ExpandedMetric.ClassNameReader</code></p>
	 */
	protected static class ClassNameReader extends ObjectInputStream {
		/** The ByteBuffer to extract the classname from */
		private final ByteBuffer valueBuffer;
		
		/**
		 * Reads the classname from the passed buffer
		 * @param valueBuffer The buffer to read from
		 * @return the class name
		 */
		public static String getClassName(ByteBuffer valueBuffer) {
			try {
				return new ClassNameReader(valueBuffer).getClassName();
			} catch (Exception e) {
				throw new RuntimeException("ClassReader Unexpected Exception", e);
			}
		}
		
		/**
		 * Creates a new ClassNameReader
		 * @param valueBuffer The ByteBuffer to extract the classname from
		 * @throws IOException thrown on errors reading the ByteBuffer 
		 */
		public ClassNameReader(ByteBuffer valueBuffer) throws IOException {
			super(IO.read(valueBuffer));
			this.valueBuffer = valueBuffer;
		}
		
		/**
		 * Returns the name of the class described by this descriptor.
		 * @return the name of the class described by this descriptor.
		 */
		public String getClassName() {
			try {
				return readClassDescriptor().getName();
			} catch (Exception e) {
				throw new RuntimeException("Failed to read classname from ByteBuffer", e);
			} finally {
				try { close(); } catch (Exception e) {}
				valueBuffer.rewind();
			}
		}
	}
	
	/**
	 * Extracts the classname from the passed BLOB type metric value
	 * @param value a BLOB type metric value
	 * @return the classname of the BLOB metric's value
	 */
	protected static Object extractValue(ICEMetricValue value) {
		return value.getValue();
	}

}
