package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.model.Unit;
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
            case Term.Const c -> "Const " + value(c.value())
                    + (c.type() == null || c.type().unitOf() == null ? "" : " " + c.type().unitOf().rendering());
            case Term.SlotValue l -> "Lees " + l.slot();
            case Term.Input i -> "Invoer " + i.field().javaClass().getSimpleName() + "." + i.field().name() + " " + i.form()
                    + " (" + tree(i.object()) + ")";
            case Term.Binder b -> "Binder " + b.slot() + " " + b.name();
            case Term.Let l -> "Let " + l.slot() + " " + l.name() + (l.eager() ? " gretig" : "") + " [" + tree(l.value())
                    + "] in [" + tree(l.body()) + "]";
            case Term.Choice k -> "Keuze(" + tree(k.condition()) + ", " + tree(k.then()) + ", " + tree(k.otherwise()) + ")";
            case Term.Call a -> a.function() + list(a.arguments());
            case Term.Restate h -> "Herschaal " + h.function() + " " + unit(h.from()) + "->" + unit(h.destination())
                    + " " + h.counter() + (h.denominator() == null ? "" : "/" + h.denominator()) + " (" + tree(h.term()) + ")";
            case Term.Division d -> "Deel " + d.function() + " sonde " + d.depositFlag() + " (" + tree(d.left()) + ", "
                    + tree(d.right()) + ")";
            case Term.SquareRoot w -> "Wortel " + w.function() + " sonde " + w.depositFlag() + " (" + tree(w.radicand()) + ")";
            case Term.Fail f -> "Faal '" + f.sentence() + "'";
            case Term.Required v -> "Vereist (" + tree(v.value()) + ") '" + v.sentence() + "'";
            case Term.TextComposition s -> "Tekstreeks" + list(s.parts());
            case Term.ListedFold g -> "GelijsteVouw " + g.function() + list(g.operands());
            case Term.FoldRead v -> "Vouwlezing " + v.deposits() + " " + v.depositFlag() + " " + v.filterFault() + " "
                    + v.memberFault() + " (" + tree(v.outcome()) + ")";
            case Term.DayKind d -> "Dagsoort " + d.name();
            case Term.Compound s -> "Samengesteld " + s.quantifier() + " " + s.threshold() + list(s.members());
            case Term.QuantifierRead k -> "Kwantorlezing " + k.function() + " " + k.quantifier() + " " + k.threshold()
                    + " sonde " + k.depositFlag() + " (" + tree(k.count()) + ", " + tree(k.contributions()) + ")";
            case Term.WithoutFault z -> "ZonderFout(" + tree(z.condition()) + ")";
        };
    }

    /** Every node of {@code t} in pre-order, each as one line that opens with {@code naam}: its position (the root
     *  {@code site}, the i-th child of the node at p at p.i), its kind, its source fragment where it carries one, and
     *  of a constant the class of its value and, where it has one, its whole IR type, as the type's record prints
     *  it. */
    static void fields(String name, Term t, String site, List<String> out) {
        StringBuilder rule = new StringBuilder(name + " " + site + " " + t.getClass().getSimpleName());
        if (t instanceof Term.Const c)
            rule.append(" ").append(c.value() == null ? "null" : c.value().getClass().getSimpleName())
                    .append(c.type() == null ? "" : " type " + c.type());
        if (t.source() != null) rule.append(" bron '").append(t.source()).append("'");
        out.add(rule.toString());
        List<Term> children = children(t);
        for (int i = 0; i < children.size(); i++) fields(name, children.get(i), site + "." + i, out);
    }

    /** The children of {@code t}, in the order the node lists them. */
    static List<Term> children(Term t) {
        return switch (t) {
            case Term.Const c -> List.of();
            case Term.SlotValue l -> List.of();
            case Term.Input i -> List.of(i.object());
            case Term.Binder b -> List.of();
            case Term.Let l -> List.of(l.value(), l.body());
            case Term.Choice k -> List.of(k.condition(), k.then(), k.otherwise());
            case Term.Call a -> a.arguments();
            case Term.Restate h -> List.of(h.term());
            case Term.Division d -> List.of(d.left(), d.right());
            case Term.SquareRoot w -> List.of(w.radicand());
            case Term.Fail f -> List.of();
            case Term.Required v -> List.of(v.value());
            case Term.TextComposition s -> s.parts();
            case Term.ListedFold g -> g.operands();
            case Term.FoldRead v -> List.of(v.outcome());
            case Term.DayKind d -> List.of();
            case Term.Compound s -> s.members();
            case Term.QuantifierRead k -> List.of(k.count(), k.contributions());
            case Term.WithoutFault z -> List.of(z.condition());
        };
    }

    private static String list(List<Term> terms) {
        return terms.stream().map(TermTree::tree).collect(Collectors.joining(", ", "(", ")"));
    }

    private static String value(Object w) {
        return w instanceof BigDecimal b ? b.toPlainString() : String.valueOf(w);
    }

    private static String unit(Unit e) {
        return e == null ? "-" : e.rendering();
    }
}
