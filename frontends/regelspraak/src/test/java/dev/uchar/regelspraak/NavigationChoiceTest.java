package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Which relation a navigated read binds, and what happens when none does (grammar/DIVERGENCES.md, "§13.4.1 A
 * navigated read binds its written role"). The written role name decides when it names exactly one relation
 * between the two types, however many relations join them, and a noun that is the other type's name and also a
 * role name counts as a written role; a type name alone binds only where exactly one relation joins the types.
 * Every other read is refused by the resolver with an ERROR, in the verifier's sentence for the document the
 * emitter would write without the path, so no emission route reaches the emitter's own refusal; a read whose binding
 * reaches a to-many role, and a fold the wire cannot spell, are refused the same way. The witnesses: two relations
 * between Schuit and Haven with different role names, one relation, none, and two relations of which one role
 * carries the type's own name.
 */
class NavigationChoiceTest {

    private static final String TYPES = """
        Objecttype de Haven (mv: Havens)
            het kadegeld Numeriek (niet-negatief geheel getal);
            de rompsom Numeriek (niet-negatief geheel getal);
            is veilig kenmerk (bijvoeglijk);

        Objecttype de Schuit (mv: Schuiten)
            de ligprijs Numeriek (niet-negatief geheel getal);
            de romplengte (mv: romplengten) Numeriek (niet-negatief geheel getal);
            is gekeurd kenmerk (bijvoeglijk);

        Objecttype de Vuurtoren (mv: Vuurtorens)
            het peil Numeriek (niet-negatief geheel getal);
        """;

    private static final String THUISLIGGING = """

        Feittype thuisligging
            de thuishaven Haven
            de ligger (mv: liggers) Schuit
        Eén thuishaven telt meerdere liggers
        """;

    private static final String WINTERBERGING = """

        Feittype winterberging
            de winterhaven Haven
            de winterschuit (mv: winterschuiten) Schuit
        Eén winterhaven telt meerdere winterschuiten
        """;

    /** The first relation with its Haven role named as the type is. */
    private static final String HAVENLIGGING = """

        Feittype havenligging
            de haven Haven
            de ligger (mv: liggers) Schuit
        Eén haven telt meerdere liggers
        """;

    private static final String WITHOUT_PATH = "an attribute reference without a path is the subject's own read, so its "
            + "subjectType is the subject type 'Schuit' — 'Haven' names another type, which only a navigation reaches";

    private static String rule(String value) {
        return """

            Regel ligprijs van een schuit
                geldig altijd
                    De ligprijs van een Schuit moet gesteld worden op %s.
            """.formatted(value);
    }

    private static String path(String factType, String role) {
        return "\"path\":[{\"node\":\"RoleBinding\",\"factType\":\"" + factType + "\",\"role\":\"" + role + "\"}]";
    }

    private static String emitCompact(String source) {
        RegelSpraakModel m = Resolver.resolve(source);
        assertTrue(m.errors().isEmpty(), () -> "onverwachte fouten: " + m.errors());
        return IrEmitter.emit(m).replaceAll("\\s+", "");
    }

    private static List<String> faults(String source) {
        return Resolver.resolve(source).errors().stream().map(RegelSpraakModel.Diagnostic::message).toList();
    }

    // ---------------- two relations between Schuit and Haven ----------------

    @Test void two_relations_the_written_role_name_binds() {
        String c = emitCompact(TYPES + THUISLIGGING + WINTERBERGING + rule("het kadegeld van zijn winterhaven"));
        assertTrue(c.contains(path("winterberging", "winterhaven")), () -> "de rolnaam wijst één relatie aan:\n" + c);
        String d = emitCompact(TYPES + THUISLIGGING + WINTERBERGING + rule("het kadegeld van de thuishaven van de Schuit"));
        assertTrue(d.contains(path("thuisligging", "thuishaven")), () -> "ook als lezing met lidwoord:\n" + d);
    }

    @Test void two_relations_the_type_name_is_refused() {
        assertEquals(List.of("genavigeerde lezing 'het kadegeld van de Haven' zonder binding (2 relaties verbinden Schuit "
                        + "met Haven, en 'de Haven' noemt geen rol die er precies één aanwijst): " + WITHOUT_PATH),
                faults(TYPES + THUISLIGGING + WINTERBERGING + rule("het kadegeld van de Haven")));
    }

    @Test void two_relations_a_role_name_that_is_the_type_name_binds() {
        // "de haven" names the type and the role of havenligging; the pair decides nothing, the role does
        for (String written : List.of("het kadegeld van de haven", "het kadegeld van de Haven")) {
            String c = emitCompact(TYPES + HAVENLIGGING + WINTERBERGING + rule(written));
            assertTrue(c.contains(path("havenligging", "haven")), () -> "'" + written + "' bindt de rol:\n" + c);
        }
    }

    // ---------------- one relation, and none ----------------

    @Test void a_relation_role_name_and_type_name_both_bind() {
        for (String written : List.of("het kadegeld van zijn thuishaven", "het kadegeld van de Haven")) {
            String c = emitCompact(TYPES + THUISLIGGING + rule(written));
            assertTrue(c.contains(path("thuisligging", "thuishaven")), () -> "'" + written + "' bindt:\n" + c);
        }
    }

    @Test void no_relation_is_refused() {
        assertEquals(List.of("genavigeerde lezing 'het kadegeld van de Haven' zonder binding (geen relatie verbindt "
                        + "Schuit met Haven): " + WITHOUT_PATH),
                faults(TYPES + rule("het kadegeld van de Haven")));
    }

    @Test void a_written_role_that_points_to_two_relations_is_refused_in_the_words_of_the_frontend() {
        // the read names the subject's own type, so the document without its path is the subject's own read,
        // which no reader refuses: the refusal has no reader sentence to take
        assertEquals(List.of("genavigeerde lezing 'de romplengte van zijn sleper' zonder binding (de geschreven rol "
                        + "'sleper' wijst vanaf Schuit niet precies één relatie aan): zonder pad zou de wire haar lezen "
                        + "als een eigen attribuut van het onderwerp, en dat is niet wat er staat"),
                faults(TYPES + """

                    Feittype sleepverband
                        de sleper Schuit
                        de gesleepte Schuit
                    Eén sleper sleept één gesleepte

                    Feittype bergverband
                        de sleper Schuit
                        de geborgene Schuit
                    Eén sleper bergt één geborgene
                    """ + rule("de romplengte van zijn sleper")));
    }

    // ---------------- a binding that reaches a to-many role ----------------

    @Test void a_read_over_a_to_many_role_is_refused_in_the_sentence_of_the_verifier() {
        assertEquals(List.of("genavigeerde lezing 'de romplengte van de ligger' over een meervoudige rol: hop 0 reaches "
                        + "the to-many role 'ligger' in a scalar slot — a to-many step needs an aggregation, never a "
                        + "silent first pick"),
                faults(TYPES + THUISLIGGING + """

                    Regel kadegeld van een haven
                        geldig altijd
                            Het kadegeld van een Haven moet gesteld worden op de romplengte van de ligger.
                    """));
    }

    @Test void the_condition_of_a_fact_creation_on_the_target_is_refused() {
        // the condition is read from the anchor, so "de schuit" — the role named as its type, which the pair of two
        // relations no longer hides — reaches the to-many role; before the choice this ended in the emitter's exception
        assertEquals(List.of("genavigeerde lezing 'de romplengte van de schuit' over een meervoudige rol: hop 0 reaches "
                        + "the to-many role 'schuit' in a scalar slot — a to-many step needs an aggregation, never a "
                        + "silent first pick"),
                faults(TYPES + WINTERBERGING + """

                    Feittype schuitligging
                        de thuishaven Haven
                        de schuit (mv: schuiten) Schuit
                    Eén thuishaven telt meerdere schuiten

                    Regel winterberging door de haven
                        geldig altijd
                            Een winterschuit van een Haven is een schuit van de Haven
                            indien de romplengte van de schuit groter is dan 18.
                    """));
    }

    // ---------------- the fold's range ----------------

    @Test void a_fold_over_two_relations_binds_by_the_role_name_and_is_refused_by_the_type_name() {
        String fold = """

            Regel rompsom van een haven
                geldig altijd
                    De rompsom van een Haven moet berekend worden als de som van de romplengten van alle %s van de Haven.
            """;
        String c = emitCompact(TYPES + THUISLIGGING + WINTERBERGING + fold.formatted("winterschuiten"));
        assertTrue(c.contains("\"sourceRole\":{\"node\":\"RoleBinding\",\"factType\":\"winterberging\","
                + "\"role\":\"winterschuit\"}"), () -> "de rolnaam draagt het bereik:\n" + c);
        assertEquals(List.of("vouw 'de som van de romplengten van alle Schuiten van de Haven' zonder bereik (2 relaties "
                        + "verbinden Haven met Schuit, en 'alle Schuiten van de Haven' noemt geen rol die er precies één "
                        + "aanwijst): a single-argument fold with an elementType carries its range (sourceRole or "
                        + "sourcePath) under every kind but total: the range is written, never derived"),
                faults(TYPES + THUISLIGGING + WINTERBERGING + fold.formatted("Schuiten")));
    }

    /** A fold over the element type's plural binds by the type pair where one relation joins the two types, as a
     *  navigated read by type name does. Before: a marker with the WARNING "vouwbron bindt niet op de geschreven
     *  naam", where two relations gave the ERROR above. */
    @Test void a_fold_over_a_relation_binds_by_the_type_plural() {
        String c = emitCompact(TYPES + THUISLIGGING + """

            Regel rompsom van een haven
                geldig altijd
                    De rompsom van een Haven moet berekend worden als de som van de romplengten van alle Schuiten van de Haven.
            """);
        assertTrue(c.contains("\"sourceRole\":{\"node\":\"RoleBinding\",\"factType\":\"thuisligging\","
                + "\"role\":\"ligger\"}"), () -> "het typepaar draagt het bereik:\n" + c);
        assertTrue(c.endsWith("\"diagnostics\":[]}"), () -> "zonder marker en zonder waarschuwing:\n" + c);
    }

    /** A range of two steps from a possessive anchor has no spelling here, and the refusal names the step the author
     *  wrote. Before: "(geen relatie verbindt Schuit met Schuit)", true of the one-step derivation the frontend tried
     *  and of nothing in the sentence. */
    @Test void a_range_from_a_possessive_anchor_names_the_written_step() {
        assertEquals(List.of("vouw 'de som van de romplengten van alle liggers van zijn thuishaven' zonder bereik (de "
                        + "geschreven bron 'alle liggers van zijn thuishaven' loopt via 'zijn thuishaven', een ander anker "
                        + "dan het onderwerp, en een bereik vanaf zo'n anker schrijft deze frontend niet): a "
                        + "single-argument fold with an elementType carries its range (sourceRole or sourcePath) under "
                        + "every kind but total: the range is written, never derived"),
                faults(TYPES + THUISLIGGING + """

                    Regel ligprijs van een schuit
                        geldig altijd
                            De ligprijs van een Schuit moet berekend worden als de som van de romplengten van alle liggers van zijn thuishaven.
                    """));
    }

    /** A fold whose source the wire cannot spell, each refused in the reader's sentence for what the emitter would
     *  write: a step written on the source (WF-053), the subject's own attribute under a sum (the loader's sentence
     *  for a fold without its range), and a total over another type's attribute, whose step a total without a range
     *  drops (WF-083). Each of the first three ended in the emitter's exception; the fourth went to disk. */
    @Test void a_fold_the_wire_does_not_spell_is_refused_in_the_sentence_of_the_reader() {
        String step = "met een geschreven stap op haar bron: a path on a fold source is ill-formed (the fold's relation "
                + "is its range, sourceRole or sourcePath)";
        List<String[]> folds = List.of(
                new String[]{"het totaal van het kadegeld van zijn thuishaven",
                        "vouw 'het totaal van het kadegeld van zijn thuishaven' " + step},
                new String[]{"het aantal liggers van zijn thuishaven",
                        "vouw 'het aantal liggers van zijn thuishaven' " + step},
                new String[]{"de som van zijn romplengte", "vouw 'de som van zijn romplengte' over een eigen attribuut van "
                        + "het onderwerp, dat geen instanties telt: a single-argument fold with an elementType carries its "
                        + "range (sourceRole or sourcePath) under every kind but total: the range is written, never "
                        + "derived"},
                new String[]{"het totaal van het kadegeld van de Haven", "vouw 'het totaal van het kadegeld van de Haven' "
                        + "over 'kadegeld' op Haven, een ander type dan het onderwerp: the elementType 'Haven' of a total "
                        + "without a range is not the fold subject 'Schuit': without a range the fold is the subject's own "
                        + "read, so its elementType must name the subject type"});
        for (String[] fold : folds)
            assertEquals(List.of(fold[1]), faults(TYPES + THUISLIGGING + rule(fold[0])), fold[0]);
    }

    // ---------------- a quantifier fold's range ----------------

    /** A counting quantifier over two relations binds by the role its member phrase writes, as a fold does. Before:
     *  the phrase's count words ("ten minste twee") were offered as the written name, nothing bound, and the fold was
     *  a marker whose WARNING said no fact type joined the two types. A type plural over two relations stays a marker,
     *  and its WARNING now says what did not bind. */
    @Test void a_quantifier_fold_over_two_relations_binds_by_the_written_role() {
        String quantifier = """

            Regel veilige haven
                geldig altijd
                    Een Haven is veilig
                    indien %s van de Haven gekeurd zijn.
            """;
        String c = emitCompact(TYPES + THUISLIGGING + WINTERBERGING + quantifier.formatted("ten minste twee van de liggers"));
        assertTrue(c.contains("\"quantifier\":\"atLeast\",\"threshold\":2") && c.contains("\"sourceRole\":{\"node\":"
                + "\"RoleBinding\",\"factType\":\"thuisligging\",\"role\":\"ligger\"}"), c);
        RegelSpraakModel m = Resolver.resolve(TYPES + THUISLIGGING + WINTERBERGING + quantifier.formatted("alle Schuiten"));
        assertTrue(m.diagnostics.stream().anyMatch(d -> d.message().startsWith("de kwantorvouw over 'alle Schuiten van de "
                + "Haven' wordt NIET gelowerd: 2 relaties verbinden Haven met Schuit, en 'alle Schuiten van de Haven' "
                + "noemt geen rol die er precies één aanwijst")), () -> m.diagnostics.toString());
    }

    // ---------------- a role check ----------------

    /** The Haven plays the role `thuishaven` in two fact types, and "de Haven heeft een ligger" names that role from
     *  the Haven's side: two bindings where the wire takes one. Before: the emitter's exception ("de rolcheck … bindt 2
     *  rollen 'thuishaven'"). With one such fact type the check binds. */
    @Test void a_role_check_that_points_to_two_relations_is_refused() {
        String rule = """

            Regel veilige haven
                geldig altijd
                    Een Haven is veilig
                    indien hij een ligger heeft.
            """;
        String second = """

            Feittype winterligging
                de thuishaven Haven
                de winterligger (mv: winterliggers) Schuit
            Eén thuishaven telt meerdere winterliggers
            """;
        assertEquals(List.of("rolcheck 'hij een ligger heeft' zonder binding (2 feittypen kennen de rol 'thuishaven' "
                        + "van Haven tegenover Schuit): missing field 'factType'"),
                faults(TYPES + THUISLIGGING + second + rule));
        String c = emitCompact(TYPES + THUISLIGGING + rule);
        assertTrue(c.contains("{\"node\":\"RoleMembership\",\"factType\":\"thuisligging\",\"role\":\"thuishaven\""),
                () -> "met één relatie bindt de rolcheck:\n" + c);
    }

    // ---------------- an allocation's recipient join ----------------

    private static final String BERGINGEN = """
        Eenheidsysteem Munt
            de euro (mv: euros) EUR €

        Objecttype de Berging (mv: Bergingen)
            de opbrengst Numeriek (geheel getal) met eenheid €;
            de onverdeelde rest Numeriek (geheel getal) met eenheid €;

        Objecttype de Berger (mv: Bergers) (bezield)
            het bergingsaandeel (mv: bergingsaandelen) Numeriek (geheel getal) met eenheid €;

        Feittype hoofddeelname
            de hoofdberging Berging
            de hoofdberger (mv: hoofdbergers) Berger
        Eén hoofdberging heeft meerdere hoofdbergers

        Feittype nevendeelname
            de nevenberging Berging
            de nevenberger (mv: nevenbergers) Berger
        Eén nevenberging heeft meerdere nevenbergers

        Regel verdeel de opbrengst van een berging
            geldig altijd
                De opbrengst van een berging wordt verdeeld over de bergingsaandelen van alle %s van de berging, waarbij wordt verdeeld:
                - in gelijke delen,
                - afgerond op 0 decimalen naar beneden.
                Als onverdeelde rest blijft de onverdeelde rest van de berging over.
        """;

    @Test void an_allocation_over_two_relations_binds_by_the_role_name() {
        // before: the emitter's IllegalStateException ("heeft geen unieke relatie tussen Berging en Berger")
        assertTrue(emitCompact(BERGINGEN.formatted("hoofdbergers")).contains("\"connectingRole\":{\"node\":"
                + "\"RoleBinding\",\"factType\":\"hoofddeelname\",\"role\":\"hoofdberger\"}"));
        assertTrue(emitCompact(BERGINGEN.formatted("nevenbergers")).contains("\"connectingRole\":{\"node\":"
                + "\"RoleBinding\",\"factType\":\"nevendeelname\",\"role\":\"nevenberger\"}"));
    }

    @Test void an_allocation_over_two_relations_is_refused_by_the_type_name() {
        assertEquals(List.of("verdeling 'verdeel de opbrengst van een berging' zonder verbindende rol (2 relaties "
                        + "verbinden Berging met Berger, en 'Bergers' noemt geen rol die er precies één aanwijst): "
                        + "missing field 'connectingRole'"),
                faults(BERGINGEN.formatted("Bergers")));
    }

    // ---------------- a parent-scoped uniqueness's scope ----------------

    /** A second relation whose Haven role carries the name of the first's. */
    private static final String WINTERLIGGING = """

        Feittype winterligging
            de thuishaven Haven
            de winterligger (mv: winterliggers) Schuit
        Eén thuishaven telt meerdere winterliggers
        """;

    private static String unique(String element) {
        return """

            Regel romplengte uniek
                geldig altijd
                    De romplengten van alle %s van de thuishaven moeten uniek zijn.
            """.formatted(element);
    }

    private static String scope(String factType) {
        return "\"scope\":{\"node\":\"RoleBinding\",\"factType\":\"" + factType + "\",\"role\":\"thuishaven\"}";
    }

    /** "de thuishaven" is a role in two relations, and the element role the sentence writes names one of them; where
     *  the parent role names one relation, it decides, however many relations join the two types. Before: the scope
     *  bound by the type pair only, so each of these was the WARNING that the rule was not lowered, and the uniqueness
     *  stayed off the wire. */
    @Test void a_scoped_uniqueness_over_two_relations_binds_by_the_written_role() {
        String c = emitCompact(TYPES + THUISLIGGING + WINTERLIGGING + unique("liggers"));
        assertTrue(c.contains(scope("thuisligging")), () -> "'liggers' wijst één relatie aan:\n" + c);
        String d = emitCompact(TYPES + THUISLIGGING + WINTERLIGGING + unique("winterliggers"));
        assertTrue(d.contains(scope("winterligging")), () -> "'winterliggers' wijst één relatie aan:\n" + d);
        String e = emitCompact(TYPES + THUISLIGGING + WINTERBERGING + unique("Schuiten"));
        assertTrue(e.contains(scope("thuisligging")), () -> "'de thuishaven' wijst één relatie aan:\n" + e);
    }

    /** A type name names no role, so nothing decides between the two relations; the document without the scope is a
     *  uniqueness over every Schuit, which no reader refuses, so the refusal keeps this frontend's words, as the rule's
     *  one message. */
    @Test void a_scoped_uniqueness_over_two_relations_is_refused_by_the_type_name() {
        RegelSpraakModel m = Resolver.resolve(TYPES + THUISLIGGING + WINTERLIGGING + unique("Schuiten"));
        assertEquals(List.of("ERROR: uniciteit 'romplengte uniek' binnen 'de thuishaven' zonder binding (2 relaties "
                        + "kennen een rol 'thuishaven' tegenover Schuit, en 'Schuiten' noemt geen rol die er precies één "
                        + "aanwijst): zonder binding zou de wire haar lezen als een uniciteit over alle instanties van "
                        + "Schuit, en dat is niet wat er staat"),
                m.diagnostics.stream().map(x -> x.severity() + ": " + x.message()).toList());
    }

    /** A scope parent is one instance. Over a relation whose parent role is to-many the scope binds, and before it went
     *  to disk with {@code diagnostics: []}, where the verifier refused the document (WF-054); now the resolver refuses
     *  it in that sentence, as the rule's one message. */
    @Test void a_scoped_uniqueness_over_a_to_many_parent_role_is_refused_in_the_sentence_of_the_verifier() {
        RegelSpraakModel m = Resolver.resolve(TYPES + """

            Feittype thuisligging
                de thuishaven (mv: thuishavens) Haven
                de ligger (mv: liggers) Schuit
            meerdere thuishavens tellen meerdere liggers
            """ + unique("liggers"));
        assertEquals(List.of("ERROR: uniciteit 'romplengte uniek' binnen 'de thuishaven': the scope role 'thuishaven' is "
                        + "to-many — a scope parent is one instance"),
                m.diagnostics.stream().map(x -> x.severity() + ": " + x.message()).toList());
    }

    // ---------------- every position the emitter writes a read at ----------------

    /** One unbound read, "het peil van de Vuurtoren" (no relation reaches a Vuurtoren), at each position the emitter
     *  writes a read at, from the subject it writes it from. Each ends in exactly that one ERROR — the resolver's
     *  walk reaches the position — and the model is refused before the emitter's own refusal could speak. */
    @Test void every_write_position_ends_in_a_diagnostic() {
        List<String[]> forms = List.of(
                new String[]{"Schuit", "De ligprijs van een Schuit moet gesteld worden op het peil van de Vuurtoren."},
                new String[]{"Schuit", """
                        De ligprijs van een Schuit moet gesteld worden op 1
                        indien het peil van de Vuurtoren groter is dan 2."""},
                new String[]{"Haven", "Het kadegeld van een Haven moet groter zijn dan het peil van de Vuurtoren."},
                new String[]{"Haven", "Een Haven heeft een ligger met romplengte gelijk aan het peil van de Vuurtoren."},
                new String[]{"Haven", """
                        Een Haven heeft een ligger met romplengte gelijk aan 3
                        indien het peil van de Vuurtoren groter is dan 2."""},
                new String[]{"Haven", """
                        Een winterschuit van een Haven is een ligger van de Haven
                        indien het peil van de Vuurtoren groter is dan 2."""},
                new String[]{"Schuit", """
                        Een Schuit is gekeurd
                        indien het peil van de Vuurtoren groter is dan 2."""},
                // the quantifier fold's test reads each contributor, so the read is written from the Schuit
                new String[]{"Schuit", """
                        Een Haven is veilig
                        indien de romplengte van alle liggers van de Haven groter is dan het peil van de Vuurtoren."""});
        for (String[] form : forms) {
            String source = TYPES + THUISLIGGING + WINTERBERGING + """

                Regel p
                    geldig altijd
                """ + form[1].indent(8);
            assertEquals(List.of(levelError(form[0])), faults(source), () -> "positie:\n" + form[1]);
        }
        // an exception to a rule is lifted into the defaults, and its condition is walked there
        assertEquals(List.of(levelError("Schuit")), faults(TYPES + rule("5") + """

            Uitzondering op ligprijs van een schuit: dure ligprijs
                geldig altijd
                    De ligprijs van een Schuit moet gesteld worden op 9
                    indien het peil van de Vuurtoren groter is dan 2.
            """));
    }

    private static String levelError(String subject) {
        return "genavigeerde lezing 'het peil van de Vuurtoren' zonder binding (geen relatie verbindt " + subject
                + " met Vuurtoren): an attribute reference without a path is the subject's own read, so its subjectType "
                + "is the subject type '" + subject + "' — 'Vuurtoren' names another type, which only a navigation "
                + "reaches";
    }
}
