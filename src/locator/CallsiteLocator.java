package locator;

import analyzer.DocumentAnalyzer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.xml.xpath.XPathExpressionException;
import org.w3c.dom.Node;

public class CallsiteLocator {
	private InstanceCalls ic;
	private HiddenCalls hc;
	private Map<String, Node> interestedClasses;
	private Set<Node> allCallsites;
	public CallsiteLocator(DocumentAnalyzer da, Map<String, Node> interestedClasses) throws XPathExpressionException {
		ic = new InstanceCalls(da);
		hc = new HiddenCalls(da);
		allCallsites = new HashSet<Node>();
		this.interestedClasses = interestedClasses;
		searchAllCallsites();
	}
	public Set<Node> getAllCallsites() {
		return allCallsites;
	}
	public void searchAllCallsites() throws XPathExpressionException {
		allCallsites.clear();
		Iterator<Map.Entry<String, Node>> it = interestedClasses.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, Node> entry = (Map.Entry<String, Node>)it.next();
			String className = (String)entry.getKey();
			Node classNode = (Node)entry.getValue();
			if (classNode != null) allCallsites.addAll(hc.getClassInsideCallsites(classNode));
			allCallsites.addAll(hc.getCallsitesByMethodName(hc.getMethodByReturnType(className)));
			allCallsites.addAll(ic.getClassGetMethodCallsites(className));
		}
	}
}
