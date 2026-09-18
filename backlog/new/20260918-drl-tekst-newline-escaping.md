# drl-tekst-newline-escaping: regeleinden overleven de DRL-tekst-tak niet

Vondst van de tekstlexeem-lensronde (2026-09-18), pre-existent en door
die kaart niet geraakt: een `Tekst`-waarde met een regeleinde speelt in
exec-model maar faalt in de classic-DRL-tak — `Namen.drlString`
escapeert `\` en `"`, maar geen CR/LF, dus het regeleinde bereikt de
gegenereerde DRL als letterlijke regelbreuk en de waarde vermindert
stil (`verwacht uit\nvoer, werkelijk uitvoer`; ref ≢ DRL). Een enkele
backslash als hele waarde geeft in de DRL-tak een luide
compile-weigering (`EXCLUDE drl compile`), geen fout antwoord — dat is
de eerlijke vorm. Lege string, kale `"`, `a\"b` en `pad\naar` (met
echte backslash) spelen in beide takken.

Geen corpusgetuige draagt deze vorm; de flank is met losse synthetische
modellen gemeten.

## Acceptatie (bij oppakken aanscherpen)

- (a) `drlString` escapeert ook regeleinden (en te bepalen: overige
  controltekens) zodat een tekstwaarde met newline in de DRL-tekst-tak
  dezelfde uitkomst geeft als exec, ref en clingo; dual-branchgetuige
  erbij.
- (b) De losse-backslashvorm wordt óf gedragen, óf blijft een luide
  per-vormweigering met reden — nooit een stil fout antwoord.
- (c) Alle veertien gates groen; kanaaltellingen ongewijzigd tenzij een
  nieuwe golden deze flank in het corpus pint (dan het gebruikelijke
  beschermd-padvoorstel voor de telling).
