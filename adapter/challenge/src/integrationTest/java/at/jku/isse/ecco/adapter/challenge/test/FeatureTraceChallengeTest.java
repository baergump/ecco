package at.jku.isse.ecco.adapter.challenge.test;

import at.jku.isse.ecco.EccoException;
import at.jku.isse.ecco.adapter.challenge.data.*;
import at.jku.isse.ecco.core.Association;
import at.jku.isse.ecco.feature.Feature;
import at.jku.isse.ecco.feature.FeatureRevision;
import at.jku.isse.ecco.featuretracerecording.FeatureTrace;
import at.jku.isse.ecco.featuretracerecording.FeatureTraceCondition;
import at.jku.isse.ecco.module.Condition;
import at.jku.isse.ecco.module.Module;
import at.jku.isse.ecco.module.ModuleRevision;
import at.jku.isse.ecco.repository.Repository;
import at.jku.isse.ecco.service.EccoService;
import at.jku.isse.ecco.storage.mem.featuretrace.MemFeatureTrace;
import at.jku.isse.ecco.storage.mem.featuretrace.MemFeatureTraceCondition;
import at.jku.isse.ecco.tree.Node;
import at.jku.isse.ecco.util.Trees;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

public class FeatureTraceChallengeTest {

    // TODO: clean up fields
    private static final Path BENCHMARK_DIR = Paths.get("C:\\Users\\user\\Desktop\\splc_challenge\\workspace\\ArgoUMLSPLBenchmark");
    private static final Path OUTPUT_DIR = Paths.get("C:\\Users\\user\\Desktop\\splc_challenge\\results");
    // set this path to a concrete scenario if you only want to run a specific one
    private static final Path SCENARIO_DIR = BENCHMARK_DIR.resolve("scenarios\\ScenarioAllVariants");
    // set this path to where the results for a specific scenario should go
    private static final Path SCENARIO_OUTPUT_DIR = OUTPUT_DIR.resolve("ScenarioAllVariants");
    // boolean of whether to include or-combination of modules (just use and-combination)
    private static final boolean NO_OR = false;
    private static final boolean USE_ONLY_MIN_ORDER = true;
    private static final int MAX_ORDER = 1;

    // ----------------------------------

    private static final Path SERVICE_BASE_DIR = Paths.get("C:\\Users\\Berni\\Desktop\\Project\\FeatureTraceChallenge\\ServiceBaseDir");
    private static final Path RESULTS_DIR = Paths.get("C:\\Users\\Berni\\Desktop\\Project\\FeatureTraceChallenge\\Results");
    private EccoService eccoService;
    private Repository.Op repository;

    // TODO: how to handle feature trace condition?
    // TODO: make a global list of written files

    @Test
    public void testFeatureTraceImport(){
        Path fromRepoPath = Paths.get("C:\\Users\\Berni\\Desktop\\Project\\FeatureTraceChallenge\\Repositories\\ScenarioRandom002Variants");
        Path toRepoPath = Paths.get("C:\\Users\\Berni\\Desktop\\Project\\FeatureTraceChallenge\\Repositories\\Dummy2Repository");
        // toRepoPath is used here as placeholder
        EccoService eccoService = this.openRepository(fromRepoPath, toRepoPath);
        Repository.Op fromRepo = (Repository.Op) eccoService.getRepository();
        eccoService = this.createRepository(toRepoPath, toRepoPath);
        Repository.Op toRepo = (Repository.Op) eccoService.getRepository();
        this.transferFeatureTraces(fromRepo, toRepo);
    }
    private EccoService openRepository(Path repoPath, Path baseDir){
        EccoService eccoService = new EccoService();
        eccoService.setRepositoryDir(repoPath.resolve(".ecco").toAbsolutePath());
        eccoService.setBaseDir(baseDir.toAbsolutePath());
        eccoService.open();
        return eccoService;
    }

