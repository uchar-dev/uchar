# anker-nestkenmerk-dragervoorwaarde: de ongeordende broers van het nestkenmerk-anker dragen de voorwaarde van hun drager

Vervolg op anker-ww-herleving-dragervoorwaarde (geland 2026-09-23) en
maintainersbesluit 2026-09-23 (tweede besluitvraag): de leden van
`Uitzonderingen ongeordend op X:` erven de rechtvaardiging van X, net als
een enkele `Uitzondering op X`. Het handanker `anchor/jutterswet-nestkenmerk`
(RUNTIME-SEMANTICS ch. 7.9, het kenmerkdoel `Jutter.vrijgesteld`) draagt
zijn ongeordende uitzonderingsgroep vandaag zonder die voorwaarde: de
wortel beslist `waar` op `nx`, de broers beslissen `onwaar` op `ny` en
`waar` op `nz`, elk alleen op het eigen kenmerk. De sessie van het
WW-anker mat: met `nz` en zonder `nx` zeggen ref, clingo en de
Drools-exec-tak "aanwezig", want de broer beslist buiten zijn drager om;
een RegelSpraak-tweeling die `nx` erft zou "afwezig" geven en kon dus
niet op dit anker uitkomen. De drie bestaande cases (`case1-y-wint`:
nx, ny; `case2-conflict`: nx, ny, nz; `case3-terugval`: nx) hebben `nx`
alle drie en veranderen niet van verwachting.

## Ontwerp

- De twee broers krijgen de voorwaarde `ALLE[nx, ny]` respectievelijk
  `ALLE[nx, nz]` (plat, drager eerst, zoals het WW-anker), de wortel blijft
  `nx`; de rose tree en de `ongeordend: true`-markering blijven staan.
- Eén nieuwe case, `case4-nz-zonder-nx` (alleen `nz` → `vrijgesteld`
  afwezig, consistent): pint het erven. Rood-eerst: op het huidige anker
  geven alle drie de uitvoerders "aanwezig".
- De NOTE (er is er nog geen naast de cases; de spec-tabel in ch. 7.9
  r. 5028–5037 beschrijft de drie cases) legt de dragervoorwaarde uit met
  dezelfde alinea-opbouw als het WW-anker. De tabel in ch. 7.9 is
  beschermd; een vierde rij daar gaat als **voorstel** mee, of de NOTE
  draagt haar.
- Niet in deze kaart, wel te noteren: de attribuutankers
  `jutterswet-nestuitzondering` en `jutterswet-nestdiepte` hebben dezelfde
  vorm (ongeordende groepen onder `grondX` zonder `grondX` in de leden) en
  geen case zonder de drager; er is geen tweeling voor gepland. Of zij mee
  moeten, beslist de maintainer als zo'n tweeling er komt.

## Acceptatie (bij oppakken aanscherpen)

- (a) Rood-eerst: `case4-nz-zonder-nx` faalt op het huidige `ir.json` in
  ref, clingo en de Drools-exec-tak; groen na de conjunctie in alle drie;
  case1–case3 ongewijzigd in verwachting.
- (b) Gate 3 (verifier) groen: WF-050/WF-075 op de sub-defaults; gates 5,
  6, 12 groen; gate 12 pins ongewijzigd (zelfde golden); gate 7: case4
  overleeft geen no-op-uitvoerder (verwachting afwezig, plus `consistent`).
- (c) Alle veertien gates groen.

May touch: `corpus/golden/anchor/jutterswet-nestkenmerk/**` (**voorstel**,
handanker), `spec/RUNTIME-SEMANTICS.md` ch. 7.9 tabel (**voorstel**, één
rij), kaartverplaatsing.

Niet in deze kaart: het dialect; de attribuutankers.

## Result

Geland op de kaartbranch als **voorstel** (beschermde paden; de maintainer
merget): `corpus/golden/anchor/jutterswet-nestkenmerk/` (`ir.json`, nieuw
`case4-nz-zonder-nx.json`, nieuw `NOTE.md`) en één rij in de tabel van
`spec/RUNTIME-SEMANTICS.md` ch. 7.9. Commits: 84fd363f (claim), ee21825f
(`ir.json` en case4), 49513ab4 (NOTE), 14e86186 (spec-rij, voorstel), en
deze. case1–case3 zijn byte-ongewijzigd; in `ir.json` veranderen alleen de
twee broervoorwaarden.

**De boom vóór (98d888ee) en na.**

```
vóór: Default 0 waar als nx; excepties ongeordend { onwaar als ny ; waar als nz }
na:   Default 0 waar als nx; excepties ongeordend { onwaar als ALLE[nx, ny] ; waar als ALLE[nx, nz] }
```

