package io.github.yhavinga.regelspraak.transpiler;

import io.github.yhavinga.regelspraak.cases.IrPad;

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
     *  notices is wrong. DE is here too: it is the one compound quantifier the fold node refuses
     *  (WF-108 (a)) but {@code Samengesteld} admits, so leaving it out would leave an arm unpinned. */
    @Test void een_kenmerk_onder_ALLE_blijft_positief() throws Exception {
        assertEquals(0, salience(doc(filter("ALLE")), "consument"),
                "ALLE geeft de polariteit door; een positieve lezing blijft positief");
        assertEquals(0, salience(doc(filter("TEN_MINSTE")), "consument"), "TEN_MINSTE geeft door");
        assertEquals(0, salience(doc(filter("DE")), "consument"), "DE geeft door");
    }

    /** Two properties of the CONTEXT itself, both unpinned until now.
     *
     *  <p><b>Nesting composes.</b> A conjunctive compound inside a negation-like one keeps the
     *  negation: the polarity is a property of the PATH, not of the innermost node.
     *
     *  <p><b>Double negation does NOT cancel.</b> {@code neg || k.negatie()} is deliberate — ch. 6.1's
     *  "once negative, conservatively negative" — so an already-negated {@code Kenmerk} inside a GEEN
     *  stays negative instead of flipping back to positive. A boolean XOR here would read "niet
     *  gemerkt onder GEEN" as a positive read and let the consumer fire in the transient window. */
    @Test void de_context_stapelt_en_dubbele_negatie_klapt_niet_terug() throws Exception {
        assertEquals(-1, salience(doc(genest("GEEN", "ALLE", false)), "consument"),
                "een ALLE binnen een GEEN houdt de negatie: de polariteit hangt aan het PAD");
        assertEquals(0, salience(doc(genest("ALLE", "TEN_MINSTE", false)), "consument"),
                "twee doorgeefkwantoren laten een positieve lezing positief");
        assertEquals(-1, salience(doc(genest("GEEN", "ALLE", true)), "consument"),
                "dubbele negatie klapt NIET terug (ch. 6.1: eenmaal negatief, behoudend negatief)");
        assertEquals(-1, salience(doc(kenmerkCheck(true)), "consument"),
                "de controle: een kaal genegeerd kenmerk is gewoon negatief");
    }

    /** <b>The context does not cross an EXPRESSION boundary.</b> The verifier's walk over expressions
     *  ({@code dep_expr}) carries no negation context at all, and the three CONDITIONS it reaches
     *  through one — a fold's filter, a union selection's filter, an if-then-else's guard — each
     *  restart at {@code ~neg:false}. So "aan GEEN van de volgende voorwaarden voldoen: het aantal X
     *  die gemerkt zijn is groter dan 0" reads {@code gemerkt} POSITIVELY: the compound negates the
     *  COUNT, not the membership test inside it. Carrying the context across that boundary is the
     *  mirror image of the gap fixed on 2026-09-22 — an extra band, and a false negative CYCLE
     *  through the cycle-local fallback. */
    @Test void de_negatiecontext_steekt_geen_expressiegrens_over() throws Exception {
        assertEquals(0, salience(genesteldDoc("GEEN"), "consument"),
                "de GEEN negeert de TELLING, niet de lidmaatschapstest in haar filter");
        assertEquals(0, salience(genesteldDoc("TEN_HOOGSTE"), "consument"), "idem voor TEN_HOOGSTE");
        assertEquals(0, salience(genesteldDoc("PRECIES"), "consument"), "idem voor PRECIES");
    }

    /** <b>The quantifier fold's matrix is read conservatively NEGATIVELY</b> — RUNTIME-SEMANTICS
     *  ch. 6.1 and the verifier's {@code dep_cond}, which passes {@code ~neg:true} to BOTH the filter
     *  and the matrix unconditionally: the quantifier is a fold with conjunction, so a member that
     *  FAILS decides the verdict the way a negated read does, and GEEN is the negation outright.
     *  Parked on 2026-09-22 on a channel-shift worry that a measurement then refuted, so it is
     *  mirrored here and pinned rather than left as a silent difference from the norm. */
    @Test void de_matrix_van_een_kwantorvouw_leest_behoudend_negatief() throws Exception {
        assertEquals(-1, salience(kwantorvouwDoc("ALLE", true), "consument"),
                "een POSITIEF kenmerk onder de matrix is toch een negatieve rand (ch. 6.1)");
        assertEquals(-1, salience(kwantorvouwDoc("TEN_MINSTE", true), "consument"),
                "de lezing hangt aan de VOUW, niet aan de kwantor erboven");
        assertEquals(-1, salience(kwantorvouwDoc("ALLE", false), "consument"),
                "en in het FILTER van de vouw geldt hetzelfde");
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
        IrLoader.Loaded geladen = IrLoader.loadAll(JSON.readTree(ir));
        List<ResolvedRegel> regels = geladen.model().resolvedRegels;
        Stratificatie s = Stratificatie.bereken(regels, k -> geladen.navigatieBindingen().containsKey(k));
        return s.salience(regels.stream().filter(r -> regel.equals(r.naam())).findFirst().orElseThrow());
    }

    private static final String HEEL =
            "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"nonNegative\"}";
    private static final String BOOL = "{\"node\":\"Boolean\"}";

    private static String kenmerkCheck(boolean negatie) {
        return ("{\"node\":\"CharacteristicCheck\",\"characteristic\":\"gemerkt\",\"negated\":%s,"
                + "\"source\":{\"snippet\":\"gemerkt zijn\"}}").formatted(negatie);
    }

    private static String samengesteld(String kwantor, String... delen) {
        return ("{\"node\":\"Compound\",\"quantifier\":\"%s\",\"threshold\":1,\"parts\":[%s],"
                + "\"source\":{\"snippet\":\"de samengestelde deelselectie\"}}")
                .formatted(kwantor, String.join(",", delen));
    }

    /** A positive {@code gemerkt} check wrapped in a compound under {@code kwantor}. */
    private static String filter(String kwantor) {
        return samengesteld(kwantor, kenmerkCheck(false));
    }

    /** {@code buiten} wrapping {@code binnen} wrapping one {@code gemerkt} check. */
    private static String genest(String buiten, String binnen, boolean negatie) {
        return samengesteld(buiten, samengesteld(binnen, kenmerkCheck(negatie)));
    }

    /** A count over the Jutter's vondsten; {@code elementType} "null" writes none, and {@code bereik} is the written
     *  range field with its leading comma, or empty. */
    private static String telling(String elementType, String filter, String bereik) {
        return ("{\"node\":\"Aggregation\",\"kind\":\"count\",\"arguments\":[{\"node\":\"AttributeReference\","
                + "\"attribute\":\"vondsten\",\"subjectType\":\"Jutter\",\"source\":{\"snippet\":\"vondsten van de Jutter\"}}]"
                + "%s,\"filter\":%s%s,\"type\":%s,\"source\":{\"snippet\":\"het aantal …\"}}")
                .formatted(elementType.equals("null") ? "" : ",\"elementType\":" + elementType, filter, bereik, HEEL);
    }

    private static String kenmerkRegel(String naam, String kenmerk, String ot, String voorwaarde) {
        return IrPad.regel(naam, "KENMERKTOEKENNING",
                "{\"node\":\"AttributeReference\",\"attribute\":\"%s\",\"subjectType\":\"%s\",\"source\":{\"snippet\":\"Een %s is %s\"}}"
                        .formatted(kenmerk, ot, ot, kenmerk),
                IrPad.literal("waar", BOOL, "waar"), voorwaarde);
    }

    private static String groterDanNul(String links) {
        return ("{\"node\":\"Comparison\",\"op\":\"greater\",\"left\":%s,"
                + "\"right\":{\"node\":\"Literal\",\"value\":{\"number\":\"0\"},\"type\":%s,\"source\":{\"snippet\":\"0\"}},"
                + "\"source\":{\"snippet\":\"… groter is dan 0\"}}").formatted(links, HEEL);
    }

    /** Jutter + Vondst, one relation, and the rules handed in. */
    private static String model(String... regels) {
        String ot = "{\"node\":\"ObjectType\",\"name\":\"Jutter\",\"animate\":true,\"plural\":\"Jutters\","
                + "\"attributes\":[],"
                + "\"characteristics\":[{\"node\":\"Characteristic\",\"name\":\"druk\",\"kind\":\"adjectival\"},"
                + "{\"node\":\"Characteristic\",\"name\":\"gemerkt\",\"kind\":\"adjectival\"}]},"
                + "{\"node\":\"ObjectType\",\"name\":\"Vondst\",\"animate\":false,\"plural\":\"Vondsten\","
                + "\"attributes\":[],"
                + "\"characteristics\":[{\"node\":\"Characteristic\",\"name\":\"gemerkt\",\"kind\":\"adjectival\"},"
                + "{\"node\":\"Characteristic\",\"name\":\"kaal\",\"kind\":\"adjectival\"}]}";
        String ft = "{\"node\":\"FactType\",\"name\":\"vondstaanmelding\","
                + "\"reciprocal\":false,\"roles\":[{\"node\":\"Role\",\"name\":\"aanmelder\","
                + "\"objectType\":\"Jutter\",\"many\":false},"
                + "{\"node\":\"Role\",\"name\":\"vondst\",\"objectType\":\"Vondst\","
                + "\"plural\":\"vondsten\",\"many\":true}]}";
        return ("{\"format\":\"uchar-ir-v1\",\"model\":{\"objectTypes\":[%s],\"factTypes\":[%s],"
                + "\"parameters\":[],\"domains\":[],\"unitSystems\":[],\"dimensions\":[],\"timelines\":[],"
                + "\"dayKinds\":[]},\"rules\":{\"rules\":[%s],"
                + "\"dayKindDefinitions\":[],\"objectCreations\":[],\"allocations\":[],\"consistencies\":[],"
                + "\"uniquenesses\":[],\"factCreations\":[],\"startPointDeterminations\":[],"
                + "\"ruleGroups\":[]},\"diagnostics\":[]}")
                .formatted(ot, ft, String.join(",", regels));
    }

    /** A consumer whose fold filter carries {@code filter} over the element type Vondst, above a plain
     *  producer of {@code gemerkt} on that same type. */
    private static String doc(String filter) {
        String bronRol = ",\"sourceRole\":{\"node\":\"RoleBinding\",\"factType\":\"vondstaanmelding\","
                + "\"role\":\"vondst\"}";
        return model(
                kenmerkRegel("consument", "druk", "Jutter",
                        groterDanNul(telling("\"Vondst\"", filter, bronRol))),
                kenmerkRegel("producent", "gemerkt", "Vondst",
                        "{\"node\":\"CharacteristicCheck\",\"characteristic\":\"kaal\",\"negated\":false,"
                                + "\"source\":{\"snippet\":\"kaal zijn\"}}"));
    }

    /** A consumer whose voorwaarde is {@code kwantor}(⟨the COUNT whose filter checks gemerkt⟩ > 0) —
     *  so the compound's negation, if it were carried, would have to cross the fold-filter boundary to
     *  reach the characteristic. */
    private static String genesteldDoc(String kwantor) {
        String bronRol = ",\"sourceRole\":{\"node\":\"RoleBinding\",\"factType\":\"vondstaanmelding\","
                + "\"role\":\"vondst\"}";
        String binnen = groterDanNul(telling("\"Vondst\"", kenmerkCheck(false), bronRol));
        return model(
                kenmerkRegel("consument", "druk", "Jutter", samengesteld(kwantor, binnen)),
                kenmerkRegel("producent", "gemerkt", "Vondst",
                        "{\"node\":\"CharacteristicCheck\",\"characteristic\":\"kaal\",\"negated\":false,"
                                + "\"source\":{\"snippet\":\"kaal zijn\"}}"));
    }

    /** A consumer whose voorwaarde IS a quantifier fold over Vondst, carrying one POSITIVE gemerkt
     *  check in its matrix ({@code inToets}) or in its filter. */
    private static String kwantorvouwDoc(String kwantor, boolean inToets) {
        String check = kenmerkCheck(false);
        String altijd = "{\"node\":\"RoleMembership\",\"factType\":\"vondstaanmelding\",\"role\":\"vondst\","
                + "\"negated\":false,\"source\":{\"snippet\":\"een vondst zijn\"}}";
        String drempel = switch (kwantor) {
            case "TEN_MINSTE", "TEN_HOOGSTE", "PRECIES" -> "\"drempel\":1,";
            default -> "";
        };
        String vouw = ("{\"node\":\"QuantifierFold\",\"quantifier\":\"%s\",%s\"elementType\":\"Vondst\","
                + "\"filter\":%s,\"test\":%s,\"source\":{\"snippet\":\"alle vondsten … \"},"
                + "\"sourceRole\":{\"node\":\"RoleBinding\",\"factType\":\"vondstaanmelding\","
                + "\"role\":\"vondst\"}}")
                .formatted(kwantor, drempel, inToets ? "null" : check, inToets ? check : altijd);
        return model(
                kenmerkRegel("consument", "druk", "Jutter", vouw),
                kenmerkRegel("producent", "gemerkt", "Vondst",
                        "{\"node\":\"CharacteristicCheck\",\"characteristic\":\"kaal\",\"negated\":false,"
                                + "\"source\":{\"snippet\":\"kaal zijn\"}}"));
    }

    /** The subject-loss document: the consumer's fold carries NO element type, so its filter is read at the
     *  enclosing subject (Jutter), where nothing produces {@code gemerkt}; meanwhile Vondst carries a
     *  same-named characteristic produced one band deep. With the name-wide fallback the consumer sinks to
     *  −2 through a homonym on a type it never walks. */
    private static String homoniemDoc() {
        String nietGemerkt = "{\"node\":\"CharacteristicCheck\",\"characteristic\":\"gemerkt\","
                + "\"negated\":true,\"source\":{\"snippet\":\"niet gemerkt zijn\"}}";
        String nietKaal = "{\"node\":\"CharacteristicCheck\",\"characteristic\":\"kaal\",\"negated\":true,"
                + "\"source\":{\"snippet\":\"niet kaal zijn\"}}";
        return model(
                kenmerkRegel("consument", "druk", "Jutter",
                        groterDanNul(telling("null", nietGemerkt, ""))),
                kenmerkRegel("ketenstap", "gemerkt", "Vondst", nietKaal),
                kenmerkRegel("basis", "kaal", "Vondst",
                        "{\"node\":\"RoleMembership\",\"factType\":\"vondstaanmelding\",\"role\":\"vondst\","
                                + "\"negated\":false,\"source\":{\"snippet\":\"een vondst zijn\"}}"));
    }
}
