# De gastheer van een kenmerkcheck: welk OBJECT?

Goal: beslissen — met een machinaal toetsbare uitkomst — wat een
kenmerkcheck met een `eigenaar` die NIET het type van het onderwerp is,
*betekent*: over welk object gaat hij. WF-100 (geland 2026-09-10) sloot de
declaratiehelft — de genoemde eigenaar moet het kenmerk verklaren. De
objecthelft staat open en wordt vandaag in twee van de drie uitvoerders met
een weigering beantwoord.

## Wat er vandaag staat (gemeten 2026-09-10)

De producent zegt het zelf. De RegelSpraak-frontend zet dit veld op precies
één vorm: de voorwaarde van een §13.4.7 feitcreatie die een kenmerk van het
DOEL noemt. De frontend bouwt die voorwaarde eerst met het ANKER als
onderwerp, krijgt een onopgeloste marker, hérbouwt hem met het doel als
onderwerp, en tagt het resultaat met `eigenaar = doel` — met als toelichting
dat de transpiler hem dan op het `$doel`-patroon plaatst "en nooit op de
anker, ook niet wanneer de anker een gelijknamig kenmerk verklaart".

De drie uitvoerders doen daar drie verschillende dingen mee:

| uitvoerder | eigenaar = onderwerptype | eigenaar ≠ onderwerptype |
| --- | --- | --- |
| `ref` | leest het onderwerp | **weigert** (sinds 2026-09-10, ch. 5.8b) |
| ASP-backend | leest het onderwerp | **weigert** (idem, bij vertaling) |
| Drools | leest het onderwerp | plaatst de constraint op het andere patroon — in een gewone regel is er geen tweede patroon, dus valt de regel weg als transpiler-skip; in een feitcreatie is er wél een tweede patroon en leest hij daar het DOEL-object |

De weigering in de twee OCaml-uitvoerders is een *kleiner* antwoord dan een
implementatie: ze zegt dat het document niet gelezen kan worden, niet wat het
betekent. Dat is bewust zo gelaten — het alternatief was de hosting-semantiek
implementeren, en dat is deze kaart.

## De uitgangsmeting (2026-09-10, op de producentvorm zelf)

Gebouwd uit de golden `jutterswet-postwacht`: feitcreatie met anker `Kustvak`
en doel `Sein`, het kenmerk `gehesen` op het DOEL gedeclareerd, guard
`eigenaar: "Sein"` — precies wat `RuleChecker.doelVoorwaarde` emitteert. Case:
S1 draagt `gehesen`, S2 niet.

| lezer | vóór WF-100 | ná WF-100 |
| --- | --- | --- |
| verifier | `WF-034 FAIL /feitCreaties/0/voorwaarde: 'gehesen' is not a declared characteristic of 'Kustvak'` | **PASS** |
| `ref` | antwoordt `vakseinaantal = 0` (leest de ANKER, die `gehesen` nooit draagt, dus vuurt de guard nooit) | **fault** (ch. 5.8b regel 2) |
| ASP-backend | idem, `0` | **fault** (bij vertaling) |
| Drools exec-model | speelt, `vakseinaantal = 1` | ongewijzigd: speelt, `1` |
| Drools DRL-tekst | speelt, `vakseinaantal = 1` | ongewijzigd: speelt, `1` |

Twee dingen die deze meting vaststelt en die de kaart hierboven aannam:

1. **De Drools-backend leest de eigenaar-INSTANTIES werkelijk.** De uitkomst 1
   volgt uit welke `Sein`-instanties `gehesen` dragen (één van twee) en niet uit
   de anker. Beide takken, dezelfde waarde. Het is dus geen aanhechtings-*hint*
   die toevallig hetzelfde antwoord geeft; het is een andere lezing.
2. **De stand van vandaag ÍS antwoord 3 hieronder**, ongekozen. Een
   verifier-schoon document dat één uitvoerder speelt en twee weigeren bestaat
   nu — WF-100 heeft die vorm conform gemaakt en de weigering heeft hem
   onspeelbaar gehouden. De weigering is een capaciteitsgrens van `ref` en de
   ASP-backend (één object per conditie), geen verkapte "eigenaar moet het
   onderwerptype zijn"-regel: de verifier zegt daar niets over en zou zo'n
   document toelaten.

