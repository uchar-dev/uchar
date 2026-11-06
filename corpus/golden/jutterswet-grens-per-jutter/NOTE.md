# jutterswet-grens-per-jutter — a Let binder inside a fold, per subject

Each rule binds a value of the Jutter in a `Let` and reads the binder inside a count over the Jutter's vondsten. A
`Let` binds per evaluation of its subject (RUNTIME-SEMANTICS ch. 10.6), so each Jutter's count reads its own bound
value, also where the binder stands inside a fold over the brokstukken of a vondst. Both cases have two Jutters with a
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

A fold reads the brokstukken of a vondst in the order of their labels (the reference interpreter builds the links
the case writes on the brokstuk side per label), so the table gives them in that order.

| vondst | jutter | nummer | brokstukken in `case.json` | brokstukken in `case2-beslissend-lid-eerst.json` |
|---|---|---|---|---|
| V1 | J1 | 3 | S1 (6, 1500), S2 (7, 2000) | the same |
| V2 | J1 | 6 | S3 (8, 3000), S4 (3, 1000) | S3 (3, 1000), S4 (8, 3000) |
| V3 | J1 | 7 | S5 (5, 800) | the same |
| V4 | J2 | 3 | S6 (3, 2500) | the same |
| V5 | J2 | 4 | S7 (3, 2500), S8 (1, 2000) | S7 (1, 2000), S8 (3, 2500) |

The order changes no count, so both cases carry the same twelve expectations.

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

In `case.json` a member that decides a quantifier fold over V2 or V5 is the last one of its vondst; in
`case2-beslissend-lid-eerst.json` it is the first, so those folds stop before their last member, after the first
member read the binder. A quantifier fold in the filter of a count is read per member and has no reading of its own
(RUNTIME-SEMANTICS ch. 5.12, "Folds over a set"): the reading writes no line for it and evaluates no member after
its stop, so it forces no binder there.

## Provenance

Own work (cards `clingo-let-binder-tussen-onderwerpen` and `clingo-let-binder-vergelijking-over-eenheden`,
2026-11-06). The frontend substitutes a variable read inside an aggregation (IR-SPEC ch. 4.10, boundary (c)), so the
IR is the frontend's document of the substituted rules with the `Let` and the `BinderRef` written by hand, and the
two forms it cannot write (the binder in the filter of the quantifier fold, the nested `Let`) added by hand. Measured
at admission: verifier PASS; ref plays the case. The second case is the first with the gewicht and lengte of S3 and
S4, and of S7 and S8, exchanged (2026-11-06). The ASP backend leaked the binder between subjects up to
`235758907` (J1: 3, 0 and 3), and has read it per subject since `a274d7554` (card `subselection-reads-subject`,
which keys a helper over a member by the form subject); it read the grenslengte as a bare number on main
`17f956d92` (J1 3 and 0, J2 2 and 0), and plays the case since card `clingo-let-binder-vergelijking-over-eenheden`.
Both branches of the Drools backend refuse the golden by name (DROOLS-193: a binder of a Let over a fold, in its
filter or matrix).
