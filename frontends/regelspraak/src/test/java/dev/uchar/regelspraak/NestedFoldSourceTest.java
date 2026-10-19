package dev.uchar.regelspraak;

import dev.uchar.regelspraak.emit.IrEmitter;
import dev.uchar.regelspraak.model.RegelSpraakModel;
import dev.uchar.regelspraak.model.RegelSpraakModel.Diagnostic;
import dev.uchar.regelspraak.model.RegelSpraakModel.Severity;
import dev.uchar.regelspraak.resolver.Resolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A MEMBER fold ("de minimale waarde van &lt;attr&gt; van alle &lt;elem&gt; van &lt;onderwerp&gt;") describes its
 * source on the uchar wire with exactly ONE relation: one {@code elementType} plus one {@code sourceRole}
 * (IR-SPEC ch. 4.3). Three written shapes used to be mapped onto that single relation in silence, and all
 * three produced a fold over a DIFFERENT collection than the sentence names:
 *
 * <ol>
 * <li>a NESTED set step — "van alle geborgen vondsten van alle gevoerde boten van de Jutter" — whose outer
 *     step simply disappeared, the fold ranging from the rule subject straight to the innermost element type;
 * <li>an intermediate TO-ONE step — "van alle vondsten van de boot van de Jutter" — losing its step exactly
 *     the same way, and invisible to (3) because the written element name may name the subject-side role
 *     perfectly well;
 * <li>a by-PAIR binding the written name contradicts — {@code connectingRoleBinding} searches on TYPES only,
 *     so with exactly one fact type joining subject and element it wins whatever role the sentence named.
 * </ol>
 *
 * <p>Measured before this suite, on the two documents the card pins. Without a direct Jutter–Vondst fact type
 * the misbinding was loud downstream (WF-048, "no fact type relates 'Jutter' to 'Vondst'") but named the wrong
 * cause; WITH one it was verifier-PASS and ref, the ASP backend and Drools all answered 5.00 where the sentence
 * says 3.00 — the differential square agreeing over the wrong set, which no gate can see. Dropping the binding
 * at the emitter does not help: a bindingless fold over a uniquely related pair is verifier-clean and every
 * executor re-derives the same wrong relation. So the RESOLUTION declines, and the fold keeps the visible
 * {@code Unresolved} marker this resolver already uses for a source it cannot bind — carrying the
 * aggregation's OWN result type, so the refused document stays WELL-FORMED and faults observably under
 * WF-044 in every executor (IR-SPEC ch. 2.4: the marker is legal wire content, the fence is behavioural).
 *
 * <p><b>Updated 2026-09-11, the wire half (WF-102).</b> The range is no longer one relation: a fold source
 * may carry a {@code sourcePath} of two or more steps, and the flat-union reading over it is normative
 * (IR-SPEC ch. 4.3, RUNTIME-SEMANTICS ch. 3.4b). Shapes (1) and (2) above are therefore EXPRESSIBLE where
 * the written chain RESOLVES, and this suite pins that instead of the refusal on exactly those documents —
 * the card's A, B and F. The refusals are not gone: they hold for every chain that declines (an
 * unresolvable step, an anchor that is not the subject, a walk that lands elsewhere), and each route keeps
 * a witness. Shape (3) is untouched — it is a one-step form with no range to state.
 *
 * <p><b>Updated 2026-09-27, the words of that refusal.</b> Until then a declining nested chain was refused with
 * the pre-WF-102 ground ("een vouwbron draagt precies één relatie … de buitenste stap zou spoorloos
 * wegvallen"), whatever made it decline, and a declining chain with a to-one step got the anchor sentences of
 * route (b), the emitter's ERROR, or nothing at all. Now every chain of three segments or more that the walk
 * declines, and every nested set step, gets one WARNING that names where the walk stopped: the step that binds
 * no role and why, an anchor that is not the subject, or a landing on another type. The witnesses below hold
 * each cause to its literal sentence. The walk binds no plural the model does not declare (IR-SPEC ch. 2.6,
 * decided 2026-08-25), so the way out that sentence names is the declaration.
 */
class NestedFoldSourceTest {

    /** The card's chain: Jutter -(bemanning, to-many)-&gt; Boot -(berging, to-many)-&gt; Vondst.gewicht, plus the
     *  DIRECT vondstaanmelding that makes the misbinding silent — the difference between the card's document A
     *  (this model without the third feittype) and document B (this model). */
    static final String MODEL = """
        Eenheidsysteem Gewicht
            het kilogram (mv: kilogrammen) kg

        Objecttype de Jutter (mv: Jutters) (bezield)
            het laagste vondstgewicht Numeriek (getal met 2 decimalen) met eenheid kg;
            het vondstaantal Numeriek (niet-negatief geheel getal);

        Objecttype de Boot (mv: Boten)

        Objecttype de Vondst (mv: Vondsten)
            het gewicht (mv: gewichten) Numeriek (getal met 2 decimalen) met eenheid kg;

        Feittype bemanning
            de schipper Jutter
            de gevoerde boot (mv: gevoerde boten) Boot
        Eén schipper heeft meerdere gevoerde boten

        Feittype berging
            de bergende boot Boot
            de geborgen vondst (mv: geborgen vondsten) Vondst
        Eén bergende boot heeft meerdere geborgen vondsten
        """;

    /** The card's document A: no fact type relates Jutter to Vondst. */
    static final String MODEL_A = MODEL;

    /** The card's document B: the same chain plus a DIRECT Jutter–Vondst fact type, which is what turned the
     *  misbinding from loud-downstream into verifier-clean-and-silently-wrong. */
    static final String MODEL_B = MODEL + """

        Feittype vondstaanmelding
            de aanmelder Jutter
            de aangemelde vondst (mv: aangemelde vondsten) Vondst
        Eén aanmelder heeft meerdere aangemelde vondsten
        """;

    /** The card's sentence: a fold whose set source nests a second set step. Hand expectation FROM THE
     *  SENTENCE is 3.00 (the minimum over both boats' vondsten); the silent one-step reading answered 5.00. */
    static final String CHAIN_RULE = """

        Regel laagste vondstgewicht
            geldig altijd
                Het laagste vondstgewicht van een Jutter moet berekend worden als de minimale waarde van het gewicht van alle geborgen vondsten van alle gevoerde boten van de Jutter.
        """;

    /** The same chain with a to-ONE step in the middle — the nested set step is not the first one. */
    static final String MIXED_CHAIN_RULE = """

        Regel laagste vondstgewicht
            geldig altijd
                Het laagste vondstgewicht van een Jutter moet berekend worden als de minimale waarde van het gewicht van alle geborgen vondsten van de bergende boot van alle gevoerde boten van de Jutter.
        """;

    /** ONE set step, no nesting: the written element name names a role of *berging*, but the only fact type
     *  relating Jutter to Vondst is *vondstaanmelding*. The by-pair search bound its role regardless. */
    static final String COLLIDING_ONE_STEP_RULE = """

        Regel laagste vondstgewicht
            geldig altijd
                Het laagste vondstgewicht van een Jutter moet berekend worden als de minimale waarde van het gewicht van alle geborgen vondsten van de Jutter.
        """;

    /** Counter-proof: the same one-step fold written with the name the fact type declares. */
    static final String CORRECT_ONE_STEP_RULE = """

        Regel laagste vondstgewicht
            geldig altijd
                Het laagste vondstgewicht van een Jutter moet berekend worden als de minimale waarde van het gewicht van alle aangemelde vondsten van de Jutter.
        """;

    /** Counter-proof: a written name that IS the element object type's own name claims no role at all, so the
     *  by-pair derivation stays the honest fallback. */
    static final String OBJECT_TYPE_NAME_RULE = """

        Regel laagste vondstgewicht
            geldig altijd
                Het laagste vondstgewicht van een Jutter moet berekend worden als de minimale waarde van het gewicht van alle Vondst van de Jutter.
        """;

    /** The to-ONE flank: the chain reaches its elements through a Boot, and the written element name names
     *  the role of the DIRECT Jutter–Vondst fact type perfectly well — so only the anchor gives it away. */
    static final String FLANKMODEL = """
        Eenheidsysteem Gewicht
            het kilogram (mv: kilogrammen) kg

        Objecttype de Jutter (mv: Jutters) (bezield)
            het laagste vondstgewicht Numeriek (getal met 2 decimalen) met eenheid kg;

        Objecttype de Boot (mv: Boten)

        Objecttype de Vondst (mv: Vondsten)
            het gewicht (mv: gewichten) Numeriek (getal met 2 decimalen) met eenheid kg;

        Feittype bemanning
            de schipper Jutter
            de boot Boot
        Eén schipper heeft één boot

        Feittype bootvangst
            de vangende boot Boot
            de vondst (mv: vondsten) Vondst
        Eén vangende boot heeft meerdere vondsten

        Feittype jutterbezit
            de bezitter Jutter
            de vondst (mv: vondsten) Vondst
        Eén bezitter heeft meerdere vondsten
        """;

    static final String PATH_STEP_RULE = """

        Regel laagste vondstgewicht
            geldig altijd
                Het laagste vondstgewicht van een Jutter moet berekend worden als de minimale waarde van het gewicht van alle vondsten van de boot van de Jutter.
        """;

    /** Counter-proof on the SAME model: without the intermediate step the fold binds and emits. */
    static final String DIRECT_RULE = """

        Regel laagste vondstgewicht
            geldig altijd
                Het laagste vondstgewicht van een Jutter moet berekend worden als de minimale waarde van het gewicht van alle vondsten van de Jutter.
        """;

    /** Counter-proof: an anchor that names a ROLE resolves to no object type, so the anchor rule leaves it
     *  alone — that is the mother card's literal-anchor question and belongs to the wire, not here. */
    static final String ROLE_ANCHOR_RULE = """

        Regel laagste vondstgewicht
            geldig altijd
                Het laagste vondstgewicht van een Jutter moet berekend worden als de minimale waarde van het gewicht van alle aangemelde vondsten van de aanmelder.
        """;

    /** Counter-proof: the §13.4.17.47 telling, whose element word sits in the ATTRIBUTE position and binds by
     *  the written name already (the Onbekend-typed wart, a different derivation). */
    static final String COUNT_RULE = """

        Regel vondstaantal
            geldig altijd
                Het vondstaantal van een Jutter moet berekend worden als het aantal aangemelde vondsten van de Jutter.
        """;

    /** Counter-proof: the same nested chain with NO fold around it — the set fence's position, which is
     *  another card's decision and must keep its own diagnosis. */
    static final String SET_READ_WITHOUT_FOLD = """

        Regel laagste vondstgewicht
            geldig altijd
                Het laagste vondstgewicht van een Jutter moet berekend worden als het gewicht van alle geborgen vondsten van alle gevoerde boten van de Jutter.
        """;

    /** A chain whose anchor names an object type that is NOT the rule subject — one step, so no path can
     *  carry it, and the anchor rule refuses it exactly as before. Route (b)'s surviving witness. */
    static final String FOREIGN_ANCHOR_RULE = """

        Regel laagste vondstgewicht
            geldig altijd
                Het laagste vondstgewicht van een Jutter moet berekend worden als de minimale waarde van het gewicht van alle geborgen vondsten van de Boot.
        """;

    /** MODEL_B plus a Jutter-to-Jutter relation, so a role anchor can be a FAR role: "de strandmaat" is a
     *  role REACHABLE FROM the subject, hence another instance, which ch. 4.1a resolves as a hop. */
    static final String ROLE_ANCHOR_MODEL = MODEL_B + """

        Feittype vriendschap
            de jutter Jutter
            de strandmaat (mv: strandmaten) Jutter
        Eén jutter heeft meerdere strandmaten
        """;

    static final String FAR_ROLE_ANCHOR_RULE = """

        Regel laagste vondstgewicht
            geldig altijd
                Het laagste vondstgewicht van een Jutter moet berekend worden als de minimale waarde van het gewicht van alle aangemelde vondsten van de strandmaat.
        """;

    /** MODEL_B plus one more to-many relation, for the DEPTH witness: three written steps. */
    static final String DEPTH_MODEL = MODEL_B + """

        Objecttype de Kist (mv: Kisten)
            het inhoudsgewicht Numeriek (getal met 2 decimalen) met eenheid kg;

        Feittype inpakking
            de verpakte vondst Vondst
            de ingepakte kist (mv: ingepakte kisten) Kist
        Eén verpakte vondst heeft meerdere ingepakte kisten
        """;

    static final String THREE_STEP_RULE = """

        Regel laagste vondstgewicht
            geldig altijd
                Het laagste vondstgewicht van een Jutter moet berekend worden als de minimale waarde van het inhoudsgewicht van alle ingepakte kisten van alle geborgen vondsten van alle gevoerde boten van de Jutter.
        """;

    /** The refusal of a declining chain, whole: a nested aggregation whose step is spelled in a plural the model
     *  does not declare. */
    static final String DERIVED_PLURAL_MESSAGE =
            "keten zonder sourcePath in een vouwbron: in 'alle geborgen vondsten van alle sloepen van de Jutter' "
            + "bindt de stap 'sloepen' vanaf Jutter geen rol: 'sloepen' is geen gedeclareerde naam of gedeclareerd "
            + "meervoud, alleen het afgeleide meervoud (+s/+en) van de rol 'sloep' van het feittype 'bemanning', en "
            + "een afgeleid meervoud bindt geen stap — een sourcePath loopt van het regelonderwerp naar de elementen "
            + "en bindt elke stap aan precies één rol (IR-SPEC ch. 4.3/WF-102), dus de aggregatie blijft onopgelost "
            + "(§13.4.17); declareer het meervoud op de rol: 'sloep (mv: sloepen)' — 'de minimale waarde van het "
            + "gewicht van alle geborgen vondsten van alle sloepen van de Jutter'";

    /** The head and the tail of that refusal, for the witnesses of the other causes. */
    static final String CHAIN_HEAD = "keten zonder sourcePath in een vouwbron: ";
    static final String CHAIN_TAIL = " — een sourcePath loopt van het regelonderwerp naar de elementen en bindt "
            + "elke stap aan precies één rol (IR-SPEC ch. 4.3/WF-102), dus de aggregatie blijft onopgelost "
            + "(§13.4.17)";

    static final String PATH_STEP_MESSAGE =
            "tussenstap in een vouwbron: 'alle vondsten van de boot van de Jutter' bereikt de elementen via "
            + "Boot, maar een vouwbron draagt precies één relatie vanaf het regelonderwerp Jutter (IR-SPEC "
            + "ch. 4.3: één elementType plus één sourceRole) — de tussenstap zou spoorloos wegvallen, dus "
            + "de aggregatie blijft onopgelost (§13.4.1/§13.4.17)";

    static final String NAME_MESSAGE =
            "vouwbron bindt niet op de geschreven naam: de zin noemt 'geborgen vondsten', maar het enige "
            + "feittype dat Jutter met Vondst verbindt is 'vondstaanmelding' met de rol 'aangemelde vondst' — "
            + "de type-paar-afleiding zou een andere verzameling vouwen dan de zin noemt, dus de aggregatie "
            + "blijft onopgelost (§13.4.1/§13.4.17) — 'de minimale waarde van het gewicht van alle geborgen "
            + "vondsten van de Jutter'";

    private static List<Diagnostic> foldSourceDiagnostics(String source) {
        RegelSpraakModel m = Resolver.resolve(source);
        return m.diagnostics.stream()
                .filter(d -> d.message().startsWith(CHAIN_HEAD)
                        || d.message().startsWith("tussenstap in een vouwbron")
                        || d.message().startsWith("rolanker in een vouwbron")
                        || d.message().startsWith("vouwbron bindt niet op de geschreven naam"))
                .toList();
    }

    private static String ir(String source) {
        RegelSpraakModel m = Resolver.resolve(source);
        assertTrue(m.isOk(), () -> "model draagt fouten: " + m.diagnostics);
        return IrEmitter.emit(m);
    }

    // ---------------- (a) the nested set step ----------------

    /** Document A: no direct relation. UNTIL 2026-09-11 this was a refusal, because the wire could not
     *  state the range; with WF-102 it states it, so the sentence resolves and the fold is emitted. */
    @Test
    void nested_chain_sentence_without_direct_relation_becomes_a_source_path() {
        assertEquals(List.of(), foldSourceDiagnostics(MODEL_A + CHAIN_RULE));
        String json = ir(MODEL_A + CHAIN_RULE);
        assertTrue(json.contains("\"kind\": \"minimum\""), "de vouw hoort nu gewoon geëmitteerd te worden");
        assertSourcePath(json, "bemanning", "gevoerde boot", "berging", "geborgen vondst");
    }

    /** Document B — the acute flank. With a direct Jutter–Vondst fact type this document WAS verifier-clean
     *  and answered 5.00 where the sentence says 3.00, in all three executors at once. The by-pair binding
     *  must now be gone from the wire entirely: the path says which relations are walked, so there is
     *  nothing left for a type search to guess. */
    @Test
    void nested_chain_sentence_with_a_direct_fact_type_also_becomes_a_source_path() {
        assertEquals(List.of(), foldSourceDiagnostics(MODEL_B + CHAIN_RULE));
        String json = ir(MODEL_B + CHAIN_RULE);
        assertSourcePath(json, "bemanning", "gevoerde boot", "berging", "geborgen vondst");
        assertFalse(json.contains("\"sourceRole\""), "sourcePath en sourceRole sluiten elkaar uit (WF-102 (a))");
        assertFalse(json.contains("\"role\": \"aangemelde vondst\""),
                "de wire mag geen rolbinding dragen die de zin niet schrijft");
    }

    /** The DEPTH decision, pinned: three written steps produce three bindings. A cap at two would refuse
     *  this sentence, and the corpus anchor executes the same shape. */
    @Test
    void a_three_step_chain_carries_three_steps() {
        assertEquals(List.of(), foldSourceDiagnostics(DEPTH_MODEL + THREE_STEP_RULE));
        assertSourcePath(ir(DEPTH_MODEL + THREE_STEP_RULE),
                "bemanning", "gevoerde boot", "berging", "geborgen vondst", "inpakking", "ingepakte kist");
    }

    /** The nested set step is not always the FIRST step of the written chain: the greedy kaleNaam decides
     *  where the chain splits, so the walk must run the whole van-chain. It binds 'gevoerde boten' from the
     *  Jutter and stops at 'bergende boot', the Boot's own role in berging and none reached from it. */
    @Test
    void a_mixed_chain_with_a_to_one_step_is_also_named() {
        List<Diagnostic> ds = foldSourceDiagnostics(MODEL_B + MIXED_CHAIN_RULE);
        assertEquals(1, ds.size(), () -> "verwacht één vouwbron-diagnose, kreeg " + ds);
        assertTrue(ds.get(0).message().contains("bindt de stap 'bergende boot' vanaf Boot geen rol: geen rol die "
                        + "vanaf Boot te bereiken is, heet 'bergende boot'"),
                () -> "de melding moet de stap noemen waar de wandeling stopt: " + ds.get(0).message());
    }

    /** A chain that DECLINES still produces the marker, and the refusal machinery is therefore alive: the
     *  mixed chain's middle step resolves to nothing, so no path can be stated and the old refusal stands. */
    @Test
    void an_unsolvable_chain_stays_a_marking() {
        String json = ir(MODEL_B + MIXED_CHAIN_RULE);
        assertFalse(json.contains("\"sourcePath\""), "een onoplosbare keten mag geen half pad op de wire zetten");
        assertFalse(json.contains("\"sourceRole\""), "een geneste keten mag geen sourceRole op de wire zetten");
        assertFalse(json.contains("\"kind\": \"minimum\""), "de vouw mag niet geëmitteerd worden");
        assertTrue(json.contains("\"node\": \"Unresolved\""), "de vouw moet een zichtbare markering worden");
    }

    // ---------------- (a) the chain that declines: where the walk stops ----------------

    /** The chain of {@link #MODEL} with two roles whose plural is regular and not declared. */
    static final String NO_PLURAL_MODEL = """
        Eenheidsysteem Gewicht
            het kilogram (mv: kilogrammen) kg

        Objecttype de Jutter (mv: Jutters) (bezield)
            het laagste vondstgewicht Numeriek (getal met 2 decimalen) met eenheid kg;

        Objecttype de Boot (mv: Boten)

        Objecttype de Vondst (mv: Vondsten)
            het gewicht (mv: gewichten) Numeriek (getal met 2 decimalen) met eenheid kg;

        Feittype bemanning
            de schipper Jutter
            de sloep Boot
        Eén schipper heeft meerdere sloepen

        Feittype berging
            de bergende boot Boot
            de geborgen vondst Vondst
        Eén bergende boot heeft meerdere geborgen vondsten
        """;

    /** The same model with both plurals declared: the way out the refusal names, taken. */
    static final String SLOEPMODEL = NO_PLURAL_MODEL
            .replace("de sloep Boot", "de sloep (mv: sloepen) Boot")
            .replace("de geborgen vondst Vondst", "de geborgen vondst (mv: geborgen vondsten) Vondst");

    /** Two roles named 'geborgen vondst' in two fact types with different fillers: the written element name is read
     *  as the first declared, Wrak, while the walk from the Boot reaches the other, Vondst. */
    static final String LANDINGMODEL = """
        Eenheidsysteem Gewicht
            het kilogram (mv: kilogrammen) kg

        Objecttype de Jutter (mv: Jutters) (bezield)
            het laagste vondstgewicht Numeriek (getal met 2 decimalen) met eenheid kg;

        Objecttype de Kotter (mv: Kotters)

        Objecttype het Wrak (mv: Wrakken)
            het gewicht Numeriek (getal met 2 decimalen) met eenheid kg;

        Objecttype de Boot (mv: Boten)

        Objecttype de Vondst (mv: Vondsten)
            het gewicht (mv: gewichten) Numeriek (getal met 2 decimalen) met eenheid kg;

        Feittype wrakberging
            de bergende kotter Kotter
            de geborgen vondst (mv: geborgen vondsten) Wrak
        Eén bergende kotter heeft meerdere geborgen vondsten

        Feittype bemanning
            de schipper Jutter
            de sloep (mv: sloepen) Boot
        Eén schipper heeft meerdere sloepen

        Feittype berging
            de bergende boot Boot
            de geborgen vondst (mv: geborgen vondsten) Vondst
        Eén bergende boot heeft meerdere geborgen vondsten
        """;

    /** A rule on the Jutter that folds the gewicht over {@code source}. */
    private static String regel(String source) {
        return "\n\nRegel laagste vondstgewicht\n    geldig altijd\n        Het laagste vondstgewicht van een Jutter "
                + "moet berekend worden als de minimale waarde van het gewicht van " + source + ".\n";
    }

    /** The one refusal the chain gets, whole. */
    private static void onlyMessage(String source, String message) {
        List<Diagnostic> ds = foldSourceDiagnostics(source);
        assertEquals(1, ds.size(), () -> "verwacht één vouwbron-diagnose, kreeg " + ds);
        assertEquals(message, ds.get(0).message());
    }

    /** A step spelled in a plural the model does not declare: the walk binds a declared name or plural only. The
     *  one-step range over the same spelling binds by the type pair, which a path step does not have (WF-102). */
    @Test
    void a_step_in_an_undeclared_plural_is_named_with_the_exit() {
        onlyMessage(NO_PLURAL_MODEL + regel("alle geborgen vondsten van alle sloepen van de Jutter"),
                DERIVED_PLURAL_MESSAGE);
        String json = ir(NO_PLURAL_MODEL + regel("alle geborgen vondsten van alle sloepen van de Jutter"));
        assertFalse(json.contains("\"sourcePath\""), "een onoplosbare keten mag geen half pad op de wire zetten");
        assertTrue(json.contains("\"node\": \"Unresolved\""), "de vouw moet een zichtbare markering worden");
    }

    /** Each step is judged on its own: with the first plural declared, the walk binds 'sloepen' and names the second
     *  step, from the Boot it reached. */
    @Test
    void the_message_names_the_step_where_the_walk_stops() {
        onlyMessage(NO_PLURAL_MODEL.replace("de sloep Boot", "de sloep (mv: sloepen) Boot")
                        + regel("alle geborgen vondsten van alle sloepen van de Jutter"),
                CHAIN_HEAD + "in 'alle geborgen vondsten van alle sloepen van de Jutter' bindt de stap 'geborgen "
                        + "vondsten' vanaf Boot geen rol: 'geborgen vondsten' is geen gedeclareerde naam of "
                        + "gedeclareerd meervoud, alleen het afgeleide meervoud (+s/+en) van de rol 'geborgen vondst' "
                        + "van het feittype 'berging', en een afgeleid meervoud bindt geen stap" + CHAIN_TAIL
                        + "; declareer het meervoud op de rol: 'geborgen vondst (mv: geborgen vondsten)' — 'de "
                        + "minimale waarde van het gewicht van alle geborgen vondsten van alle sloepen van de Jutter'");
    }

    /** The way out works: with both plurals declared the same sentence is a two-step path. */
    @Test
    void with_both_plural_declared_same_sentence_becomes_a_source_path() {
        String source = SLOEPMODEL + regel("alle geborgen vondsten van alle sloepen van de Jutter");
        assertEquals(List.of(), foldSourceDiagnostics(source));
        assertSourcePath(ir(source), "bemanning", "sloep", "berging", "geborgen vondst");
    }

    /** A chain with a to-ONE step and the same undeclared plural. Until 2026-09-27 this got route (b)'s sentence,
     *  that the set is anchored on the role 'sloep', which denotes another instance; the walk says what stopped it. */
    @Test
    void a_to_one_chain_with_an_undeclared_plural_names_the_step() {
        onlyMessage(NO_PLURAL_MODEL + regel("alle geborgen vondsten van de sloep van de Jutter"),
                CHAIN_HEAD + "in 'alle geborgen vondsten van de sloep van de Jutter' bindt de stap 'geborgen vondsten' "
                        + "vanaf Boot geen rol: 'geborgen vondsten' is geen gedeclareerde naam of gedeclareerd "
                        + "meervoud, alleen het afgeleide meervoud (+s/+en) van de rol 'geborgen vondst' van het "
                        + "feittype 'berging', en een afgeleid meervoud bindt geen stap" + CHAIN_TAIL
                        + "; declareer het meervoud op de rol: 'geborgen vondst (mv: geborgen vondsten)' — 'de "
                        + "minimale waarde van het gewicht van alle geborgen vondsten van de sloep van de Jutter'");
    }

    /** A step that names more than one role reachable from where it starts. */
    @Test
    void a_step_that_names_more_than_one_role_is_named() {
        onlyMessage(SLOEPMODEL + """

                Feittype verhuur
                    de verhuurder Jutter
                    de sloep (mv: sloepen) Boot
                Eén verhuurder heeft meerdere sloepen
                """ + regel("alle geborgen vondsten van alle sloepen van de Jutter"),
                CHAIN_HEAD + "in 'alle geborgen vondsten van alle sloepen van de Jutter' bindt de stap 'sloepen' vanaf "
                        + "Jutter geen rol: 'sloepen' noemt er meer dan één (de rollen 'sloep' van het feittype "
                        + "'bemanning' en 'sloep' van het feittype 'verhuur')" + CHAIN_TAIL + " — 'de minimale "
                        + "waarde van het gewicht van alle geborgen vondsten van alle sloepen van de Jutter'");
    }

    /** A step that names no role reachable from where it starts. */
    @Test
    void a_step_that_names_no_role_is_named() {
        onlyMessage(SLOEPMODEL + regel("alle geborgen vondsten van alle kotters van de Jutter"),
                CHAIN_HEAD + "in 'alle geborgen vondsten van alle kotters van de Jutter' bindt de stap 'kotters' vanaf "
                        + "Jutter geen rol: geen rol die vanaf Jutter te bereiken is, heet 'kotters' of declareert dat "
                        + "als meervoud" + CHAIN_TAIL + " — 'de minimale waarde van het gewicht van alle geborgen "
                        + "vondsten van alle kotters van de Jutter'");
    }

    /** A step over a reciprocal fact type, whose single role this frontend does not write as a step. */
    @Test
    void a_step_over_a_reciprocal_fact_type_is_named() {
        onlyMessage(SLOEPMODEL + """

                Wederkerig feittype strandmaatschap
                    de strandmaat (mv: strandmaten) Jutter
                Eén strandmaat heeft één strandmaat
                """ + regel("alle geborgen vondsten van alle sloepen van alle strandmaten van de Jutter"),
                CHAIN_HEAD + "in 'alle geborgen vondsten van alle sloepen van alle strandmaten van de Jutter' bindt de "
                        + "stap 'strandmaten' vanaf Jutter geen rol: 'strandmaten' noemt de rol 'strandmaat' van het "
                        + "wederkerige feittype 'strandmaatschap', en over een wederkerig feittype schrijft deze "
                        + "frontend geen stap" + CHAIN_TAIL + " — 'de minimale waarde van het gewicht van alle "
                        + "geborgen vondsten van alle sloepen van alle strandmaten van de Jutter'");
    }

    /** A step whose one role is filled by a type the model does not declare. */
    @Test
    void a_step_to_an_undeclared_type_is_named() {
        onlyMessage(SLOEPMODEL.replace("de sloep (mv: sloepen) Boot", "de sloep (mv: sloepen) Schuit")
                        + regel("alle geborgen vondsten van alle sloepen van de Jutter"),
                CHAIN_HEAD + "in 'alle geborgen vondsten van alle sloepen van de Jutter' bindt de stap 'sloepen' vanaf "
                        + "Jutter geen rol: de rol 'sloep' van het feittype 'bemanning' wordt gevuld door 'Schuit', en "
                        + "dat is geen gedeclareerd objecttype" + CHAIN_TAIL + " — 'de minimale waarde van het "
                        + "gewicht van alle geborgen vondsten van alle sloepen van de Jutter'");
    }

    /** A chain whose anchor is not the rule subject. */
    @Test
    void a_chain_with_another_anchor_is_named() {
        onlyMessage(SLOEPMODEL + regel("alle geborgen vondsten van alle sloepen van de Boot"),
                CHAIN_HEAD + "'alle geborgen vondsten van alle sloepen van de Boot' begint bij 'Boot' en niet bij het "
                        + "regelonderwerp Jutter" + CHAIN_TAIL + " — 'de minimale waarde van het gewicht van alle "
                        + "geborgen vondsten van alle sloepen van de Boot'");
    }

    /** A nested chain without an anchor: two segments, so the walk has nothing to start from. */
    @Test
    void a_nested_chain_without_an_anchor_is_named() {
        onlyMessage(SLOEPMODEL + regel("alle geborgen vondsten van alle sloepen"),
                CHAIN_HEAD + "'alle geborgen vondsten van alle sloepen' begint niet bij het regelonderwerp Jutter"
                        + CHAIN_TAIL + " — 'de minimale waarde van het gewicht van alle geborgen vondsten van alle "
                        + "sloepen'");
    }

    /** An element name that names nothing, under an anchor that IS the subject: the Jutter fills 'aanmelder'. The walk
     *  runs its steps before it compares the element type, so it names the step instead of claiming that the chain
     *  starts elsewhere. */
    @Test
    void an_unknown_element_name_is_named_as_a_step() {
        onlyMessage(MODEL_B + regel("alle kisten van alle gevoerde boten van de aanmelder"),
                CHAIN_HEAD + "in 'alle kisten van alle gevoerde boten van de aanmelder' bindt de stap 'kisten' vanaf "
                        + "Boot geen rol: geen rol die vanaf Boot te bereiken is, heet 'kisten' of declareert dat als "
                        + "meervoud" + CHAIN_TAIL + " — 'de minimale waarde van het gewicht van alle kisten van "
                        + "alle gevoerde boten van de aanmelder'");
    }

    /** A walk that lands on another type than the object type the written element name names. Since 2026-10-02 a
     *  role name that names no object type takes the type the walk lands on (ElementNameFoldSourceTest), so the
     *  refusal stands where the name is the plural of an object type, Wrak, outside the landing, Vondst. */
    @Test
    void a_chain_that_comes_out_elsewhere_is_named() {
        onlyMessage(LANDINGMODEL.replace("    de geborgen vondst (mv: geborgen vondsten) Vondst\n"
                        + "Eén bergende boot heeft meerdere geborgen vondsten",
                        "    de wrak (mv: wrakken) Vondst\nEén bergende boot heeft meerdere wrakken")
                        + regel("alle wrakken van alle sloepen van de Jutter"),
                CHAIN_HEAD + "'alle wrakken van alle sloepen van de Jutter' komt via haar stappen uit bij "
                        + "Vondst, maar de geschreven elementnaam is gelezen als Wrak" + CHAIN_TAIL + " — 'de "
                        + "minimale waarde van het gewicht van alle wrakken van alle sloepen van de Jutter'");
    }

    /** An unknown to-ONE step beside a direct Jutter–Vondst relation. Measured on main 4db70391: route (b) left the
     *  unknown anchor alone, (c) found the written name naming the one relation, and the emitter wrote a one-step
     *  fold over 'aangemelde vondst' with {@code diagnostics: []}, the step 'van de kotter' gone. The walk now
     *  names that step, and the fold is the marker. */
    @Test
    void an_unknown_to_one_step_not_more_falls_silent_away() {
        String source = MODEL_B + regel("alle aangemelde vondsten van de kotter van de Jutter");
        onlyMessage(source,
                CHAIN_HEAD + "in 'alle aangemelde vondsten van de kotter van de Jutter' bindt de stap 'kotter' vanaf "
                        + "Jutter geen rol: geen rol die vanaf Jutter te bereiken is, heet 'kotter' of declareert dat "
                        + "als meervoud" + CHAIN_TAIL + " — 'de minimale waarde van het gewicht van alle "
                        + "aangemelde vondsten van de kotter van de Jutter'");
        String json = ir(source);
        assertFalse(json.contains("\"sourceRole\""), "de wire mag de geschreven stap niet laten vallen");
        assertTrue(json.contains("\"node\": \"Unresolved\""), "de vouw moet een zichtbare markering worden");
    }

    // ---------------- (b) the intermediate to-ONE step ----------------

    /** One set step with a to-ONE step under it. The fold source names one relation from the RULE SUBJECT, so
     *  "van de boot" disappeared exactly as a nested set step does — and the by-pair binding was RIGHT about
     *  the role name, so nothing else saw it: measured `diagnostics: []`, verifier PASS, and 5.00 where the
     *  sentence means 3.00. */
    /** Document F, and the decision the acute flank left open: a to-ONE step is an ORDINARY step of the
     *  range (RUNTIME-SEMANTICS ch. 3.4b — multiplicity is a declaration fact and does not enter the
     *  reading), so the sentence is expressible and the path says so. The direct <em>jutterbezit</em>
     *  relation, which the by-pair search used to bind here, must be absent from the wire. */
    @Test
    void a_to_one_path_step_becomes_a_step_on_the_path() {
        assertEquals(List.of(), foldSourceDiagnostics(FLANKMODEL + PATH_STEP_RULE));
        String json = ir(FLANKMODEL + PATH_STEP_RULE);
        assertTrue(json.contains("\"kind\": \"minimum\""), "de vouw hoort nu gewoon geëmitteerd te worden");
        assertSourcePath(json, "bemanning", "boot", "bootvangst", "vondst");
        assertFalse(json.contains("\"factType\": \"jutterbezit\""),
                "de type-paar-afleiding mag niet meer binden waar de zin een keten schrijft");
    }

    /** Route (b) keeps its witness on the shape a path cannot rescue: ONE step whose anchor names an
     *  object type that is not the rule subject. There is no chain to resolve, so the anchor rule refuses
     *  it exactly as it did. */
    @Test
    void an_anchor_that_is_not_the_subject_stays_refused() {
        List<Diagnostic> ds = foldSourceDiagnostics(MODEL_B + FOREIGN_ANCHOR_RULE);
        assertEquals(1, ds.size(), () -> "verwacht één vouwbron-diagnose, kreeg " + ds);
        assertTrue(ds.get(0).message().startsWith("tussenstap in een vouwbron"),
                () -> "verwacht de ankerregel, kreeg " + ds.get(0).message());
    }

    /** Same model, same written element name, NO intermediate step: the fold binds and emits. This is what
     *  keeps the anchor rule off the ordinary one-step form. */
    @Test
    void without_a_path_step_the_same_sentence_binds_simply() {
        assertEquals(List.of(), foldSourceDiagnostics(FLANKMODEL + DIRECT_RULE));
        String json = ir(FLANKMODEL + DIRECT_RULE);
        assertTrue(json.contains("\"kind\": \"minimum\""), "de vouw moet gewoon geëmitteerd worden");
        assertTrue(json.contains("\"factType\": \"jutterbezit\""), "de directe relatie hoort op de wire");
    }

    /** The role-anchor question the acute flank left to this card, DECIDED (2026-09-11) and decided by the
     *  order ch. 4.1a already fixes rather than by a new test. "van de aanmelder" has ZERO far hits from the
     *  subject and the subject type fills a role of that name, so it denotes the SUBJECT: the range starts
     *  there, it is one step, and the wire spelling is the one-step {@code sourceRole}. The outcome is
     *  byte-identical to before the decision, which is the point — the decision moves no document. */
    @Test
    void a_self_denoting_role_anchor_stays_a_one_step_binding() {
        assertEquals(List.of(), foldSourceDiagnostics(MODEL_B + ROLE_ANCHOR_RULE));
        String json = ir(MODEL_B + ROLE_ANCHOR_RULE);
        assertTrue(json.contains("\"role\": \"aangemelde vondst\""),
                "een zelfdenoterend anker IS het onderwerp, dus de eenstaps-binding blijft staan");
        assertFalse(json.contains("\"sourcePath\""), "één relatie blijft één relatie: geen pad");
    }

    /** The other half of the same decision: a role REACHABLE FROM the subject denotes ANOTHER instance, so
     *  it is a hop and not an anchor. With only one written step there is no path to put it on, and binding
     *  the fold to the subject's own relation would fold the wrong set in silence — so it is refused. */
    @Test
    void a_far_role_as_anchor_is_refused() {
        List<Diagnostic> ds = foldSourceDiagnostics(ROLE_ANCHOR_MODEL + FAR_ROLE_ANCHOR_RULE);
        assertEquals(1, ds.size(), () -> "verwacht één vouwbron-diagnose, kreeg " + ds);
        assertTrue(ds.get(0).message().startsWith("rolanker in een vouwbron"),
                () -> "verwacht de rolanker-melding, kreeg " + ds.get(0).message());
        assertTrue(ds.get(0).message().contains("strandmaat"), "de melding noemt de rol");
    }

    // ---------------- (c) the by-pair binding the written name contradicts ----------------

    /** ONE step, no nesting: the sentence writes "geborgen vondsten", the by-pair search bound the role
     *  "aangemelde vondst" of vondstaanmelding. Verifier-clean and silently wrong before this check. */
    @Test
    void one_step_fold_with_a_colliding_written_name_is_named() {
        List<Diagnostic> ds = foldSourceDiagnostics(MODEL_B + COLLIDING_ONE_STEP_RULE);
        assertEquals(1, ds.size(), () -> "verwacht één vouwbron-diagnose, kreeg " + ds);
        assertEquals(NAME_MESSAGE, ds.get(0).message());
    }

    @Test
    void the_colliding_one_step_fold_source_is_not_folded() {
        String json = ir(MODEL_B + COLLIDING_ONE_STEP_RULE);
        assertFalse(json.contains("\"role\": \"aangemelde vondst\""),
                "de wire mag geen rolbinding dragen die de zin niet schrijft");
        assertTrue(json.contains("\"node\": \"Unresolved\""), "de vouw moet een zichtbare markering worden");
    }

    /** Without the direct fact type there is nothing to bind silently, so the by-pair check stays out of it
     *  and the document keeps its existing downstream failure (WF-048 until L2; since then the emitter refuses a
     *  fold it cannot bind). */
    @Test
    void a_one_step_fold_without_any_relation_stays_downstream() {
        assertEquals(List.of(), foldSourceDiagnostics(MODEL_A + COLLIDING_ONE_STEP_RULE));
    }

    // ---------------- counter-proofs ----------------

    /** The one form this card must not touch: a correct one-step fold whose written name names the role the
     *  fact type declares. It emits exactly as before, sourceRole and all. */
    @Test
    void a_correct_one_step_fold_stays_emitting_unchanged() {
        assertEquals(List.of(), foldSourceDiagnostics(MODEL_B + CORRECT_ONE_STEP_RULE));
        String json = ir(MODEL_B + CORRECT_ONE_STEP_RULE);
        assertTrue(json.contains("\"kind\": \"minimum\""), "de vouw moet gewoon geëmitteerd worden");
        assertTrue(json.contains("\"role\": \"aangemelde vondst\""), "de sourceRole hoort op de wire");
        assertFalse(json.contains("\"node\": \"Unresolved\""), "geen markering op een correcte vouw");
    }

    /** A written name that IS the element object type's own name claims no role, so the by-pair derivation
     *  remains the honest fallback and the fold keeps its binding. */
    @Test
    void a_written_object_type_name_keeps_the_type_pair_derivation() {
        assertEquals(List.of(), foldSourceDiagnostics(MODEL_B + OBJECT_TYPE_NAME_RULE));
        assertTrue(ir(MODEL_B + OBJECT_TYPE_NAME_RULE).contains("\"role\": \"aangemelde vondst\""),
                "de by-pair afleiding blijft staan waar de zin geen rol noemt");
    }

    /** The §13.4.17.47 telling binds through the WRITTEN element name already (elementRoleBinding), a different
     *  derivation on a different sentence shape. The nested telvorm is explicitly another card. */
    @Test
    void the_count_form_stays_binding_via_the_written_element_name() {
        assertEquals(List.of(), foldSourceDiagnostics(MODEL_B + COUNT_RULE));
        String json = ir(MODEL_B + COUNT_RULE);
        assertTrue(json.contains("\"kind\": \"count\""), "de telling moet gewoon geëmitteerd worden");
        assertTrue(json.contains("\"role\": \"aangemelde vondst\""), "de telling bindt via de geschreven naam");
    }

    /** The same nested chain with no fold around it is the set fence's position, decided by another card: it
     *  must keep ITS diagnosis and must not pick up this one. That diagnosis is the verifier's WF-053 sentence. */
    @Test
    void a_set_read_outside_a_fold_keeps_its_own_diagnostic() {
        assertEquals(List.of(), foldSourceDiagnostics(MODEL_B + SET_READ_WITHOUT_FOLD));
        RegelSpraakModel m = Resolver.resolve(MODEL_B + SET_READ_WITHOUT_FOLD);
        assertEquals(1, m.diagnostics.size(), () -> "verwacht enkel de setlezing-diagnose, kreeg " + m.diagnostics);
        assertTrue(m.diagnostics.get(0).message().startsWith("setlezing buiten een vouwbron")
                        && m.diagnostics.get(0).message().contains(" reaches the to-many role "),
                () -> "verwacht de setlezing-melding, kreeg " + m.diagnostics.get(0));
    }

    // ---------------- the severity, and the wire ----------------

    /** The level is the one knob this check has, pinned so a promotion is a visible edit. WARNING keeps the
     *  document on disk with its own defect stated; ERROR would make {@code Resolver.irMode} write nothing
     *  at all. Either way no wrong answer survives — the marker is not a fold. */
    @Test
    void the_diagnostic_is_a_warning_and_no_error() {
        List<Diagnostic> ds = foldSourceDiagnostics(MODEL_B + MIXED_CHAIN_RULE);
        assertEquals(1, ds.size());
        assertEquals(Severity.WARNING, ds.get(0).severity());
        assertTrue(Resolver.resolve(MODEL_B + MIXED_CHAIN_RULE).isOk(),
                "een WARNING mag de emissie niet blokkeren");
    }

    /** The marker carries the aggregation's OWN result type. A marker whose type is unknown carries none
     *  (IR-SPEC ch. 4.4), and this one's is known: the fold would have produced a kg amount, and the executors
     *  read the marker's type where a comparison or an assignment needs one. WF-044's fence is behavioural, so
     *  the failure lands as a loud evaluation fault in every executor. Measured on all three routes. */
    @Test
    void the_marking_carries_its_own_result_type() {
        for (String source : List.of(MODEL_B + MIXED_CHAIN_RULE, MODEL_B + COLLIDING_ONE_STEP_RULE,
                MODEL_B + FOREIGN_ANCHOR_RULE)) {
            String json = ir(source);
            assertTrue(json.contains("\"node\": \"Unresolved\""), "verwacht een markering");
            assertTrue(java.util.regex.Pattern.compile("\"node\": \"Unresolved\",\\s*\"type\": \\{").matcher(json).find(),
                    "de markering hoort haar eigen resultaattype te dragen");
            assertTrue(json.contains("\"abbreviation\": \"kg\""), "het eigen resultaattype hoort erop te staan");
        }
    }

    /** The emitter writes every diagnostic, so the document itself states what is wrong with it. */
    @Test
    void the_emission_carries_the_diagnostic_on_the_wire() {
        String json = ir(MODEL_B + MIXED_CHAIN_RULE);
        assertTrue(json.contains("\"severity\": \"warning\""), "de diagnose hoort op de wire");
        assertTrue(json.contains(CHAIN_HEAD.strip()), "de wire moet de reden dragen, niet alleen een niveau");
    }

    /** The emitted {@code sourcePath}, step by step and in order: the pairs are given subject-outwards, and
     *  the assertion is on the ORDER because a range read backwards walks a different chain. */
    private static void assertSourcePath(String json, String... factTypeAndRole) {
        assertTrue(json.contains("\"sourcePath\""), () -> "verwacht een sourcePath op de wire:\n" + json);
        int from = json.indexOf("\"sourcePath\"");
        for (int i = 0; i < factTypeAndRole.length; i += 2) {
            String step = "\"factType\": \"" + factTypeAndRole[i] + "\"";
            String roleName = factTypeAndRole[i + 1];
            int here = json.indexOf(step, from);
            assertTrue(here >= 0, () -> "verwacht stap " + step + " in het pad:\n" + json);
            int role = json.indexOf("\"role\": \"" + roleName + "\"", here);
            assertTrue(role > here, () -> "stap " + step + " moet rol '" + roleName + "' binden");
            from = role;
        }
    }
}
