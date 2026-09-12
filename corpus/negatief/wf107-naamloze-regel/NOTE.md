# wf107-naamloze-regel

Violates **WF-107** (`spec/IR-SPEC.md` ch. 3.1): the single `ResolvedRegel`
carries `naam: null`. The document says nothing else wrong.

    WF-107 FAIL /resolvedRegels/0/naam: form in resolvedRegels carries no
    name (a named form's identity on this wire IS its name — ch. 8 groups
    versions by it, the ch. 5.1 dependency graph keys its channel node on it,
    and WF-020 keeps it unique)

Why it is a rule and not a schema clause: the wire type is
`["string", "null"]` and stays that way, so this document is well formed and
reaches every consumer. Measured before the rule (2026-09-12): the verifier
passed it, `ref` and the ASP backend evaluated it without noticing, the
Drools backend's exec-model branch played it — and its DRL-text branch threw
a `NullPointerException` out of the rule-name escaper, which calls
`String.replace` on the name it is handed. One verifier-clean document, four
consumers, three answers and one crash.

Falsifiable in one edit: put any non-blank name back.

Its two sisters fall independently. `wf107-blanco-regelnaam` carries the
*blank* half — a presence-only check admits it — and
`wf107-naamloze-defaultsgroep` carries the array WF-020's name list never
reached at all.

Minimal hand-broken variant of `corpus/golden/jutterswet-penningdrempel`,
whose rule is named.
