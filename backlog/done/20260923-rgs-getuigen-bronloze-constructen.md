# rgs-getuigen-bronloze-constructen: objectcreatie, macht, afronding, op volgorde van en beslistabel krijgen een RegelSpraak-bron

Vijf constructen die de frontend gewoon parseert (`objectCreatie`,
`TOT_DE_MACHT`, de afrondingsrichtingen naast NAAR_BENEDEN, verdeling
`OP_VOLGORDE_VAN`, `beslistabel`) hebben in het corpus geen enkele golden
met `bron.rgs`: van de 70 rgs-goldens bevat er geen een, en de elf goldens
met `objectCreaties` (vier Catala-goldens, `jutterswet-wrakboeking`,
`synth2-budgetgrens-onder`, `synth3-kamerheffing` en de vier anchor-goldens
`jutterswet-penninguitgifte`, `jutterswet-rekenkamer`,
`jutterswet-rekenkamer-overzee`, `jutterswet-wachtersketen`) zijn
handgeschreven IR of Catala-emissie. Gate 10 (byte-identieke her-emissie
door de frontend) bewijst voor deze vijf vormen dus niets; gemeten
2026-09-23 bij het schrijven van de website, die dit gat als
"zonder RegelSpraak-bron in het corpus" moest melden. Vermoedelijke
oorzaak: de goldens dateren van vóór de komst van de frontend in de
monorepo (2026-08-27) en zijn nooit van een bron voorzien.

## Acceptatie (bij oppakken aanscherpen)

- (a) Per construct ten minste één golden met `bron.rgs` die gate 10
  byte-identiek her-emitteert. Voorkeur: een bestaande handgeschreven
  golden een bron geven waar de frontend precies de ingecheckte `ir.json`
  emitteert; waar de emissie afwijkt van de handgeschreven IR, de
  afwijking benoemen in de `Result:`-noot en NIET de handgeschreven IR
  stil herschrijven — een nieuwe Jutterswet-golden (handgerekend, via
  `scripts/admit.sh`) is dan de route. Anchor-goldens zijn beschermd pad:
  een bron erbij is een voorstel.
- (b) Het vierkant blijft dicht: ref ≡ clingo over elke nieuwe of
  gewijzigde golden; wat Drools van de nieuwe vormen niet speelt, komt bij
  naam met reden in de gate-12-lijsten (de objectcreatie-marker op een
  onoplosbare terugverwijzing is bekend).
- (c) Alle veertien gates groen. `EXPECTED_MIN_GOLDENS` in
  `scripts/check-regelspraak.sh` (38, gemeten 2026-09-10) is een
  ondergrens en hoeft niet omhoog; een bump is een voorstel op een
  gelegenheid, conform het telvloerregime.
- (d) `Result:` noemt per construct de golden die nu als rgs-getuige
  dient, zodat de website-passage "zonder RegelSpraak-bron in het corpus"
  kan vervallen.

May touch: `corpus/golden/**` (nieuwe goldens; `anchor/` als voorstel),
`scripts/check-regelspraak.sh` (**voorstel**, alleen als een vloer bewust
omhoog gaat), kaartverplaatsing.

Out of scope: frontend- of spec-wijzigingen. Blijkt een handgeschreven IR
niet door de frontend te emitteren, dan is dat een eigen kaart (frontend
of registerrij), geen reden om de IR hier aan te passen.

## Correcties bij de claim (2026-09-25, hoofdsessie)

De kaart is op 2026-09-23 op de L1-wire geschreven; de repo staat op de
L5-wire (`uchar-ir-v1` compleet, main 2f47d80e).

- Tellingen vandaag: 73 goldens met `bron.rgs` (was 70); twaalf goldens met
  een niet-lege `rules.objectCreations` (de elf van de kaart plus
  `jutterswet-opvolgerquorum`); geen van de twaalf draagt `bron.rgs`; de
  ankermap draagt geen enkele `bron.rgs`.
