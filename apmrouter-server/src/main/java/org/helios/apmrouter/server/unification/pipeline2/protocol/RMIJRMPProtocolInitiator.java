/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2013, Helios Development Group and individual contributors
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
package org.helios.apmrouter.server.unification.pipeline2.protocol;

import java.rmi.registry.Registry;

import org.helios.apmrouter.server.unification.pipeline2.AbstractInitiator;
import org.helios.apmrouter.server.unification.pipeline2.ProtocolSwitchContext;
import org.helios.apmrouter.server.unification.pipeline2.ProtocolSwitchDecoder;
import org.helios.apmrouter.server.unification.pipeline2.SwitchPhase;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.serialization.CompatibleObjectEncoder;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>Title: RMIJRMPProtocolInitiator</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.unification.pipeline2.protocol.RMIJRMPProtocolInitiator</code></p>
 * <br><br>
 * <h4>Network Header Example</h4><pre>
         +-------------------------------------------------+
         |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |
+--------+-------------------------------------------------+----------------+
|00000000| 16 03 01 00 95 01 00 00 91 03 01 51 c2 0e 2c 7d |...........Q..,}|
|00000010| 8d 1c 97 e6 0b 20 64 fe 06 49 64 db af cb 00 9b |..... d..Id.....|
|00000020| 2d cc 55 69 e4 7c e5 e8 56 08 63 00 00 2a c0 09 |-.Ui.|..V.c..*..|
|00000030| c0 13 00 2f c0 04 c0 0e 00 33 00 32 c0 07 c0 11 |.../.....3.2....|
|00000040| 00 05 c0 02 c0 0c c0 08 c0 12 00 0a c0 03 c0 0d |................|
|00000050| 00 16 00 13 00 04 00 ff 01 00 00 3e 00 0a 00 34 |...........>...4|
|00000060| 00 32 00 17 00 01 00 03 00 13 00 15 00 06 00 07 |.2..............|
|00000070| 00 09 00 0a 00 18 00 0b 00 0c 00 19 00 0d 00 0e |................|
|00000080| 00 0f 00 10 00 11 00 02 00 12 00 04 00 05 00 14 |................|
|00000090| 00 08 00 16 00 0b 00 02 01 00                   |..........      |
+--------+-------------------------------------------------+----------------+

The seemingly repeating part:  

         +-------------------------------------------------+
         |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |
+--------+-------------------------------------------------+----------------+
|00000000| 16 03 01 00 95 01 00 00 91 03 01 51             |...........Q..D&|
+--------+-------------------------------------------------+----------------+
<br>
 * </pre>
 * 
 */

public class RMIJRMPProtocolInitiator extends AbstractInitiator implements ProtocolInitiator, InitializingBean {
	/** The RMI registry */
	@Autowired(required=true)
	protected final Registry registry;
	/** JRMP stub */
	protected Object stub = null;
	/**
	 * Creates a new RMIJRMPProtocolInitiator
	 * @param registry The RMI registry
	 */
	public RMIJRMPProtocolInitiator(Registry registry) {
		super(BYTE_SIG.length, "jmx-rmijrmp");
		this.registry = registry;
	}
	
	private static final int[] BYTE_SIG = new int[]{22, 3, 1, 0, 149, 1, 0, 0, 145, 3, 1, 81};

	/**
	 * {@inheritDoc}
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		stub = registry.lookup("jmxrmi");		
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.unification.pipeline2.Initiator#match(org.jboss.netty.buffer.ChannelBuffer)
	 */
	@Override
	public boolean match(ChannelBuffer buff) {
		if(buff.readableBytes()>=BYTE_SIG.length) {
			for(int i = 0; i < BYTE_SIG.length; i++) {
				if(BYTE_SIG[i] != buff.getUnsignedByte(i)) return false;
			}
			return true;
		}
		return false;
	}
	


	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.unification.pipeline2.Initiator#process(org.helios.apmrouter.server.unification.pipeline2.ProtocolSwitchContext)
	 */
	@Override
	public SwitchPhase process(ProtocolSwitchContext ctx) {
		ChannelPipeline pipeline = ctx.getPipeline();
		if(pipeline.getContext("objectEncoder")==null) {
			pipeline.addLast("objectEncoder", new CompatibleObjectEncoder());
		}
		pipeline.remove(ProtocolSwitchDecoder.PIPE_NAME);
		ctx.getChannel().write(stub);
		return SwitchPhase.COMPLETE;
	}


}
