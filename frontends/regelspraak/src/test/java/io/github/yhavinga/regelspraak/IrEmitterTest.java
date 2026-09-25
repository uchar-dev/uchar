package io.github.yhavinga.regelspraak;

import io.github.yhavinga.regelspraak.emit.IrEmitter;
import io.github.yhavinga.regelspraak.ir.Bron;
import io.github.yhavinga.regelspraak.ir.Conditie;
import io.github.yhavinga.regelspraak.ir.Expr;
import io.github.yhavinga.regelspraak.ir.ResolvedFeitCreatie;
import io.github.yhavinga.regelspraak.ir.ResolvedRegel;
import io.github.yhavinga.regelspraak.ir.ResolvedUniciteit;
import io.github.yhavinga.regelspraak.model.Datatype;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Diagnostic;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Objecttype;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Severity;
import io.github.yhavinga.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The emitter writes the symbolic form of every reference the uchar wire carries — the
 * navigation/scope/creation role bindings — each derived from the declared names, and refuses a model
 * where a position that needs one has no unique binding. These tests exercise the EMITTED JSON (not just
 * the resolved records): the four families plus the fold-source discipline, so a regression in the wire
 * shape is caught here. Assertions compare against whitespace-collapsed JSON so they are robust to
 * indentation while still pinning the exact field/value sequence.
 */
class IrEmitterTest {

    /** The emitted IR JSON with all whitespace removed — deterministic, so an exact
     *  compacted fragment pins the wire shape without depending on indentation. */
    private static String emitCompact(String src) {
        RegelSpraakModel m = Resolver.resolve(src);
        assertTrue(m.errors().isEmpty(), () -> "onverwachte fouten: " + m.errors());
        return IrEmitter.emit(m).replaceAll("\\s+", "");
    }

    /** The emitted IR JSON UNTOUCHED. Only a witness about whitespace INSIDE a value may use this
     *  — {@link #emitCompact} would erase the very difference it asserts. */
    private static String emitRaw(String src) {
        RegelSpraakModel m = Resolver.resolve(src);
        assertTrue(m.errors().isEmpty(), () -> "onverwachte fouten: " + m.errors());
        return IrEmitter.emit(m);
    }

    @Test void navigatiebindingen_op_ketenlezing_subject_uitwaarts() {
        // "het bedrijfsniveau van het bedrijf van zijn afdeling": a §13.4.1 declared chain
        // Werknemer -[afdeling]-> Afdeling -[bedrijf]-> Bedrijf. One binding per hop, subject-
        // outward, each naming the REACHED role by its declared name.
        String src = """
            Objecttype de Werknemer
                de rang Numeriek (geheel getal);
            Objecttype de Afdeling
            Objecttype het Bedrijf
                het bedrijfsniveau Numeriek (geheel getal);
            Feittype aanstelling
                de werknemer Werknemer
                de afdeling Afdeling
            Eén afdeling telt meerdere werknemers
            Feittype onderdeel
                de afdeling Afdeling
                het bedrijf Bedrijf
            Eén bedrijf telt meerdere afdelingen
            Regel rang
                geldig altijd
                    De rang van een Werknemer moet gesteld worden op het bedrijfsniveau van het bedrijf van zijn afdeling.
            """;
        String c = emitCompact(src);
        assertTrue(c.contains(
                "\"path\":["
                + "{\"node\":\"RoleBinding\",\"factType\":\"aanstelling\",\"role\":\"afdeling\"},"
                + "{\"node\":\"RoleBinding\",\"factType\":\"onderdeel\",\"role\":\"bedrijf\"}]"),
                () -> "keten-bindingen (subject-uitwaarts, gedeclareerde namen) ontbreken:\n" + c);
    }

    @Test void navigatiebinding_op_genavigeerde_kenmerkcheck() {
        // "indien zijn afdeling kernafdeling is": a navigated §8.1.8 characteristic check binds
        // the reached role of the one hop.
        String src = """
            Objecttype de Werknemer
                is sleutelkracht kenmerk (bijvoeglijk);
            Objecttype de Afdeling
                is kernafdeling kenmerk (bijvoeglijk);
            Feittype aanstelling
                de werknemer Werknemer
                de afdeling Afdeling
            Eén afdeling telt meerdere werknemers
            Regel sleutelkracht
                geldig altijd
                    Een Werknemer is sleutelkracht indien zijn afdeling kernafdeling is.
            """;
        String c = emitCompact(src);
        assertTrue(c.contains(
                "\"navigation\":{\"node\":\"RoleBinding\",\"factType\":\"aanstelling\",\"role\":\"afdeling\"}"),
                () -> "navigation op de genavigeerde kenmerkcheck ontbreekt:\n" + c);
    }

    @Test void scoperol_op_parent_scoped_uniciteit() {
        // "De X van alle Werknemer van de afdeling moeten uniek zijn": parent-scoped uniqueness;
        // the version's scope names the scope-parent relation (the reference field made symbolic).
        String src = """
            Objecttype de Werknemer
                het personeelsnummer Numeriek (geheel getal);
            Objecttype de Afdeling
            Feittype aanstelling
                de werknemer Werknemer
                de afdeling Afdeling
            Eén afdeling telt meerdere werknemers
            Regel uniek
                geldig altijd
                    De personeelsnummer van alle Werknemer van de afdeling moeten uniek zijn.
            """;
        String c = emitCompact(src);
        assertTrue(c.contains(
                "\"scope\":{\"node\":\"RoleBinding\",\"factType\":\"aanstelling\",\"role\":\"afdeling\"}"),
                () -> "de scope op de parent-scoped uniciteit ontbreekt:\n" + c);
    }

    @Test void feitcreatie_symbolisch_blok_is_heel_of_niet() {
        // A §13.4.7 feitcreatie with one tussenstap: the whole symbolic block — the created fact type, both
        // its roles, the anchor and the path from it, the intermediate hop and then the final one, each a
        // RoleBinding — and nothing beside it: IR-SPEC ch. 3.4 leaves the storage fields to the backend's own
        // role→field mapping, and the target type is the declared filler of the target role, so it is not
        // written either.
        String src = """
            Objecttype het Bedrijf
            Objecttype de Afdeling
            Objecttype het Project
            Feittype onderdeel
                de afdeling Afdeling
                het bedrijf Bedrijf
            Eén bedrijf telt meerdere afdelingen
            Feittype uitvoering
                de afdeling Afdeling
                het project (mv: projecten) Project
            Eén afdeling telt meerdere projecten
            Feittype sponsoring
                het sponsorbedrijf Bedrijf
                het sponsorproject (mv: sponsorprojecten) Project
            Eén sponsorbedrijf telt meerdere sponsorprojecten
            Regel sponsoring
                geldig altijd
                    Een sponsorproject van een Bedrijf is een project van de afdeling van het bedrijf.
            """;
        String c = emitCompact(src);
        assertTrue(c.contains("{\"node\":\"FactCreation\",\"name\":\"sponsoring\",\"versions\":[{"
                + "\"factType\":\"sponsoring\",\"anchorRole\":\"sponsorbedrijf\",\"targetRole\":\"sponsorproject\","
                + "\"anchor\":\"Bedrijf\",\"path\":["
                + "{\"node\":\"RoleBinding\",\"factType\":\"onderdeel\",\"role\":\"afdeling\"},"
                + "{\"node\":\"RoleBinding\",\"factType\":\"uitvoering\",\"role\":\"project\"}],"
                + "\"validity\":{\"node\":\"Validity\"}}]}"),
                () -> "het symbolische feitcreatieblok ontbreekt, is onvolledig of draagt velden ernaast:\n" + c);
    }

    @Test void fold_bron_argument_draagt_geen_navigatiebindingen() {
        // The fold's SOURCE argument routes through its sourceRole; even though its type (Persoon) differs
        // from the fold's outer subject (Ploeg) — the by-pair shape — it must carry NO
        // path (IR-SPEC ch. 4.1a; the uchar verifier rejects a path in a fold slot).
        String src = """
            Objecttype de Ploeg
                het totaalbedrag Numeriek (geheel getal);
            Objecttype de Persoon
                de leeftijd Numeriek (geheel getal);
            Feittype deelname
                de groep Ploeg
                de deelnemer (mv: deelnemers) Persoon
            Eén groep telt meerdere deelnemers
            Regel som
                geldig altijd
                    Het totaalbedrag van een groep moet berekend worden als de som van de leeftijd van alle deelnemers van de groep.
            """;
        String c = emitCompact(src);
        assertTrue(c.contains(
                "\"sourceRole\":{\"node\":\"RoleBinding\",\"factType\":\"deelname\",\"role\":\"deelnemer\"}"),
                () -> "de vouw moet haar sourceRole dragen:\n" + c);
        assertFalse(c.contains("\"path\":"),
                () -> "een fold-bron-argument mag GEEN path dragen:\n" + c);
    }

    @Test void fold_filter_elementlezing_draagt_geen_navigatiebindingen() {
        // The filter is a subselectie over the fold ELEMENT: "die kleiner zijn dan 18" reads the
        // deelnemer's OWN leeftijd — an own read, not a navigation from the outer groep. So the filter
        // read carries no spurious by-pair path (the emitter walks the filter with the
        // element as subject).
        String src = """
            Objecttype de Ploeg
                het totaalbedrag Numeriek (geheel getal);
            Objecttype de Persoon
                de leeftijd Numeriek (geheel getal);
            Feittype deelname
                de groep Ploeg
                de deelnemer (mv: deelnemers) Persoon
            Eén groep telt meerdere deelnemers
            Regel som
                geldig altijd
                    Het totaalbedrag van een groep moet berekend worden als de som van de leeftijd van alle deelnemers van de groep die kleiner zijn dan 18.
            """;
        String c = emitCompact(src);
        assertTrue(c.contains(
                "\"sourceRole\":{\"node\":\"RoleBinding\",\"factType\":\"deelname\",\"role\":\"deelnemer\"}"),
                () -> "de vouw moet haar sourceRole dragen:\n" + c);
        assertFalse(c.contains("\"path\":"),
                () -> "een element-eigen filter-lezing mag GEEN path dragen:\n" + c);
    }

    @Test void unit_literal_lexeme_is_kaal_eenheid_in_type() {
        // uchar WF-081 (IR-SPEC ch. 4.1): a numeric literal's value is the BARE number;
        // the unit rides the literal's datatype ("unit"), exactly as attribute datatypes carry
        // it. The source snippet ("5 €") stays in the source record. A number carrying its unit ("5 €") is
        // verifier-ill-formed and faults in every uchar executor.
        String src = """
            Eenheidsysteem Munt
                de euro (mv: euros) EUR €
            Objecttype de Vondst
                de waarde Numeriek (geheel getal) met eenheid €;
                het meldloon Numeriek (geheel getal) met eenheid €;
            Regel meldloon
                geldig altijd
                    Het meldloon van een Vondst moet berekend worden als de waarde van de Vondst plus 5 €.
            """;
        String c = emitCompact(src);
        // the emitter JSON-escapes non-ASCII, so the compacted € is the six-char escape \\u20ac
        assertTrue(c.contains(
                "{\"node\":\"Literal\",\"value\":{\"number\":\"5\"},\"type\":{\"node\":\"Number\",\"whole\":true,"
                + "\"decimals\":0,\"sign\":\"unspecified\",\"unit\":{\"node\":\"Unit\",\"abbreviation\":\"EUR\","
                + "\"system\":\"Munt\",\"canonical\":\"EUR\",\"factorToCanonical\":{\"num\":1,\"den\":1}}},"
                + "\"source\":{\"snippet\":\"5\\u20ac\"}}"),
                () -> "de unit-literal moet een kaal lexeme dragen met de eenheid in het type:\n" + c);
        assertFalse(c.contains("\"number\":\"5\\u20ac\""),
                () -> "de eenheid mag niet in het literal-lexeme zitten:\n" + c);
    }

