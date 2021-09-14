CREATE TABLE IF NOT EXISTS guidelines
(
    id         uuid,
    created_at timestamp,
    updated_at timestamp,
    status     varchar,
    data       jsonb
);

CREATE TABLE IF NOT EXISTS directives
(
    id         uuid,
    created_at timestamp,
    updated_at timestamp,
    status     varchar,
    data       jsonb
);