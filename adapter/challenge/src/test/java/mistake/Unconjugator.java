package mistake;

import at.jku.isse.ecco.featuretrace.FeatureTrace;
import at.jku.isse.ecco.featuretrace.LogicUtils;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Variable;

import java.util.SortedSet;


public class Unconjugator implements MistakeStrategy{
    private String[] features;

    private FormulaFactory formulaFactory = new FormulaFactory();

    public Unconjugator(String[] features){
        this.features = features;
    }

    @Override
    public boolean createMistake(FeatureTrace trace) {
        String userCondition = trace.getUserConditionString();
        Formula conditionFormula = LogicUtils.parseString(this.formulaFactory, userCondition);
        Formula cnf = conditionFormula.cnf();
        SortedSet<Variable> variables = cnf.variables();
        if (variables.size() < 2){
            // there is no conjunction in the formula
            return false;
        }
        Variable toBeSwitched = this.getRandom(variables);
        String cnfString = cnf.toString();
        // TODO: true-constant?
        String newCondition = cnfString.replace(toBeSwitched.name(), "true");
        trace.setUserCondition(newCondition);
        return true;
    }
}
