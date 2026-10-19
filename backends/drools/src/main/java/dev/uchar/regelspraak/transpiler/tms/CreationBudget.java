package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.message.Msg;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The fault of a mint past the creation budget (RUNTIME-SEMANTICS ch. 5.5): an object creation would make an object at
 * creation depth {@link TmsTranspiler#CREATION_BUDGET} + 1 in its stratum. The creation rule throws it at the making,
 * before it inserts anything, so the object is not made; the driver ({@code DroolsBackend.fire}) ends the run with its
 * sentence, as a run without a fixpoint. The sentence starts with the fragment the reference interpreter and the ASP
 * backend fault with, "creation budget exhausted". The fault carries the parameters of its kind (EF-015), so no report
 * has to read them back out of the sentence.
 */
public final class CreationBudget extends RuntimeException {

    private final transient Msg msg;
    private final Map<String, String> params = new LinkedHashMap<>();

    CreationBudget(String creation, int stratum, int depth, String type, int parentDepth) {
        this(Msg.drools("EF-015", "creation", creation, "stratum", stratum, "depth", depth, "type", type,
                "parentDepth", parentDepth));
        params.put("creation", creation);
        params.put("stratum", String.valueOf(stratum));
        params.put("depth", String.valueOf(depth));
    }

    private CreationBudget(Msg msg) {
        super(msg.text());
        this.msg = msg;
    }

    /** The parameters of the kind (RUNTIME-SEMANTICS ch. 5.7b): the creation, its stratum and the depth the mint would
     *  reach. */
    public Map<String, String> params() { return params; }

    /** The sentence in {@code locale}. */
    public String message(Locale locale) { return msg.text(locale); }
}
