import at.jku.isse.ecco.feature.Feature;
import at.jku.isse.ecco.feature.FeatureRevision;
import at.jku.isse.ecco.featuretrace.evaluation.*;
import at.jku.isse.ecco.repository.Repository;
import at.jku.isse.ecco.tree.Node;
import mistake.*;
import result.persister.ResultDatabasePersister;
import result.persister.ResultPersister;
import utils.*;
import result.ResultCalculator;
import utils.vevos.ConfigTransformer;
import utils.vevos.VevosUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


public class CExperimentRunner implements ExperimentRunner {

    private final String[] REPO_NAMES = {"openvpn"};
    private final int[] FT_PERCENTAGES = {0, 5, 10, 15};
    private final int FT_PICKS = 5;
    private final int[] MISTAKE_PERCENTAGES = {0, 10};
    private final EvaluationStrategy[] STRATEGIES = {
            new UserAdditionEvaluation(),
            new UserSubtractionEvaluation(),
            new UserBasedEvaluation()
    };


    private final Path C_REPOS_BASE_PATH = Paths.get("C:\\Users\\Berni\\Desktop\\Project\\FeatureTraceChallenge\\CRepos");
    private final Path GROUND_TRUTHS_BASE_PATH = Paths.get("C:\\Users\\Berni\\Desktop\\Project\\Tools\\VEVOS_Simulation_Sampling\\simulated_variants");
    private final int MAX_SAMPLED_FEATURES = 10;


    private String cRepoName;
    private Path repositoryBasePath;
    private Path groundTruthPath;
    private int ftPercentage;
    private int mistakePercentage;
    private EvaluationStrategy strategy;
    private Repository.Op repository;
    private Path eccoRepositoryPath;
    private MistakeCreator mistakeCreator;
    private String[] features;
    private Collection<MistakeCreator> mistakeCreators = new LinkedList<>();


    public static void main(String[] args) {
        CExperimentRunner experiment = new CExperimentRunner();
        experiment.runRepositoryExperiments();
    }

    private void runRepositoryExperiments(){
        for (String cRepoName : REPO_NAMES){
            this.cRepoName = cRepoName;
            this.runRepositoryExperiment();
        }
    }

    private void runRepositoryExperiment(){
        this.repositoryBasePath = C_REPOS_BASE_PATH.resolve( this.cRepoName + "\\Repositories");
        this.setGroundTruthPath();
        for (int ftPercentage : FT_PERCENTAGES){
            this.ftPercentage = ftPercentage;
            this.iterateEccoRepositories();
        }
    }

    private void iterateEccoRepositories(){
        List<Path> eccoRepositoryPaths = DirUtils.getSubDirectoryPaths(this.repositoryBasePath);
        for(Path eccoRepositoryPath : eccoRepositoryPaths){
            this.eccoRepositoryPath = eccoRepositoryPath;
            this.iterateFeatureTracePicks();
        }
    }

    private void iterateFeatureTracePicks(){
        for (int i = 1; i <= FT_PICKS; i++){
            this.iterateMistakePercentages();
        }
    }

    private void iterateMistakePercentages(){
        for (int mistakePercentage : MISTAKE_PERCENTAGES){
            this.mistakePercentage = mistakePercentage;
            this.iterateStrategies();
        }
    }

    private void iterateStrategies(){
        for (EvaluationStrategy strategy : STRATEGIES){
            this.strategy = strategy;
            this.prepareExperiment();
            this.iterateMistakeCreators();
        }
    }

    private void iterateMistakeCreators(){
        for (MistakeCreator mistakeCreator : this.mistakeCreators){
            this.mistakeCreator = mistakeCreator;
            this.runExperiment();
        }
    }

    public void runExperiment(){
        this.printExperimentMessage();
        repository.removeFeatureTracePercentage(100 - this.ftPercentage);
        this.mistakeCreator.createMistakePercentage(this.repository, this.mistakePercentage);
        Node.Op mainTree = this.repository.fuseAssociationsWithFeatureTraces();
        this.baseCleanup(mainTree);
        this.literalNameCleanup(mainTree);
        ResultPersister resultPersister = this.prepareResultPersister();
        ResultCalculator metricsCalculator = new ResultCalculator(this.groundTruthPath, this.features, resultPersister);
        metricsCalculator.calculateMetrics(mainTree, this.strategy);
    }

    private ResultPersister prepareResultPersister(){
        return new ResultDatabasePersister();
    }

    private void prepareExperiment(){
        this.repository = ServiceUtils.openEccoRepository(this.eccoRepositoryPath);
        this.features = ConfigTransformer.gatherConfigFeatures(this.groundTruthPath.resolve("configs"), MAX_SAMPLED_FEATURES);
        this.createMistakeCreators();
    }

    private void setGroundTruthPath(){
        Path groundTruthPath = GROUND_TRUTHS_BASE_PATH.resolve(this.cRepoName);
        groundTruthPath = VevosUtils.extendPathByCommitFolder(groundTruthPath);
        this.groundTruthPath = groundTruthPath;
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

    private void printExperimentMessage(){
        System.out.println(
                "Running experiment with following settings:\n" +
                        "Strategy: " + this.strategy.getStrategyName() + "\n" +
                        "Scenario: " + this.eccoRepositoryPath.getFileName() + "\n" +
                        "Feature Trace Percentage: " + this.ftPercentage + "\n" +
                        "Mistake Percentage: " + this.mistakePercentage + "\n" +
                        "Mistake Strategy: " + this.mistakeCreator.getMistakeStrategy().getClass().getSimpleName()
        );
    }
}
