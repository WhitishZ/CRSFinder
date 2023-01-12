package test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import analyzer.BasicAnalyzer;
import analyzer.DocumentAnalyzer;
import element.CallsiteOptionsPair;
import filter.EndPointFilter;
import locator.CallsiteLocator;
import locator.ParameterInfer;
import taint.SubClasses;
import util.GrammarStringUtils;

public class Launcher {
	public static void main(String[] args) throws XPathExpressionException, SAXException, IOException, ParserConfigurationException {
		// 源代码的抽象语法树XML文件.
		String sourceXmlFilePath = "input/hadoop-common.xml";
		// 默认配置XML文件, 仅供测试评估时使用.
        String defaultConfFilePath = "";
		// String defaultConfFilePath = "input/core-default.xml";
		// 配置类主类, 作为污点传播的开始.
		// TODO: 未来或许可以进一步改进, 以自动识别配置类.
		String mainConfClassName = "Configuration";
		// 从源代码中提取出来的CRS.
		String outExtractedCRSs = "output/hadoop-common-orps.txt";
		// 从默认配置文件中提取出来的所有配置项, 仅供测试评估时使用.
		String outDefaultConfigurations = "";
		// String outDefaultConfigurations = "output/common-default-confs.txt";
		
		System.out.println("[INFO] Start extracting configurations from default conf file.");
        if (!defaultConfFilePath.isEmpty()) {
            ConfFileOptionExtractor extractor1 = new ConfFileOptionExtractor();
            extractor1.loadConfFile(defaultConfFilePath);
            System.out.println("[INFO] Configurations extracted. Writing to file.");
            extractor1.printOptionNamesToFile(outDefaultConfigurations);
            System.out.println("[INFO] Configurations from default conf file are extracted and written successfully.");
        } else {
            System.out.println("[INFO] No default configuration file specified. Skipping this step.");
        }
		System.out.println("=======================================================================================");
		System.out.println("[INFO] Start loading source file.");
		DocumentAnalyzer documentAnalyzer = DocumentAnalyzer.getInstance();
		documentAnalyzer.loadDocument(sourceXmlFilePath);
		System.out.println("[INFO] Source file is loaded. Starting further anaylsis.");
		System.out.println("=======================================================================================");
		System.out.println("[INFO] Start extracting all sub classes of main conf class.");
		SubClasses allSubClasses = new SubClasses(documentAnalyzer, mainConfClassName);
		allSubClasses.searchAllSubClasses();
		System.out.println("[INFO] All sub classes of main conf class are extracted.");
		System.out.println("=======================================================================================");
		Map<String, Node> confClasses = allSubClasses.getSubClasses();
                // 除子类以外, 它们继承/扩展的基类也要加入到配置类当中.
		confClasses.put(mainConfClassName, documentAnalyzer.getClassNode("org.apache.hadoop.conf.Configuration"));
		System.out.println("[INFO] Intersted classes are specified. Start extracting callsites.");
		CallsiteLocator extractor2 = new CallsiteLocator(documentAnalyzer, confClasses);
		Set<Node> callsites = extractor2.getAllCallsites();
		System.out.println("[INFO] Callsite extraction finished.");
		System.out.println("[INFO] Number of callsites found: " + callsites.size());
		System.out.println("=======================================================================================");
		System.out.println("[INFO] Start analyzing details of callsites.");
		BasicAnalyzer basicAnalyzer = new BasicAnalyzer();
		List<CallsiteOptionsPair> results = new ArrayList<>();
        ParameterInfer paraInfer = new ParameterInfer(documentAnalyzer);
		int callsiteNo = 1;
		for (Node callsite : callsites) {
            if (EndPointFilter.filterCallsite(callsite) == false) continue; // Filter applied here,
			System.out.println("Callsite No. " + callsiteNo + ": ");
			System.out.println("Content: " + GrammarStringUtils.removeSpaceLineBreaks(callsite.getTextContent()));
			List<String> optionNames = new ArrayList<>();
			List<Node> parameters = basicAnalyzer.getParams(callsite);
			if (parameters == null) continue;
			for (Node parameter : parameters) {
				System.out.print("Parameter: " + parameter.getTextContent());
				String inferredValue = paraInfer.getValueFromExpr(parameter.getFirstChild());
				if (inferredValue != null && !inferredValue.isEmpty())
					System.out.println(". Inferred value: " + inferredValue);
				else System.out.println();
				optionNames.add(inferredValue);
			}
			results.add(new CallsiteOptionsPair(callsite, optionNames));
			callsiteNo++;
		}
		System.out.println("[INFO] Callsite information analysis finished.");
		System.out.println("=======================================================================================");
		System.out.println("[INFO] Start printing CRS information to file.");
		Output.printCOPToFile(results, outExtractedCRSs);
		System.out.println("[INFO] CRS information is extracted and written successfully.");
		System.out.println("[INFO] Exiting.");
	}
}
