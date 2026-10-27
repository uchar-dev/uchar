package dev.uchar.regelspraak.cases;

import dev.uchar.regelspraak.graph.ProducerConsumer.ElementKey;
import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;
import dev.uchar.regelspraak.schema.FactModelGenerator;
import dev.uchar.regelspraak.transpiler.tms.ConditionReading;
import dev.uchar.regelspraak.transpiler.tms.Fixpoint;
import dev.uchar.regelspraak.transpiler.tms.Period;
import dev.uchar.regelspraak.transpiler.tms.Fault;
import dev.uchar.regelspraak.transpiler.tms.CreationStamp;
import dev.uchar.regelspraak.transpiler.tms.InputFormFault;
import dev.uchar.regelspraak.transpiler.tms.MemberTest;
import dev.uchar.regelspraak.transpiler.tms.WritersConflict;

import org.kie.api.definition.KiePackage;
import org.kie.api.definition.rule.Global;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.AgendaEventListener;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.kie.api.runtime.KieSession;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;

/**
 * The canonical text of one session fired through {@link DroolsBackend#fire}: the guard of the exec-model branch while
 * card drl-teksttak-op-truth-maintenance rebuilds its lowering into one analysis with two renderings. The harness
 * {@code FireTrace} in the test tree writes it per case of every golden the branch plays, and the seam in
 * {@link CaseRunner} per session the suite fires through the replay, where the property {@value #PROPERTY} names a
 * directory. Two dumps of one code base are equal; a dump before a step and one after it are compared with
 * {@code diff -r}.
 *
 * <p>Every line starts with its section, so a diff counts its lines per section ({@code awk '{print $2}'} over the
 * changed lines): {@code input} (every object in working memory and every global right before the fire: the input of
 * the session, so that a comparison couples a session to its case and not only to its document),
 * {@code firings} (the firings, the cap and the sentence of a run without a fixpoint), {@code fixpoint} (the outcome
 * of the fire in one line: {@code yes}, {@code no} with the sentence of the cap or the budget, or {@code throws} with
 * the exception the fire threw), {@code trace-count} (the firings per rule), {@code trace-order} (the rules in the
 * order they fired), {@code read} (everything {@link DroolsBackend#fixpointOf} gives) and {@code facts}/{@code fact}
 * (every object in working memory at the fixpoint, the beans with their fields among them, so that a write on a bean
 * during the fire shows), and for a build with readings {@code reading} (what {@link DroolsBackend#readingsOf} gives, in
 * the canonical text of {@link #readingLines}), beside the facts of the reading rules among the facts.
 * The order and the count of the firings depend on the order in which a rendering adds the rules, so a step reports
 * them and explains a difference; the other sections hold strictly, the outcome of the fire among them (round 5b of
 * card drl-teksttak-op-truth-maintenance: before, it stood only in the reported line {@code firings}).
 *
 * <p>The text names every object by a name of its own and never by its identity, which differs between two runs: an
 * input bean by its case label, quoted, and a made object by its creation, the name of its subject and its number,
 * {@code «"creation"»("subject")#k} ({@link Name}). Every name and every text stands quoted and escaped as it is: a case
 * label, the name of a rule, a global, an attribute, a characteristic, a fact type and its roles, the type and the name
 * of a member's key, a text value and a sentence. So no line holds a real line break, and two different names never
 * print alike, also where a name holds a space, "=", ".", "(" or ")", which WF-057 admits (lens on round 4). Only a
 * Java identifier stands unquoted, the class of a value and a field of a bean or of a fact, since the compiler admits
 * none of those characters in one. Nothing in the text is replaced afterwards: a case label is free text and may look
 * like an identity, so a rewrite of the text would make two sessions alike that differ (lens on step 3b). A sentence of the
 * branch that names an object is formed again with these names where the fault carries its object ({@code named});
 * one that names an object of the session by its type and identity anyway is refused, never written. Every value
 * carries its Java class, a number its scale and a text its quotes ({@link Naming#render}), so two values that a
 * fact holds unequal never print alike; every list of lines is sorted.
 */
final class SessionDump {

    /** The system property that turns the seam of {@link CaseRunner} on: the directory it writes to. */
    static final String PROPERTY = "uchar.fireTrace";

