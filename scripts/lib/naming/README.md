# scripts/lib/naming — Dutch names to English, per whole name

The tool of card `hernoemgereedschap` (programme "Engelse namen"). It measures per zone which names are still
Dutch and renames them per whole name, by a table the owner reviews. Python 3 without packages:

    python3 -I scripts/lib/naming/rename.py <subcommand> ...
    python3 -I scripts/lib/naming/test_rename.py

| file | what |
|---|---|
| `glossary.tsv` | the English word for each Dutch word (card `engelse-namen-afspraak`) |
| `technical-terms.tsv` | English words the dictionary (`/usr/share/dict/words`) lacks, with a reason |
| `proper-names.tsv` | proper names (`RegelSpraak`, `Jutterswet`), with a reason; written as their words |
| `tokens.py` | tokenizers for Java, OCaml, shell and ANTLR: identifiers, strings, comments, with offsets |
| `rename.py` | the subcommands below |
| `prompts/translate.md`, `prompts/decide.md` | the fixed instructions for the Haiku batches |
| `tables/<zone>.tsv`, `decisions/<zone>.tsv` | a rename card's table and decision list |
| `test_rename.py` | one fixture per danger (acceptance (a) of the card) |

## What a word is

A name is split into words (camelCase, snake_case, kebab-case, digit runs). A word is Dutch when the glossary has
it, `van` and `die` only inside a name of two or more words (`of` and `op` never: they are ordinary English code,
`valueOf`, `opCode`). A word neither in the glossary nor English (the dictionary with the regular English endings,
or a technical term) is searched for glossary words of four letters or more, so `Invoervormfout` is found and
`control` is not. Left alone: domain words (the words of the declared names in `corpus/**/ir.json` and of the
`jutterswet-*` directories that the glossary does not have), proper names, and a name equal to a token name of
`RegelSpraakLexer.g4`. A glossary word one of whose translations is the word itself (`over`) never makes a name
Dutch on its own.

## Zones, occurrences and the outside

A zone is one or more path prefixes. The paths of the fixed exceptions of `CLAUDE.md` (the production runtime,
`toka/`, `spec/proposals/`) are never part of one, nor `backlog/` and this directory. An identifier in a zone file
is *outside* when it names a declaration outside the zone: read through a type or module outside the zone
(`IrLoader.Documentfout`, `A.kenmerk_soort` with `module A = Ir_v0.Ast`), a member read through a value
(`r.waarde()`, `d.feittypen`) unless a type of the zone declares that member (a Java field or method that is
not static, a member of a private type only in its own file, an OCaml record field), a Java type declared only
outside, a member of a supertype or a static import from outside, an OCaml constructor or field declared only
outside (record labels included), a name of an opened module outside. The inventory does not count those names,
`apply` leaves the qualified ones and the record labels alone, and `check` refuses a row whose name is declared
only outside.

## Subcommands

- `inventory <prefix>... [--json] [--external] [--unknown N] [--by-zone DEPTH] [--quiet]` — every Dutch name of
  the zone with file and line, its count and the glossary's English words; exit 1 when it reports a name, 0 when
  the zone is clean. Identifiers outside strings and comments, file names (an OCaml file by its module name) and
  directory names.
