package at.jku.isse.ecco.adapter.challenge.vevos;

import at.jku.isse.ecco.featuretracerecording.FeatureTraceCondition;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

public class VEVOSPresenceCondition {
    private final Path filePath;
    private String presenceCondition;
    private Collection<FeatureTraceCondition> featureTraceConditions;
    private final int startLineNumber;
    private final int endLineNumber;

    public VEVOSPresenceCondition(String vevosFileLine, LogicToModuleTransformer logicToModuleTransformer){
        // VEVOS file entry structure: Path;File Condition;Block Condition;Presence Condition;start;end

        String[] lineParts = vevosFileLine.split(";");
        if (lineParts.length < 6){
            throw new IllegalArgumentException(String.format("VEVOS file entry has less than 6 comma-separated parts: %s", vevosFileLine));
        }

        this.filePath = Paths.get(lineParts[0]);
        this.featureTraceConditions = logicToModuleTransformer.transformLogicalConditionToFeatureTraceCondition(lineParts[3]);
        this.startLineNumber = Integer.parseInt(lineParts[4]);
        this.endLineNumber = Integer.parseInt(lineParts[5]);
    }

    public Path getFilePath(){
        return this.filePath;
    }

    public int getStartLineNumber(){
        return this.startLineNumber;
    }

    public int getEndLineNumber(){
        return this.endLineNumber;
    }

    public Collection<FeatureTraceCondition> getFeatureTraceConditions(){
        return this.featureTraceConditions;
    }
}
