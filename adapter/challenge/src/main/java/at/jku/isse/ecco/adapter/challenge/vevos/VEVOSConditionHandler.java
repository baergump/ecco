package at.jku.isse.ecco.adapter.challenge.vevos;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.*;

public class VEVOSConditionHandler {

    private final String PRESENCE_CONDITION_FILENAME = "pcs.variant.csv";
    private final Path vevosFilePath;
    private boolean vevosFileExists;
    private List<VEVOSPresenceCondition> vevosPresenceConditions;
    private LogicToModuleTransformer logicToModuleTransformer;

    public VEVOSConditionHandler(Path vevosFileBasePath, LogicToModuleTransformer logicToModuleTransformer){
        this.vevosFilePath = vevosFileBasePath.resolve(this.PRESENCE_CONDITION_FILENAME);
        this.logicToModuleTransformer = logicToModuleTransformer;
        this.vevosFileExists = Files.exists(vevosFilePath);
        this.vevosPresenceConditions = new LinkedList<>();
        if (this.vevosFileExists){
            this.parsePresenceConditions();
        }
    }

    private void parsePresenceConditions() {
        try {
            List<String> vevosFileLines = Files.readAllLines(this.vevosFilePath);
            // first line in a VEVOS file just showcases structure
            vevosFileLines.remove(0);
            this.parseVevosFileLines(vevosFileLines);
        } catch (IOException e){
            // TODO
            throw new RuntimeException(String.format("VEVOS file (%s) could not be read: %s", this.vevosFilePath,e.getMessage()));
        }
    }

    private void parseVevosFileLines(List<String> vevosFileLines){
        try {
            for (String line : vevosFileLines) {
                // ignore file conditions (for now)
                // TODO: check file conditions for sure (check if end line is last line in file)
                if(line.contains(";True;True;True;1")){ continue; }
                this.vevosPresenceConditions.add(new VEVOSPresenceCondition(line, logicToModuleTransformer));
            }
        } catch(IllegalArgumentException e){
            throw new RuntimeException(String.format("VEVOS file entries could not be parsed: %s", e.getMessage()));
        }
    }

    public List<VEVOSPresenceCondition> getFileSpecificPresenceConditions(Path filePath){
        // result is ordered according to line start
        List<VEVOSPresenceCondition> specificPresenceConditions = new LinkedList<>();
        for (VEVOSPresenceCondition vevosPresenceCondition : this.vevosPresenceConditions){
            if (vevosPresenceCondition.getFilePath().compareTo(filePath) == 0){
                specificPresenceConditions.add(vevosPresenceCondition);
            }
        }
        return specificPresenceConditions;
    }
}
