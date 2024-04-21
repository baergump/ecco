package utils;

import at.jku.isse.ecco.feature.Feature;
import at.jku.isse.ecco.feature.FeatureRevision;
import at.jku.isse.ecco.featuretrace.FeatureTrace;
import at.jku.isse.ecco.featuretrace.evaluation.EvaluationStrategy;
import at.jku.isse.ecco.featuretrace.evaluation.UserAdditionEvaluation;
import at.jku.isse.ecco.repository.Repository;
import at.jku.isse.ecco.service.EccoService;
import at.jku.isse.ecco.tree.Node;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class FTExperimentTest {
    private final Path REPOSITORY_PATH = Paths.get("C:\\Users\\Berni\\Desktop\\Project\\FeatureTraceChallenge\\Repositories\\DummyScenario");

    private final Path GROUND_TRUTHS = Paths.get("C:\\Users\\Berni\\Desktop\\Project\\FeatureTraceChallenge\\Scenarios\\ScenarioAllVariants");
    //private final Path GROUND_TRUTHS = Paths.get("C:\\Users\\Bernhard\\Work\\Projects\\Experiment\\Scenarios\\DummyScenario");

    private final Path RESULT_PATH = Paths.get("C:\\Users\\Berni\\Desktop\\Project\\FeatureTraceChallenge\\Results\\Add\\DummyScenario\\000_Feature_Traces\\000_Faulty_Traces");

    // how many feature traces should be used in percent
    private final int FT_PERCENT = 0;
    // how many faulty feature traces should exist in percent
    private final int FT_MISTAKES_PERCENT = 0;
    private final EvaluationStrategy EVALUATION_STRATEGY = new UserAdditionEvaluation();

    private EccoService eccoService;
    private Repository.Op repository;

    private final String[] ALL_FEATURES = {"STATEDIAGRAM", "ACTIVITYDIAGRAM", "USECASEDIAGRAM", "COLLABORATIONDIAGRAM",
            "DEPLOYMENTDIAGRAM", "SEQUENCEDIAGRAM", "COGNITIVE", "LOGGING"};


    public static void main(String[] args) {
        // train a repository before the experiment
        //EccoTrainer trainer = new EccoTrainer();
        //trainer.trainScenario();
        //EccoService eccoService = trainer.getEccoService();
        FTExperimentTest experiment = new FTExperimentTest();
        experiment.runExperiment();
    }

    private void initService(){
        this.eccoService = new EccoService();
        this.eccoService.setRepositoryDir(this.REPOSITORY_PATH.resolve(".ecco"));
        this.eccoService.open();
        this.repository = (Repository.Op) this.eccoService.getRepository();
    }

    public void runExperiment(){
        this.initService();
        repository.removeFeatureTracePercentage(100 - this.FT_PERCENT);
        Node.Op mainTree = this.repository.fuseAssociationsWithFeatureTraces();
        CounterVisitor visitor = new CounterVisitor();
        mainTree.traverse(visitor);
        this.baseCleanup(mainTree);
        this.literalNameCleanup(mainTree);
        // todo: introduce certain amount of mistakes in remaining feature traces
        MetricsCalculator metricsCalculator = new MetricsCalculator(this.GROUND_TRUTHS, this.ALL_FEATURES);
        metricsCalculator.calculateMetrics(mainTree, this.EVALUATION_STRATEGY, RESULT_PATH);
    }

    private void baseCleanup(Node.Op node){
        Feature baseFeature = this.repository.getFeaturesByName("BASE").iterator().next();
        FeatureRevision baseFeatureRevision = baseFeature.getLatestRevision();
        String baseFeatureName = baseFeatureRevision.getLogicLiteralRepresentation();
        BaseCleanUpVisitor visitor = new BaseCleanUpVisitor(baseFeatureName);
        node.traverse(visitor);
    }

    private void literalNameCleanup(Node.Op node){
        // necessary for when there are features in the ground-truth without revision-ID
        Map<String, String> literalNameMap = new HashMap<>();
        for (String groundTruthName : this.ALL_FEATURES){
            Collection<Feature> features = this.repository.getFeaturesByName(groundTruthName);
            if (features.size() != 0){
                String repoName = features.iterator().next().getLatestRevision().getLogicLiteralRepresentation();
                literalNameMap.put(groundTruthName, repoName);
            }
        }
        LiteralCleanUpVisitor visitor = new LiteralCleanUpVisitor(literalNameMap);
        node.traverse(visitor);
    }


}
