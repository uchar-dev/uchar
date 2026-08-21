# De zeventien grove inconsistent-cases verscherpen met rule/label-pins

Besluit maintainer 2026-08-21: gekozen in het werkmenu (veegbundel).

Goal: elke bestaande grove {kind: inconsistent}-verwachting krijgt de
rule- (en waar zinvol label-)pin die het kanaalpin-werk mogelijk
maakte, zodat een verkeerde-regel-fout niet langer vals-groen kan
zijn. Routinewerk, geen ontwerp; per case de juiste vormnaam uit het
document.

Acceptance: geen grove inconsistent-verwachting meer zonder pin (of
een beredeneerde uitzondering in het Result); alle acht gates groen;
twee lenzen (steekproefsgewijs op de pin-juistheid).

May touch: `corpus/golden/`, `corpus/golden/anchor/` (verwachting-
verscherping is additief beleid), `backlog/`.

Out of scope: schema- of checkerwijzigingen.

Result: zero coarse inconsistent expectations remain. All seventeen
sharpened: the pins were first determined empirically (each candidate
loaded green through BOTH executors — the rule-pin load validation's
candidate list did the shortlisting), then the semantic lens tightened
what empiricism alone missed: the five two-subject uniqueness
collisions (kustregister case2/3/4, strandregister case1, vondstnummer
duplicaatnummer) now pin PER SUBJECT — both collision participants
flag, the nummerregister/case1 precedent, so a one-sided-uniqueness
regression goes red where a rule-only pin stayed green;
penninguitgifte case2 pins BOTH consistencies that fire on the bare
Penning (penningcontrole AND houderscontrole — the latter had zero
pinned violation coverage anywhere); and afrondingswaaier case4 pins
all six rounding forms' channel deposits on V1. Everything else got
its rule pin plus the label wherever exactly one case-labelled subject
flags; labels stay off where the flagging instance is minted (::,
wachtersketen case1) — a created instance never matches a label pin by
design. Patches are textual/style-preserving; the diff lens confirmed
minimality, JSON validity, schema conformance, and demonstrated the
false-green closure red-first (a wrong-rule pin fails enumerating in
both executors). All eight gates green (111 cases).
