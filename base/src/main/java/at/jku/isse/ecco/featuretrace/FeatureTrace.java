package at.jku.isse.ecco.featuretrace;

import at.jku.isse.ecco.dao.Persistable;
import at.jku.isse.ecco.feature.Configuration;
import at.jku.isse.ecco.featuretrace.evaluation.EvaluationStrategy;
import at.jku.isse.ecco.module.ModuleRevision;
import at.jku.isse.ecco.tree.Node;
import org.logicng.formulas.FormulaFactory;

public interface FeatureTrace extends Persistable {

    boolean holds(Configuration configuration, EvaluationStrategy evaluationStrategy);

    Node getNode();

    boolean ContainsUserCondition();

    boolean equalConditions(FeatureTrace featureTrace);

    @Override
    boolean equals(Object obj);

    @Override
    int hashCode();
}
