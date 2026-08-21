# Gedeeld_door_abs en Tot_de_macht in clingo

Besluit maintainer 2026-08-21: gekozen in het werkmenu (veegbundel).

Goal: de twee resterende rekenvormen die vandaag luid faulten in de
vertaler landen in de ASP-encodering (schaalbeleid conform ch. 6b, de
wrap- en exactheidshekken van ch. 6c), differentieel gepind met
ankers naast refs bestaande semantiek (RS ch. 2.2/1.2: leeg-gedrag
van beide vormen).

Acceptance: rood-eerst (de fault vandaag), daarna ankers door gates
5–6 incl. de leeg-hoeken; alle acht gates groen; twee lenzen.

May touch: `backends/clingo/`, `spec/ASP-TRANSLATION.md`, `corpus/`,
`backlog/`.

Out of scope: wortel-achtige nieuwe vormen; ref-wijzigingen.

Result (2026-08-21): beide vormen landen in de vertaler. Rood-eerst
gedemonstreerd: het taxatie-IR faultte vóór de wijziging met
"expression kind not translated (no corpus case exercises it)" terwijl
ref alle cases al draaide. Gedeeld_door_abs is het bestaande
rounded-division-pad met (Richting_nul, 5) — ch. 2.2's gedefinieerde
truncatiepunt; gringo's / trunceert richting nul op alle vier
tekencombinaties (zelf geverifieerd én gepind in case2's volledige
tekentafel), dus geen negatief-bereik-hek. Tot_de_macht is gringo's **
op de geschaalde basis met compile-time-schaal sl^e (hek op 10^9) en
een operandhek op de e-de wortel van 2^31 (46340 bij e=2, 1290 bij
e=3; case5 pint de grootste vertaalbare machten exact). Anker
jutterswet-taxatie (5 cases: exact, tekentafel + (-3)^3, leeg-hoeken
incl. leeg-dividend→0 en leeg-basis→leeg, kanaalfeit onder gevulde
dividend, grensrand); rood-getuige division-abs-by-numeric-zero in
beide executors. ASP-TRANSLATION ch. 6b uitgebreid met beide vormen.
Alle acht gates groen; twee opus-lenzen (semantiek-fidelity + breker,
253k tokens, 90 tool-runs) beide CLEAN — nul bevindingen, een unicum.
Volgorde: deze kaart bewust vóór 20260821-asp-systematisch gedaan,
zodat het systematische hoofdstuk beide vormen dekkend kan beschrijven
in plaats van als luide-fault-vermelding. Bewust blijven liggen:
runtime-, negatieve en fractionele exponenten faulten luid (de
runtime-exponent-leeg-hoek 4^leeg = leeg is daarmee ref-only gedrag,
als zodanig genoteerd in ch. 6b); eenheid-dragende operanden blijven
geweigerd zoals in de hele multiplicatieve familie; het anker oefent
de abs-arm niet onder een regel-voorwaarde (het pad deelt de
rule_guard-machinerie met het door vindersloon-case4 gepinde
delingspad).
