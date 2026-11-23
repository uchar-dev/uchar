# STRICT-EMPTY — a reading of leeg from which nothing follows

*Status: PROPOSAL · working prose, not normative. The card is
`backlog/done/20261121-strict-empty-lowering.md`. This document is the design the cards of the executors refer to: it
fixes, form by form, what the strict reading of leeg is, with worked examples, and it gives the decisions and the open
points. It changes no spec, schema, executor, frontend or corpus file. The reading was measured with a trial: a
transformation from a document to a document (`verifier --strict-empty`), its witnesses, a golden in the strict
reading and three checks (monotonicity, preservation with complete knowledge, and a comparison on a rule set outside
the repository). The trial stands on the
branch `card/strict-empty-lowering` and does not land on `main` (§3). Where this document names a witness, it is a
document of that branch with case files whose expectations were computed by hand. Every example is own work in the
domain of the Jutterswet.*

*The reading this document designs is normative in RUNTIME-SEMANTICS ch. 1.5, with one change of the owner's decision
of 2026-11-23 (card `one-empty`, part 2): a test on leeg or gevuld, the filled-value count and a fold filter that
tests on gevuld are undetermined or open over an open value, for every form, and an initialisation is the lowest
writer of its target and no closure. Every judgement is one value of Kleene's strong table; the pair "for the form
that wrote it / for another form" and the closures the trial made are not part of it, and only `(volledig gegeven)` on
an input member closes. §5 and §7.4 give that reading, with the outcomes the witnesses under
`ref/test/fixtures/strict-empty/` pin; the other sections keep the trial's closures where they name one (§1, §6, §7.1,
§8, §9, and the measurements and decisions of §10–§12), and where an example there rests on one, ch. 1.5 and those
witnesses give its outcome.*

## 1. The rule in three sentences

1. **A value is filled, settled leeg, or open.** Settled leeg means it is established that there is no value: an input
   the rule set treats as complete and that the case leaves leeg, a read over a link that is absent, a derived
   attribute that no writer fills. Open means it is not established: an input the case leaves leeg, or a derived
   attribute whose writers are not settled.
2. **What depends on an open value is open, unless the answer stands without it.** Where every value an expression or
   a condition reads is filled or settled leeg, it is read as the ordinary reading reads it (RUNTIME-SEMANTICS ch.
   1.2–1.4: a settled leeg operand counts as 0 in a plus, a fold skips it, a comparison with it is false). One open
   value makes a computation open and a condition undetermined, unless another part decides it: "A and B" is false as
   soon as A is false; "A or B" is true as soon as A is true.
3. **Who wants to go on without a given says so in the rules:** with a test on leeg in the form that goes on ("indien
   zijn gewicht leeg is"), with an initialisation, with an opgave, or by treating the input as complete (§9).

### 1.1 Two requirements

The reading is held to two requirements, and both are measured (§10.2, §11.3):

1. **Monotonicity.** Filling an input can add an outcome, never change or withdraw one that stood. An outcome is a
   value, a characteristic that holds, a characteristic established not to hold, a settled leeg attribute, a §9.5
   fact. The forms that close the world are the named exceptions: they are where an outcome may rest on an open
   value. Measured over the witnesses, the golden and the 228 corpus goldens that the trial transforms: every
   difference that emptying one input member makes falls under a named closure, or is an input beside an undetermined
   writer, or a fault both readings give (§11.3).
2. **Preservation with complete knowledge.** Where nothing is open — every input is filled or treated as complete —
   the strict reading gives the outcome of the ordinary reading (RUNTIME-SEMANTICS ch. 1.2–1.4). Measured: with every
   input member closed, 582 of the 593 cases of the 228 transformed corpus goldens play their own expectations; the
   11 others are four limitations of the trial, none a case where the strict reading itself differs (§10.2). On the rule
   set outside the repository of §10.4, the version with every input member closed gives the ordinary reading of the
   same version in 248 of 265 situations; in the other 17, 19 members stay open, all on an object their writers do not
   reach (§12.18).

## 2. The states of a value

An attribute of an object is in one of three states; a characteristic of an object likewise holds, is established not
to hold, or is open (§5.6).

| state | an input attribute (no form writes it) | a derived attribute (a form writes it) |
|---|---|---|
| **filled** | the case gives a value | the case gives a value, or a writer decides: its condition is established true and its value filled |
| **settled leeg** | the attribute is treated as complete input (§9) and the case gives no value; or it is read over a link that is absent (§5.8) | every writer is established not to apply, or applies with a settled leeg value (§5.6a); or it is read over a link that is absent |
| **open** | the case gives no value, and the input is not treated as complete | otherwise: a writer's condition is undetermined, or it holds with an open value |

- **undetermined**: the third truth value of a condition. It is not a value a rule writes: a rule fires on true and
  writes nothing on false or undetermined.
- An expression whose reads are all filled or settled leeg is settled: it computes as the ordinary reading does, and
  its outcome is filled or settled leeg. A leeg outcome by a fault on settled operands (a division by 0, a root of a
  negative) is settled leeg, with its §9.5 deposit as today.

## 3. How a document carries the choice

The choice is one rule of the rule set, a yes/no switch named **"Afleiden uit leeg"**. `Afleiden uit leeg: nee` is the
reading of this proposal; `Afleiden uit leeg: ja`, or no such rule, is the reading of RegelSpraak as uchar mirrors it
today (RUNTIME-SEMANTICS ch. 1.2). An existing document therefore does not change. A RegelSpraak source carries

```
Afleiden uit leeg: nee
```

once, beside its declarations (`frontends/regelspraak/grammar/DIVERGENCES.md`, "Afleiden uit leeg"), and the wire
carries the property `inferFromEmpty: false` on the root of the document (IR-SPEC ch. 2). The switch stands on `main`;
the reading itself does not yet: until an executor applies it, ref, clingo and the Drools backend refuse a document
with the property by name (REF-014, CLINGO-049, DROOLS-370).

**The route** (owner, 2026-11-21). The wire carries the document as the author wrote it, with the property, and every
executor applies the strict reading in its own lowering. The rewritten document does not stand on the wire, and no
executor reads a generated form as a form of the author.

**The trial.** To measure the reading before any executor changes, the card wrote it as a transformation from a
document to a document that uses only existing nodes (§5): generated characteristics, rules and groups beside the
author's forms. The transformed document was played in the unchanged executors. That transformation is a means of
measurement, not code an executor will use, and it stays on the branch `card/strict-empty-lowering`, with its
witnesses, the golden `jutterswet-strikt-leeg` (which carries a transformed document), and the scripts of §10–§11. A
builder of an executor reads there how each form was written in existing nodes, and can hold his own lowering to the
witnesses and their hand-computed cases. §12.15 says what the landing asks per part.

## 4. Tables

`W` true, `O` false, `?` undetermined.

| and | W | O | ? | | or | W | O | ? | | not | |
|---|---|---|---|---|---|---|---|---|---|---|---|
| **W** | W | O | ? | | **W** | W | W | W | | W | O |
| **O** | O | O | O | | **O** | W | O | ? | | O | W |
| **?** | ? | O | ? | | **?** | W | ? | ? | | ? | ? |

**The composite condition** (RUNTIME-SEMANTICS ch. 5.10), with *h* members established true, *f* established false and
*u* undetermined, *n* = *h* + *f* + *u*:

| quantifier | true when | false when |
|---|---|---|
| `all` (and `the`) | *f* = *u* = 0 | *f* ≥ 1 |
| `atLeast` *k* | *h* ≥ *k* | *f* ≥ *n* − *k* + 1 |
| `none` | *h* = *u* = 0 | *h* ≥ 1 |
| `atMost` *k* | *f* ≥ *n* − *k* | *h* ≥ *k* + 1 |
| `exactly` *k* | *h* ≥ *k* and *f* ≥ *n* − *k* | *h* ≥ *k* + 1, or *f* ≥ *n* − *k* + 1 |

The same table holds for a quantifier fold over the members of a set (§8.1), with "member" for "part".

**Comparisons.** A comparison with an open operand is undetermined, `leeg = leeg` and `leeg ≠ 5` included. With both
operands filled or settled leeg the comparison is two-valued and reads as the ordinary reading does (RUNTIME-SEMANTICS
ch. 1.2): an ordering with a leeg side is false, `≠` with exactly one leeg side true, numeric `=` on two leeg sides
true. Where both sides are filled, its complement is the complementary operator:

| operator | datatypes | complement |
|---|---|---|
| `less` | number, percentage, date | `greaterOrEqual` |
| `lessOrEqual` | number, percentage, date | `greater` |
| `greater` | number, percentage, date | `lessOrEqual` |
| `greaterOrEqual` | number, percentage, date | `less` |
| `earlier` | date (one precision) | `laterOrEqual` |
| `earlierOrEqual` | date (one precision) | `later` |
| `later` | date (one precision) | `earlierOrEqual` |
| `laterOrEqual` | date (one precision) | `earlier` |
| `equal` | every datatype: number, percentage, text, boolean, enumeration, date, sum type, record | `notEqual` |
| `notEqual` | every datatype | `equal` |

The ordering of a number, a percentage and a date of one precision is total; equality of a sum type is structural
(ch. 9.3), of a record field by field (ch. 11.2). Where a
side is settled leeg, the operator and its complement can both be false in the ordinary reading; the leeg side then
belongs to the established false side (§5.3).

**Operators.** Every operator passes an open operand on: plus, min, maal, percentage van, verminderd met, gedeeld door
(both variants), tot de macht, wortel, absolute waarde, afronding, begrensd (its bounds included), a date plus or minus
a duration (the duration included), the parts of a date, a text part, and the listed `eerste`/`laatste`/`minimum`/
`maximum` of two or more operands. A settled leeg operand is read as the ordinary reading reads it: 0 in a plus, min,
maal and percentage van, no bound in begrensd, leeg in the others; a division by a settled leeg divisor is leeg with its
EF-031 deposit, by 0 leeg with its EF-005 deposit (RUNTIME-SEMANTICS ch. 1.4).

## 5. The reading per form

The trial wrote the strict reading as a transformation into existing nodes. For a condition `c` it builds two
conditions: `c+` (established true) and `c-` (established false). For an expression `e` it builds `d(e)`: the
conditions under which every value `e` reads **stands** — is filled or settled leeg. The expression itself stays: where
`d(e)` holds, both readings compute the same value, leeg included. An executor that applies the reading in its own
lowering needs the same `c+`, `c-` and `d` per form; it need not write them as nodes.

In the examples an input written "leeg" is one the case gives no value: it is open, unless it is declared complete
(§9), and then settled leeg. An outcome is written **open** where it does not stand, and settled leeg where it stands
that there is no value, as the case files of the witnesses write `attributeOpen` and `attributeEmpty`.

**One judgement, two walks.** The trial built `c+` and `c-` in two kinds of walk:

- the **own walk** builds the condition on which a form decides itself: `c+` of a rule, a default and a creation, and
  `a-` of a consistency rule (§5.9);
- the **other walk** builds the opposite of a condition for another form: the negation `K (niet)` of a characteristic
  (§5.6), "a derived attribute is settled leeg" `A (leeg)` (§5.6a), "no higher default decides" (§7.1), "no own
  exception" (§7.5), and a shared condition built from them.

In the trial the two differed in one place, the side of a test that says "X is leeg", which the own walk read as a
closure over an open X. In the reading of RUNTIME-SEMANTICS ch. 1.5 there is no such closure (§5.4), so both walks build
the same conditions: one judgement per condition, one value of Kleene's strong table. Inside a condition both
polarities occur: `c+` of "geen van: X is gevuld" is `c-` of "X is gevuld".

### 5.1 d(e): what an expression depends on

`d(e)` is a list of conditions, deduplicated:

- an attribute read contributes "the read is filled, or it reads a derived attribute that is settled leeg" (§5.6a); a
  read over a path also "a link of the path is absent" (§5.8); a cell read the gevuld test of the cell;
- a literal, a session value (`evaluationDate`, `evaluationYear`), a scalar parameter and a member the caller closes
  (§9) contribute nothing;
- a fold over a set contributes a quantifier fold over the same range (§8.2);
- a binder of a `Let` (ch. 10.6) contributes the `d` of its value, where the body reads it;
- every other node contributes the conditions of its operands. A node that is leeg on standing operands — a division
  by 0 or by a settled leeg divisor, a root of a negative, a fold over no contributor — adds nothing: its outcome is
  settled leeg, with the deposit the ordinary reading makes.

Example (witness `arithmetic`): "het brutogewicht van een Vondst is zijn gewicht plus zijn schade". With gewicht 10,00
and schade leeg (an input, open) the ordinary reading writes **10,00**; the transformed rule carries the condition
"zijn gewicht is gevuld en zijn schade is gevuld" and does not decide: brutogewicht **open**. "Het gewicht per getuige
is zijn gewicht gedeeld door zijn aantal getuigen" with aantal getuigen leeg: the ordinary reading deposits an EF-031
fact; the transformed rule does not fire, the gewicht per getuige is **open** and the channel stays **consistent**.
"Zijn gewicht, met een minimum van zijn korting en met een maximum van zijn schade" with schade leeg: ordinary
**10,00** (a leeg bound is no bound), transformed **open**.

Example (witness `empty-by-fault`): "een Vondst is afwijkend indien zijn gewicht gedeeld door zijn aantal getuigen
ongelijk is aan zijn schatting" and "het totaal is (zijn schatting plus zijn gewicht gedeeld door zijn aantal getuigen)
naar beneden afgerond op 2 decimalen". With gewicht 10,00, aantal getuigen 0 and schatting 4,00 every read stands: the
division is settled leeg with its EF-005 deposit, and both readings attribute afwijkend (leeg ≠ 4,00) and write totaal
**4,00** (the plus counts the leeg as 0). With aantal getuigen leeg the division is open: afwijkend and the totaal
are **open**, and nothing is deposited (ordinary: afwijkend, 4,00 and an EF-031 per rule). "Een
Jutter is lichtvoetig indien de minimale waarde van het gewicht van alle vondsten van de Jutter ongelijk is aan 5,00"
over a jutter without vondsten: the minimum over no member is settled leeg, and both readings attribute
**lichtvoetig**.

Example (witness `settled-empty`), with "het vindloon van een Vondst is 10 indien hij kostbaar is" (kostbaar: zijn
taxatie groter dan 1000,00), "het bergloon is 3" and "de uitbetaling is zijn vindloon plus zijn bergloon": with taxatie
50,00 it is established that the vondst is not kostbaar, so the vindloon is settled leeg and the uitbetaling is **3**,
as in the ordinary reading; with taxatie leeg the vindloon is open and the uitbetaling **open** (ordinary: 3). "Het
extra vindloon is zijn vindloon" passes the settled leeg on: with taxatie 50,00 the extra vindloon is settled leeg too,
and "de extra uitbetaling is zijn extra vindloon plus 1" is **1**.

### 5.2 A rule

A rule (an equation, a calculation, a characteristic attribution) fires on `c+` and `d(value)`: its condition becomes
the conjunction of `c+` and `d(value)`. A rule without a condition and without a read in its value is unchanged. An
initialisation keeps its place as the lowest writer of its target (§7.4); its own condition becomes `c+` and
`d(value)`.

### 5.3 A comparison

An atomic condition — a comparison, the exact-digit test, everything else that reads values — is read as the
ordinary reading reads it where all its reads stand, and that outcome is established: `c+` is `d(left)`, `d(right)`
and the comparison; `c-` is `d(left)`, `d(right)` and "the comparison is false in the ordinary reading". With both
sides filled, that is the complementary operator (§4). A side that can be settled leeg — a derived attribute, a read
over a link, a member the caller closes, a fold or a division — adds what the ordinary reading makes false on both the
operator and its complement:

| operator | one side leeg: ordinary outcome, belongs to | two sides leeg: ordinary outcome, belongs to |
|---|---|---|
| `less`, `lessOrEqual`, `greater`, `greaterOrEqual`, `earlier`, `earlierOrEqual`, `later`, `laterOrEqual` | false, `c-` (written: the side is leeg) | false, `c-` (written: either side is leeg) |
| `equal` over a number or a percentage | false, `c-` (through `notEqual`) | true, `c+` |
| `equal` over any other datatype (text, enumeration, boolean, date, sum type, record) | false, `c-` (through `notEqual`) | false, `c-` (written: both sides are leeg) |
| `notEqual` over a number or a percentage | true, `c+` | false, `c-` (through `equal`) |
| `notEqual` over any other datatype | true, `c+` | false, `c-` (written: both sides are leeg) |

Where the datatype is not known, the both-sides row adds nothing to `c-`, which leaves that case open (the safe side).
`c-` stays positive: the trial writes it with the complementary operator and leeg tests of the sides, not as the
negation of the comparison, which would read a characteristic in a fold filter of a side negatively.

Example (witness `comparison`): "een Vondst is zwaar indien zijn gewicht groter is dan 100,00" and "de lichttoeslag is 1
indien hij niet zwaar is". With gewicht leeg (an input, open) the ordinary reading writes lichttoeslag **1** (not zwaar,
because the comparison is false on leeg); in the transformed document the lichttoeslag is **open**: zwaar is
undetermined, so "niet zwaar" is undetermined too. With gewicht 40,00: **1** in both. "De gelijktoeslag is 3 indien zijn
gewicht gelijk is aan zijn schatting" with both leeg: ordinary **3** (numeric equality holds on two empties),
transformed **open**.

