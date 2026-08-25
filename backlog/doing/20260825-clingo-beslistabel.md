# Clingo real-world 3/4 — decision-table / disjunctive condition kinds

Goal: the TOKA stress test's "decision-table / disjunctive condition
kind" refusal class (3 scenarios): decision tables desugar upstream
(`BeslistabelDesugar`), but the desugared v0 wire carries condition
shapes — disjunctive/composite condition kinds among them — that
`backends/clingo` refuses while `ref/` evaluates them. Investigate the
exact wire shapes first (the S19 inventory names "the ten-minste-1
composite union", "the static-comparison composite member", "disjunctive
condition kinds" — read `backends/clingo/lib/translate.ml` reason strings
and the stress-test report `backlog/done/20260825-toka-stresstest.md`),
then bring clingo to ref parity on the forms ref carries, corpus-borne.
Ref is the meaning: no `ref/` changes, no semantics beyond ref; loud
faults for whatever remains; S19 updated, never renumbered.

Acceptance:
- Red-first, shown in the Result: per newly covered condition shape a new
  own-authored Jutterswet golden that ref replays green and today's
  clingo refuses loudly (fault text quoted); after the change gate 6
  agrees.
- Gates 1–9 all green (incl. gate 7 noop discipline).
- `spec/ASP-TRANSLATION.md` documents the translation; S19 shrinks.

May touch: `backends/clingo/`, `spec/ASP-TRANSLATION.md`,
`corpus/golden/jutterswet-*` (new own-authored goldens),
`scripts/check-noop.sh` (survivor list only, with reasons).
Out of scope: `ref/`, `spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md`,
`spec/schema/`, `spec/verifier/`, `spec/CONSTRUCTS.md` (do NOT touch),
allocation (next card), validity windows (previous card).

---

# RAPPORT (uitgevoerd 2026-08-25)

Onderzoek eerst: de weigerklasse "decision-table / disjunctieve
conditie-kind" is op de v0-draad de Samengesteld-conditie met een
tellende kwantor. ref (ref/lib/eval.ml, eval_cond) telt de ware leden
(hits) en vergelijkt: GEEN hits=0, TEN_MINSTE hits>=drempel,
TEN_HOOGSTE hits<=drempel, PRECIES hits=drempel; clingo vertaalde
alleen ALLE|DE (concatenatie) en liet al het tellende in de
catch-all-fault vallen. De S19-vermeldingen "ten-minste-1 composite
union" en "static-comparison composite member" betreffen de
gedurende-CLIP-context — daar weigert ref zelf ook ("loud in both
executors"), dus die vallen buiten ref-pariteit en blijven staan.

Vertaling (backends/clingo/lib/translate.ml, cond_atoms): per tellende
compositie een geïndexeerde helper per lid — cqN(S,i) :- is_a(S,ot),
⟨lid-atomen⟩ — precies afleidbaar waar ref het lid als hit telt (een
leeg operand faalt de positieve join: lid telt onwaar, Tabel 38
gratis), plus één telregel cqN_n(S,H) :- is_a(S,ot), H = #count { I :
cqN(S,I) }. De compositie zelf levert enkel platte atomen (cqN_n(S,H)
en de drempelvergelijking), zodat ze in élke cond_atoms-positie werkt,
aggregaat-elementen incluis (een body-aggregaat zou daar niet mogen).
De niet-monotone kwantoren (GEEN/TEN_HOOGSTE/PRECIES) zijn gedekt
door de verifier: de WF-045/046-afhankelijkheidswandeling markeert hun
leden conservatief als negatieve kanten, dus het gegronde programma
blijft gestratificeerd. Een tellende compositie zónder leden fault
luid; elk lid-soort behoudt zijn eigen fault (Periode-lid,
Rollidmaatschap, Onopgelost, periode-dragende vergelijking, genegeerde
gevuld) — niets degradeert stil.

Vier nieuwe hand-authored goldens (eigen Jutterswet-materiaal, 12
cases, absolute verwachtingen, alle noop-refuterend — survivorlijst
onaangeraakt):
- jutterswet-rubriektabel — TEN_MINSTE 1 (de disjunctieve
  beslistabelrij over tekstgelijkheden) én TEN_MINSTE 2 over gemengde
  leden (twee kenmerken + numerieke vergelijking), drempelgrens
  stuks=10 gepind;
- jutterswet-vrijgavetoets — GEEN, met de leeg-lid-telt-onwaar-hoek
  (case3: bezwaren afwezig → status 'vrijgegeven');
- jutterswet-sluimerwacht — TEN_HOOGSTE 1, hits 1/2/3 rond de drempel;
- jutterswet-tweevoudkeur — PRECIES 2, hits 2 (twee routes) en 3.

Rood-eerst, vóór de wijziging (geciteerd): verifier PASS op alle vier;
ref `oracle: PASS (3 expectations)` resp. `(2 expectations)` op alle
12 cases; clingo op elk van de vier: `TRANSLATION FAULT: condition
kind not translated (no corpus case exercises it)` (exit 1). Ná de
wijziging: alle 12 cases `clingo: PASS`. Mutatieproef tegen vacuüm
groen: verwachting 'drievoudig' i.p.v. 'tweevoudig' → `MISMATCH:
K1.oordeel = ["tweevoudig"], expected "drievoudig"`; kenmerk-flip →
`MISMATCH: S1 lacks characteristic "sluimerend"`.

Gates: alle negen groen — self-contained PASS; ir-schema 120
files; verifier 120 golden / 143 negatief; case-schema 317; oracle
297 cases (+12); clingo 297 (+12, ref ≡ clingo); noop 264 refuterend
+ 33 gepinde survivors (ongewijzigd); rood 20; catala 169 checks, 11
goldens byte-identiek.

spec/ASP-TRANSLATION.md: S7 draagt de tel-encodering met de vier
getuigen; S19 kromp met precies wat vertaalt (de scalaire tellende
composities verlieten de catch-all; de restanten staan expliciet) en
bleef de hoogst genummerde sectie, niets hernummerd.

Bewust gelaten: de gedurende-composieten (ten-minste-1-unie,
static-comparison-lid, genest bullet) — ref weigert die zelf, dus
buiten ref-pariteit; de ledenloze tellende compositie (luide fault, ref
zou GEEN op [] als waar evalueren — geen corpuscase, fault boven stil
gokken); de vormenkabinet-peilingregel blijft in het kabinet (haar
Periode-lid weigert in beide executors); geen wijziging aan ref/,
scripts/, spec/schema/, spec/verifier/, spec/CONSTRUCTS.md.
