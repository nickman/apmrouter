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
 * <p>Title: JDBCTypeBindProvider</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
@Binder(name="JDBC")
public class JDBCTypeBindProvider extends AbstractBindVariableProvider implements IBinder {
	protected JDBCType type = null;
	
	/*
	 *       {JDBC:<type code>:<name>}
	 */

	/**
	 * @param config
	 * @throws BindProviderConfigurationException
	 * @see org.helios.collectors.jdbc.binding.provider.AbstractBindVariableProvider#configure(java.lang.String)
	 */
	public void configureProvider(String config) throws BindProviderConfigurationException {
		String[] frags = config.split(":");
		if(frags==null || frags.length < 2) throw new BindProviderConfigurationException("Config fragments were null or < 2");
		try {
			int typeCode = Integer.parseInt(frags[0].trim());
			type = JDBCType.fromCode(typeCode);
			binder = this;
		} catch (Exception e) {
			throw new BindProviderConfigurationException("Unable to derive JDBCType from code [" + frags[0] + "]", e);
		}
	}
	
	/**
	 * Optional configuration entry point for a binder.
	 * @param config the confffiguration string.
	 * @throws BinderConfgurationException
	 */
	public void configureBinder(String config) throws BindProviderConfigurationException {
		// determine if config is an int or string and set the type accordingly
	}
	
	
	/**
	 * @return
	 * @see org.helios.collectors.jdbc.binding.provider.AbstractBindVariableProvider#getIBinder()
	 */
	public IBinder getIBinder() {
		return this.binder;
	}
	


	/**
	 * @param ps
	 * @param bindSequence
	 * @param bindValue
	 * @return
	 * @throws SQLException
	 * @see org.helios.collectors.jdbc.binding.binder.IBinder#bind(java.sql.PreparedStatement, int, java.lang.Object)
	 */
	public int bind(PreparedStatement ps, int bindSequence, Object bindValue) 	throws SQLException {
		if(bindValue==null) {
			ps.setNull(bindSequence, type.code());
		} else {
			ps.setObject(bindSequence, bindValue, type.code());
		}
		return bindSequence;
	}


	/**
	 * @param sqlStatement
	 * @param bindToken
	 * @param bindValue
	 * @return
	 * @throws SQLException
	 * @see org.helios.collectors.jdbc.binding.binder.IBinder#bind(java.lang.CharSequence, java.lang.String, java.lang.Object)
	 */
	public CharSequence bind(CharSequence sqlStatement, String bindToken, Object bindValue) throws SQLException {
		if(!type.literal()) throw new SQLException("The binder [" + binder.getClass().getName()  + "] for provider [" + this.bindToken + "] does not support literal binding of type [" + type.name() + "]");
		String repl = null;
		if(type.quoted()) {
			repl = "'" + bindValue + "'";
		} else {
			repl = "" + bindValue + "";
		}
		return sqlStatement.toString().replace(bindToken,repl);		
	}

}
