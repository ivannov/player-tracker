package com.nosoftskills.lineup.inbox;

import com.nosoftskills.lineup.matching.PlayerMatchingService;
import com.nosoftskills.lineup.matching.TeamResolutionService;
import com.nosoftskills.lineup.model.AmbiguityCandidate;
import com.nosoftskills.lineup.model.AmbiguityOccurrence;
import com.nosoftskills.lineup.model.AmbiguityOccurrenceEvent;
import com.nosoftskills.lineup.model.AmbiguityReview;
import com.nosoftskills.lineup.model.AmbiguityReviewStatus;
import com.nosoftskills.lineup.model.AmbiguityReviewType;
import com.nosoftskills.lineup.model.ExternalRefSource;
import com.nosoftskills.lineup.model.MatchEvent;
import com.nosoftskills.lineup.model.Player;
import com.nosoftskills.lineup.model.PlayerAppearance;
import com.nosoftskills.lineup.model.Team;
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
 * TEAM-type reviews (LT-018) go through a separate {@link #resolveTeamReview} path instead --
 * they never have candidates and must never resolve into a {@link Player}.
 */
@ApplicationScoped
public class AmbiguityInboxService {

    @Inject
    PlayerMatchingService playerMatchingService;

    @Inject
    TeamResolutionService teamResolutionService;

    @Inject
    CurrentUser currentUser;

    public record ReviewView(Long id, String rawName, String teamName, boolean teamReview, List<CandidateView> candidates) {
    }

    // otherClub is the candidate's latest club when it differs from the review's team (a likely
    // transfer), null otherwise.
    public record CandidateView(Long playerId, String playerNames, String otherClub, java.math.BigDecimal score) {
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
        Map<Long, PlayerMatchingService.LatestClub> latestClubs = playerMatchingService.latestClubs(
                candidates.stream().map(c -> c.player.id).collect(Collectors.toSet()));

        return reviews.stream()
                .map(r -> toView(r, candidatesByReview.getOrDefault(r.id, List.of()), latestClubs))
                .toList();
    }

    // Picking an existing candidate/player never creates a row, so the whole operation is safe
    // as one ambient transaction, mirroring how PlayerMatchingService.resolve() itself calls
    // writeAlias for an already-committed matched player.
    @Transactional
    public Player resolveReview(Long reviewId, Long chosenPlayerId) {
        AmbiguityReview review = requirePendingReview(reviewId);
        requireType(review, AmbiguityReviewType.PLAYER);
        Player player = Player.findById(chosenPlayerId);
        if (player == null) {
            throw new NotFoundException("Player not found: " + chosenPlayerId);
        }

        Player resolved = playerMatchingService.writeAlias(review.source, review.rawName, review.team.id, player);
        markResolved(review, resolved);
        addToLineups(review, resolved);
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
            requireType(review, AmbiguityReviewType.PLAYER);
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
            Player player = Player.findById(resolved.id);
            markResolved(review, player);
            addToLineups(review, player);
        });
        return resolved;
    }

    // Picks an existing Team for a TEAM-type review and repoints the TeamAlias to it -- the
    // correct resolution path this type of review was always missing (LT-018). Unlike the player
    // flow, there's no ranked-candidates step: the admin picks from the full team list, since
    // TeamResolutionService never computes fuzzy team candidates the way PlayerMatchingService
    // does for players.
    @Transactional
    public Team resolveTeamReview(Long reviewId, Long chosenTeamId) {
        AmbiguityReview review = requirePendingReview(reviewId);
        requireType(review, AmbiguityReviewType.TEAM);
        Team team = Team.findById(chosenTeamId);
        if (team == null) {
            throw new NotFoundException("Team not found: " + chosenTeamId);
        }

        Team resolved = teamResolutionService.resolveAlias(review.source, review.rawName, team);
        markResolvedTeam(review, resolved);
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

    // Defends the exact bug LT-018 fixed: a PLAYER-only action call site reached for a TEAM
    // review (or vice versa) must fail loudly here even if the UI is ever wired up wrong again,
    // rather than silently corrupting data (e.g. creating a bogus Player for a team name).
    private void requireType(AmbiguityReview review, AmbiguityReviewType expected) {
        if (review.type != expected) {
            throw new BadRequestException("Този преглед е от друг тип и не може да бъде разрешен по този начин.");
        }
    }

    private void markResolved(AmbiguityReview review, Player resolved) {
        review.status = AmbiguityReviewStatus.RESOLVED;
        review.resolvedPlayer = resolved;
        review.resolvedAt = LocalDateTime.now();
        review.resolvedBy = currentUser.username();
    }

    // UC-011 BR-006: Resolving a player item adds the player to the lineup of every saved match
    // the name was raised in, from the details kept there; an existing lineup entry is kept and
    // only the goals and cards not yet stored for it are added (UC-009 BR-006).
    private void addToLineups(AmbiguityReview review, Player player) {
        List<AmbiguityOccurrence> occurrences = AmbiguityOccurrence.list("ambiguityReview.id", review.id);
        for (AmbiguityOccurrence occurrence : occurrences) {
            PlayerAppearance appearance = PlayerAppearance.<PlayerAppearance>find(
                    "player.id = ?1 and match.id = ?2", player.id, occurrence.match.id).firstResult();
            if (appearance == null) {
                appearance = new PlayerAppearance();
                appearance.player = player;
                appearance.match = occurrence.match;
                appearance.participation = occurrence.participation;
                appearance.starter = occurrence.starter;
                appearance.number = occurrence.number;
                appearance.substitutedInMinute = occurrence.substitutedInMinute;
                appearance.substitutedOutMinute = occurrence.substitutedOutMinute;
                appearance.persist();
            }

            List<AmbiguityOccurrenceEvent> events = AmbiguityOccurrenceEvent.list("ambiguityOccurrence.id", occurrence.id);
            for (AmbiguityOccurrenceEvent kept : events) {
                boolean exists = MatchEvent.count("playerAppearance.id = ?1 and type = ?2 and minute = ?3",
                        appearance.id, kept.type, kept.minute) > 0;
                if (exists) continue;

                MatchEvent event = new MatchEvent();
                event.playerAppearance = appearance;
                event.type = kept.type;
                event.minute = kept.minute;
                event.persist();
            }
        }
    }

    private void markResolvedTeam(AmbiguityReview review, Team resolved) {
        review.status = AmbiguityReviewStatus.RESOLVED;
        review.resolvedTeam = resolved;
        review.resolvedAt = LocalDateTime.now();
        review.resolvedBy = currentUser.username();
    }

    private ReviewView toView(AmbiguityReview review, List<AmbiguityCandidate> candidates,
            Map<Long, PlayerMatchingService.LatestClub> latestClubs) {
        return new ReviewView(review.id, review.rawName, review.team.name,
                review.type == AmbiguityReviewType.TEAM,
                candidates.stream()
                        .map(c -> new CandidateView(c.player.id, c.player.names,
                                otherClub(latestClubs.get(c.player.id), review.team.id), c.score))
                        .toList());
    }

    private static String otherClub(PlayerMatchingService.LatestClub club, Long reviewTeamId) {
        return club == null || club.teamId().equals(reviewTeamId) ? null : club.teamName();
    }
}