- Wire-namen: `objectCreaties` → `rules.objectCreations[]` (`ObjectCreation
  {name, versions[{createdType, subject, …}]}`); de macht is een `Binary`
  met de L5-operatorspelling (lees `spec/verifier/lib/spelling.ml`, familie
  `binary_op`); de afronding is `Rounding {mode, digits}` met de L5-modi
  (familie `rounding_mode`); de verdeling op volgorde is een `Allocation`
  (IR-SPEC ch. 3.2, lees de veldnamen daar); een `Beslistabel` heeft geen
  eigen knoop: de frontend desugart elke rij tot een gewone `Rule`
  (`BeslistabelDesugar`), dus de getuige is een golden waarvan `bron.rgs` een
  beslistabel schrijft en `ir.json` de gedesugarde regels draagt.
- Elke door de frontend geëmitteerde knoop draagt `source {snippet, line?}`;
  een handgeschreven golden draagt dat niet of anders — daarom zal de
  emissie zelden byte-gelijk zijn aan een handgeschreven `ir.json`. De route
  is de tweeling (U1), niet het herschrijven van de handgeschreven IR.
- Gate 10: `make ir FILE=…` in `frontends/regelspraak` emitteert; de vloer
  `EXPECTED_MIN_GOLDENS` in `scripts/check-regelspraak.sh` lees je in het
  script (de kaart noemt 38).

## U-besluiten (hoofdsessie, 2026-09-25)

- U1 **Route: tweelingen.** Per construct een golden
  `corpus/golden/<drager>-rgs/` met `bron.rgs`, de frontend-emissie als
  `ir.json` (byte-identiek onder gate 10, round-trip onder gate 2), de cases
  van de handgeschreven drager byte-gelijk gekopieerd (zelfde absolute
  verwachtingen) en een `NOTE.md` met de structurele diff tegen de
  handgeschreven `ir.json` (elke `source` gestript, sleutels gesorteerd) en
  per verschil de triage: (i) dialect- of emissieverschil, (ii)
  IR-ontwerpfout, (iii) frontendfout — een (ii) of (iii) stopt dat construct
  en gaat als genummerd open punt naar de hoofdsessie (geen frontend- of
  spec-wijziging in deze kaart). Handgeschreven goldens en ankers blijven
  byte-ongewijzigd; geen `bron.rgs` in `anchor/`.
- U2 **Dragers kiezen door te meten:** een sweep over `corpus/golden/**/ir.json`
  op de macht-operator, de afrondingsmodi anders dan naar beneden, een
  `Allocation` met volgorde-criterium en de objectcreaties; per construct de
  kleinste handgeschreven niet-anker-drager (objectcreatie bijv.
  `jutterswet-wrakboeking` of `synth3-kamerheffing`). Bestaat er voor een vorm
  geen handgeschreven drager, dan een nieuwe Jutterswet-golden (handgerekend,
  via `scripts/admit.sh`: ref ≡ clingo, no-op weerlegd).
- U3 **Drools.** Een tweeling met objectcreatie wordt door de exec-tak
  geweigerd (de bekende marker): een `EXPECTED_CHANNEL`-regel met reden en
  een `DRL_CHANNEL`-regel (**voorstel**, `scripts/check-drools.sh`); de
  spelende tweelingen verhogen `EXPECTED_PLAYED` en `EXPECTED_DRL_PLAYED`
  (**voorstel**, met de gemeten getallen in het Result). Gate 7: staat de
  drager op de overleverslijst, dan de tweeling met dezelfde reden
  (**voorstel**, `scripts/check-noop.sh`).
- U4 Het Result noemt per construct de rgs-getuige (acceptatie (d)) en per
  tweeling de diff-triage.
- U5 **Gates** voor deze bouwer: 1, 2, 3, 4, 5, 6, 7, 10, 12 (13 als een
  tweeling een type-annotatie op een afgeleide positie draagt); de unie
  draait alle veertien.

## Result (2026-09-25, bouwer)

## Result — rgs-getuigen-bronloze-constructen

*Gecorrigeerd door de hoofdsessie na de rebase op de unie en de lens (B1): hashes, tellingen en pins zijn die van de unie; de tweeling van rangaflopend is teruggevouwen in de drager.*

### Stand

- Branch `card/rgs-getuigen-bronloze-constructen`, worktree `/Users/yeb/Developer/uchar-dev/wt/rgs-getuigen`,
  basis main 2f47d80e, claim acb32e85 (na de rebase op de unie 5d0cfb44), kop op de unie **f3c20d24** (de hashes hieronder zijn die van ná de rebase door de hoofdsessie). Werkboom schoon.
