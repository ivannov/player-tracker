package com.nosoftskills.lineup.testsupport;

import com.nosoftskills.lineup.model.Competition;
import com.nosoftskills.lineup.model.FormationType;
import com.nosoftskills.lineup.model.Participation;
import com.nosoftskills.lineup.model.Team;
import com.nosoftskills.lineup.model.TeamFormation;
import io.quarkus.hibernate.orm.panache.Panache;

/**
 * Shared Team/TeamFormation/Competition fixture used by tests. Must be called from within an
 * active transaction, e.g. QuarkusTransaction.requiringNew().run(...).
 */
public final class TeamFormationFixtures {

    private TeamFormationFixtures() {
    }

    public record Ids(Long teamId, Long teamFormationId, Long competitionId) {
    }

    public static Ids create(String teamName, String teamLocation, FormationType formationType, String competitionName) {
        Team t = new Team();
        t.name = teamName;
        t.location = teamLocation;
        t.persist();

        TeamFormation tf = new TeamFormation();
        tf.team = t;
        tf.type = formationType;
        tf.persist();

        Competition c = new Competition();
        c.name = competitionName;
        c.persist();

        return new Ids(t.id, tf.id, c.id);
    }

    public static void delete(Ids ids) {
        // Flush first: callers may have queued entity-level deletes (e.g. Match.deleteById) on
        // other tables that this bulk query depends on but that Hibernate's auto-flush won't
        // pick up, since it only checks for dirty state on the entity type being bulk-deleted.
        Panache.getEntityManager().flush();
        Participation.delete("teamFormation.team.id", ids.teamId());
        TeamFormation.delete("team.id", ids.teamId());
        Team.deleteById(ids.teamId());
        Competition.deleteById(ids.competitionId());
    }
}
