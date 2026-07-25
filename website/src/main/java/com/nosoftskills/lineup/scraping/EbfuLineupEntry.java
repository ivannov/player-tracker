package com.nosoftskills.lineup.scraping;

public record EbfuLineupEntry(int number, String rawName, boolean goalkeeper, boolean captain) {
}