- Commits (oud → nieuw):
  - 31d20071 jutterswet-wrakboeking-rgs: de objectcreatie krijgt een RegelSpraak-bron
  - 7201e3e8 synth-afrondingswaaier-rgs: de vier afrondingsrichtingen uit een bron
  - e8bca444 jutterswet-rangaflopend-rgs: de verdeling op volgorde uit een bron (door de hoofdsessie teruggevouwen in de drager, zie f3c20d24)
  - f3fb9269 jutterswet-rubriektabel-rgs: een beslistabel met rijen uit een bron
  - dd17cda2 jutterswet-kistinhoud: de macht krijgt een RegelSpraak-bron
  - 33265845 gate 12: vijf nieuwe spelers, voorstel 188 -> 193 en 167 -> 172 (**voorstel**, beschermd pad; op de unie 192/171 na f3c20d24, 194/171 met de twee dialecttweelingen)
  - 263ad833 synth-afrondingswaaier-rgs: de NOTE zegt precies waarom de whole-vlag niets verandert
  - f3c20d24 (hoofdsessie) de bron van de verdeling op volgorde in de drager `jutterswet-rangaflopend` zelf; de tweeling `-rgs` vervalt; pins 192/171; `admit.sh` start elke gate onder zijn eigen interpreter
- Geraakt: alleen vijf nieuwe mappen onder `corpus/golden/` en `scripts/check-drools.sh` (voorstel).
  Handgeschreven dragers en ankers byte-ongewijzigd; geen bron.rgs in `anchor/`; geen frontend- of specwijziging.

### Tellingen

- Goldens 242 → 246 (vier nieuwe; de vijfde bron zit in de drager zelf); goldens met `bron.rgs` 73 → 78; golden-cases 619 → 630 (+1, +5, +3, +2).
- Gate 12: EXPECTED_PLAYED 188 → 192, EXPECTED_DRL_PLAYED 167 → 171 (de drager rangaflopend speelde al); geen EXPECTED_CHANNEL- of DRL_CHANNEL-regel.
- Gate 7: geen overleverwijziging (geen drager staat op de lijst; elke nieuwe case weerlegt de no-op).

### Stap 1 — sweep (U2), gemeten 2026-09-25 over 242 goldens

| construct | niet-anker-dragers zonder frontendbron | gekozen, reden |
|---|---|---|
| objectcreatie | jutterswet-wrakboeking (3739 B), synth2-budgetgrens-onder, synth3-kamerheffing, jutterswet-opvolgerquorum (+ 4 ankers, 4 Catala) | wrakboeking: kleinste. kamerheffing valt ook af: zijn kenmerkinitialisatie leest een boolean attribuut, de frontend eist `waar`/`onwaar` |
| macht | alleen jutterswet-kindermacht: macht in een Lambda over een lijstparameter, geen RegelSpraak-vorm (anker jutterswet-taxatie uitgesloten) | nieuwe golden jutterswet-kistinhoud |
| afronding ≠ naar beneden | synth-afrondingswaaier (alle vier modi), jutterswet-kinderweging (6938 B, alleen halfAwayFromZero, onder een Let), synth2-afrondingscombinaties, synth3-drempelheffing/-letbeslag/-standtarief (Let/SumType), jutterswet-kinderfoldheffing (lijst) | afrondingswaaier: de enige drager van alle vier richtingen |
| op volgorde van | jutterswet-rangaflopend (3140 B), jutterswet-rangdeling (3213 B), jutterswet-rangmaximering (3810 B) | rangaflopend: kleinste |
| beslistabel | geen wirevorm; tweede sweep (≥ 2 voorwaardelijke regels op één doel, literal-waarden): jutterswet-vrijgavetoets (6192 B), jutterswet-rubriektabel (10390 B) | rubriektabel: vrijgavetoets heeft `Compound none`/`atLeast` over verschillende predicaten, dat heeft geen rijvorm; rubriektabel heeft precies de §12-celvormen (gelijk-reeks met "of", ongelijk-reeks met "en") |

Gecontroleerd: geen van de 73 rgs-goldens op de basis droeg een macht, een afronding ≠ floor, een geordende Allocation of een objectcreatie.

