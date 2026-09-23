# anker-ww-herleving-dragervoorwaarde: de geneste rechtvaardigingen dragen de voorwaarde van hun drager, en de twee hoeken worden cases

Gemeten in het onderzoek bij de kaart dialect-defaults-niet-en-nesting
(rapport buiten de repo:
`../research/dialect-defaults-niet-en-nesting/onderzoek-niet-in-uitzonderingen.md`
§4.4; het voorstel-concept `spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md`
§2.3 op de kaartbranch): het handanker `anchor/ww-herleving-inkomen` geeft
elke geneste default alleen haar eigen voorwaarde (herleving: `omstandigheid
vervallen`; blokkade: `nieuw recht ontstaan EN nieuw maandloon >
inkomensgrens`). RUNTIME-SEMANTICS ch. 7.9 evalueert de uitzonderingsgroepen
van een default eerst, ongeacht haar eigen rechtvaardiging, en ch. 7.1
(r. 4631–4637) schrijft voor dat een geneste laag een zelfstandige
rechtvaardiging krijgt door conjunctie met die van de omhullende laag. Twee
invoeren die de vier cases niet raken geven daardoor in ref, clingo én de
Drools-exec-tak een antwoord dat tegen de wettekst ingaat:

- hoek A: niet werkloos, maandinkomen 1800 (onder de grens 2100),
  omstandigheid vervallen → `recht op uitkering` **aanwezig** (de herleving
  beslist buiten de eindiging om); de wet: geen recht, er is niets geëindigd;
- hoek B: werkloos, maandinkomen 1800, nieuw recht met nieuw maandloon 2300 →
  **afwezig** (de blokkade van art. 21 lid 2 verdringt art. 15); de wet:
  het recht staat, want het is nooit geëindigd.

Met de dragervoorwaarden in de geneste rechtvaardigingen (herleving:
`maandinkomen > inkomensgrens EN omstandigheid vervallen`; blokkade: die twee
plus haar eigen twee) geven alle drie de uitvoerders het wetsantwoord en
houden de vier bestaande cases hun waarden (gemeten op een kladkopie, niet
gecommit). Daarnaast is gemeten dat een `niet`-besluit een in de invoer
gezet doelkenmerk in geen uitvoerder wegneemt (case4-einde met
`recht op uitkering` in de invoer → aanwezig); dat gedrag pint nog geen
case.

Dit anker is de referentie voor de RegelSpraak-tweeling (kaart
dialect-uitzondering-op-uitzondering); het herstel gaat daaraan vooraf.

## Ontwerp (maintainersbesluit 2026-09-23, "ik volg je aanbevelingen")

- De rose tree blijft (`Default.excepties`, vier niveaus); alleen de
  rechtvaardigingen op niveau 2 tot en met 4 krijgen de conjunctie met de
  drager. De wire draagt daarmee zelfstandige rechtvaardigingen, zoals
  ch. 7.1 wil; de boomstructuur blijft de bron van de wetsstructuur.
- Drie nieuwe cases: `case5-geen-eindiging-wel-nieuw-recht` (hoek B →
  aanwezig, beslist op art. 15), `case6-niet-werkloos-wel-vervallen` (hoek A
  → afwezig, niets beslist), `case7-invoer-staat-tegen-blokkade` (case3-feiten
  plus het doelkenmerk in de invoer → aanwezig; pint besluit 7-i van het
  rapport zolang de kaart kenmerkinvoer-en-invoer-boven-berekening niet
  anders beslist). Case 7 overleeft een no-op-uitvoerder en hoort dus met
  reden in de overleverslijst van gate 7.

## Acceptatie (bij oppakken aanscherpen)

- (a) `ir.json` met de geconjungeerde rechtvaardigingen; `NOTE.md` toont de
  boom met de volledige voorwaarden en de handberekening van de drie nieuwe
  cases; de vier bestaande cases ongewijzigd in verwachting.
- (b) Alle zeven cases exact in ref (gate 5), clingo (gate 6) en de
  Drools-exec-tak (gate 12); de DRL-teksttak blijft `EXCLUDE` voor dit
  golden (geen defaults-lowering), de played-pin van gate 12 verandert niet
  (zelfde golden).
