package dev.uchar.regelspraak.refusal;

import dev.uchar.regelspraak.message.Msg;
import java.util.Locale;

/** A refusal's register code together with the message that its parameters make, for a method that hands a reason to
 *  the caller who turns it into a refusal. The sentence comes from the catalogue, in the default language of the
 *  backend ({@link #text()}) or in a language asked for. */
public record Reason(DroolsCode code, Msg msg) {
    /** The sentence in the default language of the backend. */
    public String text() { return msg.text(); }

    public String text(Locale locale) { return msg.text(locale); }

    /** The same refusal inside the {@code FRAME.<frame>} sentence, whose parameters {@code frameParams} name beside
     *  {@code sentence}, the refusal's own message. */
    public Reason framed(String frame, Object... frameParams) {
        java.util.Map<String, Object> args = new java.util.LinkedHashMap<>(Msg.args(frameParams));
        args.put("sentence", msg);
        return new Reason(code, new Msg(msg.component(), "FRAME." + frame, args));
    }
}
