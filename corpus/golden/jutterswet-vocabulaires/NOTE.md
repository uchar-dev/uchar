# jutterswet-vocabulaires — two named vocabularies on every designatable kind

The golden of the vocabulary side table (`spec/IR-SPEC.md` ch. 2.12,
WF-116 to WF-120) and of the RegelSpraak dialect that writes it
(`frontends/regelspraak/grammar/DIVERGENCES.md`, "Vocabulaires en
benamingen"). `source.rgs` declares two vocabularies:

    Vocabulaire handboek (taal: nl)
    Vocabulaire cgm (standaard) (taal: nl)

and gives every declaration a designation in both, at the end of its name
group: the two object types, their six attributes and two characteristics,
the fact type and its two roles, the parameter, the three values of the
enumeration `Vondstsoort`, the five rules and the rule group — 23
declarations, the count the Drools backend's `VocabularyWitnessTest` pins.
`cgm` is the default vocabulary, so it names every one of them (WF-119);
its designations are written in capitals with underscores, which is content
and no rule: a designation is text in every vocabulary (maintainer decision
2026-09-27).

## The redundant designation, and the warning that accounts for it

The attribute `naam` of `Jutter` carries `naam` in `handboek`, its own
canonical name. The frontend writes the document with this warning, which
stands in `diagnostics` (real output of the emitted document):

    de benaming 'naam' van het attribuut 'naam' van 'Jutter' in het
    vocabulaire 'handboek' spelt haar canonieke naam en is overbodig: de
    effectieve naam is dezelfde zonder haar (WF-120)

That warning quotes the designation, which is how the document accounts
for it (WF-120), and the verifier answers `verifier: PASS`. Without the
warning the same document is the negative `wf120-redundant-designation-without-warning`.
In `cgm`, the default, the designation `NAAM` is required and never
redundant.

## The rules compute, and never through a designation

Every rule is written in canonical names; the designations change no
answer. The pins, hand-computed:

`case1`, premietarief 3:

| | soort | gewicht | premie (gewicht × 3) | waardevol |
|---|---|---|---|---|
| V1 | barnsteen | 3.00 | **9.00** | **yes** |
| V2 | wrakhout | 1.20 | **3.60** | no |
| V3 | flessenpost | 0.40 | **1.20** | no |

| | vondsten | vondstbedrag (sum of premie) | aantal waardevolle vondsten | premiewaardig (> 10) |
|---|---|---|---|---|
| J1 | V1, V2 | **12.60** | **1** | **yes** |
| J2 | V3 | **1.20** | **0** | no |
| J3 | — | **0** (the "of 0 als die er niet zijn" fallback) | **0** | no |

`case2-drempel`, premietarief 5: V4 and V5 are barnsteen of 1.00 (premie
**5.00** each, both waardevol), V6 is flessenpost of 2.50 (premie **12.50**).
J4 holds V4 and V5: vondstbedrag **10.00**, which is not greater than 10, so
J4 is **not** premiewaardig, with **2** waardevolle vondsten; J5 holds V6:
**12.50**, premiewaardig, **0** waardevolle vondsten. The edge at exactly 10
is the row that catches a `groter of gelijk` misreading.

## The square and the neutrality leg

`ref` and the ASP backend replay both cases (`oracle: PASS (16
expectations)`, `clingo: PASS (16 expectations)`; 13 for `case2-drempel`),
and both Drools branches play them (`PLAYED 2`). Gate 13 replays the golden
three more times per executor: with every designation stripped, with the two
vocabularies exchanged, and with the designations rotated between the
declarations of each scope; every executor gives the untouched answer.
