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
2. **De vorm blijft geweigerd, en de verifier weigert hem ook.** Dan wordt de
   weigering van ch. 5.8b een WF-regel (de eigenaar MOET het onderwerptype
   zijn, behalve in een feitcreatie waar hij het doeltype mag zijn), en de
   frontend-vorm wordt afgesneden. Prijs: dat snijdt een producentroute af —
   te meten hoeveel bronnen hem produceren.
3. **De vorm blijft geweigerd in `ref`/ASP en gedragen in Drools.** De stand
   van vandaag, expliciet als besluit vastgelegd. Prijs: een verifier-schoon
   document dat één uitvoerder speelt en twee weigeren, permanent.

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
