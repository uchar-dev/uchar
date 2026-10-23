# anchor-hub-own-hops — the path of an anchored read is its own hops

IR-SPEC ch. 5.3: the path of a read is the ranges of the folds it sits in,
then its own hops; a read with the form anchor starts at the subject of the
form, so its path is its own hops alone. Written in fix round A of the card
`subselection-reads-subject` (2026-10-23), review A, point 5 (proof p19).

The model of `jutterswet-registervoorrang` with a second fact type, the switch
relation `vorige vergunning` (roles `vergunning` and `vorige vergunning`, both
to-one), and on the Vergunning a `tarief`, a `basis` and the derived `som` and
`eind`:

    Het eind van een Vergunning moet berekend worden als zijn basis plus
    zijn som.
    De som van een Vergunning moet berekend worden als de som van het tarief
    van alle vergunningen van het register van de Vergunning die aan de
    volgende voorwaarde voldoen: zijn tarief is kleiner dan het eind van de
    vorige vergunning van de Vergunning zelf.

`eind` reads `som` and `som` reads `eind`: a cycle. The read of `eind` is
anchored, with the `path` [vorige vergunning]: its path is that one hop over
the switch relation, a shifted read, so the cycle is an instance-ordered part
(IR-SPEC ch. 5.3) although the read stands in the filter of a sum over a hub
(the register). The verifier admits the document. Its mirror,
`corpus/negative/wf045-instance-cycle-hub-filter-without-anchor`, is the same
document without the anchor: there the read starts at the member, its path is
the range and then the hop, and the cycle is refused
(`WF-045.dependency-cycle`).

No `source.rgs`, by choice: the RegelSpraak frontend writes these sentences
with the same anchored read and its own hop (with the frontend of
`36be0e265`, measured in fix round A, verifier-clean), inside a one-part
composite and without the fallback 0, but fix round A wrote its goldens by
hand, from the proof of review A (p19), and routed none through the frontend
while review B read it. A `source.rgs` is an open point of that round.

## Hand computation

`case1-one-predecessor.json`: R1 holds V1 (tarief 10, basis 100), V2 (50,
200, vorige vergunning V1) and V3 (300, 0). V1 and V3 have no vorige
vergunning: the anchored read is leeg, the comparison fails for every member,
som **0** (the fallback), eind **100** and **0**. V2's vorige vergunning is
V1, whose eind is 100: V1 (10) and V2 (50) pass, som **60**, eind 200 + 60 =
**260**.

`case2-chain-of-two.json`: as case 1, with V3's vorige vergunning V2. V3
reads V2's eind, 260: V1 and V2 pass (V3's 300 does not), som **60**, eind 0 +
60 = **60**. The evaluation runs V1 before V2 before V3 (RUNTIME-SEMANTICS
ch. 6.2).

Both cases are consistent.
