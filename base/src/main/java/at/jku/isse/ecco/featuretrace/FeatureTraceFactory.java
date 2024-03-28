package at.jku.isse.ecco.featuretrace;

import at.jku.isse.ecco.tree.Node;

public interface FeatureTraceFactory {

    FeatureTrace createFeatureTrace(Node node);

    FeatureTrace addUserCondition(FeatureTrace featureTrace, String userCondition);

}
