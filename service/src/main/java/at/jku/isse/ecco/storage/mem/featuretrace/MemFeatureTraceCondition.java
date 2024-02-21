package at.jku.isse.ecco.storage.mem.featuretrace;

import at.jku.isse.ecco.feature.Configuration;
import at.jku.isse.ecco.featuretracerecording.FeatureTraceCondition;
import at.jku.isse.ecco.module.ModuleRevision;

import java.util.Collection;
import java.util.HashSet;

public class MemFeatureTraceCondition implements FeatureTraceCondition {
    // modules that must hold
    private final Collection<ModuleRevision> positiveModuleRevisions;
    // modules that must not hold
    private final Collection<ModuleRevision> negativeModuleRevisions;

    public MemFeatureTraceCondition(Collection<ModuleRevision> positiveModules, Collection<ModuleRevision> negativeModules){
        // TODO: check for multiple revisions of the same Module
        this.positiveModuleRevisions = positiveModules;
        this.negativeModuleRevisions = negativeModules;
    }

    @Override
    public boolean holds(Configuration configuration){
        for(ModuleRevision moduleRevision : negativeModuleRevisions){
            if (moduleRevision.holds(configuration)){
                return false;
            }
        }
        for(ModuleRevision moduleRevision : positiveModuleRevisions){
            if (moduleRevision.holds(configuration)){
                return true;
            }
        }
        return false;
    }

    @Override
    public Collection<ModuleRevision> getAllModuleRevisions(){
        Collection<ModuleRevision> allModuleRevisions = new HashSet<>();
        allModuleRevisions.addAll(this.positiveModuleRevisions);
        allModuleRevisions.addAll(this.negativeModuleRevisions);
        return allModuleRevisions;
    }

    @Override
    public Collection<ModuleRevision> getPositiveModuleRevisions(){
        return this.positiveModuleRevisions;
    }

    @Override
    public Collection<ModuleRevision> getNegativeModuleRevisions(){
        return this.negativeModuleRevisions;
    }
}
