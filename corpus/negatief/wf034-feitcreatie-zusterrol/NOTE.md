Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6 (m)): a **fact creation** (ch. 3.4)
whose `zetVeld` names the relation's **sister** role — a declared role, so the
fifth wave's arm admits it, but not the role the v0 bean model stores the
reference under.

    WF-034 FAIL /feitCreaties/0/zetVeld: 'vaksein' is a to-many role of
    'vakbeseining': the created relation's bean field names the side the v0
    bean model stores the reference under, so it must name 'seinvak'

**Why "a declared role" was not enough.** The fifth wave crossed the four bean
fields against the declared roles of the relation their symbolic sister names,
and admitted **both** roles with the reason "which of the two the bean model
stores is a bean-model decision the wire does not carry". That reason is
wrong, and the backend in this repository is the refutation: it *derives* the
side, symbolically, from cardinality alone. A binary relation lives in the v0
bean model as ONE scalar reference field, on the object type that fills the
**to-many** role, named after the **to-one** role (§13.3.9 — the derivation
`FactModelGenerator.leden` renders and the transpiler's own skip reasons cite
as "§13.3.9 opslagrichting"). `vakbeseining` relates one `Kustvak` (role
`seinvak`, to-one) to many `Sein` (role `vaksein`, to-many), so the field is
`Sein.seinvak` and nothing else: `vaksein` is the name of a field that does
not exist on either bean.

So the law is sharper than the wave stated it:

| the relation's cardinality | admissible spellings |
| --- | --- |
| exactly one role to-one | that role, and only that role |
| both roles to-many | either name — the model derives no side |
| both roles to-one (1:1) | either name — the model derives both fields |

The middle row is not a hedge; it is the corpus. `anchor/jutterswet-erflijn`
creates `erflijn`, whose two roles (`erflater`, `erfvolger`) are **both
to-many**, and spells `zetVeld` with `erfvolger`; `anchor/jutterswet-gemeente-
berging` joins over `aanmelding`, both roles to-many, and spells `joinVeld`
with `aangemelde vondst`. A rule that demanded one side there would make two
anchor goldens ill-formed for a choice the model does not make. This clause
therefore stops exactly where the derivation stops — the tightening is scoped
to the row where the bean model has an answer.

*(The old sentence also mis-read the corpus. It said "three of the four golden
creations spell `zetVeld` with the anchor's role and one with the target's".
Counted per CREATION it is two and two: `anchor/jutterswet-erflijn` carries
**two** creations, both spelling the target's role, against `jutterswet-post-
wacht` and `anchor/jutterswet-gemeenteberging` spelling the anchor's. The
anchor/target framing predicts nothing anyway — `jutterswet-postwacht`'s
`joinVeld` is the *opposite* of its bound role — while the to-one reading
explains all eleven positions at once.)*

**Why it matters, measured.** `zetVeld` repointed from `seinvak` to `vaksein`
on `corpus/golden/jutterswet-postwacht` (baseline: it plays):

    verifier   PASS   (before this clause)
    ref        oracle: PASS (10 expectations)
    clingo     clingo: PASS (10 expectations)
    bean model ERROR ConsequenceException: Exception executing consequence
               for rule "vakbeseining door het kustvak" in defaultpkg:
               java.lang.IllegalStateException: geen getter voor 'vaksein'
               op io.github.yhavinga.regelspraak.facts.Sein

The same asymmetry the four sibling NOTEs pin, one step further in: the name
is not nonsense, it is a *declared role of the created relation*, and it still
crashes the one executor that reads the field. Two executors are blind here by
construction — they read the symbolic form and never touch the bean fields —
so nothing downstream marks the document.

*Measured cost, four zones:* **zero**. Over all 36 bean name positions in the
500 documents that predate this fix round (11 golden, 3 rood, 19 negatief, 3
parity), 21 sit on a relation with exactly one to-one role and every one of
them names it; 8 sit on equal cardinality, where the arm stands down; 4 sit on
a fact type WF-028 already reports missing; and 3 are the fifth wave's own
undeclared-name witnesses. Not one standing document moves.

A hand-broken variant of `corpus/golden/jutterswet-postwacht/ir.json` — one
field repointed, a one-line diff; schema-valid, since
`ResolvedFeitCreatie.zetVeld` is `["string","null"]` on the v0 wire. The base
golden was chosen for the same reason as its siblings: it is the one
fact-creation golden the bean-model executor actually plays, so the crash
above is a fact about a corpus document.

Falsifiable, arm-exact: drop the to-one arm (the `[ one ]` branch) from
`wf034_feitcreatie_beanvelden` (`spec/verifier/lib/checks.ml`) and this case,
alone in all four zones, turns clean — the undeclared-name arm above it does
not catch `vaksein`, and the spelling arm below it does not either, because
`vaksein` is byte-exactly a declared role.
