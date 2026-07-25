package com.nosoftskills.lineup.scraping;

import jakarta.enterprise.context.ApplicationScoped;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
public class BfuMatchScraperService {

    private static final String USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final String STARTERS_HEADING = "Стартов състав";
    private static final String RESERVES_HEADING = "Резервен състав";
    private static final Pattern MINUTE_PATTERN = Pattern.compile("(\\d+)(?:\\s*\\+\\s*(\\d+))?'");

    public BfuMatchData extractMatch(String url) throws BfuScraperException {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .referrer("https://bfu-tournaments.com")
                    .timeout(15_000)
                    .get();
            return parseMatch(doc);
        } catch (IOException e) {
            throw new BfuScraperException("Failed to fetch bfu-tournaments.com match page: " + url, e);
        }
    }

    BfuMatchData parseMatch(Document doc) throws BfuScraperException {
        Elements teamNameDivs = doc.select("div.font-bold.text-2xl");
        if (teamNameDivs.size() < 2) {
            throw new BfuScraperException("Could not find home/away team names on bfu-tournaments.com match page");
        }
        String homeTeamName = teamNameDivs.get(0).text().trim();
        String awayTeamName = teamNameDivs.get(1).text().trim();

        Element scoreDiv = doc.selectFirst("div.text-6xl.font-black");
        if (scoreDiv == null) {
            throw new BfuScraperException("Could not find match score on bfu-tournaments.com match page");
        }
        String[] scoreParts = scoreDiv.text().split("-");
        if (scoreParts.length != 2) {
            throw new BfuScraperException("Could not parse match score '" + scoreDiv.text() + "' on bfu-tournaments.com match page");
        }
        short homeScore = parseScorePart(scoreParts[0]);
        short awayScore = parseScorePart(scoreParts[1]);

        Elements startersColumns = lineupColumns(doc, STARTERS_HEADING);
        Elements reservesColumns = lineupColumns(doc, RESERVES_HEADING);

        BfuTeamLineup home = new BfuTeamLineup(homeTeamName,
                parseLineupColumn(startersColumns.get(0)), parseLineupColumn(reservesColumns.get(0)));
        BfuTeamLineup away = new BfuTeamLineup(awayTeamName,
                parseLineupColumn(startersColumns.get(1)), parseLineupColumn(reservesColumns.get(1)));

        List<BfuMatchEvent> events = new ArrayList<>();
        List<BfuSubstitution> substitutions = new ArrayList<>();
        parseTimeline(doc, events, substitutions);

        return new BfuMatchData(home, away, homeScore, awayScore, events, substitutions);
    }

    private short parseScorePart(String text) throws BfuScraperException {
        try {
            return Short.parseShort(text.trim());
        } catch (NumberFormatException e) {
            throw new BfuScraperException("Could not parse score value '" + text + "' on bfu-tournaments.com match page", e);
        }
    }

    private Elements lineupColumns(Document doc, String sectionHeading) throws BfuScraperException {
        Element heading = null;
        for (Element h3 : doc.select("h3")) {
            if (sectionHeading.equals(h3.text().trim())) {
                heading = h3;
                break;
            }
        }
        if (heading == null) {
            throw new BfuScraperException("Could not find '" + sectionHeading + "' section on bfu-tournaments.com match page");
        }
        Element container = heading.nextElementSibling();
        Elements columns = container == null ? new Elements() : container.select("> div:has(a[href*=stats/player])");
        if (columns.size() < 2) {
            throw new BfuScraperException("Could not find both lineup columns under '" + sectionHeading + "' on bfu-tournaments.com match page");
        }
        return columns;
    }

    private List<BfuLineupEntry> parseLineupColumn(Element column) throws BfuScraperException {
        List<BfuLineupEntry> entries = new ArrayList<>();
        for (Element playerLink : column.select("a[href*=stats/player]")) {
            Element numberDiv = playerLink.selectFirst("div.font-black");
            Element nameDiv = playerLink.selectFirst("div.font-bold");
            if (numberDiv == null || nameDiv == null) {
                throw new BfuScraperException("Could not parse a lineup entry on bfu-tournaments.com match page");
            }
            String numberText = numberDiv.text().trim();
            int number;
            try {
                number = Integer.parseInt(numberText);
            } catch (NumberFormatException e) {
                throw new BfuScraperException("Could not parse player number '" + numberText + "' on bfu-tournaments.com match page", e);
            }
            entries.add(new BfuLineupEntry(number, nameDiv.text().trim()));
        }
        return entries;
    }

    private void parseTimeline(Document doc, List<BfuMatchEvent> events, List<BfuSubstitution> substitutions) throws BfuScraperException {
        for (Element eventDiv : doc.select("div[x-data=\"{ hover: false }\"]")) {
            Element flexCol = eventDiv.selectFirst("div.flex.flex-col.justify-center");
            Element icon = eventDiv.selectFirst("img");
            Element playerLink = eventDiv.selectFirst("a[href*=stats/player]");
            if (flexCol == null || icon == null || playerLink == null) {
                throw new BfuScraperException("Could not parse a match timeline entry on bfu-tournaments.com match page");
            }
            boolean home = !flexCol.hasClass("flex-col-reverse");
            int minute = parseMinute(flexCol.text());
            String iconSrc = icon.attr("src");

            Element inNameDiv = playerLink.selectFirst("div.font-bold");
            Element outNameDiv = playerLink.selectFirst("div.text-xs.font-normal");
            if (inNameDiv == null) {
                throw new BfuScraperException("Could not find a player name for a match timeline entry on bfu-tournaments.com match page");
            }

            if (outNameDiv != null) {
                substitutions.add(new BfuSubstitution(home, minute, inNameDiv.text().trim(), outNameDiv.text().trim()));
                continue;
            }

            BfuMatchEventType type;
            if (iconSrc.contains("second-yellow-card")) {
                type = BfuMatchEventType.SECOND_YELLOW_CARD;
            } else if (iconSrc.contains("red-card")) {
                type = BfuMatchEventType.RED_CARD;
            } else if (iconSrc.contains("yellow-card")) {
                type = BfuMatchEventType.YELLOW_CARD;
            } else if (iconSrc.contains("goal")) {
                type = BfuMatchEventType.GOAL;
            } else {
                throw new BfuScraperException("Unknown match timeline icon '" + iconSrc + "' on bfu-tournaments.com match page");
            }
            events.add(new BfuMatchEvent(home, type, minute, inNameDiv.text().trim()));
        }
    }

    int parseMinute(String text) throws BfuScraperException {
        Matcher matcher = MINUTE_PATTERN.matcher(text);
        if (!matcher.find()) {
            throw new BfuScraperException("Could not parse event minute from '" + text + "' on bfu-tournaments.com match page");
        }
        int base = Integer.parseInt(matcher.group(1));
        int extra = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 0;
        return base + extra;
    }
}
