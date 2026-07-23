package com.nosoftskills.lineup.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "ambiguity_candidates")
public class AmbiguityCandidate extends TrackerEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ambiguity_review_id")
    public AmbiguityReview ambiguityReview;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    public Player player;

    @Column(nullable = false, precision = 5, scale = 4)
    public BigDecimal score;
}
