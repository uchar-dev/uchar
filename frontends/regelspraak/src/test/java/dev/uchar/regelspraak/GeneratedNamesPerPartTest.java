package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Designatable;
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
class GeneratedNamesPerPartTest {

    private static RegelSpraakModel clean(String source) {
        RegelSpraakModel m = Resolver.resolve(source);
        assertEquals(List.of(), m.errors().stream().map(Diagnostic::toString).toList());
        return m;
    }

    /** The designations the IR writes as the last field of the node {@code node} named {@code naam}, each
     *  "vocabulary: designation", in written order; empty where the node carries none. */
    private static List<String> onTheWire(String ir, String node, String name) {
        String[] r = ir.split("\n", -1);
        for (int i = 1; i + 1 < r.length; i++) {
            if (!r[i].trim().equals("\"node\": \"" + node + "\",")
                    || !r[i + 1].trim().equals("\"name\": \"" + name + "\",")) continue;
            String indent = r[i].substring(0, r[i].indexOf('"'));
            String closingLine = indent.substring(2) + "}";
            List<String> result = new ArrayList<>();
            String vocabulary = null;
            boolean within = false;
            for (int j = i; j < r.length && !r[j].equals(closingLine) && !r[j].equals(closingLine + ","); j++) {
                String t = r[j].trim();
                if (r[j].equals(indent + "\"designations\": [")) within = true;
                else if (!within) continue;
                else if (t.startsWith("\"vocabulary\": ")) vocabulary = t.substring(15, t.length() - 2);
                else if (t.startsWith("\"designation\": ")) result.add(vocabulary + ": " + t.substring(16, t.length() - 1));
            }
            return result;
        }
        throw new AssertionError("geen knoop " + node + " '" + name + "' in\n" + ir);
    }

    /** The ERROR {@code fragment} stands on the first line of {@code source} that carries {@code merker}. */
    private static void error(String source, String merker, String fragment) {
        RegelSpraakModel m = Resolver.resolve(source);
        String[] rules = source.split("\n", -1);
        int rule = 0;
        for (int i = 0; i < rules.length; i++) if (rules[i].contains(merker)) { rule = i + 1; break; }
        assertTrue(rule > 0, "merker '" + merker + "' niet in de bron");
        final int r = rule;
        List<Diagnostic> faults = m.errors();
        assertTrue(faults.stream().anyMatch(d -> d.message().contains(fragment) && d.line() == r),
                () -> "verwacht op regel " + r + ": " + fragment + "\nkreeg: " + faults);
    }

    private static String designation(RegelSpraakModel m, String vocabulary, Designatable element) {
        return m.designation(vocabulary, element).orElse("-");
    }

    // --- the group 'B voor S' ------------------------------------------------------------------------

    /** The subtype S's designations first, then the base rule B's. */
    private static final String GROUP = """
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

    private static final Designatable THE_GROUP = Designatable.form("meldtermijn voor Strandjutter");

    // --- the member and rule '<lid> via <rol 1> en <rol 2>' -----------------------------------------------

    /** A member line of Jutter, then the designations of the member 'gewicht', of the role 'vondst' of the first
     *  step and of the role 'weging' of the second. The role 'vondst' of the second fact type carries designations
     *  of its own: the part is the role the first step binds. */
    private static final String FOLD = """
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

    private static final Designatable THE_MEMBER = Designatable.attribute("Jutter", "gewicht via vondst en weging");
    private static final Designatable THE_RULE = Designatable.form("gewicht via vondst en weging");

    // --- the four cases ---------------------------------------------------------------------------------------

