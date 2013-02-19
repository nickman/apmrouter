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
package org.helios.collector.jdbc.binding.provider;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

/**
 * <p>Title: ProviderToken</p>
 * <p>Description: Parser and value container utility class for bind variable provider tokens.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 * org.helios.collectors.jdbc.binding.provider.ProviderToken
 */
public class ProviderToken {
    /** The provider type key */
    private String providerTypeKey = null;
    /** The provider configuration string */
    private String providerConfig = null;
    /** The provider instance key  */
    private String instanceKey = null;
    /** The binder token key  */
    private String binderTokenKey = null;
    /** The binder configuration string */
    private String binderConfig = null;
    /** The force no bind tag */
    private boolean forceNoBind = false;
    /** The whole token */
    private String providerToken = null;
    /** Class logger */
    private static final Logger LOG = Logger.getLogger(ProviderToken.class);
	
	/** The provider token parsing regular expression */
    //public static final Pattern BIND_VAR_PATTERN = Pattern.compile("(?:!?\\{(.*?):(.*)+?:(.*?)(?:\\[(\\S+?)(?::(\\S+))?\\])*\\})", Pattern.CASE_INSENSITIVE);
    //public static final Pattern BIND_VAR_PATTERN = Pattern.compile("(?:!?\\{+?(.*?):(.*)+?:(.*?)?\\})(?:\\[(\\S+?)(?::(\\S+))?\\])?", Pattern.CASE_INSENSITIVE);
    //public static final Pattern BIND_VAR_PATTERN = Pattern.compile("(?:\\{+?(.*?):(.*?):(.*?)?\\})+?", Pattern.CASE_INSENSITIVE);
    
    public static final String providerPattern = "(?:!?\\{+?(.*?):(?:\\(+(.*?)\\)+)?:(.*?)?\\})";
    public static final String binderPattern = "(?:\\[(\\S+?)(?::(\\S+))?\\])?";
    
    public static final Pattern BIND_VAR_PATTERN = Pattern.compile(providerPattern + binderPattern, Pattern.CASE_INSENSITIVE);
    
    public static void main(String[] args) {
    	try {
    		ProviderToken pt = null;
    		for(int i = 0; i < 1; i++) {
    			//pt = ProviderToken.parse("{MINIMUM:XXX,YYY:A}[Binder:FFF,GGG]");
    			pt = ProviderToken.parse("tomorrow it will be !{SysTime:(+1d:MM/dd/yyyy HH:mm:ss):AA}");
    		}
			System.out.println("Config:" + pt.getProviderConfig());
			System.out.println("Instance Key:" + pt.getInstanceKey());
		} catch (InvalidProviderTokenFormat e) {
			e.printStackTrace();
		}
    }
	
	/**
	 * Parses the passed provider token and returns a validated ProviderToken instance.
	 * @param token The string token representing a bind provider instance.
	 * @return a validated ProviderToken instance
	 * @throws InvalidProviderTokenFormat thrown if the token cannot be parsed or is invalid.
	 */
	public static ProviderToken parse(String token) throws  InvalidProviderTokenFormat {
		if(token==null) throw new InvalidProviderTokenFormat("Passed token was null");
		ProviderToken pt = null;
		Matcher m = BIND_VAR_PATTERN.matcher(token);
		if(!m.find()) throw new InvalidProviderTokenFormat("No match found for token [" + token + "]");
		try {			
			pt = new ProviderToken(m.group(), m.group(1), m.group(2), m.group(3), m.group(4), m.group(5), m.group().startsWith("!"));			
			return pt;
		} catch (Exception e) {
			throw new InvalidProviderTokenFormat("Unexpected exception while parsing token [" + token + "]", e);
		}
	}
	
	/**
	 * Extracts any located tokens in a string.
	 * @param sentence A string potentially contaiing zero, one or more tokens.
	 * @return An array of located tokens.
	 * @throws InvalidProviderTokenFormat
	 */
	public static ProviderToken[] process(String sentence) throws  InvalidProviderTokenFormat {
		Set<ProviderToken> tokens = new HashSet<ProviderToken>();
		Matcher m = BIND_VAR_PATTERN.matcher(sentence);
		while(m.find()) {
			String token = m.group();
			if(LOG.isDebugEnabled()) {
				LOG.debug("process() found token [" + token + "]");
			}
			tokens.add(parse(token));
		}
		return tokens.toArray(new ProviderToken[tokens.size()]);
	}