    /** The directory of the seam, read once; null without the property, and then the seam does nothing. */
    static final String SEAM_DIRECTORY = System.getProperty(PROPERTY);

    /** The sessions the seam wrote, per document and branch: the number in the name of the next file. */
    private static final Map<String, Integer> SEQUENCE_NUMBERS = new HashMap<>();

    private final List<String> trace = new ArrayList<>();
    private final AgendaEventListener listener;
    private final KieSession session;
    /** The lines of the section {@code input}, written when the dump starts, before the fire changes anything. */
    private final List<String> input;

    private SessionDump(KieSession session, Map<String, Object> labels) {
        this.session = session;
        this.input = input(session, labels);
        this.listener = new DefaultAgendaEventListener() {
            @Override public void afterMatchFired(AfterMatchFiredEvent e) { trace.add(e.getMatch().getRule().getName()); }
        };
        session.addEventListener(listener);
    }

    /** Records the input of {@code session}, whose input beans {@code labels} names by their case labels, and its
     *  firings from now on, in order: call it right before the fire. */
    static SessionDump op(KieSession session, Map<String, Object> labels) {
        return new SessionDump(session, labels);
    }

    /** The input of a session right before its fire: every object in working memory, named and with its fields as
     *  {@link #facts} writes them, and every global of the knowledge base with its value. Two sessions of one document
     *  with an equal input are two firings of one case. A global that is a predicate, the compiled day kind of the
     *  document, is left out: it is the same for every case, only the DRL text holds it in a global, and it prints an
     *  identity of the JVM otherwise. */
    private static List<String> input(KieSession session, Map<String, Object> labels) {
        Naming n = new Naming(session, labels, List.of());
        List<String> rules = new ArrayList<>();
        for (Object o : session.getObjects()) rules.add(n.fact(o));
        TreeMap<String, String> globals = new TreeMap<>();
        for (KiePackage p : session.getKieBase().getKiePackages())
            for (Global g : p.getGlobalVariables()) {
                Object w = session.getGlobal(g.getName());
                // the predicate of a day kind is a constant of the document, which the DRL text holds in a global and
                // the executable model closes over; no input of the case, so the input of the two renderings compares
                if (w instanceof Predicate<?>) continue;
                globals.put(g.getName(), n.render(w));
            }
        globals.forEach((g, w) -> rules.add("global " + quote(g) + " = " + w));
        rules.sort(Comparator.naturalOrder());
        return rules;
    }

