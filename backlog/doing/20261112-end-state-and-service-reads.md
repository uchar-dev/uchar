# The end state compared, the service reading through uchar, the TOKA declarations

Goal: the maintainer's decisions of 2026-11-12 after the zone reviews (card `zone-review-findings`).

## End state (uchar)

- ref (`runner --trace`) and the Drools backend (`DroolsBackendMain --readings`, both branches) write, per case file,
  the end state of the session in one text form: every object (a labelled instance and a created object with its
  type and its creation values), every attribute with a value (a timeline as its segments), every characteristic,
  every link, and the channel facts. The form is defined beside the reading text of RUNTIME-SEMANTICS ch. 5.12 and
  reuses its canonical value text and its object names.
- Gate 17 compares the two line by line per case file and branch, as it compares the readings: a difference fails
  unless a reason-carrying, stale-failing list names case and branch; floors for the compared objects, members and
  created objects. Proposal on `scripts/check-readings.sh` and the gate-17 text of `CLAUDE.md`.
- The Drools side is one public outcome reader in `backends/drools` (per object and member the value at the fixpoint,
  created objects with their type and creation values) which the dump writes and the service can read through.
- Gate 12 demands `PLAYED n` with n the number of case files of the golden, not only n > 0.

## TOKA declarations (uchar)

- `toka/casus/` gets one fragment of its own, an `Extensie van objecttype` that declares what the replay slices
  declare and the casus fragments leave undeclared (`treinmiles`, the characteristic "in het hoogseizoen", and the
  like as measured); the casus dump is pinned again once; the provenance text in `toka/README.md`, `README.md` and
  `CLAUDE.md` names the fragment as own work.
- The frontend refuses with an ERROR at the place: an undeclared attribute of an allocation (WF-034), an undeclared
  characteristic as target (WF-022), two allocations to one target (WF-026); `grammar/DIVERGENCES.md` loses the
  stale reasons for letting them through.

## Service (toolchain, its own commits)

- The pin moves to the uchar landing of the card `zone-review-findings` first, with what the service needs for it:
  a role link to an unknown label (`CaseInput.LinkTargetFault`), `compare` refusing a defective expectation with an
  `IllegalArgumentException`, an unbound parameter faulting EF-014.
- The service writes the parameters into the case (descriptor defaults < parameter set < request) and uchar's
  `CaseInput` reads and binds them; `TimelineInput.decode`, the global code in `Executor`, `defaultScalar` and the
  parameter half of `globalName` go.
- Every element of `faults` in the 422 answer carries `code` (EF-###) and `params` (an object of strings, as
  `expectedFault`) from uchar's public fault API; `kind` goes. The replay over the red cases holds the pinned code and
  parameters (subset rule of gate 8).
- A descriptor without entrypoints starts, with a warning on stderr.
- `RegelSpraakEngine` and its tests go; `Executor` is the one session facade.
- After the end-state reader lands in uchar: `AnswerBuilder` and `FixpointGivens` read through it (a later pin).

Acceptance: every item done with its witness or struck with the reason, one line each in the Result; `make gates`
green in uchar and in the toolchain; the toolchain pins the landing; a text for the workbench session on reading
`code` instead of `kind`.

May touch: `ref/`, `backends/`, `frontends/regelspraak/`, `toka/`, corpus witnesses outside `corpus/golden/anchor`,
this card. Protected (the main session lands them): `spec/`, `scripts/`, `CLAUDE.md`.
