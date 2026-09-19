# jutterswet-havenrooster — a PARAMETER inside a day-kind voorwaarde

WF-031 admits one (`IR-SPEC.md` ch. 3.7: "dates, date-components,
eerste-paasdag, literals, parameters and other day-kind names"), and until
2026-09-19 **no document in any zone carried one** — measured with the
day-kind card: zero `ParameterRef` nodes under any `resolvedDagsoorten`. A
clause that no witness exercises is the spec-level green subset, and this
golden is what removes it.

## What it declares

```
Een dag is een havendag
indien er aan alle volgende voorwaarden wordt voldaan:
- de dag later is dan de openingsdatum        <- the parameter
- de dag uit de dag gelijk is aan 1.
```

So the day kind is **the 1st of each month, but only after the harbour
opens** — a definition whose extension is not a property of the document
alone. That is the point: a day kind is stratum 0 and pure, yet pure does not
mean constant. Model data may shape it; only derived state and session values
may not (WF-031's purity clause and, since this card, its position arm).

## The pins, hand-computed

Two cases over the same instance data, differing **only** in the parameter,
so every pinned value is evidence that the parameter is read:

| case | openingsdatum | matching days | H-aanvraag | +2 havendagen | tijdsduur tot 2026-05-01 |
|---|---|---|---|---|---|
| case1 | 2026-02-20 | 1 Mar, 1 Apr, 1 May, … | 2026-02-10 | **2026-04-01** | **3** |
| case2 | 2026-03-05 | 1 Apr, 1 May, … | 2026-02-10 | **2026-05-01** | **2** |

Move the opening one fortnight and the streefdatum moves a whole month and
the span loses a day. `case1` also carries **H2**, whose aanvraagdatum *is* a
havendag (2026-03-01): `+ 1` gives 2026-04-01, not 2026-03-01 — the "A itself
does not count" rule, restated at a parameter-bound day kind — and its
`tijdsduur` over an empty interval reads **0**.

## Who carries it, and who says why not

`ref` and the ASP backend replay both cases exactly. Both Drools branches
**refuse**, and are listed by name in gate 12's two channels with the reason:
the shared day-kind predicate compiler (`transpiler/kern/Dagsoorten`) has no
parameter operand at all. It compiles a voorwaarde ONCE into the KieBase,
while parameter values arrive per case as session globals, so a
parameter-bound day kind has no compiled predicate and the reaching rule is
skipped. No executor answers this golden *differently*; two answer it and two
say, per form, that they do not.

The unbound case is red: `corpus/rood/dagsoortparameter-ongebonden`.
