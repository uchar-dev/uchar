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
