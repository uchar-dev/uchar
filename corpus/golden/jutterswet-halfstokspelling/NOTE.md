# jutterswet-halfstokspelling — an attribute and a characteristic on one bean property

A `Sein` carries a Boolean attribute `half-stok` — the post registered
the flag at half-mast — and a characteristic `half stok` that a rule
derives from the height at which the flag hangs. WF-002 keeps attributes
and characteristics apart, and the two names differ by a hyphen, so the
document is well-formed (`verifier: PASS`). The Drools backend makes a
bean property of a name by dropping its spaces and hyphens and
camel-casing its words, and both members become the property
`Sein.halfStok`. The bean model carries one field per property, the
attribute's; the characteristic has none of its own.

## The rules

    Een Sein is half stok
    indien de hoogte van het Sein kleiner is dan 5.

    De rouwmelding van een Sein moet berekend worden als 1
    indien de half-stok van het Sein gelijk is aan waar.

The first writes the characteristic; the second reads the attribute.

## Hand computation

`case1-laag-zonder-registratie.json`: S1 hangs at height 3, which is
smaller than 5, so S1 **is half stok**. No registration is given, so the
attribute `half-stok` is leeg, the condition of the second rule does not
hold, and the rouwmelding stays **leeg**.

`case2-hoog-met-registratie.json`: S2 hangs at height 8, not smaller than
5, so S2 is **not** half stok. The registration `half-stok` is waar, so
the rouwmelding is **1**.

The input carries neither the characteristic nor the rouwmelding, so a
no-op run fails both cases.

## The square

`ref` and the ASP backend replay both cases (`oracle: PASS (2
expectations)`, `clingo: PASS (2 expectations)`, each case).

The Drools backend refuses the document before it generates a bean, in
both branches and with one sentence:

    EXCLUDE beannaambotsing: het kenmerk 'half stok' van Sein valt op de beaneigenschap Sein.halfStok van het attribuut 'half-stok' van Sein en heeft geen eigen veld; regel 'halfstok van een sein' leest of schrijft het

Before that refusal both branches played the document and answered two
wrong values without a refusal: the rule that derives the characteristic
wrote the attribute's field, so S1's rouwmelding came out 1, and the
characteristic of S2 was read from the registration, so S2 came out half
stok (`verwacht leeg, werkelijk 1` and `kenmerk half stok: verwacht
afwezig, werkelijk aanwezig`). A member without a field of its own that
nothing reads or writes does not stop a document; only a use of it does.
The golden's pins are therefore a ref = clingo pair; gate 12 lists it
among the goldens it does not play, with this reason.
