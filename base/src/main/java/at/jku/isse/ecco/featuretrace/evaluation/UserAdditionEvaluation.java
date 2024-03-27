package at.jku.isse.ecco.featuretrace.evaluation;

import at.jku.isse.ecco.feature.Configuration;
import at.jku.isse.ecco.featuretrace.TraceCondition;
import org.logicng.datastructures.Assignment;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;

/**
 * Represents a disjunction of diff- and user-based condition.
 * (in a tree-composition, the user-based condition can lead to the addition of a node, but not a removal)
 */
public class UserAdditionEvaluation implements EvaluationStrategy{

    @Override
    public boolean holds (Configuration configuration, TraceCondition traceCondition){
        FormulaFactory factory = traceCondition.factory();
        assert(factory != null);
        Assignment assignment = configuration.toAssignment(factory);
        Formula formula = this.getOverallFormula(factory, traceCondition.getDiffCondition(), traceCondition.getUserCondition());
        return formula.evaluate(assignment);
    }

    private Formula getOverallFormula(FormulaFactory factory, Formula diffCondition, Formula userCondition){
        assert(diffCondition != null || userCondition != null);
        if (diffCondition != null && userCondition != null){
            return factory.or(diffCondition, userCondition);
        } else if (userCondition != null){
            return userCondition;
        } else {
            return diffCondition;
        }
    }
}
