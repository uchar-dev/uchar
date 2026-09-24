Violates WF-030 (`spec/IR-SPEC.md` ch. 3.6): a uniqueness key that is no
declared attribute of its source's object type — `vondstnummers`, the declared
plural of the attribute `vondstnummer` of `Vondst`.

    WF-030 FAIL /rules/uniciteiten/0/versions/0/sources/0/keys/0: key
    'vondstnummers' is not a declared attribute of 'Vondst'

**Why the key must resolve.** A key names the attribute whose values the
uniqueness compares (ch. 3.6, `Source.keys`). The verifier looks it up among
the declared attribute names of the source's object type, case-insensitively
and with whitespace collapsed; the attribute's declared plural, the spelling
of the source sentence ("De vondstnummers van alle Vondsten moeten uniek
zijn."), is not among them.

**Two executors answer wrongly without the rule, the third fails.** Measured
2026-09-24 against the three cases of the source golden: the reference
interpreter and the ASP backend pass the two cases without a duplicate and
both miss the duplicate of the third (`MISMATCH: expected inconsistent …`),
without a fault; the Drools backend's exec-model branch fails at evaluation
(`ERROR ConstraintEvaluationException`) and its DRL-text branch fails at DRL
compilation — the Drools loader carries no WF-030 arm, an open point of the
L4 landing.

**The document.** `corpus/golden/jutterswet-vondstnummer/ir.json` with one
value changed: the source's key `vondstnummer` → `vondstnummers`.

Falsifiable, arm-exact: emptying the arm `Some k when not (has_attr ot k)` of
the key walk in `wf030` (`spec/verifier/lib/checks.ml`) turns this case clean
and nothing else — 703 documents compared over golden, rood and negatief, one
verdict moved.
