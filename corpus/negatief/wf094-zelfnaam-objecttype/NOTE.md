Violates WF-094 (`spec/IR-SPEC.md` ch. 2.1): a **keyed declaration's
self-name** that is not the key it is stored under — `JutteR` under the key
`Jutter`. One capital, one character.

    WF-094 FAIL /objecttypen/Jutter/naam: object type self-name 'JutteR' is not
    the key 'Jutter' it is stored under: a keyed declaration carries its own
    name verbatim

**Why this is not a liberty.** The self-name is not a reference; nothing has to
resolve. It is the declaration's identity written a SECOND time — the v0
denormalisation of the one `name` a v1 `ObjectType` carries on its node
(ch. 2.2). Ch. 2.1's carriage rule therefore governs it, not its matching rule,
exactly as at the side-table keys of ch. 4.6 (p), and the comparison is
byte-exact.

**This is the table with the largest readership, and it splits the executors.**
The v0 loader of the bean-model executor takes the SELF-NAME as the object
type's name (falling back to the key only when the field is absent) and reads
it back at **105 sites** — the DRL-text transpiler 41, the exec-model
transpiler 33, the case runner 8, the fact-model generator 8 (the generated
bean's own class name is `ident(<self-name>)`), the contract validator 6, the
producer/consumer graph 4, the model helpers 3, stratification 2. The verifier
reads it exactly once, in WF-057's target-text-safety walk, and never against
the key; `ref` does not read it at all; neither does the ASP backend.

**Measured 2026-09-06**, on `corpus/golden/anchor/jutterswet-maatschap` — the
golden this document is a hand-broken copy of — with the identical
one-character change:

| consumer | before | after |
| --- | --- | --- |
| verifier (before this card) | PASS | **PASS** |
| `ref` (`case1.json`) | PASS, 7 expectations | **PASS, 7 expectations** |
| ASP backend (`case1.json`) | PASS, 7 expectations | **PASS, 7 expectations** |
| bean-model executor (`case1.json`) | PLAYED | **PLAYED** |

*So the capitalisation flank of THIS table has no executor harm today, and the
clause says so.* Over all **205** golden object-type positions, one self-name
recapitalised at a time, three reports move and **no verdict does** — and the
three are exclusion MESSAGES on goldens the backend already declines. What the
same instrument shows one step further out, with the self-name made
ci-different (`Jutterzz`), is that the readership is real: **four** of those
205 positions move verdict. Three go PLAYED → EXCLUDE
(`anchor/jutterswet-maatschap` itself, `anchor/jutterswet-wachtersketen`,
`synth2-budgetgrens-onder` — the self-pair reference field stops being derived,
because that derivation compares the ROLE's filler name, which spells the KEY,
against the object type's self-name), and one goes PLAYED → ERROR
(`jutterswet-aanmelderkrediet`: `IllegalStateException: geen getter voor
'strandmaat' op io.github.yhavinga.regelspraak.facts.Jutterzz`). A silent
drop-out out of the differential square in three cases, a crash in the fourth,
with the verifier, `ref` and the ASP backend clean throughout; three further
positions move only the TEXT of an exclusion the backend already issued.

The byte rule here therefore rests on the CONSTRUCT rather than on a
capitalisation harm this corpus can show — the same ground the two reader-less
side-table arms of ch. 4.6 (p) rest on — plus one byte-exact reader that the
corpus cannot exercise today: the §9.9 startpunt-anchor lookup compares
`resolvedStartpunten[].objecttype` to the object type's self-name with
`String.equals`, and the only golden carrying a `resolvedStartpunten` entry is
a verifier-only witness with no case file.

A hand-broken variant of `corpus/golden/anchor/jutterswet-maatschap/ir.json` —
one character recapitalised, a one-line diff; schema-valid, since `naam` is a
plain string on the v0 wire.

Falsifiable, arm-exact: drop the `objecttypen` arm from `wf094`
(`spec/verifier/lib/checks.ml`) and this case, alone in all four zones, turns
clean (532 documents compared, one verdict moved, no report elsewhere).
Relax the arm's comparison from `n <> key` to `norm n <> norm key` and this
case turns clean together with its four sisters.
