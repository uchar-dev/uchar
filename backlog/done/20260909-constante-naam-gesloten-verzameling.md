# `Constante.naam`: de contextwaarde-naam die niets kruist

Goal: sluit de laatste ongekruiste naam op de wire die de vier zones wél
dragen — de `naam` van een `Constante` (ch. 4.1's ContextValue-rij). Anders dan
WF-034's naamklasse is dit **geen resolutievraag**: er is geen
declaratietabel in het document om tegen te kruisen. De toelaatbare namen
liggen vast in de spec, dus de clausule die hier hoort is een **opsomming**, en
die opsomming schrijven is een scope-beslissing over wat v1 toelaat — niet een
arm die een bestaande tabel raadpleegt. Daarom staat deze post na de kaart
`20260906-dagsoort-kruisingsarm` nog steeds in de open lijst van ch. 4.6, met
de meting hieronder, en daarom is hij een eigen kaart.

## Waarom hij niet bij de dagsoort-kruisingsarm hoorde

Die kaart schreef WF-034 (s): elke waarde van `dagsoortPerEenheid` noemt een
dagsoort die `dagsoorten` declareert. Dat is een naam tegen een tabel die het
document zelf draagt — dezelfde vorm als (j)–(r). `Constante.naam` heeft die
tabel niet. Bovendien: de arm-exacte mutant van die kaart (alleen die arm weg →
alleen dat ene document weer schoon) blijft alleen scherp zolang de kaart één
arm en één getuige draagt. Beslissing en motivering staan in de Results van die
kaart.

## De meting (2026-09-09, op `card/dagsoort-kruisingsarm`, hermeten)

Vier zones = 555 documenten (164 golden + 23 rood + 338 negatief + 30
pariteit).

| wat | gemeten |
| --- | --- |
| `Constante`-knopen over de vier zones | **13**, over **5** documenten |
| spellingen | **2**: `de dag` (11, in 3 documenten), `Rekendatum` (2, in 2 documenten) |
| dragers | `corpus/golden/jutterswet-schouwrooster` (4× `de dag`), `corpus/negatief/wf034-dagsoorteenheid-onbekende-dagsoort` (4×, kopie), `toka/casus` (3×), `corpus/golden/anchor/jutterswet-penningvintage` (1× `Rekendatum`), `corpus/rood/vintage-uncovered-date` (1× `Rekendatum`) |
| wat ch. 4.1 opsomt | `Rekendatum` en `Rekenjaar` |

Twee gaten meteen zichtbaar: de wire draagt met `de dag` een naam die de rij
níét noemt, en `Rekenjaar` — die de rij wél noemt — wordt door **geen enkel**
document gedragen.

### Wie het veld leest, en hoe

- **verifier**: leest `naam` nergens. Alleen `typ` wordt gekruist.
- **`ref`** (`ref/lib/eval.ml`): `norm`-vergelijking (dus
  hoofdletterongevoelig) op `Rekendatum` → de evaluatiedatum; op `Rekenjaar` →
  een expliciete fault ("not exercised by any corpus case"); elke andere naam
  valt door naar `expression kind not implemented in this card`.
- **ASP-backend** (`backends/clingo/lib/translate.ml`): `norm`-vergelijking op
  `Rekendatum`; elke andere naam valt door naar de vertaalweigering.
- **Drools-backend**: **twee onafhankelijke lezers met verschillende
  gevoeligheid** — `DrlTranspiler.rekenConstante` doet
  `"Rekendatum".equals(c.naam())` (hoofdlettergevoelig!) en
  `kern/Dagsoorten` doet `"de dag".equalsIgnoreCase(c.naam())`. Een onbekende
  naam degradeert tot een niet-herkende operand, niet tot een diagnose.

### Gemeten mutanten (elk één veld, alle vier consumenten)

**A. `de dag` → `de dagX`** op `jutterswet-schouwrooster` (4 posities):

    verifier PASS | ref PASS (7) + PASS (3) | clingo PASS (7) + PASS (3) | drools PLAYED 2

Alle vier groen op dezelfde waarden — want geen enkele uitvoerder evalueert
vandaag een dagsoortdefinitie, en de Drools-lezer wordt alleen vanuit de twee
duurvormen (§6.10/§6.11) aangeroepen, die dit document niet draagt.

**B. `Rekendatum` → `RekendatumX`** op `anchor/jutterswet-penningvintage`:

    verifier PASS
    ref      EVALUATION FAULT: expression kind not implemented in this card
    clingo   TRANSLATION FAULT: expression kind not translated (no corpus case exercises it)
    drools   EXCLUDE (loader, om een andere reden: Expr:TemporalRead)

Luid in beide OCaml-uitvoerders, maar met een boodschap die het defect niet
noemt: "expression kind not implemented" is de doorvalregel, niet een
diagnose over de naam.

**C. `Rekendatum` → `rekendatum`** (alleen kapitalisatie), zelfde document:

    verifier PASS | ref PASS (2) | clingo PASS (2) | drools EXCLUDE (zelfde andere reden)

`ref` en de ASP-backend normaliseren en accepteren; de Drools-lezer doet
`equals` en zou hem **niet** herkennen. Dat is een kandidaat-divergentie
(`ref` ≡ clingo ≢ Drools) die dit corpus niet kan tonen: van de twee
`Rekendatum`-dragers is de golden om een andere reden EXCLUDE en de andere ligt
in rood. **Er is vandaag geen enkele door Drools gespeelde
`Rekendatum`-drager.**

## Wat er te beslissen valt

1. **Welke namen laat v1 toe?** `Rekendatum`, `Rekenjaar`, `de dag` — en is
   `de dag` een ContextValue of een eigen knoopsoort (het is de gebonden dag
   van een dagsoortdefinitie, ch. 3.7, geen sessiewaarde)? ch. 4.1's rij
   noemt hem nu niet, en dat is een fout in de rij of in de wire.
2. **Naam-identiteit**: hoofdletterongevoelig (zoals `ref`/clingo) of
   byte-exact (zoals één van de twee Drools-lezers)? Beide zijn te
   verdedigen; de meting zegt dat de consumenten het vandaag oneens zijn, en
   dat is precies de reden dat er een regel hoort te staan.
3. **Waar de clausule landt**: WF-034 (de naamklasse) of een eigen nummer.
   Argument voor een eigen nummer: WF-034 kruist tegen declaraties, deze
   clausule tegen een gesloten verzameling.
4. **`Rekenjaar` zonder drager**: een genoemde naam die geen document draagt en
   waarop `ref` bewust fault. Toelaten-en-onuitgevoerd, of uit de rij halen?

## Acceptance

- ch. 4.1's ContextValue-rij en de clausule (WF-034-arm of eigen nummer) zijn
  in overeenstemming met wat de wire draagt, met hertelde en **gedateerde**
  aantallen.
- Rood-eerst: minstens één negatief dat onder de verifier van vóór de kaart
  PASS is en daarna één regel met de regel-ID en de positie.
- Arm-exact: alleen die clausule weg → alleen de nieuwe getuige(n) weer schoon,
  over de volle vier-zone-sweep.
- Nul over-afwijzing, direct gemeten over alle vier zones.
- Als beslissing 2 op byte-exact uitkomt: de kapitalisatie-divergentie van
  meting C krijgt een drager die Drools daadwerkelijk speelt, of de kaart legt
  vast waarom die drager er niet kan zijn.
- Alle dertien gates groen.

May touch: `spec/verifier/**` (voorstel), `spec/IR-SPEC.md` (voorstel),
`corpus/negatief/**`, `corpus/golden/**` (als beslissing 2 een drager vraagt),
`backlog/`.

## Out of scope

- De dagsoort-duurvormen zelf (§6.10/§6.11) — eigen semantiekkaart.
- `dagsoortPerEenheid` zelf: de waardehelft is dicht (WF-034 (s),
  2026-09-09), de sleutelkant is bewust vrij gelaten met de motivering in
  ch. 2.9 en ch. 4.6 (s).

## Result (2026-09-10)

**Geland: WF-098.** De `Constante.naam` — de contextwaarde-naam die niets
kruiste — is een **gesloten verzameling van drie**, **byte-exact** op de wire:
`Rekendatum`, `Rekenjaar`, `de dag`. De clausule staat in ch. 4.1 naast de
lexeem-clausules WF-081/084/095 (dezelfde vorm: grammatica/verzameling,
motivering, meting, scope), de ContextValue-rij is bijgewerkt, er is een
registerrij in de WF-index, en de post is uit de open lijst gehaald met een
"left it on 2026-09-10"-zin in dezelfde alinea waar de vier eerdere
vertrekkers staan.

### Hermeting (2026-09-10, op HEAD d09434b)

De kaartmeting stond op 555 documenten; de vier zones plus de pariteitszone
zijn gegroeid.

| wat | kaart (2026-09-09) | hermeten (2026-09-10) |
| --- | --- | --- |
| documenten (vier zones + `toka/`) | 555 (164+23+338+30) | **578** (166 golden + 26 rood + 356 negatief + 30 pariteit) |
| `Constante`-knopen | 13, over 5 documenten | **14**, over **6** documenten |
| spellingen | 2 | **2** — `de dag` 11 (3 documenten), `Rekendatum` 3 (3 documenten) |
| dragers | 5 | + `corpus/rood/tijdsduur-reads-van-before-tot` (1× `Rekendatum`), nieuw sinds de kaartmeting |

De rest van de kaartmeting houdt stand: `Rekenjaar` wordt door geen enkel
document gedragen, en de rij noemde de wire-vaakste naam (`de dag`) niet.

**Mutanten hermeten**, elk één veld, op de binaries van vóór de clausule:

    A  de dag -> de dagX      (jutterswet-schouwrooster, 1 van 4 posities)
       verifier PASS | ref PASS (7) + PASS (3) | clingo PASS (7) + PASS (3)
       — byte voor byte de ongetwiste antwoorden

    B  Rekendatum -> RekendatumX  (anchor/jutterswet-penningvintage)
       verifier PASS
       ref     EVALUATION FAULT: expression kind not implemented in this card
       clingo  TRANSLATION FAULT: expression kind not translated

    C  Rekendatum -> rekendatum  (anchor/jutterswet-penningvintage)
       verifier PASS | ref PASS (2) | clingo PASS (2)

    D  de dag -> "De Dag"     (schouwrooster, 4 posities; extra gemeten)
       verifier PASS | ref PASS (7) | clingo PASS (7)

A, C en D zijn stil in alle drie; B is luid in beide OCaml-uitvoerders maar met
hun **doorvalboodschap**, die het defect niet noemt.

### Rood eerst, met echte output

Twee getuigen, elk een **eenregelige** hand-breuk van een golden:

- `corpus/negatief/wf098-onbekende-contextwaarde-naam` — één van vier
  `"naam": "de dag"` → `"de dagX"` op `corpus/golden/jutterswet-schouwrooster`.
- `corpus/negatief/wf098-contextwaarde-naam-kapitalisatie` — `"Rekendatum"` →
  `"rekendatum"` op `corpus/golden/anchor/jutterswet-penningvintage`.

Onder de verifier van vóór de clausule (build van HEAD d09434b):

    corpus/negatief/wf098-onbekende-contextwaarde-naam       verifier: PASS
    corpus/negatief/wf098-contextwaarde-naam-kapitalisatie   verifier: PASS

Na de clausule, elk één regel met regel-ID en positie:

    WF-098 FAIL /resolvedDagsoorten/0/voorwaarde/onderdelen/0/links/naam:
    context value 'de dagX' is not one of the three admissible names
    Rekendatum, Rekenjaar, de dag: the context-value name is a closed set,
    spelled byte for byte

    WF-098 FAIL /resolvedRegels/0/waarde/datum/naam: context value
    'rekendatum' is not one of the three admissible names Rekendatum,
    Rekenjaar, de dag: the context-value name is a closed set, spelled byte
    for byte

### Arm-exactheid en nul over-afwijzing, in één meting

Recept: vervang de tak `| Constante { naam; _ } -> (…)` in `walk_expr`
(`spec/verifier/lib/checks.ml`) door `| Constante _ -> []`. Volle sweep over
**580** documenten (de 578 plus de twee getuigen) in beide standen, output
gediffd:

    1140c1140
    < verifier: PASS
    ---
    > WF-098 FAIL /resolvedRegels/0/waarde/datum/naam: …'rekendatum'…
    1142c1142
    < verifier: PASS
    ---
    > WF-098 FAIL /resolvedDagsoorten/0/voorwaarde/onderdelen/0/links/naam: …'de dagX'…

Twee regels bewegen, allebei de nieuwe getuigen. Geen enkel bestaand document
verandert van verdict en geen enkel rapport elders krijgt een regel erbij —
over-afwijzing nul, direct gemeten over alle vijf zones.

**Tweede, fijnere val:** laat alleen de byte-exactheid vallen (`List.mem n […]`
→ een `norm`-vergelijking) en **alleen** de kapitalisatiegetuige wordt weer
schoon; de andere blijft afgewezen. De twee getuigen vallen dus onafhankelijk.

**Bereik — structureel, niet als steekproef.** De arm rijdt op `walk_expr` en
bereikt daarmee elke expressiepositie die die walk bereikt. Beide uiteinden
gemeten: alle **14** `Constante`-posities zijn één voor één getwist en de check
vuurt op alle 14, met de positie in de melding (plat `ALLE`-samenstel van een
dagsoortdefinitie, genest samenstel, functie-argument, datumindex van een
`TemporalRead`, `Let`-body); en van de **329** corpuspad-skeletten vuren er
**320** onder een geïnjecteerde contextwaarde — de 9 die dat niet doen zijn
slots die structureel geen `Constante` kunnen dragen of waar een andere regel
het document al afwijst (gemeten in de fixronde-lens, 2026-09-10).

### De vier besluiten, met de motivering die in de spec staat

1. **Drie namen, de wire volgend.** `Rekendatum`, `Rekenjaar`, `de dag`.
   Mirror-then-diverge: de wire is het v0-feit, en de wire draagt `de dag` het
   vaakst. De rij noteert dat `de dag` semantisch de **gebonden dag van een
   dagsoortdefinitie** is (ch. 3.7) — de willekeurige dag waar de definitie over
   gaat, geen sessiewaarde — als contextwaarde gespeld omdat v0 er geen eigen
   knoopsoort voor heeft. *Of v1 er een verdient is een open punt en is hier
   niet beslist* — één zin in de rij en één in de clausule, geen kaart.
2. **Byte-exact.** De lezers onder de wire zijn het oneens over kapitalisatie,
   en één backend met zichzelf: `ref` en de ASP-backend vergelijken
   genormaliseerd, de Drools-constantelezer met `equals`
   (`DrlTranspiler.rekenConstante`, `Expr.isRekenConstante`), de
   Drools-dagsoortlezer met `equalsIgnoreCase` (`kern/Dagsoorten`). De verifier
   wordt de poortwachter: met de naam byte-exact op de wire is de normalisatie
   in de twee OCaml-uitvoerders **onbereikbaar op elk verifier-schoon document**
   en hoeven de Drools-lezers niet te bewegen.
3. **Eigen nummer: WF-098.** WF-034 kruist tegen declaraties die het document
   draagt; deze clausule tegen een verzameling die de spec vastlegt.
4. **`Rekenjaar` blijft in de verzameling**, toegelaten-en-onuitgevoerd: geen
   drager, en `ref` fault er bewust op ("not exercised by any corpus case").
   Dat is het eerlijke antwoord voor een constructie die ch. 8.2 definieert en
   geen uitvoerder implementeert; hem schrappen zou een document ill-formed
   maken voor het gebruiken van iets dat de spec kent.

### De acceptance-eis over de Drools-drager

De kaart vroeg: een drager die Drools daadwerkelijk speelt met een
gerekapitaliseerde `Rekendatum`, **of** de vastlegging waarom die er niet kan
zijn. Het tweede is gekozen, en de reden is structureel, geen gat:

- Van de drie `Rekendatum`-dragers is `anchor/jutterswet-penningvintage`
  EXCLUDE bij de Drools-lader om een andere reden (`Expr:TemporalRead`), en de
  andere twee liggen in **rood**, waar falen verplicht is.
- Een drager erbij bouwen zou betekenen: een golden opnemen wiens hele inhoud
  een spelling is die drie consumenten zus en de vierde zo lezen. Dat is het
  defect, niet een test ervan.
- De wire dichtzetten is de andere route en die is genomen. Gevolg: `ref`,
  `backends/clingo` en `backends/drools` zijn door deze kaart **niet
  aangeraakt**.

Eerlijk residu, en het staat ook in de spec en in beide NOTE's: **een
handgebouwd document dat de verifier overslaat bereikt de uitvoerders nog
steeds met een niet-canonieke naam en splitst ze nog steeds.** De wire-regel
begrenst conforme documenten, niet elke invoer die een runner aangereikt kan
krijgen.

### Wat bewust niet bewoog

- **`ref`, de ASP-backend, de Drools-lezers.** Zie hierboven: met de wire dicht
  is normalisatie in de uitvoerder onbereikbaar op geldige documenten. Dit is
  gecontroleerd, niet aangenomen — het handgebouwde pad bestaat wél en is
  benoemd.
- **De lege helft, `naam: null`.** Schema-geldig (`["string", "null"]`), door
  geen enkel document gedragen, en gemeten 2026-09-10: verifier PASS, `ref` en
  de ASP-backend allebei hun generieke doorval (`expression kind not
  implemented` / `not translated`), de Drools-lezers vergelijken een null tegen
  hun literalen en bereiken geen van beide takken (marker, geen NPE). Drie
  doorvallen en nergens een dereference — anders dan WF-095's null-lexeem, dat
  één lezer dereferencet. Vastgelegd met zijn meting in ch. 4.1 (de
  (n)/(o)-precedent), niet stilzwijgend meegenomen: de kaart besliste over
  spelling, niet over aanwezigheid.
- **De positie van `de dag`.** De verzameling is plat: `de dag` is overal
  toelaatbaar, niet alleen in een dagsoortdefinitie. Een positieregel is een
  aparte clausule met een eigen beslissing en een eigen getuige.
- **`corpus/golden/**`** — onder besluit 2 was geen drager nodig; geen golden
  is aangeraakt.

### Gemeld, niet gekaart

- **`RUNTIME-SEMANTICS.md` ch. 8 kent `de dag` niet.** De runtime-betekenis van
  `de dag` is de dagsoortevaluatie van ch. 3.7 — de dag waarop het predicaat
  van de definitie wordt toegepast — maar ch. 8 (tijdselectie) noemt alleen
  `Rekendatum` en `Rekenjaar`: ch. 8.2 schrijft dat de evaluatiedatum
  "available to expressions solely as the v0 execution-date constant" is. Dat
  is een **bestaande** kloof, niet door deze kaart geopend; hij werd zichtbaar
  doordat de ch. 4.1-rij `de dag` nu benoemt. De rij verwijst er expliciet
  naar. `spec/RUNTIME-SEMANTICS.md` is **niet aangeraakt** — dichten hoort bij
  de dagsoort-semantiekkaart (§6.10/§6.11, out of scope van deze kaart).

### Eén vondst die de kaart niet voorzag: de `hij`-producentroute

De RegelSpraak-frontend bouwt een `Constante` met de naam **`hij`** voor het
bezielde voornaamwoord (`ExpressieBouwer`), en de resolver lost hem normaal
weg: van de 38 goldens met een `bron.rgs` schrijven er **14** het woord `hij`,
en geen van hun `ir.json`-bestanden draagt ook maar één `Constante`. Hij kan
wél lekken — gemeten 2026-09-10, de bron `indien hij gelijk is aan hij` emitteert
er twee met `diagnostics: []` — maar hij lekt met het **onbekend-type-sentinel**
(`Onbekend`, "niet opgelost"), en dat wijst WF-034's typeproloog al af op
dezelfde knoop. Dat is structureel, geen steekproef: de frontend heeft precies
**drie** `Constante`-constructieplekken (`ExpressieBouwer` r. 392/394/398) en
de `hij`-plek hard-codeert `onbekend()`, dus een `hij` kan de wire nooit
goedgetypeerd bereiken:

    verifier ZONDER WF-098:
    WF-034 FAIL /resolvedRegels/0/voorwaarde/links/type: unknown-type sentinel …
    WF-034 FAIL /resolvedRegels/0/voorwaarde/rechts/type: unknown-type sentinel …
    verifier MET WF-098:
    WF-034 FAIL …/links/type: unknown-type sentinel …
    WF-098 FAIL …/links/naam: context value 'hij' is not one of the three …
    WF-034 FAIL …/rechts/type: unknown-type sentinel …
    WF-098 FAIL …/rechts/naam: context value 'hij' is not one of the three …

WF-098 snijdt dus **geen conforme producentroute** af — anders dan WF-096, dat
dat wél deed: het voegt een tweede, naam-niveau regel toe op een positie die de
verifier al afwees. Geen frontend-vervolgkaart nodig.

### Gates — alle dertien groen, gedraaid in deze sessie

    gate 1  check-self-contained       PASS
    gate 2  check-ir-schema            PASS (schema valid; 192 file(s) validate and round-trip, floor 192)
    gate 3  check-verifier             PASS (192 golden clean; 358 negatief rejected for their declared rule, floor 350)
    gate 4  check-case-schema          PASS (schema valid; 453 case file(s) validate)
    gate 5  check-oracle               PASS (ref suite 489 worked examples, floor 489; 427 cases replayed exactly, floor 427)
    gate 6  check-clingo               PASS (backend suite 86 checks, floor 83; 427 cases replay exactly through clingo)
    gate 7  check-noop                 PASS (391 cases refute a do-nothing executor; 36 pinned survivors)
    gate 8  check-rood                 PASS (26 red cases fault observably in ref and clingo, floor 26)
    gate 9  check-catala               PASS (frontend suite 201 checks, floor 201; 12 catala golden(s) regenerate byte-identically, floor 12)
    gate 10 check-regelspraak          PASS (JUnit suite green, 173 tests, floor 173; 38 rgs golden(s) re-emit byte-identically, floor 38)
    gate 11 check-toka-parity          PASS (29 PLAYED / 0 loud per-form EXCLUDE / 0 FAIL-or-ERROR)
    gate 12 check-drools               PASS (played matches the pinned 119; channel floor 47 non-PLAYED pinned by name; JUnit 244 tests, floor 239)
    gate 13 check-reftype-derivation   PASS (74 unit annotations twisted over 15 goldens, floor 74; 70 executor replays reproduce the untouched outcome, 13 PLAYED by the DRL-text branch, floor 13)

Gate 3 telt 358 negatiefs waar de kop hiervoor er 356 telde: precies de twee
nieuwe getuigen. Gates 5 en 6 bevestigen nul gedragsverandering — `ref` en
clingo bewegen niet en hun corpusreplay is ongewijzigd; gate 12 bevestigt
hetzelfde voor de derde uitvoerder (het gepinde gespeelde kanaal is
onveranderd 119) en gate 11 dat `toka/casus`' drie `de dag`-dragers
toelaatbaar zijn en onaangeraakt blijven.

### Voorgestelde CONSTRUCTS-tekst (hoofdsessie, serieel — niet aangeraakt)

Rij 25 (ContextValue) draagt de nieuwe clausule. Voorstel voor de
Landed-zin op die rij:

> **Landed (2026-09-10):** WF-098 (`IR-SPEC.md` ch. 4.1) maakt de v0-`naam`
> een GESLOTEN VERZAMELING van drie — `Rekendatum`, `Rekenjaar`, `de dag` —
> byte-exact gekruist. Dat is geen resolutie maar een opsomming: geen tabel in
> het document verklaart een contextwaarde. De v1-splitsing die deze rij
> beschrijft wordt er niet door verlegd, maar de rij krijgt er wel een derde
> naam bij: `de dag`, de gebonden dag van een dagsoortdefinitie (ch. 3.7), die
> de wire draagt en die v0 als ContextValue spelt omdat er geen knoopsoort voor
> is. **Of v1 er een eigen knoopsoort van maakt is een open punt** — deze kaart
> beslist het niet en de rij noteert het. Byte-exact is de tweede helft van de
> landing: `ref` en de ASP-backend normaliseerden, de twee Drools-lezers van
> hetzelfde veld zijn het onderling oneens (`equals` versus
> `equalsIgnoreCase`), en met de naam op de wire vastgezet is die divergentie
> onbereikbaar op een verifier-schoon document.

### Nummerverschuiving: WF-098 is vergeven

Deze kaart landt eerder dan twee latere kaarten die **WF-098** als
kandidaat-nummer noemen. Die schuiven op naar **WF-099** en verder. De
hoofdsessie corrigeert dat bij die kaarten; hier is alleen vastgelegd dat het
nummer nu bezet is.
