# verifier-supertype-dubbele-schrijver: gewone regels op super- én subtype voor één doel, zonder defaults-groep

Bijvangst van de onderzoekskaart dialect-defaults-niet-en-nesting (gemeten,
oorzaak toen niet uitgezocht); het voorstel-concept
`spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md` (§9, kaartbranch) geeft
de wortel: de verifier neemt in WF-079 aan dat een kale regel van het
subtype die van het supertype vervangt (`spec/verifier/lib/checks.ml:8899–8903`),
terwijl RUNTIME-SEMANTICS ch. 7.10 een kale regel alleen in een stapel
vouwt wanneer de keten een defaults-groep declareert
(`RUNTIME-SEMANTICS.md:5179–5184`; `spec/verifier/lib/desugar.ml:286–290`).
Gemeten: de verifier zegt niets; ref en de Drools-exec-tak vinden geen
fixpunt; clingo geeft twee waarden; de DRL-teksttak antwoordt naar
bronvolgorde. Vier uitvoerders, drie antwoorden, en de spec beslist het
geval niet.

## Te beslissen bij oppakken (spec-vraag, beschermd pad)

- (a) De stapelvervanging van ch. 7.10 geldt ook voor kale regels: de kale
  regel van het subtype vervangt die van het supertype op instanties van
  het subtype (het Flora-beginsel dat SUBTYPING §4.4 al voor blokken
  volgt). Dan: ref, clingo en beide Drools-takken implementeren het, en
  een ankercase pint het.
- (b) Twee kale schrijvers op één doel in één keten zijn ill-formed: WF-079
  (of WF-026 op de effectieve stapel, IR-SPEC ch. 6.1) weigert, met een
  negatief; de uitdrukbare vorm is een defaults-groep (basis op het
  supertype, eigen basis op het subtype), die ch. 7.10 al draagt.

