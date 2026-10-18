package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The COUNT's element word and the range that follows from it — "het aantal &lt;X&gt; van &lt;onderwerp&gt;",
 * the count source that names a role, whose range the emitter derives in IrEmitter.foldRange: by the
 * WRITTEN name first, then by the TYPE PAIR.
 *
 * <p>Two things meet here and this suite pins both.
 *
 * <ol>
 * <li><b>The element word may name a POPULATION.</b> Until 2026-09-16 it was looked up as the SINGULAR
 *     of an object type or as a role name, so a written plural denoting a declared type resolved to
 *     nothing: the fold went out without its element type and the uchar verifier refused the
 *     document under WF-034. The population-plural lookup (ExpressionBuilder.countElement, the
 *     {@code typeViaPlural} fallback) closed that.
 * <li><b>No role is called that, so the range comes from the type pair.</b> Since L2 of the v1 wire
 *     migration every fold over linked instances carries its range (IR-SPEC ch. 4.3), and no reader derives
 *     one. Where the written name binds no role, the emitter takes the one fact type that relates the
 *     subject to the element type, and because the source under AANTAL names the role it counts (WF-047) it
 *     writes that role's declared name in the source's {@code attribute}; the written spelling stays in
 *     its {@code source} snippet. Where the pair is not unique — two fact types, or none — there is no range to write and
 *     the resolver refuses the model with an ERROR, in the loader's sentence for a fold without its range: the
 *     frontend never guesses a relation, and a fold without one has no spelling.
 * </ol>
 *
 * <p>The accepting case is {@code corpus/golden/jutterswet-muntpeiling}, which plays the same sentence through
 * ref, clingo and Drools.
 */
class CountRangeTest {

    /** One fact type between Jutter and Munt, and its far role is NOT called "munt": so the written
     *  "Munten" can only be the object type's own declared plural. */
    static final String MODEL = """
        Objecttype de Jutter (mv: Jutters) (bezield)
            het muntaantal Numeriek (niet-negatief geheel getal);

        Objecttype de Munt (mv: Munten)

        Feittype muntvondst
            de vinder Jutter
            de gevonden munt (mv: gevonden munten) Munt
        Eén vinder vindt meerdere gevonden munten
        """;

    /** The same model with a SECOND Jutter–Munt fact type: the type pair stops being unique. */
    static final String AMBIGUOUS_MODEL = MODEL + """

        Feittype muntbewaring
            de bewaarder Jutter
            de bewaarde munt (mv: bewaarde munten) Munt
        Eén bewaarder bewaart meerdere bewaarde munten
        """;

    /** A second object type declaring the SAME plural — the population name binds nothing. */
    static final String SHARED_PLURAL_MODEL = MODEL + """

        Objecttype het Muntstuk (mv: Munten)
        """;

    /** The COLLISION: one written word that is the object type's declared plural AND a role's declared
     *  plural at the same time. Both readings reach the same type, so nothing is ambiguous about the
     *  POPULATION; the question is only whether the relation is named, and it is. */
    static final String COLLIDING_PLURAL_MODEL = """
        Objecttype de Jutter (mv: Jutters) (bezield)
            het muntaantal Numeriek (niet-negatief geheel getal);

        Objecttype de Munt (mv: Munten)

        Feittype muntvondst
            de vinder Jutter
            de munt (mv: Munten) Munt
        Eén vinder vindt meerdere Munten
        """;

    /** No fact type relates Jutter to Munt at all — the type pair has ZERO candidates, not two. */
    static final String UNCONNECTED_MODEL = """
        Objecttype de Jutter (mv: Jutters) (bezield)
            het muntaantal Numeriek (niet-negatief geheel getal);

        Objecttype de Munt (mv: Munten)

        Objecttype de Schelp (mv: Schelpen)

        Feittype schelpvondst
            de vinder Jutter
            de gevonden schelp (mv: gevonden schelpen) Schelp
        Eén vinder vindt meerdere gevonden schelpen
        """;

    static final String TYPE_PLURAL_RULE = """

        Regel muntaantal van een jutter
            geldig altijd
                Het muntaantal van een Jutter moet berekend worden als het aantal Munten van de Jutter.
        """;

    static final String ROLE_NAME_RULE = """

        Regel muntaantal van een jutter
            geldig altijd
                Het muntaantal van een Jutter moet berekend worden als het aantal gevonden munten van de Jutter.
        """;

    /** An element word that names neither a declared type nor a role. */
    static final String FOREIGN_RULE = """

        Regel muntaantal van een jutter
            geldig altijd
                Het muntaantal van een Jutter moet berekend worden als het aantal schelpen van de Jutter.
        """;

