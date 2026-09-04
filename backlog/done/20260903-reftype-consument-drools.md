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

## Fixronde 3 (2026-09-04) — na lens 2

Lens 2 verklaarde de Java-kern bevindingloos en blokkeerde op vier
normatieve punten. Elk is eerst gereproduceerd, daarna gefixt.

**B3-1 — de boekhoudklem sleutelde op de verkeerde eenheid**
(`scripts/check-reftype-derivation.sh`, stap 6). De klem en de
verdraaitabel sleutelden op `node`. Maar §2.10's vierde uitzonderingsklasse
bestaat niet uit knoopsoorten: `GEDEELD_DOOR` en `PLUS` zijn dezelfde
knoopsoort (`Binair`), en alleen de tweede leidt af. Gevolg: een
eenheiddragende vierde-klasse-tak stond gewoon in de tabel en werd
verdraaid.

*Reproductie.* De twister uit het scripthoofd geëxtraheerd en losgelaten op
een `/tmp`-document met één eenheiddragende `Binair{op: GEDEELD_DOOR}`:

    1 2 1 0

Eén treffer — de annotatie kreeg de `Binair`-klassefactor 1e5 op zich
(`EUR` → `EUR-verdraaid5`, `factorNaarCanoniek` 1 → 100000) — en géén
klemregel. Precies de verdraaiing die niets kan bewijzen: §2.10 zegt dat
ref/clingo daar *faulten* en Drools mét de annotatie rekent, dus wat er ook
uitkomt, het harnas zou het als consumentendefect melden (verkeerde
diagnose) of er niets over zeggen (stilte waar de getuige hoort te klinken).

*Fix.* Tabel én klem sleutelen nu op de POSITIEKLASSE: `(node, op)` voor
een `Binair`, `(node, kind)` voor een `Functie`, de kale knoopsoort
daarbuiten. Afgeleid blijft twistbaar (`PLUS`/`MIN`/`VERMINDERD_MET`/`MAAL`
en de zes waarde-aggregaties); alles daarbuiten van die twee knoopsoorten
is een KLEMtreffer met een eigen, van de onbekende-knoopsoort gescheiden
melding. De klassefactor blijft per KNOOPSOORT — een herschaalgrens ligt
tussen twee knoopsoorten, nooit tussen twee operatoren van één soort — dus
de verdraaiverzameling die het corpus pint beweegt niet.

*Demonstratie, beide kanten.* (a) Een `/tmp`-corpus met twee echte goldens
plus de vierde-klasse-getuige, gedraaid door een scriptkopie die alleen in
de corpuswortel verschilt (twee regels; `diff` in de sessie):

    FAIL: a unit-bearing annotation sits at a node kind whose result unit
          v0 does not derive — the §2.10 fourth-class witness has arrived.
          …
       1   vierde-klasse-getuige Binair/GEDEELD_DOOR

(b) Het echte corpus: `PASS (46 unit annotations twisted over 10 goldens;
36 executor replays reproduce the untouched outcome; 0 pinned survivors)`.

*De nieuwe boekhoudregel*, eerlijk gerapporteerd — de 75-telling verschuift
NIET, want het corpus draagt vandaag geen enkele vierde-klasse-positie
(nagemeten per positieklasse: 35 `AttribuutRef`, 7 `Binair/PLUS`, 4
`Binair/MAAL`, 2 `Binair/MIN`, 3 `BinderRef`, 1 `Functie/SOM`, 2 `Let`, 15
`Literal`, 5 `Functie/TIJDSDUUR`, 1 `Functie/ABSOLUTE_TIJDSDUUR`). De regel
telt de klasse voortaan apart, met een eigen kolom die op nul staat:

    75 unit-bearing annotations = 46 twistable + 23 exempt (the annotation
    IS the source) + 6 without a convertible sister + 0 at a §2.10
    fourth-class branch (v0 derives no result unit) + 0 outside the twist
    table

(23 exempt = 15 `Literal` + 6 tijdsduur-Functies + 2 WF-035-duuroperanden.)

**B3-2 — de marker-zin was onwaar voor de gate-tak**
(`EffectieveEenheid`-javadoc). De javadoc claimde dat *elke*
§4-herschaalplek in *beide* backends op `convertibleNaar` /
`exacteHerschaalFactoren` poortwacht en bij een opaak product een eerlijke
marker geeft. Voor de tak die de gates draaien is dat niet zo:
`ExecModelTranspiler.restatement` (~:4633) geeft bij niet-converteerbaar de
IDENTITEIT `{1, 1}`, en `herschaalWaarde` valt via `valueFactor`'s null
door op de ongewijzigde waarde. `CaseRunner` bouwt uitsluitend de
exec-model-tak (geen enkele `DrlTranspiler`-import), dus dát is de weg van
elke replay.