    @Test void de_factor_is_de_gedeclareerde_breuk_en_de_spellingen_staan_op_de_declaratie() {
        // IR-SPEC ch. 2.5: a unit's factor is the exact fraction its declarations write, multiplied along the chain
        // ("1/60" over "1/24" is 1/1440; "1,5" is 3/2), not the product of 16-digit decimals the unit record holds. A
        // unit's further spellings — a second abbreviation, then its word and plural — stand on its declaration and
        // nowhere else, and a spelling equal to the abbreviation is none.
        String src = """
            Eenheidsysteem Inhoud
                de emmer (mv: emmers) em
                de schep sp = 1/24 em
                de korrel kr kl = 1/60 sp
                de baal bl = 1,5 em
            Objecttype de Vondst
                de zaaihoeveelheid Numeriek (geheel getal) met eenheid kr;
            """;
        String c = emitCompact(src);
        String stelsel = "\"system\":\"Inhoud\",\"canonical\":\"em\",\"factorToCanonical\":";
        assertTrue(c.contains("{\"node\":\"UnitSystem\",\"name\":\"Inhoud\",\"units\":["
                + "{\"node\":\"Unit\",\"abbreviation\":\"em\"," + stelsel + "{\"num\":1,\"den\":1},"
                + "\"spellings\":[\"emmer\",\"emmers\"]},"
                + "{\"node\":\"Unit\",\"abbreviation\":\"sp\"," + stelsel + "{\"num\":1,\"den\":24},"
                + "\"spellings\":[\"schep\"]},"
                + "{\"node\":\"Unit\",\"abbreviation\":\"kr\"," + stelsel + "{\"num\":1,\"den\":1440},"
                + "\"spellings\":[\"kl\",\"korrel\"]},"
                + "{\"node\":\"Unit\",\"abbreviation\":\"bl\"," + stelsel + "{\"num\":3,\"den\":2},"
                + "\"spellings\":[\"baal\"]}]}"),
                () -> "het stelsel draagt de gedeclareerde breuken en de spellingen op de declaratie:\n" + c);
        assertTrue(c.contains("\"datatype\":{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\","
                + "\"unit\":{\"node\":\"Unit\",\"abbreviation\":\"kr\"," + stelsel + "{\"num\":1,\"den\":1440}}}"),
                () -> "het voorkomen draagt dezelfde breuk en geen spellingen:\n" + c);
        assertEquals(4, c.split("\"spellings\"", -1).length - 1, () -> "spellingen alleen in het stelsel:\n" + c);
    }

    @Test void een_tweede_declaratie_van_een_afkorting_is_een_fout_en_vervangt_de_eerste_niet() {
        // WF-009: one Unit per abbreviation under the name identity. The resolver keyed its units on the written
        // abbreviation, so a second declaration of one replaced the first without a word — the document then carried
        // "t = 100 kg" alone, verifier-clean — and a second one that differs in case alone became a second unit.
        // Both are refused now, by name and at the second declaration's line; the first declaration stands.
        RegelSpraakModel dubbel = Resolver.resolve("""
            Eenheidsysteem Gewicht
                het kilogram kg
                de ton t = 1000 kg
                de tonne t = 100 kg
            Objecttype de Vondst
                het gewicht Numeriek (geheel getal) met eenheid t;
            """);
        assertEquals(List.of("eenheidstelsel 'Gewicht': de afkorting 't' is al gedeclareerd voor de eenheid 't'; één "
                        + "eenheid per afkorting (WF-009), dus deze tweede declaratie wordt geweigerd in plaats van de "
                        + "eerste stil te vervangen"),
                dubbel.errors().stream().map(Diagnostic::message).toList());
        assertEquals(4, dubbel.errors().get(0).line(), "de melding staat op de tweede declaratie");
        assertEquals(new java.math.BigDecimal("1000"),
                dubbel.eenheidstelsels.get("Gewicht").eenheden.get("t").factorNaarCanoniek(), "de eerste declaratie staat");

        RegelSpraakModel kast = Resolver.resolve("""
            Eenheidsysteem Gewicht
                het kilogram kg
                de kilo KG = 1 kg
            Objecttype de Vondst
                het gewicht Numeriek (geheel getal) met eenheid kg;
            """);
        assertEquals(List.of("eenheidstelsel 'Gewicht': de afkorting 'KG' is al gedeclareerd als 'kg' voor de eenheid "
                        + "'kg'; één eenheid per afkorting (WF-009), dus deze tweede declaratie wordt geweigerd in "
                        + "plaats van de eerste stil te vervangen"),
                kast.errors().stream().map(Diagnostic::message).toList());
        assertFalse(kast.eenheidstelsels.get("Gewicht").eenheden.containsKey("KG"), "de tweede declaratie is geweigerd");

        // two spellings of one abbreviation inside ONE declaration are that unit's own, and no collision
        RegelSpraakModel eigen = Resolver.resolve("""
            Eenheidsysteem Gewicht
                het kilogram kg KG
            Objecttype de Vondst
                het gewicht Numeriek (geheel getal) met eenheid kg;
            """);
        assertTrue(eigen.errors().isEmpty(), () -> "onverwachte fouten: " + eigen.errors());
    }

