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
import org.helios.apmrouter.jmx.JMXHelper;
import org.helios.collector.jdbc.binding.binder.Binder;
import org.helios.collector.jdbc.binding.binder.BinderNotFoundException;
import org.helios.collector.jdbc.binding.binder.IBinder;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.jmx.export.annotation.ManagedAttribute;

import javax.management.openmbean.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>Title: BindVariableProviderFactory</p>
 * <p>Description: Singleton factory for creating and caching instances of providers and binders.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 * org.helios.collectors.jdbc.binding.BindVariableProviderFactory
 */
//@JMXManagedObject(annotated=true, declared=true)
//@JMXNotifications(notifications={
//		@JMXNotification(description="BindVariableProviderFactory JMX Notifications", types={
//				@JMXNotificationType(type=BindVariableProviderFactory.VALUE_CHANGED_NOTIFICATION)
//		})		
//})
// TODO - NEED TO IMPLEMENT NOTIFICATION for OnValueChanged method
public class BindVariableProviderFactory implements ThreadFactory, ApplicationContextAware, IBindVariableProviderListener {
	/**  */
	private static final long serialVersionUID = -4522349786016612658L;
	/** Notification type for value changed */
	public static final String VALUE_CHANGED_NOTIFICATION = "org.helios.collectors.jdbc.provider.valuechange";
	/** The factory singleton instance  */
	protected static volatile BindVariableProviderFactory factory = null;
	/** The singleton lock */
	protected static volatile Object lock = new Object();
	/** The provider listener execution thread pool */
	protected ExecutorService notificationThreadPool = null;
	/** Additional classpaths to scannotate for provider and binder classes  */
	protected Set<URL> classPaths = new HashSet<URL>();
	/** A map of provider instances keyed by bind token */
	protected Map<String, IBindVariableProvider> providers = new ConcurrentHashMap<String, IBindVariableProvider>();
	/** A map of provider classes keyed by bind token key */
	protected Map<String, Class<IBindVariableProvider>> providerClasses = new ConcurrentHashMap<String, Class<IBindVariableProvider>>();
	/** A map of binder classes keyed by binder shorthand name */
	protected Map<String, Class<IBinder>> binderClasses = new ConcurrentHashMap<String, Class<IBinder>>();
	/** A map of binder instances keyed by the full binder token */
	protected Map<String, IBinder> binders = new ConcurrentHashMap<String, IBinder>();	
	/** Thread name suffix factory */
	protected AtomicInteger serial = new AtomicInteger(0);
	/** The spring application context */
	protected ApplicationContext appContext = null;
	/** The class loader used to load the provider and binder classes */
	protected URLClassLoader classLoader = null;
	/** The composite type container for bind providers */
	protected CompositeType bindProviderType = null;
	/** The composite type container for the bind providers */
	protected TabularType bindProviderTableType = null;
	/** The tabular data instance */
	protected TabularDataSupport bindProviderTable = null;
	/** The JMX ObjectName for this class */
	public static final String FACTORY_OBJECT_NAME = "org.helios.collectors.jdbc:service=BindVariableProviderFactory";
	/** The value names in the bindProviderType OpenType */
	public static final String[] OpenTypeNames = new String[]{"BindToken", "BindKey", "ProviderValue", "ProviderClass", "BinderClass", "ForceNoBind"};
	/** The value descriptions in the bindProviderType OpenType */
	public static final String[] OpenTypeDescriptions = new String[]{"The Provider Bind Token", "The Provider Bind Key", "The Provider Current Value", "The Provider Class Name", "The Binder Class Name", "Forces Literal Binding"};
	/** The value types in the bindProviderType OpenType */
	public static final OpenType[] OpenTypeTypes = new OpenType[]{SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, SimpleType.BOOLEAN};
	/** Sequence factory for notifications */
	protected AtomicLong notificationSequence = new AtomicLong(0);
	/** Static class logger */
	protected static final Logger LOG = Logger.getLogger(BindVariableProviderFactory.class);
	
	/**
	 * Private constructor.
	 */
	private BindVariableProviderFactory() {
		LOG.info("Instantiated BindVariableProviderFactory" );
		try {
			initComposite();
			//this.reflectObject(this);
			JMXHelper.getHeliosMBeanServer().registerMBean(this, JMXHelper.objectName(FACTORY_OBJECT_NAME));
		} catch (Exception e) {
			LOG.warn("Failed to register management interface", e);
		}
	}
	
