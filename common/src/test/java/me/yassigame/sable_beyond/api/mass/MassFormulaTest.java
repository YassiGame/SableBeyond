package me.yassigame.sable_beyond.api.mass;

import me.yassigame.sable_beyond.common.FormulaManager;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MassFormulaTest {
    @Test
    void evaluatesNumbersAndOperatorPrecedence() {
        assertFormulaEquals(14.0, "2 + 3 * 4");
        assertFormulaEquals(20.0, "(2 + 3) * 4");
        assertFormulaEquals(5.0, "17 % 6");
        assertFormulaEquals(512.0, "2 ^ 3 ^ 2");
    }

    @Test
    void evaluatesUnaryOperatorsAndWhitespace() {
        assertFormulaEquals(-3.0, " -5 + +2 ");
        assertFormulaEquals(8.0, "-(2 - 10)");
        assertFormulaEquals(4.0, "--4");
    }

    @Test
    void evaluatesVariables() {
        final FormulaManager formula = FormulaManager.compile("base_mass + volume * volume_multiplier");

        assertEquals(12.5, formula.evaluate(Map.of(
                "base_mass", 0.5,
                "volume", 3.0,
                "volume_multiplier", 4.0
        )));
    }

    @Test
    void evaluatesSupportedFunctions() {
        final Map<String, Double> variables = Map.of(
                "auto_mass", 22.0,
                "block_mass", 4.0,
                "count", 8.0
        );

        assertEquals(15.0, FormulaManager.compile("min(auto_mass, 15)").evaluate(variables));
        assertEquals(32.0, FormulaManager.compile("block_mass * count").evaluate(variables));
        assertEquals(10.0, FormulaManager.compile("clamp(auto_mass, 0, 10)").evaluate(variables));
        assertEquals(7.0, FormulaManager.compile("abs(-7)").evaluate(variables));
        assertEquals(3.0, FormulaManager.compile("sqrt(9)").evaluate(variables));
        assertEquals(8.0, FormulaManager.compile("pow(2, 3)").evaluate(variables));
        assertEquals(2.0, FormulaManager.compile("floor(2.9)").evaluate(variables));
        assertEquals(3.0, FormulaManager.compile("ceil(2.1)").evaluate(variables));
        assertEquals(4.0, FormulaManager.compile("round(3.5)").evaluate(variables));
    }

    @Test
    void rejectsBlankFormula() {
        assertThrows(IllegalArgumentException.class, () -> FormulaManager.compile(""));
        assertThrows(IllegalArgumentException.class, () -> FormulaManager.compile("   "));
        assertThrows(IllegalArgumentException.class, () -> FormulaManager.compile(null));
    }

    @Test
    void rejectsMalformedFormulaAtCompileTime() {
        assertThrows(IllegalArgumentException.class, () -> FormulaManager.compile("(2 + 3"));
        assertThrows(IllegalArgumentException.class, () -> FormulaManager.compile("2 + * 3"));
        assertThrows(IllegalArgumentException.class, () -> FormulaManager.compile("min(2, 3"));
    }

    @Test
    void rejectsUnknownVariablesAndFunctionsAtEvaluationTime() {
        assertThrows(IllegalArgumentException.class, () -> FormulaManager.compile("missing_variable + 1").evaluate(Map.of()));
        assertThrows(IllegalArgumentException.class, () -> FormulaManager.compile("unknown(1)").evaluate(Map.of()));
    }

    @Test
    void rejectsWrongFunctionArgumentCountAtEvaluationTime() {
        assertThrows(IllegalArgumentException.class, () -> FormulaManager.compile("min(1)").evaluate(Map.of()));
        assertThrows(IllegalArgumentException.class, () -> FormulaManager.compile("clamp(1, 2)").evaluate(Map.of()));
    }

    private static void assertFormulaEquals(final double expected, final String expression) {
        assertEquals(expected, FormulaManager.compile(expression).evaluate(Map.of()));
    }
}
