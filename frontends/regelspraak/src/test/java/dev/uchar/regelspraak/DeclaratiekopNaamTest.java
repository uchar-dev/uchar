package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.CaseEmitter;
import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.ir.ResolvedScenario;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.model.RegelSpraakModel.Feittype;
import dev.uchar.regelspraak.model.RegelSpraakModel.Objecttype;
import dev.uchar.regelspraak.model.RegelSpraakModel.Rol;
import dev.uchar.regelspraak.model.RegelSpraakModel.Severity;
import dev.uchar.regelspraak.resolver.Resolver;
import dev.uchar.regelspraak.resolver.ScenarioResolver;
import dev.uchar.regelspraak.resolver.ScenarioResolver.ScenarioBundel;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Names in declaration heads (grammar/DIVERGENCES.md, "Namen in declaratiekoppen"). An object-type name carries,
 * after its first capitalised word, the prepositions rule text already reads in a name, in every position the name
 * production stands; a plural runs up to its closing parenthesis; a fact-type name carries the verbs and every other
 * one-word token but the ones that open the role line, the cardinality line or a block. A member or role that begins
 * on the line where the previous declaration ended is refused, a role's filler that is no object type is reported
 * before the emission, and a declared name that rule text cannot read back is warned. Every sentence is asserted
 * whole, on its line. Measured before this work: a head with a preposition was a syntax error where a plural
 * followed it and a silent re-reading where none did, a fact-type name with 'heeft' was a syntax error, and the
 * documents the three checks now stop were written to disk, one of them with a guess in a WARNING.
 */
class DeclaratiekopNaamTest {

    private static RegelSpraakModel schoon(String bron) {
        RegelSpraakModel m = Resolver.resolve(bron);
        assertTrue(m.diagnostics.isEmpty(), () -> "verwacht geen diagnose, kreeg: " + m.diagnostics);
        return m;
    }

    /** Exactly one diagnostic of {@code ernst} carries {@code zin}, and it stands on {@code regel}. */
    private static void enigeMelding(RegelSpraakModel m, Severity ernst, int regel, String zin) {
        List<Diagnostic> passend = m.diagnostics.stream()
                .filter(d -> d.severity() == ernst && d.message().equals(zin)).toList();
        assertEquals(1, passend.size(), () -> "verwacht één " + ernst + " '" + zin + "', kreeg: " + m.diagnostics);
        assertEquals(regel, passend.get(0).line(), () -> "de melding staat op de verkeerde regel: " + passend.get(0));
    }

    private static String ir(RegelSpraakModel m) {
        return IrEmitter.emit(m).replaceAll("\\s+", "");
    }

    private static int tel(String tekst, String deel) {
        int n = 0;
        for (int i = tekst.indexOf(deel); i >= 0; i = tekst.indexOf(deel, i + 1)) n++;
        return n;
    }

    // ---------------- the object-type name ----------------

    @Test void een_typenaam_draagt_de_voorzetsels_van_regeltekst() {
        RegelSpraakModel m = schoon("""
            Objecttype het Recht op premie (mv: Rechten op premie)
                de premie Numeriek (geheel getal);
            Objecttype de Vondst bij eb
                de waarde Numeriek (geheel getal);
            """);
        assertEquals(List.of("Recht op premie", "Vondst bij eb"), new ArrayList<>(m.objecttypen.keySet()));
        assertEquals("Rechten op premie", m.objecttypen.get("Recht op premie").meervoud);
        assertEquals(List.of("premie"), new ArrayList<>(m.objecttypen.get("Recht op premie").attributen.keySet()));
        // until this work the head read 'Vondst' and one attribute 'bij eb de waarde', without a diagnostic
        assertEquals(List.of("waarde"), new ArrayList<>(m.objecttypen.get("Vondst bij eb").attributen.keySet()));
        for (String voorzetsel : List.of("op", "bij", "in", "voor", "over", "tot", "tot en met", "per", "met")) {
            RegelSpraakModel n = schoon("Objecttype de Premie " + voorzetsel + " eb\n    de waarde Numeriek (geheel getal);\n");
            assertEquals(List.of("Premie " + voorzetsel + " eb"), new ArrayList<>(n.objecttypen.keySet()), voorzetsel);
        }
    }

