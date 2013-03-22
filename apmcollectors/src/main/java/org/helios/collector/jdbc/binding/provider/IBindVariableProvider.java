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

import org.helios.collector.jdbc.binding.binder.IBinder;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * <p>Title: IBindVariableProvider</p>
 * <p>Description: Instances of this class provide bind variables and binding for bind tokens in SQL statements in SQLMapping queries.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public interface IBindVariableProvider {
	
	/**
	 * Retrieves the forceNoBind state. If true, the binder will issue a replace instead of a bind.
	 * @return the forceNoBind state.
	 */
	public boolean isForceNoBind();
	
	/**
	 * Sets the forceNoBind boolean. If true, the binder will issue a replace instead of a bind.
	 * @param forceNoBind If true, the binder will issue a replace instead of a bind.
	 */
	public void setForceNoBind(boolean forceNoBind);

	
	
	/**
	 * Concrete implementations should implement this to configure themselves.
	 * @param config The configuration string extracted from the bind token
	 * @throws BindProviderConfigurationException
	 */
	public void configureProvider(String config) throws BindProviderConfigurationException;	
	
	/**
	 * The bind token that uniquely identifies an instance of this provider.
	 * @return the bind token for this provider.
	 */
	public String getBindToken();
	
	/**
	 * Sets the bind token that uniquely identifies an instance of this provider.
	 * @param the bind token for this provider.
	 */
	public void  setBindToken(String bindToken);
	
	
	/**
	 * The bind token key that uniquely the key of this provider's class.
	 * @return the bind token key for this provider.
	 */
	public String getBindTokenKey();
	
	
	/**
	 * Returns the provider's bind value.
	 * @return the bind value.
	 */
	public Object getValue();
	
	/**
	 * The IBinder used to perform binds and replacements for this provider.
	 * @return an IBinder.
	 */
	public IBinder getIBinder();
	
	/**
	 * Overrides a provider's binder 
	 * @param binder the overriding binder instance.
	 */
	public void setBinder(IBinder binder);
	
	/**
	 * Sets the provider's bind value.
	 * @param value the bind value to set.
	 */
	public void setValue(Object value);
	
	 
	
	/**
	 * Executes a bind against a prepared statement.
	 * @param ps The prepared statement to invoke the bind against.
	 * @param bindSequence The bind sequence.
	 * @return The final bind sequence executed by this bind call. Normally, this would be the same as <code>bindSequence</code>.
	 * @throws SQLException
	 */
	public int bind(PreparedStatement ps, int bindSequence) throws SQLException;
	
	
	/**
	 * Executes a token substitution against a sql statement for the passed  bind token, replacing the token with the provider supplied literal.
	 * Intended to support binding-like operations when using drivers that do not support <code>PreparedStatements</code> and/or bind variables.
	 * @param sql The tokenized sql statement
	 * @param bindToken The bind token to replace with a literal binding.
	 * @return The substituted sql statement.
	 * @throws SQLException
	 */
	public CharSequence bind(CharSequence sql, String bindToken) throws SQLException;
	
	/**
	 * Registers a new IBindVariableProviderListener
	 * @param listener the listener to register.
	 */
	public void registerListener(IBindVariableProviderListener listener);
	
	/**
	 * Unregisters a IBindVariableProviderListener
	 * @param listener the listener to unregister.
	 */
	public void removeListener(IBindVariableProviderListener listener);
	
	
	
	
	
	
	
	
	
	
	
}
