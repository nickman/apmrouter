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
import org.helios.apmrouter.server.unification.pipeline2.content.ContentClassifier;
import org.helios.apmrouter.server.unification.pipeline2.encoding.EncodingInitiator;
import org.helios.apmrouter.server.unification.pipeline2.protocol.ProtocolInitiator;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelLocal;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.replay.ReplayingDecoder;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.logging.InternalLogLevel;
import org.springframework.jmx.export.annotation.ManagedAttribute;

/**
 * <p>Title: ProtocolSwitchDecoder</p>
 * <p>Description: A {@link ReplayingDecoder} implementation that manages the port unification protocol switch</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.unification.protocol.ProtocolSwitchDecoder</code></p>
 */

public class ProtocolSwitchDecoder extends ReplayingDecoder<SwitchPhase> {
	// JO NESB0
	// protocol initiators:   http / gzip / bzip
	// protocol decoders:   gzip / bzip
	// content classifiers:  this xml handler, or that binary fluff handler
	
	/** The port protocol switch context */
	protected final ChannelLocal<ProtocolSwitchContext> context = new ChannelLocal<ProtocolSwitchContext>(true);
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass());
	/** Non hex logging handler */
	protected final LoggingHandler nonHexLoggingHandler = new LoggingHandler(getClass(), InternalLogLevel.INFO, false);
	/** Hex logging handler */
	protected final LoggingHandler hexLoggingHandler = new LoggingHandler(getClass(), InternalLogLevel.INFO, true);
	/** The logger level */
	protected APMLogLevel level = APMLogLevel.pCode(log.getEffectiveLevel().toInt());
	/** The registered protocol initiators */
	protected final Set<ProtocolInitiator> pInitiators = new CopyOnWriteArraySet<ProtocolInitiator>();
	/** The registered encoding initiators */
	protected final Set<EncodingInitiator> encInitiators = new CopyOnWriteArraySet<EncodingInitiator>();
	/** The registered content classifiers */
	protected final Set<ContentClassifier> classifiers = new CopyOnWriteArraySet<ContentClassifier>();
	
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
		ProtocolSwitchContext portContext = context.get(channel);
		if(portContext==null) {
			portContext = new ProtocolSwitchContext();
			context.set(channel, portContext);
		}
		final long bytesAvailable = buffer.readableBytes();
		final ChannelPipeline pipeline = ctx.getPipeline();
		switch(state) {
			case INIT:
				for(ProtocolInitiator pi: pInitiators) {
					
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
		return null;
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
	

}
