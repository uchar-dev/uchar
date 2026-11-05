Violates WF-128 (`spec/IR-SPEC.md` ch. 3.2), the field `orderingAttribute`. A Berging distributes its opbrengst over its
Bergers in an ordered allocation (`spec/RUNTIME-SEMANTICS.md` ch. 4); the attribute `strandrang` of Berger, the key the recipients are ordered by,
carries a value for every month. An allocation distributes one amount at one moment, and no reading of it
per day is defined, so the document is not well-formed. The RegelSpraak frontend refuses the source with
RGS-378; this document is the frontend's output for the scalar allocation with the timeline facet added.
