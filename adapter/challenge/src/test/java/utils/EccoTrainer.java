package utils;

import at.jku.isse.ecco.service.EccoService;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class EccoTrainer {

    private static final Path BASE_PATH = Paths.get("C:\\Users\\Berni\\Desktop\\Project\\FeatureTraceChallenge\\");
    //private static final Path BASE_PATH = Paths.get("C:\\Users\\Bernhard\\Work\\Projects\\Experiment");
    private static final Path SCENARIOS_PATH = BASE_PATH.resolve("Scenarios");
    private static final Path REPOSITORIES_PATH = BASE_PATH.resolve("Repositories");
    private static final Path REFERENCE_PATH = Paths.get("C:\\Users\\Berni\\Desktop\\Project\\Tools\\argouml-spl-benchmark\\ArgoUMLSPLBenchmark\\scenarios");
    //private static final Path REFERENCE_PATH = Paths.get("C:\\Users\\Bernhard\\Work\\Tools\\argouml-spl-benchmark\\ArgoUMLSPLBenchmark\\scenarios");
    private static final Path VARIANTS_PATH = Paths.get("C:\\Users\\Berni\\Desktop\\Project\\Tools\\ArgoUMLExtractor\\variants");
    //private static final Path VARIANTS_PATH = Paths.get("C:\\Users\\Bernhard\\Work\\Projects\\Experiment\\Variants");

    private EccoService eccoService;
    private String scenarioName;
    private Path scenarioPath;
    private Path repoPath;

    public static void main(String[] args) {
        EccoTrainer.prepareScenarios();
        EccoTrainer trainer = new EccoTrainer();
        trainer.trainScenarios();
    }

    private void trainScenarios(){
        List<Path> scenarioPaths = this.getScenarioPaths();
        for (Path scenarioPath : scenarioPaths){
            try {
                this.scenarioPath = scenarioPath;
                this.scenarioName = scenarioPath.getFileName().toString();
                this.repoPath = REPOSITORIES_PATH.resolve(this.scenarioName);
                this.trainScenario();
            } catch (Exception e){
                this.cleanupService();
                e.printStackTrace();
                throw new RuntimeException(e.getMessage());
            }
        }
    }

    private List<Path> getScenarioPaths(){
        try (Stream<Path> pathStream = Files.list(SCENARIOS_PATH)){
            return pathStream.collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void prepareScenarios(){
        List<Path> scenarioReferences = VevosUtils.getVariantFolders(REFERENCE_PATH);
        for (Path scenarioReference : scenarioReferences){
            String scenarioName = FilenameUtils.getName(scenarioReference.toString());
            System.out.println("Preparing scenario " + scenarioName + ".");
            Path scenarioPath = SCENARIOS_PATH.resolve(scenarioName);
            if (scenarioPath.toFile().exists()){
                System.out.println("Scenario already prepared.\n");
                continue;
            }
            DirUtils.createDir(scenarioPath);
            Path configsDir = scenarioReference.resolve("configs");
            configsToScenarios(configsDir, scenarioPath);
            System.out.println();
        }
    }

    private static void configsToScenarios(Path configsDir, Path scenarioPath){
        try (Stream<Path> configStream = Files.list(configsDir)){
            configStream.forEach(configDir -> configToScenario(configDir, scenarioPath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void configToScenario(Path configFile, Path scenarioPath){
        String configString = ConfigTransformer.vevosConfigFileToConfig(configFile);
        configString = ConfigTransformer.addBaseFeature(configString);
        System.out.println("Found configuration: " + configString + ".");
        Path variantPath = VevosUtils.getVariantPath(VARIANTS_PATH, configString);
        System.out.println("Found respective variant " + variantPath.getFileName() + ".");
        File src = variantPath.toFile();
        File dest = scenarioPath.resolve(variantPath.getFileName()).toFile();
        try{
            System.out.println("Copying variant " + variantPath.getFileName() + "...");
            FileUtils.copyDirectory(src, dest);
            System.out.println("Copying complete.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void cleanupService() {
        if (this.eccoService != null){
            this.eccoService.close();
        }
        this.deleteDir(this.repoPath.resolve(".ecco").toAbsolutePath());
    }

    private void deleteDir(Path path) {
        try {
            File dir = path.toFile();
            if (dir.exists()) FileUtils.deleteDirectory(dir);
        }
        catch (IOException e){
            throw new RuntimeException(String.format("Could not delete directory %s: ", path) + e.getMessage());
        }
    }

    public void trainScenario(){
        System.out.println("Training scenario " + this.scenarioName + ".");
        if (this.repoPath.toFile().exists()){
            System.out.println("Scenario already trained.");
        } else {
            DirUtils.createDir(this.repoPath);
            this.createRepository();
            this.commitVariants();
            this.eccoService.close();
        }
    }

    private void commitVariants(){
        List<Path> variantPaths = VevosUtils.getVariantFolders(this.scenarioPath);
        int n = 0;
        for (Path variantPath : variantPaths){
            n++;
            System.out.printf("Committing variant %d of %d: %s%n", n, variantPaths.size(), this.scenarioPath.toString());
            System.out.println(variantPath);
            this.train(variantPath);
        }
    }

    private void createRepository(){
        this.eccoService = new EccoService();
        this.eccoService.setRepositoryDir(this.repoPath.resolve(".ecco").toAbsolutePath());
        this.eccoService.init();
    }

    private void train(Path variantPath){
        this.eccoService.setBaseDir(variantPath.toAbsolutePath());
        this.eccoService.commit();
    }
}

