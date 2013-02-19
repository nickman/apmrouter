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
package org.helios.collector.jdbc.mapping;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.MBeanServer;

//import net.sf.ehcache.Ehcache;

import org.apache.log4j.Logger;
import org.helios.collector.jdbc.binding.provider.BindVariableProviderFactory;
import org.helios.collector.jdbc.binding.provider.IBindVariableProvider;
import org.helios.collector.jdbc.extract.IReadOnlyProcessedResultSet;
import org.helios.apmrouter.jmx.XMLHelper;
//- import org.helios.ot.deltas.DeltaManager;
import org.helios.apmrouter.metric.ICEMetric;
//- import org.helios.ot.trace.Trace.Builder;
import org.helios.apmrouter.trace.ITracer;
import org.helios.apmrouter.metric.MetricType;
import org.w3c.dom.Node;

/**
 * <p>Title: MetricMap</p>
 * <p>Description: Configured instances of this class acept the results of a SQLMap's query and generate OpenTrace metrics from each row returned. </p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class MetricMap {
	/** The column from which the value should be extracted  */
	protected String valueCol = null;
	/** The metric segment expression */
	protected String segment = null;
	/** The metric name expression */
	protected String name = null;
	/** The metric type expression */
	protected String type = null;
	/** The optional scope value expression */
	protected String scope = null;
	/** flag to indicate if metric should be temporal */
	protected boolean temporal = false;
	/** flag to indicate that columns from multiple rows should be flatened out into one string */
	protected boolean flatten = false;
	/** The collector provided metric name root prefix */
	protected String[] prefix = {};
	/** The collector provided tracer */
	protected ITracer tracer = null;
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass());
	/** The default expression delimeter */
	public static final  String DEFAULT_DELIMETER = "|";
	/** The expression delimeter */
	public String delimeter = DEFAULT_DELIMETER ;
	/** The metric name formatter */
	protected Formatter nameFormatter = null;
	/** The metric value formatter */
	protected Formatter valueFormatter = null;
	/** The metric segment formatter */
	protected Formatter segmentFormatter = null;
	/** The metric type formatter */
	protected Formatter metricTypeFormatter = null;
	/** The scope formatter */
	protected volatile Formatter scopeFormatter = null;
	/** The bind formatter */
	protected volatile Formatter bindFormatter = null;
	/** Flag to indicate if tracing is enabled (if not it is probably a pre) */
	protected boolean tracerEnabled = true;	
	/** A reference to the MBeanServer where the collector is registered */
	protected MBeanServer server = null;
