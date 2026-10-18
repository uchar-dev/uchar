package dev.uchar.regelspraak.cases;

import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.transpiler.tms.Cycles;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static dev.uchar.regelspraak.cases.TmsDocuments.check;
import static dev.uchar.regelspraak.cases.TmsDocuments.characteristicRule;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Cycles on the exec-model branch, refused by name before the KieBase is built, in every order of their rules.
 * An even cycle of negations: "Een Jutter is wachtdoend indien hij niet vrijgesteld is" beside "Een Jutter is
 * vrijgesteld indien hij niet wachtdoend is". An odd one of length one: "Een Jutter is dwarsligger indien hij niet
 * dwarsligger is". An odd one of length two: "Een Jutter is wachtdoend indien hij niet afgelost is" beside "Een Jutter is
 * afgelost indien hij wachtdoend is". And a positive one: "Een Jutter is wachtdoend indien hij paraat is" beside "Een
 * Jutter is paraat indien hij wachtdoend is". Each is ill-formed (WF-045, and with a negation on it WF-046) and the
 * verifier refuses it, but this backend does not run the verifier, so a hand-written document brings it here. Truth
 * maintenance settles none of them into one answer: the even cycle is stable both ways and the rule first in the model
 * decides, the odd ones alternate or end empty, and the positive one keeps itself once derived. The sentence names the
 * members and the rules of the cycle, sorted, so every order gives the same one.
 *
 * <p>Beside that, the graph the refusal reads: an initialisation draws no edge from its own target (its emptiness test
 * reads the input, RUNTIME-SEMANTICS ch. 5.4 and 7.3), so it is no cycle of length one; a computation that reads its
 * own target is.
 *
 * <p><b>Red before the refusal</b> (measured 2026-10-08). On the branch's skeleton the five cycle rows got the
 * manifest's sentence instead of the cycle's ("but was: &lt;tms-transpiler SKIP: regel: dwarsligger tenzij dwarsligger —
 * de verlaging van een kenmerktoekenning is nog niet gebouwd&gt;"). With the lowering in place and no refusal, nothing
 * was thrown ("Expected …CaseRunner.Excluded to be thrown, but nothing was thrown"), and a probe on J1 without input
 * showed what each cycle did: the even one played in three firings and the order decided, wachtdoend with "wachtdoend
 * tenzij vrijgesteld" first and vrijgesteld with the other rule first; the odd one of length one and the one of length
 * two, in both orders, fired 100000 times and the replay answered "case: geen fixpunt (kringverwijzing?)"; the positive
 * one played the least fixpoint from the input paraat, and when paraat was taken off the bean in the running session
 * (an update and a second fire) both characteristics stayed, each the other's justification. The graph row on the
 * initialisation was green from the start.
 */
class TmsCycleWitnessTest {

    private static final String REASON = " — een kring die niet door een creatielid van een recursieve regelgroep loopt,"
            + " is onwelgevormd (WF-045), en truth maintenance geeft er geen eenduidig fixpunt voor: een positieve kring"
            + " houdt zichzelf in stand nadat haar ingang vervalt, een even ontkenningskring beslist op de volgorde van de"
            + " regels in het model, en een oneven ontkenningskring wisselt tot het vuurplafond";

    private static final List<String> CHARACTERISTICS = List.of("wachtdoend", "vrijgesteld", "dwarsligger", "afgelost", "paraat");

    @Test void a_negation_cycle_with_paired_negations_is_refused_in_every_order() throws Exception {
        refused(List.of(characteristicRule("wachtdoend tenzij vrijgesteld", "wachtdoend", check("vrijgesteld", true)),
                        characteristicRule("vrijgesteld tenzij wachtdoend", "vrijgesteld", check("wachtdoend", true))),
                "exec-model: kring in de afhankelijkheden over de leden Jutter.vrijgesteld en Jutter.wachtdoend via de regel"
                        + " \"vrijgesteld tenzij wachtdoend\" en de regel \"wachtdoend tenzij vrijgesteld\"" + REASON);
    }

    @Test void a_oneven_negation_cycle_of_length_one_is_refused() throws Exception {
        refused(List.of(characteristicRule("dwarsligger tenzij dwarsligger", "dwarsligger", check("dwarsligger", true))),
                "exec-model: kring in de afhankelijkheden over het lid Jutter.dwarsligger via de regel"
                        + " \"dwarsligger tenzij dwarsligger\"" + REASON);
    }

    @Test void an_oneven_negation_cycle_of_length_two_is_refused_in_every_order() throws Exception {
        refused(List.of(characteristicRule("wachtdoend tenzij afgelost", "wachtdoend", check("afgelost", true)),
                        characteristicRule("afgelost indien wachtdoend", "afgelost", check("wachtdoend", false))),
                "exec-model: kring in de afhankelijkheden over de leden Jutter.afgelost en Jutter.wachtdoend via de regel"
                        + " \"afgelost indien wachtdoend\" en de regel \"wachtdoend tenzij afgelost\"" + REASON);
    }

    @Test void a_positive_cycle_is_refused_in_every_order() throws Exception {
        refused(List.of(characteristicRule("wachtdoend indien paraat", "wachtdoend", check("paraat", false)),
                        characteristicRule("paraat indien wachtdoend", "paraat", check("wachtdoend", false))),
                "exec-model: kring in de afhankelijkheden over de leden Jutter.paraat en Jutter.wachtdoend via de regel"
                        + " \"paraat indien wachtdoend\" en de regel \"wachtdoend indien paraat\"" + REASON);
    }

    /** The replay, the CLI's route, gives the refusal as its EXCLUDE reason. */
    @Test void the_replay_refuses_in_the_same_sentence() throws Exception {
        Path ir = TmsDocuments.document(TmsDocuments.model(CHARACTERISTICS),
                List.of(characteristicRule("dwarsligger tenzij dwarsligger", "dwarsligger", check("dwarsligger", true))));
        Path caseDocument = Files.createTempFile("tms-kring-zaak", ".json");
        caseDocument.toFile().deleteOnExit();
        Files.writeString(caseDocument, TmsDocuments.JSON.writeValueAsString(caseDocument()));
        CaseRunner.Result r = CaseRunner.viaExecModel(ir, List.of(caseDocument));
        assertEquals("EXCLUDE", r.status(), r::toString);
        assertEquals("exec-model: kring in de afhankelijkheden over het lid Jutter.dwarsligger via de regel"
                + " \"dwarsligger tenzij dwarsligger\"" + REASON, r.reason());
    }

    // ---------------- the graph: an initialisation is no cycle of length one ----------------

    @Test void an_initialisation_is_no_self_loop_and_a_computation_on_its_own_target_indeed() throws Exception {
        String init = IrPath.regel("standaardbergloon", "INITIALISATIE", bergloon(), number("25"), "null");
        String initByWeight = IrPath.regel("bergloon naar gewicht", "INITIALISATIE", bergloon(),
                plus(own("gewicht"), number("5")), "null");
        String increment = IrPath.regel("bergloon ophogen", "BEREKENING", bergloon(), plus(own("bergloon"), number("1")), "null");
        assertNull(cycle(List.of(init)));
        assertNull(cycle(List.of(initByWeight)));
        assertEquals("exec-model: kring in de afhankelijkheden over het lid Vondst.bergloon via de regel \"bergloon ophogen\""
                + REASON, cycle(List.of(increment)));
    }

    // ---------------- helpers ----------------

    /** Every order of {@code rules} is refused by the build in exactly {@code sentence}, and so is the replay of a case. */
    private static void refused(List<String> rules, String sentence) throws Exception {
        int orders = 0;
        for (List<String> order : TmsDocuments.permutations(rules)) {
            Path ir = TmsDocuments.document(TmsDocuments.model(CHARACTERISTICS), order);
            CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                    () -> DroolsBackend.build(ir, DroolsBackend.Branch.EXEC_MODEL), order::toString);
            assertEquals(sentence, e.getMessage());
            CaseRunner.Excluded replay = assertThrows(CaseRunner.Excluded.class,
                    () -> CaseRunner.playCase(ir, caseDocument(), DroolsBackend.Branch.EXEC_MODEL));
            assertEquals(sentence, replay.getMessage());
            orders++;
        }
        assertEquals(TmsDocuments.permutations(rules).size(), orders);
    }

    private static com.fasterxml.jackson.databind.JsonNode caseDocument() throws Exception {
        return TmsDocuments.caseDocument(List.of(TmsDocuments.jutter("J1", List.of())), List.of());
    }

    /** What the refusal reads of a document of one Vondst with a gewicht and a bergloon and {@code rules}. */
    private static String cycle(List<String> rules) throws Exception {
        String two = "{\"node\":\"Number\",\"whole\":false,\"decimals\":2,\"sign\":\"unspecified\"}";
        String model = "{\"objectTypes\":[{\"node\":\"ObjectType\",\"name\":\"Vondst\",\"animate\":false,\"plural\":"
                + "\"Vondsten\",\"attributes\":[{\"node\":\"Attribute\",\"name\":\"gewicht\",\"datatype\":" + two + "},"
                + "{\"node\":\"Attribute\",\"name\":\"bergloon\",\"datatype\":" + two + "}],\"characteristics\":[]}],"
                + "\"factTypes\":[],\"parameters\":[],\"domains\":[],\"unitSystems\":[],\"dimensions\":[],\"timelines\":[],"
                + "\"dayKinds\":[]}";
        Path ir = TmsDocuments.document(model, rules);
        return Cycles.refusal(IrLoader.loadAll(TmsDocuments.JSON.readTree(Files.readString(ir))));
    }

    private static final String TWO = "{\"node\":\"Number\",\"whole\":false,\"decimals\":2,\"sign\":\"unspecified\"}";

    private static String bergloon() {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"bergloon\",\"subjectType\":\"Vondst\",\"source\":"
                + "{\"snippet\":\"Het bergloon van een Vondst\"}}";
    }

    private static String own(String attribuut) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribuut + "\",\"subjectType\":\"Vondst\","
                + "\"source\":{\"snippet\":\"zijn " + attribuut + "\"}}";
    }

    private static String number(String lexeme) {
        return IrPath.literal(lexeme, TWO, lexeme);
    }

    private static String plus(String left, String right) {
        return "{\"node\":\"Binary\",\"op\":\"plus\",\"left\":" + left + ",\"right\":" + right + ",\"type\":" + TWO
                + ",\"source\":{\"snippet\":\"plus\"}}";
    }
}
