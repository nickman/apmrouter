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
package org.helios.apmrouter.server.unification;

import org.helios.apmrouter.server.ServerComponentBean;
import org.helios.apmrouter.server.unification.pipeline2.ProtocolSwitchDecoder;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelState;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.DefaultChannelPipeline;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.logging.InternalLogLevel;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.logging.Log4JLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedAttribute;


/**
 * <p>Title: ServerPipelineFactory</p>
 * <p>Description: The factory that creates pipelines for each connecting client. The handlers that are inserted into the pipeline
 * will be specific to the type of Ajax push that the client requests.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.unification.ServerPipelineFactory</code></p>
 */
public class ServerPipelineFactory extends ServerComponentBean implements ChannelPipelineFactory {
	/** The execution handler */
	protected ExecutionHandler executionHandler = new ExecutionHandler(new OrderedMemoryAwareThreadPoolExecutor(16, 1048576, 1048576));
	/** The port unification pipeline switch */
	protected ProtocolSwitchDecoder ps = null;
	
	/** The {@link #installedLogger} code for the hex logger */
	public static final String HEX_LOGGER = "hex";
	/** The {@link #installedLogger} code for the nohex logger */
	public static final String NOHEX_LOGGER = "nohex";
	
	/** Non hex logging handler */
	protected final LoggingHandler nonHexLoggingHandler = new LoggingHandler(getClass(), InternalLogLevel.INFO, false);
	/** Hex logging handler */
	protected final LoggingHandler hexLoggingHandler = new LoggingHandler(getClass(), InternalLogLevel.INFO, true);
	
	
	/** The code indicating which logger should be installed */
	protected String installedLogger = null;
	
	/**
	 * Decodes the passed logger code
	 * @param code The logger code
	 * @return the decoded logger
	 */
	protected LoggingHandler decode(String code) {
		if(code==null || code.trim().isEmpty()) return null;
		String _code = code.trim().toLowerCase();
		if(HEX_LOGGER.equals(_code)) return hexLoggingHandler;
		if(NOHEX_LOGGER.equals(_code)) return nonHexLoggingHandler;
		throw new IllegalArgumentException("Invalid logger code [" + code + "]", new Throwable());
	}
	
	/** The anchor handler */
	protected final SimpleChannelUpstreamHandler anchor = new SimpleChannelUpstreamHandler();
	
	
	/**
	 * Returns the installed logging handler code
	 * @return the installed logging handler code
	 */
	@ManagedAttribute(description="The logging handler installed (null/hex/nohex) ")
	public String getInstalledLogger() {
		return installedLogger;
	}


	/**
	 * Sets the installed logging handler code
	 * @param installedLogger the installed logging handler code
	 */
	@ManagedAttribute(description="The logging handler installed (null/hex/nohex) ")
	public void setInstalledLogger(String installedLogger) {
		this.installedLogger = installedLogger;
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#doStart()
	 */
	@Override
	public void doStart() throws Exception {
		super.doStart();
		InternalLoggerFactory.setDefaultFactory(new Log4JLoggerFactory());
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelPipelineFactory#getPipeline()
	 */
	@Override
	public ChannelPipeline getPipeline() throws Exception {
		ChannelPipeline pipeline = Channels.pipeline();
		pipeline.addFirst("anchor", anchor);
//		DefaultChannelPipeline pipeline = new DefaultChannelPipeline() {
//			@Override
//			public void sendUpstream(ChannelEvent e) {
//				if(e instanceof ChannelStateEvent) {
//					ChannelStateEvent stateEvent = (ChannelStateEvent)e;
//					if(stateEvent.getState()==ChannelState.OPEN && stateEvent.getValue()==Boolean.FALSE) {
//						log.info("Deferred Close....");
//						return;
//					}
//				}
//				super.sendUpstream(e);
//			}
//		};
//		pipeline.addFirst("closeDefer", closeDeferred);
		LoggingHandler lh = decode(installedLogger);
		if(lh!=null) pipeline.addLast("logging", lh);		
		pipeline.addLast(ProtocolSwitchDecoder.PIPE_NAME, applicationContext.getBean("protocolSwitchDecoder", ProtocolSwitchDecoder.class));
		pipeline.addLast("exec", executionHandler);
		return pipeline;
	}

	/**
	 * Sets the protocol switch
	 * @param ps the protocol switch
	 */
	@Autowired(required=true)
	public void setProtocolSwitch(ProtocolSwitchDecoder ps) {
		this.ps = ps;
	}
	
}