	/**
	 * Singleton accessor
	 * @return the instance.
	 */
	public static BindVariableProviderFactory getInstance() {
		if(factory==null) {
			synchronized(lock) {
				if(factory==null) {
					factory = new BindVariableProviderFactory();					
				}
			}
		}
		return factory;
	}
	
	/**
	 * Starts the factory.
	 * @throws Exception
	 */
	public void start() throws Exception {
		if(notificationThreadPool==null) {
			notificationThreadPool = Executors.newCachedThreadPool(this);
		}		
	}
	
	
	
	/**
	 * Initializes the composite type to expose instances of providers. Structure:<ul>
	 * <li>BindToken Name</li>
	 * <li>BindToken Key</li>
	 * <li>Provider Class Name</li>
	 * <li>Binder Class Name</li>
	 * <li>Force No Bind</li>
	 * <li>Value</li>
	 * </ul>
	 */
	protected void initComposite() {
		try {
			bindProviderType = new CompositeType("BindVariableProvider", "Defines an exposed BindVariableProvider", OpenTypeNames, OpenTypeDescriptions, OpenTypeTypes );
			bindProviderTableType = new TabularType("BindVariableProviderTable", "Defines a table of exposed BindVariableProviders", bindProviderType, new String[]{"BindToken"});
			bindProviderTable = new TabularDataSupport(bindProviderTableType) ;
		} catch (Exception e) {
			LOG.error("Failed to instantiate composite type", e);
			throw new RuntimeException("Failed to instantiate composite type", e);
		}		
	}	
	
	/**
	 * Adds a provider to the bindProvider table.
	 * @param provider The provider to add.
	 */
	protected void insert(IBindVariableProvider provider) {
		if(provider==null) return;
		Object value = provider.getValue();
		if(value==null) value = "[null]";
		else value = value.toString();		
		try {
			Object[] items = new Object[]{provider.getBindToken(), provider.getBindTokenKey(), value, provider.getClass().getName(), provider.getIBinder().getClass().getName(), provider.isForceNoBind() };
			CompositeDataSupport cds = new CompositeDataSupport(bindProviderType, OpenTypeNames, items);
			cds.get("BindToken");
			bindProviderTable.put(cds);
		} catch (OpenDataException e) {
			LOG.warn("Failed to create CompositeData item for provider [" + provider.getBindToken() + "]." );
		}
	}
	
	
	
//	/**
//	 * Scans for all applicable classpaths and then creates an annotation database. 
//	 * @return A Map of all annotations and matching classes.
//	 * @throws IOException
//	 */
//	protected Map<String,Set<String>> prepareScans() throws IOException {
//		Collections.addAll(classPaths, ClasspathUrlFinder.findClassPaths());
//		URL[] urlArray = classPaths.toArray(new URL[classPaths.size()]);
//		classLoader = new URLClassLoader(urlArray);
//		LOG.info("Prepared [" + classPaths.size() + "] URLs to scan for providers and binders");
//		LOG.info("Scanning Classpath for Provider Classes");
//		annotationDb.setScanClassAnnotations(true);
//		annotationDb.setScanFieldAnnotations(false);
//		annotationDb.setScanMethodAnnotations(false);
//		annotationDb.setScanParameterAnnotations(false);
//		annotationDb.scanArchives(urlArray);
//		LOG.info("Scan Complete");		
//		return annotationDb.getAnnotationIndex();
//	}
	
	/**
	 * Locates and registers all bind variable provider classes.
	 * @param index the annotation map
	 */
	@SuppressWarnings("unchecked")
	protected void scanForProviders(Map<String,Set<String>> index)  {
		Set<String> classSet = index.get(BindVariableProvider.class.getName());
		if(classSet==null) return;
		for(String className: classSet) {
			if(LOG.isDebugEnabled()) LOG.info("Loading Provider [" + className + "]");
			try {
				Class<IBindVariableProvider> clazz = (Class<IBindVariableProvider>) Class.forName(className, true, classLoader);
				BindVariableProvider bvp = clazz.getAnnotation(BindVariableProvider.class);
				String tokenKey = bvp.tokenKey();
				Class<?> existingClass = providerClasses.get(tokenKey);
				if(existingClass!=null) {
					if(!clazz.equals(existingClass)) {
						LOG.warn("The provider class [" + clazz.getName() + "] has the same token key as [" + existingClass.getName() + "] but was scanned after it. It will not be added");
					}
				} else {
					providerClasses.put(tokenKey, clazz);
					if(LOG.isDebugEnabled()) LOG.info("Loaded Provider [" + className + "] with key [" + tokenKey + "]");
				}
			} catch (Exception e) {
				LOG.error("Failed to load provider class [" + className + "]");
			}			
		}
	}
	
