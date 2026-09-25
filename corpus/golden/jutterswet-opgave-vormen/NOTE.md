# jutterswet-opgave-vormen — three authored forms under the opgave marker

Exercises the part of the RegelSpraak dialect's **opgave marker**
(`frontends/regelspraak/grammar/DIVERGENCES.md` §7) that moves authored
writers: every computing writer of a marked attribute X keeps its name and its
shape and targets `berekende X`, and the marker puts one defaults group on X
with the opgave above the computation. `jutterswet-opgave-boven-berekening`
pins that for plain rules and an initialisation; this golden pins it for the
three forms that are no plain rule — a defaults tree of `Uitzondering op`
blocks with a nested exception, a declared-unordered `Uitzonderingen
ongeordend:` block, and the rows of a decision table. Own
Jutterswet-flavoured casus, own numbers, rights-free.

## Casus (own text)

> **Art. 3e (opgaven bij vondst, wacht en aanspoelsel).** 1. De waarde van
> een aangemelde vondst is de taxatie van de strandvoogd, verminderd met vijf
> euro. 2. Is de taxatie lager dan tien euro, dan is de waarde één euro.
> 3. Is zo'n vondst museaal, dan is de waarde honderd euro. 4. De premie van
> een vondst is tweemaal de waarde. 5. Een jutter die de kustwacht houdt,
> krijgt dertig wachtgeld, een havenmeester vijfenveertig; wie beide taken
> houdt, krijgt geen berekend wachtgeld, en de tegenstrijdigheid wordt
> aangetekend. 6. De stukprijs van aangespoeld zeewier is 2,50 euro, die van
> wrakhout 7,00 euro. 7. De jutter kan de waarde, het wachtgeld en de
> stukprijs opgeven; een opgegeven bedrag gaat voor het berekende.

Amounts are computed without a unit, because the marker stands on a number
without a unit only (DIVERGENCES.md §7).

## Source (`bron.rgs`)

- `waarde` of a Vondst, `wachtgeld` of a Jutter and `stukprijs` of an
  Aanspoelsel carry the marker (lid 7). The three names differ: the form names
  the marker generates carry no type name (DIVERGENCES.md §7).
- **The tree** (lid 1–3): the base rule **waardering** (`taxatie` verminderd
  met 5,00), the exception **lage taxatie** on it (1,00 indien de taxatie
  kleiner is dan 10,00), and the exception **museale vondst** on lage taxatie
  (100,00 indien hij museaal is) — a second level, so the tree nests.
- **premie** (lid 4): `premie` = `waarde` maal 2, a reader of the marked
  attribute.
- **The unordered block** (lid 5): `Uitzonderingen ongeordend:` with
  **wachtgeld voor kustwachters** (30 indien hij kustwacht is) and
  **wachtgeld voor havenmeesters** (45 indien hij havenmeester is).
- **The decision table** (lid 6): **stukprijs van een aanspoelsel**, row 1
  2,50 for soort "zeewier", row 2 7,00 for soort "wrakhout".

## Lowering (the frontend's)

| Authored form | On the wire |
|---|---|
| the tree on `waarde` | one `PrioritizedDefaults` `waardering` on `berekende waarde`, validity always, chain from the top: lage taxatie, whose `exceptions` hold one ordered `ExceptionGroup` (source `Uitzondering op lage taxatie`) with museale vondst, justified by the `all` of the carrier's comparison and its own characteristic check; then waardering under the empty `all`; the statute annotations `art. 3e lid 1`–`3` on the three defaults |
| the unordered block on `wachtgeld` | two `PrioritizedDefaults`, `wachtgeld voor kustwachters` and `wachtgeld voor havenmeesters`, both on `berekende wachtgeld` and both `unordered: true` — the set moved as a set |
| the decision table on `stukprijs` | the two row rules `stukprijs van een aanspoelsel 1` and `2`, each on `berekende stukprijs`, conditions and statute annotation `art. 3e lid 6` kept |
| per marked attribute X | the attributes `opgegeven X` and `berekende X` right after X; the group `opgave boven berekening X` on X (the opgave default above the computation default); the possessive characteristic `afwijkende opgave X` with its rule `afwijking opgave X`; the consistency `controle opgave X` |

`premie` keeps reading `waarde`: a reader of X reads the resolved value, the
opgave where the opgave decides. No marked attribute here has an
initialisation.

## Case derivations (hand-computed, absolute)

Per marked target the opgave decides when it is filled, else `berekende X`
when it is filled; a default whose consequence is empty falls through
(ch. 7.1), and when neither decides the target keeps what the input holds
(ch. 7.8, Reading A). The characteristic needs the opgave and the computation
both filled and unequal; the consistency deposits one fact `controle opgave X`
× the subject exactly when it holds.

