package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocumenten.GEHEEL;
import static dev.uchar.regelspraak.cases.TmsDocumenten.attribuut;
import static dev.uchar.regelspraak.cases.TmsDocumenten.check;
import static dev.uchar.regelspraak.cases.TmsDocumenten.feittypeEenNaarVeel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.getal;
import static dev.uchar.regelspraak.cases.TmsDocumenten.instantieMetRollen;
import static dev.uchar.regelspraak.cases.TmsDocumenten.kenmerkregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.lezing;
import static dev.uchar.regelspraak.cases.TmsDocumenten.samengesteld;
import static dev.uchar.regelspraak.cases.TmsDocumenten.vergelijking;
import static dev.uchar.regelspraak.cases.TmsDocumenten.verwacht;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The role check on the exec-model branch (RUNTIME-SEMANTICS ch. 5.8a): whether a link of the fact type holds
 * the subject in the named role and, where a counterpart type is written, an instance of that type at the other end;
 * negated, whether none does. The worked examples of ch. 5.8a on the Jutterswet: wachtindeling relates the to-many
 * role wachtjutter (Jutter) to the to-one role toegewezen wachtpost (Wachtpost), and a Strandjutter is a Jutter.
 *
 * <p>The near side, the subject's other role to-one, so the link is the subject's own reference field: "Een Jutter is
 * wachtdoend indien hij een wachtjutter is", "vrijgesteld indien hij geen wachtjutter is", and the check in a composite
 * beside a characteristic another rule derives, "inzetbaar indien hij aan alle volgende voorwaarden voldoet: hij is
 * ervaren; hij is een wachtjutter" with "Een Jutter is ervaren indien zijn dienstjaren groter is dan 5", in every order
 * of the four rules. The far side, the other role to-many, so the link is the field of the other end, which names the
 * subject: "Een Wachtpost is bemand indien hij een wachtjutter heeft", onbemand (negated), strandbemand and strandvrij
 * (the same with the counterpart type Strandjutter), in every order of the four rules. World 1: J1, a Jutter, at W1;
 * W2 empty. World 2: J1 (8 dienstjaren) and J2, a Strandjutter (3 dienstjaren), at W1; J3 (9 dienstjaren) at no
 * post; W2 empty. World 3: J1 at W1 and S1, a Strandjutter, at W2. Two links read as one (rule 1), the counterpart is
 * a class test (rule 2: W1 of world 3 is bemand and strandvrij), and the negation is the plain complement (rule 4).
 *
 * <p>A link a creation lays: jutmelding relates the to-one role melder (Jutter) to the to-many role melding (Melding),
 * and "maak een melding" makes a Melding for every gemeld Jutter over the connecting role melding, where "Een Jutter is
 * gemeld indien zijn vangst groter is dan 3". "Een Jutter is melder indien hij een melding heeft" and "stil indien hij
 * geen melding heeft" read that relation, in every order of the three rules: J1 (vangst 5) is melder and not stil, J2
 * (vangst 1) stil. Before the creation fires J1 holds no link, so in the rule layer's first rest he is stil and not
 * melder; the creation's link turns both.
 *
 * <p><b>Measured</b> (2026-10-09, on the documents of this class, every order; the verifier passes all 150): the
 * reference interpreter's runner and the ASP runner pass every case in every order.
 *
 * <p><b>Red</b> on 695bf30a: all three tests, every play "Excluded: tms-transpiler SKIP: regel: … — een
 * RoleMembership in de voorwaarde ('…'): buiten de proef van de truth-maintenance-tak".
 */
class TmsRolcontroleWitnessTest {

    static final String MODEL = TmsDocumenten.model(List.of(
            TmsDocumenten.objecttype("Wachtpost", List.of(), List.of("bemand", "onbemand", "strandbemand", "strandvrij")),
            TmsDocumenten.objecttype("Jutter", List.of(attribuut("dienstjaren", GEHEEL)),
                    List.of("ervaren", "wachtdoend", "vrijgesteld", "inzetbaar")),
            TmsDocumenten.subtype("Strandjutter", "Jutter", List.of(), List.of())),
            List.of(feittypeEenNaarVeel("wachtindeling", "toegewezen wachtpost", "Wachtpost", "wachtjutter", "Jutter",
                    "wachtjutters")));

    /** A RoleMembership: the subject plays {@code rol} of {@code feittype}; {@code tegenpartij} narrows the other end,
     *  null for none. */
    static String rolcontrole(String feittype, String rol, String tegenpartij, boolean ontkend, String bron) {
        return "{\"node\":\"RoleMembership\",\"factType\":\"" + feittype + "\",\"role\":\"" + rol + "\",\"negated\":"
                + ontkend + (tegenpartij == null ? "" : ",\"counterpartType\":\"" + tegenpartij + "\"")
                + ",\"source\":{\"snippet\":\"" + bron + "\"}}";
    }

