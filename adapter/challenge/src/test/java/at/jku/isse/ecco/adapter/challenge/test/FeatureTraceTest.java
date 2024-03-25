package at.jku.isse.ecco.adapter.challenge.test;

import at.jku.isse.ecco.repository.Repository;
import at.jku.isse.ecco.service.EccoService;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FeatureTraceTest {

    // TODO: put this test class in another module? service?

    // TODO: test tree builds with feature traces including order
    // add node with feature trace
    // (commit variant with feature A and node; commit variant with feature B and node as feature trace for feature C; built tree with feature A and C)
    // remove node with feature trace
    // (commit variant with feature A and B with two nodes; commit variant with feature A and one node and feature trace to A; build tree with B)
    // add child of ordered node with feature trace
    // (same as above with
    // remove child of ordered node with feature trace
    // compare child of ordered node of feature trace with read one that has sibling (equality in respect to sequence number)
    // (commit in sequence without feature trace and commit without sequence with feature trace)
    // change order with feature trace? (build tree with one order and change it with feature trace?)


    private final Path REPOSITORY_PATH = Paths.get("src", "test","resources", "repo");
    private final Path SERVICE_BASE_PATH = Paths.get("src", "test", "resources", "base");
    private EccoService eccoService;
    private Repository.Op repository;

    @BeforeEach
    public void setup() throws IOException {
        this.cleanupService();
        this.initService();
    }

    @AfterEach
    public void teardown() throws IOException {
        if (this.eccoService != null) this.eccoService.close();
        this.cleanupService();
    }

    private void createDir(Path path){
        File newDir = path.toFile();
        assertFalse(newDir.exists());
        assertTrue(newDir.mkdir());
    }

    private void deleteDir(Path path) throws IOException {
        File dir = path.toFile();
        if (dir.exists()) FileUtils.deleteDirectory(dir);
    }

    private void initService(){
        this.createDir(this.REPOSITORY_PATH.toAbsolutePath());
        this.createDir(this.SERVICE_BASE_PATH.toAbsolutePath());
        this.eccoService = new EccoService();
        this.eccoService.setRepositoryDir(this.REPOSITORY_PATH.toAbsolutePath());
        this.eccoService.setBaseDir(this.SERVICE_BASE_PATH.toAbsolutePath());
        this.eccoService.init();
        this.repository = (Repository.Op) this.eccoService.getRepository();
    }

    private void cleanupService() throws IOException {
        this.deleteDir(this.REPOSITORY_PATH.toAbsolutePath());
        this.deleteDir(this.SERVICE_BASE_PATH.toAbsolutePath());
    }

    @Test
    public void readFeatureTracesTest(){
        this.eccoService.commit();
        assertFalse(this.repository.getFeatureTraces().isEmpty());
    }
}
