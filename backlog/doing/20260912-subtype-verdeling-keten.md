# subtype-verdeling-keten — de ketensluiting van de verdelings-tak

Goal: een reparatiekaart uit de dialect-lensronde. Die ronde mat op
`/tmp/lens-cases/p18-verdeling-erfattribuut.rgs` een WF-034 FAIL op
`/verdelingen/0/doelAttribuut` waar het doeltype een subtype is en het
doelattribuut op een voorouder staat, en las die als een achterblijver van de
subtype-kernel: de verdelings-tak zou nog de rauwe declaratieset lezen terwijl
ch. 2.2's effectieve-ledenverzameling het attribuut aan het subtype toekent.
Deze kaart meet dat na over ELKE verdelings-zijdige en aanverwante
declaratie-lookup in `spec/verifier` (verdeling, feitcreatie, objectcreatie,
uniciteit, consistentie), repareert wat achterblijver is, documenteert per
plek die rauw BLIJFT waarom dat daar klopt, en meet daarna de hele route: kan
zo'n verdeling ook worden UITGEVOERD in ref, clingo en Drools? Spelen alle
drie, dan een kleine golden met handberekende pins als getuige.

Acceptance:
- de p18-vorm is eerst een falende getuige, daarna groen — of, als de
  weigering geen achterblijver blijkt, is de werkelijke oorzaak gemeten,
  benoemd en met een probe vastgelegd;
- elke rauw gelaten declaratie-lookup draagt zijn reden;
- alle dertien gates groen (`bash scripts/check-*.sh`);
- de getuige-golden speelt in ref, clingo en Drools met dezelfde
  handberekende pins.

May touch: `spec/verifier/`, `ref/`, `backends/`, `corpus/` (de nieuwe
golden), `scripts/check-drools.sh` (alleen de `EXPECTED_PLAYED`-gelijkheid),
`spec/IR-SPEC.md` en `spec/RUNTIME-SEMANTICS.md` (alleen zinnen die door deze
reparatie onwaar of onvolledig worden), dit kaartbestand.

Out of scope: `frontends/regelspraak` (net geland; een gat daar wordt gemeld,
niet gerepareerd), `spec/CONSTRUCTS.md` en `spec/PLAN.md` (registervoorstel
als letterlijke tekst in het Result), grote spec-herschrijvingen.

Result: (bij afronding)