    @Test void all_parts_with_a_designation() {
        RegelSpraakModel g = clean(GROUP.formatted("(handboek: strandloper) (cgm: STRANDJUTTER)",
                "(handboek: termijn van melding) (cgm: MELDTERMIJN)"));
        assertTrue(g.generatedFrom(THE_GROUP).isPresent(), () -> g.generated.toString());
        assertEquals("termijn van melding voor strandloper", designation(g, "handboek", THE_GROUP));
        assertEquals("MELDTERMIJN voor STRANDJUTTER", designation(g, "cgm", THE_GROUP));
        // the way back from the composed designation finds the group
        assertEquals(THE_GROUP, g.namedElement("handboek", Designatable.Kind.VORM, null,
                "Termijn van melding voor strandloper").orElseThrow());
        assertEquals(List.of("handboek: termijn van melding voor strandloper", "cgm: MELDTERMIJN voor STRANDJUTTER"),
                onTheWire(IrEmitter.emit(g), "PrioritizedDefaults", "meldtermijn voor Strandjutter"));

        RegelSpraakModel v = clean(FOLD.formatted("", "(handboek: massa) (cgm: GEWICHT)",
                "(handboek: vangst) (cgm: VONDST)", "(handboek: meting) (cgm: WEGING)"));
        assertTrue(v.generatedFrom(THE_MEMBER).isPresent(), () -> v.generated.toString());
        for (Designatable e : List.of(THE_MEMBER, THE_RULE)) {
            assertEquals("massa via vangst en meting", designation(v, "handboek", e), e::toString);
            assertEquals("GEWICHT via VONDST en WEGING", designation(v, "cgm", e), e::toString);
        }
        assertEquals(THE_MEMBER, v.namedElement("handboek", Designatable.Kind.ATTRIBUUT, "Jutter", "Massa via vangst en meting")
                .orElseThrow());
        String ir = IrEmitter.emit(v);
        for (String node : List.of("Attribute", "Rule"))
            assertEquals(List.of("handboek: massa via vangst en meting", "cgm: GEWICHT via VONDST en WEGING"),
                    onTheWire(ir, node, "gewicht via vondst en weging"), node);
    }

    @Test void a_part_without_designation_stays_canonical() {
        // S carries a designation in cgm only, B in handboek only
        RegelSpraakModel g = clean(GROUP.formatted("(cgm: STRANDJUTTER)", "(handboek: termijn van melding)"));
        assertEquals("termijn van melding voor Strandjutter", designation(g, "handboek", THE_GROUP));
        assertEquals("meldtermijn voor STRANDJUTTER", designation(g, "cgm", THE_GROUP));
        assertEquals(List.of("handboek: termijn van melding voor Strandjutter", "cgm: meldtermijn voor STRANDJUTTER"),
                onTheWire(IrEmitter.emit(g), "PrioritizedDefaults", "meldtermijn voor Strandjutter"));

        // the second role carries none; the member none in cgm
        RegelSpraakModel v = clean(FOLD.formatted("", "(handboek: massa)", "(handboek: vangst) (cgm: VONDST)", ""));
        for (Designatable e : List.of(THE_MEMBER, THE_RULE)) {
            assertEquals("massa via vangst en weging", designation(v, "handboek", e), e::toString);
            assertEquals("gewicht via VONDST en weging", designation(v, "cgm", e), e::toString);
        }
        // only the member carries one, as in the maintainer's example
        RegelSpraakModel member = clean(FOLD.formatted("", "(handboek: Bedrag gewicht)", "", ""));
        for (Designatable e : List.of(THE_MEMBER, THE_RULE)) {
            assertEquals("Bedrag gewicht via vondst en weging", designation(member, "handboek", e), e::toString);
            assertEquals("-", designation(member, "cgm", e), e::toString);
        }
        String ir = IrEmitter.emit(member);
        for (String node : List.of("Attribute", "Rule"))
            assertEquals(List.of("handboek: Bedrag gewicht via vondst en weging"),
                    onTheWire(ir, node, "gewicht via vondst en weging"), node);
    }

    @Test void without_designation_of_any_part_the_name_has_none() {
        RegelSpraakModel g = clean(GROUP.formatted("", ""));
        assertEquals(List.of("-", "-"), List.of(designation(g, "handboek", THE_GROUP), designation(g, "cgm", THE_GROUP)));
        assertEquals("meldtermijn voor Strandjutter", g.effectiveName("handboek", THE_GROUP));
        assertEquals(List.of(), onTheWire(IrEmitter.emit(g), "PrioritizedDefaults", "meldtermijn voor Strandjutter"));

        RegelSpraakModel v = clean(FOLD.formatted("", "", "", ""));
        for (Designatable e : List.of(THE_MEMBER, THE_RULE)) {
            assertEquals("-", designation(v, "handboek", e), e::toString);
            assertEquals("-", designation(v, "cgm", e), e::toString);
            assertEquals("gewicht via vondst en weging", v.effectiveName("cgm", e));
        }
        String ir = IrEmitter.emit(v);
        for (String node : List.of("Attribute", "Rule"))
            assertEquals(List.of(), onTheWire(ir, node, "gewicht via vondst en weging"), node);

        // a designation that spells its part's canonical name composes the canonical name, which is not written in a
        // vocabulary that is not the default (WF-120); the part's own designation is warned once
        RegelSpraakModel redundant = clean(GROUP.formatted("", "(handboek: meldtermijn)"));
        assertEquals("-", designation(redundant, "handboek", THE_GROUP));
        assertEquals(1, redundant.diagnostics.stream().filter(d -> d.message().endsWith("(WF-120)")).count(),
                redundant.diagnostics::toString);
        RegelSpraakModel redundantMember = clean(FOLD.formatted("", "(handboek: gewicht)", "", ""));
        assertEquals("-", designation(redundantMember, "handboek", THE_MEMBER));
    }

