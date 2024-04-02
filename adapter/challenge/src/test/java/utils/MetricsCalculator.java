package utils;


import at.jku.isse.ecco.featuretrace.evaluation.EvaluationStrategy;
import at.jku.isse.ecco.tree.Node;
import org.logicng.datastructures.Assignment;
import org.logicng.formulas.FormulaFactory;
import utils.AssignmentPowerset;
import utils.EvaluationVisitor;
import utils.NodeResult;
import utils.Result;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;


public class MetricsCalculator {

    private final Path groundTruths;
    private final Collection<String> allFeatures;

    public MetricsCalculator(Path groundTruths, String[] allFeatures){
        this.groundTruths = groundTruths;
        this.allFeatures = Arrays.stream(allFeatures).collect(Collectors.toList());
    }

    public void calculateMetrics(Node.Op mainTree, EvaluationStrategy evaluationStrategy, Path resultPath){
        // TODO: make multiple revisions of the same feature possible
        FormulaFactory formulaFactory = new FormulaFactory();
        Collection<Assignment> assignments = AssignmentPowerset.getAssignmentPowerset(formulaFactory, allFeatures);
        EvaluationVisitor visitor = new EvaluationVisitor(formulaFactory, assignments, groundTruths, evaluationStrategy);
        mainTree.traverse(visitor);
        Collection<NodeResult> nodeResults = visitor.getResults();
        Collection<Result> results = nodeResults.stream().map(NodeResult::getResult).collect(Collectors.toList());
        Result overallResult = Result.overallResult(results);
        overallResult.save(resultPath);
    }
}
