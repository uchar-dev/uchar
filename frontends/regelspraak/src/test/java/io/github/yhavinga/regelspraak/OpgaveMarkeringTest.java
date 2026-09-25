package io.github.yhavinga.regelspraak;

import io.github.yhavinga.regelspraak.emit.IrEmitter;
import io.github.yhavinga.regelspraak.ir.Conditie;
import io.github.yhavinga.regelspraak.ir.Default;
import io.github.yhavinga.regelspraak.ir.Expr;
import io.github.yhavinga.regelspraak.ir.Geldigheid;
import io.github.yhavinga.regelspraak.ir.PrioritizedDefaults;
import io.github.yhavinga.regelspraak.ir.ResolvedConsistentie;
import io.github.yhavinga.regelspraak.ir.ResolvedRegel;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Diagnostic;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Objecttype;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Severity;
import io.github.yhavinga.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The opgave marker (grammar/DIVERGENCES.md §7): {@code (mag opgegeven worden)} as the last element of an attribute
 * declaration. For a marked attribute X of object type T the resolver lowers five pieces onto structure the wire
 * already carries: the input attribute {@code opgegeven X}; the attribute {@code berekende X}, which every computing
 * writer of X now targets; one defaults group {@code opgave boven berekening X} on X with the opgave default above
 * the computation default and an authored initialisation of X as the lowest default; the possessive characteristic
 * {@code afwijkende opgave X}, attributed by the rule {@code afwijking opgave X}; and the consistency
 * {@code controle opgave X}. Measured on the frontend before this card: every document below that writes the
 * marker was a syntax error at the marker ("extraneous input '(' expecting {&lt;EOF&gt;, …}"), and a rule reading
 * {@code afwijkende opgave X} or {@code berekende X} kept a marker on the wire with a WARNING, so 28 of the 30
 * witnesses fail there; the two inertness witnesses pass there and must keep passing.
 */
class OpgaveMarkeringTest {

    /** The marked declaration stands on line 3. */
    private static final String VONDST = """
        Objecttype de Vondst (mv: Vondsten)
            de taxatie Numeriek (getal met 2 decimalen);
            de waarde Numeriek (getal met 2 decimalen) (mag opgegeven worden);
        """;

    private static final String WAARDERING = """

        Regel waardering
            geldig altijd
                De waarde van een Vondst moet berekend worden als de taxatie van de Vondst verminderd met 5,00.
        """;

    private static final String STANDAARDWAARDE = """

        // Bron: standaardwaarde
        Regel standaardwaarde
            geldig altijd
                De waarde van een Vondst moet geïnitialiseerd worden op 25,00.
        """;

    private static String compact(String s) {
        return s.replaceAll("\\s+", "");
    }

    private static Diagnostic enigeDiagnose(RegelSpraakModel m) {
        assertEquals(1, m.diagnostics.size(), () -> "verwachtte precies één diagnose, kreeg: " + m.diagnostics);
        return m.diagnostics.get(0);
    }

    private static Diagnostic enigeFout(RegelSpraakModel m) {
        assertEquals(1, m.diagnostics.size(), () -> "verwachtte precies één diagnose, kreeg: " + m.diagnostics);
        Diagnostic d = m.diagnostics.get(0);
        assertEquals(Severity.ERROR, d.severity(), () -> "verwachtte een ERROR: " + d);
        return d;
    }

    private static ResolvedRegel regel(RegelSpraakModel m, String naam) {
        return m.resolvedRegels.stream().filter(r -> r.naam().equals(naam)).findFirst()
                .orElseThrow(() -> new AssertionError("geen regel '" + naam + "' in " + m.resolvedRegels.stream()
                        .map(ResolvedRegel::naam).toList()));
    }

    private static PrioritizedDefaults groep(RegelSpraakModel m, String naam) {
        return m.prioritizedDefaults.stream().filter(g -> g.naam().equals(naam)).findFirst()
                .orElseThrow(() -> new AssertionError("geen defaults-groep '" + naam + "' in " + m.prioritizedDefaults
                        .stream().map(PrioritizedDefaults::naam).toList()));
    }

    private static String doelVan(ResolvedRegel r) {
        return ((Expr.AttribuutRef) r.doel()).attribuut();
    }

    private static String gelezen(Expr e) {
        return ((Expr.AttribuutRef) e).attribuut();
    }

    // -------------------------------------------------------------------- the five pieces of the lowering

