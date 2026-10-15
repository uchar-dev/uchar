package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Benoembaar;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A generated name made of more than one declared name — the group 'B voor S' of a subtype's blocks, and the member and
 * rule '&lt;lid&gt; via &lt;rol 1&gt;, … en &lt;rol n&gt;' of a fold with steps after the set — carries, in a
 * vocabulary, its own name with each part that carries a designation there replaced by that designation, taken over
 * literally; the other parts and the fixed words stay, and where no part carries one, neither does the element
 * (grammar/DIVERGENCES.md, "Gegenereerde elementen", "Part by part"; maintainer decision 2026-10-15).
 */
class GegenereerdeNamenPerDeelTest {

    private static RegelSpraakModel schoon(String bron) {
        RegelSpraakModel m = Resolver.resolve(bron);
        assertEquals(List.of(), m.errors().stream().map(Diagnostic::toString).toList());
        return m;
    }

    /** The designations the IR writes as the last field of the node {@code node} named {@code naam}, each
     *  "vocabulary: designation", in written order; empty where the node carries none. */
    private static List<String> opDeWire(String ir, String node, String naam) {
        String[] r = ir.split("\n", -1);
        for (int i = 1; i + 1 < r.length; i++) {
            if (!r[i].trim().equals("\"node\": \"" + node + "\",")
                    || !r[i + 1].trim().equals("\"name\": \"" + naam + "\",")) continue;
            String rand = r[i].substring(0, r[i].indexOf('"'));
            String sluit = rand.substring(2) + "}";
            List<String> uit = new ArrayList<>();
            String vocabulaire = null;
            boolean binnen = false;
            for (int j = i; j < r.length && !r[j].equals(sluit) && !r[j].equals(sluit + ","); j++) {
                String t = r[j].trim();
                if (r[j].equals(rand + "\"designations\": [")) binnen = true;
                else if (!binnen) continue;
                else if (t.startsWith("\"vocabulary\": ")) vocabulaire = t.substring(15, t.length() - 2);
                else if (t.startsWith("\"designation\": ")) uit.add(vocabulaire + ": " + t.substring(16, t.length() - 1));
            }
            return uit;
        }
        throw new AssertionError("geen knoop " + node + " '" + naam + "' in\n" + ir);
    }

    /** The ERROR {@code fragment} stands on the first line of {@code bron} that carries {@code merker}. */
    private static void fout(String bron, String merker, String fragment) {
        RegelSpraakModel m = Resolver.resolve(bron);
        String[] regels = bron.split("\n", -1);
        int regel = 0;
        for (int i = 0; i < regels.length; i++) if (regels[i].contains(merker)) { regel = i + 1; break; }
        assertTrue(regel > 0, "merker '" + merker + "' niet in de bron");
        final int r = regel;
        List<Diagnostic> fouten = m.errors();
        assertTrue(fouten.stream().anyMatch(d -> d.message().contains(fragment) && d.line() == r),
                () -> "verwacht op regel " + r + ": " + fragment + "\nkreeg: " + fouten);
    }

    private static String benaming(RegelSpraakModel m, String vocabulaire, Benoembaar element) {
        return m.benaming(vocabulaire, element).orElse("-");
    }

    // --- the group 'B voor S' ------------------------------------------------------------------------

    /** The subtype S's designations first, then the base rule B's. */
    private static final String GROEP = """
        Vocabulaire handboek
        Vocabulaire cgm

        Eenheidsysteem Tijd
            de dag dg
            de maand mnd

        Objecttype de Jutter (mv: Jutters) (bezield)
            de vinddag Datum in dagen;
            de meldtermijn Datum in dagen;
            de controledag Datum in dagen;

        Objecttype de Strandjutter is een Jutter (mv: Strandjutters) %s
            het vergunningseinde Datum in dagen;

        Regel meldtermijn %s
            geldig altijd
                De meldtermijn van een Jutter moet berekend worden als zijn vinddag plus 6 mnd
                indien zijn vinddag gevuld is.

        Afwijking van meldtermijn: meldtermijn tot het vergunningseinde
            geldig altijd
                De meldtermijn van een Strandjutter moet gesteld worden op zijn vergunningseinde
                indien zijn vergunningseinde gevuld is.
        """;

    private static final Benoembaar DE_GROEP = Benoembaar.vorm("meldtermijn voor Strandjutter");

    // --- the member and rule '<lid> via <rol 1> en <rol 2>' -----------------------------------------------

