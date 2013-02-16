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
import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>Title: RecursiveDirectorySearch</p>
 * <p>Description: A utility for searching a set of directories recursively for matches.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class RecursiveDirectorySearch {
	/** A directory filter to locate nested directories. */
	protected static DirectoryFileFilter dff = new DirectoryFileFilter();
	
	/**
	 * Recursively searches all the passed directories and returns a set of filenames that matched the passed filter.
	 * @param filter A file name filter. Ignored if null.
	 * @param dirs An array of strings representing directories to search.
	 * @return An array of strings representing the fully qualified file name of located files.
	 */
	public static String[] searchDirectories(FilenameFilter filter, String...dirs) {
		Set<String> locatedFiles = new HashSet<String>();
		for(String dname: dirs) {
			File dir = new File(dname);
			if(dir.isDirectory()) {
				locatedFiles.addAll(recurseDir(filter, dir.getAbsolutePath()));
			}
		}
		return locatedFiles.toArray(new String[locatedFiles.size()]);
	}
	
	/**
	 * Recursive file locator for one directory.
	 * @param filter A filename filter. Ignored if null.
	 * @param dirName The name of the directory to recurse.
	 * @return A set of located matching files.
	 */
	protected static Set<String> recurseDir(FilenameFilter filter, String dirName) {
		Set<String> locatedFiles = new HashSet<String>();
		File file = new File(dirName);
		File[] matchedFiles = null;
		File[] matchedDirs = null;
		if(file.isDirectory()) {
			if(filter != null) {
				matchedFiles = file.listFiles(filter);
			} else {
				matchedFiles = file.listFiles();
			}
			for(File f: matchedFiles) {
				if(f.isDirectory()) {
					locatedFiles.addAll(recurseDir(filter, f.getAbsolutePath()));					
				} else {
					try {
						locatedFiles.add(f.toURI().toURL().toString());
					} catch (MalformedURLException e) {
						throw new RuntimeException("Failed to convert file name [" + f + "] to URL", e);
					}
				}
			}
			matchedDirs = file.listFiles(dff);
			for(File f: matchedDirs) {
				locatedFiles.addAll(recurseDir(filter, f.getAbsolutePath()));
			}
			
		}
		return locatedFiles;
	}
	
	public static void log(Object message) {
		System.out.println(message);
	}
	
	public static void main(String args[]) {
		log("RecursiveDirectorySearch Test");
		ConfigurableFileExtensionFilter cfef = new ConfigurableFileExtensionFilter("tmp");
		String[] files = searchDirectories(cfef, args);
		log("Located [" + files.length + "] Files");
		for(String s: files) {
			log("\t" + s);
		}
	}
}
