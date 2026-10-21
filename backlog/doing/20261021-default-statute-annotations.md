# default-statute-annotations: een default draagt zijn grondslagen als lijst, zoals een regel

*Besluit van de eigenaar 2026-10-21 ("Default.source een lijst maken"), uit Fable-advies 3 bij de toolchain-kaart
`explanation-parts` (`uchar-dev/werk/defaults-decider/fable-advies-3.md` §2.3). Richting door de hoofdsessie, naar de
naamregel van `CLAUDE.md` (waar de draad een naam heeft voor een begrip, gebruikt de code die): de draad heeft voor dit
begrip al `statuteAnnotations`.*

## Aanleiding

Een regel draagt zijn `// Bron:`-annotaties op de draad als lijst, `statuteAnnotations` (IR-SPEC ch. 3.1, WF-082). Een
default (IR-SPEC ch. 6.1) heeft geen eigen veld: de frontend voegt de annotaties van het blok samen met "; "
(`RuleChecker.annotationText`) en schrijft die tekst als `source` van de default, het bronrecord van ch. 4 (`snippet`),
dat op elke andere knoop de brontekst draagt. Een afnemer die de grondslag ontleedt (de toolchain, `explanation-parts`)
kan de annotaties van een default dus niet scheiden zonder de tekst te knippen, en "; " komt ook binnen een toelichting
voor. Op de draad dragen 204 defaults in 64 bestanden (corpus en `toka/`) een `source`; welke daarvan annotaties zijn
en welke brontekst, stelt de kaart vast.

## Doel

- Een `Default` krijgt het optionele extensieveld `statuteAnnotations` (lijst van niet-lege, getrimde strings, in
  bronvolgorde; afwezig zonder annotaties; geheel of niet), onder dezelfde discipline als op een `Rule`: WF-082 dekt het
  ook op een default, met een verifier-arm en negatieve gevallen. Geen semantiek, zoals `source` en `name`.
- De frontend schrijft de annotaties van een blok daar, en niet meer als `source.snippet`; `source` van een default is
  het bronrecord van ch. 4, of afwezig waar de frontend geen brontekst heeft.
- Schema, IR-SPEC (ch. 6.1 en WF-082), verifier, de lader van de Drools-backend (het model van de default draagt de
  lijst), en de goldens en `toka/`-slices die het raakt (gate 10 en 11 bytegelijk na heruitgifte; een handgeschreven
  `anchor/`-golden alleen als zijn `source` een annotatie bleek).
- Gate 13: zoals bij `name` en `origin` een leg die de annotaties van de defaults weghaalt en hetzelfde antwoord eist,
  als dat in de bestaande leg van `Default.name` past; anders de reden in de Result.

## Acceptatie

- (a) Rood eerst: een golden met een default met twee annotaties, waarvan er één "; " bevat, waarvan de lijst bytegelijk
  op de draad staat; negatieve gevallen voor de WF-082-arm op een default.
- (b) De zeventien gates groen.
- (c) Voor de toolchain, in een eigen stap na de landing: de pin-bump, en de ontleder van de grondslag leest de lijst
  van een default in plaats van de samengevoegde tekst.

May touch: `spec/IR-SPEC.md`, `spec/schema/`, `spec/verifier/` (voorstellen; beschermde paden), `frontends/regelspraak`,
`backends/drools` (de lader en het model), de goldens en `toka/`-slices die de heruitgifte raakt, gate-scripts als
voorstel, deze kaart. CONSTRUCTS-rij als tekst in de Result.

## Voorlopige besluiten van de bouwer (2026-10-21)

- **De groep houdt zijn samengevoegde `source`.** Een defaults-groep die naar een basisregel heet, schrijft de
  annotaties van die regel als `PrioritizedDefaults.source`, samengevoegd met "; " (9 groepen in 8 goldens, gemeten).
  De kaart noemt alleen de default; `grammar/DIVERGENCES.md` legt de plaatsing op de groep met een eigen reden vast
  (de handankers dragen op elke groep een bronrecord). Ongewijzigd gelaten; de basisdefault draagt dezelfde annotaties
  als lijst. Een vervolgkaart kan de groep dezelfde lijst geven of zijn `source` laten vallen.
- **De verifier leest de lijst naast het document**, zoals de vocabulairetabel (`Loader.default_annotations`,
  `Checks.default_annotations`): het OCaml-record van een default wordt door de Catala- en F-logic-frontends heel
  gebouwd, dus een veld erbij had die twee frontends en hun suites geraakt.
