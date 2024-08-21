package at.jku.isse.ecco.storage.mem.pog;

import at.jku.isse.ecco.pog.PartialOrderGraph;
import at.jku.isse.ecco.storage.mem.pog.fullfledged.MemPartialOrderGraph;
import at.jku.isse.ecco.storage.mem.pog.fullfledged.MemPartialOrderGraphNode;

import java.util.HashMap;
import java.util.Map;

public class SimplePogMerger {

    // find overlap (ending in single node)
    // split POGs
    // merge middle parts
    // fuse start, merged middle, end

    // if left and right have an overlapping start that ends in a single node, the Overlap including non-overlapping
    // branches are returned as a new POG
    public static PartialOrderGraph.Op getOverlappingStart(PartialOrderGraph.Op left, PartialOrderGraph.Op right){
        Map<MemPartialOrderGraphNode.Op, MemPartialOrderGraphNode.Op> matchingNodes = new HashMap<>();

        MemPartialOrderGraph.Op pog = new MemPartialOrderGraph();
        // iterate over nodes and add to collection
        MemPartialOrderGraphNode.Op leftNode = left.getHead();
        MemPartialOrderGraphNode.Op rightNode = right.getHead();



        // create pog
        // add collection by adding nodes one by one

    }

    private static PartialOrderGraph.Op checkOverlappingNode(MemPartialOrderGraphNode.Op left, MemPartialOrderGraphNode.Op right){
        // check if all next nodes from left are also in right

        // if yes, add to pog

        // if not return null

    }



}
