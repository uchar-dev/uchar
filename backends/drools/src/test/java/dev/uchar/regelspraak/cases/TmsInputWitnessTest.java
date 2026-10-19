package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;
import dev.uchar.regelspraak.transpiler.tms.Fixpoint;
import dev.uchar.regelspraak.transpiler.tms.Fault;

import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieSession;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The input of a session through {@link DroolsBackend#insert} (card tms-omschakeling, point 12): the beans, and what the
 * branch reads beside them, named by the consumer in the words of the model ({@link Input}), so that
 * no consumer states a fact class of that branch.
 *
 * <ul>
 *   <li>The order of a to-many list decides which member a fault names where a fold reads its members one by one and
 *       stops at the first fault (RUNTIME-SEMANTICS ch. 5.11). On the document of {@link TmsFilterFaultWitnessTest} the
 *       filter of the fold faults for both Vondsten of J1, V1 dividing 9 and V2 4 by their deelnemers 0: in the order
 *       of the beans V1, V2 the fault names 9, in the order V2, V1 it names 4, and a list given as V2, V1 names 4 with
 *       the beans in the order V1, V2.</li>
 *   <li>A given list that misses a member whose reference field holds the holder leaves that member without a place:
 *       {@code insert} refuses it by name, and inserts nothing, where the fire would have stopped with an
 *       {@code IllegalStateException} at the first rule that needs the place. So does every other statement that does
 *       not fit the beans or the model, and a session that is not fresh.</li>
 *   <li>An object a bean refers to that the input does not give (the fix round of step 3, point 2): the input of golden
 *       {@code anchor/jutterswet-rekenkamer}, case 4, J1 with its field rekenkamer on R1, with J1 given alone. The
 *       branch derives nothing for R1 outside the session, and the link the field gives blocks the creation, so J1's
 *       heffing, which reads R1's, would stay empty; {@code insert} refuses it, naming J1, the field and the type
 *       Rekenkamer, and inserts nothing. With R1 given J1's heffing is the 6 the case pins, which the reference
 *       interpreter and the ASP runner give (measured 2026-10-10). <b>Red before the change</b> (measured 2026-10-10
 *       on {@code f4b83d7b}): "Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.", and
 *       fired and read, the input with J1 alone gave J1 no heffing: "expected: &lt;[alleen J1: J1.heffing = 6, J1 en
 *       R1: J1.heffing = 6]&gt; but was: &lt;[alleen J1: J1.heffing = leeg, J1 en R1: J1.heffing = 6]&gt;".</li>
 *   <li>A bucket scalar ({@link Input#bucketScalar}): on the document of {@link TmsInputFormWitnessTest} part C, the
 *       subject's own total without a clip of J3, whose bedrag the bean holds as the one segment of June 2024, is 7 with
 *       the statement and faults without it, since the branch then reads the segment as a full timeline (ch. 8.4).</li>
 *   <li>On the exec-model and the DRL-text branch it inserts the beans alone.</li>
 * </ul>
 *
 * <p>Measured on 2026-10-10, both documents passing the verifier: on document C with J3 alone, the reference
 * interpreter and the ASP runner give topsom 7 with the bedrag 7 as a bucket scalar, and with the same 7 as the full
 * timeline of June 2024 the one faults and the other refuses the translation with the sentence of the
 * {@code InputFormFault}, "the temporal total of "bedrag" over a case-supplied full timeline is not exercised by any
 * corpus case; the asOfDate-bucket convention carries the total". On the filter document, in either order, the
 * reference interpreter faults with "division by numeric zero", a sentence without the operands, and the ASP runner
 * reports both members without an order: which member it names, {@code TmsFilterFaultWitnessTest} measured with the
 * dividend added to the reference interpreter's sentence.
 *
 * <p><b>Red before the change</b> (measured 2026-10-10 on {@code 095a586a}): the witness does not compile there, since
 * the entry had no {@code insert} and no {@code Input}; a consumer stated {@code BucketInput} and {@code ListPlace}
 * itself, or the fire stopped at the first rule that needed a place. <b>Red by mutation</b> (measured the same day, not
 * committed): without the check that a given list names every member, the row of the forgotten member failed on
 * "Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown."
 */
class TmsInputWitnessTest {

    /** The fault of the fold's filter of {@link TmsFilterFaultWitnessTest} on the member whose gewicht is {@code radicand},
     *  the root of a number that is no square; until the card zero-division-channel the filter divided by 0. */
    private static String rootFault(String radicand) {
        return TmsDocuments.irrational("zware vondsten", radicand);
    }

    @Test void a_list_has_the_order_of_the_beans_by_default() throws Exception {
        DroolsBackend.Build b = vondstBuild();
        TmsRepeatedFireWitnessTest.inSession(b, () -> {
            List<Object> jv = jutterWithVondsten(b);
            Object j1 = jv.get(0), v1 = jv.get(1), v2 = jv.get(2);
            assertEquals(List.of(rootFault("3")), faults(b, new Input().beans(List.of(j1, v1, v2))), "V1 before V2");
            assertEquals(List.of(rootFault("2")), faults(b, new Input().beans(List.of(j1, v2, v1))), "V2 before V1");
        });
    }

    @Test void a_given_list_lays_the_order_fixed() throws Exception {
        DroolsBackend.Build b = vondstBuild();
        TmsRepeatedFireWitnessTest.inSession(b, () -> {
            List<Object> jv = jutterWithVondsten(b);
            Object j1 = jv.get(0), v1 = jv.get(1), v2 = jv.get(2);
            assertEquals(List.of(rootFault("2")),
                    faults(b, new Input().beans(List.of(j1, v1, v2)).list(j1, "aanmelder", List.of(v2, v1))),
                    "the list V2, V1 with the beans in the order V1, V2");
        });
    }

    @Test void a_forgotten_member_in_a_given_list_refuses_insert_loudly() throws Exception {
        DroolsBackend.Build b = vondstBuild();
        TmsRepeatedFireWitnessTest.inSession(b, () -> {
            List<Object> jv = jutterWithVondsten(b);
            Object j1 = jv.get(0), v1 = jv.get(1), v2 = jv.get(2);
            KieSession session = b.kieBase().newKieSession();
            try {
                IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> DroolsBackend.insert(b,
                        session, new Input().beans(List.of(j1, v1, v2)).list(j1, "aanmelder", List.of(v1))));
                assertEquals("voegIn: de lijst van " + name(j1) + " onder de rol 'aanmelder' mist " + name(v2) + ", dat"
                        + " met zijn veld 'aanmelder' naar de houder wijst: een lid buiten de gegeven volgorde heeft geen"
                        + " plaats", e.getMessage());
                assertEquals(0, session.getFactCount(), "nothing was inserted");
            } finally {
                session.dispose();
            }
        });
    }

    @Test void what_does_not_fit_the_beans_or_the_model_refuses_insert_loudly() throws Exception {
        DroolsBackend.Build b = vondstBuild();
        TmsRepeatedFireWitnessTest.inSession(b, () -> {
            List<Object> jv = jutterWithVondsten(b);
            Object j1 = jv.get(0), v1 = jv.get(1), v2 = jv.get(2);
            Object j2 = TmsDocuments.beanObject(b.beanLoader(), "Jutter", Map.of(), List.of());
            Object v3 = TmsDocuments.beanObject(b.beanLoader(), "Vondst", Map.of(), List.of());
            TmsRepeatedFireWitnessTest.setRole(v3, "aanmelder", j2);
            List<Object> beans = List.of(j1, v1, v2);
            String list = "voegIn: de lijst van " + name(j1) + " onder de rol 'aanmelder'";
            List<String> sentences = new ArrayList<>();
            sentences.add(refusal(b, new Input().beans(beans).list(j1, "aanmelder", List.of(v1, v2, v3))));
            sentences.add(refusal(b, new Input().beans(List.of(j1, v1, v2, j2, v3))
                    .list(j1, "aanmelder", List.of(v1, v2, v3))));
            sentences.add(refusal(b, new Input().beans(beans).list(j1, "aanmelder", List.of(v1, v2, v1))));
            sentences.add(refusal(b, new Input().beans(beans).list(j1, "vondst", List.of(v1, v2))));
            sentences.add(refusal(b, new Input().beans(beans).list(j1, "aanmelder", List.of(v1, v2))
                    .list(j1, "aanmelder", List.of(v1, v2))));
            sentences.add(refusal(b, new Input().beans(beans).list(j1, "aanmelder", List.of())));
            sentences.add(refusal(b, new Input().beans(beans).bucketScalar(v3, "gewicht")));
            sentences.add(refusal(b, new Input().beans(beans).bucketScalar(v1, "gewicht")));
            sentences.add(refusal(b, new Input().beans(beans).bucketScalar(v1, "lengte")));
            sentences.add(refusal(b, new Input().beans(List.of(j1, v1, v1))));
            // a bean model of another document: the bean compiler shares the classes of identical models only
            DroolsBackend.Build other = DroolsBackend.build(TmsRepeatedFireWitnessTest.valueDocument(),
                    DroolsBackend.Branch.EXEC_MODEL);
            Object foreign = TmsDocuments.beanObject(other.beanLoader(), "Vondst", Map.of(), List.of());
            sentences.add(refusal(b, new Input().beans(List.of(j1, foreign))));
            assertEquals(List.of(
                    list + " noemt " + name(v3) + ", dat geen bean van de invoer is",
                    list + " noemt " + name(v3) + ", maar diens veld 'aanmelder' wijst naar " + name(j2),
                    list + " noemt " + name(v1) + " twee keer",
                    "voegIn: de lijst van " + name(j1) + " onder de rol 'vondst' noemt " + name(v1) + ", maar een Vondst"
                            + " heeft geen referentieveld 'vondst'; de rol is die van de houder, de naam van het veld"
                            + " waarmee een lid naar de houder wijst",
                    list + " is twee keer gegeven",
                    list + " is leeg gegeven, maar " + name(v1) + " wijst met zijn veld 'aanmelder' naar de houder",
                    "voegIn: emmerscalair 'gewicht' op " + name(v3) + ", een bean die niet in de invoer staat",
                    "voegIn: emmerscalair 'gewicht' op " + name(v1) + ", maar dat attribuut van Vondst ligt niet op een"
                            + " tijdlijn; alleen de invoer van een tijdlijn kent de emmerconventie (RUNTIME-SEMANTICS ch."
                            + " 8.4)",
                    "voegIn: emmerscalair 'lengte' op " + name(v1) + ", maar Vondst heeft geen attribuut van die naam",
                    "voegIn: de bean " + name(v1) + " staat twee keer in de invoer",
                    "voegIn: " + name(foreign) + " is geen bean van dit model: een bean is een instantie van een klasse die"
                            + " Build.beanLoader() laadt"), sentences);
            KieSession session = b.kieBase().newKieSession();
            try {
                DroolsBackend.insert(b, session, new Input().beans(beans));
                IllegalStateException e = assertThrows(IllegalStateException.class,
                        () -> DroolsBackend.insert(b, session, new Input().bean(j2)));
                assertEquals("voegIn vult een verse sessie, maar deze houdt al " + session.getFactCount() + " feiten: de"
                        + " plaatsen van een lijst worden over de beans van één invoer geteld, en een bean die zonder"
                        + " voegIn in de sessie kwam, heeft er geen; geef alle beans in één Invoer aan een verse sessie,"
                        + " en verander tussen twee fires hun waarden met update", e.getMessage());
            } finally {
                session.dispose();
            }
        });
    }

    @Test void a_referenced_object_that_is_not_given_refuses_insert_loudly() throws Exception {
        Path ir = Path.of("..", "..", "corpus", "golden", "anchor", "jutterswet-rekenkamer", "ir.json");
        JsonNode caseDocument = TmsDocuments.JSON.readTree(
                Files.readString(ir.resolveSibling("case4-koppeling-op-de-gemunte-zijde.json")));
        DroolsBackend.Build b = DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL);
        TmsRepeatedFireWitnessTest.inSession(b, () -> {
            Map<String, Object> beans = CaseRunner.inputBeans(ir, caseDocument);
            Object j1 = beans.get("J1"), r1 = beans.get("R1");
            assertSame(r1, RegelSpraakRuntime.get(j1, "rekenkamer"), "the case links J1 to R1");
            KieSession session = b.kieBase().newKieSession();
            try {
                IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                        () -> DroolsBackend.insert(b, session, new Input().bean(j1)));
                assertEquals("voegIn: het veld 'rekenkamer' van " + name(j1) + " wijst naar een Rekenkamer ("
                        + name(r1) + ") die niet als bean in de invoer staat: de tak leidt alleen af voor de objecten"
                        + " in de sessie, dus geef elk object waarnaar een bean wijst ook zelf als bean op",
                        e.getMessage());
                assertEquals(0, session.getFactCount(), "nothing was inserted");
            } finally {
                session.dispose();
            }
            Fixpoint both = read(b, new Input().beans(List.of(j1, r1)));
            assertTrue(both.values().stream().anyMatch(w -> w.object() == j1 && w.attribute().equals("heffing")
                    && w.value() instanceof BigDecimal d && d.compareTo(new BigDecimal("6")) == 0),
                    () -> "with R1 given, J1 heffing 6 as the case pins: " + both.values());
        });
    }

    @Test void a_bucket_scalar_reads_the_input_as_bucket_and_otherwise_as_full_timeline() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(TmsTimelineDocuments.document(TmsInputFormWitnessTest.MODEL_C,
                TmsInputFormWitnessTest.RULES_C, List.of()), DroolsBackend.Branch.EXEC_MODEL);
        TmsRepeatedFireWitnessTest.inSession(b, () -> {
            Object j3 = TmsDocuments.beanObject(b.beanLoader(), "Jutter", Map.of("score", "10"), List.of());
            TmsTimelineDocuments.set(j3, "bedrag", List.of(
                    TmsTimelineDocuments.beanSegment(b.beanLoader(), "2024-06-01", "2024-07-01", "7")));
            Fixpoint bucket = read(b, new Input().bean(j3).bucketScalar(j3, "bedrag"));
            assertEquals(List.of(), bucket.faults(), "no fault over the bucket scalar");
            assertTrue(bucket.values().stream().anyMatch(w -> w.object() == j3 && w.attribute().equals("topsom")
                    && w.value() instanceof BigDecimal d && d.compareTo(new BigDecimal("7")) == 0),
                    () -> "J3 topsom 7: " + bucket.values());
            Fixpoint complete = read(b, new Input().bean(j3));
            assertEquals(1, complete.faults().size(), () -> "one fault over a full timeline: " + complete.faults());
            assertTrue(complete.faults().get(0).message().startsWith("fout in de regel \"topsom\": the temporal total of"
                    + " \"bedrag\" over a case-supplied full timeline is not exercised by any corpus case"),
                    complete.faults().get(0).message());
        });
    }

    // ---------------- helpers ----------------

    private static DroolsBackend.Build vondstBuild() throws Exception {
        return DroolsBackend.build(TmsFilterFaultWitnessTest.document(TmsFilterFaultWitnessTest.ZWARE),
                DroolsBackend.Branch.EXEC_MODEL);
    }

    /** J1 actief, V1 (gewicht 3) and V2 (gewicht 2), both reported by J1: the filter faults on each. Until the card
     *  zero-division-channel V1 divided 9 and V2 4 by 0. */
    private static List<Object> jutterWithVondsten(DroolsBackend.Build b) throws Exception {
        Object j1 = TmsDocuments.beanObject(b.beanLoader(), "Jutter", Map.of(), List.of("actief"));
        Object v1 = TmsDocuments.beanObject(b.beanLoader(), "Vondst", Map.of("gewicht", "3"), List.of());
        Object v2 = TmsDocuments.beanObject(b.beanLoader(), "Vondst", Map.of("gewicht", "2"), List.of());
        TmsRepeatedFireWitnessTest.setRole(v1, "aanmelder", j1);
        TmsRepeatedFireWitnessTest.setRole(v2, "aanmelder", j1);
        return List.of(j1, v1, v2);
    }

    /** The sentences of the faults that stand after {@code input} is put in a fresh session of {@code b} and fired. */
    private static List<String> faults(DroolsBackend.Build b, Input input) {
        List<String> out = new ArrayList<>();
        for (Fault f : read(b, input).faults()) out.add(f.message());
        return out;
    }

    /** What stands after {@code input} is put in a fresh session of {@code b}, with the evaluation date of
     *  {@code TmsInputFormWitnessTest} where the model reads it, and fired. */
    private static Fixpoint read(DroolsBackend.Build b, Input input) {
        KieSession session = b.kieBase().newKieSession();
        try {
            try {
                session.setGlobal("rekendatum", LocalDate.parse(TmsInputFormWitnessTest.DATE));
            } catch (RuntimeException noGlobal) {
                // the model reads no evaluation date
            }
            DroolsBackend.insert(b, session, input);
            DroolsBackend.Firing firing = DroolsBackend.fire(b, session);
            assertEquals(null, firing.noFixpoint(), "the fixpoint");
            return DroolsBackend.fixpointOf(b, session);
        } finally {
            session.dispose();
        }
    }

    /** The sentence with which {@code insert} refuses {@code input} in a fresh session of {@code b}. */
    private static String refusal(DroolsBackend.Build b, Input input) {
        KieSession session = b.kieBase().newKieSession();
        try {
            return assertThrows(IllegalArgumentException.class, () -> DroolsBackend.insert(b, session, input)).getMessage();
        } finally {
            session.dispose();
        }
    }

    private static String name(Object o) {
        return o.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(o));
    }
}
