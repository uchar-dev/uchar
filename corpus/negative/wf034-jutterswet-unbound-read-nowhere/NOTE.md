# wf034-jutterswet-unbound-read-nowhere

Violates WF-034 (`spec/IR-SPEC.md` ch. 2.2/4.6), the name-resolution arm of
an attribute reference: the navigated read names `rang` on `Jutter`, and no
object type declares a member of that name.

The rule "De bonus van een Vondst moet berekend worden als de rang van de
jutter van de Vondst plus 1" reads `rang` over the role `jutter` of
`JutterHeeftVondst`. The document writes the read as the sentence names it:
`attribute: "rang"`, `subjectType: "Jutter"`, one binding over
`JutterHeeftVondst` / `jutter`. The walk resolves and lands on its endpoint;
the member does not.

    WF-034 FAIL /rules/rules/0/versions/0/value/left: 'rang' is neither an
    attribute nor a characteristic of 'Jutter'

The RegelSpraak frontend refuses the sentence before it writes a document,
in this sentence after a Dutch part that names the member, the filler and
the role, and says that no other object type declares the member
(`OngebondenLezingTest`; `frontends/regelspraak/grammar/DIVERGENCES.md`, "An
undeclared attribute read"). Measured before that refusal (2026-10-01): the
frontend wrote the read as an attribute `rang van de jutter` of `Vondst` with
`diagnostics: []`; the verifier refused that document under WF-034 on that
name, and `ref` and the ASP backend faulted on it (`"rang van de jutter" is
not a declared attribute of "Vondst": a value position reads an attribute`).

The document is the frontend's emission of the same model with the read "de
premie van de jutter van de Vondst" (verifier-clean), with the read's
`attribute` and snippets changed to what the sentence writes; nothing else
fires. `expect.txt` pins the arm.
