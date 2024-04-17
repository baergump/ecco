package at.jku.isse.ecco.storage.mem.repository;

import at.jku.isse.ecco.featuretrace.FeatureTrace;
import at.jku.isse.ecco.tree.Node;

import java.util.HashSet;
import java.util.Set;

public class FeatureTraceCollectorVisitor implements Node.NodeVisitor {

    private Set<FeatureTrace> featureTraces = new HashSet<>();

    @Override
    public void visit(Node node) {
        Node.Op nodeOp = (Node.Op) node;
        FeatureTrace featureTrace = nodeOp.getFeatureTrace();
        if (featureTrace == null) { return; }
        if (featureTrace.containsUserCondition()) {
            this.featureTraces.add(featureTrace);
        }
    }

    public Set<FeatureTrace> getFeatureTraces() {
        return this.featureTraces;
    }
}
