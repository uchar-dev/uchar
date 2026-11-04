# runtime-semantics-ch3-rewrite: RUNTIME-SEMANTICS ch. 3 herschreven rond de lezing per dag

*Opgesteld door de hoofdsessie op 2026-11-03 uit de architectuurreview van Fable (`werk/review-fable-2026-11-03/advies.md`, §2 en advies 1).
Na `20261103-total-over-partial-unit`, de fixronde `time-followups`, `20261103-ref-trace-period-arithmetic` en
`20261103-expectation-canonical-timeline`: geen kaart vult ch. 3 nog aan vóór deze.*

## Aanleiding

Ch. 3 groeide per kaart: 38× "since 2026-", 30× "maintainer decision", 15× "Executed", Drools-codes en kaartnamen in de normatieve tekst; de
formule van het totaal staat in ch. 3.6 pas na twee "landed"-alinea's, ch. 3.12 is een statustabel van 35 rijen, ch. 3.1 beschrijft een
procedure van ref in proza, en er zijn tegenspraken: een afwezige tijdlijn telt in ch. 3.6 op de ene plaats 0 en op de andere leeg (besluit
12c: 0), ch. 3.9 noemt een expliciet leeg segment dat alleen Drools zou schrijven (verouderd), en CONSTRUCTS rij 2 en rij 11 zeggen het
tegengestelde over de precisiepoort op een tijdlijndoel. De schrijfgrens per segment staat drie keer.

## Goal

Ch. 3 als één definitie, rond het principe van `spec/proposals/IMPLICIT-TIME.md` §2, in de indeling van het advies: 3.1 de waarde (alle tijd,
open grens, `read`); 3.2 rasters en de kalender; 3.3 het principe (de scalaire regel per dag, de knip van elke producent tot de perioden van
zijn voorwaarde); 3.4 de producenten (rekenen, som, pro rata, dagentelling, kenmerk als voorwaarde), elk met één formule en zijn uitgewerkte
voorbeelden; 3.5 de collapses (totaal met de kalender, de kwalificator per emmer en als vouw); 3.6 schrijven (twee schrijvers, standing,
vacuüme waarheden en wat 0 betekent). De status, de geschiedenis, "executed by", kaartnamen en Drools-codes gaan naar CONSTRUCTS en naar
`ASP-TRANSLATION.md` en de README's; ch. 3.12 wordt een appendix van hoogstens tien open besluiten. De onbenoemde gevolgen uit het advies
(§1 punt 2 tot en met 5: vacuüme waarheden, een telling naast een tweede schrijver, "leeg schrijven" scalair tegen tijdlijn, de emmerconventie
van het eigen totaal, `bm` buiten de kalenderfamilies) krijgen een zin of een open besluit.

## Acceptance

- (a) Elk uitgewerkt voorbeeld en elke `WV-*`-verwijzing blijft, met dezelfde absolute waarden; gates 5, 6, 12, 13 en 17 onveranderd (geen
  semantiek beweegt). Een clausule die bij herschrijven een semantische vraag blijkt, gaat als vraag naar de hoofdsessie, niet stil.
- (b) Geen "since 2026-", geen "Executed by", geen kaartnaam en geen Drools-code in ch. 3; een telling van die woorden in het Result.
- (c) De drie tegenspraken gesloten; CONSTRUCTS rij 2, 11, 26, 28 en 29 herschreven als huidige stand, met één gedateerde regel per landing.
- (d) Eén schrijver, één stijl; een lezer van buiten kan uit ch. 3 de functies `read`, `totaal`, `dagentelling`, `pro rata`, de kwalificator en
  de knip afleiden zonder een ander hoofdstuk te lezen dan ch. 1.2 en 2.
- (e) `scripts/check-self-contained.sh` groen; `make gates` groen.

May touch: `spec/RUNTIME-SEMANTICS.md` ch. 3 (voorstel), `spec/CONSTRUCTS.md` (de rijen hierboven), `spec/ASP-TRANSLATION.md`,
`backends/*/README.md`, kaartverplaatsing.

Out of scope: elke semantische wijziging.

## Omvang

Middel tot groot (tekst).

## Result