Example (witness `settled-empty`): "een Vondst is duur indien zijn vindloon groter is dan 5", read as "niet duur" by a
toeslag. With taxatie 50,00 the vindloon is settled leeg, the comparison is false and established: `duur (niet)`, as
the ordinary reading has it; with taxatie leeg it is undetermined.

### 5.4 A leeg or gevuld test

A test "X is leeg" or "X is gevuld" reads the state of X (RUNTIME-SEMANTICS ch. 1.5, "The test on leeg and gevuld"):

| state of X | `X is gevuld` | `X is leeg` | `X moet gevuld zijn` |
|---|---|---|---|
| filled | true | false | holds |
| settled leeg | false | true | reports |
| open | undetermined | undetermined | reports nothing |

The test asks whether a value is there, and of an open value that does not stand: over an open X it is undetermined,
whichever form reads it, and so is a negation of the author's own ("geen van: X is gevuld"). The trial kept one
closure here, "X is leeg" true over an open X in the own decision condition of the form that wrote the test; ch. 1.5
does not. X is settled leeg where it is an input member declared complete (§9) that the
case gives no value, a read over a link that is absent (§5.8), or a derived attribute that no writer fills (§5.6a);
there both tests decide as the ordinary reading does. Where the author wants the test to decide over a value the case
does not give, the author declares the input member complete.

| rule, with X leeg | X open | X settled leeg |
|---|---|---|
| "K indien X gevuld is en X groter is dan 1000" | K undetermined | `K (niet)` |
| "K indien X leeg is" | K undetermined | K holds |
| an exception "50 indien X gevuld is en X groter is dan 100" above a base 10 | the exception does not decide, and the base does not either: the target is open | the base decides: 10 |
| a consistency rule "X moet gevuld zijn" | reports nothing | reports |

Example (witness `settled-empty`): "een Vondst is beloonbaar indien zijn vindloon gevuld is", read as "niet
beloonbaar" by a wachttoeslag 4. With taxatie 50,00 the vindloon is settled leeg: `beloonbaar (niet)` and wachttoeslag
**4**, as in the ordinary reading; with taxatie leeg the vindloon is open: beloonbaar undetermined, wachttoeslag
**open** (ordinary: 4). "Een Vondst is beloond indien zijn vindloon gevuld is en zijn vindloon groter is dan 5" reads
the same way.

Example (witness `filled-test-not-closure`), with the input taxatie and the derived schatting (zijn waarde maal 2), each
read by the four forms above (thresholds 1000,00 and 100,00 for taxatie, 100,00 and 10,00 for schatting):

| case | kostbaar (taxatie gevuld en > 1000,00), bewakingstoeslag 5 if niet kostbaar | ongetaxeerd (taxatie leeg) | vindloon | consistency on taxatie |
|---|---|---|---|---|
| taxatie leeg | undetermined, toeslag **open** (ordinary: 5) | **undetermined** | **open** (ordinary: 10) | reports nothing |
| taxatie 2000,00 | holds, toeslag settled leeg | `ongetaxeerd (niet)` | 50 | consistent |
| taxatie 50,00 | `kostbaar (niet)`, toeslag 5 | `ongetaxeerd (niet)` | 10 | consistent |