    private EccoService createRepository(Path repoPath, Path baseDir){
        EccoService eccoService = new EccoService();
        eccoService.setRepositoryDir(repoPath);
        eccoService.setBaseDir(baseDir);
        eccoService.init();
        return eccoService;
    }

    private void transferFeatureTraces(Repository.Op from, Repository.Op to){
        // the repository "to" must be empty (no features, modules etc)
        // (this method is only for challenge-purposes)
        Collection<FeatureTrace> featureTraces = from.getFeatureTraces();
        for (FeatureTrace featureTrace : featureTraces){
            featureTrace.getNode().clearFeatureTraceCondition();
            this.addFeatureTraceToRepository(to, featureTrace);
        }
    }

    private void addFeatureTraceToRepository(Repository.Op to, FeatureTrace featureTrace){
        MemFeatureTrace memFeatureTrace = (MemFeatureTrace) featureTrace;
        FeatureTraceCondition featureTraceCondition = memFeatureTrace.getPresenceCondition();
        Node node = memFeatureTrace.getNode();

        Collection<ModuleRevision> positiveModuleRevisions = featureTraceCondition.getPositiveModuleRevisions();
        Collection<ModuleRevision> newPositiveModuleRevisions = new HashSet<>();
        for (ModuleRevision moduleRevision : positiveModuleRevisions){
            newPositiveModuleRevisions.add(this.addModuleRevisionToRepository(to, moduleRevision));
        }

        Collection<ModuleRevision> negativeModuleRevisions = featureTraceCondition.getNegativeModuleRevisions();
        Collection<ModuleRevision> newNegativeModuleRevisions = new HashSet<>();
        for (ModuleRevision moduleRevision : negativeModuleRevisions){
            newNegativeModuleRevisions.add(this.addModuleRevisionToRepository(to, moduleRevision));
        }

        FeatureTraceCondition newFeatureTraceCondition = new MemFeatureTraceCondition(newPositiveModuleRevisions, newNegativeModuleRevisions);
        FeatureTrace newFeatureTrace = new MemFeatureTrace(node, newFeatureTraceCondition);
        to.addFeatureTrace(newFeatureTrace);
    }

    private ModuleRevision addModuleRevisionToRepository(Repository.Op to, ModuleRevision moduleRevision){
        FeatureRevision[] positiveFeatureRevisions = moduleRevision.getPos();
        Collection<FeatureRevision> newPositiveFeatureRevisions = new ArrayList<>();
        for (FeatureRevision featureRevision : positiveFeatureRevisions){
            Feature feature = featureRevision.getFeature();
            Feature newFeature = this.addFeatureToRepository(to, feature);
            FeatureRevision newFeatureRevision = this.addFeatureRevisionToFeature(newFeature, featureRevision);
            newPositiveFeatureRevisions.add(newFeatureRevision);
        }
        FeatureRevision[] newPositiveFeatureRevisionArray = new FeatureRevision[newPositiveFeatureRevisions.size()];
        newPositiveFeatureRevisions.toArray(newPositiveFeatureRevisionArray);

        Feature[] negativeFeatures = moduleRevision.getNeg();
        Collection<Feature> newNegativeFeatures = new ArrayList<>();
        for (Feature feature : negativeFeatures){
            Feature newFeature = this.addFeatureToRepository(to, feature);
            newNegativeFeatures.add(newFeature);
        }
        Feature[] newNegativeFeatureArray = new Feature[newNegativeFeatures.size()];
        newNegativeFeatures.toArray(newNegativeFeatureArray);

        Module module = moduleRevision.getModule();
        Module newModule = this.addModuleToRepository(to, module);

        ModuleRevision newModuleRevision = newModule.addRevision(newPositiveFeatureRevisionArray, newNegativeFeatureArray);
        if (newModuleRevision == null){
            return newModule.getRevision(newPositiveFeatureRevisionArray, newNegativeFeatureArray);
        } else {
            return newModuleRevision;
        }
    }

