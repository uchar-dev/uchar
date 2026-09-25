# jutterswet-aanspraakherziening — the ci witness of WF-068's result arm

A `Jutter` has a list of `aanspraken` of the declared SumType domain
`aanspraak` (`Erkend(bedrag)`, `Vervallen`), as in
`anchor/jutterswet-aanspraaktelling`. The function `herziening` reviews one
claim: an acknowledged claim counts at most 10.00, a lapsed one stays
lapsed. `herziene som` maps `herziening` over the claims, maps
`aanspraakwaarde` (`Erkend(b)` is b, `Vervallen` is 0.00) over the result,
and folds the values into a sum from 0.00.

The single point of the document: the `Invoke` of `herziening`, the body
of the inner `MapList`'s lambda, is typed with the domain spelled
**`AANSPRAAK`**, while the function declares its result as `aanspraak`.
The constructors are repeated exactly; only the case of the domain name
differs. IR-SPEC ch. 2.1 matches a name case-insensitively where a rule
compares two datatypes whole, and WF-068 compares the invoke's type with
the declared result, so the document is verifier-clean.

## Hand-computed absolutes (one Jutter J1)

| case | aanspraken | reviewed | J1.herziene som |
|---|---|---|---|
| case1-gemengd | Erkend 10.00, Vervallen, Erkend 2.50 | 10.00, 0.00, 2.50 | **12.50** |
| case2-boven-de-grens | Erkend 15.00, Erkend 7.25, Vervallen | 10.00, 7.25, 0.00 | **17.25** |
| case3-leeg-lijst | none (the empty list) | none | **0.00** (the fold's init) |

Each case is consistent. `ref` and the ASP backend replay the three cases
with these values (measured 2026-09-25), and a do-nothing run mismatches
every one. The Drools backend refuses the document at load in both
branches: `EXCLUDE loader: form not carried by the v0 Drools backend:
functionDefs (unknown model section)`.

## What it pins

Until 2026-09-25 the result arm of WF-068 compared the two datatypes byte
for byte, while its argument arm already compared them as ch. 2.1 does;
that verifier refuses this document, measured:

    WF-068 FAIL /rules/rules/0/versions/0/value/list/list/function/body/type:
    invoke result type differs from the declared function result

and it refused no other golden or rood document. Making the whole datatype
comparison byte for byte again (`same_datatype` as `a = b` in
`spec/verifier/lib/checks.ml`) turns this document red at two rules, and
of the other goldens only `jutterswet-meldtermijnen`:

    WF-070 FAIL /rules/rules/0/versions/0/value/list/list/function: lambda
    body is sumtype 'AANSPRAAK', but the fold requires sumtype 'aanspraak'
    WF-068 FAIL /rules/rules/0/versions/0/value/list/list/function/body/type:
    invoke result type differs from the declared function result

Deliberately carries no `bron.rgs` and no `bron.catala_en`: the drift is a
wire-level spelling that neither frontend emits. Hand-authored, in the
Jutterswet's own vocabulary.