    @Test void de_markering_bouwt_de_vijf_stukken() {
        RegelSpraakModel m = Resolver.resolve(VONDST + WAARDERING);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        Objecttype v = m.objecttypen.get("Vondst");

        // 1 and 2: the two attributes, right after X, with X's datatype
        assertEquals(List.of("taxatie", "waarde", "opgegeven waarde", "berekende waarde"),
                List.copyOf(v.attributen.keySet()));
        assertEquals(v.attributen.get("waarde"), v.attributen.get("opgegeven waarde"));
        assertEquals(v.attributen.get("waarde"), v.attributen.get("berekende waarde"));

        // 2: the computing writer keeps its name and shape and targets berekende X
        ResolvedRegel w = regel(m, "waardering");
        assertEquals("berekende waarde", doelVan(w));
        assertEquals(ResolvedRegel.Soort.BEREKENING, w.soort());
        assertEquals("taxatie", gelezen(((Expr.Binair) w.waarde()).links()));

        // 3: one group on X, from the top: the opgave, then the computation
        assertEquals(1, m.prioritizedDefaults.size(), "één defaults-groep");
        PrioritizedDefaults g = groep(m, "opgave boven berekening waarde");
        assertEquals("Vondst", g.onderwerp());
        assertEquals("waarde", g.attribuut());
        assertNull(g.kenmerk());
        assertFalse(g.ongeordend());
        assertEquals(Geldigheid.ALTIJD, g.geldigheid());
        assertNull(g.bron(), "de gegenereerde groep draagt geen statuutannotatie");
        assertEquals(2, g.defaults().size());
        assertEquals(List.of("opgegeven waarde", "berekende waarde"),
                g.defaults().stream().map(d -> gelezen(d.consequentie())).toList(), "van hoog naar laag");
        assertEquals(List.of("opgegeven waarde", "berekende waarde"),
                g.defaults().stream().map(Default::bron).toList(), "elke default draagt zijn bloknaam");
        for (Default d : g.defaults()) {
            Conditie.Samengesteld altijd = assertInstanceOf(Conditie.Samengesteld.class, d.voorwaarde());
            assertEquals(Conditie.Kwantor.ALLE, altijd.kwantor());
            assertTrue(altijd.onderdelen().isEmpty());
            assertEquals("altijd", altijd.bron().snippet());
            assertFalse(d.initialisatie());
            assertNull(d.besluit());
            assertNull(d.excepties());
        }

        // 4: the possessive characteristic and the rule that attributes it
        assertEquals(List.of("afwijkende opgave waarde"), v.kenmerken);
        assertEquals(Objecttype.KenmerkSoort.BEZITTELIJK, v.kenmerkSoorten.get("afwijkende opgave waarde"));
        ResolvedRegel a = regel(m, "afwijking opgave waarde");
        assertEquals(ResolvedRegel.Soort.KENMERKTOEKENNING, a.soort());
        assertEquals("afwijkende opgave waarde", doelVan(a));
        assertEquals(Geldigheid.ALTIJD, a.geldigheid());
        Conditie.Samengesteld alle = assertInstanceOf(Conditie.Samengesteld.class, a.voorwaarde());
        assertEquals(Conditie.Kwantor.ALLE, alle.kwantor());
        assertEquals(3, alle.onderdelen().size());
        Conditie.Predicaat p0 = assertInstanceOf(Conditie.Predicaat.class, alle.onderdelen().get(0));
        Conditie.Predicaat p1 = assertInstanceOf(Conditie.Predicaat.class, alle.onderdelen().get(1));
        Conditie.Vergelijking p2 = assertInstanceOf(Conditie.Vergelijking.class, alle.onderdelen().get(2));
        assertEquals(Conditie.Predicaatsoort.GEVULD, p0.soort());
        assertEquals("opgegeven waarde", gelezen(p0.operand()));
        assertEquals(Conditie.Predicaatsoort.GEVULD, p1.soort());
        assertEquals("berekende waarde", gelezen(p1.operand()));
        assertEquals(Conditie.Operator.ONGELIJK, p2.op());
        assertEquals("opgegeven waarde", gelezen(p2.links()));
        assertEquals("berekende waarde", gelezen(p2.rechts()));

        // 5: the consistency, its assertion the negated characteristic, at the marked declaration
        assertEquals(1, m.consistenties.size());
        ResolvedConsistentie c = m.consistenties.get(0);
        assertEquals("controle opgave waarde", c.naam());
        assertEquals("Vondst", c.subject().naam);
        Conditie.Kenmerk k = assertInstanceOf(Conditie.Kenmerk.class, c.assertie());
        assertEquals("afwijkende opgave waarde", k.kenmerk());
        assertTrue(k.negatie());
        assertEquals(3, c.line());
        assertEquals(Geldigheid.ALTIJD, c.geldigheid());

        // the order of the rule list: the authored rule where it stood, the generated one behind it
        assertEquals(List.of("waardering", "afwijking opgave waarde"),
                m.resolvedRegels.stream().map(ResolvedRegel::naam).toList());
    }

