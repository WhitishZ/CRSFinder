package locator;

import element.VariableScopePair;
import taint.SubClasses;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.xml.xpath.XPathExpressionException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import analyzer.BasicAnalyzer;
import analyzer.DocumentAnalyzer;

// 从类的实例变量入手, 推断callsites.
public class InstanceCalls {
	private DocumentAnalyzer documentAnalyzer;
	public InstanceCalls(DocumentAnalyzer documentAnalyzer) {
		this.documentAnalyzer = documentAnalyzer;
	}
	// 返回类的实例变量及其scope.
	public List<VariableScopePair> getClassInstanceVarScopes(String fullClassName)
			throws XPathExpressionException {
		List<VariableScopePair> list = new ArrayList<>();
		NodeList declNodes = documentAnalyzer.getClassInstanceVars(fullClassName);
		BasicAnalyzer basicAnalyzer = new BasicAnalyzer();
		for (int i = 0; i < declNodes.getLength(); i++) {
			Node node = declNodes.item(i);
			String varName = basicAnalyzer.getChildNodeByTagName(node, "name").iterator().next().getTextContent();
			Node scope = null;
			Node parentNode = node.getParentNode();
			Node grandParentNode = parentNode.getParentNode();
			if (parentNode.getNodeName().equals("parameter")
					&& grandParentNode.getNodeName().equals("parameter_list"))
				scope = grandParentNode.getParentNode();
			else scope = basicAnalyzer.getScope(node);
			list.add(new VariableScopePair(varName, scope));
		}
		return list;
	}
	// 在scope内搜索varName的get方法的调用点.
	// 例: int age = userConf.getAge();
	public Set<Node> searchCallsites(String varName, Node scope) throws XPathExpressionException {
		Set<Node> statements = new HashSet<>();
		String xpath = ".//call[./name/name[last()-1][text()='"+varName+"']][./name/operator[text()='.']][./name/name[last()][contains(.,'get')]]";
		BasicAnalyzer basicAnalyzer = new BasicAnalyzer();
		NodeList nodes = basicAnalyzer.getNodeList(scope, xpath);
		for (int i = 0; i < nodes.getLength(); i++) {
			statements.add(nodes.item(i));
//			System.out.println("get-method found in statement: " + nodes.item(i).getTextContent());
		}
		return statements;
	}
	public Set<Node> searchCallsites(String varName, Set<Node> scopes) throws XPathExpressionException {
		Set<Node> statements = new HashSet<>();
		Iterator<Node> it = scopes.iterator();
		while (it.hasNext()) {
			Node curScope = it.next();
			statements.addAll(searchCallsites(varName, curScope));
		}
		return statements;
	}
	// 返回类fullClassName及其一级子类的所有get方法调用点.
	public Set<Node> getClassGetMethodCallsites(String fullClassName) throws XPathExpressionException {
		// TODO: 这里因为taint.SubClasses实现有些问题, 只能查找到其一级子类.
		BasicAnalyzer basicAnalyzer = new BasicAnalyzer();
		Set<Node> statements = new HashSet<>();
		List<VariableScopePair> list = getClassInstanceVarScopes(fullClassName);
		Iterator<VariableScopePair> it = list.iterator();
		while (it.hasNext()) {
			VariableScopePair tmp = (VariableScopePair)it.next();
			Node scope = tmp.getScope();
			statements.addAll(searchCallsites(tmp.getVariableName(), tmp.getScope()));
			if (scope.getParentNode().getNodeName().equals("class")
					|| scope.getParentNode().getNodeName().equals("interface")) {
				String className = basicAnalyzer.getNodeName(scope.getParentNode());
//				System.out.println(className + " has sub classes. Searching further callsites...");
				SubClasses sc = new SubClasses(DocumentAnalyzer.getInstance(), className);
				statements.addAll(searchCallsites(tmp.getVariableName(), sc.getSubClassNodes()));
			}
		}
		return statements;
	}
}
