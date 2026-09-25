# KENMERKINVOER-EN-INVOER-BOVEN-BEREKENING — case input on a characteristic target, and a surface for "given, not derived"

*Status: PROPOSAL · 2026-09-25 · working prose, not normative. The
deliverable of the card
`backlog/doing/20260923-kenmerkinvoer-en-invoer-boven-berekening.md`, the
card of its own that decision 7 of `DIALECT-DEFAULTS-NIET-EN-NESTING.md`
§10 split off. This file lives outside every gate-1 zone; it names the
private regelspraak-java toolchain and the regelspraak-werkbank only where
the card does, and none of its text may be folded into a zoned document
without stripping those names. The RegelSpraak v2.3.0 specification
(© Belastingdienst) is cited by section number and paraphrased, never
quoted. Repository claims carry file:line at `main` 2f47d80e. The
measurements (mark [M]) were taken on 2026-09-25 on builds of that commit —
the verifier, ref, the clingo backend, both branches of the Drools backend
and the Catala frontend — with probe documents kept outside the repository
(§2.4); none of them is committed.*

## 0. How to read the marks

- **[Q]** a verbatim quote — in this file only of this repository's own
  spec and code;
- **[P]** a paraphrase of a source (RegelSpraak v2.3.0 is only ever
  paraphrased);
- **[I]** an inference of this proposal, stated in no source;
- **[M]** a measurement taken for this proposal (§2.4 gives the commands).

Wire names are the L5 names of `uchar-ir-v1` (`Default.decision`,
`PrioritizedDefaults.characteristic`, `Compound` with `quantifier: "all"`,
`source.snippet`). The ASP predicates (`heeft/2`, `inputval/3`) and the
OCaml and Java identifiers cited with file:line are the executors' internal
names, not wire names.

## 2. What is

### 2.1 The spec text

- [Q, own spec] Ch. 7.1, the characteristic target: "a justified default
  therefore *always* decides (there is no leeg boolean); a
  do-not-attribute decision displaces lower defaults and makes the group
  derive nothing, which is not a retraction (ch. 5.3's monotone write
  regime is untouched — the group decides once, then at most one
  attribution happens)" (`RUNTIME-SEMANTICS.md:4908-4912`). Further down
  the same section: "Input facts are **not** a default: they are the fact
  base the group's write meets under ch. 1.2g" (`:4926-4927`).
- [P, own spec] Ch. 7.8 restates case input on a defaults-carrying target
  as the **virtual input default**: an implicit default between the lowest
  ordinary default and the initialisation default, justified when the
  target is gevuld in the input facts, whose consequence is the input value
  (`RUNTIME-SEMANTICS.md:5160-5166`). The chapter calls it a re-description
  of ch. 7.1's "the target keeps what the fact base holds", from which every
  outcome of ch. 7.3 and ch. 7.6.2 follows by "the highest default that
  decides wins". This is Reading A — computation above input — decided on
  2026-08-20 (`PHASE4-DEFAULTS.md` §4.1 and §10 decision 1).
- [P, own spec] The attribute side is pinned by the anchor
  `jutterswet-vondstwaardering`: `case6-default-overschrijft` (a deciding
  default over an input 40 gives 50) and `case5-input-blijft` (nothing
  decides, the input 40 stands).
- [I] On a characteristic target the re-description does not settle the
  outcome in the false direction. Read literally, a deciding
  `decision: false` default sits above the virtual input default and
  displaces it; but displacement on a characteristic target only means that
  the displaced default attributes nothing, and an input characteristic is
  not an attribution: it is in the fact base before the group decides.
  Ch. 7.8 does not say which reading holds. Ch. 7.1 does, in the two
  sentences quoted above: a false decision retracts nothing, and the input
  is the fact base the group's write meets. Ch. 7.8's re-description is
  exact for attribute targets only.
