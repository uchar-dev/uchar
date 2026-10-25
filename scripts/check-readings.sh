#!/usr/bin/env bash
# Gate 17 — the reading of a condition at the fixpoint (RUNTIME-SEMANTICS ch. 5.12; card
# ref-trace-and-readings-gate, 2026-10-21).
#
# Two executors read the same clause: the reference interpreter's trace (`runner --trace`,
# ref/lib/reading.ml) and the readings of the Drools backend (`DroolsBackendMain --readings`, card
# fixpoint-readings), each per golden case file, per form with a condition and object it runs on: the
# evaluation of the condition on the fixpoint world, recorded per node, in the canonical text of ch. 5.12.
# This gate holds them equal, on both branches of the backend. The two share no code (gate 1).
#
# Checks:
#   1. ref builds, and `runner --trace` reads every golden case file, also of a golden the backend does
#      not carry: a trace that faults or refuses fails the gate;
#   2. the backend jar is fresh or rebuilt (scripts/lib/drools-jar.sh). Per golden with case files, on
#      both branches, `--readings [--drl]` ends in `READINGS <n>`, n its number of case files, or in
#      `EXCLUDE`, and both branches end in the same line. A golden the readings exclude is one the replay
#      excludes in the same line (the plain replay is asked), so no golden the square plays escapes the
#      comparison. An `ERROR` fails, unless the golden stands in READINGS_ERROR_KNOWN below, whose replay
#      must then exclude it;
#   3. per case file of a golden both branches read, the lines of ref and of each branch are equal, by
#      the comparison rule below; a case that differs fails unless DEVIATIONS lists it for that branch,
#      and a listed case that agrees, or that is not compared, fails as stale. On every case file, listed
#      or not, the tripwire below;
#   4. the comparison rule and the tripwire pass their own rows (a comparator that finds nothing is a
#      silent green);
#   5. the counting floors below.
#
# THE COMPARISON RULE. Every line is compared verbatim, except the position of a contributor of a
# quantifier fold. Since the card fold-contributor-verdicts (2026-10-21) ch. 5.12 records the table of
# ch. 3.9a at the node of such a fold: per contributor the verdict of the matrix on it, or its fault, and
# whether the evaluation reached it, the contributors after the stop and after a fault included. The
# backend evaluates the matrix for every member in a rule of its own; where the matrix can fault or
# deposit it replays the stop rule of ch. 5.10 over the contributors in their order, and where it can do
# neither, no fault and no deposit shows the order, its session keeps none, and it writes `unordered` in
# place of the position, which ch. 5.12 admits (ch. 5.11: an invisible order). Per contributor the
# comparison therefore reads: the member and its verdict, value or fault, verbatim; the position where
# both write one. A contributor is compared by its member, never by its place in the sorted list.
#
# THE TRIPWIRE. Where the backend writes `unordered` for a fold, no contributor of ref may carry a fault: a
# matrix that faults on a member is one whose order shows, and a backend that keeps none for it has missed
# that its matrix can fault, so it would fault on a member the reference interpreter's stop never reads.
# The tripwire holds on a case in DEVIATIONS too.
#
# The traces and the readings are collected, then judged (scripts/lib/parallel.sh): first every
# `runner --trace` runs, one process per core, then every `--readings` of both branches, one process per two
# cores (replay JVMs with PARALLEL_JVM_FLAGS, measured there), then the plain replay of each golden whose
# readings end in EXCLUDE or ERROR, each call into files of its own; then the loop of checks 1 and 2 reads
# those files in the order of the corpus and judges them as the calls one after the other would. A process
# that crashes or does not start leaves an exit status other than 0 or an output without its last line,
# which the loop meets at its golden or case and fails by name.
#
# An empty comparison is a failure, never silently green. Report: goldens and cases traced, compared per
# branch, excluded (per first word of the reason), and the counts the floors hold.
set -u
cd "$(git rev-parse --show-toplevel)" || exit 2

fail() { echo "check-readings: FAIL — $*" >&2; exit 1; }

