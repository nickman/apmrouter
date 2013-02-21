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
package org.helios.collector.jmx;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadInfo;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.helios.collector.core.AbstractCollector;
import org.helios.collector.core.CollectionResult;
import org.helios.collector.core.CollectorException;
import org.helios.collector.jmx.connection.IMBeanServerConnectionFactory;
import org.helios.collector.jmx.connection.MBeanServerConnectionFactoryException;
import org.helios.collector.jmx.tracers.IObjectFormatter;
import org.helios.collector.jmx.tracers.IObjectTracer;
import org.helios.collector.jmx.tracers.JMXAttributeTrace;
import org.helios.collector.jmx.tracers.JMXObject;
import org.helios.apmrouter.jmx.JMXHelper;
import org.helios.apmrouter.util.StringHelper;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;


/**
 * <p>Title: JMXCollector</p>
 * <p>Description: Collect and Trace JMX Attributes</p> 
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 * 		   Whitehead (whitehead.nicholas@gmail.com)
 */

//TODO
// Support VirtualTracers
// Invoke JVMMonitor if someone is interested in MXBean statistics - no need 
// to duplicate that functionality here 
@ManagedResource
public class JMXCollector extends AbstractCollector {
	
	/**	The properties to make the MBean Server Connection */
	protected Properties properties = new Properties();
	
	/**	The JMX Service URL for making an MBean Server Connection */
	protected String jmxServiceURL = null;	
	
	/** The MBeanServerConnectionFactory to get a JMX Connection From */
	protected IMBeanServerConnectionFactory connectionFactory = null;
	
	/** The MBeanServerConnectionFactory to get a JMX Connection From */
	protected MBeanServerConnection mBeanServerConnection = null;	
	
	/** JMXCollector Version*/
	private static final String JMX_COLLECTOR_VERSION="0.1"; 
	
	/** The availability MBean Object Name */
	protected ObjectName availabilityMBean = null;
	/** The availability MBean Attribute */
	protected String availabilityAttribute = null;
	/** The availability segments */
	protected String[] availabilitySegment = null;	
	/** List that stores all JMXObjects fed from the config file */
	protected List<JMXObject> jmxObjects = new ArrayList<JMXObject>();
	/** List that stores all processed JMXObjects with resolved segment names  */
	protected Map<String, JMXObject> resolvedJMXObjects = new HashMap<String, JMXObject>();
//	/** Flag to indicate whether MXBeans should be traced */
//	protected boolean traceMXBeans = false;
	/** The MXBean compiled segment */
	protected String mxBeanSegment = null;
	/** The composite type compound name delimeter. Defaults to "/" */
	protected String compoundNameDelimeter = "/";
//	/** A map of MXBean ObjectNames keyed by the ObjectName string */
//	protected Map<String, ObjectName> mxBeanObjectNames = new HashMap<String, ObjectName>();

//	/** MXBean ObjectNames that should be included in MXBean Collection */
//	protected Set<ObjectName> includeMXBeans = new HashSet<ObjectName>();
//	/** MXBean ObjectNames that should be excluded in MXBean Collection */
//	protected Set<ObjectName> excludeMXBeans = new HashSet<ObjectName>();	
	
//	/** MXBean ObjectNames and a boolean for ObjectNames that have been examined and included or excluded. */
//	protected Map<ObjectName, Boolean> tracedMXBeans = new HashMap<ObjectName, Boolean>(30);	
	
//	/** Indicates if the one time collection of Runtime attributes has completed */
//	protected boolean runtimeCollected = false;
//
//	/** Indicates if compiler time monitoring is supported in the target VM */
//	protected Boolean supportsCompilerTime = null;	
	
//	/** A map of GC ObjectNames keyed by the gc name */
//	protected Map<String, ObjectName> gcObjectNames = null;	
//	/** GC MXBean Mask Object Name */
//	protected ObjectName gcMXBean = null;	
	
	/** The number of times subsidiary mbeans should be queried for before stopping */
	protected int mbeanQueryAttempts = 5;
	/** The number of times subsidiary mbeans have been queried for */
	protected int mbeanQueryAttempted = 0;	
	/** The number of elapsed collections that should occur before GC % time is calculated */
	protected int gCPollCycles = 5;
	/** The number of elapsed collections that have occured since GC % time was calculated */
	protected int gCPolledCycles = 0;	
	/** A map of GC Collection and Elapsed Times keyed by the gc name */
	protected Map<String, long[]> gcTimes = new HashMap<String, long[]>();	
	
//	/** An objectName / Attribute name for a virtual tracer host name */
//	protected String virtualHostLocator = null; 
//	/** An objectName / Attribute name for a virtual tracer agent name */
//	protected String virtualAgentLocator = null; 
//	/** The virtual tracer host */
//	protected String virtualHost = null;
//	/** The virtual tracer agent */
//	protected String virtualAgent = null;
	
//	/** A map of MemoryPool ObjectNames keyed by the memory pool name */
//	protected Map<String, ObjectName> memoryPoolObjectNames = null;	
//	/** Memory Pool MXBean Mask Object Name */
//	protected ObjectName memoryPoolMXBean = null;	
//	
//	/** Indicates if thread deadlocking should be monitored */
//	protected boolean deadLockMonitor = false;	
//
//	/** Indicates if thread contention monitoring is supported in the target VM */
//	protected Boolean supportsThreadContention = null;		
//	
//	/** Indicates if thread status aggregate summary should be monitored */
//	protected boolean threadMonitor = false;
	
	public static final String[] NULL_SIG = new String[]{};
	public static final Object[] NULL_ARG = new Object[]{};	
//	protected static final String[] CLASS_LOADING_STATS = new String[]{"LoadedClassCount", "TotalLoadedClassCount", "UnloadedClassCount"};
//	protected static final String[] THREAD_STATS = new String[]{"ThreadCount", "DaemonThreadCount", "TotalStartedThreadCount", "PeakThreadCount"};	

//	/** Thread State aggregator */
//	protected Map<Thread.State, Integer> threadStates = new HashMap<Thread.State, Integer>(12);	
	
