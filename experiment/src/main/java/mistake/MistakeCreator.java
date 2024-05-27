package mistake;

import at.jku.isse.ecco.featuretrace.FeatureTrace;
import at.jku.isse.ecco.repository.Repository;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class MistakeCreator {

    private static final String[] FEATURES = {"STATEDIAGRAM", "ACTIVITYDIAGRAM", "USECASEDIAGRAM", "COLLABORATIONDIAGRAM",
            "DEPLOYMENTDIAGRAM", "SEQUENCEDIAGRAM", "COGNITIVE", "LOGGING"};

    private MistakeStrategy mistakeStrategy;

    public MistakeCreator(MistakeStrategy mistakeStrategy) {
        this.mistakeStrategy = mistakeStrategy;
    }

    public void createMistakePercentage(Repository.Op repository, int percentage){
        this.mistakeStrategy.init(repository);
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
            if (!this.mistakeStrategy.createMistake(trace)){
                attempts++;
            }
        }
    }

    public MistakeStrategy getMistakeStrategy(){
        return this.mistakeStrategy;
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
