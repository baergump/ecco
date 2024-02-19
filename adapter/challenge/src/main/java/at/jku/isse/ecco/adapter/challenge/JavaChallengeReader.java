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

			// TODO: handle cases where lines don't match (overlap)

			Collection<VEVOSPresenceCondition> fileSpecificConditions = vevosConditionHandler.getFileSpecificPresenceConditions(path);

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
					// TODO: check for feature traces for class

					// imports
					Artifact.Op<AbstractArtifactData> importsGroupArtifact = this.entityFactory.createArtifact(new AbstractArtifactData("IMPORTS"));
					Node.Op importsGroupNode = this.entityFactory.createNode(importsGroupArtifact);
					classNode.addChild(importsGroupNode);
					for (ImportDeclaration importDeclaration : cu.getImports()) {
						String importName = "import " + importDeclaration.getName().asString();
						Artifact.Op<ImportArtifactData> importArtifact = this.entityFactory.createArtifact(new ImportArtifactData(importName));
						Node.Op importNode = this.entityFactory.createNode(importArtifact);
						importsGroupNode.addChild(importNode);
						// TODO: check for feature traces for import
					}
					ArrayList<String> methods =  new ArrayList<>();
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

	private void addClassChildren(TypeDeclaration<?> typeDeclaration, Node.Op classNode, String[] lines, ArrayList<String> methods) {
		// TODO: refactor method (shorten, less procedural, more oo)

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
				// TODO: check for feature trace
				addClassChildren((ClassOrInterfaceDeclaration) node, nestedClassNode, lines, methods);
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
						// TODO: check for feature trace
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
						// TODO: check for feature trace
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
				methodsGroupNode.addChild(methodNode);
				// TODO: check for feature trace
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
							// TODO: check for feature trace
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
			methodsGroupNode.addChild(methodNode);
			// TODO: check for feature trace
			addMethodChildren(methodDeclaration, methodNode, lines);
		}
	}

	private void addMethodChildren(MethodDeclaration methodDeclaration, Node.Op methodNode, String[] lines) {
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
					// TODO: check for feature trace
				}
				i++;
			}
		}
	}

	private VEVOSPresenceCondition getMatchingPresenceCondition(Node.Op node, List<VEVOSPresenceCondition> orderedPresenceConditions){
		Collection<VEVOSPresenceCondition> matchingPresenceConditions = new HashSet<>();
		// search for the first list entry with startLine < nodeStartLine
		// collect all conditions with endLine > nodeEndLine
		// more than 1 -> exception (how should this be handled?) (most specific one?)
		// are there entries with nodeStartLine < startLine < nodeEndLine AND endLine > nodeEndLine?
		// are there entries with startLine < nodeStartLine AND nodeStartLine < endLine < nodeEndLine?

		// create feature trace if there is a result


		// maybe iterate all presence-conditions and search for matching node instead (there must be one) (no misses)
	}
	
	// TODO: method/function to prepare node for feature trace (remove everything but path to root and make everything not unique except one node)

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
