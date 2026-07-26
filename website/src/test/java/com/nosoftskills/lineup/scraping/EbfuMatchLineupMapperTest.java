package com.nosoftskills.lineup.scraping;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

class EbfuMatchLineupMapperTest {

    private final EbfuMatchLineupMapper mapper = new EbfuMatchLineupMapper();

    @Test
    void mapsBothSidesLineupsPreservingNumberAndName() {
        EbfuTeamLineup home = new EbfuTeamLineup(
                "ПФК Септември Сф",
                List.of(
                        new EbfuLineupEntry(12, "Владимир Антонов Иванов", true, false),
                        new EbfuLineupEntry(4, "Мартин Христов Христов", false, true)),
                List.of(new EbfuLineupEntry(20, "Резервен Играч", false, false)),
                "Иван Петров Иванов");
        EbfuTeamLineup away = new EbfuTeamLineup(
                "ПФК Арда Кърджали 1924",
                List.of(new EbfuLineupEntry(1, "Анатолий Енчев Господинов", true, true)),
                List.of(),
                "Александър Благов Тунчев");
        EbfuMatchLineup lineup = new EbfuMatchLineup(home, away);

        ScrapedMatch scrapedMatch = mapper.toScrapedMatch(lineup);

        assertThat(scrapedMatch.home().teamName(), is("ПФК Септември Сф"));
        assertThat(scrapedMatch.home().starters(), contains(
                new ScrapedLineupEntry(12, "Владимир Антонов Иванов"),
                new ScrapedLineupEntry(4, "Мартин Христов Христов")));
        assertThat(scrapedMatch.home().reserves(), contains(new ScrapedLineupEntry(20, "Резервен Играч")));

        assertThat(scrapedMatch.away().teamName(), is("ПФК Арда Кърджали 1924"));
        assertThat(scrapedMatch.away().starters(), contains(new ScrapedLineupEntry(1, "Анатолий Енчев Господинов")));
        assertThat(scrapedMatch.away().reserves(), empty());
    }

    @Test
    void leavesScoreEventsAndSubstitutionsEmptySinceEbfuNetOnlyProvidesLineups() {
        EbfuTeamLineup side = new EbfuTeamLineup("Team", List.of(), List.of(), "Coach");
        EbfuMatchLineup lineup = new EbfuMatchLineup(side, side);

        ScrapedMatch scrapedMatch = mapper.toScrapedMatch(lineup);

        assertThat(scrapedMatch.homeScore(), nullValue());
        assertThat(scrapedMatch.awayScore(), nullValue());
        assertThat(scrapedMatch.events(), empty());
        assertThat(scrapedMatch.substitutions(), empty());
    }

    @Test
    void dropsGoalkeeperAndCaptainFlagsNotCarriedByTheSharedContract() {
        EbfuTeamLineup side = new EbfuTeamLineup(
                "Team",
                List.of(new EbfuLineupEntry(1, "Both Flags Player", true, true)),
                List.of(),
                "Coach");
        EbfuMatchLineup lineup = new EbfuMatchLineup(side, side);

        ScrapedMatch scrapedMatch = mapper.toScrapedMatch(lineup);

        assertThat(scrapedMatch.home().starters(), contains(equalTo(new ScrapedLineupEntry(1, "Both Flags Player"))));
    }
}
