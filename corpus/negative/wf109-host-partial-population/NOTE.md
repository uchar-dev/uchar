Violates WF-109 (`spec/IR-SPEC.md` ch. 4.6 (q2)) on its **partial-population
flank at the guard of a fact creation**: the host names a STRICT SUBTYPE of the
target, one of the two objects the guard binds, so the guard would read some
targets and have no reading for the others.

    WF-109.only-some-objects FAIL /rules/factCreations/0/versions/0/condition/host:
    the characteristic check is hosted on 'Seinmast', which no object bound
    here IS: only SOME objects of the target 'Sein' are one, so the host would
    hold for some of them and fault for the rest

Broken from `corpus/golden/jutterswet-seinhijsing` by adding one declaration and
changing one field: a `Seinmast` subtype of `Sein`, and the host of the first
fact creation's guard from `Sein` to `Seinmast`. `gehesen` is inherited, so
WF-100 is satisfied and this guard is the only one speaking.

Why the guard and not a rule. At a position that binds ONE object — a rule's
subject, the member of a filter or a quantifier's test — a host strictly below
that object's type is the narrowing host (ch. 4.6 (q2); `RUNTIME-SEMANTICS.md`
ch. 5.8b rule 4): the check reads the object's own characteristic, true on an
instance of the host that has it and false on every other object, and the
golden `jutterswet-ploegblokkade` replays it. The guard of a fact creation binds
TWO objects, the anchor and the target, and the host is what says which of them
the guard is about. A host below the target is satisfied by neither binding and
overlaps one, so it names no bound object: there is no reading for a target
outside `Seinmast`, and the guard keeps this refusal.

Falsifiable in two directions, each verifier-checked on 2026-10-27; **one
turns this case clean and the other moves it to another rule**:

- put `Sein` back in the field — clean, the golden; an empty field is clean as
  well, and then the guard is an anchor-side check, which the anchor `Kustvak`
  answers through its own `gehesen`;
- drop the `Seinmast` declaration — NOT clean. The host then resolves to
  nothing and the document is **WF-034 (q)** instead (`WF-034.host-undeclared`),
  a different rule and a different report; WF-109 steps aside for an undeclared
  host.
