# wf034-sentinel-gelest-wratoperand

Violates WF-034 (`spec/IR-SPEC.md` ch. 4.3, the sentinel fence): the same
position as `wf034-sentinel-gelest-vouwoperand` — a LISTED fold operand — but
the OTHER subform, the one that behaves differently in the third executor.
Here the operand is the row-1 wart spelled VERBATIM: a declared ROLE plural
in the `attribuut` slot, `set: false`, no `navigatieRol`, the unknown
sentinel on its own `type`. The wart is the fold SOURCE's spelling; a listed
value fold has no source, so there is nothing for it to be.

    WF-034 FAIL /resolvedRegels/0/waarde/argumenten/1/type: unknown-type
    sentinel on a fold operand that is not a fold SOURCE: the sentinel is
    admissible on the single argument of an AGGREGATION alone (the v0 row-1
    wart, ch. 4.3) — with two or more arguments the node is the listed value
    fold, whose operands are ordinary reads, and at a non-aggregation kind
    there is no source at all

The document. A `MAXIMUM` with two arguments and an `elementObjecttype`;
operand 0 an ordinary read of `drempelgewicht` with its declared type,
operand 1 `attribuut: "vondsten"` (the declared plural of the role `vondst`)
on `objecttype: "Jutter"` under the sentinel. One fault, and the reason it
was the LAST shape standing: ch. 4.3's role-name exception is keyed on the
sentinel and lives in this very branch, so the name resolution half ADMITS
`vondsten` here — no other rule fires. Without this clause the document is
clean outright.

This is not a hand form. Measured 2026-09-06: the RegelSpraak frontend emits
this document, with **no diagnostic at all**, from
"… als de maximale waarde van het drempelgewicht van de Jutter en de
vondsten van de Jutter" — the reference builder finds no attribute datatype
and falls back to the sentinel, and the fold branch's role exception then
admits the name. Re-measured leaf by leaf in the fix round, the emitted
document and this one differ in **exactly three leaves**, none of them the
defect: the two `bron` provenance strings (the frontend writes the article,
`de vondsten van de Jutter`, and phrases the fold as `de maximale waarde
van …`) and the fold node's OWN `decimalen`, which the frontend derives as 0
where this document carries 2. The wart operand — `attribuut`, `objecttype`,
`set`, `navigatieRol`, `navigatiePad` and the sentinel `type` — is
byte-identical, which is the claim that matters, and the frontend's own
emission is refused by the same clause at the same pointer with the same
message. The sibling spelling, where the unresolved name matches no
declared role, always co-fires WF-034's resolution arm and was never clean;
this one, where the name happens to coincide with a declared role name or
plural, is the whole open flank.

Rood-eerst, measured on this document. Pre-rule verifier
(md5 `2ab63519d103a0abee721296973a61d7`): `verifier: PASS`. With the clause
(md5 `c95650b1ab993c8a8b370e5b0617b0e6`): the rejection above.

Executors, on this document (case: one Jutter of drempelgewicht 12.00 kg
linked to two Vondsten of 12.00 and 7.00 kg):

    ref         : EVALUATION FAULT: unsupported AttribuutRef shape
                  (set/sentinel outside a fold)
    ASP backend : TRANSLATION FAULT: expression kind not translated (no
                  corpus case exercises it)
    Drools      : ERROR ConsequenceException … java.lang.IllegalStateException:
                  geen getter voor 'vondsten' op … .facts.Jutter

The third executor READS that role name after all — it reaches for a getter
and throws. That is exactly where the two subforms part company: its sibling
(`wf034-sentinel-gelest-vouwoperand`) it plays and answers. One rule refuses
both, and the spec says why the difference does not split it: neither is a
read any executor can carry, and the wire is the one place both can be
stopped.

Falsifiable, arm-exact: drop the sentinel clause at the head of the
fold-argument branch's reference arm (`spec/verifier/lib/checks.ml`); this
case turns clean along with the five other `wf034-sentinel-*` operand
witnesses and nothing else moves over the 540 documents of the four zones.
