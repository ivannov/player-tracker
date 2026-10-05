-- Cross-club candidate search scans the whole players table by name similarity (transfers).
CREATE INDEX idx_players_names_trgm ON players USING GIN (names gin_trgm_ops);