//	/** A reference to the collector's collector cache */
//	protected Ehcache collectorCache = null;
	
	/** Result binding expression */
	protected String resultBind = null;
	/** Result binding binder expression */
	protected String binder = null;
	/** the provider for the result binding */
	protected IBindVariableProvider provider = null;
	/** flag indicating if a binder is enabled */
	protected boolean binderEnabled = false;
	
	/** The scope map */
	protected Map<String, ScopeState> scopeMap = new HashMap<String, ScopeState>();
	
	/**
	 * Parameterless constructor.
	 */
	public MetricMap() {
		
	}
	
	/**
	 * XML node based constructor.
	 * @param rootNode The root configuration XML node for this mapping.
	 */
	public MetricMap(Node node) throws InvalidMetricMappingException {
		valueCol = XMLHelper.getAttributeValueByName(node, "value");
		segment = XMLHelper.getAttributeValueByName(node, "segment");
		name = XMLHelper.getAttributeValueByName(node, "name");
		type = XMLHelper.getAttributeValueByName(node, "type");
		scope = XMLHelper.getAttributeValueByName(node, "scope");
		String temporalStr = XMLHelper.getAttributeValueByName(node, "temporal");
		temporal = "true".equalsIgnoreCase(temporalStr);
		String flattenStr = XMLHelper.getAttributeValueByName(node, "flatten");
		flatten = "true".equalsIgnoreCase(flattenStr);
		resultBind = XMLHelper.getAttributeValueByName(node, "bindresult");
		binder = XMLHelper.getAttributeValueByName(node, "provider");
	}
	
	/**
	 * Initializes the metric map.
	 * @param prefix The collector provided metric name prefix.
	 * @param tracer The collector provided  HOT tracer
	 * @param collectorCache A reference to the collector's collector cache. 
	 * @param server The collector provided MBeanServer.
	 * @throws InvalidMetricMappingException
	 */
	public void init(String[] prefix, ITracer tracer, /*Ehcache collectorCache,*/ MBeanServer server) throws InvalidMetricMappingException {
		this.prefix = prefix;
		this.tracer = tracer;
		this.server = server;
		//this.collectorCache = collectorCache;
		if(name==null||valueCol==null|segment==null||type==null) {
			
		}
		try { nameFormatter = new Formatter(delimeter, name); 	} catch (Exception e) {throw new InvalidMetricMappingException("Failed to initialize MetricMapping [Name]", e);	}
		try { valueFormatter = new Formatter(delimeter, valueCol); 	} catch (Exception e) {throw new InvalidMetricMappingException("Failed to initialize MetricMapping [Value]", e);	}
		try { segmentFormatter = new Formatter(delimeter, segment); } catch (Exception e) {throw new InvalidMetricMappingException("Failed to initialize MetricMapping [Segment]", e);	}
		try { metricTypeFormatter = new Formatter(delimeter, type); 	} catch (Exception e) {throw new InvalidMetricMappingException("Failed to initialize MetricMapping [MetricType]", e);	}
		if(scope!=null) {
			try { scopeFormatter = new Formatter(delimeter, scope); 	} catch (Exception e) {throw new InvalidMetricMappingException("Failed to initialize MetricMapping [Scope]", e);	}
		}
		if(resultBind != null) {
			try {
				bindFormatter = new Formatter(delimeter, resultBind);
			} catch (Exception e) {
				log.warn("Failed to acquire result formatter for [" + resultBind + "]",e);
				resultBind = null;
			}
		}
		if(binder != null) {
			try {
				provider = BindVariableProviderFactory.getInstance().getProvider(binder);
			} catch (Exception e) {
				log.warn("Failed to acquire bind variable provider for [" + binder + "]",e);
				binder = null;
			}
		}
		this.binderEnabled = (bindFormatter!=null && provider!=null);
	}
	
	/**
	 * Extracts data from the passed result set and connection meta-data and generates HOT traces. Invoked by the SQLMapper after the query completes.
	 * @param prs The retrieved result set.
	 * @param connMetaData The connection meta-data.
	 */
	//-
	public void traceMetrics(IReadOnlyProcessedResultSet prs, Map<String, Object> connMetaData) {
		if(!tracerEnabled) return;
		/*		MetricType metricType = MetricType.typeForCode(metricTypeFormatter.getValue(prs, connMetaData));
//		Builder builder = tracer.trace(
//				nameFormatter.getValue(prs, connMetaData),  // "Elapsed Time"
//				valueFormatter.getValue(prs, connMetaData), // String: "7543287"
//				metricType.name())  // "STICKY_DELTA_LONG_AVG" 
//		.segment(prefix)  // ["Database"]
//		.segment(segmentFormatter.getValues(prs, connMetaData))  // [ECS, Oracle, SQL, Statements, SELECT state,event_data from event where event_name=:1]
//		.temporal(temporal);
		ICEMetric trace = null;		
		if(metricType.isDelta()) {
			if(metricType.isLong()) {
				if(metricType.isSticky()) {
					
					trace = tracer.traceStickyDelta(
							new Double(
									valueFormatter.getValue(prs, connMetaData)
							).longValue(), 
							nameFormatter.getValue(prs, connMetaData), 
							prefix, 
							segmentFormatter.getValues(prs, connMetaData)
					);
					//log.info("DB Trace:\n\tRaw Value:[" + valueFormatter.getValue(prs, connMetaData) + "]\n\tTrace:" + trace);
				} else {
					trace = tracer.traceDelta(
							Long.parseLong(
									valueFormatter.getValue(prs, connMetaData)
							), 
							nameFormatter.getValue(prs, connMetaData), 
							prefix, 
							segmentFormatter.getValues(prs, connMetaData)
					);
				}
			} else if(metricType.isInt()) {
				if(metricType.isSticky()) {
					trace = tracer.traceStickyDelta(Integer.parseInt(valueFormatter.getValue(prs, connMetaData)), nameFormatter.getValue(prs, connMetaData), prefix, segmentFormatter.getValues(prs, connMetaData));
				} else {
					trace = tracer.traceDelta(Integer.parseInt(valueFormatter.getValue(prs, connMetaData)), nameFormatter.getValue(prs, connMetaData), prefix, segmentFormatter.getValues(prs, connMetaData));
				}				
			}
		} else {
			if(metricType.isLong()) {
				if(metricType.isSticky()) {
					trace = tracer.traceSticky(Long.parseLong(valueFormatter.getValue(prs, connMetaData)), nameFormatter.getValue(prs, connMetaData), prefix, segmentFormatter.getValues(prs, connMetaData));
				} else {
					trace = tracer.trace(Long.parseLong(valueFormatter.getValue(prs, connMetaData)), nameFormatter.getValue(prs, connMetaData), prefix, segmentFormatter.getValues(prs, connMetaData));
				}
			} else if(metricType.isInt()) {
				if(metricType.isSticky()) {
					trace = tracer.traceSticky(Integer.parseInt(valueFormatter.getValue(prs, connMetaData)), nameFormatter.getValue(prs, connMetaData), prefix, segmentFormatter.getValues(prs, connMetaData));
				} else {
					trace = tracer.trace(Integer.parseInt(valueFormatter.getValue(prs, connMetaData)), nameFormatter.getValue(prs, connMetaData), prefix, segmentFormatter.getValues(prs, connMetaData));
				}				
			}			
		}
		//tracer.traceTrace(trace);
		if(scope != null && trace!=null) {
			scopeMap.put(trace.getFQN(), new ScopeState(trace.getMetricType(), true, nameFormatter.getValue(prs, connMetaData), segmentFormatter.getValues(prs, connMetaData)));
		}*/
	}
	
	/**
	 * Executes the result bind directive, if it is defined.
	 * @param prs The retrieved result set.
	 * @param connMetaData The connection meta-data.
	 */
	public void executeBinds(IReadOnlyProcessedResultSet prs, Map<String, Object> connMetaData) {
		if(!binderEnabled) return;
		provider.setValue(bindFormatter.getValue(prs, connMetaData));		
	}
	
	/**
	 * Callback from SQLMapping to reset scope before tracing starts.
	 */
	public void resetScope() {
		if(!tracerEnabled) return;
		if(scope!=null) {
			for(String name: scopeMap.keySet()) {
				scopeMap.get(name).setAccounted(false);
			}
		}
	}
	
	/**
	 * Traces scope values for any metrics that failed scoping.
	 * Once the scoped value has been traced, the metric name is removed from scope.!{bean:SchemaNameProvider}
	 */
	//- 
	public void traceScopeFailures() {
		if(!tracerEnabled) return;
		/*Set<String> removes = new HashSet<String>();
		for(Map.Entry<String, ScopeState> scopes: scopeMap.entrySet()) {    
			ScopeState state = scopes.getValue(); 
			if(!state.isAccounted()) {				
				Builder builder = tracer.trace(
						state.getName(), 
						scope,
						state.getType())
				.segment(prefix)
				.segment(state.getSegment())
				.temporal(temporal)
				.deltaReset();
				Trace trace = tracer.traceTrace(builder.trace());
				if(trace!=null && state.getType().isDelta()) {
					DeltaManager.getInstance().reset(trace.getFQN());
				}
				removes.add(scopes.getKey());
				
			}
		}
		for(String name: removes) {
			scopeMap.remove(name);
		}*/
	}
	
	
	/**
	 * Creates a new MetricMap
	 * @param valueCol
	 * @param segment
	 * @param name
	 * @param type
	 * @param scope
	 * @param temporal
	 * @param flatten
	 */
	public MetricMap(String valueCol, String segment, String name, String type,
			String scope, boolean temporal, boolean flatten, String resultBind, String binder) {
		this.valueCol = valueCol;
		this.segment = segment;
		this.name = name;
		this.type = type;
		this.scope = scope;
		this.temporal = temporal;
		this.flatten = flatten;
		this.resultBind = resultBind;
		this.binder = binder;
	}
	
	
	/**
	 * @return the valueCol
	 */
	public String getValueCol() {
		return valueCol;
	}
	/**
	 * @param valueCol the valueCol to set
	 */
	public void setValueCol(String valueCol) {
		this.valueCol = valueCol;
	}
	/**
	 * @return the segment
	 */
	public String getSegment() {
		return segment;
	}
	/**
	 * @param segment the segment to set
	 */
	public void setSegment(String segment) {
		this.segment = segment;
	}
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}
	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}
	/**
	 * @return the scope
	 */
	public String getScope() {
		return scope;
	}
	/**
	 * @param scope the scope to set
	 */
	public void setScope(String scope) {
		this.scope = scope;
	}
	/**
	 * @return the temporal
	 */
	public boolean isTemporal() {
		return temporal;
	}
	/**
	 * @param temporal the temporal to set
	 */
	public void setTemporal(boolean temporal) {
		this.temporal = temporal;
	}
	/**
	 * @return the flatten
	 */
	public boolean isFlatten() {
		return flatten;
	}
	/**
	 * @param flatten the flatten to set
	 */
	public void setFlatten(boolean flatten) {
		this.flatten = flatten;
	}
	/**
	 * @param prefix the prefix to set
	 */
	public void setPrefix(String[] prefix) {
		this.prefix = prefix;
	}
	/**
	 * @param tracerFactory the tracerFactory to set
	 */
	public void setTracer(ITracer tracer) {
		this.tracer = tracer;
	}

	/**
	 * @return the delimeter
	 */
	public String getDelimeter() {
		return delimeter;
	}

	/**
	 * @param delimeter the delimeter to set
	 */
	public void setDelimeter(String delimeter) {
		this.delimeter = delimeter;
	}
	
	/**
	 * <p>Title: ValueReader</p>
	 * <p>Description: Reads a column value from a result set, result set header or DB Conn Meta Data</p> 
	 */
	public static class ValueReader {
		/** The value identifier  */
		protected int colNum = -1;
		/** The value name */
		protected String colName = null;
		/** The value type  */
		protected String colType = null;
		
		/**
		 * Creates a new ValueReader
		 * Valid types are:<ul>
		 * <li><b>c</b>: The column name</li>
		 * <li><b>v</b>: The column value for the current row</li>
		 * <li><b>m</b>: Database meta-data entry</li>
		 * <li><b>t</b>: The JDBC Type Code</li>
		 * <li><b>n</b>: The JDBC Type Name</li>
		 * <li><b>j</b>: The JDBC Java Class Name</li>
		 * <li><b>s</b>: The column database specific type name</li>
		 * <li><b>q</b>: The query name</li>
		 * </ul>
		 * @param type The value reader type 
		 * @param value The value key
		 */
		ValueReader(String type, String value) {
			if(value==null || value.length()<1) throw new RuntimeException("Invalid value specification");
			if(type==null || type.length()<1 || !validateType(type)) throw new RuntimeException("Invalid type specification");
			colType = type;
			if(colType.equalsIgnoreCase("m")) { // keep value as String
				colName = value;
			} else {  // might be a column number or name
				try { colNum = Integer.parseInt(value); colName = null; } catch (Exception e) { colName = value; colNum = -1; }
			}
		}
		/**
		 * Retrieves the configured/requested value from the result set or the metadata
		 * @param prs The processed result set to extract from
		 * @param connMetaData The connection meta-data set to extract from
		 * @return The extracted value.
		 */
		String getValue(IReadOnlyProcessedResultSet prs, Map<String, Object> connMetaData) {
			Object result =  null;
			switch ((int)colType.toCharArray()[0]) {
			case (int)'q':
				result = prs.getQueryName();
				break;
			case (int)'m':
				result = connMetaData.get(colName);
				break;				
			case (int)'c':
				result =  colName==null ? prs.getColumnName(colNum) : prs.getColumnName(colName); 
				break;
			case (int)'v':
				result =  colName==null ? prs.get(colNum) : prs.get(colName); 
				break;
			case (int)'n':
				result =  colName==null ? prs.getColumnTypeName(colNum) : prs.getColumnTypeName(colName); 
				break;
			case (int)'t':
				result =  colName==null ? prs.getColumnType(colNum) : prs.getColumnType(colName); 
				break;
			case (int)'j':
				result =  colName==null ? prs.getColumnClassName(colNum) : prs.getColumnClassName(colName); 
				break;
			case (int)'s':
				result =  colName==null ? prs.getDbTypeName(colNum) : prs.getDbTypeName(colName); 
				break;					
			default:
				throw new RuntimeException("Unlikely but unrecognized token encountered in ValueReader [" + colType + "]");
			}		
			return result==null ? "" : result.toString();
		}
		
		/**
		 * Validates the the type string is valid
		 * Valid values are:<ul>
		 * <li><b>c</b>: The column name</li>
		 * <li><b>v</b>: The column value for the current row</li>
		 * <li><b>m</b>: Database meta-data entry</li>
		 * <li><b>t</b>: The JDBC Type Code</li>
		 * <li><b>n</b>: The JDBC Type Name</li>
		 * <li><b>j</b>: The JDBC Java Class Name</li>
		 * <li><b>s</b>: The column database specific type name</li>
		 * <li><b>q</b>: The query name</li>
		 * </ul> 
		 * @param s The value type code.  (c: column, v: value, m: meta-data)
		 * @return true if the type is valid.
		 */
		boolean validateType(String s) {
			return s.equalsIgnoreCase("c") || s.equalsIgnoreCase("v") || s.equalsIgnoreCase("m") || s.equalsIgnoreCase("t") || s.equalsIgnoreCase("n") || s.equalsIgnoreCase("j") || s.equalsIgnoreCase("s") || s.equalsIgnoreCase("q");
		}
	}
	
	/**
	 * <p>Title: Formatter</p>
	 * <p>Description: Utility class to create specifically formated <code>ValueReader</code> instances and render a full value from them at collect time.
	 * Directives are:<ul>
	 * <li><b>c</b>: The column name</li>
	 * <li><b>v</b>: The column value for the current row</li>
	 * <li><b>m</b>: Database meta-data entry</li>
	 * <li><b>t</b>: The JDBC Type Code</li>
	 * <li><b>n</b>: The JDBC Type Name</li>
	 * <li><b>j</b>: The JDBC Java Class Name</li>
	 * <li><b>s</b>: The column database specific type name</li>
	 * <li><b>q</b>: The query name</li>
	 * </ul>
	 * </p> 
	 */
	public static class Formatter {
		/** The value reader token parser  */
		static final Pattern SEG_PATTERN = Pattern.compile("(\\{([v|c|m|t|n|j|s|q]):(\\d+|\\w+)\\})", Pattern.CASE_INSENSITIVE);
		/** An empty segment default value */
		static final String[] emptySegment = {};
		/** The configured delimeter */
		protected String delim = null;
		/** The configured raw value */
		protected String rawValue = null;		
		/** ValueReader map keyed by token */
		protected Map<String, ValueReader> valueReaders = new HashMap<String, ValueReader>();
		/** flag indicating an empty segment */
		protected boolean empty = false;
		
		/**
		 * Creates a new Formatter instance.
		 * @param delim The configured delimeter for multi values
		 * @param config The full raw configuration string.
		 */
		Formatter(String delim, String config) {
			this.delim = delim;
			this.rawValue = config;
			if(config==null||config.length()<1) {
				empty = true;
			} else {
				Matcher m = SEG_PATTERN.matcher(config);
				while(m.find()) {
					String token = m.group(1);				
					if(!valueReaders.containsKey(token)) {
						valueReaders.put(token, new ValueReader(m.group(2), m.group(3)));
					}
				}
			}
		}
		/**
		 * Returns a multi valued formatted value
		 * @param prs The processed result set
		 * @param connMetaData The connection meta-data map
		 * @return An array of formatted strings.
		 */
		String[] getValues(IReadOnlyProcessedResultSet prs, Map<String, Object> connMetaData) {
			if(empty) return emptySegment;
			return formatValue(prs, connMetaData).split(Pattern.quote(delim));
		}
		/**
		 * Returns a single valued formatted value
		 * @param prs The processed result set
		 * @param connMetaData The connection meta-data map
		 * @return A formatted string.
		 */
		String getValue(IReadOnlyProcessedResultSet prs, Map<String, Object> connMetaData) {
			if(empty) return "";
			return formatValue(prs, connMetaData);
		}

		/**
		 * Iterates through all registered value readers and replaces value tokens with the actual data.
		 * @param prs The processed result set
		 * @param connMetaData The connection meta-data map
		 * @return The unsplit formatted string
		 */
		private String formatValue(IReadOnlyProcessedResultSet prs, Map<String, Object> connMetaData) {
			String raw = new String(rawValue);
			for(Map.Entry<String, ValueReader> vr: valueReaders.entrySet()) {
				raw = raw.replace(vr.getKey(), clean(vr.getValue().getValue(prs, connMetaData)));
			}
			return raw;
		}
		
		public static String clean(CharSequence s) {
			if(s==null) return null;
			return s.toString().replaceAll("/", "\\\\");
		}
		
	}
	
	public static class ScopeState {
		protected MetricType type = null;
		protected boolean accounted = true;
		protected String name = null;
		protected String[] segment = null;
		/**
		 * @param type
		 * @param accounted
		 * @param name
		 * @param segment
		 */
		public ScopeState(MetricType type, boolean accounted, String name,
				String[] segment) {
			this.type = type;
			this.accounted = accounted;
			this.name = name;
			this.segment = segment;
		}
		/**
		 * @return the type
		 */
		public MetricType getType() {
			return type;
		}
		/**
		 * @param type the type to set
		 */
		public void setType(MetricType type) {
			this.type = type;
		}
		/**
		 * @return the accounted
		 */
		public boolean isAccounted() {
			return accounted;
		}
		/**
		 * @param accounted the accounted to set
		 */
		public void setAccounted(boolean accounted) {
			this.accounted = accounted;
		}
		/**
		 * @return the name
		 */
		public String getName() {
			return name;
		}
		/**
		 * @param name the name to set
		 */
		public void setName(String name) {
			this.name = name;
		}
		/**
		 * @return the segment
		 */
		public String[] getSegment() {
			return segment;
		}
		/**
		 * @param segment the segment to set
		 */
		public void setSegment(String[] segment) {
			this.segment = segment;
		}
		
	}

	/**
	 * @return
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((segment == null) ? 0 : segment.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	/**
	 * @param obj
	 * @return
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MetricMap other = (MetricMap) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (segment == null) {
			if (other.segment != null)
				return false;
		} else if (!segment.equals(other.segment))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}

	/**
	 * Constructs a <code>String</code> with all attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString()  {
	    final String TAB = "\n\t";
	    StringBuilder retValue = new StringBuilder("MetricMap [");    	    
	    retValue.append(TAB).append("tracerEnabled=").append(this.tracerEnabled);
	    if(tracerEnabled) { 
		    retValue.append(TAB).append("name=").append(this.name);    
		    retValue.append(TAB).append("segment=").append(this.segment);    
		    retValue.append(TAB).append("type=").append(this.type);    
		    retValue.append(TAB).append("valueCol=").append(this.valueCol);
	    }
	    retValue.append(TAB).append("scope=").append(this.scope);    
	    retValue.append(TAB).append("resultBind=").append(this.resultBind);
	    retValue.append(TAB).append("binder=").append(this.binder);
	    retValue.append(TAB).append("temporal=").append(this.temporal);    
	    retValue.append(TAB).append("flatten=").append(this.flatten);    
	    retValue.append(TAB).append("prefix=").append(this.prefix==null ? "" : Arrays.toString(prefix));
	    retValue.append(TAB).append("delimeter=").append(this.delimeter);    
	    retValue.append("\n]");
	    return retValue.toString();
	}

	/**
	 * @return the resultBind
	 */
	public String getResultBind() {
		return resultBind;
	}

	/**
	 * @param resultBind the resultBind to set
	 */
	public void setResultBind(String resultBind) {
		this.resultBind = resultBind;
	}
	
	/**
	 * @param binder
	 */
	public void setBinder(String binder) {
		this.binder = binder;
	}
	
	/**
	 * @return
	 */
	public String getBinder() {
		return binder;
	}

	/**
	 * @return the binderEnabled
	 */
	public boolean isBinderEnabled() {
		return binderEnabled;
	}

	/**
	 * @param binderEnabled the binderEnabled to set
	 */
	public void setBinderEnabled(boolean binderEnabled) {
		this.binderEnabled = binderEnabled;
	}

	/**
	 * @return the tracerEnabled
	 */
	public boolean isTracerEnabled() {
		return tracerEnabled;
	}

	/**
	 * @param tracerEnabled the tracerEnabled to set
	 */
	public void setTracerEnabled(boolean tracerEnabled) {
		this.tracerEnabled = tracerEnabled;
	}
	
	
	
}
