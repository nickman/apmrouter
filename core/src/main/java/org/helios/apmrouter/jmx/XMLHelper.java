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
package org.helios.apmrouter.jmx;

import static javax.xml.xpath.XPathConstants.NODE;
import static javax.xml.xpath.XPathConstants.NODESET;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;


/**
 * <p>Title: XMLHelper</p>
 * <p>Description: XML parsing helper</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.jmx.XMLHelper</code></p>
 */
public class XMLHelper {
	
	static {
		String xmlParser = System.getProperty("org.xml.sax.parser");
		if(xmlParser == null) {
			System.setProperty("org.xml.sax.parser", "com.sun.org.apache.xerces.internal.parsers.SAXParser");
		}		
	}
	


	  /**
	   * Searches throgh the passed NamedNodeMap for an attribute and returns it if it is found.
	   * If it is not found, returns a null.
	   * @param nnm NamedNodeMap
	   * @param name String
	   * @return String
	   */
	  public static String getAttributeValueByName(NamedNodeMap nnm, String name) {
	    for(int i = 0; i < nnm.getLength(); i++) {
	      Attr attr = (Attr)nnm.item(i);
	      if(attr.getName().equalsIgnoreCase(name)) {
	        return attr.getValue();
	      }
	    }
	    return null;
	  }
	  
	/**
	 * Returns the attribute value for the passed name in the passed node.
	 * @param node the node to get the attribute from
	 * @param name the name of the attribute
	 * @param defaultValue the value to return if the node did not contain the attribute
	 * @return The attribute value or the default value if it is not found.
	 */
	public static String getAttributeByName(Node node, String name, String defaultValue) {
		try {
			String val = getAttributeValueByName(node, name);
			if(val!=null && !val.trim().isEmpty()) return val.trim();
		} catch (Exception e) {}
		return defaultValue;		
	}
	
	/**
	 * Returns the long attribute value for the passed name in the passed node.
	 * @param node the node to get the attribute from
	 * @param name the name of the attribute
	 * @param defaultValue the value to return if the node did not contain the attribute
	 * @return The attribute value or the default value if it is not found.
	 */
	public static long getLongAttributeByName(Node node, String name, long defaultValue) {
		String s = getAttributeByName(node, name, null);
		if(s==null) return defaultValue;
		try {
			return Long.parseLong(s.trim());
		} catch (Exception e) {
			return defaultValue;
		}
	}
	  
	  
	/**
	 * Returns the attribute value for the passed name in the passed node.
	 * @param node the node to get the attribute from
	 * @param name the name of the attribute
	 * @return The attribute value or null if it is not found.
	 */
	public static String getAttributeValueByName(Node node, String name) {
		  return getAttributeValueByName(node.getAttributes(), name);
	  }

	  /**
	   * Searches throgh the passed NamedNodeMap for an attribute. If it is found, it will try to convert it to a boolean.
	   * @param nnm NamedNodeMap
	   * @param name String
	   * @throws RuntimeException on any failure to parse a boolean
	   * @return boolean
	   */
	  public static boolean getAttributeBooleanByName(NamedNodeMap nnm, String name) throws RuntimeException {
	    for(int i = 0; i < nnm.getLength(); i++) {
	      Attr attr = (Attr)nnm.item(i);
	      if(attr.getName().equalsIgnoreCase(name)) {
	        String tmp =  attr.getValue().toLowerCase();
	        if(tmp.equalsIgnoreCase("true")) return true;
	        if(tmp.equalsIgnoreCase("false")) return false;
	        throw new RuntimeException("Attribute " + name + " value not boolean:" + tmp);
	      }
	    }
	    throw new RuntimeException("Attribute " + name + " not found.");
	  }
	  
    /**
     * Returns the value of a named node attribute in the form of a boolean
	 * @param node The node to retrieve the attribute from
	 * @param name The name of the attribute
	 * @param defaultValue The default value if the attribute cannot be located or converted.
	 * @return true or false
	 */
	public static boolean getAttributeByName(Node node, String name, boolean defaultValue) {
		  if(node==null || name==null) return defaultValue;
		  try {
			  return getAttributeBooleanByName(node.getAttributes(), name);
		  } catch (Exception e) {
			  return defaultValue;
		  }
	  }
	
    /**
     * Returns the value of a named node attribute in the form of an int
	 * @param node The node to retrieve the attribute from
	 * @param name The name of the attribute
	 * @param defaultValue The default value if the attribute cannot be located or converted.
	 * @return an int
	 */
	public static int getAttributeByName(Node node, String name, int defaultValue) {
		  if(node==null || name==null) return defaultValue;
		  try {
			  return new Double(getAttributeValueByName(node, name).trim()).intValue();
		  } catch (Exception e) {
			  return defaultValue;
		  }
	  }
	
    /**
     * Returns the value of a named node attribute in the form of a long
	 * @param node The node to retrieve the attribute from
	 * @param name The name of the attribute
	 * @param defaultValue The default value if the attribute cannot be located or converted.
	 * @return a long
	 */
	public static long getAttributeByName(Node node, String name, long defaultValue) {
		  if(node==null || name==null) return defaultValue;
		  try {
			  return new Double(getAttributeValueByName(node, name).trim()).longValue();
		  } catch (Exception e) {
			  return defaultValue;
		  }
	  }


