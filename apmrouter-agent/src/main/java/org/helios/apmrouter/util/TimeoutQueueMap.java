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
package org.helios.apmrouter.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;


/**
 * <p>Title: TimeoutQueueMap</p>
 * <p>Description: A map to store value that will be ejected after some period of time if not removed, notifying registered timeout listeners</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.util.TimeoutQueueMap</code></p>
 * @param <K> The key type for this map
 * @param <V> The value type for this maps
 */
public class TimeoutQueueMap<K, V>  implements Runnable, Map<K, V> {
	/** The timeout queue */
	protected final DelayQueue<TimeoutQueueMapKey<K, V>> timeOutQueue = new DelayQueue<TimeoutQueueMapKey<K, V>>();  
	/** The reference map */
	protected final Map<K, V> referenceMap;
	/** The default delay time */
	protected final long defaultDelayTime;
	/** The timeout thread */
	protected final Thread timeoutThread;
	/** A set of registered timeout listeners */
	protected final Set<TimeoutListener<K, V>> timeOutListeners = new CopyOnWriteArraySet<TimeoutListener<K, V>>();
	/** The number of timeout events that have occured */
	protected final AtomicLong timeOutCount = new AtomicLong(0L);
	/** A flag indicating that the timeout thread should keep running */
	protected boolean running = true;
	/** A serial number factory for timeout threads */
	private static final AtomicLong serial = new AtomicLong(0L);
	/** The thread group that timeout threads run in */
	private static final ThreadGroup timeOutThreadGroup = new ThreadGroup(TimeoutQueueMap.class.getSimpleName() + "ThreadGroup");
	
	

	/**
	 * Creates a new TimeoutQueueMap
	 * @param defaultDelayTime The default delay time in ms. for delayed puts 
	 * @param initialCapacity the initial capacity. The implementation performs internal sizing to accommodate this many elements.
	 * @param loadFactor the load factor threshold, used to control resizing. Resizing may be performed when the average number of elements per bin exceeds this threshold.
	 * @param concurrencyLevel the estimated number of concurrently updating threads. The implementation performs internal sizing to try to accommodate this many threads. 
	 */
	public TimeoutQueueMap(long defaultDelayTime, int initialCapacity, float loadFactor, int concurrencyLevel) {
		referenceMap = new ConcurrentHashMap<K, V>(initialCapacity, loadFactor, concurrencyLevel);
		this.defaultDelayTime = defaultDelayTime;
		timeoutThread = new Thread(timeOutThreadGroup, this, TimeoutQueueMap.class.getSimpleName() + "Thread#" + serial.incrementAndGet());
		timeoutThread.setDaemon(true);
		timeoutThread.start();
	}
	
	/**
	 * Creates a new TimeoutQueueMap with the default concurrencyLevel (16). 
	 * @param defaultDelayTime The default delay time in ms. for delayed puts 
	 * @param initialCapacity the initial capacity. The implementation performs internal sizing to accommodate this many elements.
	 * @param loadFactor the load factor threshold, used to control resizing. Resizing may be performed when the average number of elements per bin exceeds this threshold.
	 */
	public TimeoutQueueMap(long defaultDelayTime, int initialCapacity, float loadFactor) {
		this(defaultDelayTime, initialCapacity, loadFactor, 16);
	}	
	
	/**
	 * Creates a new TimeoutQueueMap with default load factor (0.75) and concurrencyLevel (16).
	 * @param defaultDelayTime The default delay time in ms. for delayed puts 
	 * @param initialCapacity the initial capacity. The implementation performs internal sizing to accommodate this many elements.
	 */
	public TimeoutQueueMap(long defaultDelayTime, int initialCapacity) {
		this(defaultDelayTime, initialCapacity, 0.75f, 16);
	}
	
