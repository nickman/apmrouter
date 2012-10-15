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
package org.helios.apmrouter.util.io.filewatcher;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * <p>Title: FileWatchSubscription</p>
 * <p>Description: Defines a path and a file pattern representing a file watch subscription</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.util.io.filewatcher.FileWatchSubscription</code></p>
 */

public class FileWatchSubscription {
	/** The watch directory */
	private final Path watchDir;
	/** The file pattern */
	private final Pattern filePattern;
	/** The subscriber interested in the defined events */
	private final FileEventSubscriber subscriber;
	/** The event counter for all events */
	private final AtomicLong allEventCounter = new AtomicLong(0L);
	/** The event counter for matched events */
	private final AtomicLong matchedEventCounter = new AtomicLong(0L);
	
	/**
	 * Creates a new FileWatchSubscription
	 * @param path The directory to watch
	 * @param pattern The file pattern that file events must match
	 * @param subscriber The subscriber interested in the defined events
	 */
	public FileWatchSubscription(Path path, Pattern pattern, FileEventSubscriber subscriber) {
		watchDir = path;
		filePattern = pattern;		
		this.subscriber = subscriber;
	}

	/**
	 * Returns the watch directory
	 * @return the watch directory
	 */
	public Path getWatchDir() {
		return watchDir;
	}

	/**
	 * Returns the file pattern
	 * @return the file pattern
	 */
	public Pattern getFilePattern() {
		return filePattern;
	}
	
	/**
	 * All events originating from the watched directory will be passed here, and the ones that match will be passed to the subscriber.
	 * @param event The file event
	 */
	public void onEvent(FileEvent event) {
		allEventCounter.incrementAndGet();
		if(watchDir.equals(event.getWatchDir())) {
			if(filePattern.matcher(event.getFileName()).matches()) {
				matchedEventCounter.incrementAndGet();
				subscriber.onFileEvent(new File(watchDir.toFile(), event.getFileName()).toPath(), event.getEventType());
			}
		}
	}
	
	/**
	 * Returns the count of all events for this watched directory 
	 * @return the count of all events for this watched directory
	 */
	public long getAllEventCounter() {
		return allEventCounter.get();
	}

	/**
	 * Returns the count of matched events for this watched directory
	 * @return the count of matched events for this watched directory
	 */
	public long getMatchedEventCounter() {
		return matchedEventCounter.get();
	}
	

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((filePattern == null) ? 0 : filePattern.pattern().hashCode());
		result = prime * result
				+ ((watchDir == null) ? 0 : watchDir.hashCode());
		return result;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		FileWatchSubscription other = (FileWatchSubscription) obj;
		if (filePattern == null) {
			if (other.filePattern != null) {
				return false;
			}
		} else if (!filePattern.pattern().equals(other.filePattern.pattern())) {
			return false;
		}
		if (watchDir == null) {
			if (other.watchDir != null) {
				return false;
			}
		} else if (!watchDir.equals(other.watchDir)) {
			return false;
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("FileWatchSubscription [watchDir=");
		builder.append(watchDir);
		builder.append(", filePattern=");
		builder.append(filePattern);
		builder.append(", allevents=");
		builder.append(allEventCounter.longValue());
		builder.append(", matchedevents=");
		builder.append(matchedEventCounter.longValue());
		builder.append("]");
		return builder.toString();
	}


}
