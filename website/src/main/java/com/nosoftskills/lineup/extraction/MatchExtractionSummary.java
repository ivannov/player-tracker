package com.nosoftskills.lineup.extraction;

/**
 * Outcome of {@link MatchExtractionService#confirm(ExtractionRequest)}: how many matches were
 * newly created vs. reused (safe re-run of the same date), and how many discovered rows were
 * skipped entirely because a team side (not a player) couldn't be resolved.
 */
public record MatchExtractionSummary(int matchesCreated, int matchesReused, int rowsSkipped) {
}