Wortel, rose tree en `ongeordend: true` staan er ongewijzigd. Elke `ALLE`
heeft `drempel: 0` en als `bron` de twee conjunctbronnen met " en "
ertussen ("hij nx heeft en hij ny heeft"), zoals in het WW-anker.

**Afwijking van de kaarttekst: case4 heeft drie instanties.** De kaart
vraagt "alleen `nz` → afwezig, consistent" en een case4 die geen
no-op-uitvoerder overleeft. Met één instantie kan dat niet allebei. Gemeten:
case4 met alleen `nz` gaf onder `runner.exe --noop` exit 0. Zij klopt dan al
op de ruwe invoer, net als `case1-y-wint`, en gate 7 zou haar als niet-gepinde
overlever rood melden. De overleverslijst staat in `scripts/check-noop.sh`;
dat script is beschermd, valt buiten de may-touch-set, en de opdracht
verbood case4 als overlever. De vastgelegde case4:

- **M** (`nz`) → afwezig: de pin van de kaart;
- **N** (`nx`, `nz`) → aanwezig: verschilt van M alleen in de drager en
  weerlegt de no-op (`MISMATCH: N lacks characteristic "vrijgesteld"`,
  exit 1);
- **O** (`ny`, `nz`) → afwezig, geen conflict: de tweede combinatie waarop
  de oude boom anders besliste (hieronder). Door O toetst de verwachting
  `consistent` ook iets.

Daarbij geldt `consistent` voor de hele case. De naam `case4-nz-zonder-nx`
dekt M en O. O vangt geen mutant die M niet ook vangt (mutantmeting); wie O
niet wil, verliest alleen de pin van de tweede afwijkende combinatie.

**Rood-eerst** (op het `ir.json` van 98d888ee; ref = de runner van gate 5,
clingo = de runner van gate 6, exec = de jar van gate 12 zonder vlag).
Eerste meting, case4 met alleen M:

```
ref:    MISMATCH: M carries characteristic "vrijgesteld", expected absent
clingo: MISMATCH: M carries characteristic "vrijgesteld", expected absent
exec:   FAIL case4-nz-zonder-nx.json: kenmerk vrijgesteld: verwacht afwezig, werkelijk aanwezig
```

De vastgelegde case4 (M, N, O):

```
ref:    MISMATCH: M carries characteristic "vrijgesteld", expected absent
        MISMATCH: expected consistent, but the channel carries: Jutter.vrijgesteld/0 × O
clingo: MISMATCH: M carries characteristic "vrijgesteld", expected absent
        MISMATCH: expected consistent, but the model carries 1 inconsistent fact(s)
exec:   FAIL case4-nz-zonder-nx.json: kenmerk vrijgesteld: verwacht afwezig, werkelijk aanwezig; verwacht consistent, werkelijk inconsistent (Jutter.vrijgesteld/0 @ O)
```

N geeft op de oude boom geen fout. **Groen** op het nieuwe `ir.json`:
case1–case3 `oracle: PASS (2 expectations)` en `clingo: PASS (2
expectations)`; case4 `oracle: PASS (4 expectations)` en `clingo: PASS (4
expectations)`; de exec-tak over de vier cases `PLAYED 4`; de DRL-teksttak
ongewijzigd `EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch
has no defaults lowering`. De verifier op het bestand, vóór en na:
`verifier: PASS`. Onder `--noop`: case1 exit 0 (de bestaande overlever),
case2, case3 en case4 exit 1.

**De acht combinaties** (X = `nx`, Y = `ny`, Z = `nz`; ref en clingo
gelijk, oud tegen nieuw gemeten met proefcases buiten de repo). De oude boom
beslist op precies twee combinaties anders, en op beide ontbreekt X: bij
¬X ¬Y Z was de uitkomst aanwezig, nu afwezig (M); bij ¬X Y Z was er een
conflict `Jutter.vrijgesteld/0`, nu niet (O). Nu geldt: aanwezig ⇔ X ∧ ¬Y,
conflict ⇔ X ∧ Y ∧ Z.

**Mutantmeting** (ref en clingo, de vier cases):

| Mutant | Welke cases vallen |
|---|---|
| broer `ny` zonder `nx` | geen |
| broer `nz` zonder `nx` | case4: `MISMATCH: M carries characteristic "vrijgesteld", expected absent` en `MISMATCH: O carries characteristic "vrijgesteld", expected absent` |
| conjunctvolgorde omgekeerd (`ALLE[ny, nx]`, `ALLE[nz, nx]`) | geen |

