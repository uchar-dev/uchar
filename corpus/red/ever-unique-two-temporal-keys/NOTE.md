# ever-unique-two-temporal-keys

A uniqueness-union source whose compound key carries TWO temporal
components. Ever-uniqueness (RS ch. 5.9, decided 2026-08-20) pairs the
scalars with every value the ONE temporal component ever carries; with
two temporal components the tuple needs the same-moment join (which
values were carried TOGETHER), and no corpus case pins that reading —
so both executors MUST refuse loudly rather than guess: a cartesian
pairing of values from different moments would silently claim tuples
the instance never carried.
