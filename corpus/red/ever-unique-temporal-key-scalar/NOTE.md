# ever-unique-temporal-key-scalar

An ever-unique temporal key supplied as a bucket SCALAR instead of the
full timeline. Ever-uniqueness (RS ch. 5.9, decided 2026-08-20) claims
every value the timeline EVER carries; a scalar shows one bucket's
value and would silently miss the rest, so both executors MUST fault:
the full timeline is required, never a silent narrowing to whichever
value the case happened to supply.
