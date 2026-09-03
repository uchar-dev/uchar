# Ongepinde executor-asymmetrie: een niet-literale `Construct`-lading speelt in `ref` en wordt door de ASP-drager geweigerd

Goal: Er is vandaag een vorm die `ref` exact speelt en die de
ASP-drager luid weigert, en GEEN corpusdocument pint dat. Gemeten in
fixronde 2 van `20260902-meldplichtspoor-niveau0` (in een `git
archive`-kopie, de werkkopie ongemoeid): een `Fold` waarvan de `init`
een `Construct` is met een `AttribuutRef` als argument in plaats van een
`Literal` — de gereïficeerde meldplicht met haar EIGEN termijn,
`Openstaand(<de termijn van deze meldplicht>)` — is verifier-clean
(`verifier: PASS`), speelt door `ref` (`oracle: PASS (3
expectations)`), en wordt door de ASP-drager vóór vertaling geweigerd,
letterlijk:

```
TRANSLATION FAULT: a Construct payload other than a literal is not
translated (a bound payload would need underivability propagation; no
corpus case)
```

De weigeringstekst zegt zelf dat er geen corpuscasus is — dat is precies
het gat. Een asymmetrie die geen enkele gate ziet, is een asymmetrie die
stil kan wegdrijven: hij kan verdwijnen (dan merkt niemand dat hij weg
is) of verergeren (dan merkt niemand dat ook niet). Een van de twee moet
gebeuren: de drager gaat de vorm dragen (dan wordt het een golden die
ref ≡ clingo pint), of de weigering wordt als weigering vastgelegd. Bij
`corpus/rood/` hoort een fout die in BEIDE executors observeerbaar
faalt, en die eis haalt deze vorm niet — `ref` speelt hem gewoon — dus
rood is NIET de plek en de kaart moet eerst bepalen wat de plek dan is.

Dit is ook de smalle rest van de identiteitsgrens die de
meldplichtspoor-NOTE post (b) noemt: een relationele positie is
uitdrukbaar door reificatie, maar "elk met een eigen termijn" strandt —
niet op identiteit, op dit hek.

## Premissecorrectie (meetfase 2026-09-03)

De Goal hierboven staat er nog zoals hij geschreven is (de geschiedenis
blijft leesbaar), maar zijn diagnose is door de meting weerlegd. Drie
correcties:

1. **Het hek is niet de `Construct`-lading.** Een `Construct` met een
   `AttribuutRef`-lading speelt al lang op regelniveau (sinds fase 5b;
   de anchor-regel `herbeschikking` doet het) en ook in een Match-arm.
   Gemeten: een KALE `AttribuutRef` als vouw-init weigert net zo goed,
   zonder enige `Construct` in zicht. De lading is niet wat weigert.

2. **De ware oorzaak van de gemelde weigering is dat de vouw-init door de
   SUBJECTLOZE route loopt.** De fold-init wordt vertaald door
   `closed_term` — de gesloten-lichaam-grammatica, die per definitie geen
   subject heeft om een `val`-atoom uit te lezen. Élke subject-afhankelijke
   init strandt daar, `Construct` of niet; gemeten met een KALE
   `AttribuutRef`-init (E9), zonder enige `Construct` in zicht.

   *Correctie in fixronde 2:* er zijn drie deuren gemeten, maar ze leiden
   niet alle drie naar die ene kamer. ÉÉN is de subjectloze kamer: de
   `closed_term`-route (sinds fixronde 1 met een eigen arm vóór de
   catch-all — deur 4). De andere twee staan er los van en vuren ook
   ZONDER vouw, MÉT subject in scope. `construct_ground_term` is een
   GRONDTERM-beperking — de positie geeft een term terug en kan geen
   lichaamsatoom dragen (gemeten met E6, een somtype-vergelijkingsoperand,
   waar geen vouw aan te pas komt). Het tweede hek in `expr_term` is een
   REGELNIVEAU-ladingbeperking (gemeten met E5, `Construct(ParameterRef)`
   als gewone regelwaarde — een gewone schrijfregel, subject volop in
   scope). Dat E4 (`Openstaand(<parameter>)` als init) door de
   grondterm-deur viel en na de fix wordt gedragen zónder ook maar één
   regel subjectmachinerie te raken, is daarvan het bewijs. De gebouwde fix
   maakt exact die scheiding: 1a repareert de grondterm-kant, 1c de
   subject-kant.

3. **"Underivability propagation" is NIET de ontbrekende machinerie.**
   Leeg propageert al correct en `ref` ≡ clingo op lege ladingen: een
   lege init derft geen basisfeit, dus de hele vouw is leeg — precies wat
   RUNTIME-SEMANTICS ch. 10.1/10.2 normatief voorziet. Die reden in de
   weigeringstekst was onwaar en is met deze kaart vervangen.

