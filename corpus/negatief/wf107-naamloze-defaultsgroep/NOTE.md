# wf107-naamloze-defaultsgroep

Violates **WF-107** (`spec/IR-SPEC.md` ch. 3.1): the single
`PrioritizedDefaults` group carries `name: null`.

    WF-107 FAIL /rules/prioritizedDefaults/0/name: form in prioritizedDefaults
    carries no name (a named form's identity on this wire IS its name —
    ch. 8 groups versions by it, the ch. 5.1 dependency graph keys its channel
    node on it, and WF-020 keeps it unique)

The defaults group is the array the name rules never reached. WF-020's name
list held the seven versioned rule forms of ch. 3 and not this one, so before
WF-107 a nameless group, a duplicate between two groups and a collision
between a group and a rule all three passed the verifier — the group's name
was on the wire and crossed by nothing. Its two duplicate shapes are
`wf020-defaultsgroep-duplicaat` and `wf020-defaultsgroep-botst-met-regel`;
this case is the presence half.

Falsifiable in one edit: put the group's name back.

Minimal hand-broken variant of `corpus/golden/anchor/jutterswet-nestkenmerk`,
whose group is named (until M1 it was also named the same as its `regels`
index entry, which is where the name was read from; the `regels` stubs left
the wire at M1, delta B3).
