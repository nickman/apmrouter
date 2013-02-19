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
package org.helios.collector.jdbc.binding.provider;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.helios.collector.jdbc.binding.binder.Binder;
import org.helios.collector.jdbc.binding.binder.IBinder;

/**
 * <p>Title: SysTimeProvider</p>
 * <p>Description: A bind variable provider to supply the current and relative time stamps </p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
@BindVariableProvider(defaultBinder=SysTimeProvider.class, tokenKey="SysTime")
@Binder(name="SysTime")
public class SysTimeProvider extends AbstractBindVariableProvider implements IBinder {	
	public static final String DEFAULT_FORMAT = "MM/dd/yyyy hh:mm:ss a";
	protected String dateFormat = DEFAULT_FORMAT;
	protected Set<TimeAdjust> adjusts = new HashSet<TimeAdjust>();
	protected long offset = 0;
	protected ThreadLocal<SimpleDateFormat> formatter = new ThreadLocal<SimpleDateFormat>() {
        protected synchronized SimpleDateFormat initialValue() {
            return new SimpleDateFormat(dateFormat);
        }
    };
	

	/**
	 * Configures this instace of the provider.
	 * @param config
	 * @see org.helios.collectors.jdbc.binding.provider.AbstractBindVariableProvider#configure(java.lang.String)
	 */
	@Override
	public void configureProvider(String config) throws BindProviderConfigurationException {
		Matcher m = ProviderToken.BIND_VAR_PATTERN.matcher(config);
		while(m.find()) {			
			TimeAdjust ta = new TimeAdjust(m.group(1), Integer.parseInt(m.group(2)), m.group(3));
			offset = offset + ta.getOffSet();
			String format = m.group(4);
			if(format!=null) {
				dateFormat = format;
			    formatter = new ThreadLocal<SimpleDateFormat>() {
			        protected synchronized SimpleDateFormat initialValue() {
			            return new SimpleDateFormat(dateFormat); 
			        }
			    };
			}
		}		
	}
	

	/**
	 * Calculates the current time stamp plus or minus all the adjusted times.
	 * @return the adjusted time.
	 */
	@Override
	public Object getValue() {
		return System.currentTimeMillis() + offset;
	}
	
	/**
	 * Does not support setValue.
	 */
	@Override
	public void setValue(Object o) {}
	
	

	/**
	 * Executes a bind against a prepared statement.
	 * @param ps The prepared statement to invoke the bind against.
	 * @param bindSequence The bind sequence.
	 * @return The final bind sequence executed by this bind call. Normally, this would be the same as <code>bindSequence</code>.
	 * @throws SQLException
	 */
	public int bind(PreparedStatement ps, int bindSequence, Object value) 	throws SQLException {
		ps.setTimestamp(bindSequence, new Timestamp((Long)value));
		return bindSequence;
	}

	/**
	 * Executes a token substitution against a sql statement for the passed  bind token, replacing the token with the provider supplied literal.
	 * Intended to support binding-like operations when using drivers that do not support <code>PreparedStatements</code> and/or bind variables.
	 * @param sql The tokenized sql statement
	 * @param bindToken The bind token to replace with a literal binding.
	 * @return The substituted sql statement.
	 * @throws SQLException
	 */
	public CharSequence bind(CharSequence sql, String bindToken, Object value) throws SQLException {
		try {
			Date date = new Date((Long)value);
			String dateStr = "'" + formatter.get().format(date) + "'";
			if(log.isDebugEnabled()) log.debug("Replacing [" + bindToken + "] with [" + dateStr + "]");
			return sql.toString().replace(bindToken, dateStr);
		} catch (Exception e) {
			throw new SQLException("" + e);
		}
	}


	/**
	 * @param config
	 * @throws BindProviderConfigurationException
	 * @see org.helios.collectors.jdbc.binding.binder.IBinder#configureBinder(java.lang.String)
	 */
	public void configureBinder(String config) throws BindProviderConfigurationException {
	}

}

/**
 * <p>Title: TimeAdjust</p>
 * <p>Description: Value class to capture a set of time modifiers.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
class TimeAdjust {
	/**  */
	protected String sign = null;
	/**  */
	protected int size = 0;
	/**  */
	protected String unit = null;
	/**
	 * Creates a new TimeAdjust
	 * @param sign The sign (+ or -)
	 * @param size The size of the adjust
	 * @param unit The unit of the adjust
	 */
	public TimeAdjust(String sign, int size, String unit) {
		this.sign = sign;
		this.size = size;
		this.unit = unit;
	}
	
	public long getOffSet() {
		long offset = 0;
		if(unit.equalsIgnoreCase("ms")) {
			offset = size;
		} else if(unit.equalsIgnoreCase("s")) {
			offset = TimeUnit.MILLISECONDS.convert(size, TimeUnit.SECONDS);
		} else if(unit.equalsIgnoreCase("m")) {
			offset = TimeUnit.MILLISECONDS.convert(size, TimeUnit.SECONDS)*60;
		}  else if(unit.equalsIgnoreCase("h")) {
			offset = TimeUnit.MILLISECONDS.convert(size, TimeUnit.SECONDS)*60*60;
		}  else if(unit.equalsIgnoreCase("d")) {
			offset = TimeUnit.MILLISECONDS.convert(size, TimeUnit.SECONDS)*60*60*24;
		} else {
			throw new RuntimeException("The unit [" + unit +"] is not recognized");
		}
		if(sign.equals("-")) {
			return offset*-1;
		} else if(sign.equals("+")) {
			return offset;
		} else {
			throw new RuntimeException("The sign [" + sign +"] is not recognized");
		}

	}

	/**
	 * @return
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((sign == null) ? 0 : sign.hashCode());
		result = prime * result + size;
		result = prime * result + ((unit == null) ? 0 : unit.hashCode());
		return result;
	}

	/**
	 * @param obj
	 * @return
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TimeAdjust other = (TimeAdjust) obj;
		if (sign == null) {
			if (other.sign != null)
				return false;
		} else if (!sign.equals(other.sign))
			return false;
		if (size != other.size)
			return false;
		if (unit == null) {
			if (other.unit != null)
				return false;
		} else if (!unit.equals(other.unit))
			return false;
		return true;
	}
}

