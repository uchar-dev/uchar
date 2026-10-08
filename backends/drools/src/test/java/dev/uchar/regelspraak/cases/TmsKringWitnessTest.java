package dev.uchar.regelspraak.cases;

import dev.uchar.regelspraak.graaf.ProducentConsument;
import dev.uchar.regelspraak.loader.IrLoader;
import dev.uchar.regelspraak.transpiler.tms.Kringen;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static dev.uchar.regelspraak.cases.TmsDocumenten.check;
import static dev.uchar.regelspraak.cases.TmsDocumenten.kenmerkregel;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Cycles on the truth-maintenance branch, refused by name before the KieBase is built, in every order of their rules.
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
class TmsKringWitnessTest {

    private static final String REDEN = " — elke kring is onwelgevormd (WF-045; met een ontkenning erop ook WF-046), en"
            + " truth maintenance geeft er geen eenduidig fixpunt voor: een positieve kring houdt zichzelf in stand nadat"
            + " haar ingang vervalt, een even ontkenningskring beslist op de volgorde van de regels in het model, en een"
            + " oneven ontkenningskring wisselt tot het vuurplafond";

    private static final List<String> KENMERKEN = List.of("wachtdoend", "vrijgesteld", "dwarsligger", "afgelost", "paraat");

    @Test void een_even_ontkenningskring_wordt_in_elke_volgorde_geweigerd() throws Exception {
        geweigerd(List.of(kenmerkregel("wachtdoend tenzij vrijgesteld", "wachtdoend", check("vrijgesteld", true)),
                        kenmerkregel("vrijgesteld tenzij wachtdoend", "vrijgesteld", check("wachtdoend", true))),
                "tms: kring in de afhankelijkheden over de leden Jutter.vrijgesteld en Jutter.wachtdoend via de regel"
                        + " \"vrijgesteld tenzij wachtdoend\" en de regel \"wachtdoend tenzij vrijgesteld\"" + REDEN);
    }

    @Test void een_oneven_ontkenningskring_van_lengte_een_wordt_geweigerd() throws Exception {
        geweigerd(List.of(kenmerkregel("dwarsligger tenzij dwarsligger", "dwarsligger", check("dwarsligger", true))),
                "tms: kring in de afhankelijkheden over het lid Jutter.dwarsligger via de regel"
                        + " \"dwarsligger tenzij dwarsligger\"" + REDEN);
    }

    @Test void een_oneven_ontkenningskring_van_lengte_twee_wordt_in_elke_volgorde_geweigerd() throws Exception {
        geweigerd(List.of(kenmerkregel("wachtdoend tenzij afgelost", "wachtdoend", check("afgelost", true)),
                        kenmerkregel("afgelost indien wachtdoend", "afgelost", check("wachtdoend", false))),
                "tms: kring in de afhankelijkheden over de leden Jutter.afgelost en Jutter.wachtdoend via de regel"
                        + " \"afgelost indien wachtdoend\" en de regel \"wachtdoend tenzij afgelost\"" + REDEN);
    }

    @Test void een_positieve_kring_wordt_in_elke_volgorde_geweigerd() throws Exception {
        geweigerd(List.of(kenmerkregel("wachtdoend indien paraat", "wachtdoend", check("paraat", false)),
                        kenmerkregel("paraat indien wachtdoend", "paraat", check("wachtdoend", false))),
                "tms: kring in de afhankelijkheden over de leden Jutter.paraat en Jutter.wachtdoend via de regel"
                        + " \"paraat indien wachtdoend\" en de regel \"wachtdoend indien paraat\"" + REDEN);
    }

    /** The replay, the CLI's route, gives the refusal as its EXCLUDE reason. */
    @Test void de_replay_weigert_in_dezelfde_zin() throws Exception {
        Path ir = TmsDocumenten.document(TmsDocumenten.model(KENMERKEN),
                List.of(kenmerkregel("dwarsligger tenzij dwarsligger", "dwarsligger", check("dwarsligger", true))));
        Path zaak = Files.createTempFile("tms-kring-zaak", ".json");
        zaak.toFile().deleteOnExit();
        Files.writeString(zaak, TmsDocumenten.JSON.writeValueAsString(zaak()));
        CaseRunner.Result r = CaseRunner.viaTruthMaintenance(ir, List.of(zaak));
        assertEquals("EXCLUDE", r.status(), r::toString);
        assertEquals("tms: kring in de afhankelijkheden over het lid Jutter.dwarsligger via de regel"
                + " \"dwarsligger tenzij dwarsligger\"" + REDEN, r.reason());
    }

