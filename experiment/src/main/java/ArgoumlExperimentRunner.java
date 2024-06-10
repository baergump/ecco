import at.jku.isse.ecco.feature.Feature;
import at.jku.isse.ecco.feature.FeatureRevision;
import at.jku.isse.ecco.featuretrace.FeatureTrace;
import at.jku.isse.ecco.featuretrace.evaluation.*;
import at.jku.isse.ecco.repository.Repository;
import at.jku.isse.ecco.service.EccoService;
import at.jku.isse.ecco.tree.Node;
import mistake.*;
import org.apache.commons.io.FileUtils;
import result.ResultCalculator;
import utils.BaseCleanUpVisitor;
import utils.CounterVisitor;
import utils.LiteralCleanUpVisitor;
import utils.vevos.ConfigTransformer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ArgoumlExperimentRunner implements ExperimentRunner {

    private final EvaluationStrategy[] STRATEGIES = {
            new UserAdditionEvaluation(),
            new UserSubtractionEvaluation(),
            new UserBasedEvaluation(),
            //new DiffBasedEvaluation()
    };

    private Collection<MistakeCreator> mistakeCreators = new LinkedList<>();
    //private final int[] FT_PERCENTAGES = {0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100};
    private final int[] FT_PERCENTAGES = {100};
    //private final int[] MISTAKE_PERCENTAGES = {0, 1, 5, 10, 20};
    private final int[] MISTAKE_PERCENTAGES = {5};
    //private final Path REPOSITORY_BASE_PATH = Paths.get("C:\\Users\\Bernhard\\Work\\Projects\\Experiment\\Repositories");
    private final Path REPOSITORY_BASE_PATH = Paths.get("C:\\Users\\Berni\\Desktop\\Project\\FeatureTraceChallenge\\CRepos\\openvpn\\Repositories");
    //private final Path GROUND_TRUTHS = Paths.get("C:\\Users\\Bernhard\\Work\\Projects\\Experiment\\Variants");
    private final Path GROUND_TRUTHS = Paths.get("C:\\Users\\Berni\\Desktop\\Project\\Tools\\VEVOS_Simulation_Sampling\\simulated_variants\\openvpn\\0bdcfb99e1425cb6a73362f5462a7293ddfd699b");

    //private final Path RESULTS_BASE_PATH = Paths.get("C:\\Users\\Bernhard\\Work\\Projects\\Experiment\\Results");
    private final Path RESULTS_BASE_PATH = Paths.get("C:\\Users\\Berni\\Desktop\\Project\\FeatureTraceChallenge\\CRepos\\openvpn\\Results");

    private EccoService eccoService;
    private Path repositoryPath;
    private Repository.Op repository;
    private int ftPercentage;
    private int mistakePercentage;
    private EvaluationStrategy strategy;
    private MistakeStrategy mistakeStrategy;

    //private final String[] FEATURES = {"STATEDIAGRAM", "ACTIVITYDIAGRAM", "USECASEDIAGRAM", "COLLABORATIONDIAGRAM",
    //        "DEPLOYMENTDIAGRAM", "SEQUENCEDIAGRAM", "COGNITIVE", "LOGGING"};

    /*private final String[] FEATURES = {"ENABLE_BUSYBOX",
            "LZO1Y",
            "ETH_P_ATMFATE",
            "ENABLE_FEATURE_FDISK_WRITABLE",
            "CSUSP",
            "BASH_TEST2",
            "EXPR_H",
            "UCLIBC_VERSION__GEQ__KERNEL_VERSION___LB__0__9__34__RB__",
            "ENABLE_FEATURE_FIND_GROUP",
            "ENABLE_FEATURE_INDIVIDUAL"};
     */
    private String[] features;
    private final int MAX_FEATURE_NUMBER = 10;

    public static void main(String[] args) {
        ArgoumlExperimentRunner experiment = new ArgoumlExperimentRunner();
        experiment.features = ConfigTransformer.gatherConfigFeatures(experiment.GROUND_TRUTHS.resolve("configs"), experiment.MAX_FEATURE_NUMBER);

        //experiment.summarizeSpecificResults();
        experiment.createMistakeCreators();
        //experiment.iterateStrategies();
        experiment.runSpecificExperiment();
        //experiment.test();
        //experiment.summarizeResults();
        //experiment.checkCounts();
        //Path path = VevosUtils.getVariantPath(Paths.get("C:\\Users\\Berni\\Desktop\\Project\\FeatureTraceChallenge\\Scenarios\\ScenarioAllVariants"), "ACTIVITYDIAGRAM, BASE, COGNITIVE, COLLABORATIONDIAGRAM, DEPLOYMENTDIAGRAM, LOGGING, SEQUENCEDIAGRAM, STATEDIAGRAM, USECASEDIAGRAM");
    }

    public void runSpecificExperiment(){
        //this.repositoryPath = Paths.get("C:\\Users\\Berni\\Desktop\\Project\\FeatureTraceChallenge\\Repositories\\ScenarioRandom005Variants");
        //this.repositoryPath = Paths.get("C:\\Users\\Bernhard\\Work\\Projects\\Experiment\\Repositories\\ScenarioRandom005Variants");
        this.repositoryPath = Paths.get("C:\\Users\\Berni\\Desktop\\Project\\FeatureTraceChallenge\\CRepos\\openvpn\\Repositories\\005Variants");
        this.ftPercentage = 100;
        this.mistakePercentage = 10;
        this.strategy = new UserBasedEvaluation();

        ConditionSwapper conditionSwapper = new ConditionSwapper();
        this.mistakeStrategy = conditionSwapper;
        MistakeCreator conditionSwapCreator = new MistakeCreator(conditionSwapper);

        this.runExperiment(Paths.get("C:\\Users\\Berni\\Desktop\\Project\\FeatureTraceChallenge\\CRepos\\openvpn\\Results"), conditionSwapCreator);
        //summarizeResults();
        //experiment.checkCounts();
        //Path path = VevosUtils.getVariantPath(Paths.get("C:\\Users\\Bernhard\\Work\\Projects\\Experiment\\Variants"), "BASE, COGNITIVE, COLLABORATIONDIAGRAM, STATEDIAGRAM");
    }

    public void checkCounts(){
        this.repositoryPath = Paths.get("C:\\Users\\Berni\\Desktop\\Project\\FeatureTraceChallenge\\Repositories\\ScenarioAllVariants");
        this.initService();
        CounterVisitor ftTreeVisitor = new CounterVisitor();
        Node.Op ftTree = this.repository.getFeatureTree();
        ftTree.traverse(ftTreeVisitor);
        ftTreeVisitor.printEverything();
        System.out.println();

        CounterVisitor assocVisitor = new CounterVisitor();
        Node.Op assoc = this.repository.getAssociations().iterator().next().getRootNode();
        assoc.traverse(assocVisitor);
        assocVisitor.printEverything();
        System.out.println();

        CounterVisitor mainVisitor = new CounterVisitor();
        Node.Op mainTree = this.repository.fuseAssociationsWithFeatureTraces();
        mainTree.traverse(mainVisitor);
        mainVisitor.printEverything();
    }

    public void summarizeResults() {
        //Path basePath = Paths.get("C:\\Users\\Bernhard\\Work\\Projects\\Experiment\\Results\\AddAndRemove\\ScenarioRandom010Variants");
        Path basePath = Paths.get("C:\\Users\\Bernhard\\Work\\Projects\\Experiment\\Results\\AddAndRemove\\ScenarioAllVariants");
        Path summaryPath = Paths.get("C:\\Users\\Bernhard\\Work\\Projects\\Experiment\\Results\\Summaries\\allvariants.txt");
        String summary = "";
        try(Stream<Path> pathStream = Files.list(basePath)) {
            for (Path path : pathStream.collect(Collectors.toList())){
                Path resultFile = path.resolve("005_FAULTY_TRACES\\results.txt");
                summary = summary + "\n" + FileUtils.readFileToString(resultFile.toFile());
            }
            FileWriter fileWriter = new FileWriter(summaryPath.toString());
            PrintWriter printWriter = new PrintWriter(fileWriter);
            printWriter.print(summary);
            printWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void summarizeSpecificResults() {
        // (verschiedene Strategien) (5 Varianten) (verschiedene # FeatureTraces) (10% Fehler) (verschiedene Fehlerarten)
        // fixed strategy
        // fixed variants
        // different amounts of featureTraces
        // fixed amount of mistakes
        // fixed mistake-type
        Path basePath = Paths.get("C:\\Users\\Bernhard\\Work\\Projects\\Experiment\\Results\\AddAndRemove\\ScenarioRandom005Variants");
        Path summaryPath = Paths.get("C:\\Users\\Bernhard\\Work\\Projects\\Experiment\\Results\\Summaries\\userBased_conditionSwap.txt");
        String summary = "";
        try(Stream<Path> pathStream = Files.list(basePath)) {
            for (Path path : pathStream.collect(Collectors.toList())){
                Path resultFile = path.resolve("005_FAULTY_TRACES\\ConditionSwapper\\results.txt");
                summary = summary + "\n" + FileUtils.readFileToString(resultFile.toFile());
            }
            FileWriter fileWriter = new FileWriter(summaryPath.toString());
            PrintWriter printWriter = new PrintWriter(fileWriter);
            printWriter.print(summary);
            printWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void test(){
        //this.repositoryPath = Paths.get("C:\\Users\\Bernhard\\Work\\tmp\\ScenarioAllVariants");
        this.repositoryPath = Paths.get("C:\\Users\\Berni\\Desktop\\Project\\FeatureTraceChallenge\\Repositories\\ScenarioAllVariants");
        this.initService();
        CounterVisitor visitor = new CounterVisitor();
        Node.Op mainTree = this.repository.fuseAssociationsWithFeatureTraces();
        mainTree.traverse(visitor);
        visitor.printEverything();
        //this.ftPercentage = 100;
        //this.mistakePercentage = 10;
        //this.strategy = new UserBasedEvaluation();
        //this.initService();
        //this.runExperiment(Paths.get("C:\\Users\\Berni\\Desktop\\Project\\tmp"));
    }

    private void initService(){
        this.eccoService = new EccoService();
        this.eccoService.setRepositoryDir(this.repositoryPath.resolve(".ecco"));
        this.eccoService.open();
        this.repository = (Repository.Op) this.eccoService.getRepository();
    }

    public void runExperiment(Path resultBasePath, MistakeCreator mistaker){
        this.printExperimentMessage();
        if (Files.exists(resultBasePath.resolve("results.txt"))){
            System.out.println("Experiment already done.");
            return;
        }
        this.initService();
        new File(resultBasePath.toAbsolutePath().toString()).mkdirs();
        repository.removeFeatureTracePercentage(100 - this.ftPercentage);
        //MistakeCreator mistaker = this.createMistakeCreator(repository.getFeatureTraces());
        mistaker.createMistakePercentage(this.repository, this.mistakePercentage);
        Node.Op mainTree = this.repository.fuseAssociationsWithFeatureTraces();
        CounterVisitor visitor = new CounterVisitor();
        mainTree.traverse(visitor);
        this.baseCleanup(mainTree);
        this.literalNameCleanup(mainTree);
        //ResultCalculator metricsCalculator = new ResultCalculator(this.GROUND_TRUTHS, this.features);
        //metricsCalculator.calculateMetrics(mainTree, this.strategy, resultBasePath);
    }

    private MistakeCreator createMistakeCreator(Collection<FeatureTrace> traces){
        //MistakeStrategy mistakeStrategy = new ConditionSwapper(traces);
        //MistakeStrategy mistakeStrategy = new Conjugator(this.features);
        //MistakeStrategy mistakeStrategy = new Unconjugator(this.features);
        //MistakeStrategy mistakeStrategy = new OperatorSwapper();
        MistakeStrategy mistakeStrategy = new FeatureSwitcher(this.features);
        return new MistakeCreator(mistakeStrategy);
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
            this.iterateMistakeTypes(basePath.resolve(folderName));
        }
    }

    private void createMistakeCreators(){
        ConditionSwapper conditionSwapper = new ConditionSwapper();
        MistakeCreator conditionSwapCreator = new MistakeCreator(conditionSwapper);
        this.mistakeCreators.add(conditionSwapCreator);

        Conjugator conjugator = new Conjugator(this.features);
        MistakeCreator conjugatorCreator = new MistakeCreator(conjugator);
        this.mistakeCreators.add(conjugatorCreator);

        FeatureSwitcher featureSwitcher = new FeatureSwitcher(this.features);
        MistakeCreator featureSwitchCreator = new MistakeCreator(featureSwitcher);
        this.mistakeCreators.add(featureSwitchCreator);

        OperatorSwapper operatorSwapper = new OperatorSwapper();
        MistakeCreator operatorSwapCreator = new MistakeCreator(operatorSwapper);
        this.mistakeCreators.add(operatorSwapCreator);

        Unconjugator unconjugator = new Unconjugator();
        MistakeCreator unconjugateCreator = new MistakeCreator(unconjugator);
        this.mistakeCreators.add(unconjugateCreator);

        NoMistaker noMistaker = new NoMistaker();
        MistakeCreator noMistakeCreator = new MistakeCreator(noMistaker);
        this.mistakeCreators.add(noMistakeCreator);
    }

    private void iterateMistakeTypes(Path basePath){
        for(MistakeCreator mistakeCreator : this.mistakeCreators){
            this.mistakeStrategy = mistakeCreator.getMistakeStrategy();
            this.runExperiment(basePath.resolve(mistakeCreator.getMistakeStrategy().getClass().getSimpleName()), mistakeCreator);
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
        for (String groundTruthName : this.features){
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
                        "Mistake Percentage: " + this.mistakePercentage + "\n" +
                        "Mistake Strategy: " + this.mistakeStrategy.getClass().getSimpleName()
        );
    }
}