    /** 'van' separates the navigation of rule text and stays out of the name, as the articles and the other keywords
     *  do; the line check refuses what the head leaves behind, and the hyphen it names reads in the head and in rule
     *  text alike. */
    @Test void van_blijft_buiten_de_typenaam_en_een_koppelteken_leest_wel() {
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype de Vondst van eb
                de waarde Numeriek (geheel getal);
            """);
        enigeMelding(m, Severity.ERROR, 1, "objecttype 'Vondst': het attribuut 'van eb de waarde' begint met 'van' op "
                + "de regel waar de kop eindigt, direct na 'Vondst'; een lid begint op een eigen regel — hoort 'van' bij "
                + "de naam, verbind het dan met een koppelteken aan het woord ervoor ('Vondst-van')");
        RegelSpraakModel k = schoon("""
            Objecttype de Vondst-van eb (mv: Vondsten-van eb)
                de waarde Numeriek (geheel getal);
                de premie Numeriek (geheel getal);
            Regel premie
                geldig altijd
                    De premie van een Vondst-van eb moet berekend worden als de waarde van de Vondst-van eb plus 1.
            """);
        assertEquals(List.of("Vondst-van eb"), new ArrayList<>(k.objecttypen.keySet()));
        String ir = ir(k);
        assertEquals(0, tel(ir, "\"node\":\"Unresolved\""), ir);
        assertEquals(2, tel(ir, "\"subjectType\":\"Vondst-vaneb\""), ir);
    }

    /** One model, one name: 'Recht op premie' as a head, an extension head, the parent of an 'is een' clause, a role's
     *  filler, the subject of a rule target, a reading and a condition, and the type of a scenario instance. */
    @Test void recht_op_premie_werkt_op_elke_plaats() {
        String bron = """
            Objecttype het Recht op premie (mv: Rechten op premie)
                het vondstgewicht Numeriek (geheel getal);
                de premie Numeriek (geheel getal);
            Extensie van objecttype het Recht op premie
                is toegekend kenmerk (bijvoeglijk);
            Objecttype het Recht op premie bij eb is een Recht op premie (mv: Rechten op premie bij eb)
                de toeslag Numeriek (geheel getal);
            Objecttype de Jutter (mv: Jutters) (bezield)
                het premiebedrag Numeriek (geheel getal);
            Feittype jutter heeft premierecht
                de rechthebbende Jutter
                het recht op premie (mv: rechten op premie) Recht op premie
            Eén rechthebbende heeft meerdere rechten op premie
            Regel premie van een recht op premie
                geldig altijd
                    De premie van een Recht op premie moet berekend worden als het vondstgewicht van het Recht op premie maal 5
                    indien het vondstgewicht van het Recht op premie groter is dan 2.
            Regel toekenning van een recht op premie
                geldig altijd
                    Een Recht op premie is toegekend indien de premie van het Recht op premie groter is dan 10.
            Regel premiebedrag van een jutter
                geldig altijd
                    Het premiebedrag van een Jutter moet berekend worden als de som van de premie van alle rechten op premie van de Jutter of 0 als die er niet zijn.
            """;
        RegelSpraakModel m = schoon(bron);
        Objecttype recht = m.objecttypen.get("Recht op premie");
        assertTrue(recht.kenmerken.contains("toegekend"), "de extensiekop bereikt het objecttype niet");
        assertSame(recht, m.objecttypen.get("Recht op premie bij eb").supertype);
        Feittype ft = m.feittypen.get("jutter heeft premierecht");
        assertEquals("Recht op premie", ft.rollen.get(1).objecttypeNaam());
        String ir = ir(m);
        assertEquals(0, tel(ir, "\"node\":\"Unresolved\""), ir);
        // the target and the two readings of the first rule, the target of the second, the argument of the sum
        assertTrue(ir.contains("\"target\":{\"node\":\"AttributeReference\",\"attribute\":\"premie\","
                + "\"subjectType\":\"Rechtoppremie\""), ir);
        assertEquals(1, tel(ir, "\"condition\":{\"node\":\"Comparison\",\"op\":\"greater\",\"left\":{\"node\":"
                + "\"AttributeReference\",\"attribute\":\"vondstgewicht\",\"subjectType\":\"Rechtoppremie\""), ir);
        assertTrue(ir.contains("\"sourceRole\":{\"node\":\"RoleBinding\",\"factType\":\"jutterheeftpremierecht\","
                + "\"role\":\"rechtoppremie\"}"), ir);

        ScenarioBundel b = ScenarioResolver.resolve(bron + """
            Scenario een recht
            Gegeven is de volgende situatie:
            een Jutter J
            - de rechten op premie zijn R1
            een Recht op premie R1
            - het vondstgewicht is 3
            Met als verwacht resultaat:
            - de premie van R1 is 15
            - het premiebedrag van J is 15
            """);
        assertTrue(b.isOk(), () -> b.model().errors().toString());
        ResolvedScenario s = b.scenarios().get(0);
        assertEquals("Recht op premie", s.situatie().instanties().get(1).objecttype().naam);
        assertEquals("rechten op premie", s.situatie().instanties().get(0).rollen().get(0).sleutel());
        String cas = CaseEmitter.emit(s).replaceAll("\\s+", "");
        assertTrue(cas.contains("{\"label\":\"R1\",\"objectType\":\"Rechtoppremie\""), cas);
    }

    // ---------------- the plural ----------------

    @Test void een_meervoud_loopt_tot_zijn_sluithaakje() {
        RegelSpraakModel m = schoon("""
            Objecttype de Jutter (mv: Jutters) (bezield)
                het aantal rechten Numeriek (niet-negatief geheel getal);
                de premie per dag (mv: premies per dag) Numeriek (geheel getal);
            Objecttype het Recht op premie (mv: Rechten op premie)
                de premie Numeriek (geheel getal);
            Feittype jutter heeft premierecht
                de rechthebbende Jutter
                het recht op premie (mv: rechten op premie) Recht op premie
            Eén rechthebbende heeft meerdere rechten op premie
            Regel aantal rechten
                geldig altijd
                    Het aantal rechten van een Jutter moet berekend worden als het aantal rechten op premie van de Jutter.
            """);
        assertEquals("Rechten op premie", m.objecttypen.get("Recht op premie").meervoud);
        assertEquals("premies per dag", m.objecttypen.get("Jutter").attribuutMeervoud.get("premie per dag"));
        Rol rol = m.feittypen.get("jutter heeft premierecht").rollen.get(1);
        assertEquals("rechten op premie", rol.meervoud());
        assertTrue(rol.meervoudig(), "de kardinaliteitsregel noemt de rol in haar meervoud");
        String ir = ir(m);
        assertEquals(0, tel(ir, "\"node\":\"Unresolved\""), ir);
        assertTrue(ir.contains("\"sourceRole\":{\"node\":\"RoleBinding\",\"factType\":\"jutterheeftpremierecht\","
                + "\"role\":\"rechtoppremie\"}"), ir);
    }

    // ---------------- the fact-type name ----------------

    @Test void een_feittypenaam_draagt_werkwoorden() {
        RegelSpraakModel m = schoon("""
            Objecttype de Jutter (mv: Jutters) (bezield)
                de naam Tekst;
            Objecttype de Vondst (mv: Vondsten)
                de waarde Numeriek (geheel getal);
            Feittype jutter heeft vondst
                de vinder Jutter
                de vondst (mv: vondsten) Vondst
            Eén vinder heeft meerdere vondsten
            Feittype vondst is gemeld bij jutter
                de melder Jutter
                de gemelde vondst (mv: gemelde vondsten) Vondst
            Eén melder heeft meerdere gemelde vondsten
            Feittype jutters hebben vondsten die zijn geborgen
                de berger Jutter
                de geborgen vondst (mv: geborgen vondsten) Vondst
            Eén berger heeft meerdere geborgen vondsten
            """);
        assertEquals(List.of("jutter heeft vondst", "vondst is gemeld bij jutter", "jutters hebben vondsten die zijn geborgen"),
                new ArrayList<>(m.feittypen.keySet()));
        assertEquals(List.of("vinder", "vondst"),
                m.feittypen.get("jutter heeft vondst").rollen.stream().map(Rol::naam).toList());
    }

    /** 'de' and 'het' open the role line, so a fact-type name still ends there, and the line check refuses the role
     *  that swallowed the rest; the guess that names the likely cause stays beside it. A keyword that opens a block
     *  ends the name too: a fact type without roles fails at the next block. */
    @Test void de_rolregel_en_een_volgend_blok_beeindigen_de_feittypenaam() {
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype de Jutter (mv: Jutters) (bezield)
                de naam Tekst;
            Objecttype de Vondst (mv: Vondsten)
                de waarde Numeriek (geheel getal);
            Feittype vondst voor de jutter
                de vinder Jutter
                de vondst (mv: vondsten) Vondst
            Eén vinder heeft meerdere vondsten
            """);
        enigeMelding(m, Severity.ERROR, 5, "feittype 'vondst voor': de rol 'jutter de vinder' begint met 'de' op de "
                + "regel waar de kop eindigt, direct na 'voor'; een rol begint op een eigen regel — hoort 'de' bij de "
                + "naam, verbind het dan met een koppelteken aan het woord ervoor ('voor-de')");
        assertTrue(m.diagnostics.stream().anyMatch(d -> d.severity() == Severity.WARNING && d.line() == 5
                && d.message().contains("de rolnaam 'jutter de vinder' bevat een lidwoord")), () -> m.diagnostics.toString());
        assertEquals(1, m.errors().size(), () -> m.errors().toString());

