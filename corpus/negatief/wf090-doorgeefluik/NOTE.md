Violates WF-090 (`spec/IR-SPEC.md` ch. 2.4) in the **element-passing
aggregation** position: `EERSTE(kern van alle schuiten van de Berging)`
reads a truthfully typed `dossier` source, hands the contributor through
unchanged — with one contributor the fold makes no comparison at all — and
declares its own `type` to be `logboek`. The target attribute agrees with
the fold node, so the write half is clean and the only defect is the
claim.

    WF-090 FAIL /resolvedRegels/0/waarde/type: the EERSTE aggregation is
    typed with struct domain 'logboek', but operand 0 names struct domain
    'dossier'

Why it has to exist beside `wf090-foldbron`: without the aggregation
position the fold is a laundering step — its value comes from a `dossier`
attribute and its type says `logboek`, and every reference in the document
is faithful. This is the struct twin of
`wf089-aggregatie-doorgeefluik-enum`. Verifier-clean before; ref faulted
(`field "bedrag" is not present on the record value`); clingo stops at the
same `TRANSLATION FAULT: expression kind not translated (no corpus case
exercises it)` as the faithful twin, so ref is the executor witness.

*Neighbouring gap, closed since:* `EERSTE` is an ordering fold while
`StructType` is equality-only (ch. 2.4), so an entirely truthful struct
`EERSTE` was a WF-038 operand question. WF-038's ordering-fold operand
clause (2026-09-05) answers it — every operand of an ordering fold comes
from one comparable family, numbers or dates — so this document now
carries two independent defects, and a truthful struct `EERSTE` is no
longer reachable through a valid document at all.

Falsifiable, arm-exact: call `wf089_domain_identity` instead of
`use_site_domain` in the aggregation branch of the `Functie` arm and the
WF-090 line disappears here — one report changes and no other, so
`wf090-foldbron`, whose lie is one level down, is untouched. Re-measured
after the ordering-fold clause (2026-09-05): the document does not turn
clean under that mutant, since a struct contributor under an `EERSTE`
violates the new clause on the same operand. The falsification signal is
the rule cited: under the mutant it is rejected for WF-038 and no longer
for the WF-090 its directory declares.
