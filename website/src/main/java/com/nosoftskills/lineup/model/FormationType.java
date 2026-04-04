package com.nosoftskills.lineup.model;

public enum FormationType {
    U15("U15"),
    U16("U16"),
    U17("U17"),
    U18("U18"),
    U19("U19"),
    FIRST(""),
    SECOND("II"),
    THIRD("III");

    private final String label;

    FormationType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
