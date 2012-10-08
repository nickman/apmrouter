/**
 * ICE Futures, US
 */
package org.helios.apmrouter.util;

/**
 * <p>Title: Methods</p>
 * <p>Description: Some static utility methods</p> 
 * <p>Company: ICE Futures US</p>
 * @author Whitehead (nicholas.whitehead@theice.com)
 * @version $LastChangedRevision$
 * <p><code>org.helios.apmrouter.util.Methods</code></p>
 */

public class Methods {
	/**
	 * Validates that a parameter is not null or trimmed-empty if a char sequence
	 * @param <T> The type of the passed parameter
	 * @param arg The argument 
	 * @param name An optional name of the parameter
	 * @return the validated argument
	 */
	public static <T> T nvl(T arg, String name) {
		if(arg==null) throw new IllegalArgumentException("The passed [" + (name==null ? "value" : name) + "] was null", new Throwable());
		if(arg instanceof CharSequence) {
			if(((CharSequence)arg).toString().trim().isEmpty()) throw new IllegalArgumentException("The passed CharSequence [" + (name==null ? "value" : name) + "] was empty", new Throwable());
		}
		return arg;
	}
	
	/**
	 * Validates that a parameter is not null or trimmed-empty if a char sequence
	 * @param <T> The type of the passed parameter
	 * @param arg The argument 
	 * @return the validated argument
	 */
	public static <T> T nvl(T arg) {
		return nvl(arg, null);
	}
	
}
