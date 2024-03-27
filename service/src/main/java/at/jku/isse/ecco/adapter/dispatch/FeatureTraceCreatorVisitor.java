package at.jku.isse.ecco.adapter.dispatch;

import at.jku.isse.ecco.dao.EntityFactory;
import at.jku.isse.ecco.featuretrace.FeatureTrace;
import at.jku.isse.ecco.featuretrace.FeatureTraceCondition;
import at.jku.isse.ecco.repository.Repository;
import at.jku.isse.ecco.tree.Node;

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
        Node.Op nodeCopy = node.copyTree().createPathSkeleton();
        // at the top, there must be a root node
        Node.Op rootNode = factory.createRootNode();
        nodeCopy.setParent(rootNode);
        rootNode.addChild(nodeCopy);
        FeatureTrace featureTrace = this.factory.createFeatureTrace(nodeCopy.getParent(), node.getFeatureTrace());
        this.repository.addFeatureTrace(featureTrace);
    }
}
