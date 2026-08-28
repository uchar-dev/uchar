# A public witness for the exact naar-rato-with-maximum translator branch

Goal: the clingo translator branch admitted at the allocatie-input card —
NAAR_RATO exact (no rounding mode) WITH a maximum, translatable when the cap
is no finer than the target scale (cdec <= tdec, so the clipped write is an
exact integer at target scale) — currently has NO public witness: its only
machine-checked coverage is a private toka scenario, and the toka zone is
temporary. One rights-free, own-authored Jutterswet golden pins the branch
independently: a verdeling NAAR_RATO (exact, no afronding) with a maximum
where (a) the cap CLIPS at least one recipient, (b) at least one recipient
stays under the cap, (c) the raw proportional shares are exact integers at
the target scale (otherwise clingo's exactness viol faults — that stricter
corner is loud by design and NOT this card's subject), and (d) the
onverdeelde rest (pot minus the sum of clipped shares) lands in the
remainder attribute per RUNTIME-SEMANTICS ch. 4. Hand-computed absolute
values in a NOTE.md; noop-refuting; NO case input on the allocation target
(that layer has its own witness jutterswet-veilinginvoer). A second case
where the cap clips nothing (pure proportional) strengthens the pin.

NO code changes: backends/clingo is untouched; ref is untouched. The only
spec touch allowed: the one S6/S12 sentence in spec/ASP-TRANSLATION.md that
describes this admitted class may gain the golden's name as its witness —
nothing else moves, S19 does not change.

Acceptance:
- The new golden passes gates 2-8 (schema round-trip, verifier, case
  schema, ref oracle, clingo differential, noop-refuting without survivor
  entries, rood untouched).
- gate 12 plays it (expected: verdeling plays since the voorwaarde-fix) or
  carries a reason-carrying, evidence-backed exclusion — never silent.
- All twelve gates green in the worktree.
May touch: corpus/golden/** (the new golden only), spec/ASP-TRANSLATION.md
(witness-naming sentence only), scripts/check-drools.sh (ONLY for a
reasoned exclusion), backlog/.
Out of scope: backends/**, ref/, toka/**, spec/IR-SPEC.md,
spec/RUNTIME-SEMANTICS.md, spec/schema/, spec/verifier/, frontends/**,
scripts/check-noop.sh, spec/CONSTRUCTS.md, spec/PLAN.md, any toolchain
commit.

## Result (2026-08-28)

Landed `corpus/golden/jutterswet-buitplafond` (own-authored Jutterswet,
strandjutten domain, Dutch names): a single `NAAR_RATO` allocation,
`afrondingDecimalen: null` (exact), maximum `buitplafond`, all attributes
whole (0 decimals) so cdec = tdec = 0 — the cap floors to an exact integer
at the target scale, the admitted branch. Two hand-computed cases (NOTE.md):

- `case.json` — pot 120, weights 1:2:3 (Σ 6), raws 20/40/60 (exact
  integers); caps 30/25/50 → shares **20** (under, b), **25** (clipped, a),
  **50** (clipped, a); rest **25** into `buitrest` (ch. 4.4).
- `case2-onbeperkt.json` — pot 90, same weights, caps 100 → the cap clips
  nothing: shares **15/30/45**, rest **0** (pure proportional).

No case supplies the allocation target, so both cases refute --noop (gate 7,
no survivor entry). Values are hand-derived from RUNTIME-SEMANTICS ch. 4.2/
4.3/4.4, never copied from an executor.

All twelve gates green in the worktree: 1 self-contained, 2 schema
round-trip, 3 verifier, 4 case schema, 5 ref oracle, 6 clingo differential
(ref ≡ clingo), 7 noop, 8 rood, 9 catala, 10 regelspraak, 11 toka-parity,
12 drools. Gate 12 **PLAYS both cases** through Drools (ref ≡ clingo ≡
Drools — the square closed on the exact-with-cap branch), so no exclusion
was needed and `scripts/check-drools.sh` is untouched.

Spec touch (only): `spec/ASP-TRANSLATION.md` S12 Witnesses gains the
golden's name for the exact (unrounded) proportional-share-under-a-cap
class (cdec ≤ tdec). S19 unchanged. No code changes to `backends/**` or
`ref/`.

## Review round (2026-08-28)

Two lenses, no CRITICAL and no MAJOR. Hand-computation re-derived
independently (case.json 120·{1,2,3}/6 = 20/40/60 → clip {30,25,50} →
20/25/50, rest 25; case2 90·{1,2,3}/6 = 15/30/45, rest 0) — matches the
case files, `NOTE.md`, and every executor. The maximum machinery is
load-bearing: J2/J3 clip actively (raw 40/60 → cap 25/50), J1 stays under
(req b), rest 25 into `buitrest` (req d), raws exact so clingo's exactness
viol stays silent (req c). Directly played through the Drools jar:
`PLAYED 2`, and the golden appears in none of gate 12's exclusion lists.

Two minors were raised and **rejected with motivation** (both observations,
no defect):

- *witness covers only the equal-scale edge cdec = tdec = 0, not a strict
  cdec < tdec cap.* Rejected: the card scopes exactly one witness for this
  branch and excludes the finer/inexact corner by design; a strict
  cdec < tdec case is future coverage, not a gap in this pin.
- *the ref oracle (gate 5) cannot distinguish `afrondingDecimalen: null`
  from `0` for these whole values (floor_0 = identity).* Rejected: this is
  the card's intent — it witnesses a **clingo** translator branch. The IR
  faithfully records `null`, and the distinction is carried by gate 6 (no
  exactness viol emitted for the exact-null path) and gate 12, exactly where
  the card places it.

All twelve gates re-run green in the worktree: 1 self-contained · 2 ir-schema
(152, round-trip) · 3 verifier (152 golden / 161 negatief) · 4 case-schema
(379) · 5 oracle (357) · 6 clingo (357, ref ≡ clingo) · 7 noop (324 refute /
33 survivors, no buitplafond entry) · 8 rood (22) · 9 catala (169 checks, 11
goldens) · 10 regelspraak (130 tests, 19 rgs) · 11 toka-parity (30 loud
excludes, 0 fail) · 12 drools (played 53 incl. jutterswet-buitplafond
`PLAYED 2`, 0 exclusion added — `scripts/check-drools.sh` untouched).
