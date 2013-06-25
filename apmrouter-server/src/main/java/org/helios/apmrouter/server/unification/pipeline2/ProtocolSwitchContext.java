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

import java.lang.reflect.Field;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.helios.apmrouter.ref.RunnableReferenceQueue;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.UpstreamMessageEvent;

/**
 * <p>Title: ProtocolSwitchContext</p>
 * <p>Description: Some contextual state retained on behalf of a channel during the port protocol switch.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.unification.pipeline2.ProtocolSwitchContext</code></p>
 */

public class ProtocolSwitchContext {
	/** A set of protocol initiators which have definitively failed matching for this context */
	protected final Set<Initiator> pInitiators = new HashSet<Initiator>();
	
	/** Static class logger */
	protected static final Logger LOG = Logger.getLogger(ProtocolSwitchContext.class);
	
	/** The ReplayingDecoderBuffer class */
	protected static final Class<?> REPLAY_BUFFER_CLASS;
	/** The replay decoeer buffer replay error instance */
	protected static final Error REPLAY_ERROR;
	
	static {
		try {
			REPLAY_BUFFER_CLASS = Class.forName("org.jboss.netty.handler.codec.replay.ReplayingDecoderBuffer", true, Channel.class.getClassLoader());
			Field f = REPLAY_BUFFER_CLASS.getDeclaredField("REPLAY");
			f.setAccessible(true);
			REPLAY_ERROR = (Error)f.get(null);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	/** The channel this context is created for */
	protected final Channel channel;
	/** The channel's pipeline */
	protected final ChannelPipeline pipeline;
	
	/** The current channel handler context */
	protected ChannelHandlerContext ctx;
	/** The currently processing buffer */
	protected ChannelBuffer buffer = null;
	/** The current buffer's currently readable bytes */
	protected int readableBytes = -1;
	/** The current switch phase */
	protected SwitchPhase phase = null;
	/** The number of bytes provided in the prior {@link ProtocolSwitchDecoder#decode(ChannelHandlerContext, Channel, ChannelBuffer, SwitchPhase)} call */
	protected int priorReadBytes = 0;
	/** The current initiator to be called for a given state */
	protected final Map<SwitchPhase, Initiator> nextInitiators = new EnumMap<SwitchPhase, Initiator>(SwitchPhase.class);
	/** Indicates if aggregation has taken place within the scope of this context */
	protected boolean aggregationHasOccured = false;
	
	
	/**
	 * Sends the current buffer upstream
	 * @param handlerName the name of the handler to which the upstream event should be targetted
	 */
	public void sendCurrentBufferUpstream(String handlerName) {
		ChannelBuffer cb = unReplayChannelBuffer();
		LOG.info("Unreplayed Buffer Readable:[ " + cb.readableBytes() + "] -->" + cb.toString() + " ----> to handler [" + handlerName + "]");
		UpstreamMessageEvent evt = new UpstreamMessageEvent(channel, cb, channel.getRemoteAddress());
		pipeline.getContext(handlerName).sendUpstream(evt);		
	}
	
	
	/**
	 * Converts a replay channel buffer to a regular direct buffer
	 * @return a regular direct buffer
	 */
	public ChannelBuffer unReplayChannelBuffer() {
		ChannelBuffer cb = null;
		if(REPLAY_BUFFER_CLASS.isInstance(buffer)) {
			cb = ChannelBuffers.directBuffer(buffer.order(), readableBytes);
			cb.writeBytes(buffer);
			this.buffer = cb;
		} else {
			cb = buffer;
		}
		return cb;
	}
	
	/**
	 * Throws the replay exception causing the replay decoder to re-submit
	 */
	public void replay() {
		throw REPLAY_ERROR;
	}
	
	

	/**
	 * Clears the nextInitiators map
	 * @return this context
	 */
	ProtocolSwitchContext clearNextInitiators() {
		nextInitiators.clear();
		return this;
	}
	
	/**
	 * Sets the next initiator for the passed phases
	 * @param initiator The initiator to set. If null, the current initiator will be cleared
	 * @param phases The phases to set the initiator for
	 * @return this context
	 */
	ProtocolSwitchContext setNextInitiator(Initiator initiator, SwitchPhase...phases) {		
		if(phases!=null) {
			for(SwitchPhase phase: phases) {
				if(phase==null) continue;
				if(initiator==null) {
					nextInitiators.remove(phase);
				} else {
					nextInitiators.put(phase, initiator);
				}
			}
		}
		return this;
	}
	
	/**
	 * Returns the next initiator for the passed phase
	 * @param phase The phase to get the initiator for
	 * @return the next initiator or null if one was not set
	 */
	Initiator getInitiator(SwitchPhase phase) {
		if(phase==null) throw new IllegalArgumentException("The passed phase was null", new Throwable());
		return nextInitiators.get(phase);
	}
	
	/**
	 * Creates a new ProtocolSwitchContext
	 * @param ctx The current channel handler context
	 * @param channel The channel this context is created for
	 * @param buffer The currently processing buffer
	 * @param readableBytes The current buffer's currently readable bytes
	 * @param state the initial switch phase
	 */
	public ProtocolSwitchContext(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer, int readableBytes, SwitchPhase state) {
		this.channel = channel;
		this.pipeline = channel.getPipeline();
		this.buffer = buffer;
		this.phase = state;
		this.readableBytes = readableBytes;
		this.ctx = ctx;		
		final int channelId = channel.getId();
		RunnableReferenceQueue.getInstance().buildWeakReference(this, new Runnable(){
			@Override
			public void run() {
				LOG.info("\n\t*********************\n\t[" + new Date() + "] Enqueued ProtocolSwitchContext for Channel [" + channelId + "]\n\t*********************\n");
			}
		});
		ctx.setAttachment(this);
	}
	
	/**
	 * Clears this context from the channel handler context where it was attached, unless it no longer attached there.
	 */
	public void clear() {
		Object attachment = ctx.getAttachment();
		if(attachment!=null) {
			if(attachment==this) {
				ctx.setAttachment(null);
			}
		}
	}
	
	/**
	 * Updates the current context for each call to {@link ProtocolSwitchDecoder#decode(ChannelHandlerContext, Channel, ChannelBuffer, SwitchPhase)}
	 * @param ctx The current channel handler context
	 * @param buffer The currently processing buffer
	 * @param readableBytes The current buffer's currently readable bytes
	 * @param state the initial switch phase
	 * @return this context
	 */
	public ProtocolSwitchContext update(ChannelHandlerContext ctx, ChannelBuffer buffer, int readableBytes, SwitchPhase state) {
		this.ctx = ctx;
		this.buffer = buffer;
		this.phase = state;
		this.priorReadBytes = this.readableBytes; 
		this.readableBytes = readableBytes;		
		return this;
	}
	
	/**
	 * Returns the number of bytes provided in the prior {@link ProtocolSwitchDecoder#decode(ChannelHandlerContext, Channel, ChannelBuffer, SwitchPhase)} call
	 * @return the number of bytes provided in the prior
	 */
	int getPriorReadBytes() {
		return priorReadBytes;
	}
	
	/**
	 * Indicates if this decode call supplied additional bytes beyond the prior call assuming no bytes were read from the buffer.
	 * @return true if this decode call supplied additional bytes, false if the same size buffer was passed
	 */
	boolean readMoreBytes() {
		return readableBytes > priorReadBytes;
	}

	/**
	 * Returns the approriate read more phase for the current phase
	 * @return a read more phase different from the current phase
	 */
	SwitchPhase readMore() {
		return phase==SwitchPhase.READ_MORE_1 ? SwitchPhase.READ_MORE_2 : SwitchPhase.READ_MORE_1;
	}
	
	/**
	 * Resets the failed protocol initiators
	 * @return this context
	 */
	ProtocolSwitchContext resetInitiators() {
		pInitiators.clear();
		return this;		
	}
	
	/**
	 * Fails the passed {@link Initiator} and returns this context
	 * @param pi the failed Initiator
	 * @return this context
	 */
	ProtocolSwitchContext failInitiator(Initiator pi) {
		pInitiators.add(pi);
		return this;
	}
	
	/**
	 * Determines if this context has failed the passed {@link Initiator} 
	 * @param pi the {@link Initiator} to test
	 * @return true if failed, false otherwise
	 */
	boolean hasInitiatorFailed(Initiator pi) {
		return pInitiators.contains(pi); 
	}

	/**
	 * Returns the current channel handler context
	 * @return the current channel handler context
	 */
	public ChannelHandlerContext getCtx() {
		return ctx;
	}

	/**
	 * Returns the channel associated with this context
	 * @return the channel associated with this context
	 */
	public Channel getChannel() {
		return channel;
	}

	/**
	 * Returns the pipeline associated with this context
	 * @return the pipeline associated with this context
	 */
	public ChannelPipeline getPipeline() {
		return pipeline;
	}

	/**
	 * Returns the current buffer being processed
	 * @return the current buffer being processed
	 */
	public ChannelBuffer getBuffer() {
		return buffer;
	}

	/**
	 * Returns the current buffer's currently readable bytes
	 * @return the current buffer's currently readable bytes
	 */
	public int getReadableBytes() {
		return buffer==null ? 0 : buffer.readableBytes();
	}
	
	/**
	 * Returns the current replaying decoder buffer's currently actual readable bytes
	 * @return the current replaying decoder buffer's currently actual readable bytes
	 */
	public int getActualReadableBytes() {
		return readableBytes;
	}

	/**
	 * Returns the current switch phase
	 * @return the current switch phase
	 */
	public SwitchPhase getPhase() {
		return phase;
	}

	/**
	 * Sets the current switch phase
	 * @param state the current switch phase
	 */
	public void setPhase(SwitchPhase state) {
		this.phase = state;
	}

	/**
	 * Indicates if aggregation has taken place within the scope of this context
	 * @return true if aggregation has taken place within the scope of this context, false otherwise
	 */
	public boolean aggregationHasOccured() {
		return aggregationHasOccured;
	}

	/**
	 * Sets the aggregation-has-occured flag to true
	 */
	public void setAggregationHasOccured() {
		this.aggregationHasOccured = true;
	}
	
	
	

}
