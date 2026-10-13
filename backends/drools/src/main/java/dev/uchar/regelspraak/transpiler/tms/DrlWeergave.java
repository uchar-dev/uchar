package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.graaf.ProducentConsument.ElementSleutel;
import dev.uchar.regelspraak.ir.Geldigheid;
import dev.uchar.regelspraak.runtime.Breuk;
import dev.uchar.regelspraak.schema.FactModelGenerator;
import dev.uchar.regelspraak.transpiler.kern.Namen;
import dev.uchar.regelspraak.transpiler.tms.plan.Accumulatie;
import dev.uchar.regelspraak.transpiler.tms.plan.Alfa;
import dev.uchar.regelspraak.transpiler.tms.plan.Bestaat;
import dev.uchar.regelspraak.transpiler.tms.plan.Betatoets;
import dev.uchar.regelspraak.transpiler.tms.plan.Binding;
import dev.uchar.regelspraak.transpiler.tms.plan.Functie;
import dev.uchar.regelspraak.transpiler.tms.plan.Gevolg;
import dev.uchar.regelspraak.transpiler.tms.plan.Join;
import dev.uchar.regelspraak.transpiler.tms.plan.Lezing;
import dev.uchar.regelspraak.transpiler.tms.plan.LhsItem;
import dev.uchar.regelspraak.transpiler.tms.plan.Modelplan;
import dev.uchar.regelspraak.transpiler.tms.plan.Niet;
import dev.uchar.regelspraak.transpiler.tms.plan.Onderwerp;
import dev.uchar.regelspraak.transpiler.tms.plan.Patroon;
import dev.uchar.regelspraak.transpiler.tms.plan.Regelplan;
import dev.uchar.regelspraak.transpiler.tms.plan.Route;
import dev.uchar.regelspraak.transpiler.tms.plan.Term;
import dev.uchar.regelspraak.transpiler.tms.plan.Toetsitem;

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

/**
 * The rendering of the outcome of the analysis into DRL text ({@link #tekst}), the second rendering beside the
 * executable model ({@link PatternDslWeergave}). It decides nothing: which globals there are, what each rule reads,
 * how and in which order, what a test asks and what a firing inserts is the plan's ({@link Modelplan},
 * {@link Regelplan}); this class spells it.
 *
 * <p>The spelling follows the measurements of the second advice of card drl-teksttak-op-truth-maintenance (FABLE-2
 * §2) and the decisions taken on them:
 * <ul>
 *   <li>The rules are MVEL; every test and every value of the plan, a {@link Term}, is a function in the Java dialect
 *       with the RegelSpraak sentence of its node above it as a comment, which the rule calls. MVEL has no
 *       {@code try} and no {@code throw}, and one form for every term keeps one semantics. A node that needs
 *       statements of its own, a composite, a {@code Let} and its binder, a condition without its fault, the
 *       evaluation of a fold and of a quantifier fold, is a function of its own, which takes the parameters of the
 *       function it stands in.</li>
 *   <li>The constraints that decide which fact a pattern matches stay DRL constraints: the key of a fact, a null
 *       test, the rank of a control fact and an identity join, so that Drools indexes them. A test of a class goes
 *       through a route, since the text imports no bean class. A member of a bean stands as the name Drools reads
 *       the generator's getter under ({@code Namen.lidnamen}); a member Drools reads as something else under that
 *       name, a word of DRL or MVEL, is refused by name.</li>
 *   <li>A join meets nothing where a side reads null, as the identity join of the executable model does: where the
 *       pattern's side is a field and the other side can read null, a field of an object or a value a binding binds,
 *       the join tests the field for null first, since DRL finds two nulls equal.</li>
 *   <li>A read along a path is a function that walks the path with the routes {@code langs} and {@code lees}, null
 *       where a link is absent; never the null-safe navigation of the dialect, which drops the match instead.</li>
 *   <li>Every helper is called through its class, never through an {@code import function}: a helper named like a
 *       field of a fact shadows that field without a complaint (FABLE-2 §2.1). The text imports the classes of the
 *       branch and of the runtime it names, never a bean class, which the Java dialect cannot see: a bean stands with
 *       its full name in a pattern. A class of the JDK and an enumeration label stand with their full names.</li>
 *   <li>The local names of a consequence are fixed ({@code v}, {@code f}, {@code k}, {@code sonde}), and a function is
 *       named after its rule's place in the plan and its own place in that rule's functions, which stand before the
 *       rule in that order: one plan gives one text.</li>
 * </ul>
 *
 * <p>The consequences it carries are those of the ordinary rules, the faults, the inconsistency, the writers that
 * disagree and the standings (step 2a), and those of the rules of a fold (step 3a): a member's contribution, and the
 * deposit, the fault of the filter and the fault of a member, with the member's place in the fold's order as a function
 * of the rule; and those of a uniqueness and of the §9.5 channel (step 3b): the keys, the violation, the fault of the
 * form of an input and the standing of the channel; and those of a defaults stack (step 4): the decision of a default
 * or a reconciliation, with its fault where it is computed, and the conflict of a reconciliation, each with the facts
 * of the stack. Every other kind it refuses loudly ({@link Weigering}).
 */
final class DrlWeergave {

    /** The path of the text in the resource tree of a {@code KieHelper}: its folders follow the package, its extension
     *  gives the type. */
    static final String PAD = TmsTranspiler.PKG.replace('.', '/') + "/regels.drl";

    /** The fixed name of the probe: a local of a consequence, and the parameter of a function that a probe reaches. */
    private static final String SONDE = "sonde";

    /** A form of the plan this rendering does not carry, refused loudly with its kind, never left out: a bean class as
     *  the constant of a term, a day kind whose global is no Java identifier, and a member that Drools reads as
     *  something else where a pattern spells it. Every kind of consequence is carried since step 5; a new kind does not
     *  compile until the rendering spells it ({@link Regel#gevolg}). */
    static final class Weigering extends RuntimeException {
        private final String soort;

        Weigering(String soort) {
            super("de DRL-weergave draagt " + soort + " nog niet");
            this.soort = soort;
        }

        /** The kind refused, such as the simple name of a consequence. */
        String soort() { return soort; }
    }

    /** The classes the text imports, by their canonical names. */
    private final Set<String> imports = new TreeSet<>();
    /** The canonical name of each imported class by its simple name, so that two classes of one name fail loudly. */
    private final Map<String, String> enkelvoudig = new HashMap<>();
    /** The globals of the day kinds the terms name, by their names, each with the node of its day kind, which carries
     *  the predicate the analysis compiled. */
    private final Map<String, Term.Dagsoort> dagsoorten = new LinkedHashMap<>();
    private final StringBuilder regels = new StringBuilder();

    private DrlWeergave() {}

    /**
     * The DRL text of {@code plan}: the package, the imports, the globals of the plan and those of the day kinds its
     * terms name, then per rule, in the order of the plan, its functions and the rule, under the name of its plan. A
     * form this rendering does not carry is refused ({@link Weigering}), and so are two rules of one name, of which a
     * KieBase would keep one.
     */
    static String tekst(Modelplan plan) {
        return render(plan).tekst();
    }

    /** The DRL text of a plan ({@link #tekst}), and the predicate of every day kind whose global the text declares, by
     *  the name of the global: a session of the text sets each before it fires. The predicates are those the analysis
     *  compiled into its plan ({@code Term.Dagsoort}); the rendering only names their globals (decision 6). */
    record Gerenderd(String tekst, Map<String, java.util.function.Predicate<LocalDate>> dagsoorten) {}

    /** {@link #tekst} with the predicates of its day-kind globals. */
    static Gerenderd render(Modelplan plan) {
        DrlWeergave w = new DrlWeergave();
        Set<String> regelnamen = new HashSet<>();
        for (int n = 0; n < plan.regels().size(); n++) {
            // the header escapes a name one to one, so two rules of one name in the text are two plans of one name
            if (!regelnamen.add(plan.regels().get(n).naam()))
                throw new IllegalStateException("twee regels van het plan heten " + str(plan.regels().get(n).naam())
                        + ", en een KieBase houdt één regel per naam");
            w.new Regel(n, plan.regels().get(n)).weergeven();
        }
        Set<String> gedeclareerd = new HashSet<>();
        StringBuilder globals = new StringBuilder();
        for (Modelplan.Globaal g : plan.globals()) {
            if (!gedeclareerd.add(g.naam()))
                throw new IllegalStateException("de global " + g.naam() + " wordt twee keer gedeclareerd");
            globals.append("global ").append(w.typenaam(g.klasse())).append(' ').append(g.naam()).append(";\n");
        }
        for (String d : w.dagsoorten.keySet()) {
            if (!gedeclareerd.add(d)) throw new IllegalStateException("de global " + d + " wordt twee keer gedeclareerd");
            globals.append("global java.util.function.Predicate ").append(d).append(";\n");
        }
        StringBuilder uit = new StringBuilder("package ").append(TmsTranspiler.PKG).append(";\n\n");
        for (String i : w.imports) uit.append("import ").append(i).append(";\n");
        if (!w.imports.isEmpty()) uit.append('\n');
        if (!globals.isEmpty()) uit.append(globals).append('\n');
        Map<String, java.util.function.Predicate<LocalDate>> predikaten = new LinkedHashMap<>();
        w.dagsoorten.forEach((global, d) -> predikaten.put(global, d.predikaat()));
        return new Gerenderd(uit.append(w.regels).toString(), java.util.Collections.unmodifiableMap(predikaten));
    }

    // ---------------- names ----------------

    private static boolean isBean(Class<?> k) {
        return k.getPackageName().equals(FactModelGenerator.PAKKET);
    }