	protected Context ctx = null;
	
	
	/**
	 * The constructor for passing an instance of IMBeanServerConnectionFactory
	 * 
	 * @param properties
	 * @param connectionFactory
	 */
	public JMXCollector(IMBeanServerConnectionFactory connectionFactory) {
		this.connectionFactory=connectionFactory;
	}
		
	
	/**
	 * @param resetRequired
	 * 		false indicates that it's the initial startup of this collector so no reset required.  
	 * 		true indicates that the original MBeanServerConnection has got invalid and 
	 * 		needs to be recreated based on initial jndi properties or JMX Service URL passed
	 * 		through the bean configuration.
	 * @throws Exception
	 */
	protected void initMBeanServerConnection(boolean resetRequired) throws Exception {
		try {
			// first time connection creation
			if(!resetRequired)
				mBeanServerConnection = connectionFactory.getConnection();
			//broken connection - try to recreate now...
			else
				mBeanServerConnection = connectionFactory.resetConnection();
			if(ctx==null) {
					ctx = new InitialContext();
			}
//			if(this.virtualHostLocator!=null) {
//				virtualHost = (String)JMXHelper.getAttribute(mBeanServerConnection, virtualHostLocator);
//				if(virtualHost==null) {
//					warn("A virtual host locator [" + virtualHostLocator + "] was supplied but could not be resolved. A virtual tracer will not be used." );
//				}
//			}
//			
//			if(this.virtualAgentLocator!=null && this.virtualHostLocator!=null) {
//				virtualAgent= (String)JMXHelper.getAttribute(mBeanServerConnection, virtualAgentLocator);
//				if(virtualHost==null) {
//					warn("A virtual agent locator [" + virtualAgentLocator + "] was supplied but could not be resolved. A virtual tracer will not be used." );
//				}
//			}
//			if(virtualHost!=null && virtualAgent!=null) {
//				this.tracer = this.tracer.getVirtualTracer(virtualHost, virtualAgent);
//			}			
		}catch(MBeanServerConnectionFactoryException mex){
			throw new Exception("Unable to get MBeanServerConnection for JMXCollector",mex);
		}catch(NamingException nex){
			throw new Exception("Unable to initialize context",nex);
		}catch(Exception ex){
			throw new Exception("Unable to get MBeanServerConnection",ex);
		}
	}


	public CollectionResult collectCallback() {
		long st = System.currentTimeMillis();
		CollectionResult collectionResult = new CollectionResult();
		resetProcessingFlag();
		
		// Check the status of MBeanServerConnection
		if(checkMBeanServerConnection(collectionResult)==false){
			// Error getting MBean Server connection so no reason to proceed further
			// Return the CollectionResult object back 
			return collectionResult;			
		}
		
		// Run an additional availability check (if provided)
		if(availabilityMBean != null && availabilityAttribute != null) {
			if(availibilityCheck(collectionResult)==false){
				// User MBean Server availability check failed so no reason to proceed further
				// Return the CollectionResult object back 
				return collectionResult;
			}
		}
		
		boolean anySuccess = false;
		boolean anyFailure = false;
		for(JMXObject tr: jmxObjects) {
			try {
				List<JMXAttributeTrace> jmxAttributeTraces = tr.getTargetAttributeTraces();
				if(anyAttributesToProcess(tr, jmxAttributeTraces)==false){
					// There are no attribute defined to query for this target objectName
					// Skip the processing for this object and continue to next JMXObject
					anyFailure=true;
					continue;
				}
				Iterator<ObjectName> mbeans = null;
				Map<ObjectName, Map<String, Object>> mappedValues;
				try{
					mappedValues = JMXHelper.getMBeanAttributeMap(mBeanServerConnection, tr.targetObjectName, compoundNameDelimeter, tr.getAttributeNames());
					mbeans = mappedValues.keySet().iterator();
				} catch (Exception ioex){
					// Error communicating with MBean Server
					anyFailure=true;
					mBeanServerConnection = null;
					if(logErrors) 
						error("Failed to read results from MBeanServer", ioex);
					traceDefaultsForOffline();
					collectionResult.setAnyException(ioex);
					return determineStatus(anySuccess, anyFailure, collectionResult);
				}
				while(mbeans.hasNext()) {
					ObjectName on = (ObjectName)mbeans.next();
					if(tr.getAttributeNames().size()>0){
						// get values for all attributes in one swipe for this MBean 
						Map<String,Object> explodedResults = mappedValues.get(on);
						if(explodedResults == null){
							 // Error occured while processing attributes for this MBean
							 // skip this one and proceed processing OTHER MBeans
							 anyFailure=true;
							 continue;
						 }
						
						// Check whether this MBean has been processed before.  if yes, then used resolved 
						// metric/segment/segmentPrefixElements instead of resolving tokens again
						if(resolvedJMXObjects.containsKey(on.getCanonicalName())){
							trace("Already have the key in cache: "+on.getCanonicalName());
							JMXObject cachedObject = resolvedJMXObjects.get(on.getCanonicalName());
							if(cachedObject!=null){
								processCachedObject(on, cachedObject, explodedResults);
								anySuccess=true;
								cachedObject.setProcessed(true);
								JMXObject tempObject = new JMXObject(cachedObject);
								resolvedJMXObjects.put(on.getCanonicalName(),tempObject);
							}
						} else {
							// Either a new MBean popped up as part of the returned results 
							// or its the first poll of this JMXCollector
							trace("**************** No entry in cache for key : "+on.getCanonicalName());
							processNonCachedObject(on, tr, explodedResults,jmxAttributeTraces);
							anySuccess=true;
							// Mark Processing status to true - If this key already exist in the list, its status will be changed to true, if not
							// it will be added to this list with status true.						
							tr.setProcessed(true);
							JMXObject tempObject = new JMXObject(tr);
							resolvedJMXObjects.put(on.getCanonicalName(),tempObject);
							tr.clearResolvedAttributes();
						}
					}
				}
			} catch (Exception ex) {
				anyFailure=true;
				if(logErrors) 
					error("Exception occured while tracing JMX Attributes for MBean: "+tr.getTargetObjectName(), ex);
				continue;
			}
		}		
		
		// Done with processing of all online MBeans that were returned by this query. Now check whether
		// there are any MBean(s) that were online during the last collection but are offline this time
		traceDefaultsForOffline();
//		try {
//			if(traceMXBeans){
//				long startMX = System.currentTimeMillis();
//				collectMXBeans();
//				tracer.trace(System.currentTimeMillis()-startMX, "Elapsed Time for MXBeans", StringHelper.append(tracingNameSpace,true,mxBeanSegment));
//			}
//		} catch (Exception mxe) {
//			anyFailure=true;
//			if(logErrors) {
//				error("MXBean Collection Error", mxe);
//			}
//		}
		//tracer.traceSticky(System.currentTimeMillis() - st, "Elapsed Time", availabilitySegment);
		tracer.traceGauge(System.currentTimeMillis() - st, "Elapsed Time", availabilitySegment);
		return determineStatus(anySuccess, anyFailure, collectionResult);
	}

	/**
	 * Iterates through all registered attributes and traces the default, if defined. 
	 */
	protected void traceDefaultsForOffline() {
		// Check if it is the first run for this JMXCollector.  If yes, skip tracing 
		// defaults as we never got an MBeanServerConnection.  Otherwise proceed with tracing defaults. 
		if(resolvedJMXObjects!=null && !resolvedJMXObjects.isEmpty()){
			Iterator keys = resolvedJMXObjects.keySet().iterator();
			while(keys.hasNext()){
				String tempKey = (String)keys.next();
				JMXObject cachedObject = (JMXObject)resolvedJMXObjects.get(tempKey);
				if(! cachedObject.isProcessed()){
					for(int b=0; b<cachedObject.getResolvedAttributes().size();b++){
						JMXAttributeTrace cachedTrace = cachedObject.getResolvedAttributes().get(b);
						if(cachedTrace!=null && cachedTrace.getDefaultValue()!=null){
							//- tracer.smartTrace(cachedTrace.getTraceType(),cachedTrace.getDefaultValue(),cachedTrace.getMetricName(), StringHelper.append(tracingNameSpace,true,cachedTrace.getResolvedPrefix()), "");
							tracer.trace(cachedTrace.getDefaultValue(), cachedTrace.getMetricName(), cachedTrace.getTraceType(), StringHelper.append(tracingNameSpace,true,cachedTrace.getResolvedPrefix()));
						}
					}
				}
			}
		}
	}	
	
