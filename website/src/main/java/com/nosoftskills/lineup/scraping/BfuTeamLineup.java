package com.nosoftskills.lineup.scraping;

import java.util.List;

public record BfuTeamLineup(String teamName, List<BfuLineupEntry> starters, List<BfuLineupEntry> reserves) {
}
