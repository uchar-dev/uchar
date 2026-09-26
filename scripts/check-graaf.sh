#!/usr/bin/env bash
# Gate 15 — the rule graph of the Drools backend holds to IR-SPEC ch. 5.
#
# backends/drools/src/main/java/dev/uchar/regelspraak/graaf/ is a reading of the IR: which form of the rule
# layer writes and which reads which attribute or characteristic, a member keyed on the type that declares it.
# IR-SPEC ch. 5.1/5.2 defines the dependency graph that reading must agree with, and the verifier computes it
# for WF-045/046. The graph is analysis and presentation, never execution, so no other gate reads it; this one
# holds it in two legs.
#
# 1. THE SPEC LEG. Every corpus/golden/**/ir.json is read by the backend's LOADER (never a source file), and
#    the graph of that model, with the defaults groups and the navigation bindings the loader returns beside
#    it, is printed as its dependencies (GraafProjectie): one line per form -> member -> form path, a form
#    named by the document list it sits in and by its name. The verifier prints the ch. 5 edges of the same
#    document (`verifier --dependency-edges`), and this script projects them onto what the graph draws: every
#    edge over an attribute or characteristic node, whatever forms it joins, without the edge kind (positive,
#    strict, negative), and without the type in the node. The type is left out because the two graphs key it
#    differently on purpose: the graph keys a member on its DECLARING type (one node per member, however
#    many subtypes inherit it), the verifier keys a read on the type it spells and closes a write over the
#    types it reaches (ch. 5.1). They agree on which form depends on which form through which member, and
#    that is what is compared. An edge over a population, a relation or a channel runs over a node the graph
#    does not draw: it is counted per class (producer list, node, consumer list) and reported, not compared.
#    Every golden the loader reads must give EQUAL sets. A difference fails, unless it is listed below in
#    VERSCHIL_BEWUST, one projected line per entry with its reason; a listed line that no longer differs, or
#    whose golden is gone or refused, is a stale entry and fails as well (the check-noop pattern).
#    A golden the loader refuses (a form this backend does not carry, IrLoader.Refusal) cannot be compared;
#    each is reported with the refused form and the number of verifier dependencies nobody compared there.
#
# 2. THE ROUTE LEG. The graph must be the same whether a model comes from the wire or from the source: the
#    backend's own suite cannot resolve a source (its build carries no parser), so the tests that need the
#    frontend's resolver live under backends/drools/src/bronroute/ and are compiled here against the
#    frontend's classes and the backend's jar. GraafBronrouteTest renders the graph of every golden with a
#    bron.rgs along both routes and demands byte-identity; it must run exactly once per such golden. The
#    other classes there are the graph's unit tests: the key over a subtype chain in both directions, the
#    kind-aware key, the navigated read, the characteristic read in a fold filter, the fold source, and each
#    form beside the rules as the writer or reader of a member.
#
# Empty sets fail, never silently green: no goldens, no compared golden, no bron.rgs golden, a suite that ran
# nothing. Needs dune (the verifier), a JDK 21+ with javac, make, and mvn (the backend jar,
# scripts/lib/drools-jar.sh).
set -u
cd "$(dirname "$0")/.."

fail() { echo "check-graaf: FAIL — $*" >&2; exit 1; }

