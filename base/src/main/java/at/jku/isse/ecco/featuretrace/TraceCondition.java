package at.jku.isse.ecco.featuretrace;

import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.io.parsers.ParserException;

import java.util.Objects;

public class TraceCondition {

    private String diffCondition;
    private String userCondition;

    public TraceCondition(){}

    public String getDiffCondition(){
        return this.diffCondition;
    }

    public String getUserCondition(){
        return this.userCondition;
    }

    public void setDiffCondition(String diffCondition){ this.diffCondition = diffCondition; }
    public void setUserCondition(String userCondition){ this.userCondition = userCondition; }

    @Override
    public boolean equals(Object o){
        if (this == o) return true;
        if (!(o instanceof TraceCondition)) return false;
        if (!(this.diffCondition.equals(((TraceCondition) o).diffCondition))) return false;
        if (!(this.userCondition.equals(((TraceCondition) o).userCondition))) return false;
        return true;
    }

    @Override
    public int hashCode(){
        return Objects.hash(this.diffCondition, this.userCondition);
    }
}
