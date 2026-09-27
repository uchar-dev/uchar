# Grammar divergences — RegelSpraak frontend

Tracked divergences of this frontend's grammar from the RegelSpraak **v2.3.0**
specification (the arbiter). Each divergence is either a reconstruction of a
defective source production (noted at the rule in the `.g4` files) or an
**additive dialect extension** that is *inert on official v2.3.0 documents*.
The lexer/parser headers carry the per-rule rationale; this file records the
extensions and their lowering at one place.

The frontend takes the same inert-extension posture the grammar already uses
for `Regelgroep` (§9.10 is prose-only in v2.3.0) and the scenariotoetsen test
layer: the extension's opening tokens occur **nowhere** in the flat v2.3.0
corpus, so an official document parses byte-for-byte as before and emits an IR
with no extension keys.

**Pointers this register does not answer yet.** The frontend came over from the
source toolchain with its comments, and those comments pointed at that
toolchain's own register, a file that does not exist in this repository. Since
the fix round of 2026-09-23 the 24 in the grammar and the main sources point
here (two in existing tests still name the old file), but the text they cited
stayed behind. This register does not carry the sections
`'§9.10 recursie / regelgroep'`, `'negated membership'` and
`"concatenatie as a standalone value"`, nor the untitled topics at the other
sites: the percentage forms and the percentage sign, the unit checks of the
aggregations, the begrenzing bound, the period lowering, the "is gevuurd"
predicate, the mixed date precision, the best-effort role plural, and the
`Regelgroep` extension itself beyond the introduction above. At each site the
comment states its own rationale, and a card that touches such a construct
writes its section here. Two pointers land on text: the count's operand list
(§13.4.17.47 below) and the interaction of `Regelgroep` with the defaults
extension (§4).

---

## §4 The prioritized-defaults extension — `Uitzondering op` / `Afwijking van` / `Uitzonderingen ongeordend`

Origin: `spec/proposals/PRIORITIZED-DEFAULTS-EVOLUTIE.md` §4 (ratified
2026-08-29, Q1a). The uchar IR carries a `PrioritizedDefaults` group — a
prioritised default calculus (one writer, one target, defaults in descending
priority; `spec/RUNTIME-SEMANTICS.md` ch. 7).
Official RegelSpraak v2.3.0 has **no** exception, priority, default-override or
rule-ordering construct, so a flat RegelSpraak document can never denote
prioritized defaults. This dialect extension adds a source channel for authoring
them.

**Recognition only ever advises; it never folds.** A flat rule family is NEVER
transformed into a defaults group — the priority prioritized defaults encode is
not recoverable from a source that never wrote it
(PRIORITIZED-DEFAULTS-EVOLUTIE §4.2). Only the extension's own blocks lower to
defaults groups.

### Surface syntax (additive; new top-level alternatives)

Two productions sit beside `regel` / `regelGroep` / `beslistabel` in
`regelSpraakDocument`; the first carries two headers:

**Ordered exception** — stacks an exception above a named base rule (since
form 2, below, the name may also denote another block):

```
Regel <basisregelnaam>
    geldig <versie>
        <resultaat>            (kenmerktoekenning of gelijkstelling)
        [indien <voorwaarde>]

Uitzondering op <basisregelnaam>: <uitzonderingsnaam>
    geldig <versie>
        <resultaat>            (zelfde doel als de basisregel)
        [indien <voorwaarde>]
```

New tokens: `UITZONDERING_OP` (`'Uitzondering op'`, a whole-phrase token). The
base name runs up to the `:`; the exception's own name is an ordinary
`regelNaam` (up to `geldig`), so the body's `regelVersie+` is reached exactly as
a §13.4.2 rule's is.

**Deviation** (form 2) — `Afwijking van <basisregelnaam>: <naam>` and the same
body, the second header of the same production. New token: `AFWIJKING_VAN`
(`'Afwijking van'`, a whole-phrase token on the pattern of `UITZONDERING_OP`).

**Unordered group** — the only §9.5-conflict surface:

```
Uitzonderingen ongeordend:
Regel <lidnaam-1>
    geldig <versie>
        <resultaat>            (één gedeeld doel)
        [indien <voorwaarde>]
Regel <lidnaam-2>
    ...
```

New tokens: `UITZONDERINGEN` (`'Uitzonderingen'`), `ONGEORDEND`
(`'ongeordend'`). Like `Regelgroep`, the group scopes the **consecutive**
`regel` constructs that follow it and ends at the first non-rule construct, so
it is inert on any non-extension document. `UITZONDERINGEN` is never a prefix of
`UITZONDERING_OP` (`'Uitzonderingen'` has no space after `Uitzondering`), so the
two whole-phrase/keyword tokens never collide under maximal munch. Since form 2
the header takes an optional carrier, `Uitzonderingen ongeordend op <naam>:`,
which needs no token of its own: `op` is the preposition token `OP`, and the
carrier name runs up to the `:` as a base name does.

### Lowering (resolver)

Every form LIFTs already-resolved rules out of `resolvedRegels` into
`prioritizedDefaults`: WF-050 forbids both a rule and a defaults group on one
target, so the lifted rule's semantics live in the defaults group alone. The
model keeps a `Regel` stub for each lifted rule, but since M1 of the v1 wire
(delta B3) the emitter writes no `regels` list, so on the wire a lifted rule
exists only as its default.

- **Ordered:** the base rule and its `Uitzondering op` and `Afwijking van`
  blocks become ONE `PrioritizedDefaults` group — with everything hanging under
  them, form 2 below. Defaults descend in priority — the exception
  default(s) above, the base default below. With multiple exceptions on one base,
  the **later-written** block stacks **higher** (PRIORITIZED-DEFAULTS-EVOLUTIE
  §4.2). A characteristic rule yields a
  `besluit`-carrying default — `true`, or `false` where its sentence negates the
  characteristic (form 1, below); an attribute base yields a
  `consequentie`-carrying default. The base rule's geldigheid is carried onto the
  defaults group.
- **Unordered:** each `Uitzonderingen ongeordend` block without a carrier becomes
  SIBLING declared-unordered defaults groups (one per member) over one shared
  target — the `toelagegronden`-anchor shape. Every sibling carries
  `ongeordend: true` (WF-050: two-or-more defaults groups on one target require
  the marking on every one).
  Members deciding differently on one target route to the §9.5 Inconsistentie
  channel; deciding the same reconciles idempotently. On an attribute target the
  members differ in value; on a characteristic target a negated member (form 1,
  below) beside a positive one gives the conflict when both apply — no write and
  one §9.5 fact. Until form 1 a kenmerk assignment could only decide `waar`, and
  the conflict was expressible on an attribute target alone.

### Form 1 — a negated decision in a rule that becomes a default

Origin: maintainer decisions 1–3 of 2026-09-23
(`spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md` §4 and §10). RegelSpraak
v2.3.0 §9.2 has no negative attribution: a characteristic that no rule attributes
is false (§3.5), so in a flat document a negative attribution adds nothing, and
beside a positive one it is a conflict with no rule to decide it. Inside a
defaults group neither holds: the group is the target's only writer, its
priority is total, and a `false` decision writes nothing (RUNTIME-SEMANTICS
ch. 7.1; ch. 5.3's monotone writes hold). The dialect admits the negation there
and nowhere else.

| Kind (declaration) | Positive result | Negated result |
|---|---|---|
| bijvoeglijk — `is uitgesloten kenmerk (bijvoeglijk);` | `Een Jutter is uitgesloten` | `Een Jutter is niet uitgesloten` |
| bezittelijk — `het recht op de penning kenmerk (bezittelijk);` | `Een Jutter heeft recht op de penning` | `Een Jutter heeft geen recht op de penning` |
| zelfstandig — `de vaste ingezetene kenmerk;` | `Een Jutter is een vaste ingezetene` | `Een Jutter is geen vaste ingezetene`, `Een Jutter is niet een vaste ingezetene` |

These are the negations a condition already accepts (§8.1.8, Tabel 16), and the
resolver reads them the way it reads a condition: the WHOLE written name first,
so a declared characteristic whose name begins with `niet` or `geen` binds
itself; only when the whole name binds nothing is a leading `niet` or `geen`
read as the negation and the rest bound. A verb that does not fit the kind
(`is geen` on a bezittelijk characteristic) gets the condition side's Tabel 16
WARNING, at the line of the sentence; the lowering is the same. That lint checks
the verb against the kind and not the negation word, on both sides: `heeft niet
X` on a bezittelijk characteristic and `is geen X` on a bijvoeglijk one pass
without a message, in a result as in a condition. `is niet een` binds a
zelfstandig characteristic only, as it does in a condition; on another kind the
sentence is refused and the message gives the kind's own spelling (`is niet X`,
`heeft geen X`), since a result cannot stay an unresolved marker the way a
condition can. After `een` no negation is read at all: a condition binds the
name behind `een` whole (the EEN-form), so `is een niet X` and `heeft een geen
X` negate nothing there, and a result refuses them, with the spelling of X's
kind. A positive name keeps its written spelling, as before: the lookup only
decides whether the name is negated.

**Grammar.** `is niet X`, `is geen X` and `heeft geen X` parsed already: `NIET`
is a name word and `geen` lexes as an ordinary word, so both stand inside the
`kaleNaam`. `is niet een X` did not (`EEN` is no name word; the syntax error was
"extraneous input 'een'"). `kenmerkToekenning` has one more alternative,
`onderwerpKeten IS NIET EEN kaleNaam`, the result counterpart of the condition's
`NIET EEN` branch. `Regel` and the extension blocks share the production, so
whether a negation is admitted is decided in the resolver.

**Where it is admitted** (decision 2): in every rule that becomes a default — the
body of an `Uitzondering op` or (since form 2) an `Afwijking van`, a member of
`Uitzonderingen ongeordend`, and the base of an `Uitzondering op` chain. A negated
base is the lowest default and decides `onwaar`. In a flat model that changes no
outcome, since under the closed world an unattributed characteristic is false
anyway; what the base buys there is that the author can write a statute that opens
with the negation ("geen recht heeft …, tenzij …") and hang the exceptions on a
named base, as Catala's `definition x equals false` does. Under inheritance it
does change outcomes (RUNTIME-SEMANTICS ch. 7.10): on a subtype, its base replaces
the base of an ancestor's defaults group on the same characteristic, so a positive
inherited base stops applying to the subtype's instances; on a supertype, the
group's `onwaar` exceptions stay above a subtype's own base and block it where
they apply. A plain kenmerk rule is no base — ch. 7.10 folds attribute writers
only — so a negated subtype base does not switch off an ancestor's plain kenmerk
rule, which keeps writing on the subtype's instances. All three measured
2026-09-24 in ref, clingo and the Drools exec branch.

**Lowering.** A negated sentence becomes a default with `besluit: false` on the
target of its group; the rest is the lowering above. Between reading and
lowering the resolver carries the negation as the value `onwaar` on the
resolved kenmerktoekenning. WF-022 refuses that value on a flat rule, so a
plain rule that still carries it after the lift is refused (below), and the
value never reaches the wire.

**What a `false` decision says.** Where it is the highest deciding default of
its group, the rule set does not attribute the characteristic. It does not say
the characteristic is false: a characteristic the case input sets stays set
(RegelSpraak §3.5: input or attribution), and without input the closed world
makes it false anyway.

**Refusals and the warning** (witness: `NietInUitzonderingTest`):

- a negated kenmerktoekenning in a plain `Regel` — no exception, no member of
  an unordered block, no base of a chain — is an ERROR in the verifier's WF-022
  sentence for the value the rule would write (`regel '<naam>':
  characteristic target's value must be the boolean constant true`).
  Measured on the frontend before this card: the sentence went to disk as an
  undeclared characteristic `niet uitgesloten` with `diagnostics: []`, for the
  verifier to refuse under WF-022; in an `Uitzondering op` it drew "schrijft een
  ander doel (Jutter.niet uitgesloten (kenmerk))". A plain negated rule on a
  target that also has a defaults group gets this ERROR alone, without WF-050's
  second-writer ERROR beside it. A rule written in an extension block that was
  refused for another reason stays un-lifted and is judged by that refusal;
