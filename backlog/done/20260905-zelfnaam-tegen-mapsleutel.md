# De zelfnaam tegen de mapsleutel — één klasse, vijf tabellen

Goal: beslissen of een gesleutelde declaratie's **redundante zelfnaam** met
zijn mapsleutel moet overeenkomen, en dat als één regel over de hele klasse
sluiten of met een gemeten argument open verklaren. Vandaag kruist niets ze,
in geen enkele tabel.

## De meting (hermeten 2026-09-06, vier zones: 155 golden + 22 rood + 316 negatief + 30 pariteit = 523 documenten)

De v0-wire slaat vijf declaratiesoorten op als **map**, en elke waarde herhaalt
zijn eigen naam in een `naam`-veld:

| tabel | zelfnaam aanwezig | mee eens met de sleutel | wie leest de zelfnaam |
| --- | --- | --- | --- |
| `eenheidstelsels` | 205 van 205 | 205 van 205, byte voor byte | alleen de foutregel van de ASP-normalisatie (`unit stelsel %S declares no plain base unit`) |
| `objecttypen` | te meten | te meten | WF-057 (tekstveiligheid), verder niets |
| `feittypen` | te meten | te meten | WF-057 |
| `dimensies` | te meten | te meten | WF-057 |
| `tijdlijnen` (lijst, geen map) | n.v.t. | n.v.t. | de naam **is** hier de sleutel |

Elke lezer die er echt iets mee doet — resolutie, stratificatie, uitvoering —
gebruikt de **mapsleutel**, nooit de zelfnaam. WF-057 kruist beide op
tekstveiligheid maar vergelijkt ze niet met elkaar.

## Waarom dit een eigen kaart is en niet de zesde WF-034-golf

De zesde golf (`backlog/done/20260905-wf034-zesde-golf-restvelden.md`) kreeg
`Eenheidstelsel.naam` op de rol en heeft hem **gemotiveerd open gelaten**, in
de open-lijst van `spec/IR-SPEC.md` ch. 4.6. De reden staat daar en geldt
onverkort:

- Het veld is **geen referentie**. WF-034 gaat over verwijzingen die moeten
  resolven; hier is de vraag of een spiegelveld met zijn sleutel overeenkomt.
  Dat is de naam-identiteit van ch. 2.1, WF-001/WF-003's buurt. *Sinds de
  fixronde van 2026-09-06 heeft die buurt een werktuig:* ch. 2.1 draagt twee
  regels, een **dracht**regel (namen worden verbatim gedragen) en een
  **matching**regel (matchen is hoofdletterongevoelig), en ch. 4.6 (p) laat
  zien hoe je per positie kiest welke geldt. Deze kaart moet die keuze per
  tabel expliciet maken in plaats van "ci" als vanzelfsprekend te nemen.
- Het is één instantie van een **klasse**. Alleen `eenheidstelsels` sluiten is
  willekeurig: dezelfde vorm zit op objecttypen, feittypen en dimensies, en
  een regel die er één pakt laat de andere drie precies zo open als ze waren.

## Wat deze kaart moet opleveren

1. De ontbrekende cellen van de tabel hierboven meten (aanwezigheid, overeenkomst
   met de sleutel, lezers per consument: `spec/verifier/`, `ref/`,
   `backends/clingo`, `backends/drools`).
2. De **schade** meten: wat doen de drie uitvoerders met een zelfnaam die van
   zijn sleutel afwijkt? Als niemand hem leest is de schade nul en hoort het
   antwoord "presentatie" te zijn — het patroon dat de zesde golf voor de
   `regels`-stubs gebruikte (ch. 3, niet-normatief, met de gedateerde meting).
3. Eén beslissing voor de hele klasse, rood-eerst uitgevoerd: óf één regel met
   per tabel een arm-exacte mutant en een eigen negatief, óf een
   niet-normatieve presentatie-alinea met de meting erin.

## Acceptance

