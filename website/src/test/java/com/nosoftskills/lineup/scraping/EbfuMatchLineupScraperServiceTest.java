package com.nosoftskills.lineup.scraping;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EbfuMatchLineupScraperServiceTest {

    private final EbfuMatchLineupScraperService service = new EbfuMatchLineupScraperService();

    private Document dayListFixture() throws IOException {
        InputStream html = getClass().getResourceAsStream("/fixtures/ebfu-day-list.html");
        return Jsoup.parse(html, "UTF-8", "https://ebfu.net/teams/");
    }

    private Document lineupFixture() throws IOException {
        InputStream html = getClass().getResourceAsStream("/fixtures/ebfu-match-lineup.html");
        return Jsoup.parse(html, "UTF-8", "https://ebfu.net/teams/");
    }

    @Test
    void findMatchIdReturnsIdForMatchingTeams() throws Exception {
        String matchId = service.findMatchId(dayListFixture(), "ПФК Септември Сф", "ПФК Арда Кърджали 1924");

        assertThat(matchId, is("88210"));
    }

    @Test
    void findMatchIdThrowsWhenNoCardMatches() throws Exception {
        EbfuScraperException ex = assertThrows(EbfuScraperException.class,
                () -> service.findMatchId(dayListFixture(), "Несъществуващ Отбор", "Друг Несъществуващ Отбор"));

        assertThat(ex.getMessage(), equalTo("No match found on ebfu.net day list for Несъществуващ Отбор vs Друг Несъществуващ Отбор"));
    }

    @Test
    void findMatchIdThrowsWhenMultipleCardsMatch() {
        String duplicateCardsHtml = """
                <form>
                <table>
                <tr><td>
                  <table class="teamTable">
                    <tr>
                      <td><img></td>
                      <td><div class="teamName">Team A</div><div class="teamCity">City</div></td>
                      <td><div class="teamCity">Comp</div><div class="teamName">18:00</div></td>
                      <td><div class="teamName">Team B</div><div class="teamCity">City</div></td>
                      <td><img></td>
                    </tr>
                  </table>
                  <input type="submit" name="showMatchBtn" value="1">
                </td></tr>
                <tr><td>
                  <table class="teamTable">
                    <tr>
                      <td><img></td>
                      <td><div class="teamName">Team A</div><div class="teamCity">City</div></td>
                      <td><div class="teamCity">Comp</div><div class="teamName">20:00</div></td>
                      <td><div class="teamName">Team B</div><div class="teamCity">City</div></td>
                      <td><img></td>
                    </tr>
                  </table>
                  <input type="submit" name="showMatchBtn" value="2">
                </td></tr>
                </table>
                </form>
                """;
        Document doc = Jsoup.parse(duplicateCardsHtml);

        EbfuScraperException ex = assertThrows(EbfuScraperException.class,
                () -> service.findMatchId(doc, "Team A", "Team B"));

        assertThat(ex.getMessage(), equalTo("Multiple ambiguous matches found on ebfu.net day list for Team A vs Team B"));
    }

    @Test
    void parseLineupExtractsBothSidesWithFlagsAndCoach() throws Exception {
        EbfuMatchLineup lineup = service.parseLineup(lineupFixture());

        assertThat(lineup.home().teamName(), is("ПФК Септември Сф"));
        assertThat(lineup.home().starters(), hasSize(11));
        assertThat(lineup.home().reserves(), hasSize(11));
        assertThat(lineup.home().headCoach(), is("Иван Петров Иванов"));

        EbfuLineupEntry homeGoalkeeper = lineup.home().starters().get(0);
        assertThat(homeGoalkeeper.number(), is(12));
        assertThat(homeGoalkeeper.rawName(), is("Владимир Антонов Иванов"));
        assertThat(homeGoalkeeper.goalkeeper(), is(true));
        assertThat(homeGoalkeeper.captain(), is(false));

        EbfuLineupEntry homeCaptain = lineup.home().starters().get(3);
        assertThat(homeCaptain.rawName(), is("Мартин Христов Христов"));
        assertThat(homeCaptain.captain(), is(true));
        assertThat(homeCaptain.goalkeeper(), is(false));

        assertThat(lineup.away().teamName(), is("ПФК Арда Кърджали 1924"));
        assertThat(lineup.away().starters(), hasSize(11));
        assertThat(lineup.away().reserves(), hasSize(5));
        assertThat(lineup.away().headCoach(), is("Александър Благов Тунчев"));

        EbfuLineupEntry awayCaptainAndGoalkeeper = lineup.away().starters().get(0);
        assertThat(awayCaptainAndGoalkeeper.rawName(), is("Анатолий Енчев Господинов"));
        assertThat(awayCaptainAndGoalkeeper.captain(), is(true));
        assertThat(awayCaptainAndGoalkeeper.goalkeeper(), is(true));
    }

    @Test
    void parseLineupThrowsWhenColumnsAreMissing() {
        Document empty = Jsoup.parse("<html><body></body></html>");

        EbfuScraperException ex = assertThrows(EbfuScraperException.class, () -> service.parseLineup(empty));
        assertThat(ex.getMessage(), equalTo("Could not find both team columns on ebfu.net match page"));
    }

    @Test
    void parseEntrySplitsNameFromRoleSuffixes() {
        assertThat(service.parseEntry(7, "Plain Name"), equalTo(new EbfuLineupEntry(7, "Plain Name", false, false)));
        assertThat(service.parseEntry(1, "Goalkeeper Name - ВРАТАР"), equalTo(new EbfuLineupEntry(1, "Goalkeeper Name", true, false)));
        assertThat(service.parseEntry(4, "Captain Name - КАПИТАН"), equalTo(new EbfuLineupEntry(4, "Captain Name", false, true)));
        assertThat(service.parseEntry(1, "Both Name - КАПИТАН - ВРАТАР"), equalTo(new EbfuLineupEntry(1, "Both Name", true, true)));
    }
}