Maintainersbesluit na de meetfase: **route 1 + 3**. De
subject-gesleutelde vouw-init gaat de drager dragen (met een hek dat een
gemeten FOUT antwoord op de niet-gesleutelde bron afvangt), en de rest
wordt vastgelegd als gecorrigeerde S19-inventaris plus faults-fixtures.

**Waarom route (2) afvalt** (de acceptatie vraagt de drie routes expliciet
af te wegen, niet stilzwijgend één te kiezen). Route (2) maakt de weigering
NORMATIEF: er komt een WF-nummer bij, de vorm wordt IR-breed ongeldig, en
gate 3 wijst hem af met een negatief-casus in plaats van gate 6 met een
vertaalfout. Dat kan hier niet kloppen, want de weigering is
DRAGERSSPECIFIEK: elke geweigerde vorm is verifier-clean en `ref` rekent
haar exact uit. Gemeten, alle vier met `verifier: PASS` — E1 (bron-hek)
`oracle: PASS (2/3 expectations)`, E5 (regelniveau) `oracle: PASS (2
expectations)`, E6 (grondterm) `oracle: PASS (2 expectations)`, en de
vierde deur (`Openstaand(de termijn + 1)` als init, §L1-F2) `oracle: PASS
(2 expectations)` op `Openstaand(8)`. Een WF-nummer zou dus vormen
verbieden die RUNTIME-SEMANTICS definieert en die de referentie-interpreter
uitrekent: de IR versmallen om één drager te beschrijven. Dat is precies
omgekeerd aan "de specificatie is het product, implementaties zijn haar
bewijs". Route (2) valt daarmee inhoudelijk af, niet uit gemak — wat
overblijft is dragen waar het kan (1) en de grens als dragersbeperking
optekenen waar het niet kan (3).

Acceptance:
- De vorm hermeten en met echte uitvoer opgetekend: verifier, `ref` en
  de ASP-drager, elk met de letterlijke regel.
- Bepaal en beargumenteer WAAR de asymmetrie thuishoort. Drie routes,
  expliciet afwegen, niet stilzwijgend één kiezen: (1) de drager draagt
  de vorm — dan landt een golden en verdwijnt de asymmetrie; (2) de
  weigering is normatief bedoeld — dan hoort er een WF-nummer bij, met
  negatief-casus, en wordt zij door gate 3 gedragen in plaats van door
  een vertaalfout; (3) de weigering is een gedocumenteerde
  dragersbeperking — dan hoort zij in de weigeringsinventaris die de
  spec al voert, met de reden erbij.
- Wat route (1) zou kosten is de eerste vraag die beantwoord moet
  worden: de weigeringstekst noemt "underivability propagation" als de
  ontbrekende machinerie. Meet of dat klopt.
- Geen pin wordt aangepast om een executor te laten slagen; als de vorm
  maar door één executor speelt, landt er geen golden.
- Alle twaalf gates groen.

May touch: hangt van de gekozen route af; `backends/clingo/**` bij (1),
`spec/**` als proposal bij (2) en (3), `corpus/**` bij (1).

Out of scope: de identiteitsvraag zelf (een objectverwijzing als
datatypesoort — dat is de andere helft, post (a) in de
meldplichtspoor-NOTE, en een eigen fase); `scripts/**`.

## Bouwnotitie (2026-09-03)

### 1. Rood-eerst: zeven vormen gemeten VÓÓR de fix

Documenten en cases in `/tmp/ah-vormen/`, alle zeven verifier-clean
(`verifier: PASS`) en alle zeven gespeeld door `ref`. De ASP-drager
weigerde er vijf, met DRIE verschillende teksten — dat is de meting die
de kaartpremisse weerlegt.

| vorm | wat het is | `ref` | ASP-drager vóór |
|---|---|---|---|
| E0 | `Construct(AttribuutRef)` als gewone regelwaarde | `oracle: PASS (2 expectations)` | `clingo: PASS (2 expectations)` — speelde al |
| E1 | lijstPARAMETER als bron + `Construct(Openstaand, AttribuutRef termijn)` als init | `oracle: PASS (2/3 expectations)` | deur A |
| E4 | `Construct(Openstaand, ParameterRef)` als init | `oracle: PASS (2 expectations)` | deur A |
| E5 | `Construct(Openstaand, ParameterRef)` als gewone regelwaarde | `oracle: PASS (2 expectations)` | deur B |
| E6 | `Construct(Openstaand, AttribuutRef)` als somtype-vergelijkingsoperand | `oracle: PASS (2 expectations)` | deur A |
| E8 | EIGEN attribuutlijst als bron, twee subjecten met termijn 30 en 10 | `oracle: PASS (3 expectations)` | deur A |
| E9 | KALE `AttribuutRef` als init (geen `Construct` in zicht) | `oracle: PASS (2 expectations)` | deur C |

De drie teksten, letterlijk:

