# wf053-jutterswet-unbound-read-subtype

Violates WF-053 (`spec/IR-SPEC.md` ch. 4.1a), the endpoint arm: the walk
lands on a role whose declared filler is `Jutter`, and the reference names
the strict subtype `Strandjutter` as its endpoint. The role holds any
`Jutter`; only a `Strandjutter` carries `toeslag`.

The rule "De bonus van een Vondst moet berekend worden als de toeslag van de
jutter van de Vondst plus 1" reads `toeslag`, which only `Strandjutter`
(⊑ `Jutter`) declares, over the role `jutter` of `JutterHeeftVondst`. The
document writes the read as the sentence names it: `attribute: "toeslag"`,
`subjectType: "Strandjutter"`, one binding over `JutterHeeftVondst` /
`jutter`.

    WF-053 FAIL /rules/rules/0/versions/0/value/left: the navigation reaches
    'Jutter', not the endpoint 'Strandjutter'

The RegelSpraak frontend refuses the sentence before it writes a document,
in this sentence after a Dutch part that names the member, the filler, the
role and the subtype (`OngebondenLezingTest`;
`frontends/regelspraak/grammar/DIVERGENCES.md`, "An undeclared attribute
read"). Measured before that refusal (2026-10-01): the frontend wrote the
read as an attribute `toeslag van de jutter` of `Vondst` with
`diagnostics: []`; the verifier refused that document under WF-034 (`'toeslag
van de jutter' is neither an attribute nor a characteristic of 'Vondst'`),
and `ref` and the ASP backend faulted on it (`"toeslag van de jutter" is not
a declared attribute of "Vondst": a value position reads an attribute`).

The document is the frontend's emission of the same model with the read "de
premie van de jutter van de Vondst" (verifier-clean), with the read's
`attribute`, `subjectType` and snippets changed to what the sentence writes;
nothing else fires. `expect.txt` pins the arm.
