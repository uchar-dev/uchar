# wf034-sentinel-kale-lezing

Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6, the sentinel fence at a value
node): the unknown-type sentinel on a BARE attribute read — here the whole
value of a computing rule. This is the position the expression walk's own
type prologue has always covered; the three `wf034-sentinel-gelest*` siblings
cover the position the fold-argument branch withheld from it.

    WF-034 FAIL /resolvedRegels/0/waarde/type: unknown-type sentinel inside a
    structured node

The document. One computing rule whose value is an `AttribuutRef` on the
subject's own declared `drempelgewicht`, carrying the unknown sentinel
instead of that declaration. Deliberately a read that RESOLVES: an
unresolvable one co-fires WF-034's name-resolution arm and would leave this
arm untested. One fault.

**Not rood-eerst, and stated as such.** This shape was already rejected
before the card that added it; what was missing was a witness. Measured
2026-09-06 over the whole negatief zone with the verifier of the branch head
(md5 `2ab63519d103a0abee721296973a61d7`): NO case in the zone caused the
message above, so the arm was live but unpinned — a rule the corpus could not
have caught regressing. The card's other three witnesses are the rood-eerst
ones; this one closes the gap they made visible, so that the "one decision
for every read position" the spec states is machine-checked at every one of
them.

Executors, on this document (case: one Jutter of drempelgewicht 12.00 kg):

    ref         : EVALUATION FAULT: unsupported AttribuutRef shape
                  (set/sentinel outside a fold)
    ASP backend : TRANSLATION FAULT: expression kind not translated (no
                  corpus case exercises it)
    Drools      : PLAYED — it fires and answers 12.00

The same one-answers-two-refuse split the fold-operand siblings show, which
is why one decision covers both positions.

Falsifiable, arm-exact: make the expression walk's type prologue return no
violation on `Datatype_onbekend` (`spec/verifier/lib/checks.ml`). Swept over
golden + rood + negatief + parity (537 documents) exactly this case turns
clean — plus three report lines in the parity zone, which stays rejected
either way — and nothing else moves. Before this case existed the same
mutation freed nothing at all in the three gated zones.
