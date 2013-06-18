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
package org.helios.apmrouter.catalog.jdbc.h2;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Arrays;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.helios.apmrouter.util.URLHelper;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * <p>Title: ScriptExecution</p>
 * <p>Description: Executes SQL scripts</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.catalog.jdbc.ScriptExecution</code></p>
 */

public class ScriptExecution {
	/** The datasource to get the connection from  */
	protected final DataSource dataSource;
	/** The script source to run */
	protected String scriptSource;
	/** Instance logger */
	protected Logger log = Logger.getLogger(getClass());
	
	/** The relative resource root for DDL resources */
	public static String DEFAULT_DDL_RESOURCE_ROOT = "/ddl/";
	
	/** The sysprop name defining the root config resource */
	public static final String ROOT_CONFIG = "org.helios.apmrouter.root.config";

	
	/** The runscript template */
	public static final String URL_SCRIPT_TEMPLATE = "RUNSCRIPT FROM '%s'";
	
	/**
	 * Creates a new ScriptExecution
	 * @param dataSource The datasource to get the connection from 
	 * @param scriptSource The script source to run
	 */
	public ScriptExecution(DataSource dataSource, String scriptSource) {
		this.dataSource = dataSource;
		this.scriptSource = resolveScriptSource(scriptSource);
	}
	
	protected String resolveScriptSource(String src) {
		if(src==null || src.trim().isEmpty()) throw new IllegalArgumentException("The passed source was null or empty", new Throwable());
		src = src.trim();
		if(src.toLowerCase().startsWith("runscript ")) {
			return src.trim();
		}
		if(URLHelper.isValidURL(src)) {
			File tmpFile = extractTempFile(URLHelper.toURL(src));
			tmpFile.deleteOnExit();
			return String.format(URL_SCRIPT_TEMPLATE, tmpFile.getAbsolutePath());
		}
		String configRoot = System.getProperty(ROOT_CONFIG);
		if(configRoot==null) {
			throw new RuntimeException("No root configs defined in [" + ROOT_CONFIG + "]", new Throwable());			
		}
		String[] configRoots = configRoot.indexOf(',')==-1 ? new String[]{configRoot.trim()} : configRoot.split(",");
		PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
		for(String cr: configRoots) {
			if(!URLHelper.isValidURL(cr)) {
				cr = "classpath:" + cr + DEFAULT_DDL_RESOURCE_ROOT + src;			
			} else {
				cr = cr + DEFAULT_DDL_RESOURCE_ROOT + src;
			}			
			try {
				Resource resource = resolver.getResource(cr);
				File tmpFile = extractTempFile(resource.getURL());
				tmpFile.deleteOnExit();
				return String.format(URL_SCRIPT_TEMPLATE, tmpFile.getAbsolutePath());
				
			} catch (Exception e) {
				log.debug("Script resource not found at [" + cr + "]");
			}			
		}
		throw new RuntimeException("No script resources found in " + Arrays.toString(configRoots) + "]", new Throwable());
	}
	
	/**
	 * Creates a new ScriptExecution
	 * @param dataSource The datasource to get the connection from 
	 * @param scriptSource The script source to run
	 */
	public ScriptExecution(DataSource dataSource, URL scriptSource) {
		this.dataSource = dataSource;
		File scriptFile = extractTempFile(scriptSource);
		this.scriptSource = scriptFile.getAbsolutePath();
	}	
	
	/**
	 * Runs a script in the passed URL
	 * @param dataSource the data source to acquire the connection from
	 * @param urlSource The URL path of the script
	 */
	public static void runScript(DataSource dataSource, String urlSource) {
		final File tmpFile = extractTempFile(urlSource);
		try {
			new ScriptExecution(dataSource, String.format(URL_SCRIPT_TEMPLATE, tmpFile.getAbsolutePath())).start();
		} finally {
			tmpFile.delete();
		}
	}
	
	/**
	 * Extracts the content from the passed URL and writes it to a temp file
	 * @param urlResource The URL to read the content from
	 * @return the temp file
	 */
	protected static File extractTempFile(String urlResource) {
		return extractTempFile(URLHelper.toURL(urlResource));
	}
	
	/**
	 * Extracts the content from the passed URL and writes it to a temp file
	 * @param url The URL to read the content from
	 * @return the temp file
	 */
	protected static File extractTempFile(URL url) {
		FileOutputStream fos = null;
		try {
			byte[] content = URLHelper.getBytesFromURL(url);
			String[] frags = url.toString().split("/");
			String fileName = frags[frags.length-1].split("\\.")[0];
			File tmpFile = File.createTempFile("apmrouter-ddl-" + fileName, ".sql");
			tmpFile.deleteOnExit();
			fos = new FileOutputStream(tmpFile);
			fos.write(content);
			fos.flush();
			fos.close();
			return tmpFile;
		} catch (Exception ex) {
			throw new RuntimeException("Failed to load DDL from URL [" + url + "]", ex);
		} finally {
			if(fos!=null) try { fos.close(); } catch (Exception ex) {}
		}
	}	
	
	public void startUrl() {
		File f = null;
		try {
			URL url = ClassLoader.getSystemResource(scriptSource);
			f = extractTempFile(url);
			scriptSource = String.format(URL_SCRIPT_TEMPLATE, f.getAbsolutePath());
			start();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		} finally {
			if(f!=null) f.delete();
		}
	}
	
	/**
	 * Executes the configured script
	 */
	public void start() {
		Connection conn = null;
		PreparedStatement ps = null;
		try {
			conn = dataSource.getConnection();
			ps = conn.prepareStatement(scriptSource);
			ps.execute();
			log.info("Executed Startup Script [" + scriptSource + "]");
		} catch (Exception e) {
			throw new RuntimeException("Failed to execute script [" + scriptSource + "]", e);
		} finally {
			if(ps!=null) try { ps.close(); } catch (Exception e) {}
			if(conn!=null) try { conn.close(); } catch (Exception e) {}
		}
	}
	

}
