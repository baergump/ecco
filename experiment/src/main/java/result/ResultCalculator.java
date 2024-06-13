package result;


import at.jku.isse.ecco.featuretrace.evaluation.EvaluationStrategy;
import at.jku.isse.ecco.tree.Node;
import org.logicng.datastructures.Assignment;
import org.logicng.formulas.FormulaFactory;
import result.persister.ResultPersister;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;


public class ResultCalculator {

    private final Path groundTruths;
    private final Collection<String> allFeatures;
    private ResultPersister resultPersister;

    public ResultCalculator(Path groundTruths, String[] allFeatures, ResultPersister resultPersister){
        this.groundTruths = groundTruths;
        this.allFeatures = Arrays.stream(allFeatures).collect(Collectors.toList());
        this.resultPersister = resultPersister;
    }

    public void calculateMetrics(Node.Op mainTree, EvaluationStrategy evaluationStrategy){
        FormulaFactory formulaFactory = new FormulaFactory();
        Collection<Assignment> assignments = AssignmentPowerset.getAssignmentPowerset(formulaFactory, allFeatures);
        EvaluationVisitor visitor = new EvaluationVisitor(formulaFactory, assignments, groundTruths, evaluationStrategy);
        mainTree.traverse(visitor);
        Collection<NodeResult> nodeResults = visitor.getResults();
        Collection<Result> results = nodeResults.stream().map(NodeResult::getResult).collect(Collectors.toList());
        Result overallResult = Result.overallResult(results);
        this.resultPersister.persist(overallResult);
    }
}
