package dev.uchar.regelspraak.cases;

import dev.uchar.regelspraak.graaf.ProducentConsument.ElementSleutel;
import dev.uchar.regelspraak.runtime.RegelSpraakRuntime;
import dev.uchar.regelspraak.schema.FactModelGenerator;
import dev.uchar.regelspraak.transpiler.tms.Fixpunt;
import dev.uchar.regelspraak.transpiler.tms.Fout;
import dev.uchar.regelspraak.transpiler.tms.Gemaakt;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The canonical text of one session fired through {@link DroolsBackend#vuur}: the guard of the exec-model branch while
 * card drl-teksttak-op-truth-maintenance rebuilds its lowering into one analysis with two renderings. The harness
 * {@code Vuurspoor} in the test tree writes it per case of every golden the branch plays, and the seam in
 * {@link CaseRunner} per session the suite fires through the replay, where the property {@value #EIGENSCHAP} names a
 * directory. Two dumps of one code base are equal; a dump before a step and one after it are compared with
 * {@code diff -r}.
 *
 * <p>Every line starts with its section, so a diff counts its lines per section ({@code awk '{print $2}'} over the
 * changed lines): {@code vuringen} (the firings, the cap and the sentence of a run without a fixpoint),
 * {@code spoor-telling} (the firings per rule), {@code spoor-volgorde} (the rules in the order they fired),
 * {@code lees} (everything {@link DroolsBackend#lees} gives) and {@code feiten}/{@code feit} (every object in working
 * memory at the fixpoint, the beans with their fields among them, so that a write on a bean during the fire shows).
 * The order of the firings depends on the order in which a rendering adds the rules, so a step reports it and explains
 * a difference; the other sections hold strictly.
 *
 * <p>The text names an input bean by its case label and a made object by its creation and its subject,
 * {@code «creatie»(onderwerp)#k}, and never by its identity: the identity hash of an object differs between two runs,
 * so every {@code Type@hex} that a sentence of the branch carries is replaced by the name of its object, or by
 * {@code Type@?}. Numbers keep their scale, and every list of lines is sorted.
 */
final class Sessiedump {

    /** The system property that turns the seam of {@link CaseRunner} on: the directory it writes to. */
    static final String EIGENSCHAP = "uchar.vuurspoor";

    /** The directory of the seam, read once; null without the property, and then the seam does nothing. */
    static final String NAADMAP = System.getProperty(EIGENSCHAP);

    private static final Pattern IDENTITEIT = Pattern.compile("\\b([A-Za-z_$][A-Za-z0-9_$]*)@([0-9a-f]+)\\b");

    /** The sessions the seam wrote, per document and branch: the number in the name of the next file. */
    private static final Map<String, Integer> VOLGNUMMERS = new HashMap<>();

    private final List<String> spoor = new ArrayList<>();
    private final AgendaEventListener luisteraar;
    private final KieSession sessie;

    private Sessiedump(KieSession sessie) {
        this.sessie = sessie;
        this.luisteraar = new DefaultAgendaEventListener() {
            @Override public void afterMatchFired(AfterMatchFiredEvent e) { spoor.add(e.getMatch().getRule().getName()); }
        };
        sessie.addEventListener(luisteraar);
    }

    /** Records the firings of {@code sessie} from now on, in order: call it right before the fire. */
    static Sessiedump op(KieSession sessie) {
        return new Sessiedump(sessie);
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

    /** Appends the dump of a session whose {@code vuur} returned {@code v}: the firings, and at a fixpoint what
     *  {@link DroolsBackend#lees} gives and every object in working memory. {@code labels} maps each case label to its
     *  input bean. */
    void schrijf(StringBuilder uit, DroolsBackend.Build bouw, DroolsBackend.Vuring v, Map<String, Object> labels) {
        sessie.removeEventListener(luisteraar);
        Map<Object, String> naam = namen(labels);
        Map<String, String> identiteit = identiteiten(naam);
        uit.append("vuringen ").append(v.vuringen()).append(" plafond ").append(v.plafond()).append(" geenFixpunt ")
                .append(zonderIdentiteit(String.valueOf(v.geenFixpunt()), identiteit)).append('\n');
        spoor(uit);
        if (v.geenFixpunt() != null) return;
        Map<Object, String> label = new IdentityHashMap<>();
        labels.forEach((l, b) -> label.put(b, l));
        Fixpunt fp = DroolsBackend.lees(bouw, sessie, label::get);
        List<String> lees = new ArrayList<>();
        for (Fixpunt.AfgeleideWaarde w : fp.waarden())
            lees.add("waarde " + naamVan(w.object(), naam) + "." + w.attribuut() + " = " + render(w.waarde(), naam));
        for (Fixpunt.AfgeleidKenmerk k : fp.kenmerken()) lees.add("kenmerk " + naamVan(k.object(), naam) + " " + k.kenmerk());
        for (Fixpunt.GemaaktObject g : fp.objecten())
            lees.add("object " + naamVan(g.object(), naam) + " door " + g.creatie() + " voor " + naamVan(g.onderwerp(), naam));
        for (Fixpunt.GemaakteKoppeling k : fp.koppelingen())
            lees.add("koppeling " + k.feittype() + " " + k.eersteRol() + "=" + naamVan(k.eerste(), naam) + " "
                    + k.tweedeRol() + "=" + naamVan(k.tweede(), naam));
        for (Fout f : fp.fouten()) lees.add("fout " + naamVan(f.onderwerp(), naam) + " " + f.regel() + ": " + f.melding());
        for (Object i : fp.inconsistenties()) lees.add("inconsistentie " + render(i, naam));
        regels(uit, "lees", lees, identiteit);
        List<String> feiten = new ArrayList<>();
        for (Object o : sessie.getObjects()) feiten.add(feit(o, naam));
        uit.append("feiten ").append(feiten.size()).append('\n');
        regels(uit, "feit", feiten, identiteit);
    }

    /** Appends the dump of a session whose {@code vuur} threw {@code e}: the exception and the firings before it. */
    void schrijf(StringBuilder uit, RuntimeException e, Map<String, Object> labels) {
        sessie.removeEventListener(luisteraar);
        Map<String, String> identiteit = identiteiten(namen(labels));
        uit.append("vuringen gooit ").append(zonderIdentiteit(fout(e), identiteit)).append('\n');
        spoor(uit);
    }

    private void spoor(StringBuilder uit) {
        TreeMap<String, Integer> telling = new TreeMap<>();
        for (String r : spoor) telling.merge(r, 1, Integer::sum);
        uit.append("spoor-telling ").append(telling).append('\n');
        uit.append("spoor-volgorde ").append(spoor).append('\n');
    }

    private static void regels(StringBuilder uit, String sectie, List<String> regels, Map<String, String> identiteit) {
        List<String> schoon = new ArrayList<>();
        for (String r : regels) schoon.add(zonderIdentiteit(r, identiteit));
        schoon.sort(Comparator.naturalOrder());
        for (String r : schoon) uit.append(sectie).append(' ').append(r).append('\n');
    }

    /** The name of every bean: the input beans by their case label, a made object by its creation and its subject. The
     *  marks are taken by phase, so the subject of a mark is named before the object made for it: a creation takes no
     *  subject of its own phase. Within a phase a creation makes one object per subject. */
    private Map<Object, String> namen(Map<String, Object> labels) {
        Map<Object, String> naam = new IdentityHashMap<>();
        labels.forEach((l, b) -> naam.put(b, l));
        List<Gemaakt> merken = new ArrayList<>();
        for (Object o : sessie.getObjects(o -> o instanceof Gemaakt)) merken.add((Gemaakt) o);
        merken.sort(Comparator.comparingInt(Gemaakt::fase));
        Map<String, Integer> teller = new HashMap<>();
        int i = 0;
        while (i < merken.size()) {
            int j = i;
            while (j < merken.size() && merken.get(j).fase() == merken.get(i).fase()) j++;
            List<Map.Entry<String, Object>> fase = new ArrayList<>();
            for (Gemaakt g : merken.subList(i, j))
                fase.add(Map.entry("«" + g.regel() + "»(" + naamVan(g.onderwerp(), naam) + ")", g.object()));
            fase.sort(Map.Entry.comparingByKey());
            for (Map.Entry<String, Object> e : fase)
                naam.putIfAbsent(e.getValue(), e.getKey() + "#" + teller.merge(e.getKey(), 1, Integer::sum));
            i = j;
        }
        return naam;
    }

    /** What the branch's sentences print for each named object ({@code Feitnamen.object}), by that name. */
    private static Map<String, String> identiteiten(Map<Object, String> naam) {
        Map<String, String> uit = new HashMap<>();
        naam.forEach((o, n) -> uit.put(o.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(o)), n));
        return uit;
    }

    private static String zonderIdentiteit(String tekst, Map<String, String> identiteit) {
        Matcher m = IDENTITEIT.matcher(tekst);
        StringBuilder uit = new StringBuilder();
        while (m.find()) {
            String n = identiteit.get(m.group());
            m.appendReplacement(uit, Matcher.quoteReplacement(n != null ? n : m.group(1) + "@?"));
        }
        m.appendTail(uit);
        return uit.toString();
    }

    private static String naamVan(Object o, Map<Object, String> naam) {
        if (o == null) return "leeg";
        String n = naam.get(o);
        return n != null ? n : o.getClass().getSimpleName() + "#ongelabeld";
    }

    private static boolean isBean(Object o) {
        return o.getClass().getName().startsWith(FactModelGenerator.PAKKET + ".");
    }

    /** One object in working memory: a bean with its name and its fields, and every other fact as {@link #render}. */
    private static String feit(Object o, Map<Object, String> naam) {
        if (!isBean(o) || o.getClass().getSimpleName().equals("Inconsistentie")) return render(o, naam);
        List<String> velden = new ArrayList<>();
        for (Class<?> k = o.getClass(); k != null && k.getName().startsWith(FactModelGenerator.PAKKET + ".");
             k = k.getSuperclass())
            for (Field f : k.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) continue;
                f.setAccessible(true);
                try {
                    velden.add(f.getName() + "=" + render(f.get(o), naam));
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("het veld " + f + " van een bean is niet te lezen", e);
                }
            }
        velden.sort(Comparator.naturalOrder());
        return "bean " + naamVan(o, naam) + " " + o.getClass().getSimpleName() + "(" + String.join(", ", velden) + ")";
    }

    private static String fout(Throwable e) {
        return e.getClass().getSimpleName() + ": " + e.getMessage();
    }

    /** A value or a fact, without identities: a bean by its name, a record and every other class of the branch by its
     *  components or fields, a number with its scale, a collection in its order where it has one and sorted where not. */
    static String render(Object o, Map<Object, String> naam) {
        if (o == null) return "leeg";
        if (isBean(o)) {
            String n = naam.get(o);
            if (n != null) return n;
            String type = o.getClass().getSimpleName();
            if (type.equals("Inconsistentie"))
                return "Inconsistentie(" + RegelSpraakRuntime.get(o, "regel") + ", "
                        + render(RegelSpraakRuntime.get(o, "subject"), naam) + ")";
            if (type.equals("Segment"))
                return "Segment(" + RegelSpraakRuntime.get(o, "van") + ", " + RegelSpraakRuntime.get(o, "tot") + ", "
                        + render(RegelSpraakRuntime.get(o, "waarde"), naam) + ")";
            return type + "#ongelabeld";
        }
        if (o instanceof BigDecimal d) return d.toPlainString();
        if (o instanceof Enum<?> e) return e.name();
        if (o instanceof ElementSleutel k) return k.objecttype() + "." + k.naam() + (k.kenmerk() ? " (kenmerk)" : "");
        if (o instanceof Throwable e) return fout(e);
        if (o instanceof List<?> l) {
            List<String> r = new ArrayList<>();
            for (Object x : l) r.add(render(x, naam));
            return "[" + String.join(", ", r) + "]";
        }
        if (o instanceof Collection<?> c) {
            List<String> r = new ArrayList<>();
            for (Object x : c) r.add(render(x, naam));
            r.sort(Comparator.naturalOrder());
            return "{" + String.join(", ", r) + "}";
        }
        if (o instanceof Map<?, ?> m) {
            List<String> r = new ArrayList<>();
            m.forEach((k, w) -> r.add(render(k, naam) + "=" + render(w, naam)));
            r.sort(Comparator.naturalOrder());
            return "{" + String.join(", ", r) + "}";
        }
        Class<?> klasse = o.getClass();
        if (klasse.isRecord()) {
            List<String> r = new ArrayList<>();
            for (RecordComponent rc : klasse.getRecordComponents()) {
                try {
                    rc.getAccessor().setAccessible(true);
                    r.add(rc.getName() + "=" + render(rc.getAccessor().invoke(o), naam));
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
                    r.add(f.getName() + "=" + render(f.get(o), naam));
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("het veld " + f + " is niet te lezen", e);
                }
            }
            return klasse.getSimpleName() + "(" + String.join(", ", r) + ")";
        }
        return o.toString();
    }
}