    /** The name the text gives class {@code k}: a bean by its full name, a class of {@code java.lang} by its simple
     *  name, any other class of the JDK by its full name, and a class of the branch or of the runtime by its simple
     *  name, imported. */
    private String typenaam(Class<?> k) {
        if (k.isPrimitive()) return k.getName();
        if (k.isArray()) throw new IllegalStateException("een DRL-tekst spelt geen arraytype: " + k.getName());
        String canoniek = k.getCanonicalName();
        if (isBean(k)) return canoniek;
        if (k.getPackageName().startsWith("java."))
            return k.getPackageName().equals("java.lang") && k.getEnclosingClass() == null ? k.getSimpleName() : canoniek;
        String eerder = enkelvoudig.putIfAbsent(k.getSimpleName(), canoniek);
        if (eerder != null && !eerder.equals(canoniek))
            throw new IllegalStateException("twee klassen heten " + k.getSimpleName() + " in de DRL-tekst: " + eerder
                    + " en " + canoniek);
        imports.add(canoniek);
        return k.getSimpleName();
    }

    /** A call of route {@code r}, through its class. */
    private String route(Route r) {
        return typenaam(r.klasse()) + "." + r.methode();
    }

    /** A call of function {@code f}, through its class. */
    private String functie(Functie f) {
        return typenaam(f.klasse()) + "." + f.methode();
    }

    private static String str(String s) {
        return Namen.drlString(s);
    }

    /** The one public method of {@code klasse} called {@code naam}; the witnesses of the routes hold that there is one. */
    private static Method methode(Class<?> klasse, String naam) {
        Method uit = null;
        for (Method m : klasse.getMethods())
            if (m.getName().equals(naam)) {
                if (uit != null) throw new IllegalStateException(klasse.getName() + "." + naam + " is overladen");
                uit = m;
            }
        if (uit == null) throw new IllegalStateException("geen publieke methode " + klasse.getName() + "." + naam);
        return uit;
    }

    private static Class<?> omhul(Class<?> k) {
        if (!k.isPrimitive()) return k;
        if (k == boolean.class) return Boolean.class;
        if (k == int.class) return Integer.class;
        if (k == long.class) return Long.class;
        if (k == double.class) return Double.class;
        throw new IllegalStateException("geen omhulling voor " + k);
    }

    /** An expression of the Java dialect with its static type, null for the null literal. */
    private record Expr(String tekst, Class<?> type) {}

    /** {@code e} as an argument of a parameter of type {@code p}: cast where its static type is not that type. */
    private String cast(Class<?> p, Expr e) {
        if (p == Object.class) return e.tekst();
        Class<?> doel = omhul(p);
        if (e.type() == null) return p.isPrimitive() ? "((" + typenaam(doel) + ") null)" : e.tekst();
        if (doel.isAssignableFrom(omhul(e.type()))) return e.tekst();
        if (isBean(doel)) throw new IllegalStateException("een functie kan niet naar de beanklasse " + doel + " casten");
        return "((" + typenaam(doel) + ") " + e.tekst() + ")";
    }

    /** {@code e} as a Java truth value. */
    private static String alsBoolean(Expr e) {
        return e.type() == boolean.class || e.type() == Boolean.class ? e.tekst() : "((Boolean) " + e.tekst() + ")";
    }

    /** A constant of the plan in the Java dialect, with its Java type. */
    private Expr constante(Object w) {
        if (w == null) return new Expr("null", null);
        if (w instanceof String s) return new Expr(str(s), String.class);
        if (w instanceof Boolean b) return new Expr(b ? "Boolean.TRUE" : "Boolean.FALSE", Boolean.class);
        if (w instanceof Integer i) return new Expr("Integer.valueOf(" + i + ")", Integer.class);
        if (w instanceof Long l) return new Expr("Long.valueOf(" + l + "L)", Long.class);
        if (w instanceof BigDecimal d) return new Expr("new java.math.BigDecimal(" + str(d.toString()) + ")", BigDecimal.class);
        if (w instanceof BigInteger i) return new Expr("new java.math.BigInteger(" + str(i.toString()) + ")", BigInteger.class);
        if (w instanceof Breuk b)
            return new Expr(typenaam(Breuk.class) + ".van(new java.math.BigInteger(" + str(b.teller().toString())
                    + "), new java.math.BigInteger(" + str(b.noemer().toString()) + "))", Breuk.class);
        if (w instanceof LocalDate d) return new Expr("java.time.LocalDate.parse(" + str(d.toString()) + ")", LocalDate.class);
        if (w instanceof LocalDateTime d)
            return new Expr("java.time.LocalDateTime.parse(" + str(d.toString()) + ")", LocalDateTime.class);
        if (w instanceof Enum<?> e) return new Expr(enumlabel(e), e.getDeclaringClass());
        if (w instanceof Class<?> k) {
            if (isBean(k)) throw new Weigering("een beanklasse als constante van een term");
            return new Expr(typenaam(k) + ".class", Class.class);
        }
        if (w instanceof List<?> l) {
            List<String> delen = new ArrayList<>();
            for (Object x : l) delen.add(constante(x).tekst());
            return new Expr("java.util.Arrays.asList(" + String.join(", ", delen) + ")", List.class);
        }
        throw new IllegalStateException("een constante van het type " + w.getClass().getName() + " in het plan");
    }

    /** An enumeration label by its full name. */
    private static String enumlabel(Enum<?> e) {
        return e.getDeclaringClass().getCanonicalName() + "." + e.name();
    }

    /** A constant of a field test in MVEL. */
    private String mvelconstante(Object w) {
        if (w == null) return "null";
        if (w instanceof String s) return str(s);
        if (w instanceof Boolean || w instanceof Integer) return w.toString();
        if (w instanceof ElementSleutel e) return sleutel(e);
        if (w instanceof Enum<?> e) return enumlabel(e);
        throw new IllegalStateException("een veldtoets met een constante van het type " + w.getClass().getName());
    }

    /** The key of a member, as {@link ElementSleutel#van} makes every key of the plan. */
    private String sleutel(ElementSleutel e) {
        return typenaam(ElementSleutel.class) + ".van(" + str(e.objecttype()) + ", " + str(e.naam()) + ", "
                + e.kenmerk() + ")";
    }

    /** The global of the day kind {@code d}: its predicate, a constant of the document, which the case runner sets
     *  under this name before the session fires. The analysis compiles one predicate per day kind, so two nodes of one
     *  global carry one predicate; any other pair fails. */
    private String dagsoort(Term.Dagsoort d) {
        String naam = d.naam();
        String global = "rsDagsoort_" + Namen.ident(naam, true);
        if (!global.codePoints().allMatch(Character::isJavaIdentifierPart))
            throw new Weigering("de dagsoort '" + naam + "', waarvan de global geen Java-identifier is");
        Term.Dagsoort eerder = dagsoorten.putIfAbsent(global, d);
        if (eerder != null && !eerder.naam().equals(naam))
            throw new IllegalStateException("de dagsoorten '" + eerder.naam() + "' en '" + naam + "' delen de global "
                    + global);
        if (eerder != null && eerder.predikaat() != d.predikaat())
            throw new IllegalStateException("de dagsoort '" + naam + "' heeft in het plan twee predicaten");
        return global;
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
        private int sonde = -1;

        void parameter(String type, String naam, String argument) {
            for (String[] p : parameters)
                if (p[1].equals(naam)) throw new IllegalStateException("de parameter " + naam + " staat er twee keer");
            parameters.add(new String[]{type, naam, argument});
        }

        boolean heeft(String naam) {
            for (String[] p : parameters) if (p[1].equals(naam)) return true;
            return false;
        }

        /** The declaration of the parameters. */
        String declaratie() {
            List<String> d = new ArrayList<>();
            for (String[] p : parameters) d.add(p[0] + " " + p[1]);
            return String.join(", ", d);
        }

        /** The arguments of the call in the rule. */
        String argumenten() {
            List<String> a = new ArrayList<>();
            for (String[] p : parameters) a.add(p[2]);
            return String.join(", ", a);
        }

        /** The arguments of the call of a nested function: the parameters themselves. */
        String doorgeven() {
            List<String> a = new ArrayList<>();
            for (String[] p : parameters) a.add(p[1]);
            return String.join(", ", a);
        }

        /** This context with the cell {@code cel} of a {@code Let}, a local of that {@code Let}'s function. */
        Context metCel(String cel) {
            Context c = new Context();
            for (String[] p : parameters) c.parameters.add(new String[]{p[0], p[1], p[1]});
            c.slots.putAll(slots);
            c.binders.putAll(binders);
            c.sonde = sonde;
            c.parameter("Object", cel, cel);
            return c;
        }
    }

    // ---------------- one rule ----------------

    /** The rendering of the plan of one rule, the {@code n}th of the plan: its functions and its rule. */
    private final class Regel {
        private final int n;
        private final Regelplan plan;
        /** The functions of this rule, in the order of their names. */
        private final List<String> functies = new ArrayList<>();
        /** The variables the patterns of this rule bind to their objects, as far as they are in scope. */
        private Set<String> patroonvariabelen = new HashSet<>();

        Regel(int n, Regelplan plan) {
            this.n = n;
            this.plan = plan;
        }

        void weergeven() {
            List<String> when = new ArrayList<>();
            for (LhsItem item : plan.linkerkant()) when.add(item(item));
            String then = gevolg(plan.gevolg());
            for (String f : functies) {
                if (f == null) throw new IllegalStateException("een functie van " + plan.naam() + " is niet geschreven");
                regels.append(f).append('\n');
            }
            // the plan's name escaped as a string literal, which the parser reads back as that name (RegelnaamWitnessTest):
            // vormVan, the scenario report and the creation driver look a fired rule up by the name of its plan
            regels.append("rule ").append(str(plan.naam())).append("\n");
            regels.append("    dialect \"mvel\"\n");
            if (plan.agendagroep() != null) regels.append("    agenda-group ").append(str(plan.agendagroep())).append('\n');
            regels.append("when\n");
            for (String w : when) regels.append("    ").append(w).append('\n');
            regels.append("then\n").append(then).append("end\n\n");
        }