    // ---------------- the population name ----------------

    /** The arm this suite exists for: the count's element word is the object type's declared plural, and
     *  it resolves. Before the population-plural lookup this emitted a count without its element type. */
    @Test
    void a_count_on_a_type_plural_binds_the_object_type() {
        String c = ir(MODEL + TYPE_PLURAL_RULE);
        assertTrue(c.contains("\"kind\": \"count\""), () -> "de telling hoort geëmitteerd te worden:\n" + c);
        assertTrue(c.contains("\"elementType\": \"Munt\""),
                () -> "het geschreven meervoud moet het objecttype binden:\n" + c);
    }

    /** And its range comes from the TYPE PAIR: no role carries the written name, and the one fact type
     *  between Jutter and Munt is the relation counted over. The source then names that role by its
     *  declared name; the written "Munten" stays in the source snippet. */
    @Test
    void a_count_on_a_type_plural_carries_the_range_of_the_type_pair() {
        String c = ir(MODEL + TYPE_PLURAL_RULE);
        assertTrue(c.contains("\"sourceRole\""), () -> "een vouw over gekoppelde instanties draagt haar bereik:\n" + c);
        assertTrue(c.contains("\"factType\": \"muntvondst\""), () -> "het enige verbindende feittype:\n" + c);
        assertTrue(c.contains("\"role\": \"gevonden munt\""), () -> "de binding noemt de gedeclareerde rol:\n" + c);
        assertTrue(c.contains("\"attribute\": \"gevonden munt\""),
                () -> "de telbron noemt de gebonden rol bij haar gedeclareerde naam:\n" + c);
        assertTrue(c.contains("\"snippet\": \"Munten van de Jutter\""),
                () -> "de geschreven spelling blijft in de bron:\n" + c);
        assertFalse(c.contains("\"sourcePath\""),
                () -> "een eenstaps bereik is nooit een pad:\n" + c);
        assertFalse(c.contains("\"node\": \"Unresolved\""),
                () -> "de telling is opgelost:\n" + c);
    }

    /** Counter-proof on the same model: written as the role the fact type DECLARES, the same count binds
     *  and the wire carries the binding. The two spellings sit side by side in
     *  {@code corpus/golden/jutterswet-muntpeiling}, pinned to the same numbers. */
    @Test
    void a_count_on_the_written_role_name_keeps_its_source_role() {
        String c = ir(MODEL + ROLE_NAME_RULE);
        assertTrue(c.contains("\"sourceRole\""), () -> "de geschreven rolnaam hoort te binden:\n" + c);
        assertTrue(c.contains("\"factType\": \"muntvondst\""), () -> "de binding noemt haar feittype:\n" + c);
        assertTrue(c.contains("\"role\": \"gevonden munt\""), () -> "de binding noemt de gedeclareerde rol:\n" + c);
    }

    // ---------------- the ambiguous flank ----------------

    /** Two fact types between the same pair. The frontend does not guess one: there is no range to write,
     *  so the model is refused. A {@code sourceRole} here would be the silent wrong answer this arm exists to
     *  avoid, and a fold without one would be read as a fold over something else. */
    @Test
    void the_ambiguous_count_guesses_no_relation() {
        RegelSpraakModel m = Resolver.resolve(AMBIGUOUS_MODEL + TYPE_PLURAL_RULE);
        assertTrue(m.resolvedRegels.get(0).value() instanceof Expr.FunctionCall f
                        && f.elementObjectType() != null && f.elementObjectType().naam.equals("Munt"),
                () -> "het elementtype blijft gebonden; de meerduidigheid zit in de RELATIE");
        assertEquals(List.of("vouw 'het aantal Munten van de Jutter' zonder bereik (2 relaties verbinden Jutter met "
                        + "Munt, en 'Munten' noemt geen rol die er precies één aanwijst): " + WITHOUT_RANGE),
                faults(m));
    }

    // ---------------- the boundaries of the lookup ----------------

    /** The population lookup is ambiguity-safe: two declared types sharing one plural bind NEITHER, so the
     *  fold loses its element type instead of a type being picked. Since 2026-09-23 a count without an
     *  element type is refused HERE as well as on the wire (WF-037's count-source arm), so the document
     *  no longer reaches the verifier; the model still shows that no type was bound. */
    @Test
    void a_shared_type_plural_binds_none_of_both() {
        String c = irWithSourcelessError(SHARED_PLURAL_MODEL + TYPE_PLURAL_RULE);
        assertTrue(c.contains("\"kind\": \"count\""), () -> "de telling hoort geëmitteerd te worden:\n" + c);
        assertFalse(c.contains("\"elementType\""),
                () -> "een gedeeld meervoud mag geen van beide types binden:\n" + c);
    }

