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
package org.helios.apmrouter.destination.chronicletimeseries;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.helios.apmrouter.catalog.jdbc.h2.adapters.chronicle.ChronicleTSAdapter;
import org.helios.apmrouter.server.ServerComponentBean;
import org.helios.apmrouter.tsmodel.Tier;
import org.helios.apmrouter.tsmodel.TimeSeriesModel;

/**
 * <p>Title: ChronicleTSManager</p>
 * <p>Description: Configures and manages the chronicle time-series</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.chronicletimeseries.ChronicleTSManager</code></p>
 */

public class ChronicleTSManager extends ServerComponentBean {
	/** The time series model */
	private final TimeSeriesModel timeSeriesModel;
	/** A map of the time-series chronicle-tiers keyed by the tier name */
	private final Map<String, ChronicleTier> tiers;
	/** The live tier */
	private final ChronicleTier liveTier;
	
	/**
	 * Creates a new ChronicleTSManager
	 * @param timeSeriesConfig The string representation of the time series configuration
	 */
	public ChronicleTSManager(String timeSeriesConfig) {
		timeSeriesModel = TimeSeriesModel.create(timeSeriesConfig);
		tiers = new HashMap<String, ChronicleTier>(timeSeriesModel.getTierCount());
		List<Tier[]> _tiers = timeSeriesModel.getModelTierPairs();
		for(int i = _tiers.size()-1; i >= 0; i--) {
			Tier[] tierPair = _tiers.get(i);
			ChronicleTier cTier = new ChronicleTier(tierPair[0], tierPair[1]==null ? null : getTier(tierPair[1].getName()), this);
			tiers.put(tierPair[0].getName(), cTier);			
		}
		liveTier = tiers.get("live");
		if(liveTier==null) throw new IllegalStateException("There was no live tier", new Throwable());
		Thread t = new Thread() {
			public void run() {
				info("\n\t========================\n\tCLOSING CTS\n\t========================\n");
				for(ChronicleTier ct : tiers.values()) {
					info("Closing [", ct.chronicleName, "]");
					ct.close();
				}
				info("\n\t========================\n\tCLOSED CTS\n\t========================\n");
			}
		};
		t.setDaemon(false);
		t.setPriority(Thread.MAX_PRIORITY);
		Runtime.getRuntime().addShutdownHook(t);
		ChronicleTSAdapter.setCts(this);
	}
	
	/**
	 * Returns the live tier 
	 * @return the live tier
	 */
	public ChronicleTier getLiveTier() {
		return liveTier;
	}
	
	/**
	 * Returns the named chronicle tier
	 * @param name The name of the tier to retrieve
	 * @return a chronicle tier
	 * @throws IllegalArgumentException thrown if the name is null, empty or does not map to a ChronicleTier
	 */
	public ChronicleTier getTier(String name) {
		if(name==null || name.trim().isEmpty()) throw new IllegalArgumentException("The passed tier name was null or empty", new Throwable());
		ChronicleTier ct = tiers.get(name.trim());
		if(ct==null) throw new IllegalArgumentException("The passed tier name [" + name + "] was invalid", new Throwable());
		return ct;
	}
	

}
