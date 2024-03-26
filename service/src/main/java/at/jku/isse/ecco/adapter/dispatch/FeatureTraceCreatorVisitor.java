package at.jku.isse.ecco.adapter.dispatch;

import at.jku.isse.ecco.dao.EntityFactory;
import at.jku.isse.ecco.featuretracerecording.FeatureTrace;
import at.jku.isse.ecco.featuretracerecording.FeatureTraceCondition;
import at.jku.isse.ecco.repository.Repository;
import at.jku.isse.ecco.tree.Node;
import at.jku.isse.ecco.tree.RootNode;

import java.util.Collection;

public class FeatureTraceCreatorVisitor implements Node.Op.NodeVisitor{

    private final Repository.Op repository;
    private final EntityFactory factory;

    public FeatureTraceCreatorVisitor(Repository.Op repository, EntityFactory factory){
        this.repository = repository;
        this.factory = factory;
    }
    @Override
    public void visit(Node.Op node) {
        Collection<FeatureTraceCondition> featureTraceConditions = node.getFeatureTraceConditions();
        FeatureTraceCondition mergedCondition = featureTraceConditions.stream().reduce(null, FeatureTraceCondition::merge);
        if (mergedCondition == null) { return; } // all conditions were null
        Node.Op nodeCopy = node.copyTree().createPathSkeleton();
        // at the top, there must be a root node
        Node.Op rootNode = factory.createRootNode();
        nodeCopy.setParent(rootNode);
        rootNode.addChild(nodeCopy);
        FeatureTrace featureTrace = this.factory.createFeatureTrace(nodeCopy.getParent(), mergedCondition);
        this.repository.addFeatureTrace(featureTrace);
    }
}
