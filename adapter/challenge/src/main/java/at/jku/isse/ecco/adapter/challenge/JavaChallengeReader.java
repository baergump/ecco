package at.jku.isse.ecco.adapter.challenge;

import at.jku.isse.ecco.EccoException;
import at.jku.isse.ecco.adapter.ArtifactReader;
import at.jku.isse.ecco.adapter.challenge.data.*;
import at.jku.isse.ecco.adapter.challenge.vevos.LogicToModuleTransformer;
import at.jku.isse.ecco.adapter.challenge.vevos.VEVOSConditionHandler;
import at.jku.isse.ecco.adapter.challenge.vevos.VEVOSPresenceCondition;
import at.jku.isse.ecco.adapter.dispatch.DispatchWriter;
import at.jku.isse.ecco.adapter.dispatch.PluginArtifactData;
import at.jku.isse.ecco.artifact.Artifact;
import at.jku.isse.ecco.dao.EntityFactory;
import at.jku.isse.ecco.featuretracerecording.FeatureTrace;
import at.jku.isse.ecco.featuretracerecording.FeatureTraceCondition;
import at.jku.isse.ecco.repository.Repository;
import at.jku.isse.ecco.service.listener.ReadListener;
import at.jku.isse.ecco.tree.Node;
import com.github.javaparser.*;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.*;
import com.google.inject.Inject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

public class JavaChallengeReader implements ArtifactReader<Path, Set<Node.Op>> {

	protected static final Logger LOGGER = Logger.getLogger(DispatchWriter.class.getName());

	private final EntityFactory entityFactory;

	@Inject
	public JavaChallengeReader(EntityFactory entityFactory) {
		checkNotNull(entityFactory);

		this.entityFactory = entityFactory;
	}

	@Override
	public String getPluginId() {
		return JavaPlugin.class.getName();
	}

	private static Map<Integer, String[]> prioritizedPatterns;

	static {
		prioritizedPatterns = new HashMap<>();
		prioritizedPatterns.put(Integer.MAX_VALUE, new String[]{"**.java"});
	}

	@Override
	public Map<Integer, String[]> getPrioritizedPatterns() {
		return Collections.unmodifiableMap(prioritizedPatterns);
	}

	@Override
	public Set<Node.Op> read(Path[] input, Repository.Op repository) {
		return this.read(Paths.get("."), input, repository);
	}

