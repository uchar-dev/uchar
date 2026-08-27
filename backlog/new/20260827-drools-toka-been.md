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
