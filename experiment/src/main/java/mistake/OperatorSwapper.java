package mistake;

import at.jku.isse.ecco.featuretrace.FeatureTrace;
import at.jku.isse.ecco.repository.Repository;

public class OperatorSwapper implements MistakeStrategy{
    @Override
    public boolean createMistake(FeatureTrace trace) {
        String oldCondition = trace.getUserConditionString();
        String newCondition;
        if (oldCondition.contains("|")){
            newCondition = oldCondition.replaceFirst("\\|", "&");
        } else if (oldCondition.contains("&")){
            newCondition = oldCondition.replaceFirst("&", "|");
        } else {
            return false;
        }
        trace.setUserCondition(newCondition);
        return true;
    }

    @Override
    public void init(Repository.Op repository) {

    }
}
