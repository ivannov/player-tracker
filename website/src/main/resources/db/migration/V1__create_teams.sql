-- Requires database created with: CREATE DATABASE ... ENCODING 'UTF8' LC_COLLATE 'bg_BG.UTF-8' LC_CTYPE 'bg_BG.UTF-8'
-- All text columns use UTF-8 and support Bulgarian Cyrillic characters.

CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE teams (
    id           BIGSERIAL    PRIMARY KEY,
    version      INTEGER      NOT NULL DEFAULT 0,
    created_at   TIMESTAMP    NOT NULL,
    last_updated TIMESTAMP    NOT NULL,
    name         VARCHAR(255) NOT NULL,
    location     VARCHAR(255) NOT NULL,
    logo_url     VARCHAR(512)
);

CREATE TABLE team_formations (
    id           BIGSERIAL    PRIMARY KEY,
    version      INTEGER      NOT NULL DEFAULT 0,
    created_at   TIMESTAMP    NOT NULL,
    last_updated TIMESTAMP    NOT NULL,
    team_id      BIGINT       NOT NULL REFERENCES teams(id),
    type         VARCHAR(10)  NOT NULL,
    UNIQUE (team_id, type)
);

CREATE TABLE competitions (
    id           BIGSERIAL    PRIMARY KEY,
    version      INTEGER      NOT NULL DEFAULT 0,
    created_at   TIMESTAMP    NOT NULL,
    last_updated TIMESTAMP    NOT NULL,
    name         VARCHAR(255) NOT NULL,
    logo_url     VARCHAR(512)
);

-- Opt-in config for LT-009's unattended scheduled extraction job: a competition with no row
-- here is simply skipped by the job. fixtures_url already encodes a season query param (see
-- BfuFixtureScraperService), so it and current_season are admin-maintained together, once per
-- season -- kept out of the core competitions table since it's scheduling-specific, not a
-- property of the competition itself.
CREATE TABLE competition_extraction_configs (
    id              BIGSERIAL    PRIMARY KEY,
    version         INTEGER      NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL,
    last_updated    TIMESTAMP    NOT NULL,
    competition_id  BIGINT       NOT NULL UNIQUE REFERENCES competitions(id),
    fixtures_url    VARCHAR(512) NOT NULL,
    current_season  VARCHAR(9)   NOT NULL
);

CREATE TABLE participations (
    id                 BIGSERIAL  PRIMARY KEY,
    version            INTEGER    NOT NULL DEFAULT 0,
    created_at         TIMESTAMP  NOT NULL,
    last_updated       TIMESTAMP  NOT NULL,
    team_formation_id  BIGINT     NOT NULL REFERENCES team_formations(id),
    competition_id     BIGINT     NOT NULL REFERENCES competitions(id),
    season             VARCHAR(9) NOT NULL,
    UNIQUE (team_formation_id, competition_id, season)
);

CREATE TABLE matches (
    id             BIGSERIAL PRIMARY KEY,
    version        INTEGER   NOT NULL DEFAULT 0,
    created_at     TIMESTAMP NOT NULL,
    last_updated   TIMESTAMP NOT NULL,
    home_team_id   BIGINT    NOT NULL REFERENCES participations(id),
    away_team_id   BIGINT    NOT NULL REFERENCES participations(id),
    date           DATE      NOT NULL,
    home_score     SMALLINT,
    away_score     SMALLINT
);

CREATE TABLE players (
    id             BIGSERIAL    PRIMARY KEY,
    version        INTEGER      NOT NULL DEFAULT 0,
    created_at     TIMESTAMP    NOT NULL,
    last_updated   TIMESTAMP    NOT NULL,
    names          VARCHAR(255) NOT NULL,
    name_embedding VECTOR(768)
);

CREATE TABLE player_appearances (
    id                      BIGSERIAL PRIMARY KEY,
    version                 INTEGER   NOT NULL DEFAULT 0,
    created_at              TIMESTAMP NOT NULL,
    last_updated            TIMESTAMP NOT NULL,
    player_id               BIGINT    NOT NULL REFERENCES players(id),
    match_id                BIGINT    NOT NULL REFERENCES matches(id),
    participation_id        BIGINT    NOT NULL REFERENCES participations(id),
    starter                 BOOLEAN   NOT NULL,
    number                  SMALLINT,
    substituted_in_minute   SMALLINT CHECK (substituted_in_minute IS NULL OR substituted_in_minute BETWEEN 0 AND 130),
    substituted_out_minute  SMALLINT CHECK (substituted_out_minute IS NULL OR substituted_out_minute BETWEEN 0 AND 130),
    UNIQUE (player_id, match_id),
    CHECK (substituted_in_minute IS NULL OR substituted_out_minute IS NULL OR substituted_out_minute > substituted_in_minute)
);

