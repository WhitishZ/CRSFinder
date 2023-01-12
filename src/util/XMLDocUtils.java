package util;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

public class XMLDocUtils {
	public static DocumentBuilder xmlDocBuilder = null;
	public static XPath xpath = null;
	public static DocumentBuilder getDocumentBuilder() throws ParserConfigurationException {
		if (xmlDocBuilder == null) {
			xmlDocBuilder = DocumentBuilderFactory.newInstance()
					.newDocumentBuilder();
		}
		return xmlDocBuilder;
	}
	public static XPath getXPath() {
		if (xpath == null) {
			xpath = XPathFactory.newInstance().newXPath();
		}
		return xpath;
	}
}