# --- the reason-carrying lists (each fails when stale, in both directions) -----------------------------
# A golden case file whose trace and readings differ, per branch: '<golden>/<case file>|<branch>|<reason>',
# branch `exec` or `drl`. Empty since the gate began (2026-10-21): on every case file both branches read,
# the two executors agree.
DEVIATIONS=(
)
# A golden whose `--readings` ends in ERROR on both branches, with the reason; its plain replay must exclude
# it. Empty since card reading-after-refusal (2026-10-21). Until then four goldens stood here,
# anchor/jutterswet-bootberging, jutterswet-ketenkwantor and jutterswet-muntketen after a DROOLS-067 refusal
# and jutterswet-havenrooster after a DROOLS-179 one: a form the transpiler refused part-way left its
# reading open, and the next form's `beginReading` refused the build with readings. `meet` closes that
# reading now, and the four are compared as `EXCLUDE`, like every golden the replay excludes.
READINGS_ERROR_KNOWN=(
)

# --- monotone floors (maintainer decision 2026-09-09) --------------------------------------------------
# Lower bounds, never equalities: they catch SHRINK — a comparison that quietly reads half — while growth
# needs no bump. Raised deliberately, at an occasion; a deliberate removal lowers the floor in the same
# commit. Accepted weakness, recorded rather than hidden: a count that stays equal while its content
# shifted passes; the lists above are what cover that direction. The counts per branch are the same
# lines, so one floor holds each of them on both.
EXPECTED_MIN_TRACED_CASES=835        # 2026-10-21: measured 835 (every golden case file, traced by ref)
EXPECTED_MIN_COMPARED_GOLDENS=261    # 2026-10-21: measured 261 (goldens both branches read)
EXPECTED_MIN_COMPARED_CASES=706      # 2026-10-21: measured 706 (their case files)
EXPECTED_MIN_READINGS=3826           # 2026-10-21: measured 3826 (status lines: (case, form, object))
EXPECTED_MIN_NODE_LINES=13971        # 2026-10-21: measured 13971 (node lines)
EXPECTED_MIN_UNREACHED=3118          # 2026-10-21: measured 3118 (node lines `unreached`)
EXPECTED_MIN_CONTRIBUTOR_LINES=314   # 2026-10-21: measured 314 (node lines with contributors)
EXPECTED_MIN_TIMELINE_LINES=76       # 2026-10-21: measured 76 (node lines with a timeline)
EXPECTED_MIN_UNREACHED_CONTRIBUTORS=106 # 2026-10-21: measured 106 (contributors of ref not reached, each with its verdict)

# --- tools ---------------------------------------------------------------------------------------------
if ! command -v dune >/dev/null 2>&1 && command -v opam >/dev/null 2>&1; then
  eval "$(opam env)" || exit 2
fi
command -v dune >/dev/null 2>&1 || fail "need dune (OCaml): the reference interpreter's trace is one side of the comparison"
command -v java >/dev/null 2>&1 || fail "need java: the Drools backend's readings are the other side"
command -v python3 >/dev/null 2>&1 || fail "need python3: the comparison rule is applied in python"

. scripts/lib/ocaml-build.sh
ensure_ocaml || fail "the OCaml build is red (the ocaml-build line above says why)"
BIN=_build/default/ref/bin/runner.exe
[ -x "$BIN" ] || fail "the runner is missing at $BIN after the build"

. scripts/lib/drools-jar.sh
JAR=$(ensure_drools_jar) || fail "no fresh Drools backend jar (the drools-jar line above says why)"

. scripts/lib/parallel.sh
work=$(mktemp -d) || exit 2
trap 'rm -rf "$work"' EXIT
mkdir "$work/ref" "$work/exec" "$work/drl" "$work/traces" "$work/readings" "$work/replays" || exit 2
manifest="$work/manifest"
: > "$manifest"

