package dev.uchar.regelspraak.transpiler.tms.plan;

/**
 * One slot of a rule's value array: what it holds ({@code soort}), the key by which the analysis reads it once per rule
 * ({@code sleutel}), and the name of the variable that binds it ({@code naam}), unique within the rule and an identifier
 * that DRL text can spell; a read of a member, a member along a path, a parameter, a reference field or a channel is
 * named after what it reads. A binder and a probe have neither key nor variable: no pattern binds them, the consequence
 * fills them.
 */
public record Lezing(int slot, Leessoort soort, String sleutel, String naam) {}
