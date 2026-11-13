# The findings of the reviews of 2026-11-13

Goal: the faults found in the rest of the expression builder and emitter, the Catala frontend, the toolchain's
`Verbaliser` and the playground are fixed with a witness, and the measured simplifications are made without losing a
function. Reports with probes: the review folder `review-2026-11-13` (outside the repository, named in the builders'
working agreement).

## Frontend, binding (`ExpressionBuilder`, `RuleChecker`, `IrEmitter`, `DefinitionResolver`)

- A fold or quantifier over "alle X van ‹anchor›" whose anchor is another object than the subject ("alle vondsten
  van zijn meester", "van de Haven", "van een Haven") binds the subject's own relation: a silent wrong range, the
  three executors agree on the wrong sum; `grammar/DIVERGENCES.md` promises the refusal. One anchor test for the
  one-step range, at the three places that build it; an anchor that is not the subject either binds through the
  anchor or is refused.
- A base conjunct with a path or a fold range under an "Uitzondering op" of a subtype ends in RGS-394 on a valid model.
- The path of an attribute reference and the range of a fold call are bound where they are built (as the
  characteristic check and role membership are since 2026-11-12); `opSubtype` and `helperRules` carry the binding
  along instead of recomputing it; the emitter's subject machine (`currentSubject`, `foldRange`, the derivations the
  resolver asks through public statics) goes. The 216 goldens with a `source.rgs` stay byte-identical.
- "Percentage (niet-negatief …)" drops its sign without a word; a diagnostic at the declaration.

## Frontend, public reading (`Resolver`, `Designations`)

- uchar's parse is public: the tree, the tokens, the syntax errors with line, column and expected tokens, with the
  second parse for a label with 'en'; the playground stops parsing on its own.
- `Designations` records per designatable its line and the comment above its declaration, so the playground and an
  OpenAPI document read declarations through uchar instead of their own token walk.
- Two rule groups of one name: measure what IR-SPEC says (namespaces, WF-001, WF-117/118); if it is ill-formed, an
  ERROR.

## Catala frontend (`frontends/catala`)

- A value that may stay empty is read as a number on four routes (a copy, a subscope argument, a boolean read as a
  condition, a subscope output) where Catala raises "no applicable rule"; the plugin refuses the form or reads it as
  empty, whichever Catala's semantics gives.
- The context overrides of two subscope calls of one type land on every instance.
- The plugin exits 0 on a document the verifier refuses (a computed boolean passed as a subscope input).
- A subscope call that does not override a context variable is refused with the reason of an override.
- Simplification: one serialiser for the IR in the shared library the Catala and F-logic frontends use, instead of two
  `emit.ml` files with 219 identical lines (proposal on `spec/verifier/`); gates 9 and 14 keep every golden
  byte-identical.

## Toolchain (its own commits)

- `Verbaliser`: a derived value is no longer called the input value (read `EndState.Member.given`/`derived` instead
  of `readRaw`); "numeriek met exact N cijfers" keeps its negation, digits and period; an operand is padded to its
  declared decimals and never rounded; a value fold says "geen lid droeg een waarde bij" and counts the members with a
  value; the version of a rule through `positions().versionOf`; the two unused overloads go.
- `Verbaliser`: an allocation (share and rest), an object creation and a fact creation get a sentence of their own,
  as an ordinary rule does.
- Playground (after the pin): a lexer error is a syntax error, not a 500; a label with 'en' parses (both through
  uchar's public parse); an open bound shows as open; `Declarations` reads through uchar's `Designations`; the DRL
  checks per request that uchar's corpus witness already holds go where that loses no check on the request's own
  source.

Acceptance: every item fixed with its witness, done, or struck with the reason; `make gates` green in uchar and in
the toolchain; the toolchain pins the landing.

May touch: `frontends/`, `backends/`, `ref/`, corpus witnesses outside `corpus/golden/anchor`, this card.
Protected (the main session lands them): `spec/`, `scripts/`, `CLAUDE.md`.
