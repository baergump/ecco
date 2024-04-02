package at.jku.isse.ecco.adapter.dispatch;

import at.jku.isse.ecco.dao.EntityFactory;
import at.jku.isse.ecco.repository.Repository;
import at.jku.isse.ecco.tree.Node;


public class FeatureTraceCreatorVisitor implements Node.Op.NodeVisitor{

    private final Repository.Op repository;
    private final EntityFactory factory;

    public FeatureTraceCreatorVisitor(Repository.Op repository, EntityFactory factory){
        this.repository = repository;
        this.factory = factory;
    }

    @Override
    public void visit(Node.Op node) {
        if (!node.getFeatureTrace().containsUserCondition()) {
            return;
        }
        // todo: feature-trace.equals(): equal conditions, equal nodes to the top until root

        Node.Op nodeCopy = node.createPathSkeleton();
        Node.Op rootNode = factory.createRootNode();
        rootNode.addChild((Node.Op) nodeCopy.getRoot());
        this.repository.addFeatureTrace(nodeCopy.getFeatureTrace());
    }
}
