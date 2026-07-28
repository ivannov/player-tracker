package com.nosoftskills.lineup.extraction;

import com.nosoftskills.lineup.model.Competition;
import com.nosoftskills.lineup.model.CompetitionExtractionConfig;
import com.nosoftskills.lineup.model.FormationType;
import com.nosoftskills.lineup.model.Participation;
import com.nosoftskills.lineup.model.TeamFormation;
import com.nosoftskills.lineup.scraping.BfuScraperException;
import com.nosoftskills.lineup.testsupport.TeamFormationFixtures;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class ScheduledExtractionJobTest {

    private static final String FIXTURES_URL = "https://bfu-tournaments.com/leagues/test?view=past-matches";
    private static final String SEASON = "2025/2026";

    @InjectMock
    MatchExtractionService extractionService;

    @Inject
    ScheduledExtractionJob job;

    // Configured + has a Participation: the job must extract this one.
    private TeamFormationFixtures.Ids configuredAndParticipating;
    // Has a Participation but no CompetitionExtractionConfig: must be skipped.
    private TeamFormationFixtures.Ids participatingOnly;
    // Configured but no Participation: must be skipped.
    private TeamFormationFixtures.Ids configuredOnly;
    // A second configured + participating competition, used to verify one failure doesn't
    // block the others.
    private TeamFormationFixtures.Ids secondConfiguredAndParticipating;

    private Long participationId1;
    private Long participationId2;
    private Long configId1;
    private Long configId2;
    private Long configId3;

    @BeforeEach
    void setup() {
        QuarkusTransaction.requiringNew().run(() -> {
            configuredAndParticipating = TeamFormationFixtures.create(
                    "Scheduled Job Team A", "City A", FormationType.FIRST, "Scheduled Job League A");
            participationId1 = persistParticipation(configuredAndParticipating, SEASON);
            configId1 = persistConfig(configuredAndParticipating.competitionId(), FIXTURES_URL, SEASON);

            participatingOnly = TeamFormationFixtures.create(
                    "Scheduled Job Team B", "City B", FormationType.FIRST, "Scheduled Job League B");
            participationId2 = persistParticipation(participatingOnly, SEASON);

            configuredOnly = TeamFormationFixtures.create(
                    "Scheduled Job Team C", "City C", FormationType.FIRST, "Scheduled Job League C");
            configId3 = persistConfig(configuredOnly.competitionId(), FIXTURES_URL, SEASON);

            secondConfiguredAndParticipating = TeamFormationFixtures.create(
                    "Scheduled Job Team D", "City D", FormationType.FIRST, "Scheduled Job League D");
            persistParticipation(secondConfiguredAndParticipating, SEASON);
            configId2 = persistConfig(secondConfiguredAndParticipating.competitionId(), FIXTURES_URL, SEASON);
        });
    }

    @AfterEach
    void cleanup() {
        QuarkusTransaction.requiringNew().run(() -> {
            CompetitionExtractionConfig.deleteById(configId1);
            CompetitionExtractionConfig.deleteById(configId2);
            CompetitionExtractionConfig.deleteById(configId3);
            TeamFormationFixtures.delete(configuredAndParticipating);
            TeamFormationFixtures.delete(participatingOnly);
            TeamFormationFixtures.delete(configuredOnly);
            TeamFormationFixtures.delete(secondConfiguredAndParticipating);
        });
    }

    private Long persistParticipation(TeamFormationFixtures.Ids ids, String season) {
        Participation p = new Participation();
        p.teamFormation = TeamFormation.findById(ids.teamFormationId());
        p.competition = Competition.findById(ids.competitionId());
        p.season = season;
        p.persist();
        return p.id;
    }

    private Long persistConfig(Long competitionId, String fixturesUrl, String season) {
        CompetitionExtractionConfig config = new CompetitionExtractionConfig();
        config.competition = Competition.findById(competitionId);
        config.fixturesUrl = fixturesUrl;
        config.currentSeason = season;
        config.persist();
        return config.id;
    }

    @Test
    void onlyExtractsCompetitionsWithBothConfigAndParticipation() throws Exception {
        job.extractToday();

        ArgumentCaptor<ExtractionRequest> captor = ArgumentCaptor.forClass(ExtractionRequest.class);
        Mockito.verify(extractionService, Mockito.times(2)).confirm(captor.capture());

        List<Long> extractedCompetitionIds = captor.getAllValues().stream().map(ExtractionRequest::competitionId).toList();
        assertTrue(extractedCompetitionIds.contains(configuredAndParticipating.competitionId()));
        assertTrue(extractedCompetitionIds.contains(secondConfiguredAndParticipating.competitionId()));
        assertTrue(extractedCompetitionIds.stream().noneMatch(id -> id.equals(participatingOnly.competitionId())));
        assertTrue(extractedCompetitionIds.stream().noneMatch(id -> id.equals(configuredOnly.competitionId())));

        ExtractionRequest request = captor.getAllValues().stream()
                .filter(r -> r.competitionId().equals(configuredAndParticipating.competitionId()))
                .findFirst().orElseThrow();
        assertEquals(FIXTURES_URL, request.fixturesUrl());
        assertEquals(SEASON, request.season());
        assertEquals(LocalDate.now(), request.date());
    }

    @Test
    void oneCompetitionFailureDoesNotPreventOthersFromRunning() throws Exception {
        Mockito.when(extractionService.confirm(Mockito.argThat(
                        r -> r != null && r.competitionId().equals(configuredAndParticipating.competitionId()))))
                .thenThrow(new BfuScraperException("simulated scraper failure"));

        job.extractToday();

        Mockito.verify(extractionService).confirm(Mockito.argThat(
                r -> r.competitionId().equals(configuredAndParticipating.competitionId())));
        Mockito.verify(extractionService).confirm(Mockito.argThat(
                r -> r.competitionId().equals(secondConfiguredAndParticipating.competitionId())));
    }
}