	/**
	 * 
	 */
	protected boolean availibilityCheck(CollectionResult result) {
		try {
			mBeanServerConnection.getAttribute(availabilityMBean, availabilityAttribute);
			//tracer.traceSticky(1, defaultAvailabilityLabel, availabilitySegment);
			tracer.traceGauge(1, defaultAvailabilityLabel, availabilitySegment);
			return true;
		} catch (Exception e) {
			mBeanServerConnection=null;
			traceAvailability(0);
			if(logErrors) {
				error("Exception Running Availability Check for [" + availabilityMBean + "/" + availabilityAttribute, e);
			}
			result.setResultForLastCollection(CollectionResult.Result.FAILURE);
			result.setAnyException(e);
			return false;
		}
	}	

//	/**
//	 * Resets the environment on initial start and reconnect.
//	 * The assumption is that on start or reset, the target MXBeans may have changed.
//	 */
//	protected void resetEnv() {
//		mxBeanObjectNames.clear();
//		memoryPoolObjectNames= null;
//		gcObjectNames= null;
//		gcTimes = new HashMap<String, long[]>();
//		supportsCompilerTime= null;
//		supportsThreadContention= null;
//		tracedMXBeans.clear();
//		mbeanQueryAttempted = 0;
//		gCPolledCycles = 0;
//		runtimeCollected = false;
//		if(mxBeanSegment==null) {
//			mxBeanSegment = "MXBeans";
//		}
//	}
	
	/**
	 * Utility method that determines the overall status of the last collect call status.
	 * It could be Success, Failure or Partial "Success"
	 * 
	 * @param anySuccess
	 * @param anyFailure
	 * @param result
	 * @return
	 */
	protected CollectionResult determineStatus(boolean anySuccess, boolean anyFailure,
											CollectionResult result) {
		if(anyFailure){
			if(anySuccess){
				result.setResultForLastCollection(CollectionResult.Result.PARTIAL);
			}else{
				result.setResultForLastCollection(CollectionResult.Result.FAILURE);
			}
		}else {
			result.setResultForLastCollection(CollectionResult.Result.SUCCESSFUL);
		}	
		return result;
	}
	
	/**
	 * Query the MBeanServer for matching Attributes   
	 * 
	 * @param on ObjectName of the target MBean(s) (supports wildcard characters)
	 * @param tr JMXObject
	 * @return
	 */
	protected HashMap<String,Object> queryAttributes(ObjectName on, JMXObject tr) {
		HashMap<String,Object> explodedResults=null;
		try{
			AttributeList attributeResults = new AttributeList();
			attributeResults = mBeanServerConnection.getAttributes(on, tr.getAttributeNames().toArray(new String[0]));
			if(attributeResults.size()>0){
				explodedResults = new HashMap<String,Object>();
			}
			for(int a=0; a<attributeResults.size();a++){
				Attribute attribute = (Attribute)attributeResults.get(a);
				explodedResults.put(attribute.getName(),attribute.getValue());
			}			
		} catch (IOException ioe){
			mBeanServerConnection = null;
			if(logErrors) 
				error("Failed to read results from MBeanServer", ioe);
		} catch (InstanceNotFoundException infex) {
			if(logErrors) 
				error("The specified object name does not exist: "+on.getCanonicalName(), infex);
		} catch (ReflectionException rex) {
			if(logErrors) 
				error("Error occured while executing getAttributes on this MBean: "+on.getCanonicalName(), rex);
		}
		return explodedResults;
	}
	
	/** 
	 * Determine whether there are any Attributes to process
	 */
	protected boolean anyAttributesToProcess(JMXObject tr, List<JMXAttributeTrace> jmxAttributeTraces) {
		if(tr.getAttributeNames()==null){
			// First run - so prepare and cache the list of attributes that needs to be traced for the MBean(s)
			// returned by the querying targetObjectName property
			ArrayList<String> attributes = new ArrayList<String>();
			for(int i=0;i<jmxAttributeTraces.size();i++){
				attributes.add(jmxAttributeTraces.get(i).getTargetAttributeName());
			}
			tr.setAttributeNames(attributes);
		} 
		if(tr.getAttributeNames().size()<=0){
				if(logErrors)
					error("No target attributes defined for: "+tr.targetObjectName);
				return false;
		}
		return true;
	}

	protected void process(ObjectName on, JMXAttributeTrace trace,
			Map<String, Object> explodedResults) {
		
		if ( trace.getObjectTracers().size()<1 && trace.getObjectFormatters().size()<1){
			Object attrValue = explodedResults.get(trace.getTargetAttributeName());
			if(attrValue!=null) {
				//- tracer.smartTrace(trace.getTraceType(),attrValue.toString(),trace.getMetricName(), StringHelper.append(tracingNameSpace,true,trace.getResolvedPrefix()), "");
				tracer.trace(attrValue.toString(), trace.getMetricName(), trace.getTraceType(), StringHelper.append(tracingNameSpace,true,trace.getResolvedPrefix()));
			}
		}
		
		for(IObjectFormatter oFormatter: trace.getObjectFormatters()){
				//- tracer.smartTrace(trace.getTraceType(),oFormatter.format(explodedResults.get(trace.getTargetAttributeName())),oFormatter.getMetricName().equals("")?trace.getMetricName():oFormatter.getMetricName(), StringHelper.append(tracingNameSpace,true,trace.getResolvedPrefix()), "");
				tracer.trace(oFormatter.format(explodedResults.get(trace.getTargetAttributeName())), oFormatter.getMetricName().equals("")?trace.getMetricName():oFormatter.getMetricName(), trace.getTraceType(), StringHelper.append(tracingNameSpace,true,trace.getResolvedPrefix()));
		}
		
		for(IObjectTracer oTracer: trace.getObjectTracers()){
			try{ 
				long start = System.currentTimeMillis();
				oTracer.prepareBindings("remoteMBeanServer", mBeanServerConnection, "remoteObjectName", on, "localObjectName", objectName, "tracer", tracer, "tracingNameSpace",tracingNameSpace, "jndi", ctx);
				if(! oTracer.trace(explodedResults.get(trace.getTargetAttributeName()))){
					throw new Exception();
				}
				debug("** OTracer call completed in: "+ (System.currentTimeMillis()-start)+" milliseconds." );
			}catch(Exception ex){
				if(logErrors)
					error("There was an error rendering traces for object tracer: " + oTracer + " for Bean: " + this.getBeanName(),ex);
			}
		}
	}	
	