- [P] RegelSpraak v2.3.0 §3.5: a characteristic is absent by default and
  becomes true when the input sets it or an applicable attribution rule
  exists; §9.2 has no negative attribution. The specification does not say
  what happens when input and a rule disagree; under §9.2 a rule can only
  add, so in the language itself the question cannot arise
  (`DIALECT-DEFAULTS-NIET-EN-NESTING.md` §3.1). The dialect's negative
  decision (form 1 of that proposal) raises it, and decision 1 of
  2026-09-23 kept RegelSpraak §3.5's "input or attribution": a negative
  decision never removes an input characteristic
  (`DIALECT-DEFAULTS-NIET-EN-NESTING.md` §10, item 1).

### 2.2 Per executor, in code

| Executor | Input characteristic | Input attribute on a defaults target | The group's write |
|---|---|---|---|
| ref | loaded into the instance before evaluation (`ref/lib/case.ml:443-477`); `World.set_char` only adds, and no function in `ref/lib/world.ml` removes a characteristic (`:120-125`: "characteristics are monotone within one evaluation (ch. 5.3): default false, only ever set true") | loaded into the instance before evaluation; a deciding default overwrites it through `World.write_attr` (`ref/lib/eval.ml:3182-3186`) | `Decided (Char_dec true)` sets the characteristic; `Decided (Char_dec false) -> (w, false)` writes nothing (`ref/lib/eval.ml:3179-3181`) |
| clingo | one bare fact `heeft(label,k).` per input characteristic, defaults target or not (`backends/clingo/lib/translate.ml:10296`) | `inputval/3` instead of `val/3` when the attribute is a defaults target (`:10292`; the test `defaults_target` at `:10206`), and the group adds `val(S,a,V) :- inputval(S,a,V), is_a(S,t), not grpN_eff(S)` (`:9598-9602`) | a characteristic group derives `heeft/2` from the decision `1` only (`:9583-9588`); the arm for a group whose members are all outside their validity window says so in its comment: "A characteristic target has no defeasible entry" (`:9496-9499`) |
| Drools exec | the bean's boolean field set to `true` before the session fires (`CaseRunner.java:532`) | the bean's field set before the session fires (`CaseRunner.java:514`); a deciding default overwrites it (`ExecModelTranspiler.java:7672`) | `if (isCharF) { if (b.charBesluit()) werkBij(drools, s, getterF, setterF, Boolean.TRUE, false); }` — only a true decision writes (`ExecModelTranspiler.java:7671`) |
| Drools DRL | — | — | every defaults document is refused before it lowers: `EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering` (`CaseRunner.java:307`) |

The Drools paths are under
`backends/drools/src/main/java/io/github/yhavinga/regelspraak/`
(`cases/CaseRunner.java`, `transpiler/model/ExecModelTranspiler.java`).

[I] The three executors that carry defaults reach the same outcome by the
same absence: none of them has a write that removes a characteristic. That
is ch. 5.3's monotone write regime, which ch. 7.1 keeps.

### 2.3 What the corpus pins

- `anchor/ww-herleving-inkomen/case7-invoer-staat-tegen-blokkade` (landed
  2026-09-23 with the repair of that anchor): case3's facts, so the
  blocking default of art. 21 lid 2 a decides `decision: false`, plus the
  target `recht op uitkering` in the case input; expected present and
  consistent. Its NOTE calls the expectation provisional until this card
  decides (`corpus/golden/anchor/ww-herleving-inkomen/NOTE.md:364-368`),
  and gate 7 lists the case as a reason-carrying survivor whose reason
  names the same pending choice (`scripts/check-noop.sh:157-167`).
- [M] It is the only case in the golden corpus whose input sets the target
  of a characteristic defaults group: eight goldens carry a characteristic
  defaults target, one case sets one in its input. `corpus/rood` and the
  `toka/` zone carry no defaults group at all; `corpus/negatief` carries
  nine documents with a characteristic defaults target and, by
  construction, no case files.
- `anchor/jutterswet-toelagegronden/case6-input-blijft` pins input standing
  under the conflict of a declared-unordered group, on an attribute target.

### 2.4 Measured

Builds and calls, from the repository root, as gates 3, 5, 6 and 12 make
them:

