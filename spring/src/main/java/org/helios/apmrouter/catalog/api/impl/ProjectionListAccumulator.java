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
package org.helios.apmrouter.catalog.api.impl;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.criterion.Projection;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;

/**
 * <p>Title: ProjectionListAccumulator</p>
 * <p>Description: An accumulator for projections</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.catalog.api.impl.ProjectionListAccumulator</code></p>
 */

public class ProjectionListAccumulator implements Parsed<ProjectionList> {
	/** The projection list to accumulate against */
	protected final ProjectionList projectionList = Projections.projectionList();
	
	/** The RowCount projection. Requires no params. */
	public static final String PROJ_ROWCOUNT = "rc";
	/** The average projection. Param: the property to average */
	public static final String PROJ_AVG = "avg";
	/** The max projection. Param: the property to max */
	public static final String PROJ_MAX = "max";
	/** The min projection. Param: the property to min */
	public static final String PROJ_MIN = "min";
	/** The count projection. Param: the property to count */
	public static final String PROJ_COUNT = "cnt";
	/** The count distinct projection. Param: the property to distinct count */
	public static final String PROJ_COUNTDIST = "cntd";
	/** The group projection. Param: the property to group */
	public static final String PROJ_GRP = "grp";
	/** The sum projection. Param: the property to sum */
	public static final String PROJ_SUM = "sum";
	
	/** A map of projections method handles keyed by op name */
	private static Map<String, MethodHandle> projectionHandles = new HashMap<String, MethodHandle>();
	
	static {
		saveHandle(PROJ_SUM, Projections.class, "sum", String.class);		
		saveHandle(PROJ_GRP, Projections.class, "groupProperty", String.class);
		saveHandle(PROJ_COUNTDIST, Projections.class, "countDistinct", String.class);
		saveHandle(PROJ_COUNT, Projections.class, "count", String.class);
		saveHandle(PROJ_MIN, Projections.class, "min", String.class);
		saveHandle(PROJ_MAX, Projections.class, "max", String.class);
		saveHandle(PROJ_AVG, Projections.class, "avg", String.class);
		saveHandle(PROJ_ROWCOUNT, Projections.class, "rowCount");
		System.out.println("Op Codes:" + projectionHandles.keySet());
	}
	
	public static void main(String[] args) {}
	
	private static void saveHandle(String key, Class<?> clazz, String name, Class<?>...pattern){
		try {
			Method m = null;
			try {
				m = clazz.getDeclaredMethod(name, pattern);
			} catch (NoSuchMethodException nex) {
				m = clazz.getMethod(name, pattern);
			}
			saveHandle(key, m);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	private static void saveHandle(String dkey, Method method){
		try {
			MethodType desc = MethodType.methodType(method.getReturnType(), method.getParameterTypes());
			MethodHandle mh = MethodHandles.lookup().findStatic(method.getDeclaringClass(), method.getName(), desc);
			projectionHandles.put(dkey, mh);			
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
			throw new RuntimeException(ex);
		}
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.catalog.api.impl.Parsed#applyPrimitive(java.lang.String, java.lang.Object)
	 */
	@Override
	public Parsed<ProjectionList> applyPrimitive(String op, Object value) {
		MethodHandle mh = projectionHandles.get(op);
		Projection projection = null;
		try {
			if(mh.type().parameterCount()<1) {
				projection = (Projection) mh.invoke();				
			} else {
				if(value.getClass().isArray()) {
					int argCnt = Array.getLength(value);
					if(argCnt==1) {
						projection = (Projection)mh.invoke(Array.get(value, 0));
					} else {
						Object[] args = new Object[argCnt];
						System.arraycopy(value, 0, args, 0, args.length);
						projection = (Projection)mh.invoke(args);
					}					
				} else {
					projection = (Projection)mh.invoke(value);
				}
				
			}
			projectionList.add(projection);
		} catch (Throwable ex) {
			throw new RuntimeException(ex);
		}
		return this;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.catalog.api.impl.Parsed#get()
	 */
	@Override
	public ProjectionList get() {
		return projectionList;
	}

}
