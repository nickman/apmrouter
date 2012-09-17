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
package org.helios.apmrouter.util.io;

import java.io.File;
import java.io.FilenameFilter;

/**
 * <p>Title: ConfigurableFileExtensionFilter</p>
 * <p>Description: File filter for selecting files with defined extensions.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class ConfigurableFileExtensionFilter implements FilenameFilter {
	/** An array of file extensions that will be accepted */
	protected String[] extensions = null;
	/** Indicates if filter is case sensitive */
	protected boolean caseSensitive = false;
	
	/**
	 * Creates a new ConfigurableFileExtensionFilter which is not case sensitive
	 * and will accept files with the passed extensions.
	 * @param extensions File extensions this filter should accept.
	 */
	public ConfigurableFileExtensionFilter(String...extensions) {
		this.extensions = extensions;
		if(this.extensions==null) this.extensions = new String[]{}; 
	}
	
	/**
	 * Creates a new ConfigurableFileExtensionFilter with the defined case sensitivity
	 * and will accept files with the passed extensions. 
	 * @param caseSensitive if true, file extension tests will be case sensitive.
	 * @param extensions File extensions this filter should accept.
	 */
	public ConfigurableFileExtensionFilter(boolean caseSensitive, String...extensions) {
		this(extensions);
		this.caseSensitive = caseSensitive;
	}

	/**
	 * Tests if a specified file should be included in a file list.  
	 * @param dir the directory in which the file was found.
	 * @param name the name of the file. 
	 * @return true if and only if the name matches one of the configured extensions.
	 * @see java.io.FilenameFilter#accept(java.io.File, java.lang.String)
	 */
	public boolean accept(File dir, String name) {
		
		for(String ext: extensions) {
			if(caseSensitive) {
				if(name.endsWith(ext)) return true;				
			} else {
				if(name.toUpperCase().endsWith(ext.toUpperCase())) return true;
			}
		}
		return false;
	}
	
}
