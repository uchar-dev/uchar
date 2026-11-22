package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.graph.ProducerConsumer.ElementKey;
import dev.uchar.regelspraak.ir.Condition;
import dev.uchar.regelspraak.ir.Validity;
import dev.uchar.regelspraak.ir.ResolvedAllocation;
import dev.uchar.regelspraak.runtime.Breuk;
import dev.uchar.regelspraak.transpiler.tms.plan.Accumulation;
import dev.uchar.regelspraak.transpiler.tms.plan.Alpha;
import dev.uchar.regelspraak.transpiler.tms.plan.Exists;
import dev.uchar.regelspraak.transpiler.tms.plan.BetaTest;
import dev.uchar.regelspraak.transpiler.tms.plan.Binding;
import dev.uchar.regelspraak.transpiler.tms.plan.StaticFunction;
import dev.uchar.regelspraak.transpiler.tms.plan.Consequence;
import dev.uchar.regelspraak.transpiler.tms.plan.Join;
import dev.uchar.regelspraak.transpiler.tms.plan.ReadKind;
import dev.uchar.regelspraak.transpiler.tms.plan.ReadTable;
import dev.uchar.regelspraak.transpiler.tms.plan.Read;
import dev.uchar.regelspraak.transpiler.tms.plan.LhsItem;
import dev.uchar.regelspraak.transpiler.tms.plan.Modelplan;
import dev.uchar.regelspraak.transpiler.tms.plan.Not;
import dev.uchar.regelspraak.transpiler.tms.plan.Subject;
import dev.uchar.regelspraak.transpiler.tms.plan.FactPattern;
import dev.uchar.regelspraak.transpiler.tms.plan.RulePlan;
import dev.uchar.regelspraak.transpiler.tms.plan.Term;
import dev.uchar.regelspraak.transpiler.tms.plan.TestItem;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Plans made by hand that hold every form {@link DrlRendering} carries, over three beans of one document (a Wachter at a
 * Post on a Kustvak), so that a witness can compile the forms no golden of the corpus renders: the forms of step 2a of
 * card drl-teksttak-op-truth-maintenance ({@link #plan}), those of the rules of a fold of step 3a ({@link #folds}), and
 * those of a uniqueness and of the §9.5 channel of step 3b ({@link #channel}), those of a defaults stack of step 4
 * ({@link #defaults}), and those of a creation and a link ({@link #creations}) and of an allocation
 * ({@link #allocations}) of step 5, and in {@link #plan} the reading rule of card fixpoint-readings. They decide nothing of a document's
 * meaning, and no rule of them is meant to fire. The rules are named after what they hold.
 */
final class FormsPlan {

    private FormsPlan() {}

    /** Fragments of the text that show the forms the plan holds, one per form. */
    static final List<String> FORMS = List.of("not( ", " and ", "exists( ", "accumulate( ", "count()", "exactSum(",
            "exactMin(", "exactMax(", "import accumulate ", "collectList(", "eval( ", "agenda-group \"proefgroep\"", "ComputeHelpers.classIs(this",
            "ComputeHelpers.isA(this", "ComputeHelpers.classNotIn(this", "ComputeHelpers.inside(date",
            "ComputeHelpers.readProperty(this, \"hoogte\")", "ComputeHelpers.holds(", "ComputeHelpers.periods(", "ComputeHelpers.isSet(",
            "pad1_alleTermen(", "beta_alleTermen(", "samengesteld_alleTermen(", "zonderfout1_alleTermen(",
            "vouwlezing1_alleTermen(", "kwantorlezing_alleTermen(", "let1_alleTermen(", "binder1_alleTermen(",
            "lijst_schrijvers(", "deponering_deponeringVanEenToets(",
            "rsDagsoort_Werkdag", "second != null, second == $onderwerp.post", "first == $onderwerp", " >= 1", " < 3",
            "Outcome.Kind.DECIDED", "insert(", "insertLogical(", "ComputeHelpers.depositFlag()", "ComputeHelpers.fail(",
            "ComputeHelpers.required(", "ComputeHelpers.textComposition(", "ComputeHelpers.restateExact(",
            "ComputeHelpers.divided(", "ComputeHelpers.radicand(", "ComputeHelpers.largest(", "Breuk.van(",
            "java.time.LocalDateTime.parse(", "java.math.RoundingMode.HALF_UP", "Period.class",
            "dev.uchar.regelspraak.ir.Condition.Operator.LATER", "ComputeHelpers.contractBreach(", "ComputeHelpers.computeFault(",
            "ComputeHelpers.inconsistency(", "ComputeHelpers.writersConflict(", "new ParameterState(", "new EvaluationDate(",
            "new CharacteristicState(", "ComputeHelpers.bucketScalarOf(", "new PathState(", "new Reading($onderwerp, ",
            "lezing_lezing(", "ComputeHelpers.read(", "ComputeHelpers.composite(", "lezing.read(() -> ",
            "ComputeHelpers.emptyTestOverOpen(() -> ", "ComputeHelpers.dividedLazily(", "ComputeHelpers.negatedOverOpen(() -> ",
            "return ComputeHelpers.holdsForItself(() -> ", "return ComputeHelpers.faulted(() -> ",
            "return ComputeHelpers.deposited(() -> ", "if (!ComputeHelpers.deposited(() -> ", "return ComputeHelpers.valued(() -> ",
            "return ComputeHelpers.decided(oordeel_schrijverVanEenWaarde(", "ComputeHelpers.isOpen(v)", "new LeftOpen($onderwerp, ",
            "ComputeHelpers.open($onderwerp, ", "global OpenInput open_invoer;", "ComputeHelpers.readInput($uitkijkbonus, open_invoer, ",
            "ComputeHelpers.input($uitkijkbonus, open_invoer, ", "ComputeHelpers.input(false, open_invoer, ",
            "return ComputeHelpers.judge(() -> ", "return ComputeHelpers.judge(() -> {", "eval( ComputeHelpers.acts(",
            "if (ComputeHelpers.decides(");

    private static final String O = "$onderwerp";

    static Modelplan plan(Class<?> wachter, Class<?> post, Class<?> kustvak) {
        List<RulePlan> rules = new ArrayList<>();
        rules.add(allTerms(wachter, post, kustvak));
        Subject subject = new Subject(wachter, O, List.of(), List.of(),
                List.of(Binding.of("$uitkijkbonus", wachter, "uitkijkbonus")));
        ReadTable one = new ReadTable(List.of(new Read(0, ReadKind.INPUT, "uitkijkbonus", "$uitkijkbonus")));
        Term division = new Term.Call(StaticFunction.RS_AGG_COMPARE, List.of(new Term.Division(StaticFunction.DIVIDED, readOf(0), readOf(0),
                -1, "uitkijkbonus gedeeld door uitkijkbonus"), number("1"), text(">")), "de deling groter dan 1");
        Consequence.FaultName faultName = new Consequence.FaultName("proef", "proefregel");
        rules.add(new RulePlan("fout van een toets", "proef", null, one, List.of(subject,
                new TestItem("fout", TestItem.Role.FAULT, division, -1, List.of(0), 1)),
                new Consequence.TestFault(O, division, TestItem.Role.FAULT, List.of(0), 1, faultName, Consequence.Insertion.LOGICAL)));
        rules.add(new RulePlan("kenmerk zonder fout", "proef", "proefgroep", one, List.of(subject,
                new TestItem("zonder", TestItem.Role.WITHOUT_FAULT, division, -1, List.of(0), 1)),
                new Consequence.Characteristic(O, ElementKey.of("wachter", "gevarenwacht", true), Consequence.Insertion.LOGICAL)));
        rules.addAll(openForms(subject, one, faultName));
        rules.add(new RulePlan("vaste fout", "proef", null, one, List.of(subject,
                new TestItem("vorm", TestItem.Role.VORM, new Term.Call(StaticFunction.BUCKET_SCALAR, List.of(readOf(0),
                        new Term.Const(Boolean.FALSE, null, null)), null), -1, List.of(0), 1)),
                new Consequence.Fault(O, "een vaste zin", faultName, Consequence.Insertion.STATED)));
        rules.add(new RulePlan("inconsistentie", "proef", null, one, List.of(subject,
                new TestItem("voorwaarde", TestItem.Role.CONDITION, new Term.Call(StaticFunction.EMPTY_TEST, List.of(readOf(0),
                        new Term.Const(Boolean.TRUE, null, null)), "zijn uitkijkbonus leeg is"), -1, List.of(0), 1)),
                new Consequence.Inconsistency(O, "proef", List.of(), Consequence.Insertion.LOGICAL)));
        Term depositing = new Term.Call(StaticFunction.RS_AGG_COMPARE, List.of(new Term.Division(StaticFunction.DIVIDED,
                readOf(0), number("0"), 1, "uitkijkbonus gedeeld door 0"), number("1"), text(">")), "de deling door 0 groter dan 1");
        rules.add(new RulePlan("deponering van een toets", "proef", null, new ReadTable(List.of(
                new Read(0, ReadKind.INPUT, "uitkijkbonus", "$uitkijkbonus"), new Read(1, ReadKind.DEPOSIT_FLAG, null, null))),
                List.of(subject, new TestItem("deponering", TestItem.Role.DEPOSIT, depositing, 1, List.of(0), 2)),
                new Consequence.ConditionDeposit(O, "proef", depositing, 1, List.of(0), 2, false,
                        Consequence.Insertion.LOGICAL)));
        FactPattern values = new FactPattern(Value.class, "$waarde", List.of(new Alpha("attribuut", List.of(
                new Alpha.FieldTest("attribute", Alpha.Comparator.GELIJK, ElementKey.of("wachter", "uitkijkbonus", false))))),
                List.of(new Join("subject", "subject", O)), List.of());
        List<String> documentRules = List.of("a", "b", "c", "d", "e", "f", "g", "h");
        rules.add(new RulePlan("schrijvers", null, null,
                new ReadTable(List.of(new Read(0, ReadKind.VALUES, "waarden", "$waarden"))),
                List.of(new Subject(wachter, O, List.of(), List.of(), List.of()), new Accumulation(values,
                        List.of(new Accumulation.Aggregation(Accumulation.Kind.LIST, null, "$waarden")))),
                new Consequence.WritersConflict(O, 0, ElementKey.of("wachter", "uitkijkbonus", false), "uitkijkbonus",
                        documentRules, List.of("Wachter"), List.of("Wachter"), Consequence.Insertion.LOGICAL)));
        rules.add(new RulePlan("vereniging van de schrijvers", null, null,
                new ReadTable(List.of(new Read(0, ReadKind.VALUES, "waarden", "$waarden"),
                        new Read(1, ReadKind.INPUT, "invoer", "$invoer"))),
                List.of(new Subject(wachter, O, List.of(), List.of(), List.of(new Binding("$invoer", List.of(),
                                new Binding.Field(wachter, "uitkijkbonus"), Binding.ReadForm.VALUE, null))),
                        new Accumulation(values, List.of(new Accumulation.Aggregation(Accumulation.Kind.LIST, null,
                                "$waarden")))),
                new Consequence.WritersUnion(O, 0, 1, ElementKey.of("wachter", "uitkijkbonus", false), documentRules,
                        Consequence.Insertion.LOGICAL)));
        rules.add(new RulePlan("parameterstand", null, null, new ReadTable(List.of()), List.of(),
                new Consequence.InsertParameterState("drempel", "drempel", Consequence.Insertion.LOGICAL)));
        rules.add(new RulePlan("rekendatumstand", null, null, new ReadTable(List.of()), List.of(),
                new Consequence.EvaluationDateState("rekendatum", Consequence.Insertion.LOGICAL)));
        rules.add(new RulePlan("kenmerkstand", null, null, new ReadTable(List.of()),
                List.of(new Subject(wachter, O, List.of(), List.of(), List.of())),
                new Consequence.InsertCharacteristicState(O, ElementKey.of("wachter", "rustwacht", true), Consequence.StateSource.NONE, false,
                        Consequence.Insertion.LOGICAL)));
        rules.add(new RulePlan("stand", null, null, one, List.of(subject), new Consequence.State(O,
                ElementKey.of("wachter", "uitkijkbonus", false), Consequence.StateSource.INPUT, 0, true, null,
                Consequence.Insertion.LOGICAL)));
        rules.add(new RulePlan("padstand zonder link", null, null, new ReadTable(List.of()),
                List.of(new Subject(wachter, O, List.of(), List.of(), List.of())), new Consequence.InsertPathState(O, "Wachter:post",
                ElementKey.of("post", "hoogte", false), Consequence.StateSource.MISSING_LINK, -1, null,
                Consequence.Insertion.LOGICAL)));
        rules.add(new RulePlan("padstand", null, null, one, List.of(subject), new Consequence.InsertPathState(O,
                "Wachter:post", ElementKey.of("post", "hoogte", false), Consequence.StateSource.REACHED, 0, O,
                Consequence.Insertion.LOGICAL)));
        // the reading rule of a build with readings (card fixpoint-readings): the bound slot and a probe slot, which no
        // pattern binds and the reading carries as null
        rules.add(new RulePlan("lezing", null, null, new ReadTable(List.of(
                new Read(0, ReadKind.INPUT, "uitkijkbonus", "$uitkijkbonus"), new Read(1, ReadKind.DEPOSIT_FLAG, null, null))),
                List.of(subject), new Consequence.Reading(O, "/rules/rules/0/versions/0", List.of(0), 2,
                Consequence.Insertion.LOGICAL)));
        return new Modelplan(List.of(new Modelplan.Global("drempel", BigDecimal.class),
                new Modelplan.Global("rekendatum", LocalDate.class),
                new Modelplan.Global(Consequence.OPEN_INPUT, OpenInput.class)), rules);
    }

    /** Fragments of the text that show the forms of the plan of the allocations ({@link #allocations}), one per form. */
    static final List<String> ALLOCATION_FORMS = List.of(
            "    insertLogical(new Recipient(\"proefverdeling\", $bron, $onderwerp, $uitkijkbonus, $uitkijkbonus,"
                    + " $uitkijkbonus));",
            "    insertLogical(new Recipient(\"proefverdeling\", $bron, $onderwerp, null, null, null));",
            "    for (Object feit : ComputeHelpers.payout(\"proefverdeling\", $onderwerp,"
                    + " dev.uchar.regelspraak.ir.ResolvedAllocation.Method.NAAR_RATO, $pot, $ontvangers, true, true, false,"
                    + " Integer.valueOf(2), ElementKey.of(\"wachter\", \"uitkijkbonus\", false),"
                    + " ElementKey.of(\"post\", \"hoogte\", false), \"proefverdeling\", Integer.valueOf(2),"
                    + " Integer.valueOf(0), \"uitdeling naar rato\")) {\n"
                    + "        insertLogical(feit);\n    }",
            "ComputeHelpers.payout(\"proefverdeling\", $onderwerp,"
                    + " dev.uchar.regelspraak.ir.ResolvedAllocation.Method.OP_VOLGORDE, $pot, $ontvangers, false, false, true,"
                    + " null, ElementKey.of(\"wachter\", \"uitkijkbonus\", false), null, \"proefverdeling\", null, null,"
                    + " \"uitdeling op volgorde\")",
            "dev.uchar.regelspraak.ir.ResolvedAllocation.Method.GELIJKE_DELEN");

    /**
     * A plan with every consequence of an allocation (step 5 of card drl-teksttak-op-truth-maintenance), over the beans
     * of the same document as {@link #plan}: a recipient with its weight, key and cap, and one without them; the
     * distribution of each method, with the decimals of its floor and a rest, and without them. No rule of it is meant to
     * fire.
     */
    static Modelplan allocations(Class<?> wachter, Class<?> post) {
        List<RulePlan> rules = new ArrayList<>();
        FactPattern source = new FactPattern(post, "$bron", List.of(), List.of(), List.of());
        Subject recipient = new Subject(wachter, O, List.of(), List.of(),
                List.of(Binding.of("$uitkijkbonus", wachter, "uitkijkbonus")));
        rules.add(new RulePlan("ontvanger met waarden", "proefverdeling", null,
                table(memberSlot(0), new Read(1, ReadKind.INPUT, "uitkijkbonus", "$uitkijkbonus")),
                List.of(recipient, source), new Consequence.Recipient("$bron", "proefverdeling", 0, 1, 1, 1,
                Consequence.Insertion.LOGICAL)));
        rules.add(new RulePlan("ontvanger zonder waarden", "proefverdeling", null, table(memberSlot(0)),
                List.of(new Subject(wachter, O, List.of(), List.of(), List.of()), source),
                new Consequence.Recipient("$bron", "proefverdeling", 0, -1, -1, -1, Consequence.Insertion.LOGICAL)));
        Subject pot = new Subject(post, O, List.of(), List.of(), List.of(Binding.of("$pot", post, "hoogte")));
        Accumulation recipients = new Accumulation(new FactPattern(Recipient.class, "$ontvanger", List.of(new Alpha("verdeling",
                List.of(new Alpha.FieldTest("allocation", Alpha.Comparator.GELIJK, "proefverdeling")))),
                List.of(new Join("ontvanger-bron", "source", O)), List.of()),
                List.of(new Accumulation.Aggregation(Accumulation.Kind.LIST, null, "$ontvangers")));
        ReadTable payout = table(new Read(0, ReadKind.INPUT, "hoogte", "$pot"),
                new Read(1, ReadKind.RECIPIENTS, "ontvangers proefverdeling", "$ontvangers"));
        ElementKey share = ElementKey.of("wachter", "uitkijkbonus", false);
        rules.add(new RulePlan("uitdeling naar rato", "proefverdeling", null, payout, List.of(pot, recipients),
                new Consequence.Payout(O, "proefverdeling", ResolvedAllocation.Method.NAAR_RATO, 0, 1, true, true, false, 2,
                        share, ElementKey.of("post", "hoogte", false), "proefverdeling", 2, 0, null,
                        new Consequence.FaultName("proefverdeling", "uitdeling naar rato"), Consequence.Insertion.LOGICAL)));
        rules.add(new RulePlan("uitdeling op volgorde", "proefverdeling", null, payout, List.of(pot, recipients),
                new Consequence.Payout(O, "proefverdeling", ResolvedAllocation.Method.OP_VOLGORDE, 0, 1, false, false, true,
                        null, share, null, "proefverdeling", null, null, null,
                        new Consequence.FaultName("proefverdeling", "uitdeling op volgorde"), Consequence.Insertion.LOGICAL)));
        rules.add(new RulePlan("uitdeling in gelijke delen", "proefverdeling", null, payout, List.of(pot, recipients),
                new Consequence.Payout(O, "proefverdeling", ResolvedAllocation.Method.GELIJKE_DELEN, 0, 1, false, false,
                        false, 0, share, null, "proefverdeling", 0, null, null,
                        new Consequence.FaultName("proefverdeling", "uitdeling in gelijke delen"), Consequence.Insertion.LOGICAL)));
        return new Modelplan(List.of(), rules);
    }

    /** Fragments of the text that show the forms of the plan of the creations and the links ({@link #creations}), one
     *  per form. */
    static final List<String> CREATION_FORMS = List.of(
            "// uitkijkbonus\nfunction Object creatie_creatieMetInitialisaties(Object $uitkijkbonus, Object $fase,"
                    + " Object $herkomsten, Object $onderwerp) {",
            "    int diepte = ComputeHelpers.creationDepth(\"proefcreatie\", 0, \"Wachter\", (java.util.List) $herkomsten,"
                    + " 3);",
            "    Object i0 = null;\n    Object i1 = null;\n    try {\n        i0 = ComputeHelpers.collapse($uitkijkbonus);",
            "    } catch (RuntimeException e) {\n        return ComputeHelpers.caught(e);\n    }",
            "    Object nieuw = ComputeHelpers.newObject($onderwerp, \"dev.uchar.regelspraak.facts.Post\");\n"
                    + "    ComputeHelpers.set(nieuw, \"hoogte\", i0);",
            "    int fase = ((Integer) $fase).intValue();",
            "    feiten.add(nieuw);\n    feiten.add(new CreationStamp(\"proefcreatie\", $onderwerp, nieuw, fase, 0, diepte));\n"
                    + "    feiten.add(new RelationLink(\"wachterpost\", $onderwerp, nieuw, fase));\n"
                    + "    feiten.add(Boolean.TRUE.equals(i1) ? new HasCharacteristic(nieuw, ElementKey.of(\"post\","
                    + " \"bemand\", true)) : null);\n    return feiten;",
            "    agenda-group \"uchar-objectcreatie\"",
            "    if (ComputeHelpers.isEvaluationFault(v)) {\n        insert(new Fault($onderwerp, \"proefcreatie\","
                    + " ComputeHelpers.designate(v, \"proefcreatie\")));\n    } else {\n"
                    + "        java.util.List feiten = (java.util.List) v;\n        insert(feiten.get(0));\n"
                    + "        insert(feiten.get(1));\n        insert(feiten.get(2));\n"
                    + "        if (feiten.get(3) != null) {\n            insert(feiten.get(3));\n        }\n    }",
            "function Object creatie_creatieZonderInitialisaties(Object $fase, Object $herkomsten, Object $onderwerp) {\n"
                    + "    int diepte =",
            "    Object nieuw = ComputeHelpers.newObject($onderwerp, \"dev.uchar.regelspraak.facts.Wachter\");\n"
                    + "    int fase",
            "    feiten.add(new RelationLink(\"opvolging\", nieuw, $onderwerp, fase));",
            "    insertLogical(new RelationLink(\"wachterpost\", $onderwerp, $post, 0));",
            "    agenda-group \"uchar-feitcreatie\"",
            "    insert(new RelationLink(\"wachterpost\", $onderwerp, $post, $fase));",
            "    insertLogical(new Fault($onderwerp, \"proefrelatie\", new SecondFillingFault(\"proefrol\", $onderwerp)));");

    /**
     * A plan with every consequence of a creation and of a link (step 5 of card drl-teksttak-op-truth-maintenance), over
     * the beans of the same document as {@link #plan}: an object creation of a Post for a Wachter, in its agenda group,
     * with an attribute assignment set on the new bean and a characteristic assignment inserted after it,
     * stated as everything a creation makes (round 5b), each from a function that can fault; one of a Wachter without
     * assignments, whose new object is the first end of its link; the link a reference field of the input carries,
     * at phase 0, and the link of a fact creation, in its agenda group, at the phase it binds; and the fault of a second
     * filling. No rule of it is meant to fire.
     */
    static Modelplan creations(Class<?> wachter, Class<?> post) {
        List<RulePlan> rules = new ArrayList<>();
        FactPattern phase = new FactPattern(CreationPhase.class, "$creatiefase", List.of(new Alpha("creatiestratum", List.of(
                new Alpha.FieldTest("stratum", Alpha.Comparator.AT_LEAST, 0)))), List.of(),
                List.of(Binding.of("$fase", CreationPhase.class, "number")));
        Accumulation origin = new Accumulation(new FactPattern(CreationStamp.class, "$herkomst", List.of(),
                List.of(new Join("herkomst", "object", O)), List.of()),
                List.of(new Accumulation.Aggregation(Accumulation.Kind.LIST, null, "$herkomsten")));
        Consequence.FaultName fault = new Consequence.FaultName("proefcreatie", "proefcreatie");
        rules.add(new RulePlan("creatie met initialisaties", "proefcreatie", "uchar-objectcreatie",
                table(new Read(0, ReadKind.INPUT, "uitkijkbonus", "$uitkijkbonus"),
                        new Read(1, ReadKind.CREATION_PHASE, "creatiefase", "$fase"),
                        new Read(2, ReadKind.ORIGIN, "herkomst", "$herkomsten")),
                List.of(new Subject(wachter, O, List.of(), List.of(),
                        List.of(Binding.of("$uitkijkbonus", wachter, "uitkijkbonus"))), phase, origin),
                new Consequence.Creation(O, "proefcreatie", new Consequence.CreationDepth(2, 0, "Wachter", 3), List.of(
                        new Consequence.Assignment(ElementKey.of("post", "hoogte", false), new Term.SlotValue(0,
                                "uitkijkbonus")),
                        new Consequence.Assignment(ElementKey.of("post", "bemand", true),
                                new Term.Const(Boolean.TRUE, null, null))),
                        fault, Consequence.Insertion.STATED, new Consequence.Construction(post, List.of(
                                new Consequence.Construction.Setting(0, new Binding.Field(post, "hoogte"), null))), 1,
                        List.of(new Consequence.Insert.New(Consequence.Insertion.STATED),
                                new Consequence.Insert.Mark(Consequence.Insertion.STATED),
                                new Consequence.Insert.Link("wachterpost", false, Consequence.Insertion.STATED),
                                new Consequence.Insert.Characteristic(1, Consequence.Insertion.STATED)),
                        List.of(0, 1, 2), 3)));
        rules.add(new RulePlan("creatie zonder initialisaties", "proefcreatie", "uchar-objectcreatie",
                table(new Read(0, ReadKind.CREATION_PHASE, "creatiefase", "$fase"),
                        new Read(1, ReadKind.ORIGIN, "herkomst", "$herkomsten")),
                List.of(new Subject(wachter, O, List.of(), List.of(), List.of()), phase, origin),
                new Consequence.Creation(O, "proefopvolging", new Consequence.CreationDepth(1, 0, "Wachter", 3), List.of(), fault,
                        Consequence.Insertion.STATED, new Consequence.Construction(wachter, List.of()), 0,
                        List.of(new Consequence.Insert.New(Consequence.Insertion.STATED),
                                new Consequence.Insert.Mark(Consequence.Insertion.STATED),
                                new Consequence.Insert.Link("opvolging", true, Consequence.Insertion.STATED)),
                        List.of(0, 1), 2)));
        Subject withPost = new Subject(wachter, O, List.of(), List.of(), List.of(Binding.of("$post", wachter,
                "post")));
        rules.add(new RulePlan("koppeling uit de invoer", null, null, table(), List.of(withPost),
                new Consequence.RelationLink("wachterpost", O, "$post", -1, Consequence.Insertion.LOGICAL)));
        rules.add(new RulePlan("koppeling van een feitcreatie", "proeffeitcreatie", "uchar-feitcreatie",
                table(new Read(0, ReadKind.CREATION_PHASE, "creatiefase", "$fase")), List.of(withPost, phase),
                new Consequence.RelationLink("wachterpost", O, "$post", 0, Consequence.Insertion.STATED)));
        rules.add(new RulePlan("tweede vulling", null, null, table(),
                List.of(new Subject(wachter, O, List.of(), List.of(), List.of())),
                new Consequence.SecondFilling(O, "proefrelatie", "proefrol", Consequence.Insertion.LOGICAL)));
        return new Modelplan(List.of(), rules);
    }

    /** Fragments of the text that show the forms of the plan of a uniqueness and the channel ({@link #channel}), one per
     *  form. */
    static final List<String> CHANNEL_FORMS = List.of("componenten_sleutel(",
            "java.util.List waarden = new java.util.ArrayList();",
            "waarden.add(null);", "waarden.add(ComputeHelpers.fallback(", "// zijn uitkijkbonus; zijn uitkijkbonus of 0",
            "for (Object k : ComputeHelpers.uniquenessKeys(\"proefuniciteit\", 0, $onderwerp, null, ",
            "for (Object k : ComputeHelpers.uniquenessKeys(\"proefuniciteit\", 1, $onderwerp, $kustvak, ",
            ", -1)) {", ", 0)) {", "        insertLogical(k);",
            "insertLogical(new UniquenessViolation(\"proefuniciteit\", $onderwerp));",
            "insertLogical(new Fault($onderwerp, \"proefuniciteit\", ComputeHelpers.inputFormFault(\"uitkijkbonus\","
                    + " \"the key \\\"uitkijkbonus\\\""
                    + " needs the full timeline; \", $onderwerp, \" supplies a scalar\", \"proefuniciteit\")));",
            "insertLogical(new ChannelState(\"proef\", true));", "insertLogical(new ChannelState(\"proef\", false));");

    /**
     * A plan with every consequence of the rules of a uniqueness and of the §9.5 channel (step 3b of card
     * drl-teksttak-op-truth-maintenance), over the beans of the same document as {@link #plan}: the keys of a Wachter
     * under a form without a scope, over its uitkijkbonus, a component that is null and one through a function, each
     * with its sentence; the keys of a Post under a form whose scope is its Kustvak, over a component on a timeline;
     * the violation of the uniqueness; the fault of the form of an input, with a quote in its sentence; and the
     * standing of the channel of a form, inconsistent where an inconsistency of that form stands and consistent where
     * none does. No rule of it is meant to fire.
     */
    static Modelplan channel(Class<?> wachter, Class<?> post, Class<?> inconsistency) {
        List<RulePlan> rules = new ArrayList<>();
        Subject withBonus = new Subject(wachter, O, List.of(), List.of(),
                List.of(Binding.of("$uitkijkbonus", wachter, "uitkijkbonus")));
        rules.add(new RulePlan("sleutel", "proefuniciteit", null,
                table(new Read(0, ReadKind.INPUT, "uitkijkbonus", "$uitkijkbonus")), List.of(withBonus),
                new Consequence.UniquenessKey(O, "proefuniciteit", 0, List.of(new Term.SlotValue(0, "zijn uitkijkbonus"),
                        new Term.Const(null, null, null), new Term.Call(StaticFunction.FALLBACK, List.of(readOf(0), number("0")),
                                "zijn uitkijkbonus of 0")), -1, -1, List.of(0), 1, Consequence.Insertion.LOGICAL)));
        Subject withKustvak = new Subject(post, O, List.of(), List.of(),
                List.of(Binding.of("$hoogte", post, "hoogte"), Binding.of("$kustvak", post, "kustvak")));
        rules.add(new RulePlan("sleutel met scope en tijdlijn", "proefuniciteit", null,
                table(heightSlot(), new Read(1, ReadKind.SCOPE, "kustvak", "$kustvak")), List.of(withKustvak),
                new Consequence.UniquenessKey(O, "proefuniciteit", 1, List.of(readOf(0)), 0, 1, List.of(0, 1), 2,
                        Consequence.Insertion.LOGICAL)));
        Subject subject = new Subject(wachter, O, List.of(), List.of(), List.of());
        rules.add(new RulePlan("schending", "proefuniciteit", null, table(), List.of(subject),
                new Consequence.UniquenessViolation(O, "proefuniciteit", Consequence.Insertion.LOGICAL)));
        rules.add(new RulePlan("invoervorm", "proefuniciteit", null, table(), List.of(subject),
                new Consequence.InputFormFault(O, "uitkijkbonus", "the key \"uitkijkbonus\" needs the full timeline; ",
                        " supplies a scalar",
                        new Consequence.FaultName("proefuniciteit", "proefuniciteit"), Consequence.Insertion.LOGICAL)));
        List<FactPattern> channel = List.of(new FactPattern(inconsistency, "$inconsistentie", List.of(new Alpha("kanaal",
                List.of(new Alpha.FieldTest("rule", Alpha.Comparator.GELIJK, "proef")))), List.of(), List.of()));
        rules.add(new RulePlan("kanaalstand proef: inconsistent", null, null, table(),
                List.of(new Exists(channel, Exists.declared(channel), Exists.read(channel))),
                new Consequence.InsertChannelState("proef", true, Consequence.Insertion.LOGICAL)));
        rules.add(new RulePlan("kanaalstand proef: consistent", null, null, table(),
                List.of(new Not(channel, Exists.declared(channel), Exists.read(channel))),
                new Consequence.InsertChannelState("proef", false, Consequence.Insertion.LOGICAL)));
        return new Modelplan(List.of(), rules);
    }

    /** Fragments of the text that show the forms of the plan of the defaults ({@link #defaults}), one per form. */
    static final List<String> DEFAULTS_FORMS = List.of(
            "    Object v = ComputeHelpers.collapse($uitkijkbonus);\n    if (v != null) {\n"
                    + "        insertLogical(new Value($onderwerp, ElementKey.of(\"wachter\", \"uitkijkbonus\", false), v,"
                    + " \"proefstapel\"));\n        insertLogical(new Decides($onderwerp, \"proefstapel\", 0));",
            "// zijn uitkijkbonus of 0\nfunction Object afloop_default3(Object $uitkijkbonus) {",
            "        return ComputeHelpers.collapse(ComputeHelpers.fallback($uitkijkbonus, new java.math.BigDecimal(\"0\")));\n"
                    + "    } catch (RuntimeException e) {\n        return ComputeHelpers.caught(e);",
            "    Object v = afloop_default3($uitkijkbonus);\n"
                    + "    if (ComputeHelpers.isEvaluationFault(v)) {\n        insertLogical(new Fault($onderwerp, \"proefstapel\","
                    + " ComputeHelpers.designate(v, \"proefstapel\")));\n    } else if (v != null) {",
            "insertLogical(new Decides($onderwerp, \"proefstapel\", 3));",
            "    Object v = Boolean.TRUE;\n    if (v != null) {\n"
                    + "        insertLogical(new HasCharacteristic($onderwerp, ElementKey.of(\"wachter\", \"waakzaam\", true)));",
            "    Object v = Boolean.FALSE;\n    if (v != null) {\n"
                    + "        insertLogical(new Decides($onderwerp, \"proefstapel\", 2));\n    }",
            "    Object v = $waarde;\n    if (v != null) {\n        insertLogical(new Outcome($onderwerp, \"proefstapel\", 1, 2,"
                    + " dev.uchar.regelspraak.transpiler.tms.Outcome.Kind.DECIDED, v, null));",
            "Object conflict_verschil0() {\n    return \"proefstapel/0\";\n}",
            "insertLogical(new Outcome($onderwerp, \"proefstapel\", 0, 1,"
                    + " dev.uchar.regelspraak.transpiler.tms.Outcome.Kind.CONFLICT, null, (String) v));",
            "insertLogical(ComputeHelpers.inconsistency((String) v, $onderwerp, ComputeHelpers.list1(\"EF-027\")));",
            "    return ComputeHelpers.decided(oordeel_default4($uitkijkbonus), () -> ",
            "    } else if (ComputeHelpers.isOpen(v)) {\n        insertLogical(new LeftOpen($onderwerp, ElementKey.of("
                    + "\"wachter\", \"uitkijkbonus\", false), \"proefstapel: default 4\"));\n"
                    + "        insertLogical(new Decides($onderwerp, \"proefstapel\", 4, true));\n    } else if (v != null) {",
            "    if (ComputeHelpers.decides(oordeel_default5($uitkijkbonus))) {\n        Object v = Boolean.TRUE;",
            "Object afloop_default6(Object $uitkijkbonus) {\n    return ComputeHelpers.valued(() -> ",
            "index < 6 && open == false", "kind != dev.uchar.regelspraak.transpiler.tms.Outcome.Kind.OPEN",
            "    insertLogical(new Outcome($onderwerp, \"proefstapel\", 0, 1,"
                    + " dev.uchar.regelspraak.transpiler.tms.Outcome.Kind.OPEN, null, null));");

    /**
     * A plan with every consequence of the rules of a defaults stack (step 4 of card drl-teksttak-op-truth-maintenance),
     * over the Wachter of the same document as {@link #plan}: the decision of the simple form on an attribute, bound and
     * collapsed, with its fault in the plan and its control fact, which the consequence carries, and one computed and
     * collapsed, with its fault, through its function; the attribution of a characteristic and the decision not to
     * attribute it, constants, each with its control fact; the decision of a reconciliation bound off the outcome of a
     * member, inserted as an outcome; and the conflict of a reconciliation, under a constant key as an outcome and under
     * the key of a member as the §9.5 inconsistency. No rule of it is meant to fire.
     */
    static Modelplan defaults(Class<?> wachter) {
        List<RulePlan> rules = new ArrayList<>();
        Subject withBonus = new Subject(wachter, O, List.of(), List.of(),
                List.of(Binding.of("$uitkijkbonus", wachter, "uitkijkbonus")));
        rules.add(new RulePlan("default 0", "proefstapel", null,
                table(new Read(0, ReadKind.INPUT, "uitkijkbonus", "$uitkijkbonus")), List.of(withBonus),
                new Consequence.Decision(O, readOf(0), List.of(0), 1, true, new Consequence.FaultName("proefstapel", "proefstapel"),
                        List.of(new Consequence.StackFact.Value(ElementKey.of("wachter", "uitkijkbonus", false),
                                "proefstapel", null), new Consequence.StackFact.Decides("proefstapel", 0)),
                        Consequence.Insertion.LOGICAL)));
        Subject subject = new Subject(wachter, O, List.of(), List.of(), List.of());
        rules.add(new RulePlan("default 1", "proefstapel", null, table(), List.of(subject),
                new Consequence.Decision(O, new Term.Const(Boolean.TRUE, null, null), List.of(), 0, false, null,
                        List.of(new Consequence.StackFact.Characteristic(ElementKey.of("wachter", "waakzaam", true)),
                                new Consequence.StackFact.Decides("proefstapel", 1)), Consequence.Insertion.LOGICAL)));
        rules.add(new RulePlan("default 2", "proefstapel", null, table(), List.of(subject),
                new Consequence.Decision(O, new Term.Const(Boolean.FALSE, null, null), List.of(), 0, false, null,
                        List.of(new Consequence.StackFact.Decides("proefstapel", 2)), Consequence.Insertion.LOGICAL)));
        FactPattern decision = new FactPattern(Outcome.class, "$besluit", List.of(new Alpha("lid-besluit", List.of(
                new Alpha.FieldTest("kind", Alpha.Comparator.GELIJK, Outcome.Kind.DECIDED)))), List.of(),
                List.of(Binding.of(O, Outcome.class, "subject"), Binding.of("$waarde", Outcome.class, "value")));
        rules.add(new RulePlan("default 3", "proefstapel", null,
                table(new Read(0, ReadKind.INPUT, "uitkijkbonus", "$uitkijkbonus")), List.of(withBonus),
                new Consequence.Decision(O, new Term.Call(StaticFunction.FALLBACK, List.of(readOf(0), number("0")),
                        "zijn uitkijkbonus of 0"), List.of(0), 1, true, new Consequence.FaultName("proefstapel", "proefstapel"),
                        List.of(new Consequence.StackFact.Value(ElementKey.of("wachter", "uitkijkbonus", false),
                                "proefstapel", null), new Consequence.StackFact.Decides("proefstapel", 3)),
                        Consequence.Insertion.LOGICAL)));
        rules.add(new RulePlan("eens 0", "proefstapel", null, table(new Read(0, ReadKind.DECISION, "besluit",
                "$waarde")), List.of(decision), new Consequence.Decision(O, readOf(0), List.of(0), 1, false, null,
                List.of(new Consequence.StackFact.Outcome("proefstapel", 1, 2, Outcome.Kind.DECIDED)),
                Consequence.Insertion.LOGICAL)));
        rules.add(new RulePlan("verschil 0", "proefstapel", null, table(), List.of(subject),
                new Consequence.Conflict(O, text("proefstapel/0"), List.of(), 0,
                        new Consequence.StackFact.Outcome("proefstapel", 0, 1, Outcome.Kind.CONFLICT),
                        Consequence.Insertion.LOGICAL)));
        FactPattern conflict = new FactPattern(Outcome.class, "$conflict", List.of(new Alpha("lid-conflict", List.of(
                new Alpha.FieldTest("kind", Alpha.Comparator.GELIJK, Outcome.Kind.CONFLICT)))), List.of(),
                List.of(Binding.of(O, Outcome.class, "subject"), Binding.of("$sleutel", Outcome.class, "key")));
        rules.add(new RulePlan("doorgeven wortel", "proefstapel", null, table(new Read(0, ReadKind.CONFLICT_KEY,
                "conflictsleutel", "$sleutel")), List.of(conflict), new Consequence.Conflict(O, readOf(0), List.of(0), 1,
                new Consequence.StackFact.Inconsistency(), Consequence.Insertion.LOGICAL)));
        rules.addAll(openDefaults(withBonus));
        return new Modelplan(List.of(), rules);
    }

    /** The rules of a stack with a default that can be open (RUNTIME-SEMANTICS ch. 1.5): with a computed decision and with
     *  a constant one under a condition that can be undetermined; with a value that can read an open value, under the
     *  guard of an initialisation in each form; and the reconciliation over an open member. */
    private static List<RulePlan> openDefaults(Subject withBonus) {
        ElementKey bonus = ElementKey.of("wachter", "uitkijkbonus", false);
        ReadTable one = table(new Read(0, ReadKind.INPUT, "uitkijkbonus", "$uitkijkbonus"));
        Consequence.FaultName fault = new Consequence.FaultName("proefstapel", "proefstapel");
        Term read = new Term.Call(StaticFunction.READ, List.of(readOf(0)), "zijn uitkijkbonus");
        Term condition = new Term.Call(StaticFunction.RS_AGG_COMPARE, List.of(read, number("1"), text(">")),
                "zijn uitkijkbonus groter is dan 1");
        TestItem acts = new TestItem("oordeel", TestItem.Role.ACTS, condition, -1, List.of(0), 1, true);
        List<RulePlan> rules = new ArrayList<>();
        Consequence.LeavesOpen fourth = opens("proefstapel: default 4", bonus, 4), fifth = opens("proefstapel: default 5",
                ElementKey.of("wachter", "waakzaam", true), 5), sixth = opens("proefstapel: default 6", bonus, 6);
        rules.add(new RulePlan("default 4", "proefstapel", null, one, List.of(withBonus, acts), new Consequence.Writer(condition,
                false, List.of(0), 1, new Consequence.Decision(O, new Term.Call(StaticFunction.FALLBACK, List.of(read,
                number("0")), "zijn uitkijkbonus of 0"), List.of(0), 1, true, fault, null, List.of(new Consequence.StackFact.Value(
                bonus, "proefstapel", null), new Consequence.StackFact.Decides("proefstapel", 4)), Consequence.Insertion.LOGICAL),
                fourth)));
        rules.add(new RulePlan("default 5", "proefstapel", null, one, List.of(withBonus, acts), new Consequence.Writer(condition,
                false, List.of(0), 1, new Consequence.Decision(O, new Term.Const(Boolean.TRUE, null, null), List.of(), 1, false,
                null, List.of(new Consequence.StackFact.Characteristic(ElementKey.of("wachter", "waakzaam", true)),
                new Consequence.StackFact.Decides("proefstapel", 5)), Consequence.Insertion.LOGICAL), fifth)));
        Not deciding = new Not(new FactPattern(Decides.class, "$hoger", List.of(new Alpha("hoger", List.of(
                new Alpha.FieldTest("index", Alpha.Comparator.UNDER, 6),
                new Alpha.FieldTest("open", Alpha.Comparator.GELIJK, Boolean.FALSE)))),
                List.of(new Join("hoger-onderwerp", "subject", O)), List.of()));
        Not settled = new Not(new FactPattern(Outcome.class, "$hogere", List.of(new Alpha("hogere", List.of(
                new Alpha.FieldTest("kind", Alpha.Comparator.NOT_EQUAL, Outcome.Kind.OPEN)))),
                List.of(new Join("hogere-onderwerp", "subject", O)), List.of()));
        rules.add(new RulePlan("default 6", "proefstapel", null, one, List.of(withBonus, deciding, settled),
                new Consequence.Writer(null, false, List.of(0), 1, new Consequence.Decision(O, read, List.of(0), 1, true, fault,
                null, List.of(new Consequence.StackFact.Value(bonus, "proefstapel", null),
                new Consequence.StackFact.Decides("proefstapel", 6)), Consequence.Insertion.LOGICAL), sixth)));
        rules.add(new RulePlan("open 0", "proefstapel", null, table(), List.of(new FactPattern(Outcome.class, "$open", List.of(
                new Alpha("lid-open", List.of(new Alpha.FieldTest("kind", Alpha.Comparator.GELIJK, Outcome.Kind.OPEN)))),
                List.of(), List.of(Binding.of(O, Outcome.class, "subject")))), new Consequence.LeavesOpen(O, List.of(
                new Consequence.StackFact.Outcome("proefstapel", 0, 1, Outcome.Kind.OPEN)), Consequence.Insertion.LOGICAL)));
        return rules;
    }

    private static Consequence.LeavesOpen opens(String writer, ElementKey target, int index) {
        return new Consequence.LeavesOpen(O, List.of(new Consequence.StackFact.LeftOpen(target, writer),
                new Consequence.StackFact.OpenDefault("proefstapel", index)), Consequence.Insertion.LOGICAL);
    }

    /** What a rule named {@code proef} inserts where it leaves {@code target} open. */
    private static Consequence.LeavesOpen leftOpen(ElementKey target) {
        return new Consequence.LeavesOpen(O, List.of(new Consequence.StackFact.LeftOpen(target, "proef")),
                Consequence.Insertion.LOGICAL);
    }

    /** Fragments of the text that show the forms of the fold plan ({@link #folds}), one per form. */
    static final List<String> FOLD_FORMS = List.of("new Contribution(\"proefvouw: telling\", $vouwonderwerp, $onderwerp, null)",
            "ComputeHelpers.contribution(\"proefvouw: som\"", "\"MAAND\", null, true, false)",
            "ComputeHelpers.contribution(\"proefvouw: som per periode\"", "null, null, false, true)", "ComputeHelpers.verdict(",
            "matrix_oordeel(", "PeriodArithmetic.openSegmentFault(\"hoogte\", ",
            "lidtoets_lidtoets(", "new MemberTest(plaats, waar, fout, ComputeHelpers.reported(sonde), ComputeHelpers.depositCodes(sonde))",
            "new MemberTest(plaats, waar, fout, ComputeHelpers.reported(null), ComputeHelpers.depositCodes(null))", "plaats_lidtoets(",
            "ComputeHelpers.foldPlace(dev.uchar.regelspraak.transpiler.tms.plan.Consequence.PlaceReason.QUANTIFIER",
            "java.util.Arrays.asList(\"kustvak\", \"wachter\")", "java.util.Arrays.asList()",
            "new FilterDeposit(", "RuntimeException f = ", "new FilterFault(", "new MemberFault(",
            "ComputeHelpers.bucketScalarFault(\"hoogte\", ", "ComputeHelpers.placeOf(", "ComputeHelpers.meeting(", "ListPlace(",
            "ComputeHelpers.contribution(\"proefvouw: open som\", $vouwonderwerp, $onderwerp, ComputeHelpers.input($hoogte, "
                    + "open_invoer, $onderwerp, ", "eval( ComputeHelpers.acts(oordeel_lidwaardeOnderEenOpenFilter(",
            "for (Object f : ComputeHelpers.contributed(oordeel_lidwaardeOnderEenOpenFilter(", "insertLogical(f);",
            "\"proefvouw: gefilterde telling\", $vouwonderwerp, $onderwerp, new Contribution(\"proefvouw: gefilterde telling\", ",
            "insertLogical(ComputeHelpers.judged(\"proefvouw: open oordeel\", $vouwonderwerp, $onderwerp, matrix_openOordeel(",
            "Object matrix_openOordeel(", "FoldOpen( fold == \"proefvouw: open som\"", "ComputeHelpers.atom(() -> ",
            "ComputeHelpers.countOf(ComputeHelpers.standingCount($aantal, $openleden))",
            "ComputeHelpers.quantifierVerdictOverOpen($aantal, $som, $openleden, ", "fout = ComputeHelpers.ended(e);",
            "$vouwonderwerp, $onderwerp, ComputeHelpers.filtered(oordeel_lidtoetsOnderEenOpenFilter(", "$onderwerp, lidtoets_lidtoetsOnderEenOpenFilter(",
            "return ComputeHelpers.brokenOff(() -> ", ") != null;");

    /**
     * A plan with every consequence of the rules of a fold (step 3a of card drl-teksttak-op-truth-maintenance), over the
     * beans of the same document as {@link #plan}: the members are the Posts of a Kustvak, joined as the analysis joins
     * a member to the fold's subject. A member's contribution of each kind (its count, its value, its value as a total
     * per month and counted where it is filled, the verdict of a matrix, and the test of a matrix with a probe and a
     * place of two steps in the second selection, or without either), the deposit of a filter, the fault of a filter
     * and the fault of a member, each with the accumulates over the places of the steps that the analysis plans beside
     * them, one through the constraint of a place and one through the constraint of a meeting; and of a fold that can be
     * open (RUNTIME-SEMANTICS ch. 1.5) the contribution of a member whose value is an input that need not be complete, of
     * one whose filter or matrix can be undetermined, and the rules that read such a fold. No rule of it is meant to fire.
     */
    static Modelplan folds(Class<?> wachter, Class<?> post, Class<?> kustvak) {
        String v = "$vouwonderwerp";
        FactPattern foldSubject = new FactPattern(kustvak, v, List.of(), List.of(), List.of());
        Binding height = Binding.of("$hoogte", post, "hoogte");
        Term matrix = new Term.Call(StaticFunction.RS_AGG_COMPARE, List.of(readOf(0), number("5"), text(">")),
                "de hoogte groter dan 5");
        Term depositing = new Term.Call(StaticFunction.RS_AGG_COMPARE, List.of(new Term.Division(StaticFunction.DIVIDED, readOf(0),
                number("2"), 6, "de hoogte gedeeld door 2"), number("1"), text(">")), "de helft groter dan 1");
        Term failing = new Term.Call(StaticFunction.RS_AGG_COMPARE, List.of(new Term.Division(StaticFunction.DIVIDED, readOf(0),
                number("0"), -1, "de hoogte gedeeld door 0"), number("1"), text(">")), "het filter dat faalt");
        Term filterDepositing = new Term.Call(StaticFunction.RS_AGG_COMPARE, List.of(new Term.Division(StaticFunction.DIVIDED,
                readOf(0), number("0"), 2, "de hoogte gedeeld door 0"), number("1"), text(">")), "het filter dat deponeert");
        List<RulePlan> rules = new ArrayList<>();
        rules.add(new RulePlan("telling", "proef", null, table(new Read(0, ReadKind.SUBJECT, "lid", O)),
                List.of(foldSubject, member(post, v)), new Consequence.Contribution(v, "proefvouw: telling", 0,
                new Consequence.Input.Count(), null, List.of(0), 1, Consequence.Insertion.LOGICAL)));
        rules.add(new RulePlan("lidwaarde", "proef", null, table(heightSlot(), memberSlot(1)),
                List.of(foldSubject, member(post, v, height)), new Consequence.Contribution(v, "proefvouw: som", 1,
                new Consequence.Input.MemberValue(0, null, null, false, null, null, false), null, List.of(0, 1), 2, Consequence.Insertion.LOGICAL)));
        rules.add(new RulePlan("lidtijdlijn", "proef", null, table(heightSlot(), memberSlot(1)),
                List.of(foldSubject, member(post, v, height)), new Consequence.Contribution(v, "proefvouw: som per periode", 1,
                new Consequence.Input.MemberValue(0, null, null, false, null, null, true), null, List.of(0, 1), 2, Consequence.Insertion.LOGICAL)));
        rules.add(new RulePlan("lidwaarde per maand", "proef", null, table(heightSlot(), memberSlot(1)),
                List.of(foldSubject, member(post, v, height)), new Consequence.Contribution(v, "proefvouw: totaal", 1,
                new Consequence.Input.MemberValue(0, "MAAND", null, true, null, null, false), null, List.of(0, 1), 2, Consequence.Insertion.LOGICAL)));
        rules.add(new RulePlan("oordeel", "proef", null, table(heightSlot(), memberSlot(1)),
                List.of(foldSubject, member(post, v, height)), new Consequence.Contribution(v, "proefvouw: oordeel", 1,
                new Consequence.Input.Verdict(matrix), null, List.of(0, 1), 2, Consequence.Insertion.LOGICAL)));
        rules.add(new RulePlan("lidtoets", "proef", null, table(heightSlot(), memberSlot(1),
                placeSlot(2, ReadKind.LIST_PLACES, "$lijstplaatsen_1"), placeSlot(3, ReadKind.LIST_PLACE, "$plaats_1"),
                placeSlot(4, ReadKind.LIST_PLACES, "$lijstplaatsen_2"), placeSlot(5, ReadKind.LIST_PLACE, "$plaats_2"),
                new Read(6, ReadKind.DEPOSIT_FLAG, null, null)),
                List.of(foldSubject, member(post, v, height), places(1, "kustvak", new BetaTest("plaats-1",
                        StaticFunction.PLACE_OF, O, List.of(new Term.Const(List.of(), null, null)))),
                        places(2, "wachter", new BetaTest("plaats-2", StaticFunction.MEETING, O, List.of(
                                new Term.Const(List.of(), null, null), new Term.Const(List.of("wachter"), null, null))))),
                new Consequence.Contribution(v, "proefvouw: lidtoets", 1, new Consequence.Input.MemberTest(depositing, 6,
                        new Consequence.FoldPlace(1, List.of(new Consequence.FoldPlace.Step("kustvak", 2, 3),
                                new Consequence.FoldPlace.Step("wachter", 4, 5)), Consequence.PlaceReason.QUANTIFIER), false),
                        null, List.of(0, 1, 2, 3, 4, 5), 7, Consequence.Insertion.LOGICAL)));
        rules.add(new RulePlan("lidtoets zonder plaats", "proef", null, table(heightSlot(), memberSlot(1)),
                List.of(foldSubject, member(post, v, height)), new Consequence.Contribution(v, "proefvouw: lidtoets", 1,
                new Consequence.Input.MemberTest(matrix, -1, new Consequence.FoldPlace(0, List.of(), Consequence.PlaceReason.QUANTIFIER),
                        false),
                null, List.of(0, 1), 2, Consequence.Insertion.LOGICAL)));
        rules.add(new RulePlan("filterdeponering", "proef", null, table(heightSlot(), memberSlot(1),
                new Read(2, ReadKind.DEPOSIT_FLAG, null, null)),
                List.of(foldSubject, member(post, v, height)), new Consequence.FilterDeposit(v, "proefvouw", 1,
                filterDepositing, 2, List.of(0, 1), 3, false, Consequence.Insertion.LOGICAL)));
        rules.add(new RulePlan("filterfout", "proef", null, table(heightSlot(), memberSlot(1),
                placeSlot(2, ReadKind.LIST_PLACES, "$lijstplaatsen_1"), placeSlot(3, ReadKind.LIST_PLACE, "$plaats_1")),
                List.of(foldSubject, member(post, v, height), places(1, "kustvak", new BetaTest("plaats-1",
                        StaticFunction.PLACE_OF, O, List.of(new Term.Const(List.of(), null, null)))),
                        new TestItem("fout", TestItem.Role.FAULT, failing, -1, List.of(0), 4)),
                new Consequence.FilterFault(v, "proefvouw", 1, failing, new Consequence.FoldPlace(0,
                        List.of(new Consequence.FoldPlace.Step("kustvak", 2, 3)), Consequence.PlaceReason.FILTER_FAULT),
                        List.of(0, 1, 2, 3), 4, false, Consequence.Insertion.LOGICAL)));
        rules.add(new RulePlan("lidfout", "proef", null, table(new Read(0, ReadKind.SUBJECT, "lid", O),
                placeSlot(1, ReadKind.LIST_PLACES, "$lijstplaatsen_1"), placeSlot(2, ReadKind.LIST_PLACE, "$plaats_1")),
                List.of(foldSubject, member(post, v), places(1, "kustvak", new BetaTest("plaats-1",
                        StaticFunction.PLACE_OF, O, List.of(new Term.Const(List.of(), null, null))))),
                new Consequence.MemberFault(v, "proefvouw", 0, new Consequence.FoldPlace(0,
                        List.of(new Consequence.FoldPlace.Step("kustvak", 1, 2)), Consequence.PlaceReason.MEMBER_FAULT),
                        "the total of ", ": a scalar", "hoogte", -1, Consequence.Insertion.LOGICAL)));
        rules.add(new RulePlan("lidfout over een open segment", "proef", null, table(heightSlot(), memberSlot(1),
                placeSlot(2, ReadKind.LIST_PLACES, "$lijstplaatsen_1"), placeSlot(3, ReadKind.LIST_PLACE, "$plaats_1")),
                List.of(foldSubject, member(post, v, height), places(1, "kustvak", new BetaTest("plaats-1",
                        StaticFunction.PLACE_OF, O, List.of(new Term.Const(List.of(), null, null))))),
                new Consequence.MemberFault(v, "proefvouw", 1, new Consequence.FoldPlace(0,
                        List.of(new Consequence.FoldPlace.Step("kustvak", 2, 3)), Consequence.PlaceReason.MEMBER_FAULT),
                        null, null, "hoogte", 0, Consequence.Insertion.LOGICAL)));
        rules.addAll(openFolds(foldSubject, post, kustvak, height));
        return new Modelplan(List.of(new Modelplan.Global(Consequence.OPEN_INPUT, OpenInput.class)), rules);
    }

    /** The rules of a fold that can be open ({@link #folds}). */
    private static List<RulePlan> openFolds(FactPattern foldSubject, Class<?> post, Class<?> kustvak, Binding height) {
        String v = "$vouwonderwerp";
        ElementKey key = ElementKey.of("post", "hoogte", false);
        Read openInput = new Read(2, ReadKind.OPEN_INPUT, "open invoer", Consequence.OPEN_INPUT);
        Term read = new Term.Call(StaticFunction.READ_INPUT, List.of(readOf(0), readOf(2), readOf(1),
                new Term.Const(key, null, null)), "zijn hoogte");
        Term filter = new Term.Call(StaticFunction.RS_AGG_COMPARE, List.of(read, number("5"), text(">")), "de hoogte groter dan 5");
        TestItem filtered = new TestItem("filter", TestItem.Role.ACTS, filter, -1, List.of(0, 1, 2), 3, true);
        Consequence.Input.IncompleteInput input = new Consequence.Input.IncompleteInput(
                new Consequence.Input.MemberValue(0, null, null, true, null, null, false), 2, key);
        List<RulePlan> rules = new ArrayList<>();
        ReadTable table = table(heightSlot(), memberSlot(1), openInput);
        rules.add(new RulePlan("open lidwaarde", "proef", null, table, List.of(foldSubject, member(post, v, height)),
                new Consequence.Contribution(v, "proefvouw: open som", 1, input, null, List.of(0, 1, 2), 3, Consequence.Insertion.LOGICAL)));
        rules.add(new RulePlan("lidwaarde onder een open filter", "proef", null, table, List.of(foldSubject, member(post, v, height),
                filtered), new Consequence.FilteredContribution(filter, new Consequence.Contribution(v, "proefvouw: gefilterde som", 1,
                input, null, List.of(0, 1, 2), 3, Consequence.Insertion.LOGICAL))));
        rules.add(new RulePlan("telling onder een open filter", "proef", null, table, List.of(foldSubject, member(post, v, height),
                filtered), new Consequence.FilteredContribution(filter, new Consequence.Contribution(v, "proefvouw: gefilterde telling",
                1, new Consequence.Input.Count(), null, List.of(0, 1, 2), 3, Consequence.Insertion.LOGICAL))));
        rules.add(new RulePlan("open oordeel", "proef", null, table, List.of(foldSubject, member(post, v, height)),
                new Consequence.Contribution(v, "proefvouw: open oordeel", 1, new Consequence.Input.Judged(filter), null, List.of(0, 1, 2),
                        3, Consequence.Insertion.LOGICAL)));
        // as a build with readings plans them: each with the member's place, which its FoldOpen carries
        ReadTable placed = table(heightSlot(), memberSlot(1), openInput, placeSlot(3, ReadKind.LIST_PLACES, "$lijstplaatsen_1"),
                placeSlot(4, ReadKind.LIST_PLACE, "$plaats_1"));
        LhsItem at = places(1, "kustvak", new BetaTest("plaats-open", StaticFunction.PLACE_OF, O,
                List.of(new Term.Const(List.of(), null, null))));
        Consequence.FoldPlace place = new Consequence.FoldPlace(1, List.of(new Consequence.FoldPlace.Step("kustvak", 3, 4)),
                Consequence.PlaceReason.OPEN_MEMBER);
        rules.add(new RulePlan("open lidwaarde met plaats", "proef", null, placed, List.of(foldSubject, member(post, v, height), at),
                new Consequence.Contribution(v, "proefvouw: open som", 1, input, place, List.of(0, 1, 2, 3, 4), 5,
                        Consequence.Insertion.LOGICAL)));
        rules.add(new RulePlan("lidwaarde met plaats onder een open filter", "proef", null, placed, List.of(foldSubject,
                member(post, v, height), at, filtered), new Consequence.FilteredContribution(filter, new Consequence.Contribution(v,
                "proefvouw: gefilterde som", 1, input, place, List.of(0, 1, 2, 3, 4), 5, Consequence.Insertion.LOGICAL))));
        rules.add(new RulePlan("open oordeel met plaats", "proef", null, placed, List.of(foldSubject, member(post, v, height), at),
                new Consequence.Contribution(v, "proefvouw: open oordeel", 1, new Consequence.Input.Judged(filter), place,
                        List.of(0, 1, 2, 3, 4), 5, Consequence.Insertion.LOGICAL)));
        rules.add(new RulePlan("open lidtoets", "proef", null, table, List.of(foldSubject, member(post, v, height)),
                new Consequence.Contribution(v, "proefvouw: open lidtoets", 1, new Consequence.Input.MemberTest(filter, -1,
                        new Consequence.FoldPlace(0, List.of(), Consequence.PlaceReason.QUANTIFIER), true), null, List.of(0, 1, 2), 3,
                        Consequence.Insertion.LOGICAL)));
        rules.add(new RulePlan("lidtoets onder een open filter", "proef", null, table, List.of(foldSubject, member(post, v, height),
                filtered), new Consequence.FilteredContribution(filter, new Consequence.Contribution(v, "proefvouw: gefilterde lidtoets",
                1, new Consequence.Input.MemberTest(filter, -1, new Consequence.FoldPlace(0, List.of(),
                        Consequence.PlaceReason.QUANTIFIER), true), null, List.of(0, 1, 2), 3, Consequence.Insertion.LOGICAL))));
        rules.add(new RulePlan("open filterfout", "proef", null, table, List.of(foldSubject, member(post, v, height),
                new TestItem("breekt", TestItem.Role.BREAKS_OFF, filter, -1, List.of(0, 1, 2), 3, true)),
                new Consequence.FilterFault(v, "proefvouw", 1, filter, new Consequence.FoldPlace(0, List.of(),
                        Consequence.PlaceReason.FILTER_FAULT), List.of(0, 1, 2), 3, true, Consequence.Insertion.LOGICAL)));
        Subject subject = new Subject(kustvak, O, List.of(), List.of(), List.of());
        Accumulation fold = new Accumulation(new FactPattern(Contribution.class, "$bijdrage", List.of(new Alpha("vouw", List.of(
                new Alpha.FieldTest("fold", Alpha.Comparator.GELIJK, "proefvouw: open som")))),
                List.of(new Join("bijdrage", "subject", O)), List.of(Binding.of("$bijdragewaarde", Contribution.class, "value"))),
                List.of(new Accumulation.Aggregation(Accumulation.Kind.COUNT, null, "$aantal"),
                        new Accumulation.Aggregation(Accumulation.Kind.SUM, "$bijdragewaarde", "$som")));
        Accumulation open = new Accumulation(new FactPattern(FoldOpen.class, "$openlid", List.of(new Alpha("openlid", List.of(
                new Alpha.FieldTest("fold", Alpha.Comparator.GELIJK, "proefvouw: open som")))),
                List.of(new Join("openlid", "subject", O)), List.of()),
                List.of(new Accumulation.Aggregation(Accumulation.Kind.LIST, null, "$openleden")));
        ReadTable bound = table(new Read(0, ReadKind.FOLD_COUNT, "vouw", "$aantal"),
                new Read(1, ReadKind.FOLDED_VALUE, "vouwwaarde", "$som"), new Read(2, ReadKind.FOLD_OPEN, "open leden", "$openleden"));
        Term atom = new Term.Call(StaticFunction.ATOM, List.of(new Term.Deferred(new Term.Call(StaticFunction.RS_AGG_COMPARE,
                List.of(new Term.Call(StaticFunction.COUNT, List.of(new Term.Call(StaticFunction.STANDING_COUNT,
                        List.of(readOf(0), readOf(2)), null)), "de telling"), number("1"), text(">")), null), null)),
                "de telling groter dan 1");
        Term quantified = new Term.Call(StaticFunction.QUANTIFIER_VERDICT_OVER_OPEN, List.of(readOf(0), readOf(1), readOf(2),
                new Term.Const(Condition.Quantifier.ALLE, null, null), new Term.Const(0, null, null)), "alle posten");
        for (Term t : List.of(atom, quantified))
            rules.add(new RulePlan("lezing van een open vouw: " + t.source(), "proef", null, bound, List.of(subject, fold, open,
                    new TestItem("open", TestItem.Role.CONDITION, t, -1, List.of(0, 1, 2), 3, true)),
                    new Consequence.Inconsistency(O, "proef", List.of(), Consequence.Insertion.LOGICAL)));
        return rules;
    }

    private static ReadTable table(Read... slots) {
        return new ReadTable(List.of(slots));
    }

    private static Read heightSlot() {
        return new Read(0, ReadKind.INPUT, "hoogte", "$hoogte");
    }

    private static Read memberSlot(int slot) {
        return new Read(slot, ReadKind.SUBJECT, "lid", O);
    }

    private static Read placeSlot(int slot, ReadKind category, String name) {
        return new Read(slot, category, name, name);
    }

    /** The member, a Post of the fold's subject, bound as the subject of a rule of the fold. */
    private static Subject member(Class<?> post, String foldSubject, Binding... bindings) {
        return new Subject(post, O, List.of(), List.of(new Join("bereik", "kustvak", foldSubject)),
                List.of(bindings));
    }

    /** The accumulate over the places under {@code field} of the step {@code n}: their count and the least of them. */
    private static Accumulation places(int n, String field, BetaTest ofMember) {
        return new Accumulation(new FactPattern(ListPlace.class, "$lijstplaats_" + n, List.of(new Alpha("veld-" + n,
                List.of(new Alpha.FieldTest("field", Alpha.Comparator.GELIJK, field)))), List.of(), List.of(ofMember),
                List.of(Binding.of("$lijstplaatswaarde_" + n, ListPlace.class, "place"))),
                List.of(new Accumulation.Aggregation(Accumulation.Kind.COUNT, null, "$lijstplaatsen_" + n),
                        new Accumulation.Aggregation(Accumulation.Kind.MINIMUM, "$lijstplaatswaarde_" + n, "$plaats_" + n)));
    }

    /**
     * A rule with every kind of left-hand item, every comparator and form of reading, and a test of deposits and a
     * value with a deposit whose terms hold every node of a term.
     */
    /** The forms of a member that can be open (RUNTIME-SEMANTICS ch. 1.5): the test of every role over a condition whose
     *  judgement can be undetermined, a composite with a test on leeg and gevuld over a read that can be open and a
     *  probe; the writers of a value with an open read and of a characteristic, which leave their target open; the
     *  standing a writer leaves open, and the standings of an input that need not be complete. */
    private static List<RulePlan> openForms(Subject subject, ReadTable one, Consequence.FaultName faultName) {
        ReadTable probed = new ReadTable(List.of(new Read(0, ReadKind.INPUT, "uitkijkbonus", "$uitkijkbonus"),
                new Read(1, ReadKind.DEPOSIT_FLAG, null, null)));
        Term read = new Term.Call(StaticFunction.READ, List.of(readOf(0)), "zijn uitkijkbonus");
        Term depositing = new Term.Call(StaticFunction.RS_AGG_COMPARE, List.of(new Term.Division(
                StaticFunction.DIVIDED_LAZILY, read, new Term.Deferred(read, null), 1,
                "uitkijkbonus gedeeld door uitkijkbonus"), number("1"), text(">")), "de deling groter dan 1");
        Term composite = new Term.Compound(dev.uchar.regelspraak.ir.Condition.Quantifier.TEN_MINSTE, 1, List.of(
                new Term.Call(StaticFunction.EMPTY_TEST, List.of(number("1"), new Term.Const(Boolean.FALSE, null, null)), "1 gevuld is"),
                new Term.ClosureOverOpen(new Term.Deferred(read, null), true, 1, "zijn uitkijkbonus leeg is"), depositing), 1, 1,
                "aan ten minste een voldoet");
        Term compositeWithoutProbe = new Term.Compound(dev.uchar.regelspraak.ir.Condition.Quantifier.ALLE, 0, List.of(read, read),
                0, -1, "aan alle voldoet");
        Term withoutProbe = new Term.Call(StaticFunction.RS_AGG_COMPARE, List.of(read, number("1"), text(">")),
                "zijn uitkijkbonus groter is dan 1");
        ElementKey bonus = ElementKey.of("wachter", "uitkijkbonus", false);
        ElementKey watch = ElementKey.of("wachter", "gevarenwacht", true);
        ReadTable withInput = new ReadTable(List.of(new Read(0, ReadKind.INPUT, "uitkijkbonus", "$uitkijkbonus"),
                new Read(1, ReadKind.OPEN_INPUT, "open invoer", Consequence.OPEN_INPUT),
                new Read(2, ReadKind.SUBJECT, "onderwerp zelf", O)));
        Term inputRead = new Term.Call(StaticFunction.READ_INPUT, List.of(readOf(0), readOf(1), readOf(2),
                new Term.Const(bonus, null, null)), "zijn uitkijkbonus");
        Term inputTest = new Term.Call(StaticFunction.RS_AGG_COMPARE, List.of(inputRead, number("1"), text(">")),
                "zijn uitkijkbonus groter is dan 1");
        List<RulePlan> rules = new ArrayList<>();
        rules.add(new RulePlan("open consistentie", "proef", null, one, List.of(subject, new TestItem("open",
                TestItem.Role.CONDITION, new Term.Call(StaticFunction.NEGATED_OVER_OPEN, List.of(new Term.Deferred(withoutProbe,
                null)), "zijn uitkijkbonus groter is dan 1"), -1, List.of(0), 1, true)),
                new Consequence.Inconsistency(O, "proef", List.of(), Consequence.Insertion.LOGICAL)));
        rules.add(new RulePlan("schrijver onder een samengestelde voorwaarde", "proef", null, one, List.of(subject,
                new TestItem("oordeel", TestItem.Role.ACTS, compositeWithoutProbe, -1, List.of(0), 1, true)),
                new Consequence.Writer(compositeWithoutProbe, false, List.of(0), 1, new Consequence.Characteristic(O, watch,
                        Consequence.Insertion.LOGICAL), leftOpen(watch))));
        rules.add(new RulePlan("waarde met een open lezing", "proef", null, withInput, List.of(subject),
                new Consequence.Writer(null, false, List.of(0, 1, 2), 3, new Consequence.Value(O, bonus, "proef", inputRead,
                        List.of(0, 1, 2), 3, false, false, null, faultName, Consequence.Insertion.LOGICAL), leftOpen(bonus))));
        for (TestItem.Role role : List.of(TestItem.Role.CONDITION, TestItem.Role.WITHOUT_FAULT, TestItem.Role.FAULT))
            rules.add(new RulePlan("open " + role, "proef", null, one, List.of(subject,
                    new TestItem("open", role, withoutProbe, -1, List.of(0), 1, true)),
                    new Consequence.Inconsistency(O, "proef", List.of(), Consequence.Insertion.LOGICAL)));
        rules.add(new RulePlan("open deponering", "proef", null, probed, List.of(subject,
                new TestItem("open", TestItem.Role.DEPOSIT, composite, 1, List.of(0), 2, true)),
                new Consequence.ConditionDeposit(O, "proef", composite, 1, List.of(0), 2, true,
                        Consequence.Insertion.LOGICAL)));
        rules.add(new RulePlan("schrijver van een waarde", "proef", null, withInput, List.of(subject,
                new TestItem("oordeel", TestItem.Role.ACTS_WITHOUT_FAULT, inputTest, -1, List.of(0, 1, 2), 3, true)),
                new Consequence.Writer(inputTest, true, List.of(0, 1, 2), 3, new Consequence.Value(O, bonus, "proef",
                        inputRead, List.of(0, 1, 2), 3, false, false, null, faultName, Consequence.Insertion.LOGICAL),
                        leftOpen(bonus))));
        rules.add(new RulePlan("schrijver van een kenmerk", "proef", null, one, List.of(subject,
                new TestItem("oordeel", TestItem.Role.ACTS, withoutProbe, -1, List.of(0), 1, true)),
                new Consequence.Writer(withoutProbe, false, List.of(0), 1, new Consequence.Characteristic(O, watch,
                        Consequence.Insertion.LOGICAL), leftOpen(watch))));
        for (ElementKey member : List.of(bonus, watch))
            rules.add(new RulePlan("open stand " + member.name(), null, null, new ReadTable(List.of()), List.of(subject),
                    new Consequence.OpenStanding(O, member, Consequence.Insertion.LOGICAL)));
        rules.add(new RulePlan("onvolledige invoer", null, null, one, List.of(subject),
                new Consequence.State(O, bonus, Consequence.StateSource.INCOMPLETE_INPUT, 0, false, null,
                        Consequence.Insertion.LOGICAL)));
        rules.add(new RulePlan("onvolledige kenmerkinvoer", null, null, one, List.of(subject),
                new Consequence.InsertCharacteristicState(O, watch, Consequence.StateSource.INCOMPLETE_INPUT, false,
                        Consequence.Insertion.LOGICAL)));
        rules.add(new RulePlan("onvolledige invoer langs een pad", null, null, one, List.of(subject),
                new Consequence.InsertPathState(O, "Wachter:post", bonus, Consequence.StateSource.INCOMPLETE_INPUT, 0, O,
                        Consequence.Insertion.LOGICAL)));
        return rules;
    }

    private static RulePlan allTerms(Class<?> wachter, Class<?> post, Class<?> kustvak) {
        Binding.Field toPost = new Binding.Field(wachter, "post"), toKustvak = new Binding.Field(post, "kustvak");
        Subject subject = new Subject(wachter, O,
                List.of(new Alpha("klassen", List.of(
                        new Alpha.FieldTest(null, Alpha.Comparator.CLASS_IS, wachter),
                        new Alpha.FieldTest(null, Alpha.Comparator.IS_A, wachter),
                        new Alpha.FieldTest(null, Alpha.Comparator.CLASS_NOT_IN,
                                new LinkedHashSet<>(List.of(post, kustvak))))),
                        new Alpha("velden", List.of(
                                new Alpha.FieldTest(List.of(), "hoogte", Binding.ReadForm.VALUE, Alpha.Comparator.LEEG,
                                        null, post),
                                new Alpha.FieldTest(List.of(toPost), null, Binding.ReadForm.VALUE,
                                        Alpha.Comparator.GEVULD, null),
                                new Alpha.FieldTest(List.of(), "gevarenwacht", Binding.ReadForm.WAAR,
                                        Alpha.Comparator.GELIJK, Boolean.TRUE),
                                new Alpha.FieldTest("uitkijkbonus", Alpha.Comparator.NOT_EQUAL, null)))),
                List.of(),
                List.of(new Binding("$uitkijkbonus", List.of(), new Binding.Field(wachter, "uitkijkbonus"),
                                Binding.ReadForm.VALUE, null),
                        new Binding("$gevarenwacht", List.of(), new Binding.Field(wachter, "gevarenwacht"),
                                Binding.ReadForm.WAAR, null),
                        new Binding("$tijdvakken", List.of(), new Binding.Field(wachter, "uitkijkbonus"),
                                Binding.ReadForm.PERIODS, null),
                        new Binding("$postGezet", List.of(), toPost, Binding.ReadForm.IS_SET, post),
                        new Binding("$postHoogte", List.of(toPost), new Binding.Field(post, "hoogte"),
                                Binding.ReadForm.VALUE, null),
                        new Binding("$postKustvakVerraderlijk", List.of(toPost, toKustvak),
                                new Binding.Field(kustvak, "verraderlijk"), Binding.ReadForm.WAAR, null),
                        new Binding("$postRisicopost", List.of(), new Binding.Field(post, "risicopost"),
                                Binding.ReadForm.VALUE, null)));
        FactPattern parameter = new FactPattern(ParameterState.class, "$parameter", List.of(new Alpha("parameter",
                List.of(new Alpha.FieldTest("parameter", Alpha.Comparator.GELIJK, "drempel")))), List.of(),
                List.of(Binding.of("$drempel", ParameterState.class, "value")));
        FactPattern evaluationDate = new FactPattern(EvaluationDate.class, "$rekendatum", List.of(new Alpha("geldig",
                List.of(new Alpha.FieldTest("date", Alpha.Comparator.WITHIN,
                        new Validity(LocalDate.of(2026, 1, 1), null))))), List.of(),
                List.of(Binding.of("$datum", EvaluationDate.class, "date")));
        FactPattern outcome = new FactPattern(Outcome.class, "$uitkomst", List.of(new Alpha("rang", List.of(
                new Alpha.FieldTest("kind", Alpha.Comparator.GELIJK, Outcome.Kind.DECIDED),
                new Alpha.FieldTest("chain", Alpha.Comparator.AT_LEAST, 1),
                new Alpha.FieldTest("position", Alpha.Comparator.UNDER, 3)))),
                List.of(new Join("uitkomst", "subject", O)), List.of());
        FactPattern relationLink = new FactPattern(RelationLink.class, "$koppeling", List.of(), List.of(new Join("first", "first", O),
                new Join("second", "second", O, toPost)), List.of(new BetaTest("ontmoeting", StaticFunction.MEETING, O,
                List.of(new Term.Const(List.of(), null, null), new Term.Const(List.of("post"), null, null)))), List.of());
        FactPattern notValue = new FactPattern(Value.class, "$nietWaarde", List.of(), List.of(new Join("nw", "subject", O)),
                List.of());
        FactPattern notCharacteristic = new FactPattern(HasCharacteristic.class, "$nietKenmerk", List.of(), List.of(new Join("nk",
                "subject", "$nietWaarde", new Binding.Field(Value.class, "subject"))), List.of());
        List<FactPattern> not = List.of(notValue, notCharacteristic);
        FactPattern state = new FactPattern(State.class, "$stand", List.of(), List.of(new Join("st", "subject", O)), List.of());
        FactPattern contribution = new FactPattern(Contribution.class, "$bijdrage", List.of(new Alpha("vouw", List.of(
                new Alpha.FieldTest("fold", Alpha.Comparator.GELIJK, "proefvouw")))),
                List.of(new Join("bijdrage", "subject", O)),
                List.of(Binding.of("$bijdragewaarde", Contribution.class, "value")));
        Accumulation fold = new Accumulation(contribution, List.of(
                new Accumulation.Aggregation(Accumulation.Kind.COUNT, null, "$aantal"),
                new Accumulation.Aggregation(Accumulation.Kind.SUM, "$bijdragewaarde", "$som"),
                new Accumulation.Aggregation(Accumulation.Kind.MINIMUM, "$bijdragewaarde", "$kleinste"),
                new Accumulation.Aggregation(Accumulation.Kind.MAXIMUM, "$bijdragewaarde", "$grootste"),
                new Accumulation.Aggregation(Accumulation.Kind.LIST, null, "$lijst")));
        // the slots: what the bindings bind, the subject, two binders and the probe
        String[] names = {"$uitkijkbonus", "$gevarenwacht", "$tijdvakken", "$postGezet", "$postHoogte",
                "$postKustvakVerraderlijk", "$drempel", "$datum", "$aantal", "$som", "$kleinste", "$grootste", null,
                null, null, "$lijst", O, "$postRisicopost"};
        List<Read> slots = new ArrayList<>();
        for (int i = 0; i < names.length; i++)
            slots.add(new Read(i, names[i] == null ? (i == 14 ? ReadKind.DEPOSIT_FLAG : ReadKind.BINDER) : ReadKind.INPUT,
                    names[i] == null ? null : "slot " + i, names[i]));
        List<Integer> read = List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 15, 16, 17);
        Term test = new Term.Compound(Condition.Quantifier.ALLE, 0, List.of(
                new Term.Call(StaticFunction.RS_AGG_COMPARE, List.of(new Term.Division(StaticFunction.DIVIDED, readOf(0), number("2"), 14,
                        "de uitkijkbonus gedeeld door 2"), number("1"), text(">")), "de helft groter dan 1"),
                new Term.Call(StaticFunction.RS_AGG_COMPARE, List.of(new Term.SquareRoot(StaticFunction.RADICAND, readOf(0), 14,
                        "de wortel"), number("1"), text(">=")), "de wortel minstens 1"),
                new Term.Call(StaticFunction.RS_AGG_COMPARE, List.of(new Term.FoldRead(readOf(9), 15, 14, 10, 11,
                        "de som van de bijdragen"), number("0"), text("<")), "de som onder 0"),
                new Term.QuantifierRead(StaticFunction.QUANTIFIER_READ, readOf(8), readOf(15), Condition.Quantifier.TEN_MINSTE, 1, 14,
                        "ten minste één bijdrage"),
                new Term.WithoutFault(new Term.Call(StaticFunction.KENMERK, List.of(readOf(1), new Term.Const(Boolean.TRUE, null,
                        null)), "hij een gevarenwacht is"), null)), "alle voorwaarden");
        Term value = new Term.Let(12, "a", new Term.Call(StaticFunction.RS_PLUS, List.of(readOf(0), number("1.50")), "a"),
                new Term.Let(13, "b", new Term.Choice(new Term.WithoutFault(new Term.Call(StaticFunction.RS_AGG_COMPARE,
                        List.of(new Term.Binder(12, "a", "a"), readOf(6), text(">")), "a groter dan de drempel"), null),
                        new Term.Restate(new Term.Binder(12, "a", "a"), null, null,
                                BigDecimal.ONE, BigDecimal.TEN, null),
                        new Term.Restate(new Term.Binder(12, "a", "a"), null, null,
                                new BigDecimal("5"), new BigDecimal("2"), null), "als a groter is dan de drempel"),
                        new Term.ListedFold(StaticFunction.LARGEST, List.of(
                                new Term.Binder(13, "b", "b"),
                                new Term.Required(readOf(7), "geen rekendatum", null),
                                new Term.TextComposition(List.of(text("x"), new Term.Call(StaticFunction.RS_TEXT,
                                        List.of(readOf(0), new Term.Const(2, null, null), new Term.Const("EUR", null, null)),
                                        null)), "x en de uitkijkbonus"),
                                new Term.Call(StaticFunction.RS_ROUNDING, List.of(readOf(0), new Term.Const(2, null, null),
                                        new Term.Const(RoundingMode.HALF_UP, null, null)), null),
                                new Term.Call(StaticFunction.RS_DURATION_DAY_KIND, List.of(new Term.Const(
                                        LocalDate.of(2026, 1, 1), null, null), readOf(7), new Term.DayKind("werkdag",
                                        d -> true, null), new Term.Const(Boolean.TRUE, null, null)), null),
                                new Term.Call(StaticFunction.DATE_COMPARE, List.of(new Term.Const(
                                        LocalDateTime.of(2026, 1, 1, 12, 0), null, null), readOf(7),
                                        new Term.Const(Condition.Operator.LATER, null, null)), null),
                                new Term.Call(StaticFunction.RS_DATE_PART, List.of(readOf(7),
                                        new Term.Const(ChronoField.YEAR, null, null)), null),
                                new Term.Call(StaticFunction.RS_TIME_CLIP, List.of(readOf(2), readOf(2),
                                        new Term.Const(Period.class, null, null)), null),
                                new Term.Call(StaticFunction.RS_DIM_SUM, List.of(readOf(0), new Term.Const(List.of("a", "b"),
                                        null, null)), null),
                                new Term.Call(StaticFunction.QUANTIFIER_VERDICT, List.of(readOf(8), readOf(9),
                                        new Term.Const(Condition.Quantifier.GEEN, null, null), new Term.Const(0, null, null)),
                                        null),
                                new Term.Call(StaticFunction.FALLBACK, List.of(new Term.Const(null, null, null),
                                        new Term.Const(Breuk.van(BigInteger.ONE, BigInteger.valueOf(3)), null, null)), null),
                                new Term.Call(StaticFunction.FALLBACK, List.of(new Term.Const(5L, null, null),
                                        new Term.Const(BigInteger.valueOf(7), null, null)), null),
                                new Term.Input(readOf(16), new Binding.Field(wachter, "rustwacht"), Binding.ReadForm.WAAR,
                                        null),
                                new Term.Choice(new Term.Const(Boolean.TRUE, null, null), readOf(17),
                                        new Term.Fail("nooit", null), null),
                                new Term.FoldRead(readOf(9), 15, 14, 10, 11, null)), "de grootste"),
                        false, "b is als a groter is dan de drempel"),
                false, "a is de uitkijkbonus plus 1,50");
        List<LhsItem> lhs = List.of(subject, parameter, evaluationDate, outcome, relationLink,
                new Not(not, Exists.declared(not), Exists.read(not)), new Exists(List.of(state),
                        List.of("$stand"), List.of(O)), fold,
                new TestItem("deponering", TestItem.Role.DEPOSIT, test, 14, read, names.length));
        return new RulePlan("alle termen", "proef", "proefgroep", new ReadTable(slots), lhs,
                new Consequence.Value(O, ElementKey.of("wachter", "risicotoeslag", false), "alle termen", value, read,
                        names.length, true, false, new Consequence.Deposit(14, "proef"), new Consequence.FaultName("proef",
                        "alle termen"), Consequence.Insertion.LOGICAL));
    }

    private static Term readOf(int slot) {
        return new Term.SlotValue(slot, null);
    }

    private static Term number(String value) {
        return new Term.Const(new BigDecimal(value), null, value);
    }

    private static Term text(String value) {
        return new Term.Const(value, null, null);
    }
}
