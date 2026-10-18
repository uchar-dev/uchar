package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.model.Eenheid;
import dev.uchar.regelspraak.transpiler.tms.plan.Term;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/** A planned value or condition ({@link Term}) as text, for the witnesses that pin the plan node by node. */
final class TermTree {

    private TermTree() {}

    /** {@code t} as text: every node by its kind, with the fields the analysis decided. */
    static String tree(Term t) {
        return switch (t) {
            case Term.Const c -> "Const " + waarde(c.waarde())
                    + (c.type() == null || c.type().eenheidOf() == null ? "" : " " + c.type().eenheidOf().weergave());
            case Term.SlotValue l -> "Lees " + l.slot();
            case Term.Invoer i -> "Invoer " + i.veld().javaClass().getSimpleName() + "." + i.veld().naam() + " " + i.vorm()
                    + " (" + tree(i.object()) + ")";
            case Term.Binder b -> "Binder " + b.slot() + " " + b.naam();
            case Term.Let l -> "Let " + l.slot() + " " + l.naam() + (l.eager() ? " gretig" : "") + " [" + tree(l.waarde())
                    + "] in [" + tree(l.body()) + "]";
            case Term.Choice k -> "Keuze(" + tree(k.voorwaarde()) + ", " + tree(k.then()) + ", " + tree(k.otherwise()) + ")";
            case Term.Call a -> a.function() + lijst(a.arguments());
            case Term.Restate h -> "Herschaal " + h.function() + " " + unit(h.van()) + "->" + unit(h.naar())
                    + " " + h.counter() + (h.denominator() == null ? "" : "/" + h.denominator()) + " (" + tree(h.term()) + ")";
            case Term.Division d -> "Deel " + d.function() + " sonde " + d.depositFlag() + " (" + tree(d.left()) + ", "
                    + tree(d.right()) + ")";
            case Term.SquareRoot w -> "Wortel " + w.function() + " sonde " + w.depositFlag() + " (" + tree(w.radicand()) + ")";
            case Term.Fail f -> "Faal '" + f.sentence() + "'";
            case Term.Required v -> "Vereist (" + tree(v.waarde()) + ") '" + v.sentence() + "'";
            case Term.TextComposition s -> "Tekstreeks" + lijst(s.parts());
            case Term.ListedFold g -> "GelijsteVouw " + g.function() + lijst(g.operands());
            case Term.FoldRead v -> "Vouwlezing " + v.depositCount() + " " + v.depositFlag() + " " + v.filterFault() + " "
                    + v.memberFault() + " (" + tree(v.outcome()) + ")";
            case Term.DayKind d -> "Dagsoort " + d.naam();
            case Term.Compound s -> "Samengesteld " + s.quantifier() + " " + s.threshold() + lijst(s.members());
            case Term.QuantifierRead k -> "Kwantorlezing " + k.function() + " " + k.quantifier() + " " + k.threshold()
                    + " sonde " + k.depositFlag() + " (" + tree(k.aantal()) + ", " + tree(k.contributions()) + ")";
            case Term.WithoutFault z -> "ZonderFout(" + tree(z.voorwaarde()) + ")";
        };
    }

    /** Every node of {@code t} in pre-order, each as one line that opens with {@code naam}: its position (the root
     *  {@code site}, the i-th child of the node at p at p.i), its kind, its source fragment where it carries one, and
     *  of a constant the class of its value and, where it has one, its whole IR type, as the type's record prints
     *  it. */
    static void fields(String naam, Term t, String site, List<String> out) {
        StringBuilder regel = new StringBuilder(naam + " " + site + " " + t.getClass().getSimpleName());
        if (t instanceof Term.Const c)
            regel.append(" ").append(c.waarde() == null ? "null" : c.waarde().getClass().getSimpleName())
                    .append(c.type() == null ? "" : " type " + c.type());
        if (t.bron() != null) regel.append(" bron '").append(t.bron()).append("'");
        out.add(regel.toString());
        List<Term> children = children(t);
        for (int i = 0; i < children.size(); i++) fields(naam, children.get(i), site + "." + i, out);
    }

    /** The children of {@code t}, in the order the node lists them. */
    static List<Term> children(Term t) {
        return switch (t) {
            case Term.Const c -> List.of();
            case Term.SlotValue l -> List.of();
            case Term.Invoer i -> List.of(i.object());
            case Term.Binder b -> List.of();
            case Term.Let l -> List.of(l.waarde(), l.body());
            case Term.Choice k -> List.of(k.voorwaarde(), k.then(), k.otherwise());
            case Term.Call a -> a.arguments();
            case Term.Restate h -> List.of(h.term());
            case Term.Division d -> List.of(d.left(), d.right());
            case Term.SquareRoot w -> List.of(w.radicand());
            case Term.Fail f -> List.of();
            case Term.Required v -> List.of(v.waarde());
            case Term.TextComposition s -> s.parts();
            case Term.ListedFold g -> g.operands();
            case Term.FoldRead v -> List.of(v.outcome());
            case Term.DayKind d -> List.of();
            case Term.Compound s -> s.members();
            case Term.QuantifierRead k -> List.of(k.aantal(), k.contributions());
            case Term.WithoutFault z -> List.of(z.voorwaarde());
        };
    }

    private static String lijst(List<Term> terms) {
        return terms.stream().map(TermTree::tree).collect(Collectors.joining(", ", "(", ")"));
    }

    private static String waarde(Object w) {
        return w instanceof BigDecimal b ? b.toPlainString() : String.valueOf(w);
    }

    private static String unit(Eenheid e) {
        return e == null ? "-" : e.weergave();
    }
}