The same rules over schatting give the same columns where waarde is leeg (the schatting is open), 60,00 (schatting
120,00) and 2,00 (schatting 4,00).

**A derived member and a computed value.** Over a member a form writes, the test reads the member's state as any read
does: undetermined while the member is open, as the ordinary reading where it stands. A test of a computed value reads
the value in the strict reading: gevuld is `d(value)` and the test, leeg is `d(value)` and the leeg test, so both are
undetermined where the evaluation has an open read, and a division is evaluated only on standing operands.

Example (witness `composite-filled-test`): "de getuigentoeslag is 7 indien zijn gewicht gedeeld door zijn aantal
getuigen leeg is", "de schadetoeslag is 3 indien zijn gewicht plus zijn schade gevuld is", and "het vindloon is 1", by
a deviation 9 under the same leeg test. With gewicht leeg, aantal getuigen 2 and schade 1,00 the ordinary reading writes
getuigentoeslag **leeg** (0 / 2 is gevuld), schadetoeslag **3** and vindloon **1**; the strict reading leaves all three
**open**. With aantal getuigen leeg the ordinary reading deposits EF-031 for both forms; the strict reading leaves the
getuigentoeslag and the vindloon open, writes the schadetoeslag 3 and stays **consistent**. With aantal getuigen 0 both
readings deposit EF-005 and write 7: a division by 0 of filled operands is settled leeg, with its fault, in the strict
reading too.

**The same rule elsewhere.** A filled-value count (§8.2) is open where a member's value is open, and a fold filter that
tests on gevuld is undetermined for such a member, so the fold is open (RUNTIME-SEMANTICS ch. 1.5, "A fold over a
set"). The opgave is no test: it reads as any default over an input member (§7.4).

Example (witness `filled-test`): "een Vondst is vrijgesteld indien zijn gewicht leeg is of kleiner dan 10,00" and "een
Vondst is licht indien zijn gewicht kleiner is dan 10,00". With gewicht leeg: vrijgesteld is **undetermined** (both
members are), licht is **undetermined**, so "de vrachttoeslag is 3 indien hij niet licht is" leaves the vrachttoeslag
**open** where the ordinary reading writes **3**.

### 5.5 The composite condition

Per §4, written with `all` and `atLeast` alone:

| quantifier | `c+` | `c-` |
|---|---|---|
| `all`, `the` | `all` [parts+] | `atLeast 1` [parts−] |
| `atLeast` *k* of *n* | `atLeast k` [parts+] | `atLeast n−k+1` [parts−] |
| `none` | `all` [parts−] | `atLeast 1` [parts+] |
| `atMost` *k* of *n* | `atLeast n−k` [parts−] | `atLeast k+1` [parts+] |
| `exactly` *k* of *n* | `all` [`atLeast k` [parts+], `atLeast n−k` [parts−]] | `atLeast 1` [`atLeast k+1` [parts+], `atLeast n−k+1` [parts−]] |

A threshold of 0 or less is true without a part (`all` over no part); a threshold above *n* is false without reading a
part (ch. 5.10). A part that is never established (§5.4) is left out of an `atLeast`, and makes an `all` false; the
trial writes false as "none of [true]".

Examples, each with gewicht > 100,00 as A and schatting > 1000,00 as B:

- `all` (witness `compound-all`, with gewicht > 10,00 and schatting > 100,00): gewicht 5,00 and schatting leeg →
  kostbaar **established false** (A is false); gewicht leeg and schatting 200,00 → **undetermined**, so "de bergtoeslag
  is 7 indien hij niet kostbaar is" leaves the bergtoeslag **open** where the ordinary reading writes **7**.
- `atLeast 1` (witness `compound-any`): gewicht 150,00 and schatting leeg → opvallend **holds** (A is true); gewicht
  50,00 and schatting leeg → **undetermined**.
- `none` (witness `compound-none`): gewicht 150,00 and schatting leeg → "geen van" is **established false**; gewicht
  50,00 and schatting leeg → **undetermined**: the vrijstelling is **open**, where the ordinary reading writes **1**.
- the counting quantifiers (witness `compound-counting`, with lengte > 2,00 as C): with A true, B true, C leeg,
  "ten minste twee" **holds**, "ten hoogste één" and "precies één" are **established false**; with A true, B false, C
  leeg, all three are **undetermined**; with A false, B false, C leeg, "ten minste twee" is **false**, "ten hoogste
  één" **holds** and "precies één" is **undetermined**.

### 5.6 A characteristic and its negation

For every characteristic `K` that the document reads negatively — a check of `K` in `c-`, or a negated check of `K` in
`c+` — the transformation declares a generated characteristic **`K (niet)`** on the type that declares `K`: it is
established that `K` does not hold. A check of `K` stays: its `c+` is the check, its `c-` the check of `K (niet)`; a
negated check of `K` has `c+` the check of `K (niet)` and `c-` the check of `K`. The transformed document reads no
characteristic negatively. A navigated check adds the absent link to its `c-` (§5.8).

`K (niet)` is attributed by one generated rule per type `U` that attributes `K`, named **`K (niet) van U`**: it holds
where the condition of every attribution of `K` on `U` is established false (the conjunction of their `c-`). Where a
defaults group decides `K`, a generated group decides `K (niet)` (§7.3). A characteristic that no form attributes gets
`K (niet)` without a rule: a case says "onwaar" by naming `K` in `characteristicsAbsent` (RUNTIME-SEMANTICS ch. 1.5,
"The case"; the trial's case files named `K (niet)` among the characteristics), and a case that names it in neither
list leaves the characteristic undetermined.

**The name.** `K (niet)` and `K (niet) van U` are built from the declared spelling of `K` and of `U`. No frontend writes
a name with a parenthesis; a declared name that spells the generated one anyway (also under the property names of the
Drools backend, which drop the parenthesis) is refused by name (`generated-name-collision`). The name is stable: it
depends on the declared names alone.

Example (witness `characteristic`): "een Vondst is aangespoeld indien zijn gewicht groter is dan 50,00", "een Vondst is
aangespoeld indien hij drijfhout is", "de drijftoeslag is 2 indien hij niet drijfhout is" and "de strandtoeslag is 3
indien hij niet aangespoeld is"; drijfhout is attributed by no form. With gewicht 20,00:

| the case names | drijftoeslag | strandtoeslag | aangespoeld |
|---|---|---|---|
| nothing | **open** (ordinary: 2) | **open** (ordinary: 3) | undetermined |
| `drijfhout` in `characteristicsAbsent` | **2** | **3** | `aangespoeld (niet)` holds |
| `drijfhout` in `characteristics` | settled leeg | settled leeg | holds |

With gewicht 80,00 and nothing named, aangespoeld **holds** without drijfhout.

### 5.6a A derived attribute that no writer fills

A derived attribute is settled leeg on an object where the case gives it no value and every writer that can write it
there is established to fill nothing, the inherited ones included:

- a **rule** (an equation, a calculation, an initialisation) with condition `c` and value `v` fills nothing where `c-`
  holds, or where `c+` holds and `v` is settled leeg (`d(v)` and the value leeg): a writer that fires with a leeg value
  writes nothing (RUNTIME-SEMANTICS ch. 1.2);
- a **defaults stack** fills nothing where every default is established not to apply or applies with a settled leeg
  value, and the stack has no initialisation or its initialisation's value is settled leeg (§7.1);
- an **object creation** assigns the attribute on the object it makes, as the input of that object (ch. 7.8): it is
  filled or settled leeg after its assigned value.

For every derived attribute `A` that a read needs settled, the transformation declares a generated characteristic
**`A (leeg)`** on the type that declares `A`, attributed per type that writes `A` by one rule **`A (leeg) van U`**
whose condition is the conjunction above (as for `K (niet)`, §5.6). A read of `A` then stands where it is filled or the
object carries `A (leeg)`; `c+` of "A is leeg" is `A (leeg)` and the leeg test (an input value given beside the writer
stays filled), whichever form reads it (§5.4). Over a path the characteristic is read over the first step; for two
or more steps a generated characteristic on the type the first step reaches carries "the rest is absent, or ends
settled leeg", as for an absent path (§5.8). Where no writer can establish it (an unconditional writer of a literal, for
instance), `A (leeg)` never holds: it is not declared, and its reads drop out.

What the trial does not write, and leaves open (safe, not complete): a cell of a derived dimensioned attribute
(`note settled-empty-cell`); an attribute an object creation assigns, where its value can be settled leeg, on the object
it makes; and, where that value can be open beside a writer on a comparable type, the attribute as a whole, since that
writer's `A (leeg)` would hold on the made object where `A` is open there (`note settled-empty-by-creation`, both).
Writers of `A` on two comparable types are refused (`settled-empty-across-subtypes`): on an object of the lower type both
write it, the rule of the upper type would hold there too, and a copied condition cannot be read on another subject
type (WF-053).

Example (witness `settled-empty`): "het vindloon van een Vondst is 10 indien hij kostbaar is" gives `vindloon (leeg)`
where `kostbaar (niet)` holds. Taxatie 50,00: `vindloon (leeg)`; 2000,00: vindloon 10; leeg: open, neither.

### 5.7 A sum type

A `Construct` and a `Match` are forms without this reading (RUNTIME-SEMANTICS ch. 1.5): a document that holds one is
refused.

### 5.8 A read over a link, and the other predicates and checks

**A relation is closed.** A read over a link that is absent is settled leeg: it stands in `d`, and the form in which it
stands reads it as the ordinary reading does (RUNTIME-SEMANTICS ch. 5.8: the operand is leeg). Only a read over a link
that is present and whose attribute is open is open. In detail:

- **`d` of a read over a path** is "a link of the path is absent, or the read is filled, or it reads a derived
  attribute that is settled leeg on the object the path reaches" (§5.6a). For one step from the subject
  of its position the absence is the negated role check of that step (IR-SPEC ch. 4.5): the subject fills the other role
  of the fact type in no link. For two or more steps: the first link is absent, or the object it reaches is established
  to lack the rest of the path; the trial writes the latter as a generated characteristic per type and rest of path
  (`<role> (ontbreekt)`), attributed by a generated rule where the rest is absent, and read over the first link (a
  quantifier fold over the to-one first step would say the same, but the Drools backend refuses a fold over a to-one
  role whose other role is many, DROOLS-232). A step from the form subject in a per-member position is a quantifier
  fold `none` over that step.
