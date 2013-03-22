/**
 * ICE Futures, US
 */
package org.helios.apmrouter.metric.catalog.heap;

import org.helios.apmrouter.metric.AgentIdentity;
import org.helios.apmrouter.metric.ICEMetricValue;
import org.helios.apmrouter.metric.MetricType;
import org.helios.apmrouter.util.StringHelper;

import java.util.ArrayList;
import java.util.List;

import static org.helios.apmrouter.util.Methods.nvl;


/**
 * <p>Title: CoreICEMetric</p>
 * <p>Description: Core generic metricId. One instance exists of this class per metricId name.</p> 
 * <p>Company: ICE Futures US</p>
 * @author Whitehead (nicholas.whitehead@theice.com)
 * @version $LastChangedRevision$
 * <p><code>org.helios.apmrouter.metric.CoreICEMetric</code></p>
 */

class CoreICEMetric {
	// ===================================================================
	// 	These attributes are FINAL.
	// ===================================================================
	/** The host name */
	protected final String host;
	/** The agent name */
	protected final String agent;
	/** The metricId namespace */
	protected final String[] namespace;
	/** The metricId name */
	protected final String name;
	/** The metricId type */
	protected final MetricType type;
	/** Indicates if this is a flat or mapped metricId name */
	protected final boolean flat;
	
	
	// ===================================================================
	// 	These attributes can be modified through the UPDATER.
	// ===================================================================

	/** The token substitution ID of the metricId */
	protected long id = -1;
	/** The value for this metricId */
	protected ICEMetricValue value;
	
	// ===================================================================
	// 	Contants
	// ===================================================================
	
	/** The namespace delimiter */
	static final String NSDELIM = "/";
	/** The name delimiter */
	static final String NADELIM = ":";
	/** The timestamp start delimiter */
	static final String TS_S_DELIM = "[";
	/** The timestamp end delimiter */
	static final String TS_E_DELIM = "]";
	/** The value delimiter */
	static final String VDELIM = "/";
	/** The mapped namespace pair delimiter */
	static final String MDELIM = "=";

	/** The format for rendering a transmittable metricId */
	static final String TX_FORMAT = TS_S_DELIM + "%s" + TS_E_DELIM + "%s" + NSDELIM + "%s%s" + VDELIM + "%s" ;
	/** The format for rendering the fully qualified metricId name */
	static final String FQN_FORMAT = "%s" + NSDELIM + "%s%s" + NADELIM + "%s" ;


	
	
	

	/**
	 * Creates a new CoreICEMetric
	 * @param type The metricId type
	 * @param name The metricId name
	 * @param flat Indicates if the metricId namespace is flat or mapped
	 * @param namespace The namespace
	 */
	CoreICEMetric(MetricType type, String name, boolean flat, String... namespace) {
		this.name = name;
		this.host = AgentIdentity.ID.getHostName();
		this.agent = AgentIdentity.ID.getAgentName();
		this.namespace = namespace;		
		this.type = type;
		this.flat = flat;
	}
	
	
	
	/**
	 * Returns the host name that this metricId originated from
	 * @return the host name that this metricId originated from
	 */
	String getHost() {
		return host;
	}

	/**
	 * The name of the agent that this metricId originated from
	 * @return the name of the agent that this metricId originated from
	 */
	String getAgent() {
		return agent;
	}
	
	/**
	 * Indicates if the metricId namespace is flat or mapped
	 * @return true if the metricId namespace is flat, false if it is mapped
	 */
	boolean isFlat() {
		return flat;
	}
	
	/**
	 * Indicates if the metricId namespace is flat or mapped
	 * @return true if the metricId namespace is mapped, false if it is flat
	 */
	boolean isMapped() {
		return !flat;
	}
	

	/**
	 * The namespace of the metricId
	 * @return the namespace
	 */
	String[] getNamespace() {
		return namespace;
	}
	
	/**
	 * Returns the metricId name
	 * @return the name
	 */
	String getName() {
		return name;
	}
	
	/**
	 * Returns the metricId timestamp or -1 if no timestamp has been set
	 * @return the time
	 */
	long getTime() {
		return value==null ? -1L : value.getTime();
	}
	
	/**
	 * Returns the metricId type
	 * @return the type
	 */
	MetricType getType() {
		return type;
	}

	
	/**
	 * Returns the fully qualified metricId name
	 * @return the fully qualified metricId name
	 */
	String getFQN() {
		return String.format(FQN_FORMAT, host, agent, getNamespaceF(), name );
	}
	
	/**
	 * Returns the concatenated namespace
	 * @return the concatenated namespace
	 */
	String getNamespaceF() {
		if(namespace.length==0) return "";
		return StringHelper.fastConcatAndDelim(NSDELIM, namespace);
	}
	
	/**
	 * Returns the namespace element at the provided index
	 * @param index The namespace index
	 * @return a namespace element
	 */
	String getNamespace(int index) {
		return namespace[index];
	}
	
