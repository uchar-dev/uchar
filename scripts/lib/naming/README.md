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
| `prompts/translate.md`, `prompts/retry.md`, `prompts/decide.md` | the fixed instructions for the Haiku batches: a translation, its second round, a decision list (a range of rows, or a batch of `triage`) |
| `tables/<zone>.tsv`, `decisions/<zone>.tsv` | a rename card's table and decision list |
| `frozen/<zone>.tsv` | the names of a zone that code outside this repository reads (`freeze`, Frozen names) |
| `test_rename.py` | one fixture per danger (acceptance (a) of the card) |

## What a word is

A name is split into words (camelCase, snake_case, kebab-case, digit runs). A word is Dutch when the glossary has
it, `van` and `die` only inside a name of two or more words (`of` and `op` never: they are ordinary English code,
`valueOf`, `opCode`). A word neither in the glossary nor English (the dictionary with the regular English endings,
or a technical term) is searched for glossary words of four letters or more, so `Invoervormfout` is found and
`control` is not. Left alone: domain words (the words that the glossary does not have of the declared names in
`corpus/**/ir.json`, of the `jutterswet-*` directories, and of the RegelSpraak sources and IR documents in code:
an `.rgs` file, a string or comment of code with a declaration, the JSON of a string or of a `test/resources` file),
proper names, and a name equal to a token name of `RegelSpraakLexer.g4`. A declaration in code is read by lines in
the shape of its keyword (an object type writes a capital, a parameter a colon, a fact type is followed by a role
and a rule by `geldig`), so that English prose that starts with `Parameter` declares nothing. Where a declared name
is also a technical word of code, the glossary keeps it Dutch (`startdatum`: `STARTDATUM` in the frontend's
resolver). A name of domain words only is no Dutch name: `merge` leaves out an answer whose name is no Dutch name
any more, and `check` refuses a `translation` row for one; a batch lists the domain words of each name. A glossary
word one of whose translations is the word itself (`over`) never makes a name Dutch on its own.

`van` and `op` have glossary rows with their context (card `hernoem-drools-backend`), for the translator; they
keep the classes above. A batch lists `op` in a Dutch name with its rows: `by` for a key or a basis
(`rolOpNaam`: `roleByName`), `on` for what something acts on or holds for (`filterOpOnderwerp`:
`filterOnSubject`), `to` for a rounding (`afgerondOp2`: `roundedTo2`). `van` at the start or the end of a name is
part of what the name says and stays as `of` or `from` (`indexVan`: `indexOf`, `vanObject`: `fromObject`, `rVan`:
`rFrom`), as Java names say it (`indexOf`, `valueOf`, `fromString`); `check` refuses a `new` that drops it there.
Between two nouns English drops it (`labelVanBean`: `beanLabel`).

## Zones, occurrences and the outside

A zone is one or more path prefixes. The paths of the fixed exceptions of `CLAUDE.md` (the production runtime,
`toka/`, `spec/proposals/`) are never part of one, nor `backlog/` and this directory. An identifier in a zone file
is *outside* when it names a declaration outside the zone: read through a type or module outside the zone
(`IrLoader.Documentfout`, `A.kenmerk_soort` with `module A = Ir_v0.Ast`; a Java chain resolves from its outer type
or its package, so `ResolvedRegel.Soort.KENMERKTOEKENNING` is outside even when a zone type of the same package
nests a `Soort`), a Java constant that only code outside declares in a case label (`case GROTER_OF_GELIJK ->`), or a
constant of a switch over an enum declared outside, the type the file writes for the selector variable, else every
enum that declares all labels of the switch (card `hernoem-drools-backend`: `case SOM, TOTAAL ->` over the
frontend's `FunctieKind`, though the zone's `Accumulatie.Soort` declares `SOM`), a member read through a value
(`r.waarde()`, `d.feittypen`) unless a type of the zone declares that member (a Java field or method that is not
static, a private member or a member of a private type only in its own file, an OCaml record field) and, in Java,
the type of the expression before the dot may be a zone type: a variable has the type its declaration writes (the
declaration the read sees: the nearest local variable, parameter or pattern variable in scope, else the field; `var`
the type of its initializer), a call the return type of its method, a field its type, a cast its type, so
`f.argumenten()` on the frontend's `Expr.Functie f` is outside; whatever a value of code that sees nothing of the
zone gives is outside too (the frontend imports nothing of `backends/drools`: `d.excepties().leden()`,
`u.bronnen().get(0).sleutels()`, a lambda parameter over a stream of frontend values, the parameter of a zone
interface over frontend types that a method returns a lambda as); where the tool cannot derive the type (a type
variable, a lambda over zone values) unknown counts as may), a Java type declared only outside or that the file
imports or sees from outside (`Rol` imported from the frontend's `RegelSpraakModel`, beside the zone's
`Toetsitem.Rol`), a member of a supertype or a static import from outside, an OCaml constructor or field declared
only outside (record labels included), a name of an opened module outside. The inventory does not count those names,
`apply` leaves them alone, and `check` refuses a row whose name is declared only outside. In a string or a comment,
which is no code, a qualifier the file does not see is the zone's type when only the zone declares a Java type of
that name (card `hernoem-drools-backend`: a test of package `cases` expects the DRL text `Rekenhulpen.tijdvakken(`
of a class of package `tms`). In Java code a name behind a package of the repository (an import, `a.b.Type` in code)
is the type of that package, inside the zone when the zone declares it there.

