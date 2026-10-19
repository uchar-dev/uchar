# catalogue-gaps: Nederlandse zinsneden uit Java-hulpmethoden komen uit de catalogus

*Vervolg op `message-catalogues` (open punt 3 van haar Result); op verzoek van de eigenaar (2026-10-19).*

Goal: sinds `message-catalogues` komt de tekst van een melding uit een catalogus (NL en EN), maar een aantal hulpmethoden
(`RuleChecker.blockHead/kindText/what/designation/targetText/baseReason/rowTable`, `ExpressionBuilder.unionRefusal`,
`countAttributeCollision`, `countOverObjectType`, `walkRange`, `stepRefusal`, `rollen`, `smallerUnit`, `Rational.reason`, en
in Drools wat het meting vindt) geeft een Nederlandse zinsnede als parameterwaarde mee, zodat een Engelse weergave
Nederlandse stukken bevat. Die zinsneden komen uit de catalogus.

## De regel van de eigenaar

Wat zijn oorsprong in RegelSpraak heeft, blijft Nederlands; de rest wordt Engels.

- Een RegelSpraak-sleutelwoord dat de melding letterlijk uit de bron van de gebruiker aanhaalt, blijft staan in beide
  talen (`Afwijking van`, `Uitzondering op`, tokens van de grammatica).
- Een gedeclareerde naam blijft staan.
- Een Nederlandse zinsnede die proza van de melding is, wordt een catalogusentry (een deel, zoals `RGS-108.member`)
  met een Nederlandse en een Engelse tekst.

## Wat blijft

- De 29 Drools-codes die in de Nederlandse catalogus Engels blijven.
- Codes, parameternamen, tests en hun verwachtingen; `git diff main -- spec/ corpus/` en de testbestanden blijven leeg.
- Ongecodeerde meldingen.

## Acceptance

- Elke zinsnede uit `MEASURE.md` die proza is, staat als deel in beide talen; de rest staat met reden in het Result.
- Onder `uchar.messages.locale=nl` is elke melding byte-identiek aan main (suite- en corpusmeldingen).
- Geen testverwachting verandert.
- Alle gates van `CLAUDE.md` groen op de eindstand (één volledige run).

Gate set: alle 16 gates.

## Result

Gebouwd op main `3117f770f`, branch `card/catalogue-gaps` (commit `d81bf3e20`: de bouw; deze Result erna). Niet gepusht. De
meting staat in `werk/engelse-namen/na/gaps/MEASURE.md`.

**Omgezet.** De lijst uit open punt 3 van `message-catalogues` bleek niet de hele verzameling: de Engelse weergave van de suite, de
corpus en de Drools-statusregels liet meer Nederlandse stukken zien. Omgezet naar delen (`PART.*`, `RGS-092.*`, `RGS-132.*`):
- frontend: `rowTable`, `what`/`designation`/`kindText`, `targetText`, `baseReason`, `cellText`, `against`, de `under`/`has`/`form`
  van RGS-138/141, `Input.head`, de vormnamen, de 13 redenen van RGS-092 (de `reason` van `Expr.Unresolved` en
  `Condition.Unresolved` is nu een `Object`, een `Msg`, omdat testen er een tekenreeks in zetten), de redenen van RGS-210/212/213,
  `unionRefusal`, `countPath`, `countOverObjectType`, `countAttributeCollision`, `walkRange`/`stepRefusal`/`rollen`, `Rational`,
  de hints van RGS-028, de soorten van RGS-007/032 en de `what`/`where`/raster/periode-stukken van de scenariomeldingen;
- Drools: de `description` van 25 plekken, de rollen van een verdeling, `Origin.description`, de redenen van `RangeJoin`,
  `MembershipStorage`, `AllocationJoin`, de zin van `noJoinFieldReason`, `Cycles` (namen, vormen, ontkenning; gesorteerd op de
  Nederlandse tekst, zodat de volgorde in beide talen gelijk is) en tien `Reason.text()` die nu `Reason.msg()` zijn, zodat een
  geneste reden ook met een expliciete `Locale` in de taal van de omringende zin rendert.
Nieuw in `Msg`: `Msg.Joined` (een lijst met een eigen scheiding, voor de ", " van RGS-106 en de "; " van de Drools-cycli).
Aantallen: frontendcatalogus 515 naar 652 entries per taal, Drools 365 naar 425.

**Blijft, met reden** (volledig in `MEASURE.md`): `blockHead` en de andere RegelSpraak-sleutelwoorden die de melding uit de bron
aanhaalt (`Afwijking van`, `Uitzondering op`, `geen van de`, de gespelde negaties, `(mag opgegeven worden)`), de declaratiewoorden
van RGS-005/008/029/030, de namen van ingebouwde functies, `gelijk`, `vanaf`, datatypenamen en de Tabel 16-soorten; gedeclareerde
namen en brontekst; alle meldingen zonder code (uitzonderingen, `UnitConflict`, EF-zinnen, `Watch`, `DroolsBackend`); de sleutel van
`Coverage` ("regel: naam"), die de distributie leest. De 29 Drools-codes die in de Nederlandse catalogus Engels blijven, zijn niet
aangeraakt.

**Byte-gelijkheid onder `nl`.** Frontend: de haak in de constructor van `Diagnostic` (niet in de commits) schreef 1127 meldingen van de
suite en 146 regels van de corpus-run weg, op de bouw en op de branch; `cmp` leeg voor beide. Drools: de statusregel van elke golden
en elke rode case (300 regels, `-Duchar.messages.locale=nl`) op main en op de branch; `diff` leeg. Er is geen testbestand
gewijzigd (`git diff main -- spec corpus scripts frontends/regelspraak/src/test backends/drools/src/test` is leeg).

**Engelse uitvoer, voor en na** (zelfde invoer):
- RGS-092: `unresolved: de naam 'alle Vondsten' is geen gedeclareerde parameter, geen variabele uit een 'Daarbij geldt:'-deel en geen
  attribuut van het onderwerp (§11/§13.3.8) — …` wordt `unresolved: the name 'alle Vondsten' is no declared parameter, no variable from
  a 'Daarbij geldt:' part and no attribute of the subject (§11/§13.3.8) — …`;
- RGS-137: `'bevoegd bij noodweer' is een afwijking, not a base rule` wordt `… is a deviation, not a base rule`;
- RGS-313: `(regel 'x' is een verdeling/consistentie/objectcreatie/uniciteit, geen platte regel)` wordt `(rule 'x' is an
  allocation/consistency/object creation/uniqueness, not a flat rule)`;
- DROOLS-067: `de rol 'geborgen vondst' op Boot is meervoudig, dus haar opslag is het veld aan de overzijde` wordt `the role
  'geborgen vondst' on Boot is multi-valued, so its storage is the field on the far side`.

**Gates.** Alle 16 gates groen op `d81bf3e20`, één volledige run (`failed=0`; `werk/engelse-namen/na/gaps/gates-1.summary.txt`): gate 10
`1210 tests` (vloer 173) en `347 codes, 363 sites`, gate 12 `JUnit 1622 tests` (vloer 1573) en de Drools-catalogusleg. De Nederlandse
-woordencontrole van de scripts vroeg geen uitzondering: een sleutelwoord in een Engelse entry staat tussen aanhalingstekens.
`scripts/` is niet gewijzigd.
