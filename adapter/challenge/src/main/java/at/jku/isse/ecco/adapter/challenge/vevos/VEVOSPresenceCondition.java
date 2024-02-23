package at.jku.isse.ecco.adapter.challenge.vevos;

import at.jku.isse.ecco.featuretracerecording.FeatureTraceCondition;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

public class VEVOSPresenceCondition {
    private final Path filePath;
    private FeatureTraceCondition featureTraceConditions;
    private final int startLine;
    private final int endLine;

    public VEVOSPresenceCondition(String vevosFileLine, LogicToModuleTransformer logicToModuleTransformer){
        // VEVOS file entry structure: Path;File Condition;Block Condition;Presence Condition;start;end

        String[] lineParts = vevosFileLine.split(";");
        if (lineParts.length < 6){
            throw new IllegalArgumentException(String.format("VEVOS file entry has less than 6 comma-separated parts: %s", vevosFileLine));
        }

        this.filePath = Paths.get(lineParts[0]);
        this.featureTraceConditions = logicToModuleTransformer.transformLogicalConditionToFeatureTraceCondition(lineParts[3]);
        this.startLine = Integer.parseInt(lineParts[4]);
        this.endLine = Integer.parseInt(lineParts[5]);
    }

    public Path getFilePath(){
        return this.filePath;
    }

    public int getStartLine(){
        return this.startLine;
    }

    public int getEndLine(){
        return this.endLine;
    }

    public FeatureTraceCondition getFeatureTraceConditions(){
        return this.featureTraceConditions;
    }
}
