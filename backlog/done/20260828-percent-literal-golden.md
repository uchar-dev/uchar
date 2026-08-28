# A frontend witness for the bare percentage literal

Goal: the IR-level Percentage-typed Literal is already witnessed
(anchor/jutterswet-vindersloon, synth2-afrondingscombinaties), but NO
bron.rgs-carrying golden exercises the frontend's bare-%-literal path
(grammar frontends/regelspraak/grammar/RegelSpraak.g4:973 getalWaarde with
PERCENT_SIGN; resolver ExpressieBouwer.java:200 emits a Percentage-typed
Literal) — gate 10's byte-identical re-emission never round-trips the
%-lexeme. The two rgs-goldens that do contain % (synth-navigatiepremie,
jutterswet-bergloon) use it as the left operand of PERCENTAGE_VAN, which
the frontend types as Numeriek, not Percentage. One rights-free,
own-authored Jutterswet golden WITH bron.rgs closes this: its RegelSpraak
source uses a BARE percentage literal in a value position the grammar
admits (e.g. an attribute set equal to 21%, or a comparison against 21%) so
the emitted ir.json carries a Percentage-typed Literal produced by uchar's
own frontend. VERIFY FIRST that the grammar/resolver admit the bare %
literal at the chosen position (a quick emit test); if no position works,
report that honestly and stop — do NOT modify the frontend.

The ir.json is emitted through the frontend (make/emit path of gate 10) so
it is byte-identical by construction; hand-computed case expectations from
RUNTIME-SEMANTICS (mind the percentage semantics: how a bare Percentage
value behaves in the chosen position — compute by the spec, never copy an
executor); noop-refuting. NO code changes anywhere.

Acceptance:
- The golden passes gates 2-8; gate 10 re-emits it byte-identically
  (run gate 10 and show the count went from 19 to 20 rgs-goldens);
  gate 12 plays it or carries a reason-carrying, evidence-backed exclusion.
