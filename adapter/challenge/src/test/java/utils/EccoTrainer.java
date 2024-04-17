package utils;

import at.jku.isse.ecco.repository.Repository;
import at.jku.isse.ecco.service.EccoService;

import org.apache.commons.io.FileUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;


public class EccoTrainer {

    //private static final Path REPOSITORY_PATH = Paths.get("C:\\Users\\Berni\\Desktop\\Project\\FeatureTraceChallenge\\Repositories\\DummyScenario");
    private static final Path REPOSITORY_PATH = Paths.get("C:\\Users\\Bernhard\\Work\\Projects\\Experiment\\Repositories\\DummyScenario");


    //private static final Path SCENARIO_PATH = Paths.get("C:\\Users\\Berni\\Desktop\\Project\\FeatureTraceChallenge\\Scenarios\\DummyScenario");
    private static final Path SCENARIO_PATH = Paths.get("C:\\Users\\Bernhard\\Work\\Projects\\Experiment\\Scenarios\\DummyScenario");


    private EccoService eccoService;
    private Repository.Op repository;

    public EccoTrainer(){
        this.createRepository();
    }

    public static void main(String[] args) {
        EccoTrainer trainer = null;
        try {

            trainer = new EccoTrainer();
            trainer.trainScenario();
            trainer.eccoService.getRepository();
        } catch (Exception e){
            if (trainer != null){ trainer.cleanupService(); }
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public EccoService getEccoService(){
        return this.eccoService;
    }

    private void cleanupService() {
        this.deleteDir(REPOSITORY_PATH.resolve(".ecco").toAbsolutePath());
    }

    private void deleteDir(Path path) {
        try {
            File dir = path.toFile();
            if (dir.exists()) FileUtils.deleteDirectory(dir);
        }
        catch (IOException e){
            throw new RuntimeException(String.format("Could not delete directory %s: ", REPOSITORY_PATH) + e.getMessage());
        }
    }

    public void trainScenario(){
        List<Path> paths = VevosUtils.getVariantFolders(SCENARIO_PATH);
        int n = 0;
        for (Path path : paths){
            n++;
            System.out.println(String.format("Committing variant %d of %d: %s", n, paths.size(), SCENARIO_PATH.toString()));
            System.out.println(path);
            this.train(path);
        }
    }

    private void createRepository(){
        this.eccoService = new EccoService();
        this.eccoService.setRepositoryDir(REPOSITORY_PATH.resolve(".ecco").toAbsolutePath());
        this.eccoService.init();
    }

    private void train(Path variantPath){
        this.eccoService.setBaseDir(variantPath.toAbsolutePath());
        this.eccoService.commit();
    }
}
