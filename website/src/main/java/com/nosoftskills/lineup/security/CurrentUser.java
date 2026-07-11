package com.nosoftskills.lineup.security;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@RequestScoped
public class CurrentUser {

    @Inject
    SecurityIdentity identity;

    public String username() {
        return identity.isAnonymous() ? null : identity.getPrincipal().getName();
    }

    public boolean isAdmin() {
        return identity.hasRole("ADMIN");
    }
}