Wie deze kaart doet begint dus niet bij nul: de vorm, de case en de vijf
metingen liggen er, en het werk is de keuze plus haar artefacten.

## De vraag

Wat betekent een kenmerkcheck met een vreemde eigenaar in een **feitcreatie**?
Drie mogelijke antwoorden, elk met een prijs:

1. **De check gaat over het doel-object.** `ref` en de ASP-backend evalueren de
   feitcreatie-voorwaarde dan per DOEL in plaats van eenmalig per anker — de
   voorwaarde filtert de doelen, precies wat het bonenmodel doet. Prijs: de
   feitcreatie-evaluatie in `ref` verandert van vorm (`run_feit_creatie`
   evalueert de voorwaarde vandaag vóór de doelen bekend zijn), en de
   ASP-vertaling moet de literal op de doelvariabele zetten in plaats van op
   het onderwerp.
2. **De verifier weigert de vorm ook.** De weigering van ch. 5.8b wordt een
   WF-regel: de eigenaar MOET het type van het omvattende onderwerp zijn, zonder
   uitzondering. Dan bestaat de producentvorm niet meer op de wire — de
   frontend-route wordt afgesneden en de frontend moet iets anders emitteren of
   de guard onopgelost laten. Prijs: precies dat afsnijden, plus de meting
   hoeveel bronnen de vorm produceren; en de reparatie die WF-100 net leverde
   (de vorm werd conform) wordt weer ingetrokken, nu op een andere grond.
   *Let op: een variant met een feitcreatie-UITZONDERING — de eigenaar mag het
   doeltype zijn zodra de vorm een feitcreatie-guard is — is GEEN antwoord 2.
   Die uitzondering laat de producentvorm juist toe, en omdat `ref` en de
   ASP-backend hem dan nog steeds niet kunnen lezen, valt hij samen met
   antwoord 3: verifier-schoon, één uitvoerder die speelt, twee die weigeren.
   Wie die variant wil, kiest 3 en schrijft de uitzondering op als de vorm
   waarin 3 geldt.*
3. **De vorm blijft geweigerd in `ref`/ASP en gedragen in Drools.** De stand
   van vandaag, expliciet als besluit vastgelegd. Prijs: een verifier-schoon
   document dat één uitvoerder speelt en twee weigeren, permanent — en het
   vierkant sluit daar dus nooit.

## Wat er eerst gemeten moet worden

- Hoeveel bronnen in `corpus/` en in de pariteitszone produceren deze vorm
  werkelijk? (Vandaag: **nul** in golden/rood/negatief buiten de getuigen die
  voor de regels zelf gebouwd zijn.) Zonder producent is (2) goedkoop en (1)
  een implementatie zonder gebruiker.
- Wat doet de pariteitszone? Gemeten 2026-09-10: 30 documenten, **9**
  kenmerkchecks, **0** met een eigenaar. De toolchain-gepinde ©-casus draagt de
  vorm dus evenmin. Dat maakt (2) goedkoop en haalt het pariteitsargument onder
  (1) weg — maar het is een momentopname van de zone zoals hij nu is, niet van
  wat de producent kán maken.
- Doet de Drools-backend het in de feitcreatie-route werkelijk goed, of is de
  anker→doel-terugval een toevalstreffer die op de meerstaps-tussenstappen
  breekt?

## Acceptance

- De genummerde vraag beantwoord, met de reden en de prijs uitgeschreven.
- De meting hierboven gedaan en genoteerd.
- Bij antwoord (1): een golden met absolute verwachtingen die alle drie de
  uitvoerders halen, en de weigering uit `ref`/ASP verwijderd in dezelfde
  commit als de vervanging (nooit een gat ertussen).
- Bij antwoord (2): een `corpus/negatief/wf###-<slug>/`, plus de meting dat
  geen bestaande bron de vorm produceert.
- Bij antwoord (3): de zin in `RUNTIME-SEMANTICS.md` ch. 5.8b die de
  permanente splitsing als besluit draagt, plus de reden-dragende regel in
  gate 12.
- Alle dertien gates groen.

May touch: `spec/IR-SPEC.md` (**voorstel**), `spec/RUNTIME-SEMANTICS.md`
(**voorstel**), `spec/verifier/**` (**voorstel**), `ref/**`,
`backends/clingo/**`, `backends/drools/**`, `corpus/**`, `backlog/`.

