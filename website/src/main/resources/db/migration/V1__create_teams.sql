-- Requires database created with: CREATE DATABASE ... ENCODING 'UTF8' LC_COLLATE 'bg_BG.UTF-8' LC_CTYPE 'bg_BG.UTF-8'
-- All text columns use UTF-8 and support Bulgarian Cyrillic characters.

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
    date           DATE      NOT NULL
);

CREATE TABLE players (
    id           BIGSERIAL    PRIMARY KEY,
    version      INTEGER      NOT NULL DEFAULT 0,
    created_at   TIMESTAMP    NOT NULL,
    last_updated TIMESTAMP    NOT NULL,
    names        VARCHAR(255) NOT NULL
);

CREATE TABLE player_appearances (
    id           BIGSERIAL PRIMARY KEY,
    version      INTEGER   NOT NULL DEFAULT 0,
    created_at   TIMESTAMP NOT NULL,
    last_updated TIMESTAMP NOT NULL,
    player_id    BIGINT    NOT NULL REFERENCES players(id),
    match_id     BIGINT    NOT NULL REFERENCES matches(id),
    starter      BOOLEAN   NOT NULL,
    number       SMALLINT,
    UNIQUE (player_id, match_id)
);

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
