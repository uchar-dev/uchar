# jutterswet-zinkende-jutter — characteristic checks under a quantifier

The golden of the characteristic check that a quantifier folds over the
finds of a Jutter, in the spellings whose predicate the parse partly takes
into the member phrase (`frontends/regelspraak/grammar/DIVERGENCES.md`,
"§13.4.15.45 A characteristic check under a quantifier reads its name
whole"). In "alle vondsten van de Jutter geen drijfvermogen hebben" the
anchor's name runs on over every name word: the anchor reads "Jutter geen"
and the operator keeps "drijfvermogen" alone. The frontend now reads the
words after the name the anchor writes the subject with as part of the
predicate, so the negation lands on the matrix and a multi-word name is
bound whole.

`bron.rgs` declares a Jutter with nine adjectival characteristics and a
Vondst with five: `drijfvermogen` and `recht op berging` (possessive),
`waardevol`, `gekeurd` and `dubbel gekeurd` (adjectival), joined by the
fact type `vinder van vondst`, whose Jutter role `vinder` is not the type's
name. Each Jutter characteristic has one rule with one form:

| rule | condition | quantifier, kind, polarity |
|---|---|---|
| zinkende jutter | `alle vondsten van de Jutter geen drijfvermogen hebben` | all, possessive, negated with geen |
| berooide jutter | `alle vondsten van de Jutter niet waardevol zijn` | all, adjectival, negated with niet |
| drijvende jutter | `geen van de vondsten van de Jutter geen drijfvermogen heeft` | none, possessive, negated with geen |
| vlotte jutter | `ten minste één van de vondsten van de Jutter drijfvermogen heeft` | at least one, possessive, affirmed |
| meldplichtige jutter | `ten minste één van de vondsten van de Jutter geen recht op berging heeft` | at least one, multi-word possessive, negated with geen |
| onvermogende jutter | `geen van de vondsten van de Jutter waardevol is` | none, adjectival, affirmed |
| vrijgegeven jutter | `alle vondsten van de vinder dubbel gekeurd zijn` | all, multi-word adjectival, affirmed, anchor written as the role |
| duikplichtige jutter | bullet 1 `alle vondsten van de Jutter geen drijfvermogen hebben`, bullet 2 `ten minste één van de vondsten van de Jutter waardevol is`, under `alle volgende voorwaarden` | the negated possessive form in a bullet |
| keurplichtige jutter | `ten minste één van de vondsten van de Jutter niet gekeurd is` | at least one, adjectival, negated with niet |

Every fold carries the range `vinder van vondst` / `vondst` as its
`sourceRole` and its characteristic check as the `test`.

## Before: what the frontend wrote

The frontend built at the commit before this work wrote this source with two
warnings (real output of `--ir`):

    WARNING (line 47): de kwantorvouw over 'ten minste één van de vondsten van de Jutter geen recht op' wordt NIET gelowerd: 'berging' is geen kenmerk van Vondst — …
    WARNING (line 47): onopgelost: de geschreven naam bindt geen kenmerk en geen rol van het onderwerp (§8.1.7/§8.1.8), dus er is geen lidmaatschap om te toetsen — 'ten minste één van de vondsten van de Jutter geen recht op berging heeft' blijft als marker op de wire staan

and with these conditions, per rule (the matrix of each fold):

| rule | before | now |
|---|---|---|
| zinkende jutter | all: drijfvermogen, negated false | all: drijfvermogen, negated true |
| berooide jutter | all: waardevol, negated true | the same |
| drijvende jutter | none: drijfvermogen, negated false | none: drijfvermogen, negated true |
| vlotte jutter | atLeast: drijfvermogen, negated false | the same |
| meldplichtige jutter | Unresolved (the marker above) | atLeast: recht op berging, negated true |
| onvermogende jutter | none: waardevol, negated false | the same |
| vrijgegeven jutter | all: **gekeurd**, negated false | all: dubbel gekeurd, negated false |
| duikplichtige jutter | bullet 1 all: drijfvermogen, negated false | bullet 1 all: drijfvermogen, negated true |
| keurplichtige jutter | atLeast: gekeurd, negated true | the same |

The marker made every executor fault on the document. Without the rule
`meldplichtige jutter` the old document had `diagnostics: []` and was
answered wrongly by all of them: ref and the ASP backend reported 3
mismatches in the first case and 9 in the second (`J2 carries
characteristic "zinkend", expected absent`, `J5 carries characteristic
"vrijgegeven", expected absent`, `J7 lacks characteristic "duikplichtig"`,
…), and the Drools exec branch `FAIL` on both cases with the same values.

## Hand computation

The finds, with the characteristics each carries (d drijfvermogen, w
waardevol, r recht op berging, g gekeurd, dg dubbel gekeurd):

| jutter | finds |
|---|---|
| J1 | none |
| J2 | V1 {d, w, r, g, dg}, V2 {d, w, r, g, dg} |
| J3 | V3 {d, w, r, g, dg}, V4 {} |
| J4 | V5 {}, V6 {} |
| J5 | V7 {g}, V8 {g} |
| J6 | V9 {w, g}, V10 {d, r} |
| J7 | V11 {w, g, dg}, V12 {g} |

`alle` over no finds holds and `geen van de` over no finds holds; `ten
minste één van de` over no finds does not. Per Jutter (yes = the
characteristic is derived):

| jutter | zinkend (all ¬d) | berooid (all ¬w) | drijvend (no ¬d) | vlot (some d) | meldplichtig (some ¬r) | onvermogend (no w) | vrijgegeven (all dg) | duikplichtig (all ¬d and some w) | keurplichtig (some ¬g) |
|---|---|---|---|---|---|---|---|---|---|
| J1 | yes | yes | yes | no | no | yes | yes | no (no w) | no |
| J2 | no | no | yes | yes | no | no | yes | no | no |
| J3 | no (V3 d) | no | no (V4 ¬d) | yes | yes (V4) | no | no (V4) | no | yes (V4) |
| J4 | yes | yes | no | no | yes | yes | no | no (no w) | yes |
| J5 | yes | yes | no | no | yes | yes | **no** (no dg) | no | no |
| J6 | no (V10 d) | no (V9 w) | no (V9 ¬d) | yes | yes (V9) | no | no | no | yes (V10) |
| J7 | yes | no (V11 w) | no | no | yes | no | **no** (V12 ¬dg) | **yes** | no |

J5 and J7 carry `gekeurd` on every find but `dubbel gekeurd` not on every
find: the old reading of `dubbel gekeurd` as `gekeurd` derives
`vrijgegeven` for both. J2 has drijfvermogen on every find and J4, J5 and J7
on none: on those four the affirmed reading of `zinkend` and `drijvend`
answers the opposite of the negated one. J7 is the only Jutter that is
`duikplichtig`.

`case1-lege-volle-en-halve-vangsten.json` carries J1 to J3,
`case2-kale-gekeurde-en-gekruiste-vangsten.json` J4 to J7; both also expect
`consistent`, the document having no consistency rule. The input carries no
Jutter characteristic, so a no-op run fails both cases (J1 and J4 are
`zinkend`). `scenario.rgs` writes the two scenarios; the frontend's
`--cases` mode lowers them to the two case files, and gate 16 regenerates
them from it byte for byte.

## The square

`ref` and the ASP backend replay both cases (`oracle: PASS (28
expectations)`, `clingo: PASS (28 expectations)`; 37 for the second case),
and both Drools branches play them (`PLAYED 2`, exec model and DRL text).
