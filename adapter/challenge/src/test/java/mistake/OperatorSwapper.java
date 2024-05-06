package mistake;

import at.jku.isse.ecco.featuretrace.FeatureTrace;

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
}
