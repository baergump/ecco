package at.jku.isse.ecco.adapter.challenge.test;

import at.jku.isse.ecco.repository.Repository;
import at.jku.isse.ecco.service.EccoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class FeatureTraceTest {

    private final Path REPOSITORY_PATH = Paths.get("src","integrationTest","resources", "test_repository");
    private EccoService eccoService;
    private Repository.Op repository;

    @BeforeEach
    public void setup() throws IOException {
        this.deleteRepository();
        this.createRepository();
    }

    @AfterEach
    public void teardown(){
        if (this.eccoService != null){
            this.eccoService.close();
        }
    }

    private void createRepository(){
        this.eccoService = new EccoService();
        this.eccoService.setRepositoryDir(this.REPOSITORY_PATH.resolve(".ecco").toAbsolutePath());
        this.eccoService.setBaseDir(this.REPOSITORY_PATH.toAbsolutePath());
        this.eccoService.init();
        //this.reader = new JavaChallengeReader(new MemEntityFactory());
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
    public void readFeatureTracesTest(){
        this.eccoService.commit();
        assertFalse(this.repository.getFeatureTraces().isEmpty());
    }
}
