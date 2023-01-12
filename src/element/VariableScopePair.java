package element;

import org.w3c.dom.Node;

public class VariableScopePair {
	private String variableName;
	private Node scope;
	
	public VariableScopePair(String variableName, Node scope) {
		super();
		this.variableName = variableName;
		this.scope = scope;
	}

	public String getVariableName() {
		return variableName;
	}

	public void setVariableName(String variableName) {
		this.variableName = variableName;
	}

	public Node getScope() {
		return scope;
	}

	public void setScope(Node scope) {
		this.scope = scope;
	}
	
}
