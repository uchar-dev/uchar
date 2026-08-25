# Unit-literal emitter fix (toolchain, mandated) + wire witness

Goal: decision 1-A (2026-08-25), writer side. The v0 emitter in
`regelspraak-java` (path supplied at session start:
/Users/yeb/Developer/yhavinga/regelspraak-java) writes a unit into the
numeric literal lexeme (`"waarde": "5 €"`); WF-081 (landed first — this
card REQUIRES the wf081 card merged) now rejects that wire form. Fix the
emitter so the lexeme is the bare number and the unit lives in the
literal's type (mirroring how attribute units ride the wire — first
investigate how the wire carries units on attribute types and whether a
literal's type can already carry one; if IR-SPEC/schema must move to give
the literal type a unit slot, that delta is part of this card,
spec-first, narrowest form). This is the ONE-TIME mandated toolchain
write (maintainer decision 1-A) outside the read-only rule. Then close
the gate-blind spot: add an emitter golden whose RegelSpraak source uses
a unit-carrying numeric literal, so the corpus finally exercises the
form — verifier-clean AND replaying in both executors (the TOKA report
showed Drools computes with it; ref/clingo must now agree on the case's
pinned values).

Acceptance:
- Red-first, shown in the Result: today's emitter output on a
  unit-literal source violates WF-081 (verifier rejection quoted); after
  the fix it is verifier-clean and both executors replay the case.
- Toolchain: `make uchar-conformance UCHAR_DIR=/Users/yeb/Developer/uchar-dev/uchar`
  green — all emitter goldens byte-identical (first verify whether any
  existing golden carries a unit literal; if so, it moves spec-first with
  this card). SPEC_REV re-pinned to the uchar main that contains this
  card's uchar side.
- uchar: all nine gates green.

May touch: toolchain emitter + its tests + SPEC_REV (one-time mandate);
uchar `corpus/golden/<new emitter golden>`; only if the literal-type
unit slot requires it: `spec/schema/`, `spec/IR-SPEC.md`, `spec/verifier/`,
and executor unit handling in `ref/` and `backends/clingo/` (narrowest —
the literal must carry its unit the way attributes already do).
Out of scope: `spec/CONSTRUCTS.md` (registry note serial at merge),
executor-parser loosening (the lexeme stays bare — WF-081 stands), any
other toolchain change beyond the emitter fix.

Result: both sides landed. Investigation first: the v0 wire ALREADY
carries the unit slot — `Literal.type` is the full datatype union, and
`Numeriek`/`Percentage` carry `eenheid` exactly as attribute datatypes
do; both executors already read it (`ref` `unit_of_datatype`, clingo
`eff_unit` on the literal type). So NO uchar-side schema/IR-SPEC/
verifier/ref/clingo movement was needed: the narrowest form is
toolchain-only. Red-first: today's emitter on `... plus 5 €` wrote
`"waarde": "5 €"` (with the resolved Eenheid already in the type!);
verifier: `WF-081 FAIL /resolvedRegels/0/waarde/rechts: numeric
literal lexeme '5 €' is not a bare number`. Fix (mandate 1-A, toolchain
commit f8a3ce8): the literal's waarde is the bare NUMBER token text —
covers the unit suffix AND the same-production '%' suffix ("21%" wrote
"21%" too); `bron` keeps the full snippet; Drools consumers route
through Rationaal.van and are byte-unchanged (make test 551 pass /
1 pre-existing env-abort; drools-it 957 pass). Scan verified NONE of
the 12 emitter goldens carries a unit or % literal (all numeric
waardes bare), so none moved. New emitter golden
`corpus/golden/jutterswet-meldloon`: integer (5 €) and decimal-comma
(12,50 €) unit literals, chained, two cases with absolute values
replaying in ref AND clingo (310 doubly-replayed corpus cases).
uchar-conformance against this branch: 13 emitter goldens
byte-identical. All nine gates green. Deliberately left: the SPEC_REV
re-pin (maintainer, after this lands); executor-parser stays strict
(WF-081 stands).
