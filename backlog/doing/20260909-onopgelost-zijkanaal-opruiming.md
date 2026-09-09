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
`geenUnieLet` weg. Die assertie is niet versoepeld maar INGETROKKEN omdat ze
haar doel bereikt heeft: ze is geïnstalleerd bij commit 6fba057 ("de sealed
union groeit met Let en BinderRef — emitter + Drools fail-fast"), toen de
unie de twee records kreeg en de drools-port de omschakeling nog niet had
gemaakt. Haar taak was precies dát vast te leggen — een unie-`Let` die tóch
binnenkwam betekende dat de loader onder de port was veranderd, en elke
wandeling hier zou hem als blad hebben gelezen en dus in stilte
onder-rapporteren. Deze kaart ÍS die omschakeling: de loader levert de
unievormen nu bewust, elke wandeling matcht erop, en een assertie die het
tegendeel eist is daarmee uitgewerkt. In hetzelfde bestand werken
`letWaarde`, `letKinderen`,
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
gehaald voor de Let en niet voor de IfThenElse.** Het restant is
uitgeschreven als `backlog/new/20260909-ifthenelse-in-de-unie.md`:
`IfThenElse` als echt record in de gedeelde `Expr`-unie, spec-first langs
gate 10 en de toolchain-pin, met de vijf spelende goldens als meting.

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

**Het gemeten risico — en het kwam WEL uit, op de rode corpus.** Een echte
`Expr.Let` is voor de gevendorde `Expr.bevat` géén blad meer: die daalt af in
value én body, waar de `Onopgelost`-drager een blad was. Daarmee zien
`heeftTijdgebonden` (§4.2/§5.3 Rekendatum), `PeriodeDekking.parameterNamen`
en `ProducentConsument` méér dan voorheen. Over de hele GOUDEN corpus
verschuift dat geen uitslag en geen byte DRL. Over de RODE corpus wél, op
precies één case — en dat is de bevinding van deze kaart.

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

## Rebase op main 196ccea, en de bevinding op gate 8

De branch is gerebased van aftakpunt f141ac8 op main 196ccea: **nul
conflicten**, en main raakte in die reeks geen enkel bestand in
`backends/drools/` (wel `scripts/check-drools.sh` en `scripts/check-rood.sh`,
plus de nieuwe golden `synth3-operandvolgorde` en drie rode cases van de
operandvolgorde-kaart).

**De byte-gelijkheids-as opnieuw gemeten op de gerebasede kop**, nu als
main's constructie tegen de mijne over identiek dezelfde corpus (main's boom
uitgepakt naar een tijdelijke map en daar gebouwd, zodat alleen de
constructie verschilt): de gate-12-uitslagregel per naam is **identiek voor
alle 166 goldens**, en de geëmitteerde DRL is **byte-identiek voor alle 166
dumps**. De nieuwe golden `synth3-operandvolgorde` speelt met 2 cases en
draagt zélf een `Let` én een `BinderRef`, dus de meting raakt de wijziging
opnieuw en scherper dan vóór de rebase.

**Gates op de gerebasede kop:** 1 PASS; 2 = **192**; 3 = **192 golden / 342
negatief**; 4 = **453**; 5 = **427** (2 zonder case:
`jutterswet-seizoenvondst`, `jutterswet-vormenkabinet`); 6 = **427**;
7 = 391/36; **8 = FAIL, één punt (hieronder)**; 9 = 201/12; 10 = 173/38;
11 = 45/2/27/29 **onbewogen**; 12 = played **119** / kanaalvloer 47 /
JUnit **237**; 13 = **74** twists over 15 goldens / 70 replays / 13 PLAYED
door de DRL-tekst-tak / 0 survivors. Twaalf van de dertien groen met exact
de verwachte tellingen.

**BEVINDING (gate 8, blokkeert deze kaart).** `check-rood` meldt:

> `FAIL: reason drift — tijdsduur-reads-van-before-tot is listed as EXCLUDE
> but Drools now reports NOFAULT`

Gemeten met beide jars op dezelfde case, dus vastgesteld en niet afgeleid:

| constructie | uitslag |
|---|---|
| main 196ccea (drager + registry) | `EXCLUDE transpiler SKIP: regel: vaartduur van een vaart — regel (THEN-waarde refereert niet-geregistreerde global rekendatum)` |
| deze kaart (echte unie-knopen) | `NOFAULT ran to a model without faulting` |

Oorzaak, exact: `corpus/rood/tijdsduur-reads-van-before-tot/ir.json` draagt een
`Constante Rekendatum` **binnen de Let-body**
(`/resolvedRegels[0]/waarde/body/argumenten[0]`). Zolang een Let een
`Onopgelost`-drager was, was dat voor `Expr.bevat` een blad, zag
`heeftTijdgebonden` de Rekendatum niet, bleef de §4.2 rekendatum-global
ongeregistreerd en werd de regel LUID overgeslagen — waardoor het vuren nooit
werd bereikt. Met een echte `Expr.Let` daalt die scan af, de global wordt
geregistreerd, de regel lowert en vuurt. Wat de backend dán niet draagt is de
van/tot-ORDE zelf, dus hij loopt door tot een model.

Dit is dus geen regressie maar het wegvallen van een blinde vlek die
`LetVorm` zélf al als gat documenteerde ("een §5.3 Rekendatum achter een
drager laat de rekendatum-global ongeregistreerd"). De rode corpus ving het
binnen een dag na het landen van de nieuwe getuige.

**Wat het kost: één redencategorie in een BESCHERMD pad.** `scripts/` valt
buiten de may-touch van deze kaart én is een beschermd pad, en de opdracht
zegt uitdrukkelijk pins niet te verzetten — dus dit is NIET doorgevoerd. De
voorgestelde wijziging is regel 143 van `scripts/check-rood.sh`, waarmee de
case het exacte spiegelbeeld wordt van zijn Begrensd-tweeling op regel 130
(`begrensd-reads-bounds-in-order|NOFAULT …`), die uit dezelfde kaart komt:

```
tijdsduur-reads-van-before-tot|NOFAULT de v0-backend handhaaft geen van/tot-ORDE
op een TIJDSDUUR, dus de ch. 10.6-barrière die deze getuige plant wordt nooit een
fout; hij loopt door tot een model — het spiegelbeeld van begrensd-reads-bounds-in-order
hierboven. Tot de ch. 4.10-zijkanaalopruiming (2026-09-09) las deze case EXCLUDE om een
ANDERE reden: de bereikende regel (vaartduur van een vaart) lowerde in het geheel niet,
omdat een Rekendatum ín een Let-body onzichtbaar was voor Expr.bevat zolang een Let een
Onopgelost-drager was, zodat de §4.2 rekendatum-global ongeregistreerd bleef. Nu Let een
echte unie-knoop is daalt die scan af, wordt de global geregistreerd en bereikt de regel
het vuren
```

**Status: de kaart is NIET af** en blijft in `doing/`. Twaalf gates groen,
één rood op precies dit punt; het besluit over die ene regel ligt bij de
onderhouder. `git status --short` leeg; de branch blijft ONGEMERGED.
