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
# zone's first real delivery. Planned graduation into ZONES at Phase-1 exit:
# spec/IR-SPEC.md, spec/RUNTIME-SEMANTICS.md.
set -u
cd "$(git rev-parse --show-toplevel)" || exit 2

ZONES='corpus/golden corpus/negatief spec/schema spec/verifier ref backends'
EXCEPTIONS=':(exclude)ref/README.md'
PRIVATE='(/users/|/home/|~/|regelspraak-java|regelspraakruntime|calculemus)'

status=0

for zone in $ZONES; do
    [ -e "$zone" ] || continue
    if git grep -nIiE "$PRIVATE" -- "$zone" $EXCEPTIONS; then
        echo "FAIL: machine-local or private-repo reference in artifact zone '$zone' (WAY-OF-WORKING.md section 6)" >&2
        status=1
    fi
done

for zone in corpus/golden corpus/negatief; do
    [ -e "$zone" ] || continue
    if git grep -nIiE '(^|[^[:alpha:]])toka([^[:alpha:]]|$)' -- "$zone"; then
        echo "FAIL: TOKA material in corpus data (rights policy, corpus/README.md)" >&2
        status=1
    fi
done

[ "$status" -eq 0 ] && echo 'check-self-contained: PASS'
exit "$status"
