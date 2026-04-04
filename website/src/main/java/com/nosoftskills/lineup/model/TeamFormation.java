package com.nosoftskills.lineup.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "team_formations",
    uniqueConstraints = @UniqueConstraint(columnNames = {"team_id", "type"})
)
public class TeamFormation extends TrackerEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    public Team team;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    public FormationType type;
}
