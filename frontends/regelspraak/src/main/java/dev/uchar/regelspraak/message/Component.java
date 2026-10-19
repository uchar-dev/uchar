package dev.uchar.regelspraak.message;

/** A component that renders its messages from a catalogue, with the language it renders in unless told otherwise:
 *  the frontend in Dutch (RegelSpraak is Dutch), the Drools backend in English. */
public enum Component {
    FRONTEND("frontend", "nl"),
    DROOLS("drools", "en");

    final String resourceName;
    final String defaultLanguage;

    Component(String resourceName, String defaultLanguage) {
        this.resourceName = resourceName;
        this.defaultLanguage = defaultLanguage;
    }
}
