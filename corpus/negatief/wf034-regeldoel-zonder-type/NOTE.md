Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6): a rule **target** carrying no
static type — the unspelled twin of the sentinel that
`wf034-onbekend-regeldoel` pins, at the same position and for the same reason.

    WF-034 FAIL /rules/resolvedRegels/1/doel/type: rule target without a static type

The expression walk refuses both spellings of the unknown type on every value
node it enters (`unknown-type sentinel inside a structured node` and `value
node without a static type`, ch. 4.6), and the walk does not enter the target
lvalue — that is WF-022's. So the target needed each spelling stated
separately: the sentinel on 2026-09-04, the absent type here. The earlier
clause deliberately left this half out for want of a witness; this is that
witness.

Why it matters, measured. Nobody READS the field — all three executors write
through the target's DECLARED datatype — but two rules CROSS it, and both stand
down on an absent type:

* **WF-091's rule-target arm** crosses the target's serialized type against the
  declaration (unit, precision, sign, residual structure). It defers when
  either side is missing, so an emitter that omits the field switches the unit
  and precision checks off its own write targets.
* **WF-007's target clause** (landed 2026-09-03, one card earlier) reads
  `Some d` and has nothing to say about `None`, so the precision-form law goes
  with it — on a CHARACTERISTIC target, where WF-091 defers by construction,
  that clause is the only guard there is.

One absent field, both of the last two cards' closures switched off, and the
document stays verifier-clean.

Measured before the closure, on this document against
`corpus/golden/jutterswet-aanvoergeld/case.json`:

    ref    : oracle: PASS (7 expectations)
    clingo : clingo: PASS (7 expectations)
    Drools : ERROR ConsequenceException: Exception executing consequence for
             rule "aanvoergeld van een jutter" in defaultpkg:
             java.lang.NullPointerException: Cannot invoke
             "…model.Datatype.eenheidOf()" because "naar" is null

This is the dangerous member of the family, and the reason it is dangerous is
the ASYMMETRY: the two OCaml executors replay the document to its pinned
expectations without a murmur, so nothing downstream marks it, while the third
throws out of the engine on a document the verifier called well-formed. A
verifier-clean document that crashes an executor is exactly the class an
engine-neutral IR must not admit.

*Measured dracht:* zero outside this witness — 152 golden + 22 rood + 30
parity documents and the 238 negatief cases that predate this card carry a
type on **every** rule target (295 of 295 in the goldens). A sweep nulling all
295 golden target types one at a time left **295** verifier-clean survivors
before this clause: the position was entirely unfenced. Over-rejection is
therefore impossible by measurement.

A hand-broken variant of `corpus/golden/jutterswet-aanvoergeld/ir.json` — one
field set to `null`, a one-field diff. The shape is schema-valid: `type` is
`anyOf [datatype, null]` on the v0 wire, so an explicit `null` passes where a
MISSING key would not (the key is required; dropping it is a loader error, not
a well-formedness question). Which is precisely why the verifier, not the
schema, is the fence.

Falsifiable, clause-exact: remove the `typ = None` arm of the rule-target
clause in `expression_layer` and this case alone turns clean, every other
`wf034-*` case staying red.
