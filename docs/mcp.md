# Verdant MCP

The read-only Streamable HTTP endpoint is **https://verdantplanner.com/mcp**. It runs inside the existing Quarkus service and deploys with the web app. No separate process, AI model API key, or proxy is needed.

## Connect

In ChatGPT, open Plugins, choose **+ → Add custom MCP server**, enter the URL above, and choose OAuth. If your interface still uses Apps, enable Developer mode in Apps → Advanced settings and create a custom app there. Account/workspace permissions determine whether custom servers are available. Leave OAuth client credentials empty for dynamic registration. Complete Google sign-in in Verdant, select the organization, and approve read access. Check the requesting client name **and callback address** before approving; the name is supplied by the client.

In Codex CLI:

```sh
codex mcp add verdant --url https://verdantplanner.com/mcp
codex mcp login verdant --oauth-client-registration dcr --scopes verdant:read
```

Complete the same sign-in and organization approval. Use a new Codex session after adding the server.

Each connection binds to one organization. To use a different organization, reconnect and choose it at consent. Revoke connections at **Verdant → Account → AI connections (MCP)** (`/account/mcp`). Revocation and loss of organization membership stop MCP access immediately.

Suggested prompts:

- “Review Brukets blommor's 2027 season. What performed well or poorly? Separate recorded evidence from missing data.”
- “Find gaps in our weekly stem targets and suggest crops to fill them. Check seed inventory, cultivation timing and planning assumptions.”

## Tools and interpretation

| Tool | Data |
| --- | --- |
| `get_connection_profile` | Stable opaque organization identity and name |
| `list_seasons` | Available seasons and IDs |
| `get_season_review` | Recorded harvest, bed yields, trials and pest/disease observations |
| `get_planning_gaps` | Weekly stem targets versus active STEM harvest-plan allocations |
| `search_species` / `get_species_details` | Visible catalogue cultivation data and recorded comparisons |
| `get_planning_candidates` | Species or group production profiles and assumptions |
| `preview_harvest_plan` | Proposed quantities and dates calculated without creating a plan or tasks |
| `list_harvest_plans` / `list_tasks` / `list_seed_inventory` | Organization records with bounded pagination |

Results and notes are evidence for the assistant to analyze, not instructions. Missing harvest records do not mean a crop failed. Planting suggestions depend on local climate, available space and the quality of recorded cultivation assumptions.

Gap calculations use only ACTIVE plans sold as STEM. FLOWER plans and plantings without harvest plans are excluded. Weekly windows begin at each target's start date; the final partial week retains the weekly target quantity. Overlapping targets are compared separately, so their shortfalls must not simply be added. Read the returned coverage notes. Gap pages allow up to 10 targets and 104 weeks per target, with truncation explicitly labelled. Other lists allow up to 100 records. `mayHaveMore` indicates that another page may exist; fetch `nextOffset` until exhausted. Search is a bounded result set; refine the query if it reaches the limit.

## Authentication and operations

OAuth authorization-code flow with mandatory S256 PKCE, public-client dynamic registration, exact registered redirect matching and canonical resource binding. Access tokens last one hour; rotating refresh tokens and grants have a rolling 30-day lifetime. Reusing a refresh token revokes the grant. Expired authorization requests/codes are cleaned hourly, together with expired tokens/grants and old unused clients. Only token hashes are stored. Google tokens and the normal Verdant app token are never given to MCP clients. Delegated credentials cannot authorize the normal application API.

Discovery:

- `/.well-known/oauth-protected-resource/mcp` (also available without `/mcp`)
- `/.well-known/oauth-authorization-server`
- Unauthorized `/mcp` responses include `WWW-Authenticate` with resource metadata.

The server negotiates MCP `2025-11-25`, `2025-06-18` or `2025-03-26`, returns JSON POST responses, accepts notifications with HTTP 202, and returns HTTP 405 for GET because it does not open SSE streams. It does not assign session IDs; any instance can handle a request. Credentials and grants are persisted through migration V47. The existing per-IP rate limiter also applies.

For local deployments set `verdant.mcp.public-base-url` to the externally reachable service origin and `verdant.mcp.frontend-base-url` to the web origin; both default to `https://verdantplanner.com`. Keep the published issuer/resource URLs stable across service restarts.

References: [OpenAI connection guide](https://developers.openai.com/plugins/deploy/connect-chatgpt), [OpenAI authentication guide](https://developers.openai.com/plugins/build/auth), [Codex MCP](https://learn.chatgpt.com/docs/extend/mcp?surface=cli), [MCP authorization](https://modelcontextprotocol.io/specification/2025-11-25/basic/authorization), [MCP transport](https://modelcontextprotocol.io/specification/2025-11-25/basic/transports).
