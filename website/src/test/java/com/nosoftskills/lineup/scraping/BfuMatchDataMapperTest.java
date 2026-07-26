package com.nosoftskills.lineup.scraping;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

class BfuMatchDataMapperTest {

    private final BfuMatchDataMapper mapper = new BfuMatchDataMapper();

    @Test
    void mapsBothSidesLineupsPreservingNumberAndName() {
        BfuTeamLineup home = new BfuTeamLineup(
                "ПФК Септември Сф",
                List.of(new BfuLineupEntry(12, "Владимир Антонов Иванов")),
                List.of(new BfuLineupEntry(20, "Резервен Играч")));
        BfuTeamLineup away = new BfuTeamLineup(
                "ПФК Арда Кърджали 1924",
                List.of(new BfuLineupEntry(1, "Анатолий Енчев Господинов")),
                List.of());
        BfuMatchData matchData = new BfuMatchData(home, away, (short) 2, (short) 1, List.of(), List.of());

        ScrapedMatch scrapedMatch = mapper.toScrapedMatch(matchData);

        assertThat(scrapedMatch.home().teamName(), is("ПФК Септември Сф"));
        assertThat(scrapedMatch.home().starters(), contains(new ScrapedLineupEntry(12, "Владимир Антонов Иванов")));
        assertThat(scrapedMatch.home().reserves(), contains(new ScrapedLineupEntry(20, "Резервен Играч")));

        assertThat(scrapedMatch.away().teamName(), is("ПФК Арда Кърджали 1924"));
        assertThat(scrapedMatch.away().starters(), contains(new ScrapedLineupEntry(1, "Анатолий Енчев Господинов")));
        assertThat(scrapedMatch.away().reserves(), empty());
    }

    @Test
    void mapsScore() {
        BfuTeamLineup side = new BfuTeamLineup("Team", List.of(), List.of());
        BfuMatchData matchData = new BfuMatchData(side, side, (short) 3, (short) 0, List.of(), List.of());

        ScrapedMatch scrapedMatch = mapper.toScrapedMatch(matchData);

        assertThat(scrapedMatch.homeScore(), is((short) 3));
        assertThat(scrapedMatch.awayScore(), is((short) 0));
    }

    @Test
    void mapsEventsPreservingSideTypeMinuteAndPlayer() {
        BfuTeamLineup side = new BfuTeamLineup("Team", List.of(), List.of());
        List<BfuMatchEvent> events = List.of(
                new BfuMatchEvent(true, BfuMatchEventType.GOAL, 23, "Scorer Name"),
                new BfuMatchEvent(false, BfuMatchEventType.YELLOW_CARD, 45, "Booked Name"),
                new BfuMatchEvent(true, BfuMatchEventType.SECOND_YELLOW_CARD, 60, "Sent Off Name"),
                new BfuMatchEvent(false, BfuMatchEventType.RED_CARD, 75, "Straight Red Name"));
        BfuMatchData matchData = new BfuMatchData(side, side, (short) 0, (short) 0, events, List.of());

        ScrapedMatch scrapedMatch = mapper.toScrapedMatch(matchData);

        assertThat(scrapedMatch.events(), contains(
                new ScrapedMatchEvent(true, ScrapedMatchEventType.GOAL, 23, "Scorer Name"),
                new ScrapedMatchEvent(false, ScrapedMatchEventType.YELLOW_CARD, 45, "Booked Name"),
                new ScrapedMatchEvent(true, ScrapedMatchEventType.SECOND_YELLOW_CARD, 60, "Sent Off Name"),
                new ScrapedMatchEvent(false, ScrapedMatchEventType.RED_CARD, 75, "Straight Red Name")));
    }

    @Test
    void mapsSubstitutionsPreservingSideMinuteAndPlayers() {
        BfuTeamLineup side = new BfuTeamLineup("Team", List.of(), List.of());
        List<BfuSubstitution> substitutions = List.of(new BfuSubstitution(true, 68, "Coming On", "Going Off"));
        BfuMatchData matchData = new BfuMatchData(side, side, (short) 0, (short) 0, List.of(), substitutions);

        ScrapedMatch scrapedMatch = mapper.toScrapedMatch(matchData);

        assertThat(scrapedMatch.substitutions(), contains(new ScrapedSubstitution(true, 68, "Coming On", "Going Off")));
    }
}
