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
package org.helios.apmrouter.destination.mongodb;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.Notification;

import org.helios.apmrouter.catalog.jdbc.h2.AbstractTrigger;
import org.helios.apmrouter.catalog.jdbc.h2.NewElementTriggers;
import org.helios.apmrouter.destination.BaseDestination;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.mongodb.Mongo;

/**
 * <p>Title: MongoDbDestination</p>
 * <p>Description: Destination for a MongoDb database. Also handles secondary catalog updates.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.mongodb.MongoDbDestination</code></p>
 */

public class MongoDbDestination extends BaseDestination implements Runnable {
	/** The mongo DB template */
	protected MongoTemplate mongoTemplate = null;
	/** The raw mongo connection */
	protected Mongo mongo = null;
	/** The catalog update queue to read updates from */
	protected final BlockingQueue<Notification> notificationQueue = NewElementTriggers.notificationQueue;
	/** The catalog queue processor thread */
	protected Thread catalogProcessorThread = null;
	/** Thread run indicator */
	protected final AtomicBoolean keepRunning = new AtomicBoolean(false);
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		while(true) {
			try {
				Notification notif = notificationQueue.take();
				if(notif!=null) {
					String type = notif.getType();
					if(AbstractTrigger.NEW_HOST.equals(type)) {
						
					} else if(AbstractTrigger.NEW_AGENT.equals(type)) {
						
					} else if(AbstractTrigger.NEW_METRIC.equals(type)) {
						
					}
				}
			} catch (InterruptedException iex) {
				if(keepRunning.get()) {
					Thread.interrupted();
				}
			}
		}
	}
	
	/**
	 * Creates a new MongoDbDestination
	 * @param patterns The metric patterns accepted by this destination
	 */
	public MongoDbDestination(String... patterns) {
		super(patterns);
	}

	/**
	 * Creates a new MongoDbDestination
	 * @param patterns The metric patterns accepted by this destination
	 */
	public MongoDbDestination(Collection<String> patterns) {
		super(patterns);
	}

	/**
	 * Creates a new MongoDbDestination
	 */
	public MongoDbDestination() {
	}

	/**
	 * Sets the mongo DB template
	 * @param mongoTemplate the mongo DB template
	 */
	@Autowired(required=true)
	public void setMongoTemplate(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	/**
	 * Sets the raw mongo connection
	 * @param mongo the raw mongo connection
	 */
	@Autowired(required=true)
	public void setMongo(Mongo mongo) {
		this.mongo = mongo;
	}

}
