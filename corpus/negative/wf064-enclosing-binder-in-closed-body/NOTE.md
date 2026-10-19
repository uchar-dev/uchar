Violates WF-064 (`spec/IR-SPEC.md` ch. 4.7/4.8/4.10) — the closure
clause. The `Let` binder `f` is bound in the *enclosing* rule scope, then
read inside a **closed** fold lambda body (`λ acc x. acc + x * f`) — the
mandated "Maal in a fold in a Let" composition. A closed function or
lambda body sees only its own parameters (`acc`, `x`) and whatever it
binds inside; a `BinderRef` that reaches out to an enclosing binder is
ill-formed. The verifier reports it at
`/rules/consistencies/0/versions/0/assertion/right/body/function/body/right/right`:

    binder reference 'f' is bound outside the enclosing closed body — a
    closed function or lambda body reads only its own parameters and
    inner bindings

Sharpness — the check hits *only* the outer-binder read. A binder bound
INSIDE the closed body stays well-formed: the fold parameters `acc`/`x`
here, and equally a match arm's payload binder or a `Let` bound inside
the body (an inner `Let` in a lambda body resolves against its own
binder, never the enclosing scope).

Why it matters (the executor split the fence prevents). This document is
otherwise well-formed, so before this clause it passed the verifier and
reached both executors, which faulted *differently*: `ref` faults cleanly
at evaluation —

    EVALUATION FAULT: unbound binder "f" (the verifier should preclude this)

— while the ASP backend drops the free binder into an unsafe rule and
`clingo` grounding-crashes ungracefully (`'LB3' is unsafe`, gringo stops,
clingo exits 65) rather than emitting a clean TRANSLATION FAULT. Both
fault loudly (no wrong answer), but the split is a robustness/diagnosis
gap: WF-064's closure clause weens the ill-formed IR at gate 3, ahead of
either executor.

A minimal hand-built document.
