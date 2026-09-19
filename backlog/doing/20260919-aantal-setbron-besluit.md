# De AANTAL-setbron: de wire weigert de vorm (besluit genomen)

**Maintainersbesluit 2026-09-19: optie (ii), met een expliciete
scheiding van betekenissen.** De huidige AANTAL-setbron-vorm wordt
geweigerd; `AANTAL` blijft conform de normatieve RegelSpraak-spec
§5.8.1 en `RUNTIME-SEMANTICS.md` ch. 3.4a/3.4b INSTANTIES tellen —
drie vondsten blijven drie, ook als één gewicht leeg is. De
wire-vorm mag de twee betekenissen (ledentelling en waardentelling)
niet vermengen. Voor de waardentelling en het gemiddelde komt een
afzonderlijke, normatief gedefinieerde operatie: eigen kaart
`20260919-aantal-gevulde-gemiddelde.md`.

De meting die het besluit droeg (kaart eerste-laatste-setvouw-kloof
§3/§6, door de lens onafhankelijk gereproduceerd): `ref` telt gevulde
waarden, Drools telt leden — 2 tegen 3 zodra één lid het attribuut
mist — de ASP-backend weigert, en er is geen producentroute (de
frontend weigert de natuurlijke zin met haar eigen WF-096-melding).

Uitvoering (route ii): de WF-093/WF-096-exemptie versmalt tot
"arity 1 ∧ een WAARDE-vouwende soort" (AANTAL eruit); `ref`'s
setbron-arm en de Drools-lowering worden onbereikbare code voor
welgevormde documenten; ch. 3.4a/3.4b blijven staan zoals ze staan.

Acceptance: rood-eerst — het negatief per spelling met de
verifier-md5 vóór en ná, plus een arm-exacte mutant; de corpussweep
vóór/ná (verwacht nul beweging: geen golden draagt de vorm); de
"awaiting the maintainer decision"-passages in `spec/IR-SPEC.md`
(WF-096-exemptie-alinea, WF-102-COUNT-bullet),
`spec/RUNTIME-SEMANTICS.md` (ch. 3.4b gevolg 2), `spec/CONSTRUCTS.md`
rij 1 en `spec/ASP-TRANSLATION.md` (catch-all) worden in het
registervoorstel bijgewerkt naar het genomen besluit; alle veertien
poorten groen.

May touch: `spec/IR-SPEC.md` en `spec/verifier/**` als VOORSTEL
(beschermde paden), `corpus/negatief/**`, kaartverplaatsing.

Out of scope: EERSTE/LAATSTE — beslecht 2026-09-19, route (i)
gebouwd. De nieuwe waardentelling/GEMIDDELDE-vouw — eigen kaart.
`frontends/**` (de WF-096-meldingstekst belooft `aantal` als
vouwsoort en verdient een herformulering zodra deze kaart landt —
dat is een frontend-klusje bij de landing, zoals bij subselectie-4a).