- **De WF-082-arm draagt de parameter `kind`** (`rule` of `default`); de drie sleutels blijven, met één sjabloon elk.
- **Gate 13 neemt de annotaties mee in de naamleg**: dezelfde gestripte kopie verliest `name` en `statuteAnnotations`,
  met een eigen telling en vloer (`EXPECTED_MIN_ANNOTATIONS_STRIPPED=29`).

## Result (2026-10-21)

 een `Default` draagt de `// Bron:`-annotaties van zijn blok op de draad als
lijst `statuteAnnotations`, zoals een regel, en niet meer samengevoegd met "; " in zijn `source`. Tak
`card/default-statute-annotations`: `1aa65d2a4` (de wijziging) en `15c16259a` (de mutatiemeting in de kop van gate 13),
op `81a96781` (na de herbasering op `0b941905`: `8de8cb16` en `4373c173`). Gebouwd door een Opus-bouwer; de
hoofdsessie zette de tak boven op `clingo-day-kind-walk-after-fold-stop` (één conflict: `EXPECTED_PLAYED` in
`scripts/check-drools.sh`, nu 264, met beide redenregels), draaide de volledige set, landde de voorstellen, de
`CLAUDE.md`-tekst en CONSTRUCTS-rij 44, en schreef dit Result af. Eis van de eigenaar, uitgevoerd: de inhoud van een
annotatie is ondoorzichtig voor uchar.

**Meting vooraf (204 defaults met een `source`, 64 bestanden; `toka/` draagt er geen).** Ingedeeld op herkomst
(`u7/measure.py`, log `u7/logs/measure-1.log`):

| zone en herkomst | defaults | wat de `source` was |
|---|---:|---|
| golden met `source.rgs`, tekst = een `// Bron:`-annotatie van het blok | 27 | annotatie (in 11 goldens, steeds één annotatie per blok; geen samengevoegde) |
| golden met `source.rgs`, opgave-default `opgegeven X` / `berekende X` | 20 | gegenereerde naam van het gelezen attribuut (19 niet letterlijk in de bron, 1 toevallig wel) |
| golden met `source.rgs`, group-default | 2 | de tekst van de kop (`Afwijking van meldtermijn`, `Uitzonderingen ongeordend op …`) |
| Catala-goldens | 15 | de variabelenaam die de plugin schrijft |
| handgeschreven goldens zonder bronbestand (37 in `anchor/`, 23 elders) | 60 | handgeschreven bronrecord |
| `corpus/negative` (77) en `corpus/red` (3) | 80 | handgeschreven of gekopieerd bronrecord |

Alleen de 27 waren annotaties; zij verhuisden naar `statuteAnnotations`, de andere 177 bleven staan. Geen
`anchor/`-golden draagt een annotatie (geen bronbestand, dus geen `// Bron:`), dus geen anker veranderde. Daarnaast
dragen 9 groepen in 8 goldens de annotatie van hun basisregel als `PrioritizedDefaults.source`; die bleven ongewijzigd
(voorlopig besluit 1).

**Draadvorm.** Het veld staat als laatste van een `Default`, na `source`; één string per `// Bron:`-regel, getrimd, in
bronvolgorde, afwezig zonder annotaties. Voorbeeld uit de nieuwe golden `jutterswet-tweeledige-grondslag`:

```json
{ "node": "Default", "name": "penningrecht art 3", "decision": true, "justification": { … },
  "statuteAnnotations": [ "Jutterswet art. 3",
                          "Jutterswet art. 3b; een vondst op een stormvloeddag telt tweemaal" ] }
```

Vóór de kaart schreef het frontend daar `"source": {"snippet": "Jutterswet art. 3; Jutterswet art. 3b; een vondst op
een stormvloeddag telt tweemaal"}`. De inhoud van een entry is ondoorzichtig (eis van de eigenaar): IR-SPEC ch. 3.1,
6.1 en WF-082 zeggen dat geen uitvoerder en geen verifier een entry leest of ontleedt, op de twee vormregels van
WF-082 na; niets in code, spec, verifier-armen of tests knipt een entry.

