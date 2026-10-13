package dev.uchar.regelspraak.transpiler.kern;

/**
 * THE Dutch-phrase → Java/DRL identifier contract (the shared naming contract) — the one place a RegelSpraak name
 * becomes a bean property, getter/setter, fact class, parameter global, rule-name string, or
 * comment splice. Both back-ends import these; neither derives a name locally, so the DRL text and
 * the executable model can never disagree on what a written name maps to.
 */
public final class Namen {
    private Namen() {}

    /** A RegelSpraak name → a Java identifier: split on non-letter/digit runs, then camel-
     *  (upperFirst=false) or Pascal-case (true) the words. NO lidwoord stripping: the resolver already
     *  delivers declared names bare, and a kenmerk whose name deliberately CARRIES an article ("de oudste
     *  jutter") must bind its full name, not collide with a shorter sibling. A word written ALL-CAPS (a
     *  shouted parameter, "STANDAARD TERMIJN") is title-cased FIRST — without that the camel form of the
     *  first word reads "sTANDAARD…". An empty result is "x". A camel result CAN equal a Java
     *  keyword ("final") — reconciling that is the fact-model generator's contract (the explicit fact-model contract), which must move
     *  BOTH back-ends at once; a one-sided suffix here would diverge from the runtime's pascal-based reads. */
    public static String ident(String naam, boolean upperFirst) {
        StringBuilder b = new StringBuilder();
        for (String d : naam.trim().split("[^\\p{L}\\p{N}]+")) {
            if (d.isEmpty()) continue;
            if (isSchreeuw(d)) d = d.charAt(0) + d.substring(1).toLowerCase();
            boolean hoofd = upperFirst || b.length() > 0;
            b.append(hoofd ? Character.toUpperCase(d.charAt(0)) : Character.toLowerCase(d.charAt(0)))
             .append(d.substring(1));
        }
        return b.isEmpty() ? "x" : b.toString();
    }

    /** An ALL-CAPS word of length > 1 (at least one letter, no lowercase letters). */
    private static boolean isSchreeuw(String w) {
        if (w.length() < 2) return false;
        boolean letter = false;
        for (int i = 0; i < w.length(); i++) {
            char c = w.charAt(i);
            if (Character.isLowerCase(c)) return false;
            letter |= Character.isLetter(c);
        }
        return letter;
    }

    /** The JavaBean getter name for an attribute/kenmerk/rol ({@code get<Pascal>}; the boolean {@code is<X>}
     *  form is the FACT side's — mvel resolves the property either way). */
    public static String getter(String naam) { return "get" + ident(naam, true); }

    public static String setter(String naam) { return "set" + ident(naam, true); }

    /** The bean property of a member named {@code naam}: the camel identifier a DRL constraint spells, whose getter and
     *  setter are those of the name itself ({@link #getter}, {@link #setter}), since {@code ident} with
     *  {@code upperFirst} gives the same Pascal form of both where the first word starts with a cased letter. The
     *  executable-model branch plans it once (its plan's {@code Binding.Veld}, {@code Alfa.Veldtoets} and {@code Join}),
     *  so that no rendering derives a property from a declared name with spaces. On a fact of that branch, whose
     *  components are single lowercase words, it is the component's name itself. */
    public static String eigenschap(String naam) { return ident(naam, false); }

    /** A §9.3 regelnaam spliced inside DRL double quotes: {@code \} and {@code "} would end the string (or
     *  escape into it) and a newline ends the rule header — all four are substituted (disjoint characters,
     *  none introduces another, so order is immaterial). The SAME escaping guards the idempotency-constraint
     *  {@code regel == "…"} splice, so the two occurrences always match each other. */
    public static String escapeRegelNaam(String naam) {
        return naam.replace("\\", "/").replace("\"", "'").replace("\r", " ").replace("\n", " ");
    }

    /** User text (a {@code bron()} phrase) spliced into a {@code /* TODO … *&#47;} block comment or an
     *  end-of-line {@code // onopgelost …} comment: a {@code *&#47;} would END the block comment mid-splice
     *  (escaping by construction — the rest of the phrase then parses as DRL and sinks the package) and a newline would
     *  terminate a line comment, so both are defused. */
    public static String commentVeilig(String tekst) {
        return tekst.replace("*/", "* /").replace("\r", " ").replace("\n", " ");
    }

