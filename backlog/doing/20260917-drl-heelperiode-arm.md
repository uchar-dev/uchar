# drl-heelperiode-arm: de DRL-tekst-tak lowert ook de heelperiodematrix

Goal: De classic-DRL-tak van `backends/drools` lowert de al normatieve
`Kwantorvouw` met een §8.2-heelperiodematrix, met dezelfde uitkomst en
dezelfde luide grenzen als de exec-model-tak. Dit sluit één van de
gemeten oude-toolchainregressies: in de oorspronkelijke differentiële
fixture `PHASE_GP_ALLE` speelden beide Drools-armen vijf gevallen
(lege verzameling/vacuüm waar, een tijdlijngat, alle leden boven de
grens, exact-op-de-grens en één tegenvoorbeeld); in uchar speelt exec
die vorm en geeft classic DRL nog een `TODO §8.2 kwantorvouw`-marker.

Evidence:

- De huidige publieke getuigen zijn `jutterswet-ploegpremie`,
  `jutterswet-keurploeg`, `jutterswet-reddingsvloot` en
  `jutterswet-sleepvloot`; samen pinnen ze ALLE, tellende kwantoren,
  filter en NIET.
- De runtime-helpers bestaan al:
  `rsGeheelPeriodeAlle`, `rsGeheelPeriodeGeen` en
  `rsGeheelPeriodeTelling`. De exec-model-tak en de oorspronkelijke
  toolchain-DRL-tak leveren het patroon
  collectList-accumulate + result-var-eval.
- De niet-temporele `Kwantorvouw` speelt al in beide huidige takken.
  Deze kaart verbreedt die classic-DRL-lowering; zij ontwerpt geen
  nieuwe kwantorsemantiek.

Acceptance:

- Rood-eerst: bouw de backend en leg voor minstens één ALLE- en één
  tellende golden de huidige `EXCLUDE drl-transpiler MARKER: ...
  een §8.2-heelperiodematrix` vast:

  ```sh
  mvn -q -o -f backends/drools/pom.xml package
  java -jar backends/drools/target/uchar-drools-backend-0-SNAPSHOT-shaded.jar \
    --drl corpus/golden/jutterswet-ploegpremie/ir.json \
    corpus/golden/jutterswet-ploegpremie/case*.json
  java -jar backends/drools/target/uchar-drools-backend-0-SNAPSHOT-shaded.jar \
    --drl corpus/golden/jutterswet-reddingsvloot/ir.json \
    corpus/golden/jutterswet-reddingsvloot/case*.json
  ```

- De classic-DRL-tak draagt alle vijf kwantoren (ALLE, GEEN,
  TEN_MINSTE, TEN_HOOGSTE, PRECIES), de ledenfilter en de
  NIET-prefix-semantiek: omklap bij ALLE/GEEN en telling-landing bij
  de drempelkwantoren. Lege verzameling, ontbrekend segment,
  gedeeltelijke dekking en grensgelijkheid blijven onderscheiden.
- Alle cases van de vier genoemde goldens eindigen zowel zonder vlag
  als met `--drl` in `PLAYED`; hun verwachtingen wijzigen niet.
  De gate-12 exec-model-kanaaltelling verandert dus niet. De
  backend-JUnit-suite moet de vier dual-branch replays blijvend pinnen,
  zodat `--drl` geen eenmalige handmeting blijft.
- De lowering hergebruikt de gedeelde runtimehelpers; zij kopieert de
  heelperiodetabellen niet als afwijkende MVEL-logica. Wat de
  exec-model-tak om een inhoudelijke reden weigert, weigert classic DRL
  per construct met dezelfde as en een reden, nooit met een
  compile-fout of stille lege fold.
- Noteer in het `Result:` de precieze wijziging voor
  `spec/CONSTRUCTS.md` rij 40 en voor verouderde
  "standing channel split"-proza. Een wijziging aan normatieve
  semantiek is niet nodig en heeft geen kaartmandaat.
- `mvn -q -o -f backends/drools/pom.xml package`,
  `bash scripts/check-drools.sh`,
  `bash scripts/check-reftype-derivation.sh` en daarna alle veertien
  gates zijn groen.

May touch: `backends/drools/src/main/java/io/github/yhavinga/regelspraak/transpiler/DrlTranspiler.java`,
`backends/drools/src/main/java/io/github/yhavinga/regelspraak/runtime/`,
`backends/drools/src/main/java/io/github/yhavinga/regelspraak/cases/CaseRunner.java`
alleen wanneer de dual-branch testnaad dat vereist,
`backends/drools/src/test/**`, deze kaart en bij afronding haar
verplaatsing binnen `backlog/`. Een noodzakelijk scriptpinvoorstel is
beperkt tot `scripts/check-drools.sh` of
`scripts/check-reftype-derivation.sh` en blijft als beschermd-padvoorstel
voor de maintainer; groei van een ondergrens alleen is geen reden om
een script te wijzigen.

Out of scope: niet-temporele kwantorproductie in de RegelSpraak-frontend
(`20260917-niettemporele-vouw-uit-bron.md`), een geneste vouw als matrix,
`bronPad`, local bindings, standing-value writes, prioritized defaults
en enige wijziging aan schema/verifier/ref/clingo of corpusverwachtingen.
Deze kaart deelt `DrlTranspiler.java` met de andere Drools-pariteitskaarten
van 2026-09-18 en wordt daarom serieel gepland, niet parallel.
