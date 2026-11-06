Violates WF-129 (`spec/IR-SPEC.md` ch. 4.2), the arm of a number without a unit. The rule writes the omschrijving of a
Vondst as "Bedrag: " followed by its basis, a number of 2 decimals without a unit, and the inserted part asks for the
unit of its number (`unit: true`, RUNTIME-SEMANTICS ch. 1.2): it has none to write, so the document is not
well-formed. The RegelSpraak frontend refuses the source `«zijn basis (eenheid)»` at check; this document is the
frontend's output for `«zijn basis»` with the field added.
