#!/usr/bin/env bash
# The carrier check of the Drools backend's refusal register (card drools-refusal-codes, 2026-10-19; design
# spec/proposals/MESSAGE-CODES.md §2.3, §3.1). Called from gate 12 (scripts/check-drools.sh); runs alone as well.
#
# The register is the enum backends/drools/src/main/java/dev/uchar/regelspraak/refusal/DroolsCode.java, one entry per
# line: NAME("DROOLS-###", Kind.X, "key", "params", ef). A place that refuses a form or a case names a constant, so the
# compiler already refuses a code that is not registered; what the compiler cannot see, this script checks in BOTH
# directions:
#   - every registered constant is named at least once in the backend's sources (a code nobody raises is dead text, and
#     a gate that pins it would pin nothing);
#   - every `DroolsCode.NAME` the sources name is a registered constant (a second look at what the compiler enforces,
#     so that a register read by this script and the one the compiler reads cannot drift apart);
#   - the numbers run 001, 002, ... without a gap, a repeat or a reordering (a code is never reused or renumbered);
#   - the keys are unique, and so are the constant names;
#   - an `ef` names a row of the normative table of fault kinds (RUNTIME-SEMANTICS ch. 5.7b);
#   - the register parses whole: every line that opens an entry has the one form, and the count of entries is the count
#     of codes, never less than the floor below.
# A register that shrank, or a line the parser skipped, fails the gate; an empty register is a failure, never silently
# green.
#
# The text of a refusal stands in a catalogue, never at the place that refuses (card message-catalogues, 2026-10-19;
# design spec/proposals/MESSAGE-CODES.md §7). The catalogues (Dutch and English,
# backends/drools/src/main/resources/.../message/) are held to the register, and the places to the catalogues:
#   - every code has an entry `DROOLS-###` in both languages, and an entry is named after a code, after a part of one
#     (`DROOLS-###.slug`), after a fault kind of ch. 5.7b (`EF-###`, `EF-###.slug`), a frame (`FRAME.slug`) or a
#     shared part (`PART.slug`); an entry in one language only fails;
#   - the places of a code's entry are parameters of the code (the register names every one: a `-` is no name), and
#     the places of an entry of a fault kind are the ones EF_PLACES lists, which the carrier of the fault passes;
#   - a place that names a code in the sources (`DroolsCode.NAME`) is followed by `.reason(` or `.part(`, whose
#     arguments are names and values in turn, each name a parameter of the code, so that no place hands a sentence;
#   - the English entries carry no Dutch function word except where the script lists entry and word with the reason;
#   - each language holds at least EXPECTED_MIN_DROOLS_MESSAGES entries named after a code, a fault kind or a frame.
set -u
cd "$(dirname "$0")/.."

fail() { echo "check-drools-codes: FAIL — $*" >&2; exit 1; }

REGISTER=backends/drools/src/main/java/dev/uchar/regelspraak/refusal/DroolsCode.java
SOURCES=backends/drools/src/main/java
TABLE=spec/RUNTIME-SEMANTICS.md

# monotone floor (maintainer decision 2026-09-09): a lower bound, so that a register that quietly lost entries fails
# and one that grew needs no edit here. Measured 2026-10-19 at the card drools-refusal-codes: 289 codes.
EXPECTED_MIN_DROOLS_CODES=289
# Measured 2026-10-19 at the card message-catalogues: per language 325 entries named after a code (290), a fault kind or a
# part of one (EF-###, EF-###.slug: 23) or a frame (12); the parts of the DROOLS codes and the shared parts are not counted.
EXPECTED_MIN_DROOLS_MESSAGES=325

[ -f "$REGISTER" ] || fail "no register at $REGISTER"
[ -f "$TABLE" ] || fail "no table of fault kinds at $TABLE"

entries=$(grep -c '^    [A-Z][A-Z0-9_]*("DROOLS-' "$REGISTER")
opened=$(grep -c '("DROOLS-' "$REGISTER")
[ "$entries" -gt 0 ] || fail "the register holds no entry (an empty register is a failure, never silently green)"
[ "$entries" -eq "$opened" ] \
  || fail "$opened lines of $REGISTER name a DROOLS code but only $entries have the form NAME(\"DROOLS-###\", Kind.X, \"key\", \"params\", ef) — the parser would skip the rest"
[ "$entries" -ge "$EXPECTED_MIN_DROOLS_CODES" ] \
  || fail "the register holds $entries codes, but EXPECTED_MIN_DROOLS_CODES pins at least $EXPECTED_MIN_DROOLS_CODES — the register shrank (a deliberate removal lowers the floor in the same commit)"

