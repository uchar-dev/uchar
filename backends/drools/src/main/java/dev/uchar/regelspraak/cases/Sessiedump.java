package dev.uchar.regelspraak.cases;

import dev.uchar.regelspraak.graaf.ProducentConsument.ElementSleutel;
import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;
import dev.uchar.regelspraak.schema.FactModelGenerator;
import dev.uchar.regelspraak.transpiler.tms.Fixpunt;
import dev.uchar.regelspraak.transpiler.tms.Fout;
import dev.uchar.regelspraak.transpiler.tms.Gemaakt;
import dev.uchar.regelspraak.transpiler.tms.Invoervormfout;
import dev.uchar.regelspraak.transpiler.tms.Schrijversconflict;

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
 * The canonical text of one session fired through {@link DroolsBackend#vuur}: the guard of the exec-model branch while
 * card drl-teksttak-op-truth-maintenance rebuilds its lowering into one analysis with two renderings. The harness
 * {@code Vuurspoor} in the test tree writes it per case of every golden the branch plays, and the seam in
 * {@link CaseRunner} per session the suite fires through the replay, where the property {@value #EIGENSCHAP} names a
 * directory. Two dumps of one code base are equal; a dump before a step and one after it are compared with
 * {@code diff -r}.
 *
 * <p>Every line starts with its section, so a diff counts its lines per section ({@code awk '{print $2}'} over the
 * changed lines): {@code invoer} (every object in working memory and every global right before the fire: the input of
 * the session, so that a comparison couples a session to its case and not only to its document),
 * {@code vuringen} (the firings, the cap and the sentence of a run without a fixpoint), {@code fixpunt} (the outcome
 * of the fire in one line: {@code ja}, {@code nee} with the sentence of the cap or the budget, or {@code gooit} with
 * the exception the fire threw), {@code spoor-telling} (the firings per rule), {@code spoor-volgorde} (the rules in the
 * order they fired), {@code lees} (everything {@link DroolsBackend#lees} gives) and {@code feiten}/{@code feit}
 * (every object in working memory at the fixpoint, the beans with their fields among them, so that a write on a bean
 * during the fire shows).
 * The order and the count of the firings depend on the order in which a rendering adds the rules, so a step reports
 * them and explains a difference; the other sections hold strictly, the outcome of the fire among them (round 5b of
 * card drl-teksttak-op-truth-maintenance: before, it stood only in the reported line {@code vuringen}).
 *
 * <p>The text names every object by a name of its own and never by its identity, which differs between two runs: an
 * input bean by its case label, quoted, and a made object by its creation, the name of its subject and its number,
 * {@code «"creatie"»("onderwerp")#k} ({@link Naam}). Every name and every text stands quoted and escaped as it is: a case
 * label, the name of a rule, a global, an attribute, a characteristic, a fact type and its roles, the type and the name
 * of a member's key, a text value and a sentence. So no line holds a real line break, and two different names never
 * print alike, also where a name holds a space, "=", ".", "(" or ")", which WF-057 admits (lens on round 4). Only a
 * Java identifier stands unquoted, the class of a value and a field of a bean or of a fact, since the compiler admits
 * none of those characters in one. Nothing in the text is replaced afterwards: a case label is free text and may look
 * like an identity, so a rewrite of the text would make two sessions alike that differ (lens on step 3b). A sentence of the
 * branch that names an object is formed again with these names where the fault carries its object ({@code benoemd});
 * one that names an object of the session by its type and identity anyway is refused, never written. Every value
 * carries its Java class, a number its scale and a text its quotes ({@link Naamgeving#render}), so two values that a
 * fact holds unequal never print alike; every list of lines is sorted.
 */
final class Sessiedump {

    /** The system property that turns the seam of {@link CaseRunner} on: the directory it writes to. */
    static final String EIGENSCHAP = "uchar.vuurspoor";

    /** The directory of the seam, read once; null without the property, and then the seam does nothing. */
    static final String NAADMAP = System.getProperty(EIGENSCHAP);

    /** The sessions the seam wrote, per document and branch: the number in the name of the next file. */
    private static final Map<String, Integer> VOLGNUMMERS = new HashMap<>();

    private final List<String> spoor = new ArrayList<>();
    private final AgendaEventListener luisteraar;
    private final KieSession sessie;
    /** The lines of the section {@code invoer}, written when the dump starts, before the fire changes anything. */
    private final List<String> invoer;

    private Sessiedump(KieSession sessie, Map<String, Object> labels) {
        this.sessie = sessie;
        this.invoer = invoer(sessie, labels);
        this.luisteraar = new DefaultAgendaEventListener() {
            @Override public void afterMatchFired(AfterMatchFiredEvent e) { spoor.add(e.getMatch().getRule().getName()); }
        };
        sessie.addEventListener(luisteraar);
    }

    /** Records the input of {@code sessie}, whose input beans {@code labels} names by their case labels, and its
     *  firings from now on, in order: call it right before the fire. */
    static Sessiedump op(KieSession sessie, Map<String, Object> labels) {
        return new Sessiedump(sessie, labels);
    }

    /** The input of a session right before its fire: every object in working memory, named and with its fields as
     *  {@link #feiten} writes them, and every global of the knowledge base with its value. Two sessions of one document
     *  with an equal input are two firings of one case. A global that is a predicate, the compiled day kind of the
     *  document, is left out: it is the same for every case, only the DRL text holds it in a global, and it prints an
     *  identity of the JVM otherwise. */
    private static List<String> invoer(KieSession sessie, Map<String, Object> labels) {
        Naamgeving n = new Naamgeving(sessie, labels, List.of());
        List<String> regels = new ArrayList<>();
        for (Object o : sessie.getObjects()) regels.add(n.feit(o));
        TreeMap<String, String> globals = new TreeMap<>();
        for (KiePackage p : sessie.getKieBase().getKiePackages())
            for (Global g : p.getGlobalVariables()) {
                Object w = sessie.getGlobal(g.getName());
                // the predicate of a day kind is a constant of the document, which the DRL text holds in a global and
                // the executable model closes over; no input of the case, so the input of the two renderings compares
                if (w instanceof Predicate<?>) continue;
                globals.put(g.getName(), n.render(w));
            }
        globals.forEach((g, w) -> regels.add("global " + citeer(g) + " = " + w));
        regels.sort(Comparator.naturalOrder());
        return regels;
    }

    /** The first sixteen hex digits of the SHA-256 of the file {@code document}, as {@code shasum -a 256} gives them:
     *  the name of the seam's files, so that a document one witness writes to a fresh temporary path keeps its name. */
    static String hash(Path document) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(document));
            return HexFormat.of().formatHex(d).substring(0, 16);
        } catch (IOException e) {
            throw new UncheckedIOException("de naad " + EIGENSCHAP + " leest het document " + document + " niet", e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The seam: writes the dump of a session that {@code vuur} fired to the next file of {@code hash} and the branch. */
    void naad(String hash, DroolsBackend.Build bouw, DroolsBackend.Vuring vuring, Map<String, Object> labels) {
        StringBuilder uit = new StringBuilder();
        schrijf(uit, bouw, vuring, labels);
        bewaar(hash, bouw, uit);
    }

    /** The seam: writes the dump of a session whose {@code vuur} threw {@code e} to the next file of {@code hash}. */
    void naad(String hash, DroolsBackend.Build bouw, RuntimeException e, Map<String, Object> labels) {
        StringBuilder uit = new StringBuilder();
        schrijf(uit, e, labels);
        bewaar(hash, bouw, uit);
    }

    private static void bewaar(String hash, DroolsBackend.Build bouw, StringBuilder uit) {
        String sleutel = hash + "." + bouw.branch().name().toLowerCase(java.util.Locale.ROOT);
        int n;
        synchronized (VOLGNUMMERS) {
            n = VOLGNUMMERS.merge(sleutel, 1, Integer::sum);
        }
        Path map = Path.of(NAADMAP);
        try {
            Files.createDirectories(map);
            Files.writeString(map.resolve(sleutel + "." + n + ".txt"), uit.toString());
        } catch (IOException e) {
            throw new UncheckedIOException("de naad " + EIGENSCHAP + " schrijft niet in " + map, e);
        }
    }

    /** Appends the dump of a session whose {@code vuur} returned {@code v}: its input, the firings, at a fixpoint what
     *  {@link DroolsBackend#lees} gives, and every object in working memory. A session without a fixpoint gets its
     *  objects too, as they stand where the fire stopped: {@code lees} refuses such a session, and without them a
     *  rebuild that changes what a capped session derives would pass the comparison unseen. {@code labels} maps each
     *  case label to its input bean. */
    void schrijf(StringBuilder uit, DroolsBackend.Build bouw, DroolsBackend.Vuring v, Map<String, Object> labels) {
        sessie.removeEventListener(luisteraar);
        Naamgeving n = naamgeving(labels);
        for (String r : invoer) uit.append("invoer ").append(r).append('\n');
        uit.append("vuringen ").append(v.vuringen()).append(" plafond ").append(v.plafond()).append(" geenFixpunt ")
                .append(v.geenFixpunt() == null ? "null" : n.zin(v.geenFixpunt())).append('\n');
        // the count of the firings differs between two renderings that reach the same fixpoint, so the comparison only
        // reports the line above; whether there is a fixpoint at all it holds strictly, in a line of its own
        uit.append("fixpunt ").append(v.geenFixpunt() == null ? "ja" : "nee " + n.zin(v.geenFixpunt())).append('\n');
        spoor(uit);
        if (v.geenFixpunt() != null) {
            feiten(uit, n);
            return;
        }
        Map<Object, String> label = new IdentityHashMap<>();
        labels.forEach((l, b) -> label.put(b, l));
        Fixpunt fp = DroolsBackend.lees(bouw, sessie, label::get);
        List<String> lees = new ArrayList<>();
        for (Fixpunt.AfgeleideWaarde w : fp.waarden())
            lees.add("waarde " + n.naamVan(w.object()) + "." + citeer(w.attribuut()) + " = " + n.render(w.waarde()));
        for (Fixpunt.AfgeleidKenmerk k : fp.kenmerken())
            lees.add("kenmerk " + n.naamVan(k.object()) + " " + citeer(k.kenmerk()));
        for (Fixpunt.GemaaktObject g : fp.objecten())
            lees.add("object " + n.naamVan(g.object()) + " door " + citeer(g.creatie()) + " voor "
                    + n.naamVan(g.onderwerp()));
        for (Fixpunt.GemaakteKoppeling k : fp.koppelingen())
            lees.add("koppeling " + citeer(k.feittype()) + " " + citeer(k.eersteRol()) + "=" + n.naamVan(k.eerste())
                    + " " + citeer(k.tweedeRol()) + "=" + n.naamVan(k.tweede()));
        for (Fout f : fp.fouten())
            lees.add("fout " + n.naamVan(f.onderwerp()) + " " + citeer(f.regel()) + ": " + n.zin(f.fout()));
        for (Object i : fp.inconsistenties()) lees.add("inconsistentie " + n.render(i));
        regels(uit, "lees", lees);
        feiten(uit, n);
    }

    /** Appends the dump of a session whose {@code vuur} threw {@code e}: its input, the exception, the firings before
     *  it, and every object in working memory as it stands where the fire stopped. */
    void schrijf(StringBuilder uit, RuntimeException e, Map<String, Object> labels) {
        sessie.removeEventListener(luisteraar);
        Naamgeving n = naamgeving(labels);
        for (String r : invoer) uit.append("invoer ").append(r).append('\n');
        uit.append("vuringen gooit ").append(n.fout(e)).append('\n');
        uit.append("fixpunt gooit ").append(n.fout(e)).append('\n');
        spoor(uit);
        feiten(uit, n);
    }

    /** Appends every object in working memory, sorted and named. */
    private void feiten(StringBuilder uit, Naamgeving n) {
        List<String> feiten = new ArrayList<>();
        for (Object o : sessie.getObjects()) feiten.add(n.feit(o));
        uit.append("feiten ").append(feiten.size()).append('\n');
        regels(uit, "feit", feiten);
    }

    /** Appends the firings per rule and in order, each rule by its name quoted. */
    private void spoor(StringBuilder uit) {
        TreeMap<String, Integer> telling = new TreeMap<>();
        for (String r : spoor) telling.merge(r, 1, Integer::sum);
        List<String> geteld = new ArrayList<>(), volgorde = new ArrayList<>();
        telling.forEach((r, n) -> geteld.add(citeer(r) + "=" + n));
        for (String r : spoor) volgorde.add(citeer(r));
        uit.append("spoor-telling {").append(String.join(", ", geteld)).append("}\n");
        uit.append("spoor-volgorde [").append(String.join(", ", volgorde)).append("]\n");
    }

    private static void regels(StringBuilder uit, String sectie, List<String> regels) {
        List<String> gesorteerd = new ArrayList<>(regels);
        gesorteerd.sort(Comparator.naturalOrder());
        for (String r : gesorteerd) uit.append(sectie).append(' ').append(r).append('\n');
    }

    /** The names of the session at its end: the input beans by their case labels, and every object a creation made by
     *  its mark. The marks are taken by phase, so the subject of a mark is named before the object made for it: a
     *  creation takes no subject of its own phase. Within a phase a creation makes one object per subject, so the number
     *  of a made object counts the phases in which its creation made one for that subject. */
    private Naamgeving naamgeving(Map<String, Object> labels) {
        List<Gemaakt> merken = new ArrayList<>();
        for (Object o : sessie.getObjects(o -> o instanceof Gemaakt)) merken.add((Gemaakt) o);
        merken.sort(Comparator.comparingInt(Gemaakt::fase));
        return new Naamgeving(sessie, labels, merken);
    }

    /**
     * The name of an object of a session: an input bean by its case label {@code label}; a made object by the creation
     * {@code creatie} that made it, the name of its subject {@code onderwerp} and its number {@code nummer} among the
     * objects that creation made for that subject.
     */
    private record Naam(String label, String creatie, Naam onderwerp, int nummer) {

        /** As the dump writes it: a case label quoted and escaped; a made object as {@code «"creatie"»(subject)#k},
         *  which no quoted label can be read as. */
        String inDeDump() {
            return label != null ? citeer(label) : "«" + citeer(creatie) + "»(" + onderwerp.inDeDump() + ")#" + nummer;
        }

        /** As a sentence of the branch names the object where it is given a label ({@code benoemd}): the case label
         *  itself, as the replay names an input object, or {@code «creatie»(subject)#k}. */
        String inEenZin() {
            return label != null ? label : "«" + creatie + "»(" + onderwerp.inEenZin() + ")#" + nummer;
        }
    }

    /** The names of one moment of a session and the rendering of its objects, values and sentences with them. */
    private static final class Naamgeving {
        private final KieSession sessie;
        private final Map<Object, Naam> naam = new IdentityHashMap<>();
        /** What a sentence of the branch prints for each object the dump knows ({@code Feitnamen.object}): its type and
         *  identity; made at the first sentence. */
        private Map<String, Object> identiteiten;

        /** The input beans of {@code labels}, and the objects of {@code merken}, ordered by phase. */
        Naamgeving(KieSession sessie, Map<String, Object> labels, List<Gemaakt> merken) {
            this.sessie = sessie;
            labels.forEach((l, b) -> naam.put(b, new Naam(l, null, null, 0)));
            Map<String, Integer> teller = new HashMap<>();
            int i = 0;
            while (i < merken.size()) {
                int j = i;
                while (j < merken.size() && merken.get(j).fase() == merken.get(i).fase()) j++;
                List<Gemaakt> fase = new ArrayList<>(merken.subList(i, j));
                fase.sort(Comparator.comparing((Gemaakt g) -> sleutel(g)));
                for (Gemaakt g : fase)
                    naam.putIfAbsent(g.object(), new Naam(null, g.regel(), onderwerp(g),
                            teller.merge(sleutel(g), 1, Integer::sum)));
                i = j;
            }
        }

        /** The name of the subject of mark {@code g}, which an earlier phase or the input gave. */
        private Naam onderwerp(Gemaakt g) {
            Naam n = naam.get(g.onderwerp());
            if (n == null) throw new IllegalStateException("de dump heeft geen naam voor het onderwerp van de creatie "
                    + citeer(g.regel()) + " in fase " + g.fase() + ", een " + g.onderwerp().getClass().getSimpleName());
            return n;
        }

        private String sleutel(Gemaakt g) {
            return "«" + citeer(g.regel()) + "»(" + onderwerp(g).inDeDump() + ")";
        }

        /** {@code o} where an object stands in a line: a bean by its name, null as {@code leeg}, any other object by
         *  {@link #render}. */
        String naamVan(Object o) {
            return o == null ? "leeg" : render(o);
        }

        /** One object in working memory: a bean with its name and its fields, and every other fact as {@link #render}. */
        String feit(Object o) {
            if (!isBean(o) || o.getClass().getSimpleName().equals("Inconsistentie")) return render(o);
            List<String> velden = new ArrayList<>();
            for (Class<?> k = o.getClass(); k != null && k.getName().startsWith(FactModelGenerator.PAKKET + ".");
                 k = k.getSuperclass())
                for (Field f : k.getDeclaredFields()) {
                    if (Modifier.isStatic(f.getModifiers())) continue;
                    f.setAccessible(true);
                    try {
                        velden.add(f.getName() + "=" + render(f.get(o)));
                    } catch (IllegalAccessException e) {
                        throw new IllegalStateException("het veld " + f + " van een bean is niet te lezen", e);
                    }
                }
            velden.sort(Comparator.naturalOrder());
            return "bean " + render(o) + " " + o.getClass().getSimpleName() + "(" + String.join(", ", velden) + ")";
        }

        /** An exception as its class and its sentence ({@link #zin(Throwable)}). */
        String fout(Throwable e) {
            return e.getClass().getSimpleName() + ": " + zin(e);
        }

        /** The sentence of {@code e}, quoted: formed again with the names of the dump where the fault carries its
         *  object, the fault of two writers and the fault of the form of an input; any other sentence as it is. */
        String zin(Throwable e) {
            Throwable benoemd = e instanceof Schrijversconflict c ? c.benoemd(this::inEenZin)
                    : e instanceof Invoervormfout i ? i.benoemd(this::inEenZin) : e;
            return zin(benoemd.getMessage());
        }

        /** A sentence of the branch, quoted and escaped; refused where it names an object of the session by its type
         *  and identity, since such a name differs between two runs and a dump neither writes nor replaces one. */
        String zin(String zin) {
            if (zin == null) return "leeg";
            if (identiteiten == null) {
                identiteiten = new HashMap<>();
                for (Object o : sessie.getObjects()) identiteiten.put(identiteit(o), o);
                for (Object o : naam.keySet()) identiteiten.put(identiteit(o), o);
            }
            for (Map.Entry<String, Object> i : identiteiten.entrySet())
                if (zin.contains(i.getKey()))
                    throw new IllegalStateException("de dump schrijft geen identiteit: de zin " + citeer(zin) + " noemt "
                            + i.getKey() + ", " + (naam.containsKey(i.getValue()) ? "het object "
                            + naam.get(i.getValue()).inDeDump() : "een object zonder naam") + " van de sessie");
            return citeer(zin);
        }

        private static String identiteit(Object o) {
            return o.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(o));
        }

        /** The name of {@code o} as a sentence of the branch gives it, null where the dump has none. */
        private String inEenZin(Object o) {
            Naam n = naam.get(o);
            return n == null ? null : n.inEenZin();
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
            if (o == null) return "leeg";
            if (isBean(o)) {
                Naam n = naam.get(o);
                if (n != null) return n.inDeDump();
                String type = o.getClass().getSimpleName();
                if (type.equals("Inconsistentie"))
                    return "Inconsistentie(" + render(RegelSpraakRuntime.get(o, "regel")) + ", "
                            + render(RegelSpraakRuntime.get(o, "subject")) + ")";
                if (type.equals("Segment"))
                    return "Segment(" + RegelSpraakRuntime.get(o, "van") + ", " + RegelSpraakRuntime.get(o, "tot") + ", "
                            + render(RegelSpraakRuntime.get(o, "waarde")) + ")";
                throw new IllegalStateException("de dump heeft geen naam voor een bean van " + type
                        + ": geen caselabel en geen creatie die haar maakte");
            }
            if (o instanceof BigDecimal d) return "BigDecimal(" + d + ")";
            if (o instanceof String s) return "String(" + citeer(s) + ")";
            if (o instanceof Enum<?> e) return e.getDeclaringClass().getSimpleName() + "." + e.name();
            if (o instanceof ElementSleutel k)
                return citeer(k.objecttype()) + "." + citeer(k.naam()) + (k.kenmerk() ? " (kenmerk)" : "");
            if (o instanceof Throwable e) return fout(e);
            if (o instanceof List<?> l) {
                List<String> r = new ArrayList<>();
                for (Object x : l) r.add(render(x));
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
            Class<?> klasse = o.getClass();
            if (klasse.isRecord()) {
                List<String> r = new ArrayList<>();
                for (RecordComponent rc : klasse.getRecordComponents()) {
                    try {
                        rc.getAccessor().setAccessible(true);
                        r.add(rc.getName() + "=" + render(rc.getAccessor().invoke(o)));
                    } catch (ReflectiveOperationException e) {
                        throw new IllegalStateException("de component " + rc + " is niet te lezen", e);
                    }
                }
                return klasse.getSimpleName() + "(" + String.join(", ", r) + ")";
            }
            if (klasse.getName().startsWith("dev.uchar.regelspraak.transpiler.")) {
                // a fact class of the branch that is no record prints its objects with their identities: read its fields
                List<String> r = new ArrayList<>();
                for (Field f : klasse.getDeclaredFields()) {
                    if (Modifier.isStatic(f.getModifiers())) continue;
                    f.setAccessible(true);
                    try {
                        r.add(f.getName() + "=" + render(f.get(o)));
                    } catch (IllegalAccessException e) {
                        throw new IllegalStateException("het veld " + f + " is niet te lezen", e);
                    }
                }
                return klasse.getSimpleName() + "(" + String.join(", ", r) + ")";
            }
            try {
                if (klasse.getMethod("toString").getDeclaringClass() == Object.class)
                    throw new IllegalStateException("de dump schrijft geen identiteit: een " + klasse.getName()
                            + " heeft geen tekst van zichzelf");
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
            // the text of these classes has a fixed form without a space, a quote, a parenthesis or a line break; the
            // text any other class gives of itself has a form unknown here, so it stands quoted
            if (o instanceof Boolean || o instanceof Integer || o instanceof Long || o instanceof java.time.LocalDate
                    || o instanceof java.time.LocalDateTime)
                return klasse.getSimpleName() + "(" + o + ")";
            return klasse.getSimpleName() + "(" + citeer(o.toString()) + ")";
        }
    }

    private static boolean isBean(Object o) {
        return o.getClass().getName().startsWith(FactModelGenerator.PAKKET + ".");
    }

    /** {@code s} between double quotes, with a quote, a backslash and a line break escaped, so that a text stays on its
     *  line and tells where it ends. */
    private static String citeer(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }
}
