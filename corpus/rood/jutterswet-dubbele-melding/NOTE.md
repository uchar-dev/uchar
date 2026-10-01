# jutterswet-dubbele-melding

Two conditional rules write the meldingsdatum of a Vondst, and on one
Vondst both conditions hold:

    Objecttype de Vondst (mv: Vondsten)
        de vinddatum Datum in dagen;
        de aanspoeldatum Datum in dagen;
        de meldingsdatum Datum in dagen;

    Regel melding op vinddag
        geldig altijd
            De meldingsdatum van een Vondst moet gesteld worden op zijn vinddatum
            indien zijn vinddatum gevuld is.

    Regel melding op aanspoeldag
        geldig altijd
            De meldingsdatum van een Vondst moet gesteld worden op zijn aanspoeldatum
            indien zijn aanspoeldatum gevuld is.

`ir.json` is the RegelSpraak frontend's emission of exactly this source. The
verifier accepts it: WF-079 refuses two UNCONDITIONAL computing writers of one
target, and a guarded pair is left to its conditions (IR-SPEC ch. 3.1). Where
the conditions exclude each other the document has an answer; the green twin
`golden/jutterswet-melding-naar-aard` pins that form.

Case 1 is a Vondst V with vinddatum 04-03-2026 and aanspoeldatum 01-03-2026.
Each rule is an equation (RUNTIME-SEMANTICS ch. 5.1), and on V the two demand
04-03-2026 and 01-03-2026 for one attribute: no fact base satisfies both, so
there is no fixpoint, and ch. 5.7 makes that divergence a fault. Case 2 puts
a Vondst on which only the first condition holds beside such a Vondst, so the
report must name the instance that diverges.

What each executor observes, with the writers, the target and (where the
executor sees them) the values in its sentence:

- `ref/` runs a pass in which each rule overwrites the other, so its pass
  budget runs out; it then replays two passes form by form and names the
  attribute two writers keep changing to different values: "two writers of one
  target disagree: rule "melding op vinddag" writes 2026-03-04 and rule
  "melding op aanspoeldag" writes 2026-03-01 to the attribute meldingsdatum of
  V, a Vondst".
- `backends/clingo` lets each of two computing writers of one target derive
  its value under its own index, and derives a named `viol/2` witness for the
  pair where their values on one subject differ (a writer that fires with a
  leeg value counts as writing leeg); the program is then UNSAT and the runner
  reports the witness.
- The Drools exec-model branch reaches its fire cap, the two rules firing each
  other again; the replay keeps the last writes of the session and names the
  writers, their values and the attribute.

Before the card that made them named (2026-10-01) all three failed or
answered differently on this body: ref faulted with "no fixpoint within 100
passes — divergence is a fault (ch. 5.7)" alone, clingo gave meldingsdatum two
values in one model (`MISMATCH: V.meldingsdatum = [20513;20516]`, and without
an expectation no fault at all), the exec-model branch reported "geen fixpunt
(kringverwijzing?)", a cycle the document does not contain, and the DRL-text
branch fired each rule once and kept the value of the rule declared last.