Het conjunct `nx` in de broer `ny` is op alle zestien invoeren (de acht
combinaties, met en zonder `vrijgesteld` in de invoer) onwaarneembaar: 0
verschillen tussen anker en mutant, in ref en clingo. Waar de mutant
`onwaar` beslist, beslist het anker niets, en in geen van beide gevallen
wordt er iets geschreven. Omdat default 0 de enige default van de
wortelketen is, verdringt het `onwaar`-besluit van de mutant ook niets
(aangevuld in fixronde 1; zie open punt 3).

**Gates**, elk als losse aanroep na `eval $(opam env)`, op 14e86186 (de
inhoud van deze branch; deze laatste commit raakt alleen `backlog/`):

```
check-self-contained: PASS
check-ir-schema: PASS (schema valid; 26 pattern self-test row(s); 22 shape self-test row(s); 286 file(s) validate and round-trip, floor 192)
check-verifier: PASS (286 golden clean; 444 negatief rejected for their declared rule, floor 350)
check-case-schema: PASS (schema valid; 92 pattern self-test row(s); 659 case file(s) validate)
check-oracle: PASS (ref suite 932 worked examples, floor 489; 611 cases replayed exactly, floor 427; NOT replayed, no case file yet: corpus/golden/jutterswet-seizoenvondst corpus/golden/jutterswet-vormenkabinet)
check-clingo: PASS (backend suite 523 checks, floor 83; 611 cases replay exactly through clingo — ref = clingo over the corpus)
check-noop: PASS (574 cases refute a do-nothing executor; 37 pinned survivors pin defeasible standing)
check-rood: PASS (48 red cases fault observably in ref and clingo, floor 26; 96 pinned fragments cite none of the 36 listed bottom sentences)
check-catala: PASS — frontend suite green (207 checks, floor 201); 12 catala golden(s) regenerate byte-identically (floor 12)
check-regelspraak: PASS — JUnit suite green (507 tests, floor 173); 71 rgs golden(s) re-emit byte-identically (floor 38)
check-toka-parity: PASS
check-drools: PASS
  played 185  (loaded off the wire, wholly lowered, matched the pinned ref≡clingo expectations)
  DRL-text branch played 164  (its OWN count — the narrower branch, not the square; pin 164)
  JUnit 679 tests, floor 239  (the backend's own suite, green with a non-zero count)
check-reftype-derivation: PASS (131 unit annotations twisted over 27 goldens, floor 74; 128 executor replays reproduce the untouched outcome, 25 of them PLAYED by the DRL-text branch, floor 13; 0 pinned survivors)
check-flogic: PASS — frontend suite green (135 checks, floor 81); 2 flogic golden(s) re-emit byte-identically (floor 1)
```

Gate 12: de pins 185 (played) en 164 (DRL) zijn ongewijzigd, het is
hetzelfde golden. Gate 7: de overleverslijst is ongewijzigd (37). Een
eerdere volledige run op ee21825f (vóór NOTE en spec-rij) gaf dezelfde
regels. De gates zijn niet vooraf op 98d888ee gedraaid; tellingen van vóór
deze kaart zijn daarom niet gemeten, alleen afgeleid (één case-file meer).

**Registerzin** (voorstel voor de hoofdsessie, `spec/CONSTRUCTS.md` rij 8,
na de WW-ankerreparatie):

> **Anchor repair (2026-09-23, card `anker-nestkenmerk-dragervoorwaarde`):**
> by the maintainer's decision of 2026-09-23 that the members of
> `Uitzonderingen ongeordend op X:` inherit X's condition as a single
> `Uitzondering op X` does, `anchor/jutterswet-nestkenmerk` writes each
> unordered sibling's justification self-contained — the carrier's `nx`
> first, then its own ground, as one flat `ALLE` (`nx ∧ ny`, `nx ∧ nz`);
> root, tree, `ongeordend` marking and the three existing expectations are
> unchanged (all three carry `nx`). Of the eight combinations of `nx`, `ny`,
> `nz` the unconjoined tree answered differently on exactly two, both
> without `nx` — `nz` alone attributed, `ny` with `nz` a
> `Jutter.vrijgesteld/0` conflict — and the new `case4-nz-zonder-nx` pins
> both (instances M and O) identically in ref, clingo and the Drools exec
> branch, with a contrast instance N (`nx`, `nz` → attributed) that makes
> the case refute a do-nothing executor, so gate 7's survivor list is
> untouched. The `nx` conjunct in the `ny` sibling is unobservable on every
> input, for three reasons: a `false` decision writes nothing and takes
> nothing away, the `nz` sibling does not decide without `nx` (so no
> conflict arises), and default 0 is the root chain's only default, so a
> `false` decision displaces nothing (measured over all sixteen inputs; with
> a lower root default `nw → attribute (true)` added, the difference shows
> on `ny, nw` with and without `nz`, in ref and clingo; under option 7-ii of
> card `kenmerkinvoer-en-invoer-boven-berekening` the first reason would
> lapse). It stands because ch. 7.1 makes every nested layer
> self-contained. RS ch. 7.9's description line names the conjunction
> (`{ nx ∧ ny → …, nx ∧ nz → … }`) and its table gains the case4 row. No
> wire, verifier or executor change; the DRL-text branch keeps refusing the
> golden on its defaults sentence. The attribute anchors
> `jutterswet-nestuitzondering` and `-nestdiepte` keep the unconjoined form
> (no twin planned).