	protected void processNonCachedObject(ObjectName on, JMXObject tr, Map<String, Object> explodedResults,
										List<JMXAttributeTrace> jmxAttributeTraces) {
		for(int k=0;k<jmxAttributeTraces.size();k++){
			JMXAttributeTrace trace = new JMXAttributeTrace(jmxAttributeTraces.get(k));
			trace.setSegmentPrefixElements(resolveSegmentPrefix(trace.getSegmentPrefixElements(),on));
			// Metric name is optional so set it to AttributeName if it's not provided in config file
			if(trace.getMetricName() == null){
				trace.setMetricName(trace.getTargetAttributeName());
			} else if(trace.getMetricName().contains("{TARGET")) {
				trace.setMetricName(formatName(trace.getMetricName(), on));
			}
			if(trace.getSegment()!=null && trace.getSegment().contains("{TARGET")){
				trace.setSegment(formatName(trace.getSegment(),on));
				//-trace.setResolvedPrefix(StringHelper.append(trace.getSegmentPrefixElements(),false,trace.getSegment()));
			} else {
				trace.setResolvedPrefix(trace.getSegmentPrefixElements());
			}
			
			process(on, trace, explodedResults);
			// Cache Resolved JMXAttributeTrace for subsequent use
			tr.getResolvedAttributes().add(trace);
		}
	}

	protected void processCachedObject(ObjectName on, JMXObject cachedObject, Map<String,Object> explodedResults) {
		for(int b=0; b<cachedObject.getResolvedAttributes().size();b++){
			JMXAttributeTrace cachedTrace = cachedObject.getResolvedAttributes().get(b);
			if(cachedTrace!=null){
				process(on, cachedTrace, explodedResults);
			}
		}
	}

	protected boolean checkMBeanServerConnection(CollectionResult result) {
		try {
			if(mBeanServerConnection==null) {
				initMBeanServerConnection(true);
				//resetEnv();
			}
			traceAvailability(1);			
			return true;
		} catch (Exception e) {
			if(logErrors) error("Failed to get an MBean Server Connection for bean: " + this.getBeanName() + e);
			traceAvailability(0);
			traceDefaultsForOffline();
			result.setResultForLastCollection(CollectionResult.Result.FAILURE);
			result.setAnyException(e);
			return false; 
		}
	}

	/**
	 * 
	 */
	protected void traceAvailability(int availability) {
		if(availabilitySegment!=null) {
			//tracer.traceSticky(availability, defaultAvailabilityLabel, availabilitySegment);
			tracer.traceGauge(availability, defaultAvailabilityLabel, availabilitySegment);
		}else{
			tracer.traceGauge(availability, defaultAvailabilityLabel, getTracingNameSpace());
			//tracer.traceSticky(availability, defaultAvailabilityLabel, getTracingNameSpace());
		}
	}

	
//	/**
//	 * Executes the default MXBean collection
//	 * @throws Exception
//	 */
//	protected void collectMXBeans() throws Exception {
//		if(!traceMXBeans) return;
//		// Collect Heap and Non Heap
//		processMemory();
//		// Runtime Env
//		processRuntime();
//		// Compile Time
//		processCompiler();		
//		// Collect Class Loading
//		processClassLoading();
//		// Collect GC
//		processGCStats();
//		// Collect Memory Pools
//		processMemoryPools();
//		// Threads
//		processThreads();
//		if(mbeanQueryAttempted<=mbeanQueryAttempts) {
//			mbeanQueryAttempted++;
//		}
//	}		

//	/**
//	 * Determines if a MXBean objectName should be collected based on the include and exclude constraints. 
//	 * If the target is neither included or excluded, it will default to include.
//	 * @param target The MXBean ObjectName to test.
//	 * @return true if it should be collected, false if it should not.
//	 */
//	protected boolean shouldBeCollected(ObjectName target) {
//		if(tracedMXBeans.containsKey(target)) {
//			return tracedMXBeans.get(target);
//		} else {
//			// first examine includes
//			if(includeMXBeans.contains(target)) {
//				tracedMXBeans.put(target, true);
//				return true;
//			}
//			for(ObjectName on: includeMXBeans) {
//				if(on.apply(target)) {
//					tracedMXBeans.put(target, true);
//					return true;
//				}
//			}
//			// examine excludes
//			if(excludeMXBeans.contains(target)) {
//				tracedMXBeans.put(target, false);
//				return false;
//			}
//			for(ObjectName on: excludeMXBeans) {
//				if(on.apply(target)) {
//					tracedMXBeans.put(target, false);
//					return false;
//				}
//			}
//			// default is to include
//			tracedMXBeans.put(target, true);
//			return true;
//		}		
//	}	
	
//	/**
//	 * Collects Thread stats
//	 */
//	protected void processThreads() {
//		ObjectName threadMXBean = null;
//		long totalStartedThreads = 0;
//		long activeThreads = 0;
//		long daemonThreads = 0;
//		long nonDaemonThreads = 0;
//		Integer peakThreadCount = 0;
//		long monitorLockedThreads[] = null;
//		ThreadInfo threadInfo = null;
//		long totalBlockTime = 0;
//		long totalBlockCount = 0;
//		long totalWaitTime = 0;
//		long totalWaitCount = 0;
//		try {
//			threadMXBean = mxBeanObjectNames.get(ManagementFactory.THREAD_MXBEAN_NAME); 
//			if(threadMXBean==null) {
//				threadMXBean = new ObjectName(ManagementFactory.THREAD_MXBEAN_NAME+",*");
//				Set<ObjectName> beanSet = mBeanServerConnection.queryNames(threadMXBean, null);
//				if(beanSet.size()==1)
//					mxBeanObjectNames.put(ManagementFactory.THREAD_MXBEAN_NAME, new ObjectName(beanSet.iterator().next().getCanonicalName()));
//			}
//			if(!shouldBeCollected(threadMXBean)) return;
//			AttributeList attrs = mBeanServerConnection.getAttributes(threadMXBean, THREAD_STATS);
//			activeThreads = (Integer)getValue(attrs, THREAD_STATS[0]);
//			daemonThreads = (Integer)getValue(attrs, THREAD_STATS[1]);
//			totalStartedThreads = (Long)getValue(attrs, THREAD_STATS[2]);
//			nonDaemonThreads = activeThreads - daemonThreads;
//			peakThreadCount = (Integer)(getValue(attrs, THREAD_STATS[3]));
//
//			String[] rootSegment = StringHelper.append(tracingNameSpace,false,mxBeanSegment,"Threads");
//			tracer.traceStickyDelta(totalStartedThreads, "Threads Started (Delta)", rootSegment);
//			tracer.traceSticky(activeThreads, "Active Threads", rootSegment);
//			tracer.traceSticky(daemonThreads, "Daemon Threads", rootSegment);
//			tracer.traceSticky(nonDaemonThreads, "Non Daemon Threads", rootSegment);
//			tracer.traceSticky(peakThreadCount, "Peak Thread Count", rootSegment);
//			
//			long tmStart = System.currentTimeMillis();
//			long tmElapsed = 0;
//			
//			if(deadLockMonitor) {
//				monitorLockedThreads = (long[])mBeanServerConnection.invoke(threadMXBean, "findMonitorDeadlockedThreads", NULL_ARG, NULL_SIG);
//				if(monitorLockedThreads==null) {
//					tracer.traceSticky(0, "Monitor Deadlocked Threads", rootSegment);					
//				} else {
//					tracer.traceSticky(monitorLockedThreads.length, "Monitor Deadlocked Threads", rootSegment);
//					if(supportsThreadContention==null) {					
//						supportsThreadContention = (Boolean)mBeanServerConnection.getAttribute(threadMXBean, "ThreadContentionMonitoringSupported");
//						if(supportsThreadContention) {
//							boolean enabled = (Boolean)mBeanServerConnection.getAttribute(threadMXBean, "ThreadContentionMonitoringEnabled");
//							if(!enabled) {
//								try {
//									mBeanServerConnection.setAttribute(threadMXBean, new Attribute("ThreadContentionMonitoringEnabled", true));
//								} catch (Exception e) {
//									warn("Failed to enable ThreadContentionMonitoring for" + objectName, e);
//									supportsThreadContention = false;
//								}
//							}
//						}
//					}
//					if(supportsThreadContention) {
//						rootSegment = StringHelper.append(tracingNameSpace,false,mxBeanSegment,"Threads", "Deadlocks");						
//						CompositeData[] infos = (CompositeData[])mBeanServerConnection.invoke(threadMXBean, "getThreadInfo", new Object[]{monitorLockedThreads}, new String[]{"[J"});
//						for(CompositeData info: infos) {
//							threadInfo = ThreadInfo.from(info);
//							totalBlockCount += threadInfo.getBlockedCount();
//							totalBlockTime += threadInfo.getBlockedTime();
//							totalWaitCount += threadInfo.getWaitedCount();
//							totalWaitTime += threadInfo.getWaitedTime();
//						}
//						tracer.traceStickyDelta(totalBlockCount, "Block Count", rootSegment);
//						tracer.traceStickyDelta(totalBlockTime, "Block Time", rootSegment);
//						tracer.traceStickyDelta(totalWaitCount, "Wait Count", rootSegment);
//						tracer.traceStickyDelta(totalWaitTime, "Wait Time", rootSegment);
//					}
//				}
//				tmElapsed = System.currentTimeMillis() - tmStart;
//				tracer.trace(tmElapsed, "Deadlock Monitor Elapsed Time", StringHelper.append(tracingNameSpace,false,mxBeanSegment));
//			}
//			if(threadMonitor) {
//				rootSegment = StringHelper.append(tracingNameSpace,false,mxBeanSegment, "Threads");
//				tmStart = System.currentTimeMillis();
//				long[] allThreads = (long[])mBeanServerConnection.getAttribute(threadMXBean, "AllThreadIds");
//				CompositeData[] infos = (CompositeData[])mBeanServerConnection.invoke(threadMXBean, "getThreadInfo", new Object[]{allThreads}, new String[]{"[J"});
//				tmElapsed = System.currentTimeMillis()-tmStart;				
//				tracer.trace(tmElapsed, "Thread Monitor Elapsed Time", StringHelper.append(tracingNameSpace,false,mxBeanSegment));
//				rootSegment = StringHelper.append(tracingNameSpace,false,mxBeanSegment, "Threads", "States");
//				resetThreadStatus();
//				for(CompositeData info: infos) {
//					threadInfo = ThreadInfo.from(info);
//					incrementThreadState(threadInfo.getThreadState());
//				}	
//				for(Entry<Thread.State, Integer> entry: threadStates.entrySet()) {
//					tracer.traceSticky(entry.getValue(), entry.getKey().toString(), rootSegment);
//				}
//				
//			}
//		} catch (InstanceNotFoundException ine) {
//			if(logErrors) { warn("MXBean Collector (" + objectName + ") Could Not Locate MBean " + threadMXBean); }
//		} catch (Exception e) {
//			if(logErrors) {
//				error("Failed to process MXBean Threading Stats", e);
//			}
//		}
//	}	
//	
//	/**
//	 * Resets all thread counts.
//	 */
//	protected void resetThreadStatus() {
//		for(Thread.State state: threadStates.keySet()) {
//			threadStates.put(state, 0);
//		}
//	}	

//	/**
//	 * Increments the thread status counter.
//	 * @param state The thread state to increment.
//	 */
//	protected void incrementThreadState(Thread.State state) {
//		threadStates.put(state, threadStates.get(state)+1);
//	}
	