	@Override
	public Set<Node.Op> read(Path base, Path[] input, Repository.Op repository) {
		// TODO: refactor method (make it shorter, less procedural, more oo)
		// TODO: refactor: feature traces are created as a side-effect, which is bad practice
		LogicToModuleTransformer logicToModuleTransformer = new LogicToModuleTransformer(repository);
		VEVOSConditionHandler vevosConditionHandler = new VEVOSConditionHandler(base, logicToModuleTransformer);

		Set<Node.Op> nodes = new HashSet<>();

		long totalJavaParserTime = 0;

		for (Path path : input) {
			// Using comments by Couto et al. would mean looking at line numbers in VEVOS files, looking at
			// Couto et al. comment and connecting granularity to adapter artifact
			// Couto et al.: Package, Class, ClassSignature, InterfaceMethod, Method, MethodBody, Attribute, Statement, Expression

			// Should be possible without using Couto et al. comments.
			// parser results seem to offer the possibility to get the respective line numbers in the source code
			// -> connect vevos line numbers to parser result line numbers to recognize feature traces

			List<VEVOSPresenceCondition> fileSpecificConditions = vevosConditionHandler.getFileSpecificPresenceConditions(path);

			Path resolvedPath = base.resolve(path);

			// create plugin artifact/node
			Artifact.Op<PluginArtifactData> pluginArtifact = this.entityFactory.createArtifact(new PluginArtifactData(this.getPluginId(), path));
			Node.Op pluginNode = this.entityFactory.createNode(pluginArtifact);
			nodes.add(pluginNode);

			try {
				// read raw file contents
				String fileContent = new String(Files.readAllBytes(resolvedPath), StandardCharsets.UTF_8);
				String[] lines = fileContent.split("\\r?\\n");

				long localStartTime = System.currentTimeMillis();
				CompilationUnit cu = StaticJavaParser.parse(fileContent);
				totalJavaParserTime += (System.currentTimeMillis() - localStartTime);

				// package name
				String packageName = "";
				if (cu.getPackageDeclaration().isPresent())
					packageName = cu.getPackageDeclaration().get().getName().toString();

				for (TypeDeclaration<?> typeDeclaration : cu.getTypes()) {
					// create class artifact/node
					String className = typeDeclaration.getName().toString();
					Artifact.Op<ClassArtifactData> classArtifact = this.entityFactory.createArtifact(new ClassArtifactData(packageName + "." + className));
					Node.Op classNode = this.entityFactory.createNode(classArtifact);
					pluginNode.addChild(classNode);
					// TODO: use file conditions to create feature traces for classes

					// imports
					Artifact.Op<AbstractArtifactData> importsGroupArtifact = this.entityFactory.createArtifact(new AbstractArtifactData("IMPORTS"));
					Node.Op importsGroupNode = this.entityFactory.createNode(importsGroupArtifact);
					classNode.addChild(importsGroupNode);
					for (ImportDeclaration importDeclaration : cu.getImports()) {
						String importName = "import " + importDeclaration.getName().asString();
						Artifact.Op<ImportArtifactData> importArtifact = this.entityFactory.createArtifact(new ImportArtifactData(importName));
						Node.Op importNode = this.entityFactory.createNode(importArtifact);
						importsGroupNode.addChild(importNode);
						// check for feature traces
						VEVOSPresenceCondition matchingCondition = this.getMatchingPresenceCondition(importDeclaration, fileSpecificConditions);
						this.addFeatureTrace(matchingCondition, importNode, repository);
					}
					ArrayList<String> methods =  new ArrayList<>();
					this.addClassChildren(typeDeclaration, classNode, lines, methods, fileSpecificConditions, repository);
				}
			} catch (IOException e) {
				e.printStackTrace();
				throw new EccoException("Error parsing java file.", e);
			}
		}

		LOGGER.fine(JavaParser.class + ".parse(): " + totalJavaParserTime + "ms");
		return nodes;
	}

	/*
	public Set<Node.Op> read(Path base, Path[] input, ArrayList<String> methods) {
		Set<Node.Op> nodes = new HashSet<>();

		long totalJavaParserTime = 0;

		for (Path path : input) {
			Path resolvedPath = base.resolve(path);

			// create plugin artifact/node
			Artifact.Op<PluginArtifactData> pluginArtifact = this.entityFactory.createArtifact(new PluginArtifactData(this.getPluginId(), path));
			Node.Op pluginNode = this.entityFactory.createNode(pluginArtifact);
			nodes.add(pluginNode);

			try {
				// read raw file contents
				String fileContent = new String(Files.readAllBytes(resolvedPath), StandardCharsets.UTF_8);
				String[] lines = fileContent.split("\\r?\\n");

				long localStartTime = System.currentTimeMillis();
				//CompilationUnit cu = JavaParser.parse(resolvedPath);
				CompilationUnit cu = StaticJavaParser.parse(fileContent);
				totalJavaParserTime += (System.currentTimeMillis() - localStartTime);

				// package name
				String packageName = "";
				if (cu.getPackageDeclaration().isPresent())
					packageName = cu.getPackageDeclaration().get().getName().toString();

				for (TypeDeclaration<?> typeDeclaration : cu.getTypes()) {
					// create class artifact/node
					String className = typeDeclaration.getName().toString();
					Artifact.Op<ClassArtifactData> classArtifact = this.entityFactory.createArtifact(new ClassArtifactData(packageName + "." + className));
					Node.Op classNode = this.entityFactory.createNode(classArtifact);
					pluginNode.addChild(classNode);

					// imports
					Artifact.Op<AbstractArtifactData> importsGroupArtifact = this.entityFactory.createArtifact(new AbstractArtifactData("IMPORTS"));
					Node.Op importsGroupNode = this.entityFactory.createNode(importsGroupArtifact);
					classNode.addChild(importsGroupNode);
					for (ImportDeclaration importDeclaration : cu.getImports()) {
						String importName = "import " + importDeclaration.getName().asString();
						Artifact.Op<ImportArtifactData> importArtifact = this.entityFactory.createArtifact(new ImportArtifactData(importName));
						Node.Op importNode = this.entityFactory.createNode(importArtifact);
						importsGroupNode.addChild(importNode);
					}

					this.addClassChildren(typeDeclaration, classNode, lines, methods);
				}
			} catch (IOException e) {
				e.printStackTrace();
				throw new EccoException("Error parsing java file.", e);
			}

		}

		LOGGER.fine(JavaParser.class + ".parse(): " + totalJavaParserTime + "ms");

		return nodes;
	}
	*/