- Elke cel van de tabel gemeten en gedateerd, in de kaart én in IR-SPEC.
- Bij sluiten: per tabel een negatief, rood-eerst zichtbaar, arm-exacte mutant,
  nul over-afwijzing over de vier zones.
- Bij open verklaren: de presentatie-alinea met de meting, en de open-lijst van
  ch. 4.6 die naar die alinea wijst in plaats van naar zichzelf.
- Alle dertien gates groen.

May touch: `spec/IR-SPEC.md` (voorstel), `spec/verifier/**` (voorstel),
`corpus/negatief/**`, `backlog/`.

## Out of scope

- `Eenheid.stelsel` — dat is wél een referentie en is 2026-09-05 gesloten
  (ch. 4.6 (r)).
- WF-009's eenhedenvraag (of een `afkorting` een eenheid van het genoemde
  stelsel is): eigen regel, eigen kaart.

---

## Result (2026-09-06) — GESLOTEN met één regel, byte-exact: WF-094

**Uitkomst: de kaart sluit, hij verklaart niet open.** De schade is niet nul en
ook niet klein: één hoofdletter verschil op een parameter-zelfnaam laat de
verifier, `ref` én de ASP-backend passeren terwijl de bean-model-uitvoerder
**foute waarden** antwoordt. Dat is de differentiële ruit die openbreekt op een
verifier-schoon document, en het presentatie-patroon (de niet-normatieve alinea
naar het model van de `regels`-stubs) zou dat hebben toegedekt.

### 1. De tabel, elke cel gemeten (2026-09-06, vier zones: 155 golden + 22 rood + 320 negatief + 30 pariteit = **527** documenten vóór deze kaart)

| tabel | entries | zelfnaam aanwezig | = sleutel, byte | alleen ci gelijk | verschilt | verifier | `ref` | clingo | Drools |
| --- | ---: | ---: | ---: | ---: | ---: | --- | --- | --- | ---: |
| `objecttypen` | 744 (205/31/418/90) | 744 | 744 | 0 | 0 | WF-057 (zelfnaam, tekstveiligheid; nooit tegen de sleutel) | — | — | **105** |
| `parameters` | 654 (34/9/71/540) | 654 | 654 | 0 | 0 | — (alleen de SLEUTEL, WF-057) | — | — | **32** |
| `feittypen` | 301 (53/11/117/120) | 301 | 301 | 0 | 0 | WF-057 (zelfnaam) | — | — | **3** |
| `eenheidstelsels` | 209 (21/0/98/90) | 209 | 209 | 0 | 0 | — (alleen de SLEUTEL, WF-057) | — | **1**, alleen in twee foutteksten | **0** |
| `dimensies` | 11 (2/0/9/0) | 11 | 11 | 0 | 0 | WF-057 (zelfnaam) | — | — | **1** |
| `tijdlijnen` (lijst) | — | n.v.t. | n.v.t. | — | — | de naam **is** de identiteit | | | |

Totaal **1919** gesleutelde declaraties; alle vijf de tabellen zijn 100 %
aanwezig en 100 % byte-gelijk aan hun sleutel. Volgorde van de zonecijfers:
golden/rood/negatief/pariteit.

**Correctie op de kaart zelf, en op de open-lijst-post die hij erfde:** de
vijfde map is **`parameters`**, niet `tijdlijnen` (dat is een lijst zonder
sleutel). De kaart telde vier maps + één lijst en noemde dat vijf maps.
`parameters` bleek juist de tabel met de ergste schade — de post in ch. 4.6
noemde als enige instantie `Eenheidstelsel.naam`, de tabel met **nul** lezers.

