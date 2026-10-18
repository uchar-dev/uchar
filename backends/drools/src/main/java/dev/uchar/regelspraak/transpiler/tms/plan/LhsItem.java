package dev.uchar.regelspraak.transpiler.tms.plan;

/**
 * One item of a rule's left-hand side as the analysis plans it, in a closed vocabulary that a rendering turns into
 * Drools' executable model or into DRL text without deciding anything itself: the subject pattern, a pattern over a fact
 * class, a negated or existential pattern, an accumulate, and the test of the rule's condition.
 */
public sealed interface LhsItem permits Subject, FactPattern, Not, Exists, Accumulation, TestItem {}
