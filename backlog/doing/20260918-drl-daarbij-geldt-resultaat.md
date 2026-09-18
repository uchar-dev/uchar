# drl-daarbij-geldt-resultaat: classic DRL draagt resultaatbindings

Goal: De classic-DRL-tak lowert de door de RegelSpraak-frontend
geproduceerde `Let`/`BinderRef`-keten voor resultaatzijdige en
doorgekoppelde `Daarbij geldt`-variabelen. Daarmee speelt valide
RegelSpraak-bron die in beide oorspronkelijke Drools-armen werkte ook
weer in beide uchar-armen, zonder de binding als tekstsubstitutie te
vereenvoudigen en zonder de normatieve leegbarrière te verliezen.

Evidence:

- De uitputtende vergelijking van 624 fixture-arraygevallen vond
  precies vier oude dual-backendgevallen in deze familie:
  `PHASE_11` (een variabele gelezen in voorwaarde én resultaat; een
  latere variabele die een eerdere leest) en `PHASE_11_ABS`
  (nul-gardering; een deling pas na de gardering). Beide oorspronkelijke
  manifests waren `LOWERED` en de differentiaal vuurde beide KieBases.
- De huidige publieke brongetuigen
  `jutterswet-drempelvangst`, `jutterswet-lichterloon`,
  `jutterswet-vondstrecht` en
  `anchor/jutterswet-tariefstelling` spelen in exec-model. Classic DRL
  markeert de eerste resultaatbinding met `TODO expressie: <naam> is ...`.
- De frontend bouwt resultaatbindings als gewone `Expr.Let`- en
  `Expr.BinderRef`-knopen. `CaseRunner.drlTak` geeft de geladen
  `lets()` nu niet door en `DrlTranspiler` initialiseert
  `EffectieveEenheid` met een lege let-index.

Acceptance:

- Rood-eerst: leg de huidige marker vast op minstens de eenvoudige en
  de geketende vorm:

  ```sh
  mvn -q -o -f backends/drools/pom.xml package
  java -jar backends/drools/target/uchar-drools-backend-0-SNAPSHOT-shaded.jar \
    --drl corpus/golden/jutterswet-drempelvangst/ir.json \
    corpus/golden/jutterswet-drempelvangst/case*.json
  java -jar backends/drools/target/uchar-drools-backend-0-SNAPSHOT-shaded.jar \
    --drl corpus/golden/anchor/jutterswet-tariefstelling/ir.json \
    corpus/golden/anchor/jutterswet-tariefstelling/case*.json
  ```

- `DrlTranspiler` ontvangt de geladen let-index en lowert
  frontend-produceerbare `Let`/`BinderRef`-bomen met lexicale scope:
  een eerdere binder is leesbaar in een latere definitie en in het
  resultaat; herhaald lezen levert dezelfde behoefte; een
  binnenscope kan niet buiten zijn scope lekken. Naamgelijkheid alleen
  mag geen capture of globale DRL-variabelenbotsing veroorzaken.
- De lowering volgt RUNTIME-SEMANTICS ch. 10.6 call-by-need voor deze
  produceerbare subset: een ongelezen behoefte wordt niet geëvalueerd,
  een gelezen lege behoefte maakt de omhullende Let leeg, en een
  gardering voorkomt dat een deling in de niet-betreden route wordt
  geëvalueerd. Foutcompanions, freshness/reactiviteit en effectieve
  eenheden wandelen door de binding heen; zij mogen niet verdwijnen
  doordat alleen de waardetekst wordt ingevoegd.
- Voeg dual-branch JUnit-getuigen toe voor de vier gemeten vormen:
  binder in conditie én resultaat, latere binder leest eerdere,
  nul-gardering vuurt niet, en de gevulde delingsroute levert de
  absolute oorspronkelijke uitkomst. Pin daarnaast leeg versus gevuld,
  zodat een altijd-schrijven en een nooit-schrijven implementatie elk
  rood wordt.
- De vier genoemde goldens eindigen zonder vlag én met `--drl` in
  `PLAYED`, met ongewijzigde verwachtingen. De gate-12
  `EXPECTED_PLAYED`-telling verandert niet: de exec-model-tak speelde
  ze al.
- `mvn -q -o -f backends/drools/pom.xml package`,
  `bash scripts/check-drools.sh`,
  `bash scripts/check-reftype-derivation.sh` en alle veertien gates
  zijn groen.

May touch: `backends/drools/src/main/java/io/github/yhavinga/regelspraak/transpiler/DrlTranspiler.java`,
`backends/drools/src/main/java/io/github/yhavinga/regelspraak/cases/CaseRunner.java`,
gedeelde helpers onder
`backends/drools/src/main/java/io/github/yhavinga/regelspraak/transpiler/kern/`
alleen voor werkelijk gedeelde binding/effectieve-eenheidlogica,
`backends/drools/src/test/**`, deze kaart en bij afronding haar
verplaatsing binnen `backlog/`. Een scriptwijziging is niet voorzien;
een onverwacht stale beschermd scriptpin wordt als exact voorstel in
het `Result:` gezet, niet opportunistisch verbreed.

Out of scope: `Expr.IfThenElse` (de RegelSpraak-emitter produceert die
knoop niet), een Let in prioritized-defaultswaarden (classic DRL draagt
die extensie als geheel nog niet), list/struct/function-body Lets,
frontend- of wireherontwerp, en corpusverwachtingen. Condition-only
variabelen die de frontend al zonder Let naar beide takken brengt mogen
niet regresseren. Deze kaart deelt `DrlTranspiler.java` met de andere
Drools-pariteitskaarten van 2026-09-18 en wordt serieel gepland.
