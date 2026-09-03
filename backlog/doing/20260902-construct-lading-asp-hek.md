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

2. **De ware oorzaak is dat de vouw-init door de SUBJECTLOZE route
   loopt.** De fold-init wordt vertaald door `closed_term` — de
   gesloten-lichaam-grammatica, die per definitie geen subject heeft om
   een `val`-atoom uit te lezen. Élke subject-afhankelijke init strandt
   daar, `Construct` of niet. Er zijn DRIE deuren naar die ene kamer, elk
   met een andere tekst: `construct_ground_term` (de tekst die de Goal
   citeert), de catch-all van `closed_term` (gemeten met een kale
   `AttribuutRef`-init), en het tweede hek in `expr_term` (gemeten met
   `Construct(ParameterRef)` als gewone regelwaarde).

3. **"Underivability propagation" is NIET de ontbrekende machinerie.**
   Leeg propageert al correct en `ref` ≡ clingo op lege ladingen: een
   lege init derft geen basisfeit, dus de hele vouw is leeg — precies wat
   RUNTIME-SEMANTICS ch. 10.1/10.2 normatief voorziet. Die reden in de
   weigeringstekst was onwaar en is met deze kaart vervangen.

Maintainersbesluit na de meetfase: **route 1 + 3**. De
subject-gesleutelde vouw-init gaat de drager dragen (met een hek dat een
gemeten FOUT antwoord op de niet-gesleutelde bron afvangt), en de rest
wordt vastgelegd als gecorrigeerde S19-inventaris plus faults-fixtures.

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

### 3. Wat er is gebouwd (61 toegevoegde, 8 verwijderde regels in `translate.ml`)

**1a — `closed_term`'s `Construct`-arm delegeert recursief.** Een
niet-literale lading is zelf een gesloten-lichaam-term en lowert door
dezelfde functie; de bindingsatomen reizen mee naar buiten, zodat de
basisregel van de vouw ze in zijn lichaam krijgt. Draagt E4 (en een
rekencombinatie als lading).

**1c — het hek op de vouw-init.** Een init die het subject leest
(`init_reads_subject`: een `AttribuutRef`, of een `Construct` waarvan de
lading dat is) rijdt `expr_term` — maar alleen als de bron
subject-gesleuteld is (`source_subject_keyed`: een eigen attribuutlijst,
eventueel onder een `MapList`). Anders een luide weigering met de ware
reden. Draagt E8 en E9.

**De teksten.** `construct_ground_term` zegt niet langer
"underivability propagation" (onwaar) maar de ware reden: die route geeft
een term ZONDER bindingsatomen terug, dus een lading die gebonden moet
worden heeft er geen plek. De regelniveau-tekst noemt nu ook de
parameterlezing, die er feitelijk in valt.

Meting NA de fix, dezelfde zeven vormen:

| vorm | ASP-drager na |
|---|---|
| E0 | `clingo: PASS (2 expectations)` |
| E1 | `TRANSLATION FAULT: a subject-dependent Fold init over a list that is not subject-keyed is not translated (the accumulator is keyed by the list id alone, so two subjects' init facts collide on that one key and every subject reads every subject's outcome)` |
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

Drie faults-fixtures in `backends/clingo/test/test_clingo.ml`, één per
blijvende deur, elk assertend op de LETTERLIJKE weigeringstekst — het
overlever-patroon: een tekst die wegdrijft of een weigering die stil
verdwijnt laat de suite vallen. Bestaande checks onaangeroerd; de suite
gaat van **15 naar 18 checks**, en gate 6 draait haar met een
niet-nul-eis.

S19 (`spec/ASP-TRANSLATION.md`, voorstel) beschreef één van de drie
deuren en gaf hem een reden die niet de ware was. De alinea noemt nu vijf
plekken: twee die dragen (regelniveau-attribuutlezing; gesloten-lichaam,
recursief) en drie die weigeren, elk met zijn eigen ware reden, plus de
expliciete correctie dat leeg-propagatie NIET de reden is en de verwijzing
naar de fixtures die de weigeringen pinnen.

### 6. Poorten (exacte tellingen, basis = `c3a0eda`)

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