- (c) `scripts/check-noop.sh`: case 7 met reden in de overleverslijst
  (**voorstel**, beschermd pad); gate 7 groen, en rood als de reden
  ontbreekt.
- (d) Alle veertien gates groen.

May touch: `corpus/golden/anchor/ww-herleving-inkomen/**` (**voorstel**,
handanker), `scripts/check-noop.sh` (**voorstel**), kaartverplaatsing.

Niet in deze kaart: het dialect, de spec, de uitvoerders; de betekenis van
`niet` tegenover invoer (eigen kaart).

## Result

Geland op de kaartbranch als **voorstel** (beschermde paden, de maintainer
merget): `corpus/golden/anchor/ww-herleving-inkomen/` (`ir.json`, `NOTE.md`,
vijf nieuwe cases) en `scripts/check-noop.sh` (één overleversregel). Commits:
f93234e4 (claim), 40849db2 (het werk), en deze. De vier bestaande case-files
zijn byte-ongewijzigd; buiten de drie geneste voorwaarden is `ir.json`
structureel identiek aan 3a140fe4; de wetteksten en de rekenregels in de
NOTE zijn byte-identiek.

**De boom vóór (3a140fe4) en na.** Vóór droeg elke geneste default alleen
haar eigen voorwaarde:

```
grant      waar   als werkloos                                                art. 15
 einde     onwaar als maandinkomen > inkomensgrens                            art. 20-1-c
  herleving waar  als omstandigheid vervallen                                 art. 21-1
   blokkade onwaar als ALLE[nieuw recht ontstaan, nieuw maandloon > grens]    art. 21-2-a
```

Na draagt elke geneste default (niveau 2 t/m 4) de volledige voorwaarde van
haar drager, plat als één `ALLE`, dragerconjuncten voorop:

```
grant      waar   als werkloos
 einde     onwaar als ALLE[werkloos, maandinkomen > inkomensgrens]
  herleving waar  als ALLE[werkloos, maandinkomen > inkomensgrens, omstandigheid vervallen]
   blokkade onwaar als ALLE[werkloos, maandinkomen > inkomensgrens, omstandigheid vervallen,
                           nieuw recht ontstaan, nieuw maandloon > inkomensgrens]
```

Uitkomst zonder het doelkenmerk in de invoer: aanwezig ⇔ W ∧ (¬T ∨ (V ∧ ¬N))
(W werkloos, T maandinkomen > grens, V vervallen, N nieuw recht én nieuw
maandloon > grens).

**Aanscherping 1 — `werkloos` staat ook in de geneste voorwaarden.** De
inleiding van deze kaart noemt de gemeten voorwaarden zonder `werkloos`
(herleving T ∧ V, blokkade T ∧ V ∧ N, einde ongewijzigd); het ontwerpbesluit
zegt "niveau 2 tot en met 4 krijgen de conjunctie met de drager" en
"zelfstandige rechtvaardigingen, zoals ch. 7.1 wil", en besluit 4d van de
tweelingkaart conjungeert "ook aan de wortel". Einde (niveau 2) krijgt dus
`werkloos` van de grant, en herleving en blokkade erven dat. Gemeten verschil
op een kladkopie met de voorwaarden uit de inleiding (niet gecommit): daar
faalt alleen case8, in alle drie de uitvoerders (`MISMATCH: W8 carries
characteristic "recht op uitkering", expected absent` in ref en clingo;
`FAIL case8-niet-werkloos-boven-grens-vervallen.json: kenmerk recht op
uitkering: verwacht afwezig, werkelijk aanwezig` in de exec-tak): de
herleving beslist `waar` voor een werknemer bij wie nooit een recht ontstond.
Op de zestien combinaties van W, T, V, N is case8 de enige plek waar de twee
lezingen verschillen.

**Aanscherping 2 — negen cases in plaats van zeven.** Over de zestien
combinaties beslist de oude boom op precies vier anders dan de herstelde:
hoek B (case5), hoek A (case6), ¬W∧T∧V∧¬N (case8) en W∧¬T∧V∧N (case9: werkloos,
onder de grens, vervallen, nieuw recht boven de grens → oud afwezig, wet
aanwezig). Elk is nu een case, zodat elke invoer waarop de reparatie de
uitkomst verandert gepind staat. Wie de extra cases niet wil: case8 en case9
zijn losse bestanden, maar zonder case8 is het `werkloos`-conjunct door geen
gate zichtbaar, en zonder case9 het T-conjunct in de blokkade niet.

