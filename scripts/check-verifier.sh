#!/bin/sh
# Gate 3: the IR verifier (spec/verifier/, OCaml).
#
# Three checks:
#   1. the verifier builds (the OCaml workspace, scripts/lib/ocaml-build.sh);
#   2. every corpus/golden/**/ir.json is verifier-clean (exit 0);
#   3. every corpus/negative/<case>/ir.json is rejected (non-zero exit) AND
#      the verifier reports the WF rule the case declares in its directory
#      name (convention: wf###-<slug>, e.g. wf001-duplicate-parameter).
#      Additional incidental violations are allowed; a case that passes, or
#      fails only for other rules, turns the gate red. A negative corpus
#      without cases is reported explicitly, never silently green. A case
#      directory that carries an expect.txt must also be rejected on the ARM
#      that file names. Every violation carries a key `WF-###.<slug>` (the
#      arm, MESSAGE-CODES §2.2) and named parameters; the report line is
#      `WF-###.<slug> FAIL <pointer>: <message>`, and `verifier --arms` prints
#      key, pointer and parameters as JSON for the machine. The first line of
#      expect.txt is the key, of the rule the directory name declares; each
#      further line is `name=value`, and the verifier must report that key
#      with every such pair among the parameters of ONE violation (a subset
#      match, values byte for byte). An expect.txt in the old form (a message
#      fragment), a blank line, a duplicate line or an empty file fails, so a
#      rewording of a message breaks no pin and a changed key or value does.
#      Two more checks run over all negative reports: one key stands for ONE
#      message template (the message with each parameter value replaced by a
#      placeholder and every number by '#'; a key with two templates is
#      refused), and in checks.ml no site builds a violation without a key.
#   4. every JSON pointer a negative report cites resolves in that case's
#      document (RFC 6901): a report that sends its reader to a position
#      the document does not have is refused, whatever its rule and arm.
#      The pointer is read up to the first ": ", spaces included, and a
#      FAIL line without that separator is refused as well.
#
# With explicit file arguments it runs the verifier against those files and
# prints the reports (handy for a dump outside the repo).
set -u
cd "$(git rev-parse --show-toplevel)" || exit 2

VERIFIER_DIR='spec/verifier'
if [ ! -d "$VERIFIER_DIR" ]; then
    echo "FAIL: verifier not found at $VERIFIER_DIR" >&2
    exit 2
fi

# Locate dune; fall back to the opam environment when it is not on PATH.
if ! command -v dune >/dev/null 2>&1; then
    if command -v opam >/dev/null 2>&1; then
        eval "$(opam env)" || exit 2
    fi
fi
if ! command -v dune >/dev/null 2>&1; then
    echo 'FAIL: need dune (OCaml). Install via opam: opam install dune yojson' >&2
    exit 2
fi
command -v python3 >/dev/null 2>&1 || { echo 'FAIL: need python3 (the pointer check)' >&2; exit 2; }

# check 1: build. The verifier is a project of the dune workspace; ensure_ocaml builds the workspace
# when a source is newer than its last green build, and leaves dune alone otherwise.
. scripts/lib/ocaml-build.sh
if ! ensure_ocaml; then
    echo 'check-verifier: FAIL (build)' >&2
    exit 1
fi
BIN="_build/default/$VERIFIER_DIR/bin/verifier.exe"

if [ "$#" -gt 0 ]; then
    status=0
    for f in "$@"; do
        echo "== $f"
        "$BIN" "$f" || status=1
    done
    exit "$status"
fi

status=0