    /** A member line of Jutter, then the designations of the member 'gewicht', of the role 'vondst' of the first
     *  step and of the role 'weging' of the second. The role 'vondst' of the second fact type carries designations
     *  of its own: the part is the role the first step binds. */
    private static final String VOUW = """
        Vocabulaire handboek
        Vocabulaire cgm

        Objecttype de Ploeg (mv: Ploegen)
            het zwaarste gewicht Numeriek (geheel getal);

        Objecttype de Jutter (mv: Jutters) (bezield)
            de startdag Datum in dagen;
            %s

        Objecttype de Vondst (mv: Vondsten)
            de vinddag Datum in dagen;

        Objecttype de Weging (mv: Wegingen)
            het gewicht %s Numeriek (geheel getal);

        Feittype PloegHeeftJutter
            de ploeg Ploeg
            de jutter (mv: jutters) Jutter
        Eén ploeg heeft meerdere jutters

        Feittype JutterHeeftVondst
            de jutter Jutter
            de vondst %s Vondst
        Eén jutter heeft één vondst

        Feittype VondstHeeftWeging
            de vondst (handboek: gevonden voorwerp) (cgm: GEVONDEN) Vondst
            de weging %s Weging
        Eén vondst heeft één weging

        Regel zwaarste gewicht
            geldig altijd
                Het zwaarste gewicht van een Ploeg moet berekend worden als de maximale waarde van het gewicht van de weging van de vondst van alle jutters van de Ploeg.
        """;

    private static final Benoembaar HET_LID = Benoembaar.attribuut("Jutter", "gewicht via vondst en weging");
    private static final Benoembaar DE_REGEL = Benoembaar.vorm("gewicht via vondst en weging");

    // --- the four cases ---------------------------------------------------------------------------------------

    @Test void alle_delen_met_een_benaming() {
        RegelSpraakModel g = schoon(GROEP.formatted("(handboek: strandloper) (cgm: STRANDJUTTER)",
                "(handboek: termijn van melding) (cgm: MELDTERMIJN)"));
        assertTrue(g.gegenereerdUit(DE_GROEP).isPresent(), () -> g.gegenereerd.toString());
        assertEquals("termijn van melding voor strandloper", benaming(g, "handboek", DE_GROEP));
        assertEquals("MELDTERMIJN voor STRANDJUTTER", benaming(g, "cgm", DE_GROEP));
        // the way back from the composed designation finds the group
        assertEquals(DE_GROEP, g.benoemd("handboek", Benoembaar.Soort.VORM, null,
                "Termijn van melding voor strandloper").orElseThrow());
        assertEquals(List.of("handboek: termijn van melding voor strandloper", "cgm: MELDTERMIJN voor STRANDJUTTER"),
                opDeWire(IrEmitter.emit(g), "PrioritizedDefaults", "meldtermijn voor Strandjutter"));

        RegelSpraakModel v = schoon(VOUW.formatted("", "(handboek: massa) (cgm: GEWICHT)",
                "(handboek: vangst) (cgm: VONDST)", "(handboek: meting) (cgm: WEGING)"));
        assertTrue(v.gegenereerdUit(HET_LID).isPresent(), () -> v.gegenereerd.toString());
        for (Benoembaar e : List.of(HET_LID, DE_REGEL)) {
            assertEquals("massa via vangst en meting", benaming(v, "handboek", e), e::toString);
            assertEquals("GEWICHT via VONDST en WEGING", benaming(v, "cgm", e), e::toString);
        }
        assertEquals(HET_LID, v.benoemd("handboek", Benoembaar.Soort.ATTRIBUUT, "Jutter", "Massa via vangst en meting")
                .orElseThrow());
        String ir = IrEmitter.emit(v);
        for (String node : List.of("Attribute", "Rule"))
            assertEquals(List.of("handboek: massa via vangst en meting", "cgm: GEWICHT via VONDST en WEGING"),
                    opDeWire(ir, node, "gewicht via vondst en weging"), node);
    }