    private Module addModuleToRepository(Repository.Op to, Module module){
        Feature[] positiveFeatures = module.getPos();
        Collection<Feature> newPositiveFeatures = new ArrayList<>();
        for (Feature feature : positiveFeatures){
            Feature newFeature = this.addFeatureToRepository(to, feature);
            newPositiveFeatures.add(newFeature);
        }
        Feature[] newPositiveFeatureArray = new Feature[newPositiveFeatures.size()];
        newPositiveFeatures.toArray(newPositiveFeatureArray);

        Feature[] negativeFeatures = module.getNeg();
        Collection<Feature> newNegativeFeatures = new ArrayList<>();
        for (Feature feature : negativeFeatures){
            Feature newFeature = this.addFeatureToRepository(to, feature);
            newNegativeFeatures.add(newFeature);
        }
        Feature[] newNegativeFeatureArray = new Feature[newNegativeFeatures.size()];
        newNegativeFeatures.toArray(newNegativeFeatureArray);

        try{
            return Objects.requireNonNullElse(to.addModule(newPositiveFeatureArray, newNegativeFeatureArray),
                    to.getModule(newPositiveFeatureArray, newNegativeFeatureArray));
        } catch(Exception e){
            return to.getModule(newPositiveFeatureArray, newNegativeFeatureArray);
        }
    }

    private Feature addFeatureToRepository(Repository.Op to, Feature feature){
        Feature newFeature = to.addFeature(feature.getId(), feature.getName());
        return Objects.requireNonNullElse(newFeature, feature);
    }

    private FeatureRevision addFeatureRevisionToFeature(Feature feature, FeatureRevision featureRevision){
        FeatureRevision newFeatureRevision = feature.addRevision(featureRevision.getId());
        return Objects.requireNonNullElse(newFeatureRevision, feature.getRevision(featureRevision.getId()));
    }

    // for every test run, there is a repository
    // a test run is defined by:
    // - scenario
    // - strategy
    // - the amount of feature traces
    // - the chosen feature traces
    // - the amount of faulty feature traces
    // - the faults

    @Test
    public void runExperiment() throws IOException {
        // TODO: create repository
        // TODO: transfer feature traces

        // TODO: create results for every scenario for different amounts of feature traces
        // TODO: for every amount of feature traces, choose random ones multiple times (10?)
        Path scenarioPath = Paths.get("C:\\Users\\Berni\\Desktop\\Project\\FeatureTraceChallenge\\Repositories\\ScenarioRandom002Variants");
        Path outputpath = Paths.get("C:\\Users\\Berni\\Desktop\\Project\\FeatureTraceChallenge\\Results\\Add\\ScenarioRandom002Variants\\000_Feature_Traces\\000_Faulty_Traces");
        this.eccoService = this.openRepository(scenarioPath, SERVICE_BASE_DIR);
        this.repository = (Repository.Op) eccoService.getRepository();
        this.computeResults(outputpath);
    }

    private void computeResults(Path scenarioOutputDir) throws IOException {
        // outer map is filename + inner map
        // inner map is result string + boolean (is result a refinement?)
        Map<String, Map<String, Boolean>> results = new HashMap<>();

        // for every association create results file with name of minimal to string
        System.out.println("Max Order: " + this.repository.getMaxOrder());
        Collection<? extends Association> associations = repository.getAssociations();
        int assocCounter = 0;
        for (Association association : associations) {
            assocCounter++;
            this.computeAssociationResult(association, results, scenarioOutputDir, assocCounter);
        }

        Collection<FeatureTrace> featureTraces = this.repository.getFeatureTraces();
        for (FeatureTrace featureTrace : featureTraces){
            this.computeFeatureTraceResult(featureTrace);
        }

        // write to file (for all associations/traces)
        Path resultsDir = scenarioOutputDir.resolve("results");
        if (!Files.exists(resultsDir))
            Files.createDirectory(resultsDir);
        for (Map.Entry<String, Map<String, Boolean>> entry : results.entrySet()) {
            List<String> resultLines = new ArrayList<>();
            entry.getValue().forEach((k, v) -> {
                if (v)
                    resultLines.add(k);
                else
                    resultLines.add(k + " Refinement");
            });
            Files.write(resultsDir.resolve(entry.getKey() + ".txt"), resultLines, StandardOpenOption.CREATE);
        }
    }

