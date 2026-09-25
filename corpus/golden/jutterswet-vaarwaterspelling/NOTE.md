# jutterswet-vaarwaterspelling — the ci witness of a domain name

A `Kadeplaats` has a `vaarwater` of the declared enumeration domain
`Vaarwater` (`Wad`, `Zeegat`, `Ruimzee`), a `kadegeld` that a `Match` over
that attribute derives (`Wad` → 2, `Zeegat` → 5, `Ruimzee` → 9), and a
`standaardvaarwater` that a rule sets to `Wad`.

The domain name is written in another case, **`VAARWATER`**, at two
positions the verifier reads case-insensitively, the WF-001 naming rule. The
two cases pin absolute values (`kadegeld` 5 and 9, `standaardvaarwater`
`Wad`); nothing else in the document differs from a plain spelling.

- **The lookup.** The datatype of the attribute `vaarwater` names
  `VAARWATER`. The `Match` reads its scrutinee's declared type and looks the
  domain up to check its arms (WF-063), and WF-088 looks it up to compare
  the value list; both lookups are ci. Made case-sensitive, the lookup turns
  this golden red:

      WF-063 FAIL /rules/rules/0/versions/0/value/scrutinee: match scrutinee
      names enumeration domain 'VAARWATER', which is not a declared
      enumeration domain

- **The identity.** The rule `standaardvaarwater van een kadeplaats` writes
  the literal `Wad` typed `VAARWATER` into `standaardvaarwater`, declared
  `Vaarwater`. WF-023's write half compares the keuzetype identity (variant
  and domain name) that WF-089 compares on the read half, ci. Made
  case-sensitive, the identity turns this golden red:

      WF-023 FAIL /rules/rules/1/versions/0: a value of the enumeration
      domain 'VAARWATER' is assigned to a target of the enumeration domain
      'Vaarwater'

  WF-089's read half has no position where a keuzetype stands in a
  verifier-clean document — the element-passing folds take numbers and dates
  only (WF-038), and the composite positions are numeric — so a write
  carries the identity.

Measured 2026-09-25 with the two mutants over the golden and rood zones:
each turns this golden red and leaves every other document clean.

**What the verifier compares byte for byte.** A rule that compares two
datatypes whole (WF-062, WF-064, WF-066 to WF-070, WF-073, WF-074, WF-076)
compares the domain name in them byte for byte: on a probe outside the
corpus, a `Let` typed `VAARWATER` over a body typed `Vaarwater` is refused
under WF-076. This golden carries no such position.

**Until L2** the one drift sat on the `Match` scrutinee's own type, an
attribute reference typed `VAARWATER`. Since L2 a reference carries no type
of its own (B13); the spelling stayed only in the `Match`'s source snippet
(`match VAARWATER`), and until 2026-09-25 the golden pinned nothing that
`jutterswet-vaarwatertoeslag-catala` did not pin as well.

`ref` and the ASP backend replay both cases (3 expectations each); the
Drools backend refuses the document at load in both branches (`Expr:Match`).
Hand-authored, with no frontend source: neither frontend writes a domain
name other than the declaration's. The identity transpiler fails on it,
since `kadegeld` and `standaardvaarwater` are absent from the input.
