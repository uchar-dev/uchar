# wf107-blanco-regelnaam

Violates **WF-107** (`spec/IR-SPEC.md` ch. 3.1): the single `ResolvedRegel`
carries `naam: "   "` — present, and empty once whitespace is collapsed.

    WF-107 FAIL /resolvedRegels/0/naam: form in resolvedRegels carries a
    blank name (a named form's identity on this wire IS its name — ch. 8
    groups versions by it, the ch. 5.1 dependency graph keys its channel node
    on it, and WF-020 keeps it unique)

The BLANK arm, kept beside `wf107-naamloze-regel` because the two fall
independently: a check written as "the field is present" — the cheapest way
to write it, and the tempting one, since the schema already spells the field
nullable — passes this document and refuses that one. Emptiness is the half
that matters downstream, since a blank name is no more usable as an identity
than an absent one: WF-020 compares names case-insensitively over collapsed
whitespace, so two blank names are one name and every blank rule collides
with every other.

Falsifiable in one edit: put any non-blank name back.

Minimal hand-broken variant of `corpus/golden/jutterswet-penningdrempel`.
