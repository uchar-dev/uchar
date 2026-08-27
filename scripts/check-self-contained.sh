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
PRIVATE='(/users/|/home/|~/|regelspraak-java|regelspraakruntime|calculemus|[^a-zA-Z]main-spec)'

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
# artifact zone stays TOKA-free and this tripwire keeps firing on it. toka/ is
# deliberately absent from the scanned set below — it is NOT swept — and is
# asserted separately to carry no machine-local paths.
for zone in corpus/golden corpus/negatief corpus/rood frontends; do
    [ -e "$zone" ] || continue
    if git grep -nIiE '(^|[^[:alpha:]])toka([^[:alpha:]]|$)' -- "$zone"; then
        echo "FAIL: TOKA material in artifact zone '$zone' (rights policy, corpus/README.md; the sole exception is the isolated toka/ zone)" >&2
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
