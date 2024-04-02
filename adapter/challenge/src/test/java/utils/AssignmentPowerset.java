package utils;

import org.logicng.datastructures.Assignment;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;

import java.util.Collection;
import java.util.LinkedList;
import java.util.stream.Collectors;

public class AssignmentPowerset {

    public static Collection<Assignment> getAssignmentPowerset(FormulaFactory factory, Collection<String> literalStrings){
        Collection<Literal> literals = literalStrings.stream().
                map(s -> factory.literal(s, true))
                .collect(Collectors.toList());
        Collection<Assignment> assignments = new LinkedList<>();
        for (Literal literal : literals){
            assignments = doubleWithAddedLiteral(assignments, literal);
        }
        return assignments;
    }

    private static Collection<Assignment> doubleWithAddedLiteral(Collection<Assignment> assignments, Literal literal){
        Collection<Assignment> newAssignments = new LinkedList<>(assignments);
        for (Assignment assignment : assignments){
            Assignment newAssignment = new Assignment(assignment.literals());
            newAssignments.add(newAssignment);
        }
        return newAssignments;
    }
}
