# ever-unique-temporal-key-bucket-scalar

The document of `ever-unique-temporal-key-scalar`, the union "eenmalig
vondstnummer" over Strandvondst and Wrakvondst with the key `vondstnummer`
on a year grid, with an evaluation date: `asOfDate` 2024-06-01. The
Strandvondst S1 gives its `vondstnummer` as the scalar 7, which the bucket
convention places in the year 2024 that holds the evaluation date
(RUNTIME-SEMANTICS ch. 8.4). Ever-uniqueness (ch. 5.9) claims every value
the timeline ever carries, and a scalar shows the value of one bucket, so
every executor faults with EF-020 (ch. 5.7b), `key` vondstnummer and
`label` S1.

Without an evaluation date, as in `ever-unique-temporal-key-scalar`, the
Drools backend refuses the scalar at the case input (DROOLS-051) and never
reaches the key; with one it reads the scalar into its bucket and the key
form rule of the uniqueness faults.