```
deur A  TRANSLATION FAULT: a Construct payload other than a literal is not translated
        (a bound payload would need underivability propagation; no corpus case)

deur B  TRANSLATION FAULT: a Construct payload other than a literal or an own attribute
        read is not translated (a Match or composite payload has no corpus case)

deur C  TRANSLATION FAULT: a closed-body term here is a bound binder, a literal, a
        parameter, a +/-/×/percentage-of combination, a division (bare or abs), a power
        by a constant integer exponent, a Construct or a Let (no corpus case for more)
```

E9 is het beslissende getuigenis: een kale attribuutlezing als init
weigert net zo hard als de `Construct`. De lading was nooit het hek — de
subjectloze `closed_term`-route is het.

### 2. De 1b-valstrik: het FOUTE antwoord, met de hand nagemeten

In een wegwerpkopie (`git archive` van de worktree, werkkopie ongemoeid)
de naïeve route geïmplementeerd — subject-afhankelijke init rijdt
`expr_term`, ZONDER hek op de bron — en E1 gedraaid met twee subjecten
(J1 termijn 30, J2 termijn 10, beide reeks `[Dagverloop(20), Melding]`;
handrekening J1 = `Vervuld`, J2 = `Geschonden`):

```
MISMATCH: J1.meldpositie = [geschonden;vervuld], expected vervuld
MISMATCH: J2.meldpositie = [geschonden;vervuld], expected geschonden
```

Beide uitkomsten voor beide subjecten. De oorzaak staat in de
gegenereerde regel zelf:

```
fa1(plist("meldreeks"),0,openstaand(A2)) :- lpres(plist("meldreeks")), val(S,"termijn",A2).
fr1(plist("meldreeks"),F) :- lpres(plist("meldreeks")), N = #count { I : lelem(plist("meldreeks"),I,_) }, fa1(plist("meldreeks"),N,F).
val(S,"meldpositie",F5) :- is_a(S,"jutter"), fr1(plist("meldreeks"),F5).
```

`S` staat WEL in het lichaam en NIET in de kop: de sleutel is
`plist("meldreeks")` voor iedereen dezelfde, dus J1's 30 en J2's 10
landen als twee init-feiten op één sleutel en de vouw derft beide
uitkomsten, die de schrijfregel vervolgens aan beide subjecten geeft.
Dit is geen ontbrekend antwoord maar een FOUT antwoord — precies wat een
drager nooit stil mag doen. Dat motiveert het 1c-hek.

Ter contrast, dezelfde naïeve route op E8 (eigen attribuutlijst):
`clingo: PASS (3 expectations)`, met de sleutel

```
fa1(alist(S,"meldreeks"),0,openstaand(A2)) :- lpres(alist(S,"meldreeks")), val(S,"termijn",A2).
```

Dezelfde `S` in kop én init-lezing: geen botsing mogelijk. Dat is exact de
voorwaarde die het hek nu toetst.

### 3. Wat er is gebouwd (stand van de tak: 87 toegevoegde, 8 verwijderde regels in `translate.ml`)

**1a — `closed_term`'s `Construct`-arm delegeert recursief.** Een
niet-literale lading is zelf een gesloten-lichaam-term en lowert door
dezelfde functie; de bindingsatomen reizen mee naar buiten, zodat de
basisregel van de vouw ze in zijn lichaam krijgt. Draagt E4 (en een
rekencombinatie als lading).

**1c — het hek op de vouw-init.** Een init waarvan de VORM een
subjectlezing is (`init_is_subject_read`: een `AttribuutRef`, of een
`Construct` waarvan de lading dat is) rijdt `expr_term` — maar alleen als
de bron subject-gesleuteld is (`source_subject_keyed`: één vorm, een EIGEN
attribuutlijst). Anders een luide weigering met de ware reden. Draagt E8 en
E9. (Deze twee namen en de ene bronvorm zijn de stand ná fixronde 1: het
predicaat heette eerst `init_reads_subject`, en het hek erkende ook een
`MapList`-bron — zie §L1-F3 en §L1-F4.)

**De teksten.** `construct_ground_term` zegt niet langer
"underivability propagation" (onwaar) maar de ware reden: die route geeft
een term ZONDER bindingsatomen terug, dus een lading die gebonden moet
worden heeft er geen plek. De regelniveau-tekst noemt nu ook de
parameterlezing, die er feitelijk in valt.

Meting NA de fix, dezelfde zeven vormen (de E1-tekst is de stand ná
fixronde 1; §L1-F4 verving de eerdere formulering, die zonder de
`MapList`-tak onwaar was geworden):

