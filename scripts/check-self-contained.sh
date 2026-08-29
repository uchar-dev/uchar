#!/bin/sh
# Gate 1: self-containedness (WAY-OF-WORKING.md section 6).
#
# Artifact zones must carry their full meaning without access to the
# maintainer's machine or private repositories: no machine-local paths, no
# private-repo names, no citations into private code. Corpus data must be
# free of TOKA material (rights policy, corpus/README.md).
#
# The exception list covers placeholder READMEs in still-empty zones; it only
# shrinks — sweeping a README clean and dropping its entry is part of the
# zone's first real delivery. The list is empty since ref/ graduated with
# its first real delivery. The normative documents spec/IR-SPEC.md and
# spec/RUNTIME-SEMANTICS.md graduated into ZONES at the Phase-1 exit
# (2026-08-19): they must read stand-alone.
set -u
cd "$(git rev-parse --show-toplevel)" || exit 2

ZONES='corpus/golden corpus/negatief corpus/rood spec/schema spec/verifier ref backends frontends spec/IR-SPEC.md spec/RUNTIME-SEMANTICS.md'
EXCEPTIONS=''
# 'main-spec' is anchored on a leading non-letter so it flags a citation into the
# private RegelSpraak spec markdown (e.g. "(main-spec §12, L4633)") without matching
# innocuous words like "domain-specific".
# NB: 'regelspraakruntime' left the general private-token set when the Drools backend
# landed (decision V3-a): the production RegelSpraakRuntime now lives — legitimately and
# by design — under backends/drools/runtime/. The no-share tripwire below re-imposes the
# invariant precisely, forbidding that runtime ONLY where it must never be (ref/, spec/verifier/).
PRIVATE='(/users/|/home/|~/|regelspraak-java|calculemus|[^a-zA-Z]main-spec)'

status=0

# the graduated normative documents must always exist — a rename would
# otherwise skip them silently green through the [ -e ] guard below
for doc in spec/IR-SPEC.md spec/RUNTIME-SEMANTICS.md; do
    if [ ! -e "$doc" ]; then
        echo "FAIL: graduated normative document '$doc' is missing (rename it AND this gate together)" >&2
        status=1
    fi
done

for zone in $ZONES; do
    [ -e "$zone" ] || continue
    if git grep -nIiE "$PRIVATE" -- "$zone" $EXCEPTIONS; then
        echo "FAIL: machine-local or private-repo reference in artifact zone '$zone' (WAY-OF-WORKING.md section 6)" >&2
        status=1
    fi
done

# The TOKA rights tripwire. ©-Belastingdienst material is admitted in exactly
# ONE place — the top-level toka/ parity zone (maintainer decision 2026-08-27,
# temporary; it leaves with the zone ahead of any public flip). Every OTHER
# artifact zone stays TOKA-free and this tripwire keeps firing on it — so it
# sweeps the SAME $ZONES the private-reference scan does (backends included: the
# ported Drools backend is an artifact zone and a comment quoting a TOKA/
# RegelSpraak-spec excerpt is a leak). toka/ is deliberately absent from $ZONES
# — it is NOT swept — and is asserted separately to carry no machine-local paths.
for zone in $ZONES; do
    [ -e "$zone" ] || continue
    if git grep -nIiE '(^|[^[:alpha:]])toka([^[:alpha:]]|$)' -- "$zone"; then
        echo "FAIL: TOKA material in artifact zone '$zone' (rights policy, corpus/README.md; the sole exception is the isolated toka/ zone)" >&2
        status=1
    fi
done

# The NO-SHARE tripwire (decision V3-a, 2026-08-27). The production RegelSpraak runtime
# lives ONLY under backends/drools/runtime/; ref/ (the reference interpreter) and
# spec/verifier/ (the OCaml verifier) MUST share no code with it — separate implementations
# are the structural proof the spec is engine-neutral. A transliteration of the production
# runtime into either zone (its class, an org.drools import, or a signature runtime method —
# rsPlus, rsVerdeelOpVolgorde, Breuk) is a LOUD failure here, never a silent copy.
for zone in ref spec/verifier; do
    [ -e "$zone" ] || continue
    if git grep -nIE 'RegelSpraakRuntime|org\.drools|rsPlus|rsVerdeelOpVolgorde|Breuk' -- "$zone"; then
        echo "FAIL: production-runtime token in '$zone' — the Drools runtime lives ONLY under backends/drools/runtime/; ref/ and spec/verifier/ share no code with it (decision V3-a)" >&2
        status=1
    fi
done