	private void addClassChildren(TypeDeclaration<?> typeDeclaration,
								  Node.Op classNode,
								  String[] lines,
								  ArrayList<String> methods,
								  List<VEVOSPresenceCondition> fileSpecificConditions,
								  Repository.Op repository) {
		// TODO: refactor method (shorten, less procedural, more oo, less arguments)

		// create methods artifact/node
		Artifact.Op<AbstractArtifactData> methodsGroupArtifact = this.entityFactory.createArtifact(new AbstractArtifactData("METHODS"));
		Node.Op methodsGroupNode = this.entityFactory.createNode(methodsGroupArtifact);
		classNode.addChild(methodsGroupNode);
		// create fields artifact/node
		Artifact.Op<AbstractArtifactData> fieldsGroupArtifact = this.entityFactory.createArtifact(new AbstractArtifactData("FIELDS"));
		Node.Op fieldsGroupNode = this.entityFactory.createOrderedNode(fieldsGroupArtifact);
		classNode.addChild(fieldsGroupNode);
		// create enums artifact/node
		Artifact.Op<AbstractArtifactData> enumsGroupArtifact = this.entityFactory.createArtifact(new AbstractArtifactData("ENUMS"));
		Node.Op enumsGroupNode = this.entityFactory.createOrderedNode(enumsGroupArtifact);
		classNode.addChild(enumsGroupNode);
		for (BodyDeclaration<?> node : typeDeclaration.getMembers()) {
			// nested classes/interfaces
			if (node instanceof ClassOrInterfaceDeclaration) {
				Artifact.Op<ClassArtifactData> nestedClassArtifact = this.entityFactory.createArtifact(new ClassArtifactData(classNode.toString() + "." + ((ClassOrInterfaceDeclaration) node).getName().toString()));
				Node.Op nestedClassNode = this.entityFactory.createNode(nestedClassArtifact);
				classNode.addChild(nestedClassNode);
				// check for feature trace
				VEVOSPresenceCondition matchingCondition = this.getMatchingPresenceCondition(node, fileSpecificConditions);
				this.addFeatureTrace(matchingCondition, nestedClassNode, repository);
				addClassChildren((ClassOrInterfaceDeclaration) node, nestedClassNode, lines, methods, fileSpecificConditions, repository);
			}
			// enumerations
			else if (node instanceof EnumDeclaration) {
				int beginLine = node.getRange().get().begin.line;
				int endLine = node.getRange().get().end.line;
				int i = beginLine - 1;
				while (i <= endLine) {
					String trimmedLine = lines[i].trim();
					if (!trimmedLine.isEmpty() && !trimmedLine.equals("}") && !trimmedLine.equals("{")) {
						Artifact.Op<LineArtifactData> lineArtifact = this.entityFactory.createArtifact(new LineArtifactData(lines[i]));
						Node.Op lineNode = this.entityFactory.createNode(lineArtifact);
						enumsGroupNode.addChild(lineNode);
						// check for feature trace
						VEVOSPresenceCondition matchingCondition = this.getMatchingPresenceCondition(i, fileSpecificConditions);
						this.addFeatureTrace(matchingCondition, lineNode, repository);
					}
					i++;
				}
			}
			// fields
			else if (node instanceof FieldDeclaration) {
				int beginLine = node.getRange().get().begin.line;
				int endLine = node.getRange().get().end.line;
				String line;
				int i = beginLine - 1;
				while (i < endLine) {
					String trimmedLine = lines[i].trim();
					if (!trimmedLine.isEmpty() && !trimmedLine.equals("}") && !trimmedLine.equals("{")) {
						Artifact.Op<LineArtifactData> lineArtifact = this.entityFactory.createArtifact(new LineArtifactData(lines[i]));
						Node.Op lineNode = this.entityFactory.createNode(lineArtifact);
						fieldsGroupNode.addChild(lineNode);
						// check for feature trace
						VEVOSPresenceCondition matchingCondition = this.getMatchingPresenceCondition(i, fileSpecificConditions);
						this.addFeatureTrace(matchingCondition, lineNode, repository);
					}
					i++;
				}

			}
			// constructors
			else if (node instanceof ConstructorDeclaration) {
				String methodSignature = ((ConstructorDeclaration) node).getName().toString() + "(" +
						((ConstructorDeclaration) node).getParameters().stream().map(parameter -> parameter.getType().toString()).collect(Collectors.joining(",")) +
						")";
				methods.add(classNode.getArtifact() + " " + methodSignature);
				Artifact.Op<MethodArtifactData> methodArtifact = this.entityFactory.createArtifact(new MethodArtifactData(methodSignature));
				Node.Op methodNode = this.entityFactory.createOrderedNode(methodArtifact);
				// check for feature trace
				VEVOSPresenceCondition matchingCondition = this.getMatchingPresenceCondition(node, fileSpecificConditions);
				this.addFeatureTrace(matchingCondition, methodNode, repository);

				methodsGroupNode.addChild(methodNode);
				if (((ConstructorDeclaration) node).getBody().getStatements().isNonEmpty()) {
					int beginLine = node.getRange().get().begin.line;
					int endLine = node.getRange().get().end.line;
					int i = beginLine;
					while (i < endLine - 1) {
						String trimmedLine = lines[i].trim();
						if (!trimmedLine.isEmpty() && !trimmedLine.equals("}") && !trimmedLine.equals("{")) {
							Artifact.Op<LineArtifactData> lineArtifact = this.entityFactory.createArtifact(new LineArtifactData(lines[i]));
							Node.Op lineNode = this.entityFactory.createNode(lineArtifact);
							methodNode.addChild(lineNode);
							// check for feature trace
							matchingCondition = this.getMatchingPresenceCondition(i, fileSpecificConditions);
							this.addFeatureTrace(matchingCondition, lineNode, repository);
						}
						i++;
					}
				}
			}
		}

		// methods
		for (MethodDeclaration methodDeclaration : typeDeclaration.getMethods()) {
			String methodSignature = methodDeclaration.getName().toString() + "(" +
					methodDeclaration.getParameters().stream().map(parameter -> parameter.getType().toString()).collect(Collectors.joining(",")) +
					")";
			methods.add(classNode.getArtifact() + " " + methodSignature);
			Artifact.Op<MethodArtifactData> methodArtifact = this.entityFactory.createArtifact(new MethodArtifactData(methodSignature));
			Node.Op methodNode = this.entityFactory.createOrderedNode(methodArtifact);
			// check for feature trace
			VEVOSPresenceCondition matchingCondition = this.getMatchingPresenceCondition(methodDeclaration, fileSpecificConditions);
			this.addFeatureTrace(matchingCondition, methodNode, repository);

			methodsGroupNode.addChild(methodNode);
			addMethodChildren(methodDeclaration, methodNode, lines, fileSpecificConditions, repository);
		}
	}