| vorm | ASP-drager na |
|---|---|
| E0 | `clingo: PASS (2 expectations)` |
| E1 | `TRANSLATION FAULT: a subject-dependent Fold init is carried only over an own attribute list, whose id alist(S,attr) carries the subject: over a list parameter the id is plist(p) alone, so two subjects' init facts collide on that one key and every subject reads every subject's outcome — a wrong answer, not a missing one; a derived source (a mapped list) keeps the subject in its id but has no corpus case` |
| E4 | `clingo: PASS (2 expectations)` |
| E5 | `TRANSLATION FAULT: a Construct payload other than a literal or an own attribute read is not translated at rule level (a parameter read, a Match or a composite payload has no corpus case)` |
| E6 | `TRANSLATION FAULT: a Construct payload other than a literal is not a ground term (a payload that must be bound needs a body atom this position cannot carry; no corpus case)` |
| E8 | `clingo: PASS (3 expectations)` |
| E9 | `clingo: PASS (2 expectations)` |

### 4. De golden: `corpus/golden/jutterswet-meldtermijnen`

Vijf cases op een gereïficeerde `Meldplicht` met een eigen `termijn`, een
eigen `meldreeks` en een `meldpositie` als somtype. Elke verwachting
eerst met de hand gerekend (de tabellen staan in de NOTE), daarna door
`ref` gemeten, daarna door clingo:

| case | handrekening | `ref` | clingo |
|---|---|---|---|
| case1-eigen-termijnen | M1 (termijn 30) `Vervuld`, M2 (termijn 10) `Geschonden` op DEZELFDE reeks | PASS (3) | PASS (3) |
| case2-lege-termijn | M3 (geen termijn) leeg, M4 (termijn 20) `Openstaand(15)` | PASS (3) | PASS (3) |
| case3-lege-reeks | M5 (termijn 7), reeks aanwezig en leeg → `Openstaand(7)` | PASS (2) | PASS (2) |
| case4-laatste-dag | M6 (termijn 30) `Vervuld`, M7 (termijn 29) `Geschonden` | PASS (3) | PASS (3) |
| case5-gesettelde-stand | M8 `Vervuld` blijft, M9 `Geschonden` blijft | PASS (3) | PASS (3) |

Vijf mutanten gemeten in `ref`, elk gedood en door welke case:

| mutatie | overleeft nog | gedood door |
|---|---|---|
| init wordt de constante `Openstaand(30)` | geen | alle vijf |
| `<=` verzwakt tot `<` | 1, 2, 3, 5 | case4 (M6 → `Geschonden`) |
| aftrekking omgedraaid | 1, 3, 4, 5 | case2 (M4 → `Openstaand(-15)`) |
| buitenste `Vervuld`-arm → `Geschonden` | 1, 2, 3, 4 | case5 (M8) |
| buitenste `Geschonden`-arm → `Vervuld` | 2, 3 | case1 (M2), case4 (M7), case5 (M9) |

De eerste rij is de bestaansreden: de constante-init-mutant is precies
het document dat het corpus al kon, en alle vijf cases zien het verschil.

### 5. Fixtures en S19

Faults-fixtures in `backends/clingo/test/test_clingo.ml`, één per blijvende
deur, elk assertend op de LETTERLIJKE weigeringstekst — het
overlever-patroon: een tekst die wegdrijft of een weigering die stil
verdwijnt laat de suite vallen. Bestaande checks onaangeroerd. Geleverde
stand: **vier** fixtures (fixronde 1 zette de vierde, §L1-F2), suite van
**15 naar 19 checks**, en gate 6 draait haar met een niet-nul-eis.

S19 (`spec/ASP-TRANSLATION.md`, voorstel) beschreef één van de deuren en
gaf hem een reden die niet de ware was. Geleverde stand ná fixronde 2: de
alinea noemt ZEVEN plekken — DRIE die dragen (regelniveau-attribuutlezing;
een VERTAKKENDE gesloten lading via `body_branches`; een NIET-vertakkende
via `closed_term`, recursief) en VIER die weigeren, elk met zijn eigen ware
reden, waarvan er drie over de lading gaan en de vierde (het bron-hek) over
de VORM van de init — plus de expliciete correctie dat leeg-propagatie NIET
de reden is en de verwijzing naar de fixtures die de weigeringen pinnen.
(De fixturenamen houden hun nummering — deur 1 is het bron-hek; S19 zet dat
hek nu als laatste, omdat het als enige geen ladingpositie is.)

### 6. Poorten vóór fixronde 1 (exacte tellingen, basis = `c3a0eda`)

| poort | basis | na | delta |
|---|---|---|---|
| 1 self-contained | PASS | PASS | — |
| 2 ir-schema | 170 files | 171 files | +1 golden |
| 3 verifier | 170 golden clean, 206 negatief | 171 golden clean, 206 negatief | +1 golden |
| 4 case-schema | 418 case files | 423 case files | +5 cases |
| 5 oracle | 396 replayed | 401 replayed | +5 cases |
| 6 clingo | 396 replay | 401 replay | +5 cases |
| 7 no-op | 360 weerleggend, 36 overlevers | 365 weerleggend, 36 overlevers | +5 weerleggend, GEEN nieuwe overlever |
| 8 rood | 22 rode cases | 22 rode cases | — |
| 12 drools | played 105, auto-excluded 41 | played 105, auto-excluded 42 | +1 reden-dragende weigering (SumType-groep) |

