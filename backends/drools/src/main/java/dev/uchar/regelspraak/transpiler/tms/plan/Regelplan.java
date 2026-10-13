package dev.uchar.regelspraak.transpiler.tms.plan;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * One rule as the analysis plans it, complete: what a rendering into Drools' executable model or into DRL text takes,
 * deciding nothing itself.
 *
 * <p>A variable is declared once in its scope, and every slot has a variable of its own: a rendering spells a variable
 * by its name, so a second declaration of a name in scope would bind two values to one variable, or fail to build. The
 * plan refuses that at its construction, for every document. A declaration of the rule's top level (a pattern's
 * variable, the subject pattern's among them, a value a binding binds, the source of an accumulate and its results) is
 * in scope for every item after it; one inside a negation or an existence lives only inside it ({@link Bestaat}), so
 * two quantifications may each declare one name, while neither may declare a name in scope where it stands.
 *
 * @param naam        the rule's name, by which a KieBase keeps one rule
 * @param vorm        the form of the document the rule belongs to, as the manifest names it ({@code vormVan}); null for
 *                    a rule of no form, such as a standing or the rule of two writers of one target
 * @param agendagroep the agenda group the rule fires in, null for the main group
 * @param lezingen    the slots of the rule's value array, which the test and the consequence read
 * @param linkerkant  the left-hand side in order: the subject pattern with its constraints and bindings where the rule
 *                    has a subject, the patterns, the negations, the accumulates and the tests, each constraint with
 *                    its id
 * @param gevolg      what the rule does when it fires
 */
public record Regelplan(String naam, String vorm, String agendagroep, Leestabel lezingen, List<LhsItem> linkerkant,
                        Gevolg gevolg) {

    public Regelplan {
        linkerkant = List.copyOf(linkerkant);
        Set<String> inScope = new HashSet<>();
        for (LhsItem item : linkerkant) {
            switch (item) {
                case Onderwerp o -> {
                    verklaar(naam, o.naam(), inScope);
                    for (Binding b : o.bindingen()) verklaar(naam, b.doel(), inScope);
                }
                case Patroon p -> verklaar(naam, p, inScope);
                case Accumulatie a -> {
                    // the source's variables stay in scope, as the rendering into the executable model keeps them
                    verklaar(naam, a.bron(), inScope);
                    for (Accumulatie.Functie f : a.functies()) verklaar(naam, f.doel(), inScope);
                }
                case Niet n -> binnen(naam, n.binnen(), inScope);
                case Bestaat b -> binnen(naam, b.binnen(), inScope);
                case Toetsitem t -> { }
            }
        }
        Set<String> slotnamen = new HashSet<>();
        for (Lezing l : lezingen.slots())
            if (l.naam() != null && !slotnamen.add(l.naam()))
                throw new IllegalArgumentException("de regel " + naam + " geeft twee slots de variabele " + l.naam());
    }

    private static void verklaar(String regel, Patroon p, Set<String> inScope) {
        verklaar(regel, p.naam(), inScope);
        for (Binding b : p.bindingen()) verklaar(regel, b.doel(), inScope);
    }

    private static void verklaar(String regel, String variabele, Set<String> inScope) {
        if (!inScope.add(variabele))
            throw new IllegalArgumentException("de regel " + regel + " declareert de variabele " + variabele
                    + " twee keer");
    }

    /** The variables a quantification declares, which live only inside it: none may be in scope where it stands
     *  ({@link Bestaat} holds them distinct among themselves). */
    private static void binnen(String regel, List<String> binnen, Set<String> inScope) {
        for (String v : binnen)
            if (inScope.contains(v))
                throw new IllegalArgumentException("de regel " + regel + " declareert de variabele " + v
                        + " twee keer");
    }
}
