# jutterswet-verdeling-dubbelbezet — twee verdelingen over dezelfde ontvangers, met een lege weging

Twee verdelingen (`RUNTIME-SEMANTICS.md` ch. 4) delen één bron en één
verzameling ontvangers: de opbrengst van de Berging gaat via het feittype
`bergingsdeelname` naar dezelfde drie Bergers, eenmaal naar rato van het
gewicht (`bergingsaandeel`, rest in `onverdeelde rest`) en eenmaal op
volgorde van het jaar, bij een gelijk jaar gewogen naar het gewicht
(`rangaandeel`, rest in `rangrest`). Elke verdeling rekent voor zich; de
ene leest niets van wat de andere schrijft. Berger D2 heeft geen gewicht,
en de twee methoden behandelen die lege weging verschillend (ch. 4.2 en
4.4): naar rato valt D2 buiten de verdeling en blijft zijn aandeel leeg,
op volgorde krijgt D2 in zijn jaargroep een echte 0. In beide gevallen is
de lege weging een §9.5-fout van die verdeling op D2, die het kanaal als
inconsistentie toont.

## De handberekening

Opbrengst 1000; elke Berger heeft maximum 600; afronding op 0 decimalen.

| berger | gewicht | jaar | naar rato | op jaar |
|---|---|---|---|---|
| `D1` | 3 | 2019 | 1000 × 3/4 = 750, begrensd op **600** | groep 2019 alleen: 1000 × 3/3, begrensd op **600** |
| `D2` | leeg | 2020 | buiten de verdeling: **leeg** + fout | groep 2020: gewicht telt als 0: **0** + fout |
| `D3` | 1 | 2020 | 1000 × 1/4 = **250** | groep 2020, pot 400: 400 × 1/1 = **400** |
| rest | | | 1000 − 600 − 250 = **150** | 1000 − 600 − 400 = **0** |

Naar rato is de som van de gevulde gewichten 3 + 1 = 4. Wat het maximum
van D1 afkapt, gaat naar de rest en niet naar een andere ontvanger. Op
volgorde krijgt de groep van 2019 eerst de hele pot; wat D1 niet mag
hebben (400) blijft over voor de groep van 2020, die het onder elkaar
deelt naar het gewicht, met de lege weging van D2 als 0.

De case pint de zes aandelen, de twee resten en beide inconsistenties
op D2, één per verdeling.

## Herkomst

Eigen werk (kaart `drl-teksttak-op-truth-maintenance`, stap 5), deel B van
de getuige `TmsVerdelingWitnessTest` van de Drools-backend, als document
geschreven door de generator van die getuige. Daarom draagt het model ook
attributen die geen regel van dit document leest. Er is geen bron in
RegelSpraak: de IR is met de hand opgesteld. De vorm komt uit kaart
`drl-verdeling-dubbelbezet`: de vroegere, destructieve DRL-teksttak faalde
op deze case. Gemeten 2026-10-14 bij de toelating: verifier PASS; ref en
clingo spelen de case met de tien verwachtingen; beide takken van de
Drools-backend PLAYED 1.
