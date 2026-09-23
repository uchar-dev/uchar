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
wordt er iets geschreven.

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
> input (a `false` decision writes nothing; measured over all sixteen
> inputs) and stands because ch. 7.1 makes every nested layer
> self-contained. RS ch. 7.9's table gains the case4 row. No wire, verifier
> or executor change; the DRL-text branch keeps refusing the golden on its
> defaults sentence. The attribute anchors `jutterswet-nestuitzondering` and
> `-nestdiepte` keep the unconjoined form (no twin planned).

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
2. *De beschrijvingsregel boven de tabel in ch. 7.9.* Die noemt de broers
   nog alleen op hun eigen grond: "unordered exception groups `{ ny →
   do-not-attribute (false), nz → attribute (true) }`". De kaart gaf mandaat
   voor één rij; de nieuwe rij legt de conjunctie daarom zelf uit.
   Voorgestelde tekst voor de regel, als de maintainer die wil: "`{ nx ∧ ny
   → do-not-attribute (false), nx ∧ nz → attribute (true) }` — each
   justification conjoins its carrier's `nx` (ch. 7.1)".
3. *Wat geen gate bewaakt.* Het conjunct `nx` in de broer `ny` en de
   volgorde met de drager voorop zijn onwaarneembaar (mutantmeting). Ze
   worden alleen bewaakt door de review van het beschermde pad en, straks,
   door de frontend-diff van de RegelSpraak-tweeling tegen dit anker. De
   tweelingkaart moet daarom uitdrukkelijk de platte `ALLE` met `nx` voorop
   in beide broers eisen. Een tweeling zonder het conjunct in `ny` haalt
   alle vier de cases.
4. *Voor de tweelingkaart.* Het anker draagt nu vier cases, en case4 heeft
   drie instanties. Een tweeling die `nx` erft, komt nu op dit anker uit.
   Een tweeling die het niet erft, valt op case4 (M en O).
