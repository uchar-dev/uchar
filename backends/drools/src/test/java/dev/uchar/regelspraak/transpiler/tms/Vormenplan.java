package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.graaf.ProducentConsument.ElementSleutel;
import dev.uchar.regelspraak.ir.Conditie;
import dev.uchar.regelspraak.ir.Geldigheid;
import dev.uchar.regelspraak.runtime.Breuk;
import dev.uchar.regelspraak.transpiler.tms.plan.Accumulatie;
import dev.uchar.regelspraak.transpiler.tms.plan.Alfa;
import dev.uchar.regelspraak.transpiler.tms.plan.Bestaat;
import dev.uchar.regelspraak.transpiler.tms.plan.Betatoets;
import dev.uchar.regelspraak.transpiler.tms.plan.Binding;
import dev.uchar.regelspraak.transpiler.tms.plan.Functie;
import dev.uchar.regelspraak.transpiler.tms.plan.Gevolg;
import dev.uchar.regelspraak.transpiler.tms.plan.Join;
import dev.uchar.regelspraak.transpiler.tms.plan.Leessoort;
import dev.uchar.regelspraak.transpiler.tms.plan.Leestabel;
import dev.uchar.regelspraak.transpiler.tms.plan.Lezing;
import dev.uchar.regelspraak.transpiler.tms.plan.LhsItem;
import dev.uchar.regelspraak.transpiler.tms.plan.Modelplan;
import dev.uchar.regelspraak.transpiler.tms.plan.Niet;
import dev.uchar.regelspraak.transpiler.tms.plan.Onderwerp;
import dev.uchar.regelspraak.transpiler.tms.plan.Patroon;
import dev.uchar.regelspraak.transpiler.tms.plan.Regelplan;
import dev.uchar.regelspraak.transpiler.tms.plan.Term;
import dev.uchar.regelspraak.transpiler.tms.plan.Toetsitem;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * A plan made by hand that holds every form {@link DrlWeergave} carries in step 2a of card
 * drl-teksttak-op-truth-maintenance, over three beans of one document (a Wachter at a Post on a Kustvak), so that a
 * witness can compile the forms no golden of the corpus renders: it decides nothing of a document's meaning, and no
 * rule of it is meant to fire. The rules are named after what they hold.
 */
final class Vormenplan {

    private Vormenplan() {}

    /** Fragments of the text that show the forms the plan holds, one per form. */
    static final List<String> VORMEN = List.of("not( ", " and ", "exists( ", "accumulate( ", "count()", "sumBD(",
            "min(", "max(", "collectList(", "eval( ", "agenda-group \"proefgroep\"", "Rekenhulpen.klasseIs(this",
            "Rekenhulpen.isEen(this", "Rekenhulpen.klasseNietIn(this", "Rekenhulpen.binnen(datum",
            "Rekenhulpen.lees(this, \"hoogte\")", "Rekenhulpen.waar(", "Rekenhulpen.tijdvakken(", "Rekenhulpen.gezet(",
            "_pad", "_beta", "_samengesteld", "_zonderfout", "_vouwlezing", "_kwantorlezing", "_let", "_binder", "_lijst",
            "rsDagsoort_Werkdag", "tweede != null, tweede == $onderwerp.post", "eerste == $onderwerp", " >= 1", " < 3",
            "Uitkomst.Soort.BESLIST", "insert(", "insertLogical(", "Rekenhulpen.sonde()", "Rekenhulpen.faal(",
            "Rekenhulpen.vereist(", "Rekenhulpen.tekstreeks(", "Rekenhulpen.herschaalExact(", "Rekenhulpen.herschaal(",
            "Rekenhulpen.gedeeld(", "Rekenhulpen.wortel(", "Rekenhulpen.grootste(", "Breuk.van(",
            "java.time.LocalDateTime.parse(", "java.math.RoundingMode.HALF_UP", "Tijdvak.class",
            "dev.uchar.regelspraak.ir.Conditie.Operator.LATER", "Rekenhulpen.contractbreuk(", "Rekenhulpen.rekenfout(",
            "Rekenhulpen.inconsistentie(", "Rekenhulpen.schrijversconflict(", "new ParameterStand(", "new Rekendatum(",
            "new KenmerkStand(", "Rekenhulpen.emmerscalairVan(", "new PadStand(");

