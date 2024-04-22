package utils;

import at.jku.isse.ecco.featuretrace.FeatureTrace;
import at.jku.isse.ecco.featuretrace.LogicUtils;
import at.jku.isse.ecco.repository.Repository;
import at.jku.isse.ecco.tree.Node;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class MistakeCreator {

    // Swap a Feature
    // FA & FB --> FA (and vice versa)
    // Swap with the Condition of another Artifact
    // Change Operands
    private static final String[] FEATURES = {"STATEDIAGRAM", "ACTIVITYDIAGRAM", "USECASEDIAGRAM", "COLLABORATIONDIAGRAM",
            "DEPLOYMENTDIAGRAM", "SEQUENCEDIAGRAM", "COGNITIVE", "LOGGING"};


    private FormulaFactory formulaFactory = new FormulaFactory();

    public void createMistakePercentage(Repository repository, int percentage){
        if (percentage < 0 || percentage > 100){
            throw new RuntimeException(String.format("Percentage of feature traces is invalid (%d).", percentage));
        }
        Collection<FeatureTrace> traces = repository.getFeatureTraces();
        int noOfMistakes = (traces.size() * percentage) / 100;
        List<FeatureTrace> featureTraceList = new ArrayList<>(traces);
        Collections.shuffle(featureTraceList);
        Iterator<FeatureTrace> iterator = featureTraceList.stream().iterator();
        int attempts = noOfMistakes;
        for (int i = 1; i <= attempts; i++){
            if (!iterator.hasNext()){
                throw new RuntimeException("Failed to create enough mistakes!");
            }
            FeatureTrace trace = iterator.next();
            if (!this.createMistake(trace)){
                attempts++;
            }
        }
    }

    public boolean createMistake(FeatureTrace trace){
        return this.switchFeature(trace);
    }

    private boolean switchFeature(FeatureTrace trace){
        // get a literal(?) / variable(?) that is not "true" and switch it randomly
        String userConditionString = trace.getUserConditionString();
        Formula userCondition = LogicUtils.parseString(this.formulaFactory, userConditionString);
        SortedSet<Literal> literals = userCondition.literals();
        SortedSet<Variable> variables = userCondition.variables();
        return true;
    }

    private String getRandomOtherFeature(String feature){
        int featureNumber = MistakeCreator.FEATURES.length;
        assert(MistakeCreator.FEATURES.length > 1);
        String otherFeature = feature;
        while (otherFeature.equals(feature)){
            int randomIndex = ThreadLocalRandom.current().nextInt(0, featureNumber);
            otherFeature = MistakeCreator.FEATURES[randomIndex];
        }
        return otherFeature;
    }
}