**De negen cases: handwaarde en meting** (`maandloon` 2400,00, dus
`inkomensgrens` 2100,00 overal; elke case pint ook `consistent`; volledige
handberekening in de NOTE):

| case | W T V N | hand: recht; verstreken periode; nieuwe maxdatum | beslist | ref | clingo | Drools-exec |
|---|---|---|---|---|---|---|
| case1-herleving | 1 1 1 0 | aanwezig; 123; 2027-04-02 | herleving | PASS (5) | PASS (5) | PLAYED |
| case2-geen-eindiging | 1 0 0 0 | aanwezig; leeg; 2026-11-30 | grant | PASS (5) | PASS (5) | PLAYED |
| case3-blokkade | 1 1 1 1 | afwezig; 92; 2026-01-15 | blokkade | PASS (5) | PASS (5) | PLAYED |
| case4-einde | 1 1 0 0 | afwezig; leeg; 2026-08-31 | einde | PASS (5) | PASS (5) | PLAYED |
| case5-geen-eindiging-wel-nieuw-recht | 1 0 0 1 | aanwezig; leeg; 2026-11-30 | grant | PASS (5) | PASS (5) | PLAYED |
| case6-niet-werkloos-wel-vervallen | 0 0 1 0 | afwezig; leeg; 2026-11-30 | niets | PASS (5) | PASS (5) | PLAYED |
| case7-invoer-staat-tegen-blokkade | 1 1 1 1 + invoer | aanwezig (alleen kenmerk en consistent gepind) | blokkade, invoer staat | PASS (2) | PASS (2) | PLAYED |
| case8-niet-werkloos-boven-grens-vervallen | 0 1 1 0 | afwezig; 123; 2027-04-02 | niets | PASS (5) | PASS (5) | PLAYED |
| case9-geen-eindiging-wel-vervallen-en-nieuw-recht | 1 0 1 1 | aanwezig; leeg; 2026-11-30 | grant | PASS (5) | PASS (5) | PLAYED |

Exec-tak over alle negen: `PLAYED 9`. DRL-teksttak over alle negen:
`EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults
lowering` — `CaseRunner.drlTak` weigert elk document met een niet-lege
`prioritizedDefaults` vóór enige lowering, omdat die tak geen
defaults-lowering heeft; de DRL_CHANNEL-regel van gate 12 voor dit golden
staat ongewijzigd. Verifier: PASS.

**Rood-eerst** (de nieuwe cases tegen het `ir.json` van 3a140fe4; ref =
gate-5-runner, clingo = gate-6-runner, exec = de gate-12-jar):

```
case5  ref:    MISMATCH: W5 lacks characteristic "recht op uitkering"
       clingo: MISMATCH: W5 lacks characteristic "recht op uitkering"
case6  ref:    MISMATCH: W6 carries characteristic "recht op uitkering", expected absent
       clingo: MISMATCH: W6 carries characteristic "recht op uitkering", expected absent
case7  ref:    oracle: PASS (2 expectations)
       clingo: clingo: PASS (2 expectations)
case8  ref:    MISMATCH: W8 carries characteristic "recht op uitkering", expected absent
       clingo: MISMATCH: W8 carries characteristic "recht op uitkering", expected absent
case9  ref:    MISMATCH: W9 lacks characteristic "recht op uitkering"
       clingo: MISMATCH: W9 lacks characteristic "recht op uitkering"
exec:  FAIL case5-geen-eindiging-wel-nieuw-recht.json: kenmerk recht op uitkering: verwacht aanwezig, werkelijk afwezig | case6-niet-werkloos-wel-vervallen.json: kenmerk recht op uitkering: verwacht afwezig, werkelijk aanwezig | case8-niet-werkloos-boven-grens-vervallen.json: kenmerk recht op uitkering: verwacht afwezig, werkelijk aanwezig | case9-geen-eindiging-wel-vervallen-en-nieuw-recht.json: kenmerk recht op uitkering: verwacht aanwezig, werkelijk afwezig
```

case1–case4 passeren op beide bomen (verwachting ongewijzigd). case7
passeert op beide: zij pint gedrag dat niet verandert.

