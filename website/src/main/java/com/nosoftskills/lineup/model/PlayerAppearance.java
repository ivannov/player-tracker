package com.nosoftskills.lineup.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "player_appearances",
    uniqueConstraints = @UniqueConstraint(columnNames = {"player_id", "match_id"})
)
public class PlayerAppearance extends TrackerEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    public Player player;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    public Match match;

    @Column(name = "starter", nullable = false)
    public boolean starter;

    @Column
    public Short number;
}