	private void addMethodChildren(MethodDeclaration methodDeclaration, Node.Op methodNode, String[] lines,
								   List<VEVOSPresenceCondition> fileSpecificConditions,
								   Repository.Op repository) {
		// lines inside method
		if (methodDeclaration.getBody().isPresent()) {
			int beginLine = methodDeclaration.getBody().get().getRange().get().begin.line;
			int endLine = methodDeclaration.getBody().get().getRange().get().end.line;
			int i = beginLine;
			while (i < endLine - 1) {
				String trimmedLine = lines[i].trim();
				if (!trimmedLine.isEmpty() && !trimmedLine.equals("}") && !trimmedLine.equals("{")) {
					Artifact.Op<LineArtifactData> lineArtifact = this.entityFactory.createArtifact(new LineArtifactData(lines[i]));
					Node.Op lineNode = this.entityFactory.createNode(lineArtifact);
					methodNode.addChild(lineNode);
					// check for feature trace
					VEVOSPresenceCondition matchingCondition = this.getMatchingPresenceCondition(i, fileSpecificConditions);
					this.addFeatureTrace(matchingCondition, methodNode, repository);
				}
				i++;
			}
		}
	}

	private VEVOSPresenceCondition getMatchingPresenceCondition(
			com.github.javaparser.ast.Node astNode,
			List<VEVOSPresenceCondition> orderedPresenceConditions){
		Collection<VEVOSPresenceCondition> matchingPresenceConditions = new HashSet<>();

		// TODO: are there entries with nodeStartLine < startLine < nodeEndLine AND endLine > nodeEndLine?
		// TODO: are there entries with startLine < nodeStartLine AND nodeStartLine < endLine < nodeEndLine?

		int nodeStartLineNumber = astNode.getRange().get().begin.line;
		int nodeEndLineNumber = astNode.getRange().get().end.line;
		matchingPresenceConditions = orderedPresenceConditions.stream()
				.filter(pc -> pc.getStartLineNumber() > nodeStartLineNumber && pc.getEndLineNumber() < nodeEndLineNumber)
				.collect(Collectors.toList());

		if (matchingPresenceConditions.size() > 1){
			// TODO: better message
			throw new RuntimeException(String.format("There are more than one matching presence-condition for node with start %d and end %d.",
					nodeStartLineNumber,
					nodeEndLineNumber));
		}

		return matchingPresenceConditions.iterator().next();
	}

