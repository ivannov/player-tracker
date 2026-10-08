-- Lineup details of a pending unclear player name in one saved match, so resolving the
-- ambiguity_reviews entry can add the player_appearances row it was left out of (UC-011 BR-006).
CREATE TABLE ambiguity_occurrences (
    id                      BIGSERIAL PRIMARY KEY,
    version                 INTEGER   NOT NULL DEFAULT 0,
    created_at              TIMESTAMP NOT NULL,
    last_updated            TIMESTAMP NOT NULL,
    ambiguity_review_id     BIGINT    NOT NULL REFERENCES ambiguity_reviews(id) ON DELETE CASCADE,
    match_id                BIGINT    NOT NULL REFERENCES matches(id),
    participation_id        BIGINT    NOT NULL REFERENCES participations(id),
    starter                 BOOLEAN   NOT NULL,
    number                  SMALLINT,
    substituted_in_minute   SMALLINT CHECK (substituted_in_minute IS NULL OR substituted_in_minute BETWEEN 0 AND 130),
    substituted_out_minute  SMALLINT CHECK (substituted_out_minute IS NULL OR substituted_out_minute BETWEEN 0 AND 130),
    UNIQUE (ambiguity_review_id, match_id)
);

CREATE TABLE ambiguity_occurrence_events (
    id                       BIGSERIAL   PRIMARY KEY,
    version                  INTEGER     NOT NULL DEFAULT 0,
    created_at               TIMESTAMP   NOT NULL,
    last_updated             TIMESTAMP   NOT NULL,
    ambiguity_occurrence_id  BIGINT      NOT NULL REFERENCES ambiguity_occurrences(id) ON DELETE CASCADE,
    type                     VARCHAR(20) NOT NULL,
    minute                   SMALLINT CHECK (minute IS NULL OR minute BETWEEN 0 AND 130)
);

CREATE INDEX idx_ambiguity_occurrence_events_occurrence_id ON ambiguity_occurrence_events (ambiguity_occurrence_id);
