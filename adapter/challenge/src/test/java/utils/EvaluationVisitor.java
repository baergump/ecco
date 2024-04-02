package utils;

import at.jku.isse.ecco.featuretrace.LogicUtils;
import at.jku.isse.ecco.featuretrace.evaluation.EvaluationStrategy;
import at.jku.isse.ecco.featuretrace.parser.VevosCondition;
import at.jku.isse.ecco.featuretrace.parser.VevosConditionHandler;
import at.jku.isse.ecco.featuretrace.parser.VevosFileConditionContainer;
import at.jku.isse.ecco.tree.Node;
import at.jku.isse.ecco.util.Location;
import org.logicng.datastructures.Assignment;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;

import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedList;

public class EvaluationVisitor implements Node.Op.NodeVisitor {

    private final FormulaFactory formulaFactory;
    private Collection<NodeResult> results = new LinkedList<>();
    private final Collection<Assignment> assignments;
    private final Path groundTruths;
    private final EvaluationStrategy evaluationStrategy;

    public EvaluationVisitor(FormulaFactory formulaFactory,
                             Collection<Assignment> assignments,
                             Path groundTruths,
                             EvaluationStrategy evaluationStrategy) {
        this.formulaFactory = formulaFactory;
        this.assignments = assignments;
        this.groundTruths = groundTruths;
        this.evaluationStrategy = evaluationStrategy;
    }

    @Override
    public void visit(Node.Op node) {
        Location location = node.getLocation();
        // ignore nodes without line numbers (like plugin-node, directories etc.)
        if (location == null){ return; }
        Formula groundTruth = this.getGroundTruth(location);
        String resultConditionString = node.getFeatureTrace().getOverallConditionString(this.evaluationStrategy);
        Formula resultCondition = LogicUtils.parseString(this.formulaFactory, resultConditionString);
        this.createResult(node, resultCondition, groundTruth);
    }

    private Formula getGroundTruth(Location location){
        String configurationString = location.getConfigurationString();
        Path groundTruth = VevosUtils.getVariantPath(groundTruths, configurationString);
        VevosConditionHandler vevosHandler = new VevosConditionHandler(groundTruth);
        VevosFileConditionContainer conditionContainer = vevosHandler.getFileSpecificPresenceConditions(location.getFilePath());
        Collection<VevosCondition> matchingConditions = conditionContainer.getMatchingPresenceConditions(location.getStartLine(), location.getEndLine());
        if (matchingConditions.size() == 0){
            return this.formulaFactory.constant(true);
        } else if (matchingConditions.size() > 1){
            return matchingConditions.stream()
                    .map(vc -> LogicUtils.parseString(this.formulaFactory, vc.getConditionString()))
                    .reduce(this.formulaFactory::and).get();
        } else {
            VevosCondition condition = matchingConditions.iterator().next();
            return LogicUtils.parseString(this.formulaFactory, condition.getConditionString());
        }
    }

    private void createResult(Node.Op node, Formula resultCondition, Formula groundTruth){
        NodeResult nodeResult = new NodeResult(node, resultCondition, groundTruth);
        this.assignments.forEach(nodeResult::updateResult);
        // nodeResult.computeMetrics();
        this.results.add(nodeResult);
    }

    public Collection<NodeResult> getResults(){
        return this.results;
    }
}
