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
package org.helios.apmrouter.instrumentation;

import org.helios.apmrouter.trace.TXContext;

/**
 * <p>Title: TXDirective</p>
 * <p>Description: Enumerated {@link TXContext} directives for instrumented methods</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.instrumentation.TXDirective</code></p>
 */

public enum TXDirective {
	/** Activates the {@link TXContext} if not activated, rolls the {@link TXContext} if already activated */
	ROLL(new RollContextOp()),
	/** Clears the current thread's {@link TXContext} */
	CLEAR(new ClearContextOp()),
	/** {@link TXContext} No Op */
	NOOP(new NoopContextOp());
	
	
	private TXDirective(TXContextOp op) {
		this.op = op;
	}
	
	private final TXContextOp op;
	
	/**
	 * Invokes this option's {@link TXContext} operation 
	 */
	public void op() {
		op.op();
	}
	
	/**
	 * <p>Title: TXContextOp</p>
	 * <p>Description: Defines a {@link TXContext} operation</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.instrumentation.TXDirective.TXContextOp</code></p>
	 */
	protected static interface TXContextOp {
		/**
		 * Executes a {@link TXContext} operation
		 */
		public void op();
	}
	
	/**
	 * <p>Title: RollContextOp</p>
	 * <p>Description: {@link TXContext} roll operation</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.instrumentation.TXDirective.RollContextOp</code></p>
	 */
	protected static class RollContextOp implements TXContextOp {
		/**
		 * {@inheritDoc}
		 * @see org.helios.apmrouter.instrumentation.TXDirective.TXContextOp#op()
		 */
		@Override
		public void op() {
			TXContext.rollContext();
		}
	}
	
	/**
	 * <p>Title: ClearContextOp</p>
	 * <p>Description: {@link TXContext} clear operation</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.instrumentation.TXDirective.ClearContextOp</code></p>
	 */
	protected static class ClearContextOp implements TXContextOp {
		/**
		 * {@inheritDoc}
		 * @see org.helios.apmrouter.instrumentation.TXDirective.TXContextOp#op()
		 */
		@Override
		public void op() {
			TXContext.clearContext();
		}
	}
	
	/**
	 * <p>Title: NoopContextOp</p>
	 * <p>Description: {@link TXContext} Noop operation</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.instrumentation.TXDirective.NoopContextOp</code></p>
	 */
	protected static class NoopContextOp implements TXContextOp {
		/**
		 * {@inheritDoc}
		 * @see org.helios.apmrouter.instrumentation.TXDirective.TXContextOp#op()
		 */
		@Override
		public void op() {
		}
	}
	
	
}