**Lezers, per consument geteld (niet aangenomen).** De telling is gedaan met
een compileersonde: in een `git archive`-kopie kreeg elk van de vijf
`naam`-velden een andere naam, en `mvn compile` noemde vervolgens elke
leespositie. Uitslag per veld: `Objecttype.naam` 105 (DrlTranspiler 41,
ExecModelTranspiler 33, CaseRunner 8, FactModelGenerator 8, ContractValidator
6, ProducentConsument 4, RegelSpraakModel 3, Stratificatie 2),
`Parameter.naam()` 32, `Feittype.naam` 3, `Dimensie.naam()` 1,
`Eenheidstelsel.naam` 0. In `ref/lib` nul treffers voor alle vijf; in
`backends/clingo/lib` één (`normalise.ml`, `es_naam` in twee foutteksten); in
`spec/verifier` drie (WF-057, tekstveiligheid, nooit tegen de sleutel).
**De kern:** de Drools-loader neemt de ZELFNAAM als de naam van de declaratie
(`text(n,"naam") != null ? text(n,"naam") : key`) — en voor een parameter en
een dimensie is er niet eens die terugval. Sleutel en zelfnaam staan daar dus
elk aan één kant van dezelfde executor.

### 2. Schademeting per tabel — letterlijke uitvoer

Instrument: per golden één zelfnaam verdraaid, `git archive HEAD <golden> |
tar -x` naar `/tmp`, daarna verifier + `ref` + clingo + de Drools-replay. Twee
verdraaiingen, elk over alle 315 posities: **(a)** één karakter
geherkapitaliseerd (ci-gelijk aan de sleutel) en **(b)** `<naam>zz`
(ci-verschillend).

**`parameters` — luid bij de poort, STIL ANDERS in de executor (11 van 34, beide verdraaiingen):**

```
CHANGED corpus/golden/jutterswet-aanvoergeld  parameters['aanvoertarief'] 'aanvoertarief' -> 'aanvoertarieF'
    drools   BEFORE PLAYED 1
             AFTER  FAIL case.json: attribuut aanvoergeld: verwacht 322.50, werkelijk 310.00;
                         attribuut aanvoergeld: verwacht 99.90, werkelijk 87.40;
                         attribuut aanvoergeld: verwacht 12.50, werkelijk 0.00
CHANGED corpus/golden/synth3-takbarriere  parameters['drempel'] 'drempel' -> 'drempeL'
    drools   BEFORE PLAYED 1
             AFTER  FAIL case.json: attribuut heffing: verwacht 10.00, werkelijk leeg
```

Zeven documenten, elf posities: `jutterswet-aanvoergeld` (2),
`jutterswet-penningdrempel`, `jutterswet-penningjaar`,
`jutterswet-schouwkalender`, `jutterswet-sleeprest`, `synth3-drempelheffing`
(3), `synth3-takbarriere` (2). Verifier, `ref` en clingo bewegen op geen ervan.
Kanaal: de case bindt zijn globals onder `Namen.ident(<geschreven naam>)`, de
transpiler registreert ze onder `globalNaam(<zelfnaam>)`; het
"global not declared" dat volgt wordt bij ontwerp ingeslikt.

**`objecttypen` — ci-gelijk: NIETS beweegt (0 van 205 verdicten); ci-verschillend: 4 van 205:**

```
CHANGED corpus/golden/anchor/jutterswet-maatschap  objecttypen['Jutter'] 'Jutter' -> 'Jutterzz'
    drools   BEFORE PLAYED 1
             AFTER  EXCLUDE transpiler SKIP: regel: aantal maten van een jutter — regel
                    (het beanmodel leidt voor de enkelvoudige rol 'maat' op Jutterzz
                     geen referentieveld naar Jutterzz af (§13.3.9 opslagrichting))
CHANGED corpus/golden/jutterswet-aanmelderkrediet  objecttypen['Jutter'] 'Jutter' -> 'Jutterzz'
    drools   BEFORE PLAYED 1
             AFTER  ERROR IllegalStateException: geen getter voor 'strandmaat'
                    op io.github.yhavinga.regelspraak.facts.Jutterzz
```

Drie keer PLAYED → EXCLUDE (`anchor/jutterswet-maatschap`,
`anchor/jutterswet-wachtersketen`, `synth2-budgetgrens-onder`) — **stil**: de
golden valt uit de ruit en gate 12 blijft groen, want zijn enige vloer is een
niet-lege speelverzameling. Eén keer PLAYED → ERROR — **luid**. Drie verdere
posities verschuiven alleen de TEKST van een uitsluiting die er al was.

