/**
 * ICE Futures, US
 */
package org.helios.apmrouter.metric;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.helios.apmrouter.util.StringHelper;
import org.helios.apmrouter.util.SystemClock;

import static org.helios.apmrouter.util.Methods.nvl;


/**
 * <p>Title: ICEMetric</p>
 * <p>Description: Base generic metric</p> 
 * <p>Company: ICE Futures US</p>
 * @author Whitehead (nicholas.whitehead@theice.com)
 * @version $LastChangedRevision$
 * <p><code>org.helios.apmrouter.metric.ICEMetric</code></p>
 */

public class ICEMetric {
	// ===================================================================
	// 	These attributes are FINAL.
	// ===================================================================
	/** The host name */
	protected final String host;
	/** The agent name */
	protected final String agent;
	/** The metric namespace */
	protected final String[] namespace;
	/** The metric name */
	protected final String name;
	/** The metric type */
	protected final MetricType type;
	/** Indicates if this is a flat or mapped metric name */
	protected final boolean flat;
	
	
	// ===================================================================
	// 	These attributes can be modified through the UPDATER.
	// ===================================================================

	/** The token substitution ID of the metric */
	protected long id = -1;
	/** The value for this metric */
	protected ICEMetricValue value;
	
	// ===================================================================
	// 	Contants
	// ===================================================================
	
	/** The namespace delimiter */
	public static final String NSDELIM = "/";
	/** The name delimiter */
	public static final String NADELIM = ":";
	/** The timestamp start delimiter */
	public static final String TS_S_DELIM = "[";
	/** The timestamp end delimiter */
	public static final String TS_E_DELIM = "]";
	/** The value delimiter */
	public static final String VDELIM = "/";
	/** The mapped namespace pair delimiter */
	public static final String MDELIM = "=";

	/** The format for rendering a transmittable metric */
	public static final String TX_FORMAT = TS_S_DELIM + "%s" + TS_E_DELIM + "%s" + NSDELIM + "%s%s" + VDELIM + "%s" ;
	/** The format for rendering the fully qualified metric name */
	public static final String FQN_FORMAT = "%s" + NSDELIM + "%s%s" + NADELIM + "%s" ;


	
	
	

	/**
	 * Creates a new ICEMetric
	 * @param type The metric type
	 * @param name The metric name
	 * @param flat Indicates if the metric namespace is flat or mapped
	 * @param namespace The namespace
	 */
	private ICEMetric(MetricType type, String name, boolean flat, String... namespace) {
		this.name = name;
		this.host = AgentIdentity.ID.getHostName();
		this.agent = AgentIdentity.ID.getAgentName();
		this.namespace = namespace;		
		this.type = type;
		this.flat = flat;
	}
	
	
	
	/**
	 * Returns the host name that this metric originated from
	 * @return the host name that this metric originated from
	 */
	public String getHost() {
		return host;
	}

	/**
	 * The name of the agent that this metric originated from
	 * @return the name of the agent that this metric originated from
	 */
	public String getAgent() {
		return agent;
	}
	
	/**
	 * Indicates if the metric namespace is flat or mapped
	 * @return true if the metric namespace is flat, false if it is mapped
	 */
	public boolean isFlat() {
		return flat;
	}
	
	/**
	 * Indicates if the metric namespace is flat or mapped
	 * @return true if the metric namespace is mapped, false if it is flat
	 */
	public boolean isMapped() {
		return !flat;
	}
	

	/**
	 * The namespace of the metric
	 * @return the namespace
	 */
	public String[] getNamespace() {
		return namespace;
	}
	
	/**
	 * Returns the metric name
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Returns the metric timestamp or -1 if no timestamp has been set
	 * @return the time
	 */
	public long getTime() {
		return value==null ? -1L : value.time;
	}
	
	/**
	 * Returns the metric type
	 * @return the type
	 */
	public MetricType getType() {
		return type;
	}

	
	/**
	 * Returns the fully qualified metric name
	 * @return the fully qualified metric name
	 */
	public String getFQN() {
		return String.format(FQN_FORMAT, host, agent, getNamespaceF(), name );
	}
	
