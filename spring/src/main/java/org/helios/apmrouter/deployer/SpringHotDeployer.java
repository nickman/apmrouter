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

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.helios.apmrouter.server.ServerComponentBean;

/**
 * <p>Title: SpringHotDeployer</p>
 * <p>Description: Hot deploy/undeploy service for child app contexts</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.deployer.SpringHotDeployer</code></p>
 */

public class SpringHotDeployer extends ServerComponentBean  {
	/** The directory that is scanned for hot deploys/undeploys */
	protected Path hotDir = null;
	/** A map of file paths to Spring contexts to associate the ctx to the file it was booted from */
	protected final Map<Path, String> deployedContexts = new ConcurrentHashMap<Path, String>();
	/** The watch event handling thread */
	protected Thread watchThread = null;
	/** The watch service */
	protected WatchService watcher = null;
	/** The watch key for hot deploy files in the hot dir */
	protected WatchKey watchKey = null;
	/** The keep running flag */
	protected final AtomicBoolean keepRunning = new AtomicBoolean(false); 
	
	/** The name of the default hot deploy directory */
	public static final String DEFAULT_HOT_DIR = System.getProperty("user.home") + File.separator + ".apmrouter" + File.separator + "hotdir";
	
	public SpringHotDeployer() {
		hotDir = Paths.get(DEFAULT_HOT_DIR);
		log("HotDir [" + hotDir + "]");
		
		
	}
	
	public void startx() throws Exception {
		watcher = FileSystems.getDefault().newWatchService();
		watchKey = hotDir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
		watchThread = new Thread("SpringHotDeployerWatchThread"){
			public void run() {
				while(keepRunning.get()) {
					try {
						log("Waiting.....");
						watchKey = watcher.take();
						log("Got watch key [" + watchKey + "]");
				    } catch (InterruptedException ie) {
				        interrupted();
				        // check state
				        continue;
				    }
					if(watchKey!=null) {
						for (WatchEvent<?> event: watchKey.pollEvents()) {
							WatchEvent.Kind<?> kind = event.kind();
							if (kind == OVERFLOW) {
								log("OVERFLOW OCCURED");
					            continue;
							}
							WatchEvent<Path> ev = (WatchEvent<Path>)event;
						    Path fileName = ev.context();							
					        if (kind == ENTRY_CREATE) {
					        	log("CREATED FILE: [" + fileName + "]");
					        } else if (kind == ENTRY_DELETE) {
					        	log("DELETED FILE: [" + fileName + "]");
					        } else if (kind == ENTRY_MODIFY) {
					        	log("MODIFIED FILE: [" + fileName + "]");
					        } else {
					        	log("Unknown kind [" + kind + "]");
					        }
						}
					}
					boolean valid = watchKey.reset();
				    if (!valid) {
				    	log("Watch Key for [" + hotDir + "] is no longer valid. Polling will stop");
				    	keepRunning.set(false);
				        break;
				    }
				}
			}
		};
		watchThread.setDaemon(true);
		keepRunning.set(true);
		watchThread.start();
		log("HotDeploy watcher started on [" + hotDir + "]");
		try { Thread.currentThread().join(); } catch (Exception e) {}
	}
	
	public static void main(String[] args) {
		log("HotDeployTest");
		SpringHotDeployer shd = new SpringHotDeployer();
		try {
			shd.startx();
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	
}
