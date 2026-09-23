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

## Result

Uitgevoerd op de branch `card/anker-ww-herleving-dragervoorwaarde`, in
opdracht van de hoofdsessie (fixronde 4 van de ankerkaart; één bewerker van
het voorstel, geen conflicten). Beschermd pad `spec/proposals/`: de branch
blijft als **voorstel** staan, en de maintainer merget hem in zijn geheel.
Commits: claim, `voorstel niet-en-nesting: de drie besluiten van
2026-09-23 als besloten`, `dialectkaarten: de verwijzing naar het
gelijktrekken vervalt`, en deze. De hashes staan in het Result van de
ankerkaart, na de laatste rebase.

**(a) Het voorstel draagt de drie besluiten als besloten**, met datum en
bron: de besluitvraag van 2026-09-23 en het rapport buiten de repo, §8.2 en
§9. De eerste voorstellen staan als verworpen alternatief.
- *Kop:* "The decisions are taken (2026-09-23, §10)", met de drie punten.
- *§1-tabel:* de kolom heet "Decided"; rij 2 (de basis toegestaan, met de
  waarschuwing) en rij 4 (beperkend met erven, ook aan de wortel;
  `Afwijking van`) noemen elk hun eerste voorstel; rij 7 noemt de eigen
  kaart.
- *§2.3:* besluit 4(d) en de bevestiging van de ankerconjunctie.
- *§4.2, §4.4 en §4.5 (besluit 2):* de basis staat bij de toegelaten
  plaatsen, met een "Warned"-regel voor een groep die nooit iets afleidt;
  de weigering en de basisregelmelding gaan eruit, en de vlakke-`Regel`
  melding noemt de basis als plaats.
- *§5.2:* de conjunctie op de wire, en `Afwijking van <basis>` als
  ketenpositie met eigen voorwaarde (bergingsdrempel byte-identiek). Of
  `Afwijking van` ook een uitzondering als drager mag noemen, laat de sectie
  aan de bouwkaart.
- *§5.4:* optie (D) — beperkende `Uitzondering op` met `Afwijking van` —
  is besloten, met de redenen van het besluit. (A) (het eerste voorstel),
  (B) en (C) zijn verworpen. Eén [I]-zin erbij: (D) aanvaardt wat (B)
  verweten wordt, een bron die minder zegt dan de wire, maar op elk niveau
  gelijk.
- *§5.7:* de koptokens van de bouwkaart (`AFWIJKING_VAN`, `UITZONDERINGEN
  ONGEORDEND OP`) in plaats van "over existing tokens"; de lint van (A)
  wordt niet gebouwd.
- *§7.2:* de twee lint-kandidaten zijn beslist: de waarschuwing (besluit 2)
  en de conjunctie door het frontend (besluit 4d).
- *§7.4, de F-lijst:* F2 en F7 zijn ingetrokken, met hun eerste voorstel
  erbij. F9 komt erbij (waarschuwing: een groep die nooit iets afleidt) en
  F10 (`Afwijking van` op een onbekende drager, een getuige die de
  tweelingkaart al noemt). De bouwkaart kiest de fragmenten.
- *§8.1:* de ankerconjunctie als bevestigd; het gevolg voor G2 in de
  besloten spelling.
- *§8.2:* de "kies er geen"-passage uit fixronde 2 is weg. De bron staat in
  de besloten spelling: elk blok schrijft alleen zijn eigen voorwaarde. Het
  uitschrijven per uitzondering is verworpen alternatief.
- *§8.3:* de bron van G3 in de besloten spelling (eigen voorwaarden; de
  wire houdt de cumulatieve voorwaarden van het anker).
- *§10:* heet "Decisions". Punten 1, 3, 4(a)–(c), 5, 6 en 8 zijn besloten
  zoals voorgesteld (vergeleken met de bouwkaarten); punt 7 als eigen kaart
  `kenmerkinvoer-en-invoer-boven-berekening`, die haar eigen opties weegt;
  punten 2 en 4(d) anders, met reden en verworpen eerste voorstel.
- *§12:* 4(d) is besloten.
- Gecontroleerd met grep: "never in a base", "awaits", "proposed —", "both
  spellings" en "chooses neither" komen niet meer voor. Elke resterende (A)
  of F2/F7 staat als verworpen of ingetrokken.

**(b) De bouwkaarten en het voorstel spreken elkaar niet meer tegen.** In
`dialect-niet-in-uitzondering` en `dialect-uitzondering-op-uitzondering`
zijn de verwijzingszinnen weg: de inleidende zin "waar het voorstel … anders
luidt, wordt het voorstel gelijkgetrokken (kaart …)" en de zin "gaat mee via
de kaart voorstel-niet-en-nesting-besluiten". Beide zeiden hetzelfde en
klopten na (a) niet meer. Verder is aan die kaarten niets veranderd. Wel
blijft staan: acceptatie (b) van de tweelingkaart noemt nog "zeven cases";
dat worden er tien (open punt 2 van de ankerkaart, werk voor de
hoofdsessie).

**(c) Geen code.** Gate 1 PASS op de getrackte set. De gates van de
ankerkaart zijn na de laatste rebase opnieuw gedraaid (zie het Result
daar).

**Registerzin** (voor de hoofdsessie, `spec/CONSTRUCTS.md` rij 8, in plaats
van "two of them defer to the landed proposal text on the base-rule
negation and on the carrier-condition conjunction (`Afwijking van`), still
to be settled in one place before the dialect cards start"):

> two of them differed from the landed proposal text, which now records
> them as decided (card `voorstel-niet-en-nesting-besluiten`,
> 2026-09-23): a negated base of an `Uitzondering op` chain is admitted —
> it decides `onwaar` and writes nothing, and a group that never derives
> anything draws a warning — and `Uitzondering op X` is restrictive,
> inheriting X's already conjoined justification at every level including
> the first, flattened into one `ALLE` with the carrier's conjuncts first,
> while a widening deviation is written `Afwijking van X` with its own
> condition only (`jutterswet-bergingsdrempel` becomes one, its `ir.json`
> byte-identical); the anchor's nested justifications carry the whole
> chain below them, `werkloos` included (confirmed the same day).

**Open punten.**
1. Besluit 4(d) spreekt over `Uitzondering op`. Of de leden van
   `Uitzonderingen ongeordend op X:` ook de voorwaarde van X erven, is niet
   besloten. Het anker `jutterswet-nestkenmerk` (G4) draagt zijn leden
   zonder `nx`, en zijn drie cases onderscheiden de lezingen niet.
   Gemeten: met `nz` en zonder `nx` antwoordt het anker "aanwezig" in ref
   (`oracle: PASS (2 expectations)`), clingo (`clingo: PASS (2
   expectations)`) en de exec-tak (`PLAYED 1`); een tweeling die `nx` erft,
   zou daar "afwezig" geven. Het voorstel §8.3 noemt dit als open vraag.
   Een besluit van de maintainer, vóór de tweelingkaart G4 bouwt.
2. Of `Afwijking van` ook een uitzondering als drager mag noemen, is niet
   besloten; §5.2 laat het aan de bouwkaart.
