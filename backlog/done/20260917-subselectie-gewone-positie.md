# subselectie-gewone-positie: draagt v2.3.0 "de Jutter die bevoegd is" buiten een vouw?

Aanleiding: de diedat-kaart vond dat een §5.6-subselectie op een
GEWONE (niet-vouw)positie — "indien de leeftijd van de Jutter die
bevoegd is groter is dan 5" — stil werd weggegooid, en heeft die
stilte dichtgezet met een ERROR die de emissie blokkeert: een
uitbreidingspunt met luide weigering, want wat de subselectie op een
scalaire navigatie BETEKENT is een spec-vraag. Maintainersopdracht
2026-09-17: die spec-vraag beantwoorden — ondersteunt RegelSpraak
v2.3.0 deze vorm? Zo ja, dan moet uchar hem aankunnen.

Fase 1 — onderzoek (het Result draagt de uitkomst ook als de bouw een
vervolgkaart wordt):

- De v2.3.0-grond: welke §5.6-/onderwerpsketenproducties dragen de
  die/dat-bijzin op een niet-vouwpositie, en met welke semantiek
  (selecteert de bijzin de INSTANTIEBINDING van het onderwerp — een
  guard op de regelinstantie — of iets anders)? Vindplaatsen in het
  Result (read-only raadplegen; niets citeren in corpusmateriaal).
- Wat doet de toolchain-lezing (de gevendorde frontend en de
  toolchain-eigen paden) vandaag met die vorm — en wat deed de
  oorspronkelijke toolchain vóór de frontend-overname (git-historie)?

Fase 2 — bij een JA: het IR-ontwerp (hoe landt de subselectie op een
gewone positie — een filter op de onderwerpsbinding? een nieuw
conditieveld? hergebruik van de vouwfilter-machinerie?) als voorstel
met WF-regels, en de bouw door de lagen die het ontwerp aanwijst.
Bij een NEE: de bestaande luide weigering wordt de gedocumenteerde
eindtoestand, met de v2.3.0-vindplaats als grond in de weigeringstekst
en in rij 40.

## Acceptatie (bij oppakken aanscherpen)

- (a) Fase 1 volledig in het Result, met vindplaatsen.
- (b) Bij JA: ontwerpvoorstel + bouw of een scherp voorstel voor een
  vervolgkaart (maintainerskeuze bij de landing); bij NEE: de
  weigeringstekst en rij 40 dragen de grond.
- (c) Alle veertien gates groen; registervoorstel in het Result.

## Result — fase 1 (onderzoek); fase 2 vervalt

**NEE. RegelSpraak v2.3.0 draagt de §5.6-subselectie NIET op een gewone,
niet-vouwpositie, en de bijzin selecteert daar dus ook geen
instantiebinding.** De grond is niet dat de spec over de vorm zwijgt — de
spec spreekt er expliciet over, en wat zij zegt sluit de gewone positie uit.
Een subselectie levert ALTIJD een meervoudige lezing op (§5.8, p. 59), en de
taal kent precies twee verbruikers van een meervoudige lezing: een aggregatie
(§5.8, p. 59) of een voorwaarde met een kwantificatie vóór de
onderwerpexpressie (hfst. 10-inleiding, p. 114). Beide zijn vouwen, en uchar
draagt ze allebei al. Er is geen derde lezing, en nergens in v2.3.0 staat een
guard-op-de-regelinstantie die door een die/dat-bijzin gezet wordt; de taal
heeft voor "welke exemplaren" het voorwaardendeel (*indien*) en voor "welk
van de gelijknamige onderwerpen" het rangtelwoord (§5.5.3, p. 54).

De bestaande luide weigering is daarmee de juiste eindtoestand. Haar TEKST is
dat niet: zij noemt de gewone positie "een benoemd uitbreidingspunt", en dat
is precies wat het onderzoek weerlegt. Er valt niets uit te breiden — er is
geen bronvorm om te bedienen. §4 draagt de letterlijke vervangtekst en de
rij-40-aanvulling.

### 1. De v2.3.0-grond

Twee bronnen, allebei read-only geraadpleegd; hieronder alleen vindplaatsen
en parafrase, geen tekst. (1) De ANTLR-afbeelding in `regelspraak-java`
onder `specification/v2.3.0/` — projecteigen afgeleide notities, zoals de
README daar zelf zegt; de normatieve tekst zit daar niet bij. (2) De
normatieve v2.3.0-specificatie zelf, met haar syntaxdiagrammen en de
typeringen-bijlage, die op deze machine staat onder de werkbank-toolchain
(`/Users/yeb/Developer/uwv/regelspraak-werkbank/toolchain/specification/v2.3.0`).
Alle paginanummers hieronder zijn die van de normatieve specificatie.