    @Test void een_deel_zonder_benaming_blijft_canoniek() {
        // S carries a designation in cgm only, B in handboek only
        RegelSpraakModel g = schoon(GROEP.formatted("(cgm: STRANDJUTTER)", "(handboek: termijn van melding)"));
        assertEquals("termijn van melding voor Strandjutter", benaming(g, "handboek", DE_GROEP));
        assertEquals("meldtermijn voor STRANDJUTTER", benaming(g, "cgm", DE_GROEP));
        assertEquals(List.of("handboek: termijn van melding voor Strandjutter", "cgm: meldtermijn voor STRANDJUTTER"),
                opDeWire(IrEmitter.emit(g), "PrioritizedDefaults", "meldtermijn voor Strandjutter"));

        // the second role carries none; the member none in cgm
        RegelSpraakModel v = schoon(VOUW.formatted("", "(handboek: massa)", "(handboek: vangst) (cgm: VONDST)", ""));
        for (Benoembaar e : List.of(HET_LID, DE_REGEL)) {
            assertEquals("massa via vangst en weging", benaming(v, "handboek", e), e::toString);
            assertEquals("gewicht via VONDST en weging", benaming(v, "cgm", e), e::toString);
        }
        // only the member carries one, as in the maintainer's example
        RegelSpraakModel lid = schoon(VOUW.formatted("", "(handboek: Bedrag gewicht)", "", ""));
        for (Benoembaar e : List.of(HET_LID, DE_REGEL)) {
            assertEquals("Bedrag gewicht via vondst en weging", benaming(lid, "handboek", e), e::toString);
            assertEquals("-", benaming(lid, "cgm", e), e::toString);
        }
        String ir = IrEmitter.emit(lid);
        for (String node : List.of("Attribute", "Rule"))
            assertEquals(List.of("handboek: Bedrag gewicht via vondst en weging"),
                    opDeWire(ir, node, "gewicht via vondst en weging"), node);
    }

    @Test void zonder_benaming_van_enig_deel_heeft_de_naam_er_geen() {
        RegelSpraakModel g = schoon(GROEP.formatted("", ""));
        assertEquals(List.of("-", "-"), List.of(benaming(g, "handboek", DE_GROEP), benaming(g, "cgm", DE_GROEP)));
        assertEquals("meldtermijn voor Strandjutter", g.effectieveNaam("handboek", DE_GROEP));
        assertEquals(List.of(), opDeWire(IrEmitter.emit(g), "PrioritizedDefaults", "meldtermijn voor Strandjutter"));

        RegelSpraakModel v = schoon(VOUW.formatted("", "", "", ""));
        for (Benoembaar e : List.of(HET_LID, DE_REGEL)) {
            assertEquals("-", benaming(v, "handboek", e), e::toString);
            assertEquals("-", benaming(v, "cgm", e), e::toString);
            assertEquals("gewicht via vondst en weging", v.effectieveNaam("cgm", e));
        }
        String ir = IrEmitter.emit(v);
        for (String node : List.of("Attribute", "Rule"))
            assertEquals(List.of(), opDeWire(ir, node, "gewicht via vondst en weging"), node);

        // a designation that spells its part's canonical name composes the canonical name, which is not written in a
        // vocabulary that is not the default (WF-120); the part's own designation is warned once
        RegelSpraakModel overbodig = schoon(GROEP.formatted("", "(handboek: meldtermijn)"));
        assertEquals("-", benaming(overbodig, "handboek", DE_GROEP));
        assertEquals(1, overbodig.diagnostics.stream().filter(d -> d.message().endsWith("(WF-120)")).count(),
                overbodig.diagnostics::toString);
        RegelSpraakModel overbodigLid = schoon(VOUW.formatted("", "(handboek: gewicht)", "", ""));
        assertEquals("-", benaming(overbodigLid, "handboek", HET_LID));
    }

