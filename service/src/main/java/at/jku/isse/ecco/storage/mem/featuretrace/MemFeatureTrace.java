package at.jku.isse.ecco.storage.mem.featuretrace;

import at.jku.isse.ecco.feature.Configuration;
import at.jku.isse.ecco.featuretrace.FeatureTrace;
import at.jku.isse.ecco.featuretrace.TraceCondition;
import at.jku.isse.ecco.featuretrace.evaluation.EvaluationStrategy;
import at.jku.isse.ecco.tree.Node;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.io.parsers.ParserException;

import java.util.Objects;

public class MemFeatureTrace implements FeatureTrace {

    // TODO: parse string to get condition

    private Node node;
    private TraceCondition traceCondition;


    public MemFeatureTrace(Node node){
        this.node = node;
        this.traceCondition = new TraceCondition();
    }

    public TraceCondition getTraceCondition(){
        return this.traceCondition;
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
    public boolean ContainsUserCondition() {
        String userCondition = this.traceCondition.getUserCondition();
        return (userCondition != null);
    }

    @Override
    public boolean equalConditions(FeatureTrace featureTrace){
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
