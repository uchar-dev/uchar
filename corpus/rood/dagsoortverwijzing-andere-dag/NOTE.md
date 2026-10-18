# dagsoortverwijzing-andere-dag

The §9.8 composition — one day kind referencing another — is carried **on the
bound day only** (`RUNTIME-SEMANTICS.md` ch. 3.8). A nested test on some OTHER
date is a recognised, unexecuted form, and this is its witness.

## The shape

Two day kinds. `ankerdag` is the 15th of any month. `springtijdag` is defined
not over the day it is about, but over a **date literal**:

```
Een dag is een springtijdag indien 15-03-2026 een ankerdag is.
```

The document is verifier-clean: WF-031's purity clause admits a nested
day-kind reference and says nothing about which date it is applied to, and
WF-085's reference graph is acyclic.

## Why a RED case, and why the narrow route

The condition is not meaningless — it is *constant*, and a reader can see what
it would have to mean. It is red because of what was **measured on 2026-09-19**:
`ref` and the Drools day-kind compiler both answered this shape, while the ASP
backend refused it. One executor computing what another refuses is not a
semantics, and the day-kind card exists to end exactly that asymmetry. Two
ways out, and the narrow one was taken: all three now refuse, in the same
words, rather than two of them being taught a reading no source states.

What the carrying reading would need is a source for what a test on a fixed
date MEANS inside a definition about an arbitrary day — a constant conjunct, a
scoping error, or a shorthand for something else. That is an open question, not
a decision against: the day-kind card keeps the refusal and leaves the carrying
reading open until a source states what such a test means
(`backlog/done/20260910-dagsoort-semantiek.md`).

## What each executor pins

| executor | phase | pinned fragment |
|---|---|---|
| `ref` | evaluation | `a nested day-kind test on a date other than the bound day` |
| clingo | translation | same, verbatim |

The Drools backend does not reach fire: its day-kind predicate compiler
(`transpiler/kern/DayKinds`) now leaves the composition **uncompiled** when
the operand is not the bound day, so the reaching rule is a transpiler SKIP.
That is listed with its reason in gate 8's Drools exclusion list rather than
pinned here — the agreement is real, and only the two executors that can carry
a fragment pin one.
