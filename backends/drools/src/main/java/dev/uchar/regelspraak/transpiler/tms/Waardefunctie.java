package dev.uchar.regelspraak.transpiler.tms;

/** A value as a function of the values a rule binds, indexed as {@code TmsTranspiler.Lezingen} numbers them: the
 *  rendering of a planned value ({@link ClosureWeergave}). */
@FunctionalInterface
interface Waardefunctie {
    Object van(Object[] w);
}
