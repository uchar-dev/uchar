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

**Eerste uitvoer, het faalrapport dat de werklijst IS** (46 verdraaiingen,
10 goldens, 402 ref- + 402 clingo-replays):

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
| defaults-consequentie (`:6567`) | *las de declaratie al* | ongewijzigd |

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

178 → **195**, nul overgeslagen. Nieuw testbestand
`EffectieveEenheidWitnessTest` (17 getuigen) pint wat de corpusreplay niet
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

## Result (2026-09-04)

Geland: (1) `scripts/check-reftype-derivation.sh` — het verdraai-harnas,
rood-eerst gecommit mét zijn faalrapport (negen Drools-divergenties, ref en
clingo onverschillig), nu groen met een LEGE survivorlijst; (2)
`transpiler/kern/EffectieveEenheid` plus ~40 call-sites in beide
Drools-takken, zodat elke §4-herschaling zijn eenheid uit de declaratie of
de ch.-2.6-afleiding haalt in plaats van uit de wire-herhaling — het
3,75/3.750,00-paar divergeert niet meer; (3) RUNTIME-SEMANTICS **§2.10 "The
consumer clause"** als voorstel (protected path), met de drie
reden-dragende uitzonderingen, de twee opgetekende ASP-schaallezingen, en
het harnas benoemd als afdwingend artefact; (4) 17 nieuwe
JUnit-getuigen (178 → 195). Alle twaalf gates groen; gate 12 ongewijzigd op
elk getal.

Voor de hoofdsessie: de gate-registratie (nummer 13) en de
CONSTRUCTS-delta. Niet aangeraakt: `ref/`, `backends/clingo/`,
`spec/verifier/`, `frontends/`, `corpus/**`, bestaande scripts en
testbestanden, `CLAUDE.md`, `.github/`.

Open, met reden: het bereik van het harnas is de eenheidsfacet over tien
goldens — de andere twee facetten zijn hierboven gemeten weggelaten, en de
`Fold`/`Match`-schaallezing van de ASP-vertaler wacht op een
eenheiddragende getuige.
