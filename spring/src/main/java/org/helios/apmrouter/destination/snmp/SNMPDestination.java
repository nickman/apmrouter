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
package org.helios.apmrouter.destination.snmp;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.helios.apmrouter.destination.BaseDestination;
import org.helios.apmrouter.metric.IMetric;
import org.snmp4j.MessageException;
import org.snmp4j.PDU;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.support.MetricType;

/**
 * <p>Title: SNMPDestination</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.snmp.SNMPDestination</code></p>
 */
public class SNMPDestination extends BaseDestination {
	/** The targets for this destination to forward to  */
	protected final Set<SNMPManager> targets = new CopyOnWriteArraySet<SNMPManager>();
	
	/**
	 * Creates a new SNMPDestination
	 * @param patterns The {@link IMetric} pattern this destination accepts
	 */
	public SNMPDestination(String... patterns) {
		super(patterns);
	}

	/**
	 * Creates a new SNMPDestination
	 * @param patterns The {@link IMetric} pattern this destination accepts
	 */
	public SNMPDestination(Collection<String> patterns) {
		super(patterns);
	}

	/**
	 * Creates a new SNMPDestination
	 */
	public SNMPDestination() {

	}
	
	/**
	 * Sets the community targets for this SNMP destination
	 * @param targets a collection fo targets
	 */
	@Autowired(required=true)
	public void setTargets(Collection<SNMPManager> targets) {
		for(SNMPManager ctf: targets) {
			this.targets.add(ctf);
			info("SNMP Endpoint:", ctf);
		}
	}
	
	@Override
	public void acceptRoute(IMetric routable) {		
		super.acceptRoute(routable);
		if(!routable.getType().name().equals("PDU")) return;
		PDU pdu = (PDU)routable.getValue();
		for(SNMPManager ctf: targets) {
			try {
				try {
					ctf.send(pdu);
				} catch (MessageException e) {
					if("Port already listening".equalsIgnoreCase(e.getMessage())) {
						ctf.send(pdu);						
					}
				}
				//snmp.sendPDU();
				//snmp.notify(pdu, ctf.getTarget());
				incr("PDUSendsCompleted");
			} catch (Exception e) {
				e.printStackTrace(System.err);
				incr("PDUSendsFailed");
				error("Failed to send PDU to " + ctf, e);
			}
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponent#getSupportedMetricNames()
	 */
	@Override
	public Set<String> getSupportedMetricNames() {
		Set<String> _metrics = new HashSet<String>(super.getSupportedMetricNames());
		_metrics.add("PDUSendsCompleted");
		_metrics.add("PDUSendsFailed");
		return _metrics;
	}
	
	/**
	 * Returns the number of PDUs successfully sent to their endpoint
	 * @return the number of PDUs successfully sent to their endpoint
	 */
	@ManagedMetric(category="SNMP", metricType=MetricType.COUNTER, description="the number of PDUs successfully sent to their endpoint")
	public long getPDUSendsCompleted() {
		return getMetricValue("PDUSendsCompleted");
	}
	
	/**
	 * Returns the number of PDU sends that failed
	 * @return the number of PDU sends that failed
	 */
	@ManagedMetric(category="SNMP", metricType=MetricType.COUNTER, description="the number of PDU sends that failed")
	public long getPDUSendsFailed() {
		return getMetricValue("PDUSendsFailed");
	}
	
		
	


}
