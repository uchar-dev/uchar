Violates WF-047 (`spec/IR-SPEC.md`): an object creation's
`connectingRole` binding (the back-link extension, ch. 3.3) whose bound
role fills the wrong side — `dossierhouder` fills `Jutter`, but the
bound role must be the CREATED side (`Dossier`). The filler-agreement
clause at the creation site, pinned.

Since L4 an object creation is an `ObjectCreation` with its versions (delta
B19); the defect sits on the `ObjectCreationVersion`, whose fields are
`createdType`, `subject`, `initialisations` (each a `MemberInit`) and
`connectingRole`. `expect.txt` pins the arm.
