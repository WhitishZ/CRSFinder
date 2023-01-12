package analyzer;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import util.GrammarStringUtils;
import util.XMLDocUtils;

// 上下文强相关的分析类.
public class DocumentAnalyzer {
	private static DocumentAnalyzer instance = null;
	private BasicAnalyzer basicAnalyzer;
	private Document document;
	private HashMap<String, Node> allClasses;	// TODO: 匿名类是否需要处理.
	private DocumentAnalyzer() {
		document = null;
		allClasses = new HashMap<>();
		basicAnalyzer = new BasicAnalyzer();
	}
	public static DocumentAnalyzer getInstance() {
		if (instance == null) instance = new DocumentAnalyzer();
		return instance;
	}
	public Document getDocument() {
		return document;
	}
	public void loadDocument(String docPath) throws XPathExpressionException, SAXException, IOException, ParserConfigurationException {
		loadDocument(XMLDocUtils.getDocumentBuilder().parse(new File(docPath)));
	}
	public void loadDocument(Document doc) throws XPathExpressionException {
		allClasses.clear();
		document = doc;
		// 提取class或interface的name域.
		String xpath = ".//class/name|.//interface/name";
		NodeList classNameNodes = basicAnalyzer.getNodeList(document, xpath);
		for (int i = 0; i < classNameNodes.getLength(); i++) {
			Node fileNode = basicAnalyzer.getFileOf(classNameNodes.item(i));
			String fileName = fileNode.getAttributes().getNamedItem("filename").getNodeValue();
			String packageName = GrammarStringUtils.getPackageName(fileName);
			String fullClassName = packageName + "." + classNameNodes.item(i).getFirstChild().getTextContent();
			allClasses.put(fullClassName, classNameNodes.item(i).getParentNode());
		}
		// 最终, allClasses中的键形如: default.package1.Animal
	}
	public HashMap<String, Node> getAllClasses() {
		return allClasses;
	}
	// 返回外部引入类classNode所处于的外部包名.
	public String getPackageOfExternalClass(Node classNode) {
		Set<String> packageNames = basicAnalyzer.getImportedPackages(classNode);
		String className = classNode.getTextContent();
		Iterator<String> it = packageNames.iterator();
		while (it.hasNext()) {
			String packageName = (String)it.next();
			String fullClassName = packageName + "." + className;
			if (allClasses.containsKey(fullClassName)) return packageName;
		}
		return null;
	}
	public String getPackageOfExternalClass(Node instanceVariable, String instanceType) {
		Set<String> packageNames = basicAnalyzer.getImportedPackages(instanceVariable);
		Iterator<String> it = packageNames.iterator();
		while (it.hasNext()) {
			String packageName = (String)it.next();
			String fullClassName = packageName + "." + instanceType;
			if (allClasses.containsKey(fullClassName)) return packageName;
		}
		return null;
	}
	// 返回文件名为fileName的文件中, 名为className的类节点.
	public Node getClassNode(String fileName, String className) throws XPathExpressionException {
		String xpath = "unit/unit[@filename='" + fileName
			+ "']/class[./name='" + className + "']";
		return basicAnalyzer.getNode(document, xpath);
	}
	// 返回类名为fullClassName的节点.
	public Node getClassNode(String fullClassName) {
		fullClassName = GrammarStringUtils.removeSpaceLineBreaks(fullClassName);
		return allClasses.get(fullClassName);
	}
	// 返回包名为packageName下, 名为className类中, varName的声明.
	public NodeList getVarDecl(String packageName, String className, String varName)
			throws XPathExpressionException {
		Node classNode = getClassNode(packageName, className);
		String xpath = "./block/decl_stmt[./decl/name='" + varName + "']";
		return basicAnalyzer.getNodeList(classNode, xpath);
	}
	// 返回纵向继承superClass的一级子类.
	public NodeList getSubClasses(String superClass) throws XPathExpressionException {
		String[] splited = superClass.split("\\.");
		String className = splited[splited.length - 1];
		String xpath = "//class[./super_list/extends/super/name[text()='" + className + "']]";
		return basicAnalyzer.getNodeList(document, xpath);
	}
	// 返回类名为fullClassName的实例变量.
	public NodeList getClassInstanceVars(String fullClassName) throws XPathExpressionException {
		String[] splited = fullClassName.split("\\.");
		String className = splited[splited.length - 1];
		String xpath = "//decl[./type/name[text()='" + className + "']|./type/name/name[last()][text()='" + className + "']]";
		return basicAnalyzer.getNodeList(document, xpath);
	}
	public NodeList getNodesByLineNum(String fileName, int lineNum, String nodeName) throws XPathExpressionException {
		String xpath = "unit/unit[@filename='" + fileName+ "']//"+nodeName+"[.//name[@ line='" + lineNum + "']]";
		return basicAnalyzer.getNodeList(document, xpath);
	}
}
