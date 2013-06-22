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
package org.helios.apmrouter.server.unification.pipeline2.content;

import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

/**
 * <p>Title: ConfigurableStaxContentClassifier</p>
 * <p>Description: A configurable {@link StaxContentClassifier} that looks for the configured tag names.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.unification.pipeline2.content.ConfigurableStaxContentClassifier</code></p>
 */

public abstract class ConfigurableStaxContentClassifier extends StaxContentClassifier {
	/** A map of tag names to match against in the order defined */
	protected final Map<String, String> targetTags = new HashMap<String, String>();
	
	
	/**
	 * Creates a new ConfigurableStaxContentClassifier
	 * @param name The name of the content classified by this classifier
	 */
	public ConfigurableStaxContentClassifier(String name) {
		super(name);
	}

	/**
	 * Adds the passed tags to the target tags to be matched against
	 * @param tags The tags to add
	 */
	public void setTags(Map<String, String> tags) {
		if(tags!=null) {
			for(Map.Entry<String, String> entry: tags.entrySet()) {
				String tag = entry.getKey();
				String matchKey = entry.getValue();
				
				if(tag!=null && !tag.trim().isEmpty() && matchKey!=null && !matchKey.trim().isEmpty()) {
					targetTags.put(tag.trim(), matchKey.trim());
				}
			}
		}
		if(targetTags.isEmpty()) {
			log.warn("The ConfigurableStaxContentClassifier has no target tags");
		}
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.unification.pipeline2.content.StaxContentClassifier#isMatch(int, javax.xml.stream.XMLStreamReader, java.util.Map)
	 */
	@Override
	protected Object isMatch(int eventType, XMLStreamReader streamReader, Map<Object, Object> state) {
		if(eventType==XMLStreamConstants.START_ELEMENT) {
			String tagName = streamReader.getLocalName();
			if(tagName!=null) {
				return targetTags.get(tagName.trim());
			}
		}
		return null;
	}

}
