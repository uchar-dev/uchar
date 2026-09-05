Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6 (p)): a **side-table key** of an
object type that names no declared member.

    WF-034 FAIL /objecttypen/Vondst/kenmerkSoorten/verjaart: kenmerkSoorten key 'verjaart' names no
    declared characteristic of 'Vondst': the side table resolves against no declaration

**What the table is.** An object type declares its members in two places —
`attributen` (name to datatype) and `kenmerken` (a name list) — and hangs
eight per-member FACET maps beside them, each keyed by a member name. Nothing
required those keys to name a declared member, and they are references: every
one of them is looked up by member name and by nothing else, so a key naming
no member resolves against no declaration.

**The harm is a silent semantic change, not a missing value.** A key spelled `verjaart` where the declared characteristic is `verjaard` gives
the read form (adjectival, possessive, nominal — ch. 2.2) to a characteristic
that does not exist, and leaves the declared one at the nominal default. This is
the presentation tag, so the harm is the mildest of the eight; the table is also
by far the largest, 788 of the 924 keys the four zones hold.

**Who reads this table** (measured 2026-09-05 across the four consumers):
**Nobody, today.** No check in the verifier reads it, `ref` does not, the ASP
backend does not, and the Drools backend's loader parses it into its object-type
record without any consumer reading it back. The arm is here for the WIRE: the
table is keyed by member name in the serialization contract, and a conforming
consumer may read it tomorrow — which is the same argument the wave-3
dimension-cell arm rests on (ch. 4.6 (g)), and it is stated here rather than
left to be found.

**Measured.** A spurious key added to this table on each of the 205 golden object types in
turn: **205 of 205** stayed verifier-clean before this card, none does now. The
renaming instrument, over all **124** live golden keys: 124 of 124 clean
before, 0 after.

**Executor observability.** Nothing sees it, and that is measured rather than assumed: renaming each live
key and replaying every case of its golden gave **438** measurements, and in all
438 `ref` and the ASP backend print a byte-identical status line before and
after.

*Measured breadth:* zero documents lose anything. All 788 `kenmerkSoorten` keys
the four zones held before this card name a declared characteristic — 478 of
them in the parity zone alone.

A hand-broken variant of `corpus/golden/jutterswet-vondstverjaring/ir.json` — one key added, a one-line diff;
schema-valid, since the eight tables are `additionalProperties`-typed maps on
the v0 wire.

Falsifiable, arm-exact: delete the `orphan otn "kenmerkSoorten" ...` line from
`wf034_zijtabelsleutels` (`spec/verifier/lib/checks.ml`) and this case, alone
in all four zones, turns clean — 522 documents compared, one verdict moved, no
report elsewhere.