	  /**
	   * Helper Method. Searches through the child nodes of a node and returns the first node with a matching name.
	   * Do we need this ?
	   * @param element Element
	   * @param name String
	   * @param caseSensitive boolean
	   * @return Node
	   */

	  public static Node getChildNodeByName(Node element, String name, boolean caseSensitive) {
	    NodeList list = element.getChildNodes();
	    for(int i = 0; i < list.getLength(); i++) {
	      Node node = list.item(i);
	      if(caseSensitive) {
	        if(node.getNodeName().equals(name)) return node;
	      } else {
	        if(node.getNodeName().equalsIgnoreCase(name)) return node;
	      }
	    }
	    return null;
	  }




	  /**
	   * Helper Method. Searches through the child nodes of an element and returns an array of the matching nodes.
	   * @param element Element
	   * @param name String
	   * @param caseSensitive boolean
	   * @return ArrayList
	   */
	  public static List<Node> getChildNodesByName(Node element, String name, boolean caseSensitive) {
	    ArrayList<Node> nodes = new ArrayList<Node>();
	    NodeList list = element.getChildNodes();
	    for (int i = 0; i < list.getLength(); i++) {
	      Node node = list.item(i);
	      if (caseSensitive) {
	        if (node.getNodeName().equals(name)) nodes.add(node);
	      }
	      else {
	        if (node.getNodeName().equalsIgnoreCase(name)) nodes.add(node);
	      }
	    }
	    return nodes;
	  }
	  
	/**
	 * Parses an input source and generates an XML document.
	 * @param is An input source to an XML source.
	 * @return An XML doucument.
	 */
	public static Document parseXML(InputSource is) {
		  try {
			  Document doc = null;
			  DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			  doc = documentBuilder.parse(is);		  
		  return doc;
		  } catch (Exception e) {
			  throw new RuntimeException("Failed to parse XML source", e);
		  }
	}
	  
	  
	/**
	 * Parses an input stream and generates an XML document.
	 * @param is An input stream to an XML source.
	 * @return An XML doucument.
	 */
	public static Document parseXML(InputStream is) {
		return parseXML(new InputSource(is));
	}
	
