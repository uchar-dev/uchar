package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.model.Eenheid;
import dev.uchar.regelspraak.transpiler.tms.plan.Term;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/** A planned value or condition ({@link Term}) as text, for the witnesses that pin the plan node by node. */
final class Termboom {

    private Termboom() {}

    /** {@code t} as text: every node by its kind, with the fields the analysis decided. */
    static String boom(Term t) {
        return switch (t) {
            case Term.Const c -> "Const " + waarde(c.waarde())
                    + (c.type() == null || c.type().eenheidOf() == null ? "" : " " + c.type().eenheidOf().weergave());
            case Term.Lees l -> "Lees " + l.slot();
            case Term.Binder b -> "Binder " + b.slot() + " " + b.naam();
            case Term.Let l -> "Let " + l.slot() + " " + l.naam() + (l.gretig() ? " gretig" : "") + " [" + boom(l.waarde())
                    + "] in [" + boom(l.lichaam()) + "]";
            case Term.Keuze k -> "Keuze(" + boom(k.voorwaarde()) + ", " + boom(k.dan()) + ", " + boom(k.anders()) + ")";
            case Term.Aanroep a -> a.functie() + lijst(a.argumenten());
            case Term.Herschaal h -> "Herschaal " + h.functie() + " " + eenheid(h.van()) + "->" + eenheid(h.naar())
                    + " " + h.teller() + (h.noemer() == null ? "" : "/" + h.noemer()) + " (" + boom(h.term()) + ")";
            case Term.Deel d -> "Deel " + d.functie() + " sonde " + d.sonde() + " (" + boom(d.links()) + ", "
                    + boom(d.rechts()) + ")";
            case Term.Wortel w -> "Wortel " + w.functie() + " sonde " + w.sonde() + " (" + boom(w.radicand()) + ")";
            case Term.Faal f -> "Faal '" + f.zin() + "'";
            case Term.Vereist v -> "Vereist (" + boom(v.waarde()) + ") '" + v.zin() + "'";
            case Term.Tekstreeks s -> "Tekstreeks" + lijst(s.delen());
            case Term.GelijsteVouw g -> "GelijsteVouw " + g.functie() + lijst(g.operanden());
            case Term.Vouwlezing v -> "Vouwlezing " + v.deponeringen() + " " + v.sonde() + " " + v.filterfout() + " "
                    + v.lidfout() + " (" + boom(v.uitkomst()) + ")";
            case Term.Dagsoort d -> "Dagsoort " + d.naam();
            case Term.Samengesteld s -> "Samengesteld " + s.kwantor() + " " + s.drempel() + lijst(s.leden());
            case Term.Kwantorlezing k -> "Kwantorlezing " + k.functie() + " " + k.kwantor() + " " + k.drempel()
                    + " sonde " + k.sonde() + " (" + boom(k.aantal()) + ", " + boom(k.bijdragen()) + ")";
            case Term.ZonderFout z -> "ZonderFout(" + boom(z.voorwaarde()) + ")";
            case Term.GecompileerdeToets g -> "GecompileerdeToets '" + g.bron() + "'";
        };
    }

    /** Every node of {@code t} in pre-order, each as one line that opens with {@code naam}: its position (the root
     *  {@code plek}, the i-th child of the node at p at p.i), its kind, its source fragment where it carries one, and
     *  of a constant the class of its value and, where it has one, its whole IR type, as the type's record prints
     *  it. */
    static void velden(String naam, Term t, String plek, List<String> uit) {
        StringBuilder regel = new StringBuilder(naam + " " + plek + " " + t.getClass().getSimpleName());
        if (t instanceof Term.Const c)
            regel.append(" ").append(c.waarde() == null ? "null" : c.waarde().getClass().getSimpleName())
                    .append(c.type() == null ? "" : " type " + c.type());
        if (t.bron() != null) regel.append(" bron '").append(t.bron()).append("'");
        uit.add(regel.toString());
        List<Term> kinderen = kinderen(t);
        for (int i = 0; i < kinderen.size(); i++) velden(naam, kinderen.get(i), plek + "." + i, uit);
    }

    /** The children of {@code t}, in the order the node lists them. */
    static List<Term> kinderen(Term t) {
        return switch (t) {
            case Term.Const c -> List.of();
            case Term.Lees l -> List.of();
            case Term.Binder b -> List.of();
            case Term.Let l -> List.of(l.waarde(), l.lichaam());
            case Term.Keuze k -> List.of(k.voorwaarde(), k.dan(), k.anders());
            case Term.Aanroep a -> a.argumenten();
            case Term.Herschaal h -> List.of(h.term());
            case Term.Deel d -> List.of(d.links(), d.rechts());
            case Term.Wortel w -> List.of(w.radicand());
            case Term.Faal f -> List.of();
            case Term.Vereist v -> List.of(v.waarde());
            case Term.Tekstreeks s -> s.delen();
            case Term.GelijsteVouw g -> g.operanden();
            case Term.Vouwlezing v -> List.of(v.uitkomst());
            case Term.Dagsoort d -> List.of();
            case Term.Samengesteld s -> s.leden();
            case Term.Kwantorlezing k -> List.of(k.aantal(), k.bijdragen());
            case Term.ZonderFout z -> List.of(z.voorwaarde());
            case Term.GecompileerdeToets g -> List.of();
        };
    }

    private static String lijst(List<Term> termen) {
        return termen.stream().map(Termboom::boom).collect(Collectors.joining(", ", "(", ")"));
    }

    private static String waarde(Object w) {
        return w instanceof BigDecimal b ? b.toPlainString() : String.valueOf(w);
    }

    private static String eenheid(Eenheid e) {
        return e == null ? "-" : e.weergave();
    }
}
