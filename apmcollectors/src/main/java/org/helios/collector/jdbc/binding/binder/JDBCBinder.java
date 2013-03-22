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
package org.helios.collector.jdbc.binding.binder;

import org.helios.collector.jdbc.binding.provider.BindProviderConfigurationException;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * <p>Title: JDBCBinder</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class JDBCBinder implements IBinder {

	/**
	 * @param ps
	 * @param bindSequence
	 * @param bindValue
	 * @return
	 * @throws SQLException
	 * @see org.helios.collectors.jdbc.binding.binder.IBinder#bind(java.sql.PreparedStatement, int, java.lang.Object)
	 */
	public int bind(PreparedStatement ps, int bindSequence, Object bindValue)
			throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * @param sqlStatement
	 * @param bindToken
	 * @param bindValue
	 * @return
	 * @throws SQLException
	 * @see org.helios.collectors.jdbc.binding.binder.IBinder#bind(java.lang.CharSequence, java.lang.String, java.lang.Object)
	 */
	public CharSequence bind(CharSequence sqlStatement, String bindToken,
			Object bindValue) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @param config
	 * @throws BindProviderConfigurationException
	 * @see org.helios.collectors.jdbc.binding.binder.IBinder#configureBinder(java.lang.String)
	 */
	public void configureBinder(String config)
			throws BindProviderConfigurationException {
		// TODO Auto-generated method stub

	}

}
