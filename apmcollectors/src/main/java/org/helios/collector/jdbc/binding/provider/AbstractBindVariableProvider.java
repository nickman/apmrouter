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

import org.apache.log4j.Logger;
import org.helios.collector.jdbc.binding.binder.Binder;
import org.helios.collector.jdbc.binding.binder.IBinder;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * <p>Title: AbstractBindVariableProvider</p>
 * <p>Description: Base abstract class for bind variable provider implementations.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public abstract class AbstractBindVariableProvider implements IBindVariableProvider, ApplicationContextAware {
	/** The value to be bound to the statement */
	protected AtomicReference<Object> bindValue = new AtomicReference<Object>();
	/** the unique bind token key for this provider class */
	protected final String tokenKey = getMyTokenKey();
	/** The instance bind token */
	protected String bindToken = null;
	/** The custom configuration string */
	protected String config = null;
	/** The provider's binder */
	protected IBinder binder = null;
	/** The spring application context */
	protected ApplicationContext appContext = null;	
	/** A set of provider listeners */
	protected Set<IBindVariableProviderListener> listeners = new CopyOnWriteArraySet<IBindVariableProviderListener>();
	/** The forceNoBind flag */
	protected AtomicBoolean forceNoBind = new AtomicBoolean(false);
	/** Instance logger */
	protected Logger log = Logger.getLogger(getClass());
	
	
	
	/**
	 * Package protected constructor to avoid direct construction of providers.
	 */
	AbstractBindVariableProvider() {
		if(log.isDebugEnabled()) log.debug("Constructing Provider [" + getClass().getName() + "]" );
		try {
			Binder binderAnn = this.getClass().getAnnotation(Binder.class);		
			if(binderAnn!=null) {
				
				String name = binderAnn.name();
				if(this.getClass().equals(BindVariableProviderFactory.getInstance().getBinderClass(name))) {
					this.binder = (IBinder)this;
				} else {
					String config =  "".equals(binderAnn.config()) ? null : binderAnn.config();
					this.binder = BindVariableProviderFactory.getInstance().getBinder(name + config);
				}
			}
 
		} catch (Exception e) {
			log.warn("Failed to initialize binder from annotation", e);
		}
		if(log.isDebugEnabled()) log.debug("Constructed Provider [" + getClass().getName() + "]" );
		
	}
	
	/**
	 * Retrieves the forceNoBind state. If true, the binder will issue a replace instead of a bind.
	 * @return the forceNoBind state.
	 */
	public boolean isForceNoBind() {
		return forceNoBind.get();
	}
	
	/**
	 * Sets the forceNoBind boolean. If true, the binder will issue a replace instead of a bind.
	 * @param forceNoBind If true, the binder will issue a replace instead of a bind.
	 */
	public void setForceNoBind(boolean forceNoBind) {
		this.forceNoBind.set(forceNoBind);
	}

	
	
	/**
	 * Introspects the token key from the annotation.
	 * @return the token key.
	 */
	protected String getMyTokenKey() {
		try {
			BindVariableProvider bvp = this.getClass().getAnnotation(BindVariableProvider.class);
			return bvp==null ? null : bvp.tokenKey();
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Executes a bind against a prepared statement.
	 * @param ps The prepared statement to invoke the bind against.
	 * @param bindSequence The bind sequence.
	 * @return The final bind sequence executed by this bind call. Normally, this would be the same as <code>bindSequence</code>.
	 * @throws SQLException
	 */
	public int bind(PreparedStatement ps, int bindSequence) 	throws SQLException {
		return this.getIBinder().bind(ps, bindSequence, getValue());
	}

	/**
	 * Executes a token substitution against a sql statement for the passed  bind token, replacing the token with the provider supplied literal.
	 * Intended to support binding-like operations when using drivers that do not support <code>PreparedStatements</code> and/or bind variables.
	 * @param sql The tokenized sql statement
	 * @param bindToken The bind token to replace with a literal binding.
	 * @return The substituted sql statement.
	 * @throws SQLException
	 */
	public CharSequence bind(CharSequence sql, String bindToken) throws SQLException {
		return this.getIBinder().bind(sql, bindToken, getValue());
	}

	/**
	 * Returns the instance specific bind token.
	 * @return the bind token.
	 * @see org.helios.collectors.jdbc.binding.provider.IBindVariableProvider#getBindToken()
	 */
	public String getBindToken() {
		return bindToken;
	}
	
	/**
	 * Sets the bind token that uniquely identifies an instance of this provider.
	 * @param the bind token for this provider.
	 */
	public void setBindToken(String bindToken) { 
		this.bindToken = bindToken;
	}
	
	
	/**
	 * The bind token key that uniquely the key of this provider's class.
	 * @return the bind token key for this provider.
	 */
	public String getBindTokenKey() {
		return tokenKey;
	}

	/**
	 * Returns the instance's binder.
	 * @return the binder.
	 * @see org.helios.collectors.jdbc.binding.provider.IBindVariableProvider#getIBinder()
	 */
	public IBinder getIBinder() {
		return binder;
	}
	
	/**
	 * Overrides a provider's binder 
	 * @param binder the overriding binder instance.
	 */
	public void setBinder(IBinder binder) {
		this.binder = binder;
	}
	

	/**
	 * Returns the provider's current value.
	 * @return the provider's value.
	 * @see org.helios.collectors.jdbc.binding.provider.IBindVariableProvider#getValue()
	 */
	public Object getValue() {
		return bindValue.get();
	}

	
	/**
	 * Concrete implementations should implement this to configure themselves.
	 * @param config The configuration string extracted from the bind token
	 * @throws BindProviderConfigurationException
	 */
	public void configureProvider(String config) throws BindProviderConfigurationException{};

	/**
	 * Set's the provider's bind value.
	 * @param value the provider's new bind value.
	 * @see org.helios.collectors.jdbc.binding.provider.IBindVariableProvider#setValue(java.lang.Object)
	 */
	public void setValue(Object value) {
		Object oldValue = bindValue.get();
		bindValue.set(value);
		if(oldValue==null && value==null) return;
		if((oldValue==null && value!=null) || (value==null && oldValue!=null) ) {
			BindVariableProviderFactory.getInstance().fireProviderValueChanged(Collections.unmodifiableSet(listeners), this, oldValue, value); 
		} else if(value!=null && !value.equals(oldValue)) {
			BindVariableProviderFactory.getInstance().fireProviderValueChanged(Collections.unmodifiableSet(listeners), this, oldValue, value);
		} else if(oldValue!=null && !oldValue.equals(value)) {
			BindVariableProviderFactory.getInstance().fireProviderValueChanged(Collections.unmodifiableSet(listeners), this, oldValue, value);
		}
	}
	
	
	
	/**
	 * Registers a new IBindVariableProviderListener
	 * @param listener the listener to register.
	 */
	public void registerListener(IBindVariableProviderListener listener) {
		if(listener!=null) {
			listeners.add(listener);
		}
	}
	
	/**
	 * Unregisters a IBindVariableProviderListener
	 * @param listener the listener to unregister.
	 */
	public void removeListener(IBindVariableProviderListener listener) {
		if(listener!=null) {
			listeners.remove(listener);
		}		
	}
	
	/**
	 * Sets the spring application context
	 * @param appContext the spring application context
	 * @throws BeansException
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
	 */
	public void setApplicationContext(ApplicationContext appContext) 	throws BeansException {
		this.appContext = appContext;		
	}
	
		

}
