package locator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import analyzer.BasicAnalyzer;
import analyzer.DocumentAnalyzer;
import taint.SuperClasses;
import util.GrammarStringUtils;

public class ParameterInfer {
	private DocumentAnalyzer documentAnalyzer;
	private static HashMap<String, Node> cacheMap = new HashMap<>();
	public ParameterInfer(DocumentAnalyzer documentAnalyzer) {
		this.documentAnalyzer = documentAnalyzer;
	}
	// 返回声明语句的右部.
	// 例如 String str = "a" + "b" -> "a" + "b"
	public Node getInitValueNodeFromDecl(Node decl) throws XPathExpressionException {
		String xpath = "./decl/init/expr";
		BasicAnalyzer basicAnalyzer = new BasicAnalyzer();
		NodeList list = basicAnalyzer.getNodeList(decl, xpath);
		if (list.getLength() == 0 || list.getLength() > 1) return null;
		return list.item(0);
	}
	// 对于一个用于赋值的表达式, 返回其所对应的值, 拼接可以拼接的字符串(仅支持加号连接).
	// 例如: "str" + 1 + "." -> "str1."
	public String getValueFromExpr(Node expression) throws XPathExpressionException {
		if (expression == null) return null;
		String fullValue = "";
		BasicAnalyzer basicAnalyzer = new BasicAnalyzer();
		if (expression.getNodeName().equals("decl_stmt")) {
			expression = getInitValueNodeFromDecl(expression);
			if (expression == null) return null;
		}
		List<Node> elements = basicAnalyzer.removeWhiteSpaceNode(expression.getChildNodes());
		int i = 0;	// i为奇数时, 对应节点为操作符. 否则为量.
		for (Node element : elements) {
			if (i % 2 == 0) {
				Element oprand = (Element)element;
				if (oprand.getNodeName().equals("name")) {
					List<Node> parts = new ArrayList<Node>();
					if (oprand.getChildNodes().getLength() == 2)
						parts.add(oprand);
					else parts = basicAnalyzer.removeWhiteSpaceNode(oprand.getChildNodes());
					if (parts.size() <= 3) fullValue = getValueFromExpr(getDeclOfVarExpr(parts));
					else fullValue = getValueFromExpr(getDeclWithFullName(oprand));
				}
				if (oprand.getNodeName().equals("literal") && oprand.getAttribute("type").equals("string"))
					fullValue = fullValue + oprand.getTextContent();
			} else {
				// TODO: 暂无法处理加号以外的连接操作.
				if (!element.getTextContent().equals("+")) break;
			}
			i++;
		}
		return fullValue;
	}
	public Node getDeclOfVar(Node variableName) throws XPathExpressionException, DOMException {
		Node decl = null;
		BasicAnalyzer basicAnalyzer = new BasicAnalyzer();
		// String userName = Conf.getCurrentUser;
		//                        ^
		if (basicAnalyzer.isClassVariable(variableName)) {
			// 变量为某个类的实例.
			Node objectVariable = variableName.getPreviousSibling().getPreviousSibling();
			if (objectVariable.getTextContent().equals("this")) {
				// 变量为this.XXX形式, 直接在当前类中搜索.
				decl = getDeclOfInstanceVar(basicAnalyzer.getClassOf(variableName), variableName.getTextContent());
			} else {
				// 是否为方法中声明的变量.
				Node declInFunc = basicAnalyzer.getDeclFromFunction(objectVariable);
				// 是否为类的成员变量.(等价于this.XXX)
				Node declInClass = basicAnalyzer.getDeclFromFields(objectVariable);
				if (declInFunc != null || declInClass != null) {
					Node declInstanceVariable = null;
					if (declInFunc != null) declInstanceVariable = declInFunc;
					else declInstanceVariable = declInClass;
					decl = getDeclOfInstanceVar(getClassOfInstance(declInstanceVariable), variableName.getTextContent());
				} else {
					// 外部引入的类实例.
					String packageName = documentAnalyzer.getPackageOfExternalClass(objectVariable);
					if (packageName != null) {
						String fullClassName = packageName + "." + objectVariable.getTextContent();
						decl = getDeclOfInstanceVar(documentAnalyzer.getClassNode(fullClassName), variableName.getTextContent());
					}
				}
			}
		} else {
			// 变量为原生类型, 按方法内->类内->外部引入的顺序搜索.
			decl = basicAnalyzer.getDeclFromFunction(variableName);
			if (decl == null) {
				decl = getDeclOfInstanceVar(basicAnalyzer.getClassOf(variableName), variableName.getTextContent());
				if (decl == null) {
					String fullClassName = getFullClassNameForImportedVariable(variableName);
					if (fullClassName != null)
						decl = getDeclOfInstanceVar(documentAnalyzer.getClassNode(fullClassName), variableName.getTextContent());
				}
			}
		}
		return decl;
	}
	public Node getDeclOfVarExpr(List<Node> variableName) throws XPathExpressionException {
		BasicAnalyzer basicAnalyzer = new BasicAnalyzer();
		Node decl = null;
		// 方法内变量 -> 类内变量 -> 引入变量.
		if (variableName.size() == 1) {
			Node varNode = variableName.get(0);
			decl = basicAnalyzer.getDeclFromFunction(varNode);
			if (decl == null) {
				decl = getDeclOfInstanceVar(basicAnalyzer.getClassOf(varNode), GrammarStringUtils.removeSpaceLineBreaks(varNode.getTextContent()));
				if (decl == null) {
					String fullClassName = getFullClassNameForImportedVariable(varNode);
					if (fullClassName != null) {
						Node externalClass = cacheMap.get(fullClassName);
						if (externalClass == null) {
							externalClass = documentAnalyzer.getClassNode(fullClassName);
							if (externalClass != null) cacheMap.put(fullClassName, externalClass);
						}
						decl = getDeclOfInstanceVar(externalClass, GrammarStringUtils.removeSpaceLineBreaks(varNode.getTextContent()));
					}
				}
			}
		}
		else if (variableName.size() == 3) {
			Node instanceVar = variableName.get(0);
			Node referSymbol = variableName.get(1);
			Node varNode = variableName.get(2);
			if (instanceVar.getNodeName().equals("name")
					&& GrammarStringUtils.removeSpaceLineBreaks(referSymbol.getTextContent()).equals(".")
					&& varNode.getNodeName().equals("name")) {
				if (GrammarStringUtils.removeSpaceLineBreaks(instanceVar.getTextContent()).equals("this")) {
					decl = getDeclOfInstanceVar(basicAnalyzer.getClassOf(instanceVar),
							GrammarStringUtils.removeSpaceLineBreaks(varNode.getTextContent()));
				} else {
					Node declInFunc = basicAnalyzer.getDeclFromFunction(instanceVar);
					Node declInClass = basicAnalyzer.getDeclFromFields(instanceVar);
					if (declInFunc != null || declInClass != null) {
						Node declInstanceVariable = null;
						if (declInFunc != null) declInstanceVariable = declInFunc;
						else declInstanceVariable = declInClass;
						decl = getDeclOfInstanceVar(getClassOfInstance(declInstanceVariable), GrammarStringUtils.removeSpaceLineBreaks(varNode.getTextContent()));
					} else {
						// 外部引入的类实例.
						String packageName = documentAnalyzer.getPackageOfExternalClass(instanceVar);
						if (packageName != null) {
							String fullClassName = packageName + "." + GrammarStringUtils.removeSpaceLineBreaks(instanceVar.getTextContent());
							Node externalClass = cacheMap.get(fullClassName);
							if (externalClass == null) {
								externalClass = documentAnalyzer.getClassNode(fullClassName);
								if (externalClass != null)
									cacheMap.put(fullClassName, externalClass);
							}
							decl = getDeclOfInstanceVar(externalClass, GrammarStringUtils.removeSpaceLineBreaks(varNode.getTextContent()));
						}
					}
				}
			}
		}
		return decl;
	}
	public String getFullClassNameForImportedVariable(Node variable) {
		String className = null;
		BasicAnalyzer basicAnalyzer = new BasicAnalyzer();
		Node fileUnit = basicAnalyzer.getFileOf(variable);
		String varName = GrammarStringUtils.removeSpaceLineBreaks(variable.getTextContent());
		if (fileUnit == null) {
			System.err.println("This variable is out of all files.");
		}
		if (fileUnit instanceof Element) {
			Element fileNode = (Element)fileUnit;
			NodeList packages = fileNode.getElementsByTagName("import");
			for (int i = 0; i < packages.getLength(); i++) {
				String packageName = packages.item(i).getFirstChild().getNextSibling().getTextContent();
				if (packageName.endsWith("." + varName)) {
					className = packageName.substring(0, packageName.length() - varName.length() - 1);
					break;
				} else if (packageName.endsWith(".*")) {
					className = packageName.substring(0, packageName.length() - 2);
				}
			}
		}
		return className;
	}
	public Node getClassOfInstance(Node declInstanceVarNode) {
		String instanceType = getVariableType(declInstanceVarNode);
		String packageName = documentAnalyzer.getPackageOfExternalClass(declInstanceVarNode, instanceType);
		if (packageName != null)
			return documentAnalyzer.getClassNode(packageName + "." + instanceType);
		return null;
	}
	public String getVariableType(Node declNode) {
		String typeName = null;
		Node type = declNode.getFirstChild().getFirstChild();
		NodeList name = type.getChildNodes();
		for (int i = 0; i < name.getLength(); i++) {
			if (name.item(i).getNodeName().equals("name")) {
				typeName = name.item(i).getTextContent();
				break;
			}
		}
		return typeName;
	}
	// 在类(及其父类)中寻找variableName变量的声明语句.
	public Node getDeclOfInstanceVar(Node classNode, String variableName) throws XPathExpressionException {
		if (classNode == null) return null;
		String xpath = "./block/decl_stmt[./decl/name='" + variableName + "']";
		BasicAnalyzer basicAnalyzer = new BasicAnalyzer();
		Node declNode = basicAnalyzer.getNode(classNode, xpath);
		if (declNode == null) {
			SuperClasses inferClass = new SuperClasses(documentAnalyzer, classNode);
			Set<String> superClasses = inferClass.getSuperClasses(classNode);
			for (String sc : superClasses) {
				Node superClass = documentAnalyzer.getClassNode(sc);
				declNode = getDeclOfInstanceVar(superClass, variableName);
				if (declNode != null) break;
			}
		}
		return declNode;
	}
	public Node getDeclWithFullName(Node node) throws XPathExpressionException {
		Node decl = null;
		String str = node.getTextContent();
		str = GrammarStringUtils.removeSpaceLineBreaks(str);
		String[] splited = str.split("\\.");
		String variableName = splited[splited.length - 1];
		String fullClassName = "";
		for (int i = 0; i < splited.length - 2; i++)
			fullClassName = fullClassName + splited[i] + ".";
		fullClassName = fullClassName + splited[splited.length - 2];
		Node classNode = documentAnalyzer.getClassNode(fullClassName);
		if (classNode != null)
			decl = getDeclOfInstanceVar(classNode, variableName);
		return decl;
 	}
}
