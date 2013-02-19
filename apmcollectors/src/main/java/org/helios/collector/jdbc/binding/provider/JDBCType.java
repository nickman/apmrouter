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

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Title: JDBCType</p>
 * <p>Description:Enum wrapper for <code>java.sql.Types</code>.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public enum JDBCType {
	BIT(-7, true, false),
	TINYINT(-6, true, false),
	SMALLINT(5, true, false),
	INTEGER(4, true, false),
	BIGINT(-5, true, false),
	FLOAT(6, true, false),
	REAL(7, true, false),
	DOUBLE(8, true, false),
	NUMERIC(2, true, false),
	DECIMAL(3, true, false),
	CHAR(1, true, true),
	VARCHAR(12, true, true),
	LONGVARCHAR(-1, true, true),
	DATE(91, true, true),
	TIME(92, true, true),
	TIMESTAMP(93, true, true),
	BINARY(-2, false, false),
	VARBINARY(-3, false, false),
	LONGVARBINARY(-4, false, false),
	NULL(0, true, false),
	OTHER(1111, false, false),
	JAVA_OBJECT(2000, false, false),
	DISTINCT(2001, false, false),
	STRUCT(2002, false, false),
	ARRAY(2003, false, false),
	BLOB(2004, false, false),
	CLOB(2005, false, false),
	REF(2006, false, false),
	DATALINK(70, false, false),
	BOOLEAN(16, true, false),
	ROWID(-8, false, false),
	NCHAR(-15, true, true),
	NVARCHAR(-9, true, true),
	LONGNVARCHAR(-16, true, true),
	NCLOB(2011, false, false),
	SQLXML(2009, false, false);

	static final Map<Integer, JDBCType> CODE_TO_NAME = new HashMap<Integer, JDBCType>(36);
	
	public static JDBCType fromCode(int i) {
		JDBCType type = CODE_TO_NAME.get(i);
		if(type==null) throw new RuntimeException("No JDBCType for code [" + i + "]");
		return type;
	}
	
	static {
		CODE_TO_NAME.put(-7,JDBCType.BIT);
		CODE_TO_NAME.put(-6,JDBCType.TINYINT);
		CODE_TO_NAME.put(5,JDBCType.SMALLINT);
		CODE_TO_NAME.put(4,JDBCType.INTEGER);
		CODE_TO_NAME.put(-5,JDBCType.BIGINT);
		CODE_TO_NAME.put(6,JDBCType.FLOAT);
		CODE_TO_NAME.put(7,JDBCType.REAL);
		CODE_TO_NAME.put(8,JDBCType.DOUBLE);
		CODE_TO_NAME.put(2,JDBCType.NUMERIC);
		CODE_TO_NAME.put(3,JDBCType.DECIMAL);
		CODE_TO_NAME.put(1,JDBCType.CHAR);
		CODE_TO_NAME.put(12,JDBCType.VARCHAR);
		CODE_TO_NAME.put(-1,JDBCType.LONGVARCHAR);
		CODE_TO_NAME.put(91,JDBCType.DATE);
		CODE_TO_NAME.put(92,JDBCType.TIME);
		CODE_TO_NAME.put(93,JDBCType.TIMESTAMP);
		CODE_TO_NAME.put(-2,JDBCType.BINARY);
		CODE_TO_NAME.put(-3,JDBCType.VARBINARY);
		CODE_TO_NAME.put(-4,JDBCType.LONGVARBINARY);
		CODE_TO_NAME.put(0,JDBCType.NULL);
		CODE_TO_NAME.put(1111,JDBCType.OTHER);
		CODE_TO_NAME.put(2000,JDBCType.JAVA_OBJECT);
		CODE_TO_NAME.put(2001,JDBCType.DISTINCT);
		CODE_TO_NAME.put(2002,JDBCType.STRUCT);
		CODE_TO_NAME.put(2003,JDBCType.ARRAY);
		CODE_TO_NAME.put(2004,JDBCType.BLOB);
		CODE_TO_NAME.put(2005,JDBCType.CLOB);
		CODE_TO_NAME.put(2006,JDBCType.REF);
		CODE_TO_NAME.put(70,JDBCType.DATALINK);
		CODE_TO_NAME.put(16,JDBCType.BOOLEAN);
		CODE_TO_NAME.put(-8,JDBCType.ROWID);
		CODE_TO_NAME.put(-15,JDBCType.NCHAR);
		CODE_TO_NAME.put(-9,JDBCType.NVARCHAR);
		CODE_TO_NAME.put(-16,JDBCType.LONGNVARCHAR);
		CODE_TO_NAME.put(2011,JDBCType.NCLOB);
		CODE_TO_NAME.put(2009,JDBCType.SQLXML);

	}
	
	private JDBCType(int code, boolean literal, boolean quoted ) {
		this.code = code;
		this.quoted = quoted;
		this.literal = literal;
	}
	private int code = 0;
	private boolean quoted = false;
	private boolean literal = false;
	
	public int code() {
		return code;
	}
	public boolean quoted() {
		return quoted;
	}
	/**
	 * @return the literal
	 */
	public boolean literal() {
		return literal;
	}
	
}