	/**
	 * Creates a new TimeoutQueueMap with a default initial capacity (16), load factor (0.75) and concurrencyLevel (16). 
	 * @param defaultDelayTime The default delay time in ms. for delayed puts 
	 */
	public TimeoutQueueMap(long defaultDelayTime) {
		this(defaultDelayTime, 16, 0.75f, 16);
	}		
	
	
	public static void main(String[] args) {
		log("TimeoutQueueMap Test");
		TimeoutQueueMap<Long, String> tqm = new TimeoutQueueMap<Long, String>(500);
		tqm.addListener(new TimeoutListener<Long, String>(){
			public void onTimeout(Long key, String value) {
				log("TIMEOUT:[" + key + "][" + value + "]");
			}
		});
		tqm.put(1L, "One");
		String one = tqm.remove(1L);
		log("Pulled One: [" + one + "]  TQM Size:" + tqm.getSize() );
		tqm.put(2L, "Two");
		try { Thread.sleep(600); } catch (Exception e) {}
		String two = tqm.remove(2L);
		log("Two should be null:" + (two==null));
		log(tqm);
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	/**
	 * Registers a timeout listener
	 * @param listener The listener to register
	 */
	public void addListener(TimeoutListener<K, V> listener) {
		if(listener!=null) {
			timeOutListeners.add(listener);
		}
	}
	
	/**
	 * Unregisters a timeout listener
	 * @param listener The listener to unregister
	 */
	public void removeListener(TimeoutListener<K, V> listener) {
		if(listener!=null) {
			timeOutListeners.remove(listener);
		}
	}	
	
	/**
	 * Registers a name/value binding which will timeout in the passed delay time unless removed before the delay time
	 * @param key The key to retrieve the value by
	 * @param value The value to delay
	 * @param delayTime The timeout delay time in ms.
	 * @return The replaced value
	 */
	public synchronized V put(K key, V value, long delayTime) {
		if(!running) throw new IllegalStateException("This TimeoutQueueMap has been shutdown", new Throwable());
		V oldValue = referenceMap.put(key, value);
		if(oldValue!=null) {
			timeOutQueue.remove(getKeyFor(key));
		}
		timeOutQueue.add(getKeyFor(key, value, delayTime));
		return oldValue;
	}
	
	
	/**
	 * @param key
	 * @param value
	 * @param delayTime
	 * @return
	 */
	protected TimeoutQueueMapKey<K, V> getKeyFor(K key, V value, long delayTime) {
		return new TimeoutQueueMapKey<K, V>(key, value, delayTime);
	}

	/**
	 * Registers a name/value binding which will timeout in the default delay time unless removed before the delay time
	 * @param key The key to retrieve the value by
	 * @param value THe value to delay
	 */	
	public V put(K key, V value) {
		if(!running) throw new IllegalStateException("This TimeoutQueueMap has been shutdown", new Throwable());
		return put(key, value, defaultDelayTime);
	}
	
	/**
	 * Removes a delayed value from the timeout queue map.
	 * If the value is removed before it times out, the value is returned successfully.
	 * If the delay has expired, will return null
	 * @param key The delayed value key
	 * @return The delayed value if it has not timed out, null otherwise
	 */
	public V remove(Object key) {
		TimeoutQueueMapKey<K, V> tKey = getKeyFor(key);
		boolean removed = timeOutQueue.remove(tKey);
		V value = referenceMap.remove(key);
		if(!removed && value!=null) {
			// This should never happen
			//throw new IllegalStateException("The referenced value [" + value + "] with key [" + key + "] could not be removed from the timeout queue, but was retrieved from the map. Programmer Error ?", new Throwable());
			return null;
		}
		return value;
	}
	
	/**
	 * Polls the timeout queue for timedout values
	 * {@inheritDoc}
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		while(running) {
			try {
				TimeoutQueueMapKey<K, V> mapKey = timeOutQueue.take();
				referenceMap.remove(mapKey.key);
				timeOutCount.incrementAndGet();
				for(TimeoutListener<K, V> listener: timeOutListeners) {
					if(listener instanceof ValueFilteredTimeoutListener) {
						ValueFilteredTimeoutListener<K,V> filteringListener = (ValueFilteredTimeoutListener<K,V>)listener;
						if(filteringListener.include(mapKey.delayed)) {
							filteringListener.onTimeout(mapKey.key, mapKey.delayed);
						}
					} else {
						listener.onTimeout(mapKey.key, mapKey.delayed);
					}
				}
			} catch (Exception e) {	
				if(!running) return;
			}
		}
	}
	
	/**
	 * Returns the number of timeouts that have occured
	 * @return the number of timeouts that have occured
	 */
	public long getTimeOutCount() {
		return timeOutCount.get();
	}
	
	/**
	 * Returns the number of pending delayed items
	 * @return the number of pending delayed items
	 */
	public int getSize() {
		return referenceMap.size();
	}
	
	public void shutdown() {
		timeoutThread.interrupt();
		referenceMap.clear();
		timeOutQueue.clear();
	}
	
	/**
	 * Purges thus timeout queue map and removes all entries.
	 * A bit iffy thread-wise and has race condition issues.
	 */
	public synchronized void purge(){
		timeOutQueue.clear();
		referenceMap.clear();
	}
	
	TimeoutQueueMapKey getKeyFor(Object key) {
		return new TimeoutQueueMapKey(key, null, -1);
	}
	
	/**
	 * <p>Title: TimeoutQueueMapKey</p>
	 * <p>Description: A timestamped {@link Delayed} wrapper around a referenced object</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.patterns.queues.TimeoutQueueMap.TimeoutQueueMapKey</code></p>
	 */
	protected class TimeoutQueueMapKey<K, V> implements Delayed {
		/** The referenced delay object */
		protected final V delayed;
		/** The referenced delay object key */
		protected final K key;
		
		/** The timestamp of the creation of this delayed instance */
		protected final long timestamp;
		
	

		/**
		 * Creates a new TimeoutQueueMapKey
		 * @param key The delayed value key
		 * @param delayed The object to wrap as a delay
		 * @param delayTime The number of milliseconds to delay this object for
		 */
		public TimeoutQueueMapKey(K key, V delayed, long delayTime) {
			this.delayed = delayed;
			this.key = key;
			this.timestamp = System.currentTimeMillis() + delayTime;
		}

		/**
		 * Compares this delayed with the specified delayed for order. 
		 * Returns a negative integer, zero, or a positive integer as this object is less than, equal to, 
		 * or greater than the specified object. 
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		@Override
		public int compareTo(Delayed delayed) {
			long thisDelay = getDelay(TimeUnit.MILLISECONDS);
			long thatDelay = delayed.getDelay(TimeUnit.MILLISECONDS);
			return thisDelay <= thatDelay ? -1: 1; 			
		}

		/**
		 * Returns the time remaining on the delay for this object.
		 * {@inheritDoc}
		 * @see java.util.concurrent.Delayed#getDelay(java.util.concurrent.TimeUnit)
		 */
		@Override
		public long getDelay(TimeUnit unit) {
			return unit.convert((timestamp-System.currentTimeMillis()), TimeUnit.MILLISECONDS);
		}



		/**
		 * Constructs a <code>String</code> with all attributes
		 * in name = value format.
		 *
		 * @return a <code>String</code> representation 
		 * of this object.
		 */
		public String toString() {
		    final String TAB = "\n\t";
		    StringBuilder retValue = new StringBuilder("TimeoutQueueMapKey [")
		    	.append(TAB).append("key:").append(this.key)
		        .append(TAB).append("delayed:").append(this.delayed)
		        .append(TAB).append("timestamp:").append(this.timestamp)
		        .append(TAB).append("delay:").append(getDelay(TimeUnit.MILLISECONDS)).append(" ms.")
		        .append("\n]");    
		    return retValue.toString();
		}

		/**
		 * Returns 
		 * @return the delayed
		 */
		public V getDelayed() {
			return delayed;
		}

		/**
		 * Returns 
		 * @return the key
		 */
		public K getKey() {
			return key;
		}

		/**
		 * Returns 
		 * @return the timestamp
		 */
		public long getTimestamp() {
			return timestamp;
		}

		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((key == null) ? 0 : key.hashCode());
			return result;
		}

		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (obj == null)
				return false;
			if(obj instanceof TimeoutQueueMapKey) {
				TimeoutQueueMapKey other = (TimeoutQueueMapKey)obj;
				return key.equals(other.key);
			}
			return false;
		}

