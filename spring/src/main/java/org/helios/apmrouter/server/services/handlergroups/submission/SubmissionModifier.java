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
package org.helios.apmrouter.server.services.handlergroups.submission;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.helios.apmrouter.server.services.AbstractPipelineModifier;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.execution.ExecutionHandler;

/**
 * <p>Title: SubmissionModifier</p>
 * <p>Description: A modifier that creates a pipeline for accepting external metric submissions via HTTP.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.services.handlergroups.submission.SubmissionModifier</code></p>
 */

public class SubmissionModifier extends AbstractPipelineModifier {
	/** The handler that this modifier adds at the end of the pipeline */
	protected final ChannelHandler handler = new SubmissionHandler();
	/** The execution handler's pipeline name */
	protected String execName = null;
	/** An execution handler to hand off the metric submissions to */
	protected static final ExecutionHandler execHandler = new ExecutionHandler(Executors.newCachedThreadPool(			
			new ThreadFactory() {
				final AtomicInteger serial = new AtomicInteger(0);
				public Thread newThread(Runnable r) {
					Thread t = new Thread(r, "HttpMetricSubmissionThread#" + serial.incrementAndGet());
					t.setDaemon(true);
					return t;
				}
			}
	), false, true);
	

	/**
	 * <p>Overriden to set the pipeline name of an associated execution handler. 
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.services.AbstractPipelineModifier#setBeanName(java.lang.String)
	 */
	public void setBeanName(String name) {
		super.setBeanName(name);
		execName = name + "-Exec";
		
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.services.AbstractPipelineModifier#doGetChannelHandler()
	 */
	@Override
	protected ChannelHandler doGetChannelHandler() {
		return handler;
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.services.AbstractPipelineModifier#doModifyPipeline(org.jboss.netty.channel.ChannelPipeline)
	 */
	@Override
	protected void doModifyPipeline(ChannelPipeline pipeline) {
		if(pipeline.get(execName)==null) {
			pipeline.addLast(execName, execHandler);
		}
		if(pipeline.get(name)==null) {
			pipeline.addLast(name, handler);
		}
	}

}
