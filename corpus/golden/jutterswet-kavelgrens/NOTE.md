# jutterswet-kavelgrens

The **self-typed 1:1 witness** for the case-input law
(`RUNTIME-SEMANTICS.md` ch. 5.7a, maintainer decision 2026-09-10). Its
sister `jutterswet-ketenband` pins the same law on a 1:N relation, where
one stored reference necessarily serves both directions. This golden
pins the shape ch. 5.7a consequence 2 singles out as the *other* one: a
**1:1** relation, where a reference-field fact model holds TWO fields —
one per to-one role — so each direction has its own storage and nothing
in the model forces a one-sided delivery to be visible from the far end.
That it IS visible is a decision, and this golden is where the decision
is pinned with absolute values in all three executors.

## The shape

One self-referential fact type, two distinctly named roles, **both
to-one**, on one object type, and two rules that each navigate through
the other role:

```
Feittype kavelgrens
    de noordbuur (mv: noordburen) Duinkavel      to-one
    de zuidbuur  (mv: zuidburen)  Duinkavel      to-one
Eén noordbuur heeft één zuidbuur

noordbuurbreedte := de kavelbreedte van de noordbuur van het Duinkavel
zuidbuurbreedte  := de kavelbreedte van de zuidbuur van het Duinkavel
```

The IR carries `reciprocal: false` on the fact type and `many: false` on both
roles — a declared plural (`noordburen`, `zuidburen`) does NOT make a role
to-many; the cardinality comes from the `Eén … heeft één …` sentence, and the
wire shows `plural` and `many` side by side to say so. The emitter binds both
reads by NAME (`path [{kavelgrens, noordbuur}]` and `[{kavelgrens, zuidbuur}]`);
the by-pair derivation is ambiguous on a self-typed pair by construction and is
never reached.

## The three cases: one chain, three spellings

K1, K2, K3 with `kavelbreedte` 10, 20, 30, and TWO relation instances
throughout: **K1 is the noordbuur of K2, and K2 is the noordbuur of
K3.** Each case spells them differently.

| case | the case supplies |
|---|---|
| 1 | `K1{zuidbuur:[K2]}`, `K2{noordbuur:[K1], zuidbuur:[K3]}`, `K3{noordbuur:[K2]}` — both legs written out |
| 2 | `K1{zuidbuur:[K2]}`, `K2{zuidbuur:[K3]}` — the `zuidbuur` legs only |
| 3 | `K2{noordbuur:[K1]}`, `K3{noordbuur:[K2]}` — the `noordbuur` legs only |

The middle kavel is the point of the three-instance chain: K2 holds a
noordbuur AND a zuidbuur at once, so both bean fields are occupied on
one instance and a swapped resolution cannot cancel out.

## Hand-computed absolutes (identical in all three cases)

| Duinkavel | `kavelbreedte` | `noordbuurbreedte` | `zuidbuurbreedte` |
|---|---|---|---|
| K1 | 10 | **leeg** | **20** |
| K2 | 20 | **10** | **30** |
| K3 | 30 | **20** | **leeg** |

The identity of the three rows IS the law: how a case spells a relation
does not change what the model computes from it. The load-bearing
entries name the direction the case did not supply — case 2 reads
`K2.noordbuurbreedte = 10` and `K3.noordbuurbreedte = 20` although no
case role line in it names `noordbuur` at all, and case 3 reads
`K1.zuidbuurbreedte = 20` and `K2.zuidbuurbreedte = 30` although no case
role line in it names `zuidbuur`.

The two `leeg` entries are the flank: K1 has no noordbuur and K3 has no
zuidbuur, so ch. 5.8 rule 1 reads leeg. Establishing a relation in both
directions is not inventing one — a mirror that over-fired would find a
kavelbreedte at the ends of the chain.