**Rood eerst (acceptatie (a)).** De golden `jutterswet-tweeledige-grondslag` (`source.rgs`, `scenario.rgs`, vier
cases met de hand berekend, NOTE): een basisregel met twee annotaties, waarvan één "; " bevat, een uitzondering met één
en een zonder. Het handgeschreven `ir.json` met de lijsten was vóór de bouw rood op drie plaatsen
(`u7/logs/red-1.log`): het frontend gaf de samengevoegde snippet (gate 10-vorm: diff op twee plaatsen), het schema
weigerde `statuteAnnotations` op beide defaults, de verifier-lader gaf `unknown field 'statuteAnnotations'`. Na de
bouw geeft het frontend de golden bytegelijk uit. Negatieve gevallen: `wf082-default-empty-annotations` (`[]` op de
basisdefault), `wf082-default-blank-entry` (`[""]`), `wf082-default-padded-entry` (een omringde spatie op een default
in een uitzonderingsgroep, uit `jutterswet-uitsluiting-rgs`); elk `expect.txt` pint de sleutel en `kind=default`.

**Verifier.** WF-082 heeft dezelfde drie sleutels (`annotations-empty`, `annotation-blank`, `annotation-whitespace`),
nu met de parameter `kind` (`rule` of `default`); het sjabloon per sleutel blijft één. De lijsten van de defaults
worden naast het document gelezen (`Loader.default_annotations`, `Checks.default_annotations`, aangeroepen door
`bin/verifier.ml` in beide modi), zoals de vocabulairetabel: het OCaml-record van een default wordt door de Catala- en
F-logic-frontends heel gebouwd (voorlopig besluit 2). De lader leest het veld structureel voor elke lezer; ref en
clingo lezen er niets van.

**Wat verschoof.** Elf rgs-goldens opnieuw uitgegeven, precies de 27 vervangingen `source` → `statuteAnnotations` en
niets anders: `jutterswet-dringende-melding`, `-meldtermijn-strandjutter`, `-nestkenmerk-rgs`, `-opgave-bedrag`,
`-opgave-boven-berekening`, `-opgave-datum`, `-opgave-subtype`, `-opgave-vormen`, `-schorsing`, `-uitsluiting-rgs`,
`ww-herleving-inkomen-rgs`; hun NOTE.md waar die de `source` van een default beschreef. Geen `toka/`-slice en geen
`anchor/` veranderde. Frontendtests die het oude gedrag pinden, op de nieuwe vorm gezet (de kaart verandert dat
gedrag uitdrukkelijk): `DefaultsSourceAnnotationTest` (de lijst op de default, de groep ongewijzigd; nieuw:
`an_annotation_line_is_one_opaque_entry`), `ExceptionOnExceptionTest` (een geneste default),
`InputMarkingTest` (de initialisatiedefault, twee wire-pins). `grammar/DIVERGENCES.md` (de annotaties van gelifte
regels) en IR-SPEC ch. 3.1, 6.1, WF-082 en de indexrij bijgewerkt (voorstel).

**Gate 13.** De naamleg stript in dezelfde kopie ook elke `statuteAnnotations` van een default, met een eigen telling
en vloer `EXPECTED_MIN_ANNOTATIONS_STRIPPED=29` (29 lijsten over 12 goldens; 21 goldens, 67 namen). Past in de leg:
beide velden staan op dezelfde knopen en zijn zonder semantiek; elke geannoteerde default draagt ook een naam.
Gate 13 groen: "name leg: 21 golden(s) with named or annotated defaults, floor 20; 67 names stripped (floor 64), 29
statuteAnnotations lists (floor 29); 266 executor replays reproduce the untouched outcome". Rood eerst, gemeten met
een mutant (niet gecommit): de Drools-lader laat een wortel-default vallen op de tekst van een van zijn annotaties;
de leg wordt rood op `jutterswet-schorsing` en `jutterswet-tweeledige-grondslag`, op beide takken
(`u7/logs/gate13-mutant.log`); daarna is de lader gelijk aan HEAD en de jar opnieuw gebouwd.

**Gates die de bouwer draaide** (logs in `u7/logs/`), op de eindstand:
- frontend-suite 1222 tests groen (`fe-test-2.log`, en in gate 10);
- Drools-suite (`mvn -o package`) 1649 tests, 0 fouten, 0 overgeslagen (`drools-suite-1.log`);
- gate 1 PASS; gate 2 PASS (509 shape-rijen, 5 nieuw voor `Default.statuteAnnotations`; 352 bestanden); gate 3 PASS
  (514 negatieve gevallen, 357 op hun arm, 575 name=value-regels, 622 wijzers; geen vloer verschoven); gate 4 PASS
  (888 cases);
- gate 5 PASS (843 cases), gate 6 PASS (843 cases), gate 7 PASS (798 cases, 45 overlevers; de nieuwe golden zet in elke
  case K neer, wiens recht wordt afgeleid, zodat geen case op de ruwe invoer slaagt);
