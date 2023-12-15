package at.jku.isse.ecco.featuretracerecording;

import at.jku.isse.ecco.dao.Persistable;
import at.jku.isse.ecco.feature.Configuration;
import at.jku.isse.ecco.module.Module;
import at.jku.isse.ecco.module.ModuleRevision;

import java.util.Collection;
import java.util.Map;

/**
 * A condition in order to check feature traces to be applied for given configurations.
 * In contrast to at.jku.isse.ecco.module.Condition (in order to hold, one of the contained modules must hold),
 * all positive modules must hold and no negative module must hold for a condition to hold.
 *
 * There must not be multiple revisions of the same Module.
 */
public interface FeatureTraceCondition extends Persistable {

    boolean holds(Configuration configuration);

    Collection<ModuleRevision> getAllModuleRevisions();
}