# --- 1 and 2: the trace and the readings, per golden ----------------------------------------------------
n_traced=0; nocase=''
start=$(date +%s)
# One process per core for the traces (the runner is single-threaded), one per two cores for the JVMs (a
# replay JVM with PARALLEL_JVM_FLAGS uses 1.5 cores; the measurement is in scripts/lib/parallel.sh). The tasks
# are keyed by the golden's place in the corpus order, and a trace by its case's place among the golden's.
traces=$(parallel_jobs 1) || fail "no number of processes for the traces (the parallel line above says why)"
jvms=$(parallel_jobs 2) || fail "no number of processes for the readings (the parallel line above says why)"
echo "  traces on $traces processes (one per core), readings and replays on $jvms (one per two cores)"
i=0
while IFS= read -r ir; do
  i=$((i + 1))
  cases=$(find "$(dirname "$ir")" -maxdepth 1 -name 'case*.json' | sort)
  [ -n "$cases" ] || continue
  j=0
  for c in $cases; do
    j=$((j + 1))
    parallel_task "$work/traces" "$i.$j" err "$BIN" --trace "$ir" "$c"
  done
  parallel_task "$work/readings" "exec.$i" null java $PARALLEL_JVM_FLAGS -jar "$JAR" --readings "$ir" $cases
  parallel_task "$work/readings" "drl.$i" null java $PARALLEL_JVM_FLAGS -jar "$JAR" --readings --drl "$ir" $cases
done < <(find corpus/golden -name ir.json | sort)
parallel_run "$work/traces" "$traces" || fail "a trace did not end (the parallel line above names it)"
parallel_run "$work/readings" "$jvms" || fail "a reading did not end (the parallel line above names it)"
# the plain replay that check 2 asks of a golden whose readings end in EXCLUDE or ERROR
i=0
while IFS= read -r ir; do
  i=$((i + 1))
  [ -f "$work/readings/exec.$i.out" ] || continue
  case "$(tail -1 "$work/readings/exec.$i.out")" in
    EXCLUDE*|ERROR*)
      cases=$(find "$(dirname "$ir")" -maxdepth 1 -name 'case*.json' | sort)
      parallel_task "$work/replays" "$i" null java $PARALLEL_JVM_FLAGS -jar "$JAR" "$ir" $cases ;;
  esac
done < <(find corpus/golden -name ir.json | sort)
parallel_run "$work/replays" "$jvms" || fail "a replay did not end (the parallel line above names it)"

i=0
while IFS= read -r ir; do
  i=$((i + 1))
  dir=$(dirname "$ir"); name=${dir#corpus/golden/}
  cases=$(find "$dir" -maxdepth 1 -name 'case*.json' | sort)
  if [ -z "$cases" ]; then nocase="$nocase $name"; continue; fi
  file=$(printf '%s' "$name" | tr / _).txt
  : > "$work/ref/$file"
  ncases=0
  for c in $cases; do
    ncases=$((ncases + 1))
    echo "case $(basename "$c")" >> "$work/ref/$file"
    cat "$work/traces/$i.$ncases.out" >> "$work/ref/$file"
    if [ "$(cat "$work/traces/$i.$ncases.rc")" != 0 ]; then
      fail "ref does not trace $c: $(tail -3 "$work/traces/$i.$ncases.err")"
    fi
  done
  n_traced=$((n_traced + ncases))
  mv "$work/readings/exec.$i.out" "$work/exec/$file" && mv "$work/readings/drl.$i.out" "$work/drl/$file" \
    || fail "$name: cannot move its readings into $work"
  exec_last=$(tail -1 "$work/exec/$file"); drl_last=$(tail -1 "$work/drl/$file")
  [ "$exec_last" = "$drl_last" ] \
    || fail "$name: the two branches end their readings apart — exec: '$exec_last', drl: '$drl_last'"
  case "$exec_last" in
    "READINGS $ncases") status=READ ;;
    READINGS*) fail "$name: '$exec_last', but the golden has $ncases case file(s)" ;;
    EXCLUDE*|ERROR*)
      # a golden the readings do not read is one the replay does not play, in the same line
      replay=$(tail -1 "$work/replays/$i.out")
      case "$replay" in EXCLUDE*) ;; *) fail "$name: the readings end in '$exec_last', but the replay plays it ('$replay') — a golden the square plays is read" ;; esac
      if [ "${exec_last%% *}" = EXCLUDE ]; then
        [ "$replay" = "$exec_last" ] \
          || fail "$name: the readings exclude it as '$exec_last', the replay as '$replay'"
        status="EXCLUDE ${exec_last#EXCLUDE }"
      else
        status="ERROR ${exec_last#ERROR }"
      fi ;;
    *) fail "$name: the readings end in '$exec_last' — neither READINGS nor EXCLUDE" ;;
  esac
  printf '%s\t%s\t%s\n' "$name" "$file" "$status" >> "$manifest"
done < <(find corpus/golden -name ir.json | sort)
elapsed=$(( $(date +%s) - start ))

