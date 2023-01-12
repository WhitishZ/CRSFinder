package test;

import util.GrammarStringUtils;
import util.XMLDocUtils;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import analyzer.BasicAnalyzer;

// 测试用, 从软件配置项文件中提取配置名. 配置项文件应为xml格式.
public class ConfFileOptionExtractor {
	private List<String> optionNames;
	
	public ConfFileOptionExtractor() {
		optionNames = new ArrayList<String>();
	}
	public void loadConfFile(String fileName) throws SAXException, IOException, ParserConfigurationException, XPathExpressionException {
		optionNames.clear();
		Document confFileDocument = XMLDocUtils.getDocumentBuilder()
				.parse(fileName);
		BasicAnalyzer analyzer = new BasicAnalyzer();
		NodeList nodeList = analyzer.getNodeList(confFileDocument, "//name");
		for (int i = 0; i < nodeList.getLength(); ++i) {
			optionNames.add(GrammarStringUtils.removeSpaceLineBreaks(nodeList.item(i).getTextContent()));
		}
	}
	public void printOptionNamesToFile(String fileName) throws FileNotFoundException, UnsupportedEncodingException {
		if (optionNames.isEmpty()) {
			System.out.println("[WARN] OptionNames list is empty.");
			return;
		}
		PrintWriter writer = new PrintWriter(fileName, "UTF-8");
		for (String str : optionNames) {
			writer.println(str);
		}
		writer.flush();
		writer.close();
	}
}