### Per construct de rgs-getuige (acceptatie (d)) en de diff-triage (U4)

| construct | rgs-getuige | drager | structurele diff (`source` gestript, sleutels gesorteerd) | triage |
|---|---|---|---|---|
| objectcreatie | `corpus/golden/jutterswet-wrakboeking-rgs` | jutterswet-wrakboeking | 1 pad: `/rules/objectCreations/0/versions/0/connectingRole` (hand aanwezig, emissie afwezig) | (i): de frontend schrijft geen binding (IrEmitter.objectCreation); IR-SPEC ch. 3.3 laat haar weg, RS ch. 5.5 leidt dan de unieke relatie af, hier eenduidig (één feittype; de gemunte Strandrecht kan alleen `geboekt strandrecht` vullen) |
| macht | `corpus/golden/jutterswet-kistinhoud` (nieuw, handgerekend) | — | — | — |
| afronding ≠ naar beneden | `corpus/golden/synth-afrondingswaaier-rgs` | synth-afrondingswaaier | 6 paden `/rules/rules/{0..5}/versions/0/value/base/type/whole`: hand `true`, emissie `false` (annotatie van de deling) | (i): RS ch. 2.10 cl. 2 neemt de precisie uit ch. 2.3; WF-091 kruist de deling niet; geen uitkomst verandert (gemeten) |
| op volgorde van | `corpus/golden/jutterswet-rangaflopend` (de bron in de drager zelf) | jutterswet-rangaflopend | geen; de emissie is byte-gelijk, dus geen tweeling | — |
| beslistabel | `corpus/golden/jutterswet-rubriektabel-rgs` | jutterswet-rubriektabel | na paren op doel+waarde: 2 rijnamen (`rubriek van een aanspoelsel 1/2` tegen `rubriek bij organische/overige soort`) en de volgorde van `rules.rules` (gewone regels vóór de tabelrijen, gemeten met een proefbron) | (i) naamgeving, (i) serialisatievolgorde |

Geen (ii) of (iii): elk construct is doorgelopen.

Metingen per getuige (ref en clingo exact; no-op weerlegd op elke case; Drools):

| golden | cases | ref = clingo | no-op | Drools exec | Drools DRL |
|---|---|---|---|---|---|
| jutterswet-wrakboeking-rgs | 1 | ja | weerlegd | PLAYED 1 | PLAYED 1 |
| synth-afrondingswaaier-rgs | 5 | ja | weerlegd | PLAYED 5 | PLAYED 5 |
| jutterswet-rangaflopend (bron in de drager) | 1 | ja | weerlegd | PLAYED 1 | PLAYED 1 |
| jutterswet-rubriektabel-rgs | 3 | ja | weerlegd | PLAYED 3 | PLAYED 3 |
| jutterswet-kistinhoud | 2 | ja | weerlegd | PLAYED 2 | PLAYED 2 |

Aanvullende metingen (proefkopieën buiten het corpus):
- wrakboeking: met een extra regel die de gemunte instantie leest (`de grondslag van zijn geboekt strandrecht`) en de creatiewaarde `plus 1` geven ref, clingo en beide Drools-takken 11.00 en 3.50; een verwachting 12.00 faalt in alle vier.
- kistinhoud: een verwachting 15,62 → 15,63 faalt in alle vier.

### Gate-uitvoerregels (U5), elk los na `eval $(opam env)`

*(Gemeten op de bouwertak vóór de terugvouw van de rangaflopend-tweeling; op de unie 1aa057e8: 630 cases in gates 5 en 6, gate 12 played 194 (192 van deze kaart plus 2 dialecttweelingen), DRL 171.)*

