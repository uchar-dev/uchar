# Verklaart de genoemde eigenaar het kenmerk ook?

Goal: beslissen — met een machinaal toetsbare uitkomst — of een
`Kenmerk`-node met een expliciete `eigenaar` óók moet worden gekruist op de
vraag of dát type het genoemde kenmerk **verklaart**, en, onlosmakelijk
daarmee, **welke eigenaar gezaghebbend is: het veld of het onderwerp**. WF-034
sluit vandaag alleen de eerste helft (de naam moet een verklaard objecttype
zijn); de tweede helft staat als een uitdrukkelijke stilte in de verifier
opgeschreven en is nooit beslist.

Deze kaart is afgesplitst van `20260906-gedurende-kwalificatie-stiltes` (geland
2026-09-10). Die kaart sloot de eigenaar op één soort positie — het lid van een
`gedurende`-clip, waar de lezer helemaal geen eigenaar raadpleegt (WF-097, dat
in de fixronde van 2026-09-10 alle drie de clips ging dekken) — en liet deze
helft uitdrukkelijk liggen, omdat hij een *semantische* vraag is en geen
resolutievraag.

## Wat er vandaag staat

`spec/verifier/lib/checks.ml`, de `Kenmerk_check`-wandeling, noteert de stilte
zelf. De eigenaar-arm kruist of de naam een verklaard objecttype is; de
declaratie-kruising eronder is gekeyd op **`subject`**, niet op `eigenaar`. Het
commentaar zegt met zoveel woorden wat dat niet sluit: of het genoemde type het
kenmerk verklaart — en dat het beantwoorden ervan meteen beslist welke eigenaar
gezaghebbend is.

De asymmetrie is echt: bij een expliciete `eigenaar` die het kenmerk *niet*
verklaart, kruist de verifier vandaag het kenmerk tegen het **onderwerp** en zegt
niets over het veld. Een bean-model-uitvoerder die de eigenaar wél als
aanhechtingspunt leest, leest daar dus een ander document dan de verifier.

## De meting (2026-09-10, op de eindkop van de moederkaart)

Over de drie zones (`corpus/golden`, `corpus/rood`, `corpus/negatief`):

    Kenmerk-nodes totaal                       213
    daarvan met een expliciete eigenaar          6   (alle zes in corpus/negatief)

De zes, met per stuk de stand van de open vraag:

| document | positie | eigenaar | kenmerk | type verklaard | verklaart het kenmerk |
| --- | --- | --- | --- | --- | --- |
| `wf031-dagsoort-leest-kenmerk` | `/resolvedDagsoorten/0/voorwaarde` | `Jutter` | `penninggerechtigd` | ja | **ja** |
| `wf034-kenmerkcheck-onbekende-eigenaar` | `/resolvedRegels/0/voorwaarde` | `Bestaatniet` | `gemeld` | nee | n.v.t. (WF-034 vangt hem) |
| `wf046-negated-read-in-recursive-cycle` | `/objectCreaties/0/voorwaarde` | `Persoon` | `volwassen` | ja | **ja** |
| `wf097-clip-kenmerk-eigenaar` | `/resolvedRegels/0/waarde/filter/onderdelen/0` | `Jutter` | `borgplichtig` | ja | **ja** (positie nu WF-097-verboden) |
| `wf097-prorata-kenmerk-eigenaar` | `/resolvedRegels/0/waarde/filter` | `Jutter` | `borgplichtig` | ja | **ja** (positie nu WF-097-verboden) |
| `wf097-dagentelling-kenmerk-eigenaar` | `/resolvedRegels/0/waarde/filter` | `Jutter` | `borgplichtig` | ja | **ja** (positie nu WF-097-verboden) |

De onderste drie zijn getuigen van de moederkaart zelf en staan op posities die
WF-097 inmiddels verbiedt; voor DEZE kaart tellen ze niet mee als vindplaats,
alleen als verklaring waarom het getal van 4 naar 6 liep. De scalaire posities
die er werkelijk toe doen zijn de bovenste drie, ongewijzigd sinds 2026-09-09.

