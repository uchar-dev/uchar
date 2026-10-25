package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.graph.ProducerConsumer.ElementKey;
import dev.uchar.regelspraak.refusal.DroolsCode;
import dev.uchar.regelspraak.refusal.Reason;
import dev.uchar.regelspraak.ir.Validity;
import dev.uchar.regelspraak.runtime.Breuk;
import dev.uchar.regelspraak.schema.FactModelGenerator;
import dev.uchar.regelspraak.transpiler.core.Namen;
import dev.uchar.regelspraak.transpiler.tms.plan.Accumulation;
import dev.uchar.regelspraak.transpiler.tms.plan.Alpha;
import dev.uchar.regelspraak.transpiler.tms.plan.Exists;
import dev.uchar.regelspraak.transpiler.tms.plan.BetaTest;
import dev.uchar.regelspraak.transpiler.tms.plan.Binding;
import dev.uchar.regelspraak.transpiler.tms.plan.StaticFunction;
import dev.uchar.regelspraak.transpiler.tms.plan.Consequence;
import dev.uchar.regelspraak.transpiler.tms.plan.Join;
import dev.uchar.regelspraak.transpiler.tms.plan.Read;
import dev.uchar.regelspraak.transpiler.tms.plan.LhsItem;
import dev.uchar.regelspraak.transpiler.tms.plan.Modelplan;
import dev.uchar.regelspraak.transpiler.tms.plan.Not;
import dev.uchar.regelspraak.transpiler.tms.plan.Subject;
import dev.uchar.regelspraak.transpiler.tms.plan.FactPattern;
import dev.uchar.regelspraak.transpiler.tms.plan.RulePlan;
import dev.uchar.regelspraak.transpiler.tms.plan.Route;
import dev.uchar.regelspraak.transpiler.tms.plan.Term;
import dev.uchar.regelspraak.transpiler.tms.plan.TestItem;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.Token;
import org.drools.drl.parser.lang.DRL6Lexer;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The rendering of the outcome of the analysis into DRL text ({@link #text}), the second rendering beside the
 * executable model ({@link PatternDslRendering}). It decides nothing: which globals there are, what each rule reads,
 * how and in which order, what a test asks and what a firing inserts is the plan's ({@link Modelplan},
 * {@link RulePlan}); this class spells it.
 *
 * <p>The spelling follows the measurements of the second advice of card drl-teksttak-op-truth-maintenance (FABLE-2
 * §2) and the decisions taken on them:
 * <ul>
 *   <li>The rules are MVEL; a test or a value of the plan, a {@link Term}, that tests or computes something is a
 *       function in the Java dialect with the RegelSpraak sentence of its node above it as a comment, which the rule
 *       calls. MVEL has no {@code try} and no {@code throw}, and one form for every term keeps one semantics. A node
 *       that needs statements of its own, a {@code Let} and its binder, a condition without its fault, the
 *       evaluation of a fold and of a quantifier fold, and a composite that no test carries (the next item), is a
 *       function of its own, which takes the parameters of the function it stands in.</li>
 *   <li>A function stands only where it tests or computes something (card drl-no-passthrough-functions). A test that
 *       asks the verdict of a composite, that it holds or holds without a fault, and the verdict of the matrix of a
 *       quantifier fold carry the composite's statements themselves, within the {@code try} of a test without a fault;
 *       a test that asks whether the condition faults or deposits, whatever its verdict, keeps the composite's
 *       function, which its {@code try} calls, as the functions of the fault and of the deposit of a consequence do. A
 *       composite over one member whose verdict is that member's, as {@code ComputeHelpers.established} and
 *       {@code endVerdict} give it (ALLE, DE, TEN_MINSTE 1, PRECIES 1), is that member; a composite over no member is
 *       its verdict, {@code ComputeHelpers.endVerdict} over no member, where it stands; and a test whose condition is
 *       such a composite with the verdict true, a default without a condition, is no test: the rule has no
 *       {@code eval} for it. The sentence of a composite that goes into another function stays there as a comment
 *       line, above the member it is or above the body it gives, where it differs from that function's own.</li>
 *   <li>The constraints that decide which fact a pattern matches stay DRL constraints: the key of a fact, a null
 *       test, the rank of a control fact and an identity join, so that Drools indexes them. A test of a class goes
 *       through a route, since the text imports no bean class. A member of a bean stands as the name Drools reads
 *       the generator's getter under ({@link Namen#lidnamen}); a member Drools reads as something else under that
 *       name, a word of DRL or MVEL, is refused by name.</li>
 *   <li>A join meets nothing where a side reads null, as the identity join of the executable model does: where the
 *       pattern's side is a field and the other side can read null, a field of an object or a value a binding binds,
 *       the join tests the field for null first, since DRL finds two nulls equal.</li>
 *   <li>A read along a path is a function that walks the path with the routes {@code along} and {@code read}, null
 *       where a link is absent; never the null-safe navigation of the dialect, which drops the match instead.</li>
 *   <li>Every helper is called through its class, never through an {@code import function}: a helper named like a
 *       field of a fact shadows that field without a complaint (FABLE-2 §2.1). The text imports the classes of the
 *       branch and of the runtime it names, never a bean class, which the Java dialect cannot see: a bean stands with
 *       its full name in a pattern. A class of the JDK and an enumeration label stand with their full names.</li>
 *   <li>The local names of a consequence are fixed ({@code v}, {@code f}, {@code k}, {@code sonde}), and a function is
 *       named after its kind and its rule (card drl-function-names-from-rule): {@code <kind>_<rule>}, the kind being
 *       the word of what the function computes ({@code toets}, {@code samengesteld}, {@code afloop}, {@code waarde},
 *       {@code pad}, {@code fout}, …) and the rule part {@link Namen#ident} of the rule's name, without the code
 *       points Drools does not read in a name ({@link #namePart}), {@code x} where none remains. A kind of which the
 *       rule has more than one function is numbered from 1 in the order of the plan, right after the kind
 *       ({@code pad1_<rule>}, {@code pad2_<rule>}); a function that is the only one of its kind in its rule has no
 *       number ({@code toets_<rule>}). Rules with functions whose rule parts are the same are numbered from 1 in the
 *       order of the plan, after the rule part ({@code toets_<rule>_1}, {@code toets_<rule>_2}). A rule part has no
 *       {@code _}, so a name is never that of a global (an ident, or {@code rsDagsoort_} and one), of a member of a
 *       bean, of a local or of a word of DRL or MVEL, and it begins with its kind, a letter, also where the rule's
 *       name begins with a digit. The functions stand before their rule in the order of their places: one plan gives
 *       one text. Which piece of the text is a rule's, with its functions, the rendering gives beside the text
 *       ({@link TmsTranspiler.DrlPiece}), so that a reader reads no names.</li>
 * </ul>
 *
 * <p>The consequences it carries are those of the ordinary rules, the faults, the inconsistency, the writers that
 * disagree and the standings (step 2a), and those of the rules of a fold (step 3a): a member's contribution, and the
 * deposit, the fault of the filter and the fault of a member, with the member's place in the fold's order as a function
 * of the rule; and those of a uniqueness and of the §9.5 channel (step 3b): the keys, the violation, the fault of the
 * form of an input and the standing of the channel; and those of a defaults stack (step 4): the decision of a default
 * or a reconciliation, with its fault where it is computed, and the conflict of a reconciliation, each with the facts
 * of the stack. Every other kind it refuses loudly ({@link Refusal}).
 */
final class DrlRendering {

    /** The path of the text in the resource tree of a {@code KieHelper}: its folders follow the package, its extension
     *  gives the type. */
    static final String PATH = TmsTranspiler.PKG.replace('.', '/') + "/regels.drl";

    /** The fixed name of the probe: a local of a consequence, and the parameter of a function that a probe reaches. */
    private static final String DEPOSIT_FLAG = "sonde";

    /** A form of the plan this rendering does not carry, refused loudly with its kind, never left out: a bean class as
     *  the constant of a term, a day kind whose global is no Java identifier, and a member that Drools reads as
     *  something else where a pattern spells it. Every kind of consequence is carried since step 5; a new kind does not
     *  compile until the rendering spells it ({@link Rule#consequence}). */
    static final class Refusal extends RuntimeException {
        private final Reason reason;

        Refusal(Reason reason) {
            super(reason.text());
            this.reason = reason;
        }

        DroolsCode code() { return reason.code(); }

        Reason reason() { return reason; }

        /** The kind refused, such as the simple name of a consequence: the sentence part that fills the place
         *  {@code category} of the refusal. */
        String category() { return reason.msg().args().get("category").toString(); }
    }

    /** The classes the text imports, by their canonical names. */
    private final Set<String> imports = new TreeSet<>();
    /** The canonical name of each imported class by its simple name, so that two classes of one name fail loudly. */
    private final Map<String, String> bySimpleName = new HashMap<>();
    /** The globals of the day kinds the terms name, by their names, each with the node of its day kind, which carries
     *  the predicate the analysis compiled. */
    private final Map<String, Term.DayKind> dayKinds = new LinkedHashMap<>();
    private final StringBuilder rules = new StringBuilder();
    /** The piece of every rule rendered so far, in the order of the plan, its places counted from the beginning of
     *  {@link #rules}: the header that goes before them is known only once every rule has named its imports. */
    private final List<TmsTranspiler.DrlPiece> pieces = new ArrayList<>();

    private DrlRendering() {}

    /**
     * The DRL text of {@code plan}: the package, the imports, the globals of the plan and those of the day kinds its
     * terms name, then per rule, in the order of the plan, its functions and the rule, under the name of its plan. A
     * form this rendering does not carry is refused ({@link Refusal}), and so are two rules of one name, of which a
     * KieBase would keep one.
     */
    static String text(Modelplan plan) {
        return render(plan).text();
    }

    /** The DRL text of a plan ({@link #text}), and the predicate of every day kind whose global the text declares, by
     *  the name of the global: a session of the text sets each before it fires. The predicates are those the analysis
     *  compiled into its plan ({@code Term.DayKind}); the rendering only names their globals (decision 6). Beside them
     *  the piece of the text of every rule, in the order of the text ({@link TmsTranspiler.DrlPiece}). */
    record Rendered(String text, Map<String, java.util.function.Predicate<LocalDate>> dayKinds,
                    List<TmsTranspiler.DrlPiece> pieces) {}

    /** {@link #text} with the predicates of its day-kind globals and the pieces of its rules. */
    static Rendered render(Modelplan plan) {
        DrlRendering w = new DrlRendering();
        Set<String> ruleNames = new HashSet<>();
        // a first rendering of each rule gives the kinds of its functions in the order of their places: only the whole
        // rule tells whether a kind is numbered, and only every rule whether a rule part is, so the names follow from
        // those kinds and a second rendering writes them; the imports and day kinds the first notes, it notes again
        List<List<String>> kinds = new ArrayList<>();
        for (RulePlan r : plan.rules()) {
            // the header escapes a name one to one, so two rules of one name in the text are two plans of one name
            if (!ruleNames.add(r.name()))
                throw new IllegalStateException("twee regels van het plan heten " + str(r.name())
                        + ", en een KieBase houdt één regel per naam");
            kinds.add(w.new Rule(r, null).kinds());
        }
        List<FunctionNames> names = functionNames(plan.rules(), kinds);
        Set<String> functionNames = new HashSet<>();
        for (int n = 0; n < plan.rules().size(); n++) {
            for (String f : names.get(n).names())
                if (!functionNames.add(f)) throw new IllegalStateException("twee functies van de tekst heten " + f);
            w.new Rule(plan.rules().get(n), names.get(n)).renderRule();
        }
        Set<String> declared = new HashSet<>();
        StringBuilder globals = new StringBuilder();
        for (Modelplan.Global g : plan.globals()) {
            if (!declared.add(g.name()))
                throw new IllegalStateException("de global " + g.name() + " wordt twee keer gedeclareerd");
            globals.append("global ").append(w.typeName(g.javaClass())).append(' ').append(g.name()).append(";\n");
        }
        for (String d : w.dayKinds.keySet()) {
            if (!declared.add(d)) throw new IllegalStateException("de global " + d + " wordt twee keer gedeclareerd");
            globals.append("global java.util.function.Predicate ").append(d).append(";\n");
        }
        for (String d : declared)
            if (functionNames.contains(d)) throw new IllegalStateException("de functie " + d + " heet als een global");
        StringBuilder out = new StringBuilder("package ").append(TmsTranspiler.PKG).append(";\n\n");
        for (String i : w.imports) out.append("import ").append(i).append(";\n");
        if (!w.imports.isEmpty()) out.append('\n');
        if (!globals.isEmpty()) out.append(globals).append('\n');
        Map<String, java.util.function.Predicate<LocalDate>> predicates = new LinkedHashMap<>();
        w.dayKinds.forEach((global, d) -> predicates.put(global, d.datePredicate()));
        int header = out.length();
        List<TmsTranspiler.DrlPiece> pieces = new ArrayList<>();
        for (TmsTranspiler.DrlPiece p : w.pieces) {
            List<TmsTranspiler.DrlPiece.DrlFunction> functions = new ArrayList<>();
            for (TmsTranspiler.DrlPiece.DrlFunction f : p.functions())
                functions.add(new TmsTranspiler.DrlPiece.DrlFunction(f.name(), header + f.start(),
                        header + f.declaration(), header + f.end()));
            pieces.add(new TmsTranspiler.DrlPiece(p.rule(), header + p.start(), header + p.ruleStart(),
                    header + p.end(), functions));
        }
        return new Rendered(out.append(w.rules).toString(), java.util.Collections.unmodifiableMap(predicates),
                List.copyOf(pieces));
    }

    // ---------------- names ----------------

    private static boolean isBean(Class<?> k) {
        return k.getPackageName().equals(FactModelGenerator.PACKAGE);
    }

    /** The name the text gives class {@code k}: a bean by its full name, a class of {@code java.lang} by its simple
     *  name, any other class of the JDK by its full name, and a class of the branch or of the runtime by its simple
     *  name, imported. */
    private String typeName(Class<?> k) {
        if (k.isPrimitive()) return k.getName();
        if (k.isArray()) throw new IllegalStateException("een DRL-tekst spelt geen arraytype: " + k.getName());
        String canonical = k.getCanonicalName();
        if (isBean(k)) return canonical;
        if (k.getPackageName().startsWith("java."))
            return k.getPackageName().equals("java.lang") && k.getEnclosingClass() == null ? k.getSimpleName() : canonical;
        String earlier = bySimpleName.putIfAbsent(k.getSimpleName(), canonical);
        if (earlier != null && !earlier.equals(canonical))
            throw new IllegalStateException("twee klassen heten " + k.getSimpleName() + " in de DRL-tekst: " + earlier
                    + " en " + canonical);
        imports.add(canonical);
        return k.getSimpleName();
    }

    /** A call of route {@code r}, through its class. */
    private String route(Route r) {
        return typeName(r.javaClass()) + "." + r.method();
    }

    /** A call of function {@code f}, through its class. */
    private String function(StaticFunction f) {
        return typeName(f.javaClass()) + "." + f.method();
    }

    private static String str(String s) {
        return Namen.drlString(s);
    }

    /** The one public method of {@code javaClass} called {@code name}; the witnesses of the routes hold that there is one. */
    private static Method method(Class<?> javaClass, String name) {
        Method out = null;
        for (Method m : javaClass.getMethods())
            if (m.getName().equals(name)) {
                if (out != null) throw new IllegalStateException(javaClass.getName() + "." + name + " is overladen");
                out = m;
            }
        if (out == null) throw new IllegalStateException("geen publieke methode " + javaClass.getName() + "." + name);
        return out;
    }

    private static Class<?> wrap(Class<?> k) {
        if (!k.isPrimitive()) return k;
        if (k == boolean.class) return Boolean.class;
        if (k == int.class) return Integer.class;
        if (k == long.class) return Long.class;
        if (k == double.class) return Double.class;
        throw new IllegalStateException("geen omhulling voor " + k);
    }

    /** An expression of the Java dialect with its static type, null for the null literal. */
    private record Expr(String text, Class<?> type) {}

    /** {@code e} as an argument of a parameter of type {@code p}: cast where its static type is not that type. */
    private String cast(Class<?> p, Expr e) {
        if (p == Object.class) return e.text();
        Class<?> target = wrap(p);
        if (e.type() == null) return p.isPrimitive() ? "((" + typeName(target) + ") null)" : e.text();
        if (target.isAssignableFrom(wrap(e.type()))) return e.text();
        if (isBean(target)) throw new IllegalStateException("een functie kan niet naar de beanklasse " + target + " casten");
        return "((" + typeName(target) + ") " + e.text() + ")";
    }

    /** {@code e} as a Java truth value. */
    private static String asBoolean(Expr e) {
        return e.type() == boolean.class || e.type() == Boolean.class ? e.text() : "((Boolean) " + e.text() + ")";
    }

    /** A constant of the plan in the Java dialect, with its Java type. */
    private Expr constant(Object w) {
        if (w == null) return new Expr("null", null);
        if (w instanceof String s) return new Expr(str(s), String.class);
        if (w instanceof Boolean b) return new Expr(b ? "Boolean.TRUE" : "Boolean.FALSE", Boolean.class);
        if (w instanceof Integer i) return new Expr("Integer.valueOf(" + i + ")", Integer.class);
        if (w instanceof Long l) return new Expr("Long.valueOf(" + l + "L)", Long.class);
        if (w instanceof BigDecimal d) return new Expr("new java.math.BigDecimal(" + str(d.toString()) + ")", BigDecimal.class);
        if (w instanceof BigInteger i) return new Expr("new java.math.BigInteger(" + str(i.toString()) + ")", BigInteger.class);
        if (w instanceof Breuk b)
            return new Expr(typeName(Breuk.class) + ".van(new java.math.BigInteger(" + str(b.teller().toString())
                    + "), new java.math.BigInteger(" + str(b.noemer().toString()) + "))", Breuk.class);
        if (w instanceof LocalDate d) return new Expr("java.time.LocalDate.parse(" + str(d.toString()) + ")", LocalDate.class);
        if (w instanceof LocalDateTime d)
            return new Expr("java.time.LocalDateTime.parse(" + str(d.toString()) + ")", LocalDateTime.class);
        if (w instanceof Enum<?> e) return new Expr(enumlabel(e), e.getDeclaringClass());
        if (w instanceof Class<?> k) {
            if (isBean(k)) throw new Refusal(DroolsCode.DRL_BEAN_CLASS_CONSTANT.reason("category", DroolsCode.DRL_BEAN_CLASS_CONSTANT.part("bean-class")));
            return new Expr(typeName(k) + ".class", Class.class);
        }
        if (w instanceof List<?> l) {
            List<String> parts = new ArrayList<>();
            for (Object x : l) parts.add(constant(x).text());
            return new Expr("java.util.Arrays.asList(" + String.join(", ", parts) + ")", List.class);
        }
        throw new IllegalStateException("een constante van het type " + w.getClass().getName() + " in het plan");
    }

    /** An enumeration label by its full name. */
    private static String enumlabel(Enum<?> e) {
        return e.getDeclaringClass().getCanonicalName() + "." + e.name();
    }

    /** A constant of a field test in MVEL. */
    private String mvelConstant(Object w) {
        if (w == null) return "null";
        if (w instanceof String s) return str(s);
        if (w instanceof Boolean || w instanceof Integer) return w.toString();
        if (w instanceof ElementKey e) return key(e);
        if (w instanceof Enum<?> e) return enumlabel(e);
        throw new IllegalStateException("een veldtoets met een constante van het type " + w.getClass().getName());
    }

    /** The key of a member, as {@link ElementKey#of} makes every key of the plan. */
    private String key(ElementKey e) {
        return typeName(ElementKey.class) + ".of(" + str(e.objectType()) + ", " + str(e.name()) + ", "
                + e.characteristic() + ")";
    }

    /** The global of the day kind {@code d}: its predicate, a constant of the document, which the case runner sets
     *  under this name before the session fires. The analysis compiles one predicate per day kind, so two nodes of one
     *  global carry one predicate; any other pair fails. */
    private String dayKind(Term.DayKind d) {
        String name = d.name();
        String global = "rsDagsoort_" + Namen.ident(name, true);
        if (!global.codePoints().allMatch(Character::isJavaIdentifierPart))
            throw new Refusal(DroolsCode.DAY_KIND_GLOBAL_INVALID.reason("category", DroolsCode.DAY_KIND_GLOBAL_INVALID.part("global-not-identifier", "dayKind", name)));
        Term.DayKind earlier = dayKinds.putIfAbsent(global, d);
        if (earlier != null && !earlier.name().equals(name))
            throw new IllegalStateException("de dagsoorten '" + earlier.name() + "' en '" + name + "' delen de global "
                    + global);
        if (earlier != null && earlier.datePredicate() != d.datePredicate())
            throw new IllegalStateException("de dagsoort '" + name + "' heeft in het plan twee predicaten");
        return global;
    }

    /** The names of the functions of one rule by their places, and the kinds of the first rendering they follow from. */
    private record FunctionNames(List<String> kinds, List<String> names) {}

    /**
     * The names of the functions of every rule of {@code rules}, whose first rendering gave {@code kinds}: per function
     * its kind, its number among the functions of that kind in its rule where the rule has more than one, {@code _}
     * and the rule part ({@link #rulePart}); where more than one rule with functions has that rule part, {@code _} and
     * the rule's number among them follow, each counted from 1 in the order of the plan.
     */
    private static List<FunctionNames> functionNames(List<RulePlan> rules, List<List<String>> kinds) {
        List<String> parts = new ArrayList<>();
        Map<String, Integer> sharing = new HashMap<>();
        for (int n = 0; n < rules.size(); n++) {
            parts.add(rulePart(rules.get(n).name()));
            if (!kinds.get(n).isEmpty()) sharing.merge(parts.get(n), 1, Integer::sum);
        }
        Map<String, Integer> partsSeen = new HashMap<>();
        List<FunctionNames> out = new ArrayList<>();
        for (int n = 0; n < rules.size(); n++) {
            List<String> own = kinds.get(n);
            String part = parts.get(n);
            if (!own.isEmpty() && sharing.get(part) > 1) part = part + "_" + partsSeen.merge(part, 1, Integer::sum);
            Map<String, Integer> perKind = new HashMap<>(), kindsSeen = new HashMap<>();
            for (String kind : own) perKind.merge(kind, 1, Integer::sum);
            List<String> names = new ArrayList<>();
            for (String kind : own)
                names.add(kind + (perKind.get(kind) > 1 ? String.valueOf(kindsSeen.merge(kind, 1, Integer::sum)) : "")
                        + "_" + part);
            out.add(new FunctionNames(own, List.copyOf(names)));
        }
        return out;
    }

    /** The part of the names of its functions that the name of a rule gives: {@link Namen#ident} of it, without the code
     *  points Drools does not read in a name ({@link #namePart}), {@code x} where none remains. */
    private static String rulePart(String rule) {
        StringBuilder b = new StringBuilder();
        Namen.ident(rule, false).codePoints().filter(DrlRendering::namePart).forEach(b::appendCodePoint);
        return b.isEmpty() ? "x" : b.toString();
    }

    /** Per code point asked so far, whether Drools reads it as part of a name ({@link #namePart}). */
    private static final Map<Integer, Boolean> NAME_PARTS = new ConcurrentHashMap<>();

    /**
     * Whether Drools 8.44 reads code point {@code cp} as part of the name of a function wherever the text spells one:
     * its DRL lexer reads {@code f} followed by it as one name, and Java takes it in an identifier, as the compiler of
     * the functions and MVEL do. The lexer's table of the characters of a name is that of Java 5, Unicode 4.0 without
     * the characters outside the Basic Multilingual Plane, so it reads {@code ë}, {@code ş} and {@code 日} but not
     * {@code ȷ} (U+0237, Unicode 4.1), {@code ²} or {@code 𝔘}, where the text does not parse. Measured 2026-10-25 over
     * every letter and digit of Unicode: each of the 46100 that both take, in names of 150, compiles and fires in a
     * declaration, an {@code eval}, a binding and an MVEL consequence.
     */
    private static boolean namePart(int cp) {
        return NAME_PARTS.computeIfAbsent(cp, c -> {
            if (!Character.isJavaIdentifierPart(c)) return false;
            String name = "f" + Character.toString(c);
            DRL6Lexer lexer = new DRL6Lexer(new ANTLRStringStream(name));
            Token first = lexer.nextToken();
            return first.getType() == DRL6Lexer.ID && first.getText().equals(name)
                    && lexer.nextToken().getType() == Token.EOF && lexer.getErrors().isEmpty();
        });
    }

    // ---------------- the context of a function of a term ----------------

    /** What a function of a term sees: its parameters, in order, each with the expression the rule passes it; the
     *  parameter of each slot of the value array that the position passes; the probe's slot, -1 for none; and per
     *  binder of an enclosing {@code Let} the call of its binder function. A function nested in a term takes the
     *  parameters of the function it stands in, under the same names. */
    private static final class Context {
        private final List<String[]> parameters = new ArrayList<>();
        private final Map<Integer, String> slots = new HashMap<>();
        private final Map<Integer, String> binders = new HashMap<>();
        private int depositFlag = -1;

        void parameter(String type, String name, String argument) {
            for (String[] p : parameters)
                if (p[1].equals(name)) throw new IllegalStateException("de parameter " + name + " staat er twee keer");
            parameters.add(new String[]{type, name, argument});
        }

        boolean has(String name) {
            for (String[] p : parameters) if (p[1].equals(name)) return true;
            return false;
        }

        /** The declaration of the parameters. */
        String declaration() {
            List<String> d = new ArrayList<>();
            for (String[] p : parameters) d.add(p[0] + " " + p[1]);
            return String.join(", ", d);
        }

        /** The arguments of the call in the rule. */
        String arguments() {
            List<String> a = new ArrayList<>();
            for (String[] p : parameters) a.add(p[2]);
            return String.join(", ", a);
        }

        /** The arguments of the call of a nested function: the parameters themselves. */
        String pass() {
            List<String> a = new ArrayList<>();
            for (String[] p : parameters) a.add(p[1]);
            return String.join(", ", a);
        }

        /** This context with the cell {@code cell} of a {@code Let}, a local of that {@code Let}'s function. */
        Context withCell(String cell) {
            Context c = new Context();
            for (String[] p : parameters) c.parameters.add(new String[]{p[0], p[1], p[1]});
            c.slots.putAll(slots);
            c.binders.putAll(binders);
            c.depositFlag = depositFlag;
            c.parameter("Object", cell, cell);
            return c;
        }
    }

    // ---------------- one rule ----------------

    /** The rendering of the plan of one rule: its functions and its rule. */
    private final class Rule {
        private final RulePlan plan;
        /** The names of the functions of this rule by their places ({@link DrlRendering#functionNames}), null in the
         *  first rendering, which gives only their kinds ({@link #kinds()}). */
        private final FunctionNames given;
        /** The kind of the function at each place, as {@link #name} met it. */
        private final List<String> kinds = new ArrayList<>();
        /** The functions of this rule, in the order of their places. */
        private final List<String> functions = new ArrayList<>();
        /** Per place of {@link #functions} the name of its function and the length of the comment above its
         *  declaration, 0 for none, from which {@link #renderRule} gives each function its places
         *  ({@link TmsTranspiler.DrlPiece.DrlFunction}): noted where they are made, never read back off the text. */
        private final List<String> functionNames = new ArrayList<>();
        private final List<Integer> commentLengths = new ArrayList<>();
        /** The sentences of the composites that went into another function without one of their own, in the order
         *  they were met: a composite over one member that is that member, one over no member that is its verdict, and
         *  one whose statements a test carries. Each goes as a comment line into the body of the function it went
         *  into ({@link #sentences}), so that no sentence of the source is lost with its function. */
        private final List<String> absorbed = new ArrayList<>();
        /** Per place of {@link #functions} the length of {@link #absorbed} when it was reserved: the sentences after it
         *  went into that function or into one written inside it. */
        private final List<Integer> absorbedMarks = new ArrayList<>();
        /** The variables the patterns of this rule bind to their objects, as far as they are in scope. */
        private Set<String> patternVariables = new HashSet<>();

        Rule(RulePlan plan, FunctionNames given) {
            this.plan = plan;
            this.given = given;
        }

        /** The kinds of the functions of this rule in the order of their places: the first rendering, whose text goes
         *  nowhere. */
        List<String> kinds() {
            leftSide();
            consequence(plan.consequence());
            return List.copyOf(kinds);
        }

        /** The items of the left-hand side, but a test that holds whatever the rule binds. */
        private List<String> leftSide() {
            List<String> when = new ArrayList<>();
            for (LhsItem item : plan.leftSide())
                if (!(item instanceof TestItem t && holdsWhatever(t))) when.add(item(item));
            return when;
        }

        void renderRule() {
            List<String> when = leftSide();
            String then = consequence(plan.consequence());
            if (!absorbed.isEmpty())
                throw new IllegalStateException("de zinnen " + absorbed + " van " + plan.name()
                        + " staan in geen functie");
            if (!kinds.equals(given.kinds()))
                throw new IllegalStateException("de functies van " + plan.name() + " zijn " + kinds
                        + ", in de eerste weergave " + given.kinds());
            int start = rules.length();
            List<TmsTranspiler.DrlPiece.DrlFunction> placed = new ArrayList<>();
            for (int place = 0; place < functions.size(); place++) {
                String f = functions.get(place);
                if (f == null || functionNames.get(place) == null)
                    throw new IllegalStateException("een functie van " + plan.name() + " is niet geschreven");
                int at = rules.length();
                rules.append(f).append('\n');
                placed.add(new TmsTranspiler.DrlPiece.DrlFunction(functionNames.get(place), at,
                        at + commentLengths.get(place), rules.length()));
            }
            int ruleStart = rules.length();
            // the plan's name escaped as a string literal, which the parser reads back as that name (RuleNameWitnessTest):
            // formOf, the scenario report and the creation driver look a fired rule up by the name of its plan
            rules.append("rule ").append(str(plan.name())).append("\n");
            rules.append("    dialect \"mvel\"\n");
            if (plan.agendaGroup() != null) rules.append("    agenda-group ").append(str(plan.agendaGroup())).append('\n');
            rules.append("when\n");
            for (String w : when) rules.append("    ").append(w).append('\n');
            rules.append("then\n").append(then).append("end\n\n");
            pieces.add(new TmsTranspiler.DrlPiece(plan.name(), start, ruleStart, rules.length(), placed));
        }

        /** A place for a function of this rule, reserved where its node is met, so the functions stand in the order of
         *  the plan. */
        private int reserveer() {
            functions.add(null);
            functionNames.add(null);
            commentLengths.add(null);
            kinds.add(null);
            absorbedMarks.add(absorbed.size());
            return functions.size() - 1;
        }

        /** The name of the function of kind {@code kind} at {@code place}, noted for its piece: every function is named
         *  once, right after its place is reserved. The second rendering gives the name
         *  {@link DrlRendering#functionNames} made of the kinds of the first; the first gives the kind, a name its text
         *  does not keep. */
        private String name(String kind, int place) {
            if (kinds.set(place, kind) != null)
                throw new IllegalStateException("de functie op plaats " + place + " van " + plan.name()
                        + " krijgt een tweede naam: " + kind);
            if (given != null && place >= given.names().size())
                throw new IllegalStateException("de functie op plaats " + place + " van " + plan.name()
                        + " is er in de eerste weergave niet: " + kind);
            String name = given == null ? kind : given.names().get(place);
            functionNames.set(place, name);
            return name;
        }

        /** Writes the function at {@code place}: the RegelSpraak sentence {@code source} as a comment, where the node has
         *  one, its head and its body; above the body the sentences of the composites that went into it and that no
         *  statement of the body placed ({@link #sentences}). */
        private void fill(int place, String source, String head, String body) {
            StringBuilder f = new StringBuilder();
            if (source != null) f.append("// ").append(Namen.commentSafe(source)).append('\n');
            commentLengths.set(place, f.length());
            f.append("function ").append(head).append(" {\n")
                    .append(sentences(absorbedMarks.get(place), "    ", source)).append(body).append("}\n");
            functions.set(place, f.toString());
        }

        /** The comment lines, indented by {@code indent}, of the sentences {@link #absorbed} since {@code mark}, which
         *  leave it: each that differs from {@code host}, the sentence of the function or composite they went into, and
         *  from the line before it. */
        private String sentences(int mark, String indent, String host) {
            List<String> mine = absorbed.subList(mark, absorbed.size());
            StringBuilder out = new StringBuilder();
            String previous = host;
            for (String sentence : mine) {
                if (sentence.equals(host) || sentence.equals(previous)) continue;
                // a comment in a body reaches the Java compiler, which reads a backslash-u as a character escape even
                // there, a line break among them; above a function the comment is DRL's and stays as it is
                out.append(indent).append("// ").append(Namen.commentSafe(sentence).replace("\\u", "\\ u"))
                        .append('\n');
                previous = sentence;
            }
            mine.clear();
            return out.toString();
        }

        /** The variable of slot {@code slot}: the one a binding of the left-hand side binds. */
        private String variable(int slot) {
            Read l = plan.readings().slots().get(slot);
            if (l.name() == null)
                throw new IllegalStateException("slot " + slot + " van " + plan.name() + " heeft geen variabele: " + l);
            return l.name();
        }

        // ---------------- the left-hand side ----------------

        private String item(LhsItem item) {
            return switch (item) {
                case Subject o -> pattern(o.name(), o.beanClass(), o.alpha(), o.joins(), o.betaTests(), List.of(),
                        o.bindings());
                case FactPattern p -> pattern(p);
                case Not x -> "not( " + quantified(x.patterns()) + " )";
                case Exists x -> "exists( " + quantified(x.patterns()) + " )";
                case Accumulation a -> accumulation(a);
                case TestItem t -> test(t);
            };
        }

        private String pattern(FactPattern p) {
            return pattern(p.name(), p.factClass(), p.alpha(), p.joins(), p.betaTests(), p.afterAlpha(), p.bindings());
        }

        /** A pattern: its constraints on its object, its identity joins, its constraints between its object and
         *  another, its constraints on its object after those, and its bindings, in that order. */
        private String pattern(String name, Class<?> javaClass, List<Alpha> alpha, List<Join> joins,
                               List<BetaTest> betaTests, List<Alpha> afterAlpha, List<Binding> bindings) {
            List<String> c = new ArrayList<>();
            for (Alpha a : alpha) c.add(alpha(javaClass, a));
            for (Join j : joins) c.addAll(join(javaClass, j));
            for (BetaTest b : betaTests) c.add(betaTest(b));
            for (Alpha a : afterAlpha) c.add(alpha(javaClass, a));
            for (Binding b : bindings) c.add(binding(javaClass, b));
            patternVariables.add(name);
            return name + " : " + typeName(javaClass) + (c.isEmpty() ? "()" : "( " + String.join(", ", c) + " )");
        }

        /** The patterns of a negation or an existence; what they bind lives only inside it. */
        private String quantified(List<FactPattern> patterns) {
            Set<String> outside = new HashSet<>(patternVariables);
            List<String> p = new ArrayList<>();
            for (FactPattern x : patterns) p.add(pattern(x));
            patternVariables = outside;
            return String.join(" and ", p);
        }

        private String accumulation(Accumulation a) {
            String source = pattern(a.source());
            List<String> f = new ArrayList<>();
            for (Accumulation.Aggregation x : a.aggregations()) {
                String over = x.source() == null ? a.source().name() : x.source();
                f.add(x.target() + " : " + switch (x.kind()) {
                    case COUNT -> "count()";
                    case SUM -> "sumBD(" + over + ")";
                    case MINIMUM -> "min(" + over + ")";
                    case MAXIMUM -> "max(" + over + ")";
                    case LIST -> "collectList(" + over + ")";
                });
            }
            return "accumulate( " + source + "; " + String.join(", ", f) + " )";
        }

        /** An identity join, which meets nothing where a side reads null ({@link Join}): where the pattern's side is a
         *  field and the other side can read null, a field of an object or a value that is no pattern's object, the
         *  field is tested for null first, since DRL finds two nulls equal. */
        private List<String> join(Class<?> javaClass, Join j) {
            String left = j.field() == null ? "this" : fieldName(javaClass, j.property(), j.field());
            String right = j.targetField() == null ? j.target() : j.target() + "." + fieldName(j.targetField().javaClass(),
                    j.targetField().property(), j.targetField().name());
            boolean rightEmpty = j.targetField() != null || !patternVariables.contains(j.target());
            List<String> out = new ArrayList<>();
            if (j.field() != null && rightEmpty) out.add(left + " != null");
            out.add(left + " == " + right);
            return out;
        }

        /** A constraint between the pattern's object and the object or value another pattern binds: a function that
         *  calls the plan's function with its constants. */
        private String betaTest(BetaTest b) {
            int place = reserveer();
            String name = name("beta", place);
            Method m = method(b.function().javaClass(), b.function().method());
            Class<?>[] p = m.getParameterTypes();
            if (p.length != 2 + b.constants().size())
                throw new IllegalStateException(b.function() + " neemt " + p.length + " argumenten, niet "
                        + (2 + b.constants().size()));
            List<String> a = new ArrayList<>(List.of(cast(p[0], new Expr("eigen", Object.class)),
                    cast(p[1], new Expr("ander", Object.class))));
            for (int i = 0; i < b.constants().size(); i++) a.add(cast(p[2 + i], constant(b.constants().get(i).value())));
            Expr call = new Expr(function(b.function()) + "(" + String.join(", ", a) + ")", m.getReturnType());
            fill(place, null, "boolean " + name + "(Object eigen, Object ander)", "    return " + asBoolean(call)
                    + ";\n");
            return name + "(this, " + b.target() + ")";
        }

        /** A value a pattern binds: a field of a fact of the branch as it is; a member of a bean by its name in DRL,
         *  or by the route {@code read} where the pattern's class does not declare it, read as its form says; along a path
         *  a function that walks it. */
        private String binding(Class<?> javaClass, Binding b) {
            if (!b.path().isEmpty())
                return b.target() + " : (" + pathFunction(b.path(), b.field().property(), b.form(), b.category()) + "(this))";
            if (!isBean(javaClass)) {
                if (b.form() != Binding.ReadForm.VALUE)
                    throw new IllegalStateException("een feit van de tak wordt gelezen zoals het is: " + b);
                return b.target() + " : " + b.field().property();
            }
            boolean own = b.field().javaClass().isAssignableFrom(javaClass);
            String read = own ? fieldName(javaClass, b.field().property(), b.field().name())
                    : route(Route.READ) + "(this, " + str(b.field().property()) + ")";
            if (b.form() == Binding.ReadForm.VALUE) return b.target() + " : " + (own ? read : "(" + read + ")");
            return b.target() + " : (" + readForm(read, b.form(), b.category()) + ")";
        }

        /** How the text spells the field {@code property} of an object of {@code javaClass}: a component of a fact of
         *  the branch by its name, a member of a bean by the name Drools reads its getter under
         *  ({@link Namen#lidnamen}); a member {@code name} that Drools reads as something else where a pattern spells
         *  its name is refused, by name, since a constraint on it could hold or fail without a word. */
        private String fieldName(Class<?> javaClass, String property, String name) {
            if (!isBean(javaClass)) return property;
            String drl = Namen.lidnamen(property).drlProperty();
            if (drl == null) throw new Refusal(DroolsCode.DRL_MEMBER_NO_PROPERTY.reason("category", DroolsCode.DRL_MEMBER_NO_PROPERTY.part("member", "member", str(name))));
            return drl;
        }

        /** {@code read} read as {@code form}, through its route. */
        private String readForm(String read, Binding.ReadForm form, Class<?> category) {
            return switch (form) {
                case VALUE -> read;
                case WAAR -> route(Route.WAAR) + "(" + read + ")";
                case PERIODS -> route(Route.PERIODS) + "(" + read + ")";
                case IS_SET -> route(Route.IS_SET) + "(" + read + ", " + (category == null ? "null" : str(category.getName()))
                        + ")";
            };
        }

        /** A function of the object a pattern matches that walks the reference fields {@code path} with the route
         *  {@code along}, null where a link is absent, and reads of the object reached the member {@code property}
         *  (the object itself where it is null) as {@code form} says. */
        private String pathFunction(List<Binding.Field> path, String property, Binding.ReadForm form, Class<?> category) {
            int place = reserveer();
            String name = name("pad", place);
            List<String> steps = new ArrayList<>();
            for (Binding.Field v : path) steps.add(str(v.property()));
            String along = route(Route.ALONG) + "(o, java.util.Arrays.asList(" + String.join(", ", steps) + "))";
            if (property == null && form == Binding.ReadForm.VALUE) {
                fill(place, null, "Object " + name + "(Object o)", "    return " + along + ";\n");
                return name;
            }
            String read = property == null ? "bereikt" : route(Route.READ) + "(bereikt, " + str(property) + ")";
            fill(place, null, "Object " + name + "(Object o)", "    Object bereikt = " + along + ";\n"
                    + "    return bereikt == null ? null : " + readForm(read, form, category) + ";\n");
            return name;
        }

        /** An alpha constraint: its field tests in order, as one constraint. */
        private String alpha(Class<?> javaClass, Alpha a) {
            List<String> t = new ArrayList<>();
            for (Alpha.FieldTest v : a.tests()) t.add(fieldTest(javaClass, v));
            return String.join(" && ", t);
        }

        private String fieldTest(Class<?> javaClass, Alpha.FieldTest t) {
            String read;
            if (!t.path().isEmpty()) {
                read = pathFunction(t.path(), null, t.form(), null) + "(this)";
            } else {
                // a member the pattern's class does not declare is read through the class of the object itself
                String base = t.field() == null ? "this"
                        : t.declarant() != null ? route(Route.READ) + "(this, " + str(t.property()) + ")"
                        : fieldName(javaClass, t.property(), t.field());
                read = readForm(base, t.form(), null);
            }
            Object w = t.value();
            return switch (t.op()) {
                case GELIJK -> read + " == " + mvelConstant(w);
                case NOT_EQUAL -> read + " != " + mvelConstant(w);
                case AT_LEAST -> read + " >= " + ((Number) w).intValue();
                case UNDER -> read + " < " + ((Number) w).intValue();
                case WITHIN -> {
                    Validity g = (Validity) w;
                    yield route(Route.WITHIN) + "(" + read + ", " + (g.from() == null ? "null" : str(g.from().toString()))
                            + ", " + (g.until() == null ? "null" : str(g.until().toString())) + ")";
                }
                case IS_A -> route(Route.IS_A) + "(" + read + ", " + str(((Class<?>) w).getName()) + ")";
                case LEEG -> read + " == null";
                case GEVULD -> read + " != null";
                case CLASS_NOT_IN -> {
                    List<String> names = new ArrayList<>();
                    for (Object k : (Collection<?>) w) names.add(str(((Class<?>) k).getName()));
                    yield route(Route.CLASS_NOT_IN) + "(" + read + ", " + list(names) + ")";
                }
                case CLASS_IS -> route(Route.CLASS_IS) + "(" + read + ", " + str(((Class<?>) w).getName()) + ")";
            };
        }

        /** A list of the MVEL expressions {@code elements}: through a route of its length up to six, else a function
         *  that returns it. */
        private String list(List<String> elements) {
            if (elements.size() <= 6)
                return route(Route.valueOf("LIST" + elements.size())) + "(" + String.join(", ", elements) + ")";
            int place = reserveer();
            String name = name("lijst", place);
            fill(place, null, "java.util.List " + name + "()", "    return java.util.Arrays.asList("
                    + String.join(", ", elements) + ");\n");
            return name + "()";
        }

        /**
         * The test of the rule: a function that asks of the condition what its role says, called in an
         * {@code eval}. Where it asks the condition's verdict, that it holds or holds without a fault, it returns that
         * verdict, and so carries the statements of a composite itself ({@link #returning}); where it asks whether the
         * condition faults or deposits, whatever its verdict, its {@code try} evaluates the condition and calls the
         * function of a composite, as the functions of the fault and of the deposit of a consequence do.
         */
        private String test(TestItem t) {
            int place = reserveer();
            String name = name("toets", place);
            boolean deposit = t.role() == TestItem.Role.DEPOSIT;
            Context c = context(t.slots(), deposit ? t.depositFlag() : -1, route(Route.FRESH_DEPOSIT_FLAG) + "()", t.test());
            String caught = "        " + route(Route.CAUGHT) + "(e);\n";
            String body = switch (t.role()) {
                case CONDITION, VORM -> returning(t.test(), c, "    ");
                case WITHOUT_FAULT -> tryCatch(returning(t.test(), c, "        "), caught + "        return false;\n");
                case FAULT -> tryCatch("        boolean uitkomst = " + bool(t.test(), c) + ";\n        return false;\n",
                        caught + "        return true;\n");
                case DEPOSIT -> tryCatch("        boolean uitkomst = " + bool(t.test(), c) + ";\n", caught)
                        + "    return " + route(Route.REPORTED) + "(" + DEPOSIT_FLAG + ");\n";
            };
            fill(place, t.test().source(), "boolean " + name + "(" + c.declaration() + ")", body);
            return "eval( " + name + "(" + c.arguments() + ") )";
        }

        /** Whether test {@code t} holds whatever the rule binds, so that the rule has no test for it: it asks the
         *  verdict of a composite over no member, a default without a condition, and that verdict is true. */
        private static boolean holdsWhatever(TestItem t) {
            return (t.role() == TestItem.Role.CONDITION || t.role() == TestItem.Role.VORM
                    || t.role() == TestItem.Role.WITHOUT_FAULT)
                    && unwrapped(t.test()) instanceof Term.Compound s && s.members().isEmpty()
                    && ComputeHelpers.endVerdict(s.quantifier(), s.threshold(), 0, 0);
        }

        /** Condition {@code t} with each composite of one member whose verdict is that member's replaced by that
         *  member, outside in. */
        private static Term unwrapped(Term t) {
            while (t instanceof Term.Compound s && s.members().size() == 1 && givesMemberVerdict(s))
                t = s.members().get(0);
            return t;
        }

        /** Whether the verdict of composite {@code s} over one member is that member's: no stop trigger settles it
         *  before the member is read, and the end verdict is false where the member does not hold and true where it
         *  does. Asked of {@link ComputeHelpers#established} and {@link ComputeHelpers#endVerdict}, which the
         *  evaluation of the composite calls, so that it holds under ALLE, DE, TEN_MINSTE 1 and PRECIES 1, and under no
         *  other quantifier and threshold (GEEN over one member is its negation, TEN_HOOGSTE 1 holds unread). */
        private static boolean givesMemberVerdict(Term.Compound s) {
            return ComputeHelpers.established(s.quantifier(), s.threshold(), 0, 0, 1) == null
                    && !ComputeHelpers.endVerdict(s.quantifier(), s.threshold(), 0, 1)
                    && ComputeHelpers.endVerdict(s.quantifier(), s.threshold(), 1, 1);
        }

        /** A try with {@code tryBody} as its body and {@code catchBody} as the body of its catch of a
         *  {@code RuntimeException e}. */
        private String tryCatch(String tryBody, String catchBody) {
            return "    try {\n" + tryBody + "    } catch (RuntimeException e) {\n" + catchBody + "    }\n";
        }

        /** The context of a function of a position: a parameter per slot the position passes, in its order; the probe
         *  where the position has one, with {@code depositFlagArgument} as what the rule passes; and the global of each day
         *  kind the terms name. */
        private Context context(List<Integer> slots, int depositFlag, String depositFlagArgument, Term... terms) {
            Context c = new Context();
            for (int s : slots) {
                String v = variable(s);
                c.parameter("Object", v, v);
                c.slots.put(s, v);
            }
            if (depositFlag >= 0) {
                c.parameter("Object", DEPOSIT_FLAG, depositFlagArgument);
                c.depositFlag = depositFlag;
            }
            Set<String> globals = new LinkedHashSet<>();
            for (Term t : terms) dayKinds(t, globals);
            for (String d : globals) c.parameter("java.util.function.Predicate", d, d);
            return c;
        }

        // ---------------- the terms ----------------

        /** {@code t}, a condition, as a Java truth value. */
        private String bool(Term t, Context c) {
            if (t instanceof Term.Compound s) return compound(s, c);
            if (t instanceof Term.WithoutFault z) return withoutFault(z, c);
            return asBoolean(value(t, c));
        }

        /** {@code t} as a Java expression in the function whose context is {@code c}. */
        private Expr value(Term t, Context c) {
            return switch (t) {
                case Term.Const k -> constant(k.value());
                case Term.SlotValue l -> {
                    String p = c.slots.get(l.slot());
                    if (p == null)
                        throw new IllegalStateException("de term leest slot " + l.slot() + ", dat haar plaats in "
                                + plan.name() + " niet doorgeeft");
                    yield new Expr(p, Object.class);
                }
                case Term.Input i -> new Expr(readForm(route(Route.READ) + "(" + value(i.object(), c).text() + ", "
                        + str(i.field().property()) + ")", i.form(), null), Object.class);
                case Term.Binder b -> {
                    String call = c.binders.get(b.slot());
                    if (call == null) throw new IllegalStateException("de binder " + b.name() + " buiten zijn Let");
                    yield new Expr(call, Object.class);
                }
                case Term.Let l -> new Expr(let(l, c), Object.class);
                case Term.Choice k -> new Expr("(" + bool(k.condition(), c) + " ? (Object) " + value(k.then(), c).text()
                        + " : (Object) " + value(k.otherwise(), c).text() + ")", Object.class);
                case Term.Call a -> call(a.function(), a.arguments().stream().map(x -> value(x, c)).toList());
                case Term.Restate h -> call(h.function(), h.denominator() == null
                        ? List.of(value(h.term(), c), constant(h.counter()))
                        : List.of(value(h.term(), c), constant(h.counter()), constant(h.denominator())));
                case Term.Division d -> {
                    Expr left = value(d.left(), c), right = value(d.right(), c);
                    yield call(d.function(), List.of(left, right, depositFlag(d.depositFlag(), c)));
                }
                case Term.SquareRoot w -> call(w.function(), List.of(value(w.radicand(), c), depositFlag(w.depositFlag(), c)));
                case Term.Fail f -> routeCall(Route.FAIL, List.of(constant(f.sentence())));
                case Term.Required v -> routeCall(Route.REQUIRED, List.of(value(v.value(), c), constant(v.sentence())));
                case Term.TextComposition s -> {
                    List<String> parts = new ArrayList<>();
                    for (Term x : s.parts()) parts.add(value(x, c).text());
                    yield routeCall(Route.TEXT_COMPOSITION, List.of(new Expr("java.util.Arrays.asList("
                            + String.join(", ", parts) + ")", List.class)));
                }
                case Term.ListedFold g -> {
                    Expr to = new Expr("null", null);
                    for (Term x : g.operands()) to = call(g.function(), List.of(to, value(x, c)));
                    yield to;
                }
                case Term.FoldRead v -> new Expr(foldRead(v, c), Object.class);
                case Term.Compound s -> new Expr("Boolean.valueOf(" + compound(s, c) + ")", Boolean.class);
                case Term.QuantifierRead q -> new Expr(quantifierRead(q, c), Object.class);
                case Term.WithoutFault z -> new Expr("Boolean.valueOf(" + withoutFault(z, c) + ")", Boolean.class);
                case Term.DayKind d -> {
                    String global = dayKind(d);
                    if (!c.has(global)) throw new IllegalStateException("de dagsoort " + d.name() + " buiten haar context");
                    yield new Expr(global, java.util.function.Predicate.class);
                }
            };
        }

        /** The probe a place reports to: the position's, where the plan names it, or none with slot -1. */
        private Expr depositFlag(int slot, Context c) {
            if (slot < 0) return new Expr("null", null);
            if (slot != c.depositFlag)
                throw new IllegalStateException("een plaats in " + plan.name() + " meldt aan slot " + slot
                        + ", niet aan de sonde van haar positie");
            return new Expr(DEPOSIT_FLAG, Object.class);
        }

        /** A call of {@code f} on {@code arguments}, evaluated in that order, each cast as its parameter takes it. */
        private Expr call(StaticFunction f, List<Expr> arguments) {
            Method m = method(f.javaClass(), f.method());
            return new Expr(function(f) + "(" + argumentList(m, arguments) + ")", m.getReturnType());
        }

        private Expr routeCall(Route r, List<Expr> arguments) {
            Method m = method(r.javaClass(), r.method());
            return new Expr(route(r) + "(" + argumentList(m, arguments) + ")", m.getReturnType());
        }

        private String argumentList(Method m, List<Expr> arguments) {
            Class<?>[] p = m.getParameterTypes();
            if (p.length != arguments.size())
                throw new IllegalStateException(m.getName() + " neemt " + p.length + " argumenten, niet "
                        + arguments.size());
            List<String> a = new ArrayList<>();
            for (int i = 0; i < p.length; i++) a.add(cast(p[i], arguments.get(i)));
            return String.join(", ", a);
        }

        /** The statements, indented by {@code indent}, that return the verdict of condition {@code t} in the function
         *  that asks it: those of its composite, where it is one over a member or more once the composites of one
         *  member that are their member are that member ({@link #member}); else the return of the condition. */
        private String returning(Term t, Context c, String indent) {
            Term m = member(t);
            if (m instanceof Term.Compound s && !s.members().isEmpty()) {
                if (s.source() != null) absorbed.add(s.source());
                return compoundBody(s, c, indent);
            }
            return indent + "return " + bool(m, c) + ";\n";
        }

        /** A composite: the function of its statements ({@link #compoundBody}), called where it stands. A composite
         *  over one member whose verdict is that member's is that member ({@link #member}), and one over no member is
         *  its verdict, a constant: neither has a function, and its sentence goes into the function it stands in. */
        private String compound(Term.Compound s, Context c) {
            Term m = member(s);
            if (m != s) return bool(m, c);
            if (s.members().isEmpty()) {
                if (s.source() != null) absorbed.add(s.source());
                return route(Route.END_VERDICT) + "(" + enumlabel(s.quantifier()) + ", " + s.threshold() + ", 0, 0)";
            }
            int place = reserveer();
            String name = name("samengesteld", place);
            fill(place, s.source(), "boolean " + name + "(" + c.declaration() + ")", compoundBody(s, c, "    "));
            return name + "(" + c.pass() + ")";
        }

        /** The statements of composite {@code s} over a member or more, indented by {@code indent}: its members read in
         *  order, the stop triggers of RUNTIME-SEMANTICS ch. 5.10 tested before each, the quantifier's verdict where
         *  none stops it, each returned. The sentence of a composite that went into a member stands above the member's
         *  test ({@link #sentences}). */
        private String compoundBody(Term.Compound s, Context c, String indent) {
            String kw = enumlabel(s.quantifier());
            int n = s.members().size();
            StringBuilder b = new StringBuilder(indent + "int raak = 0;\n" + indent + "Boolean vast;\n");
            for (int i = 0; i < n; i++) {
                b.append(indent).append("vast = ").append(route(Route.ESTABLISHED)).append("(").append(kw).append(", ")
                        .append(s.threshold()).append(", raak, ").append(i).append(", ").append(n - i).append(");\n");
                b.append(indent).append("if (vast != null) {\n").append(indent).append("    return vast;\n")
                        .append(indent).append("}\n");
                int mark = absorbed.size();
                String member = bool(s.members().get(i), c);
                b.append(sentences(mark, indent, s.source()));
                b.append(indent).append("if (").append(member).append(") {\n").append(indent).append("    raak++;\n")
                        .append(indent).append("}\n");
            }
            b.append(indent).append("return ").append(route(Route.END_VERDICT)).append("(").append(kw).append(", ")
                    .append(s.threshold()).append(", raak, ").append(n).append(");\n");
            return b.toString();
        }

        /** Condition {@code t} with each composite of one member whose verdict is that member's replaced by that
         *  member ({@link #unwrapped}); the sentences of those composites are {@link #absorbed}. */
        private Term member(Term t) {
            Term m = unwrapped(t);
            for (Term u = t; u != m; u = ((Term.Compound) u).members().get(0))
                if (u.source() != null) absorbed.add(u.source());
            return m;
        }

        /** A condition where its evaluation answers, and not holding where it faults. */
        private String withoutFault(Term.WithoutFault z, Context c) {
            int place = reserveer();
            String name = name("zonderfout", place);
            String b = bool(z.condition(), c);
            fill(place, z.source(), "boolean " + name + "(" + c.declaration() + ")", tryCatch("        return " + b + ";\n",
                    "        " + route(Route.CAUGHT) + "(e);\n        return false;\n"));
            return name + "(" + c.pass() + ")";
        }

        /** The evaluation of a fold in the rule that reads it: its filter's deposit heard by the position's probe, then
         *  the first filter fault, then the first member fault, then the fold's value. */
        private String foldRead(Term.FoldRead v, Context c) {
            int place = reserveer();
            String name = name("vouwlezing", place);
            StringBuilder b = new StringBuilder();
            if (v.depositFlag() >= 0) {
                if (v.deposits() < 0)
                    throw new IllegalStateException("een vouwlezing met een sonde en zonder deponeringen in " + plan.name());
                b.append("    ").append(route(Route.REPORT_FILTER_DEPOSITS)).append("(")
                        .append(depositFlag(v.depositFlag(), c).text()).append(", ").append(slot(v.deposits(), c))
                        .append(");\n");
            }
            if (v.filterFault() >= 0) {
                String f = slot(v.filterFault(), c);
                b.append("    if (").append(f).append(" != null) {\n        throw ((").append(typeName(FilterFault.class))
                        .append(") ").append(f).append(").fault();\n    }\n");
            }
            if (v.memberFault() >= 0) {
                String f = slot(v.memberFault(), c);
                b.append("    if (").append(f).append(" != null) {\n        throw ((").append(typeName(MemberFault.class))
                        .append(") ").append(f).append(").fault();\n    }\n");
            }
            b.append("    return ").append(value(v.outcome(), c).text()).append(";\n");
            fill(place, v.source(), "Object " + name + "(" + c.declaration() + ")", b.toString());
            return name + "(" + c.pass() + ")";
        }

        private String slot(int slot, Context c) {
            String p = c.slots.get(slot);
            if (p == null)
                throw new IllegalStateException("slot " + slot + " staat niet in de context van een term van " + plan.name());
            return p;
        }

        /** The verdict of a quantifier fold whose matrix can fault or deposit, through its function. */
        private String quantifierRead(Term.QuantifierRead q, Context c) {
            int place = reserveer();
            String name = name("kwantorlezing", place);
            Expr call = call(q.function(), List.of(value(q.count(), c), value(q.contributions(), c),
                    constant(q.quantifier()), constant(q.threshold()), depositFlag(q.depositFlag(), c)));
            fill(place, q.source(), "Object " + name + "(" + c.declaration() + ")", "    return " + call.text() + ";\n");
            return name + "(" + c.pass() + ")";
        }

        /**
         * A {@code Let} by need (RUNTIME-SEMANTICS ch. 10.6): a function per {@code Let}, which makes the cell of its
         * binder and evaluates its body, and a function per binder, which evaluates the value in the enclosing scope
         * at the first read and serves that result to every further read. A leeg value raises the barrier of that cell,
         * which the {@code Let}'s own function catches as leeg; a barrier of an enclosing {@code Let} passes on.
         */
        private String let(Term.Let l, Context c) {
            int place = reserveer();
            String name = name("let", place);
            int binderPlace = reserveer();
            String binder = name("binder", binderPlace);
            String cell = "cel_" + l.slot();
            Context inside = c.withCell(cell);
            inside.binders.put(l.slot(), binder + "(" + inside.pass() + ")");
            String value = value(l.value(), c).text();
            // the binder before the body, so that the sentences the value's terms leave go into the binder
            fill(binderPlace, l.value().source(), "Object " + binder + "(" + inside.declaration() + ")",
                    "    if (!" + route(Route.BINDER_CELL_FILLED) + "(" + cell + ")) {\n        "
                            + route(Route.FILL_BINDER_CELL) + "(" + cell + ", " + value + ");\n    }\n    return "
                            + route(Route.BINDER_VALUE) + "(" + cell + ");\n");
            String body = value(l.body(), inside).text();
            fill(place, l.source(), "Object " + name + "(" + c.declaration() + ")",
                    "    Object " + cell + " = " + route(Route.BINDER_CELL) + "();\n    try {\n        return " + body
                            + ";\n    } catch (" + typeName(EmptyBarrier.class) + " barriere) {\n        return "
                            + route(Route.INTERCEPTED) + "(barriere, " + cell + ");\n    }\n");
            return name + "(" + c.pass() + ")";
        }

        // ---------------- the consequence ----------------

        private String consequence(Consequence g) {
            StringBuilder b = new StringBuilder();
            switch (g) {
                case Consequence.Value x -> value(x, b);
                case Consequence.Characteristic x -> insert(b, "    ", x.insertion(), "new " + typeName(HasCharacteristic.class) + "("
                        + x.subject() + ", " + key(x.characteristic()) + ")");
                case Consequence.TestFault x -> testFault(x, b);
                case Consequence.Fault x -> insert(b, "    ", x.insertion(), "new " + typeName(Fault.class) + "("
                        + x.subject() + ", " + str(x.fault().form()) + ", " + route(Route.COMPUTE_FAULT) + "("
                        + str(x.sentence()) + ", " + str(x.fault().rule()) + "))");
                case Consequence.Inconsistency x -> insert(b, "    ", x.insertion(), route(Route.INCONSISTENCY) + "("
                        + str(x.form()) + ", " + x.subject() + ", " + list(texts(x.codes())) + ")");
                case Consequence.ConditionDeposit x -> insert(b, "    ", x.insertion(), route(Route.INCONSISTENCY) + "("
                        + str(x.form()) + ", " + x.subject() + ", " + depositCodesAgain(x.condition(), x.depositFlag(),
                                x.slots(), "de voorwaarde van \"" + x.form() + "\"") + ")");
                case Consequence.WritersConflict x -> insert(b, "    ", x.insertion(), route(Route.WRITERS_CONFLICT)
                        + "(" + x.subject() + ", " + variable(x.values()) + ", " + str(x.attribute()) + ", "
                        + list(texts(x.documentOrder())) + ", " + list(texts(x.typeClasses())) + ", "
                        + list(texts(x.types())) + ")");
                case Consequence.InsertParameterState x -> insert(b, "    ", x.insertion(), "new "
                        + typeName(ParameterState.class) + "(" + str(x.parameter()) + ", " + x.global() + ")");
                case Consequence.EvaluationDateState x -> insert(b, "    ", x.insertion(), "new " + typeName(EvaluationDate.class)
                        + "(" + x.global() + ")");
                case Consequence.InsertCharacteristicState x -> insert(b, "    ", x.insertion(), "new " + typeName(CharacteristicState.class)
                        + "(" + x.subject() + ", " + key(x.characteristic()) + ", " + x.holds() + ")");
                case Consequence.State x -> insert(b, "    ", x.insertion(), "new " + typeName(State.class) + "("
                        + x.subject() + ", " + key(x.attribute()) + ", " + (x.bucketScalar()
                        ? route(Route.BUCKET_SCALAR) + "(" + variable(x.value()) + ")" : variable(x.value())) + ")");
                case Consequence.InsertPathState x -> insert(b, "    ", x.insertion(), "new " + typeName(PathState.class) + "("
                        + x.subject() + ", " + str(x.path()) + ", " + key(x.member()) + ", "
                        + (x.source() == Consequence.StateSource.MISSING_LINK ? "null" : variable(x.value())) + ")");
                case Consequence.Contribution x -> contribution(x, b);
                case Consequence.FilterDeposit x -> insert(b, "    ", x.insertion(), "new "
                        + typeName(FilterDeposit.class) + "(" + str(x.fold()) + ", " + x.subject() + ", "
                        + variable(x.member()) + ", " + depositCodesAgain(x.filter(), x.depositFlag(), x.slots(),
                                "het filter van " + x.fold()) + ")");
                case Consequence.FilterFault x -> filterFault(x, b);
                case Consequence.MemberFault x -> insert(b, "    ", x.insertion(), "new " + typeName(MemberFault.class) + "("
                        + str(x.fold()) + ", " + x.subject() + ", " + variable(x.member()) + ", "
                        + route(Route.INPUT_FORM_FAULT) + "(" + str(x.before()) + ", " + variable(x.member()) + ", "
                        + str(x.after()) + ", null), " + foldPlace(x.place(), x.fold(), x.member(), null) + ")");
                case Consequence.UniquenessKey x -> uniquenessKey(x, b);
                case Consequence.UniquenessViolation x -> insert(b, "    ", x.insertion(), "new "
                        + typeName(UniquenessViolation.class) + "(" + str(x.uniqueness()) + ", " + x.subject() + ")");
                case Consequence.InputFormFault x -> insert(b, "    ", x.insertion(), "new " + typeName(Fault.class) + "("
                        + x.subject() + ", " + str(x.fault().form()) + ", " + route(Route.INPUT_FORM_FAULT) + "("
                        + str(x.before()) + ", " + x.subject() + ", " + str(x.after()) + ", " + str(x.fault().rule())
                        + "))");
                case Consequence.InsertChannelState x -> insert(b, "    ", x.insertion(), "new " + typeName(ChannelState.class)
                        + "(" + str(x.producer()) + ", " + x.inconsistent() + ")");
                case Consequence.Decision x -> decisionOf(x, b);
                case Consequence.Conflict x -> conflict(x, b);
                case Consequence.Creation x -> creation(x, b);
                case Consequence.RelationLink x -> insert(b, "    ", x.insertion(), "new " + typeName(RelationLink.class) + "("
                        + str(x.relation()) + ", " + x.first() + ", " + x.second() + ", "
                        + (x.phase() < 0 ? "0" : variable(x.phase())) + ")");
                case Consequence.SecondFilling x -> insert(b, "    ", x.insertion(), "new " + typeName(Fault.class) + "("
                        + x.subject() + ", " + str(x.relation()) + ", new " + typeName(IllegalStateException.class)
                        + "(" + str(x.sentence()) + "))");
                case Consequence.Recipient x -> insert(b, "    ", x.insertion(), "new " + typeName(Recipient.class) + "("
                        + str(x.allocation()) + ", " + x.subject() + ", " + variable(x.recipient()) + ", "
                        + slotOfNull(x.weight()) + ", " + slotOfNull(x.key()) + ", " + slotOfNull(x.maximum()) + ")");
                case Consequence.Payout x -> payout(x, b);
                case Consequence.Reading x -> reading(x, b);
            }
            return b.toString();
        }

        /** The reading of {@link Consequence.Reading}: a function of the rule gives the value array as a list, the bound
         *  value at each slot the plan names and null at every other, which the Java dialect builds because MVEL builds no
         *  array; the rule inserts the reading of its subject with it. */
        private void reading(Consequence.Reading x, StringBuilder b) {
            int place = reserveer();
            String name = name("lezing", place);
            Context c = context(x.slots(), -1, null);
            StringBuilder body = new StringBuilder("    java.util.List waarden = new java.util.ArrayList();\n");
            for (int s = 0; s < x.size(); s++)
                body.append("    waarden.add(").append(c.slots.containsKey(s) ? c.slots.get(s) : "null").append(");\n");
            body.append("    return waarden;\n");
            fill(place, null, "java.util.List " + name + "(" + c.declaration() + ")", body.toString());
            insert(b, "    ", x.insertion(), "new " + typeName(Reading.class) + "(" + x.subject() + ", " + str(x.form())
                    + ", " + name + "(" + c.arguments() + "))");
        }

        private List<String> texts(List<String> values) {
            List<String> out = new ArrayList<>();
            for (String w : values) out.add(str(w));
            return out;
        }

        private void insert(StringBuilder b, String indentation, Consequence.Insertion insertion, String fact) {
            b.append(indentation).append(insertion == Consequence.Insertion.LOGICAL ? "insertLogical" : "insert").append('(')
                    .append(fact).append(");\n");
        }

        /** The value of {@link Consequence.Value}: a function that evaluates it and gives an evaluation fault as its
         *  outcome; the fault in place of the value and of the deposit, else the deposit first and then the value,
         *  where it is gevuld or the plan writes leeg. */
        private void value(Consequence.Value x, StringBuilder b) {
            Consequence.Deposit d = x.deposit();
            int place = reserveer();
            String name = name("waarde", place);
            Context c = context(x.slots(), d == null ? -1 : d.depositFlag(), DEPOSIT_FLAG, x.value());
            Expr e = value(x.value(), c);
            String v = x.collapse() ? call(StaticFunction.COLLAPSE, List.of(e)).text() : e.text();
            fill(place, x.value().source(), "Object " + name + "(" + c.declaration() + ")",
                    tryCatch("        return " + v + ";\n", "        return " + route(Route.CAUGHT) + "(e);\n"));
            if (d != null) b.append("    Object ").append(DEPOSIT_FLAG).append(" = ").append(route(Route.FRESH_DEPOSIT_FLAG)).append("();\n");
            b.append("    Object v = ").append(name).append("(").append(c.arguments()).append(");\n");
            b.append("    if (").append(route(Route.IS_EVALUATION_FAULT)).append("(v)) {\n");
            insert(b, "        ", x.insertion(), "new " + typeName(Fault.class) + "(" + x.subject() + ", "
                    + str(x.fault().form()) + ", " + route(Route.DESIGNATE) + "(v, " + str(x.fault().rule()) + "))");
            b.append("    } else {\n");
            if (d != null) {
                b.append("        if (").append(route(Route.REPORTED)).append("(").append(DEPOSIT_FLAG).append(")) {\n");
                insert(b, "            ", x.insertion(), route(Route.INCONSISTENCY) + "(" + str(d.form()) + ", "
                        + x.subject() + ", " + route(Route.DEPOSIT_CODES) + "(" + DEPOSIT_FLAG + "))");
                b.append("        }\n");
            }
            String fact = "new " + typeName(Value.class) + "(" + x.subject() + ", " + key(x.target()) + ", v, "
                    + str(x.writer()) + ")";
            if (x.writesEmpty()) {
                insert(b, "        ", x.insertion(), fact);
            } else {
                b.append("        if (!").append(route(Route.IS_EMPTY)).append("(v)) {\n");
                insert(b, "            ", x.insertion(), fact);
                b.append("        }\n");
            }
            b.append("    }\n");
        }

        /** The codes of the deposit of {@code condition}, which the test of the rule found: a function of the rule
         *  evaluates it again over the bound values at {@code slots} with a fresh probe in slot {@code depositFlag},
         *  an evaluation fault leaving what the probe heard before it, and gives the codes the probe heard, a probe
         *  that heard nothing a broken contract of {@code what}; returns its call. */
        private String depositCodesAgain(Term condition, int depositFlag, List<Integer> slots, String what) {
            int place = reserveer();
            String name = name("deponering", place);
            Context c = context(slots, depositFlag, route(Route.FRESH_DEPOSIT_FLAG) + "()", condition);
            String body = tryCatch("        boolean uitkomst = " + bool(condition, c) + ";\n",
                    "        " + route(Route.CAUGHT) + "(e);\n")
                    + "    if (!" + route(Route.REPORTED) + "(" + DEPOSIT_FLAG + ")) {\n        "
                    + route(Route.CONTRACT_BREACH) + "(" + str(what + " deponeerde in de toets en niet in het gevolg,"
                            + " over dezelfde gebonden waarden") + ");\n    }\n"
                    + "    return " + route(Route.DEPOSIT_CODES) + "(" + DEPOSIT_FLAG + ");\n";
            fill(place, condition.source(), "java.util.List " + name + "(" + c.declaration() + ")", body);
            return name + "(" + c.arguments() + ")";
        }

        /** The fault of {@link Consequence.TestFault}: the condition evaluated again over the same bound values, its
         *  evaluation fault inserted, and an answer there a broken contract. */
        private void testFault(Consequence.TestFault x, StringBuilder b) {
            int place = reserveer();
            String name = name("fout", place);
            Context c = context(x.slots(), -1, null, x.test());
            String t = bool(x.test(), c);
            fill(place, x.test().source(), "Object " + name + "(" + c.declaration() + ")",
                    tryCatch("        boolean uitkomst = " + t + ";\n        return null;\n",
                            "        return " + route(Route.CAUGHT) + "(e);\n"));
            b.append("    Object f = ").append(name).append("(").append(c.arguments()).append(");\n");
            b.append("    if (!").append(route(Route.IS_EVALUATION_FAULT)).append("(f)) {\n        ")
                    .append(route(Route.CONTRACT_BREACH)).append("(").append(str("de voorwaarde van \"" + x.fault().form()
                            + "\" faalde in de toets en niet in het gevolg, over dezelfde gebonden waarden"))
                    .append(");\n    }\n");
            insert(b, "    ", x.insertion(), "new " + typeName(Fault.class) + "(" + x.subject() + ", "
                    + str(x.fault().form()) + ", " + route(Route.DESIGNATE) + "(f, " + str(x.fault().rule()) + "))");
        }

        /** The contribution of {@link Consequence.Contribution} on the fold's subject: the member itself; its value through
         *  the route, which gives nothing where it is leeg; the verdict of the matrix of a quantifier fold, a function
         *  of the rule; or its test of the member, a function of the rule that takes the member's place first, then
         *  evaluates the matrix with the probe where the plan names one, and gives the verdict or the evaluation
         *  fault. */
        private void contribution(Consequence.Contribution x, StringBuilder b) {
            String fresh = "new " + typeName(Contribution.class) + "(" + str(x.fold()) + ", " + x.subject() + ", "
                    + variable(x.member()) + ", ";
            switch (x.input()) {
                case Consequence.Input.Count t -> insert(b, "    ", x.insertion(), fresh + "null)");
                case Consequence.Input.MemberValue l -> {
                    b.append("    Object v = ").append(route(Route.CONTRIBUTION)).append("(").append(str(x.fold()))
                            .append(", ").append(x.subject()).append(", ").append(variable(x.member())).append(", ")
                            .append(variable(l.value())).append(", ")
                            .append(l.total() == null ? "null" : str(l.total())).append(", ").append(l.filled())
                            .append(");\n");
                    b.append("    if (v != null) {\n");
                    insert(b, "        ", x.insertion(), "v");
                    b.append("    }\n");
                }
                case Consequence.Input.Verdict o -> {
                    int place = reserveer();
                    String name = name("matrix", place);
                    Context c = context(x.slots(), -1, null, o.matrix());
                    fill(place, o.matrix().source(), "boolean " + name + "(" + c.declaration() + ")",
                            returning(o.matrix(), c, "    "));
                    insert(b, "    ", x.insertion(), fresh + route(Route.VERDICT) + "(" + name + "(" + c.arguments()
                            + ")))");
                }
                case Consequence.Input.MemberTest l -> {
                    int place = reserveer();
                    String name = name("lidtoets", place);
                    Context c = context(x.slots(), l.depositFlag(), route(Route.FRESH_DEPOSIT_FLAG) + "()", l.matrix());
                    // the place first, so that a member without one breaks the contract before the matrix is evaluated
                    String body = "    java.util.List plaats = " + foldPlace(l.place(), x.fold(), x.member(), c)
                            + ";\n    Boolean waar = null;\n    RuntimeException fout = null;\n"
                            + tryCatch("        waar = Boolean.valueOf(" + bool(l.matrix(), c) + ");\n",
                                    "        fout = " + route(Route.CAUGHT) + "(e);\n")
                            + "    return new " + typeName(MemberTest.class) + "(plaats, waar, fout, "
                            + route(Route.REPORTED) + "(" + (l.depositFlag() < 0 ? "null" : DEPOSIT_FLAG) + "), "
                            + route(Route.DEPOSIT_CODES) + "(" + (l.depositFlag() < 0 ? "null" : DEPOSIT_FLAG) + "));\n";
                    fill(place, l.matrix().source(), "Object " + name + "(" + c.declaration() + ")", body);
                    insert(b, "    ", x.insertion(), fresh + name + "(" + c.arguments() + "))");
                }
            }
        }

        /** The keys of {@link Consequence.UniquenessKey}: a function of the rule gives the key components, each the
         *  value of its term over the bound values, in a list that the Java dialect builds because a component may be
         *  null; the route makes the keys of them, linked to the parent where the form has a scope, and the rule
         *  inserts each. The sentences of the components stand above the function, since a comment in its body would
         *  reach the parser of the text. */
        private void uniquenessKey(Consequence.UniquenessKey x, StringBuilder b) {
            int place = reserveer();
            String name = name("componenten", place);
            Context c = context(x.slots(), -1, null, x.components().toArray(new Term[0]));
            StringBuilder body = new StringBuilder("    java.util.List waarden = new java.util.ArrayList();\n");
            List<String> sources = new ArrayList<>();
            for (Term t : x.components()) {
                body.append("    waarden.add(").append(value(t, c).text()).append(");\n");
                if (t.source() != null) sources.add(t.source());
            }
            body.append("    return waarden;\n");
            fill(place, sources.isEmpty() ? null : String.join("; ", sources),
                    "java.util.List " + name + "(" + c.declaration() + ")", body.toString());
            b.append("    for (Object k : ").append(route(Route.UNIQUENESS_KEYS)).append("(").append(str(x.uniqueness()))
                    .append(", ").append(x.source()).append(", ").append(x.subject()).append(", ")
                    .append(x.scope() < 0 ? "null" : variable(x.scope())).append(", ").append(name).append("(")
                    .append(c.arguments()).append("), ").append(x.timeline()).append(")) {\n");
            insert(b, "        ", x.insertion(), "k");
            b.append("    }\n");
        }

        /** The decision of {@link Consequence.Decision}: a function of the rule gives its value, collapsed where the plan says
         *  so, and gives an evaluation fault as its outcome where the plan names a fault, a computed decision; any other
         *  exception leaves the fire, a broken contract, as in the executable model. The fault in place of the
         *  decision; nothing where the value is null, so that the lower defaults decide (RUNTIME-SEMANTICS ch. 7.1);
         *  else the facts of the decision in the order of the plan. */
        private void decisionOf(Consequence.Decision x, StringBuilder b) {
            int place = reserveer();
            String name = name("afloop", place);
            Context c = context(x.slots(), -1, null, x.value());
            Expr e = value(x.value(), c);
            String v = x.collapse() ? call(StaticFunction.COLLAPSE, List.of(e)).text() : e.text();
            fill(place, x.value().source(), "Object " + name + "(" + c.declaration() + ")", x.fault() == null
                    ? "    return " + v + ";\n"
                    : tryCatch("        return " + v + ";\n", "        return " + route(Route.CAUGHT) + "(e);\n"));
            b.append("    Object v = ").append(name).append("(").append(c.arguments()).append(");\n");
            if (x.fault() != null) {
                b.append("    if (").append(route(Route.IS_EVALUATION_FAULT)).append("(v)) {\n");
                insert(b, "        ", x.insertion(), "new " + typeName(Fault.class) + "(" + x.subject() + ", "
                        + str(x.fault().form()) + ", " + route(Route.DESIGNATE) + "(v, " + str(x.fault().rule()) + "))");
                b.append("    } else if (v != null) {\n");
            } else {
                b.append("    if (v != null) {\n");
            }
            for (Consequence.StackFact f : x.facts()) insert(b, "        ", x.insertion(), stackFact(f, x.subject()));
            b.append("    }\n");
        }

        /** The conflict of {@link Consequence.Conflict}: a function of the rule gives its key, a constant or the key of the
         *  conflicting member bound off its outcome, and the rule inserts the fact of the plan under it. */
        private void conflict(Consequence.Conflict x, StringBuilder b) {
            int place = reserveer();
            String name = name("conflict", place);
            Context c = context(x.slots(), -1, null, x.key());
            fill(place, x.key().source(), "Object " + name + "(" + c.declaration() + ")",
                    "    return " + value(x.key(), c).text() + ";\n");
            b.append("    Object v = ").append(name).append("(").append(c.arguments()).append(");\n");
            insert(b, "    ", x.insertion(), stackFact(x.fact(), x.subject()));
        }

        /** The fact {@code f} of a defaults stack on {@code subject}, with the local {@code v} of the consequence: the
         *  decision, or the key of a conflict. */
        private String stackFact(Consequence.StackFact f, String subject) {
            return switch (f) {
                case Consequence.StackFact.Value x -> "new " + typeName(Value.class) + "(" + subject + ", "
                        + key(x.target()) + ", v, " + str(x.writer()) + ")";
                case Consequence.StackFact.Characteristic x -> "new " + typeName(HasCharacteristic.class) + "(" + subject + ", "
                        + key(x.characteristic()) + ")";
                case Consequence.StackFact.Decides x -> "new " + typeName(Decides.class) + "(" + subject + ", "
                        + str(x.stack()) + ", " + x.index() + ")";
                case Consequence.StackFact.Outcome x -> "new " + typeName(Outcome.class) + "(" + subject + ", "
                        + str(x.stack()) + ", " + x.chain() + ", " + x.position() + ", " + enumlabel(x.kind()) + ", "
                        + (x.kind() == Outcome.Kind.DECIDED ? "v, null" : "null, (String) v") + ")";
                case Consequence.StackFact.Inconsistency x -> route(Route.INCONSISTENCY) + "((String) v, " + subject + ", "
                        + list(texts(Consequence.StackFact.Inconsistency.CODES)) + ")";
            };
        }

        /**
         * The object creation of {@link Consequence.Creation}: a function of the rule takes the steps of the plan in their order
         * and makes the facts, and the rule inserts them. The function computes the depth through its route, which throws
         * past the budget before anything is made; then the value of each initialisation, collapsed, the first evaluation
         * fault being its outcome, as in the executable model, which then makes nothing; then the new bean through the
         * route {@code newObject}, with each attribute initialisation set on it through the route {@code set} before it
         * is inserted; and it gives the facts of the insertions in the order of the plan, null for a characteristic whose
         * value is not true. The rule inserts the fault, or each fact as the plan inserts it at its place. The function
         * makes the facts since MVEL resolves no constructor with an {@code int} parameter from an argument of static
         * type {@code Object} (FABLE-3 §2.4, decision 33). The sentences of the initialisations stand above the function;
         * the plan carries none of the creation itself.
         */
        private void creation(Consequence.Creation x, StringBuilder b) {
            int place = reserveer();
            String name = name("creatie", place);
            List<Term> values = new ArrayList<>();
            List<String> sources = new ArrayList<>();
            for (Consequence.Initialisation i : x.initialisations()) {
                values.add(i.value());
                if (i.value().source() != null) sources.add(i.value().source());
            }
            Context c = context(x.slots(), -1, null, values.toArray(new Term[0]));
            // the subject is the variable of the rule's subject pattern, and a slot only where the plan reads it as one
            if (!c.has(x.subject())) c.parameter("Object", x.subject(), x.subject());
            Consequence.CreationDepth d = x.depth();
            StringBuilder f = new StringBuilder();
            f.append("    int diepte = ").append(route(Route.CREATION_DEPTH)).append("(").append(str(x.creation()))
                    .append(", ").append(d.stratum()).append(", ").append(str(d.type())).append(", (java.util.List) ")
                    .append(slot(d.origin(), c)).append(", ").append(d.budget()).append(");\n");
            if (!values.isEmpty()) {
                StringBuilder tryBody = new StringBuilder();
                for (int i = 0; i < values.size(); i++) {
                    f.append("    Object i").append(i).append(" = null;\n");
                    tryBody.append("        i").append(i).append(" = ")
                            .append(call(StaticFunction.COLLAPSE, List.of(value(values.get(i), c))).text()).append(";\n");
                }
                f.append(tryCatch(tryBody.toString(), "        return " + route(Route.CAUGHT) + "(e);\n"));
            }
            f.append("    Object nieuw = ").append(route(Route.NEW_OBJECT)).append("(").append(x.subject())
                    .append(", ").append(str(x.construction().javaClass().getName())).append(");\n");
            for (Consequence.Construction.Setting z : x.construction().settings())
                f.append("    ").append(route(Route.SET)).append("(nieuw, ").append(str(z.field().property()))
                        .append(", i").append(z.initialisation()).append(");\n");
            f.append("    int fase = ((Integer) ").append(slot(x.phase(), c)).append(").intValue();\n");
            f.append("    java.util.List feiten = new java.util.ArrayList();\n");
            for (Consequence.Insert i : x.insertions())
                f.append("    feiten.add(").append(switch (i) {
                    case Consequence.Insert.New n -> "nieuw";
                    case Consequence.Insert.Mark m -> "new " + typeName(CreationStamp.class) + "(" + str(x.creation()) + ", "
                            + x.subject() + ", nieuw, fase, " + d.stratum() + ", diepte)";
                    case Consequence.Insert.Link l -> "new " + typeName(RelationLink.class) + "(" + str(l.relation()) + ", "
                            + (l.newIsFirst() ? "nieuw, " + x.subject() : x.subject() + ", nieuw") + ", fase)";
                    case Consequence.Insert.Characteristic k -> "Boolean.TRUE.equals(i" + k.initialisation() + ") ? new "
                            + typeName(HasCharacteristic.class) + "(nieuw, "
                            + key(x.initialisations().get(k.initialisation()).member()) + ") : null";
                }).append(");\n");
            f.append("    return feiten;\n");
            fill(place, sources.isEmpty() ? null : String.join("; ", sources),
                    "Object " + name + "(" + c.declaration() + ")", f.toString());
            b.append("    Object v = ").append(name).append("(").append(c.arguments()).append(");\n");
            b.append("    if (").append(route(Route.IS_EVALUATION_FAULT)).append("(v)) {\n");
            insert(b, "        ", x.faultInsertion(), "new " + typeName(Fault.class) + "(" + x.subject() + ", "
                    + str(x.fault().form()) + ", " + route(Route.DESIGNATE) + "(v, " + str(x.fault().rule()) + "))");
            b.append("    } else {\n        java.util.List feiten = (java.util.List) v;\n");
            for (int i = 0; i < x.insertions().size(); i++) {
                Consequence.Insert s = x.insertions().get(i);
                if (s instanceof Consequence.Insert.Characteristic) {
                    b.append("        if (feiten.get(").append(i).append(") != null) {\n");
                    insert(b, "            ", s.insertion(), "feiten.get(" + i + ")");
                    b.append("        }\n");
                } else {
                    insert(b, "        ", s.insertion(), "feiten.get(" + i + ")");
                }
            }
            b.append("    }\n");
        }

        /** The variable of slot {@code slot}, or {@code null} where it is -1, a value the plan does not read. */
        private String slotOfNull(int slot) {
            return slot < 0 ? "null" : variable(slot);
        }

        /** The distribution of {@link Consequence.Payout}: the facts of the route, each inserted. The method stands as its
         *  enumeration label, which MVEL reads in the argument of a call (FABLE-3 §2.2), the decimals as an
         *  {@code Integer} or null, and the keys as {@link ElementKey#of}. */
        private void payout(Consequence.Payout x, StringBuilder b) {
            b.append("    for (Object feit : ").append(route(Route.PAYOUT)).append("(").append(str(x.form()))
                    .append(", ").append(x.subject()).append(", ").append(enumlabel(x.method())).append(", ")
                    .append(variable(x.pot())).append(", ").append(variable(x.list())).append(", ").append(x.weighted())
                    .append(", ").append(x.capped()).append(", ").append(x.descending()).append(", ")
                    .append(x.decimals() == null ? "null" : "Integer.valueOf(" + x.decimals() + ")").append(", ")
                    .append(key(x.share())).append(", ").append(x.rest() == null ? "null" : key(x.rest()))
                    .append(", ").append(str(x.writer())).append(")) {\n");
            insert(b, "        ", x.insertion(), "feit");
            b.append("    }\n");
        }

        /** The fault of {@link Consequence.FilterFault}: the filter evaluated again over the same bound values, its
         *  evaluation fault inserted with the member's place, and an answer there a broken contract. */
        private void filterFault(Consequence.FilterFault x, StringBuilder b) {
            int place = reserveer();
            String name = name("fout", place);
            Context c = context(x.slots(), -1, null, x.filter());
            String t = bool(x.filter(), c);
            fill(place, x.filter().source(), "RuntimeException " + name + "(" + c.declaration() + ")",
                    tryCatch("        boolean uitkomst = " + t + ";\n        return null;\n",
                            "        return " + route(Route.CAUGHT) + "(e);\n"));
            b.append("    RuntimeException f = ").append(name).append("(").append(c.arguments()).append(");\n");
            b.append("    if (f == null) {\n        ").append(route(Route.CONTRACT_BREACH)).append("(")
                    .append(str("het filter van " + x.fold() + " faalde in de toets en niet in het gevolg, over"
                            + " dezelfde gebonden waarden")).append(");\n    }\n");
            insert(b, "    ", x.insertion(), "new " + typeName(FilterFault.class) + "(" + str(x.fold()) + ", "
                    + x.subject() + ", " + variable(x.member()) + ", f, "
                    + foldPlace(x.place(), x.fold(), x.member(), null) + ")");
        }

        /**
         * The member's place {@code p} in the order of the fold {@code fold}, through the route, as a function of this
         * rule over the member in slot {@code member} and the count and the least place of each step, by the variables of
         * their slots; returns its call, which reads those variables where it stands: in the consequence, where the
         * left-hand side binds them, or in the function whose context is {@code c}, which must pass them.
         */
        private String foldPlace(Consequence.FoldPlace p, String fold, int member, Context c) {
            int place = reserveer();
            String name = name("plaats", place);
            List<String> parameters = new ArrayList<>(), fields = new ArrayList<>(), counts = new ArrayList<>(),
                    places = new ArrayList<>();
            parameters.add(variable(member));
            for (Consequence.FoldPlace.Step s : p.steps()) {
                fields.add(str(s.field()));
                counts.add(variable(s.count()));
                places.add(variable(s.place()));
                parameters.add(variable(s.count()));
                parameters.add(variable(s.place()));
            }
            if (c != null)
                for (String v : parameters)
                    if (!c.has(v))
                        throw new IllegalStateException("de plaats van een lid in " + plan.name() + " leest " + v
                                + ", dat de functie van haar gevolg niet krijgt");
            List<String> declaration = new ArrayList<>();
            for (String v : parameters) declaration.add("Object " + v);
            fill(place, null, "java.util.List " + name + "(" + String.join(", ", declaration) + ")",
                    "    return " + route(Route.FOLD_PLACE) + "(" + enumlabel(p.reason()) + ", " + str(fold) + ", "
                            + variable(member) + ", " + p.selection() + ", java.util.Arrays.asList("
                            + String.join(", ", fields) + "), java.util.Arrays.asList(" + String.join(", ", counts)
                            + "), java.util.Arrays.asList(" + String.join(", ", places) + "));\n");
            return name + "(" + String.join(", ", parameters) + ")";
        }

        // ---------------- the day kinds of a term ----------------

        /** Adds to {@code out} the global of every day kind {@code t} names, in the order of the tree. */
        private void dayKinds(Term t, Set<String> out) {
            switch (t) {
                case Term.DayKind d -> out.add(dayKind(d));
                case Term.Input i -> dayKinds(i.object(), out);
                case Term.Let l -> {
                    dayKinds(l.value(), out);
                    dayKinds(l.body(), out);
                }
                case Term.Choice k -> {
                    dayKinds(k.condition(), out);
                    dayKinds(k.then(), out);
                    dayKinds(k.otherwise(), out);
                }
                case Term.Call a -> a.arguments().forEach(x -> dayKinds(x, out));
                case Term.Restate h -> dayKinds(h.term(), out);
                case Term.Division d -> {
                    dayKinds(d.left(), out);
                    dayKinds(d.right(), out);
                }
                case Term.SquareRoot w -> dayKinds(w.radicand(), out);
                case Term.Required v -> dayKinds(v.value(), out);
                case Term.TextComposition s -> s.parts().forEach(x -> dayKinds(x, out));
                case Term.ListedFold g -> g.operands().forEach(x -> dayKinds(x, out));
                case Term.FoldRead v -> dayKinds(v.outcome(), out);
                case Term.Compound s -> s.members().forEach(x -> dayKinds(x, out));
                case Term.QuantifierRead q -> {
                    dayKinds(q.count(), out);
                    dayKinds(q.contributions(), out);
                }
                case Term.WithoutFault z -> dayKinds(z.condition(), out);
                case Term.Const k -> { }
                case Term.SlotValue l -> { }
                case Term.Binder x -> { }
                case Term.Fail f -> { }
            }
        }
    }
}
