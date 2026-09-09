-- ========================================
-- V1: 전체 스키마 초기화
-- ========================================

CREATE TABLE users
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    email       VARCHAR(255),
    nickname    VARCHAR(50)  NOT NULL,
    provider    VARCHAR(20)  NOT NULL,
    provider_id VARCHAR(255) NOT NULL,
    role        VARCHAR(10)  NOT NULL,
    deleted_at  DATETIME(6),
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_users_provider UNIQUE (provider, provider_id)
);

CREATE INDEX idx_users_email ON users (email);


CREATE TABLE refresh_tokens
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at DATETIME(6)  NOT NULL,
    revoked    TINYINT(1)   NOT NULL DEFAULT 0,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);


CREATE TABLE gunpla_catalog
(
    id                     BIGINT       NOT NULL AUTO_INCREMENT,
    name                   VARCHAR(200) NOT NULL,
    name_en                VARCHAR(200),
    grade                  VARCHAR(10)  NOT NULL,
    series                 VARCHAR(100),
    scale                  VARCHAR(20),
    release_price          INT,
    release_price_currency VARCHAR(3)   NOT NULL DEFAULT 'JPY',
    release_date           DATE,
    manufacturer           VARCHAR(100),
    thumbnail_url          VARCHAR(500),
    created_at             DATETIME(6)  NOT NULL,
    updated_at             DATETIME(6)  NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_gunpla_catalog_grade ON gunpla_catalog (grade);
CREATE INDEX idx_gunpla_catalog_series ON gunpla_catalog (series);


CREATE TABLE user_collection
(
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    user_id         BIGINT      NOT NULL,
    catalog_id      BIGINT      NOT NULL,
    build_status    VARCHAR(20) NOT NULL,
    purchase_price  INT,
    purchase_currency VARCHAR(3),
    purchase_date   DATE,
    purchase_place  VARCHAR(100),
    memo            TEXT,
    deleted_at      DATETIME(6),
    created_at      DATETIME(6) NOT NULL,
    updated_at      DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_user_collection_user    FOREIGN KEY (user_id)    REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_collection_catalog FOREIGN KEY (catalog_id) REFERENCES gunpla_catalog (id)
);

CREATE INDEX idx_user_collection_user_deleted ON user_collection (user_id, deleted_at);
CREATE INDEX idx_user_collection_build_status ON user_collection (build_status);


CREATE TABLE collection_images
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    collection_id BIGINT       NOT NULL,
    s3_key        VARCHAR(500) NOT NULL,
    display_order INT          NOT NULL DEFAULT 0,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_collection_images_collection FOREIGN KEY (collection_id) REFERENCES user_collection (id) ON DELETE CASCADE
);


CREATE TABLE wishlist
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    catalog_id BIGINT      NOT NULL,
    priority   VARCHAR(10) NOT NULL,
    memo       TEXT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_wishlist_user_catalog UNIQUE (user_id, catalog_id),
    CONSTRAINT fk_wishlist_user    FOREIGN KEY (user_id)    REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_wishlist_catalog FOREIGN KEY (catalog_id) REFERENCES gunpla_catalog (id)
);
