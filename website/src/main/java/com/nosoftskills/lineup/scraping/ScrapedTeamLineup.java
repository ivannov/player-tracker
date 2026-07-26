package com.nosoftskills.lineup.scraping;

import java.util.List;

public record ScrapedTeamLineup(String teamName, List<ScrapedLineupEntry> starters, List<ScrapedLineupEntry> reserves) {
}
