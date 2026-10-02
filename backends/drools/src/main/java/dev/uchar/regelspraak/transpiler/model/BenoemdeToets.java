package dev.uchar.regelspraak.transpiler.model;

import dev.uchar.regelspraak.ir.Conditie;
import dev.uchar.regelspraak.runtime.Breuk;

import org.drools.model.functions.Predicate1;
import org.drools.model.functions.Predicate2;
import org.drools.model.functions.Predicate3;
import org.drools.model.functions.Predicate4;
import org.drools.model.functions.Predicate5;
import org.drools.model.functions.PredicateInformation;

/**
 * The predicate of a condition's constraint with the rule it belongs to. A condition is tested in a constraint and not in
 * a consequence, so what it throws leaves the fire as Drools' {@code ConstraintEvaluationException}, whose message Drools
 * builds from the predicate's {@link PredicateInformation}: a bare lambda carries none, and the exception read "Error
 * evaluating constraint '' in  and in more rules" (measured 2026-10-02 on a division by a numeric zero in a condition).
 * These predicates carry the condition's source text and the rule's name, so Drools names both, and a division by a
 * numeric zero inside the test leaves it named after the rule ({@link Breuk.DelingDoorNul#inDeRegel}), the sentence a
 * division in a consequence gets from {@code DroolsBackend.benoem}. A division named already passes as it is.
 */
final class BenoemdeToets {

    private BenoemdeToets() {}

    private static PredicateInformation informatie(String regel, String pakket, Conditie c) {
        String tekst = c == null || c.bron() == null || c.bron().snippet() == null ? "" : c.bron().snippet();
        return new PredicateInformation(tekst, regel, pakket);
    }

    private static Breuk.DelingDoorNul benoemd(Breuk.DelingDoorNul nul, String regel) {
        return nul.regel() != null ? nul : nul.inDeRegel(regel, nul);
    }

    /** {@code p} with the rule {@code regel} of package {@code pakket} and the condition {@code c}; {@code p} itself
     *  where no rule is being lowered. */
    static <A> Predicate1<A> een(Predicate1<A> p, String regel, String pakket, Conditie c) {
        return regel == null ? p : new Een<>(p, regel, informatie(regel, pakket, c));
    }

    static <A, B> Predicate2<A, B> twee(Predicate2<A, B> p, String regel, String pakket, Conditie c) {
        return regel == null ? p : new Twee<>(p, regel, informatie(regel, pakket, c));
    }

    static <A, B, C> Predicate3<A, B, C> drie(Predicate3<A, B, C> p, String regel, String pakket, Conditie c) {
        return regel == null ? p : new Drie<>(p, regel, informatie(regel, pakket, c));
    }

    static <A, B, C, D> Predicate4<A, B, C, D> vier(Predicate4<A, B, C, D> p, String regel, String pakket, Conditie c) {
        return regel == null ? p : new Vier<>(p, regel, informatie(regel, pakket, c));
    }

    static <A, B, C, D, E> Predicate5<A, B, C, D, E> vijf(Predicate5<A, B, C, D, E> p, String regel, String pakket,
                                                          Conditie c) {
        return regel == null ? p : new Vijf<>(p, regel, informatie(regel, pakket, c));
    }

    private record Een<A>(Predicate1<A> p, String regel, PredicateInformation predicateInformation)
            implements Predicate1<A> {
        @Override public boolean test(A a) throws Exception {
            try { return p.test(a); } catch (Breuk.DelingDoorNul nul) { throw benoemd(nul, regel); }
        }
    }

    private record Twee<A, B>(Predicate2<A, B> p, String regel, PredicateInformation predicateInformation)
            implements Predicate2<A, B> {
        @Override public boolean test(A a, B b) throws Exception {
            try { return p.test(a, b); } catch (Breuk.DelingDoorNul nul) { throw benoemd(nul, regel); }
        }
    }

    private record Drie<A, B, C>(Predicate3<A, B, C> p, String regel, PredicateInformation predicateInformation)
            implements Predicate3<A, B, C> {
        @Override public boolean test(A a, B b, C c) throws Exception {
            try { return p.test(a, b, c); } catch (Breuk.DelingDoorNul nul) { throw benoemd(nul, regel); }
        }
    }

    private record Vier<A, B, C, D>(Predicate4<A, B, C, D> p, String regel, PredicateInformation predicateInformation)
            implements Predicate4<A, B, C, D> {
        @Override public boolean test(A a, B b, C c, D d) throws Exception {
            try { return p.test(a, b, c, d); } catch (Breuk.DelingDoorNul nul) { throw benoemd(nul, regel); }
        }
    }

    private record Vijf<A, B, C, D, E>(Predicate5<A, B, C, D, E> p, String regel, PredicateInformation predicateInformation)
            implements Predicate5<A, B, C, D, E> {
        @Override public boolean test(A a, B b, C c, D d, E e) throws Exception {
            try { return p.test(a, b, c, d, e); } catch (Breuk.DelingDoorNul nul) { throw benoemd(nul, regel); }
        }
    }
}
