# Enclosing-scope Let binder inside a closed fold body — WF-064 gap + clingo grounding crash

Goal: close the gap where a `Let` binder from an ENCLOSING scope, referenced
inside a closed fold/function body, escapes WF-064 and reaches the backends as
ill-formed IR. Today `Let(f = <parameter>) in Fold(+, 0, λacc x. acc + x*f)` —
the mandated "Maal in a fold in a Let" composition — passes the verifier, then
ref faults cleanly (`EVALUATION FAULT: unbound binder "f" (WF-064 should
preclude this)`) while clingo ungracefully grounding-crashes (`error: unsafe
variables ... 'LB3' is unsafe`, clingo exit 65) instead of emitting a clean
TRANSLATION FAULT. Both executors ultimately fault (no wrong answer) and the IR
could never become a golden, so this is a robustness/diagnosis gap, not a
soundness hole. Two candidate closes: (a) extend WF-064 so `spec/verifier/`
rejects an enclosing-scope binder reference inside a closed body; and/or (b)
have `closed_term`'s `Binder_ref` case fault cleanly ("unbound binder in a
closed body") when the resolved binder carries binding atoms it would drop,
rather than emitting an unsafe rule that crashes gringo.

Provenance: lens BEVINDING 2 [MINOR/CONFIRMED] on card
`20260825-clingo-bodymult` (backends/clingo/lib/translate.ml:2055). Pre-existing
— the `closed_term` Plus/Min case predates that card and triggers the identical
`([], var, scale)` drop; the new Maal path merely adds another route to reach
it. Reproduced 2026-08-25: verifier PASS, ref clean fault, clingo `LB3` unsafe
grounding error (exit 65).

Acceptance: an ill-formed witness like the one above is rejected — either by the
verifier (gate 3, a negatief case citing WF-064) or by a clean clingo
TRANSLATION FAULT — and no path emits an unsafe rule that crashes grounding; all
nine gates green.

May touch: spec/verifier/ (if the WF-064 route is chosen — protected, propose
only), backends/clingo/, corpus/negatief/ (the WF-064 witness), spec/IR-SPEC.md
(WF-064 text — protected, propose only), backlog/.

Out of scope: ref/ (already faults correctly); the multiplicative closed-body
broadening (landed on `20260825-clingo-bodymult`).

Besluit (maintainer, 2026-08-25): uitvoeren — het vierde slot, in de
lijn van het hekken-besluit van 2026-08-24. De lezing van een búíten
de gesloten body gebonden binder (Let-binder van de omsluitende
regel gelezen in een fold-/lambda-body) wordt onwelgevormd: de
verifier weert hem vóór de executors (die er nu verschillend maar
beide luid op struikelen). Volg het voorstel hierboven: bepaal of
dit een WF-064-verbreding of een nieuw nummer is (aansluitend op de
index), verifier-check, rood-eerst, minstens één negatief-case,
corpus-breed gevalideerd. LET OP de parallelle S19-kaart: een Let
BINNEN de body wordt daar juist gedragen — de check moet exact de
buiten-binder-lezing raken, niet de binnen-body-Let.

---

Landing (2026-08-25): een WF-064-**verbreding**, niet een nieuw nummer.
Het voorstel (candidate a) beargumenteert "extend WF-064" en de ref-fault
zegt het al letterlijk — `unbound binder "f" (WF-064 should preclude
this)` — dus WF-064 is het juiste tehuis. De verbreding is de
**closure-clausule**: binnen een gesloten body (een lambda-/functie-body,
de bodies die WF-069/WF-067 gesloten houden) is de enclosing-binder-set
*gesloten aan de grens*. Een `BinderRef` mag daar alleen de eigen
parameters raken of een binder die BINNEN de body gebonden wordt (een
match-arm-binder, een binnen-body-`Let`, een filter-binder); reikt hij naar
BUITEN — een `Let`-binder van de omsluitende regel, of de parameter van een
buitenste fold — dan is hij onwelgevormd. De binder-tegenhanger van de
attribuut-/aggregatie-/temporele/variabele-lezingen die WF-069/WF-067 al
weren.

