package dev.uchar.regelspraak.ir;

import java.util.List;

/**
 * A resolved assignment rule (§13.4.4 gelijkstelling / §13.4.9 initialisatie): a
 * target reference that receives the value of a resolved {@link Expr}. This is the
 * unit the Drools transpiler turns into a rule consequence ("then" side).
 *
 * Only the assignment-shaped resultaatdelen live in THIS record; the other resultaatdeel
 * kinds have since gained nodes of their own — kenmerktoekenning as the {@link Kind#KENMERKTOEKENNING}
 * arm here, objectcreatie as {@link ResolvedObjectCreation}, verdeling as {@link ResolvedAllocation},
 * and likewise feitcreatie, startpunt, dagsoort, consistentie and uniciteit. The voorwaardendeel
 * joined the IR as the {@code condition} component below.
 *
 * @param name           the regel name (with a per-version suffix when the rule has more
 *                       than one §13.4.2.3 version, so each version is a distinct DRL rule)
 * @param kind              which assignment form produced it
 * @param target           the assignment target — typically an {@link Expr.AttributeReference}
 * @param value         the resolved value expression (the rule's "then")
 * @param condition     the resolved condition guarding the rule, or null if unconditional
 *                       (the rule's "when")
 * @param validity     the §13.4.2.5 version validity window; {@link Validity#ALTIJD} when
 *                       always-valid (the transpiler then emits no Rekendatum guard)
 * @param statuteAnnotations the {@code // Bron: …} statuutannotaties written directly above the rule
 *                       (the text after {@code Bron:}, in source order), so uitleg can cite the
 *                       wetsartikel. On the wire they are the rule form's statuteAnnotations (WF-082).
 */
public record ResolvedRule(String name, Kind kind, Expr target, Expr value,
                            Condition condition, Validity validity, List<String> statuteAnnotations) {
    public enum Kind { GELIJKSTELLING, BEREKENING, INITIALISATIE, KENMERKTOEKENNING }

    /** Without statuutannotaties (an empty list) — for the construction sites that carry no
     *  {@code // Bron:} context (or none was written above the rule). */
    public ResolvedRule(String name, Kind kind, Expr target, Expr value,
                         Condition condition, Validity validity) {
        this(name, kind, target, value, condition, validity, List.of());
    }
}