```
eval $(opam env)
dune build ref backends spec/verifier
dune build --root spec/verifier
mvn -o -B -q -f backends/drools/pom.xml package -DskipTests
spec/verifier/_build/default/bin/verifier.exe IR
_build/default/ref/bin/runner.exe IR CASE
_build/default/backends/clingo/bin/clingo_runner.exe IR CASE
java -jar backends/drools/target/uchar-drools-backend-0-SNAPSHOT-shaded.jar IR CASE
java -jar backends/drools/target/uchar-drools-backend-0-SNAPSHOT-shaded.jar --drl IR CASE
```

A runner reports PASS against the case's pinned expectations and has no
mode that prints the end state. Each claim below is therefore measured
twice: the case with the true expectation passes, and a copy with the
expectation inverted (a "flip", kept beside the probe) fails observably in
every executor that carries the document. A PASS without its failing flip
would not tell an executor that checks the expectation from one that does
not.

**The anchor.** [M]

| Case | verifier | ref | clingo | Drools exec | Drools DRL |
|---|---|---|---|---|---|
| `case7-invoer-staat-tegen-blokkade` (present) | `verifier: PASS` | `oracle: PASS (2 expectations)` | `clingo: PASS (2 expectations)` | `PLAYED 1` | `EXCLUDE drl: ch. 7 prioritizedDefaults — …` |
| its flip (absent) | — | `MISMATCH: W7 carries characteristic "recht op uitkering", expected absent` | the same line | `FAIL case7-omgekeerd.json: kenmerk recht op uitkering: verwacht afwezig, werkelijk aanwezig` | the same EXCLUDE |
| `case3-blokkade` (the same facts without the target in the input: absent) | — | `oracle: PASS (5 expectations)` | `clingo: PASS (5 expectations)` | `PLAYED 1` | the same EXCLUDE |

**Proof A: one form on both target kinds.** [M] An own-authored document
on the L5 wire, one object type `Jutter` with the input characteristics
`gemeld` and `geschorst` (adjectival), the target characteristic
`recht op de penning` (possessive) and the target attribute `penning`
(Number, 2 decimals). Two defaults groups of the same shape: a highest
default that blocks, justified by `geschorst`, above a base that grants,
justified by `gemeld`. On the characteristic the blocking default carries
`decision: false` and the base `decision: true`; on the attribute the
blocking default's consequence is `0.00` and the base's `10.00`. Verifier:
`PASS`. Every case expects `consistent` besides the values below.

| Case | Input characteristics | Input `penning` | `recht op de penning` | `penning` |
|---|---|---|---|---|
| `case1-blokkade-zonder-invoer` | gemeld, geschorst | — | absent | 0.00 |
| `case2-blokkade-tegen-invoer` | gemeld, geschorst, recht op de penning | 40.00 | **present** | **0.00** |
| `case3-toekenning-over-invoer` | gemeld, recht op de penning | 40.00 | present | 10.00 |
| `case4-niets-beslist` | recht op de penning | 40.00 | present | 40.00 |

All four pass in ref and clingo and play in the Drools exec branch
(`PLAYED 1` each); the DRL-text branch refuses each on its defaults
sentence. `case2` is the asymmetry in one case: the same blocking default
decides on both targets, the attribute takes its value 0.00 over the input
40.00, and the characteristic keeps its input. Its two flips fail in all
three carrying executors: characteristic inverted — ref and clingo
`MISMATCH: J carries characteristic "recht op de penning", expected
absent`, the exec branch `FAIL … kenmerk recht op de penning: verwacht
afwezig, werkelijk aanwezig`; attribute inverted to 40.00 — ref
`MISMATCH: J.penning = 0, expected 40`, clingo `MISMATCH: J.penning = [0],
expected 4000`, the exec branch `FAIL … attribuut penning: verwacht 40.00,
werkelijk 0`.

The ASP program clingo receives for `case2` (captured by a wrapper on
`PATH` that copies the program file before it calls clingo; the lines of
the two groups and the case facts):

