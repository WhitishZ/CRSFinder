package analyzer;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import util.GrammarStringUtils;
import util.XMLDocUtils;

// 基本分析方法, 仅与节点上下文有关, 与当前加载的分析文件无关.
public class BasicAnalyzer {
	// 去除xml生成后的一些无用节点(如换行节点等).
	public List<Node> removeWhiteSpaceNode(NodeList list) {
		ArrayList<Node> newList = new ArrayList<>();
		for (int i = 0; i < list.getLength(); i++) {
			if (list.item(i).getNodeType() == Node.ELEMENT_NODE)
				newList.add(list.item(i));
                        if (list.item(i).getNodeType() == Node.TEXT_NODE) {
                                Node node = list.item(i);
                                String nodeStr = node.getNodeValue();
                                if (nodeStr != null && !nodeStr.trim().isEmpty())
                                        newList.add(node);
                        }
		}
		return newList;
	}
	// 返回root指向的节点下, 符合xpath的(单个)节点.
	public Node getNode(Node root, String path) throws XPathExpressionException {
		XPath xpath = XMLDocUtils.getXPath();
		XPathExpression xpathexp = xpath.compile(path);
		return (Node)xpathexp.evaluate(root, XPathConstants.NODE);
	}
	// 返回root指向的节点下, 符合xpath的节点列表.
	public NodeList getNodeList(Node root, String path) throws XPathExpressionException {
		XPath xpath = XMLDocUtils.getXPath();
		XPathExpression xpathexp = xpath.compile(path);
		return (NodeList) xpathexp.evaluate(root, XPathConstants.NODESET);
	}
	// 返回node的所属方法.
	public Node getMethodOf(Node node) {
		Node parent = node.getParentNode();
		while (parent != null) {
			String nodeName = parent.getNodeName();
			// 搜索范围不应超过当前文件.
			if (nodeName.equals("unit")) break;
			// TODO: <lambda>
			if (nodeName.equals("function") || nodeName.equals("constructor")) {
				return parent;
			}
			parent = parent.getParentNode();
		}
		return null;
	}
	// 返回node的所属类/接口.
	public Node getClassOf(Node node) {
		Node parent = node.getParentNode();
		while (parent != null) {
			String nodeName = parent.getNodeName();
			if (nodeName.equals("unit")) break;
			if (nodeName.equals("class") || nodeName.equals("interface"))
				return parent;
			parent = parent.getParentNode();
		}
		return null;
	}
	// 返回node的所属文件.
	public Node getFileOf(Node node) {
		if (node.getNodeName().equals("unit")) return node;
		Node parent = node.getParentNode();
		while (parent != null) {
			String nodeName = parent.getNodeName();
			if (nodeName.equals("unit")) return parent;
			parent = parent.getParentNode();
		}
		return null;
	}
	// 判断node是否为某个类的成员变量.
	// 例: String userName = Conf.currentUser;
	public boolean isClassVariable(Node node) {
		Node prevNode = node.getPreviousSibling();
		if (prevNode == null) return false;
		// 变量名前为.运算符
		if (!prevNode.getTextContent().equals(".")) return false;
		prevNode = prevNode.getPreviousSibling();
		if (prevNode == null) return false;
		// 再往前为类名.
		if (!prevNode.getNodeName().equals("name")) return false;
		Node parentNode = prevNode.getParentNode();
		if (parentNode == null) return false;
		// 与前两者共同组成为一个变量.
		if (!parentNode.getNodeName().equals("name")) return false;
		parentNode = parentNode.getParentNode();
		if (parentNode == null) return false;
		// 凡是提到类的成员变量, 必应用于赋值等操作, 否则没有意义.
		if (parentNode.getNodeName().equals("expr")) return true;
		return false;
	}
	// 返回node的(类内)声明.
	public Node getDeclFromFields(Node node) throws XPathExpressionException {
		Node classNode = getClassOf(node);
		String xpath = "./block/decl_stmt[./decl/name='" + node.getTextContent() + "']";
		return getNode(classNode, xpath);
	}
	// 返回node的(方法内)声明.
	public Node getDeclFromFunction(Node node) throws XPathExpressionException {
		Node parentNode = node.getParentNode();
		while (parentNode != null) {
			if (parentNode.getNodeName().equals("function")
					|| parentNode.getNodeName().equals("constructor")) break;
			String xpath = ".//decl_stmt[./decl/name='" + node.getTextContent() + "']";
			Node declNode = getNode(parentNode, xpath);
			if (declNode != null) return declNode;
			parentNode = parentNode.getParentNode();
		}
		return null;
	}
	// 返回node所属文件中, 引入的所有包名. 注意是包而非类.
	// 例如: import edu.jlu.* -> edu.jlu
	public Set<String> getImportedPackages(Node node) {
		HashSet<String> importedPackageNames = new HashSet<>();
		Node fileNode = getFileOf(node);
		if (fileNode == null) return null;	// 错误: 节点在文件之外.
		if (fileNode instanceof Element) {
			NodeList packageNames = ((Element)fileNode).getElementsByTagName("import");
			for (int i = 0; i < packageNames.getLength(); i++) {
				String packageName = "";
				NodeList packageParts = packageNames.item(i).getFirstChild().getNextSibling().getChildNodes();
				// 过滤掉所有java.*, javax.*开头的包.
				String firstPart = packageParts.item(0).getTextContent();
				if (firstPart.equals("java") || firstPart.equals("javax")) continue;
				for (int j = 0; j < packageParts.getLength() - 2; j++) {
					packageName = packageName + packageParts.item(j).getTextContent();
				}
				importedPackageNames.add(packageName);
			}
			// 这个文件当前位于的package也是它所引用的包.
			NodeList currentPackageNode = ((Element)fileNode).getElementsByTagName("package");
			if (currentPackageNode.getLength() != 1) {
				// 这个文件中的package声明有错误.
				System.err.println("[ERROR] getImportedPackages: Package declaration is wrong in file: " + fileNode.getTextContent());
			} else {
				Node currentPackageNameNode = currentPackageNode.item(0).getFirstChild().getNextSibling();
				String currentPackageName = currentPackageNameNode.getTextContent();
				importedPackageNames.add(currentPackageName);
			}
		}
		return importedPackageNames;
	}
	public List<Node> getChildNodeByTagName(Node node, String tagName) {
		List<Node> list = new ArrayList<Node>();
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			if (children.item(i).getNodeName().equals(tagName))
				list.add(children.item(i));
		}
		return list;
	}
	// 返回stm所在scope.
	public Node getScope(Node stm) {
		Node parent = stm.getParentNode();
		String nodeName = parent.getNodeName();
		while (!nodeName.equals("class") && !nodeName.equals("interface")) {
			if (nodeName.equals("block") || nodeName.equals("for") || nodeName.equals("while"))
				return parent;
			parent = parent.getParentNode();
			nodeName = parent.getNodeName();
		}
		return parent;
	}
	// 返回node所在文件的文件名(Java语法)
	public String getFileName(Node node) {
		Node fileNode = getFileOf(node);
		String astFileName = fileNode.getAttributes().getNamedItem("filename").getNodeValue();
		return GrammarStringUtils.toJavaFileName(astFileName);
	}
	// 返回callsite的参数列表.
	public List<Node> getParams(Node callsite) {
		ArrayList<Node> paras = new ArrayList<>();
		List<Node> argList = getChildNodeByTagName(callsite, "argument_list");
		if (argList.isEmpty()) return paras; // 调用时无传参.
		Iterator<Node> it = argList.iterator();
		while (it.hasNext()) {
			paras.addAll(getChildNodeByTagName(it.next(), "argument"));
		}
		return paras;
	}
	public void printNode(Node node) {
		String fileName = getFileName(node);
		String lineNum = getLineNum(node);
		System.out.println("File: " + fileName + ": " + lineNum + "---" + node.getTextContent());
	}
	public void printNodesToFile(Set<Node> nodes, String outFileName) throws FileNotFoundException, UnsupportedEncodingException {
		PrintWriter writer = new PrintWriter(outFileName, "UTF-8");
		Iterator<Node> it = nodes.iterator();
		int index = 0;
		while (it.hasNext()) {
			Node thisNode = it.next();
			String fileName = getFileName(thisNode);
			String lineNum = getLineNum(thisNode);
			String content = thisNode.getTextContent();
			content = content.replaceAll("\\r|\\n", "");
			writer.println("Node[" + index + "] File: " + fileName + ": LineNum: " + lineNum + ": Content: " + content);
			writer.flush();
			index++;
		}
		writer.close();
	}
	public String getLineNum(Node node) {
		try {
			Element e = (Element)node;
			if (e.hasAttribute("pos:start"))
				return e.getAttribute("pos:start").split(":")[0];
			else {
				while (e.hasChildNodes()) {
					Element tmp = (Element)e.getFirstChild();
					if (tmp.hasAttribute("pos:start"))
						return tmp.getAttribute("pos:start").split(":")[0];
					e = tmp;
				}
			}
		} catch (ClassCastException e) {
			System.err.println("[ERROR] Could not find the line number of a callsite.");
			return "LINENUM_NOT_FOUND";
		}
		return "LINENUM_NOT_FOUND";
	}
	public String getNodeName(Node node) {
		List<Node> nameNodes = getChildNodeByTagName(node, "name");
                if (nameNodes.isEmpty()) return null;
		return nameNodes.iterator().next().getFirstChild().getTextContent();
	}
}