	/**
	 * Locates and registers all binder classes.
	 * @param index the annotation map
	 */	
	@SuppressWarnings("unchecked")
	protected void scanForBinders(Map<String,Set<String>> index)  {
		Set<String> classSet = index.get(Binder.class.getName());
		if(classSet==null) return;
		for(String className: classSet) {
			if(LOG.isDebugEnabled()) LOG.info("Loading Binder [" + className + "]");
			// binderClasses
			try {
				Class<IBinder> clazz = (Class<IBinder>) Class.forName(className, true, classLoader);
				Binder bvp = clazz.getAnnotation(Binder.class);
				String name = bvp.name();
				Class<?> existingClass = binderClasses.get(name);
				if(existingClass!=null) {
					if(!clazz.equals(existingClass)) {
						LOG.warn("The binder class [" + clazz.getName() + "] has the same shorthand name as [" + existingClass.getName() + "] but was scanned after it. It will not be added");
					}
				} else {
					binderClasses.put(name, clazz);
					if(LOG.isDebugEnabled()) LOG.info("Loaded Binder [" + className + "] with key [" + name + "]");
				}
			} catch (Exception e) {
				LOG.error("Failed to load provider class [" + className + "]");
			}			
		}
	}
	
	
	/**
	 * Returns a provider instance for the passed bind token.
	 * @param bindToken the bind token from a SQL statement.
	 * @return a provider
	 * @throws ProviderNotFoundException
	 */
	public IBindVariableProvider getProvider(String bindToken) throws ProviderNotFoundException {
		if(bindToken==null) throw new ProviderNotFoundException("Bind token was null");
		IBindVariableProvider provider = providers.get(bindToken);
		if(provider!=null) return provider;
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(classLoader);
		try {
				ProviderToken pt = ProviderToken.parse(bindToken);
				Class<IBindVariableProvider> clazz = providerClasses.get(pt.getProviderTypeKey());
				if(clazz==null) throw new Exception("No class found for bind token key [" + pt.getProviderTypeKey() + "]");
				provider = clazz.newInstance();
				provider.setForceNoBind(pt.isForceNoBind());
				provider.setBindToken(bindToken);
				if(provider instanceof ApplicationContextAware) {
					((ApplicationContextAware)provider).setApplicationContext(appContext);
				}
				if(pt.isProviderConfigDefined()) provider.configureProvider(pt.getProviderConfig());
				if(pt.isBinderDefined()) {
					IBinder binder = getBinder(pt.getBinderToken());
					provider.setBinder(binder);
				}
				provider.registerListener(this);
				providers.put(bindToken, provider);
				insert(provider);
				return provider;
		} catch (Exception e) {
			LOG.error("Failed to get provider for bindToken [" + bindToken + "]", e);
			throw new ProviderNotFoundException("Failed to get provider for bindToken [" + bindToken + "]", e);
		} finally {
			Thread.currentThread().setContextClassLoader(cl);
		}
	}
	
	/**
	 * Returns the binder class for the passed binderTokenKey
	 * @param binderTokenKey the binder's token key
	 * @return the binder class for the passed binderTokenKey
	 */
	public Class<IBinder> getBinderClass(String binderTokenKey) {
		return binderClasses.get(binderTokenKey);
	}
	
