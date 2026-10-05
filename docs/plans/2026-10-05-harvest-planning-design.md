# Harvest targets and backward production planning

Status: draft. The requested outcome is clear; target units/date semantics and group-allocation behavior are awaiting clarification. This document is a proposed design, not implemented functionality.

## Outcome

A grower enters a target such as “300 large pom pom dahlias ready for harvest on 15 August,” selecting either a species or a grower-defined species group. Verdant works backward through the relevant lifecycle schedules and automatically creates dated, quantity-bearing tasks from purchasing propagation material through harvesting.

Dates and quantities are planning estimates with visible assumptions. The system should expose an infeasible target or missing information rather than fill the gaps with arbitrary agronomic defaults.

## Decisions to settle

1. Does the quantity mean harvestable stems within a date window, stems on one exact date, or plants ready to start harvesting? Model an explicit output unit and an inclusive harvest window; a single date is a window with identical start/end dates. Do not interpret a readiness date as permission to count all earlier harvests.
2. For a group, should Verdant suggest an editable mix, require explicit allocations, or choose the mix automatically? Group membership determines eligibility; it does not by itself determine what to grow or how much.

## Existing foundations and gaps

- `ProductionTargetService` currently models one species, stems per week, and a date range. Its forecast uses whole weeks, a species yield assumption, germination rate, and minimum days to harvest. It neither supports one-off group demand nor creates a complete lifecycle plan. A same-day range currently yields zero demand, so the existing forecast must not be reused for dated quantities.
- `WorkflowService` already supports templates, species-specific steps, per-plant copies, timing offsets, optional steps, and side branches. This is the foundation for configurable lifecycle schedules; avoid a second competing workflow editor.
- `SuccessionScheduleService.generateTasks` creates sowing tasks only. It has no production-target linkage and repeated calls create additional tasks. New planning must use idempotent generation rather than copying this behavior.
- `ScheduledTask` supports quantities, earliest dates, deadlines, and species groups, but has no production-plan step linkage or cancellation state. Generic `TODO` tasks cannot currently reference species or groups.
- Seed inventory distinguishes seeds, plugs, bulbs, tubers, and plants. Purchase planning should preserve those units instead of forcing every lifecycle to start with seeds.
- Plant actions already record workflow progress. Connect planning to these actual events rather than asking the grower to mark the same work complete in two places.

## Grower experience

1. Enter the output quantity, species/group, harvest date or window, and season.
2. View the eligible species, proposed or explicit allocations, existing stock, required new production, and missing assumptions.
3. Inspect a backward timeline for each planned batch. Override a duration, quantity, propagation method, or date for this plan without changing every future plan.
4. Save the plan to create its tasks automatically in the existing task list and calendar. A purchase task is a reminder, not an automatic supplier order.
5. Record actual purchases/receipts, sowings, plant movements, losses, and harvests through existing activities. Show their effects on the target's forecast and remaining demand.

Each generated task shows its target, species, batch, quantity/unit, date window, and prerequisite. Example task labels should use actual calculated quantities: “Order [N] tubers,” “Sow [N] seeds,” “Pot up [N] plants,” and “Harvest [N] stems.” Use one task per batch/step, not hundreds of per-plant reminders.

## Lifecycle configuration

Use the existing template → species → individual production hierarchy, extended with a versioned plan snapshot:

- A reusable template supplies the default ordered lifecycle.
- Species settings override timings, propagation method, losses, and yield assumptions.
- Organization-specific settings are needed for shared catalog species; one grower's changes must not mutate another grower's schedule. Existing workflow mutation checks currently require species ownership.
- A plan copies the effective settings and records their source. Changes to defaults affect new plans; updating an active plan is an explicit replan operation with a visible diff.

Steps distinguish an actionable task from a waiting interval or readiness milestone. Purchase lead time, receipt, sowing/start, potting up, planting out, optional care steps, harvest readiness, and harvesting can be represented without assuming every species needs every step.

For the first version, use an ordered main lifecycle with explicit inclusion/exclusion of optional steps. Do not automatically treat side branches as mandatory sequential delays or additional yield.

## Backward calculation

