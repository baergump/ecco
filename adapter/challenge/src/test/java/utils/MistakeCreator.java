package utils;

import at.jku.isse.ecco.featuretrace.FeatureTrace;

import java.util.concurrent.ThreadLocalRandom;

public class MistakeCreator {

    // switch feature
    // combination instead of feature
    // feature instead of combination
    private static final String[] FEATURES = {"STATEDIAGRAM", "ACTIVITYDIAGRAM", "USECASEDIAGRAM", "COLLABORATIONDIAGRAM",
            "DEPLOYMENTDIAGRAM", "SEQUENCEDIAGRAM", "COGNITIVE", "LOGGING"};


    public static void createMistake(FeatureTrace trace){
        //
    }

    private static void switchFeature(FeatureTrace trace){
        // get a literal(?) / variable(?) that is not "true" and switch it randomly
    }

    private static String getRandomOtherFeature(String feature){
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
