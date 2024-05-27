package at.jku.isse.ecco.adapter.cpp;

import at.jku.isse.ecco.EccoException;
import at.jku.isse.ecco.adapter.ArtifactReader;
import at.jku.isse.ecco.adapter.cpp.data.*;
import at.jku.isse.ecco.adapter.dispatch.DispatchWriter;
import at.jku.isse.ecco.adapter.dispatch.PluginArtifactData;
import at.jku.isse.ecco.artifact.Artifact;
import at.jku.isse.ecco.dao.EntityFactory;
import at.jku.isse.ecco.featuretrace.FeatureTrace;
import at.jku.isse.ecco.featuretrace.parser.VevosCondition;
import at.jku.isse.ecco.featuretrace.parser.VevosConditionHandler;
import at.jku.isse.ecco.featuretrace.parser.VevosFileConditionContainer;
import at.jku.isse.ecco.service.listener.ReadListener;
import at.jku.isse.ecco.tree.Node;
import com.google.inject.Inject;
import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.cpp.*;
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage;
import org.eclipse.cdt.core.model.ILanguage;
import org.eclipse.cdt.core.parser.*;
import org.eclipse.cdt.internal.core.dom.parser.cpp.*;
import org.eclipse.cdt.internal.core.parser.IMacroDictionary;
import org.eclipse.cdt.internal.core.parser.SavedFilesProvider;
import org.eclipse.cdt.internal.core.parser.scanner.InternalFileContent;
import org.eclipse.core.runtime.CoreException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkNotNull;


public class CppReader implements ArtifactReader<Path, Set<Node.Op>> {

    private final EntityFactory entityFactory;

    @Inject
    public CppReader(EntityFactory entityFactory) {
        checkNotNull(entityFactory);
        this.entityFactory = entityFactory;
    }

    @Override
    public String getPluginId() {
        return CppPlugin.class.getName();
    }

    private static Map<Integer, String[]> prioritizedPatterns;

    static {
        prioritizedPatterns = new HashMap<>();
        prioritizedPatterns.put(Integer.MAX_VALUE, new String[]{"**.c", "**.h", "**.cpp", "**.hpp"});
    }

    @Override
    public Map<Integer, String[]> getPrioritizedPatterns() {
        return Collections.unmodifiableMap(prioritizedPatterns);
    }

    @Override
    public Set<Node.Op> read(Path[] input) {
        return this.read(Paths.get("."), input);
    }