    /** And it invents nothing: a word that names neither a declared type nor a role leaves the fold
     *  without an element type — refused by WF-034 on the wire and, since 2026-09-23, by the
     *  count-source ERROR here, which the population lookup must not paper over either. */
    @Test
    void a_word_that_designates_nothing_yields_no_element_type() {
        String c = irWithSourcelessError(MODEL + FOREIGN_RULE);
        assertTrue(c.contains("\"kind\": \"count\""), () -> "de telling hoort geëmitteerd te worden:\n" + c);
        assertFalse(c.contains("\"elementType\""),
                () -> "een onbekend elementwoord mag geen type binden:\n" + c);
    }

    /** The COLLISION, and the order that decides it: when one written word is BOTH the object type's
     *  declared plural and a role's declared plural, the population lookup is not what answers — the
     *  role does, and the fold goes out BOUND. Both readings reach `Munt`, so no population question
     *  is open; what would be lost by binding nothing is the relation the sentence does name, and the
     *  document would then rest on a type-pair derivation it does not need. Not a new behaviour — the
     *  population lookup is a fallback and runs only where the role reading finds nothing — but the
     *  precedence had no test, so a reordering would have been invisible here. */
    @Test
    void a_word_that_is_also_a_role_plural_binds_the_relation() {
        String c = ir(COLLIDING_PLURAL_MODEL + TYPE_PLURAL_RULE);
        assertTrue(c.contains("\"elementType\": \"Munt\""),
                () -> "beide lezingen bereiken Munt, dus het elementtype staat vast:\n" + c);
        assertTrue(c.contains("\"sourceRole\""),
                () -> "een woord dat een rol NOEMT hoort die rol te binden:\n" + c);
        assertTrue(c.contains("\"role\": \"munt\""),
                () -> "de binding noemt de gedeclareerde rol:\n" + c);
    }

    /** The ZERO-candidate flank of the same derivation. Two fact types make the type pair ambiguous; NONE
     *  makes it empty, and the frontend's answer is the same in both: the element type binds, no relation
     *  is invented, and the model is refused. What this witness holds is the frontend's half — that an
     *  unconnected pair produces a resolved fold (not a marker, not a silence) and then no document. */
    @Test
    void an_unconnected_type_pair_yields_the_same_refusal() {
        RegelSpraakModel m = Resolver.resolve(UNCONNECTED_MODEL + TYPE_PLURAL_RULE);
        assertTrue(m.resolvedRegels.get(0).value() instanceof Expr.FunctionCall f
                        && f.kind() == Expr.FunctionKind.AANTAL
                        && f.elementObjectType() != null && f.elementObjectType().naam.equals("Munt"),
                () -> "het elementtype bindt ook zonder verbindend feittype, en de telling is opgelost");
        assertEquals(List.of("vouw 'het aantal Munten van de Jutter' zonder bereik (geen relatie verbindt Jutter met "
                + "Munt): " + WITHOUT_RANGE), faults(m));
    }

    /** The loader's sentence for the document the emitter would write without the range. */
    private static final String WITHOUT_RANGE = "a single-argument fold with an elementType carries its range "
            + "(sourceRole or sourcePath) under every kind but total: the range is written, never derived";

    private static List<String> faults(RegelSpraakModel m) {
        return m.errors().stream().map(RegelSpraakModel.Diagnostic::message).toList();
    }

    /** The raw emission, keys spelled as the wire spells them — whitespace is NOT stripped, because
     *  every name this suite asserts on ("gevonden munt") carries a space of its own. */
    private static String ir(String source) {
        RegelSpraakModel m = Resolver.resolve(source);
        assertTrue(m.errors().isEmpty(), () -> "onverwachte fouten: " + m.errors());
        return IrEmitter.emit(m);
    }

    /** A count that binds no element type: exactly one ERROR, the count-source refusal, and the model
     *  emitted anyway with that ERROR set aside ({@link Emission}), so the caller can read what was (not) bound. */
    private static String irWithSourcelessError(String source) {
        RegelSpraakModel m = Resolver.resolve(source);
        assertEquals(1, m.errors().size(), () -> "verwacht één fout, kreeg: " + m.errors());
        assertTrue(m.errors().get(0).message().startsWith("telling zonder bron"),
                () -> "verwacht de bronloze-tellingfout, kreeg: " + m.errors());
        return Emission.withoutErrors(m);
    }
}
