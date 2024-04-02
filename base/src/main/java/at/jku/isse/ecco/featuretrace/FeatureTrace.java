package at.jku.isse.ecco.featuretrace;

import at.jku.isse.ecco.dao.Persistable;
import at.jku.isse.ecco.feature.Configuration;
import at.jku.isse.ecco.featuretrace.evaluation.EvaluationStrategy;
import at.jku.isse.ecco.tree.Node;

public interface FeatureTrace extends Persistable {

    boolean holds(Configuration configuration, EvaluationStrategy evaluationStrategy);

    Node getNode();

    boolean containsUserCondition();

    boolean hasEqualConditions(FeatureTrace featureTrace);

    void setDiffCondition(String diffConditionString);

    void setUserCondition(String userConditionString);

    /**
     * Add a new condition that is independent of existing ones
     * @param userCondition
     */
    void addUserCondition(String userCondition);

    void buildUserConditionConjunction(String newCondition);

    String getUserConditionString();

    String getDiffConditionString();

    void fuseFeatureTrace(FeatureTrace featureTrace);

    String getOverallConditionString(EvaluationStrategy evaluationStrategy);

    @Override
    boolean equals(Object obj);

    @Override
    int hashCode();
}
