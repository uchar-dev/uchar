package dev.uchar.regelspraak.transpiler.tms;

/**
 * The stated mark that {@link TmsTranspiler#setDerivedValues} put what stands in a session onto its beans. From then
 * on a bean holds derived values beside its input, and the network did not see them arrive: a further fire would read a
 * derived value as input, and an initialisation of that member would not fire again. So the driver refuses a session
 * that holds this mark ({@code DroolsBackend.fire}); a consumer that fires more than once reads between the fires
 * without writing ({@code DroolsBackend.read}). No rule reads it.
 */
public record WrittenBack() {}
