Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6 (m)): a **fact creation** (ch. 3.4)
whose `zetVeld` names the right role in the **wrong spelling** — `seinVak` for
the declared `seinvak`.

    WF-034 FAIL /rules/feitCreaties/0/zetVeld: 'seinVak' is not the declared
    spelling of the role 'seinvak' of 'vakbeseining': the created relation's
    bean field is a bean PROPERTY name, which is case-sensitive

**Why case-insensitive is not enough here, and only here.** Names in the IR
are compared under the ch. 2.4 identity — normalized, case-insensitive — and
every other reference in this clause family is resolved that way, including
`tussenstappen[].type`, whose object-type lookup is `equalsIgnoreCase` by the
documented contract of the backend's own `objecttypeOpNaam`. The three role
*name* fields are the exception, and the reason is not a naming rule but what
the name becomes: a bean-model executor turns the role name into a **Java
property name** and reaches it by getter, and that lookup is case-sensitive.
The declared spelling is therefore the only spelling that resolves, and the
arm is a byte-exact membership test sitting *below* the two case-insensitive
arms — the undeclared-name arm never sees this name, the to-one arm never
sees it either (it compares under the ch. 2.4 identity, and `seinVak` *is*
`seinvak` there), so the byte test is the only thing between this document and
an executor crash.

The asymmetry with `tussenstappen[].type` is deliberate and stated: a byte-exact
arm there would be stricter than all three executors and would have no witness,
because that field's consumer really does fold case.

**Why the witness changes an INTERNAL letter.** The bean model's identifier
mapping lower-cases the initial (`Namen.ident(rol, false)`), so `Seinvak`
reaches the very same property `seinvak` and the document plays — measured,
`PLAYED 1`, no crash. A first-letter difference is *harmless* to the executor,
and a witness built on one would have made the crash claim below false. This
witness therefore changes an **internal** capital, the shape that really does
break the getter lookup.

**Why it matters, measured.** `zetVeld` respelled from `seinvak` to `seinVak`
on `corpus/golden/jutterswet-postwacht` (baseline: it plays):

    verifier   PASS   (before this clause)
    ref        oracle: PASS (10 expectations)
    clingo     clingo: PASS (10 expectations)
    bean model ERROR ConsequenceException: Exception executing consequence
               for rule "vakbeseining door het kustvak" in defaultpkg:
               java.lang.IllegalStateException: geen getter voor 'seinVak'
               op io.github.yhavinga.regelspraak.facts.Sein

and, for contrast, the same field spelled `Seinvak`:

    verifier   PASS   (before AND after this clause: it is byte-inexact, so
                       the arm rejects it — the arm is a spelling rule, not a
                       crash predictor, and it is stricter than the executor
                       on exactly this one shape)
    bean model PLAYED 1

That last row is the honest limit of the clause, recorded rather than hidden:
it rejects one spelling the bean model would have survived, in exchange for
rejecting every spelling it would not. The declared spelling is available to
every producer at zero cost, so the trade is cheap.

*Measured cost, four zones:* **zero**. All 36 bean name positions in the 500
documents that predate this fix round spell their role byte-exactly as
declared; the arm moves nothing that was standing.

A hand-broken variant of `corpus/golden/jutterswet-postwacht/ir.json` — one
character, a one-line diff; schema-valid, since `ResolvedFeitCreatie.zetVeld`
is `["string","null"]` on the v0 wire.

Falsifiable, arm-exact: drop the byte-exact membership branch from
`wf034_feitcreatie_beanvelden` (`spec/verifier/lib/checks.ml`) and this case,
alone in all four zones, turns clean — its two sibling arms both resolve
`seinVak` to the declared `seinvak` and say nothing.
