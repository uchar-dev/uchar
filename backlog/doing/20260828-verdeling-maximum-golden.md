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
