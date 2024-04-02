package at.jku.isse.ecco.service.utils;

import at.jku.isse.ecco.feature.Configuration;
import at.jku.isse.ecco.tree.Node;
import at.jku.isse.ecco.util.Location;

/**
 * Insert configuration-strings into the location of all tree-nodes with a location.
 */
public class ConfigInsertionVisitor implements Node.Op.NodeVisitor{

    String configurationString;

    public ConfigInsertionVisitor(Configuration configuration){
        this.configurationString = configuration.getOriginalConfigString();
    }

    @Override
    public void visit(Node.Op node) {
        Location location = node.getLocation();
        if (location == null) { return; }
        if (this.configurationString == null){
            throw new RuntimeException("There is no original configuration string stored!");
        }
        location.setConfigurationString(this.configurationString);
    }
}
