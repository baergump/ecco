package utils;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DirUtils {
    public static void createDir(Path path){
        try {
            Files.createDirectories(path);
        } catch (IOException e){
            throw new RuntimeException(e.getMessage());
        }
    }
    public static void deleteDir(Path path) throws IOException {
        File dir = path.toFile();
        if (dir.exists()) FileUtils.deleteDirectory(dir);
    }
}
