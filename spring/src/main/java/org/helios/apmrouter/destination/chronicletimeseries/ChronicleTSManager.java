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
import java.util.concurrent.TimeUnit;

import org.helios.apmrouter.catalog.EntryStatus;
import org.helios.apmrouter.catalog.jdbc.h2.adapters.chronicle.ChronicleTSAdapter;
import org.helios.apmrouter.server.ServerComponentBean;
import org.helios.apmrouter.tsmodel.Tier;
import org.helios.apmrouter.tsmodel.TimeSeriesModel;
import org.springframework.jmx.export.annotation.ManagedAttribute;

/**
 * <p>Title: ChronicleTSManager</p>
 * <p>Description: Configures and manages the chronicle time-series</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.chronicletimeseries.ChronicleTSManager</code></p>
 * TODO:
 * ==============================================
 * Add status check scheduler
 * Add worker pool
 * Execute status checks across multiple threads and wait for completion
 * Invoke fireEvents asynchronously
 * Implement status changes in metric catalog
 * Add metrics to track elapsed time and number of state changes during status checks
 * Check for first/last periods in entries (which do we want ?)  Oldest should be first.....
 * Status check optimization:  oldest period in a tier should be in the tier header
 * ==============================================
 * TODO: Add support for rolling up into next tiers.
 * TODO: Add basic query functionality
 * TODO: Fill-Ins for sticky metrics ?  Physical or implied
 */

public class ChronicleTSManager extends ServerComponentBean {
	/** The time series model */
	private final TimeSeriesModel timeSeriesModel;
	/** A map of the time-series chronicle-tiers keyed by the tier name */
	private final Map<String, ChronicleTier> tiers;
	/** The live tier */
	private final ChronicleTier liveTier;
	/** The number of periods in the live tier that marks a metric stale */
	protected int stalePeriods = 4;
	/** The stale window length in ms. */
	protected long staleWindowSize = -1L;
	/** The number of periods in the live tier that marks a metric offline */
	protected int offLinePeriods = 20;
	/** The offLine window length in ms. */
	protected long offLineWindowSize = -1L;
	
	// default ts model = p=15s,t=5m
	
	/**
	 * Returns the number of periods in the live tier that marks a metric stale
	 * @return the stale period count
	 */
	@ManagedAttribute(description="The number of periods in the live tier that marks a metric stale")
	public int getStalePeriods() {
		return stalePeriods;
	}

	/**
	 * Sets the number of periods in the live tier that marks a metric stale
	 * @param stalePeriods the number periods in the live tier without activity to mark a metric stale 
	 */
	@ManagedAttribute(description="The number of periods in the live tier that marks a metric stale")
	public void setStalePeriods(int stalePeriods) {
		this.stalePeriods = stalePeriods;
		if(this.isStarted()) {
			recalcStaleWindowSize();
		}
	}


	/**
	 * Returns the number of periods in the live tier that marks a metric offline
	 * @return the offline period count
	 */
	@ManagedAttribute(description="The number of periods in the live tier that marks a metric offline")
	public int getOffLinePeriods() {
		return offLinePeriods;
	}

	/**
	 * Sets the number of periods in the live tier that marks a metric offline
	 * @param offLinePeriods the number periods in the live tier without activity to mark a metric offline 
	 */
	@ManagedAttribute(description="The number of periods in the live tier that marks a metric offline")
	public void setOffLinePeriods(int offLinePeriods) {
		this.offLinePeriods = offLinePeriods;
		if(this.isStarted()) {
			recalcStaleWindowSize();
		}		
	}

	
	/**
	 * Returns the calculated elapsed time of the stale window in ms.
	 * @return the calculated elapsed time of the stale window in ms.
	 */
	@ManagedAttribute(description="The calculated elapsed time of the stale window in ms.")
	public long getStaleWindowSize() {
		return staleWindowSize;
	}

	/**
	 * Returns the calculated elapsed time of the offline window in ms.
	 * @return the calculated elapsed time of the offline window in ms.
	 */
	@ManagedAttribute(description="The calculated elapsed time of the offline window in ms.")	
	public long getOffLineWindowSize() {
		return offLineWindowSize;
	}

	/**
	 * Returns the model definition the time series model was built with
	 * @return the model definition the time series model was built with
	 */
	@ManagedAttribute(description="The model definition the time series model was built with")
	public String getTimeSeriesModel() {
		return timeSeriesModel.getModelDef();
	}
	
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
		recalcStaleWindowSize();
		recalcOffLineWindowSize();
		Thread t = new Thread() {
			@Override
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
	 * Recalculates and sets the stale window size in ms,  
	 */
	protected void recalcStaleWindowSize() {
		staleWindowSize = TimeUnit.MILLISECONDS.convert(liveTier.getPeriodDuration() * stalePeriods, TimeUnit.SECONDS);
	}
	
	/**
	 * Recalculates and sets the offline window size in ms,  
	 */
	protected void recalcOffLineWindowSize() {
		offLineWindowSize = TimeUnit.MILLISECONDS.convert(liveTier.getPeriodDuration() * offLinePeriods, TimeUnit.SECONDS);
	}
	
	/**
	 * Fires a status change event to all registered listeners
	 * @param entryId The id of the entry that changed state
	 * @param timestamp The timestamp of the change in ms.
	 * @param priorState The prior state
	 * @param newState The new state
	 */
	protected void fireEventStatusChangeEvent(long entryId, long timestamp, EntryStatus priorState, EntryStatus newState) {
		
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
