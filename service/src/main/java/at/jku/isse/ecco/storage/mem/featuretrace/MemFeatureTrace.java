package at.jku.isse.ecco.storage.mem.featuretrace;

import at.jku.isse.ecco.feature.Configuration;
import at.jku.isse.ecco.featuretrace.FeatureTrace;
import at.jku.isse.ecco.featuretrace.TraceCondition;
import at.jku.isse.ecco.featuretrace.evaluation.EvaluationStrategy;
import at.jku.isse.ecco.tree.Node;

import java.util.Objects;

public class MemFeatureTrace implements FeatureTrace {

    // TODO: parse string to get condition

    private Node node;
    private TraceCondition traceCondition;

    public MemFeatureTrace(Node node){
        this.node = node;
    }

    @Override
    public boolean holds(Configuration configuration, EvaluationStrategy evaluationStrategy){
        return evaluationStrategy.holds(configuration, this.traceCondition);
    }

    @Override
    public Node getNode() {
        return this.node;
    }

    @Override
    public void setUserCondition(String userCondition) {
        // TODO
    }

    @Override
    public boolean conditionEquals(FeatureTrace featureTrace){
        if (this == featureTrace) return true;
        if (!(featureTrace instanceof MemFeatureTrace)) return false;
        if (!(this.traceCondition.equals(((MemFeatureTrace) featureTrace).traceCondition))) return false;
        return true;
    }

    @Override
    public boolean equals(Object o){
        if (this == o) return true;
        if (!(o instanceof MemFeatureTrace)) return false;
        if (!(this.node.equals(((MemFeatureTrace) o).getNode()))) return false;
        if (!(this.traceCondition.equals(((MemFeatureTrace) o).traceCondition))) return false;
        return true;
    }

    @Override
    public int hashCode(){
        return Objects.hash(this.node, this.traceCondition);
    }
}