# --- monotone floor (maintainer decision 2026-09-09) ---------------------------------------------
# A lower bound, never an equality: it catches SHRINK — the negative corpus that quietly halves —
# while growth needs no bump, so an ordinary corpus card touches no protected script. Raised
# deliberately, at an occasion. Accepted weakness, recorded rather than hidden: a count that stays
# equal while its content shifted passes; the name-carrying lists are what cover that direction. A
# deliberate removal lowers the floor in the same commit.
# The negative cases are floored HERE because this is the only gate that sees them at all: a
# vanished wf###-case takes a MUST's failing witness with it (CLAUDE.md, Spec discipline) and no
# other check would notice. NO floor on n_golden: that is byte-for-byte the file set gate 2 already
# floors (both scan corpus/golden + corpus/red for ir.json), and one quantity wants one floor.
EXPECTED_MIN_NEGATIVE=350   # 2026-09-10: measured 350 (corpus/negative/**/ir.json)
# The arm fragments (U1) are floored for the same reason and in the same regime: a vanished
# expect.txt takes the arm pin of a rewritten witness with it, and its directory keeps passing on
# the rule number alone. Measured at the L2 landing: every rewritten or replaced witness of the
# reference layer carries one. Raised in the L2 fix round (84 -> 91): the restored WF-046 witness,
# the WF-102 (c) witness whose message L2 changed, and five witnesses added after the lens. Raised
# at L3 (91 -> 159): every rewritten witness of the type-system stamp list and every date witness
# whose rule reads the precision pins its arm (62), and so do the six witnesses of the new arms.
# Raised in the L3 fix round (159 -> 165): the five witnesses the lens asked for (two new arms,
# WF-009 and WF-005; the abbreviation class of WF-017 (a); WF-001 in the two namespaces whose
# message label moved) and the WF-001 witness that had no pin.
# Raised at L4 (165 -> 220): every rewritten witness of the rule- and expression-layer stamp list
# pins its arm (52 new pins, 15 re-pinned where the message moved), and so do the seven witnesses of
# the new arms; the four pins of the removed witnesses left with them.
# Raised in the L4 fix round (220 -> 228): the five witnesses the lens asked for — two arms whose
# witness left with the L4 removals (WF-037's granularity facet on a non-bucketing kind, WF-047's
# range on a node that is no instance fold) and three that never had one (WF-030's source type and
# key resolution, WF-037's bucketing builtin without a granularity) — and three more WF-037 arms the
# fix round found without one (the two builtin arities, a value fold with fewer than two operands).
EXPECTED_MIN_EXPECT=354     # 2026-10-19: measured 354 (corpus/negative/*/expect.txt), card verifier-arm-keys; before that 260 (2026-09-25, L5)
# The arm keys and their parameter lines (card verifier-arm-keys, 2026-10-19), floored for the same
# reason: a pin that quietly lost its `name=value` lines would still pass on its key.
EXPECTED_MIN_ARMS=213         # 2026-10-19: measured 213 distinct keys named by the expect.txt files
EXPECTED_MIN_PARAM_LINES=572  # 2026-10-19: measured 572 `name=value` lines over the expect.txt files
# The key and template check reads every negative report; a parse that stopped finding keys would
# check nothing. The static count is the key literals in checks.ml (two sites may share one).
EXPECTED_MIN_REPORT_KEYS=318  # 2026-10-19: measured 318 distinct keys over the negative reports
EXPECTED_MIN_ARM_SITES=421    # 2026-10-19: measured 421 key literals "WF-###.<slug>" in spec/verifier/lib/checks.ml
# The cited pointers (check 4) are floored in the same regime: a parse that stopped finding the
# pointer in a report line would check nothing and pass.
EXPECTED_MIN_POINTERS=619   # 2026-10-19: measured 619 pointers over the negative reports, all resolving (547 on 2026-09-25)

# check 2: golden dumps — and the red corpus's documents, whose faults
# are behavioural, never well-formedness — are verifier-clean.
list=$(mktemp) || exit 2
ptrs=$(mktemp) || exit 2
noptr=$(mktemp) || exit 2
trap 'rm -f "$list" "$ptrs" "$noptr"' EXIT

n_golden=0
find corpus/golden corpus/red -name ir.json 2>/dev/null | sort > "$list"
while IFS= read -r f; do
    n_golden=$((n_golden + 1))
    if ! out=$("$BIN" "$f" 2>&1); then
        printf 'FAIL: document %s is not verifier-clean:\n%s\n' "$f" "$out" >&2
        status=1
    fi
done < "$list"

