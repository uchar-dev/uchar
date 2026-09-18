# drools-rolvelden-herhaalde-typenparen: geschreven rol kiest het beanveld

Goal: Het gegenereerde Drools-feitenmodel draagt rol-adresseerbare
referentievelden wanneer meer dan één feittype hetzelfde
objecttypepaar verbindt. Zowel aggregatie als §13.4.7-feitcreatie kan
dan de uniek geschreven rol gebruiken, in plaats van te stranden op
een ambigue typepaarafleiding. De twee constructen blijven in één kaart
omdat hun huidige weigering uit exact hetzelfde
`FactModelGenerator`-contract komt; ze in twee sessies bouwen zou
dezelfde bestanden en hetzelfde opslagbesluit dubbel raken.

Evidence:

- In de oorspronkelijke `PHASE_C` speelde AANTAL over de geschreven
  elementrol `vertrekkende reiziger` terwijl meerdere feittypen
  `Luchthaven` en `Reiziger` verbonden. De rolnaam selecteerde uniek
  het juiste feittype en beide oorspronkelijke Drools-armen leverden de
  gepinde telling.
- De oorspronkelijke `PHASE_G_FEIT` (vier gevallen) en
  `PHASE_GELDIG` (twee datumgevallen) creëerden een feit over een al
  bestaande `Vlucht`-`Passagier`-relatie. De geschreven rol `reis`
  selecteerde de opslag, met anker- en doelguards en een
  geldig-vanafvenster. Beide oorspronkelijke armen speelden.
- Uchar resolveert die geschreven rollen nog steeds, maar
  `FactModelGenerator.leden` vraagt voor gewone cross-typevelden eerst
  om één uniek objecttypepaar. Bij herhaalde paren is dat antwoord null;
  exec geeft `SKIP ... geen referentieveld` en classic DRL een marker.
  Dit is geen bronambiguïteit: de geschreven rol is uniek.

Acceptance:

- Rood-eerst: voeg kleine, zelfgeschreven backendfixtures toe die de
  twee huidige redenen exact reproduceren:
  `aggregatie-join ... geen referentieveld` en
  `feitcreatie ... geen referentieveld`. Leg daarnaast vast dat de
  oorspronkelijke toolchainfixtures voor de zeven gevallen in beide
  manifests `LOWERED` waren en differentieel vuurden.
- `FactModelGenerator` leidt voor een gewone cross-type-relatie één
  scalar referentieveld per uniek resolveerbare to-one-rol af, ook
  wanneer hetzelfde typepaar via andere feittypen voorkomt. De
  veldnaam blijft de rolnaam en het Java-type het type dat die rol
  vult. Bestaande unieke-paren en self-pairs blijven ongewijzigd.
- Verander `RegelSpraakModel.verbindendeRol(typeA,typeB)` niet in een
  gokfunctie. Een consument zonder geschreven/symbolische rol blijft
  een herhaald typepaar luid ambigu vinden. Naamdeling met een
  attribuut/kenmerk, een niet-unieke rolnaam en een to-many-rol zonder
  opslagdragende overzijde blijven afzonderlijk benoemde weigeringen.
- `CaseRunner`, `ContractValidator` en beide transpilers gebruiken bij
  een expliciete `bronRol`/elementrol/`joinVeld`/`zetVeld` hetzelfde
  rol-adresseerbare contract. De invoer- en creatiepaden schrijven
  beide relationele richtingen die RUNTIME-SEMANTICS ch. 5.7a vereist;
  een veld dat alleen de juiste telling oplevert maar de verkeerde
  relatie draagt is niet voldoende.
- Voeg de eigen, rechtenvrije, bron-gedragen golden
  `corpus/golden/jutterswet-dubbelverbinding/` toe met twee feittypen
  over hetzelfde objecttypepaar. De cases scheiden minimaal:
  1. aggregatie over elk van beide geschreven rollen;
  2. feitcreatie wel/niet door ankerkenmerk;
  3. feitcreatie wel/niet door doelkenmerk of doelvergelijking;
  4. vóór en binnen het geldigheidsvenster;
  5. de aangemaakte koppeling aan beide kanten.
  De golden regenereert byte-identiek uit `bron.rgs` en speelt met
  absolute verwachtingen in ref, clingo, exec-model en classic DRL.
- Pin ook de oude `PHASE_C`-telling en de zes
  `PHASE_G_FEIT`/`PHASE_GELDIG`-uitkomsten als backend-JUnitgevallen,
  zonder TOKA-materiaal of een private padverwijzing in artifactzones.
- Verhoog `scripts/check-drools.sh` `EXPECTED_PLAYED` met precies één
  vanaf de bij oppakken gemeten basis voor
  `jutterswet-dubbelverbinding`. Geen bestaande niet-PLAYED golden mag
  door een onbedoelde verbreding van kanaal wisselen.
- `mvn -q -o -f backends/drools/pom.xml package`,
  `bash scripts/check-regelspraak.sh`,
  `bash scripts/check-drools.sh`,
  `bash scripts/check-reftype-derivation.sh` en alle veertien gates
  zijn groen.

May touch: `backends/drools/src/main/java/io/github/yhavinga/regelspraak/schema/FactModelGenerator.java`,
`backends/drools/src/main/java/io/github/yhavinga/regelspraak/schema/ContractValidator.java`,
`backends/drools/src/main/java/io/github/yhavinga/regelspraak/cases/CaseRunner.java`,
`backends/drools/src/main/java/io/github/yhavinga/regelspraak/transpiler/DrlTranspiler.java`,
`backends/drools/src/main/java/io/github/yhavinga/regelspraak/transpiler/model/ExecModelTranspiler.java`,
de minimaal benodigde rolresolutiehelper onder
`frontends/regelspraak/src/main/java/io/github/yhavinga/regelspraak/model/RegelSpraakModel.java`,
`backends/drools/src/test/**`,
`corpus/golden/jutterswet-dubbelverbinding/`,
`scripts/check-drools.sh` uitsluitend voor de door deze kaart
toegevoegde played telling (beschermd-padvoorstel), deze kaart en bij
afronding haar verplaatsing binnen `backlog/`.

Out of scope: het kiezen van een rol zonder geschreven binding,
naamdelingsvormen, beide-zijden-to-many, ambigue/wederkerige self-pairs,
de conservatieve bestaande opslagrichtinggevallen
`jutterswet-bergingstoezicht`, `jutterswet-erflijn`,
`jutterswet-gemeenteberging` en `jutterswet-penninguitgifte`,
objectcreatie-semantiek, `bronPad`, en de geaccepteerd
dimensioneel-invalide multi-globalfixtures. Geen TOKA-materiaal mag
buiten `toka/` komen. Deze kaart deelt het feitenmodel en beide
transpilers met andere pariteitskaarten en wordt serieel gepland.
