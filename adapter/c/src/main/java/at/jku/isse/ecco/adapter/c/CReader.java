package at.jku.isse.ecco.adapter.c;

import at.jku.isse.ecco.adapter.ArtifactReader;
import at.jku.isse.ecco.adapter.c.parser.CEccoTranslator;
import at.jku.isse.ecco.adapter.c.parser.CLexer;
import at.jku.isse.ecco.adapter.c.parser.CParser;
import at.jku.isse.ecco.adapter.dispatch.DispatchWriter;
import at.jku.isse.ecco.dao.EntityFactory;
import at.jku.isse.ecco.service.listener.ReadListener;
import at.jku.isse.ecco.tree.Node;
import com.google.inject.Inject;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

public class CReader implements ArtifactReader<Path, Set<Node.Op>> {

    protected static final Logger LOGGER = Logger.getLogger(DispatchWriter.class.getName());

    private final EntityFactory entityFactory;

    private Collection<ReadListener> listeners = new ArrayList<>();

    private static Map<Integer, String[]> prioritizedPatterns;

    static {
        prioritizedPatterns = new HashMap<>();
        prioritizedPatterns.put(Integer.MAX_VALUE, new String[]{"**.c", "**.h"});
    }

    @Inject
    public CReader(EntityFactory entityFactory) {
        checkNotNull(entityFactory);
        this.entityFactory = entityFactory;
    }

    @Override
    public String getPluginId() { return CPlugin.class.getName(); }

    @Override
    public Map<Integer, String[]> getPrioritizedPatterns() { return Collections.unmodifiableMap(prioritizedPatterns); }

    @Override
    public Set<Node.Op> read(Path base, Path[] input) {
        Set<Node.Op> nodes = new HashSet<>();
        for (Path path : input) {
            try{
                Path absolutePath = base.resolve(path);
                String absolutePathString = String.valueOf(absolutePath);
                CharStream contentStream = CharStreams.fromFileName(absolutePathString);
                List<String> lineList = Files.readAllLines(absolutePath);
                String[] lines = lineList.toArray(new String[0]);

                CLexer lexer = new CLexer(contentStream);
                CommonTokenStream tokens = new CommonTokenStream(lexer);
                CParser parser = new CParser(tokens);

                CEccoTranslator translator = new CEccoTranslator(lines, this.entityFactory);
                ParseTree tree = parser.translationUnit();

                Node.Op node = translator.translate(tree);





            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return nodes;
    }

    @Override
    public Set<Node.Op> read(Path[] input) { return this.read(Paths.get("."), input); }

    @Override
    public void addListener(ReadListener listener) {
        this.listeners.add(listener);
    }

    @Override
    public void removeListener(ReadListener listener) {
        this.listeners.remove(listener);
    }
}