# check 3: negative cases are rejected for the declared rule, and on the
# declared arm where the case names one (expect.txt).
n_neg=0
find corpus/negative -name ir.json 2>/dev/null | sort > "$list"
while IFS= read -r f; do
    n_neg=$((n_neg + 1))
    dir=$(basename "$(dirname "$f")")
    id=$(printf '%s' "$dir" | sed -n 's/^wf0*\([0-9][0-9]*\)-.*/\1/p')
    if [ -z "$id" ]; then
        echo "FAIL: negative case directory '$dir' does not follow wf###-<slug>" >&2
        status=1
        continue
    fi
    expect=$(printf 'WF-%03d' "$id")
    rejected=0
    if out=$("$BIN" "$f" 2>&1); then
        printf 'FAIL: negative case %s passed the verifier (must violate %s):\n%s\n' \
            "$f" "$expect" "$out" >&2
        status=1
    elif ! printf '%s\n' "$out" | grep -q "$expect"; then
        printf 'FAIL: negative case %s rejected, but not for %s:\n%s\n' \
            "$f" "$expect" "$out" >&2
        status=1
    else
        rejected=1
    fi
    # check 4's input: every pointer this report cites, beside the document it must resolve in. The
    # pointer is the text between "FAIL " and the FIRST ": ", spaces included: a pattern that stopped
    # at a space skipped such a line in silence, and the floor below left room for it. A FAIL line
    # without that separator cites no pointer anyone can read, and is refused after the loop.
    printf '%s\n' "$out" | awk -v f="$f" -v noptr="$noptr" '/^WF-[0-9]+(\.[a-z0-9-]+)? FAIL / {
        rest = $0; sub(/^WF-[0-9]+(\.[a-z0-9-]+)? FAIL /, "", rest); i = index(rest, ": ")
        if (i == 0) print f "\t" $0 >> noptr; else print f "\t" substr(rest, 1, i - 1) }' >> "$ptrs"
done < "$list"

# check 4: every FAIL line cites a readable pointer, and the cited pointers resolve (RFC 6901, ~1
# and ~0 unescaped) in the document they cite.
# Until 2026-09-25 no check read them: a verifier that cited a field by its name under a segment the
# document does not have passed gate 3 on rule and arm alone (measured on a copy with the WF-092
# pointers of the L5 base reverted).
if [ -s "$noptr" ]; then
    tab=$(printf '\t')
    while IFS="$tab" read -r f line; do
        printf 'FAIL: negative case %s reports a line with no readable pointer (no ": " after "FAIL "): %s\n' "$f" "$line" >&2
    done < "$noptr"
    status=1
fi
n_ptr=$(python3 - "$ptrs" <<'PY'
import json, sys
docs, n, bad = {}, 0, []
for line in open(sys.argv[1], encoding="utf-8"):
    f, ptr = line.rstrip("\n").split("\t", 1)
    if f not in docs:
        docs[f] = json.load(open(f, encoding="utf-8"))
    cur, ok = docs[f], ptr == "" or ptr.startswith("/")
    for raw in (ptr[1:].split("/") if ok and ptr else []):
        seg = raw.replace("~1", "/").replace("~0", "~")
        if isinstance(cur, list) and seg.isdigit() and int(seg) < len(cur):
            cur = cur[int(seg)]
        elif isinstance(cur, dict) and seg in cur:
            cur = cur[seg]
        else:
            ok = False
            break
    n += 1
    if not ok:
        bad.append((f, ptr))
for f, ptr in bad:
    sys.stderr.write("FAIL: negative case %s cites %s, which does not resolve in the document: the "
                     "report sends its reader to a position the document does not have\n" % (f, ptr))
print(n)
sys.exit(1 if bad else 0)
PY
) || status=1
if [ "${n_ptr:-0}" -lt "$EXPECTED_MIN_POINTERS" ]; then
    echo "FAIL: ${n_ptr:-0} cited pointer(s) found in the negative reports, but EXPECTED_MIN_POINTERS pins at least $EXPECTED_MIN_POINTERS — the pointer check read fewer than it did (a deliberate removal lowers the floor in the same commit)" >&2
    status=1
fi