        /** A place for a function of this rule, reserved where its node is met, so the functions stand in the order of
         *  the plan. */
        private int reserveer() {
            functies.add(null);
            return functies.size() - 1;
        }

        private String naam(String soort, int plaats) {
            return "r" + n + "_" + soort + plaats;
        }

        /** Writes the function at {@code plaats}: the RegelSpraak sentence {@code bron} as a comment, where the node has
         *  one, its head and its body. */
        private void vul(int plaats, String bron, String kop, String lichaam) {
            StringBuilder f = new StringBuilder();
            if (bron != null) f.append("// ").append(Namen.commentVeilig(bron)).append('\n');
            f.append("function ").append(kop).append(" {\n").append(lichaam).append("}\n");
            functies.set(plaats, f.toString());
        }

        /** The variable of slot {@code slot}: the one a binding of the left-hand side binds. */
        private String variabele(int slot) {
            Lezing l = plan.lezingen().slots().get(slot);
            if (l.naam() == null)
                throw new IllegalStateException("slot " + slot + " van " + plan.naam() + " heeft geen variabele: " + l);
            return l.naam();
        }

        // ---------------- the left-hand side ----------------

        private String item(LhsItem item) {
            return switch (item) {
                case Onderwerp o -> patroon(o.naam(), o.bonenklasse(), o.alfa(), o.joins(), o.betatoetsen(), List.of(),
                        o.bindingen());
                case Patroon p -> patroon(p);
                case Niet x -> "not( " + gekwantificeerd(x.patronen()) + " )";
                case Bestaat x -> "exists( " + gekwantificeerd(x.patronen()) + " )";
                case Accumulatie a -> accumulatie(a);
                case Toetsitem t -> toets(t);
            };
        }

        private String patroon(Patroon p) {
            return patroon(p.naam(), p.feitklasse(), p.alfa(), p.joins(), p.betatoetsen(), p.naAlfa(), p.bindingen());
        }

        /** A pattern: its constraints on its object, its identity joins, its constraints between its object and
         *  another, its constraints on its object after those, and its bindings, in that order. */
        private String patroon(String naam, Class<?> klasse, List<Alfa> alfa, List<Join> joins,
                               List<Betatoets> betatoetsen, List<Alfa> naAlfa, List<Binding> bindingen) {
            List<String> c = new ArrayList<>();
            for (Alfa a : alfa) c.add(alfa(klasse, a));
            for (Join j : joins) c.addAll(join(klasse, j));
            for (Betatoets b : betatoetsen) c.add(betatoets(b));
            for (Alfa a : naAlfa) c.add(alfa(klasse, a));
            for (Binding b : bindingen) c.add(binding(klasse, b));
            patroonvariabelen.add(naam);
            return naam + " : " + typenaam(klasse) + (c.isEmpty() ? "()" : "( " + String.join(", ", c) + " )");
        }

        /** The patterns of a negation or an existence; what they bind lives only inside it. */
        private String gekwantificeerd(List<Patroon> patronen) {
            Set<String> buiten = new HashSet<>(patroonvariabelen);
            List<String> p = new ArrayList<>();
            for (Patroon x : patronen) p.add(patroon(x));
            patroonvariabelen = buiten;
            return String.join(" and ", p);
        }

        private String accumulatie(Accumulatie a) {
            String bron = patroon(a.bron());
            List<String> f = new ArrayList<>();
            for (Accumulatie.Functie x : a.functies()) {
                String over = x.bron() == null ? a.bron().naam() : x.bron();
                f.add(x.doel() + " : " + switch (x.soort()) {
                    case AANTAL -> "count()";
                    case SOM -> "sumBD(" + over + ")";
                    case MINIMUM -> "min(" + over + ")";
                    case MAXIMUM -> "max(" + over + ")";
                    case LIJST -> "collectList(" + over + ")";
                });
            }
            return "accumulate( " + bron + "; " + String.join(", ", f) + " )";
        }

        /** An identity join, which meets nothing where a side reads null ({@link Join}): where the pattern's side is a
         *  field and the other side can read null, a field of an object or a value that is no pattern's object, the
         *  field is tested for null first, since DRL finds two nulls equal. */
        private List<String> join(Class<?> klasse, Join j) {
            String links = j.veld() == null ? "this" : veldnaam(klasse, j.eigenschap(), j.veld());
            String rechts = j.doelveld() == null ? j.doel() : j.doel() + "." + veldnaam(j.doelveld().klasse(),
                    j.doelveld().eigenschap(), j.doelveld().naam());
            boolean rechtsLeeg = j.doelveld() != null || !patroonvariabelen.contains(j.doel());
            List<String> uit = new ArrayList<>();
            if (j.veld() != null && rechtsLeeg) uit.add(links + " != null");
            uit.add(links + " == " + rechts);
            return uit;
        }

        /** A constraint between the pattern's object and the object or value another pattern binds: a function that
         *  calls the plan's function with its constants. */
        private String betatoets(Betatoets b) {
            int plaats = reserveer();
            String naam = naam("beta", plaats);
            Method m = methode(b.functie().klasse(), b.functie().methode());
            Class<?>[] p = m.getParameterTypes();
            if (p.length != 2 + b.constanten().size())
                throw new IllegalStateException(b.functie() + " neemt " + p.length + " argumenten, niet "
                        + (2 + b.constanten().size()));
            List<String> a = new ArrayList<>(List.of(cast(p[0], new Expr("eigen", Object.class)),
                    cast(p[1], new Expr("ander", Object.class))));
            for (int i = 0; i < b.constanten().size(); i++) a.add(cast(p[2 + i], constante(b.constanten().get(i).waarde())));
            Expr aanroep = new Expr(functie(b.functie()) + "(" + String.join(", ", a) + ")", m.getReturnType());
            vul(plaats, null, "boolean " + naam + "(Object eigen, Object ander)", "    return " + alsBoolean(aanroep)
                    + ";\n");
            return naam + "(this, " + b.doel() + ")";
        }

        /** A value a pattern binds: a field of a fact of the branch as it is; a member of a bean by its name in DRL,
         *  or by the route {@code lees} where the pattern's class does not declare it, read as its form says; along a path
         *  a function that walks it. */
        private String binding(Class<?> klasse, Binding b) {
            if (!b.pad().isEmpty())
                return b.doel() + " : (" + padfunctie(b.pad(), b.veld().eigenschap(), b.vorm(), b.soort()) + "(this))";
            if (!isBean(klasse)) {
                if (b.vorm() != Binding.Leesvorm.WAARDE)
                    throw new IllegalStateException("een feit van de tak wordt gelezen zoals het is: " + b);
                return b.doel() + " : " + b.veld().eigenschap();
            }
            boolean eigen = b.veld().klasse().isAssignableFrom(klasse);
            String lees = eigen ? veldnaam(klasse, b.veld().eigenschap(), b.veld().naam())
                    : route(Route.LEES) + "(this, " + str(b.veld().eigenschap()) + ")";
            if (b.vorm() == Binding.Leesvorm.WAARDE) return b.doel() + " : " + (eigen ? lees : "(" + lees + ")");
            return b.doel() + " : (" + leesvorm(lees, b.vorm(), b.soort()) + ")";
        }

        /** How the text spells the field {@code eigenschap} of an object of {@code klasse}: a component of a fact of
         *  the branch by its name, a member of a bean by the name Drools reads its getter under
         *  ({@code Namen.lidnamen}); a member {@code naam} that Drools reads as something else where a pattern spells
         *  its name is refused, by name, since a constraint on it could hold or fail without a word. */
        private String veldnaam(Class<?> klasse, String eigenschap, String naam) {
            if (!isBean(klasse)) return eigenschap;
            String drl = Namen.lidnamen(eigenschap).drlEigenschap();
            if (drl == null) throw new Weigering("het lid " + str(naam));
            return drl;
        }

        /** {@code lees} read as {@code vorm}, through its route. */
        private String leesvorm(String lees, Binding.Leesvorm vorm, Class<?> soort) {
            return switch (vorm) {
                case WAARDE -> lees;
                case WAAR -> route(Route.WAAR) + "(" + lees + ")";
                case TIJDVAKKEN -> route(Route.TIJDVAKKEN) + "(" + lees + ")";
                case GEZET -> route(Route.GEZET) + "(" + lees + ", " + (soort == null ? "null" : str(soort.getName()))
                        + ")";
            };
        }

        /** A function of the object a pattern matches that walks the reference fields {@code pad} with the route
         *  {@code langs}, null where a link is absent, and reads of the object reached the member {@code eigenschap}
         *  (the object itself where it is null) as {@code vorm} says. */
        private String padfunctie(List<Binding.Veld> pad, String eigenschap, Binding.Leesvorm vorm, Class<?> soort) {
            int plaats = reserveer();
            String naam = naam("pad", plaats);
            List<String> stappen = new ArrayList<>();
            for (Binding.Veld v : pad) stappen.add(str(v.eigenschap()));
            String langs = route(Route.LANGS) + "(o, java.util.Arrays.asList(" + String.join(", ", stappen) + "))";
            if (eigenschap == null && vorm == Binding.Leesvorm.WAARDE) {
                vul(plaats, null, "Object " + naam + "(Object o)", "    return " + langs + ";\n");
                return naam;
            }
            String lees = eigenschap == null ? "bereikt" : route(Route.LEES) + "(bereikt, " + str(eigenschap) + ")";
            vul(plaats, null, "Object " + naam + "(Object o)", "    Object bereikt = " + langs + ";\n"
                    + "    return bereikt == null ? null : " + leesvorm(lees, vorm, soort) + ";\n");
            return naam;
        }