[ "$n_traced" -gt 0 ] || fail "no golden case file was traced — an empty comparison is a failure, never silently green"
[ "$n_traced" -ge "$EXPECTED_MIN_TRACED_CASES" ] \
  || fail "ref traced $n_traced golden case file(s), but EXPECTED_MIN_TRACED_CASES pins at least $EXPECTED_MIN_TRACED_CASES — the traced corpus shrank (a deliberate removal lowers the floor in the same commit)"

printf '%s\n' "${DEVIATIONS[@]+"${DEVIATIONS[@]}"}" > "$work/deviations"
printf '%s\n' "${READINGS_ERROR_KNOWN[@]+"${READINGS_ERROR_KNOWN[@]}"}" > "$work/errors-known"

# --- 3, 4 and 5: the comparison --------------------------------------------------------------------------
python3 -I - "$work" "$EXPECTED_MIN_COMPARED_GOLDENS" "$EXPECTED_MIN_COMPARED_CASES" "$EXPECTED_MIN_READINGS" \
  "$EXPECTED_MIN_NODE_LINES" "$EXPECTED_MIN_UNREACHED" "$EXPECTED_MIN_CONTRIBUTOR_LINES" \
  "$EXPECTED_MIN_TIMELINE_LINES" "$EXPECTED_MIN_UNREACHED_CONTRIBUTORS" "$n_traced" "$elapsed" "$nocase" <<'PY'
import os, sys

work = sys.argv[1]
floors = dict(zip(['goldens', 'cases', 'readings', 'nodes', 'unreached', 'contributors', 'timelines',
                   'unreached_contributors'], map(int, sys.argv[2:10])))
n_traced, elapsed, nocase = int(sys.argv[10]), sys.argv[11], sys.argv[12].split()
problems = []

# --- the canonical text, read back (RUNTIME-SEMANTICS ch. 5.12) ---
def scan_object(s, i):
    """the end of the object name at s[i]: a quoted label, «"creation"»(object)#k, or empty"""
    if s[i] == '"':
        j = i + 1
        while s[j] != '"':
            j += 2 if s[j] == '\\' else 1
        return j + 1
    if s[i] == '«':
        j = scan_object(s, i + 1)
        if s[j:j + 2] != '»(':
            raise ValueError('no »( after the creation in %r' % s)
        j = scan_object(s, j + 2)
        if s[j:j + 2] != ')#':
            raise ValueError('no )# after the subject in %r' % s)
        j += 2
        while j < len(s) and s[j].isdigit():
            j += 1
        return j
    if s.startswith('empty', i):
        return i + 5
    raise ValueError('no object at %d in %r' % (i, s))

def scan_value(s, i):
    """the end of a contributor's value at s[i]: a quoted text, `fault <code>`, `-` or a token"""
    if s[i] == '"':
        return scan_object(s, i)
    if s.startswith('fault ', i):
        i += 6
    j, depth = i, 0
    while j < len(s) and (depth > 0 or s[j] not in ' ;]'):
        depth += {'[': 1, ']': -1}.get(s[j], 0)
        j += 1
    return j

def split(line):
    """the line without its contributors, and the contributors by member: {member: (value, reached)}"""
    k = line.find(' contributors [')
    if k < 0:
        return line, None
    body = line[k + len(' contributors ['):]
    if not body.endswith(']'):
        raise ValueError('contributors without their ] in %r' % line)
    body, out, i = body[:-1], {}, 0
    while i < len(body):
        j = scan_object(body, i)
        member = body[i:j]
        v = scan_value(body, j + 1)
        r = body.find(';', v)
        r = len(body) if r < 0 else r
        if member in out:
            raise ValueError('the member %s twice in %r' % (member, line))
        out[member] = (body[j + 1:v], body[v + 1:r])
        i = r + 2
    return line[:k], out

def contributors_agree(ref, drools):
    """the members, and per member its verdict, value or fault verbatim and its position where both write
    one (the backend writes `unordered` where its session keeps no order)"""
    if set(ref) != set(drools):
        return False
    for m, (dv, dr) in drools.items():
        rv, rr = ref[m]
        if rv != dv:
            return False
        if dr != 'unordered' and rr != dr:
            return False
    return True

