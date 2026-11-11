# jutterswet-meldplicht-flogic — the meldplicht, authored in F-logic

The F-logic twin of `corpus/golden/jutterswet-meldplicht`, which explains the
set of three and why this rule. `case1-drempel.json` is byte-identical to the
RegelSpraak twin's.

The inheritable signatures declare the attribute and the characteristic; the
Horn rule `?V : meldplichtig :- ?V : Strandvondst, ?V.vondstwaarde > 100.`
assigns the characteristic where its body holds. The frontend lowers a
characteristic assignment to a plain `Rule` (`frontends/flogic/README.md`); a
conditional rule for an attribute value would become an exception in a
defaults group instead.

## The diff against the RegelSpraak twin

Counted on this document's side of `diff`, in the categories of the
RegelSpraak twin's note:

1. **Source provenance:** `?V : meldplichtig`, `?V.vondstwaarde` and
   `?V.vondstwaarde > 100` against the RegelSpraak phrases; the literal `true`
   carries no `source` here, because the rule head writes no literal (3 lines on
   the twin's side).
2. **Declared surface names:** no `plural`.
3. **The rule name:** none; the id `@!{'meldplicht van een strandvondst'}` is
   the RegelSpraak head.
4. **An empty list:** `prioritizedDefaults: []`.
