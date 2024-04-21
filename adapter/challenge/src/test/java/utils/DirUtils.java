package utils;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DirUtils {
    public static void createDir(Path path){
        File newDir = path.toFile();
        if (!newDir.exists()) {
            assertTrue(newDir.mkdir());
        }
    }
    public static void deleteDir(Path path) throws IOException {
        File dir = path.toFile();
        if (dir.exists()) FileUtils.deleteDirectory(dir);
    }
}