- **A comparison** over such a read decides as the ordinary reading does where the link is absent, with the table of
  §5.3, and that outcome is established.
- **Arithmetic**: the operand counts where the link is absent as the ordinary reading counts a leeg operand (as 0 in a
  plus, for instance).
- **A navigated characteristic check** is false over an absent link, negated or not (RUNTIME-SEMANTICS ch. 5.8 rule 3).
  So `c+` of "de vinder is ervaren" is the check, and its `c-` is "the link is absent, or the object on the other side
  carries `ervaren (niet)`". `c+` of "de vinder is niet ervaren" is "the object on the other side carries `ervaren
  (niet)`" (false where the link is absent, as in the ordinary reading), and its `c-` is "the link is absent, or the
  object carries `ervaren`".
- **A leeg or gevuld test** over a read over a path reads the state of the read (§5.4): where the link is absent the
  read is settled leeg, so the gevuld test is false and the leeg test true; where the attribute at its end is open,
  both are undetermined.
- A role check and an identity check stay two-valued: `c-` is the check turned around.
- The exact-digit test: `c+` is `d(operand)` and the test, `c-` is `d(operand)` and the test turned around. Witness
  `exact-digits`: a leeg registratienummer leaves geregistreerd undetermined.

Examples. Witness `absent-link`: "de vindpremie is zijn waarde plus de bonus van de vinder van de Vondst", "afwijkend
gemeld indien het gewicht van de vinder ongelijk is aan 80,00", "deskundig gevonden indien de vinder ervaren is" and
"ongeschoold gevonden indien de vinder niet ervaren is", each negation read by a toeslag; a Jutter is ervaren where his
ervaring is greater than 5. With waarde 100,00:

| case | vindpremie | afwijkend gemeld | deskundig gevonden | ongeschoold gevonden |
|---|---|---|---|---|
| no vinder | **100,00** | holds (leeg ≠ 80,00) | `(niet)` | `(niet)` |
| a vinder without values | **open** (ordinary: 100,00) | undetermined | undetermined | undetermined |
| a vinder 80,00, bonus 20,00, ervaring 10 | 120,00 | `(niet)` | holds | `(niet)` |
| a vinder 70,00, bonus 5,00, ervaring 2 | 105,00 | holds | `(niet)` | holds |

Witness `absent-link-path`: "ploegwerk indien de omvang van de ploeg van de vinder van de Vondst groter is dan 3": no
vinder, or a vinder without a ploeg → `ploegwerk (niet)`; a ploeg of omvang 5 → ploegwerk; a ploeg without omvang →
undetermined (ordinary: niet ploegwerk). Witness `navigated-read`: "een Vondst is volwassen gevonden indien de leeftijd
van de vinder groter is dan 17": a vondst without a vinder → `volwassen gevonden (niet)` and the begeleidingstoeslag 6,
as in the ordinary reading; a vinder without leeftijd → undetermined, toeslag **open** (ordinary: 6).

### 5.9 A consistency rule

A consistency rule reports only where its assertion is established false: its assertion becomes `none [a-]`. Witness `consistency`: "het gewicht van een Vondst moet groter of gelijk zijn aan 0" with
gewicht leeg reports nothing (ordinary: a channel fact); with gewicht −1,00 it reports. Witness
`filled-test-not-closure`: "de taxatie van een Vondst moet gevuld zijn" reports nothing where the taxatie is open
(§5.4).

### 5.10 An object creation and a fact creation

The condition of the creation becomes `c+`. An assignment of an object creation whose value passes leeg on by itself
(a read, a literal, a rounding of a read, ...) stays: a leeg value assigns nothing. An assignment whose value would need
"leeg unless `d(value)`" is refused (`creation-assignment`): the IR has no node that writes leeg. The attribute it
assigns is the input of the object made, filled or settled leeg after the value; §5.6a says what the trial leaves open
there. Witness
`object-creation`: "een Jutter heeft een proefvondst met taxatie gelijk aan 12,50 indien hij niet ervaren is", with
ervaren "indien zijn ervaring groter is dan 5", and "de ontvangen taxatie van een Jutter is de taxatie van de
proefvondst van de Jutter": ervaring 2 → a proefvondst is made and the ontvangen taxatie is 12,50; ervaring 10 → none
is made, the link is absent and the ontvangen taxatie settled leeg; ervaring leeg → ervaren is open, no proefvondst is
made and the relation is open (below), so the ontvangen taxatie is **open** (ordinary: a proefvondst is made, 12,50).

A creation whose condition is undetermined leaves the relation it would lay open, and a count over an open relation is
open (RUNTIME-SEMANTICS ch. 1.5, "A creation", "A read over a link"). Witness `fact-creation`: "een hulpvondst van een
Jutter is een vondst van de Jutter indien de Jutter niet gemeld is" and "het hulptal is het aantal hulpvondsten van de
Jutter", two vondsten: `gemeld (niet)` → **2**, `gemeld` → **0**, gemeld not named → **open** (ordinary: 2).

### 5.11 What stays where

