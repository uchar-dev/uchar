# Emitter pin bump: the toolchain emits verbindendeRol and bronRol

Goal: the first pin bump of the wire-entry discipline. The toolchain
repo now emits two of the extension fields the anchors pioneered — the
allocation's `verbindendeRol` and the instance fold's `bronRol`
(emitted only when uniquely derivable from declared names; absent
otherwise, never null). This card lands the conformance side here:
re-emit the five emitter goldens on the new toolchain commit, show the
byte-diff is exactly the additive extension fields, and move the pin in
`spec/IR-SPEC.md` § Method (with the "the pinned v0 emitter never
produces it" sentence corrected to name what the pinned emitter now
produces).

Acceptance: all six gates green; `git diff` over the five emitter
goldens shows only added `verbindendeRol`/`bronRol` objects (no other
byte changes); IR-SPEC § Method names the new emitter commit;
`CONSTRUCTS.md` rows 4/17 record the landing.

May touch: `corpus/golden/{jutterswet-berging,jutterswet-penningdrempel,jutterswet-penningjaar,jutterswet-vondstnummer,jutterswet-vondsttelling}/ir.json`,
`spec/IR-SPEC.md` (§ Method pin note), `spec/CONSTRUCTS.md` (landing
notes), `backlog/`.

Out of scope: emitting the remaining extensions (`TemporalRead`,
prioritized defaults, symbolic creation fields — their surface syntax does not exist
in the toolchain yet; recorded, not attempted); any v1 format bump;
schema or verifier changes (the fields are already admitted).

Result: landed. The toolchain (final pin `edecb46`; the binding work in
`911f064`, plus a behaviour-neutral classification-order mirror) emits
`verbindendeRol` (allocations, by-pair with the runtime's exact
counting; reciprocal winners withheld) and `bronRol` (member folds by
pair; the count-names-a-role wart by declared name only). Re-emission
changed two of the five goldens, purely additively — berging gained
one verbindendeRol, vondsttelling two bronRol objects; the other three
are byte-identical (penningjaar's fold is the temporal total, not
relational — correctly unbound). All six gates green; the verifier
consumes and validates the emitted bindings (WF-047/048).

Verification round (two opus lenses) corrected before merge: the
§ Method sentence that misattributed the anchor-only remainder to
missing surface syntax (the creation surface form exists; its
emitter-side role derivation does not), the stale anchor-README claim
that extensions are emitter-unproducible as a category, and the
"emitting since" commit (446dfa6, not c51652a).

Recorded, deliberately not fixed here (all latent, none reachable by
any current golden; all fail hard in gate 3, never silently): (1)
instance folds over a wederkerig (single-role) fact type are
unverifiable on both routes — bindingless trips WF-048's two-role
candidate search, and a binding would trip resolve_binding's two-role
requirement; the reciprocal fold needs its own uchar-side design
before any emitter work. (2) The emitter's name matching
(equalsIgnoreCase, full Unicode) is wider than the verifier's
ASCII-only norm: a non-ASCII case-variant spelling could emit a
binding WF-047 rejects. (3) Folds inside objectCreatie/feitCreatie
forms get no binding (no subject context in the emitter there);
bindingless is discipline-safe unless the pair is ambiguous, which
WF-048 would surface loudly. (4) On the two re-emitted goldens the
binding and the type-pair derivation coincide by construction; the
behavioural witness for bindings that *matter* remains the
two-relations anchor (`jutterswet-bergingstoezicht`).
