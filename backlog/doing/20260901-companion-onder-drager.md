# Drools: fout-companions leren wélke paden draaien

Goal: De §6.5/§6.6-fout-companions (deling-door-leeg/nul, wortel-van-
negatief) zijn losse statische regels op de leegte van de operanden; ze
kunnen niet testen of het pad waarin de expressie staat ook drááit.
Sinds de Let-node-kaart (2026-09-01) is de gekozen kant nooit-verzinnen
(`letKinderenZeker`): een deling in een niet-genomen IfThenElse-tak, of
in een value die geen enkele lezing forceert, fabriceert geen fout meer
— maar de spiegelflank mist zijn fout. Sinds de L-unificatie (besluit
O3, golf B van `20260901-let-lowering-spec-first`) is die flank GROTER,
want onder call-by-need draait de Let-body ALTIJD:
- op `Let d = deler in inkomen gedeeld door tweede` met `tweede` leeg
  zegt ref *inconsistent* en Drools *consistent* — nu voor élke deling
  of wortel in een Let-body, niet meer alleen wanneer de value gevuld
  is (gemeten met de L-ref, 2026-09-01). Dit is precies de vorm die
  `LetDragerWandelingWitnessTest.
  een_deling_achter_een_nooit_gepasseerde_leeg_barriere_levert_geen_fout`
  vandaag pint: die assertie is groen om de restpost, niet meer om
  overeenstemming met ref, en hoort bij déze fix te worden
  herschreven (protected-achtig: bestaande witness, maintainerbesluit);
- idem voor een value wiens binder op sommige maar niet alle paden
  gelezen wordt, of wiens lezing achter een lezing van een ANDERE
  binder ligt (die kan via diens barrière het pad afbreken —
  `leestAltijd` weigert daar te certificeren).
Geen corpus-golden draagt de vorm — gate 12 wordt luid zodra er één
komt. De echte oplossing: de companion draagt de barrière-/takconditie
mee (vuurt alleen wanneer het omhullende pad aantoonbaar evalueert),
zodat beide flanken met ref meelopen.

Verwant, zelfde klasse maar andere oorzaak (alleen documenteren, geen
fix hier): `startpuntCompanions` (§6.8) is productie-semantiek die ref
in het geheel niet kent — de kale flank divergeert vandaag al; dat is
een spec-kwestie (hoort §6.8 in RUNTIME-SEMANTICS of is de port te
ruim?), geen drager-kwestie.

Acceptance:
- Rood-eerst: de gemeten spiegelflank (deling met lege operand in een
  wél-draaiende Let-body) als witness die vóór de fix faalt tegen ref.
- Na de fix: beide flankfamilies (fabricage én misser) elk met kale
  tweeling ref-conform, mutant-rood bewezen; gate 12 exact gelijk
  (played 98 / auto-excluded 39 / value-divergence 1 / no-case 1 —
  gemeten 2026-09-01) of beter; alle twaalf gates groen.
- De §6.8-vraag als notitie met meting in de Result (geen spec-wijziging
  onder deze kaart).

May touch: `backends/drools/src/main/java/**`,
`backends/drools/src/test/**`, `backlog/`.

Out of scope: spec-teksten, ref/clingo/verifier, corpuspins, de overige
v1-vormen.

## Bouwnotitie (subagent, 2026-09-02)

**Meettabel (stap 2).** Alle flanken eerst door de L-ref gemeten
(`/tmp/1b-metingen/`, dezelfde wire-vormen als de testhelpers; ref-runner
exit 0 = exacte match). Drools "voor" is de stand op HEAD vóór de fix
(letKinderenZeker, nooit-verzinnen), "na" de stand met de sonde.