        RegelSpraakModel b = Resolver.resolve("""
            Feittype vondstrelatie
            Objecttype de Jutter (mv: Jutters)
                de naam Tekst;
            """);
        Diagnostic eerste = b.errors().get(0);
        assertEquals(2, eerste.line(), () -> b.errors().toString());
        assertTrue(eerste.message().startsWith("syntaxfout: extraneous input 'Objecttype'"), eerste::toString);
    }

    // ---------------- a declaration on the line where the previous one ended ----------------

    @Test void een_lid_op_de_regel_van_de_kop_is_een_fout() {
        RegelSpraakModel m = Resolver.resolve("Objecttype de Vondst bij eb de waarde Numeriek (geheel getal);\n");
        enigeMelding(m, Severity.ERROR, 1, "objecttype 'Vondst bij eb': het attribuut 'waarde' begint met 'de' op de "
                + "regel waar de kop eindigt, direct na 'eb'; een lid begint op een eigen regel — hoort 'de' bij de "
                + "naam, verbind het dan met een koppelteken aan het woord ervoor ('eb-de')");
    }

    /** The lines decide, never the capitals: the attribute name stops at the capitalised 'BK', which reads as its
     *  domain, and the rest of the line is a second member. Until this work that document went out with two
     *  attributes and no diagnostic. Two members on one line are refused the same way when a ';' separates them. */
    @Test void een_lid_op_de_regel_van_het_vorige_lid_is_een_fout() {
        RegelSpraakModel m = Resolver.resolve("""
            Domein BK is van het type Numeriek (geheel getal)
            Objecttype de Jutter (mv: Jutters) (bezield)
                de datum einde seizoen BK bij eb Datum in dagen;
            """);
        enigeMelding(m, Severity.ERROR, 3, "objecttype 'Jutter': het attribuut 'bij eb' begint met 'bij' op de regel "
                + "waar het vorige lid 'datum einde seizoen' eindigt, direct na 'BK'; een lid begint op een eigen regel");
        RegelSpraakModel p = Resolver.resolve("""
            Objecttype de Vondst (mv: Vondsten)
                de naam Tekst; de soort Tekst;
            """);
        enigeMelding(p, Severity.ERROR, 2, "objecttype 'Vondst': het attribuut 'soort' begint met 'de' op de regel waar "
                + "het vorige lid 'naam' eindigt, direct na ';'; een lid begint op een eigen regel");
    }

    /** 'kenmerk' ends a characteristic declaration: an attribute name that carries the word is cut there, and the
     *  sentence names the hyphen that keeps the word in the name. */
    @Test void een_kenmerk_dat_een_naam_afkapt_noemt_het_koppelteken() {
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype de Vondst (mv: Vondsten)
                de premie kenmerk strand Numeriek (geheel getal);
            """);
        enigeMelding(m, Severity.ERROR, 2, "objecttype 'Vondst': het attribuut 'strand' begint met 'strand' op de "
                + "regel waar het vorige lid 'premie' eindigt, direct na 'kenmerk'; een lid begint op een eigen regel — "
                + "hoort 'kenmerk' bij de naam, verbind het dan met een koppelteken aan het woord erna ('kenmerk-strand')");
        RegelSpraakModel k = schoon("""
            Objecttype de Vondst (mv: Vondsten)
                de premie kenmerk-strand Numeriek (geheel getal);
            """);
        assertEquals(List.of("premie kenmerk-strand"), new ArrayList<>(k.objecttypen.get("Vondst").attributen.keySet()));
    }

    @Test void een_rol_op_de_regel_van_de_vorige_rol_is_een_fout() {
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype de Jutter (mv: Jutters) (bezield)
                de naam Tekst;
            Objecttype de Vondst (mv: Vondsten)
                de waarde Numeriek (geheel getal);
            Feittype vondstrelatie
                de vinder Jutter de vondst (mv: vondsten) Vondst
            Eén vinder heeft meerdere vondsten
            """);
        enigeMelding(m, Severity.ERROR, 6, "feittype 'vondstrelatie': de rol 'vondst' begint met 'de' op de regel waar "
                + "de vorige rol 'vinder' eindigt, direct na 'Jutter'; een rol begint op een eigen regel — hoort 'de' bij "
                + "de naam, verbind het dan met een koppelteken aan het woord ervoor ('Jutter-de')");
    }

    /** A member that runs on over the next line begins on a line of its own and stays admitted (the §3.9 example), as
     *  does a member without an article on the line after the head, where no capital and no article marks the
     *  boundary. */
    @Test void een_lid_dat_doorloopt_of_op_een_eigen_regel_begint_blijft_toegelaten() {
        RegelSpraakModel m = schoon("""
            Objecttype de Vondst (mv: Vondsten)
                de datum van aankomst bij het
                    vertrekpunt Datum in dagen;
                de naam Tekst;
            Objecttype de Boei
                kleur Tekst;
            """);
        assertEquals(List.of("datum van aankomst bij het vertrekpunt", "naam"),
                new ArrayList<>(m.objecttypen.get("Vondst").attributen.keySet()));
        assertEquals(List.of("kleur"), new ArrayList<>(m.objecttypen.get("Boei").attributen.keySet()));
    }

    // ---------------- a role's filler ----------------

    @Test void een_rolvuller_die_geen_objecttype_is_is_een_fout() {
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype de Jutter (mv: Jutters) (bezield)
                de naam Tekst;
            Objecttype de Vondst (mv: Vondsten)
                de waarde Numeriek (geheel getal);
            Feittype vondstrelatie
                de vinder BK Jutter
                de vondst (mv: vondsten) Vondst
            Eén vinder heeft meerdere vondsten
            """);
        enigeMelding(m, Severity.ERROR, 6, "feittype 'vondstrelatie': de rol 'vinder' heeft als objecttype 'BK Jutter', "
                + "en dat is nergens als objecttype gedeclareerd — WF-003: de vuller van een rol is een gedeclareerd "
                + "objecttype; 'Jutter' is dat wel: een rolnaam eindigt bij het eerste woord met een hoofdletter, dus 'BK' "
                + "is als deel van het objecttype gelezen");
        RegelSpraakModel d = Resolver.resolve("""
            Domein Bedrag is van het type Numeriek (getal met 2 decimalen)
            Objecttype de Vondst (mv: Vondsten)
                de waarde Bedrag;
            Feittype vondstwaardering
                de vondst Vondst
                de waardering Bedrag
            Eén vondst heeft één waardering
            """);
        enigeMelding(d, Severity.ERROR, 6, "feittype 'vondstwaardering': de rol 'waardering' heeft als objecttype "
                + "'Bedrag', en dat is een domein, geen objecttype — WF-003: de vuller van een rol is een gedeclareerd "
                + "objecttype");
    }

    /** A filler the document declares nowhere is one file of a model that declares its types in another: a WARNING,
     *  and read together with that file, nothing. A type declared after the fact type binds, as a supertype does. */
    @Test void een_rolvuller_die_nergens_gedeclareerd_is_is_een_waarschuwing() {
        String feittype = """
            Feittype vondstaanmelding
                de aanmelder Jutter
                de vondst (mv: vondsten) Vondst
            Eén aanmelder heeft meerdere vondsten
            """;
        RegelSpraakModel m = Resolver.resolve(feittype);
        assertTrue(m.errors().isEmpty(), () -> m.errors().toString());
        enigeMelding(m, Severity.WARNING, 2, "feittype 'vondstaanmelding': de rol 'aanmelder' heeft als objecttype "
                + "'Jutter', en dat is nergens als objecttype gedeclareerd — WF-003: de vuller van een rol is een "
                + "gedeclareerd objecttype; staat het in een ander bestand van het model, lees de bestanden dan samen");
        schoon(feittype + """
            Objecttype de Jutter (mv: Jutters) (bezield)
                de naam Tekst;
            Objecttype de Vondst (mv: Vondsten)
                de waarde Numeriek (geheel getal);
            """);
    }

    // ---------------- a declared name rule text does not read ----------------

    @Test void een_naam_die_regeltekst_niet_leest_krijgt_een_waarschuwing() {
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype de Jutter (mv: Jutters) (bezield)
                de naam Tekst;
            Objecttype de Vondst (mv: Vondsten)
                de premie als strand Numeriek (geheel getal);
                is van belang kenmerk (bijvoeglijk);
            Feittype vondstaanmelding
                de melder van Jutter
                de vondst (mv: vondsten) Vondst
            Eén melder van heeft meerdere vondsten
            """);
        assertTrue(m.errors().isEmpty(), () -> m.errors().toString());
        enigeMelding(m, Severity.WARNING, 4, "objecttype 'Vondst': regeltekst leest het attribuut 'premie als strand' "
                + "niet als één naam, want 'als' is daar geen woord van een naam; verbind het met een koppelteken aan "
                + "een buurwoord ('premie-als strand')");
        enigeMelding(m, Severity.WARNING, 5, "objecttype 'Vondst': regeltekst leest het kenmerk 'van belang' niet als "
                + "één naam, want 'van' is daar geen woord van een naam; verbind het met een koppelteken aan een "
                + "buurwoord ('van-belang')");
        enigeMelding(m, Severity.WARNING, 7, "feittype 'vondstaanmelding': regeltekst leest de rol 'melder van' niet "
                + "als één naam, want 'van' is daar geen woord van een naam; verbind het met een koppelteken aan een "
                + "buurwoord ('melder-van')");
        assertEquals(3, m.diagnostics.size(), () -> m.diagnostics.toString());
    }

    @Test void de_koppeltekenspelling_leest_in_regeltekst() {
        RegelSpraakModel m = schoon("""
            Objecttype de Vondst (mv: Vondsten)
                de premie-als strand Numeriek (geheel getal);
                de waarde Numeriek (geheel getal);
            Regel premie
                geldig altijd
                    De premie-als strand van een Vondst moet berekend worden als de waarde van de Vondst plus 1.
            """);
        String ir = ir(m);
        assertEquals(0, tel(ir, "\"node\":\"Unresolved\""), ir);
        assertTrue(ir.contains("\"target\":{\"node\":\"AttributeReference\",\"attribute\":\"premie-alsstrand\""), ir);
    }

    // ---------------- what did not move ----------------

    /** A head in title case reads as it did, and the rule text that wrote the name in lowercase resolves against
     *  it as before: rule text compares names without regard to case. */
    @Test void een_kop_in_titelhoofdletters_leest_als_voorheen() {
        RegelSpraakModel m = schoon("""
            Objecttype het Recht Op Premie (mv: Rechten Op Premie)
                de premie Numeriek (geheel getal);
                de waarde Numeriek (geheel getal);
            Regel premie
                geldig altijd
                    De premie van een Recht op premie moet berekend worden als de waarde van het Recht op premie plus 1.
            """);
        assertEquals(List.of("Recht Op Premie"), new ArrayList<>(m.objecttypen.keySet()));
        assertEquals(0, tel(ir(m), "\"node\":\"Unresolved\""));
    }
}
