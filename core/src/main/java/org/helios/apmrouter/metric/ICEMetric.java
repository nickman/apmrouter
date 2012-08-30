/**
 * ICE Futures, US
 */
package org.helios.apmrouter.metric;

import java.nio.ByteBuffer;


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
	protected long id;
	/** The timestamp of this metric */
	protected long time;
	/** The value of this metric */
	protected ByteBuffer value;
	
	
	/** The namespace delimiter */
	public static final String NSDELIM = "/";
	/** The value delimiter */
	public static final String VDELIM = "/";
	/** The mapped namespace pair delimiter */
	public static final String MDELIM = "=";

	
	
	

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
	 * Returns the metric timestamp
	 * @return the time
	 */
	public long getTime() {
		return time;
	}
	/**
	 * Returns the metric type
	 * @return the type
	 */
	public MetricType getType() {
		return type;
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
		
	}





}
