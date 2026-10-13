package dev.uchar.regelspraak.transpiler.tms.plan;

import java.util.List;

/**
 * One rule as the analysis plans it, complete: what a rendering into Drools' executable model or into DRL text takes,
 * deciding nothing itself.
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
    }
}
