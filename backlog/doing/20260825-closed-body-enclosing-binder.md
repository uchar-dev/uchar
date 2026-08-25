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
