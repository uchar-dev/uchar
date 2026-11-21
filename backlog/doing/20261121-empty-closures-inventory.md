# empty-closures-inventory: opsommen waar een document de wereld sluit of een ontkenning op leeg laat rusten

*Opgesteld door een werkbanksessie op 2026-11-21, op verzoek van de eigenaar. Stap 1 van drie uit het advies
"Leeg, onbekend en niet van toepassing" (werkbank, `werknotities/20261121-null-logica/rapport.md`, §3 en §4). Alleen
meten; de semantiek verandert niet.*

## Aanleiding

De eigenaar besloot op 2026-11-20: **uit leeg mag niets afgeleid worden**. Het besluit is algemeen bedoeld
(verduidelijkt op 2026-11-21): het is een eis aan het uchar-dialect, niet een keuze van één regelset; het kwam op bij
v2/herleving-ww (regelspraak-rules). Leeg is "nog niet ingevuld"; pas als een marker (`onbekend`, `niet van toepassing`) is gegeven, of een
object of feittype-relatie ontbreekt, mag worden doorgerekend (bevestigd 2026-11-21). In uchar is leeg de spiegel van
RegelSpraak v0 (RUNTIME-SEMANTICS ch. 1.2–1.4, besluit 2026-08-16): leeg telt in plus/min/maal als 0, een vergelijking
met leeg is onwaar, voorwaarden zijn tweewaardig, een kenmerk is onwaar zolang het niet is toegekend. Dat is in feite
een closed-world assumption. De regelset vangt het nu per plek af (toetsen "heeft een waarde", een hulpkenmerk
`herleving afhankelijk-van-ontbrekende invoer`); reviews vonden steeds nieuwe plekken (regelspraak-rules 8edee01,
inventaris: twaalf plekken en latere vondsten).

Het advies stelt een strikte lezing van leeg voor (Kleene K3 voor voorwaarden, leeg propageert, de markers blijven
gevulde waarden, de wereld sluiten alleen met een uitdrukkelijke vorm: local closed world), per document aan te
zetten. Voor de eigenaar daarover besluit, wil hij weten hoe groot het is.

## Goal

Een commando dat op een bestaand document, zonder iets te veranderen, opsomt:

- **sluitingen:** plekken waar een uitkomst op een lege waarde rust omdat leeg als 0 of als onwaar wordt gelezen
  (coalescerend rekenen met een invoer die leeg kan zijn; een vergelijking of predicaat waar leeg de uitkomst beslist;
  een initialisatie of standaardwaarde; een tak op "is leeg");
- **leeggevoelige ontkenningen:** "heeft geen K", "voldoet aan geen van", een uitzondering waarvan de voorwaarde niet
  kon toetsen, een basisregel onder zo'n uitzondering — plekken waar een ontbrekende invoer via een ontkenning een
  uitkomst beslist.

Per plek: bestand en regel, de soort, en welke invoer leeg kan zijn. Waar het commando hoort (bij `check`, als eigen
subcommando, of in de lint van de werkbank) stelt de bouwer voor.

## Acceptance

- Op regelspraak-rules `229d744` (v2/herleving-ww vóór de ronde van 20-11) staan de twaalf plekken van de inventaris
  (`werkbank/werknotities/20261120-leeg-afleiding/inventaris.md`) en de latere vondsten (review-regels, review-groep2)
  in de uitvoer; op `8edee01` of later zijn de afgevangen plekken herkenbaar (met hun toets of het hulpkenmerk).
- Op het corpus: per golden een telling (sluitingen, ontkenningen), zodat zichtbaar is hoeveel op coalesceren of
  negation as failure rust.
- Geen wijziging in IR, executors of uitkomsten; gates groen.
- WERKBANK.md voor de werkbank met de uitvoer op v2.

## Omvang

Klein: één ronde, alleen analyse. Daarna besluit de eigenaar over stap 2 (voorstel `STRICT-EMPTY.md` en een proef
achter een vlag) en stap 3 (landen).

## May touch

