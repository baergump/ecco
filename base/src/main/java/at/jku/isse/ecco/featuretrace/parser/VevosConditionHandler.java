package at.jku.isse.ecco.featuretrace.parser;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.*;

public class VevosConditionHandler {

    private final String PRESENCE_CONDITION_FILENAME = "pcs.variant.csv";
    private final Path vevosFilePath;
    private boolean vevosFileExists;
    private List<VevosCondition> vevosConditions;

    public VevosConditionHandler(Path vevosFileBasePath){
        this.vevosFilePath = vevosFileBasePath.resolve(this.PRESENCE_CONDITION_FILENAME);
        this.vevosFileExists = Files.exists(vevosFilePath);
        this.vevosConditions = new LinkedList<>();
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
                this.vevosConditions.add(new VevosCondition(line));
            }
        } catch(IllegalArgumentException e){
            throw new RuntimeException(String.format("VEVOS file entries could not be parsed: %s", e.getMessage()));
        }
    }

    public VevosFileConditionContainer getFileSpecificPresenceConditions(Path filePath){
        List<VevosCondition> specificPresenceConditions = new LinkedList<>();
        for (VevosCondition vevosPresenceCondition : this.vevosConditions){
            if (vevosPresenceCondition.getFilePath().compareTo(filePath) == 0){
                specificPresenceConditions.add(vevosPresenceCondition);
            }
        }
        return new VevosFileConditionContainer(specificPresenceConditions);
    }
}
