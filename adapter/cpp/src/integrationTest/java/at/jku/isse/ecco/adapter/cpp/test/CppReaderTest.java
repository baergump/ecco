package at.jku.isse.ecco.adapter.cpp.test;

import at.jku.isse.ecco.adapter.cpp.CppReader;
import at.jku.isse.ecco.storage.mem.dao.MemEntityFactory;
import at.jku.isse.ecco.tree.Node;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class CppReaderTest {

    //private final Path FILES_PATH = Paths.get("C:\\Users\\Bernhard\\Work\\Projects\\Experiment\\CRepos\\openvpn\\Scenarios\\005Variants\\Variant16");
    private final Path FILES_PATH = Paths.get("C:\\Users\\Bernhard\\Work\\Projects\\tmp\\openvpn_variant");
    //private final Path FILES_PATH = Paths.get("C:\\Users\\Bernhard\\Work\\Projects\\tmp\\openvpn");
    private CppReader reader;


    @Test
    public void readFilesTest(){
        long start = System.currentTimeMillis();

        this.reader = new CppReader(new MemEntityFactory());
        Collection<Path> relativeFiles = this.getRelativeDirContent(this.FILES_PATH);
        Path[] relativeFileAr = relativeFiles.toArray(new Path[relativeFiles.size()]);
        Set<Node.Op> nodes = this.reader.read(this.FILES_PATH, relativeFileAr);

        long finish = System.currentTimeMillis();
        long timeElapsed = finish - start;
        System.out.printf("took %d milliseconds (%d seconds).%n", timeElapsed, timeElapsed / 1000);
    }

    private Collection<Path> getRelativeDirContent(Path dir){
        Map<Integer, String[]> prioritizedPatterns = this.reader.getPrioritizedPatterns();
        String[] patterns = prioritizedPatterns.values().iterator().next();
        Collection<PathMatcher> pathMatcher = Arrays.stream(patterns)
                .map(p -> FileSystems.getDefault().getPathMatcher("glob:" + p))
                .collect(Collectors.toList());

        Set<Path> fileSet = new HashSet<>();
        try (Stream<Path> pathStream = Files.walk(dir)) {
            pathStream.forEach( path -> {
                    Boolean applicableFile = pathMatcher.stream().map(pm -> pm.matches(path)).reduce(Boolean::logicalOr).get();
                    if (applicableFile) {
                        fileSet.add(path);
                    }
                });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return fileSet.stream().map(f -> dir.relativize(f)).collect(Collectors.toList());
    }
}