Poort 7 is het punt om op te letten: case2 draagt een `attributeEmpty`
die een niets-doende executor ZOU overleven, maar de gevulde helft (M4)
in dezelfde case valt om, dus de case als geheel weerlegt en
`scripts/` blijft onaangeraakt.

### 7. Wat open blijft

- **S15 beschrijft de basisregel nog als `faN(L,0,⟨init⟩) :- lpres(L).`**
  Sinds deze kaart kan `⟨init⟩` bindingsatomen meebrengen, die in dat
  lichaam terechtkomen. Het mandaat van deze kaart beperkte de
  spec-aanraking tot de S19-inventaris, dus S15 is niet aangevuld — dat
  is een aparte, kleine spec-kaart.
- **De plist-rest.** Een subject-afhankelijke init over een lijstparameter
  is uit te drukken zodra `fa`/`fr` op (lijst-id, subject) gesleuteld
  worden in plaats van op de lijst-id alleen. Dat is een echte
  encoding-wijziging (elke vouwregel krijgt een extra argument) en heeft
  vandaag geen casus die hem vraagt; nu vastgelegd als weigering met de
  ware reden.
- **Deur B en C** (regelniveau-parameterlading, grondterm-positie) hebben
  geen casus. Beide zijn goedkoop te dragen langs dezelfde recursieve lijn
  als 1a zodra er één is.

## Fixronde 1 (2026-09-03) — vijf lensbevindingen

De lens draaide mutanten en boobytraps over deze kaart en vond twee echte
gaten van dezelfde soort als het gat dat de kaart kwam dichten, plus drie
kleinere. Alles hieronder is gemeten, met de letterlijke uitvoer.

### L1-F1 (hoog) — 1a had geen enkel artefact: GEFIXT

Onafhankelijk gereproduceerd in een wegwerpkopie (`git archive` van de
worktree): de recursieve `Construct`-arm van `closed_term` vervangen door een
fault, en poort 6 bleef volledig groen —

```
check-clingo: PASS (401 cases replay exactly through clingo — ref = clingo over the corpus)
```

De reden is scherper dan gedacht: `body_branches` heeft een EIGEN
`Construct`-arm voor een berekende lading, dus de vouwSTAP van de golden
(`Openstaand(resterend - dagen)`) komt nooit bij `closed_term`. Alleen een
positie die `closed_term` rechtstreeks aanroept — de vouw-INIT — bereikt de
nieuwe arm, en geen corpusdocument deed dat.

De golden heeft er nu een tweede regel bij: `standaardpositie van een
meldplicht`, dezelfde vouw over hetzelfde register met
`init = Openstaand(de standaardtermijn)` — een documentparameter als lading,
dus de gesloten route. Gemeten met de arm eruit gemuteerd, alle vijf cases:

```
case1-eigen-termijnen.json: TRANSLATION FAULT: MUTANT-1A: closed_term Construct recursion removed
… (idem case2 t/m case5)
```

en met de arm erin, handrekening eerst (de tabellen staan in de NOTE), daarna
`ref`, daarna clingo:

| case | verwachtingen | `ref` | clingo |
|---|---|---|---|
| case1 | 5 | PASS | PASS |
| case2 | 7 | PASS | PASS |
| case3 | 3 | PASS | PASS |
| case4 | 5 | PASS | PASS |
| case5 | 5 | PASS | PASS |

De basisregel, uit de drager zelf gehaald, is het bewijs dat de lading haar
bindingsatoom meeneemt naar buiten:

```
fa6(alist(S,"meldreeks"),0,openstaand(P7)) :- lpres(alist(S,"meldreeks")), param("standaardtermijn",P7).
```

Drie documentmutanten gemeten in `ref`: de init van `standaardpositie` laten
lezen wat `meldpositie` leest → geen enkele case overleeft; de twee routes
verwisselen → geen enkele case overleeft; de parameterlezing vervangen door de
constante 21 → case1/2/3/5 overleven, case4 weerlegt (die case zet de
parameter op 30, juist daarvoor).

### L1-F2 (hoog) — een VIERDE weigerende plek, met een misleidende tekst: GEFIXT

De catch-all van `closed_term` weigerde een subject-lezing en somde daarbij
"a Construct or a Let" op als gedragen — precies de vormen waar de weigering
doorheen kwam. De lezing die faalt is het BLAD, bereikt dóór die gedragen
vormen heen. Er staat nu een eigen arm vóór de catch-all die dat zegt:

