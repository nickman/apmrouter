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

import javax.xml.stream.XMLInputFactory;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;

/**
 * <p>Title: StaxContentClassifier</p>
 * <p>Description: A configurable XML content classifier that uses a Stax parser to attempt to locate recognized xml content within the header of the passed channel buffer</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.unification.pipeline2.content.StaxContentClassifier</code></p>
 */

public abstract class StaxContentClassifier extends XMLContentClassifier {
	/** The maximum number of XML parsing events that can occur before a match fails */
	protected int maxEvents = 10;
	/** The maximum number of XML element tag parsing events that can occur before a match fails */
	protected int maxTags = 3;
	
	/** The streaming xml parser factory */
	protected final XMLInputFactory xmlInputFactory =  XMLInputFactory.newInstance();
	
	
	/**
	 * Creates a new StaxContentClassifier
	 * @param name
	 */
	public StaxContentClassifier(String name) {
		super(name);
	}

	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.unification.pipeline2.Initiator#match(org.jboss.netty.buffer.ChannelBuffer)
	 */
	@Override
	public boolean match(ChannelBuffer buff) {
		if(!super.match(buff)) return false;
		ChannelBufferInputStream cbis = null;
		buff.resetReaderIndex();
		try {
			cbis = new ChannelBufferInputStream(buff);
			
			return true;
		} catch (Exception ex) {
			return false;
		} finally {
			if(cbis!=null) try { cbis.close(); } catch (Exception x) { /* No Op */ }
			buff.resetReaderIndex();
		}
		
		
	}


	/**
	 * Returns the maximum number of XML parsing events that can occur before a match fails
	 * @return the maximum number of XML parsing events that can occur before a match fails
	 */
	public int getMaxEvents() {
		return maxEvents;
	}


	/**
	 * Sets the maximum number of XML parsing events that can occur before a match fails
	 * @param maxEvents the maximum number of XML parsing events that can occur before a match fails
	 */
	public void setMaxEvents(int maxEvents) {
		this.maxEvents = maxEvents;
	}


	/**
	 * Returns the maximum number of XML element parsing events that can occur before a match fails
	 * @return the maximum number of XML element parsing events that can occur before a match fails
	 */
	public int getMaxTags() {
		return maxTags;
	}


	/**
	 * Sets the maximum number of XML element parsing events that can occur before a match fails
	 * @param maxTags the maximum number of XML element parsing events that can occur before a match fails
	 */
	public void setMaxTags(int maxTags) {
		this.maxTags = maxTags;
	}

}
