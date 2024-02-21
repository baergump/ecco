package at.jku.isse.ecco.adapter.challenge.vevos;

import at.jku.isse.ecco.feature.Feature;
import at.jku.isse.ecco.feature.FeatureRevision;
import at.jku.isse.ecco.featuretracerecording.FeatureTraceCondition;
import at.jku.isse.ecco.module.Module;
import at.jku.isse.ecco.module.ModuleRevision;
import at.jku.isse.ecco.repository.Repository;
import at.jku.isse.ecco.storage.mem.featuretrace.MemFeatureTraceCondition;

import java.util.*;
import java.util.stream.Collectors;

// TODO: handle feature revisions!

public class LogicToModuleTransformer {

    private Repository.Op repository;

    public LogicToModuleTransformer(Repository.Op repository){
        this.repository = repository;
    }

    public FeatureTraceCondition transformLogicalConditionToFeatureTraceCondition(String logicalCondition){
        // elements in the expressions are features
        // string may contain not(!), and(&&), or(||)

        // module logic:
        // all positive features must be present
        // no negative features must be present

        // feature trace logic:
        // all positive modules must hold
        // no negative module is allowed to hold

        // transformation:
        // !a (a is a feature) -> module with negative feature a
        // a && b && ... (a and b are features) -> module with a and b as positive features
        // a || b || ... -> feature traces a, b, ... respectively
        // everything else is not supported yet

        logicalCondition = logicalCondition.trim();

        if (logicalCondition.startsWith("!")){
            return this.transformNegation(logicalCondition);
        } else if (logicalCondition.contains("&&")) {
            return this.transformConjunction(logicalCondition);
        } else if (logicalCondition.contains("||")) {
            return this.transformDisjunction(logicalCondition);
        } else {
            return this.transformFeature(logicalCondition);
        }
    }

    private FeatureTraceCondition transformNegation(String logicalCondition){
        logicalCondition = logicalCondition.substring(1);
        this.checkSingleFeatureString(logicalCondition);
        Feature feature = this.featureNameToFeature(logicalCondition);
        // create a feature-revision if it does not exist yet
        FeatureRevision featureRevision = this.getOrCreateLatestFeatureRevision(feature);

        Feature[] positiveFeatures = {feature};
        Feature[] negativeFeatures = {};
        FeatureRevision[] positiveFeatureRevisions = {featureRevision};

        ModuleRevision moduleRevision = this.createModuleRevision(positiveFeatures, negativeFeatures, positiveFeatureRevisions);
        Collection<ModuleRevision> positiveModuleRevisions = new HashSet<>();
        Collection<ModuleRevision> negativeModuleRevisions = new HashSet<>();
        negativeModuleRevisions.add(moduleRevision);
        return new MemFeatureTraceCondition(positiveModuleRevisions, negativeModuleRevisions);
    }

    private FeatureTraceCondition transformConjunction(String logicalCondition){
        if (!(logicalCondition.startsWith("(") && logicalCondition.endsWith(")"))){
            throw new RuntimeException("The structure of the given logical expression seems to be faulty: " + logicalCondition);
        }

        // remove parentheses
        String trimmedString = logicalCondition.substring(1, logicalCondition.length() - 1);
        String[] strings = trimmedString.split(" ");
        if (!this.unevenIndicesMatchString(strings, "&&")){
            throw new RuntimeException("The given format of logical expression is not supported yet: " + logicalCondition);
        }
        List<String> featureStrings = new LinkedList<>();
        // remove all "&&" strings
        for (int i = 0; i < strings.length; i++){
            if (i % 2 == 1){ continue; }
            featureStrings.add(strings[i]);
        }

        List<Feature> positiveFeatureList = featureStrings.stream()
                .map(this::featureNameToFeature)
                .collect(Collectors.toList());
        List<FeatureRevision> positiveFeatureRevisionList = positiveFeatureList.stream()
                .map(this::getOrCreateLatestFeatureRevision)
                .collect(Collectors.toList());

        Feature[] positiveFeatures = new Feature[featureStrings.size()];
        positiveFeatureList.toArray(positiveFeatures);
        Feature[] negativeFeatures = {};
        FeatureRevision[] positiveFeatureRevisions = new FeatureRevision[positiveFeatureRevisionList.size()];
        positiveFeatureRevisionList.toArray(positiveFeatureRevisions);

        ModuleRevision moduleRevision = this.createModuleRevision(positiveFeatures, negativeFeatures, positiveFeatureRevisions);
        Collection<ModuleRevision> positiveModuleRevisions = new HashSet<>();
        Collection<ModuleRevision> negativeModuleRevisions = new HashSet<>();
        positiveModuleRevisions.add(moduleRevision);
        return new MemFeatureTraceCondition(positiveModuleRevisions, negativeModuleRevisions);
    }

