package com.nosoftskills.lineup.model;

import io.quarkus.security.jpa.RolesValue;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "roles")
public class Role extends TrackerEntity {

    @RolesValue
    @Column(nullable = false, unique = true, length = 50)
    public String name;
}