    /** The first sixteen hex digits of the SHA-256 of the file {@code document}, as {@code shasum -a 256} gives them:
     *  the name of the seam's files, so that a document one witness writes to a fresh temporary path keeps its name. */
    static String hash(Path document) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(document));
            return HexFormat.of().formatHex(d).substring(0, 16);
        } catch (IOException e) {
            throw new UncheckedIOException("de naad " + PROPERTY + " leest het document " + document + " niet", e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The seam: writes the dump of a session that {@code fire} fired to the next file of {@code hash} and the branch. */
    void seam(String hash, DroolsBackend.Build build, DroolsBackend.Firing firing, Map<String, Object> labels) {
        StringBuilder out = new StringBuilder();
        write(out, build, firing, labels);
        keep(hash, build, out);
    }

    /** The seam: writes the dump of a session whose {@code fire} threw {@code e} to the next file of {@code hash}. */
    void seam(String hash, DroolsBackend.Build build, RuntimeException e, Map<String, Object> labels) {
        StringBuilder out = new StringBuilder();
        write(out, e, labels);
        keep(hash, build, out);
    }

    private static void keep(String hash, DroolsBackend.Build build, StringBuilder out) {
        String key = hash + "." + build.branch().name().toLowerCase(java.util.Locale.ROOT);
        int n;
        synchronized (SEQUENCE_NUMBERS) {
            n = SEQUENCE_NUMBERS.merge(key, 1, Integer::sum);
        }
        Path map = Path.of(SEAM_DIRECTORY);
        try {
            Files.createDirectories(map);
            Files.writeString(map.resolve(key + "." + n + ".txt"), out.toString());
        } catch (IOException e) {
            throw new UncheckedIOException("de naad " + PROPERTY + " schrijft niet in " + map, e);
        }
    }

    /** Appends the dump of a session whose {@code fire} returned {@code v}: its input, the firings, at a fixpoint what
     *  {@link DroolsBackend#fixpointOf} gives, and every object in working memory. A session without a fixpoint gets its
     *  objects too, as they stand where the fire stopped: {@code read} refuses such a session, and without them a
     *  rebuild that changes what a capped session derives would pass the comparison unseen. {@code labels} maps each
     *  case label to its input bean. */
    void write(StringBuilder out, DroolsBackend.Build build, DroolsBackend.Firing v, Map<String, Object> labels) {
        session.removeEventListener(listener);
        Naming n = naming(labels);
        for (String r : input) out.append("input ").append(r).append('\n');
        out.append("firings ").append(v.firings()).append(" cap ").append(v.cap()).append(" noFixpoint ")
                .append(v.noFixpoint() == null ? "null" : n.sentence(v.noFixpoint())).append('\n');
        // the count of the firings differs between two renderings that reach the same fixpoint, so the comparison only
        // reports the line above; whether there is a fixpoint at all it holds strictly, in a line of its own
        out.append("fixpoint ").append(v.noFixpoint() == null ? "yes" : "no " + n.sentence(v.noFixpoint())).append('\n');
        trace(out);
        if (v.noFixpoint() != null) {
            facts(out, n);
            return;
        }
        Map<Object, String> label = new IdentityHashMap<>();
        labels.forEach((l, b) -> label.put(b, l));
        Fixpoint fp = DroolsBackend.fixpointOf(build, session, label::get);
        List<String> read = new ArrayList<>();
        for (Fixpoint.DerivedValue w : fp.values())
            read.add("value " + n.nameOf(w.object()) + "." + quote(w.attribute()) + " = " + n.render(w.value()));
        for (Fixpoint.DerivedCharacteristic k : fp.characteristics())
            read.add("characteristic " + n.nameOf(k.object()) + " " + quote(k.characteristic()));
        for (Fixpoint.MadeObject g : fp.objects())
            read.add("object " + n.nameOf(g.object()) + " by " + quote(g.creation()) + " for "
                    + n.nameOf(g.subject()));
        for (Fixpoint.MadeLink k : fp.links())
            read.add("link " + quote(k.factType()) + " " + quote(k.firstRole()) + "=" + n.nameOf(k.first())
                    + " " + quote(k.secondRole()) + "=" + n.nameOf(k.second()));
        for (Fault f : fp.faults())
            read.add("fault " + n.nameOf(f.subject()) + " " + quote(f.rule()) + ": " + n.sentence(f.fault()));
        for (Object i : fp.inconsistencies()) read.add("inconsistency " + n.render(i));
        rules(out, "read", read);
        if (build.readings()) rules(out, "reading", readingLines(build, session, labels));
        facts(out, n);
    }

    /** Appends the dump of a session whose {@code fire} threw {@code e}: its input, the exception, the firings before
     *  it, and every object in working memory as it stands where the fire stopped. */
    void write(StringBuilder out, RuntimeException e, Map<String, Object> labels) {
        session.removeEventListener(listener);
        Naming n = naming(labels);
        for (String r : input) out.append("input ").append(r).append('\n');
        out.append("firings throws ").append(n.fault(e)).append('\n');
        out.append("fixpoint throws ").append(n.fault(e)).append('\n');
        trace(out);
        facts(out, n);
    }

    /** Appends every object in working memory, sorted and named. */
    private void facts(StringBuilder out, Naming n) {
        List<String> facts = new ArrayList<>();
        for (Object o : session.getObjects()) facts.add(n.fact(o));
        out.append("facts ").append(facts.size()).append('\n');
        rules(out, "fact", facts);
    }

    /** Appends the firings per rule and in order, each rule by its name quoted. */
    private void trace(StringBuilder out) {
        TreeMap<String, Integer> count = new TreeMap<>();
        for (String r : trace) count.merge(r, 1, Integer::sum);
        List<String> counted = new ArrayList<>(), order = new ArrayList<>();
        count.forEach((r, n) -> counted.add(quote(r) + "=" + n));
        for (String r : trace) order.add(quote(r));
        out.append("trace-count {").append(String.join(", ", counted)).append("}\n");
        out.append("trace-order [").append(String.join(", ", order)).append("]\n");
    }

    private static void rules(StringBuilder out, String section, List<String> rules) {
        List<String> sorted = new ArrayList<>(rules);
        sorted.sort(Comparator.naturalOrder());
        for (String r : sorted) out.append(section).append(' ').append(r).append('\n');
    }

    /** The names of the session at its end: the input beans by their case labels, and every object a creation made by
     *  its mark. The marks are taken by phase, so the subject of a mark is named before the object made for it: a
     *  creation takes no subject of its own phase. Within a phase a creation makes one object per subject, so the number
     *  of a made object counts the phases in which its creation made one for that subject. */
    private Naming naming(Map<String, Object> labels) {
        return naming(session, labels);
    }

    private static Naming naming(KieSession session, Map<String, Object> labels) {
        List<CreationStamp> marks = new ArrayList<>();
        for (Object o : session.getObjects(o -> o instanceof CreationStamp)) marks.add((CreationStamp) o);
        marks.sort(Comparator.comparingInt(CreationStamp::phase));
        return new Naming(session, labels, marks);
    }

    /**
     * The name of an object of a session: an input bean by its case label {@code label}; a made object by the creation
     * {@code creatie} that made it, the name of its subject {@code subject} and its number {@code nummer} among the
     * objects that creation made for that subject.
     */
    private record Name(String label, String creation, Name subject, int number) {

        /** As the dump writes it: a case label quoted and escaped; a made object as {@code «"creatie"»(subject)#k},
         *  which no quoted label can be read as. */
        String inTheDump() {
            return label != null ? quote(label) : "«" + quote(creation) + "»(" + subject.inTheDump() + ")#" + number;
        }

        /** As a sentence of the branch names the object where it is given a label ({@code named}): the case label
         *  itself, as the replay names an input object, or {@code «creatie»(subject)#k}. */
        String inASentence() {
            return label != null ? label : "«" + creation + "»(" + subject.inASentence() + ")#" + number;
        }
    }

    /** The names of one moment of a session and the rendering of its objects, values and sentences with them. */
    private static final class Naming {
        private final KieSession session;
        private final Map<Object, Name> name = new IdentityHashMap<>();
        /** What a sentence of the branch prints for each object the dump knows ({@code FactNames.object}): its type and
         *  identity; made at the first sentence. */
        private Map<String, Object> identities;

        /** The input beans of {@code labels}, and the objects of {@code marks}, ordered by phase. */
        Naming(KieSession session, Map<String, Object> labels, List<CreationStamp> marks) {
            this.session = session;
            labels.forEach((l, b) -> name.put(b, new Name(l, null, null, 0)));
            Map<String, Integer> counter = new HashMap<>();
            int i = 0;
            while (i < marks.size()) {
                int j = i;
                while (j < marks.size() && marks.get(j).phase() == marks.get(i).phase()) j++;
                List<CreationStamp> phase = new ArrayList<>(marks.subList(i, j));
                phase.sort(Comparator.comparing((CreationStamp g) -> key(g)));
                for (CreationStamp g : phase)
                    name.putIfAbsent(g.object(), new Name(null, g.rule(), subject(g),
                            counter.merge(key(g), 1, Integer::sum)));
                i = j;
            }
        }

        /** The name of the subject of mark {@code g}, which an earlier phase or the input gave. */
        private Name subject(CreationStamp g) {
            Name n = name.get(g.subject());
            if (n == null) throw new IllegalStateException("de dump heeft geen naam voor het onderwerp van de creatie "
                    + quote(g.rule()) + " in fase " + g.phase() + ", een " + g.subject().getClass().getSimpleName());
            return n;
        }

        private String key(CreationStamp g) {
            return "«" + quote(g.rule()) + "»(" + subject(g).inTheDump() + ")";
        }

        /** {@code o} where an object stands in a line: a bean by its name, null as {@code leeg}, any other object by
         *  {@link #render}. */
        String nameOf(Object o) {
            return o == null ? "empty" : render(o);
        }

        /** One object in working memory: a bean with its name and its fields, and every other fact as {@link #render}. */
        String fact(Object o) {
            if (!isBean(o) || o.getClass().getSimpleName().equals("Inconsistentie")) return render(o);
            List<String> fields = new ArrayList<>();
            for (Class<?> k = o.getClass(); k != null && k.getName().startsWith(FactModelGenerator.PACKAGE + ".");
                 k = k.getSuperclass())
                for (Field f : k.getDeclaredFields()) {
                    if (Modifier.isStatic(f.getModifiers())) continue;
                    f.setAccessible(true);
                    try {
                        fields.add(f.getName() + "=" + render(f.get(o)));
                    } catch (IllegalAccessException e) {
                        throw new IllegalStateException("het veld " + f + " van een bean is niet te lezen", e);
                    }
                }
            fields.sort(Comparator.naturalOrder());
            return "bean " + render(o) + " " + o.getClass().getSimpleName() + "(" + String.join(", ", fields) + ")";
        }

        /** An exception as its class and its sentence ({@link #sentence(Throwable)}). */
        String fault(Throwable e) {
            return e.getClass().getSimpleName() + ": " + sentence(e);
        }

        /** The sentence of {@code e}, quoted: formed again with the names of the dump where the fault carries its
         *  object, the fault of two writers and the fault of the form of an input; any other sentence as it is. */
        String sentence(Throwable e) {
            Throwable named = e instanceof WritersConflict c ? c.named(this::inASentence)
                    : e instanceof InputFormFault i ? i.named(this::inASentence) : e;
            return sentence(named.getMessage());
        }

        /** A sentence of the branch, quoted and escaped; refused where it names an object of the session by its type
         *  and identity, since such a name differs between two runs and a dump neither writes nor replaces one. */
        String sentence(String sentence) {
            if (sentence == null) return "empty";
            if (identities == null) {
                identities = new HashMap<>();
                for (Object o : session.getObjects()) identities.put(identity(o), o);
                for (Object o : name.keySet()) identities.put(identity(o), o);
            }
            for (Map.Entry<String, Object> i : identities.entrySet())
                if (sentence.contains(i.getKey()))
                    throw new IllegalStateException("de dump schrijft geen identiteit: de zin " + quote(sentence) + " noemt "
                            + i.getKey() + ", " + (name.containsKey(i.getValue()) ? "het object "
                            + name.get(i.getValue()).inTheDump() : "een object zonder naam") + " van de sessie");
            return quote(sentence);
        }

        private static String identity(Object o) {
            return o.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(o));
        }

        /** The name of {@code o} as a sentence of the branch gives it, null where the dump has none. */
        private String inASentence(Object o) {
            Name n = name.get(o);
            return n == null ? null : n.inASentence();
        }

        /**
         * A value or a fact, without identities: a bean by its name, a record and every other class of the branch by
         * its components or fields, a collection in its order where it has one and sorted where not, and every other
         * value with its Java class, so that two values a fact holds unequal never print alike, as {@code BigDecimal} 2
         * and the text "2" did: a number of {@code BigDecimal} as {@link BigDecimal#toString}, one text per value and
         * scale (also where a fact compares such numbers by {@code compareTo}, which can only show a difference more,
         * never hide one); a text quoted and escaped; an enumeration label with its class; the key of a member with its
         * type and its name quoted; any other value with the text its class gives of it, quoted unless it is one of the
         * fixed forms of {@code Boolean}, {@code Integer}, {@code Long}, {@code LocalDate} and {@code LocalDateTime}. A
         * bean without a name, and an object whose class prints its identity, are refused.
         */
        String render(Object o) {
            if (o == null) return "empty";
            if (isBean(o)) {
                Name n = name.get(o);
                if (n != null) return n.inTheDump();
                String type = o.getClass().getSimpleName();
                if (type.equals("Inconsistentie"))
                    return "Inconsistentie(" + render(RegelSpraakRuntime.get(o, "rule")) + ", "
                            + render(RegelSpraakRuntime.get(o, "subject")) + ")";
                if (type.equals("Segment"))
                    return "Segment(" + RegelSpraakRuntime.get(o, "van") + ", " + RegelSpraakRuntime.get(o, "tot") + ", "
                            + render(RegelSpraakRuntime.get(o, "waarde")) + ")";
                throw new IllegalStateException("de dump heeft geen naam voor een bean van " + type
                        + ": geen caselabel en geen creatie die haar maakte");
            }
            if (o instanceof BigDecimal d) return "BigDecimal(" + d + ")";
            if (o instanceof String s) return "String(" + quote(s) + ")";
            if (o instanceof Enum<?> e) return e.getDeclaringClass().getSimpleName() + "." + e.name();
            if (o instanceof ElementKey k)
                return quote(k.objectType()) + "." + quote(k.name()) + (k.characteristic() ? " (characteristic)" : "");
            if (o instanceof Throwable e) return fault(e);
            if (o instanceof List<?> l) {
                // a list of member tests is what an accumulate collected over the contributions of a quantifier fold, in
                // the order of the agenda, which differs between the two branches; the rule that reads it sorts it by the
                // members' places (ComputeHelpers.quantifierRead), and so does the dump
                List<?> in = !l.isEmpty() && l.stream().allMatch(x -> x instanceof MemberTest)
                        ? l.stream().map(x -> (MemberTest) x).sorted().toList() : l;
                List<String> r = new ArrayList<>();
                for (Object x : in) r.add(render(x));
                return "[" + String.join(", ", r) + "]";
            }
            if (o instanceof Collection<?> c) {
                List<String> r = new ArrayList<>();
                for (Object x : c) r.add(render(x));
                r.sort(Comparator.naturalOrder());
                return "{" + String.join(", ", r) + "}";
            }
            if (o instanceof Map<?, ?> m) {
                List<String> r = new ArrayList<>();
                m.forEach((k, w) -> r.add(render(k) + "=" + render(w)));
                r.sort(Comparator.naturalOrder());
                return "{" + String.join(", ", r) + "}";
            }
            Class<?> javaClass = o.getClass();
            if (javaClass.isRecord()) {
                List<String> r = new ArrayList<>();
                for (RecordComponent rc : javaClass.getRecordComponents()) {
                    try {
                        rc.getAccessor().setAccessible(true);
                        r.add(rc.getName() + "=" + render(rc.getAccessor().invoke(o)));
                    } catch (ReflectiveOperationException e) {
                        throw new IllegalStateException("de component " + rc + " is niet te lezen", e);
                    }
                }
                return javaClass.getSimpleName() + "(" + String.join(", ", r) + ")";
            }
            if (javaClass.getName().startsWith("dev.uchar.regelspraak.transpiler.")) {
                // a fact class of the branch that is no record prints its objects with their identities: read its fields
                List<String> r = new ArrayList<>();
                for (Field f : javaClass.getDeclaredFields()) {
                    if (Modifier.isStatic(f.getModifiers())) continue;
                    f.setAccessible(true);
                    try {
                        r.add(f.getName() + "=" + render(f.get(o)));
                    } catch (IllegalAccessException e) {
                        throw new IllegalStateException("het veld " + f + " is niet te lezen", e);
                    }
                }
                return javaClass.getSimpleName() + "(" + String.join(", ", r) + ")";
            }
            try {
                if (javaClass.getMethod("toString").getDeclaringClass() == Object.class)
                    throw new IllegalStateException("de dump schrijft geen identiteit: een " + javaClass.getName()
                            + " heeft geen tekst van zichzelf");
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
            // the text of these classes has a fixed form without a space, a quote, a parenthesis or a line break; the
            // text any other class gives of itself has a form unknown here, so it stands quoted
            if (o instanceof Boolean || o instanceof Integer || o instanceof Long || o instanceof java.time.LocalDate
                    || o instanceof java.time.LocalDateTime)
                return javaClass.getSimpleName() + "(" + o + ")";
            return javaClass.getSimpleName() + "(" + quote(o.toString()) + ")";
        }
    }

    /**
     * The readings of the conditions in {@code session} of a build with readings ({@link DroolsBackend#readingsOf}) in
     * the canonical text of card fixpoint-readings, sorted, which {@code DroolsBackendMain --readings} prints per case
     * and a dump of such a session holds in its section {@code reading}: per record one line with the form, the
     * object and the status, and per node one line with the form, the object, the number and the kind of the node and
     * what the evaluation found there. An object is named as in the dump, an input bean by its case label quoted and a
     * made object as {@code «"creation"»(subject)#k}; a value without its Java class, as a case writes it: a number as
     * its decimal without trailing zeros, a date and a date-time in the ISO form of the cases, a text quoted, a truth
     * value as {@code true} or {@code false}, leeg as {@code empty}, a timeline as its periods {@code [from to value]};
     * a unit by its rendering; a fault by the EF code of its kind (RUNTIME-SEMANTICS ch. 5.7b), {@code -} where it has
     * none, and never by its sentence, which differs between executors; after the value of a node that reads a member
     * over a path ({@link dev.uchar.regelspraak.transpiler.tms.TmsTranspiler#navigates}) and did not fault, {@code on}
     * and the object the path reached, named as above, or {@code on none} where a link on it is absent; the
     * contributors of a fold sorted by member, each with its value or the code of its fault and whether it was reached
     * ({@code unordered} where its fold keeps no order). {@code labels} maps each case label to its input bean.
     */
    static List<String> readingLines(DroolsBackend.Build build, KieSession session, Map<String, Object> labels) {
        Naming n = naming(session, labels);
        List<String> lines = new ArrayList<>();
        for (ConditionReading r : DroolsBackend.readingsOf(build, session)) {
            String head = r.form() + " " + n.nameOf(r.object());
            lines.add(head + " " + r.status());
            for (ConditionReading.NodeReading x : r.nodes()) {
                StringBuilder b = new StringBuilder(head).append(' ').append(x.node()).append(' ').append(x.kind());
                if (!x.reached()) {
                    lines.add(b.append(" unreached").toString());
                    continue;
                }
                if (x.fault() != null) b.append(" fault ").append(code(x.fault()));
                else b.append(' ').append(canonical(x.value()));
                if (x.unit() != null) b.append(' ').append(quote(x.unit().rendering()));
                // the object a read over a path reached; the subject's own member stands at the head of the line
                if (x.fault() == null && build.execModelTranspiler().navigates(r.form(), x.node()))
                    b.append(" on ").append(x.object() == null ? "none" : n.nameOf(x.object()));
                if (!x.contributors().isEmpty()) {
                    List<String> cs = new ArrayList<>();
                    for (ConditionReading.Contributor c : x.contributors())
                        cs.add(n.nameOf(c.member()) + " " + (c.fault() != null ? "fault " + code(c.fault())
                                : canonical(c.value())) + " " + (c.reached() == null ? "unordered"
                                : c.reached() ? "reached" : "unreached"));
                    cs.sort(Comparator.naturalOrder());
                    b.append(" contributors [").append(String.join("; ", cs)).append(']');
                }
                lines.add(b.toString());
            }
        }
        lines.sort(Comparator.naturalOrder());
        return lines;
    }

    /** The EF code of the kind of the evaluation fault {@code f}, or {@code -} where it names none. */
    private static String code(RuntimeException f) {
        List<CaseRunner.FaultKind> kinds = CaseRunner.faultKinds(f);
        return kinds.isEmpty() ? "-" : kinds.get(0).code();
    }

    private static final java.time.format.DateTimeFormatter DATE_TIME =
            java.time.format.DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss");

    /** {@code v} as the canonical text writes a value ({@link #readingLines}); a value of any other kind is refused. */
    private static String canonical(Object v) {
        if (v == null) return "empty";
        if (v instanceof Boolean b) return b.toString();
        if (v instanceof BigDecimal d) return d.signum() == 0 ? "0" : d.stripTrailingZeros().toPlainString();
        if (v instanceof java.time.LocalDate d) return d.toString();
        if (v instanceof java.time.LocalDateTime d)
            return DATE_TIME.format(d) + (d.getNano() == 0 ? "" : String.format(".%03d", d.getNano() / 1_000_000));
        if (v instanceof String s) return quote(s);
        if (v instanceof Period p) return "[" + p.from() + " " + p.to() + " " + canonical(p.value()) + "]";
        if (v instanceof List<?> l) {
            List<String> r = new ArrayList<>();
            for (Object x : l) r.add(canonical(x));
            return "[" + String.join(", ", r) + "]";
        }
        if (v instanceof Map<?, ?> m) {
            List<String> r = new ArrayList<>();
            m.forEach((k, w) -> r.add(canonical(k) + "=" + canonical(w)));
            r.sort(Comparator.naturalOrder());
            return "{" + String.join(", ", r) + "}";
        }
        throw new IllegalStateException("de canonieke tekst van een lezing kent geen waarde van " + v.getClass().getName());
    }

    private static boolean isBean(Object o) {
        return o.getClass().getName().startsWith(FactModelGenerator.PACKAGE + ".");
    }

    /** {@code s} between double quotes, with a quote, a backslash and a line break escaped, so that a text stays on its
     *  line and tells where it ends. */
    private static String quote(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }
}
