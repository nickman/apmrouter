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
package org.helios.apmrouter.metric.catalog.direct;

import static org.helios.apmrouter.util.Methods.nvl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


import org.helios.apmrouter.metric.IMetric;
import org.helios.apmrouter.metric.MetricType;
import org.helios.apmrouter.metric.catalog.IDelegateMetric;
import org.helios.apmrouter.metric.catalog.direct.chronicle.ChronicleController;
import org.helios.apmrouter.util.StringHelper;
import org.helios.apmrouter.util.SystemClock;
import org.helios.apmrouter.util.SystemClock.ElapsedTime;

import vanilla.java.chronicle.Excerpt;
import vanilla.java.chronicle.impl.IndexedChronicle;

/**
 * <p>Title: ChronicleICEMetric</p>
 * <p>Description: An {@link IDelegateMetric} implementation that reads its values from a java-chronicle</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.metric.catalog.direct.ChronicleICEMetric</code></p>
 */

public class ChronicleICEMetric implements IDelegateMetric {
	/** The excerpt to read fields from */
	protected final Excerpt<IndexedChronicle> excerpt;
	/** The offsets of the host, agent and metric names */
	protected final int[] nameOffsets;
	
	/** The offsets of the namespaces */
	protected final int[] namespaceOffsets;
	
	/** The chronicle index */
	protected final long index;

	/** The index of the offset of the flat indicator */
	public static final int FLAT_POS = 0;	
	/** The index of the offset of the hostname */
	public static final int HOST_POS = 1;
	/** The index of the offset of the agent */
	public static final int AGENT_POS = 2;
	/** The index of the offset of the metric name */
	public static final int NAME_POS = 3;
	/** The index of the offset of the unmapped delegate metric */
	public static final int UNMAPPED_POS = 4;
	
