package dev.uchar.regelspraak.message;

import java.util.Locale;
import java.util.Map;

/** A message as code and parameters, rendered from the catalogue only when its text is wanted. A parameter value
 *  that is itself a {@code Msg} is a sentence part that the catalogue also translates; it renders in the language
 *  of the message around it, and so does each element of a list (joined by "; "). */
public record Msg(Component component, String key, Map<String, Object> args) {
    public Msg {
        args = java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(args));
    }

    /** A sentence part of the frontend's catalogue; {@code nameValues} are the parameters, name and value in turn. */
    public static Msg frontend(String key, Object... nameValues) { return new Msg(Component.FRONTEND, key, args(nameValues)); }

    /** The sentence part of the frontend's catalogue when {@code present}, else the empty string: an optional clause. */
    public static Object frontendIf(boolean present, String key, Object... nameValues) {
        return present ? frontend(key, nameValues) : "";
    }

    /** A sentence part of the Drools backend's catalogue. */
    public static Msg drools(String key, Object... nameValues) { return new Msg(Component.DROOLS, key, args(nameValues)); }

    /** Parameters from name/value pairs; a value is kept when it is a {@code Msg} or a list, else its string. */
    public static Map<String, Object> args(Object... nameValues) {
        if (nameValues.length % 2 != 0) throw new IllegalArgumentException("parameters take name/value pairs");
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        for (int i = 0; i < nameValues.length; i += 2) {
            Object v = nameValues[i + 1];
            m.put((String) nameValues[i], v instanceof Msg || v instanceof java.util.List || v instanceof Enumeration ? v : String.valueOf(v));
        }
        return m;
    }

    /** The parts of a list of names or sentences, written "a, b and c": the last two are joined by the word that the
     *  catalogue of the component gives under {@code PART.and}, the rest by commas. */
    public record Enumeration(Component component, java.util.List<?> items) {
        /** The text in the default language of the component, for a string that is still put together by hand. */
        @Override public String toString() { return Messages.valueText(this, Messages.defaultLocale(component)); }
    }

    /** The text in the component's current default language ({@link Messages#defaultLocale}). */
    public String text() { return Messages.render(component, key, args); }

    public String text(Locale locale) { return Messages.render(component, locale, key, args); }

    @Override public String toString() { return text(); }
}
