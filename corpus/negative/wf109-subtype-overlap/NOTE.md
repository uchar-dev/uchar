Violates WF-109 (`spec/IR-SPEC.md` ch. 4.6 (q2)) on the **ambiguity by
population overlap** — the flank the rule was blind to until 2026-09-19 and
the reason the hit test is no longer a plain `⊑` count.

    WF-109 FAIL /rules/factCreations/0/versions/0/condition/host: the
    characteristic check is hosted on 'Sein', which the target 'Sein' is — but
    so can the anchor 'Kustvak' be, because the model declares a type below
    both: the host names one object on paper and two at run time

`Sein` is declared a **subtype of** `Kustvak` here, which means every `Sein`
IS a `Kustvak` and can fill any position declared `Kustvak` — including the
anker of this fact creation. So `host: "Sein"` designates the doel (always)
and the anker (whenever the anker object happens to be a `Sein`), which is one
name for two objects.

The defect is in the DOCUMENT and the negative zone carries documents, not
cases — this directory holds an `ir.json` and nothing else, as every negative
directory does. The measurement below was taken with a throw-away population in
which the anker is an instance of `Sein`; that population is what makes the
ambiguity concrete, and it is deliberately not committed, because a case here
would have nothing to replay against.

Broken from `corpus/golden/jutterswet-seinhijsing` by three moves, each
necessary: `Sein` gets `supertype: "Kustvak"`; its own `gehesen` declaration is
removed, so the characteristic is INHERITED (WF-105 forbids re-declaring a name
along one chain, and the inherited reading is what makes WF-100 clean); and
`seinvoering`'s `post` role is renamed `seinpost`, because a `Sein` that is now
also a `Kustvak` inherits `kustligging`'s `post` participation and the two
same-named roles would be an unrelated ambiguity. The second creation and its
`duisteraantal` are dropped — one defect, one creation.

Why it has to exist. Measured 2026-09-19 on this exact document with that
population, BEFORE the rule was sharpened: schema **PASS**, verifier **PASS**,
`ref` **PASS** and the ASP backend **PASS**, all four agreeing on a number while
the host was ambiguous. The old test counted a binding as a hit when its DECLARED type was
`⊑` the host, and a declared `Kustvak` is not a `Sein` — so the anker was
invisible to it. Populations are extensional and the count has to be too: a
binding is near the host when the declared type graph holds a type below both
(`populations_overlap`), and a second near binding is a second candidate,
whether or not it is guaranteed.

The reading is not arbitrated, it is refused. Which of the two a `Sein`-typed
anker means is a question the wire does not answer, and no precedence is
invented for it — the same discipline as the self-typed pair
(`wf109-host-ambiguous-self-typed`) and the common supertype
(`wf109-host-ambiguous-supertype`), of which this is the third shape.

Falsifiable in three directions, each turning this case clean:

- drop the `supertype` edge — the populations separate and the host names the
  doel alone, which is the golden;
- give the anker a type that cannot be a `Sein` — the overlap goes with it;
- empty the field — the guard gates the anker and no host is designated.

`ref` refuses the same document at evaluation, in the sentence quoted above:
since 2026-09-25 the verifier and `ref` (and with it the ASP backend, which asks
`ref`'s helper) take WF-109's sentences from one place in the verifier's checks
library, so the executor does not depend on the verifier having run and says
what the verifier says. Measured on `ref`'s own suite, whose in-memory document
has this shape: the fault is that sentence, byte for byte.

Since L5 the field is `host` (delta B10); the report cites `…/host`, and
`expect.txt` pins the arm.
