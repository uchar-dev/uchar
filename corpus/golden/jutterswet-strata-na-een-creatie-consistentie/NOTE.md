# jutterswet-strata-na-een-creatie-consistentie

The consistency twin of `jutterswet-strata-na-een-creatie`
(`RUNTIME-SEMANTICS.md` ch. 6.3): a consistency of a lower stratum than a
creation is checked again over the object the creation made. H1 has `y` 1, so
it is not gesloten and therefore open; the creation "post voor een open
haven" (over `havenpost`, with `nummer` 1) gives it a Post, whose `a` is 5 by
"a van een post". The consistency "controle post", "de a van een Post moet
kleiner zijn dan 5", stands in a lower stratum than the creation, so the Post
does not exist yet when that stratum reaches its fixpoint; checked again after
the creation, it finds `a` 5 and puts a fact on the §9.5 channel. The case pins
H1 open and not gesloten, its `postwaarde` 5 and the fact of "controle post"
(without a label: the Post is a made object, which no case label names).

Measured 2026-11-06 (card `ref-strata-na-een-creatie`): the reference
interpreter from before the generation loop of
`creatie-initialisatie-leest-groeiende-waarde` (`b5a93a659`, the document on
its wire) gives `MISMATCH: expected inconsistent (a channel fact from rule
"controle post"), but the channel carries: (empty)`: the consistency fired at
the fixpoint of its stratum, before the Post existed, and not again. Ref,
clingo and both Drools branches play the case.
