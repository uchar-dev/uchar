# The uchar gate set (CLAUDE.md, "Gate set"; WAY-OF-WORKING.md §6). Maintainer decision 2026-10-25, card
# gate-builds-when-stale.
#
#     make gates
#
# first compares the gate lists: the gate scripts the targets below run must be those .github/workflows/
# gates.yml runs, in its order, and those CLAUDE.md lists; a difference stops the run before anything is
# built and names the script. Then it builds the three trees the gates read, side by side, each only when a
# source is newer than its product: `ocaml`, the OCaml workspace (scripts/lib/ocaml-build.sh); `frontend`,
# the RegelSpraak frontend and its two tool jars (frontends/regelspraak/Makefile); `jar`, the Drools
# backend jar (scripts/lib/drools-jar.sh). When all three are green it runs the 17 gates in parallel in
# this checkout, `make -k -j<cores>`, the number of cores read from the machine. A gate builds what it
# needs only when a source is newer, so after the build step no gate builds, and no two gates write the
# same file.
#
# Each target writes its output to out/gates/<run>/<target>.log. out/gates/<run>/summary.txt holds one
# line per build target and one per gate, in the order of .github/workflows/gates.yml:
#     <target> rc=<status> <seconds>s                       (a gate)
#     <target> rc=<status> <seconds>s <status line>         (a build: fresh (…), rebuilt (…) or FAIL — …)
# A target without a line did not run and counts as red; when a build is red, the gates do not run. The
# exit status is 0 only when every build and every gate is green.
#
# A gate runs on its own with its script, exactly as its command below says (`bash
# scripts/check-drools.sh`). That `make 12-drools` also runs one gate is a consequence of the targets here,
# not a second way to run it. Every target runs its command without the MAKEFLAGS of this make, so a
# script behaves as it does on its own. Works with GNU make 3.81 (macOS) and newer.

.DEFAULT_GOAL := help

# one gate per core, read from the machine; `override`, so a command line cannot set it
override CORES := $(strip $(shell getconf _NPROCESSORS_ONLN 2>/dev/null))
ifeq ($(CORES),)
$(error getconf _NPROCESSORS_ONLN gave no number of cores; make gates runs one gate per core)
endif

# One directory per run; a sub-make gets the same RUN on its command line.
RUN  := $(shell date +%Y%m%d-%H%M%S)
LOGS := out/gates/$(RUN)

BUILDS := ocaml frontend jar
# the order of .github/workflows/gates.yml, which is the order of the summary
GATES := 01-self-contained 02-ir-schema 04-case-schema 03-verifier 05-oracle 06-clingo 07-noop 08-red \
         09-catala 10-regelspraak 11-toka-parity 12-drools 13-reftype 14-flogic 15-graph 16-scenario \
         17-readings

.PHONY: help gates $(BUILDS) $(GATES)

# The commands, with the environment .github/workflows/gates.yml gives each gate: the OCaml gates under
# `opam exec`, sh or bash as the script needs.
CMD.ocaml    := opam exec -- sh -c '. scripts/lib/ocaml-build.sh && ensure_ocaml'
CMD.frontend := if $(MAKE) -q -C frontends/regelspraak compile junit-jar; then \
                  echo 'frontend: fresh (out/ newer than every source, the tool jars present)' >&2; \
                elif t0=$$(date +%s) && $(MAKE) -C frontends/regelspraak compile junit-jar; then \
                  echo "frontend: rebuilt ($$(( $$(date +%s) - t0 ))s)" >&2; \
                else echo 'frontend: FAIL — the frontend build is red' >&2; exit 1; fi
CMD.jar      := sh -c '. scripts/lib/drools-jar.sh && ensure_drools_jar >/dev/null'

CMD.01-self-contained := sh scripts/check-self-contained.sh
CMD.02-ir-schema      := sh scripts/check-ir-schema.sh
CMD.04-case-schema    := sh scripts/check-case-schema.sh
CMD.03-verifier       := opam exec -- sh scripts/check-verifier.sh
CMD.05-oracle         := opam exec -- sh scripts/check-oracle.sh
CMD.06-clingo         := opam exec -- sh scripts/check-clingo.sh
CMD.07-noop           := opam exec -- sh scripts/check-noop.sh
CMD.08-red            := opam exec -- sh scripts/check-red.sh
CMD.09-catala         := opam exec -- bash scripts/check-catala.sh
CMD.10-regelspraak    := bash scripts/check-regelspraak.sh
CMD.11-toka-parity    := opam exec -- bash scripts/check-toka-parity.sh
CMD.12-drools         := bash scripts/check-drools.sh
CMD.13-reftype        := opam exec -- bash scripts/check-reftype-derivation.sh
CMD.14-flogic         := opam exec -- bash scripts/check-flogic.sh
CMD.15-graph          := opam exec -- bash scripts/check-graph.sh
CMD.16-scenario       := bash scripts/check-scenario.sh
CMD.17-readings       := opam exec -- bash scripts/check-readings.sh