        /** An alpha constraint: its field tests in order, as one constraint. */
        private String alfa(Class<?> klasse, Alfa a) {
            List<String> t = new ArrayList<>();
            for (Alfa.Veldtoets v : a.toetsen()) t.add(veldtoets(klasse, v));
            return String.join(" && ", t);
        }

        private String veldtoets(Class<?> klasse, Alfa.Veldtoets t) {
            String lees;
            if (!t.pad().isEmpty()) {
                lees = padfunctie(t.pad(), null, t.vorm(), null) + "(this)";
            } else {
                // a member the pattern's class does not declare is read through the class of the object itself
                String basis = t.veld() == null ? "this"
                        : t.declarant() != null ? route(Route.LEES) + "(this, " + str(t.eigenschap()) + ")"
                        : veldnaam(klasse, t.eigenschap(), t.veld());
                lees = leesvorm(basis, t.vorm(), null);
            }
            Object w = t.waarde();
            return switch (t.op()) {
                case GELIJK -> lees + " == " + mvelconstante(w);
                case NIET_GELIJK -> lees + " != " + mvelconstante(w);
                case MINSTENS -> lees + " >= " + ((Number) w).intValue();
                case ONDER -> lees + " < " + ((Number) w).intValue();
                case BINNEN -> {
                    Geldigheid g = (Geldigheid) w;
                    yield route(Route.BINNEN) + "(" + lees + ", " + (g.vanaf() == null ? "null" : str(g.vanaf().toString()))
                            + ", " + (g.tot() == null ? "null" : str(g.tot().toString())) + ")";
                }
                case IS_EEN -> route(Route.IS_EEN) + "(" + lees + ", " + str(((Class<?>) w).getName()) + ")";
                case LEEG -> lees + " == null";
                case GEVULD -> lees + " != null";
                case KLASSE_NIET_IN -> {
                    List<String> namen = new ArrayList<>();
                    for (Object k : (Collection<?>) w) namen.add(str(((Class<?>) k).getName()));
                    yield route(Route.KLASSE_NIET_IN) + "(" + lees + ", " + lijst(namen) + ")";
                }
                case KLASSE_IS -> route(Route.KLASSE_IS) + "(" + lees + ", " + str(((Class<?>) w).getName()) + ")";
            };
        }

        /** A list of the MVEL expressions {@code elementen}: through a route of its length up to six, else a function
         *  that returns it. */
        private String lijst(List<String> elementen) {
            if (elementen.size() <= 6)
                return route(Route.valueOf("LIJST" + elementen.size())) + "(" + String.join(", ", elementen) + ")";
            int plaats = reserveer();
            String naam = naam("lijst", plaats);
            vul(plaats, null, "java.util.List " + naam + "()", "    return java.util.Arrays.asList("
                    + String.join(", ", elementen) + ");\n");
            return naam + "()";
        }

        /** The test of the rule: a function that asks of the condition what its role says, called in an {@code eval}. */
        private String toets(Toetsitem t) {
            int plaats = reserveer();
            String naam = naam("toets", plaats);
            boolean deponering = t.rol() == Toetsitem.Rol.DEPONERING;
            Context c = context(t.slots(), deponering ? t.sonde() : -1, route(Route.VERSE_SONDE) + "()", t.toets());
            String b = bool(t.toets(), c);
            String gevangen = "        " + route(Route.GEVANGEN) + "(e);\n";
            String lichaam = switch (t.rol()) {
                case VOORWAARDE, VORM -> "    return " + b + ";\n";
                case ZONDER_FOUT -> vang("        return " + b + ";\n", gevangen + "        return false;\n");
                case FOUT -> vang("        boolean uitkomst = " + b + ";\n        return false;\n",
                        gevangen + "        return true;\n");
                case DEPONERING -> vang("        boolean uitkomst = " + b + ";\n", gevangen) + "    return "
                        + route(Route.GEMELD) + "(" + SONDE + ");\n";
            };
            vul(plaats, t.toets().bron(), "boolean " + naam + "(" + c.declaratie() + ")", lichaam);
            return "eval( " + naam + "(" + c.argumenten() + ") )";
        }

        /** A try with {@code probeer} as its body and {@code vangst} as the body of its catch of a
         *  {@code RuntimeException e}. */
        private String vang(String probeer, String vangst) {
            return "    try {\n" + probeer + "    } catch (RuntimeException e) {\n" + vangst + "    }\n";
        }

        /** The context of a function of a position: a parameter per slot the position passes, in its order; the probe
         *  where the position has one, with {@code sondeArgument} as what the rule passes; and the global of each day
         *  kind the terms name. */
        private Context context(List<Integer> slots, int sonde, String sondeArgument, Term... termen) {
            Context c = new Context();
            for (int s : slots) {
                String v = variabele(s);
                c.parameter("Object", v, v);
                c.slots.put(s, v);
            }
            if (sonde >= 0) {
                c.parameter("Object", SONDE, sondeArgument);
                c.sonde = sonde;
            }
            Set<String> globals = new LinkedHashSet<>();
            for (Term t : termen) dagsoorten(t, globals);
            for (String d : globals) c.parameter("java.util.function.Predicate", d, d);
            return c;
        }

        // ---------------- the terms ----------------

        /** {@code t}, a condition, as a Java truth value. */
        private String bool(Term t, Context c) {
            if (t instanceof Term.Samengesteld s) return samengesteld(s, c);
            if (t instanceof Term.ZonderFout z) return zonderFout(z, c);
            return alsBoolean(waarde(t, c));
        }

        /** {@code t} as a Java expression in the function whose context is {@code c}. */
        private Expr waarde(Term t, Context c) {
            return switch (t) {
                case Term.Const k -> constante(k.waarde());
                case Term.Lees l -> {
                    String p = c.slots.get(l.slot());
                    if (p == null)
                        throw new IllegalStateException("de term leest slot " + l.slot() + ", dat haar plaats in "
                                + plan.naam() + " niet doorgeeft");
                    yield new Expr(p, Object.class);
                }
                case Term.Invoer i -> new Expr(leesvorm(route(Route.LEES) + "(" + waarde(i.object(), c).tekst() + ", "
                        + str(i.veld().eigenschap()) + ")", i.vorm(), null), Object.class);
                case Term.Binder b -> {
                    String aanroep = c.binders.get(b.slot());
                    if (aanroep == null) throw new IllegalStateException("de binder " + b.naam() + " buiten zijn Let");
                    yield new Expr(aanroep, Object.class);
                }
                case Term.Let l -> new Expr(let(l, c), Object.class);
                case Term.Keuze k -> new Expr("(" + bool(k.voorwaarde(), c) + " ? (Object) " + waarde(k.dan(), c).tekst()
                        + " : (Object) " + waarde(k.anders(), c).tekst() + ")", Object.class);
                case Term.Aanroep a -> aanroep(a.functie(), a.argumenten().stream().map(x -> waarde(x, c)).toList());
                case Term.Herschaal h -> aanroep(h.functie(), h.noemer() == null
                        ? List.of(waarde(h.term(), c), constante(h.teller()))
                        : List.of(waarde(h.term(), c), constante(h.teller()), constante(h.noemer())));
                case Term.Deel d -> {
                    Expr links = waarde(d.links(), c), rechts = waarde(d.rechts(), c);
                    yield aanroep(d.functie(), List.of(links, rechts, sonde(d.sonde(), c)));
                }
                case Term.Wortel w -> aanroep(w.functie(), List.of(waarde(w.radicand(), c), sonde(w.sonde(), c)));
                case Term.Faal f -> routeaanroep(Route.FAAL, List.of(constante(f.zin())));
                case Term.Vereist v -> routeaanroep(Route.VEREIST, List.of(waarde(v.waarde(), c), constante(v.zin())));
                case Term.Tekstreeks s -> {
                    List<String> delen = new ArrayList<>();
                    for (Term x : s.delen()) delen.add(waarde(x, c).tekst());
                    yield routeaanroep(Route.TEKSTREEKS, List.of(new Expr("java.util.Arrays.asList("
                            + String.join(", ", delen) + ")", List.class)));
                }
                case Term.GelijsteVouw g -> {
                    Expr tot = new Expr("null", null);
                    for (Term x : g.operanden()) tot = aanroep(g.functie(), List.of(tot, waarde(x, c)));
                    yield tot;
                }
                case Term.Vouwlezing v -> new Expr(vouwlezing(v, c), Object.class);
                case Term.Samengesteld s -> new Expr("Boolean.valueOf(" + samengesteld(s, c) + ")", Boolean.class);
                case Term.Kwantorlezing q -> new Expr(kwantorlezing(q, c), Object.class);
                case Term.ZonderFout z -> new Expr("Boolean.valueOf(" + zonderFout(z, c) + ")", Boolean.class);
                case Term.Dagsoort d -> {
                    String global = dagsoort(d);
                    if (!c.heeft(global)) throw new IllegalStateException("de dagsoort " + d.naam() + " buiten haar context");
                    yield new Expr(global, java.util.function.Predicate.class);
                }
            };
        }

        /** The probe a place reports to: the position's, where the plan names it, or none with slot -1. */
        private Expr sonde(int slot, Context c) {
            if (slot < 0) return new Expr("null", null);
            if (slot != c.sonde)
                throw new IllegalStateException("een plaats in " + plan.naam() + " meldt aan slot " + slot
                        + ", niet aan de sonde van haar positie");
            return new Expr(SONDE, Object.class);
        }

