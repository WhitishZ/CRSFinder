package element;

import java.util.List;
import org.w3c.dom.Node;

public class CallsiteOptionsPair {
    private Node callsite;
    private List<String> optionNames;
    public CallsiteOptionsPair(Node callsite, List<String> optionNames) {
        super();
        this.callsite = callsite;
        this.optionNames = optionNames;
    }
    public Node getCallsite() {
        return callsite;
    }
    public void setCallsite(Node callsite) {
        this.callsite = callsite;
    }
    public List<String> getOptionNames() {
        return optionNames;
    }
    public void setOptionNames(List<String> optionNames) {
        this.optionNames = optionNames;
    }
}
