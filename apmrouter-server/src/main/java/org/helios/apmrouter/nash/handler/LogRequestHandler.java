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
package org.helios.apmrouter.nash.handler;

import java.util.LinkedHashMap;
import java.util.Map;

import org.helios.apmrouter.nash.NashRequest;
import org.helios.apmrouter.nash.streams.NashRequestHandlerStreamServer;
import org.helios.apmrouter.nash.handler.NashRequestHandler;
import org.helios.apmrouter.nash.handler.NashStreamHandler;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.Delimiters;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.logging.InternalLogger;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.util.CharsetUtil;

/**
 * <p>Title: LogRequestHandler</p>
 * <p>Description: Simple {@link NashRequestHandler} implementation that logs the passed stream in accordance with the level argument.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.nash.handler.LogRequestHandler</code></p>
 */

public class LogRequestHandler implements NashRequestHandler, NashStreamHandler<String> {
	/** The created map of handlers */
	protected final Map<String, ChannelHandler> handlers;
	/** The internal logger */
	protected final InternalLogger log = InternalLoggerFactory.getInstance(getClass());	

	/**
	 * Creates a new LogRequestHandler
	 */
	public LogRequestHandler() {
		handlers = new LinkedHashMap<String, ChannelHandler>();
		handlers.put("frameDecoder", new DelimiterBasedFrameDecoder(1024, Delimiters.lineDelimiter()));
		handlers.put("stringDecoder", new StringDecoder(CharsetUtil.UTF_8));
		NashRequestHandlerStreamServer.getInstance().newCommandHandlerStreamServer(this);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.nash.handler.NashRequestHandler#onNashRequest(org.helios.apmrouter.nash.NashRequest)
	 */
	@Override
	public void onNashRequest(NashRequest request) {
		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.nash.handler.NashRequestHandler#getCommandName()
	 */
	public String getCommandName() {
		return "log";
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.nash.handler.NashStreamHandler#getChannelHandlers()
	 */
	@Override
	public Map<String, ChannelHandler> getChannelHandlers() {
		return handlers;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.nash.handler.NashStreamHandler#onStreamDecode(java.lang.Object)
	 */
	@Override
	public void onStreamDecode(String decodedEvent) {
		
	}

}
