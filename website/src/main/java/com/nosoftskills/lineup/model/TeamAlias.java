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
    name = "team_aliases",
    uniqueConstraints = @UniqueConstraint(columnNames = {"source", "raw_name"})
)
public class TeamAlias extends TrackerEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    public Team team;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public ExternalRefSource source;

    @Column(name = "raw_name", nullable = false)
    public String rawName;
}
