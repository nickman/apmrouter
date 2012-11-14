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
package org.helios.apmrouter.subscription.impls.jmx;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerDelegate;
import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.log4j.Logger;
import org.helios.apmrouter.dataservice.json.JsonRequest;
import org.helios.apmrouter.subscription.criteria.FailedCriteriaResolutionException;
import org.helios.apmrouter.subscription.criteria.SubscriptionCriteria;
import org.helios.apmrouter.subscription.criteria.SubscriptionCriteriaInstance;
import org.helios.apmrouter.subscription.criteria.builder.SubscriptionCriteriaBuilder;
import org.helios.apmrouter.subscription.session.SubscriptionSession;
import org.helios.apmrouter.util.SystemClock;

/**
 * <p>Title: JMXSubscriptionCriteriaInstance</p>
 * <p>Description: A resolved and active instance of a {@link JMXSubscriptionCriteria}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.subscription.jmx.JMXSubscriptionCriteriaInstance</code></p>
 */

public class JMXSubscriptionCriteriaInstance implements SubscriptionCriteria<String, ObjectName, NotificationFilter>, SubscriptionCriteriaInstance<Notification>, NotificationListener, NotificationFilter {
	/**  */
	private static final long serialVersionUID = -8406458051126347970L;
	/** The criteria this instance was resolved from */
	protected final JMXSubscriptionCriteria criteria;
	/** The original json request issued for this subscription */
	protected final JsonRequest request;
	/** The JMXConnector used to connect to the MBeanServer */
	protected JMXConnector jmxConnector = null;
	/** The managing subscription session */
	protected SubscriptionSession session = null;
	/** The target MBeanServer for this subscription */
	protected MBeanServerConnection mbeanServerConnection = null;
	/** A set of ObjectNames this criteria instance is activated for */
	protected final Set<ObjectName> objectNames = new CopyOnWriteArraySet<ObjectName>();
	/** A set of ObjectNames this criteria failed to activated for */
	protected final Set<ObjectName> failedObjectNames = new CopyOnWriteArraySet<ObjectName>();
	/** The arbitrary criteria key */
	protected Object subscriptionKey = null;
	
	/** The assigned internal serial number for this subscription */
	protected final Long jmxSubId;
	/** A serial number generator used to register callbacks so notifications can quickly be identified as being for this subscription */
	protected static final AtomicLong serial = new AtomicLong(0);
	/** Static class logger */
	protected static final Logger LOG = Logger.getLogger(JMXSubscriptionCriteriaInstance.class);
	
	
	/**
	 * Creates a new JMXSubscriptionCriteriaInstance
	 * @param criteria The criteria this instance will be resolved from
	 * @param request The original json request issued for this subscription
	 * @param subscriptionKey The arbitrary subscription key
	 */
	JMXSubscriptionCriteriaInstance(JMXSubscriptionCriteria criteria, JsonRequest request, Object subscriptionKey) {
		super();
		this.criteria = criteria;
		this.request = request;
		jmxSubId = serial.incrementAndGet();
		this.subscriptionKey = subscriptionKey;
	}
	
	/**
	 * Resolves the criteria.
	 * @param session The managing subscription session.
	 * @throws FailedCriteriaResolutionException
	 */
	public void resolve(SubscriptionSession session) throws FailedCriteriaResolutionException {
		try {
			JMXServiceURL serviceURL = new JMXServiceURL(criteria.getEventSource());
			jmxConnector = JMXConnectorFactory.connect(serviceURL);
			this.session = session;
			mbeanServerConnection = jmxConnector.getMBeanServerConnection();
			LOG.info("Resolved JMX Criteria MBeanServer [" + serviceURL + "]");
		} catch (Exception ex) {
			throw new FailedCriteriaResolutionException(criteria, "Failed to locate MBeanServerConnection for [" + criteria.getEventSource() + "]", ex);
		}
		if(criteria.isPattern()) {
			try {
				mbeanServerConnection.addNotificationListener(MBeanServerDelegate.DELEGATE_NAME , this, null, jmxSubId);
				LOG.info("Added MBean Registration Listener on JMX Criteria MBeanServer [" + criteria.getEventSource() + "]");
			} catch (Exception ex) {
				LOG.error("Failed to add MBean registration listener for JMX criteria [" + criteria.getEventSource() + "]", ex);
				throw new FailedCriteriaResolutionException(criteria, "Failed to add MBean registration listener for pattern [" + criteria.getEventSource() + "]", ex);
			}			
			try {
				for(ObjectName on: mbeanServerConnection.queryNames(criteria.getEventFilter(), null)) {
					addMBeanSource(on);
				}				
			} catch (Exception ex) {
				LOG.error("Failed to process existing matching MBeans for [" + criteria.getEventFilter() + "] on MBeanServer [" + criteria.getEventSource() + "]", ex);
				throw new FailedCriteriaResolutionException(criteria, "Failed to process existing matching MBeans for [" + criteria.getEventFilter() + "] on MBeanServer [" + criteria.getEventSource() + "]", ex);
			}			
		} else {
			try {
				addMBeanSource(criteria.getEventFilter());
			} catch (Exception ex) {
				LOG.error("Failed to add notification listener for non-pattern MBean [" + criteria.getEventFilter() + "]", ex);
				throw new FailedCriteriaResolutionException(criteria, "Failed to add notification listener for non-pattern MBean [" + criteria.getEventFilter() + "]", ex);
			}
		}
		Notification notif = new Notification("subscriber.registration", this, serial.incrementAndGet(), SystemClock.time());
		//JMXHelper.getHeliosMBeanServer().
	}
	
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.subscription.criteria.SubscriptionCriteriaInstance#getSubscriptionCriteria()
	 */
	@Override
	public SubscriptionCriteria getSubscriptionCriteria() {
		return criteria;
	}
	
