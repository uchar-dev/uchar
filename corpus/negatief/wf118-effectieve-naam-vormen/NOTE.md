# wf118-effectieve-naam-vormen

Violates WF-118 (`spec/IR-SPEC.md` ch. 2.12): the effective names of the
named forms, one scope across the model (WF-020), are unique per
vocabulary.

    WF-118 FAIL /rules/rules/3: forms 'afwijking opgave meldingsdatum' and
    'afwijking opgave aangiftedatum' share the effective name 'afwijking
    opgave dag van de melding' in the vocabulary 'handboek'
    WF-118 FAIL /rules/consistencies/1: forms 'controle opgave meldingsdatum'
    and 'controle opgave aangiftedatum' share the effective name 'controle
    opgave dag van de melding' in the vocabulary 'handboek'
    WF-118 FAIL /rules/prioritizedDefaults/1: forms 'opgave boven berekening
    meldingsdatum' and 'opgave boven berekening aangiftedatum' share the
    effective name 'opgave boven berekening dag van de melding' in the
    vocabulary 'handboek'

The document is what the RegelSpraak frontend writes for two attributes
with the opgave marker on two object types that carry one designation in
one vocabulary:

    Vocabulaire handboek (taal: nl)

    Objecttype de Vondst (mv: Vondsten)
        de vinddag Datum in dagen;
        de meldingsdatum (handboek: dag van de melding) Datum in dagen (mag opgegeven worden);

    Objecttype de Schuit (mv: Schuiten)
        de stranddag Datum in dagen;
        de aangiftedatum (handboek: dag van de melding) Datum in dagen (mag opgegeven worden);

with a rule computing each attribute. The frontend gives each generated
member and form a designation composed from the marked attribute's
(`frontends/regelspraak/grammar/DIVERGENCES.md`, "Gegenereerde elementen",
"Designations"), so the three generated forms of the two attributes share
their effective names in `handboek`. The frontend refuses that source under
WF-118 itself; the document was emitted from the same source with the
second designation spelled `dag van de aangifte`, and that text replaced by
`dag van de melding` in the designations and in the consistency's source
snippet, which is the document the frontend would write without its
refusal. The generated attributes and characteristics stand in the scope of
their own object type and do not collide; the forms do, one line each. No
other negatief case witnesses the scope of the named forms; the arm
`expect.txt` pins is its sentence, which carries no scope after the two
names.
