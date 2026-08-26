# TOKA slotmeting — the ref-100 goal witness

Goal: the maintainer's goal is ref at 100% on the stress measurement.
Yesterday's re-measurement (`backlog/done/20260825-toka-hermeting.md`)
scored ref 19/30 with eleven refusals: navigation 3, date/duration
family 7, band condition 1. The ref-100 wave has since landed (uchar
pin ≥ 83d48da, toolchain ≥ b4ba108): the day-precision calendar kernel,
date-part extraction + Easter, the datetime ms layer, both fold forms,
the narrowed self-reference, the type-pair fallback, the near-side
rolcheck, and the WF-084 ISO date-literal pin with its emitter fix.
Repeat the measurement with the SAME method (described in
`backlog/done/20260825-toka-stresstest.md`; artefacts from both prior
runs under /tmp/toka and /tmp/toka-hermeting): the same 30
Drools-verified scenarios, same case derivation, verifier on every
dump, ref and clingo per scenario. NB the emitter moved (WF-081/084):
regenerate the dumps with the CURRENT toolchain. Report per executor:
PASS count, refusal classes with counts, delta per class against
yesterday, and any NEW silent divergence (routes to triage, never to
acceptance). The goal condition: ref 30/30 exact agreement with the
Drools-anchored expectations — report honestly if any scenario falls
short, with its class.

RIGHTS RULE, absolute: no TOKA material enters uchar — all scenario
work in the toolchain checkout and /tmp; the card Result carries only
aggregated counts and class names.

Acceptance:
- The card lands in done/ with a Result carrying the final score
  (ref x/30, clingo y/30), the per-class delta table, and either "zero
  silent divergences" or the explicit triage list.
- Nothing outside `backlog/` touched in uchar; no toolchain commits.

May touch: `backlog/` only (uchar); toolchain and /tmp read-only work
space.
Out of scope: fixes (findings become card candidates), corpus
additions, toolchain commits, spec edits.

---

Result (slotmeting 2026-08-26, uchar b4abf75 / toolchain b4ba108):

**HET DOEL IS GEHAALD. Score: ref 30/30, clingo 15/30. Stille
divergenties: NUL.** Elk van de 60 executor-uitvoeren is een regel: of
een geverifieerde PASS met verwachtingsteller, of een expliciete fault.
De ref-100-doelconditie — ref 30/30 exacte overeenstemming met de
Drools-verankerde verwachtingen — is bereikt: alle 30 uitvoerbare
scenario's PASSEN in ref, elk niet-vacuous (45 verwachtingen getoetst
in totaal, per scenario ≥1, teller gelijk aan de case-`expected`-lengte).

**Methode-replicatie, mechanisch bewezen.** Zelfde meetopzet als de twee
eerdere metingen; `translate.py` verbatim hergebruikt. Met de HUIDIGE
toolchain (emitter bewogen, WF-081/084) opnieuw gegenereerd: de
volle-model-dump is BYTE-IDENTIEK aan die van gisteren, alle 30
IR-slices byte-identiek, alle 30 case-JSON's byte-identiek. Verklaring
van de emitterverschuiving: WF-084 (datumliteral → kale ISO-lexeme) is
voor deze casus een no-op — geen enkele slice draagt een Datum-getypeerd
literal in een regellichaam (data komen via peildatum/situatie de case
in, niet de IR); het enige ISO-datumtoken in het model stond al in
ISO-vorm. WF-081 (eenheid-in-lexeme) vuurt 0× — de emitterfix strippt de
eenheid al sinds de vorige meting. Netto: identieke invoer; alleen de
ref/clingo/verifier-binaries (de ref-100-golf) zijn veranderd, dus de
verschuivingen zijn zuiver toe te schrijven aan de golf, niet aan de IR.

