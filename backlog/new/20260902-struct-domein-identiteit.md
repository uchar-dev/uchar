# Struct-domein-identiteit: de zustertweeling van WF-089/WF-023

Goal: Fixronde 2 van de use-site-declaratie-identiteit-kaart heeft de
struct-tweeling van het domein-identiteitsgat GEMETEN en bewust buiten
scope gelaten (de maintainerterm "keuzetype-familie" dekt Enumeratie en
SumType, geen structdomeinen): een `StructLiteral` van structdomein B
geschreven in een attribuut van structdomein A is verifier-schoon; ref
fault bij de veldlezing en clingo leidt niets af waar een waarde
verwacht werd — een fout antwoord, dus een verifier-schone
ref≢clingo-divergentie van precies de klasse die WF-089 voor keuzetypes
dichtte. De uitbreiding is al gebouwd en doorgemeten in die fixronde
(0 over-afwijzing over 197 documenten, waarvan 9 met 55
struct-getypeerde referenties; het E35-reproductiedocument vangt hij) en
is dus gemeten gratis; wat ontbreekt is het maintainerbesluit over de
term (de "keuzetype-familie" verbreden, of structdomeinen als eigen
zusterclausule onder WF-023/WF-089 brengen) en het volle regime
(negatief per as, rood-eerst, indexrijen). De meetstand staat in de
WF-023-clausule (ch. 3.1), de WF-023-indexrij en de WF-089-clausule.

Acceptance:
- De termkeuze als genummerde vraag aan de maintainer vóór de bouw.
- Rood-eerst: het struct-reproductiedocument (StructLiteral-drift, en de
  lees-tweeling als die bestaat) verifier-schoon getoond vóór de regel,
  afgewezen erna; per as een negatief met NOTE en mutant-bewijs.
- De "should preclude"-restzinnen in `ref/lib/eval.ml` en de
  spec-clausules die het gat nu documenteren, aangescherpt naar de
  nieuwe stand.
- Nul over-afwijzing over golden + rood + toka (read-only), gemeten;
  alle twaalf gates groen.

May touch: `spec/verifier/**` (proposal), `spec/IR-SPEC.md` (proposal),
`corpus/negatief/**`, `ref/**` (meldteksten), `backlog/`.

Out of scope: keuzetype-armen (geland), de WF-061-expressielaag/
declaratiekant (eigen open punt), nieuwe struct-constructen.
