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
package org.helios.apmrouter.byteman.sockets;

/**
 * <p>Title: NetStatConst</p>
 * <p>Description: Netstat related constants</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.byteman.sockets.NetStatConst</code></p>
 */

public interface NetStatConst {
	
	/** The stat map constant for TcpInboundTotal readings */
	public static final String STAT_TCPINBOUNDTOTAL = "TcpInboundTotal";
	/** The stat map constant for TcpOutboundTotal readings */
	public static final String STAT_TCPOUTBOUNDTOTAL = "TcpOutboundTotal";
	/** The stat map constant for AllInboundTotal readings */
	public static final String STAT_ALLINBOUNDTOTAL = "AllInboundTotal";
	/** The stat map constant for AllOutboundTotal readings */
	public static final String STAT_ALLOUTBOUNDTOTAL = "AllOutboundTotal";
	/** The stat map constant for TcpStates readings */
	public static final String STAT_TCPSTATES = "TcpStates";
	/** The stat map constant for TcpEstablished readings */
	public static final String STAT_TCPESTABLISHED = "TcpEstablished";
	/** The stat map constant for TcpSynSent readings */
	public static final String STAT_TCPSYNSENT = "TcpSynSent";
	/** The stat map constant for TcpSynRecv readings */
	public static final String STAT_TCPSYNRECV = "TcpSynRecv";
	/** The stat map constant for TcpFinWait1 readings */
	public static final String STAT_TCPFINWAIT1 = "TcpFinWait1";
	/** The stat map constant for TcpFinWait2 readings */
	public static final String STAT_TCPFINWAIT2 = "TcpFinWait2";
	/** The stat map constant for TcpTimeWait readings */
	public static final String STAT_TCPTIMEWAIT = "TcpTimeWait";
	/** The stat map constant for TcpClose readings */
	public static final String STAT_TCPCLOSE = "TcpClose";
	/** The stat map constant for TcpCloseWait readings */
	public static final String STAT_TCPCLOSEWAIT = "TcpCloseWait";
	/** The stat map constant for TcpLastAck readings */
	public static final String STAT_TCPLASTACK = "TcpLastAck";
	/** The stat map constant for TcpListen readings */
	public static final String STAT_TCPLISTEN = "TcpListen";
	/** The stat map constant for TcpClosing readings */
	public static final String STAT_TCPCLOSING = "TcpClosing";
	/** The stat map constant for TcpIdle readings */
	public static final String STAT_TCPIDLE = "TcpIdle";
	/** The stat map constant for TcpBound readings */
	public static final String STAT_TCPBOUND = "TcpBound";
	/** An array of all the netstat metric names */
	public static final String[] STAT_ALL = {STAT_TCPINBOUNDTOTAL, STAT_TCPOUTBOUNDTOTAL, STAT_ALLINBOUNDTOTAL, STAT_ALLOUTBOUNDTOTAL, STAT_TCPSTATES, STAT_TCPESTABLISHED, STAT_TCPSYNSENT, STAT_TCPSYNRECV, STAT_TCPFINWAIT1, STAT_TCPFINWAIT2, STAT_TCPTIMEWAIT, STAT_TCPCLOSE, STAT_TCPCLOSEWAIT, STAT_TCPLASTACK, STAT_TCPLISTEN, STAT_TCPCLOSING, STAT_TCPIDLE, STAT_TCPBOUND };

}
