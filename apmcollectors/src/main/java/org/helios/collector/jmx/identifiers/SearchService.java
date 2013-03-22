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
package org.helios.collector.jmx.identifiers;

import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * <p>Title: SearchService</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.collectors.jmx.identifiers.SearchService</code></p>
 */
@ManagedResource
public class SearchService {
/*	*//** Instance logger *//*
	protected final Logger log = Logger.getLogger(getClass());

	*//**
	 * Starts the service and registers the MBean
	 * @throws Exception
	 *//*
	public void start() throws Exception {
		info("\n\t=====================\n\tStarting MBeanServerConnection Search Service\n\t=====================\n");
		JMXHelper.getHeliosMBeanServer().registerMBean(this, JMXHelper.objectName(new StringBuilder(IMBeanServerConnectionFactory.CONNECTION_MBEAN_DOMAIN).append(":service=SearchService")));
		
		info("\n\t=====================\n\tStarted MBeanServerConnection Search Service\n\t=====================\n");
	}
	
	*//**
	 * Returns an array of hosts that match the passed pattern. If the pattern is blank or null,
	 * the pattern will be <b><code>.*</code></b>.
	 * @param pattern The host matching pattern
	 * @return an array of matching host names
	 *//*
	@ManagedOperation 
	public String[] getHosts(String pattern) {
		return search(IMBeanServerConnectionFactory.PROP_KEY_HOST, pattern);
	}
	
	*//**
	 * Returns an array of vms that match the passed pattern. If the pattern is blank or null,
	 * the pattern will be <b><code>.*</code></b>.
	 * @param host The target host
	 * @param pattern The vm matching pattern
	 * @return an array of matching vm names
	 *//*
	@ManagedOperation 
	public String[] getVMs(String host, String pattern) {
		return search(IMBeanServerConnectionFactory.PROP_KEY_VM, pattern, IMBeanServerConnectionFactory.PROP_KEY_HOST + "=" + host);
	}
	
	*//**
	 * Returns an array of MBeanServer default domains that match the passed pattern. If the pattern is blank or null,
	 * the pattern will be <b><code>.*</code></b>.
	 * @param host The target host
	 * @param vm the target vm name
	 * @param pattern The MBeanServer default domains matching pattern
	 * @return an array of matching MBeanServer default domains
	 *//*
	@ManagedOperation 
	public String[] getDefaultDomains(String host, String vm, String pattern) {
		return search(IMBeanServerConnectionFactory.PROP_KEY_DOMAIN, pattern, IMBeanServerConnectionFactory.PROP_KEY_HOST + "=" + host, IMBeanServerConnectionFactory.PROP_KEY_VM + "=" + vm);
	}
	
	*//**
	 * Parameterized lookup
	 * @param type
	 * @param pattern
	 * @param modifiers
	 * @return
	 *//*
	@ManagedOperation
	public Map<String, String[]> getSpec(String type, String pattern, String...args) {
		Map<String, String> modifiers = new HashMap<String, String>(args!=null ? args.length : 0);
		if(args!=null){
			for(String s: args) {
				if("".equals(s) || !s.contains("=")) continue;
				String[] frags = s.trim().split("=");
				modifiers.put(frags[0], frags[1]);
			}
		}
		Map<String, String[]> result = new HashMap<String, String[]>(1);
		if(IMBeanServerConnectionFactory.PROP_KEY_HOST.equals(type)) {
			result.put(IMBeanServerConnectionFactory.PROP_KEY_HOST, this.getHosts(pattern));
			return result;
		} else if(IMBeanServerConnectionFactory.PROP_KEY_VM.equals(type)) {
			if(!modifiers.containsKey(IMBeanServerConnectionFactory.PROP_KEY_HOST)) throw new IllegalArgumentException("Modifiers did not contain a host");
			result.put(IMBeanServerConnectionFactory.PROP_KEY_VM, this.getVMs(modifiers.get(IMBeanServerConnectionFactory.PROP_KEY_HOST), pattern));
			return result;
		} else if(IMBeanServerConnectionFactory.PROP_KEY_DOMAIN.equals(type)) {
			if(!modifiers.containsKey(IMBeanServerConnectionFactory.PROP_KEY_HOST)) throw new IllegalArgumentException("Modifiers did not contain a host"); 
			if(!modifiers.containsKey(IMBeanServerConnectionFactory.PROP_KEY_VM)) throw new IllegalArgumentException("Modifiers did not contain a vm");
			result.put(IMBeanServerConnectionFactory.PROP_KEY_DOMAIN, this.getDefaultDomains(modifiers.get(IMBeanServerConnectionFactory.PROP_KEY_HOST), modifiers.get(IMBeanServerConnectionFactory.PROP_KEY_VM), pattern));
			return result;
		} else if(IMBeanServerConnectionFactory.PROP_KEY_SUB_DOMAIN.equals(type)) {
			if(!modifiers.containsKey(IMBeanServerConnectionFactory.PROP_KEY_HOST)) throw new IllegalArgumentException("Modifiers did not contain a host"); 
			if(!modifiers.containsKey(IMBeanServerConnectionFactory.PROP_KEY_VM)) throw new IllegalArgumentException("Modifiers did not contain a vm");
			if(!modifiers.containsKey(IMBeanServerConnectionFactory.PROP_KEY_DOMAIN)) throw new IllegalArgumentException("Modifiers did not contain a default domain");
			result.put(IMBeanServerConnectionFactory.PROP_KEY_SUB_DOMAIN, this.getSubDomains(modifiers.get(IMBeanServerConnectionFactory.PROP_KEY_HOST), modifiers.get(IMBeanServerConnectionFactory.PROP_KEY_VM), modifiers.get(IMBeanServerConnectionFactory.PROP_KEY_DOMAIN), pattern));
			return result;
		} else {
			throw new IllegalArgumentException("Invalid Type [" + type + "]");
		}
	}	
	
	*//**
	 * Returns an array of subdomains that match the passed pattern. If the pattern is blank or null,
	 * the pattern will be <b><code>.*</code></b>.
	 * @param host The target host
	 * @param vm the target vm name
	 * @param defaultDomain The target default domain
	 * @param pattern The MBeanServer subdomains matching pattern
	 * @return an array of matching MBeanServer subdomains
	 *//*
	@ManagedOperation 
	public String[] getSubDomains(String host, String vm, String defaultDomain, String pattern) {
		StringBuilder b = new StringBuilder(IMBeanServerConnectionFactory.CONNECTION_MBEAN_DOMAIN).append(":");
		b.append(IMBeanServerConnectionFactory.PROP_KEY_HOST + "=" + host + ","); b.append(IMBeanServerConnectionFactory.PROP_KEY_VM + "=" + vm + ","); b.append(IMBeanServerConnectionFactory.PROP_KEY_DOMAIN + "=" + defaultDomain);
		String[] matches = null;
		try {
			 matches = (String[])server.getAttribute(JMXHelper.objectName(b), "Domains");
		} catch (Exception e) {
			throw new RuntimeException("Failed to get domain names for [" + b.toString() + "]", e);
		}
		if(pattern==null || pattern.equals("") || pattern.equals(".*")) {
			return matches;
		} else {
			Set<String> domains = new HashSet<String>();
			Pattern p = Pattern.compile(pattern);
			for(String s: matches) {
				if(p.matcher(s).matches()) {
					domains.add(s);
				}
			}
			return domains.toArray(new String[domains.size()]);
		}
	}
	
	*//**
	 * Genericized search 
	 * @param typeKey The prop key type
	 * @param pattern The match on the target type
	 * @param preDefs Any predefined match keys (key=value)
	 * @return An array of matching items
	 *//*
	protected String[] search(String typeKey, String pattern, String...preDefs) {
		if(typeKey==null || !IMBeanServerConnectionFactory.PROP_KEYS.contains(typeKey.toLowerCase())) {
			throw new RuntimeException("Invalid type key [" + typeKey + "]", new Throwable());
		}
		
		StringBuilder _pattern = new StringBuilder(IMBeanServerConnectionFactory.CONNECTION_MBEAN_DOMAIN).append(":");		
		if(pattern==null || "".equals(pattern)) {
			pattern = "*";		
		}
		boolean wildcard = RegexHelper.containsUnescapedReserved(pattern) && "*".equals(pattern);
		if(preDefs!=null && preDefs.length > 0) {
			for(String preDef: preDefs) {
				_pattern.append(preDef).append(",");
			}
		}
		if(wildcard) {
			_pattern.append(typeKey).append("=*");
		} else {
			_pattern.append(typeKey).append("=").append(pattern);
		}
		_pattern.append(",*");
		
		Set<ObjectName> matches = server.queryNames(JMXHelper.objectName(_pattern), null);
		int sz = matches.size();
		if(sz<1) return new String[0];
		Set<String> matchNames = new HashSet<String>(sz);
		for(ObjectName on: matches) {
			matchNames.add(on.getKeyProperty(typeKey));
		}
		if(wildcard) {
			Pattern p = Pattern.compile(pattern);
			for(Iterator<String> iter = matchNames.iterator(); iter.hasNext(); ) {
				if(!p.matcher(iter.next()).matches()) {
					iter.remove();
				}
			}
		}
		return matchNames.toArray(new String[matchNames.size()]);
	}*/
}