```
TRANSLATION FAULT: an attribute read inside a closed body has no subject to read from
(a lambda or function body and a fold init are subject-free); only a Fold init whose own
SHAPE is an attribute read, bare or directly under a Construct, takes the subject-keyed
rule-level route
```

Gemeten op `init = Openstaand(de termijn + 1)` (verifier: PASS): `ref`
`oracle: PASS (2 expectations)` op `Openstaand(8)`, clingo bovenstaande
weigering. Een vierde faults-fixture pint die letterlijke tekst; met "ZZZ" in
de needle meldt de suite hem terug, dus de fixture pint de tekst en niet de
vorm van het falen. S19 telt nu zes plekken: twee die dragen, VIER die
weigeren, met de vierde als eigen bullet.

### L1-F3 (middel) — de vormtoets: BEWUST GELATEN, met de weigering als grens

`init_reads_subject` heet nu `init_is_subject_read`, want dat is wat hij doet:
hij kiest de ROUTE en noemt de twee vormen die `expr_term` mét subjectlezing
draagt. Verbreden naar "leest dit ergens het subject" is gemeten en
afgewogen:

- `Openstaand(de termijn + 1)` en `Let t = de termijn in Openstaand(t)`
  verhuizen dan van de ene luide weigering naar de andere (de
  regelniveau-`Construct`-arm draagt een samengestelde lading niet) — geen
  winst, wel een minder precieze reden.
- `Let ongebruikt = 1 in Openstaand(de termijn)` zou WEL nieuw gedragen
  worden. Gemeten: verifier PASS, `ref` `oracle: PASS (3 expectations)` op
  case1 en `PASS (2)` op case3, clingo de nieuwe deur-4-weigering. Maar per
  ch. 10.6 clause 3 is die Let gedragsloos, en dragen zonder getuige is exact
  de drift die deze kaart bestrijdt; een getuige ervoor zou een kunstmatige
  zesde regel in de golden zijn.

