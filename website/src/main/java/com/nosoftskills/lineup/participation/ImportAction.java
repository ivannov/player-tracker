package com.nosoftskills.lineup.participation;

public enum ImportAction {
    CREATE("Ще се създаде"),
    EXISTS("Вече съществува — ще бъде пропуснато"),
    SKIP("Липсват данни — редът ще бъде пропуснат");

    private final String label;

    ImportAction(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
