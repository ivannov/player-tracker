package com.nosoftskills.lineup.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "matches")
public class Match extends TrackerEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    public Participation homeTeam;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    public Participation awayTeam;

    @Column(nullable = false)
    public LocalDate date;
}
