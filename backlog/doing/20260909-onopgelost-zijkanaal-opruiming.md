# Opruiming: het Onopgelost-drager-zijkanaal in backends/drools

Goal: uchar's Drools-port (`backends/drools`) draagt de Let nog via de
Onopgelost-drager plus het identity-zijkanaal (de `LetVorm`-registry
gesleuteld op object-identiteit) — een constructie uit de tijd dat de
geconsumeerde IR geen echte Let/BinderRef-records had. Sinds de
Let-port in regelspraak-java (20260901-toolchain-let-port, geland
2026-09-09) bestaat het bewijs dat dezelfde transpiler-structuur met
echte `Expr.Let`/`BinderRef`-records eenvoudiger is (geen zijkanaal,
geen identity-sleutels). Deze kaart ruimt het zijkanaal in de
uchar-port op door de loader echte Let-knopen te laten bouwen en het
Let-cluster daarop te laten werken.

Acceptance:
- De identity-gesleutelde registry en de Onopgelost-drager voor Lets
  zijn verwijderd; de loader bouwt de Let-vorm direct.
- Gate 12 (played-set, kanaalvloer, JUnit incl. de
  CompanionPadWitnessTest-flanken) en gate 13 byte-gelijk aan de
  huidige stand: 0 gedragsverschillen, geen pin beweegt.
- Alle dertien gates groen.