	// TODO: iterate all presence-conditions and search for matching node instead of searching for presence-condition with node
	//  (there should always be one) (there should be no misses)

	private VEVOSPresenceCondition getMatchingPresenceCondition(int lineNumber, List<VEVOSPresenceCondition> orderedPresenceConditions){
		Collection<VEVOSPresenceCondition> matchingPresenceConditions = new HashSet<>();
		matchingPresenceConditions = orderedPresenceConditions.stream()
				.filter(pc -> pc.getStartLineNumber() > lineNumber && pc.getEndLineNumber() < lineNumber)
				.collect(Collectors.toList());

		if (matchingPresenceConditions.size() > 1){
			// TODO: better message
			throw new RuntimeException(String.format("There are more than one matching presence-condition for line number %d.", lineNumber));
		}

		return matchingPresenceConditions.iterator().next();
	}

	private void addFeatureTrace(VEVOSPresenceCondition vevosPresenceCondition, Node.Op eccoNode, Repository.Op repository){
		Node newNode = this.createFeatureTraceNode(eccoNode);
		for (FeatureTraceCondition featureTraceCondition : vevosPresenceCondition.getFeatureTraceConditions()){
			FeatureTrace featureTrace = entityFactory.createFeatureTrace(newNode, featureTraceCondition);
			repository.addFeatureTrace(featureTrace);
		}
	}

	private Node.Op createFeatureTraceNode(Node.Op eccoNode){
		// TODO: copy node and only make dedicated node unique
		// TODO: remove all nodes except path from dedicated node to root
		// TODO: add the new feature trace to the repository
		Node.Op newNode = eccoNode.createPathSceleton();
		return newNode;
		
	}

	private Collection<ReadListener> listeners = new ArrayList<>();

	@Override
	public void addListener(ReadListener listener) {
		this.listeners.add(listener);
	}

	@Override
	public void removeListener(ReadListener listener) {
		this.listeners.remove(listener);
	}

}
