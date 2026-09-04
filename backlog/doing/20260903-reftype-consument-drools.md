# Referentietypen: de consumentenclausule en de Drools-werklijst

Goal: de andere helft van de WF-091-kaart. De verifier eist nu dat de
kopie op referentieposities de declaratie herhaalt; deze kaart legt de
consumentenkant normatief vast en laat de Drools-backend haar halen.
Gemeten stand (meetfase + lenzen, 2026-09-03): de Drools-schrijf- en
operandgrenzen lezen wire-annotaties (`ExecModelTranspiler.java:618`,
`:3727` en verwanten, plus de DRL-tweelingen), terwijl het defaults-pad
in dezelfde backend al de declaratie leest (`:6567`); een eenheidsleugen
op een fold-RESULTAATtype als wortelwaarde is verifier-schoon (het
resultaattype is een afgeleide, WF-091 kruist het bewust niet) en geeft
ref 3,75 tegen 3.750,00 in het Drools-pad; en de eenheidslogica valt
bij een naam-mismatch stil terug op factor één
(`Eenheid.tellerConvertibel`/`tellerFactorNaar`) — een silent fallback
die eigen aandacht verdient.

Aanpak, in volgorde: (1) een verdraai-harnas als meetscript (het
no-op-patroon: verdraai referentie- en composiet-annotaties buiten de
verifier om en eis drie-executor-gelijkheid, met een reden-dragende
survivorlijst) — het faalrapport is de werklijst; (2) de clausule in
RUNTIME-SEMANTICS (voorstel): een consument leidt eenheid, soort en
precisie op referentieposities uit de declaratie af; afbakening
expliciet — het Literal-lexeem, fold-/match-resultaattypes en de
tijdseenheidroutering blijven legitieme annotatie-lezers; (3) de
werklijst afwerken in `backends/drools` (declaratie-lookups, of een
effectieve-eenheidafleiding naar het model van de ASP-vertaler); (4) de
gate erbij zodra hij groen is — niet eerder (monotonie). De
CONSTRUCTS-delta noteert de hoofdsessie.

Acceptance:
- Het harnas bestaat, zijn faalrapport vóór de fixes is gedocumenteerd
  (rood-eerst op executorniveau).
- De RUNTIME-SEMANTICS-clausule als voorstel-diff (protected path).
- De werklijst afgewerkt met ref-gemeten witnesses in nieuwe
  testbestanden; het 3,75/3.750,00-paar divergeert niet meer.
- Alle twaalf gates groen; de nieuwe gate alleen toegevoegd als hij
  groen is; herpin.

May touch: `spec/RUNTIME-SEMANTICS.md` (voorstel),
`backends/drools/src/**`, `scripts/` (voorstel, alleen de nieuwe gate),
`backlog/`.

Out of scope: de wire-vormen (v1), de verifier (WF-091 staat),
frontends, bestaande corpuspins.

## Bouwnotitie (2026-09-04)

### Het harnas en zijn rood-eerst-rapport

`scripts/check-reftype-derivation.sh` (gecommit vóór elke fix, `f1fdedf`).
Het verdraait elke EENHEID-annotatie op referentie- en composietposities
naar een converteerbaar zusje — zelfde canoniek, afkorting met een
achtervoegsel, `factorNaarCanoniek` maal 10^klasse — en laat ref, clingo en
Drools de ORIGINELE case-verwachtingen spelen op die liegende kopie. Elke
declaratie blijft byte-identiek; dat is precies wat het afgeleide antwoord
het juiste maakt. Elke positieklasse krijgt haar EIGEN factor-exponent
(AttribuutRef 10 … Begrensd 10⁹) zodat twee annotaties elkaar aan een
herschaalgrens niet uitdoven. Drools wordt tegen zijn EIGEN onverdraaide
statusregel gelegd, niet tegen `PLAYED`: een golden die gate 12 per vorm
weigert of in haar waardedivergentielijst draagt, moet onder de verdraaiing
precies even weigerend blijven — dit harnas herkauwt gate 12 niet.

**Eerste uitvoer, het faalrapport dat de werklijst IS** (46 verdraaiingen
over 10 goldens; 36 executor-replays — 13 case-bestanden maal ref+clingo,
plus één Drools-statusregel per golden. Het getal 402 hieronder in de
precisie-alinea is een ánder getal: de corpusbrede casustelling waarmee de
precisieleugen is gemeten, niet de omvang van dit harnas):

