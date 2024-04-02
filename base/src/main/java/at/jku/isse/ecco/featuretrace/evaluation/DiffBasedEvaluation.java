package at.jku.isse.ecco.featuretrace.evaluation;

import at.jku.isse.ecco.feature.Configuration;
import at.jku.isse.ecco.featuretrace.LogicUtils;
import org.logicng.datastructures.Assignment;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;

/**
 * The diff-based condition of a feature trace determines the overall condition.
 * The user-based condition is ignored.
 */
public class DiffBasedEvaluation implements EvaluationStrategy{

    @Override
    public boolean holds(Configuration configuration,
                         String userCondition,
                         String diffCondition){
        Assignment assignment = configuration.toAssignment(this.formulaFactory);
        if (diffCondition == null){ return false; }
        Formula diffFormula = LogicUtils.parseString(this.formulaFactory, diffCondition);
        return diffFormula.evaluate(assignment);
    }

    @Override
    public String getOverallConditionString(String userCondition, String diffCondition) {
        if (diffCondition == null){
            Formula falseFormula = this.formulaFactory.constant(false);
            return falseFormula.toString();
        } else {
            return diffCondition;
        }
    }
}
