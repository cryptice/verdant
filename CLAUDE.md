# CLAUDE.md

This file provides guidance for Claude and other AI coding assistants working in this repository.

## Repository overview

This repository contains three main applications:

- **Backend**: Quarkus + Kotlin
- **Android app**: Kotlin-based native Android application
- **Admin UI**: React-based web frontend

The system is treated as a single product with multiple clients. Changes should preserve consistency across API contracts, validation rules, naming, and business logic.

---

## General working principles

- Prefer **small, targeted changes** over large speculative refactors.
- Preserve existing architecture and conventions unless explicitly asked to change them.
- Favor **readability, maintainability, and correctness** over cleverness.
- Avoid introducing new dependencies unless clearly justified.
- When making cross-cutting changes, update all affected layers:
  - backend DTOs / API contracts
  - Android client models / API usage
  - React admin UI models / API usage
  - tests
  - documentation where relevant
- Do not silently change public API shapes, database semantics, or behavior relied on by clients.
- If a requested change is ambiguous, prefer the most conservative implementation consistent with existing code.

---

## Expected repo structure

Actual folder names may vary, but the repository typically contains areas similar to:

- `backend/` — Quarkus Kotlin service
- `android/` — Android app
- `admin-ui/` or `web/` — React admin UI

Before making changes:
- inspect the actual module structure
- identify each module's build system and tooling
- work within the conventions already present in that module

---

## Architecture expectations

### Backend
- Quarkus application written in Kotlin
- Likely layered architecture, for example:
  - resource/controller layer
  - service/application layer
  - persistence/DAO/repository layer
  - model/entity/DTO layer
- Keep HTTP concerns in resource/controller classes
- Keep business logic in services
- Keep persistence concerns in repositories/DAOs
- Do not move large amounts of logic across layers unless explicitly requested

### Android app
- Kotlin Android app
- Respect the existing architectural pattern in use:
  - MVVM / MVI / Clean Architecture / repository pattern, etc.
- UI code should remain thin
- Business logic should not be embedded in Activities/Fragments/Composables unless the codebase already strongly follows that style
- Reuse existing networking, state management, and navigation patterns

### Admin UI
- React application
- Follow existing patterns for:
  - component structure
  - hooks
  - API layer
  - routing
  - state management
  - styling
- Keep presentational and data-fetching concerns separated when that is already the project convention

---

## Coding style

### General
- Match the existing code style in each module
- Follow existing naming conventions exactly
- Avoid unnecessary comments; add comments only where intent is non-obvious
- Prefer explicit, predictable code over magic abstractions
- Avoid placeholder implementations unless explicitly requested
- Do not leave TODOs unless they are necessary and clearly explained

### Kotlin
- Prefer idiomatic Kotlin
- Use nullability intentionally and safely
- Prefer immutable values (`val`) unless mutation is required
- Use expressive function and variable names
- Avoid overly nested scope functions
- Keep functions focused and reasonably small
- Reuse existing extension functions and utilities where appropriate
- Do not introduce broad inheritance hierarchies without strong reason

### React / TypeScript / JavaScript
- Prefer functional components and hooks unless the codebase uses a different pattern
- Keep components focused
- Avoid deeply coupled state unless required by the existing architecture
- Reuse existing UI primitives, utilities, and API clients
- Preserve existing lint and formatting conventions

---

## API and contract changes

When changing backend APIs:
- identify all consumers of the API
- update Android app and React admin UI if needed
- preserve backward compatibility unless breaking change is explicitly intended
- update request/response types consistently
- keep validation rules aligned across clients and server where applicable

When adding new fields:
- consider serialization/deserialization impact
- consider database migration impact
- consider default values and nullability
- consider UI rendering impact in both clients

---

## Data and persistence

- Do not change schema, migrations, or persistence behavior casually
- If a data model changes, inspect:
  - persistence entities
  - DTOs
  - mappers
  - API contracts
  - tests
- Prefer explicit migrations over implicit behavior changes
- Be careful with destructive changes, especially deletes, renames, and type changes

---

## Error handling

- Follow the existing error-handling conventions in each module
- Do not swallow exceptions silently
- Return structured, predictable errors from the backend
- Preserve user-facing error clarity in Android and React clients
- Avoid leaking internal implementation details in API error responses

---

## Security and privacy

- Treat all authentication, authorization, and session logic as sensitive
- Do not weaken validation or access checks
- Do not log secrets, tokens, passwords, or sensitive personal data
- Be cautious when changing:
  - login flows
  - token handling
  - permission checks
  - admin-only functionality
  - file upload/download logic

If a change touches security-sensitive code, keep the scope minimal and explicit.

---

## Testing expectations

Whenever practical, add or update tests for the change.

### Backend
Prefer updating or adding:
- unit tests for business logic
- integration tests for resource/API behavior
- persistence tests where repository behavior changes

### Android
Prefer updating or adding:
- unit tests for view models / business logic
- UI tests only when the repo already uses them and the change warrants it

### Admin UI
Prefer updating or adding:
- component tests
- hook or utility tests
- API layer tests where relevant

Do not invent an entirely new testing approach in a focused feature change.

---

## Build and verification

Before considering work complete, run the narrowest useful verification available for the affected modules.

Examples:
- backend tests or build
- Android unit tests or compile checks
- admin UI tests, lint, typecheck, or build

Prefer targeted validation first, then broader validation if needed.

If you cannot run commands in the current environment, state that clearly.

---

## Dependency management

- Prefer existing libraries and utilities already used in the repo
- Do not add new frameworks or major libraries without strong justification
- If a dependency is necessary, explain why and keep it minimal
- Avoid duplicate utility stacks that overlap with existing solutions

---

## Documentation updates

Update documentation when changes affect:
- setup
- configuration
- environment variables
- API usage
- user flows
- developer workflow

Keep docs concise and aligned with the actual implementation.

---

## Configuration and environments

- Do not hardcode environment-specific values
- Respect the existing configuration mechanisms for each module
- Preserve separation between local, test, staging, and production configuration
- If adding config:
  - choose clear names
  - document defaults and purpose
  - wire it through the existing config system

---

## Preferred workflow for AI assistants

When given a task:

1. Identify which module(s) are affected
2. Inspect local conventions before editing
3. Make the smallest coherent change that solves the problem
4. Update dependent code paths if the change crosses module boundaries
5. Add or update tests where appropriate
6. Summarize what changed and any follow-up concerns

---

## Things to avoid

- Do not perform broad refactors without being asked
- Do not rewrite files just to match personal style
- Do not rename symbols unless necessary
- Do not introduce breaking API changes without calling them out
- Do not duplicate logic that already exists elsewhere in the repo
- Do not guess architecture; inspect first
- Do not ignore compiler, type, or lint errors caused by your changes

---

## When unsure

If implementation details are unclear:
- infer from nearby code
- follow established patterns in the same module
- choose the least disruptive option

If there are multiple valid approaches, prefer the one most consistent with the existing codebase.

---

## Output expectations for code changes

When proposing or making changes:
- be explicit about which modules are affected
- note any contract changes
- mention any migrations or config additions
- mention tests added or updated
- call out anything that still needs manual verification

---

### Backend
- package naming rules
- DTO/entity mapping conventions
- exception handling conventions
- database migration tool
- auth model

### Android
- Jetpack Compose vs XML
- navigation approach
- networking stack
- image loading library
- state management pattern

### Admin UI
- TypeScript vs JavaScript
- component library
- state management approach
- API client conventions
- form handling conventions

