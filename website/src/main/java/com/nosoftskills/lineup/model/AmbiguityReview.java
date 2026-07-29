package com.nosoftskills.lineup.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "ambiguity_reviews")
public class AmbiguityReview extends TrackerEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    public AmbiguityReviewType type;

    @Column(name = "raw_name", nullable = false)
    public String rawName;

    // The team the raw name was scraped under -- required context for both team and player
    // ambiguity reviews.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    public Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    public Match match;

    // The source the raw name was scraped from, needed to write the correct player_aliases row
    // back when an admin resolves this review from the inbox.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public ExternalRefSource source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    public AmbiguityReviewStatus status = AmbiguityReviewStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_player_id")
    public Player resolvedPlayer;

    @Column(name = "resolved_at")
    public LocalDateTime resolvedAt;

    @Column(name = "resolved_by")
    public String resolvedBy;
}