	/**
	 * Terminates this criteria instance
	 */
	@Override
	public void terminate() {
		objectNames.addAll(failedObjectNames);
		failedObjectNames.clear();
		for(ObjectName on: objectNames) {
			try { 
				mbeanServerConnection.removeNotificationListener(on, this, criteria.getEventExtendedFilter(), jmxSubId);
			} catch (Exception ex) { /* NoOp */ }
		}
		objectNames.clear();
		try { jmxConnector.close(); } catch (Exception e) { /* NoOp */ }
	}
	

	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationFilter#isNotificationEnabled(javax.management.Notification)
	 */
	@Override
	public boolean isNotificationEnabled(Notification notification) {
		// Test for newly registered or unregistered target MBeans
		// If so, add or remove, then return false.
		if(notification instanceof MBeanServerNotification) {
			if(!criteria.isPattern()) return false;
			MBeanServerNotification msn = (MBeanServerNotification)notification;
			if(criteria.getEventFilter().apply(msn.getMBeanName())) {
				if(MBeanServerNotification.REGISTRATION_NOTIFICATION.equals(msn.getType())) {
					addMBeanSource(msn.getMBeanName());
				} else {
					objectNames.remove(msn.getMBeanName());
					failedObjectNames.remove(msn.getMBeanName());
				}
			}
			return false;
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationListener#handleNotification(javax.management.Notification, java.lang.Object)
	 */
	@Override
	public void handleNotification(Notification notification, Object handback) {
		if(jmxSubId.equals(handback)) {
			session.send(request.subResponse().setContent(notification));			
		}		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.subscription.criteria.SubscriptionCriteriaInstance#getCriteriaId()
	 */
	@Override
	public long getCriteriaId() {
		return jmxSubId;
	}
	
	/**
	 * Adds an MBean's ObjectName to the subscribed notification set.
	 * If the addition of the notification listener fails, the object name
	 * is removed from the subscribed set and added to the failed set.
	 * @param mbean
	 */
	protected void addMBeanSource(ObjectName mbean) {
		if(objectNames.add(mbean)) {
			try {
				mbeanServerConnection.addNotificationListener(mbean, this, criteria.getEventExtendedFilter(), jmxSubId);
			} catch (Exception ex) {
				LOG.warn("Failed to add notification listener for new MBean [" + mbean + "]", ex);
				objectNames.remove(mbean);
				failedObjectNames.add(mbean);
			}
		}		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.subscription.criteria.SubscriptionCriteria#getEventSource()
	 */
	public String getEventSource() {
		return criteria.getEventSource();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.subscription.criteria.SubscriptionCriteria#getEventFilter()
	 */
	public ObjectName getEventFilter() {
		return criteria.getEventFilter();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.subscription.criteria.SubscriptionCriteria#getEventExtendedFilter()
	 */
	public NotificationFilter getEventExtendedFilter() {
		return criteria.getEventExtendedFilter();
	}

	/**
	 * Indicates if the event filter of the criteria is a pattern or absolute
	 * @return true if the event filter of the criteria is a pattern, false otherwise
	 */
	public boolean isPattern() {
		return criteria.isPattern();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.subscription.criteria.SubscriptionCriteria#instantiate(org.helios.apmrouter.dataservice.json.JsonRequest)
	 */
	@Override
	public SubscriptionCriteriaInstance<Notification> instantiate(JsonRequest request) {
		return this;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.subscription.criteria.SubscriptionCriteria#getBuilder()
	 */
	@Override
	public SubscriptionCriteriaBuilder getBuilder() {
		return criteria.getBuilder();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.subscription.criteria.SubscriptionCriteria#getSubcriptionKey()
	 */
	@Override
	public Object getSubcriptionKey() {
		return subscriptionKey;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.subscription.criteria.SubscriptionCriteria#setSubcriptionKey(java.lang.Object)
	 */
	public void setSubcriptionKey(Object subscriptionKey) {
		this.subscriptionKey = subscriptionKey;
	}

}
