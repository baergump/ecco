package utils;

import at.jku.isse.ecco.feature.Feature;
import at.jku.isse.ecco.feature.FeatureRevision;
import at.jku.isse.ecco.featuretrace.evaluation.*;
import at.jku.isse.ecco.repository.Repository;
import at.jku.isse.ecco.service.EccoService;
import at.jku.isse.ecco.tree.Node;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class FTExperiment {

    private final EvaluationStrategy[] STRATEGIES = {
            new UserAdditionEvaluation(),
            new UserSubtractionEvaluation(),
            new UserBasedEvaluation(),
            new DiffBasedEvaluation()
    };
    private final int[] FT_PERCENTAGES = {0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100};
    private final int[] MISTAKE_PERCENTAGES = {0};
    //private final Path REPOSITORY_BASE_PATH = Paths.get("C:\\Users\\Berni\\Desktop\\Project\\FeatureTraceChallenge\\Repositories");
    private final Path REPOSITORY_BASE_PATH = Paths.get("C:\\Users\\Bernhard\\Work\\Projects\\Experiment\\Repositories");
    private final Path GROUND_TRUTHS = Paths.get("C:\\Users\\Bernhard\\Work\\Projects\\Experiment\\Variants");

    private final Path RESULTS_BASE_PATH = Paths.get("C:\\Users\\Bernhard\\Work\\Projects\\Experiment\\Results");

    private EccoService eccoService;
    private Path repositoryPath;
    private Repository.Op repository;
    private int ftPercentage;
    private int mistakePercentage;
    private EvaluationStrategy strategy;

    private final String[] FEATURES = {"STATEDIAGRAM", "ACTIVITYDIAGRAM", "USECASEDIAGRAM", "COLLABORATIONDIAGRAM",
            "DEPLOYMENTDIAGRAM", "SEQUENCEDIAGRAM", "COGNITIVE", "LOGGING"};

    public static void main(String[] args) {
        //FTExperiment experiment = new FTExperiment();
        //experiment.iterateStrategies();

        //experiment.test();


        Path path = VevosUtils.getVariantPath(Paths.get("C:\\Users\\Bernhard\\Work\\Projects\\Experiment\\Variants"), "BASE, COGNITIVE, COLLABORATIONDIAGRAM, STATEDIAGRAM");
    }

    public void test(){
        this.repositoryPath = Paths.get("C:\\Users\\Bernhard\\Work\\tmp\\ScenarioAllVariants");
        this.ftPercentage = 100;
        this.mistakePercentage = 0;
        this.strategy = new UserBasedEvaluation();
        this.initService();
        this.runExperiment(Paths.get("C:\\Users\\Bernhard\\Work\\Test"));
    }

    private void initService(){
        this.eccoService = new EccoService();
        this.eccoService.setRepositoryDir(this.repositoryPath.resolve(".ecco"));
        this.eccoService.open();
        this.repository = (Repository.Op) this.eccoService.getRepository();
    }

    public void runExperiment(Path resultBasePath){
        this.printExperimentMessage();
        if (Files.exists(resultBasePath.resolve("results.txt"))){
            System.out.println("Experiment already done.");
            return;
        }
        this.initService();
        new File(resultBasePath.toAbsolutePath().toString()).mkdirs();
        repository.removeFeatureTracePercentage(100 - this.ftPercentage);
        Node.Op mainTree = this.repository.fuseAssociationsWithFeatureTraces();
        CounterVisitor visitor = new CounterVisitor();
        mainTree.traverse(visitor);
        this.baseCleanup(mainTree);
        this.literalNameCleanup(mainTree);
        // todo: introduce certain amount of mistakes in remaining feature traces
        MetricsCalculator metricsCalculator = new MetricsCalculator(this.GROUND_TRUTHS, this.FEATURES);
        metricsCalculator.calculateMetrics(mainTree, this.strategy, resultBasePath);
        System.out.println("");
    }

    private void iterateStrategies(){
        for (EvaluationStrategy strategy : STRATEGIES){
            this.strategy = strategy;
            String folderName = this.strategyToFolderName(strategy);
            this.iterateRepositories(RESULTS_BASE_PATH.resolve(folderName));
        }
    }

    private void iterateRepositories(Path basePath){
        try (Stream<Path> pathStream = Files.list(REPOSITORY_BASE_PATH)){
            pathStream.forEach(path -> {
                    this.repositoryPath = path;
                    this.iterateFeatureTracePercentages(basePath.resolve(path.getFileName()));
            });
        } catch(IOException e){
            e.printStackTrace();
            throw new RuntimeException("Could not access repositories: " + e.getMessage());
        }
    }

    private void iterateFeatureTracePercentages(Path basePath){
        for(int featureTracePercentage : FT_PERCENTAGES){
            this.ftPercentage = featureTracePercentage;
            String folderName = this.threeDigitString(featureTracePercentage) + "_FEATURE_TRACES";
            this.iterateMistakes(basePath.resolve(folderName));
        }
    }

    private void iterateMistakes(Path basePath){
        for (int mistakePercentage : MISTAKE_PERCENTAGES){
            this.mistakePercentage = mistakePercentage;
            String folderName = this.threeDigitString(mistakePercentage) + "_FAULTY_TRACES";
            this.runExperiment(basePath.resolve(folderName));
        }
    }

    private String threeDigitString(int i){
        if (i < 0 || i > 999){
            throw new RuntimeException();
        } else if (i < 10){
            return "00" + i;
        } else if (i < 100) {
            return "0" + i;
        } else {
            return String.valueOf(i);
        }
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
        for (String groundTruthName : this.FEATURES){
            Collection<Feature> features = this.repository.getFeaturesByName(groundTruthName);
            if (features.size() != 0){
                String repoName = features.iterator().next().getLatestRevision().getLogicLiteralRepresentation();
                literalNameMap.put(groundTruthName, repoName);
            }
        }
        LiteralCleanUpVisitor visitor = new LiteralCleanUpVisitor(literalNameMap);
        node.traverse(visitor);
    }

    private String strategyToFolderName(EvaluationStrategy strategy){
        if (strategy instanceof UserAdditionEvaluation){
            return "Add";
        } else if (strategy instanceof UserSubtractionEvaluation){
            return "Remove";
        } else if (strategy instanceof UserBasedEvaluation){
            return "AddAndRemove";
        } else if (strategy instanceof DiffBasedEvaluation){
            return "Diff";
        } else {
            throw new RuntimeException();
        }
    }

    private void printExperimentMessage(){
        System.out.println(
                "Running experiment with following settings:\n" +
                "Strategy: " + this.strategyToFolderName(this.strategy) + "\n" +
                "Scenario: " + this.repositoryPath.getFileName() + "\n" +
                "Feature Trace Percentage: " + this.ftPercentage + "\n" +
                "Mistake Percentage: " + this.mistakePercentage
        );
    }
}
