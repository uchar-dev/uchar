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
set -u
cd "$(dirname "$0")/.."

fail() { echo "check-drools-codes: FAIL — $*" >&2; exit 1; }

REGISTER=backends/drools/src/main/java/dev/uchar/regelspraak/refusal/DroolsCode.java
SOURCES=backends/drools/src/main/java
TABLE=spec/RUNTIME-SEMANTICS.md

# monotone floor (maintainer decision 2026-09-09): a lower bound, so that a register that quietly lost entries fails
# and one that grew needs no edit here. Measured 2026-10-19 at the card drools-refusal-codes: 289 codes.
EXPECTED_MIN_DROOLS_CODES=289

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

echo "check-drools-codes: PASS — $entries codes, numbered 001..$(printf '%03d' "$entries"), every one raised in the sources, every raised code registered, keys unique, every ef a row of ch. 5.7b"
