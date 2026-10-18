package dev.uchar.regelspraak.ir;

/**
 * §9.9/§13.4.12 a resolved startpuntbepaling: for {@code objectType}, the variable-startpunt
 * tijdlijn {@code tijdlijn} anchors at the per-object date held in the synthetic derived
 * attribute {@code lid} ("startpunt &lt;tijdlijn&gt;", Datum in dagen). The WRITE of that
 * attribute is an ordinary {@link ResolvedRule} named {@code regel} (so voorwaarde,
 * geldigheid, freshness and idempotence ride the standard machinery in both back-ends);
 * this record only registers the anchor member so the period machinery can find it and the
 * §6.8 leeg-fout companion knows which writes it guards. Several startpuntbepalingen for one
 * (objecttype, tijdlijn) pair — §9.9 allows different startpunten under different
 * voorwaarden — share one entry (one lid), each with its own ResolvedRule.
 */
public record ResolvedStartPointDetermination(String tijdlijn, String objectType, String member) {}
