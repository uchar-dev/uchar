# wf091-setbron-som-lijsttype

Violates WF-091 (`spec/IR-SPEC.md` ch. 4.1, the numeric-operand arm): a
`sum` over a set source whose element attribute is declared a `ListType` of
numbers. The fold adds one value per member, and a list is no amount: the
arm does not look through the list to its element type.

    WF-091 FAIL /rules/rules/0/versions/0/value/arguments/0: the sum of
    'weegreeks' on 'Vondst' folds ListType values: a total, sum or pro-rata
    part is arithmetic over a Number, and ListType values are not amounts

**The document.** `wf091-setbron-som-datum` with the member and the target
retyped to a `ListType` whose element is a `Number` with 2 decimals, and
renamed (`vondstdatum` to `weegreeks`, `vondstdagensom` to `weegreekssom`,
with the member's plural, the rule name and the source snippets following
the new names); nothing else differs. It is the arm's `ListType` witness: no document in the
four zones folds a list-typed operand under `sum`, `total` or the pro-rata
part.

Measured on this document (2026-09-25), with two members whose `weegreeks`
are `[1.50, 2.25]` and `[4.00]`: `ref` faults on the addition (`plus:
expected a number, got [1.5, 2.25]`), the ASP translation refuses
(`unscalable datatype in the ASP translation`), and both Drools branches
exclude the document at the loader (`Datatype:ListType (unknown datatype
node)`). No executor answered.

Red first: the verifier of main 2f47d80e (md5
`e0bafac419b97e989317b13194932efe`) reported `verifier: PASS`; with the arm
(md5 `7816a181b61c7e337e9f2b045877f7b6`) the single rejection above.
Mutation, arm-exact: with the arm's type test exempting a `ListType` (`when
not (is_number_like d) && (match d with List_type _ -> false | _ -> true)`),
this document passes again and gate 3 fails on this directory alone;
`expect.txt` pins the `ListType` tail.
