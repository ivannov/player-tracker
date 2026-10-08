package com.nosoftskills.lineup.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "ambiguity_occurrence_events")
public class AmbiguityOccurrenceEvent extends TrackerEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ambiguity_occurrence_id")
    public AmbiguityOccurrence ambiguityOccurrence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public MatchEventType type;

    @Column
    public Short minute;
}
