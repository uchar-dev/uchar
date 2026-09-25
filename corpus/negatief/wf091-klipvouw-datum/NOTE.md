# wf091-klipvouw-datum

Violates WF-091 (`spec/IR-SPEC.md` ch. 4.1, the numeric-operand arm): the
gedurende-clipped temporal `total` (`RUNTIME-SEMANTICS.md` ch. 3.6) over a
timeline declared `Date`. The clip does not change what the total does to
the clipped segments — it multiplies each by its whole grid units — so the
operand must be a Number as for the unclipped total
(`wf091-tijdlijntotaal-datum`).

    WF-091 FAIL /rules/rules/0/versions/0/value/arguments/0: the total of
    'wachtvergoeding per maand' on 'Klipjutter' folds Date values: a total,
    sum or pro-rata part is arithmetic over a Number, and Date values are
    calendar positions

**Origin.** Until the card ref-klipvouw-nul this document was the red case
`corpus/rood/datumtijdlijn-klipvouw` (card temporele-datumvouw), and it is
byte-identical: `golden/jutterswet-erfwacht` with the clipped attribute and
the target retyped to a day-precision `Date`. It was verifier-clean. With a
value inside the clip `ref` faults on the multiplication (`maal: expected a
number, got 2024-01-15`), and the ASP backend answered **19737** for one
clipped month until the card temporele-datumvouw made it decline the form
by name.

**The 0 in a date slot.** Measured on this document (2026-09-23, again
2026-09-25): on a case WITHOUT a value timeline, and on one whose value the
clip removes entirely, `ref` answers `K1.wachtsom = 0`. That is the clip
rule of ch. 3.6 — a fully clipped-away or absent value timeline totals 0,
never leeg — and for a numeric target it is the norm, pinned in all four
executors by `golden/jutterswet-getijdenwacht` (J2 without a timeline and
J3 without clip periods, both 0). Here the 0 lands in a `Date` slot, a value
no chapter defines. It needed no change in `ref`: the form that produced it
is ill-formed, and a verifier-clean document can no longer reach it.

The executors keep their own refusals as a second line, with their
sentences unchanged: `ref` faults on the multiplication, the ASP translation
declines the clipped date-valued timeline by name ("… Declined in
translation"), and the DRL-text branch of the Drools backend markers the
form through `Vouwwaarde.tijdlijnWeigering`. The Drools exec branch asks the
same question of the declaration since the card ref-klipvouw-nul and refuses
the document in the same sentence. Before that its case loader refused a
date-valued segment, and on the case without a value it crashed at fire
(`ClassCastException: Cannot cast java.math.BigDecimal to
java.time.LocalDate`, measured 2026-09-25); `TijdlijnKlipvouwWitnessTest`
in the Drools backend pins both branches on both clip cases.

Red first, measured on this document: the verifier of main 2f47d80e (md5
`e0bafac419b97e989317b13194932efe`) reported `verifier: PASS`; with the arm
(md5 `d2b5ce9992759f07860e95fbecec733e`) the single rejection above.
Mutation: with the arm's type test switched off (`when false && …`), or
with `total` removed from the fold branch, this document passes again and
gate 3 names it; `expect.txt` pins the arm.