    @Test void a_collision_under_WF_118_names_the_parts() {
        // the group composes 'meldtermijn voor strandloper' in handboek, the canonical name of a written rule
        String group = GROUP.formatted("(handboek: strandloper)", "") + """

            Regel meldtermijn voor strandloper
                geldig altijd
                    De controledag van een Jutter moet gesteld worden op zijn vinddag.
            """;
        error(group, "Regel meldtermijn ", "De regel 'meldtermijn voor strandloper' en de regel 'meldtermijn voor "
                + "Strandjutter' hebben in het vocabulaire 'handboek' dezelfde effectieve naam 'meldtermijn voor "
                + "strandloper'; binnen hun reikwijdte is die uniek, hoofdletterongevoelig, zodat de weg terug naar de "
                + "declaratie één antwoord heeft (WF-118); de frontend stelde de benaming van de regel 'meldtermijn voor "
                + "Strandjutter' samen uit de benaming 'strandloper' van het objecttype 'Strandjutter'");
        assertEquals(1, Resolver.resolve(group).errors().size(), () -> Resolver.resolve(group).errors().toString());

        // the member composes the designation a written attribute of Jutter carries
        String fold = FOLD.formatted("de vangstmassa (handboek: massa via vangst en meting) Numeriek (geheel getal);",
                "(handboek: massa)", "(handboek: vangst)", "(handboek: meting)");
        error(fold, "het gewicht (handboek: massa)", "Het attribuut 'vangstmassa' van 'Jutter' en het attribuut "
                + "'gewicht via vondst en weging' van 'Jutter' hebben in het vocabulaire 'handboek' dezelfde effectieve "
                + "naam 'massa via vangst en meting'; binnen hun reikwijdte is die uniek, hoofdletterongevoelig, zodat de "
                + "weg terug naar de declaratie één antwoord heeft (WF-118); de frontend stelde de benaming van het "
                + "attribuut 'gewicht via vondst en weging' van 'Jutter' samen uit de benaming 'massa' van het attribuut "
                + "'gewicht' van 'Weging', de benaming 'vangst' van de rol 'vondst' van 'JutterHeeftVondst' en de "
                + "benaming 'meting' van de rol 'weging' van 'VondstHeeftWeging'");
        assertEquals(1, Resolver.resolve(fold).errors().size(), () -> Resolver.resolve(fold).errors().toString());
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

    @Test void a_default_vocabulary_names_the_group_if_it_names_its_parts() {
        // until this card WF-119 refused the group here, on line 0: it had no place for an annotation
        RegelSpraakModel m = clean(STANDAARD.formatted("(cgm: STRANDJUTTER)", "(cgm: R_MELDTERMIJN)"));
        assertEquals("R_MELDTERMIJN voor STRANDJUTTER", designation(m, "cgm", THE_GROUP));

        // a part without a designation is refused itself; the group carries the mixed name, and no refusal of its own
        String mixed = STANDAARD.formatted("(cgm: STRANDJUTTER)", "");
        error(mixed, "Regel meldtermijn", "De regel 'meldtermijn' draagt geen benaming in het standaardvocabulaire "
                + "'cgm', dat elke declaratie noemt (WF-119)");
        RegelSpraakModel g = Resolver.resolve(mixed);
        assertEquals(1, g.errors().size(), g.errors()::toString);
        assertEquals("meldtermijn voor STRANDJUTTER", designation(g, "cgm", THE_GROUP));

        // no part carries one: the group is refused as well, with its parts named, on the line of B
        String plain = STANDAARD.formatted("", "");
        error(plain, "Objecttype de Strandjutter", "Het objecttype 'Strandjutter' draagt geen benaming in het "
                + "standaardvocabulaire 'cgm', dat elke declaratie noemt (WF-119)");
        error(plain, "Regel meldtermijn", "De regel 'meldtermijn voor Strandjutter' draagt geen benaming in het "
                + "standaardvocabulaire 'cgm', dat elke declaratie noemt (WF-119); de frontend stelt de benaming van "
                + "deze declaratie samen uit die van de regel 'meldtermijn' en het objecttype 'Strandjutter', en geen "
                + "daarvan draagt er een");
        assertEquals(3, Resolver.resolve(plain).errors().size(), () -> Resolver.resolve(plain).errors().toString());
    }
}
