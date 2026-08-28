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