Aanbeveling: (b) — het is de zin die IR-SPEC ch. 6.1 al uitspreekt ("the
single-writer rules … are evaluated on the per-type EFFECTIVE stack"), en
(a) voegt een tweede vervangingsmechanisme naast de defaults-groep toe.

## Acceptatie (bij oppakken aanscherpen)

- (a) Reproductie op alle vier uitvoerders in de kaart vastgelegd (het
  M7-document van het voorstel of een eigen rechtenvrije variant).
- (b) De gekozen weg: bij (b) een negatief `wf079-…` of `wf026-…`
  (**voorstel**), de verifier rood-eerst, en de spec-zin die het geval
  benoemt (**voorstel**); bij (a) de spec-zin, drie uitvoerders en een
  ankercase, allen exact.
- (c) Alle veertien gates groen.

May touch: `spec/verifier/**`, `spec/IR-SPEC.md`,
`spec/RUNTIME-SEMANTICS.md`, `corpus/negatief/**` (alle **voorstel**); bij
(a) `ref/**`, `backends/**`, `corpus/golden/anchor/**` (**voorstel**);
kaartverplaatsing.

Niet in deze kaart: de dialectvormen.

## Correcties bij de claim (2026-09-25, hoofdsessie)

De kaart is op 2026-09-23 op de L1-wire geschreven; de repo staat op de
L5-wire (`uchar-ir-v1` compleet, main 2f47d80e).

- Regelnummers in de kaart zijn van 2026-09-23 en gelden niet meer. Vandaag:
  de WF-079-arm en zijn comment in `spec/verifier/lib/checks.ml` r. 7611–7672
  (`let wf079`; de onware comment "A bare rule on the supertype and a bare
  rule on the subtype are not two writers" r. 7624–7628); de poortalinea van
  RUNTIME-SEMANTICS ch. 7.10 ("The fold is gated…", "Outside the gate nothing
  changes … WF-079 is what speaks about them") r. 5487–5501;
  `spec/verifier/lib/desugar.ml` r. 278–300 (de poort) en `absorbed_uncached`
  r. 526 e.v.; IR-SPEC ch. 6.1 "the per-type EFFECTIVE stack" r. 8383–8397;
  de WF-079-tekst in IR-SPEC r. 2258–2284.
- Het voorstel `spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md` is geland
  op main; §9.4 (r. 1425–1466) draagt het meetdocument en de wortel.
- Wire-namen van het meetdocument: `model.objectTypes[]` met `supertype`;
  `rules.rules[]` `{node: Rule, name, versions[]}` met per versie `target`
  een `AttributeReference {attribute, subjectType}`, `presentation:
  "equation"`, geen `condition`, `validity` afwezig; de uitdrukbare vorm is
  een `PrioritizedDefaults {subject, attribute, defaults[]}` per type.
- De verifier meldt met JSON-pointers in L5-spelling
  (`/rules/rules/<f>/versions/<v>/…`).

## U-besluiten (hoofdsessie, 2026-09-25)

- U1 **Norm: optie (b).** Twee kale schrijvers (onvoorwaardelijke,
  versievrije rekenende versies van verschillende vormen met overlappende
  geldigheid) op één doel in één typeketen zonder defaults-groep zijn
  ill-formed. De poort van ch. 7.10 blijft staan; WF-079 keurt voortaan per
  CONCREET type de effectieve stapel: elke onvoorwaardelijke rekenende versie
  op (T of een voorouder van T, attribuut) die niet in een stapel is
  opgenomen telt als schrijver op T (buiten de poort is dat elke zulke
  versie; binnen de poort is de opname `Desugar`'s zaak en spreekt WF-050).
  De bestaande vlakke arm en haar zin blijven; de ketenarm krijgt een eigen
  zin die beide declarerende typen noemt en de uitdrukbare vorm (een
  defaults-groep: basis op het supertype, eigen basis op het subtype — ch.
  7.10 regel 1). Geen nieuw WF-nummer. Optie (a) is verworpen: zij zou de
  poort voor deze paren opheffen en een tweede vervangingsmechanisme naast de
  defaults-groep leggen; wie kind-wint wil, schrijft de groep.
- U2 **Zin** (voorstel, de bouwer stelt de definitieve zin vast en meldt
  haar letterlijk in het Result): `two unconditional computing rules write
  target (Wrakrecht, uitkering) through its type chain — one declared on
  Strandrecht, one on Wrakrecht — and no defaults group folds them into one
  stack, so both fire on a Wrakrecht instance; priority is explicit (a
  defaults group, whose subtype base replaces the inherited base, ch. 7.10)
  or explicitly absent, never implicit agenda order (ch. 7.7)`. Pointer: de
  versie op het laagste type van het paar. Geen regelnummerstaart.
- U3 **Getuigen.** Negatief `wf079-keten-kale-schrijvers` (supertype-regel
  eerst in het document) met `expect.txt` op de ketenarm en NOTE met de
  echte verifieruitvoer; rood-eerst (verifier PASS vóór de arm, gemeten en
  geciteerd); mutatieproef. Positief: elke golden blijft schoon (gate 3),
  in het bijzonder `anchor/jutterswet-strandrechtketen` (keten MET groep),
  `jutterswet-wrakrecht`/`jutterswet-wrakconflict` en elke golden met een
  supertype waar maar één niveau het doel schrijft — geen nieuwe golden
  nodig; ontbreekt een schone drager van "één niveau schrijft, het andere
  niet", dan meld je dat als open punt (geen golden aanmaken zonder
  noodzaak).
- U4 **Reproductie (acceptatie (a)).** Het §9.4-document in L5-vorm, beide
  bronvolgordes, op verifier, `ref`, clingo, Drools exec en DRL-tak; de
  tabel in het Result. De uitvoerders veranderen NIET (optie (b)).
- U5 **Spec.** IR-SPEC WF-079 krijgt de ketenclausule (per concreet type,
  door de keten, buiten de poort van ch. 7.10); ch. 6.1's zin blijft; RS ch.
  7.10 "Outside the gate nothing changes …" krijgt de zin dat WF-079 daar
  per concreet type door de keten spreekt. Geen D-/U-nummers en geen
  procestaal in spec, comment of NOTE. De naamtabel-toets
  (`spec/proposals/v1-naamtabel-toets.py`) blijft rc 0.
- U6 De verifier-comment r. 7624–7628 wordt naar de waarheid herschreven.
- U7 **Zusterregels meten, niet bouwen:** WF-026 (toewijzingen) en WF-050
  keyen ook op het gedeclareerde type; meet met een proefdocument of een
  toewijzing op super- én subtype dezelfde kloof heeft en meld het als
  genummerd open punt. Kenmerktoekenningen blijven vrijgesteld.
- U8 **Toka:** de zone is niet verifier-schoon (28/30 documenten); tel of de
  ketenarm daar extra meldingen geeft (alleen tellingen en paden, geen
  citaten) en meld het.
- U9 **Gates** voor deze bouwer: 1, 2, 3; de unie draait alle veertien.

## Result (2026-09-25, bouwer)

## Result — verifier-supertype-dubbele-schrijver

### Stand

Branch `card/verifier-supertype-dubbele-schrijver` (worktree /Users/yeb/Developer/uchar-dev/wt/supertype),
basis main 2f47d80e, claim 14fae55d, kop **5fb7a988**, werkboom schoon. Commits:

- 73d6c4cb negatief wf079-keten-kale-schrijvers: rood-eerst, vóór de ketenarm
- 6edab414 verifier WF-079: de ketenarm, per concreet type buiten de poort van ch. 7.10
- 08976957 spec: WF-079 leest een typeketen per concreet type, buiten de poort van ch. 7.10
- 5fb7a988 IR-SPEC WF-index: de WF-079-rij noemt de ketenclausule en haar getuigen

Alles op beschermde paden is **voorstel**: spec/verifier/lib/checks.ml, spec/verifier/lib/desugar.ml,
spec/IR-SPEC.md, spec/RUNTIME-SEMANTICS.md, corpus/negatief/wf079-keten-kale-schrijvers/. Geen
scripts gewijzigd. Norm: optie (b) (U1); de uitvoerders zijn niet veranderd.

### De definitieve weigerzin (gelijk aan het U2-voorstel, geen afwijking)

Sjabloon in `wf079` (checks.ml), pointer op de versie van het lagere type, geen regelnummerstaart:

    two unconditional computing rules write target (%s, %s) through its type chain — one declared on %s, one on %s — and no defaults group folds them into one stack, so both fire on a %s instance; priority is explicit (a defaults group, whose subtype base replaces the inherited base, ch. 7.10) or explicitly absent, never implicit agenda order (ch. 7.7)

Op de getuige, letterlijk:

    WF-079 FAIL /rules/rules/1/versions/0: two unconditional computing rules write target (Wrakrecht, uitkering) through its type chain — one declared on Strandrecht, one on Wrakrecht — and no defaults group folds them into one stack, so both fire on a Wrakrecht instance; priority is explicit (a defaults group, whose subtype base replaces the inherited base, ch. 7.10) or explicitly absent, never implicit agenda order (ch. 7.7)

De zin staat alleen in de verifier. WF-079 is een verifierregel. De gedeelde OCaml-lader en de
Drools-lader weigeren deze vorm niet (de zin van de vlakke arm staat daar ook niet), en onder
optie (b) veranderen de uitvoerders niet. `expect.txt` pint het fragment tot en met "so both fire
on a Wrakrecht instance". De vlakke arm en zijn zin zijn ongewijzigd.

### Reproductie (U4), gemeten 2026-09-25

Het §9.4-document als RegelSpraak, geëmitteerd met de frontend van deze worktree
(`Resolver --ir`, L5-wire). Twee bronvolgordes. Case1: W1 (Wrakrecht) en S1 (Strandrecht), beide
met grondslag 10.00, verwacht W1 20.00 en S1 10.00. Case2: alleen S1. Bestanden:
(buiten de repo)

| uitvoerder | Strandrecht-regel eerst | Wrakrecht-regel eerst |
|---|---|---|
| verifier vóór de arm | `verifier: PASS` | `verifier: PASS` |
| ref (case1) | `EVALUATION FAULT: no fixpoint within 100 passes — divergence is a fault (ch. 5.7)` | idem |
| clingo (case1) | `MISMATCH: W1.uitkering = [2000;1000], expected 2000` | `MISMATCH: W1.uitkering = [1000;2000], expected 2000` |
| Drools exec (case1) | `FAIL case1-wrak-en-strand.json: geen fixpunt (kringverwijzing?)` | idem |
| Drools DRL-tak (case1) | `PLAYED 1` (20.00) | `FAIL …: attribuut uitkering: verwacht 20.00, werkelijk 10.00` |
| case2 (alleen S1), alle vier | ref en clingo `PASS (2 expectations)`, exec en DRL `PLAYED 1` | idem |
| verifier met de arm | `WF-079 FAIL /rules/rules/1/versions/0: …` | `WF-079 FAIL /rules/rules/0/versions/0: …` (dezelfde zin) |

Hiermee is de tabel van §9.4 exact gereproduceerd op de L5-wire. Met de arm weigert de verifier
beide volgordes, en de pointer staat telkens op de Wrakrecht-versie.

### De bouw

- **Poortpredicaat gedeeld, niet gedupliceerd.** `Desugar` hangt van `Checks` af, dus `wf079` kon
  `Desugar.chain_group_keys` niet aanroepen. Daarom zijn `target_key` en `chain_group_keys`
  letterlijk van desugar.ml naar checks.ml verhuisd (na `declaring_type`). desugar.ml roept ze nu
  aan als `Checks.target_key` en `Checks.chain_group_keys`. Er is één definitie, en de vouw en
  WF-079 lezen dezelfde poort. Gedrag van de vouw: de acht ketendocumenten
  (anchor/jutterswet-strandrechtketen, wrakrecht, wrakconflict, wrakbasis, wrakerfbasis,
  wrakboeking, wrakrecht-flogic, bergersdeling) spelen na de verhuizing in ref en clingo allemaal
  PASS. Dit zijn directe runner-aanroepen, geen gate.
- **De ketenarm (`chain_pairs`).** Een paar bestaat uit twee onvoorwaardelijke rekenende versies
  van verschillende vormen, met overlappende geldigheid, op hetzelfde attribuut. Ze zijn
  gedeclareerd op twee verschillende typen, waarvan het ene een strikte voorouder van het andere
  is. Het paar wordt alleen gemeld als de keten van het lagere type geen defaults-groep op het
  doel declareert. Er komt één melding per paar, op de versie van het lagere type. Dat type is
  het eerste concrete type waarop beide vuren. De sleutelset van de poort groeit alleen naar
  beneden: een lager type buiten de poort ziet hetzelfde paar, een lager type binnen de poort
  heeft het gevouwen. Dat is gelijkwaardig aan "per concreet type de effectieve stapel keuren"
  (U1).
- **Comments.** De onware comment (r. 7624–7628) is vervangen door een beschrijving van beide
  armen (U6). In de single-writer-noot bij `wf026` klopte "today the fences do not fire on a
  supertype/subtype pair at all" niet meer; die zin gaat nu over de declaratiesleutel, met één
  slotzin over de ketenarm. In desugar.ml noemt het poortcommentaar nu `Checks.chain_group_keys`
  en WF-079.
- **Proeven** ((buiten de repo), geëmitteerd uit RegelSpraak), aantal WF-079-meldingen:

  | proef | meldingen |
  |---|---|
  | drie niveaus zonder groep | 3 |
  | groep bovenaan (wrakbasis-vorm) | 0 |
  | vlak paar op het supertype + regel op het subtype | 3 (1 vlak, 2 keten) |
  | groep onder het lagere type (Diepwrak ⊑ Wrakrecht) | 1, op Wrakrecht |
  | voorwaardelijke subregel | 0 |
  | disjuncte vensters | 0 |
  | overlappende vensters | 1 |
  | zustertypen | 0 |
  | kenmerktoekenningen | 0 |
  | groep op een ander attribuut | 1 |

  Bij "groep onder het lagere type" spelen ref en clingo op Diepwrak `PASS` (30.00, gevouwen); op
  Wrakrecht geeft ref geen fixpunt en clingo `[2000;1000]`. De ketenarm en de uitvoerders zijn
  het dus per concreet type eens. De F-logic-testprogramma's overerving, aggregatie en
  kwantorvouw geven `verifier: PASS`.

### Rood-eerst en mutatieproef (U3)

- **Vóór de arm.** Binary-md5 e0bafac419b97e989317b13194932efe. Een aparte bouw uit
  `git archive 2f47d80e` gaf dezelfde md5. Op de getuige: `verifier: PASS`. Gate 3 rc=1:

      FAIL: negatief case corpus/negatief/wf079-keten-kale-schrijvers/ir.json passed the verifier (must violate WF-079):
      verifier: PASS

- **Met de arm.** md5 76d8ae96003fbfccc0c0edaa1de4faf1. Gate 3 rc=0.
- **Mutant.** Recept: de laatste regel van `wf079` wordt `pairs writers @ (if false then
  chain_pairs computing else [])`. md5 1971a2a9ac1832423a17a1bd9b6262ac. Gate 3 rc=1, met de twee
  FAIL-regels "passed the verifier" en "… not on the arm …expect.txt names". Na herstel: md5 weer
  76d8ae96…, gate 3 rc=0.
- **Arm-exact.** Over 708 documenten (golden, rood, negatief) verschuift precies één verdict en
  verandert precies één rapport: de nieuwe getuige. Dat geldt zowel arm tegen mutant als arm
  tegen basis.

### Gates (elk als losse aanroep na `eval $(opam env)`)

- Gate 1 (op de eindstand 5fb7a988): `check-self-contained: PASS`
- Gate 2: `check-ir-schema: PASS (schema valid; 79 pattern self-test row(s); 504 shape self-test row(s); 289 file(s) validate and round-trip, floor 192)`
- Gate 3: `check-verifier: PASS (289 golden clean; 419 negatief rejected for their declared rule, floor 350; 261 on their declared arm (expect.txt), floor 260)`

Tellingen: negatief-getuigen 418 → 419 en expect.txt 260 → 261. Geen floor verhoogd, want
groei vraagt dat niet.

Alle goldens zijn schoon, waaronder de ankers en ketengoldens: strandrechtketen, wrakrecht,
wrakconflict, wrakbasis, wrakerfbasis, wrakboeking, wrakrecht-flogic en bergersdeling. Er zijn
schone dragers van "één niveau schrijft, het andere niet":

- alleen het supertype schrijft: `registratiekosten` in strandrechtketen, wrakrecht en
  wrakrecht-flogic; `aangegeven gewicht` in muntweging; `vergoeding` in zustervondst;
- alleen het subtype schrijft: `bergingsvergoeding` in wrakboeking; `wachtsom` in erfwacht en in
  rood/datumtijdlijn-klipvouw; `geborgen gewicht` in muntweging;
- zustertypen schrijven elk: `toeslag` op Wrakstuk en op Drijfhout in zustervondst.

Een nieuwe golden was dus niet nodig. Naamtabel-toets: `v1-naamtabel-toets: OK`, rc 0, gedraaid
na elke spec-edit.

### U8 — toka

Resultaat: 30 `ir.json`, waarvan 0 met een supertype-rand.

| | basis | arm |
|---|---|---|
| schone documenten | 2 | 2 |
| FAIL-regels | 46 | 46 |
| WF-079-regels | 1 | 1 |

Documenten met veranderde uitvoer: 0. De ketenarm geeft daar geen extra meldingen, en dat kan
ook niet, want er is geen keten. Alleen tellingen, geen citaten.

### Spec (U5)

- **IR-SPEC WF-079 (ch. 3.1).** Nieuwe clausule "**Through a type chain the rule is read per
  concrete type** …":
  - de schrijvers van het attribuut van T zijn de versies op T en op zijn voorouders;
  - binnen de poort vouwt ch. 7.10; wat de vouw niet kan nemen, weigert zij bij naam; een versie
    naast een groep op haar eigen niveau valt onder WF-050;
  - buiten de poort vormen twee versies op twee typen van de keten een paar, zoals twee op één
    type;
  - de melding citeert de versie op het lagere type.

  In de Motivation staat één zin over het ketenpaar, met de getuige.
- **IR-SPEC ch. 6.1.** De verplichtingszin in de blockquote is ongewijzigd. De toelichtende zin
  "Today those three fences … do **not** fire on a supertype/subtype pair at all" is minimaal
  herschreven tot een uitspraak over de declaratiesleutel, plus één slotzin die naar de
  ketenclausule wijst.
- **IR-SPEC WF-index.** De WF-079-rij noemt de ketenclausule, de ketengoldens binnen de poort en
  de nieuwe negatief.
- **RS ch. 7.10.** Na "… and WF-079 is what speaks about them." staat de nieuwe zin "It speaks
  there per concrete type, through the chain: …", met de getuige.

De wijzigingen in ch. 6.1 en de index gaan verder dan de letter van U5. Ik heb ze gedaan omdat
de spec anders onwaar zou worden over de verifier, en omdat de index elke getuige per regel noemt.

### Wat aan de kaarttekst niet klopte (gemeten)

1. **`validity`.** Correcties bij de claim zeggen "`validity` afwezig". De frontend emitteert op
   elke versie van het meetdocument `"validity": {"node": "Validity"}`, een node zonder grenzen.
   De verifier leest dat, net als afwezigheid, als altijd geldig.
2. **Regelnummers** (op main 2f47d80e):
   - de poortalinea van RS begint op 5486 en eindigt op 5500 (kaart: 5487–5501);
   - het poortcommentaar in desugar.ml staat op 286–290, met `chain_group_keys` op 293 (kaart:
     278–300);
   - `absorbed_uncached` staat op 541; zijn doc-comment begint op 525 (kaart: "526 e.v.");
   - de blockquote van ch. 6.1 staat op 8382–8384 en de toelichting op 8386–8400 (kaart:
     8383–8397).
3. **U3.** Schone dragers van "één niveau schrijft" bestaan (zie Gates). Het voorbehoud "ontbreekt
   een schone drager" is dus niet nodig.
4. **U7 "WF-026 (toewijzingen)".** WF-026 gaat over allocaties (`allocations`, v0 `verdelingen`).
   Gemeten is dus een allocatiepaar.
5. **Optie (b) in de kaartkop.** De kop noemt "of WF-026 op de effectieve stapel". WF-026 dekt
   alleen allocaties en kan het regelpaar niet dragen. U1 koos terecht WF-079.

### Open punten

1. **U7: dezelfde kloof bij allocaties, gemeten, niet gebouwd.** De proeven staan in
   (buiten de repo) Allocatie 1 gaat over "alle bergers" (`targetType` Berger), allocatie 2
   over "alle hoofdbergers" (Hoofdberger ⊑ Berger), en beide schrijven `bergingsaandeel`:

   | uitvoerder | super eerst | sub eerst | vlakke controle |
   |---|---|---|---|
   | verifier | `PASS` | `PASS` | `WF-026 FAIL /rules/allocations/0/versions/0: two allocations write target (Hoofdberger, bergingsaandeel)` |
   | ref | geen fixpunt | geen fixpunt | geen fixpunt |
   | clingo | `H1.bergingsaandeel = [100;500]` | `[500;100]` | `[100;1000]`, G1 leeg |
   | Drools exec | geen fixpunt | geen fixpunt | `EXCLUDE transpiler SKIP … (>1 verdeling schrijft hetzelfde doel-attribuut …)` |
   | DRL-tak | `verwacht 500, werkelijk 100` | `PLAYED 1` | FAIL |

   De principiële skip van de exec-transpiler keyt dus ook op het gedeclareerde type.

   Ook de allocatie-lift van WF-050 heeft de kloof. Proef: een groep op Hoofdberger en een
   allocatie op Berger. Verifier `PASS`; ref en exec geen fixpunt; clingo `[7;500]`; DRL
   `EXCLUDE` (geen defaults-lowering). De vlakke controle (groep op Berger) geeft
   `WF-050 FAIL /rules/prioritizedDefaults/0: an allocation also writes defaults target 'Berger.bergingsaandeel'`.

   Allocaties worden nooit in een stapel gevouwen, dus een ketenarm voor WF-026 en voor de
   allocatie-lift van WF-050 zou geen poort hebben: elk zulk paar schrijft de instanties van het
   lagere type.
2. **Gate 3, meldingsfout (scripts/, beschermd, buiten mijn scope).** Een negatief die de verifier
   passeert en een expect.txt draagt, krijgt ook de regel "rejected for WF-079, but not on the arm
   …". Die regel is onwaar, want het geval werd niet geweigerd (gemeten in de mutatierun).
   Voorstel: het arm-blok alleen draaien als het geval geweigerd is.
3. **Negatief-README.** De README noemt nieuwe getuigen alleen in de secties per migratiestap.
   `wf079-keten-kale-schrijvers` staat er daarom niet in. Rijtekst als de hoofdsessie haar wil:
   `| wf079-keten-kale-schrijvers | WF-079 | — | toegevoegd | two unconditional computing rules write target (Wrakrecht, uitkering) through its type chain … | een kale regel op Strandrecht en een op Wrakrecht, geen defaults-groep in de keten |`.
4. **Voorstellen beschrijven de stand vóór de arm.** Het gaat om `spec/proposals/SUBTYPING.md`
   §4.2 en de samenvatting daarboven ("Today the fences do not fire on a supertype/subtype pair at
   all"), en om `DIALECT-DEFAULTS-NIET-EN-NESTING.md` §9.4 (verifier PASS). Dat is geschiedenis en
   valt buiten de may-touch. Een eventuele annotatie beslist de hoofdsessie.
5. **Suites die de verifier aanroepen zijn hier niet gedraaid** (U9: alleen gates 1–3). Het gaat
   om de clingo-suite (gate 6), de F-logic-suite (gate 14) en de catala-suite (gate 9).
   - De clingo-suite heb ik statisch nagelopen: haar documenten met een supertype-rand
     (test_clingo.ml r. 3202, 6860, 8563, 9770, 10685) hebben geen rekenpaar op twee niveaus.
   - De drie F-logic-testprogramma's geven met de nieuwe verifier `PASS` (gemeten).
   - De catala-suite heeft geen supertype-rand.

   De uniediraai is de bevestiging.
6. **Spelling van de typenaam verschilt per arm.** De vlakke arm drukt de genormaliseerde naam af
   (`(jutter, uitkomst)`); de ketenarm drukt de gedeclareerde spelling af
   (`(Wrakrecht, uitkering)`, zoals U2). De vlakke zin blijft per mandaat ongewijzigd.
7. **Identieke dubbele regels.** Draagt een voorouder twee vormen op het doel en het subtype één,
   dan geeft de ketenarm twee identieke regels op dezelfde pointer (proef "vlak paar + keten").
   De vlakke arm doet hetzelfde bij drie schrijvers op één type. Ontdubbelen per
   (pointer, zin) kan, als de hoofdsessie dat wil.

## Lens en fixronde (2026-09-25, hoofdsessie)

**Lens A (spec/verifier-kwartet, export van de unie d43eb002; oordeel EERST FIXEN).** Voor deze kaart: B1 — de poorthelft van de ketenarm had geen getuige en de WF-079-indexrij noemde drie goldens die geen paar vormen (de mutant zonder poortvoorwaarde verandert 0 van 748 documenten); A1 — RS ch. 7.10 "a bare rule" is breder dan wat WF-079 weigert; A2 — "them" zonder antecedent in ch. 6.1; A3 — "or explicitly absent" past niet bij een ketenpaar; A4 — dubbele identieke meldingen op één pointer en verschillende typespelling tussen de vlakke arm en de ketenarm; A17.1 — de allocatieketen onder WF-026/WF-050 heeft dezelfde kloof (gemeten: verifier PASS, ref en exec geen fixpunt, clingo twee waarden); A17.7 — SUBTYPING §4.2 en DIALECT-DEFAULTS §9.4 beschrijven de stand vóór de arm. Bevestigd door de lens: de verhuizing van `target_key`/`chain_group_keys` verandert de vouw niet (42 identieke runs), de reproductie van §9.4, en dat precies de nieuwe getuige verschuift.

**Besluiten van de hoofdsessie.** B1: een golden in de poortvorm (kale regels op het hoogste en laagste type, groep op het middelste) plus de gecorrigeerde rijtekst; A3: de bijzin vervalt uit de ketenzin; A4: ontdubbelen per (pointer, zin), de typespelling blijft een open punt; A17.1: eigen kaart `verifier-allocatie-keten` (backlog/new); A17.7: één geschiedeniszin per plek. De gate-3-meldingsfout (open punt 2 van de bouwer) blijft een open punt.

**Fixronde (fixer A-exec).** A17.7: SUBTYPING §4.2 en §12 punt 4 en DIALECT-DEFAULTS-NIET-EN-NESTING §9.4 dragen elk één zin, gemarkeerd als geschiedenis, dat WF-079 sinds deze kaart het ketenpaar weigert (de probe van §9.4 in beide bronvolgordes nagemeten: WF-079).

**Fixronde (fixer A-spec, card/k6-fix-a-spec, gerebaset in de unie).** B1: de golden `corpus/golden/jutterswet-diepwrakbasis` (keten Diepwrak ⊑ Wrakrecht ⊑ Strandrecht; kale regels op het hoogste en het laagste type, een groep op het middelste; case1 S1 10.00 / W1 20.00 / DW1 30.00, case2 betwist S1 10.00 / W1 99.00 / DW1 99.00; ref ≡ clingo ≡ exec, DRL EXCLUDE op de defaults-zin) is de getuige van de poorthelft: de mutant zonder poortvoorwaarde weigert precies deze golden en geen van de 754 andere documenten; de WF-079-indexrij noemt hem en beschrijft de drie bestaande goldens naar wat zij dragen (hooguit één kale schrijver per keten). A1, A2 gefixt; A3: de ketenzin verloor "or explicitly absent" — de definitieve zin luidt `two unconditional computing rules write target (<type>, <attribuut>) through its type chain — one declared on <voorouder>, one on <type> — and no defaults group folds them into one stack, so both fire on a <type> instance; priority is explicit (a defaults group, whose subtype base replaces the inherited base, ch. 7.10), never implicit agenda order (ch. 7.7)` (de U2-tekst hierboven is daarmee achterhaald; `expect.txt` pint tot "instance" en bleef gelijk); de negatief-NOTE legt uit waarom `unordered` door een keten geen uitweg is (ref en clingo weigeren beide plaatsingen bij naam). A4: ontdubbeling per (pointer, zin); de typespelling (vlak genormaliseerd, keten gedeclareerd) blijft een open punt, net als de gate-3-melding "not on the arm" bij een niet-geweigerde getuige. Gate-12-pins op de unie: +1 exec (197), DRL_CHANNEL klasse (A) +1.
