package at.jku.isse.ecco.storage.mem.featuretrace;

import at.jku.isse.ecco.feature.Configuration;
import at.jku.isse.ecco.featuretrace.FeatureTrace;
import at.jku.isse.ecco.featuretrace.evaluation.EvaluationStrategy;
import at.jku.isse.ecco.tree.Node;
import at.jku.isse.ecco.util.Trees;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.io.parsers.ParserException;

import java.util.Objects;

public class MemFeatureTrace implements FeatureTrace {

    private final Node node;
    private String userCondition;
    private String diffCondition;
    private final transient FormulaFactory formulaFactory;


    public MemFeatureTrace(Node node){
        this.node = node;
        this.formulaFactory = new FormulaFactory();
    }

    @Override
    public boolean holds(Configuration configuration, EvaluationStrategy evaluationStrategy){
        return evaluationStrategy.holds(configuration, this.userCondition, this.diffCondition);
    }

    @Override
    public Node getNode() {
        return this.node;
    }

    @Override
    public boolean containsUserCondition() {
        return (this.userCondition != null);
    }

    @Override
    public void addUserCondition(String userCondition){
        // TODO: validate input
        if (userCondition == null) { return; }
        userCondition = this.sanitizeFormulaString(userCondition);
        if (this.userCondition == null){
            this.userCondition = userCondition;
        } else {
            Formula currentCondition = this.parseString(this.userCondition);
            Formula additionalCondition = this.parseString(userCondition);
            Formula newCondition = this.formulaFactory.or(currentCondition, additionalCondition);
            this.userCondition = newCondition.toString();
        }
    }

    @Override
    public void buildUserConditionConjunction(String userCondition) {
        // TODO: validate input
        if (userCondition == null) { return; }
        userCondition = this.sanitizeFormulaString(userCondition);
        if (this.userCondition == null){
            this.userCondition = userCondition;
        } else {
            Formula currentCondition = this.parseString(this.userCondition);
            Formula additionalCondition = this.parseString(userCondition);
            Formula newCondition = this.formulaFactory.and(currentCondition, additionalCondition);
            this.userCondition = newCondition.toString();
        }
    }

    @Override
    public String getUserConditionString() {
        return this.userCondition;
    }

    @Override
    public String getDiffConditionString() {
        return this.diffCondition;
    }

    @Override
    public void fuseFeatureTrace(FeatureTrace featureTrace) {
        if (!(featureTrace instanceof MemFeatureTrace)){
            throw new RuntimeException("Cannot fuse MemFeatureTrace with non-MemFeatureTrace.");
        }
        MemFeatureTrace memFeatureTrace = (MemFeatureTrace) featureTrace;
        if (this.diffCondition != null && memFeatureTrace.diffCondition != null){
            throw new RuntimeException("There exist multiple diff-based conditions for the same artifact.");
        }
        this.diffCondition = memFeatureTrace.diffCondition;
        this.addUserCondition(memFeatureTrace.userCondition);
    }

    @Override
    public String getOverallConditionString(EvaluationStrategy evaluationStrategy) {
        if (this.diffCondition == null && this.userCondition == null){
            throw new RuntimeException("Neither diff-based nor user-based condition exists.");
        } else  {
            return evaluationStrategy.getOverallConditionString(this.userCondition, this.diffCondition);
        }
    }

    private Formula parseString(String string){
        try{
            return this.formulaFactory.parse(string);
        } catch (ParserException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean hasEqualConditions(FeatureTrace featureTrace){
        if (this == featureTrace) return true;
        if (!(featureTrace instanceof MemFeatureTrace)) return false;
        if (!(this.userCondition.equals(((MemFeatureTrace) featureTrace).userCondition))) return false;
        if (!(this.diffCondition.equals(((MemFeatureTrace) featureTrace).diffCondition))) return false;
        return true;
    }

    @Override
    public void setDiffCondition(String diffConditionString) {
        this.diffCondition = diffConditionString;
    }

    @Override
    public void setUserCondition(String userConditionString) {
        userConditionString = this.sanitizeFormulaString(userConditionString);
        this.userCondition = userConditionString;
    }

    private String sanitizeFormulaString(String formulaString){
        // "." and "-" cannot be parsed by FormulaFactory
        // conditions replace "." with "_" for indication of feature-revision-id
        // as opposed to documentation, "#" and "@" do not parse, which is why only "_" is used
        // conditions replace "-" with "_" for UUIDs (Feature-revision-IDs)
        if (formulaString == null) { return null; }
        formulaString = formulaString.replace(".", "_");
        formulaString = formulaString.replace("-", "_");
        return formulaString;
    }

    @Override
    public boolean equals(Object o){
        if (this == o) return true;
        if (!(o instanceof MemFeatureTrace)) return false;
        MemFeatureTrace memFeatureTrace = (MemFeatureTrace) o;

        if (this.node == null){
            if (memFeatureTrace.node != null) { return false; }
        // the whole tree must be the same in the current implementation
        } else if (!(Trees.equals(this.node.getRoot(), memFeatureTrace.node.getRoot()))) { return false; }

        if (this.userCondition == null){
            if (memFeatureTrace.userCondition != null) { return false; }
        } else if (!(this.userCondition.equals(memFeatureTrace.userCondition))) { return false; }

        if (this.diffCondition == null){
            if (memFeatureTrace.diffCondition != null) { return false; }
        } else if (!(this.diffCondition.equals(memFeatureTrace.diffCondition))) { return false; }

        return true;
    }

    @Override
    public int hashCode(){
        return Objects.hash(this.node, this.userCondition, this.diffCondition);
    }
}
