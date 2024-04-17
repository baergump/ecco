package at.jku.isse.ecco.storage.mem.repository;

import at.jku.isse.ecco.featuretrace.FeatureTrace;
import at.jku.isse.ecco.tree.Node;

import java.util.Set;

public class FeatureTraceCollectorVisitor implements Node.NodeVisitor {

    private Set<FeatureTrace> featureTraces;

    @Override
    public void visit(Node node) {
        Node.Op nodeOp = (Node.Op) node;
        FeatureTrace featureTrace = nodeOp.getFeatureTrace();
        if (featureTrace.containsUserCondition()) {
            this.featureTraces.add(featureTrace);
        }
    }

    public Set<FeatureTrace> getFeatureTraces() {
        return this.featureTraces;
    }
}
