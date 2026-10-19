# jutterswet-zwaarste-weging — a fold over a set followed by to-one steps

Exercises a value fold over ONE set whose member is read through to-one steps
after the set (`frontends/regelspraak/grammar/DIVERGENCES.md`, "A fold over a
set and to-one steps after it"): "de maximale waarde van het gewicht van de
weging van de vondst van alle jutters van de Ploeg". The RegelSpraak frontend
lowers the steps to a member it generates on the element type, set by a rule
it generates from the chain, and folds over that member; a subselection stays
on the element. Beside it the same model spells the flat union over a
`sourcePath`, so the two readings of a sum over a shared object stand side by
side. Own Jutterswet-flavoured casus, own values, rights-free.

## Casus (own text)

> **Art. 3j (ploegwegingen).** 1. Het zwaarste gewicht van een ploeg is het
> grootste gewicht van de weging van de vondst van een actieve jutter van de
> ploeg. 2. De eerste vinddag van een ploeg is de vroegste vinddag van de
> vondst van een jutter van de ploeg. 3. De bootcapaciteit van een ploeg is de
> som, over de jutters van de ploeg, van het laadvermogen van de boot waarop de
> jutter vaart; vaart een boot met meer jutters van de ploeg, dan telt zij voor
> elke jutter. De vlootcapaciteit van een ploeg is de som van het laadvermogen
> van de boten waarop haar jutters varen; elke boot telt één keer. 4. Een
> jutter is actief als zijn startdag bekend is.

## Source (`source.rgs`)

- **zwaarste gewicht** (lid 1): the maximum over the jutters of the Ploeg
  that are actief, of the gewicht of the weging of the vondst of the jutter —
  two to-one steps after the set, and a subselection on the element.
- **eerste vinddag** (lid 2): the earliest vinddag of the vondst of the
  jutters — one step, a date.
- **bootcapaciteit** (lid 3, first sentence): the sum of the laadvermogen of
  the boot of the jutters — one step to an object two jutters can share.
- **vlootcapaciteit** (lid 3, second sentence): the sum of the laadvermogen
  of "alle boten van alle jutters van de Ploeg" — the boat written as a set
  step, so the range is a two-step `sourcePath` and the fold the flat union of
  RUNTIME-SEMANTICS ch. 3.4b.
- **actief** (lid 4).

## Lowering (the frontend's)

The element type Jutter gets three generated members, after its own: `gewicht
via vondst en weging`, `vinddag via vondst` and `laadvermogen via boot`, each
of the datatype of the member it reaches. Three generated rules of the same
names set them, behind the authored rules: "Het gewicht via vondst en weging
van een Jutter moet gesteld worden op het gewicht van de weging van de vondst
van de Jutter", and so on — a read over the declared roles `vondst` and
`weging`, the rule an author writes by hand today. The three folds range over
those members with `sourceRole` `PloegHeeftJutter.jutter`, and the
subselection "hij is actief" filters the jutters. The generated name holds the
declared role names; two or more steps are joined with "en", which no declared
name and no read in rule text can carry.

## Hand computation

A leeg element value drops out of a value fold; a maximum, an earliest date and
a sum over no filled value are leeg (RUNTIME-SEMANTICS ch. 1.2). A read over an
absent link is leeg (ch. 5.8).

`case1-een-ploeg-met-een-gedeelde-boot.json`: P has J1 (startdag 01-03-2026,
boot B1 of laadvermogen 100, vondst V1 of 10-03-2026 with weging 12), J2
(startdag 05-02-2026, boot B1, vondst V2 of 02-03-2026 with weging 7) and J3
(no startdag, boot B2 of laadvermogen 40, vondst V3 of 20-03-2026 with weging
30). J1 and J2 are actief, J3 is not.

- zwaarste gewicht: max over J1, J2 of 12 and 7 = **12** (J3's 30 is filtered out);
- eerste vinddag: earliest of 10-03, 02-03 and 20-03-2026 = **02-03-2026**;
- bootcapaciteit: per jutter, 100 + 100 + 40 = **240**;
- vlootcapaciteit: per boat, B1 and B2, 100 + 40 = **140**.

`case2-lege-stappen-en-een-lege-ploeg.json`: P2 has J4 (actief, no vondst, no
boot) and J5 (not actief, vondst V5 of 15-03-2026 with weging 9, no boot); P3
has no jutters.

- P2: zwaarste gewicht **leeg** (J4 reaches no weging, J5 is filtered out),
  eerste vinddag **15-03-2026**, bootcapaciteit **leeg**, vlootcapaciteit **leeg**;
- P3: all four **leeg**.

## Executors

Measured on 2026-10-02: ref replays the two cases exactly (6 and 8
expectations), clingo the same, and the Drools exec-model branch plays them
(`PLAYED 2`). The DRL-text branch refuses the document on its marker `TODO
aggregatie EERSTE` (the earliest vinddag over a set source). Every case refutes
the do-nothing executor, and twelve counter-cases (each one expectation moved:
a number by 0,01, a date by a day, a characteristic turned, an empty expectation
turned into a value) fail in ref, clingo and the exec branch. The IR from
`source.rgs` alone is byte-identical to the one written beside the cases. Before
the frontend lowered the form (uchar `921fcc64`), the three folds were markers
with the WARNING that "gewicht van de weging van de vondst" and its siblings
are no declared attribute of Jutter, and the subselection was refused with an
ERROR as dropped.
