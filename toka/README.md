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
  with the original toolchain** at the frontend pin. Gate 11
  (`scripts/check-toka-parity.sh`) re-emits it through uchar's own frontend and
  requires byte-identity, so any emitter divergence between toolchain and
  frontend is caught.
- `src/test/java/**` — two of the three integration tests excluded at the
  frontend takeover (CasusTest, ModelTest), in their original form (only the
  fixture path and, where needed, the package were adjusted). They exercise
  the frontend end-to-end on the TOKA shapes. RuleCheckTest stayed behind:
  it imports the Drools transpiler, which is out of this zone's scope — it
  returns with a Drools-backend card.

## The holdout

The full casus raises exactly **one** resolver ERROR — the §4.1 datum+datum
casus-quirk in `toka-23` (the `bevestigingsinterval`, a Datum-tijd parameter
added to a Datum-tijd, which typeringen §4.1 forbids). `CasusTest` pins that as
the single tolerated diagnostic. Because `--ir` writes nothing for a model with
any error, the pinned dump is emitted from every fragment **except** `toka-23`;
the gate holds it out by name and fails if the holdout ever goes stale.

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
