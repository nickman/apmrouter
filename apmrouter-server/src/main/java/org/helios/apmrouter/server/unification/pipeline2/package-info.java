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
/**
 * <p>Title: package-info</p>
 * <p>Description: A port unification framework.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.unification.pipeline.package-info</code></p>
 * <br>
 * <br>
 * <br>
 * <p>Replay:<ol>
 * 	<li><b>Init</b>:<ul>
 * 		<li>Iterate all <b><code>ProtocolInitiator</code></b>s</li>
 * 		<li><b>http</b>: Install http protocol stack, goto <b>complete</b></li>
 * 		<li>else goto <b>encodeId</b></li>
 * 		<li></li>
 *  </ul></li>
 *  <li><b>encodeId</b>:<ul>
 *  	<li>Iterate all <b><code>DecompressionInitiator</code></b>s  (Detect gzip or bzip2.)</li>
 *  	<li>If found, install decompression stack and goto <b>decode</b></li>
 *  	<li>Else goto <b>contentId</b></li>
 *  </ul></li>
 *  <li><b>decode</b>:<ul>
 *  	<li>If <b><code>DecompressionInitiator.requiresFull</code></b> is true, install aggregator.
 *  		(may require a close listener and completion block)</li>
 *  	<li>On complete goto <b>contentId</b></li>
 *  </ul></li>
 *  <li><b>contentId</b>:<ul>
 *  	<li>Iterate all <b><code>ContentClassifier</code></b>s</li>
 *  	<li>If found, install <b><code>ContentClassifier</code></b>'s handler stack and goto <b>content</b></li>
 *  	<li>Else error</li>
 *  </ul></li>
 *  <li><b>content</b>: Run <b><code>ContentClassifier</code></b>'s handler then goto <b>complete</b></li>
 *  <li><b>complete</b></li>
 *  <li><b>error</b></li>
 * </ol>
 * 
 */

package org.helios.apmrouter.server.unification.pipeline2;