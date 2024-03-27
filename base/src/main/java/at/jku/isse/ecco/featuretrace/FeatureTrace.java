package at.jku.isse.ecco.featuretrace;

import at.jku.isse.ecco.dao.Persistable;
import at.jku.isse.ecco.feature.Configuration;
import at.jku.isse.ecco.featuretrace.evaluation.EvaluationStrategy;
import at.jku.isse.ecco.tree.Node;

// TODO: make it possible to transform arbitrary logical formulas of first order to feature traces
// TODO: (work directly with logical formulas and not sets of modules/features etc.)

public interface FeatureTrace extends Persistable {

    boolean holds(Configuration configuration, EvaluationStrategy evaluationStrategy);

    Node getNode();

    void setUserCondition(String userCondition);

    boolean conditionEquals(FeatureTrace featureTrace);

    @Override
    boolean equals(Object obj);

    @Override
    int hashCode();
}