def differences(ref_lines, drools_lines):
    """(lines only ref has, lines only the backend has), by the comparison rule"""
    def keyed(lines):
        d = {}
        for l in lines:
            h, c = split(l)
            d.setdefault(h, []).append((l, c))
        return d
    r, d = keyed(ref_lines), keyed(drools_lines)
    only_r, only_d = [], []
    for h in sorted(set(r) | set(d)):
        rs, ds = r.get(h, []), d.get(h, [])
        if len(rs) == 1 and len(ds) == 1:
            (rl, rc), (dl, dc) = rs[0], ds[0]
            if (rc is None and dc is None) or (rc is not None and dc is not None and contributors_agree(rc, dc)):
                continue
        only_r += [l for l, _ in rs]
        only_d += [l for l, _ in ds]
    return only_r, only_d

def unordered_faults(ref_lines, drools_lines):
    """the tripwire: the lines of ref with a contributor that carries a fault, at a node for which the
    backend writes `unordered`"""
    unordered = set()
    for l in drools_lines:
        h, c = split(l)
        if c is not None and any(r == 'unordered' for _, r in c.values()):
            unordered.add(h)
    out = []
    for l in ref_lines:
        h, c = split(l)
        if h in unordered and any(v.startswith('fault ') for v, _ in c.values()):
            out.append(l)
    return out

# --- 4: the comparison rule's own rows ---
P = '/rules/rules/0/versions/0 "P1" '
rows = [
    ('equal lines agree', [P + '0 Compound false'], [P + '0 Compound false'], True),
    ('a value apart differs', [P + '2 AttributeReference 4'], [P + '2 AttributeReference 5'], False),
    ('a line on one side differs', [P + 'HOLDS', P + '0 Compound true'], [P + 'HOLDS'], False),
    ('reached against unreached differs',
     [P + '0 QuantifierFold false contributors ["W1" false reached; "W2" false unreached]'],
     [P + '0 QuantifierFold false contributors ["W1" false reached; "W2" false reached]'], False),
    ('a verdict after the stop apart differs',
     [P + '0 QuantifierFold false contributors ["W1" false reached; "W2" false unreached]'],
     [P + '0 QuantifierFold false contributors ["W1" false reached; "W2" true unreached]'], False),
    ('a fault after the stop against a verdict differs',
     [P + '0 QuantifierFold false contributors ["W1" false reached; "W2" fault EF-019 unreached]'],
     [P + '0 QuantifierFold false contributors ["W1" false reached; "W2" false unreached]'], False),
    ('an equal fault after the stop agrees',
     [P + '0 QuantifierFold false contributors ["W1" false reached; "W2" fault EF-019 unreached]'],
     [P + '0 QuantifierFold false contributors ["W1" false reached; "W2" fault EF-019 unreached]'], True),
    ('unordered reads the verdict',
     [P + '0 QuantifierFold false contributors ["W1" true reached; "W2" false unreached]'],
     [P + '0 QuantifierFold false contributors ["W1" true unordered; "W2" true unordered]'], False),
    ('unordered with equal verdicts agrees',
     [P + '0 QuantifierFold false contributors ["W1" false reached; "W2" true unreached]'],
     [P + '0 QuantifierFold false contributors ["W1" false unordered; "W2" true unordered]'], True),
    ('a member apart differs',
     [P + '1 Aggregation 1 contributors ["V1" empty reached]'],
     [P + '1 Aggregation 1 contributors ["V2" empty reached]'], False),
    ('a value of a value fold apart differs',
     [P + '1 Aggregation 7 contributors ["V1" 6 reached; "V2" 1 reached]'],
     [P + '1 Aggregation 7 contributors ["V1" 6 reached; "V2" 2 reached]'], False),
    ('a made object is one member',
     [P + '1 Aggregation 1 contributors [«"maak; een"»("J1")#1 empty reached]'],
     [P + '1 Aggregation 1 contributors [«"maak; een"»("J1")#1 empty reached]'], True),
]
for name, ref, drools, agree in rows:
    o_r, o_d = differences(ref, drools)
    if (not o_r and not o_d) != agree:
        problems.append('the comparison rule fails its row "%s": it says %s' % (name, 'agree' if agree else 'differ'))