| flank | model (uit := …) | invoer | ref | Drools voor | Drools na |
|---|---|---|---|---|---|
| a spiegel | `Let d = deler in inkomen ÷ tweede` | tweede leeg, deler leeg | leeg, INCONS | consistent (misser) | INCONS |
| a spiegel | idem | tweede leeg, deler 2 | leeg, INCONS | consistent (misser) | INCONS |
| a kaal | `inkomen ÷ tweede` | tweede leeg | leeg, INCONS | INCONS | INCONS |
| b barrière-vóór | `Let d = deler in d plus (inkomen ÷ tweede)` | beide leeg | leeg, CONS | CONS | CONS |
| b barrière-vóór | idem | deler 2 | 2.0000, INCONS | consistent (misser) | INCONS |
| b barrière-ná | `Let d = deler in (inkomen ÷ tweede) plus d` | beide leeg | leeg, INCONS | consistent (misser) | INCONS |
| b barrière-ná | idem | deler 2 | 2.0000, INCONS | consistent (misser) | INCONS |
| c genomen tak | `als a > 100 dan inkomen ÷ deler anders 7` (kaal én in Let-body) | a 200, deler leeg | leeg, INCONS | consistent (misser) | INCONS |
| c niet-genomen | idem | a 1 | 7.0000, CONS | CONS | CONS |
| d value gelezen | `Let q = inkomen ÷ tweede in q plus 1` | tweede leeg | leeg, INCONS | INCONS | INCONS |
| d value ongelezen | `Let q = inkomen ÷ tweede in 42` | tweede leeg | 42.0000, CONS | CONS | CONS |
| d value één tak | `Let k = a in Let q = inkomen ÷ tweede in als k > 100 dan q anders 7` | a 200 / a 1 | leeg INCONS / 7.0000 CONS | consistent (misser) / CONS | INCONS / CONS |
| e vreemde lezing vóór | `Let x = leegding in Let q = inkomen ÷ tweede in x plus q` | beide leeg | leeg, CONS | CONS | CONS |
| e vreemde lezing ná | … `in q plus x` | beide leeg | leeg, INCONS | consistent (misser) | INCONS |
| f wortel spiegel | `Let d = deler in wortel(saldo)` (kaal: `wortel(saldo)`) | saldo −4 | leeg, INCONS | consistent (misser) / INCONS | INCONS |
| f wortel keuze | `Let a = a in als a > 100 dan wortel(saldo) anders 7` | a 200 / a 1 | leeg INCONS / 7.0000 CONS | consistent (misser) / CONS | INCONS / CONS |
| g defaults | ch.-7-groep, consequentie `Let d = deler in inkomen ÷ tweede` | tweede leeg / tweede 2 | toeslag 0.0000 CONS (ch. 7.1: speculatieve deponering vervalt) / 5.0000 CONS | weigering (delingshek) | weigering (delingshek) |
| h twee delingen | `(inkomen ÷ deler) plus (Let d = leegding in d plus (inkomen ÷ tweede))` | alles leeg / deler 2, leegding 1 / deler 2 | 0.0000 INCONS / 6.0000 INCONS / 5.0000 CONS | INCONS / consistent (misser) / CONS | INCONS / INCONS / CONS |
| p parameter in drager | `Let d = deler in (inkomen ÷ tweede) plus GRONDSLAG` (param 3) | tweede leeg / tweede 2 | 3.0000 INCONS / 8.0000 CONS | consistent (misser) / CONS | INCONS / CONS |

ref's evaluatievolgorde van `Binair` is links vóór rechts
(`ref/lib/eval.ml`, gemeten met b en e): een lezing links van de deling
kapt de Let af vóór de deling, een lezing rechts ervan niet.

**Gekozen constructie: kandidaat (ii), de sonde.** Per gastregel
verzamelen `verzamelDelingen`/`verzamelWortels` nu élke plek die KAN
draaien (volle `letKinderen`) mét de buitenste drager erboven
(`Plek(site, drager)`). Een plek zónder drager houdt het statische
leeg/negatief-predicaat (v0 ongewijzigd, DRL-pariteit). Een plek ónder
een drager krijgt een companion die in zijn consequentie de
`WaardeFn` van die buitenste drager op het subject her-evalueert met een
`Sonde` in de omgevingsmap (sleutel `"sonde fout"`, met spatie); de
delingsarm van `scalarWaarde` en de wortelarm van `scalarWrapper` melden
een §6.5/§6.6-fout aan de sonde (no-op zonder sonde — de gastregel zelf
draagt er geen), en de companion insert de Inconsistentie alleen als
precies díe plek gemeld is. Barrières, call-by-need en evaluatievolgorde
komen zo uit de value-lowering zelf, die ref's operationele semantiek
is; de value-side is niet verbouwd (twee meldregels erbij). De
consequentie bindt de globals van de drager zoals de gastregel de zijne
(`consequenceOf`/`vulGlobalMap`); een drager die niet lowert of een
global die de gast niet kon binden is een invariantschending en faalt
hard. Het patroon krijgt de freshness-expr (klasse-reactief), de
§9.5-idempotentieguard blijft. De buitenste drager is de juiste eenheid:
alle niet-dragervormen die de walk afdaalt (Binair, Afgerond, Begrensd,
Functie-argumenten) evalueren hun kinderen onvoorwaardelijk, hier en in
ref, en een barrière ontsnapt nooit aan haar eigen Let; `lowerVal` (de
aggregatie-in-arithmetiek-route) lowert geen drager (gast skipt), dus
elke dragerplek zit onder `scalarWaarde`.