*Gemeten paar* (hoort hier, niet in de javadoc). Een `/tmp`-document met
`oppervlakte (m) = lengte (m) maal breedte (m)`, invoer 300 m × 300 m:

    ref     EVALUATION FAULT: incompatible units: m vs m*m
    clingo  TRANSLATION FAULT: a product of two unit-carrying operands
            under a product is not translated
    Drools  PLAYED 1   (d.w.z. 90000,00 — de gepinde verwachting gehaald)

*Afwijking van de briefing, eerlijk.* De briefing zei "de DRL-tak markeert
luid". Nagemeten op `Eenheid` klopt dat niet voor deze vorm: voor `m·m`
tegen `m` is `convertibleNaar` false, `vereistOmrekening` false,
`omrekenfactorNaar` 1 en `exacteHerschaalFactoren` null. Elke geciteerde
markerplek in `DrlTranspiler` zit ACHTER een van die twee poorten
(`herschaalExact`:3808, `herschaalConditie`:3856 → `return expr`, de
identiteit, één regel bóven hun markertak; de §8.2-plekken 2705/2759 achter
`vereistOmrekening`), dus een opaak product bereikt de markerregel nooit.
De scheiding loopt niet tussen de backends maar tussen de SOORTEN plek:
- een plek die op `convertibleNaar` WEIGERT — de §4.1/§5.1.4 temporele
  herschalingen (`schaalbaarNaarDoel`/`schaalTijd` in DRL, de fold- en
  tijdlijnwachten in exec) — krijgt false en markert/skipt het hele
  construct. Symmetrisch in beide takken;
- een plek die alleen een FACTOR rekent — de scalaire schrijfgrens, de
  additieve operandgrens, de §5.1-Tabel-32-vergelijking — krijgt de
  identiteit en de herschaling wordt een NO-OP. Geen marker, aan geen van
  beide kanten.

De javadoc zegt dat nu zo, met de meting erbij: conservatief (geen
uitgevonden factor, dus geen leugen-gestuurde magnitude — de waarde blijft
onherschaald staan), maar een STILLE ref/Drools-divergentie, op een vorm
die frontend-`Typering`, `ref` en clingo alle drie weigeren en die het
corpus niet draagt.

**B3-3 — de MAAL-zin in §2.10 was een tweeledige disjunctie** (cancel |
opaak product). De vierde uitkomst ontbrak: de onspelbare tariefsfactor.
Gemeten met een `/tmp`-document `uitkomst (€) = tarief (€/jr) maal doorzet
(kg/jr)`, invoer 10 en 3:

    ref     EVALUATION FAULT: unit product not modelled: EUR * kg
    clingo  TRANSLATION FAULT: non-identical units under the write to
            "uitkomst" are not translated
    Drools  PLAYED 1   (30,00 — onherschaald)

En de noemer-val, tegen de gebouwde jar gemeten: `Eenheid.product` houdt de
tellerfactoren en laat de noemer vallen, dus `€/jr × kg` en `€/jr × kg/jr`
worden **beide** `EUR·kg` (`noemer=null`, `canoniek=null`, `equals` true).
§2.10 draagt die vierde uitkomst nu, met de frontend-`Typering`-fout
("een producteenheid met een noemer kan het Eenheid-model niet uitdrukken")
erbij en met de eerlijke kwalificatie: het noemerloze opake product is de
conservatieve, niet-herschaalbare uitkomst — geen bewering dat het product
spelbaar is — en de drie andere consumenten weigeren.

