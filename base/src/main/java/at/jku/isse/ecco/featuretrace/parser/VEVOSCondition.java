package at.jku.isse.ecco.featuretrace.parser;

import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

public class VEVOSCondition {
    private final Path filePath;
    private Formula condition;
    private final int startLine;
    private final int endLine;

    public VEVOSCondition(FormulaFactory formulaFactory, String vevosFileLine){
        // VEVOS file entry structure: Path;File Condition;Block Condition;Presence Condition;start;end
        String[] lineParts = vevosFileLine.split(";");
        if (lineParts.length < 6){
            throw new IllegalArgumentException(
                    String.format("VEVOS file entry has less than 6 comma-separated parts: %s", vevosFileLine));
        }

        this.filePath = Paths.get(lineParts[0]);
        this.condition = this.parseVEVOSCondition(lineParts[3]);
        this.startLine = Integer.parseInt(lineParts[4]);
        this.endLine = Integer.parseInt(lineParts[5]);
    }

    private Formula parseVEVOSCondition(String stringCondition){
        // replace operands (!; ||; &&)
        stringCondition = stringCondition.replace("!", "~");
        stringCondition = stringCondition.replace("||", "|");
        stringCondition = stringCondition.replace("&&", "&");


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

    public Formula getCondition(){
        return this.condition;
    }
}
