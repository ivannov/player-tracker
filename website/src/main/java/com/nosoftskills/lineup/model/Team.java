package com.nosoftskills.lineup.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "teams")
public class Team extends TrackerEntity {

    @Column(nullable = false)
    public String name;

    @Column(nullable = false)
    public String location;

    @Column(name = "logo_url", length = 512)
    public String logoUrl;
}