    private static final String O = "$onderwerp";

    static Modelplan plan(Class<?> wachter, Class<?> post, Class<?> kustvak) {
        List<Regelplan> regels = new ArrayList<>();
        regels.add(alleTermen(wachter, post, kustvak));
        Onderwerp onderwerp = new Onderwerp(wachter, O, List.of(), List.of(),
                List.of(Binding.van("$uitkijkbonus", wachter, "uitkijkbonus")));
        Leestabel een = new Leestabel(List.of(new Lezing(0, Leessoort.INVOER, "uitkijkbonus", "$uitkijkbonus")));
        Term deling = new Term.Aanroep(Functie.RS_AGG_VERGELIJK, List.of(new Term.Deel(Functie.GEDEELD, lees(0), lees(0),
                -1, "uitkijkbonus gedeeld door uitkijkbonus"), getal("1"), tekst(">")), "de deling groter dan 1");
        Gevolg.Foutnaam foutnaam = new Gevolg.Foutnaam("proef", "proefregel");
        regels.add(new Regelplan("fout van een toets", "proef", null, een, List.of(onderwerp,
                new Toetsitem("fout", Toetsitem.Rol.FOUT, deling, -1, List.of(0), 1)),
                new Gevolg.Toetsfout(O, deling, Toetsitem.Rol.FOUT, List.of(0), 1, foutnaam, Gevolg.Invoeging.LOGISCH)));
        regels.add(new Regelplan("kenmerk zonder fout", "proef", "proefgroep", een, List.of(onderwerp,
                new Toetsitem("zonder", Toetsitem.Rol.ZONDER_FOUT, deling, -1, List.of(0), 1)),
                new Gevolg.Kenmerk(O, ElementSleutel.van("wachter", "gevarenwacht", true), Gevolg.Invoeging.LOGISCH)));
        regels.add(new Regelplan("vaste fout", "proef", null, een, List.of(onderwerp,
                new Toetsitem("vorm", Toetsitem.Rol.VORM, new Term.Aanroep(Functie.EMMERSCALAIR, List.of(lees(0),
                        new Term.Const(Boolean.FALSE, null, null)), null), -1, List.of(0), 1)),
                new Gevolg.Fout(O, "een vaste zin", foutnaam, Gevolg.Invoeging.GESTELD)));
        regels.add(new Regelplan("inconsistentie", "proef", null, een, List.of(onderwerp,
                new Toetsitem("voorwaarde", Toetsitem.Rol.VOORWAARDE, new Term.Aanroep(Functie.LEEGTOETS, List.of(lees(0),
                        new Term.Const(Boolean.TRUE, null, null)), "zijn uitkijkbonus leeg is"), -1, List.of(0), 1)),
                new Gevolg.Inconsistentie(O, "proef", Gevolg.Invoeging.LOGISCH)));
        Patroon waarden = new Patroon(Waarde.class, "$waarde", List.of(new Alfa("attribuut", List.of(
                new Alfa.Veldtoets("attribuut", Alfa.Vergelijker.GELIJK, ElementSleutel.van("wachter", "uitkijkbonus", false))))),
                List.of(new Join("onderwerp", "onderwerp", O)), List.of());
        List<String> regelsVanHetDocument = List.of("a", "b", "c", "d", "e", "f", "g", "h");
        regels.add(new Regelplan("schrijvers", null, null,
                new Leestabel(List.of(new Lezing(0, Leessoort.WAARDEN, "waarden", "$waarden"))),
                List.of(new Onderwerp(wachter, O, List.of(), List.of(), List.of()), new Accumulatie(waarden,
                        List.of(new Accumulatie.Functie(Accumulatie.Soort.LIJST, null, "$waarden")))),
                new Gevolg.Schrijversconflict(O, 0, ElementSleutel.van("wachter", "uitkijkbonus", false), "uitkijkbonus",
                        regelsVanHetDocument, List.of("Wachter"), List.of("Wachter"), Gevolg.Invoeging.LOGISCH)));
        regels.add(new Regelplan("parameterstand", null, null, new Leestabel(List.of()), List.of(),
                new Gevolg.Parameterstand("drempel", "drempel", Gevolg.Invoeging.LOGISCH)));
        regels.add(new Regelplan("rekendatumstand", null, null, new Leestabel(List.of()), List.of(),
                new Gevolg.Rekendatumstand("rekendatum", Gevolg.Invoeging.LOGISCH)));
        regels.add(new Regelplan("kenmerkstand", null, null, new Leestabel(List.of()),
                List.of(new Onderwerp(wachter, O, List.of(), List.of(), List.of())),
                new Gevolg.Kenmerkstand(O, ElementSleutel.van("wachter", "rustwacht", true), Gevolg.Standbron.GEEN, false,
                        Gevolg.Invoeging.LOGISCH)));
        regels.add(new Regelplan("stand", null, null, een, List.of(onderwerp), new Gevolg.Stand(O,
                ElementSleutel.van("wachter", "uitkijkbonus", false), Gevolg.Standbron.INVOER, 0, true,
                Gevolg.Invoeging.LOGISCH)));
        regels.add(new Regelplan("padstand zonder link", null, null, new Leestabel(List.of()),
                List.of(new Onderwerp(wachter, O, List.of(), List.of(), List.of())), new Gevolg.Padstand(O, "Wachter:post",
                ElementSleutel.van("post", "hoogte", false), Gevolg.Standbron.ONTBREKENDE_LINK, -1,
                Gevolg.Invoeging.LOGISCH)));
        regels.add(new Regelplan("padstand", null, null, een, List.of(onderwerp), new Gevolg.Padstand(O,
                "Wachter:post", ElementSleutel.van("post", "hoogte", false), Gevolg.Standbron.BEREIKT, 0,
                Gevolg.Invoeging.LOGISCH)));
        return new Modelplan(List.of(new Modelplan.Globaal("drempel", BigDecimal.class),
                new Modelplan.Globaal("rekendatum", LocalDate.class)), regels);
    }

