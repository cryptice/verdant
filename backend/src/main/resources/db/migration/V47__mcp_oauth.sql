CREATE TABLE mcp_oauth_client (
    id VARCHAR(80) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    redirect_uris JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE mcp_org_profile (
    org_id BIGINT PRIMARY KEY REFERENCES organization(id) ON DELETE CASCADE,
    profile_id UUID NOT NULL UNIQUE
);
CREATE TABLE mcp_oauth_grant (
    id UUID PRIMARY KEY,
    client_id VARCHAR(80) NOT NULL REFERENCES mcp_oauth_client(id),
    user_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    org_id BIGINT NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ
);
CREATE INDEX mcp_oauth_grant_user ON mcp_oauth_grant(user_id);
CREATE TABLE mcp_oauth_request (
    id UUID PRIMARY KEY,
    client_id VARCHAR(80) NOT NULL REFERENCES mcp_oauth_client(id),
    redirect_uri TEXT NOT NULL,
    state TEXT,
    code_challenge VARCHAR(43) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE mcp_oauth_code (
    code_hash VARCHAR(64) PRIMARY KEY,
    grant_id UUID NOT NULL REFERENCES mcp_oauth_grant(id) ON DELETE CASCADE,
    client_id VARCHAR(80) NOT NULL,
    redirect_uri TEXT NOT NULL,
    code_challenge VARCHAR(43) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE mcp_oauth_refresh (
    token_hash VARCHAR(64) PRIMARY KEY,
    grant_id UUID NOT NULL REFERENCES mcp_oauth_grant(id) ON DELETE CASCADE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ
);