    private void computeFeatureTraceResult(FeatureTrace featureTrace){
        System.out.println("compute feature trace result.");
        FeatureTraceCondition condition = featureTrace.getPresenceCondition();
        Collection<ModuleRevision> posModuleRevisions = condition.getPositiveModuleRevisions();
        Collection<ModuleRevision> negModuleRevisions = condition.getNegativeModuleRevisions();

        if (negModuleRevisions.size() > 0 && posModuleRevisions.size() > 0){
            throw new RuntimeException("Feature Trace with positive and negative module revisions!");
        }

        if (negModuleRevisions.size() > 1){
            throw new RuntimeException("Feature Trace with multiple negative module revisions!");
        }

        for (ModuleRevision moduleRevision : posModuleRevisions){
            this.computePosModuleRevisionResult(moduleRevision, featureTrace.getNode());
        }
        for (ModuleRevision moduleRevision : negModuleRevisions){
            this.computeNegModuleRevisionResult(moduleRevision, featureTrace.getNode());
        }
    }

    private void computePosModuleRevisionResult(ModuleRevision moduleRevision, Node node){
        String filename = this.moduleRevisionToFilename(moduleRevision);

        // compute string using node
    }

    private void computeNegModuleRevisionResult(ModuleRevision moduleRevision, Node node){
        // TODO: logic to get filename
    }

    private String moduleRevisionToFilename(ModuleRevision moduleRevision){
        FeatureRevision[] posFeatureRevisions = moduleRevision.getPos();
        List<Feature> posFeatures = Arrays.stream(posFeatureRevisions).map(FeatureRevision::getFeature).collect(Collectors.toList());
        Feature[] posFeatureArray = new Feature[posFeatures.size()];
        posFeatures.toArray(posFeatureArray);
        Feature[] negFeatures = moduleRevision.getNeg();
        return this.featuresToFilename(posFeatureArray, negFeatures);
    }

    private String featuresToFilename(Feature[] posFeatures, Feature[] negFeatures){
        List<String> names = new ArrayList<>();
        List<String> posNames = new ArrayList<>();
        for (Feature feature : posFeatures) {
            if (!feature.getName().equals("BASE")) {
                names.add(feature.getName());
                posNames.add(feature.getName());
            }
        }
        List<String> negNames = new ArrayList<>();
        for (Feature feature : negFeatures) {
            if (!feature.getName().equals("BASE")) {
                names.add(feature.getName());
                negNames.add(feature.getName());
            }
        }
        return names.stream().sorted().map(name -> {
            if (posNames.contains(name)) return name;
            else if (negNames.contains(name)) return "not_" + name;
            else return "";
        }).collect(Collectors.joining("_and_"));
    }