tripwire_rows = [
    ('a fault of ref under unordered trips, equal texts or not',
     [P + '0 QuantifierFold false contributors ["W1" false reached; "W2" fault EF-019 unreached]'],
     [P + '0 QuantifierFold false contributors ["W1" false unordered; "W2" fault EF-019 unordered]'], True),
    ('verdicts under unordered do not trip',
     [P + '0 QuantifierFold false contributors ["W1" false reached; "W2" true unreached]'],
     [P + '0 QuantifierFold false contributors ["W1" false unordered; "W2" true unordered]'], False),
    ('a fault of ref where the backend keeps the order does not trip',
     [P + '0 QuantifierFold false contributors ["W1" false reached; "W2" fault EF-019 unreached]'],
     [P + '0 QuantifierFold false contributors ["W1" false reached; "W2" fault EF-019 unreached]'], False),
]
for name, ref, drools, trips in tripwire_rows:
    if bool(unordered_faults(ref, drools)) != trips:
        problems.append('the tripwire fails its row "%s": it says %s' % (name, 'trip' if trips else 'pass'))
if problems:
    print('\n'.join('check-readings: FAIL — ' + p for p in problems), file=sys.stderr)
    sys.exit(1)

# --- the lists ---
deviations = {}
for l in open(os.path.join(work, 'deviations'), encoding='utf-8'):
    l = l.rstrip('\n')
    if not l:
        continue
    parts = l.split('|', 2)
    if len(parts) != 3 or parts[1] not in ('exec', 'drl') or not parts[2].strip():
        problems.append('malformed DEVIATIONS entry %r — each is "<golden>/<case file>|exec or drl|<reason>"' % l)
        continue
    deviations[(parts[0], parts[1])] = parts[2]
errors_known = {}
for l in open(os.path.join(work, 'errors-known'), encoding='utf-8'):
    l = l.rstrip('\n')
    if not l:
        continue
    name, _, why = l.partition('|')
    if not why.strip():
        problems.append('READINGS_ERROR_KNOWN entry without a reason: %r' % l)
    errors_known[name] = why

def blocks(path):
    """case file -> its reading lines, from the output of `--readings` or of the trace"""
    out, current = {}, None
    for line in open(path, encoding='utf-8'):
        line = line.rstrip('\n')
        if line.startswith('case '):
            current = line[5:]
            if current in out:
                raise ValueError('the case %s twice in %s' % (current, path))
            out[current] = []
        elif line.startswith('reading '):
            if current is None:
                raise ValueError('a reading before the first case in %s' % path)
            out[current].append(line[8:])
        elif line.startswith('READINGS '):
            pass
        else:
            raise ValueError('a line the canonical text does not write, in %s: %r' % (path, line))
    return out

counts = dict.fromkeys(floors, 0)
excluded, seen_errors, seen_deviations, manifest_names = {}, set(), set(), set()
for row in open(os.path.join(work, 'manifest'), encoding='utf-8'):
    name, file, status = row.rstrip('\n').split('\t')
    manifest_names.add(name)
    if status.startswith('ERROR'):
        seen_errors.add(name)
        if name not in errors_known:
            problems.append('%s: the readings end in %s — not listed in READINGS_ERROR_KNOWN' % (name, status))
        continue
    if name in errors_known:
        problems.append('stale READINGS_ERROR_KNOWN entry: %s ends in %s now — remove it' % (name, status.split(':')[0]))
    if status.startswith('EXCLUDE'):
        reason = status[len('EXCLUDE '):].split(':')[0]
        excluded[reason] = excluded.get(reason, 0) + 1
        continue
    ref = blocks(os.path.join(work, 'ref', file))
    counts['goldens'] += 1
    for branch in ('exec', 'drl'):
        drools = blocks(os.path.join(work, branch, file))
        if set(drools) != set(ref):
            problems.append('%s (%s): the trace reads the cases %s, the backend %s' % (name, branch, sorted(ref), sorted(drools)))
            continue
        for case in sorted(drools):
            key = (name + '/' + case, branch)
            tripped = unordered_faults(ref[case], drools[case])
            if tripped:
                problems.append('%s/%s (%s): the backend writes no order for a fold at which ref has a contributor with a '
                                'fault — its session would fault on a member the stop of ch. 5.10 does not reach:\n%s' % (
                                    name, case, branch, '\n'.join('      ref     ' + l for l in tripped[:6])))
            o_r, o_d = differences(ref[case], drools[case])
            if o_r or o_d:
                if key in deviations:
                    seen_deviations.add(key)
                    continue
                problems.append('%s/%s (%s): the trace and the readings differ:\n%s' % (
                    name, case, branch, '\n'.join(['      ref     ' + l for l in o_r[:6]] + ['      backend ' + l for l in o_d[:6]]
                                                  + (['      … %d and %d lines' % (len(o_r), len(o_d))] if len(o_r) > 6 or len(o_d) > 6 else []))))
                continue
            if key in deviations:
                problems.append('stale DEVIATIONS entry: %s (%s) agrees now — remove it' % key)
                seen_deviations.add(key)
            if branch == 'exec':
                counts['cases'] += 1
                for l in ref[case]:
                    _, c = split(l)
                    if c is not None:
                        counts['unreached_contributors'] += sum(1 for _, r in c.values() if r == 'unreached')
                for l in drools[case]:
                    head, c = split(l)
                    parts = head.split(' ')
                    if parts[-1] in ('HOLDS', 'FAILS', 'FAULTS', 'DISPLACED', 'OUTSIDE_VALIDITY'):
                        counts['readings'] += 1
                        continue
                    counts['nodes'] += 1
                    if head.endswith(' unreached'):
                        counts['unreached'] += 1
                    if c is not None:
                        counts['contributors'] += 1
                    if ' [[' in head or head.endswith(' []'):
                        counts['timelines'] += 1
