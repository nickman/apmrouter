/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2012, Helios Development Group and individual contributors
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
package org.helios.apmrouter.dataservice.json.marshalling;

import org.jboss.netty.buffer.ChannelBuffer;

/**
 * <p>Title: JSONMarshaller</p>
 * <p>Description: Defines a service that will marshall JSON for delivery to remote clients.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.dataservice.json.marshalling.JSONMarshaller</code></p>
 */

public interface JSONMarshaller {
	/**
	 * Marshalls the passed object to JSON text and then writes it into a new ChannelBuffer.
	 * @param obj The object to marshall
	 * @return A channel buffer containing the marshalled json
	 */
	public ChannelBuffer marshallToChannel(Object obj);
	
	/**
	 * Marshalls the passed object to JSON text
	 * @param obj The object to marshall
	 * @return the json text
	 */
	public String marshallToText(Object obj); 
	
}