- 1: `check-self-contained: PASS` (ook opnieuw na de laatste commit)
- 2: `check-ir-schema: PASS (schema valid; 79 pattern self-test row(s); 504 shape self-test row(s); 294 file(s) validate and round-trip, floor 192)`
- 3: `check-verifier: PASS (294 golden clean; 418 negatief rejected for their declared rule, floor 350; 260 on their declared arm (expect.txt), floor 260)`
- 4: `check-case-schema: PASS (schema valid; 92 pattern self-test row(s); 679 case file(s) validate)`
- 5: `check-oracle: PASS (ref suite 977 worked examples, floor 489; 631 cases replayed exactly, floor 427; NOT replayed, no case file yet: corpus/golden/jutterswet-seizoenvondst corpus/golden/jutterswet-vormenkabinet)`
- 6: `check-clingo: PASS (backend suite 1726 checks, floor 83; 631 cases replay exactly through clingo — ref = clingo over the corpus)`
- 7: `check-noop: PASS (594 cases refute a do-nothing executor; 37 pinned survivors pin defeasible standing)`
- 10: `check-regelspraak: PASS — JUnit suite green (577 tests, floor 173); 78 rgs golden(s) re-emit byte-identically (floor 38)`
- 12: `check-drools: PASS` — `played 193`, `auto-excluded 51`, `value-divergence 1`, `no-case 2`, `channel floor 54 non-PLAYED goldens pinned by name; played matches the pinned 193`, `DRL-text branch played 172 (... pin 172)`, `DRL-text floor 75 non-PLAYED goldens pinned by name; sweep cost 132s wall clock`, `JUnit 773 tests, floor 239`
- 13 (de nieuwe goldens dragen annotaties op afgeleide posities, eenheidloos): `check-reftype-derivation: PASS (40 unit annotations twisted over 22 goldens, floor 38; 100 executor replays reproduce the untouched outcome, 21 of them PLAYED by the DRL-text branch, floor 20; 0 pinned survivors)`
- Niet gedraaid (buiten U5): 8, 9, 11, 14. Geen daarvan leest de nieuwe goldens (8: corpus/rood; 9 en 14: alleen goldens met bron.catala_en/bron.flogic; 11: de toka/-zone).

### Gewijzigde scriptregels (beschermd pad = voorstel)

`scripts/check-drools.sh`, twee plaatsen:
- `EXPECTED_PLAYED=188` → `EXPECTED_PLAYED=192` (op de unie; de bouwer schreef 193 met de tweeling), met een redenblok "188 -> 192 (2026-09-25, card rgs-getuigen-bronloze-constructen)" dat de vijf goldens noemt, PLAYED 1, 5, 1, 3 en 2, en vastlegt dat jutterswet-wrakboeking-rgs zonder connectingRole speelt (het 134 -> 135-blok schrijft het spelen van de drager aan zijn expliciete binding toe; die is dus niet de enige route).
- `EXPECTED_DRL_PLAYED=167` → `EXPECTED_DRL_PLAYED=171` (op de unie), met een redenblok "167 -> 171 (…)" dat naar het blok bij EXPECTED_PLAYED verwijst.
- Geen EXPECTED_CHANNEL- of DRL_CHANNEL-regel; `scripts/check-noop.sh` en `scripts/check-regelspraak.sh` ongewijzigd.

### Wat aan de kaarttekst niet klopte (gemeten)

1. U3 en acceptatie (b): de objectcreatie-tweeling wordt níet geweigerd. jutterswet-wrakboeking-rgs speelt in beide Drools-takken (PLAYED 1); de terugverwijzing is afleidbaar (één feittype, één richting), dus de marker voor een onoplosbare terugverwijzing vuurt niet. Geen kanaalregel nodig.
2. U2 "via scripts/admit.sh": admit.sh kan hier niets toelaten. Stap 6 roept elke gate aan met `sh`, en `/bin/sh` is hier bash 3.2 in POSIX-modus zonder procesvervanging. `sh -n` faalt op vijf gatescripts: check-catala.sh r. 92, check-drools.sh r. 1192, check-flogic.sh r. 96, check-regelspraak.sh r. 82, check-toka-parity.sh r. 147. Echte run voor jutterswet-kistinhoud: stappen 1–5 PASS (verifier PASS; oracle en clingo elk tweemaal `PASS (5 expectations)`; no-op weerlegd), daarna `admit: REFUSED — gate scripts/check-catala.sh turned red after the copy — admission rolled back` (5,3 s). De golden is met de hand opgenomen; ik heb het defect niet omzeild met een sh-shim.
3. "van de 70 rgs-goldens bevat er geen een" (beslistabel): jutterswet-kaletabel draagt een Beslistabel-kop zonder rijen. Die getuigt van de WARNING, niet van de desugaring; voor een tabel met rijen klopt de premisse.
4. U1-reden "de emissie zal zelden byte-gelijk zijn": voor jutterswet-rangaflopend is ze byte-gelijk (geen van beide kanten draagt source-records op een verdeling). De tweeling is daardoor een byteduplicaat van de drager.
5. "Bouw de frontend (`make` in frontends/regelspraak)": `make` zonder doel toont alleen de hulp (default goal `help`); bouwen is `make compile`, of `make ir`, dat eerst compileert.
6. Klopte wel: 12 goldens met een niet-lege `rules.objectCreations`, 73 goldens met bron.rgs op de basis, EXPECTED_MIN_GOLDENS 38, de grammaticaregels die de opdracht noemt.

