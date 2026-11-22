# complete-input-spelling: de markering heet `(volledig gegeven)`

*Besluit van de eigenaar, 2026-11-22: de markering op een invoerlid dat volledig gegeven is, wordt `(volledig gegeven)` geschreven
in plaats van `(volledig)`.*

## Aanleiding

`(volledig)` zegt niet wát volledig is en leest op een bedrag als "het volle bedrag". De markering gaat over de invoer: wat de casus
niet geeft, is er niet. Overwogen en niet gekozen: `(leeg als niet gegeven)` (past niet op een kenmerk), `(mag ontbreken)` (zegt niet
dat ontbreken "er is er geen" betekent), `(gesloten)` (vakterm). Zij wordt niet geweigerd in een document zonder `Afleiden uit leeg:
nee`: zij zegt iets over de invoer, dat in de gewone lezing ook waar is, en declaraties kunnen gedeeld worden.

## Result

- Token `VOLLEDIG_GEGEVEN`, één token met zijn haakjes, de woorden door willekeurige witruimte gescheiden, zoals de
  opgavemarkering; `(volledig)` is geen token meer. Grammatica, resolver, RGS-464, `grammar/DIVERGENCES.md`, rij 49 van
  `spec/CONSTRUCTS.md`, de golden `jutterswet-volledig-gegeven` en zeven bronnen onder `backends/drools/src/test/resources/open-values/`.
  De wire houdt `complete`; geen `ir.json` verandert.
- Gemeten: `--ir` van de 425 bijgehouden RegelSpraak-bronnen vóór en na, elk met de eigen schrijfwijze: 0 verschillen in IR, uitvoer en
  exitstatus (339 schrijven een IR).
- `CompleteMarkerTest` draagt de schrijfwijze; de tokentest toetst erbij dat `(volledig)` en `(gegeven)` namen in haakjes blijven en dat
  een regelovergang binnen de markering mag.
