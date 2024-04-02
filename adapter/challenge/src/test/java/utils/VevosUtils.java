package utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VevosUtils {

    public static Path getVariantPath(Path variantsBasePath, String configString) {
        try {
            // a config-element may be a feature or a feature-revision
            Collection<String> configElements = configStringToElements(configString);
            List<Path> variantPaths = getVariantFolders(variantsBasePath);
            for (Path variantPath : variantPaths) {
                Path configPath = variantPath.resolve(".config").toAbsolutePath();
                String variantConfig = Files.readAllLines(configPath).get(0);
                Collection<String> variantConfigElements = configStringToElements(variantConfig);
                if (configElementsMatch(configElements, variantConfigElements)) {
                    return variantPath;
                }
            }
            throw new RuntimeException("No matching variant found...");
        } catch (IOException e){
            throw new RuntimeException("Exception while reading configuration: " + e.getMessage());
        }
    }

    private static Collection<String> configStringToElements(String configString){
        String[] configElements = configString.split(",");
        for (int i = 0; i < configElements.length; i++){ configElements[i] = configElements[i].trim(); }
        return Arrays.stream(configElements).collect(Collectors.toList());
    }

    private static boolean configElementsMatch(Collection<String> elementsA, Collection<String> elementsB){
        if (!(elementsA.size() == elementsB.size())) { return false; }
        for (String configElement : elementsB){
            if (!elementsA.contains(configElement)) { return false; }
        }
        return true;
    }

    public static List<Path> getVariantFolders(Path variantsBasePath){
        try (Stream<Path> stream = Files.list(variantsBasePath)) {
            return stream.filter(Files::isDirectory)
                    .filter(folder -> !folder.getFileName().toString().equals("configs"))
                    .collect(Collectors.toList());
        } catch (IOException e){
            throw new RuntimeException(e.getMessage());
        }
    }
}
