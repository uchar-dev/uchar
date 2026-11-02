# jutterswet-tijdsevenredig-past-niet — a pro-rata segment that does not fit its target

The document of `corpus/golden/synth2-prorata-breed` with only its rule on the `ligdeel`:
the tijdsevenredig deel per maand of the yearly `jaarforfait` of a Ligplaats during the time
it is `bezet`, into a monthly `ligdeel` of 2 decimals. L1 has a jaarforfait of 15 and is bezet
from 2025-02-11 to 2025-04-01. The segment of February is 15 × 18/28 = 135/14, which does not
fit 2 decimals: the write boundary holds per segment of a timeline target (EF-006,
RUNTIME-SEMANTICS ch. 2.4 point 1, ch. 3.7, ch. 5.7b), place `L1.ligdeel`, value 135/14,
decimals 2, in the reference interpreter and the Drools backend. The ASP backend emits the code
without parameters (its sentence names none of them), so the pin is `{}`. The segment of March
is 15 and fits; with the golden's case, bezet from 2025-02-08, February is 15 × 21/28 = 45/4 =
11,25 and the document plays.
