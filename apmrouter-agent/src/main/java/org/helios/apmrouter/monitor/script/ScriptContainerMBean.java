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
package org.helios.apmrouter.monitor.script;

import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.management.NotificationBroadcaster;
import javax.script.ScriptException;

/**
 * <p>Title: ScriptContainerMBean</p>
 * <p>Description: Management interface for monitoring script instances</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.monitor.script.ScriptContainerMBean</code></p>
 */

public interface ScriptContainerMBean extends NotificationBroadcaster {
	
	/** The JMX ObjectName template */
	public static final String OBJECT_NAME_TEMPLATE = "org.helios.apmrouter.monitor.script:uri=%s,type=%s";
	
	/**
	 * The script instance name
	 * @return the script instance name
	 */
	public String getName();
	/**
	 * The script instance source
	 * @return the script instance source
	 */
	public String getSource();
	/**
	 * Sets the script instance source
	 * @param src the source to set
	 */
	public void setSource(String src);
	/**
	 * The original script URL
	 * @return the original script URL
	 */
	public URL getScriptURL();
	/**
	 * Returns the number of consecutive times this script has executed with errors
	 * @return the number of consecutive times this script has executed with errors
	 */
	public long getConsecutiveErrors();
	/**
	 * Returns the cummulative number of times this script has executed with errors
	 * @return the cummulative number of times this script has executed with errors
	 */	
	public long getTotalErrors();
	/**
	 * Returns the cummulative number of times this script has been executed with errors
	 * @return the cummulative number of times this script has been executed with errors
	 */	
	public long getTotalInvocations();
	/**
	 * Returns the last execution time for this script
	 * @return the last execution time for this script in ms.
	 */
	public long getLastElapsedTimeMs();
	/**
	 * Returns the rolling average execution time for this script
	 * @return the rolling average execution time for this script in ms.
	 */
	public long getRollingElapsedTimeMs();
	/**
	 * Returns the last compilation time for this script
	 * @return the last compilation time for this script in ms.
	 */
	public long getLastCompileTimeMs();
	/**
	 * Returns the rolling average compilation time for this script
	 * @return the rolling average compilation time for this script in ms.
	 */
	public long getRollingCompileTimeMs();	
	/**
	 * Returns a map representing this script's local (private) bindings
	 * @return a map representing this script's local bindings
	 */
	public Map<String, String> getLocalBindings();
	/**
	 * Returns a map representing the script's shared (global) bindings
	 * @return a map representing the script's shared bindings
	 */
	public Map<String, String> getGlobalBindings();
	/**
	 * Returns the last update UTC long timestamp of the source of this script
	 * @return the last update UTC long timestamp of the source of this script
	 */
	public long getSourceTimestamp();
	/**
	 * Returns the last update java date of the source of this script
	 * @return the last update java date of the source of this script
	 */
	public Date getSourceDate();
	/**
	 * Sets the enabled state of this script
	 * @param enabled true to enable, false is disable
	 */
	public void setEnabled(boolean enabled);
	/**
	 * Returns the enabled state of this script
	 * @return true if the script is enabled, false otherwise
	 */
	public boolean isEnabled();
	
	/**
	 * Indicates if the source URL is writable and modified source can be saved 
	 * @return true if the URL is writable, false otherwise
	 */
	public boolean isWritable();
	
    /**
     * Returns the full  name of the <code>ScriptEngine</code>.  For
     * instance an implementation based on the Mozilla Rhino Javascript engine
     * might return <i>Rhino Mozilla Javascript Engine</i>.
     * @return The name of the engine implementation.
     */
    public String getEngineName();

    /**
     * Returns the version of the <code>ScriptEngine</code>.
     * @return The <code>ScriptEngine</code> implementation version.
     */
    public String getEngineVersion();


    /**
     * Returns an immutable list of filename extensions, which generally identify scripts
     * written in the language supported by this <code>ScriptEngine</code>.
     * The array is used by the <code>ScriptEngineManager</code> to implement its
     * <code>getEngineByExtension</code> method.
     * @return The list of extensions.
     */
    public List<String> getExtensions();


    /**
     * Returns an immutable list of mimetypes, associated with scripts that
     * can be executed by the engine.  The list is used by the
     * <code>ScriptEngineManager</code> class to implement its
     * <code>getEngineByMimetype</code> method.
     * @return The list of mime types.
     */
    public List<String> getMimeTypes();

    /**
     * Returns an immutable list of  short names for the <code>ScriptEngine</code>, which may be used to
     * identify the <code>ScriptEngine</code> by the <code>ScriptEngineManager</code>.
     * For instance, an implementation based on the Mozilla Rhino Javascript engine might
     * return list containing {&quot;javascript&quot;, &quot;rhino&quot;}.
     */
    public List<String> getNames();

    /**
     * Returns the name of the scripting langauge supported by this
     * <code>ScriptEngine</code>.
     * @return The name of the supported language.
     */
    public String getLanguageName();

    /**
     * Returns the version of the scripting language supported by this
     * <code>ScriptEngine</code>.
     * @return The version of the supported language.
     */
    public String getLanguageVersion();

