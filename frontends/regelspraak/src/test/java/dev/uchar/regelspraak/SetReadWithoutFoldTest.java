package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.model.RegelSpraakModel.Severity;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The set fence (`spec/IR-SPEC.md` ch. 4.3; WF-096 until L2, maintainer decision 2026-09-09): a set reading
 * is admissible ONLY as the source of a single-argument instance fold. A §13.4.2 set quantifier ("alle
 * &lt;mv&gt;") with no fold around it therefore names a collection in a slot that takes one value, and the
 * document is ill-formed on the wire — since L2 as a read over a to-many step (WF-053).
 *
 * <p>Until this suite the frontend emitted exactly that node with {@code diagnostics: []} — measured
 * on five routes, all from the one sentence fragment "het gewicht van alle vondsten van de Jutter":
 * as a whole rule value, as an operand of a composite node, as an initialisatie value, inside a
 * voorwaarde, and inside a consistentieregel. Each is pinned below, together with the two
 * counter-proofs that keep the check off the FOLDED reading, which stays legitimate, and the
 * severity, which is the one knob the check has.
 *
 * <p><b>One of those five left the fence on 2026-09-19</b>: the VOORWAARDE. A condition has a
 * quantifier to give the collection a home — the written "alle" lowers to a
 * {@link dev.uchar.regelspraak.ir.Condition.QuantifierFold} — so the set reading is not excused
 * there but removed, and there is nothing left for the fence to name. The two condition rows below
 * therefore measure the lowering; the counter-proofs that the fence still stands wherever the fold
 * DECLINES (an unbindable range, a multi-step range, an unbound die/dat, a telwoord under 1) live with
 * the lowering, in {@link QuantifierFoldTest} and {@link QuantifierFoldFilterTest}. The value positions are
 * untouched: a rule value, a composite operand, a gelijkstelling/initialisatie value and an
 * objectcreatie toekenning take one value and name no quantifier, and widening one of those is a spec
 * question, not a frontend one. So is the §13.4.8 consistentieregel ASSERTION, whose row below still
 * measures the ERROR — its carrier has no violation rule for a fold, so lowering there would trade the
 * ERROR for a WARNING and drop the check from the wire.
 */
class SetReadWithoutFoldTest {

    /** Two object types related by one fact type (one aanmelder to many vondsten) — the model both
     *  card witnesses are written against — plus the Rapport the §13.4.6 objectcreatie creates, so the
     *  toekenning's generic expressie RHS can be exercised on the same sentence fragment. */
    static final String MODEL = """
        Eenheidsysteem Gewicht
            het kilogram (mv: kilogrammen) kg

        Objecttype de Jutter (mv: Jutters) (bezield)
            het maatgevend gewicht Numeriek (getal met 2 decimalen) met eenheid kg;
            het drempelgewicht Numeriek (getal met 2 decimalen) met eenheid kg;
            de peildatum Datum in dagen;

        Objecttype de Vondst (mv: Vondsten)
            het gewicht (mv: gewichten) Numeriek (getal met 2 decimalen) met eenheid kg;
            de vondstdatum (mv: vondstdata) Datum in dagen;

        Objecttype het Rapport (mv: Rapporten)
            het totaalgewicht Numeriek (getal met 2 decimalen) met eenheid kg;

        Feittype vondstaanmelding
            de aanmelder Jutter
            de vondst (mv: vondsten) Vondst
        Eén aanmelder heeft meerdere vondsten

        Feittype rapportage
            de rapporteur Jutter
            het rapport Rapport
        Eén rapporteur heeft één rapport
        """;

    /** The literal message the check raises on the two card witnesses (both read the same fragment): the
     *  verifier's sentence for the rule the emitted wire would fail at this position, WF-053's to-many clause,
     *  naming the hop the emitter writes. Until L2 the Dutch message cited WF-096, then WF-053. */
    static final String MESSAGE =
            "setlezing buiten een vouwbron 'het gewicht van alle vondsten van de Jutter': hop 0 reaches the to-many "
            + "role 'vondst' in a scalar slot — a to-many step needs an aggregation, never a silent first pick";

    private static List<Diagnostic> setReadDiagnostics(String rules) {
        RegelSpraakModel m = Resolver.resolve(MODEL + rules);
        return m.diagnostics.stream().filter(d -> d.message().startsWith("setlezing buiten een vouwbron")).toList();
    }

    private static List<String> meldingen(String rules) {
        return setReadDiagnostics(rules).stream().map(Diagnostic::message).toList();
    }

    private static void noSingleDiagnostic(String rules) {
        RegelSpraakModel m = Resolver.resolve(MODEL + rules);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
    }

    static final String RULE_VALUE = """
            Regel maatgevend gewicht
                geldig altijd
                    Het maatgevend gewicht van een Jutter moet berekend worden als het gewicht van alle vondsten van de Jutter.
            """;

    @Test void set_read_as_whole_rule_value_is_named() {
        // The card's first witness, verbatim: corpus/negative/wf096-setlezing-regelwaarde is this
        // sentence's emission, byte for byte, from before the check existed.
        assertEquals(List.of(MESSAGE), meldingen(RULE_VALUE));
    }

    @Test void set_read_as_operand_of_a_compound_node_is_named() {
        // The card's second witness (corpus/negative/wf096-setlezing-operand): the SAME node, now the
        // left operand of a Binary PLUS. One rule for both positions — they measure identically, and
        // the frontend has no reason to split them that the IR does not have.
        assertEquals(List.of(MESSAGE), meldingen("""
                Regel maatgevend gewicht
                    geldig altijd
                        Het maatgevend gewicht van een Jutter moet berekend worden als het gewicht van alle vondsten van de Jutter plus het drempelgewicht van de Jutter.
                """));
    }

    @Test void set_read_as_equation_value_is_named() {
        // The same value slot reached by the §13.4.4 gelijkstelling ("moet gesteld worden op", soort
        // GELIJKSTELLING) instead of the §13.4.2 berekening — measured WF-096 FAIL at the same pointer.
        assertEquals(List.of(MESSAGE), meldingen("""
                Regel maatgevend gewicht
                    geldig altijd
                        Het maatgevend gewicht van een Jutter moet gesteld worden op het gewicht van alle vondsten van de Jutter.
                """));
    }

    @Test void set_read_as_initialisation_value_is_named() {
        // And by the §13.4.9 initialisatie proper ("moet geïnitialiseerd worden op", soort
        // INITIALISATIE) — the third assignment form into the same slot, measured separately.
        assertEquals(List.of(MESSAGE), meldingen("""
                Regel maatgevend gewicht
                    geldig altijd
                        Het maatgevend gewicht van een Jutter moet geïnitialiseerd worden op het gewicht van alle vondsten van de Jutter.
                """));
    }

    @Test void set_read_in_a_condition_gets_a_quantifier_as_home() {
        // The one position that LEFT this fence, on 2026-09-19: a CONDITION has a quantifier to give the
        // collection a home, so the written "alle" lowers to a QuantifierFold and there is no set reading
        // left to name (QuantifierFoldTest / NonTemporalFoldTest measure the node). Until then this row
        // measured the same WF-096 ERROR as the value positions do.
        assertEquals(List.of(), meldingen("""
                Regel maatgevend gewicht
                    geldig altijd
                        Het maatgevend gewicht van een Jutter moet berekend worden als het drempelgewicht van de Jutter
                        indien het gewicht van alle vondsten van de Jutter gelijk is aan 5 kg.
                """));
    }


    @Test void set_read_as_object_creation_member_init_is_named() {
        // The §13.4.6.3 waardetoekenning is a generic expressie RHS and the third (and last) VALUE slot
        // this IR has — a live producer route of its own: before the check the verifier reported
        // WF-096 FAIL /objectCreations/0/toekenningen/0/waarde as the document's ONLY defect (measured
        // 2026-09-11, before M1; the verifier names that position /rules/objectCreations/0/toekenningen/0/waarde
        // since).
        assertEquals(List.of(MESSAGE), meldingen("""
                Regel nieuw rapport
                    geldig altijd
                        Een Jutter heeft een Rapport met het totaalgewicht gelijk aan het gewicht van alle vondsten van de Jutter.
                """));
    }

    @Test void the_sum_in_an_object_creation_member_init_stays_a_valid_fold_source() {
        // The counter-proof in that same slot: under a fold the reading is the source the fold consumes.
        noSingleDiagnostic("""
                Regel nieuw rapport
                    geldig altijd
                        Een Jutter heeft een Rapport met het totaalgewicht gelijk aan de som van het gewicht van alle vondsten van de Jutter.
                """);
    }

    @Test void the_condition_of_an_object_creation_folds_along() {
        // An objectcreatie never becomes a ResolvedRule (measured: resolvedRules 0, objectCreations 1),
        // so its voorwaarde is the witness that the condition treatment sits where EVERY resultaatdeel
        // kind's voorwaarde passes through — not on the rule carrier alone. The same holds for a
        // verdeling, a feitcreatie, a dagsoortdefinitie and a startpuntbepaling. Until 2026-09-19 it
        // witnessed the WF-096 ERROR there; it now witnesses the lowering there, which is the same
        // claim about the same one place. Since 2026-10-06 the document is refused for another reason: the creation
        // mints the vondsten its condition reads under "alle", a cycle through no recursive creation with a negative
        // edge (WF-045, WF-046), which the frontend let through until then; the set reading itself draws nothing.
        RegelSpraakModel m = Resolver.resolve(MODEL + """
                Regel nieuwe vondst
                    geldig altijd
                        Een Jutter heeft een Vondst met het gewicht gelijk aan 3 kg
                        indien het gewicht van alle vondsten van de Jutter gelijk is aan 5 kg.
                """);
        assertEquals(List.of("objectcreatie 'nieuwe vondst' hangt via een kring van zichzelf af; de afhankelijkheden "
                        + "zijn kringvrij buiten een recursieve creatie (WF-045), en een kring draagt geen negatieve "
                        + "rand, zoals een ontkenning (WF-046): dependency cycle through no recursive creation: nieuwe "
                        + "vondst -> nieuwe vondst"),
                m.diagnostics.stream().map(Diagnostic::message).toList());
    }

    @Test void set_read_in_a_consistency_rule_is_named() {
        // The §13.4.8 assertion is a Conditie in its own carrier; measured verifier-FAIL at
        // /consistenties/0/assertie/links before the check existed.
        assertEquals(1, meldingen("""
                Regel gewichtscontrole
                    geldig altijd
                        Het gewicht van alle vondsten van de Jutter moet gelijk zijn aan het drempelgewicht van de Jutter.
                """).size());
    }

    @Test void the_sum_of_a_set_read_stays_a_valid_fold_source() {
        // The counter-proof: the fence guards the POSITION, not the set reading. Under a fold the same
        // node is the source the fold consumes, and it stays verifier-clean.
        noSingleDiagnostic("""
                Regel maatgevend gewicht
                    geldig altijd
                        Het maatgevend gewicht van een Jutter moet berekend worden als de som van het gewicht van alle vondsten van de Jutter.
                """);
    }

    @Test void the_first_of_a_set_read_stays_a_valid_fold_source() {
        // The second counter-proof, on the fold kind the executors disagree about. That divergence sits
        // INSIDE the fence's exemption and is a separate, still-open question; here the form only has to
        // keep passing the frontend unchanged.
        noSingleDiagnostic("""
                Regel peildatum
                    geldig altijd
                        De peildatum van een Jutter moet berekend worden als de eerste van de vondstdatum van alle vondsten van de Jutter.
                """);
    }

    @Test void the_diagnostic_is_an_error() {
        // The one knob, pinned so that moving it is a deliberate, visible act. ERROR is the
        // fail-fast level (maintainer decision 2026-09-11, promoting the initial WARNING):
        // irMode writes nothing when the model carries an error, so the ill-formed document is
        // not produced at all — that wire is never written by this frontend. The emitter,
        // called directly, refuses the model as well (WF-113 (a), the test below).
        List<Diagnostic> ds = setReadDiagnostics(RULE_VALUE);
        assertEquals(1, ds.size());
        assertEquals(Severity.ERROR, ds.get(0).severity());
        // the line is the sentence's own, so a reader finds the setlezing by the number alone
        assertEquals("Het maatgevend gewicht van een Jutter moet berekend worden als het gewicht van alle vondsten van de Jutter.",
                (MODEL + RULE_VALUE).lines().toList().get(ds.get(0).line() - 1).strip());
    }

    @Test void the_emitter_refuses_the_model_with_the_error() {
        // The defect the card names was a document that said NOTHING about itself: the frontend emitted
        // this node with `diagnostics: []`. Since WF-113 (a) (2026-09-23) the wire carries only WARNINGs,
        // because an ERROR means the producer writes no document; the emitter therefore refuses this model
        // by itself, and the refusal names the fence's sentence.
        RegelSpraakModel m = Resolver.resolve(MODEL + RULE_VALUE);
        assertTrue(m.errors().stream().anyMatch(d -> d.message().contains("setlezing buiten een vouwbron")),
                () -> "de setlezing-fout ontbreekt in het model: " + m.diagnostics);
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> IrEmitter.emit(m));
        assertTrue(e.getMessage().contains("WF-113 (a)"), e::getMessage);
        assertTrue(e.getMessage().contains("setlezing buiten een vouwbron"), e::getMessage);
    }
}