### Definitieve weigerzinnen

Geen nieuwe of veranderde weigerzin.

### Registerzinnen voor CONSTRUCTS

De kaart vraagt er geen.

### Genummerde open punten

1. (Afgehandeld door de hoofdsessie in f3c20d24: elke gate onder zijn eigen interpreter.) **admit.sh stap 6** (beschermd pad `scripts/`, buiten mijn may-touch): `sh "$gate"` kan de vijf bash-gates niet parsen, dus elke toelating wordt teruggerold op een machine waar `sh` geen procesvervanging kent. Hier gemeten op macOS (bash 3.2 als sh). Voor Linux met dash als sh niet gemeten, maar dash kent geen procesvervanging. Voorstel: elke gate starten met zijn eigen interpreter (`bash` voor de vijf, of via de shebang).
2. (Afgehandeld door de hoofdsessie in f3c20d24: de bron zit in de drager, de tweeling vervalt.) **jutterswet-rangaflopend-rgs is een byteduplicaat** van zijn drager (ir.json en case.json gelijk). Keuze voor de hoofdsessie: laten staan, of bron.rgs in jutterswet-rangaflopend zelf zetten (de voorkeur van acceptatie (a)) en de tweeling schrappen. Dat laatste betekent gate 12 −1/−1 en een map erbij in een handgeschreven golden, wat U1 uitsloot.
3. De case van de **objectcreatie-tweeling neemt de gemunte instantie niet waar**; dat is de grens van de drager. De proef met een lezende regel (zie boven) gaf in alle vier uitvoerders hetzelfde. Een golden die die lezing pint, zou een sterkere getuige zijn; niet gebouwd, want de kaart kiest de kleinste drager.
4. De frontend schrijft **geen connectingRole** op een objectcreatie, ook niet als de zin de rol noemt. Voor een zelfgetypeerde creatie (anchor/jutterswet-wachtersketen, synth2-budgetgrens-onder, jutterswet-opvolgerquorum) is die binding volgens RS ch. 5.5 de enige gekoppelde route. Die drie hebben dus geen RegelSpraak-tweelingroute. Kandidaat voor een frontendkaart.
5. De frontend accepteert als kenmerkwaardetoekenning alleen `waar`/`onwaar` (§13.4.6.4); de boolean-expressievorm (IR-SPEC ch. 3.3, CONSTRUCTS-rij 21) heeft geen RegelSpraak-spelling. Daarom valt synth3-kamerheffing af, de enige handgeschreven creatie waarvan de case de munt waarneemt.
6. RS ch. 2.3 geeft voor de deling alleen maxDec, niets over heelheid. De frontend annoteert `whole: false`, synth-afrondingswaaier `true`. Geen uitkomst hangt ervan af (gemeten) en geen verifierregel kruist het; een specregel zou het kunnen noemen. Advies.
7. De lexer kent alleen `decimalen`. Een afronding op één decimaal moet dus `afgerond op 1 decimalen` heten; het handgeschreven snippet van synth-afrondingswaaier schrijft `1 decimaal`. Grammaticawaarneming.
8. De machtsknoop en zijn afronding dragen hetzelfde `source`-snippet, omdat de machtsproductie de verplichte afronding omvat. Waarneming, zonder gevolg.
9. (Opgehoogd in de fixronde, lensvondst A4.) EXPECTED_MIN_GOLDENS in check-regelspraak.sh blijft 38 bij een gemeten 78. Ophogen is een voorstel voor een gelegenheid (acceptatie (c)); niet gedaan.
10. Gates 8, 9, 11 en 14 heb ik niet gedraaid (buiten U5); op de unie 1aa057e8 waren alle veertien groen.

