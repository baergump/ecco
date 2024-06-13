package mistake;

import at.jku.isse.ecco.featuretrace.FeatureTrace;
import at.jku.isse.ecco.repository.Repository;

import java.util.Collection;
import java.util.stream.Collectors;

public class ConditionSwapper implements MistakeStrategy{

    Collection<String> originalConditions;

    @Override
    public boolean createMistake(FeatureTrace trace) {
        try {
            String newCondition = this.getRandom(this.originalConditions);
            while (newCondition.equals(trace.getUserConditionString())) {
                newCondition = this.getRandom(this.originalConditions);
            }
            trace.setUserCondition(newCondition);
            return true;
        } catch (Exception e){
            System.out.println("ConditionSwapper failed to create mistake.");
            return false;
        }
    }

    @Override
    public void init(Repository.Op repository) {
        Collection<FeatureTrace> featureTraces = repository.getFeatureTraces();
        this.originalConditions = featureTraces.stream().map(FeatureTrace::getUserConditionString).collect(Collectors.toList());
    }
}
