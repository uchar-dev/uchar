# drl-staande-waarden: classic DRL draagt de ch.-7.8-norm

Goal: De classic-DRL-tak implementeert dezelfde staande-waardedoelen als
ref, clingo en de exec-model-tak: een lege berekening op een staand
scalair of temporeel doel schrijft niets; een gevulde berekening
verdringt de bestaande waarde of hele tijdlijn. De
initialisatie-nevenvorm van ch. 7.7 volgt dezelfde domeinafbakening.
Dit is nodig voor volledige huidige RegelSpraak→uchar→beide-Drools
conformiteit, maar is nadrukkelijk geen herstel van oud
toolchaingedrag.

Evidence:

- RUNTIME-SEMANTICS ch. 7.8 is normatief en pinnt de scalaire
  doorval en de whole-timeline-doorval. De exec-model-implementatie
  heeft `staandeScalaireDoelen`, `ontsuikerdeDoelen` en
  `staandeTemporeleDoelen`; classic DRL kent die doelclassificatie niet.
- De bestaande exec-model-JUnitmatrices
  `StaandeScalaireNormWitnessTest` en
  `TemporeelFixpuntWitnessTest` pinnen leeg/gevuld en de domeinflanken,
  maar lopen via `CaseRunner.speelCase` alleen door exec-model.
- Huidige classic-DRL-uitkomsten:
  `anchor/jutterswet-waardestelling` wist case3 naar leeg;
  `anchor/jutterswet-waardering-v0vorm` laat in case6 de
  initialisatie 25 winnen van de staande input 40;
  `anchor/jutterswet-borgdeel` en
  `anchor/jutterswet-dagentelling` wissen de invoertijdlijn bij een
  lege fold.
- Een gerichte replay tegen de oorspronkelijke toolchain bewees het
  onderscheid: beide oude armen schreven bij een lege scalaire
  berekening `null` en bij een lege temporele berekening `[]`; een
  losse initialisatie liet invoer staan. Staande preservation is dus
  een bewuste uchar-delta, niet één van de oude-feature-regressies.

Acceptance:

- Rood-eerst: leg de vier bovenstaande classic-DRL-fouten vast, met
  minstens één scalaire, initialisatie- en tijdlijncase:

  ```sh
  mvn -q -o -f backends/drools/pom.xml package
  java -jar backends/drools/target/uchar-drools-backend-0-SNAPSHOT-shaded.jar \
    --drl corpus/golden/anchor/jutterswet-waardestelling/ir.json \
    corpus/golden/anchor/jutterswet-waardestelling/case*.json
  java -jar backends/drools/target/uchar-drools-backend-0-SNAPSHOT-shaded.jar \
    --drl corpus/golden/anchor/jutterswet-borgdeel/ir.json \
    corpus/golden/anchor/jutterswet-borgdeel/case*.json
  ```

- Trek de doelclassificatie uit naar één gedeelde, modelgebaseerde
  helper of bewijs met één gedeelde testmatrix dat beide branches exact
  dezelfde sets afleiden. De clauses zijn ref-getrouw:
  versie-vrij, één rekenregelschrijver, scalaire initialisatiepaarvorm
  apart via ch. 7.7, geen defaults/allocatie/creatiewrite en dezelfde
  other-writergrens. Classic DRL mag niet een ruimer
  "skip iedere null-write"-beleid krijgen.
- Scalar:
  leeg laat bestaande input staan; zonder input blijft het doel leeg;
  gevuld verdringt input. Naast een initialisatie laat leeg de
  initialisatie beslissen, laat bestaande input de initialisatie leeg
  en verdringt gevuld beide.
- Tijdlijn:
  een lege pro-rata- of dagtellingopbrengst laat de invoertijdlijn
  geheel staan; een niet-lege opbrengst vervangt de hele tijdlijn.
  Invoer- en berekende segmenten worden nooit per bucket gemengd.
  De bestaande idempotentie/fixpuntgarde blijft nodig en actief.
- De niet-staande flanken blijven gepind: twee rekenregels op één doel
  behouden de v0-wissemantiek; een versie-doel komt niet in het domein;
  een werkelijk beslissende write wordt nooit onderdrukt. Een test die
  alleen de vier groene doorvallen controleert is onvoldoende.
- Maak de twee bestaande witnessklassen dual-branch of voeg één
  branchespiegel toe die hun volledige geldige matrix met
  `viaExecModel` en `viaDrlTekst` draait. De WF-050-ongeldige
  defaults-mechaniekwitness hoeft niet door classic DRL, omdat die tak
  prioritized defaults als geheel luid weigert.
- Alle cases van `waardestelling`, `waardering-v0vorm`, `borgdeel` en
  `dagentelling` eindigen zonder vlag én met `--drl` in `PLAYED`, met
  ongewijzigde verwachtingen. Gate 12's exec-modelkanaaltelling
  verandert niet.
- `mvn -q -o -f backends/drools/pom.xml package`,
  `bash scripts/check-drools.sh`,
  `bash scripts/check-reftype-derivation.sh` en alle veertien gates
  zijn groen.

May touch: `backends/drools/src/main/java/io/github/yhavinga/regelspraak/transpiler/DrlTranspiler.java`,
`backends/drools/src/main/java/io/github/yhavinga/regelspraak/transpiler/model/ExecModelTranspiler.java`
uitsluitend om de doelclassificatie werkelijk te delen,
een nieuwe gedeelde helper onder
`backends/drools/src/main/java/io/github/yhavinga/regelspraak/transpiler/kern/`,
`backends/drools/src/main/java/io/github/yhavinga/regelspraak/runtime/`
alleen wanneer een gedeelde writegarde nodig is,
`backends/drools/src/main/java/io/github/yhavinga/regelspraak/cases/CaseRunner.java`,
`backends/drools/src/test/**`, deze kaart en bij afronding haar
verplaatsing binnen `backlog/`.

Out of scope: het terugbrengen van de oude destructieve leeg-write,
prioritized-defaultslowering in classic DRL, wijziging van de
ch.-7.8-norm, andere multi-writersemantiek, object-/feitcreatie,
nieuwe temporele functies en corpusverwachtingen. Deze kaart is een
uchar-conformiteitskaart en moet in `Result:` ook zo worden genoemd.
Zij deelt `DrlTranspiler.java` met de andere pariteitskaarten van
2026-09-18 en wordt serieel gepland.