# toka/ is exempt from the self-containedness prose rules (it carries the
# ©-material and names the toolchain by design) but MUST carry no machine-local
# paths — the one rule that still applies inside the zone.
if [ -e toka ]; then
    if git grep -nIiE '(/users/|/home/|~/)' -- toka; then
        echo "FAIL: machine-local path in the toka/ parity zone (WAY-OF-WORKING.md section 6; toka/ is exempt from the prose rules but not from this one)" >&2
        status=1
    fi
fi

# The RENAME tripwire (card 2026-08-29 hernoem-prioritized-defaults; wire
# epoch v0.10). The ladder/rung vocabulary was renamed to prioritized
# defaults / default / exception group; HERNOEM_LADDER_20260829.md §6
# promises the remaining old-term lines as a reason-carrying,
# fails-when-stale list — mechanised here the gate-7 way. Every old-term
# line must match an allowance below (a reintroduction fails), and every
# allowance must still match at least once (a stale list fails). The
# allowances: the two formerly notes and the annex-A naming paragraph (the
# bridge to git history), the rename card itself (it names its own
# subject), and the runGolden substring false positive (no ladder
# vocabulary; the allowance re-greps the line with that token stripped).
# This script excludes itself: it necessarily spells the patterns it hunts.
RENAME_HITS=$(mktemp) || exit 2
git grep -inE 'ladder|rung' -- . ':(exclude)toka' ':(exclude)HERNOEM_LADDER_20260829.md' ':(exclude)scripts/check-self-contained.sh' > "$RENAME_HITS" || true
c_formerly_ir=0; c_formerly_rs=0; c_annex_open=0; c_annex_close=0; c_card=0; c_rungolden=0
while IFS= read -r hit; do
    hitfile=${hit%%:*}
    allowed=''
    case "$hitfile" in
        spec/IR-SPEC.md)
            case "$hit" in *'Renamed 2026-08-29 from'*) allowed=y; c_formerly_ir=$((c_formerly_ir+1));; esac ;;
        spec/RUNTIME-SEMANTICS.md)
            case "$hit" in
                *'Renamed 2026-08-29 from'*) allowed=y; c_formerly_rs=$((c_formerly_rs+1));;
                *'replaces "ladder", coined in this'*) allowed=y; c_annex_open=$((c_annex_open+1));;
                *'ladder logic (IEC 61131-3)'*) allowed=y; c_annex_close=$((c_annex_close+1));;
            esac ;;
        backlog/doing/20260829-hernoem-prioritized-defaults.md|backlog/done/20260829-hernoem-prioritized-defaults.md)
            allowed=y; c_card=$((c_card+1)) ;;
        backends/drools/src/main/java/io/github/yhavinga/regelspraak/cases/CaseRunner.java|backends/drools/src/main/java/io/github/yhavinga/regelspraak/cases/DroolsBackendMain.java|backlog/done/20260827-drools-backend.md)
            if ! printf '%s' "$hit" | sed 's/runGolden//g' | grep -qiE 'ladder|rung'; then
                allowed=y; c_rungolden=$((c_rungolden+1))
            fi ;;
    esac
    if [ -z "$allowed" ]; then
        echo "FAIL: pre-rename vocabulary reintroduced (not on the reason-carrying list): $hit" >&2
        status=1
    fi
done < "$RENAME_HITS"
rm -f "$RENAME_HITS"
[ "$c_formerly_ir" -eq 1 ] || { echo "FAIL: rename tripwire stale — expected exactly 1 formerly note in spec/IR-SPEC.md, found $c_formerly_ir" >&2; status=1; }
[ "$c_formerly_rs" -eq 1 ] || { echo "FAIL: rename tripwire stale — expected exactly 1 formerly note in spec/RUNTIME-SEMANTICS.md, found $c_formerly_rs" >&2; status=1; }
[ "$c_annex_open" -ge 1 ] || { echo "FAIL: rename tripwire stale — the annex-A naming paragraph (opening line) is gone from spec/RUNTIME-SEMANTICS.md" >&2; status=1; }
[ "$c_annex_close" -ge 1 ] || { echo "FAIL: rename tripwire stale — the annex-A naming paragraph (IEC 61131-3 line) is gone from spec/RUNTIME-SEMANTICS.md" >&2; status=1; }
[ "$c_card" -ge 1 ] || { echo "FAIL: rename tripwire stale — the rename card is gone from backlog/doing|done/" >&2; status=1; }
[ "$c_rungolden" -ge 1 ] || { echo "FAIL: rename tripwire stale — the runGolden false-positive allowance no longer matches anything; drop it from this list" >&2; status=1; }

[ "$status" -eq 0 ] && echo 'check-self-contained: PASS'
exit "$status"
