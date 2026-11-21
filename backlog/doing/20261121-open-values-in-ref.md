# open-values-in-ref: ref rekent een document dat uit leeg niets afleidt

*Opgesteld door de hoofdsessie op 2026-11-21, na het besluit van de eigenaar van die dag voor de lezing met drie
standen. Het ontwerp per vorm, met uitgewerkte voorbeelden en de metingen, staat in `spec/proposals/STRICT-EMPTY.md`;
de schakelaar staat op `main` (`inferFromEmpty: false` op de wortel, `Afleiden uit leeg: nee` in de bron, en ref,
clingo en Drools weigeren zo'n document met REF-014, CLINGO-049 en DROOLS-370).*

## Aanleiding

Een regelset kan kiezen dat uit een lege waarde niets volgt. De eigenaar koos de lezing waarin een waarde gevuld,
vaststaand leeg of open is:

- **open**: het staat niet vast. Een invoergegeven dat de case niet geeft en dat de regelset niet als volledig
  behandelt; en elk lid dat een vorm zou schrijven of toekennen terwijl haar voorwaarde onbepaald is of haar waarde open.
- **vaststaand leeg**: er staat vast dat er geen waarde is. Een afgeleid attribuut waarvoor geen schrijver geldt, een
  lezing over een link die er niet is, een opgave die niet gegeven is.
- Wat vaststaat, rekent zoals RUNTIME-SEMANTICS ch. 1.2–1.4 het nu vastlegt. Wat een open waarde leest, is zelf open;
  een voorwaarde die een open waarde leest is onbepaald, tenzij een ander deel haar beslist (de sterke tabel van
  Kleene).

Een voorbeeld uit de Jutterswet: "Een vondst is kostbaar indien zijn taxatie groter is dan 1000", "Het vindloon van een
vondst is 10 indien hij kostbaar is", "De uitbetaling is zijn vindloon plus zijn bergloon". Taxatie 50: niet kostbaar,
het vindloon is vaststaand leeg, de uitbetaling is het bergloon. Taxatie leeg: kostbaar is onbepaald, vindloon en
uitbetaling zijn open, en een regel "indien de vondst niet kostbaar is" vuurt niet.

De lezing is gehouden aan twee eisen: **monotonie** (een gegeven invullen kan een uitkomst toevoegen, maar nooit een
uitkomst veranderen die er stond; de sluitingen die een auteur schrijft zijn de benoemde uitzondering) en **behoud bij
volledige kennis** (is niets open, dan is de uitkomst die van de gewone lezing).

## Goal

De referentie-interpreter rekent een document met `inferFromEmpty: false` volgens deze lezing, RUNTIME-SEMANTICS
beschrijft haar met uitgewerkte voorbeelden die de tests van ref zijn, het case-formaat kan "staat vast dat niet" en
"open" zeggen, en strikte goldens met handberekende verwachtingen spelen in ref. clingo en Drools weigeren die goldens
bij naam tot hun eigen kaart.

## De architectuur (bindend)

**Eén lezing, één evaluator.** De gewone lezing is het geval waarin niets open is: behoud bij volledige kennis is hier
geen toets achteraf maar de opbouw. `inferFromEmpty: false` verandert alleen welke afwezige invoer open is. Daaruit
volgt:

- Geen tweede evaluator, geen kopie van `eval_expr` of `eval_cond`, geen strikte varianten van operatoren of vouwen. Een
  uitdrukking waarvan alle lezingen vaststaan, wordt door de bestaande code gerekend.
- `Uchar_ref.Value.t` krijgt geen constructor erbij (clingo leest dat type).
- Een document zonder de eigenschap geeft byte voor byte dezelfde uitkomsten, lezingen en eindstand als nu: gate 5, 7,
  8, 13 en 17 blijven groen zonder dat een verwachting, pin of lijst verandert.
- De eigenschap wordt op één plaats gelezen (welke afwezige invoer is open); nergens anders staat een tak op de
  eigenschap.

**Aanbevolen opzet** (afwijken mag, met de reden in het Result, zolang het bovenstaande blijft staan):

1. *Open als uitkomst van een vorm.* De wereld houdt per vorm en onderwerp al een uitkomst bij (de deponeringen, of de
   fout). Daar komt bij welke doelen die evaluatie open liet. Een lid is open als de case het niet geeft en het open
   invoer is, of als een uitkomst die staat het open liet. Een lezing zonder waarde en zonder merkteken is vaststaand
   leeg.
2. *Een open lezing breekt de evaluatie af.* Een lezing van een open lid werpt een eigen exceptie, die wordt gevangen
   op de grens van wat samen één oordeel of één waarde is: een atomaire voorwaarde (dan onbepaald), de waarde van een
   vorm (dan is het doel open), de voorwaarde van een `IfThenElse`, het filter of de waarde van een lid van een vouw.
   Wat een afgebroken evaluatie tot dan toe deponeerde, vervalt. Zo raakt de lezing geen operator.
3. *Het oordeel van een voorwaarde is een paar*: wat voor de vorm zelf vaststaat, en wat voor een andere vorm vaststaat.
   Ze verschillen op één plaats: een toets die zegt dat een open X leeg is (direct, of als de ontkenning van "is
   gevuld") is voor de vorm zelf waar (de sluiting die de auteur schreef) en voor een ander onbepaald. De samengestelde
   voorwaarde leest haar leden van links naar rechts tot haar oordeel vaststaat.
4. *Een vorm* beslist als haar voorwaarde voor haarzelf waar is en haar waarde zonder afbreken rekent. Beslist zij niet,
   dan is haar doel open, tenzij haar voorwaarde voor een ander onwaar is (dan staat vast dat zij niets vult). "Staat
   vast dat niet" van een kenmerk en "vaststaand leeg" van een attribuut zijn dus: geen waarde en geen merkteken, nadat
   het stratum van het lid af is.

Bestudeer vóór de bouw hoe ref een lezing behandelt van een lid dat later in hetzelfde stratum geschreven wordt
(RUNTIME-SEMANTICS ch. 5 en 6, de schrijfacties aan het fixpunt): een merkteken "open" moet voor die regels gelden als
een schrijfactie op het doel.

## Bouwdelen

Elk deel een verse bouwer; elk deel eindigt groen en met een overdracht.

1. **De kern** (deel 1a): de tekst in RUNTIME-SEMANTICS voor de standen, de open lezing, het oordeel van een
   voorwaarde, de toets op leeg en gevuld, de regel, het kenmerk en de consistentieregel, met uitgewerkte voorbeelden;
   ref rekent die vormen; elk ander soort vorm in een strikt document weigert ref met REF-014 en de vorm als parameter.
2. **Het case-formaat, de golden en de gates** (deel 1b): de velden voor "staat vast dat niet" en "open", de cases van
   `jutterswet-afleiden-uit-leeg`, de kanaallijsten van clingo en Drools, de poot voor behoud bij volledige kennis.
3. **De stapel, de vouwen en de creaties** (deel 2): defaults met doorval, initialisatie en opgave, de vouwen en
   kwantorvouwen over een verzameling, object- en feitcreatie; de getuigen van de proef als goldens.
4. **Lezingen, eindstand, wat nog geweigerd wordt** (deel 3): ch. 5.12 en 5.12a voor een onbepaalde voorwaarde en een
   open lid, de WF-regel voor vormen zonder gedefinieerde lezing, de poot voor monotonie, het Result.

## Acceptance (deel 1a; de volgende delen krijgen de hunne in hun opdracht)

Rood eerst per voorbeeld.

- **RUNTIME-SEMANTICS** krijgt een paragraaf in ch. 1 (voorstel: 1.5) die de lezing beschrijft in de termen van de
  executie, niet van een omzetting: de drie standen en waar "open" vandaan komt; dat een document zonder de eigenschap
  geen open waarde kent; een evaluatie die een open waarde leest; het oordeel van een voorwaarde met de tabellen en de
  leesvolgorde; de toets op leeg en gevuld met haar sluiting; wanneer een regel beslist, vaststaand niets vult, of haar
  doel open laat; de drie standen van een kenmerk; de consistentieregel; de twee eisen. Elke zin die iets vastlegt
  heeft een uitgewerkt voorbeeld uit de Jutterswet met de verwachte uitkomst, en elk voorbeeld is een test in
  `ref/test/` met zijn naam (`WV-1.5-…`). Huidige stand, geen geschiedenis, geen status van een executor, geen naam van
  de proef of haar gegenereerde kenmerken. Als voorstel; de hoofdsessie redigeert bij de landing.
- **ref** rekent in een strikt document: de regel (gelijkstelling, berekening, kenmerktoekenning, initialisatieregel),
  elke voorwaarde zonder vouw (vergelijking, toets op leeg en gevuld ook van een berekende waarde, cijfertoets,
  kenmerkcheck ook genavigeerd, rolcheck, identiteitscheck, toets op een marker, de samengestelde voorwaarde met haar
  vijf kwantoren), elke uitdrukking zonder vouw of tijdlijn (door de opzet zonder wijziging), de lezing over een pad met
  een afwezige link, `Let`, en de consistentieregel. De getuigen van de proef binnen dat bereik zijn de voorbeelden
  (`arithmetic`, `comparison`, `filled-test`, `compound-all`, `compound-any`, `compound-none`, `compound-counting`,
  `characteristic`, `marker`, `absent-link`, `absent-link-path`, `navigated-read`, `consistency`, `exact-digits`,
  `settled-empty`, `settled-empty-date`, `let`, `closed-input`, en de delen zonder stapel of vouw van
  `filled-test-not-closure`, `composite-filled-test` en `empty-by-fault`), met hun handberekende uitkomsten uit het
  voorstel; de gewone documenten staan onder `spec/verifier/test/strict-empty/` op de branch
  `card/strict-empty-lowering` (worktree `/Users/yeb/Developer/uchar-dev/wt/strict-empty-lowering`, alleen lezen). Hun
  cases noemen gegenereerde namen van de proef (`K (niet)`, `A (leeg)`); vertaal die naar de standen: `K (niet)` is
  "staat vast dat niet", geen van beide is "open".
- **Gesloten invoer** is in dit deel een instelling van de sessie voor de tests (een lijst leden, en "alles"): er is
  nog geen schrijfwijze in de bron. Een opgave is gesloten door haar declaratie.
- **Wat ref nog niet strikt rekent, weigert hij bij naam**: een strikt document met een defaults-groep, een vouw of
  kwantorvouw over een verzameling, een creatie, een tijdlijnvorm, een verdeling, een dagsoort, een uniciteit, een cel
  van een gedimensioneerd attribuut, of een vorm van ch. 9–11 buiten de marker, geeft REF-014 met de vorm als parameter
  (`form`), vóór er iets gerekend wordt. Eén functie somt ze op.
- **Een gewoon document verandert niet**: de suite van ref en gate 5, 7, 8, 13 en 17 groen, zonder gewijzigde
  verwachting.
- **Behoud bij volledige kennis, gemeten** met een script in de werkmap (de gate-poot komt in deel 1b): elke golden van
  het corpus waarvan ref de vormen strikt draagt, met de eigenschap erbij gezet en alle invoer gesloten, speelt haar
  eigen verwachtingen. Rapporteer goldens, cases, gelijk, niet gelijk met de oorzaak; elke afwijking is een fout.
- **De naad, gemeten**: in het Result per bestand van `ref/lib` de toegevoegde en gewijzigde regels en de functies die
  geraakt zijn, en of de vier punten van "De architectuur" gehouden zijn.
- Los groen: gate 1, 3, 5, 6, 7, 8, 13, 17 (clingo en de suites van de OCaml-frontends bouwen mee met de bibliotheek
  van ref). `make gates` draait de hoofdsessie.

## May touch

- `ref/` (bibliotheek, runner, tests, fixtures, `codes.tsv`);
- `spec/RUNTIME-SEMANTICS.md` (de paragraaf, als voorstel) en in `spec/IR-SPEC.md` alleen de verwijzing van de paragraaf
  over `inferFromEmpty` naar de nieuwe tekst;
- wat in `backends/clingo/`, `frontends/catala/` of `frontends/flogic/` moet meebewegen omdat een signatuur van de
  bibliotheek van ref verandert (meld elke plek);
- een gate-script alleen waar een floor of lijst het vraagt, in een eigen commit;
- deze kaart.

Mandaat voor de beschermde paden: de opdracht van de eigenaar van 2026-11-21 en zijn keuze voor deze lezing. De
hoofdsessie leest na en landt.

## Out of scope (deel 1a)

Het case-schema en de casebestanden, de golden, de kanaallijsten (deel 1b); de stapel, de vouwen, de creaties (deel 2);
lezingen en eindstand (deel 3); `backends/drools/`, de vertaling van clingo, de frontend van RegelSpraak, de verifier
buiten wat een signatuur vraagt, `spec/CONSTRUCTS.md` en `spec/PLAN.md` (stel de rij voor), de toolchain, tijdlijnen,
de schrijfwijze van sluiten per gegeven.

## Besluiten vóór de bouw

Van de eigenaar (2026-11-21): de lezing met drie standen; de sterke tabel van Kleene, ook na een onbepaald lid; een
initialisatie is een sluiting over haar stapel; de wire draagt het document van de auteur en elke executor draagt de
lezing zelf; voorbeelden uit de Jutterswet.

Van de hoofdsessie (voorlopig, ter bevestiging): een lezing over een afwezige link staat vast; "X is leeg" over een
open X is alleen voor de vorm die het schreef een sluiting; een opgave is gesloten door haar declaratie; een
invoerkenmerk dat de case niet noemt is open; een afgeleid lid dat de case geen waarde geeft volgt zijn schrijvers; een
evaluatie die een open waarde leest geeft geen waarde, geen oordeel en geen deponering; de invoer naast een schrijver
die niet beslist blijft staan (ch. 7.8).

## Voorbeelden en namen

Voorbeelden komen uit de Jutterswet (vondst, jutter, taxatie, gewicht, vindloon). In niets dat in de repository komt
staat een naam, regel of casus van een regelset buiten deze repository.
