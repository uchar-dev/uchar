package dev.uchar.regelspraak.transpiler.tms;

/** A value as a function of the values a rule binds, indexed as {@code TmsTranspiler.Reads} numbers them: the
 *  rendering of a planned value ({@link ClosureRendering}). */
@FunctionalInterface
interface ValueFunction {
    Object of(Object[] w);
}
