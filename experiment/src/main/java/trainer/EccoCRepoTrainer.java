package trainer;

import at.jku.isse.ecco.service.EccoService;

import com.fasterxml.jackson.databind.ObjectMapper;
import utils.DirUtils;
import utils.ServiceUtils;
import utils.vevos.ConfigTransformer;
import utils.vevos.VevosUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


public class EccoCRepoTrainer implements EccoTrainer {

    // the different numbers of variants to train the ecco-repository with
    private static final int[] NUM_VARIANTS = {3, 5};
    private static final int NUM_SAMPLED_FEATURES = 10;
    private static final int NUM_VARIANT_PICKS = 5;
    private static final String REPO_NAME = "openvpn";
    private static final Path[] REPO_SAMPLING_BASE_PATHS = {
            Paths.get("C:\\Users\\Berni\\Desktop\\Project\\Tools\\VEVOS_Simulation_Sampling\\simulated_variants\\" + REPO_NAME)};
    private static final Path REPOSITORIES_PATH =
            Paths.get("C:\\Users\\Berni\\Desktop\\Project\\FeatureTraceChallenge\\CRepos\\" + REPO_NAME + "\\Repositories");

    private int pickNumber;
    private int sampleNumber = 0;
    private int numberOfVariants;
    private Path samplePath;
    private List<Path> variantPick;
    private EccoService eccoService;
    private Path repositoryPath;

    public static void main(String[] args) {
        EccoCRepoTrainer trainer = new EccoCRepoTrainer();
        trainer.train();
    }

    public void train(){
        for (Path repoPath : REPO_SAMPLING_BASE_PATHS){
            System.out.println("Training C-Repository " + repoPath + "...");
            List<Path> samplePaths = DirUtils.getSubDirectoryPaths(repoPath);
            List<Path> extendedSamplePaths = VevosUtils.extendSamplePathsByCommitFolder(samplePaths);
            extendedSamplePaths.forEach(this::trainSample);
        }
    }

    private void trainSample(Path samplePath){
        this.sampleNumber++;
        System.out.println("Training feature sample " + samplePath + "...");
        this.samplePath = samplePath;
        for (int numberOfVariants : NUM_VARIANTS){
            this.numberOfVariants = numberOfVariants;
            System.out.println("Training " + numberOfVariants + " variants...");
            this.trainNumberOfVariants(numberOfVariants);
        }
    }

    private void trainNumberOfVariants(int numberOfVariants){
        List<Path> variantPaths = VevosUtils.getVariantFolders(this.samplePath);
        for (int i = 1; i <= NUM_VARIANT_PICKS; i++) {
            this.pickNumber = i;
            System.out.printf("Doing variant pick %d of %d%n", i, NUM_VARIANT_PICKS);
            this.variantPick = this.pickRandomVariants(variantPaths, numberOfVariants);
            this.trainRepository();
        }
    }

    private List<Path> pickRandomVariants(List<Path> variantPaths, int pickSize){
        Collections.shuffle(variantPaths);
        return variantPaths.subList(0, pickSize);
    }

    private void trainRepository(){
        try{
            this.repositoryPath = REPOSITORIES_PATH.resolve(this.getRepositoryFolderName());

            File dir = this.repositoryPath.toFile();
            if (dir.exists()){
                System.out.println("Repository already exists...");
                return;
            }

            System.out.println("Creating Ecco-Respository " + this.repositoryPath + "...");
            DirUtils.createDir(this.repositoryPath);
            this.persistRepositoryInformation();
            this.eccoService = ServiceUtils.createEccoService(this.repositoryPath);
            this.commitVariantPick();
        } catch (Exception e){
            this.cleanupService();
            throw new RuntimeException(e);
        }
    }

    private void commitVariantPick(){
        int n = 0;
        for (Path variantPath : this.variantPick){
            n++;
            System.out.printf("Committing variant %d of %d: %s%n", n, this.variantPick.size(), variantPath.toString());
            this.train(variantPath);
        }
    }

    private void train(Path variantPath){
        this.eccoService.setBaseDir(variantPath.toAbsolutePath());
        this.eccoService.commit();
    }

    private void persistRepositoryInformation(){
        System.out.println("Writing repository information...");
        String[] sampledFeatures = ConfigTransformer.gatherConfigFeatures(this.samplePath.resolve("configs"), NUM_SAMPLED_FEATURES);
        List<String> variantConfigurations = this.createVariantConfigurationList();
        RepositoryInformation info = new RepositoryInformation(REPO_NAME, sampledFeatures, variantConfigurations);
        ObjectMapper objectMapper = new ObjectMapper();
        File file = new File(this.repositoryPath.resolve("repositoryInformation.json").toUri());
        try {
            objectMapper.writeValue(file, info);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<String> createVariantConfigurationList(){
        List<String> configList = new LinkedList<>();
        for (Path variantPath : this.variantPick){
            configList.add(VevosUtils.variantPathToConfigString(variantPath));
        }
        return configList;
    }

    private String getRepositoryFolderName(){
        return "repository_sample" + this.sampleNumber
                + "_numberOfVariants" + this.numberOfVariants
                + "_pick" + this.pickNumber;
    }

    private void cleanupService() {
        if (this.eccoService != null){ this.eccoService.close(); }
        DirUtils.deleteDir(this.repositoryPath.resolve(".ecco").toAbsolutePath());
    }
}