    @Override
    public Set<Node.Op> read(Path base, Path[] input) {
        VevosConditionHandler vevosConditionHandler = new VevosConditionHandler(base);
        Set<Node.Op> nodes = new HashSet<>();
        final List<String> headerFiles = new ArrayList<>();
        for (Path path : input) {
            VevosFileConditionContainer fileConditionContainer = vevosConditionHandler.getFileSpecificPresenceConditions(path);
            Path resolvedPath = base.resolve(path);
            File file = resolvedPath.toFile();
            String fileCont = null;
            try {
                fileCont = new String(Files.readAllBytes(resolvedPath), StandardCharsets.UTF_8);
            } catch (IOException e) {
                e.printStackTrace();
            }
            String[] lines = fileCont.split("\\r?\\n");

            Artifact.Op<PluginArtifactData> pluginArtifact = this.entityFactory.createArtifact(new PluginArtifactData(this.getPluginId(), path));
            Node.Op pluginNode = this.entityFactory.createNode(pluginArtifact);
            nodes.add(pluginNode);

            try {
                FileContent fileContent = FileContent.createForExternalFileLocation(file.getAbsolutePath());

                Map<String, String> definedSymbols = new HashMap<>();
                String[] includePaths = headerFiles.toArray(new String[headerFiles.size()]);
                IScannerInfo info = new ScannerInfo(definedSymbols, includePaths);
                IParserLogService log = new DefaultLogService();

                IncludeFileContentProvider emptyIncludes = new SavedFilesProvider() {
                    @Override
                    public InternalFileContent getContentForInclusion(String path, IMacroDictionary macroDictionary) {
                        if (!getInclusionExists(path)) {
                            return null;
                        }
                        return (InternalFileContent) FileContent.createForExternalFileLocation(path);
                    }
                };

                int opts = ILanguage.OPTION_PARSE_INACTIVE_CODE | ILanguage.OPTION_IS_SOURCE_UNIT;

                IASTTranslationUnit translationUnit = GPPLanguage.getDefault().getASTTranslationUnit(fileContent, info, emptyIncludes, null, opts, log);
                IASTPreprocessorStatement[] ppAllStatements = translationUnit.getAllPreprocessorStatements();
                IASTPreprocessorStatement[] ppMacroStatements = translationUnit.getMacroDefinitions();
                IASTPreprocessorStatement[] ppIncludeStatements = translationUnit.getIncludeDirectives();
                Map<String, Integer> errorStatements = new HashMap<>();
                ArrayList<String> macrosInsideFunctions = new ArrayList<>();
                
                for (IASTPreprocessorStatement errorStatement : ppAllStatements) {
                    if (errorStatement instanceof IASTPreprocessorErrorStatement) {
                        errorStatements.put(errorStatement.getRawSignature(), errorStatement.getFileLocation().getStartingLineNumber() - 1);
                    }
                }
                
                //comment out preprocessor directives
                try {
                    String content = getFileContentWithoutIfdefs(file, ppAllStatements);
                    fileContent = FileContent.create(file.getCanonicalPath(), content.toCharArray());
                    //parse again
                    translationUnit = GPPLanguage.getDefault().getASTTranslationUnit(fileContent, info, emptyIncludes, null, opts, log);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // create includes artifact/node
                Artifact.Op<AbstractArtifactData> includesGroupArtifact = this.entityFactory.createArtifact(new AbstractArtifactData("INCLUDES"));
                Node.Op includesGroupNode = this.entityFactory.createOrderedNode(includesGroupArtifact);
                pluginNode.addChild(includesGroupNode);
                // create defines artifact/node
                Artifact.Op<AbstractArtifactData> definesGroupArtifact = this.entityFactory.createArtifact(new AbstractArtifactData("DEFINES"));
                Node.Op definesGroupNode = this.entityFactory.createOrderedNode(definesGroupArtifact);
                pluginNode.addChild(definesGroupNode);
                // create fields artifact/node
                Artifact.Op<AbstractArtifactData> fieldsGroupArtifact = this.entityFactory.createArtifact(new AbstractArtifactData("FIELDS"));
                Node.Op fieldsGroupNode = this.entityFactory.createOrderedNode(fieldsGroupArtifact);
                pluginNode.addChild(fieldsGroupNode);
                // create functions artifact/node
                Artifact.Op<AbstractArtifactData> functionsGroupArtifact = this.entityFactory.createArtifact(new AbstractArtifactData("FUNCTIONS"));
                Node.Op functionsGroupNode = this.entityFactory.createOrderedNode(functionsGroupArtifact);
                pluginNode.addChild(functionsGroupNode);
                
                Map<String, Integer> macroPosition = new HashMap<>();
                Map<String, Integer> definesPosition = new HashMap<>();
                ArrayList<String> macros = new ArrayList<>();
                ArrayList<Integer> lineNumbers = new ArrayList<>();
                ArrayList<Integer> lineNumbersSwitchCase = new ArrayList<>();
                if (ppMacroStatements != null) {
                    for (IASTPreprocessorStatement macro : ppMacroStatements) {
                        if (macro.getContainingFilename().equals(translationUnit.getContainingFilename())) {
                            macroPosition.put(macro.getRawSignature(), macro.getFileLocation().getStartingLineNumber());
                            macros.add(macro.getRawSignature());
                        }
                    }
                }
                
                traverseAST(macrosInsideFunctions, translationUnit.getOriginalNode(), functionsGroupNode, fieldsGroupNode, lines, lineNumbers, lineNumbersSwitchCase, errorStatements, fileConditionContainer);
                
                for (Map.Entry<String, Integer> macro : macroPosition.entrySet()) {
                    for (int i = 0; i < lineNumbers.size() - 1; i += 2) {
                        if (macro.getValue() >= lineNumbers.get(i) && macro.getValue() <= lineNumbers.get(i + 1)) {
                            macros.remove(macro.getKey());
                            break;
                        }
                    }
                    if (macros.contains(macro.getKey()))
                        definesPosition.put(macro.getKey(), macro.getValue());
                }
                for (IASTPreprocessorStatement preprocessorstatement : ppAllStatements) {
                    if (preprocessorstatement instanceof IASTPreprocessorUndefStatement) {
                        if (preprocessorstatement.getContainingFilename().equals(translationUnit.getContainingFilename())) {
                            Boolean add = true;
                            for (int i = 0; i < lineNumbers.size() - 1; i += 2) {
                                if (preprocessorstatement.getFileLocation().getStartingLineNumber() >= lineNumbers.get(i) && preprocessorstatement.getFileLocation().getStartingLineNumber() <= lineNumbers.get(i + 1)) {
                                    add = false;
                                    break;
                                }
                            }
                            if (add)
                                definesPosition.put(preprocessorstatement.getRawSignature(), preprocessorstatement.getFileLocation().getStartingLineNumber());
                        }
                    }
                }
                List<Map.Entry<String, Integer>> list = new ArrayList<>(definesPosition.entrySet());
                list.sort(Map.Entry.comparingByValue());

                for (Map.Entry<String, Integer> entry : list) {
                    Artifact.Op<LineArtifactData> lineArtifact = this.entityFactory.createArtifact(new LineArtifactData(entry.getKey()));
                    Node.Op lineNode = this.entityFactory.createOrderedNode(lineArtifact);
                    this.checkForFeatureTrace(entry.getValue(), entry.getValue(), fileConditionContainer, lineNode);
                    definesGroupNode.addChild(lineNode);
                }

                if (ppIncludeStatements != null) {
                    for (IASTPreprocessorStatement preprocessorStatement : ppIncludeStatements) {
                        if (preprocessorStatement.getContainingFilename().equals(translationUnit.getContainingFilename()) && preprocessorStatement instanceof IASTPreprocessorIncludeStatement) {
                            boolean add = true;
                            for (int i = 0; i < lineNumbers.size() - 1; i += 2) {
                                if (preprocessorStatement.getFileLocation().getStartingLineNumber() >= lineNumbers.get(i) && preprocessorStatement.getFileLocation().getStartingLineNumber() <= lineNumbers.get(i + 1)) {
                                    add = false;
                                    break;
                                }
                            }
                            if (add) {
                                String includeName = preprocessorStatement.getRawSignature();
                                Artifact.Op<IncludeArtifactData> includesArtifact = this.entityFactory.createArtifact(new IncludeArtifactData(includeName));
                                Node.Op includeNode = this.entityFactory.createOrderedNode(includesArtifact);
                                this.checkForFeatureTrace(preprocessorStatement.getFileLocation().getStartingLineNumber(), preprocessorStatement.getFileLocation().getEndingLineNumber(), fileConditionContainer, includeNode);
                                includesGroupNode.addChild(includeNode);
                            }
                        }
                    }
                }
            } catch (CoreException e) {
                e.printStackTrace();
                throw new EccoException("Error parsing java file.", e);
            }
        }
        return nodes;
    }

    private void traverseAST(ArrayList<String> macrosInsideFunctions, IASTNode astNode, Node.Op functions, Node.Op fields, String[] lines, ArrayList<Integer> lineNumbers, ArrayList<Integer> lineNumbersSwitchCase, Map<String, Integer> errorStatements, VevosFileConditionContainer vevosContainer) {
        for (IASTNode child : astNode.getChildren()) {
            if (child != null && child.getContainingFilename().equals(astNode.getContainingFilename())) {
                this.traverseAstChildren(macrosInsideFunctions, child, functions, fields, lines, lineNumbers, lineNumbersSwitchCase, errorStatements, vevosContainer);
            }
        }
    }

    private void traverseAstChildren(ArrayList<String> macrosInsideFunctions,
                                     IASTNode node,
                                     Node.Op functionsNode,
                                     Node.Op fieldsNode,
                                     String[] lines,
                                     ArrayList<Integer> lineNumbers,
                                     ArrayList<Integer> lineNumbersSwitchCase,
                                     Map<String, Integer> errorStatements,
                                     VevosFileConditionContainer vevosContainer) {
        if (node instanceof IASTFieldDeclarator) {
            this.readField(node, fieldsNode, lineNumbers, vevosContainer);

        } else if (node instanceof CPPASTProblemDeclaration) {
            this.readCPPASTProblemDeclaration(node, fieldsNode, lineNumbers, vevosContainer);

        } else if (node instanceof IASTProblemDeclaration) {
            this.readIASTProblemDeclaration(node, functionsNode, lineNumbersSwitchCase, fieldsNode, lines, lineNumbers, vevosContainer);

        } else if (node instanceof IASTSimpleDeclaration) {
            this.readIASTSimpleDeclaration(node, fieldsNode, lines, lineNumbers, lineNumbersSwitchCase, vevosContainer);

        } else if (node instanceof IASTFunctionDefinition) {
            this.readIASTFunctionDefinition(macrosInsideFunctions, node, functionsNode, lines, lineNumbers, lineNumbersSwitchCase, errorStatements, vevosContainer);

        } else if (node instanceof IASTFunctionDeclarator) {
            this.readIASTFunctionDeclarator(node, functionsNode, lines, lineNumbers, lineNumbersSwitchCase, errorStatements, vevosContainer);

        } else if (node instanceof ICPPASTLinkageSpecification) {
            this.readICPPASTLinkageSpecification(node, functionsNode, lines, lineNumbers, vevosContainer);

        } else if (node instanceof CPPASTTemplateDeclaration) {
            this.readCPPASTTemplateDeclaration(node, functionsNode, lines, lineNumbers, lineNumbersSwitchCase, vevosContainer);

        } else {
            throw new RuntimeException("+++++++++++++++++++++ corner case +++++++++++ " + node.getRawSignature() + " " + node.getFileLocation().getFileName() + " " + node.getFileLocation().getStartingLineNumber());
        }
    }

    private void readIASTProblemDeclaration(IASTNode node,
                                           Node.Op functionsNode,
                                           ArrayList<Integer> lineNumbersSwitchCase,
                                           Node.Op fieldsNode,
                                           String[] lines,
                                           ArrayList<Integer> lineNumbers,
                                           VevosFileConditionContainer vevosContainer){
        if (!node.getRawSignature().equals(")") && node.getRawSignature().length() > 1) {
            int init = node.getFileLocation().getStartingLineNumber() - 1;
            int end = node.getFileLocation().getEndingLineNumber() - 1;
            if (end - init < 2) {
                String line = "";
                int actualEnd = init + 1;
                for (int i = init; i <= end; i++) {
                    if (!lineNumbersSwitchCase.contains(i + 1)) {
                        line += lines[i] + "\n";
                        lineNumbersSwitchCase.add(i + 1);
                        actualEnd = i + 1;
                    }
                }
                if (!line.equals("")) {
                    Artifact.Op<ProblemBlockArtifactData> blockArtifact = this.entityFactory.createArtifact(new ProblemBlockArtifactData(line));
                    Node.Op blockNode = this.entityFactory.createOrderedNode(blockArtifact);
                    fieldsNode.addChild(blockNode);
                    this.checkForFeatureTrace(init + 1, actualEnd, vevosContainer, blockNode);
                }
            } else {
                Artifact.Op<ProblemBlockArtifactData> blockArtifact;
                Node.Op blockNode;
                for (int i = init; i <= end; i++) {
                    if (!lineNumbersSwitchCase.contains(i + 1)) {
                        String line = lines[i] + "\n";
                        lineNumbersSwitchCase.add(i + 1);
                        if (i == init) {
                            blockArtifact = this.entityFactory.createArtifact(new ProblemBlockArtifactData(line));
                            blockNode = this.entityFactory.createOrderedNode(blockArtifact);
                            functionsNode.addChild(blockNode);
                            this.checkForFeatureTrace(i + 1, i + 1, vevosContainer, blockNode);
                        } else {
                            Artifact.Op<LineArtifactData> lineArtifact = this.entityFactory.createArtifact(new LineArtifactData(line));
                            Node.Op lineNode = this.entityFactory.createOrderedNode(lineArtifact);
                            functionsNode.addChild(lineNode);
                            this.checkForFeatureTrace(i + 1, i + 1, vevosContainer, lineNode);
                        }
                    }
                }
            }
            lineNumbers.add(node.getFileLocation().getStartingLineNumber());
            lineNumbers.add(node.getFileLocation().getEndingLineNumber());
        }
    }

    private void readIASTSimpleDeclaration(IASTNode node,
                                           Node.Op fieldsNode,
                                           String[] lines,
                                           ArrayList<Integer> lineNumbers,
                                           ArrayList<Integer> lineNumbersSwitchCase,
                                           VevosFileConditionContainer vevosContainer){
        if (((IASTSimpleDeclaration) node).getDeclSpecifier() instanceof IASTCompositeTypeSpecifier) {
            this.readField(node, fieldsNode, lineNumbers, vevosContainer);
        } else if (((IASTSimpleDeclaration) node).getDeclarators().length == 1) {
            int init = node.getFileLocation().getStartingLineNumber() - 1;
            int end = node.getFileLocation().getEndingLineNumber() - 1;
            String field = "";
            int actualEnd = init + 1;
            for (int i = init; i <= end; i++) {
                if (!lineNumbersSwitchCase.contains(i + 1)) {
                    field += lines[i] + "\n";
                    lineNumbersSwitchCase.add(i + 1);
                    actualEnd = i;
                }
            }
            if (!field.equals("")) {
                Artifact.Op<FieldArtifactData> fieldArtifact = this.entityFactory.createArtifact(new FieldArtifactData(field));
                Node.Op fieldNode = this.entityFactory.createOrderedNode(fieldArtifact);
                fieldsNode.addChild(fieldNode);
                this.checkForFeatureTrace(init + 1, actualEnd + 1, vevosContainer, fieldNode);
            }
            lineNumbers.add(node.getFileLocation().getStartingLineNumber());
            lineNumbers.add(node.getFileLocation().getEndingLineNumber());

        } else if (node instanceof CPPASTSimpleDeclaration) {
            if (!lineNumbers.contains(node.getFileLocation().getStartingLineNumber())) {
                this.readField(node, fieldsNode, lineNumbers, vevosContainer);
            }
        }
    }

    private void readIASTFunctionDefinition(ArrayList<String> macrosInsideFunctions,
                                            IASTNode node,
                                            Node.Op functionsNode,
                                            String[] lines,
                                            ArrayList<Integer> lineNumbers,
                                            ArrayList<Integer> lineNumbersSwitchCase,
                                            Map<String, Integer> errorStatements,
                                            VevosFileConditionContainer vevosContainer){
        int init = ((IASTFunctionDefinition) node).getDeclSpecifier().getFileLocation().getStartingLineNumber() - 1;
        int end = ((IASTFunctionDefinition) node).getDeclarator().getFileLocation().getEndingLineNumber() - 1;
        String function = "";
        for (int i = init; i <= end; i++) {
            function += lines[i] + "\n";
        }
        if (lines[end + 1].contains("{") && !function.contains("{") && lines[end + 1].trim().length() == 1) {
            function += "{";
            end++;
        }
        Artifact.Op<FunctionArtifactData> functionsArtifact = this.entityFactory.createArtifact(new FunctionArtifactData(function));
        Node.Op functionNode = this.entityFactory.createOrderedNode(functionsArtifact);
        functionsNode.addChild(functionNode);
        this.checkForFeatureTrace(init, end, vevosContainer, functionNode);
        lineNumbers.add(node.getFileLocation().getStartingLineNumber());
        lineNumbers.add(node.getFileLocation().getEndingLineNumber());
        for (IASTPreprocessorMacroDefinition macro : node.getTranslationUnit().getMacroDefinitions()) {
            if (macrosInsideFunctions.contains(macro.getRawSignature()) && macro.getFileLocation().getFileName().equals(node.getFileLocation().getFileName()) && macro.getFileLocation().getStartingLineNumber() > node.getFileLocation().getStartingLineNumber() && macro.getFileLocation().getStartingLineNumber() < node.getFileLocation().getEndingLineNumber()) {
                Artifact.Op<LineArtifactData> lineArtifact = this.entityFactory.createArtifact(new LineArtifactData(macro.getRawSignature()));
                // TODO
                Node.Op lineNode = this.entityFactory.createOrderedNode(lineArtifact);
                functionNode.addChild(lineNode);
                this.checkForFeatureTrace(macro.getFileLocation().getStartingLineNumber(), macro.getFileLocation().getEndingLineNumber(), vevosContainer, lineNode);
                macrosInsideFunctions.add(macro.getRawSignature());
            }
        }

        if (((IASTFunctionDefinition) node).getBody().getChildren().length > 0) {
            for (IASTNode child : ((IASTFunctionDefinition) node).getBody().getChildren()) {
                for (Map.Entry<String, Integer> errorst : errorStatements.entrySet()) {
                    if (errorst.getValue() != -1 && errorst.getValue() > init && errorst.getValue() < node.getFileLocation().getEndingLineNumber() - 1) {
                        if (errorst.getValue() < Integer.valueOf(child.getFileLocation().getStartingLineNumber())) {
                            this.readLine(errorStatements, functionNode, errorst, vevosContainer);
                        }

                    }
                }
                addChild(child, functionNode, lines, lineNumbersSwitchCase, vevosContainer);
            }
        } else {
            for (Map.Entry<String, Integer> errorst : errorStatements.entrySet()) {
                if (errorst.getValue() != -1 && errorst.getValue() > init && errorst.getValue() < node.getFileLocation().getEndingLineNumber() - 1) {
                    this.readLine(errorStatements, functionNode, errorst, vevosContainer);
                }
            }
        }
        Artifact.Op<LineArtifactData> lineArtifact = this.entityFactory.createArtifact(new LineArtifactData("}"));
        Node.Op lineNode = this.entityFactory.createOrderedNode(lineArtifact);
        functionNode.addChild(lineNode);
        this.checkForFeatureTrace(node.getFileLocation().getEndingLineNumber(), node.getFileLocation().getEndingLineNumber(), vevosContainer, lineNode);
    }

    private void readIASTFunctionDeclarator(IASTNode node,
                                            Node.Op functionsNode,
                                            String[] lines,
                                            ArrayList<Integer> lineNumbers,
                                            ArrayList<Integer> lineNumbersSwitchCase,
                                            Map<String, Integer> errorStatements,
                                            VevosFileConditionContainer vevosContainer){
        int init = node.getFileLocation().getStartingLineNumber() - 1;
        int end = node.getFileLocation().getEndingLineNumber() - 1;
        String functionName = node.getRawSignature();
        Artifact.Op<FunctionArtifactData> functionsArtifact = this.entityFactory.createArtifact(new FunctionArtifactData(functionName));
        // TODO
        Node.Op functionNode = this.entityFactory.createOrderedNode(functionsArtifact);
        functionsNode.addChild(functionNode);
        this.checkForFeatureTrace(init + 1, end + 1, vevosContainer, functionNode);
        lineNumbers.add(node.getFileLocation().getStartingLineNumber());
        lineNumbers.add(node.getFileLocation().getEndingLineNumber());
        this.readNodeMacros(node, functionNode, vevosContainer);

        if (((IASTFunctionDefinition) node).getBody().getChildren().length > 0) {
            for (IASTNode nodechild : node.getChildren()) {
                for (Map.Entry<String, Integer> errorStatement : errorStatements.entrySet()) {
                    if (errorStatement.getValue() != -1 && errorStatement.getValue() > init && errorStatement.getValue() < end) {
                        if (errorStatement.getValue() < nodechild.getFileLocation().getStartingLineNumber()) {
                            this.readLine(errorStatements, functionNode, errorStatement, vevosContainer);
                        }

                    }
                }
                addChild(nodechild, functionNode, lines, lineNumbersSwitchCase, vevosContainer);
            }
        } else {
            for (Map.Entry<String, Integer> errorStatement : errorStatements.entrySet()) {
                if (errorStatement.getValue() != -1 && errorStatement.getValue() > init && errorStatement.getValue() < end) {
                    this.readLine(errorStatements, functionNode, errorStatement, vevosContainer);
                }
            }
        }
        Artifact.Op<LineArtifactData> lineArtifact = this.entityFactory.createArtifact(new LineArtifactData("}"));
        // TODO
        Node.Op lineNode = this.entityFactory.createOrderedNode(lineArtifact);
        functionNode.addChild(lineNode);
        this.checkForFeatureTrace(end + 1, end + 1, vevosContainer, lineNode);
    }

    private void readICPPASTLinkageSpecification(IASTNode node,
                                                 Node.Op functionsNode,
                                                 String[] lines,
                                                 ArrayList<Integer> lineNumbers,
                                                 VevosFileConditionContainer vevosContainer){
        Artifact.Op<BlockArtifactData> blockArtifact = this.entityFactory.createArtifact(new BlockArtifactData(lines[node.getFileLocation().getStartingLineNumber() - 1]));
        // TODO
        Node.Op blockNode = this.entityFactory.createOrderedNode(blockArtifact);
        functionsNode.addChild(blockNode);
        this.checkForFeatureTrace(node.getFileLocation().getStartingLineNumber() - 1, node.getFileLocation().getStartingLineNumber() - 1, vevosContainer, blockNode);
        Node.Op lineNode;
        for (int i = node.getFileLocation().getStartingLineNumber(); i <= node.getFileLocation().getEndingLineNumber() - 1; i++) {
            Artifact.Op<LineArtifactData> lineArtifact = this.entityFactory.createArtifact(new LineArtifactData(lines[i]));
            // TODO
            lineNode = this.entityFactory.createOrderedNode(lineArtifact);
            blockNode.addChild(lineNode);
            this.checkForFeatureTrace(i, i, vevosContainer, lineNode);
        }
        lineNumbers.add(node.getFileLocation().getStartingLineNumber());
        lineNumbers.add(node.getFileLocation().getEndingLineNumber());
    }

    private void readCPPASTProblemDeclaration(IASTNode node,
                                              Node.Op fieldsNode,
                                              ArrayList<Integer> lineNumbers,
                                              VevosFileConditionContainer vevosContainer){
        if (!node.getRawSignature().equals(")")) {
            Artifact.Op<ProblemBlockArtifactData> blockArtifact = this.entityFactory.createArtifact(new ProblemBlockArtifactData(node.getRawSignature()));
            // TODO
            Node.Op blockNode = this.entityFactory.createOrderedNode(blockArtifact);
            fieldsNode.addChild(blockNode);
            this.checkForFeatureTrace(node.getFileLocation().getStartingLineNumber(), node.getFileLocation().getEndingLineNumber(), vevosContainer, blockNode);
            lineNumbers.add(node.getFileLocation().getStartingLineNumber());
            lineNumbers.add(node.getFileLocation().getEndingLineNumber());
        }
    }

    private void readCPPASTTemplateDeclaration(IASTNode node,
                                               Node.Op functionsNode,
                                               String[] lines,
                                               ArrayList<Integer> lineNumbers,
                                               ArrayList<Integer> lineNumbersSwitchCase,
                                               VevosFileConditionContainer vevosContainer){
        String name = lines[node.getFileLocation().getStartingLineNumber() - 1];
        if (!name.contains("{"))
            name += "\n{";
        Artifact.Op<FunctionArtifactData> functionsArtifact = this.entityFactory.createArtifact(new FunctionArtifactData(name));
        // TODO
        Node.Op functionNode = this.entityFactory.createOrderedNode(functionsArtifact);
        functionsNode.addChild(functionNode);
        this.checkForFeatureTrace(node.getFileLocation().getStartingLineNumber(), node.getFileLocation().getEndingLineNumber(), vevosContainer, functionNode);
        lineNumbers.add(node.getFileLocation().getStartingLineNumber());
        lineNumbers.add(node.getFileLocation().getEndingLineNumber());
        this.readNodeMacros(node, functionNode, vevosContainer);

        for (IASTNode nodechild : ((CPPASTTemplateDeclaration) node).getDeclaration().getChildren()) {
            if (nodechild instanceof CPPASTCompoundStatement) {
                for (IASTNode nodechild2 : nodechild.getChildren()) {
                    addChild(nodechild2, functionNode, lines, lineNumbersSwitchCase, vevosContainer);
                }
            }
        }
        Artifact.Op<LineArtifactData> lineArtifact = this.entityFactory.createArtifact(new LineArtifactData("}"));
        // TODO
        Node.Op lineNode = this.entityFactory.createOrderedNode(lineArtifact);
        functionNode.addChild(lineNode);
        this.checkForFeatureTrace(node.getFileLocation().getEndingLineNumber(), node.getFileLocation().getEndingLineNumber(), vevosContainer, lineNode);
    }
    
    private void readField(IASTNode node,
                           Node.Op fieldsNode,
                           ArrayList<Integer> lineNumbers,
                           VevosFileConditionContainer vevosContainer){
        Artifact.Op<FieldArtifactData> fieldArtifact = this.entityFactory.createArtifact(new FieldArtifactData(node.getRawSignature()));
        // TODO
        Node.Op fieldNode = this.entityFactory.createOrderedNode(fieldArtifact);
        fieldsNode.addChild(fieldNode);
        this.checkForFeatureTrace(node.getFileLocation().getStartingLineNumber(), node.getFileLocation().getEndingLineNumber(), vevosContainer, fieldNode);
        lineNumbers.add(node.getFileLocation().getStartingLineNumber());
        lineNumbers.add(node.getFileLocation().getEndingLineNumber());
    }
    
    private void readLine(Map<String, Integer> errorStatements,
                          Node.Op functionNode,
                          Map.Entry<String, Integer> errorst,
                          VevosFileConditionContainer vevosContainer){
        Artifact.Op<LineArtifactData> lineArtifact = this.entityFactory.createArtifact(new LineArtifactData(errorst.getKey()));
        // TODO
        Node.Op lineNode = this.entityFactory.createOrderedNode(lineArtifact);
        functionNode.addChild(lineNode);
        this.checkForFeatureTrace(errorst.getValue(), errorst.getValue(), vevosContainer, lineNode);
        errorStatements.computeIfPresent(errorst.getKey(), (k, v) -> -1);
    }

    private void readNodeMacros(IASTNode node, Node.Op functionNode, VevosFileConditionContainer vevosContainer){
        for (IASTPreprocessorMacroDefinition macro : node.getTranslationUnit().getMacroDefinitions()) {
            if (macro.getFileLocation().equals(node.getFileLocation()) && macro.getFileLocation().getStartingLineNumber() > node.getFileLocation().getStartingLineNumber() && macro.getFileLocation().getStartingLineNumber() < node.getFileLocation().getEndingLineNumber()) {
                Artifact.Op<LineArtifactData> lineArtifact = this.entityFactory.createArtifact(new LineArtifactData(macro.getRawSignature()));
                // TODO
                Node.Op lineNode = this.entityFactory.createOrderedNode(lineArtifact);
                functionNode.addChild(lineNode);
                this.checkForFeatureTrace(macro.getFileLocation().getStartingLineNumber(), macro.getFileLocation().getEndingLineNumber(), vevosContainer, lineNode);
            }
        }
    }

    private void readBlock(IASTNode node,
                           Node.Op parentNode,
                           VevosFileConditionContainer vevosContainer){
        Artifact.Op<BlockArtifactData> blockArtifact = this.entityFactory.createArtifact(new BlockArtifactData(node.getRawSignature()));
        // TODO
        Node.Op blockNode = this.entityFactory.createOrderedNode(blockArtifact);
        parentNode.addChild(blockNode);
        System.out.println(node + "  " + node.getRawSignature());
    }
    
    public void addChild(IASTNode node,
                         Node.Op parentNode,
                         String[] lines,
                         ArrayList<Integer> lineNumbersSwitchCase,
                         VevosFileConditionContainer vevosContainer) {
        if (node instanceof IASTExpressionStatement ||
                node instanceof CPPASTContinueStatement ||
                node instanceof IASTDeclarationStatement ||
                node instanceof IASTReturnStatement ||
                node instanceof IASTLabelStatement ||
                node instanceof IASTGotoStatement ||
                node instanceof IASTBinaryExpression ||
                node instanceof IASTFunctionCallExpression ||
                node instanceof CPPASTBreakStatement ||
                node instanceof CPPASTFieldReference ||
                node instanceof CPPASTLiteralExpression) {
            this.arbitraryAddChildSubFunction(node, parentNode, lines, lineNumbersSwitchCase, vevosContainer);

        } else if (node instanceof IASTIfStatement) {
            this.addIASTIfStatementAsChildren(node, parentNode, lines, lineNumbersSwitchCase, vevosContainer);

        } else if (node instanceof IASTWhileStatement) {
            this.addIASTWhileStatementAsChildren(node, parentNode, lines, lineNumbersSwitchCase, vevosContainer);

        } else if (node instanceof IASTForStatement) {
            this.addIASTForStatementAsChildren(node, parentNode, lines, lineNumbersSwitchCase, vevosContainer);

        } else if (node instanceof IASTSwitchStatement) {
            this.addIASTSwitchStatementAsChildren(node, parentNode, lines, lineNumbersSwitchCase, vevosContainer);

        } else if (node instanceof CPPASTCompoundStatement) {
            this.addCPPASTCompoundStatementAsChildren(node, parentNode, lines, lineNumbersSwitchCase, vevosContainer);

        } else if (node instanceof CPPASTDefaultStatement) {
            this.addCPPASTDefaultStatementAsChildren(node, parentNode, vevosContainer);

        } else if (node instanceof IASTDoStatement) {
            this.addIASTDoStatementAsChildren(node, parentNode, lines, lineNumbersSwitchCase, vevosContainer);

        } else if (node instanceof CPPASTIdExpression) {
            this.addCPPASTIdExpressionAsChildren(node, parentNode, lines, vevosContainer);

        } else if (node instanceof CPPASTUnaryExpression) {
            this.addCPPASTUnaryExpressionAsChildren(node, parentNode, lines, lineNumbersSwitchCase, vevosContainer);

        } else if (node instanceof CPPASTProblemStatement) {
            this.addCPPASTProblemStatementAsChildren(node, parentNode, lines, lineNumbersSwitchCase, vevosContainer);

        } else if (node instanceof CPPASTProblem) {
            this.addCPPASTProblemAsChildren(node, parentNode, vevosContainer);

        } else if (node instanceof CPPASTNullStatement) {
            this.readBlock(node, parentNode, vevosContainer);
            // TODO: Assert is not recognized by parser

        } else {
            throw new RuntimeException("Corner Case discovered: " + this.astNodeToLocationString(node));
            // todo: uncomment:
            //this.readBlock(node, parentNode, vevosContainer);
        }
    }

    private void arbitraryAddChildSubFunction(IASTNode node,
                                      Node.Op parentNode,
                                      String[] lines,
                                      ArrayList<Integer> lineNumbersSwitchCase,
                                      VevosFileConditionContainer vevosContainer){
        Artifact.Op<LineArtifactData> lineArtifact;
        Node.Op lineNode;
        IASTTranslationUnit tu = node.getTranslationUnit();
        final Boolean[] second = {false};
        tu.accept(new ASTVisitor() {
            {
                shouldVisitInitializers = true;
            }
            @Override
            public int visit(IASTInitializer initializer) {
                if (initializer.getParent() instanceof CPPASTArrayDeclarator) {
                    second[0] = true;
                }
                return ASTVisitor.PROCESS_CONTINUE;
            }
        });
        if (node.getFileLocation().getStartingLineNumber() != node.getFileLocation().getEndingLineNumber() && second[0]) {
            Artifact.Op<BlockArtifactData> blockArtifactDataOp = this.entityFactory.createArtifact(new BlockArtifactData(node.getRawSignature()));
            Node.Op blocknode = this.entityFactory.createOrderedNode(blockArtifactDataOp);
            parentNode.addChild(blocknode);
            this.checkForFeatureTrace(node.getFileLocation().getStartingLineNumber(), node.getFileLocation().getEndingLineNumber(), vevosContainer, blocknode);
        } else {
            if (node.getFileLocation().getStartingLineNumber() != node.getFileLocation().getEndingLineNumber()) {
                for (int o = node.getFileLocation().getStartingLineNumber() - 1; o <= node.getFileLocation().getEndingLineNumber() - 1; o++) {
                    lineArtifact = this.entityFactory.createArtifact(new LineArtifactData(lines[o]));
                    lineNode = this.entityFactory.createOrderedNode(lineArtifact);
                    parentNode.addChild(lineNode);
                    this.checkForFeatureTrace(o + 1, o + 1, vevosContainer, lineNode);
                    lineNumbersSwitchCase.add(o);
                }
            } else if (!lineNumbersSwitchCase.contains(node.getFileLocation().getStartingLineNumber() - 1)) {
                lineNumbersSwitchCase.add(node.getFileLocation().getStartingLineNumber() - 1);
                lineArtifact = this.entityFactory.createArtifact(new LineArtifactData(lines[node.getFileLocation().getStartingLineNumber() - 1]));
                lineNode = this.entityFactory.createOrderedNode(lineArtifact);
                this.checkForFeatureTrace(node.getFileLocation().getStartingLineNumber(), node.getFileLocation().getStartingLineNumber(), vevosContainer, lineNode);
                parentNode.addChild(lineNode);
            }
        }
    }

    private void addIASTIfStatementAsChildren(IASTNode node,
                                              Node.Op parentNode,
                                              String[] lines,
                                              ArrayList<Integer> lineNumbersSwitchCase,
                                              VevosFileConditionContainer vevosContainer){
        if (((IASTIfStatement) node).getConditionExpression() != null) {
            boolean first = true;
            Artifact.Op<IfBlockArtifactData> blockArtifact;
            Node.Op blockNode = null;
            StringBuilder ifexpression = new StringBuilder();
            if (node.getFileLocation().getStartingLineNumber() == node.getFileLocation().getEndingLineNumber()) {
                if (!lineNumbersSwitchCase.contains(node.getFileLocation().getStartingLineNumber() - 1)) {
                    blockArtifact = this.entityFactory.createArtifact(new IfBlockArtifactData(lines[node.getFileLocation().getStartingLineNumber() - 1]));
                    blockNode = this.entityFactory.createOrderedNode(blockArtifact);
                    parentNode.addChild(blockNode);
                    this.checkForFeatureTrace(node.getFileLocation().getStartingLineNumber(), node.getFileLocation().getStartingLineNumber(), vevosContainer, blockNode);
                    lineNumbersSwitchCase.add(node.getFileLocation().getStartingLineNumber() - 1);
                }
            } else {
                if (((ICPPASTIfStatement) node).getThenClause() != null) {
                    int init = node.getFileLocation().getStartingLineNumber();
                    int end = ((ICPPASTIfStatement) node).getConditionExpression().getFileLocation().getEndingLineNumber();
                    for (int i = node.getFileLocation().getStartingLineNumber() - 1; i <= ((ICPPASTIfStatement) node).getConditionExpression().getFileLocation().getEndingLineNumber() - 1; i++) {
                        if (!ifexpression.toString().equals(""))
                            ifexpression.append("\n").append(lines[i]);
                        else
                            ifexpression.append(lines[i]);
                    }
                    if (ifexpression.toString().equals("")) {
                        ifexpression = new StringBuilder(lines[((ICPPASTIfStatement) node).getFileLocation().getStartingLineNumber() - 1]);
                    }
                    if (((ICPPASTIfStatement) node).getConditionExpression().getFileLocation().getEndingLineNumber() - ((ICPPASTIfStatement) node).getThenClause().getFileLocation().getStartingLineNumber() == -1)
                        if (lines[((ICPPASTIfStatement) node).getConditionExpression().getFileLocation().getEndingLineNumber()].trim().equals("){") || lines[((ICPPASTIfStatement) node).getConditionExpression().getFileLocation().getEndingLineNumber()].trim().equals("{")) {
                            ifexpression.append("\n").append(lines[((ICPPASTIfStatement) node).getThenClause().getFileLocation().getStartingLineNumber() - 1]);
                            end = ((ICPPASTIfStatement) node).getThenClause().getFileLocation().getStartingLineNumber();
                        }
                    blockArtifact = this.entityFactory.createArtifact(new IfBlockArtifactData(ifexpression.toString()));
                    blockNode = this.entityFactory.createOrderedNode(blockArtifact);
                    this.checkForFeatureTrace(init, end, vevosContainer, blockNode);
                    parentNode.addChild(blockNode);
                    if (((ICPPASTIfStatement) node).getThenClause().getFileLocation().getEndingLineNumber() != node.getFileLocation().getStartingLineNumber()) {
                        if (((ICPPASTIfStatement) node).getConditionExpression().getFileLocation().getEndingLineNumber() == ((ICPPASTIfStatement) node).getThenClause().getFileLocation().getStartingLineNumber())
                            lineNumbersSwitchCase.add(((ICPPASTIfStatement) node).getThenClause().getFileLocation().getStartingLineNumber() - 1);
                        addChild(((ICPPASTIfStatement) node).getThenClause(), blockNode, lines, lineNumbersSwitchCase, vevosContainer);
                    }
                }
                if (blockNode == null) {
                    String ifaux = lines[((IASTIfStatement) node).getConditionExpression().getFileLocation().getStartingLineNumber() - 1].substring(lines[((IASTIfStatement) node).getConditionExpression().getFileLocation().getStartingLineNumber() - 1].lastIndexOf(")") + 1);
                    blockArtifact = this.entityFactory.createArtifact(new IfBlockArtifactData("if(" + ((IASTIfStatement) node).getConditionExpression().getRawSignature() + ") " + ifaux));
                    // TODO
                    blockNode = this.entityFactory.createOrderedNode(blockArtifact);
                    parentNode.addChild(blockNode);
                    first = false;
                }
                if (((ICPPASTIfStatement) node).getElseClause() != null) {
                    if (((ICPPASTIfStatement) node).getElseClause().getFileLocation().getStartingLineNumber() != ((ICPPASTIfStatement) node).getElseClause().getFileLocation().getEndingLineNumber()) {
                        for (int i = ((ICPPASTIfStatement) node).getElseClause().getFileLocation().getStartingLineNumber() - 1; i <= ((ICPPASTIfStatement) node).getElseClause().getFileLocation().getEndingLineNumber() - 1; i++) {
                            String elsestring = "";
                            if (!lines[i].contains("else") && blockNode.getArtifact().getData().toString().contains("{") && i == ((ICPPASTIfStatement) node).getElseClause().getFileLocation().getStartingLineNumber() - 1 && lines[((ICPPASTIfStatement) node).getElseClause().getFileLocation().getStartingLineNumber() - 1].contains("}")) {
                                elsestring += "}else" + "\n";
                            } else if (!lines[i].contains("else") && i == ((ICPPASTIfStatement) node).getElseClause().getFileLocation().getStartingLineNumber() - 1) {
                                elsestring += "else" + "\n";
                            }
                            Artifact.Op<LineArtifactData> lineArtifact = this.entityFactory.createArtifact(new LineArtifactData((elsestring + lines[i])));
                            Node.Op lineNodeChild = this.entityFactory.createOrderedNode(lineArtifact);
                            this.checkForFeatureTrace(i + 1, i + 1, vevosContainer, lineNodeChild);
                            blockNode.addChild(lineNodeChild);
                        }
                    } else {
                        Artifact.Op<LineArtifactData> lineArtifact = null;
                        if (lines[((ICPPASTIfStatement) node).getElseClause().getFileLocation().getStartingLineNumber() - 1].contains("else")) {
                            lineArtifact = this.entityFactory.createArtifact(new LineArtifactData((lines[((ICPPASTIfStatement) node).getElseClause().getFileLocation().getStartingLineNumber() - 1])));
                            // TODO
                            Node.Op lineNodeChild = this.entityFactory.createOrderedNode(lineArtifact);
                            blockNode.addChild(lineNodeChild);
                        } else {
                            lineArtifact = this.entityFactory.createArtifact(new LineArtifactData(("else")));
                            Node.Op lineNodeChild = this.entityFactory.createOrderedNode(lineArtifact);
                            int elseLineNumber = ((ICPPASTIfStatement) node).getElseClause().getFileLocation().getStartingLineNumber() - 1;
                            this.checkForFeatureTrace(elseLineNumber, elseLineNumber, vevosContainer, lineNodeChild);
                            blockNode.addChild(lineNodeChild);
                            lineArtifact = this.entityFactory.createArtifact(new LineArtifactData((lines[((ICPPASTIfStatement) node).getElseClause().getFileLocation().getStartingLineNumber() - 1])));
                            lineNodeChild = this.entityFactory.createOrderedNode(lineArtifact);
                            int elseStatementLineNumber = ((ICPPASTIfStatement) node).getElseClause().getFileLocation().getStartingLineNumber();
                            this.checkForFeatureTrace(elseStatementLineNumber, elseStatementLineNumber, vevosContainer, lineNodeChild);
                            blockNode.addChild(lineNodeChild);
                        }
                    }
                }
                if (!first && blockNode.getArtifact().getData().toString().contains("{")) {
                    Artifact.Op<LineArtifactData> lineArtifact = this.entityFactory.createArtifact(new LineArtifactData(("}")));
                    // TODO
                    Node.Op lineNodeChild = this.entityFactory.createOrderedNode(lineArtifact);
                    blockNode.addChild(lineNodeChild);
                }
            }
        } else {
            Node.Op blockNode;
            if (((ICPPASTIfStatement) node).getThenClause().getChildren().length > 0) {
                String ifaux = lines[((ICPPASTIfStatement) node).getConditionDeclaration().getFileLocation().getStartingLineNumber() - 1].substring(lines[((ICPPASTIfStatement) node).getConditionDeclaration().getFileLocation().getStartingLineNumber() - 1].lastIndexOf(")") + 1);
                Artifact.Op<IfBlockArtifactData> blockArtifact = this.entityFactory.createArtifact(new IfBlockArtifactData("if(" + ((ICPPASTIfStatement) node).getConditionDeclaration().getRawSignature() + ") " + ifaux));
                blockNode = this.entityFactory.createOrderedNode(blockArtifact);
                this.checkForFeatureTrace(((ICPPASTIfStatement) node).getConditionDeclaration().getFileLocation().getStartingLineNumber(), ((ICPPASTIfStatement) node).getConditionDeclaration().getFileLocation().getStartingLineNumber(), vevosContainer, blockNode);
                parentNode.addChild(blockNode);
                for (IASTNode child : ((ICPPASTIfStatement) node).getThenClause().getChildren()) {
                    addChild(child, blockNode, lines, lineNumbersSwitchCase, vevosContainer);
                }
                Artifact.Op<LineArtifactData> lineArtifact = this.entityFactory.createArtifact(new LineArtifactData(("}")));
                // TODO
                Node.Op lineNodeChild = this.entityFactory.createOrderedNode(lineArtifact);
                blockNode.addChild(lineNodeChild);
            } else {
                String ifaux = lines[((ICPPASTIfStatement) node).getConditionDeclaration().getFileLocation().getStartingLineNumber() - 1].substring(lines[((ICPPASTIfStatement) node).getConditionDeclaration().getFileLocation().getStartingLineNumber() - 1].lastIndexOf(")") + 1);
                Artifact.Op<IfBlockArtifactData> blockArtifact = this.entityFactory.createArtifact(new IfBlockArtifactData("if(" + ((ICPPASTIfStatement) node).getConditionDeclaration().getRawSignature() + ") " + ifaux));
                // TODO
                blockNode = this.entityFactory.createOrderedNode(blockArtifact);
                parentNode.addChild(blockNode);
                for (IASTNode child : ((ICPPASTIfStatement) node).getThenClause().getChildren()) {
                    addChild(child, blockNode, lines, lineNumbersSwitchCase, vevosContainer);
                }
            }
            if (((ICPPASTIfStatement) node).getElseClause() != null) {
                String elseline = "";
                if (!lines[((ICPPASTIfStatement) node).getElseClause().getFileLocation().getStartingLineNumber() - 1].contains("else")) {
                    elseline = ("else\n");
                }
                if (((ICPPASTIfStatement) node).getElseClause().getFileLocation().getStartingLineNumber() != ((ICPPASTIfStatement) node).getThenClause().getFileLocation().getEndingLineNumber()) {
                    for (int i = ((ICPPASTIfStatement) node).getElseClause().getFileLocation().getStartingLineNumber() - 1; i <= ((ICPPASTIfStatement) node).getElseClause().getFileLocation().getEndingLineNumber() - 1; i++) {
                        Artifact.Op<LineArtifactData> lineArtifact = this.entityFactory.createArtifact(new LineArtifactData((elseline + lines[i])));
                        // TODO
                        Node.Op lineNodeChild = this.entityFactory.createOrderedNode(lineArtifact);
                        blockNode.addChild(lineNodeChild);
                        elseline = "";
                    }
                } else {
                    for (int i = ((ICPPASTIfStatement) node).getElseClause().getFileLocation().getStartingLineNumber() - 1; i < ((ICPPASTIfStatement) node).getElseClause().getFileLocation().getEndingLineNumber() - 1; i++) {
                        Artifact.Op<LineArtifactData> lineArtifact = this.entityFactory.createArtifact(new LineArtifactData((lines[i])));
                        // TODO
                        Node.Op lineNodeChild = this.entityFactory.createOrderedNode(lineArtifact);
                        blockNode.addChild(lineNodeChild);
                    }
                }
            }
        }
    }
    
    private void addIASTWhileStatementAsChildren(IASTNode node,
                                            Node.Op parentNode,
                                            String[] lines,
                                            ArrayList<Integer> lineNumbersSwitchCase,
                                            VevosFileConditionContainer vevosContainer){
        if (((IASTWhileStatement) node).getCondition() != null) {
            String whileaux = lines[((IASTWhileStatement) node).getCondition().getFileLocation().getStartingLineNumber() - 1].substring(lines[((IASTWhileStatement) node).getCondition().getFileLocation().getStartingLineNumber() - 1].lastIndexOf(")") + 1);
            if (node.getFileLocation().getStartingLineNumber() != node.getFileLocation().getEndingLineNumber()) {
                Artifact.Op<WhileBlockArtifactData> blockArtifact;
                if (((IASTWhileStatement) node).getCondition().getFileLocation().getStartingLineNumber() == ((IASTWhileStatement) node).getCondition().getFileLocation().getEndingLineNumber())
                    blockArtifact = this.entityFactory.createArtifact(new WhileBlockArtifactData("while( " + ((IASTWhileStatement) node).getCondition().getRawSignature() + " ) " + whileaux));
                else
                    blockArtifact = this.entityFactory.createArtifact(new WhileBlockArtifactData("while( " + ((IASTWhileStatement) node).getCondition().getRawSignature() + " ) "));
                Node.Op blockNode = this.entityFactory.createOrderedNode(blockArtifact);
                this.checkForFeatureTrace(node.getFileLocation().getStartingLineNumber(), node.getFileLocation().getStartingLineNumber(), vevosContainer, blockNode);
                parentNode.addChild(blockNode);

                for (IASTNode child : ((ICPPASTWhileStatement) node).getBody().getChildren()) {
                    if (((IASTWhileStatement) node).getBody() instanceof CPPASTIfStatement) {
                        addChild(((ICPPASTWhileStatement) node).getBody(), blockNode, lines, lineNumbersSwitchCase, vevosContainer);
                        break;
                    } else {
                        addChild(child, blockNode, lines, lineNumbersSwitchCase, vevosContainer);
                    }
                }
                if (whileaux.contains("{")) {
                    Artifact.Op<LineArtifactData> lineArtifact = this.entityFactory.createArtifact(new LineArtifactData(("}")));
                    Node.Op lineNodeChild = this.entityFactory.createOrderedNode(lineArtifact);
                    this.checkForFeatureTrace(node.getFileLocation().getEndingLineNumber(), node.getFileLocation().getEndingLineNumber(), vevosContainer, lineNodeChild);
                    blockNode.addChild(lineNodeChild);
                }
            } else {
                Artifact.Op<WhileBlockArtifactData> blockArtifact = this.entityFactory.createArtifact(new WhileBlockArtifactData(lines[node.getFileLocation().getStartingLineNumber() - 1]));
                // TODO
                Node.Op blockNode = this.entityFactory.createOrderedNode(blockArtifact);
                parentNode.addChild(blockNode);
            }
        } else {
            if (node.getFileLocation().getStartingLineNumber() != node.getFileLocation().getEndingLineNumber()) {

                Artifact.Op<WhileBlockArtifactData> blockArtifact = this.entityFactory.createArtifact(new WhileBlockArtifactData("while( " + ((ICPPASTWhileStatement) node).getConditionDeclaration().getRawSignature() + ") {"));
                // TODO
                Node.Op blockNode = this.entityFactory.createOrderedNode(blockArtifact);
                parentNode.addChild(blockNode);
                for (IASTNode child : ((ICPPASTWhileStatement) node).getBody().getChildren()) {
                    addChild(child, blockNode, lines, lineNumbersSwitchCase, vevosContainer);
                }
                Artifact.Op<LineArtifactData> lineArtifact = this.entityFactory.createArtifact(new LineArtifactData(("}")));
                // TODO
                Node.Op lineNodeChild = this.entityFactory.createOrderedNode(lineArtifact);
                blockNode.addChild(lineNodeChild);
            } else {
                Artifact.Op<WhileBlockArtifactData> blockArtifact = this.entityFactory.createArtifact(new WhileBlockArtifactData(lines[node.getFileLocation().getStartingLineNumber() - 1]));
                // TODO
                Node.Op blockNode = this.entityFactory.createOrderedNode(blockArtifact);
                parentNode.addChild(blockNode);
            }
        }
    }
    
    private void addIASTForStatementAsChildren(IASTNode node,
                                               Node.Op parentNode,
                                               String[] lines,
                                               ArrayList<Integer> lineNumbersSwitchCase,
                                               VevosFileConditionContainer vevosContainer){
        if (node.getFileLocation().getStartingLineNumber() != node.getFileLocation().getEndingLineNumber()) {
            String init = ";";
            if (((IASTForStatement) node).getInitializerStatement() != null) {
                init = ((IASTForStatement) node).getInitializerStatement().getRawSignature();
            }
            if (init.equals(";") && !node.getRawSignature().substring(node.getRawSignature().indexOf("(") + 1, node.getRawSignature().indexOf(";")).equals(";") && !node.getRawSignature().substring(node.getRawSignature().indexOf("(") + 1, node.getRawSignature().indexOf(";")).equals(""))
                init = node.getRawSignature().substring(node.getRawSignature().indexOf("(") + 1, node.getRawSignature().indexOf(";") + 1);
            String condition = "";
            if (((IASTForStatement) node).getConditionExpression() != null) {
                condition = ((IASTForStatement) node).getConditionExpression().getRawSignature();
            } else if (node instanceof ICPPASTForStatement && ((ICPPASTForStatement) node).getConditionDeclaration() != null) {
                condition = ((ICPPASTForStatement) node).getConditionDeclaration().getRawSignature();
            }
            String iteration = "";
            if (((IASTForStatement) node).getIterationExpression() != null) {
                iteration = ((IASTForStatement) node).getIterationExpression().getRawSignature();
            }
            String foraux = "";
            int i = 0;
            if (((IASTForStatement) node).getIterationExpression() != null) {
                i = ((IASTForStatement) node).getIterationExpression().getFileLocation().getStartingLineNumber();
                foraux = lines[((IASTForStatement) node).getIterationExpression().getFileLocation().getStartingLineNumber() - 1].substring(lines[((IASTForStatement) node).getIterationExpression().getFileLocation().getStartingLineNumber() - 1].lastIndexOf(")") + 1);
            } else if (((IASTForStatement) node).getConditionExpression() != null) {
                i = ((IASTForStatement) node).getConditionExpression().getFileLocation().getStartingLineNumber();
                foraux = lines[((IASTForStatement) node).getConditionExpression().getFileLocation().getStartingLineNumber() - 1].substring(lines[((IASTForStatement) node).getConditionExpression().getFileLocation().getStartingLineNumber() - 1].lastIndexOf(")") + 1);
            } else if (node instanceof ICPPASTForStatement && ((ICPPASTForStatement) node).getConditionDeclaration() != null) {
                i = ((ICPPASTForStatement) node).getConditionDeclaration().getFileLocation().getStartingLineNumber();
                foraux = lines[((ICPPASTForStatement) node).getConditionDeclaration().getFileLocation().getStartingLineNumber() - 1].substring(lines[((ICPPASTForStatement) node).getConditionDeclaration().getFileLocation().getStartingLineNumber() - 1].lastIndexOf(")") + 1);
            }
            if (foraux.equals("") && lines[i].trim().equals("{"))
                foraux = "\n{";
            Artifact.Op<ForBlockArtifactData> blockArtifact = this.entityFactory.createArtifact(new ForBlockArtifactData("for(" + init + " " + condition + "; " + iteration + ")" + foraux));
            Node.Op blockNode = this.entityFactory.createOrderedNode(blockArtifact);
            this.checkForFeatureTrace(node.getFileLocation().getStartingLineNumber(), node.getFileLocation().getStartingLineNumber(), vevosContainer, blockNode);

            parentNode.addChild(blockNode);
            for (IASTNode child : ((IASTForStatement) node).getBody().getChildren()) {
                if (((IASTForStatement) node).getBody() != null && ((IASTForStatement) node).getBody() instanceof CPPASTIfStatement || ((IASTForStatement) node).getBody() instanceof CPPASTForStatement) {
                    addChild(((IASTForStatement) node).getBody(), blockNode, lines, lineNumbersSwitchCase, vevosContainer);
                    break;
                }
                if (child instanceof CPPASTArraySubscriptExpression)
                    addChild(child.getParent(), blockNode,  lines, lineNumbersSwitchCase, vevosContainer);
                else
                    addChild(child, blockNode, lines, lineNumbersSwitchCase, vevosContainer);
            }
            if (foraux.lastIndexOf("{") != -1) {
                Artifact.Op<LineArtifactData> lineArtifact = this.entityFactory.createArtifact(new LineArtifactData(("}")));
                Node.Op lineNodeChild = this.entityFactory.createOrderedNode(lineArtifact);
                this.checkForFeatureTrace(node.getFileLocation().getEndingLineNumber(), node.getFileLocation().getEndingLineNumber(), vevosContainer, lineNodeChild);
                blockNode.addChild(lineNodeChild);
            }
        } else {
            Artifact.Op<ForBlockArtifactData> blockArtifact = this.entityFactory.createArtifact(new ForBlockArtifactData(lines[node.getFileLocation().getStartingLineNumber() - 1]));
            // TODO
            Node.Op blockNode = this.entityFactory.createOrderedNode(blockArtifact);
            parentNode.addChild(blockNode);
        }
    }

    private void addIASTSwitchStatementAsChildren(IASTNode node,
                                                  Node.Op parentNode,
                                                  String[] lines,
                                                  ArrayList<Integer> lineNumbersSwitchCase,
                                                  VevosFileConditionContainer vevosContainer){
        if (((IASTSwitchStatement) node).getControllerExpression() != null) {
            String switchline = "switch(" + ((IASTSwitchStatement) node).getControllerExpression().getRawSignature() + ")";
            if (lines[((IASTSwitchStatement) node).getControllerExpression().getFileLocation().getEndingLineNumber()].trim().equals("{"))
                switchline += "\n{";
            else
                switchline += "{";
            Artifact.Op<SwitchBlockArtifactData> blockArtifact = this.entityFactory.createArtifact(new SwitchBlockArtifactData(switchline));
            Node.Op blockNode = this.entityFactory.createOrderedNode(blockArtifact);
            this.checkForFeatureTrace(((IASTSwitchStatement) node).getControllerExpression().getFileLocation().getStartingLineNumber(), ((IASTSwitchStatement) node).getControllerExpression().getFileLocation().getEndingLineNumber(), vevosContainer, blockNode);
            parentNode.addChild(blockNode);
            Node.Op blockChildNode = blockNode;
            Artifact.Op<CaseBlockArtifactData> blockCaseArtifact;
            for (IASTNode child : ((IASTSwitchStatement) node).getBody().getChildren()) {
                if (child instanceof CPPASTCaseStatement || child instanceof CPPASTDefaultStatement) {
                    if (lines[child.getFileLocation().getStartingLineNumber() - 1].contains("break;")) {
                        String nodelenght = lines[child.getFileLocation().getStartingLineNumber() - 1].substring(0, lines[child.getFileLocation().getStartingLineNumber() - 1].indexOf(":") + 1);
                        String nodelenghtupbreak = lines[child.getFileLocation().getStartingLineNumber() - 1].substring(nodelenght.length(), lines[child.getFileLocation().getStartingLineNumber() - 1].indexOf("break;"));

                        if (!lineNumbersSwitchCase.contains(child.getFileLocation().getStartingLineNumber() - 1)) {
                            blockCaseArtifact = this.entityFactory.createArtifact(new CaseBlockArtifactData(nodelenght));
                            lineNumbersSwitchCase.add(child.getFileLocation().getStartingLineNumber() - 1);
                            blockCaseArtifact.getData().setSameline(true);
                            // TODO
                            blockChildNode = this.entityFactory.createOrderedNode(blockCaseArtifact);
                            blockNode.addChild(blockChildNode);
                        }
                        Artifact.Op<LineArtifactData> lineArtifact = this.entityFactory.createArtifact(new LineArtifactData((nodelenghtupbreak)));
                        // TODO
                        Node.Op lineNodeChild = this.entityFactory.createOrderedNode(lineArtifact);
                        blockChildNode.addChild(lineNodeChild);
                        lineArtifact = this.entityFactory.createArtifact(new LineArtifactData(("break;")));
                        // TODO
                        lineNodeChild = this.entityFactory.createOrderedNode(lineArtifact);
                        blockChildNode.addChild(lineNodeChild);
                    } else {
                        if (!lineNumbersSwitchCase.contains(child.getFileLocation().getStartingLineNumber() - 1)) {
                            blockCaseArtifact = this.entityFactory.createArtifact(new CaseBlockArtifactData(lines[child.getFileLocation().getStartingLineNumber() - 1]));
                            lineNumbersSwitchCase.add(child.getFileLocation().getStartingLineNumber() - 1);
                            blockCaseArtifact.getData().setSameline(false);
                            // TODO
                            blockChildNode = this.entityFactory.createOrderedNode(blockCaseArtifact);
                            this.checkForFeatureTrace(child.getFileLocation().getStartingLineNumber(), child.getFileLocation().getStartingLineNumber(), vevosContainer, blockChildNode);
                            blockNode.addChild(blockChildNode);
                        }
                    }
                } else if (!lineNumbersSwitchCase.contains(Integer.valueOf(child.getFileLocation().getStartingLineNumber() - 1))) {
                    addChild(child, blockChildNode, lines, lineNumbersSwitchCase, vevosContainer);
                } else if (child.getChildren().length > 1) {
                    addChild(child, blockChildNode, lines, lineNumbersSwitchCase, vevosContainer);
                } else if (child instanceof CPPASTCompoundStatement) {
                    addChild(child, blockChildNode, lines, lineNumbersSwitchCase, vevosContainer);
                }
            }
            Artifact.Op<LineArtifactData> lineArtifact = this.entityFactory.createArtifact(new LineArtifactData(("}")));
            Node.Op lineNodeChild = this.entityFactory.createOrderedNode(lineArtifact);
            this.checkForFeatureTrace(node.getFileLocation().getEndingLineNumber(), node.getFileLocation().getEndingLineNumber(), vevosContainer, lineNodeChild);
            blockNode.addChild(lineNodeChild);
        } else {
            Artifact.Op<SwitchBlockArtifactData> blockArtifact = this.entityFactory.createArtifact(new SwitchBlockArtifactData("switch(" + ((ICPPASTSwitchStatement) node).getControllerDeclaration().getRawSignature() + "){"));
            // TODO
            Node.Op blockNode = this.entityFactory.createOrderedNode(blockArtifact);
            parentNode.addChild(blockNode);
            Node.Op blockChildNode = null;
            Artifact.Op<CaseBlockArtifactData> blockCaseArtifact = null;
            for (IASTNode child : ((ICPPASTSwitchStatement) node).getBody().getChildren()) {
                if (child instanceof CPPASTCaseStatement || child instanceof CPPASTDefaultStatement) {
                    if (lines[child.getFileLocation().getStartingLineNumber() - 1].contains("break;")) {
                        String nodelenght = lines[child.getFileLocation().getStartingLineNumber() - 1].substring(0, lines[child.getFileLocation().getStartingLineNumber() - 1].indexOf(":") + 1);
                        String nodelenghtupbreak = lines[child.getFileLocation().getStartingLineNumber() - 1].substring(nodelenght.length(), lines[child.getFileLocation().getStartingLineNumber() - 1].indexOf("break;"));

                        if (!lineNumbersSwitchCase.contains(child.getFileLocation().getStartingLineNumber() - 1)) {
                            blockCaseArtifact = this.entityFactory.createArtifact(new CaseBlockArtifactData(nodelenght));
                            lineNumbersSwitchCase.add(child.getFileLocation().getStartingLineNumber() - 1);
                            blockCaseArtifact.getData().setSameline(true);
                            // TODO
                            blockChildNode = this.entityFactory.createOrderedNode(blockCaseArtifact);
                            blockNode.addChild(blockChildNode);
                        }
                        Artifact.Op<LineArtifactData> lineArtifact = this.entityFactory.createArtifact(new LineArtifactData((nodelenghtupbreak)));
                        // TODO
                        Node.Op lineNodeChild = this.entityFactory.createOrderedNode(lineArtifact);
                        blockChildNode.addChild(lineNodeChild);
                        lineArtifact = this.entityFactory.createArtifact(new LineArtifactData(("break;")));
                        // TODO
                        lineNodeChild = this.entityFactory.createOrderedNode(lineArtifact);
                        blockChildNode.addChild(lineNodeChild);
                    } else {
                        if (!lineNumbersSwitchCase.contains(child.getFileLocation().getStartingLineNumber() - 1)) {
                            blockCaseArtifact = this.entityFactory.createArtifact(new CaseBlockArtifactData(lines[child.getFileLocation().getStartingLineNumber() - 1]));
                            lineNumbersSwitchCase.add(child.getFileLocation().getStartingLineNumber() - 1);
                            blockCaseArtifact.getData().setSameline(false);
                            // TODO
                            blockChildNode = this.entityFactory.createOrderedNode(blockCaseArtifact);
                            blockNode.addChild(blockChildNode);
                        }
                    }
                } else if (!lineNumbersSwitchCase.contains(Integer.valueOf(child.getFileLocation().getStartingLineNumber() - 1))) {
                    addChild(child, blockChildNode, lines, lineNumbersSwitchCase, vevosContainer);
                } else if (child.getChildren().length > 1) {
                    addChild(child, blockChildNode, lines, lineNumbersSwitchCase, vevosContainer);
                } else if (child instanceof CPPASTCompoundStatement) {
                    addChild(child, blockChildNode, lines, lineNumbersSwitchCase, vevosContainer);
                }
            }
        }
    }
    
    private void addCPPASTCompoundStatementAsChildren(IASTNode node,
                                                      Node.Op parentNode,
                                                      String[] lines,
                                                      ArrayList<Integer> lineNumbersSwitchCase,
                                                      VevosFileConditionContainer vevosContainer){
        for (IASTNode child : node.getChildren()) {
            addChild(child, parentNode, lines, lineNumbersSwitchCase, vevosContainer);
        }
        if (parentNode.getArtifact().getData().toString().contains("{") && !parentNode.getArtifact().getData().toString().contains("do")) {
            if (node.getParent() instanceof CPPASTIfStatement && ((CPPASTIfStatement) node.getParent()).getElseClause() != null) {
                if (!lines[((CPPASTIfStatement) node.getParent()).getElseClause().getFileLocation().getStartingLineNumber() - 1].contains("}")) {
                    Artifact.Op<LineArtifactData> lineArtifact = this.entityFactory.createArtifact(new LineArtifactData(("}")));
                    // TODO
                    Node.Op lineNodeChild = this.entityFactory.createOrderedNode(lineArtifact);
                    parentNode.addChild(lineNodeChild);
                }
            } else {
                Artifact.Op<LineArtifactData> lineArtifact = this.entityFactory.createArtifact(new LineArtifactData(("}")));
                Node.Op lineNodeChild = this.entityFactory.createOrderedNode(lineArtifact);
                this.checkForFeatureTrace(node.getFileLocation().getEndingLineNumber(), node.getFileLocation().getEndingLineNumber(), vevosContainer, lineNodeChild);
                parentNode.addChild(lineNodeChild);
            }
        }
    }
    
    private void addCPPASTDefaultStatementAsChildren(IASTNode node,
                                                     Node.Op parentNode,
                                                     VevosFileConditionContainer vevosContainer){
        Artifact.Op<BlockArtifactData> blockArtifact = this.entityFactory.createArtifact(new BlockArtifactData(node.getRawSignature() + " {"));
        // TODO
        Node.Op blockChildNode = this.entityFactory.createOrderedNode(blockArtifact);
        parentNode.addChild(blockChildNode);
    }
    
    private void addIASTDoStatementAsChildren(IASTNode node,
                                              Node.Op parentNode,
                                              String[] lines,
                                              ArrayList<Integer> lineNumbersSwitchCase,
                                              VevosFileConditionContainer vevosContainer){
        if (node.getFileLocation().getStartingLineNumber() != node.getFileLocation().getEndingLineNumber()) {
            Artifact.Op<DoBlockArtifactData> blockArtifact = this.entityFactory.createArtifact(new DoBlockArtifactData("do{"));
            Node.Op blockNode = this.entityFactory.createOrderedNode(blockArtifact);
            this.checkForFeatureTrace(node.getFileLocation().getStartingLineNumber(), node.getFileLocation().getStartingLineNumber(), vevosContainer, blockNode);
            parentNode.addChild(blockNode);
            for (IASTNode child : node.getChildren()) {
                addChild(child, blockNode, lines, lineNumbersSwitchCase, vevosContainer);
            }
        } else if (!lineNumbersSwitchCase.contains(node.getFileLocation().getStartingLineNumber() - 1)) {
            String doblock = "do" + ((IASTDoStatement) node).getBody().getRawSignature() + "while(" + ((IASTDoStatement) node).getCondition().getRawSignature() + ");";
            Artifact.Op<DoBlockArtifactData> blockArtifact = this.entityFactory.createArtifact(new DoBlockArtifactData(doblock));//this.entityFactory.createArtifact(new DoBlockArtifactData("do"));
            // TODO: this may as well be a macro expansion, in which case the artifact is wrong!
            Node.Op blocknode = this.entityFactory.createOrderedNode(blockArtifact);
            //this.checkForFeatureTrace();
            parentNode.addChild(blocknode);
            lineNumbersSwitchCase.add(node.getFileLocation().getStartingLineNumber() - 1);
        }
    }
    
    private void addCPPASTIdExpressionAsChildren(IASTNode node,
                                                 Node.Op parentNode,
                                                 String[] lines,
                                                 VevosFileConditionContainer vevosContainer){
        Artifact.Op<LineArtifactData> lineArtifact = this.entityFactory.createArtifact(new LineArtifactData(lines[node.getFileLocation().getStartingLineNumber() - 1]));
        // TODO
        Node.Op lineNodeChild = this.entityFactory.createOrderedNode(lineArtifact);
        parentNode.addChild(lineNodeChild);
    }
    
    private void addCPPASTUnaryExpressionAsChildren(IASTNode node,
                                                    Node.Op parentNode,
                                                    String[] lines,
                                                    ArrayList<Integer> lineNumbersSwitchCase,
                                                    VevosFileConditionContainer vevosContainer){
        if (!parentNode.getArtifact().getData().toString().contains("do{")) {
            if (node.getParent() instanceof IASTIfStatement) {
                String line = lines[node.getParent().getFileLocation().getStartingLineNumber() - 1];
                Artifact.Op<IfBlockArtifactData> lineArtifact = this.entityFactory.createArtifact(new IfBlockArtifactData(line));
                // TODO
                Node.Op lineNodeChild = this.entityFactory.createOrderedNode(lineArtifact);
                parentNode.addChild(lineNodeChild);
            } else if (node.getParent() instanceof CPPASTReturnStatement) {
                String line = lines[node.getParent().getFileLocation().getStartingLineNumber() - 1];
                Artifact.Op<LineArtifactData> lineArtifact = this.entityFactory.createArtifact(new LineArtifactData(line));
                // TODO
                Node.Op lineNodeChild = this.entityFactory.createOrderedNode(lineArtifact);
                parentNode.addChild(lineNodeChild);
            } else {
                Artifact.Op<LineArtifactData> lineArtifact = this.entityFactory.createArtifact(new LineArtifactData(node.getRawSignature() + ";"));
                Node.Op lineNodeChild = this.entityFactory.createOrderedNode(lineArtifact);
                this.checkForFeatureTrace(node.getFileLocation().getStartingLineNumber(), node.getFileLocation().getStartingLineNumber(), vevosContainer, lineNodeChild);
                parentNode.addChild(lineNodeChild);
            }
        } else {
            Artifact.Op<LineArtifactData> lineArtifact = this.entityFactory.createArtifact(new LineArtifactData(lines[node.getFileLocation().getStartingLineNumber() - 1]));
            // TODO
            Node.Op lineNodeChild = this.entityFactory.createOrderedNode(lineArtifact);
            parentNode.addChild(lineNodeChild);
        }
    }
    
    private void addCPPASTProblemStatementAsChildren(IASTNode node,
                                                     Node.Op parentNode,
                                                     String[] lines,
                                                     ArrayList<Integer> lineNumbersSwitchCase,
                                                     VevosFileConditionContainer vevosContainer){
        if (!node.getRawSignature().equals(")") && node.getRawSignature().length() > 1) {
            if (!lineNumbersSwitchCase.contains(node.getFileLocation().getStartingLineNumber() - 1) && node.getFileLocation().getStartingLineNumber() == node.getFileLocation().getEndingLineNumber()) {
                Artifact.Op<ProblemBlockArtifactData> blockArtifact = this.entityFactory.createArtifact(new ProblemBlockArtifactData(lines[node.getFileLocation().getStartingLineNumber() - 1]));
                lineNumbersSwitchCase.add(node.getFileLocation().getStartingLineNumber() - 1);
                Node.Op blockNode = this.entityFactory.createOrderedNode(blockArtifact);
                this.checkForFeatureTrace(node.getFileLocation().getStartingLineNumber(), node.getFileLocation().getStartingLineNumber(), vevosContainer, blockNode);
                parentNode.addChild(blockNode);
            } else if (node.getFileLocation().getStartingLineNumber() != node.getFileLocation().getEndingLineNumber()) {
                StringBuilder line = new StringBuilder();
                int actualEnd = node.getFileLocation().getStartingLineNumber();
                for (int i = node.getFileLocation().getStartingLineNumber() - 1; i <= node.getFileLocation().getEndingLineNumber() - 1; i++) {
                    line.append(lines[i]).append("\n");
                    lineNumbersSwitchCase.add(i);
                    actualEnd = i + 1;
                }
                Artifact.Op<ProblemBlockArtifactData> blockArtifact = this.entityFactory.createArtifact(new ProblemBlockArtifactData(line.toString()));
                Node.Op blockNode = this.entityFactory.createOrderedNode(blockArtifact);
                this.checkForFeatureTrace(node.getFileLocation().getStartingLineNumber(), actualEnd, vevosContainer, blockNode);
                parentNode.addChild(blockNode);
            }
        }
    }
    
    private void addCPPASTProblemAsChildren(IASTNode node,
                                            Node.Op parentNode,
                                            VevosFileConditionContainer vevosContainer){
        if (!node.getRawSignature().equals(")") && node.getRawSignature().length() > 1) {
            Artifact.Op<ProblemBlockArtifactData> blockArtifact = this.entityFactory.createArtifact(new ProblemBlockArtifactData(node.getRawSignature()));
            // TODO
            Node.Op blockNode = this.entityFactory.createOrderedNode(blockArtifact);
            parentNode.addChild(blockNode);
        }
    }

    private static String getFileContentWithoutIfdefs(File f, IASTPreprocessorStatement[] ppStatements) throws IOException {
        StringBuilder content = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(f));
        String line = reader.readLine();
        while (line != null) {
            for (IASTPreprocessorStatement ppStatement : ppStatements) {
                if (ppStatement instanceof IASTPreprocessorIfdefStatement ||
                        ppStatement instanceof IASTPreprocessorIfndefStatement ||
                        ppStatement instanceof IASTPreprocessorIfStatement ||
                        ppStatement instanceof IASTPreprocessorElseStatement ||
                        ppStatement instanceof IASTPreprocessorElifStatement ||
                        ppStatement instanceof IASTPreprocessorEndifStatement) {
                    if (line.contains(ppStatement.getRawSignature())) {
                        line = line.replace(ppStatement.getRawSignature(), "//" + ppStatement.getRawSignature().substring(2));
                        break;
                    }
                }
            }
            content.append(line).append("\n");
            line = reader.readLine();
        }
        reader.close();
        return content.toString();
    }

    private void checkForFeatureTrace(int startLine, int endLine, VevosFileConditionContainer fileConditionContainer, Node.Op node){
        if (fileConditionContainer == null){ return; }
        Collection<VevosCondition> matchingConditions = fileConditionContainer.getMatchingPresenceConditions(startLine, endLine);
        for(VevosCondition condition : matchingConditions){
            FeatureTrace nodeTrace = node.getFeatureTrace();
            nodeTrace.buildUserConditionConjunction(condition.getConditionString());
        }
    }

    private String astNodeToLocationString(IASTNode node){
        return " File: " + node.getContainingFilename()
                + " Lines: " + node.getFileLocation().getStartingLineNumber() + " - " + node.getFileLocation().getEndingLineNumber()
                + " Class: " + node.getClass().getName();
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