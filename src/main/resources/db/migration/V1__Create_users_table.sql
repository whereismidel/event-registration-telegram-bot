CREATE TABLE users (
    id BIGINT PRIMARY KEY NOT NULL,
    username VARCHAR(255),
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    phone_number VARCHAR(255),
    state VARCHAR(255) NOT NULL CONSTRAINT state_check_constraint CHECK (state IN ('NAME', 'SURNAME', 'PHONE', 'REGISTERED')),
    status VARCHAR(255) NOT NULL CONSTRAINT status_check_constraint  CHECK (status IN ('ACTIVE', 'BLOCKED')),
    registered_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);