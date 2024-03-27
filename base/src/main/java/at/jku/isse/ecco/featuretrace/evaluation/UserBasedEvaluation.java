package at.jku.isse.ecco.featuretrace.evaluation;

import at.jku.isse.ecco.feature.Configuration;
import at.jku.isse.ecco.featuretrace.TraceCondition;
import org.logicng.datastructures.Assignment;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;

/**
 * The user-based condition of a feature trace determines the overall condition.
 * The diff-based condition is ignored.
 */
public class UserBasedEvaluation implements EvaluationStrategy{
    @Override
    public boolean holds(Configuration configuration, TraceCondition traceCondition) {
        FormulaFactory factory = traceCondition.factory();
        assert(factory != null);
        Assignment assignment = configuration.toAssignment(factory);
        Formula userCondition = traceCondition.getUserCondition();
        assert(userCondition != null);
        return userCondition.evaluate(assignment);
    }
}
