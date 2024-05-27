package at.jku.isse.ecco.adapter.challenge.test;

import at.jku.isse.ecco.adapter.challenge.data.AbstractArtifactData;
import at.jku.isse.ecco.adapter.challenge.data.ClassArtifactData;
import at.jku.isse.ecco.adapter.challenge.data.LineArtifactData;
import at.jku.isse.ecco.adapter.challenge.data.MethodArtifactData;
import at.jku.isse.ecco.adapter.dispatch.DirectoryArtifactData;
import at.jku.isse.ecco.adapter.dispatch.PluginArtifactData;
import at.jku.isse.ecco.artifact.ArtifactData;
import at.jku.isse.ecco.featuretrace.FeatureTrace;
import at.jku.isse.ecco.repository.Repository;
import at.jku.isse.ecco.service.EccoService;
import at.jku.isse.ecco.storage.mem.featuretrace.MemFeatureTrace;
import at.jku.isse.ecco.tree.EmptyVisitor;
import at.jku.isse.ecco.tree.Node;
import at.jku.isse.ecco.tree.RootNode;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class FeatureTraceTest {


    // TODO: put this test class in another module? service?
    // TODO: test tree builds with feature traces including order


    private final Path REPOSITORY_PATH = Paths.get("src", "test","resources", "repo").toAbsolutePath();

    // config: BASE, FEATUREA
    // artifacts: X
    // feature traces: FEATUREA-X
    private final Path TEST_VARIANT1 = Paths.get("src", "test", "resources", "test_variant_1").toAbsolutePath();

    // config: BASE, FEATUREB
    // artifacts: Y
    // feature traces: FEATUREC-Y
    private final Path TEST_VARIANT2 = Paths.get("src", "test", "resources", "test_variant_2").toAbsolutePath();

    private final Path WHOLE_CLASS_TRACE_VARIANT = Paths.get("src", "test", "resources", "test_variant_whole_class_trace").toAbsolutePath();

    private final Path WHOLE_METHOD_TRACE_VARIANT = Paths.get("src", "test", "resources", "test_variant_whole_method_trace").toAbsolutePath();

    private final Path TRACELESS_VARIANT1 = Paths.get("src", "test", "resources", "traceless_variant_1").toAbsolutePath();
    private final Path TRACELESS_VARIANT2 = Paths.get("src", "test", "resources", "traceless_variant_2").toAbsolutePath();

    private EccoService eccoService;
    private Repository.Op repository;

    @BeforeEach
    public void setup() throws IOException {
        //this.cleanupService();
        this.initService();
    }

    @AfterEach
    public void teardown() throws IOException {
        if (this.eccoService != null) this.eccoService.close();
        //this.cleanupService();
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
        //this.createDir(this.REPOSITORY_PATH);
        this.eccoService = new EccoService();
        this.eccoService.setRepositoryDir(this.REPOSITORY_PATH.resolve(".ecco"));
        //this.eccoService.init();
        this.eccoService.open();
        this.repository = (Repository.Op) this.eccoService.getRepository();
        System.out.println("test");
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

    private Node.Op getDirectoryChild(Node.Op node, String path){
        List<Node.Op> children = node.getChildren().stream()
                .filter(n -> n.getArtifact().getData() instanceof DirectoryArtifactData)
                .filter(n -> ((DirectoryArtifactData)n.getArtifact().getData()).getPath().toString().equals(path))
                .collect(Collectors.toList());
        return children.get(0);
    }

    private Node.Op getPluginChild(Node.Op node, String pluginName){
        List<Node.Op> children = node.getChildren().stream()
                .filter(n -> n.getArtifact().getData() instanceof PluginArtifactData)
                .filter(n -> ((PluginArtifactData) n.getArtifact().getData()).getPluginId().equals(pluginName))
                .collect(Collectors.toList());
        return children.get(0);
    }

    private Node.Op getClassChild(Node.Op node, String className){
        List<Node.Op> children = node.getChildren().stream()
                .filter(n -> n.getArtifact().getData() instanceof ClassArtifactData)
                .filter(n -> ((ClassArtifactData) n.getArtifact().getData()).getName().equals(className))
                .collect(Collectors.toList());
        return children.get(0);
    }

    private Node.Op getAbstractChild(Node.Op node, String abstractId){
        List<Node.Op> children = node.getChildren().stream()
                .filter(n -> n.getArtifact().getData() instanceof AbstractArtifactData)
                .filter(n -> ((AbstractArtifactData) n.getArtifact().getData()).getId().equals(abstractId))
                .collect(Collectors.toList());
        return children.get(0);
    }

    private Node.Op getMethodChild(Node.Op node, String methodSignature){
        List<Node.Op> children = node.getChildren().stream()
                .filter(n -> n.getArtifact().getData() instanceof MethodArtifactData)
                .filter(n -> ((MethodArtifactData) n.getArtifact().getData()).getSignature().equals(methodSignature))
                .collect(Collectors.toList());
        return children.get(0);
    }

    private Node.Op getLineChild(Node.Op node, String line){
        List<Node.Op> children = node.getChildren().stream()
                .filter(n -> n.getArtifact().getData() instanceof LineArtifactData)
                .filter(n -> ((LineArtifactData) n.getArtifact().getData()).getLine().equals(line))
                .collect(Collectors.toList());
        return children.get(0);
    }

    private boolean containsFeatureTraceAX(FeatureTrace featureTrace){
        // the respective tree may contain further feature traces and nodes
        MemFeatureTrace memTrace = (MemFeatureTrace) featureTrace;
        Node.Op node = (Node.Op) memTrace.getNode();
        if(node == null){ return false; }
        assertSame(featureTrace, node.getFeatureTrace());

        ArtifactData artifactData = node.getArtifact().getData();
        assertTrue(artifactData instanceof LineArtifactData);
        assertEquals("        System.out.println(\"Code line x\");", ((LineArtifactData) artifactData).getLine());

        Node.Op root = (Node.Op) node.getRoot();
        assertTrue(root instanceof RootNode);

        Node.Op child = this.getDirectoryChild(root, "");
        child = this.getDirectoryChild(child, "test_product");
        child = this.getPluginChild(child, "at.jku.isse.ecco.adapter.challenge.JavaPlugin");
        child = this.getClassChild(child, "at.jku.isse.ecco.adapter.challenge.TestClass");
        child = this.getAbstractChild(child, "METHODS");
        child = this.getMethodChild(child, "TestMethod()");
        child = this.getLineChild(child, "        System.out.println(\"Code line x\");");

        MemFeatureTrace traceComparison = new MemFeatureTrace(node);
        traceComparison.addUserCondition("FEATUREA");
        assertEquals(memTrace, traceComparison);
        return true;
    }

    private boolean containsFeatureTraceCY(FeatureTrace featureTrace) {
        // the respective tree may contain further feature traces and nodes
        MemFeatureTrace memTrace = (MemFeatureTrace) featureTrace;
        Node.Op node = (Node.Op) memTrace.getNode();
        assertNotNull(node);
        assertSame(featureTrace, node.getFeatureTrace());

        ArtifactData artifactData = node.getArtifact().getData();
        assertTrue(artifactData instanceof LineArtifactData);
        assertEquals("        System.out.println(\"Code line x\");", ((LineArtifactData) artifactData).getLine());

        Node.Op root = (Node.Op) node.getRoot();
        assertTrue(root instanceof RootNode);

        Node.Op child = this.getDirectoryChild(root, "");
        child = this.getDirectoryChild(child, "test_product");
        child = this.getPluginChild(child, "at.jku.isse.ecco.adapter.challenge.JavaPlugin");
        child = this.getClassChild(child, "at.jku.isse.ecco.adapter.challenge.TestClass");
        child = this.getAbstractChild(child, "METHODS");
        child = this.getMethodChild(child, "TestMethod()");
        child = this.getLineChild(child, "        System.out.println(\"Code line y\");");

        MemFeatureTrace traceComparison = new MemFeatureTrace(node);
        traceComparison.addUserCondition("FEATUREC");
        assertEquals(memTrace, traceComparison);
        return true;
    }

    // test todos:
    // inserted configuration in nodes is correct
    // the feature traces are correct after commit with single feature-trace
    // the feature traces are correct after commit with multiple feature-traces
    // feature traces are still correct after closing and opening a repository
    // (Trees::extractFeatureTraceTree)
    // (commit without feature trace file still works)

    // ((build tree) add node with feature trace (commit variant with feature A and node; commit variant with feature B and node as feature trace for feature C; build tree with feature A and C))
    // ((build tree) remove node with feature trace (commit variant with feature A and B with two nodes; commit variant with feature A and one node and feature trace to A; build tree with B))

    @Test
    public void dummyTest(){
        System.out.println("");
    }

    @Test
    public void someFeatureTracesExistAfterCommit(){
        this.commitVariantByPath(this.TEST_VARIANT1);
        assertFalse(this.repository.getFeatureTraces().isEmpty());
    }

    @Test void wholeClassGetsUserCondition(){
        this.commitVariantByPath(this.WHOLE_CLASS_TRACE_VARIANT);
        Collection<FeatureTrace> traces = this.repository.getFeatureTraces();
        assertTrue(traces.stream().anyMatch(trace -> {
            ArtifactData classData = trace.getNode().getArtifact().getData();
            return (classData instanceof ClassArtifactData && trace.containsUserCondition());
        }));
    }

    @Test void wholeMethodGetsUserCondition(){
        this.commitVariantByPath(this.WHOLE_METHOD_TRACE_VARIANT);
        Collection<FeatureTrace> traces = this.repository.getFeatureTraces();
        assertTrue(traces.stream().anyMatch(trace -> {
            ArtifactData methodData = trace.getNode().getArtifact().getData();
            return (methodData instanceof MethodArtifactData && trace.containsUserCondition());
        }));
    }

    @Test
    public void testSingleFeatureTraceCommit(){
        /*
        todo
        this.commitVariantByPath(this.TEST_VARIANT1);
        Collection<FeatureTrace> traces = this.repository.getFeatureTraces();

        MemFeatureTrace memTrace = (MemFeatureTrace) featureTrace;
        Node.Op node = (Node.Op) memTrace.getNode();
        if(node == null){ return false; }
        assertSame(featureTrace, node.getFeatureTrace());

        ArtifactData artifactData = node.getArtifact().getData();
        assertTrue(artifactData instanceof LineArtifactData);
        assertEquals("        System.out.println(\"Code line x\");", ((LineArtifactData) artifactData).getLine());

        Node.Op root = (Node.Op) node.getRoot();
        assertTrue(root instanceof RootNode);

        Node.Op child = this.getDirectoryChild(root, "");
        child = this.getDirectoryChild(child, "test_product");
        child = this.getPluginChild(child, "at.jku.isse.ecco.adapter.challenge.JavaPlugin");
        child = this.getClassChild(child, "at.jku.isse.ecco.adapter.challenge.TestClass");
        child = this.getAbstractChild(child, "METHODS");
        child = this.getMethodChild(child, "TestMethod()");
        child = this.getLineChild(child, "        System.out.println(\"Code line x\");");

        MemFeatureTrace traceComparison = new MemFeatureTrace(node);
        traceComparison.addUserCondition("FEATUREA");
        assertEquals(memTrace, traceComparison);
        return true;

         */
    }

    @Test
    public void multipleFeatureTracesExistCompletelyAfterCommit(){

        this.commitVariantByPath(this.TEST_VARIANT1);
        this.commitVariantByPath(this.TEST_VARIANT2);
        this.repository = (Repository.Op) this.eccoService.getRepository();


        /*
        Collection<FeatureTrace> traces = this.repository.getFeatureTraces();
        assertTrue(traces.stream().anyMatch(this::containsFeatureTraceAX));
        assertTrue(traces.stream().anyMatch(this::containsFeatureTraceCY));

         */
    }



    @Test
    public void saveAndLoadRepository(){
        this.commitVariantByPath(this.TEST_VARIANT1);
        this.repository = (Repository.Op) this.eccoService.getRepository();
        assertFalse(this.repository.getFeatureTraces().isEmpty());
    }

    @Test
    public void fuseAssociationsWithFeatureTraces(){
        this.commitVariantByPath(this.TEST_VARIANT1);
        this.repository = (Repository.Op) this.eccoService.getRepository();
        Node.Op mainTree = this.repository.fuseAssociationsWithFeatureTraces();
    }

    /*
    TODO

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
     */



}
