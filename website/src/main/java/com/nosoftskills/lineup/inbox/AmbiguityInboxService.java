package com.nosoftskills.lineup.inbox;

import com.nosoftskills.lineup.matching.PlayerMatchingService;
import com.nosoftskills.lineup.model.AmbiguityCandidate;
import com.nosoftskills.lineup.model.AmbiguityReview;
import com.nosoftskills.lineup.model.AmbiguityReviewStatus;
import com.nosoftskills.lineup.model.ExternalRefSource;
import com.nosoftskills.lineup.model.Player;
import com.nosoftskills.lineup.security.CurrentUser;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Powers the admin ambiguity inbox (LT-009): lists {@link AmbiguityReview} rows still PENDING,
 * with their ranked {@link AmbiguityCandidate} players, and resolves them either by picking an
 * existing player or by confirming a brand-new one -- both write a {@link com.nosoftskills.lineup.model.PlayerAlias}
 * back via {@link PlayerMatchingService#writeAlias} exactly like an auto-resolved match would.
 */
@ApplicationScoped
public class AmbiguityInboxService {

    @Inject
    PlayerMatchingService playerMatchingService;

    @Inject
    CurrentUser currentUser;

    public record ReviewView(Long id, String rawName, String teamName, List<CandidateView> candidates) {
    }

    public record CandidateView(Long playerId, String playerNames, java.math.BigDecimal score) {
    }

    public long countPending() {
        return AmbiguityReview.count("status = ?1", AmbiguityReviewStatus.PENDING);
    }

    @Transactional
    public List<ReviewView> listPending() {
        List<AmbiguityReview> reviews = AmbiguityReview.find(
                "SELECT r FROM AmbiguityReview r JOIN FETCH r.team WHERE r.status = ?1 ORDER BY r.createdAt",
                AmbiguityReviewStatus.PENDING).list();
        if (reviews.isEmpty()) {
            return List.of();
        }

        List<Long> reviewIds = reviews.stream().map(r -> r.id).toList();
        List<AmbiguityCandidate> candidates = AmbiguityCandidate.find(
                "SELECT c FROM AmbiguityCandidate c JOIN FETCH c.player WHERE c.ambiguityReview.id IN ?1 ORDER BY c.score DESC",
                reviewIds).list();
        Map<Long, List<AmbiguityCandidate>> candidatesByReview = candidates.stream()
                .collect(Collectors.groupingBy(c -> c.ambiguityReview.id));

        return reviews.stream()
                .map(r -> toView(r, candidatesByReview.getOrDefault(r.id, List.of())))
                .toList();
    }

    // Picking an existing candidate/player never creates a row, so the whole operation is safe
    // as one ambient transaction, mirroring how PlayerMatchingService.resolve() itself calls
    // writeAlias for an already-committed matched player.
    @Transactional
    public Player resolveReview(Long reviewId, Long chosenPlayerId) {
        AmbiguityReview review = requirePendingReview(reviewId);
        Player player = Player.findById(chosenPlayerId);
        if (player == null) {
            throw new NotFoundException("Player not found: " + chosenPlayerId);
        }

        Player resolved = playerMatchingService.writeAlias(review.source, review.rawName, review.team.id, player);
        markResolved(review, resolved);
        return resolved;
    }

    // Unlike resolveReview, the new Player row doesn't exist yet -- it must be created and
    // committed in its own transaction BEFORE writeAlias's separate REQUIRES_NEW transaction
    // tries to reference it by FK, otherwise that insert isn't visible yet and the FK check
    // blocks/fails. So this method deliberately isn't a single @Transactional method; each step
    // is its own committed unit, mirroring the real commit order this operation requires.
    public Player confirmNewPlayer(Long reviewId) {
        ReviewSnapshot snapshot = QuarkusTransaction.requiringNew().call(() -> {
            AmbiguityReview review = requirePendingReview(reviewId);
            return new ReviewSnapshot(review.rawName, review.team.id, review.source);
        });

        Long newPlayerId = QuarkusTransaction.requiringNew().call(() -> {
            Player player = new Player();
            player.names = snapshot.rawName();
            player.persist();
            return player.id;
        });
        Player newPlayer = QuarkusTransaction.requiringNew().call(() -> Player.findById(newPlayerId));

        Player resolved = playerMatchingService.writeAlias(
                snapshot.source(), snapshot.rawName(), snapshot.teamId(), newPlayer);

        QuarkusTransaction.requiringNew().run(() -> {
            AmbiguityReview review = AmbiguityReview.findById(reviewId);
            markResolved(review, Player.findById(resolved.id));
        });
        return resolved;
    }

    private record ReviewSnapshot(String rawName, Long teamId, ExternalRefSource source) {
    }

    private AmbiguityReview requirePendingReview(Long reviewId) {
        AmbiguityReview review = AmbiguityReview.findById(reviewId);
        if (review == null) {
            throw new NotFoundException("Ambiguity review not found: " + reviewId);
        }
        if (review.status != AmbiguityReviewStatus.PENDING) {
            throw new BadRequestException("Този преглед вече е разрешен от друг администратор.");
        }
        return review;
    }

    private void markResolved(AmbiguityReview review, Player resolved) {
        review.status = AmbiguityReviewStatus.RESOLVED;
        review.resolvedPlayer = resolved;
        review.resolvedAt = LocalDateTime.now();
        review.resolvedBy = currentUser.username();
    }

    private ReviewView toView(AmbiguityReview review, List<AmbiguityCandidate> candidates) {
        return new ReviewView(review.id, review.rawName, review.team.name,
                candidates.stream()
                        .map(c -> new CandidateView(c.player.id, c.player.names, c.score))
                        .toList());
    }
}
