# jutterswet-grens-per-jutter — a Let binder inside a fold, per subject

Each rule binds a value of the Jutter in a `Let` and reads the binder inside a count over the Jutter's vondsten. A
`Let` binds per evaluation of its subject (RUNTIME-SEMANTICS ch. 10.6), so each Jutter's count reads its own bound
value, also where the binder stands inside a fold over the brokstukken of a vondst. The case has two Jutters with a
different bound value, so a value that leaks from one Jutter to the other changes a count.

| rule | the binder stands | value |
|---|---|---|
| zware vondsten | in the test of a quantifier fold (all brokstukken: gewicht > grens), grens = drempel | count |
| lichte vondsten | in the filter of a quantifier fold (none of the brokstukken with gewicht > grens has gewicht > 0) | count |
| vondsten boven de ondergrens | a nested Let: ondergrens = grens min 1, in the test of the quantifier fold | count |
| late vondsten | in the filter of the count itself (nummer > grens) | count |
| lange vondsten | in the test of a quantifier fold, grens = grenslengte in km against a lengte in m | count |
| passende vondsten | as above, under "ten minste één" with an equality | count |

## The hand calculation

J1: drempel 5, grenslengte 1 km. J2: drempel 2, grenslengte 2 km. The brokstukken carry (gewicht, lengte in m).

| vondst | jutter | nummer | brokstukken, in the order of the case |
|---|---|---|---|
| V1 | J1 | 3 | (6, 1500), (7, 2000) |
| V2 | J1 | 6 | (8, 3000), (3, 1000) |
| V3 | J1 | 7 | (5, 800) |
| V4 | J2 | 3 | (3, 2500) |
| V5 | J2 | 4 | (3, 2500), (1, 2000) |

| attribute | J1 | J2 |
|---|---|---|
| aantal zware vondsten (all gewicht > drempel) | V1: **1** | V4: **1** |
| aantal lichte vondsten (no gewicht > drempel) | V3: **1** | none: **0** |
| aantal vondsten boven de ondergrens (all gewicht > drempel − 1) | V1, V3: **2** | V4: **1** |
| aantal late vondsten (nummer > drempel) | V2, V3: **2** | V4, V5: **2** |
| aantal lange vondsten (all lengte > grenslengte) | V1: **1** | V4: **1** |
| aantal passende vondsten (some lengte = grenslengte) | V2: **1** | V5: **1** |

With J2's drempel 2 for J1, as the leak read it, J1 would count 3 heavy, 0 light and 3 above the lower bound. With
the grenslengte read as a bare number, J1 would count 3 long and 0 fitting vondsten, J2 2 and 0.

In every quantifier fold whose test reads a binder, a member that decides the fold is the last one of its vondst, so
the fold stops at no member before the last: the reference interpreter's reading (RUNTIME-SEMANTICS ch. 5.12, gate
17) evaluates a member the evaluation did not reach, and refuses by name to force a binder there.

## Provenance

Own work (cards `clingo-let-binder-tussen-onderwerpen` and `clingo-let-binder-vergelijking-over-eenheden`,
2026-11-06). The frontend substitutes a variable read inside an aggregation (IR-SPEC ch. 4.10, boundary (c)), so the
IR is the frontend's document of the substituted rules with the `Let` and the `BinderRef` written by hand, and the
two forms it cannot write (the binder in the filter of the quantifier fold, the nested `Let`) added by hand. Measured
at admission: verifier PASS; ref plays the case. The ASP backend leaked the binder between subjects up to
`235758907` (J1: 3, 0 and 3), and has read it per subject since `a274d7554` (card `subselection-reads-subject`,
which keys a helper over a member by the form subject); it read the grenslengte as a bare number on main
`17f956d92` (J1 3 and 0, J2 2 and 0), and plays the case since card `clingo-let-binder-vergelijking-over-eenheden`.
Both branches of the Drools backend refuse the golden by name (DROOLS-193: a binder of a Let over a fold, in its
filter or matrix).
