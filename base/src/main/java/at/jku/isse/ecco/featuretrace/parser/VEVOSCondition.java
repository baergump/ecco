package at.jku.isse.ecco.featuretrace.parser;

import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.io.parsers.ParserException;

import java.nio.file.Path;
import java.nio.file.Paths;

public class VEVOSCondition {
    private final Path filePath;
    private final String conditionString;
    private final int startLine;
    private final int endLine;

    public VEVOSCondition(String vevosFileLine){
        // VEVOS file entry structure: Path;File Condition;Block Condition;Presence Condition;start;end
        String[] lineParts = vevosFileLine.split(";");
        if (lineParts.length < 6){
            throw new IllegalArgumentException(
                    String.format("VEVOS file entry has less than 6 comma-separated parts: %s", vevosFileLine));
        }

        this.filePath = Paths.get(lineParts[0]);
        this.conditionString = this.prepareConditionString(lineParts[3]);
        this.startLine = Integer.parseInt(lineParts[4]);
        this.endLine = Integer.parseInt(lineParts[5]);
    }

    private String prepareConditionString(String stringCondition){
        // replace operands (!; ||; &&)
        stringCondition = stringCondition.replace("!", "~");
        stringCondition = stringCondition.replace("||", "|");
        stringCondition = stringCondition.replace("&&", "&");
        return stringCondition;
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

    public String getConditionString(){
        return this.conditionString;
    }
}