    @Test void de_wire_draagt_de_lowering_in_de_spelling_van_het_uitgeschreven_patroon() {
        RegelSpraakModel m = Resolver.resolve(VONDST + WAARDERING);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        String ir = IrEmitter.emit(m);
        Verslag.verantwoord(ir);
        String c = compact(ir);
        String groep = compact("""
            {"node": "PrioritizedDefaults", "name": "opgave boven berekening waarde", "subject": "Vondst",
             "attribute": "waarde", "defaults": [
               {"node": "Default",
                "justification": {"node": "Compound", "quantifier": "all", "threshold": 0, "parts": [],
                                  "source": {"snippet": "altijd"}},
                "consequence": {"node": "AttributeReference", "attribute": "opgegeven waarde", "subjectType": "Vondst",
                                "source": {"snippet": "zijn opgegeven waarde"}},
                "source": {"snippet": "opgegeven waarde"}},
               {"node": "Default",
                "justification": {"node": "Compound", "quantifier": "all", "threshold": 0, "parts": [],
                                  "source": {"snippet": "altijd"}},
                "consequence": {"node": "AttributeReference", "attribute": "berekende waarde", "subjectType": "Vondst",
                                "source": {"snippet": "zijn berekende waarde"}},
                "source": {"snippet": "berekende waarde"}}],
             "validity": {"node": "Validity"}}""");
        assertTrue(c.contains(groep), () -> "de groep staat niet zo op de wire:\n" + ir);
        String regel = compact("""
            {"node": "Rule", "name": "afwijking opgave waarde", "versions": [
              {"validity": {"node": "Validity"},
               "target": {"node": "AttributeReference", "attribute": "afwijkende opgave waarde", "subjectType": "Vondst",
                          "source": {"snippet": "Een Vondst heeft afwijkende opgave waarde"}},
               "value": {"node": "Literal", "value": {"boolean": true}, "type": {"node": "Boolean"},
                         "source": {"snippet": "waar"}},
               "condition": {"node": "Compound", "quantifier": "all", "threshold": 0, "parts": [
                   {"node": "Predicate", "kind": "filled",
                    "operand": {"node": "AttributeReference", "attribute": "opgegeven waarde", "subjectType": "Vondst",
                                "source": {"snippet": "zijn opgegeven waarde"}},
                    "negated": false, "digits": 0, "source": {"snippet": "zijn opgegeven waarde is gevuld"}},
                   {"node": "Predicate", "kind": "filled",
                    "operand": {"node": "AttributeReference", "attribute": "berekende waarde", "subjectType": "Vondst",
                                "source": {"snippet": "zijn berekende waarde"}},
                    "negated": false, "digits": 0, "source": {"snippet": "zijn berekende waarde is gevuld"}},
                   {"node": "Comparison", "op": "notEqual",
                    "left": {"node": "AttributeReference", "attribute": "opgegeven waarde", "subjectType": "Vondst",
                             "source": {"snippet": "zijn opgegeven waarde"}},
                    "right": {"node": "AttributeReference", "attribute": "berekende waarde", "subjectType": "Vondst",
                              "source": {"snippet": "zijn berekende waarde"}},
                    "source": {"snippet": "zijn opgegeven waarde is ongelijk aan zijn berekende waarde"}}],
                 "source": {"snippet": "hij aan alle volgende voorwaarden voldoet: - zijn opgegeven waarde is gevuld - zijn berekende waarde is gevuld - zijn opgegeven waarde is ongelijk aan zijn berekende waarde."}},
               "initialisation": false}]}""");
        assertTrue(c.contains(regel), () -> "de afwijkingsregel staat niet zo op de wire:\n" + ir);
        String consistentie = compact("""
            {"node": "Consistency", "name": "controle opgave waarde", "versions": [
              {"subject": "Vondst",
               "assertion": {"node": "CharacteristicCheck", "characteristic": "afwijkende opgave waarde",
                             "negated": true, "source": {"snippet": "hij heeft geen afwijkende opgave waarde"}},
               "validity": {"node": "Validity"},
               "source": {"snippet": "de waarde Numeriek (getal met 2 decimalen) (mag opgegeven worden)", "line": 3}}]}""");
        assertTrue(c.contains(consistentie), () -> "de consistentie staat niet zo op de wire:\n" + ir);
        String leden = compact("""
            {"node": "Attribute", "name": "opgegeven waarde",
             "datatype": {"node": "Number", "whole": false, "decimals": 2, "sign": "unspecified"}},
            {"node": "Attribute", "name": "berekende waarde",
             "datatype": {"node": "Number", "whole": false, "decimals": 2, "sign": "unspecified"}}],
            "characteristics": [{"node": "Characteristic", "name": "afwijkende opgave waarde", "kind": "possessive"}]""");
        assertTrue(c.contains(leden), () -> "de gegenereerde leden staan niet zo op de wire:\n" + ir);
        assertTrue(c.contains(compact("""
            "target": {"node": "AttributeReference", "attribute": "berekende waarde", "subjectType": "Vondst",
                       "source": {"snippet": "De waarde van een Vondst"}}""")),
                () -> "de rekenregel schrijft niet 'berekende waarde' met zijn eigen brontekst:\n" + ir);
    }