**Gate 7, de overleversregel** (voorstel, `scripts/check-noop.sh`):
`corpus/golden/anchor/ww-herleving-inkomen/case7-invoer-staat-tegen-blokkade.json`,
met de reden dat een `besluit: false` de lagere defaults verdringt en niets
schrijft, dus een invoerkenmerk op het doel laat staan (ch. 7.1 "not a
retraction"; ch. 5.3), gepind zolang de kaart
kenmerkinvoer-en-invoer-boven-berekening ch. 7.8 laat zoals het is. Zonder de
regel is gate 7 rood (exit 1): `FAIL:
corpus/golden/anchor/ww-herleving-inkomen/case7-invoer-staat-tegen-blokkade.json
is satisfied by a do-nothing executor and is not a pinned survivor`; met de
regel groen. De gate dwingt het pad af, niet de redentekst: die is, zoals bij
de 36 bestaande regels, proza voor de reviewer. case5, case6, case8 en case9
weerleggen de no-op-uitvoerder zelf (hun afgeleide attributen).

**Gates**, elk als losse aanroep in de worktree; vóór = 3a140fe4, na =
40849db2. Alle veertien groen, vóór en na.

| gate | vóór | na |
|---|---|---|
| 1 self-contained | PASS | PASS |
| 2 ir-schema | 278 bestanden | 278 bestanden |
| 3 verifier | 278 golden schoon, 440 negatief | idem |
| 4 case-schema | 638 case-files | 643 |
| 5 oracle | suite 806; 595 cases | suite 806; 600 cases |
| 6 clingo | suite 523; 595 cases | suite 523; 600 cases |
| 7 noop | 559 weerleggen; 36 overlevers | 563; 37 |
| 8 rood | 43 rood; Drools 9 + 34; 86 fragmenten | idem |
| 9 catala | 201 checks; 12 goldens | idem |
| 10 regelspraak | 409 tests; 70 goldens | idem |
| 11 toka | 45 rgs; 27 tests; 29 cases | idem |
| 12 drools | played 182; auto 50; divergentie 1; DRL 161; JUnit 620 | idem |
| 13 reftype | 131 twists over 27 goldens; 128 replays; DRL 25 | idem |
| 14 flogic | 130 checks; 2 goldens | idem |

De played-pin van gate 12 (182) en de DRL-pin (161) bewegen niet: zelfde
golden, zelfde kanalen.

**Registerzin** (voor de hoofdsessie, `spec/CONSTRUCTS.md` rij 8):

> **Anchor repair (2026-09-23, card `anker-ww-herleving-dragervoorwaarde`):**
> `anchor/ww-herleving-inkomen` writes each nested default's justification
> self-contained, as ch. 7.1 prescribes — its carrier's conjuncts first, then
> its own, as one flat `ALLE` (einde `werkloos ∧ T`, herleving `+ V`, blokkade
> `+ N`) — because ch. 7.9 decides a default's exception groups before and
> regardless of its own justification; the tree and the four existing
> expectations are unchanged. Four new cases pin the four inputs on which the
> unconjoined tree answered against art. 15/21 WW, identically in ref, clingo
> and the Drools exec branch (case5/6/8/9; case8 also separates the full
> conjunction from one without `werkloos`), and a fifth pins the input rule on
> a characteristic target: a `besluit: false` leaves an input-set target
> characteristic standing (ch. 7.1, "not a retraction"; case7, a
> reason-carrying gate-7 survivor until card
> `kenmerkinvoer-en-invoer-boven-berekening` decides otherwise), and a sixth
> pins the blokkade's own `nieuw maandloon > inkomensgrens` conjunct (case10,
> case3 with the new right's maandloon below the grens). The target
> characteristic is marked bezittelijk, the spelling its RegelSpraak twin
> writes (`heeft (geen) recht op uitkering`); measured, no executor reads
> the kind. No wire, verifier or executor change; the DRL-text branch keeps
> refusing the golden on its defaults sentence.

**Open punten.**

1. Gate 13 toetst dit golden niet. Het anker heeft nul verdraaibare
   annotaties: de enige eenheid, `dag`, declareert geen canoniek en geen
   factor, en de drie eenheiddragende annotaties zijn twee vrijgestelde en één
   zonder converteerbare zus, vóór en na gelijk. Acceptatie (d) van de kaart
   eist alleen veertien groene gates, en die zijn groen. Niet de kaart maar
   de opdracht aan de bouwer ging uit van een onjuiste premisse: zij scherpte
   (d) aan met "gate 13 draait over dit golden met zijn cases". Gate 13
   blijft groen, maar raakt deze kaart niet. Dekking vergt converteerbare
   eenheden in het model; dat valt buiten deze kaart.
2. **Voor de tweelingkaart dialect-uitzondering-op-uitzondering.**
   - *Vorm.* De RegelSpraak-tweeling moet dezelfde platte `ALLE`-vorm
     uitzenden als het anker: per geneste default één `Samengesteld` met
     kwantor `ALLE`, de dragerconjuncten voorop in de volgorde van de
     drager, `werkloos` ook in de uitzondering op de basis (einde), en
     `recht op uitkering` bezittelijk. Ze draagt tien cases, niet de "zeven
     cases" van haar kaarttekst. De spelling in de bron is sinds
     2026-09-23 besloten (besluit 4d): elk blok schrijft alleen zijn eigen
     voorwaarde, het frontend conjungeert; §8.2 van het voorstel geeft de
     bron.
   - *Meting van de lens, nagemeten in fixronde 3.* Elk conjunct apart
     weggehaald, alle tien cases in ref en clingo.
     - Twee van de zes dragerconjuncten zijn waarneembaar: `werkloos` in de
       herleving (case8 valt) en T in de blokkade (case9 valt). Sinds
       case10 is ook het eigen conjunct `nieuw maandloon > inkomensgrens`
       van de blokkade gepind (case10 valt).
     - Vier dragerconjuncten hebben geen waarneembaar effect: `werkloos` in
       einde, `werkloos` in de blokkade, T in de herleving en V in de
       blokkade. Bij T en V geeft een lagere default hetzelfde besluit (de
       grant `waar`, einde `onwaar`). Bij de twee `werkloos`-conjuncten
       beslist de mutant `onwaar` waar het anker niets beslist, en een
       `onwaar`-besluit schrijft niets, dus het kenmerk blijft in beide
       gevallen afwezig.
   - *Wat die vier bewaakt.* Geen gate. De vier mutanten zijn
     schema-geldig, hun round-trip is identiek (gate 2 slaagt dus), de
     verifier geeft PASS en alle tien cases blijven groen. De lens schreef
     dat gate 2 ze met een round-trip bewaakt; dat klopt niet, want gate 2
     toetst alleen de serialisatievorm. Ze worden bewaakt door de review
     van het beschermde pad en, straks, door de frontend-diff van de
     tweeling tegen dit anker.
   - *Gevolg.* De tweelingkaart moet de platte vorm met de dragerconjuncten
     voorop uitdrukkelijk eisen; een tweeling zonder die vier conjuncten
     haalt alle cases.
   - De kaarttekst bijwerken is werk voor de hoofdsessie.
3. *Gefixt in fixronde 2.* Het voorstel `DIALECT-DEFAULTS-NIET-EN-NESTING.md`
   volgt het gelande anker nu overal: §2.3 en §8.1 sinds fixronde 1, en §1
   punt 6, §2.2 M12, §5.4, §8.2 en §8.4 sinds fixronde 2 (commit 9c01eb36,
   zie hieronder).
4. *Gefixt in fixronde 2.* `recht op uitkering` is `BEZITTELIJK` in
   `kenmerkSoorten` (commit 48800a13, op verzoek van de maintainer). De
   tweeling schrijft "heeft (geen) recht op uitkering" en moet byte-identiek
   op dit anker uitkomen. De soort stuurt alleen die bronspelling; geen
   uitvoerder leest haar (meting hieronder).
5. Het wetsantwoord van case8 steunt op de lezing van `werkloos` als de grond
   van art. 15 op het moment dat het recht ontstond. De NOTE schrijft die
   lezing nu uit; case4 steunde er al op (werkloos én een inkomen boven de
   grens). De maintainer bevestigt of verwerpt haar; verwerpt hij haar, dan
   vervalt case8 en is het `werkloos`-conjunct een ontwerpkeuze zonder
   getuige.
6. Het eigen conjunct `nieuw recht ontstaan` van de blokkade pint geen case,
   ook na case10 niet (gemeten: een blokkade zonder dat conjunct haalt alle
   tien cases). Het is alleen waarneembaar met een nieuw maandloon boven de
   grens zonder nieuw recht, een invoer die in de casus niets betekent. De
   NOTE zegt dat; een case ervoor is niet toegevoegd.

**Fixronde 1 (hoofdsessie, 2026-09-23).** Verbreding van de may-touch-set,
op voorstelmandaat: `spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md`,
alleen §2.3 en §8.1. §2.3 heet nu "The anchor's corners" en draagt de
opsomming over de zestien combinaties: de ongeconjungeerde boom wijkt op
vier af (probes A–D = case6, case5, case8, case9). Die opsomming is als
[I] gemerkt; dat de drie uitvoerders op elk van de vier het ongeconjungeerde
antwoord geven, is [M]. De sectie meldt ook de twee gemeten reparaties
(zonder `werkloos` faalt alleen C; de gelande slaagt op alle negen) en de
lezing van `werkloos` waarop C rust. §8.1 legt de gelande vorm vast: het
platte `ALLE` per geneste default met de dragerconjuncten voorop,
`werkloos` ook in einde, negen cases met namen, verwachting en beslisser,
case7 in de overleverslijst van gate 7, `recht op uitkering` blijft
`BIJVOEGLIJK`. Waar het afwijkt van de eerste versie, noemt het die. De
sectie sluit af met het gevolg voor G2: dezelfde platte vorm, negen cases.
Anker, NOTE en `check-noop.sh` zijn niet aangeraakt. Gate 1 opnieuw: PASS.

**Fixronde 2 (hoofdsessie, 2026-09-23).** Twee items, elk een eigen commit.

*Item 1, commit 48800a13: `recht op uitkering` wordt `BEZITTELIJK`* (mandaat
voor `ir.json` en NOTE). Eén waarde in `kenmerkSoorten`. De NOTE zegt nu
waarom (de tweeling) en dat de soort alleen de bronspelling stuurt: `heeft
(geen) recht op uitkering` in plaats van `is (niet) recht op uitkering`.
Gemeten vóór (`BIJVOEGLIJK`) en na (`BEZITTELIJK`):
- verifier PASS, alle negen cases PASS in ref en clingo, exec `PLAYED 9`,
  DRL `EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no
  defaults lowering`; de volledige uitvoer van vóór en na is byte-identiek;
- clingo: via een PATH-shim het ASP-programma per case vastgelegd dat
  clingo oplost, vóór en na: 9 van 9 byte-identiek. Het kenmerk staat in
  beide als `heeft(S,"recht op uitkering")`: het predicaat is uniform en
  hangt niet van de soort af;
- Drools: `IrLoader` (r. 354) parseert de tabel in het model, en geen code
  in de backend leest haar terug. De enige aanroepers van `kenmerkSoort()`
  zitten in de `RuleChecker` van het frontend, die de backend niet
  compileert. De beanleden uit `FactModelGenerator.leden` (een probe tegen
  de gate-12-jar) zijn vóór en na gelijk: `Lid[eigenschap=rechtOpUitkering,
  javaType=boolean, isVorm=true]`; alleen de ingelezen tabel verschilt;
- ref en de ASP-backend verwijzen nergens in hun bibliotheekcode naar
  `kenmerk_soorten`; de gedeelde OCaml-loader parseert de tabel en de
  verifier toetst alleen verweesde sleutels. IR-SPEC zegt hetzelfde: "which
  the Drools loader parses and nothing reads back";
- gate 2 PASS (278 bestanden, round-trip), gate 3 PASS, gate 12 PASS met
  played 182 en DRL 161 (JUnit 620). case1–case4 zijn byte-ongewijzigd
  tegenover 3a140fe4.

*Item 2, commit 9c01eb36: de overige verouderde plaatsen in het voorstel.*
- §1 punt 6, M12, §5.4 en §8.4 staan nu als gedateerde metingen (8dd05e25)
  naast de gelande stand: vier verschilpunten, negen cases, PLAYED 9.
- M12 is opnieuw gemeten op het gelande anker, omgebouwd naar de ketenvorm
  van de tweeling (einde als ketenpositie boven toekenning, dieper genest):
  verifier PASS; ref en clingo 9 van 9; exec `PLAYED 9`; DRL `EXCLUDE`.
- §8.2 geeft de tweelingbron volgens het gelande anker: `werkloos` in
  einde, herleving en blokkade; `recht op uitkering` bezittelijk; negen
  cases.
- §8.2 zegt uitdrukkelijk dat de spelling van de dragerconjunctie in de
  bron afhangt van besluit 4d, dat nog open is. Het geeft beide spellingen,
  zonder te kiezen: (1) per uitzondering uitgeschreven (optie A), of (2)
  impliciet in een beperkende `Uitzondering op`, met `Afwijking van` voor
  verruimingen (de lezing van besluit 4d van de tweelingkaart). Beide
  moeten op dezelfde platte `ALLE` uitkomen.

Alle veertien gates opnieuw groen op de inhoud van deze ronde. Gate 1, 2, 3
en 12 draaiden na item 1; gate 4 tot en met 11, 13 en 14 draaiden erna. Item
2 raakt alleen het voorstel, buiten elke gatezone; gate 1 is daarna
opnieuw gedraaid: PASS.

**Fixronde 3 (hoofdsessie, 2026-09-23; oordeel van de lens: mergen, alle
42 verwachtingen met de hand nagerekend).** Zes adviezen, elk een eigen
commit, plus één gevolgcommit op het voorstel. Volgorde van de commits: 1,
3, 4, 2, gevolg, 5, 6, zodat de telling in de tekst van item 2 al klopt met
case10.

- **1, a94301ff — NOTE, case7:** de verwachting is voorlopig. Ch. 7.8 zegt
  niets over een kenmerkdoel in de richting onwaar; optie 7-i van de kaart
  kenmerkinvoer-en-invoer-boven-berekening legt het gedrag vast, optie 7-ii
  laat de verwachting omslaan.
- **3, ac088a23 — NOTE, case6 en case8:** hun data horen bij een recht dat
  onder de grondslaglezing nooit ontstond. Ze staan er bewust (case8 is
  case1 met één verschil) en zijn geen bewijs dat er een recht was.
- **4, 22730193 — `case10-nieuw-recht-onder-grens`:** de feiten van case3
  met een nieuw maandloon van 2000,00. Handwaarden: aanwezig (de herleving
  beslist); `inkomensgrens` 2100,00; `verstreken periode` 92; `nieuwe
  maxdatum` 2026-01-15; consistent. Handberekening in de NOTE.
  - Rood-eerst op een mutant zonder `nieuw maandloon > inkomensgrens` in de
    blokkade, letterlijk:
    - ref: `MISMATCH: W10 lacks characteristic "recht op uitkering"`
    - clingo: dezelfde regel
    - exec: `FAIL case10-nieuw-recht-onder-grens.json: kenmerk recht op
      uitkering: verwacht aanwezig, werkelijk afwezig`
  - Op het anker: ref `oracle: PASS (5 expectations)`, clingo `clingo: PASS
    (5 expectations)`, exec `PLAYED 1`; alle tien samen: exec `PLAYED 10`,
    DRL `EXCLUDE`.
  - Geen overlever: `--noop` geeft exit 1 met vier MISMATCH-regels.
  - De soortmeting is over tien cases herhaald: met `BIJVOEGLIJK` en
    `BEZITTELIJK` dezelfde uitkomsten (exec `PLAYED 10`), en het
    ASP-programma van case10 is byte-identiek.
- **2, 05793e66 — `check-noop.sh`, alleen tekst (voorstel):** de reden van
  case7 zegt nu "as long as the card … does not choose its option 7-ii". Met
  case10 zijn er negen broercases.
- **Gevolg, 326ed500 — voorstel:** de tellingen staan op tien en case10 staat
  in de tabel van §8.1. De ketenvorm van de tweeling is opnieuw gemeten:
  verifier PASS, ref en clingo 10/10, exec `PLAYED 10`, DRL `EXCLUDE`.
- **5, eaa3fd04 — open punt 2:** de mutantmeting, met één correctie op de
  lens: gate 2 bewaakt de vier onwaarneembare conjuncten niet.
- **6, 7779e436 — open punt 1:** gecorrigeerd.

**Mutantmeting.** Elk conjunct apart weggehaald, tien cases; ref en clingo
geven overal dezelfde uitkomst.

| Mutant | Welke cases vallen |
|---|---|
| einde zonder W | geen |
| einde zonder T | case2, case5, case9 |
| herleving zonder W | case8 |
| herleving zonder T | geen |
| herleving zonder V | case4 |
| blokkade zonder W | geen |
| blokkade zonder T | case9 |
| blokkade zonder V | geen |
| blokkade zonder `nieuw recht ontstaan` | geen |
| blokkade zonder `nieuw maandloon > inkomensgrens` | case10 |

De vier mutanten die een onwaarneembaar dragerconjunct weghalen, zijn
schema-geldig en canoniek (round-trip identiek) en krijgen van de verifier
PASS.

**Gates**, elk als losse aanroep, gemeten vóór de rebases (op de commit
die nu 7779e436 heet; de inhoud van het anker is sindsdien niet veranderd):

| gate | uitslag |
|---|---|
| 2 | PASS, 278 bestanden, round-trip |
| 4 | PASS, 644 case-files |
| 5 | PASS, suite 806, 601 cases |
| 6 | PASS, suite 523, 601 cases |
| 7 | PASS, 564 weerleggen, 37 overlevers |
| 12 | PASS, played 182, DRL 161, JUnit 620 |
| 13 | PASS, 131 twists over 27 goldens, 128 replays |
| 1 | PASS |

**Fixronde 4 (hoofdsessie, 2026-09-23).** De maintainer besliste op main
(e7e9e178) de drie punten en sneed de kaart
`voorstel-niet-en-nesting-besluiten`. Omdat deze branch het voorstel al
bewerkte, ging die kaart hier mee.
- *Rebase op e7e9e178*, conflictvrij; de hashes in dit Result en in het
  voorstel zijn omgezet (commit `kaart en voorstel: commit-hashes na de
  rebase op e7e9e178`).
- *De besluitenkaart*: geclaimd, uitgevoerd en naar done, met een eigen
  Result en registerzin (zie `backlog/done/20260923-voorstel-niet-en-nesting-besluiten.md`).
  Het voorstel draagt besluit 2 (een genegeerde basis toegestaan, met een
  waarschuwing), besluit 4d (beperkende `Uitzondering op` met erven, ook
  aan de wortel; `Afwijking van` voor verruiming) en de bevestigde
  ankerconjunctie als besloten. De "kies er geen"-passage uit fixronde 2
  is vervallen, en de verwijzingszinnen in de twee dialectkaarten zijn weg.
- *Eindrebase op main a1b9daf0*, conflictvrij; alle hashes zijn opnieuw
  omgezet. Commits van deze kaart na de eindrebase: f93234e4 (claim),
  40849db2 (werk), 78a0b801 (Result), f0faa257 en 9c01eb36 (voorstel,
  rondes 1–2), 48800a13 (bezittelijk), d0df9512 (Result ronde 2),
  a94301ff, ac088a23, 22730193 en 05793e66 (ronde 3), 326ed500 (voorstel,
  tien cases), eaa3fd04, 7779e436 en b9a76f22 (Result ronde 3), 5eb1df82
  (hashes). Van de besluitenkaart: 62c4873b (claim), 0a8a1fca (voorstel),
  c2ef9c07 (dialectkaarten) en fcf4756e (done).
- *Open punt 2 aangevuld*: onder besluit 4d is de spelling van de tweeling
  nu besloten — elk blok schrijft alleen zijn eigen voorwaarde en het
  frontend conjungeert. De eis aan de wire blijft dezelfde platte `ALLE`
  met de dragerconjuncten voorop.
- *Gates na de eindrebase*, elk als losse aanroep op 9f2911ba (deze
  laatste commit verandert alleen deze kaarttekst):

  | gate | uitslag |
  |---|---|
  | 1 | PASS |
  | 2 | PASS, 282 bestanden, round-trip (278 van deze branch plus 4 rode documenten van main) |
  | 4 | PASS, 648 case-files |
  | 5 | PASS, suite 806, 601 cases |
  | 6 | PASS, suite 523, 601 cases |
  | 7 | PASS, 564 weerleggen, 37 overlevers |
  | 12 | PASS, played 182, DRL 161, JUnit 620 |
  | 13 | PASS, 131 twists over 27 goldens, 128 replays |
