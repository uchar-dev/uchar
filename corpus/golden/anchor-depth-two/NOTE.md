# anchor-depth-two — the anchor two folds deep

The model of `jutterswet-registervoorrang` (`ingangsdag`, `tarief` on the
Vergunning). Each rule folds the Vergunningen of the register of the
Vergunning (the outer fold, member m) with a filter that holds a second fold
or quantifier over the Vergunningen of m's register (the inner one, member k).
The anchored read in the inner fold's filter or test reads the subject of the
form, the Vergunning the rule runs on, not m (IR-SPEC ch. 4.1a: "whichever
fold and filter it stands in, at any depth"; RUNTIME-SEMANTICS ch. 3.4a).
Written in fix round A of the card `subselection-reads-subject` (2026-10-23),
review A, point 4.

| rule | inner fold in the filter of the outer one |
|---|---|
| tarief bij latere in register | the highest tarief of m's register later than the Vergunning zelf is above 0 |
| tarief als laatste | none of m's register starts later than the Vergunning zelf |
| tarief bij duur zonder zichzelf | the highest tarief of m's register without the Vergunning zelf (an `IdentityCheck`) is above 15 |
| tarief bij een latere | exactly one of m's register that starts later than the Vergunning zelf (the anchored read in the inner quantifier's filter) has a tarief above 0 |

The Drools backend refuses an anchored node read per member of another fold
by name (DROOLS-293); `scripts/check-drools.sh` pins the golden there, so its
pins are a ref = clingo pair.

No `source.rgs`: written by hand, from the proofs of review A (p5, p12, p18,
p21); whether the frontend writes these sentences was not measured in fix
round A, which routed no golden through the frontend while review B read it.

## Hand computation

`case1-two-registers.json`: R1 holds V1 (2026-01-01, 10), V2 (2026-03-01, 20)
and V3 (2026-06-01, 15); R2 holds V4 (2026-02-01, 5) alone. Every m of one
register has the same inner fold, so the outer filter lets all members through
or none.

- tarief bij latere in register: the highest tarief later than V1 is 20, than
  V2 15, than V3 none, than V4 none: V1 **20**, V2 **20**, V3 leeg, V4 leeg.
  A reader that took the anchor for m would let V1 and V2 through for V3.
- tarief als laatste: only for V3 does no Vergunning of R1 start later: V3
  **20**; V4 is alone in R2: **5**; V1, V2 leeg.
- tarief bij duur zonder zichzelf: without V1 the highest is 20, without V2
  15, without V3 20, without V4 none: V1 **20**, V3 **20**, V2 and V4 leeg.
- tarief bij een latere: later than V1 are two, than V2 one (V3), than V3 and
  V4 none: V2 **20**, the rest leeg.

Consistent.
