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
package org.helios.collector.jmx.tracers.script;

import java.io.File;
/**
 * <p>Title: ScriptBean</p>
 * <p>Description: POJO that holds information related to a script integrated with Helios</p> 
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 */
public class ScriptBean {
	/* Scripting Language to be used */
	protected String language=null;
	/* A java.io.File object pointing to a script source on local drive*/
	protected File scriptFile=null;
	/* Inline script to be used.  You can either use script file or Inline script.  */
	protected String inlineScript=null;
	/* FQ Class name of the interface that will be used for this script.  */
	protected Class<? extends Object> scriptBeanInterfaceClazz = null;
	/* A flag to indicate whether the inline script or file is modified during runtime. If they are, 
	 * it will reevaluate the script and acquire the Interface again.  */ 
	boolean isModified = false;
	/* In case of the script file, this field holds the last modified time of the file. */
	protected long lastModified = 0L;
	/* A counter that indicates how many times a file's last modified check should be skipped.  Default value is 5 
	 * but setting this counter to -1 would disable any skipping and check file last modified timestamp every time.  
	 * For performance reasons, you may want to fine tune the value of this flag.  */
	protected int skipFileCheckCounter = 5;
	/* Internal counter tto keep track of how many iterations are actually skipped so far. */
	protected int actualFileCheckSkippedCounter = 0;
	
	/**
	 * Constructor to create ScriptBean object for a specific language for which the script is provided in 
	 * the java.io.File object.  It also provides class to be used as an Interface for this script.  
	 * 
	 * @param language
	 * @param scriptFile
	 * @param scriptBeanInterfaceClazz
	 */
	public ScriptBean(String language, File scriptFile, Class<? extends Object> scriptBeanInterfaceClazz) {
		this.language = language;
		this.scriptFile = scriptFile;
		this.lastModified = scriptFile.lastModified();
		this.scriptBeanInterfaceClazz = scriptBeanInterfaceClazz;
	}
	
	/**
	 * Constructor to create ScriptBean object for a specific language for which the script is provided  
	 * inline in the configuration file.  It also provides class to be used as an Interface for this script.  
 
	 * @param language
	 * @param inlineScript
	 * @param scriptBeanInterfaceClazz
	 */
	public ScriptBean(String language, String inlineScript, Class<? extends Object> scriptBeanInterfaceClazz) {
		this.language = language;
		this.inlineScript = inlineScript;
		this.scriptBeanInterfaceClazz = scriptBeanInterfaceClazz;
	}

	/**
	 * Constructor to create ScriptBean object for a specific language for which the script is provided in 
	 * the java.io.File object.    
	 * 
	 * @param language
	 * @param scriptFile
	 */
	public ScriptBean(String language, File scriptFile) {
		this.language = language;
		this.scriptFile = scriptFile;
		this.lastModified = scriptFile.lastModified();		
	}
	
	/**
	 * Constructor to create ScriptBean object for a specific language for which the script is provided  
	 * inline in the configuration file.    
	 *  
	 * @param language
	 * @param inlineScript
	 */
	public ScriptBean(String language, String inlineScript){
		this.language = language;
		this.inlineScript = inlineScript;		
	}

	/**
	 * 
	 * @return the inlineScript
	 */
	public String getInlineScript() {
		return inlineScript;
	}
	
	/**
	 * 
	 * @param inlineScript the inlineScript to set
	 * @throws ScriptBeanException - If trying to set inlineScript when this ScriptBean is created for script file 
	 */
	public void setInlineScript(String inlineScript) throws ScriptBeanException {
		if(scriptFile!=null){
			throw new ScriptBeanException("You cannot set inlineScript for a ScriptBean created with scriptFile attribute.");
		}
		this.inlineScript = inlineScript;
		this.isModified = true;
	}
	
	/**
	 * @return the language
	 */
	public String getLanguage() {
		return language;
	}

	/**
	 * @return the scriptFile
	 */
	public File getScriptFile() {
		return scriptFile;
	}
	/**
	 * @param scriptFile the scriptFile to set
	 * @throws ScriptBeanException - If trying to set script File when this ScriptBean is created for inlineScript
	 */
	public void setScriptFile(File scriptFile) throws ScriptBeanException {
		if(inlineScript!=null){
			throw new ScriptBeanException("You cannot set scriptFile for a ScriptBean created with inlineScript attribute.");
		}		
		this.scriptFile = scriptFile;
		this.isModified = true;
		this.actualFileCheckSkippedCounter=0;
		this.lastModified = scriptFile.lastModified();
	}

	/**
	 * @return the scriptBeanInterfaceClazz
	 */
	public Class<? extends Object> getScriptBeanInterfaceClazz() {
		return scriptBeanInterfaceClazz;
	}

	/**
	 * @param scriptBeanInterfaceClazz the scriptBeanInterfaceClazz to set
	 */
	public void setScriptBeanInterfaceClazz(Class<Object> scriptBeanInterfaceClazz) {
		this.scriptBeanInterfaceClazz = scriptBeanInterfaceClazz;
	}
	/**
	 * @param language the language to set
	 */
	public void setLanguage(String language) {
		this.language = language;
	}

	/**
	 * @return the lastModified
	 */
	public long getLastModified() {
		return lastModified;
	}
	/**
	 * @param lastModified the lastModified to set
	 */
	public void setLastModified(long lastModified) {
		this.lastModified = lastModified;
	}
	
	/**
	 * Determines whether a refresh is required for this ScriptBean.
	 * 
	 * @return the isModified
	 */
	public boolean isModified() {
		if(scriptFile!=null){
			if(skipFileCheckCounter!=-1){
				if(actualFileCheckSkippedCounter == skipFileCheckCounter){
					actualFileCheckSkippedCounter=0;
					return (isFileModified() || isModified);
				}else{
					actualFileCheckSkippedCounter++;					
				}
			}else{
				return (isFileModified() || isModified);
			}
		}
		return isModified;
	}
	
	/**
	 * @param isModified the isModified to set
	 */
	public void setModified(boolean isModified) {
		this.isModified = isModified;
	}	
	
	/**
	 * Compares file's last modified stamp to determine whether the script file has been modified
	 * during runtime.  
	 * 
	 * @return
	 */
	public boolean isFileModified()  {
		if(lastModified==0L) return false;
		try {
			if(scriptFile.lastModified() > lastModified){
				lastModified = scriptFile.lastModified();
				return true;
			}else{
				return false;
			}
		} catch (SecurityException sex) { 
			return false; 
		} 
	}
	
	/**
	 * @return the skipFileCheckCounter
	 */
	public int getSkipFileCheckCounter() {
		return skipFileCheckCounter;
	}
	
	/**
	 * @param skipFileCheckCounter the skipFileCheckCounter to set
	 */
	public void setSkipFileCheckCounter(int skipFileCheckCounter) {
		this.skipFileCheckCounter = skipFileCheckCounter;
	}	
	
}
