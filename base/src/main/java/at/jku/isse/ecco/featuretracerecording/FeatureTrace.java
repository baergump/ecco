package at.jku.isse.ecco.featuretracerecording;


import at.jku.isse.ecco.dao.Persistable;
import at.jku.isse.ecco.feature.Configuration;
import at.jku.isse.ecco.module.ModuleRevision;
import at.jku.isse.ecco.tree.Node;

import java.util.Collection;

/**
 *
 */
public interface FeatureTrace extends Persistable {
    boolean holds(Configuration configuration);

    Node getNode();

    Collection<ModuleRevision> getAllModuleRevisions();
}
