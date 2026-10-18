package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocuments.WHOLE;
import static dev.uchar.regelspraak.cases.TmsDocuments.attribuut;
import static dev.uchar.regelspraak.cases.TmsDocuments.check;
import static dev.uchar.regelspraak.cases.TmsDocuments.factTypeOneToMany;
import static dev.uchar.regelspraak.cases.TmsDocuments.number;
import static dev.uchar.regelspraak.cases.TmsDocuments.instanceWithRoles;
import static dev.uchar.regelspraak.cases.TmsDocuments.characteristicRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.reading;
import static dev.uchar.regelspraak.cases.TmsDocuments.compound;
import static dev.uchar.regelspraak.cases.TmsDocuments.comparison;
import static dev.uchar.regelspraak.cases.TmsDocuments.verwacht;
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
class TmsRoleCheckWitnessTest {

    static final String MODEL = TmsDocuments.model(List.of(
            TmsDocuments.objecttype("Wachtpost", List.of(), List.of("bemand", "onbemand", "strandbemand", "strandvrij")),
            TmsDocuments.objecttype("Jutter", List.of(attribuut("dienstjaren", WHOLE)),
                    List.of("ervaren", "wachtdoend", "vrijgesteld", "inzetbaar")),
            TmsDocuments.subtype("Strandjutter", "Jutter", List.of(), List.of())),
            List.of(factTypeOneToMany("wachtindeling", "toegewezen wachtpost", "Wachtpost", "wachtjutter", "Jutter",
                    "wachtjutters")));

    /** A RoleMembership: the subject plays {@code rol} of {@code feittype}; {@code counterpart} narrows the other end,
     *  null for none. */
    static String roleCheck(String feittype, String rol, String counterpart, boolean negated, String bron) {
        return "{\"node\":\"RoleMembership\",\"factType\":\"" + feittype + "\",\"role\":\"" + rol + "\",\"negated\":"
                + negated + (counterpart == null ? "" : ",\"counterpartType\":\"" + counterpart + "\"")
                + ",\"source\":{\"snippet\":\"" + bron + "\"}}";
    }

    static final String ERVAREN = characteristicRule("ervarenheid", "Jutter", "ervaren",
            comparison("greater", reading("dienstjaren", "Jutter"), number("5")));
    static final String WACHTDOEND = characteristicRule("wachtdienst", "Jutter", "wachtdoend",
            roleCheck("wachtindeling", "wachtjutter", null, false, "hij een wachtjutter is"));
    static final String VRIJGESTELD = characteristicRule("vrijstelling", "Jutter", "vrijgesteld",
            roleCheck("wachtindeling", "wachtjutter", null, true, "hij geen wachtjutter is"));
    static final String INZETBAAR = characteristicRule("inzetbaarheid", "Jutter", "inzetbaar", compound("all", 0, List.of(
            check("ervaren", false), roleCheck("wachtindeling", "wachtjutter", null, false, "hij een wachtjutter is"))));
    static final String BEMAND = characteristicRule("bemanning", "Wachtpost", "bemand",
            roleCheck("wachtindeling", "toegewezen wachtpost", null, false, "hij een wachtjutter heeft"));
    static final String ONBEMAND = characteristicRule("onbemanning", "Wachtpost", "onbemand",
            roleCheck("wachtindeling", "toegewezen wachtpost", null, true, "hij geen wachtjutter heeft"));
    static final String STRANDBEMAND = characteristicRule("strandbemanning", "Wachtpost", "strandbemand",
            roleCheck("wachtindeling", "toegewezen wachtpost", "Strandjutter", false, "hij een strandjutter heeft"));
    static final String STRANDVRIJ = characteristicRule("strandvrijheid", "Wachtpost", "strandvrij",
            roleCheck("wachtindeling", "toegewezen wachtpost", "Strandjutter", true, "hij geen strandjutter heeft"));

    static String post(String label) {
        return instanceWithRoles(label, "Wachtpost", Map.of(), List.of(), Map.of());
    }

    /** A Jutter, or a Strandjutter, with {@code dienstjaren}, at {@code post} where not null. */
    static String jutter(String label, String type, int dienstjaren, String post) {
        return instanceWithRoles(label, type, Map.of("dienstjaren", "" + dienstjaren), List.of(),
                post == null ? Map.of() : Map.of("toegewezen wachtpost", List.of(post)));
    }

