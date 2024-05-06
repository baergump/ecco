package at.jku.isse.ecco.adapter.cpp.test;

import at.jku.isse.ecco.repository.Repository;
import at.jku.isse.ecco.service.EccoService;
import at.jku.isse.ecco.tree.Node;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CppReaderTest {

    private final Path REPOSITORY_PATH = Paths.get("src", "integrationTest","resources", "test_repository");
    private final Path VARIANT_PATH = Paths.get("C:\\Users\\Berni\\Desktop\\Project\\Repositories\\BusyBox\\busybox");
    private EccoService eccoService;
    private Repository.Op repository;
    //private CppReader reader;

    @BeforeEach
    public void setup() throws IOException {
        this.deleteRepository();
        this.createRepository(this.VARIANT_PATH);
        //this.openRepository(this.VARIANT_PATH);
        //EntityFactory factory = new MemEntityFactory();
        //this.reader = new CppReader(factory);
    }

    @AfterEach
    public void teardown() throws IOException {
        if (this.eccoService != null){
            this.eccoService.close();
        }
        this.deleteRepository();
    }

    private void createRepository(Path variantPath) throws IOException {
        this.eccoService = new EccoService();
        this.createDir(this.REPOSITORY_PATH);
        this.eccoService.setRepositoryDir(this.REPOSITORY_PATH.resolve(".ecco").toAbsolutePath());
        this.eccoService.setBaseDir(variantPath.toAbsolutePath());
        this.eccoService.init();
        this.repository = (Repository.Op) this.eccoService.getRepository();
    }

    /*
    private void openRepository(Path variantPath){
        this.eccoService = new EccoService();
        this.eccoService.setRepositoryDir(this.REPOSITORY_PATH.resolve(".ecco").toAbsolutePath());
        this.eccoService.setBaseDir(variantPath.toAbsolutePath());
        this.eccoService.open();
        this.repository = (Repository.Op) this.eccoService.getRepository();
    }
     */

    private void deleteRepository() throws IOException {
        Path repositoryFolderPath = this.REPOSITORY_PATH.resolve(".ecco");
        File repositoryFolder = repositoryFolderPath.toFile();
        if (repositoryFolder.exists()) {
            FileUtils.deleteDirectory(repositoryFolder);
        }
        //FileUtils.cleanDirectory(this.REPOSITORY_PATH.toFile());
    }

    private void createDir(Path path){
        File newDir = path.toFile();
        assertFalse(newDir.exists());
        assertTrue(newDir.mkdir());
    }

    @Test
    public void dummytext(){
        System.out.println("Nothing to see here!");
    }

    @Test
    public void parseFileTest(){
        long start = System.currentTimeMillis();
        Set<Node.Op> nodes = this.eccoService.readFiles();
        long finish = System.currentTimeMillis();

        long timeElapsed = finish - start;
        System.out.printf("took %d milliseconds (%d seconds).%n", timeElapsed, timeElapsed / 1000);
    }
}