        /** A call of {@code f} on {@code argumenten}, evaluated in that order, each cast as its parameter takes it. */
        private Expr aanroep(Functie f, List<Expr> argumenten) {
            Method m = methode(f.klasse(), f.methode());
            return new Expr(functie(f) + "(" + argumentlijst(m, argumenten) + ")", m.getReturnType());
        }

        private Expr routeaanroep(Route r, List<Expr> argumenten) {
            Method m = methode(r.klasse(), r.methode());
            return new Expr(route(r) + "(" + argumentlijst(m, argumenten) + ")", m.getReturnType());
        }

        private String argumentlijst(Method m, List<Expr> argumenten) {
            Class<?>[] p = m.getParameterTypes();
            if (p.length != argumenten.size())
                throw new IllegalStateException(m.getName() + " neemt " + p.length + " argumenten, niet "
                        + argumenten.size());
            List<String> a = new ArrayList<>();
            for (int i = 0; i < p.length; i++) a.add(cast(p[i], argumenten.get(i)));
            return String.join(", ", a);
        }

        /** A composite: its members read in order, the stop triggers of RUNTIME-SEMANTICS ch. 5.10 tested before
         *  each, the quantifier's verdict where none stops it. */
        private String samengesteld(Term.Samengesteld s, Context c) {
            int plaats = reserveer();
            String naam = naam("samengesteld", plaats);
            String kw = enumlabel(s.kwantor());
            int n = s.leden().size();
            StringBuilder b = new StringBuilder("    int raak = 0;\n    Boolean vast;\n");
            for (int i = 0; i < n; i++) {
                b.append("    vast = ").append(route(Route.VASTGESTELD)).append("(").append(kw).append(", ")
                        .append(s.drempel()).append(", raak, ").append(i).append(", ").append(n - i).append(");\n");
                b.append("    if (vast != null) {\n        return vast;\n    }\n");
                b.append("    if (").append(bool(s.leden().get(i), c)).append(") {\n        raak++;\n    }\n");
            }
            b.append("    return ").append(route(Route.EINDOORDEEL)).append("(").append(kw).append(", ")
                    .append(s.drempel()).append(", raak, ").append(n).append(");\n");
            vul(plaats, s.bron(), "boolean " + naam + "(" + c.declaratie() + ")", b.toString());
            return naam + "(" + c.doorgeven() + ")";
        }

        /** A condition where its evaluation answers, and not holding where it faults. */
        private String zonderFout(Term.ZonderFout z, Context c) {
            int plaats = reserveer();
            String naam = naam("zonderfout", plaats);
            String b = bool(z.voorwaarde(), c);
            vul(plaats, z.bron(), "boolean " + naam + "(" + c.declaratie() + ")", vang("        return " + b + ";\n",
                    "        " + route(Route.GEVANGEN) + "(e);\n        return false;\n"));
            return naam + "(" + c.doorgeven() + ")";
        }

        /** The evaluation of a fold in the rule that reads it: its filter's deposit heard by the position's probe, then
         *  the first filter fault, then the first member fault, then the fold's value. */
        private String vouwlezing(Term.Vouwlezing v, Context c) {
            int plaats = reserveer();
            String naam = naam("vouwlezing", plaats);
            StringBuilder b = new StringBuilder();
            if (v.sonde() >= 0) {
                if (v.deponeringen() < 0)
                    throw new IllegalStateException("een vouwlezing met een sonde en zonder deponeringen in " + plan.naam());
                b.append("    if (((Number) ").append(slot(v.deponeringen(), c)).append(").longValue() > 0) {\n        ")
                        .append(route(Route.MELD)).append("(").append(sonde(v.sonde(), c).tekst()).append(");\n    }\n");
            }
            if (v.filterfout() >= 0) {
                String f = slot(v.filterfout(), c);
                b.append("    if (").append(f).append(" != null) {\n        throw ((").append(typenaam(Filterfout.class))
                        .append(") ").append(f).append(").fout();\n    }\n");
            }
            if (v.lidfout() >= 0) {
                String f = slot(v.lidfout(), c);
                b.append("    if (").append(f).append(" != null) {\n        throw ((").append(typenaam(Lidfout.class))
                        .append(") ").append(f).append(").fout();\n    }\n");
            }
            b.append("    return ").append(waarde(v.uitkomst(), c).tekst()).append(";\n");
            vul(plaats, v.bron(), "Object " + naam + "(" + c.declaratie() + ")", b.toString());
            return naam + "(" + c.doorgeven() + ")";
        }

        private String slot(int slot, Context c) {
            String p = c.slots.get(slot);
            if (p == null)
                throw new IllegalStateException("slot " + slot + " staat niet in de context van een term van " + plan.naam());
            return p;
        }

        /** The verdict of a quantifier fold whose matrix can fault or deposit, through its function. */
        private String kwantorlezing(Term.Kwantorlezing q, Context c) {
            int plaats = reserveer();
            String naam = naam("kwantorlezing", plaats);
            Expr aanroep = aanroep(q.functie(), List.of(waarde(q.aantal(), c), waarde(q.bijdragen(), c),
                    constante(q.kwantor()), constante(q.drempel()), sonde(q.sonde(), c)));
            vul(plaats, q.bron(), "Object " + naam + "(" + c.declaratie() + ")", "    return " + aanroep.tekst() + ";\n");
            return naam + "(" + c.doorgeven() + ")";
        }

        /**
         * A {@code Let} by need (RUNTIME-SEMANTICS ch. 10.6): a function per {@code Let}, which makes the cell of its
         * binder and evaluates its body, and a function per binder, which evaluates the value in the enclosing scope
         * at the first read and serves that result to every further read. A leeg value raises the barrier of that cell,
         * which the {@code Let}'s own function catches as leeg; a barrier of an enclosing {@code Let} passes on.
         */
        private String let(Term.Let l, Context c) {
            int plaats = reserveer();
            String naam = naam("let", plaats);
            int binderplaats = reserveer();
            String binder = naam("binder", binderplaats);
            String cel = "cel_" + l.slot();
            Context binnen = c.metCel(cel);
            binnen.binders.put(l.slot(), binder + "(" + binnen.doorgeven() + ")");
            String waarde = waarde(l.waarde(), c).tekst();
            String lichaam = waarde(l.lichaam(), binnen).tekst();
            vul(binderplaats, l.waarde().bron(), "Object " + binder + "(" + binnen.declaratie() + ")",
                    "    if (!" + route(Route.BINDERCEL_GEVULD) + "(" + cel + ")) {\n        "
                            + route(Route.VUL_BINDERCEL) + "(" + cel + ", " + waarde + ");\n    }\n    return "
                            + route(Route.BINDERWAARDE) + "(" + cel + ");\n");
            vul(plaats, l.bron(), "Object " + naam + "(" + c.declaratie() + ")",
                    "    Object " + cel + " = " + route(Route.BINDERCEL) + "();\n    try {\n        return " + lichaam
                            + ";\n    } catch (" + typenaam(Leegbarriere.class) + " barriere) {\n        return "
                            + route(Route.OPGEVANGEN) + "(barriere, " + cel + ");\n    }\n");
            return naam + "(" + c.doorgeven() + ")";
        }

        // ---------------- the consequence ----------------