**Verworpen: kandidaat (i), statische padpredicaten.** Een predicaat per
plek zou de IfThenElse-condities (letConditie-lowering) én de
Let-leesstructuur moeten coderen, inclusief de vraag welke binder vóór
de plek in evaluatievolgorde gelezen wordt en of diens value leeg is —
geneste Lets, gedeelde binders, lezingen in condities. Dat is een tweede
implementatie van ch. 10.6 naast `letWaarde`, die bij elke verbreding van
letConditie/letWaarde mee zou moeten bewegen en die precies de
volgordevraag zou moeten modelleren die (ii) gratis meet. Niet smaller,
wel fout-gevoeliger.

**Indexering.** Ongewijzigd voor v0-vormen. Voor regels mét drager telt
de kwalificerende lijst nu ook plekken in takken/values die
letKinderenZeker uitsloot; het suffix blijft per kwalificerende plek
(flank h: `(deling door leeg 1)` statisch, `(deling door leeg 2)` sonde,
in één KieBase gebouwd). DRL lowert geen Let, dus daar is geen pariteit
te bewaren.

**Vervallen.** `letKinderenZeker`, `Lees`, `leestAltijd`, `samen`,
`samenTakken` (137 regels); de `LetVorm`-javadoc herschreven naar
(a) refusal/registratie-walks via letKinderen, (b) de twee
answer-walks via letKinderen mét sonde, (c) bewust niet afdalen.

**Mutanten** (in /tmp-kopieën via git archive): (1) padconditie uit
(`if (true || plek.drager() == null)`): 9 rood, precies de
fabricage-flanken (niet-genomen tak, ongelezen value, één-tak
niet-genomen, barrière-vóór, vreemde lezing eerst, wortel niet-genomen,
h3, plus de twee nooit-verzinnen-witnesses in LetDragerWandeling); de
misser-flanken groen. (2) afdaling weg: 14 rood, precies de
misser-flanken (11 in CompanionPad, 3 in LetDragerWandeling); de
fabricage-flanken groen.

**Gates.** Gate 1 PASS. Gate 12 PASS: played 104 / auto-excluded 39 /
value-divergence 1 / no-case 1, JUnit 125 → 138, nul skipped
(12 nieuwe witnesses + 1 zuster-witness). Geen spelende golden draagt
een kwalificerende deling/wortel onder een drager (gecontroleerd:
kinderweging en tariefstelling delen door een literal), dus de telling
kon niet bewegen.

**Open punten.**
- `companionOperand`-scope bewust gehouden (kaartgrens): een BinderRef
  als dividend (`Let n = teller in n ÷ deler`, de vorm die de
  emitter voor "Daarbij geldt" produceert) kwalificeert niet en krijgt
  geen companion waar ref wél inconsistent zegt. Met de sonde is de
  verbreding triviaal (zij observeert de evaluatie in plaats van de
  operanden opnieuw te lezen) — kandidaat voor een vervolgkaart.
- `startpuntCompanions` (§6.8) blijft drager-blind; niet aangeraakt,
  alleen gedocumenteerd (de spec-vraag staat in de kaarttekst).
- Companions (statisch én sonde) dragen géén stratificatie-salience
  terwijl de gastregel die kan hebben; pre-existent. De sonde verbreedt
  de leesverzameling tot de hele drager: een later vurende producent kan
  alleen een fout tóevoegen (insert-only, klasse-reactief), en een fout
  die vroeg op een nog-niet-afgeleide lezing wordt geïnsert wordt nooit
  ingetrokken — dezelfde klasse als bij de statische companions.
- (g) defaults: ref laat de speculatieve §9.5-deponering van een
  doorgevallen default vervallen (0.0000, consistent); Drools blijft
  het delingshek weigeren (waarde niet geproduceerd) — een
  defaults-kaart, niet deze.
- De klasse-javadoc van `LetDragerWandelingWitnessTest` (punt 4, "must
  NOT see through … descending there fabricates") beschrijft het
  vóór-fix-mechanisme; de witnesses erin gelden nog. Tekst buiten de
  toegestane methode, dus niet aangeraakt.
- De `Excluded`-vraag: een dragerplek waarvan de drager niet her-lowert
  faalt hard (`IllegalStateException`) in plaats van luid te skippen;
  bewust (invariant: de gast lowerde die drager via dezelfde
  scalarWaarde), maar het is een throw in de transpile-fase, geen
  Dekking-skip.
