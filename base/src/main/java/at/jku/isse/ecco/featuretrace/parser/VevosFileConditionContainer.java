package at.jku.isse.ecco.featuretrace.parser;

import java.util.Collection;
import java.util.HashSet;

public class VevosFileConditionContainer {

    Collection<VevosCondition> fileSpecificConditions;

    public VevosFileConditionContainer(Collection<VevosCondition> conditions){
        this.fileSpecificConditions = conditions;
    }

    public Collection<VevosCondition> getMatchingPresenceConditions(int startLine, int endLine){
        Collection<VevosCondition> matchingConditions = new HashSet<>();
        for (VevosCondition condition : this.fileSpecificConditions){
            this.validateLines(startLine, endLine, condition);
            if (condition.getStartLine() <= startLine && condition.getEndLine() >= endLine){
                matchingConditions.add(condition);
            }
        }
        return matchingConditions;
    }

    private void validateLines(int startLine, int endLine, VevosCondition condition){
        if (this.rangesAreOverlapping(startLine, endLine, condition.getStartLine(), condition.getEndLine())){
            throw new RuntimeException(String.format("Line ranges of ast node and condition are overlapping. node: %d - %d; condition: %d - %d"
                    , startLine, endLine, condition.getStartLine(), condition.getEndLine()));
        }
    }

    private boolean rangesAreOverlapping(int start1, int end1, int start2, int end2){
        if (start1 < start2 && start2 < end1 && end1 < end2) { return true; }
        if (start2 < start1 && start1 < end2 && end2 < end1) { return true; }
        return false;
    }
}