Wat wél moest gebeuren is gebeurd: de weigering noemt nu zelf de as waarop de
grens ligt, en een fixture pint hem. De asymmetrie is daarmee vastgelegd —
precies de dispositie die de kaart voorschrijft ("de drager draagt de vorm, óf
de weigering wordt als weigering vastgelegd").

### L1-F4 (laag) — de ongedekte `Map_list`-tak: GESCHRAPT

De tak was een verbreding die geen poort zag. Hij is weg; het hek erkent nu
één bronvorm. De weigeringstekst was zonder die tak ONWAAR geworden (voor een
afgebeelde lijst is de id `lm(alist(S,attr))` en botst er niets), dus de tekst
noemt die vorm nu apart en met haar ware reden:

```
TRANSLATION FAULT: a subject-dependent Fold init is carried only over an own attribute
list, whose id alist(S,attr) carries the subject: over a list parameter the id is
plist(p) alone, so two subjects' init facts collide on that one key and every subject
reads every subject's outcome — a wrong answer, not a missing one; a derived source
(a mapped list) keeps the subject in its id but has no corpus case
```

Gemeten op de golden met een identiteits-`MapList` om de bron heen: verifier
PASS, `ref` `oracle: PASS (3 expectations)`, clingo bovenstaande weigering.
Fixture 1 pint de nieuwe tekst (dat is de fixture die deze kaart zelf zette;
de vijftien checks van vóór de kaart zijn onaangeroerd).

### L1-F5 (laag) — de afwezige-bron-flank: GEPIND

case2 draagt er een derde instantie bij: M10, mét termijn en ZONDER
`meldreeks`. Beide posities leeg, om een andere reden dan M3's lege init — daar
derft de init niets, hier derft de LIJST geen aanwezigheid. `ref` en clingo
beide `PASS (7 expectations)`. Poort 7 krijgt er geen overlever bij: M4 blijft
de weerlegger in dezelfde case.

En de scherpste bijvangst van de tweede regel zit in dezelfde case: M3's
`meldpositie` is leeg terwijl zijn `standaardpositie` `Openstaand(16)` is — op
één instantie, in één case. Daarmee is de leegte bewijsbaar die van de
init-lezing en van niets anders.

### Poorten na fixronde 1

| poort | uitkomst |
|---|---|
| 1 self-contained | `check-self-contained: PASS` |
| 2 ir-schema | `PASS (schema valid; 171 file(s) validate and round-trip)` |
| 3 verifier | `PASS (171 golden clean; 206 negatief rejected for their declared rule)` |
| 4 case-schema | `PASS (schema valid; 423 case file(s) validate)` |
| 5 oracle | `PASS (401 cases replayed exactly …)` |
| 6 clingo | `test_clingo: 19 checks, 0 failures` + `PASS (401 cases replay exactly …)` |
| 7 no-op | `PASS (365 cases refute a do-nothing executor; 36 pinned survivors …)` |
| 8 rood | `PASS (22 red cases fault observably in ref and clingo)` |

Geen tellingen verschoven: de golden groeide in REGELS en VERWACHTINGEN, niet
in bestanden (5, 7, 3, 5 en 5 verwachtingen, was 3, 3, 2, 3 en 3). De suite
gaat van 18 naar 19 checks.

## Fixronde 2 (2026-09-03) — zeven lensbevindingen

De code en de artefacten hielden: de vier deuren zijn stuk voor stuk echt,
de vier fixtures pinnen hun letterlijke tekst, de basisregels staan in de
drager zoals NOTE en kaart ze citeren. Wat niet hield was één inhoudelijke
tekstclaim en vier stukken kaartadministratie. Alles hieronder is gemeten.

### L2-F1 (middel) — een DERDE dragende plek, die S19 niet telde: GEFIXT

S19 stelde: "SIX distinct places … two carry it". Zelf hermeten in een
wegwerpkopie (`git archive` van de tak, werkkopie ongemoeid): `body_branches`
heeft een EIGEN `Construct`-arm voor een berekende lading — de arm die de
reduce-lowering en élke vertakkende vouwSTAP rijdt. Die arm vervangen door
een fault (`if true then fault "MUTANT-BB: …"`, `dune build backends/clingo`
slaagt) en poort 6 gedraaid:

```
$ sh scripts/check-clingo.sh 2>&1 | grep -c "MUTANT-BB"
18
$ grep '^FAIL' | sed 's#/case.*##' | sort | uniq -c
   2 corpus/golden/jutterswet-kamerhuishouden-catala
   8 corpus/golden/jutterswet-meldplichtspoor
   5 corpus/golden/jutterswet-meldtermijnen
   3 corpus/golden/jutterswet-penningstaffel-catala
```

Achttien cases, inclusief alle vijf van de NIEUWE golden: haar vouwstap is
`Openstaand(<resterend> - <dagen>)` onder een `IfThenElse`, dus vertakkend,
dus door `body_branches`. Het is dezelfde soort fout als de fout die deze
kaart kwam repareren — en het is er de OORZAAK van: juist doordat S19 de twee
gesloten-lichaam-armen als één telde, leek de vouwstap de getuige van de
`closed_term`-arm te zijn, en bleef L1-F1 een ronde lang onopgemerkt.
(Verzachtend: de arm stond elders in hetzelfde document wél correct, bij de
reduce-lowering; fout was de TELLING en de claim "two carry it".)

S19 telt nu ZEVEN plekken: DRIE die dragen — (i) regelniveau, (ii) een
VERTAKKENDE gesloten lading via `body_branches`, (iii) een NIET-vertakkende
via `closed_term` — en VIER die weigeren. De alinea zegt er expliciet bij dat
(ii) en (iii) losse armen in losse functies zijn en dat een vouwSTAP (iii)
nooit bereikt: dat is precies waarom (iii) een eigen getuige nodig had.

### L2-F2 (middel) — Bouwnotitie §3 en §5 beschreven code die er niet meer is: GEFIXT

§3 stond in de tegenwoordige tijd maar was de stand van de eerste commit:

```
$ git diff --numstat main...HEAD -- backends/clingo/lib/translate.ml
87	8	backends/clingo/lib/translate.ml     # kop zei 61 / 8
```

Drie punten rechtgezet in §3 (kop 87/8; `init_reads_subject` heet
`init_is_subject_read`; het hek erkent ÉÉN bronvorm, niet ook een `MapList`),
plus een vierde die dezelfde soort was en niet in de bevinding stond: de
E1-rij van de "meting NA de fix"-tabel droeg nog de weigeringstekst van vóór
§L1-F4. Elke correctie draagt de verwijzing naar de fixronde die haar
veroorzaakte, zodat §3 leest als de geleverde stand en niet als een
momentopname.

§5 had dezelfde kwaal en is meegenomen: "drie faults-fixtures … 15 naar 18
checks" is de geleverde vier fixtures en 15 → 19 checks geworden, en de
S19-samenvatting daar telt nu zeven plekken in plaats van vijf.

### L2-F3 (laag) — deur 1 is geen `Construct`-ladingpositie: GEFIXT

Het bron-hek toetst `init_is_subject_read`, en dat is waar voor een KALE
`Attribuut_ref`: de deur vuurt op documenten zonder één enkele `Construct`.
De kaart wist dat zelf (E9 in §1), maar S19 zette de deur onder de kop "a
non-literal `Construct` payload meets …" en de NOTE noemde hem "one of four
constructor-payload positions". Beide teksten scheiden nu: zes plekken gaan
over de LADING, de zevende — het bron-hek — vuurt op de VORM van de init en
staat als vierde weigering apart beschreven, met de kale attribuutlezing
expliciet als voorbeeld.

### L2-F4 (laag) — route (2) was niet afgewogen: GEFIXT

De acceptatie eist de drie routes expliciet af te wegen; de Premissecorrectie
kondigde "route 1 + 3" aan zonder het argument tegen (2). Dat argument staat
er nu, met de vier metingen die het dragen: elke geweigerde vorm is
verifier-clean en `ref` speelt haar (E1 `PASS (2/3)`, E5 `PASS (2)`, E6 `PASS
(2)`, deur 4 `PASS (2)` op `Openstaand(8)`), dus een WF-nummer zou vormen
verbieden die RUNTIME-SEMANTICS definieert en de referentie-interpreter
uitrekent — de IR versmallen om één drager te beschrijven.

### L2-F5 (laag) — "alle twaalf gates groen" was voor vier poorten niet opgetekend: GEFIXT

De poorttabel stopte bij 8. Alle twaalf zijn nu zelf gedraaid, op de
fixronde-2-stand, en staan hieronder met hun letterlijke slotregel. Twee
verrassingen bij het meten: catala staat op deze machine wél in de
opam-switch, dus poort 9 kon echt draaien; en de tijdelijke pariteitszone is
door deze tak niet aangeraakt maar draait groen mee.

### L2-F6 (laag) — "DRIE deuren naar die ene kamer" was te ruim: GEFIXT

Punt 2 van de Premissecorrectie is gesplitst. ÉÉN deur is de subjectloze
kamer (de `closed_term`-route). De andere twee vuren ook zónder vouw en MÉT
subject in scope: `construct_ground_term` is een grondterm-beperking (E6, een
somtype-vergelijking) en het tweede hek in `expr_term` een
regelniveau-ladingbeperking (E5, een gewone schrijfregel). Het bewijs staat in
de kaart zelf: E4 viel door de grondterm-deur en wordt na 1a gedragen zonder
dat er één regel subjectmachinerie aan te pas komt.

### L2-F7 (laag) — "would be safe" was een gedragsbewering zonder artefact: GEFIXT

S19 zei over de afgebeelde lijst dat hij "would be safe" — een
gedragsbewering die gemeten was toen de `Map_list`-tak nog bestond en die na
§L1-F4 door geen enkele poort meer geraakt kan worden. De zin is
teruggebracht tot wat na te lezen is: `list_source` bouwt voor een afgebeelde
lijst de id `lm<N>(<binnenste>)`, dus over een eigen attribuutlijst
`lm(alist(S,"attr"))`, en dáárom raakt de botsing die deze weigering
beschrijft die vorm niet. Wat de drager doet, blijft: één bronvorm, want
niets vraagt meer.

### Poorten na fixronde 2 (alle twaalf zelf gedraaid)

| poort | slotregel |
|---|---|
| 1 self-contained | `check-self-contained: PASS` |
| 2 ir-schema | `check-ir-schema: PASS (schema valid; 171 file(s) validate and round-trip)` |
| 3 verifier | `check-verifier: PASS (171 golden clean; 206 negatief rejected for their declared rule)` |
| 4 case-schema | `check-case-schema: PASS (schema valid; 423 case file(s) validate)` |
| 5 oracle | `check-oracle: PASS (401 cases replayed exactly; NOT replayed, no case file yet: corpus/golden/jutterswet-vormenkabinet)` |
| 6 clingo | `test_clingo: 19 checks, 0 failures` + `check-clingo: PASS (401 cases replay exactly through clingo — ref = clingo over the corpus)` |
| 7 no-op | `check-noop: PASS (365 cases refute a do-nothing executor; 36 pinned survivors pin defeasible standing)` |
| 8 rood | `check-rood: PASS (22 red cases fault observably in ref and clingo)` |
| 9 catala | `check-catala: PASS — frontend suite green (201 checks); 12 catala golden(s) regenerate byte-identically` — de nieuwe golden draagt geen `bron.catala_en`, dus 12 blijft 12 |
| 10 regelspraak | `check-regelspraak: PASS — JUnit suite green (159 tests); 26 rgs golden(s) re-emit byte-identically` — de nieuwe golden draagt geen `bron.rgs`, dus 26 blijft 26 |
| 11 pariteitszone | `PASS` — door deze tak niet aangeraakt; ref 29/29, clingo 29/29 zonder vertaalfout, de derde executor 29 gespeeld |
| 12 drools | `check-drools: PASS`, `played 105`, `auto-excluded 42`, `JUnit 153 tests` |

Poort 12 laadt elke golden-`ir.json` van de wire, dus de tweede regel en de
derde instantie van fixronde 1 zijn daar meegereisd: `played` blijft 105 en de
nieuwe golden blijft in de reden-dragende weigeringsgroep (SumType), nu met
`auto-excluded 42` zoals §6 al voorzag.

Geen pin aangepast; het corpus kent nul verwijderde regels. Deze ronde raakte
uitsluitend tekst: `spec/ASP-TRANSLATION.md` (binnen S19), de NOTE van de
nieuwe golden, en deze kaart.
