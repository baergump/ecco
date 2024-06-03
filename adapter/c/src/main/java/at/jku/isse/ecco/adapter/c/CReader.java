package at.jku.isse.ecco.adapter.c;

import at.jku.isse.ecco.adapter.ArtifactReader;
import at.jku.isse.ecco.adapter.dispatch.DispatchWriter;
import at.jku.isse.ecco.dao.EntityFactory;
import at.jku.isse.ecco.service.listener.ReadListener;
import at.jku.isse.ecco.tree.Node;
import com.google.inject.Inject;

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
        return null;
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
