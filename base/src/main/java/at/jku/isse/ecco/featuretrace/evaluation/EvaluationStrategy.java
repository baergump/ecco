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

    default Formula parseString(String string){
        if (string == null){
            return null;
        }
        try{
            return this.formulaFactory.parse(string);
        } catch (ParserException e) {
            throw new RuntimeException(e);
        }
    }
}
