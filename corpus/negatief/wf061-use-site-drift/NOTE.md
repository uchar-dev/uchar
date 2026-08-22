Violates WF-061 (`spec/IR-SPEC.md` ch. 2.4): the `beschikking` attribute
use site repeats a constructor list that disagrees with the declaring
domain — its third constructor is `Uitgesteld`, where the declaration
has `Aangehouden`. A use site must name a declared domain whose
SumType agrees (same constructors, same payloads, same order).

A minimal hand-built document.
