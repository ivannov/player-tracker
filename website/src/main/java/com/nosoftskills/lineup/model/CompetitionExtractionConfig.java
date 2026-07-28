package com.nosoftskills.lineup.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Opt-in per-competition config for LT-009's unattended scheduled extraction job: a competition
 * with no row here is simply skipped by the job. {@code fixturesUrl} already encodes a season
 * query param (see BfuFixtureScraperService), so it and {@code currentSeason} are admin-maintained
 * together, once per season.
 */
@Entity
@Table(name = "competition_extraction_configs")
public class CompetitionExtractionConfig extends TrackerEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    public Competition competition;

    @Column(name = "fixtures_url", nullable = false, length = 512)
    public String fixturesUrl;

    @Column(name = "current_season", nullable = false, length = 9)
    public String currentSeason;
}