	/**
	 * Creates a new provider token value object.
	 * @param providerToken
	 * @param providerTypeKey
	 * @param providerConfig
	 * @param instanceKey
	 * @param binderTokenKey
	 * @param binderConfig
	 * @!param forceNoBind Indicates the token started with a <code>!</code> meaning it is a forceNoBind.
	 * @throws  InvalidProviderTokenFormat
	 */
	private ProviderToken(String providerToken, String providerTypeKey, String providerConfig, String instanceKey, String binderTokenKey, String binderConfig, boolean forceNoBind) throws  InvalidProviderTokenFormat  {
		if(providerTypeKey==null || providerTypeKey.length() <1) throw new InvalidProviderTokenFormat("No value provided for mandatory field [Provider Type Key]");
		if(instanceKey==null) throw new InvalidProviderTokenFormat("No value provided for mandatory field [Instance Key]");
		this.providerToken = providerToken;
		this.providerTypeKey = providerTypeKey;	
		this.instanceKey = instanceKey;		
		this.providerConfig = ("".equals(providerConfig) ? null : providerConfig);
		this.binderTokenKey = ("".equals(binderTokenKey) ? null : binderTokenKey);
		this.binderConfig = ("".equals(binderConfig) ? null : binderConfig);
		if((this.binderConfig!=null && this.binderTokenKey==null) || (this.binderTokenKey!=null && this.binderTokenKey.startsWith(":") && this.binderConfig==null  ) ) throw new InvalidProviderTokenFormat("Binder configuration provided with no binder token key");
		this.forceNoBind = forceNoBind;
	}
	
	/**
	 * Returns true if a binder has been defined
	 * @return true if a binder has been defined
	 */
	public boolean isBinderDefined() {
		return binderTokenKey!=null;
	}
	
	/**
	 * Returns true if a binder has been defined and has config
	 * @return true if a binder with config has been defined
	 */
	public boolean isBinderConfigDefined() {
		return binderTokenKey!=null && binderConfig != null;
	}
	
	/**
	 * Returns true if the provider has config
	 * @return true if the provider has config
	 */
	public boolean isProviderConfigDefined() {
		return providerConfig != null;
	}
	
	
	/**
	 * Returns the whole binder token i.e. <code><b>binder token key</b>:<b>binder config</b></code>
	 * @return the binder token
	 */
	public String getBinderToken() {
		if(!isBinderDefined()) throw new RuntimeException("Cannot request binder token when no binder was defined");
		return this.binderTokenKey + (this.binderConfig==null ? "" : ":" + this.binderConfig);
	}

	/**
	 * The provider type key that uniquely identifies the provider class.
	 * @return the providerTypeKey
	 */
	public String getProviderTypeKey() {
		return providerTypeKey;
	}

	/**
	 * The provider implementation specific configuration string.
	 * @return the providerConfig
	 */
	public String getProviderConfig() {
		return providerConfig;
	}

	/**
	 * The provider instance key that uniquely identifies a provider instance.
	 * @return the instanceKey
	 */
	public String getInstanceKey() {
		return instanceKey;
	}

	/**
	 * The binder type key that unquely identifies the binder class that will override the provider's default binder.
	 * @return the binderTokenKey
	 */
	public String getBinderTokenKey() {
		return binderTokenKey;
	}

	/**
	 * The binder implementation specific configuration string.
	 * @return the binderConfig
	 */
	public String getBinderConfig() {
		return binderConfig;
	}

	/**
	 * Constructs a <code>String</code> with all attributes in name = value format.
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString()  {
	    final String TAB = "\n\t";
	    StringBuilder retValue = new StringBuilder("ProviderToken [");    
	    retValue.append(TAB).append("providerTypeKey=").append(this.providerTypeKey);    
	    retValue.append(TAB).append("providerConfig=").append(this.providerConfig);    
	    retValue.append(TAB).append("instanceKey=").append(this.instanceKey);    
	    retValue.append(TAB).append("binderTokenKey=").append(this.binderTokenKey);    
	    retValue.append(TAB).append("binderConfig=").append(this.binderConfig);    
	    retValue.append(TAB).append("forceNoBind=").append(this.forceNoBind);
	    retValue.append("\n]");
	    return retValue.toString();
	}

	/**
	 * Returns true if the provider token indicated a forceNoBind.
	 * @return the forceNoBind
	 */
	public boolean isForceNoBind() {
		return forceNoBind;
	}

	/**
	 * @return the providerToken
	 */
	public String getProviderToken() {
		return providerToken;
	}
}
