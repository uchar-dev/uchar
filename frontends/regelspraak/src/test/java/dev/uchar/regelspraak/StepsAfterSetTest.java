package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.ResolvedRule;
import dev.uchar.regelspraak.model.Datatype;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A fold over one set followed by to-one steps (grammar/DIVERGENCES.md, "A fold over a set and to-one steps after
 * it"): "de maximale waarde van het gewicht van de weging van de vondst van alle jutters van de Ploeg". The name of the
 * read runs over every inner 'van', so the fold read the member "gewicht van de weging van de vondst" of the element
 * type Jutter, which does not exist; until 2026-10-02 the fold stayed a marker with the WARNING that it is no declared
 * attribute of Jutter, and with a subselection the subselection was refused as dropped. The frontend now lowers the
 * steps to a generated member of the element type, computed by a generated rule from the to-one chain, and folds over
 * that member, the subselection staying on the element. The wire, the verifier and the executors are unchanged: the
 * lowering is the document an author writes by hand with such a member.
 */
class StepsAfterSetTest {

    private static final String MODEL = """
        Objecttype de Ploeg (mv: Ploegen)
            het zwaarste gewicht Numeriek (geheel getal);
            de eerste vinddag Datum in dagen;
            het vondstgewicht Numeriek (geheel getal);

        Objecttype de Jutter (mv: Jutters) (bezield)
            de startdag Datum in dagen;
            het eigen gewicht Numeriek (geheel getal);
            is actief kenmerk (bijvoeglijk);

        Objecttype de Vondst (mv: Vondsten)
            de vinddag Datum in dagen;

        Objecttype de Weging (mv: Wegingen)
            het gewicht Numeriek (geheel getal);

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

        Regel actief
            geldig altijd
                Een Jutter is actief
                indien zijn startdag gevuld is.

        """;

    private static final String ZWAARSTE = "Regel zwaarste gewicht\n    geldig altijd\n"
            + "        Het zwaarste gewicht van een Ploeg moet berekend worden als de maximale waarde van het gewicht "
            + "van de weging van de vondst van alle jutters van de Ploeg.\n";

    private static RegelSpraakModel clean(String source) {
        RegelSpraakModel m = Resolver.resolve(source);
        assertEquals(List.of(), m.diagnostics.stream().map(RegelSpraakModel.Diagnostic::toString).toList());
        return m;
    }

    private static ResolvedRule rule(RegelSpraakModel m, String name) {
        return m.resolvedRules.stream().filter(r -> r.name().equals(name)).findFirst()
                .orElseThrow(() -> new AssertionError("geen regel '" + name + "' in "
                        + m.resolvedRules.stream().map(ResolvedRule::name).toList()));
    }

    /** The fold of a rule's value and its one argument, a set read of {@code member} on Jutter. */
    private static Expr.FunctionCall foldOver(RegelSpraakModel m, String rule, Expr.FunctionKind kind, String member) {
        Expr.FunctionCall f = assertInstanceOf(Expr.FunctionCall.class, rule(m, rule).value());
        assertEquals(kind, f.kind());
        Expr.AttributeReference a = assertInstanceOf(Expr.AttributeReference.class, f.arguments().get(0));
        assertEquals(member, a.attribute());
        assertEquals("Jutter", a.objectType().name);
        assertTrue(a.set());
        return f;
    }

    /** The generated rule: the member on Jutter, set from the chain of to-one steps the sentence wrote. */
    private static void helperRule(RegelSpraakModel m, String member, String targetMember, String targetType, List<String> path) {
        ResolvedRule r = rule(m, member);
        Expr.AttributeReference target = assertInstanceOf(Expr.AttributeReference.class, r.target());
        assertEquals(member, target.attribute());
        assertEquals("Jutter", target.objectType().name);
        Expr.AttributeReference w = assertInstanceOf(Expr.AttributeReference.class, r.value());
        assertEquals(targetMember, w.attribute());
        assertEquals(targetType, w.objectType().name);
        assertEquals(path, w.navigationPath());
        assertNull(r.condition());
    }

    // ---- the three forms of the reproduction

    @Test void a_two_steps_after_the_set() {
        // before: WARNING "'gewicht van de weging van de vondst' is geen (meervoud van een) gedeclareerd attribuut van
        // Jutter — de aggregatie blijft onopgelost (§13.4.17) — …", the value a marker
        RegelSpraakModel m = clean(MODEL + ZWAARSTE);
        foldOver(m, "zwaarste gewicht", Expr.FunctionKind.MAXIMUM, "gewicht via vondst en weging");
        assertInstanceOf(Datatype.Numeric.class, m.objectTypes.get("Jutter").attribute("gewicht via vondst en weging"));
        helperRule(m, "gewicht via vondst en weging", "gewicht", "Weging", List.of("vondst", "weging"));
    }

    @Test void b_with_a_subselection_on_the_element() {
        // before: the WARNING above and ERROR "de 'die/dat'-deelselectie 'aan de volgende voorwaarde voldoen: hij is
        // actief' wordt niet omgezet en zou stil wegvallen: …"
        RegelSpraakModel m = clean(MODEL + ZWAARSTE.replace("van alle jutters van de Ploeg.",
                "van alle jutters van de Ploeg die aan de volgende voorwaarde voldoen: hij is actief."));
        Expr.FunctionCall f = foldOver(m, "zwaarste gewicht", Expr.FunctionKind.MAXIMUM, "gewicht via vondst en weging");
        assertNotNull(f.filter(), "de deelselectie staat op het element");
        assertTrue(IrEmitter.emit(m).contains("\"characteristic\": \"actief\""));
    }

    @Test void c_a_step_after_the_set() {
        // before: WARNING "'vinddag van de vondst' is geen (meervoud van een) gedeclareerd attribuut van Jutter — …"
        RegelSpraakModel m = clean(MODEL + "Regel eerste vinddag\n    geldig altijd\n        De eerste vinddag van een "
                + "Ploeg moet berekend worden als de eerste van de vinddag van de vondst van alle jutters van de Ploeg.\n");
        foldOver(m, "eerste vinddag", Expr.FunctionKind.EERSTE, "vinddag via vondst");
        assertInstanceOf(Datatype.DateInDays.class, m.objectTypes.get("Jutter").attribute("vinddag via vondst"));
        helperRule(m, "vinddag via vondst", "vinddag", "Vondst", List.of("vondst"));
    }

    // ---- the lowering is the document written by hand

    @Test void the_generated_member_is_the_hand_written_form() {
        // the bridge an author writes today: a member on the element set from the chain, and the fold over it. The
        // generated name holds 'en', which no declared name can, so the hand-written member is named otherwise and the
        // generated name is read as that name in the comparison
        String hand = MODEL.replace("    het eigen gewicht Numeriek (geheel getal);\n",
                "    het eigen gewicht Numeriek (geheel getal);\n    het bereikte gewicht Numeriek (geheel getal);\n")
                + "Regel bereikte gewicht\n    geldig altijd\n        Het bereikte gewicht van een Jutter moet gesteld "
                + "worden op het gewicht van de weging van de vondst van de Jutter.\n\n"
                + "Regel zwaarste gewicht\n    geldig altijd\n        Het zwaarste gewicht van een Ploeg moet berekend "
                + "worden als de maximale waarde van het bereikte gewicht van alle jutters van de Ploeg.\n";
        String lowered = IrEmitter.emit(clean(MODEL + ZWAARSTE)).replace("gewicht via vondst en weging", "bereikte gewicht");
        assertEquals(withoutSource(IrEmitter.emit(clean(hand))), withoutSource(lowered));
    }

    /** The emitted document without its source anchors and its origins, and with the rules in name order: what the
     *  lowering and the hand-written document must agree on is the model and every rule, not where the text stood or
     *  that the frontend made the member (IR-SPEC ch. 2.13). */
    private static String withoutSource(String json) {
        Object d = Report.json(json);
        return String.valueOf(normalise(d));
    }

    @SuppressWarnings("unchecked")
    private static Object normalise(Object x) {
        if (x instanceof java.util.Map<?, ?> map) {
            java.util.Map<String, Object> result = new java.util.TreeMap<>();
            for (var e : ((java.util.Map<String, Object>) map).entrySet())
                if (!e.getKey().equals("source") && !e.getKey().equals("statuteAnnotations")
                        && !e.getKey().equals("origin"))
                    result.put(e.getKey(), normalise(e.getValue()));
            return result;
        }
        if (x instanceof List<?> l) {
            List<Object> result = new java.util.ArrayList<>();
            for (Object o : l) result.add(normalise(o));
            if (!result.isEmpty() && result.stream().allMatch(o -> o instanceof java.util.Map<?, ?> mm && mm.get("name") != null))
                result.sort(java.util.Comparator.comparing(o -> String.valueOf(((java.util.Map<?, ?>) o).get("name"))));
            return result;
        }
        return x;
    }

    @Test void two_folds_over_the_same_steps_share_a_member() {
        RegelSpraakModel m = clean(MODEL + ZWAARSTE + "\nRegel vondstgewicht\n    geldig altijd\n        Het "
                + "vondstgewicht van een Ploeg moet berekend worden als de som van het gewicht van de weging van de "
                + "vondst van alle jutters van de Ploeg.\n");
        foldOver(m, "vondstgewicht", Expr.FunctionKind.SOM, "gewicht via vondst en weging");
        assertEquals(1, m.resolvedRules.stream().filter(r -> r.name().equals("gewicht via vondst en weging")).count());
    }

    @Test void a_declared_member_with_the_generated_name_refuses() {
        // a name of one step holds no 'en' and can be declared; the generated member does not shadow it
        RegelSpraakModel m = Resolver.resolve(MODEL.replace("    het eigen gewicht Numeriek (geheel getal);\n",
                "    het eigen gewicht Numeriek (geheel getal);\n    de vinddag via vondst Datum in dagen;\n")
                + "Regel eerste vinddag\n    geldig altijd\n        De eerste vinddag van een Ploeg moet berekend worden "
                + "als de eerste van de vinddag van de vondst van alle jutters van de Ploeg.\n");
        assertEquals(List.of("vouw 'de eerste van de vinddag van de vondst van alle jutters van de Ploeg': de stappen na "
                + "de verzameling worden het gegenereerde lid 'vinddag via vondst' van Jutter, en Jutter declareert die "
                + "naam al — een gegenereerd lid bestaat nog niet in de erflijn van het element"),
                m.errors().stream().map(RegelSpraakModel.Diagnostic::message).toList());
    }

    @Test void a_fold_over_a_subtype_and_one_over_its_supertype_share_the_member_in_every_order() {
        // the member stands on the filler of the first step's other role (Jutter), so the fold over the strandjutters
        // reads it inherited, whether it comes before or after the fold over the jutters
        String model = MODEL.replace("Objecttype de Vondst (mv: Vondsten)",
                "Objecttype de Strandjutter is een Jutter (mv: Strandjutters)\n    de vergunning Datum in dagen;\n\n"
                + "Objecttype de Vondst (mv: Vondsten)")
                + "Feittype PloegHeeftStrandjutter\n    de strandploeg Ploeg\n    de strandjutter (mv: strandjutters) "
                + "Strandjutter\nEén strandploeg heeft meerdere strandjutters\n\n";
        String strand = "Regel strand\n    geldig altijd\n        Het zwaarste gewicht van een Ploeg moet berekend worden "
                + "als de maximale waarde van het gewicht van de weging van de vondst van alle strandjutters van de Ploeg.\n\n";
        String all = "Regel alle\n    geldig altijd\n        Het vondstgewicht van een Ploeg moet berekend worden als de "
                + "som van het gewicht van de weging van de vondst van alle jutters van de Ploeg.\n\n";
        for (String source : List.of(model + strand + all, model + all + strand)) {
            RegelSpraakModel m = clean(source);
            assertNotNull(m.objectTypes.get("Jutter").attributes.get("gewicht via vondst en weging"));
            assertNull(m.objectTypes.get("Strandjutter").attributes.get("gewicht via vondst en weging"));
            assertEquals(1, m.resolvedRules.stream().filter(r -> r.name().equals("gewicht via vondst en weging")).count());
            Expr.AttributeReference a = assertInstanceOf(Expr.AttributeReference.class,
                    assertInstanceOf(Expr.FunctionCall.class, rule(m, "strand").value()).arguments().get(0));
            assertEquals("gewicht via vondst en weging", a.attribute());
            assertEquals("Strandjutter", a.objectType().name);
        }
    }

    // ---- what stays as it was

    @Test void a_read_of_the_same_chain_outside_the_fold_stays_a_chain() {
        RegelSpraakModel m = clean(MODEL + ZWAARSTE + "\nRegel eigen gewicht\n    geldig altijd\n        Het eigen "
                + "gewicht van een Jutter moet gesteld worden op het gewicht van de weging van de vondst van de Jutter.\n");
        Expr.AttributeReference w = assertInstanceOf(Expr.AttributeReference.class, rule(m, "eigen gewicht").value());
        assertEquals("gewicht", w.attribute());
        assertEquals(List.of("vondst", "weging"), w.navigationPath());
    }

    @Test void e_steps_before_the_set_stay_a_source_path() {
        RegelSpraakModel m = clean(MODEL.replace("    het eigen gewicht Numeriek (geheel getal);\n",
                "    het eigen gewicht Numeriek (geheel getal);\n    de eerste startdag in de ploeg Datum in dagen;\n")
                + "Regel eerste startdag in de ploeg\n    geldig altijd\n        De eerste startdag in de ploeg van een "
                + "Jutter moet berekend worden als de eerste van de startdag van alle jutters van de ploeg van de Jutter.\n");
        Expr.AttributeReference a = assertInstanceOf(Expr.AttributeReference.class,
                assertInstanceOf(Expr.FunctionCall.class, rule(m, "eerste startdag in de ploeg").value()).arguments().get(0));
        assertEquals("startdag", a.attribute());
        assertEquals(2, a.rangePath().size());
        assertEquals(List.of("startdag", "eigen gewicht", "eerste startdag in de ploeg"),
                List.copyOf(m.objectTypes.get("Jutter").attributes.keySet()));
    }

    @Test void the_member_on_the_element_with_the_hand_stays() {
        RegelSpraakModel m = clean(MODEL + "Regel eigen gewicht\n    geldig altijd\n        Het eigen gewicht van een "
                + "Jutter moet gesteld worden op het gewicht van de weging van de vondst van de Jutter.\n\n"
                + "Regel zwaarste gewicht\n    geldig altijd\n        Het zwaarste gewicht van een Ploeg moet berekend "
                + "worden als de maximale waarde van het eigen gewicht van alle jutters van de Ploeg.\n");
        foldOver(m, "zwaarste gewicht", Expr.FunctionKind.MAXIMUM, "eigen gewicht");
        assertEquals(List.of("startdag", "eigen gewicht"), List.copyOf(m.objectTypes.get("Jutter").attributes.keySet()));
    }
}