for key in deviations:
    if key not in seen_deviations:
        problems.append('stale DEVIATIONS entry: %s (%s) is not compared — remove it' % key)
for name in errors_known:
    if name not in manifest_names:
        problems.append('stale READINGS_ERROR_KNOWN entry: %s is no golden with case files — remove it' % name)

if counts['cases'] == 0:
    problems.append('no case file was compared — an empty comparison is a failure, never silently green')
names = {'goldens': 'EXPECTED_MIN_COMPARED_GOLDENS', 'cases': 'EXPECTED_MIN_COMPARED_CASES', 'readings': 'EXPECTED_MIN_READINGS',
         'nodes': 'EXPECTED_MIN_NODE_LINES', 'unreached': 'EXPECTED_MIN_UNREACHED',
         'contributors': 'EXPECTED_MIN_CONTRIBUTOR_LINES', 'timelines': 'EXPECTED_MIN_TIMELINE_LINES',
         'unreached_contributors': 'EXPECTED_MIN_UNREACHED_CONTRIBUTORS'}
for k, floor in floors.items():
    if counts[k] < floor:
        problems.append('%d %s compared, but %s pins at least %d — the comparison shrank (a deliberate removal lowers the floor in the same commit)' % (counts[k], k.replace('_', ' '), names[k], floor))

print('  counted: ref traced %d golden case files; compared per branch: %d goldens, %d case files, %d readings, '
      '%d node lines, %d unreached, %d with contributors, %d with a timeline; %d contributors of ref not reached' % (
          n_traced, counts['goldens'], counts['cases'], counts['readings'], counts['nodes'], counts['unreached'],
          counts['contributors'], counts['timelines'], counts['unreached_contributors']))
for k in sorted(excluded):
    print('  excluded by the backend: %3d golden(s) %s' % (excluded[k], k))
for name in sorted(seen_errors):
    print('  readings ERROR, listed:   %s — %s' % (name, errors_known.get(name, '')))
if nocase:
    print('  no case file, not read:  ' + ' '.join(nocase))
if problems:
    print('\n'.join('check-readings: FAIL — ' + p for p in problems), file=sys.stderr)
    sys.exit(1)
print('check-readings: PASS (ref traced %d golden case files; on both branches %d goldens and %d case files compared, '
      '%d readings, %d node lines of which %d unreached, %d with contributors and %d with a timeline, %d contributors '
      'of ref not reached, all at or above their floors; deviations %d; %d comparison rows, %d tripwire rows; %s s)' % (
          n_traced, counts['goldens'], counts['cases'], counts['readings'], counts['nodes'], counts['unreached'],
          counts['contributors'], counts['timelines'], counts['unreached_contributors'], len(deviations), len(rows),
          len(tripwire_rows), elapsed))
PY