**De PRODUCTIE draagt de vorm overal.** §13.4.1.1 (`<onderwerpketen>`,
hoofdspec p. 131, met eigen syntaxdiagram) is een vlakke vijfweg
waarvan `<subselectie>` één alternatief is, en §13.4.2.8 (p. 132) definieert
`<subselectie> ::= <onderwerpketen> ("die"|"dat") <predicaat>`. De RHS van
§13.4.1.1 heeft in de bron één ongesloten haakje; zowel de ANTLR-afbeelding
als het mermaid-diagram modelleren de bedoelde vlakke keuze, en die twee zijn
het eens. Gevolg: overal waar een `<onderwerpketen>` staat mag een subselectie
staan — dus ook in de staart van §13.4.2.9 `<attribuutvanonderwerp>`
("de leeftijd van …"), in §13.4.5 `<kenmerktoekenning>`, in §13.4.6
`<objectcreatie>` en in §13.4.7 `<feitcreatie>`. Grammaticaal is de vorm op
een gewone positie dus toegestaan. Dat is ook precies waarom de frontend haar
parseert en waarom zij stil kón wegvallen.

**De SEMANTIEK sluit de gewone positie uit.** Drie vindplaatsen, samen
sluitend:

1. **§5 (p. 42)** definieert het onderscheid: een expressie die één waarde
   oplevert is enkelvoudig, een expressie die een lijst waarden oplevert is
   meervoudig. Plurality is dus een eersterangs eigenschap van expressies, met
   eigen regels — niet een stijlkwestie.
2. **§5.6 (p. 57)** zegt in haar eerste zin wat een subselectie IS: het
   mechanisme om een GROEP instanties of waarden te maken die aan
   selectiecriteria voldoen. Beide uitgewerkte voorbeelden van §5.6 staan in
   een telling: de bijzin staat achter het getelde element, binnen "het
   aantal …". §5.7 (p. 58) beschrijft concatenatie vervolgens als het
   samenvoegen van de VERZAMELINGEN die zulke selecties opleveren. De
   subselectie is in v2.3.0 dus een verzamelingsconstructor, niet een
   aanwijzer.
3. **§5.8 (p. 59)** maakt dat normatief en categorisch. De paragraaf somt op
   welke expressies meervoudig KUNNEN zijn — een concatenatie altijd, een
   selectie soms (als haar object meervoudig is, of als haar rolselector
   meervoudig is) — en zet de subselectie in datzelfde rijtje als derde punt,
   met "altijd". Dezelfde paragraaf definieert aggregatie als precies de
   operatie die een meervoudige invoer tot een enkelvoudig resultaat maakt.

Uit (3) plus §5.8-punt 2a volgt de sleutelstap voor de kaartzin: als de
onderwerpexpressie achter "van" meervoudig is, is de SELECTIE zelf ook
meervoudig. "de leeftijd van de Jutter die bevoegd is" is dus zelf een
meervoudige expressie — een lijst leeftijden — en niet een getal.

**En de vierde vindplaats zegt wat er dan moet gebeuren.** De inleiding van
hoofdstuk 10 (p. 114) scheidt enkelvoudige van meervoudige voorwaarden en
stelt de eis: bij een meervoudige voorwaarde moet de onderwerpexpressie
meervoudig zijn én moet er een KWANTIFICATIE vóór staan. Het uitgewerkte
meervoudige voorbeeld op diezelfde pagina heeft die kwantificatie ook: het
schrijft "alle" zowel voor het gelezen attribuut als voor de leden waarover
het kwantificeert, en daarna pas de vergelijking. De kaartzin — *indien de
leeftijd van de Jutter die bevoegd is groter is dan 5* — heeft een
meervoudige onderwerpexpressie zonder kwantificatie en zonder aggregatie. Die zin is in v2.3.0 niet onvolledig ondersteund; zij is
niet welgevormd.

**Wat de spec WEL biedt op de plek waar de kaartzin naar reikt.** Twee
mechanismen, beide bestaand en beide al in uchar:

- de aggregatie (§5.8) — "de som van / het aantal / de maximale waarde van …
  van alle vondsten van de Jutter die waardevol zijn"; dit is de instantievouw
  (IR-SPEC ch. 4.3, veld `filter`);
- de gekwantificeerde voorwaarde (hfst. 10-inleiding, p. 114) — "indien … van
  ALLE/GEEN VAN DE/TEN MINSTE n VAN DE … die waardevol zijn …"; dit is de
  kwantorvouw (ch. 4.5, veld `filter`).

**Twee expliciete uitsluitingen van de guard-lezing.** §5.5 (p. 51) en §5.5.3
(p. 52-54) leggen de instantiebinding uit: de onderwerpexpressie wordt bij
uitvoering aan instanties gebonden en de regel draait één keer per instantie;
het universeel onderwerp bepaalt op welke instanties. Er staat geen woord dat
een die/dat-bijzin die binding vernauwt. En §5.5.3 (p. 54) geeft de taal een
ANDER, specifiek middel voor de enige verwante behoefte — als een regel
meerdere onderwerpen met dezelfde rol- of attribuutnaam heeft, wordt de
bedoelde aangewezen met een RANGTELWOORD, niet met een bijzin. Het
versiebeheer (§1, rij 2.2.0, 4-9-2025) noemt dat rangtelwoordgebruik zelfs als
een bewuste toevoeging aan §5.5.3. Waar de taal die vraag beantwoordde,
antwoordde zij met een ordinaal.