- gate 10 PASS (127 rgs-goldens bytegelijk), gate 11 PASS (geen slice veranderd), gate 12 `--fast` PASS met
  `EXPECTED_PLAYED` 263 (eerst rood op 262: de nieuwe golden speelt, PLAYED 4 op beide takken), gate 13 PASS, gate 16
  PASS (32 scenario-goldens, 110 cases).
Niet gedraaid: 8, 9, 12 volledig, 14, 15, 17.

**Ondoorzichtigheid (eis van de eigenaar, via de hoofdsessie).** uchar draagt per `// Bron:`-regel één string,
getrimd, in bronvolgorde, en niets meer. Geen code, spec-zin, verifier-arm of test knipt een entry of kent er structuur
in; IR-SPEC ch. 3.1 ("Each entry is opaque"), ch. 6.1 en WF-082 zeggen dat met zoveel woorden, ook het schema en het
record `Default`. De golden en de negatieve gevallen gebruiken eigen Jutterswet-tekst.

**Voorlopige besluiten** (ook in de kaart):
1. De groep houdt zijn samengevoegde `source` (`grammar/DIVERGENCES.md` legt die plaatsing met een eigen reden vast);
   de basisdefault draagt dezelfde annotaties als lijst. Een vervolgkaart kan de groep dezelfde lijst geven of zijn
   `source` laten vallen.
2. De verifier leest de lijst naast het document (zie boven).
3. De WF-082-arm draagt `kind`.
4. Gate 13 neemt de annotaties mee in de naamleg.

**Open.** Acceptatie (c), de toolchain: pin-bump en `Verbaliser.groundOf` (leest nu `v.d().source()`, na de bump null
voor elke default uit een blok) in één stap; zie `u7/overdracht.md`. Gate 12 `EXPECTED_PLAYED` 262 → 263; U6 verhoogt
dezelfde constante, bij het rebasen tellen beide op.

**Voorgestelde `CLAUDE.md`-tekst.**

Gate 3, achter "`EXPECTED_MIN_POINTERS` to 619.":
> Since 2026-10-21 (card `default-statute-annotations`) WF-082 judges the `statuteAnnotations` of a Default too
> (IR-SPEC ch. 6.1); the verifier reads those lists beside the document (`Loader.default_annotations`), and each of
> the three WF-082 keys carries the parameter `kind` (`rule` or `default`).

Gate 13, achter "(measured 2026-10-21)." van de NAME leg:
> Since the card `default-statute-annotations` (2026-10-21) the same stripped copy also loses every
> `statuteAnnotations` of a Default (IR-SPEC ch. 6.1), with its own floor `EXPECTED_MIN_ANNOTATIONS_STRIPPED=29`
> (measured 2026-10-21).

**CONSTRUCTS-rij (tekst, rij 44).**
> | 44 | Statute annotations of a default — the `// Bron:` lines above its block | a consumer that shows where a deciding
> default comes from needs the annotations of its block one by one; the RegelSpraak frontend joined them with `"; "`
> into the default's `source` snippet, from which they could not be recovered | core (metadata) | neither: an
> extension field without semantics | v0 has none. **Landed (2026-10-21, card `default-statute-annotations`):**
> optional `statuteAnnotations` on `Default`, written last, the field a Rule carries (IR-SPEC ch. 3.1 and 6.1), under
> WF-082 with a verifier arm per key (`kind=default`) and three negative witnesses; each entry is an opaque string per
> annotation line, read or parsed by no executor or verifier beyond WF-082's form rules. The frontend writes the
> annotations of the block a default comes from and no longer a `source` on such a default; Catala, F-logic and the
> hand-written anchors write none. The Drools loader keeps the list, the shared OCaml loader reads it for its
> structure and the verifier beside the document. Witnesses: the golden `jutterswet-tweeledige-grondslag`, 11
> re-emitted rgs goldens (27 lists), `DefaultsSourceAnnotationTest`; gate 13's name leg holds the executors
> indifferent to the field stripped | out: the `source` of a defaults group named after its base keeps that base's
> annotations joined with `"; "` (`grammar/DIVERGENCES.md`) |

### Volledige set, door de hoofdsessie

Op `4373c173` (`werk/defaults-decider/u7/gates-1`): alle zeventien `rc=0`, `EXIT 0`. Gate 12 744 s, played 264 op
beide takken; gate 13 199 s, de naamleg met 21 goldens, 67 namen en 29 lijsten `statuteAnnotations` gestript, 266
replays gelijk; gate 17 386 s.
