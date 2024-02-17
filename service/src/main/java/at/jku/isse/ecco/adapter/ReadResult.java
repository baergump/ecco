package at.jku.isse.ecco.adapter;

import at.jku.isse.ecco.featuretracerecording.FeatureTrace;
import at.jku.isse.ecco.tree.Node;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ReadResult {
    private Set<Node.Op> nodes;
    private Collection<FeatureTrace> featureTraces;

    public ReadResult(){
        this.nodes = new HashSet<>();
        this.featureTraces = new HashSet<>();
    }

    public Set<Node.Op> getNodes(){
        return this.nodes;
    }

    public Collection<FeatureTrace> getFeatureTraces(){
        return this.featureTraces;
    }

    public void setNodes(Set<Node.Op> nodes){
        this.nodes = nodes;
    }

    public void setFeatureTraces(Set<FeatureTrace> featureTraces){
        this.featureTraces = featureTraces;
    }
}