## Out of scope

- De declaratiehelft: WF-100 (geland 2026-09-10) sluit die.
- Het clip-lid: WF-097 weigert de eigenaar daar al.
- De `periode` op de scalaire kenmerkcheck — de §8.2-familie, eigen kaart.


## Meetrapport 2026-09-19 (read-only meetsessie, samen met de
feitcreatie-triagekaart; besluit ligt hierna bij de maintainer)

De volledige tabel is gemeten op bdea4ff4 met eigen verifier-schone
invoeren waarin het ANKER hetzelfde kenmerk draagt als het doel, zodat
de lezing positief onderscheiden is (anker-lezing 2, doel-lezing 1):

- Vorm A (doelkenmerk als feitcreatie-guard), uniek én herhaald paar:
  verifier PASS, frontend emitteert, ref/clingo FAULT, beide
  Drools-takken spelen de DOEL-lezing (1) — ook met twee tussenstappen
  (doel 3, niet tussenstap 1 of anker 4; de guard kent maar twee
  aanhechtingsplaatsen). Zelfgetypeerd is gedegenereerd (eigenaar-string
  onderscheidt anker en doel niet; alle vier lezen het anker) en de
  frontend heeft er geen route (Onopgelost-guard).
- Vorm B (doelvergelijking): bij anker ≠ doel bestaat GEEN
  verifier-schone instantie — structureel: de afgeleide lezing valt op
  WF-045 (cyclus) of WF-053 (ambigu). Antwoord (1) generaliseert dus
  niet naar de vergelijking zonder nieuw wire-veld. Beide Drools-takken
  spelen documenten die de verifier afwijst.
- Vorm C (vreemde eigenaar in een gewone regel): verifier PASS, geen
  frontend-route, ref/clingo FAULT, beide takken EXCLUDE.

Producentmeting bdea4ff4: 651 documenten, 264 kenmerkchecks, 10 met
eigenaar, 2 vreemd (beide al-afgewezen negatief-getuigen), 0 in enige
feitcreatie-voorwaarde, toka 9/0. Kosten gemeten: (1) is voor het kale
kenmerk ~6 regels per OCaml-uitvoerder (guard na de keten per doel;
owner_hosts stopt vanzelf met falen) maar generiek vergt het een
gastheerveld dat B10/B11/B13 niet plannen en dat exec voor samengestelde
guards niet kan dragen; (2) kost nul conforme documenten maar verplaatst
de breuk naar de Onopgelost-guard, waar sinds
`20260919-exec-stille-guardflanken` **beide Drools-takken weigeren** —
geen enkele uitvoerder antwoordt de vorm dan nog; (3) is één ch.-5.8b-zin + een rood-getuige met
NOFAULT-regel (gemeten haalbaar: ref/clingo voldoen aan expectedFault,
Drools levert het NOFAULT-token).

AANBEVELING van de meetsessie (maintainersprincipes gewogen): leg (3)
nu scherp vast — het veld is een gastheerhint die alleen een
meerpatroons-uitvoerder kan honoreren; conform maar niet herspeelbaar —
mét de drie ontbrekende zinnen (derde-type-rij, Onopgelost-guard,
zelfgetypeerde degeneratie) en één rood-getuige; en zet (1) apart als
latere GENERIEKE kaart. Kies (1) niet stuksgewijs voor het kenmerk
alleen; kies (2) niet zoals geformuleerd. V1-wisselwerking: dit is P7
en blokkeert B10.

Nieuwe metingen die aparte opvolging kregen én intussen gedicht zijn
(`20260919-exec-stille-guardflanken.md`, gemeten VÓÓR die kaart): exec
speelde STIL 0 op een Onopgelost-feitcreatie-guard (een Onopgelost
verdween — de gate-8-familie heeft op die positie nog geen getuige) en
een derde-type-eigenaar spleet de twee Drools-takken (exec inert →
geteld als gespeeld met 0, drl EXCLUDE); sinds die kaart weigeren
beide takken benoemd. Los daarvan gemeten:
`AttribuutRef.objecttype` met een vreemd type wordt door ALLE lezers
stil als navigatie over de afgeleide relatie gelezen — de wire geeft
daarmee twee tegengestelde antwoorden op dezelfde intuïtie; dat hoort
bij dit besluit op tafel.
