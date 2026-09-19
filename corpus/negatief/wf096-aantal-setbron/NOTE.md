# wf096-aantal-setbron

Violates WF-096 (`spec/IR-SPEC.md` ch. 4.3) in the clause's KIND half —
the narrowing of 2026-09-19, which took the set fence's exemption from
WF-093's pair (a single argument under any folding kind) down to the six
VALUE folds alone. This is the form the decision was taken about: a count
whose single argument is a set reading of an element attribute.

    WF-096 FAIL /resolvedRegels/0/waarde/argumenten/0: set reading on a
    fold operand that is not a VALUE fold's SOURCE: a set reading is
    admissible on the single argument of a value AGGREGATION alone
    (ch. 4.3) — with two or more arguments the node is the listed value
    fold, whose operands are ordinary reads; at a non-aggregation kind
    there is no source at all; and under AANTAL the fold reads the
    CONTRIBUTORS, never a value per contributor, so a set reading there
    names a collection the count does not consume

The document. `corpus/golden/jutterswet-vondsttelling` with its second rule
removed and the first one's source replaced: the `AANTAL` over
`elementObjecttype: "Vondst"` keeps its `bronRol` (`vondstaanmelding` /
`vondst`) and its count-typed target, and its single argument is now the set
read the golden's `SOM` carries — `gewicht` on `Vondst`, a real kg type with
two decimals, `set: true`. Every other clause holds: the anchor is the
element type as WF-083 (c) demands of a set source, the binding resolves,
the node type is the count's own. The KIND is the only defect, and the
verifier reports one fault.

Why the form left the wire. It was verifier-clean and had no producer route
— the RegelSpraak frontend refuses the sentence that would write it, citing
this very rule — while the two executors that carried it read it two
different ways, which is visible only when a member's attribute is leeg.
The case below is exactly that document, and it is the golden's own case
file: `J1` has three vondsten of which `V3` carries no `gewicht`, `J3` has
one such vondst.

Executors, on this document, with the golden's pinned counts (3 for `J1`,
1 for `J3` — the INSTANCE counts the bare wart produces):

    ref         : MISMATCH J1 = 2, J3 = 0   (it counts the FILLED VALUES)
    ASP backend : TRANSLATION FAULT: expression kind not translated (no
                  corpus case exercises it)
    Drools      : PLAYED — 3 and 1 (it counts the MEMBERS), and against the
                  ref answers it says "verwacht 2, werkelijk 3"

One executor refuses and two answer differently by one whenever the set has
a leeg member: no differential gate can see it, because the corpus carries
no such document. `RUNTIME-SEMANTICS.md` ch. 1.2e and ch. 3.4a settle which
of the two readings is the count's — the contributors, not their values —
so the disagreement was an error rather than a variant, and the maintainer
refused the wire form instead of amending a clause to fit an executor
(2026-09-19, option (ii) of
`backlog/done/20260909-eerste-laatste-setvouw-kloof.md` §6). `ref`'s
set-source count arm and the Drools lowering stay as they are, unreachable
through a well-formed document, never dead branches.

Rood-eerst, measured on this document. Pre-narrowing verifier
(md5 `441c25d84dc4cc54931ed8a5504b00f5`): `verifier: PASS`. With the
narrowing (md5 `3549d7ea72a43cd3164585e7f2955ae2`): the single rejection
above.

Falsifiable, arm-exact: put `AANTAL` back into the set fence's exemption
(`set_source_exempt` in `spec/verifier/lib/checks.ml`, one disjunct) and
leave everything else untouched. Swept over golden + rood + negatief +
parity (666 documents) exactly four documents move: this case and its two
siblings turn clean again, and `wf083-vouwbron-anker-op-operand` loses the
co-firing line it gained. No golden, no red case and no other negatief
changes a verdict, and the mutant's reports differ from the pre-narrowing
verifier's in wording alone.
