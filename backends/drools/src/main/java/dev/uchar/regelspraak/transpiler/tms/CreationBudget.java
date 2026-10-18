package dev.uchar.regelspraak.transpiler.tms;

/**
 * The fault of a mint past the creation budget (RUNTIME-SEMANTICS ch. 5.5): an object creation would make an object at
 * creation depth {@link TmsTranspiler#CREATION_BUDGET} + 1 in its stratum. The creation rule throws it at the making,
 * before it inserts anything, so the object is not made; the driver ({@code DroolsBackend.vuur}) ends the run with its
 * sentence, as a run without a fixpoint. The sentence starts with the fragment the reference interpreter and the ASP
 * backend fault with, "creation budget exhausted".
 */
public final class CreationBudget extends RuntimeException {

    CreationBudget(String sentence) {
        super(sentence);
    }
}
