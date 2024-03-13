package at.jku.isse.ecco.service.test;

import at.jku.isse.ecco.adapter.text.TextReader;
import at.jku.isse.ecco.core.Association;
import at.jku.isse.ecco.feature.Configuration;
import at.jku.isse.ecco.feature.Feature;
import at.jku.isse.ecco.feature.FeatureRevision;
import at.jku.isse.ecco.featuretracerecording.FeatureTrace;
import at.jku.isse.ecco.featuretracerecording.FeatureTraceCondition;
import at.jku.isse.ecco.module.Module;
import at.jku.isse.ecco.module.ModuleRevision;
import at.jku.isse.ecco.repository.Repository;
import at.jku.isse.ecco.service.EccoService;
import at.jku.isse.ecco.storage.mem.dao.MemEntityFactory;
import at.jku.isse.ecco.storage.mem.feature.MemFeature;
import at.jku.isse.ecco.storage.mem.feature.MemFeatureRevision;
import at.jku.isse.ecco.storage.mem.featuretrace.MemFeatureTrace;
import at.jku.isse.ecco.storage.mem.featuretrace.MemFeatureTraceCondition;
import at.jku.isse.ecco.storage.mem.tree.MemRootNode;
import at.jku.isse.ecco.tree.Node;
import at.jku.isse.ecco.util.Trees;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class FtrTest {

    private final Path REPOSITORY_PATH = Paths.get("src","test","resources", "test_repository", "Repository");
    private EccoService eccoService;
    private Repository.Op repository;
    private TextReader textReader;

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
        this.textReader = new TextReader(new MemEntityFactory());
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
    
    // Recorded Feature Traces refine Mappings
    @Test
    public void tracesRefineMappingsTest(){
        // make first commit
        Path variant1FolderPath = Paths.get("src","test","resources", "test_repository", "F1F2");
        Path[] variant1Files = new Path[]{Paths.get("text_file.txt")};
        Set<Node.Op> nodes1 = this.textReader.read(variant1FolderPath.toAbsolutePath(), variant1Files);
        Configuration config1 = this.eccoService.parseConfigurationString(this.eccoService.getConfigStringFromFile(variant1FolderPath));
        this.repository.extract(config1, nodes1, "testCommitter");

        Collection<Association.Op> associations = (Collection<Association.Op>) this.repository.getAssociations();
        for (Association a : this.repository.getAssociations()) {
            System.out.println("A(" + a.getRootNode().countArtifacts() + "): " + a.computeCondition().toString());
        }

        this.eccoService.store();

        Node.Op ftrTreeRootNode = associations.iterator().next().getRootNode().copySingleNode();
        ftrTreeRootNode.setUnique(false);
        Node.Op pluginNode = ftrTreeRootNode.getChildren().get(0);
        pluginNode.setUnique(false);
        List<Node.Op> lineNodes = (List<Node.Op>) pluginNode.getChildren();
        // remove second line
        lineNodes.remove(1);

        Collection<Feature> features = (Collection<Feature>) this.repository.getFeatures();
        Feature feature1 = features.stream().filter(x -> x.getName().equals("F2")).findFirst().get();
        FeatureRevision feature1revision1 = feature1.getLatestRevision();
        Feature[] onlyF1Array = {feature1};
        FeatureRevision[] onlyF1R1Array = {feature1revision1};
        Feature[] emptyFeatureArray = {};

        Module onlyF1Module = this.repository.getModule(onlyF1Array, emptyFeatureArray);
        ModuleRevision onlyF1ModuleRevision = onlyF1Module.getRevision(onlyF1R1Array, emptyFeatureArray);

        Collection<ModuleRevision> positiveModuleRevisions = new HashSet<>();
        Collection<ModuleRevision> negativeModuleRevisions = new HashSet<>();
        positiveModuleRevisions.add(onlyF1ModuleRevision);

        FeatureTraceCondition presenceCondition = new MemFeatureTraceCondition(positiveModuleRevisions, negativeModuleRevisions);

        FeatureTrace featureTrace = new MemFeatureTrace(ftrTreeRootNode, presenceCondition);
        this.repository.addFeatureTrace(featureTrace);

        this.eccoService.store();

        this.eccoService.checkout("F1.1");
    }

    // Recorded Feature Traces match Mappings
    @Test
    public void tracesMatchMappingsTest(){
        // make first commit
        Path variant1FolderPath = Paths.get("src","test","resources", "test_repository", "F1");
        Path[] variant1Files = new Path[]{Paths.get("text_file.txt")};
        Set<Node.Op> nodes1 = this.textReader.read(variant1FolderPath.toAbsolutePath(), variant1Files);
        Configuration config1 = this.eccoService.parseConfigurationString(this.eccoService.getConfigStringFromFile(variant1FolderPath));
        this.repository.extract(config1, nodes1, "testCommitter");

        Collection<Association.Op> associations = (Collection<Association.Op>) this.repository.getAssociations();
        for (Association a : this.repository.getAssociations()) {
            System.out.println("A(" + a.getRootNode().countArtifacts() + "): " + a.computeCondition().toString());
        }

        this.eccoService.store();

        Node.Op ftrTreeRootNode = associations.iterator().next().getRootNode().copySingleNode();
        ftrTreeRootNode.setUnique(false);
        Node.Op pluginNode = ftrTreeRootNode.getChildren().get(0);
        pluginNode.setUnique(false);

        Collection<Feature> features = (Collection<Feature>) this.repository.getFeatures();
        Feature feature1 = features.stream().filter(x -> x.getName().equals("F1")).findFirst().get();
        FeatureRevision feature1revision1 = feature1.getLatestRevision();
        Feature[] onlyF1Array = {feature1};
        FeatureRevision[] onlyF1R1Array = {feature1revision1};
        Feature[] emptyFeatureArray = {};

        Module onlyF1Module = this.repository.getModule(onlyF1Array, emptyFeatureArray);
        ModuleRevision onlyF1ModuleRevision = onlyF1Module.getRevision(onlyF1R1Array, emptyFeatureArray);

        Collection<ModuleRevision> positiveModuleRevisions = new HashSet<>();
        Collection<ModuleRevision> negativeModuleRevisions = new HashSet<>();
        positiveModuleRevisions.add(onlyF1ModuleRevision);

        FeatureTraceCondition presenceCondition = new MemFeatureTraceCondition(positiveModuleRevisions, negativeModuleRevisions);

        FeatureTrace featureTrace = new MemFeatureTrace(ftrTreeRootNode, presenceCondition);
        this.repository.addFeatureTrace(featureTrace);

        this.eccoService.store();

        this.eccoService.checkout("F1.1");
    }

    // Recorded Feature Traces contradict Mappings
    @Test
    public void tracesContradictMappingsTest(){
        // make first commit
        Path variant1FolderPath = Paths.get("src","test","resources", "test_repository", "F1_Contradiction");
        Path[] variant1Files = new Path[]{Paths.get("text_file.txt")};
        Set<Node.Op> nodes1 = this.textReader.read(variant1FolderPath.toAbsolutePath(), variant1Files);
        Configuration config1 = this.eccoService.parseConfigurationString(this.eccoService.getConfigStringFromFile(variant1FolderPath));
        this.repository.extract(config1, nodes1, "testCommitter");

        Collection<Association.Op> associations = (Collection<Association.Op>) this.repository.getAssociations();
        for (Association a : this.repository.getAssociations()) {
            System.out.println("A(" + a.getRootNode().countArtifacts() + "): " + a.computeCondition().toString());
        }

        this.eccoService.store();

        // create feature trace of new line to new feature
        Path variant2FolderPath = Paths.get("src","test","resources", "test_repository", "F1_Contradiction");
        Path[] variant2Files = new Path[]{Paths.get("text_file.txt")};
        Set<Node.Op> newNodes = this.textReader.read(variant2FolderPath.toAbsolutePath(), variant2Files);
        // make only new node unique
        Node.Op newPluginNode = newNodes.iterator().next();
        newPluginNode.setUnique(false);
        Node.Op rootNode = new MemRootNode();
        rootNode.addChild(newPluginNode);

        // create new Feature
        String newFeatureId = UUID.randomUUID().toString();
        Feature newFeature = new MemFeature(newFeatureId, "F1");
        FeatureRevision newFeatureRevision = new MemFeatureRevision(newFeature, "1");
        newFeature.addRevision(newFeatureRevision.getId());

        Feature[] onlyF2Array = {newFeature};
        FeatureRevision[] onlyF2R1Array = {newFeatureRevision};

        Feature[] emptyFeatureArray = {};

        Module onlyF2Module = this.repository.addModule(onlyF2Array, emptyFeatureArray);
        ModuleRevision onlyF3ModuleRevision = onlyF2Module.addRevision(onlyF2R1Array, emptyFeatureArray);

        Collection<ModuleRevision> positiveModuleRevisions = new HashSet<>();
        Collection<ModuleRevision> negativeModuleRevisions = new HashSet<>();
        positiveModuleRevisions.add(onlyF3ModuleRevision);

        FeatureTraceCondition presenceCondition = new MemFeatureTraceCondition(positiveModuleRevisions, negativeModuleRevisions);


        FeatureTrace featureTrace = new MemFeatureTrace(rootNode, presenceCondition);
        this.repository.addFeatureTrace(featureTrace);

        this.eccoService.store();

        this.eccoService.checkout("F2.1");
    }

    // Recorded Feature Traces refine and extend Mappings
    @Test
    public void tracesExtendsMappingsTest(){
        // commit file with two lines and two features
        Path variant1FolderPath = Paths.get("src","test","resources", "test_repository", "F1");
        Path[] variant1Files = new Path[]{Paths.get("text_file.txt")};
        Set<Node.Op> nodes1 = this.textReader.read(variant1FolderPath.toAbsolutePath(), variant1Files);
        Configuration config1 = this.eccoService.parseConfigurationString(this.eccoService.getConfigStringFromFile(variant1FolderPath));
        this.repository.extract(config1, nodes1, "testCommitter");

        Collection<Association.Op> associations = (Collection<Association.Op>) this.repository.getAssociations();
        for (Association a : this.repository.getAssociations()) {
            System.out.println("A(" + a.getRootNode().countArtifacts() + "): " + a.computeCondition().toString());
        }

        this.eccoService.store();

        // create feature trace of new line to new feature
        Path variant2FolderPath = Paths.get("src","test","resources", "test_repository", "F2");
        Path[] variant2Files = new Path[]{Paths.get("text_file.txt")};
        Set<Node.Op> newNodes = this.textReader.read(variant2FolderPath.toAbsolutePath(), variant2Files);
        // make only new node unique
        Node.Op newPluginNode = newNodes.iterator().next();
        newPluginNode.setUnique(false);
        Node.Op rootNode = new MemRootNode();
        rootNode.addChild(newPluginNode);

        // create new Feature
        String newFeatureId = UUID.randomUUID().toString();
        Feature newFeature = new MemFeature(newFeatureId, "F2");
        FeatureRevision newFeatureRevision = new MemFeatureRevision(newFeature, "1");
        newFeature.addRevision(newFeatureRevision.getId());

        Feature[] onlyF3Array = {newFeature};
        FeatureRevision[] onlyF3R1Array = {newFeatureRevision};

        Feature[] emptyFeatureArray = {};

        Module onlyF3Module = this.repository.addModule(onlyF3Array, emptyFeatureArray);
        ModuleRevision onlyF3ModuleRevision = onlyF3Module.addRevision(onlyF3R1Array, emptyFeatureArray);

        Collection<ModuleRevision> positiveModuleRevisions = new HashSet<>();
        Collection<ModuleRevision> negativeModuleRevisions = new HashSet<>();
        positiveModuleRevisions.add(onlyF3ModuleRevision);

        FeatureTraceCondition presenceCondition = new MemFeatureTraceCondition(positiveModuleRevisions, negativeModuleRevisions);

        Trees.sequence(rootNode);
        for (Association association : associations){
            Trees.mergePartialOrderGraphs(rootNode, (Node.Op) association.getRootNode());
        }


        FeatureTrace featureTrace = new MemFeatureTrace(rootNode, presenceCondition);
        this.repository.addFeatureTrace(featureTrace);

        this.eccoService.store();

        // checkout all three features
        this.eccoService.checkout("F1.1, F2.1, F3.1");
    }

    // Recorded Feature Traces refine and extend Mappings
    @Test
    public void tracesRefineAndExtendMappingsTest(){
        // commit file with two lines and two features
        Path variant1FolderPath = Paths.get("src","test","resources", "test_repository", "F1F2");
        Path[] variant1Files = new Path[]{Paths.get("text_file.txt")};
        Set<Node.Op> nodes1 = this.textReader.read(variant1FolderPath.toAbsolutePath(), variant1Files);
        Configuration config1 = this.eccoService.parseConfigurationString(this.eccoService.getConfigStringFromFile(variant1FolderPath));
        this.repository.extract(config1, nodes1, "testCommitter");

        Collection<Association.Op> associations = (Collection<Association.Op>) this.repository.getAssociations();
        for (Association a : this.repository.getAssociations()) {
            System.out.println("A(" + a.getRootNode().countArtifacts() + "): " + a.computeCondition().toString());
        }

        this.eccoService.store();

        // create feature trace for one line to one feature
        Node.Op ftrTreeRootNode = associations.iterator().next().getRootNode().copySingleNode();
        ftrTreeRootNode.setUnique(false);
        Node.Op pluginNode = ftrTreeRootNode.getChildren().get(0);
        pluginNode.setUnique(false);
        List<Node.Op> lineNodes = (List<Node.Op>) pluginNode.getChildren();
        // remove second line
        lineNodes.remove(1);

        Collection<Feature> features = (Collection<Feature>) this.repository.getFeatures();
        Feature feature1 = features.stream().filter(x -> x.getName().equals("F2")).findFirst().get();
        FeatureRevision feature1revision1 = feature1.getLatestRevision();
        Feature[] onlyF1Array = {feature1};
        FeatureRevision[] onlyF1R1Array = {feature1revision1};
        Feature[] emptyFeatureArray = {};

        Module onlyF1Module = this.repository.getModule(onlyF1Array, emptyFeatureArray);
        ModuleRevision onlyF1ModuleRevision = onlyF1Module.getRevision(onlyF1R1Array, emptyFeatureArray);

        Collection<ModuleRevision> positiveModuleRevisions = new HashSet<>();
        Collection<ModuleRevision> negativeModuleRevisions = new HashSet<>();
        positiveModuleRevisions.add(onlyF1ModuleRevision);

        FeatureTraceCondition presenceCondition = new MemFeatureTraceCondition(positiveModuleRevisions, negativeModuleRevisions);

        FeatureTrace featureTrace = new MemFeatureTrace(ftrTreeRootNode, presenceCondition);
        this.repository.addFeatureTrace(featureTrace);

        this.eccoService.store();

        // create feature trace of new line to new feature
        Path variant2FolderPath = Paths.get("src","test","resources", "test_repository", "F3");
        Path[] variant2Files = new Path[]{Paths.get("text_file.txt")};
        Set<Node.Op> newNodes = this.textReader.read(variant2FolderPath.toAbsolutePath(), variant2Files);
        // make only new node unique
        Node.Op newPluginNode = newNodes.iterator().next();
        newPluginNode.setUnique(false);
        Node.Op rootNode = new MemRootNode();
        rootNode.addChild(newPluginNode);

        // create new Feature
        String newFeatureId = UUID.randomUUID().toString();
        Feature newFeature = new MemFeature(newFeatureId, "F3");
        FeatureRevision newFeatureRevision = new MemFeatureRevision(newFeature, "1");
        newFeature.addRevision(newFeatureRevision.getId());

        Feature[] onlyF3Array = {newFeature};
        FeatureRevision[] onlyF3R1Array = {newFeatureRevision};

        Module onlyF3Module = this.repository.addModule(onlyF3Array, emptyFeatureArray);
        ModuleRevision onlyF3ModuleRevision = onlyF3Module.addRevision(onlyF3R1Array, emptyFeatureArray);

        positiveModuleRevisions = new HashSet<>();
        negativeModuleRevisions = new HashSet<>();
        positiveModuleRevisions.add(onlyF3ModuleRevision);

        presenceCondition = new MemFeatureTraceCondition(positiveModuleRevisions, negativeModuleRevisions);

        Trees.sequence(rootNode);
        for (Association association : associations){
            Trees.mergePartialOrderGraphs(rootNode, (Node.Op) association.getRootNode());
        }

        featureTrace = new MemFeatureTrace(rootNode, presenceCondition);
        this.repository.addFeatureTrace(featureTrace);

        this.eccoService.store();

        // checkout all three features
        this.eccoService.checkout("F1.1, F2.1, F3.1");
    }
}
