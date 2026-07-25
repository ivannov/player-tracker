package com.nosoftskills.lineup.scraping;

import jakarta.enterprise.context.ApplicationScoped;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class EbfuMatchLineupScraperService {

    private static final String TEAMS_URL = "https://ebfu.net/teams/";
    private static final String USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final String GOALKEEPER_TAG = "ВРАТАР";
    private static final String CAPTAIN_TAG = "КАПИТАН";

    public EbfuMatchLineup extractLineup(LocalDate date, String homeTeamName, String awayTeamName) throws EbfuScraperException {
        try {
            Document dayListDoc = Jsoup.connect(TEAMS_URL)
                    .userAgent(USER_AGENT)
                    .referrer(TEAMS_URL)
                    .timeout(15_000)
                    .method(Connection.Method.POST)
                    .data("curDate", date.toString())
                    .data("selectDateBtn", "1")
                    .post();

            String matchId = findMatchId(dayListDoc, homeTeamName, awayTeamName);

            Document lineupDoc = Jsoup.connect(TEAMS_URL)
                    .userAgent(USER_AGENT)
                    .referrer(TEAMS_URL)
                    .timeout(15_000)
                    .method(Connection.Method.POST)
                    .data("curDate", date.toString())
                    .data("showMatchBtn", matchId)
                    .post();

            return parseLineup(lineupDoc);
        } catch (IOException e) {
            throw new EbfuScraperException(
                    "Failed to fetch ebfu.net lineup for " + homeTeamName + " vs " + awayTeamName, e);
        }
    }

    String findMatchId(Document dayListDoc, String homeTeamName, String awayTeamName) throws EbfuScraperException {
        List<Element> candidates = new ArrayList<>();
        for (Element button : dayListDoc.select("input[name=showMatchBtn]")) {
            Element card = button.closest("td");
            if (card == null) {
                continue;
            }
            Element teamTable = card.selectFirst("table.teamTable");
            if (teamTable == null) {
                continue;
            }
            Element topRow = teamTable.selectFirst("tr");
            if (topRow == null) {
                continue;
            }
            Elements cells = topRow.select("> td");
            if (cells.size() < 4) {
                continue;
            }
            String cardHome = textOf(cells.get(1).selectFirst("div.teamName"));
            String cardAway = textOf(cells.get(3).selectFirst("div.teamName"));
            if (namesMatch(cardHome, homeTeamName) && namesMatch(cardAway, awayTeamName)) {
                candidates.add(button);
            }
        }
        if (candidates.isEmpty()) {
            throw new EbfuScraperException(
                    "No match found on ebfu.net day list for " + homeTeamName + " vs " + awayTeamName);
        }
        if (candidates.size() > 1) {
            throw new EbfuScraperException(
                    "Multiple ambiguous matches found on ebfu.net day list for " + homeTeamName + " vs " + awayTeamName);
        }
        return candidates.get(0).attr("value");
    }

    EbfuMatchLineup parseLineup(Document lineupDoc) throws EbfuScraperException {
        Elements sideContainers = lineupDoc.select("td[width=450]");
        if (sideContainers.size() < 2) {
            throw new EbfuScraperException("Could not find both team columns on ebfu.net match page");
        }
        EbfuTeamLineup home = parseTeamLineup(sideContainers.get(0));
        EbfuTeamLineup away = parseTeamLineup(sideContainers.get(1));
        return new EbfuMatchLineup(home, away);
    }

    private EbfuTeamLineup parseTeamLineup(Element sideContainer) throws EbfuScraperException {
        Elements tables = sideContainer.select("> table");
        if (tables.size() < 2) {
            throw new EbfuScraperException("Could not find starters/reserves tables for a team on ebfu.net match page");
        }
        Element startersTable = tables.get(0);
        Element reservesTable = tables.get(1);

        Element teamNameCell = startersTable.selectFirst("tr td");
        if (teamNameCell == null) {
            throw new EbfuScraperException("Could not find team name on ebfu.net match page");
        }
        String teamName = textOf(teamNameCell);

        List<EbfuLineupEntry> starters = parseEntries(startersTable);
        List<EbfuLineupEntry> reserves = parseEntries(reservesTable);

        String headCoach = null;
        Element coachNameSpan = sideContainer.selectFirst("span.robotoBlackB20");
        if (coachNameSpan != null) {
            headCoach = textOf(coachNameSpan);
        }

        return new EbfuTeamLineup(teamName, starters, reserves, headCoach);
    }

    private List<EbfuLineupEntry> parseEntries(Element table) throws EbfuScraperException {
        List<EbfuLineupEntry> entries = new ArrayList<>();
        Elements rows = table.select("tr");
        for (int i = 1; i < rows.size(); i++) {
            Elements cells = rows.get(i).select("td");
            if (cells.size() < 2) {
                continue;
            }
            String numberText = textOf(cells.get(0));
            int number;
            try {
                number = Integer.parseInt(numberText);
            } catch (NumberFormatException e) {
                throw new EbfuScraperException("Could not parse player number '" + numberText + "' on ebfu.net match page", e);
            }
            entries.add(parseEntry(number, textOf(cells.get(1))));
        }
        return entries;
    }

    EbfuLineupEntry parseEntry(int number, String rawText) {
        String[] parts = rawText.split(" - ");
        String name = parts[0].trim();
        boolean goalkeeper = false;
        boolean captain = false;
        for (int i = 1; i < parts.length; i++) {
            String tag = parts[i].trim();
            if (tag.equalsIgnoreCase(GOALKEEPER_TAG)) {
                goalkeeper = true;
            } else if (tag.equalsIgnoreCase(CAPTAIN_TAG)) {
                captain = true;
            }
        }
        return new EbfuLineupEntry(number, name, goalkeeper, captain);
    }

    private boolean namesMatch(String scraped, String provided) {
        if (scraped == null || provided == null) {
            return false;
        }
        String a = scraped.toLowerCase();
        String b = provided.toLowerCase();
        return a.contains(b) || b.contains(a);
    }

    private String textOf(Element element) {
        return element == null ? "" : element.text().trim();
    }
}
