# test-documents-symbol: de testdocumenten schrijven het symbool zoals de emitters

*Open punt van `backlog/done/20261107-unit-symbol.md`; opdracht van de eigenaar, 2026-11-07.*

Goal: vier inline testdocumenten schreven `€` nog als eerste spelling van `EUR`
(`"spellings": ["€", "euro", "euros"]`), de vorm van vóór `DeclaredUnit.symbol`. Geldige draad, maar geen emitter
schrijft hem nog, en wie zo'n document als voorbeeld overneemt, schrijft het symbool weer als spelling. Ze schrijven nu
`"symbol": "€", "spellings": ["euro", "euros"]`, in hun eigen escaping.

Acceptance: `git grep` vindt geen document meer met `€` in `spellings`; geen bewering verandert (geen van de toetsen gaat
over eenheden); `make gates` groen.

May touch: `ref/test/test_ref.ml`, `backends/clingo/test/test_clingo.ml`,
`backends/drools/src/test/java/dev/uchar/regelspraak/cases/ReaderUnderWriterWitnessTest.java`, deze kaart.

## Result

Vier vervangingen: `test_ref.ml` (r. 4224, `€`), `test_clingo.ml` (r. 6560 met `€` en r. 9948 met `€`) en
`ReaderUnderWriterWitnessTest` (r. 419). `git grep` op `€` of `€` als spelling vindt daarna niets meer.
