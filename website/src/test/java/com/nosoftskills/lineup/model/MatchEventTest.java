package com.nosoftskills.lineup.model;

import com.nosoftskills.lineup.testsupport.TeamFormationFixtures;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@QuarkusTest
class MatchEventTest {

    private Long teamId;
    private Long teamFormationId;
    private Long competitionId;
    private Long homeParticipationId;
    private Long awayParticipationId;
    private Long matchId;
    private Long playerId;
    private Long playerAppearanceId;

    @BeforeEach
    void setup() {
        QuarkusTransaction.requiringNew().run(() -> {
            TeamFormationFixtures.Ids ids = TeamFormationFixtures.create(
                    "Match Event Test Team", "Test City", FormationType.U15, "Match Event Test League");
            teamId = ids.teamId();
            teamFormationId = ids.teamFormationId();
            competitionId = ids.competitionId();

            TeamFormation tf = TeamFormation.findById(teamFormationId);
            Competition c = Competition.findById(competitionId);

            Participation home = new Participation();
            home.teamFormation = tf;
            home.competition = c;
            home.season = "2024/2025";
            home.persist();
            homeParticipationId = home.id;

            Participation away = new Participation();
            away.teamFormation = tf;
            away.competition = c;
            away.season = "2023/2024";
            away.persist();
            awayParticipationId = away.id;

            Match m = new Match();
            m.homeTeam = home;
            m.awayTeam = away;
            m.date = java.time.LocalDate.of(2026, 3, 1);
            m.homeScore = 3;
            m.awayScore = 1;
            m.persist();
            matchId = m.id;

            Player p = new Player();
            p.names = "Test Player";
            p.persist();
            playerId = p.id;

            PlayerAppearance pa = new PlayerAppearance();
            pa.player = p;
            pa.match = m;
            pa.participation = home;
            pa.starter = false;
            pa.number = 17;
            pa.substitutedInMinute = 60;
            pa.substitutedOutMinute = 85;
            pa.persist();
            playerAppearanceId = pa.id;
        });
    }

    @AfterEach
    void cleanup() {
        QuarkusTransaction.requiringNew().run(() -> {
            PlayerAppearance.deleteById(playerAppearanceId);
            Player.deleteById(playerId);
            Match.deleteById(matchId);
            TeamFormationFixtures.delete(new TeamFormationFixtures.Ids(teamId, teamFormationId, competitionId));
        });
    }

    @Test
    void matchScoreAndSubstitutionMinutesRoundTrip() {
        QuarkusTransaction.requiringNew().run(() -> {
            Match m = Match.findById(matchId);
            assertEquals((short) 3, m.homeScore);
            assertEquals((short) 1, m.awayScore);

            PlayerAppearance pa = PlayerAppearance.findById(playerAppearanceId);
            assertEquals((short) 60, pa.substitutedInMinute);
            assertEquals((short) 85, pa.substitutedOutMinute);
        });
    }

    @Test
    void playerAppearanceParticipationDerivesHomeOrAwaySide() {
        QuarkusTransaction.requiringNew().run(() -> {
            Match m = Match.findById(matchId);
            PlayerAppearance homeAppearance = PlayerAppearance.findById(playerAppearanceId);

            assertEquals(homeParticipationId, homeAppearance.participation.id);
            assertEquals(m.homeTeam.id, homeAppearance.participation.id);
            assertNotEquals(m.awayTeam.id, homeAppearance.participation.id);
        });

        Long awayAppearanceId = QuarkusTransaction.requiringNew().call(() -> {
            Player awayPlayer = new Player();
            awayPlayer.names = "Away Side Player";
            awayPlayer.persist();

            PlayerAppearance pa = new PlayerAppearance();
            pa.player = awayPlayer;
            pa.match = Match.findById(matchId);
            pa.participation = Participation.findById(awayParticipationId);
            pa.starter = true;
            pa.persist();
            return pa.id;
        });

        QuarkusTransaction.requiringNew().run(() -> {
            Match m = Match.findById(matchId);
            PlayerAppearance awayAppearance = PlayerAppearance.findById(awayAppearanceId);

            assertEquals(awayParticipationId, awayAppearance.participation.id);
            assertEquals(m.awayTeam.id, awayAppearance.participation.id);
            assertNotEquals(m.homeTeam.id, awayAppearance.participation.id);

            Long playerToDelete = awayAppearance.player.id;
            PlayerAppearance.deleteById(awayAppearanceId);
            Player.deleteById(playerToDelete);
        });
    }

    @Test
    void matchEventPersistsAndReloadsWithEnumType() {
        QuarkusTransaction.requiringNew().run(() -> {
            MatchEvent event = new MatchEvent();
            event.playerAppearance = PlayerAppearance.findById(playerAppearanceId);
            event.type = MatchEventType.SECOND_YELLOW_CARD;
            event.minute = 78;
            event.persist();
        });

        QuarkusTransaction.requiringNew().run(() -> {
            MatchEvent reloaded = MatchEvent.find("playerAppearance.id", playerAppearanceId).firstResult();
            assertNotNull(reloaded);
            assertEquals(MatchEventType.SECOND_YELLOW_CARD, reloaded.type);
            assertEquals((short) 78, reloaded.minute);
        });
    }

    @Test
    void deletingPlayerAppearanceCascadesToItsMatchEvents() {
        QuarkusTransaction.requiringNew().run(() -> {
            MatchEvent event = new MatchEvent();
            event.playerAppearance = PlayerAppearance.findById(playerAppearanceId);
            event.type = MatchEventType.YELLOW_CARD;
            event.minute = 33;
            event.persist();
        });

        QuarkusTransaction.requiringNew().run(() -> PlayerAppearance.deleteById(playerAppearanceId));

        QuarkusTransaction.requiringNew().run(() -> {
            long remaining = MatchEvent.count("playerAppearance.id", playerAppearanceId);
            assertEquals(0, remaining);
        });

        QuarkusTransaction.requiringNew().run(() -> {
            PlayerAppearance pa = new PlayerAppearance();
            pa.player = Player.findById(playerId);
            pa.match = Match.findById(matchId);
            pa.participation = Participation.findById(homeParticipationId);
            pa.starter = false;
            pa.number = 17;
            pa.persist();
            playerAppearanceId = pa.id;
        });
    }

    @Test
    void unplayedMatchHasNullScoresAndFullMinutesAppearanceHasNullSubstitutionMinutes() {
        QuarkusTransaction.requiringNew().run(() -> {
            Match unplayed = new Match();
            unplayed.homeTeam = Participation.findById(homeParticipationId);
            unplayed.awayTeam = Participation.findById(awayParticipationId);
            unplayed.date = java.time.LocalDate.of(2026, 3, 8);
            unplayed.persist();

            Player fullMinutesPlayer = new Player();
            fullMinutesPlayer.names = "Full Minutes Player";
            fullMinutesPlayer.persist();

            PlayerAppearance fullMinutes = new PlayerAppearance();
            fullMinutes.player = fullMinutesPlayer;
            fullMinutes.match = unplayed;
            fullMinutes.participation = Participation.findById(awayParticipationId);
            fullMinutes.starter = true;
            fullMinutes.persist();

            assertNull(unplayed.homeScore);
            assertNull(unplayed.awayScore);
            assertNull(fullMinutes.substitutedInMinute);
            assertNull(fullMinutes.substitutedOutMinute);

            PlayerAppearance.deleteById(fullMinutes.id);
            fullMinutesPlayer.delete();
            unplayed.delete();
        });
    }
}
