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
