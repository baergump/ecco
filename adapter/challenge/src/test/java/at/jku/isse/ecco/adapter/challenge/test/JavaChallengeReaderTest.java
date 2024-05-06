package at.jku.isse.ecco.adapter.challenge.test;

import at.jku.isse.ecco.adapter.challenge.JavaChallengeReader;
import at.jku.isse.ecco.dao.EntityFactory;
import at.jku.isse.ecco.repository.Repository;
import at.jku.isse.ecco.service.EccoService;
import at.jku.isse.ecco.storage.mem.dao.MemEntityFactory;
import at.jku.isse.ecco.tree.Node;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JavaChallengeReaderTest {

    private final Path REPOSITORY_PATH = Paths.get("src", "test","resources", "test_repository");
    private final Path VARIANT_PATH = Paths.get("src\\test\\resources\\test_variant\\Variant_5");
    private final Path VARIANTS_BASE_PATH = Paths.get("src\\test\\resources\\test_variant\\Variant_5");
    private EccoService eccoService;
    private Repository.Op repository;
    private JavaChallengeReader reader;

    @BeforeEach
    public void setup() throws IOException {
        //this.deleteRepository();
        this.createRepository(this.VARIANT_PATH);
        //this.openRepository(this.VARIANT_PATH);
        EntityFactory factory = new MemEntityFactory();
        this.reader = new JavaChallengeReader(factory);
    }

    private void setup(Path variantPath) throws IOException {
        this.deleteRepository();
        this.createRepository(variantPath);
        EntityFactory factory = new MemEntityFactory();
        this.reader = new JavaChallengeReader(factory);
    }

    //@AfterEach
    public void teardown() throws IOException {
        if (this.eccoService != null){
            this.eccoService.close();
        }
        this.deleteRepository();
    }

    private void createRepository(Path variantPath){
        this.eccoService = new EccoService();
        this.eccoService.setRepositoryDir(this.REPOSITORY_PATH.resolve(".ecco").toAbsolutePath());
        this.eccoService.setBaseDir(variantPath.toAbsolutePath());
        this.eccoService.init();
        this.repository = (Repository.Op) this.eccoService.getRepository();
    }

    private void openRepository(Path variantPath){
        this.eccoService = new EccoService();
        this.eccoService.setRepositoryDir(this.REPOSITORY_PATH.resolve(".ecco").toAbsolutePath());
        this.eccoService.setBaseDir(variantPath.toAbsolutePath());
        this.eccoService.open();
        this.repository = (Repository.Op) this.eccoService.getRepository();
    }

    private void deleteRepository() throws IOException {
        Path repositoryFolderPath = this.REPOSITORY_PATH.resolve(".ecco");
        File repositoryFolder = repositoryFolderPath.toFile();
        if (repositoryFolder.exists()){
            FileUtils.deleteDirectory(repositoryFolder);
        }
        FileUtils.cleanDirectory(this.REPOSITORY_PATH.toFile());
    }

    @Test
    public void parseFileTest(){
        long start = System.currentTimeMillis();
        Set<Node.Op> nodes = this.eccoService.readFiles();
        long finish = System.currentTimeMillis();

        long timeElapsed = finish - start;
        System.out.printf("took %d milliseconds (%d seconds).%n", timeElapsed, timeElapsed / 1000);
    }

    @Test
    public void logicalExpressionsTest() throws IOException {
        for(int i = 0; i <= 255; i++){
            Path variantPath = Paths.get("C:\\Users\\Bernhard\\Work\\Tools\\ArgoUMLExtractor\\variants\\Variant_" + i);
            this.setup(variantPath);
            long start = System.currentTimeMillis();
            Set<Node.Op> nodes = this.eccoService.readFiles();
            long finish = System.currentTimeMillis();
            long timeElapsed = finish - start;
            System.out.printf("took %d milliseconds (%d seconds).%n", timeElapsed, timeElapsed / 1000);
            this.teardown();
        }
    }

    @Test
    public void listAllLogicalExpressions(){
        Path VARIANTS_PATH = Paths.get("C:\\Users\\Berni\\Desktop\\Project\\Tools\\ArgoUMLExtractor\\variants");
        try {
            Stream<Path> folderStream = Files.walk(VARIANTS_PATH)
                    .filter(Files::isDirectory);
            List<Path> paths = folderStream.collect(Collectors.toList());
            for (Path path : paths){

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void readFeatureTracesTest(){
        this.eccoService.commit();
        assertFalse(this.repository.getFeatureTraces().isEmpty());
    }

    @Test
    public void trainAllVariants(){

    }

    // TODO: Trees.treeFusion: multiple nodes with equal artifacts in the same variant will be fused correctly
    // TODO: (the correct "same" node must be chosen during fusion -> same sequence number)
}
