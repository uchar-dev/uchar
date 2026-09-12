# wf020-defaultsgroep-botst-met-regel

Violates **WF-020** (`spec/IR-SPEC.md` ch. 3.1): the `PrioritizedDefaults`
group is named `penninggerechtigd tenzij uitgesloten`, which is already the
name of the `ResolvedRegel` in the same document.

    WF-020 FAIL /prioritizedDefaults/0: name 'penninggerechtigd tenzij
    uitgesloten' collides with 'penninggerechtigd tenzij uitgesloten' in
    resolvedRegels (unique across all named forms)

The CROSS-KIND shape, and the reason WF-020's name list grew rather than a
new number being minted: uniqueness of form names is one property, and it
already said "across all rule forms" — the defaults group was simply missing
from the list the check read. A separate number would have made the same
collision cite one rule between two rules and another between a rule and a
group.

Its sister `wf020-crossform-collision` carries the same property between two
of the seven ch. 3 forms; the two fall independently, because a check reading
only the ch. 3 list refuses that one and passes this one.

Falsifiable in one edit: give the group any other name.

Minimal hand-broken variant of `corpus/golden/anchor/jutterswet-uitsluiting`,
where the rule and the group are two named forms with two names.
