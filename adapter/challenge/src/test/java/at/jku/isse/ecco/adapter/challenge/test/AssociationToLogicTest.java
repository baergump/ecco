package at.jku.isse.ecco.adapter.challenge.test;

import at.jku.isse.ecco.repository.Repository;
import at.jku.isse.ecco.service.EccoService;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AssociationToLogicTest {

    private final Path REPOSITORY_PATH = Paths.get("src", "test","resources", "repo").toAbsolutePath();
    private final Path TRACELESS_VARIANT1 = Paths.get("src", "test", "resources", "traceless_variant_1").toAbsolutePath();
    private final Path TRACELESS_VARIANT2 = Paths.get("src", "test", "resources", "traceless_variant_2").toAbsolutePath();

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
        this.createDir(this.REPOSITORY_PATH);
        this.eccoService = new EccoService();
        this.eccoService.setRepositoryDir(this.REPOSITORY_PATH.resolve(".ecco"));
        this.eccoService.init();
        this.repository = (Repository.Op) this.eccoService.getRepository();
    }

    private void cleanupService() throws IOException {
        this.deleteDir(this.REPOSITORY_PATH.toAbsolutePath());
    }

    private void commitVariantByPath(Path path){
        this.eccoService.setBaseDir(path);
        this.eccoService.commit();
    }

}