Each case carries a seventh expectation, `{"kind": "consistent"}`: the §9.5
error channel is empty. Hand-confirmed from the wire rather than from a run —
the document declares `consistencies`, `uniquenesses`, `allocations`,
`objectCreations`, `factCreations` and `ruleGroups` all EMPTY and carries no
prioritized defaults, and its two rules are plain navigated attribute
assignments, so no construct that deposits an error fact is present at all. It
is pinned anyway, because without the pin a spurious channel deposit — the one
class of wrong answer that changes no attribute — would be invisible here.

## Red-first

1. **The values were pinned nowhere before.** Measured over the
   replayable zones: **five** documents besides this one carry a
   non-reciprocal self-typed fact type with both roles to-one —
   `anchor/jutterswet-erflijn`, `anchor/jutterswet-wachtersketen`,
   `synth2-budgetgrens-onder`, `corpus/red/finite-chain-over-budget`
   and `corpus/red/runaway-recursion`. (Two more carry the shape in
   `corpus/negative` — `wf012-ambiguous-roles`,
   `wf033-no-creation-member` — and fall **outside** this count by
   construction. The criterion is REPLAY, not the presence of pinned
   values: golden and red are both replayed — golden pins values, red
   pins the fault it must raise, and neither of the red zone's 26 cases
   carries an `expected` at all — whereas a negative document is only
   ever READ, by the verifier, and never executed. A shape that is
   never run cannot witness what an executor does with it.)

   What separates this golden from all five is narrower than "supplies
   and reads", and worth stating exactly. `anchor/jutterswet-erflijn`
   does supply `opvolging.opvolger` from case input **and** read that
   relation — not from a rule, but through the joins of its two fact
   creations, both of whose `path` ends in the same
   binding `{opvolging, opvolger}`. It reads the
   role the case **supplied**, though. The other four supply no role
   keys at all (object creation, or no relation read). So the property
   no corpus document had before is this one: **reading the direction
   the case did not supply**, on a self-typed 1:1 relation. Cases 2 and
   3 do exactly that, in opposite directions.
2. **The directed reading is refuted, in all four lenses.** Re-run
   cases 2 and 3 with the expectations a non-mirroring loader would
   produce — the unsupplied direction reading leeg:

   ```
   case2  ref      MISMATCH: K2.noordbuurbreedte = 10, expected leeg
                   MISMATCH: K3.noordbuurbreedte = 20, expected leeg
          clingo   idem
          Drools exec-model  FAIL: verwacht leeg, werkelijk 10; verwacht leeg, werkelijk 20
          Drools DRL-tekst   idem
   case3  ref      MISMATCH: K1.zuidbuurbreedte = 20, expected leeg
                   MISMATCH: K2.zuidbuurbreedte = 30, expected leeg
          clingo, both Drools branches  idem
   ```
3. **A do-nothing refutes every case** (gate 7): under `--noop` each of
   the three mismatches four computed values, so no survivor entry is
   needed.
4. **PLAYED is a real value comparison.** One expectation twisted
   (10 → 99) on case 2:

   ```
   Drools exec-model  FAIL case_mut.json: attribuut noordbuurbreedte: verwacht 99, werkelijk 10
   Drools DRL-tekst   idem
   ref                MISMATCH: K2.noordbuurbreedte = 10, expected 99
   clingo             MISMATCH: K2.noordbuurbreedte = [10], expected 99
   ```

## The differential square

verifier PASS; `ref` PASS (7 expectations per case) ≡ `clingo` PASS
(7 per case) over all 21, anchored to the hand-written absolutes above;
Drools PLAYS all three cases in BOTH branches — the exec-model path
(gate 12) and the classic DRL-text path (`--drl`, the branch gate 13
also drives). ref ≡ clingo ≡ Drools, the first corpus witness for which
that holds on a self-typed **1:1** relation supplied from one end only.

Gate 13: no `Number` here carries a `unit`, so the golden
falls outside the twist set and moves no gate-13 count. Giving
`kavelbreedte` a unit would pull it in and would force a re-measurement
of the DRL-branch floor; that is extra work without demonstrated risk,
so the deliberate default is no unit.