**`feittypen` — 0 van 53, beide verdraaiingen. `eenheidstelsels` — 0 van 21,
beide verdraaiingen. `dimensies` — 0 van 2, en dat nul is structureel:** de
enige twee goldens met een `dimensies`-tabel zijn de verifier-only
getuigenkast-documenten zonder `case*.json`, want geen enkele uitvoerder voert
een dimensiecel uit (`ref`: *expression kind not implemented*; clingo:
*expression kind not translated*; Drools: auto-uitsluiting op een
gedimensioneerd attribuut).

**Een valkuil die de meting bijna had gemist, en die zelf bewijs is.** Een
verdraaiing die het HELE woord van kast wisselt meet op `parameters` **nul**:
`Namen.ident` title-cased een ALL-CAPS woord, dus `AANVOERTARIEF` vouwt terug
op `aanvoertarief`. De schade is precies zichtbaar wanneer de verdraaiing die
normalisatie OVERLEEFT — wat aantoont dat het identifier-contract het kanaal is.

### 3. De beslissing en waarom, met de prijs van het alternatief

**WF-094, ch. 2.1, byte-exact, vijf armen** (`objecttypen`, `parameters`,
`eenheidstelsels`, `feittypen`, `dimensies`).

*Waarom sluiten en niet presenteren.* Het presentatie-patroon eist schade nul.
Die is er niet: 11 gouden parameterposities laten drie consumenten passeren en
de vierde fout rekenen. Een niet-normatieve alinea zou precies het document
hebben vrijgelaten dat de ruit breekt, en de prijs was betaald in stilte —
gate 12 ziet een FAIL wel, maar alleen omdat het toevallig een *waarde*verschil
is; bij de objecttypen-arm is de uitkomst een uitsluiting die geen gate telt.

*Waarom dracht (byte) en niet matching (ci) — het ch.-2.1-frame, per tabel
expliciet.* De zelfnaam is **geen referentie**: er hoeft niets te resolven. Hij
is de identiteit van de declaratie een tweede keer opgeschreven — de
v0-denormalisatie van het ENE `name` dat een v1-declaratie op zijn knoop
draagt. Dat is de **drachtregel**, net als bij de zijtabelsleutels van ch. 4.6
(p), en niet de matchingregel van (q)/(r), waar een naam ergens naar WIJST.
De meting bevestigt het aan de dure kant: op `parameters` is de ci-gelijke
verdraaiing precies zo schadelijk als de ci-verschillende — elf posities, foute
waarden. **Een ci-regel had die elf documenten toegelaten.** Dat is de prijs van
de andere kant, gemeten en niet beredeneerd.
Op de vier andere tabellen kost byte niets (0 van 1919 entries wijkt af) en
levert het geen gemeten winst op de kapitalisatieflank; daar rust de arm op het
CONSTRUCT — dezelfde grond als de twee lezerloze zijtabel-armen van (p) — plus,
op `objecttypen`, één byte-exacte lezer die het corpus vandaag niet kan
aanspreken: de §9.9-startpuntanker-opzoeking vergelijkt
`resolvedStartpunten[].objecttype` met `String.equals` tegen de zelfnaam, en de
enige golden met zo'n entry is een verifier-only getuige zonder case.
Eén regel per tabel splitsen zou zeggen dat de zelfnaam per tabel iets anders
betekent; dat doet hij niet.

*De ASP-foutregel-lezer, expliciet gewogen.* `es_naam` staat in twee
foutteksten van de fijnste-eenheidsafleiding. Die lezer kan niet veranderen ÓF
de backend weigert, alleen wat de weigering zégt — en geen enkele
`expectedFault` in `corpus/rood` pint een van beide fragmenten, dus zelfs de
tekst heeft geen waarneembaar kanaal. Hij telt dus **niet** als semantische
lezer; `eenheidstelsels` is de arm met nul lezers en staat er op de
wire-grond, niet op schade.

### 4. Rood-eerst, mutanten, over-afwijzing

