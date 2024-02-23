package at.jku.isse.ecco.adapter.challenge.test;

import at.jku.isse.ecco.adapter.challenge.vevos.LogicToModuleTransformer;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LogicTransformationTest {

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
    public void singleFeatureTest(){
        FeatureTraceCondition featureTraceCondition = this.logicToModuleTransformer.transformLogicalConditionToFeatureTraceCondition("FEATUREA");

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
        assertEquals("FEATUREA", feature.getName());
    }

    @Test
    public void negateFeatureTest(){
        FeatureTraceCondition featureTraceCondition = this.logicToModuleTransformer.transformLogicalConditionToFeatureTraceCondition("!FEATUREA");

        Collection<ModuleRevision> positiveModuleRevisions = featureTraceCondition.getPositiveModuleRevisions();
        Collection<ModuleRevision> negativeModuleRevisions = featureTraceCondition.getNegativeModuleRevisions();
        assertEquals(0, positiveModuleRevisions.size());
        assertEquals(1, negativeModuleRevisions.size());

        ModuleRevision negativeModuleRevision = negativeModuleRevisions.iterator().next();
        FeatureRevision[] positiveFeatures = negativeModuleRevision.getPos();
        Feature[] negativeFeatures = negativeModuleRevision.getNeg();
        assertEquals(1, positiveFeatures.length);
        assertEquals(0, negativeFeatures.length);

        Feature feature = positiveFeatures[0].getFeature();
        assertEquals("FEATUREA", feature.getName());
    }

    @Test
    public void conjunctionTest() {
        FeatureTraceCondition featureTraceCondition = this.logicToModuleTransformer.transformLogicalConditionToFeatureTraceCondition("(FEATUREA && FEATUREB)");

        Collection<ModuleRevision> positiveModuleRevisions = featureTraceCondition.getPositiveModuleRevisions();
        Collection<ModuleRevision> negativeModuleRevisions = featureTraceCondition.getNegativeModuleRevisions();
        assertEquals(1, positiveModuleRevisions.size());
        assertEquals(0, negativeModuleRevisions.size());

        ModuleRevision positiveModuleRevision = positiveModuleRevisions.iterator().next();
        FeatureRevision[] positiveFeatures = positiveModuleRevision.getPos();
        Feature[] negativeFeatures = positiveModuleRevision.getNeg();
        assertEquals(2, positiveFeatures.length);
        assertEquals(0, negativeFeatures.length);

        List<FeatureRevision> featureAs = Arrays.stream(positiveFeatures).filter(f -> f.getFeature().getName().equals("FEATUREA")).collect(Collectors.toList());
        List<FeatureRevision> featureBs = Arrays.stream(positiveFeatures).filter(f -> f.getFeature().getName().equals("FEATUREB")).collect(Collectors.toList());
        assertEquals(1, featureAs.size());
        assertEquals(1, featureBs.size());
    }

    @Test
    public void multipleConjunctionTest() {
        FeatureTraceCondition featureTraceCondition = this.logicToModuleTransformer.transformLogicalConditionToFeatureTraceCondition("(FEATUREA && FEATUREB && FEATUREC)");

        Collection<ModuleRevision> positiveModuleRevisions = featureTraceCondition.getPositiveModuleRevisions();
        Collection<ModuleRevision> negativeModuleRevisions = featureTraceCondition.getNegativeModuleRevisions();
        assertEquals(1, positiveModuleRevisions.size());
        assertEquals(0, negativeModuleRevisions.size());

        ModuleRevision positiveModuleRevision = positiveModuleRevisions.iterator().next();
        FeatureRevision[] positiveFeatures = positiveModuleRevision.getPos();
        Feature[] negativeFeatures = positiveModuleRevision.getNeg();
        assertEquals(3, positiveFeatures.length);
        assertEquals(0, negativeFeatures.length);

        List<FeatureRevision> featureAs = Arrays.stream(positiveFeatures).filter(f -> f.getFeature().getName().equals("FEATUREA")).collect(Collectors.toList());
        List<FeatureRevision> featureBs = Arrays.stream(positiveFeatures).filter(f -> f.getFeature().getName().equals("FEATUREB")).collect(Collectors.toList());
        List<FeatureRevision> featureCs = Arrays.stream(positiveFeatures).filter(f -> f.getFeature().getName().equals("FEATUREC")).collect(Collectors.toList());
        assertEquals(1, featureAs.size());
        assertEquals(1, featureBs.size());
        assertEquals(1, featureCs.size());

    }

    @Test
    public void disjunctionTest() {
        FeatureTraceCondition featureTraceCondition = this.logicToModuleTransformer.transformLogicalConditionToFeatureTraceCondition("(FEATUREA || FEATUREB)");

        Collection<ModuleRevision> positiveModuleRevisions = featureTraceCondition.getPositiveModuleRevisions();
        Collection<ModuleRevision> negativeModuleRevisions = featureTraceCondition.getNegativeModuleRevisions();
        assertEquals(2, positiveModuleRevisions.size());
        assertEquals(0, negativeModuleRevisions.size());

        Iterator<ModuleRevision> iterator = positiveModuleRevisions.iterator();
        ModuleRevision moduleRevision1 = iterator.next();
        ModuleRevision moduleRevision2 = iterator.next();

        FeatureRevision[] positiveFeatures = moduleRevision1.getPos();
        Feature[] negativeFeatures = moduleRevision1.getNeg();
        assertEquals(1, positiveFeatures.length);
        assertEquals(0, negativeFeatures.length);

        Feature feature = positiveFeatures[0].getFeature();
        assertEquals("FEATUREB", feature.getName());

        positiveFeatures = moduleRevision2.getPos();
        negativeFeatures = moduleRevision2.getNeg();
        assertEquals(1, positiveFeatures.length);
        assertEquals(0, negativeFeatures.length);

        feature = positiveFeatures[0].getFeature();
        assertEquals("FEATUREA", feature.getName());
    }

    @Test
    public void testMultipleParentheses(){
        FeatureTraceCondition featureTraceCondition = this.logicToModuleTransformer.transformLogicalConditionToFeatureTraceCondition("((FEATUREA || FEATUREB) && FEATUREC)");
    }

    @Test
    public void negationOfDisjunctionThrowsExceptionTest(){
        assertThrows(RuntimeException.class, () -> this.logicToModuleTransformer.transformLogicalConditionToFeatureTraceCondition("!(FEATUREA || FEATUREB)"));
    }

    @Test
    public void negationOfConjunctionThrowsExceptionTest(){
        assertThrows(RuntimeException.class, () -> this.logicToModuleTransformer.transformLogicalConditionToFeatureTraceCondition("!(FEATUREA || FEATUREB)"));
    }

    @Test
    public void spaceWithoutParenthesesThrowsExceptionTest(){
        assertThrows(RuntimeException.class, () -> this.logicToModuleTransformer.transformLogicalConditionToFeatureTraceCondition("FEATUREA || FEATUREB"));
    }
}