```
nl1_dec0(S,0) :- is_a(S,"jutter"), heeft(S,"geschorst").
nl1_dec1(S,1) :- is_a(S,"jutter"), heeft(S,"gemeld").
nl1_out(S,V) :- nl1_dec0(S,V).
nl1_out(S,V) :- nl1_dec1(S,V), not nl1_stl0(S).
heeft(S,"recht op de penning") :- nl1_out(S,1), is_a(S,"jutter").
nl2_dec0(S,0) :- is_a(S,"jutter"), heeft(S,"geschorst").
nl2_dec1(S,1000) :- is_a(S,"jutter"), heeft(S,"gemeld").
nl2_out(S,V) :- nl2_dec0(S,V).
nl2_out(S,V) :- nl2_dec1(S,V), not nl2_stl0(S).
val(S,"penning",V) :- nl2_out(S,V), is_a(S,"jutter").
grp1_eff(S) :- nl2_out(S,_), is_a(S,"jutter").
val(S,"penning",V) :- inputval(S,"penning",V), is_a(S,"jutter"), not grp1_eff(S).
inputval("J","penning",4000).
heeft("J","gemeld").
heeft("J","geschorst").
heeft("J","recht op de penning").
```

The characteristic group has no rule that reads or blocks the input fact;
the attribute group has one (the last rule of the group). The anchor's
case7 program has the same two features: `heeft("W7","recht op
uitkering").` as a bare fact, and one write head for the group, from the
decision `1`.

**Proof A2: the conflict.** [M] Proof A's characteristic target as two
declared-unordered groups, one per default (`geschorst` → `decision:
false`, `gemeld` → `decision: true`), so that both grounds together are the
ch. 7.2 conflict. Verifier `PASS`; ref and clingo pass, the exec branch
plays (`PLAYED 1`) all three cases, the DRL-text branch refuses them:

| Case | Input characteristics | `recht op de penning` | Channel |
|---|---|---|---|
| `case1-conflict-zonder-invoer` | gemeld, geschorst | absent | inconsistent `Jutter.recht op de penning` |
| `case2-conflict-tegen-invoer` | gemeld, geschorst, recht op de penning | **present** | inconsistent `Jutter.recht op de penning` |
| `case3-onwaar-tegen-invoer` | geschorst, recht op de penning | **present** | consistent |

`case2`'s flip (absent) fails in all three carrying executors.

The probes live in `/tmp/kenmerkinvoer/` on the measuring machine
(`anker/`, `proef-a/`, `proef-a2/`, each with a `maak.py` that writes the
documents), outside the repository.

### 2.5 Reading B today

- *As case input:* nowhere. Case input is Reading A (§2.1).
- *As program structure:* the hardship pattern of `PHASE4-DEFAULTS.md`
  §4.1 — the discretionary decision enters the dossier as a fact of its
  own, and a highest default reads it. The Catala frontend generates that
  shape for a `context` variable that a calling scope overrides
  (`PHASE5-CATALA.md` decision 6, executed 2026-08-24; the golden
  `jutterswet-hardheidsaftrek-catala`, whose generated highest default
  carries `source.snippet` `context-override aftrek` and reads the caller
  over the synthesised back-link). In the RegelSpraak dialect an author
  can write the pattern by hand on an attribute target as the last-written
  first-level exception (`DIALECT-DEFAULTS-NIET-EN-NESTING.md` §6).
- [M] *A Catala `context` variable that no scope calls.* The Catala
  frontend accepts a `context` variable of a scope that no other scope of
  the program calls, and lowers its own default to the base of a defaults
  group, as it does for a called scope (`frontends/catala/lib/translate.ml`,
  `top_default_vars`, `:2336-2369`; the refusal the header comment at
  `:51-59` still describes no longer exists — the code at `:1297-1302`
  says so). The only overrider of such a scope in uchar is the case
  dossier, and the dossier gets Reading A. Measured on a probe scope
  `Kamer` (`input grondslag`, `context drempel` defined as 10,
  `uitkomst = grondslag - drempel`): with a case that supplies grondslag
  50 and drempel 7, ref and clingo both pass on uitkomst **40** — the own
  default displaces the supplied 7. `clerk run --scope Test` on the same
  scope called by a scope `Test` that overrides drempel with 7 gives
  `uitkomst = 43`. No corpus case supplies such a variable: the one
  corpus source with a `context` variable
  (`jutterswet-hardheidsaftrek-catala`) overrides it from a calling scope.
  §5.10 returns to this.
