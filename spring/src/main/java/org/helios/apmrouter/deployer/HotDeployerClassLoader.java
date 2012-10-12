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

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;

import org.helios.apmrouter.util.URLHelper;

/**
 * <p>Title: HotDeployerClassLoader</p>
 * <p>Description: Custom classloader to add to the private class path of a hot deployed application context</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.deployer.HotDeployerClassLoader</code></p>
 */

public class HotDeployerClassLoader extends URLClassLoader  {
	/** The classpath entires in the form of files */
	protected final Set<String> classPathEntries = new HashSet<String>();
	/** If true, all classpath entries are validated */
	protected boolean validateEntries = true;
	
	/**
	 * Creates a new HotDeployerClassLoader
	 */
	public HotDeployerClassLoader() {
		super(new URL[]{}, HotDeployerClassLoader.class.getClassLoader());
	}
	
	/**
	 * Initializes the classloader with the configured URLS
	 * @throws Exception thrown on any error initializing
	 */
	public void init() throws Exception {
		for(String entry: classPathEntries) {
			this.addURL(toURL(entry));
		}
	}
	
	
	/**
	 * Attempts to convert the passed string to a URL or a File
	 * @param url The string to convert
	 * @return A URL
	 */
	protected URL toURL(String url) {
		if(url==null || url.trim().isEmpty()) {
			throw new RuntimeException("Configured classpath entry was empty or null", new Throwable());
		}
		url = url.trim();
		if(URLHelper.isValidURL(url)) {
			URL cpUrl = URLHelper.toURL(url);
			if(validateEntries) {
				URLHelper.getBytesFromURL(cpUrl);
			}
			return cpUrl;
		}
		File furl = new File(url);
		if(validateEntries) {
			if(!furl.canRead()) {
				throw new RuntimeException("Classpath entry [" + url + "] was not a URL or a readable file", new Throwable());
			}
		}
		return URLHelper.toURL(furl);
	}

	/**
	 * Indicates if classpath entries are being be validated
	 * @return true if classpath entries are being be validated, false to explode later
	 */
	public boolean isValidateEntries() {
		return validateEntries;
	}

	/**
	 * Sets if classpath entries should be validated
	 * @param validateEntries true to validate entries, false to explode later
	 */
	public void setValidateEntries(boolean validateEntries) {
		this.validateEntries = validateEntries;
	}

	/**
	 * Returns the configured classpath entries 
	 * @return the classPath entries
	 */
	public Set<String> getClassPathEntries() {
		return classPathEntries;
	}
	
	/**
	 * Sets the configured classpath entries 
	 * @param entries the configured classpath entries 
	 */
	public void setClassPathEntries(Set<String> entries) {
		if(entries!=null) {
			classPathEntries.addAll(entries);
		}
	}

}
