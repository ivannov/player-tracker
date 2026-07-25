package com.nosoftskills.lineup.scraping;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BfuMatchScraperServiceTest {

    private final BfuMatchScraperService service = new BfuMatchScraperService();

    private Document fixture(String name) throws IOException {
        InputStream html = getClass().getResourceAsStream("/fixtures/" + name);
        return Jsoup.parse(html, "UTF-8", "https://bfu-tournaments.com/");
    }

    @Test
    void parseMatchExtractsScoreLineupsAndTimelineFromYellowCardMatch() throws Exception {
        BfuMatchData data = service.parseMatch(fixture("bfu-match-yellow-card.html"));

        assertThat(data.home().teamName(), is("ПФК Арда Кърджали 1924"));
        assertThat(data.away().teamName(), is("ПФК Славия 1913"));
        assertThat(data.homeScore(), is((short) 1));
        assertThat(data.awayScore(), is((short) 0));

        assertThat(data.home().starters(), hasSize(11));
        assertThat(data.home().reserves(), hasSize(7));
        assertThat(data.away().starters(), hasSize(11));
        assertThat(data.away().reserves(), hasSize(11));

        BfuLineupEntry homeGoalkeeper = data.home().starters().get(0);
        assertThat(homeGoalkeeper.number(), is(1));
        assertThat(homeGoalkeeper.playerName(), is("Анатолий Господинов"));

        assertThat(data.events(), hasSize(4));
        assertThat(data.events(), hasItem(new BfuMatchEvent(true, BfuMatchEventType.YELLOW_CARD, 28, "Антонио Вутов")));
        assertThat(data.events(), hasItem(new BfuMatchEvent(false, BfuMatchEventType.YELLOW_CARD, 62, "Илиян Стефанов")));
        assertThat(data.events(), hasItem(new BfuMatchEvent(true, BfuMatchEventType.GOAL, 94, "Атанас Кабов")));

        assertThat(data.substitutions(), hasSize(9));
        assertThat(data.substitutions(), hasItem(new BfuSubstitution(false, 45, "Илиян Стефанов", "Васил Казълджиев")));
        assertThat(data.substitutions(), hasItem(new BfuSubstitution(true, 68, "Доминик Янков", "Преслав Бачев")));
        assertThat(data.substitutions(), hasItem(new BfuSubstitution(false, 82, "Борис Тодоров", "Иван Минчев")));
        assertThat(data.substitutions(), hasItem(new BfuSubstitution(false, 82, "Йоан Борносузов", "Владимир Николов")));
    }

    @Test
    void parseMatchExtractsRedCardFromRedCardMatch() throws Exception {
        BfuMatchData data = service.parseMatch(fixture("bfu-match-red-card.html"));

        assertThat(data.events(), hasItem(new BfuMatchEvent(true, BfuMatchEventType.RED_CARD, 93, "Ивайло Тодоров")));
    }

    @Test
    void parseMatchThrowsWhenTeamNamesAreMissing() {
        Document empty = Jsoup.parse("<html><body></body></html>");

        BfuScraperException ex = assertThrows(BfuScraperException.class, () -> service.parseMatch(empty));
        assertThat(ex.getMessage(), equalTo("Could not find home/away team names on bfu-tournaments.com match page"));
    }

    @Test
    void parseMatchThrowsWhenScoreIsMissing() {
        String html = """
                <div class="font-bold text-2xl">Home Team</div>
                <div class="font-bold text-2xl">Away Team</div>
                """;
        Document doc = Jsoup.parse(html);

        BfuScraperException ex = assertThrows(BfuScraperException.class, () -> service.parseMatch(doc));
        assertThat(ex.getMessage(), equalTo("Could not find match score on bfu-tournaments.com match page"));
    }

    @Test
    void parseMinuteHandlesStoppageTime() throws Exception {
        assertThat(service.parseMinute("28'"), is(28));
        assertThat(service.parseMinute("90 +4'"), is(94));
    }

    @Test
    void parseMinuteThrowsWhenUnparseable() {
        BfuScraperException ex = assertThrows(BfuScraperException.class, () -> service.parseMinute("half-time"));
        assertThat(ex.getMessage(), equalTo("Could not parse event minute from 'half-time' on bfu-tournaments.com match page"));
    }
}