- `propose <prefix>... <dir>` — writes `names-NNN.jsonl` and `sentences-NNN.jsonl` (test methods with `_` in
  their name), at most 200 names each, and `zone.json`. One JSON object per line: `id`, `name`, `kind` (the
  declaration: `type`, `method`, `test`, `variable`, `parameter`, `constant`, `value`, `field`, `constructor`,
  `module`, `rule`, `label`, `file`, `dir`, or `reference`), `file`, `line`, `declaration` (the line),
  `doc` (the first sentence of the comment above), `words` (the proposed split), `dutch` (each Dutch word with
  the glossary's English words), `occurrences`, `files`. The answer of batch `X.jsonl` is `X.answer.jsonl`
  (`prompts/translate.md`).
- `merge <dir> <table> [--force]` — the answers as a table; fails on a missing, double or unknown answer. A
  flagged name becomes a row with an empty `new`, kind `improved` and reason `flagged: ...`, which `check`
  refuses until the session gives it a name.
- `merge-decisions <list> <answer>...` — the decider's JSON lines (`row`, `old`, `decision`, `why`) into the
  column `decision`; fails on a missing or double row.
- `check <table> [--classpath <jars>]` — every row is checked and every error names its table line: the format;
  `old` occurs as a whole token (or file or directory name) in its scope; the style of `new` follows `old`
  (capital first, capitals, separator, no capitals inside when `old` separates its words with `_`; a one-word
  `old` lets `new` choose, except in OCaml and shell, whose names separate words with `_`: `fact_type`, not
  `factType`); `new` is no keyword; two rows of one name do not overlap at equal reach; injective (two names onto
  one in scopes that see each other); fresh (`new` is no name that stays where `old` is renamed: in that file, in
  the files of the zone supertypes of its Java types, in the zone modules an OCaml file opens; a name read through
  a dot such as `IrLoader.load` not counted; no member of a library or repository supertype of a zone type —
  `java.lang.Object`, `Record` and `Enum` included — read with `javap`; `--classpath` for jars outside the JDK);
  a renamed file lands on no existing file; a type or module of the zone that a row renames is not used as code
  outside the zone; no OCaml label of a type outside the zone is punned on the name (`{ geheel; ... }`,
  `{ A.objecttypen; ... }`); a renamed read through a value lands on no type outside the zone that has both
  members, `old` and `new` (there it would compile silently); `new` holds no Dutch word; for `translation` rows `words` joined is `old` (case and
  `_ - '` ignored) and every content word of `words` has one of its glossary translations in `new`, in any order
  and in any English inflection (`weigert` is `refuses`, and `is_refused` passes), while what `new` holds beyond
  them is English function words: a translation that holds one counts whole (`peildatum` → `asOfDate`), `is` and
  `in`, alike in both languages, ask for nothing, and negations stay content words (`nietLeeg` → `empty` is
  refused). It prints the file renames.
- `apply <table> [--decisions <list>] [--files-only]` — refuses a dirty work tree. `--files-only` renames files
  and directories with `git mv` (a Java file by its class, an OCaml file by its module) and nothing else, for a
  commit of its own. Without it, one pass over the tokens of the zone's code: every identifier is replaced by
  the row of its name with the most specific scope, at most once, so the order of the rows does not matter. In
  a comment it replaces the names of two or more words and the qualified ones (`A.b`, `A#b`, `A::b`,
  `{@link ...}`, `{!...}`). Every other occurrence goes to `decisions/<table name>.tsv`: one-word names in
  comments, names in strings and in the zone's text files, and outside the zone the names of two or more words,
  the zone's types and modules, and renamed file names with their extension (`aggregatie.flogic`). With a list
  to fill, `apply` writes it, changes nothing and exits 3; with `--decisions` every listed occurrence must carry
  `keep` or `change` (or `change:<name>` when the name has several new names), and a missing or stale entry
  fails.
- `verify <table>... --base <commit> [--decisions <list>...] [--renderings <dir>] [--gate-logs <before> <after>]` —
  (1) applies the tables and their lists to a detached worktree of `<commit>` and compares it with the work tree,
  outside `scripts/lib/naming/`, `backlog/` and `.git-blame-ignore-revs`: handwork fails. A branch that renames
  two zones gives both tables: the file renames of each, then the contents, in the order given; with several
  tables a list goes with the table it is named after (`decisions/<table name>.tsv`). (2) scans the renderings for old
  names of two or more words; (3) compares the gate logs after removing the `drools-jar:` lines and spelling
  durations, clock times and temporary paths alike, with the table applied to the before logs (names of two or
  more words, qualified names, renamed file names). A leg not asked for is reported as NOT RUN, and the verdict
  then says it is no landing proof.
- `render <dir>` — the renderings of every golden with case files: the DRL text (the test class that reads the
  property `uchar.drlvorm`, under `mvn -o test`), the session dump of the exec-model branch (the seam
  `uchar.vuurspoor` of the Drools jar, per golden) and the ASP program of every case (a `clingo` in front of the
  real one copies each program). Runs for minutes; start it in the background. The exec-model branch has no text
  form of its PatternDSL model in the repository; the session dump, which names every fact class and member the
  rendering inserts, stands in for it.

## The table

`scripts/lib/naming/tables/<zone>.tsv`, UTF-8, a line `# zone: <prefix>...`, then the header
`scope old new kind words reason`. `scope` is `*` (the zone), a path in the zone (a file or a directory), or a Java
type or OCaml module of the zone (`Outer.Inner` for a nested type; a path when a simple name is not unique). `old`
and `new` are complete names; `kind` is `translation` or `improved`; `reason` is required for `improved`.

The decision list: `file line col old new decision context`, `line` and `col` 1-based, `col` in characters.

## A rename card, step by step

1. `inventory <zone>`; `propose <zone> <dir>`; the session runs one Haiku agent per batch with
   `prompts/translate.md`; `merge <dir> scripts/lib/naming/tables/<zone>.tsv`.
2. The session names the flagged rows (`improved`, with a reason) and runs `check` until it is clean; commit.
3. `apply --files-only`; commit (only renames, so `git log --follow` keeps the history).
4. `apply`; when it writes `decisions/<zone>.tsv`: Haiku with `prompts/decide.md`, `merge-decisions`, review,
   commit the list, `apply --decisions ...`; commit; add the hash of that content commit to
   `.git-blame-ignore-revs` (GitHub reads it; locally `git config blame.ignoreRevsFile .git-blame-ignore-revs`).
5. A compile error is fixed in the table or the list, never by hand: `git reset --hard` to the files commit,
   apply again.
6. `render <dir>` and the full gate set before and after, then `verify --base <commit before step 3>
   --decisions ... --renderings <dir> --gate-logs <before> <after>`; `inventory <zone>` reports nothing.

## Limits, measured or known

- Java and OCaml are read by tokens, not parsed. A declaration the heuristics miss can make a zone name look
  outside; a name read through a variable whose type is outside counts as outside only when the zone declares
  no member of that name. OCaml values and types share one namespace here. The compiler catches what slips
  through, loudly; the checks are for the silent cases (capture, shadowing, a library member).
- An OCaml punned record label (`{ geheel; ... }`) of a type outside the zone binds a variable of the field's
  name; renaming that variable needs the pun written out, which is a code change, not a rename.
- Gate logs: an occurrence a decision kept as it was, in a string that a gate prints, shows as a difference.