Anchor the plan to the requested harvest window. Calculate the preceding required milestones using the configured intervals. A missing interval blocks that portion of the schedule; a date in the past is shown as infeasible/overdue, not silently moved to today.

Quantities are calculated for each allocated species and batch:

- Determine expected harvestable output per productive plant **in the requested window**. The existing unqualified `expectedStemsPerPlant` is not enough to promise date-specific output.
- Divide allocated demand by that yield and round up to productive plants.
- Work backward through configured establishment/survival/germination rates, rounding up at each stage. Validate rates and yields, and make any deliberate safety buffer visible rather than applying it twice.
- Subtract suitable, unreserved stock from the required starting material. Purchase only the remaining shortage, respecting the selected stock unit and configured supplier lead time.
- Distinguish ordered material from received material. Do not increase usable stock when a purchase reminder is merely marked complete.

Existing growing batches may contribute only when they have an explicit expected contribution to the harvest window and that output has not been allocated elsewhere. Seed stock and future harvest output need separate reservations. Do not count the same batch's entire seasonal yield against several date-specific targets.

For group targets, calculate each species using its own lifecycle and yield assumptions, then aggregate the forecast. Never average all species into a single sowing date. Suggested allocations must be transparent and editable; missing data should prevent a false claim that the mix is achievable.

## Execution and replanning

Keep three separate values: the target commitment, the saved plan, and the current forecast from actual progress. Finishing a task late must not silently change the customer's required date.

- Bind actual batches and activity records to plan steps, including partial completion and quantities.
- Use actual progress to update remaining work and flag a likely shortfall or late harvest.
- Preview changes before replacing an active schedule; preserve completed work and explicitly pinned dates.
- Regeneration updates the same pending steps, cancels superseded work, and never duplicates tasks.
- Cancellation releases unused reservations and cancels future tasks while retaining production and harvest history.
- Changing a group's membership does not rewrite active plans. Keep the saved eligible-species set and allocation until replanning.

## Implementation shape

Extend production targets with a distinct dated-quantity mode while retaining existing weekly targets and their data. Use additive migrations and update backend, web, and Android contracts together.

Introduce organization-owned plan records, species/batch allocations, snapshotted steps, inventory reservations, and actual-event links. Tasks reference stable plan-step IDs with a database uniqueness constraint. Save/replan operations must be transactional, version-checked, and safe to retry; overlapping stock reservations must be checked under concurrency.

Put calculations in a deterministic planning service that accepts the target, lifecycle snapshots, stock/allocations, and an explicit current date. Keep date arithmetic and quantity calculations out of the clients. Preview and save must use the same calculation rules, with stock availability revalidated when saving.

Roll out in usable increments:

1. Dated species/group targets, configurable lifecycle snapshots, explicit species allocation, backward-plan preview, and idempotent generation of purchase-to-harvest tasks. Add typed procurement tasks and client routing instead of disguising them as sowing events.
2. Stock reservations and linked actual batches/events, partial completion, remaining-demand forecasts, and safe replanning. These are required before presenting existing stock/output as committed to a target.
3. Suggested species mixes, allocation across competing targets, succession recommendations, and optional space/labor constraints.

Weather-derived timing and the unfinished weather experiment are outside this scope. Initial schedules use explicit grower-configured assumptions; optional seasonal constraints can be entered directly.

## Acceptance scenarios

- A same-day target retains the full requested quantity instead of becoming zero weeks of demand.
- A group with two differently timed species produces separate schedules and a combined output forecast.
- Missing timing/yield information is explained without an invented promise of readiness.
- Changing germination, survival, lead time, or harvest date changes the relevant quantities/dates predictably.
- Seed-, plug-, and tuber-started plans retain the correct units and selected lifecycle steps.
- Repeated saves or concurrent generation cannot create duplicate tasks or over-reserve stock.
- Completing half a sowing task creates/links only the actual production and leaves the correct remaining work.
- Actual losses or delays change the forecast and expose a shortfall without changing the original target.
- Replanning preserves completed work; cancellation releases only unused reservations.
- Group/default-schedule edits do not silently alter existing plans.
- Users cannot read, plan with, reserve, or mutate another organization's stock, targets, workflows, or tasks.
