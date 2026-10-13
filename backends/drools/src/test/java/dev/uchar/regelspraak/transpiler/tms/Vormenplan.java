package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.graaf.ProducentConsument.ElementSleutel;
import dev.uchar.regelspraak.ir.Conditie;
import dev.uchar.regelspraak.ir.Geldigheid;
import dev.uchar.regelspraak.ir.ResolvedVerdeling;
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
 * Plans made by hand that hold every form {@link DrlWeergave} carries, over three beans of one document (a Wachter at a
 * Post on a Kustvak), so that a witness can compile the forms no golden of the corpus renders: the forms of step 2a of
 * card drl-teksttak-op-truth-maintenance ({@link #plan}), those of the rules of a fold of step 3a ({@link #vouwen}), and
 * those of a uniqueness and of the §9.5 channel of step 3b ({@link #kanaal}), those of a defaults stack of step 4
 * ({@link #defaults}), and those of a creation and a link ({@link #creaties}) and of an allocation
 * ({@link #verdelingen}) of step 5. They decide nothing of a document's
 * meaning, and no rule of them is meant to fire. The rules are named after what they hold.
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

    /** Fragments of the text that show the forms of the plan of the allocations ({@link #verdelingen}), one per form. */
    static final List<String> VERDELINGSVORMEN = List.of(
            "    insertLogical(new Ontvanger(\"proefverdeling\", $bron, $onderwerp, $uitkijkbonus, $uitkijkbonus,"
                    + " $uitkijkbonus));",
            "    insertLogical(new Ontvanger(\"proefverdeling\", $bron, $onderwerp, null, null, null));",
            "    for (Object feit : Rekenhulpen.uitdeling(\"proefverdeling\", $onderwerp,"
                    + " dev.uchar.regelspraak.ir.ResolvedVerdeling.Methode.NAAR_RATO, $pot, $ontvangers, true, true, false,"
                    + " Integer.valueOf(2), ElementSleutel.van(\"wachter\", \"uitkijkbonus\", false),"
                    + " ElementSleutel.van(\"post\", \"hoogte\", false), \"proefverdeling\")) {\n"
                    + "        insertLogical(feit);\n    }",
            "Rekenhulpen.uitdeling(\"proefverdeling\", $onderwerp,"
                    + " dev.uchar.regelspraak.ir.ResolvedVerdeling.Methode.OP_VOLGORDE, $pot, $ontvangers, false, false, true,"
                    + " null, ElementSleutel.van(\"wachter\", \"uitkijkbonus\", false), null, \"proefverdeling\")",
            "dev.uchar.regelspraak.ir.ResolvedVerdeling.Methode.GELIJKE_DELEN");

    /**
     * A plan with every consequence of an allocation (step 5 of card drl-teksttak-op-truth-maintenance), over the beans
     * of the same document as {@link #plan}: a recipient with its weight, key and cap, and one without them; the
     * distribution of each method, with the decimals of its floor and a rest, and without them. No rule of it is meant to
     * fire.
     */
    static Modelplan verdelingen(Class<?> wachter, Class<?> post) {
        List<Regelplan> regels = new ArrayList<>();
        Patroon bron = new Patroon(post, "$bron", List.of(), List.of(), List.of());
        Onderwerp ontvanger = new Onderwerp(wachter, O, List.of(), List.of(),
                List.of(Binding.van("$uitkijkbonus", wachter, "uitkijkbonus")));
        regels.add(new Regelplan("ontvanger met waarden", "proefverdeling", null,
                tabel(lidslot(0), new Lezing(1, Leessoort.INVOER, "uitkijkbonus", "$uitkijkbonus")),
                List.of(ontvanger, bron), new Gevolg.Ontvanger("$bron", "proefverdeling", 0, 1, 1, 1,
                Gevolg.Invoeging.LOGISCH)));
        regels.add(new Regelplan("ontvanger zonder waarden", "proefverdeling", null, tabel(lidslot(0)),
                List.of(new Onderwerp(wachter, O, List.of(), List.of(), List.of()), bron),
                new Gevolg.Ontvanger("$bron", "proefverdeling", 0, -1, -1, -1, Gevolg.Invoeging.LOGISCH)));
        Onderwerp pot = new Onderwerp(post, O, List.of(), List.of(), List.of(Binding.van("$pot", post, "hoogte")));
        Accumulatie ontvangers = new Accumulatie(new Patroon(Ontvanger.class, "$ontvanger", List.of(new Alfa("verdeling",
                List.of(new Alfa.Veldtoets("verdeling", Alfa.Vergelijker.GELIJK, "proefverdeling")))),
                List.of(new Join("ontvanger-bron", "bron", O)), List.of()),
                List.of(new Accumulatie.Functie(Accumulatie.Soort.LIJST, null, "$ontvangers")));
        Leestabel uitdeling = tabel(new Lezing(0, Leessoort.INVOER, "hoogte", "$pot"),
                new Lezing(1, Leessoort.ONTVANGERS, "ontvangers proefverdeling", "$ontvangers"));
        ElementSleutel aandeel = ElementSleutel.van("wachter", "uitkijkbonus", false);
        regels.add(new Regelplan("uitdeling naar rato", "proefverdeling", null, uitdeling, List.of(pot, ontvangers),
                new Gevolg.Uitdeling(O, "proefverdeling", ResolvedVerdeling.Methode.NAAR_RATO, 0, 1, true, true, false, 2,
                        aandeel, ElementSleutel.van("post", "hoogte", false), "proefverdeling", Gevolg.Invoeging.LOGISCH)));
        regels.add(new Regelplan("uitdeling op volgorde", "proefverdeling", null, uitdeling, List.of(pot, ontvangers),
                new Gevolg.Uitdeling(O, "proefverdeling", ResolvedVerdeling.Methode.OP_VOLGORDE, 0, 1, false, false, true,
                        null, aandeel, null, "proefverdeling", Gevolg.Invoeging.LOGISCH)));
        regels.add(new Regelplan("uitdeling in gelijke delen", "proefverdeling", null, uitdeling, List.of(pot, ontvangers),
                new Gevolg.Uitdeling(O, "proefverdeling", ResolvedVerdeling.Methode.GELIJKE_DELEN, 0, 1, false, false,
                        false, 0, aandeel, null, "proefverdeling", Gevolg.Invoeging.LOGISCH)));
        return new Modelplan(List.of(), regels);
    }

    /** Fragments of the text that show the forms of the plan of the creations and the links ({@link #creaties}), one
     *  per form. */
    static final List<String> CREATIEVORMEN = List.of(
            "// uitkijkbonus\nfunction Object r0_creatie0(Object $uitkijkbonus, Object $fase, Object $herkomsten,"
                    + " Object $onderwerp) {",
            "    int diepte = Rekenhulpen.creatiediepte(\"proefcreatie\", 0, \"Wachter\", (java.util.List) $herkomsten,"
                    + " 3);",
            "    Object i0 = null;\n    Object i1 = null;\n    try {\n        i0 = Rekenhulpen.collapse($uitkijkbonus);",
            "    } catch (RuntimeException e) {\n        return Rekenhulpen.gevangen(e);\n    }",
            "    Object nieuw = Rekenhulpen.nieuwObject($onderwerp, \"dev.uchar.regelspraak.facts.Post\");\n"
                    + "    Rekenhulpen.zet(nieuw, \"hoogte\", i0);",
            "    int fase = ((Integer) $fase).intValue();",
            "    feiten.add(nieuw);\n    feiten.add(new Gemaakt(\"proefcreatie\", $onderwerp, nieuw, fase, 0, diepte));\n"
                    + "    feiten.add(new Koppeling(\"wachterpost\", $onderwerp, nieuw, fase));\n"
                    + "    feiten.add(Boolean.TRUE.equals(i1) ? new HeeftKenmerk(nieuw, ElementSleutel.van(\"post\","
                    + " \"bemand\", true)) : null);\n    return feiten;",
            "    agenda-group \"uchar-objectcreatie\"",
            "    if (Rekenhulpen.isEvaluatiefout(v)) {\n        insert(new Fout($onderwerp, \"proefcreatie\","
                    + " Rekenhulpen.benoem(v, \"proefcreatie\")));\n    } else {\n"
                    + "        java.util.List feiten = (java.util.List) v;\n        insert(feiten.get(0));\n"
                    + "        insert(feiten.get(1));\n        insert(feiten.get(2));\n"
                    + "        if (feiten.get(3) != null) {\n            insertLogical(feiten.get(3));\n        }\n    }",
            "function Object r1_creatie0(Object $fase, Object $herkomsten, Object $onderwerp) {\n    int diepte =",
            "    Object nieuw = Rekenhulpen.nieuwObject($onderwerp, \"dev.uchar.regelspraak.facts.Wachter\");\n"
                    + "    int fase",
            "    feiten.add(new Koppeling(\"opvolging\", nieuw, $onderwerp, fase));",
            "    insertLogical(new Koppeling(\"wachterpost\", $onderwerp, $post, 0));",
            "    agenda-group \"uchar-feitcreatie\"",
            "    insert(new Koppeling(\"wachterpost\", $onderwerp, $post, $fase));",
            "    insertLogical(new Fout($onderwerp, \"proefrelatie\", new IllegalStateException(\"een tweede vulling\")));");

    /**
     * A plan with every consequence of a creation and of a link (step 5 of card drl-teksttak-op-truth-maintenance), over
     * the beans of the same document as {@link #plan}: an object creation of a Post for a Wachter, in its agenda group,
     * with an attribute initialisation set on the new bean and a characteristic initialisation inserted with an
     * insertion of its own, each from a function that can fault; one of a Wachter without initialisations, whose new
     * object is the first end of its link; the link a reference field of the input carries, at phase 0, and the link of
     * a fact creation, in its agenda group, at the phase it binds; and the fault of a second filling. No rule of it is
     * meant to fire.
     */
    static Modelplan creaties(Class<?> wachter, Class<?> post) {
        List<Regelplan> regels = new ArrayList<>();
        Patroon fase = new Patroon(Creatiefase.class, "$creatiefase", List.of(new Alfa("creatiestratum", List.of(
                new Alfa.Veldtoets("stratum", Alfa.Vergelijker.MINSTENS, 0)))), List.of(),
                List.of(Binding.van("$fase", Creatiefase.class, "nummer")));
        Accumulatie herkomst = new Accumulatie(new Patroon(Gemaakt.class, "$herkomst", List.of(),
                List.of(new Join("herkomst", "object", O)), List.of()),
                List.of(new Accumulatie.Functie(Accumulatie.Soort.LIJST, null, "$herkomsten")));
        Gevolg.Foutnaam fout = new Gevolg.Foutnaam("proefcreatie", "proefcreatie");
        regels.add(new Regelplan("creatie met initialisaties", "proefcreatie", "uchar-objectcreatie",
                tabel(new Lezing(0, Leessoort.INVOER, "uitkijkbonus", "$uitkijkbonus"),
                        new Lezing(1, Leessoort.CREATIEFASE, "creatiefase", "$fase"),
                        new Lezing(2, Leessoort.HERKOMST, "herkomst", "$herkomsten")),
                List.of(new Onderwerp(wachter, O, List.of(), List.of(),
                        List.of(Binding.van("$uitkijkbonus", wachter, "uitkijkbonus"))), fase, herkomst),
                new Gevolg.Creatie(O, "proefcreatie", new Gevolg.Creatiediepte(2, 0, "Wachter", 3), List.of(
                        new Gevolg.Initialisatie(ElementSleutel.van("post", "hoogte", false), new Term.Lees(0,
                                "uitkijkbonus")),
                        new Gevolg.Initialisatie(ElementSleutel.van("post", "bemand", true),
                                new Term.Const(Boolean.TRUE, null, null))),
                        fout, Gevolg.Invoeging.GESTELD, new Gevolg.Constructie(post, List.of(
                                new Gevolg.Constructie.Zetting(0, new Binding.Veld(post, "hoogte")))), 1,
                        List.of(new Gevolg.Invoegsel.Nieuw(Gevolg.Invoeging.GESTELD),
                                new Gevolg.Invoegsel.Merk(Gevolg.Invoeging.GESTELD),
                                new Gevolg.Invoegsel.Link("wachterpost", false, Gevolg.Invoeging.GESTELD),
                                new Gevolg.Invoegsel.Kenmerk(1, Gevolg.Invoeging.LOGISCH)),
                        List.of(0, 1, 2), 3)));
        regels.add(new Regelplan("creatie zonder initialisaties", "proefcreatie", "uchar-objectcreatie",
                tabel(new Lezing(0, Leessoort.CREATIEFASE, "creatiefase", "$fase"),
                        new Lezing(1, Leessoort.HERKOMST, "herkomst", "$herkomsten")),
                List.of(new Onderwerp(wachter, O, List.of(), List.of(), List.of()), fase, herkomst),
                new Gevolg.Creatie(O, "proefopvolging", new Gevolg.Creatiediepte(1, 0, "Wachter", 3), List.of(), fout,
                        Gevolg.Invoeging.GESTELD, new Gevolg.Constructie(wachter, List.of()), 0,
                        List.of(new Gevolg.Invoegsel.Nieuw(Gevolg.Invoeging.GESTELD),
                                new Gevolg.Invoegsel.Merk(Gevolg.Invoeging.GESTELD),
                                new Gevolg.Invoegsel.Link("opvolging", true, Gevolg.Invoeging.GESTELD)),
                        List.of(0, 1), 2)));
        Onderwerp metPost = new Onderwerp(wachter, O, List.of(), List.of(), List.of(Binding.van("$post", wachter,
                "post")));
        regels.add(new Regelplan("koppeling uit de invoer", null, null, tabel(), List.of(metPost),
                new Gevolg.Koppeling("wachterpost", O, "$post", -1, Gevolg.Invoeging.LOGISCH)));
        regels.add(new Regelplan("koppeling van een feitcreatie", "proeffeitcreatie", "uchar-feitcreatie",
                tabel(new Lezing(0, Leessoort.CREATIEFASE, "creatiefase", "$fase")), List.of(metPost, fase),
                new Gevolg.Koppeling("wachterpost", O, "$post", 0, Gevolg.Invoeging.GESTELD)));
        regels.add(new Regelplan("tweede vulling", null, null, tabel(),
                List.of(new Onderwerp(wachter, O, List.of(), List.of(), List.of())),
                new Gevolg.TweedeVulling(O, "proefrelatie", "een tweede vulling", Gevolg.Invoeging.LOGISCH)));
        return new Modelplan(List.of(), regels);
    }

    /** Fragments of the text that show the forms of the plan of a uniqueness and the channel ({@link #kanaal}), one per
     *  form. */
    static final List<String> KANAALVORMEN = List.of("_componenten", "java.util.List waarden = new java.util.ArrayList();",
            "waarden.add(null);", "waarden.add(Rekenhulpen.terugval(", "// zijn uitkijkbonus; zijn uitkijkbonus of 0",
            "for (Object k : Rekenhulpen.uniciteitssleutels(\"proefuniciteit\", 0, $onderwerp, null, ",
            "for (Object k : Rekenhulpen.uniciteitssleutels(\"proefuniciteit\", 1, $onderwerp, $kustvak, ",
            ", -1)) {", ", 0)) {", "        insertLogical(k);",
            "insertLogical(new Uniciteitsschending(\"proefuniciteit\", $onderwerp));",
            "insertLogical(new Fout($onderwerp, \"proefuniciteit\", Rekenhulpen.invoervormfout(\"the key \\\"uitkijkbonus\\\""
                    + " needs the full timeline; \", $onderwerp, \" supplies a scalar\", \"proefuniciteit\")));",
            "insertLogical(new KanaalStand(\"proef\", true));", "insertLogical(new KanaalStand(\"proef\", false));");

    /**
     * A plan with every consequence of the rules of a uniqueness and of the §9.5 channel (step 3b of card
     * drl-teksttak-op-truth-maintenance), over the beans of the same document as {@link #plan}: the keys of a Wachter
     * under a form without a scope, over its uitkijkbonus, a component that is null and one through a function, each
     * with its sentence; the keys of a Post under a form whose scope is its Kustvak, over a component on a timeline;
     * the violation of the uniqueness; the fault of the form of an input, with a quote in its sentence; and the
     * standing of the channel of a form, inconsistent where an inconsistency of that form stands and consistent where
     * none does. No rule of it is meant to fire.
     */
    static Modelplan kanaal(Class<?> wachter, Class<?> post, Class<?> inconsistentie) {
        List<Regelplan> regels = new ArrayList<>();
        Onderwerp metBonus = new Onderwerp(wachter, O, List.of(), List.of(),
                List.of(Binding.van("$uitkijkbonus", wachter, "uitkijkbonus")));
        regels.add(new Regelplan("sleutel", "proefuniciteit", null,
                tabel(new Lezing(0, Leessoort.INVOER, "uitkijkbonus", "$uitkijkbonus")), List.of(metBonus),
                new Gevolg.Uniciteitssleutel(O, "proefuniciteit", 0, List.of(new Term.Lees(0, "zijn uitkijkbonus"),
                        new Term.Const(null, null, null), new Term.Aanroep(Functie.TERUGVAL, List.of(lees(0), getal("0")),
                                "zijn uitkijkbonus of 0")), -1, -1, List.of(0), 1, Gevolg.Invoeging.LOGISCH)));
        Onderwerp metKustvak = new Onderwerp(post, O, List.of(), List.of(),
                List.of(Binding.van("$hoogte", post, "hoogte"), Binding.van("$kustvak", post, "kustvak")));
        regels.add(new Regelplan("sleutel met scope en tijdlijn", "proefuniciteit", null,
                tabel(hoogteslot(), new Lezing(1, Leessoort.SCOPE, "kustvak", "$kustvak")), List.of(metKustvak),
                new Gevolg.Uniciteitssleutel(O, "proefuniciteit", 1, List.of(lees(0)), 0, 1, List.of(0, 1), 2,
                        Gevolg.Invoeging.LOGISCH)));
        Onderwerp onderwerp = new Onderwerp(wachter, O, List.of(), List.of(), List.of());
        regels.add(new Regelplan("schending", "proefuniciteit", null, tabel(), List.of(onderwerp),
                new Gevolg.Uniciteitsschending(O, "proefuniciteit", Gevolg.Invoeging.LOGISCH)));
        regels.add(new Regelplan("invoervorm", "proefuniciteit", null, tabel(), List.of(onderwerp),
                new Gevolg.Invoervormfout(O, "the key \"uitkijkbonus\" needs the full timeline; ", " supplies a scalar",
                        new Gevolg.Foutnaam("proefuniciteit", "proefuniciteit"), Gevolg.Invoeging.LOGISCH)));
        List<Patroon> kanaal = List.of(new Patroon(inconsistentie, "$inconsistentie", List.of(new Alfa("kanaal",
                List.of(new Alfa.Veldtoets("regel", Alfa.Vergelijker.GELIJK, "proef")))), List.of(), List.of()));
        regels.add(new Regelplan("kanaalstand proef: inconsistent", null, null, tabel(),
                List.of(new Bestaat(kanaal, Bestaat.verklaard(kanaal), Bestaat.gelezen(kanaal))),
                new Gevolg.Kanaalstand("proef", true, Gevolg.Invoeging.LOGISCH)));
        regels.add(new Regelplan("kanaalstand proef: consistent", null, null, tabel(),
                List.of(new Niet(kanaal, Bestaat.verklaard(kanaal), Bestaat.gelezen(kanaal))),
                new Gevolg.Kanaalstand("proef", false, Gevolg.Invoeging.LOGISCH)));
        return new Modelplan(List.of(), regels);
    }

    /** Fragments of the text that show the forms of the plan of the defaults ({@link #defaults}), one per form. */
    static final List<String> DEFAULTSVORMEN = List.of("Object r0_afloop0(Object $uitkijkbonus)",
            "        return Rekenhulpen.collapse($uitkijkbonus);\n    } catch (RuntimeException e) {\n"
                    + "        return Rekenhulpen.gevangen(e);",
            "    if (Rekenhulpen.isEvaluatiefout(v)) {\n        insertLogical(new Fout($onderwerp, \"proefstapel\","
                    + " Rekenhulpen.benoem(v, \"proefstapel\")));\n    } else if (v != null) {",
            "insertLogical(new Waarde($onderwerp, ElementSleutel.van(\"wachter\", \"uitkijkbonus\", false), v,"
                    + " \"proefstapel\"));",
            "insertLogical(new Beslist($onderwerp, \"proefstapel\", 0));",
            "Object r1_afloop0() {\n    return Boolean.TRUE;\n}",
            "insertLogical(new HeeftKenmerk($onderwerp, ElementSleutel.van(\"wachter\", \"waakzaam\", true)));",
            "Object r2_afloop0() {\n    return Boolean.FALSE;\n}",
            "    if (v != null) {\n        insertLogical(new Beslist($onderwerp, \"proefstapel\", 2));\n    }",
            "insertLogical(new Uitkomst($onderwerp, \"proefstapel\", 1, 2,"
                    + " dev.uchar.regelspraak.transpiler.tms.Uitkomst.Soort.BESLIST, v, null));",
            "Object r4_conflict0() {\n    return \"proefstapel/0\";\n}",
            "insertLogical(new Uitkomst($onderwerp, \"proefstapel\", 0, 1,"
                    + " dev.uchar.regelspraak.transpiler.tms.Uitkomst.Soort.CONFLICT, null, (String) v));",
            "insertLogical(Rekenhulpen.inconsistentie((String) v, $onderwerp));");

    /**
     * A plan with every consequence of the rules of a defaults stack (step 4 of card drl-teksttak-op-truth-maintenance),
     * over the Wachter of the same document as {@link #plan}: the decision of the simple form on an attribute, computed
     * and collapsed, with its fault and its control fact; the attribution of a characteristic and the decision not to
     * attribute it, constants, each with its control fact; the decision of a reconciliation bound off the outcome of a
     * member, inserted as an outcome; and the conflict of a reconciliation, under a constant key as an outcome and under
     * the key of a member as the §9.5 inconsistency. No rule of it is meant to fire.
     */
    static Modelplan defaults(Class<?> wachter) {
        List<Regelplan> regels = new ArrayList<>();
        Onderwerp metBonus = new Onderwerp(wachter, O, List.of(), List.of(),
                List.of(Binding.van("$uitkijkbonus", wachter, "uitkijkbonus")));
        regels.add(new Regelplan("default 0", "proefstapel", null,
                tabel(new Lezing(0, Leessoort.INVOER, "uitkijkbonus", "$uitkijkbonus")), List.of(metBonus),
                new Gevolg.Afloop(O, lees(0), List.of(0), 1, true, new Gevolg.Foutnaam("proefstapel", "proefstapel"),
                        List.of(new Gevolg.Stapelfeit.Waarde(ElementSleutel.van("wachter", "uitkijkbonus", false),
                                "proefstapel"), new Gevolg.Stapelfeit.Beslist("proefstapel", 0)),
                        Gevolg.Invoeging.LOGISCH)));
        Onderwerp onderwerp = new Onderwerp(wachter, O, List.of(), List.of(), List.of());
        regels.add(new Regelplan("default 1", "proefstapel", null, tabel(), List.of(onderwerp),
                new Gevolg.Afloop(O, new Term.Const(Boolean.TRUE, null, null), List.of(), 0, false, null,
                        List.of(new Gevolg.Stapelfeit.Kenmerk(ElementSleutel.van("wachter", "waakzaam", true)),
                                new Gevolg.Stapelfeit.Beslist("proefstapel", 1)), Gevolg.Invoeging.LOGISCH)));
        regels.add(new Regelplan("default 2", "proefstapel", null, tabel(), List.of(onderwerp),
                new Gevolg.Afloop(O, new Term.Const(Boolean.FALSE, null, null), List.of(), 0, false, null,
                        List.of(new Gevolg.Stapelfeit.Beslist("proefstapel", 2)), Gevolg.Invoeging.LOGISCH)));
        Patroon besluit = new Patroon(Uitkomst.class, "$besluit", List.of(new Alfa("lid-besluit", List.of(
                new Alfa.Veldtoets("soort", Alfa.Vergelijker.GELIJK, Uitkomst.Soort.BESLIST)))), List.of(),
                List.of(Binding.van(O, Uitkomst.class, "onderwerp"), Binding.van("$waarde", Uitkomst.class, "waarde")));
        regels.add(new Regelplan("eens 0", "proefstapel", null, tabel(new Lezing(0, Leessoort.BESLUIT, "besluit",
                "$waarde")), List.of(besluit), new Gevolg.Afloop(O, lees(0), List.of(0), 1, false, null,
                List.of(new Gevolg.Stapelfeit.Uitkomst("proefstapel", 1, 2, Uitkomst.Soort.BESLIST)),
                Gevolg.Invoeging.LOGISCH)));
        regels.add(new Regelplan("verschil 0", "proefstapel", null, tabel(), List.of(onderwerp),
                new Gevolg.Conflict(O, tekst("proefstapel/0"), List.of(), 0,
                        new Gevolg.Stapelfeit.Uitkomst("proefstapel", 0, 1, Uitkomst.Soort.CONFLICT),
                        Gevolg.Invoeging.LOGISCH)));
        Patroon conflict = new Patroon(Uitkomst.class, "$conflict", List.of(new Alfa("lid-conflict", List.of(
                new Alfa.Veldtoets("soort", Alfa.Vergelijker.GELIJK, Uitkomst.Soort.CONFLICT)))), List.of(),
                List.of(Binding.van(O, Uitkomst.class, "onderwerp"), Binding.van("$sleutel", Uitkomst.class, "sleutel")));
        regels.add(new Regelplan("doorgeven wortel", "proefstapel", null, tabel(new Lezing(0, Leessoort.CONFLICTSLEUTEL,
                "conflictsleutel", "$sleutel")), List.of(conflict), new Gevolg.Conflict(O, lees(0), List.of(0), 1,
                new Gevolg.Stapelfeit.Inconsistentie(), Gevolg.Invoeging.LOGISCH)));
        return new Modelplan(List.of(), regels);
    }

    /** Fragments of the text that show the forms of the fold plan ({@link #vouwen}), one per form. */
    static final List<String> VOUWVORMEN = List.of("new Bijdrage(\"proefvouw: telling\", $vouwonderwerp, $onderwerp, null)",
            "Rekenhulpen.bijdrage(\"proefvouw: som\"", "\"MAAND\", true)", "Rekenhulpen.oordeel(", "_matrix",
            "_lidtoets", "new Lidtoets(plaats, waar, fout, Rekenhulpen.gemeld(sonde))",
            "new Lidtoets(plaats, waar, fout, Rekenhulpen.gemeld(null))", "_plaats",
            "Rekenhulpen.vouwplaats(dev.uchar.regelspraak.transpiler.tms.plan.Gevolg.Plaatsreden.KWANTOR",
            "java.util.Arrays.asList(\"kustvak\", \"wachter\")", "java.util.Arrays.asList()",
            "new Filterdeponering(", "RuntimeException f = ", "new Filterfout(", "new Lidfout(",
            "Rekenhulpen.invoervormfout(", "Rekenhulpen.plaatsVan(", "Rekenhulpen.ontmoeting(", "Lijstplaats(");

    /**
     * A plan with every consequence of the rules of a fold (step 3a of card drl-teksttak-op-truth-maintenance), over the
     * beans of the same document as {@link #plan}: the members are the Posts of a Kustvak, joined as the analysis joins
     * a member to the fold's subject. A member's contribution of each kind (its count, its value, its value as a total
     * per month and counted where it is filled, the verdict of a matrix, and the test of a matrix with a probe and a
     * place of two steps in the second selection, or without either), the deposit of a filter, the fault of a filter
     * and the fault of a member, each with the accumulates over the places of the steps that the analysis plans beside
     * them, one through the constraint of a place and one through the constraint of a meeting. No rule of it is meant
     * to fire.
     */
    static Modelplan vouwen(Class<?> wachter, Class<?> post, Class<?> kustvak) {
        String v = "$vouwonderwerp";
        Patroon vouwonderwerp = new Patroon(kustvak, v, List.of(), List.of(), List.of());
        Binding hoogte = Binding.van("$hoogte", post, "hoogte");
        Term matrix = new Term.Aanroep(Functie.RS_AGG_VERGELIJK, List.of(lees(0), getal("5"), tekst(">")),
                "de hoogte groter dan 5");
        Term deponerend = new Term.Aanroep(Functie.RS_AGG_VERGELIJK, List.of(new Term.Deel(Functie.GEDEELD, lees(0),
                getal("2"), 6, "de hoogte gedeeld door 2"), getal("1"), tekst(">")), "de helft groter dan 1");
        Term falend = new Term.Aanroep(Functie.RS_AGG_VERGELIJK, List.of(new Term.Deel(Functie.GEDEELD, lees(0),
                getal("0"), -1, "de hoogte gedeeld door 0"), getal("1"), tekst(">")), "het filter dat faalt");
        List<Regelplan> regels = new ArrayList<>();
        regels.add(new Regelplan("telling", "proef", null, tabel(new Lezing(0, Leessoort.ONDERWERP, "lid", O)),
                List.of(vouwonderwerp, lid(post, v)), new Gevolg.Bijdrage(v, "proefvouw: telling", 0,
                new Gevolg.Inbreng.Telling(), List.of(0), 1, Gevolg.Invoeging.LOGISCH)));
        regels.add(new Regelplan("lidwaarde", "proef", null, tabel(hoogteslot(), lidslot(1)),
                List.of(vouwonderwerp, lid(post, v, hoogte)), new Gevolg.Bijdrage(v, "proefvouw: som", 1,
                new Gevolg.Inbreng.Lidwaarde(0, null, false), List.of(0, 1), 2, Gevolg.Invoeging.LOGISCH)));
        regels.add(new Regelplan("lidwaarde per maand", "proef", null, tabel(hoogteslot(), lidslot(1)),
                List.of(vouwonderwerp, lid(post, v, hoogte)), new Gevolg.Bijdrage(v, "proefvouw: totaal", 1,
                new Gevolg.Inbreng.Lidwaarde(0, "MAAND", true), List.of(0, 1), 2, Gevolg.Invoeging.LOGISCH)));
        regels.add(new Regelplan("oordeel", "proef", null, tabel(hoogteslot(), lidslot(1)),
                List.of(vouwonderwerp, lid(post, v, hoogte)), new Gevolg.Bijdrage(v, "proefvouw: oordeel", 1,
                new Gevolg.Inbreng.Oordeel(matrix), List.of(0, 1), 2, Gevolg.Invoeging.LOGISCH)));
        regels.add(new Regelplan("lidtoets", "proef", null, tabel(hoogteslot(), lidslot(1),
                plaatsslot(2, Leessoort.LIJSTPLAATSEN, "$lijstplaatsen_1"), plaatsslot(3, Leessoort.LIJSTPLAATS, "$plaats_1"),
                plaatsslot(4, Leessoort.LIJSTPLAATSEN, "$lijstplaatsen_2"), plaatsslot(5, Leessoort.LIJSTPLAATS, "$plaats_2"),
                new Lezing(6, Leessoort.SONDE, null, null)),
                List.of(vouwonderwerp, lid(post, v, hoogte), plaatsen(1, "kustvak", new Betatoets("plaats-1",
                        Functie.PLAATS_VAN, O, List.of(new Term.Const(List.of(), null, null)))),
                        plaatsen(2, "wachter", new Betatoets("plaats-2", Functie.ONTMOETING, O, List.of(
                                new Term.Const(List.of(), null, null), new Term.Const(List.of("wachter"), null, null))))),
                new Gevolg.Bijdrage(v, "proefvouw: lidtoets", 1, new Gevolg.Inbreng.Lidtoets(deponerend, 6,
                        new Gevolg.Vouwplaats(1, List.of(new Gevolg.Vouwplaats.Stap("kustvak", 2, 3),
                                new Gevolg.Vouwplaats.Stap("wachter", 4, 5)), Gevolg.Plaatsreden.KWANTOR)),
                        List.of(0, 1, 2, 3, 4, 5), 7, Gevolg.Invoeging.LOGISCH)));
        regels.add(new Regelplan("lidtoets zonder plaats", "proef", null, tabel(hoogteslot(), lidslot(1)),
                List.of(vouwonderwerp, lid(post, v, hoogte)), new Gevolg.Bijdrage(v, "proefvouw: lidtoets", 1,
                new Gevolg.Inbreng.Lidtoets(matrix, -1, new Gevolg.Vouwplaats(0, List.of(), Gevolg.Plaatsreden.KWANTOR)),
                List.of(0, 1), 2, Gevolg.Invoeging.LOGISCH)));
        regels.add(new Regelplan("filterdeponering", "proef", null, tabel(new Lezing(0, Leessoort.ONDERWERP, "lid", O)),
                List.of(vouwonderwerp, lid(post, v)), new Gevolg.Filterdeponering(v, "proefvouw", 0,
                Gevolg.Invoeging.LOGISCH)));
        regels.add(new Regelplan("filterfout", "proef", null, tabel(hoogteslot(), lidslot(1),
                plaatsslot(2, Leessoort.LIJSTPLAATSEN, "$lijstplaatsen_1"), plaatsslot(3, Leessoort.LIJSTPLAATS, "$plaats_1")),
                List.of(vouwonderwerp, lid(post, v, hoogte), plaatsen(1, "kustvak", new Betatoets("plaats-1",
                        Functie.PLAATS_VAN, O, List.of(new Term.Const(List.of(), null, null)))),
                        new Toetsitem("fout", Toetsitem.Rol.FOUT, falend, -1, List.of(0), 4)),
                new Gevolg.Filterfout(v, "proefvouw", 1, falend, new Gevolg.Vouwplaats(0,
                        List.of(new Gevolg.Vouwplaats.Stap("kustvak", 2, 3)), Gevolg.Plaatsreden.FILTERFOUT),
                        List.of(0, 1, 2, 3), 4, Gevolg.Invoeging.LOGISCH)));
        regels.add(new Regelplan("lidfout", "proef", null, tabel(new Lezing(0, Leessoort.ONDERWERP, "lid", O),
                plaatsslot(1, Leessoort.LIJSTPLAATSEN, "$lijstplaatsen_1"), plaatsslot(2, Leessoort.LIJSTPLAATS, "$plaats_1")),
                List.of(vouwonderwerp, lid(post, v), plaatsen(1, "kustvak", new Betatoets("plaats-1",
                        Functie.PLAATS_VAN, O, List.of(new Term.Const(List.of(), null, null))))),
                new Gevolg.Lidfout(v, "proefvouw", 0, new Gevolg.Vouwplaats(0,
                        List.of(new Gevolg.Vouwplaats.Stap("kustvak", 1, 2)), Gevolg.Plaatsreden.LIDFOUT),
                        "the total of ", ": a scalar", Gevolg.Invoeging.LOGISCH)));
        return new Modelplan(List.of(), regels);
    }

    private static Leestabel tabel(Lezing... slots) {
        return new Leestabel(List.of(slots));
    }

    private static Lezing hoogteslot() {
        return new Lezing(0, Leessoort.INVOER, "hoogte", "$hoogte");
    }

    private static Lezing lidslot(int slot) {
        return new Lezing(slot, Leessoort.ONDERWERP, "lid", O);
    }

    private static Lezing plaatsslot(int slot, Leessoort soort, String naam) {
        return new Lezing(slot, soort, naam, naam);
    }

    /** The member, a Post of the fold's subject, bound as the subject of a rule of the fold. */
    private static Onderwerp lid(Class<?> post, String vouwonderwerp, Binding... bindingen) {
        return new Onderwerp(post, O, List.of(), List.of(new Join("bereik", "kustvak", vouwonderwerp)),
                List.of(bindingen));
    }

    /** The accumulate over the places under {@code veld} of the step {@code n}: their count and the least of them. */
    private static Accumulatie plaatsen(int n, String veld, Betatoets vanLid) {
        return new Accumulatie(new Patroon(Lijstplaats.class, "$lijstplaats_" + n, List.of(new Alfa("veld-" + n,
                List.of(new Alfa.Veldtoets("veld", Alfa.Vergelijker.GELIJK, veld)))), List.of(), List.of(vanLid),
                List.of(Binding.van("$lijstplaatswaarde_" + n, Lijstplaats.class, "plaats"))),
                List.of(new Accumulatie.Functie(Accumulatie.Soort.AANTAL, null, "$lijstplaatsen_" + n),
                        new Accumulatie.Functie(Accumulatie.Soort.MINIMUM, "$lijstplaatswaarde_" + n, "$plaats_" + n)));
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