- `spec/verifier/lib/empty_closures.ml` (nieuw), `spec/verifier/bin/verifier.ml`, `spec/verifier/README.md`;
- `spec/verifier/test/empty-closures/` (nieuw): één getuige per soort;
- `scripts/check-verifier.sh` (een poot in gate 3) en de alinea van gate 3 in `CLAUDE.md`: een voorstel, dat de
  hoofdsessie landt;
- deze kaart.

## Out of scope

Het IR-schema, de executors (ref, clingo, Drools), de semantiek en de verlaging van de frontends, het corpus, de
toolchain, de werkbank, regelspraak-rules en de dataset. Geen naam of afspraak van een regelset in code, tests of
README.

## Besluiten vóór de bouw

Van de eigenaar (2026-11-21):

- Het commando hoort in uchar zelf, als analyse over de IR; niet in de toolchain en niet in de lint van de werkbank.
  De stappen 2 en 3 van het advies komen in de frontend en de IR van uchar.
- De meting op v2 komt in het Result en in WERKBANK.md; alleen de telling op het corpus is een test.
- Waar de IR geen bestand en regel draagt, geeft het commando de JSON-pointer en de positie die de IR wel draagt, en
  het Result benoemt wat ontbreekt.
- Een afvanging meldt het commando naar haar vorm ("staat onder een toets op gevuld", "afgevangen door een kenmerk
  waarvan een regel op leeg toetst"), zonder namen van een regelset.
- Het principe geldt algemeen, voor het dialect. De eigenaar wil geen closed-world assumption, het onderscheid tussen
  leeg en de twee markers behouden, en een lezing die uitlegbaar is voor engineers en regelanalisten.
- Het advies dat voorligt, nog geen besluit: een regelset leest standaard open en sluit uitdrukkelijk per gegeven of
  relatie (local closed world); geen schakelaar bij het uitrekenen. Deze meting dient dat besluit.

Van de hoofdsessie (voorlopig, ter bevestiging):

- **Vorm.** `verifier --empty-closures <ir.json>`: één tab-gescheiden regel per plek, in documentvolgorde; exit 0 ook
  als er plekken zijn, 2 bij een laadfout. De analyse staat in een eigen module `Empty_closures` van de
  verifier-bibliotheek en leest de afhankelijkheidsgraaf die WF-045 en WF-046 lopen.
- **Maatstaf.** Een plek telt wanneer de tweewaardige lezing van RUNTIME-SEMANTICS ch. 1.2 een vorm laat beslissen
  (een waarde schrijven, een kenmerk toekennen, een consistentiefout melden) in een toestand waarin een waarde die zij
  leest leeg is, terwijl een lezing waarin leeg doorgeeft en een voorwaarde over leeg onbepaald is (het advies, §3.3)
  daar niets zou beslissen. Een vergelijking die op leeg onwaar is en waar verder niets uit volgt, telt dus niet; dezelfde
  vergelijking onder een ontkenning wel.
- **Twee klassen.** `closure`: de waarde- en vormkant (een operand die als 0 telt, een bijdrage die een vouw overslaat,
  een lege grens, een vergelijking die op leeg waar is, een toets op leeg, een initialisatie, een terugvalwaarde).
  `negation`: een kenmerk of voorwaarde in een ontkennende positie, en een default die beslist onder een hogere default
  of uitzonderingsgroep die op leeg niet kon toetsen.
- **Per plek.** De soort, de pointer, de naam van de vorm, de leden die op de plek gelezen worden en leeg kunnen zijn,
  de invoerleden (die geen vorm schrijft) van waaruit leeg de plek kan bereiken, de afvangingen, en het bronfragment.
- **De test.** Een poot in gate 3: het commando loopt op elke golden en rode `ir.json`, elke pointer bestaat in het
  document, de totalen per klasse en het aantal soorten houden een floor, en elke soort heeft een getuige met een
  vaste uitvoer onder `spec/verifier/test/empty-closures/`.
- **Een derde klasse, apart geteld.** `absent-instance`: een plek waar het ontbreken van een instantie of een link een
  uitkomst beslist. Of een relatie open of gesloten gelezen wordt, is een open beslispunt; de meting laat zien hoe groot
  het is, los van de twee klassen van de kaart.
- **Een telling per invoerlid.** Omdat sluiten per gegeven of relatie zou gebeuren, geeft de meting per invoerlid het
  aantal plekken dat het bereikt.

## Result (bouwer, 2026-11-21)

Het principe "uit leeg mag niets afgeleid worden" is een eis aan het dialect; v2 is de regelset waarop het gemeten is.
Het commando meet; IR, schema, executors, frontends en corpus veranderen niet. Merge en push: niet gedaan.

**Commits** (branch `card/empty-closures-inventory`, vanaf `ee9a7e4f3`):

- `d244fdcc3` de module `spec/verifier/lib/empty_closures.ml`, de modus `verifier --empty-closures` in
  `bin/verifier.ml` (gebruiksregel bijgewerkt), de sectie in `spec/verifier/README.md`, en de kaarttekst van de
  hoofdsessie (reikwijdte van het besluit, de derde klasse, de telling per invoerlid);
- `e54cb9176` 24 getuigen onder `spec/verifier/test/empty-closures/`;
- `0d80c7d55` de poot in gate 3 (`scripts/check-verifier.sh`);
- `d4e9595d4` drie fouten uit de lezing van de hoofdsessie: een afvanging telt alleen een schrijver die op leeg sluit;
  elk lid gespeld op het type dat het declareert; een default met eigen inhoud en uitzonderingsgroepen geeft ook de
  lezingen van die groepen door;
- `7ebcbc62d` de floor van de negation-plekken op de nieuwe meting (382 → 383);
- de commit van dit Result.

**De kolommen** (tab-gescheiden, één regel per plek, documentvolgorde, exit 0; exit 2 bij een laadfout): klasse; soort;
JSON-pointer van de knoop; naam van de vorm zoals de WF-meldingen haar noemen; de leden die leeg kunnen zijn,
`<knoop>=input|derived`, gespeld op het declarerende type (`attr:…`, `char:…`, een cel met haar labels, `param:<naam>`
voor een lijstparameter, `rel:<feittype>:<rol>` voor de derde klasse); de invoerleden (transitief over de graaf van
WF-045/046); de afvangingen; het regelnummer of `-`; het bronfragment op één regel of `-`.

**De soorten** (één zin elk; ruim/krap in de README):

- `closure`: `coalesce-operand` (operand van plus, min, maal, percentage van die als 0 telt, per dag op een tijdlijn),
  `coalesce-subtrahend` (rechteroperand van verminderd met), `coalesce-dividend` (het deeltal), `empty-divisor-fault`
  (een lege deler meldt een §9.5-feit, EF-031), `coalesce-duration` (de duur bij een datum), `fold-skips-empty` (som,
  totaal, minimum, maximum, eerste, laatste slaan lege bijdragen over), `filled-count` (de telling van gevulde
  waarden), `empty-fallback` (de terugvalwaarde als alle bijdragen leeg zijn), `clamp-empty-bound` (een lege grens is
  geen grens), `empty-text-part` (een leeg tekstdeel wordt de lege string), `allocation-empty-input` (gewicht, maximum
  of volgorde van een verdeling, ch. 4.4), `true-on-empty` (een vergelijking of predicaat die op leeg waar is, in een
  positie die op waar beslist), `empty-test` (een toets op leeg of gevuld in de positie waar het lege oordeel beslist),
  `initialisation` (een regelversie of default met `initialisation`);
- `negation`: `false-on-empty` (een vergelijking, predicaat of periodevenster die op leeg onwaar is, in een positie
  die op onwaar beslist), `negated-characteristic` (een kenmerkcheck in een positie die op onwaar beslist),
  `fallthrough` (een default, of een initialisatie naast een berekening, die beslist onder een hogere waarvan de
  rechtvaardiging op leeg niet kan toetsen of de consequentie leeg kan zijn, de uitzonderingsgroepen van de hogere
  inbegrepen; gemeld bij de lagere met wat de hogere leest, zonder wat de lagere zelf nodig heeft);
- `absent-instance`: `negated-role-check` (een rolcheck in een positie die op onwaar beslist), `vacuous-quantifier`
  (een kwantorvouw waarvan het oordeel zonder lid beslist), `empty-set-count` (telling, totaal of gevulde-telling over
  een bereik: 0 zonder lid), `empty-set-fallback` (de terugvalwaarde zonder lid).
- De afvangingen: `filled-test:<lid>` (de lezing staat onder een toets op gevuld van dezelfde lezing, de dominantie van
  WF-127), `characteristic-check:<kenmerk>` (de vorm draagt als conjunct een check op een kenmerk waarvan een schrijver op
  de leegte van een lid sluit: een `empty-test`-plek van die schrijver), `tested-elsewhere:<invoer>><kenmerk>,…` (een
  andere vorm die een kenmerk schrijft, sluit op de leegte van dat invoerlid).

**Rood eerst.** De eerste versie van de detector bestond vóór de getuigen; de verwachting van elke getuige is met de
hand uit haar `ir.json` geschreven vóór de detector op dat document draaide. Daarbij was `fallthrough` rood (de lagere
default herhaalde de leden van zijn eigen voorwaarde; opgelost met de regel "wat de lagere zelf nodig heeft") en waren
de vier `absent-instance`-getuigen en de twee die er een regel bij kregen (`empty-fallback`, `filled-count`) rood tot de
derde klasse er was. Daarna per getuige rood met een mutant die precies die soort of afvanging weglaat, gebouwd in een
eigen worktree: 24 van 24 RED (`werk/empty-closures-inventory/logs/red-per-kind-2.log`, script
`tools/red_per_kind.py`). De poot: groen (`logs/gate3-2.log`, `logs/gate3-3.log`), rood met
`EXPECTED_MIN_EMPTY_NEGATION=383` op de oude meting en een gewijzigde verwachting van `fallthrough`
(`logs/gate3-red-1.log`: de diff van de getuige en "382 places of class negation found, but
EXPECTED_MIN_EMPTY_NEGATION pins at least 383").

**Gewijzigde getuigen** (`d4e9595d4`; de nieuwe verwachtingen rood op de binary van `0d80c7d55`,
`werk/empty-closures-inventory/probes/red-round2.txt`):

- `fallthrough`: een tweede stapel, een form-a default 'heffing bij boete' met de uitzonderingsgroep 'heffing bij
  korting' boven 'heffing basis'. Oud: de getuige had alleen de stapel van penningrecht, met één regel. Nieuw: die regel
  en `fallthrough` bij `/rules/prioritizedDefaults/1/defaults/0` met `attr:jutter:korting=input` en bij `…/1/defaults/1`
  met `attr:jutter:boete=input; attr:jutter:korting=input`; de binary van `0d80c7d55` gaf bij die laatste alleen
  `attr:jutter:boete=input` (de lezing van de uitzonderingsgroep viel weg).
- `guard-characteristic-check`: tegenvoorbeeld 'aangegeven grondslag' ("indien zijn grondslag gevuld is") als tweede
  conjunct. Oud: de afvangingen noemden ook `characteristic-check:char:jutter:aangegeven`; nieuw alleen
  `characteristic-check:char:jutter:onvolledig` en `tested-elsewhere:attr:jutter:toeslag>char:jutter:onvolledig`.
- `guard-tested-elsewhere`: hetzelfde tegenvoorbeeld en een lid van een supertype (`gewicht` van Vondst, op leeg
  getoetst op Vondst, gelezen op Wrakvondst). Oud: `tested-elsewhere:attr:jutter:grondslag>char:jutter:aangegeven` stond
  erbij, en de lezing op Wrakvondst was `attr:wrakvondst:gewicht=input` zonder afvanging; nieuw: alleen
  `tested-elsewhere:attr:jutter:toeslag>char:jutter:onvolledig`, en `attr:vondst:gewicht=input` met
  `tested-elsewhere:attr:vondst:gewicht>char:vondst:ongewogen`.

**Floors** (meting 2026-11-21 over 506 golden en rode documenten): `EXPECTED_MIN_EMPTY_CLOSURE=827`,
`EXPECTED_MIN_EMPTY_NEGATION=383`, `EXPECTED_MIN_EMPTY_ABSENT=206`, `EXPECTED_MIN_EMPTY_KINDS=20` (20 van 21;
`clamp-empty-bound` heeft alleen zijn getuige), `EXPECTED_MIN_EMPTY_WITNESSES=24`. De pointercontrole van check 4 is een
functie die beide leest; 1453 pointers van de inventaris lossen op.

**Het corpus** (430 goldens; `werk/empty-closures-inventory/meting/tally-corpus.tsv`, `tally-inputs-corpus.tsv`,
`tally-members-corpus.tsv`, script `meting/tally.py`, samenvatting `meting/summary-corpus.txt`): 1331 plekken, closure
775, negation 364, absent-instance 192. Per soort: fold-skips-empty 202, coalesce-operand 184, false-on-empty 126,
fallthrough 123, negated-characteristic 115, empty-set-count 114, empty-divisor-fault 93, coalesce-dividend 92,
vacuous-quantifier 48, true-on-empty 42, initialisation 42, empty-fallback 28, allocation-empty-input 26,
empty-set-fallback 26, empty-test 20, coalesce-duration 16, filled-count 11, empty-text-part 11, coalesce-subtrahend 8,
negated-role-check 4, clamp-empty-bound 0. Goldens met ten minste één plek: closure 257, negation 161, absent-instance
96; zonder plek 92. Afgevangen tegenover niet: closure 27/748, negation 9/355, absent-instance 0/192 (alle met
`filled-test`; het corpus heeft geen hulpkenmerken). De tien goldens met de meeste plekken: anchor-forms 38,
jutterswet-rekenen-per-dag 37, jutterswet-sluitdag-gemeld 19, jutterswet-deling-in-afwijking 18,
jutterswet-deling-in-afwijking-omgekeerd 18, jutterswet-grens-per-jutter 18, jutterswet-seinwacht 16,
jutterswet-tekst-van-een-waarde 16, jutterswet-vouwfilter-buiten-de-beurt 15, anchor-depth-two 14. Gelezen leden: 553
(428 invoer, 125 afgeleid); plekken die alleen invoer lezen 1068, alleen afgeleide 212, beide 51; bovenaan
attr:vondst:gewicht (72), attr:vergunning:tarief (37), attr:vondst:opbrengst (29). Invoerleden van de sluiting: 501;
de 53 met de meeste plekken dragen samen de helft.

**v2** (`werk/empty-closures-inventory/meting/meting-v2.md`): 206 plekken op `229d744`, 441 op `8edee01`, 621 op
`6d39e7d`; afgevangen 79, 160, 179 (met `filled-test` of `characteristic-check`: 62, 93, 94). De twaalf plekken van de
inventaris (a1–a5, b1–b7) en de latere vondsten R5–R8, G3 en G7 staan op `229d744` in de uitvoer; G4, G5 en G6 gaan over
regels die pas later bestaan en staan op die commits. Geen plek ontbreekt. Op `229d744` draagt geen van de twaalf het
hulpkenmerk `herleving afhankelijk-van-ontbrekende invoer`; a1, a3 en b3 dragen alleen een afvanging op een kenmerk dat
op andere invoer sluit, a2, a5 en b6 niets. Op `8edee01` zijn a1, a3, a5 (in hww-18), b6 en b7 herkenbaar aan het
hulpkenmerk, a2 alleen via de lezers. Gelezen leden op `6d39e7d`: 78 (38 invoer); plekken die alleen invoer lezen 288,
alleen afgeleide 296, beide 37; bovenaan de code reden beëindiging (126), de relatie naar de eindigingsgronden (57) en
de code soort aanvraag (19). Per invoerlid van de sluiting dragen 1 of 2 invoerleden de helft, door de ruime sluiting.
Het concept voor de werkbank: `werk/empty-closures-inventory/WERKBANK.md`.

De werkbanksessie meldde: het afvangen per plek in v2 is een tussenstand die na de strikte lezing vervalt, met gelijke
uitkomsten als acceptatie; de afgevangen versie is dan de referentie.

**De gegenereerde opgavestapel.** De default "opgave boven berekening" meldt een `fallthrough` op de lege opgave: 9
plekken in v2 met een gegenereerd opgave-lid (herkomst `inputAboveComputation`, invoer), 8 met alleen zulke leden; op
het corpus 24, waarvan 16 alleen. Onder de strikte lezing van het advies (§3.3) zou de berekening dan nooit beslissen
zolang de opgave leeg is: een vraag voor het voorstel van de volgende stap.

**Wat de wire niet draagt.** Geen bestandsnaam; een regelnummer alleen op de versie van een consistentieregel en op een
regelgroep (zoals de frontend ze schrijft). Bestand en regel van een plek in v2 kwamen uit de bronnen, op de naam van
de vorm, met een script in de werkmap (`meting/locate.py`), niet uit het commando.

**Voorgestelde tekst voor de alinea van gate 3 in `CLAUDE.md`** (de hoofdsessie zet haar erin):

> 3. `scripts/check-verifier.sh` — the OCaml verifier (`spec/verifier/`) builds in the workspace `_build`
>    (`scripts/lib/ocaml-build.sh`), the one gates 11 and 15 use, and builds on its own: no dune file under
>    `spec/verifier` names a library of another directory, so `dune build --root spec/verifier` works. Every golden
>    and red `ir.json` is verifier-clean; every `corpus/negative/<wf###-slug>/ir.json` is rejected citing the rule its
>    directory names, one negative per arm of every rule. A violation carries a key `WF-###.<slug>` (one per message
>    template) and named parameters; the report line is `WF-###.<slug> FAIL <pointer>: <message>`, and
>    `verifier --arms` prints key, pointer and parameters. `expect.txt`, where present, is a key line of the declared
>    rule plus `name=value` lines, matched as a subset against the parameters of one violation. Every JSON pointer a
>    negative report cites resolves in its document (RFC 6901, read up to the first `: `; a FAIL line without that
>    separator fails). The gate refuses a key with two message templates and a violation built without a key in
>    `checks.ml`. The empty-closures inventory (`verifier --empty-closures`, `spec/verifier/README.md`) runs on every
>    golden and red `ir.json` with exit 0; every line carries nine columns, a class (`closure`, `negation`,
>    `absent-instance`), a kind slug and a pointer that resolves in its document; every witness under
>    `spec/verifier/test/empty-closures/<kind>/` is verifier-clean, gives its `expected.txt` byte for byte and reports
>    the kind or guard its directory names, and every kind reported has a witness. Floors: `EXPECTED_MIN_ARMS`,
>    `EXPECTED_MIN_PARAM_LINES`, `EXPECTED_MIN_REPORT_KEYS`, `EXPECTED_MIN_ARM_SITES`, `EXPECTED_MIN_EXPECT`,
>    `EXPECTED_MIN_POINTERS`, `EXPECTED_MIN_NEGATIVE`, `EXPECTED_MIN_EMPTY_CLOSURE`, `EXPECTED_MIN_EMPTY_NEGATION`,
>    `EXPECTED_MIN_EMPTY_ABSENT`, `EXPECTED_MIN_EMPTY_KINDS`, `EXPECTED_MIN_EMPTY_WITNESSES`.

**Voorlopige besluiten** (ter bevestiging):

1. Een plek per sluitende knoop (de operator, de vouw, de vergelijking), met de leden van al haar operanden; de
   initialisatie en de doorval staan op de pointer van de versie of default.
2. Elk attribuut en elke cel kan leeg zijn (ruim); een parameter niet (een ongebonden parameter geeft EF-014), behalve
   een lijstparameter.
3. Polariteit "beide" voor het filter van een waardevouw, de voorwaarde van een IfThenElse en de leden van `exactly`.
4. Een lege deler is een eigen soort (`empty-divisor-fault`): het §9.5-feit is een gemelde fout op leeg.
5. `fallthrough` laat weg wat de lagere default zelf nodig heeft; een toets op gevuld in de hogere rechtvaardiging geldt
   als afgevangen (een uitdrukkelijke sluiting). Een initialisatie naast een berekening op hetzelfde doel krijgt een
   `fallthrough` met wat de berekening leest.
6. Een schrijver "sluit op leeg" waar hij een `empty-test`-plek heeft; alleen die telt voor `characteristic-check` en
   `tested-elsewhere`. `characteristic-check` telt een check in elke polariteit; `tested-elsewhere` sluit de eigen vorm
   uit en noemt per invoerlid de kenmerken. "Afgevangen" in de tellingen: de kolom met afvangingen is niet leeg.
7. Elk lid is gespeld op het type dat het declareert (`Checks.declaring_type`), in de uitvoer en in de sleutels van de
   afvangingen; twee leden met dezelfde naam zonder gemeenschappelijke declaratie blijven twee.
8. De derde klasse spelt een relatie `rel:<feittype>:<rol>`; haar invoer volgt ook relaties en populaties die een
   creatie schrijft. Een genavigeerde kenmerkcheck over een afwezige link blijft `negated-characteristic`; een doorval op
   een rolcheck of kwantorvouw in een hogere default wordt niet gemeld.
9. Een uniciteitsregel heeft geen plek: een lege sleutel botst nooit, de regel meldt dan niets (ch. 5.9).
10. Bronfragment en regelnummer van de dichtstbijzijnde knoop op of boven de plek die er een draagt.
11. De kolom met invoerleden blijft de hele sluiting (op v2 is de uitvoer 270 kB tot 800 kB); de tellingen per invoerlid
    en per gelezen lid komen eruit, in de meetscripts.

**Open punten**

1. `characteristic-check` telt een check in elke polariteit. Een afvanging die de polariteit volgt ("heeft geen K" waar K
   op leeg gezet wordt) zou "afgevangen" scheiden van "uitdrukkelijk gesloten".
2. De invoersluiting volgt ook de voorwaarden van de schrijvers; in v2 bereikt één invoerlid de helft van de plekken. De
   telling per gelezen lid staat ernaast; een sluiting die alleen volgt wat leeg doorgeeft naar de geschreven waarde,
   zou de kolom zelf bruikbaarder maken.
3. Een regel die de vouw van ch. 7.10 (overerving) in een stapel opneemt, wordt niet als default gelezen; een
   initialisatie naast een defaultsblok op hetzelfde doel krijgt geen `fallthrough`.
4. Gesloten bodies (lambda, `FunctionDef`) worden niet gelopen; een sluitende operator daarin over een parameter wordt
   niet gemeld (in het corpus zelden, in v2 niet).
5. `clamp-empty-bound` komt in het corpus niet voor (elke grens is een literal); de floor van de soorten is 20 van 21.
6. Bijvangst: de RegelSpraak-frontend schrijft als bronfragment van een Default de `// Bron:`-tekst, niet de regel; de
   kolom met het fragment van een `fallthrough` is daardoor in v2 weinig zeggend.
7. De gegenereerde opgavestapel en de strikte lezing (hierboven): een vraag voor het voorstel van de volgende stap.
8. De alinea van gate 3 in `CLAUDE.md` (voorstel hierboven) landt de hoofdsessie.

**`make gates`** op `7ebcbc62d` (`werk/empty-closures-inventory/logs/gates-2.log`, `out/gates/20261121-110550/`), de
samenvattingsregels:

```
ocaml rc=0 0s ocaml-build: fresh (stamp newer than 574 source files)
frontend rc=0 0s frontend: fresh (out/ newer than every source, the tool jars present)
jar rc=0 0s drools-jar: fresh (jar newer than 1008 source files)
01-self-contained rc=0 3s
02-ir-schema rc=0 183s
04-case-schema rc=0 10s
03-verifier rc=0 59s
05-oracle rc=0 30s
06-clingo rc=0 770s
07-noop rc=0 24s
08-red rc=0 307s
09-catala rc=0 7s
10-regelspraak rc=0 408s
11-toka-parity rc=0 328s
12-drools rc=0 622s
13-reftype rc=0 689s
14-flogic rc=0 3s
15-graph rc=0 70s
16-scenario rc=0 59s
17-readings rc=0 400s
after-run rc=0 0s the three builds fresh, no product newer than the end of the build step
end 2026-11-21 11:18:40 770s red=0
```

Een eerdere run op `0d80c7d55` (`logs/gates-1.log`) had de 17 gates groen en `after-run` rood, omdat de bron tijdens de
run veranderde (de fixes van `d4e9595d4`); zij telt niet.
