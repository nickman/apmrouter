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
package org.helios.apmrouter.catalog.domain;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

import com.google.gson.annotations.Expose;




/**
 * <p>Title: AgentMetricSet</p>
 * <p>Description: An optimization to retrieve a more structured JSON representation of the metrics for a specified agent</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.catalog.domain.AgentMetricSet</code></p>
 */

public class AgentMetricSet {
	/** Static class logger */
	protected static final Logger LOG = Logger.getLogger(AgentMetricSet.class);
	/** The root level folders */
	protected final Map<String, Folder> folders = new LinkedHashMap<String, Folder>();
	/** The root level metrics */
	protected final Set<Map<String, Map<String, Object>>> metrics = new LinkedHashSet<Map<String, Map<String, Object>>>();

	/** The attribute property key for the ID */
	public static final String FOLDER_TAG_ID = "id";
	/** The attribute property key for the folder name */
	public static final String FOLDER_TAG_FOLDER_NAME = "folder_name";
	/** The attribute property key for the rel (metric tree node type)  */
	public static final String FOLDER_TAG_REL = "rel";
	/** The attribute property key for the agent Id  */
	public static final String FOLDER_TAG_AGENT_ID = "agentid";
	/** The data property key for the title  */
	public static final String FOLDER_TAG_TITLE = "title";
	
	
	/**
	 * Creates a new AgentMetricSet
	 * @param session The hibernate session to query the metric tree
	 * @param agentId The agent ID of the agent to get the metric tree for
	 * @return an AgentMetricSet
	 */
	public static AgentMetricSet newInstance(Session session, int agentId) {
		LOG.info("Fetching AMS for agent [" + agentId + "]");
		Query query = session.getNamedQuery("allMetricsForAgent");
		query.setInteger("agentId", agentId);
		@SuppressWarnings("unchecked")
		List<Metric> metrics = query.list();
		return new AgentMetricSet(metrics);
	}
	
	/**
	 * Creates an indent of tabs
	 * @param level The number of tabs
	 * @return a char array of tabs
	 */
	public static char[] indent(int level) {
		char[] ind = new char[level];
		Arrays.fill(ind, '\t');
		return ind;
	}
	
	private AgentMetricSet(List<Metric> metrics) {
		LOG.info("Building AMS with [" + metrics.size() + "] Metrics");
		for(Metric metric: metrics) {
			String[] narr = metric.getNarr();
			int cnt = narr.length;
			if(cnt==0) {
				this.metrics.add(mapMetric(metric));  // zero level metric (no namespace)
			} else {
				Folder folder = folders.get(narr[0]);
				if(folder==null) {
					LOG.info(new StringBuilder("FOLDER ").append(narr[0]));
					folders.put(narr[0], new Folder(metric));										
				} else {
					folder.processMetric(metric);
				}				
			}
		}
	}
	
	/**
	 * Generates a metric tree friendly representation of a metric
	 * @param metric The metric to render
	 * @return the map of maps representing the metric
	 */
	protected static Map<String, Map<String, Object>> mapMetric(Metric metric) {
		Map<String, Map<String, Object>> map = new HashMap<String, Map<String, Object>>(2);
		Map<String, Object> attr = new HashMap<String, Object>();
		Map<String, Object> data = new HashMap<String, Object>();		
		attr.put(FOLDER_TAG_ID, "metric-" + metric.getMetricId());
		attr.put(FOLDER_TAG_FOLDER_NAME, metric.getName());
		attr.put(FOLDER_TAG_REL, "metric");		
		data.put(FOLDER_TAG_TITLE, metric.getName());
		map.put("attr", attr);
		map.put("data", data);
		return map;
	}
	
	
	/**
	 * <p>Title: Folder</p>
	 * <p>Description: Represents a notional folder within which a metric exists</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.catalog.domain.AgentMetricSet.Folder</code></p>
	 */
	public static class Folder {
		/** The host Id */
		@Expose(serialize=false)
		private final int hostId;
		/** The agent Id */
		@Expose(serialize=false)
		private final int agentId;
		/** Indicates if this folder has metrics directly in it */		
		@Expose(serialize=false)
		private boolean hasMetrics = false;
		/** The folders directly in this folder */
		@Expose(serialize=false)
		protected final Map<String, Folder> folders = new LinkedHashMap<String, Folder>();
		/** The metrics directly in this folder */
		@Expose(serialize=false)
		protected final Set<Map<String, Map<String, Object>>> metrics = new LinkedHashSet<Map<String, Map<String, Object>>>();
		/** Attribute properties */
		protected final Map<String, Object> attr = new LinkedHashMap<String, Object>();
		/** Data properties */
		protected final Map<String, Object> data = new LinkedHashMap<String, Object>();

