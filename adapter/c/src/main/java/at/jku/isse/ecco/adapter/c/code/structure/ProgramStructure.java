package at.jku.isse.ecco.adapter.c.code.structure;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class ProgramStructure {

    private List<FunctionStructure> functionStructures;
    private String[] codeLines;

    public ProgramStructure(String[] codeLines){
        this.codeLines = codeLines;
        this.functionStructures = new LinkedList<>();
    }

    public void addFunctionStructure(int start, int end){
        String[] functionLines = Arrays.copyOfRange(this.codeLines, start - 1, end - 1);
        this.functionStructures.add(new FunctionStructure(start, end, functionLines));
    }
}
