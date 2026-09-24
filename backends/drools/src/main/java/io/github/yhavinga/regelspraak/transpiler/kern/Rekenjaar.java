package io.github.yhavinga.regelspraak.transpiler.kern;

import io.github.yhavinga.regelspraak.ir.Expr;

/**
 * The execution year, as this backend refuses it. {@code spec/IR-SPEC.md} ch. 4.1 keeps the context value
 * {@code evaluationYear} in WF-098's closed set as ADMITTED-AND-UNEXECUTED: no document in any zone carries it, and
 * the reference interpreter faults on it by name when it evaluates the node. The records keep its v0 name,
 * {@code Rekenjaar}, which the loader reads the wire name onto.
 *
 * <p>This backend read it as the year of the evaluation date until the L4 landing's fix round. Measured on
 * verifier-clean documents: as a rule value, an arithmetic operand or a comparison operand both branches PLAYED where
 * {@code ref} faulted and the ASP backend declined, and as a consistency assertion, a creation guard or
 * initialisation, an allocation guard or a fold filter at least one branch lowered and fired it. An executor that
 * answers where the others refuse answers with a value nothing checks.
 *
 * <p>The refusal is made in two places, both in the reference interpreter's own sentence. The replay refuses a
 * document that carries the node before either branch lowers anything ({@code CaseRunner}): one check, every
 * position the node can hold, one line in the gate report for both branches. And each transpiler refuses the node
 * where it would lower it, so neither branch implements it when it is driven without the replay. The second alone
 * does not suffice: the exec-model branch reads a consistency assertion through operand readers that never reach
 * this node, and there it could only report its generic scope note.
 */
public final class Rekenjaar {
    private Rekenjaar() {}

    /** The reference interpreter's refusal of the node, verbatim. */
    public static final String WEIGERING = "the evaluationYear context value is not exercised by any corpus case"
            + " (ch. 8.2)";

    /** Whether {@code e} is the evaluation year: the record's name {@code Rekenjaar}, which the loader gives the one
     *  wire name WF-098 admits for it. */
    public static boolean is(Expr e) {
        return e instanceof Expr.Constante c && "Rekenjaar".equals(c.naam());
    }
}
