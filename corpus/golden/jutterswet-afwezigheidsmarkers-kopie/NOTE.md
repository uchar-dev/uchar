# jutterswet-afwezigheidsmarkers-kopie

The copy of a timeline of a sum type (`spec/RUNTIME-SEMANTICS.md` ch. 9.4, 9.5), a document written by hand: the
RegelSpraak dialect gives every attribute with markers a domain of its own, so a source cannot copy one into another
(`frontends/regelspraak/grammar/DIVERGENCES.md`, "Afwezigheidsmarkeringen"), and the copy is a form of the IR only. A
Vondst has three attributes for every month of the sum type over the domain `opslagkosten van een Vondst`, with the
constructors `waarde` (a whole number), `onbekend` and `niet van toepassing`: `opslagkosten`, `opslagkopie` and
`verkoopkopie`. The rules, the first two those of the golden `jutterswet-afwezigheidsmarkers`:

- "opslag onbekend in de winter": the opslagkosten are `onbekend` from 01-01-2025 to 01-03-2025;
- "opslag niet van toepassing na verkoop": the opslagkosten are `niet van toepassing` while the Vondst is verkocht;
- "opslagkopie": the opslagkopie is the opslagkosten, an own timeline of the same sum type read whole and written
  segment by segment (ch. 3.4, 3.11);
- "verkoopkopie": the verkoopkopie is the opslagkosten while the Vondst is verkocht, the copy cut to the periods of its
  condition (ch. 3.10).

The cases are ref's fixture `copy-of-a-timeline` (`ref/test/fixtures/absence-markers`) and two beside it. With ref,
clingo and both branches of the Drools backend:

| case | input | opslagkosten and opslagkopie | verkoopkopie |
|---|---|---|---|
| 1 | opslagkosten `waarde(10)` on 2025 | `onbekend` on [2025-01-01, 2025-03-01), `waarde(10)` on [2025-03-01, 2026-01-01) | |
| 2 | as 1, verkocht on [2025-06-01, 2026-01-01) | `onbekend`, `waarde(10)` on [2025-03-01, 2025-06-01), `niet van toepassing` on [2025-06-01, 2026-01-01) | `niet van toepassing` on [2025-06-01, 2026-01-01) |
| 3 | opslagkosten `waarde(10)` on [2025-04-01, 2025-08-01), opslagkopie `waarde(4)` on 2025 | opslagkosten `onbekend` on [2025-01-01, 2025-03-01) and `waarde(10)` on [2025-04-01, 2025-08-01); opslagkopie the same and `waarde(4)` from its input on [2025-03-01, 2025-04-01) and [2025-08-01, 2026-01-01), the days its source is leeg | |
