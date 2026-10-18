package dev.uchar.regelspraak.transpiler.core;

import dev.uchar.regelspraak.ir.Condition;

/**
 * THE comparison-operator table both back-ends share (single shared implementation): the mvel/rsAggVergelijk symbol,
 * the §13.4.8 logical negation ({@link #flip}) and the operand-swap reversal ({@link #reverse}).
 * The two transformations are deliberately side by side because confusing them is THE classic bug:
 * negation complements the boundary (GROTER → KLEINER_OF_GELIJK), reversal mirrors it (GROTER →
 * KLEINER) — §8.2 putting a right-written temporal operand on the left needs omkeren; flip there
 * would move the OF_GELIJK boundary.
 */
public final class Operators {
    private Operators() {}

    /** The mvel / {@code rsAggVergelijk} comparison symbol. */
    public static String symbol(Condition.Operator op) {
        return switch (op) {
            case KLEINER, EERDER -> "<";
            case KLEINER_OF_GELIJK, EERDER_OF_GELIJK -> "<=";
            case GROTER, LATER -> ">";
            case GROTER_OF_GELIJK, LATER_OF_GELIJK -> ">=";
            case GELIJK -> "==";
            case ONGELIJK -> "!=";
        };
    }

    /** §13.4.8 logical NEGATION — the complement that detects a violation ("moet kleiner zijn dan" is
     *  violated when {@code >=}). */
    public static Condition.Operator flip(Condition.Operator op) {
        return switch (op) {
            case GELIJK -> Condition.Operator.ONGELIJK;
            case ONGELIJK -> Condition.Operator.GELIJK;
            case KLEINER -> Condition.Operator.GROTER_OF_GELIJK;
            case KLEINER_OF_GELIJK -> Condition.Operator.GROTER;
            case GROTER -> Condition.Operator.KLEINER_OF_GELIJK;
            case GROTER_OF_GELIJK -> Condition.Operator.KLEINER;
            case EERDER -> Condition.Operator.LATER_OF_GELIJK;
            case EERDER_OF_GELIJK -> Condition.Operator.LATER;
            case LATER -> Condition.Operator.EERDER_OF_GELIJK;
            case LATER_OF_GELIJK -> Condition.Operator.EERDER;
        };
    }

    /** Operand REVERSAL (swap LHS/RHS): "a = b" reverses to "b = a" (GELIJK stays GELIJK), "a &gt; b" to
     *  "b &lt; a". NOT a negation — see the class doc. */
    public static Condition.Operator reverse(Condition.Operator op) {
        return switch (op) {
            case GELIJK -> Condition.Operator.GELIJK;
            case ONGELIJK -> Condition.Operator.ONGELIJK;
            case KLEINER -> Condition.Operator.GROTER;
            case KLEINER_OF_GELIJK -> Condition.Operator.GROTER_OF_GELIJK;
            case GROTER -> Condition.Operator.KLEINER;
            case GROTER_OF_GELIJK -> Condition.Operator.KLEINER_OF_GELIJK;
            case EERDER -> Condition.Operator.LATER;
            case EERDER_OF_GELIJK -> Condition.Operator.LATER_OF_GELIJK;
            case LATER -> Condition.Operator.EERDER;
            case LATER_OF_GELIJK -> Condition.Operator.EERDER_OF_GELIJK;
        };
    }
}
