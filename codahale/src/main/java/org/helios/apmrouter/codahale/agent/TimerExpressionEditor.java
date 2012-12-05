/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2012, Helios Development Group and individual contributors
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
package org.helios.apmrouter.codahale.agent;

import com.yammer.metrics.annotation.Timed;

import javassist.CannotCompileException;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

/**
 * <p>Title: TimerExpressionEditor</p>
 * <p>Description: Expression editor to inject a call to the named timer</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.codahale.agent.TimerExpressionEditor</code></p>
 */

public class TimerExpressionEditor extends ExprEditor {
	/** The field name of the timer to call */
	protected final String timerFieldName;
	
	/**
	 * Creates a new TimerExpressionEditor
	 * @param timer The field name of the timer to call
	 */
	public TimerExpressionEditor(String timer) {
		timerFieldName = timer;
	}
	
	/**
	 * {@inheritDoc}
	 * @see javassist.expr.ExprEditor#edit(javassist.expr.MethodCall)
	 */
	@Override
	public void edit(MethodCall m) throws CannotCompileException {
		try {			
			CtMethod method = m.getMethod();
			ClassPool cp = method.getDeclaringClass().getClassPool();
			cp.appendClassPath(new ClassClassPath(Timed.class));
			String fieldName = method.getDeclaringClass().makeUniqueName("timerContext"); 
			method.addLocalVariable(fieldName, cp.get("com.yammer.metrics.core.TimerContext"));
			method.insertBefore(fieldName + "=" + timerFieldName + ".time();");
			method.insertAfter("{" + fieldName + ".stop();}", false);
		} catch (NotFoundException e) {
			throw new CannotCompileException("Failed to get method for method call", e);
		}
	}
}
