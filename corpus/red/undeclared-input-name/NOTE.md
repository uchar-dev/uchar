# undeclared-input-name

The case input gives the `Schuit` `S1` a value for `rompbreedte`, an
attribute the document does not declare (a `Schuit` declares `romplengte`
only). A name the input writes is the declaration that it is under the
name equality of IR-SPEC ch. 2.1, and a name that is no declaration is
refused while the case is read, never read as another member whose name it
resembles. Every executor refuses the case with the fault kind EF-040 of
the case input (RUNTIME-SEMANTICS ch. 5.7 step 3, ch. 5.7b), with the label
of the instance and the name as written as its parameters.

EF-040 covers four names of the input: the object type of an instance, an
attribute, a characteristic and a role key. This case witnesses the
attribute; the Drools backend's own suite (`CaseNameRuleWitnessTest`) pins
the other three with their code and parameters. The document is the one
of `unknown-role-target-label`.
