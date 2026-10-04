package com.nosoftskills.lineup.scraping;

import jakarta.enterprise.context.ApplicationScoped;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.List;

@ApplicationScoped
public class BfuLeagueScraperService {

    public List<String> extractTeamNames(String url) throws BfuScraperException {
        try {
            Document doc = BfuHttp.get(url);
            return parseTeamNames(doc);
        } catch (IOException e) {
            throw new BfuScraperException("Failed to fetch BFU page: " + url + " (" + e.getMessage() + ")", e);
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
