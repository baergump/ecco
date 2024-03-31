package at.jku.isse.ecco.featuretrace.evaluation;

import at.jku.isse.ecco.feature.Configuration;
import org.logicng.datastructures.Assignment;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;

/**
 * Represents a disjunction of diff- and user-based condition.
 * (in a tree-composition, the user-based condition can lead to the addition of a node, but not a removal)
 */
public class UserAdditionEvaluation implements EvaluationStrategy{

    @Override
    public boolean holds (Configuration configuration,
                          String userCondition,
                          String diffCondition){
        Assignment assignment = configuration.toAssignment(formulaFactory);
        Formula userFormula = this.parseString(userCondition);
        Formula diffFormula = this.parseString(diffCondition);
        Formula formula = this.getOverallFormula(diffFormula, userFormula);
        return formula.evaluate(assignment);
    }

    private Formula getOverallFormula(Formula diffCondition, Formula userCondition){
        assert(diffCondition != null || userCondition != null);
        if (diffCondition != null && userCondition != null){
            return this.formulaFactory.or(diffCondition, userCondition);
        } else if (userCondition != null){
            return userCondition;
        } else {
            return diffCondition;
        }
    }
}