	/**
	 * Returns the provider class for the passed providerTokenKey
	 * @param providerTokenKey the provider's token key
	 * @return the provider class for the passed providerTokenKey
	 */
	public Class<IBindVariableProvider> getBindVariableProviderClass(String providerTokenKey) {
		return providerClasses.get(providerTokenKey);
	}
	
	
	/**
	 * Returns an instance of a binder for the passed shorthand name
	 * @param binderToken the shorthand name and optional config
	 * @return a binder instance
	 * @throws BinderNotFoundException
	 */
	public IBinder getBinder(String binderToken) throws BinderNotFoundException  {
		String[] frags = binderToken.split(":");
		String name = frags[0];
		String config = frags.length>1 ? frags[1] : null;
		IBinder binder = binders.get(binderToken);
		if(binder!=null) return binder;
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(classLoader);
		try {
				Class<IBinder> clazz = binderClasses.get(name);
				if(clazz==null) throw new Exception("No class found for shorthand key [" + name + "]");
				binder = clazz.newInstance();
				if(config!=null) {
					binder.configureBinder(config);
				}
				binders.put(binderToken, binder);
				return binder;
		} catch (Exception e) {
			LOG.error("Failed to get binder for shorthand key [" + name + "]");
			throw new BinderNotFoundException("Failed to get binder for shorthand key [" + name + "]");
		} finally {
			Thread.currentThread().setContextClassLoader(cl);
		}		
	}
	
	
	/**
	 * Asynchronously propagates a provider value change event to all registered listeners.
	 * @param listeners The listeners to propagate to.
	 * @param provider The provider that fired the change.
	 * @param oldObject The old provider value.
	 * @param newObject The new provider value.
	 */
	public void fireProviderValueChanged(Set<IBindVariableProviderListener> listeners, IBindVariableProvider provider, Object oldObject, Object newObject) {
		ProviderChangeEvent pce = new ProviderChangeEvent(listeners, provider, oldObject, newObject);
		this.notificationThreadPool.submit(pce);
	}
	
	
	/**
	 * @param notificationThreadPool the notificationThreadPool to set
	 */
	public void setNotificationThreadPool(ExecutorService notificationThreadPool) {
		this.notificationThreadPool = notificationThreadPool;
	}
	
	
	/**
	 * Adds an additional set of URLs to scan for providers and binders.
	 * @param classPaths the classPath URLs to add.
	 */
	public void setClassPaths(Set<URL> classPaths) {
		if(classPaths!=null) {
			this.classPaths.addAll(classPaths);
		}
	}
	
	/**
	 * Manually adds a set of bind variable provider classes.
	 * @param providerClasses the provider Classes to add
	 */
	public void setProviderClasses(Map<String, Class<IBindVariableProvider>> providerClasses) {
		if(providerClasses!=null) {
			this.providerClasses.putAll(providerClasses);
		}
	}
	
	/**
	 * Manually adds a set of binder classes.
	 * @param binderClasses the binder Classes to add
	 */
	public void setBinderClasses(Map<String, Class<IBinder>> binderClasses) {
		if(binderClasses!=null) {
			this.binderClasses.putAll(binderClasses);
		}
		
	}


	/**
	 * Creates threads for the notification thread pool.
	 * @param r The runnable to be executed by the thread pool.
	 * @return A new thread.
	 * @see java.util.concurrent.ThreadFactory#newThread(java.lang.Runnable)
	 */
	public Thread newThread(Runnable r) {
		Thread t = new Thread(r);
		t.setDaemon(true);
		t.setName("ProviderChangeNotificationThread-" + serial.incrementAndGet());
		return t;
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
	
	/**
	 * Manually adds new provider instances with the specified token keys to the factory.
	 * @param providers A map of provider instances keyed by their unique token key.
	 */
	public void setProviders(Map<String, IBindVariableProvider> providers) {
		if(providers!=null) {
			for(Map.Entry<String, IBindVariableProvider> p: providers.entrySet()) {
				IBindVariableProvider provider = p.getValue();
				if(!this.providers.containsKey(p.getKey().toLowerCase())) {
					this.providers.put(p.getKey().toLowerCase(), provider);
				} else {
					LOG.warn("The provider with class [" + provider.getClass().getName() + "] and Token Key [" + p.getKey() + "] was not registered as a provider with that key was already registered.");
				}
			}
		}
	}

	/**
	 * A composite open type accessor that outlines the details of each bind provider in cache.
	 * @return A bind provider summary table 
	 */
	@ManagedAttribute
	public TabularDataSupport getBindProviders() {
		return bindProviderTable;
	}
	
	/**
	 * Returns the number of providers in cache
	 * @return the number of providers in cache
	 */
	@ManagedAttribute
	public int getBindProviderCount() {
		return providers.size();
	}
	
	/**
	 * Returns the number of binders in cache.
	 * @return the number of binders in cache.
	 */
	@ManagedAttribute
	public int getBinderCount() {
		return binders.size();
	}
	

	/**
	 * Fired when a provider's value changes.
	 * @param provider The provider that fired the change
	 * @param oldValue The old value
	 * @param newValue The new value
	 * @see org.helios.collectors.jdbc.binding.provider.IBindVariableProviderListener#onValueChanged(org.helios.collectors.jdbc.binding.provider.IBindVariableProvider, java.lang.Object, java.lang.Object)
	 */
	public void onValueChanged(IBindVariableProvider provider, Object oldValue, Object newValue) {
//		Notification not = new AttributeChangeNotification(this.objectName, notificationSequence.incrementAndGet(), System.currentTimeMillis(), "The value of bind provider [" + provider.getBindToken() + "] changed", provider.getBindToken(), provider.getBindTokenKey(), oldValue, newValue);
//		sendNotification(not);		
	}
	
	
}
