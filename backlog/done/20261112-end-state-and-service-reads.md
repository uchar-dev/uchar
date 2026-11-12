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

Result (2026-11-12):
- End state: RUNTIME-SEMANTICS ch. 5.12a defines the end state of a session (every object, made objects included,
  with its type; every attribute with a value, a timeline as segments, a cell with its labels; every characteristic
  that holds; every link; the §9.5 facts) in the text of ch. 5.12, with three worked examples (`WV-5.12a-…`). ref
  writes it under `runner --trace` (`ref/lib/end_state.ml`); the Drools backend reads it through one public reader
  (`DroolsBackend.endStateOf` → `EndState`) and writes it under `--readings` on both branches. Gate 17 compares the
  two line by line (`STATE_DEVIATIONS` empty; floors 3015 objects, 479 made, 8337 members). Three representation
  differences were fixed in the writers (the exact value of a non-terminating number, now carried by
  `Fixpoint.DerivedValue`; the asOfDate-bucket scalar as the timeline of its one period; the declared type name);
  no outcome difference turned up. Gate 12 demands `PLAYED n` with n the golden's case files. Provisional: the end
  state does not keep the order of links over one role (sorted); a member without a value and a characteristic
  that does not hold get no line.
- `EndState.Member` carries `derived` and `given` (what the bean field holds without the write-back). A second
  entry `DroolsBackend.standingOf` reads a session in which a fault stands with the same reader: not normed (ch.
  5.7), no end state, a view for diagnosis that the service's fixpoint detail of a 422 reads; `endStateOf` keeps its
  refusal. Witnesses in `EndStateWitnessTest` (`jutterswet-invoer-naast-objectcreatie`,
  `red/jutterswet-fout-aan-fixpunt`, the DROOLS-312 limit of `anchor/jutterswet-verzuimrente-30-360`).
- Every fault carries its EF code: six new kinds in ch. 5.7b, raised by ref, clingo and Drools with a red case each —
  EF-041 (case periods of a characteristic a rule writes), EF-042 (own total without clip over a full case
  timeline), EF-043 (the same without an evaluation date), EF-044 (fractional power of a negative base), EF-045
  (irrational root or power without rounding), EF-046 (a date part that is no whole number); clingo stands in the
  divergence list of gate 8 for EF-044/045 (CLINGO-041) and EF-046 (new CLINGO-048). Struck: a division by zero
  raised by the runtime (every division is guarded; a division by zero is the deposit EF-005); a day-kind or bucket
  walk over an open period and an Easter Sunday of a non-whole year (unreachable). Case-input defects stay case
  errors without a code (ch. 5.8c). Provisional: EF-041 on the fault channel; EF-043 a kind of its own beside EF-011.
- TOKA declarations: `toka/casus/own-declarations.rgs` declares the six members the 47 fragments use and do not
  declare (as the 29 slices do); the casus dump is re-pinned from 41 fragments; toka-33 to 37 (allocations after the
  first to `treinmiles`) are holdouts of gate 11 (maintainer decision). The frontend refuses an undeclared member of
  an allocation (RGS-425, WF-034), two allocations to one target (RGS-426, WF-026), an undeclared characteristic as
  target (RGS-427, WF-022) and an own read of an undeclared name (RGS-428). The verifier's report on the casus dump
  goes from 14 to 2.
- Service (toolchain, own commits): pinned the uchar landings; the service writes parameters into the case
  (descriptor default < parameter set < request) and uchar's `CaseInput.setGlobals` binds them (`TimelineInput.decode`
  and the service's global code gone); every fault of a 422 carries `code` and `params` from uchar's fault API, `kind`
  gone, and the replay's red leg holds 64 red cases to their pinned code; a link cycle of the input (EF-033) is a 422
  instead of a 500; a descriptor without entrypoints starts with a warning; `RegelSpraakEngine` gone; the answer reads
  uchar's `EndState`. The fixpoint detail reads `standingOf` at the next pin.
- `make gates` green.
