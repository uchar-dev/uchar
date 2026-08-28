# jutterswet-veilinginvoer

The **case-input-on-an-allocation-target** witness (ASP-TRANSLATION S10/S12).
One equal-shares allocation (`GELIJKE_DELEN`, RUNTIME-SEMANTICS ch. 4.2)
distributes a strandveiling's `veilingopbrengst` over its `veilingjutters`
into each jutter's `veilingdeel`, writing the undivided remainder into
`veilingrestant` (ch. 4.4). The case pre-fills `veilingdeel` on every jutter,
so the golden witnesses BOTH halves of the defeasible-input layer at once:

- **the allocation OVERWRITES** the supplied input on every jutter it
  reaches (World.write_attr, ch. 4 — ref, like v0, replaces the target);
- **the supplied input STANDS** on a jutter the allocation does not reach
  (no connecting link, so it is never a recipient and is never written).

## Hand-computed absolutes (RUNTIME-SEMANTICS ch. 4.2 / 4.4)

`veilingopbrengst = 100`, recipients `veilingjutters = {J1, J2, J3}` (n = 3);
equal share `= ⌊100 / 3⌋ = 33` (floor, ch. 4.2); undivided remainder
`= 100 − 3·33 = 100 − 99 = 1` (ch. 4.4).

| jutter | linked to V1 | case input | outcome | reason |
|--------|--------------|------------|---------|--------|
| J1     | yes          | 7          | **33**  | allocation reaches it → overwrite |
| J2     | yes          | 7          | **33**  | allocation reaches it → overwrite |
| J3     | yes          | 7          | **33**  | allocation reaches it → overwrite |
| J4     | no           | 5          | **5**   | never a recipient → input stands  |

`V1.veilingrestant = 1`. The channel is empty → `consistent`.

## noop refutation

The three overwrites (`7 → 33`) and the written remainder (`∅ → 1`) each
differ from the raw input, so `runner --noop` fails (gate 7): the golden is
no survivor — it pins the overwrite, not a standing input.

## Red-first (clingo)

Before this card the clingo translator refused any allocation whose case
supplies the target — `TRANSLATION FAULT: the case supplies input for
allocation target "veilingdeel"…` (exit 1). Now the S10 layer carries it:
`veilingdeel` case input enters as `inputval/3`, the per-allocation
effectiveness helper `vd0_eff/1` fires exactly on the reached jutters, and
`val(D,veilingdeel,V) :- inputval(D,veilingdeel,V), is_a(D,Jutter), not
vd0_eff(D)` re-admits the input on J4. ref ≡ clingo over all six
expectations (gate 5 ≡ gate 6).

The stratification fence stays loud: were `veilingdeel` read anywhere that
could feed back into the pot, weights, criteria or condition, the translator
would refuse rather than emit an unstratifiable `not vd0_eff` cycle.
