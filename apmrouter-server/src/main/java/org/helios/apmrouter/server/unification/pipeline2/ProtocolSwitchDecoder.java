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
package org.helios.apmrouter.server.unification.pipeline2;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.log4j.Logger;
import org.helios.apmrouter.logging.APMLogLevel;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.LifeCycleAwareChannelHandler;
import org.jboss.netty.handler.codec.replay.ReplayingDecoder;
import org.springframework.jmx.export.annotation.ManagedAttribute;


/**
 * <p>Title: ProtocolSwitchDecoder</p>
 * <p>Description: A {@link ReplayingDecoder} implementation that manages the port unification protocol switch</p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.unification.pipeline2.ProtocolSwitchDecoder</code></p>
 */
public class ProtocolSwitchDecoder extends ReplayingDecoder<SwitchPhase> {
	// JO NESB0
	// protocol initiators:   http / gzip / bzip
	// protocol decoders:   gzip / bzip
	// content classifiers:  this xml handler, or that binary fluff handler
	
//	/** The port protocol switch context */
//	protected final ChannelLocal<ProtocolSwitchContext> context = new ChannelLocal<ProtocolSwitchContext>(true);
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass());
	/** The logger level */
	protected APMLogLevel level = APMLogLevel.pCode(log.getEffectiveLevel().toInt());
	/** The registered protocol initiators */
	protected final Set<Initiator> pInitiators = new CopyOnWriteArraySet<Initiator>();
	
	/** The maximum number of bytes allowed for protocol/content negotiation */
	protected int maxInitiatorBytes = 1024;

	
	
	/**
	 * Adds a set of {@link Initiator}s to be considered in the protocol switch
	 * @param initiators a set of {@link Initiator}s
	 */
	public void setInitiators(Set<Initiator> initiators) {
		if(initiators!=null) {
			pInitiators.addAll(initiators);
		}
	}
	
	/** The name of this decoder in the pipeline */
	public static final String PIPE_NAME = "psd";
	/**
	 * Creates a new ProtocolSwitchDecoder
	 */
	public ProtocolSwitchDecoder() {
		super(SwitchPhase.INIT);
	}


	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.handler.codec.replay.ReplayingDecoder#decode(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.Channel, org.jboss.netty.buffer.ChannelBuffer, java.lang.Enum)
	 */
	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer, SwitchPhase state) throws Exception {
		if(state==SwitchPhase.COMPLETE) {			
			while(buffer.readableBytes()>0) {
				buffer.readByte();
			}
			return buffer;
		}
		final int bytesAvailable = super.actualReadableBytes();
		
		//ProtocolSwitchContext portContext = context.get(channel);
		ProtocolSwitchContext portContext = (ProtocolSwitchContext)ctx.getAttachment();
		if(portContext==null) {
			portContext = new ProtocolSwitchContext(ctx, channel, buffer, bytesAvailable, state);
			//context.set(channel, portContext);
			ctx.setAttachment(portContext);
			channel.getCloseFuture().addListener(new ChannelFutureListener() {				
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					log.info("\n\t*****************\n\tChannel Closed\n\t*****************");
				}
			});
		} else {
			portContext.update(ctx, buffer, bytesAvailable, state);
		}		
		
		log.info("Prior Bytes: Now:" + bytesAvailable + "  Prior:" + portContext.getPriorReadBytes());
		log.info("");
		switch(state) {
			case INIT:
				boolean found = false;
				boolean insufBytes = false;
				for(Initiator pi: pInitiators) {
					int requiredBytes = pi.requiredBytes();
					if(bytesAvailable < requiredBytes && requiredBytes > 0) {
						insufBytes = true;
						portContext.failInitiator(pi);
						continue;
					}
					
					if(!portContext.hasInitiatorFailed(pi)) {
						try {
							ChannelBuffer copiedBuffer = buffer.copy(0, bytesAvailable);
							Object matchKey = pi.match(copiedBuffer); 
							if(matchKey==null) {
								portContext.failInitiator(pi);
								log.info("PI [" + pi.getName() + "] failed");
								continue;
							}
							log.info("PI [" + pi.getName() + "] MATCHED");
							found = true;
							portContext.update(ctx, copiedBuffer, bytesAvailable, state);
							SwitchPhase phase = pi.process(portContext, matchKey);
							if(phase!=null) {
								checkpoint(phase);
							} else {
								buffer.getByte(Integer.MAX_VALUE);
							}							
							return null;
						} catch (Exception ex) {
							portContext.failInitiator(pi);
							log.info("PI [" + pi.getName() + "] failed");
							continue;							
						}
					}
				}
				if(insufBytes) {
					// this means that no initiator matched, but at least 1 initiator
					// reported insufficient bytes to declare a match or miss.
					// therefore we need to try to keep reading bytes until we get a match,
					// or, if we determine that the max number of bytes [or all of the bytes]
					// have been read, we throw a content not recognized error
				}
				break;		
			case COMPDETECT:
				break;
			case COMPLETE:
				break;
			case CONTENT:
				break;
			case CONTENTDETECT:
				break;
			case DECOMP:
				break;
			case ERROR:
				break;
		}
		throw new RuntimeException();
		//return null;
	}
	
	/**
	 * Returns the level of this instance's logger
	 * @return the level of this instance's logger
	 */
	@ManagedAttribute(description="The logging level of this component")
	public String getLevel() {
		return level.name();
	}
	
	/**
	 * Sets the logging level for this instance
	 * @param levelName the name of the logging level for this instance
	 */
	@ManagedAttribute(description="The logging level of this component")
	public void setLevel(String levelName) {
		level = APMLogLevel.valueOfName(levelName);
		log.setLevel(level.getLevel());
		log.info("Set Logger to level [" + log.getLevel().toString() + "]");
	}


	/**
	 * Returns the maximum number of bytes allowed for protocol/content negotiation
	 * @return the maximum number of bytes allowed for protocol/content negotiation
	 */
	@ManagedAttribute(description="The maximum number of bytes allowed for protocol/content negotiation")
	public int getMaxInitiatorBytes() {
		return maxInitiatorBytes;
	}


	/**
	 * Sets the maximum number of bytes allowed for protocol/content negotiation
	 * @param maxInitiatorBytes the maximum number of bytes allowed for protocol/content negotiation
	 */
	@ManagedAttribute(description="The maximum number of bytes allowed for protocol/content negotiation")
	public void setMaxInitiatorBytes(int maxInitiatorBytes) {
		this.maxInitiatorBytes = maxInitiatorBytes;
	}

	public void afterAdd(ChannelHandlerContext ctx) throws Exception  {
		log.info("HANDLER EVENT: AfterAdd [" + ctx.getName() + "]");
		super.afterAdd(ctx);
	}
	
	public void beforeAdd(ChannelHandlerContext ctx) throws Exception  {
		log.info("HANDLER EVENT: BeforeAdd [" + ctx.getName() + "]");
		super.beforeAdd(ctx);
	}
	
	public void afterRemove(ChannelHandlerContext ctx) throws Exception  {
		log.info("HANDLER EVENT: AfterRemove[" + ctx.getName() + "]");
		super.afterRemove(ctx);
	}
	
	public void beforeRemove(ChannelHandlerContext ctx) throws Exception  {
		log.info("HANDLER EVENT: beforeRemove[" + ctx.getName() + "]");
		super.beforeRemove(ctx);
	}
	
	
	
	

}
