# Golden corpus

One directory per case: `bron.rgs` (the RegelSpraak source, authored
against the Jutterswet — `corpus/jutterswet/`) and `ir.json` (its
`regelspraak-ir-v0` dump). Admission: gate 2 (schema-valid, byte-identical
round-trip) and gate 3 (verifier-clean) — see `corpus/README.md` for the
rights policy and `spec/IR-SPEC.md` for the WF index the cases witness.

| Case | Jutterswet basis | Exercises |
|---|---|---|
| `jutterswet-penningdrempel` | art. 3 jo. 7 (de vondstdrempel) | characteristic attribution, parameter reference, comparison |
| `jutterswet-vondsttelling` | art. 3a–3b (aanmelding en telling) | fact type + roles, count over a role, sum over a navigated attribute, kg units |
| `jutterswet-berging` | art. 9 (gezamenlijke berging) | allocation: proportional by weight, per-recipient maximum, floor rounding, undistributed rest; unit systems (€, kg) |
| `jutterswet-vondstnummer` | art. 3a (vondstnummer en aanmelding) | uniqueness over a key; consistency with a date-ordering comparison |
| `jutterswet-penningjaar` | art. 3b jo. 7 (het penningjaar; jaarlijkse vaststelling) | named timeline + facet adoption, timeline attribute, yearly parameters |

Coverage notes:

- `jutterswet-vondsttelling` exercises the count/sum *shape* of
  art. 3a–3b only: the penningjaar scoping of the telling and the
  stormvloeddag double count are not rendered (they need the timeline
  and day-kind machinery a later case adds), and the gewicht-som rule
  exists to exercise SOM, units, and the empty-source fallback — art. 3a
  records the gewicht but mandates no sum.
- `jutterswet-berging` parks the undistributed rest on the Berging
  itself, where art. 9 sends it to the eilandfonds: the v0 remainder
  target is an attribute of the allocation *source*, and no eilandfonds
  object exists in the case — a deliberate, recorded modelling
  approximation, not a faithful render of the destination.
- `jutterswet-vondstnummer` renders art. 3a's timing discipline as "de
  aanmelddatum is niet eerder dan de vondstdag"; the article's exact
  clause — aanmelding in the *penningjaar* of the vondstdag — needs a
  function-valued comparison (year-of) that the v0 consistency form
  cannot express. Recorded as a v0 limit (`spec/CONSTRUCTS.md` row 19),
  not silently approximated away.
- The v0 count-over-a-role encoding (a pseudo attribute reference naming
  the role, sentinel-typed) is deliberate v0 shape; the WF-034 index
  cell records the verifier exception, and `jutterswet-vondsttelling`
  is its witness.

## Case files

A golden may carry one or more `case*.json` files — the evaluation data
half of the case (inputs, parameter values, evaluation date, expected
outcomes) in the `uchar-case-v0` format (`spec/schema/`, gate 4). The
expectation values trace to the worked examples of
`spec/RUNTIME-SEMANTICS.md`; the reference interpreter replays them as
its test suite (the oracle guard) once `ref/` exists.
