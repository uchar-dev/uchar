# KENMERKINVOER-EN-INVOER-BOVEN-BEREKENING — case input on a characteristic target, and a surface for "given, not derived"

*Status: PROPOSAL · 2026-09-25 · working prose, not normative. The
deliverable of the card
`backlog/done/20260923-kenmerkinvoer-en-invoer-boven-berekening.md`, the
card of its own that decision 7 of `DIALECT-DEFAULTS-NIET-EN-NESTING.md`
§10 split off. It records what the executors do with case input on a
defaults target of either kind (§2, measured), weighs the card's three
options (§3), records the main session's decisions of 2026-09-25 with
their reasons and the rejected alternatives (§4), proposes the spelling,
lowering, explanation and contradiction handling of the marker of option
(7-iii) (§5), and gives the build card for that marker as text (§6). On
this branch it proposes one sentence for `RUNTIME-SEMANTICS.md` ch. 7.8, a
protected path (§4.1). Nothing else changes: no wire, schema, verifier,
executor, frontend, gate script or corpus file. This file lives outside
every gate-1 zone; it names the private regelspraak-java toolchain and the
regelspraak-werkbank only where the card does, and none of its text may be
folded into a zoned document without stripping those names. The RegelSpraak v2.3.0 specification
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

## 1. The answer in brief

1. **What is [M].** Case input on a characteristic that is the target of a
   defaults group stands against every decision of the group: in ref,
   clingo and the Drools exec branch a deciding `decision: false` default
   leaves an input characteristic present, and so does a conflict. Case
   input on an attribute target is replaced by a deciding default. The
   asymmetry follows from two texts the repository already has:
   RUNTIME-SEMANTICS ch. 7.1 (a do-not-attribute decision derives nothing,
   "which is not a retraction"; input facts are "the fact base the group's
   write meets") and RegelSpraak §3.5 (a characteristic is true when the
   input sets it or a rule attributes it). Ch. 7.8's virtual input default
   describes the attribute side exactly; for the characteristic side it
   says nothing. The anchor case
   `ww-herleving-inkomen/case7-invoer-staat-tegen-blokkade` pins the
   behaviour. The Drools DRL-text branch refuses every defaults document,
   this one included (§2).
2. **(7-i), decided: record what is.** One sentence in ch. 7.8, proposed on
   this branch, with case7 as its worked example (§4.1). No code, no wire
   change, no `CONSTRUCTS.md` delta: the sentence records landed behaviour.
3. **(7-ii), decided against.** Characteristic input stays
   non-defeasible. §4.2 records the option as the rejected alternative,
   with its cost: a tracked ch. 7.8 delta, three executors, case7's
   expectation turned to absent, and decision 1 of 2026-09-23 reversed.
4. **(7-iii), proposed, not built.** A marker `(mag opgegeven worden)` on
   an attribute declaration in the RegelSpraak dialect, lowered by the
   frontend to structure the wire already carries: an input attribute
   `opgegeven <naam>`, the authored writers moved to `berekende <naam>`, a
   two-default group on `<naam>` with the opgave above the computation,
   and a consistency that deposits one §9.5 fact when opgave and
   computation are both filled and differ (§5). [M] That lowering, written
   by hand on the L5 wire, replays in ref, clingo and the Drools exec
   branch on thirteen cases (the lowering and initialisation proofs of
   §5.4), each with a failing flip.
   It changes neither the wire nor ch. 7.8. The semantic variant — an
   input default above the ordinary defaults, per target, in ch. 7.8 — is
   the recorded alternative (§5.9).
5. **The build card for the marker** is §6, as text; the main session
   places it, and whether it lands before or after the pin-bump of
   regelspraak-java is the maintainer's call.

| Option (card) | Decided by the main session, 2026-09-25 |
|---|---|
| (7-i) record what is | yes: one sentence in ch. 7.8, a proposal on this branch (protected path) |
| (7-ii) characteristic input defeasible | no: kept non-defeasible; recorded as the rejected alternative |
| (7-iii) a surface for Reading B | a proposal without a build: sugar over the hardship pattern, the marker of §5, the build card of §6; the semantic variant recorded as the alternative |

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
mode that prints the end state. The case that carries a claim is therefore
measured twice: with the true expectation it passes, and a copy with the
expectation inverted (a "flip", kept beside the probe) fails observably in
every executor that carries the document. A PASS alone would not tell an
executor that checks the expectation from one that does not.

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

