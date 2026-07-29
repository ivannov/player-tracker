package com.nosoftskills.lineup.matching;

import com.nosoftskills.lineup.model.AmbiguityCandidate;
import com.nosoftskills.lineup.model.AmbiguityReview;
import com.nosoftskills.lineup.model.AmbiguityReviewStatus;
import com.nosoftskills.lineup.model.AmbiguityReviewType;
import com.nosoftskills.lineup.model.ExternalRefSource;
import com.nosoftskills.lineup.model.Player;
import com.nosoftskills.lineup.model.PlayerAlias;
import com.nosoftskills.lineup.model.Team;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves a raw player name scraped under a given team to an existing {@link Player}, scoped to
 * players who have actually appeared for that team so unrelated same-name players elsewhere don't
 * compete as candidates. Never guesses: a confident single match auto-resolves and is remembered
 * as a {@link PlayerAlias}; anything less certain is queued as an {@link AmbiguityReview} for an
 * admin to decide.
 */
@ApplicationScoped
public class PlayerMatchingService {

    static final double AUTO_RESOLVE_THRESHOLD = 0.6;
    static final double CANDIDATE_MIN_SCORE = 0.2;
    static final double AMBIGUITY_MARGIN = 0.05;
    static final double EMBEDDING_AUTO_RESOLVE_THRESHOLD = 0.8;
    static final double EMBEDDING_MIN_SCORE = 0.3;
    static final double EMBEDDING_AMBIGUITY_MARGIN = 0.05;
    static final int MAX_CANDIDATES = 5;

    @Inject
    EntityManager em;

    @Inject
    OllamaEmbeddingClient embeddingClient;

    public record MatchResult(Player resolvedPlayer, AmbiguityReview pendingReview) {
        public boolean isResolved() {
            return resolvedPlayer != null;
        }
    }

    @Transactional
    public MatchResult resolve(String rawName, Long teamId, ExternalRefSource source) {
        PlayerAlias existing = PlayerAlias
                .find("source = ?1 and rawName = ?2 and team.id = ?3", source, rawName, teamId)
                .firstResult();
        if (existing != null) {
            return new MatchResult(existing.player, null);
        }

        List<Candidate> trigramCandidates = findCandidates(rawName, teamId);
        List<Candidate> embeddingCandidates = embeddingClient.embed(rawName)
                .map(vector -> findEmbeddingCandidates(vector, teamId))
                .orElse(List.of());

        // Never compare a trigram score against a cosine-similarity score in the same confidence
        // decision -- each metric gets its own homogeneous candidate pool and its own
        // threshold/margin. Embedding evidence, when available, takes priority since it's scoped
        // to the whole team roster rather than only the names that already passed the trigram
        // floor (see findEmbeddingCandidates).
        boolean haveEmbeddingSignal = !embeddingCandidates.isEmpty();
        List<Candidate> primary = haveEmbeddingSignal ? embeddingCandidates : trigramCandidates;
        double threshold = haveEmbeddingSignal ? EMBEDDING_AUTO_RESOLVE_THRESHOLD : AUTO_RESOLVE_THRESHOLD;
        double margin = haveEmbeddingSignal ? EMBEDDING_AMBIGUITY_MARGIN : AMBIGUITY_MARGIN;

        if (isConfidentMatch(primary, threshold, margin)) {
            Player matched = Player.findById(primary.get(0).playerId());
            Player resolved = writeAlias(source, rawName, teamId, matched);
            return new MatchResult(resolved, null);
        }

        List<Candidate> candidatesForReview = mergeCandidates(trigramCandidates, embeddingCandidates);
        return new MatchResult(null, queueForReview(rawName, teamId, source, candidatesForReview));
    }

    private boolean isConfidentMatch(List<Candidate> candidates, double threshold, double margin) {
        if (candidates.isEmpty()) {
            return false;
        }
        Candidate top = candidates.get(0);
        if (top.score() < threshold) {
            return false;
        }
        return candidates.size() == 1 || top.score() - candidates.get(1).score() >= margin;
    }

    private record Candidate(Long playerId, double score) {
    }

    // Scoped to players with at least one PlayerAppearance for a participation of this team,
    // not the whole players table -- see class Javadoc. Tiebreaker on p.id keeps LIMIT
    // deterministic across repeated calls when several candidates share the top score.
    @SuppressWarnings("unchecked")
    private List<Candidate> findCandidates(String rawName, Long teamId) {
        Query query = em.createNativeQuery("""
                SELECT p.id, similarity(p.names, :rawName) AS score
                FROM players p
                WHERE p.id IN (
                    SELECT DISTINCT pa.player_id
                    FROM player_appearances pa
                    JOIN participations part ON pa.participation_id = part.id
                    JOIN team_formations tf ON part.team_formation_id = tf.id
                    WHERE tf.team_id = :teamId
                )
                AND similarity(p.names, :rawName) >= :minScore
                ORDER BY score DESC, p.id ASC
                LIMIT :maxCandidates
                """);
        query.setParameter("rawName", rawName);
        query.setParameter("teamId", teamId);
        query.setParameter("minScore", CANDIDATE_MIN_SCORE);
        query.setParameter("maxCandidates", MAX_CANDIDATES);

        List<Object[]> rows = query.getResultList();
        return rows.stream()
                .map(row -> new Candidate(((Number) row[0]).longValue(), ((Number) row[1]).doubleValue()))
                .toList();
    }

