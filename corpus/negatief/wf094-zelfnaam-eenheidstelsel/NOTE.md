Violates WF-094 (`spec/IR-SPEC.md` ch. 2.1): a **keyed declaration's
self-name** that is not the key it is stored under — `MunT` under the key
`Munt`. One capital, one character.

    WF-094 FAIL /eenheidstelsels/Munt/naam: unit system self-name 'MunT' is not
    the key 'Munt' it is stored under: a keyed declaration carries its own name
    verbatim

**Why this is not a liberty.** The self-name is not a reference; nothing has to
resolve. It is the declaration's identity written a SECOND time — the v0
denormalisation of the one `name` a v1 `UnitSystem` carries on its node
(ch. 2.5). Ch. 2.1's carriage rule governs it, not its matching rule, and the
comparison is byte-exact.

**This is the arm with no reader at all, and that is stated rather than
smoothed over.** Measured 2026-09-06 across the four consumers:
`Eenheidstelsel.naam` is read at **zero** sites in the bean-model executor —
its loader parses the field and nothing reads it back — the verifier never
touches it (WF-057 checks unit-system names through the map KEY only), and
`ref` does not read it. Its ONE reader anywhere is the ASP backend's
finest-unit derivation, which splices the self-name into two FAULT MESSAGES
(`unit stelsel %S declares no plain base unit to normalise` and its
admits-no-finest sibling). That reader cannot change WHETHER the backend
faults, only what the refusal says — and no `expectedFault` in the red corpus
pins either fragment, so even the text has no observable channel today. It is
named here because a fault message is not a semantic reader and the clause must
not be read as if it were.

**Measured**, on `corpus/golden/jutterswet-berging` — the golden this document
is a hand-broken copy of — with the identical one-character change:

| consumer | before | after |
| --- | --- | --- |
| verifier (before this card) | PASS | **PASS** |
| `ref` (`case.json`) | PASS, 5 expectations | **PASS, 5 expectations** |
| ASP backend (`case.json`) | PASS, 5 expectations | **PASS, 5 expectations** |
| bean-model executor (`case.json`) | PLAYED | **PLAYED** |

Over all **21** golden unit-system positions, one self-name at a time, nothing
moves under a recapitalisation and nothing moves under a ci-DIFFERENT name
either — 0 of 21 in both instruments, in all four consumers.

So this arm is for the WIRE, on the same ground the two reader-less side-table
arms of ch. 4.6 (p) rest on: the map is keyed by the declared name in the
serialization contract, the value's `naam` is that same identity written out
beside it, and a conforming consumer may read either tomorrow. It is also why
this table's open-list entry — the one `Eenheidstelsel.naam` post that stood in
ch. 4.6 since the sixth wave — could never be settled on its own: the harm of
the CLASS sits on its sister tables, and closing only the table with no reader
would have pinned the wrong thing.

A hand-broken variant of `corpus/golden/jutterswet-berging/ir.json` — one
character recapitalised, a one-line diff; schema-valid, since `naam` is a plain
string on the v0 wire.

Falsifiable, arm-exact: drop the `eenheidstelsels` arm from `wf094`
(`spec/verifier/lib/checks.ml`) and this case, alone in all four zones, turns
clean (532 documents compared, one verdict moved, no report elsewhere).
Relax the arm's comparison from `n <> key` to `norm n <> norm key` and this
case turns clean together with its four sisters.
