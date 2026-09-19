# gesloten-body-golden: de vouwvorm met Let-body als corpusgetuige

Gemeten (asp-ch102-kaart, 2026-09-19): de gesloten lambdabody — een
vouw uit `jutterswet-kinderdeler`-vorm met body `Let m = grondslag / x
in (als x >= 0 dan acc plus 1,00 anders acc plus m)` — is verifier
PASS, ref PASS, clingo PASS, en BEIDE Drools-takken weigeren op de
loader (`Datatype:ListType (unknown datatype node)`). De golden kon in
die kaart niet landen omdat de benodigde `EXPECTED_CHANNEL`-regel
buiten het kanaaltellingsmandaat viel.

De regel die daarbij hoort, letterlijk uit dat Result
(`EXPECTED_PLAYED` blijft ONGEWIJZIGD — een nieuwe golden die niet
speelt is nooit meegeteld):

    "synth3-armlichaam|EXCLUDE|loader Refusal: Datatype:ListType — a
    closed lambda body needs a list-typed fold operand (WF-069), which
    the v0 loader does not carry; the form itself is pinned by ref and
    clingo"

## Acceptatie (bij oppakken aanscherpen)

- (a) De golden met handberekende verwachtingen, gespeeld door
  verifier, ref en clingo; beide Drools-takken EXCLUDE met de gepinde
  loader-reden.
- (b) De `EXPECTED_CHANNEL`-regel als gemandateerd
  beschermd-padvoorstel; alle veertien gates groen.

May touch: nieuwe goldenmap onder `corpus/golden/`,
`scripts/check-drools.sh` uitsluitend de kanaallijst (voorstel),
kaartverplaatsing.
