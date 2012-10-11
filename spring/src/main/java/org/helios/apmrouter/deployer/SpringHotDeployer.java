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
import java.util.regex.Pattern;

import javax.management.ObjectName;

import org.helios.apmrouter.jmx.ConfigurationHelper;
import org.helios.apmrouter.jmx.JMXHelper;
import org.helios.apmrouter.server.ServerComponentBean;
import org.helios.apmrouter.spring.ctx.ApplicationContextService;
import org.helios.apmrouter.util.SystemClock;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.support.GenericApplicationContext;

/**
 * <p>Title: SpringHotDeployer</p>
 * <p>Description: Hot deploy/undeploy service for child app contexts</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.deployer.SpringHotDeployer</code></p>
 */

public class SpringHotDeployer extends ServerComponentBean  {
	/** The directories that are scanned for hot deploys/undeploys */
	protected Set<Path> hotDirs = new CopyOnWriteArraySet<Path>();
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
			for(Path p: hotDirs) {
				b.append("\n\t").append(p.toString());
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
		keepRunning.set(false);
		watchThread.interrupt();
		processingThread.interrupt();
		processingQueue.clear();
		for(GenericApplicationContext appCtx: deployedContexts.values()) {
			undeploy(appCtx);
		}
		deployedContexts.clear();
		super.doStop();
	}
	
	/**
	 * <p>Responds <code>true</code> for {@link ContextStartedEvent}s.
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#supportsEventType(java.lang.Class)
	 */
	@Override
	public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
		return (ContextStartedEvent.class.isAssignableFrom(eventType));
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
		ContextStartedEvent cse = (ContextStartedEvent)event;
		info("Root AppCtx Started [", new Date(cse.getTimestamp()), "]:[", cse.getApplicationContext().getDisplayName(), "]");
		startFileEventListener();
	}
	
	/**
	 * Starts the file change listener
	 */
	public void startFileEventListener() {
		fileNamePattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
		startProcessingThread();
		try {
			watcher = FileSystems.getDefault().newWatchService();
			for(Path p: hotDirs) {
				p.register(watcher, ENTRY_DELETE, ENTRY_MODIFY);
			}
			watchThread = new Thread("SpringHotDeployerWatchThread"){
				WatchKey watchKey = null;
				public void run() {
					while(keepRunning.get()) {
						try {
							watchKey = watcher.take();
							info("Got watch key [" + watchKey + "]");
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
							    Path fileName = ev.context();
							    if(fileNamePattern.matcher(fileName.toFile().getAbsolutePath()).matches()) {
							    	processingQueue.offer(new FileEvent(fileName.toFile().getAbsolutePath(), ev.kind()));
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
			info("HotDeploy watcher started on [" + hotDirs + "]");
			try { Thread.currentThread().join(); } catch (Exception e) {}
		} catch (Exception ex) {
			error("Failed to start hot deployer", ex);
		}
	}
	
	/**
	 * Enqueues a file event, removing any older instances that this instance will replace
	 * @param fe The file event to enqueue
	 */
	protected void enqueueFileEvent(FileEvent fe) {
		while(processingQueue.remove(fe)) {};
		processingQueue.add(fe);
	}
	
	/**
	 * Starts the processing queue processor thread
	 */
	void startProcessingThread() {
		processingThread = new Thread("SpringHotDeployerProcessingThread") {
			@Override
			public void run() {
				while(keepRunning.get()) {
					try {
						FileEvent fe = processingQueue.take();
						if(fe!=null) {
							if(inProcess.contains(fe)) {
								fe.setTimestamp(SystemClock.time() + 2000);
								enqueueFileEvent(fe);
							} else {
								if(fe.getEventType()==ENTRY_DELETE) {
									killAppCtx(fe);
								} else {
									redeployAppCtx(fe);
								}
							}
						}
					} catch (Exception e) {
						if(interrupted()) interrupted();
					}
				}
			}
		};
	}
	
	/**
	 * Stops and undeploys the application context associated with the deleted file
	 * @param fe The deleted file event
	 */
	protected void killAppCtx(FileEvent fe) {
		GenericApplicationContext appCtx = deployedContexts.remove(fe.getFileName());
		if(appCtx!=null) {
			undeploy(appCtx);
		}
	}
	
	/**
	 * Unregisters the application context mbean and closes the app context
	 * @param appCtx The app context to undeploy
	 */
	protected void undeploy(GenericApplicationContext appCtx) {
		try { 
			ObjectName on = JMXHelper.objectName(ApplicationContextService.HOT_OBJECT_NAME_PREF + ObjectName.quote(appCtx.getDisplayName()));
			if(JMXHelper.getHeliosMBeanServer().isRegistered(on)) {
				JMXHelper.getHeliosMBeanServer().unregisterMBean(on);
			}
		} catch (Exception ex) {
			warn("Failed to undeploy AppCtx MBean for [", appCtx.getDisplayName(), "]", ex);
		}
		appCtx.close();
		
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
		deployedContexts.put(fe.getFileName(), appCtx);
	}
	
	

	/**
	 * Returns the regex pattern used to filter in the desired hot deployed files in the hot dirs. Defaults to <b><code>.*.apmrouter.xml</code></b> 
	 * @return the hot deployed file name filter pattern
	 */
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