    private void computeAssociationResult(Association association, Map<String, Map<String, Boolean>> results, Path scenarioOutputDir, int associationCounter) throws IOException {
        System.out.println("NUM_ARTIFACTS: " + Trees.countArtifacts(association.getRootNode()));
        Condition condition = association.computeCondition();
        System.out.println("TYPE: " + condition.getType());
        System.out.println("LONG: " + condition.getModuleConditionString());
        System.out.println("SHORT: " + condition.getSimpleModuleConditionString());

        Collection<Module> finalModules = this.selectModules(association);

        // compute results
        StringBuilder sb = new StringBuilder();
        Map<String, Boolean> lines = new HashMap<>();
        this.computeString(association.getRootNode(), sb, lines, null);
        System.out.println(sb);

        // loop over modules, create filename by: removing base feature, concatenating with "_and_" or "_or" (depending on type) and prefixing "not_" for negative modules
        for (Module module : finalModules) {
            String filename = featuresToFilename(module.getPos(), module.getNeg());
            if (filename.isEmpty())
                continue;

            // write to file (per association/trace)
            Path resultsSplitDir = scenarioOutputDir.resolve("results_split");
            if (!Files.exists(resultsSplitDir))
                Files.createDirectory(resultsSplitDir);
            Path associationDir = resultsSplitDir.resolve("A" + associationCounter);
            if (!Files.exists(associationDir))
                Files.createDirectory(associationDir);
            Files.write(associationDir.resolve(filename + ".txt"), sb.toString().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            // add lines to results
            results.computeIfAbsent(filename, s -> new HashMap<>());
            Map<String, Boolean> filenameEntries = results.get(filename);
            for (Map.Entry<String, Boolean> entry : lines.entrySet()) {
                filenameEntries.compute(entry.getKey(), (k, v) -> {
                    if (v == null)
                        return entry.getValue();
                    else
                        return v | entry.getValue();
                });
            }
        }

        System.out.println("---------");
    }

    private Collection<Module> selectModules(Association association){
        Condition condition = association.computeCondition();
        // get all min-modules of association with order <= minOrder
        Collection<Module> modules = condition.getModules().keySet();
        // minOrder was seemingly introduced in case there are no modules left if MAX_ORDER is enforced
        // e.g. there are only modules with order 3, but MEX_ORDER is 2
        int minOrder = modules.isEmpty() ? 0 : modules.stream().min((m1, m2) -> m1.getOrder() - m2.getOrder()).get().getOrder();
        // modules with order <= minOrder
        Collection<Module> minModules = modules.stream().filter(module -> module.getOrder() <= minOrder).collect(Collectors.toList());

        Collection<Module> finalModules = new ArrayList<>();
        // exclude the association if or-conditions should be ignored
        if (NO_OR && condition.getType() == Condition.TYPE.OR && minModules.size() > 1){
            System.out.println("Ignore modules because or-conditions should be ignored.");
            return finalModules;
        }

        if (USE_ONLY_MIN_ORDER) {
            finalModules = minModules;
        } else {
            if (condition.getType() == Condition.TYPE.OR) {
                // use min modules for OR traces still
                finalModules = minModules;
            } else if (condition.getType() == Condition.TYPE.AND) {
                // use modules up to including MAX_ORDER or the minimal order if no such module exists
                finalModules = modules.stream().filter(module -> module.getOrder() <= Math.max(MAX_ORDER, minOrder)).collect(Collectors.toList());
            }
        }
        return finalModules;
    }

    private boolean checkNonMethodDescendants(Node node) {
        // get the node data and see if it exists
        if (node.getArtifact() != null && node.getArtifact().getData() != null) {
            // see if the node is an import or variable declaration child
            //if (node.getArtifact().getData() instanceof ImportArtifactData || node.getArtifact().getData() instanceof FieldArtifactData) {
            //if (!(node.getArtifact().getData() instanceof MethodArtifactData)) {
            if (node.getArtifact().getData() instanceof ImportArtifactData || node.getArtifact().getData() instanceof FieldArtifactData || node.getArtifact().getData() instanceof LineArtifactData) {
                return true;
            }
        }

        boolean nonMethodDescendants = false;
        if (node.getArtifact() == null || node.getArtifact().getData() == null || !(node.getArtifact().getData() instanceof MethodArtifactData)) {
            for (Node childNode : node.getChildren()) {
                nonMethodDescendants = nonMethodDescendants | this.checkNonMethodDescendants(childNode);
            }
        }
        return nonMethodDescendants;
    }

    private void computeString(Node node, StringBuilder sb, Map<String, Boolean> lines, String currentClassName) {
        if (node.getArtifact() != null && node.getArtifact().getData() != null) {
            if (node.getArtifact().getData() instanceof ClassArtifactData) {
                currentClassName = ((ClassArtifactData) node.getArtifact().getData()).getName();

                boolean nonMethodDescendants = this.checkNonMethodDescendants(node);

                if (lines.containsKey(currentClassName))
                    throw new EccoException("Class already exists.");
                if (node.isUnique() && (!node.getParent().isUnique() || (node.getParent().getArtifact() != null && node.getArtifact().getData() != null && !(node.getParent().getArtifact().getData() instanceof ClassArtifactData)))) {
                    sb.append(currentClassName + "\n");
                    lines.put(currentClassName, true);
                } else if (!node.isUnique() && nonMethodDescendants) {
                    sb.append(currentClassName + " Refinement\n");
                    lines.put(currentClassName, false);
                }
            } else if (node.getArtifact().getData() instanceof MethodArtifactData) {
                String methodSignature = ((MethodArtifactData) node.getArtifact().getData()).getSignature().replaceAll(", ", ",");
                String fullMethodSignature = currentClassName + " " + methodSignature;
                if (lines.containsKey(fullMethodSignature))
                    throw new EccoException("Method already exists.");
                if (node.isUnique() && !node.getParent().isUnique()) {
                    sb.append(fullMethodSignature + "\n");
                    lines.put(fullMethodSignature, true);
                } else if (!node.isUnique() && !node.getChildren().isEmpty()) { // it has unique descendants
                    sb.append(fullMethodSignature + " Refinement\n");
                    lines.put(fullMethodSignature, false);
                }
            }
        }
        for (Node childNode : node.getChildren()) {
            this.computeString(childNode, sb, lines, currentClassName);
        }
    }

    @Test
    public void Analyze_Differences() throws IOException {
        Path GT_PATH = BENCHMARK_DIR.resolve("groundTruth");
        //Path MY_PATH = BENCHMARK_DIR.resolve("yourResults");
        Path MY_PATH = SCENARIO_OUTPUT_DIR.resolve("results");

        Set<Path> myFiles = Files.list(MY_PATH).map(Path::getFileName).filter(path -> path.toString().endsWith(".txt")).collect(Collectors.toSet());
        Set<Path> groundTruthFiles = Files.list(GT_PATH).map(Path::getFileName).filter(path -> path.toString().endsWith(".txt")).collect(Collectors.toSet());

        Set<Path> commonFiles = new HashSet<>(groundTruthFiles);
        commonFiles.retainAll(myFiles);
        Set<Path> onlyMyFiles = new HashSet<>(myFiles);
        onlyMyFiles.removeAll(groundTruthFiles);
        Set<Path> onlyGTFiles = new HashSet<>(groundTruthFiles);
        onlyGTFiles.removeAll(myFiles);

        System.out.println("ONLY MY FILES:");
        onlyMyFiles.forEach(System.out::println);
        System.out.println("ONLY GT FILES:");
        onlyGTFiles.forEach(System.out::println);

        for (Path commonFile : commonFiles) {
            System.out.println("----------------------------------------");
            System.out.println("# FILE: " + commonFile);

            List<String> GTLines = Files.readAllLines(GT_PATH.resolve(commonFile));
            Set<String> GTEntries = new HashSet<>(GTLines);
            if (GTLines.size() != GTEntries.size())
                System.out.println("THERE ARE DUPLICATE LINES IN GT FILE " + commonFile);

            List<String> MyLines = Files.readAllLines(MY_PATH.resolve(commonFile));
            Set<String> MyEntries = new HashSet<>(MyLines);
            if (MyLines.size() != MyEntries.size())
                System.out.println("THERE ARE DUPLICATE LINES IN MY FILE " + commonFile);

            Set<String> onlyMyEntries = new HashSet<>(MyEntries);
            onlyMyEntries.removeAll(GTEntries);
            Set<String> onlyGTEntries = new HashSet<>(GTEntries);
            onlyGTEntries.removeAll(MyEntries);

            System.out.println("ENTRIES ONLY IN MY FILE:");
            onlyMyEntries.forEach(System.out::println);
            System.out.println("ENTRIES ONLY IN GT FILE:");
            onlyGTEntries.forEach(System.out::println);
        }
    }
}
