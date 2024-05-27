package at.jku.isse.ecco.tree;

import at.jku.isse.ecco.tree.Node;

public class EmptyVisitor implements Node.NodeVisitor{
    @Override
    public void visit(Node node) {}
}
