package at.jku.isse.ecco.featuretracerecording;


import at.jku.isse.ecco.dao.Persistable;
import at.jku.isse.ecco.feature.Configuration;
import at.jku.isse.ecco.module.ModuleRevision;
import at.jku.isse.ecco.tree.Node;

import java.util.Collection;

// TODO: make it possible to transform arbitrary logical formulas of first order to feature traces
// TODO: (work directly with logical formulas and not sets of modules/features etc.)

/**
 *
 */
public interface FeatureTrace extends Persistable {
    boolean holds(Configuration configuration);

    Node getNode();

    Collection<ModuleRevision> getAllModuleRevisions();

    @Override
    boolean equals(Object obj);

    @Override
    int hashCode();
}
