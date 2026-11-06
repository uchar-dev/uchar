Violates WF-129 (`spec/IR-SPEC.md` ch. 4.2), the arm of a value that is no number. The rule writes the status of a
Vondst as "Gemeld: " followed by its bevestiging, a boolean, and the inserted part asks for the unit of its number
(`unit: true`, RUNTIME-SEMANTICS ch. 1.2): a boolean has no unit, so the document is not well-formed. The
RegelSpraak frontend refuses the source `«zijn bevestiging (eenheid)»` at check; this document is the frontend's
output for `«zijn bevestiging»` with the field added.
