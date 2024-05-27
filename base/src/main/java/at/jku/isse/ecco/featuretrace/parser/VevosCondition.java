package at.jku.isse.ecco.featuretrace.parser;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

public class VevosCondition {
    private final Path filePath;
    private final String conditionString;
    private final int startLine;
    private final int endLine;

    public VevosCondition(String vevosFileLine){
        // VEVOS file entry structure: Path;File Condition;Block Condition;Presence Condition;start;end
        String[] lineParts = vevosFileLine.split(";");
        if (lineParts.length < 6){
            throw new IllegalArgumentException(
                    String.format("VEVOS file entry has less than 6 comma-separated parts: %s", vevosFileLine));
        }

        this.filePath = Paths.get(lineParts[0]);
        this.conditionString = this.prepareConditionString(lineParts[3]);
        // TODO: make multiple formats possible via interface
        //this.startLine = Integer.parseInt(lineParts[4]);
        //this.endLine = Integer.parseInt(lineParts[5]);
        this.startLine = Integer.parseInt(lineParts[5]);
        this.endLine = Integer.parseInt(lineParts[6]);
    }

    private String prepareConditionString(String stringCondition){
        // replace operands (!; ||; &&)
        stringCondition = stringCondition.replace("!", "~");
        stringCondition = stringCondition.replace("||", "|");
        stringCondition = stringCondition.replace("&&", "&");
        return stringCondition;
    }

    /*
    public static VevosCondition getMostPreciseCondition(Collection<VevosCondition> conditions){
        if (conditions.size() == 1){
            return conditions.iterator().next();
        } else {
            return conditions.stream().reduce(VevosCondition::getMostPreciseCondition).get();
        }
    }

    public static VevosCondition getMostPreciseCondition(VevosCondition condition1, VevosCondition condition2){
        if (condition1.startLine > condition2.startLine){ return condition1; }
        if (condition2.startLine > condition2.startLine){ return condition2; }
        if (condition1.endLine < condition2.endLine){ return condition1; }
        if (condition2.endLine < condition2.endLine){ return condition2; }
        throw new RuntimeException("both condition refer to the same lines, which should not be the case");
    }
     */

    public Path getFilePath(){
        return this.filePath;
    }

    public int getStartLine(){
        return this.startLine;
    }

    public int getEndLine(){
        return this.endLine;
    }

    public String getConditionString(){
        return this.conditionString;
    }
}
