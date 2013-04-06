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

import org.helios.apmrouter.nash.NashRequest;
import org.helios.apmrouter.nash.handler.NashRequestHandler;

/**
 * <p>Title: NashCommandHandlerAdder</p>
 * <p>Description: A default installed request handler that allow the client to install new handlers.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.nash.handler.NashCommandHandlerAdder</code></p>
 */

public class NashCommandHandlerAdder implements NashRequestHandler {

	/**
	 * Creates a new NashCommandHandlerAdder
	 */
	public NashCommandHandlerAdder() {
		
	}

	/**
	 * Request format:<ol>
	 * 	<li>The handler class name <b>mandatory</b>: The name of the handler class to activate</li>
	 *  <li>A URL specifying the location of the jar containing the named class <b>optional</b>: An optional
	 *  URL pointing to the jar containing the referenced class</li>
	 * </ol>
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.nash.handler.NashRequestHandler#onNashRequest(org.helios.apmrouter.nash.NashRequest)
	 */
	@Override
	public void onNashRequest(NashRequest request) {
		String className = null;
		String jarUrl = null;
		String[] args = request.getArguments();
		if(args.length==0) { 
			request.err("Invalid argument. Arguments are: [class name] <[URL to jar]>");
		} 
		if(args.length>0) {
			
		}
	
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.nash.handler.NashRequestHandler#getCommandName()
	 */
	@Override
	public String getCommandName() {
		return null;
	}

}
