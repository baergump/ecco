package at.jku.isse.ecco.featuretrace;

import at.jku.isse.ecco.dao.Persistable;
import at.jku.isse.ecco.feature.Configuration;
import at.jku.isse.ecco.featuretrace.evaluation.EvaluationStrategy;
import at.jku.isse.ecco.tree.Node;

public interface FeatureTrace extends Persistable {

    boolean holds(Configuration configuration, EvaluationStrategy evaluationStrategy);

    Node getNode();

    boolean containsUserCondition();

    boolean equalConditions(FeatureTrace featureTrace);

    void setDiffCondition(String diffConditionString);

    void setUserCondition(String userConditionString);

    void addUserCondition(String userCondition);

    String getUserConditionString();

    void fuseFeatureTrace(FeatureTrace featureTrace);

    @Override
    boolean equals(Object obj);

    @Override
    int hashCode();
}