Verifier van de branch-kop: `dune build --root spec/verifier`, binary
`spec/verifier/_build/default/bin/verifier.exe`, md5
**957d3eeb5667188e33d5468b80f21b71** — alle vijf negatiefs `verifier: PASS`
(commit `af9ae31`). Na de regel: md5
**f6b83c4f25cab090c8bea95a75a089ea** — alle vijf `WF-094 FAIL`, elk citeert
alleen WF-094.

| negatief | bron-golden | verdraaiing | rood-eerst | na de regel |
| --- | --- | --- | --- | --- |
| `wf094-zelfnaam-objecttype` | `anchor/jutterswet-maatschap` | `Jutter` → `JutteR` | PASS | WF-094 FAIL `/objecttypen/Jutter/naam` |
| `wf094-zelfnaam-parameter` | `jutterswet-penningdrempel` | `vondstdrempel` → `vondstdrempeL` | PASS | WF-094 FAIL `/parameters/vondstdrempel/naam` |
| `wf094-zelfnaam-eenheidstelsel` | `jutterswet-berging` | `Munt` → `MunT` | PASS | WF-094 FAIL `/eenheidstelsels/Munt/naam` |
| `wf094-zelfnaam-feittype` | `jutterswet-vondsttelling` | `vondstaanmelding` → `vondstaanmeldinG` | PASS | WF-094 FAIL `/feittypen/vondstaanmelding/naam` |
| `wf094-zelfnaam-dimensie` | `jutterswet-seizoenvondst` | `seizoen` → `seizoeN` | PASS | WF-094 FAIL `/dimensies/seizoen/naam` |

**Nul over-afwijzing, direct gemeten.** De verifier van vóór de regel is
opnieuw gebouwd uit een `git archive` van `af9ae31` en reproduceert md5
`957d3eeb…`; beide binaries over alle **532** documenten van de vier zones:
5 rapporten bewegen, 5 verdicten, en het zijn precies de vijf nieuwe getuigen.
Geen golden, geen rood, geen pariteitsdocument, geen bestaand negatief beweegt
— ook geen incidentele extra regel.

**Mutanten** (elk een `git archive`-kopie, één bewerking, harde bouwfaal,
`test -x`, md5 per sweep, daarna de volle vier-zone-sweep tegen de
branch-verdicten):

| mutant | binary-md5 | vrijgemaakt |
| --- | --- | --- |
| arm `objecttypen` weg | `b04b64f8c305e84b06eaec452674ce12` | alleen `wf094-zelfnaam-objecttype` |
| arm `parameters` weg | `91a36297abfd279606c55acc1b98f12e` | alleen `wf094-zelfnaam-parameter` |
| arm `eenheidstelsels` weg | `596a85818113788c38ecaad388486da1` | alleen `wf094-zelfnaam-eenheidstelsel` |
| arm `feittypen` weg | `b36b56a8b9a6a0b69d2491be85cea343` | alleen `wf094-zelfnaam-feittype` |
| arm `dimensies` weg | `cb147dc0824605965b6fe43d0c8186ae` | alleen `wf094-zelfnaam-dimensie` |
| `n <> key` → `norm n <> norm key` (ci-terugval) | `e85bf5f019c490c138a27f68af5cda18` | **alle vijf** |

Recept voor de ci-terugval: in `spec/verifier/lib/checks.ml`, in `wf094`, de
regel `| Some n when n <> key ->` vervangen door
`| Some n when norm n <> norm key ->`. Recept voor een arm: de betreffende
`arm "<tabel>" …`-regel uit de `@`-keten schrappen. Geen mutant maakte ergens
anders iets schoon of vies.

### 5. De dertien gates, vanuit de worktree

