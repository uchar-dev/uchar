Violates WF-109 (`spec/IR-SPEC.md` ch. 4.6 (q2)) on its **second flank**: the
host names a type that BOTH bound objects are. The fact creation is
self-typed — a `Wachter` anker and a `Wachter` doel — so `eigenaar: "Wachter"`
picks out the anker and the doel equally well, and the type string settles
nothing.

    WF-109 FAIL /feitCreaties/0/voorwaarde/eigenaar: the characteristic check
    is hosted on 'Wachter', which the anker 'Wachter' and the doel 'Wachter'
    both are: the host names one object and this names two

Broken from the anchor `corpus/golden/anchor/jutterswet-erflijn` by adding one
declaration and one guard: a `bevoegd` characteristic on `Wachter`, and that
characteristic as the first creation's condition with `Wachter` in the host
field. The anchor itself is untouched and stays pinned.

Why it has to exist, and why it is not a corner. A one-object position cannot
produce this report: there the host either is the subject's type or is not.
Only the fact-creation guard binds two, and the moment the two are the SAME
type the field degenerates — it is a type name where the reading needs an
object. Nothing on the wire breaks the tie, so nothing may invent one: an
anker-wins rule and a doel-wins rule are both defensible and neither was
written. The producer agrees by abstaining — on `doel == anker` the RegelSpraak
frontend leaves the guard an unresolved marker rather than tagging it, so this
shape has no author.

The same report covers a case the self-typed pair only makes obvious: any
common SUPERTYPE of the anker and the doel names both objects too (satisfaction
is extensional, ch. 2.2), and is refused for exactly the same reason.

Falsifiable in two directions, each turning this case clean:

- empty the field — the creation's guard then gates the anker, which is what an
  absent host has always meant, and the document answers;
- make the pair heterotypic — give the creation a doel type distinct from the
  anker's, and the host names one object again.
