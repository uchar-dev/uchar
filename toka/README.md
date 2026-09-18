# toka/ — the isolated TOKA parity zone (temporary)

This top-level zone is the **one place** in the repository where
©-Belastingdienst **TOKA** material is admitted, by maintainer decision of
2026-08-27, **for feature-parity testing** of uchar's own RegelSpraak frontend
against the original toolchain. It is isolated in a single directory so it can
be removed in one move.

## What is here

- `casus/*.rgs` — the TOKA casus: a mechanical, verbatim split of the
  object/parameter/rule definitions of the worked example published in the
  RegelSpraak v2.3.0 specification. The TOKA data is explicitly fictitious
  ("volledig verzonnen", per the spec's own disclaimer) and relates to no real
  case. The casus document itself is © Belastingdienst and is **not**
  redistributed beyond these definition fragments.
- `casus/ir.json` — the pinned parity dump: the faithful v0 IR emitted **once
  with the original toolchain** at the frontend pin, minus the recorded
  divergences below. Gate 11 (`scripts/check-toka-parity.sh`) re-emits it
  through uchar's own frontend and requires byte-identity, so any *unrecorded*
  emitter divergence between toolchain and frontend is caught.
- `cases/<scenario>/{ir.json,case.json}` — the replay fixtures of gate 11's
  differential leg. Those `ir.json` are **slices** of the casus dump: they carry
  no sibling `.rgs`, so gate 11 re-emits none of them (it schema-checks and
  replays them instead). A slice therefore does not follow the casus dump by
  itself — when the emitter moves, the slices are moved by hand in the same
  card, or the zone carries two spellings of one construct.
- `src/test/java/**` — two of the three integration tests excluded at the
  frontend takeover (CasusTest, ModelTest), in their original form (only the
  fixture path and, where needed, the package were adjusted). They exercise
  the frontend end-to-end on the TOKA shapes. RuleCheckTest stayed behind:
  it imports the Drools transpiler, which is out of this zone's scope — it
  returns with a Drools-backend card.

## The holdout

The full casus raises exactly **two** resolver ERRORs, both pinned by
`CasusTest` as the tolerated diagnostics: the §4.1 datum+datum casus-quirk in
`toka-23` (the `bevestigingsinterval`, a Datum-tijd parameter added to a
Datum-tijd, which typeringen §4.1 forbids), and the dimensional inconsistency
in `toka-45` (decision 16Y, 2026-08-29: the identity unit typing types the
product, so the written subtraction fails the §4 unit rule — uchar-v1
deliberately diverges from the toolchain here). Because `--ir` writes nothing
for a model with any error, the pinned dump is emitted from every fragment
**except** those two; the gate holds both out by name and fails if either
holdout ever goes stale.

## Deliberate divergences from the toolchain emission

Gate 11 compares uchar's frontend against the files pinned here, not against a
live toolchain. So once uchar's emitter changes **on purpose**, the pin is
re-taken in the same card and the comparison becomes "frontend == the recorded
state" for that construct. Every such move is listed below, so the parity claim
stays honest about where it no longer proves toolchain agreement.

- **2026-09-18 — a Tekst literal's `waarde` is the parsed string.** Maintainer
  decision of that date, executed by the card
  `backlog/done/20260918-tekstlexeem-canonicalisatie.md`. The emitter used to
  write the source snippet *with* its own outer `"` delimiters; it now writes
  what stands between them. (The RegelSpraak string grammar has no escape
  sequence — a `"` always closes the string — so that strip is total, lossless
  and never unescapes anything.) 48 literal values moved: 12 in `casus/ir.json`
  and the same 12 in each of the three `cases/scenario-toka-46-woonregio-factor-*`
  slices, plus the 3 matching quote-carrying case inputs, which had to move with
  their literals because every executor compares Tekst **verbatim**. Nothing
  else in those seven files changed. The original toolchain still writes the
  snippet form, so for text literals this dump has stopped being a toolchain
  dump.
- The precedent runs the other way: `toka-45` (decision 16Y, 2026-08-29) is held
  **out** of the re-emission rather than re-pinned, because uchar's unit typing
  rejects the fragment as written. Divergence by holdout and divergence by
  re-pin are both recorded; a silent divergence is not on offer.

## Rights and lifecycle — read before touching

- This material enters git **history** from the card that created the zone.
  While the zone exists the repository **must stay private**.
- Any public flip (PLAN §9 decision 10) requires **both** removing this zone
  **and** a history rewrite (filter-repo or a fresh public copy) — deleting the
  files alone does not erase them from history.
- Gate 11 is explicitly **temporary**: it leaves the gate set with the zone —
  the one documented exception to gate-set monotonicity.
- Every other zone stays TOKA-free: the rights tripwire in
  `scripts/check-self-contained.sh` keeps firing everywhere else. This zone is
  exempt from the self-containedness prose rules but must carry **no
  machine-local paths**.
