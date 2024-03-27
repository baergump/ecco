package at.jku.isse.ecco.featuretrace.evaluation;

import at.jku.isse.ecco.feature.Configuration;
import at.jku.isse.ecco.featuretrace.TraceCondition;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;

public interface EvaluationStrategy {

    boolean holds (Configuration configuration, TraceCondition traceCondition);

}
