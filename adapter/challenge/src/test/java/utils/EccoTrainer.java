package utils;

import at.jku.isse.ecco.service.EccoService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EccoTrainer {

    public static void main(String[] args) {
        Path repositoryPath = Paths.get("C:\\Users\\Bernhard\\Work\\Projects\\ArgoUML_Challenge\\Repositories\\ScenarioAllVariants_AllFeatureTraces");
        //Path repositoryPath = Paths.get("C:\\Users\\Bernhard\\Work\\Projects\\ArgoUML_Challenge\\Repositories\\DummyScenario");
        Path scenarioPath = Paths.get("C:\\Users\\Bernhard\\Work\\Projects\\ArgoUML_Challenge\\ScenarioAllVariants");
        //Path scenarioPath = Paths.get("C:\\Users\\Bernhard\\Work\\Projects\\ArgoUML_Challenge\\DummyScenario");
        EccoTrainer trainer = new EccoTrainer(repositoryPath);
        trainer.trainScenario(scenarioPath);
    }

    private EccoService eccoService;

    public EccoTrainer(Path repositoryPath){
        this.createRepository(repositoryPath);
    }

    public void trainScenario(Path scenarioPath){
        List<Path> paths = getVariantFolders(scenarioPath);
        int n = 0;
        for (Path path : paths){
            System.out.println(String.format("Committing variant %d of %d: %s", n + 1, paths.size(), scenarioPath.toString()));
            this.train(path);
            n++;
        }
    }

    private List<Path> getVariantFolders(Path scenarioPath){
        try (Stream<Path> stream = Files.list(scenarioPath)) {
            return stream.filter(Files::isDirectory)
                    .filter(folder -> !folder.getFileName().toString().equals("configs"))
                    .collect(Collectors.toList());
        } catch (IOException e){
            throw new RuntimeException(e.getMessage());
        }
    }

    private void createRepository(Path repositoryPath){
        this.eccoService = new EccoService();
        this.eccoService.setRepositoryDir(repositoryPath.resolve(".ecco").toAbsolutePath());
        this.eccoService.init();
    }

    private void train(Path variantPath){
        this.eccoService.setBaseDir(variantPath.toAbsolutePath());
        this.eccoService.commit();
    }
}