Implementatie (`spec/verifier/lib/checks.ml`): scherp en minimaal. Bij het
kruisen van de gesloten-body-grens (`check_fold_fn`'s lambda-body en
`wf067`'s functie-body) worden de omsluitende `binders` **weggelaten** — de
body ziet nog slechts zijn eigen parameters — waardoor een buiten-binder
vanzelf niet meer resolvet en in de bestaande WF-064-tak valt. `Closed_pos`
draagt nu de weggelaten namen mee (`body_pos = Closed_pos of string *
string list`) puur om de diagnose te verscherpen: een buiten-lezing citeert
de naam ("bound outside the enclosing closed body"), een echt ongebonden
naam houdt "resolves to no enclosing binder". Shadowing blijft correct
(inner-only `binders`); een `Let` BINNEN de body blijft welgevormd (de
binder is een INNER-binding — de S19-kaart raakt dat niet).

Rood-eerst — het minimale document dat de OUDE verifier accepteert en de
NIEUWE afkeurt (`corpus/negatief/wf064-enclosing-binder-in-closed-body/`,
de mandaat-compositie `Let(f=<parameter>) in Fold(+, 0, λacc x.
acc + x*f)` — "Maal in a fold in a Let"):

- oude verifier: `verifier: PASS`
- nieuwe verifier:
  `WF-064 FAIL /consistenties/0/assertie/rechts/body/function/body/rechts/rechts: binder reference 'f' is bound outside the enclosing closed body — a closed function or lambda body reads only its own parameters and inner bindings`

De executor-split die het hek voorkomt (een runbare BEREKENING-variant van
hetzelfde getuige, direct langs ref en clingo — de verifier weigert hem nu,
dus dit is het gedrag ACHTER het hek):

- ref: `EVALUATION FAULT: unbound binder "f" (WF-064 should preclude this)`
  (schone fault, exit 1)
- clingo: `error: unsafe variables in: ... |LB3|>46340` / `note: 'LB3' is
  unsafe` → `*** ERROR: (clingo): grounding stopped because of errors` →
  `clingo exited 65` (ongracieuze grond-crash, geen nette TRANSLATION FAULT)

Beide struikelen luid (geen fout antwoord), maar verschillend — precies de
robuustheids-/diagnose-leemte. WF-064's closure-clausule weert de
ill-gevormde IR nu op gate 3, vóór beide executors.

Corpus-breed: gate 3 vóór = `104 golden clean; 142 negatief`; ná =
`104 golden clean; 143 negatief`. Geen enkele golden overrejecteert — de
104 anchors/goldens die folds, lambdas, Let-binders en functie-bodies
gebruiken blijven schoon; de verbreding raakt uitsluitend de
buiten-binder-lezing.

Alle negen gates groen (slotregels):
1. `check-self-contained: PASS`
2. `check-ir-schema: PASS (schema valid; 104 file(s) validate and round-trip)`
3. `check-verifier: PASS (104 golden clean; 143 negatief rejected for their declared rule)`
4. `check-case-schema: PASS (schema valid; 279 case file(s) validate)`
5. `check-oracle: PASS (261 cases replayed exactly; NOT replayed, no case file yet: corpus/golden/jutterswet-vormenkabinet)`
6. `check-clingo: PASS (261 cases replay exactly through clingo — ref = clingo over the corpus)`
7. `check-noop: PASS (228 cases refute a do-nothing executor; 33 pinned survivors pin defeasible standing)`
8. `check-rood: PASS (18 red cases fault observably in both executors)`
9. `check-catala: PASS — frontend suite green (169 checks); 11 catala golden(s) regenerate byte-identically`

Buiten scope, bewust gelaten: de clingo grond-crash zelf (candidate b, een
nette `closed_term`-TRANSLATION-FAULT) — het besluit koos de verifier-route,
en achter het hek is de crash onbereikbaar voor welgevormde IR. `ref/`
faultte al correct. `spec/CONSTRUCTS.md` niet geraakt.

Protected paths aangeraakt (propose-only, maintainer merget):
`spec/IR-SPEC.md` (WF-064-tekst + tabelrij), `spec/verifier/lib/checks.ml`.
