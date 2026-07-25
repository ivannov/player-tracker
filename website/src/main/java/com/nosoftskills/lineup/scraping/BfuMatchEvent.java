package com.nosoftskills.lineup.scraping;

public record BfuMatchEvent(boolean home, BfuMatchEventType type, int minute, String playerName) {
}
