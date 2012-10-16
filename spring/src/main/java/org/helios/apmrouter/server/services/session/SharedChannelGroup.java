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
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.DefaultChannelGroup;

/**
 * <p>Title: SharedChannelGroup</p>
 * <p>Description: A common netty channel group for tracking all client and server channels that supports decorated channels.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.services.session.SharedChannelGroup</code></p>
 */
public class SharedChannelGroup implements ChannelGroup, ChannelFutureListener {
	/** The singleton instance */
	private static volatile SharedChannelGroup instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass());
	
	/** A map of sets of channels keyed by their channel type */
	protected final Map<ChannelType, Set<DecoratedChannel>> channelsByType = new EnumMap<ChannelType, Set<DecoratedChannel>>(ChannelType.class);
	/** The core delegate channel group */
	protected ChannelGroup channelGroup = new DefaultChannelGroup("APMRouterChannelGroup");
	/** A set of registered channel session listeners */
	protected final Set<ChannelSessionListener> listeners = new CopyOnWriteArraySet<ChannelSessionListener>();
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
			channelsByType.get(dchannel.getChannelType()).add(dchannel);
			dchannel.getCloseFuture().addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					remove(dchannel);
				}
			});
			registerWithListeners(dchannel);
			log.info("Adding Channel From [" + channel.getPipeline().getLast().getClass().getSimpleName() + "/\t" + channel.getId() + "]");
		} else {
			log.info("Channel From [" + channel.getPipeline().getLast().getClass().getSimpleName() + "/\t" + channel.getId() + "] already registered");
		}
		return isNew;
	}
	
	/**
	 * @param dchannel
	 * FIXME: MUTLITHREAD, EVENTS, NOTIFS, SEND NEW CHANNEL
	 */
	private void registerWithListeners(final DecoratedChannel dchannel) {
		if(listeners.isEmpty()) return;
		final Set<ChannelSessionListener> forwardTo = new HashSet<ChannelSessionListener>();
		ChannelFutureListener relay = new ChannelFutureListener() {			
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				for(ChannelSessionListener listener: forwardTo) {
					listener.onClosedChannel(dchannel);
				}				
			}
		};
		for(ChannelSessionListener listener: listeners) {
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
	 * Removes a channel from the ChannelGroup
	 * @param channel The channel to remove
	 * @return true if the channel was present and was removed
	 * @see java.util.Set#remove(java.lang.Object)
	 */
	public boolean remove(Channel channel) {
		if(channel instanceof DecoratedChannel) {
			DecoratedChannel dchannel = (DecoratedChannel)channel;
			channelsByType.get(dchannel.getChannelType());
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
	 * @see java.util.Set#isEmpty()
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
	 * @see org.jboss.netty.channel.group.ChannelGroup#getName()
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
	 * @see org.jboss.netty.channel.group.ChannelGroup#disconnect()
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
	 * @see org.jboss.netty.channel.group.ChannelGroup#close()
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
	 * @see java.util.Set#clear()
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
}