    // ---------------- the graph: an initialisation is no cycle of length one ----------------

    @Test void een_initialisatie_is_geen_zelflus_en_een_berekening_op_haar_eigen_doel_wel() throws Exception {
        String init = IrPad.regel("standaardbergloon", "INITIALISATIE", bergloon(), getal("25"), "null");
        String initOpGewicht = IrPad.regel("bergloon naar gewicht", "INITIALISATIE", bergloon(),
                plus(eigen("gewicht"), getal("5")), "null");
        String ophogen = IrPad.regel("bergloon ophogen", "BEREKENING", bergloon(), plus(eigen("bergloon"), getal("1")), "null");
        assertNull(kring(List.of(init)));
        assertNull(kring(List.of(initOpGewicht)));
        assertEquals("tms: kring in de afhankelijkheden over het lid Vondst.bergloon via de regel \"bergloon ophogen\""
                + REDEN, kring(List.of(ophogen)));
    }

    // ---------------- helpers ----------------

    /** Every order of {@code regels} is refused by the build in exactly {@code zin}, and so is the replay of a case. */
    private static void geweigerd(List<String> regels, String zin) throws Exception {
        int volgordes = 0;
        for (List<String> volgorde : TmsDocumenten.permutaties(regels)) {
            Path ir = TmsDocumenten.document(TmsDocumenten.model(KENMERKEN), volgorde);
            CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                    () -> DroolsBackend.build(ir, DroolsBackend.Branch.TRUTH_MAINTENANCE), volgorde::toString);
            assertEquals(zin, e.getMessage());
            CaseRunner.Excluded replay = assertThrows(CaseRunner.Excluded.class,
                    () -> CaseRunner.speelCase(ir, zaak(), DroolsBackend.Branch.TRUTH_MAINTENANCE));
            assertEquals(zin, replay.getMessage());
            volgordes++;
        }
        assertEquals(TmsDocumenten.permutaties(regels).size(), volgordes);
    }

    private static com.fasterxml.jackson.databind.JsonNode zaak() throws Exception {
        return TmsDocumenten.zaak(List.of(TmsDocumenten.jutter("J1", List.of())), List.of());
    }

    /** What the refusal reads of a document of one Vondst with a gewicht and a bergloon and {@code regels}. */
    private static String kring(List<String> regels) throws Exception {
        String twee = "{\"node\":\"Number\",\"whole\":false,\"decimals\":2,\"sign\":\"unspecified\"}";
        String model = "{\"objectTypes\":[{\"node\":\"ObjectType\",\"name\":\"Vondst\",\"animate\":false,\"plural\":"
                + "\"Vondsten\",\"attributes\":[{\"node\":\"Attribute\",\"name\":\"gewicht\",\"datatype\":" + twee + "},"
                + "{\"node\":\"Attribute\",\"name\":\"bergloon\",\"datatype\":" + twee + "}],\"characteristics\":[]}],"
                + "\"factTypes\":[],\"parameters\":[],\"domains\":[],\"unitSystems\":[],\"dimensions\":[],\"timelines\":[],"
                + "\"dayKinds\":[]}";
        Path ir = TmsDocumenten.document(model, regels);
        IrLoader.Loaded l = IrLoader.loadAll(TmsDocumenten.JSON.readTree(Files.readString(ir)));
        return Kringen.weigering(l.model(), ProducentConsument.vormen(l.model(), l.prioritizedDefaults(),
                ProducentConsument.wireNavigatie(l)));
    }

    private static final String TWEE = "{\"node\":\"Number\",\"whole\":false,\"decimals\":2,\"sign\":\"unspecified\"}";

    private static String bergloon() {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"bergloon\",\"subjectType\":\"Vondst\",\"source\":"
                + "{\"snippet\":\"Het bergloon van een Vondst\"}}";
    }

    private static String eigen(String attribuut) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"" + attribuut + "\",\"subjectType\":\"Vondst\","
                + "\"source\":{\"snippet\":\"zijn " + attribuut + "\"}}";
    }

    private static String getal(String lexeem) {
        return IrPad.literal(lexeem, TWEE, lexeem);
    }

    private static String plus(String links, String rechts) {
        return "{\"node\":\"Binary\",\"op\":\"plus\",\"left\":" + links + ",\"right\":" + rechts + ",\"type\":" + TWEE
                + ",\"source\":{\"snippet\":\"plus\"}}";
    }
}
