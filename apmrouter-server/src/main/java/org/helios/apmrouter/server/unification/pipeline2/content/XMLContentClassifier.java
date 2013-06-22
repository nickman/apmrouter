/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2013, Helios Development Group and individual contributors
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
package org.helios.apmrouter.server.unification.pipeline2.content;

import org.helios.apmrouter.server.unification.pipeline2.AbstractInitiator;
import org.jboss.netty.buffer.ChannelBuffer;

/**
 * <p>Title: XMLContentClassifier</p>
 * <p>Description: A partial content classifier that determines if a channel buffer contains xml</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.unification.pipeline2.content.XMLContentClassifier</code></p>
 */

public abstract class XMLContentClassifier extends AbstractInitiator {
	
	/** The size of a standard xml header */
	public static final int XML_HEADER_SIZE = 38;
	
	// <?xml version="1.0" encoding="UTF-8"?>
	
	/** Skip leading char for a tab */
	public static final int TAB = 9;
	/** Skip leading char for an EOL */
	public static final int EOL = 10;
	/** Skip leading char for a space */
	public static final int SPACE = 20;
	
	/** The xml header signature. i.e. <b><code></code></b> */
	private static final int[] XML_HEADER_SIG = {60, 63, 120, 109, 108};

	/**
	 * Creates a new XMLContentClassifier
	 * @param name The name of this Initiator
	 */
	public XMLContentClassifier(String name) {
		super(5, name);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.unification.pipeline2.Initiator#match(org.jboss.netty.buffer.ChannelBuffer)
	 */
	@Override
	public Object match(ChannelBuffer buff) {
		// read out the white space
		while(true) {
			int readByte = buff.readUnsignedByte();
			if(readByte != TAB && readByte != EOL && readByte != SPACE) {
				break;
			}			
		}
		for(int i = 0; i < XML_HEADER_SIG.length; i++) {
			if(buff.getUnsignedByte(i) != XML_HEADER_SIG[i]) return null;
		}
		return true;
	}




	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.unification.pipeline2.Initiator#requiredBytes()
	 */
	@Override
	public int requiredBytes() {
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.unification.pipeline2.Initiator#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

}
