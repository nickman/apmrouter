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
package org.helios.apmrouter.deployer;

import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import org.helios.apmrouter.jmx.ConfigurationHelper;
import org.helios.apmrouter.server.ServerComponentBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jmx.export.annotation.ManagedAttribute;

/**
 * <p>Title: SpringHotDeployer</p>
 * <p>Description: Hot deploy/undeploy service for child app contexts</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.deployer.SpringHotDeployer</code></p>
 */

public class SpringHotDeployer extends ServerComponentBean  {
	/** The watch keys registered for each directory keyed by the path of the watched directory */
	protected Map<Path, WatchKey> hotDirs = new ConcurrentHashMap<Path, WatchKey>();
	/** A map of file paths to Spring contexts to associate the ctx to the file it was booted from */
	protected final Map<String, GenericApplicationContext> deployedContexts = new ConcurrentHashMap<String, GenericApplicationContext>();
	/** The watch event handling thread */
	protected Thread watchThread = null;
	/** The processingQueue handling thread */
	protected Thread processingThread = null;

	/** The watch service */
	protected WatchService watcher = null;
	/** The keep running flag */
	protected final AtomicBoolean keepRunning = new AtomicBoolean(false);
	/** The processing delay queue that ensures the same file is not processed concurrently for two different events */
	protected final DelayQueue<FileEvent> processingQueue = new DelayQueue<FileEvent>();
	/** A set of file events that are in process */
	protected Set<FileEvent> inProcess = new CopyOnWriteArraySet<FileEvent>();
	/** The application context deployer */
	protected final ApplicationContextDeployer deployer = new ApplicationContextDeployer();
	/** The regex pattern used to filter in the desired hot deployed files in the hot dirs. Defaults to <b><code>.*.apmrouter.xml</code></b> */
	protected String pattern = ".*\\.apmrouter.xml";
	/** The compiled file name filter pattern */
	protected Pattern fileNamePattern = null;
	/** Configuration added hot dir names */
	protected final Set<String> hotDirNames = new HashSet<String>();
	/** The application listener on the root context that forwards to the child contexts */
	protected final ApplicationListener<?> childForwarder = new ApplicationListener() {

		@Override
		public void onApplicationEvent(ApplicationEvent event) {
			if(!(event instanceof ApplicationContextEvent)) {
				// If an application event from root context is not an ApplicationContextEvent
				// it should be forwarded to all the child contexts
				for(GenericApplicationContext appCtx: deployedContexts.values()) {
					appCtx.publishEvent(event);
				}				
			} else {
				// If an application event from root context is an ApplicationContextEvent
				// the only ones we care about are STOP and CLOSE.
				ApplicationContextEvent appCtxEvent = (ApplicationContextEvent)event;
				
			}
 
			
		}
		
	};
	
	/** The name of the default hot deploy directory */
	public static final String DEFAULT_HOT_DIR = System.getProperty("user.home") + File.separator + ".apmrouter" + File.separator + "hotdir";
	/** The system prop that specifies the hot deploy directories */
	public static final String HOT_DIR_PROP = "org.helios.apmrouter.spring.hotdir";
	
