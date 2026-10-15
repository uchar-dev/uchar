package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.ir.Spelling;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Benoembaar;
import dev.uchar.regelspraak.model.RegelSpraakModel.GegenereerdUit;
import dev.uchar.regelspraak.model.RegelSpraakModel.Herkomstvorm;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The origin on the wire (IR-SPEC ch. 2.13): on every declaration the frontend made itself
 * ({@link RegelSpraakModel#gegenereerd}) the emitter writes the field {@code origin} just before {@code designations} —
 * the form in its wire spelling, and the declaration it comes from by kind, owner and name — and on no other. A
 * decision table, which the model records as a form because it is no element of the model, is the kind
 * {@code decisionTable} on the wire. The field adds itself and nothing else: the model with its table cleared emits the
 * same document without it, byte for byte. Measured before this card: the emitter wrote no origin anywhere.
 */
class HerkomstOpDeDraadTest {

    private static final Path GOLDEN = Path.of("..", "..", "corpus", "golden");

    private static RegelSpraakModel schoon(String bron) {
        RegelSpraakModel m = Resolver.resolve(bron);
        assertEquals(List.of(), m.errors().stream().map(RegelSpraakModel.Diagnostic::toString).toList());
        return m;
    }

    /** Every origin in the emitted document, as "<node> <owner.>name: <form> <- <kind> <owner.>name", in document
     *  order; the owner of a member is the object type or fact type it stands in. */
    private static List<String> herkomsten(String ir) {
        List<String> uit = new ArrayList<>();
        loop(Verslag.json(ir), null, uit);
        return uit;
    }

    private static void loop(Object x, String eigenaar, List<String> uit) {
        if (x instanceof Map<?, ?> m) {
            if (m.get("origin") instanceof Map<?, ?> o) {
                Map<?, ?> van = (Map<?, ?>) o.get("from");
                boolean lid = "Attribute".equals(m.get("node")) || "Characteristic".equals(m.get("node"))
                        || "Role".equals(m.get("node"));
                uit.add(m.get("node") + " " + (lid ? eigenaar + "." : "") + m.get("name") + ": " + o.get("form")
                        + " <- " + van.get("kind") + " " + (van.containsKey("owner") ? van.get("owner") + "." : "")
                        + van.get("name"));
            }
            String binnen = "ObjectType".equals(m.get("node")) || "FactType".equals(m.get("node"))
                    ? (String) m.get("name") : eigenaar;
            for (Object v : m.values()) loop(v, binnen, uit);
        } else if (x instanceof List<?> l) {
            for (Object v : l) loop(v, eigenaar, uit);
        }
    }

    /** The emission of {@code m} holds one origin per entry of its table, no more. */
    private static List<String> emissie(RegelSpraakModel m) {
        List<String> h = herkomsten(IrEmitter.emit(m));
        assertEquals(m.gegenereerd.size(), h.size(), () -> "een origin per gegenereerd element: " + h);
        return h;
    }

    // ---------------- every form on the wire ----------------

    @Test void de_opgave_familie_komt_uit_het_gemarkeerde_attribuut() {
        RegelSpraakModel m = schoon("""
            Objecttype de Vondst (mv: Vondsten)
                de taxatie Numeriek (getal met 2 decimalen);
                de waarde Numeriek (getal met 2 decimalen) (mag opgegeven worden);

            Regel waardering
                geldig altijd
                    De waarde van een Vondst moet berekend worden als de taxatie van de Vondst verminderd met 5,00.
            """);
        assertEquals(List.of(
                "Attribute Vondst.opgegeven waarde: inputAboveComputation <- attribute Vondst.waarde",
                "Attribute Vondst.berekende waarde: inputAboveComputation <- attribute Vondst.waarde",
                "Characteristic Vondst.afwijkende opgave waarde: inputAboveComputation <- attribute Vondst.waarde",
                "Rule afwijking opgave waarde: inputAboveComputation <- attribute Vondst.waarde",
                "Consistency controle opgave waarde: inputAboveComputation <- attribute Vondst.waarde",
                "PrioritizedDefaults opgave boven berekening waarde: inputAboveComputation <- attribute Vondst.waarde"),
                emissie(m));
    }

    @Test void de_groep_van_een_subtype_komt_uit_de_basisregel_en_de_basisgroep_draagt_geen_origin() {
        RegelSpraakModel m = schoon("""
            Eenheidsysteem Tijd
                de dag dg
                de maand mnd

            Objecttype de Jutter (mv: Jutters) (bezield)
                de vinddag Datum in dagen;
                de meldtermijn Datum in dagen;

            Objecttype de Strandjutter is een Jutter (mv: Strandjutters)
                het vergunningseinde Datum in dagen;

            Regel meldtermijn
                geldig altijd
                    De meldtermijn van een Jutter moet berekend worden als zijn vinddag plus 6 mnd
                    indien zijn vinddag gevuld is.

            Afwijking van meldtermijn: meldtermijn tot het vergunningseinde
                geldig altijd
                    De meldtermijn van een Strandjutter moet gesteld worden op zijn vergunningseinde
                    indien zijn vergunningseinde gevuld is.
            """);
        // the group 'meldtermijn' carries the name of the base rule the author wrote, and no origin
        assertEquals(List.of(
                "PrioritizedDefaults meldtermijn voor Strandjutter: exceptionForSubtype <- namedForm meldtermijn"),
                emissie(m));
    }

    @Test void een_rij_van_een_beslistabel_komt_uit_een_tabel_en_niet_uit_een_vorm() {
        RegelSpraakModel m = schoon("""
            Objecttype de Jutter (mv: Jutters) (bezield)
                de bonus Numeriek (geheel getal);
                het aantal geborgen wrakken Numeriek (niet-negatief geheel getal);

            Beslistabel Bonustabel
                geldig altijd
            | | de bonus van een Jutter moet gesteld worden op | indien zijn aantal geborgen wrakken groter is dan |
            |---|---|---|
            | 1 | 10 | 5 |
            | 2 | 20 | 10 |
            """);
        assertEquals(List.of(
                "Rule Bonustabel 1: decisionTableRow <- decisionTable Bonustabel",
                "Rule Bonustabel 2: decisionTableRow <- decisionTable Bonustabel"),
                emissie(m));
    }

    /** A rule may carry the name of a decision table, which is no form: with the kind namedForm the rows would name
     *  that rule as their source. */
    @Test void een_regel_met_de_naam_van_de_tabel_is_niet_de_bron_van_haar_rijen() {
        RegelSpraakModel m = schoon("""
            Objecttype de Jutter (mv: Jutters) (bezield)
                de bonus Numeriek (geheel getal);
                de premie Numeriek (geheel getal);
                het aantal geborgen wrakken Numeriek (niet-negatief geheel getal);

            Regel Bonustabel
                geldig altijd
                    De premie van een Jutter moet berekend worden als zijn aantal geborgen wrakken maal 2.

            Beslistabel Bonustabel
                geldig altijd
            | | de bonus van een Jutter moet gesteld worden op | indien zijn aantal geborgen wrakken groter is dan |
            |---|---|---|
            | 1 | 10 | 5 |
            | 2 | 20 | 10 |
            """);
        assertTrue(m.benoembaar().contains(Benoembaar.vorm("Bonustabel")));
        assertEquals(List.of(
                "Rule Bonustabel 1: decisionTableRow <- decisionTable Bonustabel",
                "Rule Bonustabel 2: decisionTableRow <- decisionTable Bonustabel"),
                emissie(m));
    }

    @Test void het_lid_van_een_vouw_met_stappen_komt_uit_het_attribuut_op_het_type_dat_het_declareert() {
        RegelSpraakModel m = schoon("""
            Objecttype de Ploeg (mv: Ploegen)
                het zwaarste gewicht Numeriek (geheel getal);

            Objecttype de Jutter (mv: Jutters) (bezield)
                de startdag Datum in dagen;

            Objecttype de Vondst (mv: Vondsten)
                de vinddag Datum in dagen;

            Objecttype de Meting (mv: Metingen)
                het gewicht Numeriek (geheel getal);

            Objecttype de Weging is een Meting (mv: Wegingen)
                de weegdag Datum in dagen;

            Feittype PloegHeeftJutter
                de ploeg Ploeg
                de jutter (mv: jutters) Jutter
            Eén ploeg heeft meerdere jutters

            Feittype JutterHeeftVondst
                de jutter Jutter
                de vondst Vondst
            Eén jutter heeft één vondst

            Feittype VondstHeeftWeging
                de vondst Vondst
                de weging Weging
            Eén vondst heeft één weging

            Regel zwaarste gewicht
                geldig altijd
                    Het zwaarste gewicht van een Ploeg moet berekend worden als de maximale waarde van het gewicht van de weging van de vondst van alle jutters van de Ploeg.
            """);
        assertEquals(List.of(
                "Attribute Jutter.gewicht via vondst en weging: stepsAfterSet <- attribute Meting.gewicht",
                "Rule gewicht via vondst en weging: stepsAfterSet <- attribute Meting.gewicht"),
                emissie(m));
    }

    private static final String STARTPUNT = """
        Tijdlijn het penningjaar heeft tijdvakken van een jaar met variabel startpunt;

        Objecttype de Jutter (mv: Jutters) (bezield)
            de aanvangsdatum Datum in dagen;
        %s
        Regel penningjaar van een jutter
            geldig altijd
                Het startpunt van het penningjaar voor een Jutter wordt bepaald door zijn aanvangsdatum.
        """;

    @Test void het_anker_van_een_startpuntbepaling_komt_uit_de_regel_en_een_geschreven_anker_draagt_geen_origin() {
        assertEquals(List.of(
                "Attribute Jutter.startpunt penningjaar: startPoint <- namedForm penningjaar van een jutter"),
                emissie(schoon(STARTPUNT.formatted(""))));
        RegelSpraakModel eigen = schoon(STARTPUNT.formatted("    het startpunt penningjaar Datum in dagen;\n"));
        assertEquals(List.of(), emissie(eigen));
    }

    // ---------------- the place of the field ----------------

    /** The origin stands just before the designations, which stay the last field of their node. */
    @Test void origin_staat_vlak_voor_designations() {
        RegelSpraakModel m = schoon("""
            Vocabulaire handboek

            Objecttype de Vondst (mv: Vondsten)
                de taxatie Numeriek (getal met 2 decimalen);
                de waarde (handboek: getaxeerde waarde) Numeriek (getal met 2 decimalen) (mag opgegeven worden);

            Regel waardering
                geldig altijd
                    De waarde van een Vondst moet berekend worden als de taxatie van de Vondst verminderd met 5,00.
            """);
        List<List<String>> sleutels = new ArrayList<>();
        sleutelsMetOrigin(Verslag.json(IrEmitter.emit(m)), sleutels);
        assertEquals(6, sleutels.size());
        for (List<String> k : sleutels)
            assertEquals(List.of("origin", "designations"), k.subList(k.size() - 2, k.size()), k::toString);
    }

    private static void sleutelsMetOrigin(Object x, List<List<String>> uit) {
        if (x instanceof Map<?, ?> m) {
            if (m.containsKey("origin")) uit.add(m.keySet().stream().map(String::valueOf).toList());
            for (Object v : m.values()) sleutelsMetOrigin(v, uit);
        } else if (x instanceof List<?> l) {
            for (Object v : l) sleutelsMetOrigin(v, uit);
        }
    }

    // ---------------- inertness: the field adds itself and nothing else ----------------

    /** An origin record as the emitter writes it, with the comma that joins it to the field before it. */
    private static final Pattern ORIGIN = Pattern.compile(",\\n *\"origin\": \\{\\n.*?\\n *\\}\\n *\\}", Pattern.DOTALL);

    /** Over every rgs golden whose model has a made declaration: the model with its table cleared emits the golden's
     *  document with every origin taken out, byte for byte, and no origin. A golden without one emits none, which gate
     *  10 holds byte for byte against the committed document. */
    @Test void zonder_de_tabel_is_het_document_hetzelfde_zonder_origin() throws Exception {
        int met = 0, zonder = 0;
        try (var mappen = Files.list(GOLDEN)) {
            for (Path map : mappen.sorted().toList()) {
                Path bron = map.resolve("bron.rgs");
                if (!Files.exists(bron)) continue;
                RegelSpraakModel m = Resolver.resolve(Files.readString(bron));
                assertTrue(m.isOk(), () -> map + " resolvet niet: " + m.diagnostics);
                String ir = IrEmitter.emit(m);
                if (m.gegenereerd.isEmpty()) {
                    assertFalse(ir.contains("\"origin\""), map::toString);
                    zonder++;
                    continue;
                }
                met++;
                assertEquals(m.gegenereerd.size(), herkomsten(ir).size(), map::toString);
                m.gegenereerd.clear();
                String kaal = IrEmitter.emit(m);
                assertFalse(kaal.contains("\"origin\""), map::toString);
                assertEquals(ORIGIN.matcher(ir).replaceAll(""), kaal, map::toString);
            }
        }
        assertTrue(met >= 9, "rgs-goldens met een gemaakte declaratie: " + met);
        assertTrue(zonder >= 100, "rgs-goldens zonder: " + zonder);
    }

    // ---------------- the spelling, and the schema's enums ----------------

    @Test void de_spelling_van_vorm_en_soort_is_die_van_het_schema() throws Exception {
        Map<?, ?> defs = (Map<?, ?>) ((Map<?, ?>) Verslag.json(Files.readString(
                Path.of("..", "..", "spec", "schema", "uchar-ir-v1.schema.json")))).get("$defs");
        List<?> vormen = (List<?>) ((Map<?, ?>) ((Map<?, ?>) ((Map<?, ?>) defs.get("Origin")).get("properties"))
                .get("form")).get("enum");
        List<?> soorten = (List<?>) ((Map<?, ?>) ((Map<?, ?>) ((Map<?, ?>) defs.get("OriginSource")).get("properties"))
                .get("kind")).get("enum");
        assertEquals(vormen, List.copyOf(Spelling.ORIGIN_FORM.rijen().values()));
        List<String> java = new ArrayList<>(Spelling.ORIGIN_KIND.rijen().values());
        java.add(Spelling.ORIGIN_DECISION_TABLE);
        assertEquals(soorten, java);
        // the five forms of the frontend have a spelling; the backend's two manifest keys none
        for (Herkomstvorm v : Herkomstvorm.values()) {
            boolean frontend = v != Herkomstvorm.DEFAULTSSTAPEL && v != Herkomstvorm.VERSIE;
            assertEquals(frontend, Spelling.ORIGIN_FORM.rijen().containsKey(v), v::toString);
            if (!frontend) assertThrows(IllegalArgumentException.class, () -> Spelling.ORIGIN_FORM.spelling(v));
        }
        for (Benoembaar.Soort s : Benoembaar.Soort.values())
            assertEquals(s, Spelling.originSoort(Spelling.ORIGIN_KIND.spelling(s)));
        // a decision table is a form in the model and its own kind on the wire
        assertEquals(Benoembaar.Soort.VORM, Spelling.originSoort("decisionTable"));
        assertEquals("decisionTable", Spelling.originKind(
                new GegenereerdUit(Benoembaar.vorm("Bonustabel"), Herkomstvorm.BESLISTABELRIJ)));
        assertEquals("namedForm", Spelling.originKind(
                new GegenereerdUit(Benoembaar.vorm("meldtermijn"), Herkomstvorm.UITZONDERING_VOOR_SUBTYPE)));
        assertNull(Spelling.originSoort("form"));
        assertNull(Spelling.originSoort("VORM"));
    }
}
