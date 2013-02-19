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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.helios.collector.jdbc.binding.binder.Binder;
import org.helios.collector.jdbc.binding.binder.IBinder;

/**
 * <p>Title: NumberBinder</p>
 * <p>Description: Binds values as numbers and replaces as non-quoted numbers </p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
@Binder(name="NumberBinder")
public class NumberBinder implements IBinder {
	
	/**
	 * No Op
	 * @param config
	 * @throws BindProviderConfigurationException
	 * @see org.helios.collectors.jdbc.binding.binder.IBinder#configure(java.lang.String)
	 */
	public void configureBinder(String config) throws BindProviderConfigurationException {
		
	}

	/**
	 * Binds the passed object to the statement as an object.
	 * @param ps
	 * @param bindSequence
	 * @param bindValue
	 * @return the incremented sequence or the unchanged value.
	 * @throws SQLException
	 * @see org.helios.collectors.jdbc.binding.binder.IBinder#bind(java.sql.PreparedStatement, int, java.lang.Object)
	 */
	public int bind(PreparedStatement ps, int bindSequence, Object bindValue) 	throws SQLException {
		Number number = null;
		if(bindValue!=null && Number.class.isAssignableFrom(bindValue.getClass())) {
			number = (Number)bindValue;
			Class<Number> nClass = (Class<Number>) bindValue.getClass();
			if(AtomicInteger.class.equals(nClass) || Integer.class.equals(nClass)) {
				ps.setInt(bindSequence, number.intValue());
			} else if(AtomicLong.class.equals(number) || Long.class.equals(nClass)) {
				ps.setLong(bindSequence, number.longValue());
			} else if(BigDecimal.class.equals(number) ) {
				ps.setBigDecimal(bindSequence, (BigDecimal)number);
			} else if(BigInteger.class.equals(number) ) {
				ps.setObject(bindSequence, number, Types.BIGINT);
			} else if(Byte.class.equals(number) ) {
				ps.setByte(bindSequence, number.byteValue());
			} else if(Double.class.equals(number) ) {
				ps.setDouble(bindSequence, number.doubleValue());
			} else if(Float.class.equals(number) ) {
				ps.setFloat(bindSequence, number.floatValue());
			} else if(Short.class.equals(number) ) {
				ps.setShort(bindSequence, number.shortValue());
			} else {
				throw new SQLException("Unable to determine numeric type mapping to bind type of [" + bindValue.getClass().getName() + "]");
			}
		} else {
			if(bindValue!=null) {
				try {
					ps.setObject(bindSequence, bindValue.toString(), Types.NUMERIC);
				} catch (Exception e) {
					ps.setNull(bindSequence,Types.NUMERIC);
				}				
			}
			ps.setNull(bindSequence,Types.NUMERIC);
		}		
		return bindSequence;
	}

	/**
	 * Replaces the passed token with a toString of the object without quotes.
	 * @param sqlStatement
	 * @param bindToken
	 * @param bindValue
	 * @return the modified sql string
	 * @throws SQLException
	 * @see org.helios.collectors.jdbc.binding.binder.IBinder#bind(java.lang.CharSequence, java.lang.String, java.lang.Object)
	 */
	public CharSequence bind(CharSequence sqlStatement, String bindToken, Object bindValue) throws SQLException {
		Number number = null;
		if(bindValue!=null && Number.class.isAssignableFrom(bindValue.getClass())) {
			number = (Number)bindValue;
		}
		return sqlStatement.toString().replace(bindToken, number==null ? bindValue==null ? " null " : bindValue.toString() : number.toString());
	}

}
