package dev.uchar.regelspraak.cases;

import dev.uchar.regelspraak.ir.Expr;
import dev.uchar.regelspraak.ir.PrioritizedDefaults;
import dev.uchar.regelspraak.ir.ResolvedObjectCreation;
import dev.uchar.regelspraak.ir.ResolvedRule;
import dev.uchar.regelspraak.model.Datatype;
import dev.uchar.regelspraak.model.RegelSpraakModel;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The forms of a sum type no executor carries (RUNTIME-SEMANTICS ch. 9.4, "Forms not carried"), the one place that lists
 * them: the build refuses a document that holds one before it builds anything, naming the first form of {@link #SLUGS}
 * it holds ({@code DroolsBackend.build}, {@code DROOLS-373}), as the reference interpreter and the ASP backend refuse it.
 * The loader refuses the forms it does not carry in any document before this list is read, a {@code Match} among them.
 */
final class SumTypeForms {
    private SumTypeForms() {}

    /** The slugs of the table of ch. 9.4, in its order, of which the first a document holds is named. */
    static final List<String> SLUGS = List.of("timeline", "dimension", "creation", "payload-unit", "defaults-group");

    /** The first slug of {@link #SLUGS} whose form {@code model} holds, with its defaults groups {@code defaults}; null
     *  where it holds none. */
    static String first(RegelSpraakModel model, List<PrioritizedDefaults> defaults) {
        Set<String> found = new HashSet<>();
        for (RegelSpraakModel.ObjectType t : model.objectTypes.values())
            for (Map.Entry<String, Datatype> a : t.attributes.entrySet())
                if (a.getValue() instanceof Datatype.SumType) {
                    if (t.attributeGranularity.get(a.getKey()) != null) found.add("timeline");
                    List<String> dimensions = t.attributeDimensions.get(a.getKey());
                    if (dimensions != null && !dimensions.isEmpty()) found.add("dimension");
                }
        for (RegelSpraakModel.Parameter p : model.parameters.values())
            if (p.datatype() instanceof Datatype.SumType && p.granularity() != null) found.add("timeline");
        // an attribute and a characteristic may share a name (IR-SPEC WF-002): only an attribute's value counts
        for (ResolvedObjectCreation o : model.objectCreations)
            for (ResolvedObjectCreation.MemberAssignment a : o.assignments())
                if (!a.characteristic() && o.createdType() != null && sum(o.createdType(), a.attribute()))
                    found.add("creation");
        for (Datatype d : model.domains.values())
            if (d instanceof Datatype.SumType s)
                for (Datatype.Constructor c : s.constructors())
                    if (c.payload() != null && c.payload().unitOf() != null) found.add("payload-unit");
        for (PrioritizedDefaults p : defaults)
            if (!p.isCharacteristic()) {
                RegelSpraakModel.ObjectType subject = model.objectTypeByName(p.subject());
                if (subject != null && sum(subject, p.target())) found.add("defaults-group");
            }
        // an initialisation is the lowest default of a defaults group on its target (ch. 7.3), with or without a
        // condition or a validity
        for (ResolvedRule r : model.resolvedRules)
            if (r.kind() == ResolvedRule.Kind.INITIALISATIE && r.target() instanceof Expr.AttributeReference a
                    && a.objectType() != null && sum(a.objectType(), a.attribute()))
                found.add("defaults-group");
        for (String slug : SLUGS) if (found.contains(slug)) return slug;
        return null;
    }

    private static boolean sum(RegelSpraakModel.ObjectType type, String attribute) {
        return attribute != null && type.attribute(attribute) instanceof Datatype.SumType;
    }
}
