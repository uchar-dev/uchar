package dev.uchar.regelspraak.refusal;

/** A refusal's sentence together with its register code, for a method that hands a reason to the caller who turns
 *  it into a refusal. The code stays beside the text, never inside it. */
public record Reason(DroolsCode code, String text) {}
