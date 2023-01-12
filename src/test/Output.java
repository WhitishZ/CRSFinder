package test;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.w3c.dom.Node;

import analyzer.BasicAnalyzer;
import element.CallsiteOptionsPair;

public class Output {
	public static void printCOPToFile(List<CallsiteOptionsPair> list, String outFileName) throws FileNotFoundException, UnsupportedEncodingException {
		PrintWriter writer = new PrintWriter(outFileName, "UTF-8");
		BasicAnalyzer basicAnalyzer = new BasicAnalyzer();
		int i = 0;
		for (CallsiteOptionsPair cop : list) {
			Node callsite = cop.getCallsite();
			String fileName = basicAnalyzer.getFileName(callsite);
			String lineNum = basicAnalyzer.getLineNum(callsite);
			String method = callsite.getTextContent().replaceAll("\\r|\\n", "");
			List<String> optionNames = cop.getOptionNames();
			String options = "";
			for (String opName : optionNames) {
				if (opName == null || opName.isEmpty()) continue;
				options = options + opName.replace("\"", "") + ", ";
			}
			writer.println("Found callsite[" + i + "] in File: " + fileName
					+ " LineNum: " + lineNum + " Method name: " + method);
			writer.println("Inferred parameter values: " + options);
			writer.println();
			writer.flush();
			i++;
		}
		writer.close();
	}
}
