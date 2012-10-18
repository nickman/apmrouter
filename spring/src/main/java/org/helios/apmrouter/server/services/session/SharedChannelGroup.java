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
package org.helios.apmrouter.server.services.session;

import java.net.SocketAddress;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcaster;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;

import org.apache.log4j.Logger;
import org.helios.apmrouter.jmx.JMXHelper;
import org.helios.apmrouter.jmx.ThreadPoolFactory;
import org.helios.apmrouter.server.services.session.ChannelSessionEvent.ChannelSessionStartedEvent;
import org.helios.apmrouter.server.services.session.ChannelSessionEvent.ChannelSessionStoppedEvent;
import org.helios.apmrouter.util.SystemClock;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * <p>Title: SharedChannelGroup</p>
 * <p>Description: A common netty channel group for tracking all client and server channels that supports decorated channels.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.services.session.SharedChannelGroup</code></p>
 */
public class SharedChannelGroup implements ChannelGroup, ChannelFutureListener, ApplicationContextAware, SharedChannelGroupMXBean, NotificationBroadcaster  {
	/** The singleton instance */
	private static volatile SharedChannelGroup instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass());
	/** Thread pool for dispatchng events and notifications */
	protected final ThreadPoolExecutor threadPool;
	/** The JMX notification support delegate */
	protected final NotificationBroadcasterSupport notificationBroadcaster; 
	/** The application context */
	protected ApplicationContext applicationContext = null;
	/** A map of sets of channels keyed by their channel type */
	protected final Map<ChannelType, Set<DecoratedChannel>> channelsByType = Collections.synchronizedMap(new EnumMap<ChannelType, Set<DecoratedChannel>>(ChannelType.class));
	/** A map of remotely connected channels keyed by remote socket address */
	protected final Map<SocketAddress, DecoratedChannel> channelsByRemote = new ConcurrentHashMap<SocketAddress, DecoratedChannel>();
	
	/** The core delegate channel group */
	protected ChannelGroup channelGroup = new DefaultChannelGroup("APMRouterChannelGroup");
	/** A set of registered channel session listeners */
	protected final Set<ChannelSessionListener> listeners = new CopyOnWriteArraySet<ChannelSessionListener>();
	/** The pring scheduler for sessions that need to be pinged */
	protected final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2, new ThreadFactory(){
		private final AtomicInteger serial = new AtomicInteger(0);
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r, "SharedChannelGroupPingerThread#" + serial.incrementAndGet());
			t.setDaemon(true);
			return t;
		}
	});
	
	/** Serial number generator for jmx notifications */
	protected final AtomicLong jmxNotifSerial = new AtomicLong(0L);
	/**
	 * Retrieves the SharedChannelGroup singleton instance
	 * @return the SharedChannelGroup singleton instance
	 */
	public static SharedChannelGroup getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new SharedChannelGroup();
				}
			}
		}
		return instance;
	}
	
	
	
	/**
	 * Creates a new SharedChannelGroup
	 */
	private SharedChannelGroup() {
		for(ChannelType type: ChannelType.values()) {
			channelsByType.put(type, new CopyOnWriteArraySet<DecoratedChannel>());
		}
		threadPool = (ThreadPoolExecutor) ThreadPoolFactory.newCachedThreadPool("org.helios.apmrouter.session", "SharedChannelGroupPool");
		notificationBroadcaster = new NotificationBroadcasterSupport(threadPool, new MBeanNotificationInfo[]{
				new MBeanNotificationInfo(new String[]{NEW_SESSION_EVENT}, Notification.class.getName(), "Notification broadcast when a channel session is initiated"),
				new MBeanNotificationInfo(new String[]{CLOSED_SESSION_EVENT}, Notification.class.getName(), "Notification broadcast when a channel session is closed"),
				new MBeanNotificationInfo(new String[]{IDENTIFIED_SESSION_EVENT}, Notification.class.getName(), "Notification broadcast when a channel is identified")
		});
		JMXHelper.registerMBean(OBJECT_NAME, this);
		scheduler.scheduleAtFixedRate(new Runnable(){
			public void run() {
				for(DecoratedChannel dc: channelsByRemote.values()) {
					if(dc.host==null || dc.agent==null) {
						dc.sendWho();
					}
				}
			}
		}, 3000, 3000, TimeUnit.MILLISECONDS);
		
	}
	
	/**
	 * Returns an array of the decorated channels
	 * @return an array of the decorated channels
	 */
	public DecoratedChannelMBean[] getChannels() {
		return toArray(new DecoratedChannelMBean[size()]);
	}
	
	/**
	 * Registers a new session listener
	 * @param listener the listener to register
	 */
	public void addSessionListener(ChannelSessionListener listener) {
		if(listener!=null) {
			listeners.add(listener);
		}
	}
	
	/**
	 * Removes a registered session listener
	 * @param listener the listener to remove
	 */
	public void removeSessionListener(ChannelSessionListener listener) {
		if(listener!=null) {
			listeners.remove(listener);
		}
	}
	
	
	/**
	 * ChannelFutureListener impl that removes Channels from the group when they close.
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelFutureListener#operationComplete(org.jboss.netty.channel.ChannelFuture)
	 */
	@Override
	public void operationComplete(ChannelFuture f) throws Exception {
		if(f.isDone()) {
			remove(f.getChannel());
		}
	}
	
	/**
	 * Returns the channel connected to the passed remote address
	 * @param sa the remote address
	 * @return a channel or null if one was not found
	 */
	public Channel getByRemote(SocketAddress sa) {
		if(sa==null) return null;
		return channelsByRemote.get(sa);
	}
	
	/**
	 * Converts the passed channel to a {@link DecoratedChannel} and adds it to the channel group 
	 * @param channel The channel to add
	 * @param type The channel type
	 * @param name The assigned channel name
	 * @return true if the channel was not already registered in this channel group, false otherwise
	 */
	public boolean add(Channel channel, ChannelType type, String name) {
		return add((channel instanceof DecoratedChannel) ? channel : new DecoratedChannel(channel, type, name));
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.util.Set#add(java.lang.Object)
	 */
	@Override
	public boolean add(Channel channel) {
		final DecoratedChannel dchannel;
		if(channel instanceof DecoratedChannel) {
			dchannel = (DecoratedChannel)channel;
		} else {
			dchannel = new DecoratedChannel(channel, ChannelType.OTHER, channel.getClass().getSimpleName());
		}
		
		boolean isNew =  channelGroup.add(dchannel); 
		if(isNew) {
			SocketAddress remote = dchannel.getRemoteAddress();
			if(remote!=null && !channelsByRemote.containsKey(remote)) {
				channelsByRemote.put(dchannel.getRemoteAddress(), dchannel);
			}
			channelsByType.get(dchannel.getChannelType()).add(dchannel);
			dchannel.getCloseFuture().addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					remove(dchannel);
				}
			});
			registerWithListeners(dchannel);
			if(dchannel.getDelegate() instanceof VirtualUDPChannel) {
				final VirtualUDPChannel vuc = (VirtualUDPChannel)dchannel.getDelegate();
				vuc.setPingSchedule(
						scheduler.scheduleAtFixedRate(new Runnable(){
							public void run() {
								vuc.ping();
							}
						}, 10000, 10000, TimeUnit.MILLISECONDS)
				);
			}
		} 
		return isNew;
	}
	
	
	/**
	 * @param dchannel
	 * FIXME: MUTLITHREAD, EVENTS, NOTIFS, SEND NEW CHANNEL
	 */
	private void registerWithListeners(final DecoratedChannel dchannel) {
		sendChannelConnectedEvent(dchannel);		
		final Set<ChannelSessionListener> forwardTo = new HashSet<ChannelSessionListener>();
		ChannelFutureListener relay = new ChannelFutureListener() {			
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				sendChannelClosedEvent(dchannel);
				for(final ChannelSessionListener listener: forwardTo) {
					threadPool.submit(new Runnable() {
						@Override
						public void run() {
							listener.onClosedChannel(dchannel);
						}
					});					
				}				
			}
		};
		for(final ChannelSessionListener listener: listeners) {
			if(listener instanceof FilteredChannelSessionListener) {
				if(((FilteredChannelSessionListener)listener).include(dchannel)) {
					forwardTo.add(listener);
				}
			} else {
				forwardTo.add(listener);				
			}
		}
		dchannel.getCloseFuture().addListener(relay);		
	}
	
	
	/**
	 * Sends a channel closed event to jmx and spring
	 * @param dchannel The closed channel
	 */
	protected void sendChannelClosedEvent(final DecoratedChannelMBean dchannel) {
		Notification notif = new Notification(CLOSED_SESSION_EVENT, OBJECT_NAME, jmxNotifSerial.incrementAndGet(), SystemClock.time(), "Channel Session Closed [" + dchannel.toString() + "]");
		notif.setUserData(dchannel);
		sendNotification(notif);
		if(applicationContext != null) {
			threadPool.submit(new Runnable() {
				@Override
				public void run() {
					applicationContext.publishEvent(new ChannelSessionStoppedEvent(dchannel));
				}
			});								
		}		
	}
	
	/**
	 * Sends a channel connected event to listeners, jmx and spring
	 * @param dchannel The connected channel
	 */
	protected void sendChannelConnectedEvent(final DecoratedChannelMBean dchannel) {
		for(final ChannelSessionListener listener: listeners) {
			if(listener instanceof FilteredChannelSessionListener) {
				if(((FilteredChannelSessionListener)listener).include(dchannel)) {
					threadPool.submit(new Runnable() {
						@Override
						public void run() {
							listener.onConnectedChannel((DecoratedChannel)dchannel);
						}
					});					
					
				}
			} else {
				threadPool.submit(new Runnable() {
					@Override
					public void run() {
						listener.onConnectedChannel((DecoratedChannel)dchannel);
					}
				});									
			}
		}
		Notification notif = new Notification(NEW_SESSION_EVENT, OBJECT_NAME, jmxNotifSerial.incrementAndGet(), SystemClock.time(), "Channel Session Started [" + dchannel.toString() + "]");
		notif.setUserData(dchannel);
		sendNotification(notif);
		if(applicationContext != null) {
			threadPool.submit(new Runnable() {
				@Override
				public void run() {
					applicationContext.publishEvent(new ChannelSessionStartedEvent(dchannel));
				}
			});								
		}
	}
	
	/**
	 * Sends a JMX notification indicating that a channel session has been identified.
	 * @param dchannel The channel that has been identified.
	 */
	public void sendIdentifiedChannelEvent(final DecoratedChannel dchannel) {
		for(final ChannelSessionListener listener: listeners) {
			if(listener instanceof FilteredChannelSessionListener) {
				if(((FilteredChannelSessionListener)listener).include(dchannel)) {
					threadPool.submit(new Runnable() {
						@Override
						public void run() {
							listener.onIdentifiedChannel(dchannel);
						}
					});					
					
				}
			} else {
				threadPool.submit(new Runnable() {
					@Override
					public void run() {
						listener.onIdentifiedChannel(dchannel);
					}
				});									
			}
		}
		
		Notification notif = new Notification(IDENTIFIED_SESSION_EVENT, OBJECT_NAME, jmxNotifSerial.incrementAndGet(), SystemClock.time(), "Channel Session Identified [" + dchannel.host + "/" + dchannel.agent + "]");
		notif.setUserData(dchannel);
		sendNotification(notif);
	}

	/**
	 * Removes a channel from the ChannelGroup
	 * @param channel The channel to remove
	 * @return true if the channel was present and was removed
	 * @see java.util.Set#remove(java.lang.Object)
	 */
	public boolean remove(Channel channel) {
		if(channel instanceof DecoratedChannel) {
			DecoratedChannel dchannel = (DecoratedChannel)channel;
			channelsByType.get(dchannel.getChannelType()).remove(dchannel);
			SocketAddress sa = dchannel.getRemoteAddress();
			if(sa!=null) {
				channelsByRemote.remove(sa);
			} else {
				for(Iterator<DecoratedChannel> iter = channelsByRemote.values().iterator(); iter.hasNext();) {
					if(iter.next().equals(dchannel)) {
						iter.remove();
						break;
					}
				}
			}
			return channelGroup.remove(dchannel);
		}
		return channelGroup.remove(channel);
	}	
	

	/**
	 * {@inheritDoc}
	 * @see java.util.Set#remove(java.lang.Object)
	 */
	@Override
	public boolean remove(Object obj) {
		if(obj!=null && obj instanceof Channel) {
			return remove((Channel)obj);
		} 
		return false;
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.util.Set#size()
	 */
	@Override
	public int size() {
		return channelGroup.size();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.services.session.SharedChannelGroupMXBean#getSize()
	 */
	public int getSize() {
		return size();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.services.session.SharedChannelGroupMXBean#isEmpty()
	 */
	@Override
	public boolean isEmpty() {
		return channelGroup.isEmpty();
	}

	/**
	 * {@inheritDoc}
	 * @see java.util.Set#contains(java.lang.Object)
	 */
	@Override
	public boolean contains(Object o) {
		return channelGroup.contains(o);
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(ChannelGroup o) {
		return channelGroup.compareTo(o);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.services.session.SharedChannelGroupMXBean#getName()
	 */
	@Override
	public String getName() {
		return channelGroup.getName();
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.group.ChannelGroup#find(java.lang.Integer)
	 */
	@Override
	public Channel find(Integer id) {
		return channelGroup.find(id);
	}

	/**
	 * {@inheritDoc}
	 * @see java.util.Set#iterator()
	 */
	@Override
	public Iterator<Channel> iterator() {
		return channelGroup.iterator();
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.group.ChannelGroup#setInterestOps(int)
	 */
	@Override
	public ChannelGroupFuture setInterestOps(int interestOps) {
		return channelGroup.setInterestOps(interestOps);
	}

	/**
	 * {@inheritDoc}
	 * @see java.util.Set#toArray()
	 */
	@Override
	public Object[] toArray() {
		return channelGroup.toArray();
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.group.ChannelGroup#setReadable(boolean)
	 */
	@Override
	public ChannelGroupFuture setReadable(boolean readable) {
		return channelGroup.setReadable(readable);
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.group.ChannelGroup#write(java.lang.Object)
	 */
	@Override
	public ChannelGroupFuture write(Object message) {
		return channelGroup.write(message);
	}

	/**
	 * {@inheritDoc}
	 * @see java.util.Set#toArray(T[])
	 */
	@Override
	public <T> T[] toArray(T[] a) {
		return channelGroup.toArray(a);
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.group.ChannelGroup#write(java.lang.Object, java.net.SocketAddress)
	 */
	@Override
	public ChannelGroupFuture write(Object message, SocketAddress remoteAddress) {
		return channelGroup.write(message, remoteAddress);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.services.session.SharedChannelGroupMXBean#disconnect()
	 */
	@Override
	public ChannelGroupFuture disconnect() {
		return channelGroup.disconnect();
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.group.ChannelGroup#unbind()
	 */
	@Override
	public ChannelGroupFuture unbind() {
		return channelGroup.unbind();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.services.session.SharedChannelGroupMXBean#close()
	 */
	@Override
	public ChannelGroupFuture close() {
		return channelGroup.close();
	}

	/**
	 * {@inheritDoc}
	 * @see java.util.Set#containsAll(java.util.Collection)
	 */
	@Override
	public boolean containsAll(Collection<?> c) {
		return channelGroup.containsAll(c);
	}

	/**
	 * {@inheritDoc}
	 * @see java.util.Set#addAll(java.util.Collection)
	 */
	@Override
	public boolean addAll(Collection<? extends Channel> c) {
		boolean changed = false;
		for(Channel channel: c) {
			boolean ch = add(channel);
			if(ch) changed = true;
		}
		return changed;
	}

	/**
	 * {@inheritDoc}
	 * @see java.util.Set#retainAll(java.util.Collection)
	 */
	@Override
	public boolean retainAll(Collection<?> c) {
		boolean changed = false;
		Set<Channel> remove = new HashSet<Channel>();
		for(Iterator<Channel> iter = iterator(); iter.hasNext();) {
			Channel channel = iter.next();
			if(!c.contains(channel)) {
				remove.add(channel);
			}
		}
		for(Channel channel: remove) {
			boolean ch = remove(channel);
			if(ch) changed = true;
		}		
		return changed;
	}

	/**
	 * {@inheritDoc}
	 * @see java.util.Set#removeAll(java.util.Collection)
	 */
	@Override
	public boolean removeAll(Collection<?> c) {
		boolean changed = false;
		Set<Channel> remove = new HashSet<Channel>();
		for(Iterator<Channel> iter = iterator(); iter.hasNext();) {
			Channel channel = iter.next();
			if(c.contains(channel)) {
				remove.add(channel);
			}
		}
		for(Channel channel: remove) {
			boolean ch = remove(channel);
			if(ch) changed = true;
		}		
		return changed;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.services.session.SharedChannelGroupMXBean#clear()
	 */
	@Override
	public void clear() {
		for(Set<DecoratedChannel> set : channelsByType.values()) {
			set.clear();
		}
		channelGroup.clear();
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object o) {
		return channelGroup.equals(o);
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return channelGroup.hashCode();
	}
	
	// ==============================================================================
	// 		Notification Broadcaster Support
	// ==============================================================================
	
	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationBroadcaster#addNotificationListener(javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
	 */
	@Override
	public void addNotificationListener(NotificationListener listener,
			NotificationFilter filter, Object handback) {
		notificationBroadcaster.addNotificationListener(listener, filter,
				handback);
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationBroadcaster#removeNotificationListener(javax.management.NotificationListener)
	 */
	@Override
	public void removeNotificationListener(NotificationListener listener)
			throws ListenerNotFoundException {
		notificationBroadcaster.removeNotificationListener(listener);
	}

	/**
	 * Removes a notification listener registered with a specific filter
	 * @param listener The listener to remove
	 * @param filter The filter the listener was added with
	 * @param handback The handback the listener was added with
	 * @throws ListenerNotFoundException thrown if the passed listener was not registered
	 * @see javax.management.NotificationBroadcasterSupport#removeNotificationListener(javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
	 */
	public void removeNotificationListener(NotificationListener listener,
			NotificationFilter filter, Object handback)
			throws ListenerNotFoundException {
		notificationBroadcaster.removeNotificationListener(listener, filter,
				handback);
	}


	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationBroadcaster#getNotificationInfo()
	 */
	@Override
	public MBeanNotificationInfo[] getNotificationInfo() {
		return notificationBroadcaster.getNotificationInfo();
	}

	/**
	 * Sends a notification
	 * @param notification the notification to send
	 * @see javax.management.NotificationBroadcasterSupport#sendNotification(javax.management.Notification)
	 */
	public void sendNotification(Notification notification) {
		notificationBroadcaster.sendNotification(notification);
	}

	/**
	 * {@inheritDoc}
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}
	
}