In the trial every form, declaration and node outside a rewritten condition keeps its pointer and its source record; a
node copied into a generated condition keeps its source record, a node the transformation changes (a complementary
operator, a test turned around, a fold's filter) carries none. Rewritten are: the condition or assertion of a version,
the justification of a default and the filter of a fold. A generated characteristic (`K (niet)`, `A (leeg)`, the
characteristics of a stack and of a path) is appended to its type's characteristics, a generated rule to the rules, a
generated defaults group to the defaults groups. The filter a fold carries in the output is its `c+`.

### 5.12 Consequences for evaluation

- **A member after an undetermined member is read, provisionally.** `c-` of `all [A, B]` is `atLeast 1 [A-, B-]`:
  where A is undetermined, B- is evaluated and its judgement counts, but what it deposits in the §9.5 channel is
  dropped and a fault of it leaves it undetermined (RUNTIME-SEMANTICS ch. 1.5, "The judgement of a condition"), so a
  value given to A later, which can stop the read at A, takes no fact or fault away. In the trial a fault or a §9.5
  fact of B arose there, where the ordinary reading had stopped at A. The owner decided for this strong table over
  reading left to right (§12).
- **Stratification.** A gevuld test is a negative edge of WF-046 (RUNTIME-SEMANTICS ch. 6.1), so a test on a derived
  member adds a negative edge on that member, and the negated role check of an absent link one on a relation; a
  defaults group reads every justification read negatively (WF-051), the generated characteristics of §7 included.
  Outside a defaults group the transformed document reads a characteristic negatively only in a leeg test of a computed
  value (§5.4): where the value reads a fold whose filter checks a characteristic, or through the negation of
  `A (leeg)` that the trial's closure over an open value needed; the reading of ch. 1.5 has no such closure. The
  verifier refuses such an edge on a cycle, and the trial writes no document the verifier refuses (§12.5); §10.3
  counts them.
- **A fault deposit is repeated where a condition is.** The transformed document evaluates a condition or value of a
  form again in a generated form: `K (niet)`, `A (leeg)`, or the justification of a lower default ("no higher default
  applies"). Where that evaluation deposits a §9.5 fact (a division by 0 or by leeg), the fact stands there too. Inside a
  defaults group the ordinary reading keeps such a deposit speculative (RUNTIME-SEMANTICS ch. 7.1: only the deciding
  default deposits), and the transformed document cannot: a higher default that does not decide deposits through the
  justification of the lower one. A limitation of the trial, not of the reading: an executor's own lowering evaluates the
  condition once (§10.2).
- **A K3 limitation.** The reading looks per condition, not over all fillings: "indien gewicht < grens → A is 1" and
  "indien gewicht ≥ grens → A is 1" leave A open for an open gewicht although every filling gives 1. That is safe — never
  a wrong outcome — and explainable ("the rule needed the gewicht").
- **A hand-written opgave decides nothing on an open value.** "De meldtermijn is zijn vinddag plus 6 maanden", with
  the deviation "de meldtermijn is zijn vergunningseinde indien zijn vergunningseinde gevuld is": where the
  vergunningseinde is open, the deviation does not decide and the base below it does not either, so the meldtermijn is
  open (§5.4); where it is settled leeg, the deviation gives way and the base decides. A rule set says "the base
  applies where nothing is given" by declaring the member complete (§9): a test "indien zijn vergunningseinde leeg is"
  in the base is undetermined over an open vergunningseinde, and so is the opgave form over an open opgave (§7.4).
  Measured: one corpus case rests on this idiom over an open input (§11.2).

## 6. What the trial refuses

A form outside §5 to §9 is refused by name, never passed on: the document is not written, and the mode reports one line
per form, `<slug> TAB <pointer>`, exit 3. The slugs:

| slug | the form |
|---|---|
| `defaults-unordered` | a declared-unordered defaults group, or two or more exception groups under one default (§7.6) |
| `defaults-inheritance` | a target whose stack is composed over a type chain other than one level of own exception groups above an inherited group, or with a rule on another level (§7.5) |
| `quantifier-count` | a counting quantifier fold whose count the IR cannot write: a range of two or more steps, or a role whose name is also a member of the element type (§8.1) |
| `set-fold` | a fold over a set this transformation does not carry (an anchored fallback in a per-member position) |
| `dimension-fold` | a fold over the cells of a dimensioned attribute |
| `temporal-total`, `whole-period`, `timeline-member`, `time-window`, `temporal-read`, `time-proportional-part`, `start-point` | the timeline family |
| `allocation` | an allocation |
| `day-kind` | a day-kind definition, a day-kind test, a day count |
| `if-then-else`, `match`, `list-family`, `record`, `invoke`, `function-def` | the expression forms of ch. 9–11 |
| `let` | a binder that a fold inside the body of its Let reads: the condition a rule then needs would name it outside the Let |
| `uniqueness` | a uniqueness whose key a form writes |
| `rule-status-check` | a test of the channel of a rule |
| `unresolved`, `variable-reference` | the markers of an unresolved construct |
| `builtin-filter` | a builtin call with a filter |
| `creation-assignment` | §5.10 |
| `initialisation-value` | an initialisation default whose value does not pass leeg on by itself (§7.4) |
| `characteristic-by-creation` | a negated characteristic that an object creation assigns |
| `characteristic-by-defaults` | a negated characteristic that a defaults group decides beside a rule of its chain |
| `characteristic-across-subtypes` | a negated characteristic attributed on two comparable types: on an object of the lower type both attribute it, and a condition cannot ask for the type of its object |
| `characteristic-value` | a negated characteristic attributed with a value other than waar |
| `generated-name-collision` | §5.6, and the generated names of §5.6a, §5.8 and §7 |
| `default-vocabulary` | a generated declaration in a document with a default vocabulary, where WF-119 asks a designation of it |
| `settled-empty-across-subtypes` | a derived attribute written on two comparable types (§5.6a) |

Counted without refusal, as `note TAB <kind> TAB <pointer>`: `negation-over-versions` — an attribution or a writer with
a validity window is read as part of `K (niet)` or `A (leeg)` whatever its window, and the generated group of a dated
defaults group carries its window, so outside it neither is established: stricter than necessary and therefore safe;
`characteristic-always` — an unconditional attribution, on whose type `K (niet)` gets no rule; `initialisation` — an
initialisation, a closure (§7.4); `input-above-computation` — an opgave, closed by its declaration (§7.4); and
`closure-over-derived` — a leeg test, a filled-value count or a gevuld filter an author writes over a member a form
writes (§5.4); `settled-empty-by-creation` and `settled-empty-cell` — where a settled leeg attribute stays open (§5.6a).

## 7. The defaults stack and the initialisation

### 7.1 The rule

Walk the stack from high to low. A default whose justification is established false is skipped. A default whose
justification holds with a filled consequence decides. A default whose justification holds with a settled leeg
consequence falls through, as in the ordinary reading (RUNTIME-SEMANTICS ch. 7.1). At the first default that is none
of these three — its justification undetermined, or holding with an open consequence — the target is open, unless an
initialisation closes the stack (§7.4). Where every default is skipped or falls through and there is no initialisation,
the target is **settled leeg** (§5.6a).

So a default `i` decides under `cᵢ+`, `d(consequenceᵢ)`, and for every default `j` above it that `j` **gives way**:
`cⱼ-`, or `cⱼ+` with a settled leeg consequence (`d(consequenceⱼ)` and the consequence leeg), both built in the other
walk. The transformation keeps the group and its order and rewrites each default's justification into that
conjunction; the group's own fall-through then needs no change. Where a consequence passes leeg on by itself (a read, a
rounding of a read) its `d` is left out of the justification: the group falls through on its leeg anyway, and the
default below it requires that the leeg is settled. A consequence that cannot be leeg where it stands (a literal, an
input read under its `d`) gives way only by `cⱼ-`.

"Gives way" is that for a plain default; for a default with exception groups it is that the exception groups give way
and its own content does (§7.2). The condition "no default above this point decides" grows with the depth of the stack;
where two or more places read it — the next default and the condition of the one below it — it is written once, as a
generated characteristic **`<target> (geen hogere <n>)`** on the group's subject with one rule, numbered per target.
The stack stays linear in size: every condition is written once.

Example (witness `defaults-stack`): the vindloon of a Vondst, base "zijn gewicht maal 2", with three exceptions, highest
first: "op 2,00 indien zijn gewicht kleiner is dan 1,00", "op 0,00 indien hij betwist is", "zijn schatting plus zijn
schade indien zijn gewicht groter is dan 100,00". Two generated characteristics carry "neither the first" and "neither
the first nor the second".

| gewicht | schatting | schade | betwist | vindloon | ordinary |
|---|---|---|---|---|---|
| 150,00 | 1000,00 | 50,00 | `betwist (niet)` | **1050,00** | 1050,00 |
| 50,00 | 1000,00 | 50,00 | `betwist (niet)` | **100,00** | 100,00 |
| 0,50 | — | — | not named | **2,00** (the highest default needs nothing below it) | 2,00 |
| 150,00 | 1000,00 | 50,00 | not named | **leeg** (betwist undetermined) | 1050,00 |
| 150,00 | 1000,00 | leeg | `betwist (niet)` | **leeg** (the third holds with an open value) | 1000,00 |
| leeg | 1000,00 | — | `betwist` | **leeg** (the first is undetermined, so betwist does not decide) | 0,00 |
| 50,00 | — | — | `betwist` | **0,00** | 0,00 |
| 150,00, input vindloon 7,00 | 1000,00 | 50,00 | not named | **7,00** (the stack decides nothing; the input stands) | 1050,00 |

Example (witness `settled-empty-stack`): the bergloon of a Vondst, base "7 indien hij gemeld is", above it the
exception "zijn vindloon indien zijn gewicht groter is dan 100,00" (the frontend conjoins the base's condition), with
the vindloon of §5.6a (10 indien kostbaar, kostbaar: taxatie groter dan 1000,00), and "de uitbetaling is zijn bergloon
plus 3":

| gewicht | taxatie | gemeld | bergloon | uitbetaling | ordinary |
|---|---|---|---|---|---|
| 200,00 | 50,00 | `gemeld` | **7** (the exception holds with a settled leeg vindloon and falls through) | 10 | 7, 10 |
| 200,00 | leeg | `gemeld` | **open** (the exception holds with an open vindloon) | leeg | 7, 10 |
| 50,00 | 2000,00 | `gemeld (niet)` | **settled leeg** (no default applies) | **3** | leeg, 3 |
| 200,00 | 2000,00 | `gemeld` | 10 | 13 | 10, 13 |
| 200,00 | 50,00 | not named | open | leeg | leeg, 3 |
| leeg | 50,00 | `gemeld` | open (the exception is undetermined) | leeg | 7, 10 |

### 7.2 Exception groups

The exception groups of a default (ch. 7.9) count as above its own content: the own content decides only where its
exception groups give way, and every default inside an exception group carries the condition of its carrier's position
(the defaults above the carrier give way). One ordered member is carried;
two or more members beside each other are refused (`defaults-unordered`, §7.6).

### 7.3 A stack on a characteristic

A default of a characteristic stack decides "attribute" or "do not attribute". `K (niet)` is decided by a generated
group on the same subject, **`K (niet) van <subject>`**, with the same defaults and justifications, every decision
turned around, and below them the default "no default applies" (the condition of §7.1 over the whole stack) with the
decision to attribute. Example (witness `defaults-characteristic`): "een Jutter is vindgerechtigd indien zijn ervaring
groter is dan 2", with the exception "niet vindgerechtigd indien hij geschorst is" (whose justification conjoins the
base's), "de vindpremie is 10 indien hij vindgerechtigd is" and "de wachtpremie is 3 indien hij niet vindgerechtigd is":

| ervaring | geschorst | vindgerechtigd | vindpremie | wachtpremie |
|---|---|---|---|---|
| 5 | `geschorst (niet)` | holds | **10** | leeg |
| 5 | `geschorst` | `vindgerechtigd (niet)` | leeg | **3** |
| 1 | not named | `vindgerechtigd (niet)` (no default applies) | leeg | **3** |
| leeg | `geschorst (niet)` | **undetermined** | leeg | **leeg** (ordinary: 3) |
| 5 | not named | **undetermined** | **leeg** (ordinary: 10) | leeg |

### 7.4 The initialisation and the opgave

**The initialisation is the lowest writer of its target, and no closure** (RUNTIME-SEMANTICS ch. 1.5, "An
initialisation", "The initialisation of a stack"). It decides where it stands of every writer above it, the input among
them, that it fills nothing; there its condition and value are read as a rule's. Below a default that is undetermined
or holds with an open consequence the target is open, and where the target is an open input member, or another writer
leaves it open, the initialisation is not at its turn either. The target is settled leeg where every writer above gives
way and the initialisation fills nothing (§5.6a). An initialisation makes no member derived: a member that only an
initialisation writes is an input member with a default value, open where the case gives it no value, unless it is
declared complete (§9); then it is settled leeg above the initialisation, which gives its value. The trial read the
initialisation as a closure that decides also below a default that is undetermined; the owner's decision of 2026-11-23
(card `one-empty`, part 2) replaced that with the reading here, and §12 records the earlier one. The trial left an
initialisation default as it is: it carries no justification (WF-050), so where its value does not pass leeg on by
itself (a fold that skips a leeg member, a sum that counts one as 0) the trial refused it (`initialisation-value`, §6).

**The opgave reads as any default over an input member** (ch. 1.5, "An opgave"). In a group of form
`inputAboveComputation` (IR-SPEC ch. 2.13) the opgave is an input member (`opgegeven X`, which no form writes): where the
case gives it no value it is open, and no default below it decides, an initialisation among them. An opgave declared
complete (§9) is settled leeg where the case gives it no value: its default gives way, and the computation decides. The
trial closed the opgave by its declaration; ch. 1.5 does not. Example (witness `input-above-computation`, the opgave
declared complete): "het bergloon (mag opgegeven worden) is zijn gewicht plus zijn schade", initialisation 25,00:
opgave 30,00 → **30,00** and an inconsistency of the opgave against the computed 12,00; no opgave, gewicht 10,00, schade
2,00 → **12,00**; no opgave, schade leeg → the computation reads an open value, so the initialisation below it does not
decide: **open** (ordinary: 10,00); nothing given → **open**. Witness `input-above-computation-open`: an opgave not
declared complete and not given leaves the bergloon **open**.

Example (witness `initialisation`): "het strandloon is zijn vangst maal 2 indien hij gemeld is" and "het strandloon moet
geïnitialiseerd worden op 5 indien hij niet gemeld is": gemeld not named → **open** (ordinary: 5); `gemeld (niet)` →
**5**; input strandloon 7 and `gemeld (niet)` → **7**.

### 7.5 A stack composed over a type chain

A subtype inherits the whole stack of its supertype (ch. 7.10). One shape is carried: a group of own exception groups on
a subtype S, without a base of its own, above a group on a supertype T, with no rule on the target anywhere in the chain.
On an instance of S the defaults of T may decide only where S's own exception groups are established not to apply. The
transformation declares **`<target> (geen eigen uitzondering)`** on T, decided by two generated groups — on T always, on S
where S's own exception groups are established not to apply — and every default of T's group requires it; the
composition of ch. 7.10 then puts S's group in place of T's per type. Every other composition is refused
(`defaults-inheritance`).

Example (witness `defaults-subtype`): "het meldloon van een Jutter is zijn ervaring maal 2 indien hij gemeld is", for a
Strandjutter the exception "zijn vergunningsduur maal 3 indien zijn vergunningsduur groter is dan 0" and under that one
"50 indien zijn vergunningsduur groter is dan 10". A gemelde Strandjutter with ervaring 5: vergunningsduur 4 → **12**, 12
→ **50**, 0 → **10** (the inherited base), leeg → **leeg** (ordinary: 10).

### 7.6 What the stack does not carry

A declared-unordered group (several groups on one target) and two or more exception groups under one default reconcile
members that stand beside each other: one member decides, another conflicts. A member's decision is established only
where every other member is established to decide nothing or decides too; that needs, per member, "decides" and "decides
nothing" as conditions, which the transformation could write as generated characteristics; it does not. Refused
(`defaults-unordered`).

## 8. Folds and quantifiers over a set

### 8.1 The quantifier fold

A quantifier fold reads per member its filter F and test T in three values. Write X for "established excluded or
failing" (F- or T-):

| quantifier | `c+` | `c-` |
|---|---|---|
| `all` | every member: F- or T+ | some member: F+ and T- |
| `none` | every member: X | some member: F+ and T+ |
| `atLeast` *k* | at least *k* members F+ and T+ | *k* = 1: every member X; else count(X) + *k* − 1 ≥ count() |
| `atMost` *k* | count(X) + *k* ≥ count() | at least *k*+1 members F+ and T+ |
| `exactly` *k* | at least *k* members F+ and T+, and count(X) + *k* ≥ count() | at least *k*+1 members F+ and T+, or the `c-` of `atLeast` *k* |

"Every member" and "some member" are quantifier folds over the same range; count(X) and count() are counts of the range
with and without the filter X, against each other in a comparison. A count over a range of two or more steps is not
writable (WF-102), so a counting quantifier whose complement needs one over such a range is refused
(`quantifier-count`).

Example (witness `quantifier-fold`, the gewicht of the vondsten of a jutter against 10,00, and "keurder" over the vondsten
that are gemerkt): gewichten 20,00, 30,00 and leeg → "alle" undetermined, "geen" established false, "ten minste 1" holds,
"ten minste 2" holds, "ten hoogste 1" and "precies 1" established false; 5,00, 6,00 and leeg → "ten minste 2" established
false and "ten hoogste 1" holds (two members are established failing), "precies 1" undetermined; a vondst whose
gemerkt is not named makes "keurder" undetermined (ordinary: holds, the vondst dropped by the filter).

### 8.2 The folds over a set

The set is closed. `d` of a sum, minimum, maximum, first or last is a quantifier fold `all` over the same range: per
member, the filter is established false, or it is established true and the member's value stands. `d` of a count is
that every member's filter is established, either way. The fold's own filter becomes `F+`. Where `d` holds, the fold
computes as the ordinary reading does: it skips a settled leeg value, and a sum, minimum, maximum, first or last over
no contributor is settled leeg (a sum with a fallback gives its fallback). The filled-value count counts what is
filled: a closure for the form that wrote it; read for another form, its `d` also asks every member's value (§5.4). A
filter that tests on gevuld is a closure in the same way. The count of an instance-set union (ch. 3.4d) asks per branch
that every member's filter is established.

Example (witness `set-fold`): vondsten of 20,00, leeg and 30,00 → totaalgewicht, zwaargewicht (over the vondsten that
are zwaar), minimum, maximum and zwaaraantal **leeg**; vondstaantal **3**; the filled-value count **2**; eerste and
laatste vinddag stand. No vondst → totaalgewicht and the minimum **leeg**, settled, as in the ordinary reading; the sum
"of 0 als die er niet zijn" 0,00; counts 0.

Example (witness `settled-empty-fold`): "het totaal vindloon van een Jutter is de som van het vindloon van alle
vondsten van de Jutter", with the vindloon of §5.6a, and "het jutterloon is zijn totaal vindloon plus 1": a vondst with
vindloon 10 and one with taxatie 50,00 → **10** and jutterloon **11** (the settled leeg vindloon is skipped); one with
vindloon 10 and one with taxatie leeg → **leeg**, jutterloon leeg (ordinary: 10, 11); two vondsten with taxatie 50,00 and
60,00 → settled leeg, jutterloon **1**; no vondst → settled leeg, jutterloon **1**.

A minimum, maximum, first or last over a set that is listed beside other operands ("de eerste van A en B", §4) is an
operand like any other: over no contributor it is settled leeg, and the listed fold skips it as the ordinary reading
skips a leeg operand; a member whose value is open leaves the listed fold open. Example (witness `listed-fold`): "de
eerste meldingsdag is de eerste van (de eerste vinddag van zijn vondsten) en (de eerste klachtdag van zijn klachten)",
two vondsten of 5 and 1 maart and no klacht → **1 maart** in both readings; one vondst without vinddag → **leeg**
(ordinary: 1 maart); no vondst and no klacht → leeg.

### 8.3 Let

`d` of a `Let` is the `d` of its body, where a read of the binder contributes the `d` of the binder's value (ch. 10.6:
a leeg value empties the Let where the body reads it). Where a binder is read inside a fold in the body, its `d` stands
outside the fold, conjoined with the fold's own: stricter than necessary over an empty set, and safe. A generated
condition stands outside every Let: a binder of the same depth in it is replaced by its value; one read inside a fold is
refused (`let`). Witness `let`: "het drempelgewicht is de vangst maal 2, daarbij geldt: de vangst is zijn gewicht plus
zijn schade" with schade leeg → **leeg** (ordinary: twice the gewicht).

## 9. Closing per given

A member that counts as complete input is settled leeg where the case gives it no value: its read contributes nothing
to `d`, a comparison over it decides as the ordinary reading (§5.3), a leeg test of it reads as today in both walks, and
for a characteristic in the list "niet K" is not K, without `K (niet)`. In the trial this is a parameter of the mode, a
repeated `--closed <member>` (`attr:<type>:<attribute>`, `char:<type>:<characteristic>`, the declaring type); a member
that is not declared, or that a form writes, is an error of the caller. `--closed-inputs` closes every member that no
form writes: the measurement of preservation with complete knowledge (§1.1, §10).

Example (witness `closed-input`, with schade and beschadigd closed): "het nettogewicht is zijn gewicht min zijn schade"
with schade leeg → **10,00** (the gewicht, schade counting as complete); "de omvang is zijn gewicht plus zijn lengte"
with lengte leeg → **leeg**; "de hersteltoeslag is 5 indien hij niet beschadigd is" with beschadigd not named → **5**.

Forms the dialect can get for it (§12.3): a marker on the declaration, beside "(mag opgegeven worden)", such as
`het gewicht Numeriek (getal met 2 decimalen) (volledig);`; a rule of the rule set listing the members, `Volledig: de
schade van een Vondst, beschadigd van een Vondst.`; or a property of the case or the service call that names them per
evaluation. The first two put the closure in the rules, where §1 rule 3 asks for it. §10.4 measures what closing yields on
a rule set.

## 10. Measured

All measurements on the branch `card/strict-empty-lowering`, with its trial transformation.

### 10.1 The witnesses

Thirty-three documents, 150 cases with hand-computed expectations for the strict reading. On the ordinary documents 59
cases fail, at least one per witness: there the ordinary reading decides on an open value. The four witnesses of a
settled leeg derived attribute (`settled-empty`, `settled-empty-stack`, `settled-empty-fold`, `settled-empty-date`) were
written before the reading they pin, and failed on the transformation of that moment: 6 cases on the transformation
that read every leeg attribute as open, and 1 more (`settled-empty-date`) on a transformation that took a date plus a
duration never to be leeg. On the transformed documents:

| executor | plays | refuses |
|---|---|---|
| reference interpreter | 150 of 150 cases | — |
| Drools, executable model | 33 of 33 witnesses (150 cases) | — |
| Drools, DRL text | 33 of 33 witnesses (150 cases) | — |
| clingo | 139 of 150 cases | `arithmetic` (4 cases): CLINGO-005, a clamp bound that is not a ground literal — the ordinary document is refused alike; `comparison` (4) and `exact-digits` (3): "unscalable datatype in the ASP translation", without a register code, on a gevuld test of an enumeration or text attribute — the backend refuses that test in an ordinary document too; `exact-digits` is refused in the ordinary document by CLINGO-046 as well |

The gevuld test of an enumeration or a text attribute has since been translated in clingo on `main` (the golden
`jutterswet-gevuldtoets-soorten`); the row above is the measurement on the branch.

Every transformed witness is verifier-clean and passes the schema, key-order and byte checks of gate 2, and has no
negative edge on a characteristic outside a defaults group. Eleven report witnesses hold the refusals and notes the
transformation decides itself. The golden `jutterswet-strikt-leeg` (four cases) does not change with the third state —
it reads no derived attribute leeg —, its four cases are green in the reference interpreter, and the same bytes played
before in clingo and both Drools branches.

### 10.2 Preservation with complete knowledge

For every corpus golden that transforms and each of its cases: the transformation with every input member closed
(`--closed-inputs`: every attribute and characteristic that no form writes), played in the reference interpreter
against the case's own expectations, those of the ordinary reading.

| | goldens | cases | equal | not equal |
|---|---|---|---|---|
| corpus | 228 | 593 | 582 | 11 (in 5 goldens) |

The 11 per cause; none is a case where the strict reading itself differs from the ordinary one:

| cause | cases | goldens |
|---|---|---|
| a fault deposit of a higher default that does not decide, repeated in the justification of the lower one (§5.12): a division by 0 or by leeg in a default that falls through | 6 | 2 |
| a characteristic only a subtype declares, read on an object of its supertype (the narrowing host, RUNTIME-SEMANTICS ch. 5.8b): on an object outside the subtype it is established false, and the trial has no positive type test to say so | 3 | 1 |
| an attribute declared on a type and written only on a subtype: on an object of the type alone no writer applies, so it is input there, but a member closes per declaring type and a form writes it, so it is not closed | 1 | 1 |
| a dated defaults group outside its validity: what it alone decides is settled there, and the generated group carries its window (`negation-over-versions`) | 1 | 1 |

Reading these differences found places where the transformation left a settled value open, each fixed before these
numbers, among them a `Let` whose value is leeg (ch. 10.6), a date plus a duration over a leeg date (the witness
`settled-empty-date`) and an object creation whose assigned value made a writer's `A (leeg)` hold on the object it makes (§5.6a).

On the rule set of §10.4: the transformation of the version without guards with every input member closed, against the
ordinary reading of the same version, in 248 of 265 situations equal (the 6 that name a member it does not declare
included), 7 053 of 7 072 observable members equal, 19 open, none with another value or an outcome the ordinary reading
lacks. The 19 are of the third cause above, for characteristics: a derived characteristic on an object of a type its
writers do not reach (an object of the supertype, where the stack that writes it stands on a subtype).

### 10.3 The corpus

Of 389 goldens, 228 transform without a refusal. All 228 are verifier-clean (the mode checks it); every form and node
outside a rewritten region keeps its pointer and source; 74 negative edges on attributes, 9 on relations and 2 on
characteristics are new — the two on characteristics are leeg tests of a computed value whose fold filter reads a
characteristic (`zwaar`, and `A (leeg)` of a derived date), in a stack condition written outside its group and in an
`A (leeg)` rule (§5.12); no transformed document fails WF-046. The transformed document is 0.92 to 5.01 times the size
of the ordinary one (median 1.31). The refusals, per slug (places, documents): timeline-member 168/28, whole-period
48/17, temporal-total 33/19, time-window 29/13, list-family 25/19, dimension-fold 24/6, allocation 22/20, day-kind
21/15, record 21/7, creation-assignment 15/9, if-then-else 15/12, defaults-inheritance 11/8, defaults-unordered 10/6,
time-proportional-part 9/6, match 9/8, function-def 6/3, let 6/1, default-vocabulary 3/1, unresolved 3/1,
quantifier-count 2/2, generated-name-collision 2/1, and settled-empty-across-subtypes, initialisation-value,
rule-status-check, start-point and characteristic-by-creation 1/1 each. Notes: 39 initialisations, 13 opgaven, 9
closures over a derived member (in 7 goldens), 10 assignments of a creation where a settled leeg attribute stays open
(8 goldens), 1 dated group.

### 10.4 A rule set outside the repository

Three versions of a rule set outside this repository (the measurement, its names and the reading per difference are in
the card's work directory): the version before its hand-written guards against leeg (B), the version with them (A),
and a later one. All three transform without a refusal. The 265 situations of A's scenario documents were made into
case files with the RegelSpraak frontend and played in the reference interpreter on A, on B, and on B transformed (C);
6 situations name a member B does not declare and were played without it. Compared are the derived members both A and
B declare, per situation and object; a characteristic that no form reads negatively gets no `K (niet)`, so its absence
in C does not tell false from open, and it is counted apart (2043 members).

| | situations equal to A | members equal | open where A gives an outcome | an outcome where A has none | another value |
|---|---|---|---|---|---|
| B, ordinary | 224 of 265 | 8903 of 9116 | 0 | 186 | 27 |
| C, strict | 34 of 265 | 5812 of 6942 | 1069 | 55 | 6 |
| C with every input member closed | 209 of 265 | 6841 of 7073 | 20 | 186 | 26 |

With every input member closed, C is the ordinary reading of B but for 19 members (§10.2), and its distance to A is
B's: the hand-written guards of A, not the strict reading. The 6 other values of C are B's own outcomes (a closure B
writes on a derived date, read as the ordinary reading reads a settled leeg) that A's guards changed.

**Closing per given**, measured: for each of the 29 input members that a situation leaves leeg, C with only that member
closed, played on the situations where it is leeg. Ten members move members from open to A's outcome (714, 28, 18, 11,
11, 9, 2, 2, 2 and 1; two also move 8 and 2 members to another value than A's, B's own), 19 move nothing. Greedy,
closing each time the member that moves most over the members already closed, stops after eleven members: together they
move 846 members from open to A's outcome (open 1069 → 209), and 188 situations become equal to A. Closing all 29 at
once is the row with every input member closed in the table above: 20 open, 209 situations equal. One input member — a
count of days no situation gives — accounts for most of it (714): what stays open with a member left out is mostly what
rests on it.

### 10.5 What it costs

On the version without guards the transformed document is 4.66 times the ordinary one; on the two later versions 6.44
and 8.85 times. The growth comes from the generated forms (the rules of `K (niet)`, `A (leeg)` and the stack
characteristics), from the disjunction "filled, or settled leeg" per read of a derived attribute, and from the leeg
sides of a comparison over one. The Drools backend, wall-clock time of one run (JVM start, load, build, one situation),
median of three, with a load average of 2.7 to 7.9 at the start of a run on 16 cores: executable model 0.9 s ordinary,
1.3 s transformed; DRL text 2.8 s ordinary, 262.3 s transformed (runs of 262.3, 262.0 and 276.2 s). The DRL-text build
grows with the disjunctions: the transformed document carries 732 `atLeast` composites, 308 checks of an `A (leeg)` and
195 negated role checks; the executable model hardly notices them. An executor's own lowering need not write these
disjunctions as forms. Two measures would reduce the size without a new node: the filter of a fold written once, as a
generated characteristic of the element type per filter that `F+` and `F-` read; and the `d` of a value that several
forms read written once, as a generated characteristic per member. Neither is built.

## 11. The corpus and the golden

### 11.1 The golden

`jutterswet-strikt-leeg` (on the branch) carries a computation, a characteristic and its negation, an explicit closure,
a stack with a deviation ("Afwijking van"), a composite condition and a fold with a filter. Its `ir.json` is the output
of the trial transformation on `ordinary.json`, which the RegelSpraak frontend emits from `ordinary.rgs`. Its four cases
pin the input complete, one input leeg (a taxatie: whether the vondst is waardevol, its opslagtoeslag and the
zwaargewicht over the waardevolle vondsten do not stand), another part deciding (a gepensioneerde jutter without
leeftijd is vrijgesteld), and the closure (a leeg verpakking gives the verpakkingstoeslag while the brutogewicht does not
stand). On `main` a strict golden carries the ordinary document with the property of §3, and every executor plays it
strict itself.

### 11.2 The corpus cases

Of the 593 cases of the 228 transformed goldens, **458** keep their expectation in the reference interpreter without
closing anything. The 135 others rest on an open value; per case the categories that play a part (a case can have
several):

| category | cases |
|---|---|
| an operand that the ordinary reading counts as 0 or skips | 64 |
| a default or an initialisation below one that did not decide on an open value | 50 |
| a fault or a §9.5 fact the strict reading does not give, or gives where the ordinary reading did not (a leeg divisor's EF-031; a member read behind an undetermined member, EF-011) | 23 |
| an input characteristic the case does not name | 19 |
| a comparison over an open value | 16 |
| a negation of a derived characteristic over an open value | 14 |
| an absent instance (an empty set, a role check) | 1 |
| a hand-written opgave (§5.12) | 1 |
| an object outside the type a narrowing host names | 1 |
| an error of the transformation | 0 |

The categories come from the places `verifier --empty-closures` reports on the ordinary document behind each mismatched
expectation, filtered to the members the case leaves leeg; 3 cases were read by hand. Against the transformation that
read every leeg attribute as open, 11 cases now keep their expectation (a sum, minimum or division over a settled leeg
member, a chain over a settled leeg date, a fold without a contributor) and none lost it.

### 11.3 Monotonicity

For every case and every input member of it separately (an attribute value, a characteristic the case names), the
member is made empty and the transformed document played in the reference interpreter; every line of the end state of
that run (`runner --trace`, the lines after `state `: values, characteristics — `K (niet)` and `A (leeg)` included —,
links, objects, §9.5 facts) is looked up in the end state of the run on the full input. A line the full run does not
have (member `m` emptied, the line over member `M`) is placed, in this order:

1. **an input beside an undetermined writer**: the line is the value the case gives for a member that a writer replaces
   in the full run and not with `m` empty (RUNTIME-SEMANTICS ch. 7.8; the values a creation assigns are the input of the
   object it makes), or a member read from such a member;
2. a §9.5 fact of EF-031 is never placed: the strict reading does not fault on an open value;
3. **a named closure**: `verifier --empty-closures` on the transformed document has a place `P` of kind
   `initialisation`, `empty-test`, `filled-count` or `empty-fallback`, or a count over a relation a creation makes, or
   any place where `m` is a member the caller closes; `m` is among the members or input members of `P` (for an
   initialisation, also those of the defaults above it); and the form of `P` writes `M`, or a writer of `M` reads,
   transitively, a member the form of `P` writes;
4. **a fault in both readings**: the run faults, or the line is a §9.5 fact, and the ordinary document gives the same on
   the same case — a fault on filled values, reached because a member before it no longer decides (ch. 5.10).

Anything else fails. Over the witnesses and the golden: 34 documents, 154 cases, **327 runs**, 49 lines, all placed (a
leeg or gevuld test 31, a filled-value count 7, an initialisation 3, a closed member 3, a count over a relation a
creation makes 1, an input beside a writer 4). Over the 228 transformed corpus goldens:

| placement | lines |
|---|---|
| a named closure: a leeg or gevuld test | 82 |
| a named closure: an initialisation | 49 |
| a named closure: a filled-value count | 18 |
| a named closure: a count over a relation a creation makes | 18 |
| an input beside an undetermined writer, the line itself | 42 |
| an input beside an undetermined writer, a member read from it | 26 |
| a fault in both readings (30 February from an initialisation; a division by 0 reached behind an undetermined member) | 6 |
| not placed | 0 |

228 documents, 593 cases (one whose full run faults is not compared), 2124 runs, 241 lines. The placement is a static
reach over the dependency graph, not a proof per line. Run on the corpus, this check found two places where the
transformation had left the ordinary reading in force, each with a witness: a leeg or gevuld test of a computed value
(§5.4) and the value of an initialisation default (§7.4).

### 11.4 What `--empty-closures` still reports

On the 228 transformed corpus goldens the inventory reports 147 places of class `negation`: 59 at the fall-through of a
defaults stack or onto an initialisation, 59 comparisons false on leeg and 29 negated characteristics in the filter of a
fold; and 98 places of class `closure` that are no explicit closure and that the inventory does not see as guarded
(`fold-skips-empty` 78, `coalesce-*` 13, `empty-divisor-fault` 6, `true-on-empty` 1). Each is a place where the
inventory does not see the guard the transformation wrote; the acceptance of the card asks for none, that is not met,
and a dominance rule in the inventory that would see the guards is not built.

- **The fall-through of a defaults stack**: the lower default requires that the higher one gives way, inline or as a
  generated characteristic `(geen hogere <n>)`, and "gives way" is established only from standing reads. The inventory
  does not relate the generated characteristic to the higher default's reads.
- **The fall-through onto an initialisation**: the closure of §7.4, which the owner decided.
- **The filter of a fold**: the filter becomes `F+`, and the fold's `d` requires every member to be `F-` or `F+` with
  its value standing; where a member's filter is undetermined the form does not fire. The inventory reads the filter by
  itself, in a position that decides on false.
- **A settled leeg operand**: a fold that skips a member, a plus that counts one as 0, a comparison false on one — the
  ordinary reading over a value `A (leeg)` establishes; the inventory does not read `A (leeg)` as the guard.

Monotonicity (§11.3) is the measured proof that nothing in these positions rests on an open value. In an executor's
own lowering the generated conditions are its own, and the inventory can be held to the author's document (§12.15).

## 12. Decision points

Decided by the owner (2026-11-21):

1. **Kleene's strong tables, not left to right.** "A en B" is established false as soon as B is, also where A is
   undetermined; B is then read where the ordinary reading had stopped, and a fault of B can arise (one corpus case:
   EF-011, an evaluation year without a date). Reading left to right and stopping at the first undetermined member was
   measured as the alternative: the two differed in the outcome of 1 corpus case, where left to right loses an outcome
   the strong table establishes.
2. **The initialisation is a closure over its stack.** It decides where nothing above it decides, also where a default
   above it is undetermined or holds with an open consequence (§7.4). Its own value is read strictly: a rule
   initialisation fires on `d(value)`, and an initialisation default whose value does not pass leeg on is refused
   (`initialisation-value`; 1 corpus golden, 0 in the rule set of §10.4).

Decided by the main session after the measurement on the rule set of §10.4 (2026-11-21; provisional, for the owner to
confirm):

10. **A read over a link that is absent is settled leeg.** An absent instance is no open input, also for whoever reads
    over it: the form reads it as the ordinary reading does, and only a read over a link that is present and whose
    attribute is open is open (§5.8; the owner's description of leeg: "not yet filled in", and an object or relation
    that is absent may be computed on). A path of two or more steps is closed as soon as a step is absent; the trial
    writes it with a generated characteristic per type and rest of path.
12. **"X is leeg" over an open X is established only for the form that wrote it** (§5.4). Over a settled leeg X it is
    established everywhere; over an open X it is a closure in the walk of the form's own decision, and never
    established in a negation built for another form; a closure over a derived member stays and is counted.
16. **The third state and its two requirements** (§1, §2, §5.6a). A value is filled, settled leeg or open; a derived
    attribute is settled leeg where every writer is established to fill nothing; an expression and an atomic condition
    over standing values read as the ordinary reading; a default that holds with a settled leeg value falls through. The
    reading is held to monotonicity and to preservation with complete knowledge (§1.1). Measured: preservation in 582 of
    593 corpus cases and in 248 of 265 situations of the rule set of §10.4, the rest four limitations of the trial
    (§10.2); monotonicity without an unplaced line (§11.3).

Open, each with the measurement and a recommendation:

3. **Closing per given** (§9). Measured on the rule set of §10.4: eleven input members, closed one by one where they
   gain most, move 846 members to the guarded version's outcome and make 188 of 265 situations equal; all 29 that a
   situation leaves leeg leave 20 members open and 209 situations equal, the rest being the guarded version's own
   choices. Recommendation: a marker on the declaration, `(volledig)`, beside `(mag opgegeven worden)`: the closure
   stays in the rules and is read where the member is declared.
4. **An input characteristic beside a rule that derives its negation.** A case that gives `K` while the document
   establishes `K (niet)` gets both, and readers of both decide (measured in ref and both Drools branches: the vindpremie
   10 and the wachtpremie 3 side by side). It can be written with existing nodes: a generated consistency per `K` that
   reports where `K` and `K (niet)` both hold. Recommendation: generate that consistency.
5. **Negative edges.** A gevuld test is a negative edge (WF-046), and the transformation adds them on derived attributes
   and relations (74 and 9 over the corpus), and in two documents on a characteristic, through a leeg test of a computed
   value (§5.12). No transformed document fails WF-046. Recommendation: keep WF-046 as it is.
6. **A sum over no contributor is settled leeg**, the ordinary reading's leeg, not 0 (§8.2): preservation asks it. A rule
   set that wants 0 writes the fallback ("of 0 als die er niet zijn"). Recommendation: as measured.
7. **A count over a relation a creation makes** counts the links that stand, so an undetermined creation counts 0
   (§5.10). Monotonicity places 18 corpus lines on it. The alternative asks, per potential link, that the creation is
   settled. Recommendation: keep the closure and name it in the dialect's text.
8. **The input beside an undetermined writer stands** (§11.3: 42 lines, and 26 read from them): a writer that would
   replace the input does not decide, and the input value stands. The strict alternative leaves the target leeg, which
   no existing node can write over an input. Recommendation: keep it; it is the standing norm of ch. 7.8 and is visible
   in the explanation.
9. **An opgave is closed by its declaration** (§7.4): where the opgave is leeg, the computation below it decides, in
   both walks. Corpus: 13 opgaven noted; the rule set of §10.4 has 7. Recommendation: keep it; "mag opgegeven worden" says
   that a missing opgave is no opgave. It is also the form for what an exception "indien X gevuld is" tries to say
   (§5.12).
11. **An input characteristic the case does not name is undetermined.** A case says "onwaar" by naming `K (niet)`.
    Corpus: 19 cases rest on it. Recommendation: keep it, with the closure of §9 for the characteristics a rule set
    treats as complete input.
13. **A listed first or last over a fold without a member** skips it (§8.2): the fold over no contributor is settled
    leeg, as the set is closed, and the listed fold skips a settled leeg operand as the ordinary reading does.
14. **Unordered groups and other compositions** (§7.5, §7.6) are refused: 10 places in 6 goldens and 11 in 8. Writable
    with generated characteristics per member; to be built when a rule set needs them.
17. **A fault deposit in a default that does not decide** (§5.12). The trial evaluates a higher default's condition and
    value again in the justification of the lower one, where a deposit persists; the ordinary reading keeps it
    speculative (ch. 7.1). 6 corpus cases in 2 goldens. Not a question of the reading: an executor's own lowering reads
    the condition once. Recommendation: the executors keep the speculation; nothing to decide.
18. **A member on an object its writers do not reach.** A characteristic only a subtype declares, read on an object of
    the supertype (the narrowing host), is established false outside the subtype; a member declared on a type and
    written only on a subtype is input on an object of the type alone. The trial has no positive type test and leaves
    both open (corpus 4 cases in 2 goldens, the rule set of §10.4 19 members). An executor knows the type of its object.
    Recommendation: the reading as stated — false, and input — and closing per object type rather than per declaring
    type (§9), so that complete knowledge can be said for such a member.
19. **A dated defaults group outside its validity** decides nothing, so `K (niet)` holds and `A (leeg)` is settled
    there; the trial's generated group carries the group's window and leaves it open (1 corpus case). Writable for a
    characteristic only beside the group, which WF-050 forbids for a rule on the same target; for an attribute as rules
    with the complementary windows. Recommendation: the reading as stated.
20. **What the trial leaves open where the reading settles it**, safe and counted: a cell of a derived dimensioned
    attribute, and an attribute an object creation assigns (§5.6a, notes `settled-empty-cell` and
    `settled-empty-by-creation`: corpus 0 and 10). Recommendation: the reading as stated; an executor's lowering writes
    it per cell and per made object.
15. **What the landing asks**, on the route of §3 (the author's document with the property on the wire, the strict
    reading in every executor's own lowering):
    - *The IR, the schema and the frontend* carry the switch (§3). Still to write: the normative text of the reading in
      RUNTIME-SEMANTICS, from this proposal, with its worked examples.
    - *Every executor* (ref, clingo, Drools) applies the reading of §5–§9 in its own lowering, and is held to the
      witnesses of the branch, whose cases pin the outcome of every form: each witness becomes a golden with the
      ordinary document and the property, and its cases. The trial's transformation shows per form how `c+`, `c-` and
      `d` can be written; an executor need not write them as nodes.
    - *The case format* says "established not to hold" and "open" in fields of its own, not with a generated name
      (§5.6): the names `K (niet)` and `A (leeg)` are the trial's and do not reach a case or the wire.
    - *Monotonicity* as a gate over the strict goldens, with the placement of §11.3.
    - *Preservation with complete knowledge* as a gate: every strict golden played with every input closed must give the
      expectations of the ordinary reading of its document (§10.2).
    - *The explanation.* A rule that does not fire because a value it needs is open says so ("de regel had het gewicht
      nodig"); a settled leeg value can say why ("geen regel gaf het vindloon: de vondst is niet kostbaar"); a closure
      over a derived member (§5.4) can be named; generated conditions are not shown as forms of the author.
    - *The inventory.* `--empty-closures` reads the author's document with the property and knows the reading, so that
      the positions of §11.4 are recognised as guarded.
21. **The route per executor** (main session, 2026-11-21; provisional, for the owner to confirm). ref computes the
    reading directly: three states per member and, per condition, the two judgments "established true" and
    "established false". The Drools backend carries it in the plan per rule, before its two renderings. clingo gets a
    translation of its own (an own predicate for "established not", the plain join for an open operand) and does not
    apply the trial's transformation at load: that would keep the transformation as a permanent part beside three
    lowerings, grow the ASP program with the document (§10.5), and leave timelines to the workarounds this route
    avoids. No transformation serves as the measure; the measure is what the corpus uses everywhere: goldens with
    hand-computed cases that every executor plays, and the two requirements of §1.1 as gates.
