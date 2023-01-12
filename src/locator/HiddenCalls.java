package locator;

import java.util.HashSet;
import java.util.Set;
import javax.xml.xpath.XPathExpressionException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import analyzer.BasicAnalyzer;
import analyzer.DocumentAnalyzer;

// 形式上不通过类的实例调用get方法的情况, 使用此类进行callsite推断.
public class HiddenCalls {
	private DocumentAnalyzer documentAnalyzer;
	public HiddenCalls(DocumentAnalyzer documentAnalyzer) {
		this.documentAnalyzer = documentAnalyzer;
	}
	// 类内部的其他方法调用了get方法.
	public Set<Node> getClassInsideCallsites(Node classNode) throws XPathExpressionException {
		// TODO: 子类或友元类.
		Set<Node> callsites = new HashSet<>();
		String xpath = ".//call[./name[contains(.,'get')]]";
		BasicAnalyzer basicAnalyzer = new BasicAnalyzer();
		NodeList callsiteList = basicAnalyzer.getNodeList(classNode, xpath);
		for (int i = 0; i < callsiteList.getLength(); i++) {
			callsites.add(callsiteList.item(i));
		}
		return callsites;
	}
	// 通过返回类型返回配置类实例的情况, 例如单例模式.
	// 该方法与后一个方法配合使用.
	public Set<String> getMethodByReturnType(String returnType) throws XPathExpressionException {
		String xpath = ".//function_decl[./type//name[text()='" + returnType + "']]|.//function[./type/name[text()='" + returnType + "']]";
		Set<String> funcNames = new HashSet<>();
		BasicAnalyzer basicAnalyzer = new BasicAnalyzer();
		NodeList funcNodes = basicAnalyzer.getNodeList(documentAnalyzer.getDocument(), xpath);
		for (int i = 0; i < funcNodes.getLength(); i++) {
			funcNames.add(basicAnalyzer.getNodeName(funcNodes.item(i)));
		}
		return funcNames;
	}
	public Set<Node> getCallsitesByMethodName(String methodName) throws XPathExpressionException {
		Set<Node> callsites = new HashSet<>();
		String xpath = ".//expr[./call[last()-1][.//name[text()='" + methodName + "']] and ./call[last()]/name[contains(.,'get')]]";
		BasicAnalyzer basicAnalyzer = new BasicAnalyzer();
		NodeList callsiteList = basicAnalyzer.getNodeList(documentAnalyzer.getDocument(), xpath);
		for (int i = 0; i < callsiteList.getLength(); i++) {
			callsites.add(callsiteList.item(i).getLastChild());
		}
		return callsites;
	}
	public Set<Node> getCallsitesByMethodName(Set<String> methodNames) throws XPathExpressionException {
		Set<Node> callsites = new HashSet<>();
		for (String methodName : methodNames) {
			callsites.addAll(getCallsitesByMethodName(methodName));
		}
		return callsites;
	}
}
