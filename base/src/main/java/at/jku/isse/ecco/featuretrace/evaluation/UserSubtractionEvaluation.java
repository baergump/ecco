package at.jku.isse.ecco.featuretrace.evaluation;

import at.jku.isse.ecco.feature.Configuration;
import at.jku.isse.ecco.featuretrace.TraceCondition;
import org.logicng.datastructures.Assignment;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;

/**
 * Represents a conjunction of diff- and user-based condition.
 * (in a tree-composition, the user-based condition can lead to the removal of a node, but not an addition)
 * (if there is no diff-based condition, it evaluates to false)
 */
public class UserSubtractionEvaluation implements EvaluationStrategy{

    @Override
    public boolean holds (Configuration configuration, TraceCondition traceCondition){
        FormulaFactory factory = traceCondition.factory();
        assert(factory != null);
        Formula diffCondition = traceCondition.getDiffCondition();
        Formula userCondition = traceCondition.getUserCondition();
        Assignment assignment = configuration.toAssignment(factory);
        Formula formula;

        if (diffCondition == null){
            return false;
        } else if (userCondition != null){
            formula = factory.and(diffCondition, userCondition);
        } else {
            formula = diffCondition;
        }

        return formula.evaluate(assignment);
    }
}