## Lens en fixronde (2026-09-25, hoofdsessie)

**Lens B (export van de unie d43eb002; oordeel EERST FIXEN).** B1 — het Result beschreef de bouwertak en niet de unie (de tweeling van rangaflopend, de pins 193/172, de hashes van vóór de rebase): door de hoofdsessie gecorrigeerd in het Result hierboven; B2 — de NOTE van `jutterswet-kistinhoud` noemde alleen de operandgrens 12,90 van de machtsknoop, terwijl de afrondingsguard 2^29 op de geschaalde derdemacht eerst geldt (gemeten: 8,12 PASS, 8,13 UNSAT); A1 — de NOTE van wrakboeking-rgs verwarde `boeker` en `geboekt strandrecht`; A2 — de volgordeclaim van de beslistabel zonder bron (`RuleChecker.check`, RS ch. 5.2); A3 — de NOTE van de drager wrakboeking noemde de expliciete binding nog als voorwaarde om in Drools te spelen; A4 — `EXPECTED_MIN_GOLDENS` 38 bij gemeten 80; A5 — het commentaar in admit.sh te smal (ook dash en arrays); A6 — de objectcreatie-getuige bewijst alleen de emissie, niet de uitvoering (een lezende variant is in alle vier uitvoerders gemeten); O1 — de frontend schrijft nooit een `connectingRole` op een objectcreatie; O2 — RS ch. 2.3 zegt niets over de heelheid van een deling; O3 — admit.sh kopieert geen NOTE.md en geen `bron.catala_en`/`bron.flogic`. Bevestigd: alle vijf bronnen emitteren byte-gelijk, de cases byte-gelijk aan de dragers, de triage (i) houdt op elk van de drie verschillen stand, de pins kloppen, de nieuwe admit.sh laat toe.

**Besluiten van de hoofdsessie.** Alles gefixt, inclusief de vloer van gate 10 naar 80, de extra golden die de gemunte instantie leest (pins +1/+1) en de RS-zin voor de deling (voorstel); O1 blijft een open punt voor een frontendkaart.

**Fixronde (fixer B, branch card/k6-fix-b, kop d2b4af57, gerebaset in de unie).** B2: de NOTE van kistinhoud noemt beide clingo-grenzen met de gemeten 8,12/8,13; A1, A2, A3: de drie NOTE's gecorrigeerd (de drager wrakboeking: de tweeling zonder binding speelt in beide takken; zonder `source`-records verschilt hij van de drager alleen in de binding); A4: `EXPECTED_MIN_GOLDENS` 38 → 80 (2026-09-25; gemeten daarna 81, de vloer is een ondergrens); A5 en O3: admit.sh noemt dash en arrays en kopieert ook `NOTE.md`, `bron.catala_en` en `bron.flogic`; O2: RS ch. 2.3 rij van de deling "maxDec, not whole, as for tot de macht"; A6: de nieuwe golden `corpus/golden/jutterswet-wrakboekwaarde` (via `scripts/admit.sh`, 521 s, alle gates groen): een regel leest de gemunte instantie via de terugkoppeling (W1 bergingsvergoeding 10.00 / boekwaarde 11.00, W2 2.50 / 3.50, consistent), ref ≡ clingo ≡ beide Drools-takken, de no-op weerlegd, en zonder de objectcreatie faalt de case in ref en clingo waar die van de tweeling groen blijft — de objectcreatie is nu ook in uitvoering gepind. De rgs-getuige voor objectcreatie is daarmee het paar `jutterswet-wrakboeking-rgs` (emissie) en `jutterswet-wrakboekwaarde` (uitvoering). Open gebleven: O1 (de frontend schrijft nooit `connectingRole`; zodra hij dat doet, emitteren beide goldens anders); de rij `maal` in RS ch. 2.3 zegt niet "not whole" terwijl WF-091 haar zo citeert; 23 handgeschreven delingsannotaties met `whole: true` (15 in 7 goldens, 8 in 5 rood-documenten) spreken de nieuwe rij tegen — de verifier kruist die positie niet, geen gate verandert; admit.sh laat bij een falende `cp` een halve map achter.
