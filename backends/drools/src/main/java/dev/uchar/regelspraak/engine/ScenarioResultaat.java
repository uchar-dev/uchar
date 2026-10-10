package dev.uchar.regelspraak.engine;

import java.util.List;

/**
 * The report of one case of a replay ({@code CaseRunner.rapport}): its {@link Status}, the per-expectation verdicts,
 * the fired-rules {@link Trace} and the coverage of the lowered constructs. A case is a scenario lowered by the
 * RegelSpraak frontend or one written by hand; the report reads the case file and the IR document and nothing else.
 * It carries both the expected and the actual value per expectation, which is what a failing expectation prints.
 *
 * <p>It carries what the replay knows and nothing a later layer adds: an explanation of the outcome in words is
 * presentation, built by a layer that verbalises, and that layer carries it beside this record.
 *
 * @param naam          the case file's name
 * @param status        PASS (every expectation met), FAIL (an expectation missed, no fixpoint, or the case could not
 *                      be played: its play raised an exception), or MARKER (the backend does not carry the document or
 *                      a form of the case — the replay's EXCLUDE, a construct the transpiler does not lower among them —
 *                      so accepted-but-inert never reads as a pass)
 * @param verwachtingen the per-expectation verdicts, in the case's order; empty for MARKER and for a case that could not
 *                      be played
 * @param trace         the firings per form with the labels of the input instances they bound
 * @param dekking       per lowered construct whether it fired in this case
 * @param diagnose      why it is not a PASS: the replay's verdict line for FAIL (for a case that could not be played,
 *                      the case's name and the replay's ERROR line), the reason for MARKER; empty for PASS
 */
public record ScenarioResultaat(String naam, Status status, List<VerwachtingResultaat> verwachtingen,
                                Trace trace, List<DekkingRegel> dekking, String diagnose) {

    public enum Status { PASS, FAIL, MARKER }

    public boolean pass() { return status == Status.PASS; }

    /** One expectation's verdict: the expectation ({@code bron}: its kind and what it is about), the expected and the
     *  actual value rendered alike, whether it held, and its 1-based position in the case's {@code expected}. */
    public record VerwachtingResultaat(String bron, String verwacht, String werkelijk, boolean ok, int positie) {}

    /** The firings per form, in the order a rule of the form first fired. */
    public record Trace(List<Vuring> vuringen) {
        public int totaal() { return vuringen.stream().mapToInt(Vuring::aantal).sum(); }
    }
    /** The firings of one form of the document: its name (no version suffix), the labels of the input instances its
     *  matches bound, and how often its rules fired. A rule the lowering keeps for every form alike belongs to no form
     *  and has no row. */
    public record Vuring(String regel, List<String> labels, int aantal) {}

    /** One lowered construct's coverage in this case. */
    public record DekkingRegel(String regel, DekkingSoort soort) {}
    /** Whether a lowered construct fired. A construct the transpiler does not lower has no entry: the replay
     *  refuses the document before a case plays, and the case's status says MARKER. */
    public enum DekkingSoort { GEVUURD, NIET_GERAAKT }
}
