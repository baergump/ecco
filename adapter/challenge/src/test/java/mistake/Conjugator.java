package mistake;

import at.jku.isse.ecco.featuretrace.FeatureTrace;
import at.jku.isse.ecco.featuretrace.LogicUtils;
import at.jku.isse.ecco.repository.Repository;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Variable;

import java.util.SortedSet;

public class Conjugator implements MistakeStrategy{

    private String[] features;

    private FormulaFactory formulaFactory = new FormulaFactory();

    public Conjugator(String[] features){
        this.features = features;
    }

    @Override
    public boolean createMistake(FeatureTrace trace) {
        String userConditionString = trace.getUserConditionString();
        Formula userCondition = LogicUtils.parseString(this.formulaFactory, userConditionString);
        SortedSet<Variable> variables = userCondition.variables();
        Variable variable = this.getRandom(variables);
        String randomFeature = this.features[(int) (this.features.length * Math.random())];
        while (variable.toString().equals(randomFeature)){
            randomFeature = this.features[(int) (this.features.length * Math.random())];
        }
        String conjugation = variable + " & " + randomFeature;
        trace.setUserCondition(userConditionString.replace(variable.toString(), conjugation));
        return true;
    }

    @Override
    public void init(Repository.Op repository) {

    }
}