**Geen enkel voorbeeld in de normatieve spec schrijft een subselectie buiten
een vouw.** Gemeten over het hele document: elk voorkomen van het woord
"subselectie" staat in §5.6, §5.8-punt 3, of in de formele syntax van hfst.
13. En er zijn in de hele normatieve specificatie precies DRIE uitgewerkte
zinnen met een die/dat-bijzin — twee in §5.6 en één in §5.7 — en alle drie
staan zij achter "het aantal". De TOKA-casus (de eigen uitgewerkte casus van
de spec) bevat er **nul**; dat sluit aan op de gate-11-meting, waar alle 45
TOKA-bronnen byte-identiek heremitteren zonder ooit op deze vorm te leunen.
De typeringen-bijlage zwijgt over plurality volledig — zij is een
datatype-matrix — dus de plurality-regels staan uitsluitend in het proza van
§5, §5.8 en hfst. 10.

**Eén losse waarneming, gemeld omdat zij anders als tegenbewijs kan worden
gelezen.** Het tweede §5.6-voorbeeld schrijft het samengestelde predicaat in
het ENKELVOUD terwijl het over de leden gaat; het eerste schrijft meervoud.
De inflectie is daar dus niet consistent. Dat raakt de conclusie niet: §5.8
maakt de uitspraak over de LEZING, niet over de werkwoordsvorm, en de
frontend leest beide registers al (grammar-commentaar bij `predicaat`). De
enkelvoudsvorm in de kaartzin ("die bevoegd IS") is dus geen aanwijzing voor
een enkelvoudige lezing.

### 2. Wat de gereedschappen vandaag doen

#### 2a. De gevendorde frontend — 34 schrijfposities, twee keer gemeten

Meetharnas: `Resolver.resolve` op een Jutter/Vondst-model, gecompileerd tegen
`frontends/regelspraak/out`. Per positie geteld: aantal §5.6-weigeringen,
aantal fouten, alle diagnostiek. De weigeringen komen uit
`RuleChecker.keurDeelselecties` (r. 113-127), die na afloop de hele parseboom
afloopt en elke `PredicaatContext` weigert die
`ExpressieBouwer.verbruikteDeelselecties()` niet noemt.

Twee metingen, omdat `main` tijdens deze kaart verschoof: eerst op de basis
van de kaart (`4a1e524e`), daarna op `f08cf7cc` — de landing van
`niettemporele-vouw-uit-bron`, die de §8.2-poort van de kwantorvouw weghaalde.
Het weigerblok zelf (javadoc r. 94-112 plus `keurDeelselecties` r. 113-127) is
tussen die twee commits BYTE-IDENTIEK, dus de weigering is niet bewogen; wat
bewoog is wat er omheen gelowerd wordt. Deze kaart is op `f08cf7cc` gerebased
en de cijfers hieronder zijn die van `f08cf7cc`; waar de twee verschillen
staat het erbij.

**De weigering valt op ELKE niet-vouwpositie, uniform, en identiek in beide
metingen (tien van de 34):**

| positie | zin (kern) | uitkomst |
|---|---|---|
| A voorwaarde, scalaire navigatie (de kaartzin) | `indien de leeftijd van de Jutter die bevoegd is groter is dan 5` | 1 weigering, 1 fout |
| B idem, meervoudsinflectie | `… die bevoegd zijn …` | 1 weigering, 1 fout |
| C resultaatdeel, gelijkstelling-LHS | `Het vindersloon van een Jutter die bevoegd is moet gesteld worden op 10` | 1 weigering, 1 fout |
| D resultaatdeel, kenmerktoekenning | `Een Jutter die bevoegd is is gemachtigd` | 1 weigering, 1 fout |
| E gelijkstelling-RHS, scalair | `… moet gesteld worden op de leeftijd van de Jutter die bevoegd is` | 1 weigering, 1 fout |
| F variabelendeel | `de x is de leeftijd van de Jutter die bevoegd is` | 1 weigering, 1 fout |
| G navigatie over een enkelvoudige rol | `de leeftijd van de aanmelder van de Vondst die bevoegd is` | 1 weigering, 1 fout |
| I objectcreatie | `Een Jutter die bevoegd is heeft een vondst` | 1 weigering, 1 fout |
| J consistentieregel, enkel criterium | `De leeftijd van de Jutter die bevoegd is moet groter zijn dan 5` | 1 weigering, 1 fout |
| K samengesteld predicaat op een gewone positie | `… van de Jutter die aan alle volgende voorwaarden voldoet: …` | 1 weigering, 1 fout |

