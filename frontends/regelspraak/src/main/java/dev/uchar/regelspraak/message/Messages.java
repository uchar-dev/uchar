package dev.uchar.regelspraak.message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Renders a message from its catalogue entry: one template per key, in Dutch ({@code nl}) and English ({@code en}),
 * per component. A catalogue is the resource {@code messages-<component>.<language>.tsv}, one {@code key<TAB>template}
 * per line; a template names its parameters as {@code {name}} and writes a literal brace twice. Nothing falls back:
 * an unknown language, a key without an entry and a place that no parameter fills are faults that name the key
 * (design {@code spec/proposals/MESSAGE-CODES.md} §7, card 6). A parameter that the template leaves out is no
 * fault: a code names the object it is about by parameters that its sentence need not print.
 *
 * <p>The language is the argument; without one it is the system property {@value #PROPERTY} and without that the
 * default of the component (frontend {@code nl}, Drools {@code en}).
 */
public final class Messages {
    private Messages() {}

    public static final String PROPERTY = "uchar.messages.locale";

    private static final Map<String, Map<String, String>> CATALOGUES = new ConcurrentHashMap<>();

    /** The language {@code render} uses when none is given: the system property, else the component's default. */
    public static Locale defaultLocale(Component component) {
        String p = System.getProperty(PROPERTY);
        return Locale.forLanguageTag(p == null ? component.defaultLanguage : p);
    }

    public static String render(Component component, String key, Map<String, ?> args) {
        return render(component, defaultLocale(component), key, args);
    }

    public static String render(Component component, Locale locale, String key, Map<String, ?> args) {
        String language = locale.getLanguage();
        if (!language.equals("nl") && !language.equals("en"))
            throw new IllegalArgumentException(key + ": no catalogue for the language '" + locale.toLanguageTag()
                    + "' (nl and en only)");
        String template = catalogue(component, language).get(key);
        if (template == null)
            throw new IllegalArgumentException(key + ": no entry in the " + language + " catalogue of the "
                    + component.resourceName);
        return fill(key, template, language, component, locale, args);
    }

    /** A parameter value as text: a {@link Msg} renders in {@code locale}, a list renders its elements the same way
     *  and joins them with "; " (the separator of sentences, in either language), anything else as {@code
     *  String.valueOf}. */
    public static String valueText(Object value, Locale locale) {
        if (value instanceof Msg m) return m.text(locale);
        if (value instanceof Msg.Enumeration e) {
            java.util.List<String> parts = e.items().stream().map(i -> valueText(i, locale)).toList();
            if (parts.size() < 2) return String.join("", parts);
            return String.join(", ", parts.subList(0, parts.size() - 1)) + " "
                    + render(e.component(), locale, "PART.and", Map.of()) + " " + parts.get(parts.size() - 1);
        }
        if (value instanceof Msg.Joined j)
            return j.items().stream().map(i -> valueText(i, locale)).collect(java.util.stream.Collectors.joining(j.separator()));
        if (value instanceof java.util.List<?> l)
            return l.stream().map(e -> valueText(e, locale)).collect(java.util.stream.Collectors.joining("; "));
        return String.valueOf(value);
    }

    /** Whether the catalogue of the component holds the key in the language; the checks of the gates read the
     *  catalogue files themselves, this is for the one that renders. */
    public static Set<String> keys(Component component, String language) {
        return catalogue(component, language).keySet();
    }

    private static String fill(String key, String template, String language, Component component, Locale locale,
                               Map<String, ?> args) {
        StringBuilder out = new StringBuilder();
        int n = template.length();
        for (int i = 0; i < n; i++) {
            char c = template.charAt(i);
            if (c == '{') {
                if (i + 1 < n && template.charAt(i + 1) == '{') { out.append('{'); i++; continue; }
                int close = template.indexOf('}', i);
                if (close < 0) throw new IllegalArgumentException(key + ": unclosed '{' in the " + language + " template");
                String name = template.substring(i + 1, close);
                if (!args.containsKey(name))
                    throw new IllegalArgumentException(key + ": the " + language + " template has the place {" + name
                            + "} and the message carries no such parameter");
                out.append(valueText(args.get(name), locale));
                i = close;
            } else if (c == '}') {
                if (i + 1 < n && template.charAt(i + 1) == '}') { out.append('}'); i++; continue; }
                throw new IllegalArgumentException(key + ": stray '}' in the " + language + " template");
            } else out.append(c);
        }
        return out.toString();
    }

    private static Map<String, String> catalogue(Component component, String language) {
        return CATALOGUES.computeIfAbsent(component.resourceName + "." + language, k -> load(component, language));
    }

    private static Map<String, String> load(Component component, String language) {
        String path = "/dev/uchar/regelspraak/message/messages-" + component.resourceName + "." + language + ".tsv";
        try (InputStream in = Messages.class.getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("catalogue " + path + " is not on the classpath");
            Map<String, String> m = new HashMap<>();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line; int no = 0;
                while ((line = r.readLine()) != null) {
                    no++;
                    int tab = line.indexOf('\t');
                    if (tab <= 0) throw new IllegalStateException(path + ":" + no + ": not key<TAB>template");
                    if (m.put(line.substring(0, tab), line.substring(tab + 1)) != null)
                        throw new IllegalStateException(path + ":" + no + ": key " + line.substring(0, tab) + " twice");
                }
            }
            return m;
        } catch (IOException e) {
            throw new IllegalStateException("catalogue " + path + " is unreadable", e);
        }
    }
}