    @Test void twee_afkortingen_die_een_naam_zijn_in_een_stelsel_worden_geweigerd() {
        // The emitter's own guard behind the resolver's refusal above: a model that keys two units of one system on
        // spellings the name identity joins (kg and KG) would put one name on the wire twice, which every reader
        // refuses (WF-009), so the emitter refuses the model instead of writing that document. Only a model the
        // resolver did not build reaches it, so this one is built by hand.
        RegelSpraakModel m = new RegelSpraakModel();
        RegelSpraakModel.Eenheidstelsel gewicht = new RegelSpraakModel.Eenheidstelsel("Gewicht");
        gewicht.eenheden.put("kg", new io.github.yhavinga.regelspraak.model.Eenheid("kg", "Gewicht"));
        gewicht.eenheden.put("KG", new io.github.yhavinga.regelspraak.model.Eenheid("KG", "Gewicht"));
        m.eenheidstelsels.put("Gewicht", gewicht);
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> IrEmitter.emit(m));
        assertTrue(e.getMessage().contains("'kg' en 'KG'") && e.getMessage().contains("(WF-009)"), e.getMessage());
    }

    @Test void percentage_literal_lexeme_is_kaal() {
        // Same WF-081 fence for the '%' suffix: "21%" emits the number "21", its type a number with the unit '%'.
        // The presentation tag belongs to the declaration and never to a type annotation (IR-SPEC ch. 2.4).
        String src = """
            Objecttype de Vondst
                het heffingspercentage Percentage (geheel getal) met eenheid %;
            Regel heffingspercentage
                geldig altijd
                    Het heffingspercentage van een Vondst moet gesteld worden op 21%.
            """;
        String c = emitCompact(src);
        assertTrue(c.contains("{\"node\":\"Literal\",\"value\":{\"number\":\"21\"},\"type\":{\"node\":\"Number\",\"whole\":true,"
                + "\"decimals\":0,\"sign\":\"unspecified\",\"unit\":{\"node\":\"Unit\",\"abbreviation\":\"%\","
                + "\"canonical\":\"%\",\"factorToCanonical\":{\"num\":1,\"den\":1}}},\"source\":{\"snippet\":\"21%\"}}"),
                () -> "de percentage-literal moet een kaal lexeme dragen, als getal met de eenheid %, zonder tag:\n" + c);
        assertTrue(c.contains("{\"node\":\"Attribute\",\"name\":\"heffingspercentage\",\"datatype\":{\"node\":"
                + "\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\",\"unit\":{\"node\":\"Unit\","
                + "\"abbreviation\":\"%\",\"canonical\":\"%\",\"factorToCanonical\":{\"num\":1,\"den\":1}},"
                + "\"presentation\":\"percentage\"}}"),
                () -> "de declaratie draagt de presentatietag:\n" + c);
        // the rule version's own presentation is the dialect tag equation, a field of another node
        assertEquals(1, c.split("\"presentation\":\"percentage\"", -1).length - 1,
                () -> "de presentatietag staat alleen op de declaratie:\n" + c);
        assertTrue(c.contains("\"initialisation\":false,\"presentation\":\"equation\"}"),
                () -> "de regelversie draagt haar eigen presentatie:\n" + c);
        assertFalse(c.contains("\"number\":\"21%\""),
                () -> "het %-teken mag niet in het literal-lexeme zitten:\n" + c);
    }

    @Test void datum_literal_lexeme_is_iso_snippet_in_bron() {
        // uchar WF-084 (IR-SPEC ch. 4.1): a date literal's value is the BARE ISO lexeme the uchar
        // executors parse (yyyy-mm-dd), NOT the Dutch day-first source snippet ("31-01-2024"). The
        // day-first form ("31-01-2024") was silently misread as year 31 (0036-07-16) by both
        // executors — a differential-blind wrong answer. The source snippet stays in the source record.
        String src = """
            Objecttype de Vondst
                de vondstdag Datum in dagen;
                is verjaard kenmerk (bijvoeglijk);
            Regel verjaard
                geldig altijd
                    Een Vondst is verjaard
                    indien zijn vondstdag eerder is dan 31-01-2024.
            """;
        String c = emitCompact(src);
        assertTrue(c.contains(
                "{\"node\":\"Literal\",\"value\":{\"date\":\"2024-01-31\"},\"type\":{\"node\":\"Date\","
                + "\"precision\":\"day\"},\"source\":{\"snippet\":\"31-01-2024\"}}"),
                () -> "de datum-literal moet het kale ISO-lexeme dragen, de bron-snippet in source:\n" + c);
        assertFalse(c.contains("\"date\":\"31-01-2024\""),
                () -> "de day-first bron-snippet mag niet in de literalwaarde zitten:\n" + c);
    }

    @Test void datum_literal_dedato_prefix_wordt_iso_lexeme() {
        // The §13.2.19 "dd. " de-dato prefix (and the day-first order) belong to the source snippet,
        // never to the parsed lexeme: "dd. 31-01-2024" emits the date "2024-01-31", the snippet unchanged.
        String src = """
            Objecttype de Vondst
                de vondstdag Datum in dagen;
                is verjaard kenmerk (bijvoeglijk);
            Regel verjaard
                geldig altijd
                    Een Vondst is verjaard
                    indien zijn vondstdag eerder is dan dd. 31-01-2024.
            """;
        String c = emitCompact(src);
        assertTrue(c.contains("{\"node\":\"Literal\",\"value\":{\"date\":\"2024-01-31\"},\"type\":{\"node\":\"Date\",\"precision\":\"day\"}"),
                () -> "de de-dato-literal moet het kale ISO-lexeme dragen:\n" + c);
        assertFalse(c.contains("\"date\":\"dd.31-01-2024\"") || c.contains("\"date\":\"31-01-2024\""),
                () -> "de bron-snippet (prefix of day-first) mag niet in de literalwaarde zitten:\n" + c);
    }

    @Test void datumtijd_literal_lexeme_is_iso_snippet_in_bron() {
        // uchar WF-095 (IR-SPEC ch. 4.1, narrowed 2026-09-22): a datum-tijd literal's value is the
        // canonical ISO lexeme yyyy-mm-ddThh:mm:ss.fff, the day-first source snippet stays in source —
        // the same split the date branch makes. The snippet in the value is refused by the verifier,
        // so this is what keeps the emitter's own output on the wire. The "dd. " prefix belongs to
        // the snippet too.
        String src = """
            Objecttype de Aanmelding
                de meldstip Datum en tijd in millisecondes;
                is tijdig kenmerk (bijvoeglijk);
            Regel tijdig
                geldig altijd
                    Een Aanmelding is tijdig
                    indien zijn meldstip eerder is dan %s.
            """;
        // emitRaw, not emitCompact: the snippet's own space is part of what the source snippet must keep.
        String c = emitRaw(src.formatted("15-03-2026 12:00:00.000"));
        java.util.regex.Pattern literal = java.util.regex.Pattern.compile(
                "\\{\\s*\"node\"\\s*:\\s*\"Literal\",\\s*\"value\"\\s*:\\s*\\{\\s*\"dateTime\"\\s*:\\s*\"([^\"]*)\"\\s*\\},\\s*"
                + "\"type\"\\s*:\\s*\\{\\s*\"node\"\\s*:\\s*\"Date\",\\s*\"precision\"\\s*:\\s*\"millisecond\"\\s*\\},"
                + "\\s*\"source\"\\s*:\\s*\\{\\s*\"snippet\"\\s*:\\s*\"([^\"]*)\"");
        java.util.regex.Matcher m = literal.matcher(c);
        assertTrue(m.find(), () -> "geen datum-tijd-literal in de emissie:\n" + c);
        assertEquals("2026-03-15T12:00:00.000", m.group(1), "de waarde draagt het ISO-lexeme");
        assertEquals("15-03-2026 12:00:00.000", m.group(2), "source draagt de bron-snippet, spatie inbegrepen");
        String d = emitRaw(src.formatted("dd. 5-3-2026 09:07:05.004"));
        java.util.regex.Matcher md = literal.matcher(d);
        assertTrue(md.find(), () -> "geen datum-tijd-literal in de emissie:\n" + d);
        assertEquals("2026-03-05T09:07:05.004", md.group(1),
                "de-dato-prefix en eencijferige dag/maand horen bij de snippet, niet bij het lexeme");
        assertEquals("dd. 5-3-2026 09:07:05.004", md.group(2));
    }

    @Test void datumtijd_literal_buiten_de_kalender_is_een_fout() {
        // The DATE token admits 30-02-2026 12:00:00.000; there is no ISO lexeme for it, so the emitter
        // reports an ERROR (which stops emission) instead of writing a lexeme WF-095 would refuse.
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype de Aanmelding
                de meldstip Datum en tijd in millisecondes;
                is tijdig kenmerk (bijvoeglijk);
            Regel tijdig
                geldig altijd
                    Een Aanmelding is tijdig
                    indien zijn meldstip eerder is dan 30-02-2026 12:00:00.000.
            """);
        assertEquals(1, m.errors().size(), () -> "verwacht precies een fout, kreeg: " + m.errors());
        assertTrue(m.errors().toString().contains("datum-tijd-literal '30-02-2026 12:00:00.000'"),
                () -> "de fout noemt het lexeme niet: " + m.errors());
    }

    /** A model whose one rule compares a Datum-in-dagen attribute with the literal {@code lexeem}. */
    private static RegelSpraakModel datumLiteralModel(String lexeem) {
        return Resolver.resolve("""
            Objecttype de Aanmelding
                de meldatum Datum in dagen;
                is tijdig kenmerk (bijvoeglijk);
            Regel tijdig
                geldig altijd
                    Een Aanmelding is tijdig
                    indien zijn meldatum eerder is dan %s.
            """.formatted(lexeem));
    }

    @Test void datum_literal_buiten_de_kalender_is_een_fout() {
        // The bare-date sister of the datum-tijd test above: the DATE token admits 30-02-2026 too,
        // and the shared reader answers null. That null once reached .toString() unguarded, so the
        // frontend died on a NullPointerException instead of naming the lexeme.
        RegelSpraakModel m = datumLiteralModel("30-02-2026");
        assertEquals(java.util.List.of("datum-literal '30-02-2026' is geen geldige datum (§13.2.19)"),
                m.errors().stream().map(RegelSpraakModel.Diagnostic::message).toList());
    }

    @Test void datum_literal_met_meercijferige_dag_is_een_fout() {
        // The DATE token takes DIGIT+ for day and month, and the shared reader searches with find():
        // 2024-01-2026 matched its ISO arm as 2024-01-20 and 123-01-2026 its day-first arm as
        // 23-01-2026, both silently. The frontend now demands the whole lexeme be a d-m-jjjj date.
        for (String lexeem : new String[] {"2024-01-2026", "123-01-2026", "1-123-2026"}) {
            RegelSpraakModel m = datumLiteralModel(lexeem);
            assertEquals(java.util.List.of("datum-literal '" + lexeem + "' is geen geldige datum (§13.2.19)"),
                    m.errors().stream().map(RegelSpraakModel.Diagnostic::message).toList(), lexeem);
        }
        assertTrue(datumLiteralModel("dd. 1-2-2026").errors().isEmpty(), "het dd.-voorvoegsel blijft gelezen");
    }

    /** A model whose one rule carries the validity {@code geldig}. */
    private static RegelSpraakModel geldigheidModel(String geldig) {
        return Resolver.resolve("""
            Objecttype de Vondst
                de waarde Numeriek (geheel getal);
            Regel waarde
                geldig %s
                    De waarde van een Vondst moet gesteld worden op 1.
            """.formatted(geldig));
    }

    private static String tijdcomponentFout(String grens) {
        return "versiegeldigheidsgrens '" + grens + "' draagt een tijdcomponent; de geldigheid is datumgranulair "
                + "(verwacht een datum dd-mm-jjjj of een jaartal van vier cijfers)";
    }

    private static String grensFout(String grens) {
        return "ongeldige versiegeldigheidsgrens '" + grens
                + "' (verwacht een datum dd-mm-jjjj of een jaartal van vier cijfers)";
    }

    @Test void versiegeldigheid_met_tijdcomponent_is_een_fout() {
        // The validity window is day-granular (Geldigheid holds LocalDates), so a bound written
        // with a time of day has no carrier. It used to be read by a find() that stopped after
        // the date and emitted "vanaf": "2026-01-01" with the 10:00 silently gone.
        String[][] gevallen = {{"vanaf 01-01-2026 10:00:00.000", "01-01-2026 10:00:00.000"},
                               {"t/m 31-12-2026 23:59:59.999", "31-12-2026 23:59:59.999"},
                               {"vanaf 2025 t/m 31-12-2026 10:00:00.000", "31-12-2026 10:00:00.000"}};
        for (String[] g : gevallen) {
            assertEquals(java.util.List.of(tijdcomponentFout(g[1])),
                    geldigheidModel(g[0]).errors().stream().map(RegelSpraakModel.Diagnostic::message).toList(), g[0]);
        }
    }

    @Test void versiegeldigheid_met_onmogelijke_datum_en_tijd_meldt_beide() {
        // Both defects are reported at once: reporting only the time component would send the
        // author round a second time to learn the date itself does not exist.
        assertEquals(java.util.List.of(grensFout("30-02-2026 10:00:00.000"), tijdcomponentFout("30-02-2026 10:00:00.000")),
                geldigheidModel("vanaf 30-02-2026 10:00:00.000").errors().stream()
                        .map(RegelSpraakModel.Diagnostic::message).toList());
    }

    @Test void versiegeldigheid_met_meercijferige_dag_is_een_fout() {
        // The bound reader searched for d-m-jjjj with find(), so 2024-01-2026 was read as
        // 24-01-2026 and emitted "vanaf": "2026-01-24" without a word. The whole lexeme counts now.
        for (String grens : new String[] {"2024-01-2026", "123-01-2026"}) {
            assertEquals(java.util.List.of(grensFout(grens)),
                    geldigheidModel("vanaf " + grens).errors().stream().map(RegelSpraakModel.Diagnostic::message).toList(),
                    grens);
        }
        assertTrue(geldigheidModel("vanaf dd. 1-1-2026").errors().isEmpty(), "het dd.-voorvoegsel blijft gelezen");
    }

    @Test void beslistabel_met_tijdcomponent_in_geldigheid_meldt_eenmaal() {
        // A beslistabel desugars to one regel per data row, each re-parsed with the header's
        // validity, so the bound is checked once per row. The author wrote it once.
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype de Vondst
                de waarde Numeriek (geheel getal);
                het gewicht Numeriek (geheel getal);
            Beslistabel Vondstwaarde
                geldig vanaf 01-01-2026 10:00:00.000
            | | de waarde van een Vondst moet gesteld worden op | indien zijn gewicht groter is dan |
            |---|---|---|
            | 1 | 1 | 5 |
            | 2 | 2 | 10 |
            """);
        assertEquals(java.util.List.of(tijdcomponentFout("01-01-2026 10:00:00.000")),
                m.errors().stream().map(RegelSpraakModel.Diagnostic::message).toList());
    }

    /** A Tekst-attribuut plus one rule that assigns {@code lexeem} to it — the smallest model that
     *  puts a text literal on the wire. The lexeme is inserted RAW, so a test can hand it a source
     *  form the grammar rejects and assert on the diagnostics. */
    private static String tekstBron(String lexeem) {
        return """
            Objecttype de Vondst
                het etiket Tekst;
            Regel etiket
                geldig altijd
                    Het etiket van een Vondst moet gesteld worden op %s.
            """.formatted(lexeem);
    }

    @Test void tekst_literal_lexeme_is_de_geparste_waarde_snippet_in_bron() {
        // uchar, maintainer decision 2026-09-18 (IR-SPEC ch. 4.1): a Tekst literal's value is the
        // PARSED string — what stands BETWEEN the delimiters — not the source snippet with its own
        // outer '"' still attached. The delimited form ("\"Flevoland\"") gave v0 two lexeme
        // families for one text value, and since all four consumers compare Tekst verbatim, an
        // emitted golden only played when its case input carried the delimiters too. Third in the
        // bare-lexeme row beside WF-081 (number) and WF-084 (date); the source record keeps the snippet.
        String c = emitCompact(tekstBron("\"Flevoland\""));
        assertTrue(c.contains(
                "{\"node\":\"Literal\",\"value\":{\"text\":\"Flevoland\"},\"type\":{\"node\":\"Text\"},\"source\":{\"snippet\":\"\\\"Flevoland\\\"\"}}"),
                () -> "de tekst-literal moet de geparste waarde dragen, de bron-snippet in source:\n" + c);
        assertFalse(c.contains("\"text\":\"\\\"Flevoland\\\"\""),
                () -> "de buitenste aanhalingstekens mogen niet in de literalwaarde zitten:\n" + c);
    }

    @Test void tekst_literal_grammatica_kent_geen_escape_dus_nooit_een_ingebed_aanhalingsteken() {
        // WHY the unquoting is total and lossless, measured at the grammar rather than assumed:
        // tekstWaarde is STRING_START tekstReeksDeel* STRING_END and a literal chunk is STRING_TEXT
        // ~["«»\r\n]+ (RegelSpraakLexer.g4). There is NO escape sequence, so a '"' always CLOSES the
        // string and a chunk can never hold one. Dropping the delimiters therefore removes exactly
        // the delimiters — it can never eat a character the author wrote, and it never unescapes.
        // The consequence this pins: a wire value with an embedded double quote (merk "AA", which
        // corpus/golden/jutterswet-keurmerk carries) is HAND-AUTHORED by construction — this emitter
        // cannot write one. What both back-ends do with such a value stays verbatim; that flank is
        // pinned by LiteralenTest.tekstOfEnumLabel_leest_tekst_verbatim and the Drools dual-branch
        // witnesses, not here.
        RegelSpraakModel m = Resolver.resolve(tekstBron("\"merk \"AA\"\""));
        assertFalse(m.errors().isEmpty(),
                "de grammatica kent geen escape: een tweede aanhalingsteken sluit de string en de rest is syntaxfout");
        assertTrue(m.errors().toString().contains("syntaxfout"),
                () -> "verwacht een syntaxfout, kreeg: " + m.errors());
    }

    @Test void tekst_literal_normaliseert_verder_niets() {
        // "Outer delimiters off, nothing else" — taken from the STRING_TEXT tokens, not from
        // bron.snippet() minus its first and last char. DefinitionResolver.src collapses every
        // whitespace run to one space, so a snippet-derived value silently rewrote a double space;
        // the snippet still shows that collapse, which is exactly why the two fields differ here. A
        // backslash and a single quote are ordinary characters and stay as written; an empty string
        // stays empty (zero STRING_TEXT chunks, not a two-character lexeme).
        String raw = emitRaw(tekstBron("\"a  b\""));
        assertTrue(raw.contains("\"text\": \"a  b\""),
                () -> "de dubbele spatie binnen de delimiters hoort bij de waarde:\n" + raw);
        assertTrue(raw.contains("\"snippet\": \"\\\"a b\\\"\""),
                () -> "source blijft de (whitespace-gevouwen) bronsnippet:\n" + raw);

        String backslash = emitCompact(tekstBron("\"pad\\naar\""));
        assertTrue(backslash.contains("\"text\":\"pad\\\\naar\""),
                () -> "een backslash is een gewoon teken, niet een escape-inleiding:\n" + backslash);

        String enkel = emitRaw(tekstBron("\"het 'merk'\""));
        assertTrue(enkel.contains("\"text\": \"het 'merk'\""),
                () -> "enkele aanhalingstekens zijn gewone tekens in een Tekst-literal:\n" + enkel);

        String leeg = emitCompact(tekstBron("\"\""));
        assertTrue(leeg.contains("{\"node\":\"Literal\",\"value\":{\"text\":\"\"},\"type\":{\"node\":\"Text\"},\"source\":{\"snippet\":\"\\\"\\\"\"}}"),
                () -> "de lege tekst is leeg, niet twee aanhalingstekens:\n" + leeg);
    }

    /** A domain plus a Vondst whose {@code oordeel} is of that domain and whose {@code etiket} is Tekst,
     *  with {@code regels} appended — the smallest model that puts an enumeratiewaarde on the wire in
     *  each of the positions that can (and cannot) supply a domain. */
    private static String enumBron(String regels) {
        return """
            Domein Keurklasse is van het type Enumeratie
                'gaaf'
                'beschadigd'
            Objecttype de Vondst
                de staat Keurklasse;
                het oordeel Keurklasse;
                het etiket Tekst;
                is veilbaar kenmerk (bijvoeglijk);
            %s
            """.formatted(regels);
    }

    /** The one enumeratiewaarde shape the wire admits: the BARE label (WF-039 crosses it byte-exactly
     *  against the declared domain) typed with the domain the position names (WF-034 (r) refuses the
     *  unknown sentinel), the delimited source lexeme in the source snippet. */
    private static final String GAAF_GETYPEERD =
            "{\"node\":\"Literal\",\"value\":{\"enumeration\":\"gaaf\"},"
            + "\"type\":{\"node\":\"Enumeration\",\"domain\":\"Keurklasse\",\"values\":[\"gaaf\",\"beschadigd\"]},"
            + "\"source\":{\"snippet\":\"'gaaf'\"}}";

    @Test void enum_literal_krijgt_zijn_domein_van_het_schrijfdoel() {
        // uchar V8 (spec/proposals/V1-WIREMIGRATIE.md, maintainer decision 2026-09-19): an
        // enumeratiewaarde reaches the wire as the bare label typed with its domain. The delimited form
        // ("'gaaf'", type the unknown sentinel) is doubly ill-formed — WF-039 crosses the
        // label byte-exactly against the declared value list and WF-034 (r) refuses the sentinel —
        // so the frontend could not emit the construct at all and no bron.rgs golden carried one.
        // Derivation rule, first half: the domain comes from the WRITE TARGET, i.e. the declared
        // datatype of the attribute being assigned (§13.3.4.2 / WF-023's keuzetype clause).
        String c = emitCompact(enumBron("""
            Regel oordeel
                geldig altijd
                    Het oordeel van een Vondst moet gesteld worden op 'gaaf'.
            """));
        assertTrue(c.contains(GAAF_GETYPEERD),
                () -> "het schrijfdoel moet het domein van de enumeratiewaarde leveren:\n" + c);
        assertFalse(c.contains("\"enumeration\":\"'gaaf'\""),
                () -> "de delimiters mogen niet in het literal-lexeme zitten:\n" + c);
    }

    @Test void enum_literal_krijgt_zijn_domein_van_de_andere_vergelijkingszijde() {
        // Derivation rule, second half: in a comparison the domain comes from the OTHER side — the
        // same context §13.3.4.2's membership check already reads (RuleChecker.meldVergelijk crosses
        // the label against the sibling's domain). Both directions, since the rule is symmetric:
        // the marker is promoted whether it stands left or right of the operator.
        String rechts = emitCompact(enumBron("""
            Regel veilbaarheid
                geldig altijd
                    Een Vondst is veilbaar indien zijn staat gelijk is aan 'gaaf'.
            """));
        assertTrue(rechts.contains(GAAF_GETYPEERD),
                () -> "de linkerzijde moet het domein van de rechter enumeratiewaarde leveren:\n" + rechts);

        String links = emitCompact(enumBron("""
            Regel veilbaarheid
                geldig altijd
                    Een Vondst is veilbaar indien 'gaaf' gelijk is aan zijn staat.
            """));
        assertTrue(links.contains(GAAF_GETYPEERD),
                () -> "de rechterzijde moet het domein van de linker enumeratiewaarde leveren:\n" + links);
    }

    @Test void enum_literal_door_de_letketen_van_een_variabelendeel() {
        // §11/ch. 4.10: a value written through the variabelendeel denotes exactly what the bare literal
        // denotes (ch. 10.6), so it must reach the wire in the same shape — and the whole chain must, not
        // just its value: the Let is typed FROM the value, so an unpromoted chain would leave an unknown
        // type at two positions the wire has no spelling for, and the emitter would refuse the model. The
        // binder read carries no type of its own; its binding is its declaration. This is also what keeps
        // the variabelendeel diagnostically transparent (VariabeledeelLoweringTest pins that the
        // §13.3.4.2 membership check says the same thing through the chain as beside it).
        String c = emitCompact(enumBron("""
            Regel oordeel
                geldig altijd
                    Het oordeel van een Vondst moet gesteld worden op de tint.
                    Daarbij geldt:
                        de tint is 'gaaf'.
            """));
        String enumType = "{\"node\":\"Enumeration\",\"domain\":\"Keurklasse\",\"values\":[\"gaaf\",\"beschadigd\"]}";
        assertTrue(c.contains("\"node\":\"Let\",\"type\":" + enumType + ",\"binder\":\"tint\",\"value\":"
                        + GAAF_GETYPEERD + ",\"body\":{\"node\":\"BinderRef\",\"name\":\"tint\",\"source\":"),
                () -> "de hele letketen moet het domein van het schrijfdoel dragen:\n" + c);
    }

    @Test void het_domein_draagt_zijn_naam_op_de_wrapper_en_de_referentie_houdt_hem() {
        // M1 (delta B2, IR-SPEC ch. 2.3): the Domain wrapper writes the name once, so the Enumeration at the
        // declaration position carries no domain; at a reference position domain is the domain reference
        // and stays.
        String c = emitCompact(enumBron(""));
        assertTrue(c.contains("\"domains\":[{\"node\":\"Domain\",\"name\":\"Keurklasse\","
                        + "\"datatype\":{\"node\":\"Enumeration\",\"values\":[\"gaaf\",\"beschadigd\"]}}]"),
                () -> "de declaratiepositie hoort geen zelfnaam te dragen:\n" + c);
        assertTrue(c.contains("{\"node\":\"Attribute\",\"name\":\"staat\",\"datatype\":{\"node\":\"Enumeration\","
                        + "\"domain\":\"Keurklasse\","),
                () -> "de referentiepositie houdt haar domeinverwijzing:\n" + c);
    }

    @Test void een_ongelabeld_kenmerk_is_zelfstandig_en_een_zijkaart_zonder_lid_wordt_geweigerd() {
        // The model keeps its member-keyed side-maps; the wire carries the facets on the member nodes, so
        // what those nodes cannot carry is refused instead of dropped.
        RegelSpraakModel m = Resolver.resolve(enumBron(""));
        assertTrue(m.errors().isEmpty(), () -> "onverwachte fouten: " + m.errors());
        var vondst = m.objecttypen.get("Vondst");
        vondst.kenmerkSoorten.remove("veilbaar");
        String c = IrEmitter.emit(m).replaceAll("\\s+", "");
        assertTrue(c.contains("{\"node\":\"Characteristic\",\"name\":\"veilbaar\",\"kind\":\"nominal\"}"),
                () -> "een kenmerk zonder soort is zelfstandig (ch. 2.2):\n" + c);

        vondst.attribuutMeervoud.put("gewicht", "gewichten");
        IllegalStateException wees = assertThrows(IllegalStateException.class, () -> IrEmitter.emit(m));
        assertTrue(wees.getMessage().contains("Vondst.attribuutMeervoud"), wees::getMessage);
        vondst.attribuutMeervoud.remove("gewicht");

        vondst.attribuutTijdlijnAantal.put("staat", 2);
        IllegalStateException zonderRaster = assertThrows(IllegalStateException.class, () -> IrEmitter.emit(m));
        assertTrue(zonderRaster.getMessage().contains("Vondst.attribuutTijdlijnAantal"), zonderRaster::getMessage);
    }

    @Test void enum_literal_zonder_domeincontext_blijft_een_luide_marker() {
        // The fail-fast half of the rule: where NEITHER a write target nor a comparison sibling names
        // an Enumeratie, the frontend has no domain to derive and emits the honest marker rather than
        // guessing one. A Tekst target is exactly such a position. The marker is loud — Resolver's
        // marker discipline (2026-09-19) raises a WARNING beside every Onopgelost that survives into
        // the finished model — and it carries the delimited source lexeme, so the author sees which
        // one stayed unresolved. A guessed domain would be worse than a marker: it would put a label
        // on the wire under a domain nobody declared for this position.
        RegelSpraakModel m = Resolver.resolve(enumBron("""
            Regel etiket
                geldig altijd
                    Het etiket van een Vondst moet gesteld worden op 'gaaf'.
            """));
        assertTrue(m.errors().isEmpty(), () -> "onverwachte fouten: " + m.errors());
        String c = IrEmitter.emit(m).replaceAll("\\s+", "");
        // IR-SPEC ch. 4.4: a marker whose type is unknown carries no type at all
        assertTrue(c.contains("{\"node\":\"Unresolved\",\"source\":{\"snippet\":\"'gaaf'\"}}"),
                () -> "een enumeratiewaarde zonder domeincontext hoort een marker te zijn:\n" + c);
        assertTrue(m.diagnostics.stream().anyMatch(d -> !d.isError()
                        && d.toString().contains("een enumeratiewaarde ontleent haar domein aan de context")),
                () -> "de marker moet luid zijn (WARNING met reden): " + m.diagnostics);
    }

    @Test void enum_literal_label_komt_uit_de_tokentekst_niet_uit_de_gevouwen_snippet() {
        // WHERE the label is read, measured at the grammar: ENUM_LITERAL is ONE lexer token
        // ('\'' ~['\r\n]* '\'', RegelSpraakLexer.g4), so the token text IS the whole source slice and
        // the delimiters are its first and last character — stripping them is total and lossless.
        // DefinitionResolver.src, which every other bron goes through, folds each whitespace run to a
        // single space; the DECLARATION does not (enumeratie() takes the token text), so a folded
        // label would not equal the declared value and WF-039 would refuse a document the frontend
        // itself wrote. Reading the token keeps wire and declaration byte-identical.
        String raw = emitRaw("""
            Domein Kleur is van het type Enumeratie
                'licht  blauw'
                'rood'
            Objecttype de Jutter
                de kleur Kleur;
            Regel kleur
                geldig altijd
                    De kleur van een Jutter moet gesteld worden op 'licht  blauw'.
            """);
        assertTrue(raw.contains("\"enumeration\": \"licht  blauw\""),
                () -> "het label moet dezelfde witruimte-run houden als de declaratie:\n" + raw);
        assertTrue(raw.contains("\"snippet\": \"'licht  blauw'\""),
                () -> "de source-snippet is de tokentekst, niet de gevouwen snippet:\n" + raw);
        assertFalse(raw.contains("licht blauw"),
                () -> "geen enkele positie mag de gevouwen vorm dragen — declaratie en label zouden uiteenlopen:\n" + raw);
    }

    @Test void tijdsevenredig_deel_draagt_basistype_geen_sentinel() {
        // §7.3.2/§13.4.17.58: the pro-rata node carries its BASE's value type on the wire; a derived
        // position the frontend cannot type would be written null, which is not what this node is.
        String src = """
            Objecttype de Bakenmeester (mv: Bakenmeesters) (bezield)
                het onderhoudstarief Numeriek (getal met 2 decimalen) voor elk jaar;
                de onderhoudsbijdrage Numeriek (getal met 2 decimalen) voor elke maand;
                is dienstdoend kenmerk (bijvoeglijk) voor elke maand;
            Regel onderhoudsbijdrage
                geldig altijd
                    De onderhoudsbijdrage van een Bakenmeester moet gesteld worden op het tijdsevenredig deel per maand van zijn onderhoudstarief gedurende de tijd dat hij dienstdoend is.
            """;
        String c = emitCompact(src);
        int at = c.indexOf("\"kind\":\"timeProportionalPart\"");
        assertTrue(at >= 0, () -> "de pro-rata-node ontbreekt:\n" + c);
        // scope to the node's OWN type field: it sits between the node's granularity
        // (the first one after kind — the argument refs and the CharacteristicCheck filter carry none) and
        // its source, so the base argument's type object cannot satisfy the assertion.
        int from = c.indexOf("\"granularity\"", at);
        int to = c.indexOf("\"source\"", from);
        assertTrue(from > at && to > from, () -> "de node-veldvolgorde is onverwacht:\n" + c);
        String ownType = c.substring(from, to);
        assertTrue(ownType.contains("\"type\":{\"node\":\"Number\"") && ownType.contains("\"decimals\":2"),
                () -> "de pro-rata-node draagt het basistype (een getal met 2 decimalen) niet zelf:\n" + ownType);
        assertFalse(ownType.contains("\"type\":null"),
                () -> "de pro-rata-node mag geen ontypeerbaar resultaat dragen:\n" + ownType);
    }

    @Test void bron_annotaties_op_de_draad_heel_of_niet() {
        // uchar WF-082 (IR-SPEC ch. 3.1): the // Bron: statuutannotaties above a rule ride the
        // Rule as the optional statuteAnnotations array (after its versions), in source order;
        // a rule WITHOUT annotations leaves no trace, and a blank annotation is dropped (an
        // empty or blank form is verifier-ill-formed — written whole or not at all).
        String src = """
            Objecttype de Jutter
                de vondstteller Numeriek (geheel getal);
                het drempeloverschot Numeriek (geheel getal);
                het hardheidsoverschot Numeriek (geheel getal);
                de jaarsom Numeriek (geheel getal);
            // Bron: art. 3 Jutterswet
            // Bron: art. 5 Jutterswet
            Regel hardheidsoverschot
                geldig altijd
                    Het hardheidsoverschot van een Jutter moet berekend worden als de vondstteller van de Jutter min 6.
            Regel jaarsom
                geldig altijd
                    De jaarsom van een Jutter moet berekend worden als het drempeloverschot van de Jutter plus het hardheidsoverschot van de Jutter.
            //Bron:
            Regel drempeloverschot
                geldig altijd
                    Het drempeloverschot van een Jutter moet berekend worden als de vondstteller van de Jutter min 12.
            """;
        String c = emitCompact(src);
        // emitCompact strips ALL whitespace, also inside string values.
        assertTrue(c.contains("\"statuteAnnotations\":[\"art.3Jutterswet\",\"art.5Jutterswet\"]}"),
                () -> "beide annotaties moeten in bronvolgorde na de versies staan:\n" + c);
        assertEquals(c.indexOf("statuteAnnotations"), c.lastIndexOf("statuteAnnotations"),
                () -> "alleen de geannoteerde regel draagt het veld (geen spoor bij afwezigheid, blanco vervalt):\n" + c);
    }

    /**
     * IR-SPEC ch. 4.10 — {@code IfThenElse} is NEVER EMITTER-PRODUCED, and this pins what that means for this
     * class. No RegelSpraak construct lowers to the node, so the resolver builds none and no golden's re-emission
     * can meet one; the sealed {@code Expr} union nevertheless forces an arm here, and that arm REFUSES rather
     * than serialising. The reason is that an emit path nothing exercises could carry a wrong key order or a
     * wrong field name — the wire spells them {@code condition}/{@code then}/{@code else} — and no gate would
     * ever read those bytes: a loud refusal cannot be silently wrong, a dead serialiser can. Pinned as a TYPE
     * and a reason, so the choice is checked instead of merely commented.
     *
     * <p>The node is grafted onto an otherwise resolver-built model, because the resolver itself will not
     * produce one — which is the claim under test.
     */
    @Test void een_ifthenelse_wordt_geweigerd_want_de_emitter_produceert_hem_nooit() {
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype de Jutter
                de vondstteller Numeriek (geheel getal);
                de jaarsom Numeriek (geheel getal);
            Regel jaarsom
                geldig altijd
                    De jaarsom van een Jutter moet berekend worden als de vondstteller van de Jutter min 6.
            """);
        assertTrue(m.errors().isEmpty(), () -> "onverwachte fouten: " + m.errors());
        assertEquals(1, m.resolvedRegels.size());
        // sanity: the resolver produced no such node by itself — "never emitter-produced" starts there.
        ResolvedRegel origineel = m.resolvedRegels.get(0);
        assertFalse(Expr.bevat(origineel.waarde(), x -> x instanceof Expr.IfThenElse),
                "de resolver bouwt de vorm niet");

        Expr keuze = new Expr.IfThenElse(
                new Conditie.Vergelijking(Conditie.Operator.GROTER, origineel.waarde(),
                        new Expr.Literal("0", origineel.waarde().type(), Bron.synthetisch("0")),
                        Bron.synthetisch("groter dan 0")),
                origineel.waarde(), origineel.waarde(), origineel.waarde().type(),
                Bron.synthetisch("als … dan … anders …"));
        m.resolvedRegels.set(0, new ResolvedRegel(origineel.naam(), origineel.soort(), origineel.doel(),
                keuze, origineel.voorwaarde(), origineel.geldigheid()));

        UnsupportedOperationException geweigerd =
                assertThrows(UnsupportedOperationException.class, () -> IrEmitter.emit(m),
                        "de emitter schrijft deze vorm niet; hij weigert haar");
        assertTrue(geweigerd.getMessage().contains("never emitter-produced"),
                () -> "de weigering noemt de reden: " + geweigerd.getMessage());
    }

    /** WF-113 (a) at the producer boundary: the wire carries a diagnostic only as a warning whose message is
     *  neither empty nor blank and whose line is at least 0, and the verifier refuses every other shape. The
     *  resolver writes none of the twisted diagnostics below, so each is grafted onto a resolver-built model;
     *  that the emitter refuses them by itself is the claim. Until 2026-09-23 nothing pinned INFO or a null
     *  severity (a guard narrowed to ERROR kept the suite green), and a blank message or line -1 was written. */
    @Test void wf113a_de_emitter_schrijft_alleen_wat_de_verifier_als_diagnose_aanneemt() {
        String bron = """
            Objecttype de Jutter
                de vondstteller Numeriek (geheel getal);
            """;
        RegelSpraakModel goed = Resolver.resolve(bron);
        assertTrue(goed.diagnostics.isEmpty(), () -> "onverwachte diagnoses: " + goed.diagnostics);
        goed.diagnostics.add(new Diagnostic(Severity.WARNING, "een melding", 0));
        String c = IrEmitter.emit(goed).replaceAll("\\s+", "");
        assertTrue(c.contains("\"diagnostics\":[{\"node\":\"Diagnostic\",\"severity\":\"warning\","
                        + "\"message\":\"eenmelding\",\"line\":0}]"),
                () -> "een WARNING met een melding op regel 0 hoort geschreven te worden:\n" + c);

        for (Diagnostic d : List.of(
                new Diagnostic(Severity.INFO, "een melding", 1),
                new Diagnostic(null, "een melding", 1),
                new Diagnostic(Severity.WARNING, "", 1),
                new Diagnostic(Severity.WARNING, "   ", 1),
                new Diagnostic(Severity.WARNING, null, 1),
                new Diagnostic(Severity.WARNING, "een melding", -1))) {
            RegelSpraakModel m = Resolver.resolve(bron);
            m.diagnostics.add(d);
            IllegalStateException e = assertThrows(IllegalStateException.class, () -> IrEmitter.emit(m),
                    () -> "de emitter schreef " + d);
            assertTrue(e.getMessage().contains("WF-113 (a)"), e::getMessage);
        }
    }

    /** WF-113 (b) at the producer boundary: a marker is accounted for by a warning that quotes its source snippet
     *  verbatim, so a marker whose snippet is null or blank can be accounted for by nothing and the verifier refuses
     *  the document. The frontend can build one ({@code DefinitionResolver.bron} gives an empty snippet for a
     *  missing context or an empty match); the emitter refuses to write it, at either marker node, with a WARNING
     *  on the model that a literal reading of an empty snippet would count as its account. An empty snippet is
     *  refused before WF-113 is asked, in the sentence every reader gives for it at load. */
    @Test void wf113b_de_emitter_schrijft_geen_marker_zonder_bron() {
        for (Bron bron : new Bron[] { Bron.synthetisch(""), Bron.synthetisch("   "), Bron.synthetisch(null), null }) {
            for (boolean conditie : new boolean[] { false, true }) {
                RegelSpraakModel m = Resolver.resolve("""
                    Objecttype de Jutter
                        de vondstteller Numeriek (geheel getal);
                        de jaarsom Numeriek (geheel getal);
                    Regel jaarsom
                        geldig altijd
                            De jaarsom van een Jutter moet berekend worden als de vondstteller van de Jutter min 6.
                    """);
                assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnoses: " + m.diagnostics);
                ResolvedRegel r = m.resolvedRegels.get(0);
                m.resolvedRegels.set(0, conditie
                        ? new ResolvedRegel(r.naam(), r.soort(), r.doel(), r.waarde(),
                                new Conditie.Onopgelost(bron, "test"), r.geldigheid())
                        : new ResolvedRegel(r.naam(), r.soort(), r.doel(),
                                new Expr.Onopgelost(r.waarde().type(), bron, "test"), r.voorwaarde(), r.geldigheid()));
                m.diagnostics.add(new Diagnostic(Severity.WARNING, "een melding", 1));
                IllegalStateException e = assertThrows(IllegalStateException.class, () -> IrEmitter.emit(m),
                        () -> "de emitter schreef een " + (conditie ? "conditie" : "expressie") + "marker met bron "
                                + (bron == null ? null : "'" + bron.snippet() + "'"));
                assertTrue(e.getMessage().contains(bron != null && "".equals(bron.snippet())
                        ? "a source snippet is never empty" : "WF-113 (b)"), e::getMessage);
            }
        }
    }

    // ---------------- L2: the reference layer carries its symbolic form only ----------------

    @Test void een_rolcheck_gaat_aan_beide_zijden_als_rolemembership_de_wire_op() {
        // IR-SPEC ch. 4.5: one node for the role check, whichever end of the fact type holds the reference —
        // the fact type and the role the SUBJECT plays in it, by declared name, in the schema's key order.
        // The near side ("hij is een wachtjutter": the Jutter plays wachtjutter) and the far side ("hij heeft
        // een wachtjutter": the Wachtpost plays toegewezen wachtpost) differ only in that role.
        String c = emitCompact("""
            Objecttype de Jutter (mv: Jutters) (bezield)
                is wachtdoend kenmerk (bijvoeglijk);
            Objecttype de Wachtpost (mv: Wachtposten)
                is bemand kenmerk (bijvoeglijk);
            Feittype wachtindeling
                de wachtjutter (mv: wachtjutters) Jutter
                de toegewezen wachtpost Wachtpost
            Eén toegewezen wachtpost telt meerdere wachtjutters
            Regel wachtdoend
                geldig altijd
                    Een Jutter is wachtdoend indien hij een wachtjutter is.
            Regel bemand
                geldig altijd
                    Een Wachtpost is bemand indien hij een wachtjutter heeft.
            """);
        assertTrue(c.contains("\"condition\":{\"node\":\"RoleMembership\",\"factType\":\"wachtindeling\","
                        + "\"role\":\"wachtjutter\",\"negated\":false,\"source\":{\"snippet\":\"hijeenwachtjutteris\"}}"),
                () -> "de rolcheck aan de nabije zijde:\n" + c);
        assertTrue(c.contains("\"condition\":{\"node\":\"RoleMembership\",\"factType\":\"wachtindeling\","
                        + "\"role\":\"toegewezenwachtpost\",\"negated\":false,\"source\":{\"snippet\":\"hijeenwachtjutterheeft\"}}"),
                () -> "de rolcheck aan de verre zijde noemt de rol die het onderwerp speelt:\n" + c);
    }

    @Test void een_rolcheck_die_de_andere_zijde_vernauwt_draagt_het_type_van_die_zijde() {
        // IR-SPEC ch. 4.5, RoleMembership: a check whose other end names a STRICT subtype of that role's declared filler
        // ("heeft een Strandjutter", where wachtjutter is declared on Jutter) narrows the link it asks for, and
        // the wire says so in counterpartType; naming the declared filler itself narrows nothing and the field
        // stays absent. The frontend resolves a written role to its declared filler, so the narrowing is set on
        // the resolved record here, as the hand-written golden jutterswet-postbezetting carries it.
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype de Jutter (mv: Jutters) (bezield)
            Objecttype de Strandjutter is een Jutter (mv: Strandjutters) (bezield)
            Objecttype de Wachtpost (mv: Wachtposten)
                is bemand kenmerk (bijvoeglijk);
            Feittype wachtindeling
                de wachtjutter (mv: wachtjutters) Jutter
                de toegewezen wachtpost Wachtpost
            Eén toegewezen wachtpost telt meerdere wachtjutters
            Regel bemand
                geldig altijd
                    Een Wachtpost is bemand indien hij een wachtjutter heeft.
            """);
        assertTrue(m.errors().isEmpty(), () -> "onverwachte fouten: " + m.errors());
        ResolvedRegel r = m.resolvedRegels.get(0);
        assertTrue(r.voorwaarde() instanceof Conditie.VerreRollidmaatschap v && v.filler().naam.equals("Jutter"),
                () -> "de resolver hoort de verre rolcheck te bouwen: " + r.voorwaarde());
        String zonder = IrEmitter.emit(m).replaceAll("\\s+", "");
        assertTrue(zonder.contains("\"role\":\"toegewezenwachtpost\",\"negated\":false,"),
                () -> "de gedeclareerde vuller vernauwt niets:\n" + zonder);
        Conditie.VerreRollidmaatschap v = (Conditie.VerreRollidmaatschap) r.voorwaarde();
        m.resolvedRegels.set(0, new ResolvedRegel(r.naam(), r.soort(), r.doel(), r.waarde(),
                new Conditie.VerreRollidmaatschap(m.objecttypen.get("Strandjutter"), v.backref(), v.negatie(), v.bron()),
                r.geldigheid()));
        String met = IrEmitter.emit(m).replaceAll("\\s+", "");
        assertTrue(met.contains("{\"node\":\"RoleMembership\",\"factType\":\"wachtindeling\","
                        + "\"role\":\"toegewezenwachtpost\",\"counterpartType\":\"Strandjutter\",\"negated\":false,"),
                () -> "een strikt subtype van de gedeclareerde vuller vernauwt de link:\n" + met);
    }

    @Test void een_lezing_die_navigeert_zonder_unieke_binding_wordt_geweigerd() {
        // IR-SPEC ch. 4.1a: a read without bindings is the subject's own, so a read the resolver took to another
        // type but whose relation it cannot name has no spelling — written without its binding, it would read
        // the subject's own attribute. The pair is made ambiguous by a second fact type added after resolution,
        // so the read itself is the resolver's.
        RegelSpraakModel m = Resolver.resolve(WERKNEMER_AFDELING + """
            Regel rang
                geldig altijd
                    De rang van een Werknemer moet berekend worden als het niveau van zijn afdeling.
            """);
        assertTrue(m.errors().isEmpty(), () -> "onverwachte fouten: " + m.errors());
        assertTrue(IrEmitter.emit(m).replaceAll("\\s+", "").contains(
                "\"path\":[{\"node\":\"RoleBinding\",\"factType\":\"aanstelling\",\"role\":\"afdeling\"}]"),
                "de ongemoeide lezing draagt haar binding");
        tweedeAfdelingsrol(m);
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> IrEmitter.emit(m));
        assertTrue(e.getMessage().contains("het niveau van zijn afdeling") && e.getMessage().contains("navigeert"),
                e::getMessage);
    }

    @Test void een_kenmerkcheck_die_navigeert_zonder_unieke_binding_wordt_geweigerd() {
        // IR-SPEC ch. 4.5: the binding IS the hop; without it the check would test the subject's own kenmerk.
        RegelSpraakModel m = Resolver.resolve(WERKNEMER_AFDELING + """
            Regel rang
                geldig altijd
                    De rang van een Werknemer moet gesteld worden op 1 indien zijn afdeling actief is.
            """);
        assertTrue(m.errors().isEmpty(), () -> "onverwachte fouten: " + m.errors());
        assertTrue(IrEmitter.emit(m).replaceAll("\\s+", "").contains(
                "\"navigation\":{\"node\":\"RoleBinding\",\"factType\":\"aanstelling\",\"role\":\"afdeling\"}"),
                "de ongemoeide kenmerkcheck draagt haar binding");
        tweedeAfdelingsrol(m);
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> IrEmitter.emit(m));
        assertTrue(e.getMessage().contains("kenmerkcheck"), e::getMessage);
    }

    @Test void een_feitcreatie_zonder_heel_symbolisch_blok_wordt_geweigerd() {
        // IR-SPEC ch. 3.4: the creation IS its symbolic block, so a creation missing any binding has no spelling.
        RegelSpraakModel m = Resolver.resolve(FEITCREATIE);
        assertTrue(m.errors().isEmpty(), () -> "onverwachte fouten: " + m.errors());
        ResolvedFeitCreatie fc = m.feitCreaties.get(0);
        m.feitCreaties.set(0, new ResolvedFeitCreatie(fc.naam(), fc.anker(), fc.doel(), fc.zetVeld(), fc.joinVeld(),
                fc.tussenstappen(), fc.voorwaarde(), fc.geldigheid(), fc.feittype(), fc.ankerRol(), fc.doelRol(), null));
        IllegalStateException zonderJoin = assertThrows(IllegalStateException.class, () -> IrEmitter.emit(m));
        assertTrue(zonderJoin.getMessage().contains("niet heel symbolisch gebonden"), zonderJoin::getMessage);

        ResolvedFeitCreatie.Tussenstap t = fc.tussenstappen().get(0);
        m.feitCreaties.set(0, new ResolvedFeitCreatie(fc.naam(), fc.anker(), fc.doel(), fc.zetVeld(), fc.joinVeld(),
                List.of(new ResolvedFeitCreatie.Tussenstap(t.type(), t.veld(), t.viaOuderVeld())),
                fc.voorwaarde(), fc.geldigheid(), fc.feittype(), fc.ankerRol(), fc.doelRol(), fc.joinRol()));
        IllegalStateException zonderStap = assertThrows(IllegalStateException.class, () -> IrEmitter.emit(m));
        assertTrue(zonderStap.getMessage().contains("niet heel symbolisch gebonden"), zonderStap::getMessage);
    }

    @Test void een_begrensde_uniciteit_zonder_rol_wordt_geweigerd() {
        // IR-SPEC ch. 3.6: the binding IS the scoping; written without it the uniqueness would hold across
        // every instance instead of within each afdeling.
        RegelSpraakModel m = Resolver.resolve(WERKNEMER_AFDELING + """
            Regel uniek
                geldig altijd
                    De rang van alle Werknemer van de afdeling moeten uniek zijn.
            """);
        assertTrue(m.errors().isEmpty(), () -> "onverwachte fouten: " + m.errors());
        ResolvedUniciteit u = m.uniciteiten.get(0);
        assertNotNull(u.scopeRol(), "de ongemoeide uniciteit is gebonden");
        m.uniciteiten.set(0, new ResolvedUniciteit(u.naam(), u.subject(), u.sleutels(), u.scopeVeld(), u.bron(),
                u.geldigheid()));
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> IrEmitter.emit(m));
        assertTrue(e.getMessage().contains("geen unieke rol"), e::getMessage);
    }

    @Test void een_onbekend_type_is_null_op_een_afgeleide_positie_en_heeft_elders_geen_spelling() {
        // uchar-ir-v1 has no datatype for "unknown". A DERIVED position the frontend cannot type (a datum plus a
        // datum, reported and not refused) is written null; a marker leaves its type out; any other position —
        // a literal, a declaration, a payload — would carry a type the wire cannot spell, and is refused.
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype de Jutter
                de vondstteller Numeriek (geheel getal);
                de jaarsom Numeriek (geheel getal);
            Regel jaarsom
                geldig altijd
                    De jaarsom van een Jutter moet berekend worden als de vondstteller van de Jutter min 6.
            """);
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnoses: " + m.diagnostics);
        ResolvedRegel r = m.resolvedRegels.get(0);
        Expr.Binair b = (Expr.Binair) r.waarde();
        Datatype onbekend = new Datatype.Onbekend("test");
        m.resolvedRegels.set(0, new ResolvedRegel(r.naam(), r.soort(), r.doel(),
                new Expr.Binair(b.op(), b.links(), b.rechts(), onbekend, b.bron()), r.voorwaarde(), r.geldigheid()));
        String c = IrEmitter.emit(m).replaceAll("\\s+", "");
        assertTrue(c.contains("\"type\":null,\"source\":{\"snippet\":\"devondsttellervandeJutter"),
                () -> "een afgeleide positie zonder type schrijft null:\n" + c);
        assertFalse(c.contains("Onbekend"), () -> "het onbekende type heeft geen spelling:\n" + c);

        Expr.Literal zes = (Expr.Literal) b.rechts();
        m.resolvedRegels.set(0, new ResolvedRegel(r.naam(), r.soort(), r.doel(),
                new Expr.Binair(b.op(), b.links(), new Expr.Literal(zes.waarde(), onbekend, zes.bron()), b.type(), b.bron()),
                r.voorwaarde(), r.geldigheid()));
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> IrEmitter.emit(m));
        assertTrue(e.getMessage().contains("onbekend datatype"), e::getMessage);

        m.resolvedRegels.set(0, new ResolvedRegel(r.naam(), r.soort(), r.doel(),
                new Expr.Onopgelost(onbekend, b.bron(), "test"), r.voorwaarde(), r.geldigheid()));
        m.diagnostics.add(new Diagnostic(Severity.WARNING, "onopgelost: " + b.bron().snippet(), 1));
        String marker = IrEmitter.emit(m).replaceAll("\\s+", "");
        assertTrue(marker.contains("{\"node\":\"Unresolved\",\"source\":{\"snippet\":\"devondsttellervandeJuttermin6\"}}"),
                () -> "een marker van onbekend type draagt geen type:\n" + marker);
    }

    // ---------------- L4: the rule layer and the expression layer ----------------

    /** A rule with two versions, the model's shape of it: one entry per version, named "waarde #1" and "waarde #2". */
    private static final String TWEE_VERSIES = """
            Objecttype de Vondst
                de waarde Numeriek (geheel getal);
            Regel waarde
                geldig vanaf 2025 t/m 2025
                    De waarde van een Vondst moet gesteld worden op 1.
                geldig vanaf 2026
                    De waarde van een Vondst moet gesteld worden op 2.
            """;

    @Test void de_versies_van_een_regel_worden_een_vorm_met_zijn_versies_in_volgorde() {
        // IR-SPEC ch. 3, Versioning: the model keeps one entry per version and names them "<name> #<i>"; the wire
        // writes the rule once, under its own name, with its versions in the order of i and each its own validity.
        // No rgs golden and no document of the parity zone carries two versions, so this is the grouping's witness.
        RegelSpraakModel m = Resolver.resolve(TWEE_VERSIES);
        assertTrue(m.errors().isEmpty(), () -> "onverwachte fouten: " + m.errors());
        assertEquals(List.of("waarde #1", "waarde #2"), m.resolvedRegels.stream().map(ResolvedRegel::naam).toList(),
                "het model draagt één invoer per versie");
        String c = IrEmitter.emit(m).replaceAll("\\s+", "");
        String doel = "\"target\":{\"node\":\"AttributeReference\",\"attribute\":\"waarde\",\"subjectType\":\"Vondst\",";
        int eerste = c.indexOf("\"rules\":[{\"node\":\"Rule\",\"name\":\"waarde\",\"versions\":[{\"validity\":"
                + "{\"node\":\"Validity\",\"from\":\"2025-01-01\",\"to\":\"2025-12-31\"}," + doel);
        int tweede = c.indexOf("},{\"validity\":{\"node\":\"Validity\",\"from\":\"2026-01-01\"}," + doel);
        assertTrue(eerste >= 0 && tweede > eerste, () -> "één vorm met twee versies in volgorde verwacht:\n" + c);
        assertTrue(c.indexOf("\"value\":{\"number\":\"1\"}") > eerste && c.indexOf("\"value\":{\"number\":\"2\"}") > tweede,
                () -> "elke versie draagt haar eigen waarde:\n" + c);
        assertEquals(1, c.split("\"node\":\"Rule\"", -1).length - 1, () -> "precies één vorm:\n" + c);
        assertFalse(c.contains(" #") || c.contains("#1") || c.contains("#2"),
                () -> "het versieachtervoegsel is de spelling van het model, niet die van de wire:\n" + c);
    }

    @Test void een_versiegroep_die_de_wire_niet_kan_spellen_wordt_geweigerd() {
        // The grouping reads the model's names exactly as m4.py reads the flattened wire, and refuses what it
        // cannot spell rather than guessing a form: versions that do not stand together, a suffix on one entry
        // (a form with one version carries its bare name, so "x #1" alone would read as a version of "x"), numbers
        // that are not exactly 1 to n, a form name that itself ends in a suffix, and versions that carry
        // different statute annotations (the wire writes one list on the form).
        record Geval(String reden, List<String> namen, List<List<String>> annotaties) {}
        List<List<String>> geen = List.of(List.of(), List.of(), List.of());
        for (Geval g : List.of(
                new Geval("niet aaneengesloten", List.of("x #1", "y", "x #2"), geen),
                new Geval("op één invoer", List.of("x #1", "y", "z"), geen),
                new Geval("niet precies 1 tot en met", List.of("x #1", "x #3", "y"), geen),
                new Geval("niet precies 1 tot en met", List.of("x #01", "x #2", "y"), geen),
                new Geval("eindigt zelf op een versieachtervoegsel", List.of("x #1 #1", "x #1 #2", "y"), geen),
                new Geval("verschillende statuutannotaties", List.of("x #1", "x #2", "y"),
                        List.of(List.of("art. 1"), List.of("art. 2"), List.of())))) {
            RegelSpraakModel m = drieRegels();
            for (int i = 0; i < 3; i++) {
                ResolvedRegel r = m.resolvedRegels.get(i);
                m.resolvedRegels.set(i, new ResolvedRegel(g.namen().get(i), r.soort(), r.doel(), r.waarde(),
                        r.voorwaarde(), r.geldigheid(), g.annotaties().get(i)));
            }
            IllegalStateException e = assertThrows(IllegalStateException.class, () -> IrEmitter.emit(m),
                    () -> g.namen() + " hoort geweigerd te worden");
            assertTrue(e.getMessage().contains(g.reden()), () -> g.namen() + ": " + e.getMessage());
        }
        // an entry without a suffix beside a group of the same name is a form of its own (WF-020 then speaks)
        RegelSpraakModel m = drieRegels();
        List<String> namen = List.of("x", "x #1", "x #2");
        for (int i = 0; i < 3; i++) {
            ResolvedRegel r = m.resolvedRegels.get(i);
            m.resolvedRegels.set(i, new ResolvedRegel(namen.get(i), r.soort(), r.doel(), r.waarde(), r.voorwaarde(),
                    r.geldigheid(), r.bronAnnotaties()));
        }
        String c = IrEmitter.emit(m).replaceAll("\\s+", "");
        assertEquals(2, c.split("\"node\":\"Rule\",\"name\":\"x\"", -1).length - 1,
                () -> "de kale naam en de groep zijn twee vormen:\n" + c);
    }

    /** Three single-version rules on one attribute, for the grouping's refusals to rename. */
    private static RegelSpraakModel drieRegels() {
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype de Vondst
                de waarde Numeriek (geheel getal);
            Regel een
                geldig altijd
                    De waarde van een Vondst moet gesteld worden op 1.
            Regel twee
                geldig altijd
                    De waarde van een Vondst moet gesteld worden op 2.
            Regel drie
                geldig altijd
                    De waarde van een Vondst moet gesteld worden op 3.
            """);
        assertTrue(m.errors().isEmpty(), () -> "onverwachte fouten: " + m.errors());
        return m;
    }

    @Test void de_regelsoort_gaat_de_wire_op_als_initialisatie_en_presentatie() {
        // IR-SPEC ch. 3.1: a gelijkstelling and a berekening are initialisation false with their tag, an
        // initialisation is initialisation true without one, and a characteristic attribution carries neither —
        // that its target is a characteristic is the declaration's to say, and its value is the boolean true.
        String c = emitCompact("""
            Objecttype de Vondst
                de waarde Numeriek (geheel getal);
                het gewicht Numeriek (geheel getal);
                de drempel Numeriek (geheel getal);
                is zwaar kenmerk (bijvoeglijk);
            Regel waarde
                geldig altijd
                    De waarde van een Vondst moet gesteld worden op 1.
            Regel gewicht
                geldig altijd
                    Het gewicht van een Vondst moet berekend worden als de waarde van de Vondst plus 1.
            Regel drempel
                geldig altijd
                    De drempel van een Vondst moet geïnitialiseerd worden op 5.
            Regel zwaar
                geldig altijd
                    Een Vondst is zwaar indien zijn gewicht groter is dan 10.
            """);
        assertTrue(c.contains("\"value\":{\"number\":\"1\"},\"type\":{\"node\":\"Number\",\"whole\":true,\"decimals\":0,"
                        + "\"sign\":\"unspecified\"},\"source\":{\"snippet\":\"1\"}},\"initialisation\":false,\"presentation\":\"equation\"}"),
                () -> "de gelijkstelling:\n" + c);
        assertTrue(c.contains("\"initialisation\":false,\"presentation\":\"calculation\"}"), () -> "de berekening:\n" + c);
        assertTrue(c.contains("\"source\":{\"snippet\":\"5\"}},\"initialisation\":true}"), () -> "de initialisatie, zonder tag:\n" + c);
        assertTrue(c.contains("\"value\":{\"node\":\"Literal\",\"value\":{\"boolean\":true},\"type\":{\"node\":\"Boolean\"},"),
                () -> "de kenmerktoekenning schrijft de booleaanse waarde waar:\n" + c);
        assertTrue(c.contains("\"initialisation\":false}]}"), () -> "de kenmerktoekenning draagt geen tag:\n" + c);
        assertFalse(c.contains("\"soort\"") || c.contains("KENMERKTOEKENNING") || c.contains("INITIALISATIE"),
                () -> "de soort is structuur, geen veld:\n" + c);
    }

    @Test void een_doel_dat_attribuut_en_kenmerk_noemt_wordt_geweigerd() {
        // WF-022: which member a rule writes decides its kind, so a target whose name is both an attribute and a
        // characteristic of the subject type decides nothing; the frontend does not write such a document. The
        // characteristic is added after resolution, so the rule itself is the resolver's.
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype de Jutter
                de toelage Numeriek (geheel getal);
            Regel toelage
                geldig altijd
                    De toelage van een Jutter moet gesteld worden op 1.
            """);
        assertTrue(m.errors().isEmpty(), () -> "onverwachte fouten: " + m.errors());
        assertTrue(IrEmitter.emit(m).contains("\"presentation\": \"equation\""), "de ongemoeide regel wordt geschreven");
        m.objecttypen.get("Jutter").kenmerken.add("Toelage");
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> IrEmitter.emit(m));
        assertTrue(e.getMessage().contains("zowel een attribuut als een kenmerk") && e.getMessage().contains("(WF-022)"),
                e::getMessage);
    }

    @Test void de_uniciteiten_staan_in_een_lijst_de_enkelvoudige_eerst() {
        // IR-SPEC ch. 3.6: one list of uniquenesses, each a version with its sources — one for the single-type form,
        // two or more for the union — the single-source forms first and then the unions, whatever order the source
        // wrote them in. No rgs golden and no document of the parity zone carries a union.
        String c = emitCompact("""
            Objecttype de Cirkel (mv: Cirkels)
                de naam Tekst;
            Objecttype de Vierkant (mv: Vierkanten)
                de naam Tekst;
                het nummer Numeriek (geheel getal);
            Regel namen uniek
                geldig altijd
                    De namen van alle Cirkels verenigd met de namen van alle Vierkanten moeten uniek zijn.
            Regel nummers uniek
                geldig altijd
                    De nummers van alle Vierkanten moeten uniek zijn.
            """);
        String geldig = "\"validity\":{\"node\":\"Validity\"}";
        int enkel = c.indexOf("{\"node\":\"Uniqueness\",\"name\":\"nummersuniek\",\"versions\":[{\"sources\":["
                + "{\"node\":\"Source\",\"objectType\":\"Vierkant\",\"keys\":[\"nummer\"]}]," + geldig + ",\"source\":");
        int unie = c.indexOf("{\"node\":\"Uniqueness\",\"name\":\"namenuniek\",\"versions\":[{\"sources\":["
                + "{\"node\":\"Source\",\"objectType\":\"Cirkel\",\"keys\":[\"naam\"]},"
                + "{\"node\":\"Source\",\"objectType\":\"Vierkant\",\"keys\":[\"naam\"]}]," + geldig + ",\"source\":");
        assertTrue(c.indexOf("\"uniquenesses\":[{\"node\":\"Uniqueness\",\"name\":\"nummersuniek\"") >= 0 && enkel >= 0
                        && unie > enkel, () -> "de enkelvoudige vorm eerst, dan de unie, in één lijst:\n" + c);
        assertFalse(c.contains("uniciteitUnies"), () -> "de tweede lijst bestaat op de wire niet meer:\n" + c);
    }

    @Test void een_verdeling_zonder_methode_wordt_weggelaten_en_gemeld() {
        // IR-SPEC ch. 3.2 gives an allocation one of three methods. An allocation the frontend could not give one
        // (its placeholder) has no spelling: it is left out, and a WARNING that names it is appended to the
        // diagnostics, line 0 because the model keeps no line for it (WF-113 (a)). The other allocations stand.
        RegelSpraakModel m = Resolver.resolve("""
            Objecttype de Berging (mv: Bergingen)
                de opbrengst Numeriek (geheel getal);
            Objecttype de Berger (mv: Bergers) (bezield)
                het aandeel Numeriek (geheel getal);
            Feittype bergingsdeelname
                de berging Berging
                de berger (mv: bergers) Berger
            Eén berging heeft meerdere bergers
            Regel verdeling
                geldig altijd
                    De opbrengst van een berging wordt verdeeld over de aandeel van alle bergers van de berging, waarbij wordt verdeeld in gelijke delen.
            """);
        assertTrue(m.errors().isEmpty(), () -> "onverwachte fouten: " + m.errors());
        assertEquals(1, m.verdelingen.size());
        assertTrue(IrEmitter.emit(m).replaceAll("\\s+", "").contains("\"method\":\"equalShares\""),
                "de ongemoeide verdeling wordt geschreven");
        io.github.yhavinga.regelspraak.ir.ResolvedVerdeling v = m.verdelingen.get(0);
        m.verdelingen.set(0, new io.github.yhavinga.regelspraak.ir.ResolvedVerdeling(v.naam(), v.bronType(),
                v.bronAttribuut(), v.doelType(), v.doelAttribuut(),
                io.github.yhavinga.regelspraak.ir.ResolvedVerdeling.Methode.COMPLEX, v.ratoAttribuut(),
                v.maximumAttribuut(), v.afrondingDecimalen(), v.restantAttribuut(), v.groepeerAttribuut(), v.afnemend(),
                v.voorwaarde(), v.geldigheid()));
        String c = IrEmitter.emit(m).replaceAll("\\s+", "");
        assertTrue(c.contains("\"allocations\":[],"), () -> "de verdeling zonder methode hoort weg te blijven:\n" + c);
        assertFalse(c.contains("COMPLEX"), () -> "de plaatshouder heeft geen spelling:\n" + c);
        assertTrue(c.contains("\"diagnostics\":[{\"node\":\"Diagnostic\",\"severity\":\"warning\",\"message\":"
                        + "\"verdeling'verdeling'wordtnietgeschreven:"), () -> "de WARNING noemt de verdeling:\n" + c);
        assertTrue(c.endsWith("\"line\":0}]}"), () -> "regel 0, als laatste diagnose:\n" + c);
        assertEquals(1, IrEmitter.ongeschrevenVerdelingen(m).size());
        Verslag.verantwoord(IrEmitter.emit(m));
    }

    @Test void de_gebonden_dag_en_de_contextwaarden_hebben_elk_hun_knoop() {
        // IR-SPEC ch. 4.1: the bound day of a day-kind definition is a node of its own without a name; the session
        // values are a ContextValue under their name.
        String c = emitCompact("""
            Dagsoort de werkdag (mv: werkdagen)
            Objecttype de Vondst
                de meldatum Datum in dagen;
                het meldjaar Numeriek (geheel getal);
            Regel werkdag
                geldig altijd
                    Een dag is een werkdag indien de dag gelijk is aan de Rekendatum.
            Regel meldjaar
                geldig altijd
                    Het meldjaar van een Vondst moet gesteld worden op het Rekenjaar.
            """);
        assertTrue(c.contains("{\"node\":\"BoundDay\",\"type\":{\"node\":\"Date\",\"precision\":\"day\"},\"source\":{\"snippet\":\"dedag\"}}"),
                () -> "de gebonden dag:\n" + c);
        assertTrue(c.contains("{\"node\":\"ContextValue\",\"name\":\"evaluationDate\",\"type\":{\"node\":\"Date\","
                + "\"precision\":\"day\"},"), () -> "de rekendatum:\n" + c);
        assertTrue(c.contains("{\"node\":\"ContextValue\",\"name\":\"evaluationYear\",\"type\":{\"node\":\"Number\","),
                () -> "het rekenjaar:\n" + c);
        assertFalse(c.contains("Constante"), () -> "de v0-knoop bestaat op de wire niet meer:\n" + c);
        assertFalse(c.contains("\"name\":\"Rekendatum\"") || c.contains("\"name\":\"Rekenjaar\""),
                () -> "de v0-spelling van de naam staat niet op de wire:\n" + c);
    }

    @Test void een_literal_zonder_type_en_een_onbekende_constante_worden_geweigerd() {
        // The literal's value is the variant its type names, so a literal without a type has none; a boolean whose
        // lexeme is not waar or onwaar has no value; and a constant outside the bound day and the two session values
        // has no spelling at all. Each is refused rather than written in a guessed shape.
        RegelSpraakModel m = drieRegels();
        ResolvedRegel r = m.resolvedRegels.get(0);
        Expr.Literal een = (Expr.Literal) r.waarde();
        for (Expr waarde : List.<Expr>of(
                new Expr.Literal(een.waarde(), null, een.bron()),
                new Expr.Literal("ja", new Datatype.Booleaan(), een.bron()),
                new Expr.Constante("Rekenmaand", new Datatype.DatumInDagen(), een.bron()),
                new Expr.Constante("de dag", null, een.bron()))) {
            m.resolvedRegels.set(0, new ResolvedRegel(r.naam(), r.soort(), r.doel(), waarde, r.voorwaarde(),
                    r.geldigheid()));
            IllegalStateException e = assertThrows(IllegalStateException.class, () -> IrEmitter.emit(m),
                    () -> "geschreven: " + waarde);
            assertTrue(e.getMessage().contains("geen type") || e.getMessage().contains("booleaanse waarde")
                    || e.getMessage().contains("geen spelling"), e::getMessage);
        }
    }

    @Test void een_komma_wordt_een_punt_en_de_bron_houdt_de_komma() {
        // IR-SPEC ch. 4.1 (WF-081): the number is written with a point; the author's decimal comma stays in the
        // source snippet.
        String c = emitCompact("""
            Objecttype de Vondst
                de waarde Numeriek (getal met 2 decimalen);
            Regel waarde
                geldig altijd
                    De waarde van een Vondst moet gesteld worden op 12,50.
            """);
        assertTrue(java.util.regex.Pattern.compile("\\{\"node\":\"Literal\",\"value\":\\{\"number\":\"12\\.50\"\\},"
                + "\"type\":\\{\"node\":\"Number\",[^}]*\\},\"source\":\\{\"snippet\":\"12,50\"\\}\\}").matcher(c).find(),
                () -> "punt in de waarde, komma in de bron:\n" + c);
    }

    @Test void een_vouwveld_op_de_andere_functieknoop_wordt_geweigerd() {
        // IR-SPEC ch. 4.3: an Aggregation has no place for a granularity, a BuiltinCall none for a fold's element
        // type, empty value or dimension selection. The frontend builds neither, so the fields are grafted on.
        RegelSpraakModel m = drieRegels();
        ResolvedRegel r = m.resolvedRegels.get(0);
        Objecttype vondst = m.objecttypen.get("Vondst");
        Expr.AttribuutRef lezing = (Expr.AttribuutRef) r.doel();
        for (Expr waarde : List.<Expr>of(
                new Expr.Functie(Expr.FunctieKind.ABSOLUTE_WAARDE, List.of(r.waarde()), vondst, null, null, null,
                        r.waarde().type(), r.waarde().bron()),
                new Expr.Functie(Expr.FunctieKind.ABSOLUTE_WAARDE, List.of(r.waarde()), null, r.waarde(), null, null,
                        r.waarde().type(), r.waarde().bron()),
                new Expr.Functie(Expr.FunctieKind.SOM, List.of(lezing), null, null, null, null,
                        r.waarde().type(), r.waarde().bron(), RegelSpraakModel.Granulariteit.MAAND))) {
            m.resolvedRegels.set(0, new ResolvedRegel(r.naam(), r.soort(), r.doel(), waarde, r.voorwaarde(),
                    r.geldigheid()));
            IllegalStateException e = assertThrows(IllegalStateException.class, () -> IrEmitter.emit(m),
                    () -> "geschreven: " + waarde);
            assertTrue(e.getMessage().contains("staat dat veld alleen op"), e::getMessage);
        }
    }

    @Test void een_feitcreatie_naar_een_ander_type_dan_de_vuller_wordt_geweigerd() {
        // IR-SPEC ch. 3.4: the target type is not written — a reader takes the declared filler of the target role —
        // so a creation whose target type the model holds otherwise would be read as another creation.
        RegelSpraakModel m = Resolver.resolve(FEITCREATIE);
        assertTrue(m.errors().isEmpty(), () -> "onverwachte fouten: " + m.errors());
        ResolvedFeitCreatie fc = m.feitCreaties.get(0);
        m.feitCreaties.set(0, new ResolvedFeitCreatie(fc.naam(), fc.anker(), m.objecttypen.get("Afdeling"), fc.zetVeld(),
                fc.joinVeld(), fc.tussenstappen(), fc.voorwaarde(), fc.geldigheid(), fc.feittype(), fc.ankerRol(),
                fc.doelRol(), fc.joinRol()));
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> IrEmitter.emit(m));
        assertTrue(e.getMessage().contains("gedeclareerde vuller van de rol 'sponsorproject'"), e::getMessage);
    }

    // ---------------- L5: the source record ----------------

    /** A rule group over a rule and a consistency rule: the two nodes whose source record carries a line. */
    private static final String BRONRECORD = """
            Objecttype de Vondst (mv: Vondsten)
                het gewicht Numeriek (geheel getal);
                de waarde Numeriek (geheel getal);

            Regelgroep weging
            Regel waarde
                geldig altijd
                    De waarde van een Vondst moet berekend worden als het gewicht van de Vondst maal 2.

            Regel positief gewicht
                geldig altijd
                    Het gewicht van een Vondst moet groter zijn dan 0.
            """;

    /** IR-SPEC ch. 4 (B26): a node's source text is its record {@code source}, the snippet first. The line stands in
     *  the record of a consistency version and of a rule group alone — the two places the model keeps one — and no
     *  record carries a file or a column, although the model's anchor of every parsed node holds both. */
    @Test void het_bronrecord_draagt_de_regel_alleen_op_de_consistentieversie_en_de_regelgroep() {
        RegelSpraakModel m = Resolver.resolve(BRONRECORD);
        assertTrue(m.errors().isEmpty(), () -> "onverwachte fouten: " + m.errors());
        assertTrue(m.diagnostics.isEmpty(), () -> "onverwachte diagnoses: " + m.diagnostics);
        assertNotNull(m.consistenties.get(0).assertie().bron().file(), "het model kent het bestand van de assertie");
        String c = IrEmitter.emit(m).replaceAll("\\s+", "");
        assertTrue(c.contains("\"validity\":{\"node\":\"Validity\"},\"source\":{\"snippet\":"
                + "\"HetgewichtvaneenVondstmoetgroterzijndan0\",\"line\":12}}"), () -> "de consistentieversie:\n" + c);
        assertTrue(c.contains("{\"node\":\"RuleGroup\",\"name\":\"weging\",\"recursive\":false,\"members\":"
                + "[\"waarde\",\"positiefgewicht\"],\"source\":{\"snippet\":\"weging\",\"line\":5}}"), () -> "de regelgroep:\n" + c);
        assertEquals(2, c.split("\"line\":", -1).length - 1, () -> "een regel staat alleen op de twee dragers:\n" + c);
        assertFalse(c.contains("\"file\"") || c.contains("\"column\"") || c.contains("\"bron\""),
                () -> "het bronrecord draagt geen bestand en geen kolom, en de wire geen bron:\n" + c);
    }

    /** The model's 0 is "no line": a synthetic node has none, and the record admits a line of at least 1 only, so the
     *  record goes out without it. A negative line has no meaning and is refused. */
    @Test void een_regel_0_blijft_weg_en_een_negatieve_regel_wordt_geweigerd() {
        RegelSpraakModel m = Resolver.resolve(BRONRECORD);
        assertTrue(m.errors().isEmpty(), () -> "onverwachte fouten: " + m.errors());
        var cons = m.consistenties.get(0);
        var groep = m.regelGroepen.get(0);
        m.consistenties.set(0, new io.github.yhavinga.regelspraak.ir.ResolvedConsistentie(cons.naam(), cons.subject(),
                cons.assertie(), cons.bron(), 0, cons.geldigheid()));
        m.regelGroepen.set(0, new io.github.yhavinga.regelspraak.ir.ResolvedRegelGroep(groep.naam(), groep.recursief(),
                groep.leden(), groep.bron(), 0));
        String c = IrEmitter.emit(m).replaceAll("\\s+", "");
        assertTrue(c.contains("\"source\":{\"snippet\":\"HetgewichtvaneenVondstmoetgroterzijndan0\"}}"),
                () -> "de consistentieversie zonder regel:\n" + c);
        assertTrue(c.contains("\"source\":{\"snippet\":\"weging\"}}"), () -> "de regelgroep zonder regel:\n" + c);
        assertFalse(c.contains("\"line\":"), () -> "geen regel 0 op de wire:\n" + c);

        m.regelGroepen.set(0, new io.github.yhavinga.regelspraak.ir.ResolvedRegelGroep(groep.naam(), groep.recursief(),
                groep.leden(), groep.bron(), -1));
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> IrEmitter.emit(m));
        // the refusal speaks the readers' sentence since the card frontend-emitterrestanten
        assertEquals("IrEmitter: a source line is at least 1", e.getMessage());
    }

    /** A node without source text carries no record; an empty text is refused, not dropped: a snippet is never empty,
     *  and a document that lost the record would read as a node without source text. */
    @Test void een_knoop_zonder_brontekst_draagt_geen_record_en_een_lege_wordt_geweigerd() {
        RegelSpraakModel m = Resolver.resolve(BRONRECORD);
        assertTrue(m.errors().isEmpty(), () -> "onverwachte fouten: " + m.errors());
        var cons = m.consistenties.get(0);
        Conditie.Vergelijking v = (Conditie.Vergelijking) cons.assertie();
        Expr.Literal nul = (Expr.Literal) v.rechts();
        for (String snippet : new String[] { null, "" }) {
            m.consistenties.set(0, new io.github.yhavinga.regelspraak.ir.ResolvedConsistentie(cons.naam(), cons.subject(),
                    new Conditie.Vergelijking(v.op(), v.links(), new Expr.Literal(nul.waarde(), nul.type(),
                            Bron.synthetisch(snippet)), v.periode(), v.bron()), cons.bron(), cons.line(), cons.geldigheid()));
            if (snippet == null) {
                String c = IrEmitter.emit(m).replaceAll("\\s+", "");
                assertTrue(c.contains("\"value\":{\"number\":\"0\"},\"type\":{\"node\":\"Number\",\"whole\":true,\"decimals\":0,"
                        + "\"sign\":\"unspecified\"}},\"source\":"), () -> "de literal zonder brontekst:\n" + c);
                continue;
            }
            IllegalStateException e = assertThrows(IllegalStateException.class, () -> IrEmitter.emit(m));
            assertTrue(e.getMessage().contains("a source snippet is never empty: a node without source text carries no source"),
                    e::getMessage);
        }
    }

    /** The Werknemer–Afdeling model the L2 refusal witnesses share: one fact type between the pair, and a
     *  kenmerk and a numeric attribute on the far end. */
    private static final String WERKNEMER_AFDELING = """
            Objecttype de Werknemer
                de rang Numeriek (geheel getal);
            Objecttype de Afdeling
                het niveau Numeriek (geheel getal);
                is actief kenmerk (bijvoeglijk);
            Feittype aanstelling
                de werknemer Werknemer
                de afdeling Afdeling
            Eén afdeling telt meerdere werknemers
            """;

    /** A second fact type in which a Werknemer reaches an Afdeling through a role also called afdeling: the
     *  by-name and the by-pair search both stop being unique. */
    private static void tweedeAfdelingsrol(RegelSpraakModel m) {
        RegelSpraakModel.Feittype ft = new RegelSpraakModel.Feittype("detachering");
        ft.rollen.add(new RegelSpraakModel.Rol("gedetacheerde", "Werknemer", null, false));
        ft.rollen.add(new RegelSpraakModel.Rol("afdeling", "Afdeling", null, false));
        m.feittypen.put(ft.naam, ft);
    }

    private static final String FEITCREATIE = """
            Objecttype het Bedrijf
            Objecttype de Afdeling
            Objecttype het Project
            Feittype onderdeel
                de afdeling Afdeling
                het bedrijf Bedrijf
            Eén bedrijf telt meerdere afdelingen
            Feittype uitvoering
                de afdeling Afdeling
                het project (mv: projecten) Project
            Eén afdeling telt meerdere projecten
            Feittype sponsoring
                het sponsorbedrijf Bedrijf
                het sponsorproject (mv: sponsorprojecten) Project
            Eén sponsorbedrijf telt meerdere sponsorprojecten
            Regel sponsoring
                geldig altijd
                    Een sponsorproject van een Bedrijf is een project van de afdeling van het bedrijf.
            """;
}
