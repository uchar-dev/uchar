package dev.uchar.regelspraak.transpiler.tms;

import java.util.List;
import java.util.Objects;

/**
 * The value a {@link State} carries for an attribute on a timeline whose case input is the asOfDate-bucket scalar
 * ({@link BucketInput}, RUNTIME-SEMANTICS ch. 8.4): the period of the attribute's grid that holds the evaluation date,
 * with the scalar as its value. It is not a timeline: it states no period of its own beyond that one, so it serves the
 * subject's own total without a clip, where the session's date is the horizon (ch. 3.6), and every form that reads the
 * full timeline faults on it ({@link InputFormFault}). Compared by the period, as {@link Period} is.
 */
public record BucketScalar(Period bucket) {

    public BucketScalar {
        Objects.requireNonNull(bucket, "bucket");
    }

    /** The bucket scalar the bean of an {@link BucketInput} carries as {@code segments}: the case input lays it there
     *  as the one segment of the evaluation date's period. Anything else breaks that contract. */
    static BucketScalar of(List<?> segments) {
        List<Period> timeline = Period.list(segments);
        if (timeline == null || timeline.size() != 1)
            throw new IllegalStateException("een emmerscalair staat op de bean als het ene segment van de periode van de"
                    + " rekendatum, niet als " + timeline);
        return new BucketScalar(timeline.get(0));
    }

    /** The bucket as a timeline of one segment, which the subject's own total folds (ch. 3.6). */
    List<Period> timeline() {
        return List.of(bucket);
    }

    @Override public String toString() {
        return "BucketScalar " + bucket;
    }
}
