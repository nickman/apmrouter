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
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
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
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;

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
	protected ApplicationContextDeployer deployer;
	/** The regex pattern used to filter in the desired hot deployed files in the hot dirs. Defaults to <b><code>.*.apmrouter.xml</code></b> */
	protected String pattern = ".*\\.apmrouter.xml";
	/** The compiled file name filter pattern */
	protected Pattern fileNamePattern = null;
	
	/** Configuration added hot dir names */
	protected final Set<String> hotDirNames = new HashSet<String>();
	// =======================================================================================
	//		These settings control how much automated deployment will be done
	// =======================================================================================
	/** Indicates if the default hot deploy directory at <code>${user.home}/.apmrouter/hotdir</code> should be disabled. By default it is enabled */
	protected boolean disableDefaultHotDir = false;
	/** Indicates if the default module hot deploy lib directory class loading should be disabled. By default it is enabled */
	protected boolean disableHotDirLibs = false;
	/** Indicates if the default module hot deploy app directory loading should be disabled. By default it is enabled */
	protected boolean disableHotDirApps = false;
	/** The hot deploy application directory name pattern. This is the name of a hot deployed application directory which, unless disabled by {@link #disableHotDirApps}, when found in the root of 
	 * a hot directory will be automatically deployed. By default it is <code>XXX.app</code> */
	protected String hotDeployAppDirectoryExt = ".app";
	// =======================================================================================
	
	
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
		deployer = new ApplicationContextDeployer(disableHotDirLibs);
		initDefaultHotDir();
		initEnvHotDirs();
		validateInitialHotDirs();
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
	 * Initializes the default hot dir unless it has been disabled. 
	 */
	protected void initDefaultHotDir() {
		if(!disableDefaultHotDir) {
			File defaultHotDir = new File(System.getProperty("user.home") + File.separator + ".apmrouter" + File.separator + "hotdir");
			if(defaultHotDir.exists()) {
				if(!defaultHotDir.isDirectory()) {
					warn("\n\t###########################################\n\tProblem: The default hot deploy directory [", defaultHotDir, "] exists but it is a file\n\t###########################################\n");
				} else {
					hotDirNames.add(defaultHotDir.getAbsolutePath());
					scanForApplications(defaultHotDir.toPath(), true);
				}
			} else {
				if(!defaultHotDir.mkdirs()) {
					warn("\n\t###########################################\n\tProblem: Failed to create hot deploy directory [", defaultHotDir, "]\n\t###########################################\n");
				} else {
					hotDirNames.add(defaultHotDir.getAbsolutePath());
					scanForApplications(defaultHotDir.toPath(), true);
				}
			}
		}
	}
	
	/**
	 * Validates the initial hot directories, removing any that are invalid
	 */
	protected void validateInitialHotDirs() {
		for(Iterator<String> iter = hotDirNames.iterator(); iter.hasNext();) {
			File f = new File(iter.next().trim());
			if(!f.exists() || !f.isDirectory()) {
				warn("Configured hot dir path was invalid [", f, "]");
				iter.remove();
			}			
		}
	}
	
	/**
	 * Configures watches for system prop or environment defined hot directories
	 */
	protected void initEnvHotDirs() {
		String[] hds = ConfigurationHelper.getSystemThenEnvProperty(HOT_DIR_PROP, DEFAULT_HOT_DIR).split(",");
		for(String hd: hds) {
			if(hd.trim().isEmpty()) continue;
			hd = hd.trim();
			Path hdPath = Paths.get(hd);
			if(!Files.exists(hdPath) || Files.isDirectory(hdPath)) continue;
			hotDirNames.add(hd);
			scanForApplications(hdPath, true);
		}
	}
	
	/**
	 * Scans the passed path for application directories and adds them to the watched set, unless apps have been disabled
	 * @param hotDir The path to scan
	 * @param add If true, the located application directories will be added, if false, just returns the names, taking no further action 
	 * @return A set of the application directory names that were added
	 */
	protected Set<String> scanForApplications(Path hotDir, boolean add) {
		Set<String> added = new HashSet<String>();
		if(!disableHotDirApps && add) {			
			for(File f: hotDir.toFile().listFiles()) {
				if(f.isDirectory() && f.getName().toLowerCase().endsWith(hotDeployAppDirectoryExt)) {
					hotDirNames.add(f.getAbsolutePath());
					added.add(f.getAbsolutePath());
				}
			}
		}
		return added;
	}
	
	/**
	 * Adds a hot deploy directory
	 * @param dirName the name of the hot deploy directory to add
	 * @return a string message summarizing the results of the operation
	 */
	@ManagedOperation(description="Adds a hot deploy directory")
	@ManagedOperationParameters({
		@ManagedOperationParameter(name="dirName", description="The name of the hot deploy directory to add")
	})
	public String addHotDir(String dirName) {
		if(dirName==null || dirName.trim().isEmpty()) {
			return "Null or empty directory name";
		}		
		File f = new File(dirName.trim());
		StringBuilder b = new StringBuilder("Adding hot directory [").append(dirName).append("]");
		if(f.exists() && f.isDirectory()) {
			hotDirNames.add(f.getAbsolutePath());
			b.append("\n\tAdded [").append(f.getAbsolutePath()).append("]");
			Set<String> apps = scanForApplications(f.toPath(), true);			
			if(!apps.isEmpty()) {
				for(String appDir: apps) {
					b.append("\n\tAdded [").append(appDir).append("]");
				}
			}
		} else {
			b.append("\n\tDirectory did not exist");
		}
		b.append("\n");
		try { updateWatchers(); } catch (IOException ioe) {
			error("Failure during updateWatchers", ioe);
		}
		return b.toString();
	}
	
	/**
	 * Removes a hot deploy directory
	 * @param dirName the name of the hot deploy directory to remove
	 * @return a string message summarizing the results of the operation
	 */
	@ManagedOperation(description="Removes a hot deploy directory")
	@ManagedOperationParameters({
		@ManagedOperationParameter(name="dirName", description="The name of the hot deploy directory to remove")
	})
	public String removeHotDir(String dirName) {
		if(dirName==null || dirName.trim().isEmpty()) {
			return "Null or empty directory name";
		}		
		File f = new File(dirName.trim());
		StringBuilder b = new StringBuilder("Removing hot directory [").append(dirName).append("]");
		WatchKey watchKey = null;
		if(f.exists() && f.isDirectory()) {
			hotDirNames.remove(f.getAbsolutePath());
			watchKey = hotDirs.remove(f.toPath());
			if(watchKey!=null) watchKey.cancel();
			b.append("\n\tRemoved [").append(f.getAbsolutePath()).append("]");
			Set<String> apps = scanForApplications(f.toPath(), false);			
			if(!apps.isEmpty()) {
				for(String appDir: apps) {
					f = new File(appDir);
					hotDirNames.remove(appDir);
					watchKey = hotDirs.remove(f.toPath());
					if(watchKey!=null) watchKey.cancel();					
					b.append("\n\tRemoved [").append(appDir).append("]");
				}
			}
		} else {
			b.append("\n\tDirectory did not exist");
		}
		b.append("\n");
		return b.toString();
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
//		ContextRefreshedEvent cse = (ContextRefreshedEvent)event;
//		if(applicationContext==cse.getApplicationContext()) {
//			info("Root AppCtx Started [", new Date(cse.getTimestamp()), "]:[", cse.getApplicationContext().getDisplayName(), "]");
//			keepRunning.set(true);
//			startFileEventListener();
//		}
	}
	
	/**
	 * @param ctx
	 */
	/**
	 * Callback when the current app context refreshes
	 * @param cse The context refreshed event
	 */
	public void onApplicationContextRefresh(ContextRefreshedEvent cse) {
		info("Root AppCtx Started [", new Date(cse.getTimestamp()), "]:[", cse.getApplicationContext().getDisplayName(), "]");
		keepRunning.set(true);
		startFileEventListener();
		
	}
	
	/**
	 * Scans the hot diretory names and registers a watcher for any unwatched names,
	 * then removes any registered watchers that are no longer in the hot diretory names set 
	 * @throws IOException thrown on IO exceptions related to paths
	 */
	protected synchronized void updateWatchers() throws IOException {
		Map<Path, WatchKey> hotDirSnapshot = new HashMap<Path, WatchKey>(hotDirs);
		for(String fn: hotDirNames) {
			Path path = Paths.get(fn);
			if(hotDirs.containsKey(path)) {
				hotDirSnapshot.remove(path);
			} else {
				WatchKey watchKey = path.register(watcher, ENTRY_DELETE, ENTRY_MODIFY);
				hotDirs.put(path, watchKey);
				info("Added watched deployer directory [", path, "]");
			}
		}
		for(Map.Entry<Path, WatchKey> remove: hotDirSnapshot.entrySet()) {
			remove.getValue().cancel();
			info("Cancelled watch on deployer directory [", remove.getKey(), "]");
		}
		hotDirSnapshot.clear();
	}
	
	/**
	 * Starts the file change listener
	 */
	public void startFileEventListener() {
		fileNamePattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
		startProcessingThread();
		try {
			watcher = FileSystems.getDefault().newWatchService();
			scanHotDirsAtStart();
			updateWatchers();
			
			
			
			watchThread = new Thread("SpringHotDeployerWatchThread"){
				WatchKey watchKey = null;
				public void run() {
					info("Started HotDeployer File Watcher Thread");
					while(keepRunning.get()) {
						try {
							watchKey = watcher.take();
							debug("Got watch key for [" + watchKey.watchable() + "]");
							debug("File Event Queue:", processingQueue.size());
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
										watchKey.cancel();
										Path dir = (Path)watchKey.watchable();
										hotDirNames.remove(dir.toFile().getAbsolutePath());
										hotDirs.remove(dir);
									}
						            continue;
								}								
								WatchEvent<Path> ev = (WatchEvent<Path>)event;
								Path dir = (Path)watchKey.watchable();
								
							    Path fileName = Paths.get(dir.toString(), ev.context().toString());
							    if(fileNamePattern.matcher(fileName.toFile().getAbsolutePath()).matches()) {
							    	enqueueFileEvent(500, new FileEvent(fileName.toFile().getAbsolutePath(), ev.kind()));							    	
							    } else if(fileName.toFile().isDirectory() && fileName.toFile().getName().endsWith(hotDeployAppDirectoryExt)) {
							    	addHotDir(fileName.toFile().getAbsolutePath());
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
		} catch (Exception ex) {
			error("Failed to start hot deployer", ex);
		}
	}
	
	/**
	 * Scans the hot dirs looking for files to deploy at startup. 
	 * Since there's no file change events, we need to go and look for them.
	 */
	protected void scanHotDirsAtStart() {
		for(String hotDirPathName: hotDirNames) {
			File hotDirPath = new File(hotDirPathName);
			for(File f: hotDirPath.listFiles()) {
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
		debug("Queued File Event for [", fe.getFileName(), "] and dropped [" , removes , "] older versions");
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
							debug("Processing File Event [" , fe.getFileName(), "]" );
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
						debug("Received [", event.getClass().getSimpleName(), "] from child context [", name, "]. Removing inert context");
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

	/**
	 * Returns the disabled state of the default hot directory in <code>${user.home}/.apmrouter/hotdir</code>
	 * @return true if disabled, false if enabled
	 */
	@ManagedAttribute(description="The disabled state of the default hot directory")
	public boolean isDisableDefaultHotDir() {
		return disableDefaultHotDir;
	}

	/**
	 * Sets the disabled state of the default hot directory in <code>${user.home}/.apmrouter/hotdir</code>
	 * @param disableDefaultHotDir true to disable, false to enable
	 */
	@ManagedAttribute(description="The disabled state of the default hot directory")
	public void setDisableDefaultHotDir(boolean disableDefaultHotDir) {
		this.disableDefaultHotDir = disableDefaultHotDir;
	}

	/**
	 * Returns the disabled state of the hot deployer's automatic jar library classpath extender 
	 * @return true if disabled, false if enabled
	 */
	@ManagedAttribute(description="The disabled state of the hot deployer's automatic jar library classpath extender")
	public boolean isDisableHotDirLibs() {
		return disableHotDirLibs;
	}

	/**
	 * Sets the disabled state of the hot deployer's automatic jar library classpath extender 
	 * for <code>.lib</code> sub directories in hot directories or hot application directories
	 * @param disableHotDirLibs true to disable, false to enable
	 */
	@ManagedAttribute(description="The disabled state of the hot deployer's automatic jar library classpath extender")
	public void setDisableHotDirLibs(boolean disableHotDirLibs) {
		this.disableHotDirLibs = disableHotDirLibs;
	}

	/**
	 * Returns the disabled state of the hot deployer's automatic application deployer
	 * @return true if disabled, false if enabled
	 */
	@ManagedAttribute(description="The disabled state of the hot deployer's automatic hot directory application subdirectories")
	public boolean isDisableHotDirApps() {
		return disableHotDirApps;
	}

	/**
	 * Sets the disabled state of the hot deployer's automatic application deployer 
	 * @param disableHotDirApps true to disable, false to enable
	 */
	@ManagedAttribute(description="The disabled state of the hot deployer's automatic hot directory application subdirectories")
	public void setDisableHotDirApps(boolean disableHotDirApps) {
		this.disableHotDirApps = disableHotDirApps;
	}

	/**
	 * Returns the extension of hot deployed application directories.
	 * @return the extension of hot deployed application directories.
	 */
	@ManagedAttribute(description="The hot directory application subdirectory extension")
	public String getHotDeployAppDirectoryExt() {
		return hotDeployAppDirectoryExt;
	}

	/**
	 * Sets the extension of hot deployed application directories.
	 * @param hotDeployAppDirectoryExt the extension of hot deployed application directories
	 */
	@ManagedAttribute(description="The hot directory application subdirectory extension")
	public void setHotDeployAppDirectoryExt(String hotDeployAppDirectoryExt) {
		this.hotDeployAppDirectoryExt = hotDeployAppDirectoryExt;
	}
	
	
}
