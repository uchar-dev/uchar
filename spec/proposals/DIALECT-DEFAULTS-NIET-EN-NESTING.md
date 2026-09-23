# DIALECT-DEFAULTS-NIET-EN-NESTING — negation and nesting in the `Uitzondering op` dialect

*Status: PROPOSAL · 2026-09-23 · working prose, not normative. The
deliverable of the research card
`backlog/doing/20260923-dialect-defaults-niet-en-nesting.md` (moved to
`done/` in this branch's last commit). It answers the card's research
question — why RegelSpraak (and RuleSpeak) know no "niet &lt;kenmerk&gt;" —
works out dialect forms 1 and 2, splits form 3 off into a card of its own,
lists the well-formedness coverage, gives a golden plan, and ends in §10
with an answer to each of the card's decisions 1–8. **The decisions are
taken (2026-09-23, §10). The maintainer decided points 2 and 4(d)
otherwise than first proposed here, and confirmed that the anchor's nested
justifications carry the whole chain below them. Those three stand here as
decided, with the first proposals named as rejected alternatives (card
`voorstel-niet-en-nesting-besluiten`).**
Nothing here changes `IR-SPEC.md`, `RUNTIME-SEMANTICS.md`, the schema, the
verifier, an executor, a frontend, a gate script or the corpus.
Repository claims carry file:line at `main` 8dd05e25. The measurements
(mark M below) were taken on 2026-09-23 on scratch builds of this commit —
the RegelSpraak frontend, the verifier, ref, the clingo backend and the
Drools backend — with probe documents kept outside the repository; none
of them is committed. Like `PHASE4-DEFAULTS.md`, this file lives outside
every gate-1 zone and names the private toolchain (regelspraak-java) and
the private regelspraak-werkbank; none of its text may be folded into a
zoned document without stripping those names first. The RegelSpraak
v2.3.0 specification (© Belastingdienst) is cited by section number and
paraphrased, never quoted.*

## 0. How to read the marks

Every factual statement about a source carries one of four marks:

- **[Q]** a verbatim quote, with author, year, title, section and URL in
  the source list at the end;
- **[P]** a paraphrase of a source (RegelSpraak v2.3.0 is only ever
  paraphrased);
- **[I]** an inference of this proposal — not stated in any source;
- **[M]** a measurement taken for this proposal (tool output quoted where
  it is load-bearing).

## 1. The answer in brief

1. **Why RegelSpraak has no negative attribution.** [P] The specification
   gives one reason (RegelSpraak v2.3.0 §9.2): a characteristic that no
   rule attributes is false (§3.5), so a negative attribution adds
   nothing. [I] Two further reasons follow from the language's lineage and
   are not stated in the specification: (i) RegelSpraak grew out of SBVR
   and RuleSpeak (§2.1), whose Accommodation and Wholeness principles
   resolve a conflict between rules by restating the rules, never by a
   priority — and a negative attribution beside a positive one is exactly
   such a conflict, for which a flat language without priority has no
   decision rule; (ii) its executors are forward-chaining production-rule
   engines without retraction, where the correctness of every negated read
   depends on characteristics only ever becoming true. **Inside a defaults
   group none of the three reasons applies**: the group has one writer and
   a total priority, and a negative decision writes nothing.
2. **RuleSpeak and SBVR** do have negation — SBVR as a logical operation
   and as the modal forms "it is prohibited that" / "it is impossible
   that", RuleSpeak as "must not" and, in Ross's articles of 2010, "is
   never considered" — but neither has an exception or priority device:
   an exception is written into the rule it limits (§3.2). The Dutch
   RuleSpeak text of 2009 has no negative derivation form at all.
3. **Catala has exactly the combination proposed here** (§3.3): a
   `condition` is a boolean whose default is `false` — RegelSpraak's
   characteristic — and a rule may conclude `not fulfilled`. Priority is
   structural; two applicable exceptions that decide differently are a
   fatal conflict [M].
4. **Chapter 5.3 is not violated** (§3.4). A `besluit: false` decision
   displaces lower defaults and writes nothing, the group decides once at
   the fixpoint of its justification strata (WF-051), and a
   characteristic the case input sets stays set [M].
5. **Forms 1 and 2 need no change to the wire, the verifier or an
   executor.** `besluit: false` and nested `Default.excepties` are carried
   by WF-050/051/075 and by ref, clingo and the Drools exec-model branch
   today [M]; the Drools DRL-text branch refuses every defaults document,
   as it does for the 24 defaults goldens it already lists. The work is in
   the RegelSpraak frontend. No new WF rule is needed; one existing clause
   (WF-022's value clause) has a verifier check but no negatief of its own
   (§7.3).
6. **A finding in the hand anchor.** `anchor/ww-herleving-inkomen` wrote
   its nested justifications without its carriers' conditions. Under
   RUNTIME-SEMANTICS ch. 7.9 an exception decides whether or not its
   carrier applies, so on inputs that its four cases did not exercise, the
   anchor answered against the statute text — identically in all three
   executors [M, §2.3]. Measured first on two inputs (8dd05e25); the
   enumeration at the repair card found four of the sixteen input
   combinations. The repair (G1, §8.1: every nested justification
   conjoined with its carrier's, `werkloos` included, ten cases) landed on
   2026-09-23 as a protected-path proposal, before the RegelSpraak twin.
7. **Form 3 (input above computation) is split off** into a card of its
   own (§6): it is expressible today as program structure, the missing
   part is sugar and explanation, and a semantic variant would touch the
   ch. 7.8 input default — a different axis from forms 1 and 2.
8. **The super/subtype byproduct has a root cause** (§9): the verifier's
   WF-079 assumes that a subtype's bare rule replaces the supertype's
   (`spec/verifier/lib/checks.ml:8899-8903`), while RUNTIME-SEMANTICS
   ch. 7.10 folds a bare rule into a stack only when the chain declares a
   defaults group (`RUNTIME-SEMANTICS.md:5179-5184`,
   `spec/verifier/lib/desugar.ml:286-290`). [M] The verifier passes, ref
   and the Drools exec branch find no fixpoint, clingo gives two values,
   and the DRL-text branch answers by source order.

The decisions, one line each (full text in §10). Decided 2026-09-23;
points 2 and 4 otherwise than first proposed here:

| # | Question (card) | Decided |
|---|---|---|
| 1 | Is "niet" in an exception legitimate? | Yes: it keeps §3.5/§9.2's closed world and ch. 5.3's monotone writes; it extends a departure from SBVR/RuleSpeak that `Uitzondering op` already made |
| 2 | Where may "niet" stand? | In every rule that becomes a default: an `Uitzondering op` body, a member of `Uitzonderingen ongeordend`, and the base of an `Uitzondering op` chain; never in a plain `Regel` that becomes no default; a group that never derives anything gets a warning. *First proposed:* never in a base |
| 3 | Spelling per kind | bijvoeglijk `is niet X`, bezittelijk `heeft geen X`, zelfstandig `is geen X` (and `is niet een X`) — the spellings conditions already accept, resolved by the same verbatim-first matcher; targeted messages replace measurement 1's |
| 4 | Nesting | Same header `Uitzondering op <naam>`; later written = higher; no depth bound; `Uitzondering op X` is restrictive — the frontend conjoins X's (itself conjoined) justification with the exception's own condition, at every level including the first; a widening deviation is written `Afwijking van X` with its own condition only. *First proposed:* justifications written out by the author, with a warning when a nested exception omits its carrier's condition |
| 5 | Nested exception under an inherited exception | Refused; the expressible form is a subtype-own exception with the combined condition |
| 6 | `Uitzonderingen ongeordend` under an exception | Yes, as `Uitzonderingen ongeordend op <naam>:`; under an exception it becomes that exception's `excepties`, under a base a group-default directly above the base |
| 7 | Form 3 | A card of its own (`kenmerkinvoer-en-invoer-boven-berekening`); §6's directions are input to it, not decided |
| 8 | Follow-up work in regelspraak-java | Cards of their own there; the grammar part of `testDocument` lands with the uchar frontend card |

## 2. What was measured

### 2.1 The frontend today

- **M1 — measurement 1 of the card, reproduced.** The card's p2 source
  gives, verbatim: `ERROR (line 11): Uitzondering op 'herleving art 21 lid
  1': uitzondering 'geen herleving art 21 lid 2 a' schrijft een ander doel
  (Recht.niet herleefd (kenmerk)) dan de basisregel (Recht.herleefd
  (kenmerk)) — een uitzondering moet hetzelfde onderwerp en
  kenmerk/attribuut betreffen (§4, WF-050)`. The cause: the result
  production `kenmerkToekenning : onderwerpKeten ( IS | HEEFT ) EEN?
  kaleNaam` (`frontends/regelspraak/grammar/RegelSpraak.g4:671`) lets
  `NIET` into the name (`kaleNaamWoord` includes `NIET`, `:559-566`), and
  `RuleChecker.kenmerk` (`RuleChecker.java:1533-1540`) takes the written
  name, article-stripped, as the target without looking it up.
- **M2 — measurement 2, reproduced.** `ERROR (line 17): Uitzondering op
  'geen herleving art 21 lid 2 a': geen basisregel met die naam gevonden
  — de basis moet een gewone platte regel (kenmerktoekenning of
  gelijkstelling) zijn`. The grammar already parses the header
  (`uitzonderingRegel`, `RegelSpraak.g4:59`, takes any name before the
  colon); the refusal comes from `ordeningsDefaults`
  (`RuleChecker.java:206-283`), which groups exceptions per base name and
  removes each lifted exception from `resolvedRegels`, so a later lookup
  of an exception's name finds nothing.
- **M3 — "niet" in a plain rule is emitted without a word.** `Regel geen
  herleving … Een Recht is niet herleefd indien hij nieuw recht ontstaan
  is.` is written to disk with `diagnostics: []`, as a positive
  `KENMERKTOEKENNING` of an undeclared characteristic `niet herleefd`.
  The verifier then says `WF-022 FAIL /resolvedRegels/0/doel: target 'niet
  herleefd' is neither an attribute nor a characteristic of 'Recht'`. The
  same holds for `heeft geen recht op uitkering` and `is geen
  uitkeringsgerechtigde` (targets `geen recht op uitkering`, `geen
  uitkeringsgerechtigde`). So §9.2 is enforced today only downstream, by
  name resolution, with a message that again names a characteristic the
  author never declared.
- **M4 — `is niet een X` does not parse as a result**: `syntaxfout:
  extraneous input 'een' expecting {<EOF>, UITZONDERING_OP, …}`. `EEN` is
  not a name word, so the zelfstandig negation that conditions accept
  (`RegelSpraak.g4:873`) has no result counterpart.
- **M5 — a `Regelgroep` ends at the first `Uitzondering op`** (card
  byproduct 1). A group `berging` over a base, an exception and a third
  rule emits `leden: ["bergingsbevoegd bij quotum"]` with `diagnostics:
  []`; the exception and the rule after it fall outside the group, and
  because the base is lifted into a defaults group the verifier refuses
  the document: `WF-033 FAIL /regelGroepen/0: group member 'bergingsbevoegd
  bij quotum' does not resolve to a rule form`.
- **M6 — `// Bron:` annotations vanish in a defaults group** (byproduct
  2). Two annotated blocks (a base and its exception) produce an IR in
  which neither annotation text occurs anywhere; the verifier passes. The
  lowering sets `bron: null` on the group and on every default
  (`RuleChecker.java:331-351`, "no per-group bron", "No per-default bron").
- **M7 — super- and subtype bare rules on one target** (byproduct 4, §9).

### 2.2 The IR and the executors

- **M8 — the anchor plays in the Drools exec branch.**
  `anchor/ww-herleving-inkomen` (four levels, `besluit: false` at two of
  them): `PLAYED 4` in the exec branch; the DRL-text branch prints
  `EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no
  defaults lowering` (listed as such in `scripts/check-drools.sh:845`).
- **M9 — a negative decision does not remove an input characteristic.**
  The anchor's `case4-einde` with `recht op uitkering` added to the input
  characteristics (the einde default decides `besluit: false`): the
  characteristic stays present in ref, clingo and the exec branch.
- **M10 — a flat negative attribution splits the executors, which is why
  WF-022 refuses it.** A `KENMERKTOEKENNING` on `herleefd` whose value is
  the literal `onwaar`: the verifier says `WF-022 FAIL
  /resolvedRegels/0/waarde: characteristic target's value must be the
  boolean constant true`; forced through anyway, ref leaves the
  characteristic absent, clingo stops with `TRANSLATION FAULT:
  characteristic attribution with a non-waar value`, the exec branch sets
  it present and the DRL-text branch leaves it absent.
- **M11 — unordered exceptions above a base, on a characteristic.** A
  defaults group `[group-default{ongeordend: [x → waar], [y → onwaar]},
  base → waar]`: verifier PASS; four cases (base only, x, y, x and y) pass
  in ref, clingo and the exec branch, the last one with the conflict fact
  keyed `Werknemer.recht op uitkering/0`.
- **M12 — the wire the RegelSpraak twin should emit** (§8.2): first-level
  exception as a chain position, deeper exceptions nested, conditions
  written out, `recht op uitkering` bezittelijk. Measured on 8dd05e25 with
  the conditions first proposed (without `werkloos`): verifier PASS; seven
  cases (the anchor's four, probes A and B of §2.3, the input case of M9)
  passed in ref, clingo and the exec branch. Re-measured 2026-09-23 on the
  landed anchor (§8.1: conditions conjoined with `werkloos`, bezittelijk)
  restructured to this shape: verifier PASS; all ten cases pass in ref and
  clingo, `PLAYED 10` in the exec branch; the DRL-text branch refuses it on
  its defaults sentence.

### 2.3 The anchor's corners

*Updated 2026-09-23 at the landing of card
`anker-ww-herleving-dragervoorwaarde` (branch commits 40849db2, 78a0b801):
the anchor carries the repair of §8.1. The first measurement (probes A and
B, on 8dd05e25) stands; the sixteen-combination enumeration of that card,
and its measurements, extend it.*

Before the repair, the anchor's tree
(`corpus/golden/anchor/ww-herleving-inkomen/NOTE.md:81-93` at 8dd05e25) gave
each nested default only its own condition: einde `maandinkomen >
inkomensgrens`, herleving `omstandigheid vervallen`, blokkade `nieuw recht
ontstaan EN nieuw maandloon > inkomensgrens`. RUNTIME-SEMANTICS ch. 7.9
evaluates a default's exception groups first, whatever the default's own
justification (`RUNTIME-SEMANTICS.md:4937-4940`, `:4951-4955`). Write W =
`werkloos`, T = `maandinkomen > inkomensgrens`, V = `omstandigheid
vervallen` and N = `nieuw recht ontstaan` ∧ `nieuw maandloon >
inkomensgrens`. [I] Enumerated over the sixteen combinations of W, T, V
and N under ch. 7.9, the unconjoined tree answers against the statute
reading on exactly four, and the four existing cases reach none of them:

| Probe | Input | Statute reading | Unconjoined tree (ref, clingo, exec) | Landed case |
|---|---|---|---|---|
| A | ¬W ¬T V ¬N — not werkloos; maandinkomen 1800 (below the 2100 grens); omstandigheid vervallen | no right: nothing arose, nothing ended, nothing revives | **present** — herleving decides waar outside einde | `case6-niet-werkloos-wel-vervallen` |
| B | W ¬T ¬V N — werkloos; maandinkomen 1800; nieuw recht ontstaan, nieuw maandloon 2300 | right stands: it never ended, and lid 2 only blocks a revival | **absent** — blokkade decides onwaar outside herleving | `case5-geen-eindiging-wel-nieuw-recht` |
| C | ¬W T V ¬N — case1 without werkloos: maandinkomen 2500, omstandigheid vervallen | no right: without werkloosheid no right arose (art. 15), so none ended and none revives | **present** — herleving decides waar outside the grant | `case8-niet-werkloos-boven-grens-vervallen` |
| D | W ¬T V N — probe B plus omstandigheid vervallen | right stands, as in B | **absent** — blokkade decides onwaar outside einde | `case9-geen-eindiging-wel-vervallen-en-nieuw-recht` |

[M] On each of the four, all three executors give the unconjoined answer
(`MISMATCH` against the statute reading in ref and clingo, `FAIL` in the
exec branch): the executors are right about the IR, the IR was not right
about art. 15 and 21.

[M] Two repairs were measured. The one this section first proposed — the
carriers' conditions without W (einde `T`, herleving `T ∧ V`, blokkade
`T ∧ V ∧ N`) — passes A, B and D but still answers **present** on C in all
three executors: herleving decides waar for a werknemer in whom no right
ever arose. [I] C is the only one of the sixteen combinations on which it
differs from the landed repair. [M] The landed repair conjoins at every
nested level, the carrier's (itself conjoined) justification first, as one
flat `ALLE`: einde `ALLE[W, T]`, herleving `ALLE[W, T, V]`, blokkade
`ALLE[W, T, V, nieuw recht ontstaan, nieuw maandloon > inkomensgrens]`. It
passes all ten cases of §8.1 in ref, clingo and the exec branch, the four
existing ones with their expected values unchanged. [I] Without the target
characteristic in the input it gives `recht op uitkering` ⇔ W ∧ (¬T ∨ (V
∧ ¬N)).

[I] Art. 21 lid 1 itself restates its carrier ("op grond van artikel 20,
eerste lid, onderdeel a of c, is geëindigd"), lid 2 does not ("Het recht
herleeft niet indien"), and art. 20 lid 1 presupposes an existing right
("Het recht op uitkering eindigt"): statute drafting mixes both styles, so
a model has to decide per exception what the text presupposes. §5.4
records decision 4(d) for the dialect: `Uitzondering op` is restrictive at
every level (decided 2026-09-23). For the anchor's wire the maintainer's
card settled it, and the decision question of 2026-09-23 confirmed it:
levels 2 to 4 each conjoin with their carrier, the first nested level
(einde, on the grant's `werkloos`) included. C's statute reading rests on reading `werkloos` as the ground of
art. 15 at the moment the right arose — the reading case4 already relied on
(werkloos together with an income above the grens) and that the anchor's
NOTE now states.

## 3. The research question: why no "niet &lt;kenmerk&gt;"?

### 3.1 (a) RegelSpraak itself

**What the specification says.**

- [P] §3.5: a characteristic is a property an object has or does not
  have. By default it is absent (onwaar); it can only be true when the
  input sets it or when an applicable characteristic-attribution rule
  exists (§9.2). Unlike a Boolean attribute it is never leeg: rules test
  its presence, not a value. It comes in three kinds — bijvoeglijk
  (spoken with *zijn*), bezittelijk (spoken with *hebben*) and neither
  (spoken with *zijn* and an article, called zelfstandig in this repo).
- [P] §9.2: a characteristic attribution decides whether an instance gets
  the characteristic and assigns it if so, in three patterns (`is een`,
  `is`, `heeft`). Because a characteristic is false when no attribution
  was made, a negative attribution cannot be made; the section gives a
  bijvoeglijk negative sentence as an example of what is not admitted.
- [P] §8.1.8 and Tabel 16: conditions do negate characteristics —
  bijvoeglijk `is niet`, bezittelijk `heeft geen`, neither `is geen`;
  Tabel 16's singular declarative column lists `is niet` / `heeft niet` /
  `is niet een`. The language negates in conditions and never in results.
- [P] §2.1: the first attempts at managing rule specifications at the
  Belastingdienst were based on SBVR and RuleSpeak; the makers found
  those languages not strict enough and created RegelSpraak (end of 2009)
  as a stricter extension. The specification follows the ALEF
  implementation, whose code generator turned the rules into Blaze rules.
  [I] Blaze Advisor is a production-rule engine; the toolchain this repo
  mirrors targets Drools, another one.

**The three reasons.**

1. *The closed world* (the stated reason). [I] §9.2 is the closed-world
   assumption — in SBVR's wording [Q, §3.2 below] "if a proposition cannot
   be proved true, it is assumed to be false" — applied to
   characteristics. Under it a negative attribution carries information
   only where some other rule, or the input, would make the
   characteristic true. Everywhere else it is redundant. The frontend
   applies the same reasoning to decision tables: a kenmerk conclusion
   `onwaar` is skipped with a warning
   (`BeslistabelDesugar.java:201-204`), which is harmless as long as the
   table's rows exclude each other.
2. *No priority* (not stated). [I] The one place where a negative
   attribution would carry information — beside a positive one for the
   same characteristic — is a conflict. A flat language needs a decision
   rule for it, and RegelSpraak has none: v2.3.0 has no exception,
   priority or rule-ordering construct
   (`PRIORITIZED-DEFAULTS-EVOLUTIE.md:192-195`). Its SBVR/RuleSpeak lineage
   explains why none was wanted: those sources resolve conflicts by
   restating the rules (Accommodation, §3.2), and RegelSpraak writes an
   exception as a negated conjunct in the base rule's condition
   (`PRIORITIZED-DEFAULTS-EVOLUTIE.md:200-205`).
3. *Monotone execution* (not stated). [I] In a forward-chaining engine
   without retraction a negated read is sound only if the characteristics
   it reads can only become true: RUNTIME-SEMANTICS ch. 5.3 requires
   negated reads of derived characteristics at the fixpoint of the
   positive part, which exists independently of rule order only because
   every characteristic write sets `waar`. [M] The Drools backend here
   uses no truth maintenance (no `insertLogical`, `retract` or `delete`
   anywhere under `backends/drools/src/main`), and its stratifier states
   the dependency on monotonicity explicitly: [Q, own code]
   "kenmerken are monotone, so a missed positive is recoverable; a
   consumed negation is not" (`Stratificatie.java:30-32`). A negative
   attribution would be a destructive write beside a positive one, and
   the agenda order would decide between them — [M] measurement M7 (§9) shows
   that effect on two attribute writers in the DRL-text branch, whose
   answer follows source order.

**What "niet" would mean against an input characteristic.** [P] §3.5
puts the input on a par with a rule as a source of truth: a
characteristic is true when the input sets it *or* a rule attributes it.
[I] A negative decision can therefore at most prevent an attribution; it
cannot contradict the dossier. [M9] This is how all three executors
behave today inside a defaults group: an input characteristic stays
present under a deciding `besluit: false`. [I] It differs from attribute
targets, where a deciding default overwrites an input value (Reading A,
RUNTIME-SEMANTICS ch. 7.8); for characteristics the input already stands
above a negative computation. §6 returns to this.

**Conclusion (a).** [I] The closed-world argument is the reason the
specification gives, but not the whole reason: the absence of a priority
and the monotone execution model are equally structural. All three hold
for a flat rule set. None of them holds inside a defaults group, which
has one writer, a total priority and a decision that is taken once at the
fixpoint of its justification strata (§3.4).

### 3.2 (b) RuleSpeak and SBVR

**RuleSpeak Sentence Forms** (Ross, version 2.2, 2001–2009).

- [Q] "Every Business Rule can be expressed using one of the following two
  words. • “must” • “only”" (Basic Concepts).
- [Q] The derivation form is "… must be considered … if …", which
  "Indicates that something is to be classified or derived a certain way
  if some condition(s) hold true." (Table 1, form 1.0). The negative forms
  of the table are prohibitions: "… must not …" — "Indicates that
  something is not allowed." (form 2.0). [I] The table has no negative
  derivation; "must not be considered … if …" would, by the table, be a
  prohibition on classifying, not a derivation that something is not so.
- [Q] "The general point to remember about expressing guidance is that
  nothing is ever required (that is, everything is allowed) unless a
  Business Rule explicitly removes some degree of freedom." [I] RuleSpeak's
  default concerns permission, not derived facts.
- [Q] The forms "are also not aimed at the highest forms of guidance –
  that is, the language of laws, regulations, contracts, business
  policies, etc." [I] RuleSpeak expects statute text to be interpreted
  into practicable rules first; the exception structure of the statute is
  dissolved in that step, which is the step the defaults machinery exists
  to avoid.

**Ross's later keywords for definitional rules.**

- [Q] "RuleSpeak prescribes the rule keywords *always* or *never* to
  express a definitional rule." (Ross 2010a, "What Is a Business Rule?")
- [Q] Its negative example: "A customer is never considered a gold
  customer if the customer has been incorporated less than a year."
  (Ross 2010a); the positive one: "A customer is always considered a gold
  customer if the customer places more than 12 orders during a calendar
  year." (Ross 2010b, "Business Rules vs. System Design Choices").
- [Q] "Definitional rules can be ill-conceived, misunderstood, or
  misapplied, but they cannot be directly violated." (Ross 2010b)
- [Q] "*All* constraints should be expressed as rules separate from
  definitions." (Ross 2006) The article does not discuss negated
  definitions — [M] searched, not found.
- [I] So RuleSpeak does know a negative classification ("is never
  considered"), as a definitional rule of impossibility. Ross's two
  examples overlap (a customer younger than a year with more than twelve
  orders), and RuleSpeak offers no priority to settle the overlap; the
  next point says what it offers instead.

**Exceptions: Accommodation and Wholeness.**

- [Q] Ross (2009) lists, among the approaches that do not work, "Setting
  up some priority scheme to determine which 'wins'." His verdict on all of
  them: "sometimes semantics are hidden or, at least, external. Not good."
  and, after restating the rules, "once you *accommodate*, there really
  are no exceptions(!)."
- [Q] SBVR 1.5 §16.3.2, the Accommodation Principle: "An element of
  guidance whose meaning conflicts with some other element(s) of guidance
  must be taken that way; if no conflict is intended, the element(s) of
  guidance must be expressed in such a way as to avoid the conflict." and
  "Exceptions to elements of guidance must be accommodated explicitly;
  that is, cases where exceptions to elements of guidance are intended
  must be worded in such a way to avoid any conflict in the meanings."
- [Q] SBVR 1.5 §16.3.3, the Wholeness Principle: "An element of guidance
  means only exactly what it says, so it must say everything it means."
- [I] This is the stance RegelSpraak took over: an exception is folded,
  negated, into the rule it limits. The uchar dialect left it on
  2026-08-29 (`PRIORITIZED-DEFAULTS-EVOLUTIE.md` Q1a): an `Uitzondering op`
  block states a priority and does not say everything it means on its
  own. Adding "niet" does not add a new departure; it completes that one
  for characteristic targets.

**Negation and the open world in SBVR.**

- [Q] SBVR 1.5 Annex A.2.1.2: "it is not the case that p — logical
  negation". Annex A.2.1.3: "it is prohibited that p — obligation
  formulation embedding a logical negation"; "it is impossible that p —
  necessity formulation embedding a logical negation"; "… never … —
  necessity formulation embedding a logical negation". And: "The keyword
  ‘not’ is used within an expression after the verb “is” as a way of
  introducing a logical negation."
- [Q] SBVR 1.5 §24.2.1.3: "Adopting closed world semantics basically
  means that all relevant facts are known (either as primitives – not
  defined in terms of other things – or derivable). So if a proposition
  cannot be proved true, it is assumed to be false. This closed world
  assumption entails negation by failure, since failure to find a fact
  implies its negation." and "One might assume open world semantics by
  default, and then apply local closure to specific parts as desired; or
  alternatively, assume closed world semantics by default and then apply
  “local openness.” We adopt the former approach as it seems more
  realistic when modeling real business domains."
- [P] In the same section, where negative facts are known and must be
  stated, SBVR models them as a separate characteristic ('Person is a
  nonsmoker') with an exclusion constraint against the positive one.
- [Q] SBVR 1.5 §16.4.1: "In a light world, anything that is not
  expressly prohibited is assumed permitted, and anything not expressly
  declared as impossible is assumed possible."
- [I] SBVR needs explicit negation because it is open-world by default.
  RegelSpraak closes the world for characteristics (§3.5) and can
  therefore drop negative attribution — the step §9.2 takes. SBVR's way
  of stating a known negative fact (a separate positive characteristic)
  remains available in RegelSpraak, and the card's measurement 4 (the
  gate characteristic "uitgesloten van herleving") is that pattern.

**The Dutch RuleSpeak text** (Spreeuwenberg, versie 2.3, 2009).

- [Q] Its rule keywords: "“moet” • “mag niet” • “(mag) alleen”" (p. 5).
- [Q] Its derivation form "… moet beschouwd worden als … indien …" —
  "Geeft aan hoe iets geclassificeerd moet worden als aan bepaalde
  voorwaarden is voldaan." (Tabel 1, p. 7); and "… mag niet …" — "Geeft
  aan dat iets niet is toegestaan."
- [Q] "Volg tijdens het opstellen van aanwijzingen steeds het algemene
  principe dat niets moet (met andere woorden, alles mag) tenzij een regel
  expliciet een bepaalde vrijheid ontneemt." (p. 11)
- [M] Not found: a negative derivation form, or a Dutch counterpart of
  "is never considered". [I] Ross's *always*/*never* keywords appear in
  his 2010 articles, after this translation.

**Conclusion (b).** [I] Neither RuleSpeak nor SBVR rejects negation as
such; SBVR is built on it. What both reject is deciding between
conflicting rules by priority: a negative rule beside a positive one must
be restated until the two no longer overlap. RegelSpraak inherits that
stance, and because it also closes the world for characteristics, the
negative rule has nothing left to say. The reason for §9.2 is therefore
the combination "closed world + no priority"; a dialect that has priority
removes the second half.

### 3.3 (c) Catala

**The documentation.**

- [Q] "`condition` is a syntactic sugar for declaring a boolean scope
  variable whose default value is `false`." (Catala book, "Catala-specific
  questions", section "When to choose `condition`, `rule` *vs* booleans?")
- [Q] "In the body of the scope, you must use `rule` instead of
  `definition` for defining under which conditions the `condition` should
  be `fulfilled` (`true`) or `not fulfilled` (`false`)." (same section)
- [Q] "It is possible to use `exception` and `label` on rules like for
  definitions, but all the `rule`s are implicitly exceptions of a base
  case where the condition is `false`." (same section)
- [Q] The paper's desugaring table rewrites "rule X under cond. Y cons.
  fulfilled" to "label L𝑋 def. X equals false (inserted once)" plus
  "exception L𝑋 def. X under cond. Y cons. equals true" (Merigoux,
  Chataing & Protzenko 2021, §4.1, Table 1 (i)); in §3.2 the paper
  describes the same thing as "the built-in default, set to false, is
  overridden with a rule that has higher priority".
- [Q] "Each of the exceptions 𝑒𝑖 is evaluated; if two or more are valid
  (i.e. not of the form ∅), a conflict error ⊛ is raised." (§4.1) The
  error is "fatal by D-ContextConflictError: the reduction aborts, under
  any context 𝐶" (§4.2).
- [Q] "The children of a given node are mutually-exclusive exceptions to
  their parent definition; either one exception applies, and the
  computation stops. Or if no exception applies, the parent definition is
  evaluated instead." (§4.4)

[I] A Catala `condition` is RegelSpraak's characteristic: boolean, closed
world, default false. Catala nevertheless has a negative consequence,
`not fulfilled`, and needs it for the same reason as the WW case: an
exception that withdraws what a lower rule grants.

**Measured with catala 1.2.1** (probe programs outside the repository,
each interpreted through a small test scope):

| Probe | Program | Input | Result |
|---|---|---|---|
| T1 | lid1: `rule herleefd under condition omstandigheid_vervallen consequence fulfilled`; lid2a `exception lid1`: `… under condition nieuw_recht consequence not fulfilled`; `exception lid2a`: `… under condition wia consequence fulfilled` | omstandigheid vervallen | `true` |
| T2 | same | + nieuw recht | `false` — `not fulfilled` displaces lid1 |
| T3 | same | + nieuw recht + wia | `true` — the exception on the exception |
| T4 | same | only wia | **`true`** — the nested exception decides although neither carrier applies |
| T5 | same | nothing | `false` — the condition's default |
| V1 | two exceptions on one label, `not fulfilled` and `fulfilled`, both applicable | — | `During evaluation: conflict between multiple valid consequences for assigning the same variable.` |
| V2 | same, only the `not fulfilled` one applicable | — | `false` |
| W1 | two exceptions on one label, both `not fulfilled`, both applicable | — | `false`, no conflict (equal values; the PHASE4-DEFAULTS §2.2 field note) |
| Z1 | two plain rules, one `fulfilled`, one `not fulfilled`, both applicable | — | conflict error, as V1 |
| Y1 | a `context` condition overridden by the caller (`rule r.herleefd under condition opgave consequence fulfilled`) while the callee's own exception says `not fulfilled` | opgave | `true` — the caller's value wins (Reading B) |
| Y2 | same | no opgave | `false` — the callee's own computation |

[I] Four things follow for the dialect. Catala's exceptions carry
self-contained conditions (T4) — the same reading as RUNTIME-SEMANTICS
ch. 7.9 and the reason for the anchor's corners (§2.3). A plain negative
rule in Catala is an unordered sibling of the positive rules, with a
conflict where both apply (Z1): that is a coherent semantics for
decision 2's alternative (b) (§10). Catala's input override is a caller
mechanism (Y1), the polarity of form 3 (§6). And the conflict is fatal,
where uchar keeps it observable on the §9.5 channel (the tracked row-8
deviation).

**The golden and the frontend.** Line 25 of
`corpus/golden/jutterswet-penningrecht-catala/bron.catala_en` is
`definition penninggerechtigd equals false`: the
plugin lowers the boolean default tree to a characteristic defaults group
whose lowest default carries `besluit: false`
(`frontends/catala/lib/translate.ml:1645-1669`); the lens of the 2026-08-21
card showed that default to be inert on every input, the input
characteristic included (`backlog/done/20260821-fase5a-jutterswet-catala.md`,
triage item 3a). A single boolean definition that is literally false —
with no exception tree around it — is refused: "a boolean defined as
literal false is not attributable as a characteristic"
(`translate.ml:1612`, fixture `frontends/catala/test/refuse_boolfalse.catala_en`).
[I] The Catala frontend thus already draws the line decision 2 proposes:
`false` exists inside a defaults tree and never as a flat attribution.

### 3.4 (d) Does "niet" in one defaults group violate ch. 5.3?

No. [P, own spec] Ch. 5.3 requires two things: characteristics are
monotone within one evaluation (default false, only ever set true), and a
negated read of a derived characteristic is evaluated at the fixpoint of
the positive part. A negative decision inside a defaults group keeps
both:

- **It writes nothing.** ch. 7.1: "a do-not-attribute decision displaces
  lower defaults and makes the group derive nothing, which is not a
  retraction" (`RUNTIME-SEMANTICS.md:4603-4608`). The three executors
  implement exactly that: ref `| Decided (Char_dec false) -> (w, false)`
  (`ref/lib/eval.ml:3165`); clingo derives `heeft/2` from a group only for
  the decision `1` (`backends/clingo/lib/translate.ml:8063-8064`); the
  Drools exec branch writes only `if (b.charBesluit())`
  (`ExecModelTranspiler.java:7649`). The characteristic is therefore
  written at most once, `waar`, or never.
- **It decides once, after its reads have settled.** WF-051 makes every
  justification read — recursively through the nested exception groups —
  a negative edge toward the group's write (`IR-SPEC.md:7725-7738`), so
  the group sits strictly above every producer it reads, and no transient
  "not yet true" can reach its decision.
- **Its readers are unchanged.** A rule that reads the target positively
  reads a monotone characteristic; a rule that reads it negated gets the
  usual negative edge and sits above the group.
- **It cannot undo the input.** [M9] An input characteristic stays set.

[I] Negation in a defaults group is thus displacement, not a write — the
reading RUNTIME-SEMANTICS already gives it, in the sentence the card
quotes ("ch. 5.3's monotone write regime is untouched — the group
decides", `RUNTIME-SEMANTICS.md:4606-4608`). A flat negative attribution
would be a write, and that is why WF-022 keeps refusing it (M10).

### 3.5 What "niet" in an exception says — and what it does not

Proposed wording for the dialect documentation: *A negative decision in
an exception states that, where this exception is the highest deciding
default of its group, the rule set does not attribute the characteristic.
It does not state that the characteristic is false: a characteristic the
input sets stays set, and without input the closed world of RegelSpraak
§3.5 makes it false anyway.* [I] This is the literal content of
`besluit: false`, and it keeps RegelSpraak §3.5's "input or attribution"
intact.

## 4. Form 1 — the negative decision in an exception

### 4.1 Spelling per kind of characteristic

The negative result takes the spellings conditions already accept
(RegelSpraak v2.3.0 §8.1.8 and Tabel 16, [P]; the frontend's condition side
in `RuleChecker.bindKenmerk`, `RuleChecker.java:2793-2811`, and the
`is niet een` branch, `RegelSpraak.g4:873`):

| Kind (declaration) | Positive result (§9.2) | Negative result (proposed) | Negative condition today |
|---|---|---|---|
| bijvoeglijk — `is herleefd kenmerk (bijvoeglijk);` | `Een Recht is herleefd` | `Een Recht is niet herleefd` | `hij is niet herleefd` |
| bezittelijk — `het recht op uitkering kenmerk (bezittelijk);` | `Een Werknemer heeft recht op uitkering` | `Een Werknemer heeft geen recht op uitkering` | `hij heeft geen recht op uitkering` |
| zelfstandig — `de uitkeringsgerechtigde kenmerk;` | `Een Werknemer is een uitkeringsgerechtigde` | `Een Werknemer is geen uitkeringsgerechtigde` (and `is niet een uitkeringsgerechtigde`) | `hij is geen …`, `hij is niet een …` |

The resolution rule is the condition side's, unchanged: the written name
is matched **verbatim first**, so a declared characteristic whose name
begins with `niet` or `geen` binds positively; only when the whole name
binds nothing is a leading `niet ` or `geen ` read as the negation and the
rest matched; if that binds nothing either, the sentence is refused with a
message naming the undeclared characteristic (§4.5). A verb that does not
fit the kind (`is geen` on a bezittelijk characteristic) gets the WARNING
the condition side already gives (`keurNegatieSoort`,
`RuleChecker.java:2613-2623`). [I] One negation vocabulary for both sides
of a rule is the simplest thing to teach: whatever an author may test as
`niet` in a condition, he may decide as `niet` in an exception.

### 4.2 Where a negative decision may stand (decision 2)

*Decided 2026-09-23: the base of an `Uitzondering op` chain is admitted
(first proposed: refused).*

- **Admitted:** the body of an `Uitzondering op` block, a member of an
  `Uitzonderingen ongeordend` group (with or without `op <naam>`, §5.6),
  and the base of an `Uitzondering op` chain. In an unordered group a
  negative member never changes the outcome by itself — where it alone
  decides, nothing is written, as where nothing decides — but a negative
  and a positive member that both apply give the
  ch. 7.2 conflict: no write and one §9.5 fact. [I] That makes the
  characteristic conflict, today hand-authored only
  (`anchor/jutterswet-toelagegronden`), writable from source, and it
  replaces the sentence of `DIVERGENCES.md:101-103` ("a genuine §9.5
  conflict is only expressible on an attribute target"). A negative base
  is the lowest default: under the closed world it decides `onwaar` and
  writes nothing, so the outcome does not change. What it buys is that the
  author can write the statute ("herleeft niet, tenzij …") and that the
  exceptions hang on a named base without an invented positive rule —
  Catala's `definition x equals false` (the penningrecht base is the inert
  example).
- **Warned:** a defaults group in which every default decides `onwaar`
  derives nothing on any input. It is well-formed — it means "never" — so
  it is no error, but the RuleChecker warns (F9 in §7.4).
- **Refused:** a plain `Regel` that becomes no default. A negative plain
  rule is inert under the closed world or, beside a positive rule, a
  conflict without a priority (§3.1). §9.2 thus keeps holding for every
  plain `Regel`, and the divergence from the arbiter stays inside the
  extension: the blocks whose headers announce it and the bases they name.
- *Rejected (first proposed here):* refusing the base as well, so that §9.2
  would hold for every `Regel`, a base included.

### 4.3 Grammar impact on the rule level

- **No change** for `is niet X`, `is geen X` and `heeft geen X`: they
  already parse, because `NIET` is a name word and `geen` lexes as an
  ordinary lower-case word (`RegelSpraak.g4:559-566`). What changes is the
  resolver's reading of the name (§4.4).
- **One alternative** for `is niet een X`:
  `kenmerkToekenning : onderwerpKeten ( IS | HEEFT ) EEN? kaleNaam |
  onderwerpKeten IS NIET EEN kaleNaam ;` — no new token and no reserved
  name word. [M4] No document parses this sentence today, so the
  alternative is inert on every existing document.
- The production is shared by `Regel` and `Uitzondering op`
  (`regelVersie`, `RegelSpraak.g4:59`, `:613-630`). Whether a negative
  result is admitted is therefore the resolver's decision, not the
  grammar's.
- The inertness claim for the resolver change needs the same measurement
  the filled-count extension recorded (`DIVERGENCES.md` §6: 876 flat
  documents emitted byte-identically before and after). [I] The only
  documents that can change are those with a negative result sentence,
  which §9.2 excludes from official RegelSpraak and which today yield a
  WF-022-failing IR (M3).

### 4.4 RuleChecker consequences

1. `RuleChecker.kenmerk` (`RuleChecker.java:1533-1540`) resolves the
   written name with the condition-side matcher (verbatim first, article
   strip, `niet`/`geen` strip) and records the polarity. A negative result
   gets an internal marker — for instance the value literal `onwaar` —
   that is never emitted on a flat rule.
2. `defaultVan` (`:344-351`) maps the polarity to `besluit: false`
   instead of always `Boolean.TRUE`.
3. A check after the lift refuses every rule that still carries the
   negative marker — a plain `Regel` that became no default; a base is
   admitted (decision 2) — with the targeted message of §4.5. The frontend
   then refuses a flat negative attribution itself, citing §9.2 and
   WF-022, instead of emitting it (M3). A group in which every default
   decides `onwaar` gets a WARNING (F9).
4. `groepsDefaults` (`:285-329`) admits negative members.
5. Target equality (`DefaultsDoel.gelijk`, `:189-193`) compares the
   de-negated target, so `niet herleefd` and `herleefd` are one target.
6. The javadoc of `ir/PrioritizedDefaults.java` still says "the
   RegelSpraak resolver never builds it", which has been untrue since
   2026-08-29; the build card corrects the sentence because it changes the
   lowering the sentence describes.
7. `DIVERGENCES.md` §4: the sentences "the frontend never emits
   `besluit: false`" (`:91-92`) and the attribute-only conflict sentence
   (`:101-103`) are replaced by the spelling table, the admitted
   positions and the refusals.

### 4.5 The misleading message of measurement 1, and its replacements

| Situation | Today | Proposed |
|---|---|---|
| `is niet X` in an `Uitzondering op`, X declared | ERROR "schrijft een ander doel (Recht.niet herleefd (kenmerk))" (M1) | no message; `besluit: false` |
| `is niet X` in an `Uitzondering op`, X not declared | the same misleading ERROR | ERROR "uitzondering '&lt;naam&gt;': 'niet X' ontkent geen kenmerk van Recht — 'X' is niet gedeclareerd (§3.5)" |
| `is niet X` in a plain `Regel` | no diagnostic; IR on disk; verifier WF-022 FAIL on target 'niet X' (M3) | ERROR "regel '&lt;naam&gt;': een kenmerktoekenning kan niet ontkennen — zonder toekenning is 'X' al onwaar (§9.2, WF-022); een ontkenning staat in een 'Uitzondering op'-blok, in een 'Uitzonderingen ongeordend'-groep of in de basis van een 'Uitzondering op'-keten" |
| `is niet X` in a base | as for a plain `Regel` | no message; `besluit: false` on the base (decision 2, decided 2026-09-23; first proposed: an ERROR "de basisregel ontkent"); a group whose every default decides `onwaar` gets the WARNING of F9 |
| `is niet een X` | syntax error "extraneous input 'een'" (M4) | parses; then as the rows above |
| `Uitzondering op <naam van een uitzondering>` | ERROR "geen basisregel met die naam gevonden — de basis moet een gewone platte regel … zijn" (M2) | no message; nesting (§5) |
| `Uitzondering op <onbekende naam>` | the same ERROR | ERROR "Uitzondering op '&lt;naam&gt;': geen regel of uitzondering met die naam gevonden" |

## 5. Form 2 — the exception on an exception

### 5.1 The form

`Uitzondering op <naam van een uitzondering>: <naam>` — the header of
today, unchanged. The name before the colon may now denote a plain
`Regel` (a base) or another `Uitzondering op` block. The resolver builds a
tree: every block names one parent, a plain `Regel` is a root. Refused,
each with its own message: a parent name that resolves to nothing; a cycle
(`A` on `B`, `B` on `A`), reported with its path; a parent on another
target (the existing WF-050 message); a parent with another validity
window or with more than one version (the existing fences); and, in the
first build card, a carrier that has both ordered children and an
`ongeordend` group (§5.6).

### 5.2 Lowering

| Source | Wire |
|---|---|
| `Regel B` with `Uitzondering op B: E1` and `Uitzondering op B: E2` | one group, chain `[E2, E1, B]` — as today (`RuleChecker.java:271`, later written stacks higher) |
| `Uitzondering op E1: F1` and `Uitzondering op E1: F2` | `E1` becomes a form-a default with `excepties = {ongeordend: false, leden: [ExceptionGroup [F2, F1]]}` |
| `Uitzonderingen ongeordend op E1:` with members `U1`, `U2` | `E1.excepties = {ongeordend: true, leden: [ExceptionGroup [U1], ExceptionGroup [U2]]}` |
| `Uitzonderingen ongeordend op B:` with members `U1`, `U2` | a group-default directly above `B`: chain `[…, {excepties: {ongeordend: true, leden: [[U1], [U2]]}}, B]` |
| any deeper level | the same rules, recursively |

Under decision 4(d) (§5.4, decided) the shapes above do not change; what
changes is the justification on the wire: every `Uitzondering op` block
carries its carrier's conjoined justification followed by its own
condition, flattened into one `ALLE`. An `Afwijking van <basis>: <naam>`
block lowers as a first-level `Uitzondering op` does — a chain position
above the base — with its own condition only (`jutterswet-bergingsdrempel`,
whose `ir.json` stays byte-identical). Whether `Afwijking van` may also
name an exception is left to the build card.

**Why the first level stays a chain.** [P, own spec] RUNTIME-SEMANTICS
ch. 7.10 defines the base as the lowest ordinary default that decides
something itself, lets a subtype's own base replace the inherited base,
and keeps inherited exceptions above it (`RUNTIME-SEMANTICS.md:5085-5102`).
A form-a base — own content plus `excepties` — is a base, and a subtype
base replaces it whole. [I] If the dialect nested first-level exceptions
inside the base, as the hand anchor does, a subtype's base would silently
remove every exception the supertype wrote. Kept as chain positions, the
exceptions survive base override, which `jutterswet-wrakrecht` already
relies on (its inherited `betwist` exception outranks the subtype base).

**Equivalence with the anchor's shape.** [I] With single-member exception
groups, `[E2, F, E1, B]`, `[E2, E1{F}, B]` and `B{[E2, E1{F}]}` order the
defaults identically (F before E1's own decision before B), so they decide
identically on every input; they differ in the index path of a nested
conflict key and in what a subtype's base replaces. [M12] The twin's wire
of §8.2 — first level a chain, deeper levels nested — passes the anchor's
cases in all three executors.

### 5.3 Sibling order and depth

- **Order:** among the children of one carrier, the later-written block
  ranks higher — the rule the first level follows today. [I] Keeping one
  rule for every level avoids a second convention; statute order (earlier
  paragraph higher) is the alternative and would reverse existing
  behaviour.
- **Depth:** no bound. WF-075 recurses to any depth, the three executors
  recurse (`ref/lib/eval.ml:3120-3155`, the clingo per-path predicate
  family, the exec branch's recursive `DefaultsRun.decide`), and the
  anchors pin three and four levels. [I] A bound would be a number without
  a statute behind it; SUBTYPING.md §11 point 5 left chain depth unbounded
  for the same reason.

### 5.4 The carrier's justification (decision 4, last question)

*Decided 2026-09-23 (the maintainer's decision question, on the research
report outside the repository,
`../research/dialect-defaults-niet-en-nesting/onderzoek-niet-in-uitzonderingen.md`
§8.2 and §9; card `voorstel-niet-en-nesting-besluiten`): option (D) below.
This section first proposed (A); (A), (B) and (C) stay as rejected
alternatives.*

Does a nested exception implicitly carry its carrier's condition? The
facts:

- [P, own spec] RUNTIME-SEMANTICS ch. 7.9 evaluates a default's exception
  groups before, and independently of, its own justification; ch. 7.1
  asks for self-contained justifications and says that a frontend desugars
  textual nesting by conjoining the enclosing layer's condition
  (`RUNTIME-SEMANTICS.md:4633-4637`).
- [M] Catala does not conjoin (T4); [P, own spec] neither does the IR.
  Two authors wrote nested exceptions as if it did: the hand anchor (§2.3) and the
  card's own probe p3 (`tegenuitzondering … indien hij wia ontstaan is`).
- [M] Written without the carrier's condition, the anchor's nested
  exceptions decided wrongly on four of the sixteen input combinations
  (two measured first, on 8dd05e25; four after the enumeration of §2.3).
  Written with the carriers' conditions at every nested level, `werkloos`
  included, all ten cases of §8.1 pass; written with them but without
  `werkloos`, probe C stays wrong.
- [I] At the first level the override reading is intended: an exception
  there may widen its base (`jutterswet-bergingsdrempel`: noodweer and
  eight wrecks against a base of twenty; art. 5 Jutterswet: "zes" in place
  of "twaalf"), so conjoining the base's condition there would switch the
  exception off. The decision answers this with a separate header for the
  widening case, not with an unconjoined first level.

Four options:

- **(D) A restrictive `Uitzondering op`, with `Afwijking van` for
  widenings — decided.** The frontend conjoins X's justification — itself
  already conjoined, so the whole chain below X — with the exception's own
  condition, at every level, the first included, and emits one flat
  `ALLE`, carrier conjuncts first (the landed anchor's wire, §8.1). A
  deviation that widens its base (hardship: "als ware twaalf vervangen door
  zes") is not an exception but a deviation, written `Afwijking van X:
  <naam>` with its own condition only; `jutterswet-bergingsdrempel`
  (noodweer with "≥ 8" above "≥ 20") becomes one, its `ir.json`
  byte-identical. *Reasons (the decision's):* ch. 7.1 prescribes the
  conjunction; no formalism lets an exception inherit its base's condition
  — Catala measurably does not (T4) — so it has to be written somewhere,
  and the frontend is where that happens mechanically and visibly on the
  wire; the legal meaning of an exception is restrictive; corner B of the
  anchor (§2.3) is the silent error of the other choice. [I] (D) accepts
  the cost (B) is faulted for below — a source that says less than its
  wire — but at every level alike, so one header keeps one meaning.
- **(A) Self-contained, written by the author, with a lint** — first
  proposed here; rejected. The dialect emits exactly the condition
  written, at every level. The RuleChecker WARNS when a nested exception's
  condition does not
  contain every conjunct of its carrier's condition (flattened `ALLE`
  members, compared without `bron`): "uitzondering 'F' op 'X' herhaalt de
  voorwaarde van 'X' niet (ontbreekt: …) — zij beslist dan ook waar 'X'
  niet geldt (RUNTIME-SEMANTICS ch. 7.9); bedoel je dat, schrijf haar dan
  als uitzondering op de drager van 'X'". One meaning per construct at
  every level, Catala's reading, SBVR's Wholeness, and the wire equals the
  source. The cost is repetition, which ordinary RegelSpraak factoring
  absorbs: a derived characteristic ("is herleefbaar geëindigd") can carry
  a carrier's condition once.
- **(B) Implicit for nested levels** — rejected. The frontend conjoins the
  carrier's effective condition into every exception on an exception,
  never at the first level. Closest to ch. 7.1's wording and to how
  statute text presupposes its scope ("Het recht herleeft niet indien");
  but one header then means two things depending on what it names, and a
  source that says less than its wire is exactly the hidden semantics Ross
  rejects (§3.2).
- **(C) An explicit scope keyword** — rejected. For instance
  `Uitzondering binnen <naam>: <naam>`, which conjoins the named rule's
  effective condition at any level, beside `Uitzondering op`
  (self-contained, as today). Explicit and uniform; it costs a second
  header and its own inertness measurement. (D) is (C) turned around: the
  existing header conjoins, and the new one, `Afwijking van`, does not.

The anchor carries the repair of §8.1 (landed); its wire is
self-contained by definition, so it is the same under every option.

### 5.5 Inheritance (decision 5)

A subtype never edits an inherited exception (SUBTYPING.md §4.3,
RUNTIME-SEMANTICS ch. 7.10), and the wire has no place for it: an
exception declared for the supertype lives in the supertype's defaults
group, and a subtype's group cannot address one of its defaults — that
would be the per-default addressing PRIORITY-RELATION decision 1 closed.
Proposed: an `Uitzondering op X` whose subject is a strict subtype of X's
subject, with X an exception of the supertype, is **refused**, with the
expressible form in the message: a subtype-own exception on the subtype's
base carrying X's condition plus its own and its own decision — by the
fixed block order it outranks the inherited X exactly where both apply.
Nested exceptions written on the supertype are inherited with it unchanged
(SUBTYPING.md §4.4). [I] The refusal costs nothing the wire could carry.

### 5.6 Unordered exceptions under a rule (decision 6)

Proposed: `Uitzonderingen ongeordend op <naam>:` followed by `Regel`
members, lowered as in §5.2 — under an exception into its `excepties`
with `ongeordend: true`, under a base into a group-default directly above
the base. [M11] The group-default form carries both decisions and the
conflict key `…/0` in ref, clingo and the exec branch. [P, own code] The
Catala frontend lowers a chain exception carrying unordered exceptions to
the same nested form (`frontends/catala/lib/translate.ml:28-31`); it
refuses unordered exceptions on a shared base (fixture
`refuse_sharedbase.catala_en`), which the group-default form of §5.2 now
makes expressible — a Catala-side observation, not work of this card. For
art. 21 WW this form carries lid 2 and lid 3 as parallel grounds under
lid 1, and onderdelen a and b of lid 2 as parallel grounds within lid 2.

### 5.7 Grammar and RuleChecker impact of form 2

- **Grammar:** nothing for nesting — the header exists. One optional part
  for decision 6: `uitzonderingGroep : UITZONDERINGEN ONGEORDEND ( OP
  uitzonderingBasisNaam )? COLON regel+ ;`, and for decision 4(d) a
  header `Afwijking van <naam>: <naam>`; the build card makes both
  whole-phrase header tokens (`UITZONDERINGEN ONGEORDEND OP`,
  `AFWIJKING_VAN`) on the pattern of `UITZONDERING_OP`. [I] Inert:
  neither `Uitzonderingen ongeordend op` nor `Afwijking van` parses today.
- **RuleChecker:** `ordeningsDefaults` (`RuleChecker.java:206-283`)
  becomes a tree build — resolve every block first, build the parent map,
  refuse cycles with their path, check target, window and version per
  edge, build defaults bottom-up, and remove the lifted rules from
  `resolvedRegels` only at the end (removing them per base, as now, is
  what turns M2's lookup into "not found"). `groepsDefaults` handles the
  `op <naam>` form. New are the conjunction of §5.4 (D) — the carrier's
  justification first, flattened into one `ALLE` — the `Afwijking van`
  header and the inheritance refusal of §5.5; the scope lint of the
  rejected option (A) is not built.
- **DIVERGENCES.md:** the limitation "Exception-on-exception … is not
  authored from source in this card" (`:129-130`) is replaced by the
  lowering table of §5.2, the order and depth rules, the chosen reading of
  §5.4 and the refusals.

## 6. Form 3 — input above computation: split off (decision 7)

**What exists.**

- [P, own spec] Case input follows Reading A: the virtual input default
  sits below every ordinary default, so a deciding default overwrites an
  input value (RUNTIME-SEMANTICS ch. 7.8; `anchor/jutterswet-vondstwaardering`,
  `case6-default-overschrijft`). Reading B for case input was declined on
  2026-08-20 (`PHASE4-DEFAULTS.md` §10 decision 1).
- [P, own spec] Reading B exists as program structure: the input enters the
  dossier as a fact of its own, and a highest default reads it
  (`PHASE4-DEFAULTS.md` §4.1, the hardship pattern). For Catala `context`
  variables it is generated from source (`PHASE5-CATALA.md` decision 6,
  executed 2026-08-24; golden `jutterswet-hardheidsaftrek-catala`). [M]
  Catala itself: Y1/Y2 of §3.3.
- [I] In the dialect the pattern is writable today for an attribute
  target, as the last-written first-level exception:

  ```
  Uitzondering op maxdatum van een recht: opgegeven maxdatum
      geldig altijd
          De maxdatum van een Recht moet gesteld worden op zijn opgegeven maxdatum
          indien zijn opgegeven maxdatum gevuld is.
  ```

- [M9] For a characteristic target the positive direction already holds:
  an input characteristic stands against every negative decision. The
  negative direction needs form 1 and an explicit negative input
  characteristic — SBVR's 'nonsmoker' pattern (§3.2):

  ```
  Uitzondering op toekenning art 15: opgave zonder recht
      geldig altijd
          Een Werknemer heeft geen recht op uitkering
          indien hij opgegeven zonder recht is.
  ```

  An input that sets both `recht op uitkering` and `opgegeven zonder
  recht` keeps the characteristic (M9): the pattern overrides a
  computation, never another input.

**Why a card of its own.** [I] Form 3 is a different axis from forms 1
and 2: the polarity of input per target, not the shape of the exception
tree. As sugar it needs a declaration or rule marker and an explanation
that names the deciding block — decisions that belong with the werkbank's
user interface. As semantics — a per-target input default *above* the
ordinary defaults — it would change RUNTIME-SEMANTICS ch. 7.8, the clingo
`inputval/3` encoding, the Drools input handling and possibly the case
format: protected paths and three executors, where forms 1 and 2 touch
none of them.

**Directions for that card**, answering decision 7's sub-questions:

- *Per attribute or per group:* per target; a defaults group is per target
  already. The natural place for a marker is the attribute declaration;
  the card decides whether the marker generates the pattern above (sugar,
  no wire change — the proposed start) or changes ch. 7.8 (semantics).
- *The explanation "opgegeven, niet afgeleid":* the deciding default is
  the named block (`opgegeven maxdatum`); an explanation that reports the
  deciding default says it. That requires the regelspraak-java explanation
  to read defaults groups at all (byproduct 3, §9).
- *Facts and opgave that contradict each other:* materialise the computed
  value on its own attribute (`berekende maxdatum`) and compare the two in a
  consistency rule, which deposits one §9.5 fact when they differ
  (`PHASE4-DEFAULTS.md` §4.1, the last paragraph of the hardship pattern).
  No new mechanism.
- *Not to be reopened by it:* case input as a whole stays Reading A.

Card: `backlog/new/20260923-invoer-boven-berekening.md`.

## 7. Well-formedness

### 7.1 Rules that already cover forms 1 and 2

| Rule | What it guarantees for forms 1–2 | Witnesses today |
|---|---|---|
| WF-050 (`IR-SPEC.md:7688-7707`) | a characteristic default carries `besluit` — either value — and no consequence; the shape checks recurse into sub-defaults; one writer per target; two or more groups on one target all `ongeordend` | anchors `jutterswet-uitsluiting` (a `false` default in the chain), `jutterswet-nestkenmerk`, `ww-herleving-inkomen`; ten negatiefs, among them `wf050-subdefault-wrong-shape`, `wf050-defaults-beside-rule` |
| WF-075 (`:7739-7754`) | the nested form: at least one exception group; two or more carry `ongeordend`, at every level; no initialisation in a sub-default; no contentless default; a group-default carries no justification; any depth | anchors `jutterswet-nestuitzondering`, `-nestkenmerk`, `-nestdiepte`; five negatiefs |
| WF-051 (`:7725-7738`) | every descendant read is a negative edge toward the root write; no cycle through a group | `jutterswet-uitsluiting`; `wf051-cycle-through-subdefault` |
| WF-086 (`:7708-7724`) | no temporal target, nested forms included | its negatiefs |
| WF-020 / WF-107 (`:2015-2065`) | the lifted group carries the base rule's name, unique and non-blank | every golden |
| WF-022 (`:2075-2082`) | a flat characteristic attribution's value is the constant true — the wire form of §9.2 | `wf022-target-unresolvable` only (§7.3) |

[I] Form 1 is a `besluit` value WF-050 already admits; form 2 is the form
WF-075 was written for. The frontend only has to produce what the hand
anchors and the Catala plugin already produce.

### 7.2 No new wire rule — the candidates considered

- *A characteristic group must contain a `besluit: true`.* An all-false
  group derives nothing on every input: inert, not ill-formed, and no
  executor disagrees on it (the Catala plugin emits an inert false base).
  At most a frontend warning — decided: the RuleChecker warns (decision 2,
  F9).
- *A depth bound.* No principled number (§5.3).
- *A nested exception's condition implies its carrier's.* Undecidable in
  general and contrary to the self-contained reading of ch. 7.9. Under
  decision 4(d) the frontend conjoins the carrier's condition itself, so
  the wire satisfies it by construction; the lint of the rejected option
  (A) lapses.
- *No nested exception under an inherited exception.* Unwritable on the
  wire — a subtype's group cannot address a default of the supertype's
  group — so it is a frontend refusal, as WF-106 is a refusal without a
  verifier check.
- *Form 3.* Out of this card (§6).

### 7.3 A witness gap in WF-022

WF-022 has two clauses: the target resolves, and a characteristic target's
value is the constant true. The verifier checks both
(`spec/verifier/lib/checks.ml:1400-1416`), but the WF index lists one
negatief, `wf022-target-unresolvable`, which exercises only the first
(`IR-SPEC.md:7806`). [M10] A document violating the second clause splits
the executors four ways. Proposed negatief, as text (the card cuts the
file):

- directory `corpus/negatief/wf022-kenmerk-onwaar/`, `ir.json`: one object
  type `Recht` with the characteristics `herleefd` and `nieuw recht
  ontstaan`, and one flat rule —

  ```json
  {"node": "ResolvedRegel", "naam": "geen herleving", "soort": "KENMERKTOEKENNING",
   "doel": {"node": "AttribuutRef", "attribuut": "herleefd", "objecttype": "Recht",
            "onderwerpBron": "Een Recht", "navigatieRol": null, "type": {"node": "Booleaan"},
            "bron": "Een Recht is herleefd", "set": false, "navigatiePad": []},
   "waarde": {"node": "Literal", "waarde": "onwaar", "type": {"node": "Booleaan"}, "bron": "onwaar"},
   "voorwaarde": {"node": "Kenmerk", "kenmerk": "nieuw recht ontstaan", "navigatie": null,
                  "negatie": false, "eigenaar": null, "periode": null,
                  "bron": "hij nieuw recht ontstaan is"},
   "geldigheid": {"node": "Geldigheid", "vanaf": null, "tot": null}}
  ```

- expected rejection: `WF-022 FAIL /resolvedRegels/0/waarde:
  characteristic target's value must be the boolean constant true` [M10];
- the WF-022 index row gains the negatief (a protected-path edit, made as
  a proposal).

### 7.4 Frontend refusals and their fixtures

The refusals of forms 1 and 2 are frontend checks, not wire rules; like WF-106's, their
witnesses are frontend fixtures (JUnit, beside
`DefaultsExtensieTest.java`), each pinning a message fragment:

| Id | Source (abridged) | Expected fragment |
|---|---|---|
| F1 | `Regel geen herleving … Een Recht is niet herleefd indien …` | ERROR "een kenmerktoekenning kan niet ontkennen" |
| F2 | *withdrawn (decision 2, decided 2026-09-23: a negated base is admitted).* First proposed: `Regel stilte … Een Recht is niet herleefd …` plus `Uitzondering op stilte: … Een Recht is herleefd …` | — (first proposed: ERROR "de basisregel ontkent") |
| F3 | in an `Uitzondering op`: `Een Recht is niet vervallen` with `vervallen` undeclared | ERROR "ontkent geen kenmerk van Recht" |
| F4 | `Uitzondering op B: A` and `Uitzondering op A: B` | ERROR "kring in de uitzonderingsboom: A → B → A" |
| F5 | an `Uitzondering op <uitzondering van Strandrecht>` whose sentence is about a `Wrakrecht` | ERROR "een subtype bewerkt geen geërfde uitzondering" |
| F6 | one carrier with an `Uitzondering op X` child and an `Uitzonderingen ongeordend op X:` group | ERROR naming both children (first card) |
| F7 | *withdrawn (decision 4(d), decided 2026-09-23: the frontend conjoins the carrier's condition itself).* First proposed: a nested exception without its carrier's conjuncts (under the rejected §5.4 (A)) | — (first proposed: WARNING "herhaalt de voorwaarde van") |
| F8 | `is geen` on a bezittelijk characteristic | WARNING "Tabel 16 vereist 'heeft'" (the existing lint) |
| F9 | a defaults group in which every default decides `onwaar` — for instance a negated base whose exceptions all decide `onwaar` as well | WARNING; the build card chooses the fragment (the group never derives anything; decision 2) |
| F10 | `Afwijking van <onbekende naam>: <naam>` | ERROR naming the unknown carrier; the build card chooses the fragment (decision 4(d)) |

## 8. Golden plan

### 8.1 G1 — repair of `anchor/ww-herleving-inkomen` (protected path)

*Landed 2026-09-23 as a proposal branch the maintainer merges (card
`anker-ww-herleving-dragervoorwaarde`, commits 40849db2 and 78a0b801, the
bezittelijk kind in 48800a13, case10 in 22730193). This section records the
landed form; where it differs from the version first proposed here, the
first version is named.*

- Every nested default's justification is one flat `ALLE` (one
  `Samengesteld`, no nested `ALLE`): its carrier's conjuncts first, in the
  carrier's order, then its own. einde `ALLE[werkloos, maandinkomen >
  inkomensgrens]`; herleving `ALLE[werkloos, maandinkomen > inkomensgrens,
  omstandigheid vervallen]`; blokkade `ALLE[werkloos, maandinkomen >
  inkomensgrens, omstandigheid vervallen, nieuw recht ontstaan, nieuw
  maandloon > inkomensgrens]`. The tree keeps its four levels in
  `Default.excepties`; outside these three justifications `ir.json` is
  structurally unchanged. *First proposed:* herleving and blokkade without
  `werkloos`, einde unchanged — that version answers probe C of §2.3
  against the statute. Confirmed by the maintainer on 2026-09-23 (decision
  question): the nested justifications carry the whole chain below them,
  `werkloos` included.
- Ten cases, six of them new, expected values from the statute reading
  (hand computations in the anchor's NOTE; `inkomensgrens` 2100.00 in all):

| Case | Input | `recht op uitkering` | Decided by |
|---|---|---|---|
| `case1-herleving` … `case4-einde` | unchanged | unchanged | herleving, grant, blokkade, einde |
| `case5-geen-eindiging-wel-nieuw-recht` | probe B (case2 plus the nieuw recht) | present | grant (art. 15) |
| `case6-niet-werkloos-wel-vervallen` | probe A | absent | nothing |
| `case7-invoer-staat-tegen-blokkade` | case3's facts plus `recht op uitkering` in the input | present | blokkade decides onwaar; the input stands |
| `case8-niet-werkloos-boven-grens-vervallen` | probe C (case1 without werkloos) | absent | nothing |
| `case9-geen-eindiging-wel-vervallen-en-nieuw-recht` | probe D (case5 plus omstandigheid vervallen) | present | grant (art. 15) |
| `case10-nieuw-recht-onder-grens` | case3's facts with a nieuw maandloon of 2000.00, below the grens | present | herleving (art. 21-1) |

  The four corner cases also pin `inkomensgrens`, `verstreken periode` and
  `nieuwe maxdatum`. case7 is the first case to pin ch. 7.1's "not a
  retraction" on a characteristic (the 2026-08-21 card left that corner
  without a case) and pins only the characteristic and `consistent`.
  case10 (fix round 3 of the card) pins the blokkade's own conjunct
  `nieuw maandloon > inkomensgrens`: [M] a blokkade without it fails
  case10 in ref, clingo and the exec branch, and before case10 no case
  noticed its removal. Its other own conjunct, `nieuw recht ontstaan`,
  stays unpinned: it is observable only with a nieuw maandloon above the
  grens and no nieuw recht, an input without meaning in the casus.
  *First proposed:* three cases — `case5-herleving-zonder-einde` (probe
  A), `case6-blokkade-zonder-einde` (probe B) and `case7-invoer-blijft`
  on case4's facts; probes C and D were not yet known.
- [M] No-op check: case5, case6, case8, case9 and case10 refute a no-op run
  through their computed attributes. case7 survives it by construction —
  the standing is what it tests — and stands with its reason in the
  survivor list of `scripts/check-noop.sh` (gate 7: 564 refuting, 37
  survivors; red without the entry). *First proposed:* no new entry, which
  held for a case4-based input case that also pinned `inkomensgrens`.
- `recht op uitkering` is `BEZITTELIJK` in `kenmerkSoorten`, as first
  proposed here: the card left it out, and its fix round 2 (commit
  48800a13) added it at the maintainer's request, so the twin can write
  `heeft (geen) recht op uitkering` and land byte-identically on the
  anchor. [M] The kind steers only that source spelling. With
  `BIJVOEGLIJK` and with `BEZITTELIJK` the verifier passes, all ten cases
  pass in ref and clingo, the exec branch plays 10, gate 12's pins stay
  182 and 161, the ASP program clingo solves is byte-identical per case,
  and the Drools bean carries the same field (`rechtOpUitkering`,
  boolean). The Drools loader parses the table and no backend code reads
  it back; ref and the ASP backend do not read it at all.
- The NOTE gains the self-contained reading of ch. 7.9, the four corners
  of §2.3 with what the unconjoined tree answers on each, the reading of
  `werkloos` on which probe C rests, the reason for the bezittelijk kind,
  and all ten hand computations.

[M] The repaired tree passes all ten cases in ref, clingo and the Drools
exec branch (`PLAYED 10`); the DRL-text branch keeps refusing the golden on
its defaults sentence, and gate 12's pins (played 182, DRL 161) do not
move. Gate 13 does not probe this golden: its only unit, `dag`, declares no
canonical and no factor, so there is nothing to twist.

*Consequence for G2 (§8.2):* the twin's wire must carry the same flat
`ALLE` per nested default, carrier conjuncts first and `werkloos` included
at einde, with `recht op uitkering` bezittelijk, and its cases are these
ten. §8.2 gives the source in the spelling decision 4(d) fixes.

### 8.2 G2 — the RegelSpraak twin (required by the card)

Directory `corpus/golden/ww-herleving-inkomen-rgs/` (name open), built by
the penningrecht method (`PHASE5D-DIFF.md` §2–3;
`backlog/done/20260821-fase5a-jutterswet-catala.md`): the same law through
a second channel, the emitted IR checked in and canonicalised through gate
2's serialiser, a structural read with every `bron` removed and keys
sorted, the same cases with the same absolute expectations as the (repaired)
hand anchor, and each difference classified as (i) dialect difference,
(ii) IR design flaw or (iii) frontend defect.

*Updated 2026-09-23 (card `anker-ww-herleving-dragervoorwaarde`, fix
rounds 2 and 4; card `voorstel-niet-en-nesting-besluiten`): the source
follows the landed anchor of §8.1 — `werkloos` in the conditions of einde,
herleving and blokkade, `recht op uitkering` bezittelijk, ten cases — in
the spelling decision 4(d) fixes: each block writes its own condition, and
the restrictive `Uitzondering op` supplies its carrier's.*

Source (`bron.rgs`) under the decisions of 2026-09-23 (§10) — the
declarations, the base rule and the three rekenregels:

```
Objecttype de Werknemer (mv: Werknemers) (bezield)
    het maandloon Numeriek (niet-negatief getal met 2 decimalen);
    het maandinkomen Numeriek (niet-negatief getal met 2 decimalen);
    het nieuw maandloon Numeriek (niet-negatief getal met 2 decimalen);
    de inkomensgrens Numeriek (getal met 2 decimalen);
    de eindigingsdatum Datum in dagen;
    de vervaldatum Datum in dagen;
    de maxdatum Datum in dagen;
    de verstreken periode Numeriek (geheel getal) met eenheid dagen;
    de nieuwe maxdatum Datum in dagen;
    is werkloos kenmerk (bijvoeglijk);
    is omstandigheid vervallen kenmerk (bijvoeglijk);
    is nieuw recht ontstaan kenmerk (bijvoeglijk);
    het recht op uitkering kenmerk (bezittelijk);

// Bron: WW art. 15
Regel toekenning art 15
    geldig altijd
        Een Werknemer heeft recht op uitkering
        indien hij werkloos is.

Regel inkomensgrens van een werknemer
    geldig altijd
        De inkomensgrens van een Werknemer moet berekend worden als zijn maandloon maal 0,8750 rekenkundig afgerond op 2 decimalen.

Regel verstreken periode bij herleving
    geldig altijd
        De verstreken periode van een Werknemer moet gesteld worden op de tijdsduur van zijn eindigingsdatum tot zijn vervaldatum in hele dagen.

Regel nieuwe maxdatum bij herleving
    geldig altijd
        De nieuwe maxdatum van een Werknemer moet berekend worden als zijn maxdatum plus zijn verstreken periode.
```

The three `Uitzondering op` blocks. Under decision 4(d) each block writes
its own condition only; the frontend conjoins the carrier's justification,
the first level included, and flattens the result into one `ALLE`,
carrier conjuncts first in the carrier's order — the landed wire of §8.1:

```
// Bron: WW art. 20 lid 1 onderdeel c
Uitzondering op toekenning art 15: einde art 20 lid 1 c
    geldig altijd
        Een Werknemer heeft geen recht op uitkering
        indien zijn maandinkomen groter is dan zijn inkomensgrens.

// Bron: WW art. 21 lid 1
Uitzondering op einde art 20 lid 1 c: herleving art 21 lid 1
    geldig altijd
        Een Werknemer heeft recht op uitkering
        indien hij omstandigheid vervallen is.

// Bron: WW art. 21 lid 2 onderdeel a
Uitzondering op herleving art 21 lid 1: blokkade art 21 lid 2 a
    geldig altijd
        Een Werknemer heeft geen recht op uitkering
        indien hij aan alle volgende voorwaarden voldoet:
        - hij is nieuw recht ontstaan
        - zijn nieuw maandloon is groter dan zijn inkomensgrens.
```

[I] The source says less than its wire: the frontend supplies `werkloos`
at einde (the first level included) and the carrier conditions at
herleving and blokkade. *Rejected alternative (first proposed, §5.4
(A)):* each block writes its carrier's conditions out and the frontend
emits exactly what is written — einde `werkloos` and T, herleving
`werkloos`, T and V, blokkade all five.

[M] The declarations, the three rekenregels, the bezittelijk attribution
and the list form of a composite condition (a four-part one on the probe)
already resolve on the current frontend (a probe without the exception
blocks, 8dd05e25); only the three `Uitzondering op` blocks need the build
card.

Expected result of the diff:

| Item | Class | Status |
|---|---|---|
| einde is a chain position above toekenning; the anchor nests it in toekenning's `excepties` | (i) — §5.2: inheritance-safe lowering, identical on every input | [M12] the twin's shape passes all ten cases on the landed conditions |
| herleving and blokkade nested as in the anchor | — | identical after G1 |
| group name `toekenning art 15` versus `recht op uitkering bij herleving`; `bron` texts | (i) | not compared (bron stripped) |
| literal `0,8750` with 3 decimals (product 5) versus the anchor's `"0.8750"` with 4 (product 6) | (i) — the lexeme and a literal-typing detail; the result is rounded to 2 decimals | [M] measured on the probe |
| unit `dag` with `canoniek: "dag"`, factor 1, `teken: "ONBEPAALD"` versus `null`/`null`/`null` | (i) — both spellings satisfy WF-092 | [M] measured on the probe |
| `recht op uitkering` bezittelijk | none: the anchor is bezittelijk since G1 | — |

Cases: the ten of G1, same inputs, same expectations. Gates: 2, 3, 4,
5, 6, 7, 10 (the re-emission of `bron.rgs`), 12 and 13.

### 8.3 G3 and G4 — two further twins

- **G3, `jutterswet-uitsluiting-rgs`, art. 4 Jutterswet** — the text
  RUNTIME-SEMANTICS ch. 7.1 uses as its nesting example ("geldt niet
  indien …, dat laatste geldt weer niet indien …"), with the bijvoeglijk
  negation. Diff partner: `anchor/jutterswet-uitsluiting` (flat chain,
  conditions written out, four cases). Declarations: the anchor's five
  bijvoeglijk characteristics on `Jutter`.

  ```
  Regel uitsluiting van het penningrecht
      geldig altijd
          Een Jutter is uitgesloten
          indien hij vergunning geweigerd is.

  Uitzondering op uitsluiting van het penningrecht: herroepen weigering
      geldig altijd
          Een Jutter is niet uitgesloten
          indien hij weigering herroepen is.

  Uitzondering op herroepen weigering: vervallen herroeping
      geldig altijd
          Een Jutter is uitgesloten
          indien hij herroeping vervallen is.

  Regel penninggerechtigd tenzij uitgesloten
      geldig altijd
          Een Jutter is penninggerechtigd
          indien hij niet uitgesloten is.
  ```

  Each block writes its own condition only (decision 4(d)); the frontend
  conjoins the carrier's, so the wire carries the anchor's cumulative
  conditions — `ALLE[vergunning geweigerd, weigering herroepen]` and
  `ALLE[vergunning geweigerd, weigering herroepen, herroeping vervallen]`,
  carrier conjuncts first. *Rejected (first proposed):* the conditions
  written out cumulatively in the source. Expected diff: the third layer
  nested in the second versus the anchor's flat chain — (i), identical
  order.
- **G4, `jutterswet-nestkenmerk-rgs`** — unordered exceptions on a base,
  one of them negative, with the nested characteristic conflict. Diff
  partner: `anchor/jutterswet-nestkenmerk` (three cases). Declarations:
  `nx`, `ny`, `nz` and `vrijgesteld`, bijvoeglijk, on `Jutter`.

  ```
  Regel vrijstelling van een jutter
      geldig altijd
          Een Jutter is vrijgesteld
          indien hij nx is.

  Uitzonderingen ongeordend op vrijstelling van een jutter:
  Regel vrijstelling vervalt bij ny
      geldig altijd
          Een Jutter is niet vrijgesteld
          indien hij ny is.

  Regel vrijstelling bij nz
      geldig altijd
          Een Jutter is vrijgesteld
          indien hij nz is.
  ```

  Expected diff: a group-default above the base versus the anchor's
  form-a base — (i), same decisions and the same conflict key
  `Jutter.vrijgesteld/0` ([M11] on the analogous probe). *Not decided:*
  decision 4(d) is written for `Uitzondering op`; whether the members of an
  `Uitzonderingen ongeordend op X:` group also inherit X's condition is
  open. The anchor's members carry their own condition only, and its three
  cases cannot tell the readings apart: [M] with `nz` and without `nx` the
  anchor answers present in ref, clingo and the exec branch; [I] a twin
  whose members inherit `nx` would answer absent there. The twin's
  `case1-y-wint` survives a no-op run as the anchor's does and joins the
  survivor list of `scripts/check-noop.sh` with the anchor's reason.

### 8.4 What the Drools branches carry

- **Exec-model branch: everything needed, today.** [M8] the anchor played
  4/4 on 8dd05e25 and plays 10/10 since its repair (§8.1); [M11] a
  characteristic group-default with unordered exceptions and a conflict
  plays 4/4; [M12] the twin's shape played 7/7 on 8dd05e25 and plays 10/10
  on the landed conditions. The branch lowers a
  defaults group to one reduce-rule whose walk recurses into nested groups
  and writes a characteristic only for a `true` decision
  (`ExecModelTranspiler.java:7543-7687`, `:7649`). Its fences do not touch
  the twins: no temporal target, no dated validity window (`geldig altijd`),
  no characteristic that shares its name with an attribute, and every
  justification is subject-local. No backend change is needed.
- **DRL-text branch: nothing, by design.** It refuses every document with
  `prioritizedDefaults` before any lowering (`DrlTranspiler.java:116-120`); the
  twins join class (A) of its non-player list (`check-drools.sh:826-861`)
  with the existing sentence "ch. 7 prioritizedDefaults: no defaults
  lowering in this branch", and `EXPECTED_DRL_PLAYED` does not move.
  Carrying them would need a defaults lowering in that branch — a card of
  its own for all 24 + 3 goldens, not for this plan.
- "Green in ref ≡ clingo ≡ Drools" therefore means: ref and clingo pass,
  the exec branch plays (gate 12's `EXPECTED_PLAYED` rises by the number
  of twins that land), the DRL-text branch lists them by name.

### 8.5 Order and gate touch-points

1. G1 (protected) first: G2's expectations are G1's.
2. The frontend card (§11) builds forms 1 and 2 and their fixtures.
3. The goldens card admits G2–G4: corpus directories (not protected), and,
   as proposals on its branch, `scripts/check-drools.sh`
   (`EXPECTED_PLAYED` + 3, class (A) + 3 names) and `scripts/check-noop.sh`
   (G4's survivor).
4. The main session lands the row-8 register text of the card's Result.

## 9. The card's byproducts

### 9.1 A `Regelgroep` ends at the first `Uitzondering op` (M5)

`regelGroep : REGELGROEP kaleNaam ( IS RECURSIEF )? groepLid+ ;` with
`groepLid : regel | beslistabel ;` (`RegelSpraak.g4:77-78`): the group
ends at the first construct that is not a rule, which includes an
`Uitzondering op` block. The exception and every rule after it fall
outside the group without a message, and because the group's base member
is lifted into a defaults group, the emitted document fails WF-033
(`IR-SPEC.md:2537-2552`). Options for its card: (a) refuse an extension
block inside a `Regelgroep` loudly — both are known to the frontend; (b)
admit extension blocks as members, which needs WF-033 to accept a defaults
group as a member and a statement of what a defaults group in a recursive
group means (a spec decision). Direction: (a), with (b) recorded.

### 9.2 `// Bron:` annotations vanish in defaults groups (M6)

`defaultsVan` and `defaultVan` set `bron: null` (`RuleChecker.java:331-351`).
Options: (a) carry each block's annotation text into its `Default.bron`
and the base's into the group's `bron` — the fields the hand anchors
already use for statute references ("art. 15: …"), no wire change; several
annotations on one block are joined; (b) a `bronAnnotaties` list on
`Default` and `PrioritizedDefaults`, an extension field under WF-082 — a
wire change, lossless. `PRIORITY-RELATION.md` §4.5 already places priority
provenance on `Default.bron`. Direction: (a). The code is the frontend
card's, so the card runs after it or inside it, never beside it.

### 9.3 `testDocument` and the werkbank tooling

`testDocument : ( definitie | regelGroep | regel | beslistabel |
situatieDefinitie | scenario )* EOF` (`RegelSpraak.g4:88-90`) lacks
`uitzonderingRegel` and `uitzonderingGroep`, so a scenario document with
defaults does not parse. This grammar is uchar's; regelspraak-java
vendors it (`vendor/uchar-frontend/` there). The scenario harness is
regelspraak-java's and must resolve scope names that now denote defaults
groups or exceptions rather than entries of `resolvedRegels`. There, too,
the playground graph, the explanation and the coverage iterate
`model.resolvedRegels` only (`RegelGraaf.java:63`, `:87`, `:105`;
`ProducentConsument.java:89`), so defaults families are invisible in the
werkbank. Direction: the two grammar alternatives with the uchar frontend
card; the rest in one regelspraak-java card.

### 9.4 Super- and subtype bare rules on one target (M7)

The probe: `Strandrecht` with `grondslag` and `uitkering`; `Wrakrecht is
een Strandrecht`; `De uitkering van een Strandrecht moet gesteld worden op
zijn grondslag.`; `De uitkering van een Wrakrecht moet gesteld worden op
zijn grondslag maal 2.`; no defaults group anywhere. A `Wrakrecht` with
grondslag 10,00:

| Executor | Strandrecht rule first in the source | Wrakrecht rule first |
|---|---|---|
| verifier | `verifier: PASS` | `verifier: PASS` |
| ref | `EVALUATION FAULT: no fixpoint within 100 passes — divergence is a fault (ch. 5.7)` | the same |
| clingo | `MISMATCH: W1.uitkering = [2000;1000], expected 2000` — two values | `[1000;2000]` |
| Drools exec | `FAIL …: geen fixpunt (kringverwijzing?)` | the same |
| Drools DRL-text | 20,00 (plays) | 10,00 (`verwacht 20.00, werkelijk 10.00`) |

A `Strandrecht` instance gives 10,00 everywhere.

**Root cause.** Two statements in the repository contradict each other.
The verifier's WF-079 comment: [Q, own code] "A bare rule on the supertype
and a bare rule on the subtype are not two writers: the subtype's rule IS
its base and replaces the inherited one" (`checks.ml:8899-8903`). The
normative text: [Q, own spec] "A rule is absorbed only on a target whose
CHAIN declares a defaults group somewhere. A bare rule with no group above
or below it is the ordinary ch. 7.8 standing norm and enters no stack at
all" (`RUNTIME-SEMANTICS.md:5179-5184`), implemented as the gate in
`desugar.ml:286-290`. Under the gate, a subtype instance has two
unconditional writers — the un-pinnable form WF-079 exists to refuse
(`IR-SPEC.md:2126-2142`) — and WF-079 does not see the pair because it
keys on the declared type. Options for its card, both on protected paths:
(a) fold a bare rule into its level's base also when no group exists,
whenever another level of the chain writes the same target — child wins,
the maintainer's 2026-09-12 principle ("definities op een kind winnen
altijd van definities op een parent", `SUBTYPING.md` §11.2); the
absorption rule the three executors share (RUNTIME-SEMANTICS ch. 7.10)
then covers the pair, and the verifier's comment becomes true; (b) keep
the gate and let WF-079 refuse the pair when neither rule is folded ("do
these two writers survive into one instance's effective stack?" — here
they do), correcting the comment. (a) follows the inheritance mandate;
(b) is the smaller fail-fast step.

## 10. Decisions

Each item states the proposal, the reason and the alternatives. **Decided
2026-09-23**: the cards of commit 85f58237 and the maintainer's decision
question of that day, on the recommendations of the research report
outside the repository
(`../research/dialect-defaults-niet-en-nesting/onderzoek-niet-in-uitzonderingen.md`
§9). Points 1, 3, 4(a)–(c), 5, 6 and 8 as proposed; point 7 as a card of
its own; points 2 and 4(d) otherwise, recorded below with the first
proposal as the rejected alternative (card
`voorstel-niet-en-nesting-besluiten`).

1. **The research outcome — decided as proposed.** "Niet" in
   an exception is a legitimate uchar-dialect extension and collides with
   no RegelSpraak design principle the project wants to keep. Kept: the
   closed world of RegelSpraak §3.5/§9.2 (an unattributed characteristic
   is false), the monotone write regime of RUNTIME-SEMANTICS ch. 5.3 (a
   negative decision writes nothing), and RegelSpraak §3.5's "input or
   attribution" (a negative decision never removes an input
   characteristic, §3.5 of this document). Given up,
   knowingly: SBVR/RuleSpeak's Accommodation and Wholeness for exceptions
   — already given up for `Uitzondering op` on 2026-08-29; "niet"
   completes that construct for characteristic targets, where today only
   attribute targets can reverse a lower default. *Reasons:* §3.1–3.4;
   Catala's `not fulfilled` on closed-world conditions; three executors
   carry it (M8, M11, M12). *Alternatives:* (b) keep §9.2 absolute inside
   the extension too, and keep the gate-characteristic workaround of the
   card's measurement 4 — it works, but it hides the statute's structure
   in a helper characteristic and in conjuncts, and it keeps the two target
   kinds asymmetric; (c) admit "niet" everywhere — decision 2.
2. **The reach — decided 2026-09-23, otherwise than first proposed.** In
   every rule that becomes a default: an `Uitzondering op` body, a member
   of `Uitzonderingen ongeordend` (with or without `op <naam>`), and the
   base of an `Uitzondering op` chain. Refused with a targeted message in a
   plain `Regel` that becomes no default. A group in which every default
   decides `onwaar` derives nothing — well-formed, it means "never" — and
   gets a RuleChecker WARNING (F9). *Reasons:* under the closed world a
   negated base decides `onwaar` and writes nothing, so the outcome does not
   change; what it buys is that the author can write the statute ("herleeft
   niet, tenzij …") and that the exceptions hang on a named base without an
   invented positive rule (Catala's `definition x equals false`). Outside
   the extension there is no writer with a priority, so §9.2 keeps holding
   for every plain `Regel`. *Rejected (first proposed here):* the base
   refused as well — only in blocks an extension header opens, so that §9.2
   would hold for every `Regel`, a base included.
   *Alternatives:* (b) a plain negative `Regel` as an unordered sibling of
   the positive rules on its target, with ch. 7.2 conflicts — Catala's own
   reading (Z1), but one sentence would change the meaning of the other
   plain rules on that target, and it contradicts §9.2 in the core
   construct; (c) only in `Uitzondering op` bodies — smaller, and the
   characteristic conflict stays hand-authored only.
3. **The spelling — decided as proposed.** The table of
   §4.1; resolution by the condition side's verbatim-first matcher; `is
   niet een X` admitted through one grammar alternative; the register lint
   as a WARNING; the messages of §4.5 replace measurement 1's.
   *Reasons:* one negation vocabulary for conditions and results; each kind
   keeps its verb; no new token and no reserved word. *Alternatives:* (b)
   only the three §8.1.8 prose forms, `is niet een` refused with a pointer
   to `is geen` — no grammar change, but conditions and results would
   differ; (c) one spelling, `niet`, for every kind — ungrammatical for a
   bezittelijk characteristic.
4. **Nesting — (a)–(c) decided as proposed; (d) decided 2026-09-23,
   otherwise than first proposed.** (a) *Form:* the existing header
   `Uitzondering op <naam>`, whose name may denote a base or an exception.
   (b) *Order:* among the children of one carrier the later-written block
   ranks higher, at every level. (c) *Depth:* no bound. (d) *The carrier's
   justification:* option (D) of §5.4 — `Uitzondering op X` is
   restrictive: the frontend conjoins X's (itself conjoined) justification
   with the exception's own condition, at every level including the
   first, flattened into one `ALLE` with the carrier's conjuncts first; a
   deviation that widens its base is written `Afwijking van X: <naam>`
   with its own condition only (`jutterswet-bergingsdrempel` becomes one,
   its `ir.json` byte-identical). *Lowering:* §5.2 — the first level a
   chain position, deeper levels `Default.excepties`. *Reasons for (d):*
   ch. 7.1 prescribes the conjunction; no formalism lets an exception
   inherit the condition (Catala does not, T4), so it has to be written
   somewhere, and the frontend does it mechanically and visibly on the
   wire; the legal meaning of an exception is restrictive; the anchor's
   corner B is the silent error of the other choice. *Rejected for (d):*
   (A), self-contained conditions written by the author with the warning
   F7 (first proposed here); (B) implicit conjunction at nested levels
   only; (C) an explicit `Uitzondering binnen <naam>` header (§5.4).
   *Alternatives* for (b) statute order; for (c) a bound; for the
   lowering, nesting the first level too — rejected on inheritance
   (§5.2).
5. **Inheritance — decided as proposed.** A nested exception
   under an inherited exception is refused, the message naming the
   expressible form (§5.5). *Reasons:* a subtype never edits an inherited
   exception, and the wire cannot address one. *Alternatives:* (b) copy
   the inherited exception into the subtype's own block with the nested one
   under it — an implicit copy that also moves the exception into the
   subtype-own block; (c) allow editing inherited exceptions — reopens
   PRIORITY-RELATION decision 1.
6. **Unordered exceptions under a rule — decided as proposed.**
   `Uitzonderingen ongeordend op <naam>:`, under an
   exception and under a base, lowered as in §5.2; a carrier with both
   ordered and unordered children is refused in the first card.
   *Reasons:* art. 21 lid 2 and lid 3 are parallel grounds; the IR and
   the three executors carry both variants (M11), and the Catala frontend
   already emits the variant under an exception.
   *Alternatives:* (b) under exceptions only, the Catala frontend's
   current scope; (c) no unordered nesting; for mixed children, the
   group-default at its writing position in the carrier's child chain
   (recorded, not proposed).
7. **Form 3 — decided: a card of its own,**
   `20260923-kenmerkinvoer-en-invoer-boven-berekening`, which weighs its
   own options (7-i to 7-iii); the directions this proposal gave are
   input to that card, not decided. First proposed here: a card
   `20260923-invoer-boven-berekening`, with §6's directions: per target;
   sugar over the hardship pattern first; the deciding block's name as the
   explanation; a materialised computed value plus a consistency rule for
   contradictions; case input stays Reading A. *Alternatives:* (b) inside
   this card's build cards — it would mix a semantic axis into a
   frontend-only change; (c) no surface at all — §6's pattern stays the
   route.
8. **regelspraak-java — decided as proposed.** Cards of
   their own there: the pin-bump that vendors the new frontend (its
   exec-model transpiler already lowers `besluit` and nested `excepties`,
   `ExecModelTranspiler.compileDefault`), and the scenario, graph and
   explanation card of byproduct 3. The two `testDocument` alternatives
   land with the uchar frontend card, because the grammar is the vendored
   slice. The drafts sit in this repository's `backlog/new/`, marked for
   transfer. *Alternative:* (b) include the regelspraak-java work in the
   uchar build cards — one card would then span two repositories and two
   pin disciplines.

## 11. Cards

*Superseded at landing (2026-09-23): the maintainer cut the follow-up cards
himself in commit 85f58237 — eight cards in `backlog/new/` carrying his
decisions on 1–8. The drafts listed below were removed at landing; only
`20260923-wf022-waardeclausule-getuige` survived, having no counterpart.*

Build cards (concepts in `backlog/new/`, each opening with "wacht op de
besluiten 1–8 van spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md"):

- `20260923-dialect-niet-nesting-frontend.md` — grammar (two
  alternatives plus the two `testDocument` alternatives), RuleChecker,
  `DIVERGENCES.md`, fixtures F1–F8, the inertness sweep;
- `20260923-dialect-niet-nesting-goldens.md` — G2, G3, G4 and their gate
  pins;
- `20260923-anker-ww-herleving-hoeken.md` — G1, the anchor repair
  (protected path);
- `20260923-wf022-waardeclausule-getuige.md` — the negatief of §7.3 (the
  "eventually verifier" card: witness only, no verifier code);
- `20260923-regelspraak-java-dialect-pinbump.md` — the follow-up in
  regelspraak-java (to be moved there);
- `20260923-invoer-boven-berekening.md` — form 3.

Byproduct cards, one per byproduct of the card:

- `20260923-regelgroep-uitzondering-afsluiting.md` (§9.1);
- `20260923-bron-annotaties-defaults.md` (§9.2);
- `20260923-testdocument-uitzonderingsvormen.md` (§9.3);
- `20260923-keten-kale-regels-divergentie.md` (§9.4).

## 12. Open points and what was not found

- *Not reached:* the Dutch government rule-register page on RuleSpeak
  (regels.overheid.nl) returned a DNS failure; nothing in this document
  depends on it. RegelSpraak's own §2.1 is the source for its lineage.
- *Not found:* a negative derivation form in the RuleSpeak Sentence Forms
  v2.2 and in the Dutch Zinsstructuur v2.3; a Dutch counterpart of "is
  never considered"; a discussion of negated definitions in Ross (2006);
  a priority-based exception mechanism in SBVR 1.5 (§16.4 knows
  accommodation and authorisations only — a finding, not a gap).
- *Inferences, not sources:* the second and third reasons of §3.1 are
  inferences; RegelSpraak states only the closed-world reason. ALEF and the
  Blaze rule base were not examined.
- *Read, not run:* the regelspraak-java scenario harness and graph.
- *A modelling observation for the werkbank's rebuild of art. 21* [I]:
  Regeling herlevingstermijn WW art. 1 disapplies the six-month term ("De
  termijn van zes maanden … blijft buiten toepassing"). Modelled as a
  positive exception nested under lid 3, it would decide "herleefd" and
  override lid 2's block wherever lid 3 ranks above lid 2, or conflict with
  it where the two are unordered. The text points at the term, so the
  faithful model is a defaults group on the term's own target (for
  instance a characteristic "zesmaandstermijn van toepassing": base waar,
  exception "niet" for the werknemer of art. 7 RVVSZ), read by lid 3's
  condition — which needs form 1 as well.
- *Observed, not investigated:* during probe M11 a group-default written
  without the `besluit` key was refused by the verifier, ref and clingo
  ("missing field 'besluit'") and accepted by the Drools loader; with the
  key present (`null`, as the anchors write it) all four agree.
- *The one decision with a real trade-off* was 4(d); it is decided
  (§10), and G1, needed under every option, has landed (§8.1).

## Sources

- Merigoux, D., Chataing, N., Protzenko, J. (2021). *Catala: A
  Programming Language for the Law.* Proc. ACM Program. Lang. 5 (ICFP),
  article 77. arXiv:2103.03198v2, §3.2, §4.1 (Table 1), §4.2, §4.4.
  https://arxiv.org/abs/2103.03198
- The Catala book, "Catala-specific questions", section "When to choose
  `condition`, `rule` *vs* booleans?".
  https://book.catala-lang.org/en/4-2-catala-specific.html — with
  "Definitions and exceptions",
  https://book.catala-lang.org/en/5-4-definitions-exceptions.html, and
  "Conditional definitions and exceptions",
  https://book.catala-lang.org/en/2-2-conditionals-exceptions.html
  (consulted 2026-09-23). Probes run on the catala compiler 1.2.1.
- Ross, R. G. *RuleSpeak® Sentence Forms: Specifying Natural-Language
  Business Rules in English*, version 2.2. Business Rule Solutions, LLC,
  © 2001–2009 (Basic Concepts; Tables 1 and 2).
  https://www.rulespeak.com/en/RuleSpeak%20Sentence%20Forms%20v2-2-3.pdf
- Ross, R. G. (2006). "Concepts, Definitions, and Rules: RuleSpeak®
  Practices." *Business Rules Journal* 7(5).
  https://www.brcommunity.com/articles.php?id=b289
- Ross, R. G. (2009). "When is an Exception Really an Exception? The
  Business Rule Principles of Accommodation and Wholeness." *Business
  Rules Journal* 10(12). https://www.brcommunity.com/articles.php?id=b511
- Ross, R. G. (2010a). "What Is a Business Rule?" *Business Rules
  Journal* 11(3). https://www.brcommunity.com/articles.php?id=b525
- Ross, R. G. (2010b). "Business Rules vs. System Design Choices."
  *Business Rules Journal* 11(7).
  https://www.brcommunity.com/articles.php?id=b544
- Spreeuwenberg, S. (2009). *RuleSpeak® Zinsstructuur — Het opstellen van
  bedrijfsregels in begrijpelijk Nederlands*, versie 2.3 (Dutch version of
  Ross's Sentence Forms), LibRT B.V.
  https://www.rulespeak.com/nl/RuleSpeak%20Zinsstructuren%20v2-NL5.pdf
- Object Management Group (2019). *Semantics of Business Vocabulary and
  Business Rules (SBVR)*, version 1.5, formal/2019-10-02: §16.3.2,
  §16.3.3 and §16.4.1 (p. 103), §24.2.1.3 (pp. 226–231), Annex A.2.1.2
  and A.2.1.3 (pp. 273–274). https://www.omg.org/spec/SBVR/1.5/PDF
- RegelSpraak-specificatie v2.3.0 (Belastingdienst): §2.1, §3.5, §8.1.8
  with Tabel 16, §9.2, §12 — paraphrased, not quoted, not redistributed.
- Werkloosheidswet (BWBR0004045) art. 15, 20 and 21, and Regeling
  herlevingstermijn WW (BWBR0016001) art. 1, as quoted in the card and in
  `anchor/ww-herleving-inkomen/NOTE.md` (statute text, art. 11
  Auteurswet).
