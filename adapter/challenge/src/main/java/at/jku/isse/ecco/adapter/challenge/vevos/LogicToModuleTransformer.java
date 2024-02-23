package at.jku.isse.ecco.adapter.challenge.vevos;

import at.jku.isse.ecco.feature.Feature;
import at.jku.isse.ecco.feature.FeatureRevision;
import at.jku.isse.ecco.featuretracerecording.FeatureTraceCondition;
import at.jku.isse.ecco.module.Module;
import at.jku.isse.ecco.module.ModuleRevision;
import at.jku.isse.ecco.repository.Repository;
import at.jku.isse.ecco.storage.mem.featuretrace.MemFeatureTraceCondition;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// TODO: handle feature revisions!

public class LogicToModuleTransformer {

    private Repository.Op repository;

    public LogicToModuleTransformer(Repository.Op repository){
        this.repository = repository;
    }

    private enum ExpressionType{
        AND,
        OR,
        NEG,
        SIMPLE
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
        // && -> put both operands in one module
        // || -> put both operands in its own module
        // ((a || b) && c) == ((a && c) || (b && c))
        // ((a && b) || c)

        //((STATEDIAGRAM || ACTIVITYDIAGRAM) && ACTIVITYDIAGRAM && STATEDIAGRAM)

        logicalCondition = logicalCondition.trim();

        if (logicalCondition.startsWith("!")){
            return this.transformNegation(logicalCondition);
        } else if (logicalCondition.startsWith("((")){
            // TODO: replace hacky solution with proper one
            return this.hackySolution(logicalCondition);
        } else if (logicalCondition.contains("&&")) {
            return this.transformConjunction(logicalCondition);
        } else if (logicalCondition.contains("||")) {
            return this.transformDisjunction(logicalCondition);
        } else {
            return this.transformFeature(logicalCondition);
        }
    }

    private FeatureTraceCondition hackySolution(String logicalCondition){
        // TODO: make a regex check
        // structure ((a || b) && c && ...)
        // remove parentheses
        String trimmedString = logicalCondition.replace("(", "");
        trimmedString = trimmedString.replace(")", "");
        // split by space
        String[] strings = trimmedString.split(" ");
        // (check if first operator is "||" and all the others are "&&"
        if (!strings[1].equals("||")){
            throw new RuntimeException(String.format("Structure of logical expression not supported: %s", logicalCondition));
        }
        // remove operators
        // TODO: refactor this monstrosity
        Set<String> andFeatureStrings = new HashSet<>();
        Set<String> orFeatureStrings = new HashSet<>();
        for (int i = 0; i < strings.length; i++){
            if (i == 0 || i == 2){
                orFeatureStrings.add(strings[i]);
                continue;
            } else if (i == 1){
                continue;
            } else if (i % 2 == 1){
                if (!strings[i].equals("&&")){
                    throw new RuntimeException(String.format("Structure of logical expression not supported: %s", logicalCondition));
                }
                continue;
            }
            andFeatureStrings.add(strings[i]);
        }
        // make modules for the first two operators and put one feature in each positive feature collection
        Set<String> featureCombination1 = new HashSet<>();
        Set<String> featureCombination2 = new HashSet<>();

        Iterator<String> iterator = orFeatureStrings.iterator();
        featureCombination1.add(iterator.next());
        featureCombination2.add(iterator.next());
        featureCombination1.addAll(andFeatureStrings);
        featureCombination2.addAll(andFeatureStrings);

        // put all other features in each module in positive feature collection
        ModuleRevision moduleRevision1 = positiveFeatureNamesToModuleRevision(featureCombination1);
        ModuleRevision moduleRevision2 = positiveFeatureNamesToModuleRevision(featureCombination2);
        Collection<ModuleRevision> positiveModuleRevisions = new HashSet<>();
        Collection<ModuleRevision> negativeModuleRevisions = new HashSet<>();
        positiveModuleRevisions.add(moduleRevision1);
        positiveModuleRevisions.add(moduleRevision2);
        return new MemFeatureTraceCondition(positiveModuleRevisions, negativeModuleRevisions);
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
            // throw new RuntimeException("The given format of logical expression is not supported yet: " + logicalCondition);
            System.out.println("The given format of logical expression is not supported yet: " + logicalCondition);
            return null;
        }
        Set<String> featureStrings = new HashSet<>();
        // remove all "&&" strings
        for (int i = 0; i < strings.length; i++){
            if (i % 2 == 1){ continue; }
            featureStrings.add(strings[i]);
        }

        ModuleRevision moduleRevision = positiveFeatureNamesToModuleRevision(featureStrings);
        Collection<ModuleRevision> positiveModuleRevisions = new HashSet<>();
        Collection<ModuleRevision> negativeModuleRevisions = new HashSet<>();
        positiveModuleRevisions.add(moduleRevision);
        return new MemFeatureTraceCondition(positiveModuleRevisions, negativeModuleRevisions);
    }

    private ModuleRevision positiveFeatureNamesToModuleRevision(Set<String> featureNames){
        List<Feature> positiveFeatureList = featureNames.stream()
                .map(this::featureNameToFeature)
                .collect(Collectors.toList());
        List<FeatureRevision> positiveFeatureRevisionList = positiveFeatureList.stream()
                .map(this::getOrCreateLatestFeatureRevision)
                .collect(Collectors.toList());

        Feature[] positiveFeatures = new Feature[featureNames.size()];
        positiveFeatureList.toArray(positiveFeatures);
        Feature[] negativeFeatures = {};
        FeatureRevision[] positiveFeatureRevisions = new FeatureRevision[positiveFeatureRevisionList.size()];
        positiveFeatureRevisionList.toArray(positiveFeatureRevisions);

        return this.createModuleRevision(positiveFeatures, negativeFeatures, positiveFeatureRevisions);
    }

    private FeatureTraceCondition transformDisjunction(String logicalCondition){
        if (!(logicalCondition.startsWith("(") && logicalCondition.endsWith(")"))){
            //throw new RuntimeException("The structure of the given logical expression seems to be faulty: " + logicalCondition);
            System.out.println("The given format of logical expression is not supported yet: " + logicalCondition);
            return null;
        }

        // remove parentheses
        String trimmedString = logicalCondition.substring(1, logicalCondition.length() - 1);
        String[] strings = trimmedString.split(" ");
        if (!this.unevenIndicesMatchString(strings, "||")){
            //throw new RuntimeException("The given format of logical expression is not supported yet: " + logicalCondition);
            System.out.println("The given format of logical expression is not supported yet: " + logicalCondition);
            return null;
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
