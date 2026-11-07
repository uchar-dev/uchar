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
