# strict-empty — the witnesses of a document that infers nothing from an empty value

A document per form of RUNTIME-SEMANTICS ch. 1.5, each with `inferFromEmpty: false` and case files whose expectations
are computed by hand; the suite of the reference interpreter plays every case (`WV-1.5-witness-*`), and the documents
come from `spec/proposals/STRICT-EMPTY.md` §5. A case file says every state itself: `attributeEmpty` and
`characteristicAbsent` where a member is settled leeg, `attributeOpen` and `characteristicOpen` where it is open.

Over an open value a test on leeg or gevuld is undetermined, as every condition that reads one is; so is a filter of a
fold that tests on gevuld, and the filled-value count is open (ch. 1.5, "The test on leeg and gevuld", "A fold over a
set"). An initialisation is the lowest writer of its target: below an open opgave or a computation that is open it
does not decide. The cases where that sets the outcome:

| witness / case | expectation | why |
|---|---|---|
| `composite-filled-test` case2, case3, case5 | getuigentoeslag and vindloon open | "zijn gewicht gedeeld door zijn aantal getuigen leeg is" reads the open gewicht or getuigen, so the test is undetermined: the rule leaves the getuigentoeslag open, and the deviation 9 of the vindloon is open, so its stack is |
| `filled-test` case3 | vrijgesteld and opslagtoeslag open | "zijn gewicht is leeg" and "zijn gewicht is kleiner dan 10,00" are both undetermined over the open gewicht, so vrijgesteld is open, and so is the opslagtoeslag "indien hij niet vrijgesteld is" |
| `filled-test-not-closure` case1 | ongetaxeerd, taxatietoeslag and taxatiekosten open; consistent | "zijn taxatie leeg is" is undetermined over the open taxatie; the two toeslagen read ongetaxeerd; "de taxatie moet gevuld zijn" reports nothing over an open taxatie |
| `filled-test-not-closure` case2 | ongeschat, schattingstoeslag and schattingskosten open; consistent | the same over the schatting, open since the waarde it is computed from is open |
| `input-above-computation` case3, case4 | bergloon open | the opgave is complete and gives way; the computation "zijn gewicht plus zijn schade" reads an open value, so it is open, and the initialisation 25 below it does not decide |
| `input-above-computation-open` case1 | bergloon open | the opgave is open, and no default below it decides, the initialisation 25 among them |
| `set-fold` case2 | gewogen aantal open | the filled-value count reads the gewicht of each vondst, and one is open |

`let-closure` is the document of `WV-1.5-filled-count-in-a-binder-*`: a binder computed as a filled-value count is
open where a gewicht is, and so is every condition that reads it. `initialisation` has its initialisation "indien hij
niet gemeld is", which does not decide where gemeld is open.

`defaults-initialisation-only` has two stacks whose one default is their initialisation, "op 5" for the strandloon and
"op 3" for the meldloon: such a stack makes no member derived (ch. 1.5, "The initialisation of a stack"), so both are
input. Without a value the strandloon is open, and the meldloon, declared complete, is settled leeg above its
initialisation, which gives 3; with the values 7 and 4 in the case they are 7 and 4.

`defaults-initialisation-exception` has one stack whose one default is its initialisation "op 5", with the exception
"op 8 indien hij gemeld is": the exception decides above the input (ch. 7.9), so the stack makes the strandloon derived
(ch. 1.5, "The three states"). It is 8 where gemeld holds, also with 7 in the case; 5 where gemeld stands not to hold and
the case gives no strandloon, 7 where it gives 7; and open where gemeld is open, with or without 7 in the case.

`contributor-filter` does not come from `spec/proposals/STRICT-EMPTY.md`: it is the worked example of the filter of a
contributor in RUNTIME-SEMANTICS ch. 5.12 (`WV-5.12-contributor-filter`), its IR emitted by the RegelSpraak frontend.
Four quantifier folds `alle` over the vondsten of J1, each with a compound filter: "gemerkt of drijfhout" and "gemerkt en
drijfhout" over "groter dan 10,00", "gemerkt of een gewicht groter dan de keurgrens" over the same, and "gemerkt of
drijfhout" over "groter dan de keurgrens". `red1-filterfout-na-open-filter.json` pins the fault of the third filter on V2
after V1's undetermined one (EF-014); the witness loop reads only the `case*` files, and the readings test reads it.