    @Test void de_initialisatie_blijft_op_het_doel_als_laagste_default() {
        RegelSpraakModel m = Resolver.resolve(VONDST + WAARDERING + STANDAARDWAARDE);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        PrioritizedDefaults g = groep(m, "opgave boven berekening waarde");
        assertEquals("waarde", g.attribuut());
        assertEquals(3, g.defaults().size());
        Default init = g.defaults().get(2);
        assertTrue(init.initialisatie(), "de initialisatie is de laagste default");
        assertNull(init.voorwaarde(), "een initialisatiedefault draagt geen geschreven rechtvaardiging");
        assertEquals("25,00", ((Expr.Literal) init.consequentie()).waarde());
        assertEquals("standaardwaarde", init.bron(), "de statuutannotatie van de initialisatieregel");
        assertFalse(g.defaults().get(0).initialisatie());
        assertFalse(g.defaults().get(1).initialisatie());
        assertEquals(List.of("waardering", "afwijking opgave waarde"),
                m.resolvedRegels.stream().map(ResolvedRegel::naam).toList(),
                "de initialisatieregel is opgenomen in de groep en staat niet meer als regel");
        String c = compact(IrEmitter.emit(m));
        assertTrue(c.contains(compact("""
            {"node": "Default",
             "consequence": {"node": "Literal", "value": {"number": "25.00"},
                             "type": {"node": "Number", "whole": true, "decimals": 0, "sign": "unspecified"},
                             "source": {"snippet": "25,00"}},
             "initialisation": true, "source": {"snippet": "standaardwaarde"}}],""")), c);
    }

    @Test void lezers_van_het_doel_blijven_het_doel_lezen() {
        RegelSpraakModel m = Resolver.resolve(VONDST.replace("(mv: Vondsten)\n",
                "(mv: Vondsten)\n    de heffing Numeriek (getal met 2 decimalen);\n") + WAARDERING + """

            Regel heffing
                geldig altijd
                    De heffing van een Vondst moet berekend worden als de waarde van de Vondst maal 2.
            """);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        ResolvedRegel h = regel(m, "heffing");
        assertEquals("heffing", doelVan(h));
        assertEquals("waarde", gelezen(((Expr.Binair) h.waarde()).links()), "de lezer leest het doel zelf");
    }

    @Test void het_gegenereerde_kenmerk_en_de_berekende_waarde_zijn_leesbaar() {
        RegelSpraakModel m = Resolver.resolve(VONDST.replace("(mv: Vondsten)\n",
                "(mv: Vondsten)\n    is ter controle kenmerk (bijvoeglijk);\n    de korting Numeriek (getal met 2 decimalen);\n")
                + WAARDERING + """

            Regel controle
                geldig altijd
                    Een Vondst is ter controle
                    indien hij afwijkende opgave waarde heeft.

            Regel korting
                geldig altijd
                    De korting van een Vondst moet berekend worden als de taxatie van de Vondst min zijn berekende waarde.
            """);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        Conditie.Kenmerk k = assertInstanceOf(Conditie.Kenmerk.class, regel(m, "controle").voorwaarde());
        assertEquals("afwijkende opgave waarde", k.kenmerk());
        assertFalse(k.negatie());
        assertEquals("berekende waarde", gelezen(((Expr.Binair) regel(m, "korting").waarde()).rechts()));
    }

    @Test void een_uitzonderingsboom_verhuist_heel_naar_de_berekening() {
        RegelSpraakModel m = Resolver.resolve(VONDST.replace("(mv: Vondsten)\n",
                "(mv: Vondsten)\n    is beschadigd kenmerk (bijvoeglijk);\n") + WAARDERING + """

            Uitzondering op waardering: waardering bij schade
                geldig altijd
                    De waarde van een Vondst moet berekend worden als de taxatie van de Vondst verminderd met 10,00
                    indien hij beschadigd is.
            """);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        assertEquals(List.of("waardering", "opgave boven berekening waarde"),
                m.prioritizedDefaults.stream().map(PrioritizedDefaults::naam).toList());
        PrioritizedDefaults boom = groep(m, "waardering");
        assertEquals("berekende waarde", boom.attribuut(), "de boom schrijft de berekening");
        assertEquals(2, boom.defaults().size(), "naam en vorm blijven");
        assertEquals("waarde", groep(m, "opgave boven berekening waarde").attribuut());
        assertEquals(List.of("afwijking opgave waarde"), m.resolvedRegels.stream().map(ResolvedRegel::naam).toList());
    }

