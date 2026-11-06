# jutterswet-strata-na-een-creatie

Ch. 6.3 of `RUNTIME-SEMANTICS.md`: the forms of a lower stratum reach their
fixpoint before a form of a higher stratum fires, also over an object a
creation made. A Haven gets a Post ("post voor een open haven", over
`havenpost`, with `nummer` 1) once it is open. The Post's `a` is 5 by "a van
een post" (stratum 0); "gemarkeerde post" marks a Post whose `a` is not 5
(stratum 1, a characteristic assignment reads strictly); "bewaakte haven"
reads whether the Haven's post is gemarkeerd. The reader stands first in the
document, before the writer of `a`.

On the new Post the writer of `a` must come to rest before the marker reads
it: then `a` is 5, the Post is not gemarkeerd and H1 is not bewaakt. Read on
the intermediate state where `a` is still leeg, "leeg ongelijk aan 5" holds
(ch. 1.2), the Post would be gemarkeerd and H1 bewaakt. The case pins H1 open,
its `postwaarde` 5 (the `a` of the one Post) and H1 not bewaakt.

Measured 2026-11-06 (card `ref-strata-na-een-creatie`): the reference
interpreter from before the generation loop of
`creatie-initialisatie-leest-groeiende-waarde` (`b5a93a659`, the document on
its wire) gives `MISMATCH: H1 carries characteristic "bewaakt", expected
absent`: it ran the forms of strata 0 to k in one pass in document order after
a creation. Ref, clingo and both Drools branches play the case.
