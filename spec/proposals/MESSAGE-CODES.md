# MESSAGE-CODES — a code for every message, apart from its text

*Status: PROPOSAL · 2026-10-18 · working prose, not normative. Card
`backlog/doing/20261017-meldingscodes.md`, point 1 and acceptance (a). One
architecture review by Fable (2026-10-18) preceded the normative slice (the
card's point 2). **Revised the same day:** the main session adopted Fable's
recommendations — the answers to the six questions, the findings and the
smallest slice — and this revision applies them before the slice is built;
§8 gives the decision and its reason per question. Nothing in this document
changes `IR-SPEC.md`, `RUNTIME-SEMANTICS.md`, the schema, the verifier, an
executor, a gate or the corpus; the slice does that.*

*Conventions of this document. Text in quotation marks is quoted verbatim
from the file named beside it. "Measured" marks a count made on `main` at
e2343945 by the method given in §1. "Proposal", "recommendation" and
"decision for the slice" mark conclusions of this document, not facts of
the repository. Fable's review is a working file outside the repository;
its parts are cited as "Fable Q1"…"Q6" (part 1, the six questions), "Fable
§2 (a)"…"(i)" (part 2, what is wrong or will hurt later) and "part 3" (the
smallest slice).*

## 0. What is given

The card asks for a design in which a message carries a code apart from its
text, so that gates read codes, people read text, and a second language
needs no second set of pins. The maintainer decided the following on
2026-10-18; this design takes them as fixed:

1. **Language.** What has its origin in RegelSpraak stays Dutch; the rest
   becomes English. Codes are always English. A diagnostic of the frontend
   on a `.rgs` source has a Dutch text by default and an English catalogue
   beside it. Messages of an engine — the verifier, `ref`, the clingo
   backend, the Drools backend — have an English text; the Drools backend
   gets a Dutch catalogue beside it, because the distribution delivers it.
   The distribution chooses the language (a CLI flag; `Accept-Language` in
   the service).