# --- monotone floors (maintainer decision 2026-09-09) --------------------------------------------
# Lower bounds, never equalities: they catch SHRINK while growth needs no bump, so an ordinary corpus card
# touches no protected script. Raised deliberately, at an occasion. Accepted weakness, recorded rather than
# hidden: a count that stays equal while its content shifted passes; VERSCHIL_BEWUST and the per-golden
# comparison are what cover that direction. A deliberate removal lowers the floor in the same commit.
# Raised the same day (fix round on the union of the wave), at the arrival of jutterswet-erfketen: the first
# golden with a rule -> rule dependency over a subtype chain, in both directions (an attribute written on the
# supertype and read on the subtype, an attribute and a characteristic written on the subtype and read on the
# supertype).
# Before it the spec leg could not see the declarant key: a graph keyed on the holder type stayed green here
# (232 of 269 goldens, 45 dependencies) and only the route leg's unit tests caught it; with it, the spec leg
# fails on the golden's three chain dependencies. The union of the wave and that fix round brought 5 goldens
# the loader reads, 4 dependencies and 4 goldens with a bron.rgs.
EXPECTED_MIN_GOLDENS=234   # 2026-09-26 at the landing of the wave before the move: measured 234 (goldens the loader reads, compared with the verifier)
EXPECTED_MIN_AFHANKELIJK=116   # 2026-09-26 at the landing of the wave before the move: measured 116 (dependencies compared, every form of the rule layer)
EXPECTED_MIN_BRONROUTE=98   # 2026-09-26 at the landing of the wave before the move: measured 98 (goldens with a bron.rgs, bron route against wire route)
EXPECTED_MIN_TESTS=23   # 2026-09-26 at the landing of the wave before the move: measured 23 (the graph's unit tests under backends/drools/src/bronroute)

# --- the deliberate differences (reason-carrying; fails when stale) ------------------------------
# One entry per projected dependency that only one of the two graphs draws:
#     '<golden dir, relative to corpus/golden>|<graaf|verifier>|<producer list>|<producer>|<attr|char>|<member>|<consumer list>|<consumer>|<reason>'
# names normalised as in the report (lower case, collapsed whitespace, no version suffix). Two readings are
# known to differ and are deliberate. In both the graph draws a dependency the verifier does not, because the
# graph keys a member on its declaring type where ch. 5.1 closes a write over the instances it reaches: two
# sibling subtypes sharing an inherited member, one writing and one reading it (no golden, measured
# 2026-09-26); and an object creation assigning an inherited member that a form reads on a strict subtype of
# the created type, which the creation never mints, since creation is not polymorphic and ch. 5.1 closes its
# write upward only (the entries below).
VERSCHIL_BEWUST=(
    'jutterswet-wrakboeking|graaf|objectCreations|boek een strandrecht bij elk wrakrecht|attr|grondslag|rules|bereken de bergingsvergoeding van een wrakrecht|the creation mints a Strandrecht and sets its grondslag; the rule reads grondslag on the subtype Wrakrecht, which the creation never mints'
    'jutterswet-wrakboeking-rgs|graaf|objectCreations|boek een strandrecht bij elk wrakrecht|attr|grondslag|rules|bereken de bergingsvergoeding van een wrakrecht|the source of jutterswet-wrakboeking, the same creation and the same read'
    'jutterswet-wrakboekwaarde|graaf|objectCreations|boek een strandrecht bij elk wrakrecht|attr|grondslag|rules|bereken de bergingsvergoeding van een wrakrecht|the creation and the read of jutterswet-wrakboeking, with a value computed on the created Strandrecht beside them'
)

command -v java  >/dev/null 2>&1 || fail "java not found (the backend jar and the route suite need a JDK 21+)"
command -v javac >/dev/null 2>&1 || fail "javac not found (the route suite is compiled here; a bare JRE is not enough)"
command -v make  >/dev/null 2>&1 || fail "make not found (the frontend build)"
if ! command -v dune >/dev/null 2>&1 && command -v opam >/dev/null 2>&1; then eval "$(opam env)"; fi
command -v dune  >/dev/null 2>&1 || fail "dune not found (the verifier; run: eval \$(opam env))"

TMP=$(mktemp -d -t check-graaf-XXXXXX) || fail "mktemp"
trap 'rm -rf "$TMP"' EXIT

# --- builds ---------------------------------------------------------------------------------------
dune build --root spec/verifier 2>"$TMP/dune.err" || { cat "$TMP/dune.err" >&2; fail "the verifier does not build"; }
VERIFIER=spec/verifier/_build/default/bin/verifier.exe
. scripts/lib/drools-jar.sh
JAR=$(ensure_drools_jar) || fail "no fresh backend jar (see the drools-jar line above)"

# --- 1. the spec leg -------------------------------------------------------------------------------
find corpus/golden -name ir.json | LC_ALL=C sort > "$TMP/goldens"
n_goldens=$(grep -c . "$TMP/goldens")
[ "$n_goldens" -gt 0 ] || fail "no corpus/golden/**/ir.json found (an empty corpus is a failure, never silently green)"

