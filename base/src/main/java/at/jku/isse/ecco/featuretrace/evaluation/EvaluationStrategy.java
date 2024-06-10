package at.jku.isse.ecco.featuretrace.evaluation;

import at.jku.isse.ecco.feature.Configuration;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.io.parsers.ParserException;

public interface EvaluationStrategy {

    FormulaFactory formulaFactory = new FormulaFactory();

    boolean holds(Configuration configuration,
                  String userCondition,
                  String diffCondition);

    String getOverallConditionString(String userCondition, String diffCondition);

    String getStrategyName();
}
