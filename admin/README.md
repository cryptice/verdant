# Verdant Admin

React + TypeScript administration UI for species, seed providers, outlets, users, and gardens. The production backend serves it at `/admin`; the end-user web app is in `../web/`.

## Development

Use Node.js 22+ and run the backend on port 8081:

```bash
npm ci
npm run dev
```

Vite runs on port 5174 and proxies `/api` to the backend. Sign in with the configured admin email and password. Backend dev-only seed/reset actions are unavailable in production.

## Checks

```bash
npm run lint   # zero errors or warnings
npm run build  # TypeScript check and Vite production build
```

There is no admin test suite yet. Both checks run in Cloud Build before deployment, and can run in Docker from the repository root with `./scripts/run-tests.sh admin`.

Shared request handling and error types live in `../shared/src`. The root Dockerfile builds both browser applications and bundles them into the backend. See the [root README](../README.md) for deployment and configuration.
