Violates WF-094 (`spec/IR-SPEC.md` ch. 2.1): a **keyed declaration's
self-name** that is not the key it is stored under — `seizoeN` under the key
`seizoen`. One capital, one character.

    WF-094 FAIL /dimensies/seizoen/naam: dimension self-name 'seizoeN' is not
    the key 'seizoen' it is stored under: a keyed declaration carries its own
    name verbatim

**Why this is not a liberty.** The self-name is not a reference; nothing has to
resolve. It is the declaration's identity written a SECOND time — the v0
denormalisation of the one `name` a v1 `Dimension` carries on its node
(ch. 2.7). Ch. 2.1's carriage rule governs it, not its matching rule, and the
comparison is byte-exact.

**One reader, and it is a LOOKUP.** Measured 2026-09-06: `Dimensie.naam` is
read at exactly **one** site anywhere — the bean-model executor's
by-name dimension lookup, which tries the map KEY exactly first and then falls
back to a scan comparing each declaration's SELF-NAME case-insensitively. So
the self-name is a second, ci-keyed index into the same table: a
recapitalisation leaves it working, a ci-different name silently removes the
fallback, and a dimension reference spelled in another capitalisation than the
key then resolves to nothing. The verifier reads the self-name once, in
WF-057's target-text-safety walk, never against the key; `ref` and the ASP
backend do not read it.

**Measured, and the measurement is that the corpus cannot execute this table
at all.** The four zones hold **11** dimension declarations (2 golden, 9
negatief), and the only two goldens that carry one — `jutterswet-seizoenvondst`
(this document's source) and `jutterswet-vormenkabinet` — are the two
**verifier-only** witness-cabinet documents: they carry no `case*.json`,
because no executor implements a dimension-cell read (`ref` faults `expression
kind not implemented`, the ASP backend `expression kind not translated`, the
bean-model executor auto-excludes a dimensioned attribute as a Map bean
member — see `corpus/golden/jutterswet-seizoenvondst/NOTE.md`). So:

| consumer | before | after |
| --- | --- | --- |
| verifier (before this card) | PASS | **PASS** |
| `ref` | *(no case file — the shape is not executable)* | — |
| ASP backend | *(no case file — the shape is not executable)* | — |
| bean-model executor | *(no case file — the shape is not executable)* | — |

Over both golden dimension positions, one self-name at a time, nothing moves
under a recapitalisation and nothing under a ci-different name — there is no
executor verdict to move. That is the `kenmerkTijdlijnAantal` situation of
ch. 4.6 (p) (an arm with no live golden key at all) one step further: an arm
with a live key and no live EXECUTION. The wire clause is what stands in for
the executor that is not there yet, and when the cell read becomes executable
this witness gains a case-carrying sister.

A hand-broken variant of `corpus/golden/jutterswet-seizoenvondst/ir.json` — one
character recapitalised, a one-line diff; schema-valid, since `naam` is a plain
string on the v0 wire.

Falsifiable, arm-exact: drop the `dimensies` arm from `wf094`
(`spec/verifier/lib/checks.ml`) and this case, alone in all four zones, turns
clean (532 documents compared, one verdict moved, no report elsewhere).
Relax the arm's comparison from `n <> key` to `norm n <> norm key` and this
case turns clean together with its four sisters.
