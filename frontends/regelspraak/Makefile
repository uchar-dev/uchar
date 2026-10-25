# frontends/regelspraak — jar-only ANTLR build (Java target, JDK 21+).
#
# Builds the RegelSpraak v2.3.0 parser from grammar/*.g4 using a *local* ANTLR
# jar, so no global `antlr4` install is required: the complete jar bundles the
# ANTLR tool and the Java runtime, and the JUnit 5 standalone console launcher
# bundles the platform + jupiter engine. The whole frontend (generate ->
# compile -> resolve -> emit IR -> test) runs with only a JDK + these two
# jars: no Maven, no build daemon. The jars are fetched from their public
# release URLs into lib/ (gitignored — a cache, not a source).

.DEFAULT_GOAL := help
.PHONY: help antlr-jar junit-jar parser compile test ir clean clean-jar

ANTLR4_VERSION := 4.13.2
ANTLR4_URL     := https://www.antlr.org/download/antlr-$(ANTLR4_VERSION)-complete.jar
LIB_DIR        := $(CURDIR)/lib
ANTLR4_JAR     := $(LIB_DIR)/antlr-$(ANTLR4_VERSION)-complete.jar
ANTLR4         := java -jar $(ANTLR4_JAR)

JUNIT_VERSION := 1.10.2
JUNIT_JAR     := $(LIB_DIR)/junit-platform-console-standalone-$(JUNIT_VERSION).jar
JUNIT_URL     := https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/$(JUNIT_VERSION)/junit-platform-console-standalone-$(JUNIT_VERSION).jar

MAIN_SRC := $(shell find src/main/java -name '*.java' 2>/dev/null)
MAIN_RES := $(shell find src/main/resources -type f 2>/dev/null)
# Directories count as well as files: deleting or renaming a source updates its directory, not any file.
MAIN_DIRS := $(shell find src/main/java src/main/resources -type d 2>/dev/null)
TEST_SRC := $(shell find src/test/java -name '*.java' 2>/dev/null)

# Java package for the generated parser, and the dirs that follow from it.
PACKAGE  := dev.uchar.regelspraak.parser
PKG_PATH := $(subst .,/,$(PACKAGE))
GEN_DIR  := gen/$(PKG_PATH)
OUT_DIR  := out
TEST_OUT_DIR := out-test

# The stamps of the two products: `parser` and `compile` do nothing when neither grammar, source, resource,
# tool jar nor this Makefile (its commands and flags) is newer than its stamp (card gate-builds-when-stale,
# 2026-10-25), so gates that run side by side in one checkout find the frontend built and leave out/ alone.
# Each stamp takes its time from the moment its recipe starts (a marker made then and moved into place at
# the end), so a source saved during the build is newer than the stamp and the next call builds again. GNU
# make 3.81 compares whole seconds: an edit in the same second as the start of the build passes as fresh.
GEN_STAMP := gen/.generated
OUT_STAMP := $(OUT_DIR)/.compiled

