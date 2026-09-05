Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6): a **fact creation** (ch. 3.4)
whose `zetVeld` — the bean field the creation WRITES — names a role *no
declaration carries*.

    WF-034 FAIL /feitCreaties/0/zetVeld: 'bestaatniet' is not a declared role
    of 'vakbeseining': the created relation's bean field resolves against no
    declaration

**The clause, in full** (the three sibling NOTEs in this family point here).
A `ResolvedFeitCreatie` carries two halves. The symbolic half — `feittype`,
`ankerRol`, `doelRol`, `joinRol`, `tussenstappen[].rolBinding` — says which
relation is created and along which path; WF-028 crosses every one of them.
The v0 half — `zetVeld`, `joinVeld`, `tussenstappen[].veld` and
`tussenstappen[].type` — is what a bean-model executor navigates and writes,
and until this clause **not one of the four was held against a declaration**.

They are not free-form. A binary relation lives in the v0 bean model as ONE
reference field, on the side that stores it, named after a role of that
relation. Each bean field therefore belongs to a relation its symbolic sister
already names, and the law is that it spells a **declared role of that
relation**:

| field | relation | crossed against |
| --- | --- | --- |
| `zetVeld` | the created one | a role of `feittype` |
| `joinVeld` | the final hop | a role of `joinRol`'s fact type |
| `tussenstappen[j].veld` | hop *j* | a role of that `rolBinding`'s fact type |

Both roles are admitted and neither is assumed, because the corpus uses both:
three of the four golden creations spell `zetVeld` with the **anchor's** role
and one — the self-typed `anchor/jutterswet-erflijn`, both ends `Wachter`,
both to-many — with the **target's**; `joinVeld` is the bound role three times
and the opposite role once. Which of the two the bean model stores is a
bean-model decision the wire does not carry, so the clause asks no more than
that the name be one of them. Only the declared name binds — the plural is
presentation, exactly as WF-047 already has it for the bindings themselves.

The fourth field, `tussenstappen[j].type`, is not a role name but the
intermediate object type the hop lands on, so it crosses against the hop: the
filler of the role that binding binds. That is the sibling NOTE
`wf034-feitcreatie-onbekend-tussenstaptype`.

**Why it matters, measured.** One field at a time set to a name no declaration
carries, over all **14** golden positions (4 `zetVeld`, 4 `joinVeld`, 3
`type`, 3 `veld`, spread over `anchor/jutterswet-erflijn`,
`anchor/jutterswet-gemeenteberging` and `jutterswet-postwacht`):

    verifier   14/14 PASS
    ref        14/14 clean — replays to the pinned expectations
    clingo     14/14 clean — the same

Both OCaml executors are **blind here by construction**: they read the
symbolic fields and never touch the bean fields, so a nonsense name changes no
answer and there is no divergence for the differential square to catch. The
bean-model executor reads them, reflectively, and on `jutterswet-postwacht`
(baseline: it plays the golden) each of the four throws it out of the engine.
For this document:

    ref       : oracle: PASS (10 expectations)
    clingo    : clingo: PASS (10 expectations)
    bean model: ERROR ConsequenceException: Exception executing consequence
                for rule "vakbeseining door het kustvak" in defaultpkg:
                java.lang.IllegalStateException: geen getter voor
                'bestaatniet' op …facts.Sein

That asymmetry is the whole point: two executors replay a verifier-clean
document to its pinned expectations without a murmur, so nothing downstream
marks it, while the third crashes on it. A verifier-clean document that
crashes an executor is exactly the class an engine-neutral IR must not admit —
the same shape `wf034-regeldoel-zonder-type` pins one node over.

*Measured breadth:* **zero** documents outside these four witnesses lose
anything. All **8** fact creations in golden, rood, negatief and the parity
zone spell every one of their bean fields against a declared role, and the
four-zone sweep (500 documents) moves no other verdict and no other report.

A hand-broken variant of `corpus/golden/jutterswet-postwacht/ir.json` — one
field repointed, a one-line diff; schema-valid, since
`ResolvedFeitCreatie.zetVeld` is `["string","null"]` on the v0 wire. The base
golden was chosen deliberately: it is the one of the three that the bean-model
executor actually plays, so the crash above is a fact about a corpus document
and not about a hypothetical one.

Falsifiable, arm-exact: replace the `spells_role … (at ^ "/zetVeld") …` line
of `wf034_feitcreatie_beanvelden` (`spec/verifier/lib/checks.ml`) by `[]` and
this case, alone in all four zones, turns clean — 500 documents compared, one
verdict moved, no report elsewhere. Clause-exact instead: make the whole
clause inert and exactly its four witnesses turn clean, and nothing else.
