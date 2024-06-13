import at.jku.isse.ecco.feature.Feature;
import at.jku.isse.ecco.feature.FeatureRevision;
import at.jku.isse.ecco.featuretrace.evaluation.*;
import at.jku.isse.ecco.repository.Repository;
import at.jku.isse.ecco.tree.Node;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import mistake.*;
import result.ResultContext;
import result.persister.ResultDatabasePersister;
import result.persister.ResultPersister;
import trainer.RepositoryInformation;
import utils.*;
import result.ResultCalculator;
import utils.vevos.ConfigTransformer;
import utils.vevos.VevosUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
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


    private final Path C_REPOS_BASE_PATH = Paths.get("C:\\Users\\Bernhard\\Work\\Projects\\Experiment\\CRepos");
    private final Path GROUND_TRUTHS_BASE_PATH = Paths.get("C:\\Users\\Bernhard\\Work\\Tools\\VEVOS_Simulation_Sampling\\simulated_variants");
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
    private RepositoryInformation repositoryInformation;

    public static void main(String[] args) {
        CExperimentRunner experiment = new CExperimentRunner();
        experiment.runSpecificExperiment();
    }

    /*public static void main(String[] args) {
        CExperimentRunner experiment = new CExperimentRunner();

        long startTime = System.currentTimeMillis();
        experiment.runRepositoryExperiments();
        long endTime   = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        System.out.println((totalTime / 1000) + " Seconds runtime...");
    }*/


    public void runSpecificExperiment(){
        this.cRepoName = "openvpn";
        this.repositoryBasePath = C_REPOS_BASE_PATH.resolve( this.cRepoName + "\\Repositories");
        this.eccoRepositoryPath = Paths.get ("C:\\Users\\Bernhard\\Work\\Projects\\Experiment\\CRepos\\openvpn\\Repositories\\repository_sample1_numberOfVariants3_pick1");
        this.setRepositoryInformation();
        this.setGroundTruthPath();
        this.ftPercentage = 10;
        this.mistakePercentage = 10;
        this.strategy = new UserBasedEvaluation();
        this.prepareExperiment();

        Conjugator conjugator = new Conjugator(this.features);
        //ConditionSwapper conditionSwapper = new ConditionSwapper();
        this.mistakeCreator = new MistakeCreator(conjugator);
        this.runExperiment();
    }

    private void runRepositoryExperiments(){
        for (String cRepoName : REPO_NAMES){
            this.cRepoName = cRepoName;
            this.iterateEccoRepositories();
        }
    }

    private void iterateEccoRepositories(){
        this.repositoryBasePath = C_REPOS_BASE_PATH.resolve( this.cRepoName + "\\Repositories");
        List<Path> eccoRepositoryPaths = DirUtils.getSubDirectoryPaths(this.repositoryBasePath);
        for(Path eccoRepositoryPath : eccoRepositoryPaths){
            this.eccoRepositoryPath = eccoRepositoryPath;
            this.setRepositoryInformation();
            this.setGroundTruthPath();
            this.runRepositoryExperiment();
        }
    }

    private void runRepositoryExperiment(){
        for (int ftPercentage : FT_PERCENTAGES){
            this.ftPercentage = ftPercentage;
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
        //repository.removeFeatureTracePercentage(100 - this.ftPercentage);
        repository.removeFeatureTracePercentage(100 - 50);
        this.mistakeCreator.createMistakePercentage(this.repository, this.mistakePercentage);
        Node.Op mainTree = this.repository.fuseAssociationsWithFeatureTraces();
        this.baseCleanup(mainTree);
        this.literalNameCleanup(mainTree);
        ResultContext resultContext = this.createResultContext();
        ResultPersister resultPersister = this.prepareResultPersister(resultContext);
        ResultCalculator metricsCalculator = new ResultCalculator(this.groundTruthPath, this.features, resultPersister);
        metricsCalculator.calculateMetrics(mainTree, this.strategy);
    }

    private ResultPersister prepareResultPersister(ResultContext resultContext){
        ResultDatabasePersister persister = new ResultDatabasePersister();
        persister.setResultContext(resultContext);
        return persister;
    }

    private void prepareExperiment(){
        this.repository = ServiceUtils.openEccoRepository(this.eccoRepositoryPath);
        this.features = ConfigTransformer.gatherConfigFeatures(this.groundTruthPath.resolve("configs"), MAX_SAMPLED_FEATURES);
        this.createMistakeCreators();
    }

    private void setRepositoryInformation(){
        try {
            URL jsonURL = this.eccoRepositoryPath.resolve("repositoryInformation.json").toUri().toURL();
            this.repositoryInformation = new ObjectMapper().readValue(jsonURL, RepositoryInformation.class);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (JsonMappingException e) {
            throw new RuntimeException(e);
        } catch (JsonParseException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void setGroundTruthPath(){
        Path groundTruthPath = GROUND_TRUTHS_BASE_PATH.resolve(this.cRepoName);
        groundTruthPath = groundTruthPath.resolve(this.repositoryInformation.sampleName);
        groundTruthPath = VevosUtils.extendPathByCommitFolder(groundTruthPath);
        this.groundTruthPath = groundTruthPath;
    }

    private void createMistakeCreators(){
        this.mistakeCreators = new LinkedList<>();

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

    private ResultContext createResultContext(){
        return new ResultContext(this.repositoryInformation,
                this.ftPercentage,
                this.mistakePercentage,
                this.strategy.getStrategyName(),
                this.mistakeCreator.getMistakeStrategy().getClass().getSimpleName());
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
