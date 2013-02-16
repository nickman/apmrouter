/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2012, Helios Development Group and individual contributors
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
package org.helios.apmrouter.catalog.api.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * <p>Title: ArrayAccumulator</p>
 * <p>Description: Parsed compliant array accumulator</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.catalog.api.impl.ArrayAccumulator</code></p>
 */

public class ArrayAccumulator implements Parsed<Object[]>, List<Object> {
	/** The values accumulated set */
	protected List<Object> values = new ArrayList<Object>();
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.catalog.api.impl.Parsed#applyPrimitive(java.lang.String, java.lang.Object)
	 */
	@Override
	public Parsed<Object[]> applyPrimitive(String op, Object value) {
		values.add(value);
		return this;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.catalog.api.impl.Parsed#get()
	 */
	@Override
	public Object[] get() {	
		return toArray(new Object[size()]);
	}	
	
	public int size() {
		return values.size();
	}
	public boolean isEmpty() {
		return values.isEmpty();
	}
	public boolean contains(Object o) {
		return values.contains(o);
	}
	public Iterator<Object> iterator() {
		return values.iterator();
	}
	public Object[] toArray() {
		return values.toArray();
	}
	public <T> T[] toArray(T[] a) {
		return values.toArray(a);
	}
	public boolean add(Object e) {
		return values.add(e);
	}
	public boolean remove(Object o) {
		return values.remove(o);
	}
	public boolean containsAll(Collection<?> c) {
		return values.containsAll(c);
	}
	public boolean addAll(Collection<? extends Object> c) {
		return values.addAll(c);
	}
	public boolean addAll(int index, Collection<? extends Object> c) {
		return values.addAll(index, c);
	}
	public boolean removeAll(Collection<?> c) {
		return values.removeAll(c);
	}
	public boolean retainAll(Collection<?> c) {
		return values.retainAll(c);
	}
	public void clear() {
		values.clear();
	}
	public boolean equals(Object o) {
		return values.equals(o);
	}
	public int hashCode() {
		return values.hashCode();
	}
	public Object get(int index) {
		return values.get(index);
	}
	public Object set(int index, Object element) {
		return values.set(index, element);
	}
	public void add(int index, Object element) {
		values.add(index, element);
	}
	public Object remove(int index) {
		return values.remove(index);
	}
	public int indexOf(Object o) {
		return values.indexOf(o);
	}
	public int lastIndexOf(Object o) {
		return values.lastIndexOf(o);
	}
	public ListIterator<Object> listIterator() {
		return values.listIterator();
	}
	public ListIterator<Object> listIterator(int index) {
		return values.listIterator(index);
	}
	public List<Object> subList(int fromIndex, int toIndex) {
		return values.subList(fromIndex, toIndex);
	}



}
