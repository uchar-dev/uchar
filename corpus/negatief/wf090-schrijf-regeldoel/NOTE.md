Violates WF-090 (`spec/IR-SPEC.md` ch. 2.4) at the first write position —
a rule target. The document is the `jutterswet-vondstdossier` model with a
second, internally consistent struct domain `logboek` (`bedrag`, `aantal`)
beside `dossier` (`waarde`, `telling`). The `BEREKENING` on
`Jutter.samenvatting`, declared `dossier`, writes a `StructLiteral` of
`logboek`; a second rule reads `samenvatting.waarde` through a faithfully
typed `AttribuutRef`, so WF-073 and WF-074 have nothing to say — each
crosses a use site against the domain it names, never against the
declaration the value enters.

    WF-090 FAIL /rules/resolvedRegels/2/versions/0: a value of the struct domain
    'logboek' is assigned to a target of the struct domain 'dossier'

Why it has to exist: this is the flank the use-site-declaration-identity
card measured and left open — WF-023's domain clause is scoped to the
keuzetype family (Enumeratie and SumType), so nothing typed a struct value
against its target. Verifier-clean before; ref faulted
(`field "waarde" is not present on the record value`) while clingo left
`J1.waardekopie` underived where 125.50 was expected — a verifier-clean
ref ≢ clingo divergence of exactly the class WF-089/WF-023 closed for
keuzetypes. The faithful twin (the same document with a `dossier`
literal) replays `oracle: PASS` and `clingo: PASS`.

Falsifiable, arm-exact: suppress WF-090 at write position 1 only (filter
`x.wf <> "WF-090"` off the `wf023_assignment` result in the rule-target
branch of `wf023`) and this case alone turns clean — the object-creation
and defaults cases and every read-side case stay red. Dropping the whole
struct clause from `wf023_assignment` clears all three write cases
together, which is what makes them one arm in three positions.
