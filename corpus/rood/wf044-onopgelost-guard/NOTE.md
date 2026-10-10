# wf044-onopgelost-guard

The unsolved-state guard (WF-044) on the **condition** position. Its
sibling `wf044-onopgelost-reached` pins the same marker as a computed
*value*; this one pins it as a *guard* — the voorwaarde of a §13.4.7
feitcreatie, where an `Unresolved` is the shape a resolver hands the wire
when it cannot rebuild the condition against the creation's target: a
creation whose doel type equals its anker type has no second subject to
resolve a doel-hosted guard against, so the anker's unresolved condition
is passed through unchanged.

The document is `jutterswet-postwacht`'s golden with **exactly one field
changed** — the creation version's `condition`, absent in the golden, set to
the marker — and
the case carries the same population that golden replays. Everything a
green run needs is therefore present and correct; the only reason this
case is red is the marker itself. Verifier-clean, as an honest marker
must be: WF-044 is behavioural, not a well-formedness rule.

Both executors that read every red form fault where they read the
creation, and each names the position:

- reference interpreter — `Unresolved condition reached in evaluation`;
- ASP backend — `Unresolved condition reached in translation: this rule
  fails observably`.

The Drools red run drives the exec-model branch, which derives with truth
maintenance since the card `tms-omschakeling` (2026-10-10). It carries the
form and faults where the evaluation reaches the marker, in the reference
interpreter's words: `fout in de regel "vakbeseining door het kustvak":
Unresolved condition reached in evaluation`, the fragment pinned for it.
Until that card the red run drove the destructive lowering; both of its
transpiler branches refused the creation by construct, because a guard
that lowers to neither the anker's nor the doel's pattern cannot be
dropped without firing the rule unconditionally and over-creating the
relation. That refusal was a not-carried outcome, listed with its reason
in gate 8's Drools leg.

A harness in which this case stops failing has started guessing where the
spec says "cannot express" — and, worse than in the value position, it
has started guessing about *which objects a relation holds between*.

**WF-113 (since M1, 2026-09-23).** The document carries one `warning` in
`diagnostics` whose message contains the marker's source snippet verbatim, in
the frontend's message form (`onopgelost: <reason> — '<bron>' blijft als marker
op de wire staan`): every marker on the wire is accounted for by a diagnostic,
and this document is one of the first corpus carriers of that clause (WF-113
(b)). `line` is 0 because no source file carries the marker's phrase; 0 is the
value the RegelSpraak frontend writes for a marker without a source position.
The case and its `expectedFault` are unchanged: the diagnostic carries no
semantics (WF-113 (c)).
