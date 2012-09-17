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
package org.helios.apmrouter.trace;

import static org.helios.apmrouter.util.Methods.nvl;

import java.util.ArrayList;
import java.util.List;

import org.snmp4j.PDU;
import org.snmp4j.smi.Counter32;
import org.snmp4j.smi.Counter64;
import org.snmp4j.smi.Gauge32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;

/**
 * <p>Title: PDUBuilder</p>
 * <p>Description: A fluent style interface for building SNMP PDUs</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.trace.PDUBuilder</code></p>
 */
public class PDUBuilder {
	/** The PDU type */
	private final int pduType;
	/** The created variables */
	private final List<VariableBinding> bindings = new ArrayList<VariableBinding>();
	/** The OID prefix for all OID variables in the PDU to be created */
	private final String oidPrefix;
	
	/**
	 * Creates a new PDU builder
	 * @param pduType The PDU type code
	 * @return A PDUBuilder
	 */
	public static PDUBuilder builder(int pduType) {
		return new PDUBuilder(pduType);
	}
	
	/**
	 * Creates a new PDU builder
	 * @param pduType The PDU type code
	 * @param oidPrefix The OID prefix for all OID variables in the PDU to be created
	 * @return A PDUBuilder
	 */
	public static PDUBuilder builder(int pduType, CharSequence oidPrefix) {
		return new PDUBuilder(pduType, oidPrefix);
	}
	
	
	/**
	 * Creates a new PDUBuilder
	 * @param pduType the PDU type
	 */
	private PDUBuilder(int pduType) {
		this(pduType, "");
	}
	
	/**
	 * Creates a new PDUBuilder
	 * @param pduType the PDU type
	 * @param oidPrefix The OID prefix for all OID variables in the PDU to be created
	 */
	private PDUBuilder(int pduType, CharSequence oidPrefix) {
		this.pduType = pduType;
		this.oidPrefix = oidPrefix.toString();
	}	
	
	/**
	 * Builds and returns the PDU
	 * @return the built PDU
	 */
	public PDU build() {
		PDU pdu = new PDU();
		pdu.setType(pduType);
		pdu.addAll(bindings.toArray(new VariableBinding[0]));
		return pdu;
	}
	
	/**
	 * Adds a new {@link OctetString} based variable binding to the PDU.
	 * @param oid The OID of the variable binding
	 * @param value The string value of the variable binding
	 * @return this builder
	 */
	public PDUBuilder string(CharSequence oid, CharSequence value) {
		nvl(oid, "OID");
		nvl(value, "Variable Value");
		bindings.add(new VariableBinding(new OID(oidPrefix + oid.toString()), new OctetString(value.toString())));
		return this;
	}
	
	/**
	 * Adds a new {@link Counter32} based variable binding to the PDU.
	 * @param oid The OID of the variable binding
	 * @param value The 32 bit unsigned integer value of the counter32 variable binding
	 * @return this builder
	 */
	public PDUBuilder counter32(CharSequence oid, long value) {
		nvl(oid, "OID");
		nvl(value, "Variable Value");
		bindings.add(new VariableBinding(new OID(oidPrefix + oid.toString()), new Counter32(value)));
		return this;
	}
	
	/**
	 * Adds a new {@link Counter64} based variable binding to the PDU.
	 * @param oid The OID of the variable binding
	 * @param value The 64 bit unsigned integer value of the counter64 variable binding
	 * @return this builder
	 */
	public PDUBuilder counter64(CharSequence oid, long value) {
		nvl(oid, "OID");
		bindings.add(new VariableBinding(new OID(oidPrefix + oid.toString()), new Counter64(value)));
		return this;
	}
	
	/**
	 * Adds a new {@link Gauge32} based variable binding to the PDU.
	 * @param oid The OID of the variable binding
	 * @param value The 32 bit unsigned integer value of the gauge32 variable binding
	 * @return this builder
	 */
	public PDUBuilder gauge32(CharSequence oid, long value) {
		nvl(oid, "OID");
		nvl(value, "Variable Value");
		bindings.add(new VariableBinding(new OID(oidPrefix + oid.toString()), new Gauge32(value)));
		return this;
	}
	
	
	
}