    /**
     * Returns the value of an attribute whose meaning may be implementation-specific.
     * Keys for which the value is defined in all implementations are:
     * <ul>
     * <li>ScriptEngine.ENGINE</li>
     * <li>ScriptEngine.ENGINE_VERSION</li>
     * <li>ScriptEngine.NAME</li>
     * <li>ScriptEngine.LANGUAGE</li>
     * <li>ScriptEngine.LANGUAGE_VERSION</li>
     * </ul>
     * <p>
     * The values for these keys are the Strings returned by <code>getEngineName</code>,
     * <code>getEngineVersion</code>, <code>getName</code>, <code>getLanguageName</code> and
     * <code>getLanguageVersion</code> respectively.<br><br>
     * A reserved key, <code><b>THREADING</b></code>, whose value describes the behavior of the engine
     * with respect to concurrent execution of scripts and maintenance of state is also defined.
     * These values for the <code><b>THREADING</b></code> key are:<br><br>
     * <ul>
     * <li><code>null</code> - The engine implementation is not thread safe, and cannot
     * be used to execute scripts concurrently on multiple threads.
     * <li><code>&quot;MULTITHREADED&quot;</code> - The engine implementation is internally
     * thread-safe and scripts may execute concurrently although effects of script execution
     * on one thread may be visible to scripts on other threads.
     * <li><code>&quot;THREAD-ISOLATED&quot;</code> - The implementation satisfies the requirements
     * of &quot;MULTITHREADED&quot;, and also, the engine maintains independent values
     * for symbols in scripts executing on different threads.
     * <li><code>&quot;STATELESS&quot;</code> - The implementation satisfies the requirements of
     * <li><code>&quot;THREAD-ISOLATED&quot;</code>.  In addition, script executions do not alter the
     * mappings in the <code>Bindings</code> which is the engine scope of the
     * <code>ScriptEngine</code>.  In particular, the keys in the <code>Bindings</code>
     * and their associated values are the same before and after the execution of the script.
     * </ul>
     * <br><br>
     * Implementations may define implementation-specific keys.
     *
     * @param key The name of the parameter
     * @return The value for the given parameter. Returns <code>null</code> if no
     * value is assigned to the key.
     *
     */
    public Object getParameter(String key);

    /**
     * Returns a String which can be used to invoke a method of a  Java object using the syntax
     * of the supported scripting language.  For instance, an implementaton for a Javascript
     * engine might be;
     * <p>
     * <pre>
     * <code>
     * public String getMethodCallSyntax(String obj,
     *                                   String m, String... args) {
     *      String ret = obj;
     *      ret += "." + m + "(";
     *      for (int i = 0; i < args.length; i++) {
     *          ret += args[i];
     *          if (i < args.length - 1) {
     *              ret += ",";
     *          }
     *      }
     *      ret += ")";
     *      return ret;
     * }
     *</code>
     *</pre>
     * <p>
     *
     * @param obj The name representing the object whose method is to be invoked. The
     * name is the one used to create bindings using the <code>put</code> method of
     * <code>ScriptEngine</code>, the <code>put</code> method of an <code>ENGINE_SCOPE</code>
     * <code>Bindings</code>,or the <code>setAttribute</code> method
     * of <code>ScriptContext</code>.  The identifier used in scripts may be a decorated form of the
     * specified one.
     *
     * @param m The name of the method to invoke.
     * @param args names of the arguments in the method call.
     *
     * @return The String used to invoke the method in the syntax of the scripting language.
     */
    public String getMethodCallSyntax(String obj, String m, String... args);

    /**
     * Returns a String that can be used as a statement to display the specified String  using
     * the syntax of the supported scripting language.  For instance, the implementaton for a Perl
     * engine might be;
     * <p>
     * <pre><code>
     * public String getOutputStatement(String toDisplay) {
     *      return "print(" + toDisplay + ")";
     * }
     * </code></pre>
     *
     * @param toDisplay The String to be displayed by the returned statement.
     * @return The string used to display the String in the syntax of the scripting language.
     *
     *
     */
    public String getOutputStatement(String toDisplay);


    /**
     * Returns A valid scripting language executable progam with given statements.
     * For instance an implementation for a PHP engine might be:
     * <p>
     * <pre><code>
     * public String getProgram(String... statements) {
     *      $retval = "&lt;?\n";
     *      int len = statements.length;
     *      for (int i = 0; i < len; i++) {
     *          $retval += statements[i] + ";\n";
     *      }
     *      $retval += "?&gt;";
     *
     * }
     * </code></pre>
     *
     *  @param statements The statements to be executed.  May be return values of
     *  calls to the <code>getMethodCallSyntax</code> and <code>getOutputStatement</code> methods.
     *  @return The Program
     */

    public String getProgram(String... statements);
	
	/**
	 * Invokes this script
	 * @return The return value of the script execution
	 * @throws ScriptException thrown on any script execution error
	 */
	public Object invoke() throws ScriptException;
	
	/**
	 * Issues a hard reset on this script, resetting all metrics and <coude>touch</code>ing the file timestamp.
	 */
	public void reset();
	
	/**
	 * Returns the custom frequency of the invocations of this script in ms.
	 * @return the custom frequency of the invocations of this script or -1 if it is the monitor default.
	 */
	public long getCustomFrequency();

	/**
	 * Sets the custom frequency of the invocations of this script in ms.
	 * @param customFrequency the custom frequency in ms. or -1 to assume the default
	 */
	public void setCustomFrequency(long customFrequency);	
	
}
