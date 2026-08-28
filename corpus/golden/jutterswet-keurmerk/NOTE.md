# jutterswet-keurmerk

The **quote-escaping** witness (ASP-TRANSLATION S2/S19). A `Vondst` carries a
text `keurmerk` whose value embeds a double-quote; the `gekeurd`
kenmerktoekenning compares it for equality against a text literal that embeds
the same double-quote, and the `afslag` gelijkstelling writes a second
quote-bearing text. So the encoding meets the escape on BOTH sides — the literal
side (`symbolic_literal_term`) and the value side (`symbolic_value_term`, both
the case-input `val/3` carriage and the computed-text write) — and the equality
join stays sound because the two sides escape identically (embedded `"` becomes
backslash-quote).

## Hand-computed absolutes

| Vondst | keurmerk        | gekeurd | afslag           |
|--------|-----------------|---------|------------------|
| V1     | `merk "AA"`     | present | `veiling "Zee"`  |
| V2     | `merk "ZZ"`     | absent  | leeg             |
| V3     | (leeg)          | absent  | leeg             |

`gekeurd` is `waar` iff `keurmerk` equals `merk "AA"` — V1 matches, V2 differs,
V3 is leeg (Tabel 38: any-empty equality is false). `afslag` is written only
where `gekeurd` holds (V1), leeg otherwise.

ref = clingo over every expectation (gate 5 ≡ gate 6). NOOP-REFUTING: V1's
`gekeurd`/`afslag` are computed, absent from the raw input, so a do-nothing run
mismatches them.
