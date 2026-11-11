# jutterswet-meldplicht-catala — the meldplicht, authored in Catala

The Catala twin of `corpus/golden/jutterswet-meldplicht`, which explains the
set of three and why this rule. `case1-drempel.json` is byte-identical to the
RegelSpraak twin's.

The scope `Strandvondst` becomes the object type, the input `vondstwaarde` an
attribute the case fills, and the boolean output `meldplichtig` a
characteristic; its single definition becomes a `Rule` whose value is `true`
and whose condition is the definition's expression
(`frontends/catala/lib/translate.ml`, "single boolean definition").

## The diff against the RegelSpraak twin

Counted on this document's side of `diff`, in the categories of the
RegelSpraak twin's note:

1. **Source provenance:** the target's snippet `de meldplichtig` and the read's
   `de vondstwaarde` against `Een Strandvondst is meldplichtig` and
   `zijn vondstwaarde`; the comparison carries no `source` here (3 lines on the
   twin's side).
2. **Declared surface names:** no `plural`; `animate` is `true`; the
   attribute's `sign` is `unspecified`, because Catala's `integer` has no sign.
3. **The rule name:** `kenmerktoekenning meldplichtig`.
4. **An empty list:** `prioritizedDefaults: []`.