**B3-4 — ont-"never"-d.** De §2.10-sectietitel ("a type annotation is never
the source") en de openingsjavadoc van `EffectieveEenheid` ("never from the
node's own wire annotation") werden vier alinea's later door hun eigen
uitzonderingslijst weersproken. Titel is nu "**where** a type annotation is
not the source"; de openingsalinea's van beide dragen de gescopete
waarheid: bij een REFERENTIE- of COMPOSIET-positie is de annotatie niet de
bron, en de uitzonderingsklassen zijn *benoemde posities* — een
scopegrens, geen ontsnappingsluik. De vierde-klasse-alinea zegt nu ook
hardop dat zij een verzameling POSITIES is en geen verzameling
knoopsoorten, en waarom de klem daarom op `(node, op)` sleutelt.

**B3-5 — de dekkingsalinea, en de S19-kruisverwijzing.** Het scripthoofd
verzweeg hoeveel van de Drools-tak een groene regel dekt. Gemeten met een
tijdelijke `StackWalker`-sonde op `ExecModelTranspiler.effType` (daarna
teruggedraaid): op het ONVERDRAAIDE corpus wijkt de afleiding NERGENS van
de annotatie af (nul divergenties — precies wat WF-091 belooft); op de
liegende kopieën overrulet zij de annotatie op exact VIER call-sites, en
die vier zijn het hele bereik van wat dit harnas de exec-tak oplegt:

| call-site | wat | treffers |
|---|---|---|
| `regel`:635 | de SCHRIJFGRENS (`wf.herschaald(effType(waarde), effType(doel))`) | 18 |
| `scalarWaarde`:3748 | de ADDITIEVE OPERANDGRENS (rhs in de lhs-eenheid) | 13 |
| `regel`:546 | de AGGREGATIE-HERSCHALING (`aggVan`/`aggNaar`) | 2 |
| `leafPredicate`:3187 | de ATTR-vs-ATTR-VERGELIJKING (§5.1 Tabel 32) | 2 |

Elke andere `effType`-aanroep is op dit corpus een doorgeefluik: gestéld,
niet gemeten. En `DrlTranspiler` wordt door dit harnas alleen GECOMPILEERD
— `CaseRunner` bouwt geen DRL-pakket — dus zijn eenheidsgedrag, inclusief
de "OPAQUE product … honest marker"-plekken, leunt op de JUnit-getuigen
(gate 12's plicht), niet op een groene regel hier. Beide alinea's staan nu
in het scripthoofd naast de bestaande no-op-twistklassen-alinea.

Tot slot de kruisverwijzing: §2.10 wees de `Fold`/`Match`-schaallezing van
de ASP-vertaler naar `ASP-TRANSLATION.md` **S19**. Nagekeken: S19 is "What
does not translate — the complete loud inventory"; de schaalcodering staat
in **S2** (`val/3` geschaald) en **S4** (de `expr_term`-triple ⟨body, term,
scale⟩). Gecorrigeerd, met de reden erbij.

**Stand na fixronde 3.** Harnas groen met ongewijzigde boekhouding (46
verdraaid over 10 goldens, 36 replays, 0 survivors; 75 = 46 + 23 + 6 + 0 +
0). JUnit **198**, nul overgeslagen, nul fouten. Gates 1, 3, 6 en 12 groen;
gate 12 ongewijzigd op elk getal.

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

## Result (2026-09-04)

Geland in een bouwgolf en drie fixronden onder twee lenzen. (1) Het
verdraai-harnas `scripts/check-reftype-derivation.sh`, rood-eerst: 46
verdraaide eenheidsannotaties over 10 goldens gaven negen
Drools-divergenties (waaronder de foldwortel 3,75 → 3.750.000) terwijl
ref en clingo onverstoorbaar bleven — dat faalrapport was de werklijst.
(2) `EffectieveEenheid` (het Java-evenbeeld van de ASP-eff_unit):
declaratie voor referentieposities, ch.-4.10-scope met WF-064-shadowing
voor binders (inclusief de levend gemaakte eenheidloze-binding), de
ch.-2.6-afleiding voor composieten met MAAL als cancel-of-opaak-product
(spiegel van de frontend-Typering; de tariefsfactor blijft een
noemerloos opaak product waar frontend, ref en clingo weigeren —
eerlijk in 2.10 genoteerd), doorgevoerd op ~45 plekken in beide
transpilertakken; het defaults-bronpad incluis. (3) De consumentenclausule
RUNTIME-SEMANTICS 2.10 (voorstel): referentieposities lezen de
declaratie, composietposities het effectieve type, met de vier benoemde
uitzonderingsklassen en de gesplitste soort/precisie-zin. (4) Twee
klemmen in het harnas: de boekhoudklem op (knoop, operator/soort) — de
eerste eenheiddragende vierde-klasse-positie (bijv. een deling) is een
gate-FAIL, geen stille twist — en de vierdelige
survivor-verouderingsklem; boekhouding 75 = 46 twistbaar + 23
vrijgesteld + 6 zonder converteerbaar zusje + 0 vierde-klasse + 0
ongedekt. (5) Gate 13 geregistreerd (CLAUDE.md + CI-leg,
maintainerbesluit 2026-09-04); CONSTRUCTS rij 1 bijgesteld. JUnit 178 →
198 (EffectieveEenheidWitnessTest, mutatiegemeten), gate 12 op elk getal
ongewijzigd (played 105 / 43 / 1). Lens-vangsten, verwerkt: de
MAAL-links-lezing die de opaak-productbescherming sloopte; de dode
containsKey-bewaking; de klem die de §2.10-vierde-klasse-getuige niet
kon melden; de marker-zin die per pleksoort bleek te lopen, niet per
backend (het exec-pad maakt een niet-converteerbare herschaling een
no-op — gemeten ref-fault tegen Drools 90000 op een vorm die het corpus
niet draagt en drie consumenten weigeren). Eerlijk open: de
DRL-teksttak leunt op de JUnit-getuigen (het harnas meet de
exec-model-tak; vier call-sites wijken ooit af), precisie/soort op
composieten blijven de WF-091-notitie-4-deferral, en de stille
factor-één-terugval in de frontend-eenheidslogica is
frontendkaart-materiaal. Herpin bij de gebundelde landing.