# check 5 (card verifier-arm-keys, 2026-10-19): the arm keys. Over every negative report: (a) each
# expect.txt is a key line plus `name=value` lines, the key is of the rule its directory declares,
# and the verifier reports that key with every pair among the parameters of one violation; (b) one
# key stands for one message template across all reports; (c) checks.ml builds no violation
# without a key, and its slugs are at most three words. The numbers are floored below.
CHECKS_ML="$VERIFIER_DIR/lib/checks.ml"
counts=$(python3 - "$BIN" "$list" "$CHECKS_ML" <<'PY'
import json, os, re, subprocess, sys
exe, listfile, checks = sys.argv[1:4]
docs = [l.rstrip("\n") for l in open(listfile, encoding="utf-8") if l.strip()]
bad = []
def fail(msg):
    bad.append(msg)
KEY = re.compile(r"^WF-[0-9]{3}\.[a-z][a-z0-9]*(-[a-z0-9]+){0,2}$")
PAIR = re.compile(r"^[a-z][A-Za-z0-9]*=.+$")
templates, templates_b, n_expect, n_params, pinned = {}, {}, 0, 0, set()
for f in docs:
    d = os.path.dirname(f)
    m = re.match(r"wf0*([0-9]+)-", os.path.basename(d))
    if not m:
        continue  # the directory-name failure is reported by check 3
    rule = "WF-%03d" % int(m.group(1))
    report = subprocess.run([exe, f], capture_output=True, text=True).stdout.split("\n")
    armed = subprocess.run([exe, "--arms", f], capture_output=True, text=True).stdout.split("\n")
    report = [l for l in report if " FAIL " in l]
    armed = [l for l in armed if l]
    if len(report) != len(armed):
        fail("%s: %d report lines but %d --arms lines" % (f, len(report), len(armed)))
        continue
    viol = []
    for line, arm in zip(report, armed):
        key, at, js = arm.split("\t", 2)
        params = json.loads(js, object_pairs_hook=lambda p: p)
        mm = re.match(r"(\S+) FAIL (.*?): (.*)$", line, re.S)
        if mm is None or mm.group(1) != key:
            fail("%s: report line and --arms line disagree on the key: %s / %s" % (f, line, key))
            continue
        viol.append((key, params))
        text = mm.group(3)
        # two normalisations of the template: every occurrence of a value, and the first occurrence
        # of each value in parameter order; a key is one template when either agrees, because a
        # value that coincides with a word of the sentence defeats one of them, never both
        a, b, cur = text, text, 0
        for _, v in sorted(params, key=lambda x: -len(x[1])):
            if v and not v.isdigit():
                a = re.sub(r"(?<![A-Za-z0-9_])" + re.escape(v) + r"(?![A-Za-z0-9_])", "{}", a)
        for _, v in params:
            if v and not v.isdigit():
                pat = re.compile(r"(?<![A-Za-z0-9_])" + re.escape(v) + r"(?![A-Za-z0-9_])")
                hit = pat.search(b, cur) or pat.search(b)
                if hit:
                    b = b[:hit.start()] + "{}" + b[hit.end():]
                    cur = hit.start() + 2
        templates.setdefault(key, {}).setdefault(re.sub(r"[0-9]+", "#", a), f)
        templates_b.setdefault(key, {}).setdefault(re.sub(r"[0-9]+", "#", b), f)
    arm = os.path.join(d, "expect.txt")
    if not os.path.isfile(arm):
        continue
    n_expect += 1
    raw = open(arm, encoding="utf-8").read()
    lines = raw.split("\n")
    if lines and lines[-1] == "":
        lines.pop()
    if not lines or not any(x.strip() for x in lines):
        fail("%s is empty or blank: it pins no arm" % arm); continue
    if any(not x.strip() for x in lines):
        fail("%s has a blank line" % arm); continue
    if not KEY.match(lines[0]):
        fail("%s: the first line must be an arm key WF-###.<slug> (a message fragment is the old form): %r" % (arm, lines[0])); continue
    if lines[0][:6] != rule:
        fail("%s pins %s, but the directory declares %s" % (arm, lines[0], rule)); continue
    if len(set(lines)) != len(lines):
        fail("%s has a duplicate line" % arm); continue
    wanted = []
    for x in lines[1:]:
        if not PAIR.match(x):
            fail("%s: %r is not name=value" % (arm, x)); break
        wanted.append(tuple(x.split("=", 1)))
    else:
        n_params += len(wanted)
        pinned.add(lines[0])
        mine = [p for k, p in viol if k == lines[0]]
        if not any(all(w in p for w in wanted) for p in mine):
            fail("negative case %s rejected, but not on the arm %s names: key %s with %s expected, the %s keys reported are %s%s"
                 % (f, arm, lines[0], wanted, rule, sorted({k for k, _ in viol if k[:6] == rule}),
                    "" if not mine else "; the parameters of that key are %s" % [dict(p) for p in mine]))
for key, ts in sorted(templates.items()):
    if len(ts) > 1 and len(templates_b[key]) > 1:
        fail("key %s stands for %d message templates (a key is one template; split it):\n    %s"
             % (key, len(templates_b[key]), "\n    ".join("%s  [%s]" % (t, g) for t, g in templates_b[key].items())))
src = open(checks, encoding="utf-8").read()
# comments hold no sites
src = re.sub(r"\(\*.*?\*\)", lambda m: " " * len(m.group(0)), src, flags=re.S)
for mm in re.finditer(r'\bv\s+(?:~params:\[[^\]]*\]\s+)?"(WF-[0-9]+)"', src):
    fail("%s:%d: a violation built with the bare rule %s: it needs a key WF-###.<slug>" % (checks, src.count("\n", 0, mm.start()) + 1, mm.group(1)))
sites = re.findall(r'"(WF-[0-9]{3})\.([a-z0-9]+(?:-[a-z0-9]+)*)"', src)
for rule_, slug in sites:
    if not re.fullmatch(r"[a-z][a-z0-9]*(-[a-z0-9]+){0,2}", slug):
        fail("%s: slug %r of %s is not kebab-case of at most three words" % (checks, slug, rule_))
for x in bad:
    sys.stderr.write("FAIL: " + x + "\n")
print(n_expect, n_params, len(pinned), len(templates), len(sites), 1 if bad else 0)
PY
) || { echo "FAIL: the arm check could not run" >&2; exit 1; }
set -- $counts
n_expect=$1; n_params=$2; n_arms=$3; n_keys=$4; n_sites=$5
[ "$6" -eq 0 ] || status=1
for floor in "n_arms:EXPECTED_MIN_ARMS:arm keys named by expect.txt files" "n_params:EXPECTED_MIN_PARAM_LINES:name=value lines" "n_keys:EXPECTED_MIN_REPORT_KEYS:keys in the negative reports" "n_sites:EXPECTED_MIN_ARM_SITES:key literals in checks.ml"; do
    var=${floor%%:*}; rest=${floor#*:}; name=${rest%%:*}; what=${rest#*:}
    eval "have=\$$var; min=\$$name"
    if [ "$have" -lt "$min" ]; then
        echo "FAIL: $have $what found, but $name pins at least $min — the arm check read fewer than it did (a deliberate removal lowers the floor in the same commit)" >&2
        status=1
    fi
done

# The floor is evaluated BEFORE the unpopulated-corpus exit, or that exit would answer the very
# case the floor exists for: with the zones moved aside both counts are 0 and an "all zero -> exit
# 0" branch placed first never lets the floor run. Measured 2026-09-10, before this order: both
# zones aside gave PASS at 0. The floor also precedes the no-negative check, so the zero case is
# reported with BOTH numbers rather than by the weaker non-empty message.
if [ "$n_neg" -lt "$EXPECTED_MIN_NEGATIVE" ]; then
    echo "FAIL: $n_neg negative case(s) found, but EXPECTED_MIN_NEGATIVE pins at least $EXPECTED_MIN_NEGATIVE — the negative corpus shrank (a deliberate removal lowers the floor in the same commit)" >&2
    status=1
elif [ "$n_golden" -eq 0 ] && [ "$n_neg" -eq 0 ]; then
    # Only reachable while no floor is pinned yet (floor 0) — the genuinely empty corpus.
    echo 'check-verifier: build OK; 0 corpus files (corpus not yet populated)'
    exit 0
elif [ "$n_neg" -eq 0 ]; then
    echo 'FAIL: no negative cases — every implemented WF check needs a failing case (spec discipline)' >&2
    status=1
fi
if [ "$n_expect" -lt "$EXPECTED_MIN_EXPECT" ]; then
    echo "FAIL: $n_expect arm fragment(s) (expect.txt) found, but EXPECTED_MIN_EXPECT pins at least $EXPECTED_MIN_EXPECT — arm pins vanished (a deliberate removal lowers the floor in the same commit)" >&2
    status=1
fi

if [ "$status" -eq 0 ]; then
    echo "check-verifier: PASS ($n_golden golden clean; $n_neg negative rejected for their declared rule, floor $EXPECTED_MIN_NEGATIVE; $n_expect on their declared arm (expect.txt), floor $EXPECTED_MIN_EXPECT, $n_arms arm keys, floor $EXPECTED_MIN_ARMS, $n_params name=value lines, floor $EXPECTED_MIN_PARAM_LINES; $n_keys keys of one template each in the reports, floor $EXPECTED_MIN_REPORT_KEYS, $n_sites key literals in checks.ml, floor $EXPECTED_MIN_ARM_SITES; $n_ptr cited pointers resolve in their documents, floor $EXPECTED_MIN_POINTERS)"
fi
exit "$status"
