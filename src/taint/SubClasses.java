package taint;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import javax.xml.xpath.XPathExpressionException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import analyzer.BasicAnalyzer;
import analyzer.DocumentAnalyzer;
import filter.EndPointFilter;

public class SubClasses {
	private DocumentAnalyzer documentAnalyzer;
	private HashSet<String> subClassNames;
	private HashSet<String> allSubClassNames;
	private HashSet<Node> subClassNodes;
	private HashSet<Node> allSubClassNodes;
	private HashMap<String, Node> subClasses;
	private HashMap<String, Node> allSubClasses;
	private String fatherClassName;
	public SubClasses(DocumentAnalyzer documentAnalyzer, String className) throws XPathExpressionException {
		this.documentAnalyzer = documentAnalyzer;
		subClassNames = new HashSet<>();
		subClassNodes = new HashSet<>();
		subClasses = new HashMap<>();
		allSubClassNames = new HashSet<>();
		allSubClassNodes = new HashSet<>();
		allSubClasses = new HashMap<>();
		fatherClassName = className;
		searchDirectSubClasses();
	}
	
	public HashSet<String> getSubClassNames() {
		return subClassNames;
	}

	public HashSet<Node> getSubClassNodes() {
		return subClassNodes;
	}

	public HashMap<String, Node> getSubClasses() {
		return subClasses;
	}
	// 搜索并储存某类的扩展类.
	@SuppressWarnings("unchecked")
	public void searchAllSubClasses() throws XPathExpressionException {
		allSubClassNames.clear();
		allSubClassNodes.clear();
		allSubClassNames.addAll(subClassNames);
		allSubClassNodes.addAll(subClassNodes);
		allSubClasses = (HashMap<String, Node>)subClasses.clone();
		LinkedList<String> workList = new LinkedList<>();
		HashSet<String> curSubClassNames = new HashSet<>();
		workList.addAll(subClassNames);
		BasicAnalyzer basicAnalyzer = new BasicAnalyzer();
		while (!workList.isEmpty()) {
			String temp = workList.removeFirst();
			curSubClassNames.clear();
			NodeList list = documentAnalyzer.getSubClasses(temp);
			for (int i = 0; i < list.getLength(); i++) {
				List<Node> subClass = basicAnalyzer.getChildNodeByTagName(list.item(i), "name");
				String subClassName = subClass.iterator().next().getFirstChild().getTextContent();
                                // 编辑距离启发式过滤.
                                if (EndPointFilter.calculateEditDistance(subClassName, temp) > temp.length() / 2) continue;
				curSubClassNames.add(subClassName);
				allSubClassNames.add(subClassName);
				allSubClassNodes.add(list.item(i));
				allSubClasses.put(subClassName, list.item(i));
			}
			workList.addAll(curSubClassNames);
		}
	}
	// 搜索并储存某类的一级子类.
	public void searchDirectSubClasses() throws XPathExpressionException {
		subClassNames.clear();
		subClassNodes.clear();
		subClasses.clear();
		NodeList list = documentAnalyzer.getSubClasses(fatherClassName);
		BasicAnalyzer basicAnalyzer = new BasicAnalyzer();
		for (int i = 0; i < list.getLength(); i++) {
			List<Node> subClasses = basicAnalyzer.getChildNodeByTagName(list.item(i), "name");
			String subClassName = subClasses.iterator().next().getFirstChild().getTextContent();
			subClassNames.add(subClassName);
			subClassNodes.add(list.item(i));
			this.subClasses.put(subClassName, list.item(i));
		}
	}
}
