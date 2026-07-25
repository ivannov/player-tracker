package com.nosoftskills.lineup.scraping;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.Month;
import java.time.MonthDay;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BfuFixtureScraperServiceTest {

    private final BfuFixtureScraperService service = new BfuFixtureScraperService();

    private Document fixture(String name) throws IOException {
        InputStream html = getClass().getResourceAsStream("/fixtures/" + name);
        return Jsoup.parse(html, "UTF-8", "https://bfu-tournaments.com/");
    }

    @Test
    void findsMatchesPlayedOnGivenDateInTheDefaultOpenRound() throws Exception {
        List<BfuFixture> fixtures = service.parseFixtures(fixture("bfu-results-page.html"), LocalDate.of(2025, 5, 19));

        assertThat(fixtures, hasSize(2));
        assertThat(fixtures, hasItem(new BfuFixture("https://bfu-tournaments.com/stats/match/15601",
                "ФК Централен Спортен Клуб на Армията 1948", "ФК Крумовград")));
        assertThat(fixtures, hasItem(new BfuFixture("https://bfu-tournaments.com/stats/match/15602",
                "ПФК Славия 1913", "ПФК ЛОКОМОТИВ ПЛОВДИВ 1926 АД")));
    }

    @Test
    void findsMatchesPlayedOnGivenDateInACollapsedEarlierRound() throws Exception {
        List<BfuFixture> fixtures = service.parseFixtures(fixture("bfu-results-page.html"), LocalDate.of(2025, 5, 15));

        assertThat(fixtures, hasSize(3));
        assertThat(fixtures, hasItem(new BfuFixture("https://bfu-tournaments.com/stats/match/15594",
                "ФК Крумовград", "ПФК Славия 1913")));
    }

    @Test
    void returnsEmptyListWhenNothingWasPlayedOnThatDate() throws Exception {
        List<BfuFixture> fixtures = service.parseFixtures(fixture("bfu-results-page.html"), LocalDate.of(2025, 1, 1));

        assertThat(fixtures, empty());
    }

    @Test
    void parseRowDateHandlesNonBreakingSpaceAndReturnsMonthDay() throws Exception {
        MonthDay parsed = service.parseRowDate("19 май, 20:15");

        assertThat(parsed, is(MonthDay.of(Month.MAY, 19)));
    }

    @Test
    void parseRowDateThrowsOnUnparseableText() {
        BfuScraperException ex = assertThrows(BfuScraperException.class, () -> service.parseRowDate("some garbage"));
        assertThat(ex.getMessage(), is("Could not parse fixture date from 'some garbage' on bfu-tournaments.com results page"));
    }

    @Test
    void parseRowDateThrowsOnUnknownMonthName() {
        BfuScraperException ex = assertThrows(BfuScraperException.class, () -> service.parseRowDate("19 фуу, 20:15"));
        assertThat(ex.getMessage(), is("Unknown Bulgarian month name 'фуу' on bfu-tournaments.com results page"));
    }

    @Test
    void parseFixturesThrowsOnMalformedRow() {
        String html = """
                <table><tr>
                <td><dd>19&nbsp;май,&nbsp;20:15</dd></td>
                <td><dd>Home Team</dd></td>
                <td><a href="https://bfu-tournaments.com/stats/match/1">1 : 0</a></td>
                </tr></table>
                """;
        Document doc = Jsoup.parse(html);

        BfuScraperException ex = assertThrows(BfuScraperException.class,
                () -> service.parseFixtures(doc, LocalDate.of(2025, 5, 19)));
        assertThat(ex.getMessage(), is("Could not parse a fixture row on bfu-tournaments.com results page"));
    }
}
