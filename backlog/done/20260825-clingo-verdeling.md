# Clingo real-world 4/4 — allocation/verdeling to ref parity

Goal: the heaviest clingo gap from the TOKA stress test (6 scenarios):
"allocatie/verdeling nog niet volledig vertaald". The S19 inventory
names non-proportional and conditional allocation and misaligned
allocation among the loud faults. Bring `backends/clingo` to ref parity
on the allocation forms `ref/` actually carries (RUNTIME-SEMANTICS
allocation chapter: recurrence over groups, maximum/rounding/remainder),
corpus-borne with own-authored Jutterswet goldens. Where ref itself
refuses a form, clingo keeps its loud fault — do not out-run the
reference. Ref is the meaning: no `ref/` changes, no semantics beyond
ref; the narrowest change that reaches parity; S19 updated, never
renumbered (S19 stays highest).

Acceptance:
- Red-first, shown in the Result: per newly covered allocation form a
  new own-authored Jutterswet golden (with cases pinning absolute
  values) that ref replays green and today's clingo refuses loudly
  (fault text quoted); after the change gate 6 agrees.
- Gates 1–9 all green (incl. gate 7 noop discipline; allocation cases
  are prime noop-refuters — justify any survivor).
- `spec/ASP-TRANSLATION.md` documents the allocation translation; S19
  shrinks.

May touch: `backends/clingo/`, `spec/ASP-TRANSLATION.md`,
`corpus/golden/jutterswet-*` and `corpus/golden/anchor/jutterswet-*`
(new own-authored goldens), `scripts/check-noop.sh` (survivor list only,
with reasons).
Out of scope: `ref/`, `spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md`,
`spec/schema/`, `spec/verifier/`, `spec/CONSTRUCTS.md` (do NOT touch),
condition kinds and windows (previous cards in this chain).

---

# RAPPORT (uitgevoerd 2026-08-25)

Onderzoek eerst: de stresstest-weigerklasse "allocatie/verdeling nog
niet volledig vertaald" (6 scenario's) viel op de draad uiteen in drie
vertaalfaults: "only the proportional allocation is translated yet"
(GELIJKE_DELEN en OP_VOLGORDE), "conditional allocation not translated
yet" (voorwaarde) en "allocation scale mismatch …: only the aligned
form is translated". ref draagt (ref/lib/alloc.ml, ongewijzigd) alle
drie methoden als instanties van de ch. 4.2-recurrentie; dat is exact
wat vertaald is — niets voorbij ref.

Vertaling (backends/clingo/lib/translate.ml, translate_verdeling
herschreven): één poort vdI_src(B) — is_a plus de conditie-atomen van
de voorwaarde (S7) — draagt de conditionele allocatie; elke
semantische guard en §9.5-afzetting rijdt op die poort (de
wrap-fences bewust NIET: grounding rekent de producten ongeacht de
conditiewaarheid — clingo weigert daar luid waar ref de bron enkel
overslaat). De schaallaag draagt misaligneerde declaraties: floor op
afrondingsschaal d ≤ t door de gehele deling zelf (pot ×10^(d−p) in de
teller of deler ×10^(p−d)), share-write ×10^(t−d), rest op eigen
schaal r ≥ max(p,d) over de EIGEN share-atomen; de wrap-grenzen
krimpen met precies de multipliers (46340² < 2^31; gelijke schalen
houden de historische 46340). Zonder gedeclareerde afronding schrijft
ref de exacte share en fault een niet-passende write — dat is nu een
exactheids-viol op doelschaal (voorheen floorde de "aligned" vorm daar
STIL: een gerepareerde stille divergentie, zie bijvangst). Methoden:
gelijke delen als één groep (vdI_n, zelfde negatieve/lege-pot-guards
als naar rato — het ch. 4.4-v0-gat); op volgorde als de recurrentie
zelf — vdI_grank telt voorgangers (lege sleutel LAATST in beide
richtingen), vdI_pot(B,R,P) deelt per rang de pot-bij-binnenkomst
(gewogen tie-break: lege weging reëel 0 + §9.5, negatieve 0 stil,
0-som-groep alle leden 0), shares gecapt/gefloord/geklemd op 0, en
max(0,P−ΣS) draagt door — de recursie door de assignment-aggregaten
grondt omdat de rang strikt stijgt onder de ranks-grens (proef vooraf
los in clingo gevalideerd tegen het ch. 4.6-voorbeeld). Negatieve
raws klemmen naar de reële 0 die ref toekent, dus geordend heeft geen
negatieve-pot-viol nodig en een lege pot is daar Nothing (§9.7.6).

Zeven nieuwe hand-authored goldens (eigen Jutterswet-materiaal, 11
cases, absolute verwachtingen, alle noop-refuterend — survivorlijst
onaangeraakt):
- jutterswet-gelijkdeling — gelijke delen: 90/3 en 1001/2 met floor en
  rest; bron zonder ontvangers schrijft niets (rest leeg);
