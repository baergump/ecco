package utils;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Transform VEVOS configuration files to ECCO configuration files and put them in respective variant folders.
 */
public class ConfigTransformer {

    public static void main(String[] args) {
        final Path VARIANTS_BASE_PATH = Paths.get("C:\\Users\\Bernhard\\Work\\Projects\\ArgoUML_Challenge\\ScenarioAllVariants");
        transformConfigurations(VARIANTS_BASE_PATH);
    }

    public static void transformConfigurations(Path variantsBasePath){
        try (Stream<Path> stream = Files.list(variantsBasePath.resolve("configs"))) {
            List <Path> paths = stream.filter(file -> !Files.isDirectory(file))
                    .filter(file -> file.getFileName().toString().contains(".config"))
                    .collect(Collectors.toList());
            for (Path path : paths){
                transformConfiguration(path);
            }
        } catch (IOException e){
            System.out.printf(e.getMessage());
        }
    }

    public static void transformConfiguration(Path configFilePath){
        String configFileName = configFilePath.getFileName().toString();
        String extension = FilenameUtils.getExtension(configFileName);
        configFileName = configFileName.replace("." + extension, "");
        Path eccoConfigFile = configFilePath.getParent().getParent().resolve(configFileName + "\\.config").toAbsolutePath();
        try {
            List<String> allLines = Files.readAllLines(configFilePath);
            allLines = allLines.stream().filter(f -> !f.isEmpty()).collect(Collectors.toList());
            String eccoConfigString = String.join(", ", allLines);

            if (eccoConfigString.isEmpty()){
                eccoConfigString = "BASE";
            } else {
                eccoConfigString = "BASE, " + eccoConfigString;
            }

            Files.write(eccoConfigFile, eccoConfigString.getBytes());
        } catch (IOException e) {
            System.out.printf(e.getMessage());
        }
    }
}