    static final String ERVAREN = kenmerkregel("ervarenheid", "Jutter", "ervaren",
            vergelijking("greater", lezing("dienstjaren", "Jutter"), getal("5")));
    static final String WACHTDOEND = kenmerkregel("wachtdienst", "Jutter", "wachtdoend",
            rolcontrole("wachtindeling", "wachtjutter", null, false, "hij een wachtjutter is"));
    static final String VRIJGESTELD = kenmerkregel("vrijstelling", "Jutter", "vrijgesteld",
            rolcontrole("wachtindeling", "wachtjutter", null, true, "hij geen wachtjutter is"));
    static final String INZETBAAR = kenmerkregel("inzetbaarheid", "Jutter", "inzetbaar", samengesteld("all", 0, List.of(
            check("ervaren", false), rolcontrole("wachtindeling", "wachtjutter", null, false, "hij een wachtjutter is"))));
    static final String BEMAND = kenmerkregel("bemanning", "Wachtpost", "bemand",
            rolcontrole("wachtindeling", "toegewezen wachtpost", null, false, "hij een wachtjutter heeft"));
    static final String ONBEMAND = kenmerkregel("onbemanning", "Wachtpost", "onbemand",
            rolcontrole("wachtindeling", "toegewezen wachtpost", null, true, "hij geen wachtjutter heeft"));
    static final String STRANDBEMAND = kenmerkregel("strandbemanning", "Wachtpost", "strandbemand",
            rolcontrole("wachtindeling", "toegewezen wachtpost", "Strandjutter", false, "hij een strandjutter heeft"));
    static final String STRANDVRIJ = kenmerkregel("strandvrijheid", "Wachtpost", "strandvrij",
            rolcontrole("wachtindeling", "toegewezen wachtpost", "Strandjutter", true, "hij geen strandjutter heeft"));

    static String post(String label) {
        return instantieMetRollen(label, "Wachtpost", Map.of(), List.of(), Map.of());
    }

    /** A Jutter, or a Strandjutter, with {@code dienstjaren}, at {@code post} where not null. */
    static String jutter(String label, String type, int dienstjaren, String post) {
        return instantieMetRollen(label, type, Map.of("dienstjaren", "" + dienstjaren), List.of(),
                post == null ? Map.of() : Map.of("toegewezen wachtpost", List.of(post)));
    }

    static List<String> wereld(int n) {
        return switch (n) {
            case 1 -> List.of(post("W1"), post("W2"), jutter("J1", "Jutter", 8, "W1"));
            case 2 -> List.of(post("W1"), post("W2"), jutter("J1", "Jutter", 8, "W1"),
                    jutter("J2", "Strandjutter", 3, "W1"), jutter("J3", "Jutter", 9, null));
            case 3 -> List.of(post("W1"), post("W2"), jutter("J1", "Jutter", 8, "W1"),
                    jutter("S1", "Strandjutter", 6, "W2"));
            default -> throw new IllegalArgumentException("wereld " + n);
        };
    }

    /** The near side per world: per Jutter, wachtdoend, vrijgesteld and inzetbaar. */
    static List<String> dichtbij(int n) {
        List<String> v = new ArrayList<>();
        switch (n) {
            case 1 -> v.addAll(jutterVerwacht("J1", true, true));
            case 2 -> {
                v.addAll(jutterVerwacht("J1", true, true));
                v.addAll(jutterVerwacht("J2", true, false));
                v.addAll(jutterVerwacht("J3", false, true));
            }
            case 3 -> {
                v.addAll(jutterVerwacht("J1", true, true));
                v.addAll(jutterVerwacht("S1", true, true));
            }
            default -> throw new IllegalArgumentException("wereld " + n);
        }
        return v;
    }

    private static List<String> jutterVerwacht(String label, boolean wachtdoend, boolean ervaren) {
        return List.of(verwacht(label, "wachtdoend", wachtdoend), verwacht(label, "vrijgesteld", !wachtdoend),
                verwacht(label, "ervaren", ervaren), verwacht(label, "inzetbaar", wachtdoend && ervaren));
    }