    // Independent of the trigram floor above -- scores the same team roster by cosine similarity
    // regardless of trigram overlap, so a genuine match with low character overlap (a nickname,
    // a transliteration variant) isn't discarded before the semantic signal gets a chance to see
    // it. Same deterministic tiebreaker as findCandidates.
    @SuppressWarnings("unchecked")
    private List<Candidate> findEmbeddingCandidates(double[] queryEmbedding, Long teamId) {
        Query query = em.createNativeQuery("""
                SELECT p.id, 1 - (p.name_embedding <=> CAST(:queryVector AS vector)) AS score
                FROM players p
                WHERE p.id IN (
                    SELECT DISTINCT pa.player_id
                    FROM player_appearances pa
                    JOIN participations part ON pa.participation_id = part.id
                    JOIN team_formations tf ON part.team_formation_id = tf.id
                    WHERE tf.team_id = :teamId
                )
                AND p.name_embedding IS NOT NULL
                AND 1 - (p.name_embedding <=> CAST(:queryVector AS vector)) >= :minScore
                ORDER BY score DESC, p.id ASC
                LIMIT :maxCandidates
                """);
        query.setParameter("queryVector", PgVector.toLiteral(queryEmbedding));
        query.setParameter("teamId", teamId);
        query.setParameter("minScore", EMBEDDING_MIN_SCORE);
        query.setParameter("maxCandidates", MAX_CANDIDATES);

        List<Object[]> rows = query.getResultList();
        return rows.stream()
                .map(row -> new Candidate(((Number) row[0]).longValue(), ((Number) row[1]).doubleValue()))
                .toList();
    }

    // Union of both pools, for the ambiguity-review candidate list only -- a human reviewer
    // doesn't need the two metrics on one scale the way an automated threshold decision does,
    // they just need every plausible candidate surfaced. Embedding scores win on overlap since
    // they're the more meaningful signal when present.
    private List<Candidate> mergeCandidates(List<Candidate> trigramCandidates, List<Candidate> embeddingCandidates) {
        if (embeddingCandidates.isEmpty()) {
            return trigramCandidates;
        }
        if (trigramCandidates.isEmpty()) {
            return embeddingCandidates;
        }

        Map<Long, Candidate> byId = new LinkedHashMap<>();
        for (Candidate c : embeddingCandidates) {
            byId.put(c.playerId(), c);
        }
        for (Candidate c : trigramCandidates) {
            byId.putIfAbsent(c.playerId(), c);
        }
        return byId.values().stream()
                .sorted(Comparator.comparingDouble(Candidate::score).reversed())
                .limit(MAX_CANDIDATES)
                .toList();
    }

    // Runs in its own REQUIRES_NEW transaction so a unique-constraint violation from a concurrent
    // resolve() call for the same (source, rawName, team) -- e.g. on-demand extraction racing the
    // scheduled job -- can be caught and handled here (reuse whichever alias committed first)
    // instead of aborting the caller's outer transaction with an unhandled exception. Public so
    // the ambiguity inbox (LT-009.02) can reuse the same alias-writing path when an admin
    // resolves a queued review -- callers must ensure `player` already exists as a committed row
    // before calling this, since it runs in its own separate database transaction.
    public Player writeAlias(ExternalRefSource source, String rawName, Long teamId, Player player) {
        try {
            return QuarkusTransaction.requiringNew().call(() -> {
                PlayerAlias alias = new PlayerAlias();
                alias.player = em.getReference(Player.class, player.id);
                alias.source = source;
                alias.rawName = rawName;
                alias.team = em.getReference(Team.class, teamId);
                alias.persist();
                return player;
            });
        } catch (RuntimeException e) {
            PlayerAlias winner = PlayerAlias
                    .find("source = ?1 and rawName = ?2 and team.id = ?3", source, rawName, teamId)
                    .firstResult();
            return winner != null ? winner.player : player;
        }
    }

    // Reuses an existing PENDING review for the same rawName+team instead of piling up duplicate
    // review rows every time an unresolved name gets re-scraped (e.g. by the nightly job). Source
    // is only set when creating a new review, not when reusing one, so a re-scrape from a
    // different source doesn't repoint an already-queued review away from its original source.
    private AmbiguityReview queueForReview(String rawName, Long teamId, ExternalRefSource source,
            List<Candidate> candidates) {
        AmbiguityReview review = AmbiguityReview
                .find("type = ?1 and rawName = ?2 and team.id = ?3 and status = ?4",
                        AmbiguityReviewType.PLAYER, rawName, teamId, AmbiguityReviewStatus.PENDING)
                .firstResult();
        if (review == null) {
            review = new AmbiguityReview();
            review.type = AmbiguityReviewType.PLAYER;
            review.rawName = rawName;
            review.team = em.getReference(Team.class, teamId);
            review.source = source;
            review.status = AmbiguityReviewStatus.PENDING;
            review.persist();
        } else {
            AmbiguityCandidate.delete("ambiguityReview.id", review.id);
        }

        for (Candidate candidate : candidates) {
            AmbiguityCandidate ac = new AmbiguityCandidate();
            ac.ambiguityReview = review;
            ac.player = em.getReference(Player.class, candidate.playerId());
            ac.score = BigDecimal.valueOf(candidate.score()).setScale(4, RoundingMode.HALF_UP);
            ac.persist();
        }

        return review;
    }
}
