package utils;

import at.jku.isse.ecco.featuretrace.FeatureTrace;
import at.jku.isse.ecco.tree.Node;
import at.jku.isse.ecco.util.Location;

public class CounterVisitor implements Node.Op.NodeVisitor {

    private int nodeCount = 0;
    private int someConditionCount = 0;
    private int diffConditionCount = 0;
    private int userConditionCount = 0;
    private int locationCount = 0;

    @Override
    public void visit(Node.Op node) {
        this.nodeCount++;
        FeatureTrace trace = node.getFeatureTrace();

        if (trace.containsUserCondition()){
            this.userConditionCount++;
        }

        String diffCondition = trace.getDiffConditionString();
        if (diffCondition != null){
            this.diffConditionCount++;
        }

        if (trace.containsUserCondition() || diffCondition != null){
            this.someConditionCount++;
        }

        Location location = node.getLocation();
        if (location != null){
            this.locationCount++;
        }
    }

    public void printEverything(){
        System.out.println(
                "NodeCount: " + this.nodeCount + "\n"
                + "SomeConditionCount: " + this.someConditionCount + "\n"
                + "DiffConditionCount: " + this.diffConditionCount + "\n"
                + "UserConditionCount: " + this.userConditionCount + "\n"
                + "LocationCount: " + this.locationCount
        );
    }
}
