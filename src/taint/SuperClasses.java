package taint;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.w3c.dom.Node;
import analyzer.BasicAnalyzer;
import analyzer.DocumentAnalyzer;
import util.GrammarStringUtils;

public class SuperClasses {
	private DocumentAnalyzer documentAnalyzer;
	private Set<String> superClasses = new HashSet<String>();
	public SuperClasses(DocumentAnalyzer documentAnalyzer, Node classNode) {
		this.documentAnalyzer = documentAnalyzer;
		searchSuperClasses(classNode);
	}
	public void searchSuperClasses(Node classNode) {
		superClasses.clear();
		BasicAnalyzer basicAnalyzer = new BasicAnalyzer();
		HashMap<String, Node> allClasses = documentAnalyzer.getAllClasses();
		LinkedList<Node> workList = new LinkedList<>();
		workList.add(classNode);
		while (!workList.isEmpty()) {
			Node node = workList.remove();
			List<Node> nodeList = basicAnalyzer.getChildNodeByTagName(node, "super");
			if (nodeList.isEmpty()) return;
			if (nodeList.size() > 1) {
				// TODO: 暂不考虑多继承.
			}
			Node postfixNode = nodeList.iterator().next().getChildNodes().item(0);
			String fullName = null;
			String nameWithoutArgs = GrammarStringUtils.removeArgs(basicAnalyzer.getNodeName(postfixNode));
			String lastPart = GrammarStringUtils.getLastPart(nameWithoutArgs);
			Set<String> importedPackageNames = basicAnalyzer.getImportedPackages(postfixNode);
			for (String str : importedPackageNames) {
				fullName = str + lastPart;
				if (allClasses.containsKey(fullName)) {
					workList.add(allClasses.get(fullName));
					superClasses.add(fullName);
				}
			}
		}
	}
	public Set<String> getSuperClasses(Node classNode) {
		return superClasses;
	}
}