    /** A value spliced as a DRL/mvel STRING literal: backslashes, embedded double quotes and the two line
     *  terminators are ESCAPED — never stripped (escaping by construction: an enum label with an embedded quote must compare
     *  against the real label, not a silently altered one). Returns the quoted literal including its delimiters.
     *
     *  <p>CR and LF are here because an UNESCAPED one reaches the generated DRL as a REAL line break, and the two
     *  halves of a rule then disagree about it: the constraint side keeps the character, while the consequence
     *  block's pre-processor drops it, so a text value with a line break is WRITTEN without it — measured
     *  2026-09-18 on a Tekst consequence, {@code verwacht afslag\nnoord, werkelijk afslagnoord} against a ref that
     *  plays. That is a wrong answer with no complaint, the one outcome a back-end may not produce.
     *
     *  <p>DELIBERATELY only those two. Every other control character was measured through both branches on the
     *  same day — tab, form feed, backspace, vertical tab, NUL — and each round-trips through the DRL text
     *  unaltered, so escaping them would be scope without a witness. The one form this helper does NOT rescue is
     *  a value ENDING in a backslash: the doubled backslash makes the consequence pre-processor read the closing
     *  quote as escaped and run off the end, which fails LOUDLY as a compile refusal (the back-end reports the
     *  form as not carried) and never as a wrong value. Rendering it would have to change how the consequence is
     *  built, not how a value is spelled.
     *
     *  <p>The four substitutions are applied backslash-first, so the backslashes the other three introduce are
     *  not doubled a second time; none of the three can match what the others produce. */
    public static String drlString(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"")
                       .replace("\r", "\\r").replace("\n", "\\n") + "\"";
    }

    /** ref's {@code Checks.norm} ({@code checks.ml}: {@code lowercase_ascii (collapse_ws s)}): the form in
     *  which two DECLARED names are the same name. The IR spells every name as the author wrote it (there
     *  is no canonical spelling on the wire), so a name-keyed lookup that compares raw Strings reads a
     *  differently-capitalised reference as ABSENT where ref, clingo and the verifier all resolve it.
     *
     *  <p>Deliberately NARROWER than the obvious Java one-liner, because ref is the arbiter and a name
     *  equality WIDER than ref's is not a fix but a new divergence: {@code collapse_ws} splits on SPACE and
     *  TAB only (a newline inside a name is a character, not a separator) and {@code lowercase_ascii} folds
     *  A-Z only (so "COÖRDINAAT" and "coördinaat" are DIFFERENT names to ref, the verifier and clingo).
     *  {@code toLowerCase(Locale.ROOT)} + {@code \s+} folds both pairs together, and a key site that reads
     *  "temporal" where ref reads "scalar" then joins a timeline against a {@code BigDecimal} bean member —
     *  a bare backend ERROR on a document all three other executors play. */
    public static String norm(String naam) {
        if (naam == null) return "";
        StringBuilder uit = new StringBuilder();
        for (String woord : naam.split("[ \t]+")) {
            if (woord.isEmpty()) continue;                       // a leading separator run yields ""
            if (uit.length() > 0) uit.append(' ');
            for (int i = 0; i < woord.length(); i++) {
                char c = woord.charAt(i);
                uit.append(c >= 'A' && c <= 'Z' ? (char) (c + 32) : c);
            }
        }
        return uit.toString();
    }

    /** ref's {@code Checks.attr_temporal} as a name contract: whether {@code naam} occurs among the keys of a
     *  declared-name-keyed map, compared with {@link #norm}. THE one temporality test of both back-ends' key
     *  sites — deriving it locally is how the DRL text and the executable model came to disagree with ref
     *  about a mis-capitalised uniqueness key (backlog: drools-sleutelconventies). */
    public static boolean bevatNaam(java.util.Map<String, ?> perNaam, String naam) {
        if (perNaam == null || naam == null) return false;
        return perNaam.containsKey(gedeclareerdeNaam(perNaam.keySet(), naam));
    }

    /** {@link #bevatNaam}'s reading twin: the value {@code perNaam} holds for the declaration that IS
     *  {@code naam} under {@link #norm}, null when none does. The temporal facet maps (granularity, period
     *  count) are keyed by DECLARED name while every rule and case names a member as WRITTEN, so a lookup
     *  that decides "temporal" by norm and then reads the granularity or the period count by exact spelling
     *  would split one member into a timeline without a grain, or a multi-period timeline read as a single
     *  one. Both questions therefore go through this one contract (card drools-temporele-naamnorm). */
    public static <V> V opNaam(java.util.Map<String, V> perNaam, String naam) {
        if (perNaam == null || naam == null) return null;
        return perNaam.get(gedeclareerdeNaam(perNaam.keySet(), naam));
    }

    /** The DECLARED spelling of a WRITTEN name: the member of {@code gedeclareerd} that IS this name under
     *  {@link #norm}, or {@code naam} unchanged when nothing declares it. The naming contract above turns a
     *  DECLARED name into a bean member, so every name that reaches {@link #ident}, {@link #getter} or the
     *  runtime's property resolver must first be brought back to its declaration: a reference written in
     *  another capitalisation ("vondstNummer" for a declared "vondstnummer") resolves for ref, clingo and
     *  the verifier but pascal-cases into a getter the generated fact class does not have. Unchanged when
     *  absent, so an undeclared name still reaches the runtime's own loud "geen getter voor …". */
    public static String gedeclareerdeNaam(java.util.Collection<String> gedeclareerd, String naam) {
        if (gedeclareerd == null || naam == null) return naam;
        if (gedeclareerd.contains(naam)) return naam;            // the common case: written as declared
        String n = norm(naam);
        for (String k : gedeclareerd) if (norm(k).equals(n)) return k;
        return naam;
    }
}
