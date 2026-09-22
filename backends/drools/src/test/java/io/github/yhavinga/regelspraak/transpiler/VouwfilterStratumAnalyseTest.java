package io.github.yhavinga.regelspraak.transpiler;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.yhavinga.regelspraak.ir.ResolvedRegel;
import io.github.yhavinga.regelspraak.loader.IrLoader;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * §9.2 / RUNTIME-SEMANTICS ch. 6.1 — the stratification analysis over the reads an EXPRESSION operand
 * hides (an aggregation's die/dat filter, a §5.7 union selection's filter). Two properties that the walk
 * which found those reads did not have when it landed on 2026-09-22, each measured on its own document.
 *
 * <p>This file asks the ANALYSIS directly rather than replaying a case, because both defects are about a
 * stratum NUMBER: one produces the wrong band, the other the wrong edge, and a replay only shows the
 * subset of wrong numbers that happen to change an answer on the case at hand.
 */
class VouwfilterStratumAnalyseTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** <b>Polarity under a compound quantifier.</b> {@code Samengesteld} with GEEN / TEN_HOOGSTE / PRECIES
     *  is negation-like: RUNTIME-SEMANTICS ch. 6.1 says so and the verifier's {@code dep_cond} implements
     *  it ("negation-like: conservative"). The walk read the polarity out of {@code Kenmerk.negatie()}
     *  alone and entered a compound with no negation context, so "aan GEEN van de volgende voorwaarden
     *  voldoen: gemerkt zijn" — the same sentence as "niet gemerkt", differently spelled — produced a
     *  POSITIVE read and left the consumer in band 0. Measured 2026-09-22 on a GEEN-rewrite of
     *  {@code jutterswet-uniestratum}: ref and the ASP backend PASS with 11 expectations, both Drools
     *  branches FAIL with {@code verwacht afwezig, werkelijk aanwezig}. */
    @Test void een_kenmerk_onder_GEEN_is_een_negatieve_rand() throws Exception {
        assertEquals(-1, salience(doc(filter("GEEN")), "consument"),
                "een kenmerk onder GEEN is negatief gelezen (ch. 6.1), dus de consument zinkt een band");
        assertEquals(-1, salience(doc(filter("TEN_HOOGSTE")), "consument"), "TEN_HOOGSTE is negatie-achtig");
        assertEquals(-1, salience(doc(filter("PRECIES")), "consument"), "PRECIES is negatie-achtig");
    }

    /** The other half of the same clause: ALLE / DE / TEN_MINSTE pass the polarity THROUGH rather than
     *  flipping it, so a positive read under one of them stays positive and the consumer stays in band 0.
     *  Without this row the fix could have been "treat every compound as negative", which is sound but
     *  sinks rules that have no reason to sink — and a band that only ever deepens is a band nobody
     *  notices is wrong. */
    @Test void een_kenmerk_onder_ALLE_blijft_positief() throws Exception {
        assertEquals(0, salience(doc(filter("ALLE")), "consument"),
                "ALLE geeft de polariteit door; een positieve lezing blijft positief");
        assertEquals(0, salience(doc(filter("TEN_MINSTE")), "consument"), "TEN_MINSTE geeft door");
    }

    /** <b>The surrounding subject.</b> A fold whose {@code elementObjecttype} is absent handed the filter
     *  walk a null subject, and null means the NAME-WIDE producer match — a fallback
     *  {@link Stratificatie} reserves for an unresolved NAVIGATED read alone. The verifier keeps the
     *  enclosing subject and falls back to the element type only when there IS one
     *  ({@code fsubject = elementObjecttype else subject}). Measured 2026-09-22: with a same-named
     *  characteristic produced on an UNRELATED object type, one band deep, the consumer landed in band
     *  −2 where −1 is normative — the homonym on the other type is no edge at all. A false edge is not
     *  only a wrong number: it can close a false CYCLE and trip the cycle-local fallback. */
    @Test void een_vouwfilter_zonder_elementtype_leest_op_het_omliggende_onderwerp() throws Exception {
        assertEquals(-1, salience(homoniemDoc(), "consument"),
                "de gelijknamige producent op een ANDER objecttype vormt geen rand");
        // the control: the analysis still SEES the producer chain it should, on its own type
        assertEquals(-1, salience(homoniemDoc(), "ketenstap"), "de echte keten op Vondst blijft staan");
    }

    // --- the documents ------------------------------------------------------------------------------

    private static int salience(String ir, String regel) throws Exception {
        List<ResolvedRegel> regels = IrLoader.loadAll(JSON.readTree(ir)).model().resolvedRegels;
        Stratificatie s = Stratificatie.bereken(regels);
        return s.salience(regels.stream().filter(r -> regel.equals(r.naam())).findFirst().orElseThrow());
    }

    private static final String HEEL =
            "{\"node\":\"Numeriek\",\"geheel\":true,\"decimalen\":0,\"teken\":\"NIET_NEGATIEF\",\"eenheid\":null}";
    private static final String BOOL = "{\"node\":\"Booleaan\"}";

    /** A positive {@code gemerkt} check wrapped in a compound under {@code kwantor}. */
    private static String filter(String kwantor) {
        return ("{\"node\":\"Samengesteld\",\"kwantor\":\"%s\",\"drempel\":1,\"onderdelen\":["
                + "{\"node\":\"Kenmerk\",\"kenmerk\":\"gemerkt\",\"navigatie\":null,\"negatie\":false,"
                + "\"eigenaar\":null,\"periode\":null,\"bron\":\"gemerkt zijn\"}],"
                + "\"bron\":\"de samengestelde deelselectie\"}").formatted(kwantor);
    }

    private static String telling(String elementType, String filter, String bronRol) {
        return ("{\"node\":\"Functie\",\"kind\":\"AANTAL\",\"argumenten\":[{\"node\":\"AttribuutRef\","
                + "\"attribuut\":\"vondsten\",\"objecttype\":\"Jutter\",\"onderwerpBron\":\"de Jutter\","
                + "\"navigatieRol\":null,\"type\":{\"node\":\"Onbekend\",\"reden\":\"niet opgelost\"},"
                + "\"bron\":\"vondsten van de Jutter\",\"set\":false,\"navigatiePad\":[]}],"
                + "\"elementObjecttype\":%s,\"leegwaarde\":null,\"filter\":%s,\"dimensieSelectie\":null,"
                + "\"type\":%s,\"bron\":\"het aantal …\",\"granulariteit\":null%s}")
                .formatted(elementType, filter, HEEL, bronRol);
    }

    private static String kenmerkRegel(String naam, String kenmerk, String ot, String voorwaarde) {
        return ("{\"node\":\"ResolvedRegel\",\"naam\":\"%s\",\"soort\":\"KENMERKTOEKENNING\","
                + "\"doel\":{\"node\":\"AttribuutRef\",\"attribuut\":\"%s\",\"objecttype\":\"%s\","
                + "\"onderwerpBron\":\"Een %s\",\"navigatieRol\":null,\"type\":%s,\"bron\":\"Een %s is %s\","
                + "\"set\":false,\"navigatiePad\":[]},"
                + "\"waarde\":{\"node\":\"Literal\",\"waarde\":\"waar\",\"type\":%s,\"bron\":\"waar\"},"
                + "\"voorwaarde\":%s,\"geldigheid\":{\"node\":\"Geldigheid\",\"vanaf\":null,\"tot\":null}}")
                .formatted(naam, kenmerk, ot, ot, BOOL, ot, kenmerk, BOOL, voorwaarde);
    }

    private static String groterDanNul(String links) {
        return ("{\"node\":\"Vergelijking\",\"op\":\"GROTER\",\"links\":%s,"
                + "\"rechts\":{\"node\":\"Literal\",\"waarde\":\"0\",\"type\":%s,\"bron\":\"0\"},"
                + "\"periode\":null,\"bron\":\"… groter is dan 0\"}").formatted(links, HEEL);
    }

    /** Jutter + Vondst, one relation, and the rules handed in. */
    private static String model(String... regels) {
        String ot = "\"Jutter\":{\"node\":\"Objecttype\",\"naam\":\"Jutter\",\"attributen\":{},"
                + "\"attribuutMeervoud\":{},\"attribuutDimensies\":{},\"attribuutGranulariteit\":{},"
                + "\"attribuutTijdlijnAantal\":{},\"attribuutTijdlijnNaam\":{},"
                + "\"kenmerken\":[\"druk\",\"gemerkt\"],"
                + "\"kenmerkSoorten\":{\"druk\":\"BIJVOEGLIJK\",\"gemerkt\":\"BIJVOEGLIJK\"},"
                + "\"kenmerkGranulariteit\":{},\"kenmerkTijdlijnAantal\":{},\"bezield\":true,"
                + "\"meervoud\":\"Jutters\"},"
                + "\"Vondst\":{\"node\":\"Objecttype\",\"naam\":\"Vondst\",\"attributen\":{},"
                + "\"attribuutMeervoud\":{},\"attribuutDimensies\":{},\"attribuutGranulariteit\":{},"
                + "\"attribuutTijdlijnAantal\":{},\"attribuutTijdlijnNaam\":{},"
                + "\"kenmerken\":[\"gemerkt\",\"kaal\"],"
                + "\"kenmerkSoorten\":{\"gemerkt\":\"BIJVOEGLIJK\",\"kaal\":\"BIJVOEGLIJK\"},"
                + "\"kenmerkGranulariteit\":{},\"kenmerkTijdlijnAantal\":{},\"bezield\":false,"
                + "\"meervoud\":\"Vondsten\"}";
        String ft = "\"vondstaanmelding\":{\"node\":\"Feittype\",\"naam\":\"vondstaanmelding\","
                + "\"wederkerig\":false,\"rollen\":[{\"node\":\"Rol\",\"naam\":\"aanmelder\","
                + "\"objecttypeNaam\":\"Jutter\",\"meervoud\":null,\"meervoudig\":false},"
                + "{\"node\":\"Rol\",\"naam\":\"vondst\",\"objecttypeNaam\":\"Vondst\","
                + "\"meervoud\":\"vondsten\",\"meervoudig\":true}]}";
        return ("{\"formaat\":\"regelspraak-ir-v0\",\"objecttypen\":{%s},\"domeinen\":{},\"parameters\":{},"
                + "\"eenheidstelsels\":{},\"feittypen\":{%s},\"dimensies\":{},\"tijdlijnen\":[],"
                + "\"dagsoorten\":[],\"dagsoortPerEenheid\":{},\"regels\":[],\"resolvedRegels\":[%s],"
                + "\"resolvedDagsoorten\":[],\"objectCreaties\":[],\"verdelingen\":[],\"consistenties\":[],"
                + "\"uniciteiten\":[],\"uniciteitUnies\":[],\"feitCreaties\":[],\"resolvedStartpunten\":[],"
                + "\"regelGroepen\":[],\"diagnostics\":[]}")
                .formatted(ot, ft, String.join(",", regels));
    }

    /** A consumer whose fold filter carries {@code filter} over the element type Vondst, above a plain
     *  producer of {@code gemerkt} on that same type. */
    private static String doc(String filter) {
        String bronRol = ",\"bronRol\":{\"node\":\"RolBinding\",\"feittype\":\"vondstaanmelding\","
                + "\"rol\":\"vondst\"}";
        return model(
                kenmerkRegel("consument", "druk", "Jutter",
                        groterDanNul(telling("\"Vondst\"", filter, bronRol))),
                kenmerkRegel("producent", "gemerkt", "Vondst",
                        "{\"node\":\"Kenmerk\",\"kenmerk\":\"kaal\",\"navigatie\":null,\"negatie\":false,"
                                + "\"eigenaar\":null,\"periode\":null,\"bron\":\"kaal zijn\"}"));
    }

    /** The subject-loss document: the consumer's fold carries NO element type, so its filter is read at the
     *  enclosing subject (Jutter), where nothing produces {@code gemerkt}; meanwhile Vondst carries a
     *  same-named characteristic produced one band deep. With the name-wide fallback the consumer sinks to
     *  −2 through a homonym on a type it never walks. */
    private static String homoniemDoc() {
        String nietGemerkt = "{\"node\":\"Kenmerk\",\"kenmerk\":\"gemerkt\",\"navigatie\":null,"
                + "\"negatie\":true,\"eigenaar\":null,\"periode\":null,\"bron\":\"niet gemerkt zijn\"}";
        String nietKaal = "{\"node\":\"Kenmerk\",\"kenmerk\":\"kaal\",\"navigatie\":null,\"negatie\":true,"
                + "\"eigenaar\":null,\"periode\":null,\"bron\":\"niet kaal zijn\"}";
        return model(
                kenmerkRegel("consument", "druk", "Jutter",
                        groterDanNul(telling("null", nietGemerkt, ""))),
                kenmerkRegel("ketenstap", "gemerkt", "Vondst", nietKaal),
                kenmerkRegel("basis", "kaal", "Vondst",
                        "{\"node\":\"Rollidmaatschap\",\"rol\":\"vondst\",\"anderTeFiller\":null,"
                                + "\"negatie\":false,\"bron\":\"een vondst zijn\"}"));
    }
}
