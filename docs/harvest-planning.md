# Harvest planning API

All endpoints are authenticated and require the existing `X-Organization-Id` header. Species and groups may be shared or owned by the active organization; seasons and saved plans must belong to it. These endpoints are additive to the legacy `/api/production-targets` API. The admin UI does not consume planning endpoints.

| Endpoint | Behavior |
| --- | --- |
| `GET /api/harvest-plans/species?speciesId=…` or `?groupId=…` | Effective organization profiles, falling back to species workflow defaults |
| `PUT /api/harvest-plans/species/{id}` | Save a complete validated `ProductionProfile` for this organization |
| `POST /api/harvest-plans/preview` | Calculate allocations, steps, issues, unallocated quantity, `canSave`, and fingerprint |
| `POST /api/harvest-plans` | Save the preview snapshot and generate all tasks atomically |
| `GET /api/harvest-plans?limit=50&offset=0` | Newest plans first; maximum limit 100 |
| `GET /api/harvest-plans/{id}` | Snapshot and current task progress |
| `POST /api/harvest-plans/{id}/tasks/{taskId}/complete` | Record planning progress with optimistic concurrency |
| `POST /api/harvest-plans/{id}/cancel` | Cancel outstanding tasks, preserving counts and completed work |

Preview request example:

```json
{
  "seasonId": 1,
  "groupId": 7,
  "quantity": 300,
  "sellableUnit": "FLOWER",
  "harvestDate": "2027-08-15",
  "allocations": { "12": 100, "19": 0 }
}
```

Choose exactly one `speciesId` or `groupId`. Allocation keys are species IDs: absent means suggested, zero means excluded, positive means fixed output. Quantity is 1–1,000,000; groups are limited to 100 species. The calculator caps required plants and starting material at 10,000,000 per allocation. Feasibility checks use the Stockholm calendar day, consistent with task scheduling.

A production profile contains `sellableUnit` (`STEM`/`FLOWER`), `outputPerPlant`, `establishmentPercent`, `startingUnit` (`SEED`/`PLUG`/`BULB`/`TUBER`/`PLANT`) and `steps`. Each step has a stable unique `key`, `name`, `activityType`, `daysBeforeHarvest` (0–3650), and `quantityBasis` (`START`/`PLANT`/`OUTPUT`). Three to thirty steps run from purchase to harvest in descending offset order. Seeds require a `SOW` starting step; other material requires `PLANT`. Supported activities: `PURCHASE`, `SOW`, `POT_UP`, `PLANT`, `PINCH`, `SUPPORT`, `WATER`, `FERTILIZE`, `HARVEST`, `TODO`.

Save with `{ "requestKey": "<UUID>", "fingerprint": "<preview fingerprint>", "target": <preview request> }`. Reuse the same key on a transport retry; use a new key after changing the request. Reusing a key for different input returns 409. A changed preview returns 409 and requires reviewing a fresh preview. Saved requests remain retryable after profiles change. Updates to profiles or group membership never rewrite saved snapshots.

Complete a task with `{ "expectedRemaining": 188, "processedCount": 50 }`. Counts must be positive and no larger than the remaining quantity. A stale count returns 409; reload before recording further work. Completion does not create inventory or plant events. Cancellation is repeatable and prevents further progress recording.

Scheduled task responses add nullable `harvestPlanId` and `quantityUnit`; task status now also supports `CANCELLED`. The web and Android task lists show only `PENDING` tasks. Manage generated tasks via their plan; legacy task mutation endpoints reject them. Existing clients and existing non-plan tasks retain their earlier semantics.

`V45__harvest_plans.sql` adds two tables and nullable task columns. Existing weekly targets and historical stem data are untouched. Plan snapshots retain the original season ID even if the season is subsequently deleted and the relational season link becomes null. No deployment or production-data backfill is needed beyond applying the migration on a normal backend deployment.

Not yet implemented: task/event reconciliation, inventory reservations, actual flower harvest/sales units, saved drafts, automatic rescheduling, existing-batch allocation, or capacity/climate feasibility. See [the design](plans/2026-10-05-harvest-planning-design.md) for later stages.
