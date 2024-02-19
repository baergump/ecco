package at.jku.isse.ecco.adapter.challenge.vevos;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class VEVOSConditionHandler {

    private final String PRESENCE_CONDITION_FILENAME = "pc.variant.csv";
    private Set<VEVOSPresenceCondition> vevosPresenceConditions;

    public VEVOSConditionHandler(Path vevosFileBasePath, Path relativeArtifactFilePath){
        this.vevosPresenceConditions = new HashSet<>();
        // TODO: add respective presence-conditions
    }


}