    private FeatureTraceCondition transformDisjunction(String logicalCondition){
        if (!(logicalCondition.startsWith("(") && logicalCondition.endsWith(")"))){
            throw new RuntimeException("The structure of the given logical expression seems to be faulty: " + logicalCondition);
        }

        // remove parentheses
        String trimmedString = logicalCondition.substring(1, logicalCondition.length() - 1);
        String[] strings = trimmedString.split(" ");
        if (!this.unevenIndicesMatchString(strings, "||")){
            throw new RuntimeException("The given format of logical expression is not supported yet: " + logicalCondition);
        }
        List<String> featureStrings = new LinkedList<>();
        // remove all "||" strings
        for (int i = 0; i < strings.length; i++){
            if (i % 2 == 1){ continue; }
            featureStrings.add(strings[i]);
        }

        Collection<ModuleRevision> positiveModuleRevisions = new HashSet<>();
        Collection<ModuleRevision> negativeModuleRevisions = new HashSet<>();

        for (String featureString : featureStrings){
            Feature feature = this.featureNameToFeature(featureString);
            FeatureRevision featureRevision = this.getOrCreateLatestFeatureRevision(feature);

            Feature[] positiveFeatures = {feature};
            Feature[] negativeFeatures = {};
            FeatureRevision[] positiveFeatureRevisions = {featureRevision};

            ModuleRevision moduleRevision = this.createModuleRevision(positiveFeatures, negativeFeatures, positiveFeatureRevisions);
            positiveModuleRevisions.add(moduleRevision);
        }

        return new MemFeatureTraceCondition(positiveModuleRevisions, negativeModuleRevisions);
    }

    private boolean unevenIndicesMatchString(String[] stringArray, String matchingString){
        for (int i = 0; i < stringArray.length; i++){
            if (i % 2 == 0){ continue; }
            if (!stringArray[i].equals(matchingString)){
                return false;
            }
        }
        return true;
    }

    private FeatureTraceCondition transformFeature(String featureName) {
        Feature feature = this.featureNameToFeature(featureName);
        FeatureRevision featureRevision = this.getOrCreateLatestFeatureRevision(feature);

        Feature[] positiveFeatures = {feature};
        Feature[] negativeFeatures = {};
        FeatureRevision[] positiveFeatureRevisions = {featureRevision};

        ModuleRevision moduleRevision = this.createModuleRevision(positiveFeatures, negativeFeatures, positiveFeatureRevisions);
        Collection<ModuleRevision> positiveModuleRevisions = new HashSet<>();
        Collection<ModuleRevision> negativeModuleRevisions = new HashSet<>();
        positiveModuleRevisions.add(moduleRevision);
        return new MemFeatureTraceCondition(positiveModuleRevisions, negativeModuleRevisions);
    }

    private Feature featureNameToFeature(String featureName){
        this.checkSingleFeatureString(featureName);

        // TODO: is a transaction necessary here?

        Collection<Feature> features = this.repository.getFeaturesByName(featureName);
        if (features.size() > 1){
            // TODO: (possibly) handle multiple existing features with the same name
            throw new RuntimeException(String.format("There are more than one features with the given name, which is not yet supported for feature traces (name: %s)", featureName));
        } else if (features.isEmpty()){
            return this.repository.addFeature(UUID.randomUUID().toString(), featureName);
        }  else {
            return features.iterator().next();
        }
    }

    private FeatureRevision getOrCreateLatestFeatureRevision(Feature feature){
        FeatureRevision featureRevision = feature.getLatestRevision();
        if (featureRevision == null){
            featureRevision = feature.addRevision(UUID.randomUUID().toString());
        }
        return featureRevision;
    }

    private ModuleRevision createModuleRevision(Feature[] positiveFeatures,
                                                Feature[] negativeFeatures,
                                                FeatureRevision[] positiveFeatureRevisions){
        Module module = this.repository.getModule(positiveFeatures, negativeFeatures);
        if (module == null) { module = this.repository.addModule(positiveFeatures, negativeFeatures); }
        ModuleRevision moduleRevision = module.getRevision(positiveFeatureRevisions, negativeFeatures);
        if (moduleRevision == null) { moduleRevision = module.addRevision(positiveFeatureRevisions, negativeFeatures); }
        return moduleRevision;
    }

    private void checkSingleFeatureString(String featureName){
        if (featureName.contains(" ")){
            throw new RuntimeException("Condition is supposed to be a single feature, but contains spaces: " + featureName);
        }
    }
}