## Frozen names

Code outside this repository can read names of a zone: the distribution `uchar-dev/regelspraak-toolchain` reads
types, packages and members of `backends/drools` and `frontends/regelspraak`. Such a name stays until that code is
renamed with it. `freeze` writes them to `frozen/<zone>.tsv`: a line `# zone: <prefix>...`, comment lines that say
which checkout and commit they were read from, then `name file count` (the file of that code that reads the name
most, and its reads in all of that code). In a zone that has a file of such a list's zone, the inventory reports
those names apart and does not count them, `propose` leaves them out, and `check` and `apply` refuse a row whose
`old` is one of them and whose scope covers a file of that zone.

A read is an identifier of the other code, outside strings and comments, that names a declaration of the zone: a
type or package of the zone (imported, qualified, in an import line), a member read through a type of the zone or
brought by a static import or a zone supertype, a member read through a value whose written type may be a zone type,
and a constant of a zone enum in a case label. An identifier that code declares itself and reads unqualified is its
own (a local `naam`, its own nested `Teller`, a test method of the same name). The type of a value is the type its
declaration in that file writes (`Wereld w`, `var w = new Wereld()`, `new Speler(build).speel(c)`); a library type
is no zone type, and a type of this repository or of that code is one only through a zone supertype. Where the code
writes no type (a lambda parameter, `var x = f()`, `a.b().c`), the read counts when a zone type declares a member
of that name: unknown counts as may. The list is by name, so it also holds the zone's locals of that name.

An enum of the zone keeps all its constants where that code reads them by their spelling (card
`hernoem-drools-backend`: the distribution's service writes `d.soort().name()` of `ScenarioResultaat.DekkingSoort` in
its JSON and `DekkingSoort.values()` in its OpenAPI, so `NIET_GERAAKT` stays, which it never writes): `name()` or
`toString()` through a value of the enum, `values()` or `valueOf` through the enum, a method reference `E::name`,
`E::toString` or `E::valueOf`, and a string literal whose value is one of its constants in a file that reads the enum
(through it, a value of it or a constant of it). Each such read counts for every constant of the enum. A spelling
alone is no read: RegelSpraak words and the other code's own enums spell constants of the zone (`KENMERK`, `INVOER`,
`REKENFOUT`; 141 names of `backends/drools` at the distribution's `97d25bb`), and a string in any file of that code
would count the vocabulary kinds `ATTRIBUUT` and `KENMERK` of its playground tests as reads of `RegelGraaf.NodeKind`.
The type before the dot is the one the classifier derives for the zone's own code (Zones, above); where it derives
none, a call does not count, since every value has a `toString()` (39 calls of `name()` or `toString()` of the
distribution at `97d25bb`; the three of them in files that read an enum of the zone are on other types).