	/**
	 * Returns the number of elements in the namespace
	 * @return the number of elements in the namespace
	 */
	int getNameSpaceSize() {
		return namespace.length;
	}
	
	/**
	 * Creates a new initial ICEMetricBuilder
	 * @param name The metricId name
	 * @param type The metricId type
	 * @param namespace An optional array of namespace entries
	 * @return an ICEMetricBuilder
	 * @throws RuntimeException Thrown if any of the initial metricId parameters are invalid
	 */
	static ICEMetricBuilder builder(CharSequence name, MetricType type, String...namespace) {
		try {
			return new ICEMetricBuilder(AgentIdentity.ID.getHostName(), AgentIdentity.ID.getAgentName(), nvl(name, "IMetric Name").toString(), type, namespace);
		} catch (Exception e) {
			throw new RuntimeException("Failed to create ICEMetricBuilder", e);
		}
	}
	
	/**
	 * Creates a new initial ICEMetricBuilder
	 * @param name The metricId name
	 * @param type The metricId type name
	 * @param namespace An optional array of namespace entries
	 * @return an ICEMetricBuilder
	 * @throws RuntimeException Thrown if any of the initial metricId parameters are invalid
	 */
	static ICEMetricBuilder builder(CharSequence name, CharSequence type, String...namespace) {
		try {
			return new ICEMetricBuilder(AgentIdentity.ID.getHostName(), AgentIdentity.ID.getAgentName(), nvl(name, "IMetric Name").toString(), MetricType.valueOfName(type), namespace);
		} catch (Exception e) {
			throw new RuntimeException("Failed to create ICEMetricBuilder", e);
		}
	}
	
	/**
	 * Creates a new initial ICEMetricBuilder
	 * @param name The metricId name
	 * @param type The metricId type ordinal
	 * @param namespace An optional array of namespace entries
	 * @return an ICEMetricBuilder
	 * @throws RuntimeException Thrown if any of the initial metricId parameters are invalid
	 */
	static ICEMetricBuilder builder(CharSequence name, int type, String...namespace) {
		try {
			return new ICEMetricBuilder(AgentIdentity.ID.getHostName(), AgentIdentity.ID.getAgentName(), nvl(name, "IMetric Name").toString(), MetricType.valueOf(type), namespace);
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
	 * <p><code>org.helios.apmrouter.metric.CoreICEMetric.ICEMetricBuilder</code></p>
	 */
	static class ICEMetricBuilder {
		/** The host name */
		protected final String host;
		/** The agent name */
		protected final String agent;
		/** The metricId namespace */
		protected final List<String> namespace = new ArrayList<String>();
		/** The metricId name */
		protected final String name;
		/** The metricId type */
		protected final MetricType type;
		/** Indicates if this is a flat or mapped metricId name */
		protected Boolean flat = null;
		
		/**
		 * Creates a new initial ICEMetricBuilder
		 * @param host The host name
		 * @param agent The agent name
		 * @param name The metricId name
		 * @param type The metricId type
		 * @param namespace An optional array of namespace entries
		 * @throws Exception Thrown if any of the initial metricId parameters are invalid
		 */
		ICEMetricBuilder(String host, String agent, String name, MetricType type, String...namespace) throws Exception {
			this.host = nvl(host, "Host Name");
			this.agent = nvl(agent, "Agent Name");
			this.name = nvl(name, "IMetric Name");
			this.type = nvl(type, "IMetric Type");
			if(namespace!=null && namespace.length>0) {
				for(String ns: namespace) {
					addNamespace(ns);
				}
			}
		}
		
		/**
		 * Builds the CoreICEMetric
		 * @return the CoreICEMetric
		 */
		CoreICEMetric build() {			
			return new CoreICEMetric(type, name, flat==null ? true : flat, namespace.toArray(new String[namespace.size()]));
		}
		
		/**
		 * Adds a namespace to the metricId
		 * Throws a runtime exception if any of the namespaces are invalid
		 * @param namespace An array of namespaces to add
		 * @return this builder
		 */
		ICEMetricBuilder ns(String...namespace) {
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
		ICEMetricBuilder mns(String key, String value) {
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
		 * If the ns is flat, but the builder has already marked the metricId as mapped, the entry will be merged by removing the MDELIM.
		 * If the ns is mapped, but the builder has already marked the metricId as flat, an exception is thrown.
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
				// The metricId flattness has already been assigned
				if(_flat != flat) {
					// This namespace entry has a different flatness from the metricId
					if(flat) {
						// Merge mapped to flat
						ns = merge(ns, mindex);
					} else {
						// Exception: IMetric is mapped but namespace entry was flat
						throw new Exception("The supplied namespace [" + ns + "] is flat but being added to a metricId name which is mapped " + namespace.toString(), new Throwable());
					}
				}
			} else {
				// Flatness of the metricId was not already set, so set it
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
