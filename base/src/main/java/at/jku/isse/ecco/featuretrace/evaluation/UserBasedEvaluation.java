package at.jku.isse.ecco.featuretrace.evaluation;

import at.jku.isse.ecco.feature.Configuration;
import org.logicng.datastructures.Assignment;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;

/**
 * The user-based condition of a feature trace determines the overall condition.
 * The diff-based condition is ignored.
 */
public class UserBasedEvaluation implements EvaluationStrategy{

    @Override
    public boolean holds(Configuration configuration,
                         String userCondition,
                         String diffCondition) {
        Assignment assignment = configuration.toAssignment(this.formulaFactory);
        assert(userCondition != null);
        Formula userFormula = this.parseString(userCondition);
        return userFormula.evaluate(assignment);
    }
}