The production runtime, which the tool never renames, reads names of `backends/drools` too (card
`hernoem-drools-backend`: `Namen.lidnamen` and `Namen.Lidnamen` in `runtime/Accessors.java`). Such a name stays in
the same way, without a list: the inventory reports it under `# frozen` with the runtime's path, `propose` and `merge`
leave it out, and `check` and `apply` refuse its row. A read counts as above, except that a read through a value
counts only where the runtime writes a zone type for that value: the repository compiles the runtime with the zone,
so a read the tool misses fails to compile, while one counted wrongly would keep a name for good
(`Breuk.van(1).delen(...)` reads the runtime's own `Breuk`, beside a `delen` of the zone). The runtime also reads a
property by its name, which no compiler checks: `get(seg, "waarde")` of `RegelSpraakRuntime` looks up `getWaarde`,
else `isWaarde`, by reflection, so those getters of the zone stay too (the segments of `Period`).

## Subcommands

- `inventory <prefix>... [--json] [--external] [--unknown N] [--by-zone DEPTH] [--quiet]` — every Dutch name of
  the zone with file and line, its count and the glossary's English words; exit 1 when it reports a name, 0 when
  the zone is clean. Identifiers outside strings and comments, file names (an OCaml file by its module name) and
  directory names. Frozen names follow under `# frozen`, with their list line, and do not count; with `--json`
  they carry a key `frozen`. A new name of a table of the zone (`tables/<zone>.tsv`) is no Dutch name in that zone
  where `check` reads it as English, a Dutch word in it being a translation the glossary gives of a word of its row
  (card `hernoem-drools-backend`: `roleLinks` of `rolKoppelingen` and `TEN` of `TIEN`, as `links` and `ten` are
  Dutch for left and at).
- `freeze <prefix>... <list> --using <checkout> [--exclude <path>]...` — writes the frozen list of the zone: its
  Dutch names that the Java sources of the git checkout read (see Frozen names), without the paths given to
  `--exclude` (`uchar/`, the distribution's submodule, which is a copy of this repository). Prints every name of
  the zone that the checkout spells as an identifier without reading it, with the reason, as `left out`.
- `propose <prefix>... <dir>` — writes `names-NNN.jsonl` and `sentences-NNN.jsonl` (test methods with `_` in
  their name), at most 200 names each, and `zone.json`, without the frozen names. One JSON object per line: `id`,
  `name`, `kind` (the declaration: `type`, `method`, `test`, `variable`, `parameter`, `constant`, `value`, `field`,
  `constructor`, `module`, `rule`, `label`, `file`, `dir`, or `reference`), `file`, `line`, `declaration` (the
  line), `doc` (the first sentence of the comment above), `words` (the proposed split), `dutch` (each Dutch word
  with the glossary's English words), `occurrences`, `files`. The answer of batch `X.jsonl` is `X.answer.jsonl`
  (`prompts/translate.md`).
- `merge <dir>... <table> [--force]` — the answers as a table; fails on a missing, double or unknown answer. A
  later directory answers again what an earlier one answered (a second round, a session's choice), with the
  format of `propose` (`zone.json` and batches whose items carry their earlier `id`), and its answer replaces
  the earlier one of the same `scope`; an answer with a scope its id has no answer for adds a row for that scope
  beside the others (a file where the name needs another new name). An id no earlier directory has is a name of its own, one the glossary made Dutch after
  `propose` (`compileer`, which the dictionary takes for English); it fails where an earlier id answers that name.
  A name frozen since `propose` (a frozen list, or a name the production runtime reads) gets no row, as `propose`
  leaves it out.
  An answer with a `reason` is an `improved` row, so the
  table stays the merge of its answers, never a file edited by hand. A
  flagged name becomes a row with an empty `new`, kind `improved` and reason `flagged: ...`, which `check`
  refuses until the session gives it a name. Every answer is written by its convention (Spelling, below): the
  translator wrote English words together in some batches. A directory or a file that is no code, with the name
  of an identifier the table renames, gets a row of its own with its path as scope where its neighbours spell
  the name otherwise (`dagsoorten`: `dayKinds`, the directory `day-kinds`), unless a code file below it holds
  the name.
- `merge-decisions <list> <answer>... [--corrections <answer>...]` — the decider's JSON lines (`row`, `old`,
  `decision`, `why`) into the column `decision`; fails on a missing or double row. The session's corrections of
  those answers are files of their own after `--corrections` (card `hernoem-drools-backend`), so the list stays the
  merge of its answer files: a correction replaces the earlier answer of its row, and fails when it repeats that
  answer, corrects a row twice or names no row; it fills no row the answers left open. The count of corrections per
  direction (`keep -> change`, `change -> keep`, `change -> change` for another new name) is printed.
- `triage <list> <dir>` — decides the rows of a decision list that a rule of the rename card or of `CLAUDE.md`
  decides by itself, before the decider sees the list (card `hernoem-drools-backend`: a table that renames
  identifiers named as ordinary words, `de`, `een`, `Regel`, puts 38.549 occurrences of `backends/drools` on the
  list). It reads the list as `apply` wrote it (no decisions; every row still holds its `old` at its line and
  column, or it fails) and the table of the list's name. It writes `<dir>/triage.answer.jsonl` in the format of
  `merge-decisions`, `why` starting with the name of the rule, and every other row in batches `rest-NNN.jsonl` of at
  most 180 rows, as equal in size as possible (`row`, `file`, `line`, `col`, `old`, `new`, `context`), with
  `INDEX.txt` (the full path and the count of each batch). The decider answers batch `X.jsonl` in `X.answer.jsonl`
  (`prompts/decide.md`); `merge-decisions <list> <dir>/triage.answer.jsonl <answers>...` fills the list. A rule
  decides only where it is certain; outside the zone only the first two hold, and every other row there goes to the
  decider:
  - `regelspraak` (keep): an `.rgs` file; a string of Java or OCaml code that the lexicon reads as a RegelSpraak
    source, the literal or the `+` chain of literals it stands in (`MODEL + "Regel ..."`).
  - `wire` (keep): `ir.json`, `case*.json`, or a JSON file whose top level holds the required keys of an IR document
    or a case; a string of Java or OCaml code that the lexicon reads as IR or case JSON, literal or chain. A shell
    script is read whole, so a heredoc that holds a program is no document, though JSON stands in it.
  - `message` (keep), zone only: a Java string at the top level of the arguments of `new X(...)` where `X` is an
    exception or an error, or a type of the repository whose supertypes reach one (the refusal types, `Weigering`);
    of JUnit's `fail`; the last argument of a JUnit assertion beyond its arity (`assertEquals(a, b, "...")`, not
    the expected `"..."` of `assertEquals("...", b)`); an argument of `println`, `print` or `printf` on
    `System.out`, `System.err` or a `PrintStream` or `PrintWriter` of the file; and a message's expectation, an
    argument of `contains`, `startsWith`, `endsWith` or `equals` on a receiver that reads `getMessage()`, or of an
    `assertEquals` whose other argument does. A string inside a nested call (`String.format`) is not decided.
  - `function-word` (keep), zone only: a name every glossary row of which is a function word (`de`, `dan`, `met`;
    `een` is also the numeral, `waar` also true), standing as a word of prose (whitespace or a quote before it;
    whitespace, a quote, `, ; : ! ?` or the period of a sentence after it; never a dot of a name, a bracket, a dash
    or a slash): in a comment outside code markup, unless its line is plainly English outside a quote; in a string
    that holds another Dutch marker word and no English one. A string or a quote with code characters (`[ ] { } $ =
    |`, an arrow) is a dump whose labels are function words (`join [] beta [] na [] bind`): not decided.
  - `code-markup` (change), zone only: a name in `{@code ...}`, `<code>...</code>` or backquotes of a comment, or in
    backquotes of a Markdown file, where the span is code (no space, quote, dash, slash or `$`), the name is no word
    of a declared name (of the corpus, a RegelSpraak source or an IR document in code; glossary words included,
    `gewicht`), and it is declared in that file, or a Java type of the zone only that the specification does not
    name (it names v0 nodes, `Kringen`; a test class counts always), or read through such types (`Lezer.lees`, not
    the runtime's `Breuk.DelingDoorNul.inDeRegel`). Where the row offers several new names, the decision is
    `change:<the first>`, the row at its place.
  - `dutch-prose` (keep), zone only: a lowercase name standing as a word of prose in a comment, not after a javadoc
    tag (`@param`), in Dutch text: two Dutch marker words or more, no English one and no code characters, read in
    the double-quoted span that holds the name, else in its line outside the quotes. A name in an English line is
    not decided: on `backends/drools` (446 rows) it was as often a RegelSpraak term (`leeg`, `gevuld`), a declared
    name of a test document (`netten`), an English word (`links`, `past`) or part of a quoted message as a reference
    to the identifier.
  - With `--default-keep`, two rules follow last, zone only (card `hernoem-drools-backend`): `string-default`
    (keep), every other string of the zone's code and every `.txt` file of the zone (the expected dumps), and
    `comment-default` (keep), every other comment of the zone's code. A kept name changes no behaviour. Whether a
    string must follow a renamed name depends on how the code makes it: a string literal stays, an enum constant, a
    class name or a record component follows the rename; the suite (and on `backends/drools` both branches of gate
    12) shows that after `apply --decisions`, and those rows are then turned to `change`. On `backends/drools`
    (38.542 rows) they keep 14.837 strings and 6.202 comment lines of Java and 104 `.txt` rows, mostly expected
    plan dumps, labels, test data, prose and terms of RegelSpraak or the spec. The other rules decide only where they
    are certain; these two are the card's choice, made at the call, so `README.md`, `pom.xml` and the rows outside
    the zone still go to the decider.
- `check <table> [--classpath <jars>]` — every row is checked and every error names its table line: the format;
  `old` is not frozen (`apply` refuses a frozen row as well); `old` occurs as a whole token (or file or directory
  name) in its scope; the style of `new` follows `old` (capital first, capitals, separator, no capitals inside
  when `old` separates its words with `_`; a one-word
  `old` lets `new` choose, except in OCaml and shell, whose names separate words with `_`: `fact_type`, not
  `factType`); the spelling of `new` is its convention (Spelling, below), for its identifiers and for each file
  and directory name it renames; `new` is no keyword; two rows of one name do not overlap at equal reach;
  injective (no two names onto one where a file reads both: a file where both are renamed, or one that sees the
  file of the other, as for fresh below; a row that renames a file or a directory name, in scopes that see each
  other); fresh (`new` is no name that stays where `old` is renamed unqualified, since a renamed read through a
  dot (`e.plek()`) is captured by no name that stays, and in Java a renamed variable meets no method that stays,
  since a call `text(n)` is no variable `text`: in that file, in
  the files of the zone supertypes of its Java types, in the zone modules an OCaml file opens; a name read through
  a dot such as `IrLoader.load` not counted; no member of a library or repository supertype of the zone types in
  those files — `java.lang.Object`, `Record` and `Enum` included, a nested one such as `Map.Entry` as well — read
  with `javap`; `--classpath` for jars outside the JDK);
  a renamed file lands on no existing file; a type or module of the zone that a row renames is not used as code
  outside the zone (Java code that resolves the name to a zone file, through its own types, imports, package or
  the qualifier before it; OCaml code, for an OCaml declaration of the zone; an OCaml constructor or a type of
  another Java zone with the same spelling is another declaration); no OCaml label of a type outside the zone is
  punned on the name (`{ geheel; ... }`, `{ A.objecttypen; ... }`); a renamed read through a value lands on no
  type outside the zone that has both members, `old` and `new` (there it would compile silently; a member of the
  read's language that is not private); `new` holds no Dutch word (a word the glossary gives as a translation of a
  word of the row's `words` is English there, also where the glossary has it as a Dutch word: `links` for
  `koppelingen`, `ten` for `tien`, `stand` for `staan`); for `translation` rows `words` joined is `old` (case and
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
  the zone's types and modules, renamed file names with their extension (`aggregatie.flogic`), a renamed directory
  in a path written from the repository root (`backends/drools/src/bronroute`, which gate 15 searches) and a
  one-word member read through the zone's Java type that declares it (`FactModelGenerator.creatiekoppeling`,
  `Gen#veld`: a static method under a type whose English name stays; card `hernoem-drools-backend`); outside the
  zone a shell script is read whole, as a text file, since it names classes and tests in words of code (`java -cp
  "$JAR" a.b.Main`, `MAIN=a.b.Main`, a test name a gate greps for), and `check` leaves those words to the list.
  A name of the table with a dash (`zoals-ref`, `twee-dagsoorten`: directories) is one occurrence, wherever it
  stands in a string, a comment or a text file, in the zone or outside it, and the words inside it are not looked
  at apart; it always waits for a decision, also in a comment, since it names a directory or a slug, not code
  (card `hernoem-drools-backend`: looked for per word, the 40 such names of `backends/drools` stood on no list,
  and `Path.of(..., "zoals-ref")` kept the name of a directory the files commit had renamed). An occurrence in the
  zone is offered the new name of the row at its place first, then the other new names of its name: a piece of a
  path takes the spelling of the renamed directory (`dagsoorten`: `dayKinds|day-kinds`), and a string that prints
  a record names its component (`waar=true`, where the row at its place gives where and the component is holds).
  With a list to fill, `apply` writes it, changes nothing and exits 3; with `--decisions` every listed
  occurrence must carry `keep` or `change` (or `change:<name>` when the name has several new names), and a
  missing or stale entry fails.
- `verify <table>... --base <commit> [--decisions <list>...] [--renderings <dir>] [--gate-logs <before> <after>]` —
  (1) applies the tables and their lists to a detached worktree of `<commit>` and compares it with the work tree,
  outside `scripts/lib/naming/`, `backlog/` and `.git-blame-ignore-revs`: handwork fails. A branch that renames
  two zones gives both tables: the file renames of each, then the contents, in the order given; with several
  tables a list goes with the table it is named after (`decisions/<table name>.tsv`). (2) scans the renderings for old
  names of two or more words, except one read through a qualifier that names a Java type outside the zone, a member
  of that type (card `hernoem-drools-backend`: the DRL text writes the frontend's
  `dev.uchar.regelspraak.ir.ResolvedVerdeling.Methode.NAAR_RATO`, beside the zone's renamed `NAAR_RATO`); (3) compares
  the gate logs after removing the `drools-jar:` lines and spelling
  durations, clock times and temporary paths alike, with the table applied to the before logs (names of two or
  more words, qualified names, renamed file names). A leg not asked for is reported as NOT RUN, and the verdict
  then says it is no landing proof.
- `render <dir>` — the renderings of every golden with case files: the DRL text (the test class that reads the
  property `uchar.drlvorm`, under `mvn -o test`), the session dump of the exec-model branch (the seam
  `uchar.vuurspoor` of the Drools jar, per golden) and the ASP program of every case (a `clingo` in front of the
  real one copies each program). Runs for minutes; start it in the background. The exec-model branch has no text
  form of its PatternDSL model in the repository; the session dump, which names every fact class and member the
  rendering inserts, stands in for it.

## Spelling

A new name is written by the convention of its language, with its English words apart, also where `old` is one
Dutch word (card `hernoem-drools-backend`). Old's own separator or humps decide first (`een_knoop_weigert`,
`labelVanBean`); then a constant in capitals separates its words with `_` (`WORTEL`: `SQUARE_ROOT`); then the place:
a file or directory name that is no Java class or OCaml module writes its words as the names beside it, the most
frequent separator among those of two words or more, and `-` where none has two (`bronroute` beside `main` and
`test`: `source-route`, as the repository's directory names `zoals-ref` and `jutterswet-*` do); an OCaml or shell
name separates them with `_` (`feittype`: `fact_type`); any other name is camelCase (`creatiediepte`:
`creationDepth`, `Sessiedump`: `SessionDump`). The first letter follows `old`. A run of letters in one case that is
no English word and that joins the English words of the row's translation (the glossary's words for the Dutch words
of `words`, and the other words of `words` as they are) is split at them; a test sentence with a code word in it
(`sectie_en_functionDefs`) keeps that word's humps. `merge` writes every answer so, and `check` refuses a `new`
written otherwise, with the spelling to write. A Java package directory would be written beside its neighbours
too; no package name in the repository has two words.

## The table

`scripts/lib/naming/tables/<zone>.tsv`, UTF-8, a line `# zone: <prefix>...`, then the header `scope old new kind
words reason`. `scope` is `*` (the zone), a path in the zone (a file or a directory, as the tree has it before
`--files-only`; it holds after the file renames as well, read forward through the rows), or a Java type or OCaml
module of the zone (`Outer.Inner` for a nested type; a path when a simple name is not unique), or a Java method
`Type#method` (every declaration of that name directly in the type). A Java type or method holds the comment,
annotations and modifiers above it, up to the member or statement before it: its comment speaks of its own names
(card `hernoem-drools-backend`: `waar` is "where" in most of `backends/drools`, "true" in a record component, whose
comment says `{@code waar}`, and in one parameter of a class whose other method has a `String waar`). A type or
method scope covers its own text only: a read of the member in another file takes the row of that file, except a
member read through a dot whose type the classifier derives as a zone type (`T.m`, `x.m()` with a zone type for `x`,
`T::m`), which takes the row of the member's declaration (card `hernoem-drools-backend`: `TmsTranspiler` renames its
locals `toets` as `testTerm`, beside a local `test`, and its reads `t.toets()` of the record `Toetsitem` as that
component, `test`). `old` and `new` are complete names; `kind` is `translation` or `improved`; `reason` is required
for `improved`.

The decision list: `file line col old new decision context`, `line` and `col` 1-based, `col` in characters.

## A rename card, step by step

0. When code outside this repository uses the zone: `freeze <zone> scripts/lib/naming/frozen/<zone>.tsv --using
   <checkout> --exclude <its copy of this repository>`; commit the list, and freeze again before the landing.
1. `inventory <zone>`; `propose <zone> <dir>`; the session runs one Haiku agent per batch with
   `prompts/translate.md`; `merge <dir> scripts/lib/naming/tables/<zone>.tsv`. A second round (the rows `check`
   refused, with `prompts/retry.md`) and the session's own choices are directories of their own, given after
   the first: `merge <dir> <round 2> <choices> <table> --force`.
2. The session names the flagged rows (`improved`, with a reason) and runs `check` until it is clean; commit.
3. `apply --files-only`; commit (only renames, so `git log --follow` keeps the history).
4. `apply`; when it writes `decisions/<zone>.tsv`: `triage <list> <dir>`; Haiku with `prompts/decide.md` per batch
   of `<dir>/INDEX.txt`; `merge-decisions <list> <dir>/triage.answer.jsonl <answers>...`, review (the session's
   corrections in a file of its own: `... <answers>... --corrections <file>`), commit the list, `apply --decisions
   ...`; commit; add the hash of that content commit to `.git-blame-ignore-revs` (GitHub reads it; locally `git
   config blame.ignoreRevsFile .git-blame-ignore-revs`).
5. A compile error is fixed in the table or the list, never by hand: `git reset --hard` to the files commit,
   apply again.
6. `render <dir>` and the full gate set before and after, then `verify --base <commit before step 3>
   --decisions ... --renderings <dir> --gate-logs <before> <after>`; `inventory <zone>` reports nothing.

## Limits, measured or known

- A word the glossary does not have whole is split at the glossary words inside it: `verzoek` became `ver|zoek`,
  `voorouder` `voor|ouder`, `verdeelt` `ver|deelt`, `vernieuw` `ver|nieuw`, and a glossary word inside an English
  compound the dictionary lacks is taken for Dutch (`rato` in `beangenerator`, as in `iterator`). The cure is
  the whole word: a glossary row, a technical term, a proper name (`java lang`: `javaLang` is no Dutch name) or
  a domain word (`maatgevers`, `bevoegdheid`, which a test declares).
- Java and OCaml are read by tokens, not parsed. A declaration the heuristics miss can make a zone name look
  outside; a name read through a value whose type the tool does not derive (in Java a type variable or a lambda
  over zone values, in OCaml every value) counts as outside only when the zone declares no member of that name. OCaml values and
  types share one namespace here. The compiler catches what slips through, loudly; the checks are for the silent
  cases (capture, shadowing, a library member).
- An OCaml punned record label (`{ geheel; ... }`) of a type outside the zone binds a variable of the field's
  name; renaming that variable needs the pun written out, which is a code change, not a rename.
- Gate logs: an occurrence a decision kept as it was, in a string that a gate prints, shows as a difference.
