# The scenario layer reads a recovered tree

Goal: a test document with a syntax error in its scenario layer gives that syntax error (RGS-216) on both routes that
read one, `--cases` and the semantic tokens, and returns. Reported by the workbench on 2026-11-07: a `Situatie` header
without its `:`, followed by a `Scenario`, ends in a NullPointerException in `ScenarioResolver.validateBlock`; the
playground answers 500 and `bin/regelspraak scenario` exits on an uncaught exception.

Measured on `a5b857a47` with a harness that leaves out one token or one line of every `scenario.rgs` of the corpus
(17,469 variants): four places throw, each only on a tree ANTLR recovered after a reported syntax error. Recovery
leaves a context out where a match failed before it, or makes a token up (an error node with token index -1):

| Place | Input | Exception |
|---|---|---|
| `ScenarioResolver.validateBlock` | `Situatie X` without `:` | NullPointerException |
| `ScenarioResolver.validateInstance` | an opener without its label (bullet left out) | NullPointerException |
| `SemanticTokens.labelDeclaration` | a label the recovery made up (`een Vondst` without a label) | ArrayIndexOutOfBoundsException |
| `ScenarioResolver.readAndType` | `niet toepassing` (a made-up `van`) | IllegalStateException |

Acceptance:
- `ScenarioRecoveryTest` (new) passes; on `a5b857a47` four of its five tests fail with the four exceptions above.
- The workbench's reproduction gives the syntax error, exit 1, and writes nothing.
- The harness, also with every token doubled (31,835 variants), throws nowhere.
- The frontend suite and `make gates` are green.

May touch: `frontends/regelspraak/src/main/java/dev/uchar/regelspraak/resolver/ScenarioResolver.java`,
`SemanticTokens.java`, `DefinitionResolver.java` (`recovered` package-private),
`frontends/regelspraak/src/test/java/dev/uchar/regelspraak/ScenarioRecoveryTest.java`, this card.

Out of scope: extending the sweep of `SemanticTokensRecoveryTest` (which mutates `source.rgs` only) to `scenario.rgs`;
the wording of ANTLR's syntax error; the pin of the toolchain.

Result: landed 2026-11-07. Each of the four places skips the recovered node and leaves the syntax error as the ERROR
the route reports: `registerSituation` reads a situation without a block as an empty block, `validateBlock` skips an
opener without a label, `SemanticTokens.label` treats a made-up label (token index -1) as absent, and `readAndType`
returns null on a marker the recovery read (`DefinitionResolver.recovered`, now package-private). `--cases` writes
nothing then, as for every ERROR. Measured: the workbench's reproduction gives `syntaxfout: mismatched input 'is'
expecting {GEBASEERD_OP_DE_SITUATIE, ':'}` on line 10, exit 1, no files; the harness throws in none of 31,835 variants
(token left out, token doubled, line left out); `ScenarioRecoveryTest` fails in four of its five tests on `a5b857a47`
and passes here; frontend suite 1543/1543; `make gates` green on `220c053a8` (17 gates, 470 s,
`out/gates/20261107-105757/`). Left: the sweep of `SemanticTokensRecoveryTest` still mutates `source.rgs` only, which
is why it never reached the scenario layer; the toolchain needs a pin bump for the playground and
`bin/regelspraak scenario` to carry the fix.

## Extension (2026-11-07, owner's request): the sweep reads the scenario layer

Goal: `SemanticTokensRecoveryTest` sweeps the `scenario.rgs` of every golden test document as it sweeps `source.rgs`
(cut at forty places, each of twenty lines left out in turn), read beside its `source.rgs` on the tokens route, which
runs the scenario layer; no exception, the output passes `SemanticTokensCheck`, floors on the runs and on the runs
with a syntax error (RGS-216).

Acceptance: the new sweep fails on `a5b857a47` (measured: 115 problems, at `validateInstance`, `readAndType` and
`labelDeclaration`; a situation header without its `:` is reached by no cut, and stays pinned by
`ScenarioRecoveryTest`), passes on main; frontend suite and `make gates` green.

May touch: `frontends/regelspraak/src/test/java/dev/uchar/regelspraak/SemanticTokensRecoveryTest.java`, this card.