- a negation whose rest names no characteristic of the subject (`is niet
  vervallen` with `vervallen` undeclared, and no characteristic `niet vervallen`
  either) is an ERROR naming the rest ("… ontkent geen kenmerk van Jutter —
  'vervallen' is geen kenmerk van Jutter …"); when the rest is a declared
  ATTRIBUTE the message says so, and for a Boolean attribute it points to `moet
  gesteld worden op onwaar`.
  The lowering then skips the block's family instead of adding a
  target-mismatch ERROR. A POSITIVE result on an undeclared characteristic is
  still not refused here: it reaches the verifier's WF-022;
- `is niet een X` on an X that is bijvoeglijk or bezittelijk is an ERROR ("de
  vorm 'is niet een' ontkent een zelfstandig kenmerk (Tabel 16) …");
- a negation written after `een` (`is een niet X`, `heeft een geen X`) is an
  ERROR ("… ontkent niets — achter 'een' leest de frontend de naam als geheel
  …") that gives the spelling of X's kind;
- there is no negated gelijkstelling: an attribute has no polarity. `moet
  gesteld worden op onwaar` on a Boolean attribute stays a value — a
  `consequentie`, never a `besluit` — and `niet` written into an attribute name
  is no negation (the undeclared-attribute refusal of WF-022);
- on a characteristic target where every default of every group decides
  `onwaar`, the groups never attribute the characteristic. That is well-formed
  and draws no ERROR; the RuleChecker gives one WARNING per target ("… kent het
  nooit toe …", with several groups "kennen"), which the emitter writes into
  `diagnostics` (WF-113 (a)). Per target, not per group: an unordered block
  spreads its target over sibling groups, and a target with a negated sibling
  beside a positive one derives through the positive one. The warning is left
  out where the characteristic has another writer in the subject's inheritance
  line: a defaults group there composes with these groups and the all-`onwaar`
  group then changes outcomes (above); a plain kenmerk rule there keeps writing
  beside the groups, and the warning's hint, a missing `waar` exception, would
  point away from it. It is left out beside a plain rule on the same target
  too, which draws WF-050's ERROR instead;
- the refusals above stand at the decision table's own line when the rule is
  a table row (a row is re-parsed in a stream of its own) and name the table;
  a rule with several versions draws one ERROR, not one per version.

**Which documents change.** Only a document with a result sentence whose name,
after an article, begins with `niet` or `geen` and does not bind as a whole, or
with `is niet een` — the forms §9.2 keeps out of official RegelSpraak. Before
this card no such document reached a valid IR: it drew the target-mismatch ERROR
or a syntax error, or it was emitted with a characteristic named `niet …` or
`geen …` that its object type does not declare, which the verifier refuses
(WF-022 on a flat rule, the defaults-target check on a group). The 71 `bron.rgs`
goldens that existed before this card re-emit byte-identically
(gate 10). The golden `jutterswet-schorsing` (Jutterswet art. 3 and 4bis) is the
first whose source carries a negated exception.

### Form 2 — the exception tree, `Afwijking van` and `Uitzonderingen ongeordend op`

Origin: maintainer decisions 4–6 of 2026-09-23
(`spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md` §5 and §10), with the two
decisions of that day's second decision question: an `Afwijking van` names a
base rule only, and the members of an `Uitzonderingen ongeordend op` block
inherit their carrier's justification. Measured on the frontend before the card
that built it (`dialect-uitzondering-op-uitzondering`): an `Uitzondering op`
naming an exception drew "geen basisregel met die naam gevonden", because the
lowering took each family out of `resolvedRegels` before it looked up the next
carrier; `Afwijking van` was a syntax error ("extraneous input 'Afwijking'"),
and so was `Uitzonderingen ongeordend op` ("mismatched input 'op' expecting
':'").

**The tree.** Every `Uitzondering op` block, every `Afwijking van` block and
every member of an `Uitzonderingen ongeordend op` block names one carrier. The
resolver looks the name up among these blocks first and among the plain rules
second. A carrier that is no block is the base rule of its tree, and each tree
becomes one defaults group, named after that base. Every block is resolved
before anything is lifted, so where a block is written does not decide whether
its carrier is found. Among the children of one carrier the later-written block
ranks higher, at every level, as the first level always did. The depth has no
bound: WF-075 and the three executors recurse to any depth.

**Lowering** (the lowering of the proposal's §5.2, in the wire's names):

| Source | Wire |
|---|---|
| `Uitzondering op B: E1` and `Uitzondering op B: E2`, B a base rule | chain positions above the base: `[E2, E1, B]` |
| `Uitzondering op E1: F1` and `Uitzondering op E1: F2`, E1 a block | `E1.exceptions = {unordered: false, members: [ExceptionGroup [F2, F1]]}` |
| `Uitzonderingen ongeordend op E1:` with members P, Q | `E1.exceptions = {unordered: true, members: [ExceptionGroup [P], ExceptionGroup [Q]]}` |
| `Uitzonderingen ongeordend op B:` with members P, Q, B a base rule | a group-default directly above the base: `[{exceptions: {unordered: true, members: [[P], [Q]]}}, B]` |
| `Afwijking van B: A`, B a base rule | a chain position above the base, as a first-level `Uitzondering op` |

The first level stays a chain and does not nest into the base: a subtype's own
base replaces the inherited base whole (RUNTIME-SEMANTICS ch. 7.10), and
exceptions nested in it would go with it. With single-member exception groups
the chain and the nested shape order the defaults identically and decide
identically on every input (ch. 7.9). A group-default carries no justification of
its own (WF-075). A head makes an `ExceptionGroup` and a group-default, not a rule,
so no annotation belongs to either; each carries the head's text up to the
carrier's name as its source: `Uitzondering op E1` on the ordered group of E1's
children, whose heads all read so, and `Uitzonderingen ongeordend op X` on each
member's group and on the group-default of that block. Every hand-written anchor
that nests gives these nodes a source. A nested block's `// Bron:` annotation
stands on its own default (next subsection).

**`Uitzondering op X` is restrictive.** The justification of the default the
block becomes is one flat `Compound` with quantifier `all` and threshold 0: the
conjuncts of X's justification first — that justification is itself conjoined, so
the whole chain down to the base comes along — and then the block's own
condition. An own condition that is itself an `all` is flattened, at any depth; a
condition under another quantifier stays one conjunct; an exception without a
condition carries the carrier's conjuncts alone. Where the carrier contributes no
conjunct — an unconditional base, whose justification is the empty `all` — the
exception keeps the condition it was written with, since conjoining with the
empty `all` changes nothing (`jutterswet-wrakrecht`, two unconditional bases,
re-emits byte-identically). The compound's source text is the conjuncts' texts
joined by " en ", without the period that closes a compound conjunct's bullet list
where another conjunct follows it. The reasons: RUNTIME-SEMANTICS ch. 7.9 decides
a default's exception groups before and regardless of the default's own
justification, so an exception that does not carry its carrier's condition decides
where the carrier does not hold; ch. 7.1 asks the frontend for self-contained
justifications, built by conjoining the enclosing layer's condition; and the legal
meaning of an exception is restrictive. The members of an `Uitzonderingen
ongeordend op X` block inherit X's justification in the same way. The carrier's
conjuncts are the same nodes in every justification that repeats them, and the two
sweeps over the finished defaults — the report of unresolved markers and the
WF-112 fence — judge each node once: a marker in a base's condition is one
WARNING, not one per exception above it.

An exception a subtype inherits keeps the conjunction it was written with, the
condition of the supertype's base included, also on a subtype whose own base
replaces that base (RUNTIME-SEMANTICS ch. 7.10): `Uitzondering op X` restricts to
where X holds, and X is the supertype's. Measured 2026-09-25 in ref, clingo and the
Drools exec branch on a supertype base `10,00 indien erkend` with the exception
`99,00 indien betwist` and a subtype base `20,00 indien diep`: a subtype instance
that is betwist and diep but not erkend gets 20,00, where the exception without
the conjunction would decide 99,00. No golden has conditional bases on two levels
(`jutterswet-wrakrecht` has unconditional ones), so none pins this.

**`Afwijking van B` widens.** A block that widens its base — a hardship clause
that applies the base "als ware twaalf vervangen door zes" — is not an exception
but a deviation: its default carries its own condition only, and the empty `all`
when it has none. Its carrier is a base rule only: in the middle of a tree a
block that inherits nothing could decide where its carrier's condition does not
hold. An `Uitzondering op` or an `Uitzonderingen ongeordend op` block may name a
deviation as its carrier, and then inherits the deviation's own condition.
`jutterswet-bergingsdrempel` (noodweer with eight wrecks above a base of twenty)
is written with this header; its `ir.json` is byte-identical to the one its
`Uitzondering op` gave before form 2.

**Refusals**, each one ERROR at the block, naming what can be written instead
(witness: `UitzonderingOpUitzonderingTest`):

- a carrier that names nothing: "Uitzondering op 'X': geen basisregel met die
  naam gevonden, en ook geen uitzondering of afwijking — noem als drager …", and
  for a deviation "Afwijking van 'X': geen basisregel met die naam gevonden — de
  drager van een afwijking is een gewone platte regel …";
- a cycle of carriers, with its path: "de dragers vormen een kring ('A' -> 'B'
  -> 'A')"; a block that names itself is the shortest cycle;
- an `Afwijking van` naming an exception, a deviation or a member: the message
  names `Uitzondering op X: <naam>` (restrictive) and `Afwijking van <basis>:
  <naam>` (with the full condition);
- a block written for a subtype under a node of its supertype's tree
  (RUNTIME-SEMANTICS ch. 7.10; `spec/proposals/SUBTYPING.md` §4.3–4.4: a subtype
  inherits an exception together with what is nested under it, and addresses none
  of it). The message names a subtype-own exception on a base rule of the subtype
  carrying the full condition of the node plus its own: the fixed block order lets
  it decide exactly where both apply. Where the subtype has no rule of its own on
  the target, the message adds that it has to write one with the outcome and the
  condition of the base it inherits (named for a direct subtype), because an own
  base replaces the inherited one. A block for a subtype under the supertype's
  base rule keeps the target refusal below;
- a carrier with ordered children and an `Uitzonderingen ongeordend op` block, or
  with two such blocks: nothing fixes the priority between them. The message names
  the two expressible forms, all ordered or all members of one block. Recorded, not
  built: the group-default at its writing position in the carrier's chain;
- a member of an `Uitzonderingen ongeordend:` block WITHOUT a carrier, named as a
  carrier: such a member is a defaults group of its own beside the other members,
  not a node of a tree. The message names `Uitzonderingen ongeordend op
  <basisregel>:`, whose members can carry. Before form 2 the same document drew
  "groepslid '…' leverde geen kenmerktoekenning/gelijkstelling op". Recorded,
  not built: lowering such a member's sibling group to the chain `[F, member]`,
  the member as its base and the block F that names it above it. The way out
  the message names asks for a base rule, which parallel grounds without a
  common base do not have;
- per edge, the fences of the first level: the target (WF-050), the validity window
  (the group carries one), and one version per rule.

A refused node keeps its whole tree un-lifted, as a refused family always stayed,
and nothing below a refused node is judged, so one mistake draws one ERROR. The
members of an `Uitzonderingen ongeordend op` block share its header: once one
member is refused, the block's other members are not judged, so a block draws
one ERROR however many of its members would be refused.

**Which documents change.** Two goldens. `jutterswet-schorsing` keeps its
`Uitzondering op` (art. 4bis restricts art. 3) and was re-emitted: its
exception's justification now carries the base's `>= 12` first; its four cases
are unchanged and pass in ref, clingo and the Drools exec branch.
`jutterswet-bergingsdrempel` changed its header to `Afwijking van`, with a
byte-identical `ir.json`. Every other `bron.rgs` golden re-emits byte-identically
(gate 10), and so does every re-emitted source of the parity zone (gate 11). The
phrase `Afwijking van` occurs in no source of the repository besides that golden
(counted 2026-09-25 over the 81 corpus sources and the 47 of the parity zone) and
nowhere in the v2.3.0 specification set.

Goldens: `jutterswet-uitsluiting-rgs` (art. 4 Jutterswet, an exception on an
exception) and `jutterswet-nestkenmerk-rgs` (an unordered block under a base
rule, with the conflict key `Jutter.vrijgesteld/0`), the RegelSpraak twins of the
anchors of the same name; each NOTE carries its structural diff against the
anchor.

### `// Bron:` annotations of lifted rules

A lifted rule leaves `resolvedRegels`, and with it the `bronAnnotaties` list
that carries its `// Bron:` annotation on a flat rule. Until 2026-09-23 the
lowering also set `bron: null` on every defaults group and every default, so
each annotation on an extension block vanished. Measured on the frontend before
the change (the M6 document of `spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md`
§9.2: an annotated base and its annotated exception): neither annotation text
occurred anywhere in the IR, with `diagnostics: []` and a verifier PASS. The
placement since, with no new wire field (card `frontend-bron-annotatie-in-defaults`):

- a lifted rule's annotation lands in the `Default.bron` of the default it
  becomes: the exception's on the exception default, the base's on the base
  default, an unordered member's on its sibling group's single default;
- the group's `PrioritizedDefaults.bron` carries the annotation of the rule the
  group is named after: the base of an ordered family, the member itself for an
  unordered sibling. The base's text therefore stands twice, on the group and
  on the lowest default. The hand-written defaults anchors decided this: in
  every one of them each default, the lowest included, carries its own
  provenance beside the group's (`anchor/ww-herleving-inkomen`: the group
  "art. 15 tot en met 21: …", its base default "art. 15: …"). Writing the base's
  text on the group alone would give the base default a shape no anchor has,
  and it would lose the per-default citation the statute trace hangs on. The
  anchors' group-level citation is an umbrella the frontend cannot write
  without inventing text ("art. 3 en 5"), so the group takes the text of the
  block it is named after;
- several annotations on one block are joined with `"; "` in source order, and a
  blank one is dropped, as the emitter drops it from `bronAnnotaties`. The join
  loses a distinction and cannot be undone: one annotation that itself contains
  `"; "` reads exactly like two, and the wire already carries such a text (the
  group citation of the golden `jutterswet-samenloop`), so a reader cannot
  recover the list from a joined snippet. Recorded, not repaired: `Default.bron`
  and `PrioritizedDefaults.bron` are single strings (on the wire the `source`
  snippet), and keeping the list would take a new wire field (option (b) of
  the proposal's §9.2, not taken). Where the separate annotations matter, write
  one per block;
- an unannotated block writes no `source` (its `bron` stays null): the three
  extension goldens carry no annotation and re-emit byte-identically;
- the `Regel` stubs carry no annotation either: since M1 of the v1 wire (delta
  B3) the emitter writes no `regels` list, so a lifted rule's annotation reaches
  the wire through its default alone;
- nesting (form 2): a nested block's annotation lands on its own default inside
  the `ExceptionGroup`. Every lifted rule becomes a default in one place
  (`RuleChecker.defaultVan`), so the nested lowering inherits the placement. The
  `ExceptionGroup` itself and a group-default carry the text of the head that
  made them, not an annotation (form 2, above; witness
  `UitzonderingOpUitzonderingTest`);
- an annotation written above the `Uitzonderingen ongeordend:` header belongs
  to no rule — an annotation belongs to the rule directly below it, and the
  header is none — so it reaches no default and no group (the group-default an
  `Uitzonderingen ongeordend op` header makes carries the header's own text,
  form 2). It used to vanish without a word; it now draws a WARNING that names
  it, instead of being hung on a member the author did not choose. The same
  WARNING, citing §9.10, covers an annotation above a `Regelgroep` header (fix
  round, 2026-09-23).
  The group's own `bron` is no place for it: on a `ResolvedRegelGroep` that
  record's snippet is the group's name (the `source` of its `RuleGroup`). A
  `Beslistabel` header is the one header whose annotation does reach the wire,
  because the table is rules: the annotation lands in the `bronAnnotaties` of
  every rule the
  table desugars to, the multi-version precedent of IR-SPEC ch. 3.1 (one comment
  block above a shared header, stamped per flattened entry). Until the same fix
  round it vanished, since each row is re-parsed in a stream of its own where no
  comment stands above it. An annotation above
  a definition (an `Objecttype`, `Feittype`, `Domein`, `Parameter`, …) draws no
  warning: it documents the declaration, which has no wire slot for it, and it
  is common practice — 351 of the 736 annotations in the measured operational
  ruleset collection stand above a definition.

Witness: `DefaultsBronAnnotatieTest`, `BronAnnotatiePlaatsTest`.

### Fail-fast refusals (resolver, loud diagnostics)

- an `Uitzondering op`, `Afwijking van` or `Uitzonderingen ongeordend op` naming a
  carrier that does not exist, and the other refusals of form 2 (above);
- an exception (or group member) writing a DIFFERENT target than its carrier
  (or than a prior member) — a defaults group is over ONE `(onderwerp, kenmerk XOR
  attribuut)`;
- a base rule that is not a plain kenmerktoekenning/gelijkstelling (an
  initialisatie, or a verdeling/consistentie/objectcreatie/uniciteit);
- a negated kenmerktoekenning in a plain `Regel`, a negation whose rest names
  no declared characteristic, `is niet een` on a characteristic that is not
  zelfstandig, and a negation written after `een` (form 1, above);
- a base rule or exception with more than one `geldig`-versie (not carried by
  this first card);
- an extension block within the scope of a `Regelgroep`, or over a base rule
  that is a group member (next subsection);
- a second writer on a group's target (WF-050, fix round 3): a plain rule
  beside a defaults group — another rule on the base's target, or the other
  rows of a decision table when one row is the base — or a second group on the
  target unless every group there is declared unordered (two ordered
  families, a family beside an unordered block). Each reached disk silently
  for the verifier to refuse; the ERROR is the verifier's WF-050 sentence for
  the clause, after a Dutch context that names the groups and the other
  writers. Keyed on the declared object type, as WF-050 is, so a supertype's
  and a subtype's stacks stay two targets. Two unordered blocks on one target
  stay admitted. A rule written in an extension block whose family or block
  the lowering refused is not counted as a second writer (2026-09-23, card
  `dialect-niet-in-uitzondering`): its block's own ERROR stands, and a
  second-writer ERROR would name a rule of the very block it stands in.

### Interaction with `Regelgroep` (§9.10) — refused

Maintainer decision 2026-09-23, option (a) of the card
`frontend-regelgroep-sluit-bij-uitzondering`. A `Regelgroep` scopes the
consecutive rules and decision tables behind its header and exists only to
mark recursion (§9.10, prose-only in v2.3.0). `groepLid` takes no extension
block, so until that day a group **ended silently** at the first extension
block. Measured on the frontend before the refusal (the M5 document of
`spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md` §9.1): a group over a
base rule, its `Uitzondering op` and a third rule emitted `leden` holding the
base alone with `diagnostics: []`; the exception and the rule behind it fell
outside the group without a word, and because the lowering lifted the base
into a defaults group, the verifier then refused the document under WF-033 —
on the group rather than on the block, and without the cause.

The dialect does not admit the combination. Option (b) — a defaults group as
a group member — would need WF-033 to accept one and a statement of what a
defaults group inside a recursive group means: a spec question with three
executors behind it, recorded and not taken. The refusal is one ERROR at the
block's own head, with two arms:

- **positional** — the block stands directly after a group's last member,
  where the group would read on if the block were a rule. The arm keys on
  "neither a definition nor a group" rather than on the heads by name,
  so `Afwijking van`, which came later, is refused there by construction
  instead of reopening the silent end;
- **membership** — the block's base rule is a group member, wherever the
  block stands. A definition that ends the group does not help: the lift
  would still take the member out of the rule forms, and the group would
  name nothing (WF-033). A decision table in the group stands for the rules
  of its rows ("Bergingspremie 1", …), which is what a block names as its
  base; the table's own name names no rule. The group's own `leden` list
  those rows too: until the same fix round it carried the table's name, and
  the verifier refused every group holding a table under WF-033. A row is
  named after the table and its number, so rows without a number, or with the
  same one, share a name; that table is refused by name, in the verifier's
  WF-020 sentence (`duplicate name '<rij>' in rules`), and the group lists each
  name once (fix round 3). The arm speaks only where the
  lift would happen: a base that resolves to no plain rule — a table's own
  name, an object creation — gets the lowering's "no base rule" refusal,
  which names the construct (fix round 2, 2026-09-23); for a table it names
  the rows and says why none of them alone can be a base (fix round 3).
  `Afwijking van` shares the production of `Uitzondering op` and so the arm;
  `Uitzonderingen ongeordend op` joined it by name (form 2). A later head that
  names a carrier of its own has to join it the same way.

The membership arm speaks the verifier's WF-033 sentence for the group the lift
would leave (`group member '<basisregel>' does not resolve to a rule form`),
after a Dutch context naming the base's group; the positional arm alone keeps
its Dutch words, citing §9.10 and this section, because the document it keeps
off disk is well-formed (the block and the rules behind it stand outside the
group). A block for which both hold gets one diagnostic, the membership arm's,
with the placement in its context, and a refused block stays un-lifted
together with its whole base family, as the other refusals leave it. The way
out the positional message names is one that holds wherever the author
follows it: before the document's FIRST group header,
which no group's scope reaches, or behind a definition that ends the group.
"Before the header of this group" was named until fix round 2 and did not
hold — with two groups back to back it lands behind the other group's last
member. A header without any member is a syntax error of its own and draws
no placement message. What stays clean, and is pinned as clean: a block
before the group's header, and a block behind a definition that ends the
group, each over a base outside the group. Two placements that were
verifier-clean before are refused since — an `Uitzondering op` directly after
a group whose members do not include its base, and an `Uitzonderingen
ongeordend` block directly after a group — because at that position the rules
behind the block would fall outside the group in silence. No `.rgs` source in
the repository — golden, rood or parity zone — carries a `Regelgroep` at all,
so nothing in the corpus moves. Witness: `RegelgroepUitzonderingTest`.

### Emission (v0-mirror invariant)

`prioritizedDefaults` is emitted ONLY when the extension, or the opgave marker
of §7, produced defaults groups. A document without either emits no such key,
so the v0-mirror byte-identity of every flat golden holds. Within the list,
`attribuut` is emitted only on attribute defaults and `ongeordend` only when
true, matching the schema and the hand-written prioritized-defaults anchors.

### Scope / limitations of the first card

- The first card exercised the ordered shape on a characteristic only. Since
  then `jutterswet-wrakrecht` exercises it on an attribute target, on a
  supertype and its subtype, `jutterswet-schorsing` with a negated
  exception (form 1, above), and `jutterswet-uitsluiting-rgs` and
  `jutterswet-nestkenmerk-rgs` form 2.
- Exception-on-exception is authored from source since form 2 (above): nested
  exceptions lower to `Default.exceptions`, and `Afwijking van` and
  `Uitzonderingen ongeordend op` joined the surface.
- Multi-version base/exception rules are refused (single-version only).
- The new keyword tokens narrow the ordinary-name space: `Uitzonderingen`
  (capitalised) and `ongeordend` are reserved and can no longer appear as
  name words (e.g. a plural `(mv: Uitzonderingen)` or a kenmerk named
  `ongeordend` now refuses loudly at the lexer) — the same loud posture the
  existing `Regelgroep`/`recursief` extension tokens take. The corpus and
  every committed source are free of these words. Since form 2 the capitalised
  phrase `Afwijking van` is one whole-phrase token as well: a name that is read
  word by word — an object type, an attribute, a characteristic — can no longer
  contain it (a syntax error), while a name word `Afwijking` without `van`
  behind it still lexes as a name. A rule name runs up to `geldig` over any
  token, so it can still contain the phrase (`Regel Afwijking van de norm`
  emits a rule named 'Afwijking van de norm'); only at the head of a block does
  the phrase open a deviation. It occurs in no committed source besides the
  header of `jutterswet-bergingsdrempel`.
- An exception whose validity window differs from its base refuses loudly:
  the defaults group carries one window and a per-default window is
  unrepresentable — split per window or align the windows.
- An UNCONDITIONAL base rule (no `indien`) lowers to a default carrying the
  vacuously true justification — an `ALLE`-group over no parts, the same
  "altijd" the hand-written prioritized-defaults anchors spell. WF-050
  requires every non-initialisation default to carry a justification, and a
  flat rule without a condition is the ch. 7.8 standing norm: as the lowest
  default of a group it stands whenever nothing above it decides.

---

## §5 The subtype extension — `is een`

Origin: `spec/proposals/SUBTYPING.md` §9.2, under the maintainer's mandate for
object-type hierarchy (2026-09-10). The uchar IR carries `Objecttype.supertype`
— one direct parent by declared name (`spec/IR-SPEC.md` ch. 2.2,
`spec/RUNTIME-SEMANTICS.md` ch. 7.10). Official RegelSpraak v2.3.0 has **no**
inheritance construct at all: a flat document can only repeat a supertype's
members on every subtype, which is what the canonical data model this corpus
mirrors does *not* do. This dialect extension adds the source channel for
declaring the relation the IR already carries.

### Surface syntax (additive; one optional element in an existing production)

```
Objecttype het Wrakrecht is een Strandrecht (mv: Wrakrechten)
    de bergingsdiepte Numeriek (niet-negatief geheel getal);
```

The clause sits **after the object-type name and before the `mv:` group**. It
does not touch the order of the existing head annotations, which the grammar
already fixes (`mv:` before `(bezield)`), and both remain reachable behind it.

**No new lexer token, deliberately.** A whole-phrase `IS_EEN : 'is' WSP 'een'`
is not safe here and the `IS_VAN_HET_TYPE` precedent does not carry over.
Lexing is not head-local: under maximal munch such a token fires wherever the
phrase occurs, and the phrase already occurs in ordinary **rule text** ("Een dag
is een schouwdag"), so it would change the IR of documents that have nothing to
do with subtyping. `IS_VAN_HET_TYPE` is safe only because its phrase occurs
nowhere else. The extension therefore sits at the **parser** level: the existing
`IS` and `EEN` tokens in sequence at the head position, where the surrounding
context disambiguates. A §13.3.2 kenmerk declaration is `IS lowerNaam …`, and
`naamWoordLower` excludes `EEN`, so no member declaration can be read as this
clause and no clause can be read as a member. The extension consequently
reserves **no** name words: unlike `Uitzonderingen`/`ongeordend`, both its
tokens already existed and their other uses are untouched.

The article before the parent name is the language's own `onbepaaldLidwoord`,
read exactly where the language reads one elsewhere; it is required, so a bare
`is <Naam>` is not the head form and does not bind.

### Lowering (resolver)

`is een <Naam>` records a parent NAME; the binding happens once every
declaration has been read, so resolution is **document-wide and order-free** —
a forward reference is legal and only an undeclared name refuses. The bound
parent lands in `Objecttype.supertype`, whose chain accessors (`keten`,
`isEen`, `attribuut`, `heeftLid`, `kenmerkSoort`, `declarantVan`) the model
already carried; the member maps stay OWN, so a subtype declares only what it
adds.

### Fail-fast refusals (resolver, loud diagnostics)

The frontend refuses BEFORE the verifier does, because it has the chain at
hand. Each refusal cites its rule:

- **WF-106** — more than one direct supertype, in either spelling (two
  `is een` clauses on one head, or `is een A en B`). The grammar deliberately
  ADMITS both shapes so the refusal can name the parents instead of degenerating
  into a syntax error, and the message names the two known
  multiple-inheritance cases (`RechtOpUitkeringWazo`, `RechtUitkeringZw`) as
  refused rather than flattened. This refusal is one of that rule's only two
  witnesses anywhere: on the wire `supertype` is a bare string, so a second
  parent cannot be WRITTEN, and WF-106 carries no verifier check and no
  `corpus/negatief` case (`spec/IR-SPEC.md` ch. 2.2). A head repeating ONE
  parent is redundant, not a second parent, and is not refused.
- **WF-104** — self-reference (`X is een X`), and a cycle of two steps or
  more, reported with its named path. The back edge is cut after it is
  reported, so every later pass terminates on a document already rejected.
- **WF-103** — a parent name nothing declares as an object type. When the
  document declares that name as something else, the message says which kind
  (a domein, a parameter, a feittype, a dimensie, an eenheidsysteem, a
  dagsoort) rather than sending the author hunting for a typo.
- **WF-105** — a member name declared twice along one chain (ci, per kind) is
  refused, at the redeclaration's own line. What is **not** refused, and
  deliberately, is the **sibling** collision — two incomparable subtypes of one
  supertype each declaring the same name — since no instance is of two
  incomparable types, so no read is ambiguous.

A misspelled head form is a bare ANTLR "no viable alternative", which points
the author at the wrong thing, so two common slips get a targeted message
beside it (never instead of it): `is <Naam>` with the article dropped — an `IS`
after the name is otherwise a §13.3.2 kenmerk declaration, but that continues
with a `lowerNaam`, so a capitalised name can only be this — and the clause
written behind the `mv:` group. Both are read off the token stream over the
HEAD only, so the `is een` of ordinary rule text is never mistaken for either.

Explicit **non-**refusal: a target carrying rules on two levels. That is the
kernel's group inheritance (`RUNTIME-SEMANTICS.md` ch. 7.10), and the kernel
witnesses are the semantic referee.

### Emission

`supertype` is emitted ONLY for a type that has one, directly after `naam`
(the schema's property order). A root type emits no key, so the v0-mirror
byte-identity of every flat golden holds.

One emission consequence comes with the relation rather than with the syntax.
A role's declared filler type is an **upper bound** (`IR-SPEC.md` ch. 2.6), so
WF-048's uniqueness is computed over the **chain-closed** type-pair set: adding
a subtype can make a pair that was uniquely connected doubly connected, and a
fold that went out bindingless yesterday would be refused today. The emitter is
ahead of it — where the by-pair search goes silent, the fold's `bronRol` falls
back to the role the sentence WROTE, which still names exactly one.

That fallback carries one tightening the other role searches do not need, and
it is what makes it safe. The name candidates are formed by dropping leading
words, so a truncated phrase can land on a same-named role in **another** fact
type; the upper-bound filler test that is right everywhere else would wave that
through, because the element type is a subtype of the other role's filler. The
reached role must therefore declare **exactly** the element type — identity, not
`⊑`. A phrase naming no role, naming more than one, or naming one whose filler
is not the element type itself stays bindingless, and gets the loud WF-048
refusal. Binding the wrong relation would instead be verifier-clean and
silently fold the wrong set, which is by far the worse of the two.

### Scope / limitations of this card

- Only `Objecttype` carries the clause. `Extensie van objecttype` (§13.3.1.2)
  re-opens a type to add members and is left alone: a second declaration site
  for one relation would immediately reopen the second-parent question across
  constructs.
- Multiple inheritance is refused, not modelled (mandate decision 2).
- The clause declares the relation only. Nothing about the defaults-stack
  composition is authored from source; that is the kernel's, keyed on the
  relation this extension writes.

---

## §6 The filled-value-count extension — `het aantal gevulde waarden van`

Origin: `spec/IR-SPEC.md` ch. 4.3 and `spec/RUNTIME-SEMANTICS.md` ch. 3.4c
(maintainer decision G9, 2026-09-19). The uchar IR carries an aggregation kind
`filledCount` (in memory `AANTAL_GEVULDE`): over one set source it counts the
contributors whose value is not leeg, so `{5, 7, leeg}` answers **2** where the
instance count answers 3.
The kind was written on the wire first and answered by all four executors
before any frontend could author it; this extension is its producer route.

**Official RegelSpraak v2.3.0 has no spelling for the operation at all.**
§5.8's `<getalaggregatiefunctie>` is a closed alternation of four phrases (the
count, the maximum, the minimum, the sum), with two more for dates; there is no
fifth number form and no place to write one. `gevuld` exists in the language
only as a PREDICATE — §8.1.2, typing §5.11, a boolean test on one subject
expression — and never as an aggregation. So a flat document can neither write
this fold nor mean it.

### Surface syntax (additive; one new alternative of `functieAanroep`)

```
Het aantal gewogen vondsten van een Jutter moet berekend worden als
    het aantal gevulde waarden van het gewicht van alle vondsten van de Jutter.
```

New token: `AANTAL_GEVULDE_WAARDEN_VAN`, a whole-phrase token
(`'het aantal gevulde waarden van'`). The operand is a `primaireExpressie`, for
the same precedence reason the §13.4.17.47 telling gives: a trailing
`plus`/`maal` binds OUTSIDE the count, which is what an author means. The
alternative carries **no facet slot** — no `leegwaardeAlternatief`, no
`dimensieSelectie`, no `conditieBijExpressie` — because all three are ill-formed
on this kind (WF-037's "sum only" and "sum/min/max only", and decision G7). A
facet that cannot be written needs no refusal.

**The extension reserves no name word.** Unlike `Uitzonderingen`/`ongeordend`
(§4), a whole-phrase token takes only the whole phrase: `gevulde` and `waarden`
keep lexing as `LOWER_ID` everywhere else, and an attribute or role whose name
merely begins with "gevulde" is untouched. Only the exact five-word sequence
changes meaning — and under maximal munch it takes precedence over
`HET AANTAL_WORD`, which is the risk this section measures.

### Inertness — measured, not assumed

The `is een` extension (§5) could argue inertness from the shape of the
construct; this one cannot, because the phrase *extends* an existing one. A
document naming a role or attribute "gevulde waarden …" would have its count
sentence re-read. So the claim is a measurement, taken 2026-09-20 over **876
flat RegelSpraak documents** in seven collections: the 64 emitter goldens of
this corpus, the 47 official-casus documents of the repository's temporary
parity zone, 176 documents of the reference toolchain's own corpus, 513
documents of an operational ruleset collection, and 76 more across three
further rule collections found on a second pass — plus the six markdown
documents of the v2.3.0 specification set (the main specification, the typing
document, the syntax-diagram document, the ANTLR-targeted draft, the casus
document and its conversion notes).

- **The phrase occurs nowhere.** Zero occurrences of `aantal gevulde` in any of
  the 876 documents, and zero in the six specification documents.
- **The word `gevulde` occurs twice in 876 documents**, both inside `//` line
  comments in the operational collection, where the lexer cannot reach it. The
  single specification hit is the substring inside "ingevulde" in a prose figure
  caption, not a RegelSpraak name.
- **Every document emits byte-identically before and after.** Each of the 876
  was run through the frontend built at the commit before this extension and at
  the commit after it, and the emitted IR plus the diagnostics compared. Zero
  reinterpretations. Two TEXT changes appear and are accounted for, neither of
  them a reading: WF-096's message lists the admissible kinds and that list grew
  by one name, and the ANTLR "expecting {…}" set in a syntax-error message names
  the new token. Normalising exactly those two leaves no residual difference in
  any document.
- **The seventh collection was found by a second reader, not by the first.**
  Three of the seven were missed on the first pass, and the sentence that
  claimed the sweep was exhaustive was therefore too strong. It is recorded here
  because the lesson outlives the number: a coverage claim over "everything
  reachable" is one a second reader has to re-count, and that re-count found
  three. The re-count agreed — 76 of 76 identical, no diagnostic differences —
  so the conclusion widened rather than moved.

**What the measurement does not claim.** It cannot exclude that some unmeasured
document names a role or an attribute with the exact phrase. What bounds that
case is the grammar itself, and both of its failure modes are LOUD:

- a colliding **role or count sentence** — the phrase consumes its `van`, so
  what follows must be the fold's operand, and there the remainder is a to-ONE
  reading rather than a set reading, which the WF-037 refusal below rejects at
  ERROR level;
- a colliding **attribute name** never reaches a rule at all: the phrase is a
  token, so a member declaration containing it breaks on its own declaration
  line with a syntax error ("extraneous input 'het aantal gevulde waarden
  van'"), and the document does not resolve.

The shift is loud in both, never a different answer. That pair — measured
absence plus a bounded failure — is the whole argument, and both halves carry a
fixture (`AantalGevuldeDialectTest`).

### Lowering (resolver)

The phrase builds an ordinary `Expr.Functie` with `kind = AANTAL_GEVULDE` and
one argument, reusing the value folds' machinery rather than adding any:

- the element type is the argument's own object type (`aggregatieElement`), the
  VALUE-fold derivation — not the count's, whose element word is a role name;
- the range is the shared one: a single written step goes out as `sourceRole`,
  and a resolved multi-step range as `sourcePath` (WF-102), which is ch. 3.4b's
  flat union and the reason the kind exists at that width;
- the `die/dat` subselectie rides the shared `aggregatieFilter` path;
- the result type is a whole, unitless, non-negative number whatever the folded
  attribute measures (ch. 3.4c). The source's unit is deliberately NOT inherited;
  a cardinality has none.
- `meldAggregatie` carries an explicit no-check arm for the kind. Decision G8
  makes the test typeless — presence is asked of a text, a date, an enumeration
  value and a boolean alike — so a numeric-operand check here would undo the
  decision in silence.

`RuleChecker.VOUWSOORTEN` grows with the kind in the same movement, because
WF-096 admits a set reading on a value fold's source alone and a filled count
is a count in its RESULT but a value fold in its READING. That set and the
verifier's `set_source_exempt` are one boundary in two languages;
`VouwsoortenGrensTest` now READS the verifier's own kind list and the line that
keys on it, so the two cannot drift again in either direction.

### Fail-fast refusals (resolver, loud diagnostics)

- **WF-037** — a source that is not a set reading. The kind reads a value per
  contributor, so a to-one source ("… van het gewicht van de Vondst") would ask
  for the filled count of one own value, which is 0 or 1 and a form no author
  writes. ERROR, so the document never reaches disk; every other kind admits a
  non-set source for a reason of its own (the §7.1 timeline fold, the row-1
  count wart), which is why the fence is drawn per kind.
- The refusals the value folds already carry reach the new kind unchanged,
  because it joined the shared aggregation predicate: a source naming no
  declared member of the element type markers with a WARNING, and the three
  fold-source refusals (a nested set step, an intermediate to-one anchor, a
  by-pair binding the written name contradicts) keep refusing here too.

### Emission

Nothing about emission is new: the kind reaches the wire through the same
`Functie` writer as the other seven, with the same `bronRol`/`bronPad`
exclusivity. A document that does not write the phrase emits exactly what it
emitted before, which is the measurement above stated as a property.

### Scope / limitations of this card

- Only the ONE-operand form exists. There is no listed variant ("het aantal
  gevulde waarden van A, B en C"), because the wire has none: WF-037 pins the
  arity at exactly one.
- **The token matches lowercase `het` only**, as `AANTAL_DAGEN_IN` and
  `TOTAAL_VAN` do, so a sentence-initial "Het aantal gevulde waarden van …"
  falls back to the pre-extension reading — the instance count swallows the
  words as its element name and markers with a WARNING. Measured 2026-09-20:
  that document still reaches disk, exactly as it did before the extension. It
  is no regression (the base behaviour is unchanged) and no aggregation phrase
  in this grammar is written sentence-initially, but it is a real asymmetry with
  the lowercase form and is recorded rather than discovered.
- The temporal set source stays refused downstream, not here. A filled count
  over a timeline attribute has no moment at which to ask, and the two OCaml
  executors refuse it by name (`corpus/rood/aantalgevulde-temporele-setbron`);
  the frontend admits the sentence, as it does for the other folds.
- The catala and flogic frontends do not get the kind. Each carries its own
  refusal list, and a wire kind they never write costs them nothing.

---

## §8.4.3 A time window's bounds — narrowed to the wire's two shapes

Not a dialect extension and not inert: a narrowing of what v2.3.0 spells.
§13.4.15.68 gives a periodevergelijking ("vanaf …", "van … tot …", "tot en met
…") any date expression as a bound, and the frontend used to build every one of
them into a window. WF-111 (`spec/IR-SPEC.md` ch. 4.6, maintainer decision
2026-09-23) admits two shapes on the wire — a date literal, or an own scalar
date attribute of the subject — both of day precision; the general date
expression is an OPEN wire decision (`spec/RUNTIME-SEMANTICS.md` ch. 3.12).
Measured before the fence (2026-09-23): a parameter, a navigated read, a
datum-tijd attribute, a computed date ("zijn peildatum plus 1 dg"), a
time-dependent attribute, a datum-tijd literal and an attribute of another
object type each reached disk with `diagnostics: []`, and the verifier refused
every one. `RuleChecker.periodeGrensFout` refuses them now with an ERROR in the
verifier's WF-111 sentence for the bound it would write, after the written
bound, so the document never reaches disk. No source in any zone writes a
window with such a bound, so nothing moves.

**"Own" is written on the subject, and compared by identity.** WF-111's text
admits an attribute "on an object type the subject satisfies" (`S ⊑ O`). This
frontend writes an own read — an inherited attribute included — with the
subject as its object type: "zijn peildatum" in a rule on a subtype is an
`AttribuutRef` on that subtype. A read that names any other type, a supertype of
the subject included ("de peildatum van de Jutter" in a rule on a subtype of
Jutter), goes out as a navigation, with by-pair bindings when a fact type
relates the two types, which the verifier refuses as navigated (WF-111). Where
no fact type relates them the emitter refuses the model (measured 2026-09-25),
so no document carries that bound and its refusal keeps the Dutch words, which
for the supertype-named read say how to read the subject's own date instead.
So the fence compares by identity, as the Drools gate
`Tijdrekening.periodeGrensLowerbaar` does; with `⊑` it would pass the
supertype-named read on to a verifier that refuses it. Witness:
`PeriodegrensTest`, `StructuurZinTest`.

**The window's position — WF-112** (fix round 2, 2026-09-23). v2.3.0 lets a
"het is de periode …" bullet stand in any compound condition (§13.4.15.69),
under any quantifier. The wire does not: a window gates the RULE, so under
'geen van de' or a counting quantifier ('ten minste', 'ten hoogste',
'precies') its verdict would be complemented or counted, and no reading is
specified for that. Measured before the fence: the bullet under each of those
four quantifiers, and under a 'geen van de' sub-compound of an 'alle' compound,
reached disk with `diagnostics: []`, and the verifier refused every one.
`RuleChecker.keurVensterposities` now walks every condition the emitter writes —
the reach of the verifier's own walk, conditions nested in expressions included
— and refuses each composite or quantifier fold of that kind that holds a
window, with an ERROR naming the quantifier, in the verifier's WF-112
sentence. Under 'alle', or as the rule's own
voorwaardendeel, the window stays admitted. No source in any zone writes the
refused form. Witness: `VensterKwantorTest`.

**A window bullet written last in a compound subselection** (same fix round).
The grammar ends such a subselection at the bullet's words "het is de periode",
reads them as a characteristic of that name (a marker), and takes the bound as
the RULE's voorwaardendeel: a window on the whole rule, which is a different
document from the one written, while the warnings name only the marker. The
same bullet written earlier in the list is read as a window inside the filter.
The frontend refuses the split with an ERROR at the rule; the grammar is not
changed. Witness: `RegeldoelTest`.

---

## §13.4.4 A rule target is written on the subject — narrowed

§13.4.4 gives a gelijkstelling an `<attribuutvanonderwerp>` target, whose
onderwerpketen may run through roles ("De vakpremie van het strandvak van een
Jutter …"). This frontend builds a target on the rule's universal subject only,
and such a sentence used to come out as an attribute named "vakpremie van het
strandvak" on Jutter — the name swallowed the navigation — reaching disk with
`diagnostics: []` for the verifier to refuse under WF-022 and WF-034. An
undeclared attribute target did the same, and so did a characteristic whose
subject is a navigation, which resolves to no object type at all. Since fix
round 2 (2026-09-23) each is an ERROR naming the form
(`RuleChecker.keurRegeldoelen`). An undeclared characteristic on a resolved
subject is not fenced here: that is the ground of the dialect card for negated
decisions ("is niet X"). No source in any zone writes the refused forms. A
refused target inside an extension block speaks alone: the defaults lowering
skips its family, whose checks would otherwise judge a target the author did
not write and add a second refusal ("is geen kenmerktoekenning of
gelijkstelling", "schrijft een ander doel") beside the true one (fix round 3).
A target that navigates from an implied subject ("De premie van zijn vakhouder
…") is refused the same way; it ended in the emitter's exception until the fix
round of 2026-09-26. A name that is both an attribute and a characteristic of
the subject type decides no member and is refused in WF-022's sentence
(`StructuurZinTest`). Witness: `RegeldoelTest`.

**A cell of a dimensioned attribute as the target** (fix round 3). The wire has
no cell assignment: a rule's target is an attribute reference, which WF-022
demands, and the dimension-cell node is a read (IR-SPEC ch. 4.3). "De premie
van een Jutter in voorjaar moet gesteld worden op 10" put the label into the
subject text and wrote the WHOLE attribute — verifier PASS on a different
document than the one written. It is refused now with the cell named
(attribute, dimension, label), and so is "De premie in voorjaar van een Jutter
…", which was refused before as an undeclared attribute without naming the
dimension. A whole-attribute write of a dimensioned attribute, with no label at
all, stays unfenced: the verifier admits it, `ref` and clingo write the value as
a scalar, and the Drools exec branch fails at fire time — a norm question for
the verifier line, recorded in the card. Witness: `DimensieDoelTest`.

---

## §13.4.1 A navigated read binds its written role

Not a dialect extension and not inert on every document: the reading this
frontend takes where v2.3.0 leaves the choice open, and a narrowing where it
defines nothing. A fact type declares each role by a name of its own beside
the filler type's name (§13.3.9), so a role may carry its type's name and one
role name may occur in two fact types. v2.3.0 lets a subject reference name an
object type or a role (§13.4.1), and a role navigates the fact type it belongs
to (§5.5.5); it does not say which reading wins for a noun that is both, and
it gives a read that names another object type than the rule's no navigation
at all (a reference by type name points back to a subject of that type,
§5.5.3). The frontend reads it this way (maintainer decision 2026-09-26):

1. A written role name binds the one relation in which a role of that name,
   reachable from the subject, is filled by the type read — however many
   relations join the two types. This was already so.
2. A noun that is the other type's name and also the declared name or
   declared plural of a role binds that role when the type pair alone does
   not decide, as a written role does: "het kadegeld van de haven" on a
   Schuit, where one of two relations between Schuit and Haven names its
   Haven role `haven`. Until 2026-09-26 the type reading won, the pair was
   ambiguous, and the emitter ended in an exception.
3. A type name that names no such role binds by the type pair, and only where
   exactly one relation joins the two types. That derivation is a convenience
   of this frontend; v2.3.0 has no such navigation.
4. Every other navigated read is refused with an ERROR
   (`RuleChecker.keurNavigatiebindingen`, over what the emitter writes, from
   the subject it writes each read from). Where the read names another type
   than the subject's, the refusal is the verifier's WF-053 sentence for the
   document without a path (`an attribute reference without a path is the
   subject's own read, …`); where it names the subject's own type — a written
   role that two relations of a self-relation carry — or has no subject, that
   document is the subject's own read, which no reader refuses, and the
   refusal keeps this frontend's words. The Dutch part before the sentence
   says what did not bind: no relation, a reciprocal one, several of which the
   written name picks none, or a written role or chain.

A fold's range follows the same order: the count source by its written name
and then by the pair, a set reading by the pair and then by the written name
(whose role must declare the element type itself). A set reading that writes
the element type's name or its declared plural ("de som van de romplengten
van alle Schuiten van de Haven") claims no role, and binds by the pair where
exactly one relation joins the two types, as case 3 does; with several it has
no range and is refused below. Until the fix round of 2026-09-26 the declared
plural was not taken for the type's name, and one relation gave a marker with
the WARNING of a written name the pair contradicts. That WARNING remains for
a written name that names another role than the one relation carries ("alle
geborgen vondsten" where only `aangemelde vondst` joins the types): the pair
would fold another set than the sentence names, so the fold stays a marker.
A source that writes a step beyond its element from an anchor other than the
subject ("de som van de romplengten van alle liggers van zijn thuishaven" on a
Schuit) has a range of two steps from another object, which this frontend
does not spell; the refusal's Dutch part names that anchor. A quantifier fold
("ten minste twee van de liggers van de Haven") binds its range the same way,
by the role its member phrase writes where the pair does not decide; until the
fix round of 2026-09-26 the count words were offered as the written name, the
fold stayed a marker over two relations, and its WARNING said no fact type
joined the types. A fold without a range is
refused with an ERROR in the loader's sentence (`a single-argument fold with
an elementType carries its range … : the range is written, never derived`),
and under a total whose element is another type in WF-083's sentence; a total
over the subject's own type without a range keeps this frontend's words. The
other folds the wire cannot spell are refused the same way, each in the
sentence for what the emitter would write: a step or chain written on the
source ("het aantal liggers van zijn thuishaven", "het totaal van het kadegeld
van zijn thuishaven") in WF-053's (`a path on a fold source is ill-formed …`),
any fold but a total over the subject's own attribute ("de som van zijn
romplengte") in the loader's, and a total over another type's attribute ("het
totaal van het kadegeld van de Haven" on a Schuit), whose step a total without
a range drops, in WF-083's. The first two ended in the emitter's exception;
the third went to disk with `diagnostics: []` and the verifier refused it.

A read whose binding reaches a to-many role in a slot that takes one value is
refused with an ERROR too, in the verifier's WF-053 sentence (`hop 0 reaches
the to-many role … in a scalar slot — a to-many step needs an aggregation,
never a silent first pick`), the one the set fence speaks for a set reading.
Before, such a read went to disk with `diagnostics: []` and the verifier
refused the document. A §13.4.7 fact creation's condition on the created
relation's target ("… is een schuit van de Haven indien de romplengte van de
schuit …") is such a read: the wire reads that condition from the anchor, so
the target is reached over the to-many role. Where the target is named as its
type, case 2 now binds its role, and the sentence ends in this refusal instead
of the emitter's exception.

An allocation's recipient join (IR-SPEC ch. 3.2, `connectingRole`) binds in the
same order: the role the recipient phrase writes ("de bergingsaandelen van alle
hoofdbergers van de berging"), where it names one relation between the source
and the recipient type, else the one relation between the two types. An
allocation that binds neither — a type name with two relations — is refused
with an ERROR in the loader's sentence for the document without the join
(`missing field 'connectingRole'`). Until the fix round of 2026-09-26 the join
came from the type pair only, and two relations ended in the emitter's
exception whatever the phrase named.

A parent-scoped uniqueness binds its scope (IR-SPEC ch. 3.6, `scope`) by the
written names as well: the parent role the sentence writes ("van de
thuishaven") where it names one relation from the element, however many
relations join the two types, else the element role it writes where that
names the element's role in exactly one of the relations carrying the parent
role ("de nummers van alle liggers van de thuishaven", where two relations
carry a role `thuishaven` and one a role `liggers`). A scope that binds
neither, such as a type name over two such relations, is refused with an ERROR
in this frontend's words: the document without the scope is a uniqueness over
every instance, which no reader refuses (measured 2026-09-26: the verifier
admits it, and admits it bound to the other relation). A scope whose parent
role is to-many is refused in the verifier's WF-054 sentence (`the scope role
… is to-many — a scope parent is one instance`); until the fix round of
2026-09-26 it went to disk with `diagnostics: []` and the verifier refused the
document. Each of these refusals, like the one of a scope over a reciprocal
fact type, is the rule's only message. Until the fix round of 2026-09-26 the
scope was bound by the type pair only, so two relations between the element
and the parent type left the rule with the WARNING that it was not lowered,
and the uniqueness was left off the wire.

Measured before the change (2026-09-26): with two relations between Schuit and
Haven, a read by type name, a read with no relation, and the noun of case 2
each ended in an `IllegalStateException` of the emitter, and so did a count or
a sum over the type's plural with two relations or none. Now case 2 binds and
the others end in the ERROR; every rgs golden and every source of the parity
zone re-emits unchanged (gates 10 and 11), since each read that binds now threw
before. Witnesses: `NavigatiekeuzeTest`, `TelbereikTest`, `SubtypeDialectTest`.

---

## §13.4.17.47 The count's operand list — an ACCEPTING superset, fenced in the resolver

Unlike the three sections above this is **not** a dialect extension and it is
**not** inert: it is a production that accepts more than v2.3.0 spells, and it
was found by a wire decision rather than designed. It is recorded here because
the register is where a divergence has to be findable, whether it was chosen or
inherited.

`functieAanroep`'s `aantalFunc` alternative reads

```
HET AANTAL_WORD primaireExpressie ( COMMA primaireExpressie )* ( EN primaireExpressie )?
```

while §13.4.17.46's EBNF gives `<getalaggregatiefunctie>` one `<expressie>`
operand and §5.8.1 ranges the count over **instances**; the typing document
(§4.11) adds that a count takes onderwerpexpressies only, so leegwaarden do not
apply to it. There is a listed spelling in the source language — "de eerste van
A en B" (§5.8.3) — but it belongs to the ordering folds, and the count has
none. The `( COMMA … )* ( EN … )?` tail is therefore this frontend's own
widening, carried over from the neighbouring `minMaxFunc` and
`eersteLaatsteFunc` alternatives that legitimately have it.

**Two divergences live at this rule, and only one of them was registered.** The
`primaireExpressie` operand (instead of the symmetric `expressie`) is a
deliberate, narrowing reconstruction — a trailing `maal`/`plus` binds OUTSIDE
the count, which §4.11 requires — and it is noted at the rule. The operand LIST
is the widening, and until 2026-09-20 it was noted nowhere.

**What it cost, measured 2026-09-20.** `ExpressieBouwer.args` builds every child
as its own argument, so the list reached the wire as an `AANTAL` with two or
more `argumenten`. Three sentences emitted unblocked — the `en` spelling and the
`comma` spelling with `diagnostics: []`, the element-binding spelling ("het
aantal vondsten van de Jutter en kavels van de Jutter") with a WARNING that does
not stop emission — and the uchar verifier rejects all three: under WF-037 since
that day's count-arity decision (`spec/IR-SPEC.md` ch. 4.3), the third under
WF-034 as well. One executor answered such a document and three refused it,
which is the class `spec/PLAN.md` §6 calls a spec fault.

**The fence stands in the resolver, not in the grammar** — `ExpressieBouwer`'s
`tellingAriteitFout`, an ERROR, so `Resolver.irModus` writes nothing and the
ill-formed document never reaches disk. Witness:
`AantalOpgesomdTest` (both spellings, the element-binding route, the
off-disk property, and the two forms that must stay clean).

Why not simply delete the tail:

- It is a **parse** change, not a diagnostic change. `( EN primaireExpressie )?`
  decides how a trailing conjunction binds inside a larger sentence; removing it
  re-associates those sentences instead of refusing them, and 64 byte-identical
  emitter goldens sit downstream of every parse.
- Mirror-then-diverge: the widening is a fact about this grammar, and a fact
  that is deleted stops being registered. Fenced and written down, it stays
  readable; deleted, the next reader rediscovers it from scratch.
- The refusal is the same either way. What differs is only whether the author
  gets a named refusal in the verifier's sentence, or a syntax error.

This is the same posture `AantalSetbronTest` records one decision earlier: when
the wire narrows, the frontend's fence moves and its grammar does not.

### Half of it stopped being a divergence (2026-09-20, the unie-telling card)

**The tail is a widening of the EBNF and it always was. What was wrong was the
reading of what it widened INTO.** §13.4.17.46 gives the count one `<expressie>`
operand, and the register above concluded from that — and from §5.8.1 ranging
the count over instances — that a count has no articulated operand list at all.
That is true of an operand list of VALUES and false of the sentence: §5.7 spells
a **concatenation of subject expressions** — the instance sets the selections
yield, joined into one new set — and the typing document's own §4.11 says of the
count operator that a concatenation of expressions may be used with it,
unbounded in number, subject expressions only.
The previous card cited §4.11 for the half that excludes the value reading and
did not read the half that admits the concatenation.

So the grammar's `( COMMA primaireExpressie )* ( EN primaireExpressie )?` is now
**two things at once**, and the resolver is where they part:

- operands that are **rolselecties** ("aangemelde vondsten van de Jutter die
  gemerkt zijn") are the §5.7 concatenation. `ExpressieBouwer.bouwInstantieUnie`
  builds ONE `Expr.InstanceSetUnion` from them — a selection per operand, each
  with its own population, its own one-step relation and its own die/dat
  subselection — and the count carries it as its SINGLE argument. The wire form
  is `InstanceSetUnion` (uchar `IR-SPEC.md` ch. 4.3, WF-110). At this half the
  production is no longer a divergence: it spells what the source language
  spells.
- operands that are **waardelezingen** ("de gewogen vangst van de Jutter") stay
  the widening, and `tellingAriteitFout` keeps refusing them.

The refusal's message gained a TAIL rather than a rewrite, because its Dutch
head would have misled on its own: it said the source language spells no
listed form, which is true of the list and false of the sentence. Since
2026-09-25 the message is the verifier's WF-037 sentence for the listed count
after the written count, and the tail that named the §5.7 route for value
operands left with the head it corrected. Where the route opened and
`bouwInstantieUnie` refused it for a reason of its own, that reason stands in
the Dutch context before the sentence (`opgesomde telling '<zin>', geen unie
van selecties: <reden>: a count folds INSTANCES …`). Three such reasons, each an
ERROR that keeps the document off disk:

- **two populations** ("het aantal aangemelde vondsten van de Jutter en
  duinkavels van de Jutter"): one count answers a cardinality over ONE declared
  type (WF-110 (d)). The v0 toolchain refuses the same sentence on a mechanism
  ground of its own — one Drools accumulate has one source pattern — so the
  boundary agrees and the reason does not.
- **an underivable relation** for a selection (WF-110 (e)): a union names each
  selection's relation, never derives one, because every selection of one union
  shares one type pair.
- **a die/dat that is not a characteristic of the element**: a dropped filter
  would count too many AND the union would absorb the error, since an unfiltered
  selection is a superset of every filtered one over the same relation.

Where uchar deliberately carries what the toolchain markers: a **selection
without a subselection beside a filtered one**. Its marker there is the same
mechanism fact — it lowers a concatenation by OR-ing the operands' filter terms
and has no term to OR for an unfiltered operand — and our wire carries the
selections themselves, so `filter: null` is the ordinary case.

Witness: `AantalUnieTest` (the two-relation union, the two-characteristic union,
the unfiltered selection, the comma spelling, the wire shape, the three named
refusals, and the two counter-proofs). Measured red-first on the pre-card
frontend (5b42e5b9): both of this card's golden sources — `jutterswet-vondstunie`
and `jutterswet-keurunie` — were refused with the WF-037 arity ERROR and wrote
nothing.

### A type as the operand: no count over all instances (2026-09-23)

`aantalFunc` takes any `primaireExpressie`, so "het aantal Vondsten" — an
object type's plural with nothing it is counted from — parses as a count. The
card `frontend-typemeervoud-telling` asked whether the source language knows a
count over ALL instances of a type, and measured that it does not:

- the count takes a plural subject expression (§5.8.1; typeringen §4.11 admits
  subject expressions only), and §5.8 enumerates the plural ones — a
  concatenation (§5.7), a selection whose object or role selector is plural
  (§5.5.2, §5.5.5), a subselection (§5.6) — each hanging on a subject the rule
  binds per instance (§5.5.3);
- an object type stands in an onderwerpketen only in the singular, behind an
  article: as the universal subject the rule is applied to per instance, or as
  a reference back to it (§5.5.1, §5.5.3, §13.4.1). Its plural has no place
  there;
- the only document-wide range over a type, "van alle <objecttype>", is the
  uniqueness check's own subject expression (§8.1.6, its syntax in §9.5),
  admitted in a single criterion of a consistency rule and nowhere as an
  aggregation operand;
- the reference toolchain has no lowering for the sentence either — its
  resolver leaves it a marker and emits nothing without an explicit override —
  and no count in the official casus of the repository's temporary parity zone
  counts a type: its two count sentences count a role.

So the form stays refused, and nothing reaches the wire. The refusal is older
than the card (WF-037's source-less-count arm), but its sentence said that the
operand "names no instances to count", which is untrue for an author who named
a population. The card's sentence stated the norm and the way out — count
through a role from the rule's subject. Since 2026-09-25 the refusal is the
verifier's WF-037 sentence for a count without an element type, and the object
type the operand named stands in its Dutch context (`telling zonder bron
'<zin>', over het objecttype <T>: the count is an INSTANCE fold …`), for every
spelling that reaches it: bare, with an article or "alle", the type's own name,
the plural in lower case, in parentheses; the norm and the way out are this
section's. Only an operand that resolved to nothing declared qualifies, so a
parameter or a "Daarbij geldt" variable that shares a type's name keeps the
generic context. A subselection
over a bare type ("het aantal Vondsten die gewogen zijn") parses another way
and is refused by the subselection fence of §5.6 with its own sentence.
Witness: `TypemeervoudTellingTest`.

---

## §7 The opgave marker — `(mag opgegeven worden)` on an attribute

Origin: `spec/proposals/KENMERKINVOER-EN-INVOER-BOVEN-BEREKENING.md` §4.3 and
§5 (decided by the main session on 2026-09-25) and the card
`dialect-opgave-boven-berekening`, which built it. Case input on a computed
target is Reading A: a deciding writer displaces the input value
(`spec/RUNTIME-SEMANTICS.md` ch. 7.8), for the case as a whole. "Given, not
derived" — Reading B for one attribute, where a value the case supplies beats
the computation — is program structure in the uchar IR, the hardship pattern of
`spec/proposals/PHASE4-DEFAULTS.md` §4.1: the opgave as an input attribute of
its own and a defaults group that puts it above the computation. Official
RegelSpraak v2.3.0 has no declaration for either. This extension gives the
dialect one, as sugar: the marker changes neither the wire nor ch. 7.8, and
everything it lowers to is structure the wire already carries.

### Surface syntax (additive; one optional element in an existing production)

```
Objecttype de Vondst (mv: Vondsten)
    de taxatie Numeriek (getal met 2 decimalen);
    de waarde Numeriek (getal met 2 decimalen) (mag opgegeven worden);
```

New token: `MAG_OPGEGEVEN_WORDEN`, `'(mag' WSP 'opgegeven' WSP 'worden)'`, a
whole token with its parentheses like the declaration markers `BEZIELD`,
`BEZITTELIJK` and `BIJVOEGLIJK`. It is the last element of
`attribuutSpecificatie`, after the optional timeline and before the optional
`;`, so every clause of the production keeps its place. The grammar also admits
it at the end of both alternatives of `kenmerkSpecificatie`, so that the
resolver refuses it there with a message of its own instead of a bare syntax
error. The token reserves no name word: `mag`, `opgegeven` and `worden` keep
lexing as `LOWER_ID` everywhere else, and a comment, an enumeration literal or
a text literal that contains the sequence is never lexed as the token (witness:
`OpgaveMarkeringTest`, an attribute named "mag worden stand" and the text
literal `"(mag opgegeven worden)"`).

### Inertness — measured, not assumed

Counted on 2026-09-25, before the token existed: the opening `(mag` and the
whole sequence occur in none of the 81 `bron.rgs` sources of the corpus, none
of the 47 sources of the repository's temporary parity zone, none of the six
markdown documents of the v2.3.0 specification set, none of 660 `.rgs` files
in the operational rule collections and toolchain examples outside this
repository, and none of 164 in a checkout of the reference toolchain (build
output left out in both). In the tracked files of this repository the sequence
stands in prose alone: cards and proposals, no grammar, no test source, no
corpus file.

The method of §6: all 952 of those documents went through the frontend built
at the commit before the token and at the commits that added it and the
lowering, the emitted IR plus the diagnostics compared. Zero differences in
all 952: 233 emit an IR, 708 carry an ERROR (among them the scenario and
fragment files, which are no `regelSpraakDocument`), and the emitter refuses
11. Unlike §6 no syntax-error text changed either: the new token enters an
"expecting {…}" set only at the end of a member declaration, and no measured
document has a syntax error there.

What bounds an unmeasured document is the grammar: the token is admitted only
at the end of a member declaration, so anywhere else the sequence is a syntax
error on its own line, never a different reading.

### Lowering (resolver)

For a marked attribute X of object type T the resolver lowers five pieces, in
two passes of `RuleChecker`:

| Piece | Lowered to |
|---|---|
| 1 | attribute `opgegeven X` on T, X's datatype, right after X; no writer — the case supplies it |
| 2 | attribute `berekende X` on T, right after `opgegeven X`; every computing writer of X — a gelijkstelling or berekening, a row of a decision table, a defaults group of an `Uitzondering op` or `Afwijking van` tree, the sibling groups of an `Uitzonderingen ongeordend` block — keeps its name, shape, condition, window and statute annotation and targets `berekende X`; an unordered set moves as a set, every member still unordered |
| 3 | one `PrioritizedDefaults` `opgave boven berekening X` on X, validity always, from the top: the opgave default (justification the empty `all`, source `altijd`, as the anchors spell it; consequence `opgegeven X`; source `opgegeven X`), the computation default (the same with `berekende X`), and, where the author wrote an initialisation of X, that initialisation as the lowest default (`initialisation: true`, its value as consequence, its statute annotation as source); the initialisation rule leaves the rule list |
| 4 | the possessive characteristic `afwijkende opgave X` at the end of T's characteristics, attributed by the rule `afwijking opgave X` under one `all` of: `opgegeven X` filled, `berekende X` filled, `opgegeven X` notEqual `berekende X` |
| 5 | the consistency `controle opgave X`, whose assertion is the characteristic check of `afwijkende opgave X`, negated |

The declaration pass runs before any rule resolves: it refuses what the marker
cannot stand on (below) and declares the three members, so a rule can read
them — an author routes a deviating case by reading `afwijkende opgave X`, and
can read the materialised `berekende X`. The writer pass runs after the
defaults lowering of §4, so a tree or an unordered block moves whole, and
appends the rule, the group and the consistency behind the authored forms of
their lists. A read of X elsewhere stays a read of X: it reads the resolved
value.

The initialisation stays on X. Its justification is "X is empty in the input
facts" (ch. 7.3); on `berekende X`, which the case never supplies, it would
always hold, and an initialisation would then beat an input value of X and
count as a computation that an opgave deviates from. Where the computation
decides nothing the order is the flat one: an initialisation beside a
computation already decides only where the computation does not (the ch. 7.7
desugaring), and the marker adds the opgave above both. A computation that
has to leave room for the initialisation must be able to decide nothing:
`maal` counts an empty operand as 0 (ch. 1.2), so the golden's weight rule
carries "indien zijn gewicht gevuld is".

The source texts. The two generated defaults carry their block's name, so an
explanation that reports the deciding default says "opgegeven waarde" or
"berekende waarde"; `berekende X` keeps the computed value also where the
opgave decides. The rule of piece 4 and every read in it carry the texts the
frontend writes for the same rule written by hand ("Een Vondst heeft
afwijkende opgave waarde", "zijn opgegeven waarde is gevuld", …). The
consistency's source is the marked declaration up to the marker, with its line:
the one place the author wrote it. A moved writer keeps the source text of its
own target ("De waarde van een Vondst").

The contradiction. An opgave that differs from a filled computation attributes
`afwijkende opgave X` and deposits one §9.5 fact, `controle opgave X` × the
subject (ch. 5.6); an opgave equal to the computation, or an opgave without a
computable value, deposits nothing.

### Fail-fast refusals (resolver, loud diagnostics)

Each is one ERROR at the line of the marked declaration, naming the first
reason that holds, and the marker then generates nothing (witness:
`OpgaveMarkeringTest`, every sentence literal):

- the marker on a characteristic — the positive direction holds without it,
  since input on a characteristic stands against every decision (ch. 7.8), and
  the negative direction needs an opgave with three values, which the case
  format does not have for a characteristic ("kenmerk 'K' van T:
  '(mag opgegeven worden)' staat alleen op een attribuut — …");
- an attribute that is no Numeriek without a unit, the unit written in the
  datatype, after a domain, or carried by the domain ("… staat alleen op een
  Numeriek attribuut zonder eenheid, en 'X' is Tekst — …"): every attribute
  defaults target of the corpus is a number without a unit, so that is the
  one family the three executors are measured on;
- a time-dependent attribute (WF-086) and a dimensioned one (a defaults group
  on a dimensioned attribute does not play in every executor);
- an allocation, or an object creation's initialisation, that writes X: the
  marker moves neither, and beside the group each is a second writer (WF-050);
- a writer of X on another type of T's inheritance line: the ch. 7.10
  composition of the generated group with that type's writers is not built;
- a generated member name that T's inheritance line already declares, and a
  writer of a generated member — `opgegeven X` is the case's, `berekende X`
  the moved writers', `afwijkende opgave X` the generated rule's — each with
  both places in the message ("(bronregel N)");
- a generated form name another form already carries, the one another marker
  generates included: the three form names are unique in the whole model
  (WF-020), so two marked attributes of one name on two types collide — the
  message names both;
- more than one initialisation of X, or one with a window other than always or
  with several versions: the group carries one initialisation default and no
  window per default;
- an initialisation of X with a condition ("… heeft een voorwaarde — de
  initialisatiedefault van de gegenereerde defaults-groep is alleen
  gerechtvaardigd waar 'X' in de invoer leeg is, en draagt geen eigen
  voorwaarde (WF-050) …"): the initialisation default is justified by an empty
  input alone (ch. 7.3) and carries no written justification (WF-050).
  Lowered without its condition, the initialisation decided where the condition
  fails: measured 2026-09-25 on an initialisation of 25,00 under `indien zijn
  soort gelijk is aan "wrakhout"` beside a computation that decides nothing, a
  Vondst of another kind got 25,00 in ref, clingo and the Drools exec branch
  where the unmarked document leaves the target empty, with no diagnostic and a
  clean verifier. The condition is refused, not dropped.

And a WARNING: the marker on an attribute that no rule computes or sets (an
initialisation alone included) — X is input already, the marker generates
nothing, and the three declared members are withdrawn. Where a rule reads one
of them nonetheless, the name would read nothing, and the WARNING is an ERROR
naming the name read.

### Emission

Nothing about emission is new: the generated attributes, characteristic, rule,
group and consistency go out through the writers every other form uses. A
document that does not write the marker emits exactly what it emitted before,
which is the measurement above stated as a property.

### Scope / limitations of this card

- A number without a unit only. A date, a text, an enumeration or a number with
  a unit waits for a golden per family that plays in the three executors.
- No marker on a characteristic: the three-valued opgave of the proposal's §5.8
  is a card of its own.
- No retargeting of allocations and object creations, and no composition with
  writers elsewhere in the inheritance line.
- The generated names are fixed by the attribute's name; they carry no type
  name, so the form names of two marked attributes of one name collide.
- A computation of X that reads X, directly or through another attribute, is a
  dependency cycle the verifier refuses under WF-045 with or without the marker;
  with the marker the cycle runs through the generated group and WF-046 and
  WF-051 speak as well. Measured 2026-09-25. The frontend gives both the same
  WARNING, `kringverwijzing tussen berekeningen: Vondst.waarde -> Vondst.waarde
  — …` for the direct self-read, in the author's names, and still writes the
  document, as it does for every cycle (witness: `ZelflezingKringTest`). The
  clamp idiom "x = x, met een maximum van y" is such a self-read too, and so is
  a computation whose condition reads its own target: the verifier refuses both
  under WF-045 as well (measured the same day). The frontend's graph mirrors the
  reads of RUNTIME-SEMANTICS ch. 5.1 and 6.1 on the rule's own subject: what a
  rule's condition reads is an edge as much as what its value reads, and a
  characteristic is a node as much as an attribute, so a cycle through a
  condition over another attribute and a characteristic attribution reading its
  own characteristic are warned as well. A cycle that only the lowering closes
  runs through a generated member: a computation of X that reads `berekende X`,
  which it writes once it has moved, or `afwijkende opgave X`, which the
  generated rule attributes from `berekende X`, directly or through another
  attribute. The verifier refuses it under WF-045, and a cycle through
  `afwijkende opgave X` under WF-046 as well, since the generated attribution
  reads a value, a strict edge; the frontend said nothing, because its graph
  ran over the authored forms only (measured 2026-09-25). Where a marker was
  lowered, the graph runs once more over the lowered forms — the moved writers,
  the generated rule and every defaults group — and such a cycle gets the same
  WARNING in the names the author read, `kringverwijzing tussen berekeningen:
  Vondst.berekende waarde -> Vondst.premie -> Vondst.berekende waarde — …`; a
  cycle the authored forms already showed is not warned twice, since there
  `berekende X` counts as X (witness: `ZelflezingKringTest`). A document without
  a lowered marker never reaches that second pass. A navigated read, a read in
  the filter or test of a fold, a role check and a channel test are no edges of
  it; a read in a fold's argument is one, as in the verifier. And a node
  belongs to the type the rule is written on, where the verifier lets a write
  reach every comparable type of the inheritance line, so a cycle over a
  supertype and a subtype is no cycle of the frontend's graph: measured
  2026-09-25 on `x` of a Klok computed from its `y` and `y` of a Ding, its
  supertype, computed from its `x`, the verifier gives `WF-045 … zet x -> zet y
  -> zet x` and the frontend says nothing. A cycle only these close is refused
  by the verifier and not warned by the frontend.
- The semantic variant — a per-target marking on the wire that moves the input
  default of ch. 7.8 above the ordinary defaults — is the recorded alternative
  (the proposal's §5.9), not built.
- The explanation that reports the deciding default belongs to the consumers
  that explain defaults groups at all; the wire carries what they need.

Goldens: `jutterswet-opgave-boven-berekening` (two marked attributes, one with
an initialisation, and a rule reading the generated characteristic; ref,
clingo and the Drools exec branch replay its twelve cases, the DRL-text branch
refuses it on its defaults sentence) and `jutterswet-opgave-vormen` (the three
authored forms that are no plain rule under the marker: a tree with a nested
exception, an unordered block whose conflict is recorded under the moved
target, and the rows of a decision table; fifteen cases, replayed and refused
the same way).

---

## Vocabulaires en benamingen — named vocabularies and designations

Origin: the card `vocabulaires-benamingen` (maintainer decision 2026-09-27,
three decisions on the surface the same day, below). The uchar IR carries a
vocabulary side table (`spec/IR-SPEC.md` ch. 2.12): `vocabularies` on the
document and `designations` on the declarations a vocabulary can name — an
object type, an attribute, a characteristic, a fact type, a role, an
enumeration value (in `valueDesignations` beside its list), a parameter, a
named form and a rule group — each a name of that declaration in another
vocabulary: a term of a handbook, a field name of a data model. The table
carries no semantics. Official RegelSpraak v2.3.0 has no way to write one,
and a flat document can only carry such names in comments; this extension
gives the dialect the source channel.

### Surface syntax (additive; a new definition and one optional element in existing productions)

```
Vocabulaire handboek (taal: nl)
Vocabulaire cgm (standaard) (taal: nl)

Objecttype de Jutter (mv: Jutters) (bezield) (handboek: strandjutter) (cgm: JUTTER)
    het vondstbedrag (handboek: totale premie) (cgm: VONDST_BEDRAG) Numeriek (getal met 2 decimalen);
    is premiewaardig (handboek: komt in aanmerking voor premie) (cgm: PREMIE_WAARDIG) kenmerk (bijvoeglijk);
```

The head `Vocabulaire <naam>` is a definition beside the unit systems and
the day kinds. The name is one word. Each property stands in its own
parentheses, as `(mv: …)` does, in free order and each at most once: the
optional `(standaard)`, which makes the vocabulary the default, and the
optional `(taal: <code>)`, whose code goes to `language` as written;
`Vocabulaire handboek` without either is complete (maintainer decision
2026-09-27). A vocabulary has no kind: every designation is text, whether
the vocabulary serves a handbook or a data contract, and whether its
designations can serve as identifiers is the question of the consumer that
speaks it (maintainer decision 2026-09-27).

A designation is the annotation `(<vocabulaire>: <benaming>)`: free text
between the colon and the closing parenthesis, trimmed at both ends, with
spaces inside, and without a parenthesis. It stands at the very END of a
declaration's name group, in one fixed position the grammar holds as it
holds the mutual order of `(mv: …)` and `(bezield)` (maintainer decision
2026-09-27): on an object type behind `(mv: …)` and `(bezield)`; on an
attribute behind the name and its plural, before the datatype; on a
characteristic behind the name (and plural), before `kenmerk`; on a role
behind the role name and its plural, before the object type; on a fact type,
a parameter (before the colon), an enumeration value (behind its literal), a
rule and a rule group directly behind the name. The vocabularies of one
declaration's annotations stand in any order; the emitter writes a
declaration's `designations` in the declaration order of the vocabularies.

Four new tokens. `VOCABULAIRE` (`Vocabulaire`) is a keyword before
`UPPER_ID`, as `Dagsoort` is. `STANDAARD` is the whole token `(standaard)`,
as `BEZIELD` is. `BENAMING_START` is the opener `(<woord>:` up to its colon:
a parenthesis directly followed by a word — a letter, then letters, digits,
`_`, `-` or `'`, the characters of a name word — and a colon. `MV_START`
stands before it in the lexer and wins the tie on `(mv:`, so a vocabulary
cannot be named `mv`. The opener pushes a lexer mode in which everything up
to the closing parenthesis on the same line is ONE token,
`BENAMING_TEKST`: keywords, numbers, quotes and punctuation in a designation
are never lexed, so a designation cannot change the reading of what follows
it. A line end before the closing parenthesis ends the mode as a token of its
own, so an annotation left open is refused by name instead of swallowing the
next line. The language annotation `(taal: nl)` of a vocabulary head is the
same token shape, read by the resolver. The rule-name run (`regelNaam`) and
the decision-table name run stop at the opener, so an annotation written
there is not taken into a name.

### Inertness — measured, not assumed

Counted on 2026-09-27 over 638 distinct RegelSpraak sources (by content):
the rgs goldens and scenario files of this corpus, the sources of the
repository's temporary parity zone, the rule collections and toolchain
examples outside this repository. `Vocabulaire` occurs in none of them, and
`(standaard)` in none. The opener shape `(<woord>:` other than `(mv:`
occurs 60 times, all inside `//` line comments, where the lexer never
reaches it (a comment is one token of the hidden channel). In the six
markdown documents of the v2.3.0 specification set `Vocabulaire` and
`(standaard)` occur nowhere, and the opener shape 11 times, all in prose.

The method of §6: every one of the 638 documents went through the frontend
built at the commit before this card and at the commit that brought it, and
the emitted IR, the diagnostics and the exit status were compared. Zero
reinterpretations: 193 emit an IR, byte for byte the same, and 445 carry an
ERROR (fragments and scenario files among them, which are no
`regelSpraakDocument`), with the same diagnostics but for one TEXT change:
the ANTLR "expecting {…}" set of a syntax-error message names the new
tokens — `'Vocabulaire'` in 294 messages, `BENAMING_START` in 4. Normalising
exactly those two leaves no residual difference. Gates 10 and 11 hold the
same on their documents: the 99 older rgs goldens and the parity zone's
casus re-emit unchanged.

### Lowering (resolver)

The resolver (`Benamingen`) reads the side table in two passes. After the
declarations it reads every vocabulary head, then every annotation of every
designatable declaration, and adds each designation to the model's side
table (`RegelSpraakModel.vocabulaires`, `.benamingen`) under the declaration
— its kind, its owner where the kind is scoped, and its model name. After
the rule check, when the model holds every form the emitter writes, it
judges what needs the whole model: a rule's designation needs a named form of
that rule on the wire; the default vocabulary names every designatable
declaration; the effective names are unique per scope; a redundant
designation is warned. A designation never resolves: no rule reads the side
table, so a rule that writes a designation where it means a declaration is
refused as any undeclared name is — as a target by WF-022's refusal
("regeldoel 'De totale premie van een Jutter': 'totale premie' is geen
gedeclareerd attribuut van Jutter (WF-022, IR-SPEC ch. 3.1)"), in a read as
an unresolved name that stays a marker with its warning — and a designation
that spells the canonical name of another declaration reads that other one
(witnesses: `VocabulaireDialectTest`).

### Fail-fast refusals (resolver, loud diagnostics)

Every refusal is an ERROR on the line of the head or the annotation (a
declaration's line for the checks of the second pass) in a sentence of its
own, placeholders in angle brackets (witnesses: `VocabulaireDialectTest`,
every sentence on its line):

- `vocabulaire 'mv': die naam is gereserveerd, want '(mv:' opent de
  meervoudsvorm van een naam, en een benaming in dit vocabulaire zou als
  meervoud gelezen worden`
- `vocabulaire '<v>' is al gedeclareerd als '<eerder>'; de namen van de
  vocabulaires zijn uniek, hoofdletterongevoelig (WF-116)`
- `vocabulaire '<v>' draagt '(standaard)' twee keer`, and the same for
  `'(taal: …)'`
- `vocabulaire '<v>': '(<woord>: …)' is geen eigenschap van een vocabulaire;
  een vocabulairekop kent '(standaard)' en '(taal: <code>)'` — among them the
  `(soort: …)` of the card's first design, which the maintainer's decision
  withdrew
- `vocabulaire '<v>': de taalcode '<code>' is leeg of bevat witruimte; een
  taalcode is één woord (WF-116)`
- `de vocabulaires '<a>' en '<b>' zijn allebei het standaardvocabulaire; een
  document heeft er hooguit één (WF-116)`
- `de annotatie '(<v>: <tekst>' bij <declaratie> is niet op haar regel
  gesloten; een benaming staat tussen haakjes op één regel`
- `de benaming van <declaratie> in het vocabulaire '<v>' is leeg; een
  benaming is tekst tussen de dubbele punt en het sluithaakje (WF-117)`
- `de benaming '<b>' van <declaratie> in het vocabulaire '<v>' bevat een
  haakje; een benaming is tekst zonder haakjes, want het sluithaakje sluit de
  annotatie (WF-117)`
- `de benaming '<b>' van <declaratie> noemt het vocabulaire '<v>', dat het
  document niet declareert ('Vocabulaire <v>', WF-117)`
- `<Declaratie> draagt twee benamingen in het vocabulaire '<v>', '<a>' en
  '<b>'; een declaratie heeft hooguit één benaming per vocabulaire (WF-117)`
- `beslistabel '<t>' draagt een benaming, en een beslistabel heeft er geen:
  zij staat voor de regels van haar rijen, die hun namen van de frontend
  krijgen; schrijf de benaming op een regel`
- `het blok '<n>' draagt een benaming, en een uitzonderings- of
  afwijkingsblok heeft er geen: het wordt een default in de groep van zijn
  basisregel, en een default heeft geen naam op de wire; schrijf de benaming
  op de basisregel`
- `de regel '<r>' draagt een benaming, maar wordt op de wire geen benoemde
  vorm (een regel, creatie, verdeling, consistentie, uniciteit of
  defaultsgroep): een dagsoortdefinitie en een startpuntbepaling hebben geen
  naam, en een verdeling zonder methode wordt niet geschreven; zonder vorm
  heeft de benaming geen plaats`
- `<Declaratie> draagt geen benaming in het standaardvocabulaire '<v>', dat
  elke declaratie noemt (WF-119)`, with `; de frontend schreef deze
  declaratie zelf, zonder plaats voor een annotatie` where the declaration is
  one the frontend wrote (a row of a decision table, a member a marker
  generates)
- `<Declaratie a> en <declaratie b> hebben in het vocabulaire '<v>' dezelfde
  effectieve naam '<n>'; binnen hun reikwijdte is die uniek,
  hoofdletterongevoelig, zodat de weg terug naar de declaratie één antwoord
  heeft (WF-118)` — in the scopes of the canonical names: the object types,
  the fact types, the parameters, the named forms and the rule groups each on
  their own, the attributes and the characteristics of an object type with
  the ones it inherits (reported at the subtype's own member), the roles of a
  fact type, the values of a domain

<declaratie> is `het objecttype '<T>'`, `het attribuut '<a>' van '<T>'`,
`het kenmerk '<k>' van '<T>'`, `het feittype '<F>'`, `de rol '<r>' van
'<F>'`, `de enumeratiewaarde '<w>' van '<D>'`, `de parameter '<p>'`, `de
regel '<r>'` or `de regelgroep '<g>'`. A designation in a code-like
spelling and one with spaces are refused alike or not at all: there is no
kind to hold them to.

And a WARNING, which leaves the document on disk: `de benaming '<b>' van
<declaratie> in het vocabulaire '<v>' spelt haar canonieke naam en is
overbodig: de effectieve naam is dezelfde zonder haar (WF-120)` — for a
designation, in a vocabulary that is not the default, byte for byte the
declaration's own name. It quotes the designation, which is how the written
document accounts for it (WF-120); in the default vocabulary such a
designation is required and never warned, and one that differs in case is
the vocabulary's spelling.

### Emission

`vocabularies` stands after `format`, one `Vocabulary` node per head in
declaration order (`name`, `default`, and `language` when written); every
designatable node writes its `designations` as its LAST field, and an
enumeration's declaration its `valueDesignations` after `values`, value by
value. Each is written only where there is something to write, so a
document without the extension emits exactly what it emitted before, which
is the measurement above stated as a property (`VocabulaireDialectTest`
holds it on the golden's source with its annotations taken out, and gate 10
on the corpus).

### Scope / limitations of this card

- The nine kinds of the card and no others. A domain, a unit system, a
  dimension, a timeline, a day kind and a decision table carry no
  designation; an annotation on one of the first five is a syntax error, on a
  table and on an exception block a named refusal.
- A default vocabulary names every declaration, also the ones the frontend
  writes itself: the rules of a decision table's rows, the members and forms
  the opgave marker generates (§7), a start point's anchor. Those have no
  place for an annotation, so a document with a default vocabulary cannot use
  those constructs; the refusal says so. A vocabulary without `(standaard)`
  has no such limit.
- Not built, and deliberately: a service or an explanation per vocabulary, a
  status per designation (preferred, deprecated), a vocabulary per language
  beside another, an output that strips the annotations from a source, and
  designations in the case format, which names declarations by their
  canonical names only.
- Witnesses: the golden `jutterswet-vocabulaires` (both vocabularies on every
  kind; ref, clingo and both Drools branches replay its two cases, and gate
  13's vocabulary leg replays them stripped, exchanged and rotated), the
  frontend's `VocabulaireDialectTest` and the Drools backend's
  `VocabulaireWitnessTest` (the model's reading entry: `benaming`,
  `effectieveNaam`, `benoemd`).

---

## Scenariotoetsen — scenarios written in RegelSpraak, lowered to case files

Origin: the scenario layer of the source toolchain, its resolver and its IR
record brought into this frontend at the card `scenariolaag-in-uchar`
(2026-09-26). v2.3.0 has no test chapter, so this is an additive extension,
reachable only through the start rule `testDocument`: `regelSpraakDocument`
is unchanged, so production parsing and the re-emission of every golden are
untouched (gate 10), and a scenario block does not parse there
(`ScenarioParseTest`), nor does a parameter set (`ScenarioParametersetTest`).
The parameter set came at the card `scenario-parametersets` (2026-09-27):
the source toolchain read parameter sets from a JSON file beside a scenario
or a service descriptor, and here a set is a block of the scenario
document.

**A scenario is a case file written in RegelSpraak.** The lowering writes
one case file of the format uchar-case-v0
(`spec/schema/uchar-case-v0.schema.json`) per scenario, and the IR document
the scenarios run against beside them. The replay of the case files is the
runner: the executors replay them as they replay every other case (gates 5,
6 and 12). Nothing in the case format or in an executor changes. What the
source toolchain's runner added on top of the comparison — the fired rules,
the coverage per rule, the marker status — is a report of the Drools
backend's replay (`ScenarioResultaat`), not a form of this frontend.

### Surface

Three blocks, free among the definitions and rules of a test document.

A **situation**: `Situatie <naam>`, optionally `gebaseerd op de situatie
<naam>` once or more, then `:` and a situation block. A situation block
holds, in any order, instances and parameter values:

```
een Jutter J
- de naam is "Aukje"
- het vondstgewicht is 1,5 t
- de borgdagen is 4 van 01-01-2024 tot 01-02-2024
- de vondsten zijn V1, V2 en V3
- is borgplichtig van 01-01-2024 tot 15-01-2024
- heeft strandrecht
de parameter de vondstdrempel is 3
de parameter het penningbedrag is 7 van 01-01-2024 tot 01-01-2025
```

An instance opens with `een <objecttype> <LABEL>` (one capitalised word)
and carries bullets: an attribute value (`de|het <attribuut> is <waarde>`),
a to-one role (`de|het <rol> is <LABEL>`), a to-many role (`de|het <rol>
zijn <LABEL>, <LABEL> en <LABEL>`) and a characteristic (`is|heeft
[een|de|het] <kenmerk>`). A parameter value has no bullet. An attribute,
characteristic or parameter value may carry one period, `van <datum> tot
<datum>`: from inclusive, to exclusive, the case format's segment.

A **parameter set**: `Parameterset <naam>:`, then one or more parameter
rows in the form of a situation block and nothing else:

```
Parameterset jaarlijkse vaststelling:
de parameter de vondstdrempel is 12 van 01-01-2025 tot 01-01-2026
de parameter het penningbedrag is 250 € van 01-01-2025 tot 01-01-2026
de parameter de vondstdrempel is 14 van 01-01-2026 tot 01-01-2027
de parameter het penningbedrag is 275 € van 01-01-2026 tot 01-01-2027
```

The rows of a parameter without granularity are its **jaargangen**: each
holds its value in its period, from inclusive to exclusive, and a row
without a period holds on every date. The rows of a parameter with a
timeline are the segments of that timeline, as in a situation. A scenario
chooses one set in its header. The block depends on nothing but the
declarations: no situation and no scenario enters it, so a reader that
knows the declarations can read a set without the scenarios of the
document.

A **scenario**:

```
Scenario conflict
toetst de regels rubriek van een aanspoelsel en weegplicht bij dubbele borging
met peildatum 01-06-2026
gebaseerd op de situatie havens met hun schuiten
Gegeven is de volgende situatie:
<situatieblok>
Met als verwacht resultaat:
<verwachtingen>
```

The header is the name, then optionally a scope (`toetst de regel <naam>`,
`toetst de regels <naam>, <naam> en <naam>` or `toetst de regelgroep
<naam>`), optionally `met peildatum <datum>`, optionally `met parameterset
<naam>`, and `gebaseerd op de situatie <naam>` any number of times, in that
order; then optionally an inline situation, and the expectations. The name
ends at the first header keyword. The expectations, one bullet each:

| bullet | case expectation |
|---|---|
| `- de\|het <attribuut> van <LABEL> is <waarde>` | `attributeValue` |
| `- de\|het <attribuut> van <LABEL> is leeg` | `attributeEmpty` |
| `- de\|het <attribuut> van <LABEL> is <waarde> van <datum> tot <datum>` | one segment of an `attributeTimeline` |
| `- de\|het <attribuut> van <LABEL> is altijd leeg` | `attributeTimeline` with no segment |
| `- <LABEL> is\|heeft [een\|de\|het] <kenmerk>` | `characteristicPresent` |
| `- <LABEL> is\|heeft niet\|geen <kenmerk>` | `characteristicAbsent` |
| `- de gegevens zijn consistent` | `consistent` |
| `- de gegevens zijn inconsistent [volgens "<regel>" [voor <LABEL>]]` | `inconsistent` with its optional `rule` and `label` |

The segment bullets of one label and attribute are one `attributeTimeline`,
at the place of the first. The values are the literal forms of the language:
a number with the unit its declaration demands (`1,5 t`, `25 %`, `3/4
vadem`), a date `dd-mm-jjjj`, a date-time `dd-mm-jjjj hh:mm:ss.mmm`,
`waar`/`onwaar`, an enumeration value `'strandgoed'`, a text `"Aukje"`.

**Two new tokens, for the parameter set only.** The other forms this
repository added to the scenario layer reuse the lexer as it stands,
because a keyword token changes the lexing of every source that carries the
word. A period is `VAN DATE TOT DATE`, bare date
tokens only: a name word may be a number, so a date that may also be a
`NUMBER` would let a characteristic's `van`-segment compete for `van 2024`.
The empty timeline is `ALTIJD LEEG`. `volgens` is matched as a `LOWER_ID`
by its text, on the one position after `inconsistent` (a source carries the
word elsewhere as a name word). The rule of an inconsistency is a text
literal because a conflict channel's name (`Jutter.vrijgesteld/0`) is no
token run. `tot en met` is one token and is no period
(`ScenarioParseTest`). The parameter set takes two tokens:
`PARAMETERSET` (`Parameterset`) and `MET_PARAMETERSET` (`met
parameterset`, one whole-phrase token like `MET_PEILDATUM`). A block header
must end the block before it, and a header clause must end the name run
before it; an `UPPER_ID` is a name word in `kaleNaamWoord` and
`naamWoordAny` and a unit in `eenheidAfkorting`, so a bare `Parameterset`
would be read into the characteristic name that ends an expectation, or as
the unit of a number written without one (measured with another
capitalised word in that place: `strandrecht Jaarset tarieven` became one
name, `3 Jaarset` a number with a unit), and `met` followed by a
`LOWER_ID` runs on inside the scenario name and the single-rule scope.
Measured on 2026-09-27, before the parameter set's own witnesses: the word
`parameterset`, in any capitalisation, occurred in none of the 104
RegelSpraak sources of this repository outside the parity zone;
`Parametersets` stays an `UPPER_ID` and `met parameters`
stays `MET` `LOWER_ID` (`ScenarioParametersetTest`).

### Resolution

The non-scenario blocks go, in document order, through the one model
pipeline of the production route (`Resolver.bouw`), so the scenario route
builds the same model and, without a scope, emits the same IR document byte
for byte (`ScenarioLoweringTest`, and the six corpus witnesses below). The
scenario blocks then resolve against that model (`ScenarioResolver`):

- **Situations** resolve independently of their order in the document. A
  situation contributes its bases, each expanded in written order, then its
  own block. A basis that names no situation, a name declared twice, and a
  cycle of bases (`A` based on `B` based on `A`, or `A` on itself) are
  refused.
- **The merge**: a scenario's sources are its bases in written order, then
  its inline situation. One label is one instance; a later assignment of the
  same attribute, role or characteristic replaces the earlier one in place,
  a timeline as a whole; a later parameter value replaces the earlier one in
  place.
- **Parameter sets** are validated whether a scenario chooses them or not,
  each row typed as a situation's parameter value is. The jaargangen of one
  parameter share no day, in whatever order they are written; a row without
  a period shares every day, so it stands alone. The rows of a parameter
  with a timeline follow the timeline rules below. A scenario takes from the
  set it chooses every parameter the set carries: of a parameter with
  jaargangen the one whose period holds the peildatum — exactly one, and no
  peildatum or none that holds it is refused, for every parameter of the
  set whether or not a form reads it, as the case file carries them all —
  and of a parameter with a timeline the whole timeline; whether it covers
  the peildatum is the executors' session-start check where the document
  reads it at that date (RUNTIME-SEMANTICS ch. 8.4). A parameter that both the
  set and the merged situation give a value is refused: neither wins.
- **Typing**: every value is typed against its declaration by the rule
  pipeline's own assignment typing (`Typering.toekenning`), so a unit is
  required where the declaration has one and a value may carry no more
  decimals than its target. A convertible unit is restated exactly into the
  declared unit. An enumeration value must be one of its domain's values,
  compared without regard to case and written as declared.
- **Roles** resolve as the case readers resolve a role key: the written name
  names the other role of a fact type in which the instance's type fills a
  role, by its declared name or its declared plural. The verb follows the
  cardinality (`is` to-one, `zijn` to-many), and every label a role names
  must stand in the merged situation with a type that fills the role.
- **Characteristics**: the verb follows the kind (`heeft` for a possessive
  one, `is` otherwise). A time-dependent characteristic is delivered as
  periods only, as every case reader demands; a period belongs to a
  time-dependent attribute, characteristic or parameter only. The periods of
  one timeline stand in order and do not overlap.
- **The grid**: both bounds of a segment of an attribute, a parameter or an
  expected timeline sit on a wall of the declared grid — its granularity,
  count and fixed start point, and without a start point the calendar's month,
  quarter or year grid or every day — because every case reader holds a
  timeline's segments to those walls. A variable start lays its walls per
  object from an anchor a rule writes, and a week grid or a grid of several
  units without a date start lays none a day can be held against: those are
  left to the readers. A characteristic's periods count in days whatever its
  grid, as the readers take them and as the pinned anchor cases write them
  (the case schema's description of `characteristicTimelines` asks for the
  grid; the readers and the anchors do not).
- **Two gates**, over the forms the scenario carries: a form with a validity
  window, or one that reads the evaluation date, needs a peildatum; every
  parameter a form reads needs a value in the merged situation or in the
  chosen parameter set. Measured
  against the pinned corpus on 2026-09-26, `corpus/golden` with its anchor
  subset: none of the 12 cases whose document carries a validity window
  lacks an `asOfDate`, and of the 86 cases whose document reads a parameter
  one leaves it without a value (73 and none without the anchor subset).
  That one, `anchor/jutterswet-staffelvergoeding/case3-afwezig.json`, leaves
  the parameter out on purpose and pins the empty result: a scenario document
  cannot write it, since the gate refuses the absent value and the surface has
  no form for one. No form is needed (measured 2026-09-27): the parameter left out
  there is a list, a datatype RegelSpraak does not declare, and a scalar
  parameter that is read and left out faults in `ref` and clingo — binding is
  fail-fast (RUNTIME-SEMANTICS ch. 5.7), as the red case
  `aanvoergeld-parameter-ongebonden` pins — so the gate refuses what those
  executors would refuse, and a parameter set, which gives each parameter it
  carries a value, does not change that.
- **One document, one IR document**: every scenario of a document must
  carry the same forms, because one case directory has one `ir.json`.

### Lowering

`CaseEmitter` writes one case file per scenario with the emitter's own
serialiser, so a case file and an IR document share one shape.

- `asOfDate` is the peildatum, or null. The instances come in first-seen
  order with their attributes, roles and characteristics in first-seen
  order; `characteristicTimelines` is written when a time-dependent
  characteristic holds somewhere, after `characteristics`. That is the
  corpus's key order: the case schema lists the field before
  `characteristics`, and all 51 instances in the corpus that carry the field
  write it after (measured 2026-09-26).
- The parameters are those the chosen set gives on the peildatum, in the
  order of each parameter's first row in the set, then the situation's own.
  The resolver has chosen the jaargangen, so the case file carries one value
  per parameter without a timeline, and the IR does not change: a parameter
  without granularity has one value on the wire, and which one is a question
  of the case file, not of the document.
- A number is an exact lexeme in the declared unit. Without a restatement it
  is the lexeme the emitter writes for the written number, one normalisation
  for a case file and an IR document: a decimal keeps its written precision
  (`2400,00` is `2400.00`), a fraction is reduced (`6/8` is `3/4`, `1_1/2` is
  `3/2`) and a whole number where it reduces to one (`2_0/5` is `2`). A
  restated value is a terminating decimal without trailing zeros (`1,5 t` is
  `1500` kg), or else a reduced fraction (`1 voet` is `1/6` vadem), restated
  through the fractions the unit declarations write, as the wire carries them
  (`2 gtd` with `gtd = 1/3 etm` is `2/3` etm). A
  date-time carries its three millisecond digits, as the language writes
  every date-time and as the IR literal's lexeme does; a hand-written case
  whose date-time leaves the fraction out, which the case format admits (31
  of the 50 date-times pinned in `corpus/golden`), therefore does not come
  out of a scenario document byte for byte.
- The file name is `case<n>-<slug>.json`: `n` the scenario's 1-based
  position in the document, the slug its name with diacritics taken off,
  lowercased, every run of characters other than `a-z` and `0-9` one `-`,
  none at either end.
- **The scope lowers to the IR document** — a change of meaning against the
  old toolchain's scenario runner, which fired the whole document and used
  the scope only for coverage and for the marker and parameter gates; when an
  external rule set moved onto this layer (2026-09-26), 15 of its 36
  scenarios read values that rules outside their scope derived, and failed
  here for that reason alone — not to the case files: the
  document keeps the declarations, the day-kind definitions and the start
  points whole, and of the named forms — rules, object and fact creations,
  allocations, consistencies, uniquenesses and defaults groups — the ones the
  scope names. It keeps the diagnostics of its declarations and its forms: a
  diagnostic raised on a line of a source block whose every form the scope
  leaves out — a rule, a decision table, an extension block, a rule group — is
  dropped. A rule and a table are placed by name, an extension block by the
  source lines of its defaults group's nodes; a block that yields no named
  form (a day-kind definition, a start point, a rule that yields nothing)
  keeps its diagnostics, and so does a diagnostic without a line. A rule is named by its name without a version
  suffix, a decision table by its name (all its rows), a defaults group by
  the name of its base rule, a rule group by its name (its members). A rule
  group keeps its carried members and disappears without one. Without a
  scope the document is the production route's.

The CLI writes both: `Resolver --ir <out.json> --cases <map> <file.rgs…>`,
or `make ir FILE="bron.rgs scenario.rgs" OUT=<out.json> CASES=<map>`. The
sources are read as one test document. A model with an ERROR writes neither
the IR document nor a case file; each diagnostic names its file and the line
in it (`ERROR (<file>:<line>): …`), and no line of the chain of files. Every
file is decided before one is written, and a map that already
holds a case file this document does not write is refused (it is not
cleaned): a case left beside the new ones would be replayed against an IR
document it was not written for. A case file the run writes itself is
overwritten.

### Refusals

Every form that does not fit the case format, or the one IR document, is an
ERROR with a sentence and never a silent loss. Of the source toolchain's
contract, four forms are refused here: a peildatum with a time (a case
carries its evaluation date as a day); scenarios with different scopes in
one document; a scope in a document with a start-point determination (the
determination and the rule that writes its anchor belong together); and a
scope that names an exception block or a member of an unordered block (it
names the group by its base rule). Beside those, three value forms have no
case form: a dimensioned attribute (a case value carries no dimension
coordinates), a text value with an `«expressie»` (a case value is literal),
and a period bound with a time.

The extension's sentences are its own and Dutch: no reader refuses a case
file for these defects, because a case file that carries one is never
written. Where the typing of a value fails, the sentence is the rule
pipeline's, after the value's position (`het attribuut '<a>': toekenning: a
text value is assigned to a number target`). Each sentence carries the
prefix `scenario: `, placeholders in angle brackets:

- `de scenario's van één document lopen tegen één IR-document, maar het
  scenario '<b>' toetst andere regels dan het scenario '<a>'; zet
  scenario's met een andere scope in een eigen document`
- `de situatie '<s>' is meer dan eens gedeclareerd`
- `de situatie '<s>' is nergens in het document gedeclareerd`
- `de situaties vormen een kring via 'gebaseerd op': '<a>' → '<b>' → '<a>'`
- `de naam van het scenario '<n>' geeft geen bestandsnaam: zij draagt geen
  letter of cijfer`
- `het scenario '<n>' heeft geen situatie: het is nergens op gebaseerd en
  draagt geen 'Gegeven is de volgende situatie:'`
- `de peildatum '<d>' draagt een tijd, maar een casebestand draagt als
  peildatum alleen een dag (asOfDate)`
- `de peildatum '<d>' is geen geldige datum`
- `een scope in een document met een startpuntbepaling wordt geweigerd: de
  bepaling en de regel die haar anker schrijft horen bij elkaar, en een scope
  die er één van laat vallen legt de tijdlijn op een ander raster`
- `de regelgroep '<g>' uit de scope bestaat niet in het document`
- `'<g>' uit de scope is een regelgroep; schrijf 'toetst de regelgroep <g>'`
- `de regel '<r>' uit de scope is opgegaan in een defaultsgroep; de scope
  noemt die groep bij de naam van haar basisregel`
- `de regel '<r>' uit de scope bestaat niet in het document`
- `de regel '<r>' heeft een geldigheidsvenster, maar het scenario '<n>' heeft
  geen peildatum (voeg 'met peildatum <datum>' toe)`, and the same with
  `leest de rekendatum`
- `de regel '<r>' leest de parameter '<p>', die in de situatie van het
  scenario '<n>' geen waarde heeft`
- `het objecttype '<t>' is niet gedeclareerd`
- `het label '<l>' botst met (het eerste woord van) de objecttypenaam '<t>'`
- `het label '<l>' is eerder gedeclareerd als '<t>', hier als '<u>'`
- `het objecttype '<t>' kent geen attribuut '<a>'`, `… geen kenmerk '<k>'`,
  `… geen rol '<r>'`
- `het attribuut '<a>' is gedimensioneerd, en een casebestand draagt geen
  dimensiecoördinaten`
- `het attribuut '<a>' krijgt in dit blok zowel perioden als een vaste
  waarde`, and `… zowel een vaste waarde als perioden`; the same for a
  parameter
- `het attribuut '<a>' is niet tijdsafhankelijk; een periode ('van … tot …')
  hoort bij een attribuut met een tijdlijn`; the same for a characteristic
  and a parameter
- `het kenmerk '<k>' is tijdsafhankelijk; een casebestand levert het als
  perioden ('is <k> van <datum> tot <datum>'), niet als vaste waarde`
- `het kenmerk '<k>' is bezittelijk; schrijf 'heeft <k>'`, and `… is niet
  bezittelijk; schrijf 'is <k>'`
- `de rol '<r>' vanaf het objecttype '<t>' is niet eenduidig: meer dan één
  feittype kent haar`
- `de rol '<r>' is een naar-veel-rol; schrijf de 'zijn'-lijstvorm ('de <r>
  zijn …')`, and `… is een naar-één-rol; schrijf de 'is'-vorm ('de <r> is
  …')`
- `de rol '<r>' verwijst naar het label '<l>', dat nergens in de
  samengestelde situatie gedeclareerd is`
- `de rol '<r>' verwacht een <T>, maar het label '<l>' is een <U>`
- `de parameter '<p>' is niet gedeclareerd`
- `de parameterset '<s>' is meer dan eens gedeclareerd`, and `de
  parameterset '<s>' is nergens in het document gedeclareerd` (the parameter
  gate then stays silent for that scenario: what the set would have given
  cannot be said)
- `de parameterset '<s>' geeft de parameter '<p>' twee jaargangen die
  elkaar overlappen (<van <d> tot <d> | zonder periode> en <…>); de
  peildatum van een scenario kiest precies één jaargang`
- `de parameterset '<s>' heeft voor de parameter '<p>' geen jaargang die de
  peildatum <d> van het scenario '<n>' bevat`, once per parameter
- `de parameter '<p>' heeft in de parameterset '<s>' jaargangen, maar het
  scenario '<n>' heeft geen peildatum die er één kiest (voeg 'met peildatum
  <datum>' toe)`, once per scenario
- `de parameter '<p>' krijgt een waarde uit de parameterset '<s>' en uit de
  situatie van het scenario '<n>'; een parameter krijgt zijn waarde uit één
  van beide`
- the parameter gate of a scenario that chooses a set: `de regel '<r>' leest
  de parameter '<p>', die in de situatie van het scenario '<n>' en in de
  parameterset '<s>' geen waarde heeft`
- `de periodegrens '<d>' draagt een tijd, maar een periode telt in hele
  dagen`, and `de periodegrens '<d>' is geen geldige datum`
- `de periode van <d> tot <d> is leeg of loopt terug`
- `de periodegrens <d> van <het attribuut '<a>' | de parameter '<p>' | de
  verwachting voor '<a>'> ligt niet op het <maandraster | raster van <n>
  maanden | …> van de declaratie[ (vast startpunt <s>)]; een casebestand
  levert een tijdlijn in de perioden van dat raster`, once per bound
- `de periode van <d> tot <d> begint vóór het einde van de vorige (<d>); de
  perioden van één tijdlijn staan in volgorde en overlappen niet`
- `het label '<l>' in de verwachting is nergens in de situatie gedeclareerd`
- `het attribuut '<a>' is niet tijdsafhankelijk; een tijdlijnverwachting
  hoort bij een attribuut met een tijdlijn`
- `de verwachting dat '<a>' van <l> altijd leeg is, spreekt de perioden
  ervoor tegen`, and `de periode voor '<a>' van <l> spreekt de verwachting
  tegen dat het attribuut altijd leeg is`
- `de regel '<r>' van de inconsistentie bestaat niet in het document en is
  geen conflictkanaal ('<Objecttype>.<attribuut of kenmerk>', eventueel met
  '/<pad>')`
- `de verwachting '<bron>' is syntactisch niet herkend`
- after a value's position (`het attribuut '<a>'`, `de parameter '<p>'`,
  `de verwachting voor '<a>'`): `de datum '<d>' is geen geldige datum`, `een
  enumeratiewaarde ('<w>') past niet op een <type>`, `'<w>' is geen waarde van
  de enumeratie <domein> (<waarden>)`, `de waarde '<bron>' is niet gelezen`
  and `er staat geen waarde` (beside a syntax error), `de tekstwaarde draagt
  een «expressie», en een waarde in een scenario is letterlijk`. A number the
  parse cannot read and a restatement without an exact factor are no refusal:
  the parse enters a number on a number token only, and the typing admits two
  units with one canonical only, each with the exact factor its system
  declares; both stand as invariants.
- and from the CLI: `het document draagt geen scenario, dus er valt geen
  casebestand te schrijven`, and `de map <map> draagt al casebestanden die
  dit document niet schrijft (<bestanden>); ruim ze op of kies een andere map,
  want een casebestand naast de nieuwe speelt tegen een IR-document waarvoor
  het niet geschreven is`

### Witnesses

The frontend suite: `ScenarioParseTest` (the forms and the two fences),
`ScenarioTypeGateTest` (one refusal per defect on its line, and the merge,
the restatement, the role key and the timelines), `ScenarioMutatieTest`
(a clean document and nine mutations of it, a cycle and a duplicate among
them), `ScenarioLoweringTest` (a case file written by hand from the
schema, the file name, the scope and its IR document, the CLI) and
`ScenarioParametersetTest` (the two tokens, a case file with a jaargang and
a whole timeline written by hand, the exclusive end, and each refusal of
the parameter set on its line). The corpus:
seven goldens carry a `scenario.rgs` beside their `bron.rgs` whose lowering
reproduces their pinned case files, and whose scenario route reproduces their
`ir.json`, byte for byte — the handwritten expectations are the anchor —:
`ww-herleving-inkomen-rgs`, `jutterswet-uitsluiting-rgs`,
`jutterswet-nestkenmerk-rgs`, `jutterswet-dagentelling-rgs` (the timelines),
`jutterswet-rubriektabel-rgs` (a scope over a decision table and a rule, which
names every form of its document: a scope that leaves a form out writes an IR
document no golden pins, so the leaving out, the dropped diagnostics among it,
is witnessed by `ScenarioLoweringTest` alone) and
`jutterswet-dubbelverbinding` (a shared situation, roles written from both
sides with a plural key, a peildatum) and `jutterswet-jaarvaststelling` (a
parameter set with two jaargangen of a threshold, an amount, a percentage
and a date, chosen by two scenarios one day apart). Together they carry
every expectation kind of the case format.

---

## Producer refusals without a reader sentence

Where the verifier refuses the document a resolver refusal would have written,
the message is the verifier's sentence after a Dutch context prefix (the unit,
typing, fold-operand and value refusals, and the structural ones: a rule form, a
group, a declaration, a day-kind definition and what its condition reads, a start
point, a window's position and bounds, a count's shape, a set reading, a read
whose name took in a word before 'van', a constant the wire has no node for
("hij" written as a value, in WF-098's sentence), a unit's conversion factor
outside the wire's bounds (zero, negative, or past 2^62 - 1 along its chain,
in the loader's sentence), a role check whose written role names no relation
from the subject or several (in the loader's sentence for the check without
its binding, `missing field 'factType'`), a rule target whose name is both an
attribute and a characteristic of its type (WF-022), a written day-kind spelling
another day kind already carries (WF-017; a derived plural gives way instead), a
parent-scoped uniqueness whose parent role is to-many (WF-054);
witnesses `EenheidZinTest`,
`TyperingZinTest`, `VouwOperandZinTest`, `StructuurZinTest`), and
`LezerszinGrensTest` requires every English fragment of the resolver's and the
emitter's refusals to stand in the verifier's string literals, so a sentence the
verifier rewords turns it red. The refusals listed here keep their Dutch words
because no reader refuses that document, measured on 2026-09-25 by changing a
valid emission to exactly the defect, or by emitting it with the refusal
switched off, and running the verifier on it, or because no document can carry
the defect: a unit beside a unitless number literal at plus, min and assignment
(`€ plus 5`, `€ := 5`), where the verifier adopts the literal's unit and
RegelSpraak adopts it in a comparison only; plus or min where neither operand is
a date and one is no number; a percentage in a product, a quotient, a minimum, a
maximum, a sum or a sum's fallback (tables 11, 14 and 21 to 23 list no
percentage row) and as the operand of a square root or an absolute value (tables
16 and 20), and a number beside a percentage at equality or assignment where a
unitless literal adopts the '%'; a date in a minimum or maximum and a number in
a first or last (tables 22 to 25); one-sided or inconvertible units among the
operands of a minimum or maximum, and a total over a unit without a time
denominator (table 30), for which the verifier has no unit arm; more decimals
than the target admits, at an assignment and at an allocation share (§6.1.2),
and the family and unit checks of an allocation, since the verifier crosses no
decimals and no allocation types; a unit the unit model cannot spell (a
reciprocal, a rate factor, a rate divisor); a counting quantifier whose telwoord
is not a whole number, which the wire would carry as 0, and an exact-digit count
that is not one; a date or date-time token that is no calendar date, since the
frontend forms a wire lexeme from a valid date only and the verifier's lexeme
sentences (WF-084, WF-095) describe a written lexeme; a characteristic
initialisation in an object creation whose value is boolean-typed but not the
literal waar or onwaar (§13.4.6.4); an object creation of the subject's own type
whose sentence names no role for the new object, where the derivation from the
type pair links nothing (RUNTIME-SEMANTICS ch. 5.5) and the verifier admits the
unlinked document; the whole-period qualifier on a navigated characteristic or
on a condition that is no comparison, predicate or characteristic check; a
recursive group whose only creation member is a relation creation, which §9.10
does not count as the recursion's carrier and the verifier admits (ch. 5.5);
overlapping versions of a day-kind definition, which WF-021 does not compare (a
second definition of one day kind and a bounded window are WF-031's, refused
beside the overlap in its sentences); a navigated read that binds nothing where
it names the subject's own type or has no subject, and a total over the
subject's own type without a range, since the document without the path or
the range is the subject's own read (§13.4.1 above); a parent-scoped
uniqueness whose written names bind no single relation for its scope, since
the document without the scope is a uniqueness over every instance (§13.4.1
above); a window bound that
navigates where no
path is derivable — a read of another object type, a sub- or supertype of the
subject included, that no fact type reaches, or a written navigation without a
derivable path — since the emitter then writes no navigation: it refuses the
model, or writes the zero-hop self-reference as the subject's own read, which
the verifier admits; a set reading outside a fold source whose path the emitter
cannot derive or whose hops are all to-one, and a set reading as a count's
source that names no member of the element type; an attribute or parameter typed
with an undeclared domain, which has no wire spelling, since the wire writes a
domain's datatype where it is used; an extension block directly after a group's
last member over a base that is no group member, where the document is
well-formed with the block and the rules behind it outside the group; a member
of an `Uitzonderingen ongeordend` block that writes another target than the
members before it, since the lowering writes every member on the first member's
target, a well-formed document. Refusals of the source text itself (syntax, the
desugaring of decision tables and exception blocks, the dialect's fences — among
them a member of an unordered block that is no kenmerktoekenning or
gelijkstelling, or yields no rule at all — a timeline reference that names no
declared timeline, among them a multi-period form written without its start
date ("voor elke 2 maanden", which parses as the name of a timeline and was
written as a single value until the fix round of 2026-09-26), a unit clause
after an attribute's type that has no reading (a percentage, a value that is
no number, a number that carries a unit already; on a domain-typed number the
clause sets the unit, §3.4, where until that fix round it was parsed and
dropped), and forms this frontend does not lower — among them a uniqueness
scoped over a reciprocal fact type, which the wire writes as the binding of a
fact type with two roles and which ended in the emitter's exception until that
fix round) have no reader either, because no document carries them. The same holds
for the refusals of the scenariotoetsen extension (its section above): no reader
refuses a case file for them, because a case file that carries such a defect is
never written.

---

## §6.1.4 A one-sided clamp unit — written with a warning

A clamp whose base and bound carry a unit on one side only, a unit-bearing bound
over a unitless value or a unitless attribute bound over a unit-bearing value,
is refused by the verifier (WF-099: `clamp minimum and base have incompatible
units`, and the same for the maximum). The frontend writes such a document with
a WARNING in that sentence instead of an ERROR, because the casus of the parity
zone carries one such clamp and an ERROR would stop its emission; gate 11 lists
the replay slice that carries it as its one verifier holdout. A unitless number
literal as the bound adopts the base's unit and draws nothing, two inconvertible
units are an ERROR in the same WF-099 sentence, and a base or bound that is no
number is an ERROR in the verifier's WF-035 sentence (`clamp base is <kind>,
must be numeric`). When the parity zone leaves the repository, the warning can
become an ERROR like the other refusals the verifier shares.

---

## An undeclared attribute read — refused for one shape

The name of a read runs over every inner `van`, so a phrase the language has no
function for, "het maximum van de romplengte van de Schuit", reads an attribute
`maximum van de romplengte` of Schuit. The verifier refuses such a read (WF-034:
`'maximum van de romplengte' is neither an attribute nor a characteristic of
'Schuit'`). The resolver refuses it in that sentence where the name is no member
of the type it is read on and the part behind one of its `van`s is a declared
attribute of that type, or the written plural of one: the name then took in a
word before `van` (`StructuurZinTest`, at every position the emitter writes a
read at). A read of any other undeclared name is written, for the verifier to
refuse, because the casus of the parity zone reads names that only its replay
slices declare, and an ERROR would stop its emission; a WARNING would change its
pinned document. When the parity zone leaves the repository, the refusal can
cover every undeclared read.