		/*
		attr : {
			id: "folder-" + (metric[0].replace('/', '').replace('=', '-')),
			folder_name : metric[0],
			type : metric[1],
			rel: "folder",
			agentid : $(me).attr('agentid')
		},  
		data : {title: metric[0], id : $(me).attr('id') + metric[0]}
		
		public static final String FOLDER_TAG_ID = "id";
		public static final String FOLDER_TAG_FOLDER_NAME = "folder_name";
		public static final String FOLDER_TAG_REL = "rel";
		public static final String FOLDER_TAG_AGENT_ID = "agentid";
		public static final String FOLDER_TAG_TITLE = "title";
	 */
		
		
		
		
		
		/**
		 * Creates a new root Folder and hierarchy from a metric
		 * @param metric The metric to create the folder tree from
		 */
		public Folder(Metric metric) {
			hostId = metric.getAgent().getHost().getHostId();
			agentId = metric.getAgent().getAgentId();
			attr.put(FOLDER_TAG_ID, String.format("host-%s-agent-%s-%s", hostId, agentId, metric.getNarr()[0].replace("/", "").replace('=', '_')));
			attr.put(FOLDER_TAG_FOLDER_NAME, metric.getNarr()[0]);
			attr.put(FOLDER_TAG_REL, "folder");
			attr.put(FOLDER_TAG_AGENT_ID, agentId);
			data.put(FOLDER_TAG_TITLE, metric.getNarr()[0].replace("/", ""));
			processMetric(metric);
		}
		
		/**
		 * Processes the full hierarchy of the passed metric within this folder
		 * @param metric The metric to process
		 */
		protected void processMetric(Metric metric) {
			String[] narr = metric.getNarr();
			final int cnt = narr.length;
			Folder currentFolder = this;
			StringBuilder fNode = new StringBuilder(narr[0]);
			for(int i = 1; i < cnt; i++) {
				fNode.append("-").append(narr[i]);
				Folder newFolder = currentFolder.folders.get(narr[i]);
				if(newFolder==null) {						
						newFolder = new Folder(hostId, agentId, narr[i], fNode.toString());
						currentFolder.folders.put(narr[i], newFolder);
						LOG.info(new StringBuilder().append(indent(i)).append("FOLDER ").append(narr[i]));
				}				
				currentFolder = newFolder;
			}
			LOG.info(new StringBuilder().append(indent(cnt)).append("METRIC ").append(metric.getName()));
			currentFolder.metrics.add(mapMetric(metric));
			currentFolder.hasMetrics = true;
		}
		

		/**
		 * Creates a new Folder
		 * @param hostId The host ID
		 * @param agentId The agent ID
		 * @param localNode The local node name
		 * @param fullNode The cummulative fully qualified node name
		 */
		private Folder(int hostId, int agentId, String localNode, String fullNode) {			
			this.hostId = hostId;
			this.agentId = agentId;
			attr.put(FOLDER_TAG_ID, String.format("host-%s-agent-%s-%s", hostId, agentId, localNode.replace("/", "").replace('=', '_')));
			attr.put(FOLDER_TAG_FOLDER_NAME, localNode);
			attr.put(FOLDER_TAG_REL, "folder");
			attr.put(FOLDER_TAG_AGENT_ID, agentId);
			data.put(FOLDER_TAG_TITLE, localNode.replace("/", ""));
			
		}


		/**
		 * Indicates if this folder has metrics directly in it
		 * @return true if this folder has metrics directly in it, false otherwise
		 */
		public boolean isHasMetrics() {
			return hasMetrics;
		}


		/**
		 * Returns the host id 
		 * @return the hostId
		 */
		public int getHostId() {
			return hostId;
		}

		/**
		 * Returns the agent id
		 * @return the agentId
		 */
		public int getAgentId() {
			return agentId;
		}

	}
}