No rule of the characteristic group mentions the input fact; the last
rule of the attribute group passes the input value through only where the
group writes nothing. The anchor's case7 program has the same two
features: `heeft("W7","recht op uitkering").` as a bare fact, and one
write head for the group, from the decision `1`.

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
  `top_default_vars`, `:2345-2378`; the code at `:1306-1311` and the
  header comment at `:34-36` and `:54-68` say so). The only overrider of
  such a scope in uchar is the case dossier, and the dossier gets Reading
  A. Measured on a probe scope
  `Kamer` (`input grondslag`, `context drempel` defined as 10,
  `uitkomst = grondslag - drempel`; the frontend's IR is verifier-clean):
  with a case that supplies grondslag 50 and drempel 7, ref, clingo and the
  Drools exec branch all answer uitkomst **40** — the own default
  displaces the supplied 7; the flip expecting 43 fails in all three (ref
  `MISMATCH: K.uitkomst = 40, expected 43`). `clerk run --scope Test` on
  the same scope called by a scope `Test` that overrides drempel with 7
  gives `uitkomst = 43`. No corpus case supplies such a variable: the one
  corpus source with a `context` variable
  (`jutterswet-hardheidsaftrek-catala`) overrides it from a calling scope.
  §5.10 returns to this.

## 3. The three options

The card states three options; (7-ii) and (7-iii) are independent of each
other, and (7-i) is the minimal record either way.

### 3.1 (7-i) Record what is

Ch. 7.8 gets one sentence: on a characteristic target no decision removes
what the virtual input default holds, because a do-not-attribute decision
writes nothing; on an attribute target a deciding default replaces the
input value. [M] The three executors that carry defaults already behave
so (§2.4), and case7 pins it. Cost: the sentence.

### 3.2 (7-ii) Characteristic input made defeasible

Reading A for characteristics too: an input characteristic on a defaults
target stands only where the group decides nothing; a deciding
`decision: false` removes it. What it would take, sketched on the code of
§2.2 [I]:

- *clingo:* the case emits the input characteristic of a defaults target as
  an input atom of its own instead of the bare `heeft/2` fact, and the
  group derives `heeft(S,k)` from it only where the group reaches no
  decision — the card's `heeft(S,k) :- inputchar(S,k), not grpG_eff(S)`,
  with an effectiveness atom that holds where the group decides, either
  way, without a conflict (the attribute arm has its counterpart,
  `backends/clingo/lib/translate.ml:9589-9602`); a conflict writes
  nothing, so the input would stand under it, as ch. 7.2 wants.
- *ref:* input characteristics of defaults targets kept out of the instance
  at load and added after the group when it decides nothing; ch. 5.3's
  monotone regime holds for writes but no longer for the input, which
  becomes a pending value.
- *Drools exec:* the bean field of such a characteristic left unset at load
  and set by the group's consequence when the input had it and the group
  decided nothing.
- *Spec:* a tracked delta in ch. 7.8 and in `CONSTRUCTS.md` (the defaults
  record, row 8); ch. 7.1's "input facts are the fact base the group's
  write meets" no longer holds for characteristics. The case format stays.
- *Corpus:* case7's expectation turns to absent; the case leaves gate 7's
  survivor list, since a do-nothing run would then fail it; the anchor's
  NOTE changes. [M] No other golden case moves (§2.3).
- *Dialect documentation:* "What a `false` decision says"
  (`frontends/regelspraak/grammar/DIVERGENCES.md` §4) and the documentation
  sentence of `DIALECT-DEFAULTS-NIET-EN-NESTING.md` §3.5 invert, and
  decision 1 of 2026-09-23 ("a negative decision never removes an input
  characteristic") is reversed.

The gain is symmetry: Reading A on both target kinds.

### 3.3 (7-iii) A surface for Reading B

A marker on the target ("mag opgegeven worden") that makes case input a
highest default, as the Catala frontend does for `context` through a
subscope call; an explanation channel ("opgegeven, niet afgeleid"); a
choice for facts and opgave that contradict each other (a §9.5 fact, or the
opgave silently); per attribute or per group. Two readings: as **sugar**,
the frontend generates structure the wire already carries and ch. 7.8
stays (§5.1–5.8); as **semantics**, a per-target marking on the wire moves
the input default above the ordinary defaults (§5.9).

### 3.4 Interactions

| Option | RUNTIME-SEMANTICS ch. 7.8 | ch. 7.1 | RegelSpraak §3.5 | PHASE5-CATALA decision 6 |
|---|---|---|---|---|
| (7-i) | one sentence; the re-description of the input default is then stated for both target kinds | unchanged; the sentence restates "not a retraction" for input | kept: input or attribution | untouched |
| (7-ii) | a tracked delta: the input default displaced on characteristic targets too | its clause on input facts no longer holds for characteristics | given up inside the dialect extension: an input characteristic can end absent, which no flat RegelSpraak document can produce | untouched |
| (7-iii) as sugar | unchanged; input on the target itself keeps Reading A ([M], §5.4, cases 6 and 7) | unchanged | not touched: §3.5 concerns characteristics; for an attribute the specification does not say whether input or computation wins ([P], the research report §6.3; only the §9.6 initialisation is layered), and ch. 7.8 decided Reading A | followed: Reading B as default order, the input default reserved for the dossier |
| (7-iii) as semantics | a delta per target | unchanged | not touched | crossed: Reading B would sit in the input default, which decision 6 and `PHASE4-DEFAULTS.md` §2.2 keep apart |

## 4. Decisions

The main session took the card's three questions on 2026-09-25, with the
defaults it reported to the maintainer; the card records them. The
maintainer can overturn each; the alternatives stand under each decision.

### 4.1 (7-i): yes — one sentence in ch. 7.8

The sentence, proposed on this branch in `spec/RUNTIME-SEMANTICS.md` (a
protected path), in the first paragraph of ch. 7.8, after "Like the
initialisation default's, its justification reads the input world and
contributes no dependency edges.":

> On a characteristic target no decision removes what the virtual input
> default holds: a do-not-attribute decision displaces the lower defaults
> and derives nothing, which is not a retraction (ch. 7.1), so a
> characteristic the case input sets stays set — the input is a source of
> the characteristic beside the attribution (§3.5) — whereas on an
> attribute target a deciding default replaces the input value
> (ch. 7.6.2); worked, absolute, through all three executors (gates 5, 6
> and 12): the anchor `ww-herleving-inkomen/case7-invoer-staat-tegen-blokkade`,
> whose blocking default decides do-not-attribute over an input
> `recht op uitkering`, leaves that characteristic **present**.

`§3.5` is a RegelSpraak section number, as every `§` in
RUNTIME-SEMANTICS is (its header). The placement keeps the next sentence
of the paragraph ("This is a re-description of ch. 7.1's …") true: it now
follows a statement that covers both target kinds.

*Checked against the measurement, clause by clause [M]:*

| Clause | Measured by |
|---|---|
| no decision removes what the input default holds | a deciding `decision: false`: case7, proof A `case2`, proof A2 `case3`; a deciding `decision: true`: proof A `case3`; a conflict, which is no decision and removes nothing either: proof A2 `case2` |
| a do-not-attribute decision displaces the lower defaults and derives nothing | proof A `case1`: without input the blocking default decides and the granting base below it does not attribute (absent) |
| a characteristic the case input sets stays set | every case above with the target in the input: present |
| on an attribute target a deciding default replaces the input value | proof A `case2` (0.00 over 40.00) and `case3` (10.00 over 40.00); `jutterswet-vondstwaardering/case6-default-overschrijft` in the corpus |
| case7 through all three executors | ref and clingo pass it, the Drools exec branch plays it (§2.4); the DRL-text branch, which executes no defaults document, refuses it on its defaults sentence |

*Reasons.* The three executors agree (§2.4); the sentence says what ch. 7.1
and RegelSpraak §3.5 already imply (§2.1) and what decision 1 of
2026-09-23 kept; case7 pins it through gates 5, 6 and 12; and it removes
the one place where ch. 7.8's re-description reads as if it decided the
characteristic outcome, which it does not (§2.1, the inference).

*What it does not change.* No code, no wire, no WF rule. No
`CONSTRUCTS.md` delta: the behaviour is landed, the sentence records it.
No entry in `spec/proposals/v1-naamtabel.json`: the sentence names no wire
element, and the toets reads the schema and IR-SPEC only. Two protected
files outside this card's reach still describe case7's expectation as
pending on this card: the anchor's NOTE and gate 7's survivor reason
(§7, points 1 and 2).

### 4.2 (7-ii): no — characteristic input stays non-defeasible

*Reasons.* (a) Decision 1 of 2026-09-23 kept RegelSpraak §3.5's "input or
attribution": a negative decision never removes an input characteristic. A
flat RegelSpraak document can never end with an input characteristic
absent, and the dialect extension should not introduce one that does [I].
(b) A `false` decision is a non-derivation: that is its literal content
(`DIALECT-DEFAULTS-NIET-EN-NESTING.md` §3.5,
`frontends/regelspraak/grammar/DIVERGENCES.md` §4, "What a `false`
decision says"). Letting it remove input would make it a write of absence,
the write ch. 5.3 and ch. 7.1 exclude. (c) Where an author wants an
opgave that also decides "not present", §5.8 sketches it as sugar over a
three-valued opgave attribute, without touching the input of the
characteristic itself. (d) The cost of §3.2.

*The rejected alternative, recorded:* (7-ii) as §3.2 describes it, with its
cost. Should it be taken later, the order is spec first (the ch. 7.8 delta
and its `CONSTRUCTS.md` row), then the three executors in one card with
case7's expectation turned in the same commit.

### 4.3 (7-iii): a proposal, no build

Sugar over the hardship pattern (`PHASE4-DEFAULTS.md` §4.1), in the shape
the Catala frontend's `context` lowering already has (`PHASE5-CATALA.md`
decision 6): §5 gives the spelling, the lowering, the explanation and the
handling of contradicting facts; §6 gives the build card as text.

*Reasons.* (a) The wire carries the lowering today: [M] the lowering and
initialisation proofs replay in ref, clingo and the Drools exec branch
(§5.4). (b) Reading B stays program structure and case input stays Reading
A, which `DIALECT-DEFAULTS-NIET-EN-NESTING.md` §6 said the form-3 card
should not reopen. (c) The case says by the attribute it fills whether a
value is a fact (`<naam>`) or an opgave (`opgegeven <naam>`). (d) The
materialised computation gives an explanation and a contradiction check
something to read, which a displaced default never leaves behind (ch. 7.1:
default evaluation is speculative).

*One refinement of the main session's sketch.* The sketch placed the
contradiction in a consistency over the materialised value. This proposal
keeps that and gives the consistency's guard ("both filled and different")
a generated characteristic of its own, because [M] the one-assertion form
— a consistency whose assertion is an `atLeast` 1 compound — is outside the
Drools exec branch's consistency lowering today (§5.6).

*The recorded alternative:* the semantic variant of §5.9.

## 5. The marker for (7-iii)

### 5.1 Spelling

The marker is a property of the attribute, so it sits on the attribute's
declaration — not on a rule: the case supplies attributes, and the
werkbank's input form lists them. Proposed:

```
Objecttype de Vondst
    de taxatie Numeriek (getal met 2 decimalen);
    de waarde Numeriek (getal met 2 decimalen) (mag opgegeven worden);
```

One new lexer token, `MAG_OPGEGEVEN_WORDEN : '(mag' WSP 'opgegeven' WSP
'worden)'`, a whole token with its parentheses, like the declaration
markers the grammar has (`BEZIELD : '(bezield)'`, `BEZITTELIJK`,
`BIJVOEGLIJK`, `frontends/regelspraak/grammar/RegelSpraakLexer.g4:53-55`).
Its place is the last element of `attribuutSpecificatie`, after the
optional timeline and before the optional `;`
(`frontends/regelspraak/grammar/RegelSpraak.g4:368-373`), so every clause
the production has keeps its place. The grammar also admits it at the end
of `kenmerkSpecificatie`, so that the resolver can refuse it there with a
message of its own instead of leaving a syntax error (§5.7).

*Why these words.* The card's own phrase; the generated input attribute
(`opgegeven <naam>`) and the explanation ("opgegeven, niet afgeleid") use
the same verb; and it states what the marker grants — the case may give
the value; without an opgave the computation decides.

*Alternatives considered.* `(opgaaf)` or `(opgave)` — shorter, but a noun
that does not say what is allowed; `(opgeefbaar)` — one adjective like
`(bezield)`, but a rare word; `(invoer boven berekening)` — names the
semantics rather than the permission; a clause without parentheses
(`, mag opgegeven worden;`) — `mag`, `opgegeven` and `worden` lex as
ordinary name words today (only multi-word tokens contain `worden`), so the
clause needs either three reserved words or a whole-phrase token without
the parenthesis that bounds it.

*Per attribute or per group* (the card's question). Per attribute: a
defaults group is per target anyway, and the marker's lowering builds the
target's one group itself (§5.3). The hand-written form of the pattern — a
last-written `Uitzondering op` reading an opgave attribute
(`DIALECT-DEFAULTS-NIET-EN-NESTING.md` §6) — stays available and is what
the marker spares the author.

### 5.2 Inertness

The question the build card answers by measurement
(`frontends/regelspraak/grammar/DIVERGENCES.md` §6, "Inertness —
measured, not assumed"): does any flat v2.3.0 document lex differently
with the token? Under maximal munch the token takes the exact character
sequence `(mag opgegeven worden)` (any whitespace between the words)
wherever the lexer is in its default mode. A line comment is one token
(`LINE_COMMENT`, `frontends/regelspraak/grammar/RegelSpraakLexer.g4:466`),
an enumeration literal is one token (`:245`), and the inside of a text
literal is lexed in a mode of its own (`:244`, `:473-476`), so the sequence
inside any of them is untouched.

- [M] First count, 2026-09-25: the sequence, and its opening `(mag`, occur
  in none of the 120 `.rgs` files in the repository (73 `bron.rgs`
  goldens, 47 in `toka/`) and in none of the six markdown documents of the
  v2.3.0 specification set. The words `opgegeven` (17 lines) and
  `berekende` (20 lines) occur in the specification set only in its
  descriptive prose and tables, never as a declared name or in a rule
  example; `opgaaf` occurs nowhere.
- [I] The failure mode of a collision is loud: the token is admitted only
  at the end of a member declaration, so anywhere else it is a syntax
  error on its own line, never a different reading.
- The build card repeats the sweep over every flat RegelSpraak document the
  werkbank can reach (876 documents in seven collections at the last
  count, `DIVERGENCES.md` §6), emitted by the frontend before and after the
  token, IR and diagnostics compared.

The token reserves no name word: `mag`, `opgegeven` and `worden` keep
lexing as before everywhere else. The generated names (§5.3) are the other
half of the question — a model that already declares `opgegeven <naam>`,
`berekende <naam>` or `afwijkende opgave <naam>` — and are refused by name
where they collide (§5.7).

### 5.3 What the frontend lowers

For a marked attribute X of object type T, in L5 wire terms:

1. **The opgave attribute.** `opgegeven X` on T, with X's datatype. No rule
   writes it; the case supplies it.
2. **The computation attribute.** `berekende X` on T, with X's datatype.
   Every computing writer the author wrote for X gets `berekende X` as its
   target: a plain `Rule` (gelijkstelling, berekening), a decision-table
   row, and the `PrioritizedDefaults` groups the dialect's `Uitzondering op`
   and `Uitzonderingen ongeordend` blocks lower to. A writer keeps its name
   and its shape; a declared-unordered set moves as a set, all members
   still `unordered` on their shared new target (WF-050).
3. **The group on X.** One `PrioritizedDefaults` with `subject` T and
   `attribute` X, named `opgave boven berekening X`, with, from the top:
   - the opgave default — `justification` the vacuous `Compound`
     (`quantifier: "all"`, no `parts`, `source.snippet` "altijd", as the
     anchors spell it), `consequence` an `AttributeReference` to
     `opgegeven X`, `source.snippet` `opgegeven X`;
   - the computation default — the same vacuous justification,
     `consequence` an `AttributeReference` to `berekende X`,
     `source.snippet` `berekende X`;
   - where the author wrote an initialisation for X: that initialisation as
     the group's initialisation default (`initialisation: true`, its value
     as `consequence`), the lowest (ch. 7.3). It stays on X: its
     justification is "X is empty in the input facts", which on
     `berekende X` — never in the input — would always hold (§5.4, the
     initialisation proof).

   No gevuld test is written: a default whose consequence evaluates leeg
   falls through (ch. 7.1). Without an opgave the computation decides;
   without either, the initialisation; without that, X keeps what the fact
   base holds — its own input value under Reading A, unchanged.
4. **The deviation characteristic.** `afwijkende opgave X` on T
   (possessive: "heeft afwijkende opgave X"), attributed by a rule
   `afwijking opgave X` under the condition `Compound` `all` of three
   parts: `Predicate` `filled` on `opgegeven X`, `Predicate` `filled` on
   `berekende X`, and `Comparison` `notEqual` of the two.
5. **The consistency.** `controle opgave X`, whose assertion is the
   `CharacteristicCheck` of `afwijkende opgave X` with `negated: true`.

Reads of X elsewhere in the document stay reads of X: they read the
resolved value. Per marked attribute the document gains two attributes,
one characteristic and three named forms (the group, the attribution rule,
the consistency); nothing on the wire is new.

### 5.4 Measured on the lowered wire

[M] The lowering of §5.3 written by hand on the L5 wire for the example of
§5.1 with one computing rule, `waardering`: "De waarde van een Vondst moet
berekend worden als de taxatie van de Vondst verminderd met 5,00", now
targeting `berekende waarde`. The probes differ from §5.3 in names only
where noted: the group is named `waarde`, the deviation characteristic is
adjectival (the kind steers spelling, not execution — the WW anchor's
NOTE measured the same for its target). Verifier `PASS` on each document.

**The lowering proof** (the lowering of §5.3). Pinned expectations per case:

| Case | Input | Pinned |
|---|---|---|
| `case1-geen-opgave` | taxatie 40.00 | waarde 35.00; consistent |
| `case2-opgave-gelijk` | taxatie 40.00, opgegeven waarde 35.00 | waarde 35.00; consistent |
| `case3-opgave-wijkt-af` | taxatie 40.00, opgegeven waarde 50.00 | waarde **50.00**; berekende waarde 35.00; inconsistent `controle opgave waarde` |
| `case4-opgave-zonder-feiten` | opgegeven waarde 50.00 | waarde 50.00; berekende waarde leeg; consistent |
| `case5-niets` | — | waarde leeg; consistent |
| `case6-doel-zelf-in-invoer` | taxatie 40.00, waarde 60.00 | waarde 35.00 (Reading A on X itself); consistent |
| `case7-doel-zelf-zonder-feiten` | waarde 60.00 | waarde 60.00 (the input stands); consistent |
| `case8-afwijking-leesbaar` | taxatie 40.00, opgegeven waarde 50.00 | `afwijkende opgave waarde` present; inconsistent `controle opgave waarde` |

All eight pass in ref and clingo and play in the Drools exec branch
(`PLAYED 1` each); the DRL-text branch refuses each on its defaults
sentence. Three flips fail in all three executors: case3 expected
consistent — ref `MISMATCH: expected consistent, but the channel carries:
controle opgave waarde × V`, clingo `MISMATCH: expected consistent, but the
model carries 1 inconsistent fact(s)`, the exec branch `FAIL …: verwacht
consistent, werkelijk inconsistent (controle opgave waarde @ V)`; case3
expected waarde 35.00 — ref `MISMATCH: V.waarde = 50, expected 35`, clingo
`MISMATCH: V.waarde = [5000], expected 3500`, the exec branch `FAIL …:
attribuut waarde: verwacht 35.00, werkelijk 50.00`; case4 expected
inconsistent — ref `MISMATCH: expected inconsistent (a channel fact from
rule "controle opgave waarde"), but the channel carries: (empty)`, clingo
the same in its words, the exec branch `FAIL …: verwacht inconsistent
(controle opgave waarde), werkelijk consistent`.

**The initialisation proof** (the lowering proof plus an authored
initialisation of 25,00 on `waarde`, kept on `waarde` as the group's
initialisation default):

| Case | Input | Pinned |
|---|---|---|
| `case1-niets-init` | — | waarde 25.00; berekende waarde leeg; consistent |
| `case2-doel-zelf-staat` | waarde 60.00 | waarde 60.00 (the initialisation default's justification fails on an input value); consistent |
| `case3-opgave-zonder-feiten` | opgegeven waarde 50.00 | waarde 50.00; consistent |
| `case4-berekening` | taxatie 40.00 | waarde 35.00; consistent |
| `case5-opgave-wijkt-af` | taxatie 40.00, opgegeven waarde 50.00 | waarde 50.00; inconsistent `controle opgave waarde` |

All five pass in ref and clingo and play in the exec branch; the DRL-text
branch refuses them; two flips (case1 expected leeg, case2 expected 25.00)
fail in all three. [I] Had the initialisation moved to `berekende waarde`,
case2 would give 25.00 over the input 60.00, and case3 would report a
deviation between the opgave 50.00 and the initialisation value 25.00.

**The guard proof** (the same as the lowering proof, but the guard inside
the consistency: one assertion `Compound` `atLeast` 1 of `Predicate`
`empty` on `opgegeven waarde`, `Predicate` `empty` on `berekende waarde`,
and `Comparison` `equal` of the two; no deviation characteristic). Ref and
clingo pass the same seven value cases and fail the same three flips. The
Drools exec branch excludes the document on every case: `EXCLUDE
transpiler SKIP: consistentie: controle opgave waarde — consistentie
(schending buiten Phase-D scope): controle opgave waarde`. Its consistency
lowering takes an `all` compound, leeg and gevuld predicates,
characteristic checks and comparisons, and returns nothing for any other
compound (`ExecModelTranspiler.java:5550-5594`, "other compound: later";
the reason at `:5751`).

The probes live in `/tmp/kenmerkinvoer/` on the measuring machine
(`proef-b2/` the lowering proof, `proef-b3/` the initialisation proof,
`proef-b/` the guard proof, each with its `maak.py`), outside the
repository.

### 5.5 The explanation

"Uitleg = the name of the deciding block." Each generated default carries
its block's name in `source.snippet` — `opgegeven X` or `berekende X`; the
Catala frontend's `context-override <naam>` is the precedent
(`jutterswet-hardheidsaftrek-catala`). An explanation that reports the
deciding default of a group prints "waarde: opgegeven waarde" — given, not
derived. Because the computation is materialised, it can print the value
the computation gives beside it ("berekende waarde: 35,00"), which a
displaced default never leaves behind (ch. 7.1: default evaluation is
speculative; `PHASE4-DEFAULTS.md` §4.1). No executor in this repository
reports which default decided — the case format compares outcomes only —
so the explanation itself belongs to the regelspraak-java and werkbank
card that makes their explanation read defaults groups at all
(`backlog/new/20260923-regelspraak-java-defaults-in-scenario-en-werkbank.md`,
gap 3). The wire carries what it needs.

### 5.6 Opgave and facts that contradict each other

Decided (the main session): the materialised computation plus a
consistency, so that a contradiction deposits one §9.5 fact. [M] What
follows from it (§5.4):

- An opgave that differs from a filled computation deposits one fact,
  `controle opgave X` × the subject (ch. 5.6), and the case reads
  inconsistent. That includes a deliberate override such as a hardship
  decision: the fact then records that the override was used against a
  computation that said otherwise — the trace `PHASE4-DEFAULTS.md` §4.1
  asks a deviation to leave.
- An opgave without a computable value (no facts) is no contradiction and
  deposits nothing (the lowering proof's `case4`, the initialisation
  proof's `case3`); nor does an opgave equal to the computation (the
  lowering proof's `case2`).
- The characteristic `afwijkende opgave X` is readable by rules, so an
  author can route a deviating case (to a manual check, say); the §9.5
  fact itself is not rule-readable in v1 (ch. 7.2).

*Alternatives.* (b) The silent opgave: no consistency and no
characteristic — rejected: the deviation would leave no trace, which is
what the hardship pattern exists to avoid. (c) A choice per marker between
the two — not proposed: a second spelling for a choice no case asks for
yet. (d) One consistency with the guard in an `atLeast` 1 assertion
(the guard proof) — one generated name fewer, but outside the Drools exec
branch's consistency lowering today; taking it adds an arm there (the
violation of "at least one part holds" is "every part violated"), which is
backend work outside a frontend card.

### 5.7 Refusals, the warning, and the first card's limits

Each refusal is an ERROR at the declaration's line, naming its reason:

- the marker on a characteristic: the positive direction holds without a
  marker (ch. 7.8, the sentence of §4.1), and the negative direction needs
  the three-valued opgave of §5.8, a card of its own;
- the marker on an attribute whose datatype is not a number: the corpus
  witnesses attribute defaults targets of datatype Number only ([M]: 28 of
  28, none with a unit), so the first card admits a Number without a unit,
  and every other datatype waits for a golden per datatype family that
  plays in the three executors (a date, a text, an enumeration, a number
  with a unit);
- the marker on a time-dependent attribute (WF-086: no temporal defaults
  target) or a dimensioned one (fenced by the Drools exec branch's
  defaults lowering, `ExecModelTranspiler.java:7614-7615`);
- an allocation or an object-creation initialisation that writes X (their
  retargeting is not in the first card);
- a writer of X on a supertype or subtype of the declaring type (the
  ch. 7.10 composition of the generated group with inherited stacks is not
  in the first card);
- a generated name that the model already declares or uses — `opgegeven
  X`, `berekende X`, `afwijkende opgave X`, `opgave boven berekening X`,
  `afwijking opgave X`, `controle opgave X` — with both places in the
  message.

And one WARNING: the marker on an attribute that no rule writes. It is
input already; the marker changes nothing, and the frontend generates
nothing for it.

### 5.8 Characteristic targets

[I] A marker on a characteristic is not needed for the positive
direction: case input on the characteristic itself already stands against
every decision (§2, and the sentence of §4.1). What a marker would add is
the negative direction — an opgave "not present" that beats a computed
attribution. The case format cannot say "given absent" for a characteristic:
its `characteristics` list is two-valued (listed or not). Sugar without a
format change exists: a Boolean attribute `opgegeven <kenmerk>` (waar,
onwaar or leeg) and two generated defaults on the characteristic, justified
by `opgegeven <kenmerk>` equal to waar (`decision: true`) and equal to
onwaar (`decision: false`), above a default justified by a characteristic
`berekend <kenmerk>` that carries the authored writers. It needs form 1 (a
`decision: false` default, landed) and is not measured here. The Catala
frontend refuses the same shape for a `context` characteristic ("a
characteristic highest default is not wired",
`frontends/catala/lib/translate.ml:1724-1728`). Not in the first build card.

### 5.9 The semantic variant (the alternative)

A per-target marking on the wire — for instance a boolean on
`PrioritizedDefaults` — that moves the virtual input default of that target
above the ordinary defaults: Reading B per target, in ch. 7.8.

*What it would take:* a schema field; a WF rule (attribute targets only,
since the case format cannot give a characteristic "absent"); a tracked
ch. 7.8 delta and its `CONSTRUCTS.md` row; three executors — clingo's input
rule without its `not grpN_eff(S)` and the group's write guarded by the
`grpN_has_input(S)` atom the attribute arm already derives
(`backends/clingo/lib/translate.ml:9531-9533`), ref's input deciding before
the chain, the Drools exec branch skipping the group where the bean field
is filled.

*What it would lose:* (i) provenance in the case — a value on X would be a
fact on one target and an override on the next, depending on a flag in the
model, where the sugar gives the two their own attributes; (ii) the
computed value, never persisted when the input decides (ch. 7.1), so
neither an explanation nor a consistency could see a deviation
(`PHASE4-DEFAULTS.md` §4.1, last paragraph of the hardship pattern);
(iii) the separation `PHASE5-CATALA.md` decision 6 and `PHASE4-DEFAULTS.md`
§2.2 require — Reading B as default order, the input default reserved for
the dossier; (iv) `PHASE4-DEFAULTS.md` §10 decision 1 reopened per target,
which `DIALECT-DEFAULTS-NIET-EN-NESTING.md` §6 said the form-3 card should
not do.

*What it would gain:* no generated names, and the case supplies X itself.

Recorded as the alternative the maintainer can choose instead of the
sugar; not proposed.

### 5.10 The Catala frontend

- *The same pattern.* A highest default reading a value no rule writes,
  the ch. 7.8 input default untouched: decision 6's shape and the
  marker's.
- *A different lowering.* The Catala frontend prepends the override onto
  the callee's own chain, whose own default it forces into a defaults-group
  base (`frontends/catala/lib/translate.ml:1719-1774`, `:2455-2476`); its
  refused flanks follow from that — an own default that is a root-level
  unordered group ("a caller highest default above an unordered group is a
  recorded besluit-6 flank", `:1732-1737`), a branching override, a
  characteristic target. [I] The marker's lowering moves the authored
  writers whole onto `berekende X` and gives X a fresh two-default group,
  so it needs no case split on the shape of the authored writers: a
  root-unordered set moves as a set.
- *A scope that no scope calls* (§2.5). [M] Its `context` variable gets
  Reading A from the case dossier — 40 where `clerk run` with an
  overriding caller gives 43. The marker's lowering is a route for the
  Catala frontend too: an `opgegeven <naam>` attribute and the same
  two-default group for such a variable. Not proposed here: the card
  concerns the RegelSpraak dialect (§7, point 4).

## 6. The build card (text)

The text below is for the main session to place, as
`backlog/new/20260925-dialect-opgave-boven-berekening.md`; builders cut no
cards. Whether it lands before or after the pin-bump of regelspraak-java
is the maintainer's call.

````markdown
# dialect-opgave-boven-berekening: de markering `(mag opgegeven worden)` op een attribuut (vorm 3, suiker)

Bouwkaart uit de kaart kenmerkinvoer-en-invoer-boven-berekening (optie
7-iii: voorstel zonder bouw, hoofdsessie 2026-09-25). Bron:
`spec/proposals/KENMERKINVOER-EN-INVOER-BOVEN-BEREKENING.md` §5. Na de
kaart dialect-uitzondering-op-uitzondering (zelfde resolverbestanden, dus
serieel). Of zij vóór of ná de pin-bump van regelspraak-java komt, is het
sein van de maintainer.

Een werkbankgebruiker wil een afgeleid attribuut als invoer kunnen opgeven
en de afleiding laten wijken ("opgegeven, niet afgeleid"; Reading B),
zonder dat casusinvoer als geheel van Reading A afwijkt (RUNTIME-SEMANTICS
ch. 7.8; PHASE4-DEFAULTS §10 besluit 1). Het hardheidspatroon
(PHASE4-DEFAULTS §4.1) doet dat vandaag als programmastructuur, het
Catala-frontend genereert het voor `context` (PHASE5-CATALA besluit 6).
Deze kaart geeft het RegelSpraak-dialect er een declaratie voor. Geen
wire-, spec-, verifier- of uitvoerderswijziging: de lowering hieronder is
met de hand op de L5-wire gemeten en speelt in ref, clingo en de
Drools-exectak (voorstel §5.4, de lowering- en de initialisatieproef:
dertien cases, elk met een omgekeerde tegencase).

## Ontwerp (voorstel §5.1–5.6)

- Spelling: `(mag opgegeven worden)` als laatste element van een
  attribuutdeclaratie, vóór de `;`:

      de waarde Numeriek (getal met 2 decimalen) (mag opgegeven worden);

  Eén nieuw lexertoken `MAG_OPGEGEVEN_WORDEN : '(mag' WSP 'opgegeven' WSP
  'worden)'`, geheel met haakjes zoals `BEZIELD`, `BEZITTELIJK` en
  `BIJVOEGLIJK`; de grammatica laat het ook toe aan het eind van een
  kenmerkdeclaratie, zodat de resolver daar gericht weigert. Het token
  reserveert geen naamwoord.
- Lowering voor een gemarkeerd attribuut X van objecttype T:
  1. attribuut `opgegeven X` op T, datatype van X; geen schrijver, de
     casus levert het;
  2. attribuut `berekende X` op T, datatype van X; elke rekenende
     schrijver die de auteur voor X schreef (gelijkstelling, berekening,
     beslistabelrij, en de defaults-groepen van `Uitzondering op` en
     `Uitzonderingen ongeordend`) krijgt `berekende X` als doel; naam en
     vorm blijven, een ongeordende set verhuist als set;
  3. één `PrioritizedDefaults` op X met naam `opgave boven berekening X`,
     van hoog naar laag: de opgave-default (rechtvaardiging de lege
     `Compound` met `quantifier: "all"` en `source.snippet` "altijd",
     consequentie `opgegeven X`, `source.snippet` `opgegeven X`), de
     rekendefault (idem met `berekende X`) en, als de auteur een
     initialisatie voor X schreef, die initialisatie als
     `initialisation`-default onderaan: zij blijft op X;
  4. bezittelijk kenmerk `afwijkende opgave X` op T, toegekend door de
     regel `afwijking opgave X` onder `Compound` `all` van: `opgegeven X`
     gevuld, `berekende X` gevuld, `opgegeven X` ongelijk aan
     `berekende X`;
  5. consistentie `controle opgave X` met als assertie het kenmerk
     `afwijkende opgave X`, ontkend.
  Lezers van X elders blijven X lezen.
- Uitleg: de beslissende default draagt zijn bloknaam in `source.snippet`
  (`opgegeven X` of `berekende X`); `berekende X` houdt de berekende
  waarde vast, ook als de opgave beslist.
- Tegenspraak: opgave en berekening beide gevuld en ongelijk geeft één
  §9.5-feit `controle opgave X` × onderwerp plus het kenmerk; een opgave
  zonder berekenbare waarde is geen tegenspraak.
- Casusinvoer op X zelf blijft Reading A (ch. 7.8 ongewijzigd; voorstel
  §5.4 cases 6 en 7).

## Weigeringen (ERROR op de regel van de declaratie) en één waarschuwing (voorstel §5.7)

- de markering op een kenmerk (de positieve richting geldt zonder
  markering, ch. 7.8; de negatieve vraagt de driewaardige opgave van
  voorstel §5.8, eigen kaart);
- de markering op een attribuut dat geen Numeriek zonder eenheid is (de
  corpus draagt als attribuut-defaultsdoel alleen Numeriek, 28 van 28,
  geen met eenheid); een ander datatype of een eenheid pas met een eigen
  golden die in de drie uitvoerders speelt;
- de markering op een tijdsafhankelijk attribuut (WF-086) of op een
  gedimensioneerd attribuut;
- een verdeling of objectcreatie-initialisatie die X schrijft;
- een schrijver van X op een super- of subtype (ch. 7.10);
- een gegenereerde naam die het model al declareert of gebruikt, met beide
  plekken in de melding.
- WARNING: de markering op een attribuut dat geen regel schrijft (al
  invoer; er wordt niets gegenereerd).

## Acceptatie (bij oppakken aanscherpen)

- (a) Inertheid gemeten, niet aangenomen (de methode van `DIVERGENCES.md`
  §6): elk vlak RegelSpraak-document dat de werkbank bereikt, door het
  frontend vóór en na het token, IR plus diagnostics vergeleken; alle
  `bron.rgs`-goldens byte-identiek via gate 10.
- (b) Rood-eerst JUnit-getuigen: de vijf stukken van de lowering (namen,
  volgorde, `source.snippet`), elke weigering en de waarschuwing, met de
  weigerzinnen letterlijk.
- (c) Een golden met `bron.rgs` (werknaam
  `jutterswet-opgave-boven-berekening`, eigen Jutterswet-casus) met de
  cases van voorstel §5.4 (geen opgave, gelijk, afwijkend met het
  §9.5-feit, opgave zonder feiten, niets, X zelf in de invoer met en zonder
  feiten, initialisatie), groen in ref ≡ clingo ≡ Drools-exec. Gate 12:
  `EXPECTED_PLAYED` +1 en de golden in klasse (A) van `DRL_CHANNEL` (de
  DRL-teksttak heeft geen defaults-lowering); gate 7: de cases die een
  niets-doende uitvoerder haalt met reden op de overleverslijst. Beide
  scripts beschermd: **voorstel** op de branch.
- (d) `DIVERGENCES.md`: een eigen sectie voor de markering (spelling,
  lowering, weigeringen, de inertheidsmeting).
- (e) De registerzin voor `spec/CONSTRUCTS.md` in het Result (het dialect
  krijgt een oppervlak voor het hardheidspatroon; geen wire-delta); de
  hoofdsessie landt hem.
- (f) Alle veertien gates groen.

May touch: `frontends/regelspraak/**`, `corpus/golden/<nieuwe golden>/**`,
`scripts/check-drools.sh` en `scripts/check-noop.sh` (**voorstel**),
kaartverplaatsing.

Niet in deze kaart: de markering op een kenmerk (voorstel §5.8); de
semantische variant (voorstel §5.9); de uitleg in regelspraak-java en de
werkbank (kaart regelspraak-java-defaults-in-scenario-en-werkbank, gat 3);
het Catala-frontend (voorstel §5.10); de `atLeast`-arm in de
consistentielowering van de Drools-exectak (voorstel §5.6, alternatief d).
````

## 7. Open points

1. **The anchor's NOTE** (`corpus/golden/anchor/ww-herleving-inkomen/NOTE.md:364-368`,
   a protected path outside this card's reach) calls case7's expectation
   provisional until this card decides, and names options 7-i and 7-ii.
   Once the sentence of §4.1 lands, the paragraph can cite ch. 7.8 instead.
   *Resolved at the landing:* the bullet cites ch. 7.8 (a proposal on the
   protected path).
2. **Gate 7's survivor reason for case7** (`scripts/check-noop.sh:157-167`,
   protected) says the case is pinned "as long as the card
   kenmerkinvoer-en-invoer-boven-berekening does not choose its option
   7-ii"; with (7-ii) decided against, it can cite ch. 7.8. *Resolved at
   the landing:* the reason cites ch. 7.8 (a proposal on the protected
   script).
3. **A stale header comment in the Catala frontend**
   (`frontends/catala/lib/translate.ml:34` and `:51-59`) said `context`
   variables are refused; since decision 6 was executed (2026-08-24) the
   code lowers them (`:1306-1311`, `:1821-1856`). *Resolved at the
   landing:* the comment (`:34-36`, `:54-68`) describes the lowering.
4. **A `context` variable of a scope that no scope calls** gets Reading A
   from the case dossier (§2.5: 40 against Catala's 43). Whether that is a
   recorded flank, a refusal, or a case for the lowering of §5.3 in the
   Catala frontend is not decided anywhere; no corpus case reaches it.
5. **The characteristic marker** (§5.8) waits for a statute that needs an
   opgave "not present" above a computed attribution.
6. **The Drools exec branch lowers no consistency whose assertion is a
   compound other than `all`** (`ExecModelTranspiler.java:5594`): any such
   consistency is excluded, whatever card writes it. Found by the guard
   proof; no golden carries one today.
7. **The explanation** (§5.5) depends on the regelspraak-java card that
   makes the explanation read defaults groups at all
   (`backlog/new/20260923-regelspraak-java-defaults-in-scenario-en-werkbank.md`);
   that card can name the generated defaults' `source.snippet` as its
   source for "opgegeven, niet afgeleid".

## Sources

- RegelSpraak-specificatie v2.3.0 (Belastingdienst, 4 December 2025):
  §3.5, §9.2, §9.5, §9.6 — paraphrased, not quoted, not redistributed; the
  counts of §5.2 over the six markdown documents of the specification set
  in `regelspraak-werkbank/toolchain/specification/v2.3.0/`.
- Merigoux, D., Chataing, N., Protzenko, J. (2021). *Catala: A Programming
  Language for the Law.* Proc. ACM Program. Lang. 5 (ICFP), article 77.
  arXiv:2103.03198v2 — the caller override (C-Def) as `PHASE4-DEFAULTS.md`
  §2.2 and `PHASE5-CATALA.md` §3 map it. https://arxiv.org/abs/2103.03198
- The catala compiler 1.2.1 and its build tool `clerk` (the probe of
  §2.5).
- This repository: `spec/RUNTIME-SEMANTICS.md` ch. 5.3, 5.6, 7.1–7.3, 7.6,
  7.8, 7.10; `spec/IR-SPEC.md` ch. 3.5, 4.5, 6.1;
  `spec/proposals/PHASE4-DEFAULTS.md` §2.2, §4.1, §10;
  `spec/proposals/PHASE5-CATALA.md` §3, §8 decision 6;
  `spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md` §3.1, §3.5, §6, §10;
  `frontends/regelspraak/grammar/DIVERGENCES.md` §4, §6.
- Outside the repository: the research report
  `../research/dialect-defaults-niet-en-nesting/onderzoek-niet-in-uitzonderingen.md`
  (23 September 2026), §4.4 (flank 2, the measurement case7 now pins),
  §8.4 (form 3 and the options 7-i to 7-iii), §9 (decision 7).