Rij 8 zegt nu nog "Two points stay open for the maintainer: whether members
of `Uitzonderingen ongeordend op X:` inherit X's condition, …". Volgens deze
kaart is dat punt op 2026-09-23 beslist (de tweede besluitvraag); die zin
bijwerken is werk voor de hoofdsessie.

**Open punten.**

1. *De attribuutankers.* `jutterswet-nestuitzondering` en
   `jutterswet-nestdiepte` hebben dezelfde vorm en beslissen zonder
   `grondX` ook buiten hun drager om. Gemeten op de huidige ankers met
   proefcases buiten de repo (ref; clingo geeft dezelfde uitkomst in zijn
   eigen notatie):
   - nestuitzondering, `grondY` alleen: `MISMATCH: M.heffing = 10, expected
     leeg`; `grondZ` alleen: `M.heffing = 20`; `grondY`, `grondZ`: `the
     channel carries: Jutter.heffing/0 × M`;
   - nestdiepte, `grondY` alleen: `M.heffing = 10`; `grondP` alleen:
     `M.heffing = 30`; `grondQ` alleen (zonder `grondX` en zonder `grondP`):
     `M.heffing = 5`; `grondQ`, `grondR`: `the channel carries:
     Jutter.heffing/0/0 × M`.

   Er is voor deze ankers geen tweeling gepland. Of zij de dragervoorwaarde
   krijgen, beslist de maintainer als er een tweeling komt. Bij nestdiepte
   gaat het om twee niveaus: `grondQ` en `grondR` zouden `grondX` én
   `grondP` erven.
