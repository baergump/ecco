package at.jku.isse.ecco.adapter.challenge.test;

import at.jku.isse.ecco.adapter.challenge.JavaChallengeReader;
import at.jku.isse.ecco.adapter.challenge.vevos.LogicToModuleTransformer;
import at.jku.isse.ecco.adapter.challenge.vevos.VEVOSConditionHandler;
import at.jku.isse.ecco.dao.EntityFactory;
import at.jku.isse.ecco.repository.Repository;
import at.jku.isse.ecco.service.EccoService;
import at.jku.isse.ecco.storage.mem.dao.MemEntityFactory;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JavaChallengeReaderTest {

    private final Path REPOSITORY_PATH = Paths.get("src", "test","resources", "test_repository");
    private final Path VARIANT_PATH = Paths.get("src", "test", "resources", "test_variant", "Variant_5");
    private final Path TEST_FILE_PATH = Paths.get("argouml-app", "src", "org", "argouml", "application", "Main.java");
    private EccoService eccoService;
    private Repository.Op repository;
    private JavaChallengeReader reader;

    @BeforeEach
    public void setup() throws IOException {
        this.deleteRepository();
        this.createRepository();
        EntityFactory factory = new MemEntityFactory();
        this.reader = new JavaChallengeReader(factory);
    }

    @AfterEach
    public void teardown() throws IOException {
        if (this.eccoService != null){
            this.eccoService.close();
        }
        this.deleteRepository();
    }

    private void createRepository(){
        this.eccoService = new EccoService();
        this.eccoService.setRepositoryDir(this.REPOSITORY_PATH.resolve(".ecco").toAbsolutePath());
        this.eccoService.setBaseDir(this.VARIANT_PATH.toAbsolutePath());
        this.eccoService.init();
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
    public void parseFileTest() throws IOException {
        Path[] filesToRead = {this.TEST_FILE_PATH};
        this.reader.read(this.VARIANT_PATH, filesToRead
                //, this.repository
                );
    }
}
