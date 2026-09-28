package dev.uchar.regelspraak;

import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.model.RegelSpraakModel.Severity;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A characteristic check under a quantifier whose negation, or whose leading name words, the greedy parse
 * took into the anchor of the member phrase: "alle vondsten van de Jutter geen drijfvermogen hebben".
 *
 * <p>The anchor's name runs on over every name word, so it reads "Jutter geen" and the operator keeps only
 * "drijfvermogen". A trailing "niet" was put back in front of the characteristic ("… niet waardevol zijn"
 * lowered to the negated check); "geen" was not, so the negated possessive form lowered to the POSITIVE
 * check with {@code diagnostics: []} — a verifier-clean document that every executor answered the same way
 * and wrongly. The same run-on cut a multi-word name down to its last word: "dubbel gekeurd" bound the
 * characteristic "gekeurd". The anchor names the rule subject, so the words after that name are the ones
 * the parse took; the fold now puts all of them back, as the subject's own check and the navigated check
 * already did.
 */
class OntkendeKwantorTest {

    /** The reproduction: one Jutter to many Vondsten, a possessive characteristic on the element. */
    static final String REPRODUCTIE = """
        Objecttype de Jutter (mv: Jutters) (bezield)
            is zinkend kenmerk (bijvoeglijk);
        Objecttype de Vondst (mv: Vondsten)
            het drijfvermogen kenmerk (bezittelijk);
        Feittype JutterHeeftVondst
            de jutter Jutter
            de vondst (mv: vondsten) Vondst
        Eén jutter heeft meerdere vondsten

        Regel zinkend
            geldig altijd
                Een Jutter is zinkend
                indien %s.
        """;

    /** The wider model: both kinds of characteristic, multi-word names, a role name that is not the type
     *  name, and a second relation whose role is no characteristic of the element. */
    static final String MODEL = """
        Objecttype de Jutter (mv: Jutters) (bezield)
            is zinkend kenmerk (bijvoeglijk);
            is gestrand kenmerk (bijvoeglijk);
        Objecttype de Vondst (mv: Vondsten)
            het drijfvermogen kenmerk (bezittelijk);
            is waardevol kenmerk (bijvoeglijk);
            het recht op berging kenmerk (bezittelijk);
            is gekeurd kenmerk (bijvoeglijk);
            is dubbel gekeurd kenmerk (bijvoeglijk);
            is geen wrakhout kenmerk (bijvoeglijk);
        Objecttype het Wrak (mv: Wrakken)
            is gezonken kenmerk (bijvoeglijk);
        Feittype VinderVanVondst
            de vinder Jutter
            de vondst (mv: vondsten) Vondst
        Eén vinder heeft meerdere vondsten
        Feittype WrakDraagtLading
            het wrak Wrak
            de lading (mv: ladingen) Vondst
        Eén wrak draagt meerdere ladingen

        Regel zinkend
            geldig altijd
                Een Jutter is zinkend
                indien %s.
        """;

    private static final Pattern VOUW = Pattern.compile("\"condition\":\\s*\\{\\s*\"node\":\\s*\"QuantifierFold\",\\s*"
            + "\"quantifier\":\\s*\"(\\w+)\"");
    private static final Pattern MATRIX = Pattern.compile("\"test\":\\s*\\{\\s*\"node\":\\s*\"CharacteristicCheck\",\\s*"
            + "\"characteristic\":\\s*\"([^\"]*)\",\\s*\"negated\":\\s*(true|false)");

    /** The single rule's condition in words: "&lt;kwantor&gt;: &lt;kenmerk&gt; ontkend|bevestigd", or the node that stands
     *  there instead. */
    private static String vouw(RegelSpraakModel m) {
        String e = Emissie.zonderFouten(m);
        Matcher v = VOUW.matcher(e);
        if (!v.find()) return e.contains("\"node\": \"Unresolved\"") ? "Unresolved" : "geen vouw";
        Matcher t = MATRIX.matcher(e);
        if (!t.find(v.end())) return v.group(1) + ": geen kenmerkmatrix";
        return v.group(1) + ": " + t.group(1) + (t.group(2).equals("true") ? " ontkend" : " bevestigd");
    }