**Er is geen enkele tegenvoorbeeld-getuige in het corpus** — geen document
waarin de genoemde eigenaar het kenmerk niet verklaart. Wie deze kaart doet,
maakt zijn eerste getuige zelf, en de kaart is daarmee expliciet een
*ontwerp*beslissing op een lege verzameling, niet het opruimen van gemeten
schade. Vaststellen dat de verzameling nog steeds leeg is, is het eerste werk.

De `wf031`-positie is de scherpste van de drie: in een dagsoort-voorwaarde kent
de wandeling **geen onderwerp**, dus daar is het veld de enige eigenaar die er
is. Elke regel die "het onderwerp is gezaghebbend" zou zeggen, moet die positie
apart beantwoorden.

## De vraag die beslist moet worden

Eén genummerde vraag, met drie mogelijke antwoorden, elk met een prijs:

1. **Het veld is gezaghebbend.** De declaratie-kruising verhuist van `subject`
   naar `eigenaar` waar die aanwezig is, en een eigenaar die het kenmerk niet
   verklaart is een fout. Prijs: dit verandert de *lezing* van bestaande
   documenten (het `wf031`-geval leest dan tegen `Jutter` in plaats van tegen
   niets), en de uitvoerders moeten mee — anders is het een verifier-only regel
   met een uitvoerder die iets anders doet.
2. **Het onderwerp is gezaghebbend, het veld is presentatie.** Dan wordt de
   eigenaar-arm van WF-034 een pure resolutiecontrole en moet ergens staan dat
   het veld verder *niets* betekent. Prijs: de `wf031`-positie heeft geen
   onderwerp, dus die lezing moet daar iets anders zeggen.
3. **Beide moeten het verklaren.** De strengste, en de enige die geen keuze
   maakt tussen twee autoriteiten. Prijs: over-afwijzing als er ooit een
   legitiem document komt waarin ze verschillen.

## Kernwaarschuwingen

1. **Dit is geen opruimklus.** Er is nul gemeten schade; er is een gemeten
   *stilte*. De beslissing moet op de semantiek rusten, niet op een
   corpustelling.
2. **Beweeg de uitvoerders mee of beweeg niets.** `ref/lib/eval.ml` en
   `backends/clingo/lib/translate.ml` matchen `eigenaar` vandaag weg met `_` op
   de scalaire kenmerkcheck (`Kenmerk_check { … eigenaar = _; … }`); de
   Drools-transpiler leest hem wél. Een verifier-regel zonder de uitvoerders
   erbij maakt precies de stilte die de moederkaart sloot.
3. **Het clip-lid is klaar en valt hierbuiten.** WF-097 weigert de eigenaar daar
   al; deze kaart gaat over de scalaire posities ernaast.

## Acceptance

- De genummerde vraag beantwoord, met de reden, en de prijs van het gekozen
  antwoord uitgeschreven.
- De meting hierboven **opnieuw geteld** (niet overgeschreven) en genoteerd.
- Per beslissing een artefact dat hem toetst: bij weigeren een
  `corpus/negatief/wf###-<slug>/`; bij een gewijzigde lezing een golden met
  absolute verwachtingen die **beide** uitvoerders halen.
- Rood eerst: de getuige faalt aantoonbaar vóór de wijziging.
- Alle dertien gates groen.

May touch: `spec/verifier/**` (**voorstel** — beschermd pad), `spec/IR-SPEC.md`
(**voorstel**), `ref/**`, `backends/clingo/**`, `backends/drools/**`,
`corpus/negatief/**`, `corpus/golden/**`, `backlog/`.

## Out of scope

- Het clip-lid: WF-097 (geland 2026-09-09 op de totaal-clip, 2026-09-10
  verbreed tot alle drie de clips) weigert daar zowel de eigenaar als de
  periode.
- De `periode` op de scalaire kenmerkcheck. Dat is de §8.2-familie, een eigen
  vraag met een eigen prijs (de RegelSpraak-frontend produceert die vorm uit de
  bron), uitdrukkelijk buiten de snit van de moederkaart gehouden.
- Het `eigenaar`-veld schrappen. Dat is de v1-delta die `CONSTRUCTS.md` ch. 2.2
  al draagt; deze kaart gaat over wat hij betekent zolang hij op de wire staat.