	/**
	 * Parses a file and generates an XML document.
	 * @param file
	 * @return An XML doucument.
	 */
	public static Document parseXML(File file) {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			return parseXML(fis);
		} catch (Exception e) {
			throw new RuntimeException("Failed to open XML file:" + file, e);
		} finally {
			try { fis.close(); } catch (Exception e) {}
		}		
	}
	
	/**
	 * Parses the input stream of a URL and generates an XML document.
	 * @param xmlUrl The URL of the XML document.
	 * @return The parsed document.
	 */
	public static Document parseXML(URL xmlUrl) {
		InputStream is = null;
		BufferedInputStream bis = null;
		try {
			is = xmlUrl.openConnection().getInputStream();
			bis = new BufferedInputStream(is);
			return parseXML(bis);
		} catch (Exception e) {
			throw new RuntimeException("Failed to read XML URL:" + xmlUrl, e);
		} finally {
			try {bis.close();} catch (Exception e) {}
			try {is.close();} catch (Exception e) {}
		}
	}
	
	/**
	 * Parses an XML string and generates an XML document.
	 * @param xml The XML to parse
	 * @return An XML doucument.
	 */
	public static Document parseXML(CharSequence xml) {
		StringReader sr = new StringReader(xml.toString());
		return parseXML(new InputSource(sr));
	}
	
	/**
	 * Renders an XML node to a string
	 * @param node The xml node to render
	 * @return the rendered string or null if it failed conversion
	 */
	public static String renderNode(Node node) {
		if(node==null) return null;
		try {
			StringWriter writer = new StringWriter();
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.transform(new DOMSource(node), new StreamResult(writer));
			return writer.toString();
		} catch (Throwable e) {
			return null;
		}
		
	}
	
	/**
	 * Writes an element out to a file.
	 * @param element The XML element to write out.
	 * @param fileName The file name to write to. Existing file is overwriten.
	 */
	public static void writeElement(Element element, String fileName) {
		File file = new File(fileName);
		file.delete();
		DOMSource domSource = new DOMSource(element); 
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);
			StreamResult result = new StreamResult(fos);
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.transform(domSource, result);
			fos.flush();
			fos.close();
		} catch (Exception e) {
			throw new RuntimeException("Failed to write XML element to:" + fileName, e);
		} finally {
			try { fos.flush(); } catch (Exception e) {}
			try { fos.close(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Locates the attribute defined by the XPath expression in the XML file and replaces it with the passed value.
	 * @param fileName The XML file to update.
	 * @param xPathExpression An XPath expression that locates the attribute to update.
	 * @param attributeName The name of the attribute to update.
	 * @param value The value to update the attribute to.
	 */
	public static void updateAttributeInXMLFile(String fileName, String xPathExpression, String attributeName, String value) {
		try {
			Document document = parseXML(new File(fileName));			
			XPath xpath = XPathFactory.newInstance().newXPath();
			XPathExpression xpathExpression = xpath.compile(xPathExpression);
			Element element = (Element)xpathExpression.evaluate(document, NODE);
			
			element.getAttributeNode(attributeName).setValue(value);
			writeElement(document.getDocumentElement(), fileName);
			
		} catch (Exception e) {
			throw new RuntimeException("Failed to extract element from:" + fileName, e);
		}				
	}
	
	public static String getNodeTextValue(Node node) {
		return node.getFirstChild().getNodeValue();
	}	
	
	/**
	 * Locates the element defined by the XPath expression in the XML file and replaces the child text with the passed value.
	 * @param fileName The XML file to update.
	 * @param xPathExpression An XPath expression that locates the element to update.
	 * @param value The value to update the attribute to.
	 */
	public static void updateElementValueInXMLFile(String fileName, String xPathExpression, String value) {
		try {
			Document document = parseXML(new File(fileName));			
			XPath xpath = XPathFactory.newInstance().newXPath();
			XPathExpression xpathExpression = xpath.compile(xPathExpression);
			Element element = (Element)xpathExpression.evaluate(document, NODE);
			element.getFirstChild().setNodeValue(value);		
			writeElement(document.getDocumentElement(), fileName);			
		} catch (Exception e) {
			throw new RuntimeException("Failed to extract element from:" + fileName, e);
		}				
	}	
	
	public static String getAttribute(String fileName, String xPathExpression, String attributeName) {
		try {
			Document document = parseXML(new File(fileName));			
			XPath xpath = XPathFactory.newInstance().newXPath();
			XPathExpression xpathExpression = xpath.compile(xPathExpression);
			Node node = (Node)xpathExpression.evaluate(document, NODE);
			return getAttributeValueByName(node, attributeName);
		} catch (Exception e) {
			throw new RuntimeException("Failed to extract element from:" + fileName, e);
		}		
	}
	
	public static void main(String[] args) {
		log("XMLHelper");
		if(args.length < 1) return;
		if(args[0].equalsIgnoreCase("updateAttributeInXMLFile")) {
			if(args.length != 5) {
				System.err.println("Invalid Argument Count. Usage: updateAttributeInXMLFile <fileName> <xpath> <attributeName> <newValue>");
			}
			updateAttributeInXMLFile(args[1], args[2], args[3], args[4]);
		} else if(args[0].equalsIgnoreCase("updateElementValueInXMLFile")) {
			if(args.length != 4) {
				System.err.println("Invalid Argument Count. Usage: updateElementValueInXMLFile <fileName> <xpath> <newValue>");
			}
			updateElementValueInXMLFile(args[1], args[2], args[3]);			
		}
		
	}
	
	public static void log(Object message) {
		System.out.println(message);
	}
	
	/**
	 * Uses the passed XPath expression to locate a set of nodes in the passed element.
	 * @param targetNode The node to search.
	 * @param expression The XPath expression.
	 * @return A list of located nodes.
	 */
	public static List<Node> xGetNodes(Node targetNode, String expression) {
		List<Node> nodes = new ArrayList<Node>();		
		XPath xpath = null;
		try {
			xpath = XPathFactory.newInstance().newXPath();
			XPathExpression xpathExpression = xpath.compile(expression);
			NodeList nodeList = (NodeList)xpathExpression.evaluate(targetNode, NODESET);
			if(nodeList!=null) {
				for(int i = 0; i < nodeList.getLength(); i++) {
					nodes.add(nodeList.item(i));
				}
			}
			return nodes;
		} catch (Exception e) {
			throw new RuntimeException("XPath:Failed to locate the nodes:" + expression, e);
		}		
	}
	
	/**
	 * Uses the passed XPath expression to locate a single node in the passed element.
	 * @param targetNode The node to search.
	 * @param expression The XPath expression.
	 * @return The located node or null if one is not found.
	 */
	public static Node xGetNode(Node targetNode, String expression) {
		Node node = null;		
		XPath xpath = null;
		try {
			xpath = XPathFactory.newInstance().newXPath();
			XPathExpression xpathExpression = xpath.compile(expression);
			node = (Node)xpathExpression.evaluate(targetNode, NODE);
			return node;
		} catch (Exception e) {
			throw new RuntimeException("XPath:Failed to locate the node:" + expression, e);
		}		
	}
	
	/**
	 * Converts a node to a string.
	 * @param node The node to convert.
	 * @return A string representation of the node.
	 */
	public static String getStringFromNode(Node node) {
		DOMSource domSource = new DOMSource(node);
		ByteArrayOutputStream baos = null;
		try {
			baos = new ByteArrayOutputStream();
			StreamResult result = new StreamResult(baos);
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.transform(domSource, result);
			baos.flush();
			return new String(baos.toByteArray());
		} catch (Exception e) {
			throw new RuntimeException("Failed to stream node to string", e);
		} finally {
			try { baos.close(); } catch (Exception e) {}
		}
		
		
	}
	
	
	  
}
