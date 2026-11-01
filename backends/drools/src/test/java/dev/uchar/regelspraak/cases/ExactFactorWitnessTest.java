package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A declared factor without a finite decimal stays an exact fraction up to the collapse point (IR-SPEC ch. 2.5,
 * RUNTIME-SEMANTICS ch. 2.1, 2.6), at every site that restates between two units, in both branches. The corpus does not
 * hold such a factor, because the ASP backend refuses an additive join over two units by name (ch. 2.6, ASP-TRANSLATION
 * S19) and gate 6 has no list of refusals; a verifier-clean document the frontend emits does, and each value below lands
 * exactly on a rounding boundary, where a factor rounded to any number of digits falls on the wrong side. Every expected
 * value is what {@code ref} answers on the same document and case.
 */
class ExactFactorWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /**
     * The factor 1/3. Source:
     * <pre>
     * Eenheidsysteem Wachtrooster
     *     de dienst (mv: diensten) dn
     *     de wacht (mv: wachten) wa = 1/3 dn
     * Objecttype de Jutter (mv: Jutters) (bezield)
     *     is gedekt kenmerk (bijvoeglijk);
     *     is ruim kenmerk (bijvoeglijk);
     *     de dienstduur Numeriek (getal met 2 decimalen) met eenheid dn;
     *     de wachtduur Numeriek (getal met 2 decimalen) met eenheid wa;
     *     de inzet Numeriek (getal met 2 decimalen) met eenheid dn;
     *     de afgeronde inzet Numeriek (geheel getal) met eenheid dn;
     *     de omgerekende deel Numeriek (getal met 2 decimalen) met eenheid dn;
     *     de omgerekende basis Numeriek (getal met 2 decimalen) met eenheid wa;
     *     de begrensde basis Numeriek (getal met 2 decimalen) met eenheid dn;
     *     de grootste Numeriek (getal met 2 decimalen) met eenheid dn;
     *     het tarief Numeriek (getal met 2 decimalen) met eenheid 1/dn;
     *     het afgeronde product Numeriek (geheel getal);
     *     het afgeronde quotient Numeriek (geheel getal);
     * Regel inzet: … als de dienstduur van de Jutter plus de wachtduur van de Jutter.
     * Regel afgeronde inzet: … als de dienstduur van de Jutter plus de wachtduur van de Jutter
     *     naar beneden afgerond op 0 decimalen.
     * Regel omgerekende deel: … als de wachtduur van de Jutter.
     * Regel omgerekende basis: … als de dienstduur van de Jutter.
     * Regel begrensde basis: … als de dienstduur van de Jutter, met een minimum van de wachtduur van de Jutter.
     * Regel grootste: … als de maximale waarde van de dienstduur van de Jutter en de wachtduur van de Jutter.
     * Regel afgeronde product: … als het tarief van de Jutter maal de wachtduur van de Jutter
     *     naar beneden afgerond op 0 decimalen.
     * Regel afgeronde quotient: … als de dienstduur van de Jutter gedeeld door de wachtduur van de Jutter
     *     naar boven afgerond op 0 decimalen.
     * Regel gedekt: Een Jutter is gedekt indien zijn wachtduur gelijk is aan zijn dienstduur.
     * Regel ruim: Een Jutter is ruim indien zijn wachtduur groter is dan zijn dienstduur.
     * </pre>
     * 3.00 wa is 1 dn exactly, and 1 dn is 3 wa.
     */
    @Test void the_factor_one_third_is_exact_in_both_branches() throws Exception {
        Path ir = document("Wachtrooster", "dn", "dienst", "wa", "wacht", 3, "dienstduur", "wachtduur", "inzet",
                "afgeronde inzet", "floor", "naar beneden", "ceiling", "naar boven");
        BothBranches.green(ir, caseDocument("dienstduur", "1.00", "wachtduur", "3.00", "6.00",
                Map.of("inzet", "2.00", "afgeronde inzet", "2", "omgerekende deel", "1", "omgerekende basis", "3",
                        "begrensde basis", "1", "grootste", "1", "afgeronde product", "6", "afgeronde quotient", "1"),
                List.of("gedekt"), List.of("ruim")), "1.00 dn tegen 3.00 wa");
        BothBranches.green(ir, caseDocument("dienstduur", "0.50", "wachtduur", "3.00", "6.00",
                Map.of("inzet", "1.50", "afgeronde inzet", "1", "omgerekende deel", "1", "omgerekende basis", "1.5",
                        "begrensde basis", "1", "grootste", "1", "afgeronde product", "6", "afgeronde quotient", "1"),
                List.of("ruim"), List.of("gedekt")), "0.50 dn tegen 3.00 wa");
    }

    /**
     * The factor 1/24, a factor the parity zone declares for its hour. Source as above, with
     * {@code Eenheidsysteem Getijrooster}, {@code het tij (mv: tijen) tj}, {@code de slag (mv: slagen) sl = 1/24 tj}, the
     * attributes {@code tijduur} (tj), {@code slagduur} (sl), {@code looptijd} (tj, 2 decimals) and
     * {@code afgeronde looptijd} (tj, whole), the rules for the sum and the product rounding {@code naar boven} and the
     * quotient {@code naar beneden}. 216.00 sl is 9 tj exactly.
     */
    @Test void the_factor_one_twenty_fourth_is_exact_in_both_branches() throws Exception {
        Path ir = document("Getijrooster", "tj", "tij", "sl", "slag", 24, "tijduur", "slagduur", "looptijd",
                "afgeronde looptijd", "ceiling", "naar boven", "floor", "naar beneden");
        BothBranches.green(ir, caseDocument("tijduur", "0.00", "slagduur", "216.00", "2.00",
                Map.of("looptijd", "9.00", "afgeronde looptijd", "9", "omgerekende deel", "9", "omgerekende basis", "0",
                        "begrensde basis", "9", "grootste", "9", "afgeronde product", "18", "afgeronde quotient", "0"),
                List.of("ruim"), List.of("gedekt")), "0.00 tj tegen 216.00 sl");
        BothBranches.green(ir, caseDocument("tijduur", "9.00", "slagduur", "216.00", "2.00",
                Map.of("looptijd", "18.00", "afgeronde looptijd", "18", "omgerekende deel", "9", "omgerekende basis", "216",
                        "begrensde basis", "9", "grootste", "9", "afgeronde product", "18", "afgeronde quotient", "1"),
                List.of("gedekt"), List.of("ruim")), "9.00 tj tegen 216.00 sl");
    }

    /**
     * A restated value with no finite decimal, which stays a fraction up to the rounding. Source: the unit system and the
     * two addends of the first row, and
     * <pre>
     * Objecttype de Jutter (mv: Jutters) (bezield)
     *     is gedekt kenmerk (bijvoeglijk);
     *     de afgeronde som Numeriek (geheel getal) met eenheid dn;
     *     de afgeronde klem Numeriek (geheel getal) met eenheid dn;
     * Regel afgeronde som: … als de dienstduur van de Jutter plus de wachtduur van de Jutter plus de wachtduur van de
     *     Jutter plus de wachtduur van de Jutter naar beneden afgerond op 0 decimalen.
     * Regel afgeronde klem: … als (de dienstduur van de Jutter, met een minimum van de wachtduur van de Jutter) maal 3
     *     naar beneden afgerond op 0 decimalen.
     * Regel gedekt: Een Jutter is gedekt indien zijn dienstduur plus zijn wachtduur plus zijn wachtduur plus zijn
     *     wachtduur gelijk is aan 1 dn.
     * </pre>
     * 1.00 wa is 1/3 dn, and three of it are 1 dn.
     */
    @Test void a_restated_value_without_a_finite_decimal_stays_exact_up_to_the_rounding() throws Exception {
        Path ir = chainDocument();
        ObjectNode caseDocument = JSON.createObjectNode();
        caseDocument.put("format", "uchar-case-v0");
        caseDocument.putNull("asOfDate");
        ObjectNode instance = caseDocument.putObject("input").putArray("instances").addObject();
        caseDocument.withObject("/input").putObject("parameters");
        instance.put("label", "J1").put("objectType", "Jutter");
        ObjectNode attributes = instance.putObject("attributes");
        attributes.putObject("dienstduur").put("number", "0.00");
        attributes.putObject("wachtduur").put("number", "1.00");
        instance.putObject("roles");
        instance.putArray("characteristics");
        ArrayNode expected = caseDocument.putArray("expected");
        for (String attribute : List.of("afgeronde som", "afgeronde klem")) {
            ObjectNode e = expected.addObject().put("kind", "attributeValue").put("label", "J1").put("attribute", attribute);
            e.putObject("value").put("number", "1");
        }
        expected.addObject().put("kind", "characteristicPresent").put("label", "J1").put("characteristic", EQUAL);
        expected.addObject().put("kind", "consistent");
        BothBranches.green(ir, caseDocument, "1.00 wa is 1/3 dn");
    }

    // ---------------- the documents the frontend emits for the three sources, with their names ----------------

    private static final String TRUE = "{\"node\":\"Literal\",\"value\":{\"boolean\":true},\"type\":{\"node\":\"Boolean\"},\"source\":{\"snippet\":\"waar\"}}";

    private static final String RESTATED_PART = "omgerekende deel", RESTATED_BASE = "omgerekende basis",
            CLAMPED = "begrensde basis", GREATEST = "grootste", RATE = "tarief", PRODUCT = "afgeronde product",
            QUOTIENT = "afgeronde quotient", EQUAL = "gedekt", GREATER = "ruim";

    /** The emitted document: one unit system of a base unit {@code base} and a unit {@code part} = 1/{@code denominator}
     *  of it; a Jutter with the two addends and one target per site that restates; a rule per target. */
    private static Path document(String system, String base, String baseWord, String part, String partWord,
                                 int denominator, String left, String right, String sum, String rounded,
                                 String mode, String modeSource, String otherMode, String otherModeSource) throws Exception {
        String baseUnit = unit(system, base, base, 1, "");
        String partUnit = unit(system, part, base, denominator, "");
        String baseType = number(false, 2, baseUnit);
        String partType = number(false, 2, partUnit);
        String wholeBase = number(true, 0, baseUnit);
        String whole = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\"}";
        String l = ownRead(left), r = ownRead(right);
        String of = "de " + left + " van de Jutter", ofRight = "de " + right + " van de Jutter";

        String addition = ("{\"node\":\"Binary\",\"op\":\"plus\",\"left\":%s,\"right\":%s,\"type\":%s,"
                + "\"source\":{\"snippet\":\"%s plus %s\"}}").formatted(l, r, baseType, of, ofRight);
        String clamp = ("{\"node\":\"Clamp\",\"base\":%s,\"minimum\":%s,\"type\":%s,"
                + "\"source\":{\"snippet\":\"%s, met een minimum van %s\"}}").formatted(l, r, baseType, of, ofRight);
        String greatest = ("{\"node\":\"Aggregation\",\"kind\":\"maximum\",\"arguments\":[%s,%s],\"elementType\":\"Jutter\","
                + "\"type\":%s,\"source\":{\"snippet\":\"de maximale waarde van %s en %s\"}}").formatted(l, r, baseType, of, ofRight);
        String product = ("{\"node\":\"Binary\",\"op\":\"times\",\"left\":%s,\"right\":%s,"
                + "\"type\":{\"node\":\"Number\",\"whole\":false,\"decimals\":4,\"sign\":\"unspecified\"},"
                + "\"source\":{\"snippet\":\"het %s van de Jutter maal %s\"}}").formatted(read(RATE, "het " + RATE + " van de Jutter"), r, RATE, ofRight);
        String quotient = ("{\"node\":\"Binary\",\"op\":\"dividedBy\",\"left\":%s,\"right\":%s,"
                + "\"type\":{\"node\":\"Number\",\"whole\":false,\"decimals\":2,\"sign\":\"unspecified\"},"
                + "\"source\":{\"snippet\":\"%s gedeeld door %s\"}}").formatted(l, r, of, ofRight);

        String objectType = ("{\"node\":\"ObjectType\",\"name\":\"Jutter\",\"animate\":true,\"plural\":\"Jutters\","
                + "\"attributes\":[%s],\"characteristics\":[{\"node\":\"Characteristic\",\"name\":\"%s\",\"kind\":\"adjectival\"},"
                + "{\"node\":\"Characteristic\",\"name\":\"%s\",\"kind\":\"adjectival\"}]}").formatted(String.join(",",
                attribute(left, baseType), attribute(right, partType), attribute(sum, baseType), attribute(rounded, wholeBase),
                attribute(RESTATED_PART, baseType), attribute(RESTATED_BASE, partType), attribute(CLAMPED, baseType),
                attribute(GREATEST, baseType), attribute(RATE, number(false, 2, unit(system, base, base, 1, ",\"exponent\":-1"))),
                attribute(PRODUCT, whole), attribute(QUOTIENT, whole)), EQUAL, GREATER);
        String units = "[" + declared(baseUnit, baseWord) + "," + declared(partUnit, partWord) + "]";
        String rules = String.join(",", rule(sum, addition),
                rule(rounded, rounding(addition, mode, wholeBase, of + " plus " + ofRight, modeSource)),
                rule(RESTATED_PART, r), rule(RESTATED_BASE, l), rule(CLAMPED, clamp), rule(GREATEST, greatest),
                rule(PRODUCT, rounding(product, mode, whole, "het " + RATE + " van de Jutter maal " + ofRight, modeSource)),
                rule(QUOTIENT, rounding(quotient, otherMode, whole, of + " gedeeld door " + ofRight, otherModeSource)),
                characteristicRule(EQUAL, "equal", right, left, "gelijk is aan"),
                characteristicRule(GREATER, "greater", right, left, "groter is dan"));
        return written(objectType, system, units, rules);
    }

    private static Path chainDocument() throws Exception {
        String system = "Wachtrooster", left = "dienstduur", right = "wachtduur";
        String baseUnit = unit(system, "dn", "dn", 1, ""), partUnit = unit(system, "wa", "dn", 3, "");
        String baseType = number(false, 2, baseUnit), wholeBase = number(true, 0, baseUnit);
        String objectType = ("{\"node\":\"ObjectType\",\"name\":\"Jutter\",\"animate\":true,\"plural\":\"Jutters\","
                + "\"attributes\":[%s],\"characteristics\":[{\"node\":\"Characteristic\",\"name\":\"%s\",\"kind\":\"adjectival\"}]}")
                .formatted(String.join(",", attribute(left, baseType), attribute(right, number(false, 2, partUnit)),
                        attribute("afgeronde som", wholeBase), attribute("afgeronde klem", wholeBase)), EQUAL);
        String of = "de " + left + " van de Jutter", ofRight = "de " + right + " van de Jutter";
        String clamp = ("{\"node\":\"Clamp\",\"base\":%s,\"minimum\":%s,\"type\":%s,"
                + "\"source\":{\"snippet\":\"%s, met een minimum van %s\"}}").formatted(ownRead(left), ownRead(right), baseType, of, ofRight);
        String timesThree = ("{\"node\":\"Binary\",\"op\":\"times\",\"left\":%s,\"right\":{\"node\":\"Literal\",\"value\":{\"number\":\"3\"},"
                + "\"type\":{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\"},\"source\":{\"snippet\":\"3\"}},"
                + "\"type\":%s,\"source\":{\"snippet\":\"(%s, met een minimum van %s) maal 3\"}}").formatted(clamp, baseType, of, ofRight);
        String sum = sumOfThree(left, right, baseType, of, ofRight), ownSum = sumOfThree(left, right, baseType, "zijn " + left, "zijn " + right);
        String one = ("{\"node\":\"Literal\",\"value\":{\"number\":\"1\"},\"type\":%s,\"source\":{\"snippet\":\"1 dn\"}}").formatted(
                number(true, 0, baseUnit));
        String sumSnippet = "zijn %s plus zijn %s plus zijn %s plus zijn %s".formatted(left, right, right, right);
        String condition = ("{\"node\":\"Comparison\",\"op\":\"equal\",\"left\":%s,\"right\":%s,"
                + "\"source\":{\"snippet\":\"%s gelijk is aan 1 dn\"}}").formatted(ownSum, one, sumSnippet);
        String rules = String.join(",",
                rule("afgeronde som", rounding(sum, "floor", wholeBase, "%s plus %s plus %s plus %s".formatted(of, ofRight, ofRight, ofRight), "naar beneden")),
                rule("afgeronde klem", rounding(timesThree, "floor", wholeBase, "(%s, met een minimum van %s) maal 3".formatted(of, ofRight), "naar beneden")),
                IrPath.rule(EQUAL, "KENMERKTOEKENNING", read(EQUAL, "Een Jutter is " + EQUAL), TRUE, condition));
        return written(objectType, system, "[" + declared(baseUnit, "dienst") + "," + declared(partUnit, "wacht") + "]", rules);
    }

    private static String sumOfThree(String left, String right, String type, String of, String ofRight) {
        String sum = plus(read(left, of), read(right, ofRight), type, of + " plus " + ofRight);
        sum = plus(sum, read(right, ofRight), type, "%s plus %s plus %s".formatted(of, ofRight, ofRight));
        return plus(sum, read(right, ofRight), type, "%s plus %s plus %s plus %s".formatted(of, ofRight, ofRight, ofRight));
    }

    private static String plus(String left, String right, String type, String snippet) {
        return "{\"node\":\"Binary\",\"op\":\"plus\",\"left\":%s,\"right\":%s,\"type\":%s,\"source\":{\"snippet\":\"%s\"}}"
                .formatted(left, right, type, snippet);
    }

    private static Path written(String objectType, String system, String units, String rules) throws Exception {
        String doc = ("{\"format\":\"uchar-ir-v1\",\"model\":{\"objectTypes\":[%s],\"factTypes\":[],\"parameters\":[],"
                + "\"domains\":[],\"unitSystems\":[{\"node\":\"UnitSystem\",\"name\":\"%s\",\"units\":%s}],"
                + "\"dimensions\":[],\"timelines\":[],\"dayKinds\":[]},\"rules\":{\"rules\":[%s],"
                + "\"dayKindDefinitions\":[],\"objectCreations\":[],\"allocations\":[],\"consistencies\":[],"
                + "\"uniquenesses\":[],\"factCreations\":[],\"startPointDeterminations\":[],"
                + "\"ruleGroups\":[]},\"diagnostics\":[]}").formatted(objectType, system, units, rules);
        Path tmp = Files.createTempFile("exacte-factor", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, doc);
        return tmp;
    }

    private static String rounding(String base, String mode, String type, String written, String modeSource) {
        return ("{\"node\":\"Rounding\",\"base\":%s,\"mode\":\"%s\",\"decimals\":0,\"type\":%s,"
                + "\"source\":{\"snippet\":\"%s %s afgerond op 0 decimalen\"}}").formatted(base, mode, type, written, modeSource);
    }

    private static String unit(String system, String abbreviation, String canonical, int denominator, String exponent) {
        return ("{\"node\":\"Unit\",\"abbreviation\":\"%s\",\"system\":\"%s\",\"canonical\":\"%s\","
                + "\"factorToCanonical\":{\"num\":1,\"den\":%d}%s}").formatted(abbreviation, system, canonical, denominator, exponent);
    }

    /** A unit as its system declares it: the unit's fields, then its word and the word's plural. */
    private static String declared(String unit, String word) {
        return unit.substring(0, unit.length() - 1)
                + ",\"spellings\":[\"%s\",\"%sen\"]}".formatted(word, word);
    }

    private static String number(boolean whole, int decimals, String unit) {
        return "{\"node\":\"Number\",\"whole\":%b,\"decimals\":%d,\"sign\":\"unspecified\",\"unit\":[%s]}"
                .formatted(whole, decimals, unit);
    }

    private static String attribute(String name, String datatype) {
        return "{\"node\":\"Attribute\",\"name\":\"%s\",\"datatype\":%s}".formatted(name, datatype);
    }

    private static String ownRead(String attribute) { return read(attribute, "de " + attribute + " van de Jutter"); }

    private static String read(String attribute, String snippet) {
        return "{\"node\":\"AttributeReference\",\"attribute\":\"%s\",\"subjectType\":\"Jutter\",\"source\":{\"snippet\":\"%s\"}}"
                .formatted(attribute, snippet);
    }

    private static String rule(String target, String value) {
        String article = target.equals(PRODUCT) || target.equals(QUOTIENT) ? "Het" : "De";
        return IrPath.rule(target, "BEREKENING", read(target, article + " " + target + " van een Jutter"), value, "null");
    }

    private static String characteristicRule(String characteristic, String op, String left, String right, String written) {
        String condition = ("{\"node\":\"Comparison\",\"op\":\"%s\",\"left\":%s,\"right\":%s,"
                + "\"source\":{\"snippet\":\"zijn %s %s zijn %s\"}}").formatted(op, read(left, "zijn " + left), read(right, "zijn " + right), left, written, right);
        return IrPath.rule(characteristic, "KENMERKTOEKENNING", read(characteristic, "Een Jutter is " + characteristic),
                TRUE, condition);
    }

    /** One Jutter with the two addends and the rate, the EXACT values of the targets as expectations, and which
     *  characteristics hold and which do not. */
    private static JsonNode caseDocument(String left, String leftValue, String right, String rightValue, String rate,
                                         Map<String, String> values, List<String> present, List<String> absent) {
        Map<String, String> input = new LinkedHashMap<>();
        input.put(left, leftValue);
        input.put(right, rightValue);
        input.put(RATE, rate);
        ObjectNode document = JSON.createObjectNode();
        document.put("format", "uchar-case-v0");
        document.putNull("asOfDate");
        ObjectNode instance = document.putObject("input").putArray("instances").addObject();
        document.withObject("/input").putObject("parameters");
        instance.put("label", "J1");
        instance.put("objectType", "Jutter");
        ObjectNode attributes = instance.putObject("attributes");
        input.forEach((name, value) -> attributes.putObject(name).put("number", value));
        instance.putObject("roles");
        instance.putArray("characteristics");
        ArrayNode expected = document.putArray("expected");
        values.forEach((name, value) -> {
            ObjectNode e = expected.addObject();
            e.put("kind", "attributeValue");
            e.put("label", "J1");
            e.put("attribute", name);
            e.putObject("value").put("number", value);
        });
        for (String c : present) expected.addObject().put("kind", "characteristicPresent").put("label", "J1").put("characteristic", c);
        for (String c : absent) expected.addObject().put("kind", "characteristicAbsent").put("label", "J1").put("characteristic", c);
        expected.addObject().put("kind", "consistent");
        return document;
    }
}