2. **Declared names** go into the text untranslated, as parameters.
3. **No text of an existing message changes** in the normative slice
   (the card's acceptance (e)).

Two consequences follow directly and are used below. The card's phrase
"English as reference" holds for the engines; for the frontend the decision
of 2026-10-18 makes Dutch the default text and English the catalogue
beside it. And a code may never be spliced into an existing text, because a
substring pin that spans the splice point would break (measured instances
in §3.4).

## 1. The problem, measured

All counts on `main` e2343945, 2026-10-18. OCaml call sites are counted with
comments and string literals stripped; Java sites with comments stripped.

**The red corpus (gate 8).**

- 50 case files `corpus/rood/**/case*.json` in 45 directories.
- `expectedFault.ref`: 50 pins, 34 distinct fragments.
  `expectedFault.clingo`: 50 pins, 35 distinct. `expectedFault.drools`: 26
  pins, 20 distinct. `ref` and `clingo` carry the identical fragment in 31
  of the 50 cases. 19 of the 50 cases pin a fragment that quotes a declared
  name, a label or a lexeme.
- The Drools leg's exclusion list in `scripts/check-rood.sh`: 23 entries
  (19 `EXCLUDE`, 4 `NOFAULT`), covering the 24 cases without a `drools`
  pin.
- **25 fault kinds** stand behind the 50 cases: 4 on the case-input channel,
  17 on the fault channel, 4 declined forms (table in §4.1). Method: each
  case was copied with its fragments replaced by an unmatchable string, so
  that `ref` and the clingo runner print their full report; the kinds were
  read from those reports and from the `drools` pins.
- For 7 of the 50 cases the clingo backend fails for a reason that is not
  the case's kind: twice a refusal of a form it does not translate
  (`begrensd-reads-bounds-in-order`, "a Clamp minimum bound that is not a
  ground literal"; `som-fallback-always-evaluated`, "a sum over listed
  operands is not translated"), five times its own integer range
  (`dagsoortstap-buiten-bereik`, `datumtijdstap-buiten-bereik` cases 1–4,
  "exceeds clingo's integer range"). *(Corrected 2026-10-18 at the slice:
  the first count said 6 and "four times", while the list it gave names
  five cases.)* For 3 more cases (§4.1, EF-006) it names the kind at
  another position. All 26 `drools` pins name the case's kind.
- The bottom-sentence list of gate 8: **36 sentences**, 12 in
  `ref/lib/eval.ml` and 24 in `backends/clingo/lib/translate.ml`.

**The verifier (gate 3).**

- 511 negatief directories over 111 rules; **354 `expect.txt`** over 71 of
  those rules, 327 distinct fragments, 201 of which quote a declared name.
- `spec/verifier/lib/checks.ml`: 435 calls `v "WF-###" …` over 104 rules,
  plus 6 records written with `wf = "WF-###"`. The report line is
  `WF-### FAIL <pointer>: <message>` (`spec/verifier/bin/verifier.ml`).
- `spec/verifier/lib/loader.ml`: 53 `fail` sites (the `LOAD ERROR` of a wire
  document, read by `ref` and clingo too).

**Gate 12 and the Drools backend.**

- `EXPECTED_CHANNEL` in `scripts/check-drools.sh`: 48 entries, 45 `EXCLUDE`
  and 3 `NOCASE`. By their first clause the 45 are 37 loader refusals over
  7 forms (`Datatype:ListType` 15, `Datatype:StructType` 7,
  `Datatype:SumType` 6, `functionDefs` 4, `Expr:Match` 2,
  `Expr:TemporalRead` 2, `Kwantorvouw.bronPad` 1), 5 transpiler `SKIP`s and
  3 bean-name collisions. `EXCLUDE_KNOWN` is empty. The script compares the
  channel only: "Only the CHANNEL is compared; the reason is documentation
  (as in EXCLUDE_KNOWN above) and may be reworded without tripping the
  gate." (`scripts/check-drools.sh`, comment above `EXPECTED_CHANNEL`).
- Refusal sites in `backends/drools/src/main`: 202 `new Refusal(` (183 in
  `TmsTranspiler`, 16 in `IrLoader`, 3 in `DrlRendering`) and 30
  `new Excluded(`. `IrLoader.Refusal` already carries a `form` field beside
  its message ("form not carried by the v0 Drools backend: " + form).
- Roughly 258 assertions on message text in the backend's tests (a grep for
  `getMessage()` followed by `contains`/`startsWith`/`equals`; a rough
  count).

**`ref` and the clingo backend.**

- `ref/lib`: 263 `fault` call sites (exception `Value.Fault of string`);
  `ref/lib/case.ml`: 71 `fail` sites (exception `Case.Error of string`, the
  case reader).
- `backends/clingo/lib`: 358 `fault` sites (341 in `translate.ml`), all
  raising `ref`'s `Value.Fault`; 56 lines in `translate.ml` that write a
  `viol/2` guard. The clingo runner reads the case through `ref`'s case
  reader (`Uchar_ref.Case`), which is why the case-input refusals read the
  same in both.
- `ref`'s fault sites mix three classes under one exception: normative
  faults ("division by numeric zero"), contract faults on a document the
  verifier would reject ("unknown function %S (the verifier should preclude
  this)"), and refusals of forms `ref` does not carry ("expression kind not
  implemented in this card").

**The frontend.**

- `RegelSpraakModel.Diagnostic(Severity severity, String message, int line)`
  in `frontends/regelspraak/src/main/java/dev/uchar/regelspraak/model/`.
- 132 `new Diagnostic(` in 7 main files; 5 of them sit in the wrappers
  `error` (in `RuleChecker`, `DefinitionResolver`, `Designations`,
  `ScenarioResolver`) and `refuse` (in `DefinitionResolver`), which are
  called 121 times. That makes **248 places where the frontend makes a
  `Diagnostic`**. `Severity.ERROR` occurs 94 times, `WARNING` 41, `INFO` 1.
- 211 assertions on `message()` text (`contains`, `startsWith`, `equals`,
  `matches`) in 49 test files.

**The §9.5 channel.**

- 64 golden case files pin `{"kind": "inconsistent"}` (optionally with
  `rule` and `label`). The channel fact is `{ rule; subject }`
  (`ref/lib/world.ml`); the reason string a producer passes is discarded at
  the insert (`ref/lib/eval.ml`, the `(fun w (_ : string) ->
  World.insert_error …)` of the rule loop).

## 2. Four families, their carrier and their reader

| Family | Normative | Code | Carrier | Reader |
|---|---|---|---|---|
| Faults of a session (the §9.5 channel and the other fault channels of RUNTIME-SEMANTICS) | yes | `EF-###` | the fault of each executor (`ref`, clingo, Drools) | gate 8 (red cases); later gates 5, 6, 12 through `inconsistent` (§4.2) |
| Rejections by the verifier | yes | `WF-###` with an arm key | `checks.ml` violation | gate 3 (`expect.txt`) |
| Refusals by an executor (a form it does not carry; a limit of its own) | no | one prefix per executor | the executor's refusal | gate 12, the Drools leg of gate 8, gate 8's fallback list |
| Diagnostics of the RegelSpraak frontend on a `.rgs` source | no | `RGS-###` | `Diagnostic` | the distribution; the frontend's tests |

### 2.1 Faults of a session — normative

A fault kind is a property of the semantics, not of an executor: "division
by numeric zero" is the same fault whether `ref` meets it at evaluation,
clingo at grounding or Drools at fire. **Proposal:** one code per kind,
`EF-###`, defined in a table of RUNTIME-SEMANTICS (the slice; draft in
§4.1), with its channel, its parameters and at least one red witness. The
code names the kind, not the phase: an executor may detect a kind at
reading, translation or evaluation (measured: clingo reports 27 of the 50
red cases as `TRANSLATION FAULT`, where `ref` faults after it has read the
case), and the code is the same.

The channels the table distinguishes, read from RUNTIME-SEMANTICS:

- **case input** — the case document is refused while it is read (the
  "loaders fail fast" of ch. 11.3; the case reader of `ref` and clingo);
- **fault** — the session aborts on a well-formed input: the hard runtime
  error of ch. 1.4 point 4, the session-start errors of ch. 5.7 step 2 and
  ch. 8.4, the divergence of ch. 5.7, the budget of ch. 5.5;
- **declined** — a form the specification leaves without a meaning and
  every executor refuses by name (ch. 3.12: "refused by name in all three
  executors"; ch. 3.4c; ch. 5.9's "loud until a corpus case lands");
- **§9.5** — "§9.5 inconsistencies are **facts on a channel**, not
  exceptions" (RUNTIME-SEMANTICS ch. 5.6); no abort (§4.2).

A code enters the table together with its red witness, never before: the
spec discipline "A spec sentence without a machine-checkable artifact is not
done" (`CLAUDE.md`) applies to a code as to a sentence. Normative faults
without a red witness today — the general pass budget of ch. 5.7 step 4,
"exponent %s is out of range", the fire cap of the Drools backend — keep
their text and get a code when a red case for them lands.

`ref`'s contract faults (a document the verifier rejects) are not
normative fault kinds: they are an executor noticing a WF violation. They
get executor codes of the kind `contract` (§2.3).

### 2.2 Rejections by the verifier — WF-### with an arm key

`WF-###` stays the code of a rule. Within a rule, an **arm** — one of the
violation sites of that rule — gets a fixed key, and `expect.txt` pins the
key instead of a fragment of the message.

**Proposal for the form:** `WF-###.<slug>`, the slug an English kebab-case
name of at most three words, unique within its rule
(`WF-001.case-collision`). Reasons for a slug and not a number: an arm has
no registry of its own, and a slug needs none to stay unique, where a
number would need one to avoid reuse; a slug survives the reordering and
splitting of arms in `checks.ml`; and a reader of `expect.txt` sees which arm
is pinned without a lookup. The violation record gets the key beside `wf`;
the report line becomes `WF-###.<slug> FAIL <pointer>: <message>`. Gate 3
keeps finding the rule (`WF-###` is a prefix of the key); its pointer
pattern `^WF-[0-9]+ FAIL ` widens to admit the key.

**What a bare key loses (revised after Fable's review, §2 (c)).** Today
201 of the 354 fragments quote a declared name, and that name is the only
thing that pins WHICH object the arm flagged: gate 3's pointer check only
verifies that a cited pointer resolves (RFC 6901), never that it is an
expected position, and `expect.txt` is pointer-free by rule. A bare key
pins the arm and nothing else. So the arm-key card gives `expect.txt` the
shape of §3.3: a key line plus optional `name=value` lines, matched as a
subset against the violation's parameters (the violation record gets `key`
and `params`). The card's gain is survival of rewording — the verifier has
no second language and no catalogue — against 441 sites and 354 files; it
may go last (§7).

*Landed 2026-10-19 (card `verifier-arm-keys`).* The count on `main` was 458
violation sites over 111 rules, not 441 over 104 (435 calls of `v "WF-###"`,
17 of a rule-local `v`, 2 direct records, 4 sites whose rule is a value).
`Checks.violation` carries `key` and `params`; `v` takes the key, so a call
with a bare `WF-###` raises where it is built and gate 3 refuses it in the
source. The report line is `WF-###.<slug> FAIL <pointer>: <message>`, the
message unchanged, and `verifier --arms` prints key, pointer and parameters as
JSON. All 354 `expect.txt` files are a key line plus `name=value` lines (213
distinct keys, 572 pairs); gate 3 matches the pairs as a subset against the
parameters of one violation, and refuses a key with two message templates. A
helper that builds a sentence for several arms (`resolve_binding`, `nav_hops`,
`literal_sign_refusal`, `wf109_host_sentence`, `fixed_start_point_refusal`)
returns the slug and the parameters with the sentence.

### 2.3 Refusals by an executor — not normative

A refusal says what one executor does not carry; another executor may carry
it. **Proposal:** one prefix per executor — `REF-###`, `CLINGO-###`,
`DROOLS-###` — numbered in that executor's own registry, with a `kind` per
code:

- `refusal` — a form the executor does not carry (the Drools loader's
  `Datatype:ListType`);
- `limit` — a limit of the executor's own representation (clingo's integer
  range);
- `contract` — the executor met a document the verifier would reject;
- `fallback` — the refusal of a fall-through arm: the bottom sentence of
  gate 8, "A BOTTOM SENTENCE is such an arm's refusal, and its defining
  property is that it is INVARIANT over everything the arm catches"
  (`scripts/check-rood.sh`).

The bottom-sentence list of gate 8 becomes the list of codes of kind
`fallback`; the fence becomes: no pin and no list entry names a `fallback`
code as what a witness witnesses. Because `expectedFault.code` is always an
`EF` code (§5), the only place a fallback code could appear as a pin is the
divergence list of gate 8 (§5) and the Drools lists, and the fence checks
those.

The registries (proposal): for `ref` and clingo, whose text stays English
and gets no catalogue, a small table file in the component (code, kind, the
function that raises it) with a carrier check in both directions — every
registered code is raised somewhere, every raised code is registered, the
check the bottom-sentence list has had since 2026-09-20. For Drools the
catalogue files of §6 are the registry.

**Code on demand (revised after Fable's review, §2 (d)).** Coding every
`ref` and clingo fault site (~690) serves no reader: there is no catalogue
for these two, and the gates read only pinned cases and the fence list.
Card 5 codes the 36 bottom sentences as `fallback`, the sites a pin or a
list names, and the `contract` class not at all — about 100 sites — with
the two-way carrier check over that coded subset. No executor code and no
registry is in the slice: its divergence list (§5) names a category, not a
code, and the `CLINGO` code column joins that list when card 5 lands the
registry.

### 2.4 Diagnostics of the frontend — not normative

`Diagnostic` gets `code` and `params`:
`Diagnostic(Severity severity, String code, Map<String, String> params,
String message, int line)`, with `RGS-###` codes. In the codes card the
`message` stays the literal Dutch text it is today, so the 211 text
assertions in the tests stay green without a test change; in the catalogue
card `message` is rendered from the catalogue of the chosen language
(default Dutch). A diagnostic that restates a WF rule may name it as a
parameter (`wf`), but keeps its `RGS` code: the frontend's sentence is about
the source, the verifier's about the wire.

`Diagnostic` is public API the distribution uses (measured in
`uchar-dev/regelspraak-toolchain` at cc9b4f9: eight main files read
`RegelSpraakModel.Diagnostic` or `.diagnostics`, and
`toolchain/.../engine/RegelSpraakEngine.java` constructs one). New record
components change the canonical constructor; the distribution meets that
one construction at its next pin bump. Its own JSON keys `fout`,
`schendingen`, `veld` and `soort` are out of scope (the card).

**`Diagnostic` is also a wire node (revised after Fable's review, §2 (b)).**
It is IR-SPEC ch. 2's `diagnostics` entry (`node, severity, message, line`,
`additionalProperties: false`, WF-113); 4 corpus documents and 2 toka dumps
carry entries, and the Drools backend compiles the frontend's `model/` and
constructs the record at four sites (`Coverage`, `PeriodCoverage`,
`RegelSpraakCompileException`, `IrLoader.diagnostic`, the last reading the
wire with an exact field list). Decision: `code` and `params` stay **off the
wire**; a code on the wire would be a spec card of its own (schema,
verifier, loaders, re-emission through gates 10, 11 and 16). Card 4 edits
the four Drools sites, so cards 3 and 4 share `backends/drools/` and run
one after the other, never in parallel (§7).

### 2.5 Outside the card's list

The IR loader's 53 `LOAD ERROR` sites, the refusals of the F-logic frontend
(`frontends/flogic`, exception `Refusal`) and those of the Catala frontend
follow the same scheme when a card takes them up. The loader sits in
`spec/verifier/` and fits the arm-key card best; the two frontends keep
their text until then.

## 3. The form of a code

### 3.1 Prefixes

| Prefix | Family | Registry |
|---|---|---|
| `WF-###` (+ `.<slug>`) | verifier, normative | IR-SPEC (rule); `checks.ml` (arm) |
| `EF-###` | fault kinds of a session, normative | the RUNTIME-SEMANTICS table |
| `REF-###` | refusals of `ref` | a table file in `ref/` |
| `CLINGO-###` | refusals of the clingo backend | a table file in `backends/clingo/` |
| `DROOLS-###` | refusals of the Drools backend | its catalogues |
| `RGS-###` | diagnostics of the RegelSpraak frontend | its catalogues |

`EF` stands beside `WF` on purpose: two letters, a hyphen, three digits,
and the first letter names the document (`W`ell-formedness in IR-SPEC,
`E`valuation in RUNTIME-SEMANTICS — "the evaluation session" of ch. 5.7
covers reading the input as well as the fixpoint). One namespace serves all
channels, with the channel as a column of the table, so that a kind that
moves channel by a later decision (as the negative weight of ch. 4.4 did,
"Decided (v1)") keeps its code.

The executor prefixes spell the executor, the way the case schema's keys
`ref`, `clingo` and `drools` do; they are longer than two letters because no
reader should have to learn an abbreviation for a component it already knows
by name.

### 3.2 Numbering

Sequential from 001 per prefix, in order of arrival; a code is never reused
and never renumbered; a retired code stays in its registry marked retired,
as WF rules do. No blocks per channel or chapter: blocks invite gaps and
quarrels about which block a kind belongs to, and the channel is a column.
The draft numbering of §4.1 orders the 25 kinds by channel and chapter; the
slice may renumber before anything lands.

### 3.3 Parameters

- Named, never positional: `{rule}`, `{attribute}`, `{objectType}`,
  `{label}`, `{parameter}`, `{value}`, `{decimals}`, `{lexeme}`, `{field}`,
  `{role}`, `{date}`. Names are English and use the wire's word where the
  wire has one (the convention of `CLAUDE.md`).
- A parameter value is a string: a declared name as declared (Dutch, as
  authored, never translated), a label as in the case, a value as its wire
  lexeme (`2026-03-04`, `40.005`). Catalogues do not format values; a
  date reads the same in every language.
- A code declares its parameters in its registry. A catalogue entry uses
  exactly that set; a catalogue gate checks it (§6).
- RUNTIME-SEMANTICS already states some parameter sets in prose: "The fault
  names the writers, the values they write and the attribute and instance;
  it names no cycle" (ch. 5.7). The table turns such sentences into a
  column.
- **Not emitted in the slice; mandatory before the fragments go (revised
  after Fable's review, Q4 (b)).** While the fragments stand nothing reads
  the parameters, and three executors would have to emit them. Before any
  fragment goes (card 5) they are mandatory: without them the two pairs of
  §8 question 4 collapse into duplicates, and 19 pins lose the object they
  identify. One machine line for all channels, `CODE EF-### {json params}`,
  matched as a subset against `expectedFault.params`. The slice's table
  names the keys; its preamble says the column is checked from card 5 on —
  the one declared-but-unchecked column of the slice.

### 3.4 Report format and transport

**Report format:** `CODE: text`, the code first, so that a gate reads the
code and a person reads the text. A runner may keep its phase label in
front of it (`EVALUATION FAULT: EF-005: division by numeric zero`,
`FAULT EF-005: Breuk deling door 0`). Several faults in one report (the
Drools backend's `EvaluationFaults`, several `viol/2` witnesses) list their
codes in report order: `EF-005, EF-016: …`.

**Transport: the code travels beside the text, never inside it.** The
exception, record or object that carries the message carries the code and
the parameters as fields; the runner renders `CODE: ` at the head of the
whole report. Two measured reasons, besides cleanliness:

- the Drools backend wraps a sentence in a rule prefix
  (`ComputeFault`: `"fout in de regel \"" + regel + "\": " + sentence`),
  and `corpus/rood/jutterswet-uitsluiting-zonder-rekendatum` pins
  `fout in de regel "defaults K:Jutter.uitgesloten": a validity-bounded
  form needs an evaluation date` across that seam;
- `ref` wraps the writers conflict in its pass-budget sentence
  ("no fixpoint within 100 passes — divergence is a fault (ch. 5.7): two
  writers of one target disagree: …"), and the red pin starts at "two
  writers".

A code spliced into the sentence would break both pins; a code at the head
of the report breaks no substring pin. That is how the slice meets
acceptance (e).

For the clingo backend's `viol/2` the same rule holds: the code is not an
argument of `viol/2` (the pin `viol("creation budget exhausted",oc0(` would
break), but known to the translator per guard reason and rendered by the
runner after it has diagnosed which guards fired.

**The carriers (revised after Fable's review, Q4 (a)).**

- *OCaml.* The only honest carrier is the exception payload:
  `Value.Fault of string` becomes `Value.Fault of { code : string option;
  text : string }`, and `Case.Error` likewise (EF-001..003 are raised
  there). A helper `faultc` (`failc` in the case reader) raises a coded
  fault; `fault` and `fail` stay uncoded. Pattern sites change
  mechanically, the two test suites included; a second exception type or
  a side channel would avoid that edit and leave two carriers forever.
- *Drools.* The production runtime's `Breuk.DelingDoorNul` gets no field
  for a backend concern: the red path classifies at the catch (a
  `DelingDoorNul` in the cause chain is EF-005), and only the backend's own
  exceptions get a `code()`. The red result carries the code beside the
  report (`RedResult.code`), and the status line renders it at the head:
  `FAULT EF-005: …` (the gate reads `tail -1`).
- *Two machine renderings, kept apart.* The head prefix above is for a
  person; a gate reads the `CODE` line of the ref and clingo runners
  (`CODE EF-005`, several codes comma-separated, `-` for none) and the
  Drools status line. The runner, not the shell, decodes the case's
  `expectedFault.code` and prints it as `CODE-PINNED EF-005`, the
  `--bottom-sentence` precedent: a JSON string may spell any character as
  an escape.

### 3.5 Alternatives considered and declined

- **A code inside the text.** Declined: §3.4.
- **One prefix per channel** (`EF` for faults, `CI` for case input, `IC` for
  §9.5). Declined: a kind that moves channel would change code, and the
  reader of a code needs the channel no more than the reader of a WF rule
  needs its chapter.
- **Numbered arm keys** (`WF-001.2`). Declined: §2.2.
- **Executor codes in the case file**, per executor
  (`expectedFault.codes.clingo`). Declined: a refusal is executor conduct,
  not a property of the case, and the gate already keeps executor conduct
  in reason-carrying lists (the Drools leg); §5 and §8, question 2.
- **`java.text.MessageFormat`** for catalogues. Declined: its placeholders
  are positional and it treats the apostrophe as an escape, and today's
  messages are full of single quotes (`'gewicht'`, `'2026-02-30'`); a
  renderer of a few lines with named `{name}` placeholders does what is
  needed.

## 4. The fault kinds

### 4.1 The kinds the red corpus witnesses: 25 measured, 24 codes

Draft codes, kinds, parameters and spec homes are proposals. The witnesses
and the per-executor column are measured. "ref = clingo" means one sentence
in both reports.

*Revised after Fable's review (§2 (g)): EF-019 and EF-020 are one clause
(WF-044 reached, ch. 5.7) at two positions, so they are one code with a
`position` parameter; EF-021..025 move up one place and the slice lands 24
codes. The renumbering, the slice's numbers:*

| Draft | Slice |
|---|---|
| EF-001..EF-018 | unchanged |
| EF-019, EF-020 | EF-019 (`position`: `expression` or `condition`) |
| EF-021 | EF-020 |
| EF-022 | EF-021 |
| EF-023 | EF-022 |
| EF-024 | EF-023 |
| EF-025 | EF-024 |

*The draft table below keeps its draft numbers, as reviewed.*

| Draft | Kind | Channel | Parameters | Spec home | Witnesses (`corpus/rood/`) | Executors today |
|---|---|---|---|---|---|---|
| EF-001 | a role key of the case input resolves to two roles | case input | `roleKey`, `objectType` | IR-SPEC ch. 2.6 (no clause in RUNTIME-SEMANTICS found) | `ambiguous-case-role-key` | ref = clingo (shared case reader); Drools EXCLUDE (a bean-field limit) |
| EF-002 | a role link of the case input names an undeclared label | case input | `role`, `label`, `target` | none found; the case reader | `unknown-role-target-label` | ref = clingo; Drools EXCLUDE (its loader names the same label) |
| EF-003 | a date lexeme of the case names no calendar day | case input | `field`, `lexeme` | none found; the case reader | `asofdate-kalender`, `inputdatum-kalender` | ref = clingo; Drools FAULT while it reads the case, in its own sentence |
| EF-004 | an input value does not fit its declared decimals | case input | `place`, `value`, `decimals` | ch. 11.3, "the input-precision fence" | `input-overprecision`, `input-overprecision-param`, `list-element-overprecision`, `struct-field-overprecision` | ref and clingo same kind, other words; Drools NOFAULT 2, EXCLUDE 2 |
| EF-005 | division by numeric zero | fault | `rule`, `dividend` (Drools names them; ref names none) | ch. 1.4 point 4 | `aantalgevulde-ongewaakte-deling`, `atmost-reads-until-settled`, `begrensd-reads-bounds-in-order`, `closed-runtime-division-by-zero`, `division-abs-by-numeric-zero`, `division-by-numeric-zero`, `som-fallback-always-evaluated`, `vouwfilter-deelt-door-nul` | ref 8; clingo 6, and 2 refusals (§1); Drools FAULT 6, EXCLUDE 2 |
| EF-006 | a written value does not fit the declared decimals of its target | fault | `place`, `value`, `decimals` | ch. 2.4 | `closed-nonexact-division`, `closed-runtime-nonexact-division`, `decimal-nonterminating` | ref at the write; clingo "non-exact division" at the quotient (decision for the slice: same kind, or a clingo position); Drools EXCLUDE 2, NOFAULT 1 |
| EF-007 | a datum-tijd step leaves the representable millisecond range | fault | — | ch. 3.8 | `dagsoortstap-buiten-bereik`, `datumtijdstap-buiten-bereik` cases 1–3 | ref = Drools; clingo its integer range (`limit`) |
| EF-008 | a duration count is out of range | fault | `count` | ch. 3.8 | `datumtijdstap-buiten-bereik` case 4 | ref = Drools; clingo its integer range (`limit`) |
| EF-009 | a day-kind walk passes the reach of 40 000 days | fault | `from`, `reach` | ch. 3.8, "The reach fence (normative)" | `dagsoort-onvervulbare-wandeling`, `dagsoort-reikwijdte-buiten-tabel` | all three |
| EF-010 | the bound day is read outside a day-kind evaluation | fault | — | ch. 3.7, as the messages cite it | `contextwaarde-de-dag-buiten-dagsoort` | all three; Drools adds the rule name |
| EF-011 | a context value needs an evaluation date the case does not carry | fault | `contextValue` | ch. 8.2 | `rekenjaar-zonder-rekendatum`, `tijdsduur-reads-van-before-tot` | all three |
| EF-012 | a validity-bounded form in a time-free session | fault | — | ch. 8.1 | `jutterswet-uitsluiting-zonder-rekendatum` | all three |
| EF-013 | the Parameterset timeline does not cover the evaluation date | fault (session start) | `parameter`, `date` | ch. 8.4 | `vintage-uncovered-date` | ref = clingo; Drools EXCLUDE |
| EF-014 | a parameter the case does not bind is read | fault | `parameter` | ch. 5.7 | `aanvoergeld-parameter-ongebonden` (2 cases), `dagsoortparameter-ongebonden` | ref = clingo; Drools NOFAULT 2, EXCLUDE 1 |
| EF-015 | the creation budget is exhausted | fault | `creation`, `stratum`, `depth` | ch. 5.5 | `finite-chain-over-budget`, `jutterswet-budgetgrens-over`, `runaway-recursion` | all three (clingo as `viol/2`) |
| EF-016 | two writers of one target disagree | fault | `rules`, `values`, `attribute`, `label`, `objectType` | ch. 5.7 | `jutterswet-dubbele-melding` (2 cases) | all three; ref inside its pass-budget sentence |
| EF-017 | a to-one role gets a second filler | fault | `role`, `label`, `filler` | ch. 5.5 | `to-one-second-filler` | ref and clingo same kind, other words; Drools EXCLUDE |
| EF-018 | the back-link of a created object is ambiguous | fault | `objectType`, `createdType` | ch. 5.5 | `ambiguous-backlink` | ref = clingo (clingo at translation); Drools EXCLUDE |
| EF-019 | an Unresolved expression is reached | fault | — | ch. 5.7, WF-044 | `wf044-onopgelost-reached` | all three (clingo "in translation") |
| EF-020 | an Unresolved condition is reached | fault | — | ch. 5.7, WF-044 | `wf044-onopgelost-guard` | all three |
| EF-021 | a temporal key is supplied as a scalar | fault | `key`, `label` | ch. 5.9 | `uni-temporal-key-scalar`, `uu-temporal-key-scalar` | ref = clingo; Drools EXCLUDE |
| EF-022 | a filled count over a temporal attribute of a set | declined | `attribute`, `fold` | ch. 3.4c | `aantalgevulde-temporele-setbron` | ref = clingo; Drools EXCLUDE on the same ground |
| EF-023 | a nested day-kind test on a date other than the bound day | declined | — | ch. 3.12, ch. 3.8 | `dagsoortverwijzing-andere-dag` | ref = clingo; Drools EXCLUDE |
| EF-024 | a period condition on a temporal characteristic target | declined | `characteristic` | ch. 3.10, 3.11 | `periodevenster-tijdlijnkenmerk` | ref and clingo ("at evaluation" / "in translation"); Drools EXCLUDE |
| EF-025 | a uniqueness key with more than one temporal component | declined | `key` | ch. 5.9 | `uni-two-temporal-keys`, `uu-two-temporal-keys` | ref = clingo; Drools EXCLUDE |

Totals: 8 witnesses on the case-input channel, 37 on the fault channel, 5
declined; 50.

Three findings the slice must settle, settled after Fable's review:

1. EF-001, EF-002 and EF-003 have no clause in RUNTIME-SEMANTICS that names
   them as a fault (EF-001 is described in IR-SPEC ch. 2.6; the other two
   live only in the case reader `ref/lib/case.ml`). Each needs a spec
   sentence before it gets a code. *Settled:* the slice writes three
   one-sentence homes under ch. 5.7 step 3, beside ch. 11.3's input fence.
2. EF-006: whether every document on which clingo's "non-exact division"
   guard fires is one where `ref`'s write gate fires. If yes, one code; if
   no, clingo's guard is a `CLINGO` code of kind `limit` and the three cases
   enter the divergence list. *Settled (Fable §2 (f)): one code, no
   divergence entries.* ASP-TRANSLATION records the guard as a "loud
   asymmetry" that fires where `ref` writes exactly, an over-approximation
   of the ch. 2.4 write gate; on the three red cases both detect one
   observable, and an over-approximating detector is a coverage limit of
   clingo, already recorded, not a different kind.
3. EF-022 to EF-025 are declined forms: normative refusals. This document
   puts them in the `EF` table because the specification, not an executor,
   declines them, and all executors must refuse them in one way (§8,
   question 5). *Their destiny:* each is a static property of the document,
   so each ends as a WF rule (as WF-112 did for the period condition under
   a counting quantifier; the witness moves to `negatief`) or as a
   specified form (the witness moves to golden), and the code retires, as
   WF rules do.

**EF-005 is already decided to move** (Fable §2 (a)): RUNTIME-SEMANTICS
ch. 1.4 point 4, "Decided: v1 routes numeric-zero division into the same
inconsistency channel". The kind with the most witnesses (8) moves from the
fault channel to §9.5. The slice's table records that decision in EF-005's
channel cell, and gate 8's witness check admits a red **or golden** witness
from day one, so that the first execution of a spec decision does not turn
gate 8 red by construction.

### 4.2 The §9.5 channel — codes, but not in the slice

The card puts the §9.5 channel in the normative family. Its producers are
named in RUNTIME-SEMANTICS ch. 5.6, ch. 4.4 and ch. 7.5: a violated
consistency assertion; a duplicate uniqueness key; a declared-unordered
defaults group whose decisions differ; four allocation degrades (weight
empty, proportional; weight empty, ordered tie-break; negative weight,
proportional; cap empty or negative); a division by an empty divisor with a
filled dividend; a negative radicand; an empty variable-start anchor. Ten
kinds.

They cannot meet the card's acceptance (b), "elke code heeft een getuige in
`corpus/rood`": a §9.5 deposit does not fail, so no red case witnesses it.
Their witness is a golden case whose `inconsistent` expectation names the
code. That needs:

- the channel fact to carry its codes: the identity of a fact stays rule ×
  subject (the idempotency of ch. 5.6 is unchanged), and the codes
  deposited for that pair form a set beside it; the channel read
  "regelversie X is inconsistent" does not look at the set;
- an optional `code` on the `inconsistent` expectation of the case schema;
- `ref` to keep the reason it now discards, clingo to carry it in
  `inconsistent/2`'s neighbourhood, Drools in its `Deposit`;
- golden witnesses, outside the slice's "May touch".

**Recommendation:** a follow-up card of its own (§7, card 7), normative, on
Opus. The slice reserves no numbers for it; the ten kinds get the next free
`EF` numbers when that card lands.

*Decided after Fable's review (Q3):* card 7, serial with card 5. It needs
no `CONSTRUCTS.md` row: CONSTRUCTS tracks construct verdicts and IR deltas,
and a code set beside the channel fact changes no construct, no wire, and
not the fact's identity (rule × subject); it is a ch. 5.6 clause plus a
case-schema extension, tracked where those live. Card 7 is also where
EF-005's decided move (§4.1) is executed, so its gate must accept a golden
witness for a code that was red — which gate 8's witness check does from
the slice on.

## 5. Gate 8 with codes (the slice)

**The mechanism of the slice (revised after Fable's review, Q2, §2 (h),
§2 (i) and part 3):**

- `expectedFault.code`: one `EF` code per case, pattern `^EF-[0-9]{3}$`,
  optional in the schema (the change is additive; `expectedFault` has
  `additionalProperties: false`, so it is a schema change) and demanded by
  gate 8 on every red case. Gate 4's pattern self-test gets a row set for
  the new pattern position.
- On the red path the ref and clingo runners print the code or codes they
  observed on a `CODE` line (`CODE EF-005`; `CODE -` for a fault without a
  code) and the case's own code, decoded by the runner's case reader, on a
  `CODE-PINNED` line; the Drools red run renders the code at the head of
  its status line (`FAULT EF-005: …`). The fragment check stays in the
  runners as today.
- Gate 8 demands of `ref`, of clingo, and of Drools where the case pins a
  `drools` fragment, that the observed codes contain the case's code — with
  one exception mechanism: a **divergence list** in the script,
  `<case>|<executor>|<CATEGORY> <reason>`, for a case on which an executor
  fails for a reason of its own. The first word of the reason is its
  category, `REFUSAL` or `LIMIT` (the Drools leg's shape; `FALLBACK` and
  `CONTRACT` are not admitted: a bottom sentence is fenced already, and a
  red document is verifier-clean). Measured: 7 clingo entries (§1). The
  entries carry no code: the fragment pins keep covering the actual reason
  until card 5, and the code column joins when card 5 lands the `CLINGO`
  registry; so the slice needs no `CLINGO` code, no registry and no
  carrier check. The list is stale-guarded in both directions: an entry
  whose executor now reports a code — the case's or another — or whose
  case is gone fails the gate, and so does an unlisted check whose
  executor reports no code or another one.
- **The table is read by the gate, strictly.** A sidecar registry would
  duplicate the normative table, so gate 8 parses the RUNTIME-SEMANTICS
  table itself — the first gate to parse a spec table (gate 1 greps the
  spec files for paths only). Every line that starts with `| EF-` must
  parse as a row of the fixed grammar (code, kind, channel, parameters,
  spec home, witnesses) and stand in the table's section, or the gate
  fails naming the line; codes are unique and ascending. Every witness the
  witness column names exists and pins that code: a red directory through
  `expectedFault.code`, a golden one through an `inconsistent` expectation's
  `code` (none yet; card 7 adds the field, and the check admits the zone
  from day one, §4.1). Every code a red case pins is a row of the table. So
  a stale row is a gate failure, never a spec lie.
- Floors: `EXPECTED_MIN_RED_CODES`, the number of ref and clingo code
  checks that matched, measured at the slice; `EXPECTED_MIN_EF_CODES`, the
  number of table rows (24). The Drools leg's code checks need no floor of
  their own, for the reason its fault count has none: each is a case with
  a `drools` fragment, and those are held by the fragment pins and the
  name-carrying exclusion list.
- The mutation of acceptance (d): one executor made to report another `EF`
  code for one kind turns the gate red, naming each case, the executor and
  both codes; run once and recorded in the card.
- The fragments stay, and so do the bottom-sentence fence and
  `ref --bottom-sentence`; every existing pin stays green (acceptance (e)).
- Out of the slice: the emission of `params`, executor codes and
  registries, any change to the fence, §9.5 codes, catalogues, the wire,
  and any text of a message.

Why a list in the script and not per-executor fields in the case: the
case states what the semantics demands — one fault kind — and the list
states what an executor does instead. The Drools leg has kept that split
since 2026-08-27; the divergence list extends it to clingo.

## 6. Catalogues and language

- **Engines.** The verifier, `ref` and clingo keep their English text at
  the raising site, beside the code; no catalogue. The Drools backend gets
  English as its text and a Dutch catalogue (decision 0.1). Today many of
  its sentences are Dutch ("deling door nul in de regel …", "twee schrijvers
  van één doel …"); in the catalogue card those become its Dutch catalogue
  verbatim, and the English text is written new. Its sentences that are
  already "in the reference interpreter's words" (`ComputeFault`) keep
  them as the English text and get a Dutch translation.
- **Frontend.** Dutch default text, English catalogue beside it
  (decision 0.1); today's text becomes the Dutch catalogue verbatim.
- **Form.** One catalogue per component and language, keyed by code, with
  named `{name}` placeholders, and one renderer of a few lines. The Drools
  backend already compiles the frontend's source tree except `resolver/`
  and `emit/` (`backends/drools/pom.xml`, the execution
  `add-frontend-ir-records`), so a renderer in an engine-neutral package of
  the frontend serves both components without a new dependency; each
  component keeps its own catalogue files.
- **Choice of language.** The distribution passes a locale (its CLI flag,
  its `Accept-Language`); the components render through public API, since
  the distribution reads only public API (`CLAUDE.md`, Repo relationships).
- **A catalogue gate.** Every code raised in a component has an entry in
  each of its catalogues; every entry belongs to a raised code; each entry
  uses exactly the parameters its code declares. A new gate script, or a
  leg of gates 10 and 12; the catalogue card decides.
- **Order constraint.** The Drools backend's default text turns English in
  the catalogue card; the Dutch `drools` fragments of 26 red cases would no
  longer match. The fragment cleanup (card 5) therefore precedes the
  catalogue card (card 6).
- **Open for the maintainer: the Drools backend's default language**
  (Fable §2 (e); not decided here). The distribution ships the backend; its
  users see Dutch runtime sentences today and would see English after
  card 6 unless the distribution passes a locale. Before card 6 it must be
  decided whether the default locale is a setting the distribution sets —
  then its pin bump must set `nl` — and that decision belongs in card 6;
  otherwise the switch lands as a silent behaviour change at the bump.

## 7. Follow-up cards: order, size, model

Model rule of the programme: Haiku for bulk (assigning codes to hundreds of
messages, translating catalogues), Sonnet for building, Opus only where the
work is normative design.

| # | Card | Depends on | Size (estimate) | Model |
|---|---|---|---|---|
| 1 | **Normative slice** (this card, point 2): the `EF` table in RUNTIME-SEMANTICS (24 rows, the three spec homes of §4.1 finding 1, the ch. 1.4 note on EF-005), `expectedFault.code`, the code beside the text in `ref`, clingo and Drools and at the head of the report, the divergence list (category and reason, no code), gate 8 reading the table | Fable's review | about 40 raising sites (ref ~25, clingo ~10 plus a reason-to-code table for its `viol/2` guards, Drools: a classification at the catch and `code()` on its own exceptions), the pattern sites of the OCaml carriers (ref, clingo, the two test suites), 50 case files one line each, RUNTIME-SEMANTICS ~80 lines, gate 8 ~150 lines, gate 4's pattern self-test; one session | Opus (the card says so; the table is normative) |
| 2 | **Arm keys of the verifier**: the key and its parameters on each violation, `expect.txt` from fragment to a key line plus optional `name=value` lines (§2.2), gate 3; one key ↔ one message template within a rule, no key for two templates, both checked by gate 3 | 1 (the form) | 441 violation sites over 104 rules; 354 `expect.txt` rewritten by a script (run the verifier, take the key and parameters of the line whose message holds the old fragment); gate 3 ~60 lines. May go last: its gain is survival of rewording | Haiku names the arms rule by rule; Sonnet builds |
| 3 | **Codes of gate 12**: `DROOLS` codes for the backend's refusals, the 45 `EXCLUDE` entries of gate 12 and the 23 of gate 8's Drools leg pin a code — which gate 12 then compares, where today it compares the channel only | 1 | ~232 refusal sites; the manifest (`Coverage`); 68 list entries | Haiku assigns codes; Sonnet builds |
| 4 | **Codes of the frontend**: `Diagnostic` gets `code` and `params`, OFF the wire (§2.4), `RGS` codes on 248 sites, text unchanged; the four Drools sites that construct a `Diagnostic` follow | 1, 3 (both edit `backends/drools/`) | 248 sites plus 4 in the Drools backend; no test change needed, the text being unchanged | Haiku assigns codes and parameters; Sonnet builds |
| 5 | **Cleanup of the fragments**: `params` emitted by the three executors on the `CODE` line and pinned as `expectedFault.params` (mandatory before any fragment goes, §3.3); `ref` and clingo coded on demand (§2.3: the 36 bottom sentences as `fallback`, the sites a pin or a list names, no `contract` codes) with a two-way carrier check; the `CLINGO` code column of the divergence list; the fence on codes; the fragments out of the 50 cases and the schema, `ref --bottom-sentence` out | 1, 3 (both edit `scripts/check-rood.sh`) | ~100 OCaml sites; 50 case files; gate 8 ~150 lines out, ~80 in; the gate-8 paragraph of `CLAUDE.md` (main session) | Haiku codes the sites; Sonnet builds |
| 6 | **Catalogues NL/EN** of the frontend and the Drools backend, the renderer, the locale API, the catalogue gate; first the maintainer's decision on the Drools default language (§6) | 3, 4, 5 | ~248 frontend and ~260 Drools messages in two languages; the Drools tests' ~258 text assertions and the frontend's 211 either run under the Dutch catalogue or move to codes — a change to test files, which needs the maintainer's consent in the card | Haiku translates; Sonnet builds |
| 7 | **§9.5 codes** (added by this design, §4.2): the code set on a channel fact, `inconsistent.code`, golden witnesses; executes EF-005's decided move to §9.5 (§4.1) | 1; serial with 5 (both change the case schema and gate 8's neighbourhood) | ten kinds plus EF-005; three executors' channel; schema; ~10 golden cases | Opus (normative) |

**Order (revised after Fable's review, §2 (b)):** 1 → 2 ∥ (3 → 4) → 5 → 6;
card 7 after 1, serial with 5. Card 2 runs beside the pair 3 → 4 (disjoint
files: `spec/verifier/` with gate 3 and `corpus/negatief/`), and may as
well go last (§2.2). Cards 3 and 4 run one after the other because card 4
edits the four Drools sites that construct a `Diagnostic`, inside the
`backends/drools/` that card 3 recodes. The order differs from the card's
list in one place: the cleanup comes before the catalogues, for the reason
in §6. Each component keeps its own registry, so parallel cards share none
(`WAY-OF-WORKING.md` §1); the `EF` table is touched by cards 1 and 7 only.

Protected paths: cards 1, 2, 3 and 5 touch `scripts/`, card 1 and 7
`spec/RUNTIME-SEMANTICS.md` and `spec/schema/`, card 2 `spec/verifier/`; each
stays a proposal until the maintainer merges it.

## 8. Questions for Fable, and the decisions

Fable answered the six questions on 2026-10-18; the main session adopted
the answers the same day. Below each question, the decision and its reason
in one sentence.

1. **One `EF` namespace with the channel as a column** (case input, fault,
   declined, §9.5), or a prefix per channel (§3.1, §3.5)?
   **Decision:** one `EF` namespace with the channel as a column, because
   EF-005 is already decided to move from the fault channel to §9.5
   (RUNTIME-SEMANTICS ch. 1.4 point 4), and one namespace keeps its code
   where a prefix per channel would recode it.
2. **Executor divergence**: one normative code per red case plus a
   reason-carrying divergence list in gate 8 (§5), or per-executor code
   fields in the case file?
   **Decision:** the divergence list in the script, with a category and a
   reason and without codes, because what an executor does instead of the
   demanded kind is executor conduct, and the corpus should not move when
   an executor moves.
3. **The §9.5 channel**: codes as a set beside a fact whose identity stays
   rule × subject, witnessed by golden `inconsistent.code`, in a card of its
   own after the slice (§4.2) — or inside the slice, against the card's
   acceptance (b)? And is that set an observable change that needs a
   `CONSTRUCTS.md` row?
   **Decision:** a card of its own (card 7), serial with card 5 and without a
   `CONSTRUCTS.md` row, because its witness is a golden `inconsistent.code`
   that needs a code on the channel fact in three executors plus the schema,
   while a code set beside the fact changes no construct, no wire and not
   the fact's identity.
4. **Transport and parameters**: the code as a field beside the text,
   rendered at the head of the report (§3.4). And before the fragments go,
   should a red case pin parameters (`expectedFault.params`, a subset
   match)? Today 19 of the 50 cases pin a name or lexeme in their fragment,
   and two pairs of cases in one directory differ only in a parameter
   (`aanvoergeld-parameter-ongebonden`: the parameter;
   `jutterswet-dubbele-melding`: the label); a code alone no longer tells
   them apart. If yes, the runners need a machine-readable form of the
   parameters (a JSON line, or a `--json` report).
   **Decision:** the code travels in the exception payload beside the text
   and is rendered at the head of the report, and `params` stay out of the
   slice but become mandatory in card 5, because a head prefix breaks no
   measured pin while, once the fragments go, a code without parameters can
   no longer tell the two pairs apart.
5. **Declined forms as `EF` codes** (EF-022 to EF-025): normative refusals
   in the normative table, or executor refusals?
   **Decision:** `EF` codes, because the specification mandates the refusal
   of every executor (ch. 3.12) and only a shared code can check that all
   three refuse the same thing.
6. **Arm keys as slugs** (`WF-001.case-collision`) rather than numbers
   (§2.2)?
   **Decision:** slugs, with two rules gate 3 enforces (one key ↔ one
   message template within a rule, no key for two templates) and parameter
   lines beside the key (§2.2), because a slug needs no registry to stay
   unique while a bare key would lose the object identity the fragment
   pinned.

## 9. Sources read

`CLAUDE.md` (Spec discipline; gates 3, 8, 12; Conventions); the card
`backlog/doing/20261017-meldingscodes.md`; `spec/RUNTIME-SEMANTICS.md`
ch. 1.4, 2.4, 3.8, 3.12, 4.4, 5.5, 5.6, 5.7, 5.7a, 5.9, 8.2, 11.3;
`spec/IR-SPEC.md` ch. 2.6; `spec/schema/uchar-case-v0.schema.json`
(`expectedFault`, the `inconsistent` expectation); `scripts/check-rood.sh`;
`scripts/check-verifier.sh`; `scripts/check-drools.sh` (`EXCLUDE_KNOWN`,
`EXPECTED_CHANNEL`); `ref/bin/runner.ml`, `ref/lib/value.ml`,
`ref/lib/world.ml`, `ref/lib/arith.ml`, `ref/lib/eval.ml`;
`backends/clingo/bin/clingo_runner.ml`, `backends/clingo/lib/translate.ml`;
`backends/drools/src/main/java/dev/uchar/regelspraak/cases/DroolsRedMain.java`,
`CaseRunner.java`, `DroolsBackend.java`, `loader/IrLoader.java`,
`transpiler/tms/ComputeFault.java`, `EvaluationFaults.java`,
`plan/Consequence.java`; `spec/verifier/bin/verifier.ml`,
`spec/verifier/lib/checks.ml`; `frontends/regelspraak/.../RegelSpraakModel.java`,
`resolver/Resolver.java`; 50 red case files and a sample of `expect.txt`.