        private String gevolg(Gevolg g) {
            StringBuilder b = new StringBuilder();
            switch (g) {
                case Gevolg.Waarde x -> waarde(x, b);
                case Gevolg.Kenmerk x -> voegIn(b, "    ", x.invoeging(), "new " + typenaam(HeeftKenmerk.class) + "("
                        + x.onderwerp() + ", " + sleutel(x.kenmerk()) + ")");
                case Gevolg.Toetsfout x -> toetsfout(x, b);
                case Gevolg.Fout x -> voegIn(b, "    ", x.invoeging(), "new " + typenaam(Fout.class) + "("
                        + x.onderwerp() + ", " + str(x.fout().vorm()) + ", " + route(Route.REKENFOUT) + "("
                        + str(x.zin()) + ", " + str(x.fout().regel()) + "))");
                case Gevolg.Inconsistentie x -> voegIn(b, "    ", x.invoeging(), route(Route.INCONSISTENTIE) + "("
                        + str(x.vorm()) + ", " + x.onderwerp() + ")");
                case Gevolg.Schrijversconflict x -> voegIn(b, "    ", x.invoeging(), route(Route.SCHRIJVERSCONFLICT)
                        + "(" + x.onderwerp() + ", " + variabele(x.waarden()) + ", " + str(x.attribuut()) + ", "
                        + lijst(teksten(x.documentvolgorde())) + ", " + lijst(teksten(x.typeklassen())) + ", "
                        + lijst(teksten(x.typen())) + ")");
                case Gevolg.Parameterstand x -> voegIn(b, "    ", x.invoeging(), "new "
                        + typenaam(ParameterStand.class) + "(" + str(x.parameter()) + ", " + x.global() + ")");
                case Gevolg.Rekendatumstand x -> voegIn(b, "    ", x.invoeging(), "new " + typenaam(Rekendatum.class)
                        + "(" + x.global() + ")");
                case Gevolg.Kenmerkstand x -> voegIn(b, "    ", x.invoeging(), "new " + typenaam(KenmerkStand.class)
                        + "(" + x.onderwerp() + ", " + sleutel(x.kenmerk()) + ", " + x.waar() + ")");
                case Gevolg.Stand x -> voegIn(b, "    ", x.invoeging(), "new " + typenaam(Stand.class) + "("
                        + x.onderwerp() + ", " + sleutel(x.attribuut()) + ", " + (x.emmerscalair()
                        ? route(Route.EMMERSCALAIR) + "(" + variabele(x.waarde()) + ")" : variabele(x.waarde())) + ")");
                case Gevolg.Padstand x -> voegIn(b, "    ", x.invoeging(), "new " + typenaam(PadStand.class) + "("
                        + x.onderwerp() + ", " + str(x.pad()) + ", " + sleutel(x.lid()) + ", "
                        + (x.bron() == Gevolg.Standbron.ONTBREKENDE_LINK ? "null" : variabele(x.waarde())) + ")");
                case Gevolg.Bijdrage x -> bijdrage(x, b);
                case Gevolg.Filterdeponering x -> voegIn(b, "    ", x.invoeging(), "new "
                        + typenaam(Filterdeponering.class) + "(" + str(x.vouw()) + ", " + x.onderwerp() + ", "
                        + variabele(x.lid()) + ")");
                case Gevolg.Filterfout x -> filterfout(x, b);
                case Gevolg.Lidfout x -> voegIn(b, "    ", x.invoeging(), "new " + typenaam(Lidfout.class) + "("
                        + str(x.vouw()) + ", " + x.onderwerp() + ", " + variabele(x.lid()) + ", "
                        + route(Route.INVOERVORMFOUT) + "(" + str(x.voor()) + ", " + variabele(x.lid()) + ", "
                        + str(x.na()) + ", null), " + vouwplaats(x.plaats(), x.vouw(), x.lid(), null) + ")");
                case Gevolg.Uniciteitssleutel x -> uniciteitssleutel(x, b);
                case Gevolg.Uniciteitsschending x -> voegIn(b, "    ", x.invoeging(), "new "
                        + typenaam(Uniciteitsschending.class) + "(" + str(x.uniciteit()) + ", " + x.onderwerp() + ")");
                case Gevolg.Invoervormfout x -> voegIn(b, "    ", x.invoeging(), "new " + typenaam(Fout.class) + "("
                        + x.onderwerp() + ", " + str(x.fout().vorm()) + ", " + route(Route.INVOERVORMFOUT) + "("
                        + str(x.voor()) + ", " + x.onderwerp() + ", " + str(x.na()) + ", " + str(x.fout().regel())
                        + "))");
                case Gevolg.Kanaalstand x -> voegIn(b, "    ", x.invoeging(), "new " + typenaam(KanaalStand.class)
                        + "(" + str(x.producent()) + ", " + x.inconsistent() + ")");
                case Gevolg.Afloop x -> afloop(x, b);
                case Gevolg.Conflict x -> conflict(x, b);
                case Gevolg.Creatie x -> creatie(x, b);
                case Gevolg.Koppeling x -> voegIn(b, "    ", x.invoeging(), "new " + typenaam(Koppeling.class) + "("
                        + str(x.relatie()) + ", " + x.eerste() + ", " + x.tweede() + ", "
                        + (x.fase() < 0 ? "0" : variabele(x.fase())) + ")");
                case Gevolg.TweedeVulling x -> voegIn(b, "    ", x.invoeging(), "new " + typenaam(Fout.class) + "("
                        + x.onderwerp() + ", " + str(x.relatie()) + ", new " + typenaam(IllegalStateException.class)
                        + "(" + str(x.zin()) + "))");
                case Gevolg.Ontvanger x -> voegIn(b, "    ", x.invoeging(), "new " + typenaam(Ontvanger.class) + "("
                        + str(x.verdeling()) + ", " + x.onderwerp() + ", " + variabele(x.ontvanger()) + ", "
                        + slotOfNull(x.gewicht()) + ", " + slotOfNull(x.sleutel()) + ", " + slotOfNull(x.maximum()) + ")");
                case Gevolg.Uitdeling x -> uitdeling(x, b);
            }
            return b.toString();
        }

        private List<String> teksten(List<String> waarden) {
            List<String> uit = new ArrayList<>();
            for (String w : waarden) uit.add(str(w));
            return uit;
        }

        private void voegIn(StringBuilder b, String inspringing, Gevolg.Invoeging invoeging, String feit) {
            b.append(inspringing).append(invoeging == Gevolg.Invoeging.LOGISCH ? "insertLogical" : "insert").append('(')
                    .append(feit).append(");\n");
        }

        /** The value of {@link Gevolg.Waarde}: a function that evaluates it and gives an evaluation fault as its
         *  outcome; the fault in place of the value and of the deposit, else the deposit first and then the value,
         *  where it is gevuld or the plan writes leeg. */
        private void waarde(Gevolg.Waarde x, StringBuilder b) {
            Gevolg.Deponering d = x.deponering();
            int plaats = reserveer();
            String naam = naam("waarde", plaats);
            Context c = context(x.slots(), d == null ? -1 : d.sonde(), SONDE, x.waarde());
            Expr e = waarde(x.waarde(), c);
            String v = x.collapse() ? aanroep(Functie.COLLAPSE, List.of(e)).tekst() : e.tekst();
            vul(plaats, x.waarde().bron(), "Object " + naam + "(" + c.declaratie() + ")",
                    vang("        return " + v + ";\n", "        return " + route(Route.GEVANGEN) + "(e);\n"));
            if (d != null) b.append("    Object ").append(SONDE).append(" = ").append(route(Route.VERSE_SONDE)).append("();\n");
            b.append("    Object v = ").append(naam).append("(").append(c.argumenten()).append(");\n");
            b.append("    if (").append(route(Route.IS_EVALUATIEFOUT)).append("(v)) {\n");
            voegIn(b, "        ", x.invoeging(), "new " + typenaam(Fout.class) + "(" + x.onderwerp() + ", "
                    + str(x.fout().vorm()) + ", " + route(Route.BENOEM) + "(v, " + str(x.fout().regel()) + "))");
            b.append("    } else {\n");
            if (d != null) {
                b.append("        if (").append(route(Route.GEMELD)).append("(").append(SONDE).append(")) {\n");
                voegIn(b, "            ", x.invoeging(), route(Route.INCONSISTENTIE) + "(" + str(d.vorm()) + ", "
                        + x.onderwerp() + ")");
                b.append("        }\n");
            }
            String feit = "new " + typenaam(Waarde.class) + "(" + x.onderwerp() + ", " + sleutel(x.doel()) + ", v, "
                    + str(x.schrijver()) + ")";
            if (x.schrijftLeeg()) {
                voegIn(b, "        ", x.invoeging(), feit);
            } else {
                b.append("        if (!").append(route(Route.IS_LEEG)).append("(v)) {\n");
                voegIn(b, "            ", x.invoeging(), feit);
                b.append("        }\n");
            }
            b.append("    }\n");
        }

        /** The fault of {@link Gevolg.Toetsfout}: the condition evaluated again over the same bound values, its
         *  evaluation fault inserted, and an answer there a broken contract. */
        private void toetsfout(Gevolg.Toetsfout x, StringBuilder b) {
            int plaats = reserveer();
            String naam = naam("fout", plaats);
            Context c = context(x.slots(), -1, null, x.toets());
            String t = bool(x.toets(), c);
            vul(plaats, x.toets().bron(), "Object " + naam + "(" + c.declaratie() + ")",
                    vang("        boolean uitkomst = " + t + ";\n        return null;\n",
                            "        return " + route(Route.GEVANGEN) + "(e);\n"));
            b.append("    Object f = ").append(naam).append("(").append(c.argumenten()).append(");\n");
            b.append("    if (!").append(route(Route.IS_EVALUATIEFOUT)).append("(f)) {\n        ")
                    .append(route(Route.CONTRACTBREUK)).append("(").append(str("de voorwaarde van \"" + x.fout().vorm()
                            + "\" faalde in de toets en niet in het gevolg, over dezelfde gebonden waarden"))
                    .append(");\n    }\n");
            voegIn(b, "    ", x.invoeging(), "new " + typenaam(Fout.class) + "(" + x.onderwerp() + ", "
                    + str(x.fout().vorm()) + ", " + route(Route.BENOEM) + "(f, " + str(x.fout().regel()) + "))");
        }

        /** The contribution of {@link Gevolg.Bijdrage} on the fold's subject: the member itself; its value through
         *  the route, which gives nothing where it is leeg; the verdict of the matrix of a quantifier fold, a function
         *  of the rule; or its test of the member, a function of the rule that takes the member's place first, then
         *  evaluates the matrix with the probe where the plan names one, and gives the verdict or the evaluation
         *  fault. */
        private void bijdrage(Gevolg.Bijdrage x, StringBuilder b) {
            String nieuw = "new " + typenaam(Bijdrage.class) + "(" + str(x.vouw()) + ", " + x.onderwerp() + ", "
                    + variabele(x.lid()) + ", ";
            switch (x.inbreng()) {
                case Gevolg.Inbreng.Telling t -> voegIn(b, "    ", x.invoeging(), nieuw + "null)");
                case Gevolg.Inbreng.Lidwaarde l -> {
                    b.append("    Object v = ").append(route(Route.BIJDRAGE)).append("(").append(str(x.vouw()))
                            .append(", ").append(x.onderwerp()).append(", ").append(variabele(x.lid())).append(", ")
                            .append(variabele(l.waarde())).append(", ")
                            .append(l.totaal() == null ? "null" : str(l.totaal())).append(", ").append(l.gevuld())
                            .append(");\n");
                    b.append("    if (v != null) {\n");
                    voegIn(b, "        ", x.invoeging(), "v");
                    b.append("    }\n");
                }
                case Gevolg.Inbreng.Oordeel o -> {
                    int plaats = reserveer();
                    String naam = naam("matrix", plaats);
                    Context c = context(x.slots(), -1, null, o.matrix());
                    vul(plaats, o.matrix().bron(), "boolean " + naam + "(" + c.declaratie() + ")",
                            "    return " + bool(o.matrix(), c) + ";\n");
                    voegIn(b, "    ", x.invoeging(), nieuw + route(Route.OORDEEL) + "(" + naam + "(" + c.argumenten()
                            + ")))");
                }
                case Gevolg.Inbreng.Lidtoets l -> {
                    int plaats = reserveer();
                    String naam = naam("lidtoets", plaats);
                    Context c = context(x.slots(), l.sonde(), route(Route.VERSE_SONDE) + "()", l.matrix());
                    // the place first, so that a member without one breaks the contract before the matrix is evaluated
                    String lichaam = "    java.util.List plaats = " + vouwplaats(l.plaats(), x.vouw(), x.lid(), c)
                            + ";\n    Boolean waar = null;\n    RuntimeException fout = null;\n"
                            + vang("        waar = Boolean.valueOf(" + bool(l.matrix(), c) + ");\n",
                                    "        fout = " + route(Route.GEVANGEN) + "(e);\n")
                            + "    return new " + typenaam(Lidtoets.class) + "(plaats, waar, fout, "
                            + route(Route.GEMELD) + "(" + (l.sonde() < 0 ? "null" : SONDE) + "));\n";
                    vul(plaats, l.matrix().bron(), "Object " + naam + "(" + c.declaratie() + ")", lichaam);
                    voegIn(b, "    ", x.invoeging(), nieuw + naam + "(" + c.argumenten() + "))");
                }
            }
        }

