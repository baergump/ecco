package utils;

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

    public static void transformConfigurations(Path variantsBasePath){
        try (Stream<Path> stream = Files.list(variantsBasePath)) {
            // TODO: only consider ".config" files
            stream.filter(file -> !Files.isDirectory(file))
                    .forEach(ConfigTransformer::transformConfiguration);
        } catch (IOException e){
            // TODO
        }
    }

    public static void transformConfiguration(Path configFilePath){
        // TODO: get variant number
        try {
            List<String> allLines = Files.readAllLines(configFilePath);
            allLines = allLines.stream().filter(f -> !f.isEmpty()).collect(Collectors.toList());
            String eccoConfigString = String.join(", ", allLines);
            // TODO
            Files.write(Paths.get("todo"), eccoConfigString.getBytes());
        } catch (IOException e) {
            // TODO
        }
    }

}
