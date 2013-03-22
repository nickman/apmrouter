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
package org.helios.collector.jdbc.utils;

import java.util.*;
import java.util.Map.Entry;

/**
 * <p>Title: DualKeyMap</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class DualKeyMap<K, A, V>  {
	protected Map<K, V> kmap  = null;
	protected Map<A, V> amap  = null;
	protected Map<K, A> ktoa = null;
	protected Map<A, K> atok = null;
	protected Class<K> kClass;
	protected Class<A> aClass;
	
	

	/**
	 * Creates a default size map.
	 */
	public DualKeyMap(Class<K> primaryClass, Class<A> alternateClass ) {
		this(primaryClass, alternateClass, 32);
	}
	
	/**
	 * Creates a sized map.
	 * @param size The initial size of the map.
	 */
	public DualKeyMap(Class<K> primaryClass, Class<A> alternateClass, int size) {
		kClass = primaryClass;
		aClass = alternateClass;
		kmap  = new HashMap<K, V>(size);
		amap = new HashMap<A, V>(size);
		ktoa = new HashMap<K, A>(size);
		atok = new HashMap<A, K>(size);
	}
	
	
	public static void main(String[] args) {
//		DualKeyMap<Integer, String, Object> dkm = new DualKeyMap<Integer, String, Object>(Integer.class, String.class);
//		dkm.put(1, "One", "A");
//		dkm.put(2, "Two", "B");
//		dkm.put(3, "Three", "C");
//		assert dkm.kmap.size()==3;
//		assert dkm.amap.size()==3;
//		assert dkm.ktoa.size()==3;
//		assert dkm.atok.size()==3;
//
//		assert dkm.get(2).equals("B");
//		assert dkm.get("Two").equals("B");
//		assert !dkm.get(1).equals("B");
//		assert !dkm.get("One").equals("B");
//		assert dkm.remove(1).equals("A");
//		assert dkm.remove("Three").equals("C");
//		assert dkm.kmap.size()==1;
//		assert dkm.amap.size()==1;
//		assert dkm.ktoa.size()==1;
//		assert dkm.atok.size()==1;
//		dkm.clear();
//		assert dkm.kmap.size()==0;
//		assert dkm.amap.size()==0;
//		assert dkm.ktoa.size()==0;
//		assert dkm.atok.size()==0;
//
//		dkm.put(1, "One", "A");
//		dkm.put(2, "Two", "B");
//		dkm.put(3, "Three", "C");
//		Map map = new HashMap();
//		map.put("One", "A");
//		map.put("Two", "B");
//		map.put("Three", "C");
//
//		assert dkm.entrySet(String.class).toString().equals(map.entrySet().toString());
//		assert dkm.entrySet(String.class).equals(map.entrySet());
//		assert !dkm.entrySet(Integer.class).toString().equals(map.entrySet().toString());
//		assert !dkm.entrySet(Integer.class).equals(map.entrySet());
//
//
//		map.clear();
//		map.put(1, "A");
//		map.put(2, "B");
//		map.put(3, "C");
//
//		assert dkm.entrySet(Integer.class).toString().equals(map.entrySet().toString());
//		assert dkm.entrySet(Integer.class).equals(map.entrySet());
//		assert !dkm.entrySet(String.class).toString().equals(map.entrySet().toString());
//		assert !dkm.entrySet(String.class).equals(map.entrySet());
//
//		dkm.clear();
//		map.clear();
//
//		assert dkm.entrySet(Integer.class).toString().equals(map.entrySet().toString());
//		assert dkm.entrySet(Integer.class).equals(map.entrySet());
//		assert dkm.entrySet(String.class).toString().equals(map.entrySet().toString());
//		assert dkm.entrySet(String.class).equals(map.entrySet());
//
//
//
//		log("All assertions passed");
//
		
	}
	
	
	public static void log(Object o) {
		System.out.println(o);
	}

	/**
	 * 
	 * @see java.util.Map#clear()
	 */	
	public void clear() {
		kmap.clear();
		amap.clear();
		ktoa.clear();
		atok.clear();
		
	}

	/**
	 * @param key
	 * @return
	 * @see java.util.Map#containsKey(java.lang.Object)
	 */
	
	public boolean containsKey(Object key) {
		if(kClass.isInstance(key)) {
			return kmap.containsKey(key);
		} else {
			return amap.containsKey(key);
		}
	}

	/**
	 * @param value
	 * @return
	 * @see java.util.Map#containsValue(java.lang.Object)
	 */
	
	public boolean containsValue(Object value) {		
		return kmap.containsValue(value) || amap.containsValue(value);
	}

	/**
	 * @return
	 * @see java.util.Map#entrySet()
	 */
	
	public Set<Map.Entry<K, V>> primaryEntrySet() {
		return kmap.entrySet();
	}
	
	/**
	 * @return
	 * @see java.util.Map#entrySet()
	 */
	
	public Set<Map.Entry<A, V>> alternateEntrySet() {
		return amap.entrySet();
	}
	
	/**
	 * @return
	 * @see java.util.Map#entrySet()
	 */
	
	@SuppressWarnings("unchecked")
	public Set<Entry> entrySet(Class<?> keyType) {
		Map map = new HashMap();
		if(kClass.isAssignableFrom(keyType)) {
			map.putAll(kmap);
		} else {
			map.putAll(amap);
		}
		return map.entrySet();
		
	}
	
	

	/**
	 * @param key
	 * @return
	 * @see java.util.Map#get(java.lang.Object)
	 */
	
	public V get(Object key) {
		if(kClass.isInstance(key)) {
			return kmap.get(key);
		} else {
			return amap.get(key);
		}
	}

	/**
	 * @return
	 * @see java.util.Map#isEmpty()
	 */
	
	public boolean isEmpty() {
		return kmap.isEmpty() && amap.isEmpty();
	}

	/**
	 * @return
	 * @see java.util.Map#keySet()
	 */
	
	@SuppressWarnings("unchecked")
	public Set keySet(Class<?> keyType) {
		Set set = new HashSet();
		if(kClass.isAssignableFrom(keyType)) {
			set.addAll(kmap.keySet());
		} else {
			set.addAll(amap.keySet());
		}
		return set;
	}
	
	public Set<K> primaryKeySet() {
		return kmap.keySet();
	}
	
	public Set<A> alternateKeySet() {
		return amap.keySet();
	}
	

	/**
	 * @param key
	 * @param akey
	 * @param value
	 * @return
	 */
	public V put(K key, A akey, V value) {
		atok.put(akey, key);
		ktoa.put(key, akey);
		amap.put(akey, value);
		return kmap.put(key, value);
	}

	/**
	 * @param m
	 * @see java.util.Map#putAll(java.util.Map)
	 */
	
	public void putAll(DualKeyMap<? extends K, ? extends A, ? extends V> m) {
		kmap.putAll(m.kmap);
		amap.putAll(m.amap);		
	}

	/**
	 * @param key
	 * @return
	 * @see java.util.Map#remove(java.lang.Object)
	 */
	
	public V remove(Object key) {
		V val = null;
		if(kClass.isInstance(key)) {
			val = kmap.remove(key);
			amap.remove(ktoa.get(key));
			atok.remove(ktoa.remove(key));
		} else {
			val = amap.remove(key);
			kmap.remove(atok.get(key));		
			ktoa.remove(atok.remove(key));
		}
		return val;
	}

	/**
	 * @return
	 * @see java.util.Map#size()
	 */
	
	public int size() {
		return kmap.size();
	}

	/**
	 * @return
	 * @see java.util.Map#values()
	 */
	
	public Collection<V> values() {
		return kmap.values();
	}

}