	/**
	 * Returns the concatenated namespace
	 * @return the concatenated namespace
	 */
	public String getNamespaceF() {
		if(namespace.length==0) return "";
		return StringHelper.fastConcatAndDelim(NSDELIM, namespace);
	}
	
	/**
	 * Returns the namespace element at the provided index
	 * @param index The namespace index
	 * @return a namespace element
	 */
	public String getNamespace(int index) {
		return namespace[index];
	}
	
	/**
	 * Returns the number of elements in the namespace
	 * @return the number of elements in the namespace
	 */
	public int getNameSpaceSize() {
		return namespace.length;
	}
	
	/**
	 * Creates a new initial ICEMetricBuilder
	 * @param name The metric name
	 * @param type The metric type
	 * @param namespace An optional array of namespace entries
	 * @return an ICEMetricBuilder
	 * @throws RuntimeException Thrown if any of the initial metric parameters are invalid
	 */
	public static ICEMetricBuilder builder(CharSequence name, MetricType type, String...namespace) {
		try {
			return new ICEMetricBuilder(AgentIdentity.ID.getHostName(), AgentIdentity.ID.getAgentName(), nvl(name, "Metric Name").toString(), type, namespace);
		} catch (Exception e) {
			throw new RuntimeException("Failed to create ICEMetricBuilder", e);
		}
	}
	
	/**
	 * Creates a new initial ICEMetricBuilder
	 * @param name The metric name
	 * @param type The metric type name
	 * @param namespace An optional array of namespace entries
	 * @return an ICEMetricBuilder
	 * @throws RuntimeException Thrown if any of the initial metric parameters are invalid
	 */
	public static ICEMetricBuilder builder(CharSequence name, CharSequence type, String...namespace) {
		try {
			return new ICEMetricBuilder(AgentIdentity.ID.getHostName(), AgentIdentity.ID.getAgentName(), nvl(name, "Metric Name").toString(), MetricType.valueOfName(type), namespace);
		} catch (Exception e) {
			throw new RuntimeException("Failed to create ICEMetricBuilder", e);
		}
	}
	
	/**
	 * Creates a new initial ICEMetricBuilder
	 * @param name The metric name
	 * @param type The metric type ordinal
	 * @param namespace An optional array of namespace entries
	 * @return an ICEMetricBuilder
	 * @throws RuntimeException Thrown if any of the initial metric parameters are invalid
	 */
	public static ICEMetricBuilder builder(CharSequence name, int type, String...namespace) {
		try {
			return new ICEMetricBuilder(AgentIdentity.ID.getHostName(), AgentIdentity.ID.getAgentName(), nvl(name, "Metric Name").toString(), MetricType.valueOf(type), namespace);
		} catch (Exception e) {
			throw new RuntimeException("Failed to create ICEMetricBuilder", e);
		}
	}
	
	

	
	/**
	 * <p>Title: ICEMetricBuilder</p>
	 * <p>Description: A builder factory for ICEMetrics</p> 
	 * <p>Company: ICE Futures US</p>
	 * @author Whitehead (nicholas.whitehead@theice.com)
	 * @version $LastChangedRevision$
	 * <p><code>org.helios.apmrouter.metric.ICEMetric.ICEMetricBuilder</code></p>
	 */
	public static class ICEMetricBuilder {
		/** The host name */
		protected final String host;
		/** The agent name */
		protected final String agent;
		/** The metric namespace */
		protected final List<String> namespace = new ArrayList<String>();
		/** The metric name */
		protected final String name;
		/** The metric type */
		protected final MetricType type;
		/** Indicates if this is a flat or mapped metric name */
		protected Boolean flat = null;
		
		/**
		 * Creates a new initial ICEMetricBuilder
		 * @param host The host name
		 * @param agent The agent name
		 * @param name The metric name
		 * @param type The metric type
		 * @param namespace An optional array of namespace entries
		 * @throws Exception Thrown if any of the initial metric parameters are invalid
		 */
		private ICEMetricBuilder(String host, String agent, String name, MetricType type, String...namespace) throws Exception {
			this.host = nvl(host, "Host Name");
			this.agent = nvl(agent, "Agent Name");
			this.name = nvl(name, "Metric Name");
			this.type = nvl(type, "Metric Type");
			if(namespace!=null && namespace.length>0) {
				for(String ns: namespace) {
					addNamespace(ns);
				}
			}
		}
		
