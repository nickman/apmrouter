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
package org.helios.apmrouter.metric;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import org.helios.apmrouter.destination.MetricTextFormatter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * <p>Title: JSONFormatterImpl</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.metric.JSONFormatterImpl</code></p>
 */

public class JSONFormatterImpl implements JSONFormatter, MetricTextFormatter {
	/** If true, the metric identifier will use only the metric id. Otherwise uses the FQN */
	protected final boolean compressedIdentifier;
	/** If true, the requests for byte array json content will be gzipped */
	protected final boolean gzip;
	

	
	/**
	 * Creates a new JSONFormatterImpl
	 * @param compressedIdentifier If true, the metric identifier will use only the metric id. Otherwise uses the FQN
	 * @param gzip If true, the requests for byte array json content will be gzipped
	 */
	public JSONFormatterImpl(boolean compressedIdentifier, boolean gzip) {
		this.compressedIdentifier = compressedIdentifier;
		this.gzip = gzip;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.JSONFormatter#toJSONObject(org.helios.apmrouter.metric.IMetric[])
	 */
	@Override
	public JSONObject toJSONObject(IMetric... metrics) throws JSONException {
		return toJSONObject(new int[]{0}, metrics);
	}


	/**
	 * Formats the passed metrics into a JSONObject
	 * @param counter A counter reference
	 * @param metrics The metrics to format
	 * @return a JSONObject representing the formatted metrics
	 * @throws JSONException thrown on JSON creation errors
	 */
	protected JSONObject toJSONObject(int[] counter, IMetric... metrics) throws JSONException {
		if(metrics==null || metrics.length<1) return new JSONObject();
		JSONObject root = new JSONObject();
//		Map<Object, JSONObject> map = new HashMap<Object, JSONObject>(metrics.length);
		root.put("type", "metrics");
		JSONArray data = new JSONArray();
		root.put("data", data);
		
		for(IMetric metric: metrics) {
			//root.put(arg0, arg1)
			if(metric==null || metric.getType()==MetricType.ERROR || metric.getType()==MetricType.BLOB || metric.getType()==MetricType.PDU || (compressedIdentifier && metric.getToken()==-1)) continue;
			counter[0]++;
			JSONObject jsonMetric = new JSONObject();
			if(compressedIdentifier) {
				jsonMetric.put("id", metric.getToken());
			} else {
				jsonMetric.put("id", metric.getFQN());
			}
			if(metric.getType().isLong()) {
				jsonMetric.put("value", metric.getLongValue());
			} else {
				jsonMetric.put("event", metric.getValue());
			}	
			data.put(jsonMetric);
		}
		return root;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.JSONFormatter#toJSON(org.helios.apmrouter.metric.IMetric[])
	 */
	@Override
	public String toJSON(IMetric... metrics) throws JSONException {		
		return toJSONObject(metrics).toString();
	}
	

	/**
	 * Formats the passed metrics into json bytes
	 * @param counter A counter reference
	 * @param metrics The metrics to format
	 * @return a byte array representing the JSON formatted metrics
	 * @throws JSONException thrown on JSON creation errors
	 */
	public byte[] toJSONBytes(int[] counter, IMetric... metrics) throws JSONException {
		if(!gzip) return toJSONObject(counter, metrics).toString().getBytes();
		byte[] bytes = toJSONObject(counter, metrics).toString().getBytes();
		ByteArrayOutputStream baos = new ByteArrayOutputStream(bytes.length);
		try {
			GZIPOutputStream gzout = new GZIPOutputStream(baos);
			gzout.write(bytes);
			gzout.close();
			baos.flush();
			byte[] gzipped = baos.toByteArray();
			baos.close();
			return gzipped;
		} catch (IOException e) {
			throw new RuntimeException("Failed to gzip json content", e);
		}		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.JSONFormatter#toJSONBytes(org.helios.apmrouter.metric.IMetric[])
	 */
	@Override
	public byte[] toJSONBytes(IMetric... metrics) throws JSONException {
		return toJSONBytes(new int[]{0}, metrics);
	}

	/**
	 * Returns true if the metric identifier will use only the metric id. Otherwise uses the FQN
	 * @return the compressedIdentifier
	 */
	public boolean isCompressedIdentifier() {
		return compressedIdentifier;
	}

	/**
	 * Returns true if byte array json content will be gzipped
	 * @return the gzip
	 */
	public boolean isGzip() {
		return gzip;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.MetricTextFormatter#format(java.io.OutputStream, org.helios.apmrouter.metric.IMetric[])
	 */
	@Override
	public int format(OutputStream os, IMetric... metrics) throws IOException {
		int[] counter = new int[]{0};
		try {
			os.write(toJSONBytes(counter, metrics));
			return counter[0];
		} catch (JSONException e) {
			throw new IOException("Failed to json format", e);
		}		
	}

}