| gate | uitslag |
| --- | --- |
| 1 `check-self-contained` | PASS |
| 2 `check-ir-schema` | PASS (schema valid; **177** files validate and round-trip) |
| 3 `check-verifier` | PASS (**177** golden clean; **325** negatief rejected for their declared rule) |
| 4 `check-case-schema` | PASS (schema valid; **429** case files) |
| 5 `check-oracle` | PASS (**407** cases replayed exactly; 2 goldens zonder case) |
| 6 `check-clingo` | PASS (**407** cases replay exactly — ref ≡ clingo) |
| 7 `check-noop` | PASS (**371** cases refute a do-nothing executor; **36** survivors) |
| 8 `check-rood` | PASS (**22** red cases; Drools-been 5 fault-pinned, 17 not-carried) |
| 9 `check-catala` | PASS (**201** checks; **12** catala goldens byte-identiek) |
| 10 `check-regelspraak` | PASS (**173** tests; **30** rgs goldens byte-identiek) |
| 11 `check-toka-parity` | PASS (45 rgs re-emit, 27 TOKA-tests, 29 replays ref/clingo/Drools, 0 divergenties) |
| 12 `check-drools` | PASS (played **109** / auto-excluded **43** / value-divergence **1** / no-case **2** / JUnit **207**) |
| 13 `check-reftype-derivation` | PASS (**68** annotaties over **14** goldens; **0** survivors) |

Alle dertien met hun echte exitcode gemeten (niet door een `tail` heen).
Gate 3 gaat van 320 naar **325** negatiefs; alle andere tellingen staan
ongewijzigd op de door de kaart verwachte waarden.

### 6. Bijvangst, expliciet

1. **`parameters` is de vijfde map, niet `tijdlijnen`.** De kaart en de
   open-lijst-post telden de klasse verkeerd. De ontbrekende tabel is de enige
   met gemeten schade. Beide zijn gecorrigeerd in `IR-SPEC.md`.
2. **De Drools-loader verkiest de zelfnaam boven de sleutel**, en voor
   `Parameter` en `Dimensie` valt hij niet eens terug op de sleutel — een
   `naam: null` geeft daar `Parameter(null, …)`. Dat is de LEGE helft; ze staat
   nu in de open lijst van ch. 4.6 op de plek die `Eenheidstelsel.naam`
   achterliet, met de meting erbij (nul voorkomens in alle vier de zones, dus
   geen getuige mogelijk vandaag).
3. **Eén byte-exacte lezer zonder corpusgetuige:** `isStartpuntLid` /
   `startpuntLid` vergelijken `resolvedStartpunten[].objecttype` met
   `String.equals` tegen de zelfnaam van het objecttype. Het enige golden met
   een `resolvedStartpunten`-entry (`jutterswet-vormenkabinet`) heeft geen case.
   Benoemd in de NOTE van `wf094-zelfnaam-objecttype`.
4. **De ci-fallback van `dimensieOpNaam`** indexeert de dimensietabel een
   tweede keer op de ZELFNAAM (exact op de sleutel eerst, dan ci over de
   zelfnamen). Benoemd in de NOTE van `wf094-zelfnaam-dimensie`.
5. **Geen enkele `expectedFault` in `corpus/rood` pint de ASP-fouttekst** die
   `es_naam` splitst — de enige "lezer" van die tabel heeft dus ook op de
   tekstflank geen kanaal.
6. **Reproduceerbaarheidsanker:** de pre-regel-verifier herbouwt uit
   `git archive af9ae31` tot dezelfde md5 `957d3eeb5667188e33d5468b80f21b71`.

### 7. Twijfels en wat NIET is gedaan

- De byte-regel op `feittypen`, `eenheidstelsels` en `dimensies` rust op het
  construct, niet op gemeten schade op de kapitalisatieflank. Dat staat zo in
  de clausule en in de drie NOTEs; wie het anders weegt, weegt precies die drie
  armen, niet de regel.
- `naam: null` is **niet** gesloten (geen getuige, nul voorkomens). Eigen kaart.
- `spec/CONSTRUCTS.md` en `spec/PLAN.md` zijn niet aangeraakt; de
  `dagsoortPerEenheid`-post in dezelfde open lijst is niet aangeraakt.
- `spec/IR-SPEC.md` is een beschermd pad: deze branch is een **voorstel** en is
  niet gemerged.
