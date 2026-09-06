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

*How that 105 was counted, so it can be re-taken.* The field was renamed in a
throw-away copy and the Java compiler asked to name every read — and it has to
be asked with `-Xmaxerrs 5000`, because javac stops at **100** diagnostics by
default: a bare compile of this backend reports exactly 100 and silently caps
the very number that makes this the largest readership of the five. Uncapped it
reports **106** positions, the 106th being the declaring class's own
constructor assignment — a write, not a read — which leaves the 105 above.

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
clause says so.* Over all **205** golden object-type positions — **203** of
them in a golden that has a case file and can be replayed at all — one
self-name
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
'strandmaat' op io.github.yhavinga.regelspraak.facts.Jutterzz`). A drop-out out
of the differential square in three cases, a crash in the fourth,
with the verifier, `ref` and the ASP backend clean throughout; three further
positions move only the TEXT of an exclusion the backend already issued.

*What the gate sees of that drop-out, and what it does not.* When this was
measured, a golden that fell out of the played channel changed nothing gate 12
reported: its only floor was a NON-EMPTY played set, so the differential shrank
by one and the gate stayed green — loud about the form, mute about the loss.
That half is closed since the **expected-channel floor** landed the same day
(`scripts/check-drools.sh`): every non-played golden is pinned by name, PLAYED
is the default, and a player that quietly becomes an exclusion now fails BY
NAME, in both directions. What the floor cannot do is notice a document that
arrives already broken — it compares channels against a list, not spellings
against each other — and it is a gate over the GOLDEN corpus, so it says
nothing about the document a producer hands a consumer tomorrow. **The wire
still has to catch what no gate catches**, which is what this document is for.

The byte rule here therefore rests on the CONSTRUCT rather than on a
capitalisation harm this corpus can show — the same ground the two reader-less
side-table arms of ch. 4.6 (p) rest on. One further byte-exact reader was named
here as unexercised, and the second lens measured it instead of assuming: the
§9.9 startpunt-anchor lookup compares `resolvedStartpunten[].objecttype` to the
object type's self-name with `String.equals`. No GOLDEN can exercise it — the
only one carrying a `resolvedStartpunten` entry is a verifier-only witness with
no case file — but **two parity documents carry such an entry WITH a case, and
gate 11 replays both through all three executors.** Twisting the self-name of
the anchored object type there, both ci-EQUAL and ci-DIFFERENT, moves nothing:
`ref` PASS, the ASP backend PASS, the bean-model executor PLAYED, before and
after, on both documents and under both twists (the verifier turns red on all
four, on this rule, which is the point). So that reader is REACHABLE and its
observable channel is measured EMPTY — a stronger statement than the one it
replaces, and a narrower one: it says the corpus found no harm there, not that
the corpus could not look.

A hand-broken variant of `corpus/golden/anchor/jutterswet-maatschap/ir.json` —
one character recapitalised, a one-line diff; schema-valid, since `naam` is a
plain string on the v0 wire.

Falsifiable, arm-exact: drop the `objecttypen` arm from `wf094`
(`spec/verifier/lib/checks.ml`) and this case, alone in all four zones, turns
clean (532 documents compared, one verdict moved, no report elsewhere). This
arm is the HEAD of the `@`-chain, so deleting its line alone does not compile:
the next line has to lose its leading `@` in the same edit, or the mutant fails
to build and the sweep quietly re-runs the previous binary — a green that means
nothing.
Relax the arm's comparison from `n <> key` to `norm n <> norm key` and this
case turns clean together with its four sisters.
