package com.nosoftskills.lineup.scraping;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BfuLeagueScraperServiceTest {

    private final BfuLeagueScraperService service = new BfuLeagueScraperService();

    @Test
    void parsesAllTeamNamesFromFixture() throws Exception {
        InputStream html = getClass().getResourceAsStream("/fixtures/bfu-league-page.html");
        Document doc = Jsoup.parse(html, "UTF-8", "");

        List<String> names = service.parseTeamNames(doc);

        assertThat(names, not(empty()));
        assertThat(names, hasItems("ПФК Лудогорец 1945", "ПФК Левски", "ПФК ЧЕРНО МОРЕ АД"));
    }

    @Test
    void throwsWhenNoTeamsFound() {
        Document empty = Jsoup.parse("<html><body></body></html>");

        BfuScraperException ex = assertThrows(BfuScraperException.class,
                () -> service.parseTeamNames(empty));
        assertTrue(ex.getMessage().contains("No team names found"));
    }
}