*Schrijver: bouwer runtime-semantics-ch3-rewrite, 2026-11-04, branch `card/runtime-semantics-ch3-rewrite` op main
`504522cc6`. Niet gemerged, niet gepusht. Beschermd, dus voorstel: `spec/RUNTIME-SEMANTICS.md`.*

### Commits

- `f22a093ab` kaart runtime-semantics-ch3-rewrite naar doing
- `c97bafbe9` spec: RS ch. 3 herschreven rond de lezing per dag (voorstel)
- `4d68f0e56` constructs: rij 2, 11, 26, 28 en 29 als huidige stand
- `085dea46e` docs: uitvoerdersnoten uit RS ch. 3 naar ASP-TRANSLATION en de README
- de commit van dit Result

### De indeling, en waarom de nummers blijven (VB-1)

Buiten RS noemen 2306 regels een "ch. 3.x" (code, tests, catalogi, corpus-NOTE's, IR-SPEC, proposals; een deel is IR-SPEC
ch. 3), onder meer 420 in `backends/drools/src/main` en de catalogi (DROOLS-031 … -273), 234 in `ref/lib`, 357 in `corpus`. De
nummering van de kaart (3.6 = schrijven enz.) zou elk van die verwijzingen fout maken. **Voorlopig besluit VB-1:** de nummers
blijven; de zes delen van de kaart zijn de rol van elke sectie, als tabel aan het begin van ch. 3: de waarde 3.1; rasters en
kalender 3.2 (de kalender verhuisde uit 3.6 hierheen); het principe 3.3 (lezing per dag, `periods(c)`, de knip, de scalaire
contexten, de tijdpuntlezing); producenten 3.4, 3.5, 3.7, 3.8, 3.10; collapses 3.6, 3.9; schrijven 3.11; vouwen over instanties
3.4a–3.4d en 3.9a (zonder geschiedenis, op hun plaats); open besluiten 3.12 (tien rijen). Regels oud → nieuw:

| sectie | oud | nieuw | | sectie | oud | nieuw |
|---|---|---|---|---|---|---|
| inleiding | 12 | 29 | | 3.6 | 335 | 267 |
| 3.1 | 89 | 86 | | 3.7 | 66 | 45 |
| 3.2 | 157 | 165 | | 3.8 | 590 | 473 |
| 3.3 | 49 | 87 | | 3.9 | 332 | 261 |
| 3.4 | 122 | 121 | | 3.9a | 260 | 233 |
| 3.4a | 304 | 259 | | 3.10 | 204 | 162 |
| 3.4b | 98 | 70 | | 3.11 | 94 | 160 |
| 3.4c | 142 | 128 | | 3.12 | 47 | 24 |
| 3.4d | 92 | 91 | | **ch. 3** | **3061** | **2725** |
| 3.5 | 67 | 63 | | | | |

### Telling (b), oud → nieuw in ch. 3

`since 2026-`/`Since 2026-` 31 → 0; `Executed`/`executed` 45 → 0 (waarvan `executed by` 13 → 0); `maintainer decision(s)` 29 →
0; `landed` 8 → 0; `OPEN` 23 → 0; `Measured` als meetverslag 9 → 0 (de vier resterende "measured" zijn "gemeten in eenheden");
`Until/since L1–L6` 23 → 0; data 2026-08 t/m 2026-11 88 → 3 (de drie zijn het voorbeeld "jaar uit 2026-11-03"); kaartnamen 12 →
0 (`conditions-per-period` 2, `drools-venster-vouwtoets` 5, `date-with-in-ref-and-clingo`, `drools-datumstap-buiten-bereik`,
`heelperiode-forall`, `subselection-reads-subject`, `timeline-arithmetic-per-period`); DROOLS-### 8 → 0; CLINGO-### 2 → 0; RGS-###
2 → 0; "Drools" 54 → 0; "clingo/ASP backend/ASP translation" 72 → 0. Woorden die toevallig een kaartnaam zijn ("datum-tijd",
"per-element", "states") en de fixturemap `ref/test/fixtures/conditions-per-period/` (4×, een pad) staan er nog. Gemeten met een
eigen script over de tekst vóór en na (niet gecommit).

### Voorbeelden

