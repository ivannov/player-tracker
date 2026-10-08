package com.nosoftskills.lineup.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

// The lineup details a pending AmbiguityReview's name had in one saved match -- what a
// PlayerAppearance would hold, minus the player nobody has picked yet.
@Entity
@Table(
    name = "ambiguity_occurrences",
    uniqueConstraints = @UniqueConstraint(columnNames = {"ambiguity_review_id", "match_id"})
)
public class AmbiguityOccurrence extends TrackerEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ambiguity_review_id")
    public AmbiguityReview ambiguityReview;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    public Match match;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    public Participation participation;

    @Column(name = "starter", nullable = false)
    public boolean starter;

    @Column
    public Short number;

    @Column(name = "substituted_in_minute")
    public Short substitutedInMinute;

    @Column(name = "substituted_out_minute")
    public Short substitutedOutMinute;
}