    @Test void een_botsing_onder_WF_118_noemt_de_delen() {
        // the group composes 'meldtermijn voor strandloper' in handboek, the canonical name of a written rule
        String groep = GROEP.formatted("(handboek: strandloper)", "") + """

            Regel meldtermijn voor strandloper
                geldig altijd
                    De controledag van een Jutter moet gesteld worden op zijn vinddag.
            """;
        fout(groep, "Regel meldtermijn ", "De regel 'meldtermijn voor strandloper' en de regel 'meldtermijn voor "
                + "Strandjutter' hebben in het vocabulaire 'handboek' dezelfde effectieve naam 'meldtermijn voor "
                + "strandloper'; binnen hun reikwijdte is die uniek, hoofdletterongevoelig, zodat de weg terug naar de "
                + "declaratie één antwoord heeft (WF-118); de frontend stelde de benaming van de regel 'meldtermijn voor "
                + "Strandjutter' samen uit de benaming 'strandloper' van het objecttype 'Strandjutter'");
        assertEquals(1, Resolver.resolve(groep).errors().size(), () -> Resolver.resolve(groep).errors().toString());

        // the member composes the designation a written attribute of Jutter carries
        String vouw = VOUW.formatted("de vangstmassa (handboek: massa via vangst en meting) Numeriek (geheel getal);",
                "(handboek: massa)", "(handboek: vangst)", "(handboek: meting)");
        fout(vouw, "het gewicht (handboek: massa)", "Het attribuut 'vangstmassa' van 'Jutter' en het attribuut "
                + "'gewicht via vondst en weging' van 'Jutter' hebben in het vocabulaire 'handboek' dezelfde effectieve "
                + "naam 'massa via vangst en meting'; binnen hun reikwijdte is die uniek, hoofdletterongevoelig, zodat de "
                + "weg terug naar de declaratie één antwoord heeft (WF-118); de frontend stelde de benaming van het "
                + "attribuut 'gewicht via vondst en weging' van 'Jutter' samen uit de benaming 'massa' van het attribuut "
                + "'gewicht' van 'Weging', de benaming 'vangst' van de rol 'vondst' van 'JutterHeeftVondst' en de "
                + "benaming 'meting' van de rol 'weging' van 'VondstHeeftWeging'");
        assertEquals(1, Resolver.resolve(vouw).errors().size(), () -> Resolver.resolve(vouw).errors().toString());
    }

    // --- a default vocabulary --------------------------------------------------------------------------------

    private static final String STANDAARD = """
        Vocabulaire cgm (standaard)

        Eenheidsysteem Tijd
            de dag dg
            de maand mnd

        Objecttype de Jutter (mv: Jutters) (bezield) (cgm: JUTTER)
            de vinddag (cgm: VINDDAG) Datum in dagen;
            de meldtermijn (cgm: MELDTERMIJN) Datum in dagen;

        Objecttype de Strandjutter is een Jutter (mv: Strandjutters) %s
            het vergunningseinde (cgm: VERGUNNINGSEINDE) Datum in dagen;

        Regel meldtermijn %s
            geldig altijd
                De meldtermijn van een Jutter moet berekend worden als zijn vinddag plus 6 mnd
                indien zijn vinddag gevuld is.

        Afwijking van meldtermijn: meldtermijn tot het vergunningseinde
            geldig altijd
                De meldtermijn van een Strandjutter moet gesteld worden op zijn vergunningseinde
                indien zijn vergunningseinde gevuld is.
        """;

    @Test void een_standaardvocabulaire_noemt_de_groep_als_het_haar_delen_noemt() {
        // until this card WF-119 refused the group here, on line 0: it had no place for an annotation
        RegelSpraakModel m = schoon(STANDAARD.formatted("(cgm: STRANDJUTTER)", "(cgm: R_MELDTERMIJN)"));
        assertEquals("R_MELDTERMIJN voor STRANDJUTTER", benaming(m, "cgm", DE_GROEP));

        // a part without a designation is refused itself; the group carries the mixed name, and no refusal of its own
        String gemengd = STANDAARD.formatted("(cgm: STRANDJUTTER)", "");
        fout(gemengd, "Regel meldtermijn", "De regel 'meldtermijn' draagt geen benaming in het standaardvocabulaire "
                + "'cgm', dat elke declaratie noemt (WF-119)");
        RegelSpraakModel g = Resolver.resolve(gemengd);
        assertEquals(1, g.errors().size(), g.errors()::toString);
        assertEquals("meldtermijn voor STRANDJUTTER", benaming(g, "cgm", DE_GROEP));

        // no part carries one: the group is refused as well, with its parts named, on the line of B
        String kaal = STANDAARD.formatted("", "");
        fout(kaal, "Objecttype de Strandjutter", "Het objecttype 'Strandjutter' draagt geen benaming in het "
                + "standaardvocabulaire 'cgm', dat elke declaratie noemt (WF-119)");
        fout(kaal, "Regel meldtermijn", "De regel 'meldtermijn voor Strandjutter' draagt geen benaming in het "
                + "standaardvocabulaire 'cgm', dat elke declaratie noemt (WF-119); de frontend stelt de benaming van "
                + "deze declaratie samen uit die van de regel 'meldtermijn' en het objecttype 'Strandjutter', en geen "
                + "daarvan draagt er een");
        assertEquals(3, Resolver.resolve(kaal).errors().size(), () -> Resolver.resolve(kaal).errors().toString());
    }
}