**The tree** (`berekende waarde` from the chain; a nested exception applies
only where its carrier's condition holds too, since its justification carries
the carrier's conjuncts; `premie` = `waarde` × 2):

| Case | Input | berekende waarde | waarde | premie | afwijkende opgave waarde | channel |
|---|---|---|---|---|---|---|
| case1-boom-basis | taxatie 40.00 | 40.00 − 5.00 = **35.00** (base) | **35.00** | 70.00 | absent | consistent |
| case2-boom-uitzondering | taxatie 8.00 | **1.00** (lage taxatie) | **1.00** | 2.00 | — | consistent |
| case3-boom-geneste-uitzondering | taxatie 8.00, museaal | **100.00** (museale vondst above its carrier) | **100.00** | 200.00 | — | consistent |
| case4-boom-geneste-uitzondering-buiten-haar-drager | taxatie 40.00, museaal | **35.00** (40.00 is no low taxatie, so the nested exception does not hold) | **35.00** | — | — | consistent |
| case5-boom-opgave-wijkt-af | taxatie 40.00, opgegeven waarde 50.00 | 35.00 | **50.00** (the opgave) | 100.00 | **present** | `controle opgave waarde` × V |
| case6-boom-opgave-gelijk-aan-uitzondering | taxatie 8.00, opgegeven waarde 1.00 | 1.00 | **1.00** | 2.00 | absent (equal) | consistent |

**The unordered block** (the two groups on `berekende wachtgeld`; where both
decide and differ they do not write, and one §9.5 fact records the conflict
under the group's target form, ch. 7.2 — here the moved target):

| Case | Input | berekende wachtgeld | wachtgeld | afwijkende opgave wachtgeld | channel |
|---|---|---|---|---|---|
| case7-blok-een-lid | kustwacht | **30** | **30** | absent | consistent |
| case8-blok-opgave-wijkt-af | kustwacht, opgegeven wachtgeld 50 | 30 | **50** | **present** | `controle opgave wachtgeld` × J |
| case9-blok-conflict | kustwacht, havenmeester | **empty** (30 against 45) | **empty** (nothing decides) | absent | `Jutter.berekende wachtgeld` × J |
| case10-blok-conflict-met-opgave | kustwacht, havenmeester, opgegeven wachtgeld 40 | **empty** | **40** (the opgave) | absent (no computation) | `Jutter.berekende wachtgeld` × J |
| case11-blok-doel-verdrongen | havenmeester, wachtgeld 10 | **45** | **45** (the group decides; the input 10 is displaced) | — | consistent |

**The decision table** (the row rules on `berekende stukprijs`):

| Case | Input | berekende stukprijs | stukprijs | afwijkende opgave stukprijs | channel |
|---|---|---|---|---|---|
| case12-tabel-rij | soort "zeewier" | **2.50** (row 1) | **2.50** | — | consistent |
| case13-tabel-opgave-gelijk | soort "wrakhout", opgegeven stukprijs 7.00 | 7.00 (row 2) | **7.00** | absent (equal) | consistent |
| case14-tabel-opgave-wijkt-af | soort "wrakhout", opgegeven stukprijs 8.00 | 7.00 | **8.00** (the opgave) | **present** | `controle opgave stukprijs` × A |
| case15-tabel-opgave-zonder-rij | soort "barnsteen", opgegeven stukprijs 3.00 | **empty** (no row applies) | **3.00** | absent (no computation) | consistent |

## Executors

Each line below is the executor's own output on this golden.

- ref, per case: `oracle: PASS (5 expectations)` for case1 and the like —
  all fifteen pass.
- clingo, per case: `clingo: PASS (5 expectations)` for case1 and the like —
  all fifteen pass.
- The Drools exec branch over the fifteen cases: `PLAYED 15`.
- The DRL-text branch: `EXCLUDE drl: ch. 7 prioritizedDefaults — the
  DRL-text branch has no defaults lowering`, as for every defaults document
  (class (A) of `DRL_CHANNEL` in `scripts/check-drools.sh`).

Sixteen flipped counter-cases, each one pinned expectation of a case turned,
kept outside the repository, fail in all three executors. Among them:

- case2 with `berekende waarde` 3.00 (the base above the exception): ref
  `MISMATCH: V.berekende waarde = 1, expected 3`, clingo `MISMATCH:
  V.berekende waarde = [100], expected 300`, the exec branch `FAIL …:
  attribuut berekende waarde: verwacht 3.00, werkelijk 1`;
- case4 with `waarde` 100.00 (the nested exception outside its carrier): ref
  `MISMATCH: V.waarde = 35, expected 100`, clingo `MISMATCH: V.waarde =
  [3500], expected 10000`, the exec branch `FAIL …: attribuut waarde:
  verwacht 100.00, werkelijk 35.00`;
- case9 with the conflict under `Jutter.wachtgeld` (the unmoved target): ref
  `MISMATCH: expected inconsistent (a channel fact from rule
  "Jutter.wachtgeld" on "J"), but the channel carries: Jutter.berekende
  wachtgeld × J`, clingo `MISMATCH: expected inconsistent (an inconsistent
  fact from rule "Jutter.wachtgeld" on "J"), but the model carries:
  jutter.berekende wachtgeld × J`, the exec branch `FAIL …: verwacht
  inconsistent (Jutter.wachtgeld @ J), niet gevonden onder Jutter.berekende
  wachtgeld @ J`;
- case14 with `berekende stukprijs` 8.00 (the opgave copied into the
  computation): ref `MISMATCH: A.berekende stukprijs = 7, expected 8`, clingo
  `MISMATCH: A.berekende stukprijs = [700], expected 800`, the exec branch
  `FAIL …: attribuut berekende stukprijs: verwacht 8.00, werkelijk 7`.

The other twelve turn case1 `waarde` to the taxatie 40.00, case3 `waarde` to
the carrier's 1.00, case5 `waarde` to 35.00 and case5 to consistent, case6 to
inconsistent, case7 `wachtgeld` to empty, case8 to consistent, case10
`wachtgeld` to empty, case11 `wachtgeld` to the input 10, case12 `stukprijs`
to empty, case13 to inconsistent and case15 `stukprijs` to empty.

Under the no-op guard (gate 7) no case survives: each pins a computed value
or a channel fact that the input does not hold.
