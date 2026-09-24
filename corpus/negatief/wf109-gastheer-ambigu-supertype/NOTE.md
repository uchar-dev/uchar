Violates WF-109 (`spec/IR-SPEC.md` ch. 4.6 (q2)) on its **two-satisfying-objects
flank**, in the shape the rule names explicitly and had no witness for until
now: a **common supertype**.

    WF-109 FAIL /rules/feitCreaties/0/versions/0/condition/eigenaar: the
    characteristic check is hosted on 'Baken', which the anchor 'Kustvak' and
    the target 'Sein' both are: the host names one object and this names two

`Kustvak` and `Sein` are both declared subtypes of `Baken`, and `gehesen` is
declared on `Baken` alone — so WF-100 is satisfied (the host declares the
characteristic) and WF-034 is (the host is a declared type). What the host does
not do is pick: every anker IS a `Baken` and every doel IS one, so the type
string reaches both objects and settles nothing.

Broken from `corpus/golden/jutterswet-seinhijsing` by adding one type and
moving two declarations: a `Baken` root carrying `gehesen`, with `Kustvak` and
`Sein` as its subtypes and their own `gehesen` declarations removed (WF-105
forbids re-declaring along a chain), and the guard's host set to `Baken`.

This is the SELF-TYPED pair's general form. There the anker type and the doel
type are the same name; here they are two names that meet above. Both are one
type string over two objects, which is why one rule and one report line cover
them, and why neither gets a precedence invented for it — the producer writes
neither shape.

Falsifiable in three directions, each turning this case clean:

- host it on `Sein` — the doel reading, the golden's own;
- host it on `Kustvak` — the anker reading;
- drop one of the two supertype edges — whichever type leaves the chain stops
  being a `Baken`, and the host names one object again.
