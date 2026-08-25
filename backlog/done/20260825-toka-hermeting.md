# TOKA re-measurement — the stress score after the decision wave

Goal: yesterday's stress test (`backlog/done/20260825-toka-stresstest.md`,
method described there) scored the hourglass on 30 Drools-verified
real-world scenarios: ref ≡ Drools 18/30, clingo ≡ Drools 4/30, zero
silent divergences, all other outcomes loud refusals. Since then the
decision wave landed: WF-081 + the emitter unit-literal fix (the one
genuine outcome divergence), and clingo translations for non-numeric
scalars, validity windows, the counting composites (decision-table
disjunction) and the full allocation family. Repeat the measurement with
the SAME method on the new state (uchar pin ≥ 0240766, toolchain ≥
68cf8ce): the same 30 scenarios, the same case derivation from the
scenario expectations, the verifier on every dump, ref and clingo per
scenario. Report per executor: PASS count, refusal classes with counts,
the delta against yesterday per class, and — most important — any NEW
silent divergence (a differing value without a fault), which routes to
adversarial triage, never to acceptance.

RIGHTS RULE, absolute: no TOKA material enters uchar — all scenario work
happens in the toolchain checkout and /tmp; this card's Result carries
only aggregated counts and class names.

Acceptance:
- The card lands in done/ with a Result carrying the new score
  (ref x/30, clingo y/30), the per-class delta table against
  yesterday's counts, and either "zero silent divergences" or the
  explicit triage list.
- Nothing outside `backlog/` touched in uchar; no toolchain commits.

May touch: `backlog/` only (uchar). The toolchain and /tmp are working
space, read-only for the repo.
Out of scope: fixes (findings become card candidates), corpus additions,
toolchain commits, spec edits.

---

Result (hermeting 2026-08-25, uchar b80c073 / toolchain 68cf8ce):

**Methode-replicatie, bewezen.** Zelfde meetopzet als gisteren, mechanisch
geverifieerd: alle 30 case-JSON's byte-identiek aan gisterens afleiding;
28/30 IR-slices byte-identiek — de 2 gewijzigde zijn precies de twee
unit-literal-scenario's (de emitterfix chirurgisch zichtbaar); de
volle-model-dump identiek op de 2 unit-literal-regels na; de per-slice
verifier-inventaris identiek (28x WF-016-slicing-artefact, 2x WF-053); het
volle model dezelfde 6 WF-klassen met dezelfde tellingen (WF-026 15x,
WF-034 5x, WF-053 2x, WF-079 1x, WF-045 1x, WF-036 1x). Kruisproef: de
nieuwe verifier meldt op gisterens dump 2x WF-081 en op de nieuwe dump 0x —
WF-081 en de emitterfix sluiten exact op elkaar. De 2 niet-0-scenario's
(marker/resolve-fout) opnieuw eerlijk overgeslagen, zelfde status als
gisteren. De toolchain is uitsluitend gelezen/gedraaid (hercompilatie naar
out/); git status daar schoon, nul commits.

**Score: ref 19/30 (+1), clingo 7/30 (+3). Stille divergenties: NUL.**
Alle 60 executor-uitvoeren zijn een regel: of een expliciete fault, of een
geverifieerde PASS met verwachtingsteller. Geen mismatch-zonder-fault;
triagelijst leeg.

Delta per klasse, ref (12 -> 11 weigeringen):
- unit-dragende literal 2 -> 0: een scenario nu exact akkoord op een harde
  attribuutwaarde; bij het andere schoof de fout een laag dieper naar een
  nog niet gedragen conditiesoort (band-conditie) 0 -> 1.
- navigatie-resolutie (casus-omissie) 3 -> 3, ongewijzigd.
- datum/duur-familie 7 -> 7, ongewijzigd (tijdsduur 2, instance-fold 2,
  datum-tijd-rekenkunde 1, datumfuncties 2).

Delta per klasse, clingo (26 -> 23 weigeringen):
- niet-numerieke datatypes (unscalable) 4 -> 2: twee scenario's nu akkoord.
- beslistabel/disjunctieve conditie 3 -> 0, gesloten: een scenario nu
  akkoord; bij drie andere kwam daaronder een NIEUWE klasse bloot:
  quote-dragende tekstlabels die de ASP-stringencodering weigert 0 -> 3
  (het tekstfideliteitspunt 12b uit gisterens inventaris, nu op de
  clingo-laag).
- vensters+som 2 -> 1: validity-windows gesloten (de fout schoof daar
  dieper: niet-numerieke rekenoperand 1 -> 2); som-zonder-0-fallback
  blijft 1.
- unit-lexeme 1 -> 0, gesloten.
- verdeling 6 -> 6, maar de klasse VERSCHOOF wezenlijk: niet langer "niet
  vertaald" maar een uniforme expliciete weigering omdat de case input
  levert voor het allocatiedoel dat ref/v0 overschrijft (geen monotone
  encodering). De vertaling van de allocatiefamilie is geland; de
  resterende blokker is case-vorm/overschrijfsemantiek.
- rest: expressiesoort 6 -> 7 (het conditie-gesloten datumfunctie-scenario
  schoof deze klasse in), navigatie 1 -> 1, conditiesoort (band-conditie)
  1, zelfde scenario als bij ref.

**Overeenstemming.** ref = clingo = Drools waar beide dragen: 6 scenario's
(gisteren 3), waaronder een harde attribuutwaarde. Eerlijk over
assertiesterkte: van clingo's 7 zijn 3 consistentie-only (waaronder de ene
clingo-only PASS waar ref op navigatie weigert — zelfde asymmetrie als
gisteren, geen waarde-divergentie), 3 kenmerk-asserties, 1 harde
attribuutwaarde; van refs 19 zijn 14 harde-attribuutwaarde-scenario's, 3
kenmerk, 2 consistentie-only.

**Kaartkandidaten uit de delta** (fixes buiten scope): (1) clingo:
allocatie-case-input/overschrijfsemantiek — 6; (2) clingo: quote-dragende
tekstlabels in de ASP-stringencodering — 3; (3) ref+clingo: band-conditie
— 1+1. De overige klassen stonden al in gisterens gaten-inventaris.

Rechtenregel nageleefd: al het scenario-werk in /tmp en de toolchain;
hier uitsluitend geaggregeerde tellingen en klassennamen. Gates: alle 9
groen in deze worktree (130 golden schoon; 147 negatief afgewezen; 335
cases valide; 315 exact door ref en door clingo; 282 noop-refuters + 33
gepinde survivors; 20 rood in beide executors; catala 169 checks, 11
goldens byte-identiek).
