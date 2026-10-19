# naming-cleanup: het hernoemgereedschap gaat weg, de conventie blijft

*Laatste stap van het programma "Engelse namen" (besluit van de eigenaar 2026-10-18: na stap 8 en de syncronde
blijven alleen de conventie en `.git-blame-ignore-revs`). Raakt `scripts/` en `CLAUDE.md`; mandaat van de eigenaar.*

Goal: `scripts/lib/naming/` (het hernoemgereedschap met zijn tabellen, beslislijsten, bevroren lijsten en
woordenlijst) verdwijnt uit de boom; de git-historie bewaart het. `CLAUDE.md` verwijst er niet meer naar en noemt in
plaats van de woordenlijst de bron van de termen en een handvol vaste keuzes. `.git-blame-ignore-revs` blijft, met
een kop die niet meer naar een bestaand script wijst.

Acceptance:
- (a) `git ls-files scripts/lib/naming` is leeg; geen getrackt bestand buiten `backlog/done/` noemt `lib/naming`.
- (b) Geen gate leest de map (gemeten: geen verwijzing in `scripts/*.sh`).
- (c) Alle 16 gates groen op de eindstand.

Gate set: alle gates van `CLAUDE.md`, groen op de eindstand van de branch.

Result (2026-10-19):
- `ead9bcc2` haalt `scripts/lib/naming/` weg (30 bestanden: `rename.py`, de tests, de woordenlijst, de tabellen, de
  beslislijsten en de bevroren lijsten). `CLAUDE.md` (Conventions) noemt nu de bron van de termen (de draad, de Engelse
  spectekst) en vier vaste keuzes; `.git-blame-ignore-revs` blijft, met een kop zonder verwijzing naar het script.
- (a) `git ls-files scripts/lib/naming` is leeg; buiten `backlog/` noemt geen getrackt bestand `lib/naming`. (b) Geen
  gatescript las de map. (c) Volledige gateset 16/16 groen op `ead9bcc2` (12:22-12:40).
- In de distributie haalde de kaart `naming-cleanup` (main `957f7ec`) haar `tools/naming/` weg; `make gates` groen.