		private TimeoutQueueMap getOuterType() {
			return TimeoutQueueMap.this;
		}
	}
	/**
	 * Constructs a <code>String</code> with all attributes
	 * in name = value format.
	 *
	 * @return a <code>String</code> representation 
	 * of this object.
	 */
	public String toString() {
	    final String TAB = "\n\t";
	    StringBuilder retValue = new StringBuilder("TimeoutQueueMap [")
	        .append(TAB).append("timeOutQueue:").append(this.timeOutQueue)
	        .append(TAB).append("referenceMap:").append(this.referenceMap)
	        .append(TAB).append("defaultDelayTime:").append(this.defaultDelayTime)
	        .append(TAB).append("timeoutThread:").append(this.timeoutThread)
	        .append(TAB).append("timeOutListeners:").append(this.timeOutListeners)
	        .append(TAB).append("timeOutCount:").append(this.timeOutCount)
	        .append("\n]");    
	    return retValue.toString();
	}

	/**
	 * @return
	 * @see java.util.Map#size()
	 */
	public int size() {
		return referenceMap.size();
	}

	/**
	 * @return
	 * @see java.util.Map#isEmpty()
	 */
	public boolean isEmpty() {
		return referenceMap.isEmpty();
	}

	/**
	 * @param key
	 * @return
	 * @see java.util.Map#containsKey(java.lang.Object)
	 */
	public boolean containsKey(Object key) {
		return referenceMap.containsKey(key);
	}

	/**
	 * @param value
	 * @return
	 * @see java.util.Map#containsValue(java.lang.Object)
	 */
	public boolean containsValue(Object value) {
		return referenceMap.containsValue(value);
	}

	/**
	 * @param key
	 * @return
	 * @see java.util.Map#get(java.lang.Object)
	 */
	public V get(Object key) {
		return referenceMap.get(key);
	}



	/**
	 * Puts all the elements in the passed map into this map
	 * @param m The map of members to add
	 * @see java.util.Map#putAll(java.util.Map)
	 */
	public void putAll(Map<? extends K, ? extends V> m) {
		if(!running) throw new IllegalStateException("This TimeoutQueueMap has been shutdown", new Throwable());
		for(Map.Entry<? extends K, ? extends V> entry: m.entrySet()) {
			put(entry.getKey(), entry.getValue());
		}
		referenceMap.putAll(m);
	}
	
	/**
	 * Puts all the elements in the passed map into this map
	 * @param m The map of members to add
	 * @param timeout The timeout in ms. of the elements being added
	 * @see java.util.Map#putAll(java.util.Map)
	 */
	public void putAll(Map<? extends K, ? extends V> m, long timeout) {
		if(!running) throw new IllegalStateException("This TimeoutQueueMap has been shutdown", new Throwable());
		for(Map.Entry<? extends K, ? extends V> entry: m.entrySet()) {
			put(entry.getKey(), entry.getValue(), timeout);
		}
		referenceMap.putAll(m);
	}
	

	/**
	 * 
	 * @see java.util.Map#clear()
	 */
	public void clear() {
		purge();
	}

	/**
	 * @return
	 * @see java.util.Map#keySet()
	 */
	public Set<K> keySet() {
		return referenceMap.keySet();
	}

	/**
	 * @return
	 * @see java.util.Map#values()
	 */
	public Collection<V> values() {
		return referenceMap.values();
	}
	
	private class ReadOnlyEntry implements  Entry<K,V> {
		private final Entry<K,V> inner;
		/**
		 * Creates a new ReadOnlyEntry
		 * @param inner
		 */
		public ReadOnlyEntry(java.util.Map.Entry<K, V> inner) {
			this.inner = inner;
		}
		public K getKey() {
			return inner.getKey();
		}
		public V getValue() {			
			return inner.getValue();
		}
		public V setValue(V value) {
			throw new UnsupportedOperationException("Setting values in Entries is not supported", new Throwable());
		}				
	}
	
	

	/**
	 * Returns a read only entry set
	 * @return a read only entry set
	 * @see java.util.Map#entrySet()
	 */
	public Set<Entry<K, V>> entrySet() {
		Set<Entry<K, V>> entrySet = new HashSet<Entry<K, V>>(size());
		for(Entry<K,V> entry: referenceMap.entrySet()) {
			entrySet.add(new ReadOnlyEntry(entry));
		}
		return entrySet;
	}

	/**
	 * @param o
	 * @return
	 * @see java.util.Map#equals(java.lang.Object)
	 */
	public boolean equals(Object o) {
		return referenceMap.equals(o);
	}

	/**
	 * @return
	 * @see java.util.Map#hashCode()
	 */
	public int hashCode() {
		return referenceMap.hashCode();
	}
}
