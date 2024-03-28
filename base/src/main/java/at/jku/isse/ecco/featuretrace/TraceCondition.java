package at.jku.isse.ecco.featuretrace;

import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.io.parsers.ParserException;

import java.util.Objects;

public class TraceCondition {

    private Formula diffCondition;
    private Formula userCondition;

    public TraceCondition(Formula diffCondition, Formula userCondition){
        this.diffCondition = diffCondition;
        this.userCondition = userCondition;
    }

    public FormulaFactory factory(){
        FormulaFactory diffFactory = this.diffCondition.factory();
        FormulaFactory userFactory = this.userCondition.factory();
        if (diffFactory != null && userFactory != null){
            assert(diffFactory == userFactory);
            return diffFactory;
        } else if (diffFactory != null){
            return diffFactory;
        } else if (userFactory != null){
            return userFactory;
        } else {
            return null;
        }
    }

    public Formula getDiffCondition(){
        return this.diffCondition;
    }

    public Formula getUserCondition(){
        return this.userCondition;
    }

    public void setDiffCondition(Formula diffCondition){ this.diffCondition = diffCondition; }
    public void setUserCondition(Formula userCondition){ this.userCondition = userCondition; }


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
