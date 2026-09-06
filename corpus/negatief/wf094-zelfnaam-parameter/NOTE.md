Violates WF-094 (`spec/IR-SPEC.md` ch. 2.1): a **keyed declaration's
self-name** that is not the key it is stored under — `vondstdrempeL` under the
key `vondstdrempel`. One capital, one character.

    WF-094 FAIL /parameters/vondstdrempel/naam: parameter self-name
    'vondstdrempeL' is not the key 'vondstdrempel' it is stored under: a keyed
    declaration carries its own name verbatim

**Why this is not a liberty.** The self-name is not a reference: nothing has to
resolve. It is the declaration's identity written a SECOND time — the v0
denormalisation of the one `name` a v1 `Parameter` carries on its node
(ch. 2.10). So ch. 2.1's two rules split here the way they split at the
side-table keys of ch. 4.6 (p): this is the **carriage** rule (declared names
are carried verbatim, whitespace-collapsed, case preserved), not the matching
rule, and the comparison is byte-exact.

**The measurement that decides byte over case-insensitive**, and it is this
table's, taken 2026-09-06 on `corpus/golden/jutterswet-penningdrempel` — the
golden this document is a hand-broken copy of — with the identical
one-character change:

| consumer | before | after |
| --- | --- | --- |
| verifier (before this card) | PASS | **PASS** |
| `ref` (`case.json`) | PASS, 3 expectations | **PASS, 3 expectations** |
| ASP backend (`case.json`) | PASS, 3 expectations | **PASS, 3 expectations** |
| bean-model executor (`case.json`) | PLAYED | **FAIL** — `kenmerk penninggerechtigd: verwacht aanwezig, werkelijk afwezig` |

Not a refusal: a **wrong answer**. Three consumers agree the document is fine
and the fourth computes a different program. The channel is a name meeting a
name: the case supplies its parameter values by the WRITTEN name and the
runner binds each global under `Namen.ident(<case key>)`, while the transpiler
registers its globals under `globalNaam(<self-name>)`. Differ by one capital
and the two identifiers part; the setter's "global not declared" is swallowed
by design (no rule reads it — except that here one does), so the parameter
simply never arrives and the rule computes on nothing.

*Measured breadth of that harm, one parameter self-name recapitalised at a
time over every golden:* **11 of the 34** golden parameter positions move, all
of them to a wrong value, over seven documents — `jutterswet-aanvoergeld`
(`aanvoergeld` 322.50 → 310.00), `jutterswet-penningdrempel`,
`jutterswet-penningjaar`, `jutterswet-schouwkalender`, `jutterswet-sleeprest`,
`synth3-drempelheffing`, `synth3-takbarriere`. The verifier, `ref` and the ASP
backend move on none of them.

*Measured cost:* none. Of the **1919** keyed declarations the four zones hold
across the five tables (`objecttypen` 744, `parameters` 654, `feittypen` 301,
`eenheidstelsels` 209, `dimensies` 11), every one carries a self-name and every
one spells its key byte for byte. The rule rejects nothing that was there; the
five `wf094-zelfnaam-*` witnesses are the only documents anywhere that
disagree.

A hand-broken variant of `corpus/golden/jutterswet-penningdrempel/ir.json` —
one character recapitalised, a one-line diff; schema-valid, since `naam` is a
plain string on the v0 wire.

Falsifiable, arm-exact: drop the `parameters` arm from `wf094`
(`spec/verifier/lib/checks.ml`) and this case, alone in all four zones, turns
clean (532 documents compared, one verdict moved, no report elsewhere).
Relax the arm's comparison from `n <> key` to `norm n <> norm key` and this
case turns clean together with its four sisters — every witness is a
capitalisation, so the ci relapse is measurable as a five-document move.
