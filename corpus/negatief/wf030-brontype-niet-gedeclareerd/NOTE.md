Violates WF-030 (`spec/IR-SPEC.md` ch. 3.6): a uniqueness source that names
no declared object type — `objectType` `Vondsten`, the declared plural of
`Vondst`.

    WF-030 FAIL /rules/uniciteiten/0/versions/0/sources/0/objectType: source
    type 'Vondsten' is not declared

**Why the plural does not name the type.** A source names its object type by
the declared name; the verifier looks it up among the declared object-type
names, case-insensitively and with whitespace collapsed, and a declared
plural is not among them. The source sentence of the golden, "De vondstnummers
van alle Vondsten moeten uniek zijn.", uses the plural; the wire writes the
name.

**The executors part without the rule.** Measured 2026-09-24 against the three
cases of the source golden: the reference interpreter passes the two cases
without a duplicate and misses the duplicate of the third (`MISMATCH: expected
inconsistent (a channel fact from rule "eenmalig vondstnummer" on "V1"), but
the channel carries: (empty)`); the ASP backend refuses every case
(`TRANSLATION FAULT: unknown object type "Vondsten"`); the Drools backend's
exec-model branch excludes the uniqueness at transpilation (`transpiler
SKIP`) while its DRL-text branch crashes (`NullPointerException …
ResolvedUniciteit.subject() is null`) — the Drools loader carries no WF-030
arm, an open point of the L4 landing.

**The document.** `corpus/golden/jutterswet-vondstnummer/ir.json` with one
value changed: the source's `objectType` `Vondst` → `Vondsten`.

Falsifiable, arm-exact: emptying the arm `None` of the object-type lookup in
`wf030` (`spec/verifier/lib/checks.ml`) turns this case clean and nothing else
— 703 documents compared over golden, rood and negatief, one verdict moved.
