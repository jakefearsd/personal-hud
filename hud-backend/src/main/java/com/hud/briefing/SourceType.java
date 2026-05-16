package com.hud.briefing;

/**
 * How a {@link NewsSource}'s links are discovered.
 * RSS  — the url is an RSS/Atom feed parsed for article links.
 * ISW  — Institute for the Study of War; the url holds a theater keyword
 *        ("ukraine", "mideast", "global") used to filter the publications index.
 * CSIS — Center for Strategic and International Studies analysis index.
 */
public enum SourceType {
    RSS,
    ISW,
    CSIS
}
