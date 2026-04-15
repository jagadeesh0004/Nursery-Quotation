-- V1__initial_schema.sql
-- Nursery Quotation System - Initial Database Schema

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================
-- TABLE: users
-- ============================================================
CREATE TABLE users (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone_number VARCHAR(15)  NOT NULL UNIQUE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at   TIMESTAMPTZ  NULL,
    version      INTEGER      NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_users_phone ON users (phone_number);
CREATE INDEX IF NOT EXISTS idx_users_deleted_at ON users (deleted_at);

-- ============================================================
-- TABLE: otps
-- ============================================================
CREATE TABLE otps (
    id           BIGSERIAL    PRIMARY KEY,
    phone_number VARCHAR(15)  NOT NULL,
    otp_code     VARCHAR(10)  NOT NULL,
    expires_at   TIMESTAMPTZ  NOT NULL,
    used         BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_otps_phone ON otps (phone_number);
CREATE INDEX IF NOT EXISTS idx_otps_expires ON otps (expires_at);

-- ============================================================
-- TABLE: nursery_profiles
-- ============================================================
CREATE TABLE nursery_profiles (
    user_id      UUID          PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    nursery_name VARCHAR(100)  NOT NULL,
    owner_name   VARCHAR(100)  NOT NULL,
    address      TEXT          NOT NULL,
    phone        VARCHAR(15)   NOT NULL,
    logo_path    VARCHAR(255)  NULL,
    initials     VARCHAR(10)   NOT NULL,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- ============================================================
-- TABLE: plants
-- ============================================================
CREATE TABLE plants (
    id              BIGSERIAL    PRIMARY KEY,
    user_id         UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    name_normalized VARCHAR(100) NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ  NULL
);

-- Partial unique index: unique plant name per user when not deleted
CREATE UNIQUE INDEX IF NOT EXISTS idx_plants_user_name_unique
    ON plants (user_id, name_normalized)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_plants_user_id ON plants (user_id);
CREATE INDEX IF NOT EXISTS idx_plants_deleted_at ON plants (deleted_at);

-- ============================================================
-- TABLE: dimensions
-- ============================================================
CREATE TABLE dimensions (
    id         BIGSERIAL    PRIMARY KEY,
    plant_id   BIGINT       NOT NULL REFERENCES plants(id) ON DELETE CASCADE,
    dimension  VARCHAR(20)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ  NULL,
    CONSTRAINT uq_dimensions_plant_dim UNIQUE (plant_id, dimension),
    CONSTRAINT chk_dimension_format CHECK (dimension ~ '^\d+\s*[xX]\s*\d+$')
);

CREATE INDEX IF NOT EXISTS idx_dimensions_plant_id ON dimensions (plant_id);
CREATE INDEX IF NOT EXISTS idx_dimensions_deleted_at ON dimensions (deleted_at);

-- ============================================================
-- TABLE: quotations
-- ============================================================
CREATE TABLE quotations (
    id                     BIGSERIAL     PRIMARY KEY,
    user_id                UUID          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    quotation_number       VARCHAR(50)   NULL UNIQUE,
    year                   INT           NOT NULL,
    sequence               INT           NULL,
    status                 VARCHAR(10)   NOT NULL DEFAULT 'DRAFT'
                                         CHECK (status IN ('DRAFT', 'FINAL')),
    customer_name          VARCHAR(150)  NULL,
    customer_phone         VARCHAR(15)   NULL,
    customer_address       TEXT          NULL,
    total_amount           DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    version                INTEGER       NOT NULL DEFAULT 0,
    public_token           VARCHAR(64)   NULL UNIQUE,
    public_token_expires_at TIMESTAMPTZ NULL,
    finalized_at           TIMESTAMPTZ  NULL,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at             TIMESTAMPTZ  NULL,
    CONSTRAINT uq_quotation_user_year_seq UNIQUE (user_id, year, sequence)
);

CREATE INDEX IF NOT EXISTS idx_quotations_user_status ON quotations (user_id, status) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_quotations_customer_name ON quotations (LOWER(customer_name)) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_quotations_created_at ON quotations (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_quotations_public_token ON quotations (public_token);
CREATE INDEX IF NOT EXISTS idx_quotations_deleted_at ON quotations (deleted_at);
CREATE INDEX IF NOT EXISTS idx_quotations_user_year ON quotations (user_id, year);

-- ============================================================
-- TABLE: quotation_items
-- ============================================================
CREATE TABLE quotation_items (
    id            BIGSERIAL     PRIMARY KEY,
    quotation_id  BIGINT        NOT NULL REFERENCES quotations(id) ON DELETE CASCADE,
    plant_name    VARCHAR(150)  NOT NULL,
    dimension     VARCHAR(20)   NOT NULL,
    quantity      INTEGER       NOT NULL CHECK (quantity > 0),
    price         DECIMAL(12,2) NOT NULL CHECK (price >= 0),
    item_total    DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_quotation_items_quotation_id ON quotation_items (quotation_id);
