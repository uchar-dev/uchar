package dev.uchar.regelspraak.engine;

import java.util.List;

/**
 * The report of one case of a replay ({@code CaseRunner.rapport}): its {@link Status}, the per-expectation verdicts,
 * the fired-rules {@link Trace} and the coverage of the lowered constructs. A case is a scenario lowered by the
 * RegelSpraak frontend or one written by hand; the report reads the case file and the IR document and nothing else.
 * It carries both the expected and the actual value per expectation, which is what a failing expectation prints.
 *
 * @param naam          the case file's name
 * @param status        PASS (every expectation met), FAIL (an expectation missed, or no fixpoint), or MARKER (the
 *                      backend does not carry the document or a form of the case — the replay's EXCLUDE, a construct
 *                      the transpiler does not lower among them — so accepted-but-inert never reads as a pass)
 * @param verwachtingen the per-expectation verdicts, in the case's order; empty for MARKER
 * @param trace         the fired rules with the labels of the input instances they bound
 * @param dekking       per lowered construct whether it fired in this case
 * @param diagnose      why it is not a PASS: the replay's verdict line for FAIL, the reason for MARKER; empty for PASS
 * @param uitleg        an explanation per line, for a layer that verbalises the outcome; the replay leaves it empty
 */
public record ScenarioResultaat(String naam, Status status, List<VerwachtingResultaat> verwachtingen,
                                Trace trace, List<DekkingRegel> dekking, String diagnose, List<String> uitleg) {

    /** Without an explanation, as the replay reports. */
    public ScenarioResultaat(String naam, Status status, List<VerwachtingResultaat> verwachtingen,
                             Trace trace, List<DekkingRegel> dekking, String diagnose) {
        this(naam, status, verwachtingen, trace, dekking, diagnose, List.of());
    }

    /** This resultaat with an explanation attached. */
    public ScenarioResultaat metUitleg(List<String> uitleg) {
        return new ScenarioResultaat(naam, status, verwachtingen, trace, dekking, diagnose,
                uitleg == null ? List.of() : List.copyOf(uitleg));
    }

    public enum Status { PASS, FAIL, MARKER }

    public boolean pass() { return status == Status.PASS; }

    /** One expectation's verdict: the expectation ({@code bron}: its kind and what it is about), the expected and the
     *  actual value rendered alike, whether it held, and its 1-based position in the case's {@code expected}. */
    public record VerwachtingResultaat(String bron, String verwacht, String werkelijk, boolean ok, int positie) {}

    /** The fired rules, in the order they first fired. */
    public record Trace(List<Vuring> vuringen) {
        public int totaal() { return vuringen.stream().mapToInt(Vuring::aantal).sum(); }
    }
    /** One fired rule: its base name (no group prefix, no version suffix), the labels of the input instances its
     *  matches bound, the produced value where the report determines one (else empty), and how often it fired. */
    public record Vuring(String regel, List<String> labels, String waarde, int aantal) {}

    /** One lowered construct's coverage in this case. */
    public record DekkingRegel(String regel, DekkingSoort soort) {}
    /** Whether a lowered construct fired. A construct the transpiler does not lower has no entry: the replay
     *  refuses the document before a case plays, and the case's status says MARKER. */
    public enum DekkingSoort { GEVUURD, NIET_GERAAKT }
}
