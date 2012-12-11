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
package test.org.helios.apmrouter.asm;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.AdviceAdapter;

/**
 * <p>Title: ClockAdviceAdapter</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.org.helios.apmrouter.asm.ClockAdviceAdapter</code></p>
 */

public class ClockAdviceAdapter extends AdviceAdapter {
	String owner = "foo";
	/**
	 * Creates a new ClockAdviceAdapter
	 * @param api
	 * @param mv
	 * @param access
	 * @param name
	 * @param desc
	 */
	public ClockAdviceAdapter(int api, MethodVisitor mv, int access,
			String name, String desc) {
		super(api, mv, access, name, desc);
	}
	
	
	@Override
	protected void onMethodEnter() {
		mv.visitFieldInsn(GETSTATIC, owner, "timer", "J");
		mv.visitMethodInsn(INVOKESTATIC, "java/lang/System",
				"currentTimeMillis", "()J");
		mv.visitInsn(LSUB);
		mv.visitFieldInsn(PUTSTATIC, owner, "timer", "J");
	}

	@Override
	protected void onMethodExit(int opcode) {
		mv.visitFieldInsn(GETSTATIC, owner, "timer", "J");
		mv.visitMethodInsn(INVOKESTATIC, "java/lang/System",
				"currentTimeMillis", "()J");
		mv.visitInsn(LADD);
		mv.visitFieldInsn(PUTSTATIC, owner, "timer", "J");
	}

	@Override
	public void visitMaxs(int maxStack, int maxLocals) {
		super.visitMaxs(maxStack + 4, maxLocals);
	}	

}
