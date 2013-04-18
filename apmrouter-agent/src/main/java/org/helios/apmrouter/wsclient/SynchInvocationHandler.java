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
package org.helios.apmrouter.wsclient;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.helios.apmrouter.sender.SynchOpSupport;
import org.helios.apmrouter.util.SimpleLogger;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.json.JSONObject;

/**
 * <p>Title: SynchInvocationHandler</p>
 * <p>Description: Handler inserted into the pipeline to capture the result of a synchronous invocation.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.wsclient.SynchInvocationHandler</code></p>
 */

public class SynchInvocationHandler extends SimpleChannelUpstreamHandler {
	/** the request id being listened for  */
	protected volatile long currentRequestId = -1;
	/** The most recently received response */
	protected volatile JSONObject response = null;
	/** The most recently set latch */
	protected volatile CountDownLatch latch = null;
	
	
	/**
	 * Sets the request id of the pending request id
	 * @param rid the pending request id
	 * @param latch The latch to countdown on request completion
	 */
	public void prepSynchRequest(long rid, final CountDownLatch latch) {
		currentRequestId = rid;
		this.latch = latch;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#messageReceived(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.MessageEvent)
	 */
	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		JSONObject _response = (JSONObject)e.getMessage();
		long rerid = getResponseReId(_response);
		if(rerid==currentRequestId) {			
			this.response = _response;
			latch.countDown();
		} else {
			SimpleLogger.warn("Failed to match rerid [", rerid, "] when expecting [", currentRequestId, "]");
		}
	}
	
	/**
	 * Returns the response of the invocation
	 * @param rid The request id
	 * @param synchRequestTimeout The synch invocation timeout in ms.
	 * @return the invocation response
	 */
	public JSONObject getSynchResponse(long rid, long synchRequestTimeout) {
		final boolean complete;
		final long _timeout = synchRequestTimeout;
		this.currentRequestId = rid;
		try {
			complete = latch.await(_timeout , TimeUnit.MILLISECONDS);
		} catch (InterruptedException iex) {
			throw new RuntimeException("MetricURI Operation Interrupted", iex);
		} 
		if(complete) {
			if(response!=null && getResponseReId(response)==rid) {
				return response;
			}
			throw new RuntimeException("Failed to get the response", new Throwable());			
		}
		throw new RuntimeException("Synch Request [" + rid + "] timed out after [" + synchRequestTimeout + "]", new Throwable());
	}
	
	/**
	 * Extracts the rerid from the response
	 * @param resp The repsonse to get the rerid from
	 * @return the rerid or -1 if one was not found
	 */
	protected long getResponseReId(JSONObject resp) {
		JSONObject _response = resp!=null ? resp : this.response;
		try {
			if(_response!=null && _response.has("rerid")) {
				return _response.getLong("rerid");
			}
			return -1L;
		} catch (Exception ex) {
			throw new RuntimeException("Failed to extract rerid from response", ex);
		}
	}

	/**
	 * Returns the request id being listened for 
	 * @return the currentRequestId
	 */
	public long getrId() {
		return currentRequestId;
	}

	/**
	 * Sets the request id to listen for
	 * @param currentRequestId the currentRequestId to set
	 */
	public void setrId(long currentRequestId) {
		this.currentRequestId = currentRequestId;
	}

}