    private static List<Diagnostic> tabel16(RegelSpraakModel m) {
        return m.diagnostics.stream().filter(d -> d.message().contains("maar Tabel 16 vereist")).toList();
    }

    /** The reproduction itself, beside its positive twin: until the fold read the absorbed "geen", both lowered to
     *  one and the same condition. */
    @Test void de_reproductie_lowert_tot_de_ontkende_controle() {
        RegelSpraakModel ontkend = Resolver.resolve(REPRODUCTIE.formatted("alle vondsten van de Jutter geen drijfvermogen hebben"));
        RegelSpraakModel bevestigd = Resolver.resolve(REPRODUCTIE.formatted("alle vondsten van de Jutter drijfvermogen hebben"));
        assertTrue(ontkend.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + ontkend.diagnostics);
        assertEquals("all: drijfvermogen ontkend", vouw(ontkend),
                "'geen drijfvermogen hebben' onder een kwantor hoort de ontkende controle te worden");
        assertEquals("all: drijfvermogen bevestigd", vouw(bevestigd),
                "de bevestigde vorm hoort de bevestigde controle te blijven");
    }

    /** The possessive form lowers the way the adjectival one always did: the negation lands on the matrix. */
    @Test void gelijk_aan_de_bijvoeglijke_vorm() {
        assertEquals("all: waardevol ontkend",
                vouw(Resolver.resolve(MODEL.formatted("alle vondsten van de Jutter niet waardevol zijn"))));
        assertEquals("all: drijfvermogen ontkend",
                vouw(Resolver.resolve(MODEL.formatted("alle vondsten van de Jutter geen drijfvermogen hebben"))));
    }

    /**
     * The measured space: three quantifiers, both kinds, affirmed and negated with either word, both verbs of
     * Tabel 16. The negation lands on the matrix whichever word writes it; a verb that does not fit the kind
     * draws the register warning of the unquantified check, and only on a negated check, as there.
     */
    @Test void elke_kwantor_elk_soort_elke_ontkenning_elk_werkwoord() {
        record Kwantor(String zin, String wire, boolean meervoud) {}
        record Kenmerk(String naam, boolean bezittelijk) {}
        for (Kwantor q : List.of(new Kwantor("alle vondsten van de Jutter", "all", true),
                                 new Kwantor("geen van de vondsten van de Jutter", "none", false),
                                 new Kwantor("ten minste één van de vondsten van de Jutter", "atLeast", false)))
            for (Kenmerk k : List.of(new Kenmerk("drijfvermogen", true), new Kenmerk("waardevol", false)))
                for (String ontkenning : List.of("", "niet ", "geen "))
                    for (boolean hebben : List.of(true, false)) {
                        String werkwoord = hebben ? (q.meervoud() ? "hebben" : "heeft") : (q.meervoud() ? "zijn" : "is");
                        String zin = q.zin() + " " + ontkenning + k.naam() + " " + werkwoord;
                        RegelSpraakModel m = Resolver.resolve(MODEL.formatted(zin));
                        boolean ontkend = !ontkenning.isEmpty();
                        assertEquals(q.wire() + ": " + k.naam() + (ontkend ? " ontkend" : " bevestigd"), vouw(m), zin);
                        int verwacht = ontkend && hebben != k.bezittelijk() ? 1 : 0;
                        assertEquals(verwacht, tabel16(m).size(), () -> zin + " — " + m.diagnostics);
                        assertEquals(verwacht, m.diagnostics.size(), () -> zin + " — " + m.diagnostics);
                    }
    }

    /** A multi-word name is read whole: the parse leaves the operator its last word only, and that word may be a
     *  characteristic of its own. */
    @Test void een_meerwoordige_naam_wordt_heel_gelezen() {
        record Rij(String zin, String vouw) {}
        for (Rij r : List.of(
                new Rij("alle vondsten van de Jutter geen recht op berging hebben", "all: recht op berging ontkend"),
                new Rij("alle vondsten van de Jutter recht op berging hebben", "all: recht op berging bevestigd"),
                new Rij("alle vondsten van de Jutter het recht op berging hebben", "all: recht op berging bevestigd"),
                new Rij("geen van de vondsten van de Jutter geen recht op berging heeft", "none: recht op berging ontkend"),
                new Rij("alle vondsten van de Jutter dubbel gekeurd zijn", "all: dubbel gekeurd bevestigd"),
                new Rij("alle vondsten van de Jutter niet dubbel gekeurd zijn", "all: dubbel gekeurd ontkend"),
                new Rij("ten minste één van de vondsten van de Jutter geen dubbel gekeurd is", "atLeast: dubbel gekeurd ontkend"))) {
            RegelSpraakModel m = Resolver.resolve(MODEL.formatted(r.zin()));
            assertEquals(r.vouw(), vouw(m), r.zin());
            assertTrue(m.diagnostics.isEmpty(), () -> r.zin() + " — " + m.diagnostics);
        }
    }

