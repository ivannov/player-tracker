package com.nosoftskills.lineup.scraping;

import jakarta.enterprise.context.ApplicationScoped;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.List;

@ApplicationScoped
public class BfuLeagueScraperService {

    public List<String> extractTeamNames(String url) throws BfuScraperException {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .referrer("https://bfu-tournaments.com")
                    .timeout(15_000)
                    .get();
            return parseTeamNames(doc);
        } catch (IOException e) {
            throw new BfuScraperException("Failed to fetch BFU page: " + url, e);
        }
    }

    List<String> parseTeamNames(Document doc) throws BfuScraperException {
        List<String> names = doc.select("img[alt=team logo] ~ span")
                .stream()
                .map(el -> el.text())
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();
        if (names.isEmpty()) {
            throw new BfuScraperException("No team names found — check the CSS selector or URL format");
        }
        return names;
    }
}
