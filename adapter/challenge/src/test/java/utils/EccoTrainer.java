package utils;

import at.jku.isse.ecco.service.EccoService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EccoTrainer {

    public static void main(String[] args) {
        Path repositoryPath = Paths.get("C:\\Users\\Bernhard\\Work\\Projects\\ArgoUML_Challenge\\Repositories\\ScenarioRandom004Variants");
        //Path repositoryPath = Paths.get("C:\\Users\\Bernhard\\Work\\Projects\\ArgoUML_Challenge\\Repositories\\DummyRepository");
        //Path repositoryPath = Paths.get("C:\\Users\\Berni\\Desktop\\Project\\FeatureTraceChallenge\\Repositories\\ScenarioRandom002Variants");
        Path scenarioPath = Paths.get("C:\\Users\\Bernhard\\Work\\Projects\\ArgoUML_Challenge\\Scenarios_no_FT\\ScenarioRandom004Variants");
        //Path scenarioPath = Paths.get("C:\\Users\\Bernhard\\Work\\Projects\\ArgoUML_Challenge\\DummyScenario");
        //Path scenarioPath = Paths.get("C:\\Users\\Berni\\Desktop\\Project\\FeatureTraceChallenge\\Scenarios\\ScenarioRandom002Variants");
        EccoTrainer trainer = new EccoTrainer(repositoryPath);
        trainer.trainScenario(scenarioPath);
    }

    /*public static void main(String[] args) throws IOException {
        Path variantsPath = Paths.get("C:\\Users\\Bernhard\\Work\\Projects\\ArgoUML_Challenge\\Scenarios\\ScenarioAllVariants");
        Path repoPath = Paths.get("C:\\Users\\Bernhard\\Work\\Projects\\ArgoUML_Challenge\\Repositories\\DummyScenario");
        EccoTrainer eccoTrainer = new EccoTrainer(repoPath);
        String[] configStrings = {"BASE",
                "COGNITIVE",
                "ACTIVITYDIAGRAM",
                "SEQUENCEDIAGRAM"};
        Path path = eccoTrainer.searchVariant(variantsPath, configStrings);
        System.out.println(path);
    }
     */

    private EccoService eccoService;

    public EccoTrainer(Path repositoryPath){
        this.createRepository(repositoryPath);
    }

    public void trainScenario(Path scenarioPath){
        List<Path> paths = getVariantFolders(scenarioPath);
        int n = 0;
        for (Path path : paths){
            System.out.println(String.format("Committing variant %d of %d: %s", n + 1, paths.size(), scenarioPath.toString()));
            System.out.println(path);
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

    public void generateScenario(){
        Path configsPath = Paths.get("C:\\Users\\Bernhard\\Work\\Tools\\argouml-spl-benchmark\\ArgoUMLSPLBenchmark\\scenarios\\ScenarioRandom002Variants\\configs");
        Path scenarioPath = Paths.get("C:\\Users\\Bernhard\\Work\\Projects\\ArgoUML_Challenge\\Scenarios\\ScenarioRandom004Variants");
        Path allVariantsPath = Paths.get("C:\\Users\\Bernhard\\Work\\Projects\\ArgoUML_Challenge\\Scenarios\\variants");

        // read all configs
        // for every config
    }

    public Path searchVariant(Path variantsPath, String[] configStrings) throws IOException {
        List<Path> variantPaths = this.getVariantFolders(variantsPath);
        for (Path variantPath : variantPaths){
            Path configPath = variantPath.resolve(".config").toAbsolutePath();
            List<String> allLines = Files.readAllLines(configPath);
            String variantConfig = allLines.get(0);
            String[] variantConfigStrings = variantConfig.split(",");
            for (int i = 0; i < variantConfigStrings.length; i++){ variantConfigStrings[i] = variantConfigStrings[i].trim(); }
            List<String> variantConfigStringsList = Arrays.stream(variantConfigStrings).collect(Collectors.toList());

            if (!(variantConfigStrings.length == configStrings.length)) continue;
            boolean allThere = true;
            for (String config : configStrings){
                if (!variantConfigStringsList.contains(config)) {
                    allThere = false;
                    break;
                }
            }
            if (allThere) return variantPath;
        }
        throw new RuntimeException("No matching variant found...");
    }
}
