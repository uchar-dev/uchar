# ambiguous-backlink

The ch. 5.5 refusal to guess: two fact types relate the creating
subject to the created type, so the created object's back-link is
ambiguous. The pinned v0 runtime silently drops the back-link there (an
"onopgelost terugkoppeling" comment in generated code); execution here
faults instead — in the reference interpreter at evaluation, in the ASP
backend at translation, through the shared derivation — a recorded
delta, fail-fast over silent degradation. The document is
verifier-clean: no well-formedness rule pins connecting-relation
uniqueness for object creation yet (a candidate WF if a corpus case
ever needs the ambiguity statically rejected).
