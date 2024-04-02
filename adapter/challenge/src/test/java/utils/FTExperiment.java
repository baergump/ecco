package utils;

import at.jku.isse.ecco.featuretrace.evaluation.EvaluationStrategy;
import at.jku.isse.ecco.featuretrace.evaluation.UserAdditionEvaluation;
import at.jku.isse.ecco.repository.Repository;
import at.jku.isse.ecco.service.EccoService;
import at.jku.isse.ecco.tree.Node;

import java.nio.file.Path;
import java.nio.file.Paths;


public class FTExperiment {

    private final Path SCENARIO_PATH = Paths.get("C:\\Users\\Berni\\Desktop\\Project\\FeatureTraceChallenge\\Repositories\\DummyScenario");
    private final Path GROUND_TRUTHS = Paths.get("C:\\Users\\Berni\\Desktop\\Project\\FeatureTraceChallenge\\Scenarios\\ScenarioAllVariants");
    private final Path RESULT_PATH = Paths.get("C:\\Users\\Berni\\Desktop\\Project\\FeatureTraceChallenge\\Results\\Add\\ScenarioRandom002Variants\\000_Feature_Traces\\000_Faulty_Traces");
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
        EccoTrainer trainer = new EccoTrainer();
        trainer.trainScenario();
        EccoService eccoService = trainer.getEccoService();
        FTExperiment experiment = new FTExperiment();
        experiment.runExperiment(eccoService);
    }

    public void runExperiment(EccoService eccoService){
        // this.initService();
        Repository.Op repository = (Repository.Op) eccoService.getRepository();
        repository.removeFeatureTracePercentage(100 - this.FT_PERCENT);
        // todo: introduce certain amount of mistakes in remaining feature traces
        Node.Op mainTree = this.repository.fuseAssociationsWithFeatureTraces();
        MetricsCalculator metricsCalculator = new MetricsCalculator(this.GROUND_TRUTHS, this.ALL_FEATURES);
        metricsCalculator.calculateMetrics(mainTree, this.EVALUATION_STRATEGY, RESULT_PATH);
    }

    private void initService(){
        this.eccoService = new EccoService();
        this.eccoService.setRepositoryDir(this.SCENARIO_PATH.resolve(".ecco"));
        this.eccoService.open();
        this.repository = (Repository.Op) this.eccoService.getRepository();
    }
}