CREATE TABLE match_events (
    id                    BIGSERIAL   PRIMARY KEY,
    version               INTEGER     NOT NULL DEFAULT 0,
    created_at            TIMESTAMP   NOT NULL,
    last_updated          TIMESTAMP   NOT NULL,
    player_appearance_id  BIGINT      NOT NULL REFERENCES player_appearances(id) ON DELETE CASCADE,
    type                  VARCHAR(20) NOT NULL,
    minute                SMALLINT CHECK (minute IS NULL OR minute BETWEEN 0 AND 130)
);

CREATE INDEX idx_match_events_player_appearance_id ON match_events (player_appearance_id);

-- Scopes a team's raw scraped name by source, so re-encountering the same team next season
-- is an exact lookup instead of a fresh fuzzy match.
CREATE TABLE team_aliases (
    id           BIGSERIAL    PRIMARY KEY,
    version      INTEGER      NOT NULL DEFAULT 0,
    created_at   TIMESTAMP    NOT NULL,
    last_updated TIMESTAMP    NOT NULL,
    team_id      BIGINT       NOT NULL REFERENCES teams(id),
    source       VARCHAR(20)  NOT NULL,
    raw_name     VARCHAR(255) NOT NULL,
    UNIQUE (source, raw_name)
);

-- Scoped by team_id (the team the name was scraped under), not globally -- a player transferring
-- to a new team is a fresh resolution rather than an auto-match, since two different players can
-- share a common Bulgarian name.
CREATE TABLE player_aliases (
    id           BIGSERIAL    PRIMARY KEY,
    version      INTEGER      NOT NULL DEFAULT 0,
    created_at   TIMESTAMP    NOT NULL,
    last_updated TIMESTAMP    NOT NULL,
    player_id    BIGINT       NOT NULL REFERENCES players(id),
    source       VARCHAR(20)  NOT NULL,
    raw_name     VARCHAR(255) NOT NULL,
    team_id      BIGINT       NOT NULL REFERENCES teams(id),
    UNIQUE (source, raw_name, team_id)
);

CREATE INDEX idx_player_aliases_raw_name_trgm ON player_aliases USING GIN (raw_name gin_trgm_ops);

-- Admin inbox item: a raw name that couldn't be confidently resolved.
CREATE TABLE ambiguity_reviews (
    id                  BIGSERIAL    PRIMARY KEY,
    version             INTEGER      NOT NULL DEFAULT 0,
    created_at          TIMESTAMP    NOT NULL,
    last_updated        TIMESTAMP    NOT NULL,
    type                VARCHAR(10)  NOT NULL,
    raw_name            VARCHAR(255) NOT NULL,
    team_id             BIGINT       NOT NULL REFERENCES teams(id),
    match_id            BIGINT       REFERENCES matches(id),
    status              VARCHAR(10)  NOT NULL DEFAULT 'PENDING',
    resolved_player_id  BIGINT       REFERENCES players(id),
    resolved_at         TIMESTAMP,
    resolved_by         VARCHAR(255)
);

-- Ranked candidate players for an ambiguity_reviews entry.
CREATE TABLE ambiguity_candidates (
    id                    BIGSERIAL     PRIMARY KEY,
    version               INTEGER       NOT NULL DEFAULT 0,
    created_at            TIMESTAMP     NOT NULL,
    last_updated          TIMESTAMP     NOT NULL,
    ambiguity_review_id   BIGINT        NOT NULL REFERENCES ambiguity_reviews(id) ON DELETE CASCADE,
    player_id             BIGINT        NOT NULL REFERENCES players(id),
    score                 NUMERIC(5,4)  NOT NULL
);

CREATE INDEX idx_ambiguity_candidates_ambiguity_review_id ON ambiguity_candidates (ambiguity_review_id);

CREATE TABLE roles (
    id           BIGSERIAL   PRIMARY KEY,
    version      INTEGER     NOT NULL DEFAULT 0,
    created_at   TIMESTAMP   NOT NULL,
    last_updated TIMESTAMP   NOT NULL,
    name         VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE users (
    id           BIGSERIAL    PRIMARY KEY,
    version      INTEGER      NOT NULL DEFAULT 0,
    created_at   TIMESTAMP    NOT NULL,
    last_updated TIMESTAMP    NOT NULL,
    username     VARCHAR(255) NOT NULL UNIQUE,
    password     VARCHAR(255) NOT NULL
);

CREATE TABLE user_roles (
    user_id  BIGINT NOT NULL REFERENCES users(id),
    role_id  BIGINT NOT NULL REFERENCES roles(id),
    PRIMARY KEY (user_id, role_id)
);

INSERT INTO roles (version, created_at, last_updated, name) VALUES
    (0, NOW(), NOW(), 'ADMIN'),
    (0, NOW(), NOW(), 'USER');

-- Default admin user. Change the password after first login in production.
INSERT INTO users (version, created_at, last_updated, username, password) VALUES
    (0, NOW(), NOW(), 'admin', '$2a$10$xxFgME5YwoHikq05Z3keSOyGOqRXeBrcU5ZCChHAlrzd1MHIyWRky');

INSERT INTO user_roles (user_id, role_id)
    SELECT u.id, r.id FROM users u, roles r
    WHERE u.username = 'admin' AND r.name = 'ADMIN';
