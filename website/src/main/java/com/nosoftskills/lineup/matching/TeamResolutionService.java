package com.nosoftskills.lineup.matching;

import com.nosoftskills.lineup.model.AmbiguityReview;
import com.nosoftskills.lineup.model.AmbiguityReviewStatus;
import com.nosoftskills.lineup.model.AmbiguityReviewType;
import com.nosoftskills.lineup.model.ExternalRefSource;
import com.nosoftskills.lineup.model.Team;
import com.nosoftskills.lineup.model.TeamAlias;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolves a raw scraped team name to a canonical {@link Team} via {@link TeamAlias}, shared by
 * the participation import wizard (LT-001) and the match extraction wizard (LT-008) so both go
 * through the same lookup-or-create-or-queue logic. Never repoints an already-established
 * mapping to a different team -- that would corrupt future lookups for this raw name -- instead
 * it queues a TEAM {@link AmbiguityReview} for an admin to reconcile.
 */
@ApplicationScoped
public class TeamResolutionService {

    // Batches the alias lookup for a whole request up front so recordAlias() never issues a
    // per-row query; pass the result into repeated recordAlias() calls for the same source.
    public Map<String, TeamAlias> preloadAliases(ExternalRefSource source, Collection<String> rawTeamNames) {
        Map<String, TeamAlias> result = new HashMap<>();
        if (rawTeamNames == null) return result;
        Set<String> keys = rawTeamNames.stream().filter(s -> !isBlank(s)).collect(Collectors.toSet());
        if (keys.isEmpty()) return result;
        for (TeamAlias alias : TeamAlias.<TeamAlias>list("source = ?1 and rawName in ?2", source, keys)) {
            result.put(alias.rawName, alias);
        }
        return result;
    }

    @Transactional
    public void recordAlias(Map<String, TeamAlias> existingAliases, ExternalRefSource source,
            String rawTeamName, Team team) {
        if (isBlank(rawTeamName)) return;

        TeamAlias alias = existingAliases.get(rawTeamName);
        if (alias == null) {
            alias = new TeamAlias();
            alias.source = source;
            alias.rawName = rawTeamName;
            alias.team = team;
            alias.persist();
            existingAliases.put(rawTeamName, alias);
        } else if (!alias.team.id.equals(team.id)) {
            queueAmbiguityReview(source, rawTeamName, alias.team);
        }
    }

    private void queueAmbiguityReview(ExternalRefSource source, String rawTeamName, Team currentlyMappedTeam) {
        boolean alreadyQueued = AmbiguityReview.count(
                "type = ?1 and rawName = ?2 and status = ?3",
                AmbiguityReviewType.TEAM, rawTeamName, AmbiguityReviewStatus.PENDING) > 0;
        if (alreadyQueued) return;

        AmbiguityReview review = new AmbiguityReview();
        review.type = AmbiguityReviewType.TEAM;
        review.rawName = rawTeamName;
        review.team = currentlyMappedTeam;
        review.source = source;
        review.status = AmbiguityReviewStatus.PENDING;
        review.persist();
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