De weigering citeert de bijzin letterlijk ("bevoegd is", "bevoegd zijn",
"aan alle volgende voorwaarden voldoet: - hij is bevoegd - hij is
gemachtigd"), is ERROR, en blokkeert de emissie. Geen enkele positie ontsnapt;
geen enkele wordt dubbel gemeld.

**De vouwposities blijven ongemoeid — dertien van de 34, nul diagnostiek, op
`f08cf7cc`:** de telling met kenmerkbijzin (M1) en met getalpredicaat-bijzin
(N8), de samengestelde bijzin op de instantievouw (M6), de §5.7-unie met een
bijzin per operand (M7), `de som van`/`de maximale waarde van`/`de eerste van`
met een kenmerkbijzin in de corpusgedragen spelling (N1/N2/N3), de
getalpredicaat-bijzin in de `jutterswet-drempelvangst`-spelling (N7), de
§8.2-gekwalificeerde kwantorvouw (H, L), **en sinds `f08cf7cc` ook de
ONGEKWALIFICEERDE hfst.-10-vorm in beide spellingen (M8, N4).**

**Dat laatste is de belangrijkste verschuiving tussen de twee metingen, en zij
valt precies de goede kant op.** Op de kaartbasis leverde N4 — *indien het
gewicht van alle vondsten van de Jutter die waardevol zijn groter is dan 5*,
oftewel exact de vorm die hfst. 10 p. 114 als de LEGALE meervoudige voorwaarde
voorschrijft — twee fouten op: WF-096 (`setlezing buiten een vouwbron`, de
staande §8.2-poort) plus de deelselectie-ERROR. Op `f08cf7cc` levert diezelfde
zin **nul diagnostiek**: de kwantorvouw lowert haar, en de bijzin wordt
verbruikt als `filter` (ch. 4.5). De consequentie voor deze kaart is niet
klein: de v2.3.0-conforme manier om "de Jutter die bevoegd is" in een
voorwaarde te schrijven komt sinds die landing uit bron, dus de weigering op de
gewone positie blokkeert geen enkele uitdrukbare bedoeling meer — zij stuurt
naar een vorm die WERKT.

**Wat er op `f08cf7cc` nog overblijft naast de tien gewone posities.** Drie
schrijfvormen (vier metingen), elk met een zonder-bijzin-controle:

| vorm | met bijzin | zonder bijzin (controle) |
|---|---|---|
| N5 hfst.-10-vorm mét §8.2-kwalificatie op een NIET-temporeel attribuut | §8.2-ERROR + deelselectie-ERROR | de §8.2-fout valt hoe dan ook — een echte typefout, geen positiegrens |
| M5 telling over een niet-gedragen navigatie ("de gewichten van de vondsten van de Jutter") | deelselectie-ERROR | de vouwbron bindt daar sowieso niet; N7/N8 tonen dat de gedragen spelling schoon resolvet |
| M9 / P1 §9.5 uniciteitscontrole met bijzin | WARNING + deelselectie-ERROR | P2: **alleen WARNING — dit document zou wél emitteren** |
| M10 §10.2 samengestelde voorwaarde, meervoudig subject | deelselectie-ERROR | P3: 0 diagnostiek, maar de wire draagt `Samengesteld` met één `Onopgelost`-onderdeel — het document faalt bij evaluatie op WF-044 |

Dus: **op geen enkele gemeten positie maakt de weigering van een WERKEND
uchar-document een geweigerd document.** Op precies één vorm — de
§9.5-uniciteitscontrole (M9/P1 vs P2) — verschuift het oordeel van "emitteert
met een waarschuwing" naar "geblokkeerd", en dáár is de bijzin volgens
§9.5/`<alleattribuutvanonderwerp>` wél legaal. Praktisch is dat vandaag geen
verlies, want de consistentieregel wordt zelf "nog niet (volledig) omgezet";
als die ooit landt, wordt het een echte overweigering. Dat is de enige
gemeten rand van de huidige weigering, en zij staat hieronder in §5.

Drie metingen (M2/M3/M4, `som`/`maximale waarde`/`eerste van` over de
navigatie "de gewichten van de vondsten van de Jutter") gaven een
`Onopgelost`-filter met een WARNING in plaats van een weigering, in beide
metingen gelijk. Dat is een spellingsartefact van mijn eerste probe: in de
corpusgedragen spelling ("van ALLE vondsten van de Jutter die …", N1/N2/N3)
resolven dezelfde drie schoon. Het is dus geen grens van de aggregatiefamilie.

#### 2b. De oorspronkelijke toolchain vóór de frontend-overname

Gelezen in `regelspraak-java` (read-only). De overname is
`9f7b001` ("Invert the dependency: consume the vendored frontend, delete
toolchain copies", 2026-08-29); daarvóór had de toolchain zijn eigen
frontendkopie. In `9f7b001^` staat de hele §5.6-lezing op **één** plek:

```
9f7b001^:src/main/java/…/resolver/ExpressieBouwer.java:837
    if (t instanceof OnderwerpKetenContext k && k.predicaat() != null) return k.predicaat();
```

Dat is `vindDie(ParseTree)`, en die wordt uitsluitend aangeroepen vanuit het
aggregatiepad (de enkel-operand vouw en de §5.8.1-unie van tellingen). Er is
geen tweede bezoeker van `predicaat()` in de hele toolchain-resolver, en geen
enkele diagnose over een niet-verbruikte bijzin: `deelselectie` komt in
`RuleChecker` alleen voor als commentaar bij de samengestelde vouwfilter, en
in `DrlTranspiler`/`ExecModelTranspiler` alleen bij het accumulate-SOURCE-patroon.
Beide transpilers krijgen de filter dus alleen van de vouw, nooit van een
gewone positie. Conclusie: **de oorspronkelijke toolchain gooide de bijzin op
elke niet-vouwpositie stil weg — geparseerd, nooit gelezen, nooit gemeld.**

De werkbank-toolchain (`/Users/yeb/Developer/uwv/regelspraak-werkbank/toolchain`,
package `nl.uwv.regelspraak`) draagt dezelfde code onder een andere naam:
`resolver/ExpressieBouwer.java:824` is dezelfde `vindDie` met hetzelfde
commentaar, en de hele `src/main/java` bevat nul treffers op "deelselectie".
Dat is dus geen onafhankelijke bevestiging maar dezelfde lijn — het is wel het
bewijs dat het gedrag in beide takken van de productielijn hetzelfde was en is.

Het gedrag van de frontend vóór de diedat-kaart (2026-09-16) is in die kaart
al gemeten en hier niet herhaald: een `Vergelijking` met de bijzin nog in
`onderwerpBron`, geen spoor van het kenmerk in de conditie, nul diagnoses.

### 3. De conclusie, met haar bewijs

De vraag van de kaart was tweeledig. Beide helften vallen negatief uit:

- **Draagt v2.3.0 de vorm op een gewone positie?** Grammaticaal ja, semantisch
  nee. De productie laat haar overal toe (§13.4.1.1/§13.4.2.8), maar §5.8
  (p. 59) geeft haar precies één lezing — altijd meervoudig — en de taal
  levert voor een meervoudige lezing precies twee verbruikers, allebei vouwen
  (§5.8 aggregatie; hfst. 10-inleiding p. 114 kwantificatie). Op een gewone
  positie staat geen van beide.
- **Selecteert de bijzin de instantiebinding van het onderwerp?** Nee. De
  instantiebinding wordt in §5.5/§5.5.3 (p. 51-54) door de onderwerpexpressie
  gelegd en door het voorwaardendeel beperkt; voor het aanwijzen van het
  bedoelde onderwerp geeft §5.5.3 (p. 54) het rangtelwoord. Een
  guard-semantiek voor de die/dat-bijzin staat nergens in v2.3.0.

**En de legale alternatieve vorm is sinds `f08cf7cc` geen theorie meer.** De
landing van `niettemporele-vouw-uit-bron` haalde de §8.2-poort van de
kwantorvouw weg, dus de v2.3.0-voorgeschreven manier om deze bedoeling op te
schrijven — dezelfde bijzin, mét een kwantificatie ervoor — komt nu uit bron
en draagt de bijzin als `filter` op de wire (gemeten: N4/M8, nul diagnostiek).
De weigering op de gewone positie ontneemt de schrijver dus niets; zij wijst
hem naar de zin die de taal bedoelt en die de hele keten aankan.

**Fase 2 vervalt daarmee.** Er is geen IR-ontwerp nodig: er is geen bronvorm
om te bedienen. Een filterveld op een scalaire navigatie zou een
uchar-DIALECTuitbreiding zijn, geen v2.3.0-conformiteit, en de kaart vraagt
daar niet om. Als de maintainer ooit tóch zo'n dialectvorm wil, is de
goedkoopste route gemeten en bekend — de vouwfilter-machinerie is al generiek
(`ExpressieBouwer.deelselectie(ParseTree, Objecttype, Expr)` bedient sinds de
diedat-kaart zowel ch. 4.3 als ch. 4.5) — maar dat is dan een
dialect-kaart met een eigen CONSTRUCTS-rij, niet deze.

### 4. Registervoorstel (letterlijk, voor de hoofdsessie)

#### 4a. De weigeringstekst — `frontends/regelspraak/.../RuleChecker.java`

De huidige tekst zegt "op deze positie is zij een benoemd uitbreidingspunt".
Dat is na dit onderzoek onjuist: de positie heeft in v2.3.0 geen lezing, dus
er valt niets uit te breiden. Voorgestelde vervanging van de
boodschapstring in `keurDeelselecties` (r. 119-125), letterlijk:

```
"de 'die/dat'-deelselectie '" + src(p) + "' wordt niet omgezet en zou stil wegvallen: een "
+ "deelselectie kiest WELKE exemplaren de zin bedoelt, dus haar weglaten beantwoordt een "
+ "andere vraag dan er staat. Een subselectie is in RegelSpraak v2.3.0 ALTIJD MEERVOUDIG "
+ "(§5.8), en een meervoudige lezing kent daar precies twee verbruikers: een aggregatie "
+ "(§5.8) of een voorwaarde met een kwantificatie vóór de onderwerpexpressie (hfst. 10). "
+ "Op deze schrijfpositie staat geen van beide, dus de zin heeft in v2.3.0 geen lezing — "
+ "deze weigering is de eindtoestand en geen uitbreidingspunt. Schrijf de selectie als "
+ "vouwbereik (IR-SPEC ch. 4.3 of ch. 4.5, veld 'filter') of zet er een kwantificatie voor "
+ "(§5.6)"
```

Het javadoc-blok erboven (r. 94-112) draagt dezelfde onjuiste grond
("what a subselection means on a scalar navigation is a spec question") en
zou in dezelfde beweging mee moeten. Voorgestelde kernzin, letterlijk:

> *This is not an extension point. Measured against RegelSpraak v2.3.0
> (card subselectie-gewone-positie, 2026-09-19): a subselectie is ALWAYS
> plural (§5.8), and the language admits exactly two consumers of a plural
> reading — an aggregation (§5.8) and a condition whose subject expression
> carries a quantifier (ch. 10). Both are folds, both already reach the wire
> (ch. 4.3 / ch. 4.5, `filter`). A subselection on an ordinary position has
> no reading in the source language at all, so this refusal is the
> DOCUMENTED END STATE and not a placeholder for a semantics to be built.*

**Niet gebouwd in deze kaart** — `May touch` beperkt haar tot de
kaartverplaatsing en dit Result, en acceptatie (b) vraagt de weigeringstekst
als registervoorstel. De wijziging is één string plus één javadoc-blok in
`frontends/regelspraak/src/main/java/io/github/yhavinga/regelspraak/resolver/RuleChecker.java`,
plus de assertie in
`src/test/java/io/github/yhavinga/regelspraak/DeelselectieZonderVouwTest.java:63`,
die op `contains("bevoegd is")` toetst en dus ongewijzigd blijft. Een
vervolgkaartje van een half uur; of de hoofdsessie neemt hem mee.

#### 4b. Rij 40 in `spec/CONSTRUCTS.md` — aan te vullen achter de bestaande tekst

De clausule die dit onderzoek raakt staat middenin rij 40: "**A SUBSELECTION
ON AN ORDINARY (NON-FOLD) POSITION is the named extension point of this row's
neighbourhood**… so this is a loud refusal at an extension point… not a
semantics." Die formulering wordt door dit onderzoek VERSMALD: de refusal
blijft, de kwalificatie "extension point" vervalt. De aanvulling hieronder
komt ACHTER de clausule die `niettemporele-vouw-uit-bron` op 2026-09-19
toevoegde (de §8.2-poort), en leunt daarop: die landing maakte de legale
alternatieve vorm uit bron mogelijk, en dat is precies wat deze negatieve
sluiting draagbaar maakt.

De voorgestelde tekst draagt §-nummers, paginanummers en parafrase van
v2.3.0-semantiek, en geen brontekst. Dat is dezelfde vorm als de bestaande
rij 40 (die §8.2 al citeert en parafraseert) en `spec/CONSTRUCTS.md` valt niet
onder de `$ZONES` van poort 1; de maintainer beslist bij de landing. Letterlijk:

> **Settled (2026-09-19, kaart subselectie-gewone-positie) — the "extension point" above is CLOSED, and closed NEGATIVELY: the clause that calls an ordinary-position subselection a named extension point now reads as the DOCUMENTED END STATE.** The source language was consulted and it is not silent: RegelSpraak v2.3.0 §13.4.1.1/§13.4.2.8 admit `<subselectie>` as one of `<onderwerpketen>`'s five alternatives, so the form PARSES everywhere an onderwerpketen may stand — which is exactly why it could fall silently — but §5.8 (p. 59) gives it exactly one reading, and that reading is categorical: a subselectie is ALWAYS plural, listed there beside concatenation (always plural) and selection (plural when its object or its role selector is). §5.8 in the same breath defines aggregation as the operation taking a plural input to a singular result, and the chapter-10 opening (p. 114) states the only other admission: a plural condition requires the onderwerpexpressie to be plural AND to carry a KWANTIFICATIE, which is the worked plural example on that page. Two consumers of a plural reading, both folds, both already on the wire as `filter` (ch. 4.3 instance fold, ch. 4.5 quantifier fold). A third does not exist. Nor does the language ever write the form outside a fold: the whole normative specification carries exactly THREE worked sentences with a die/dat bijzin — two in §5.6, one in §5.7 — and all three sit behind "het aantal"; the spec's own TOKA casus carries NONE. And the guard reading — the one this card was cut to test, that the bijzin narrows the rule's INSTANCE BINDING — is excluded twice over: §5.5/§5.5.3 (p. 51-54) bind the onderwerpexpressie to instances and run the rule once per instance with no mention of a die/dat narrowing, and where the language DID need to say which of several like-named subjects is meant it added an ORDINAL for it (§5.5.3, p. 54; recorded as a deliberate addition in the v2.3.0 changelog for 2.2.0). So "indien de leeftijd van de Jutter DIE BEVOEGD IS groter is dan 5" is not an unsupported feature: it is a plural expression in a scalar slot with neither an aggregation nor a quantifier, and it is not a well-formed v2.3.0 sentence. The refusal stays an ERROR, its text gains this ground, and there is no fase-2 IR design: a `filter` on a scalar navigation would be a uchar DIALECT extension with a row of its own, not v2.3.0 conformance. And the clause directly above this one is what makes the negative closure carry no cost: with the §8.2 gate gone, the v2.3.0-prescribed way to write this intent — the SAME bijzin with a kwantificatie in front of it — now comes out of `bron.rgs` and lands as `filter` (ch. 4.5). The refusal on the ordinary position therefore takes nothing away; it points at the sentence the language means. Measured on the vendored frontend over 34 written positions, twice — once on this card's base and once on the commit that closed the gate, with the refusal block (javadoc + `keurDeelselecties`) BYTE-IDENTICAL between them: the refusal fires on all ten ordinary ones (voorwaarde, gelijkstelling LHS and RHS, kenmerktoekenning, variabelendeel, role navigation, objectcreatie, consistentie criterium, compound predicate, and the plural inflection of the same) in both, and on none of the thirteen fold positions in the later one (telling with either predicate class, som, maximale waarde, eerste van, the §5.7 union, the compound subselectie, the §8.2-qualified quantifier fold, and — newly — the UNQUALIFIED chapter-10 form in both spellings, which on the base still fell on WF-096 and now resolves with zero diagnostics). On the three FURTHER shapes where it still fires beside a STANDING refusal, a without-bijzin control shows the document was already refused or already `Onopgelost` — with exactly one exception, the §9.5 uniciteitscontrole, where the bijzin is legal per `<alleattribuutvanonderwerp>` and the document would otherwise emit with the standing "consistentieregel wordt nog niet (volledig) omgezet" WARNING; that is the one measured over-reach and it is harmless while the consistentieregel is unlowered. What the ORIGINAL toolchain did is the same thing the frontend did before 2026-09-16, and the git history says so exactly: at `9f7b001^` (the commit before the frontend takeover) the whole §5.6 reading sits at ONE call site, `ExpressieBouwer.vindDie`, reachable only from the aggregation path, with no diagnostic anywhere for an unconsumed bijzin — parsed, never read, never reported. The werkbank toolchain carries that same code under another package with the same single site and zero mentions of the refusal. The silence this row closed was the production behaviour, in both branches of the line

### 5. Gemeten randen die deze kaart NIET gerepareerd heeft

Drie, elk met zijn meting, elk expliciet buiten `May touch` gelaten. (i) en
(iii) raken de markerdiscipline en horen daarmee in de buurt van
`backlog/new/20260919-frontend-stille-markers.md`, die de maintainer bij de
vorige landing al opende; (ii) is tijdens deze kaart vanzelf vervallen.

- **(i) De §9.5-uniciteitscontrole is de enige gemeten overweigering.**
  P1 vs P2 hierboven. Voorstel als het ooit knelt: `keurDeelselecties` laat
  een bijzin binnen een `uniciteitsControle`-subtree staan zolang de
  consistentieregel zelf als "nog niet volledig omgezet" gewaarschuwd wordt —
  de WARNING dekt de stilte dan al. Dat is een kaartje van een uur en het
  wacht best tot de consistentieregel landt.
- **(ii) VERVALLEN tijdens de kaart.** Op de kaartbasis kregen N4 en N5 TWEE
  foutmeldingen op één zin, en de tweede beschreef de positie verkeerd: dat
  ÍS een vouwpositie, alleen een die de frontend toen niet uit bron lowerde.
  De landing van `niettemporele-vouw-uit-bron` (`f08cf7cc`) haalde de
  §8.2-poort weg, en N4 is daarmee nul diagnostiek geworden. Wat overblijft is
  N5, en daar is de dubbele melding terecht: de §8.2-kwalificatie op een
  niet-temporeel attribuut is een echte typefout náást de niet-bindende
  bijzin.
- **(iii) De navigatie "… van de `<meervoudsrol>` van de `<onderwerp>` die …"**
  bindt de vouwbron niet (M2/M3/M4 leveren een `Onopgelost`-filter met een
  WARNING, M5 een weigering); de corpusgedragen spelling "… van ALLE
  `<elem>` van de `<onderwerp>` die …" bindt wél (N1/N2/N3/N7/N8, nul
  diagnostiek). Dit raakt de subselectie niet — het is de navigatie die niet
  bindt — en het staat hier alleen zodat die vier metingen niet voor een
  filtergrens worden aangezien.

### 6. De veertien poorten

Documentkaart; er is geen code of corpus aangeraakt, dus dit is een
onaangeroerd-groen-controle.

```
1  check-self-contained: PASS
2  check-ir-schema: PASS (schema valid; 24 pattern self-test rows; 230 files validate and round-trip, floor 192)
3  check-verifier: PASS (230 golden clean; 395 negatief rejected for their declared rule, floor 350)
4  check-case-schema: PASS (schema valid; 554 case files validate)
5  check-oracle: PASS (ref suite 643 worked examples, floor 489; 526 cases replayed exactly, floor 427)
6  check-clingo: PASS (backend suite 362 checks, floor 83; 526 cases replay exactly through clingo)
7  check-noop: PASS (490 cases refute a do-nothing executor; 36 pinned survivors)
8  check-rood: PASS (28 red cases fault observably in ref and clingo, floor 26; Drools leg 6 fault-pinned, 22 not-carried with reason)
9  check-catala: PASS (frontend suite 201 checks, floor 201; 12 catala goldens regenerate byte-identically, floor 12)
10 check-regelspraak: PASS (JUnit 298 tests, floor 173; 56 rgs goldens re-emit byte-identically, floor 38)
11 check-toka-parity: PASS (29 cases: ref 29 PASS / clingo 29 PASS / Drools 29 PLAYED, 0 divergences)
12 check-drools: PASS (played 153 = pinned 153; channel floor 49; value-divergence 1; JUnit 455 tests, floor 239)
13 check-reftype-derivation: PASS (108 annotations twisted over 22 goldens, floor 74; 100 replays, 21 DRL-text PLAYED, floor 13)
14 check-flogic: PASS (frontend suite 130 checks, floor 81; 2 flogic goldens re-emit byte-identically, floor 1)
```

Gedraaid op de gerebasede boom (`f08cf7cc` + de twee kaartcommits).
Exitcodes 0 over de hele rij, in deze volgorde. Vier van de veertien
scripts staan in de index als `100644` (`check-case-schema`, `check-clingo`,
`check-oracle`, `check-verifier`) en zijn dus met `bash ./scripts/<naam>.sh`
gedraaid; dat is de stand op `main` en niet door deze kaart veroorzaakt of
gewijzigd (`scripts/` is beschermd).

### 7. Eerlijke twijfel

- **De sterkte van de conclusie rust op één normatieve zin.** §5.8-punt 3
  ("altijd meervoudig") is de spil; haal die weg en de gewone positie wordt
  weer open. Ik heb geprobeerd haar te ontkrachten en dat is niet gelukt: de
  paragraaf is normatief proza in de expressie-hoofdstukken, zij staat naast
  twee andere plurality-uitspraken die onomstreden zijn, de
  hfst.-10-inleiding sluit er precies op aan, en géén enkel voorbeeld in de
  hele spec schrijft de vorm anders. Maar het is één zin, en dat hoort de
  maintainer te weten.
- **De §13.4.14.1-kwantificatie is dubbelzinnig.** De samengestelde voorwaarde
  neemt een `<voorwaardekwantificatie>` die zowel als instantiekwantor
  (`geen van de`, `ten minste n van de`) als als criteriumkwantor (`alle`
  volgende voorwaarden) leest. Bij M10/P3 kon ik daardoor niet hard maken of
  een meervoudig subject dáár legaal is. Het raakt de conclusie niet — de
  kaartzin is een elementaire voorwaarde, niet een samengestelde — maar als
  iemand de grens ooit oprekt, is dit de plek waar de spec het antwoord niet
  geeft.
- **De werkbank-toolchain is geen tweede getuige.** Zelfde code, andere
  package. Ik heb hem nagelezen maar niet gedraaid; een onafhankelijke
  implementatie van v2.3.0 om tegen af te zetten bestaat in dit huis niet.
- **`main` verschoof tijdens de kaart, en ik heb de meting opnieuw gedaan in
  plaats van de oude te melden.** De kaart begon op `4a1e524e`; toen ik ging
  landen stond `main` op `f08cf7cc` (de §8.2-poort weg). Het weigerblok is
  tussen die twee byte-identiek, dus de tien gewone posities zijn onveranderd,
  maar de vouwkant is wél verschoven — N4/M8/N6 gingen van WF-096 naar nul
  diagnostiek. De kaart is op `f08cf7cc` gerebased, §2a draagt beide metingen
  en de poortentabel is op de gerebasede boom gedraaid. Wat ik niet heb gedaan:
  de §1-spec-lezing overdoen, want die hangt niet aan uchar-code.
