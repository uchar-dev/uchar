# drools-tekstliteral-lexeem: beide takken vergelijken v0-tekst verbatim

Goal: Beide Drools-takken geven een v0 `Literal` van type `Tekst`
dezelfde lexicale waarde: `waarde` wordt verbatim vergeleken en alleen
bij DRL-emissie ge-escaped. Classic DRL mag geen aanhalingstekens uit
de semantische waarde verwijderen; de exec-model-tak gebruikt niet
langer een afwijkende private tekstlezer. Dit herstelt zowel
beslistabelcellen uit de oorspronkelijke toolchain als canonieke
uchar-tekst met ingebedde quotes.

Evidence:

- IR-SPEC ch. 4.1 zegt dat v0 de onontlede bronstring in `value`
  draagt. Ref maakt daarvan verbatim `Value.Text`; case-invoer maakt
  van `{"text": ...}` eveneens verbatim tekst.
- Classic DRL roept `Literalen.tekstOfEnumLabel` aan, dat nu alle
  `"`-tekens met `replace` verwijdert. Exec-model heeft juist een
  private verbatimlezing. Het commentaar dat er één gedeelde
  interpretatie is, is daardoor aantoonbaar onwaar.
- `jutterswet-keurmerk` bewijst de ingebedde-quoteflank:
  exec speelt, classic DRL vergelijkt `merk "AA"` als `merk AA` en
  mist ook de geschreven waarde `veiling "Zee"`.
- De drie geïsoleerde `scenario-toka-46-woonregio-factor-*`-replays
  bewijzen de oude bronlexeemflank: zowel literal als casewaarde dragen
  `"Friesland"`-achtige buitenste quote-tekens; exec speelt en classic
  DRL faalt. De oorspronkelijke
  `ExecModelDifferentialTest.BESLISTABEL`-fixture speelde in beide
  oorspronkelijke armen. Een tegenprobe met beide waarden rauw keerde
  de huidige takuitkomst om; de branches interpreteren dus dezelfde
  wire aantoonbaar verschillend.

Acceptance:

- Rood-eerst: leg de classic-DRL-fout van
  `corpus/golden/jutterswet-keurmerk` en van alle drie
  `toka/cases/scenario-toka-46-woonregio-factor-*`-gevallen vast.
- `Literalen.tekstOfEnumLabel` (of een duidelijker hernoemde gedeelde
  helper) retourneert voor `Tekst` exact `lit.waarde()`. Alleen
  enumeratiewaarden verwijderen hun gepaarde enkel-quote-delimiters.
  `DrlTranspiler` gebruikt `drlString` uitsluitend om die waarde veilig
  in DRL te renderen; escaping verandert de waarde niet.
- Verwijder de private semantische tekstinterpretatie uit
  `ExecModelTranspiler`; alle tekstliteralvergelijkingen en
  tekstliteralwrites in beide branches gebruiken dezelfde helper.
- Unit- en dual-branchgetuigen pinnen afzonderlijk:
  1. rauw `Flevoland` == rauw `Flevoland`;
  2. quote-dragend `"Flevoland"` == exact dezelfde quote-dragende
     casewaarde;
  3. `merk "AA"` behoudt de ingebedde quotes;
  4. rauw en quote-dragend zijn onderling ongelijk (geen impliciete
     case-normalisatie);
  5. GELIJK en ONGELIJK, een `Samengesteld`/beslistabel-`of`-rij en een
     tekstwaarde in de consequentie.
- `jutterswet-keurmerk` en de drie geïsoleerde TOKA-replays eindigen
  zonder vlag én met `--drl` in `PLAYED`, zonder wijziging van IR,
  cases of verwachtingen. De backend-JUnit-getuige voor de
  buitenste-quotevorm is eigen geschreven en bevat geen TOKA-herkomst
  of private verwijzing.
- De v0-lexeemkeuze wordt niet stil omgezet in de toekomstige v1
  typed-value-delta. Noteer een eventueel gewenst canoniek
  migratiebesluit als vervolgkaart; wijzig deze kaart niet onderweg in
  een wiremigratie.
- `mvn -q -o -f backends/drools/pom.xml package`,
  `bash scripts/check-regelspraak.sh`,
  `bash scripts/check-toka-parity.sh`,
  `bash scripts/check-drools.sh` en alle veertien gates zijn groen.

May touch: `frontends/regelspraak/src/main/java/io/github/yhavinga/regelspraak/kern/Literalen.java`,
`frontends/regelspraak/src/test/java/io/github/yhavinga/regelspraak/kern/LiteralenTest.java`,
`backends/drools/src/main/java/io/github/yhavinga/regelspraak/transpiler/DrlTranspiler.java`,
`backends/drools/src/main/java/io/github/yhavinga/regelspraak/transpiler/model/ExecModelTranspiler.java`,
`backends/drools/src/test/**`, deze kaart en bij afronding haar
verplaatsing binnen `backlog/`. Bestaande corpus- en TOKA-bestanden zijn
alleen leesbare acceptatiegetuigen.

Out of scope: case-inputnormalisatie, het herschrijven van oude IR,
v1 typed literals, tekstinterpolatie (`TekstReeks`), verruiming van de
RegelSpraak-stringgrammatica, enumdomeinwerk en iedere kopie van
TOKA-materiaal buiten `toka/`. De exec gate-12-kanaaltelling wijzigt
niet. Deze kaart deelt beide transpilers met andere pariteitskaarten en
wordt serieel gepland.
