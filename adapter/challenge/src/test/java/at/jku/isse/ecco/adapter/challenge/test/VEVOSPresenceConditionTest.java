package at.jku.isse.ecco.adapter.challenge.test;

import at.jku.isse.ecco.adapter.challenge.vevos.LogicToModuleTransformer;
import at.jku.isse.ecco.adapter.challenge.vevos.VEVOSPresenceCondition;
import at.jku.isse.ecco.feature.Feature;
import at.jku.isse.ecco.feature.FeatureRevision;
import at.jku.isse.ecco.featuretracerecording.FeatureTraceCondition;
import at.jku.isse.ecco.module.ModuleRevision;
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
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class VEVOSPresenceConditionTest {

    private final Path REPOSITORY_PATH = Paths.get("src", "test","resources", "test_repository");
    private final Path VARIANT_PATH = Paths.get("src", "test", "resources", "test_variant", "Variant_5");
    private EccoService eccoService;
    private Repository.Op repository;
    private LogicToModuleTransformer logicToModuleTransformer;

    @BeforeEach
    public void setup() throws IOException {
        this.deleteRepository();
        this.createRepository();
        this.logicToModuleTransformer = new LogicToModuleTransformer(this.repository);
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
    public void parseVevosFileLineTest(){
        VEVOSPresenceCondition vevosPresenceCondition = new VEVOSPresenceCondition(
                "argouml-app\\src\\org\\argouml\\kernel\\MemberList.java;True;COGNITIVE;COGNITIVE;322;327",
                this.logicToModuleTransformer);
        assertEquals("argouml-app\\src\\org\\argouml\\kernel\\MemberList.java", vevosPresenceCondition.getFilePath().toString());
        assertEquals(322, vevosPresenceCondition.getStartLine());
        assertEquals(327, vevosPresenceCondition.getEndLine());
        FeatureTraceCondition featureTraceCondition = vevosPresenceCondition.getFeatureTraceConditions();
        Collection<ModuleRevision> positiveModuleRevisions = featureTraceCondition.getPositiveModuleRevisions();
        Collection<ModuleRevision> negativeModuleRevisions = featureTraceCondition.getNegativeModuleRevisions();
        assertEquals(1, positiveModuleRevisions.size());
        assertEquals(0, negativeModuleRevisions.size());

        ModuleRevision positiveModuleRevision = positiveModuleRevisions.iterator().next();
        FeatureRevision[] positiveFeatures = positiveModuleRevision.getPos();
        Feature[] negativeFeatures = positiveModuleRevision.getNeg();
        assertEquals(1, positiveFeatures.length);
        assertEquals(0, negativeFeatures.length);

        Feature feature = positiveFeatures[0].getFeature();
        assertEquals("COGNITIVE", feature.getName());
    }
}
