package at.jku.isse.ecco.storage.mem.featuretrace;

import at.jku.isse.ecco.featuretrace.FeatureTrace;
import at.jku.isse.ecco.featuretrace.FeatureTraceFactory;
import at.jku.isse.ecco.featuretrace.TraceCondition;
import at.jku.isse.ecco.tree.Node;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.io.parsers.ParserException;

public class MemFeatureTraceFactory implements FeatureTraceFactory {

    private FormulaFactory formulaFactory;

    public MemFeatureTraceFactory(){
        this.formulaFactory = new FormulaFactory();
    }

    @Override
    public FeatureTrace createFeatureTrace(Node node) {
        return new MemFeatureTrace(node);
    }

    @Override
    public FeatureTrace addUserCondition(FeatureTrace featureTrace, String userConditionString) {
        assert(featureTrace instanceof MemFeatureTrace);
        MemFeatureTrace memFeatureTrace = (MemFeatureTrace) featureTrace;

        TraceCondition traceCondition= memFeatureTrace.getTraceCondition();
        Formula userCondition = this.parseCondition(memFeatureTrace.getTraceCondition().getUserCondition());
        Formula givenUserCondition = this.parseCondition(userConditionString);
        Formula newUserCondition;
        if (userCondition == null){
            newUserCondition = givenUserCondition;
        } else {
            newUserCondition = this.formulaFactory.or(userCondition, givenUserCondition);
        }
        traceCondition.setUserCondition(newUserCondition.toString());
        return memFeatureTrace;
    }

    private Formula parseCondition(String stringCondition){
        try {
            return this.formulaFactory.parse(stringCondition);
        } catch (ParserException e){
            throw new RuntimeException(String.format("Parsing of string %s failed!", stringCondition));
        }
    }

}