# the graph side: one JVM over every golden; a malformed document (a Documentfout) or any other error is a
# failure here, since every golden is well-formed by gate 3
if ! java -cp "$JAR" dev.uchar.regelspraak.graaf.GraafProjectie $(cat "$TMP/goldens") >"$TMP/graaf" 2>"$TMP/graaf.err"; then
    tail -20 "$TMP/graaf.err" >&2
    fail "GraafProjectie did not read every golden"
fi
[ "$(grep -c '^document	' "$TMP/graaf")" -eq "$n_goldens" ] \
    || fail "GraafProjectie reported $(grep -c '^document	' "$TMP/graaf") documents for $n_goldens goldens"

# the verifier side, projected: every edge over an attribute or characteristic node, a form at either end named
# by the document list its pointer names (/rules/<list>/...), the kind and the type dropped; names normalised as
# ref's norm after strip_version. Every edge is also counted by its class (producer list, node, consumer list),
# inside the projection or outside it, in $TMP/randklassen.
: >"$TMP/verifier"; : >"$TMP/randklassen"
while IFS= read -r ir; do
    printf 'document\t%s\n' "$ir" >>"$TMP/verifier"
    printf 'document\t%s\n' "$ir" >>"$TMP/randklassen"
    "$VERIFIER" --dependency-edges "$ir" >"$TMP/edges" 2>"$TMP/edges.err" \
        || { cat "$TMP/edges.err" >&2; fail "$ir: the verifier does not print its dependency edges"; }
    LC_ALL=C awk -F'\t' -v OFS='\t' -v doc="$ir" -v klassen="$TMP/randklassen" '
        function nrm(s) { sub(/ #[0-9]+$/, "", s); gsub(/[ \t]+/, " ", s); sub(/^ /, "", s); sub(/ $/, "", s); return tolower(s) }
        function lijst(p,   d) { if (p !~ /^\/rules\/[A-Za-z]+\/[0-9]/) return ""; split(p, d, "/"); return d[3] }
        NF != 6 { print "ONLEESBAAR", doc, $0; next }
        {
            pl = lijst($2); cl = lijst($5); n = split($4, deel, ":")
            if (pl == "" || cl == "") { print "ONLEESBAAR", doc, $0; next }
            if (deel[1] == "attr" || deel[1] == "char") {
                if (n != 3) { print "ONLEESBAAR", doc, $4; next }
                print "rand", pl, nrm($3), deel[1], deel[3], cl, nrm($6)
                print "binnen", pl " -> " deel[1] " -> " cl >>klassen
            } else if (deel[1] == "pop" || deel[1] == "rel" || deel[1] == "chan") {
                print "buiten", pl " -> " deel[1] " -> " cl >>klassen
            } else print "ONLEESBAAR", doc, $4
        }' "$TMP/edges" | LC_ALL=C sort -u >>"$TMP/verifier"
done <"$TMP/goldens"
if grep -q '^ONLEESBAAR' "$TMP/verifier"; then
    grep '^ONLEESBAAR' "$TMP/verifier" >&2
    fail "a verifier edge line or node cannot be projected (a name carrying ':', a pointer outside /rules/<list>/, or a changed report format)"
fi

# per golden: split both sides into files, then compare
mkdir -p "$TMP/g" "$TMP/v"
awk -F'\t' -v dir="$TMP/g" '
    $1 == "document" { n++; f = dir "/" n; print $2 > (f ".doc"); printf "" > (f ".rand"); next }
    $1 == "weigering" { print $2 > (f ".weigering"); next }
    $1 == "rand" { print > (f ".rand") }' "$TMP/graaf"
awk -F'\t' -v dir="$TMP/v" '
    $1 == "document" { n++; f = dir "/" n; print $2 > (f ".doc"); printf "" > (f ".rand"); next }
    $1 == "rand" { print > (f ".rand") }' "$TMP/verifier"
awk -F'\t' -v dir="$TMP/v" '
    $1 == "document" {
        if (f != "") { close(f ".binnen"); close(f ".buiten") }
        n++; f = dir "/" n; printf "" > (f ".binnen"); printf "" > (f ".buiten"); next
    }
    { print $2 > (f "." $1) }' "$TMP/randklassen"

n_compared=0; n_deps=0; n_refused=0; n_refused_deps=0; n_refused_buiten=0
: >"$TMP/verschil"; : >"$TMP/geweigerd"; : >"$TMP/vergeleken"; : >"$TMP/binnen"; : >"$TMP/buiten"
for i in $(seq 1 "$n_goldens"); do
    ir=$(cat "$TMP/v/$i.doc")
    [ "$(cat "$TMP/g/$i.doc")" = "$ir" ] || fail "the two reports disagree on the order of the goldens at $ir"
    golden=${ir#corpus/golden/}; golden=${golden%/ir.json}
    vdeps=$(grep -c . "$TMP/v/$i.rand")
    if [ -f "$TMP/g/$i.weigering" ]; then
        n_refused=$((n_refused + 1)); n_refused_deps=$((n_refused_deps + vdeps))
        n_refused_buiten=$((n_refused_buiten + $(grep -c . "$TMP/v/$i.buiten")))
        vorm=$(sed 's/^form not carried by the v0 Drools backend: \([^ ]*\).*/\1/' "$TMP/g/$i.weigering") \
            || fail "sed over the refusal of $golden"
        printf '%s\t%s\t%s\n' "$vorm" "$golden" "$vdeps" >>"$TMP/geweigerd"
        continue
    fi
    n_compared=$((n_compared + 1)); n_deps=$((n_deps + vdeps))
    # what the count per class reads: the compared dependencies, and the verifier's edges inside and outside
    cat "$TMP/v/$i.rand" >>"$TMP/vergeleken" && cat "$TMP/v/$i.binnen" >>"$TMP/binnen" \
        && cat "$TMP/v/$i.buiten" >>"$TMP/buiten" || fail "collecting the classes of $golden"
    # a line only one side draws, tagged with its golden and that side
    LC_ALL=C sort -u -o "$TMP/g/$i.rand" "$TMP/g/$i.rand" || fail "sort over $golden"
    LC_ALL=C comm -13 "$TMP/v/$i.rand" "$TMP/g/$i.rand" >"$TMP/alleen-graaf" || fail "comm over $golden"
    LC_ALL=C comm -23 "$TMP/v/$i.rand" "$TMP/g/$i.rand" >"$TMP/alleen-verifier" || fail "comm over $golden"
    for kant in graaf verifier; do
        awk -F'\t' -v OFS='\t' -v golden="$golden" -v kant="$kant" \
            '{ $1 = golden OFS kant; print }' "$TMP/alleen-$kant" >>"$TMP/verschil" || fail "awk over $golden"
    done
done

# the listed differences, in the same tab form; an entry must carry all nine fields, the last its reason
: >"$TMP/bewust"
for e in ${VERSCHIL_BEWUST[@]+"${VERSCHIL_BEWUST[@]}"}; do
    [ "$(printf '%s' "$e" | awk -F'|' '{ print NF }')" -eq 9 ] && [ -n "${e##*|}" ] \
        || fail "VERSCHIL_BEWUST entry is not '<golden>|<graaf|verifier>|<producer list>|<producer>|<attr|char>|<member>|<consumer list>|<consumer>|<reason>': $e"
    printf '%s\n' "${e%|*}" | tr '|' '\t' >>"$TMP/bewust"
done
LC_ALL=C sort -o "$TMP/verschil" "$TMP/verschil"
LC_ALL=C sort -o "$TMP/bewust" "$TMP/bewust"
unlisted=$(LC_ALL=C comm -23 "$TMP/verschil" "$TMP/bewust")
stale=$(LC_ALL=C comm -13 "$TMP/verschil" "$TMP/bewust")
if [ -n "$unlisted" ]; then
    echo "the graph and the verifier's ch. 5 edges differ (golden, side that draws it, producer list, producer, node, member, consumer list, consumer):" >&2
    printf '%s\n' "$unlisted" | sed 's/^/  /' >&2
    fail "the rule graph disagrees with IR-SPEC ch. 5 on $(printf '%s\n' "$unlisted" | grep -c .) dependency line(s) — fix the graph, or list a deliberate difference in VERSCHIL_BEWUST with its reason"
fi
if [ -n "$stale" ]; then
    printf '%s\n' "$stale" | sed 's/^/  /' >&2
    fail "VERSCHIL_BEWUST is stale: the entries above no longer differ (or their golden is gone or refused) — drop them"
fi

[ "$n_compared" -gt 0 ] || fail "no golden was compared (an empty comparison is a failure, never silently green)"
[ "$n_compared" -ge "$EXPECTED_MIN_GOLDENS" ] \
    || fail "$n_compared goldens compared, but EXPECTED_MIN_GOLDENS pins at least $EXPECTED_MIN_GOLDENS — the comparison shrank (a deliberate removal lowers the floor in the same commit)"
[ "$n_deps" -ge "$EXPECTED_MIN_AFHANKELIJK" ] \
    || fail "$n_deps dependencies compared, but EXPECTED_MIN_AFHANKELIJK pins at least $EXPECTED_MIN_AFHANKELIJK — the comparison shrank (a deliberate removal lowers the floor in the same commit)"

echo "check-graaf: spec leg — $n_compared of $n_goldens goldens compared, $n_deps dependencies equal to the verifier's ch. 5 edges (floors $EXPECTED_MIN_GOLDENS / $EXPECTED_MIN_AFHANKELIJK), $(grep -c . "$TMP/bewust") listed difference(s)"
# per class (producer list -> node -> consumer list): the dependencies compared and the verifier edges they
# come from, one dependency standing for every edge that differs from it only in its kind or its type
n_binnen=$(grep -c . "$TMP/binnen"); n_buiten=$(grep -c . "$TMP/buiten")
echo "check-graaf: compared per class — $n_deps dependencies from $n_binnen verifier edges over an attribute or characteristic:"
awk -F'\t' '
    FILENAME == ARGV[1] { dep[$2 " -> " $4 " -> " $6]++; next }
    { randen[$1]++ }
    END { for (k in randen) printf "%d\t%s\t%d\n", dep[k], k, randen[k] }' "$TMP/vergeleken" "$TMP/binnen" \
    | LC_ALL=C sort -t "$(printf '\t')" -k1,1nr -k2,2 \
    | awk -F'\t' '{ printf "    %d %s (from %d edge%s)\n", $1, $2, $3, ($3 == 1 ? "" : "s") }'
echo "check-graaf: outside the comparison — $n_buiten of the $((n_binnen + n_buiten)) verifier edges on the compared goldens run over a population, relation or channel, a node the graph does not draw$([ "$n_buiten" -gt 0 ] && echo '; per class:')"
LC_ALL=C sort "$TMP/buiten" | uniq -c | LC_ALL=C sort -k1,1nr -k2 | sed 's/^ */    /'
if [ "$n_refused" -gt 0 ]; then
    echo "check-graaf: not comparable — $n_refused golden(s) the loader refuses, carrying $n_refused_deps verifier dependencies and $n_refused_buiten edge(s) outside the projection nobody compared:"
    cut -f1 "$TMP/geweigerd" | LC_ALL=C sort | uniq -c | sort -rn | sed 's/^ */    /'
    awk -F'\t' '$3 > 0 { printf "    %s: %s dependencies (%s)\n", $2, $3, $1 }' "$TMP/geweigerd"
fi

# --- 2. the route leg ------------------------------------------------------------------------------
FE=frontends/regelspraak
build_out=$(make -C "$FE" compile 2>&1) || { printf '%s\n' "$build_out" | tail -20 >&2; fail "the frontend build is red"; }
make -C "$FE" junit-jar >/dev/null 2>&1 || fail "could not fetch the JUnit console jar"
ANTLR=$(ls "$FE"/lib/antlr-*-complete.jar 2>/dev/null | head -1)
JUNIT=$(ls "$FE"/lib/junit-platform-console-standalone-*.jar 2>/dev/null | head -1)
[ -n "$ANTLR" ] && [ -n "$JUNIT" ] && [ -d "$FE/out" ] || fail "the frontend build left no classes or tool jars under $FE"

SRC=$(find backends/drools/src/bronroute -name '*.java' | LC_ALL=C sort)
[ -n "$SRC" ] || fail "no route suite under backends/drools/src/bronroute (an empty suite is a failure)"
# the frontend's classes first: they carry the resolver and the emitter, and the record types the jar carries
# too, compiled from the same source tree
CP="$FE/out:$ANTLR:$JAR"
mkdir -p "$TMP/klassen"
javac -cp "$CP:$JUNIT" -d "$TMP/klassen" $SRC 2>"$TMP/javac.err" || { cat "$TMP/javac.err" >&2; fail "the route suite does not compile"; }

# A red run shows each failing test by name with the first line of its message, then the console's counts. The
# tail of the output does not: a route failure's message carries two whole renders, which push the test names
# out of any tail (measured: 129 lines for two red goldens, their names on lines 3 and 59). Output that names no
# failing test (a class that does not load, a discovery error) is shown as its tail instead.
junit_rood() {
    fouten=$(printf '%s\n' "$1" | grep -E '^(Failures \([0-9]+\):|  JUnit Jupiter:|    => )')
    if [ -n "$fouten" ]; then printf '%s\n' "$fouten" >&2; else printf '%s\n' "$1" | tail -40 >&2; fi
    printf '%s\n' "$1" | grep -E '^\[ *[0-9]+ tests (found|skipped|successful|failed|aborted) *\]$' >&2
}

# the JUnit console's own summary: N successful, and nothing failed or skipped
junit() {
    out=$(java -jar "$JUNIT" execute --class-path "$CP:$TMP/klassen" "$@" --details=summary --disable-banner 2>&1)
    status=$?
    if [ $status -ne 0 ]; then junit_rood "$out"; return 1; fi
    ok=$(printf '%s\n' "$out" | sed -n 's/^\[ *\([0-9][0-9]*\) tests successful *\]$/\1/p' | tail -1)
    skipped=$(printf '%s\n' "$out" | sed -n 's/^\[ *\([0-9][0-9]*\) tests skipped *\]$/\1/p' | tail -1)
    [ -n "$ok" ] && [ "${skipped:-1}" -eq 0 ] || { junit_rood "$out"; return 1; }
    echo "$ok"
}

n_rgs=$(find corpus/golden -name bron.rgs | grep -c .)
[ "$n_rgs" -gt 0 ] || fail "no corpus/golden/**/bron.rgs found (an empty route comparison is a failure)"
[ "$n_rgs" -ge "$EXPECTED_MIN_BRONROUTE" ] \
    || fail "$n_rgs goldens carry a bron.rgs, but EXPECTED_MIN_BRONROUTE pins at least $EXPECTED_MIN_BRONROUTE — the route comparison shrank (a deliberate removal lowers the floor in the same commit)"
routes=$(junit --select-class dev.uchar.regelspraak.graaf.GraafBronrouteTest) \
    || fail "the route comparison is red: the graph of a golden's ir.json differs from the graph of its bron.rgs (above)"
[ "$routes" -eq "$n_rgs" ] \
    || fail "the route comparison ran $routes goldens, but $n_rgs carry a bron.rgs — every one of them must be compared"
tests=$(junit --scan-class-path "$TMP/klassen" --exclude-classname '.*GraafBronrouteTest') \
    || fail "the graph's unit tests are red (above), or skipped one"
[ "$tests" -gt 0 ] || fail "the graph's unit tests ran nothing (an empty suite is a failure)"
[ "$tests" -ge "$EXPECTED_MIN_TESTS" ] \
    || fail "the graph's unit tests ran $tests, but EXPECTED_MIN_TESTS pins at least $EXPECTED_MIN_TESTS — the suite shrank (a deliberately dropped test lowers the floor in the same commit)"
echo "check-graaf: route leg — the graph of the wire equals the graph of the source on all $routes goldens with a bron.rgs (floor $EXPECTED_MIN_BRONROUTE); $tests unit tests green (floor $EXPECTED_MIN_TESTS)"
echo "check-graaf: PASS"
