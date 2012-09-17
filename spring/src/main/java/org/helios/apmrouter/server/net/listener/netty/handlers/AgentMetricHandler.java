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
package org.helios.apmrouter.server.net.listener.netty.handlers;

import java.net.SocketAddress;

import org.helios.apmrouter.ReceiverOpCode;
import org.helios.apmrouter.SenderOpCode;
import org.helios.apmrouter.metric.IMetric;
import org.helios.apmrouter.metric.catalog.IMetricCatalog;
import org.helios.apmrouter.router.PatternRouter;
import org.helios.apmrouter.server.ServerComponentBean;
import org.helios.apmrouter.trace.DirectMetricCollection;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedAttribute;

/**
 * <p>Title: AgentMetricHandler</p>
 * <p>Description: A cross netty protocol agent metric handler.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.net.listener.netty.handlers.AgentMetricHandler</code></p>
 */

public class AgentMetricHandler extends ServerComponentBean implements AgentRequestHandler {
	/** The metric catalog */
	protected IMetricCatalog metricCatalog = null;
	/** The pattern router for handing off metrics to */
	protected PatternRouter router = null;
	
	/** The OpCodes this handler accepts */
	protected final SenderOpCode[] OP_CODES = new SenderOpCode[]{ SenderOpCode.SEND_METRIC, SenderOpCode.SEND_METRIC_DIRECT };
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.net.listener.netty.handlers.AgentRequestHandler#getHandledOpCodes()
	 */
	@Override
	public SenderOpCode[] getHandledOpCodes() {
		return OP_CODES;
	}	
	
	/**
	 * Processes a channel buffer containing agent submitted metrics
	 * @param opCode The opCode for this request
	 * @param buff The channel buffer
	 * @param remoteAddress The remote address of the agent that sent the metrics
	 * @param channel The channel the metrics were received on
	 */
	@Override
	public void processAgentRequest(SenderOpCode opCode, ChannelBuffer buff, SocketAddress remoteAddress, Channel channel) {
		incr("BytesReceived", buff.getInt(2));
		DirectMetricCollection dmc = DirectMetricCollection.fromChannelBuffer(buff);		
//		int byteOrder = buff.getByte(1);
//		int totalSize = buff.getInt(2);
		IMetric[] metrics = null;
		try {
			metrics = dmc.decode();
		} catch (Exception e) {
			e.printStackTrace(System.err);					
		}	
		
		if(metrics!=null) incr("MetricsReceived", metrics.length);
		//dmc.destroy();
		for(final IMetric metric: metrics) {
			if(opCode==SenderOpCode.SEND_METRIC_DIRECT) {
				sendConfirm(channel, remoteAddress,  metric);
			}
			if(metric.getToken()==-1) {						
				sendToken(channel, remoteAddress,  metric);
			}			
		}
		router.route(metrics);
	}
	
	/**
	 * Confirms the receipt of a direct metric
	 * @param channel The channel on which the metric was received
	 * @param remoteAddress The remote address of the sender
	 * @param metric The direct metric
	 */
	protected void sendConfirm(Channel channel, SocketAddress remoteAddress, final IMetric metric) {
		String key = new StringBuilder(metric.getFQN()).append(metric.getTime()).toString();
		byte[] bytes = key.getBytes();
		// Buffer size:  OpCode, key size, key bytes
		ChannelBuffer cb = ChannelBuffers.directBuffer(1 + 4 + bytes.length);
		cb.writeByte(ReceiverOpCode.CONFIRM_METRIC.op());
		cb.writeInt(bytes.length);
		cb.writeBytes(bytes);		
		
		if(!channel.isConnected()) channel.connect(remoteAddress).awaitUninterruptibly(); 
		channel.write(cb, remoteAddress).addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if(future.isSuccess()) {
					incr("ConfirmsSent");
					//System.out.println("Sent Token for [" + metric.getFQN() + "]");
				} else {
					System.err.println("Failed to send confirm for direct metric [" + metric + "]");
					future.getCause().printStackTrace(System.err);
				}
				
			}
		});
		
		
	}
	
	
	/**
	 * When an untokenized metric is received, the token is generated from the metric catalog
	 * and returned to the caller in this protocol:<ol>
	 * 	<li>0 for the op-code (1 byte)</li>
	 *  <li>The size of the metric FQN (1 int)</li>
	 *  <li>The metric FQN's bytes  (n bytes)</li>
	 *  <li>The metric token (1 long)</li>
	 * </ol>
	 * @param channel The channel on which the untokenized metric was received
	 * @param address The remote address of the sender
	 * @param metric The untokenized metric
	 */
	protected void sendToken(final Channel channel, final SocketAddress address, final IMetric metric) {
		final long token = metricCatalog.setToken(metric);		
		byte[] bytes = metric.getFQN().getBytes();
		// Buffer size:  OpCode, fqn size, fqn bytes, token
		ChannelBuffer cb = ChannelBuffers.directBuffer(1 + 4 + bytes.length + 8 );
		cb.writeByte(ReceiverOpCode.SEND_METRIC_TOKEN.op());
		cb.writeInt(bytes.length);
		cb.writeBytes(bytes);
		cb.writeLong(token);
		
		if(!channel.isConnected()) channel.connect(address).awaitUninterruptibly(); 
		channel.write(cb, address).addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if(future.isSuccess()) {
					//System.out.println("Sent Token for [" + metric.getFQN() + "]");
					incr("TokensSent");
				} else {
					System.err.println("Failed to send token for [" + metric.getFQN() + "]");
					future.getCause().printStackTrace(System.err);
				}
				
			}
		});
		
	}

	/**
	 * Returns the metric catalog
	 * @return the metricCatalog
	 */
	public IMetricCatalog getMetricCatalog() {
		return metricCatalog;
	}

	/**
	 * Sets the metric catalog
	 * @param metricCatalog the metricCatalog to set
	 */
	public void setMetricCatalog(IMetricCatalog metricCatalog) {
		this.metricCatalog = metricCatalog;
	}
	
	/**
	 * Returns the number of bytes received
	 * @return the number of bytes received
	 */
	@ManagedAttribute
	public long getBytesReceived() {
		return getMetricValue("BytesReceived");
	}
	
	/**
	 * Returns the number of metrics received
	 * @return the number of metrics received
	 */
	@ManagedAttribute
	public long getMetricsReceived() {
		return getMetricValue("MetricsReceived");
	}
	
	/**
	 * Returns the number of metric confirms sent
	 * @return the number of metric confirms sent
	 */
	@ManagedAttribute
	public long getConfirmsSent() {
		return getMetricValue("ConfirmsSent");
	}	
	
	/**
	 * Returns the number of tokens sent
	 * @return the number of tokens sent
	 */
	@ManagedAttribute
	public long getTokensSent() {
		return getMetricValue("TokensSent");
	}		
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponent#getSupportedMetricNames()
	 */
	@Override
	public String[] getSupportedMetricNames() {
		return new String[]{"BytesReceived", "MetricsReceived", "ConfirmsSent", "TokensSent"};
	}

	/**
	 * Sets the pattern router
	 * @param router the router to set
	 */
	@Autowired(required=true)
	public void setRouter(PatternRouter router) {
		this.router = router;
	}




	
}