        /** The keys of {@link Gevolg.Uniciteitssleutel}: a function of the rule gives the key components, each the
         *  value of its term over the bound values, in a list that the Java dialect builds because a component may be
         *  null; the route makes the keys of them, linked to the parent where the form has a scope, and the rule
         *  inserts each. The sentences of the components stand above the function, since a comment in its body would
         *  reach the parser of the text. */
        private void uniciteitssleutel(Gevolg.Uniciteitssleutel x, StringBuilder b) {
            int plaats = reserveer();
            String naam = naam("componenten", plaats);
            Context c = context(x.slots(), -1, null, x.componenten().toArray(new Term[0]));
            StringBuilder lichaam = new StringBuilder("    java.util.List waarden = new java.util.ArrayList();\n");
            List<String> bronnen = new ArrayList<>();
            for (Term t : x.componenten()) {
                lichaam.append("    waarden.add(").append(waarde(t, c).tekst()).append(");\n");
                if (t.bron() != null) bronnen.add(t.bron());
            }
            lichaam.append("    return waarden;\n");
            vul(plaats, bronnen.isEmpty() ? null : String.join("; ", bronnen),
                    "java.util.List " + naam + "(" + c.declaratie() + ")", lichaam.toString());
            b.append("    for (Object k : ").append(route(Route.UNICITEITSSLEUTELS)).append("(").append(str(x.uniciteit()))
                    .append(", ").append(x.bron()).append(", ").append(x.onderwerp()).append(", ")
                    .append(x.scope() < 0 ? "null" : variabele(x.scope())).append(", ").append(naam).append("(")
                    .append(c.argumenten()).append("), ").append(x.tijdlijn()).append(")) {\n");
            voegIn(b, "        ", x.invoeging(), "k");
            b.append("    }\n");
        }

        /** The decision of {@link Gevolg.Afloop}: a function of the rule gives its value, collapsed where the plan says
         *  so, and gives an evaluation fault as its outcome where the plan names a fault, a computed decision; any other
         *  exception leaves the fire, a broken contract, as in the executable model. The fault in place of the
         *  decision; nothing where the value is null, so that the lower defaults decide (RUNTIME-SEMANTICS ch. 7.1);
         *  else the facts of the decision in the order of the plan. */
        private void afloop(Gevolg.Afloop x, StringBuilder b) {
            int plaats = reserveer();
            String naam = naam("afloop", plaats);
            Context c = context(x.slots(), -1, null, x.waarde());
            Expr e = waarde(x.waarde(), c);
            String v = x.collapse() ? aanroep(Functie.COLLAPSE, List.of(e)).tekst() : e.tekst();
            vul(plaats, x.waarde().bron(), "Object " + naam + "(" + c.declaratie() + ")", x.fout() == null
                    ? "    return " + v + ";\n"
                    : vang("        return " + v + ";\n", "        return " + route(Route.GEVANGEN) + "(e);\n"));
            b.append("    Object v = ").append(naam).append("(").append(c.argumenten()).append(");\n");
            if (x.fout() != null) {
                b.append("    if (").append(route(Route.IS_EVALUATIEFOUT)).append("(v)) {\n");
                voegIn(b, "        ", x.invoeging(), "new " + typenaam(Fout.class) + "(" + x.onderwerp() + ", "
                        + str(x.fout().vorm()) + ", " + route(Route.BENOEM) + "(v, " + str(x.fout().regel()) + "))");
                b.append("    } else if (v != null) {\n");
            } else {
                b.append("    if (v != null) {\n");
            }
            for (Gevolg.Stapelfeit f : x.feiten()) voegIn(b, "        ", x.invoeging(), stapelfeit(f, x.onderwerp()));
            b.append("    }\n");
        }

        /** The conflict of {@link Gevolg.Conflict}: a function of the rule gives its key, a constant or the key of the
         *  conflicting member bound off its outcome, and the rule inserts the fact of the plan under it. */
        private void conflict(Gevolg.Conflict x, StringBuilder b) {
            int plaats = reserveer();
            String naam = naam("conflict", plaats);
            Context c = context(x.slots(), -1, null, x.sleutel());
            vul(plaats, x.sleutel().bron(), "Object " + naam + "(" + c.declaratie() + ")",
                    "    return " + waarde(x.sleutel(), c).tekst() + ";\n");
            b.append("    Object v = ").append(naam).append("(").append(c.argumenten()).append(");\n");
            voegIn(b, "    ", x.invoeging(), stapelfeit(x.feit(), x.onderwerp()));
        }

        /** The fact {@code f} of a defaults stack on {@code onderwerp}, with the local {@code v} of the consequence: the
         *  decision, or the key of a conflict. */
        private String stapelfeit(Gevolg.Stapelfeit f, String onderwerp) {
            return switch (f) {
                case Gevolg.Stapelfeit.Waarde x -> "new " + typenaam(Waarde.class) + "(" + onderwerp + ", "
                        + sleutel(x.doel()) + ", v, " + str(x.schrijver()) + ")";
                case Gevolg.Stapelfeit.Kenmerk x -> "new " + typenaam(HeeftKenmerk.class) + "(" + onderwerp + ", "
                        + sleutel(x.kenmerk()) + ")";
                case Gevolg.Stapelfeit.Beslist x -> "new " + typenaam(Beslist.class) + "(" + onderwerp + ", "
                        + str(x.stapel()) + ", " + x.index() + ")";
                case Gevolg.Stapelfeit.Uitkomst x -> "new " + typenaam(Uitkomst.class) + "(" + onderwerp + ", "
                        + str(x.stapel()) + ", " + x.keten() + ", " + x.positie() + ", " + enumlabel(x.soort()) + ", "
                        + (x.soort() == Uitkomst.Soort.BESLIST ? "v, null" : "null, (String) v") + ")";
                case Gevolg.Stapelfeit.Inconsistentie x -> route(Route.INCONSISTENTIE) + "((String) v, " + onderwerp + ")";
            };
        }

