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

[ "$status" -eq 0 ] && echo 'check-self-contained: PASS'
exit "$status"
