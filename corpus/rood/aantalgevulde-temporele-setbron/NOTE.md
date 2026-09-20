# aantalgevulde-temporele-setbron

The one exclusion `RUNTIME-SEMANTICS.md` ch. 3.4c states normatively, as a
red case. The document is `jutterswet-gevuldetelling`'s twin with one
change: `gewicht` is declared TIME-DEPENDENT (granularity DAG). Everything
else — the fold, the element type, the source role, the count's own type —
is unchanged and well-formed, so the verifier passes it and the refusal
must come from the executors.

A set read of a time-dependent attribute folds TWICE: each member's own
timeline collapses over time first, and only those scalars fold over the
set (ch. 3.6, §7.1). That first collapse is defined for the som and the
totaal and for nothing else, because only they integrate. A count has no
defined MOMENT at which to ask a timeline whether it is filled, and
picking one — the session date, the first segment, the last — would be an
executor inventing a reading the chapter does not carry. Both OCaml
executors therefore refuse the form by name, each naming the attribute,
rather than answering from whatever bucket happens to be at hand.

What this case guards against is a SILENT answer: an executor that reads
the segment list as "some object, not null" counts the member as filled
and returns a number no clause justifies. The two pinned fragments are
each their own arm's sentence, not a fall-through — the fall-through
sentence of either executor describes a position rather than a construct
and would witness nothing.

The door is closed and not locked: a later card with a real casus may give
the filled count its own moment rule, and it would be a clause in ch. 3.4c
rather than an executor's choice. On that day this case changes with the
chapter.
