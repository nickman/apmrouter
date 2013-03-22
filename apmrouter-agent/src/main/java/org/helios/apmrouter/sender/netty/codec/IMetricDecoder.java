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
package org.helios.apmrouter.sender.netty.codec;


import org.helios.apmrouter.metric.ICEMetric;
import org.helios.apmrouter.metric.IMetric;
import org.helios.apmrouter.metric.MetricType;
import org.helios.apmrouter.metric.catalog.ICEMetricCatalog;
import org.helios.apmrouter.metric.catalog.IDelegateMetric;
import org.helios.apmrouter.metric.catalog.IMetricCatalog;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.replay.ReplayingDecoder;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.helios.apmrouter.sender.netty.codec.IMetricDecodePhase.*;

/**
 * <p>Title: IMetricDecoder</p>
 * <p>Description: A replaying decoder for {@link IMetric} instances.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.sender.netty.codec.IMetricDecoder</code></p>
 * FIXME: The IMetricRebuilder will be very inneficient since it has to be created once per incoming metric.
 * We should re-create it as a ChannelLocal or extend Channel and add the state there.
 */
@ChannelHandler.Sharable
public class IMetricDecoder extends ReplayingDecoder<IMetricDecodePhase> {
	/** The metric catalog for tokenization */
	private final IMetricCatalog metricCatalog;
	
	/**
	 * Creates a new IMetricDecoder
	 */
	public IMetricDecoder() {
		metricCatalog = ICEMetricCatalog.getInstance();
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.handler.codec.replay.ReplayingDecoder#decode(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.Channel, org.jboss.netty.buffer.ChannelBuffer, java.lang.Enum)
	 */
	@Override
	protected IMetric decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer, IMetricDecodePhase state) throws Exception {
		IMetricRebuilder rebuilder = (IMetricRebuilder)ctx.getAttachment();
		if(rebuilder==null) {
			rebuilder = new IMetricRebuilder();
			ctx.setAttachment(rebuilder);
			state = BYTE_ORDER;
		}
		switch (state) {
			case BYTE_ORDER:
				rebuilder.setByteOrder(buffer.readByte()==0 ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
				checkpoint(ISTOKENIZED);				
			//$FALL-THROUGH$
			case ISTOKENIZED:
				IDelegateMetric dmetric  = null;
				long token = buffer.readLong();
				if(token!=-1) {
					dmetric = metricCatalog.get(token);
					if(dmetric!=null) {
						rebuilder.setDelegateMetric(dmetric);
						checkpoint(TIMESTAMP);
						break;
					}
					token = -1;
				}
				checkpoint(TYPE);
			//$FALL-THROUGH$
			case TYPE:
				rebuilder.setType(MetricType.valueOf(buffer.readByte()));
				checkpoint(FQNLENGTH);				
			//$FALL-THROUGH$
			case FQNLENGTH:
				rebuilder.setFqnLength(buffer.readInt());
				checkpoint(FQN);				
			//$FALL-THROUGH$
			case FQN:
				byte[] fqnbytes = new byte[rebuilder.getFqnLength()]; 
				buffer.readBytes(fqnbytes);
				rebuilder.setFqn(new String(fqnbytes));		
				checkpoint(TIMESTAMP);
			//$FALL-THROUGH$
			case TIMESTAMP:
				rebuilder.setTs(buffer.readLong());
				checkpoint(LONGVALUE);				
			//$FALL-THROUGH$	
			case LONGVALUE:
				if(rebuilder.getType().isLong()) {
					rebuilder.setLvalue(buffer.readLong());
					return rebuilder.buildMetric();
				}
				checkpoint(VLENGTH);
			//$FALL-THROUGH$
			case VLENGTH:
				rebuilder.setVlength(buffer.readInt());
				checkpoint(VALUE);
			//$FALL-THROUGH$
			case VALUE:
				buffer.readBytes(rebuilder.getValue());
				return rebuilder.buildMetric();
		}
		return null;
	}
	
	/**
	 * <p>Title: IMetricRebuilder</p>
	 * <p>Description: Container class that serves as the replaying decoder's state during the decode, and rebuilds the received metric when decoding is complete.</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.sender.netty.codec.IMetricDecoder.IMetricRebuilder</code></p>
	 */
	private class IMetricRebuilder {
		/** The byte order of the incoming bytes */
		private ByteOrder byteOrder;
		/** The decoded delegate metric ID */
		private IDelegateMetric delegateMetric;
		/** The length of the metric FQN */
		private int fqnLength = -1;
		/** The metric's FQN */
		private String fqn = null;
		/** The metric type */
		private MetricType type = null;
		/** The metric timestamp */
		private long ts;
		/** The metric's long value */
		private long lvalue;
		/** The metric's non-long value bytes */
		private ByteBuffer value;
		
		/**
		 * Returns the length of the FQN in bytes
		 * @return the fqn length
		 */
		public int getFqnLength() {
			return fqnLength;
		}

		/**
		 * Sets the FQN length
		 * @param fqnLength the fqn length to set
		 */
		public void setFqnLength(int fqnLength) {
			this.fqnLength = fqnLength;
		}

		/**
		 * Returns the metric type
		 * @return the type
		 */
		public MetricType getType() {
			return type;
		}


		/**
		 * Sets the type
		 * @param type the type to set
		 */
		public void setType(MetricType type) {
			this.type = type;
		}

		/**
		 * @param vlength the vlength to set
		 */
		public void setVlength(int vlength) {
			this.value = MetricType.allocate(vlength);
		}

		/**
		 * Returns the value container byte buffer
		 * @return the value container byte buffer
		 */
		public ByteBuffer getValue() {
			return value;
		}

		/**
		 * Sets the byte order for the submitted metric
		 * @param byteOrder the byte order to set
		 */
		public void setByteOrder(ByteOrder byteOrder) {
			this.byteOrder = byteOrder;
		}

		/**
		 * Sets the delegate metric when the submission is tokenized
		 * @param delegateMetric the delegate metric to set
		 */
		public void setDelegateMetric(IDelegateMetric delegateMetric) {
			this.delegateMetric = delegateMetric;
			this.type = this.delegateMetric.getType();
		}

		/**
		 * Sets the FQN
		 * @param fqn the fqn to set
		 */
		public void setFqn(String fqn) {
			this.fqn = fqn;
			setDelegateMetric(metricCatalog.build(fqn, type));
		}

		/**
		 * Sets the metric timestamp
		 * @param ts the ts to set
		 */
		public void setTs(long ts) {
			this.ts = ts;
		}

		/**
		 * Sets the value when the metric type is a long 
		 * @param lvalue the long value to set
		 */
		public void setLvalue(long lvalue) {
			this.lvalue = lvalue;
		}



		/**
		 * Builds an IMetric
		 * @return an IMetric
		 */
		IMetric buildMetric() {
			return type.isLong() ? ICEMetric.newMetric(ts, lvalue, type, delegateMetric) : ICEMetric.newMetric(ts, value, type, delegateMetric);
		}
	}


}