    /** The far side per world: per post, bemand and strandbemand, and their complements. */
    static List<String> veraf(int n) {
        boolean[][] bemanning = switch (n) {
            case 1 -> new boolean[][]{{true, false}, {false, false}};
            case 2 -> new boolean[][]{{true, true}, {false, false}};
            case 3 -> new boolean[][]{{true, false}, {true, true}};
            default -> throw new IllegalArgumentException("wereld " + n);
        };
        List<String> v = new ArrayList<>();
        for (int p = 0; p < 2; p++) {
            String label = "W" + (p + 1);
            v.add(verwacht(label, "bemand", bemanning[p][0]));
            v.add(verwacht(label, "onbemand", !bemanning[p][0]));
            v.add(verwacht(label, "strandbemand", bemanning[p][1]));
            v.add(verwacht(label, "strandvrij", !bemanning[p][1]));
        }
        return v;
    }

    @Test void de_nabije_rolcontrole_ook_in_een_samenstelling_met_een_later_afgeleid_kenmerk() throws Exception {
        List<String> uitkomsten = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> volgorde : TmsDocumenten.permutaties(List.of(ERVAREN, WACHTDOEND, VRIJGESTELD, INZETBAAR))) {
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            Path ir = TmsDocumenten.document(MODEL, volgorde);
            for (int n = 1; n <= 3; n++) {
                JsonNode zaak = TmsDocumenten.zaak(wereld(n), dichtbij(n));
                uitkomsten.add(namen + " " + n + " " + TmsFoutWitnessTest.speelBeide(ir, zaak));
                verwacht.add(namen + " " + n + " gespeeld");
            }
        }
        assertEquals(verwacht, uitkomsten);
    }

    @Test void de_verre_rolcontrole_met_en_zonder_tegenpartij() throws Exception {
        List<String> uitkomsten = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> volgorde : TmsDocumenten.permutaties(List.of(BEMAND, ONBEMAND, STRANDBEMAND, STRANDVRIJ))) {
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            Path ir = TmsDocumenten.document(MODEL, volgorde);
            for (int n = 1; n <= 3; n++) {
                JsonNode zaak = TmsDocumenten.zaak(wereld(n), veraf(n));
                uitkomsten.add(namen + " " + n + " " + TmsFoutWitnessTest.speelBeide(ir, zaak));
                verwacht.add(namen + " " + n + " gespeeld");
            }
        }
        assertEquals(verwacht, uitkomsten);
    }

    // ---------------- a link a creation lays ----------------

    static final String MELDMODEL = TmsDocumenten.model(List.of(
            TmsDocumenten.objecttype("Jutter", List.of(attribuut("vangst", GEHEEL)), List.of("gemeld", "melder", "stil")),
            TmsDocumenten.objecttype("Melding", List.of(), List.of())),
            List.of(feittypeEenNaarVeel("jutmelding", "melder", "Jutter", "melding", "Melding", "meldingen")));

    static final String GEMELD = kenmerkregel("melding", "Jutter", "gemeld",
            vergelijking("greater", lezing("vangst", "Jutter"), getal("3")));
    static final String MELDER = kenmerkregel("melderschap", "Jutter", "melder",
            rolcontrole("jutmelding", "melder", null, false, "hij een melding heeft"));
    static final String STIL = kenmerkregel("stilte", "Jutter", "stil",
            rolcontrole("jutmelding", "melder", null, true, "hij geen melding heeft"));
    static final String MAAK = TmsCreatieWitnessTest.objectcreatie("maak een melding", "Jutter", "Melding", List.of(),
            check("gemeld", false), "jutmelding", "melding");

    @Test void een_rolcontrole_leest_de_link_die_een_creatie_legt() throws Exception {
        List<String> uitkomsten = new ArrayList<>(), verwacht = new ArrayList<>();
        JsonNode zaak = TmsDocumenten.zaak(List.of(
                instantieMetRollen("J1", "Jutter", Map.of("vangst", "5"), List.of(), Map.of()),
                instantieMetRollen("J2", "Jutter", Map.of("vangst", "1"), List.of(), Map.of())),
                List.of(verwacht("J1", "gemeld", true), verwacht("J1", "melder", true), verwacht("J1", "stil", false),
                        verwacht("J2", "gemeld", false), verwacht("J2", "melder", false), verwacht("J2", "stil", true)));
        for (List<String> volgorde : TmsDocumenten.permutaties(List.of(GEMELD, MELDER, STIL))) {
            List<String> namen = TmsDocumenten.regelnamen(volgorde);
            Path ir = TmsCreatieWitnessTest.document(MELDMODEL, volgorde, List.of(), List.of(MAAK), List.of(), List.of());
            uitkomsten.add(namen + " " + TmsFoutWitnessTest.speelBeide(ir, zaak));
            verwacht.add(namen + " gespeeld");
        }
        assertEquals(verwacht, uitkomsten);
    }
}