    @Test void een_ongeordend_blok_verhuist_als_set() {
        RegelSpraakModel m = Resolver.resolve(VONDST.replace("(mv: Vondsten)\n",
                "(mv: Vondsten)\n    is beschadigd kenmerk (bijvoeglijk);\n    is gerestaureerd kenmerk (bijvoeglijk);\n")
                + """

            Uitzonderingen ongeordend:
            Regel waardering bij schade
                geldig altijd
                    De waarde van een Vondst moet gesteld worden op 10,00
                    indien hij beschadigd is.

            Regel waardering na restauratie
                geldig altijd
                    De waarde van een Vondst moet gesteld worden op 30,00
                    indien hij gerestaureerd is.
            """);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        assertEquals(3, m.prioritizedDefaults.size());
        for (String lid : List.of("waardering bij schade", "waardering na restauratie")) {
            PrioritizedDefaults g = groep(m, lid);
            assertEquals("berekende waarde", g.attribuut(), lid + " schrijft de berekening");
            assertTrue(g.ongeordend(), lid + " blijft ongeordend");
        }
        PrioritizedDefaults opgave = groep(m, "opgave boven berekening waarde");
        assertEquals("waarde", opgave.attribuut());
        assertFalse(opgave.ongeordend());
    }

    @Test void een_beslistabel_verhuist_per_rij() {
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype de Jutter (mv: Jutters) (bezield)
                de bonus Numeriek (geheel getal) (mag opgegeven worden);
                het aantal geborgen wrakken Numeriek (niet-negatief geheel getal);

            Beslistabel Bonustabel
                geldig altijd
            | | de bonus van een Jutter moet gesteld worden op | indien zijn aantal geborgen wrakken groter is dan |
            |---|---|---|
            | 1 | 10 | 5 |
            | 2 | 20 | 10 |
            """);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        assertEquals(List.of("berekende bonus", "berekende bonus"), m.resolvedRegels.stream()
                .filter(r -> r.naam().startsWith("Bonustabel")).map(OpgaveMarkeringTest::doelVan).toList());
        assertEquals("bonus", groep(m, "opgave boven berekening bonus").attribuut());
    }

    // -------------------------------------------------------------------- inertness: the token and its words

    @Test void zonder_markering_verandert_er_niets() {
        String zonder = VONDST.replace(" (mag opgegeven worden)", "") + WAARDERING + """

            // de waarde (mag opgegeven worden) staat hier in commentaar
            """;
        RegelSpraakModel m = Resolver.resolve(zonder);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        assertEquals("waarde", doelVan(regel(m, "waardering")));
        assertTrue(m.prioritizedDefaults.isEmpty());
        assertTrue(m.consistenties.isEmpty());
        String ir = IrEmitter.emit(m);
        assertFalse(ir.contains("opgegeven") || ir.contains("berekende"), ir);
    }

    @Test void de_woorden_van_de_markering_blijven_naamwoorden_en_tekst() {
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype de Vondst (mv: Vondsten)
                de opgegeven stand Numeriek (geheel getal);
                de mag worden stand Numeriek (geheel getal);
                de toelichting Tekst;

            Regel toelichting
                geldig altijd
                    De toelichting van een Vondst moet gesteld worden op "(mag opgegeven worden)".
            """);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnostiek: " + m.diagnostics);
        assertEquals(List.of("opgegeven stand", "mag worden stand", "toelichting"),
                List.copyOf(m.objecttypen.get("Vondst").attributen.keySet()));
        assertTrue(m.prioritizedDefaults.isEmpty());
        assertEquals("(mag opgegeven worden)", ((Expr.Literal) regel(m, "toelichting").waarde()).waarde());
    }

    // -------------------------------------------------------------------- refusals, each an ERROR at the declaration

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "bijvoeglijk | is getaxeerd kenmerk (bijvoeglijk) (mag opgegeven worden); | getaxeerd",
            "bezittelijk | het keurmerk kenmerk (bezittelijk) (mag opgegeven worden); | keurmerk"
    })
    void de_markering_op_een_kenmerk_weigert(String soort, String declaratie, String kenmerk) {
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype de Vondst (mv: Vondsten)
                de taxatie Numeriek (getal met 2 decimalen);
                %s
            """.formatted(declaratie));
        Diagnostic d = enigeFout(m);
        assertEquals("kenmerk '" + kenmerk + "' van Vondst: '(mag opgegeven worden)' staat alleen op een attribuut — "
                + "casusinvoer op een kenmerk staat al tegen elk besluit van een defaults-groep (RUNTIME-SEMANTICS "
                + "ch. 7.8), en een opgave 'niet aanwezig' boven een berekende toekenning vraagt een opgave met drie "
                + "waarden, die de casus voor een kenmerk niet kent (dialect §7)", d.message());
        assertEquals(3, d.line());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "Tekst                                   | Tekst",
            "Numeriek (geheel getal) met eenheid €  | Numeriek met eenheid €",
            "Percentage (getal met 2 decimalen)      | Percentage",
            "Boolean                                 | Boolean",
            "Datum in dagen                          | Datum in dagen"
    })
    void de_markering_op_iets_anders_dan_een_getal_zonder_eenheid_weigert(String datatype, String genoemd) {
        RegelSpraakModel m = Resolver.resolve("""
            Eenheidsysteem Munt
                de euro (mv: euros) EUR €

            Objecttype de Vondst (mv: Vondsten)
                de opgave %s (mag opgegeven worden);
            """.formatted(datatype));
        Diagnostic d = enigeFout(m);
        assertEquals("attribuut 'opgave' van Vondst: '(mag opgegeven worden)' staat alleen op een Numeriek attribuut "
                + "zonder eenheid, en 'opgave' is " + genoemd + " — een defaults-groep op een attribuut van een ander "
                + "datatype of met een eenheid is nog niet in alle drie de uitvoerders gemeten (dialect §7)", d.message());
        assertEquals(5, d.line());
    }

    @Test void de_markering_op_een_tijdsafhankelijk_attribuut_weigert() {
        RegelSpraakModel m = Resolver.resolve(VONDST.replace("(getal met 2 decimalen) (mag",
                "(getal met 2 decimalen) voor elk jaar (mag"));
        Diagnostic d = enigeFout(m);
        assertEquals("attribuut 'waarde' van Vondst: '(mag opgegeven worden)' staat niet op een tijdsafhankelijk "
                + "attribuut — een defaults-groep beslist één keer en heeft geen tijdlijn om op te schrijven (WF-086) "
                + "(dialect §7)", d.message());
        assertEquals(3, d.line());
    }

    @Test void de_markering_op_een_gedimensioneerd_attribuut_weigert() {
        RegelSpraakModel m = Resolver.resolve("""
            Dimensie het seizoen, bestaande uit de seizoenen (na het attribuut met voorzetsel in):
                1. voorjaar
                2. najaar

            Objecttype de Jutter (mv: Jutters) (bezield)
                de premie Numeriek (geheel getal) gedimensioneerd met seizoen (mag opgegeven worden);
            """);
        Diagnostic d = enigeFout(m);
        assertEquals("attribuut 'premie' van Jutter: '(mag opgegeven worden)' staat niet op een gedimensioneerd "
                + "attribuut, en 'premie' is gedimensioneerd met seizoen — een defaults-groep op een gedimensioneerd "
                + "attribuut speelt nog niet in alle drie de uitvoerders (dialect §7)", d.message());
        assertEquals(6, d.line());
    }

    @Test void een_verdeling_die_het_doel_schrijft_weigert() {
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype de Sluitkas (mv: Sluitkassen)
                de sluitpremie Numeriek (geheel getal);
                de sluitrest Numeriek (geheel getal);

            Objecttype de Jutter (mv: Jutters) (bezield)
                de strandrang Numeriek (geheel getal);
                het sluitdeel Numeriek (geheel getal) (mag opgegeven worden);

            Feittype sluitdienst
                de sluitkas Sluitkas
                de sluitjutter (mv: sluitjutters) Jutter
            Eén sluitkas heeft meerdere sluitjutters

            Regel verdeel de sluitpremie
                geldig altijd
                    De sluitpremie van een sluitkas wordt verdeeld over het sluitdeel van alle sluitjutters van de sluitkas, waarbij wordt verdeeld:
                    - op volgorde van afnemende strandrang,
                    - bij een even groot criterium in gelijke delen,
                    - afgerond op 0 decimalen naar beneden.
                    Als onverdeelde rest blijft de sluitrest van de sluitkas over.
            """);
        Diagnostic d = enigeFout(m);
        assertEquals("attribuut 'sluitdeel' van Jutter: '(mag opgegeven worden)', maar de verdeling 'verdeel de "
                + "sluitpremie' schrijft 'sluitdeel' — de markering verlegt een verdeling niet naar 'berekende "
                + "sluitdeel', en naast de defaults-groep op 'sluitdeel' is zij een tweede schrijver (WF-050) "
                + "(dialect §7)", d.message());
        assertEquals(7, d.line());
    }

    @Test void een_objectcreatie_die_het_doel_toekent_weigert() {
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype het Strandrecht (mv: Strandrechten)
                de grondslag Numeriek (niet-negatief getal met 2 decimalen) (mag opgegeven worden);

            Objecttype het Wrakrecht (mv: Wrakrechten)
                de bergingsvergoeding Numeriek (niet-negatief getal met 2 decimalen);

            Feittype wrakboeking
                de boeker Wrakrecht
                het geboekt strandrecht Strandrecht
            Eén boeker boekt één geboekt strandrecht

            Regel boek een strandrecht
                geldig altijd
                    Een Wrakrecht heeft een geboekt strandrecht met grondslag gelijk aan zijn bergingsvergoeding.
            """);
        Diagnostic d = enigeFout(m);
        assertEquals("attribuut 'grondslag' van Strandrecht: '(mag opgegeven worden)', maar de objectcreatie 'boek een "
                + "strandrecht' kent 'grondslag' toe bij het aanmaken — de markering verlegt die toekenning niet naar "
                + "'berekende grondslag', en naast de defaults-groep op 'grondslag' is zij een tweede schrijver "
                + "(WF-050) (dialect §7)", d.message());
        assertEquals(2, d.line());
    }

    @Test void een_schrijver_op_een_subtype_weigert() {
        RegelSpraakModel m = Resolver.resolve(VONDST + """

            Objecttype de Wrakvondst is een Vondst (mv: Wrakvondsten)
                de wrakkorting Numeriek (getal met 2 decimalen);
            """ + WAARDERING + """

            Regel wrakwaardering
                geldig altijd
                    De waarde van een Wrakvondst moet berekend worden als de taxatie van de Wrakvondst verminderd met de wrakkorting van de Wrakvondst.
            """);
        Diagnostic d = enigeFout(m);
        assertEquals("attribuut 'waarde' van Vondst: '(mag opgegeven worden)', maar regel 'wrakwaardering' schrijft "
                + "'waarde' voor Wrakvondst, een subtype van Vondst — de gegenereerde defaults-groep op Vondst zou met "
                + "de schrijvers van Wrakvondst samengaan tot één stapel per type (RUNTIME-SEMANTICS ch. 7.10), en die "
                + "samenstelling bouwt de markering niet (dialect §7)", d.message());
        assertEquals(3, d.line());
    }

    @Test void een_gegenereerde_ledennaam_die_al_gedeclareerd_is_weigert() {
        RegelSpraakModel m = Resolver.resolve(VONDST + "    de opgegeven waarde Numeriek (getal met 2 decimalen);\n"
                + WAARDERING);
        Diagnostic d = enigeFout(m);
        assertEquals("attribuut 'waarde' van Vondst (bronregel 3): '(mag opgegeven worden)' genereert het attribuut "
                + "'opgegeven waarde', en Vondst declareert die naam al als attribuut (bronregel 4) — een naam die de "
                + "markering genereert, bestaat nog niet in de erflijn van Vondst (dialect §7)", d.message());
        assertEquals(3, d.line());
    }

    @Test void een_gegenereerde_vormnaam_die_een_regel_al_draagt_weigert() {
        RegelSpraakModel m = Resolver.resolve(VONDST.replace("(mv: Vondsten)\n",
                "(mv: Vondsten)\n    de heffing Numeriek (getal met 2 decimalen);\n") + WAARDERING + """

            Regel Afwijking opgave waarde
                geldig altijd
                    De heffing van een Vondst moet gesteld worden op 1,00.
            """);
        Diagnostic d = enigeFout(m);
        assertEquals("attribuut 'waarde' van Vondst (bronregel 4): '(mag opgegeven worden)' genereert de regel "
                + "'afwijking opgave waarde', en regel 'Afwijking opgave waarde' heet al zo (bronregel 10) — een "
                + "vormnaam is uniek in het hele model, hoofdletters niet meegeteld (WF-020) (dialect §7)", d.message());
        assertEquals(4, d.line());
    }

    @Test void twee_markeringen_op_een_naam_in_twee_objecttypen_botsen() {
        RegelSpraakModel m = Resolver.resolve(VONDST + """

            Objecttype het Wrak (mv: Wrakken)
                de taxatie Numeriek (getal met 2 decimalen);
                de waarde Numeriek (getal met 2 decimalen) (mag opgegeven worden);
            """ + WAARDERING + """

            Regel wrakwaardering
                geldig altijd
                    De waarde van een Wrak moet berekend worden als de taxatie van het Wrak verminderd met 9,00.
            """);
        Diagnostic d = enigeFout(m);
        assertEquals("attribuut 'waarde' van Wrak (bronregel 7): '(mag opgegeven worden)' genereert de defaults-groep "
                + "'opgave boven berekening waarde', en de markering op attribuut 'waarde' van Vondst genereert die "
                + "naam al (bronregel 3) — een vormnaam is uniek in het hele model, hoofdletters niet meegeteld "
                + "(WF-020) (dialect §7)", d.message());
        assertEquals(7, d.line());
    }

    @Test void een_regel_die_een_gegenereerd_lid_schrijft_weigert() {
        RegelSpraakModel m = Resolver.resolve(VONDST + WAARDERING + """

            Regel directe berekening
                geldig altijd
                    De berekende waarde van een Vondst moet gesteld worden op 7,00.
            """);
        Diagnostic d = enigeFout(m);
        assertEquals("attribuut 'waarde' van Vondst (bronregel 3): regel 'directe berekening' schrijft 'berekende "
                + "waarde' (bronregel 11), een lid dat '(mag opgegeven worden)' genereert — 'opgegeven waarde' levert "
                + "de casus, 'berekende waarde' krijgt de schrijvers van 'waarde', en 'afwijkende opgave waarde' kent "
                + "de regel 'afwijking opgave waarde' toe; een andere schrijver verdraagt zo'n lid niet (dialect §7)",
                d.message());
        assertEquals(3, d.line());
    }

    @Test void twee_initialisaties_van_het_doel_weigeren() {
        RegelSpraakModel m = Resolver.resolve(VONDST + WAARDERING + STANDAARDWAARDE + """

            Regel tweede standaardwaarde
                geldig altijd
                    De waarde van een Vondst moet geïnitialiseerd worden op 30,00.
            """);
        Diagnostic d = enigeFout(m);
        assertEquals("attribuut 'waarde' van Vondst: '(mag opgegeven worden)', en 'waarde' heeft meer dan één "
                + "initialisatie ('standaardwaarde' en 'tweede standaardwaarde') — de gegenereerde defaults-groep draagt "
                + "één initialisatiedefault, de laagste (WF-050) (dialect §7)", d.message());
        assertEquals(3, d.line());
    }

    @Test void een_initialisatie_met_een_venster_weigert() {
        RegelSpraakModel m = Resolver.resolve(VONDST + WAARDERING
                + STANDAARDWAARDE.replace("geldig altijd", "geldig vanaf 2024"));
        Diagnostic d = enigeFout(m);
        assertEquals("attribuut 'waarde' van Vondst: '(mag opgegeven worden)', en de initialisatie 'standaardwaarde' "
                + "van 'waarde' heeft een ander geldigheidsvenster dan altijd — de gegenereerde defaults-groep geldt "
                + "altijd, en een default draagt geen eigen venster (dialect §7)", d.message());
        assertEquals(3, d.line());
    }

    // -------------------------------------------------------------------- the warning: nothing to put the opgave above

    @Test void zonder_rekenende_schrijver_waarschuwt_de_markering_en_genereert_niets() {
        RegelSpraakModel m = Resolver.resolve(VONDST + STANDAARDWAARDE);
        Diagnostic d = enigeDiagnose(m);
        assertEquals(Severity.WARNING, d.severity());
        assertEquals("attribuut 'waarde' van Vondst: '(mag opgegeven worden)' staat op een attribuut dat geen regel "
                + "berekent of vaststelt — 'waarde' is al invoer, en de markering genereert niets (dialect §7)",
                d.message());
        assertEquals(3, d.line());
        Objecttype v = m.objecttypen.get("Vondst");
        assertEquals(List.of("taxatie", "waarde"), List.copyOf(v.attributen.keySet()));
        assertTrue(v.kenmerken.isEmpty());
        assertTrue(m.prioritizedDefaults.isEmpty());
        assertTrue(m.consistenties.isEmpty());
        assertEquals("waarde", doelVan(regel(m, "standaardwaarde")), "de initialisatie blijft een gewone regel");
        Verslag.verantwoord(IrEmitter.emit(m));
    }

    @Test void zonder_rekenende_schrijver_weigert_een_lezer_van_een_gegenereerde_naam() {
        RegelSpraakModel m = Resolver.resolve(VONDST.replace("(mv: Vondsten)\n",
                "(mv: Vondsten)\n    de korting Numeriek (getal met 2 decimalen);\n") + """

            Regel korting
                geldig altijd
                    De korting van een Vondst moet berekend worden als de taxatie van de Vondst min zijn berekende waarde.
            """);
        Diagnostic d = enigeFout(m);
        assertEquals("attribuut 'waarde' van Vondst: '(mag opgegeven worden)' genereert niets, omdat geen regel "
                + "'waarde' berekent of vaststelt, maar het model leest 'berekende waarde' — die naam bestaat alleen "
                + "waar de markering een berekening van 'waarde' verlegt (dialect §7)", d.message());
        assertEquals(4, d.line());
    }
}