	/**
	 * Retreieves a named value from an attribute list.
	 * @param al
	 * @param s
	 * @return The attribute value, or null if it is not found.
	 */
	protected Object getValue(AttributeList al, String s) {
		for(int i=0; i<=al.size();i++){
			Attribute attr = (Attribute)al.get(i);
			if(s.equals(attr.getName())) return attr.getValue();	
		}
		return null;
	}	
	
//	/**
//	 * Collects memory pool stats
//	 */
//	protected void processMemoryPools() {
//		String rootSegment[] = null;
//		String poolType = null;
//		CompositeDataSupport  usage = null;		
//		try {
//			if(memoryPoolObjectNames==null) {
//				memoryPoolObjectNames = new HashMap<String, ObjectName>();
//				try {
//					memoryPoolMXBean = new ObjectName(ManagementFactory.MEMORY_POOL_MXBEAN_DOMAIN_TYPE+",*");
//					if(!shouldBeCollected(memoryPoolMXBean)) return;
//					Set<ObjectName> memoryPools = (Set<ObjectName>)mBeanServerConnection.queryNames(memoryPoolMXBean, null);
//					if(memoryPools.size()==0) {
//						if(mbeanQueryAttempted<mbeanQueryAttempts) {
//							memoryPoolObjectNames=null;
//							return;
//							// will retry
//						} 
//					}
//					for(ObjectName on: memoryPools) {
//						if(shouldBeCollected(on)) {
//							memoryPoolObjectNames.put(on.getKeyProperty("name"), on);
//						}
//					}
//				} catch (Exception e) {
//					// If an error occurs finding matching MBeans, it is probably because the target MBean was not available.
//					// Set the collection to null and retry next time.
//					memoryPoolObjectNames=null;
//					return;
//				}
//					
//			}
//			if(!shouldBeCollected(memoryPoolMXBean)) return;
//			
//			for(Entry<String, ObjectName> entry: memoryPoolObjectNames.entrySet()) {
//				poolType = (String)mBeanServerConnection.getAttribute(entry.getValue(), "Type");
//				usage = (CompositeDataSupport)mBeanServerConnection.getAttribute(entry.getValue(), "Usage");
//				rootSegment = StringHelper.append(tracingNameSpace,false,mxBeanSegment,"Memory Pools", poolType, entry.getKey());
//				for(String key: (Set<String>)usage.getCompositeType().keySet()) {
//					tracer.trace((Long)usage.get(key),key,rootSegment);
//				}
//				getPercentUsedOfCommited(usage, rootSegment);
//				getPercentUsedOfCapacity(usage, rootSegment);
//				
//			}
//		} catch (InstanceNotFoundException ine) {
//			if(logErrors) { warn("MXBean Collector (" + objectName + ") Could Not Locate MBean " + memoryPoolMXBean); }						
//		} catch (IOException ioex){
//			if(logErrors) { error("There are some issues connecting to the MBeanServer for bean " + this.getBeanName()); }
//			mBeanServerConnection=null;
//		} catch (Exception e) {
//			if(logErrors) {
//				error("Failed to process MXBean Memory Pool Stats", e);
//			}			
//		}
//	}	
//	
//	/**
//	 * Collects garbage collector stats.
//	 */
//	protected void processGCStats() {
//		String rootSegment[] = null;
//		CompositeDataSupport  usage = null;			
//		long collectionCount = 0;
//		long collectionTime = 0;
//		long elapsedTime = 0;
//		long elapsedGCTime = 0;
//		long currentTime = 0;
//		long percentGCTime = 0;
//		boolean pollGCPercent = false;
//		try {
//			if(gcObjectNames==null) {
//				gcObjectNames = new HashMap<String, ObjectName>();
//				try {
//					gcMXBean = new ObjectName(ManagementFactory.GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE+",*");
//					if(!shouldBeCollected(gcMXBean)) return;
//					Set<ObjectName> gcs = (Set<ObjectName>)mBeanServerConnection.queryNames(gcMXBean, null);
//					if(gcs.size()==0) {
//						if(mbeanQueryAttempted<mbeanQueryAttempts) {
//							gcObjectNames=null;
//							return;
//							// will retry
//						} 
//					}
//					
//					for(ObjectName on: gcs) {
//						if(shouldBeCollected(on)) {
//							gcObjectNames.put(on.getKeyProperty("name"), on);
//						}
//					}
//				} catch (Exception e) {
//					// If an error occurs finding matching MBeans, it is probably because the target MBean was not available.
//					// Set the collection to null and retry next time.
//					gcObjectNames=null;
//					return;
//				}
//			}	
//			if(!shouldBeCollected(gcMXBean)) return;
//			gCPolledCycles++;
//			if(gCPolledCycles>gCPollCycles) {
//				pollGCPercent=true;
//				gCPolledCycles=0;
//			} else {
//				pollGCPercent=false;
//			}
//			for(Entry<String, ObjectName> entry: gcObjectNames.entrySet()) {
//				rootSegment = StringHelper.append(tracingNameSpace,false,mxBeanSegment,"Garbage Collectors", entry.getKey());
//				collectionCount = (Long)mBeanServerConnection.getAttribute(entry.getValue(), "CollectionCount");
//				collectionTime = (Long)mBeanServerConnection.getAttribute(entry.getValue(), "CollectionTime");
//				currentTime = System.currentTimeMillis();
//				tracer.traceStickyDelta(collectionTime, "Collection Time (Delta)",rootSegment);
//				tracer.traceStickyDelta(collectionCount, "Collection Count (Delta)",rootSegment);
//				
//				if(pollGCPercent) {
//					if(gcTimes.containsKey(entry.getKey())) {
//						long[] times = gcTimes.get(entry.getKey());
//						elapsedTime = times[0] - currentTime;
//						elapsedGCTime = times[1] - collectionTime;
//						try {
//							percentGCTime = percent(elapsedGCTime, elapsedTime);
//							tracer.trace(percentGCTime, "% Time Spent in GC",rootSegment);
//						} catch (Exception e) {}
//					} 
//					gcTimes.put(entry.getKey(), new long[]{currentTime, collectionTime});					
//				}
//			}			
//		} catch (InstanceNotFoundException ine) {
//			if(logErrors) { warn("MXBean Collector (" + objectName + ") Could Not Locate MBean " + gcMXBean); }									
//		} catch (IOException ioex){
//			if(logErrors) { error("There are some issues connecting to the MBeanServer for bean " + this.getBeanName()); }
//			mBeanServerConnection=null;
//		} catch (Exception e) {
//			if(logErrors) {
//				error("Failed to process MXBean Garbage Collector Stats", e);
//			}			
//		}
//	}	
//	
//	/**
//	 * Collects memory stats
//	 */
//	protected void processClassLoading() {
//		ObjectName clMXBean = null;
//		AttributeList stats = null;
//		try {
//			clMXBean = mxBeanObjectNames.get(ManagementFactory.CLASS_LOADING_MXBEAN_NAME); 
//			if(clMXBean==null) {
//				clMXBean = new ObjectName(ManagementFactory.CLASS_LOADING_MXBEAN_NAME+",*");
//				Set<ObjectName> beanSet = mBeanServerConnection.queryNames(clMXBean, null);
//				if(beanSet.size()==1)
//					mxBeanObjectNames.put(ManagementFactory.CLASS_LOADING_MXBEAN_NAME, new ObjectName(beanSet.iterator().next().getCanonicalName()));				
//			}
//			if(!shouldBeCollected(clMXBean)) return;
//			String rootSegment[] = StringHelper.append(tracingNameSpace,false,mxBeanSegment,"Class Loading");
//			stats = mBeanServerConnection.getAttributes(clMXBean, CLASS_LOADING_STATS);
//			for (int i=0;i<stats.size();i++){
//				Attribute attr = (Attribute)stats.get(i);
//				if("LoadedClassCount".equals(attr.getName())) {
//					tracer.traceSticky((Integer)attr.getValue(), attr.getName(),rootSegment);
//				} else {
//					tracer.traceStickyDelta( (Long)attr.getValue(), attr.getName()+" (Delta)",rootSegment);
//				}
//			}
//		} catch (InstanceNotFoundException ine) {
//			if(logErrors) { warn("MXBean Collector (" + objectName + ") Could Not Locate MBean " + clMXBean); }			
//		} catch (IOException ioex){
//			if(logErrors) { error("There are some issues connecting to the MBeanServer for bean " + this.getBeanName()); }
//			mBeanServerConnection=null;
//		} catch (Exception e) {
//			if(logErrors) {
//				error("Failed to process MXBean Class Loading Stats", e);
//			}
//		}
//	}	
//	
//	/**
//	 * Collects JIT Compiler stats
//	 */
//	protected void processCompiler() {
//		ObjectName jitMXBean = null;
//		try {
//			if(supportsCompilerTime != null && !supportsCompilerTime) return;
//			jitMXBean = mxBeanObjectNames.get(ManagementFactory.COMPILATION_MXBEAN_NAME); 
//			if(jitMXBean==null) {
//				jitMXBean = new ObjectName(ManagementFactory.COMPILATION_MXBEAN_NAME+",*");
//				Set<ObjectName> beanSet = mBeanServerConnection.queryNames(jitMXBean, null);
//				if(beanSet.size()==1)
//					mxBeanObjectNames.put(ManagementFactory.COMPILATION_MXBEAN_NAME, new ObjectName(beanSet.iterator().next().getCanonicalName()));				
//			}
//			if(!shouldBeCollected(jitMXBean)) return;			
//			if(supportsCompilerTime==null) {
//				supportsCompilerTime = (Boolean)mBeanServerConnection.getAttribute(jitMXBean, "CompilationTimeMonitoringSupported");
//			}
//			if(!supportsCompilerTime) return;
//			String rootSegment[] = StringHelper.append(tracingNameSpace,false,mxBeanSegment,"JIT Compiler");
//			long totalComplilationTime = (Long)mBeanServerConnection.getAttribute(jitMXBean, "TotalCompilationTime");
//			tracer.traceStickyDelta(totalComplilationTime, "Compile Time (Delta)", rootSegment);
//			tracer.traceSticky(totalComplilationTime, "Total Compile Time", rootSegment);
//		} catch (InstanceNotFoundException ine) {
//			if(logErrors) { warn("MXBean Collector (" + objectName + ") Could Not Locate MBean " + jitMXBean); }			
//		} catch (IOException ioex){
//			if(logErrors) { error("There are some issues connecting to the MBeanServer for bean " + this.getBeanName()); }
//			mBeanServerConnection=null;
//		} catch (Exception e) {
//			if(logErrors) {
//				error("Failed to process MXBean Class Loading Stats", e);
//			}
//		}
//	}		
//	
//	/**
//	 * One time collection of runtime MXBean stats
//	 */
//	protected void processRuntime() {
//		ObjectName runTimeMXBean = null;
//		try {
//			if(runtimeCollected) return;
//			runTimeMXBean = mxBeanObjectNames.get(ManagementFactory.RUNTIME_MXBEAN_NAME); 
//			if(runTimeMXBean==null) {
//				runTimeMXBean = new ObjectName(ManagementFactory.RUNTIME_MXBEAN_NAME+",*");
//				Set<ObjectName> beanSet = mBeanServerConnection.queryNames(runTimeMXBean, null);
//				if(beanSet.size()==1)
//					mxBeanObjectNames.put(ManagementFactory.RUNTIME_MXBEAN_NAME, new ObjectName(beanSet.iterator().next().getCanonicalName()));				
//			}
//			if(!shouldBeCollected(runTimeMXBean)) {
//				runtimeCollected = true;
//				return;			
//			}
//			String[] rootSegment = StringHelper.append(tracingNameSpace,false,mxBeanSegment,"Runtime");
//			long startTime = (Long)mBeanServerConnection.getAttribute(runTimeMXBean, "StartTime");
//			String[] inputArguments = (String[])mBeanServerConnection.getAttribute(runTimeMXBean, "InputArguments");
//			StringBuilder buff = new StringBuilder();
//			for(String s: inputArguments) {
//				buff.append(s).append("\n");
//			}
//			
//			tracer.trace(new Date(startTime), "Start Time", rootSegment);
//			tracer.trace(buff.toString(), "JVM Input Arguments",rootSegment);
//			runtimeCollected = true;
//		} catch (InstanceNotFoundException ine) {
//			if(logErrors) { warn("MXBean Collector (" + objectName + ") Could Not Locate MBean " + runTimeMXBean); }
//		} catch (IOException ioex){
//			if(logErrors) { error("There are some issues connecting to the MBeanServer for bean " + this.getBeanName()); }
//			mBeanServerConnection=null;
//		} catch (Exception e) {
//			if(logErrors) {
//				error("Failed to process MXBean Runtime Stats", e);
//			}
//			runtimeCollected = false;
//		}
//	}	
	
//	/**
//	 * Collects memory stats
//	 */
//	protected void processMemory() {
//		ObjectName memoryMXBean = null;
//		try {
//			String rootSegment[] = null;			
//			memoryMXBean = mxBeanObjectNames.get(ManagementFactory.MEMORY_MXBEAN_NAME); 
//			if(memoryMXBean==null) {
//				memoryMXBean = new ObjectName(ManagementFactory.MEMORY_MXBEAN_NAME+",*");
//				Set<ObjectName> beanSet = mBeanServerConnection.queryNames(memoryMXBean, null);
//				if(beanSet.size()==1)
//					mxBeanObjectNames.put(ManagementFactory.MEMORY_MXBEAN_NAME, new ObjectName(beanSet.iterator().next().getCanonicalName()));				
//			}
//			if(!shouldBeCollected(memoryMXBean)) return;
//			
//			CompositeDataSupport heap = (CompositeDataSupport) mBeanServerConnection.getAttribute(memoryMXBean, "HeapMemoryUsage");
//			rootSegment = StringHelper.append(tracingNameSpace,false,mxBeanSegment,"Memory", "Heap Memory Usage");
//			for(String key: (Set<String>)heap.getCompositeType().keySet()) {
//				tracer.trace((Long)heap.get(key),key,rootSegment);
//			}
//			getPercentUsedOfCommited(heap, rootSegment);
//			getPercentUsedOfCapacity(heap, rootSegment);
//			
//			CompositeDataSupport nonHeap = (CompositeDataSupport) mBeanServerConnection.getAttribute(memoryMXBean, "NonHeapMemoryUsage");
//			rootSegment = StringHelper.append(tracingNameSpace,false,mxBeanSegment, "Memory", "Non Heap Memory Usage");
//			for(String key: (Set<String>)nonHeap.getCompositeType().keySet()) {
//				tracer.trace((Long)heap.get(key),key,rootSegment);
//			}
//			getPercentUsedOfCommited(nonHeap, rootSegment);
//			getPercentUsedOfCapacity(nonHeap, rootSegment);
//			
//			rootSegment = StringHelper.append(tracingNameSpace,false,mxBeanSegment, "Memory");
//			tracer.trace((Integer)mBeanServerConnection.getAttribute(memoryMXBean, "ObjectPendingFinalizationCount"), "Objects Pending Finalization",rootSegment);
//		} catch (InstanceNotFoundException ine) {
//			if(logErrors) { warn("MXBean Collector for bean collector " + this.getBeanName() + " could Not Locate MBean " + memoryMXBean); }												
//		} catch (IOException ioex){
//			if(logErrors) { error("There are some issues connecting to the MBeanServer for bean " + this.getBeanName()); }
//			mBeanServerConnection=null;
//		}catch (Exception e) {
//			if(logErrors) {
//				error("Failed to process MXBean Memory Stats", e);
//			}
//		}
//	}	
//	
//	/**
//	 * Calculates the percentage of commited memory in use.
//	 * @param cd A CompositeDataSupport that can generate a MemoryUsage.
//	 * @param rootSegment The root segment for the tracing. Will not trace if this is zero length.
//	 * @return the percentage of commited memory in use
//	 */
//	protected long getPercentUsedOfCommited(CompositeDataSupport cd, String...rootSegment) {
//		long value = -1;
//		MemoryUsage memoryUsage = MemoryUsage.from(cd);
//		try {
//			value = percent(memoryUsage.getUsed(), memoryUsage.getCommitted());
//			if(rootSegment.length>0) {
//				tracer.trace(value,"Used %",rootSegment);
//			}
//		} catch (Exception e) {
//			value = -1;
//		}		
//		return value;
//	}
//	
//	/**
//	 * Helper method to generate a percentage.
//	 * @param part
//	 * @param all
//	 * @return A percentage value.
//	 */
//	protected static long percent(float part, float all) {
//		return (long)((part)/all*100);
//	}
//	
//	/**
//	 * Calculates the percentage of total capacity memory in use.
//	 * @param cd A CompositeDataSupport that can generate a MemoryUsage.
//	 * @param rootSegment The root segment for the tracing. Will not trace if this is zero length.
//	 * @return percentage of total capacity memory in use.
//	 */
//	protected long getPercentUsedOfCapacity(CompositeDataSupport cd, String...rootSegment) {
//		long value = -1;
//		MemoryUsage memoryUsage = MemoryUsage.from(cd);
//		try {
//			value = percent(memoryUsage.getUsed(), memoryUsage.getMax());
//			if(rootSegment.length>0) {
//				tracer.trace(value, "Capacity %", rootSegment);
//			}
//			
//		} catch (Exception e) {
//			value = -1;
//		}		
//		return value;
//	}
	
	
	/**
	 * Tokenizes segment prefix elements.
	 * Should be called on start() to ensure that the sequencing of attribute sets does not impact tokens.
	 */
	protected String[] resolveSegmentPrefix(String[] unresolvedSegmentPrefix, ObjectName on) {
		if(unresolvedSegmentPrefix==null || unresolvedSegmentPrefix.length < 1) {
			unresolvedSegmentPrefix = new String[0];
		} else {			
			for(int i = 0; i < unresolvedSegmentPrefix.length; i++) {
				unresolvedSegmentPrefix[i] = formatName(unresolvedSegmentPrefix[i],on);
			}
		}
		return unresolvedSegmentPrefix;		
	}	
	
