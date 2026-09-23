Violates WF-113 (a) (`spec/IR-SPEC.md` ch. 2): a `WARNING` whose `message` is
the empty string.

    WF-113 FAIL /diagnostics/0/message: diagnostic with an empty or blank message: an entry that says nothing accounts for nothing

A minimal hand-broken variant of `corpus/golden/jutterswet-vondstverjaring/ir.json`:
the golden unchanged, plus one entry with severity `WARNING`, line 8, and
`"message": ""`. The schema types `message` as a string and cannot demand that
it is non-empty; the verifier does. An empty message can account for no marker
under WF-113 (b) — it contains no `bron` — so it is an entry that reports
nothing. The blank variant is `wf113-message-blanco`.
