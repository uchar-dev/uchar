package dev.uchar.regelspraak.transpiler.tms;

import dev.uchar.regelspraak.graaf.ProducentConsument.ElementSleutel;

/** How the logical facts of this branch print: a log of a session lists them, and a bean's own toString is Object's. */
final class Feitnamen {
    private Feitnamen() {}

    static String object(Object o) {
        return o.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(o));
    }

    static String lid(ElementSleutel e) {
        return e.objecttype() + "." + e.naam();
    }
}
