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

import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

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
	 * @param name The name of the XML content this classifier identifies
	 */
	public StaxContentClassifier(String name) {
		super(name);
	}

	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.unification.pipeline2.Initiator#match(org.jboss.netty.buffer.ChannelBuffer)
	 */
	@Override
	public Object match(ChannelBuffer buff) {
		if(super.match(buff)==null) return null;
		int events = 0, tags = 0;
		ChannelBufferInputStream cbis = null;
		XMLStreamReader xReader = null;
		buff.resetReaderIndex();
		try {
			cbis = new ChannelBufferInputStream(buff);
			xReader = xmlInputFactory.createXMLStreamReader(cbis);
			Map<Object, Object> state = new HashMap<Object, Object>();
			while(xReader.hasNext()) {
				events++; if(events > maxEvents) return null;
				int type = xReader.next();
				if(type==XMLStreamConstants.START_ELEMENT) {
					tags++; if(tags > maxTags) return null;
					Object matchKey = isMatch(type, xReader, state);
					if(matchKey!=null) return matchKey;
				}
			}
			return null;
		} catch (Exception ex) {
			return null;
		} finally {
			if(xReader!=null) try { xReader.close(); } catch (Exception x) { /* No Op */ }
			if(cbis!=null) try { cbis.close(); } catch (Exception x) { /* No Op */ }
			buff.resetReaderIndex();
		}
	}
	
	
	/**
	 * Concrete classes should implement this method to test each event supplied by the main {@link #match(ChannelBuffer)} method loop.
	 * When and if a match is made, the method should return the match key.
	 * @param evenType The event type id of the current event
	 * @param streamReader The stream reader from which the remaining details of the event can be read
	 * @param state A map supplied by the caller to allow the inner match to maintain state. The map will be the same instance
	 * across multiple sequential calls for the same stream reader.
	 * @return the match key, or null if one was not found
	 */
	protected abstract Object isMatch(int evenType, XMLStreamReader streamReader, Map<Object, Object> state);


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