| golden | ref | clingo | Drools onverdraaid → verdraaid |
|---|---|---|---|
| jutterswet-lichterloon | ok | ok | PLAYED 1 → FAIL 360 → 252000000; 100 → 10000000 |
| jutterswet-meldloon | ok | ok | PLAYED 2 → FAIL 105 → 1005000,0; 92,50 → 10049987500,00 |
| jutterswet-sleepbaan | ok | ok | PLAYED 1 → FAIL 2800 → 55000000,0 |
| jutterswet-sleeprest | ok | ok | PLAYED 2 → FAIL 44,00 → 440000,00; 40,00 → 400000,00 |
| jutterswet-stukopbrengst | ok | ok | PLAYED 1 → FAIL 190 → 15000400000; 112 → 10000120000 |
| jutterswet-vergunningsduur | ok | ok | PLAYED 1 → FAIL 27 → 540000,0 |
| jutterswet-vondstrecht | ok | ok | PLAYED 1 → FAIL 42 → 1203000000 |
| jutterswet-vondsttelling | ok | ok | PLAYED 1 → FAIL **3,75 → 3750000,00** |
| synth-grensrand | ok | ok | PLAYED 2 → FAIL 50 → 500000; 30 → 300000 |
| jutterswet-vondsttarief | ok | ok | EXCLUDE → EXCLUDE (ongewijzigd) |

Gegroepeerd naar de plek die de annotatie las: **(a)** de schrijfgrens
(`regel`, `herschaald(waarde.type(), doel.type())`) — alle negen; **(b)** de
additieve operandgrens (`binairWaarde`) — sleepbaan, sleeprest, meldloon,
stukopbrengst, synth-grensrand, vergunningsduur; **(c)** het Let-cluster
(drageranotatie + binderlezing) — lichterloon, vondstrecht; **(d)** het
aggregatie-RESULTAATtype (§4.6 Eenheid1) — vondsttelling, exact de
3,75/3.750,00-klasse die de WF-091-kaart openliet. ref en clingo staan
nergens rood: beide leiden al af (`world.ml` collapse naar de declaratie;
`translate.ml` `eff_unit`).

### De werklijst — per plek de oude lezing → de nieuwe bron

De afleiding staat in ÉÉN nieuwe klasse,
`backends/drools/.../transpiler/kern/EffectieveEenheid.java`: declaratie
voor `AttribuutRef` / `ParameterRef` / `DimensieCel`, ch.-4.10-scope voor
`BinderRef` (een identiteitstabel over het `letVormen`-zijkanaal, met
WF-064-shadowing), en de ch.-2.6-afleiding voor `Binair` / `Let` /
`Afgerond` / `Begrensd` en de zes waarde-aggregaties — het Java-evenbeeld
van clingo's `eff_unit`. Zij leidt alleen de EENHEID af en ent die op de
eigen annotatie: soort leest deze backend als routeringspredicaat (WF-078 /
WF-091 kruisen dat in de verifier) en `Datatype.decimalen` leest hij
nergens — alleen `Afgerond.decimalen`, een knoopveld.

| plek | oude lezing | nieuwe bron |
|---|---|---|
| `ExecModelTranspiler.regel` schrijfgrens (dimAgg, tijdTotaal, som/agg, gemengd, scalair) | `r.waarde().type()`, `doel.type()` | `effType(r.waarde())`, `effType(doel)` |
| `binairWaarde` additieve operandgrens | `b.rechts().type()`, `b.links().type()` | `effType(...)` |
| `Begrensd` waarde- én conditietak (beide backends) | `bg.basis()/minimum()/maximum().type()` | `effType(...)` |
| `concatenatieFold` (Eenheid1, beide backends) | `argumenten.get(0).type()`, `arg.type()` | `effType(...)` |
| dertien `restatement(...)`-plekken in de conditiepaden | `la/ra/lit/v.links/v.rechts .type()` | `effType(...)` |
| `tijdreeksSomAlleSet`, `temporeleWaarde`, `temporeelRekenOperand` | `a.type()`, `ba.type()`, `bin.links().type()` | `effType(...)` |
| `vergelijkingClip` + de twee §8.2-grenzen (beide backends) | `tijd/ander.type().eenheidOf()` | `eff.eenheidVan(...)` |
| `parityOnveilig` / `grensOnveilig` | annotatie (en `static`) | `effType(...)`, nu instantiemethoden |
| `DrlTranspiler` schrijfgrens, `binairTerm`, drie `vergelijkingsOperanden`, `temporeelBasis`, de temporele som | `.type()` | `effType(...)` |
| defaults-consequentie (`:6590`) | DOEL las de declaratie (`onderwerp.attribuut`), BRON las `r.consequentie().type()` | `effType(r.consequentie())` (fixronde) |

