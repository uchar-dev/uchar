# jutterswet-emmerscalair-knip — the compared timeline of a gedurende clip as the asOfDate-bucket scalar

The wachtsom of a Jutter is the total of his vergoeding during the time that
his controle is greater than 5: a gedurende clip whose comparison reads the
controle per segment (RUNTIME-SEMANTICS ch. 3.6), so it reads that timeline
whole. J1 has a vergoeding of 10 a month through 2024, and the case gives
his controle as the asOfDate-bucket scalar 7 (asOfDate 2024-06-15), which
carries no period. A form that needs the full timeline and finds the bucket
scalar faults (ch. 8.4): the session faults with EF-037, attribute
`controle`, label `J1`, in the reference interpreter, the ASP backend and
both branches of the Drools backend. Read as no timeline, the clip would
have no true period and the wachtsom would be 0.
