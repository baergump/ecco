package at.jku.isse.ecco.adapter.challenge.test;

import at.jku.isse.ecco.adapter.challenge.data.AbstractArtifactData;
import at.jku.isse.ecco.adapter.challenge.data.ClassArtifactData;
import at.jku.isse.ecco.adapter.challenge.data.LineArtifactData;
import at.jku.isse.ecco.artifact.Artifact;
import at.jku.isse.ecco.artifact.ArtifactData;
import at.jku.isse.ecco.core.Checkout;
import at.jku.isse.ecco.feature.Configuration;
import at.jku.isse.ecco.repository.Repository;
import at.jku.isse.ecco.service.EccoService;
import at.jku.isse.ecco.storage.mem.feature.MemConfiguration;
import at.jku.isse.ecco.tree.Node;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.EmptyVisitor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

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


    private final Path REPOSITORY_PATH = Paths.get("src", "test","resources", "repo").toAbsolutePath();

    // config: BASE, FEATUREA
    // artifacts: X
    // feature traces: FEATUREA-X
    private final Path TEST_VARIANT1 = Paths.get("src", "test", "resources", "test_variant_1").toAbsolutePath();

    // config: BASE, FEATUREB
    // artifacts: Y
    // feature traces: FEATUREC-Y
    private final Path TEST_VARIANT2 = Paths.get("src", "test", "resources", "test_variant_2").toAbsolutePath();

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

    private Node buildTree(Set<Node> nodes){
        // visit all nodes in order for lazy composition nodes to build the actual tree
        // returned node is lazy composition node! actual nodes are the children
        EmptyVisitor visitor = new EmptyVisitor();
        Node node = nodes.iterator().next();
        node.traverse(visitor);
        return node;
    }

    private boolean findLineArtifactData(List<Node> nodes, String line){
        for (Node node : nodes){
            ArtifactData artifactData = node.getArtifact().getData();
            if (!(artifactData instanceof LineArtifactData)) continue;
            LineArtifactData lineArtifactData = (LineArtifactData) artifactData;
            String artifactLine = lineArtifactData.getLine();
            if (line.equals(artifactLine)) return true;
        }
        return false;
    }

    private Node getMethodsNode(Node node){
        List<Node> children = (List<Node>) node.getChildren();
        for (Node child : children){
            AbstractArtifactData artifactData = (AbstractArtifactData) child.getArtifact().getData();
            if (artifactData.getId().equals("METHODS")){
                return child;
            }
        }
        return null;
    }

    @Test
    public void featureTracesExistAfterCommittingFeatureTraces(){
        this.commitVariantByPath(this.TEST_VARIANT1);
        assertFalse(this.repository.getFeatureTraces().isEmpty());
    }

    // TODO: commit without feature trace file works

    @Test
    public void featureTraceAddsArtifact(){
        this.commitVariantByPath(this.TEST_VARIANT1);
        this.commitVariantByPath(this.TEST_VARIANT2);

        Configuration configuration = this.eccoService.parseConfigurationString("BASE, FEATUREA, FEATUREC");
        Checkout checkout = this.repository.compose(configuration);
        Set<Node> nodes = this.eccoService.compareArtifacts(checkout);
        Node node = this.buildTree(nodes);

        Node dirNode = node.getChildren().get(0);
        Node pluginNode = dirNode.getChildren().get(0);
        Node fileNode = pluginNode.getChildren().get(0);
        Node methodsNode = this.getMethodsNode(fileNode);
        Node methodNode = methodsNode.getChildren().get(0);
        assertEquals(2, methodNode.getChildren().size());

        List<Node> lineNodes = (List<Node>) methodNode.getChildren();
        assertTrue(this.findLineArtifactData(lineNodes, "        System.out.println(\"Code line y\");"));
    }

    /*
    @Test
    public void traceslessVariants(){
        this.commitVariantByPath(this.TRACELESS_VARIANT1);
        this.commitVariantByPath(this.TRACELESS_VARIANT2);

        Configuration configuration = this.eccoService.parseConfigurationString("BASE, FEATUREA, FEATUREB");
        Checkout checkout = this.repository.compose(configuration);
        Set<Node> nodes = this.eccoService.compareArtifacts(checkout);

        // visit all nodes in order for lazy composition nodes to build the actual tree
        EmptyVisitor visitor = new EmptyVisitor();
        Node node = nodes.iterator().next();
        node.traverse(visitor);
    }
     */
}