### Wat een uitzondering bleef, en waarom

Drie plekken houden hun annotatie, elk met de reden ter plaatse in de code
en in het scripthoofd:

1. **het `Literal`-lexeem** — het type is de drager van de waarde zelf
   (`"3,5 EUR"`), kopie van geen enkele declaratie; alle drie de executors
   lezen het, en horen dat te doen;
2. **de duur-operand van datum ± duur** (WF-035) — de kalendereenheid die
   de rekenkunde routeert is dat statische type, en ref routeert op
   hetzelfde. Afleiden zou de twee uit elkaar trekken;
3. **de eigen eenheid van een `tijdsduur`** (WF-038) — die eenheid IS de
   operatorparameter ("in jaren"), geen afgeleid resultaattype.

Twee lezingen zijn opgetekend maar niet gezegend: de ASP-vertaler haalt
zijn integerschaal uit de eigen `decimalen` van een `Fold` en een `Match`
(S19). Zolang dat een coderingsdetail is mag het; zodra daar een
eenheiddragende getuige komt vallen ze onder clausule 2.

De **survivorlijst van het harnas is leeg** — gemeten, geen enkele executor
heeft een gedocumenteerde uitzondering nodig.

### Bewust NIET verdraaid (en waarom dat een meting is, geen aanname)

- **soort**: een soortleugen is een ander document — ref routeert zijn ch.
  1.2d-leegtegelijkheid en zijn datumrekenkunde op het statische soort, dus
  verdraaien zou ref's routering meten in plaats van een declaratielezing.
  WF-078/WF-091 kruisen soort in de verifier.
- **precisie**: gemeten — de ASP-vertaler schaalt zijn integercodering op
  de geannoteerde `decimalen` (`value_scale`), dus een precisieleugen laat
  clingo overlopen of weigeren in **21 van de 402** cases (bereikoverschrijding,
  de `eerste-paasdag`-heelheidscheck, een gesloten-termroute). En het zou
  hier niets meten: geen van beide Drools-takken leest ooit
  `Datatype.decimalen` of `Datatype.geheel`.

Daarmee is de eenheid zowel de enige facet waar alle drie de consumenten
onverschillig voor kunnen zijn als de enige die deze backend leest — 46 van
de ~1.180 numerieke annotaties in de goldens dragen er een. Dat is het hele
bereik dat het corpus vandaag kan uitdrukken; het harnas zegt dat hardop in
zijn PASS-regel.

### JUnit-stand

178 → **195** (na de fixronde **198**), nul overgeslagen. Nieuw testbestand
`EffectieveEenheidWitnessTest` (17 getuigen, na de fixronde 20) pint wat de corpusreplay niet
kan zien: de declaratielezing per referentievorm, de ch.-2.6-afleiding per
composietvorm, de binderlezing diep in een body, WF-064-shadowing, de drie
uitzonderingen, en dat een KLOPPENDE annotatie hetzelfde object teruggeeft
(de no-op-route die een verifier-schoon document neemt). Mutatiegemeten:
met `van()` teruggezet op de annotatie vallen precies 10 van de 17 om — de
7 die overeind blijven zijn de uitzonderingen en de terugvallen.

### Gate 12 is niet verschoven

`played 105`, `auto-excluded 43`, `value-divergence 1` — identiek aan de
stand vóór deze kaart (nagemeten: de volledige onverdraaide sweep over alle
150 goldens vóór de fix geeft dezelfde 43 weigeringen in dezelfde vormen en
dezelfde ene FAIL). De kustwacht-divergentie is niet opgelost en niet
verplaatst; de reden-dragende lijst in `check-drools.sh` blijft
ongewijzigd, en haar verouderingswacht bevestigt dat.

