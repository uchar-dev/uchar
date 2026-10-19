package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.graph.ProducerConsumer.ElementKey;

/** How the logical facts of this branch print: a log of a session lists them, and a bean's own toString is Object's. */
final class FactNames {
    private FactNames() {}

    static String object(Object o) {
        return o.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(o));
    }

    static String member(ElementKey e) {
        return e.objectType() + "." + e.name();
    }
}
