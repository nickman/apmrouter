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
package org.helios.apmrouter.metric.catalog.heap;

import static org.helios.apmrouter.util.Methods.nvl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.helios.apmrouter.metric.IMetric;
import org.helios.apmrouter.metric.MetricType;
import org.helios.apmrouter.metric.catalog.IDelegateMetric;
import org.helios.apmrouter.util.StringHelper;

/**
 * <p>Title: HeapICEMetric</p>
 * <p>Description: An {@link IDelegateMetric} implementation which is a simple pojo and stored in heap.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.metric.catalog.heap.HeapICEMetric</code></p>
 */

public class HeapICEMetric implements IDelegateMetric {
	/** The host name */
	protected final String host;
	/** The agent name */
	protected final String agent;
	/** The metricId namespace */
	protected final String[] namespace;
	/** The metricId name */
	protected final String name;
	/** The metricId type */
	protected final MetricType type;
	/** Indicates if this is a flat or mapped metricId name */
	protected final boolean flat;
	/** The serialization token */
	protected transient long token = -1;
	/** The byte size of the metricId, held until the token is received */
	protected transient volatile int byteSize = -1;
	/** The unmapped instance of a mapped metric ID */
	protected IDelegateMetric unmapped = null;

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("HeapICEMetric [host=");
		builder.append(host);
		builder.append(", agent=");
		builder.append(agent);
		builder.append(", namespace=");
		builder.append(Arrays.toString(namespace));
		builder.append(", name=");
		builder.append(name);
		builder.append(", type=");
		builder.append(type);
		builder.append("]");
		return builder.toString();
	}

	/**
	 * Creates a new HeapICEMetric
	 * @param host The host name
	 * @param agent The agent name
	 * @param name The metricId name
	 * @param type The metricId type
	 * @param namespace An optional array of namespace entries
	 */
	HeapICEMetric(String host, String agent, CharSequence name, MetricType type, CharSequence... namespace) {
		this.host = nvl(host, "Host Name");
		this.agent = nvl(agent, "Agent Name");
		this.name = nvl(name, "Metric Name").toString();
		this.type = nvl(type, "Metric Type");
		final List<String> ns = new ArrayList<String>();		
		this.flat = processNamespace(ns, namespace);
		this.namespace = ns.toArray(new String[ns.size()]);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.catalog.IDelegateMetric#unmap()
	 * FIXME: All this stuff should be in a common abstract class
	 */
	public IDelegateMetric unmap() {
		if(!isMapped()) return this;
		if(unmapped==null) {
			String[] unmappedNamespace = new String[namespace.length];
			for(int i = 0; i < namespace.length; i++) {
				int index = namespace[i].indexOf("=");
				unmappedNamespace[i] = index==-1 ? namespace[i] : namespace[i].substring(index+1); 
			}
			unmapped = new HeapICEMetric(host, agent, name, type, unmappedNamespace);
		}
		return unmapped;
	}
	
	/**
	 * Writes the supplied namespace into the passed string array list, validating and converting each namespace entry  
	 * @param ns The string array list to write to
	 * @param namespace The namespace value
	 * @return true if the namespace is flat, false if it is mapped
	 */
	private boolean processNamespace(final List<String> ns, CharSequence...namespace) {
		Boolean f = null;
		if(namespace==null || namespace.length<1) return true;
		int index = 0;
		for(CharSequence nms: namespace) {
			if(nms==null) continue;
			String s = nms.toString().trim();
			if(s.isEmpty()) continue;
			if(s.indexOf(" ")!=-1) s.replace(" ", "");
			int mindex = s.indexOf('=');
			if(f==null) {
				f = mindex==-1;
				if(f) {
					ns.add(s);
				} else {
					ns.add(validateMapped(s, mindex));
				}
			} else {
				if(f) {
					if(mindex==-1) {
						ns.add(s);
					} else {
						ns.add(merge(s, mindex));
					}
				} else {
					if(mindex==-1) {
						ns.add(String.format("ns%s=%s", index, s));
					} else {
						ns.add(validateMapped(s, mindex));
					}
				}
			}
			index++;
		}
		return f==null ? true : f;
	}
	
	/**
	 * Merges a mapped namespace entry into a flat namespace.
	 * If either the key or the value are empty, throws an exceptiom
	 * @param ns The namespace to merge
	 * @param index The index of the MDELIM
	 * @return the merged namespace
	 */
	private String merge(String ns, int index) {			
		return new StringBuilder(ns.substring(0, index)).append(ns.substring(index-1)).toString();
	}
	
	
	/**
	 * Validates a mapped namespace entry 
	 * If either the key or the value are empty, throws an exceptiom
	 * @param ns The namespace to validate
	 * @param index The index of the MDELIM
	 * @return the validated string
	 * @throws RuntimeException  Thrown if the key or the value is empty
	 */
	private String validateMapped(String ns, int index) {
		if(ns.substring(0, index).isEmpty() || ns.substring(index-1).isEmpty()) {
			throw new RuntimeException("Mapped namespace entry [" + ns + "] had empty key or value", new Throwable());			}
		return ns;
	}	
	
	/**
	 * {@inheritDoc} 
	 * @see org.helios.apmrouter.metric.catalog.IDelegateMetric#getNamespaceF()
	 */
	public String getNamespaceF() {
		String[] ns = getNamespace();
		if(ns.length==0) return "";
		return StringHelper.fastConcatAndDelim(IMetric.NSDELIM, ns);
	}
	
	/**
	 * {@inheritDoc} 
	 * @see org.helios.apmrouter.metric.catalog.IDelegateMetric#getFQN()
	 */
	public String getFQN() {
		return String.format(FQN_FORMAT, getHost(), getAgent(), getNamespaceF(), getName());
	}	
	
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.catalog.IDelegateMetric#getHost()
	 */
	@Override
	public String getHost() {
		return host;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.catalog.IDelegateMetric#getAgent()
	 */
	@Override
	public String getAgent() {
		return agent;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.catalog.IDelegateMetric#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.catalog.IDelegateMetric#getNamespace()
	 */
	@Override
	public String[] getNamespace() {
		return namespace;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.catalog.IDelegateMetric#isFlat()
	 */
	@Override
	public boolean isFlat() {
		return flat;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.catalog.IDelegateMetric#isMapped()
	 */
	@Override
	public boolean isMapped() {
		return !flat;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.catalog.IDelegateMetric#getType()
	 */
	@Override
	public MetricType getType() {
		return type;
	}
	
	/**
	 * Returns the serialization token for this IMetric
	 * @return the serialization token for this IMetric or -1 if one has not been assigned
	 */	
	@Override
	public long getToken() {
		return token;
	}
	
	/**
	 * Sets the serialization token for this IMetric
	 * @param token the serialization token for this IMetric
	 */
	@Override
	public void setToken(long token) {
		this.token = token;
		byteSize = -1;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.catalog.IDelegateMetric#getSerSize()
	 */
	@Override
	public int getSerSize() {
		if(getToken()!=-1) return 8;
		if(byteSize!=-1) return byteSize;
		byteSize = getFQN().getBytes().length + 4; // just the FQN and +4 for the int size . Type is serialized seperately		
		return byteSize;
	}
	

}
