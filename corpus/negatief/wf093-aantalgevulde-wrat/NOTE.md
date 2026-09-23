# wf093-aantalgevulde-wrat

Violates WF-093 (`spec/IR-SPEC.md` ch. 4.3): the row-1 wart is a COUNT
source, and an `AANTAL_GEVULDE` is not a count in the sense that rule means.

    WF-093 FAIL /rules/resolvedRegels/0/waarde/argumenten/0: the v0 instance-fold
    source (the row-1 wart, spelled with the unknown sentinel on the
    argument's own type) is a COUNT source: a fold source whose own type is
    the unknown sentinel yields the linked instances, which only AANTAL
    reads — so it cannot be the source of this AANTAL_GEVULDE aggregation

**A CONTROLLED PREDICTION, and this document is what makes it checkable.**
The design said the new kind would fall under WF-093 exactly as the six
existing value folds do, *without a letter of WF-093 changing*, because the
rule keys on the sentinel and asks one question: does this kind read a value
per contributor? The filled count does. The prediction was written down
before the arm was built and it holds: WF-093's text, guard and message are
untouched, and the kind's arrival in `value_fold_name` is the whole of it.

What the wart hands the fold is the subject's linked INSTANCES. A filled
count has nothing to ask them — presence is a property of a VALUE, and this
source names no attribute to read one from. `ref` says the same thing at
evaluation (`value aggregation over a bare role source`), where it stays as
the second line of defence, unreachable through a well-formed document.

The document. The well-formed shape with its source replaced by the bare
wart: `attribuut` naming the role plural `vondsten`, `objecttype` the fold
subject, `set: false`, and the unknown sentinel on the argument's own
`type`. Everything else holds. The verifier reports one fault — WF-037's
set-source arm deliberately steps aside on the sentinel so that one
malformation carries one rule.

Rood-eerst, measured on this document, and the before/after is itself the
evidence. With the wire enum landed and the arms absent (verifier md5
`220741bf2cee253d71ca6b13b8ad1402`): **WF-034**, the sentinel fence, because
the kind was not yet in the folding family the fence exempts. With the arms
(md5 `d970a571d0ecbc88de4bea5b14da9fc7`): **WF-093**, as predicted. The
diagnosis moved from "the sentinel does not belong at this position" to "the
sentinel belongs here and hands you the wrong thing", which is the true
statement of the two.

Falsifiable, arm-exact, twice. (1) Take the `Aantal_gevulde` row out of
`value_fold_name` in `spec/verifier/lib/checks.ml`: swept over golden +
rood + negatief + parity (695 documents) exactly four documents move, this
one back to WF-034. (2) Remove WF-037's sentinel exit — the
`Some _, [ Attribuut_ref { typ = Some (Datatype_onbekend _); _ } ] -> []`
case: exactly this document moves, and it moves by GAINING a second
citation for the same defect. Nothing else in the 695 changes a line.