**Verifier-inventaris — elke verschuiving verklaard.** Kruisproef: de
huidige verifier op gisterens byte-identieke dump geeft exact dezelfde
tally als op de nieuwe — bewijs dat de verschuiving de verifier-binary
is, niet de IR. Volle model: WF-053 2→0 (de near-side-rolcheck lost de
twee navigaties op die de verifier eerder weigerde) en WF-034 5→4 (de
versmalde zelfreferentie / type-paar-fallback / gearticuleerde folds
lossen één unknown-type-fold-sentinel op); WF-026 15×, WF-036 1×,
WF-045 1×, WF-079 1× ongewijzigd. Per-slice: WF-016 28× ongewijzigd
(het slicing-artefact); 2 startpunt-slices WF-053→CLEAN; op één
fold-slice valt WF-034 weg. Geen nieuwe WF (WF-081/084) vuurt op corpus
of casus.

**Delta per klasse, ref (11 weigeringen → 0):**

| klasse                     | hermeting | slotmeting | Δ  |
|----------------------------|-----------|------------|----|
| tijdsduur/datumverschil    | 2         | 0          | −2 |
| instance-fold-telling      | 2         | 0          | −2 |
| datum-tijd-rekenkunde      | 1         | 0          | −1 |
| datumfuncties              | 2         | 0          | −2 |
| navigatie-resolutie        | 3         | 0          | −3 |
| band-conditie              | 1         | 0          | −1 |
| **PASS**                   | **19**    | **30**     | **+11** |

De dag-precisie-kalenderkern, datumdeel-extractie + Pasen, de datetime-
ms-laag, beide foldvormen, de near-side-rolcheck en de versmalde
zelfreferentie sluiten samen alle elf resterende ref-weigeringen.

**Delta per klasse, clingo (23 weigeringen → 15):**

| klasse                         | hermeting | slotmeting | Δ  |
|--------------------------------|-----------|------------|----|
| expressiesoort (duur/fold/datumfunctie) | 7 | 2      | −5 |
| band-conditie                  | 1         | 0          | −1 |
| navigatie-resolutie            | 1         | 0          | −1 |
| niet-numerieke rekenoperand    | 2         | 1          | −1 |
| som-zonder-0-fallback          | 1         | 1          | 0  |
| niet-schaalbaar datatype       | 2         | 2          | 0  |
| allocatie-case-input/overschrijf | 6       | 6          | 0  |
| quote-dragende tekstlabels     | 3         | 3          | 0  |
| **PASS**                       | **7**     | **15**     | **+8** |

**Overeenstemming.** ref ≡ clingo ≡ Drools waar beide dragen: 15
scenario's (gisteren 6) — elk een geverifieerde PASS in beide executors.
Eerlijk over assertiesterkte: van refs 30 PASSen zijn 18
harde-attribuutwaarde-scenario's, 7 kenmerk, 5 consistentie-only; van
clingo's 15 zijn 3 harde-attribuutwaarde, 7 kenmerk, 5 consistentie-only.
De 2 niet-uitvoerbare scenario's (marker + de datum-tijd+datum-tijd
resolve-fout die de toolchain zelf als enige bekende CasusTest-fout
draagt) opnieuw eerlijk overgeslagen, zelfde status als beide vorige
metingen.

**Kaartkandidaten uit de resterende clingo-restanten** (fixes buiten
scope; ref heeft geen restanten meer): allocatie-case-input/overschrijf
6; quote-dragende tekstlabels 3; niet-schaalbaar datatype 2;
expressiesoort 2; niet-numerieke rekenoperand 1; som-zonder-0-fallback 1.

Rechtenregel nageleefd, absoluut: al het scenario-werk in /tmp en de
toolchain (uitsluitend gelezen/hercompileerd naar het ongetrackte out/,
git status daar schoon, nul commits); hier uitsluitend geaggregeerde
tellingen en klassennamen — geen scenario-/regel-/attribuutnaam, geen
citaat. Gates: alle 9 groen in deze worktree (143 golden schoon; 161
negatief afgewezen; 365 cases valide; 343 exact door ref én door clingo;
308 noop-refuters + 35 gepinde survivors; 22 rood in beide executors;
catala 169 checks, 11 goldens byte-identiek). Niets buiten `backlog/`
aangeraakt.
