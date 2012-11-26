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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * <p>Title: TimeSeriesModel</p>
 * <p>Description: Container and parser for the timeseries core structure and tier model.</p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.tsmodel.TimeSeriesModel</code></p>
 */

public class TimeSeriesModel {
	
	/** The timeseries tiers */
	protected final LinkedHashSet<Tier> tiers = new LinkedHashSet<Tier>();
	
	/**
	 * Creates a new TimeSeriesModel
	 * @param model
	 */
	private TimeSeriesModel(String model) {
		if(model==null) throw new IllegalArgumentException("The passed model was null", new Throwable());
		model = model.trim().toLowerCase().replace(" ", "");
		String[] frags = model.split("\\|");
		Tier previousTier = null;
		for(int i = 0; i < frags.length; i++) {
			Tier tier = new Tier(frags[i], i);			
			if(!  tiers.add(tier)) {				
				throw new InvalidTierModelException("Duplicate Tier [" + frags[i] + "] in tier model [" + model + "]");
			}			
			if(previousTier!=null) {
				if(tier.periodDuration.seconds%previousTier.periodDuration.seconds!=0) {
					throw new InvalidTierModelException("The period duration of tier [" + tier.name + "] is not an even multiple of the prior tier [" + previousTier.name + "]");
				}
			}
			previousTier = tier;
		}			
	}
	
	/**
	 * Returns the number of tiers
	 * @return the number of tiers
	 */
	public int getTierCount() {
		return tiers.size();
	}
	
	/**
	 * Returns a copy of the tier collection for this model
	 * @return a copy of the tier collection for this model
	 */
	public List<Tier> getModelTiers() {
		return new ArrayList<Tier>(tiers);
	}
	
	/**
	 * Returns a list of tier pairs where the second tier is the parent of the first, or null if the first has no parent
	 * @return a list of child/parent tier pairs 
	 */
	public List<Tier[]> getModelTierPairs() {
		ArrayList<Tier[]> pairs = new ArrayList<Tier[]>();
		Tier[] _tiers = tiers.toArray(new Tier[tiers.size()]);
		for(int i = 0; i < _tiers.length; i++) {
			Tier[] childParent = new Tier[2];
			childParent[0] = _tiers[i];
			childParent[1] = i==_tiers.length-1 ? null : _tiers[i+1];
			pairs.add(childParent);
		}
		return pairs;
	}
	
	/**
	 * Returns a two dimensional matrix of all the tiers in the model with values expressed in seconds.
	 * @return a two dimensional matrix of all the tiers in the model with values expressed in seconds.
	 */
	public long[][] getModelMatrix() {
		long[][] matrix = new long[tiers.size()][];
		int i = 0;
		for(Tier tier: tiers) {
			matrix[i] = new long[]{ tier.periodDuration.seconds, tier.tierDuration.seconds, tier.periodCount };
			i++;
		}
		return matrix;
	}
	
	/**
	 * Creates a new TimeSeriesModel from the passed stringified model
	 * @param model The string representation of the model
	 * @return a new TimeSeriesModel 
	 */
	public static TimeSeriesModel create(String model) {
		return new TimeSeriesModel(model);
	}
	
	
	public static void main(String[] args) {
//		log("Test TimeSeriesModel");		
//		String config = "p=15s,t=7d |  p=60s,t=23d | p=15m,t=355d";
//		TimeSeriesModel model1 = TimeSeriesModel.create(config);
//		log(model1);
//		TimeSeriesModel model2 = TimeSeriesModel.create(config);
//		log("Model1 equals Model2:" + model1.equals(model2));
//		log("Model2 equals Model1:" + model2.equals(model1));
//		config = "p=15s,t=7d |  p=60s,t=28d | p=15m,t=355d";
//		model2 = TimeSeriesModel.create(config);
//		log("Model1 equals Model2:" + model1.equals(model2));
//		for(long[] t : model2.getModelMatrix()) {
//			log(Arrays.toString(t));
//		}
		log("Test TimeSeriesModel");		
		//Logger.getLogger(Tier.class).setLevel(Level.DEBUG);
		String config = "p=5s,t=1m | p=1m,t=2m | p=5m,t=15m";
		TimeSeriesModel model1 = TimeSeriesModel.create(config);
		log(model1);
		for(Tier[] t: model1.getModelTierPairs()) {
			log(Arrays.toString(t));
		}
		
		
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder("Tier Model [");
		for(Tier tier: tiers) {
			b.append("\n\tLevel ").append(tier.level).append(":").append(tier);
		}
		b.append("\n]");
		return b.toString();
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((tiers == null) ? 0 : tiers.hashCode());
		return result;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		TimeSeriesModel other = (TimeSeriesModel) obj;
		if (tiers == null) {
			if (other.tiers != null) {
				return false;
			}
		} else if (!tiers.equals(other.tiers)) {
			return false;
		}
		return true;
	}
}
	
	