    /** A plan whose one rule has a consequence of a kind this step does not carry. */
    static Modelplan bijdrage(Class<?> wachter) {
        return new Modelplan(List.of(), List.of(new Regelplan("bijdrage", null, null,
                new Leestabel(List.of(new Lezing(0, Leessoort.ONDERWERP, "onderwerp zelf", O))),
                List.of(new Onderwerp(wachter, O, List.of(), List.of(), List.of())),
                new Gevolg.Bijdrage(O, "proefvouw", 0, new Gevolg.Inbreng.Telling(), List.of(0), 1,
                        Gevolg.Invoeging.LOGISCH))));
    }

    /**
     * A rule with every kind of left-hand item, every comparator and form of reading, and a test of deposits and a
     * value with a deposit whose terms hold every node of a term.
     */
    private static Regelplan alleTermen(Class<?> wachter, Class<?> post, Class<?> kustvak) {
        Binding.Veld naarPost = new Binding.Veld(wachter, "post"), naarKustvak = new Binding.Veld(post, "kustvak");
        Onderwerp onderwerp = new Onderwerp(wachter, O,
                List.of(new Alfa("klassen", List.of(
                        new Alfa.Veldtoets(null, Alfa.Vergelijker.KLASSE_IS, wachter),
                        new Alfa.Veldtoets(null, Alfa.Vergelijker.IS_EEN, wachter),
                        new Alfa.Veldtoets(null, Alfa.Vergelijker.KLASSE_NIET_IN,
                                new LinkedHashSet<>(List.of(post, kustvak))))),
                        new Alfa("velden", List.of(
                                new Alfa.Veldtoets(List.of(), "hoogte", Binding.Leesvorm.WAARDE, Alfa.Vergelijker.LEEG,
                                        null, post),
                                new Alfa.Veldtoets(List.of(naarPost), null, Binding.Leesvorm.WAARDE,
                                        Alfa.Vergelijker.GEVULD, null),
                                new Alfa.Veldtoets(List.of(), "gevarenwacht", Binding.Leesvorm.WAAR,
                                        Alfa.Vergelijker.GELIJK, Boolean.TRUE),
                                new Alfa.Veldtoets("uitkijkbonus", Alfa.Vergelijker.NIET_GELIJK, null)))),
                List.of(),
                List.of(new Binding("$uitkijkbonus", List.of(), new Binding.Veld(wachter, "uitkijkbonus"),
                                Binding.Leesvorm.WAARDE, null),
                        new Binding("$gevarenwacht", List.of(), new Binding.Veld(wachter, "gevarenwacht"),
                                Binding.Leesvorm.WAAR, null),
                        new Binding("$tijdvakken", List.of(), new Binding.Veld(wachter, "uitkijkbonus"),
                                Binding.Leesvorm.TIJDVAKKEN, null),
                        new Binding("$postGezet", List.of(), naarPost, Binding.Leesvorm.GEZET, post),
                        new Binding("$postHoogte", List.of(naarPost), new Binding.Veld(post, "hoogte"),
                                Binding.Leesvorm.WAARDE, null),
                        new Binding("$postKustvakVerraderlijk", List.of(naarPost, naarKustvak),
                                new Binding.Veld(kustvak, "verraderlijk"), Binding.Leesvorm.WAAR, null),
                        new Binding("$postRisicopost", List.of(), new Binding.Veld(post, "risicopost"),
                                Binding.Leesvorm.WAARDE, null)));
        Patroon parameter = new Patroon(ParameterStand.class, "$parameter", List.of(new Alfa("parameter",
                List.of(new Alfa.Veldtoets("parameter", Alfa.Vergelijker.GELIJK, "drempel")))), List.of(),
                List.of(Binding.van("$drempel", ParameterStand.class, "waarde")));
        Patroon rekendatum = new Patroon(Rekendatum.class, "$rekendatum", List.of(new Alfa("geldig",
                List.of(new Alfa.Veldtoets("datum", Alfa.Vergelijker.BINNEN,
                        new Geldigheid(LocalDate.of(2026, 1, 1), null))))), List.of(),
                List.of(Binding.van("$datum", Rekendatum.class, "datum")));
        Patroon uitkomst = new Patroon(Uitkomst.class, "$uitkomst", List.of(new Alfa("rang", List.of(
                new Alfa.Veldtoets("soort", Alfa.Vergelijker.GELIJK, Uitkomst.Soort.BESLIST),
                new Alfa.Veldtoets("keten", Alfa.Vergelijker.MINSTENS, 1),
                new Alfa.Veldtoets("positie", Alfa.Vergelijker.ONDER, 3)))),
                List.of(new Join("uitkomst", "onderwerp", O)), List.of());
        Patroon koppeling = new Patroon(Koppeling.class, "$koppeling", List.of(), List.of(new Join("eerste", "eerste", O),
                new Join("tweede", "tweede", O, naarPost)), List.of(new Betatoets("ontmoeting", Functie.ONTMOETING, O,
                List.of(new Term.Const(List.of(), null, null), new Term.Const(List.of("post"), null, null)))), List.of());
        Patroon nietWaarde = new Patroon(Waarde.class, "$nietWaarde", List.of(), List.of(new Join("nw", "onderwerp", O)),
                List.of());
        Patroon nietKenmerk = new Patroon(HeeftKenmerk.class, "$nietKenmerk", List.of(), List.of(new Join("nk",
                "onderwerp", "$nietWaarde", new Binding.Veld(Waarde.class, "onderwerp"))), List.of());
        List<Patroon> niet = List.of(nietWaarde, nietKenmerk);
        Patroon stand = new Patroon(Stand.class, "$stand", List.of(), List.of(new Join("st", "onderwerp", O)), List.of());
        Patroon bijdrage = new Patroon(Bijdrage.class, "$bijdrage", List.of(new Alfa("vouw", List.of(
                new Alfa.Veldtoets("vouw", Alfa.Vergelijker.GELIJK, "proefvouw")))),
                List.of(new Join("bijdrage", "onderwerp", O)),
                List.of(Binding.van("$bijdragewaarde", Bijdrage.class, "waarde")));
        Accumulatie vouw = new Accumulatie(bijdrage, List.of(
                new Accumulatie.Functie(Accumulatie.Soort.AANTAL, null, "$aantal"),
                new Accumulatie.Functie(Accumulatie.Soort.SOM, "$bijdragewaarde", "$som"),
                new Accumulatie.Functie(Accumulatie.Soort.MINIMUM, "$bijdragewaarde", "$kleinste"),
                new Accumulatie.Functie(Accumulatie.Soort.MAXIMUM, "$bijdragewaarde", "$grootste"),
                new Accumulatie.Functie(Accumulatie.Soort.LIJST, null, "$lijst")));
        // the slots: what the bindings bind, the subject, two binders and the probe
        String[] namen = {"$uitkijkbonus", "$gevarenwacht", "$tijdvakken", "$postGezet", "$postHoogte",
                "$postKustvakVerraderlijk", "$drempel", "$datum", "$aantal", "$som", "$kleinste", "$grootste", null,
                null, null, "$lijst", O, "$postRisicopost"};
        List<Lezing> slots = new ArrayList<>();
        for (int i = 0; i < namen.length; i++)
            slots.add(new Lezing(i, namen[i] == null ? (i == 14 ? Leessoort.SONDE : Leessoort.BINDER) : Leessoort.INVOER,
                    namen[i] == null ? null : "slot " + i, namen[i]));
        List<Integer> gelezen = List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 15, 16, 17);
        Term toets = new Term.Samengesteld(Conditie.Kwantor.ALLE, 0, List.of(
                new Term.Aanroep(Functie.RS_AGG_VERGELIJK, List.of(new Term.Deel(Functie.GEDEELD, lees(0), getal("2"), 14,
                        "de uitkijkbonus gedeeld door 2"), getal("1"), tekst(">")), "de helft groter dan 1"),
                new Term.Aanroep(Functie.RS_AGG_VERGELIJK, List.of(new Term.Wortel(Functie.WORTEL, lees(0), 14,
                        "de wortel"), getal("1"), tekst(">=")), "de wortel minstens 1"),
                new Term.Aanroep(Functie.RS_AGG_VERGELIJK, List.of(new Term.Vouwlezing(lees(9), 8, 14, 10, 11,
                        "de som van de bijdragen"), getal("0"), tekst("<")), "de som onder 0"),
                new Term.Kwantorlezing(Functie.KWANTORLEZING, lees(8), lees(15), Conditie.Kwantor.TEN_MINSTE, 1, 14,
                        "ten minste één bijdrage"),
                new Term.ZonderFout(new Term.Aanroep(Functie.KENMERK, List.of(lees(1), new Term.Const(Boolean.TRUE, null,
                        null)), "hij een gevarenwacht is"), null)), "alle voorwaarden");
        Term waarde = new Term.Let(12, "a", new Term.Aanroep(Functie.RS_PLUS, List.of(lees(0), getal("1.50")), "a"),
                new Term.Let(13, "b", new Term.Keuze(new Term.ZonderFout(new Term.Aanroep(Functie.RS_AGG_VERGELIJK,
                        List.of(new Term.Binder(12, "a", "a"), lees(6), tekst(">")), "a groter dan de drempel"), null),
                        new Term.Herschaal(Functie.HERSCHAAL_EXACT, new Term.Binder(12, "a", "a"), null, null,
                                BigDecimal.ONE, BigDecimal.TEN, null),
                        new Term.Herschaal(Functie.HERSCHAAL, new Term.Binder(12, "a", "a"), null, null,
                                new BigDecimal("2.5"), null, null), "als a groter is dan de drempel"),
                        new Term.GelijsteVouw(Functie.GROOTSTE, List.of(
                                new Term.Binder(13, "b", "b"),
                                new Term.Vereist(lees(7), "geen rekendatum", null),
                                new Term.Tekstreeks(List.of(tekst("x"), new Term.Aanroep(Functie.RS_TEKST,
                                        List.of(lees(0)), null)), "x en de uitkijkbonus"),
                                new Term.Aanroep(Functie.RS_AFRONDING, List.of(lees(0), new Term.Const(2, null, null),
                                        new Term.Const(RoundingMode.HALF_UP, null, null)), null),
                                new Term.Aanroep(Functie.RS_TIJDSDUUR_DAGSOORT, List.of(new Term.Const(
                                        LocalDate.of(2026, 1, 1), null, null), lees(7), new Term.Dagsoort("werkdag",
                                        d -> true, null), new Term.Const(Boolean.TRUE, null, null)), null),
                                new Term.Aanroep(Functie.DATUM_VERGELIJK, List.of(new Term.Const(
                                        LocalDateTime.of(2026, 1, 1, 12, 0), null, null), lees(7),
                                        new Term.Const(Conditie.Operator.LATER, null, null)), null),
                                new Term.Aanroep(Functie.RS_DATUM_DEEL, List.of(lees(7),
                                        new Term.Const(ChronoField.YEAR, null, null)), null),
                                new Term.Aanroep(Functie.RS_TIJD_KNIP, List.of(lees(2), lees(2),
                                        new Term.Const(Tijdvak.class, null, null)), null),
                                new Term.Aanroep(Functie.RS_DIM_SOM, List.of(lees(0), new Term.Const(List.of("a", "b"),
                                        null, null)), null),
                                new Term.Aanroep(Functie.KWANTOROORDEEL, List.of(lees(8), lees(9),
                                        new Term.Const(Conditie.Kwantor.GEEN, null, null), new Term.Const(0, null, null)),
                                        null),
                                new Term.Aanroep(Functie.TERUGVAL, List.of(new Term.Const(null, null, null),
                                        new Term.Const(Breuk.van(BigInteger.ONE, BigInteger.valueOf(3)), null, null)), null),
                                new Term.Aanroep(Functie.TERUGVAL, List.of(new Term.Const(5L, null, null),
                                        new Term.Const(BigInteger.valueOf(7), null, null)), null),
                                new Term.Invoer(lees(16), new Binding.Veld(wachter, "rustwacht"), Binding.Leesvorm.WAAR,
                                        null),
                                new Term.Keuze(new Term.Const(Boolean.TRUE, null, null), lees(17),
                                        new Term.Faal("nooit", null), null),
                                new Term.Vouwlezing(lees(9), 8, 14, 10, 11, null)), "de grootste"),
                        false, "b is als a groter is dan de drempel"),
                false, "a is de uitkijkbonus plus 1,50");
        List<LhsItem> lhs = List.of(onderwerp, parameter, rekendatum, uitkomst, koppeling,
                new Niet(niet, Bestaat.verklaard(niet), Bestaat.gelezen(niet)), new Bestaat(List.of(stand),
                        List.of("$stand"), List.of(O)), vouw,
                new Toetsitem("deponering", Toetsitem.Rol.DEPONERING, toets, 14, gelezen, namen.length));
        return new Regelplan("alle termen", "proef", "proefgroep", new Leestabel(slots), lhs,
                new Gevolg.Waarde(O, ElementSleutel.van("wachter", "risicotoeslag", false), "alle termen", waarde, gelezen,
                        namen.length, true, false, new Gevolg.Deponering(14, "proef"), new Gevolg.Foutnaam("proef",
                        "alle termen"), Gevolg.Invoeging.LOGISCH));
    }

    private static Term lees(int slot) {
        return new Term.Lees(slot, null);
    }

    private static Term getal(String waarde) {
        return new Term.Const(new BigDecimal(waarde), null, waarde);
    }

    private static Term tekst(String waarde) {
        return new Term.Const(waarde, null, null);
    }
}
