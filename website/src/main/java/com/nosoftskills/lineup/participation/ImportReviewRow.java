package com.nosoftskills.lineup.participation;

public record ImportReviewRow(
        String scrapedName,
        String teamId,
        String teamName,
        String teamLocation,
        String formationTypeId,
        String newFormationType,
        String teamDisplay,
        String formationDisplay,
        ImportAction action) {
}