	/**
	 * Reset Processing Status for each resolved Object to false
	 */
	public void resetProcessingFlag(){
		if(resolvedJMXObjects!=null && !resolvedJMXObjects.isEmpty()){
			Iterator keys = resolvedJMXObjects.keySet().iterator();
			while(keys.hasNext()){
				String tempKey = (String)keys.next();
				JMXObject tempObject = (JMXObject)resolvedJMXObjects.get(tempKey);
				tempObject.setProcessed(false);
				resolvedJMXObjects.put(tempKey,tempObject);
			}
		}
	}
	
	@ManagedAttribute
	public String getCollectorVersion() {
		return "JMXCollector v. "+JMX_COLLECTOR_VERSION;
	}

	/**
	 * 
	 */
	public void startCollector() throws CollectorException {
		try{
			initMBeanServerConnection(false);
		}catch(Exception ex){
			throw new CollectorException("An error occured while creating an MBean Server connection", ex);
		}
	}
	
	

	/**
	 * 
	 */
	public void stopCollector(){
		if(mBeanServerConnection!=null){
			mBeanServerConnection = null;
		}
	}	
	
	/**
	 * @param jndiProperties the jndiProperties to set
	 */
	public void setJndiProperties(Properties jndiProperties) {
		this.properties = jndiProperties;
	}