tmp=$(mktemp) || exit 2
trap 'rm -f "$tmp"' EXIT
# name <TAB> number <TAB> kind <TAB> key <TAB> ef
TAB=$(printf '\t')
sed -nE 's/^    ([A-Z][A-Z0-9_]*)\("DROOLS-([0-9]{3})", Kind\.([A-Z]+), "([^"]*)", "[^"]*", (null|"EF-[0-9]{3}")\)[,;]$/\1'"$TAB"'\2'"$TAB"'\3'"$TAB"'\4'"$TAB"'\5/p' "$REGISTER" > "$tmp"
parsed=$(wc -l < "$tmp" | tr -d ' ')
[ "$parsed" -eq "$entries" ] || fail "$entries entries in $REGISTER, but only $parsed parse in the one form (a kind, a key or an ef off the pattern)"

n=0
while IFS="$TAB" read -r name num kind key ef; do
  n=$((n + 1))
  want=$(printf '%03d' "$n")
  [ "$num" = "$want" ] || fail "$name is DROOLS-$num, but the register's ${n}th entry must be DROOLS-$want (numbers run without a gap, a repeat or a reordering)"
  case "$kind" in REFUSAL|LIMIT|CONTRACT|FALLBACK) ;; *) fail "DROOLS-$num has the kind $kind (REFUSAL, LIMIT, CONTRACT or FALLBACK)" ;; esac
  expected_name=$(printf '%s' "$key" | tr 'a-z-' 'A-Z_')
  [ "$name" = "$expected_name" ] || fail "DROOLS-$num: the constant $name does not spell its key '$key' ($expected_name)"
  if [ "$ef" != null ]; then
    code=${ef//\"/}
    grep -q "^| $code |" "$TABLE" || fail "DROOLS-$num names $code, which is no row of the table of fault kinds in $TABLE (ch. 5.7b)"
  fi
done < "$tmp"

dup=$(cut -f4 "$tmp" | sort | uniq -d | head -1)
[ -z "$dup" ] || fail "the key '$dup' is registered twice (one key per message template)"
dup=$(cut -f1 "$tmp" | sort | uniq -d | head -1)
[ -z "$dup" ] || fail "the constant $dup is registered twice"

# the named constants, in the sources outside the register, comments left out: a comment is no place that refuses
named=$(find "$SOURCES" -name '*.java' ! -path "$REGISTER" -print0 \
  | xargs -0 sed -e 's://.*$::' -e 's:^[[:space:]]*\*.*$::' \
  | grep -o 'DroolsCode\.[A-Z][A-Z0-9_]*' | sed 's/^DroolsCode\.//' | sort -u)
for name in $named; do
  cut -f1 "$tmp" | grep -qx "$name" || fail "the sources name DroolsCode.$name, which the register does not hold"
done
unraised=$(cut -f1 "$tmp" | sort | comm -23 - <(printf '%s\n' "$named") | tr '\n' ' ')
[ -z "$unraised" ] || fail "registered but never named by a place that refuses: $unraised(a code nobody raises is dead text; remove it, and renumber what follows while no gate pins it)"

python3 -I - "$REGISTER" "$SOURCES" "$TABLE" "$EXPECTED_MIN_DROOLS_MESSAGES" <<'PYEOF' || exit 1
import os, re, sys
register, sources, table, floor = sys.argv[1], sys.argv[2], sys.argv[3], int(sys.argv[4])
faults = []
def fail(m): faults.append(m)

# the register: id -> name, params
reg = {}; names = {}
for l in open(register, encoding="utf-8"):
    m = re.match(r'\s+([A-Z][A-Z0-9_]*)\("(DROOLS-\d{3})", Kind\.\w+, "[^"]*", "([^"]*)", (?:null|"EF-\d{3}")\)', l)
    if not m: continue
    params = [p for p in m.group(3).split(",") if p]
    if "-" in params: fail("%s: %s names a parameter '-' (every parameter has a name)" % (register, m.group(2)))
    reg[m.group(2)] = params; names[m.group(1)] = m.group(2)

PLACE = re.compile(r"\{([A-Za-z][A-Za-z0-9]*)\}")
DUTCH = set("de het een niet geen van voor zonder bij naar zijn wordt moet kan dat die als aan om vereist heeft wel ook maar dus deze dit haar er nog zo tot uit worden dan en op met te".split())
DUTCH_EXCEPTIONS = {("DROOLS-257", "de"): "'DE' is the quantifier of the wire, which names one element"}
# the places of the entries of a fault kind (a sentence; every other `EF-###.slug` is a part of one): the parameters its carrier passes (the table of ch. 5.7b names the ones a red
# case pins; a sentence may print others, such as the type of the creation budget or the detail of a calendar fault)
EF_PLACES = {
    "EF-003.malformed": {"lexeme", "field"}, "EF-003.calendar": {"lexeme", "field", "detail"},
    "EF-005.bare": set(), "EF-005.in-rule": {"rule", "dividend"}, "EF-007": set(), "EF-008": {"count"},
    "EF-009": {"reach", "from"}, "EF-010": set(), "EF-011.date": set(), "EF-011.year": set(), "EF-012": set(),
    "EF-015": {"creation", "stratum", "depth", "type", "parentDepth"},
    "EF-016": {"count", "enumeration", "attribute", "object"}, "EF-019.expression": set(), "EF-019.condition": set(),
}
def dutch_words(t):
    u = t.replace("{{", "").replace("}}", "")
    return set(w for w in re.findall(r"[a-zà-ÿ]+", re.sub(r"'[^']*'", "", PLACE.sub("", u)).lower()) if w in DUTCH)

rows = set(re.findall(r"^\| (EF-\d{3}) \|", open(table, encoding="utf-8").read(), re.M))
cats = {}
d = os.path.join(os.getcwd(), "backends/drools/src/main/resources/dev/uchar/regelspraak/message")
for lang in ("nl", "en"):
    p = os.path.join(d, "messages-drools.%s.tsv" % lang)
    rel = os.path.relpath(p)
    if not os.path.isfile(p): fail("%s: the %s catalogue is missing" % (rel, lang)); continue
    text = open(p, encoding="utf-8").read(); entries = {}
    if text and not text.endswith("\n"): fail("%s: the last line has no newline" % rel)
    for n, l in enumerate(text.split("\n")[:-1], 1):
        if "\t" not in l: fail("%s:%d: not key<TAB>template" % (rel, n)); continue
        k, t = l.split("\t", 1)
        if k in entries: fail("%s:%d: the key %s twice" % (rel, n, k)); continue
        entries[k] = (t, n)
    cats[lang] = (rel, entries)
for lang, (rel, entries) in cats.items():
    for k, (t, n) in entries.items():
        where = "%s:%d (%s, %s)" % (rel, n, k, lang)
        u = t.replace("{{", "").replace("}}", ""); places = set(PLACE.findall(u))
        if re.search(r"[{}]", PLACE.sub("", u)): fail("%s: a brace that is no place and is not written twice" % where)
        m = re.fullmatch(r"(DROOLS-\d{3})(\.[a-z0-9]+(?:-[a-z0-9]+)*)?", k)
        if m:
            if m.group(1) not in reg: fail("%s: the entry belongs to no code of the register" % where); continue
            if not m.group(2):
                for pl in sorted(places - set(reg[k])): fail("%s: the place {%s} is no parameter of the code (parameters %s)" % (where, pl, reg[k]))
        elif re.fullmatch(r"EF-\d{3}(\.[a-z0-9]+(?:-[a-z0-9]+)*)?", k):
            if k[:6] not in rows: fail("%s: %s is no row of the table of fault kinds in %s (ch. 5.7b)" % (where, k[:6], table))
            elif k in EF_PLACES and places != EF_PLACES[k]: fail("%s: the places %s differ from EF_PLACES %s" % (where, sorted(places), sorted(EF_PLACES[k])))
        elif not re.fullmatch(r"(FRAME|PART)\.[a-z0-9]+(?:[.-][a-z0-9]+)*", k):
            fail("%s: the key is no code, no fault kind, no part of one, no frame and no shared part" % where)
        if lang == "en":
            for w in sorted(dutch_words(t)):
                if (k, w) not in DUTCH_EXCEPTIONS: fail("%s: the English entry carries the Dutch word '%s'" % (where, w))
for k in EF_PLACES:
    for lang, (rel, entries) in cats.items():
        if k not in entries: fail("%s: EF_PLACES lists %s, which the catalogue lacks" % (rel, k))
if len(cats) == 2:
    for k in sorted(set(cats["nl"][1]) ^ set(cats["en"][1])):
        lang = "nl" if k in cats["nl"][1] else "en"
        fail("%s: the entry %s has no sister entry in the other catalogue" % (cats[lang][0], k))
    for key, w in DUTCH_EXCEPTIONS:
        if key not in cats["en"][1] or w not in dutch_words(cats["en"][1][key][0]):
            fail("the exception (%s, %s) of DUTCH_EXCEPTIONS is stale" % (key, w))
for lang, (rel, entries) in cats.items():
    for c in reg:
        if c not in entries: fail("%s: the register code %s has no entry in the %s catalogue" % (rel, c, lang))
    n = sum(1 for k in entries if re.fullmatch(r"DROOLS-\d{3}|EF-\d{3}(\.[a-z0-9-]+)?|FRAME\..*", k))
    if n < floor: fail("%s holds %d entries named after a code, a fault kind or a frame, EXPECTED_MIN_DROOLS_MESSAGES pins at least %d" % (rel, n, floor))

# the places that name a code
def blank(s):
    out = []; i = 0; n = len(s)
    while i < n:
        c = s[i]
        if s.startswith("//", i):
            j = s.find("\n", i); j = n if j < 0 else j; out.append(" " * (j - i)); i = j
        elif s.startswith("/*", i):
            j = s.find("*/", i); j = n if j < 0 else j + 2; out.append(re.sub(r"[^\n]", " ", s[i:j])); i = j
        elif c in "\"'":
            j = i + 1
            while s[j] != c: j += 2 if s[j] == "\\" else 1
            out.append(s[i:j + 1]); i = j + 1
        else: out.append(c); i += 1
    return "".join(out)
def args_of(s, o):
    d = 0; start = o + 1; args = []; j = o
    while True:
        c = s[j]
        if c in "\"'":
            j += 1
            while s[j] != c: j += 2 if s[j] == "\\" else 1
        elif c in "([{": d += 1
        elif c in ")]}":
            d -= 1
            if d == 0:
                last = s[start:j]
                if last.strip() or args: args.append(last)
                return args, j + 1
        elif c == "," and d == 1: args.append(s[start:j]); start = j + 1
        j += 1
used = 0
for dp, _, fs in os.walk(sources):
    for f in sorted(fs):
        if not f.endswith(".java") or os.path.join(dp, f) == register: continue
        p = os.path.join(dp, f); s = blank(open(p, encoding="utf-8").read())
        for m in re.finditer(r"\bDroolsCode\.([A-Z][A-Z0-9_]*)\b(?!\s*\.\s*(?:reason|part)\s*\()", s):
            if m.group(1) in names:
                fail("%s:%d: DroolsCode.%s stands without .reason( or .part( — a place hands its code with parameters, never a sentence" % (p, s.count("\n", 0, m.start()) + 1, m.group(1)))
        for m in re.finditer(r"\bDroolsCode\.([A-Z][A-Z0-9_]*)\s*\.\s*(reason|part)\s*\(", s):
            if m.group(1) not in names: fail("%s: DroolsCode.%s is not in the register" % (p, m.group(1))); continue
            used += 1
            a, _ = args_of(s, m.end() - 1)
            off = 1 if m.group(2) == "part" else 0
            if m.group(2) == "part":
                lits = re.findall(r'"([^"]*)"', a[0]) if a else []
                if not lits or not all(re.fullmatch(r"[a-z0-9]+(-[a-z0-9]+)*", x) for x in lits) or re.sub(r'"[^"]*"|[?:\s]|[A-Za-z_.()&|!=<>0-9,]+', "", a[0]): fail("%s:%d: .part( takes the name of the part first" % (p, s.count("\n", 0, m.start()) + 1)); continue
            if (len(a) - off) % 2: fail("%s:%d: DroolsCode.%s.%s( takes names and values in turn, this call has an odd number" % (p, s.count("\n", 0, m.start()) + 1, m.group(1), m.group(2))); continue
            if m.group(2) == "reason":
                for i in range(0, len(a), 2):
                    nm = re.fullmatch(r'\s*"([A-Za-z][A-Za-z0-9]*)"\s*', a[i])
                    if not nm: fail("%s:%d: DroolsCode.%s.reason( names a parameter by a string literal, this one is %r" % (p, s.count("\n", 0, m.start()) + 1, m.group(1), a[i].strip()))
                    elif nm.group(1) not in reg[names[m.group(1)]]:
                        fail("%s:%d: DroolsCode.%s.reason( passes %s, the register has %s" % (p, s.count("\n", 0, m.start()) + 1, m.group(1), nm.group(1), reg[names[m.group(1)]]))
if used == 0: fail("no place that names a code in the sources (an empty scan is a failure)")
if faults:
    for f_ in faults: print("check-drools-codes: FAIL — " + f_, file=sys.stderr)
    sys.exit(1)
PYEOF

echo "check-drools-codes: PASS — $entries codes, numbered 001..$(printf '%03d' "$entries"), every one raised in the sources, every raised code registered, keys unique, every ef a row of ch. 5.7b, both catalogues held to the register and the places to the catalogues (floor $EXPECTED_MIN_DROOLS_MESSAGES)"