2. *Gefixt in fixronde 1 (mandaat uitgebreid).* De beschrijvingsregel
   boven de tabel in ch. 7.9 noemde de broers alleen op hun eigen grond;
   zij luidt nu "`{ nx ∧ ny → do-not-attribute (false), nx ∧ nz → attribute
   (true) }` — each justification conjoins its carrier's `nx` (ch. 7.1)"
   (commit e0a488c1).
3. *Wat geen gate bewaakt* (aangevuld in fixronde 1, lenspunt 5a). Het
   conjunct `nx` in de broer `ny` (groep 0) en de volgorde met de drager
   voorop zijn onwaarneembaar (mutantmeting). Geen gate bewaakt ze; alleen
   de review van het beschermde pad doet dat, en straks de frontend-diff van
   de RegelSpraak-tweeling tegen dit anker. Een tweeling zonder het
   conjunct in `ny` haalt alle vier de cases, dus de tweelingkaart moet de
   platte `ALLE` met `nx` voorop in beide broers uitdrukkelijk eisen. In
   uitvoering kan het conjunct alleen gepind worden met een lagere
   wortel-default. Gemeten met `waar als nw` (een nieuw kenmerk `nw`, beide
   varianten `verifier: PASS`): anker en mutant verschillen op `{ny, nw}`
   en `{ny, nz, nw}`, in ref en clingo (anker `oracle: PASS`, mutant
   `MISMATCH: M lacks characteristic "vrijgesteld"`). Zo'n default verandert
   de boom van het anker; of die er komt, beslist de maintainer.
4. *Voor de tweelingkaart.* Het anker draagt nu vier cases, en case4 heeft
   drie instanties. Een tweeling die `nx` erft, komt nu op dit anker uit.
   Een tweeling die het niet erft, valt op case4 (M en O).
5. *De `bron`-teksten in `ir.json`* (lenspunt 5b; bestond al vóór deze
   kaart). De `bron` van de bundel is "art. 7: de vrijstelling met een
   geneste uitzondering" (`ir.json` r. 156), terwijl art. 7 van de
   Jutterswet over vondstdrempel en penningbedrag gaat
   (`corpus/jutterswet/README.md` r. 45). De `bron`-spelling "hij nx heeft"
   is die van een bezittelijk kenmerk, terwijl `nx`, `ny` en `nz` bijvoeglijk
   zijn; de nieuwe `bron`-teksten van de conjuncties ("hij nx heeft en hij ny
   heeft") nemen die spelling over. De tweelingkaart beslist de spelling.
6. *Verouderde teksten buiten de may-touch-set* (lenspunt 5c; de
   hoofdsessie doet ze bij de landing):
   - `spec/CONSTRUCTS.md` r. 44 (rij 8): de erving door de leden van
     `Uitzonderingen ongeordend op X:` staat daar nog als open punt (zie
     hierboven);
   - `spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md` r. 1306 ("three
     cases") en r. 1329–1333 (de erving "is open", de leden dragen alleen hun
     eigen voorwaarde, "its three cases cannot tell the readings apart");
   - `spec/ASP-TRANSLATION.md` r. 2022–2026: de beschrijving van het anker
     noemt alleen case1 tot en met case3.

**Fixronde 1 (hoofdsessie, 2026-09-23).** De lens keurde de wijziging van
`ir.json`, de vier cases en de afwijking in case4 goed (handafleiding = ref
= clingo op 80 van 80 metingen) en vond één blokkerend punt plus advies. Het
mandaat is uitgebreid tot de beschrijvingsregel boven de tabel in ch. 7.9.
Elk punt is een eigen commit:

- **1 (blokkerend), e0a488c1 — RS ch. 7.9, de beschrijvingsregel.** Het
  hoofd van de voorbeelden belooft de ankers woordelijk. De regel luidt nu
  "`{ nx ∧ ny → do-not-attribute (false), nx ∧ nz → attribute (true) }` —
  each justification conjoins its carrier's `nx` (ch. 7.1)".
- **2, 719540a3 — RS ch. 7.9, de case4-rij.** "each exception group's
  justification" wordt "the justification of each exception group's
  default": een groep heeft volgens ch. 7.9 zelf geen rechtvaardiging.
- **3, af00b36e — NOTE, het onwaarneembare conjunct.** De uitleg noemt nu
  alle drie de redenen: een `onwaar`-besluit schrijft niets en neemt niets
  weg; de broer `nz` beslist zonder `nx` niet; default 0 is de enige
  default van de wortelketen, dus `onwaar` verdringt niets. Het verschil
  staat nu precies omschreven: "waar `ny` geldt en `nx` ontbreekt". De
  derde reden is zelf nagemeten (zie open punt 3). De NOTE noemt ook de
  afhankelijkheid van optie (7-ii) van de kaart
  kenmerkinvoer-en-invoer-boven-berekening: daaronder vervalt de eerste
  reden, en `ny` met `vrijgesteld` in de invoer, met en zonder `nz`, zou
  het verschil zichtbaar maken. Dat is een gevolgtrekking; geen uitvoerder
  implementeert (7-ii). De alinea over de zes gelijke combinaties noemt
  dezelfde derde reden, en de afgebroken regel is weg.
- **4, b68a4e52 — NOTE, taal.** Een broer krijgt "zijn"/"hij" (r. 6–7, 68,
  78, 187; r. 85 zat in punt 3). De overige "haar"/"zij" slaan op default
  0, een groep, de markering of de case en blijven staan. Verder "het
  geneste kenmerkdoel" en "Volgens ch. 7.9 beslissen de
  uitzonderingsgroepen …", met de alinea opnieuw uitgezet.
- **5** staat als open punten 3, 5 en 6 hierboven (niet gefixt).

`ir.json` en de vier case-files zijn in deze ronde niet veranderd:
`git diff ee21825f..HEAD` over de json-bestanden van het anker geeft 0
regels. Deze ronde raakt alleen `NOTE.md` en `spec/RUNTIME-SEMANTICS.md`;
dat is proza, en van de gates leest alleen gate 1 die bestanden. Gate 1, 2
en 3 zijn opnieuw gedraaid, elk los na `eval $(opam env)`, op b68a4e52:

```
check-self-contained: PASS
check-ir-schema: PASS (schema valid; 26 pattern self-test row(s); 22 shape self-test row(s); 286 file(s) validate and round-trip, floor 192)
check-verifier: PASS (286 golden clean; 444 negatief rejected for their declared rule, floor 350)
```

De regels van gate 4 tot en met 14 hierboven (op 14e86186) gelden voor
dezelfde `ir.json` en cases.
