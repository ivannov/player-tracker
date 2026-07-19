package com.nosoftskills.lineup.model;

public enum MatchEventType {
    GOAL("Гол"),
    PENALTY_GOAL("Гол (дузпа)"),
    OWN_GOAL("Автогол"),
    YELLOW_CARD("Жълт картон"),
    SECOND_YELLOW_CARD("Втори жълт картон"),
    RED_CARD("Червен картон");

    private final String displayLabel;

    MatchEventType(String displayLabel) {
        this.displayLabel = displayLabel;
    }

    public String getDisplayLabel() {
        return displayLabel;
    }
}
