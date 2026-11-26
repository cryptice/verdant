-- Space-separated OAuth scopes. Existing requests and grants were read-only.
ALTER TABLE mcp_oauth_request ADD COLUMN scope VARCHAR(100) NOT NULL DEFAULT 'verdant:read';
ALTER TABLE mcp_oauth_grant ADD COLUMN scope VARCHAR(100) NOT NULL DEFAULT 'verdant:read';