- jutterswet-rangdeling — geordend ongewogen stijgend: uitputting over
  jaargroepen (101 → 50/50/1/0, rest 0), lege sleutel laatst, lege pot
  = Nothing naast een dragende bron, negatieve pot → reële 0'en en
  rest −50;
- jutterswet-rangmaximering — de volle recurrentie (cap + floor +
  uitputting; 1800 → 500/500/433/250/91, rest 26) plus de
  0/0-tie-groep, negatieve weging telt 0, lege weging reëel 0 mét
  gepinde §9.5-afzetting (rule × subject W5);
- jutterswet-rangaflopend — afnemend: hoogste rang neemt alles, lege
  sleutel ook dalend laatst;
- jutterswet-voorwaardedeling — naar rato onder een voorwaarde
  (score ≥ 10): ware, onware én lege conditie-arm (doelen blijven
  leeg);
- jutterswet-schaaldeling — hele-euro-pot en -cap in een centendoel
  onder 2-decimalenafronding (33.33/50.00, rest 16.67);
- jutterswet-centdeling — centenpot onder hele-euroafronding in een
  centendoel (62.00/62.00, rest 1.00).

Rood-eerst, vóór de wijziging (geciteerd; verifier PASS en ref
`oracle: PASS` op alle 11 cases eerst vastgesteld):
- gelijkdeling/rangdeling/rangmaximering/rangaflopend: `TRANSLATION
  FAULT: only the proportional allocation is translated yet`;
- voorwaardedeling: `TRANSLATION FAULT: conditional allocation not
  translated yet`;
- schaaldeling: `TRANSLATION FAULT: allocation scale mismatch (pot 1,
  target 100, floor 100, cap 1, rest 100): only the aligned form is
  translated`;
- centdeling: `TRANSLATION FAULT: allocation scale mismatch (pot 100,
  target 100, floor 1, cap 100, rest 100): only the aligned form is
  translated`.
Ná de wijziging: alle 11 `clingo: PASS`. Mutatieproef tegen vacuüm
groen: verwachting 434 i.p.v. 433 → `MISMATCH: W3.penningtoewijzing =
[433], expected 434` (clingo) en `= 433, expected 434` (ref);
niet-exacte onafgeronde splitsing (gewicht 59) → ref `EVALUATION
FAULT: … 5900/79 does not fit the declared 0 decimals` en clingo
`UNSATISFIABLE — … viol("allocation: a share does not fit the target
scale …")` — de gerepareerde flank weigert nu in béide executors.

Gates: alle negen groen — self-contained PASS; ir-schema 127 files
(round-trip byte-identiek, de goldens zijn in canonieke vorm
geschreven); verifier 127 golden / 143 negatief; case-schema 328;
oracle 308 cases (+11); clingo 308 (+11, ref ≡ clingo); noop 275
refuterend + 33 gepinde survivors (ongewijzigd); rood 20; catala 169
checks, 11 goldens byte-identiek.

spec/ASP-TRANSLATION.md: S12's allocatieparagraaf herschreven (poort,
schaallaag, drie methoden, recurrentie, fences) met de zeven getuigen;
S13-tabelrij en -proza bijgewerkt (allocatie-afzettingen rijden de
src-poort; de lege geordende tie-weging erbij); S19 kromp met precies
wat vertaalt en benoemt expliciet wat luid blijft: afronding FIJNER
dan de doelschaal, rest grover dan pot-/afrondingsschaal, en de
onafgeronde allocatie mét maximum (welke van exacte share of cap het
geschreven minimum is beslist ref's write-fault — geen corpuscase).
S19 bleef de hoogst genummerde sectie, niets hernummerd.

Bijvangst, eerlijk benoemd: (1) de onafgeronde "aligned" vorm
vertaalde vóór deze kaart met een STILLE floor waar ref een
niet-passende write fault — die flank draagt nu de exactheids-viol
(bergingstoezicht, exact delend, blijft groen); (2) de onafgeronde
vorm mét maximum, voorheen (alle schalen 0) stil-floorend vertaald, is
nu een luide vertaalfault — S19 documenteert hem; (3) de
proportionele rest sommeert nu de eigen share-atomen i.p.v. val/3
(de |Sh|>46340-fence verviel mét zijn reden: de som leest geen val
meer); (4) WF-024/025-backstopfaults toegevoegd (onbereikbaar op
verifier-schone invoer).

Bewust gelaten: allocatie-geldigheidsvensters (luide fault blijft —
vorige kaart in de keten); de drie S19-schaalvorm-fences hierboven;
geen rood-cases voor de nieuwe viols (corpus/rood valt buiten de
May-touch — kaartkandidaat: de exactheids-viol als rood-case in beide
executors pinnen); de negatieve-pot/gewicht-viols voor gelijk/naar
rato blijven clingo-nauwer dan ref (floor-vs-trunc-bescherming,
bestaand patroon); geen wijziging aan ref/, scripts/, spec/schema/,
spec/verifier/, spec/CONSTRUCTS.md.
