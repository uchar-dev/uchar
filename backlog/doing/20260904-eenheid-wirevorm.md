# De halfgevulde eenheidswirevorm wordt ill-formed; ref verliest zijn factor-terugval

Goal: de kaart `20260904-eenheid-terugval-failfast` mat twee open einden
(haar §8): (a) de wirevorm `canoniek` gevuld + `factorNaarCanoniek: null`
is schema-geldig (beide velden nullable), glipt door `convertibleNaar`
heen en betekent niets — gemeten dracht nul over alle 204 `ir.json` in
`corpus/golden`, `corpus/rood` en `toka/` (32 distincte eenheidsvormen,
geen enkele halfgevuld); de spiegelvorm (factor zonder canoniek) is even
betekenisloos en even dracht-nul. Een WF-regel (WF-092 — volgende vrije
nummer, check de index) maakt beide halfvormen ill-formed: `canoniek` en
`factorNaarCanoniek` zijn SAMEN gevuld of SAMEN null, recursief door de
noemer waar de eenheidsstructuur er een draagt. Dan valt de fout bij
verificatie, vóór elke executor. (b) `ref/lib/arith.ml:37` draagt
letterlijk dezelfde stille terugval die de frontend-kaart in Java
wegnam: `None -> Q.one`. Gemeten op een sleepbaan-variant met
`canoniek: "m"` en `factorNaarCanoniek: null` op `km`: de verifier laat
de vorm vandaag door, ref rekent met factor 1 (605/2 = 302,5) en het
Drools-pad gooit — het orakel is op deze vorm de toegeeflijke. De
terugval wordt een luide evaluation fault met de eenheidsnaam erin;
na WF-092 is dat de tweede verdedigingslinie, geen dode letter — een
executor rekent nooit stil op een vorm die de verifier afkeurt.

Meetvraag die erbij hoort: draagt `backends/clingo` (de
eenheidsvertaling in `translate.ml`) dezelfde stille factor-terugval?
Meet op dezelfde sleepbaan-variant; zo ja, dan is de symmetrische
fail-fast daar kaartwerk van dezelfde klasse (gemelde verruiming), zo
nee, dan staat de gemeten afwezigheid in Results.

Acceptance:
- WF-092 in `spec/IR-SPEC.md` (voorstel) + verifier-clausule (voorstel)
  + twee negatiefs (één per halfvorm), rood-eerst zichtbaar,
  clausule-exact mutant-bewezen, nul over-afwijzing over de vier zones.
- De ref-terugval vervangen door een luide fault; de sleepbaan-meting
  vóór (302,5) en ná (fault) letterlijk in Results; geen corpusuitkomst
  beweegt (dracht nul, gates 5/6 op gelijke tellingen).
- De clingo-meting gedaan en beantwoord (fix of gemeten afwezigheid).
- Alle dertien gates groen.

May touch: `spec/verifier/**` (voorstel), `spec/IR-SPEC.md` (voorstel),
`corpus/negatief/**`, `ref/lib/**`, `backlog/`;
`backends/clingo/lib/**` alleen als de meting dezelfde terugval
blootlegt (gemelde verruiming, zelfde klasse).

Out of scope: de frontends (Eenheid.java is al luid) en de
Drools-backend (gooit al); de kale vorm — canoniek én factor beide null
— blijft legitiem (28 corpusvoorkomens: de WF-038-tijdsduurparameters
en WF-035-duuroperanden).