May touch: backends/drools/**, backlog/.

Out of scope: gedragswijzigingen (dit is een pure
constructie-opruiming); spec-tekst; de andere executors;
regelspraak-java.

---

## Resultaat

**Wat verdwenen is.** `loader/LetVorm.java` — de sealed interface met de drie
records `Binding`, `Lezing` en `Keuze` — is als bestand weg (101 regels). In
`IrLoader` is `letDrager` weg en daarmee de drager voor Let en BinderRef: de
wire-knopen `Let` en `BinderRef` worden nu als `Expr.Let` respectievelijk
`Expr.BinderRef` in de boom gebouwd. In `ExecModelTranspiler` is
`geenUnieLet` weg — de fail-fast die de unievormen júist verbood, zes regels
die nu de normale gang zijn — en werken `letWaarde`, `letKinderen`,
`kwalificeertOperand`, `scalarWaarde` en de twee companion-walks
(`verzamelDelingen`, `verzamelWortels`) op de knopen zelf in plaats van op
`letVormen.get(e)`. `EffectieveEenheid` leidt de Let en de binder-lezing af
uit de boom; alleen zijn voorberekende lezingentabel krijgt nog een
Let-index mee, want een losse `BinderRef` draagt geen route omhoog naar zijn
eigen binding. Netto over `backends/drools/`: 269 regels erbij, 308 eraf.

**Wat gebleven is, en waarom.** De `IfThenElse` houdt zijn
`Expr.Onopgelost`-drager en een identity-gesleutelde kaart — nu met precies
één vorm erin (`IrLoader.Keuze`) in plaats van drie. Reden: de gedeelde
`Expr`-unie draagt sinds de let-lowering wél `Let` en `BinderRef`, maar géén
`IfThenElse` (die knoop is never emitter-produced), en de unie woont in
`frontends/regelspraak/`, buiten het may-touch-bereik van deze kaart. De
alternatieven waren allebei slechter: `IfThenElse` weigeren zou vijf
spelende goldens uit het kanaal duwen (`anchor/jutterswet-tariefstelling`,
`jutterswet-tariefrecht-catala`, `synth3-conditielezing`,
`synth3-drempelheffing`, `synth3-takbarriere`) en dus een gedragswijziging
zijn; de unie uitbreiden valt buiten de kaart. **Acceptatiepunt 1 is dus
gehaald voor de Let en niet voor de IfThenElse.** Het restant is één
kandidaat-kaartje waard: `IfThenElse` in de gedeelde `Expr`-unie, spec-first
langs gate 10 en de toolchain-pin.

**De byte-gelijkheidsmeting.** Twee onafhankelijke assen, beide vóór en ná
gemeten op dezelfde 165 goldens, en de ná-meting is op de definitieve kop
herhaald:

- *exec-model-tak*: de gate-12-uitslagregel per golden (`PLAYED n` /
  `EXCLUDE <reden>` / `FAIL` / `NOCASE`), regel voor regel gediff'd —
  **geen enkel verschil**; 118 PLAYED / 44 EXCLUDE / 1 FAIL (kustwacht,
  de gepinde value-divergence) / 2 NOCASE.
- *DRL-tekst-tak*: de geëmitteerde DRL van elke golden, inclusief de
  marker- en weigeringsregels, gedumpt via een wegwerp-harnas op
  `IrLoader.loadAll` + `DrlTranspiler.build` — **alle 165 dumps
  byte-identiek**.

Dat de meting de wijziging ook echt raakt: elf van de vijftien goldens met
een `Let` op de wire spelen door de nieuwe constructie
(`anchor/jutterswet-tariefstelling`, `jutterswet-bergingstoeslag`,
`jutterswet-drempelvangst`, `jutterswet-kinderweging`,
`jutterswet-lichterloon`, `jutterswet-tariefrecht-catala`,
`jutterswet-vondstrecht`, `synth3-barrierebezit`, `synth3-conditielezing`,
`synth3-drempelheffing`, `synth3-takbarriere`); de andere vier weigert de
loader al op een ListType/SumType/functionDefs.

**Het gemeten risico dat niet uitkwam.** Een echte `Expr.Let` is voor de
gevendorde `Expr.bevat` géén blad meer — die daalt af in value én body,
waar de `Onopgelost`-drager een blad was. Daarmee zien `heeftTijdgebonden`
(§4.2/§5.3 Rekendatum), `PeriodeDekking.parameterNamen` en
`ProducentConsument` in beginsel méér dan voorheen. Over de hele corpus
verandert dat geen enkele uitslag en geen byte DRL — maar het is de plek
waar een toekomstige golden met een Rekendatum ín een Let-waarde het
verschil wél zou laten zien, en dan is het een verbetering, geen regressie.

**Tests die bewogen (twee bestanden, allebei constructie-volgend, geen
assertie gewijzigd).** `EffectieveEenheidWitnessTest`: de vijf
Let-cluster-getuigen bouwden hun modellen met `drager(...)` +
`LetVorm.Binding/Lezing`; die bouwen nu `new Expr.Let(...)` /
`new Expr.BinderRef(...)` en geven de Let-index mee via `overLets(...)`. De
verwachtingen (KG/TON/null) en de boodschappen staan letterlijk ongewijzigd;
de `Keuze`-getuige houdt zijn drager. Het ongebruikte hulprecord `Cluster`
is meegegaan. `EenheidsconflictWitnessTest`: twee aanroepregels volgen de
gewijzigde constructor/`build`-signatuur. Suite onveranderd **237 tests,
nul skips**.

**Dertien gates groen** (vanuit deze worktree, ná commit; `sh`/`bash` per
shebang, `eval $(opam env)` vóór 9; 11, 12 en 13 hebben de jar herbouwd):
1 PASS; 2 = **188**; 3 = **188 golden / 339 negatief**; 4 = **448**;
5 = **425** (2 zonder case: `jutterswet-seizoenvondst`,
`jutterswet-vormenkabinet`); 6 = **425**; 7 = 389/36; 8 = 23 (Drools-leg
6/17); 9 = 201/12; 10 = 173/38; 11 = 45/2/27/29 **onbewogen**;
12 = played **118** / kanaalvloer 47 / auto-excluded 44 /
value-divergence 1 / no-case 2 / JUnit **237**; 13 = **74** twists over 15
goldens / 70 replays / 13 PLAYED door de DRL-tekst-tak / 0 survivors.
**Geen pin verplaatst.** `git status --short` leeg; de branch blijft
ONGEMERGED.
