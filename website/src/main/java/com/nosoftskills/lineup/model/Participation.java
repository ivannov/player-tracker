package com.nosoftskills.lineup.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "participations",
    uniqueConstraints = @UniqueConstraint(columnNames = {"team_formation_id", "competition_id", "season"})
)
public class Participation extends TrackerEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_formation_id")
    public TeamFormation teamFormation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    public Competition competition;

    @Column(nullable = false, length = 9)
    public String season;
}
