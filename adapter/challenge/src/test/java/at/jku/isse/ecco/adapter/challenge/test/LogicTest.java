package at.jku.isse.ecco.adapter.challenge.test;

import org.junit.jupiter.api.Test;
import org.logicng.formulas.CTrue;
import org.logicng.formulas.Constant;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Formula;
import org.logicng.io.parsers.ParserException;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class LogicTest {

    @Test
    public void combineFormulasOfDifferentFactories(){
        FormulaFactory factory1 = new FormulaFactory();
        FormulaFactory factory2 = new FormulaFactory();
        Formula formula1 = factory1.literal("A", true);
        Formula formula2 = factory2.literal("B", false);
        assertThrows(UnsupportedOperationException.class, () -> factory2.or(formula1, formula2));
    }

    @Test
    public void uuidDoesNotParse() throws ParserException {
        FormulaFactory factory1 = new FormulaFactory();
        Formula formula1 = factory1.parse("FEATUREA_220b5953_c14f_483f_ac25_ffd051f28c9b");
    }

    @Test
    public void parseConstant() throws ParserException {
        FormulaFactory factory = new FormulaFactory();
        Formula formula1 = factory.literal("A", true);
        Formula formula2 = factory.literal("B", false);
        Formula conjunction = factory.and(formula1, formula2, factory.verum());
        System.out.println(factory.verum());
        System.out.println(factory.falsum());
    }

    @Test
    public void trueFalseTest() throws ParserException {
        FormulaFactory factory = new FormulaFactory();
        System.out.println(factory.constant(true).toString());
        System.out.println(factory.constant(false).toString());
        Formula trueFormula = factory.parse(factory.constant(true).toString());
        Formula falseFormula = factory.parse(factory.constant(false).toString());
    }
}