    static List<String> world(int n) {
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
    static List<String> nearby(int n) {
        List<String> v = new ArrayList<>();
        switch (n) {
            case 1 -> v.addAll(jutterExpected("J1", true, true));
            case 2 -> {
                v.addAll(jutterExpected("J1", true, true));
                v.addAll(jutterExpected("J2", true, false));
                v.addAll(jutterExpected("J3", false, true));
            }
            case 3 -> {
                v.addAll(jutterExpected("J1", true, true));
                v.addAll(jutterExpected("S1", true, true));
            }
            default -> throw new IllegalArgumentException("wereld " + n);
        }
        return v;
    }

    private static List<String> jutterExpected(String label, boolean wachtdoend, boolean ervaren) {
        return List.of(verwacht(label, "wachtdoend", wachtdoend), verwacht(label, "vrijgesteld", !wachtdoend),
                verwacht(label, "ervaren", ervaren), verwacht(label, "inzetbaar", wachtdoend && ervaren));
    }

    /** The far side per world: per post, bemand and strandbemand, and their complements. */
    static List<String> far(int n) {
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

    @Test void the_nearby_role_check_also_in_a_compound_with_a_later_derived_characteristic_check() throws Exception {
        List<String> outcomes = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> order : TmsDocuments.permutations(List.of(ERVAREN, WACHTDOEND, VRIJGESTELD, INZETBAAR))) {
            List<String> names = TmsDocuments.ruleNames(order);
            Path ir = TmsDocuments.document(MODEL, order);
            for (int n = 1; n <= 3; n++) {
                JsonNode caseDocument = TmsDocuments.caseDocument(world(n), nearby(n));
                outcomes.add(names + " " + n + " " + TmsFaultWitnessTest.playBoth(ir, caseDocument));
                verwacht.add(names + " " + n + " gespeeld");
            }
        }
        assertEquals(verwacht, outcomes);
    }

    @Test void the_remote_role_check_with_and_without_counterpart() throws Exception {
        List<String> outcomes = new ArrayList<>(), verwacht = new ArrayList<>();
        for (List<String> order : TmsDocuments.permutations(List.of(BEMAND, ONBEMAND, STRANDBEMAND, STRANDVRIJ))) {
            List<String> names = TmsDocuments.ruleNames(order);
            Path ir = TmsDocuments.document(MODEL, order);
            for (int n = 1; n <= 3; n++) {
                JsonNode caseDocument = TmsDocuments.caseDocument(world(n), far(n));
                outcomes.add(names + " " + n + " " + TmsFaultWitnessTest.playBoth(ir, caseDocument));
                verwacht.add(names + " " + n + " gespeeld");
            }
        }
        assertEquals(verwacht, outcomes);
    }

    // ---------------- a link a creation lays ----------------

    static final String REPORT_MODEL = TmsDocuments.model(List.of(
            TmsDocuments.objecttype("Jutter", List.of(attribuut("vangst", WHOLE)), List.of("gemeld", "melder", "stil")),
            TmsDocuments.objecttype("Melding", List.of(), List.of())),
            List.of(factTypeOneToMany("jutmelding", "melder", "Jutter", "melding", "Melding", "meldingen")));

    static final String REPORTED = characteristicRule("melding", "Jutter", "gemeld",
            comparison("greater", reading("vangst", "Jutter"), number("3")));
    static final String MELDER = characteristicRule("melderschap", "Jutter", "melder",
            roleCheck("jutmelding", "melder", null, false, "hij een melding heeft"));
    static final String SILENT = characteristicRule("stilte", "Jutter", "stil",
            roleCheck("jutmelding", "melder", null, true, "hij geen melding heeft"));
    static final String MAKE = TmsCreationWitnessTest.objectCreation("maak een melding", "Jutter", "Melding", List.of(),
            check("gemeld", false), "jutmelding", "melding");

    @Test void a_role_check_reads_the_link_that_a_creation_lays() throws Exception {
        List<String> outcomes = new ArrayList<>(), verwacht = new ArrayList<>();
        JsonNode caseDocument = TmsDocuments.caseDocument(List.of(
                instanceWithRoles("J1", "Jutter", Map.of("vangst", "5"), List.of(), Map.of()),
                instanceWithRoles("J2", "Jutter", Map.of("vangst", "1"), List.of(), Map.of())),
                List.of(verwacht("J1", "gemeld", true), verwacht("J1", "melder", true), verwacht("J1", "stil", false),
                        verwacht("J2", "gemeld", false), verwacht("J2", "melder", false), verwacht("J2", "stil", true)));
        for (List<String> order : TmsDocuments.permutations(List.of(REPORTED, MELDER, SILENT))) {
            List<String> names = TmsDocuments.ruleNames(order);
            Path ir = TmsCreationWitnessTest.document(REPORT_MODEL, order, List.of(), List.of(MAKE), List.of(), List.of());
            outcomes.add(names + " " + TmsFaultWitnessTest.playBoth(ir, caseDocument));
            verwacht.add(names + " gespeeld");
        }
        assertEquals(verwacht, outcomes);
    }
}
