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

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.helios.collector.jdbc.binding.binder.Binder;
import org.helios.collector.jdbc.binding.binder.IBinder;

/**
 * <p>Title: SimpleObjectBinder</p>
 * <p>Description: Binds values as objects and replaces as strings</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
@Binder(name="ObjectBinder")
public class SimpleObjectBinder implements IBinder {
	
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
	 * @return
	 * @throws SQLException
	 * @see org.helios.collectors.jdbc.binding.binder.IBinder#bind(java.sql.PreparedStatement, int, java.lang.Object)
	 */
	public int bind(PreparedStatement ps, int bindSequence, Object bindValue) 	throws SQLException {
		ps.setObject(bindSequence, bindValue);
		return bindSequence;
	}

	/**
	 * Replaces the passed token with a toString of the object.
	 * @param sqlStatement
	 * @param bindToken
	 * @param bindValue
	 * @return
	 * @throws SQLException
	 * @see org.helios.collectors.jdbc.binding.binder.IBinder#bind(java.lang.CharSequence, java.lang.String, java.lang.Object)
	 */
	public CharSequence bind(CharSequence sqlStatement, String bindToken, Object bindValue) throws SQLException {		
		return sqlStatement.toString().replace(bindToken, bindValue==null ? "''" : "'" + bindValue.toString() + "'");
	}

}
