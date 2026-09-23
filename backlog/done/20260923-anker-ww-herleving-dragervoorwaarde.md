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
848c6ebb (claim), 560859e0 (het werk), en deze. De vier bestaande case-files
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
560859e0. Alle veertien groen, vóór en na.

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
> `kenmerkinvoer-en-invoer-boven-berekening` decides otherwise). No wire,
> verifier or executor change; the DRL-text branch keeps refusing the golden
> on its defaults sentence.

**Open punten.**

1. Gate 13 toetst dit golden niet. Het anker heeft nul verdraaibare
   annotaties: de enige eenheid, `dag`, declareert geen canoniek en geen
   factor, en de drie eenheiddragende annotaties zijn twee vrijgestelde en één
   zonder converteerbare zus, vóór en na gelijk. Acceptatie (d) gaat dus van
   een onjuiste premisse uit: gate 13 blijft groen, maar raakt deze kaart
   niet. Dekking vergt converteerbare eenheden in het model; dat valt buiten
   deze kaart.
2. De tweelingkaart dialect-uitzondering-op-uitzondering noemt "zeven cases";
   het worden er negen. Voor byte-identiteit na canonicalisatie moet de
   tweeling dezelfde vorm uitzenden: een plat `ALLE`, dragerconjuncten
   voorop, en `werkloos` ook in de uitzondering op de basis (einde), wat haar
   besluit 4d ("ook aan de wortel") oplevert. De kaarttekst bijwerken is werk
   voor de hoofdsessie.
3. Het voorstel `DIALECT-DEFAULTS-NIET-EN-NESTING.md` §2.3 en §8.1 beschrijft
   een andere reparatie (zonder `werkloos`, andere casenamen, "check-noop.sh
   needs no new entry"). Deze kaart vervangt dat; het voorstel is niet
   aangeraakt.
4. §8.1 van dat voorstel zette `recht op uitkering` ook op `BEZITTELIJK`, voor
   "heeft (geen) recht op uitkering" in de tweeling. Dat staat niet in deze
   kaart en is niet gedaan; heeft de tweeling het nodig, dan is het een
   wijziging aan dit anker (beschermd pad) om in die kaart voor te stellen.
5. Het wetsantwoord van case8 steunt op de lezing van `werkloos` als de grond
   van art. 15 op het moment dat het recht ontstond. De NOTE schrijft die
   lezing nu uit; case4 steunde er al op (werkloos én een inkomen boven de
   grens). De maintainer bevestigt of verwerpt haar; verwerpt hij haar, dan
   vervalt case8 en is het `werkloos`-conjunct een ontwerpkeuze zonder
   getuige.
