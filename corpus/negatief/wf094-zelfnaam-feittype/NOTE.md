Violates WF-094 (`spec/IR-SPEC.md` ch. 2.1): a **keyed declaration's
self-name** that is not the key it is stored under — `vondstaanmeldinG` under
the key `vondstaanmelding`. One capital, one character.

    WF-094 FAIL /feittypen/vondstaanmelding/naam: fact type self-name
    'vondstaanmeldinG' is not the key 'vondstaanmelding' it is stored under: a
    keyed declaration carries its own name verbatim

**Why this is not a liberty.** The self-name is not a reference; nothing has to
resolve. It is the declaration's identity written a SECOND time — the v0
denormalisation of the one `name` a v1 `FactType` carries on its node
(ch. 2.6). Ch. 2.1's carriage rule governs it, not its matching rule, and the
comparison is byte-exact.

**A small, honest readership.** Measured 2026-09-06: `Feittype.naam` is read at
**three** sites in the bean-model executor, all three in the shared model
helpers, and all three do the same thing — they mint a role binding
(`RolBindingRef(<fact-type self-name>, <role>)`) that a later transpiler pass
resolves by scanning `feittypen` and comparing the map KEY
case-insensitively. So the self-name is written into a value that is matched
back against the key: a recapitalisation survives that round trip, a
ci-different name does not. The verifier reads the self-name once, in WF-057's
target-text-safety walk, never against the key; `ref` and the ASP backend do
not read it at all.

**Measured**, on `corpus/golden/jutterswet-vondsttelling` — the golden this
document is a hand-broken copy of — with the identical one-character change:

| consumer | before | after |
| --- | --- | --- |
| verifier (before this card) | PASS | **PASS** |
| `ref` (`case.json`) | PASS, 7 expectations | **PASS, 7 expectations** |
| ASP backend (`case.json`) | PASS, 7 expectations | **PASS, 7 expectations** |
| bean-model executor (`case.json`) | PLAYED | **PLAYED** |

Over all **53** golden fact-type positions, one self-name at a time, nothing
moves under a recapitalisation and nothing moves under a ci-DIFFERENT name
either — 0 of 53 in both instruments. The three reader sites are real code but
the corpus does not exercise the paths that mint those bindings from the model
side; the ones the goldens do carry come off the wire.

So this arm too is for the WIRE and for the construct, not for a harm this
corpus can show — the ground the reader-less side-table arms of ch. 4.6 (p)
stand on. Stating that plainly is the point: an arm whose measured cost AND
measured harm are both zero is still the same identity, and a rule that skipped
it would say the self-name means something different per table.

A hand-broken variant of `corpus/golden/jutterswet-vondsttelling/ir.json` — one
character recapitalised, a one-line diff; schema-valid, since `naam` is a plain
string on the v0 wire.

Falsifiable, arm-exact: drop the `feittypen` arm from `wf094`
(`spec/verifier/lib/checks.ml`) and this case, alone in all four zones, turns
clean (532 documents compared, one verdict moved, no report elsewhere).
Relax the arm's comparison from `n <> key` to `norm n <> norm key` and this
case turns clean together with its four sisters.
