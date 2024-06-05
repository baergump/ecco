package at.jku.isse.ecco.adapter.c.code.structure;

public class FunctionStructure {

    private int startLine;
    private int endLine;
    private String[] codeLines;

    public FunctionStructure(int startLine, int endLine, String[] codeLines){
        this.startLine = startLine;
        this.endLine = endLine;
        this.codeLines = codeLines;
    }

    public int getStartLine(){
        return this.startLine;
    }

    public int getEndLine(){
        return this.endLine;
    }

    public String[] getCodeLines(){
        return this.codeLines;
    }
}
