package com.nosoftskills.lineup.extraction;

import com.nosoftskills.lineup.model.CompetitionExtractionConfig;
import com.nosoftskills.lineup.model.Participation;
import com.nosoftskills.lineup.scraping.BfuScraperException;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.LocalDate;
import java.util.List;

/**
 * Runs the LT-008 extraction unattended, once a day, for every competition that has an
 * admin-configured {@link CompetitionExtractionConfig} and at least one {@link Participation} --
 * a competition missing either is simply skipped. Reuses {@link MatchExtractionService#confirm}
 * as-is: it never blocks on ambiguity, it just queues an AmbiguityReview, exactly like the
 * on-demand wizard.
 */
@ApplicationScoped
public class ScheduledExtractionJob {

    private static final Logger LOG = Logger.getLogger(ScheduledExtractionJob.class);

    @Inject
    MatchExtractionService extractionService;

    // SKIP: a slow scrape run legitimately outrunning a day is not expected, but this still
    // guards against a stuck previous run overlapping with the next tick.
    @Scheduled(cron = "0 0 23 * * ?", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void extractToday() {
        LocalDate today = LocalDate.now();
        for (CompetitionExtractionConfig config : configsWithParticipations()) {
            try {
                extractionService.confirm(new ExtractionRequest(
                        config.competition.id, config.fixturesUrl, config.currentSeason, today));
            } catch (BfuScraperException e) {
                LOG.errorf(e, "Scheduled extraction failed for competition %d on %s",
                        config.competition.id, today);
            }
        }
    }

    // Its own short-lived transaction, separate from the blocking scraper calls triggered by
    // confirm() below, so a pooled DB connection is never held open across network I/O (same
    // reasoning as PlayerEmbeddingSyncJob).
    private List<CompetitionExtractionConfig> configsWithParticipations() {
        return QuarkusTransaction.requiringNew().call(() ->
                CompetitionExtractionConfig.<CompetitionExtractionConfig>listAll().stream()
                        .filter(config -> Participation.count("competition.id = ?1", config.competition.id) > 0)
                        .toList());
    }
}
