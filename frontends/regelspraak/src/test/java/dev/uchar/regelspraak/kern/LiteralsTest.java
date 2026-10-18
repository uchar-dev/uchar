package dev.uchar.regelspraak.kern;

import dev.uchar.regelspraak.ir.Bron;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.model.Datatype;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/** The single literal reader both back-ends share (single shared implementation) — what a literal MEANS lives here once. */
class LiteralsTest {

    private static Expr.Literal num(String w) { return new Expr.Literal(w, new Datatype.Numeriek(false, 0, null, null), Bron.synthetic(w)); }
    private static Expr.Literal tekst(String w) { return new Expr.Literal(w, new Datatype.Tekst(), Bron.synthetic(w)); }

    /* Until 2026-09-23 these pins read the day-first source snippet through this reader ("dd. 25-12-2024",
     * "dd. 8-9-2024", "dd. 25-12-2024 10:30:00.500") and held the snippet and the ISO lexeme to the same
     * moment. The reader is the WIRE reader the back-ends share: the emitter writes ISO into waarde (WF-084,
     * WF-095 since card p5-datumtijd-lexeem), and the frontend reads its own source snippet with its own
     * reader in ExpressionBuilder, which the emitter tests pin. So the snippet pins turned into refusals
     * (cards datumtijd-lezers-versmalling, datum-lexeem-hele-match). */
    @Test void dates_and_date_times_parse_the_iso_lexeme_with_milli_to_nano_scale() {
        assertEquals(LocalDate.of(2024, 12, 25), Literals.parseDate("2024-12-25"));
        assertEquals(LocalDate.of(2024, 9, 8), Literals.parseDate("2024-09-08"));
        assertNull(Literals.parseDate("geen datum"));
        assertEquals(LocalDateTime.of(2024, 12, 25, 10, 30, 0, 500_000_000),
                Literals.parseDateTime("2024-12-25T10:30:00.500"), "IR-SPEC ch. 4.1, met fractie");
        assertEquals(LocalDateTime.of(2024, 12, 25, 10, 30, 0),
                Literals.parseDateTime("2024-12-25T10:30:00"), "fractie is optioneel");
        assertNull(Literals.parseDateTime("2024-12-25"), "zonder tijddeel geen datumtijd");
    }

    @Test void the_source_snippet_is_no_wire_lexeme() {
        assertNull(Literals.parseDate("dd. 25-12-2024"), "dag-eerst met dd.-voorvoegsel");
        assertNull(Literals.parseDate("25-12-2024"), "dag-eerst zonder voorvoegsel (WF-084 weigert het)");
        assertNull(Literals.parseDate("dd. 8-9-2024"), "enkelcijferige dag/maand");
        assertNull(Literals.parseDateTime("dd. 25-12-2024 10:30:00.500"), "datumtijd-snippet met voorvoegsel");
        assertNull(Literals.parseDateTime("25-12-2024 10:30:00.500"), "datumtijd-snippet zonder voorvoegsel");
        assertNull(Literals.parseDate("dd. 2024-12-25"), "het dd.-voorvoegsel voor een ISO-datum is geen lexeem");
        assertNull(Literals.parseDateTime("dd. 2024-12-25T10:30:00.500"), "idem voor een ISO-datumtijd");
    }

    @Test void date_matches_the_whole_lexeme() {
        assertNull(Literals.parseDate("2024-01-2026"), "werd met find() stil 2024-01-20");
        assertNull(Literals.parseDate("x 2024-01-31 y"), "het lexeem is de hele waarde");
        assertNull(Literals.parseDate("2024-1-31"), "vaste veldbreedtes");
        assertNull(Literals.parseDate("2024-02-30"), "de kalender beslist");
    }

    @Test void date_time_refuses_what_falls_outside_the_lexeme() {
        assertNull(Literals.parseDateTime("2024-12-25T10:30"), "seconden verplicht");
        assertNull(Literals.parseDateTime("2024-12-25T10:30:00Z"), "geen zone-suffix");
        assertNull(Literals.parseDateTime("2024-12-25T10:30:00+01:00"), "geen offset");
        assertNull(Literals.parseDateTime("2024-12-25 10:30:00"), "T, geen spatie, in het canonieke lexeem");
        assertNull(Literals.parseDateTime("2024-1-25T9:30:00"), "canoniek heeft vaste veldbreedtes");
        assertNull(Literals.parseDateTime("2024-12-25T10:30:00.1"), "fractie exact drie cijfers");
        assertNull(Literals.parseDateTime("2024-12-25T10:30:00.1234"), "nooit stil afkappen");
        assertNull(Literals.parseDateTime("x 2024-12-25T10:30:00 y"), "het lexeem is de hele waarde");
    }

