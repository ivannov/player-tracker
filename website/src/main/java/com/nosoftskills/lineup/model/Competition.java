package com.nosoftskills.lineup.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "competitions")
public class Competition extends TrackerEntity {

    @Column(nullable = false)
    public String name;

    @Column(name = "logo_url", length = 512)
    public String logoUrl;
}