    /** A written name that is no characteristic of the element stays a marker, said out loud, however much of it
     *  the anchor took: the last word alone ("gekeurd") is a characteristic, and binding it would answer another
     *  question than the sentence asks. */
    @Test void een_onbekende_meerwoordige_naam_weigert_luid() {
        RegelSpraakModel m = Resolver.resolve(MODEL.formatted("alle vondsten van de Jutter niet extern gekeurd zijn"));
        assertEquals("Unresolved", vouw(m), "'extern gekeurd' is geen kenmerk van Vondst");
        assertTrue(m.diagnostics.stream().anyMatch(d -> d.severity() == Severity.WARNING
                        && d.message().contains("'extern gekeurd' is geen kenmerk van Vondst")),
                () -> "de weigering hoort de geschreven naam te noemen: " + m.diagnostics);
    }

    /** The anchor may name the subject by its role in the fold's relation, or with the indefinite article. */
    @Test void het_anker_als_rolnaam_of_met_een() {
        assertEquals("all: drijfvermogen ontkend",
                vouw(Resolver.resolve(MODEL.formatted("alle vondsten van de vinder geen drijfvermogen hebben"))));
        assertEquals("all: dubbel gekeurd bevestigd",
                vouw(Resolver.resolve(MODEL.formatted("alle vondsten van de vinder dubbel gekeurd zijn"))));
        assertEquals("all: drijfvermogen ontkend",
                vouw(Resolver.resolve(MODEL.formatted("alle vondsten van een Jutter geen drijfvermogen hebben"))));
    }

    /** Verbatim first: a characteristic whose declared name begins with "geen" binds itself, affirmed. */
    @Test void een_kenmerk_dat_met_geen_begint_bindt_zichzelf() {
        RegelSpraakModel m = Resolver.resolve(MODEL.formatted("alle vondsten van de Jutter geen wrakhout zijn"));
        assertEquals("all: geen wrakhout bevestigd", vouw(m));
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
    }

    /** The verb-trailing register in a bullet takes the same route as the sentence after 'indien'. */
    @Test void de_vragende_bullet_draagt_de_ontkenning_ook() {
        RegelSpraakModel m = Resolver.resolve(MODEL.formatted("""
                hij aan ten minste één van de volgende voorwaarden voldoet:
                        - alle vondsten van de Jutter geen drijfvermogen hebben
                        - hij is gestrand"""));
        Matcher t = MATRIX.matcher(Emissie.zonderFouten(m));
        assertTrue(t.find(), () -> "geen kenmerkmatrix in de bullet: " + Emissie.zonderFouten(m));
        assertEquals("drijfvermogen true", t.group(1) + " " + t.group(2),
                "de ontkenning in de vragende bullet hoort op de matrix te landen");
    }

    /** A role check of the element under a quantifier is another question and stays a loud marker, negated or
     *  not; the refusal names the role without the negation word, as before. */
    @Test void een_ontkende_rolcontrole_van_het_element_blijft_een_marker() {
        for (String zin : List.of("alle vondsten van de Jutter geen lading zijn",
                                  "geen van de vondsten van de Jutter geen lading is")) {
            RegelSpraakModel m = Resolver.resolve(MODEL.formatted(zin));
            assertEquals("Unresolved", vouw(m), zin);
            assertTrue(m.diagnostics.stream().anyMatch(d -> d.severity() == Severity.WARNING
                            && d.message().contains("'lading' is geen kenmerk van Vondst")),
                    () -> zin + " — " + m.diagnostics);
        }
    }
}