## Fixronde (2026-09-04) — na lens 1

Vijf blokkerende en zes kleinere punten. Elk blokkerend punt is eerst
gereproduceerd (rood), daarna gefixt.

**B2-1 — de MAAL-afleiding.** `eenheid()` behandelde `MAAL` als `PLUS`:
links, anders rechts. Fout tegen ch. 2.6 én het sloopte de enige
bescherming die deze backend tegen een tweeledig product had — de
OPAAKHEID van `Eenheid.product` (vier `DrlTranspiler`-plekken rond
`:2700`/`:3808`/`:3849`/`:3855` leunen erop: "an OPAQUE product unit stays
an honest marker"). Reproductie, twee nieuwe getuigen rood:

```
€/jr maal jr  expected: <Eenheid[afkorting=EUR, …, noemer=null]>
              but was:  <Eenheid[afkorting=EUR, …, noemer=Eenheid[jr…]]>
€ maal kg     expected: <Eenheid[afkorting=EUR·kg, canoniek=null, …]>
              but was:  <Eenheid[afkorting=EUR, canoniek=EUR, factor=1, …]>
```

Fix: bij twee eenheiddragende operanden de frontend-`Typering.
samengesteldeEenheid` spiegelen — de cancel-clausule (beide richtingen)
naar `el.teller()`, anders `Eenheid.product(el, er)`; één eenheiddragende
operand blijft doorgeven. De onuitspreekbare vorm die de frontend luid
weigert (een tariefsfactor) landt hier óók op het opake product: dat
weigert te herschalen in plaats van een factor te verzinnen. Getuigen:
`een_maal_met_twee_eenheden_valt_weg_tegen_zijn_noemer` en
`een_maal_van_twee_losse_eenheden_geeft_het_opake_product`, die het opake
gedrag zelf pint (geen canoniek, geen exacte herschaalfactoren naar €,
niet convertibel naar € of kg, wél naar een identiek product).

**B2-2 — de eenheidloze binder.** `metBinder` deed `binnen.remove(k)` bij
een null-eenheid, waardoor de `containsKey`-bewaking in `letEenheid`
("bound to a unitless value") dode code was: zo'n lezing viel terug op de
annotatie, en een gelijknamige BUITENSTE binder werd bovendien
ont-overschaduwd (WF-064). Reproductie (nieuwe getuige rood): `de binding
is eenheidloos; de annotatie (t) is haar leugen ==> expected: <null> but
was: <Eenheid[afkorting=t, …]>`. Fix: `binnen.put(k, null)` (HashMap
draagt null), en `letEenheid` vraagt bij BEIDE tabellen `containsKey` in
plaats van `get() != null`, zodat "gebonden aan eenheidloos" en
"ongebonden" gescheiden blijven. `noteer` legde het onderscheid al goed
vast; het lezerspad wist er alleen niets mee te doen.

**B2-3 — de faalklem voor nieuwe knoopsoorten** (`check-…-derivation.sh`
stap 6). De twister boekt nu ELKE eenheiddragende annotatie: verdraaid,
vrijgesteld (de reden-dragende uitzonderingsset: `Literal`, de
duur-operand van datum±duur, `TIJDSDUUR`/`ABSOLUTE_TIJDSDUUR`), zonder
converteerbaar zusje, of ONGEDEKT — een knoopsoort buiten de tabel én
buiten de uitzonderingen. Ongedekt > 0 → FAIL met knoopsoort + golden.
Gemeten stand, en het harnas zegt ze nu hardop in zijn PASS-regel:

```
corpus-wide accounting: 75 unit-bearing annotations = 46 twistable
  + 23 exempt (the annotation IS the source) + 6 without a convertible
  sister; 0 outside the twist table
```

(75 = 46 verdraaid + 8 eigen `Literal` + 7 `Literal`-duuroperanden + 2
`AttribuutRef`-duuroperanden + 6 `tijdsduur` + 6 zonder zusje — dat
laatste zijn eenheden zonder canoniek/factor, waarvoor geen
CONVERTEERBAAR zusje bestaat; een niet-converteerbare leugen zou
weigergedrag meten in plaats van een declaratielezing.) De klem is dus
vandaag gratis. Demonstratie dat hij bijt — `Binair` tijdelijk uit de
tabel gehaald, exit 1:

```
FAIL: a unit-bearing annotation sits at a node kind the twist table does
      not carry, and that the exception set does not exempt …
   3 jutterswet-lichterloon Binair
   2 jutterswet-meldloon Binair      (… 8 goldens, 13 voorkomens)
```

**B2-4 — de survivorklem afgemaakt.** Het script controleerde alleen het
BESTAAN van de survivor-golden. Nu houdt het per lijstpositie bij of hij
daadwerkelijk RAAKTE (`$RAKERS`, één regel per divergentie) en faalt op
drie manieren, het check-noop-patroon compleet: onbekende executor,
verdwenen golden, en een entry die niet meer divergeert. Demonstratie met
drie verzonnen entries, exit 1:

```
FAIL: stale survivor entry:
  jutterswet-vondsttelling|drools — no longer diverges under the twist; drop the entry
  verdwenen-golden|ref — no such golden
  jutterswet-meldloon|drool — unknown executor (expected ref, clingo or drools)
```

De POSITIEVE helft is apart gemeten (zie de mutatiemeting hieronder): met
een mutant die de annotatie weer leest, en `jutterswet-vondsttelling|
drools` als enige gepinde entry, meldt het harnas acht divergenties en
accepteert die ene — geen verouderingsklacht.

**B2-5 — §2.10 aan de code** (`spec/RUNTIME-SEMANTICS.md`, voorstel).
(a) "never the node's own annotation" klopte niet op composietposities:
de default-tak laat `GEDEELD_DOOR`, `TOT_DE_MACHT`, `PERCENTAGE_VAN`,
`WORTEL`, `ABSOLUTE_WAARDE` en de datum-extractors op de annotatie staan
(status quo van vóór de kaart). Dat staat er nu als de VIERDE
uitzonderingsklasse, expliciet zwakker geformuleerd: niet "de annotatie
is de bron" maar "v0 heeft daar geen regel" — met de reden (clingo's
`eff_unit` geeft er `None` en zijn hekken `unit_free_ok` /
`units_identical_ok` FAULTEN; deze backend heeft dat hek niet en rekent
met de annotatie) en de gemeten bereikbaarheid (nul eenheiddragende
voorkomens; B2-3's klem meldt de eerste getuige). (b) De soort/precisie-
zin is gesplitst: clausule 1 (referentieposities) wordt door WF-091's
identiteitsmotor over ALLE facetten gekruist; clausule 2
(composietposities) is voor de EENHEID afgeleid en machinaal gecontroleerd,
terwijl soort en precisie op een AFGELEID type door niets gekruist worden
— de open deferral van WF-091's eerlijkheidsnotitie 4, met verwijzing.
Clausule 2 kreeg er bovendien de MAAL-regel (cancel + opaak product) en
de binderlezing-met-eenheidloze-binding bij, zodat B2-1 en B2-2 ook
normatief staan en niet alleen in Java.

**B2-6 — de vier gemiste annotatielezingen** (alle gemeten onbereikbaar
vandaag, JUnit en harnas identiek groen): `DrlTranspiler.
tijdreeksSomAlleSet` (`schaalbaarNaarDoel` + `schaalTijd`) en
`temporeelRekenOperand` — de DRL-tweelingen van al omgezette
exec-plekken; het temporele DOEL-type (`td.type()` in DRL,
`doel.type()` op de twee exec-plekken — §2.10 noemt de doel expliciet
referentiepositie); `ExecModelTranspiler.lowerVal` (rechts/links aan de
additieve grens); en de objectcreatie-`TkLower` (`t.waarde().type()`, twee
takken). De soort-lezingen ernaast (`isNumeriek`, `isDatum`) blijven staan:
routeringspredicaten, geen herschaling.

**B2-7 — het defaults-pad.** De bron las `r.consequentie().type()` → nu
`effType(...)`. De bouwnotitie-zin "las al de declaratie" gold alleen het
DOEL (`onderwerp.attribuut(doel)`); de tabelregel is gecorrigeerd.

**B2-8 — bouwnotitie eerlijk.** Het 402-getal is gecorrigeerd (36
executor-replays; 402 is de corpusbrede casustelling van de
precisiemeting). Het scripthoofd zegt nu twee dingen hardop die het
verzweeg: (1) vier verdraaiklassen zijn voor de DROOLS-tak per constructie
een no-op — `ParameterRef`, `DimensieCel`, `Afgerond` en `Begrensd`
LEIDEN hun `type()` in de Java-IR-records af (uit de parameterdeclaratie,
uit de binnenreferentie, uit de basis) en dragen dus helemaal geen
wire-annotatie; de graft bereikt die tak daar niet, en hun Drools-dekking
leunt volledig op de `EffectieveEenheidWitnessTest`-getuigen (ref en
clingo worden er wél door geprobed); (2) het harnas meet
ONVERSCHILLIGHEID, niet noodzakelijkheid — het kan een consument die
afleidt niet onderscheiden van een die eenheden helemaal negeert. Dat mag,
omdat de noodzakelijkheid elders gepind staat: gates 5/6 houden ref en
clingo aan de absolute handgeschreven verwachtingen en gate 12 houdt Drools
aan diezelfde waarden, dus een eenheden-negerende backend is daar rood op
het ONVERDRAAIDE corpus. Onverschilligheid is de helft die geen andere
gate kan zien, juist omdat een liegend document er geen mag halen.

**Mutatiemeting van het harnas (de tanden).** `van()` tijdelijk
teruggezet op `e.type()` (de stand van vóór de kaart) → het harnas meldt
exact de negen bekende divergenties terug, inclusief `PLAYED 1 → FAIL
… verwacht 3,75, werkelijk 3750000,00`-klasse en de acht andere; met één
gepinde survivor blijven er acht FAILs over en klaagt de
verouderingswacht niet. Bron hersteld, harnas weer PASS.

**Stand na de fixronde.** Harnas: `PASS (46 unit annotations twisted over
10 goldens; 36 executor replays reproduce the untouched outcome; 0 pinned
survivors)` plus de boekhoudregel. JUnit: **198**, nul overgeslagen (195 +
3 getuigen). Gate 12: `played 105 / auto-excluded 43 / value-divergence 1`
— ongewijzigd op elk getal.

## Result (2026-09-04)

Geland: (1) `scripts/check-reftype-derivation.sh` — het verdraai-harnas,
rood-eerst gecommit mét zijn faalrapport (negen Drools-divergenties, ref en
clingo onverschillig), nu groen met een LEGE survivorlijst; (2)
`transpiler/kern/EffectieveEenheid` plus ~40 call-sites in beide
Drools-takken, zodat elke §4-herschaling zijn eenheid uit de declaratie of
de ch.-2.6-afleiding haalt in plaats van uit de wire-herhaling — het
3,75/3.750,00-paar divergeert niet meer; (3) RUNTIME-SEMANTICS **§2.10 "The
consumer clause"** als voorstel (protected path), met de drie
reden-dragende uitzonderingen, de VIERDE (zwakkere) klasse voor de
knoopsoorten die v0 niet afleidt, de per-clausule gesplitste soort/precisie-
stand, de twee opgetekende ASP-schaallezingen, en het harnas benoemd als
afdwingend artefact; (4) 20 nieuwe JUnit-getuigen (178 → **198**). Alle
twaalf gates groen; gate 12 ongewijzigd op elk getal (`played 105 /
auto-excluded 43 / value-divergence 1`, JUnit 198). De fixronde hierboven
hoort bij deze landing: zij bracht de MAAL-afleiding, de eenheidloze
binder, twee klemmen in het harnas en de §2.10-correcties.

Voor de hoofdsessie: de gate-registratie (nummer 13) en de
CONSTRUCTS-delta. Niet aangeraakt: `ref/`, `backends/clingo/`,
`spec/verifier/`, `frontends/`, `corpus/**`, bestaande scripts en
testbestanden, `CLAUDE.md`, `.github/`.

Open, met reden: het bereik van het harnas is de eenheidsfacet over tien
goldens — de andere twee facetten zijn hierboven gemeten weggelaten, en de
`Fold`/`Match`-schaallezing van de ASP-vertaler wacht op een
eenheiddragende getuige. Even open, en nu expliciet in §2.10: soort en
precisie op een AFGELEID type worden door niets gekruist (WF-091's
eerlijkheidsnotitie 4), en de vierde uitzonderingsklasse — de knoopsoorten
zonder v0-afleidingsregel — is vandaag leeg maar niet leeg van betekenis;
B2-3's klem meldt haar eerste getuige.
