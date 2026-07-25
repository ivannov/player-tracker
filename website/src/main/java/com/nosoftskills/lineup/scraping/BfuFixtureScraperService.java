package com.nosoftskills.lineup.scraping;

import jakarta.enterprise.context.ApplicationScoped;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.time.MonthDay;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds which matches were played on a given date, by scanning a bfu-tournaments.com
 * league "results" page (e.g. https://bfu-tournaments.com/leagues/first?season=2024-2025&view=past-matches),
 * which renders every round of the season statically in one page load.
 */
@ApplicationScoped
public class BfuFixtureScraperService {

    private static final String USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d+)\\s+([а-яА-Я]+)");
    private static final Map<String, Month> BULGARIAN_MONTHS = Map.ofEntries(
            Map.entry("януари", Month.JANUARY),
            Map.entry("февруари", Month.FEBRUARY),
            Map.entry("март", Month.MARCH),
            Map.entry("април", Month.APRIL),
            Map.entry("май", Month.MAY),
            Map.entry("юни", Month.JUNE),
            Map.entry("юли", Month.JULY),
            Map.entry("август", Month.AUGUST),
            Map.entry("септември", Month.SEPTEMBER),
            Map.entry("октомври", Month.OCTOBER),
            Map.entry("ноември", Month.NOVEMBER),
            Map.entry("декември", Month.DECEMBER));

    public List<BfuFixture> findMatches(String url, LocalDate date) throws BfuScraperException {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .referrer("https://bfu-tournaments.com")
                    .timeout(15_000)
                    .get();
            return parseFixtures(doc, date);
        } catch (IOException e) {
            throw new BfuScraperException("Failed to fetch bfu-tournaments.com results page: " + url, e);
        }
    }

    List<BfuFixture> parseFixtures(Document doc, LocalDate date) throws BfuScraperException {
        MonthDay target = MonthDay.from(date);
        List<BfuFixture> fixtures = new ArrayList<>();
        for (Element row : doc.select("tr:has(a[href*=stats/match])")) {
            Elements cells = row.select("> td");
            if (cells.size() < 4) {
                throw new BfuScraperException("Could not parse a fixture row on bfu-tournaments.com results page");
            }

            Element dateDd = cells.get(0).selectFirst("dd");
            if (dateDd == null) {
                throw new BfuScraperException("Could not find fixture date on bfu-tournaments.com results page");
            }
            if (!target.equals(parseRowDate(dateDd.text()))) {
                continue;
            }

            Element homeNameEl = cells.get(1).selectFirst("dd");
            Element matchLink = cells.get(2).selectFirst("a[href*=stats/match]");
            Element awayNameEl = cells.get(3).selectFirst("dd");
            if (homeNameEl == null || matchLink == null || awayNameEl == null) {
                throw new BfuScraperException("Could not parse a fixture row on bfu-tournaments.com results page");
            }
            fixtures.add(new BfuFixture(matchLink.attr("href"), homeNameEl.text().trim(), awayNameEl.text().trim()));
        }
        return fixtures;
    }

    MonthDay parseRowDate(String text) throws BfuScraperException {
        String normalized = text.replace(' ', ' ');
        Matcher matcher = DATE_PATTERN.matcher(normalized);
        if (!matcher.find()) {
            throw new BfuScraperException("Could not parse fixture date from '" + text + "' on bfu-tournaments.com results page");
        }
        int day = Integer.parseInt(matcher.group(1));
        Month month = BULGARIAN_MONTHS.get(matcher.group(2).toLowerCase(Locale.ROOT));
        if (month == null) {
            throw new BfuScraperException("Unknown Bulgarian month name '" + matcher.group(2) + "' on bfu-tournaments.com results page");
        }
        try {
            return MonthDay.of(month, day);
        } catch (java.time.DateTimeException e) {
            throw new BfuScraperException("Invalid fixture day '" + day + "' for month '" + matcher.group(2) + "' on bfu-tournaments.com results page", e);
        }
    }
}
