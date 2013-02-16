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
package org.helios.apmrouter.catalog.jdbc.h2;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;

import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;

/**
 * <p>Title: StringArrayDataType</p>
 * <p>Description: Hibernate custom type for String arrays</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.catalog.jdbc.h2.StringArrayDataType</code></p>
 */

public class StringArrayDataType implements UserType {
	private static final int[] SQL_TYPES = { Types.JAVA_OBJECT };
	/**
	 * {@inheritDoc}
	 * @see org.hibernate.usertype.UserType#sqlTypes()
	 */
	@Override
	public int[] sqlTypes() {
		return SQL_TYPES;
	}

	/**
	 * {@inheritDoc}
	 * @see org.hibernate.usertype.UserType#returnedClass()
	 */
	@Override
	public Class<?> returnedClass() {
		return String[].class;
	}

	/**
	 * {@inheritDoc}
	 * @see org.hibernate.usertype.UserType#equals(java.lang.Object, java.lang.Object)
	 */
	@Override
	public boolean equals(Object x, Object y) throws HibernateException {
		if (x == y)
            return true;
        else if (x == null || y == null)
            return false;
        else if (!x.getClass().isArray() || !y.getClass().isArray()) {
        	return false;
        }
        return Arrays.equals((String[])x, (String[])y);		
	}

	/**
	 * {@inheritDoc}
	 * @see org.hibernate.usertype.UserType#hashCode(java.lang.Object)
	 */
	@Override
	public int hashCode(Object x) throws HibernateException {
		return Arrays.hashCode((String[])x);
	}

	/**
	 * {@inheritDoc}
	 * @see org.hibernate.usertype.UserType#nullSafeGet(java.sql.ResultSet, java.lang.String[], java.lang.Object)
	 */
	@Override
	public Object nullSafeGet(ResultSet rs, String[] names, Object owner) throws HibernateException, SQLException {
		String[] result = null;
        Object array = rs.getObject(names[0]);
        if (rs.wasNull()) {
            return result;
        }
    	int len = Array.getLength(array);
    	result = new String[len];
    	for(int i = 0; i < len; i++) {
    		result[i] = Array.get(array, i).toString();
    	}
    	return result;
        
        
	}

	/**
	 * {@inheritDoc}
	 * @see org.hibernate.usertype.UserType#nullSafeSet(java.sql.PreparedStatement, java.lang.Object, int)
	 */
	@Override
	public void nullSafeSet(PreparedStatement st, Object value, int index) throws HibernateException, SQLException {
		if (value == null)
            st.setNull(index, SQL_TYPES[0]);
        else {
            st.setObject(index, (String[])value);
        }

	}

	/**
	 * {@inheritDoc}
	 * @see org.hibernate.usertype.UserType#deepCopy(java.lang.Object)
	 */
	@Override
	public Object deepCopy(Object value) throws HibernateException {
		return Arrays.copyOf((String[])value, java.lang.reflect.Array.getLength(value));
	}

	/**
	 * {@inheritDoc}
	 * @see org.hibernate.usertype.UserType#isMutable()
	 */
	@Override
	public boolean isMutable() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 * @see org.hibernate.usertype.UserType#disassemble(java.lang.Object)
	 */
	@Override
	public Serializable disassemble(Object value) throws HibernateException {
		return (String[])value;
	}

	/**
	 * {@inheritDoc}
	 * @see org.hibernate.usertype.UserType#assemble(java.io.Serializable, java.lang.Object)
	 */
	@Override
	public Object assemble(Serializable cached, Object owner) throws HibernateException {
		return (String[])cached;
	}

	/**
	 * {@inheritDoc}
	 * @see org.hibernate.usertype.UserType#replace(java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public Object replace(Object original, Object target, Object owner) throws HibernateException {
		return target;
	}

}
