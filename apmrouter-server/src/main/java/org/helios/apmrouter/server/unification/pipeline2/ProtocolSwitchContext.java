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
package org.helios.apmrouter.server.unification.pipeline2;

import java.util.HashSet;
import java.util.Set;

import org.helios.apmrouter.server.unification.pipeline2.content.ContentClassifier;
import org.helios.apmrouter.server.unification.pipeline2.encoding.EncodingInitiator;
import org.helios.apmrouter.server.unification.pipeline2.protocol.ProtocolInitiator;

/**
 * <p>Title: ProtocolSwitchContext</p>
 * <p>Description: Some contextual state retained on behalf of a channel during the port protocol switch.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.unification.pipeline2.ProtocolSwitchContext</code></p>
 */

public class ProtocolSwitchContext {
	/** A set of protocol initiators which have definitively failed matching for this context */
	protected final Set<ProtocolInitiator> pInitiators = new HashSet<ProtocolInitiator>();
	/** A set of encoding initiators which have definitively failed matching for this context */
	protected final Set<EncodingInitiator> encInitiators = new HashSet<EncodingInitiator>();
	/** A set of content classifiers  which have definitively failed matching for this context */
	protected final Set<ContentClassifier> classifiers = new HashSet<ContentClassifier>();

	/**
	 * Creates a new ProtocolSwitchContext
	 */
	public ProtocolSwitchContext() {
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * Fails the passed {@link ProtocolInitiator} and returns this context
	 * @param pi the failed ProtocolInitiator
	 * @return this context
	 */
	ProtocolSwitchContext failProtocolInitiator(ProtocolInitiator pi) {
		pInitiators.add(pi);
		return this;
	}
	
	/**
	 * Determines if this context has failed the passed {@link ProtocolInitiator} 
	 * @param pi the {@link ProtocolInitiator} to test
	 * @return true if failed, false otherwise
	 */
	boolean hasProtocolInitiatorFailed(ProtocolInitiator pi) {
		return pInitiators.contains(pi); 
	}
	
	

}