# A target without a command would run an empty recipe and report rc=0.
$(foreach t,$(BUILDS) $(GATES),$(if $(CMD.$(t)),,$(error no command CMD.$(t) for the target $(t))))

# the gate script of each gate target, in the order of GATES
GATE_SCRIPTS := $(foreach g,$(GATES),$(filter scripts/check-%.sh,$(CMD.$(g))))

help:
	@echo 'make gates    runs the gate set: the three builds, then the 17 gates in parallel (CLAUDE.md, Gate set);'
	@echo '              logs and summary under out/gates/<run>/. A single gate runs with its script.'

gates:
	@mk=' $(GATE_SCRIPTS) '; bad=0; \
	yml=$$(grep -v '^[[:space:]]*#' .github/workflows/gates.yml | grep -o 'scripts/check-[a-z0-9-]*\.sh' | awk '!seen[$$0]++'); \
	doc=$$(grep -o '^[0-9][0-9]*\. `scripts/check-[a-z0-9-]*\.sh`' CLAUDE.md | grep -o 'scripts/check-[a-z0-9-]*\.sh'); \
	for s in $$yml; do case "$$mk" in *" $$s "*) ;; *) echo "make gates: .github/workflows/gates.yml runs $$s, no target of the Makefile does" >&2; bad=1 ;; esac; done; \
	for s in $$doc; do case "$$mk" in *" $$s "*) ;; *) echo "make gates: CLAUDE.md lists the gate $$s, no target of the Makefile runs it" >&2; bad=1 ;; esac; done; \
	for s in $(GATE_SCRIPTS); do \
	  case " $$(echo $$yml) " in *" $$s "*) ;; *) echo "make gates: the Makefile runs $$s, .github/workflows/gates.yml does not" >&2; bad=1 ;; esac; \
	  case " $$(echo $$doc) " in *" $$s "*) ;; *) echo "make gates: the Makefile runs $$s, CLAUDE.md lists no such gate" >&2; bad=1 ;; esac; \
	done; \
	if [ $$bad -eq 0 ] && [ "$$(echo $$yml)" != "$(GATE_SCRIPTS)" ]; then \
	  echo "make gates: GATES is not in the order of .github/workflows/gates.yml, the order of the summary: $$(echo $$yml)" >&2; bad=1; \
	fi; \
	if [ $$bad -ne 0 ]; then echo 'make gates: the gate lists of the Makefile, .github/workflows/gates.yml and CLAUDE.md differ (above); nothing ran' >&2; exit 2; fi
	@mkdir -p out/gates; mkdir $(LOGS) || { echo "make gates: $(LOGS) exists; start the run again" >&2; exit 2; }
	@echo "start $$(date '+%F %T') $$(git rev-parse --short HEAD) $$(git branch --show-current)" > $(LOGS)/summary.txt
	@t0=$$(date +%s); \
	if $(MAKE) --no-print-directory -k -j3 RUN=$(RUN) $(BUILDS); then \
	  $(MAKE) --no-print-directory -k -j$(CORES) RUN=$(RUN) $(GATES); \
	fi; \
	red=0; \
	for t in $(BUILDS) $(GATES); do \
	  if [ -f $(LOGS)/$$t.line ]; then line=$$(cat $(LOGS)/$$t.line); else line="$$t did not run"; fi; \
	  case "$$line" in *' rc=0 '*) ;; *) red=$$((red + 1)) ;; esac; \
	  echo "$$line" >> $(LOGS)/summary.txt; \
	done; \
	echo "end $$(date '+%F %T') $$(( $$(date +%s) - t0 ))s red=$$red" >> $(LOGS)/summary.txt; \
	echo; cat $(LOGS)/summary.txt; echo "logs: $(LOGS)/"; \
	[ $$red -eq 0 ]

# One recipe for every build and gate: the command's output to its log, then its summary line, which a
# build ends with the last line of its log, its status line.
$(BUILDS) $(GATES):
	@mkdir -p $(LOGS); t0=$$(date +%s); \
	( unset MAKEFLAGS MFLAGS MAKELEVEL; $(CMD.$@) ) > $(LOGS)/$@.log 2>&1; rc=$$?; \
	line="$@ rc=$$rc $$(( $$(date +%s) - t0 ))s"; \
	case ' $(BUILDS) ' in *' $@ '*) line="$$line $$(tail -n 1 $(LOGS)/$@.log)" ;; esac; \
	echo "$$line" > $(LOGS)/$@.line; echo "$$line"; \
	exit $$rc
