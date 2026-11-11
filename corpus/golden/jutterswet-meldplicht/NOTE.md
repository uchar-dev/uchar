# jutterswet-meldplicht — one rule, authored in RegelSpraak

The smallest rule the three source languages carry the same way: a
Strandvondst is meldplichtig when its vondstwaarde is greater than 100. It is
one of three source twins with byte-identical case files:

- `jutterswet-meldplicht` — RegelSpraak (`source.rgs`), this golden;
- `jutterswet-meldplicht-catala` — Catala (`source.catala_en`);
- `jutterswet-meldplicht-flogic` — F-logic (`source.flogic`).

Each `ir.json` is what its frontend emits (gates 10, 9 and 14 regenerate it).
The three documents carry the same `Rule`: target the characteristic
`meldplichtig`, value `true`, condition `vondstwaarde > 100` as one
`Comparison`. The website's languages section shows the three sources side by
side; this set keeps that example running.

The rule is chosen for what it leaves out. A product such as a quarter of a
value with two decimals has four, more than a two-decimal target admits
(RUNTIME-SEMANTICS ch. 2.3): RegelSpraak then needs an explicit rounding,
Catala's `money` rounds by itself, and F-logic has no rounding, so the three
would not reach the same IR. A default with an exception reaches a
`PrioritizedDefaults` in all three, but needs two definitions; inheritance has
no Catala source (the Catala frontend models no type hierarchy).

## The case

`case1-drempel.json`: V1 with vondstwaarde 150 is meldplichtig, V2 with
vondstwaarde 100 is not — the comparison is strict, so the threshold value
itself stays below it. Expected values by hand; the same file stands in all
three goldens.

## The differences between the three documents

Against this document, the twins differ in four categories and no fifth;
`NOTE.md` of each twin lists its lines.

1. **Source provenance (`source`).** Each document quotes its own source.
   No executor reads the field.
2. **Declared surface names a language has no grammar for.** Neither Catala nor
   F-logic declares a plural, so `plural` is absent in both. Catala has no
   non-negative integer, so the attribute's `sign` is `unspecified` there, and
   the Catala frontend declares every object type `animate`.
3. **The rule name.** The Catala frontend names a characteristic definition
   `kenmerktoekenning <name>`; F-logic takes the id `@!{'…'}`, which is written
   here as the RegelSpraak head.
4. **An empty list.** The Catala and F-logic frontends write an empty
   `prioritizedDefaults`; the RegelSpraak frontend leaves it out.

No semantic difference: the object type, its attribute and characteristic, and
the rule's target, value and condition are identical in all three.