	/** An empty string array */
	private static final String[] EMPTY_STR_ARR = {};
	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (index ^ (index >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ChronicleICEMetric other = (ChronicleICEMetric) obj;
		if (index != other.index)
			return false;
		return true;
	}

	/**
	 * Creates a new ChronicleICEMetric
	 * @param index The chronicle index for this metric
	 * @param excerpt The excerpt to read fields from
	 * @param nameOffsets The offsets of the host, agent and metric names
	 * @param namespaceOffsets The offsets of the namespaces
	 */
	public ChronicleICEMetric(long index, Excerpt<IndexedChronicle> excerpt, int[] nameOffsets, int[] namespaceOffsets) {
		this.index = index;
		this.excerpt = excerpt;
		this.nameOffsets = nameOffsets;
		this.namespaceOffsets = namespaceOffsets;
	}
	
	/**
	 * Creates a new ChronicleICEMetric
	 * @param index The chronicle index for this metric
	 */
	public ChronicleICEMetric(long index) {
		this(ChronicleController.getInstance().createExcerpt(), index);
	}
	
	
	/**
	 * Creates a new ChronicleICEMetric
	 * @param ex The excerpt to use
	 * @param index The chronicle index for this metric
	 */
	public ChronicleICEMetric(Excerpt<IndexedChronicle> ex, long index) {
		this.index = index;
		excerpt = ex;
		if(!excerpt.index(index)) throw new IllegalStateException("No metric for index [" + index + "]", new Throwable());
		nameOffsets = new int[5];
		excerpt.position(0);
		excerpt.readLong(); // the token
		excerpt.readInt(); // the type
		namespaceOffsets = new int[excerpt.readInt()];  // offset is the token (8) and the type (4)
		nameOffsets[FLAT_POS] = excerpt.position();
		excerpt.readByte();
		//excerpt.skipBytes(9);  // skip two ints and a byte
		nameOffsets[HOST_POS] = excerpt.position();
		excerpt.skipBytes(excerpt.readInt());
		nameOffsets[AGENT_POS] = excerpt.position();
		excerpt.skipBytes(excerpt.readInt());
		nameOffsets[NAME_POS] = excerpt.position();
		excerpt.skipBytes(excerpt.readInt());
		for(int i = 0; i < namespaceOffsets.length; i++) {
			namespaceOffsets[i] = excerpt.readInt();
			excerpt.skipBytes(namespaceOffsets[i]);
		}
		nameOffsets[UNMAPPED_POS] = excerpt.position();
	}
	
	/**
	 * Writes a new metric to the chronicle. The chronicle entry format is: <ol>
	 * 	<li>An <b>int</b> for the MetricType<li>
	 *  <li>An <b>int</b> for the number of namespace entries<li>
	 *  <li>A <b>byte</b> indicating if the namespace is:<ol>
	 *  	<li><b>flat</b>:1</li>
	 *  	<li><b>mapped</b>:0</li>
	 *  </ol><li>
	 * 	<li>The host name as a <b>UTF String</b><li>
	 *  <li>The agent name as a <b>UTF String</b><li>
	 *  <li>The metric name as a <b>UTF String</b><li>
	 *  <li>The namespaces as <b>UTF String</b>s<li>
	 * </ol>
	 * @param host The metric host name
	 * @param agent The metric agent
	 * @param name The metric name
	 * @param type The metric type
	 * @param namespace The metric namespace
	 * @return the ChronicleICEMetric that reads its values from the chronicle
	 * FIXME: the record structure above is WAY out of date
	 */
	public static ChronicleICEMetric newInstance(String host, String agent, CharSequence name, MetricType type, CharSequence...namespace) {
		try {
			nvl(host, "Host Name");
			nvl(agent, "Agent Name");
			nvl(agent, "Metric Name");
			int exSize = 40 + 4 + 4 + 8;  // the size of the unmapped long reference, the ordinal int, the ns size int and 8 ints used to track the name lengths & offsets
			int[] nameOffsets = new int[4];
			int[] nameLengths = new int[]{0, host.trim().getBytes().length, agent.trim().getBytes().length, name.toString().trim().getBytes().length};
			int[] namespaceOffsets;
			int[] namespaceLengths;
			byte flat = 1;
			List<String> ns = new ArrayList<String>(namespace==null ? 0 : namespace.length);
			int offind = 0;
			if(namespace!=null) {
				for(CharSequence cs: namespace) {
					if(cs==null) continue;
					String s = cs.toString();
					if(s.trim().isEmpty()) continue;
					ns.add(s.trim());
					if(s.trim().indexOf('=')!=-1) flat = 0;
				}
			}			
			exSize += (ns.size()*8);  // Adding space for an additional 2 ints per namespace 
			namespaceOffsets = new int[ns.size()];
			namespaceLengths = new int[ns.size()];
			for(int i = 0; i < ns.size(); i++) {
				namespaceLengths[i] = ns.get(i).getBytes().length;
				exSize += namespaceLengths[i];
			}
			// Calculate the total size of the excerpt to be written
//			DataOutputStream daos = new DataOutputStream(new OutputStream(){
//				@Override
//				public void write(int b) throws IOException {}
//			});
//			daos.writeUTF(host);
//			daos.writeUTF(agent);
//			daos.writeUTF(name);			
//			for(String s: ns) {
//				daos.writeUTF(s);
//				if(s.indexOf('=')!=-1) flat = 0;
//			}
			
			Excerpt<IndexedChronicle> ex = ChronicleController.getInstance().createExcerpt();
			ex.startExcerpt(exSize+20);
			ex.position(0);
			ex.writeLong(-1L); // the initial token
			ex.writeInt(type.ordinal());
			ex.writeInt(ns.size());
			nameOffsets[FLAT_POS] = ex.position();
			ex.write(flat);
			nameOffsets[HOST_POS] = ex.position();
			ex.writeInt(nameLengths[HOST_POS]);
			ex.write(host.getBytes());
			nameOffsets[AGENT_POS] = ex.position();
			ex.writeInt(nameLengths[AGENT_POS]);
			ex.write(agent.getBytes());
			nameOffsets[NAME_POS] = ex.position();
			ex.writeInt(nameLengths[NAME_POS]);
			ex.write(name.toString().trim().getBytes());
			offind = 0;
			for(String s: ns) {
				namespaceOffsets[offind] = ex.position();
				ex.writeInt(namespaceLengths[offind]);
				ex.write(s.getBytes());
				offind++;				
			}
			ex.writeLong(-1L); // until an unmapped is requested
			ex.finish();
			return new ChronicleICEMetric(ex.index(), ex, nameOffsets, namespaceOffsets);
		} catch (Exception e) {
			throw new RuntimeException("Failed to create new ChronicleICEMetric", e);
		}		
	}
	
	/**
	 * Reads a string from the excerpt at the specified offset
	 * @param offset The offset to read the string from
	 * @return the read string
	 */
	protected String readString(int offset) {
		byte[] bytes = new byte[excerpt.readInt(offset)];
		excerpt.position(offset+4);
		excerpt.readFully(bytes);
		return new String(bytes);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.catalog.IDelegateMetric#getHost()
	 */
	@Override
	public String getHost() {
		return readString(nameOffsets[HOST_POS]);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.catalog.IDelegateMetric#getAgent()
	 */
	@Override
	public String getAgent() {
		return readString(nameOffsets[AGENT_POS]);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.catalog.IDelegateMetric#getName()
	 */
	@Override
	public String getName() {
		return readString(nameOffsets[NAME_POS]);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.catalog.IDelegateMetric#getType()
	 */
	@Override
	public MetricType getType() {
		return MetricType.valueOf(excerpt.readInt(8));
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.catalog.IDelegateMetric#getNamespace()
	 */
	@Override
	public String[] getNamespace() {
		if(namespaceOffsets.length==0) return EMPTY_STR_ARR;
		String[] ns = new String[namespaceOffsets.length];
		for(int i = 0; i < namespaceOffsets.length; i++) {
			ns[i] = readString(namespaceOffsets[i]);
		}
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
	 * @see org.helios.apmrouter.metric.catalog.IDelegateMetric#isFlat()
	 */
	@Override
	public boolean isFlat() {
		return excerpt.readByte(nameOffsets[FLAT_POS])==1;
	}
	
	/**
	 * Returns the serialization token for this IMetric
	 * @return the serialization token for this IMetric or -1 if one has not been assigned
	 */	
	@Override
	public long getToken() {
		return excerpt.readLong(0);
	}
	
	/**
	 * Returns the ID of the unmapped version of this metric.
	 * If this metric is not mapped, this will be -1
	 * @return the ID of the unmapped version of this metric.
	 */
	protected long getUnmappedId() {
		return excerpt.readLong(nameOffsets[UNMAPPED_POS]);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.catalog.IDelegateMetric#unmap()
	 */
	@Override
	public IDelegateMetric unmap() {
		if(!isMapped()) return this;
		long mappedId = getUnmappedId();
		ChronicleICEMetric unmapped = null;
		if(mappedId==-1) {
			synchronized(this) {
				mappedId = getUnmappedId();
				if(mappedId==-1) {
					String[] namespace = getNamespace();
					String[] unmappedNamespace = new String[namespace.length];
					for(int i = 0; i < namespace.length; i++) {
						int _index = namespace[i].indexOf("=");
						unmappedNamespace[i] = _index==-1 ? namespace[i] : namespace[i].substring(_index+1); 
					}					
					unmapped = newInstance(getHost(), getAgent(), getName(), getType(), unmappedNamespace);
					excerpt.writeLong(nameOffsets[UNMAPPED_POS], unmapped.index);
				}
			}
		}
		if(unmapped == null) {
			return new ChronicleICEMetric(mappedId);
		}
		return unmapped;
	}
	
	
	/**
	 * Sets the serialization token for this IMetric
	 * @param token the serialization token for this IMetric
	 */
	@Override
	public void setToken(long token) {
		if(!excerpt.index(index)) {
			throw new IllegalStateException("Failed to set index to [" + index + "]", new Throwable());
		}
		excerpt.writeLong(0, token);
//		excerpt.finish();
//		Excerpt<IndexedChronicle> ex = excerpt.chronicle().createExcerpt();
//		if(!ex.index(index)) {
//			throw new IllegalStateException("Failed to set index to [" + index + "] AFTER token write", new Throwable());
//		}
//
//		long readToken = ex.readLong(0); 
//		if(readToken!=token) {
//			throw new RuntimeException("Failed to validate token update. Set [" + token + "] but read back [" + readToken + "]", new Throwable());
//		}
		//excerpt.finish();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.catalog.IDelegateMetric#getSerSize()
	 */
	@Override
	public int getSerSize() {
		if(getToken()!=-1) return 8;
		// bigger than it needs to be, but fast
		return excerpt.length();
	}
	
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.catalog.IDelegateMetric#isMapped()
	 */
	@Override
	public boolean isMapped() {
		return excerpt.readByte(nameOffsets[FLAT_POS])==0;
	}
	
	
	
	public static void main(String[] args) {
		log("DirectMetric Test");
		System.setProperty("apmrouter.chronicle.retain", "true");
		int loopCount = 1000000;
//		int loopCount = 100;
		for(int i = 0; i < loopCount; i++) {
			ChronicleICEMetric.newInstance("MyHost" + i, "MyAgent", "metric" + i, MetricType.LONG_GAUGE, (i%2!=0) ? ("ns" + i) : ("ns" + i + "=foobar" + i));
		}
		ChronicleController.getInstance().clear();
		ChronicleController.getInstance().useUnsafe(true);
		log("Create Warmup complete");
		SystemClock.startTimer();
		for(int i = 0; i < loopCount; i++) {
			ChronicleICEMetric.newInstance("MyHost" + i, "MyAgent", "metric" + i, MetricType.LONG_GAUGE, (i%2!=0) ? ("ns" + i) : ("ns" + i + "=foobar" + i));
		}
		ElapsedTime et = SystemClock.endTimer();
		log("Create Test complete in " + et);
		log("Create Average Per:" + et.avgNs(loopCount) + " ns.");

		for(int i = 0; i < loopCount; i++) {
			IDelegateMetric dim = new ChronicleICEMetric(i);
			if(!("MyHost" + i).equals(dim.getHost())) {
				throw new RuntimeException("Invalid Metric. Got [" + dim.getHost() + "] expected [" + ("MyHost" + i) + "]", new Throwable());
			}
		}
		log("Lookup Warmup complete");

		SystemClock.startTimer();
		for(int i = 0; i < loopCount; i++) {
			IDelegateMetric dim = new ChronicleICEMetric(i);
			dim.setToken(i+1);
		}
		et = SystemClock.endTimer();
		log("Set Token Test complete in " + et);
		log("Set Token Average Per:" + et.avgNs(loopCount) + " ns.");
		log("Closing Chronicle .....");
		ChronicleController.getInstance().close();
		
		
		log("Closed Chronicle");
		long size = ChronicleController.getInstance().size();
		log("Re-opened chronicle with size:" + size);
		ChronicleController.getInstance().useUnsafe(true);
		
		
		
		
		SystemClock.startTimer();
		for(int i = 0; i < loopCount; i++) {
			IDelegateMetric dim = new ChronicleICEMetric(i);
			if(!("MyHost" + i).equals(dim.getHost())) {
				throw new RuntimeException("Invalid Metric. Got [" + dim.getHost() + "] expected [" + ("MyHost" + i) + "]", new Throwable());
			}
			if((i+1)!=dim.getToken()) {
				throw new RuntimeException("Invalid Token. Got [" + dim.getToken() + "] expected [" + (i+1) + "]", new Throwable());
			}
			
		}
		et = SystemClock.endTimer();
		log("Lookup Test complete in " + et);
		log("Lookup Average Per:" + et.avgNs(loopCount) + " ns.");
		
//		log("Done Creating");
//		for(ChronicleICEMetric dim: metrics) {
//			log(dim);
//		}
//		log("Done");
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ChronicleICEMetric (");
		builder.append(", host=");
		builder.append(getHost());
		builder.append(", agent=");
		builder.append(getAgent());
		builder.append(", name=");
		builder.append(getName());
		builder.append(", namespace=");
		builder.append(Arrays.toString(getNamespace()));
		builder.append(")");
		return builder.toString();
	}
	
	/**
	 * Renders the metric with some additional internal data for diagnostics
	 * @return a debug string render of the metric
	 */
	public String toDebugString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ChronicleICEMetric [");
		builder.append("offsets=");
		builder.append(Arrays.toString(nameOffsets));
		builder.append(", nsoffsets=");
		builder.append(Arrays.toString(namespaceOffsets));
		builder.append(", flat=");
		builder.append(isFlat());
		builder.append(", exSize=");
		builder.append(excerpt.length());
		builder.append(", exIndex=");
		builder.append(index);		
		
		builder.append(", host=");
		builder.append(getHost());
		builder.append(", agent=");
		builder.append(getAgent());
		builder.append(", name=");
		builder.append(getName());
		builder.append(", namespace=");
		builder.append(Arrays.toString(getNamespace()));
		builder.append(", unmappedId=");
		builder.append(getUnmappedId());
		
		builder.append("]");
		return builder.toString();
	}
	

	
	

}