Elke `WV-*`-verwijzing van het oude ch. 3 staat in het nieuwe (55 van 55; nieuw verwezen: `WV-5.7-per-dag`). Elke datarij van een
tabel met uitgewerkte waarden staat er letterlijk (gemeten: van de 139 tabelregels zijn 44 niet letterlijk overgenomen, alle uit
de twee statustabellen — ch. 3.10 "per wire position" en ch. 3.12 — plus twee kopregels: "true-periods" → "true periods",
"per-member verdict" → "verdict per member"). Elke vetgedrukte uitkomst blijft (177 → 186 vette getallen; geen getal minder).
Verplaatst: `WV-3.7-schrijfgrens-maand/-jaar/-onbegrensd` ch. 3.7 → ch. 3.11 (de schrijfgrens per segment op één plaats); de
voorbeelden van de knip (`jutterswet-getijdenwacht` 71/66/0/0, J4 10) en van de samengestelde knip (`jutterswet-dubbelwacht`
30/0/0/30) van de kop van ch. 3.6 naar de voorbeeldenlijst van ch. 3.6. Nieuw aangehaald, met waarden die de golden al pint:
`jutterswet-vast-startpunt` case 4 (stormpremie **50**, maandpremie **leeg**) in ch. 3.11 als voorbeeld van "geen vacuüme
waarheid". Verwijzingen naar voorbeelden uit ch. 3.3: `WV-3.4-domein`, `WV-3.4-per-dag`, `WV-3.10-voorwaarde-per-periode`.

### Bijgewerkte verwijzingen

- Nummering behouden, dus geen verwijzing in code, catalogi, tests of corpus aangepast (0).
- RS ch. 2.4 en 2.8: "(ch. 3.7)" → "(ch. 3.11)" voor de schrijfgrens per segment (2); Drools-README "(…, ch. 3.12)" → "(…, ch. 3.9)"
  voor de binnenste ontkenning, die specificatie is en geen open besluit (1).
- Namen die van buiten worden aangehaald, staan letterlijk in de nieuwe tekst: "the clip generalised" (12× van buiten), "the
  calendar" in ch. 3.6, "the per-element fold", "This qualifier is not a scalar context", "Per bucket on a timeline target",
  "What this does NOT give", "the point-in-time read", "the narrow route", "Clamp, not inverse", "Date construction",
  "The reach fence (normative)", "Fixed starting points", "Once or per member", "The form anchor", "Two spellings".

### Gesloten tegenspraken

1. *Een afwezige tijdlijn telt 0 overal.* Ch. 3.6 zegt het voor elke route; de zin van ch. 3.10 "the difference between an absent
   and a segmentless timeline (ch. 3.6) stays where it was" is weg; ch. 3.12 heeft geen rij meer.
2. *Ch. 3.9 "Only the Drools backend writes such a segment"* is weg: of een producent een expliciet leeg segment schrijft, is
   representatie (ch. 3.1), en een case draagt er geen.