		/**
		 * Adds a namespace to the metric
		 * Throws a runtime exception if any of the namespaces are invalid
		 * @param namespace An array of namespaces to add
		 * @return this builder
		 */
		public ICEMetricBuilder ns(String...namespace) {
			try {
				if(namespace!=null && namespace.length>0) {
					for(String ns: namespace) {
						addNamespace(ns);
					}
				}
				return this;
			} catch (Exception e) {
				throw new RuntimeException("Namespace element was invalid", e);
			}
		}
		
		/**
		 * Adds a new mapped namespace
		 * Throws a runtime exception if the namespaces are invalid
		 * @param key The mapped namespace key
		 * @param value The mapped namespace key
		 * @return this builder
		 */
		public ICEMetricBuilder mns(String key, String value) {
			try {
				nvl(key, "Namespace Key");
				nvl(value, "Namespace Value");
				addNamespace(new StringBuilder(key).append(MDELIM).append(value).toString());
				return this;
			} catch (Exception e) {
				throw new RuntimeException("Mapped Namespace element was invalid", e);
			}			
		}
		
		/**
		 * Processes a namespace addition.
		 * If the passed ns is null or empty, it is ignored.
		 * If the ns is flat, but the builder has already marked the metric as mapped, the entry will be merged by removing the MDELIM.
		 * If the ns is mapped, but the builder has already marked the metric as flat, an exception is thrown.
		 * @param ns The namespace to add
		 * @throws Exception Thrown if the namespace is not null or empty, but is somehow invalid
		 */
		private void addNamespace(String ns) throws Exception {
			if(ns==null) return;
			ns = ns.trim();
			if(ns.isEmpty()) return;
			if(ns.indexOf(" ")!=-1) ns = ns.replace(" ", "");
			int mindex = ns.indexOf(MDELIM);
			boolean _flat = mindex==-1;
			if(flat!=null) {
				// The metric flattness has already been assigned
				if(_flat != flat) {
					// This namespace entry has a different flatness from the metric
					if(flat) {
						// Merge mapped to flat
						ns = merge(ns, mindex);
					} else {
						// Exception: Metric is mapped but namespace entry was flat
						throw new Exception("The supplied namespace [" + ns + "] is flat but being added to a metric name which is mapped " + namespace.toString(), new Throwable());
					}
				}
			} else {
				// Flatness of the metric was not already set, so set it
				flat = _flat;				
			}
			// Validate the key/value pair if the type is mapped
			if(!_flat) validateMapped(ns, mindex);
			// Add the namespace
			namespace.add(ns);
		}
		
		/**
		 * Merges a mapped namespace entry into a flat namespace.
		 * If either the key or the value are empty, throws an exceptiom
		 * @param ns The namespace to merge
		 * @param index The index of the MDELIM
		 * @return the merged namespace
		 */
		private String merge(String ns, int index) {			
			return new StringBuilder(ns.substring(0, index)).append(ns.substring(index-1)).toString();
		}

		/**
		 * Validates a mapped namespace entry 
		 * If either the key or the value are empty, throws an exceptiom
		 * @param ns The namespace to validate
		 * @param index The index of the MDELIM
		 * @throws Exception  Thrown if the key or the value is empty
		 */
		private void validateMapped(String ns, int index) throws Exception {
			if(ns.substring(0, index).isEmpty() || ns.substring(index-1).isEmpty()) {
				throw new Exception("Mapped namespace entry [" + ns + "] had empty key or value", new Throwable());			}
		}
		
		
//		String a = "foo=bar";
//		int index = a.indexOf("=");
//		a1 = a.substring(0, index);
//		a2 = a.substring(index+1);
//		println "[${a1}]/[${a2}]";
		
		
	}





}
