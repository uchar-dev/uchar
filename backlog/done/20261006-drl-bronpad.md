# drl-bronpad: de DRL-tak geeft Drools de tekst onder een pad dat bij zijn package past

*Kleine fix zonder kaart, opdracht van de hoofdsessie van 2026-10-06;
gebouwd vanaf uchar `abf184e7` op de tak `fix/drl-bronpad`, fix-commit
`a2384ed1`. Dit record legt de fix vast, zodat de eigenaar er zijn Keuring
bij kan zetten.*

## Aanleiding

Sinds regelspraak-toolchain `33d89cf` draagt de jar van de distributie een
SLF4J-backend (slf4j-simple op WARN). Daardoor kwam bij elke bouw van de
DRL-teksttak, in `bin/regelspraak drl` en in het log van de playground,
deze regel op stderr:

    [main] WARN org.drools.compiler.kie.builder.impl.KieBuilderImpl - File 'file0.drl' is in folder '' but declares package 'dev.uchar.regelspraak.generated'. It is advised to have a correspondance between package and folder names.

`DroolsBackend.drlTak` gaf de tekst aan `KieHelper.addContent(drl,
ResourceType.DRL)`. Zonder naam schrijft KieHelper haar als
`src/main/resources/file0.drl`, in de hoofdmap, en
`KieBuilderImpl.packageNameForFile` (Drools 8.44) vergelijkt die map met de
package-regel van de tekst en logt het verschil met `log.warn`. Het is een
logregel en geen bericht in de `Results` van de bouw; zonder SLF4J-backend,
zoals op het testklassenpad van deze backend, gaat hij verloren. Daarom is
er geen rode getuige in de JUnit-suite: die zou de regel niet zien. De
meting loopt via de distributie.

## Wat landde

- `DrlTranspiler.DRL_PACKAGE`: de package die de DRL declareert, één
  constante voor de package-regel en voor het pad.
- `DroolsBackend.drlTak` geeft de tekst aan `KieHelper.addContent(drl,
  naam)` onder `dev/uchar/regelspraak/generated/regels.drl`, de mappen van
  `DRL_PACKAGE`; KieHelper zet er `src/main/resources/` voor en de extensie
  geeft het resourcetype.

Elders in uchar en in de distributie gaat geen DRL-tekst naar Drools:
`drlTak` is de enige aanroep van `KieHelper`, `ResourceType.DRL` of een
`KieFileSystem`. De exectak bouwt met `KieBaseBuilder.createKieBaseFromModel`
en geeft Drools geen resource.

## Metingen

Vóór = de distributie met uchar op `abf184e7`, na = met uchar op
`a2384ed1`, telkens met een herbouwde jar.

- `bin/regelspraak drl` op `service/src/test/resources/keten/jutterswet-strandvondsten.rgs`
  van de distributie en op een tweede bron (meldtermijn): vóór de regel
  hierboven op stderr, na stderr leeg (0 bytes); de DRL-uitvoer is in beide
  gevallen byte-gelijk.
- `PLAYGROUND=true bin/regelspraak serve …` met een POST op
  `/playground/analyse`: vóór, met meldtermijn, dezelfde regel in het log
  (thread `pool-1-thread-1`); na, met beide bronnen, geen WARN in stdout en
  stderr. Het veld `drl` van de analyse van meldtermijn is vóór en na
  gelijk.
- De zestien gates groen op `a2384ed1`. Gate 12: de exectak speelt 239, de
  DRL-teksttak 193, beide gelijk aan hun pin; JUnit 1094 tests.

## Open punt

`drlTak` kort de melding van een mislukte DRL-bouw af op 200 tekens
(`drl compile: …`). Het pad in die melding is 33 tekens langer dan
`file0.drl`, dus van de tekst van Drools blijven 33 tekens minder over; regel
en kolom staan er nog in. Gemeten op een DRL met een syntaxfout:

    vóór: drl compile: [Message [id=1, kieBase=defaultKieBase, level=ERROR, path=file0.drl, line=0, column=0    text=Line 5:8 unexpected exception at input ')'. Exception: java.lang.NullPointerException: Cannot invoke "java…
    na:   drl compile: [Message [id=1, kieBase=defaultKieBase, level=ERROR, path=dev/uchar/regelspraak/generated/regels.drl, line=0, column=0    text=Line 5:8 unexpected exception at input ')'. Exception: java.lang.NullPoin…

Geen golden wordt op de DRL-tak geweigerd met `drl compile`, dus geen gate
ziet dit verschil. De grens van 200 tekens is niet aangepast.
