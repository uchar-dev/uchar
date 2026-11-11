# The session readers leave TmsTranspiler

Goal: `TmsTranspiler.java` holds the analysis of a document into plans and nothing else. The code that reads a fired
session back is in a class of its own, and the tail every lowering method ends in is one method. No behaviour changes.

Measured on `ccf6de21c` (a read of the file, line numbers of that commit): the class is 10,310 lines.
- Lines 990-1912 read a session: `readOf`, `decisionsOf`, `periodDecisionsOf`, `setDerivedValues`, `readFixpoint`
  and their helpers. Lines 5636-5860 do so again: `readingsOf`, `reading`, `contributors`. Together about 1,150
  lines that use the plans the analysis left and analyse nothing.
- The tail "where it can fail a `conditionFaultRule`; a `depositRule`; a `validityFault`" stands at eight places:
  `:2198-2200`, `:3305-3307`, `:3399-3401`, `:3562-3566`, `:4005-4009`, `:4238-4242`, `:4414-4418`, `:5117-5123`.
- `readingRule` has three overloads of 11, 13 and 15 parameters (`:5506-5533`); the first two pass nulls on.
- Small: `rootChains` computed twice with one expression (`:1085`, `:1186`); the same alpha built twice (`:3616`,
  `:3618`) and the same `FactPattern` twice (`:3877`, `:3882`); `refuseMixedDeposits` (`:3852`), which its own
  comment calls unneeded; the `-1` arms after `depositSite` at `:6080`, `:6137`, `:6322`, `:6827`, which cannot be
  reached; `declaredVariables()` (`:5582`), whose callers are to be checked.

Acceptance:
- A class `SessionReaders` (or two, if the reads of lines 990-1912 and 5636-5860 share nothing) in `transpiler/tms`
  holds the readers; `TmsTranspiler` keeps the public methods the backend and the distribution call, each a call
  through. Measure those callers first, in `backends/drools` and in `regelspraak-toolchain`.
- The tail is one method, called at the eight places.
- A method, arm or overload goes only where no caller is left in uchar and the distribution, tests included; each
  one is named in the Result.
- The generated DRL text and PatternDSL are equal to those of `ccf6de21c` for every golden and red case
  (`BranchDumpsCorpusWitnessTest`, `DrlPiecesCorpusWitnessTest` and the pins hold unchanged).
- The backend suite passes with the same test count; `make gates` green; the distribution builds and its gates are
  green on the new pin.

May touch: `backends/drools/src/main/java/dev/uchar/regelspraak/transpiler/tms/TmsTranspiler.java`, new classes in
that package, this card.

Out of scope: the about sixty mutable fields that serve as implicit parameters of the analysis; the near-copies
`timelineInitialisationRule` / `writeRule` and `settledOn`/`chainOn` / `reconciled`/`descended`; any test; the walker
`deposits` (card `review-candidates-triage`, A3).
