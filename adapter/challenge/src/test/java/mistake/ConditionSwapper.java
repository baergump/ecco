package mistake;

import at.jku.isse.ecco.featuretrace.FeatureTrace;

import java.util.Collection;
import java.util.stream.Collectors;

public class ConditionSwapper implements MistakeStrategy{

    Collection<String> originalConditions;

    public ConditionSwapper(Collection<FeatureTrace> featureTraces){
        this.originalConditions = featureTraces.stream().map(FeatureTrace::getUserConditionString).collect(Collectors.toList());
    }

    @Override
    public boolean createMistake(FeatureTrace trace) {
        String newCondition = this.getRandom(this.originalConditions);
        while(newCondition.equals(trace.getUserConditionString())){
            newCondition = this.getRandom(this.originalConditions);
        }
        trace.setUserCondition(newCondition);
        return true;
    }
}
