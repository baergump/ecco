package at.jku.isse.ecco.featuretrace.evaluation;

import at.jku.isse.ecco.feature.Configuration;
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
    public boolean holds (Configuration configuration,
                          String userCondition,
                          String diffCondition){
        Assignment assignment = configuration.toAssignment(this.formulaFactory);
        Formula formula;
        Formula userFormula = this.parseString(userCondition);
        Formula diffFormula = this.parseString(diffCondition);
        if (diffFormula == null){
            return false;
        } else if (userFormula != null){
            formula = this.formulaFactory.and(diffFormula, userFormula);
        } else {
            formula = diffFormula;
        }

        return formula.evaluate(assignment);
    }
}
