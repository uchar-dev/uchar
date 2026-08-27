# The temporary toka leg of the square (leaves with the zone)

Goal: decision V6-A (2026-08-27, the maintainer reconfirmed the
privacy/history consequence). The slotmeting proved Drools ≡ ref ≡
clingo on the toka intersection with the case JSONs living only in
/tmp. Deliver, stacked on the drools cards: (1) commit the derived toka
`case*.json` files under `toka/cases/` (the isolated zone — ©-material
stays ONLY there; derive them with the slotmeting method from
/tmp/toka-slotmeting, or re-derive read-only from the toolchain
scenario expectations); (2) extend `scripts/check-toka-parity.sh`
(gate 11, temporary): beyond the byte-identical re-emission, every toka
case must now replay through ref AND clingo AND Drools with the same
outcome classes the slotmeting recorded (exact agreement where all
carry; loud refusals elsewhere; ZERO silent divergences — a value
difference without a fault fails the gate); (3) the counts land in the
gate output (like the slotmeting's per-class table). The whole leg is
temporary and leaves with the zone, per the CLAUDE.md charter.

Acceptance:
- Red-first: show the gate red before the cases land (or on a
  deliberately wrong expectation), then green with counts.
- Gates 1–12 green; TOKA material still only under toka/ (the gate-1
  probe still trips outside).

May touch: `toka/**`, `scripts/check-toka-parity.sh`,
`backends/drools/**` (only if the runner needs a toka-path hook).
Out of scope: everything outside the zone and the gate script;
`spec/**`; new fixtures beyond the case derivations.

---

Result (2026-08-27, branch `card/drools-toka-been` on `card/drools-rood`):

**Landed.** (1) 30 replay fixtures under `toka/cases/<scenario>/{ir.json,
case.json}` — byte-identical copies of the slotmeting derivation
(`/tmp/toka-slotmeting`): each `ir.json` is the toolchain slice the scenario's
companions resolve to, each `case.json` the hand-derived Situatie+verwachting.
(2) `toka/cases/parity.manifest` pins every scenario's outcome class in each
executor. (3) `scripts/check-toka-parity.sh` gained a 4th obligation — the
differential replay: every toka case runs through ref, clingo, and the
uchar-IR-loading Drools backend (jar off the wire, the architecture anchor),
and each observed class MUST match the pinned one; per-class counts land in the
report. `backends/drools/**` untouched (no toka-path hook was needed — the jar
takes `<ir.json> <case.json>` already).

**The measurement (gate 11 green, reproduces the slotmeting exactly).**
- ref: 30/30 PASS (the ref-100 oracle carries every toka case).
- clingo: 15 PASS / 15 loud translation-fault — allocation-input 6, expression-
  kind 2, unscalable-datatype 2, quote-carrying-label 3, som-0-fallback 1,
  non-numeric-operand 1.
- Drools: 0 PLAYED / 30 loud per-form EXCLUDE — wederkerig self-relation
  (§13.3.9.2) ×29, objectcreatie (§13.4.6) ×1.
- **ZERO silent divergences** — every one of 90 executor runs is either a
  verified pass against the pinned expectation or a loud, fragment-pinned refusal.

**Honest scope of the Drools leg.** The uchar Drools backend carries NONE of
the 30 slices: every slice bundles the whole structural model, including the
wederkerig `Partnerrelatie` feittype, which the v0 backend refuses per model
(§13.3.9.2) — a LOUD per-form EXCLUDE, exactly the architecture anchor's "weiger
LUID per vorm, niets degradeert stil". toka-28 additionally carries an
objectcreatie (§13.4.6), checked first. So the "all three carry" intersection is
empty on the Drools axis; this leg proves the zero-silent-divergence discipline
and the loud-refusal contract, not a positive Drools play on this casus. That is
the faithful measured state — no slice was narrowed to force a play.

**Red-first (cited).** With `toka-20` clingo flipped PASS→(actual FAULT) and
`toka-28` Drools flipped EXCLUDE→PLAYED, the gate reddened with both
divergences named; reverting the two lines returned it green.

**Gates 1–12 all green.** 1 self-contained; 2 ir-schema 143; 3 verifier 143
golden clean / 161 negatief; 4 case-schema 365; 5 oracle 343; 6 clingo 343;
7 noop 308 + 35 survivors; 8 rood 22 (Drools leg 1 fault-pinned / 21
not-carried); 9 catala 169 checks / 11 goldens; 10 regelspraak 130 tests / 19
goldens; 11 toka-parity 46 .rgs byte-identical / TOKA suite 27 / 30 replay cases;
12 drools 41 played / 72 auto-excluded / 7 value-divergence. The gate-1 TOKA
tripwire was shown to still fire outside `toka/` (a planted token in
corpus/golden reddened it; removed, green again).

**Deliberately left / extra scope.** Committing the leg required, beyond the
`case*.json` the card names, the per-scenario `ir.json` slices and the
`parity.manifest` under `toka/` — a case cannot replay without its IR, and the
recorded classes must be pinned somewhere; both are part of the slotmeting
derivation and live only in the charter zone (gate-1-clean, machine-path-free).
Per-slice re-emission byte-identity is NOT asserted (the toolchain companion
subsets are not committed here); the full-casus re-emission remains the
frontend==toolchain parity proof. The whole leg is temporary and leaves with the
zone. Protected path touched: `scripts/check-toka-parity.sh` — branch left
unmerged as the maintainer's proposal.
