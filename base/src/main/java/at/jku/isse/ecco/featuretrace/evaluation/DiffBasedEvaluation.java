package at.jku.isse.ecco.featuretrace.evaluation;

import at.jku.isse.ecco.feature.Configuration;
import org.logicng.datastructures.Assignment;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;

/**
 * The diff-based condition of a feature trace determines the overall condition.
 * The user-based condition is ignored.
 */
public class DiffBasedEvaluation implements EvaluationStrategy{

    private final FormulaFactory formulaFactory = new FormulaFactory();

    @Override
    public boolean holds(Configuration configuration,
                         String userCondition,
                         String diffCondition){
        Assignment assignment = configuration.toAssignment(this.formulaFactory);
        assert(diffCondition != null);
        Formula diffFormula = this.parseString(diffCondition);
        return diffFormula.evaluate(assignment);
    }
}