	/**
	 * Creates a new SpringHotDeployer
	 */
	public SpringHotDeployer() {
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#doStart()
	 */
	@Override
	protected void doStart() throws Exception {
		super.doStart();
		String[] hds = ConfigurationHelper.getSystemThenEnvProperty(HOT_DIR_PROP, DEFAULT_HOT_DIR).split(",");
		for(String hd: hds) {
			if(hd.trim().isEmpty()) continue;
			hd = hd.trim();
			Path hdPath = Paths.get(hd);
			if(!Files.exists(hdPath) || Files.isDirectory(hdPath)) continue;
			hotDirNames.add(hd);
		}
		for(Iterator<String> iter = hotDirNames.iterator(); iter.hasNext();) {
			File f = new File(iter.next().trim());
			if(!f.exists() || !f.isDirectory()) {
				warn("Configured hot dir path was invalid [", f, "]");
				iter.remove();
			}			
		}
		if(hotDirNames.isEmpty()) {
			warn("No hot deploy directories were defined or found. New directories can be added through the JMX interface.");
		} else {
			StringBuilder b = new StringBuilder("\n\t====================\n\tHot Deploy Directories\n\t====================");
			for(String s: hotDirNames) {
				b.append("\n\t").append(s);
			}
			b.append("\n");
			info(b);
		}
		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#doStop()
	 */
	@Override
	protected void doStop() {
		for(WatchKey wk: hotDirs.values()) {
			wk.cancel();
		}
		hotDirs.clear();
		keepRunning.set(false);
		watchThread.interrupt();
		processingThread.interrupt();
		processingQueue.clear();
		for(GenericApplicationContext appCtx: deployedContexts.values()) {
			deployer.undeploy(appCtx);
		}
		deployedContexts.clear();
		super.doStop();
	}
	
	/**
	 * <p>Responds <code>true</code> for {@link ContextRefreshedEvent}s.
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#supportsEventType(java.lang.Class)
	 */
	@Override
	public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
		return (ContextRefreshedEvent.class.isAssignableFrom(eventType));
	}
	
	/**
	 * <p>Responds <code>true</code>.
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#supportsSourceType(java.lang.Class)
	 */
	@Override
	public boolean supportsSourceType(Class<?> sourceType) {
		return true;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.springframework.context.ApplicationListener#onApplicationEvent(org.springframework.context.ApplicationEvent)
	 */
	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		ContextRefreshedEvent cse = (ContextRefreshedEvent)event;
		if(applicationContext==cse.getApplicationContext()) {
			info("Root AppCtx Started [", new Date(cse.getTimestamp()), "]:[", cse.getApplicationContext().getDisplayName(), "]");
			keepRunning.set(true);
			startFileEventListener();
		}
	}
	
	/**
	 * Starts the file change listener
	 */
	public void startFileEventListener() {
		fileNamePattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
		startProcessingThread();
		try {
			watcher = FileSystems.getDefault().newWatchService();
			for(String fn: hotDirNames) {
				Path path = Paths.get(fn);
				WatchKey watchKey = path.register(watcher, ENTRY_DELETE, ENTRY_MODIFY);
				hotDirs.put(path, watchKey);
			}
			scanHotDirsAtStart();
			watchThread = new Thread("SpringHotDeployerWatchThread"){
				WatchKey watchKey = null;
				public void run() {
					info("Started HotDeployer File Watcher Thread");
					while(keepRunning.get()) {
						try {
							watchKey = watcher.take();
							info("Got watch key for [" + watchKey.watchable() + "]");
							info("File Event Queue:", processingQueue.size());
					    } catch (InterruptedException ie) {
					        interrupted();
					        // check state
					        continue;
					    }
						if(watchKey!=null) {
							for (WatchEvent<?> event: watchKey.pollEvents()) {
								WatchEvent.Kind<?> kind = event.kind();
								if (kind == OVERFLOW) {
									warn("OVERFLOW OCCURED");
									if(!watchKey.reset()) {
										info("Hot Dir for watch key [", watchKey, "] is no longer valid");
									}
						            continue;
								}								
								WatchEvent<Path> ev = (WatchEvent<Path>)event;
								Path dir = (Path)watchKey.watchable();
								
							    Path fileName = Paths.get(dir.toString(), ev.context().toString());
							    if(fileNamePattern.matcher(fileName.toFile().getAbsolutePath()).matches()) {
							    	enqueueFileEvent(500, new FileEvent(fileName.toFile().getAbsolutePath(), ev.kind()));							    	
							    }
							}
						}
						boolean valid = watchKey.reset();						
					    if (!valid) {
					    	warn("Watch Key for [" , watchKey , "] is no longer valid. Polling will stop");
					        break;
					    }
					}
				}
			};
			watchThread.setDaemon(true);
			keepRunning.set(true);
			watchThread.start();
			info("HotDeploy watcher started on [" + hotDirs.keySet() + "]");
			try { Thread.currentThread().join(); } catch (Exception e) {}
		} catch (Exception ex) {
			error("Failed to start hot deployer", ex);
		}
	}
	
	/**
	 * Scans the hot dirs looking for files to deploy at startup. 
	 * Since there's no file change events, we need to go and look for them.
	 */
	protected void scanHotDirsAtStart() {
		for(Path hotDirPath: hotDirs.keySet()) {
			for(File f: hotDirPath.toFile().listFiles()) {
				if(f.isDirectory() || !f.canRead()) continue;
				if(fileNamePattern.matcher(f.getName()).matches()) {
					enqueueFileEvent(500, new FileEvent(f.getAbsolutePath(),  ENTRY_MODIFY));
				}
			}
		}
	}
	
	/**
	 * Enqueues a file event, removing any older instances that this instance will replace
	 * @param delay The delay to add to the passed file event to give the queue a chance to conflate obsolete events already queued
	 * @param fe The file event to enqueue
	 */
	protected void enqueueFileEvent(long delay, FileEvent fe) {
		int removes = 0;
		while(processingQueue.remove(fe)) {removes++;};
		fe.addDelay(delay);
		processingQueue.add(fe);
		info("Queued File Event for [", fe.getFileName(), "] and dropped [" , removes , "] older versions");
	}
	
	protected static final AtomicLong serial = new AtomicLong(0L);
	
	/**
	 * Starts the processing queue processor thread
	 */
	void startProcessingThread() {
		processingThread = new Thread("SpringHotDeployerProcessingThread") {
			@Override
			public void run() {
				info("Started HotDeployer Queue Processor Thread");
				while(keepRunning.get()) {
					try {
						final FileEvent fe = processingQueue.take();						
						if(fe!=null) {
							info("Processing File Event [" , fe.getFileName(), "]" );
							if(inProcess.contains(fe)) {								
								enqueueFileEvent(2000, fe);
							} else {
								Thread t = new Thread("SpringHotDeployer#" + serial.incrementAndGet()) {
									public void run() {
										if(fe.getEventType()==ENTRY_DELETE) {
											killAppCtx(fe);
										} else {
											redeployAppCtx(fe);
										}
									}
								};
								t.setDaemon(true);
								t.start();
							}
						}
					} catch (Exception e) {
						if(interrupted()) interrupted();
					}
				}
			}
		};
		processingThread.setDaemon(true);
		processingThread.start();
	}
	
	/**
	 * Stops and undeploys the application context associated with the deleted file
	 * @param fe The deleted file event
	 */
	protected void killAppCtx(FileEvent fe) {
		GenericApplicationContext appCtx = deployedContexts.remove(fe.getFileName());
		if(appCtx!=null) {
			deployer.undeploy(appCtx);
		}
	}
	
	
	/**
	 * Deploys the application context associated with the modified or new file.
	 * If the application context is already deployed, it will be undeployed first.
	 * @param fe the modified or new file event
	 */
	protected void redeployAppCtx(FileEvent fe) {
		if(deployedContexts.containsKey(fe.getFileName())) {
			killAppCtx(fe);
		}
		GenericApplicationContext appCtx = deployer.deploy(applicationContext, fe);
		appCtx.addApplicationListener(new ApplicationListener(){
			@Override
			public void onApplicationEvent(ApplicationEvent event) {
				if(!(event instanceof ApplicationContextEvent)) {
					//applicationContext.publishEvent(event);
				} else {
					// If the event indicates a child app ctx has stopped or closed
					// remove the appCtx from the deployed context map
					ApplicationContextEvent appCtxEvent = (ApplicationContextEvent)event;
					if((appCtxEvent instanceof ContextStoppedEvent) || (appCtxEvent instanceof ContextClosedEvent)) {
						String name = appCtxEvent.getApplicationContext().getDisplayName();
						info("Received [", event.getClass().getSimpleName(), "] from child context [", name, "]. Removing inert context");
						deployedContexts.remove(name);
					}
					
				}
			}
		});
		deployedContexts.put(fe.getFileName(), appCtx);
		info("\n\t***********************************************\n\tHot Deployed Context [", fe.getFileName(), "]\n\t***********************************************\n");
	}
	
	

	/**
	 * Returns the regex pattern used to filter in the desired hot deployed files in the hot dirs. Defaults to <b><code>.*.apmrouter.xml</code></b> 
	 * @return the hot deployed file name filter pattern
	 */
	@ManagedAttribute(description="The hot deploy file pattern")
	public String getPattern() {
		return pattern;
	}

	/**
	 * Sets the regex pattern used to filter in the desired hot deployed files in the hot dirs. Defaults to <b><code>.*.apmrouter.xml</code></b>
	 * @param pattern the hot deployed file name filter pattern
	 */
	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	/**
	 * Returns the hot directory names
	 * @return the hot directory names
	 */
	@ManagedAttribute(description="The registered hot deploy directories")
	public Set<String> getHotDirNames() {
		return Collections.unmodifiableSet(hotDirNames);
	}
	
	/**
	 * Sets the hot directory names
	 * @param hotDirNames the hot directory names
	 */
	public void setHotDirNames(Set<String> hotDirNames) {
		if(hotDirNames!=null) {
			this.hotDirNames.addAll(hotDirNames);
		}
	}
	
	
}
