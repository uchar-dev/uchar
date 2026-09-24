Violates WF-109 (`spec/IR-SPEC.md` ch. 4.6 (q2)) at its **first scope fence**:
the doel is a bound object only where the check IS the whole guard. Here the
doel host sits inside a composite, one conjunct beside an anker-hosted one, and
at that nested position the anker is the only object bound — so `Sein` names
nothing.

    WF-109 FAIL /rules/feitCreaties/0/versions/0/condition/onderdelen/0/eigenaar:
    the characteristic check is hosted on 'Sein', which no object bound here can
    be: this position binds the subject 'Kustvak'

Broken from `corpus/golden/jutterswet-seinhijsing` by wrapping one field: the
first creation's bare guard becomes an ALLE composite over itself and a second
check hosted on the anker. Both conjuncts are individually well-formed; it is
the nesting that the rule refuses.

Why the fence exists, measured rather than assumed. The Drools exec-model
branch lowers a doel-side guard through a fact-only leaf predicate — one
constraint inside the `$doel` pattern — and has no composite route there;
measured 2026-09-19, a nested doel host makes that branch decline the guard
altogether. The classic DRL-text branch can inline a composite on the doel
pattern, so the two branches would answer differently about one document. A
verifier that admits what one executor cannot carry re-opens exactly the split
this rule closed, so the position is refused for all of them instead. The fence
is cheap to lift: a witness in all four executors is its whole price.

Note what is NOT refused. A composite guard whose checks carry no host at all,
or whose hosts name the anker, is untouched — that is the ordinary anker-side
guard and it keeps working (`corpus/golden/jutterswet-dubbelverbinding` carries
one).

Falsifiable in two directions, each turning this case clean:

- unwrap it — the doel host as the whole guard, which is the golden;
- re-host the first conjunct on `Kustvak` — a purely anker-side composite, at
  which point the whole guard gates the anker.
