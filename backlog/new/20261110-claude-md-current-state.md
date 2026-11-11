# CLAUDE.md describes the current state of the gate set

Goal: the section "Gate set" of `CLAUDE.md` says what each gate holds today and nothing about how it came to hold it.
The file calls itself "the constitution's index, not a logbook", and every session and every subagent loads it whole.

Measured on `ccf6de21c`: `CLAUDE.md` is 67,536 bytes; the section "Gate set" runs from line 132 to line 908, 777 of
about 1,010 lines. Most of it is history in the form "Since the card `<name>` (<date>) …", and the entry of gate 12
alone names several dozen cards. That history is in `git log` and in `backlog/done/`.

Acceptance:
- Per gate one entry: the script, what it holds, its lists and their stale rule, its floors by name (the numbers live
  in the scripts), and what makes it fail. No "Since the card", no dates except those of a maintainer decision that
  is itself the rule (where the gates run, how the set runs, the counting floors).
- Nothing a gate holds today is dropped from the description: every list, floor name and witness class the present
  text names as current is still named. A sentence about a mechanism that no longer exists goes.
- The check of `make gates` that the list names the scripts the `Makefile` and `.github/workflows/gates.yml` run, in
  that order, passes unchanged.
- The section is at most a third of its present length.
- `make gates` green.

May touch: `CLAUDE.md` (protected path: the main session lands it, as the maintainer's hand), this card.

Out of scope: the gate scripts and their comments; the other sections of `CLAUDE.md`; `WAY-OF-WORKING.md`; moving
the history to another file (it is in git already).
