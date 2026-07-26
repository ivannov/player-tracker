package com.nosoftskills.lineup.scraping;

public record ScrapedMatchEvent(boolean home, ScrapedMatchEventType type, int minute, String playerName) {
}
