package at.jku.isse.ecco.storage.mem.featuretrace;

import at.jku.isse.ecco.feature.Configuration;
import at.jku.isse.ecco.featuretracerecording.FeatureTrace;
import at.jku.isse.ecco.featuretracerecording.FeatureTraceCondition;
import at.jku.isse.ecco.module.ModuleRevision;
import at.jku.isse.ecco.tree.Node;

import java.util.Collection;

public class MemFeatureTrace implements FeatureTrace {

    // condition for the artefact to be present (unfulfilled presence condition does not imply absence)
    private FeatureTraceCondition presenceCondition;
    // condition for the artefact to be absent (unfulfilled absence condition does not imply presence)
    //private FeatureTraceCondition absenceCondition;

    private Node node;

    public MemFeatureTrace(Node node, FeatureTraceCondition presenceCondition){
        this.node = node;
        this.presenceCondition = presenceCondition;
    }

    @Override
    public Node getNode(){
        return this.node;
    }

    @Override
    public boolean holds(Configuration configuration){
        return presenceCondition.holds(configuration);
    }

    @Override
    public Collection<ModuleRevision> getAllModuleRevisions(){
        return this.presenceCondition.getAllModuleRevisions();
    }
}