    @Test void number_reads_rationals_exactly_or_on_DECIMAL64() {
        assertEquals(new BigDecimal("0.5"), Literals.getal(num("1/2")), "eindigend quotient EXACT");
        assertEquals(new BigDecimal("0.3333333333333333"), Literals.getal(num("1/3")), "niet-eindigend op DECIMAL64");
        assertEquals(0, new BigDecimal("42").compareTo(Literals.getal(num("42"))));
        assertNull(Literals.getal(num("geen getal")));
    }

    @Test void enumLabel_strips_only_the_delimiters_and_keeps_embedded_signs() {
        assertEquals("eerste klas", Literals.enumLabel("'eerste klas'"));
        assertEquals("typografisch", Literals.enumLabel("’typografisch‘"));
        // escaping by construction: an embedded double quote is PART of the label — kept, never stripped
        assertEquals("zg. \"digitale\" kaart", Literals.enumLabel("'zg. \"digitale\" kaart'"));
        assertNull(Literals.enumLabel("geen enum"));
    }

    @Test void textOfEnumLabel_reads_text_verbatim() {
        // IR-SPEC ch. 4.1: waarde IS the value. Ref reads it verbatim and a case input carries it verbatim, so
        // whatever this reader removes is compared against a string no other executor ever sees.
        assertEquals("Flevoland", Literals.textOrEnumLabel(tekst("Flevoland")), "een rauw lexeem is zijn eigen waarde");
        assertEquals("\"Flevoland\"", Literals.textOrEnumLabel(tekst("\"Flevoland\"")),
                "buitenste aanhalingstekens horen bij de waarde — Tekst heeft geen delimiters om te strippen");
        assertEquals("merk \"AA\"", Literals.textOrEnumLabel(tekst("merk \"AA\"")),
                "een ingebed aanhalingsteken overleeft (de keurmerk-golden)");
    }

    @Test void textOfEnumLabel_does_not_normalise_silently() {
        // The mutation witness of the pair above: a reader that strips quotes folds these two DIFFERENT wire values
        // onto one string, and an equality then fires on a document that does not license it.
        assertNotEquals(Literals.textOrEnumLabel(tekst("Flevoland")),
                Literals.textOrEnumLabel(tekst("\"Flevoland\"")),
                "rauw en quote-dragend zijn twee waarden, niet een");
    }

    @Test void textOfEnumLabel_strips_only_the_enum_delimiters() {
        assertEquals("eerste klas", Literals.textOrEnumLabel(num("'eerste klas'")), "een enum rijdt op elk niet-Tekst type");
        assertEquals("zg. \"digitale\" kaart", Literals.textOrEnumLabel(num("'zg. \"digitale\" kaart'")),
                "ook hier blijven ingebedde tekens staan");
        assertNull(Literals.textOrEnumLabel(num("42")));
    }

    @Test void textOfEnumLabel_reads_an_enumeration_typed_literal_verbatim() {
        // WF-039 kruist deze waarde byte-exact tegen het gedeclareerde domein, dus de wire-vorm IS het kale
        // label — de delimiters zijn er al af als de emitter klaar is. De enum-arm hierboven door enumLabel
        // laten lopen gaf op elk conform document null, en dat is hoe beide Drools-takken ertoe kwamen elke
        // enumgelijkheid en elke enumtoekenning te weigeren (kaart drools-enumlabel-lezer, 2026-09-20).
        assertEquals("wrakgoed", Literals.textOrEnumLabel(enumLit("wrakgoed", "drijfgoed", "wrakgoed")));
        assertEquals("licht blauw", Literals.textOrEnumLabel(enumLit("licht blauw", "licht blauw")),
                "een label met een spatie is één label");
        // En verbatim betekent verbatim: een gedelimiteerde waarde op een Enumeratie-type is ill-formed op de
        // wire (WF-039 weigert haar), dus deze lezer repareert haar niet alsnog stilletjes tot een label dat
        // de verifier nooit gezien heeft.
        assertEquals("'wrakgoed'", Literals.textOrEnumLabel(enumLit("'wrakgoed'", "wrakgoed")),
                "de lezer verzint geen label bij een waarde die de verifier al afkeurt");
    }

    private static Expr.Literal enumLit(String w, String... waarden) {
        return new Expr.Literal(w, new Datatype.Enumeratie("Vondstklasse", java.util.List.of(waarden)),
                Bron.synthetic("'" + w + "'"));
    }

    @Test void true_reads_the_boolean_literals() {
        assertTrue(Literals.parseBoolean("waar"));
        assertFalse(Literals.parseBoolean("onwaar"));
    }
}
