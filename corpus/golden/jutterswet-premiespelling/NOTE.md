# jutterswet-premiespelling — two object types, one Java class

Two object types whose names differ only in how the words are joined:
`RechtOpPremie`, written as one word, and its subtype `Recht op premie`,
written as three. WF-001 compares object-type names case-insensitively,
so `rechtoppremie` and `recht op premie` are two names and the document
is well-formed (`verifier: PASS`). The Drools backend turns a name into
a Java class name by dropping the spaces and capitalising each word, and
both names become the one fact class
`dev.uchar.regelspraak.facts.RechtOpPremie`. The golden pins what the
two other executors answer and holds the Drools backend to a refusal
that names both types and the class.

## The rule

    De premie van een Recht op premie moet berekend worden als de grondslag
    van het Recht op premie plus 1.

`grondslag` is declared on `RechtOpPremie` and inherited by `Recht op
premie`; `premie` is the subtype's own.

## Hand computation

`case1-grondslag-vier.json`: P1 is a `Recht op premie` with grondslag 4,
so its premie is 4 + 1 = **5**. P2 is a `RechtOpPremie`, the supertype,
with grondslag **7**; the rule ranges over `Recht op premie` only and
the supertype declares no premie, so nothing is computed for P2 and its
grondslag reads back as given.

`case2-grondslag-nul.json`: P3, a `Recht op premie` with grondslag 0, has
premie 0 + 1 = **1**; a grondslag of zero is a value, not leeg.

The input carries no premie, so a no-op run fails both cases.

## The square

`ref` and the ASP backend replay both cases (`oracle: PASS (2
expectations)`, `clingo: PASS (2 expectations)`; 1 for the second case).

The Drools backend refuses the document before it generates a bean, in
both branches and with one sentence:

    EXCLUDE beannaambotsing: de objecttypen 'RechtOpPremie' en 'Recht op premie' vallen op één feitklasse, dev.uchar.regelspraak.facts.RechtOpPremie, en het beanmodel maakt één klasse per naam

Before that refusal both branches crashed while compiling the generated
beans: the class written for `Recht op premie` replaced the one written
for `RechtOpPremie` and extended itself (`cyclic inheritance involving
dev.uchar.regelspraak.facts.RechtOpPremie`). Without the supertype the
same collision broke the bean contract instead (a getter of the first
type missing on the class of the second), and with identical members the
one class served both types, so a rule on one type also matched the
instances of the other: a wrong value with no refusal. The golden's pins
are therefore a ref = clingo pair; gate 12 lists it among the goldens it
does not play, with this reason.
