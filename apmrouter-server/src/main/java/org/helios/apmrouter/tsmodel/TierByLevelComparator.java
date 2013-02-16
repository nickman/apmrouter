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
package org.helios.apmrouter.tsmodel;

import java.util.Comparator;

/**
 * <p>Title: TierByLevelComparator</p>
 * <p>Description: A comparator to sort {@link Tier}s by level </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.tsmodel.TierByLevelComparator</code></p>
 */

public class TierByLevelComparator implements Comparator<Tier> {
	
	/**
	 * Creates a new TierByLevelComparator, private since only 1 instance is needed
	 */
	private TierByLevelComparator() {}
	
	/** The shared comparator instance */
	public static final TierByLevelComparator INSTANCE = new TierByLevelComparator();
	
	/**
	 * {@inheritDoc}
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(Tier tier1, Tier tier2) {
		if(tier1.level==tier2.level) return 0;
		return tier1.level<tier2.level ? -1 : 0;
	}

}
