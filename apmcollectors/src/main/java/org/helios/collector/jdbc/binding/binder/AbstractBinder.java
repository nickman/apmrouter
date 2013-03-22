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
 * <p>Title: AbstractBinder</p>
 * <p>Description: Base class for implementing concrete binders. </p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public abstract class AbstractBinder implements IBinder {

	/**
	 * Optional configuration entry point for a binder.
	 * @param config the confffiguration string.
	 * @throws BinderConfgurationException
	 */
	public void configureBinder(String config) throws BindProviderConfigurationException {
		
	}
	
	/**
	 * Executes a PreparedStatement bind.
	 * @param ps The prepared statement to bind against.
	 * @param bindSequence The sequence of the bind variable.
	 * @param bindValue The value to bind.
	 * @return The final bind sequence executed by this bind call. Normally, this would be the same as <code>bindSequence</code>.
	 * @throws SQLException
	 */
	public abstract int bind(PreparedStatement ps, int bindSequence, Object bindValue) throws SQLException;
	
	/**
	 * Replaces the bindToken embedded in the passed sqlStatement with the correctly formatted literal value.
	 * @param sqlStatement The SQL string containing the bind token to be replaced.
	 * @param bindToken The bind token to be replaced in the SQL string.
	 * @param bindValue The value to replace the bind token as a literal. 
	 * @return The modified SQL string.
	 * @throws SQLException
	 */
	public CharSequence bind(CharSequence sqlStatement, String bindToken, Object bindValue) throws SQLException {
		return sqlStatement.toString().replace(bindToken, decorateBindValue(bindValue));
	}
	
	/**
	 * @param bindValue
	 * @return
	 */
	protected abstract CharSequence decorateBindValue(Object bindValue);

}