	/**
	 * @return the connectionFactory
	 */
	public IMBeanServerConnectionFactory getConnectionFactory() {
		return connectionFactory;
	}

	/**
	 * @param connectionFactory the connectionFactory to set
	 */
	public void setConnectionFactory(IMBeanServerConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}


	/**
	 * @return the availabilityMBean
	 */
	@ManagedAttribute
	public String getAvailabilityMBean() {
		if(availabilityMBean!=null)
			return availabilityMBean.toString();
		else
			return "";
	}

	/**
	 * @param availabilityMBean the availabilityMBean to set
	 */
	public void setAvailabilityMBean(String availabilityMBean) {
		try {
			this.availabilityMBean = ObjectName.getInstance(availabilityMBean);
		} catch (MalformedObjectNameException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @return the availabilityAttribute
	 */
	@ManagedAttribute
	public String getAvailabilityAttribute() {
		return availabilityAttribute;
	}

	/**
	 * @param availabilityAttribute the availabilityAttribute to set
	 */
	public void setAvailabilityAttribute(String availabilityAttribute) {
		this.availabilityAttribute = availabilityAttribute;
	}

	/**
	 * @return the availabilitySegment
	 */
	@ManagedAttribute
	public String[] getAvailabilitySegment() {
		return (String[])availabilitySegment.clone();
	}

	/**
	 * @param availabilitySegment the availabilitySegment to set
	 */
	public void setAvailabilitySegment(String[] availabilitySegment) {
		this.availabilitySegment = availabilitySegment;
	}

	/**
	 * @return the jmxObjects
	 */
	public List<JMXObject> getJmxObjects() {
		return jmxObjects;
	}

	/**
	 * @param jmxObjects the jmxObjects to set
	 */
	public void setJmxObjects(List<JMXObject> jmxObjects) {
		this.jmxObjects = jmxObjects;
	}
//
//	/**
//	 * @return the traceMXBeans
//	 */
//	@ManagedAttribute
//	public boolean getTraceMXBeans() {
//		return traceMXBeans;
//	}
//
//	/**
//	 * @param traceMXBeans the traceMXBeans to set
//	 */
//	public void setTraceMXBeans(boolean traceMXBeans) {
//		this.traceMXBeans = traceMXBeans;
//	}

	/**
	 * @return the mXBeanSegment
	 */
	@ManagedAttribute
	public String getMxBeanSegment() {
		return mxBeanSegment;
	}

	/**
	 * @param beanSegment the mXBeanSegment to set
	 */
	public void setMxBeanSegment(String beanSegment) {
		mxBeanSegment = beanSegment;
	}

//	/**
//	 * @return the deadLockMonitor
//	 */
//	@ManagedAttribute
//	public boolean getDeadLockMonitor() {
//		return deadLockMonitor;
//	}
//
//	/**
//	 * @param deadLockMonitor the deadLockMonitor to set
//	 */
//	public void setDeadLockMonitor(boolean deadLockMonitor) {
//		this.deadLockMonitor = deadLockMonitor;
//	}


//	/**
//	 * Returns the virtual host locator.
//	 * @return the virtual host locator.
//	 */
//	@ManagedAttribute
//	public String getVirtualHostLocator() {
//		return virtualHostLocator;
//	}
//
//
//	/**
//	 * Sets the virtual host locator
//	 * @param virtualTracerHost
//	 */
//	public void setVirtualHostLocator(String virtualHostLocator) {
//		this.virtualHostLocator = virtualHostLocator;
//	}
//
//
//	/**
//	 * Returns the virtual agent locator.
//	 * @return the virtual agent locator.
//	 */
//	@ManagedAttribute
//	public String getVirtualAgentLocator() {
//		return virtualAgentLocator;
//	}
//
//
//	/**
//	 * Sets the virtual agent locator.
//	 * @param virtualTracerAgent
//	 */
//	public void setVirtualAgentLocator(String virtualAgentLocator) {
//		this.virtualAgentLocator = virtualAgentLocator;
//	}
//
//	/**
//	 * Returns the virtual host for this target MBeanServer
//	 * @return the virtual host for this target MBeanServer
//	 */
//	@ManagedOperation
//	public String getVirtualHost() {
//		return virtualHost;
//	}
//
//
//	/**
//	 * Returns the virtual agent for this target MBeanServer
//	 * @return the virtual agent for this target MBeanServer
//	 */
//	@ManagedOperation
//	public String getVirtualAgent() {
//		return virtualAgent;
//	}
	
}
