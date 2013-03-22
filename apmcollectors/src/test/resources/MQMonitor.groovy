            /* 
                Provided Values as bindings are:
                ================================
                collector: A reference to the collector
                tracer: The opentrace tracer
                log: The collector's log4j logger
                cache: the collector cache
                cachettl: The cache entry default time to live
                pcfFactory: The factory that provides PCF Agents
                qMgrName: The configured queue manager name
                iscope: The one off iscope tracer for strings
            */
            import com.ibm.mq.constants.MQConstants
            import java.text.SimpleDateFormat
            import com.ibm.mq.pcf.*
            import net.sf.ehcache.*;
            
            cachettl = collector.cacheEntryTimeToLive;
            
            def int[] mqiacf_attrs = [
                                    CMQC.MQCA_Q_NAME, CMQC.MQIA_CURRENT_Q_DEPTH,  
                                    CMQC.MQIA_OPEN_INPUT_COUNT, CMQC.MQIA_OPEN_OUTPUT_COUNT, 
                                    CMQCFC.MQIACF_UNCOMMITTED_MSGS,  CMQCFC.MQIACF_OLDEST_MSG_AGE
            ];
            
            public long time(Closure cl) {
            	long start = System.currentTimeMillis();
            	cl.call();
            	return System.currentTimeMillis()-start;
            }
            
            public void time(Closure cl, String metricName, String...namespace) {
            	long start = System.currentTimeMillis();
            	cl.call();            	
            	tracer.traceSticky(System.currentTimeMillis()-start, metricName, namespace);            	
            }
            
            public String getHexString(byte[] b) throws Exception {
                  String result = "";
                  for (int i=0; i < b.length; i++) {
                    result +=
                          Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
                  }
                  return result;
            }
            
				    public Object[] resolveServiceStatus(int status){
		            def NULL_STATUS = 0;
		            def OK = 1;
		            def WARN = 2;
		            def CRITICAL = 3;
		            Map statues = [:]
				        statues.put(0, ['STOPPED', CRITICAL] as Object[]);
				        statues.put(1, ['STARTING', WARN] as Object[]);
				        statues.put(2, ['RUNNING', OK] as Object[]);
				        statues.put(3, ['STOPPING', WARN] as Object[]);
				        statues.put(4, ['RETRYING', WARN] as Object[]);
				    	
				        return statues.get(status);
				    }
				    
				    public Object[] resolveQMStatus(int status){
		            def NULL_STATUS = 0;
		            def OK = 1;
		            def WARN = 2;
		            def CRITICAL = 3;
		            Map statues = [:]
				        statues.put(1, ['STARTING', WARN] as Object[]);
				        statues.put(2, ['RUNNING', OK] as Object[]);
				        statues.put(3, ['QUIESCING', WARN] as Object[]);
				        return statues.get(status);
				    }
				    
            
            
				    public Object[] resolveChStatus(int status){
		            def NULL_STATUS = 0;
		            def OK = 1;
		            def WARN = 2;
		            def CRITICAL = 3;
		            Map statues = [:]
				        statues.put(0, ['INACTIVE', CRITICAL] as Object[]);
				        statues.put(1, ['BINDING', CRITICAL] as Object[]);
				        statues.put(2, ['STARTING', WARN] as Object[]);
				        statues.put(3, ['RUNNING', OK] as Object[]);
				        statues.put(4, ['STOPPING', WARN] as Object[]);
				        statues.put(5, ['RETRYING', CRITICAL] as Object[]);
				        statues.put(6, ['STOPPED', CRITICAL] as Object[]);
				        statues.put(7, ['REQUESTING', WARN] as Object[]);
				        statues.put(8, ['PAUSED', WARN] as Object[]);
				        statues.put(13, ['INITIALIZING', WARN] as Object[]);
				    	
				        return statues.get(status);
				    }
            
				    public String resolveChType(int type){
	            Map channelTypes = [:]
	            channelTypes.put(1, "Sender");
	            channelTypes.put(2, "Server");
	            channelTypes.put(3, "Receiver");
	            channelTypes.put(4, "Requester");
	            channelTypes.put(6, "Client Connection");
	            channelTypes.put(7, "Server Connection");
	            channelTypes.put(8, "Cluster Receiver");
	            channelTypes.put(9, "Cluster Sender");
				        return channelTypes.get(type);
				    }
            
                        
            public int perc(part, total) {
            	if(part<1 || total <1) return 0;
            	return part/total*100;
            }
            
            public void traceQueueMetrics(q, it, queueMaxDepths, matchingQueues, namespace) {            	
            	if(matchingQueues==null || matchingQueues.contains(q)) {
                  tracer.traceSticky(it.get(CMQC.MQIA_CURRENT_Q_DEPTH), "Queue Depth", namespace);
                  try { tracer.traceSticky(perc(it.get(CMQC.MQIA_CURRENT_Q_DEPTH), queueMaxDepths.get(q.trim())), "Percent Full", namespace); } catch (e) {e.printStackTrace(System.err);}
                  tracer.traceSticky(it.get(CMQC.MQIA_OPEN_INPUT_COUNT), "Open Input Count", namespace);
                  tracer.traceSticky(it.get(CMQC.MQIA_OPEN_OUTPUT_COUNT), "Open Output Count", namespace);
                  tracer.traceSticky(it.get(CMQCFC.MQIACF_UNCOMMITTED_MSGS), "Uncommitted Messages", namespace);
                  if(it.get(CMQCFC.MQIACF_OLDEST_MSG_AGE)>-1) {
                  	tracer.traceSticky(it.get(CMQCFC.MQIACF_OLDEST_MSG_AGE), "Age of Oldest Message (s)", namespace);
                  }
            	}            	
            }
            
            public List request(byName, agent, type, parameters) {
                def responses = [];
                def PCFMessage request = new PCFMessage(type);
                parameters.each() { name, value ->
                    request.addParameter(name, value);
                }
                try {
	                agent.send(request).each() {
	                    def responseValues = [:];
	                    it.getParameters().toList().each() { pcfParam ->
	                        def value = pcfParam.getValue();
	                        if(value instanceof String) value = value.trim();
	                        responseValues.put(byName ? pcfParam.getParameterName() : pcfParam.getParameter(), value);
	                    }
	                    responses.add(responseValues);
	                }
	                return responses;
	              } catch (e) {
	              		println "Exception Request Type:${type}, Parameters:\n${parameters}";
	              		e.printStackTrace(System.err);
	              		throw e;
	              }
            }
            
            
            def byte[] emptyConn = new byte[24];
            
            def pcf = null;
            def qManager = null;
            def matchingQueues = new HashSet();
            def queueMaxDepths = [:];
            def channelNames = null;
            def cachePrefix = pcfFactory.getObjectName().toString() + "-";
            def channelNamesCacheKey = cachePrefix + "ChannelNames";
            def queueNamesCacheKey = cachePrefix + "QueueNames";
            def topicNamesCacheKey = cachePrefix + "TopicNames";
            def queueMaxDepthCacheKey = cachePrefix + "MaxQDepths";            
            long start = System.currentTimeMillis();
            try {
                pcf = pcfFactory.newResource();
                long connTime = System.currentTimeMillis();                
                qManager = pcf.getQManagerName();
                iscope.recordDataPoint(qManager, "WebSphere MQ", qManager, "Name");	  
                tracer.traceSticky(System.currentTimeMillis()-connTime, "PCF Connect Time (ms)", "WebSphere MQ", qManager, "Monitor");
                // =================================================
                // Get Channel Stats
                // =================================================
                time({                
                	int channelCount = 0;
	                request(false, pcf, MQConstants.MQCMD_INQUIRE_CHANNEL_STATUS, [(MQConstants.MQCACH_CHANNEL_NAME):"*"]).each() {
	                	channelCount++;
	                	channelName = it.get(MQConstants.MQCACH_CHANNEL_NAME).trim();
	                	channelType = resolveChType(it.get(MQConstants.MQIACH_CHANNEL_TYPE));
	                	channelStatus = it.get(MQConstants.MQIACH_CHANNEL_STATUS);
	                	decodedChannelStatus = resolveChStatus(channelStatus);
	                	connectionName = it.get(MQConstants.MQCACH_CONNECTION_NAME);
	                	xmitQueueName = it.get(MQConstants.MQCACH_XMIT_Q_NAME);
	                	tracer.traceSticky(channelStatus, "Status Code", "WebSphere MQ", qManager, "Channels", channelType, channelName);
	                	tracer.traceSticky(decodedChannelStatus[1], "Status", "WebSphere MQ", qManager, "Channels", channelType, channelName);
	                	iscope.recordDataPoint(decodedChannelStatus[0], "WebSphere MQ", qManager, "Channels", channelType, channelName, "Status Name");	                	
	                	tracer.traceStickyDelta(it.get(MQConstants.MQIACH_BUFFERS_RCVD), "Buffers Received", "WebSphere MQ", qManager, "Channels", channelType, channelName);
	                	tracer.traceStickyDelta(it.get(MQConstants.MQIACH_BUFFERS_SENT), "Buffers Sent", "WebSphere MQ", qManager, "Channels", channelType, channelName);
	                	tracer.traceStickyDelta(it.get(MQConstants.MQIACH_BYTES_RCVD), "Bytes Received", "WebSphere MQ", qManager, "Channels", channelType, channelName);
	                	tracer.traceStickyDelta(it.get(MQConstants.MQIACH_BYTES_SENT), "Bytes Sent", "WebSphere MQ", qManager, "Channels", channelType, channelName);
	                }
	                tracer.traceSticky(channelCount, "Channel Count", "WebSphere MQ", qManager, "Channels");    			                	               	
	              }, "Channel Metrics Time (ms)", "WebSphere MQ", qManager, "Monitor");                	
                
                // =================================================
                // Get Queue Names
                // =================================================
                time({
	                element = cache.get(queueNamesCacheKey);
	                if(element!=null) {
	                	queueNames = element.getObjectValue();
	                } else {
		                queueNames = request(false, pcf, CMQCFC.MQCMD_INQUIRE_Q_NAMES, [(CMQC.MQCA_Q_NAME):"*", (CMQC.MQIA_Q_TYPE):CMQC.MQQT_LOCAL]).get(0).get(CMQCFC.MQCACF_Q_NAMES);      
		                for(i in 0..queueNames.length-1) {
		                    queueNames[i] = queueNames[i].trim();
		                    if(!queueNames[i].startsWith("SYSTEM.") && !queueNames[i].startsWith("AMQ.")) {
		                    	matchingQueues.add(queueNames[i]);
		                    }
		                }
		                cache.put(new Element(queueNamesCacheKey, queueNames, true, 0, cachettl));
	                }
	              }, "Fetch Queue Names (ms)", "WebSphere MQ", qManager, "Monitor");                	
                // =================================================
                // Get Max Queue Depths
                // =================================================								
								time({	                
	                element = cache.get(queueMaxDepthCacheKey);
	                if(element!=null) {                	
	                	queueMaxDepths = element.getValue();
	                } else {	                	
                		queueMaxDepths.clear();
		                request(false, pcf, CMQCFC.MQCMD_INQUIRE_Q, [(CMQC.MQCA_Q_NAME):"*", (CMQC.MQIA_Q_TYPE):CMQC.MQQT_LOCAL, (CMQCFC.MQIACF_Q_ATTRS):[CMQC.MQCA_Q_NAME, CMQC.MQIA_MAX_Q_DEPTH] as int[]]).each() {
		                    queueMaxDepths.put(it.get(CMQC.MQCA_Q_NAME).trim(), it.get(CMQC.MQIA_MAX_Q_DEPTH));
		                }  
		                cache.put(new Element(queueMaxDepthCacheKey, queueMaxDepths, true, 0, cachettl));      
		                tracer.traceSticky(queueMaxDepths.size(), "Queue Count", "WebSphere MQ", qManager, "Queues");    			                	
	                }
	              }, "Fetch Max QDepths (ms)", "WebSphere MQ", qManager, "Monitor");	              
                // =================================================
                // Get Topic Names
                // =================================================
                time({                
	                element = cache.get(topicNamesCacheKey);
	                if(element!=null) {
	                	topicNames = element.getObjectValue();
	                } else {
		                topicNames = request(false, pcf, CMQCFC.MQCMD_INQUIRE_TOPIC_NAMES, [(CMQC.MQCA_TOPIC_NAME):"*"]).get(0).get(CMQCFC.MQCACF_TOPIC_NAMES);
		                for(i in 0..topicNames.length-1) {
		                    topicNames[i] = topicNames[i].trim();
		                }                		
		                cache.put(new Element(topicNamesCacheKey, topicNames, true, 0, cachettl));	 
		                tracer.traceSticky(topicNames.length, "Topic Count", "WebSphere MQ", qManager, "Topics");    			                	               	
	                }                
								}, "Fetch Topic Names (ms)", "WebSphere MQ", qManager, "Monitor");
                // =================================================
                // Get Queue Metrics
                // =================================================
                time({
			            request(false, pcf, CMQCFC.MQCMD_INQUIRE_Q_STATUS, [(CMQC.MQCA_Q_NAME):"*", (CMQC.MQIA_Q_TYPE):CMQC.MQQT_LOCAL]).each() {     
			            	def q = it.get(CMQC.MQCA_Q_NAME).trim();   
			            	def namespace = ["WebSphere MQ", qManager, "Queues", q] as String[];    	
			            	traceQueueMetrics(q, it, queueMaxDepths, matchingQueues, namespace);
			            }
			          }, "Queue Metrics Time (ms)", "WebSphere MQ", qManager, "Monitor");
                // =================================================
                // Get Topic Metrics
                // =================================================
                int totalSubscribers = 0;
                int totalPublishers = 0;
                time({
			            request(false, pcf, CMQCFC.MQCMD_INQUIRE_TOPIC_STATUS, [(CMQC.MQCA_TOPIC_STRING):"#"]).each() {     
			            	int totalMsgsPublished = 0;
			            	int totalMsgsReceived = 0;
			            	String topicName = it.get(CMQC.MQCA_TOPIC_STRING).trim();
			            	String tName = topicName.replace(" ", "").replace("/", ".");
			            	if(tName.isEmpty()) tName = "Blank";			            	
			            	int publishers = it.get(CMQC.MQIA_PUB_COUNT);
			            	totalPublishers += publishers;
			            	int subscribers = it.get(CMQC.MQIA_SUB_COUNT);
			            	totalSubscribers += subscribers;
			            	tracer.traceSticky(publishers, "Publishers", "WebSphere MQ", qManager, "Topics", tName);
			            	tracer.traceSticky(subscribers, "Subscribers", "WebSphere MQ", qManager, "Topics", tName);		
			            }
			            tracer.traceSticky(totalSubscribers, "Total Subscribers", "WebSphere MQ", qManager, "Topics");		
			            //println "${qManager} Total Subscribers:${totalSubscribers}";
			            tracer.traceSticky(totalPublishers, "Total Publishers", "WebSphere MQ", qManager, "Topics");		
			            //println "${qManager} Total Publishers:${totalPublishers}";
			            // ===============================================================
			            //	Topic Subscriber Metrics
			            // ===============================================================
			            df = new java.text.SimpleDateFormat("yyyy-MM-ddHH:mm:ss");
			            if(totalSubscribers>0) { 
			            	try {
					            request(false, pcf, CMQCFC.MQCMD_INQUIRE_TOPIC_STATUS, [(CMQC.MQCA_TOPIC_STRING):"#", (CMQCFC.MQIACF_TOPIC_STATUS_TYPE):CMQCFC.MQIACF_TOPIC_SUB]).each() {     
					            	String topicName = it.get(CMQC.MQCA_TOPIC_STRING).trim();
					            	String tName = topicName.replace(" ", "").replace("/", ".");
					            	byte[] subId = it.get(CMQCFC.MQBACF_SUB_ID);
					            	byte[] connId = it.get(CMQCFC.MQBACF_CONNECTION_ID);					            	
					            	String subUserId = it.get(CMQCFC.MQCACF_SUB_USER_ID);
					            	String durable = it.get(CMQCFC.MQIACF_DURABLE_SUBSCRIPTION)==1 ? "Durable" : "NonDurable";
					            	//println "[${tName}]/[${subUserId}]  Sub:${getHexString(subId)}  Conn:${getHexString(connId)}"
					            	long timeSinceResume = -1;
					            	try { timeSinceResume = df.parse(it.get(CMQC.MQCA_RESUME_DATE) +  it.get(CMQC.MQCA_RESUME_TIME)).getTime()/1000; } catch (e) {}
					            	long timeSinceLastMessage = -1;
					            	try { timeSinceLastMessage = df.parse(it.get(CMQCFC.MQCACF_LAST_PUB_DATE) +  it.get(CMQCFC.MQCACF_LAST_PUB_TIME)).getTime()/1000; } catch (e) {}
					            	long msgCount = Long.parseLong(it.get(CMQCFC.MQIACF_MESSAGE_COUNT).toString());
												if(tName.isEmpty()) tName = "Blank";
												tracer.traceStickyDelta(msgCount, "Put Messages", "WebSphere MQ", qManager, "Topics", tName);			            	
												//tracer.traceSticky(timeSinceResume, "Last Resume Age (s)", "WebSphere MQ", qManager, "Topics", tName);			            	
												//tracer.traceSticky(timeSinceLastMessage, "Last Message Age (s)", "WebSphere MQ", qManager, "Topics", tName);		
												request(false, pcf, CMQCFC.MQCMD_INQUIRE_SUBSCRIPTION, [(CMQCFC.MQBACF_SUB_ID):subId]).each() {  
													if(it.get(CMQCFC.MQIACF_DURABLE_SUBSCRIPTION)==1) {
														String durableQueue = it.get(CMQCFC.MQCACF_DESTINATION);
														String subscriptionName = it.get(CMQCFC.MQCACF_SUB_NAME).replace(":", ";").replace("/", "\\").replace(" ", "");														
								            request(false, pcf, CMQCFC.MQCMD_INQUIRE_Q_STATUS, [(CMQC.MQCA_Q_NAME):durableQueue, (CMQC.MQIA_Q_TYPE):CMQC.MQQT_LOCAL]).each() {     
								            	def q = it.get(CMQC.MQCA_Q_NAME).trim();   
								            	def namespace = ["WebSphere MQ", qManager, "Topics", tName, "Subscriptions", subscriptionName, "Managed Queue", durableQueue] as String[];    	
								            	traceQueueMetrics(durableQueue, it, queueMaxDepths, null, namespace);
								            }														
													}
												}	      
												request(false, pcf, CMQCFC.MQCMD_INQUIRE_SUB_STATUS, [(CMQCFC.MQBACF_SUB_ID):subId]).each() {     
													String subscriptionName = it.get(CMQCFC.MQCACF_SUB_NAME).replace(":", ";").replace("/", "\\").replace(" ", "");														
													msgCount = it.get(CMQCFC.MQIACF_MESSAGE_COUNT);
													tracer.traceStickyDelta(msgCount, "Put Messages", "WebSphere MQ", qManager, "Topics", tName, "Subscriptions", subscriptionName);			            	
												}
												
												
												/*
			            request(false, pcf, CMQCFC.MQCMD_INQUIRE_Q_STATUS, [(CMQC.MQCA_Q_NAME):"*", (CMQC.MQIA_Q_TYPE):CMQC.MQQT_LOCAL]).each() {     
			            	def q = it.get(CMQC.MQCA_Q_NAME).trim();   
			            	def namespace = ["WebSphere MQ", qManager, "Queues", q] as String[];    	
			            	traceQueueMetrics(q, it, matchingQueues, namespace);
			            }
												
												
												*/
												
					            }
					           } catch (e) {
					           	System.err.println("\n\t!!!!!!!!!!!!!\n\t${e}\n\t!!!!!!!!!!!!!\n");
					           	e.printStackTrace(System.err);	
					           }
				          }
			            // ===============================================================
			            //	Topic Subscriber Metrics
			            // ===============================================================			            
									if(totalPublishers>0) { 
										try {      
					            request(false, pcf, CMQCFC.MQCMD_INQUIRE_TOPIC_STATUS, [(CMQC.MQCA_TOPIC_STRING):"*", (CMQCFC.MQIACF_TOPIC_STATUS_TYPE):CMQCFC.MQIACF_TOPIC_PUB]).each() {     					            	
					            	String topicName = it.get(CMQC.MQCA_TOPIC_STRING).trim();
					            	String tName = topicName.replace(" ", "").replace("/", ".");
					            	byte[] connId = it.get(CMQCFC.MQBACF_CONNECTION_ID);
					            	long timeSinceLastPub = -1;
					            	try { timeSinceLastPub = df.parse(it.get(CMQCFC.MQCACF_LAST_PUB_DATE) +  it.get(CMQCFC.MQCACF_LAST_PUB_TIME)).getTime()/1000; } catch (e) {}
					            	long msgCount = Long.parseLong(it.get(CMQCFC.MQIACF_MESSAGE_COUNT).toString());
												if(tName.isEmpty()) tName = "Blank";												
												tracer.traceStickyDelta(msgCount, "Sent Messages", "WebSphere MQ", qManager, "Topics", tName);			            	
												//tracer.traceSticky(timeSinceLastMessage, "Time Since Last Pub (s)", "WebSphere MQ", qManager, "Topics", tName);			            	
					            }
					          } catch (e) {}
					       }
			          }, "Topic Metrics Time (ms)", "WebSphere MQ", qManager, "Monitor");
		            // ===============================================================
		            //	Queue Manager Status and Metrics
		            // ===============================================================			    
		            time({
									try {      
				            request(true, pcf, CMQCFC.MQCMD_INQUIRE_Q_MGR_STATUS, []).each() {   
				            	decodedQMStatus = resolveQMStatus(it.get("MQIACF_Q_MGR_STATUS"));  
				            	commandServerStatus = resolveServiceStatus(it.get("MQIACF_CMD_SERVER_STATUS"));  
				            	chInitStatus = resolveServiceStatus(it.get("MQIACF_CHINIT_STATUS"));  
											tracer.traceSticky(decodedQMStatus[1], "Status", "WebSphere MQ", qManager, "Queue Manager");
											tracer.traceSticky(commandServerStatus[1], "Status", "WebSphere MQ", qManager, "Queue Manager", "Command Server");
											tracer.traceSticky(chInitStatus[1], "Status", "WebSphere MQ", qManager, "Queue Manager", "Channel Initiator");
											tracer.traceSticky(it.get("MQIACF_CONNECTION_COUNT"), "Connection Count", "WebSphere MQ", qManager, "Queue Manager");
											
											iscope.recordDataPoint(decodedQMStatus[0], "WebSphere MQ", qManager, "Queue Manager", "Status Name");
											iscope.recordDataPoint(commandServerStatus[0], "WebSphere MQ", qManager, "Queue Manager", "Command Server", "Status Name");
											iscope.recordDataPoint(chInitStatus[0], "WebSphere MQ", qManager, "Queue Manager", "Channel Initiator", "Status Name");
											
				            }
				          } catch (e) {}
			          }, "Queue Manager Time (ms)", "WebSphere MQ", qManager, "Monitor");
			          
            
						long elapsed = System.currentTimeMillis() - start;            
						tracer.traceSticky(elapsed, "Elapsed Monitor Time (ms)", "WebSphere MQ", qManager, "Monitor");
            tracer.traceSticky(1, "Monitor Status", "WebSphere MQ", qManager, "Monitor");
          } catch (e) {
          		def ie = e.getCause();          		
          		log.error("Collection for ${qManager} Failed", ie==null ? e : ie);
          		tracer.traceSticky(2, "Monitor Status", "WebSphere MQ", qMgrName, "Monitor");          		
          } finally {
              try { pcf.disconnect(); } catch (e) {}              
          }        
