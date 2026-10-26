-- Personal API keys: a user's long-lived credential for one organization's
-- regular API. The key itself is a signed token carrying this row's id; the
-- row holds what is needed to list, expire and revoke it, never the secret.
CREATE TABLE api_key (
    id           UUID PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    org_id       BIGINT       NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
    name         VARCHAR(100) NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expires_at   TIMESTAMPTZ  NOT NULL,
    last_used_at TIMESTAMPTZ,
    revoked_at   TIMESTAMPTZ
);
CREATE INDEX idx_api_key_user_org ON api_key(user_id, org_id);