- All twelve gates green in the worktree.
May touch: corpus/golden/** (the new golden only, WITH bron.rgs),
scripts/check-drools.sh (ONLY for a reasoned exclusion), backlog/.
Out of scope: frontends/** (code/grammar), backends/**, ref/, spec/**,
toka/**, scripts/check-noop.sh, spec/CONSTRUCTS.md, spec/PLAN.md, any
toolchain commit.
Registered observation (NOT this card): the anchor jutterswet-vindersloon
types the operand of "20% van" as Percentage while uchar's frontend types
the same construct as Numeriek — a latent frontend-vs-anchor form question
for a later card.

Werkregels (bindend):
- Werk UITSLUITEND in de worktree /Users/yeb/Developer/uchar-dev/wt/percent-literal-golden (branch card/percent-literal-golden, basis main 4959647). Root-checkout /Users/yeb/Developer/uchar-dev/uchar NIET aanraken (behalve jars kopiëren, zie onder). Geen merges, geen push.
- Eerste commit: maak backlog/doing/20260828-percent-literal-golden.md met de kaarttekst hierboven (verbatim). Na een heredoc-append altijd expliciet git add in dezelfde &&-keten.
- RECHTENREGEL: geen ©-TOKA-inhoud; eigen Jutterswet-materiaal (strandjutten-domein, Nederlandse namen).
- Frontend-emissie: kopieer eerst frontends/regelspraak/lib/*.jar uit /Users/yeb/Developer/uchar-dev/uchar/frontends/regelspraak/lib/ naar de worktree; bouw en emit zoals gate 10 / de frontend-README het doet (bestudeer scripts/check-regelspraak.sh voor de emit-aanroep). De ir.json in de golden is de frontend-emissie zelf.
- Verwachtingen HANDBEREKEND; berekening in NOTE.md. Percentage-semantiek: lees spec/RUNTIME-SEMANTICS.md over percentages vóór je de casuswaarden kiest, en kijk hoe anchor/jutterswet-vindersloon zijn case doet.
- OCaml: eval $(opam env); dune build vanaf de workspace-root. Gates: ALLE twaalf; voor 12: mvn -q -o -f backends/drools/pom.xml package.
- Gate-12-eerlijkheid: verwachting NOOIT aanpassen; exclusie alleen reden-dragend met bewijs.
- Rapporteer compact: de gekozen grammaticapositie + emit-bewijs, gate-uitkomsten (met de 19→20-telling van gate 10), handberekening, diff-stat.

## Result (2026-08-28, card percent-literal-golden)

Authored one rights-free, own-authored Jutterswet golden
(`corpus/golden/jutterswet-vondsttarief/`, WITH `bron.rgs`) that witnesses
the frontend's bare-percentage-literal path. NO code anywhere: frontends,
backends, ref, spec, toka, `check-drools.sh`, `check-noop.sh` all untouched.

### Grammar positions (both admitted; verified by emit test before authoring)
- **berekening RHS**: `Het bergingstarief van een Vondst moet berekend worden als 21%.`
- **comparison RHS**: `... indien het vondsttarief van de Vondst gelijk is aan 21%.`

The frontend (`getalWaarde : NUMBER PERCENT_SIGN`, RegelSpraak.g4:973 ->
`ExpressieBouwer.java:200`) emits a `Literal{waarde:"21", type: Percentage,
bron:"21%"}` at both positions — the round-trip gate 10 never exercised (the
two existing %-goldens use `%` only as the Numeriek left operand of
`percentage van`). `diagnostics: []`. `ir.json` is the frontend emission
itself, so gate 10 re-emits it byte-identically by construction.

### Hand computation (spec, not copied from an executor)
Bare `21%` is NOT `0,21`: RUNTIME-SEMANTICS ch. 2.2 (l.288) puts the `/100`
in the `percentage van` operator ONLY; no operator applies here, so `21%`
denotes magnitude **21** in `%` units. berekening `bergingstarief := 21`
(0-dec into 0-dec, no rounding). comparison `vondsttarief = 21` true iff the
input equals 21 (both operands gevuld, identical `%` unit factor 1). Cases:
V1(21) -> bergingstarief 21, standaardbelast present; V2(9) -> bergingstarief
21, standaardbelast absent; `consistent`. Computed outputs are absent from the
input world -> noop-refuting (not a survivor). ref (gate 5) = clingo (gate 6).

### Review round (both lens minors processed)
- **NOTE.md gate-12 block (LENS 1)**: corrected — the CLI prints EXACTLY one
  line (the berekening's `waarde buiten scope`, `:553`); the transpiler
  short-circuits on the first regel it cannot lower, so the comparison's
  `voorwaarde buiten Phase-B scope` (`:306`) is never reached or printed. The
  earlier prose implying both messages appear was wrong; verified by direct
  `java -jar` on the golden.
- **NOTE.md percentage derivation (LENS 2)**: recast to rest on the spec
  (ch. 2.2) alone; ref/clingo now cited as independent confirmation of the
  hand-computed 21, not as the source. Dropped the `ref/lib/value.ml` /
  `Arith.pct_of` executor-internal citations.

### All twelve gates green in the worktree (re-run after the review fixes)
1 self-contained PASS · 2 ir-schema PASS (152 validate+round-trip) ·
3 verifier PASS (152 golden clean, 161 negatief) · 4 case-schema PASS (378) ·
5 oracle PASS (356 exact) · 6 clingo PASS (356, ref=clingo) ·
7 noop PASS (323 refute, 33 survivors — new golden is NOT a survivor) ·
8 rood PASS (22) · 9 catala PASS (169 checks, 11 goldens) ·
10 regelspraak **PASS — 19 -> 20 rgs-goldens byte-identical, 130 JUnit tests** ·
11 toka-parity PASS (30 ref, 30 clingo, 30 drools-exclude) ·
12 drools **PASS (52 played / 73 auto-excluded / 4 value-divergence / 1 no-case)**.

Gate 12 auto-excludes the new golden via channel 1 (per-form transpiler SKIP,
`waarde buiten scope`) — a bare-`%` witness is intrinsically Percentage-typed,
so it cannot both carry the `%`-lexeme AND be lowered by the v0 Drools
exec-model. Evidence it is exactly the `%`-form: the Numeriek-retyped shape
plays. `scripts/check-drools.sh` is UNTOUCHED (not in the divergence list).

NO merge, NO push. The registered anchor-vs-frontend `20% van` typing
question was left for a later card (out of scope here).
