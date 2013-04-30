package org.helios.collector.jdbc.editors;

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

import java.beans.PropertyEditorSupport;

import org.helios.apmrouter.jmx.XMLHelper;
import org.w3c.dom.Node;

/**
 * <p>Title: XMLNodeEditor</p>
 * <p>Description: A property editors for XML Nodes.</p>
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class XMLNodeEditor extends PropertyEditorSupport {
    /**
     * Converts the passed String to an XML node and applies.
     * @param text The string to convert and apply.
     * @see java.beans.PropertyEditorSupport#setAsText(java.lang.String)
     */
    public void setAsText(final String text) {
        try {
            Node node = XMLHelper.parseXML(text);
            setValue(node);
        } catch (Exception e) {
            throw new IllegalArgumentException("The value [" + text + "] is not a valid XML Node", e);
        }
    }

}