        /**
         * The object creation of {@link Gevolg.Creatie}: a function of the rule takes the steps of the plan in their order
         * and makes the facts, and the rule inserts them. The function computes the depth through its route, which throws
         * past the budget before anything is made; then the value of each initialisation, collapsed, the first evaluation
         * fault being its outcome, as in the executable model, which then makes nothing; then the new bean through the
         * route {@code nieuwObject}, with each attribute initialisation set on it through the route {@code zet} before it
         * is inserted; and it gives the facts of the insertions in the order of the plan, null for a characteristic whose
         * value is not true. The rule inserts the fault, or each fact as the plan inserts it at its place. The function
         * makes the facts since MVEL resolves no constructor with an {@code int} parameter from an argument of static
         * type {@code Object} (FABLE-3 §2.4, decision 33). The sentences of the initialisations stand above the function;
         * the plan carries none of the creation itself.
         */
        private void creatie(Gevolg.Creatie x, StringBuilder b) {
            int plaats = reserveer();
            String naam = naam("creatie", plaats);
            List<Term> waarden = new ArrayList<>();
            List<String> bronnen = new ArrayList<>();
            for (Gevolg.Initialisatie i : x.initialisaties()) {
                waarden.add(i.waarde());
                if (i.waarde().bron() != null) bronnen.add(i.waarde().bron());
            }
            Context c = context(x.slots(), -1, null, waarden.toArray(new Term[0]));
            // the subject is the variable of the rule's subject pattern, and a slot only where the plan reads it as one
            if (!c.heeft(x.onderwerp())) c.parameter("Object", x.onderwerp(), x.onderwerp());
            Gevolg.Creatiediepte d = x.diepte();
            StringBuilder f = new StringBuilder();
            f.append("    int diepte = ").append(route(Route.CREATIEDIEPTE)).append("(").append(str(x.creatie()))
                    .append(", ").append(d.stratum()).append(", ").append(str(d.type())).append(", (java.util.List) ")
                    .append(slot(d.herkomst(), c)).append(", ").append(d.budget()).append(");\n");
            if (!waarden.isEmpty()) {
                StringBuilder probeer = new StringBuilder();
                for (int i = 0; i < waarden.size(); i++) {
                    f.append("    Object i").append(i).append(" = null;\n");
                    probeer.append("        i").append(i).append(" = ")
                            .append(aanroep(Functie.COLLAPSE, List.of(waarde(waarden.get(i), c))).tekst()).append(";\n");
                }
                f.append(vang(probeer.toString(), "        return " + route(Route.GEVANGEN) + "(e);\n"));
            }
            f.append("    Object nieuw = ").append(route(Route.NIEUW_OBJECT)).append("(").append(x.onderwerp())
                    .append(", ").append(str(x.constructie().klasse().getName())).append(");\n");
            for (Gevolg.Constructie.Zetting z : x.constructie().zettingen())
                f.append("    ").append(route(Route.ZET)).append("(nieuw, ").append(str(z.veld().eigenschap()))
                        .append(", i").append(z.initialisatie()).append(");\n");
            f.append("    int fase = ((Integer) ").append(slot(x.fase(), c)).append(").intValue();\n");
            f.append("    java.util.List feiten = new java.util.ArrayList();\n");
            for (Gevolg.Invoegsel i : x.invoegingen())
                f.append("    feiten.add(").append(switch (i) {
                    case Gevolg.Invoegsel.Nieuw n -> "nieuw";
                    case Gevolg.Invoegsel.Merk m -> "new " + typenaam(Gemaakt.class) + "(" + str(x.creatie()) + ", "
                            + x.onderwerp() + ", nieuw, fase, " + d.stratum() + ", diepte)";
                    case Gevolg.Invoegsel.Link l -> "new " + typenaam(Koppeling.class) + "(" + str(l.relatie()) + ", "
                            + (l.nieuwIsEerste() ? "nieuw, " + x.onderwerp() : x.onderwerp() + ", nieuw") + ", fase)";
                    case Gevolg.Invoegsel.Kenmerk k -> "Boolean.TRUE.equals(i" + k.initialisatie() + ") ? new "
                            + typenaam(HeeftKenmerk.class) + "(nieuw, "
                            + sleutel(x.initialisaties().get(k.initialisatie()).lid()) + ") : null";
                }).append(");\n");
            f.append("    return feiten;\n");
            vul(plaats, bronnen.isEmpty() ? null : String.join("; ", bronnen),
                    "Object " + naam + "(" + c.declaratie() + ")", f.toString());
            b.append("    Object v = ").append(naam).append("(").append(c.argumenten()).append(");\n");
            b.append("    if (").append(route(Route.IS_EVALUATIEFOUT)).append("(v)) {\n");
            voegIn(b, "        ", x.foutinvoeging(), "new " + typenaam(Fout.class) + "(" + x.onderwerp() + ", "
                    + str(x.fout().vorm()) + ", " + route(Route.BENOEM) + "(v, " + str(x.fout().regel()) + "))");
            b.append("    } else {\n        java.util.List feiten = (java.util.List) v;\n");
            for (int i = 0; i < x.invoegingen().size(); i++) {
                Gevolg.Invoegsel s = x.invoegingen().get(i);
                if (s instanceof Gevolg.Invoegsel.Kenmerk) {
                    b.append("        if (feiten.get(").append(i).append(") != null) {\n");
                    voegIn(b, "            ", s.invoeging(), "feiten.get(" + i + ")");
                    b.append("        }\n");
                } else {
                    voegIn(b, "        ", s.invoeging(), "feiten.get(" + i + ")");
                }
            }
            b.append("    }\n");
        }

        /** The variable of slot {@code slot}, or {@code null} where it is -1, a value the plan does not read. */
        private String slotOfNull(int slot) {
            return slot < 0 ? "null" : variabele(slot);
        }

        /** The distribution of {@link Gevolg.Uitdeling}: the facts of the route, each inserted. The method stands as its
         *  enumeration label, which MVEL reads in the argument of a call (FABLE-3 §2.2), the decimals as an
         *  {@code Integer} or null, and the keys as {@code ElementSleutel.van}. */
        private void uitdeling(Gevolg.Uitdeling x, StringBuilder b) {
            b.append("    for (Object feit : ").append(route(Route.UITDELING)).append("(").append(str(x.vorm()))
                    .append(", ").append(x.onderwerp()).append(", ").append(enumlabel(x.methode())).append(", ")
                    .append(variabele(x.pot())).append(", ").append(variabele(x.lijst())).append(", ").append(x.gewogen())
                    .append(", ").append(x.gemaximeerd()).append(", ").append(x.afnemend()).append(", ")
                    .append(x.decimalen() == null ? "null" : "Integer.valueOf(" + x.decimalen() + ")").append(", ")
                    .append(sleutel(x.aandeel())).append(", ").append(x.rest() == null ? "null" : sleutel(x.rest()))
                    .append(", ").append(str(x.schrijver())).append(")) {\n");
            voegIn(b, "        ", x.invoeging(), "feit");
            b.append("    }\n");
        }

        /** The fault of {@link Gevolg.Filterfout}: the filter evaluated again over the same bound values, its
         *  evaluation fault inserted with the member's place, and an answer there a broken contract. */
        private void filterfout(Gevolg.Filterfout x, StringBuilder b) {
            int plaats = reserveer();
            String naam = naam("fout", plaats);
            Context c = context(x.slots(), -1, null, x.filter());
            String t = bool(x.filter(), c);
            vul(plaats, x.filter().bron(), "RuntimeException " + naam + "(" + c.declaratie() + ")",
                    vang("        boolean uitkomst = " + t + ";\n        return null;\n",
                            "        return " + route(Route.GEVANGEN) + "(e);\n"));
            b.append("    RuntimeException f = ").append(naam).append("(").append(c.argumenten()).append(");\n");
            b.append("    if (f == null) {\n        ").append(route(Route.CONTRACTBREUK)).append("(")
                    .append(str("het filter van " + x.vouw() + " faalde in de toets en niet in het gevolg, over"
                            + " dezelfde gebonden waarden")).append(");\n    }\n");
            voegIn(b, "    ", x.invoeging(), "new " + typenaam(Filterfout.class) + "(" + str(x.vouw()) + ", "
                    + x.onderwerp() + ", " + variabele(x.lid()) + ", f, "
                    + vouwplaats(x.plaats(), x.vouw(), x.lid(), null) + ")");
        }

        /**
         * The member's place {@code p} in the order of the fold {@code vouw}, through the route, as a function of this
         * rule over the member in slot {@code lid} and the count and the least place of each step, by the variables of
         * their slots; returns its call, which reads those variables where it stands: in the consequence, where the
         * left-hand side binds them, or in the function whose context is {@code c}, which must pass them.
         */
        private String vouwplaats(Gevolg.Vouwplaats p, String vouw, int lid, Context c) {
            int plaats = reserveer();
            String naam = naam("plaats", plaats);
            List<String> parameters = new ArrayList<>(), velden = new ArrayList<>(), aantallen = new ArrayList<>(),
                    plaatsen = new ArrayList<>();
            parameters.add(variabele(lid));
            for (Gevolg.Vouwplaats.Stap s : p.stappen()) {
                velden.add(str(s.veld()));
                aantallen.add(variabele(s.aantal()));
                plaatsen.add(variabele(s.plaats()));
                parameters.add(variabele(s.aantal()));
                parameters.add(variabele(s.plaats()));
            }
            if (c != null)
                for (String v : parameters)
                    if (!c.heeft(v))
                        throw new IllegalStateException("de plaats van een lid in " + plan.naam() + " leest " + v
                                + ", dat de functie van haar gevolg niet krijgt");
            List<String> declaratie = new ArrayList<>();
            for (String v : parameters) declaratie.add("Object " + v);
            vul(plaats, null, "java.util.List " + naam + "(" + String.join(", ", declaratie) + ")",
                    "    return " + route(Route.VOUWPLAATS) + "(" + enumlabel(p.reden()) + ", " + str(vouw) + ", "
                            + variabele(lid) + ", " + p.selectie() + ", java.util.Arrays.asList("
                            + String.join(", ", velden) + "), java.util.Arrays.asList(" + String.join(", ", aantallen)
                            + "), java.util.Arrays.asList(" + String.join(", ", plaatsen) + "));\n");
            return naam + "(" + String.join(", ", parameters) + ")";
        }

        // ---------------- the day kinds of a term ----------------

        /** Adds to {@code uit} the global of every day kind {@code t} names, in the order of the tree. */
        private void dagsoorten(Term t, Set<String> uit) {
            switch (t) {
                case Term.Dagsoort d -> uit.add(dagsoort(d));
                case Term.Invoer i -> dagsoorten(i.object(), uit);
                case Term.Let l -> {
                    dagsoorten(l.waarde(), uit);
                    dagsoorten(l.lichaam(), uit);
                }
                case Term.Keuze k -> {
                    dagsoorten(k.voorwaarde(), uit);
                    dagsoorten(k.dan(), uit);
                    dagsoorten(k.anders(), uit);
                }
                case Term.Aanroep a -> a.argumenten().forEach(x -> dagsoorten(x, uit));
                case Term.Herschaal h -> dagsoorten(h.term(), uit);
                case Term.Deel d -> {
                    dagsoorten(d.links(), uit);
                    dagsoorten(d.rechts(), uit);
                }
                case Term.Wortel w -> dagsoorten(w.radicand(), uit);
                case Term.Vereist v -> dagsoorten(v.waarde(), uit);
                case Term.Tekstreeks s -> s.delen().forEach(x -> dagsoorten(x, uit));
                case Term.GelijsteVouw g -> g.operanden().forEach(x -> dagsoorten(x, uit));
                case Term.Vouwlezing v -> dagsoorten(v.uitkomst(), uit);
                case Term.Samengesteld s -> s.leden().forEach(x -> dagsoorten(x, uit));
                case Term.Kwantorlezing q -> {
                    dagsoorten(q.aantal(), uit);
                    dagsoorten(q.bijdragen(), uit);
                }
                case Term.ZonderFout z -> dagsoorten(z.voorwaarde(), uit);
                case Term.Const k -> { }
                case Term.Lees l -> { }
                case Term.Binder x -> { }
                case Term.Faal f -> { }
            }
        }
    }
}