3. *CONSTRUCTS rij 2 tegen rij 11*: rij 2 zegt nu dat een tijdlijndoel per segment in de precisiepoort staat, het tijdsevenredig
   deel uitgezonderd, zoals rij 11 en RS ch. 2.4 punt 4. Dezelfde verouderde zin stond in RS ch. 2.8 ("leaves a timeline target
   alone: the part is the one computed write such a target takes") en is daar ook rechtgezet (VB-4).
4. *De schrijfgrens per segment* staat op één plaats, ch. 3.11, met haar voorbeeld; ch. 2.4 (een zin), 2.8 en 3.7 verwijzen ernaar.

### De onbenoemde gevolgen (advies §1 punt 2–5)

- Vacuüme waarheden bij een geschreven telling: ch. 3.11 "What 0 means", punt 1, met `jutterswet-vast-startpunt` case 4.
- Een telling naast een tweede schrijver: ch. 3.11 "What 0 means", punt 3, met `WV-5.7-per-dag`.
- "Leeg schrijven" scalair tegen tijdlijn: ch. 3.11 "Leeg in a write" (drie regels naast elkaar) en open besluit ch. 3.12 rij 10.
- De emmerconventie van het eigen totaal: ch. 3.6 (welke tijdlijn het eigen totaal leest) en open besluit rij 9.
- `bm` buiten de kalenderfamilies: ch. 3.2 "The calendar" en open besluit rij 2.

### Tekstvoorstellen van `exact-anchor-30-360`

Verwerkt: ch. 3.8 (de afgeronde zustergolden draagt de dagsoorttoets en de eenheid, het anker de exacte maandrente), ch. 3.10
(formulering A op beide), de drie rijen van ch. 3.12 als status in CONSTRUCTS rij 11, en CONSTRUCTS rij 2 (DROOLS-312,
CLINGO-033) en rij 11 (141 2/3 € en 72 2/9 €, CLINGO-033/-034, DROOLS-312, EF-020). Niet verwerkt, buiten deze kaart: de rij EF-020
van RS ch. 5.7b (getuige `corpus/red/ever-unique-temporal-key-bucket-scalar` erbij); de ch. 2.4-zin over DROOLS-312 stond er al.

### CONSTRUCTS

Rij 2, 11, 26, 28 en 29 beschrijven de huidige stand (de wire, de semantiek, wat elke uitvoerder draagt en bij naam weigert, de
getuigen, de gevolgde v1-delta's), met eronder per landing één gedateerde regel, in de cel gescheiden door `<br>` (VB-5; geen
script leest CONSTRUCTS). Rij 11: 18120 → 10548 tekens; rij 29: 7223 → 5328; rij 26: 6685 → 4756; rij 28: 5471 → 4675; rij 2:
5308 → 5439 (de oude rij noemde de huidige vorm van de regelnode niet). Rij 11 krijgt ook een regel voor deze kaart (datum
2026-11-04; de hoofdsessie zet de landingsdatum).

### Voorlopige besluiten

- VB-1 — nummering behouden; de indeling van de kaart als rol per sectie (hierboven).
- VB-2 — de tien rijen van ch. 3.12 zijn gekozen zodat elke verwijzing van buiten naar "ch. 3.12" (OPEN multi-period, kalender-
  emmers dag/week/kwartaal, de vensterrijen van de pachtvenster- en wachtvenster-NOTE, DROOLS-244/-246/-262/-203, EF-022) en elke
  `declined`-soort met ch. 3 (EF-021, EF-022, EF-023; ch. 5.7b noemt ch. 3.12 voor `declined`) er een plaats vindt. Uitvoerdersstatus
  staat in CONSTRUCTS 11/29, ASP-TRANSLATION en de README's; ch. 3.12 wijst daarheen.
- VB-3 — 3.4a–3.4d en 3.9a houden hun nummer en inhoud, zonder "until L2"-geschiedenis, datums en uitvoerdersmetingen.
- VB-4 — RS ch. 2.4 (één zin) en ch. 2.8 (twee zinsdelen) aangepast, buiten de may-touch van de kaart, omdat "de schrijfgrens op één
  plaats" en de tegenspraak van rij 2/11 anders in ch. 2 blijven staan. Kan de hoofdsessie terugdraaien zonder ch. 3 te raken.
- VB-5 — één gedateerde regel per landing met `<br>` in de tabelcel.
- VB-6 — `spec/proposals/IMPLICIT-TIME.md` wordt één keer in de inleiding genoemd als grond, en in ch. 3.10 (formulering A) en
  ch. 3.12 (punt 8b, punt 9) waar een besluit daar de bron van een open rij is.
- VB-7 — ch. 3.9 noemde voor het geleverd-als-perioden-zijn van een kenmerk "the ch. 8.2 apartness"; die staat in ch. 8.4. Nu
  "(ch. 8.4)".
- VB-8 — ch. 3.4c noemt de weigering van de gevulde telling over een tijdsafhankelijk attribuut nu met haar soort, EF-021
  (`declined`, ch. 5.7b noemt ch. 3.4c); de soort bestond al.

### Vragen aan de hoofdsessie

1. *De macht over een tijdlijn.* Ch. 3.4 sluit de macht uit, ch. 3.3 leest elke operator zonder eigen tijdsemantiek per dag, en
   geen tekst noemde haar OPEN (oud ch. 3.12: "The power stays refused"). Ik schrijf in ch. 3.3 dat de macht als enige niet per dag
   gelezen wordt en zet haar in ch. 3.12 rij 4. Per dag (het principe), of een open besluit?
2. *IMPLICIT-TIME punt 9* ("besloten: de specificatie": minimum, maximum, eerste en laatste per periode, en de weigering van een
   rangschikkende vouw over een lid vervalt omdat er per periode een moment is) staat niet in RS: ch. 3.5 (v0-noot), ch. 3.4c, ch. 3.6
   (de per-element-vouw) en ch. 3.12 rij 4 houden de weigering, en ik heb dat zo gelaten. Inschrijven of open laten?
3. *Eén tegen twee schrijvers op een tijdlijndoel.* Met één schrijver vervangt de geschreven tijdlijn het doel met haar lege dagen
   (de invoer verdwijnt daar, ch. 5.7 "On a target of one writer …"), met twee laat een lege dag de invoer staan (ch. 5.7 "Two
   writers per day"); ch. 7.8 voegt de staande schrijver toe. Een tweede schrijver verandert dus de lezing van een dag die geen van
   beide beslist. Bedoeld? (ch. 3.11 "Leeg in a write", ch. 3.12 rij 10.)
4. *Een tijdsafhankelijke parameter als operand van de leeg/gevuld-toets* onder de kwalificator: oud ch. 3.9 zei "passes WF-043 and
   is folded by no executor"; ik noem hem als operand (de formule leest elke tijdlijn) en zet de weigering van alle drie in
   CONSTRUCTS rij 29. Akkoord, of hoort hij in ch. 3.12?
5. *Een kenmerkdoel zonder venster.* v0 verlaagt een conjunctieve of minstens-één-samenstelling van eigen kenmerken zonder venster
   (doorsnede, vereniging); RS definieert het schrijven alleen binnen een venster en de uitvoerders weigeren de rest. Ik noteer dat als
   v0-noot in ch. 3.11 en als open in rij 7. Gevolgde afwijking (CONSTRUCTS-delta) of gat?

### Gezien, buiten deze kaart, niet aangeraakt

- Verouderde verwijzingen naar oud ch. 3.12 of oude stand: `frontends/regelspraak/grammar/DIVERGENCES.md` r. 1281 (WF-111 "OPEN wire
  decision", sinds de verruiming niet meer waar); `spec/IR-SPEC.md` r. 6750; de testcommentaren "the 'own and navigated' of the
  ch. 3.12 delta line" (`ref/test/test_ref.ml` r. 7354, `backends/clingo/test/test_clingo.ml` r. 3582; nu CONSTRUCTS rij 29); de
  catalogusregel DROOLS-242 "(RUNTIME-SEMANTICS ch. 3.9, 3.12)" (gepind in `TmsWholePeriodWitnessTest`; ch. 3.9 klopt, ch. 3.12 wijst
  nu naar de uitvoerdersdocumenten).
- `corpus/negative/wf023-precision-time-proportional-part/NOTE.md`: "A target with a timeline stands outside the gate" — dezelfde
  verouderde zin als rij 2.
- RS ch. 10.2 (r. 8865) en CONSTRUCTS rij 1 noemen "ch. 3.5a", die niet bestaat (waarschijnlijk 3.4a).
- RS ch. 2.4 punt 4 zet de pro rata buiten de poort met een uitvoerdersreden (advies §1 punt 7); niet veranderd.
- RS ch. 5.7 `WV-5.7-per-dag` noemt 15, 30 en 12, niet de 0 vóór 2025-11-01 en vanaf 2026-04-01 die schrijver 2 als dagentelling
  over alle tijd schrijft (IMPLICIT-TIME punt 5 noemt ze wel).

### Gates

`scripts/check-self-contained.sh` PASS. `make gates` groen op `085dea46e` (log
`werk/runtime-semantics-ch3-rewrite/logs/gates-1.log`, `out/gates/20261104-095759/`): drie bouwstappen, 17 gates en `after-run`
rc=0, 410 s, red=0. Dezelfde tellingen als op main: gate 5 `ref suite 2340 … 1062 cases replayed exactly`; gate 6 `2474 checks …
1058 cases … 2 golden(s) in the refusal channel`; gate 12 `JUnit 1840 tests`, `played matches the pinned 328`, `DRL-text branch
played 328`; gate 13 `226 executor replays … 0 pinned survivors`; gate 17 `7352 readings, 24006 node lines`. Geen semantiek bewoog.
