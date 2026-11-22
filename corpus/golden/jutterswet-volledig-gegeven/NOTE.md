# jutterswet-volledig-gegeven — members whose input is complete

The RegelSpraak marker `(volledig gegeven)` (grammar/DIVERGENCES.md, "Volledig gegeven") in a document with
`Afleiden uit leeg: nee`. The frontend writes the marker as `complete: true` on the member's declaration in `ir.json`
(IR-SPEC ch. 2.2), and on the two attributes an opgave marker generates, `opgegeven bergloon` and `berekende bergloon`;
gate 10 re-emits `ir.json` from `source.rgs` byte for byte. The document is read as RUNTIME-SEMANTICS ch. 1.5 lays
down: an input member is filled, settled leeg or open, and a member declared complete is settled leeg where the case
gives it no value, or stands not to hold where the case does not name it.

A Vondst with two pairs of input members, each pair one member declared complete and one without the marker, under
another name: the attributes `toeslag` (complete) and `bijslag`, and the characteristics `drijfhout` (complete) and
`wrakhout`. Each is read by a rule of its own:

- `uitbetaling`: zijn taxatie plus zijn toeslag;
- `bijbetaling`: zijn taxatie plus zijn bijslag;
- `drijftoeslag`: 2 indien hij niet drijfhout is;
- `wraktoeslag`: 2 indien hij niet wrakhout is;
- `bergloon`: zijn gewicht maal 2, under the opgave marker `het bergloon (mag opgegeven worden)`: the stack
  `opgave boven berekening bergloon` with the opgave above the computation, the deviation characteristic
  `afwijkende opgave bergloon` and the consistency `controle opgave bergloon` (dialect §7).

The expectations, computed by hand. "settled leeg" is an `attributeEmpty` expectation, "stands not" a
`characteristicAbsent`, "open" an `attributeOpen` or `characteristicOpen`.

| case | input | the members read | the reader |
|---|---|---|---|
| case1 | taxatie 50, bijslag 5 | toeslag settled leeg | uitbetaling 50, bijbetaling 55 |
| case2 | taxatie 50, toeslag 5 | bijslag open | uitbetaling 55, bijbetaling open |
| case3, V1 | nothing named | drijfhout stands not, wrakhout open | drijftoeslag 2, wraktoeslag open |
| case3, V2 | drijfhout and wrakhout named | both hold | drijftoeslag and wraktoeslag settled leeg |
| case4 | gewicht 10, opgave 30 | — | bergloon 30, berekende bergloon 20, afwijkende opgave holds, the §9.5 fact of the consistency (EF-025) |
| case5 | gewicht 10 | opgegeven bergloon settled leeg | berekende bergloon 20, bergloon 20, afwijkende opgave stands not |
| case6 | nothing | opgegeven bergloon settled leeg, gewicht open | berekende bergloon open, bergloon open, afwijkende opgave stands not |

case1: the toeslag is declared complete and the case gives it no value, so it is settled leeg, and the plus counts it
as 0 (ch. 1.2): the uitbetaling is 50. The bijslag is given, and the bijbetaling is 55.

case2: the bijslag is not declared complete and the case gives it no value, so it is open, and so is the bijbetaling
that reads it (ch. 1.5, "An open read"). The toeslag is given: the uitbetaling is 55.

case3: drijfhout is declared complete and V1's case does not name it, so it stands not to hold; "niet drijfhout" is
established true and the drijftoeslag is 2. Wrakhout is not declared complete: not named, it is open, "niet wrakhout"
is undetermined, and the wraktoeslag is open. V2 names both: they hold, "niet …" is established false for another form,
and both rules fill nothing, so both toeslagen are settled leeg.

case4: the opgave is given, so the highest default of the stack decides: the bergloon is 30. The computation is
`berekende bergloon`, 20. The opgave and the computation are both filled and differ, so `afwijkende opgave bergloon`
holds and the consistency, which asserts that it does not, deposits its fact.

case5: the opgave is declared complete (the frontend writes it so for every opgave) and the case gives it no value, so
it is settled leeg: the opgave default holds with a settled leeg value and gives way (ch. 1.5, "A stack"), and the
computation decides 20. The first member of the deviation's condition, "zijn opgegeven bergloon is gevuld", is
established false, so `afwijkende opgave bergloon` stands not to hold, and the consistency holds.

case6: as case5, but without a gewicht: the computation reads an open gewicht, so `berekende bergloon` is open, and the
computation default below the opgave that gave way is open: the bergloon is open. The deviation stands not to hold, as
in case5: its first member is established false.

Without the field — the same document with `complete` stripped, measured — cases 1, 3, 5 and 6 differ: the toeslag,
drijfhout and the opgave are open, and so are the uitbetaling, the drijftoeslag, the bergloon of case5 and the
deviation of cases 5 and 6; cases 2 and 4 do not read a member declared complete without a value.

The reference interpreter plays the six cases (gate 5), and so do both Drools branches (gate 12). The ASP backend refuses
the document with CLINGO-049 (gate 6), and the Drools build with readings with DROOLS-370 (gate 17), as
`jutterswet-afleiden-uit-leeg`: those gates list the golden by name.
