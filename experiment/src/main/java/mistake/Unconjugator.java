package mistake;

import at.jku.isse.ecco.featuretrace.FeatureTrace;
import at.jku.isse.ecco.featuretrace.LogicUtils;
import at.jku.isse.ecco.repository.Repository;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Variable;

import java.util.SortedSet;


public class Unconjugator implements MistakeStrategy{

    private FormulaFactory formulaFactory = new FormulaFactory();

    @Override
    public boolean createMistake(FeatureTrace trace) {
        try {
            String userCondition = trace.getUserConditionString();
            Formula conditionFormula = LogicUtils.parseString(this.formulaFactory, userCondition);
            Formula cnf = conditionFormula.cnf();
            SortedSet<Variable> variables = cnf.variables();
            if (variables.size() < 2) {
                // there is no conjunction in the formula
                return false;
            }
            Variable toBeSwitched = this.getRandom(variables);
            String cnfString = cnf.toString();
            String newCondition = cnfString.replace(toBeSwitched.name(), formulaFactory.verum().toString());
            trace.setUserCondition(newCondition);
            return true;
        } catch (Exception e){
            System.out.println("Unconjugator failed to create mistake.");
            return false;
        }
    }

    @Override
    public void init(Repository.Op repository) {

    }
}