GRAMMAR_DIR := grammar
LEXER_G4  := $(GRAMMAR_DIR)/RegelSpraakLexer.g4
PARSER_G4 := $(GRAMMAR_DIR)/RegelSpraak.g4
GRAMMARS  := $(wildcard $(GRAMMAR_DIR)/*.g4)

help:
	@echo "frontends/regelspraak — ANTLR $(ANTLR4_VERSION) (Java target, jar-only)"
	@echo ""
	@echo "  make antlr-jar             Ensure lib/antlr-$(ANTLR4_VERSION)-complete.jar is present"
	@echo "  make parser                Generate the Java lexer+parser (lexer first, then parser)"
	@echo "  make compile               Compile the generated parser + the frontend sources"
	@echo "  make test                  Run the JUnit 5 suite via the standalone console launcher"
	@echo "  make ir FILE=\"a.rgs …\" [OUT=…]  Emit the IR JSON (uchar-ir-v1) for one-or-more .rgs files"
	@echo "  make ir FILE=\"source.rgs scenario.rgs\" OUT=… CASES=<map>  … and lower the scenarios to case files"
	@echo "  make clean                 Remove generated sources (gen/) and classes (out/, out-test/)"
	@echo "  make clean-jar             Remove the downloaded jars (lib/)"

antlr-jar: $(ANTLR4_JAR)

$(ANTLR4_JAR):
	mkdir -p $(LIB_DIR)
	curl -fsSL $(ANTLR4_URL) -o $@.tmp
	mv $@.tmp $@

junit-jar: $(JUNIT_JAR)

$(JUNIT_JAR):
	mkdir -p $(LIB_DIR)
	curl -fsSL $(JUNIT_URL) -o $@.tmp
	mv $@.tmp $@

# Generate the lexer FIRST: it emits RegelSpraakLexer.tokens, which the parser
# grammar imports via `options { tokenVocab=RegelSpraakLexer; }`. -lib points
# the parser step at the freshly-written .tokens. ANTLR is invoked from inside
# $(GRAMMAR_DIR) with a bare filename: given a grammar path with a directory
# component, ANTLR would mirror that directory *under* -o.
parser: $(GEN_STAMP)

$(GEN_STAMP): $(GRAMMARS) $(ANTLR4_JAR) Makefile
	@test -f $(LEXER_G4)  || { echo "missing $(LEXER_G4)";  exit 1; }
	@test -f $(PARSER_G4) || { echo "missing $(PARSER_G4)"; exit 1; }
	mkdir -p $(GEN_DIR)
	touch $@.begin
	cd $(GRAMMAR_DIR) && $(ANTLR4) -Dlanguage=Java -visitor -no-listener -package $(PACKAGE) -o $(CURDIR)/$(GEN_DIR) $(notdir $(LEXER_G4))
	cd $(GRAMMAR_DIR) && $(ANTLR4) -Dlanguage=Java -visitor -no-listener -package $(PACKAGE) -lib $(CURDIR)/$(GEN_DIR) -o $(CURDIR)/$(GEN_DIR) $(notdir $(PARSER_G4))
	mv $@.begin $@

# Compile the generated parser, then the frontend (resolver + IR emitter)
# against it. The ANTLR complete jar provides the runtime both link against.
# -Xdoclint:reference keeps @param and {@link} naming members that exist (a rename leaves them behind otherwise).
DOCLINT := -Xdoclint:reference

compile: $(OUT_STAMP)

# javac compiles every source anyway; an empty $(OUT_DIR) keeps a class of a renamed type from staying behind, which on a
# case-insensitive file system (macOS) takes the new name's place when the rename changes only the case (Objecttype -> ObjectType).
# The recipe runs only when the generated parser, a source, a resource, a source directory, the ANTLR jar or this Makefile
# (DOCLINT among its flags) is newer than the stamp.
$(OUT_STAMP): $(GEN_STAMP) $(MAIN_SRC) $(MAIN_RES) $(MAIN_DIRS) $(ANTLR4_JAR) Makefile
	rm -rf $(OUT_DIR)
	mkdir -p $(OUT_DIR)
	touch $@.begin
	javac -cp $(ANTLR4_JAR) -d $(OUT_DIR) $(GEN_DIR)/*.java
	javac $(DOCLINT) -cp $(ANTLR4_JAR):$(OUT_DIR) -d $(OUT_DIR) $(MAIN_SRC)
	cp -R src/main/resources/. $(OUT_DIR)/
	mv $@.begin $@

# Build + run the JUnit 5 tests via the standalone console launcher. The test classes go to their own
# directory, emptied first, so $(OUT_DIR) holds exactly the classes its stamp vouches for and a removed test
# leaves no class behind.
test: compile $(JUNIT_JAR)
	rm -rf $(TEST_OUT_DIR)
	mkdir -p $(TEST_OUT_DIR)
	javac $(DOCLINT) -cp $(ANTLR4_JAR):$(JUNIT_JAR):$(OUT_DIR) -d $(TEST_OUT_DIR) $(TEST_SRC)
	java -jar $(JUNIT_JAR) execute --class-path $(ANTLR4_JAR):$(OUT_DIR):$(TEST_OUT_DIR) \
		--scan-class-path --details=tree --disable-banner

# Emit the IR (uchar-ir-v1) — a FAITHFUL JSON dump of the complete resolved model,
# every node, key and enumeration value under its IR-SPEC name (the Java records
# keep their v0 names). Over one-or-more
# .rgs files (concatenated in listed order). An unresolvable model writes
# NOTHING and exits 1. With CASES=<map> the files are read as one test document
# and each scenario is lowered to a case file in <map> as well (scenariotoetsen,
# grammar/DIVERGENCES.md).
IR_OUT := $(if $(OUT),$(OUT),$(OUT_DIR)/ir.json)
ir: compile
	@java -cp $(ANTLR4_JAR):$(OUT_DIR) dev.uchar.regelspraak.resolver.Resolver --ir $(IR_OUT) $(if $(CASES),--cases $(CASES)) $(FILE)

clean:
	rm -rf gen $(OUT_DIR) $(TEST_OUT_DIR)

clean-jar:
	rm -rf $(LIB_DIR)
