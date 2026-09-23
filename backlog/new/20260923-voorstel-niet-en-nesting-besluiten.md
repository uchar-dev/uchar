# voorstel-niet-en-nesting-besluiten: het gelande voorstel gelijktrekken met de besluiten van de maintainer

Het voorstel `spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md` landde
op 2026-09-23 (`9c0afba5`, `462640bb`) met in §1 en §10 *voorgestelde*
antwoorden op de besluiten 1–8 van de onderzoekskaart. De maintainer nam
dezelfde dag de besluiten langs het onderzoeksrapport buiten de repo
(`../research/dialect-defaults-niet-en-nesting/onderzoek-niet-in-uitzonderingen.md`,
§9) en bevestigde drie punten in een besluitvraag. Op twee daarvan wijkt de
proposaltekst af; op het derde zwijgt zij. De bouwkaarten
(dialect-niet-in-uitzondering, dialect-uitzondering-op-uitzondering,
anker-ww-herleving-dragervoorwaarde) dragen de besluiten al; het voorstel
moet mee, zodat er één norm is. Beschermd pad (`spec/proposals/`):
voorstel-branch, de maintainer merget.

## De besluiten (2026-09-23)

1. **Besluit 2 — `niet` in de basisregel van een `Uitzondering op`-keten:
   toegestaan.** Onder de gesloten wereld beslist zo'n basis `onwaar` en
   schrijft niets, dus de uitkomst verandert niet; de winst is dat de
   auteur de wet kan naschrijven ("herleeft niet, tenzij …") en dat de
   uitzonderingen een benoemde basis hebben zonder een verzonnen positieve
   regel (Catala's `definition x equals false`). Het voorstel zegt
   "never in a base"; die zin en de bijbehorende weigering F-x vervallen,
   de waarschuwing voor een groep die nooit iets afleidt blijft.
2. **Besluit 4d — `Uitzondering op X` is beperkend, met erven van de
   dragervoorwaarde, en `Afwijking van X` voor verruimende afwijkingen.**
   Het frontend conjungeert de (zelf al geconjungeerde) rechtvaardiging van
   X met de eigen voorwaarde van de uitzondering, ook aan de wortel; de
   wire draagt zelfstandige rechtvaardigingen (ch. 7.1). Een uitzondering
   die de basis verruimt krijgt de kop `Afwijking van` met onafhankelijke
   voorwaarde (`jutterswet-bergingsdrempel` wordt zo'n afwijking). Het
   voorstel kiest zelfstandig geschreven voorwaarden met waarschuwing F7;
   §5.4, §10 en de F-lijst gaan mee. Motivering: rapport §8.2 en §9 (ch. 7.1
   schrijft de conjunctie voor; geen formalisme laat de uitzondering de
   voorwaarde erven, dus zij moet ergens staan; de juridische betekenis van
   een uitzondering is beperkend; hoek B van het anker is de stille fout
   van de andere keuze).
3. **Het anker: de geneste rechtvaardigingen dragen de hele keten
   eronder, ook `werkloos` van art. 15.** De sessie die de ankerkaart
   uitvoert vond dat zonder die conjunct de herleving `waar` beslist voor
   iemand bij wie nooit een recht ontstond (haar case8: niet werkloos,
   boven de grens, vervallen → moet afwezig zijn) en dat de oude boom over
   de zestien combinaties van werkloos, eindiging, vervallen en nieuw recht
   op precies vier plekken afwijkt. Bevestigd door de maintainer; het
   voorstel §2.3 en §8.1 noemen alleen de directe drager en gaan mee.

## Acceptatie (bij oppakken aanscherpen)

- (a) §1 (de tabel), §5.4, §8.1, §10 en de weigeringslijst van het voorstel
  dragen de drie besluiten hierboven als **besloten** (datum, bron), niet
  meer als voorgesteld; de sporen van de andere keuze zijn weg of als
  verworpen alternatief benoemd.
- (b) De bouwkaarten en het voorstel spreken elkaar nergens meer tegen
  (controle: de zinnen "gaat mee via de kaart voorstel-niet-en-nesting-
  besluiten" in de twee dialectkaarten kunnen dan vervallen).
- (c) Geen code; alle gates ongewijzigd groen (gate 1 op de getrackte set
  volstaat voor prosa).

May touch: `spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md`
(**voorstel**), de twee dialectkaarten (alleen de verwijzingszinnen),
kaartverplaatsing.

Niet in deze kaart: de uitvoering van welke dialectvorm dan ook.
